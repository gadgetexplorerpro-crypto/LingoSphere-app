package com.example.presence

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import com.example.R
import com.example.data.model.CommunityProfile
import com.example.data.repository.FirestoreDiagnostics
import com.example.data.repository.FirestoreQuotaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class RtdbPresenceRecord(
  val userId: String = "",
  val state: String = "offline", // "online", "away", "offline"
  val isOnline: Boolean = false,
  val showLastSeen: Boolean = true,
  val lastSeenEpochMs: Long = 0L,
  val lastHeartbeatEpochMs: Long = 0L
)

data class LocalPresenceStatus(
  val isAppInForeground: Boolean = false,
  val isNetworkConnected: Boolean = true,
  val isRtdbConnected: Boolean = false,
  val isOnlineVisible: Boolean = true,
  val showLastSeen: Boolean = true,
  val backgroundPresenceMode: String = "Away",
  val effectiveState: String = "offline", // "online", "away", "offline"
  val lastUpdatedEpochMs: Long = 0L
) {
  val isActivelyOnline: Boolean
    get() = effectiveState == "online" && isAppInForeground && isNetworkConnected && isOnlineVisible
}

/**
 * Real-Time User Presence System for LingoSphere.
 *
 * Combines:
 * 1. Firebase Realtime Database (`.info/connected` + `onDisconnect()`) for instant server-side
 *    disconnect/crash detection.
 * 2. Cloud Firestore (`community_profiles/{userId}`) real-time presence & heartbeat sync with
 *    automatic timeout expiration (`CommunityProfile.PRESENCE_TIMEOUT_MS`) so crashed or
 *    abruptly disconnected clients are automatically marked Offline after a short timeout.
 * 3. Android process foreground/background lifecycle tracking + `ConnectivityManager.NetworkCallback`
 *    so Online status is shown ONLY when the app is in the foreground and connected.
 */
object PresenceManager {
  private const val TAG = "PresenceManager"
  private const val PREFS_NAME = "lingosphere_presence_prefs"
  private const val KEY_ONLINE_VISIBLE = "is_online_visible"
  private const val KEY_SHOW_LAST_SEEN = "show_last_seen"
  private const val KEY_BG_MODE = "background_presence_mode"

  private const val HEARTBEAT_INTERVAL_MS = 25_000L
  private const val TICKER_INTERVAL_MS = 10_000L
  private const val BACKGROUND_TO_OFFLINE_DELAY_MS = 60_000L
  private const val RTDB_MIN_WRITE_INTERVAL_MS = 12_000L
  private const val FIRESTORE_PRESENCE_MIN_INTERVAL_MS = 10 * 60_000L

  private val initialized = AtomicBoolean(false)
  private val startedActivities = AtomicInteger(0)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private var appContext: Context? = null
  private var heartbeatJob: Job? = null
  private var tickerJob: Job? = null
  private var backgroundTimeoutJob: Job? = null

  private var rtdbConnectedListener: ValueEventListener? = null
  private var rtdbAllStatusListener: ValueEventListener? = null
  private var boundRtdbUid: String? = null

  @Volatile
  private var activitySubState: String = "" // "", "in_call", "available_to_match", "busy"

  @Volatile
  private var lastRtdbWriteEpochMs: Long = 0L

  @Volatile
  private var lastRtdbPublishedState: String = ""

  @Volatile
  private var lastFirestoreSyncEpochMs: Long = 0L

  @Volatile
  private var lastFirestorePublishedState: String = ""

  private val _localStatus = MutableStateFlow(LocalPresenceStatus())
  val localStatus: StateFlow<LocalPresenceStatus> = _localStatus.asStateFlow()

  private val _rtdbPresenceMap = MutableStateFlow<Map<String, RtdbPresenceRecord>>(emptyMap())
  val rtdbPresenceMap: StateFlow<Map<String, RtdbPresenceRecord>> = _rtdbPresenceMap.asStateFlow()

  private val _presenceTicker = MutableStateFlow(System.currentTimeMillis())
  val presenceTicker: StateFlow<Long> = _presenceTicker.asStateFlow()

  fun setInCallStatus(inCall: Boolean) {
    val next = if (inCall) "in_call" else if (activitySubState == "in_call") "" else activitySubState
    if (activitySubState != next) {
      activitySubState = next
      recalculateAndPublishPresence(reason = if (inCall) "entered_call" else "left_call")
    }
  }

