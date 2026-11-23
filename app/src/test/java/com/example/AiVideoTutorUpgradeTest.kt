package com.example

import com.example.ai.GeminiTutorService
import com.example.avatar.AvatarProvider
import com.example.avatar.AvatarStreamingService
import com.example.avatar.CefrLevel
import com.example.avatar.TutorPersona
import com.example.data.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiVideoTutorUpgradeTest {

  @Test
  fun tutorPersonas_areFullyConfiguredWithFemaleAndMaleOptions() {
    val tutors = TutorPersona.ALL_TUTORS
    assertTrue("Should offer at least 6 diverse tutors", tutors.size >= 8)

    val femaleTutors = tutors.filter { it.gender == "Female" }
    val maleTutors = tutors.filter { it.gender == "Male" }
    assertTrue("Should include female tutors", femaleTutors.isNotEmpty())
    assertTrue("Should include male tutors", maleTutors.isNotEmpty())

    val elena = tutors.firstOrNull { it.id == "tutor_elena" }
    assertNotNull("Elena should be present as female flagship tutor", elena)
    assertEquals("Elena Morales", elena?.name)
    assertEquals("Female", elena?.gender)
    assertTrue("Default greeting should not be blank", elena?.defaultGreeting?.isNotBlank() == true)
    assertTrue("Avatar preview URL should be valid", elena?.avatarPreviewUrl?.startsWith("http") == true)

    // Verify language finding helper
    val esTutor = TutorPersona.findTutorForLanguage("es", preferFemale = true)
    assertEquals("tutor_elena", esTutor.id)

    val frTutor = TutorPersona.findTutorForLanguage("fr", preferFemale = true)
    assertEquals("tutor_chloe", frTutor.id)
  }

  @Test
  fun cefrLevels_coverA1ToC2WithAdaptiveSpeed() {
    val levels = CefrLevel.values()
    assertEquals(6, levels.size)
    assertEquals("A1", levels[0].code)
    assertEquals("C2", levels[5].code)
    assertTrue("A1 should speak slower than C2", levels[0].speed < levels[5].speed)
  }

  @Test
  fun geminiVideoTutor_parsesResponseReliably() {
    val rawGeminiOutput = """
      REPLY: ¡Hola! Me alegro mucho de verte. ¿Cómo va tu práctica de español hoy?
      PHONETICS: oh-lah! meh ah-leh-groh moo-choh deh vehr-teh.
      TRANSLATION: Hello! I am so glad to see you. How is your Spanish practice going today?
      GRAMMAR_FEEDBACK: Remember to use "me alegro de" when expressing personal joy.
      CORRECTION: Me alegro de verte.
      SCORE: 94
      VOCAB_TIP: Alegrarse : To be glad or rejoice
      EXPRESSION: warm_smile
    """.trimIndent()

    val parsed = GeminiTutorService.parseVideoTutorResponse(
      rawText = rawGeminiOutput,
      userSpeechInput = "Hola profesora",
      targetLanguage = Language.findByCode("es"),
      nativeLanguage = Language.findByCode("en"),
      cefrLevel = "B1"
    )

    assertEquals("¡Hola! Me alegro mucho de verte. ¿Cómo va tu práctica de español hoy?", parsed.replyInTargetLanguage)
    assertEquals("Hello! I am so glad to see you. How is your Spanish practice going today?", parsed.translationInNativeLanguage)
    assertEquals("Me alegro de verte.", parsed.suggestedCorrection)
    assertEquals(94, parsed.pronunciationScore)
    assertEquals("Alegrarse : To be glad or rejoice", parsed.vocabularyTip)
    assertEquals("warm_smile", parsed.facialExpression)
    assertEquals("B1", parsed.cefrLevelEvaluated)
  }

  @Test
  fun avatarProviders_haveAccurateDescriptions() {
    val heygen = AvatarProvider.HEYGEN
    val tavus = AvatarProvider.TAVUS
    assertTrue(heygen.displayName.isNotEmpty())
    assertTrue(tavus.displayName.isNotEmpty())
    assertTrue(heygen.providerDescription.isNotEmpty())
    assertTrue(tavus.providerDescription.isNotEmpty())
  }

  @Test
  fun aiVoiceConversationPartnersAnd15Voices_areFullyConfigured() {
    val voices = com.example.data.model.TutorVoice.entries
    assertEquals("Should provide all 15 Gemini voices", 15, voices.size)
    val expectedVoiceNames = listOf(
      "Aoede", "Kore", "Zephyr", "Puck", "Charon",
      "Fenrir", "Leda", "Orus", "Achernar", "Achird",
      "Gacrux", "Sulafat", "Despina", "Erinome", "Umbriel"
    )
    expectedVoiceNames.forEach { name ->
      val found = com.example.data.model.TutorVoice.findById(name)
      assertEquals(name, found.voiceName)
      assertTrue(found.samplePhrase.isNotBlank())
    }

    val partners = com.example.data.model.AiConversationPartner.ALL_PARTNERS
    assertTrue("Should provide at least 12 AI conversation partners", partners.size >= 12)
    val expectedPartners = listOf(
      "Emma", "James", "Sophia", "Daniel", "Olivia", "Michael",
      "Amelia", "Noah", "Isabella", "Ethan", "Mia", "Lucas", "Chloe"
    )
    expectedPartners.forEach { partnerName ->
      val partner = com.example.data.model.AiConversationPartner.findById(partnerName)
      assertEquals(partnerName, partner.name)
      assertTrue(partner.roleTitle.isNotBlank())
      assertTrue(partner.sampleGreeting.isNotBlank())
      assertTrue(partner.photoUrl.startsWith("http"))
    }

    assertEquals(15, com.example.data.model.CallMode.CORE_15_TOPICS.size)
    assertEquals(3, com.example.data.model.CorrectionMode.entries.size)
  }

  @Test
  fun roleplayAudioVerification_rejectsSilenceNoiseAndEchoAndNeverInventsChickenRice() = kotlinx.coroutines.runBlocking {
    val chloe = com.example.data.model.AiConversationPartner.findById("partner_chloe")
    assertEquals("Chloe", chloe.name)
    assertEquals(com.example.data.model.TutorVoice.AOEDE, chloe.voice)

    com.example.data.repository.AccessPolicyManager.updateEntitlement(
      com.example.data.repository.ServerEntitlementRecord(
        uid = "test_audio_verifier",
        status = com.example.data.repository.EntitlementStatus.PRO_ACTIVE,
        productId = com.example.data.model.SubscriptionPlan.PRODUCT_ID_PRO,
        basePlanId = com.example.data.model.SubscriptionPlan.BASE_PLAN_MONTHLY,
        verifiedWithPlayApi = true,
        expiryEpochMs = System.currentTimeMillis() + 30L * 24L * 3600L * 1000L
      )
    )
    GeminiTutorService.testResponseInterceptor = { prompt, _ ->
      if (prompt.contains("Ordering at a Restaurant")) {
        """
          REPLY: Sure thing! One plate of chicken and rice coming right up. Would you like anything to drink?
          PHONETICS: shoor thing! wun playt uv chik-en and rys
          TRANSLATION: Sure thing! One plate of chicken and rice coming right up. Would you like anything to drink?
          CORRECTION: NONE
        """.trimIndent()
      } else {
        null
      }
    }

    try {
      // TEST 1 (Audio Layer): Silent and low-energy ambient hum PCM buffers must be rejected before calling Gemini
      val emptyWav = ByteArray(44)
      assertFalse("Empty WAV header must be rejected", GeminiTutorService.hasMeaningfulSpeechPcm(emptyWav))

      // 1 second of silence (16,000 samples = 32,000 bytes + 44 header)
      val oneSecondSilenceWav = ByteArray(44 + 32000)
      assertFalse("Silent WAV buffer must be rejected", GeminiTutorService.hasMeaningfulSpeechPcm(oneSecondSilenceWav))

      // 1 second of low-amplitude room hum (peak = 350, well below human speech threshold 2200)
      val lowHumWav = ByteArray(44 + 32000)
      var idx = 44
      while (idx + 1 < lowHumWav.size) {
        val sample = 300
        lowHumWav[idx] = (sample and 0xFF).toByte()
        lowHumWav[idx + 1] = ((sample shr 8) and 0xFF).toByte()
        idx += 2
      }
      assertFalse("Low-amplitude ambient hum must be rejected", GeminiTutorService.hasMeaningfulSpeechPcm(lowHumWav))

      val restaurantHistory = listOf(
        "AI" to "Hello! Welcome to our restaurant. What would you like to order?"
      )

      // Calling chatWithTutorAudio on silence or low hum in Restaurant Roleplay must return null (NEVER invent "chicken rice")
      val silentTurnResult = GeminiTutorService.chatWithTutorAudio(
        wavBytes = lowHumWav,
        fallbackTextIfAny = "",
        targetLanguage = Language.findByCode("en"),
        nativeLanguage = Language.findByCode("en"),
        proficiencyLevel = "A1",
        scenarioTitle = "Ordering at a Restaurant",
        scenarioInstruction = "Order a meal and request the bill.",
        conversationHistory = restaurantHistory,
        partnerName = "Chloe"
      )
      org.junit.Assert.assertNull(
        "Silence/ambient noise in Restaurant Roleplay must NEVER generate 'chicken rice' or any user turn",
        silentTurnResult
      )

      // TEST 4: Audio Echo Rejection — Chloe's own voice picked up by mic must be rejected
      val aiGreeting = "Hello! Welcome to our restaurant. What would you like to order?"
      assertTrue(
        "Exact echo of Chloe's greeting must be detected",
        GeminiTutorService.isLikelyEchoOfAiUtterance(aiGreeting, listOf(aiGreeting))
      )
      assertTrue(
        "Partial speaker bleed of Chloe's question must be detected",
        GeminiTutorService.isLikelyEchoOfAiUtterance("What would you like to order?", listOf(aiGreeting))
      )
      assertFalse(
        "Genuine user order must NOT be flagged as echo",
        GeminiTutorService.isLikelyEchoOfAiUtterance("I want chicken and rice.", listOf(aiGreeting))
      )

      val echoTurnResult = GeminiTutorService.chatWithTutorAudio(
        wavBytes = lowHumWav,
        fallbackTextIfAny = "What would you like to order?",
        targetLanguage = Language.findByCode("en"),
        nativeLanguage = Language.findByCode("en"),
        proficiencyLevel = "A1",
        scenarioTitle = "Ordering at a Restaurant",
        scenarioInstruction = "Order a meal and request the bill.",
        conversationHistory = restaurantHistory,
        partnerName = "Chloe"
      )
      org.junit.Assert.assertNull(
        "Audio echo of Chloe's own speech must be rejected and return null",
        echoTurnResult
      )

      // TEST 2: Real Speech — Genuine spoken transcript is accepted once and answered naturally
      val realSpeechResult = GeminiTutorService.chatWithTutorAudio(
        wavBytes = lowHumWav,
        fallbackTextIfAny = "I want chicken and rice.",
        targetLanguage = Language.findByCode("en"),
        nativeLanguage = Language.findByCode("en"),
        proficiencyLevel = "A1",
        scenarioTitle = "Ordering at a Restaurant",
        scenarioInstruction = "Order a meal and request the bill.",
        conversationHistory = restaurantHistory,
        partnerName = "Chloe"
      )
      assertNotNull("Genuine user speech must produce a turn result", realSpeechResult)
      assertEquals("I want chicken and rice.", realSpeechResult?.userTranscript)
      assertTrue(
        "Chloe's reply must be non-blank",
        realSpeechResult?.tutorResponse?.replyInTargetLanguage?.isNotBlank() == true
      )
    } finally {
      GeminiTutorService.testResponseInterceptor = null
      com.example.ai.AiProviderManager.resetStateForTesting()
      com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
    }
  }

  @Test
  fun allSupportedLanguages_haveUniqueCodesAndDeliver500PlusLessonsAcrossCefrA1ToC2() {
    val allLanguages = Language.SUPPORTED_LANGUAGES
    assertTrue("Should support at least 90 unique languages", allLanguages.size >= 90)

    val uniqueCodes = allLanguages.map { it.code }.toSet()
    assertEquals("Every language code in SUPPORTED_LANGUAGES must be unique", allLanguages.size, uniqueCodes.size)

    // Verify RTL detection for all RTL languages
    val rtlCodes = listOf("ar", "ur", "fa", "ps", "he", "sd", "yi")
    rtlCodes.forEach { code ->
      val lang = Language.findByCode(code)
      assertTrue("Language $code (${lang.name}) must be marked RTL", lang.isRtl)
      assertTrue("Language.isRtlLanguage($code) must return true", Language.isRtlLanguage(code))
    }

    // Verify all 93 languages have non-blank metadata and >= 500 progressive lessons
    val cefrExpected = setOf("A1", "A2", "B1", "B2", "C1", "C2")
    allLanguages.forEach { lang ->
      assertTrue("Language ${lang.code} must have a writingSystem", lang.writingSystem.isNotBlank())
      assertTrue("Language ${lang.code} must have a sampleGreeting", lang.sampleGreeting.isNotBlank())
      assertTrue("Language ${lang.code} must have a localeCode", lang.localeCode.isNotBlank())

      val lessons = com.example.data.repository.CurriculumData.getLessonsForLanguage(
        targetLangCode = lang.code,
        nativeLangCode = "ur"
      )
      assertTrue(
        "Language ${lang.code} (${lang.name}) must have >= 500 progressive lessons, found ${lessons.size}",
        lessons.size >= 500
      )

      val cefrPresent = lessons.map { it.cefrLevel }.toSet()
      assertTrue(
        "Language ${lang.code} must cover A1 through C2, found $cefrPresent",
        cefrPresent.containsAll(cefrExpected)
      )

      val firstLesson = lessons.first()
      assertTrue("First lesson of ${lang.code} must have exercises", firstLesson.exercises.isNotEmpty())
      assertTrue(
        "First exercise of ${lang.code} must have localized explanation",
        firstLesson.exercises.first().explanationNative.isNotBlank()
      )
    }
  }

  @Test
  fun multilingualFeatures_supportVocabularyReadingListeningGrammarPlacementAndDailyCard() {
    val priorityCodes = listOf(
      "en", "es", "fr", "de", "ar", "ur", "hi", "zh", "ja", "ko",
      "pt", "it", "tr", "ru", "nl", "fa", "ps", "he", "bn", "sw"
    )

    priorityCodes.forEach { code ->
      val flashcards = com.example.data.repository.CurriculumData.getStarterFlashcards(code)
      assertTrue("Language $code must have starter flashcards", flashcards.isNotEmpty())
      assertEquals(code, flashcards.first().languageCode)

      val reading = com.example.data.repository.CurriculumData.getReadingAcademyPassages(code)
      assertTrue("Language $code must have Reading Academy passages", reading.isNotEmpty())
      assertTrue("Reading passage for $code must have comprehension questions", reading.first().questions.isNotEmpty())

      val listening = com.example.data.repository.CurriculumData.getListeningAcademyItems(code)
      assertTrue("Language $code must have Listening Academy dialogues", listening.isNotEmpty())
      assertTrue("Listening dialogue for $code must have transcript lines", listening.first().lines.isNotEmpty())

      val placement = com.example.data.repository.CurriculumData.getPlacementQuestions(code)
      assertTrue("Language $code must have placement questions (A1-C1)", placement.size >= 4)

      val dailyItem = com.example.data.repository.CurriculumData.getDailyLearningItem(code)
      assertTrue("Language $code must have a daily learning phrase", dailyItem.phraseOrIdiom.isNotBlank())

      val grammarTopics = com.example.data.repository.CurriculumData.getGrammarStudioTopics(code, "ur")
      assertTrue("Language $code must have Grammar Studio topics", grammarTopics.isNotEmpty())

      val grammarQuiz = com.example.data.repository.CurriculumData.getGrammarStudioQuizQuestions(code, "ur")
      assertTrue("Language $code must have Grammar Studio quiz questions", grammarQuiz.isNotEmpty())
    }
  }

  @Test
  fun languageSwitchingAndProgressSnapshots_preserveIndependentProgressPerLanguage() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val manager = com.example.data.repository.LearningMemoryAndOfflineManager(context)

    // Simulate studying English -> Arabic -> Japanese -> Urdu -> Spanish -> back to English
    val enSnap = com.example.data.model.LanguageLearningProgressSnapshot(
      languageCode = "en",
      proficiencyLevel = "Intermediate",
      xpPoints = 450L,
      completedLessonIds = listOf("en_lesson_1", "ext_en_a1_1_1", "ext_en_a1_1_2"),
      currentLessonId = "ext_en_a1_1_3",
      streakDays = 7L
    )
    val arSnap = com.example.data.model.LanguageLearningProgressSnapshot(
      languageCode = "ar",
      proficiencyLevel = "Beginner",
      xpPoints = 180L,
      completedLessonIds = listOf("ar_lesson_1", "ext_ar_a1_1_1"),
      currentLessonId = "ext_ar_a1_1_2",
      streakDays = 7L
    )
    val jaSnap = com.example.data.model.LanguageLearningProgressSnapshot(
      languageCode = "ja",
      proficiencyLevel = "Beginner",
      xpPoints = 220L,
      completedLessonIds = listOf("ja_lesson_1", "ext_ja_a1_1_1", "ext_ja_a1_1_2", "ext_ja_a1_1_3"),
      currentLessonId = "ext_ja_a1_1_4",
      streakDays = 7L
    )
    val urSnap = com.example.data.model.LanguageLearningProgressSnapshot(
      languageCode = "ur",
      proficiencyLevel = "Advanced",
      xpPoints = 640L,
      completedLessonIds = listOf("ur_lesson_1", "ext_ur_b2_1_1"),
      currentLessonId = "ext_ur_b2_1_2",
      streakDays = 7L
    )

    manager.saveLanguageProgressSnapshot(enSnap)
    manager.saveLanguageProgressSnapshot(arSnap)
    manager.saveLanguageProgressSnapshot(jaSnap)
    manager.saveLanguageProgressSnapshot(urSnap)

    val loadedEn = manager.loadLanguageProgressSnapshot("en")
    val loadedAr = manager.loadLanguageProgressSnapshot("ar")
    val loadedJa = manager.loadLanguageProgressSnapshot("ja")
    val loadedUr = manager.loadLanguageProgressSnapshot("ur")

    assertNotNull(loadedEn)
    assertEquals("Intermediate", loadedEn?.proficiencyLevel)
    assertEquals(3, loadedEn?.completedLessonIds?.size)
    assertEquals("ext_en_a1_1_3", loadedEn?.currentLessonId)

    assertNotNull(loadedAr)
    assertEquals("Beginner", loadedAr?.proficiencyLevel)
    assertEquals(2, loadedAr?.completedLessonIds?.size)

    assertNotNull(loadedJa)
    assertEquals(4, loadedJa?.completedLessonIds?.size)

    assertNotNull(loadedUr)
    assertEquals("Advanced", loadedUr?.proficiencyLevel)
    assertEquals(640L, loadedUr?.xpPoints)

    // Verify independent Native, Target, and App Interface language persistence
    val settings = com.example.data.model.UserAccountSettings(
      appInterfaceLanguageCode = "en",
      preferredVoiceId = "aoede",
      defaultPartnerId = "emma"
    )
    manager.saveUserAccountSettings("test_multilingual_user", settings)
    val loadedSettings = manager.loadUserAccountSettings("test_multilingual_user")
    assertEquals("en", loadedSettings.appInterfaceLanguageCode)
  }
}

