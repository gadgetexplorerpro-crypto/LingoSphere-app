package com.example

import com.example.ai.GeminiTutorService
import com.example.data.model.ExerciseType
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.RoleplayScenario
import com.example.data.model.SpacedRepetitionEngine
import com.example.data.model.SrsRating
import com.example.data.model.WordPronunciationItem
import com.example.data.repository.CurriculumData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LingoSphereUpgradeTest {

  @Test
  fun spacedRepetitionEngine_computesSm2IntervalsAndBoxLevelsCorrectly() {
    val card = FlashcardProgress(
      id = "es_hola",
      userId = "user_1",
      wordId = "es_hola",
      languageCode = "es",
      frontText = "Hola",
      backText = "Hello",
      boxLevel = 1L,
      nextReviewEpochDay = 0L
    )

    // 1. GOOD rating on new card -> repetition 1, interval 2 days, box >= 2
    val firstGood = SpacedRepetitionEngine.computeNextReview(
      card = card,
      currentMeta = null,
      rating = SrsRating.GOOD,
      todayEpochDay = 100L
    )
    assertEquals(1, firstGood.updatedMetadata.repetitions)
    assertEquals(2, firstGood.updatedMetadata.intervalDays)
    assertTrue(firstGood.updatedBoxLevel >= 2L)
    assertEquals(102L, firstGood.nextReviewEpochDay)

    // 2. Second GOOD rating -> repetition 2, interval 5 days
    val cardAfterFirst = card.copy(
      boxLevel = firstGood.updatedBoxLevel,
      nextReviewEpochDay = firstGood.nextReviewEpochDay
    )
    val secondGood = SpacedRepetitionEngine.computeNextReview(
      card = cardAfterFirst,
      currentMeta = firstGood.updatedMetadata,
      rating = SrsRating.GOOD,
      todayEpochDay = 102L
    )
    assertEquals(2, secondGood.updatedMetadata.repetitions)
    assertEquals(5, secondGood.updatedMetadata.intervalDays)
    assertTrue(secondGood.updatedBoxLevel >= 3L)

    // 3. AGAIN rating -> resets repetitions to 0, increments lapses, boxLevel = 1
    val afterLapse = SpacedRepetitionEngine.computeNextReview(
      card = cardAfterFirst.copy(boxLevel = secondGood.updatedBoxLevel),
      currentMeta = secondGood.updatedMetadata,
      rating = SrsRating.AGAIN,
      todayEpochDay = 107L
    )
    assertEquals(0, afterLapse.updatedMetadata.repetitions)
    assertEquals(1, afterLapse.updatedMetadata.lapses)
    assertEquals(1L, afterLapse.updatedBoxLevel)
    assertTrue(SpacedRepetitionEngine.isDifficultCard(cardAfterFirst, afterLapse.updatedMetadata))

    // 4. EASY rating increases ease factor and jumps interval
    val easyReview = SpacedRepetitionEngine.computeNextReview(
      card = card,
      currentMeta = null,
      rating = SrsRating.EASY,
      todayEpochDay = 110L
    )
    assertTrue(easyReview.updatedMetadata.easeFactor > 2.5)
    assertTrue(easyReview.updatedMetadata.intervalDays >= 4)
  }

  @Test
  fun curriculumData_providesA1ToC2LessonsSpeakingExercisesAndAcademyContent() {
    val lessons = CurriculumData.getLessonsForLanguage("es", "en")
    assertTrue("Expected at least 8 lessons across A1-C2", lessons.size >= 8)

    val cefrLevels = lessons.map { it.cefrLevel }.toSet()
    assertTrue(cefrLevels.containsAll(listOf("A1", "A2", "B1", "B2", "C1", "C2")))

    val hasSpeakingExercise = lessons.any { lesson ->
      lesson.exercises.any { it.type == ExerciseType.SPEAKING }
    }
    assertTrue("Curriculum must include interactive SPEAKING exercises", hasSpeakingExercise)

    val hasRevisionLesson = lessons.any { it.isRevision }
    assertTrue("Curriculum must include revision lessons", hasRevisionLesson)

    val listeningItems = CurriculumData.getListeningAcademyItems("es")
    assertTrue("Listening Academy must provide multi-level items", listeningItems.size >= 3)
    assertTrue(listeningItems.all { it.lines.isNotEmpty() && it.questions.isNotEmpty() })

    val readingPassages = CurriculumData.getReadingAcademyPassages("es")
    assertTrue("Reading Academy must provide graded passages", readingPassages.size >= 2)
    assertTrue(readingPassages.all { it.glossary.isNotEmpty() && it.questions.isNotEmpty() })
  }

  @Test
  fun placementEvaluation_mapsScoresToCefrLevelsWithHonestCertificationNotice() {
    val evalA1 = CurriculumData.evaluatePlacementScore(correctCount = 1, totalCount = 6)
    assertEquals("A1", evalA1.recommendedCefrBand)
    assertEquals("Beginner", evalA1.mappedProficiencyLevel)
    assertTrue(evalA1.disclaimer.contains("internal LingoSphere learning stage"))

    val evalB1 = CurriculumData.evaluatePlacementScore(correctCount = 4, totalCount = 6)
    assertEquals("B1", evalB1.recommendedCefrBand)
    assertEquals("Intermediate", evalB1.mappedProficiencyLevel)

    val evalC1 = CurriculumData.evaluatePlacementScore(correctCount = 6, totalCount = 6)
    assertEquals("C1", evalC1.recommendedCefrBand)
    assertEquals("Advanced", evalC1.mappedProficiencyLevel)
  }

  @Test
  fun geminiTutorService_parsesPronunciationAndWritingResponsesReliably() {
    val es = Language.findByCode("es")
    val en = Language.findByCode("en")

    val diagRaw = """
      IPA: /ˈbwe.nos ˈði.as/
      STRESS: BWE-nos DI-as
      SOUNDS: rolled r, soft d
      TONGUE_MOUTH: Place the tongue lightly behind the upper teeth for soft 'd'.
      INTONATION: Keep a smooth rising-falling cadence.
      ACCENT_NOTE: Castilian Spanish uses clear apical s and dental d.
    """.trimIndent()

    val parsedDiag = GeminiTutorService.parsePronunciationDiagnostic(
      raw = diagRaw,
      targetPhrase = "Buenos días amigo",
      recognizedSpeech = "Buenos días",
      accuracyPercent = 67,
      wordBreakdown = listOf(WordPronunciationItem("Buenos", "Buenos", "MATCH")),
      targetLanguage = es,
      regionalAccent = "Castilian"
    )
    assertEquals(67, parsedDiag.accuracyPercent)
    assertEquals("/ˈbwe.nos ˈði.as/", parsedDiag.ipaTranscription)
    assertTrue(parsedDiag.problematicSounds.contains("rolled r"))

    val writingRaw = """
      CORRECTED: Ayer fui al restaurante con mis amigos.
      CEFR: B1
      SCORE: 92
      SUMMARY: Good past tense structure; remember 'a + el' contracts to 'al'.
      ITEM: a el restaurante | al restaurante | Grammar | ERROR | In Spanish, 'a' + 'el' always contracts to 'al'.
      ITEM: con mis amigos | junto a mis amigos | Style | STYLE | Optional stylistic variation for variety.
      NATURAL: pasarlo bien (to have a good time), charlar un rato (to chat for a while)
      DRILL_Q: Complete: Vamos ___ cine (a + el).
      DRILL_A: al
    """.trimIndent()

    val parsedWriting = GeminiTutorService.parseWritingCoachResponse(
      raw = writingRaw,
      userText = "Ayer fui a el restaurante con mis amigos.",
      writingFormat = "Sentence",
      targetLanguage = es,
      nativeLanguage = en
    )
    assertEquals("Ayer fui al restaurante con mis amigos.", parsedWriting.correctedText)
    assertEquals(2, parsedWriting.corrections.size)
    assertTrue(parsedWriting.corrections[0].isGenuineError)
    assertFalse(parsedWriting.corrections[1].isGenuineError)
  }

  @Test
  fun roleplayScenarios_includeAllRequiredRealWorldSituationsAndObjectives() {
    val scenarios = RoleplayScenario.SCENARIOS
    assertTrue("Expected at least 16 roleplay scenarios", scenarios.size >= 16)
    assertTrue(scenarios.all { it.taskObjective.isNotBlank() && it.keyVocabulary.isNotEmpty() })
  }

  @Test
  fun geminiSubscriptionPlansAndProfitProtection_verifyAllowancesMarginsAndQuotaWarnings() {
    // 1. Verify Free (0 AI) and LingoSphere Pro ($9.99/mo) allowances
    val free = com.example.ai.GeminiCostAndQuotaManager.FREE_ALLOWANCES
    assertEquals(0, free.monthlyAiVoiceMinutes)
    assertEquals(0, free.monthlyAiVideoMinutes)
    assertEquals(0, free.dailyGrammarChecks)
    assertFalse(free.unlimitedBasicLessons)

    val premiumMonthly = com.example.ai.GeminiCostAndQuotaManager.PREMIUM_MONTHLY_ALLOWANCES
    assertEquals(9.99, premiumMonthly.monthlyPriceUsd, 0.001)
    assertEquals(150, premiumMonthly.monthlyAiVoiceMinutes)
    assertEquals(45, premiumMonthly.monthlyAiVideoMinutes)
    assertEquals(50, premiumMonthly.dailyGrammarChecks)
    assertEquals(50, premiumMonthly.dailyPronunciationChecks)
    assertEquals(40, premiumMonthly.dailyIntelligentStudioUses)
    assertEquals(120, premiumMonthly.dailyHumanCallMinutes)

    val pro999 = com.example.ai.GeminiCostAndQuotaManager.PREMIUM_PRO_999_ALLOWANCES
    assertEquals(9.99, pro999.monthlyPriceUsd, 0.001)
    assertEquals(150, pro999.monthlyAiVoiceMinutes)
    assertEquals(45, pro999.monthlyAiVideoMinutes)
    assertEquals(50, pro999.dailyGrammarChecks)
    assertEquals(50, pro999.dailyPronunciationChecks)
    assertEquals(40, pro999.dailyIntelligentStudioUses)
    assertEquals(120, pro999.dailyHumanCallMinutes)

    // 2. Verify profitability after 15% Google Play fee and worst-case Gemini API spend
    val reports = com.example.ai.GeminiCostAndQuotaManager.evaluateAllPlansProfitability()
    assertEquals(3, reports.size)
    val premiumReport = reports.first { it.tierCode == "premium_monthly" }
    val proReport = reports.first { it.tierCode == "premium_pro" }
    assertTrue("Pro Monthly $9.99 must be profitable even at 100% allowance usage", premiumReport.isProfitableAtMaxUsage)
    assertTrue("Pro Monthly $9.99 net profit must exceed $5.00", premiumReport.worstCaseNetProfitUsd > 5.0)
    assertTrue("LingoSphere Pro $9.99 must be profitable even at 100% allowance usage", proReport.isProfitableAtMaxUsage)
    assertTrue("LingoSphere Pro $9.99 net profit must exceed $5.00", proReport.worstCaseNetProfitUsd > 5.0)

    // 3. Verify smart model routing & context window compression
    assertEquals(
      com.example.ai.GeminiModelTier.FLASH_LITE,
      com.example.ai.GeminiCostAndQuotaManager.selectOptimalGeminiModel(com.example.ai.GeminiFeatureCategory.GRAMMAR_CHECK)
    )
    assertEquals(
      com.example.ai.GeminiModelTier.FLASH_STANDARD,
      com.example.ai.GeminiCostAndQuotaManager.selectOptimalGeminiModel(com.example.ai.GeminiFeatureCategory.AI_VOICE_TUTOR)
    )

    val longHistory = (1..8).map { i ->
      (if (i % 2 == 0) "TUTOR" else "USER") to "This is conversational turn number $i practicing Spanish subjunctive and travel phrases."
    }
    val (compressedBlock, tokensSaved) = com.example.ai.GeminiCostAndQuotaManager.compressConversationHistory(longHistory)
    assertTrue(compressedBlock.contains("Compressed Earlier Context:"))
    assertTrue(tokensSaved > 0)

    // 4. Verify 80% and 95% usage warning levels
    com.example.ai.GeminiCostAndQuotaManager.switchActiveAccount("test_quota_user", "premium_monthly")
    val status80 = com.example.ai.FeatureQuotaStatus(
      feature = com.example.ai.GeminiFeatureCategory.AI_VOICE_TUTOR,
      usedAmount = 48,
      maxAllowed = 60,
      unitLabel = "mins",
      periodLabel = "Monthly"
    )
    assertEquals(com.example.ai.QuotaWarningLevel.WARNING_80_PERCENT, status80.warningLevel)
    assertTrue(status80.friendlyWarningMessage?.contains("80%") == true)

    val status95 = com.example.ai.FeatureQuotaStatus(
      feature = com.example.ai.GeminiFeatureCategory.AI_VOICE_TUTOR,
      usedAmount = 58,
      maxAllowed = 60,
      unitLabel = "mins",
      periodLabel = "Monthly"
    )
    assertEquals(com.example.ai.QuotaWarningLevel.WARNING_95_PERCENT, status95.warningLevel)
  }
}

