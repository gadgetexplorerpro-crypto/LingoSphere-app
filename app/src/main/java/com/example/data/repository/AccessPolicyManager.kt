package com.example.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

/**
 * Canonical Feature Keys used across both the LingoSphere Android app and the
 * Firebase Cloud Functions backend (`config/accessPolicy`).
 *
 * Strictly TWO plans exist:
 *  - Free ($0 forever)
 *  - LingoSphere Pro (Monthly $9.99 / Yearly $79.99)
 */
object AccessPolicyFeatureKeys {
  const val LESSONS_DAILY = "lessons.daily"
  const val AI_VOICE_SECONDS = "ai.voice.seconds"
  const val AI_TEXT_MESSAGES = "ai.text.messages"
  const val AI_ROLEPLAY_SESSIONS = "ai.roleplay.sessions"
  const val AI_CORRECTIONS = "ai.corrections"
  const val BOOKS_AUDIO = "books.audio"
  const val BOOKS_READING = "books.reading"
  const val LISTENING = "listening"
  const val GAMES = "games"
  const val LABS = "labs"
  const val STUDIO = "studio"
  const val VOICES = "voices"
  const val CULTURE = "culture"
  const val EXCHANGE_SMART_MATCH = "exchange.smartMatch"
  const val CALLS_AUDIO = "calls.audio"
  const val CALLS_VIDEO = "calls.video"
  const val ROOMS = "rooms"
  const val ANALYTICS = "analytics"
  const val INTERVIEW_PREP = "interviewPrep"
  const val PERSONALIZED_MATERIAL = "personalizedMaterial"

  val ALL_KEYS = listOf(
    LESSONS_DAILY,
    AI_VOICE_SECONDS,
    AI_TEXT_MESSAGES,
    AI_ROLEPLAY_SESSIONS,
    AI_CORRECTIONS,
    BOOKS_AUDIO,
    BOOKS_READING,
    LISTENING,
    GAMES,
    LABS,
    STUDIO,
    VOICES,
    CULTURE,
    EXCHANGE_SMART_MATCH,
    CALLS_AUDIO,
    CALLS_VIDEO,
    ROOMS,
    ANALYTICS,
    INTERVIEW_PREP,
    PERSONALIZED_MATERIAL
  )
}

/**
 * Authoritative subscription entitlement state machine stored at
 * `users/{uid}/private/entitlement` (server-written only; read-only for the authenticated user).
 */
enum class EntitlementStatus(val code: String) {
  FREE("free"),
  PRO_ACTIVE("pro_active"),
  PRO_GRACE("pro_grace"),
  PRO_ON_HOLD("pro_on_hold"),
  EXPIRED("expired");

  val grantsProAccess: Boolean
    get() = this == PRO_ACTIVE || this == PRO_GRACE

  companion object {
    fun fromCode(raw: String?): EntitlementStatus {
      return when (raw?.trim()?.lowercase(Locale.US)) {
        "pro_active", "active", "sub_state_active", "subscription_state_active", "subscription_state_canceled" -> PRO_ACTIVE
        "pro_grace", "in_grace_period", "subscription_state_in_grace_period" -> PRO_GRACE
        "pro_on_hold", "on_hold", "paused", "subscription_state_on_hold", "subscription_state_paused" -> PRO_ON_HOLD
        "expired", "revoked", "subscription_state_expired" -> EXPIRED
        else -> FREE
      }
    }
  }
}

data class ServerEntitlementRecord(
  val uid: String = "",
  val userId: String = uid,
  val status: EntitlementStatus = EntitlementStatus.FREE,
  val planId: String = "free", // "free" or "lingosphere_pro"
  val productId: String = planId,
  val basePlanId: String? = null, // "monthly" or "yearly"
  val subscriptionTier: String = "free", // "free", "premium_monthly", "premium_yearly", "premium_pro"
  val expiryEpochMs: Long = 0L,
  val expiryTimeMillis: Long = expiryEpochMs,
  val autoRenewing: Boolean = false,
  val inGracePeriod: Boolean = false,
  val onHold: Boolean = false,
  val verifiedWithPlayApi: Boolean = false,
  val purchaseTokenHash: String? = null,
  val lastVerifiedEpochMs: Long = 0L,
  val timezone: String = TimeZone.getDefault().id
) {
  val effectiveUid: String
    get() = uid.ifBlank { userId }

  val effectiveExpiryMs: Long
    get() = if (expiryEpochMs > 0L) expiryEpochMs else expiryTimeMillis

  val hasProEntitlement: Boolean
    get() {
      if (!status.grantsProAccess) return false
      val exp = effectiveExpiryMs
      if (exp > 0L && System.currentTimeMillis() > exp && !inGracePeriod) {
        return false
      }
      return true
    }
}

/**
 * Server-driven access policy stored in Firestore at `config/accessPolicy`.
 * Cached in memory by the backend and read-only for the Android client UI.
 */
data class PlanAccessLimits(
  // Free Plan ($0 forever) — Zero AI usage; curated non-AI learning only
  val lessonsProOnly: Boolean = true,
  val roleplayProOnly: Boolean = true,
  val allAiProOnly: Boolean = true,
  val freeDailyLessons: Int = 0,
  val freeDailyAiVoiceSeconds: Int = 0,
  val freeDailyAiTextMessages: Int = 0,
  val freeDailyAiRoleplaySessions: Int = 0,
  val freeDailyAiCorrections: Int = 0,
  val freeBooksPerMonth: Int = 5,
  val freeAudiobooksCount: Int = 5,
  val freeReadingBooksCount: Int = 5,
  val freeDailyListeningExercises: Int = 3,
  val freeMaxGameLevel: Int = 5, // First 5 levels of each non-AI game
  val freeLifetimePracticalLabsTrials: Int = 0,
  val freeDailyStudioSessions: Int = 0,
  val freeAiTutorVoiceIds: List<String> = emptyList(),
  val freeCultureJourneyPreviewsPerCountry: Int = 1,
  val freeDailyHumanAudioCallMinutes: Int = 20,
  val freeDailyHumanVideoCallMinutes: Int = 10,
  val freeSpeakingRoomsCanListenAndJoinPublic: Boolean = true,
  val freeSpeakingRoomsCanHost: Boolean = false,

  // LingoSphere Pro Plan (Monthly $9.99 / Yearly $79.99)
  val proUnlimitedLessons: Boolean = true, // All 500+ sequential lessons
  val proDailyAiVoiceSeconds: Int = 900, // 15 min/day (ai.tutor.proSecondsPerDay)
  val proMonthlyAiVoiceSeconds: Int = 9000, // 150 min/month (ai.tutor.proSecondsPerMonth)
  val proDailyAiTextMessages: Int = 150,
  val proDailyAiRoleplaySessions: Int = 8,
  val proDailyAiCorrections: Int = 50,
  val proDailyPersonalizedMaterials: Int = 20,
  val proAllVoicesUnlocked: Boolean = true, // All 15 AI tutor voices
  val proAllBooksUnlocked: Boolean = true, // Full LibriVox & Gutendex catalog + summaries + narration
  val proAllListeningAndGamesUnlocked: Boolean = true,
  val proFullLabsStudioCultureUnlocked: Boolean = true,
  val proSmartMatchAndRoomsHostUnlocked: Boolean = true,
  val proDailyHumanAudioCallMinutes: Int = 120,
  val proDailyHumanVideoCallMinutes: Int = 60,
  val proFullAnalyticsAndInterviewPrepUnlocked: Boolean = true,

  // Canonical AI Tutor Server-Driven Allowances (config/accessPolicy)
  val aiTutorFreeSecondsPerDay: Int = 0, // Zero Free AI usage
  val aiTutorProSecondsPerDay: Int = 900, // 15 minutes/day for Pro
  val aiTutorProSecondsPerMonth: Int = 9000, // 150 minutes/month for Pro
  val aiTutorMaxSessionSeconds: Int = 600, // 10 minutes max per single session
  val aiTutorIdleTimeoutSeconds: Int = 45 // Auto-end after 45s with no user speech
)

