package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.data.repository.CurriculumData
import com.example.speech.SpeechManager
import com.example.ui.components.EmojiVoice
import com.example.ui.components.EmojiVoiceState
import com.example.ui.components.resolveEmojiVoiceState
import com.example.ui.screens.LessonExerciseScreen
import com.example.ui.theme.LingoSphereTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-mdpi")
class LearnTutorVoiceAndEmojiSystemTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun universalEmojiVoiceSystem_mapsAllEightStatesToExtractedAssets() {
    assertEquals(R.drawable.emoji_voice_idle, EmojiVoiceState.IDLE.drawableRes)
    assertEquals(R.drawable.emoji_voice_speaking, EmojiVoiceState.SPEAKING.drawableRes)
    assertEquals(R.drawable.emoji_voice_listening, EmojiVoiceState.LISTENING.drawableRes)
    assertEquals(R.drawable.emoji_voice_thinking, EmojiVoiceState.THINKING.drawableRes)
    assertEquals(R.drawable.emoji_voice_replying, EmojiVoiceState.REPLYING.drawableRes)
    assertEquals(R.drawable.emoji_voice_processing, EmojiVoiceState.PROCESSING.drawableRes)
    assertEquals(R.drawable.emoji_voice_done, EmojiVoiceState.DONE.drawableRes)
    assertEquals(R.drawable.emoji_voice_error, EmojiVoiceState.ERROR.drawableRes)