  fun setAvailableToMatchStatus(available: Boolean) {
    val next = if (available) "available_to_match" else if (activitySubState == "available_to_match") "" else activitySubState
    if (activitySubState != next) {
      activitySubState = next
      recalculateAndPublishPresence(reason = if (available) "available_to_match_on" else "available_to_match_off")
    }
  }

  fun setBusyStatus(busy: Boolean) {
    val next = if (busy) "busy" else if (activitySubState == "busy") "" else activitySubState
    if (activitySubState != next) {
      activitySubState = next
      recalculateAndPublishPresence(reason = if (busy) "busy_on" else "busy_off")
    }
  }

  fun initialize(application: Application) {
    if (!initialized.compareAndSet(false, true)) return
    val context = application.applicationContext
    appContext = context

    startedActivities.compareAndSet(0, 1)
    _localStatus.value = _localStatus.value.copy(isAppInForeground = true)

    registerActivityLifecycle(application)
    startPresenceTicker()
    // Continuous 25s RTDB heartbeat loop disabled so background writes never consume cloud database quota.

    scope.launch {
      try {
        delay(800L)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedOnlineVisible = prefs.getBoolean(KEY_ONLINE_VISIBLE, true)
        val savedShowLastSeen = prefs.getBoolean(KEY_SHOW_LAST_SEEN, true)
        val savedBgMode = prefs.getString(KEY_BG_MODE, "Away") ?: "Away"

        val initialNetwork = checkActiveNetworkConnected(context)
        _localStatus.value = _localStatus.value.copy(
          isNetworkConnected = initialNetwork,
          isOnlineVisible = savedOnlineVisible,
          showLastSeen = savedShowLastSeen,
          backgroundPresenceMode = savedBgMode
        )

        registerNetworkMonitoring(context)
        observeFirebaseAuth()
      } catch (e: Exception) {
        Log.w(TAG, "PresenceManager background init warning: ${e.message}")
      }
    }
  }

  /**
   * Synchronizes user privacy preferences from [com.example.data.model.UserAccountSettings]
   * and immediately updates live presence in RTDB and Firestore.
   */
  fun updatePreferences(
    context: Context,
    isOnlineVisible: Boolean,
    showLastSeen: Boolean,
    backgroundPresenceMode: String
  ) {
    val cleanCtx = context.applicationContext
    if (appContext == null) {
      appContext = cleanCtx
    }
    cleanCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ONLINE_VISIBLE, isOnlineVisible)
      .putBoolean(KEY_SHOW_LAST_SEEN, showLastSeen)
      .putString(KEY_BG_MODE, backgroundPresenceMode)
      .apply()

    val current = _localStatus.value
    val changed = current.isOnlineVisible != isOnlineVisible ||
      current.showLastSeen != showLastSeen ||
      current.backgroundPresenceMode != backgroundPresenceMode

    _localStatus.value = current.copy(
      isOnlineVisible = isOnlineVisible,
      showLastSeen = showLastSeen,
      backgroundPresenceMode = backgroundPresenceMode
    )

