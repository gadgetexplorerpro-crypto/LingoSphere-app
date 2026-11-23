package com.example.data.repository

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.example.data.model.ReferralRewardConfig
import com.example.data.model.ReferralShareChannel
import com.example.data.model.ReferralSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Lightweight local store preserving deep-linked or Play Install Referrer codes
 * across Onboarding -> Google Sign-In -> First Profile Creation, plus a TTL cache
 * for referral summaries so Profile/Invite Friends never spams Firestore reads.
 */
object ReferralInstallAttributionStore {
  private const val PREFS_NAME = "lingosphere_referral_prefs"
  private const val KEY_PENDING_REF_CODE = "pending_referral_code"
  private const val KEY_PENDING_REF_SOURCE = "pending_referral_source"
  private const val KEY_INSTALL_REFERRER_CHECKED = "install_referrer_checked"
  private const val KEY_LOCAL_PENDING_COUNT_PREFIX = "local_pending_invites_"
  private const val KEY_SUMMARY_CACHE_PREFIX = "summary_cache_"
  private const val SUMMARY_CACHE_TTL_MS = 5 * 60_000L

  private val _pendingReferralCodeFlow = MutableStateFlow<String?>(null)
  val pendingReferralCodeFlow: StateFlow<String?> = _pendingReferralCodeFlow.asStateFlow()

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun savePendingReferralCode(
    context: Context,
    rawCode: String?,
    source: String = "app_link"
  ): String? {
    val cleanCode = ReferralRewardConfig.normalizeReferralCode(rawCode) ?: return null
    prefs(context).edit()
      .putString(KEY_PENDING_REF_CODE, cleanCode)
      .putString(KEY_PENDING_REF_SOURCE, source.ifBlank { "app_link" })
      .apply()
    _pendingReferralCodeFlow.value = cleanCode
    return cleanCode
  }

  fun captureIncomingIntent(context: Context, intent: Intent?): String? {
    val dataUri = intent?.data ?: return null
    val code = ReferralRewardConfig.extractReferralCodeFromUri(dataUri) ?: return null
    val source = if (dataUri.scheme == ReferralRewardConfig.CUSTOM_SCHEME) {
      "deep_link"
    } else {
      "app_link"
    }
    return savePendingReferralCode(context, code, source)
  }

  fun getPendingReferralCode(context: Context): String? {
    val stored = prefs(context).getString(KEY_PENDING_REF_CODE, null)
    val clean = ReferralRewardConfig.normalizeReferralCode(stored)
    if (_pendingReferralCodeFlow.value != clean) {
      _pendingReferralCodeFlow.value = clean
    }
    return clean
  }

  fun getPendingReferralSource(context: Context): String {
    return prefs(context).getString(KEY_PENDING_REF_SOURCE, "app_link") ?: "app_link"
  }

  fun clearPendingReferralCode(context: Context) {
    prefs(context).edit()
      .remove(KEY_PENDING_REF_CODE)
      .remove(KEY_PENDING_REF_SOURCE)
      .apply()
    _pendingReferralCodeFlow.value = null
  }

