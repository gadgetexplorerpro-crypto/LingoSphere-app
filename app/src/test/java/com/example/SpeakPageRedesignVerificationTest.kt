package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AiConversationPartner
import com.example.data.model.TutorVoice
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.screens.SpeakScreen
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
class SpeakPageRedesignVerificationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun speakHome_organizesThreeCleanAreasWithoutOverflowOrDuplicatedTopBar() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    var voiceCallTriggered = false
    var videoCallTriggered = false
    var groupCallPartnersCount = 0
    var selectedPartnerCallbackId = ""

    composeTestRule.setContent {
      LingoSphereTheme {
        SpeakScreen(
          userProfile = UserProfile(
            userId = "u_speak_1",
            targetLanguageCode = "es",
            nativeLanguageCode = "en",
            proficiencyLevel = "Intermediate"
          ),
          speechManager = speechManager,
          onSaveSession = {},
          onStartVoiceCall = { voiceCallTriggered = true },
          onStartVideoTutorCall = { videoCallTriggered = true },
          onSelectPartner = { selectedPartnerCallbackId = it.id },
          onStartConfiguredVoiceConversation = { _, groupPartners, _, _, _, _, _, _ ->
            if (groupPartners.isNotEmpty()) {
              groupCallPartnersCount = groupPartners.size
            }
          }
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Verify no duplicated top sub-bar on Speak Home
    composeTestRule.onNode(hasTestTag("speak_subbar_overview")).assertDoesNotExist()

    // 2. Verify Area 1 (Tutor Card), Area 2 (Activities 2x3 Grid), Area 3 (Group Card), and Bottom Dock exist
    composeTestRule.onNodeWithTag("main_ai_tutor_card").assertExists()
    composeTestRule.onNodeWithTag("speaking_activities_grid").assertExists()
    composeTestRule.onNodeWithTag("speak_mic_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speak_chat_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speak_send_button").assertIsDisplayed()

    // Verify Tutor card stays cleanly within 360dp mobile viewport without horizontal overflow
    val tutorBounds = composeTestRule.onNodeWithTag("main_ai_tutor_card").fetchSemanticsNode().boundsInRoot
    assertTrue("Tutor card must not overflow 360dp screen: right=${tutorBounds.right}", tutorBounds.right <= 360f + 1f)

    // Verify Start Speaking & Video Call buttons trigger callbacks
    composeTestRule.onNodeWithTag("start_ai_voice_call_btn").performClick()
    assertTrue(voiceCallTriggered)
    composeTestRule.onNodeWithTag("start_ai_video_call_btn").performClick()
    assertTrue(videoCallTriggered)

    // Verify 15 Voices selector opens and lists all 15 TutorVoice entries
    assertEquals(15, TutorVoice.entries.size)
    composeTestRule.onNodeWithTag("speak_voice_selector_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("voice_chip_${TutorVoice.entries.first().id}").assertExists()
    composeTestRule.onNodeWithTag("voice_chip_${TutorVoice.entries[1].id}").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("apply_voice_btn").performClick()
    composeTestRule.waitForIdle()

    // Verify Change Tutor opens all 12 AI tutors directory and switches tutor
    assertEquals(12, AiConversationPartner.ALL_PARTNERS.size)
    composeTestRule.onNodeWithTag("change_tutor_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_partner_search_input").assertExists()
    val secondPartner = AiConversationPartner.ALL_PARTNERS[1]
    composeTestRule.onNodeWithTag("partner_card_${secondPartner.id}").performClick()
    composeTestRule.waitForIdle()
    assertEquals(secondPartner.id, selectedPartnerCallbackId)
    composeTestRule.onNodeWithText(secondPartner.name).assertExists()

    // Verify Area 3 Group Conversation Start Group button works
    composeTestRule.onNodeWithTag("speak_chat_list").performScrollToNode(hasTestTag("group_ai_conversation_card"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("start_group_conversation_btn").performClick()
    composeTestRule.waitForIdle()
    assertTrue("Group conversation should start with at least 2 partners", groupCallPartnersCount >= 2)

    speechManager.release()
  }

  @Test
  fun speakAllSixActivitiesAndTextConversation_navigateAndFunctionCorrectly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    var immersionOpened = false
    var cameraLensOpened = false

    composeTestRule.setContent {
      LingoSphereTheme {
        SpeakScreen(
          userProfile = UserProfile(
            userId = "u_speak_2",
            targetLanguageCode = "fr",
            nativeLanguageCode = "en",
            proficiencyLevel = "Beginner"
          ),
          speechManager = speechManager,
          onSaveSession = {},
          onOpenImmersionMode = { immersionOpened = true },
          onOpenCameraLens = { cameraLensOpened = true }
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Text Chat from Tutor Card -> opens Conversation view with mic, input, send, rescue
    composeTestRule.onNodeWithTag("start_conversation_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("conversation_mic_btn").assertExists()
    composeTestRule.onNodeWithTag("conversation_input_field").assertExists()
    composeTestRule.onNodeWithTag("conversation_rescue_btn").assertExists()

    // Switch cleanly using the 3-option segmented control: Exams -> Interview -> Practice
    composeTestRule.onNodeWithTag("speak_tab_assessment").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_assessment_list").assertExists()
    composeTestRule.onNodeWithTag("speaking_assessment_input").performTextInput("Bonjour, je m'appelle Alex.")
    composeTestRule.onNodeWithTag("evaluate_speaking_btn").assertExists()

    composeTestRule.onNodeWithTag("speak_tab_interview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_interview_list").assertExists()

    // Return to Practice (Speak Home)
    composeTestRule.onNodeWithTag("speak_subbar_overview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("main_ai_tutor_card").assertExists()

    // Open Missions from Practice Home
    composeTestRule.onNodeWithTag("speak_chat_list").performScrollToNode(hasTestTag("speak_tab_roleplays"))
    composeTestRule.onNodeWithTag("speak_tab_roleplays").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_missions_list").assertExists()

    // Return to Practice and open Phone Calls
    composeTestRule.onNodeWithTag("speak_subbar_overview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_chat_list").performScrollToNode(hasTestTag("speak_tab_phone_calls"))
    composeTestRule.onNodeWithTag("speak_tab_phone_calls").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_phone_calls_list").assertExists()

    // Return to Practice and open Speaking Labs
    composeTestRule.onNodeWithTag("speak_subbar_overview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_chat_list").performScrollToNode(hasTestTag("speak_tab_labs"))
    composeTestRule.onNodeWithTag("speak_tab_labs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("speak_labs_list").assertExists()
    composeTestRule.onNode(hasText("AI Immersion Mode")).performClick()
    assertTrue(immersionOpened)
    composeTestRule.onNode(hasText("Camera Speaking Lens")).performClick()
    assertTrue(cameraLensOpened)

    // Return to Overview (Speak Home)
    composeTestRule.onNodeWithTag("speak_subbar_overview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("main_ai_tutor_card").assertExists()

    speechManager.release()
  }

  @Test
  fun aiPartners_allTwelveHaveUniqueVoicesDistinctProfiles2dAvatarsAndPersistSelection() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("lingosphere_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val speechManager = SpeechManager(context)
    val partners = AiConversationPartner.ALL_PARTNERS

    // 1. Verify all 12 partners exist and each has a distinct TutorVoice
    assertEquals(12, partners.size)
    val distinctVoices = partners.map { it.voice.id }.toSet()
    assertEquals("All 12 partners must have 12 distinct TutorVoice identities", 12, distinctVoices.size)

    // Verify balanced Male and Female voices with distinct pitch/rate/locale profiles
    val femaleCount = partners.count { it.voice.gender == "Female" }
    val maleCount = partners.count { it.voice.gender == "Male" }
    assertEquals(6, femaleCount)
    assertEquals(6, maleCount)

    val distinctAcousticSummaries = partners.map {
      speechManager.resolveDistinctVoiceProfileSummary(it.voice, "en")
    }.toSet()
    assertEquals("All 12 partners must resolve to 12 distinct acoustic voice profiles", 12, distinctAcousticSummaries.size)

    // 2. Verify all 12 partners + Chloe have distinct 2D emoji-style human character specs
    val distinctVisualSpecs = (partners + AiConversationPartner.CHLOE_PARTNER).map { partner ->
      val spec = com.example.ui.components.resolvePartnerCharacterVisualSpec(partner.id, partner.name)
      "${spec.partnerId}|${spec.skinTone}|${spec.hairColor}|${spec.hairStyle}|${spec.outfitPrimary}|${spec.accessory}|${spec.isFemale}"
    }.toSet()
    assertEquals(13, distinctVisualSpecs.size)

    // 3. Verify 2D avatar, Play Voice button, and persistence when selecting another partner
    composeTestRule.setContent {
      LingoSphereTheme {
        SpeakScreen(
          userProfile = UserProfile(
            userId = "u_speak_3",
            targetLanguageCode = "en",
            nativeLanguageCode = "es",
            proficiencyLevel = "Intermediate"
          ),
          speechManager = speechManager,
          onSaveSession = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Open AI Partners directory via Change Tutor
    composeTestRule.onNodeWithTag("change_tutor_btn").performClick()
    composeTestRule.waitForIdle()

    val targetPartner = partners[3] // Daniel (Australian Male - Orus)
    composeTestRule.onNodeWithTag("partners_directory_list")
      .performScrollToNode(hasTestTag("partner_card_${targetPartner.id}"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("preview_voice_btn_${targetPartner.id}").assertExists().performClick()
    composeTestRule.waitForIdle()
    assertEquals(targetPartner.voice, speechManager.selectedTutorVoice.value)

    // Select the partner card and verify persistence in SharedPreferences and active SpeechManager voice
    composeTestRule.onNodeWithTag("partner_card_${targetPartner.id}").performClick()
    composeTestRule.waitForIdle()

    assertEquals(targetPartner.id, prefs.getString("selected_ai_partner_id", null))
    assertEquals(targetPartner.voice.id, prefs.getString("selected_tutor_voice_id", null))
    assertEquals(targetPartner.voice, speechManager.selectedTutorVoice.value)

    speechManager.release()
  }
}

