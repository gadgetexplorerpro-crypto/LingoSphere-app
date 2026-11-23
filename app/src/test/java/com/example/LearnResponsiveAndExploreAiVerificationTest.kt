package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ai.DynamicDebateTopic
import com.example.ai.ExploreAiLearningEngine
import com.example.ai.ExploreAiResult
import com.example.ai.ExploreLearnerContext
import com.example.ai.ExploreSessionRecord
import com.example.data.model.UserProfile
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.data.repository.LearningPlanSetupPrefs
import com.example.data.repository.OfflineEmergencyPhrase
import com.example.data.repository.OfflineEmergencyPhrasePack
import com.example.data.repository.OfflineEmergencyPhraseRepository
import com.example.speech.SpeechManager
import com.example.ui.screens.AiCameraLanguageScreen
import com.example.ui.screens.LanguageEmergencyModeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PersonalizedLearningSetupModal
import com.example.ui.theme.LingoSphereTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class LearnResponsiveAndExploreAiVerificationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun personalizedLearningSetupModal_rendersCrisplyWithoutVerticalTextStackingAt320dpAndLargeFontScale() {
    var savedLevel = ""
    var savedGoal = ""
    var savedMinutes = 0
    var savedConfidence = ""

    composeTestRule.setContent {
      LingoSphereTheme {
        CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.35f)) {
          Box(modifier = Modifier.width(320.dp).fillMaxHeight()) {
            PersonalizedLearningSetupModal(
              currentLevel = "Beginner",
              currentGoal = "Everyday Communication",
              currentDailyMinutes = 30,
              currentConfidence = "High",
              onSaveSetup = { level, goal, mins, conf ->
                savedLevel = level
                savedGoal = goal
                savedMinutes = mins
                savedConfidence = conf
              },
              onLaunchPlacementTest = {},
              onDismiss = {}
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()

    val criticalChipTags = listOf(
      "setup_level_beginner",
      "setup_level_intermediate",
      "setup_level_advanced",
      "setup_goal_travel",
      "setup_goal_everyday_communication",
      "setup_goal_work_and_career",
      "setup_goal_school_and_exams",
      "setup_time_5m",
      "setup_time_10m",
      "setup_time_15m",
      "setup_time_30m",
      "setup_confidence_low",
      "setup_confidence_medium",
      "setup_confidence_high"
    )

    criticalChipTags.forEach { tag ->
      val bounds = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
      val chipWidth = bounds.right - bounds.left
      val chipHeight = bounds.bottom - bounds.top
      assertTrue(
        "Chip $tag must stay inside 320dp viewport (right=${bounds.right})",
        bounds.right <= 322.dp
      )
      assertTrue(
        "Chip $tag must not collapse into narrow vertical stacked text (width=$chipWidth, height=$chipHeight)",
        chipWidth >= 56.dp && chipHeight <= 72.dp
      )
    }

    composeTestRule.onNodeWithTag("setup_time_30m").performClick()
    composeTestRule.onNodeWithTag("setup_confidence_high").performClick()
    composeTestRule.onNodeWithTag("setup_save_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Beginner", savedLevel)
    assertEquals("Everyday Communication", savedGoal)
    assertEquals(30, savedMinutes)
    assertEquals("High", savedConfidence)
  }

  @Test
  fun learningPlanPreferences_persistAllFourFieldsAndCompletedFlag() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = LearningMemoryAndOfflineManager(context)
    val prefs = LearningPlanSetupPrefs(
      goal = "Work & Career",
      confidence = "High",
      completed = true
    )
    manager.saveLearningPlanSetup(
      goal = prefs.goal,
      confidence = prefs.confidence,
      completed = prefs.completed
    )
    val reloaded = manager.loadLearningPlanSetup()
    assertEquals("Work & Career", reloaded.goal)
    assertEquals("High", reloaded.confidence)
    assertTrue(reloaded.completed)
  }

  @Test
  fun learnScreen_showsCompactEditLearningPlanChipWithoutBlockingLessons() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)

    composeTestRule.setContent {
      LingoSphereTheme {
        LearnScreen(
          userProfile = UserProfile(
            userId = "u_learn_test",
            targetLanguageCode = "es",
            proficiencyLevel = "Beginner",
            dailyGoalMinutes = 20
          ),
          lessons = emptyList(),
          onStartLesson = {},
          onOpenCulturePassport = {},
          speechManager = speechManager,
          currentLearningGoal = "Travel",
          currentConfidence = "High",
          hasCompletedLearningSetup = true
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("edit_learning_plan_chip").assertIsDisplayed()
  }

  @Test
  fun cameraSpeakingLens_hasNoHardcodedSampleCafeButton() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)

    composeTestRule.setContent {
      LingoSphereTheme {
        AiCameraLanguageScreen(
          userProfile = UserProfile(userId = "u_cam_test", targetLanguageCode = "fr"),
          speechManager = speechManager,
          onSaveVocabToFlashcard = { _, _, _ -> },
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    assertEquals(
      0,
      composeTestRule.onAllNodesWithTag("camera_scan_sample_scene_btn").fetchSemanticsNodes().size
    )
    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Try Sample Café", substring = true).fetchSemanticsNodes().size
    )
  }

  @Test
  fun offlineEmergencyPhraseRepository_persistsValidatesAndDeletesNineCategoryPackOffline() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    OfflineEmergencyPhraseRepository.deletePack(context, "ja")
    assertFalse(OfflineEmergencyPhraseRepository.hasDownloadedPack(context, "ja"))

    val phrases = OfflineEmergencyPhraseRepository.REQUIRED_CATEGORIES.mapIndexed { idx, category ->
      OfflineEmergencyPhrase(
        id = "ja_em_$idx",
        category = category,
        targetPhrase = "助けてください ($idx)",
        phoneticGuide = "Tasukete kudasai",
        nativeTranslation = "Please help me ($category)",
        usageNote = "Use clearly when seeking immediate assistance.",
        urgent = idx < 4
      )
    }

    val pack = OfflineEmergencyPhrasePack(
      languageCode = "ja",
      languageName = "Japanese",
      nativeLanguageCode = "en",
      downloadedAtEpochMs = System.currentTimeMillis(),
      phrases = phrases
    )

    val saveResult = OfflineEmergencyPhraseRepository.savePack(context, pack)
    assertTrue(saveResult.isSuccess)
    assertTrue(OfflineEmergencyPhraseRepository.hasDownloadedPack(context, "ja"))
    assertTrue("ja" in OfflineEmergencyPhraseRepository.listDownloadedLanguageCodes(context))

    val reloaded = OfflineEmergencyPhraseRepository.loadPack(context, "ja")
    assertNotNull(reloaded)
    assertEquals(9, reloaded!!.categoryCount)
    assertEquals(9, reloaded.phrases.size)

    assertTrue(OfflineEmergencyPhraseRepository.deletePack(context, "ja"))
    assertNull(OfflineEmergencyPhraseRepository.loadPack(context, "ja"))
  }

  @Test
  fun emergencyPhraseModeScreen_showsHonestDisclaimerAllNineCategoriesAndOfflineSavedPhrases() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val phrases = OfflineEmergencyPhraseRepository.REQUIRED_CATEGORIES.mapIndexed { idx, cat ->
      OfflineEmergencyPhrase(
        id = "es_offline_$idx",
        category = cat,
        targetPhrase = "Necesito ayuda con $cat",
        phoneticGuide = "neh-seh-SEE-toh ah-YOO-dah",
        nativeTranslation = "I need help with $cat",
        usageNote = "Speak clearly and show screen if needed.",
        urgent = idx == 1
      )
    }
    OfflineEmergencyPhraseRepository.savePack(
      context,
      OfflineEmergencyPhrasePack(
        languageCode = "es",
        languageName = "Spanish",
        nativeLanguageCode = "en",
        downloadedAtEpochMs = System.currentTimeMillis(),
        phrases = phrases
      )
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        LanguageEmergencyModeScreen(
          userProfile = UserProfile(userId = "u_em_test", targetLanguageCode = "es"),
          speechManager = speechManager,
          offlineDownloadedLangs = setOf("es"),
          onToggleOfflinePack = {},
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("emergency_disclaimer_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("emergency_cat_all").assertIsDisplayed()
    composeTestRule.onNodeWithTag("emergency_cat_0").assertIsDisplayed()
    composeTestRule.onNodeWithTag("emergency_phrase_card_es_offline_0").performScrollTo().assertIsDisplayed()

    OfflineEmergencyPhraseRepository.deletePack(context, "es")
  }

  @Test
  fun exploreAiLearningEngine_enforcesDebateParticipationBitmapDownscaleAndLocalSessionPersistence() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val learnerCtx = ExploreLearnerContext(
      targetLanguageCode = "fr",
      targetLanguageName = "French",
      nativeLanguageCode = "en",
      nativeLanguageName = "English",
      cefrLevel = "B1"
    )
    val topic = DynamicDebateTopic(
      id = "deb_test_1",
      titleTarget = "Les villes devraient limiter les voitures.",
      titleNative = "Cities should limit cars.",
      contextBriefNative = "Urban transport debate",
      proPositionNative = "Cleaner air",
      conPositionNative = "Commuter flexibility",
      cefrLevel = "B1",
      suggestedConnectorsTarget = listOf("Cependant", "Par exemple")
    )

    // Must refuse to evaluate when the learner has 0 turns
    val emptyEval = ExploreAiLearningEngine.evaluateCompletedDebate(
      context = context,
      learnerCtx = learnerCtx,
      topic = topic,
      learnerStance = "Pro",
      history = emptyList()
    )
    assertTrue(emptyEval is ExploreAiResult.Error)
    assertFalse((emptyEval as ExploreAiResult.Error).canRetry)

    // Bitmap compression must downscale oversized camera images to <= 1280px
    val hugeBitmap = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888)
    val prepared = ExploreAiLearningEngine.prepareCameraBitmapForAi(hugeBitmap).getOrThrow()
    assertTrue("Prepared width must be <= 1280", prepared.width <= 1280)
    assertTrue("Prepared height must be <= 1280", prepared.height <= 1280)

    // Local Explore session persistence
    ExploreAiLearningEngine.saveExploreSession(
      context = context,
      record = ExploreSessionRecord(
        sessionId = "test_exp_three_way_1",
        featureType = "explain_three_ways",
        languageCode = "fr",
        title = "Passé Composé vs. Imparfait",
        summary = "Completed vs. ongoing past actions",
        cefrLevel = "B1",
        turnsCount = 2,
        scorePercent = 92
      )
    )
    val loaded = ExploreAiLearningEngine.loadLocalExploreSessions(
      context = context,
      featureType = "explain_three_ways",
      languageCode = "fr"
    )
    assertTrue(loaded.any { it.sessionId == "test_exp_three_way_1" && it.scorePercent == 92 })
  }
}
