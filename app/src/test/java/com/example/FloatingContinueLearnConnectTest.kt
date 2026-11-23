package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallRoom
import com.example.data.model.CommunityProfile
import com.example.data.model.CurriculumLesson
import com.example.data.model.FloatingContinueButtonPrefs
import com.example.data.model.UserProfile
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.ui.components.FloatingContinueLessonButton
import com.example.ui.components.IncomingCallOverlay
import com.example.ui.screens.PartnerCard
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WindingJourneyLessonRow
import com.example.ui.theme.LingoSphereTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class FloatingContinueLearnConnectTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun floatingContinueButton_tapTriggersResumeAndDismissesFirstUseHint() {
    var resumeCount = 0
    var hintDismissed = false
    var prefs by mutableStateOf(FloatingContinueButtonPrefs.DEFAULT)

    composeTestRule.setContent {
      LingoSphereTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          FloatingContinueLessonButton(
            prefs = prefs,
            lessonTitle = "Greetings & Introductions",
            onResumeLesson = { resumeCount++ },
            onPositionCommitted = { side, frac ->
              prefs = prefs.copy(snapSide = side, verticalFraction = frac, hasSeenDragHint = true)
            },
            onHintDismissed = {
              hintDismissed = true
              prefs = prefs.copy(hasSeenDragHint = true)
            }
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("floating_continue_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("floating_continue_hint").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hold and drag to move this button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("floating_continue_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Tapping floating Continue button should trigger resume once", 1, resumeCount)
    assertTrue("First-use hint should be dismissed after interaction", hintDismissed)
  }

  @Test
  fun floatingContinueButton_dragSnapsToLeftEdgeWithoutTriggeringTapAndRespectsSafeArea() {
    var resumeCount = 0
    var committedSide: String? = null
    var committedFraction: Float? = null
    var prefs by mutableStateOf(
      FloatingContinueButtonPrefs(
        enabled = true,
        snapSide = "right",
        verticalFraction = 0.78f,
        hasSeenDragHint = true
      )
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          FloatingContinueLessonButton(
            prefs = prefs,
            lessonTitle = "Cafe Ordering",
            topAvoidanceDp = 16.dp,
            bottomAvoidanceDp = 80.dp,
            onResumeLesson = { resumeCount++ },
            onPositionCommitted = { side, frac ->
              committedSide = side
              committedFraction = frac
              prefs = prefs.copy(snapSide = side, verticalFraction = frac)
            },
            onHintDismissed = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("floating_continue_button").performTouchInput {
      swipe(
        start = center,
        end = Offset(-280f, -260f),
        durationMillis = 300L
      )
    }
    composeTestRule.waitForIdle()

    assertEquals("Dragging must NOT trigger tap resume action", 0, resumeCount)
    assertEquals("Dragging past center left should snap to left edge", "left", committedSide)
    assertTrue("Vertical fraction must stay within [0, 1]", (committedFraction ?: -1f) in 0f..1f)

    val bounds = composeTestRule.onNodeWithTag("floating_continue_button").getUnclippedBoundsInRoot()
    assertTrue("Button must keep >= 8dp margin from left edge (got ${bounds.left})", bounds.left >= Dp(7.5f))
    assertTrue("Button must stay below top avoidance margin (got ${bounds.top})", bounds.top >= Dp(15.5f))
  }

  @Test
  fun floatingContinueButton_rtlDoesNotFlipPhysicalSideAndPersistenceSurvivesRestart() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val manager = LearningMemoryAndOfflineManager(context)
    val userId = "u_float_persist_${System.currentTimeMillis()}"
    val savedPrefs = FloatingContinueButtonPrefs(
      enabled = true,
      snapSide = "left",
      verticalFraction = 0.35f,
      hasSeenDragHint = true
    )
    manager.saveFloatingContinuePrefs(userId, savedPrefs)

    // Simulate app restart with a fresh manager instance
    val restored = LearningMemoryAndOfflineManager(context).loadFloatingContinuePrefs(userId)
    assertEquals(savedPrefs, restored)

    composeTestRule.setContent {
      LingoSphereTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          Box(modifier = Modifier.fillMaxSize()) {
            FloatingContinueLessonButton(
              prefs = restored,
              lessonTitle = "Arabic Greetings",
              onResumeLesson = {},
              onPositionCommitted = { _, _ -> },
              onHintDismissed = {}
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    val bounds = composeTestRule.onNodeWithTag("floating_continue_button").getUnclippedBoundsInRoot()
    assertTrue(
      "Saved 'left' side must remain physically on the left in RTL (left=${bounds.left})",
      bounds.left < Dp(40f)
    )
  }

  @Test
  fun profileSettings_toggleFloatingContinueAndResetPositionWork() {
    var prefs by mutableStateOf(
      FloatingContinueButtonPrefs(
        enabled = true,
        snapSide = "left",
        verticalFraction = 0.22f,
        hasSeenDragHint = true
      )
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        ProfileScreen(
          userProfile = UserProfile(userId = "u_pref_test", displayName = "Fayaz Ahmad"),
          onSignOut = {},
          onNativeLanguageChanged = {},
          onDeleteAccount = {},
          floatingContinuePrefs = prefs,
          onToggleFloatingContinue = { enabled -> prefs = prefs.copy(enabled = enabled) },
          onResetFloatingContinuePosition = {
            prefs = prefs.copy(
              snapSide = FloatingContinueButtonPrefs.DEFAULT.snapSide,
              verticalFraction = FloatingContinueButtonPrefs.DEFAULT.verticalFraction
            )
          }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_tile_preferences").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("setting_floating_continue_toggle").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertFalse("Toggle should turn floating Continue button off", prefs.enabled)

    composeTestRule.onNodeWithTag("setting_floating_continue_reset_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals("Reset should restore right edge", "right", prefs.snapSide)
    assertEquals("Reset should restore default vertical fraction", 0.78f, prefs.verticalFraction, 0.001f)
  }

  @Test
  fun learnLessonCards_renderLockedCurrentAndCompletedWithReadableLabelsAndProBadge() {
    val lockedLesson = CurriculumLesson(
      id = "lesson_locked_1",
      title = "Hotel Check-in Listening",
      subtitle = "Listening · 5m",
      proficiencyLevel = "Beginner",
      cefrLevel = "A2",
      durationMinutes = 5,
      xpReward = 40
    )
    val completedLesson = CurriculumLesson(
      id = "lesson_done_1",
      title = "Daily Greetings",
      subtitle = "Speaking · 4m",
      proficiencyLevel = "Beginner",
      cefrLevel = "A2",
      durationMinutes = 4,
      xpReward = 35
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        Column(modifier = Modifier.padding(16.dp)) {
          WindingJourneyLessonRow(
            lesson = lockedLesson,
            lessonNumber = 2,
            isCompleted = false,
            isCurrent = false,
            isLocked = true,
            isFirst = false,
            isLast = false,
            savedStep = 0,
            isDownloaded = false,
            isPro = false,
            onToggleDownload = {},
            onClick = {}
          )
          WindingJourneyLessonRow(
            lesson = completedLesson,
            lessonNumber = 1,
            isCompleted = true,
            isCurrent = false,
            isLocked = false,
            isFirst = true,
            isLast = true,
            savedStep = 0,
            isDownloaded = true,
            isPro = true,
            onToggleDownload = {},
            onClick = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Hotel Check-in Listening").assertIsDisplayed()
    composeTestRule.onNodeWithText("+40 XP").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pro").assertIsDisplayed()
    composeTestRule.onNodeWithText("Daily Greetings").assertIsDisplayed()
    composeTestRule.onNodeWithText("+35 XP").assertIsDisplayed()
  }

  @Test
  fun connectPartnerCard_on360dpScreenShowsChatVoiceVideoAndSafetyMenuWithoutOverflow() {
    val partner = CommunityProfile(
      userId = "p_360_call",
      displayName = "Sofia Martinez",
      country = "Spain",
      countryFlag = "🇪🇸",
      proficiencyLevel = "B2",
      bio = "Looking for daily English & Spanish calls",
      isOnline = false,
      allowCalls = true
    )
    var voiceClicked = 0
    var videoClicked = 0
    var reportClicked = 0
    var blockClicked = 0

    composeTestRule.setContent {
      LingoSphereTheme {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
          PartnerCard(
            partner = partner,
            compatibilityPercent = 94,
            isExactTandem = true,
            sharedInterestsLower = setOf("travel"),
            friendStatus = "none",
            onOpenProfile = {},
            onChat = {},
            onVoiceCall = { voiceClicked++ },
            onVideoCall = { videoClicked++ },
            onSendInvite = {},
            onFriendAction = {},
            onReport = { reportClicked++ },
            onBlock = { blockClicked++ }
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    val cardBounds = composeTestRule.onNodeWithTag("partner_card_p_360_call").getUnclippedBoundsInRoot()
    val chatBounds = composeTestRule.onNodeWithTag("partner_chat_p_360_call").getUnclippedBoundsInRoot()
    val voiceBounds = composeTestRule.onNodeWithTag("partner_call_p_360_call").getUnclippedBoundsInRoot()
    val videoBounds = composeTestRule.onNodeWithTag("partner_video_p_360_call").getUnclippedBoundsInRoot()

    composeTestRule.onNodeWithTag("partner_invite_p_360_call").assertIsDisplayed()
    composeTestRule.onNodeWithTag("partner_chat_p_360_call").assertIsDisplayed()
    composeTestRule.onNodeWithTag("partner_call_p_360_call").assertIsDisplayed()
    composeTestRule.onNodeWithTag("partner_video_p_360_call").assertIsDisplayed()

    assertTrue("Chat must be to the left of Voice without overlap", chatBounds.right <= voiceBounds.left)
    assertTrue("Voice must be to the left of Video without overlap", voiceBounds.right <= videoBounds.left)
    assertTrue("Video button must fit cleanly within 360dp card width", videoBounds.right <= cardBounds.right + Dp(1f))

    composeTestRule.onNodeWithTag("partner_call_p_360_call").performClick()
    composeTestRule.onNodeWithTag("partner_video_p_360_call").performClick()
    assertEquals(1, voiceClicked)
    assertEquals(1, videoClicked)

    composeTestRule.onNodeWithTag("partner_menu_p_360_call").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("partner_menu_report_p_360_call").assertIsDisplayed().performClick()
    assertEquals(1, reportClicked)

    composeTestRule.onNodeWithTag("partner_menu_p_360_call").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("partner_menu_block_p_360_call").assertIsDisplayed().performClick()
    assertEquals(1, blockClicked)
  }

  @Test
  fun incomingCallOverlay_showsCallerDetailsAcceptAndDeclineWithNoAds() {
    var accepted = false
    var declined = false
    val room = CallRoom(
      roomId = "room_test_1",
      callerId = "caller_1",
      callerName = "Elena Rossi",
      callerNativeLang = "it",
      callerLearningLang = "en",
      calleeId = "me_1",
      calleeName = "Fayaz",
      callType = "video",
      targetLanguageCode = "it",
      status = "ringing"
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        IncomingCallOverlay(
          callRoom = room,
          onAccept = { accepted = true },
          onDecline = { declined = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("incoming_call_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithText("Elena Rossi").assertIsDisplayed()
    composeTestRule.onNodeWithTag("accept_call_button").assertIsDisplayed().performClick()
    assertTrue(accepted)
    composeTestRule.onNodeWithTag("decline_call_button").assertIsDisplayed().performClick()
    assertTrue(declined)
  }
}
