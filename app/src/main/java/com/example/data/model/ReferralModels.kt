package com.example.data.model

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.util.Locale

object ReferralRewardConfig {
  const val APP_DOMAIN_URL = "https://ais-pre-smnebhnagujwa3llo3lj4b-861509315491.asia-east1.run.app"
  const val INVITE_PATH = "/invite"
  const val CUSTOM_SCHEME = "lingosphere"
  const val CUSTOM_HOST = "invite"
  const val ANDROID_PACKAGE_NAME = "com.aistudio.lingosphere.kxpqzm"

  const val INVITER_XP_PER_REFERRAL = 100L
  const val REFERRED_WELCOME_XP = 100L

  const val MILESTONE_3_THRESHOLD = 3
  const val MILESTONE_3_BADGE_ID = "community_builder_3"
  const val MILESTONE_3_BADGE_TITLE = "Language Ambassador"

  const val MILESTONE_5_THRESHOLD = 5
  const val MILESTONE_5_BONUS_XP = 250L
  const val MILESTONE_5_BADGE_ID = "lingo_mentor_5"
  const val MILESTONE_5_BADGE_TITLE = "Lingo Mentor"
  const val MILESTONE_5_SPEAKING_CREDITS = 3

  const val NEW_ACCOUNT_WINDOW_MS = 24L * 60L * 60L * 1000L
  const val SHARE_GREETING_MESSAGE =
    "Hey! I'm learning languages with LingoSphere. Join me and let's practice speaking together! 🌍🗣️"

  val CODE_REGEX = Regex("^[A-Z0-9]{6,16}$")

  fun isValidReferralCode(raw: String?): Boolean {
    val normalized = raw?.trim()?.uppercase(Locale.US).orEmpty()
    return CODE_REGEX.matches(normalized)
  }

  fun normalizeReferralCode(raw: String?): String? {
    val cleaned = raw?.trim()?.uppercase(Locale.US)
      ?.replace(Regex("[^A-Z0-9]"), "")
      .orEmpty()
    return if (CODE_REGEX.matches(cleaned)) cleaned else null
  }

  fun buildInviteLink(referralCode: String): String {
    val cleanCode = normalizeReferralCode(referralCode) ?: referralCode.trim().uppercase(Locale.US)
    return "$APP_DOMAIN_URL$INVITE_PATH?ref=$cleanCode"
  }

  fun buildPlayStoreReferralUrl(referralCode: String): String {
    val cleanCode = normalizeReferralCode(referralCode) ?: referralCode.trim().uppercase(Locale.US)
    val encodedReferrer = Uri.encode("ref=$cleanCode")
    return "https://play.google.com/store/apps/details?id=$ANDROID_PACKAGE_NAME&referrer=$encodedReferrer"
  }

  fun buildShareText(referralCode: String): String {
    val link = buildInviteLink(referralCode)
    return "$SHARE_GREETING_MESSAGE\n$link"
  }

  fun extractReferralCodeFromUri(uri: Uri?): String? {
    if (uri == null) return null
    val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
    val host = uri.host?.lowercase(Locale.US).orEmpty()
    val path = uri.path.orEmpty()
    val matchesDomain = scheme == "https" &&
      host == "ais-pre-smnebhnagujwa3llo3lj4b-861509315491.asia-east1.run.app" &&
      path.startsWith("/invite")
    val matchesCustom = scheme == CUSTOM_SCHEME && (host == CUSTOM_HOST || path.startsWith("/invite"))
    if (!matchesDomain && !matchesCustom && uri.getQueryParameter("ref").isNullOrBlank()) {
      return null
    }
    val rawRef = uri.getQueryParameter("ref")
      ?: uri.getQueryParameter("code")
      ?: uri.getQueryParameter("utm_content")
    return normalizeReferralCode(rawRef)
  }

  fun extractReferralCodeFromUriString(rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    return runCatching { extractReferralCodeFromUri(Uri.parse(rawUrl.trim())) }.getOrNull()
  }

  fun extractReferralCodeFromPlayReferrer(referrerString: String?): String? {
    if (referrerString.isNullOrBlank()) return null
    val raw = referrerString.trim()
    // Direct bare code fallback
    normalizeReferralCode(raw)?.let { return it }
    // Parse key=value&key2=value2
    val pairs = raw.split("&")
    for (pair in pairs) {
      val idx = pair.indexOf('=')
      if (idx > 0) {
        val key = pair.substring(0, idx).trim().lowercase(Locale.US)
        val value = Uri.decode(pair.substring(idx + 1).trim())
        if (key == "ref" || key == "referral_code" || key == "utm_content") {
          normalizeReferralCode(value)?.let { return it }
        }
      }
    }
    return null
  }

