package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CommunityProfile
import com.example.data.model.InvitedFriendItem
import com.example.data.model.ReferralClaimOutcome
import com.example.data.model.ReferralDashboardState
import com.example.data.model.ReferralRecord
import com.example.data.model.ReferralRewardConfig
import com.example.data.model.ReferralShareChannel
import com.example.data.model.ReferralSummary
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class ReferralRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
  private val appContext: Context? = null
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
    context.applicationContext
  )

  private val cachedDashboardByUser = ConcurrentHashMap<String, Pair<Long, ReferralDashboardState>>()
  private val cachedFriendProfiles = ConcurrentHashMap<String, CommunityProfile>()

  private fun requireUid(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing referral data.")
  }

  private fun expectedTotalInviterXp(referralCount: Long): Long {
    if (referralCount <= 0L) return 0L
    val base = referralCount * ReferralRewardConfig.INVITER_XP_PER_REFERRAL
    val milestone5Bonus = if (referralCount >= ReferralRewardConfig.MILESTONE_5_THRESHOLD) {
      ReferralRewardConfig.MILESTONE_5_BONUS_XP
    } else {
      0L
    }
    return base + milestone5Bonus
  }

  private fun milestonesForCount(count: Long): List<String> = buildList {
    if (count >= ReferralRewardConfig.MILESTONE_3_THRESHOLD) {
      add(ReferralRewardConfig.MILESTONE_3_BADGE_ID)
    }
    if (count >= ReferralRewardConfig.MILESTONE_5_THRESHOLD) {
      add(ReferralRewardConfig.MILESTONE_5_BADGE_ID)
    }
  }

  /**
   * Ensures every user receives exactly one unique permanent referral code in `referral_codes/{code}`
   * and an accompanying `referral_summaries/{uid}` document.
   */
  suspend fun getOrCreateUserReferralCode(userProfile: UserProfile): ReferralSummary {
    val uid = auth.currentUser?.uid ?: userProfile.userId.ifBlank { requireUid() }
    val summaryRef = db.collection("referral_summaries").document(uid)

    val existingSnap = summaryRef.get().await()
    FirestoreDiagnostics.recordRead(1, "referral_summary_get")
    if (existingSnap.exists()) {
      val parsed = parseReferralSummary(existingSnap, uid)
      if (parsed.referralCode.isNotBlank()) {
        return parsed
      }
    }

    val safeDisplayName = userProfile.displayName.trim().ifBlank { "Lingo Learner" }.take(80)
    val safeTargetLang = userProfile.targetLanguageCode.ifBlank { "es" }.take(10)
    val safeNativeLang = userProfile.nativeLanguageCode.ifBlank { "en" }.take(10)
    val localPending = appContext?.let {
      ReferralInstallAttributionStore.getLocalPendingInvites(it, uid).toLong()
    } ?: 0L

    var chosenCode = ReferralRewardConfig.generateDeterministicCode(uid, safeDisplayName, 0)
    for (attempt in 0..5) {
      val candidate = ReferralRewardConfig.generateDeterministicCode(uid, safeDisplayName, attempt)
      val codeSnap = db.collection("referral_codes").document(candidate).get().await()
      FirestoreDiagnostics.recordRead(1, "referral_code_probe")
      if (!codeSnap.exists() || codeSnap.getString("ownerUid") == uid) {
        chosenCode = candidate
        break
      }
    }

    val codeRef = db.collection("referral_codes").document(chosenCode)
    db.runTransaction { tx ->
      val currSummary = tx.get(summaryRef)
      if (currSummary.exists()) {
        return@runTransaction
      }
      val currCode = tx.get(codeRef)
      if (!currCode.exists()) {
        tx.set(
          codeRef,
          mapOf(
            "code" to chosenCode,
            "ownerUid" to uid,
            "ownerDisplayName" to safeDisplayName,
            "ownerTargetLanguage" to safeTargetLang,
            "ownerNativeLanguage" to safeNativeLang,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }
      tx.set(
        summaryRef,
        mapOf(
          "userId" to uid,
          "referralCode" to chosenCode,
          "successfulReferralsCount" to 0L,
          "syncedReferralsCount" to 0L,
          "pendingInvitationsCount" to localPending.coerceIn(0L, 10000L),
          "totalXpEarnedFromReferrals" to 0L,
          "unlockedBadges" to emptyList<String>(),
          "bonusSpeakingCredits" to 0L,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }.await()

    val createdSummary = ReferralSummary(
      userId = uid,
      referralCode = chosenCode,
      successfulReferralsCount = 0L,
      pendingInvitationsCount = localPending,
      totalXpEarnedFromReferrals = 0L,
      unlockedBadges = emptyList(),
      bonusSpeakingCredits = 0L
    )
    appContext?.let { ReferralInstallAttributionStore.saveSummaryCache(it, createdSummary) }
    FirestoreQuotaManager.recordReadSuccess()
    return createdSummary
  }

  /**
   * Reconciles any newly completed referral rewards into the inviter's `referral_summaries/{uid}`
   * (milestone 3 & 5 badges, +250 bonus XP, +3 speaking credits) and `users/{uid}` document
   * (`xpPoints`, `level`, `achievements`) idempotently using `syncedReferralsCount`.
   */
  suspend fun syncPendingInviterXpIfNeeded(uid: String, summarySnap: DocumentSnapshot? = null): Long {
    val snap = summarySnap ?: db.collection("referral_summaries").document(uid).get().await()
    if (!snap.exists()) return 0L

    val successfulCount = (snap.getLong("successfulReferralsCount") ?: 0L).coerceAtLeast(0L)
    val syncedCount = (snap.getLong("syncedReferralsCount") ?: 0L).coerceAtLeast(0L)
    val existingBadges = (snap.get("unlockedBadges") as? List<*>)
      ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
      ?: emptyList()
    val needsMilestone3 = successfulCount >= ReferralRewardConfig.MILESTONE_3_THRESHOLD &&
      !existingBadges.contains(ReferralRewardConfig.MILESTONE_3_BADGE_ID)
    val needsMilestone5 = successfulCount >= ReferralRewardConfig.MILESTONE_5_THRESHOLD &&
      !existingBadges.contains(ReferralRewardConfig.MILESTONE_5_BADGE_ID)

    if (successfulCount <= syncedCount && !needsMilestone3 && !needsMilestone5) return 0L

    val deltaXp = (expectedTotalInviterXp(successfulCount) - expectedTotalInviterXp(syncedCount))
      .coerceAtLeast(0L)
    val userRef = db.collection("users").document(uid)
    val summaryRef = db.collection("referral_summaries").document(uid)

    db.runTransaction { tx ->
      val freshSummary = tx.get(summaryRef)
      val freshSuccessful = (freshSummary.getLong("successfulReferralsCount") ?: 0L).coerceAtLeast(0L)
      val freshSynced = (freshSummary.getLong("syncedReferralsCount") ?: 0L).coerceAtLeast(0L)
      val freshBadges = (freshSummary.get("unlockedBadges") as? List<*>)
        ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
        ?: emptyList()
      val grantM3 = freshSuccessful >= ReferralRewardConfig.MILESTONE_3_THRESHOLD &&
        !freshBadges.contains(ReferralRewardConfig.MILESTONE_3_BADGE_ID)
      val grantM5 = freshSuccessful >= ReferralRewardConfig.MILESTONE_5_THRESHOLD &&
        !freshBadges.contains(ReferralRewardConfig.MILESTONE_5_BADGE_ID)

      if (freshSuccessful <= freshSynced && !grantM3 && !grantM5) return@runTransaction

      val userSnap = tx.get(userRef)
      if (userSnap.exists()) {
        val prevXp = userSnap.getLong("xpPoints") ?: 0L
        val xpToSyncToProfile = (expectedTotalInviterXp(freshSuccessful) - expectedTotalInviterXp(freshSynced))
          .coerceAtLeast(0L)
        val nextXp = prevXp + xpToSyncToProfile
        val nextLevel = (nextXp / 100L) + 1L
        val existingAchievements = (userSnap.get("achievements") as? List<*>)
          ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
          ?: emptyList()
        val mergedAchievements = (existingAchievements + milestonesForCount(freshSuccessful))
          .distinct()
          .takeLast(50)

        tx.update(
          userRef,
          mapOf(
            "xpPoints" to nextXp,
            "level" to nextLevel,
            "achievements" to mergedAchievements,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      val prevSummaryXp = (freshSummary.getLong("totalXpEarnedFromReferrals") ?: 0L).coerceAtLeast(0L)
      val prevCredits = (freshSummary.getLong("bonusSpeakingCredits") ?: 0L).coerceAtLeast(0L)
      val updatedBadges = (freshBadges + milestonesForCount(freshSuccessful)).distinct().take(20)
      val summaryBonusXp = if (grantM5) ReferralRewardConfig.MILESTONE_5_BONUS_XP else 0L
      val summaryCreditsDelta = if (grantM5) {
        ReferralRewardConfig.MILESTONE_5_SPEAKING_CREDITS.toLong()
      } else {
        0L
      }

      tx.update(
        summaryRef,
        mapOf(
          "syncedReferralsCount" to freshSuccessful,
          "unlockedBadges" to updatedBadges,
          "totalXpEarnedFromReferrals" to (prevSummaryXp + summaryBonusXp),
          "bonusSpeakingCredits" to (prevCredits + summaryCreditsDelta),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }.await()

    return deltaXp
  }

  /**
   * Loads the user's referral dashboard using quota-safe one-time `.get()` calls and
   * a 5-minute TTL cache (zero continuous Firestore snapshot listeners).
   */
  suspend fun loadReferralDashboard(
    userProfile: UserProfile,
    forceRefresh: Boolean = false
  ): ReferralDashboardState {
    val uid = auth.currentUser?.uid ?: userProfile.userId
    if (uid.isBlank()) {
      return ReferralDashboardState(errorMessage = "Sign in to view your referral code.")
    }

    val pendingLocalCode = appContext?.let { ReferralInstallAttributionStore.getPendingReferralCode(it) }
    val isNewUserAccount = isCurrentAccountWithinNewRegistrationWindow()

    val nowMs = System.currentTimeMillis()
    val inMem = cachedDashboardByUser[uid]
    if (!forceRefresh && inMem != null && (nowMs - inMem.first) < DASHBOARD_CACHE_TTL_MS) {
      return inMem.second.copy(
        pendingReferrerCode = pendingLocalCode,
        isEligibleForNewSignupReferral = isNewUserAccount && inMem.second.alreadyReferredByName == null
      )
    }

    if (FirestoreQuotaManager.shouldBlockRead(nowMs)) {
      val cachedSummary = appContext?.let {
        ReferralInstallAttributionStore.loadSummaryCache(it, uid, ignoreTtl = true)
      }
      val fallbackCode = cachedSummary?.referralCode?.takeIf { it.isNotBlank() }
        ?: ReferralRewardConfig.generateDeterministicCode(uid, userProfile.displayName, 0)
      return ReferralDashboardState(
        isLoading = false,
        referralCode = fallbackCode,
        inviteLink = ReferralRewardConfig.buildInviteLink(fallbackCode),
        shareMessage = ReferralRewardConfig.buildShareText(fallbackCode),
        successfulReferralsCount = cachedSummary?.successfulReferralsCount?.toInt() ?: 0,
        pendingInvitationsCount = cachedSummary?.pendingInvitationsCount?.toInt() ?: 0,
        totalXpEarned = cachedSummary?.totalXpEarnedFromReferrals ?: 0L,
        bonusSpeakingCredits = cachedSummary?.bonusSpeakingCredits ?: 0L,
        unlockedBadges = cachedSummary?.unlockedBadges ?: emptyList(),
        invitedFriends = inMem?.second?.invitedFriends ?: emptyList(),
        pendingReferrerCode = pendingLocalCode,
        isEligibleForNewSignupReferral = isNewUserAccount && cachedSummary?.hasClaimedReferral != true,
        errorMessage = "Showing cached referral stats while cloud quota cools down."
      )
    }

    return try {
      val summary = getOrCreateUserReferralCode(userProfile)
      val initialSummarySnap = db.collection("referral_summaries").document(uid).get().await()
      FirestoreDiagnostics.recordRead(1, "referral_summary_refresh")
      val syncedDelta = runCatching { syncPendingInviterXpIfNeeded(uid, initialSummarySnap) }.getOrDefault(0L)
      val summarySnap = if (syncedDelta > 0L) {
        db.collection("referral_summaries").document(uid).get().await()
      } else {
        initialSummarySnap
      }

      val refreshedSummary = if (summarySnap.exists()) parseReferralSummary(summarySnap, uid) else summary
      val referralRecords = queryInviterReferrals(uid)

      val invitedFriends = referralRecords.map { record ->
        val cachedProfile = cachedFriendProfiles[record.referredUid]
          ?: fetchCommunityProfileSafe(record.referredUid)?.also {
            cachedFriendProfiles[record.referredUid] = it
          }

        InvitedFriendItem(
          friendUserId = record.referredUid,
          displayName = cachedProfile?.displayName?.ifBlank { record.referredName } ?: record.referredName,
          avatarUrl = cachedProfile?.avatarUrl.orEmpty(),
          nativeLanguageCode = cachedProfile?.nativeLanguageCode ?: record.nativeLanguageCode,
          targetLanguageCode = cachedProfile?.targetLanguageCode ?: record.targetLanguageCode,
          proficiencyLevel = cachedProfile?.proficiencyLevel ?: "Beginner",
          isOnline = cachedProfile?.isEffectivelyOnline() ?: false,
          allowCalls = cachedProfile?.allowCalls ?: true,
          xpAwarded = record.inviterXpAwarded,
          status = record.status,
          joinedAtEpochMs = record.completedAt?.toDate()?.time
            ?: record.createdAt?.toDate()?.time
            ?: System.currentTimeMillis(),
          communityProfile = cachedProfile
        )
      }

      val alreadyReferredByName = if (refreshedSummary.hasClaimedReferral) {
        val ownReferralSnap = runCatching {
          db.collection("referrals").document(uid).get().await()
        }.getOrNull()
        ownReferralSnap?.getString("inviterName")?.takeIf { it.isNotBlank() }
          ?: refreshedSummary.referredByCode
      } else {
        null
      }

      val localPending = appContext?.let {
        ReferralInstallAttributionStore.getLocalPendingInvites(it, uid).toLong()
      } ?: 0L
      val mergedPending = maxOf(refreshedSummary.pendingInvitationsCount, localPending).toInt()
      val activeCount = maxOf(refreshedSummary.successfulReferralsCount.toInt(), invitedFriends.size)
      val mergedXp = maxOf(
        refreshedSummary.totalXpEarnedFromReferrals,
        expectedTotalInviterXp(activeCount.toLong())
      )

      val state = ReferralDashboardState(
        isLoading = false,
        referralCode = refreshedSummary.referralCode,
        inviteLink = ReferralRewardConfig.buildInviteLink(refreshedSummary.referralCode),
        shareMessage = ReferralRewardConfig.buildShareText(refreshedSummary.referralCode),
        successfulReferralsCount = activeCount,
        pendingInvitationsCount = mergedPending,
        totalXpEarned = mergedXp,
        bonusSpeakingCredits = refreshedSummary.bonusSpeakingCredits,
        unlockedBadges = refreshedSummary.unlockedBadges.ifEmpty {
          milestonesForCount(activeCount.toLong())
        },
        invitedFriends = invitedFriends,
        pendingReferrerCode = pendingLocalCode,
        alreadyReferredByName = alreadyReferredByName,
        isEligibleForNewSignupReferral = isNewUserAccount && !refreshedSummary.hasClaimedReferral
      )

      cachedDashboardByUser[uid] = System.currentTimeMillis() to state
      appContext?.let {
        ReferralInstallAttributionStore.saveSummaryCache(
          it,
          refreshedSummary.copy(
            successfulReferralsCount = activeCount.toLong(),
            pendingInvitationsCount = mergedPending.toLong(),
            totalXpEarnedFromReferrals = mergedXp
          )
        )
      }
      FirestoreQuotaManager.recordReadSuccess()
      state
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
        FirestoreQuotaManager.recordQuotaExhausted(e)
      }
      val fallbackSummary = appContext?.let {
        ReferralInstallAttributionStore.loadSummaryCache(it, uid, ignoreTtl = true)
      }
      val fallbackCode = fallbackSummary?.referralCode?.takeIf { it.isNotBlank() }
        ?: ReferralRewardConfig.generateDeterministicCode(uid, userProfile.displayName, 0)
      ReferralDashboardState(
        isLoading = false,
        referralCode = fallbackCode,
        inviteLink = ReferralRewardConfig.buildInviteLink(fallbackCode),
        shareMessage = ReferralRewardConfig.buildShareText(fallbackCode),
        successfulReferralsCount = fallbackSummary?.successfulReferralsCount?.toInt() ?: 0,
        pendingInvitationsCount = fallbackSummary?.pendingInvitationsCount?.toInt() ?: 0,
        totalXpEarned = fallbackSummary?.totalXpEarnedFromReferrals ?: 0L,
        bonusSpeakingCredits = fallbackSummary?.bonusSpeakingCredits ?: 0L,
        unlockedBadges = fallbackSummary?.unlockedBadges ?: emptyList(),
        invitedFriends = inMem?.second?.invitedFriends ?: emptyList(),
        pendingReferrerCode = pendingLocalCode,
        isEligibleForNewSignupReferral = isNewUserAccount && fallbackSummary?.hasClaimedReferral != true,
        errorMessage = "Could not refresh referral rewards right now. Please try again shortly."
      )
    }
  }

  /**
   * Records an outgoing share action cleanly by incrementing local and cloud pending invite counters
   * without inflating successful referrals.
   */
  suspend fun recordInvitationShared(userId: String, channel: ReferralShareChannel): Int {
    val uid = auth.currentUser?.uid ?: userId
    if (uid.isBlank()) return 0
    val localCount = appContext?.let {
      ReferralInstallAttributionStore.incrementLocalPendingInvites(it, uid)
    } ?: 1

    try {
      val summaryRef = db.collection("referral_summaries").document(uid)
      val snap = summaryRef.get().await()
      if (snap.exists()) {
        val currentCloudPending = (snap.getLong("pendingInvitationsCount") ?: 0L).coerceAtLeast(0L)
        val nextCloudPending = maxOf(currentCloudPending + 1L, localCount.toLong()).coerceAtMost(10000L)
        summaryRef.update(
          mapOf(
            "pendingInvitationsCount" to nextCloudPending,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      cachedDashboardByUser[uid]?.let { (ts, prev) ->
        cachedDashboardByUser[uid] = ts to prev.copy(
          pendingInvitationsCount = maxOf(prev.pendingInvitationsCount + 1, localCount)
        )
      }
    } catch (e: Exception) {
      Log.d("ReferralRepo", "Pending share count sync skipped (${channel.id}): ${e.message}")
    }
    return localCount
  }

  /**
   * Atomically claims a referral code for a newly registered user account.
   * Rejects self-referrals, existing/old accounts, invalid codes, and duplicate claims.
   */
  suspend fun claimReferralCode(
    rawCode: String,
    referredProfile: UserProfile,
    attributionSource: String = "app_link",
    isNewAccountRegistration: Boolean = isCurrentAccountWithinNewRegistrationWindow()
  ): ReferralClaimOutcome {
    val cleanCode = ReferralRewardConfig.normalizeReferralCode(rawCode)
      ?: return ReferralClaimOutcome(
        success = false,
        message = "Please enter a valid 6–16 character referral code."
      )

    val uid = auth.currentUser?.uid ?: referredProfile.userId.ifBlank { requireUid() }
    val normalizedSource = when (attributionSource) {
      "app_link", "deep_link", "install_referrer", "manual_code" -> attributionSource
      else -> "app_link"
    }

    if (!isNewAccountRegistration) {
      appContext?.let { ReferralInstallAttributionStore.clearPendingReferralCode(it) }
      return ReferralClaimOutcome(
        success = false,
        message = "Referral codes can only be claimed by newly registered accounts."
      )
    }

    return try {
      val codeSnap = db.collection("referral_codes").document(cleanCode).get().await()
      FirestoreDiagnostics.recordRead(1, "referral_code_lookup")
      if (!codeSnap.exists() || codeSnap.getBoolean("isActive") != true) {
        return ReferralClaimOutcome(
          success = false,
          message = "Referral code '$cleanCode' was not found."
        )
      }

      val inviterUid = codeSnap.getString("ownerUid").orEmpty().trim()
      val inviterName = codeSnap.getString("ownerDisplayName")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.take(80)
        ?: "LingoSphere Friend"

      if (inviterUid.isBlank() || inviterUid == uid) {
        appContext?.let { ReferralInstallAttributionStore.clearPendingReferralCode(it) }
        return ReferralClaimOutcome(
          success = false,
          message = "Self-referrals are not allowed. Share your code with a friend instead!"
        )
      }

      val existingClaimSnap = db.collection("referrals").document(uid).get().await()
      FirestoreDiagnostics.recordRead(1, "referral_duplicate_check")
      if (existingClaimSnap.exists()) {
        appContext?.let { ReferralInstallAttributionStore.clearPendingReferralCode(it) }
        return ReferralClaimOutcome(
          success = false,
          message = "You have already claimed a referral reward on this account.",
          inviterUid = existingClaimSnap.getString("inviterUid"),
          inviterName = existingClaimSnap.getString("inviterName"),
          alreadyClaimed = true
        )
      }

      val safeReferredName = referredProfile.displayName.trim().ifBlank { "New Learner" }.take(80)
      val safeTargetLang = referredProfile.targetLanguageCode.ifBlank { "es" }.take(10)
      val safeNativeLang = referredProfile.nativeLanguageCode.ifBlank { "en" }.take(10)

      val referralRef = db.collection("referrals").document(uid)
      val inviterSummaryRef = db.collection("referral_summaries").document(inviterUid)
      val referredSummaryRef = db.collection("referral_summaries").document(uid)
      val referredUserRef = db.collection("users").document(uid)

      val xpForInviter = ReferralRewardConfig.INVITER_XP_PER_REFERRAL

      db.runTransaction { tx ->
        val dupCheck = tx.get(referralRef)
        if (dupCheck.exists()) {
          throw IllegalStateException("ALREADY_CLAIMED")
        }

        val referredSummarySnap = tx.get(referredSummaryRef)
        val referredUserSnap = tx.get(referredUserRef)

        tx.set(
          referralRef,
          mapOf(
            "referralId" to uid,
            "inviterUid" to inviterUid,
            "inviterName" to inviterName,
            "referredUid" to uid,
            "referredName" to safeReferredName,
            "referralCode" to cleanCode,
            "status" to "completed",
            "inviterXpAwarded" to ReferralRewardConfig.INVITER_XP_PER_REFERRAL,
            "referredXpAwarded" to ReferralRewardConfig.REFERRED_WELCOME_XP,
            "milestone3BadgeAwarded" to false,
            "milestone5BonusAwarded" to false,
            "attributionSource" to normalizedSource,
            "targetLanguageCode" to safeTargetLang,
            "nativeLanguageCode" to safeNativeLang,
            "createdAt" to FieldValue.serverTimestamp(),
            "completedAt" to FieldValue.serverTimestamp()
          )
        )

        // Blind atomic transform on inviter summary (privacy-safe: friend never reads inviter summary)
        tx.update(
          inviterSummaryRef,
          mapOf(
            "successfulReferralsCount" to FieldValue.increment(1L),
            "totalXpEarnedFromReferrals" to FieldValue.increment(ReferralRewardConfig.INVITER_XP_PER_REFERRAL),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        if (referredUserSnap.exists()) {
          val prevUserXp = referredUserSnap.getLong("xpPoints") ?: 0L
          val nextUserXp = prevUserXp + ReferralRewardConfig.REFERRED_WELCOME_XP
          tx.update(
            referredUserRef,
            mapOf(
              "xpPoints" to nextUserXp,
              "level" to ((nextUserXp / 100L) + 1L),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }

        if (referredSummarySnap.exists()) {
          tx.update(
            referredSummaryRef,
            mapOf(
              "referredByCode" to cleanCode,
              "referredByUid" to inviterUid,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }.await()

      // Send celebration notification to inviter (best-effort)
      runCatching {
        db.collection("notifications")
          .document("ref_$uid")
          .set(
            mapOf(
              "userId" to inviterUid,
              "title" to "Friend Joined LingoSphere! 🎉",
              "message" to "$safeReferredName joined using your invite link! +$xpForInviter XP earned.",
              "type" to "achievement",
              "isRead" to false,
              "targetRoute" to "speaking_invitation",
              "targetId" to uid,
              "senderId" to uid,
              "senderName" to safeReferredName,
              "createdAt" to FieldValue.serverTimestamp()
            )
          )
          .await()
      }

      appContext?.let { ReferralInstallAttributionStore.clearPendingReferralCode(it) }
      cachedDashboardByUser.remove(uid)
      cachedDashboardByUser.remove(inviterUid)
      FirestoreQuotaManager.recordReadSuccess()

      ReferralClaimOutcome(
        success = true,
        message = "Referral applied! You and $inviterName each earned +${ReferralRewardConfig.REFERRED_WELCOME_XP} XP 🎉",
        inviterUid = inviterUid,
        inviterName = inviterName,
        xpAwardedToReferred = ReferralRewardConfig.REFERRED_WELCOME_XP,
        xpAwardedToInviter = xpForInviter,
        unlockedMilestoneBadge = null
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e.message?.contains("ALREADY_CLAIMED") == true) {
        appContext?.let { ReferralInstallAttributionStore.clearPendingReferralCode(it) }
        return ReferralClaimOutcome(
          success = false,
          message = "You have already claimed a referral reward on this account.",
          alreadyClaimed = true
        )
      }
      if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
        FirestoreQuotaManager.recordQuotaExhausted(e)
      }
      ReferralClaimOutcome(
        success = false,
        message = "Could not verify referral code right now. Please try again later."
      )
    }
  }

  private fun isCurrentAccountWithinNewRegistrationWindow(): Boolean {
    val meta = auth.currentUser?.metadata ?: return true
    val createdAt = meta.creationTimestamp
    if (createdAt <= 0L) return true
    return (System.currentTimeMillis() - createdAt) <= ReferralRewardConfig.NEW_ACCOUNT_WINDOW_MS
  }

  private suspend fun queryInviterReferrals(inviterUid: String): List<ReferralRecord> {
    return try {
      val snap = db.collection("referrals")
        .whereEqualTo("inviterUid", inviterUid)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)
        .get()
        .await()
      FirestoreDiagnostics.recordRead(snap.size().coerceAtLeast(1), "referrals_list")
      snap.documents.mapNotNull { parseReferralRecord(it) }
    } catch (e: Exception) {
      // Fallback query if composite index is still building
      val snap = db.collection("referrals")
        .whereEqualTo("inviterUid", inviterUid)
        .limit(50)
        .get()
        .await()
      FirestoreDiagnostics.recordRead(snap.size().coerceAtLeast(1), "referrals_list_fallback")
      snap.documents.mapNotNull { parseReferralRecord(it) }
        .sortedByDescending { it.completedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
    }
  }

  private suspend fun fetchCommunityProfileSafe(partnerId: String): CommunityProfile? {
    if (partnerId.isBlank()) return null
    return try {
      val doc = db.collection("community_profiles").document(partnerId).get().await()
      FirestoreDiagnostics.recordRead(1, "referral_friend_profile")
      if (!doc.exists()) null
      else CommunityProfile(
        userId = doc.getString("userId")?.takeIf { it.isNotBlank() } ?: doc.id,
        displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: "Lingo Partner",
        avatarUrl = doc.getString("avatarUrl").orEmpty(),
        country = doc.getString("country")?.takeIf { it.isNotBlank() } ?: "Global",
        countryFlag = doc.getString("countryFlag")?.takeIf { it.isNotBlank() } ?: "🌍",
        bio = doc.getString("bio")?.takeIf { it.isNotBlank() } ?: "Joined LingoSphere via your invite!",
        nativeLanguageCode = doc.getString("nativeLanguageCode")?.takeIf { it.isNotBlank() } ?: "en",
        targetLanguageCode = doc.getString("targetLanguageCode")?.takeIf { it.isNotBlank() } ?: "es",
        proficiencyLevel = doc.getString("proficiencyLevel")?.takeIf { it.isNotBlank() } ?: "Beginner",
        xpPoints = doc.getLong("xpPoints") ?: 0L,
        level = (doc.getLong("level") ?: 1L).coerceAtLeast(1L),
        isOnline = doc.getBoolean("isOnline") ?: false,
        allowCalls = doc.getBoolean("allowCalls") ?: true,
        presenceState = doc.getString("presenceState") ?: "offline"
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun parseReferralSummary(doc: DocumentSnapshot, fallbackUid: String): ReferralSummary {
    val badges = (doc.get("unlockedBadges") as? List<*>)
      ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
      ?: emptyList()
    return ReferralSummary(
      userId = doc.getString("userId")?.takeIf { it.isNotBlank() } ?: fallbackUid,
      referralCode = doc.getString("referralCode").orEmpty(),
      successfulReferralsCount = (doc.getLong("successfulReferralsCount") ?: 0L).coerceAtLeast(0L),
      pendingInvitationsCount = (doc.getLong("pendingInvitationsCount") ?: 0L).coerceAtLeast(0L),
      totalXpEarnedFromReferrals = (doc.getLong("totalXpEarnedFromReferrals") ?: 0L).coerceAtLeast(0L),
      unlockedBadges = badges,
      bonusSpeakingCredits = (doc.getLong("bonusSpeakingCredits") ?: 0L).coerceAtLeast(0L),
      referredByCode = doc.getString("referredByCode")?.takeIf { it.isNotBlank() },
      referredByUid = doc.getString("referredByUid")?.takeIf { it.isNotBlank() },
      updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    )
  }

  private fun parseReferralRecord(doc: DocumentSnapshot): ReferralRecord? {
    val referredUid = doc.getString("referredUid")?.takeIf { it.isNotBlank() } ?: doc.id
    if (referredUid.isBlank()) return null
    return ReferralRecord(
      referralId = doc.getString("referralId") ?: doc.id,
      inviterUid = doc.getString("inviterUid").orEmpty(),
      inviterName = doc.getString("inviterName") ?: "LingoSphere Friend",
      referredUid = referredUid,
      referredName = doc.getString("referredName") ?: "Invited Learner",
      referralCode = doc.getString("referralCode").orEmpty(),
      status = doc.getString("status") ?: "completed",
      inviterXpAwarded = doc.getLong("inviterXpAwarded") ?: ReferralRewardConfig.INVITER_XP_PER_REFERRAL,
      referredXpAwarded = doc.getLong("referredXpAwarded") ?: ReferralRewardConfig.REFERRED_WELCOME_XP,
      milestone3BadgeAwarded = doc.getBoolean("milestone3BadgeAwarded") ?: false,
      milestone5BonusAwarded = doc.getBoolean("milestone5BonusAwarded") ?: false,
      attributionSource = doc.getString("attributionSource") ?: "app_link",
      targetLanguageCode = doc.getString("targetLanguageCode") ?: "es",
      nativeLanguageCode = doc.getString("nativeLanguageCode") ?: "en",
      createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
      completedAt = doc.getTimestamp("completedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    )
  }

  private companion object {
    const val DASHBOARD_CACHE_TTL_MS = 5 * 60_000L
  }
}
