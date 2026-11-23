package com.example.update

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.data.repository.FirestoreQuotaManager
import com.example.notifications.SmartNotificationContract
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoGold
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AppReleaseInfo(
  val versionCode: Int,
  val versionName: String,
  val title: String = "LingoSphere Update Available",
  val summary: String = "Discover new improvements, better voice conversations, and smoother learning.",
  val releaseNotes: List<String>,
  val isCritical: Boolean = false,
  val minRequiredVersionCode: Int = 1,
  val publishedDateLabel: String = "Latest Release"
)

data class AppUpdateUiState(
  val isUpdateAvailable: Boolean = false,
  val availableRelease: AppReleaseInfo? = null,
  val isFlexibleUpdateDownloaded: Boolean = false,
  val isDownloadingFlexibleUpdate: Boolean = false,
  val isCheckingForUpdates: Boolean = false,
  val lastCheckStatusMessage: String? = null,
  val showInAppAnnouncement: Boolean = false
) {
  val isFlexibleDownloadReadyToInstall: Boolean
    get() = isFlexibleUpdateDownloaded
}

/**
 * Verified changelog catalog of real LingoSphere releases and features.
 * Used for the "What's New" section in Settings/Profile and for enriching
 * Play Store / Firestore release metadata with accurate LingoSphere release notes.
 */
object RealReleaseChangelogCatalog {

  val RELEASE_HISTORY: List<AppReleaseInfo> = listOf(
    AppReleaseInfo(
      versionCode = 29,
      versionName = "29.0",
      title = "LingoSphere Update Available",
      summary = "Discover new improvements, better voice conversations, and smoother learning.",
      releaseNotes = listOf(
        "LingoSphere Pro Monthly & Yearly subscriptions with transparent renewal dates and instant purchase restoration",
        "Invite Friends & Referral Rewards system with +100 XP per referral, milestone badges, and direct friend calling",
        "Automatic Google Play In-App Updates (Flexible & Immediate) and What's New release history",
        "Quota-safe Global Partner Exchange pagination with real-time online presence and country/language filters",
        "Enhanced AI Video & Voice Tutors with 15 expressive voices and responsive 320px–412px layouts"
      ),
      isCritical = false,
      minRequiredVersionCode = 25,
      publishedDateLabel = "Current Installed Release"
    ),
    AppReleaseInfo(
      versionCode = 28,
      versionName = "28.0",
      title = "Verified Book Reader & Audio Sync",
      summary = "Chapter-verified classic literature with synchronized human and AI narration.",
      releaseNotes = listOf(
        "Strict book ID and chapter synchronization matching full book text with verified human recordings",
        "Exact-text AI Voice narration using your selected language and tutor voice without summarizing",
        "Interactive tap-to-translate vocabulary saving directly to spaced-repetition flashcards"
      ),
      isCritical = false,
      minRequiredVersionCode = 24,
      publishedDateLabel = "Previous Release"
    ),
    AppReleaseInfo(
      versionCode = 27,
      versionName = "27.0",
      title = "Multi-Transport WebRTC Calling & Live AI Assistant",
      summary = "Reliable peer-to-peer voice and video language exchange calls.",
      releaseNotes = listOf(
        "End-to-end encrypted WebRTC voice and video calling with TURN relay fallback",
        "In-call Live AI Assistant and Conversation Rescue prompts during real partner calls",
        "Emma & 12 AI Conversation Partners with live speech recognition and bilingual subtitles"
      ),
      isCritical = false,
      minRequiredVersionCode = 22,
      publishedDateLabel = "Previous Release"
    )
  )

  val currentRelease: AppReleaseInfo
    get() = getCurrentInstalledRelease()

  fun getCurrentInstalledRelease(): AppReleaseInfo {
    return RELEASE_HISTORY.firstOrNull { it.versionCode == BuildConfig.VERSION_CODE }
      ?: RELEASE_HISTORY.first().copy(
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME
      )
  }

