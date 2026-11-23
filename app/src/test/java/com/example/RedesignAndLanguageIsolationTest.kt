package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiTutorService
import com.example.data.model.ConversationReplaySession
import com.example.data.model.PortfolioSpeakingSample
import com.example.data.model.RepeatedMistakeSummary
import com.example.data.model.UserProfile
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.ui.screens.ExploreAiPracticeLabsScreen
import com.example.ui.screens.ExploreCategoryRoute
import com.example.ui.screens.HomeExploreBottomSheet
import com.example.ui.theme.LingoSphereTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-mdpi")
class RedesignAndLanguageIsolationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun exploreHub_displaysHeaderFourCategoriesAndFiltersViaSearch() {
    var clickedCategory: ExploreCategoryRoute? = null

    composeTestRule.setContent {
      LingoSphereTheme {
        HomeExploreBottomSheet(
          onNavigateToPractice = {},
          onOpenCultureJourney = {},
          onNavigateToVideos = {},
          onOpenSpeakingRooms = {},
          onOpenLanguageExchange = {},
          onOpenSmartMatching = {},
          onOpenDailyChallenges = {},
          onOpenProgressDashboard = {},
          onOpenExploreLanguages = {},
          onOpenImmersionMode = {},
          onOpenConversationReplay = {},
          onOpenAccentStudio = {},
          onOpenLanguagePortfolio = {},
          onOpenMistakeRecovery = {},
          onOpenCameraLens = {},
          onOpenConversationRepairLab = {},
          onOpenLanguageEmergencyMode = {},
          onOpenAiDebatePartner = {},
          onOpenExplainItThreeWays = {},
          onOpenConversationDetective = {},
          onOpenTeachTheAi = {},
          onOpenInviteFriends = {},
          onDismiss = {},
          userProfile = UserProfile(userId = "u_explore_test", targetLanguageCode = "fr"),
          completedLessonsCount = 8,
          weeklySessionsCount = 4,
          cefrStageLabel = "A2",
          onOpenCategoryRoute = { clickedCategory = it }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Explore").assertIsDisplayed()
    composeTestRule.onNodeWithText("Discover new ways to learn.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("explore_category_community").assertIsDisplayed()
    composeTestRule.onNodeWithTag("explore_category_ai_labs").assertIsDisplayed()
    composeTestRule.onNodeWithTag("explore_category_progress").assertIsDisplayed()
    composeTestRule.onNodeWithTag("explore_category_learning_tools").assertIsDisplayed()

    // Search filters destinations directly
    composeTestRule.onNodeWithTag("explore_search_input").performTextInput("Debate")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("explore_tile_debate").performScrollTo().assertIsDisplayed()

    // Clear search and tap AI Practice Labs category
    composeTestRule.onNodeWithTag("explore_search_clear_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("explore_category_ai_labs").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(ExploreCategoryRoute.AI_PRACTICE_LABS, clickedCategory)
  }

  @Test
  fun mistakeRecoveryAndLanguageMemory_isolateByLanguageAndReturnNoFakeSpanishDefaults() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val memoryManager = LearningMemoryAndOfflineManager(context)
    memoryManager.clearAllMemoryAndHistory()

    val freshFrenchExercises = GeminiTutorService.generateMistakeRecoveryExercises(
      mistakes = emptyList(),
      targetLanguage = "French"
    )
    assertTrue(
      "Mistake recovery must not fabricate Spanish defaults when a French learner has no mistakes",
      freshFrenchExercises.isEmpty()
    )

    val spanishMistake = RepeatedMistakeSummary(
      id = "es_1",
      targetLanguage = "Spanish",
      category = "Ser vs. Estar",
      exampleWrong = "Soy cansado",
      exampleCorrected = "Estoy cansado",
      ruleSummary = "Use estar for states"
    )
    memoryManager.saveRepeatedMistakeSummaries(listOf(spanishMistake))

    val frenchMistakes = memoryManager.loadRepeatedMistakeSummaries("French")
    assertTrue("French mistake list must stay empty when only Spanish mistakes exist", frenchMistakes.isEmpty())

    val frenchProfile = memoryManager.buildAiLanguageMemoryProfile("French", cefrLevel = "A2")
    assertTrue(
      "French profile must not contain hardcoded Spanish vocabulary weaknesses",
      frenchProfile.vocabularyWeaknesses.none { it.contains("Por vs. Para", ignoreCase = true) }
    )

    val sampleEn = PortfolioSpeakingSample(
      id = "s_en",
      title = "Sample",
      targetLanguage = "English",
      cefrLevel = "B1",
      spokenTranscript = "Hello there",
      improvedVersion = "Hello there",
      pronunciationScore = 88,
      fluencyScore = 87,
      durationSeconds = 12,
      recordedAtEpochMs = 1L,
      dateLabel = "Today"
    )
    memoryManager.savePortfolioSpeakingSample(sampleEn)
    assertTrue(
      "Japanese portfolio must stay isolated from English samples",
      memoryManager.loadPortfolioSpeakingSamples("Japanese").isEmpty()
    )

    val replayJa = ConversationReplaySession(
      id = "r_ja",
      scenarioTitle = "Tokyo Cafe",
      targetLanguage = "Japanese",
      cefrLevel = "A2",
      timestampLabel = "Today",
      overallScore = 90,
      turns = emptyList()
    )
    memoryManager.saveConversationReplaySession(replayJa)
    assertTrue(
      "German replays must stay isolated from Japanese replays",
      memoryManager.loadConversationReplaySessions("German").isEmpty()
    )
  }

  @Test
  fun exploreAiPracticeLabs_showsSingleLivingImmersionFeaturedCardAndCategoryChips() {
    composeTestRule.setContent {
      LingoSphereTheme {
        ExploreAiPracticeLabsScreen(
          onOpenImmersionMode = {},
          onOpenCameraLens = {},
          onOpenLanguageEmergencyMode = {},
          onOpenAiDebatePartner = {},
          onOpenConversationRepairLab = {},
          onOpenExplainItThreeWays = {},
          onOpenConversationDetective = {},
          onOpenMistakeRecovery = {},
          onOpenTeachTheAi = {},
          onOpenAccentStudio = {},
          onOpenConversationReplay = {},
          onBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    // Living Immersion Mode appears ONLY once (as the Featured Lab card, no duplicate in Real-Life list)
    assertEquals(1, composeTestRule.onAllNodesWithText("Living Immersion Mode").fetchSemanticsNodes().size)
    // Category chips switch between Real-life, Conversation, and Improve (1 section at a time)
    composeTestRule.onNodeWithTag("practice_labs_chip_real_life").assertIsDisplayed()
    composeTestRule.onNodeWithTag("practice_labs_chip_conversation").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("AI Debate Partner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("practice_labs_chip_improve").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Mistake Recovery").assertIsDisplayed()
  }
}