/**
 * Designated free books configuration stored at `config/freeContent` in Firestore.
 * Free users get full reading & audio access to these 2 audiobooks and 2 reading books.
 */
data class FreeContentConfig(
  val freeAudiobookIds: List<String> = listOf(
    "gutenberg_1661", // The Adventures of Sherlock Holmes (LibriVox human audio + text)
    "gutenberg_11"    // Alice's Adventures in Wonderland (LibriVox human audio + text)
  ),
  val freeReadingBookIds: List<String> = listOf(
    "gutenberg_1342", // Pride and Prejudice (Gutendex full text)
    "gutenberg_4507"  // As a Man Thinketh (Gutendex full text)
  ),
  val freeReadingAcademyPassageIds: List<String> = listOf(
    "story_1",
    "story_2"
  )
) {
  fun isFreeAudiobook(bookId: String, gutendexId: Int? = null): Boolean {
    val clean = bookId.trim().lowercase(Locale.US)
    if (freeAudiobookIds.any { it.equals(clean, ignoreCase = true) }) return true
    if (gutendexId != null && freeAudiobookIds.any { it.equals("gutenberg_$gutendexId", ignoreCase = true) || it == gutendexId.toString() }) {
      return true
    }
    return false
  }

  fun isFreeReadingBook(bookId: String, gutendexId: Int? = null): Boolean {
    val clean = bookId.trim().lowercase(Locale.US)
    if (freeReadingBookIds.any { it.equals(clean, ignoreCase = true) }) return true
    if (freeReadingAcademyPassageIds.any { it.equals(clean, ignoreCase = true) }) return true
    if (gutendexId != null && freeReadingBookIds.any { it.equals("gutenberg_$gutendexId", ignoreCase = true) || it == gutendexId.toString() }) {
      return true
    }
    return false
  }

  fun isBookUnlockedForFreeUser(bookId: String, gutendexId: Int? = null): Boolean {
    return isFreeAudiobook(bookId, gutendexId) || isFreeReadingBook(bookId, gutendexId)
  }
}

data class AccessCheckDecision(
  val featureKey: String,
  val allowed: Boolean,
  val isProUser: Boolean,
  val usedUnits: Int = 0,
  val limitUnits: Int = 0,
  val remainingUnits: Int = 0,
  val softWarning80Percent: Boolean = false,
  val reasonMessage: String = "",
  val resetTimeDescription: String = ""
)

enum class AiTutorLimitReason {
  FREE_DAILY_60S,
  PRO_DAILY_15M,
  PRO_MONTHLY_150M
}

/**
 * Real-time AI Tutor quota and session state synchronized with `usage/{uid}/daily/{yyyy-mm-dd}`
 * and `usage/{uid}/monthly/{yyyy-mm}`.
 */
data class AiTutorUsageState(
  val uid: String = "",
  val isPro: Boolean = false,
  val usedDailySeconds: Int = 0,
  val usedMonthlySeconds: Int = 0,
  val dailyLimitSeconds: Int = 0,
  val monthlyLimitSeconds: Int = 0,
  val remainingTodaySeconds: Int = 0,
  val remainingMonthSeconds: Int = 0,
  val effectiveRemainingSeconds: Int = 0,
  val maxSingleSessionSeconds: Int = 600,
  val dailyLimitReached: Boolean = true,
  val monthlyLimitReached: Boolean = false,
  val softWarning80PercentMonthly: Boolean = false,
  val activeSessionId: String? = null,
  val activeDeviceId: String? = null,
  val nextDailyResetEpochMs: Long = 0L,
  val nextMonthlyResetDateLabel: String = ""
) {
  val dailyUsedSeconds: Int
    get() = usedDailySeconds

  val monthlyUsedSeconds: Int
    get() = usedMonthlySeconds

  val remainingSecondsToday: Int
    get() = effectiveRemainingSeconds

  val isMonthlyExhausted: Boolean
    get() = monthlyLimitReached

  val isNearMonthlyLimit: Boolean
    get() = softWarning80PercentMonthly

  val nextMonthResetDateLabel: String
    get() = nextMonthlyResetDateLabel

  val nextResetEpochMs: Long
    get() = nextDailyResetEpochMs

  val limitReachedReason: AiTutorLimitReason?
    get() = when {
      effectiveRemainingSeconds > 0 -> null
      isPro && monthlyLimitReached -> AiTutorLimitReason.PRO_MONTHLY_150M
      isPro -> AiTutorLimitReason.PRO_DAILY_15M
      else -> AiTutorLimitReason.FREE_DAILY_60S
    }

  fun formatRemainingMmSs(): String {
    val rem = effectiveRemainingSeconds.coerceAtLeast(0)
    val mins = rem / 60
    val secs = rem % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
  }

  fun formatHomeSubtitle(): String {
    if (!isPro) return "Unlock AI learning with Pro"
    val rem = effectiveRemainingSeconds.coerceAtLeast(0)
    if (rem <= 0) {
      return if (monthlyLimitReached && nextMonthlyResetDateLabel.isNotBlank()) {
        "Resets on $nextMonthlyResetDateLabel"
      } else {
        AccessPolicyManager.formatCountdownToMidnight()
      }
    }
    val mins = rem / 60
    val secs = rem % 60
    return String.format(Locale.US, "%d:%02d remaining", mins, secs)
  }
}

/**
 * Single shared policy helper (`AccessPolicyManager`) for Phase 1–6 & AI Tutor Free/Pro enforcement.
 * Synchronizes `config/accessPolicy`, `config/freeContent`, and `users/{uid}/private/entitlement`
 * from Firestore and provides `canUserAccess(featureKey, ...)` across all screens.
 */
object AccessPolicyManager {

  private const val SEVEN_DAYS_MS = 7L * 24L * 3600L * 1000L

  @Volatile
  var lastFirestoreInstance: FirebaseFirestore? = null
    private set

  private val _policyState = MutableStateFlow(PlanAccessLimits())
  val policyState: StateFlow<PlanAccessLimits> = _policyState.asStateFlow()
  val limitsState: StateFlow<PlanAccessLimits>
    get() = policyState

  private val _freeContentState = MutableStateFlow(FreeContentConfig())
  val freeContentState: StateFlow<FreeContentConfig> = _freeContentState.asStateFlow()

  private val _entitlementState = MutableStateFlow(ServerEntitlementRecord())
  val entitlementState: StateFlow<ServerEntitlementRecord> = _entitlementState.asStateFlow()