  fun buildReleaseForVersion(
    versionCode: Int,
    versionName: String,
    customTitle: String? = null,
    customSummary: String? = null,
    customNotes: List<String>? = null,
    isCritical: Boolean = false,
    minRequiredVersionCode: Int = 1
  ): AppReleaseInfo {
    val cleanNotes = customNotes?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
    val knownEntry = RELEASE_HISTORY.firstOrNull { it.versionCode == versionCode }
    val resolvedNotes = when {
      cleanNotes.isNotEmpty() -> cleanNotes
      knownEntry != null -> knownEntry.releaseNotes
      else -> RELEASE_HISTORY.first().releaseNotes
    }
    return AppReleaseInfo(
      versionCode = versionCode,
      versionName = versionName.ifBlank { "${versionCode}.0" },
      title = customTitle?.takeIf { it.isNotBlank() } ?: "LingoSphere Update Available",
      summary = customSummary?.takeIf { it.isNotBlank() }
        ?: "Discover new improvements, better voice conversations, and smoother learning.",
      releaseNotes = resolvedNotes,
      isCritical = isCritical || BuildConfig.VERSION_CODE < minRequiredVersionCode,
      minRequiredVersionCode = minRequiredVersionCode
    )
  }
}

object LingoAppUpdateController {
  private const val TAG = "LingoAppUpdate"
  private const val PREFS_NAME = "lingosphere_app_update_prefs"
  private const val KEY_DISMISSED_VERSION_CODE = "dismissed_update_version_code"
  private const val KEY_LAST_NOTIFIED_VERSION_CODE = "last_notified_update_version_code"
  private const val KEY_LAST_INSTALLED_VERSION_CODE = "last_installed_version_code"
  private const val KEY_CACHED_REMOTE_VERSION_CODE = "cached_remote_version_code"
  private const val KEY_CACHED_REMOTE_VERSION_NAME = "cached_remote_version_name"
  private const val KEY_CACHED_REMOTE_SUMMARY = "cached_remote_summary"
  private const val KEY_CACHED_REMOTE_NOTES = "cached_remote_notes"
  private const val KEY_CACHED_REMOTE_CRITICAL = "cached_remote_critical"
  private const val KEY_LAST_FIRESTORE_CHECK_MS = "last_firestore_release_check_ms"

  const val CHANNEL_ID_APP_UPDATES = "lingosphere_app_updates_v1"
  const val NOTIFICATION_ID_APP_UPDATE = 94029
  const val FCM_TOPIC_APP_UPDATES = "lingosphere_app_updates"
  private const val FIRESTORE_CHECK_TTL_MS = 30L * 60L * 1000L // 30 minutes cache

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _updateUiState = MutableStateFlow(AppUpdateUiState())
  val updateUiState: StateFlow<AppUpdateUiState> = _updateUiState.asStateFlow()

  @Volatile
  private var playUpdateInfoCached: AppUpdateInfo? = null

  @Volatile
  private var appUpdateManagerInstance: AppUpdateManager? = null