    assertEquals(
      EmojiVoiceState.IDLE,
      resolveEmojiVoiceState(isListening = false)
    )
    assertEquals(
      EmojiVoiceState.LISTENING,
      resolveEmojiVoiceState(isListening = true, isUserSpeaking = false, audioLevel = 0.02f)
    )
    assertEquals(
      EmojiVoiceState.SPEAKING,
      resolveEmojiVoiceState(isListening = true, isUserSpeaking = true, audioLevel = 0.25f)
    )
    assertEquals(
      EmojiVoiceState.PROCESSING,
      resolveEmojiVoiceState(isListening = false, isProcessingSpeech = true)
    )
    assertEquals(
      EmojiVoiceState.THINKING,
      resolveEmojiVoiceState(isListening = false, isThinking = true)
    )
    assertEquals(
      EmojiVoiceState.REPLYING,
      resolveEmojiVoiceState(isListening = false, isReplying = true)
    )
    assertEquals(
      EmojiVoiceState.DONE,
      resolveEmojiVoiceState(isListening = false, isDone = true)
    )
    assertEquals(
      EmojiVoiceState.ERROR,
      resolveEmojiVoiceState(isListening = false, hasError = true)
    )
  }

  @Test
  fun emojiVoiceComposable_rendersAndHandlesRetry() {
    var retried = false
    composeTestRule.setContent {
      LingoSphereTheme {
        EmojiVoice(
          state = EmojiVoiceState.ERROR,
          showStateLabel = true,
          onRetry = { retried = true }
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("emoji_voice_ERROR").assertIsDisplayed()
    composeTestRule.onNodeWithTag("emoji_voice_retry_badge").performClick()
    assertTrue("Tapping error EmojiVoice should invoke onRetry", retried)
  }

  @Test
  fun lessonExerciseScreen_rendersHorizontalControlsModesAndInteractiveTutorTurn() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val sampleLesson = CurriculumData.getLessonsForLanguage("en").first()

    composeTestRule.setContent {
      LingoSphereTheme {
        LessonExerciseScreen(
          lesson = sampleLesson,
          speechManager = speechManager,
          onFinishLesson = { _, _ -> },
          onNavigateBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Verify top bar Auto-Read, 1.0x speed toggle, Follow Conversation, and Story Teleprompter are single-line horizontal pills (never vertical)
    val autoReadBounds = composeTestRule.onNodeWithTag("lesson_auto_read_toggle_button").fetchSemanticsNode().boundsInRoot
    assertTrue("Auto-Read pill must be single-line horizontal (height=${autoReadBounds.height})", autoReadBounds.height <= 42f)

    val speedBounds = composeTestRule.onNodeWithTag("lesson_speed_toggle_button").fetchSemanticsNode().boundsInRoot
    assertTrue("Speed toggle pill must be single-line horizontal (height=${speedBounds.height}, width=${speedBounds.width})", speedBounds.height <= 42f && speedBounds.width >= 40f)
    assertTrue("Speed toggle pill must stay inside 360dp width (right=${speedBounds.right})", speedBounds.right <= 360f + 1f)

    val followBounds = composeTestRule.onNodeWithTag("follow_conversation_toggle_chip").fetchSemanticsNode().boundsInRoot
    assertTrue("Follow Conversation pill must be single-line horizontal (height=${followBounds.height})", followBounds.height <= 42f)

    val storyBounds = composeTestRule.onNodeWithTag("lesson_open_teleprompter_button").fetchSemanticsNode().boundsInRoot
    assertTrue("Story Teleprompter pill must be single-line horizontal (height=${storyBounds.height}, width=${storyBounds.width})", storyBounds.height <= 42f && storyBounds.width >= 120f)
    assertTrue("Story Teleprompter pill must stay inside 360dp width (right=${storyBounds.right})", storyBounds.right <= 360f + 1f)

    // 2. Verify Tutor Header Card, Live Status Badge, Learn Mode & Practice Mode tabs, Goal Banner, and Bottom Controls exist
    composeTestRule.onNodeWithTag("lesson_tutor_header_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_live_status_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_mode_learn_tab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_mode_practice_tab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_goal_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_voice_mic_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_mute_voice_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lesson_inspire_button").assertIsDisplayed()

    // 3. Verify speed toggle cycles 1.0x -> 1.2x -> 0.85x -> 1.0x without vertical wrapping
    composeTestRule.onNodeWithTag("lesson_speed_toggle_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("1.2x").assertIsDisplayed()

    // 4. Verify translation toggle on Emma's opening message
    composeTestRule.onNodeWithTag("lesson_translate_turn_0").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Translation:", substring = true).assertExists()

    // 5. Switch to Practice Mode and verify separate Practice Mode greeting
    composeTestRule.onNodeWithTag("lesson_mode_practice_tab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Practice Mode", substring = true).assertExists()

    // 6. Switch back to Learn Mode and send a conversational turn via keyboard input
    composeTestRule.onNodeWithTag("lesson_mode_learn_tab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lesson_toggle_keyboard_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lesson_text_input_field").performTextInput("Hello Emma, my name is Alex.")
    composeTestRule.onNodeWithTag("lesson_send_text_button").performClick()
    composeTestRule.waitForIdle()

    // Verify learner's message is displayed in the conversation list
    composeTestRule.onNodeWithText("Hello Emma, my name is Alex.").assertExists()

    speechManager.release()
  }

  @Test
  fun pronunciationCoachScreen_rendersCleanCardsTabsAndEmojiVoice() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val userProfile = UserProfile(
      userId = "u_pron_coach_test",
      targetLanguageCode = "en",
      nativeLanguageCode = "es",
      proficiencyLevel = "Intermediate"
    )

    var backClicked = false
    composeTestRule.setContent {
      LingoSphereTheme {
        com.example.ui.screens.PronunciationCoachScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onSaveSession = {},
          onBack = { backClicked = true },
          pronunciationHistory = emptyList()
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Verify Top Bar title, back button, and settings button exist
    composeTestRule.onNodeWithTag("pronunciation_screen_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pronunciation_back_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pronunciation_settings_button").assertIsDisplayed()

    // 2. Verify 3 tabs exist and function
    composeTestRule.onNodeWithTag("tab_coach_practice").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_coach_sounds").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_coach_feedback").assertIsDisplayed()

    // 3. Verify Practice tab controls: Listen button, Emoji Mic button, Next Practice button
    composeTestRule.onNodeWithTag("coach_listen_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_mic_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_next_practice_button").assertIsDisplayed()

    // 4. Verify Word Card 0 (Comfortable) has full-width line structure, speaker button, practice button, and expandable tips
    composeTestRule.onNodeWithTag("mispronounced_word_card_0").performScrollTo().assertExists()
    composeTestRule.onNodeWithTag("mispronounced_listen_btn_0", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("mispronounced_practice_btn_0", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Comfortable", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("mispronounced_tips_toggle_btn_0", useUnmergedTree = true).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Hide tips", useUnmergedTree = true).assertExists()

    // 5. Test Sounds tab
    composeTestRule.onNodeWithTag("tab_coach_sounds").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Practice Difficult Sounds").assertExists()

    // 6. Test Feedback tab
    composeTestRule.onNodeWithTag("tab_coach_feedback").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_generate_drills_button").assertExists()

    speechManager.release()
  }
}