    if (changed) {
      recalculateAndPublishPresence(reason = "preferences_changed")
    }
  }

  /**
   * Explicitly marks the user offline (e.g., before sign-out or account deletion).
   */
  suspend fun markUserOfflineNow(context: Context, userId: String? = null) {
    val uid = userId ?: try {
      FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Exception) {
      null
    } ?: return

    activitySubState = ""
    val now = System.currentTimeMillis()
    val showLastSeen = _localStatus.value.showLastSeen
    _localStatus.value = _localStatus.value.copy(
      effectiveState = "offline",
      lastUpdatedEpochMs = now
    )

    // 1. Update RTDB
    try {
      val statusRef = FirebaseDatabase.getInstance().getReference("status").child(uid)
      val payload = mapOf(
        "userId" to uid,
        "state" to "offline",
        "isOnline" to false,
        "showLastSeen" to showLastSeen,
        "lastSeenEpochMs" to ServerValue.TIMESTAMP,
        "lastHeartbeatEpochMs" to now,
        "updatedAt" to ServerValue.TIMESTAMP
      )
      statusRef.setValue(payload).await()
    } catch (e: Exception) {
      Log.d(TAG, "RTDB offline update skipped or unavailable: ${e.message}")
    }

    // 2. Update Firestore community_profiles/{uid} without extra get() read, gated by quota breaker
    if (!FirestoreQuotaManager.shouldBlockRead()) {
      try {
        val db = getFirestore(context.applicationContext)
        val docRef = db.collection("community_profiles").document(uid)
        docRef.update(
          mapOf(
            "isOnline" to false,
            "presenceState" to "offline",
            "lastSeenEpochMs" to now,
            "lastHeartbeatEpochMs" to now,
            "showLastSeen" to showLastSeen,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        lastFirestorePublishedState = "offline"
        lastFirestoreSyncEpochMs = now
        FirestoreDiagnostics.recordPresenceWrite("firestore_offline")
      } catch (e: Exception) {
        if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
          FirestoreQuotaManager.recordQuotaExhausted(e)
        }
        Log.w(TAG, "Firestore offline update deferred: ${e.message}")
      }
    }
  }

  /**
   * Merges RTDB real-time presence snapshot (if available) with Firestore [CommunityProfile]
   * and enforces heartbeat timeout rules so stale/crashed sessions never appear online.
   */
  fun resolvePartnerPresence(
    profile: CommunityProfile,
    rtdbMap: Map<String, RtdbPresenceRecord> = _rtdbPresenceMap.value,
    nowEpochMs: Long = System.currentTimeMillis()
  ): CommunityProfile {
    val rtdb = rtdbMap[profile.userId]
    val merged = if (rtdb != null && rtdb.lastHeartbeatEpochMs >= profile.lastHeartbeatEpochMs) {
      val activeStates = listOf("online", "in_call", "available_to_match", "busy")
      profile.copy(
        isOnline = rtdb.isOnline && rtdb.state in activeStates,
        presenceState = rtdb.state,
        showLastSeen = rtdb.showLastSeen,
        lastSeenEpochMs = maxOf(profile.lastSeenEpochMs, rtdb.lastSeenEpochMs),
        lastHeartbeatEpochMs = maxOf(profile.lastHeartbeatEpochMs, rtdb.lastHeartbeatEpochMs)
      )
    } else {
      profile
    }

    val effectivelyOnline = merged.isEffectivelyOnline(nowEpochMs)
    val effectivelyAway = merged.isEffectivelyAway(nowEpochMs)
    val rawState = merged.presenceState.trim().lowercase()
    val resolvedState = when {
      effectivelyOnline -> when (rawState) {
        "in_call" -> "in_call"
        "available_to_match" -> "available_to_match"
        "busy" -> "busy"
        else -> "online"
      }
      effectivelyAway -> "away"
      else -> "offline"
    }
    return merged.copy(
      isOnline = effectivelyOnline,
      presenceState = resolvedState
    )
  }

  private fun registerActivityLifecycle(application: Application) {
    application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

      override fun onActivityStarted(activity: Activity) {
        val count = startedActivities.incrementAndGet()
        if (count == 1) {
          backgroundTimeoutJob?.cancel()
          _localStatus.value = _localStatus.value.copy(isAppInForeground = true)
          recalculateAndPublishPresence(reason = "app_foregrounded")
        }
      }

      override fun onActivityResumed(activity: Activity) {
        if (!_localStatus.value.isAppInForeground) {
          _localStatus.value = _localStatus.value.copy(isAppInForeground = true)
          recalculateAndPublishPresence(reason = "activity_resumed")
        }
      }

      override fun onActivityPaused(activity: Activity) {}

      override fun onActivityStopped(activity: Activity) {
        val count = startedActivities.decrementAndGet().coerceAtLeast(0)
        startedActivities.set(count)
        if (count == 0 && !activity.isChangingConfigurations) {
          _localStatus.value = _localStatus.value.copy(isAppInForeground = false)
          recalculateAndPublishPresence(reason = "app_backgrounded")
          scheduleBackgroundToOfflineTimeout()
        }
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(activity: Activity) {}
    })
  }

  private fun scheduleBackgroundToOfflineTimeout() {
    backgroundTimeoutJob?.cancel()
    backgroundTimeoutJob = scope.launch {
      delay(BACKGROUND_TO_OFFLINE_DELAY_MS)
      if (!_localStatus.value.isAppInForeground) {
        recalculateAndPublishPresence(reason = "background_timeout_offline", forceOffline = true)
      }
    }
  }

  private fun registerNetworkMonitoring(context: Context) {
    try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

      cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          val wasConnected = _localStatus.value.isNetworkConnected
          _localStatus.value = _localStatus.value.copy(isNetworkConnected = true)
          if (!wasConnected) {
            recalculateAndPublishPresence(reason = "network_available")
          }
        }

        override fun onLost(network: Network) {
          val stillConnected = checkActiveNetworkConnected(context)
          _localStatus.value = _localStatus.value.copy(isNetworkConnected = stillConnected)
          if (!stillConnected) {
            _localStatus.value = _localStatus.value.copy(
              effectiveState = "offline",
              lastUpdatedEpochMs = System.currentTimeMillis()
            )
          }
        }
      })
    } catch (e: Exception) {
      Log.w(TAG, "NetworkCallback registration failed: ${e.message}")
    }
  }

  private fun observeFirebaseAuth() {
    try {
      FirebaseAuth.getInstance().addAuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != null) {
          bindRealtimeDatabasePresence(uid)
          recalculateAndPublishPresence(reason = "auth_state_signed_in")
        } else {
          unbindRealtimeDatabasePresence()
          _localStatus.value = _localStatus.value.copy(
            effectiveState = "offline",
            lastUpdatedEpochMs = System.currentTimeMillis()
          )
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Auth listener setup skipped: ${e.message}")
    }
  }

  private fun bindRealtimeDatabasePresence(uid: String) {
    if (boundRtdbUid == uid) return
    unbindRealtimeDatabasePresence()
    boundRtdbUid = uid

    try {
      val db = FirebaseDatabase.getInstance()
      val connectedRef = db.getReference(".info/connected")
      val userStatusRef = db.getReference("status").child(uid)
      val allStatusRef = db.getReference("status")

      val connListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val connected = snapshot.getValue(Boolean::class.java) ?: false
          _localStatus.value = _localStatus.value.copy(isRtdbConnected = connected)
          if (connected) {
            val now = System.currentTimeMillis()
            val showLastSeen = _localStatus.value.showLastSeen
            val offlinePayload = mapOf(
              "userId" to uid,
              "state" to "offline",
              "isOnline" to false,
              "showLastSeen" to showLastSeen,
              "lastSeenEpochMs" to ServerValue.TIMESTAMP,
              "lastHeartbeatEpochMs" to now,
              "updatedAt" to ServerValue.TIMESTAMP
            )
            userStatusRef.onDisconnect().setValue(offlinePayload).addOnCompleteListener {
              recalculateAndPublishPresence(reason = "rtdb_connected_onDisconnect_registered")
            }
          }
        }

        override fun onCancelled(error: DatabaseError) {
          Log.d(TAG, "RTDB .info/connected cancelled: ${error.message}")
        }
      }
      connectedRef.addValueEventListener(connListener)
      rtdbConnectedListener = connListener
      // Global /status full-tree listener intentionally omitted so clients never stream all users' RTDB changes.
    } catch (e: Exception) {
      Log.d(TAG, "RTDB presence setup unavailable in this environment: ${e.message}")
    }
  }

  private fun unbindRealtimeDatabasePresence() {
    try {
      val db = FirebaseDatabase.getInstance()
      rtdbConnectedListener?.let { db.getReference(".info/connected").removeEventListener(it) }
      rtdbAllStatusListener?.let { db.getReference("status").removeEventListener(it) }
    } catch (_: Exception) {
    }
    rtdbConnectedListener = null
    rtdbAllStatusListener = null
    boundRtdbUid = null
  }

  private fun startPresenceTicker() {
    tickerJob?.cancel()
    tickerJob = scope.launch {
      while (isActive) {
        delay(TICKER_INTERVAL_MS)
        _presenceTicker.value = System.currentTimeMillis()
      }
    }
  }

  private fun startHeartbeatLoop() {
    // Transition-only presence: continuous 25s heartbeat polling is disabled to keep cloud usage $0.
    heartbeatJob?.cancel()
    heartbeatJob = null
  }

  fun recalculateAndPublishPresence(reason: String = "manual", forceOffline: Boolean = false) {
    val current = _localStatus.value
    val nextState = when {
      forceOffline -> "offline"
      !current.isNetworkConnected -> "offline"
      !current.isOnlineVisible -> "offline"
      current.isAppInForeground -> if (activitySubState.isNotBlank()) activitySubState else "online"
      activitySubState == "in_call" -> "in_call"
      current.backgroundPresenceMode.equals("Offline", ignoreCase = true) -> "offline"
      else -> "away"
    }
    val isOnlineFlag = nextState in listOf("online", "in_call", "available_to_match", "busy")
    val now = System.currentTimeMillis()

    _localStatus.value = current.copy(
      effectiveState = nextState,
      lastUpdatedEpochMs = now
    )

    scope.launch {
      if (reason == "app_foregrounded" || reason == "activity_resumed") {
        delay(1500L)
      }
      val uid = try {
        FirebaseAuth.getInstance().currentUser?.uid
      } catch (_: Exception) {
        null
      } ?: return@launch

      publishToRtdb(
        uid = uid,
        state = nextState,
        isOnline = isOnlineFlag,
        showLastSeen = current.showLastSeen,
        nowEpochMs = now,
        forcePublish = forceOffline || reason != "foreground_heartbeat"
      )

      // Heartbeat NEVER hits Firestore; Firestore presence sync is rate-limited & triggered
      // only on explicit transitions / long cooldowns, with zero pre-read get() calls.
      val isMeaningfulTransition = reason in setOf(
        "preferences_changed",
        "viewmodel_profile_synced",
        "background_timeout_offline",
        "entered_call",
        "left_call"
      )
      val firestoreStateChanged = nextState != lastFirestorePublishedState
      val firestoreCooldownElapsed = (now - lastFirestoreSyncEpochMs) >= FIRESTORE_PRESENCE_MIN_INTERVAL_MS
      val shouldSyncFirestore = reason != "foreground_heartbeat" &&
        (isMeaningfulTransition || (firestoreStateChanged && firestoreCooldownElapsed) || forceOffline)

      if (shouldSyncFirestore && !FirestoreQuotaManager.shouldBlockRead(now)) {
        publishToFirestore(uid, nextState, isOnlineFlag, current.showLastSeen, now)
      }
    }
  }

  private fun publishToRtdb(
    uid: String,
    state: String,
    isOnline: Boolean,
    showLastSeen: Boolean,
    nowEpochMs: Long,
    forcePublish: Boolean = false
  ) {
    val stateChanged = state != lastRtdbPublishedState
    if (!forcePublish && !stateChanged && (nowEpochMs - lastRtdbWriteEpochMs) < RTDB_MIN_WRITE_INTERVAL_MS) {
      return
    }
    try {
      val statusRef = FirebaseDatabase.getInstance().getReference("status").child(uid)
      val offlinePayload = mapOf(
        "userId" to uid,
        "state" to "offline",
        "isOnline" to false,
        "showLastSeen" to showLastSeen,
        "lastSeenEpochMs" to ServerValue.TIMESTAMP,
        "lastHeartbeatEpochMs" to nowEpochMs,
        "updatedAt" to ServerValue.TIMESTAMP
      )
      statusRef.onDisconnect().setValue(offlinePayload)

      val activePayload = mapOf(
        "userId" to uid,
        "state" to state,
        "isOnline" to isOnline,
        "showLastSeen" to showLastSeen,
        "lastSeenEpochMs" to nowEpochMs,
        "lastHeartbeatEpochMs" to nowEpochMs,
        "updatedAt" to ServerValue.TIMESTAMP
      )
      statusRef.setValue(activePayload)
      lastRtdbWriteEpochMs = nowEpochMs
      lastRtdbPublishedState = state
      FirestoreDiagnostics.recordPresenceWrite("rtdb")
    } catch (e: Exception) {
      Log.d(TAG, "RTDB publish skipped: ${e.message}")
    }
  }

  private suspend fun publishToFirestore(
    uid: String,
    state: String,
    isOnline: Boolean,
    showLastSeen: Boolean,
    nowEpochMs: Long
  ) {
    val ctx = appContext ?: return
    if (FirestoreQuotaManager.shouldBlockRead(nowEpochMs)) return
    try {
      val db = getFirestore(ctx)
      val docRef = db.collection("community_profiles").document(uid)
      // Direct partial update without docRef.get(): security rules evaluate post-merge state
      // and missing docs cleanly throw NOT_FOUND until syncCommunityProfile() creates the doc.
      docRef.update(
        mapOf(
          "isOnline" to isOnline,
          "presenceState" to state,
          "lastSeenEpochMs" to nowEpochMs,
          "lastHeartbeatEpochMs" to nowEpochMs,
          "showLastSeen" to showLastSeen,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      lastFirestoreSyncEpochMs = nowEpochMs
      lastFirestorePublishedState = state
      FirestoreDiagnostics.recordPresenceWrite("firestore")
    } catch (e: Exception) {
      if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
        FirestoreQuotaManager.recordQuotaExhausted(e)
      }
      Log.d(TAG, "Firestore presence sync deferred: ${e.message}")
    }
  }

  private fun checkActiveNetworkConnected(context: Context): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }

  private fun getFirestore(context: Context): FirebaseFirestore {
    val dbId = context.getString(R.string.firestore_database_id)
    return FirebaseFirestore.getInstance(dbId)
  }
}
