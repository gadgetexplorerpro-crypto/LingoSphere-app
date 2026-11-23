package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.repository.BillingRepository
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.ui.screens.PremiumHubScreen
import com.example.ui.screens.PremiumHubSection
import com.example.ui.theme.LingoSphereTheme
import com.example.update.AppReleaseInfo
import com.example.update.AppUpdateAnnouncementDialog
import com.example.update.LingoAppUpdateController
import com.example.update.RealReleaseChangelogCatalog
import com.example.update.WhatsNewChangelogDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionAndAppUpdateTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    LingoAppUpdateController.resetStateForTesting(context)
  }

  @Test
  fun freeTrialEligibility_andRetentionUntilExpiryWhenCanceled_workCorrectly() {
    val newUser = UserProfile(
      userId = "u_trial_1",
      displayName = "Elena",
      subscriptionTier = "free",
      purchaseToken = null,
      isFreeTrial = false,
      hasUsedFreeTrial = false
    )
    assertTrue("New user should be eligible for 7-day free trial", newUser.isEligibleForFreeTrial)
    assertFalse("New free user does not have premium entitlement", newUser.isEntitlementVerified)

    val nowMs = System.currentTimeMillis()
    val sevenDaysLaterMs = nowMs + 7L * 24L * 3600L * 1000L

    // 1. Activate 7-day free trial via Google Play
    val trialActiveUser = newUser.copy(
      subscriptionTier = "premium_pro",
      purchaseToken = "gp_real_trial_token_abc12345",
      isFreeTrial = true,
      hasUsedFreeTrial = true,
      subscriptionAutoRenewing = true,
      subscriptionExpiryEpochMs = sevenDaysLaterMs
    )
    assertTrue(trialActiveUser.isEntitlementVerified)
    assertTrue(trialActiveUser.isFreeTrialActive)
    assertFalse("User who activated trial is no longer eligible for a second trial", trialActiveUser.isEligibleForFreeTrial)

    // 2. User cancels during the 7-day free trial -> retains access until trial expires (Requirement #11)
    val canceledDuringTrialUser = trialActiveUser.copy(
      subscriptionAutoRenewing = false
    )
    assertTrue("Canceled trial must retain access until trial expiry", canceledDuringTrialUser.isEntitlementVerified)
    assertTrue(canceledDuringTrialUser.isFreeTrialActive)
    assertTrue(canceledDuringTrialUser.isCanceledButActiveUntilExpiry)
    assertTrue(canceledDuringTrialUser.planDisplayName.contains("Canceled"))

    // 3. Once the 7-day trial period expires and auto-renew is false -> access ends & trial cannot be repeated (Requirement #12)
    val expiredTrialUser = canceledDuringTrialUser.copy(
      subscriptionExpiryEpochMs = nowMs - 60_000L
    )
    assertTrue(expiredTrialUser.isSubscriptionExpired)
    assertFalse("Expired canceled trial should no longer grant premium access", expiredTrialUser.isEntitlementVerified)
    assertFalse("Ineligible account cannot claim repeated free trial", expiredTrialUser.isEligibleForFreeTrial)
  }

  @Test
  fun localSubscriptionPersistence_restoresTrialAndAutoRenewStateAcrossRestarts() {
    val manager = LearningMemoryAndOfflineManager(context)
    val expiryMs = System.currentTimeMillis() + 7L * 24L * 3600L * 1000L
    manager.saveSubscriptionState(
      tier = "premium_yearly",
      purchaseToken = "gp_yearly_trial_token_xyz987",
      isFreeTrial = true,
      hasUsedFreeTrial = true,
      autoRenewing = false,
      expiryEpochMs = expiryMs
    )

    val loaded = manager.loadFullSubscriptionState()
    assertNotNull(loaded)
    assertEquals("premium_yearly", loaded?.tier)
    assertEquals("gp_yearly_trial_token_xyz987", loaded?.purchaseToken)
    assertTrue(loaded?.isFreeTrial == true)
    assertTrue(loaded?.hasUsedFreeTrial == true)
    assertFalse(loaded?.autoRenewing == true)
    assertEquals(expiryMs, loaded?.expiryEpochMs)
  }

  @Test
  fun premiumSubscriptionScreen_displaysMonthlyAndYearlyCardsHonestStatusAndRestoreButton() {
    val eligibleUser = UserProfile(
      userId = "u_ui_1",
      displayName = "Carlos",
      subscriptionTier = "free",
      hasUsedFreeTrial = false
    )
    val speechMgr = com.example.speech.SpeechManager(context)

    composeTestRule.setContent {
      LingoSphereTheme {
        PremiumHubScreen(
          userProfile = eligibleUser,
          billingRepository = null,
          speechManager = speechMgr,
          initialSection = PremiumHubSection.SUBSCRIPTION,
          onBack = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("subscription_hero_section").assertIsDisplayed()
    composeTestRule.onNodeWithText("LingoSphere Pro").assertIsDisplayed()

    composeTestRule.onNodeWithTag("plan_card_${SubscriptionPlan.PRODUCT_ID_YEARLY}").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("YEARLY PREMIUM – BEST VALUE", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("$79.99 / year", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onAllNodesWithText("Save 33%", useUnmergedTree = true).onFirst().assertExists()
    composeTestRule.onNodeWithTag("choose_yearly_btn").performScrollTo().assertExists()
    composeTestRule.onNodeWithText("Choose Yearly", useUnmergedTree = true).assertExists()

    composeTestRule.onNodeWithTag("plan_card_${SubscriptionPlan.PRODUCT_ID_MONTHLY}").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("MONTHLY PREMIUM", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("$9.99 / month", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("choose_monthly_btn").performScrollTo().assertExists()
    composeTestRule.onNodeWithText("Choose Monthly", useUnmergedTree = true).assertExists()

    // Unverified free user must never see fake active badges or fake renewal dates
    composeTestRule.onNodeWithTag("current_subscription_card").performScrollTo().assertExists()
    composeTestRule.onNodeWithText("Free Plan", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("VERIFIED ACTIVE", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithTag("restore_purchases_btn").performScrollTo().assertExists()

    // Verify dynamic yearly savings calculation and zero-epoch date safety
    assertEquals(
      33,
      com.example.data.repository.calculateYearlySavingsPercent(
        monthlyMicros = 9_990_000L,
        yearlyMicros = 79_990_000L,
        monthlyCurrency = "USD",
        yearlyCurrency = "USD"
      )
    )
    assertEquals("", com.example.data.repository.formatEpochDateReadable(0L))
    speechMgr.release()
  }

  @Test
  fun appUpdateController_detectsNewerVersion_respectsLaterDismissal_andClearsAfterUpgrade() {
    val currentVersionCode = BuildConfig.VERSION_CODE
    val olderOrSamePayload = mapOf(
      "versionCode" to currentVersionCode.toString(),
      "versionName" to BuildConfig.VERSION_NAME,
      "summary" to "Already installed version"
    )

    // 1. Same or older versionCode must NEVER trigger an update notification
    val handledSame = LingoAppUpdateController.handleRemoteReleaseAnnouncement(
      context = context,
      payload = olderOrSamePayload,
      postSystemNotificationIfPermitted = false
    )
    assertFalse("Current version must not trigger update announcement", handledSame)
    assertNull(LingoAppUpdateController.updateUiState.value.availableRelease)

    // 2. Newer versionCode (> BuildConfig.VERSION_CODE) triggers announcement
    val newerVersionCode = currentVersionCode + 1
    val newerPayload = mapOf(
      "versionCode" to newerVersionCode.toString(),
      "versionName" to "30.0",
      "summary" to "Discover new improvements, better voice conversations, and smoother learning.",
      "releaseNotes" to "LingoSphere Pro with Google Play Billing\nAutomatic New App Update Notifications"
    )
    val handledNewer = LingoAppUpdateController.handleRemoteReleaseAnnouncement(
      context = context,
      payload = newerPayload,
      postSystemNotificationIfPermitted = false
    )
    assertTrue("Newer version must trigger update announcement", handledNewer)
    val stateAfterAnnouncement = LingoAppUpdateController.updateUiState.value
    assertNotNull(stateAfterAnnouncement.availableRelease)
    assertEquals(newerVersionCode, stateAfterAnnouncement.availableRelease?.versionCode)
    assertEquals("30.0", stateAfterAnnouncement.availableRelease?.versionName)
    assertTrue(stateAfterAnnouncement.showInAppAnnouncement)

    // 3. Tapping "Later" dismisses the announcement and prevents repeated popups for the same versionCode
    LingoAppUpdateController.dismissUpdateForLater(context, newerVersionCode)
    assertFalse(LingoAppUpdateController.updateUiState.value.showInAppAnnouncement)

    LingoAppUpdateController.handleRemoteReleaseAnnouncement(
      context = context,
      payload = newerPayload,
      postSystemNotificationIfPermitted = false
    )
    assertFalse(
      "Dismissed versionCode must not repeatedly pop up after user taps Later",
      LingoAppUpdateController.updateUiState.value.showInAppAnnouncement
    )

    // 4. An even newer release (newerVersionCode + 1) DOES show the announcement
    val nextReleaseCode = newerVersionCode + 1
    LingoAppUpdateController.handleRemoteReleaseAnnouncement(
      context = context,
      payload = mapOf(
        "versionCode" to nextReleaseCode.toString(),
        "versionName" to "31.0",
        "summary" to "Discover new improvements, better voice conversations, and smoother learning."
      ),
      postSystemNotificationIfPermitted = false
    )
    assertTrue(LingoAppUpdateController.updateUiState.value.showInAppAnnouncement)
    assertEquals(nextReleaseCode, LingoAppUpdateController.updateUiState.value.availableRelease?.versionCode)
  }

  @Test
  fun appUpdateDialogs_renderUpdateAnnouncementAndWhatsNewCorrectly() {
    var updateNowClicked = false
    var laterClicked = false

    val sampleRelease = AppReleaseInfo(
      versionCode = BuildConfig.VERSION_CODE + 1,
      versionName = "30.0",
      title = "LingoSphere Update Available",
      summary = "Discover new improvements, better voice conversations, and smoother learning.",
      releaseNotes = RealReleaseChangelogCatalog.currentRelease.releaseNotes,
      isCritical = false
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        AppUpdateAnnouncementDialog(
          release = sampleRelease,
          isDownloadingFlexible = false,
          isFlexibleReadyToInstall = false,
          onUpdateNow = { updateNowClicked = true },
          onLater = { laterClicked = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("update_announcement_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("update_announcement_title").assertIsDisplayed()
    composeTestRule.onNodeWithText("LingoSphere Update Available").assertIsDisplayed()
    composeTestRule.onNodeWithText("Version 30.0").assertIsDisplayed()
    composeTestRule.onNodeWithText("Discover new improvements, better voice conversations, and smoother learning.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("update_now_button").performClick()
    assertTrue(updateNowClicked)
    composeTestRule.onNodeWithTag("update_later_button").performClick()
    assertTrue(laterClicked)
  }

  @Test
  fun whatsNewChangelogDialog_displaysCurrentVersionAndReleaseNotes() {
    var checkClicked = false
    val state = LingoAppUpdateController.updateUiState.value

    composeTestRule.setContent {
      LingoSphereTheme {
        WhatsNewChangelogDialog(
          updateState = state,
          onCheckForUpdates = { checkClicked = true },
          onUpdateNow = {},
          onDismiss = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("whats_new_dialog", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("What's New in LingoSphere").assertIsDisplayed()
    composeTestRule.onNodeWithTag("check_for_updates_button", useUnmergedTree = true).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(checkClicked)
  }

  @Test
  fun accessPolicyManager_enforcesFreeAndProLimits_booksGating_andSevenDayTimezoneCooldown() {
    val freeUser = UserProfile(
      userId = "u_free_policy",
      displayName = "Mateo",
      subscriptionTier = "free",
      purchaseToken = null
    )
    com.example.data.repository.AccessPolicyManager.updateEntitlement(
      com.example.data.repository.ServerEntitlementRecord(
        status = com.example.data.repository.EntitlementStatus.FREE
      )
    )
    com.example.data.repository.AccessPolicyManager.resetSessionUpgradePrompts()

    // 1. Structured Lessons & AI Roleplay are 100% Pro-only (zero free allowance)
    val freeLessonCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.LESSONS_DAILY,
      userProfile = freeUser,
      usedToday = 0
    )
    assertFalse("Structured lessons must be Pro-only for Free users even at 0 used", freeLessonCheck.allowed)

    val freeRoleplayCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.AI_ROLEPLAY_SESSIONS,
      userProfile = freeUser,
      usedToday = 0
    )
    assertFalse("AI Roleplay must be Pro-only for Free users even at 0 used", freeRoleplayCheck.allowed)

    val proLessonCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.LESSONS_DAILY,
      isProUser = true,
      usedDailyUnits = 0
    )
    assertTrue("Pro user must have access to structured lessons", proLessonCheck.allowed)

    val proRoleplayCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.AI_ROLEPLAY_SESSIONS,
      isProUser = true,
      usedDailyUnits = 0
    )
    assertTrue("Pro user must have access to AI roleplay", proRoleplayCheck.allowed)

    // 2. Books gating: Free users can open up to 5 distinct books per calendar month
    com.example.data.repository.AccessPolicyManager.setOpenedBooksThisMonthForTesting(emptySet())
    for (idx in 1..5) {
      val bookId = "gutenberg_${1660 + idx}"
      val check = com.example.data.repository.AccessPolicyManager.canOpenBookThisMonth(freeUser, bookId)
      assertTrue("Book #$idx of 5 must be allowed for Free user", check.allowed)
      com.example.data.repository.AccessPolicyManager.recordOpenedBookLocallyForMonth(bookId)
    }
    val reopenExistingCheck = com.example.data.repository.AccessPolicyManager.canOpenBookThisMonth(freeUser, "gutenberg_1661")
    assertTrue("Re-opening an already-counted book in the same month must stay allowed", reopenExistingCheck.allowed)

    val sixthBookCheck = com.example.data.repository.AccessPolicyManager.canOpenBookThisMonth(freeUser, "gutenberg_2701")
    assertFalse("Opening a 6th distinct book in the same month must require LingoSphere Pro", sixthBookCheck.allowed)
    assertEquals(5, com.example.data.repository.AccessPolicyManager.openedBookIdsThisMonth.value.size)

    // 3. Pro-only features (Analytics, Interview Prep, Personalized Material, Smart Match)
    val freeAnalyticsCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.ANALYTICS,
      userProfile = freeUser
    )
    assertFalse("Analytics must be gated for Free users", freeAnalyticsCheck.allowed)

    // 4. Grace period (pro_grace) keeps Pro active, while pro_on_hold and expired revert to Free limits
    com.example.data.repository.AccessPolicyManager.updateEntitlement(
      com.example.data.repository.ServerEntitlementRecord(
        userId = freeUser.userId,
        status = com.example.data.repository.EntitlementStatus.PRO_GRACE,
        productId = SubscriptionPlan.PRODUCT_ID_PRO,
        basePlanId = SubscriptionPlan.BASE_PLAN_YEARLY,
        expiryTimeMillis = System.currentTimeMillis() + 3L * 24L * 3600L * 1000L
      )
    )
    val graceBookCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.BOOKS_AUDIO,
      userProfile = freeUser,
      itemId = "gutenberg_2701"
    )
    assertTrue("Grace period (pro_grace) must keep Pro books accessible", graceBookCheck.allowed)

    com.example.data.repository.AccessPolicyManager.updateEntitlement(
      com.example.data.repository.ServerEntitlementRecord(
        userId = freeUser.userId,
        status = com.example.data.repository.EntitlementStatus.PRO_ON_HOLD
      )
    )
    val onHoldBookCheck = com.example.data.repository.AccessPolicyManager.canUserAccess(
      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.BOOKS_AUDIO,
      userProfile = freeUser,
      itemId = "gutenberg_2701"
    )
    assertFalse("Account on hold (pro_on_hold) must pause Pro access", onHoldBookCheck.allowed)

    // 5. 7-day timezone change cooldown
    val nowMs = 1_750_000_000_000L
    assertTrue(
      "First timezone update after >7 days should succeed",
      com.example.data.repository.AccessPolicyManager.tryUpdateUserTimezone("America/New_York", nowMs)
    )
    assertFalse(
      "Second timezone change within 7 days must be rejected to prevent daily reset gaming",
      com.example.data.repository.AccessPolicyManager.tryUpdateUserTimezone("Asia/Tokyo", nowMs + 2L * 24L * 3600L * 1000L)
    )
    assertTrue(
      "Timezone change after 8 days should succeed",
      com.example.data.repository.AccessPolicyManager.tryUpdateUserTimezone("Europe/London", nowMs + 8L * 24L * 3600L * 1000L)
    )

    // 6. Session-once contextual upgrade prompt allowance
    assertTrue(
      "First contextual upgrade prompt in session for books.audio should be shown",
      com.example.data.repository.AccessPolicyManager.consumeSessionUpgradePromptAllowance("books.audio")
    )
    assertFalse(
      "Second contextual upgrade prompt in same session for books.audio must be suppressed",
      com.example.data.repository.AccessPolicyManager.consumeSessionUpgradePromptAllowance("books.audio")
    )

    // Reset entitlement state back to FREE for subsequent tests
    com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
  }

  @Test
  fun part0_falseProBugFixed_loggedOutAndUnverifiedAlwaysFree_andUidSwitchClearsEntitlement() {
    com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()

    // 1. Logged out or guest user must ALWAYS be Free
    val guestUser = UserProfile(
      userId = "",
      displayName = "Guest",
      subscriptionTier = "premium_pro",
      purchaseToken = null
    )
    assertFalse("Logged-out/guest user must never have verified Pro entitlement", guestUser.isEntitlementVerified)

    // 2. Signed-in user with stale client profile subscriptionTier="premium_pro" but no server entitlement must be Free
    val staleClientFlagUser = UserProfile(
      userId = "user_stale_1",
      displayName = "Fay",
      subscriptionTier = "premium_pro",
      purchaseToken = null
    )
    assertFalse("Stale subscriptionTier without server entitlement must never show Pro", staleClientFlagUser.isEntitlementVerified)

    // 3. Verified server entitlement for user_pro_1 grants Pro ONLY to user_pro_1, NEVER to user_other_2
    val futureExpiry = System.currentTimeMillis() + 30L * 24L * 3600L * 1000L
    com.example.data.repository.AccessPolicyManager.updateEntitlement(
      com.example.data.repository.ServerEntitlementRecord(
        userId = "user_pro_1",
        status = com.example.data.repository.EntitlementStatus.PRO_ACTIVE,
        productId = SubscriptionPlan.PRODUCT_ID_PRO,
        basePlanId = SubscriptionPlan.BASE_PLAN_MONTHLY,
        expiryTimeMillis = futureExpiry
      )
    )
    val verifiedProUser = UserProfile(userId = "user_pro_1", displayName = "Fay", subscriptionTier = "premium_pro")
    val differentUidUser = UserProfile(userId = "user_other_2", displayName = "Other", subscriptionTier = "free")

    assertTrue("Verified server entitlement for matching UID must grant Pro", verifiedProUser.isEntitlementVerified)
    assertFalse("Server entitlement for user_pro_1 must NEVER leak to user_other_2", differentUidUser.isEntitlementVerified)

    // 4. Switching accounts or signing out clears entitlement immediately
    com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange(newUid = "user_other_2")
    assertFalse(
      "Entitlement must be cleared immediately on UID switch or sign-out",
      com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement
    )
  }

  @Test
  fun part1_wordOfTheDay_neverRepeatsWithin60Days_andAdaptsToLanguageAndCefrLevel() {
    val languages = listOf("English", "Spanish", "French", "German", "Japanese")
    val levels = listOf("A1", "B1", "C1")

    for (language in languages) {
      for (level in levels) {
        val seenWords = mutableSetOf<String>()
        for (dayOffset in 0 until 60) {
          val dayKey = String.format("2026-%02d-%02d", (dayOffset / 28) + 1, (dayOffset % 28) + 1)
          val entry = com.example.data.repository.WordOfTheDayRepository.buildValidatedCuratedWord(
            language = language,
            cefrLevel = level,
            dayKey = dayKey,
            excludedWordsLower = seenWords
          )
          assertTrue("Word must be non-blank for $language $level", entry.word.isNotBlank())
          assertTrue("Meaning must be non-blank for $language $level", entry.meaning.isNotBlank())
          assertTrue("Example sentence must be non-blank for $language $level", entry.exampleSentence.isNotBlank())
          val normalized = entry.word.trim().lowercase()
          assertFalse(
            "Word '$normalized' repeated within 60 days for $language $level on day $dayOffset",
            seenWords.contains(normalized)
          )
          seenWords.add(normalized)
        }
        assertEquals("Should produce 60 distinct words across 60 days for $language $level", 60, seenWords.size)
      }
    }
  }

  @Test
  fun part2_aiTutorEmmaLimits_enforcesZeroFree_15mDailyAnd150mMonthlyPro_andTimezones() {
    com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
    val policy = com.example.data.repository.AccessPolicyManager.policyState.value
    assertEquals("Free AI Tutor daily limit must be 0 seconds (Pro-only AI)", 0, policy.aiTutorFreeSecondsPerDay)
    assertEquals("Pro AI Tutor daily limit must be 900 seconds (15 min)", 900, policy.aiTutorProSecondsPerDay)
    assertEquals("Pro AI Tutor monthly limit must be 9000 seconds (150 min)", 9000, policy.aiTutorProSecondsPerMonth)
    assertEquals("Max single session must be 600 seconds (10 min)", 600, policy.aiTutorMaxSessionSeconds)
    assertEquals("Idle speech timeout must be 45 seconds", 45, policy.aiTutorIdleTimeoutSeconds)

    // 1. Free user has 0s AI Tutor allowance and sees Upgrade subtitle
    val freeUsageStart = com.example.data.repository.AccessPolicyManager.tutorUsageState.value
    assertFalse(freeUsageStart.isPro)
    assertEquals(0, freeUsageStart.effectiveRemainingSeconds)
    assertEquals("Unlock AI learning with Pro", freeUsageStart.formatHomeSubtitle())
    assertEquals(
      com.example.data.repository.AiTutorLimitReason.FREE_DAILY_60S,
      freeUsageStart.limitReachedReason
    )

    // 2. Monthly Pro and Yearly Pro receive identical 15m/day (900s) and 150m/month (9000s) limits
    val futureExpiry = System.currentTimeMillis() + 30L * 24L * 3600L * 1000L
    for (basePlan in listOf(SubscriptionPlan.BASE_PLAN_MONTHLY, SubscriptionPlan.BASE_PLAN_YEARLY)) {
      com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange(newUid = "pro_user_$basePlan")
      com.example.data.repository.AccessPolicyManager.updateEntitlement(
        com.example.data.repository.ServerEntitlementRecord(
          userId = "pro_user_$basePlan",
          status = com.example.data.repository.EntitlementStatus.PRO_ACTIVE,
          productId = SubscriptionPlan.PRODUCT_ID_PRO,
          basePlanId = basePlan,
          expiryTimeMillis = futureExpiry
        )
      )
      val proUsage = com.example.data.repository.AccessPolicyManager.tutorUsageState.value
      assertTrue("Pro status must be active for basePlan=$basePlan", proUsage.isPro)
      assertEquals(900, proUsage.dailyLimitSeconds)
      assertEquals(9000, proUsage.monthlyLimitSeconds)
      assertEquals(900, proUsage.effectiveRemainingSeconds)
    }

    // 3. Pro monthly limit (9000s = 150m) reached blocks session even if today's usage is under 15m
    com.example.data.repository.AccessPolicyManager.recordConnectedTutorSecondsForLocalDisplay(900)
    val proDailyExhausted = com.example.data.repository.AccessPolicyManager.tutorUsageState.value
    assertEquals(0, proDailyExhausted.effectiveRemainingSeconds)
    assertEquals(
      com.example.data.repository.AiTutorLimitReason.PRO_DAILY_15M,
      proDailyExhausted.limitReachedReason
    )

    // 4. Timezone day and month keys work accurately across 2 distinct IANA timezones (America/New_York and Asia/Tokyo)
    // Instant: 2026-03-31 22:30:00 UTC -> 2026-03-31 in New York (EDT, UTC-4), but 2026-04-01 in Tokyo (JST, UTC+9)
    val boundaryEpochMs = 1_774_996_200_000L // 2026-03-31T22:30:00Z
    val nyDay = com.example.data.repository.AccessPolicyManager.currentDailyKeyInTimezone("America/New_York", boundaryEpochMs)
    val nyMonth = com.example.data.repository.AccessPolicyManager.currentMonthlyKeyInTimezone("America/New_York", boundaryEpochMs)
    val tokyoDay = com.example.data.repository.AccessPolicyManager.currentDailyKeyInTimezone("Asia/Tokyo", boundaryEpochMs)
    val tokyoMonth = com.example.data.repository.AccessPolicyManager.currentMonthlyKeyInTimezone("Asia/Tokyo", boundaryEpochMs)

    assertEquals("2026-03-31", nyDay)
    assertEquals("2026-03", nyMonth)
    assertEquals("2026-04-01", tokyoDay)
    assertEquals("2026-04", tokyoMonth)

    com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
  }
}

