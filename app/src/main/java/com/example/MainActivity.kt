package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.BillingRepository
import com.example.data.repository.CurriculumData
import com.example.speech.SpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  @Volatile
  private var speechManagerInstance: SpeechManager? = null

  @Synchronized
  private fun getOrCreateSpeechManager(): SpeechManager {
    speechManagerInstance?.let { return it }
    return try {
      SpeechManager(applicationContext).also { speechManagerInstance = it }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "SpeechManager initialization error", e)
      SpeechManager(this).also { speechManagerInstance = it }
    }
  }

  private var billingRepository: BillingRepository? = null

  @Volatile
  private var initialWindowSyncCompleted: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val isRobolectric = Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    if (isRobolectric) {
      WindowCompat.setDecorFitsSystemWindows(window, false)
      initialWindowSyncCompleted = true
      com.example.ai.AiErrorHandler.startNetworkMonitoring(applicationContext)
      rememberPendingAppCheckIntent(applicationContext, intent)
      configureAppCheck(this, intent)
      com.example.ai.GeminiService.initialize(applicationContext)
      setContent {
        LingoAppRoot(
          initialBootstrapState = null,
          onComposeReady = {},
          speechManagerProvider = { getOrCreateSpeechManager() },
          onInitializeBilling = { onEntitlementVerified, onSubscriptionDetailsVerified ->
            if (billingRepository == null) {
              try {
                billingRepository = BillingRepository(
                  context = this@MainActivity,
                  externalScope = CoroutineScope(Dispatchers.Main),
                  onEntitlementVerified = onEntitlementVerified,
                  onSubscriptionDetailsVerified = onSubscriptionDetailsVerified
                )
              } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "BillingRepository init failed", e)
              }
            }
            billingRepository
          }
        )
      }
      return
    }

    // Paint a bare native dark FrameLayout + lightweight VSYNC heartbeat View so every
    // ViewRootImpl SurfaceSyncGroup (including wmsSync-VRI[MainActivity]#2 and VRI[MainActivity]#3)
    // commits a RenderThread buffer within <16ms with zero coroutine/dispatcher/IO contention.
    val darkBgColor = AndroidColor.parseColor("#070B14")
    val splashContainer = android.widget.FrameLayout(this).apply {
      setBackgroundColor(darkBgColor)
    }
    val syncHeartbeatView = object : android.view.View(this) {
      private var frameTick = 0
      override fun onDraw(canvas: android.graphics.Canvas) {
        canvas.drawColor(darkBgColor)
        if (!initialWindowSyncCompleted && frameTick < 90) {
          frameTick++
          postInvalidateOnAnimation()
        }
      }
    }
    splashContainer.addView(
      syncHeartbeatView,
      android.widget.FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
      )
    )
    setContentView(
      splashContainer,
      android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
      )
    )

    val launchIntent = intent
    var composeBootstrapStarted = false

    val startPostSyncBootstrap = Runnable {
      if (composeBootstrapStarted || isDestroyed || isFinishing) return@Runnable
      composeBootstrapStarted = true
      initialWindowSyncCompleted = true
      runCatching { splashContainer.removeView(syncHeartbeatView) }
      runCatching { WindowCompat.setDecorFitsSystemWindows(window, false) }

      lifecycleScope.launch(Dispatchers.IO) {
        runCatching { com.example.ai.AiErrorHandler.startNetworkMonitoring(applicationContext) }
        runCatching { rememberPendingAppCheckIntent(applicationContext, launchIntent) }
        val preloaded = runCatching { loadLightweightStartupBootstrap(applicationContext) }.getOrNull()
        kotlinx.coroutines.withContext(Dispatchers.Main) {
          if (!isDestroyed && !isFinishing) {
            attachLingoComposeRootView(
              activity = this@MainActivity,
              hostContainer = splashContainer,
              bootstrapState = preloaded,
              onComposeReady = {},
              speechManagerProvider = { getOrCreateSpeechManager() },
              onInitializeBilling = { onEntitlementVerified, onSubscriptionDetailsVerified ->
                if (billingRepository == null) {
                  try {
                    billingRepository = BillingRepository(
                      context = this@MainActivity,
                      externalScope = CoroutineScope(Dispatchers.Main),
                      onEntitlementVerified = onEntitlementVerified,
                      onSubscriptionDetailsVerified = onSubscriptionDetailsVerified
                    )
                  } catch (e: Throwable) {
                    android.util.Log.w("MainActivity", "BillingRepository init failed", e)
                  }
                }
                billingRepository
              }
            )
          }
        }
        delay(1500L)
        configureAppCheckSafely(applicationContext, launchIntent)
        com.example.ai.GeminiService.initialize(applicationContext)

        if (launchIntent?.data != null) {
          delay(800L)
          runCatching {
            com.example.data.repository.ReferralInstallAttributionStore
              .captureIncomingIntent(applicationContext, launchIntent)
          }
        }
        if (launchIntent?.extras != null) {
          kotlinx.coroutines.withContext(Dispatchers.Main) {
            if (!isDestroyed && !isFinishing) {
              handleIncomingCallIntent(launchIntent)
              handleSmartNotificationIntent(launchIntent)
            }
          }
        }
        delay(1800L)
        runCatching { coil.Coil.imageLoader(applicationContext) }
        delay(800L)
        try {
          com.example.webrtc.NativeCallNotificationManager.ensureIncomingCallChannel(applicationContext)
          com.example.webrtc.NativeCallNotificationManager.ensureActiveCallChannel(applicationContext)
          com.example.update.LingoAppUpdateController.reconcileInstalledVersionOnStartup(applicationContext)
        } catch (e: Exception) {
          android.util.Log.w("MainActivity", "Call channels init warning", e)
        }
        runCatching { CurriculumData.getLessonsForLanguage("es") }
        runCatching {
          com.example.data.repository.InstallReferrerAttributionBridge
            .fetchAndStoreInstallReferrerIfNeeded(applicationContext)
        }
      }
    }

    // Wait 1550ms using standard Android Handler before touching lifecycleScope/Dispatchers/Compose
    // so wmsSync-VRI[MainActivity]#2 and VRI[MainActivity]#3 complete their 1000ms window cleanly.
    splashContainer.postDelayed(startPostSyncBootstrap, 1550L)
  }

  override fun onResume() {
    super.onResume()
    if (!initialWindowSyncCompleted) {
      return
    }
    lifecycleScope.launch(Dispatchers.IO) {
      com.example.data.repository.AccessPolicyManager.lastFirestoreInstance?.let { db ->
        com.example.data.repository.AccessPolicyManager.syncPolicyAndEntitlementFromFirestore(db)
      }
      billingRepository?.queryExistingPurchases()
      com.example.ads.AdService.onUserProfileUpdated(
        com.example.ads.AdService.state.value.lastKnownUserProfile
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    rememberPendingAppCheckIntent(applicationContext, intent)
    if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      configureAppCheck(this, intent)
    } else {
      lifecycleScope.launch(Dispatchers.IO) {
        configureAppCheckSafely(applicationContext, intent)
      }
    }
    if (intent.data != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        com.example.data.repository.ReferralInstallAttributionStore.captureIncomingIntent(applicationContext, intent)
      }
    }
    handleIncomingCallIntent(intent)
    handleSmartNotificationIntent(intent)
  }

  private fun handleSmartNotificationIntent(intent: Intent?) {
    val deepLink = com.example.notifications.SmartNotificationContract.fromIntent(intent) ?: return
    com.example.notifications.SmartNotificationLaunchRegistry.dispatchDeepLink(deepLink)
  }

  private fun handleIncomingCallIntent(intent: Intent?) {
    intent ?: return
    val parsedFromContract = com.example.webrtc.FcmCallPayload.fromIntent(intent)
    val fallbackPayload = if (parsedFromContract == null) {
      val extras = intent.extras
      val roomId = extras?.getString("roomId") ?: extras?.getString("callId")
      if (extras != null && !roomId.isNullOrBlank()) {
        val map = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
          extras.getString(key)?.let { map[key] = it }
        }
        com.example.webrtc.FcmCallPayload.fromMap(map)?.let {
          it.toCallRoom() to com.example.webrtc.IncomingCallContract.ACTION_SHOW_CALL
        }
      } else {
        null
      }
    } else {
      null
    }

    val (callRoom, action) = parsedFromContract ?: fallbackPayload ?: return
    enableLockScreenDisplayForCall()

    if (action == com.example.webrtc.IncomingCallContract.ACTION_ANSWER_CALL ||
      action == com.example.webrtc.IncomingCallContract.ACTION_DECLINE_CALL
    ) {
      com.example.webrtc.IncomingCallRegistry.markRoomTerminal(this, callRoom.roomId)
      com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(this, callRoom.roomId)
    }
    com.example.webrtc.IncomingCallRegistry.dispatchLaunchCommand(callRoom, action)
  }

  private fun enableLockScreenDisplayForCall() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        km?.requestDismissKeyguard(this, null)
      } else {
        @Suppress("DEPRECATION")
        window.addFlags(
          android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
      }
    } catch (_: Exception) {}
  }

  override fun onDestroy() {
    com.example.ads.AdService.detachActivity(this)
    super.onDestroy()
    speechManagerInstance?.release()
  }
}
