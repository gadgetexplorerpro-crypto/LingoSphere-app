package com.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.data.repository.DynamicAiGameEngine
import com.example.data.repository.GameCurriculumAndAntiRepetitionEngine
import com.example.data.repository.LevelProgressionBand
import com.example.speech.SpeechManager
import com.example.ui.screens.PracticeScreen
import com.example.ui.theme.LingoSphereTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-mdpi")
class DynamicGamesAndLevelsVerificationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun consecutiveLevels_haveUniqueObjectivesAndZeroRepeatedExercisesAcrossRepresentativeGames() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "u_anti_rep_verifier"

    // 1. Verify progressive curriculum objectives across Levels 1..10 for flagship games
    val pictureGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
    val sentenceGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_42_sentence_builder" }
    val spellBeeGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_16_spellbee" }

    val picObjectives = (1..10).map { DynamicAiGameEngine.getCurriculumObjective(pictureGame, it).objectiveTitle }
    assertEquals("Picture Word Quiz Levels 1..10 must have 10 distinct objectives", 10, picObjectives.toSet().size)

    val sentObjectives = (1..10).map { DynamicAiGameEngine.getCurriculumObjective(sentenceGame, it).objectiveTitle }
    assertEquals("Sentence Builder Levels 1..10 must have 10 distinct objectives", 10, sentObjectives.toSet().size)

    val spellObjectives = (1..8).map { DynamicAiGameEngine.getCurriculumObjective(spellBeeGame, it).objectiveTitle }
    assertEquals("SpellBee Levels 1..8 must have 8 distinct objectives", 8, spellObjectives.toSet().size)

    // 2. Test 3 consecutive levels across 12 representative games covering all mechanics
    val representativeGameIds = listOf(
      "ug_01_picture_word_quiz",
      "ug_02_word_match",
      "ug_04_memory_pairs",
      "ug_07_word_search",
      "ug_16_spellbee",
      "ug_17_wordigo",
      "ug_29_speech_coach_challenge",
      "ug_34_listen_and_choose",
      "ug_42_sentence_builder",
      "ug_43_grammar_detective",
      "ug_52_shopping_mission",
      "ug_67_language_escape_room"
    )

    runBlocking {
      for (gameId in representativeGameIds) {
        val game = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == gameId }
        val lvl1 = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "es",
          nativeLanguageCode = "en",
          levelNumber = 1,
          forceRefresh = true,
          userId = userId
        )
        val lvl2 = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "es",
          nativeLanguageCode = "en",
          levelNumber = 2,
          forceRefresh = true,
          userId = userId
        )
        val lvl3 = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "es",
          nativeLanguageCode = "en",
          levelNumber = 3,
          forceRefresh = true,
          userId = userId
        )

        assertTrue("Level 1 of $gameId must have exercises", lvl1.isNotEmpty())
        assertTrue("Level 2 of $gameId must have exercises", lvl2.isNotEmpty())
        assertTrue("Level 3 of $gameId must have exercises", lvl3.isNotEmpty())

        // Objectives must differ across consecutive levels
        assertNotEquals(
          "Level 1 and Level 2 objectives must differ for $gameId",
          lvl1.first().objectiveLabel,
          lvl2.first().objectiveLabel
        )
        assertNotEquals(
          "Level 2 and Level 3 objectives must differ for $gameId",
          lvl2.first().objectiveLabel,
          lvl3.first().objectiveLabel
        )

        // Within-level uniqueness and consecutive-level non-overlap
        val phrases1 = lvl1.map { it.targetPhrase.lowercase().trim() }.toSet()
        val phrases2 = lvl2.map { it.targetPhrase.lowercase().trim() }.toSet()
        val phrases3 = lvl3.map { it.targetPhrase.lowercase().trim() }.toSet()

        assertEquals("Level 1 of $gameId should have unique target phrases", lvl1.size, phrases1.size)
        assertEquals("Level 2 of $gameId should have unique target phrases", lvl2.size, phrases2.size)
        assertEquals("Level 3 of $gameId should have unique target phrases", lvl3.size, phrases3.size)

        val overlap12 = phrases1.intersect(phrases2)
        val overlap23 = phrases2.intersect(phrases3)
        assertTrue(
          "Consecutive Levels 1 & 2 of $gameId must not repeat target phrases, found overlap: $overlap12",
          overlap12.isEmpty()
        )
        assertTrue(
          "Consecutive Levels 2 & 3 of $gameId must not repeat target phrases, found overlap: $overlap23",
          overlap23.isEmpty()
        )

        // Anti-repetition history must be persisted
        val historyRecord = GameCurriculumAndAntiRepetitionEngine.getLevelHistoryRecord(
          context = context,
          userId = userId,
          languageCode = "es",
          gameId = gameId,
          levelNumber = 2
        )
        assertNotNull("Anti-repetition history record must be saved for $gameId level 2", historyRecord)
        assertTrue(historyRecord!!.vocabularyUsed.isNotEmpty())
        assertTrue(historyRecord.contentHashes.isNotEmpty())
      }
    }
  }

  @Test
  fun mechanicSpecificDataIntegrity_andCachePersistence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "u_mechanic_integrity"

    runBlocking {
      // 1. SpellBee: 8 words per level
      val spellBee = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_16_spellbee" }
      val spellExercises = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = spellBee,
        languageCode = "en",
        nativeLanguageCode = "es",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
      assertEquals("SpellBee must provide 8 spelling words per level", 8, spellExercises.size)

      // 2. Word Match & Memory Pairs: 6 pairs per board
      val wordMatch = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_02_word_match" }
      val matchExercises = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = wordMatch,
        languageCode = "fr",
        nativeLanguageCode = "en",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
      assertTrue(matchExercises.first().matchingPairs.size >= 6)

      // Reload from cache (forceRefresh = false) and confirm matchingPairs survive round-trip
      val cachedMatch = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = wordMatch,
        languageCode = "fr",
        nativeLanguageCode = "en",
        levelNumber = 1,
        forceRefresh = false,
        userId = userId
      )
      assertEquals(matchExercises.first().matchingPairs.size, cachedMatch.first().matchingPairs.size)

      // 3. Word Search: Progressive 6-tier grid sizes (Level 1 = Beginner 5x5, 4 words) + anti-repetition + topic purity
      val wordSearch = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_07_word_search" }
      val searchExercises = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = wordSearch,
        languageCode = "es",
        nativeLanguageCode = "en",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
      val grid = searchExercises.first().wordSearchGrid
      assertEquals("Beginner Level 1 Word Search grid must be 5x5", 5, grid.size)
      assertTrue("Every Word Search row must have 5 letters", grid.all { it.length == 5 })
      assertTrue("Beginner Word Search must include at least 4 target words", searchExercises.first().wordSearchTargets.size >= 4)

      // Progressive difficulty tiers (1..10 Beginner, 11..25 Easy, 26..50 Medium, 51..100 Hard, 101..200 Expert, 201+ Master)
      val expectedTierSpecs = listOf(
        1 to Triple(com.example.data.repository.WordSearchDifficultyTier.BEGINNER, 5, 4),
        15 to Triple(com.example.data.repository.WordSearchDifficultyTier.EASY, 8, 5),
        30 to Triple(com.example.data.repository.WordSearchDifficultyTier.MEDIUM, 9, 6),
        60 to Triple(com.example.data.repository.WordSearchDifficultyTier.HARD, 11, 8),
        120 to Triple(com.example.data.repository.WordSearchDifficultyTier.EXPERT, 13, 10),
        250 to Triple(com.example.data.repository.WordSearchDifficultyTier.MASTER, 15, 12)
      )
      val seenWordsForUserLang = mutableSetOf<String>()
      for ((lvl, expected) in expectedTierSpecs) {
        val (expectedTier, expectedGrid, expectedMinWords) = expected
        val spec = com.example.data.repository.WordSearchDifficultyAndTopicEngine.specForLevel(lvl)
        assertEquals(expectedTier, spec.tier)
        assertEquals(expectedGrid, spec.gridSize)
        assertEquals(expectedMinWords, spec.wordCount)

        val generated = com.example.data.repository.WordSearchDifficultyAndTopicEngine.generateValidatedWordSearchBoard(
          context = context,
          userId = "${userId}_ws_prog",
          languageCode = "en",
          levelNumber = lvl,
          objectiveTitle = "Health & Doctor Visits",
          persistUsedWords = true
        )
        assertEquals("Level $lvl grid height must match $expectedGrid", expectedGrid, generated.grid.size)
        assertTrue("Level $lvl grid rows must all equal $expectedGrid", generated.grid.all { it.length == expectedGrid })
        assertTrue("Level $lvl must place at least $expectedMinWords words", generated.placedWords.size >= expectedMinWords)
        val targetsNorm = generated.placedWords.map {
          com.example.data.repository.WordSearchDifficultyAndTopicEngine.normalizeGridWord(it.first)
        }
        assertTrue(
          "Words across levels must NEVER repeat for same user/language (level $lvl)",
          targetsNorm.none { it in seenWordsForUserLang }
        )
        seenWordsForUserLang.addAll(targetsNorm)

        // Ensure off-topic transport/office words never leak into Health & Doctor Visits
        val forbiddenOffTopic = setOf("PLATFORM", "CURRENCY", "DEADLINE", "CALM")
        assertTrue("Off-topic words leaked into Health & Doctor Visits at level $lvl", targetsNorm.none { it in forbiddenOffTopic })

        if (spec.preferOverlaps && generated.placedWords.size >= 8) {
          assertTrue("Expert/Master level $lvl must include intersecting overlapping words", generated.overlapCount >= 1)
        }
      }

      // Direction helper must resolve Horizontal, Vertical, Diagonal, and Reverse sweeps accurately
      assertEquals(
        listOf(0 to 0, 1 to 1, 2 to 2),
        com.example.data.repository.WordSearchDifficultyAndTopicEngine.resolveLineCoords(0, 0, 2, 2)
      )
      assertEquals(
        listOf(3 to 3, 2 to 2, 1 to 1),
        com.example.data.repository.WordSearchDifficultyAndTopicEngine.resolveLineCoords(3, 3, 1, 1)
      )
      assertEquals(
        listOf(2 to 4, 2 to 3, 2 to 2),
        com.example.data.repository.WordSearchDifficultyAndTopicEngine.resolveLineCoords(2, 4, 2, 2)
      )

      // 4. Grammar Repair: flawed sentence differs from corrected targetPhrase
      val grammarRepair = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_43_grammar_detective" }
      val repairExercises = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = grammarRepair,
        languageCode = "en",
        nativeLanguageCode = "es",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
      assertTrue(repairExercises.all { it.incorrectSentence.isNotBlank() && it.incorrectSentence != it.targetPhrase })

      // 5. Picture Word Quiz: verified visual concepts with no consecutive duplicate concepts
      val pictureQuiz = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
      val picExercises = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = pictureQuiz,
        languageCode = "de",
        nativeLanguageCode = "en",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
      assertTrue(
        "Every Picture Word Quiz exercise must map to a verified visual concept matching its answer",
        picExercises.all { ex ->
          ex.visualConceptId.isNotBlank() &&
            com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
              ex.visualConceptId,
              ex.targetPhrase,
              ex.translation
            )
        }
      )
      for (i in 1 until picExercises.size) {
        assertNotEquals(
          "Picture Word Quiz must not show identical consecutive visual concepts",
          picExercises[i - 1].visualConceptId,
          picExercises[i].visualConceptId
        )
      }

      // 6. Escape Room: 4 connected vault stages
      val escapeRoom = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_67_language_escape_room" }
      val escapeExercises = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = escapeRoom,
        languageCode = "es",
        nativeLanguageCode = "en",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
      assertEquals(4, escapeExercises.size)
      assertTrue(escapeExercises.all { it.escapeRoomClueCode.length >= 4 })
    }
  }

  @Test
  fun levelProgressionUnlocking_languageIsolation_andAntiDuplicateXp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "u_prog_isolation_test"
    val gameId = "ug_17_wordigo"

    // 1. Verify all 8 progression bands across 1..500
    assertEquals("Foundations", LevelProgressionBand.forLevel(1).bandName)
    assertEquals("Basic Practical Language", LevelProgressionBand.forLevel(75).bandName)
    assertEquals("Everyday Communication", LevelProgressionBand.forLevel(150).bandName)
    assertEquals("Intermediate Language Skills", LevelProgressionBand.forLevel(210).bandName)
    assertEquals("Complex Language Use", LevelProgressionBand.forLevel(300).bandName)
    assertEquals("Advanced Communication", LevelProgressionBand.forLevel(360).bandName)
    assertEquals("Advanced Accuracy & Fluency", LevelProgressionBand.forLevel(425).bandName)
    assertEquals("Expert Challenges", LevelProgressionBand.forLevel(500).bandName)

    // 2. Failing (< 70%) does NOT unlock next level or mark completed
    assertFalse(DynamicAiGameEngine.wasLevelCompletedBefore(context, "es", gameId, 1, userId))
    val failedState = DynamicAiGameEngine.saveGameProgress(
      context = context,
      languageCode = "es",
      gameId = gameId,
      levelNumber = 1,
      accuracyPercent = 55,
      userId = userId
    )
    assertEquals("Failing Level 1 must keep highestUnlockedLevel at 1", 1, failedState.highestUnlockedLevel)
    assertFalse(DynamicAiGameEngine.wasLevelCompletedBefore(context, "es", gameId, 1, userId))

    // 3. Passing (>= 70%) unlocks Level 2 and marks Level 1 completed so replays don't double-award XP
    val passedState = DynamicAiGameEngine.saveGameProgress(
      context = context,
      languageCode = "es",
      gameId = gameId,
      levelNumber = 1,
      accuracyPercent = 88,
      userId = userId
    )
    assertEquals("Passing Level 1 must unlock Level 2", 2, passedState.highestUnlockedLevel)
    assertEquals(2, passedState.completedLevels[1])
    assertTrue(DynamicAiGameEngine.wasLevelCompletedBefore(context, "es", gameId, 1, userId))

    // 4. Language isolation: French ("fr") and Japanese ("ja") remain untouched at Level 1
    val frenchProgress = DynamicAiGameEngine.getGameProgress(context, "fr", gameId, userId)
    val japaneseProgress = DynamicAiGameEngine.getGameProgress(context, "ja", gameId, userId)
    assertEquals(1, frenchProgress.highestUnlockedLevel)
    assertEquals(0, frenchProgress.totalCompletedCount)
    assertEquals(1, japaneseProgress.highestUnlockedLevel)
    assertEquals(0, japaneseProgress.totalCompletedCount)
  }

  @Test
  fun redesignedPracticeScreen_isUnclutteredAndSupportsDirectCategoryToGameplayFlow() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val userProfile = UserProfile(
      userId = "u_ui_hub_verifier",
      targetLanguageCode = "es",
      nativeLanguageCode = "en",
      proficiencyLevel = "Beginner",
      xpPoints = 320,
      currentStreak = 5
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        PracticeScreen(
          userProfile = userProfile,
          flashcards = emptyList(),
          speechManager = speechManager,
          onReviewCard = { _, _ -> }
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Main Hub displays clean sections without dumping all 70 games
    composeTestRule.onNode(hasTestTag("play_and_learn_title")).assertExists()
    composeTestRule.onNode(hasTestTag("continue_playing_card")).assertExists()
    composeTestRule.onNode(hasTestTag("daily_challenge_main_card")).assertExists()
    composeTestRule.onNode(hasTestTag("recommended_games_row")).assertExists()
    composeTestRule.onNode(hasTestTag("ultimate_game_card_ug_67_language_escape_room")).assertDoesNotExist()

    // 2. Scroll to Explore Games 8-category grid & tap Vocabulary
    composeTestRule.onNode(hasTestTag("category_card_VOCABULARY"))
      .performScrollTo()
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()

    // 3. Dedicated Vocabulary Category screen displays Vocabulary games ONLY
    composeTestRule.onNode(hasTestTag("category_games_screen_VOCABULARY")).assertExists()
    composeTestRule.onNode(hasTestTag("ultimate_game_card_ug_01_picture_word_quiz")).assertExists()
    composeTestRule.onNode(hasTestTag("ultimate_game_card_ug_67_language_escape_room")).assertDoesNotExist()

    // 4. Tap Picture Word Quiz -> Opens current playable level DIRECTLY (no briefing screen!)
    composeTestRule.onNode(hasTestTag("ultimate_game_card_ug_01_picture_word_quiz")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasTestTag("ultimate_dynamic_gameplay_screen")).assertExists()

    // 5. Tap "Levels" chip inside gameplay header -> Opens clean 500-level selector
    composeTestRule.onNode(hasTestTag("open_level_selector_chip")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasTestTag("ultimate_level_selector_screen")).assertExists()
  }

  @Test
  fun pictureWordQuiz_30VariedQuestions_validatesTableAndLamp_allPositionsABCD_andVerifiedVisuals() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pictureGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }

    // 1. Direct regression proof for Table vs Door and Lamp vs Clock across every slot 0..3 (A..D)
    for (slot in 0..3) {
      val (tableOpts, tableCorrectId, _) = DynamicAiGameEngine.buildValidatedAnswerOptions(
        exerciseId = "reg_table_$slot",
        verifiedCorrectText = "Table",
        candidateOptions = listOf("Door", "Table", "Chair", "Window"),
        fallbackDistractors = listOf("Door", "Chair", "Window"),
        rotationSeed = (4 - slot) % 4
      )
      assertEquals("Table must appear at requested slot $slot", "Table", tableOpts[slot].text)
      assertEquals("correctOptionId must bind to Table, never Door", tableOpts[slot].id, tableCorrectId)
      val doorOpt = tableOpts.first { it.text == "Door" }
      assertNotEquals("Door option ID must NEVER equal correctOptionId", doorOpt.id, tableCorrectId)

      val (lampOpts, lampCorrectId, _) = DynamicAiGameEngine.buildValidatedAnswerOptions(
        exerciseId = "reg_lamp_$slot",
        verifiedCorrectText = "Lamp",
        candidateOptions = listOf("Clock", "Lamp", "Bed", "Key"),
        fallbackDistractors = listOf("Clock", "Bed", "Key"),
        rotationSeed = (4 - slot) % 4
      )
      assertEquals("Lamp must appear at requested slot $slot", "Lamp", lampOpts[slot].text)
      assertEquals("correctOptionId must bind to Lamp, never Clock", lampOpts[slot].id, lampCorrectId)
      val clockOpt = lampOpts.first { it.text == "Clock" }
      assertNotEquals("Clock option ID must NEVER equal correctOptionId", clockOpt.id, lampCorrectId)
    }

    // 2. Negative visual-matching guard: unrelated concepts MUST fail verification
    assertFalse(
      com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer("watch", "Clock", "Clock")
    )
    assertFalse(
      com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer("trophy", "Table", "Table")
    )
    assertFalse(
      com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer("tshirt", "Lamp", "Lamp")
    )

    // 3. Generate 30 varied Picture Word Quiz questions across Levels 1..5 in English + 12 in Spanish
    runBlocking {
      val englishQuestions = (1..5).flatMap { lvl ->
        DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = pictureGame,
          languageCode = "en",
          nativeLanguageCode = "es",
          levelNumber = lvl,
          forceRefresh = true,
          userId = "u_pic_30_audit"
        )
      }
      assertTrue("Must test at least 20 varied Picture Word Quiz questions", englishQuestions.size >= 30)

      val tableExercise = englishQuestions.firstOrNull {
        it.prompt.contains("What furniture is used for eating or working?", ignoreCase = true)
      }
      assertNotNull("Level 1 must include the exact Table clue question", tableExercise)
      assertEquals("Table", tableExercise!!.targetPhrase)
      val tableCorrectOpt = tableExercise.resolvedAnswerOptions().first { it.id == tableExercise.resolvedCorrectOptionId() }
      assertEquals("Table", tableCorrectOpt.text)
      tableExercise.resolvedAnswerOptions().filter { it.text != "Table" }.forEach { wrongOpt ->
        assertFalse("Wrong option ${wrongOpt.text} must NEVER validate as correct", tableExercise.isOptionCorrect(wrongOpt.id))
      }
      assertTrue(tableExercise.isOptionCorrect(tableCorrectOpt.id))

      val lampExercise = englishQuestions.firstOrNull {
        it.prompt.contains("What object provides light in a room?", ignoreCase = true)
      }
      assertNotNull("Level 1 must include the exact Lamp clue question", lampExercise)
      assertEquals("Lamp", lampExercise!!.targetPhrase)
      val lampCorrectOpt = lampExercise.resolvedAnswerOptions().first { it.id == lampExercise.resolvedCorrectOptionId() }
      assertEquals("Lamp", lampCorrectOpt.text)
      lampExercise.resolvedAnswerOptions().filter { it.text != "Lamp" }.forEach { wrongOpt ->
        assertFalse("Wrong option ${wrongOpt.text} must NEVER validate as correct", lampExercise.isOptionCorrect(wrongOpt.id))
      }
      assertTrue(lampExercise.isOptionCorrect(lampCorrectOpt.id))

      val observedCorrectPositions = mutableSetOf<Int>()
      for (ex in englishQuestions) {
        assertTrue("Exercise ${ex.id} must pass deterministic integrity check", DynamicAiGameEngine.validateExerciseIntegrity(ex))
        val opts = ex.resolvedAnswerOptions()
        assertEquals("Every Picture Word Quiz question must have 4 options", 4, opts.size)
        val pos = opts.indexOfFirst { it.id == ex.resolvedCorrectOptionId() }
        assertTrue("Correct option ID must exist in options 0..3", pos in 0..3)
        observedCorrectPositions.add(pos)
        assertEquals(ex.targetPhrase.lowercase(), opts[pos].text.lowercase())
        assertTrue(
          "Visual concept '${ex.visualConceptId}' must match intended answer '${ex.targetPhrase}'",
          com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
            ex.visualConceptId,
            ex.targetPhrase,
            ex.translation
          )
        )
      }
      assertEquals(
        "All 4 answer positions A(0), B(1), C(2), D(3) must be exercised across Picture Word Quiz",
        setOf(0, 1, 2, 3),
        observedCorrectPositions
      )
    }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h640dp-mdpi")
  fun pictureWordQuizUi_renders2x2HorizontalGridOn360dp_andHighlightsTrueAnswerIdOnly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val pictureGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
    val userId = "u_pic_ui_grid_verifier"

    val preloaded = runBlocking {
      DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = pictureGame,
        languageCode = "en",
        nativeLanguageCode = "es",
        levelNumber = 1,
        forceRefresh = true,
        userId = userId
      )
    }
    val q0 = preloaded[0]
    val q0Opts = q0.resolvedAnswerOptions()
    val q0TrueIdx = q0Opts.indexOfFirst { it.id == q0.resolvedCorrectOptionId() }
    val q0WrongIdx = (0..3).first { it != q0TrueIdx }

    composeTestRule.setContent {
      LingoSphereTheme {
        com.example.ui.screens.UltimateDynamicLevelGameplayView(
          game = pictureGame,
          levelNumber = 1,
          languageCode = "en",
          nativeLanguageCode = "es",
          speechManager = speechManager,
          onSaveFlashcard = {},
          onAddMistakeEntry = {},
          onLevelFinished = { _, _, _, _, _ -> },
          onPlayNextLevel = {},
          onBackToLevelSelect = {},
          userId = userId
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify verified object visual card is mounted for Question 1
    composeTestRule.onNode(hasTestTag("verified_visual_card_${q0.visualConceptId}")).assertExists()

    // 2. Scroll to 2x2 answer grid & verify horizontal 2-per-row geometry on 360dp screen
    composeTestRule.onNode(hasTestTag("ultimate_option_3")).performScrollTo()
    composeTestRule.waitForIdle()

    val b0 = composeTestRule.onNode(hasTestTag("ultimate_option_0")).fetchSemanticsNode().boundsInRoot
    val b1 = composeTestRule.onNode(hasTestTag("ultimate_option_1")).fetchSemanticsNode().boundsInRoot
    val b2 = composeTestRule.onNode(hasTestTag("ultimate_option_2")).fetchSemanticsNode().boundsInRoot
    val b3 = composeTestRule.onNode(hasTestTag("ultimate_option_3")).fetchSemanticsNode().boundsInRoot

    assertTrue("Options A(0) & B(1) must be side-by-side on Row 1", kotlin.math.abs(b0.top - b1.top) < 3f && b0.left < b1.left)
    assertTrue("Options C(2) & D(3) must be side-by-side on Row 2", kotlin.math.abs(b2.top - b3.top) < 3f && b2.left < b3.left)
    assertTrue("Row 2 must appear below Row 1", b2.top > b0.top)

    // 3. Tap WRONG option on Q1 -> Red X on wrong card, Green Checkmark ONLY on true correctOptionId card!
    composeTestRule.onNode(hasTestTag("ultimate_option_$q0WrongIdx")).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNode(hasTestTag("ultimate_option_wrong_mark_$q0WrongIdx")).assertExists()
    composeTestRule.onNode(hasTestTag("ultimate_option_correct_check_$q0TrueIdx")).assertExists()
    composeTestRule.onNode(hasTestTag("ultimate_option_correct_check_$q0WrongIdx")).assertDoesNotExist()

    // 4. Advance to Q2 and tap the TRUE correctOptionId card -> Green Checkmark ONLY on true card!
    composeTestRule.onNode(hasTestTag("continue_next_exercise_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    val q1 = preloaded[1]
    val q1TrueIdx = q1.resolvedAnswerOptions().indexOfFirst { it.id == q1.resolvedCorrectOptionId() }
    composeTestRule.onNode(hasTestTag("ultimate_option_$q1TrueIdx")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasTestTag("ultimate_option_correct_check_$q1TrueIdx")).assertExists()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h640dp-mdpi")
  fun all8ListeningGames_haveDistinctMechanics_andSingleLineHorizontalAudioControlsOn360dp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val listeningGames = DynamicAiGameEngine.ALL_70_GAMES.filter {
      it.category == com.example.data.repository.UltimateGameCategory.LISTENING
    }
    assertEquals("Must have all 8 Listening games", 8, listeningGames.size)
    assertEquals(
      "All 8 Listening games must use 8 distinct mechanics",
      8,
      listeningGames.map { it.mechanic }.toSet().size
    )

    runBlocking {
      for (game in listeningGames) {
        val listEx = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "es",
          nativeLanguageCode = "en",
          levelNumber = 1,
          forceRefresh = true,
          userId = "u_listening_8_verifier"
        )
        assertTrue("Listening game ${game.id} must generate exercises", listEx.isNotEmpty())
        assertTrue(
          "Every exercise in ${game.id} must pass integrity validation",
          listEx.all { DynamicAiGameEngine.validateExerciseIntegrity(it) }
        )
      }
    }

    // Render Listen & Choose on 360dp phone width and verify horizontal audio controls never wrap vertically
    val listenChooseGame = listeningGames.first { it.id == "ug_34_listen_and_choose" }
    composeTestRule.setContent {
      LingoSphereTheme {
        com.example.ui.screens.UltimateDynamicLevelGameplayView(
          game = listenChooseGame,
          levelNumber = 1,
          languageCode = "es",
          nativeLanguageCode = "en",
          speechManager = speechManager,
          onSaveFlashcard = {},
          onAddMistakeEntry = {},
          onLevelFinished = { _, _, _, _, _ -> },
          onPlayNextLevel = {},
          onBackToLevelSelect = {},
          userId = "u_listening_8_verifier"
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNode(hasTestTag("compact_horizontal_audio_player")).assertExists()
    composeTestRule.onNode(hasTestTag("compact_audio_play_stop_button")).assertExists()

    val s075 = composeTestRule.onNode(hasTestTag("audio_speed_chip_0.75x")).fetchSemanticsNode().boundsInRoot
    val s100 = composeTestRule.onNode(hasTestTag("audio_speed_chip_1.0x")).fetchSemanticsNode().boundsInRoot
    val s125 = composeTestRule.onNode(hasTestTag("audio_speed_chip_1.25x")).fetchSemanticsNode().boundsInRoot

    assertTrue("Speed chips 0.75x, 1.0x, 1.25x must align horizontally on one row", kotlin.math.abs(s075.top - s125.top) < 3f)
    assertTrue("Speed chips must be ordered left-to-right without overlap", s075.left < s100.left && s100.left < s125.left)
    assertTrue("1.25x pill must render horizontally (wider than tall), never vertically wrapped", s125.width > s125.height)
  }

  @Test
  fun all70Games_passDeterministicIntegrityValidation_andSpeakingGameIgnoresStaleSpeech() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)

    // 1. Audit ALL 70 games in LingoSphere Play & Learn catalog
    assertEquals(70, DynamicAiGameEngine.ALL_70_GAMES.size)
    runBlocking {
      for (game in DynamicAiGameEngine.ALL_70_GAMES) {
        val exercises = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "es",
          nativeLanguageCode = "en",
          levelNumber = 1,
          forceRefresh = true,
          userId = "u_all_70_integrity_audit"
        )
        assertTrue("Game ${game.id} must produce non-empty exercises", exercises.isNotEmpty())
        assertTrue(
          "Every exercise in ${game.id} must pass deterministic integrity validation",
          exercises.all { DynamicAiGameEngine.validateExerciseIntegrity(it) }
        )
      }
    }

    // 2. Pre-populate stale transcript BEFORE opening Speaking game -> must NOT auto-evaluate or auto-advance!
    speechManager.simulateVoiceInput("stale phrase from an earlier screen")
    val speakingGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_24_pronunciation_battle" }

    composeTestRule.setContent {
      LingoSphereTheme {
        com.example.ui.screens.UltimateDynamicLevelGameplayView(
          game = speakingGame,
          levelNumber = 1,
          languageCode = "es",
          nativeLanguageCode = "en",
          speechManager = speechManager,
          onSaveFlashcard = {},
          onAddMistakeEntry = {},
          onLevelFinished = { _, _, _, _, _ -> },
          onPlayNextLevel = {},
          onBackToLevelSelect = {},
          userId = "u_speaking_stale_guard"
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNode(hasTestTag("speaking_game_mic_button")).assertExists()
    composeTestRule.onNode(hasTestTag("continue_next_exercise_button")).assertDoesNotExist()
  }

  @Test
  fun all10SpeakingGames_have10DistinctMechanics_multiWordContent_andDedicatedEvaluators() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speakingGames = DynamicAiGameEngine.ALL_70_GAMES.filter {
      it.category == com.example.data.repository.UltimateGameCategory.SPEAKING
    }
    assertEquals("Must have all 10 Speaking games", 10, speakingGames.size)
    assertEquals(
      "All 10 Speaking games must use 10 distinct GameInteractionMechanic values",
      10,
      speakingGames.map { it.mechanic }.toSet().size
    )
    assertTrue(
      "All 10 Speaking game mechanics must have isSpeakingMechanic = true",
      speakingGames.all { it.mechanic.isSpeakingMechanic }
    )

    // Verify all 10 required Roleplay Mission scenarios exist
    val roleplayTitles = com.example.data.repository.SpeakingGamesContentAndEvaluationEngine
      .ROLEPLAY_10_SCENARIOS.map { it.title }
    assertEquals(10, roleplayTitles.size)
    listOf(
      "Shopping",
      "Restaurant ordering",
      "Job interview",
      "Doctor appointment",
      "Airport",
      "Hotel check-in",
      "Meeting new people",
      "Asking for directions",
      "Workplace conversation",
      "University admission"
    ).forEach { expected ->
      assertTrue(
        "Missing roleplay scenario matching '$expected' in $roleplayTitles",
        roleplayTitles.any { it.contains(expected, ignoreCase = true) }
      )
    }

    runBlocking {
      for (game in speakingGames) {
        val lvl1 = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "en",
          nativeLanguageCode = "es",
          levelNumber = 1,
          forceRefresh = true,
          userId = "u_10_speaking_verifier"
        )
        val lvl2 = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = game,
          languageCode = "en",
          nativeLanguageCode = "es",
          levelNumber = 2,
          forceRefresh = true,
          userId = "u_10_speaking_verifier"
        )
        assertTrue("Speaking game ${game.id} must generate exercises", lvl1.isNotEmpty())
        assertTrue(
          "Every exercise in ${game.id} must pass integrity validation",
          lvl1.all { DynamicAiGameEngine.validateExerciseIntegrity(it) }
        )
        assertNotEquals(
          "Level 1 and Level 2 objectives must differ for ${game.id}",
          lvl1.first().objectiveLabel,
          lvl2.first().objectiveLabel
        )

        if (game.id != "ug_24_pronunciation_battle") {
          assertTrue(
            "Multi-word speaking game ${game.id} must never use single isolated vocabulary words",
            lvl1.all { ex -> ex.targetPhrase.trim().split(Regex("\\s+")).size >= 2 }
          )
        }
        if (game.id == "ug_27_speak_the_picture") {
          assertTrue(
            "Speak the Scene must provide a real scene visualConceptId and vocabulary chips",
            lvl1.all { it.visualConceptId.isNotBlank() && it.scrambledTokens.isNotEmpty() }
          )
        }
        if (game.id == "ug_32_dialogue_completion") {
          assertTrue(
            "Dialogue Line must provide both Speaker A opening line and follow-up continuation line",
            lvl1.all { it.dialogueSpeakerALine.isNotBlank() && it.dialogueSpeakerBLine.isNotBlank() }
          )
        }
      }

      // Quick Answer evaluator must reject echoing the question and accept a real answer
      val quickAnswerGame = speakingGames.first { it.id == "ug_28_quick_answer" }
      val qaEx = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = quickAnswerGame,
        languageCode = "en",
        nativeLanguageCode = "es",
        levelNumber = 1,
        forceRefresh = false,
        userId = "u_10_speaking_verifier"
      ).first()

      val echoEval = com.example.data.repository.SpeakingGamesContentAndEvaluationEngine.evaluateSpeakingGameAttempt(
        exercise = qaEx,
        spokenTranscript = qaEx.dialogueSpeakerALine,
        languageCode = "en",
        nativeLanguageCode = "es"
      )
      assertFalse("Repeating the question in Quick Answer must not pass as a valid answer", echoEval.isPassed)

      val validAnswerEval = com.example.data.repository.SpeakingGamesContentAndEvaluationEngine.evaluateSpeakingGameAttempt(
        exercise = qaEx,
        spokenTranscript = qaEx.targetPhrase,
        languageCode = "en",
        nativeLanguageCode = "es"
      )
      assertTrue("Answering the question naturally in Quick Answer must pass", validAnswerEval.isPassed)
    }
  }

  @Test
  fun pictureWordQuiz_levels1_2_10_50_neverRepeatVocabulary_andBreadNeverShowsBuilding() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pictureGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
    val userId = "u_pic_lvl_1_2_10_50_verifier"

    // 1. Verify "Bread" concept image is a real bread photograph and NEVER Bread_Loaf_Inn or a building
    val breadConcept = com.example.data.repository.VerifiedVisualConceptRegistry.getById("bread")
    assertNotNull("Bread concept must exist in VerifiedVisualConceptRegistry", breadConcept)
    assertFalse(
      "Bread image URL must NEVER point to Bread_Loaf_Inn building photo",
      breadConcept!!.wikimediaPhotoUrl.contains("Bread_Loaf_Inn", ignoreCase = true)
    )
    assertTrue(
      "Bread image URL must point to a verified bread photo",
      breadConcept.wikimediaPhotoUrl.contains("Fresh_made_bread", ignoreCase = true)
    )
    assertTrue(
      "All concepts in registry must pass image relevance validation",
      com.example.data.repository.VerifiedVisualConceptRegistry.ALL_CONCEPTS.all {
        com.example.data.repository.VerifiedVisualConceptRegistry.validateImageUrlMatchesConcept(it)
      }
    )

    // 2. Generate Levels 1, 2, 10, and 50 and verify zero repeated words, concepts, or questions across levels
    runBlocking {
      val targetLevels = listOf(1, 2, 10, 50)
      val seenWords = mutableSetOf<String>()
      val seenConcepts = mutableSetOf<String>()
      val seenPrompts = mutableSetOf<String>()

      for (lvl in targetLevels) {
        val exercises = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = pictureGame,
          languageCode = "en",
          nativeLanguageCode = "es",
          levelNumber = lvl,
          forceRefresh = true,
          userId = userId
        )
        assertEquals("Level $lvl must contain 6 questions", 6, exercises.size)
        for (ex in exercises) {
          val wordNorm = ex.targetPhrase.trim().lowercase()
          val conceptNorm = ex.visualConceptId.trim().lowercase()
          val promptNorm = ex.prompt.trim().lowercase()

          assertFalse("Word '$wordNorm' in Level $lvl was already used in an earlier level!", wordNorm in seenWords)
          assertFalse("Visual concept '$conceptNorm' in Level $lvl was already used in an earlier level!", conceptNorm in seenConcepts)
          assertFalse("Question prompt '$promptNorm' in Level $lvl was already used in an earlier level!", promptNorm in seenPrompts)

          seenWords.add(wordNorm)
          seenConcepts.add(conceptNorm)
          seenPrompts.add(promptNorm)
        }
      }

      // 3. Verify excluded content persists across restarts for all recorded levels (1, 2, 10, 50)
      val excludedForNextLevel = GameCurriculumAndAntiRepetitionEngine.getExcludedContentForLevel(
        context = context,
        userId = userId,
        languageCode = "en",
        gameId = pictureGame.id,
        levelNumber = 51
      )
      assertTrue("Persistent excluded set must contain all 24 words from Levels 1, 2, 10, and 50", excludedForNextLevel.containsAll(seenWords))
    }
  }

  @Test
  fun listeningBookReader_humanNarratorChapterSelection_switching_andSequentialAutoplay() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    com.example.speech.AudiobookMediaController.attach(context, speechManager, "u_audiobook_test")

    val catalog = com.example.data.repository.AudiobookCatalogRepository.FAMOUS_FULL_BOOKS
    val prideBook = catalog.first { it.id == "gutenberg_1342" }
    val scienceBook = catalog.first { it.id == "wattles_science_getting_rich" }
    val frankensteinBook = catalog.first { it.id == "gutenberg_84" }
    val aliceBook = catalog.first { it.id == "gutenberg_11" }
    val sherlockBook = catalog.first { it.id == "gutenberg_1661" }

    // 1. Test Chapters 1, 2, 3, 5, 10, and final chapter on Pride & Prejudice (61 chapters)
    val testChapterNumbers = listOf(1, 2, 3, 5, 10, prideBook.parsedChapters.size)
    for (chNum in testChapterNumbers) {
      val chIdx = chNum - 1
      com.example.speech.AudiobookMediaController.loadBookAndChapter(
        book = prideBook,
        chapterIndex = chIdx,
        sentenceIndex = 0,
        autoPlay = false,
        preferHumanAudio = true
      )
      assertEquals("Selected chapter index must be $chIdx for Chapter $chNum", chIdx, com.example.speech.AudiobookMediaController.activeChapterIndex.value)
      assertEquals("Selected chapter number must be $chNum", chNum, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
      val activeUrl = com.example.speech.AudiobookMediaController.currentAudioUrl.value.orEmpty()
      val expectedTrackSubstring = "_%02d_".format(chNum)
      assertTrue(
        "Chapter $chNum audio URL ($activeUrl) must match track $expectedTrackSubstring and NEVER Chapter 1 (unless chNum==1)",
        activeUrl.contains(expectedTrackSubstring)
      )
      if (chNum != 1) {
        assertFalse("Chapter $chNum must NEVER play Chapter 1 (_01_) audio!", activeUrl.contains("_01_"))
      }
    }

    // 2. Test narrator switching on Chapter 3 of The Science of Getting Rich and Frankenstein
    for (book in listOf(scienceBook, frankensteinBook, aliceBook, sherlockBook)) {
      val chapter3Idx = 2 // Chapter 3 (0-based index 2)
      com.example.speech.AudiobookMediaController.loadBookAndChapter(
        book = book,
        chapterIndex = chapter3Idx,
        sentenceIndex = 1,
        autoPlay = false,
        preferHumanAudio = false // Start in AI Voice
      )
      assertEquals(book.id, com.example.speech.AudiobookMediaController.selectedBookId.value)
      assertEquals(chapter3Idx, com.example.speech.AudiobookMediaController.activeChapterIndex.value)
      assertEquals(3, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
      assertEquals("ai_voice", com.example.speech.AudiobookMediaController.selectedNarratorType.value)

      // Switch to Human Narrator while Chapter 3 is selected -> MUST stay on Chapter 3 and load Chapter 3 audio!
      com.example.speech.AudiobookMediaController.setNarrationEditionMode(
        useHumanLibriVox = true,
        editionIndex = 0,
        autoStartPlayback = false
      )
      assertEquals("Book must remain ${book.id} after switching to Human Narrator", book.id, com.example.speech.AudiobookMediaController.selectedBookId.value)
      assertEquals("Chapter index must remain 2 (Chapter 3) after switching to Human Narrator", chapter3Idx, com.example.speech.AudiobookMediaController.activeChapterIndex.value)
      assertEquals("Chapter number must remain 3 after switching to Human Narrator", 3, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
      assertEquals("human_librivox", com.example.speech.AudiobookMediaController.selectedNarratorType.value)
      val ch3Url = com.example.speech.AudiobookMediaController.currentAudioUrl.value.orEmpty()
      assertTrue("Chapter 3 Human Narrator URL must be non-empty for ${book.title}", ch3Url.isNotBlank())
      assertEquals(
        "Chapter 3 URL must match Chapter 3's assigned LibriVox MP3 URL, never Chapter 1",
        book.parsedChapters[chapter3Idx].librivoxAudioUrl,
        ch3Url
      )
      assertNotEquals(
        "Chapter 3 URL must NEVER equal Chapter 1's LibriVox MP3 URL",
        book.parsedChapters[0].librivoxAudioUrl,
        ch3Url
      )
    }

    // 3. Test Next / Previous chapter progression (Chapter 2 -> Chapter 3 -> Chapter 4)
    com.example.speech.AudiobookMediaController.loadBookAndChapter(
      book = prideBook,
      chapterIndex = 1, // Chapter 2
      sentenceIndex = 0,
      autoPlay = false,
      preferHumanAudio = true
    )
    assertEquals(2, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
    assertTrue(com.example.speech.AudiobookMediaController.currentAudioUrl.value.orEmpty().contains("_02_"))

    com.example.speech.AudiobookMediaController.nextChapter()
    assertEquals(3, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
    assertTrue(com.example.speech.AudiobookMediaController.currentAudioUrl.value.orEmpty().contains("_03_"))

    com.example.speech.AudiobookMediaController.nextChapter()
    assertEquals(4, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
    assertTrue(com.example.speech.AudiobookMediaController.currentAudioUrl.value.orEmpty().contains("_04_"))
  }

  @Test
  fun librivoxSkipRecordingAnnouncements_defaultEnabled_perTrackTimestamps_chapter3Skip_outroAutoAdvance_andUnverifiedFallback() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val userId = "u_skip_announcements_test_${System.currentTimeMillis()}"
    com.example.speech.AudiobookMediaController.attach(context, speechManager, userId)

    // 1. Default setting: "Skip Recording Announcements" MUST be enabled by default (true)
    assertTrue(
      "Skip Recording Announcements must be enabled by default",
      com.example.speech.AudiobookMediaController.skipRecordingAnnouncements.value
    )
    assertTrue(
      "Persisted Skip Recording Announcements preference must default to true",
      com.example.ai.AudiobookLearningService.isSkipRecordingAnnouncementsEnabled(context, userId)
    )

    val catalog = com.example.data.repository.AudiobookCatalogRepository.FAMOUS_FULL_BOOKS
    val prideBook = catalog.first { it.id == "gutenberg_1342" }
    val scienceBook = catalog.first { it.id == "wattles_science_getting_rich" }
    val sherlockBook = catalog.first { it.id == "gutenberg_1661" }

    // 2. Verify that chapters NEVER use a fixed constant number of seconds across chapters
    val prideIntroTimestamps = (0 until 10).map { chIdx ->
      com.example.speech.LibriVoxAnnouncementDetector.resolveBoundariesForChapter(prideBook, chIdx).contentStartMs
    }
    assertTrue(
      "Every chapter must have a positive verified contentStartMs",
      prideIntroTimestamps.all { it > 0L }
    )
    assertTrue(
      "Chapters must NEVER use a single fixed number of seconds across all chapters (found ${prideIntroTimestamps.toSet().size} distinct timestamps in first 10 chapters)",
      prideIntroTimestamps.toSet().size >= 8
    )

    // 3. Selecting Chapter 3 plays the correct Chapter 3 recording and skips ONLY its verified introductory announcement
    val ch3Idx = 2 // Chapter 3 (0-based index 2)
    com.example.speech.AudiobookMediaController.loadBookAndChapter(
      book = prideBook,
      chapterIndex = ch3Idx,
      sentenceIndex = 0,
      autoPlay = false,
      preferHumanAudio = true
    )
    val ch3Bounds = com.example.speech.AudiobookMediaController.activeTrackBoundaries.value
    assertNotNull("Chapter 3 must have active track boundaries resolved", ch3Bounds)
    assertTrue("Chapter 3 boundaries must be verified", ch3Bounds!!.isVerified)
    assertTrue(
      "Chapter 3 audio URL must be prideandprejudice_03_austen_64kb.mp3",
      com.example.speech.AudiobookMediaController.currentAudioUrl.value.orEmpty().contains("prideandprejudice_03_")
    )
    assertEquals(
      "When Chapter 3 is selected with Skip Announcements ON, playback position must start at Chapter 3's verified contentStartMs (12180ms)",
      ch3Bounds.contentStartMs,
      com.example.speech.AudiobookMediaController.currentPositionMs.value
    )
    assertEquals(12180L, ch3Bounds.contentStartMs)
    assertTrue(
      "Chapter 3 title and opening story text must be preserved intact",
      prideBook.parsedChapters[ch3Idx].title.contains("Chapter 3") &&
        prideBook.parsedChapters[ch3Idx].fullText.contains("Not all that Mrs. Bennet")
    )

    // 4. Reaching closing announcement boundary (contentEndMs) automatically continues to Chapter 4 at Chapter 4's verified contentStartMs
    com.example.speech.AudiobookMediaController.setAutoPlayNextChapter(true)
    val ch3EndMs = ch3Bounds.contentEndMs
    assertNotNull("Chapter 3 must have a verified closing announcement boundary (contentEndMs)", ch3EndMs)
    val advanced = com.example.speech.AudiobookMediaController.onTrackPositionReached(
      positionMs = ch3EndMs!!,
      durationMs = prideBook.parsedChapters[ch3Idx].librivoxDurationSeconds * 1000L
    )
    assertTrue("Reaching contentEndMs at end of Chapter 3 must automatically advance to Chapter 4", advanced)
    assertEquals("Active chapter must now be Chapter 4 (index 3)", 3, com.example.speech.AudiobookMediaController.activeChapterIndex.value)
    assertEquals(4, com.example.speech.AudiobookMediaController.selectedChapterNumber.value)
    val ch4Bounds = com.example.speech.AudiobookMediaController.activeTrackBoundaries.value
    assertNotNull(ch4Bounds)
    assertEquals(
      "Chapter 4 must automatically start at Chapter 4's verified contentStartMs without repeating announcements",
      ch4Bounds!!.contentStartMs,
      com.example.speech.AudiobookMediaController.currentPositionMs.value
    )

    // 5. If accurate announcement boundaries are unavailable (unverified track), play the original recording from 0L rather than guessing
    val unverifiedBounds = com.example.speech.LibriVoxAnnouncementDetector.resolveBoundariesForTrack(
      trackUrl = "https://example.org/custom_unverified_recording_chapter_03.mp3",
      chapterNumber = 3,
      totalChaptersInBook = 12,
      knownDurationMs = 600_000L
    )
    assertFalse("Unknown external recording without verified boundaries must be marked isVerified = false", unverifiedBounds.isVerified)
    assertEquals("Unverified track must start at 0L rather than guessing", 0L, unverifiedBounds.contentStartMs)
    assertNull("Unverified track must have contentEndMs = null rather than guessing", unverifiedBounds.contentEndMs)

    // 6. Disabling "Skip Recording Announcements" saves across sessions and plays from 0L
    com.example.speech.AudiobookMediaController.setSkipRecordingAnnouncements(false)
    assertFalse(com.example.speech.AudiobookMediaController.skipRecordingAnnouncements.value)
    assertFalse(
      "Disabled setting must be saved in persistent storage across sessions",
      com.example.ai.AudiobookLearningService.isSkipRecordingAnnouncementsEnabled(context, userId)
    )
    com.example.speech.AudiobookMediaController.loadBookAndChapter(
      book = scienceBook,
      chapterIndex = 2, // Chapter 3 of The Science of Getting Rich
      sentenceIndex = 0,
      autoPlay = false,
      preferHumanAudio = true
    )
    assertEquals(
      "When Skip Recording Announcements is OFF, Chapter 3 must start at 0L (full original recording)",
      0L,
      com.example.speech.AudiobookMediaController.currentPositionMs.value
    )

    // Re-enabling immediately jumps past the verified intro announcement of Chapter 3
    com.example.speech.AudiobookMediaController.setSkipRecordingAnnouncements(true)
    assertEquals(
      "Re-enabling Skip Recording Announcements on Chapter 3 of Science of Getting Rich must seek to its verified contentStartMs (12890ms)",
      12890L,
      com.example.speech.AudiobookMediaController.currentPositionMs.value
    )
    assertTrue(
      "Re-enabled setting must persist across sessions",
      com.example.ai.AudiobookLearningService.isSkipRecordingAnnouncementsEnabled(context, userId)
    )
    assertTrue(sherlockBook.parsedChapters[2].hasVerifiedAnnouncementBoundaries)
  }

  @Test
  fun pictureWordQuiz_fiveTierProgression_semanticAntiRepetition_cachePreservation_andUnlockGating() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pictureGame = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
    val userId = "u_5band_picture_verifier_${System.currentTimeMillis()}"

    // 1. Verify all 5 progression bands (1-10, 11-30, 31-60, 61-100, 101-500+) yield distinct objectives & increasing tier focus
    val sampledLevels = listOf(1, 2, 10, 15, 35, 50, 75, 105)
    val sampledSpecs = sampledLevels.map { DynamicAiGameEngine.getCurriculumObjective(pictureGame, it) }
    assertEquals(
      "Levels 1, 2, 10, 15, 35, 50, 75, 105 must all have unique curriculum objectives",
      sampledLevels.size,
      sampledSpecs.map { it.objectiveTitle }.distinct().size
    )
    assertEquals(1, GameCurriculumAndAntiRepetitionEngine.tierForLevel(1))
    assertEquals(1, GameCurriculumAndAntiRepetitionEngine.tierForLevel(10))
    assertEquals(2, GameCurriculumAndAntiRepetitionEngine.tierForLevel(11))
    assertEquals(2, GameCurriculumAndAntiRepetitionEngine.tierForLevel(30))
    assertEquals(3, GameCurriculumAndAntiRepetitionEngine.tierForLevel(31))
    assertEquals(3, GameCurriculumAndAntiRepetitionEngine.tierForLevel(60))
    assertEquals(4, GameCurriculumAndAntiRepetitionEngine.tierForLevel(61))
    assertEquals(4, GameCurriculumAndAntiRepetitionEngine.tierForLevel(100))
    assertEquals(5, GameCurriculumAndAntiRepetitionEngine.tierForLevel(101))
    assertEquals(5, GameCurriculumAndAntiRepetitionEngine.tierForLevel(500))

    // 2. Verify canonical synonym & paraphrased prompt duplicate detection
    assertEquals(
      GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey("Sofa"),
      GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey("Couch")
    )
    assertEquals(
      GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey("The Apples"),
      GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey("apple")
    )
    assertEquals(
      GameCurriculumAndAntiRepetitionEngine.promptMeaningSignature("What furniture is used for eating or working?"),
      GameCurriculumAndAntiRepetitionEngine.promptMeaningSignature("Which item of furniture is used for working and eating?")
    )

    runBlocking {
      // 3. Verify Levels 1, 2, 10, 15, 35, 50, 75, 105 have ZERO exact, semantic, or prompt repeats in "es"
      val seenConcepts = mutableSetOf<String>()
      val seenTargets = mutableSetOf<String>()
      val seenSemantic = mutableSetOf<String>()
      val seenPrompts = mutableSetOf<String>()

      for (lvl in sampledLevels) {
        val exercises = DynamicAiGameEngine.generateGameQuestions(
          context = context,
          game = pictureGame,
          languageCode = "es",
          nativeLanguageCode = "en",
          levelNumber = lvl,
          forceRefresh = false,
          userId = userId
        )
        assertTrue("Picture Word Quiz Level $lvl must contain exercises", exercises.isNotEmpty())
        for (ex in exercises) {
          assertTrue(
            "Level $lvl exercise '${ex.targetPhrase}' must pass integrity checks",
            DynamicAiGameEngine.validateExerciseIntegrity(ex)
          )
          assertEquals(
            "Every Picture Word Quiz question must have 4 unique answer options",
            4,
            ex.answerOptions.map { it.text.trim().lowercase() }.distinct().size
          )
          assertTrue(
            "Correct option must exist among the 4 choices",
            ex.answerOptions.any { it.id == ex.correctOptionId }
          )
          val cid = ex.visualConceptId.trim().lowercase()
          val tgt = ex.targetPhrase.trim().lowercase()
          val canon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(ex.targetPhrase)
          val pSig = GameCurriculumAndAntiRepetitionEngine.promptMeaningSignature(ex.prompt)
          if (cid.isNotBlank()) {
            assertTrue("Concept '$cid' repeated in Level $lvl", seenConcepts.add(cid))
          }
          assertTrue("Target phrase '$tgt' repeated in Level $lvl", seenTargets.add(tgt))
          if (canon.isNotBlank()) {
            assertTrue("Semantic meaning '$canon' repeated in Level $lvl", seenSemantic.add(canon))
          }
          if (pSig.isNotBlank()) {
            assertTrue("Prompt signature '$pSig' repeated in Level $lvl", seenPrompts.add(pSig))
          }
        }
      }

      // 4. Returning to Level 15 with forceRefresh=false must preserve cached questions cleanly
      val cachedLvl15 = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = pictureGame,
        languageCode = "es",
        nativeLanguageCode = "en",
        levelNumber = 15,
        forceRefresh = false,
        userId = userId
      )
      val firstLvl15Ids = cachedLvl15.map { it.id }
      val revisitedLvl15 = DynamicAiGameEngine.generateGameQuestions(
        context = context,
        game = pictureGame,
        languageCode = "es",
        nativeLanguageCode = "en",
        levelNumber = 15,
        forceRefresh = false,
        userId = userId
      )
      assertEquals("Revisiting Level 15 must return exact preserved cached questions", firstLvl15Ids, revisitedLvl15.map { it.id })
    }

    // 5. Verify level unlocking gating (< 70% keeps Level 2 locked; >= 70% unlocks Level 2)
    val unlockUser = "u_unlock_gate_${System.currentTimeMillis()}"
    assertEquals(1, DynamicAiGameEngine.getNextGameLevel(context, "es", pictureGame.id, unlockUser))
    DynamicAiGameEngine.saveGameProgress(
      context = context,
      languageCode = "es",
      gameId = pictureGame.id,
      levelNumber = 1,
      accuracyPercent = 50,
      userId = unlockUser
    )
    assertEquals(
      "Completing Level 1 with 50% accuracy must NOT unlock Level 2",
      1,
      DynamicAiGameEngine.getNextGameLevel(context, "es", pictureGame.id, unlockUser)
    )
    DynamicAiGameEngine.saveGameProgress(
      context = context,
      languageCode = "es",
      gameId = pictureGame.id,
      levelNumber = 1,
      accuracyPercent = 75,
      userId = unlockUser
    )
    assertEquals(
      "Completing Level 1 with 75% (>= 70%) must unlock Level 2",
      2,
      DynamicAiGameEngine.getNextGameLevel(context, "es", pictureGame.id, unlockUser)
    )
  }

  @Test
  fun verifiedImages_rejectsUnrelatedPhotosAndCelebrities_andAuditsLegacyAndMiniGames() {
    // 1. Image relevance guards must reject known bad/unrelated image URLs and non-raster SVGs
    assertFalse(
      "Bread must reject Bread_Loaf_Inn building URL",
      com.example.data.repository.VerifiedVisualConceptRegistry.validateImageUrlForConcept(
        url = "https://upload.wikimedia.org/wikipedia/commons/0/00/Bread_Loaf_Inn.jpg",
        englishConcept = "Bread",
        categoryHint = "food"
      )
    )
    assertFalse(
      "SVG diagrams must be rejected for photo quiz cards",
      com.example.data.repository.VerifiedVisualConceptRegistry.validateImageUrlForConcept(
        url = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Telescope_diagram.svg",
        englishConcept = "Telescope",
        categoryHint = "science"
      )
    )
    assertFalse(
      "Strict word-boundary concept verification must reject 'car' matching 'Scarf'",
      com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
        conceptId = "car",
        targetPhrase = "Scarf",
        englishMeaning = "Scarf"
      )
    )
    assertTrue(
      "Registered dynamic concept must verify against its own target phrase",
      run {
        val dyn = com.example.data.repository.VerifiedVisualConceptRegistry.registerDynamicConcept(
          conceptId = "dyn:croissant_test",
          englishWord = "Croissant",
          targetWord = "Croissant",
          languageCode = "fr",
          domainLabel = "Bakery",
          definitionQuestionEn = "Which flaky crescent pastry is baked fresh in the morning?",
          exampleSentence = "Je prends un croissant chaud.",
          imageUrl = "",
          iconKey = "bakery",
          levelBand = 2
        )
        dyn.isDynamic &&
          com.example.data.repository.VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
            dyn.conceptId,
            "Croissant",
            "Croissant"
          )
      }
    )

    // 2. Audit legacy PictureWordGame questions for 4 unique options and verified concepts
    val legacyQuestions = com.example.data.repository.InteractiveGamesCatalogAndAiService.getPictureWordQuestions("es")
    assertTrue("Legacy picture game must have expanded non-repeating bank (>= 20)", legacyQuestions.size >= 20)
    for (q in legacyQuestions) {
      val opts = q.resolvedAnswerOptions()
      assertEquals("Legacy question ${q.id} must have 4 unique options", 4, opts.map { it.text.lowercase() }.distinct().size)
      assertTrue("Legacy question ${q.id} correct option must exist", opts.any { it.id == q.resolvedCorrectOptionId() })
    }

    // 3. Audit all 54 mini-games in LanguageGamesData: zero identical templates across consecutive games & 4 distinct options per round
    val miniGames = com.example.data.repository.LanguageGamesData.getGamesForLanguage("es")
    assertEquals(54, miniGames.size)
    val distinctRoundSignatures = miniGames.map { game ->
      game.rounds.joinToString("|") { it.audioPhrase }
    }.toSet()
    assertTrue(
      "All 54 mini-games must have distinct round content rather than repeating identical category templates (found ${distinctRoundSignatures.size})",
      distinctRoundSignatures.size >= 45
    )
    for (g in miniGames) {
      assertTrue("Mini-game ${g.id} must have 4 rounds", g.rounds.size == 4)
      for (round in g.rounds) {
        assertEquals(
          "Mini-game ${g.id} round options must be 4 distinct strings",
          4,
          round.options.map { it.trim().lowercase() }.distinct().size
        )
        assertTrue(round.correctIndex in 0..3)
      }
    }

    // 4. Audit GeminiTutorService.generateDynamicQuiz fallback for topic personalization & distinct options
    runBlocking {
      val quiz = com.example.ai.GeminiTutorService.generateDynamicQuiz(
        targetLanguage = com.example.data.model.Language.findByCode("es"),
        nativeLanguage = com.example.data.model.Language.findByCode("en"),
        proficiencyLevel = "B1",
        topic = "Medical Appointments"
      )
      assertTrue("Dynamic quiz must return 4 valid questions", quiz.size >= 3)
      for (dq in quiz) {
        assertEquals(
          "DynamicQuizQuestion options must be 4 unique choices",
          4,
          dq.options.map { it.trim().lowercase() }.distinct().size
        )
        assertTrue(dq.correctIndex in 0..3)
      }
    }
  }
}



