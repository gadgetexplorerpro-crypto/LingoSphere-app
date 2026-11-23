package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ReferralRewardConfig
import com.example.data.model.ReferralShareChannel
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class ReferralRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testGetOrCreateUserReferralCode_idempotentUniquePerUser() = runBlocking {
    val uid = signInTestUser("inviter_${UUID.randomUUID()}@test.com")
    val lingoRepo = LingoRepository(firestore, auth)
    val referralRepo = ReferralRepository(firestore, auth)
    val profile = UserProfile(
      userId = uid,
      displayName = "Alice Learner",
      nativeLanguageCode = "en",
      targetLanguageCode = "es"
    )

    withTimeout(TIMEOUT_MS) {
      lingoRepo.createUserProfile(profile)
      val first = referralRepo.getOrCreateUserReferralCode(profile)
      val second = referralRepo.getOrCreateUserReferralCode(profile)

      assertTrue(ReferralRewardConfig.isValidReferralCode(first.referralCode))
      assertEquals(first.referralCode, second.referralCode)
      assertEquals(0L, first.successfulReferralsCount)
    }
  }

  @Test
  fun testClaimReferralCode_twoUsers_awardsRewardsSyncsXpAndPreventsDuplicatesAndSelfReferral() = runBlocking {
    val inviterEmail = "inviter_${UUID.randomUUID()}@test.com"
    val inviterUid = signInTestUser(inviterEmail)
    val inviterLingoRepo = LingoRepository(firestore, auth)
    val inviterReferralRepo = ReferralRepository(firestore, auth)
    val inviterProfile = UserProfile(
      userId = inviterUid,
      displayName = "Alice Inviter",
      nativeLanguageCode = "en",
      targetLanguageCode = "es",
      xpPoints = 200L,
      level = 3L
    )

    val inviterCode = withTimeout(TIMEOUT_MS) {
      inviterLingoRepo.createUserProfile(inviterProfile)
      inviterReferralRepo.recordInvitationShared(inviterUid, ReferralShareChannel.WHATSAPP)
      val summary = inviterReferralRepo.getOrCreateUserReferralCode(inviterProfile)
      inviterReferralRepo.recordInvitationShared(inviterUid, ReferralShareChannel.TELEGRAM)

      // Self-referral attempt must fail
      val selfClaim = inviterReferralRepo.claimReferralCode(
        rawCode = summary.referralCode,
        referredProfile = inviterProfile,
        attributionSource = "manual_code",
        isNewAccountRegistration = true
      )
      assertFalse(selfClaim.success)
      assertTrue(selfClaim.message.contains("Self-referrals are not allowed"))

      summary.referralCode
    }

    // Switch to newly registered friend
    val friendUid = signInTestUser("friend_${UUID.randomUUID()}@test.com")
    val friendLingoRepo = LingoRepository(firestore, auth)
    val friendReferralRepo = ReferralRepository(firestore, auth)
    val friendProfile = UserProfile(
      userId = friendUid,
      displayName = "Bob Joined",
      nativeLanguageCode = "es",
      targetLanguageCode = "en",
      xpPoints = 0L,
      level = 1L
    )

    withTimeout(TIMEOUT_MS) {
      friendLingoRepo.createUserProfile(friendProfile)
      friendLingoRepo.syncCommunityProfile(
        profile = friendProfile,
        country = "Spain",
        countryFlag = "🇪🇸",
        bio = "Excited to practice English!",
        interests = listOf("Travel"),
        isOnline = true,
        allowCalls = true
      )

      val outcome = friendReferralRepo.claimReferralCode(
        rawCode = inviterCode,
        referredProfile = friendProfile,
        attributionSource = "app_link",
        isNewAccountRegistration = true
      )
      assertTrue("Claim failed: ${outcome.message}", outcome.success)
      assertEquals(inviterUid, outcome.inviterUid)
      assertEquals(100L, outcome.xpAwardedToReferred)
      assertEquals(100L, outcome.xpAwardedToInviter)

      // Verify friend's welcome +100 XP was persisted on profile
      val updatedFriend = friendLingoRepo.getUserProfile(friendUid)
      assertEquals(100L, updatedFriend?.xpPoints)

      // Duplicate claim attempt by the same friend must be rejected
      val duplicateOutcome = friendReferralRepo.claimReferralCode(
        rawCode = inviterCode,
        referredProfile = friendProfile,
        attributionSource = "manual_code",
        isNewAccountRegistration = true
      )
      assertFalse(duplicateOutcome.success)
      assertTrue(duplicateOutcome.alreadyClaimed)
    }

    // Sign back in as inviter and load dashboard -> syncs +100 XP & shows Bob in invitedFriends
    withTimeout(TIMEOUT_MS) {
      signInTestUser(inviterEmail)
      val dashboard = inviterReferralRepo.loadReferralDashboard(inviterProfile, forceRefresh = true)
      assertEquals(1, dashboard.successfulReferralsCount)
      assertTrue(dashboard.pendingInvitationsCount >= 1)
      assertEquals(100L, dashboard.totalXpEarned)
      assertEquals(1, dashboard.invitedFriends.size)
      assertEquals(friendUid, dashboard.invitedFriends.first().friendUserId)
      assertEquals("Bob Joined", dashboard.invitedFriends.first().displayName)
      assertNotNull(dashboard.invitedFriends.first().communityProfile)

      val updatedInviter = inviterLingoRepo.getUserProfile(inviterUid)
      assertEquals(300L, updatedInviter?.xpPoints)
    }
  }

  @Test
  fun testMilestone3AndMilestone5_unlockBadgesBonusXpAndSpeakingCredits() = runBlocking {
    val inviterEmail = "mentor_${UUID.randomUUID()}@test.com"
    val inviterUid = signInTestUser(inviterEmail)
    val inviterLingoRepo = LingoRepository(firestore, auth)
    val inviterReferralRepo = ReferralRepository(firestore, auth)
    val inviterProfile = UserProfile(
      userId = inviterUid,
      displayName = "Mentor Host",
      xpPoints = 0L,
      level = 1L
    )

    val code = withTimeout(TIMEOUT_MS) {
      inviterLingoRepo.createUserProfile(inviterProfile)
      inviterReferralRepo.getOrCreateUserReferralCode(inviterProfile).referralCode
    }

    for (idx in 1..5) {
      val refUid = signInTestUser("ref_friend_${idx}_${UUID.randomUUID()}@test.com")
      val refLingoRepo = LingoRepository(firestore, auth)
      val refRepo = ReferralRepository(firestore, auth)
      val refProfile = UserProfile(
        userId = refUid,
        displayName = "Friend $idx",
        nativeLanguageCode = "fr",
        targetLanguageCode = "en"
      )
      withTimeout(TIMEOUT_MS) {
        refLingoRepo.createUserProfile(refProfile)
        val res = refRepo.claimReferralCode(
          rawCode = code,
          referredProfile = refProfile,
          attributionSource = "install_referrer",
          isNewAccountRegistration = true
        )
        assertTrue("Claim $idx failed: ${res.message}", res.success)
        assertEquals(100L, res.xpAwardedToInviter)
      }
    }

    withTimeout(TIMEOUT_MS) {
      signInTestUser(inviterEmail)
      val dashboard = inviterReferralRepo.loadReferralDashboard(inviterProfile, forceRefresh = true)
      assertEquals(5, dashboard.successfulReferralsCount)
      // 5 * 100 XP + 250 bonus XP = 750 XP
      assertEquals(750L, dashboard.totalXpEarned)
      assertEquals(3L, dashboard.bonusSpeakingCredits)
      assertTrue(dashboard.unlockedBadges.contains(ReferralRewardConfig.MILESTONE_3_BADGE_ID))
      assertTrue(dashboard.unlockedBadges.contains(ReferralRewardConfig.MILESTONE_5_BADGE_ID))

      val inviterUserAfter = inviterLingoRepo.getUserProfile(inviterUid)
      assertEquals(750L, inviterUserAfter?.xpPoints)
      assertTrue(
        inviterUserAfter?.achievements?.contains(ReferralRewardConfig.MILESTONE_3_BADGE_ID) == true
      )
      assertTrue(
        inviterUserAfter?.achievements?.contains(ReferralRewardConfig.MILESTONE_5_BADGE_ID) == true
      )
    }
  }

  @Test
  fun testUnauthenticated_accessReferralSummary_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("referral_summaries").document("unauth_uid").get().await()
      }
      fail("Expected permission denied for unauthenticated referral_summaries read")
    } catch (e: Exception) {
      val fsError = e.unwrapFirestoreException()
      assertTrue(
        "Expected PERMISSION_DENIED, got $e",
        fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
          e.message?.contains("PERMISSION_DENIED") == true
      )
    }
  }

  private companion object {
    const val TIMEOUT_MS = 8000L
  }
}
