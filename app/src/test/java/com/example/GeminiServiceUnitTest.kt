package com.example

import com.example.ai.GeminiResult
import com.example.ai.GeminiService
import com.example.ai.GeminiTutorService
import com.example.ai.GeminiUserContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiServiceUnitTest {

  @Before
  fun setUp() {
    GeminiService.initialize(androidx.test.core.app.ApplicationProvider.getApplicationContext())
    GeminiTutorService.testResponseInterceptor = null
    GeminiTutorService.testSimulateGeminiFailure = false
    GeminiService.testForceOffline = null
    GeminiService.testBypassDailyQuota = false
    GeminiService.syncProStatus(false)
    GeminiService.resetDailyQuotaForTests()
  }

  @After
  fun tearDown() {
    GeminiTutorService.testResponseInterceptor = null
    GeminiTutorService.testSimulateGeminiFailure = false
    GeminiService.testForceOffline = null
    GeminiService.testBypassDailyQuota = false
    GeminiService.syncProStatus(false)
    GeminiService.resetDailyQuotaForTests()
  }

  @Test
  fun systemPromptInjectsTargetNativeAndCefrLevel() {
    val ctx = GeminiUserContext(
      targetLanguageCode = "es",
      nativeLanguageCode = "en",
      cefrLevel = "A2",
      learnerMemory = "Mixes up ser and estar"
    )
    val header = ctx.toSystemPromptHeader("You are Emma.")
    assertTrue(header.contains("Spanish"))
    assertTrue(header.contains("English"))
    assertTrue(header.contains("A2"))
    assertTrue(header.contains("ser and estar"))
  }

  @Test
  fun structuredVoiceTurnParsesScoreGrammarAndNaturalPhrase() = runBlocking {
    var capturedPrompt = ""
    GeminiTutorService.testResponseInterceptor = { prompt, _ ->
      capturedPrompt = prompt
      """
        {
          "reply": "¡Hola! Me encanta el café con leche por la mañana.",
          "phonetics": "OH-lah! Meh en-KAHN-tah el kah-FEH kohn LEH-cheh",
          "translation": "Hello! I love coffee with milk in the morning.",
          "pronunciationScore": 87,
          "grammarFix": "Use 'quiero un café' instead of 'yo querer café'.",
          "naturalPhrase": "Me gustaría tomar un café con leche, por favor."
        }
      """.trimIndent()
    }

    val result = GeminiService.generateVoiceTurnWithFeedback(
      userContext = GeminiUserContext(targetLanguageCode = "es", nativeLanguageCode = "en", cefrLevel = "A1"),
      partnerName = "Emma",
      partnerPersona = "Warm Spanish Tutor",
      topic = "Ordering at a Café",
      userUtterance = "Yo querer café",
      recentHistory = emptyList()
    )

    assertTrue(result is GeminiResult.Success)
    val payload = (result as GeminiResult.Success).data
    assertEquals(87, payload.pronunciationScore)
    assertEquals("Use 'quiero un café' instead of 'yo querer café'.", payload.grammarFix)
    assertEquals("Me gustaría tomar un café con leche, por favor.", payload.naturalPhrase)
    assertTrue(capturedPrompt.contains("CEFR Level: A1"))
  }

  @Test
  fun localCacheServesSecondLessonOfflineWithoutDuplicateApiCalls() = runBlocking {
    var callCount = 0
    GeminiTutorService.testResponseInterceptor = { _, _ ->
      callCount += 1
      """
        {
          "title": "Ordering Coffee",
          "description": "Order drinks politely at a café.",
          "category": "Speaking",
          "estimatedMinutes": 5,
          "xpReward": 40,
          "vocabulary": [],
          "exercises": [
            {
              "exerciseType": "MULTIPLE_CHOICE",
              "prompt": "How do you ask for coffee politely?",
              "targetPhrase": "Un café, por favor.",
              "phonetic": "oon kah-FEH por fah-BOR",
              "correctAnswer": "Un café, por favor.",
              "options": ["Un café, por favor.", "Adiós", "Buenas noches", "Hasta luego"],
              "explanation": "Use 'por favor' for polite requests."
            }
          ]
        }
      """.trimIndent()
    }

    val ctx = GeminiUserContext(targetLanguageCode = "es", nativeLanguageCode = "en", cefrLevel = "A1")
    val first = GeminiService.generateCefrLessonStructured(ctx, topic = "UnitTest Coffee Cache")
    assertTrue(first is GeminiResult.Success)
    assertFalse((first as GeminiResult.Success).fromCache)
    assertEquals(1, callCount)

    // Go offline and request the exact same lesson — must load from local cache cleanly
    GeminiService.testForceOffline = true
    val second = GeminiService.generateCefrLessonStructured(ctx, topic = "UnitTest Coffee Cache")
    assertTrue(second is GeminiResult.Success)
    assertTrue((second as GeminiResult.Success).fromCache)
    assertEquals(1, callCount)
  }

  @Test
  fun offlineDetectionReturnsOfflineResultWhenUncached() = runBlocking {
    GeminiService.testForceOffline = true
    val result = GeminiService.generateText(
      userContext = GeminiUserContext("fr", "en", "B1"),
      roleInstruction = "You are Emma.",
      userPrompt = "Bonjour!"
    )
    assertTrue(result is GeminiResult.Offline)
  }

  @Test
  fun proUsersHaveUnlimitedDailyQuota() {
    GeminiService.syncProStatus(false)
    assertEquals(GeminiService.FREE_DAILY_AI_CALL_LIMIT, GeminiService.quotaUiState.value.remainingDailyCalls)
    GeminiService.syncProStatus(true)
    assertEquals(Int.MAX_VALUE, GeminiService.quotaUiState.value.remainingDailyCalls)
    assertFalse(GeminiService.quotaUiState.value.isLimitReached)
  }
}
