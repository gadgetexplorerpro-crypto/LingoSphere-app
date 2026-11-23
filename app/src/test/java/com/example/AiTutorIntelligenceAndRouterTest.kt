package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiProviderManager
import com.example.ai.GeminiCostAndQuotaManager
import com.example.ai.GeminiTutorService
import com.example.ai.TutorConversationMode
import com.example.data.model.AiConversationPartner
import com.example.data.model.CallMode
import com.example.data.model.CorrectionMode
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.TutorVoice
import com.example.data.model.VoiceCallTurn
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiTutorIntelligenceAndRouterTest {

  private lateinit var context: Context
  private val english = Language.findByCode("en")
  private val spanish = Language.findByCode("es")

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    GeminiCostAndQuotaManager.initialize(
      context = context,
      userId = "tutor_iq_verifier",
      subscriptionTier = "PREMIUM_PRO"
    )
    AiProviderManager.resetStateForTesting()
    GeminiTutorService.testResponseInterceptor = null
    GeminiTutorService.testSimulateGeminiFailure = false

    val debugIntent = Intent().apply {
      putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", "test-verified-appcheck-debug-token-2026")
    }
    configureAppCheck(context, debugIntent)
  }

  @After
  fun tearDown() {
    GeminiTutorService.testResponseInterceptor = null
    GeminiTutorService.testSimulateGeminiFailure = false
    AiProviderManager.resetStateForTesting()
  }

  @Test
  fun verifyUnifiedRouterModeSelectionAndFallbackBehavior() = runBlocking {
    val capturedPayloads = mutableListOf<Pair<String, JSONObject>>()

    AiProviderManager.openAiBackendTestInterceptor = { path, payload, _, _ ->
      capturedPayloads.add(path to JSONObject(payload.toString()))
      when (path) {
        "/aiTutorRouter" -> {
          val mode = payload.optString("taskType")
          val chosenProvider = if (mode == "LANGUAGE_LESSON") "gemini" else "openai"
          JSONObject().apply {
            put("provider", chosenProvider)
            put("model", if (chosenProvider == "gemini") "gemini-2.5-flash" else "gpt-4o-mini")
            put("failoverUsed", false)
            put(
              "outputText",
              """
              REPLY: I'm doing great, thank you! How are you today?
              PHONETICS: /aɪm ˈduːɪŋ ɡreɪt, θæŋk juː/
              TRANSLATION: I'm doing great, thank you! How are you today?
              CORRECTION: NONE
              """.trimIndent()
            )
          }
        }
        else -> null
      }
    }

    // 1. Free conversation should hit /aiTutorRouter with preferredProvider = "openai"
    val freeTurn = GeminiTutorService.chatWithTutor(
      userSpeechInput = "How are you?",
      targetLanguage = english,
      nativeLanguage = english,
      proficiencyLevel = "Beginner",
      conversationMode = TutorConversationMode.FREE_CONVERSATION
    )
    assertFalse(freeTurn.isServiceError)
    assertEquals("I'm doing great, thank you! How are you today?", freeTurn.replyInTargetLanguage)
    assertNull("Accurate question must not show a redundant correction", freeTurn.grammarFeedback)
    val freePayload = capturedPayloads.last { it.first == "/aiTutorRouter" }.second
    assertEquals("openai", freePayload.optString("preferredProvider"))

    // 2. Learn mode should route /aiTutorRouter with taskType = LANGUAGE_LESSON & preferredProvider = openai (OpenAI primary)
    val learnTurn = GeminiTutorService.chatWithTutor(
      userSpeechInput = "What is your ne?",
      targetLanguage = english,
      nativeLanguage = english,
      proficiencyLevel = "A1",
      scenarioTitle = "A1 Greetings & First Introductions",
      conversationMode = TutorConversationMode.LEARN
    )
    assertFalse(learnTurn.isServiceError)
    val learnPayload = capturedPayloads.last { it.first == "/aiTutorRouter" }.second
    assertEquals("openai", learnPayload.optString("preferredProvider"))
    assertEquals("LANGUAGE_LESSON", learnPayload.optString("taskType"))
  }

  @Test
  fun verifyAllTenConversationalScenariosAndContextPrompting() = runBlocking {
    val scenarioTurnInputs = listOf(
      "Hi",
      "How are you?",
      "What is your name?",
      "What is your ne?",
      "I want to learn English.",
      "Yes",
      "My name is Ali.",
      "I am from Pakistan.",
      "Can you help me?",
      "I goed to school yesterday."
    )
    val recordedPrompts = mutableListOf<JSONObject>()
    val runningHistory = mutableListOf<Pair<String, String>>()

    AiProviderManager.openAiBackendTestInterceptor = { path, payload, _, _ ->
      if (path == "/aiTutorRouter") {
        recordedPrompts.add(JSONObject(payload.toString()))
        val msg = payload.optString("userMessage")
        val replyLine = when {
          msg == "Hi" -> "Hi! Nice to meet you. What's your name?"
          msg == "How are you?" -> "I'm doing great, thank you! How are you today?"
          msg == "What is your name?" || msg == "What is your ne?" -> "My name is Emma! What's your name?"
          msg == "I want to learn English." -> "That's wonderful! Let's practice together. Can you tell me your name?"
          msg == "Yes" -> "Great! Let's start with a simple introduction. Say, \"My name is...\""
          msg == "My name is Ali." -> "Nice to meet you, Ali! Where are you from?"
          msg == "I am from Pakistan." -> "Welcome, Ali! How is the weather in Pakistan today?"
          msg == "Can you help me?" -> "Of course, Ali! Tell me what you'd like to practice right now."
          msg.contains("goed") -> "Sounds like a busy day! What did you study at school?"
          else -> "Tell me more!"
        }
        val corrLine = when {
          msg == "What is your ne?" -> "Did you mean: \"What is your name?\""
          msg.contains("goed") -> "Use 'went' instead of 'goed': \"I went to school yesterday.\""
          else -> "NONE"
        }
        JSONObject().apply {
          put("provider", "openai")
          put("model", "gpt-4o-mini")
          put(
            "outputText",
            """
            REPLY: $replyLine
            PHONETICS: Clear natural rhythm
            TRANSLATION: $replyLine
            CORRECTION: $corrLine
            """.trimIndent()
          )
        }
      } else {
        null
      }
    }

    scenarioTurnInputs.forEach { input ->
      runningHistory.add("USER" to input)
      val response = GeminiTutorService.chatWithTutor(
        userSpeechInput = input,
        targetLanguage = english,
        nativeLanguage = english,
        proficiencyLevel = "Beginner",
        conversationHistory = runningHistory.toList(),
        partnerName = "Emma",
        conversationMode = TutorConversationMode.FREE_CONVERSATION
      )
      assertFalse("Scenario '$input' must succeed", response.isServiceError)
      assertFalse(
        "Must never contain old robotic meta-question filler",
        response.replyInTargetLanguage.contains("What made you think of", ignoreCase = true)
      )
      assertFalse(
        "Must never contain generic Nice phrasing filler",
        response.replyInTargetLanguage.contains("Nice phrasing!", ignoreCase = true)
      )
      runningHistory.add("TUTOR" to response.replyInTargetLanguage)
    }

    assertEquals(10, recordedPrompts.size)
    // Verify multi-turn conversationHistory grows up to window size (turn 10 has prior history)
    val lastHistoryCount = recordedPrompts.last().optJSONArray("conversationHistory")?.length() ?: 0
    assertTrue("Later turn must carry prior turns in conversationHistory", lastHistoryCount >= 6)

    // Verify system prompt enforces direct answers & typo tolerance
    val sysPrompt = recordedPrompts.first().optString("systemInstructions")
      .ifBlank { recordedPrompts.first().optString("systemInstruction") }
    assertTrue(sysPrompt.contains("Answer the user's question directly", ignoreCase = true))
    assertTrue(sysPrompt.contains("What made you think of", ignoreCase = true)) // Explicitly forbidden in prompt rules
  }

  @Test
  fun verifyNamedBackendFunctionsAndProviderHealthCheck() = runBlocking {
    val calledPaths = mutableListOf<String>()
    AiProviderManager.openAiBackendTestInterceptor = { path, payload, _, _ ->
      calledPaths.add(path)
      when (path) {
        "/chatWithOpenAI", "/generateTutorResponse" -> JSONObject().apply {
          put("ok", true)
          put("provider", "OPENAI")
          put("model", "gpt-4o-mini")
          put("outputText", "REPLY: Hi! How can I help?")
        }
        "/chatWithGemini", "/generateLessonContent" -> JSONObject().apply {
          put("ok", true)
          put("provider", "GEMINI")
          put("model", "gemini-2.5-flash")
          put("outputText", "Lesson Step 1: Say Good morning.")
        }
        "/checkAIProviderHealth" -> JSONObject().apply {
          put("ok", true)
          put("openAi", JSONObject().put("configured", true).put("status", "configured").put("model", "gpt-4o-mini"))
          put("gemini", JSONObject().put("configured", true).put("status", "configured").put("model", "gemini-2.5-flash"))
        }
        else -> null
      }
    }

    val openAiRes = AiProviderManager.chatWithOpenAI("Hello")
    val geminiRes = AiProviderManager.chatWithGemini("Create a quick vocabulary drill")
    val tutorRes = AiProviderManager.generateTutorResponse("Help me practice")
    val lessonRes = AiProviderManager.generateLessonContent("Generate A1 Greetings quiz")
    val health = AiProviderManager.checkAIProviderHealth(probeRemote = false)

    assertEquals("REPLY: Hi! How can I help?", openAiRes?.outputText)
    assertEquals("Lesson Step 1: Say Good morning.", geminiRes?.outputText)
    assertEquals(com.example.ai.AiProvider.OPENAI, tutorRes?.providerUsed)
    assertEquals(com.example.ai.AiProvider.GEMINI, lessonRes?.providerUsed)
    assertTrue(health.ok)
    assertTrue(health.openAiConfigured)
    assertTrue(health.geminiConfigured)
    assertEquals(
      listOf(
        "/chatWithOpenAI",
        "/chatWithGemini",
        "/generateTutorResponse",
        "/generateLessonContent",
        "/checkAIProviderHealth"
      ),
      calledPaths
    )
  }

  @Test
  fun verifyGenericComplimentFilteringAndRealErrorPreservation() {
    assertNull(GeminiTutorService.filterMeaningfulCorrection("NONE"))
    assertNull(GeminiTutorService.filterMeaningfulCorrection("Natural phrasing! Your sentence is clear."))
    assertNull(GeminiTutorService.filterMeaningfulCorrection("Nice phrasing! Keep going."))
    assertNull(GeminiTutorService.filterMeaningfulCorrection("Great job! No grammatical errors found."))

    val realFix = GeminiTutorService.filterMeaningfulCorrection(
      "Use 'went' instead of 'goed' for the past tense: \"I went to school yesterday.\""
    )
    assertEquals("Use 'went' instead of 'goed' for the past tense: \"I went to school yesterday.\"", realFix)

    val parsed = GeminiTutorService.parseTutorResponse(
      rawText = """
        REPLY: Sounds fun! Who did you go with?
        PHONETICS: Clear
        TRANSLATION: Sounds fun! Who did you go with?
        CORRECTION: Natural phrasing! Keep it up!
      """.trimIndent(),
      userSpeechInput = "I went to the park.",
      targetLanguage = english,
      nativeLanguage = english
    )
    assertNull("Generic praise in CORRECTION must filter to null", parsed.grammarFeedback)

    val voiceParsed = GeminiTutorService.parseVoiceCallResponse(
      rawText = """
        REPLY: Sounds fun!
        PHONETICS: Clear
        TRANSLATION: Sounds fun!
        CORRECTION: Use 'went' instead of 'goed'.
        NATURAL: I went to school yesterday.
      """.trimIndent(),
      userSpeechInput = "I goed to school yesterday.",
      targetLanguage = spanish,
      nativeLanguage = english
    )
    assertEquals("Use 'went' instead of 'goed'.", voiceParsed.gentleCorrection)
    assertEquals("I went to school yesterday.", voiceParsed.naturalAlternative)
  }

  @Test
  fun verifyOrderedA1GreetingsLessonObjectivesInjection() {
    val section = GeminiTutorService.buildLessonObjectivesPromptSection(
      scenarioTitle = "A1 Greetings & First Introductions",
      conversationMode = TutorConversationMode.LEARN,
      conversationHistory = listOf("USER" to "Hello", "TUTOR" to "Hi! What is your name?")
    )
    assertTrue(section.contains("Step 1:"))
    assertTrue(section.contains("Step 2:"))
    assertTrue(section.contains("Step 3:"))
    assertTrue(section.contains("Step 6:"))
    assertTrue(section.contains("greetings & introductions", ignoreCase = true))
  }

  @Test
  fun verifyHonestServiceErrorWhenAllAiProvidersFail() = runBlocking {
    GeminiTutorService.testSimulateGeminiFailure = true
    GeminiTutorService.testResponseInterceptor = null
    AiProviderManager.openAiBackendTestInterceptor = { _, _, _, _ ->
      JSONObject().put("error", "503 Backend unavailable")
    }

    val chatError = GeminiTutorService.chatWithTutor(
      userSpeechInput = "How are you?",
      targetLanguage = english,
      nativeLanguage = english,
      proficiencyLevel = "A1"
    )
    assertTrue(chatError.isServiceError)
    assertTrue(chatError.replyInTargetLanguage.isBlank())
    assertTrue(!chatError.errorMessage.isNullOrBlank())

    val callError = GeminiTutorService.chatInVoiceCall(
      userSpeechInput = "What is your ne?",
      targetLanguage = english,
      nativeLanguage = english,
      proficiencyLevel = "Beginner",
      mode = CallMode.FREE_TALK,
      tutorVoice = TutorVoice.AOEDE,
      history = listOf(VoiceCallTurn(id = "1", speaker = MessageSender.AI_TUTOR, text = "Hello! I'm Emma.")),
      partner = AiConversationPartner.ALL_PARTNERS.first(),
      correctionMode = CorrectionMode.IMMEDIATE,
      cefrLevel = "A1"
    )
    assertTrue(callError.isServiceError)
    assertTrue(callError.replyInTargetLanguage.isBlank())
    assertTrue(!callError.errorMessage.isNullOrBlank())
  }

  @Test
  fun verifyNoClientSideAiApiKeysInEnvOrGradle() {
    val rootDir = File(System.getProperty("user.dir") ?: ".")
    val envExample = File(rootDir, ".env.example")
    if (envExample.exists()) {
      val content = envExample.readText()
      assertFalse(".env.example must not define OPENAI_API_KEY", content.contains("OPENAI_API_KEY"))
      assertFalse(".env.example must not contain OpenAI secret prefix", content.contains("sk-"))
      assertFalse(".env.example must not contain Gemini secret prefix", content.contains("AIzaSy"))
    }
    val appBuildGradle = File(rootDir, "app/build.gradle.kts")
    if (appBuildGradle.exists()) {
      val gradleText = appBuildGradle.readText()
      assertFalse("app/build.gradle.kts must not expose OPENAI_API_KEY", gradleText.contains("OPENAI_API_KEY"))
      assertFalse("app/build.gradle.kts must not expose sk- keys", gradleText.contains("sk-"))
    }
  }
}
