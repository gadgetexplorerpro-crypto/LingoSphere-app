package com.example

import com.example.ai.AiErrorHandler
import com.example.ai.AiUiError
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.data.repository.AccessPolicyManager
import com.example.data.repository.BillingRepository
import com.example.data.repository.EntitlementStatus
import com.example.data.repository.ServerEntitlementRecord
import java.net.UnknownHostException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiErrorHandlerAndAccessPolicyTest {

  @Before
  fun setUp() {
    AiErrorHandler.testForceOffline = false
    AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
    AccessPolicyManager.setOpenedBooksThisMonthForTesting(emptySet())
  }

  @After
  fun tearDown() {
    AiErrorHandler.testForceOffline = false
    AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
    AccessPolicyManager.setOpenedBooksThisMonthForTesting(emptySet())
  }

  @Test
  fun sanitizeUserFacingMessage_stripsAllInternalTermsAndPreservesCalmCopy() {
    val bannedRawMessages = listOf(
      "Gemini API error 500 Internal Server Error",
      "OpenAI rate limit 429 RESOURCE_EXHAUSTED",
      "Firebase App Check token missing or invalid 401",
      "java.lang.IllegalStateException: apikey unauthorized http 403",
      "Stacktrace in AppCheck verification"
    )
    val forbiddenWords = listOf(
      "gemini", "openai", "firebase", "app check", "appcheck",
      "token", "api key", "apikey", "401", "429", "500", "exception"
    )

    for (raw in bannedRawMessages) {
      val sanitized = AiErrorHandler.sanitizeUserFacingMessage(raw)
      val lower = sanitized.lowercase()
      for (word in forbiddenWords) {
        assertFalse("Sanitized message '$sanitized' must never contain '$word'", lower.contains(word))
      }
      assertTrue("Sanitized message must be calm & non-blank", sanitized.isNotBlank())
    }

    val offlineHint = AiErrorHandler.sanitizeUserFacingMessage("Firebase network exception: offline no internet")
    assertTrue(offlineHint.contains("offline", ignoreCase = true))

    val proHint = AiErrorHandler.sanitizeUserFacingMessage("Gemini 403 error: feature requires Pro")
    assertTrue(proHint.contains("Pro", ignoreCase = true))

    val safeMessage = "Please check your pronunciation of the vowel sound."
    assertEquals(safeMessage, AiErrorHandler.sanitizeUserFacingMessage(safeMessage))
  }

  @Test
  fun handleError_classifiesProRequired_SignIn_Offline_RateLimit_andTransientCalmly() {
    val proErr = AiErrorHandler.handle(serverCode = "PRO_REQUIRED", httpStatus = 403, serverMessage = "Requires LingoSphere Pro")
    assertTrue(proErr is AiUiError.ProRequired)
    assertFalse(proErr.canRetry)

    val signInErr = AiErrorHandler.handle(serverCode = "SIGN_IN_REQUIRED")
    assertTrue(signInErr is AiUiError.SignInRequired)
    assertFalse(signInErr.canRetry)

    val offlineErr = AiErrorHandler.handle(throwable = UnknownHostException("api.example.com"))
    assertTrue(offlineErr is AiUiError.Offline)
    assertTrue(offlineErr.isOffline)
    assertTrue(offlineErr.canRetry)

    val rateErr = AiErrorHandler.handle(httpStatus = 429, rawMessage = "RESOURCE_EXHAUSTED")
    assertTrue(rateErr is AiUiError.RateLimited)
    assertTrue(rateErr.canRetry)

    val transientErr = AiErrorHandler.handle(throwable = RuntimeException("Socket hung up HTTP 503"))
    assertTrue(transientErr is AiUiError.Transient)
    assertTrue(transientErr.canRetry)
    assertFalse(transientErr.message.contains("503"))
    assertFalse(transientErr.message.contains("Socket"))
  }

  @Test
  fun accessPolicyManager_enforcesZeroFreeAi_ProOnlyLessonsAndRoleplay_UidIsolation_andFiveMonthlyBooks() {
    val freeUser = UserProfile(userId = "uid_free", displayName = "Amina", subscriptionTier = "free")
    val futureExpiry = System.currentTimeMillis() + 30L * 24L * 3600L * 1000L

    val proOnlyKeys = listOf(
      AccessPolicyFeatureKeys.LESSONS_DAILY,
      AccessPolicyFeatureKeys.AI_ROLEPLAY_SESSIONS,
      AccessPolicyFeatureKeys.AI_VOICE_SECONDS,
      AccessPolicyFeatureKeys.AI_TEXT_MESSAGES,
      AccessPolicyFeatureKeys.AI_CORRECTIONS,
      AccessPolicyFeatureKeys.LABS,
      AccessPolicyFeatureKeys.STUDIO,
      AccessPolicyFeatureKeys.VOICES
    )
    for (key in proOnlyKeys) {
      val decision = AccessPolicyManager.canUserAccess(featureKey = key, userProfile = freeUser, usedToday = 0)
      assertFalse("Free user must not have access to Pro-only feature '$key'", decision.allowed)
    }

    // Grant server entitlement strictly to uid_pro_owner
    AccessPolicyManager.updateEntitlement(
      ServerEntitlementRecord(
        uid = "uid_pro_owner",
        status = EntitlementStatus.PRO_ACTIVE,
        productId = SubscriptionPlan.PRODUCT_ID_PRO,
        basePlanId = SubscriptionPlan.BASE_PLAN_MONTHLY,
        verifiedWithPlayApi = true,
        expiryEpochMs = futureExpiry
      )
    )
    val proOwner = UserProfile(userId = "uid_pro_owner", displayName = "Sofia")
    assertTrue(AccessPolicyManager.isUserVerifiedPro(proOwner))
    assertFalse("UID mismatch must never leak Pro entitlement", AccessPolicyManager.isUserVerifiedPro(freeUser))

    for (key in proOnlyKeys) {
      val proDecision = AccessPolicyManager.canUserAccess(featureKey = key, userProfile = proOwner, usedToday = 0)
      assertTrue("Verified Pro user must have access to '$key'", proDecision.allowed)
    }

    // Verify 5 distinct books per month limit for Free users
    AccessPolicyManager.clearEntitlementAndUsageOnAuthChange("uid_free")
    for (i in 1..5) {
      val id = "book_free_$i"
      assertTrue(AccessPolicyManager.canOpenBookThisMonth(freeUser, id).allowed)
      AccessPolicyManager.recordOpenedBookLocallyForMonth(id)
    }
    assertTrue(AccessPolicyManager.canOpenBookThisMonth(freeUser, "book_free_3").allowed)
    assertFalse(AccessPolicyManager.canOpenBookThisMonth(freeUser, "book_free_6").allowed)
  }

  @Test
  fun billingCatalogPricing_hasStrictlyMonthlyAndYearlyPro_withNoTrialCopy() {
    val pricing = BillingRepository.defaultCatalogPricing(hasUsedFreeTrial = false)
    assertEquals(2, pricing.size)
    val monthly = pricing[SubscriptionPlan.PRODUCT_ID_MONTHLY]!!
    val yearly = pricing[SubscriptionPlan.PRODUCT_ID_YEARLY]!!

    assertEquals("$9.99 / month", monthly.formattedPrice)
    assertFalse(monthly.hasFreeTrial)
    assertNull(monthly.trialPeriodLabel)

    assertEquals("$79.99 / year", yearly.formattedPrice)
    assertFalse(yearly.hasFreeTrial)
    assertNull(yearly.trialPeriodLabel)
    assertEquals(33, yearly.savingsPercent)
  }
}
