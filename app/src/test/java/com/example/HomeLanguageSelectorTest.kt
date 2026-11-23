package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LanguageLearningProgressSnapshot
import com.example.data.model.UserProfile
import com.example.data.model.mapCefrToProficiencyLevel
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.ui.components.ChangeLearningLanguageBottomSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.LingoSphereTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class HomeLanguageSelectorTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun homeHeader_on360dpScreenWithLongName_placesLanguageButtonBetweenNameAndBellWithoutOverlap() {
    val profile = UserProfile(
      userId = "u_360_test",
      displayName = "Fayaz Ahmad Al-Mansoor Siddiqui Jr.",
      targetLanguageCode = "en",
      proficiencyLevel = "Beginner",
      languageLevels = mapOf("en" to "A2"),
      xpPoints = 850L,
      currentStreak = 7L
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        HomeScreen(
          userProfile = profile,
          nextLesson = null,
          onNavigateToLearn = {},
          onNavigateToSpeak = {},
          onNavigateToPractice = {},
          onStartLesson = {},
          onTargetLanguageChanged = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    val headerBounds = composeTestRule.onNodeWithTag("home_header_section").getUnclippedBoundsInRoot()
    val nameBounds = composeTestRule.onNodeWithTag("home_user_display_name").getUnclippedBoundsInRoot()
    val langBounds = composeTestRule.onNodeWithTag("home_header_language_button").getUnclippedBoundsInRoot()
    val bellBounds = composeTestRule.onNodeWithTag("home_notifications_bell").getUnclippedBoundsInRoot()

    composeTestRule.onNodeWithTag("home_header_language_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_header_language_label", useUnmergedTree = true)
      .assertTextEquals("🇺🇸 A2")

    // Must not show full language name in header button
    assertTrue(
      "Header button should not show full language name",
      composeTestRule.onAllNodesWithText("English", substring = true).fetchSemanticsNodes().isEmpty()
    )

    // Strict left-to-right ordering with zero overlap on a 360dp phone
    assertTrue(
      "Name block (${nameBounds.right}) must not overlap language button (${langBounds.left})",
      nameBounds.right <= langBounds.left
    )
    assertTrue(
      "Language button (${langBounds.right}) must sit strictly to the left of notification bell (${bellBounds.left})",
      langBounds.right <= bellBounds.left
    )
    assertTrue(
      "Notification bell (${bellBounds.right}) must fit within 360dp header bounds (${headerBounds.right})",
      bellBounds.right <= headerBounds.right + androidx.compose.ui.unit.Dp(1f)
    )
  }

  @Test
  fun homeHeader_withRtlLanguage_doesNotFlipHeaderLayout() {
    val arabicProfile = UserProfile(
      userId = "u_rtl_test",
      displayName = "Fayaz Ahmad",
      targetLanguageCode = "ar",
      proficiencyLevel = "Intermediate",
      languageLevels = mapOf("ar" to "B1")
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          HomeScreen(
            userProfile = arabicProfile,
            nextLesson = null,
            onNavigateToLearn = {},
            onNavigateToSpeak = {},
            onNavigateToPractice = {},
            onStartLesson = {},
            onTargetLanguageChanged = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()

    val profileTriggerBounds = composeTestRule.onNodeWithTag("home_header_profile_trigger").getUnclippedBoundsInRoot()
    val langBounds = composeTestRule.onNodeWithTag("home_header_language_button").getUnclippedBoundsInRoot()
    val bellBounds = composeTestRule.onNodeWithTag("home_notifications_bell").getUnclippedBoundsInRoot()

    composeTestRule.onNodeWithTag("home_header_language_label", useUnmergedTree = true)
      .assertTextEquals("🇸🇦 B1")

    assertTrue(
      "RTL language must NOT flip header: profile trigger (${profileTriggerBounds.right}) must stay left of language button (${langBounds.left})",
      profileTriggerBounds.right <= langBounds.left
    )
    assertTrue(
      "RTL language must NOT flip header: language button (${langBounds.right}) must stay left of bell (${bellBounds.left})",
      langBounds.right <= bellBounds.left
    )
  }

  @Test
  fun tappingHeaderLanguageButton_opensSharedSheet_updatesLanguageAndLevelAndSyncsHomeAndProfile() {
    var profile by mutableStateOf(
      UserProfile(
        userId = "u_sync_test",
        displayName = "Fayaz Ahmad",
        targetLanguageCode = "es",
        proficiencyLevel = "Beginner",
        languageLevels = mapOf("es" to "A2"),
        xpPoints = 1200L,
        currentStreak = 11L
      )
    )
    var showSharedSheet by mutableStateOf(false)
    var showProfile by mutableStateOf(false)

    composeTestRule.setContent {
      LingoSphereTheme {
        Column {
          if (!showProfile) {
            HomeScreen(
              userProfile = profile,
              nextLesson = null,
              onNavigateToLearn = {},
              onNavigateToSpeak = {},
              onNavigateToPractice = {},
              onStartLesson = {},
              onTargetLanguageChanged = {},
              onOpenLanguageSelector = { showSharedSheet = true }
            )
          } else {
            ProfileScreen(
              userProfile = profile,
              onSignOut = {},
              onNativeLanguageChanged = {},
              onDeleteAccount = {},
              onOpenLanguageSelector = { showSharedSheet = true }
            )
          }

          if (showSharedSheet) {
            ChangeLearningLanguageBottomSheet(
              userProfile = profile,
              onLanguageAndLevelSelected = { code, cefr ->
                profile = profile.copy(
                  targetLanguageCode = code,
                  proficiencyLevel = mapCefrToProficiencyLevel(cefr),
                  languageLevels = profile.languageLevels + (code to cefr)
                )
              },
              onApplyPlacementEvaluation = { code, eval ->
                profile = profile.copy(
                  targetLanguageCode = code,
                  proficiencyLevel = eval.mappedProficiencyLevel,
                  languageLevels = profile.languageLevels + (code to eval.recommendedCefrBand)
                )
              },
              onDismiss = { showSharedSheet = false }
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_header_language_label", useUnmergedTree = true)
      .assertTextEquals("🇪🇸 A2")

    // Tap compact language button on Home -> opens shared sheet
    composeTestRule.onNodeWithTag("home_header_language_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("change_learning_language_sheet").assertIsDisplayed()

    // Verify zero Pro lock or Ad banner inside the sheet
    assertTrue(
      composeTestRule.onAllNodesWithText("Go Pro", substring = true).fetchSemanticsNodes().isEmpty()
    )

    // Filter and pick French -> transitions to CEFR level step
    composeTestRule.onNodeWithTag("language_sheet_search_field").performTextInput("French")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("language_sheet_item_fr").performClick()
    composeTestRule.waitForIdle()

    // Level step displays A1..C1 and "Not sure — test my level"
    composeTestRule.onNodeWithTag("language_sheet_level_step").assertIsDisplayed()
    composeTestRule.onNodeWithTag("language_sheet_level_A1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("language_sheet_level_B1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("language_sheet_level_C1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("language_sheet_placement_test_button").assertIsDisplayed()

    // Select B1 -> updates Home immediately, preserves global XP & streak, and keeps Spanish A2
    composeTestRule.onNodeWithTag("language_sheet_level_B1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_header_language_label", useUnmergedTree = true)
      .assertTextEquals("🇫🇷 B1")
    assertEquals("fr", profile.targetLanguageCode)
    assertEquals("A2", profile.languageLevels["es"])
    assertEquals("B1", profile.languageLevels["fr"])
    assertEquals(1200L, profile.xpPoints)
    assertEquals(11L, profile.currentStreak)

    // Switch to Profile and verify it stays 100% in sync with Home
    showProfile = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_target_language_picker_btn")
      .assertIsDisplayed()
      .assertTextContains("🇫🇷 Learning French · B1", substring = true)

    // Tapping Profile's language chip opens the exact same shared sheet
    composeTestRule.onNodeWithTag("profile_target_language_picker_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("change_learning_language_sheet").assertIsDisplayed()
  }

  @Test
  fun sharedSheet_placementTestFlow_evaluatesAndAppliesCefrLevel() {
    var appliedLang = ""
    var appliedBand = ""

    composeTestRule.setContent {
      LingoSphereTheme {
        ChangeLearningLanguageBottomSheet(
          userProfile = UserProfile(
            userId = "u_place_test",
            targetLanguageCode = "es",
            languageLevels = mapOf("es" to "A2")
          ),
          onLanguageAndLevelSelected = { _, _ -> },
          onApplyPlacementEvaluation = { code, eval ->
            appliedLang = code
            appliedBand = eval.recommendedCefrBand
          },
          onDismiss = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("language_sheet_search_field").performTextInput("German")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("language_sheet_item_de").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("language_sheet_placement_test_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("language_sheet_placement_step").assertIsDisplayed()

    // Answer all 5 placement questions (option 0 is correct across CurriculumData)
    repeat(5) {
      composeTestRule.onNodeWithTag("language_sheet_placement_option_0").performClick()
      composeTestRule.waitForIdle()
    }

    composeTestRule.onNodeWithTag("language_sheet_placement_result_band").assertIsDisplayed()
    composeTestRule.onNodeWithTag("language_sheet_apply_placement_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals("de", appliedLang)
    assertEquals("C1", appliedBand)
  }

  @Test
  fun cachedLanguageAndPerLanguageLevels_surviveSimulatedAppRestart() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val firstSessionManager = LearningMemoryAndOfflineManager(context)
    firstSessionManager.clearAllMemoryAndHistory()

    // Save Spanish (A2) and French (B1) progress snapshots and cached profile metadata
    firstSessionManager.saveLanguageProgressSnapshot(
      LanguageLearningProgressSnapshot(
        languageCode = "es",
        proficiencyLevel = "Beginner",
        cefrLevel = "A2",
        xpPoints = 1450L,
        completedLessonIds = listOf("es_vocab_1", "es_grammar_1"),
        streakDays = 9L
      )
    )
    firstSessionManager.saveLanguageProgressSnapshot(
      LanguageLearningProgressSnapshot(
        languageCode = "fr",
        proficiencyLevel = "Intermediate",
        cefrLevel = "B1",
        xpPoints = 1450L,
        completedLessonIds = listOf("fr_vocab_1"),
        streakDays = 9L
      )
    )
    firstSessionManager.saveCachedProfileDetails(
      userId = "u_restart_test",
      displayName = "Fayaz Ahmad",
      country = "United States",
      countryFlag = "🇺🇸",
      bio = "Learning daily",
      interests = listOf("Travel"),
      nativeLanguageCode = "en",
      targetLanguageCode = "fr",
      proficiencyLevel = "Intermediate",
      dailyGoalMinutes = 15L,
      languageLevels = mapOf("es" to "A2", "fr" to "B1")
    )

    // Simulate a fresh app restart by creating a new manager instance reading from SharedPreferences
    val restartedManager = LearningMemoryAndOfflineManager(context)
    val restoredMeta = restartedManager.loadCachedProfileDetails("u_restart_test")
    val restoredEsSnap = restartedManager.loadLanguageProgressSnapshot("es")
    val restoredFrSnap = restartedManager.loadLanguageProgressSnapshot("fr")

    assertNotNull("Cached profile metadata must survive restart", restoredMeta)
    assertEquals("fr", restoredMeta!!.targetLanguageCode)
    assertEquals("Intermediate", restoredMeta.proficiencyLevel)
    assertEquals("A2", restoredMeta.languageLevels["es"])
    assertEquals("B1", restoredMeta.languageLevels["fr"])

    assertNotNull(restoredEsSnap)
    assertEquals("A2", restoredEsSnap!!.cefrLevel)
    assertEquals(listOf("es_vocab_1", "es_grammar_1"), restoredEsSnap.completedLessonIds)

    assertNotNull(restoredFrSnap)
    assertEquals("B1", restoredFrSnap!!.cefrLevel)
    assertEquals(listOf("fr_vocab_1"), restoredFrSnap.completedLessonIds)
    assertFalse(restoredFrSnap.completedLessonIds.contains("es_vocab_1"))
  }
}