  private val installStateListener = InstallStateUpdatedListener { state ->
    when (state.installStatus()) {
      InstallStatus.DOWNLOADED -> {
        _updateUiState.value = _updateUiState.value.copy(
          isFlexibleUpdateDownloaded = true,
          lastCheckStatusMessage = "Update downloaded! Tap 'Complete Update' to finish installing."
        )
      }
      InstallStatus.INSTALLED -> {
        _updateUiState.value = AppUpdateUiState(
          isUpdateAvailable = false,
          availableRelease = null,
          isFlexibleUpdateDownloaded = false,
          showInAppAnnouncement = false
        )
      }
      else -> {}
    }
  }

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Called on app startup and after package replacement.
   * Clears any stale update notification or banner if the installed versionCode
   * is now >= the previously notified or cached update versionCode (Requirement #10).
   */
  fun reconcileInstalledVersionOnStartup(context: Context, currentVersionCode: Int = BuildConfig.VERSION_CODE) {
    val sp = prefs(context)
    val lastNotified = sp.getInt(KEY_LAST_NOTIFIED_VERSION_CODE, 0)
    val cachedRemote = sp.getInt(KEY_CACHED_REMOTE_VERSION_CODE, 0)
    val dismissed = sp.getInt(KEY_DISMISSED_VERSION_CODE, 0)

    if (currentVersionCode >= lastNotified && lastNotified > 0) {
      cancelSystemUpdateNotification(context)
    }
    if (currentVersionCode >= cachedRemote && cachedRemote > 0) {
      sp.edit()
        .remove(KEY_CACHED_REMOTE_VERSION_CODE)
        .remove(KEY_CACHED_REMOTE_VERSION_NAME)
        .remove(KEY_CACHED_REMOTE_SUMMARY)
        .remove(KEY_CACHED_REMOTE_NOTES)
        .remove(KEY_CACHED_REMOTE_CRITICAL)
        .apply()
      cancelSystemUpdateNotification(context)
      _updateUiState.value = AppUpdateUiState()
    }
    if (currentVersionCode >= dismissed && dismissed > 0) {
      sp.edit().remove(KEY_DISMISSED_VERSION_CODE).apply()
    }
    sp.edit().putInt(KEY_LAST_INSTALLED_VERSION_CODE, currentVersionCode).apply()
  }

  fun subscribeToUpdateAnnouncementsTopic() {
    runCatching {
      FirebaseMessaging.getInstance().subscribeToTopic(FCM_TOPIC_APP_UPDATES)
    }
  }

  /**
   * Checks both Google Play In-App Updates API and Firestore `app_releases/latest`
   * for a newer published version of LingoSphere.
   */
  fun checkForUpdatesAsync(
    context: Context,
    isManualUserCheck: Boolean = false,
    installedVersionCode: Int = BuildConfig.VERSION_CODE
  ) {
    val appCtx = context.applicationContext
    reconcileInstalledVersionOnStartup(appCtx, installedVersionCode)

    if (isManualUserCheck) {
      _updateUiState.value = _updateUiState.value.copy(
        isCheckingForUpdates = true,
        lastCheckStatusMessage = "Checking Google Play for LingoSphere updates..."
      )
    }

    // 1. Check Google Play In-App Updates API
    runCatching {
      val manager = appUpdateManagerInstance ?: AppUpdateManagerFactory.create(appCtx).also {
        appUpdateManagerInstance = it
        runCatching { it.registerListener(installStateListener) }
      }
      manager.appUpdateInfo.addOnSuccessListener { info ->
        playUpdateInfoCached = info
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
          _updateUiState.value = _updateUiState.value.copy(
            isFlexibleUpdateDownloaded = true,
            isCheckingForUpdates = false,
            lastCheckStatusMessage = "New LingoSphere update is downloaded and ready to install."
          )
        }
        val playAvailableVersionCode = info.availableVersionCode()
        if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
          playAvailableVersionCode > installedVersionCode
        ) {
          val isCriticalPlayUpdate = info.updatePriority() >= 4 &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
          val release = RealReleaseChangelogCatalog.buildReleaseForVersion(
            versionCode = playAvailableVersionCode,
            versionName = "${playAvailableVersionCode}.0",
            isCritical = isCriticalPlayUpdate
          )
          onNewerReleaseDetected(
            context = appCtx,
            release = release,
            installedVersionCode = installedVersionCode,
            forceShowDialog = isManualUserCheck,
            postSystemNotificationIfAllowed = !isManualUserCheck
          )
        }
      }
    }

