package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiCostAndQuotaManager
import com.example.ai.GeminiModelTier
import com.example.ai.GeminiTutorService
import com.example.data.model.AiConversationPartner
import com.example.data.model.CallMode
import com.example.data.model.CorrectionMode
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.data.repository.DynamicAiGameEngine
import com.example.data.repository.LevelProgressionBand
import com.example.speech.SpeechManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiIntegrationVerificationTest {

  private lateinit var context: Context
  private val spanish = Language.findByCode("es")
  private val english = Language.findByCode("en")
  private val capturedPromptsAndModels = mutableListOf<Pair<String, String>>()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    GeminiCostAndQuotaManager.initialize(
      context = context,
      userId = "test_verifier",
      subscriptionTier = "PREMIUM_PRO"
    )
    capturedPromptsAndModels.clear()

    val debugIntent = Intent().apply {
      putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", "test-verified-appcheck-debug-token-2026")
    }
    configureAppCheck(context, debugIntent)
  }

  @After
  fun tearDown() {
    GeminiTutorService.testResponseInterceptor = null
    GeminiTutorService.testSimulateGeminiFailure = false
    com.example.ai.AiProviderManager.resetStateForTesting()
    com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
  }

  @Test
  fun verifyFirebaseAppCheckAndSupportedGeminiModels() = runBlocking {
    try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      assertFalse("FirebaseApp must be initialized", FirebaseApp.getApps(context).isEmpty())

      // Verify App Check debug secret was persisted synchronously for DebugAppCheckProviderFactory
      val prefsName = "com.google.firebase.appcheck.debug.store.${FirebaseApp.getInstance().persistenceKey}"
      val storedSecret = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        .getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null)
      assertEquals("test-verified-appcheck-debug-token-2026", storedSecret)

      // Verify configured Gemini model IDs match supported Firebase AI Logic models
      assertEquals("gemini-3.8-flash", GeminiModelTier.FLASH_STANDARD.modelId)
      assertEquals("gemini-3.5-flash-lite", GeminiModelTier.FLASH_LITE.modelId)
      assertEquals("gemini-3.1-flash-tts-preview", GeminiTutorService.apiHealthStatus.value.configuredTtsModelId)

      // Verify Firebase.ai generativeModel instances initialize cleanly for all 3 models
      val flashStandardModel = Firebase.ai.generativeModel(GeminiModelTier.FLASH_STANDARD.modelId)
      val flashLiteModel = Firebase.ai.generativeModel(GeminiModelTier.FLASH_LITE.modelId)
      val flashTtsModel = Firebase.ai.generativeModel("gemini-3.1-flash-tts-preview")
      assertNotNull(flashStandardModel)
      assertNotNull(flashLiteModel)
      assertNotNull(flashTtsModel)

      // Verify no API key is exposed in BuildConfig or .env.example
      val buildConfigFields = BuildConfig::class.java.declaredFields.map { it.name }
      assertFalse("BuildConfig must not expose GEMINI_API_KEY", buildConfigFields.contains("GEMINI_API_KEY"))
      assertFalse("BuildConfig must not expose OPENAI_API_KEY", buildConfigFields.contains("OPENAI_API_KEY"))
      val envExample = File("../.env.example")
      if (envExample.exists()) {
        val envText = envExample.readText()
        assertFalse(".env.example must not declare GEMINI_API_KEY", envText.contains("GEMINI_API_KEY"))
        assertFalse(".env.example must not declare OPENAI_API_KEY", envText.contains("OPENAI_API_KEY"))
      }

      // Verify health probe updates GeminiApiHealthStatus
      GeminiTutorService.testResponseInterceptor = { prompt, modelId ->
        capturedPromptsAndModels.add(prompt to modelId)
        "READY"
      }
      val status = GeminiTutorService.verifyGeminiConnection()
      assertTrue(status.isInitialized)
      assertTrue(status.lastCallSucceeded)
      assertTrue(status.totalSuccessfulCalls >= 1)
    } catch (t: Throwable) {
      throw AssertionError("Failed in verifyFirebaseAppCheckAndSupportedGeminiModels: ${t.javaClass.name}: ${t.message}", t)
    }
  }

  @Test
  fun verifyAllTenAiFeaturesConnectedEndToEnd() = runBlocking {
    com.example.data.repository.AccessPolicyManager.updateEntitlement(
      com.example.data.repository.ServerEntitlementRecord(
        uid = "test_verifier",
        status = com.example.data.repository.EntitlementStatus.PRO_ACTIVE,
        productId = com.example.data.model.SubscriptionPlan.PRODUCT_ID_PRO,
        basePlanId = com.example.data.model.SubscriptionPlan.BASE_PLAN_MONTHLY,
        verifiedWithPlayApi = true,
        expiryEpochMs = System.currentTimeMillis() + 30L * 24L * 3600L * 1000L
      )
    )
    GeminiTutorService.testResponseInterceptor = { prompt, modelId ->
      capturedPromptsAndModels.add(prompt to modelId)
      when {
        prompt.contains("HUMAN TEACHER RULES") || prompt.contains("LIVE REAL-TIME CONVERSATION") -> """
          REPLY: ¡Qué buena idea! Viajar por Madrid en primavera es maravilloso. ¿Qué museos quieres visitar?
          PHONETICS: keh bweh-nah ee-deh-ah! byah-har por mah-dreed
          TRANSLATION: What a great idea! Traveling through Madrid in spring is wonderful. Which museums do you want to visit?
          CORRECTION: Tip: Use 'por Madrid' when talking about traveling around the city.
          NATURAL: Me gustaría recorrer el centro histórico de Madrid.
        """.trimIndent()

        prompt.contains("STRICT RESPONSE FORMAT") && prompt.contains("REPLY:") -> """
          REPLY: ¡Hola! Me encantaría ayudarte a pedir en el restaurante hoy. ¿Qué te gustaría tomar de primero?
          PHONETICS: oh-lah! meh en-kan-tah-ree-ah ah-yoo-dar-teh ah peh-deer
          TRANSLATION: Hello! I would love to help you order at the restaurant today. What would you like for your starter?
          CORRECTION: Great greeting! Remember to use 'Quisiera' for polite restaurant requests.
        """.trimIndent()

        prompt.contains("ACHIEVED:") && prompt.contains("UPGRADES:") -> """
          ACHIEVED: YES
          SCORE: 94
          FEEDBACK: Polite ordering expressions and clear pronunciation of menu items.
          NOTES: Good polite forms; Clear food vocabulary
          UPGRADES: Me gustaría pedir la especialidad; ¿Nos trae la cuenta, por favor?
        """.trimIndent()

        prompt.contains("LESSON_TITLE:") -> """
          LESSON_TITLE: Navigating High-Speed Rail in Spain
          SUMMARY: Master ticket booking, platform inquiries, and seat confirmations at Atocha Station.
          GRAMMAR_TIP: Use '¿A qué hora sale el tren hacia...?' to ask for departure times.
          EX1: MULTIPLE_CHOICE | How do you ask which platform the train leaves from? | ¿De qué andén sale el tren? | ¿De qué andén sale el tren? | ¿Dónde está la biblioteca? | ¿Cuánto cuesta el taxi? | ¿Tiene mesa libre? | 'Andén' means platform in Spanish rail stations.
          EX2: TRANSLATE | Billete de ida y vuelta | Round-trip ticket | Round-trip ticket | One-way flight | Baggage claim | Hotel reservation | 'Ida y vuelta' means round-trip.
          EX3: FILL_BLANK | El tren de alta velocidad ___ a las ocho en punto. | sale | sale | comen | vivimos | fui | 'Salir' in 3rd person singular present is 'sale'.
          EX4: SPEAK | Quisiera reservar un asiento junto a la ventana | I would like to reserve a window seat | kee-SYEH-rah reh-ser-VAR oon ah-SYEN-toh | Polite seat reservation phrase.
        """.trimIndent()

        prompt.contains("WORD:") && prompt.contains("Extract up to 3 useful vocabulary") -> """
          WORD: el itinerario | the itinerary | el ee-tee-neh-RAH-ryoh
          WORD: hacer escala | to have a layover | ah-SER es-KAH-lah
          WORD: el equipaje de mano | carry-on luggage | el eh-kee-PAH-heh deh MAH-noh
        """.trimIndent()

        prompt.contains("VOCAB_WORD:") || prompt.contains("Vocabulary Builder") -> """
          WORD: imprescindible | eem-pres-seen-DEE-bleh | essential / indispensable | Adjective | B1 | Travel & Work | El pasaporte es imprescindible para viajar. | A passport is essential for traveling. | Syn: esencial; Ant: opcional
          WORD: desarrollar | deh-sahr-roh-YAHR | to develop | Verb | B1 | Career | Queremos desarrollar un proyecto nuevo. | We want to develop a new project. | Used in professional and personal growth contexts.
        """.trimIndent()

        prompt.contains("CORRECTED:") && prompt.contains("CEFR:") -> """
          CORRECTED: Ayer fui al mercado con mis amigos y compré frutas frescas para la cena.
          CEFR: B1
          SCORE: 91
          SUMMARY: Strong narrative flow! Remember that 'ir' in the preterite first-person singular is 'fui'.
          ITEM: yo fue | fui | Verb Tense | ERROR | Irregular preterite tense of 'ir' (to go) for 'yo' is 'fui'.
          NATURAL: Ayer pasé por el mercado con unos amigos
          DRILL_Q: Complete the sentence: Ayer yo ___ (ir) al cine.
          DRILL_A: fui
        """.trimIndent()

        prompt.contains("PARAGRAPH:") && prompt.contains("GLOSS:") -> """
          TITLE: El Café Literario de Madrid
          PARAGRAPH: En el corazón de Madrid existe un antiguo café donde escritores y viajeros conversan cada tarde mientras disfrutan de un chocolate caliente. | In the heart of Madrid there is an old café where writers and travelers chat every afternoon while enjoying hot chocolate.
          GLOSS: antiguo | old / historic | Adjective | Un edificio antiguo en el centro.
          GLOSS: siglo | century | Noun | Más de un siglo de historia.
          QUESTION: ¿Qué toman los visitantes mientras conversan en el café? | Un chocolate caliente | Un té frío | Un zumo de naranja | Agua con gas | The passage states they enjoy 'un chocolate caliente'.
        """.trimIndent()

        prompt.contains("ACCENTS:") && prompt.contains("LINE:") -> """
          TITLE: Reservando una Habitación con Vista al Mar
          CATEGORY: Travel & Hotel
          CEFR: B1
          ACCENTS: Madrid Castilian, Barcelona Coastal, Mexican Standard
          LINE: Recepcionista | ¡Buenas tardes! Bienvenido al Hotel Mediterráneo. ¿Tiene una reserva? | Good afternoon! Welcome to Hotel Mediterráneo. Do you have a reservation? | reserva | reservation
          LINE: Viajero | Sí, tengo una reserva a nombre de Carlos para tres noches. | Yes, I have a reservation under the name Carlos for three nights. | tres noches | three nights
          QUESTION: ¿Por cuántas noches es la reserva de Carlos? | Tres noches | Una noche | Cinco noches | Dos semanas | Carlos confirms his reservation is for 'tres noches'.
        """.trimIndent()

        prompt.contains("Create a 4-question multiple-choice quiz") -> """
          Q: Which Spanish phrase politely asks for the restaurant bill? | La cuenta, por favor | La maleta, por favor | El tren sale ahora | Hace mucho frío | 'La cuenta, por favor' is the standard polite way to request the check.
          Q: What does 'el andén' mean at a Spanish train station? | The platform | The suitcase | The passport | The pharmacy | Trains depart from 'el andén' (the platform).
          Q: Choose the correct preterite form: 'Ayer yo ___ al museo.' | fui | voy | iré | iba | 'Fui' is the first-person singular preterite of 'ir'.
          Q: How do you ask someone to speak more slowly in Spanish? | ¿Puede hablar más despacio, por favor? | ¿Dónde está la salida? | ¿A qué hora abre? | Muchas gracias por todo | 'Más despacio' means 'more slowly'.
        """.trimIndent()

        prompt.contains("ACCURACY:") && prompt.contains("IPA:") -> """
          ACCURACY: 93
          IPA: /ki.ˈsje.ɾa u.na ˈme.sa/
          STRESS: qui-SIE-ra u-na ME-sa
          PROBLEMS: Rolled/tapped r in quisiera
          MOUTH_TIP: Lightly tap the tip of your tongue against the alveolar ridge for the single 'r' in 'quisiera'.
          INTONATION: Keep a smooth, polite rising-falling melody across the request.
          ACCENT_NOTE: Standard Castilian and Latin American Spanish share clear vowel timing here.
          WORD: Quisiera | MATCH | Clear diphthong and stress
          WORD: una | MATCH | Accurate vowel quality
          WORD: mesa | MATCH | Crisp initial nasal and open vowel
        """.trimIndent()

        else -> """
          [{"englishConcept":"coffee cup","imageCategory":"Café & Dining","visualConceptId":"","prompt":"Select the Spanish word for a warm cup of coffee","targetPhrase":"el café","translation":"coffee","phonetic":"el kah-FEH","correctOption":"el café","options":["el café","la cuchara","el billete","la ventana"],"hint":"Popular morning drink","explanation":"'El café' means coffee in Spanish."}]
        """.trimIndent()
      }
    }

    // 1. AI Tutor Conversation
    val tutorReply = GeminiTutorService.chatWithTutor(
      userSpeechInput = "Hola, quiero pedir comida en el restaurante.",
      targetLanguage = spanish,
      nativeLanguage = english,
      proficiencyLevel = "Intermediate (B1)",
      scenarioTitle = "Ordering at a Madrid Tapas Restaurant",
      partnerName = "Sofia"
    )
    assertTrue(tutorReply.replyInTargetLanguage.contains("restaurante"))
    assertTrue(tutorReply.translationInNativeLanguage.contains("restaurant"))
    assertNotNull(tutorReply.grammarFeedback)

    // 2. Speaking (Real-Time Voice Call)
    val voiceCallReply = GeminiTutorService.chatInVoiceCall(
      userSpeechInput = "Me encanta viajar a Madrid en primavera.",
      targetLanguage = spanish,
      nativeLanguage = english,
      proficiencyLevel = "Intermediate (B1)",
      mode = CallMode.FREE_CONVERSATION,
      tutorVoice = TutorVoice.SULAFAT,
      history = emptyList(),
      partner = AiConversationPartner.ALL_PARTNERS.first(),
      correctionMode = CorrectionMode.IMMEDIATE,
      cefrLevel = "B1"
    )
    assertTrue(voiceCallReply.replyInTargetLanguage.contains("Madrid"))
    assertTrue(voiceCallReply.translationInNativeLanguage.contains("Madrid"))

    // 3. Roleplay Evaluation
    val roleplayReport = GeminiTutorService.evaluateRoleplayCompletion(
      scenarioId = "rp_cafe_1",
      scenarioTitle = "Ordering Tapas in Madrid",
      taskObjective = "Order a starter, main dish, and ask for the bill",
      userTurns = listOf("Hola, quisiera pedir paella", "La cuenta, por favor"),
      targetLanguage = spanish,
      nativeLanguage = english
    )
    assertTrue(roleplayReport.objectiveAchieved)
    assertTrue(roleplayReport.scorePercent >= 85)
    assertTrue(roleplayReport.taskFeedback.isNotBlank())

    // 4. Learn (Personalized Skill Lesson)
    val skillLesson = GeminiTutorService.generateAiSkillLesson(
      targetLanguage = spanish,
      nativeLanguage = english,
      cefrLevel = "B1",
      skillArea = "Travel & Transport",
      topic = "Navigating High-Speed Rail in Spain"
    )
    assertTrue(skillLesson.title.isNotBlank())
    assertTrue(skillLesson.exercises.isNotEmpty())

    // 5. Vocabulary (Vocabulary Builder + Chat Vocab Extraction)
    val vocabWords = GeminiTutorService.generateVocabularyBuilderWords(
      targetLanguage = spanish,
      nativeLanguage = english,
      cefrLevel = "B1",
      topic = "Travel & Work"
    )
    assertTrue(vocabWords.isNotEmpty())
    val extractedVocab = GeminiTutorService.extractChatMessageVocab(
      text = "Necesito revisar el itinerario y llevar el equipaje de mano.",
      targetLanguage = spanish,
      nativeLanguage = english
    )
    assertEquals(3, extractedVocab.size)

    // 6. Grammar and Writing
    val writingEval = GeminiTutorService.evaluateWritingAndGrammar(
      userText = "Ayer yo fue al mercado con mis amigos y compré frutas.",
      writingFormat = "Paragraph",
      targetLanguage = spanish,
      nativeLanguage = english,
      proficiencyLevel = "B1"
    )
    assertTrue(writingEval.correctedText.contains("fui"))
    assertTrue(writingEval.overallScore >= 80)
    assertTrue(writingEval.corrections.isNotEmpty())

    // 7. Reading
    val readingPassage = GeminiTutorService.generateAiReadingPassage(
      targetLanguage = spanish,
      nativeLanguage = english,
      cefrLevel = "B1",
      category = "Culture & Stories"
    )
    assertTrue(readingPassage.title.isNotBlank())
    assertTrue(readingPassage.paragraphs.isNotEmpty())
    assertTrue(readingPassage.questions.isNotEmpty())

    // 8. Listening
    val listeningItem = GeminiTutorService.generateAiListeningDialogue(
      targetLanguage = spanish,
      nativeLanguage = english,
      cefrLevel = "B1",
      category = "Travel & Hotel"
    )
    assertTrue(listeningItem.title.isNotBlank())
    assertTrue(listeningItem.lines.isNotEmpty())
    assertTrue(listeningItem.questions.isNotEmpty())

    // 9. Games (Unique Dynamic Questions & 500-Level Difficulty Progression)
    val dynamicQuiz = GeminiTutorService.generateDynamicQuiz(
      targetLanguage = spanish,
      nativeLanguage = english,
      proficiencyLevel = "B1",
      topic = "Train Station & Dining"
    )
    assertEquals(4, dynamicQuiz.size)
    assertEquals(4, dynamicQuiz.map { it.question }.distinct().size)

    val game = DynamicAiGameEngine.ALL_70_GAMES.first()
    val level1Band = LevelProgressionBand.forLevel(1)
    val level300Band = LevelProgressionBand.forLevel(300)
    assertEquals("A1-", level1Band.cefrEquivalent)
    assertEquals("B1", level300Band.cefrEquivalent)

    val level1Exercises = DynamicAiGameEngine.generateGameQuestions(
      context = context,
      game = game,
      languageCode = "es",
      nativeLanguageCode = "en",
      levelNumber = 1,
      forceRefresh = true,
      userId = "gemini_verifier"
    )
    assertTrue("Game engine must produce validated exercises", level1Exercises.isNotEmpty())
    val uniqueTargetCount = level1Exercises.map { ex -> ex.targetPhrase.lowercase() }.distinct().size
    assertEquals(
      "Exercises within a level must have unique target phrases",
      level1Exercises.size,
      uniqueTargetCount
    )

    // 10. Pronunciation
    val pronDiag = GeminiTutorService.analyzePronunciationDetailed(
      targetPhrase = "Quisiera una mesa",
      recognizedSpeech = "Quisiera una mesa",
      targetLanguage = spanish,
      nativeLanguage = english
    )
    assertTrue(pronDiag.accuracyPercent >= 90)
    assertTrue(pronDiag.ipaTranscription.isNotBlank())
    assertTrue(pronDiag.wordBreakdown.isNotEmpty())

    // Verify both Gemini model tiers were routed appropriately
    val usedModels = capturedPromptsAndModels.map { it.second }.toSet()
    assertTrue("Must route live voice/tutor calls to gemini-3.8-flash", usedModels.contains("gemini-3.8-flash"))
    assertTrue("Must route structured lessons/quizzes to gemini-3.5-flash-lite", usedModels.contains("gemini-3.5-flash-lite"))
  }

  @Test
  fun verifyVoiceFunctionalityAndDistinctVoices() = runBlocking {
    val speechManager = SpeechManager(context)
    try {
      // Verify all 15 TutorVoice profiles and both Female and Male voice options are preserved
      val allVoices = TutorVoice.entries
      assertEquals(15, allVoices.size)
      val femaleVoices = allVoices.filter { it.gender.equals("Female", ignoreCase = true) }
      val maleVoices = allVoices.filter { it.gender.equals("Male", ignoreCase = true) }
      assertTrue("Female tutor voices must be preserved", femaleVoices.size >= 7)
      assertTrue("Male tutor voices must be preserved", maleVoices.size >= 7)

      val distinctSignatures = mutableSetOf<String>()
      val mappedOpenAiVoices = mutableSetOf<String>()
      allVoices.forEach { voice ->
        speechManager.setPartnerVoice(voice)
        assertEquals(voice, speechManager.partnerVoice.value)
        assertTrue(voice.providerVoiceId.isNotBlank())
        assertTrue(voice.openAiVoiceId.isNotBlank())
        distinctSignatures.add(voice.distinctVocalSignature)
        mappedOpenAiVoices.add(voice.openAiVoiceId)
      }
      assertEquals(
        "Every TutorVoice selection must produce a distinct vocal signature",
        15,
        distinctSignatures.size
      )
      assertTrue(
        "Multiple OpenAI Realtime voice personas must be mapped across TutorVoice selections",
        mappedOpenAiVoices.size >= 6
      )

      // Verify spoken input recognition pipeline emits transcript and forwards to AI Tutor
      var callbackTranscript: String? = null
      speechManager.setCallSpeechCallbacks(
        onPartial = null,
        onResult = { transcript -> callbackTranscript = transcript },
        onSpeechStarted = null
      )
      speechManager.emitRecognizedSpeech("Buenos días, ¿cómo estás hoy?")
      assertEquals("Buenos días, ¿cómo estás hoy?", speechManager.spokenText.value)
      assertEquals("Buenos días, ¿cómo estás hoy?", callbackTranscript)

      // Verify non-preview TTS failure completes cleanly without leaving isSpeaking stuck true
      var doneInvoked = false
      speechManager.testSimulateTtsFailure = true
      speechManager.speakWithTutorVoice(
        text = "Hola, bienvenido al restaurante.",
        voice = TutorVoice.AOEDE,
        languageCode = "es",
        onDone = { doneInvoked = true }
      )
      org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      assertFalse("isSpeaking must reset cleanly when TTS engine unavailable", speechManager.isSpeaking.value)
      assertTrue("onDone callback must still invoke cleanly", doneInvoked)

      // Verify honest service error is returned (no fake static demo replies) when all cloud AI providers are unavailable
      GeminiTutorService.testSimulateGeminiFailure = true
      GeminiTutorService.testResponseInterceptor = null
      com.example.ai.AiProviderManager.openAiBackendTestInterceptor = null
      val failedTurn = GeminiTutorService.chatWithTutor(
        userSpeechInput = "Hola, quiero pedir paella por favor.",
        targetLanguage = spanish,
        nativeLanguage = english,
        proficiencyLevel = "B1",
        scenarioTitle = "Ordering at a Restaurant",
        conversationHistory = listOf("USER" to "Hola, quiero pedir paella por favor.")
      )
      assertTrue("Must surface honest isServiceError when AI providers fail", failedTurn.isServiceError)
      assertTrue("Must include actionable error message", !failedTurn.errorMessage.isNullOrBlank())
      assertTrue("Must not spoof a fake AI reply when unavailable", failedTurn.replyInTargetLanguage.isBlank())
    } finally {
      speechManager.testSimulateTtsFailure = false
      speechManager.release()
    }
  }

  @Test
  fun verifyOpenAiResponsesAndRealtimeVoiceAndFailoverWithContext() = runBlocking {
    com.example.ai.AiProviderManager.resetStateForTesting()

    // 1. Verify OpenAI text tutoring succeeds through the secure backend (/openAiTutorResponse)
    var openAiCalledCount = 0
    var geminiCalledCount = 0
    var capturedHistoryCountInOpenAi = 0

    com.example.ai.AiProviderManager.openAiBackendTestInterceptor = { path, json, _, _ ->
      if (path == "/openAiTutorResponse") {
        openAiCalledCount++
        val historyArr = json.optJSONArray("conversationHistory")
        capturedHistoryCountInOpenAi = historyArr?.length() ?: 0
        org.json.JSONObject().apply {
          put("provider", "openai")
          put("model", "gpt-4o-mini")
          put(
            "outputText",
            """
            REPLY: Good try! Say: Yesterday I went to the market. What did you buy?
            PHONETICS: good try! say: YES-ter-day eye went too dhuh MAR-kit. wut did yoo buy?
            TRANSLATION: Good try! Say: Yesterday I went to the market. What did you buy?
            CORRECTION: Use the past tense 'went' (instead of 'go') with 'yesterday', and include 'the' before 'market'.
            """.trimIndent()
          )
        }
      } else if (path == "/createOpenAiRealtimeSession") {
        org.json.JSONObject().apply {
          put("provider", "openai-realtime")
          put("sessionId", "sess_realtime_test_01")
          put("model", "gpt-4o-mini-realtime-preview")
          put("voice", json.optString("voice", "coral"))
          put("ephemeralKey", "ek_test_short_lived_secret_998877")
          put("expiresAt", (System.currentTimeMillis() / 1000L) + 60L)
          put("webrtcUrl", "https://api.openai.com/v1/realtime")
        }
      } else {
        null
      }
    }

    GeminiTutorService.testResponseInterceptor = { _, _ ->
      geminiCalledCount++
      "REPLY: Gemini backup response"
    }

    val history = listOf(
      "USER" to "Hi Emma, I want to talk about my weekend.",
      "TUTOR" to "I'd love to hear about it! What did you do?"
    )

    val tutorResponse = GeminiTutorService.chatWithTutor(
      userSpeechInput = "Yesterday I go to market.",
      targetLanguage = english,
      nativeLanguage = english,
      proficiencyLevel = "B1",
      conversationHistory = history,
      partnerName = "Emma"
    )

    assertEquals("OpenAI primary must be called once without simultaneous Gemini call", 1, openAiCalledCount)
    assertEquals("Gemini must NOT be called simultaneously when OpenAI succeeds", 0, geminiCalledCount)
    assertEquals("Full conversation context must be passed to OpenAI", 2, capturedHistoryCountInOpenAi)
    assertEquals(
      "Good try! Say: Yesterday I went to the market. What did you buy?",
      tutorResponse.replyInTargetLanguage
    )
    assertTrue(tutorResponse.grammarFeedback.orEmpty().contains("went"))

    // 2. Verify OpenAI Realtime voice session & WebRTC SDP handshake + interruption events
    val realtimeSession = com.example.ai.AiProviderManager.createOpenAiRealtimeVoiceSession(
      tutorVoice = TutorVoice.AOEDE,
      targetLanguage = english,
      nativeLanguage = spanish,
      cefrLevel = "B1",
      partnerName = "Emma",
      scenarioTitle = "Shopping Roleplay"
    )
    assertNotNull("Realtime session config must be created via backend", realtimeSession)
    assertEquals("coral", realtimeSession!!.voice)
    assertEquals("ek_test_short_lived_secret_998877", realtimeSession.ephemeralClientSecret)

    com.example.ai.AiProviderManager.openAiWebRtcSdpTestInterceptor = { url, ephemeralKey, sdpOffer ->
      assertTrue(url.contains("https://api.openai.com/v1/realtime"))
      assertEquals("ek_test_short_lived_secret_998877", ephemeralKey)
      assertTrue(sdpOffer.contains("v=0"))
      "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\ns=OpenAI Realtime Answer\r\n"
    }
    val sdpAnswer = com.example.ai.AiProviderManager.exchangeRealtimeWebRtcSdpOffer(
      sessionConfig = realtimeSession,
      sdpOffer = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=LingoSphere Offer\r\n"
    )
    assertNotNull(sdpAnswer)
    assertTrue(sdpAnswer!!.contains("OpenAI Realtime Answer"))

    val interruptionEvents = com.example.ai.AiProviderManager.buildRealtimeInterruptionEvents()
    assertEquals(2, interruptionEvents.size)
    assertTrue(interruptionEvents[0].contains("response.cancel"))
    assertTrue(interruptionEvents[1].contains("output_audio_buffer.clear"))

    // 3. Verify OpenAI failure -> fails over to Gemini while preserving conversation context
    openAiCalledCount = 0
    geminiCalledCount = 0
    var capturedGeminiPrompt = ""
    com.example.ai.AiProviderManager.openAiBackendTestInterceptor = { _, _, _, _ ->
      openAiCalledCount++
      throw IllegalStateException("Simulated OpenAI 503 Unavailable")
    }
    GeminiTutorService.testResponseInterceptor = { prompt, _ ->
      geminiCalledCount++
      capturedGeminiPrompt = prompt
      """
      REPLY: Good try! Say: Yesterday I went to the market. What did you buy?
      PHONETICS: good try! say: YES-ter-day eye went too dhuh MAR-kit.
      TRANSLATION: Good try! Say: Yesterday I went to the market. What did you buy?
      CORRECTION: Use past tense 'went' with 'yesterday'.
      """.trimIndent()
    }

    val failoverResult = com.example.ai.AiProviderManager.executeWithIntelligentRouting(
      taskType = com.example.ai.AiTaskType.ADVANCED_AI_TUTOR,
      systemInstructionText = "You are a warm human tutor.",
      userPromptText = "Yesterday I go to market.",
      historyTurns = history,
      targetLanguage = "English",
      nativeLanguage = "English",
      cefrLevel = "B1"
    )
    assertNotNull(failoverResult)
    assertEquals(com.example.ai.AiProvider.GEMINI, failoverResult!!.providerUsed)
    assertTrue(failoverResult.switchedToFallbackProvider)
    assertEquals(com.example.ai.AiProvider.OPENAI, failoverResult.primaryProviderAttempted)
    assertEquals(2, failoverResult.preservedHistoryTurnCount)
    assertTrue("Gemini fallback prompt must preserve conversation history", capturedGeminiPrompt.contains("weekend"))

    // 4. Verify Gemini failure -> fails over to OpenAI when permitted while preserving conversation context
    openAiCalledCount = 0
    geminiCalledCount = 0
    GeminiTutorService.testResponseInterceptor = null
    GeminiTutorService.testSimulateGeminiFailure = true
    com.example.ai.AiProviderManager.openAiBackendTestInterceptor = { _, json, _, _ ->
      openAiCalledCount++
      val historyArr = json.optJSONArray("history")
      assertEquals(2, historyArr?.length() ?: 0)
      org.json.JSONObject().apply {
        put("provider", "openai")
        put("model", "gpt-4o-mini")
        put("text", "EX: Polite greeting | Hola | oh-lah | Hello | Bye | No | Wait | Greeting")
      }
    }

    com.example.ai.AiProviderManager.selectActiveProvider(com.example.ai.AiProvider.GEMINI)
    val geminiToOpenAiFailover = com.example.ai.AiProviderManager.executeWithIntelligentRouting(
      taskType = com.example.ai.AiTaskType.LESSON_GENERATION,
      systemInstructionText = "Generate a lesson.",
      userPromptText = "Create 1 exercise",
      historyTurns = history,
      targetLanguage = "Spanish",
      nativeLanguage = "English",
      cefrLevel = "A1"
    )
    com.example.ai.AiProviderManager.selectActiveProvider(com.example.ai.AiProvider.OPENAI)
    assertNotNull(geminiToOpenAiFailover)
    assertEquals(com.example.ai.AiProvider.OPENAI, geminiToOpenAiFailover!!.providerUsed)
    assertTrue(geminiToOpenAiFailover.switchedToFallbackProvider)
    assertEquals(com.example.ai.AiProvider.GEMINI, geminiToOpenAiFailover.primaryProviderAttempted)
    assertEquals(2, geminiToOpenAiFailover.preservedHistoryTurnCount)

    // 5. Verify deterministic human-like grammar correction for "Yesterday I go to market."
    val grammarAnalysis = com.example.ai.AiProviderManager.analyzeAndCorrectLearnerSentence(
      userMessage = "Yesterday I go to market.",
      targetLanguage = english,
      nativeLanguage = english
    )
    assertTrue(grammarAnalysis.hasGrammarError)
    assertEquals("Yesterday I went to the market.", grammarAnalysis.correctedSentence)
    assertEquals(
      "Good try! Say: Yesterday I went to the market. What did you buy?",
      grammarAnalysis.warmTutorReply
    )

    // 6. Verify Free (0 AI) vs Pro quota limits
    assertEquals(0, com.example.ai.AiProviderManager.getDailyOpenAiTextLimitForTier("free"))
    assertEquals(0, com.example.ai.AiProviderManager.getDailyOpenAiRealtimeLimitForTier("free"))
    assertTrue(
      com.example.ai.AiProviderManager.getDailyOpenAiTextLimitForTier("premium_pro") >
        com.example.ai.AiProviderManager.getDailyOpenAiTextLimitForTier("free")
    )
    assertTrue(
      com.example.ai.AiProviderManager.getDailyOpenAiRealtimeLimitForTier("premium_pro") >
        com.example.ai.AiProviderManager.getDailyOpenAiRealtimeLimitForTier("free")
    )
  }

  @Test
  fun verifyGoogleSignInOAuthConfigAndFlowDeduplication() {
    val resolvedClientId = com.example.ui.components.resolveServerClientId(context)
    assertNotNull("default_web_client_id must be present", resolvedClientId)
    assertEquals(
      "default_web_client_id must match the active Web OAuth 2.0 client in gen-lang-client-0662926032",
      "1072392835874-s1d1vuimntk12t0n9ca7hlsmgpvvd07f.apps.googleusercontent.com",
      resolvedClientId
    )
    assertFalse(
      "Stale web client ID causing 28444 must not be used",
      resolvedClientId == "1072392835874-0ul3s4al0c0e8tc8a4uv8topch3thgje.apps.googleusercontent.com"
    )

    val signingReport = com.example.ui.components.inspectApkSigningConfig(context)
    assertEquals("com.aistudio.lingosphere.kxpqzm", signingReport.packageName)
    assertTrue(signingReport.isWebClientConfigured)
    assertTrue(
      "Registered SHA-1 set must include the active debug.keystore SHA-1",
      com.example.ui.components.REGISTERED_OAUTH_SHA1_HASHES.contains(
        "8E:26:8B:6F:5E:FD:A8:12:22:38:AE:1F:F9:3B:EA:3B:40:33:81:64"
      )
    )

    // Verify both Continue with Google (GetSignInWithGoogleOption) and Continue with Gmail One-Tap (GetGoogleIdOption)
    // build single-option CredentialManager requests with the active Web Client ID
    val googleSignInOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
      .Builder(serverClientId = resolvedClientId!!)
      .build()
    val googleSignInRequest = androidx.credentials.GetCredentialRequest.Builder()
      .addCredentialOption(googleSignInOption)
      .build()
    assertEquals(1, googleSignInRequest.credentialOptions.size)
    assertEquals(resolvedClientId, googleSignInOption.serverClientId)

    val gmailOneTapOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(false)
      .setServerClientId(resolvedClientId)
      .setAutoSelectEnabled(true)
      .build()
    val gmailOneTapRequest = androidx.credentials.GetCredentialRequest.Builder()
      .addCredentialOption(gmailOneTapOption)
      .build()
    assertEquals(1, gmailOneTapRequest.credentialOptions.size)
    assertEquals(resolvedClientId, gmailOneTapOption.serverClientId)
    assertFalse(gmailOneTapOption.filterByAuthorizedAccounts)
    assertTrue(gmailOneTapOption.autoSelectEnabled)

    val prepayError = RuntimeException(
      "Your prepayment credits are depleted. Please go to AI Studio at https://ai.studio/projects to manage your project and billing. Learn more at https://ai.google.dev/gemini-api/docs/billing#prepay."
    )
    assertTrue(
      "Must detect prepayment credit depletion to prevent retry spam",
      GeminiTutorService.isProjectBillingOrQuotaDepletedError(prepayError)
    )
  }
}