  private val _isEntitlementLoading = MutableStateFlow(false)
  val isEntitlementLoading: StateFlow<Boolean> = _isEntitlementLoading.asStateFlow()

  private val _tutorUsageState = MutableStateFlow(AiTutorUsageState())
  val tutorUsageState: StateFlow<AiTutorUsageState> = _tutorUsageState.asStateFlow()

  private val _openedBookIdsThisMonth = MutableStateFlow<Set<String>>(emptySet())
  val openedBookIdsThisMonth: StateFlow<Set<String>> = _openedBookIdsThisMonth.asStateFlow()

  val freeBooksRemainingThisMonth: Int
    get() {
      val bonusBooks = com.example.ads.AdService.state.value.bonusBooksUnlockedThisMonth
      val totalLimit = _policyState.value.freeBooksPerMonth + bonusBooks
      return (totalLimit - _openedBookIdsThisMonth.value.size).coerceAtLeast(0)
    }

  fun canOpenBookThisMonth(
    userProfile: com.example.data.model.UserProfile?,
    bookId: String,
    gutendexId: Int? = null
  ): AccessCheckDecision {
    val isPro = isUserVerifiedPro(userProfile)
    if (isPro) {
      return AccessCheckDecision(
        featureKey = AccessPolicyFeatureKeys.BOOKS_READING,
        allowed = true,
        isProUser = true,
        remainingUnits = Int.MAX_VALUE
      )
    }
    val cleanId = normalizeBookKey(bookId, gutendexId)
    val opened = _openedBookIdsThisMonth.value
    val bonusBooks = com.example.ads.AdService.state.value.bonusBooksUnlockedThisMonth
    val limit = (_policyState.value.freeBooksPerMonth + bonusBooks).coerceAtLeast(1)
    val alreadyOpened = cleanId.isNotBlank() && opened.any { it.equals(cleanId, ignoreCase = true) }
    val unlockedByRewardedAd = com.example.ads.AdService.hasActiveRewardedUnlock(
      com.example.ads.RewardedFeatureType.BONUS_BOOK,
      cleanId
    )
    val allowed = alreadyOpened || unlockedByRewardedAd || opened.size < limit
    val remaining = (limit - opened.size).coerceAtLeast(0)
    return AccessCheckDecision(
      featureKey = AccessPolicyFeatureKeys.BOOKS_READING,
      allowed = allowed,
      isProUser = false,
      usedUnits = opened.size,
      limitUnits = limit,
      remainingUnits = remaining,
      reasonMessage = if (allowed) "" else "You've opened $limit of $limit free books this month. Watch a short ad for +1 bonus book or upgrade to Pro for unlimited books."
    )
  }

  fun recordOpenedBookLocallyForMonth(bookId: String, gutendexId: Int? = null) {
    val cleanId = normalizeBookKey(bookId, gutendexId)
    if (cleanId.isBlank()) return
    _openedBookIdsThisMonth.update { prev -> prev + cleanId }
  }

  fun setOpenedBooksThisMonthForTesting(bookIds: Set<String>) {
    _openedBookIdsThisMonth.value = bookIds
  }

  private fun normalizeBookKey(bookId: String, gutendexId: Int? = null): String {
    val trimmed = bookId.trim()
    if (trimmed.isNotBlank()) return trimmed.lowercase(Locale.US)
    return gutendexId?.let { "gutenberg_$it" } ?: ""
  }

  fun isUserVerifiedPro(userProfile: com.example.data.model.UserProfile?): Boolean {
    val ent = _entitlementState.value
    val entUid = ent.effectiveUid
    return when {
      userProfile == null -> entUid.isNotBlank() && ent.hasProEntitlement
      userProfile.userId.isBlank() || userProfile.userId.startsWith("guest", ignoreCase = true) || userProfile.userId == "default_user" -> false
      userProfile.isSubscriptionExpired && (entUid.isBlank() || entUid != userProfile.userId || !ent.hasProEntitlement) -> false
      entUid.isNotBlank() && entUid == userProfile.userId -> ent.hasProEntitlement
      else -> userProfile.isEntitlementVerified && !userProfile.isSubscriptionExpired
    }
  }

  // Session-scoped upgrade prompt tracker: shows the contextual upgrade sheet at most once per feature per session
  private val shownUpgradeSheetFeaturesThisSession: MutableSet<String> = ConcurrentHashMap.newKeySet()

  // Local timezone tracking with 7-day cooldown to prevent timezone-hopping daily reset abuse
  @Volatile
  private var storedUserTimezoneId: String = TimeZone.getDefault().id

  @Volatile
  private var lastTimezoneUpdateEpochMs: Long = 0L

  private var entitlementListener: ListenerRegistration? = null
  private var dailyUsageListener: ListenerRegistration? = null
  private var monthlyUsageListener: ListenerRegistration? = null

  /**
   * Returns true if the contextual upgrade sheet has NOT been shown for [featureKey] during this app session,
   * and marks it as shown.
   */
  fun consumeSessionUpgradePromptAllowance(featureKey: String): Boolean {
    return shownUpgradeSheetFeaturesThisSession.add(featureKey)
  }

  fun hasShownUpgradePromptThisSession(featureKey: String): Boolean {
    return shownUpgradeSheetFeaturesThisSession.contains(featureKey)
  }

  fun resetSessionUpgradePromptsForTesting() {
    shownUpgradeSheetFeaturesThisSession.clear()
  }

  /**
   * Updates the user's stored timezone only if no timezone change occurred within the past 7 days.
   * Returns true if the timezone was updated, or false if rejected by the 7-day cooldown.
   */
  fun tryUpdateUserTimezone(
    newTimezoneId: String,
    nowEpochMs: Long = System.currentTimeMillis()
  ): Boolean {
    val cleanTz = newTimezoneId.trim()
    if (cleanTz.isBlank()) return false
    if (cleanTz == storedUserTimezoneId) {
      if (lastTimezoneUpdateEpochMs == 0L) {
        lastTimezoneUpdateEpochMs = nowEpochMs
      }
      return true
    }
    if (lastTimezoneUpdateEpochMs > 0L && (nowEpochMs - lastTimezoneUpdateEpochMs) < SEVEN_DAYS_MS) {
      return false
    }
    storedUserTimezoneId = cleanTz
    lastTimezoneUpdateEpochMs = nowEpochMs
    return true
  }

  fun getEffectiveUserTimezoneId(): String = storedUserTimezoneId

  fun setStoredTimezoneForTesting(timezoneId: String, updatedAtEpochMs: Long) {
    storedUserTimezoneId = timezoneId
    lastTimezoneUpdateEpochMs = updatedAtEpochMs
  }

  /**
   * Computes the `yyyy-MM-dd` daily usage bucket key in the user's stored timezone.
   */
  fun currentDailyKey(nowEpochMs: Long = System.currentTimeMillis(), timezoneId: String = storedUserTimezoneId): String {
    val tz = TimeZone.getTimeZone(timezoneId)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = tz
    return sdf.format(Date(nowEpochMs))
  }