    // 2. Check Firestore `app_releases/latest` (respecting cache TTL and quota guard)
    scope.launch {
      try {
        val sp = prefs(appCtx)
        val now = System.currentTimeMillis()
        val lastCheck = sp.getLong(KEY_LAST_FIRESTORE_CHECK_MS, 0L)
        if (!isManualUserCheck && (now - lastCheck) < FIRESTORE_CHECK_TTL_MS) {
          val cachedCode = sp.getInt(KEY_CACHED_REMOTE_VERSION_CODE, 0)
          if (cachedCode > installedVersionCode) {
            val cachedName = sp.getString(KEY_CACHED_REMOTE_VERSION_NAME, "${cachedCode}.0").orEmpty()
            val cachedSummary = sp.getString(KEY_CACHED_REMOTE_SUMMARY, null)
            val cachedNotes = sp.getString(KEY_CACHED_REMOTE_NOTES, null)
              ?.split("\n")
              ?.filter { it.isNotBlank() }
            val cachedCritical = sp.getBoolean(KEY_CACHED_REMOTE_CRITICAL, false)
            val release = RealReleaseChangelogCatalog.buildReleaseForVersion(
              versionCode = cachedCode,
              versionName = cachedName,
              customSummary = cachedSummary,
              customNotes = cachedNotes,
              isCritical = cachedCritical
            )
            onNewerReleaseDetected(
              context = appCtx,
              release = release,
              installedVersionCode = installedVersionCode,
              forceShowDialog = false,
              postSystemNotificationIfAllowed = false
            )
          }
          return@launch
        }

        if (FirestoreQuotaManager.shouldBlockRead()) {
          if (isManualUserCheck) {
            _updateUiState.value = _updateUiState.value.copy(
              isCheckingForUpdates = false,
              lastCheckStatusMessage = "You are on LingoSphere v${BuildConfig.VERSION_NAME} (up to date)."
            )
          }
          return@launch
        }

        val dbId = appCtx.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        val doc = db.collection("app_releases").document("latest").get().await()
        sp.edit().putLong(KEY_LAST_FIRESTORE_CHECK_MS, now).apply()

        if (doc.exists()) {
          val remoteCode = (doc.getLong("versionCode") ?: 0L).toInt()
          val remoteName = doc.getString("versionName")?.takeIf { it.isNotBlank() } ?: "${remoteCode}.0"
          val remoteTitle = doc.getString("title")
          val remoteSummary = doc.getString("summary")
          val rawNotes = (doc.get("releaseNotes") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
          val isCritical = doc.getBoolean("isCritical") ?: false
          val minRequired = (doc.getLong("minRequiredVersionCode") ?: 1L).toInt()

          if (remoteCode > installedVersionCode) {
            val release = RealReleaseChangelogCatalog.buildReleaseForVersion(
              versionCode = remoteCode,
              versionName = remoteName,
              customTitle = remoteTitle,
              customSummary = remoteSummary,
              customNotes = rawNotes,
              isCritical = isCritical,
              minRequiredVersionCode = minRequired
            )
            cacheRemoteRelease(appCtx, release)
            onNewerReleaseDetected(
              context = appCtx,
              release = release,
              installedVersionCode = installedVersionCode,
              forceShowDialog = isManualUserCheck,
              postSystemNotificationIfAllowed = true
            )
            return@launch
          }
        }

        if (isManualUserCheck && !_updateUiState.value.isUpdateAvailable) {
          _updateUiState.value = _updateUiState.value.copy(
            isCheckingForUpdates = false,
            lastCheckStatusMessage = "LingoSphere v${BuildConfig.VERSION_NAME} is up to date!"
          )
        }
      } catch (e: Exception) {
        if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
          FirestoreQuotaManager.recordQuotaExhausted(e)
        }
        if (isManualUserCheck && !_updateUiState.value.isUpdateAvailable) {
          _updateUiState.value = _updateUiState.value.copy(
            isCheckingForUpdates = false,
            lastCheckStatusMessage = "LingoSphere v${BuildConfig.VERSION_NAME} is up to date."
          )
        }
      }
    }
  }

  /**
   * Processes an incoming FCM or programmatic release announcement.
   * Strictly verifies `remoteVersionCode > installedVersionCode` so fake or
   * outdated update notifications are never displayed.
   */
  fun handleRemoteReleaseAnnouncement(
    context: Context,
    remoteVersionCode: Int,
    remoteVersionName: String,
    title: String? = null,
    summary: String? = null,
    releaseNotes: List<String>? = null,
    isCritical: Boolean = false,
    minRequiredVersionCode: Int = 1,
    installedVersionCode: Int = BuildConfig.VERSION_CODE,
    postSystemNotification: Boolean = true
  ): Boolean {
    val appCtx = context.applicationContext
    reconcileInstalledVersionOnStartup(appCtx, installedVersionCode)

    // Never show an update notification if the remote version is not newer than installed
    if (remoteVersionCode <= installedVersionCode) {
      return false
    }

    val release = RealReleaseChangelogCatalog.buildReleaseForVersion(
      versionCode = remoteVersionCode,
      versionName = remoteVersionName,
      customTitle = title,
      customSummary = summary,
      customNotes = releaseNotes,
      isCritical = isCritical,
      minRequiredVersionCode = minRequiredVersionCode
    )
    cacheRemoteRelease(appCtx, release)
    onNewerReleaseDetected(
      context = appCtx,
      release = release,
      installedVersionCode = installedVersionCode,
      forceShowDialog = false,
      postSystemNotificationIfAllowed = postSystemNotification
    )
    return true
  }

  fun handleRemoteReleaseAnnouncement(
    context: Context,
    payload: Map<String, String>,
    postSystemNotificationIfPermitted: Boolean = true,
    installedVersionCode: Int = BuildConfig.VERSION_CODE
  ): Boolean {
    val remoteCode = payload["versionCode"]?.trim()?.toIntOrNull() ?: return false
    val remoteName = payload["versionName"]?.trim()?.takeIf { it.isNotBlank() } ?: "${remoteCode}.0"
    val title = payload["title"]?.trim()
    val summary = (payload["summary"] ?: payload["body"] ?: payload["message"])?.trim()
    val notes = payload["releaseNotes"]
      ?.split("\n", "•", "|")
      ?.map { it.trim() }
      ?.filter { it.isNotBlank() }
    val isCritical = payload["isCritical"]?.equals("true", ignoreCase = true) == true
    val minRequired = payload["minSupportedVersionCode"]?.toIntOrNull()
      ?: payload["minRequiredVersionCode"]?.toIntOrNull()
      ?: 1
    return handleRemoteReleaseAnnouncement(
      context = context,
      remoteVersionCode = remoteCode,
      remoteVersionName = remoteName,
      title = title,
      summary = summary,
      releaseNotes = notes,
      isCritical = isCritical,
      minRequiredVersionCode = minRequired,
      installedVersionCode = installedVersionCode,
      postSystemNotification = postSystemNotificationIfPermitted
    )
  }

  private fun cacheRemoteRelease(context: Context, release: AppReleaseInfo) {
    prefs(context).edit()
      .putInt(KEY_CACHED_REMOTE_VERSION_CODE, release.versionCode)
      .putString(KEY_CACHED_REMOTE_VERSION_NAME, release.versionName)
      .putString(KEY_CACHED_REMOTE_SUMMARY, release.summary)
      .putString(KEY_CACHED_REMOTE_NOTES, release.releaseNotes.joinToString("\n"))
      .putBoolean(KEY_CACHED_REMOTE_CRITICAL, release.isCritical)
      .apply()
  }

  private fun onNewerReleaseDetected(
    context: Context,
    release: AppReleaseInfo,
    installedVersionCode: Int,
    forceShowDialog: Boolean,
    postSystemNotificationIfAllowed: Boolean
  ) {
    if (release.versionCode <= installedVersionCode) return

    val sp = prefs(context)
    val dismissedCode = sp.getInt(KEY_DISMISSED_VERSION_CODE, 0)
    val isDismissedByUser = dismissedCode == release.versionCode && !release.isCritical
    val shouldShowBannerOrDialog = forceShowDialog || !isDismissedByUser

    _updateUiState.value = _updateUiState.value.copy(
      isUpdateAvailable = true,
      availableRelease = release,
      isCheckingForUpdates = false,
      showInAppAnnouncement = shouldShowBannerOrDialog,
      lastCheckStatusMessage = "Update available: LingoSphere Version ${release.versionName}"
    )

    if (postSystemNotificationIfAllowed && !isDismissedByUser) {
      val lastNotified = sp.getInt(KEY_LAST_NOTIFIED_VERSION_CODE, 0)
      if (lastNotified < release.versionCode) {
        val posted = postSystemUpdateNotification(context, release)
        if (posted) {
          sp.edit().putInt(KEY_LAST_NOTIFIED_VERSION_CODE, release.versionCode).apply()
        }
      }
    }
  }

  /**
   * Called when the user taps "Later" on the in-app update dialog.
   * Persists the dismissed versionCode so the same update announcement is not shown repeatedly.
   */
  fun dismissUpdateForLater(context: Context, versionCode: Int? = null) {
    val currentRelease = _updateUiState.value.availableRelease
    val targetCode = versionCode ?: currentRelease?.versionCode ?: 0
    if (targetCode > 0 && currentRelease?.isCritical != true) {
      prefs(context).edit()
        .putInt(KEY_DISMISSED_VERSION_CODE, targetCode)
        .apply()
    }
    _updateUiState.value = _updateUiState.value.copy(
      showInAppAnnouncement = false
    )
  }

  fun isVersionDismissed(context: Context, versionCode: Int): Boolean {
    return prefs(context).getInt(KEY_DISMISSED_VERSION_CODE, 0) == versionCode
  }

  fun showUpdateDialogNow() {
    if (_updateUiState.value.availableRelease != null) {
      _updateUiState.value = _updateUiState.value.copy(showInAppAnnouncement = true)
    }
  }

  fun completeFlexibleUpdate(context: Context) {
    val manager = appUpdateManagerInstance
    if (manager != null) {
      runCatching { manager.completeUpdate() }
    } else {
      openGooglePlayStoreListing(context)
    }
  }

  fun startUpdateNow(activity: Activity?, context: Context) {
    startUpdateFlowOrOpenPlayStore(activity, context)
  }

  /**
   * Starts the official Google Play In-App Update flow (Flexible or Immediate)
   * or falls back to opening the official Google Play Store listing for LingoSphere.
   */
  fun startUpdateFlowOrOpenPlayStore(activity: Activity?, context: Context) {
    val release = _updateUiState.value.availableRelease
    val manager = appUpdateManagerInstance
    val info = playUpdateInfoCached

    if (_updateUiState.value.isFlexibleUpdateDownloaded && manager != null) {
      runCatching {
        manager.completeUpdate()
        return
      }
    }

    if (activity != null && manager != null && info != null &&
      info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
    ) {
      val desiredType = if (release?.isCritical == true && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
        AppUpdateType.IMMEDIATE
      } else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
        AppUpdateType.FLEXIBLE
      } else {
        AppUpdateType.IMMEDIATE
      }

      val launched = runCatching {
        manager.startUpdateFlowForResult(
          info,
          activity,
          AppUpdateOptions.newBuilder(desiredType).build(),
          9429
        )
      }.getOrDefault(false)

      if (launched) {
        if (desiredType == AppUpdateType.FLEXIBLE) {
          _updateUiState.value = _updateUiState.value.copy(showInAppAnnouncement = false)
        }
        return
      }
    }

    openGooglePlayStoreListing(activity ?: context)
    if (release?.isCritical != true) {
      _updateUiState.value = _updateUiState.value.copy(showInAppAnnouncement = false)
    }
  }

  fun openGooglePlayStoreListing(contextOrActivity: Context) {
    val pkg = contextOrActivity.packageName
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      contextOrActivity.startActivity(marketIntent)
    } catch (_: Exception) {
      val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      runCatching { contextOrActivity.startActivity(webIntent) }
    }
  }

  fun ensureUpdateNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (nm.getNotificationChannel(CHANNEL_ID_APP_UPDATES) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID_APP_UPDATES,
          "LingoSphere App Updates",
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = "Notifications when a new version of LingoSphere is available on Google Play"
          enableVibration(true)
        }
        nm.createNotificationChannel(channel)
      }
    }
  }

  fun canPostSystemNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return runCatching {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(true)
  }

  fun postSystemUpdateNotification(context: Context, release: AppReleaseInfo): Boolean {
    if (release.versionCode <= BuildConfig.VERSION_CODE) return false
    if (!canPostSystemNotifications(context)) return false

    return try {
      ensureUpdateNotificationChannel(context)
      val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        action = SmartNotificationContract.ACTION_OPEN_SMART_NOTIFICATION
        putExtra(SmartNotificationContract.EXTRA_ROUTE, "app_update")
        putExtra("update_version_code", release.versionCode)
        putExtra("update_version_name", release.versionName)
      }
      val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID_APP_UPDATE,
        launchIntent,
        pendingFlags
      )

      val bulletPreview = release.releaseNotes.take(2).joinToString(" • ")
      val expandedBody = buildString {
        append("Version ${release.versionName} — ${release.summary}")
        if (bulletPreview.isNotBlank()) {
          append("\n• ")
          append(release.releaseNotes.take(3).joinToString("\n• "))
        }
      }

      val notification = NotificationCompat.Builder(context, CHANNEL_ID_APP_UPDATES)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("LingoSphere Update Available (v${release.versionName})")
        .setContentText(release.summary)
        .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .addAction(
          0,
          "Update Now",
          pendingIntent
        )
        .build()

      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_APP_UPDATE, notification)
      true
    } catch (e: Exception) {
      Log.w(TAG, "Failed to post system update notification: ${e.message}")
      false
    }
  }

  fun cancelSystemUpdateNotification(context: Context) {
    runCatching {
      NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_APP_UPDATE)
    }
  }

  fun resetForTesting(context: Context) {
    prefs(context).edit().clear().commit()
    _updateUiState.value = AppUpdateUiState()
    cancelSystemUpdateNotification(context)
  }

  fun resetStateForTesting(context: Context) = resetForTesting(context)
}

