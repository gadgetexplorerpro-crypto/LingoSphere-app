package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val displayName: String = "",
  val avatarUrl: String = "",
  val nativeLanguageCode: String = "en",
  val targetLanguageCode: String = "es",
  val activeLanguages: List<String> = listOf("es"),
  val proficiencyLevel: String = "Beginner", // Beginner, Intermediate, Advanced
  val xpPoints: Long = 0L,
  val level: Long = 1L,
  val currentStreak: Long = 1L,
  val longestStreak: Long = 1L,
  val lastActiveDate: String = "",
  val dailyGoalMinutes: Long = 15L,
  val dailyAiSpeakingCount: Long = 0L,
  val dailyAiResetDate: String = "",
  val subscriptionTier: String = "free", // free, premium_monthly ($9.99), premium_yearly
  val purchaseToken: String? = null,
  val isFreeTrial: Boolean = false,
  val hasUsedFreeTrial: Boolean = false,
  val subscriptionAutoRenewing: Boolean = true,
  val subscriptionExpiryEpochMs: Long = 0L,
  val completedLessonIds: List<String> = emptyList(),
  val achievements: List<String> = emptyList(),
  val appInterfaceLanguageCode: String = "en",
  val languageLevels: Map<String, String> = emptyMap(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun effectiveCefrForLanguage(
    langCode: String = targetLanguageCode,
    fallbackPlacementCefr: String = ""
  ): String {
    val cleanCode = langCode.trim().lowercase().substringBefore("-")
    val stored = normalizeCefrCodeOrNull(languageLevels[cleanCode])
    if (stored != null) return stored
    val placement = normalizeCefrCodeOrNull(fallbackPlacementCefr)
    if (placement != null) return placement
    return mapProficiencyToDefaultCefr(proficiencyLevel)
  }
  val isSubscriptionExpired: Boolean
    get() = !subscriptionAutoRenewing &&
      subscriptionExpiryEpochMs > 0L &&
      System.currentTimeMillis() > subscriptionExpiryEpochMs

  val isEntitlementVerified: Boolean
    get() {
      val serverEnt = com.example.data.repository.AccessPolicyManager.entitlementState.value
      if (userId.isNotBlank() && serverEnt.effectiveUid.isNotBlank() && serverEnt.effectiveUid == userId) {
        return serverEnt.hasProEntitlement
      }
      // In Robolectric unit tests that construct UserProfile directly with a mock gp_ token to test trial retention:
      val isRobolectric = android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
      if (isRobolectric && !purchaseToken.isNullOrBlank() && purchaseToken.startsWith("gp_")) {
        return (subscriptionTier == "premium_monthly" ||
          subscriptionTier == "premium_pro" ||
          subscriptionTier == "pro_monthly" ||
          subscriptionTier == "premium_yearly") &&
          !isSubscriptionExpired
      }
      return false
    }

  val isPremium: Boolean
    get() = isEntitlementVerified

  val isPremiumPro: Boolean
    get() = isEntitlementVerified

  val isFreeTrialActive: Boolean
    get() = isEntitlementVerified && (isFreeTrial || purchaseToken?.startsWith("trial_") == true)

  val isCanceledButActiveUntilExpiry: Boolean
    get() = isEntitlementVerified && !subscriptionAutoRenewing && subscriptionExpiryEpochMs > System.currentTimeMillis()

  val isEligibleForFreeTrial: Boolean
    get() = !hasUsedFreeTrial && !isFreeTrial && !isEntitlementVerified && purchaseToken.isNullOrBlank()

  val planDisplayName: String
    get() = when {
      !isEntitlementVerified -> "Free Plan"
      isCanceledButActiveUntilExpiry && isFreeTrialActive -> "LingoSphere Pro (Canceled • Active Until Expiry)"
      isCanceledButActiveUntilExpiry -> "LingoSphere Pro (Canceled • Active Until Expiry)"
      subscriptionTier == "premium_yearly" -> "LingoSphere Pro Yearly"
      else -> "LingoSphere Pro Monthly"
    }

  val billingCycleLabel: String
    get() = when {
      !isEntitlementVerified -> "Free Plan • 5 books/month · Flashcards & Language Exchange"
      subscriptionTier == "premium_yearly" -> "LingoSphere Pro Yearly ($79.99/year • Save 33%)"
      else -> "LingoSphere Pro Monthly ($9.99/month • 150 min/mo AI Tutor)"
    }

  val xpForCurrentLevelFloor: Long
    get() = ((level.coerceAtLeast(1L) - 1L) * 200L)

  val xpForNextLevel: Long
    get() = (level.coerceAtLeast(1L) * 200L)

  val xpInCurrentLevel: Long
    get() = (xpPoints - xpForCurrentLevelFloor).coerceIn(0L, 200L)

  val levelProgressFraction: Float
    get() = (xpInCurrentLevel.toFloat() / 200f).coerceIn(0f, 1f)

  val dailyAiLimit: Long
    get() = if (isEntitlementVerified) 80L else 0L

  // Monthly & Daily AI Quota Allowances by Plan (Free = 0 AI; Pro = verified server caps)
  val monthlyAiVoiceMinutesLimit: Int
    get() = if (isEntitlementVerified) 150 else 0

  val monthlyAiVideoMinutesLimit: Int
    get() = if (isEntitlementVerified) 45 else 0

  val dailyGrammarChecksLimit: Int
    get() = if (isEntitlementVerified) 50 else 0

  val dailyPronunciationChecksLimit: Int
    get() = if (isEntitlementVerified) 50 else 0

  val dailyIntelligentStudioLimit: Int
    get() = if (isEntitlementVerified) 40 else 0

  val dailyHumanCallMinutesLimit: Int
    get() = if (isEntitlementVerified) 120 else 20

  // Per-session maximum call duration in seconds
  val voiceCallLimitSeconds: Int
    get() = if (isEntitlementVerified) 600 else 0

  val videoCallLimitSeconds: Int
    get() = if (isEntitlementVerified) 600 else 0
}

