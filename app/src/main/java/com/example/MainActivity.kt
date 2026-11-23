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
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val mountComposeRoot: (StartupBootstrapState?) -> Unit = { preloadedState ->
      setContent {
        LingoAppRoot(
          initialBootstrapState = preloadedState,
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

    if (isRobolectric) {
      initialWindowSyncCompleted = true
      com.example.ai.AiErrorHandler.startNetworkMonitoring(applicationContext)
      rememberPendingAppCheckIntent(applicationContext, intent)
      configureAppCheck(this, intent)
      com.example.ai.GeminiService.initialize(applicationContext)
      mountComposeRoot(null)
    } else {
      // Paint a bare native dark FrameLayout on the first VRI SurfaceSyncGroup transactions (#2 / #3)
      // with zero main-thread or IO contention so WindowManager receives transaction-ready in <20ms.
      val splashContainer = android.widget.FrameLayout(this).apply {
        setBackgroundColor(AndroidColor.parseColor("#070B14"))
      }
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
        lifecycleScope.launch(Dispatchers.IO) {
          // Keep the first 1450ms completely quiet so wmsSync-VRI[MainActivity]#2 and VRI[MainActivity]#3
          // finish their 1000ms transaction-ready window with zero main-thread, RenderThread, or IO contention.
          delay(1450L)
          initialWindowSyncCompleted = true
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
        }
      }

      splashContainer.post {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          runCatching {
            window.decorView.viewTreeObserver.registerFrameCommitCallback(startPostSyncBootstrap)
          }.onFailure {
            splashContainer.postOnAnimation(startPostSyncBootstrap)
          }
        } else {
          splashContainer.postOnAnimation(startPostSyncBootstrap)
        }
      }
      // Fallback safeguard after initial SurfaceSyncGroup window if frame commit callback is not invoked
      splashContainer.postDelayed(startPostSyncBootstrap, 1200L)
    }

    if (intent?.data != null) {
      val incomingDataIntent = intent
      lifecycleScope.launch(Dispatchers.IO) {
        delay(3500L)
        com.example.data.repository.ReferralInstallAttributionStore.captureIncomingIntent(applicationContext, incomingDataIntent)
      }
    }

    if (intent?.extras != null) {
      lifecycleScope.launch {
        delay(3200L)
        handleIncomingCallIntent(intent)
        handleSmartNotificationIntent(intent)
      }
    }

    lifecycleScope.launch(Dispatchers.IO) {
      delay(4800L)
      runCatching { coil.Coil.imageLoader(applicationContext) }
      delay(1200L)
      try {
        com.example.webrtc.NativeCallNotificationManager.ensureIncomingCallChannel(applicationContext)
        com.example.webrtc.NativeCallNotificationManager.ensureActiveCallChannel(applicationContext)
        com.example.update.LingoAppUpdateController.reconcileInstalledVersionOnStartup(applicationContext)
      } catch (e: Exception) {
        android.util.Log.w("MainActivity", "Call channels init warning", e)
      }
    }

    lifecycleScope.launch(Dispatchers.Default) {
      delay(5500L)
      runCatching { CurriculumData.getLessonsForLanguage("es") }
      if (!isRobolectric) {
        runCatching {
          com.example.data.repository.InstallReferrerAttributionBridge
            .fetchAndStoreInstallReferrerIfNeeded(applicationContext)
        }
      }
    }
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
    super.onDestroy()
    speechManagerInstance?.release()
  }
}