  fun isInstallReferrerChecked(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_INSTALL_REFERRER_CHECKED, false)
  }

  fun markInstallReferrerChecked(context: Context) {
    prefs(context).edit().putBoolean(KEY_INSTALL_REFERRER_CHECKED, true).apply()
  }

  fun incrementLocalPendingInvites(context: Context, userId: String): Int {
    if (userId.isBlank()) return 0
    val key = KEY_LOCAL_PENDING_COUNT_PREFIX + userId
    val next = prefs(context).getInt(key, 0) + 1
    prefs(context).edit().putInt(key, next).apply()
    return next
  }

  fun getLocalPendingInvites(context: Context, userId: String): Int {
    if (userId.isBlank()) return 0
    return prefs(context).getInt(KEY_LOCAL_PENDING_COUNT_PREFIX + userId, 0)
  }

  fun saveSummaryCache(context: Context, summary: ReferralSummary, nowMs: Long = System.currentTimeMillis()) {
    if (summary.userId.isBlank()) return
    val json = JSONObject().apply {
      put("userId", summary.userId)
      put("referralCode", summary.referralCode)
      put("successfulReferralsCount", summary.successfulReferralsCount)
      put("pendingInvitationsCount", summary.pendingInvitationsCount)
      put("totalXpEarnedFromReferrals", summary.totalXpEarnedFromReferrals)
      put("bonusSpeakingCredits", summary.bonusSpeakingCredits)
      put("referredByCode", summary.referredByCode ?: "")
      put("referredByUid", summary.referredByUid ?: "")
      put("unlockedBadges", org.json.JSONArray(summary.unlockedBadges))
      put("cachedAtMs", nowMs)
    }
    prefs(context).edit()
      .putString(KEY_SUMMARY_CACHE_PREFIX + summary.userId, json.toString())
      .apply()
  }

  fun loadSummaryCache(
    context: Context,
    userId: String,
    nowMs: Long = System.currentTimeMillis(),
    ignoreTtl: Boolean = false
  ): ReferralSummary? {
    if (userId.isBlank()) return null
    val raw = prefs(context).getString(KEY_SUMMARY_CACHE_PREFIX + userId, null) ?: return null
    return runCatching {
      val json = JSONObject(raw)
      val cachedAt = json.optLong("cachedAtMs", 0L)
      if (!ignoreTtl && (nowMs - cachedAt) > SUMMARY_CACHE_TTL_MS) {
        return@runCatching null
      }
      val badgesArray = json.optJSONArray("unlockedBadges")
      val badges = buildList {
        if (badgesArray != null) {
          for (i in 0 until badgesArray.length()) {
            val item = badgesArray.optString(i).trim()
            if (item.isNotBlank()) add(item)
          }
        }
      }
      ReferralSummary(
        userId = json.optString("userId", userId),
        referralCode = json.optString("referralCode", ""),
        successfulReferralsCount = json.optLong("successfulReferralsCount", 0L),
        pendingInvitationsCount = json.optLong("pendingInvitationsCount", 0L),
        totalXpEarnedFromReferrals = json.optLong("totalXpEarnedFromReferrals", 0L),
        unlockedBadges = badges,
        bonusSpeakingCredits = json.optLong("bonusSpeakingCredits", 0L),
        referredByCode = json.optString("referredByCode", "").takeIf { it.isNotBlank() },
        referredByUid = json.optString("referredByUid", "").takeIf { it.isNotBlank() }
      )
    }.getOrNull()
  }

  fun resetForTests(context: Context) {
    prefs(context).edit().clear().apply()
    _pendingReferralCodeFlow.value = null
  }
}

/**
 * Fetches Google Play Install Referrer metadata once after install and extracts `ref=CODE`.
 */
object InstallReferrerAttributionBridge {
  suspend fun fetchAndStoreInstallReferrerIfNeeded(context: Context): String? {
    val existing = ReferralInstallAttributionStore.getPendingReferralCode(context)
    if (!existing.isNullOrBlank()) return existing
    if (ReferralInstallAttributionStore.isInstallReferrerChecked(context)) return null

    val referrerStr = queryPlayInstallReferrer(context)
    ReferralInstallAttributionStore.markInstallReferrerChecked(context)
    val parsedCode = ReferralRewardConfig.extractReferralCodeFromPlayReferrer(referrerStr)
    if (!parsedCode.isNullOrBlank()) {
      return ReferralInstallAttributionStore.savePendingReferralCode(
        context = context,
        rawCode = parsedCode,
        source = "install_referrer"
      )
    }
    return null
  }

  internal suspend fun queryPlayInstallReferrer(context: Context): String? =
    suspendCancellableCoroutine { cont ->
      try {
        val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
        var resumed = false
        fun finishWith(value: String?) {
          if (!resumed && cont.isActive) {
            resumed = true
            runCatching { client.endConnection() }
            cont.resume(value)
          }
        }
        client.startConnection(object : InstallReferrerStateListener {
          override fun onInstallReferrerSetupFinished(responseCode: Int) {
            if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
              val referrer = runCatching { client.installReferrer?.installReferrer }.getOrNull()
              finishWith(referrer)
            } else {
              finishWith(null)
            }
          }

          override fun onInstallReferrerServiceDisconnected() {
            finishWith(null)
          }
        })
        cont.invokeOnCancellation {
          runCatching { client.endConnection() }
        }
      } catch (e: Throwable) {
        Log.d("InstallReferrer", "Play Install Referrer unavailable: ${e.message}")
        if (cont.isActive) cont.resume(null)
      }
    }
}

/**
 * Pure Intent builder and launcher for copying and sharing referral invitations
 * via WhatsApp, Facebook, Telegram, Messenger, SMS, Email, and the Android Share Sheet.
 */
object ReferralShareHelper {