/**
 * Clean, modern, non-crowded In-App Update Announcement Dialog.
 */
@Composable
fun AppUpdateAnnouncementDialog(
  release: AppReleaseInfo,
  isFlexibleDownloaded: Boolean = false,
  isDownloadingFlexible: Boolean = false,
  isFlexibleReadyToInstall: Boolean = isFlexibleDownloaded,
  onUpdateNow: () -> Unit,
  onLater: () -> Unit
) {
  Dialog(
    onDismissRequest = {
      if (!release.isCritical) {
        onLater()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = !release.isCritical,
      dismissOnClickOutside = !release.isCritical,
      usePlatformDefaultWidth = false
    )
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1837)),
      border = BorderStroke(1.2.dp, LingoCyan.copy(alpha = 0.6f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .testTag("update_announcement_dialog")
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(LingoElectricBlue, LingoCyan))
              )
          ) {
            Icon(
              imageVector = Icons.Default.SystemUpdate,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "LingoSphere Update Available",
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 18.sp,
              modifier = Modifier.testTag("update_announcement_title")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = LingoCyan.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.5f))
              ) {
                Text(
                  text = "Version ${release.versionName}",
                  color = LingoCyan,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.sp,
                  modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("update_announcement_version")
                )
              }
              if (release.isCritical) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = LingoAmber.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "Required Update",
                    color = LingoAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }

        Text(
          text = release.summary,
          color = Color(0xFFE2E8F0),
          fontSize = 13.5.sp,
          lineHeight = 19.sp,
          modifier = Modifier.testTag("update_announcement_summary")
        )

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF071126),
          border = BorderStroke(1.dp, Color(0xFF1D3A73)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("update_announcement_notes")
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
          ) {
            Text(
              text = "What's Included",
              color = LingoCyan,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp
            )
            release.releaseNotes.take(4).forEach { note ->
              Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = LingoEmerald,
                  modifier = Modifier
                    .size(15.dp)
                    .padding(top = 2.dp)
                )
                Text(
                  text = note,
                  color = Color(0xFFCBD5E1),
                  fontSize = 12.sp,
                  lineHeight = 16.sp,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (!release.isCritical) {
            OutlinedButton(
              onClick = onLater,
              border = BorderStroke(1.dp, Color(0xFF334155)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("update_later_button")
            ) {
              Text(
                text = "Later",
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
              )
            }
          }

          Button(
            onClick = onUpdateNow,
            colors = ButtonDefaults.buttonColors(
              containerColor = LingoCyan,
              contentColor = Color(0xFF040B1A)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("update_now_button")
          ) {
            Text(
              text = when {
                isFlexibleReadyToInstall || isFlexibleDownloaded -> "Install Now"
                isDownloadingFlexible -> "Downloading..."
                else -> "Update Now"
              },
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.5.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  }
}

/**
 * "What's New" Dialog accessible from Profile / Preferences & Settings.
 */
@Composable
fun WhatsNewChangelogDialog(
  updateUiState: AppUpdateUiState = AppUpdateUiState(),
  updateState: AppUpdateUiState = updateUiState,
  onCheckForUpdates: () -> Unit,
  onUpdateNow: () -> Unit,
  onOpenPlayStore: () -> Unit = onUpdateNow,
  onDismiss: () -> Unit
) {
  val effectiveState = if (updateState != AppUpdateUiState()) updateState else updateUiState
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1837)),
      border = BorderStroke(1.2.dp, Color(0xFF1E3A8A)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 24.dp)
        .testTag("whats_new_dialog")
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.NewReleases,
              contentDescription = null,
              tint = LingoCyan,
              modifier = Modifier.size(24.dp)
            )
            Column {
              Text(
                text = "What's New in LingoSphere",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
              )
              Text(
                text = "Installed: Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                color = LingoCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("whats_new_installed_version")
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(36.dp)
              .testTag("close_whats_new_dialog_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = Color(0xFF94A3B8)
            )
          }
        }

        val available = effectiveState.availableRelease
        if (effectiveState.isUpdateAvailable && available != null) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LingoCyan.copy(alpha = 0.14f)),
            border = BorderStroke(1.dp, LingoCyan),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("whats_new_update_available_card")
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(14.dp)
            ) {
              Text(
                text = "New Update Available: Version ${available.versionName}",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp
              )
              Text(
                text = available.summary,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp
              )
              Button(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(
                  containerColor = LingoCyan,
                  contentColor = Color(0xFF040B1A)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .testTag("whats_new_update_now_btn")
              ) {
                Text(
                  text = "Update Now on Google Play",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 13.sp
                )
              }
            }
          }
        } else if (!effectiveState.lastCheckStatusMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0E234E),
            border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = effectiveState.lastCheckStatusMessage,
              color = LingoEmerald,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
          }
        }

        RealReleaseChangelogCatalog.RELEASE_HISTORY.forEach { release ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF071126),
            border = BorderStroke(1.dp, Color(0xFF1D3A73)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("changelog_card_v${release.versionCode}")
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(14.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Version ${release.versionName}",
                  color = LingoGold,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 14.sp
                )
                Text(
                  text = release.publishedDateLabel,
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Text(
                text = release.summary,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
              )
              HorizontalDivider(color = Color(0xFF1D3A73))
              release.releaseNotes.forEach { item ->
                Row(
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = LingoCyan,
                    modifier = Modifier
                      .size(14.dp)
                      .padding(top = 2.dp)
                  )
                  Text(
                    text = item,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedButton(
            onClick = onCheckForUpdates,
            border = BorderStroke(1.dp, LingoCyan),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("check_for_updates_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              tint = LingoCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Check for Updates",
              color = LingoCyan,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }

          Button(
            onClick = onOpenPlayStore,
            colors = ButtonDefaults.buttonColors(
              containerColor = LingoElectricBlue,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("open_google_play_store_button")
          ) {
            Text(
              text = "Google Play Store",
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}
