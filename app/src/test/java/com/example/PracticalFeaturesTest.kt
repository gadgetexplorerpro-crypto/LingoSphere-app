package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.PracticalFeaturesAiService
import com.example.data.model.HabitRoutineSlot
import com.example.data.model.LivingWorldCharacterProgress
import com.example.data.model.MediaPrivacyConsentState
import com.example.data.model.PracticalFeaturesCatalog
import com.example.data.model.RealLifeDetectedItem
import com.example.data.model.RealLifeObjectRecognition
import com.example.data.model.VerifiedHabitCompletion
import com.example.data.repository.LearningMemoryAndOfflineManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PracticalFeaturesTest {

    private lateinit var context: Context
    private lateinit var manager: LearningMemoryAndOfflineManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("lingosphere_learning_memory_and_offline", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        manager = LearningMemoryAndOfflineManager(context)
    }

    @Test
    fun `1 Speak What You See generates contextual questions and evaluates spoken answers`() = runBlocking {
        val scene = RealLifeObjectRecognition(
            sceneTitle = "Madrid Neighborhood Café",
            sceneDescription = "Coffee cup, menu board, and croissant on a wooden table",
            targetLanguage = "Spanish",
            detectedItems = listOf(
                RealLifeDetectedItem(
                    targetWord = "la taza de café",
                    nativeWord = "coffee cup",
                    pronunciationGuide = "lah TAH-sah deh kah-FEH",
                    exampleSentence = "La taza de café está caliente en la mesa.",
                    exampleTranslation = "The coffee cup is hot on the table.",
                    contextualExplanation = "Everyday café item"
                )
            )
        )
        val questions = PracticalFeaturesAiService.generateSpeakWhatYouSeeQuestions(scene, "es")
        assertTrue(questions.isNotEmpty())
        val firstQ = questions.first()
        val evaluation = PracticalFeaturesAiService.evaluateSpeakWhatYouSeeAnswer(
            question = firstQ,
            spokenAnswer = "Veo la taza de café caliente en esta mesa hoy.",
            languageCode = "es"
        )
        assertTrue(evaluation.answeredQuestionDirectly)
        assertTrue(evaluation.correctedSentenceTargetLanguage.isNotBlank())
    }

    @Test
    fun `2 Conversation Rescue provides phrase suggestions native explanations and in-place retry`() = runBlocking {
        val rescue = PracticalFeaturesAiService.generateConversationRescue(
            languageCode = "es",
            lastPartnerPrompt = "¿Qué opinas sobre este proyecto y cuándo podemos reunirnos?",
            learnerPartialOrStruggledDraft = "Yo creo..."
        )
        assertEquals("es", rescue.languageCode)
        assertTrue(rescue.suggestions.size >= 3)
        assertTrue(rescue.suggestions.all { it.targetPhrase.isNotBlank() && it.nativeMeaning.isNotBlank() })
        assertTrue(rescue.grammarBridgeTip.isNotBlank())
    }

    @Test
    fun `3 Living Language World persists character trust memory and spoken task completion`() = runBlocking {
        val locations = PracticalFeaturesCatalog.livingWorldLocationsForLanguage("es")
        assertTrue(locations.isNotEmpty())
        val mercado = locations.first()
        val initialProgress = LivingWorldCharacterProgress(
            locationId = mercado.id,
            characterName = mercado.characterName,
            trustScore = 20
        )
        val outcome = PracticalFeaturesAiService.advanceLivingWorldTurn(
            location = mercado,
            progress = initialProgress,
            userSpokenMessage = "Hola Doña Carmen, hoy voy a preparar una cena familiar y quiero saber qué queso me recomienda."
        )
        assertNotNull(outcome.taskCompletedThisTurn)
        assertTrue(outcome.trustDelta > 0)

        val updatedProgress = initialProgress.copy(
            trustScore = initialProgress.trustScore + outcome.trustDelta,
            completedTaskIds = listOfNotNull(outcome.taskCompletedThisTurn?.id),
            rememberedLearnerFacts = listOfNotNull(outcome.newlyRememberedFact),
            totalSpokenTurns = 1
        )
        manager.saveLivingWorldProgress(updatedProgress)

        val loaded = manager.getLivingWorldProgressMap()[mercado.id]
        assertNotNull(loaded)
        assertTrue((loaded?.trustScore ?: 0) > 20)
        assertEquals(1, loaded?.completedTaskIds?.size)
    }

    @Test
    fun `4 Future Me Speaking preserves consent and explicitly labels synthetic reference without voice-cloning claims`() = runBlocking {
        val consent = MediaPrivacyConsentState(
            cameraCaptureConsent = true,
            microphoneSpeakingConsent = true,
            storeRecordingsOnDevice = true,
            syntheticFluentReferenceConsent = true
        )
        manager.saveMediaPrivacyConsent(consent)
        assertTrue(manager.getMediaPrivacyConsent().syntheticFluentReferenceConsent)

        val comparison = PracticalFeaturesAiService.generateFutureMeComparison(
            languageCode = "es",
            learnerTranscript = "Yo quiero hablar español más rápido en mi trabajo.",
            consentVerified = true
        )
        assertEquals("Synthetic Fluent Reference — Not Voice Cloning", comparison.explicitSyntheticLabel)
        assertTrue(comparison.actionableDrills.isNotEmpty())

        manager.recordFutureMeComparison(comparison)
        assertEquals(1, manager.getFutureMeComparisons().size)
    }

    @Test
    fun `5 Language Emergency Mode supports offline packs and explicit language capability notices`() = runBlocking {
        val noticeJa = PracticalFeaturesCatalog.capabilityNoticeForLanguage("ja")
        assertTrue(noticeJa.usesNonLatinScript)
        assertFalse(noticeJa.isRightToLeft)

        val noticeAr = PracticalFeaturesCatalog.capabilityNoticeForLanguage("ar")
        assertTrue(noticeAr.isRightToLeft)

        val phrasesEs = PracticalFeaturesCatalog.emergencyPhrasesForLanguage("es")
        assertTrue(phrasesEs.size >= 4)

        val eval = PracticalFeaturesAiService.evaluateEmergencySpokenResponse(
            item = phrasesEs.first(),
            userSpokenText = phrasesEs.first().targetPhrase
        )
        assertTrue(eval.contains("Clear emergency communication"))

        val updatedPacks = manager.setOfflineEmergencyPackDownloaded("de", true)
        assertTrue("de" in updatedPacks)
    }

    @Test
    fun `6 AI Debate Partner evaluates language clarity and structure without judging personal opinions`() = runBlocking {
        val topic = PracticalFeaturesCatalog.debateTopics().first()
        val eval = PracticalFeaturesAiService.conductDebateTurn(
            topic = topic,
            languageCode = "es",
            learnerArgument = "Por un lado el trabajo remoto ofrece flexibilidad, sin embargo la colaboración presencial ayuda al equipo."
        )
        assertTrue(eval.aiCounterArgumentTarget.isNotBlank())
        assertTrue(eval.neutralityStatement.contains("never personal opinions"))
    }

    @Test
    fun `7 Explain It Three Ways generates simultaneous Beginner Intermediate and Advanced explanations plus check`() = runBlocking {
        val result = PracticalFeaturesAiService.explainItThreeWays(
            queryConcept = "Por vs. Para",
            languageCode = "es"
        )
        assertEquals("Beginner", result.beginnerLevel.levelName)
        assertEquals("Intermediate", result.intermediateLevel.levelName)
        assertEquals("Advanced", result.advancedLevel.levelName)
        assertTrue(result.comprehensionCheck.options.size >= 2)
    }

    @Test
    fun `8 Conversation Detective supports multiple plausible cultural interpretations rather than a single rigid meaning`() {
        val cases = PracticalFeaturesCatalog.detectiveCasesForLanguage("es")
        assertTrue(cases.isNotEmpty())
        val firstCase = cases.first()
        val plausibleCount = firstCase.interpretations.count { it.isPlausibleInterpretation }
        assertTrue("Expected multiple plausible cultural interpretations", plausibleCount >= 2)
        assertTrue(firstCase.multiPerspectiveNote.isNotBlank())
    }

    @Test
    fun `9 Language Habit Coach tracks verified spoken or written completions rather than notification opens`() {
        val missions = PracticalFeaturesCatalog.habitMissionsForLanguage("es")
        assertEquals(4, missions.size)
        manager.toggleHabitRoutineSlot(HabitRoutineSlot.LUNCH_BREAK.id)
        assertTrue(HabitRoutineSlot.LUNCH_BREAK.id in manager.getSelectedHabitRoutineIds())

        val completion = VerifiedHabitCompletion(
            routineSlotId = HabitRoutineSlot.MORNING_COFFEE.id,
            languageCode = "es",
            missionId = missions.first().id,
            userSpokenOrWrittenTranscript = "Esta mañana tomo café con leche y hoy quiero terminar dos informes.",
            wordCount = 12,
            feedbackSummary = "Verified 12-word spoken routine completion"
        )
        val saved = manager.recordVerifiedHabitCompletion(completion)
        assertEquals(1, saved.size)
        assertEquals(12, saved.first().wordCount)
    }

    @Test
    fun `10 Teach the AI asks clarifying questions and evaluates explanation clarity`() = runBlocking {
        val topic = PracticalFeaturesCatalog.teachAiTopics().first()
        val eval = PracticalFeaturesAiService.interactWithAiStudent(
            topic = topic,
            languageCode = "es",
            learnerExplanation = "Primero cortamos las verduras frescas en trozos pequeños y luego las sofreímos con aceite de oliva."
        )
        assertTrue(eval.aiStudentFollowUpQuestionTarget.isNotBlank())
        assertTrue(eval.conceptClarityReached)
    }

    @Test
    fun `11 Conversation Repair Lab trains learners to resolve misunderstandings and persists completion`() = runBlocking {
        val scenarios = PracticalFeaturesCatalog.repairScenariosForLanguage("es")
        assertTrue(scenarios.isNotEmpty())
        val firstScenario = scenarios.first()
        val result = PracticalFeaturesAiService.evaluateConversationRepairTurn(
            scenario = firstScenario,
            learnerRepairAttempt = "Perdone, ¿podría repetirlo un poco más despacio? ¿A qué andén debo ir?"
        )
        assertTrue(result.repairSucceeded)
        manager.recordCompletedRepairScenario(firstScenario.id)
        assertTrue(firstScenario.id in manager.getCompletedRepairScenarioIds())
    }

    @Test
    fun `12 Language Skill Passport compiles evidence-based competencies with explicit non-certified disclaimer`() {
        manager.recordCompletedRepairScenario("rep_es_1")
        val passport = manager.buildLanguageSkillPassportSummary(
            languageCode = "es",
            flashcards = emptyList(),
            speakingSessions = emptyList()
        )
        assertEquals("es", passport.languageCode)
        assertEquals(4, passport.competencies.size)
        assertTrue(passport.officialDisclaimer.contains("does not constitute an official certification or CEFR-accredited examination"))
    }
}