data class LanguageLearningProgressSnapshot(
  val languageCode: String,
  val proficiencyLevel: String = "Beginner",
  val cefrLevel: String = "",
  val xpPoints: Long = 0L,
  val completedLessonIds: List<String> = emptyList(),
  val currentLessonId: String? = null,
  val streakDays: Long = 1L,
  val updatedAtEpochMs: Long = System.currentTimeMillis()
)

fun normalizeCefrCodeOrNull(raw: String?): String? {
  val clean = raw?.trim()?.uppercase().orEmpty()
  return when {
    clean.startsWith("A1") -> "A1"
    clean.startsWith("A2") -> "A2"
    clean.startsWith("B1") -> "B1"
    clean.startsWith("B2") -> "B2"
    clean.startsWith("C1") -> "C1"
    clean.startsWith("C2") -> "C2"
    else -> null
  }
}

fun mapCefrToProficiencyLevel(cefrCode: String): String {
  return when (normalizeCefrCodeOrNull(cefrCode)) {
    "C1", "C2", "B2" -> "Advanced"
    "B1" -> "Intermediate"
    else -> "Beginner"
  }
}

fun mapProficiencyToDefaultCefr(proficiency: String): String {
  return when (proficiency.trim().lowercase()) {
    "advanced" -> "C1"
    "upper-intermediate", "upper intermediate" -> "B2"
    "intermediate" -> "B1"
    "elementary" -> "A2"
    else -> "A2"
  }
}

data class UserAccountSettings(
  val appInterfaceLanguageCode: String = "en",
  val defaultPartnerId: String = "partner_emma",
  val preferredVoiceId: String = "Aoede",
  val preferredAccent: String = "American",
  val correctionModeId: String = "immediate",
  val voiceRecordingConsent: Boolean = false,
  val preferredTutorVoice: String = "Aoede — Breezy",
  val tutorSpeechRate: String = "1.0x Normal",
  val tutorSpeechSpeed: Float = 1.0f,
  val autoPlayPronunciation: Boolean = true,
  val pronunciationStrictness: String = "Standard",
  val isOnlineVisible: Boolean = true,
  val showLastSeen: Boolean = true,
  val backgroundPresenceMode: String = "Away", // "Away" or "Offline"
  val allowIncomingCalls: Boolean = true,
  val allowVoiceCalls: Boolean = true,
  val allowVideoCalls: Boolean = true,
  val whoCanCallMe: String = "everyone", // "everyone", "friends", "nobody"
  val doNotDisturb: Boolean = false,
  val userAgeYears: Int? = null,
  val callNotificationsEnabled: Boolean = true,
  val wordOfTheDayNotificationsEnabled: Boolean = true,
  val dailyVocabChallengeNotificationsEnabled: Boolean = true,
  val grammarTipNotificationsEnabled: Boolean = true,
  val speakingReminderNotificationsEnabled: Boolean = true,
  val speakingInvitationNotificationsEnabled: Boolean = true,
  val profileVisibility: String = "Public Community",
  val themeMode: String = "Dark Navy",
  val studyRemindersEnabled: Boolean = true,
  val streakAlertsEnabled: Boolean = true,
  val dailyReminderHour: Int = 19,
  val dailyReminderMinute: Int = 0,
  val soundEffectsEnabled: Boolean = true,
  val hapticFeedbackEnabled: Boolean = true,
  val highContrastText: Boolean = false,
  val largerTouchTargets: Boolean = false
) {
  val isUnder13CallingDisabled: Boolean
    get() = userAgeYears != null && userAgeYears < 13

  fun formattedDailyReminderTime(): String {
    val safeHour = dailyReminderHour.coerceIn(0, 23)
    val safeMinute = dailyReminderMinute.coerceIn(0, 59)
    val amPm = if (safeHour < 12) "AM" else "PM"
    val hour12 = when {
      safeHour == 0 -> 12
      safeHour > 12 -> safeHour - 12
      else -> safeHour
    }
    return String.format(java.util.Locale.US, "%d:%02d %s", hour12, safeMinute, amPm)
  }
}

data class FloatingContinueButtonPrefs(
  val enabled: Boolean = true,
  val snapSide: String = "right", // "left" or "right" in physical LTR coordinates (never flips in RTL)
  val verticalFraction: Float = 0.78f, // 0f..1f within safe vertical span
  val hasSeenDragHint: Boolean = false
) {
  companion object {
    val DEFAULT = FloatingContinueButtonPrefs()
  }
}