  /**
   * Computes the `yyyy-MM` monthly usage bucket key in UTC.
   */
  fun currentMonthlyKeyUtc(nowEpochMs: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(nowEpochMs))
  }

  /**
   * Human-friendly reset time label in the user's stored timezone.
   */
  fun getNextDailyResetLabel(timezoneId: String = storedUserTimezoneId): String {
    val tz = TimeZone.getTimeZone(timezoneId)
    return "Resets at midnight (${tz.id})"
  }

  /**
   *Evaluates `canUserAccess(featureKey, ...)` for any feature key using the single shared policy table.
   */
  fun canUserAccess(
    featureKey: String,
    isProUser: Boolean,
    usedDailyUnits: Int = 0,
    usedMonthlyUnits: Int = 0,
    usedLifetimeUnits: Int = 0,
    requestedUnits: Int = 1,
    itemId: String? = null,
    gutendexId: Int? = null,
    gameLevel: Int = 1,
    countryPreviewIndex: Int = 0
  ): AccessCheckDecision {
    val policy = _policyState.value
    val freeContent = _freeContentState.value
    val resetDesc = getNextDailyResetLabel()

    return when (featureKey) {
      AccessPolicyFeatureKeys.LESSONS_DAILY -> {
        val allowed = isProUser
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = allowed,
          isProUser = isProUser,
          usedUnits = usedDailyUnits,
          limitUnits = if (isProUser) Int.MAX_VALUE else 0,
          remainingUnits = if (isProUser) Int.MAX_VALUE else 0,
          reasonMessage = if (allowed) "" else "Structured CEFR lessons are included with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.AI_VOICE_SECONDS -> {
        if (!isProUser) {
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = false,
            isProUser = false,
            usedUnits = 0,
            limitUnits = 0,
            remainingUnits = 0,
            reasonMessage = "Unlock AI learning with LingoSphere Pro for live AI voice conversations (15 min/day · 150 min/mo)."
          )
        } else {
          val dailyLimit = policy.proDailyAiVoiceSeconds
          val monthlyLimit = policy.proMonthlyAiVoiceSeconds
          val dailyRemaining = (dailyLimit - usedDailyUnits).coerceAtLeast(0)
          val monthlyRemaining = (monthlyLimit - usedMonthlyUnits).coerceAtLeast(0)
          val effectiveRemaining = minOf(dailyRemaining, monthlyRemaining)
          val allowed = requestedUnits <= effectiveRemaining
          val usageRatio = maxOf(
            usedDailyUnits.toFloat() / dailyLimit.coerceAtLeast(1).toFloat(),
            usedMonthlyUnits.toFloat() / monthlyLimit.coerceAtLeast(1).toFloat()
          )
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = true,
            usedUnits = usedDailyUnits,
            limitUnits = dailyLimit,
            remainingUnits = effectiveRemaining,
            softWarning80Percent = usageRatio >= 0.80f && allowed,
            reasonMessage = if (allowed) {
              if (usageRatio >= 0.80f) "You've used ${(usageRatio * 100).toInt()}% of your AI voice allowance." else ""
            } else {
              "You've reached your LingoSphere Pro AI Voice safety allowance (${dailyLimit / 60} min/day or ${monthlyLimit / 60} min/month). $resetDesc."
            },
            resetTimeDescription = resetDesc
          )
        }
      }

      AccessPolicyFeatureKeys.AI_TEXT_MESSAGES -> {
        if (!isProUser) {
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = false,
            isProUser = false,
            usedUnits = 0,
            limitUnits = 0,
            remainingUnits = 0,
            reasonMessage = "AI chat & tutoring are unlocked with LingoSphere Pro."
          )
        } else {
          val limit = policy.proDailyAiTextMessages
          val remaining = (limit - usedDailyUnits).coerceAtLeast(0)
          val allowed = (usedDailyUnits + requestedUnits) <= limit
          val ratio = usedDailyUnits.toFloat() / limit.coerceAtLeast(1).toFloat()
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = true,
            usedUnits = usedDailyUnits,
            limitUnits = limit,
            remainingUnits = remaining,
            softWarning80Percent = ratio >= 0.80f && allowed,
            reasonMessage = if (allowed) "" else "Daily AI chat safety cap ($limit messages/day) reached. $resetDesc.",
            resetTimeDescription = resetDesc
          )
        }
      }

      AccessPolicyFeatureKeys.AI_ROLEPLAY_SESSIONS -> {
        if (!isProUser) {
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = false,
            isProUser = false,
            usedUnits = 0,
            limitUnits = 0,
            remainingUnits = 0,
            reasonMessage = "AI Roleplay & real-world scenarios are included with LingoSphere Pro."
          )
        } else {
          val limit = policy.proDailyAiRoleplaySessions
          val remaining = (limit - usedDailyUnits).coerceAtLeast(0)
          val allowed = (usedDailyUnits + requestedUnits) <= limit
          val ratio = usedDailyUnits.toFloat() / limit.coerceAtLeast(1).toFloat()
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = true,
            usedUnits = usedDailyUnits,
            limitUnits = limit,
            remainingUnits = remaining,
            softWarning80Percent = ratio >= 0.80f && allowed,
            reasonMessage = if (allowed) "" else "Daily AI roleplay limit ($limit sessions/day) reached. $resetDesc.",
            resetTimeDescription = resetDesc
          )
        }
      }

      AccessPolicyFeatureKeys.AI_CORRECTIONS -> {
        if (!isProUser) {
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = false,
            isProUser = false,
            usedUnits = 0,
            limitUnits = 0,
            remainingUnits = 0,
            reasonMessage = "AI grammar, writing & pronunciation feedback is unlocked with LingoSphere Pro."
          )
        } else {
          val limit = policy.proDailyAiCorrections
          val remaining = (limit - usedDailyUnits).coerceAtLeast(0)
          val allowed = (usedDailyUnits + requestedUnits) <= limit
          val ratio = usedDailyUnits.toFloat() / limit.coerceAtLeast(1).toFloat()
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = true,
            usedUnits = usedDailyUnits,
            limitUnits = limit,
            remainingUnits = remaining,
            softWarning80Percent = ratio >= 0.80f && allowed,
            reasonMessage = if (allowed) "" else "Daily grammar & writing corrections cap ($limit/day) reached. $resetDesc.",
            resetTimeDescription = resetDesc
          )
        }
      }

      AccessPolicyFeatureKeys.BOOKS_AUDIO,
      AccessPolicyFeatureKeys.BOOKS_READING -> {
        if (isProUser) {
          AccessCheckDecision(featureKey = featureKey, allowed = true, isProUser = true, remainingUnits = Int.MAX_VALUE)
        } else {
          val opened = _openedBookIdsThisMonth.value
          val limit = policy.freeBooksPerMonth.coerceAtLeast(1)
          val cleanKey = if (!itemId.isNullOrBlank()) normalizeBookKey(itemId, gutendexId) else ""
          val alreadyOpened = cleanKey.isNotBlank() && opened.any { it.equals(cleanKey, ignoreCase = true) }
          val allowed = alreadyOpened || opened.size < limit
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = false,
            usedUnits = opened.size,
            limitUnits = limit,
            remainingUnits = (limit - opened.size).coerceAtLeast(0),
            reasonMessage = if (allowed) "" else "Free includes $limit books per month. Upgrade to LingoSphere Pro for unlimited books and audio narration."
          )
        }
      }

      AccessPolicyFeatureKeys.LISTENING -> {
        if (isProUser) {
          AccessCheckDecision(featureKey = featureKey, allowed = true, isProUser = true, remainingUnits = Int.MAX_VALUE)
        } else {
          val limit = policy.freeDailyListeningExercises
          val allowed = (usedDailyUnits + requestedUnits) <= limit
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = false,
            usedUnits = usedDailyUnits,
            limitUnits = limit,
            remainingUnits = (limit - usedDailyUnits).coerceAtLeast(0),
            reasonMessage = if (allowed) "" else "You've completed your $limit free listening exercises today. $resetDesc, or upgrade to LingoSphere Pro for unlimited listening.",
            resetTimeDescription = resetDesc
          )
        }
      }

      AccessPolicyFeatureKeys.GAMES -> {
        if (isProUser) {
          AccessCheckDecision(featureKey = featureKey, allowed = true, isProUser = true, remainingUnits = Int.MAX_VALUE)
        } else {
          val allowed = gameLevel <= policy.freeMaxGameLevel
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = false,
            usedUnits = gameLevel,
            limitUnits = policy.freeMaxGameLevel,
            remainingUnits = (policy.freeMaxGameLevel - gameLevel + 1).coerceAtLeast(0),
            reasonMessage = if (allowed) "" else "Free includes the first ${policy.freeMaxGameLevel} levels of each language game. Unlock all levels with LingoSphere Pro."
          )
        }
      }

      AccessPolicyFeatureKeys.LABS -> {
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = isProUser,
          isProUser = isProUser,
          usedUnits = if (isProUser) usedLifetimeUnits else 0,
          limitUnits = if (isProUser) Int.MAX_VALUE else 0,
          remainingUnits = if (isProUser) Int.MAX_VALUE else 0,
          reasonMessage = if (isProUser) "" else "AI Speaking Labs are unlocked with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.STUDIO -> {
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = isProUser,
          isProUser = isProUser,
          usedUnits = if (isProUser) usedDailyUnits else 0,
          limitUnits = if (isProUser) Int.MAX_VALUE else 0,
          remainingUnits = if (isProUser) Int.MAX_VALUE else 0,
          reasonMessage = if (isProUser) "" else "AI Studios & personalized learning labs are unlocked with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.VOICES -> {
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = isProUser,
          isProUser = isProUser,
          usedUnits = 0,
          limitUnits = if (isProUser) 15 else 0,
          remainingUnits = if (isProUser) 15 else 0,
          reasonMessage = if (isProUser) "" else "All 15 AI tutor voices are unlocked with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.CULTURE -> {
        if (isProUser) {
          AccessCheckDecision(featureKey = featureKey, allowed = true, isProUser = true, remainingUnits = Int.MAX_VALUE)
        } else {
          val allowed = countryPreviewIndex < policy.freeCultureJourneyPreviewsPerCountry
          AccessCheckDecision(
            featureKey = featureKey,
            allowed = allowed,
            isProUser = false,
            usedUnits = countryPreviewIndex,
            limitUnits = policy.freeCultureJourneyPreviewsPerCountry,
            remainingUnits = if (allowed) 1 else 0,
            reasonMessage = if (allowed) "" else "Free includes 1 preview per country. Unlock full Culture Journey immersion across all countries with LingoSphere Pro."
          )
        }
      }

      AccessPolicyFeatureKeys.EXCHANGE_SMART_MATCH -> {
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = isProUser,
          isProUser = isProUser,
          limitUnits = if (isProUser) 1 else 0,
          remainingUnits = if (isProUser) 1 else 0,
          reasonMessage = if (isProUser) "" else "Smart Partner Matching & priority filters are included with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.CALLS_AUDIO -> {
        val limit = if (isProUser) policy.proDailyHumanAudioCallMinutes else policy.freeDailyHumanAudioCallMinutes
        val allowed = (usedDailyUnits + requestedUnits) <= limit
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = allowed,
          isProUser = isProUser,
          usedUnits = usedDailyUnits,
          limitUnits = limit,
          remainingUnits = (limit - usedDailyUnits).coerceAtLeast(0),
          reasonMessage = if (allowed) "" else if (isProUser) {
            "Daily human audio call limit ($limit min/day) reached. $resetDesc."
          } else {
            "You've used your $limit free minutes of human audio calls today. $resetDesc, or upgrade to LingoSphere Pro for 120 min/day."
          },
          resetTimeDescription = resetDesc
        )
      }

      AccessPolicyFeatureKeys.CALLS_VIDEO -> {
        val limit = if (isProUser) policy.proDailyHumanVideoCallMinutes else policy.freeDailyHumanVideoCallMinutes
        val allowed = (usedDailyUnits + requestedUnits) <= limit
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = allowed,
          isProUser = isProUser,
          usedUnits = usedDailyUnits,
          limitUnits = limit,
          remainingUnits = (limit - usedDailyUnits).coerceAtLeast(0),
          reasonMessage = if (allowed) "" else if (isProUser) {
            "Daily human video call limit ($limit min/day) reached. $resetDesc."
          } else {
            "You've used your $limit free minutes of human video calls today. $resetDesc, or upgrade to LingoSphere Pro for 60 min/day."
          },
          resetTimeDescription = resetDesc
        )
      }

      AccessPolicyFeatureKeys.ROOMS -> {
        val wantsHost = itemId == "host"
        val allowed = if (isProUser) true else !wantsHost
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = allowed,
          isProUser = isProUser,
          limitUnits = 1,
          remainingUnits = if (allowed) 1 else 0,
          reasonMessage = if (allowed) "" else "Free learners can listen and join public Speaking Rooms. Upgrade to LingoSphere Pro to host your own Speaking Rooms."
        )
      }

      AccessPolicyFeatureKeys.ANALYTICS -> {
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = isProUser,
          isProUser = isProUser,
          limitUnits = if (isProUser) 1 else 0,
          remainingUnits = if (isProUser) 1 else 0,
          reasonMessage = if (isProUser) "" else "Advanced fluency analytics & personalized learning roadmap are available with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.INTERVIEW_PREP -> {
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = isProUser,
          isProUser = isProUser,
          limitUnits = if (isProUser) 1 else 0,
          remainingUnits = if (isProUser) 1 else 0,
          reasonMessage = if (isProUser) "" else "Full AI Job Interview Prep simulator with executive rubric scoring is unlocked with LingoSphere Pro."
        )
      }

      AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL -> {
        val limit = if (isProUser) policy.proDailyPersonalizedMaterials else 0
        val allowed = isProUser && (usedDailyUnits + requestedUnits) <= limit
        AccessCheckDecision(
          featureKey = featureKey,
          allowed = allowed,
          isProUser = isProUser,
          usedUnits = usedDailyUnits,
          limitUnits = limit,
          remainingUnits = (limit - usedDailyUnits).coerceAtLeast(0),
          reasonMessage = if (allowed) "" else if (isProUser) {
            "You've generated your $limit personalized AI learning materials today. $resetDesc."
          } else {
            "AI-generated custom learning materials and personalized study packs are a LingoSphere Pro feature (10/day)."
          },
          resetTimeDescription = resetDesc
        )
      }

      else -> AccessCheckDecision(featureKey = featureKey, allowed = true, isProUser = isProUser)
    }
  }

  /**
   * Overload accepting `UserProfile` directly (evaluates server-verified entitlement for `userProfile.userId`).
   */
  fun canUserAccess(
    featureKey: String,
    userProfile: com.example.data.model.UserProfile?,
    usedToday: Int = 0,
    usedMonth: Int = 0,
    usedLifetime: Int = 0,
    requestedUnits: Int = 1,
    itemId: String? = null,
    gutendexId: Int? = null,
    gameLevel: Int = 1,
    countryPreviewIndex: Int = 0
  ): AccessCheckDecision {
    val isVerifiedPro = isUserVerifiedPro(userProfile)
    return canUserAccess(
      featureKey = featureKey,
      isProUser = isVerifiedPro,
      usedDailyUnits = usedToday,
      usedMonthlyUnits = usedMonth,
      usedLifetimeUnits = usedLifetime,
      requestedUnits = requestedUnits,
      itemId = itemId,
      gutendexId = gutendexId,
      gameLevel = gameLevel,
      countryPreviewIndex = countryPreviewIndex
    )
  }

  fun resetSessionUpgradePrompts() {
    shownUpgradeSheetFeaturesThisSession.clear()
  }

  fun updateEntitlement(record: ServerEntitlementRecord) {
    val normalizedUid = record.uid.ifBlank { record.userId }
    val normalizedExpiry = if (record.expiryEpochMs > 0L) record.expiryEpochMs else record.expiryTimeMillis
    val normalizedPlan = if (record.planId != "free") record.planId else record.productId
    _entitlementState.value = record.copy(
      uid = normalizedUid,
      userId = normalizedUid,
      planId = normalizedPlan,
      productId = normalizedPlan,
      expiryEpochMs = normalizedExpiry,
      expiryTimeMillis = normalizedExpiry
    )
    _isEntitlementLoading.value = false
    recalculateTutorUsageLimits()
  }

  /**
   * Immediately clears all cached entitlement and usage state on sign-out or UID change.
   * Ensures logged-out or switched accounts NEVER see stale Pro status.
   */
  fun clearEntitlementAndUsageOnAuthChange(newUid: String = "") {
    entitlementListener?.remove()
    entitlementListener = null
    dailyUsageListener?.remove()
    dailyUsageListener = null
    monthlyUsageListener?.remove()
    monthlyUsageListener = null
    _openedBookIdsThisMonth.value = emptySet()
    _entitlementState.value = ServerEntitlementRecord(
      uid = newUid,
      userId = newUid,
      status = EntitlementStatus.FREE,
      timezone = storedUserTimezoneId
    )
    _isEntitlementLoading.value = false
    _tutorUsageState.value = AiTutorUsageState(
      uid = newUid,
      isPro = false,
      usedDailySeconds = 0,
      usedMonthlySeconds = 0,
      dailyLimitSeconds = _policyState.value.aiTutorFreeSecondsPerDay,
      monthlyLimitSeconds = _policyState.value.aiTutorFreeSecondsPerDay,
      remainingTodaySeconds = _policyState.value.aiTutorFreeSecondsPerDay,
      remainingMonthSeconds = _policyState.value.aiTutorFreeSecondsPerDay,
      effectiveRemainingSeconds = _policyState.value.aiTutorFreeSecondsPerDay,
      maxSingleSessionSeconds = _policyState.value.aiTutorMaxSessionSeconds,
      dailyLimitReached = _policyState.value.aiTutorFreeSecondsPerDay <= 0,
      nextDailyResetEpochMs = computeNextMidnightEpochMs(),
      nextMonthlyResetDateLabel = computeNextMonthResetDateLabel()
    )
  }

  fun currentDayKeyInTimezone(
    timezoneId: String = storedUserTimezoneId,
    nowEpochMs: Long = System.currentTimeMillis()
  ): String = currentDailyKey(nowEpochMs, timezoneId)

  fun currentDailyKeyInTimezone(
    timezoneId: String = storedUserTimezoneId,
    nowEpochMs: Long = System.currentTimeMillis()
  ): String = currentDailyKey(nowEpochMs, timezoneId)

  /**
   * Computes `yyyy-MM` in the user's configured IANA timezone (DST-safe).
   */
  fun currentMonthlyKeyInTimezone(
    timezoneId: String = storedUserTimezoneId,
    nowEpochMs: Long = System.currentTimeMillis()
  ): String {
    val tz = TimeZone.getTimeZone(timezoneId)
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    sdf.timeZone = tz
    return sdf.format(Date(nowEpochMs))
  }

  /**
   * Computes exact epoch milliseconds for the next local midnight in [timezoneId] (DST-safe via Calendar).
   */
  fun computeNextMidnightEpochMs(
    nowEpochMs: Long = System.currentTimeMillis(),
    timezoneId: String = storedUserTimezoneId
  ): Long {
    val tz = TimeZone.getTimeZone(timezoneId)
    val cal = java.util.Calendar.getInstance(tz, Locale.US).apply {
      timeInMillis = nowEpochMs
      add(java.util.Calendar.DAY_OF_YEAR, 1)
      set(java.util.Calendar.HOUR_OF_DAY, 0)
      set(java.util.Calendar.MINUTE, 0)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  /**
   * Computes human-readable countdown to the user's next local midnight (e.g. "Resets in 5h 12m").
   */
  fun formatCountdownToMidnight(
    nowEpochMs: Long = System.currentTimeMillis(),
    timezoneId: String = storedUserTimezoneId
  ): String {
    val nextMidnight = computeNextMidnightEpochMs(nowEpochMs, timezoneId)
    val diffMs = (nextMidnight - nowEpochMs).coerceAtLeast(60_000L)
    val totalMinutes = (diffMs / 60_000L).toInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) {
      "Resets in ${hours}h ${mins}m"
    } else {
      "Resets in ${mins}m"
    }
  }

  /**
   * Computes the formatted date label for the 1st of next month at midnight in [timezoneId].
   */
  fun computeNextMonthResetDateLabel(
    nowEpochMs: Long = System.currentTimeMillis(),
    timezoneId: String = storedUserTimezoneId
  ): String {
    val tz = TimeZone.getTimeZone(timezoneId)
    val cal = java.util.Calendar.getInstance(tz, Locale.US).apply {
      timeInMillis = nowEpochMs
      add(java.util.Calendar.MONTH, 1)
      set(java.util.Calendar.DAY_OF_MONTH, 1)
      set(java.util.Calendar.HOUR_OF_DAY, 0)
      set(java.util.Calendar.MINUTE, 0)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
    sdf.timeZone = tz
    return sdf.format(cal.time)
  }

  private fun recalculateTutorUsageLimits(
    usedDailySec: Int = _tutorUsageState.value.usedDailySeconds,
    usedMonthlySec: Int = _tutorUsageState.value.usedMonthlySeconds
  ) {
    val policy = _policyState.value
    val ent = _entitlementState.value
    val isPro = ent.hasProEntitlement
    val dailyLimit = if (isPro) policy.aiTutorProSecondsPerDay else policy.aiTutorFreeSecondsPerDay
    val monthlyLimit = if (isPro) policy.aiTutorProSecondsPerMonth else policy.aiTutorFreeSecondsPerDay
    val remToday = (dailyLimit - usedDailySec).coerceAtLeast(0)
    val remMonth = if (isPro) (monthlyLimit - usedMonthlySec).coerceAtLeast(0) else remToday
    val effectiveRem = if (isPro) minOf(remToday, remMonth) else remToday
    val dailyReached = remToday <= 0
    val monthlyReached = isPro && remMonth <= 0
    val soft80Monthly = isPro && monthlyLimit > 0 && (usedMonthlySec.toFloat() / monthlyLimit.toFloat()) >= 0.80f

    _tutorUsageState.value = AiTutorUsageState(
      uid = ent.effectiveUid,
      isPro = isPro,
      usedDailySeconds = usedDailySec.coerceAtLeast(0),
      usedMonthlySeconds = usedMonthlySec.coerceAtLeast(0),
      dailyLimitSeconds = dailyLimit,
      monthlyLimitSeconds = monthlyLimit,
      remainingTodaySeconds = remToday,
      remainingMonthSeconds = remMonth,
      effectiveRemainingSeconds = effectiveRem,
      maxSingleSessionSeconds = policy.aiTutorMaxSessionSeconds,
      dailyLimitReached = dailyReached,
      monthlyLimitReached = monthlyReached,
      softWarning80PercentMonthly = soft80Monthly,
      nextDailyResetEpochMs = computeNextMidnightEpochMs(),
      nextMonthlyResetDateLabel = computeNextMonthResetDateLabel()
    )
  }

  fun recordConnectedTutorSecondsForLocalDisplay(connectedDeltaSeconds: Int) {
    if (connectedDeltaSeconds <= 0) return
    val current = _tutorUsageState.value
    recalculateTutorUsageLimits(
      usedDailySec = current.usedDailySeconds + connectedDeltaSeconds,
      usedMonthlySec = current.usedMonthlySeconds + connectedDeltaSeconds
    )
  }

  fun setTutorUsageForTesting(usedDailySeconds: Int, usedMonthlySeconds: Int = usedDailySeconds) {
    recalculateTutorUsageLimits(usedDailySeconds, usedMonthlySeconds)
  }

  /**
   * Synchronizes `config/accessPolicy` and `config/freeContent` from Firestore,
   * and attaches a real-time listener to `users/{uid}/private/entitlement`.
   */
  suspend fun syncPolicyAndEntitlementFromFirestore(db: FirebaseFirestore?) {
    if (db == null) return
    lastFirestoreInstance = db
    val currentUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull().orEmpty()
    if (currentUid.isBlank()) {
      clearEntitlementAndUsageOnAuthChange()
      return
    }
    if (_entitlementState.value.uid != currentUid) {
      _isEntitlementLoading.value = true
      _entitlementState.value = ServerEntitlementRecord(uid = currentUid, status = EntitlementStatus.FREE)
    }

    runCatching {
      val policyDoc = db.collection("config").document("accessPolicy").get().await()
      if (policyDoc.exists()) {
        val current = _policyState.value
        val freeTutorSec = (
          policyDoc.getLong("ai.tutor.freeSecondsPerDay")?.toInt()
            ?: policyDoc.getLong("aiTutorFreeSecondsPerDay")?.toInt()
            ?: current.aiTutorFreeSecondsPerDay
          ).coerceAtLeast(0)
        val proTutorDaySec = (
          policyDoc.getLong("ai.tutor.proSecondsPerDay")?.toInt()
            ?: policyDoc.getLong("aiTutorProSecondsPerDay")?.toInt()
            ?: current.aiTutorProSecondsPerDay
          ).coerceAtLeast(60)
        val proTutorMonthSec = (
          policyDoc.getLong("ai.tutor.proSecondsPerMonth")?.toInt()
            ?: policyDoc.getLong("aiTutorProSecondsPerMonth")?.toInt()
            ?: current.aiTutorProSecondsPerMonth
          ).coerceAtLeast(proTutorDaySec)
        val maxSessionSec = (
          policyDoc.getLong("ai.tutor.maxSessionSeconds")?.toInt()
            ?: policyDoc.getLong("aiTutorMaxSessionSeconds")?.toInt()
            ?: current.aiTutorMaxSessionSeconds
          ).coerceAtLeast(60)
        val idleTimeoutSec = (
          policyDoc.getLong("ai.tutor.idleTimeoutSeconds")?.toInt()
            ?: policyDoc.getLong("aiTutorIdleTimeoutSeconds")?.toInt()
            ?: current.aiTutorIdleTimeoutSeconds
          ).coerceAtLeast(15)
        val freeBooksMonth = (
          policyDoc.getLong("freeBooksPerMonth")?.toInt()
            ?: current.freeBooksPerMonth
          ).coerceAtLeast(0)

        _policyState.value = current.copy(
          lessonsProOnly = policyDoc.getBoolean("lessonsProOnly") ?: true,
          roleplayProOnly = policyDoc.getBoolean("roleplayProOnly") ?: true,
          allAiProOnly = policyDoc.getBoolean("allAiProOnly") ?: true,
          freeDailyLessons = (policyDoc.getLong("freeDailyLessons")?.toInt() ?: current.freeDailyLessons).coerceAtLeast(0),
          freeDailyAiVoiceSeconds = freeTutorSec,
          freeDailyAiTextMessages = (policyDoc.getLong("freeDailyAiTextMessages")?.toInt() ?: current.freeDailyAiTextMessages).coerceAtLeast(0),
          freeDailyAiRoleplaySessions = (policyDoc.getLong("freeDailyAiRoleplaySessions")?.toInt() ?: current.freeDailyAiRoleplaySessions).coerceAtLeast(0),
          freeDailyAiCorrections = (policyDoc.getLong("freeDailyAiCorrections")?.toInt() ?: current.freeDailyAiCorrections).coerceAtLeast(0),
          freeBooksPerMonth = freeBooksMonth,
          freeAudiobooksCount = freeBooksMonth,
          freeReadingBooksCount = freeBooksMonth,
          freeDailyListeningExercises = (policyDoc.getLong("freeDailyListeningExercises")?.toInt() ?: current.freeDailyListeningExercises).coerceAtLeast(0),
          freeMaxGameLevel = (policyDoc.getLong("freeMaxGameLevel")?.toInt() ?: current.freeMaxGameLevel).coerceAtLeast(1),
          freeLifetimePracticalLabsTrials = (policyDoc.getLong("freeLifetimePracticalLabsTrials")?.toInt() ?: current.freeLifetimePracticalLabsTrials).coerceAtLeast(0),
          freeDailyStudioSessions = (policyDoc.getLong("freeDailyStudioSessions")?.toInt() ?: current.freeDailyStudioSessions).coerceAtLeast(0),
          freeDailyHumanAudioCallMinutes = (policyDoc.getLong("freeDailyHumanAudioCallMinutes")?.toInt() ?: current.freeDailyHumanAudioCallMinutes).coerceAtLeast(5),
          freeDailyHumanVideoCallMinutes = (policyDoc.getLong("freeDailyHumanVideoCallMinutes")?.toInt() ?: current.freeDailyHumanVideoCallMinutes).coerceAtLeast(5),
          proDailyAiVoiceSeconds = proTutorDaySec,
          proMonthlyAiVoiceSeconds = proTutorMonthSec,
          proDailyAiTextMessages = (policyDoc.getLong("proDailyAiTextMessages")?.toInt() ?: current.proDailyAiTextMessages).coerceAtLeast(20),
          proDailyAiRoleplaySessions = (policyDoc.getLong("proDailyAiRoleplaySessions")?.toInt() ?: current.proDailyAiRoleplaySessions).coerceAtLeast(2),
          proDailyAiCorrections = (policyDoc.getLong("proDailyAiCorrections")?.toInt() ?: current.proDailyAiCorrections).coerceAtLeast(10),
          proDailyPersonalizedMaterials = (policyDoc.getLong("proDailyPersonalizedMaterials")?.toInt() ?: current.proDailyPersonalizedMaterials).coerceAtLeast(1),
          proDailyHumanAudioCallMinutes = (policyDoc.getLong("proDailyHumanAudioCallMinutes")?.toInt() ?: current.proDailyHumanAudioCallMinutes).coerceAtLeast(30),
          proDailyHumanVideoCallMinutes = (policyDoc.getLong("proDailyHumanVideoCallMinutes")?.toInt() ?: current.proDailyHumanVideoCallMinutes).coerceAtLeast(15),
          aiTutorFreeSecondsPerDay = freeTutorSec,
          aiTutorProSecondsPerDay = proTutorDaySec,
          aiTutorProSecondsPerMonth = proTutorMonthSec,
          aiTutorMaxSessionSeconds = maxSessionSec,
          aiTutorIdleTimeoutSeconds = idleTimeoutSec
        )
        recalculateTutorUsageLimits()
      }
    }

    runCatching {
      val freeDoc = db.collection("config").document("freeContent").get().await()
      if (freeDoc.exists()) {
        val audioList = (freeDoc.get("freeAudiobookIds") as? List<*>)
          ?.mapNotNull { it?.toString()?.trim() }
          ?.filter { it.isNotBlank() }
        val readingList = (freeDoc.get("freeReadingBookIds") as? List<*>)
          ?.mapNotNull { it?.toString()?.trim() }
          ?.filter { it.isNotBlank() }
        _freeContentState.update { prev ->
          prev.copy(
            freeAudiobookIds = if (!audioList.isNullOrEmpty()) audioList else prev.freeAudiobookIds,
            freeReadingBookIds = if (!readingList.isNullOrEmpty()) readingList else prev.freeReadingBookIds
          )
        }
      }
    }

    observeUserEntitlement(db)
  }

  /**
   * Attaches a real-time Firestore snapshot listener to `users/{uid}/private/entitlement`
   * and `usage/{uid}/daily/{yyyy-mm-dd}` + `usage/{uid}/monthly/{yyyy-mm}`.
   */
  fun observeUserEntitlement(db: FirebaseFirestore?) {
    entitlementListener?.remove()
    entitlementListener = null
    dailyUsageListener?.remove()
    dailyUsageListener = null
    monthlyUsageListener?.remove()
    monthlyUsageListener = null

    if (db == null) {
      _isEntitlementLoading.value = false
      return
    }
    lastFirestoreInstance = db
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val uid = currentUser?.uid.orEmpty()
    if (uid.isBlank() || currentUser?.isAnonymous == true) {
      clearEntitlementAndUsageOnAuthChange()
      return
    }

    _isEntitlementLoading.value = true
    entitlementListener = db.collection("users")
      .document(uid)
      .collection("private")
      .document("entitlement")
      .addSnapshotListener { snap, err ->
        _isEntitlementLoading.value = false
        if (err != null || snap == null || !snap.exists()) {
          _entitlementState.value = ServerEntitlementRecord(uid = uid, status = EntitlementStatus.FREE)
          recalculateTutorUsageLimits()
          return@addSnapshotListener
        }
        val statusRaw = snap.getString("status") ?: snap.getString("entitlementState") ?: "free"
        val status = EntitlementStatus.fromCode(statusRaw)
        val verifiedPlay = snap.getBoolean("verifiedWithPlayApi") ?: false
        val effectiveStatus = if (verifiedPlay) status else EntitlementStatus.FREE
        val tier = if (effectiveStatus.grantsProAccess) {
          snap.getString("subscriptionTier") ?: "premium_pro"
        } else {
          "free"
        }
        val record = ServerEntitlementRecord(
          uid = uid,
          status = effectiveStatus,
          planId = if (effectiveStatus.grantsProAccess) (snap.getString("planId") ?: "lingosphere_pro") else "free",
          basePlanId = if (effectiveStatus.grantsProAccess) snap.getString("basePlanId") else null,
          subscriptionTier = tier,
          expiryEpochMs = snap.getLong("expiryEpochMs") ?: snap.getLong("subscriptionExpiryEpochMs") ?: 0L,
          autoRenewing = snap.getBoolean("autoRenewing") ?: snap.getBoolean("subscriptionAutoRenewing") ?: false,
          inGracePeriod = effectiveStatus == EntitlementStatus.PRO_GRACE || (snap.getBoolean("inGracePeriod") == true),
          onHold = effectiveStatus == EntitlementStatus.PRO_ON_HOLD || (snap.getBoolean("onHold") == true),
          verifiedWithPlayApi = verifiedPlay,
          purchaseTokenHash = snap.getString("purchaseTokenHash"),
          lastVerifiedEpochMs = snap.getLong("lastVerifiedEpochMs") ?: System.currentTimeMillis()
        )
        _entitlementState.value = record
        recalculateTutorUsageLimits()
      }

    val dayKey = currentDailyKey()
    val monthKey = currentMonthlyKeyInTimezone()
    dailyUsageListener = db.collection("usage")
      .document(uid)
      .collection("daily")
      .document(dayKey)
      .addSnapshotListener { snap, _ ->
        val tutorSec = (snap?.getLong("tutorSeconds")?.toInt()
          ?: snap?.getLong("ai_voice_seconds")?.toInt()
          ?: 0).coerceAtLeast(0)
        recalculateTutorUsageLimits(
          usedDailySec = tutorSec,
          usedMonthlySec = _tutorUsageState.value.usedMonthlySeconds
        )
      }

    monthlyUsageListener = db.collection("usage")
      .document(uid)
      .collection("monthly")
      .document(monthKey)
      .addSnapshotListener { snap, _ ->
        val tutorMonthSec = (snap?.getLong("tutorSeconds")?.toInt()
          ?: snap?.getLong("ai_voice_seconds")?.toInt()
          ?: 0).coerceAtLeast(0)
        val openedBooks = (snap?.get("openedBookIds") as? List<*>)
          ?.mapNotNull { it?.toString()?.trim()?.lowercase(Locale.US) }
          ?.filter { it.isNotBlank() }
          ?.toSet()
          ?: emptySet()
        _openedBookIdsThisMonth.value = openedBooks
        recalculateTutorUsageLimits(
          usedDailySec = _tutorUsageState.value.usedDailySeconds,
          usedMonthlySec = tutorMonthSec
        )
      }
  }

  fun updateEntitlementForTesting(record: ServerEntitlementRecord) {
    _entitlementState.value = record
    _isEntitlementLoading.value = false
    recalculateTutorUsageLimits()
  }
}