  fun generateDeterministicCode(userId: String, displayName: String, attempt: Int = 0): String {
    val cleanPrefix = displayName.trim()
      .uppercase(Locale.US)
      .replace(Regex("[^A-Z0-9]"), "")
      .take(4)
      .ifBlank { "LING" }
    val digest = MessageDigest.getInstance("SHA-256")
      .digest("$userId|$attempt|lingosphere_ref_v1".toByteArray(Charsets.UTF_8))
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val suffix = buildString {
      for (i in 0 until 4) {
        val idx = (digest[i].toInt() and 0xFF) % alphabet.length
        append(alphabet[idx])
      }
    }
    return (cleanPrefix + suffix).take(8).padEnd(6, 'X')
  }

  fun badgeLabelForId(badgeId: String): String = when (badgeId) {
    MILESTONE_3_BADGE_ID -> "🏆 $MILESTONE_3_BADGE_TITLE (3+ Referrals)"
    MILESTONE_5_BADGE_ID -> "🌟 $MILESTONE_5_BADGE_TITLE (5+ Referrals)"
    else -> badgeId
  }
}

enum class ReferralShareChannel(
  val id: String,
  val displayName: String,
  val packageName: String? = null
) {
  WHATSAPP("whatsapp", "WhatsApp", "com.whatsapp"),
  FACEBOOK("facebook", "Facebook", "com.facebook.katana"),
  TELEGRAM("telegram", "Telegram", "org.telegram.messenger"),
  MESSENGER("messenger", "Messenger", "com.facebook.orca"),
  SMS("sms", "SMS", null),
  EMAIL("email", "Email", null),
  NATIVE_SHARE("native_share", "More Apps", null)
}

@IgnoreExtraProperties
data class ReferralCodeRecord(
  val code: String = "",
  val ownerUid: String = "",
  val ownerDisplayName: String = "",
  val ownerTargetLanguage: String = "es",
  val ownerNativeLanguage: String = "en",
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReferralSummary(
  val userId: String = "",
  val referralCode: String = "",
  val successfulReferralsCount: Long = 0L,
  val pendingInvitationsCount: Long = 0L,
  val totalXpEarnedFromReferrals: Long = 0L,
  val unlockedBadges: List<String> = emptyList(),
  val bonusSpeakingCredits: Long = 0L,
  val referredByCode: String? = null,
  val referredByUid: String? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val hasClaimedReferral: Boolean
    get() = !referredByUid.isNullOrBlank() || !referredByCode.isNullOrBlank()
}

@IgnoreExtraProperties
data class ReferralRecord(
  val referralId: String = "",
  val inviterUid: String = "",
  val inviterName: String = "",
  val referredUid: String = "",
  val referredName: String = "",
  val referralCode: String = "",
  val status: String = "completed", // pending, completed, rejected
  val inviterXpAwarded: Long = ReferralRewardConfig.INVITER_XP_PER_REFERRAL,
  val referredXpAwarded: Long = ReferralRewardConfig.REFERRED_WELCOME_XP,
  val milestone3BadgeAwarded: Boolean = false,
  val milestone5BonusAwarded: Boolean = false,
  val attributionSource: String = "app_link", // app_link, install_referrer, manual_code
  val targetLanguageCode: String = "es",
  val nativeLanguageCode: String = "en",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val completedAt: Timestamp? = null
)

data class InvitedFriendItem(
  val friendUserId: String,
  val displayName: String,
  val avatarUrl: String = "",
  val nativeLanguageCode: String = "en",
  val targetLanguageCode: String = "es",
  val proficiencyLevel: String = "Beginner",
  val isOnline: Boolean = false,
  val allowCalls: Boolean = true,
  val xpAwarded: Long = ReferralRewardConfig.INVITER_XP_PER_REFERRAL,
  val status: String = "completed",
  val joinedAtEpochMs: Long = 0L,
  val communityProfile: CommunityProfile? = null
)

data class ReferralDashboardState(
  val isLoading: Boolean = false,
  val isClaimingCode: Boolean = false,
  val referralCode: String = "",
  val inviteLink: String = "",
  val shareMessage: String = "",
  val successfulReferralsCount: Int = 0,
  val pendingInvitationsCount: Int = 0,
  val totalXpEarned: Long = 0L,
  val bonusSpeakingCredits: Long = 0L,
  val unlockedBadges: List<String> = emptyList(),
  val invitedFriends: List<InvitedFriendItem> = emptyList(),
  val pendingReferrerCode: String? = null,
  val alreadyReferredByName: String? = null,
  val isEligibleForNewSignupReferral: Boolean = false,
  val statusMessage: String? = null,
  val errorMessage: String? = null
)

data class ReferralClaimOutcome(
  val success: Boolean,
  val message: String,
  val inviterUid: String? = null,
  val inviterName: String? = null,
  val xpAwardedToReferred: Long = 0L,
  val xpAwardedToInviter: Long = 0L,
  val unlockedMilestoneBadge: String? = null,
  val alreadyClaimed: Boolean = false
)
