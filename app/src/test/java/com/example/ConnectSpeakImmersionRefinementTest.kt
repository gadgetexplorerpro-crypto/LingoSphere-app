package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.data.repository.LingoRepository
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.google.firebase.FirebaseApp
import com.example.ui.screens.AiImmersionModeScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.SpeakScreen
import com.example.ui.theme.LingoSphereTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xxhdpi")
class ConnectSpeakImmersionRefinementTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun connectScreen_at360dp_restoresClassicMatchCardsHeaderIconsAndZeroFakeProfiles() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
    val speechManager = SpeechManager(context)
    val viewModel = LingoViewModel(
      repository = LingoRepository(context),
      userId = "test_connect_360",
      memoryManager = LearningMemoryAndOfflineManager(context)
    )
    val profile = UserProfile(
      userId = "test_connect_360",
      displayName = "Alex",
      nativeLanguageCode = "en",
      targetLanguageCode = "es",
      proficiencyLevel = "Intermediate"
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        CommunityHubScreen(
          userProfile = profile,
          viewModel = viewModel,
          speechManager = speechManager,
          onStartLiveCall = {},
          onStartAiVoiceCall = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Header: Connect title, real live online pill, and direct Safety shield button (no crowded 3-dot overflow)
    composeTestRule.onNodeWithTag("online_users_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("connect_privacy_button").assertIsDisplayed()

    // 3 underline tabs: Partners, Friends, Calls
    composeTestRule.onNodeWithTag("connect_three_tab_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("connect_tab_0").assertIsDisplayed()
    composeTestRule.onNodeWithTag("connect_tab_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("connect_tab_3").assertIsDisplayed()

    // Signature gradient Smart Match hero card with Voice, Video, and Match preferences bottom sheet
    composeTestRule.onNodeWithTag("smart_match_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("smart_match_voice_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("smart_match_video_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("smart_match_preferences_btn").assertIsDisplayed()

    // Slim AI Speaking Partner row with Unlock Pro / Start button
    composeTestRule.onNodeWithTag("find_partners_list")
      .performScrollToNode(hasTestTag("ai_speaking_partner_card"))
    composeTestRule.onNodeWithTag("ai_speaking_partner_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("connect_ai_partner_btn").assertIsDisplayed()

    // Zero fake or demo profiles injected into the real directory
    composeTestRule.onNodeWithTag("find_partners_list")
      .performScrollToNode(hasTestTag("connect_available_count"))
    composeTestRule.onNodeWithTag("partner_card_ai_partner_emma").assertDoesNotExist()
    composeTestRule.onNodeWithTag("partner_card_ai_partner_liam").assertDoesNotExist()

    // Safety shield button opens Privacy & Safety settings dialog
    composeTestRule.onNodeWithTag("connect_privacy_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Privacy & Safety").assertIsDisplayed()

    speechManager.release()
  }

  @Test
  fun speakScreen_at360dp_hasClean3SegmentBarDisabledEmptyExamSubmitAndRichInterviewSetup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val profile = UserProfile(
      userId = "test_speak_360",
      displayName = "Alex",
      nativeLanguageCode = "en",
      targetLanguageCode = "fr",
      proficiencyLevel = "Intermediate"
    )
    val pastInterview = SpeakingSession(
      languageCode = "fr",
      mode = "Interview",
      scenarioTitle = "Technology & Software · Standard",
      proficiencyLevel = "Intermediate",
      turnsCount = 3L,
      fluencyScore = 88L
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        SpeakScreen(
          userProfile = profile,
          speechManager = speechManager,
          speakingSessions = listOf(pastInterview),
          onSaveSession = {},
          onStartVoiceCall = {},
          onStartVideoTutorCall = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Go to Exams via the 3-segment bar
    composeTestRule.onNodeWithTag("speak_segment_exams").performClick()
    composeTestRule.waitForIdle()

    // Sub-screen bar is clean 3-option segmented bar with zero overflow at 360dp
    composeTestRule.onNodeWithTag("speak_sub_segmented_bar").assertIsDisplayed()
    val segBounds = composeTestRule.onNodeWithTag("speak_sub_segmented_bar").getUnclippedBoundsInRoot()
    assertTrue("Segmented bar must fit within 360dp width", segBounds.right <= 362.dp)

    // Level grid & progress bar exist, and Submit is DISABLED when answer is blank
    composeTestRule.onNodeWithTag("exam_level_grid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("exam_progress_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speak_assessment_list")
      .performScrollToNode(hasTestTag("evaluate_speaking_btn"))
    composeTestRule.onNodeWithTag("evaluate_speaking_btn").assertIsNotEnabled()

    // Typing an actual response enables Submit
    composeTestRule.onNodeWithTag("speak_assessment_list")
      .performScrollToNode(hasTestTag("speaking_assessment_input"))
    composeTestRule.onNodeWithTag("speaking_assessment_input")
      .performTextInput("Bonjour, j'aime apprendre le français chaque matin.")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_assessment_list")
      .performScrollToNode(hasTestTag("evaluate_speaking_btn"))
    composeTestRule.onNodeWithTag("evaluate_speaking_btn").assertIsEnabled()

    // Switch to Interview tab -> wrapping FlowRow chips + Recent Interviews + Tip card
    composeTestRule.onNodeWithTag("speak_tab_interview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("interview_industry_flow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("interview_style_flow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speak_interview_list")
      .performScrollToNode(hasTestTag("recent_interviews_card"))
    composeTestRule.onNodeWithTag("recent_interviews_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Technology & Software · Standard").assertIsDisplayed()

    // Start interview -> Feedback button disabled until user answers
    composeTestRule.onNodeWithTag("speak_interview_list")
      .performScrollToNode(hasTestTag("start_interview_btn"))
    composeTestRule.onNodeWithTag("start_interview_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_interview_list")
      .performScrollToNode(hasTestTag("interview_feedback_btn"))
    composeTestRule.onNodeWithTag("interview_feedback_btn").assertIsNotEnabled()

    speechManager.release()
  }

  @Test
  fun livingImmersion_at360dp_collapsesMissionsExplainsTrustAndDeduplicatesTranslationBehindToggle() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
    val speechManager = SpeechManager(context)
    val viewModel = LingoViewModel(
      repository = LingoRepository(context),
      userId = "test_immersion_360",
      memoryManager = LearningMemoryAndOfflineManager(context)
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        AiImmersionModeScreen(
          viewModel = viewModel,
          speechManager = speechManager,
          onOpenReplayStudio = {},
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Horizontal scenario cards exist
    composeTestRule.onNodeWithTag("immersion_scenario_cards_row").assertIsDisplayed()

    // Trust progress bar + interactive tooltip toggle
    composeTestRule.onNodeWithTag("immersion_trust_progress").assertIsDisplayed()
    composeTestRule.onNodeWithTag("immersion_trust_tooltip").assertDoesNotExist()
    composeTestRule.onNodeWithTag("immersion_trust_info_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("immersion_trust_tooltip").assertIsDisplayed()

    // Missions start collapsed behind "Missions X/Y" chip, then expand on tap
    composeTestRule.onNodeWithTag("immersion_missions_list").assertDoesNotExist()
    composeTestRule.onNodeWithTag("immersion_missions_toggle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("immersion_missions_list").assertIsDisplayed()

    // AI opening message shows Translate toggle and does not duplicate native text before tap
    composeTestRule.onNode(hasText("Translate")).assertIsDisplayed()
    composeTestRule.onNode(hasText("Translate")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasText("Hide")).assertIsDisplayed()

    // Conversation Rescue button is clearly visible
    composeTestRule.onNodeWithTag("living_world_rescue_button").assertIsDisplayed()

    speechManager.release()
  }
}
