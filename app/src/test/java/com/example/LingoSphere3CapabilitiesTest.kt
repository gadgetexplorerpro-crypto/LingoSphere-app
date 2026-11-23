package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiTutorService
import com.example.data.model.AdaptiveLearningEngine
import com.example.data.model.CameraDetectedItem
import com.example.data.model.CameraSceneResult
import com.example.data.model.ConversationReplaySession
import com.example.data.model.ImmersionDialogueTurn
import com.example.data.model.LingoSphere3Catalog
import com.example.data.model.PortfolioPrivacySettings
import com.example.data.model.PortfolioSpeakingSample
import com.example.data.model.ReplayTurnItem
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.speech.SpeechManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LingoSphere3CapabilitiesTest {

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun capability1_aiLanguageMemory_persistsAndBuildsPersonalizationPrompt() {
    val memoryManager = LearningMemoryAndOfflineManager(context)
    memoryManager.saveLearningGoals(listOf("Order food in Paris", "Pass DELF B2"))
    memoryManager.appendConversationHistorySummary("Practiced French café dialogue (91%)")
    memoryManager.recordOrIncrementRepeatedMistake(
      category = "Preposition Contraction",
      ruleSummary = "Contraction of à + le = au",
      exampleWrong = "Je suis allé au le marché",
      exampleCorrected = "Je suis allé au marché"
    )

    val profile = memoryManager.buildAiLanguageMemoryProfile(
      targetLanguage = "French",
      cefrLevel = "B1",
      extraWeakWords = listOf("l'addition", "cependant")
    )

    assertTrue(profile.learningGoals.contains("Order food in Paris"))
    assertTrue(profile.vocabularyWeaknesses.contains("l'addition"))
    assertTrue(profile.repeatedGrammarMistakes.any { it.exampleCorrected == "Je suis allé au marché" })
    assertTrue(profile.toPersonalizationPrompt().contains("AI LANGUAGE MEMORY — PERSONALIZATION CONTEXT"))

    GeminiTutorService.updateActiveMemoryProfile(profile)
  }

  @Test
  fun capability2_liveConversationAssistant_generatesCaptionsVocabAndSuggestions() = runBlocking {
    val (caption, vocab, phrases) = GeminiTutorService.analyzeLiveCallUtterance(
      speakerName = "Mateo",
      utteranceText = "Hola, ¿qué te gustaría pedir hoy en el restaurante?",
      targetLanguage = "Spanish",
      nativeLanguage = "English",
      conversationTopic = "Restaurant & Culture"
    )

    assertEquals("Mateo", caption.speakerName)
    assertTrue(caption.originalText.isNotBlank())
    assertTrue(caption.translatedCaption.isNotBlank())
    assertTrue(vocab.isNotEmpty())
    assertTrue(phrases.isNotEmpty())
  }

  @Test
  fun capability3_speakFromRealLife_generatesAndEvaluatesSpeakingExercises() = runBlocking {
    val scene = CameraSceneResult(
      sceneTitle = "French Bistro Table",
      sceneDescription = "French bistro table with coffee and croissant",
      items = listOf(
        CameraDetectedItem(
          detectedObjectOrText = "Croissant",
          nameInTargetLanguage = "Le croissant au beurre",
          nativeTranslation = "Butter croissant",
          phoneticGuide = "luh krwa-sahn oh bur",
          exampleSentenceTarget = "Je voudrais un croissant au beurre, s'il vous plaît.",
          exampleSentenceNative = "I would like a butter croissant, please.",
          grammarNote = "Use 'Je voudrais' for polite requests."
        )
      )
    )

    val exercises = GeminiTutorService.buildSpeakingExercisesFromCameraScene(sceneResult = scene)
    assertEquals(1, exercises.size)
    val ex = exercises.first()
    assertEquals("Le croissant au beurre", ex.itemTerm)

    val eval = GeminiTutorService.evaluateRealLifeSpeakingExercise(
      exercise = ex,
      userSpokenTranscript = "Je voudrais un croissant au beurre s'il vous plaît",
      targetLanguage = "French"
    )
    assertTrue(eval.pronunciationScore >= 70)
    assertTrue(eval.contextualAccuracyScore >= 70)
    assertTrue(eval.improvedNativeVersion.isNotBlank())
    assertTrue(eval.passed)
  }

  @Test
  fun capability4_aiImmersionMode_providesTokyoParisLondonScenariosAndDynamicResponses() = runBlocking {
    val japaneseScenarios = LingoSphere3Catalog.immersionScenariosForLanguage("Japanese")
    val frenchScenarios = LingoSphere3Catalog.immersionScenariosForLanguage("French")
    val englishScenarios = LingoSphere3Catalog.immersionScenariosForLanguage("English")

    assertTrue(japaneseScenarios.any { it.cityAndCountry.contains("Tokyo") })
    assertTrue(frenchScenarios.any { it.cityAndCountry.contains("Paris") })
    assertTrue(englishScenarios.any { it.cityAndCountry.contains("London") })

    val parisScenario = frenchScenarios.first()
    val dynamicResponse = GeminiTutorService.advanceImmersionScenario(
      scenario = parisScenario,
      dialogueHistory = listOf(
        ImmersionDialogueTurn(
          speaker = "character",
          targetLanguageText = parisScenario.openingLineInTargetLang
        )
      ),
      userUtteranceInTargetLang = "Bonjour Camille, je voudrais la formule du midi en terrasse, s'il vous plaît."
    )

    assertTrue(dynamicResponse.characterReplyInTargetLang.isNotBlank())
    assertTrue(dynamicResponse.nextDecisionOptionsInTargetLang.isNotEmpty())
    assertTrue(dynamicResponse.goalProgressPercent > 20)
  }

  @Test
  fun capability5_intelligentMistakeRecovery_tracksRepeatedMistakesAndMeasuresImprovement() = runBlocking {
    val memoryManager = LearningMemoryAndOfflineManager(context)
    memoryManager.recordOrIncrementRepeatedMistake(
      category = "Gustar Construction",
      ruleSummary = "Gustar uses indirect object pronouns (me/te/le).",
      exampleWrong = "Yo gusto el café",
      exampleCorrected = "Me gusta el café"
    )
    memoryManager.recordOrIncrementRepeatedMistake(
      category = "Gustar Construction",
      ruleSummary = "Gustar uses indirect object pronouns (me/te/le).",
      exampleWrong = "Yo gusto el café",
      exampleCorrected = "Me gusta el café"
    )

    val beforeList = memoryManager.loadRepeatedMistakeSummaries()
    val targetMistake = beforeList.first { it.exampleCorrected == "Me gusta el café" }
    assertTrue(targetMistake.occurrenceCount >= 2)

    val exercises = GeminiTutorService.generateMistakeRecoveryExercises(
      mistakes = listOf(targetMistake),
      targetLanguage = "Spanish"
    )
    assertTrue(exercises.isNotEmpty())

    // Record successful recovery attempts to verify mastered/improved tracking
    memoryManager.recordMistakeRecoveryProgress(targetMistake.id, scoreEarned = 92)
    val updatedMistake = memoryManager.recordMistakeRecoveryProgress(targetMistake.id, scoreEarned = 95)
    assertNotNull(updatedMistake)
    assertTrue(updatedMistake!!.masteryScore >= 75)
    assertTrue(updatedMistake.improved)
  }

  @Test
  fun capability6_dailyLifeLanguageMissions_evaluatesRealSpokenOrWrittenResponses() = runBlocking {
    val missions = LingoSphere3Catalog.dailyLifeMissionsForLanguage("Spanish", "B1")
    assertEquals(5, missions.size)
    val introMission = missions.first()

    val evaluation = GeminiTutorService.evaluateDailyLifeMissionResponse(
      mission = introMission,
      userResponseText = "Hola, me llamo Alex y vivo en Madrid. Estudio español todos los días porque trabajo con clientes internacionales y me encanta viajar por España.",
      submissionMode = "Spoken"
    )

    assertTrue(evaluation.completedSuccessfully)
    assertTrue(evaluation.fluencyAndCoherenceScore >= 60)
    assertTrue(evaluation.improvedModelResponse.isNotBlank())
  }

  @Test
  fun capability7_aiConversationReplay_savesTranscriptsAndUpdatesRetryScore() {
    val memoryManager = LearningMemoryAndOfflineManager(context)
    val session = ConversationReplaySession(
      id = "rep_test_1",
      scenarioTitle = "Ordering Tapas in Madrid",
      targetLanguage = "Spanish",
      cefrLevel = "B1",
      timestampLabel = "Today",
      overallScore = 81,
      turns = listOf(
        ReplayTurnItem(
          turnId = "t_user_1",
          speaker = "user",
          originalText = "Quiero pedir patatas bravas",
          improvedNativeVersion = "¿Nos trae una ración de patatas bravas, por favor?",
          pronunciationIpaOrPhonetic = "nos TRA-eh OO-na ra-SYON",
          correctionExplanation = "Polite plural request in Madrid tapas bars.",
          initialScore = 78
        )
      )
    )
    memoryManager.saveConversationReplaySession(session)
    val updated = memoryManager.updateReplayTurnPracticeScore(
      sessionId = "rep_test_1",
      turnId = "t_user_1",
      practicedTranscript = "¿Nos trae una ración de patatas bravas, por favor?",
      newScore = 96,
      targetLanguage = "Spanish"
    )

    val updatedTurn = updated.first { it.id == "rep_test_1" }.turns.first()
    assertEquals(96, updatedTurn.practicedRetryScore)
  }

  @Test
  fun capability8_accentUnderstandingStudio_providesRegionalAccentsWithoutSingleStandardBias() {
    val spanishAccents = LingoSphere3Catalog.regionalAccentsForLanguage("Spanish")
    assertTrue(spanishAccents.size >= 3)
    assertTrue(spanishAccents.any { it.regionName.contains("Madrid") })
    assertTrue(spanishAccents.any { it.regionName.contains("Mexico") })
    assertTrue(spanishAccents.any { it.regionName.contains("Buenos Aires") })
    assertTrue(spanishAccents.all { it.regionalVocabulary.isNotEmpty() })

    val speechManager = SpeechManager(context)
    speechManager.setSpeechRate(0.75f)
    assertEquals(0.75f, speechManager.speechRate.value)
    speechManager.release()
  }

  @Test
  fun capability9_personalLanguagePortfolio_exportsWavComparesRecordingsAndSupportsPrivacyAndDeletion() {
    val speechManager = SpeechManager(context)
    val memoryManager = LearningMemoryAndOfflineManager(context)

    val wavPath = speechManager.exportLastRecordingToPortfolio("sample_test_wav")
    assertNotNull(wavPath)
    val wavFile = File(wavPath!!)
    assertTrue(wavFile.exists())
    assertTrue(wavFile.length() > 44L)

    val sample = PortfolioSpeakingSample(
      id = "sample_test_wav",
      title = "Speaking Sample 1",
      targetLanguage = "Spanish",
      cefrLevel = "B1",
      spokenTranscript = "Hola desde mi portafolio de idiomas",
      improvedVersion = "¡Hola desde mi portafolio personal de idiomas!",
      pronunciationScore = 90,
      fluencyScore = 88,
      durationSeconds = 15,
      recordedAtEpochMs = System.currentTimeMillis(),
      dateLabel = "Today",
      audioFilePath = wavPath,
      isPrivate = true
    )
    memoryManager.savePortfolioSpeakingSample(sample)

    // Toggle privacy
    val toggled = memoryManager.togglePortfolioSpeakingSamplePrivacy("sample_test_wav", "Spanish")
    assertFalse(toggled.first { it.id == "sample_test_wav" }.isPrivate)

    // Save privacy settings
    val settings = PortfolioPrivacySettings(
      defaultPrivateRecordings = true,
      allowProgressSharingInCommunity = false,
      storeAudioOnDeviceOnly = true
    )
    memoryManager.savePortfolioPrivacySettings(settings)
    assertTrue(memoryManager.loadPortfolioPrivacySettings().storeAudioOnDeviceOnly)

    // Delete stored recording file and metadata
    val deletedFile = speechManager.deletePortfolioRecordingFile(wavPath)
    assertTrue(deletedFile)
    val afterDelete = memoryManager.deletePortfolioSpeakingSample("sample_test_wav", "Spanish")
    assertTrue(afterDelete.none { it.id == "sample_test_wav" })
    speechManager.release()
  }

  @Test
  fun capability10_adaptiveLearningEngine_calibratesDifficultyAndSkipsMasteredTopics() {
    val plan = AdaptiveLearningEngine.computeAdaptivePlan(
      targetLanguage = "Spanish",
      currentLevel = "B1",
      completedLessonsCount = 12,
      averageQuizScore = 91,
      averagePronunciationScore = 71,
      averageFluencyScore = 85,
      dueSrsCount = 14,
      masteredVocabularyCount = 45,
      unresolvedMistakes = emptyList(),
      weakVocabulary = listOf("subjuntivo", "sin embargo")
    )

    assertTrue(plan.recommendedCefrDifficulty in listOf("B1", "B2"))
    assertTrue(plan.weakestSkill.isNotBlank())
    assertTrue(plan.reviewFrequencyLabel.isNotBlank())
    assertTrue(plan.skippedMasteredTopics.isNotEmpty())
    assertTrue(plan.prioritizedNextActions.isNotEmpty())
  }
}