  fun copyTextToClipboard(context: Context, label: String, value: String): Boolean {
    return try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return false
      clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
      true
    } catch (_: Exception) {
      false
    }
  }

  fun copyInviteLink(context: Context, referralCode: String): String {
    val link = ReferralRewardConfig.buildInviteLink(referralCode)
    copyTextToClipboard(context, "LingoSphere Invite Link", link)
    return link
  }

  fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
      context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
      true
    } catch (_: Throwable) {
      @Suppress("DEPRECATION")
      try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
      } catch (_: Throwable) {
        false
      }
    }
  }

  fun buildNativeChooserIntent(referralCode: String, addNewTaskFlag: Boolean = false): Intent {
    val shareText = ReferralRewardConfig.buildShareText(referralCode)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Join me on LingoSphere 🌍🗣️")
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
    return Intent.createChooser(sendIntent, "Invite Friends to LingoSphere").apply {
      if (addNewTaskFlag) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  fun buildShareIntent(
    context: Context,
    channel: ReferralShareChannel,
    referralCode: String,
    packageInstalledCheck: (String) -> Boolean = { pkg -> isPackageInstalled(context, pkg) }
  ): Intent {
    val inviteLink = ReferralRewardConfig.buildInviteLink(referralCode)
    val shareText = ReferralRewardConfig.buildShareText(referralCode)
    val needsNewTask = context !is Activity

    fun applyFlags(intent: Intent): Intent = intent.apply {
      if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return when (channel) {
      ReferralShareChannel.WHATSAPP -> {
        val pkg = channel.packageName!!
        if (packageInstalledCheck(pkg)) {
          applyFlags(
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              setPackage(pkg)
              putExtra(Intent.EXTRA_TEXT, shareText)
            }
          )
        } else {
          applyFlags(
            Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://wa.me/?text=${Uri.encode(shareText)}")
            )
          )
        }
      }

      ReferralShareChannel.FACEBOOK -> {
        val pkg = channel.packageName!!
        if (packageInstalledCheck(pkg)) {
          applyFlags(
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              setPackage(pkg)
              putExtra(Intent.EXTRA_TEXT, shareText)
            }
          )
        } else {
          val fbUrl = "https://www.facebook.com/sharer/sharer.php?u=${Uri.encode(inviteLink)}&quote=${Uri.encode(ReferralRewardConfig.SHARE_GREETING_MESSAGE)}"
          applyFlags(Intent(Intent.ACTION_VIEW, Uri.parse(fbUrl)))
        }
      }

      ReferralShareChannel.TELEGRAM -> {
        val pkg = channel.packageName!!
        if (packageInstalledCheck(pkg)) {
          applyFlags(
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              setPackage(pkg)
              putExtra(Intent.EXTRA_TEXT, shareText)
            }
          )
        } else {
          val tgUrl = "https://t.me/share/url?url=${Uri.encode(inviteLink)}&text=${Uri.encode(ReferralRewardConfig.SHARE_GREETING_MESSAGE)}"
          applyFlags(Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl)))
        }
      }

      ReferralShareChannel.MESSENGER -> {
        val pkg = channel.packageName!!
        if (packageInstalledCheck(pkg)) {
          applyFlags(
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              setPackage(pkg)
              putExtra(Intent.EXTRA_TEXT, shareText)
            }
          )
        } else {
          buildNativeChooserIntent(referralCode, addNewTaskFlag = needsNewTask)
        }
      }

      ReferralShareChannel.SMS -> {
        applyFlags(
          Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
            putExtra("sms_body", shareText)
            putExtra(Intent.EXTRA_TEXT, shareText)
          }
        )
      }

      ReferralShareChannel.EMAIL -> {
        applyFlags(
          Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Learn languages with me on LingoSphere 🌍🗣️")
            putExtra(Intent.EXTRA_TEXT, shareText)
          }
        )
      }

      ReferralShareChannel.NATIVE_SHARE -> {
        buildNativeChooserIntent(referralCode, addNewTaskFlag = needsNewTask)
      }
    }
  }

  /**
   * Launches the targeted share intent, gracefully falling back to the native Android
   * Share Sheet if the specific client/handler is unavailable.
   */
  fun launchShare(
    context: Context,
    channel: ReferralShareChannel,
    referralCode: String
  ): Boolean {
    val primaryIntent = buildShareIntent(context, channel, referralCode)
    return try {
      context.startActivity(primaryIntent)
      true
    } catch (_: ActivityNotFoundException) {
      try {
        context.startActivity(
          buildNativeChooserIntent(referralCode, addNewTaskFlag = context !is Activity)
        )
        true
      } catch (_: Exception) {
        false
      }
    } catch (_: Exception) {
      try {
        context.startActivity(
          buildNativeChooserIntent(referralCode, addNewTaskFlag = context !is Activity)
        )
        true
      } catch (_: Exception) {
        false
      }
    }
  }
}
