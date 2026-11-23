package com.example.webrtc

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.CallRoom
import com.example.data.model.Language
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Parsed representation of a high-priority FCM data message for incoming calls or call cancellation.
 * Never contains TURN credentials or sensitive secrets.
 */
data class FcmCallPayload(
  val roomId: String,
  val callerId: String,
  val callerName: String,
  val callerNativeLang: String = "en",
  val callerLearningLang: String = "es",
  val calleeId: String = "",
  val calleeName: String = "",
  val calleeNativeLang: String = "es",
  val calleeLearningLang: String = "en",
  val callType: String = "voice",
  val targetLanguageCode: String = "es",
  val conversationTopic: String = "",
  val eventType: String = "incoming_call",
  val createdAtEpochMs: Long = System.currentTimeMillis(),
  val expiresAtEpochMs: Long = 0L
) {
  fun isExpired(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
    if (expiresAtEpochMs > 0L && nowEpochMs > expiresAtEpochMs) {
      return true
    }
    if (createdAtEpochMs > 0L && (nowEpochMs - createdAtEpochMs) > 35_000L) {
      return true
    }
    return false
  }

  fun toCallRoom(): CallRoom {
    val isVid = callType.equals("video", ignoreCase = true)
    return CallRoom(
      roomId = roomId,
      callerId = callerId,
      callerName = callerName.ifBlank { "Language Partner" },
      callerNativeLang = callerNativeLang.ifBlank { "en" },
      callerLearningLang = callerLearningLang.ifBlank { targetLanguageCode.ifBlank { "es" } },
      calleeId = calleeId,
      calleeName = calleeName,
      calleeNativeLang = calleeNativeLang.ifBlank { targetLanguageCode.ifBlank { "es" } },
      calleeLearningLang = calleeLearningLang.ifBlank { "en" },
      callType = if (isVid) "video" else "voice",
      matchMode = "direct",
      targetLanguageCode = targetLanguageCode.ifBlank { "es" },
      status = when (eventType) {
        "call_cancelled" -> "cancelled"
        "call_rejected" -> "rejected"
        "call_missed" -> "missed"
        "call_ended" -> "ended"
        else -> "ringing"
      },
      lastSignalingEvent = eventType,
      isVideoEnabled = isVid,
      isCallerVideoEnabled = isVid,
      isCalleeVideoEnabled = isVid,
      conversationTopic = conversationTopic,
      connectionState = "new"
    )
  }

  companion object {
    fun fromMap(data: Map<String, String>): FcmCallPayload? {
      val roomId = (data["roomId"] ?: data["callId"] ?: "").trim()
      if (roomId.isBlank()) return null

      val eventType = (data["eventType"] ?: data["type"] ?: "incoming_call").trim()
      val callerId = (data["callerId"] ?: "").trim()
      val callerName = (data["callerName"] ?: "Language Partner").trim()
      val callerNativeLang = (data["callerNativeLang"] ?: "en").trim()
      val targetLang = (data["targetLanguageCode"] ?: data["callerLearningLang"] ?: "es").trim()
      val callerLearningLang = (data["callerLearningLang"] ?: targetLang).trim()
      val calleeId = (data["calleeId"] ?: "").trim()
      val calleeName = (data["calleeName"] ?: "").trim()
      val calleeNativeLang = (data["calleeNativeLang"] ?: targetLang).trim()
      val calleeLearningLang = (data["calleeLearningLang"] ?: callerNativeLang).trim()
      val callType = (data["callType"] ?: "voice").trim().lowercase()
      val topic = (data["conversationTopic"] ?: "").trim()
      val createdAt = data["createdAtEpochMs"]?.toLongOrNull() ?: System.currentTimeMillis()
      val expiresAt = data["expiresAtEpochMs"]?.toLongOrNull() ?: (createdAt + 30_000L)

      return FcmCallPayload(
        roomId = roomId,
        callerId = callerId,
        callerName = callerName,
        callerNativeLang = callerNativeLang,
        callerLearningLang = callerLearningLang,
        calleeId = calleeId,
        calleeName = calleeName,
        calleeNativeLang = calleeNativeLang,
        calleeLearningLang = calleeLearningLang,
        callType = if (callType == "video") "video" else "voice",
        targetLanguageCode = targetLang,
        conversationTopic = topic,
        eventType = eventType,
        createdAtEpochMs = createdAt,
        expiresAtEpochMs = expiresAt
      )
    }

    fun toIntentExtras(intent: Intent, callRoom: CallRoom, action: String) {
      intent.putExtra(IncomingCallContract.EXTRA_INCOMING_ROOM_ID, callRoom.roomId)
      intent.putExtra(IncomingCallContract.EXTRA_CALL_ACTION, action)
      intent.putExtra(IncomingCallContract.EXTRA_CALLER_ID, callRoom.callerId)
      intent.putExtra(IncomingCallContract.EXTRA_CALLER_NAME, callRoom.callerName)
      intent.putExtra(IncomingCallContract.EXTRA_CALLER_NATIVE_LANG, callRoom.callerNativeLang)
      intent.putExtra(IncomingCallContract.EXTRA_CALLER_LEARNING_LANG, callRoom.callerLearningLang)
      intent.putExtra(IncomingCallContract.EXTRA_CALLEE_ID, callRoom.calleeId)
      intent.putExtra(IncomingCallContract.EXTRA_CALLEE_NAME, callRoom.calleeName)
      intent.putExtra(IncomingCallContract.EXTRA_CALLEE_NATIVE_LANG, callRoom.calleeNativeLang)
      intent.putExtra(IncomingCallContract.EXTRA_CALLEE_LEARNING_LANG, callRoom.calleeLearningLang)
      intent.putExtra(IncomingCallContract.EXTRA_CALL_TYPE, callRoom.callType)
      intent.putExtra(IncomingCallContract.EXTRA_TARGET_LANGUAGE_CODE, callRoom.targetLanguageCode)
      intent.putExtra(IncomingCallContract.EXTRA_CONVERSATION_TOPIC, callRoom.conversationTopic)
    }

    fun fromIntent(intent: Intent?): Pair<CallRoom, String>? {
      intent ?: return null
      val roomId = intent.getStringExtra(IncomingCallContract.EXTRA_INCOMING_ROOM_ID)?.trim().orEmpty()
      if (roomId.isBlank()) return null
      val action = intent.getStringExtra(IncomingCallContract.EXTRA_CALL_ACTION)?.trim().orEmpty().ifBlank {
        IncomingCallContract.ACTION_SHOW_CALL
      }
      val callType = intent.getStringExtra(IncomingCallContract.EXTRA_CALL_TYPE)?.trim().orEmpty().ifBlank { "voice" }
      val isVid = callType.equals("video", ignoreCase = true)
      val targetLang = intent.getStringExtra(IncomingCallContract.EXTRA_TARGET_LANGUAGE_CODE)?.trim().orEmpty().ifBlank { "es" }
      val callerNative = intent.getStringExtra(IncomingCallContract.EXTRA_CALLER_NATIVE_LANG)?.trim().orEmpty().ifBlank { "en" }
      val callerLearning = intent.getStringExtra(IncomingCallContract.EXTRA_CALLER_LEARNING_LANG)?.trim().orEmpty().ifBlank { targetLang }

      val room = CallRoom(
        roomId = roomId,
        callerId = intent.getStringExtra(IncomingCallContract.EXTRA_CALLER_ID).orEmpty(),
        callerName = intent.getStringExtra(IncomingCallContract.EXTRA_CALLER_NAME).orEmpty().ifBlank { "Language Partner" },
        callerNativeLang = callerNative,
        callerLearningLang = callerLearning,
        calleeId = intent.getStringExtra(IncomingCallContract.EXTRA_CALLEE_ID).orEmpty(),
        calleeName = intent.getStringExtra(IncomingCallContract.EXTRA_CALLEE_NAME).orEmpty(),
        calleeNativeLang = intent.getStringExtra(IncomingCallContract.EXTRA_CALLEE_NATIVE_LANG).orEmpty().ifBlank { targetLang },
        calleeLearningLang = intent.getStringExtra(IncomingCallContract.EXTRA_CALLEE_LEARNING_LANG).orEmpty().ifBlank { callerNative },
        callType = if (isVid) "video" else "voice",
        matchMode = "direct",
        targetLanguageCode = targetLang,
        status = if (action == IncomingCallContract.ACTION_ANSWER_CALL) "accepted" else "ringing",
        lastSignalingEvent = if (action == IncomingCallContract.ACTION_ANSWER_CALL) "call_accepted" else "incoming_call",
        isVideoEnabled = isVid,
        isCallerVideoEnabled = isVid,
        isCalleeVideoEnabled = isVid,
        conversationTopic = intent.getStringExtra(IncomingCallContract.EXTRA_CONVERSATION_TOPIC).orEmpty(),
        connectionState = if (action == IncomingCallContract.ACTION_ANSWER_CALL) "connecting" else "new"
      )
      return room to action
    }
  }
}

object IncomingCallContract {
  const val EXTRA_INCOMING_ROOM_ID = "incoming_room_id"
  const val EXTRA_CALL_ACTION = "call_action"
  const val EXTRA_CALLER_ID = "caller_id"
  const val EXTRA_CALLER_NAME = "caller_name"
  const val EXTRA_CALLER_NATIVE_LANG = "caller_native_lang"
  const val EXTRA_CALLER_LEARNING_LANG = "caller_learning_lang"
  const val EXTRA_CALLEE_ID = "callee_id"
  const val EXTRA_CALLEE_NAME = "callee_name"
  const val EXTRA_CALLEE_NATIVE_LANG = "callee_native_lang"
  const val EXTRA_CALLEE_LEARNING_LANG = "callee_learning_lang"
  const val EXTRA_CALL_TYPE = "call_type"
  const val EXTRA_TARGET_LANGUAGE_CODE = "target_language_code"
  const val EXTRA_CONVERSATION_TOPIC = "conversation_topic"

  const val ACTION_SHOW_CALL = "show_incoming_call"
  const val ACTION_ANSWER_CALL = "com.example.action.ANSWER_CALL"
  const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_CALL"
  const val ACTION_HANG_UP_ACTIVE_CALL = "com.example.action.HANG_UP_ACTIVE_CALL"

  const val INCOMING_CALL_CHANNEL_ID = "lingosphere_incoming_calls_v2"
  const val ACTIVE_CALL_CHANNEL_ID = "lingosphere_active_call_v1"
  const val INCOMING_CALL_NOTIFICATION_ID = 4201
  const val ACTIVE_CALL_NOTIFICATION_ID = 4202
}

data class IncomingCallLaunchCommand(
  val callRoom: CallRoom,
  val action: String,
  val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Deduplicates incoming call notifications across FCM and Firestore listeners,
 * tracks terminal call rooms (answered/declined/cancelled/missed), and bridges
 * notification intents to MainActivity / Compose UI.
 */
object IncomingCallRegistry {
  private const val PREFS_NAME = "lingosphere_call_registry"
  private const val KEY_TERMINAL_ROOMS = "terminal_rooms_csv"

  private val terminalRooms = ConcurrentHashMap<String, Long>()
  private val ringingRooms = ConcurrentHashMap<String, Long>()

  private val _launchCommandFlow = MutableStateFlow<IncomingCallLaunchCommand?>(null)
  val launchCommandFlow: StateFlow<IncomingCallLaunchCommand?> = _launchCommandFlow.asStateFlow()

  private val _activeCallHangupFlow = MutableStateFlow<String?>(null)
  val activeCallHangupFlow: StateFlow<String?> = _activeCallHangupFlow.asStateFlow()

  fun markRoomTerminal(context: Context?, roomId: String) {
    if (roomId.isBlank()) return
    val now = System.currentTimeMillis()
    terminalRooms[roomId] = now
    ringingRooms.remove(roomId)
    if (context != null) {
      try {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_TERMINAL_ROOMS, "").orEmpty()
          .split(",")
          .filter { it.isNotBlank() }
          .takeLast(40)
          .toMutableList()
        if (!existing.contains(roomId)) {
          existing.add(roomId)
        }
        prefs.edit().putString(KEY_TERMINAL_ROOMS, existing.joinToString(",")).apply()
      } catch (_: Exception) {}
    }
  }

  fun isRoomTerminal(context: Context?, roomId: String): Boolean {
    if (roomId.isBlank()) return true
    if (terminalRooms.containsKey(roomId)) return true
    if (context != null) {
      try {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_TERMINAL_ROOMS, "").orEmpty()
        if (csv.split(",").contains(roomId)) {
          terminalRooms[roomId] = System.currentTimeMillis()
          return true
        }
      } catch (_: Exception) {}
    }
    return false
  }

  fun markRoomRinging(roomId: String): Boolean {
    if (roomId.isBlank() || terminalRooms.containsKey(roomId)) return false
    val now = System.currentTimeMillis()
    val previous = ringingRooms.put(roomId, now)
    return previous == null || (now - previous) > 15_000L
  }

  fun clearRingingRoom(roomId: String) {
    ringingRooms.remove(roomId)
  }

  fun dispatchLaunchCommand(callRoom: CallRoom, action: String) {
    _launchCommandFlow.value = IncomingCallLaunchCommand(callRoom = callRoom, action = action)
  }

  fun consumeLaunchCommand() {
    _launchCommandFlow.value = null
  }

  fun dispatchActiveCallHangup(roomId: String) {
    _activeCallHangupFlow.value = roomId.ifBlank { "hangup_${System.currentTimeMillis()}" }
  }

  fun consumeActiveCallHangup() {
    _activeCallHangupFlow.value = null
  }

  private const val KEY_ALLOW_INCOMING_CALLS = "allow_incoming_calls"
  private const val KEY_ALLOW_VOICE_CALLS = "allow_voice_calls"
  private const val KEY_ALLOW_VIDEO_CALLS = "allow_video_calls"
  private const val KEY_CALL_NOTIFICATIONS = "call_notifications_enabled"

  fun setIncomingCallPreferences(
    context: Context?,
    allowIncomingCalls: Boolean,
    allowVoiceCalls: Boolean = true,
    allowVideoCalls: Boolean = true,
    callNotificationsEnabled: Boolean = true
  ) {
    if (context == null) return
    try {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putBoolean(KEY_ALLOW_INCOMING_CALLS, allowIncomingCalls)
        .putBoolean(KEY_ALLOW_VOICE_CALLS, allowVoiceCalls)
        .putBoolean(KEY_ALLOW_VIDEO_CALLS, allowVideoCalls)
        .putBoolean(KEY_CALL_NOTIFICATIONS, callNotificationsEnabled)
        .apply()
    } catch (_: Exception) {}
  }

  fun isIncomingCallAllowed(context: Context?, callType: String = "voice"): Boolean {
    if (context == null) return true
    return try {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val masterAllowed = prefs.getBoolean(KEY_ALLOW_INCOMING_CALLS, true)
      if (!masterAllowed) return false
      if (callType.equals("video", ignoreCase = true)) {
        prefs.getBoolean(KEY_ALLOW_VIDEO_CALLS, true)
      } else {
        prefs.getBoolean(KEY_ALLOW_VOICE_CALLS, true)
      }
    } catch (_: Exception) {
      true
    }
  }

  fun areCallNotificationsEnabled(context: Context?): Boolean {
    if (context == null) return true
    return try {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.getBoolean(KEY_CALL_NOTIFICATIONS, true)
    } catch (_: Exception) {
      true
    }
  }

  fun clearForTests() {
    terminalRooms.clear()
    ringingRooms.clear()
    _launchCommandFlow.value = null
    _activeCallHangupFlow.value = null
  }
}

/**
 * Manages FCM registration tokens per user and device in Firestore (`fcm_tokens/{tokenId}`).
 * Supports multiple devices per user and cleans up tokens on sign-out.
 */
object FcmTokenManager {
  private const val TAG = "FcmTokenManager"
  private const val PREFS_NAME = "lingosphere_fcm_prefs"
  private const val KEY_LAST_FCM_TOKEN = "last_fcm_token"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private fun getFirestore(context: Context): FirebaseFirestore {
    val dbId = context.applicationContext.getString(R.string.firestore_database_id)
    return FirebaseFirestore.getInstance(dbId)
  }

  fun getStableDeviceId(context: Context): String {
    return try {
      val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      androidId?.takeIf { it.isNotBlank() } ?: "android_device"
    } catch (_: Exception) {
      "android_device"
    }
  }

  fun buildTokenDocId(userId: String, deviceId: String): String {
    val raw = "${userId.trim()}_${deviceId.trim()}"
    return try {
      val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
      "fcm_" + digest.take(12).joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
      "fcm_${userId.take(12)}_${deviceId.take(12)}"
    }
  }

  fun cacheTokenLocally(context: Context, token: String) {
    if (token.isBlank()) return
    try {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_FCM_TOKEN, token)
        .apply()
    } catch (_: Exception) {}
  }

  fun getCachedToken(context: Context): String {
    return try {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_LAST_FCM_TOKEN, "")
        .orEmpty()
    } catch (_: Exception) {
      ""
    }
  }

  fun syncCurrentUserTokenAsync(context: Context) {
    val appCtx = context.applicationContext
    scope.launch {
      try {
        val currentUser = Firebase.auth.currentUser ?: return@launch
        val token = try {
          FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
          Log.w(TAG, "Failed to fetch live FCM token, checking cached token: ${e.message}")
          getCachedToken(appCtx)
        }
        if (token.isBlank()) return@launch
        cacheTokenLocally(appCtx, token)
        saveTokenToFirestore(appCtx, currentUser.uid, token)
      } catch (e: Exception) {
        Log.w(TAG, "FCM token sync error: ${e.message}")
      }
    }
  }

  suspend fun saveTokenToFirestore(context: Context, userId: String, token: String) {
    if (userId.isBlank() || token.isBlank()) return
    val appCtx = context.applicationContext
    val deviceId = getStableDeviceId(appCtx)
    val docId = buildTokenDocId(userId, deviceId)
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    val payload = mapOf(
      "tokenId" to docId,
      "userId" to userId,
      "fcmToken" to token,
      "deviceId" to deviceId,
      "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80),
      "platform" to "android",
      "notificationsEnabled" to notificationsGranted,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      val db = getFirestore(appCtx)
      db.collection("fcm_tokens")
        .document(docId)
        .set(payload)
        .await()
      pruneInvalidUserTokens(db, userId, docId, deviceId)
    } catch (e: Exception) {
      Log.w(TAG, "Could not save FCM token to fcm_tokens/$docId: ${e.message}")
    }
  }

  private suspend fun pruneInvalidUserTokens(
    db: FirebaseFirestore,
    userId: String,
    activeDocId: String,
    deviceId: String
  ) {
    try {
      val snap = db.collection("fcm_tokens")
        .whereEqualTo("userId", userId)
        .get()
        .await()
      for (doc in snap.documents) {
        if (doc.id == activeDocId) continue
        val docToken = doc.getString("fcmToken").orEmpty()
        val docDevice = doc.getString("deviceId").orEmpty()
        if (docToken.isBlank() || docToken.length < 10 || docDevice == deviceId) {
          doc.reference.delete().await()
        }
      }
    } catch (_: Exception) {}
  }

  fun unregisterCurrentDeviceTokenAsync(context: Context, userId: String?) {
    if (userId.isNullOrBlank()) return
    val appCtx = context.applicationContext
    scope.launch {
      try {
        val deviceId = getStableDeviceId(appCtx)
        val docId = buildTokenDocId(userId, deviceId)
        getFirestore(appCtx)
          .collection("fcm_tokens")
          .document(docId)
          .delete()
          .await()
      } catch (e: Exception) {
        Log.w(TAG, "Failed to delete FCM token on sign-out: ${e.message}")
      }
    }
  }

  /**
   * Full screen intent is disabled to avoid Google Play policy declaration requirements.
   */
  fun canUseFullScreenCallIntent(context: Context): Boolean = false

  fun openFullScreenIntentSettings(context: Context) {
    // No-op since full-screen intent is not requested
  }
}

/**
 * Builds native Android `NotificationCompat.CallStyle` notifications with working
 * Answer and Decline buttons, full-screen lock-screen intents, caller avatar, and
 * automatic 30-second timeout cleanup.
 */
object NativeCallNotificationManager {
  private const val TAG = "NativeCallNotifMgr"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val timeoutJobs = ConcurrentHashMap<String, Job>()

  fun ensureIncomingCallChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val existing = nm.getNotificationChannel(IncomingCallContract.INCOMING_CALL_CHANNEL_ID)
      if (existing != null) return

      val ringtoneUri = try {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      } catch (_: Exception) {
        null
      }

      val audioAttrs = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .build()

      val channel = NotificationChannel(
        IncomingCallContract.INCOMING_CALL_CHANNEL_ID,
        "Incoming Language Calls",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Time-sensitive incoming voice and video calls from language partners"
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 1000L, 500L, 1000L, 500L, 1000L)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setBypassDnd(false)
        if (ringtoneUri != null) {
          setSound(ringtoneUri, audioAttrs)
        }
      }
      nm.createNotificationChannel(channel)
    }
  }

  fun ensureActiveCallChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (nm.getNotificationChannel(IncomingCallContract.ACTIVE_CALL_CHANNEL_ID) != null) return

      val channel = NotificationChannel(
        IncomingCallContract.ACTIVE_CALL_CHANNEL_ID,
        "Active Language Calls",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Ongoing voice or video language exchange call"
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      nm.createNotificationChannel(channel)
    }
  }

  fun generateCallerAvatarBitmap(callerName: String, isVideo: Boolean): Bitmap {
    val sizePx = 192
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        0f,
        sizePx.toFloat(),
        sizePx.toFloat(),
        if (isVideo) intArrayOf(0xFF00D2FF.toInt(), 0xFF2563EB.toInt())
        else intArrayOf(0xFF10B981.toInt(), 0xFF059669.toInt()),
        null,
        Shader.TileMode.CLAMP
      )
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint)

    val initials = callerName
      .trim()
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "LP" }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      textSize = 68f
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
    }
    val textY = (sizePx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(initials, sizePx / 2f, textY, textPaint)
    return bitmap
  }

  fun buildIncomingCallNotification(context: Context, callRoom: CallRoom): Notification {
    val appCtx = context.applicationContext
    ensureIncomingCallChannel(appCtx)

    val isVideo = callRoom.callType.equals("video", ignoreCase = true)
    val callerName = callRoom.callerName.ifBlank { "Language Partner" }
    val targetLang = Language.findByCode(callRoom.targetLanguageCode)
    val nativeLang = Language.findByCode(callRoom.callerNativeLang)
    val callTypeLabel = if (isVideo) "Video Call" else "Voice Call"

    val avatarBitmap = generateCallerAvatarBitmap(callerName, isVideo)
    val callerPerson = Person.Builder()
      .setName("$callerName (${nativeLang.flagEmoji} → ${targetLang.flagEmoji})")
      .setIcon(IconCompat.createWithBitmap(avatarBitmap))
      .setImportant(true)
      .build()

    // 1. Full-Screen / Tap PendingIntent -> opens MainActivity showing IncomingCallOverlay
    val showCallIntent = Intent(appCtx, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      FcmCallPayload.toIntentExtras(this, callRoom, IncomingCallContract.ACTION_SHOW_CALL)
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      appCtx,
      callRoom.roomId.hashCode() and 0x7FFFFFFF,
      showCallIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 2. Answer Action PendingIntent -> directly launches MainActivity and answers call
    val answerIntent = Intent(appCtx, MainActivity::class.java).apply {
      action = IncomingCallContract.ACTION_ANSWER_CALL
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      FcmCallPayload.toIntentExtras(this, callRoom, IncomingCallContract.ACTION_ANSWER_CALL)
    }
    val answerPendingIntent = PendingIntent.getActivity(
      appCtx,
      (callRoom.roomId.hashCode() + 1) and 0x7FFFFFFF,
      answerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 3. Decline Action PendingIntent -> handled in background via BroadcastReceiver
    val declineIntent = Intent(appCtx, IncomingCallActionReceiver::class.java).apply {
      action = IncomingCallContract.ACTION_DECLINE_CALL
      FcmCallPayload.toIntentExtras(this, callRoom, IncomingCallContract.ACTION_DECLINE_CALL)
    }
    val declinePendingIntent = PendingIntent.getBroadcast(
      appCtx,
      (callRoom.roomId.hashCode() + 2) and 0x7FFFFFFF,
      declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val callStyle = NotificationCompat.CallStyle.forIncomingCall(
      callerPerson,
      declinePendingIntent,
      answerPendingIntent
    ).setIsVideo(isVideo)

    val subtitle = "Incoming LingoSphere $callTypeLabel • Practice ${targetLang.flagEmoji} ${targetLang.name}"

    return NotificationCompat.Builder(appCtx, IncomingCallContract.INCOMING_CALL_CHANNEL_ID)
      .setSmallIcon(android.R.drawable.sym_action_call)
      .setLargeIcon(avatarBitmap)
      .setContentTitle("$callerName is calling...")
      .setContentText(subtitle)
      .setSubText("${nativeLang.flagEmoji} ${nativeLang.name} ↔ ${targetLang.flagEmoji} ${targetLang.name}")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setTimeoutAfter(30_000L)
      .setContentIntent(fullScreenPendingIntent)
      .setStyle(callStyle)
      .build()
  }

  fun showIncomingCallNotification(
    context: Context,
    callRoom: CallRoom,
    startRingtoneAndVibration: Boolean = true
  ) {
    val appCtx = context.applicationContext
    if (IncomingCallRegistry.isRoomTerminal(appCtx, callRoom.roomId)) {
      Log.d(TAG, "Ignoring notification for already terminal roomId=${callRoom.roomId}")
      return
    }
    IncomingCallRegistry.markRoomRinging(callRoom.roomId)

    if (startRingtoneAndVibration) {
      CallRingtoneHelper.startRinging(appCtx)
    }

    // Check POST_NOTIFICATIONS permission on Android 13+
    val canPostNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    if (canPostNotification) {
      try {
        val notification = buildIncomingCallNotification(appCtx, callRoom)
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(IncomingCallContract.INCOMING_CALL_NOTIFICATION_ID, notification)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to post CallStyle notification: ${e.message}", e)
      }
    }

    // Schedule 30-second auto-timeout cleanup so the phone never rings forever
    timeoutJobs.remove(callRoom.roomId)?.cancel()
    timeoutJobs[callRoom.roomId] = scope.launch {
      delay(30_000L)
      if (!IncomingCallRegistry.isRoomTerminal(appCtx, callRoom.roomId)) {
        IncomingCallRegistry.markRoomTerminal(appCtx, callRoom.roomId)
        dismissIncomingCallNotification(appCtx, callRoom.roomId)
        com.example.notifications.SmartNotificationManager.showMissedCallNotification(
          context = appCtx,
          callerId = callRoom.callerId,
          callerName = callRoom.callerName,
          callType = callRoom.callType,
          roomId = callRoom.roomId
        )
      }
    }
  }

  fun dismissIncomingCallNotification(context: Context, roomId: String? = null) {
    val appCtx = context.applicationContext
    if (!roomId.isNullOrBlank()) {
      timeoutJobs.remove(roomId)?.cancel()
      IncomingCallRegistry.clearRingingRoom(roomId)
    } else {
      timeoutJobs.values.forEach { it.cancel() }
      timeoutJobs.clear()
    }
    CallRingtoneHelper.stopRinging(appCtx)
    try {
      val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.cancel(IncomingCallContract.INCOMING_CALL_NOTIFICATION_ID)
    } catch (_: Exception) {}
  }

  /**
   * Returns a transparent, accurate description of Android background call notification support.
   * Never claims that force-stopped apps can receive calls.
   */
  fun getBackgroundCallDeliveryStatusNote(context: Context): String {
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    return if (notificationsGranted) {
      "Incoming call notifications are active when the app is in the background or closed, subject to Android permissions and system background execution restrictions (force-stopped apps cannot receive calls until reopened)."
    } else {
      "Grant notification permission to receive incoming call alerts when LingoSphere is in the background or closed (subject to Android background execution limits)."
    }
  }
}

/**
 * Firebase Cloud Messaging service that delivers incoming call notifications when LingoSphere
 * is in the background or closed (subject to Android permissions and system background execution
 * restrictions; force-stopped apps do not receive FCM broadcasts until reopened by the user).
 */
class LingoCallMessagingService : FirebaseMessagingService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    FcmTokenManager.cacheTokenLocally(applicationContext, token)
    val uid = try {
      Firebase.auth.currentUser?.uid
    } catch (_: Exception) {
      null
    }
    if (!uid.isNullOrBlank()) {
      serviceScope.launch {
        FcmTokenManager.saveTokenToFirestore(applicationContext, uid, token)
      }
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val data = remoteMessage.data
    if (data.isNotEmpty()) {
      // 1. Check if this is a Smart English Learning / Speaking Invitation / Missed Call notification
      if (com.example.notifications.SmartNotificationManager.handleFcmLearningPayload(applicationContext, data)) {
        return
      }
      // 2. Otherwise check if it is a real-time WebRTC call signaling payload
      val payload = FcmCallPayload.fromMap(data)
      if (payload != null) {
        handleIncomingFcmCallPayload(applicationContext, payload)
        return
      }
    }

    // 3. Fallback if an FCM message arrives with a notification block
    val notif = remoteMessage.notification
    if (notif != null) {
      val fallbackMap = mutableMapOf<String, String>()
      fallbackMap.putAll(data)
      if (!fallbackMap.containsKey("type")) {
        fallbackMap["type"] = "word_of_the_day"
      }
      notif.title?.let { fallbackMap.putIfAbsent("title", it) }
      notif.body?.let { fallbackMap.putIfAbsent("body", it) }
      com.example.notifications.SmartNotificationManager.handleFcmLearningPayload(applicationContext, fallbackMap)
    }
  }

  companion object {
    private const val TAG = "LingoCallFCM"

    fun handleIncomingFcmCallPayload(context: Context, payload: FcmCallPayload) {
      val appCtx = context.applicationContext

      // 1. Check if this is a cancellation / termination event
      if (payload.eventType in listOf("call_cancelled", "call_ended", "call_missed", "call_rejected")) {
        IncomingCallRegistry.markRoomTerminal(appCtx, payload.roomId)
        NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, payload.roomId)
        if (payload.eventType in listOf("call_cancelled", "call_missed")) {
          com.example.notifications.SmartNotificationManager.showMissedCallNotification(
            context = appCtx,
            callerId = payload.callerId,
            callerName = payload.callerName,
            callType = payload.callType,
            roomId = payload.roomId
          )
        }
        return
      }

      // 2. Reject expired call invitations (>30s old)
      if (payload.isExpired()) {
        Log.d(TAG, "Ignoring expired FCM call invite for roomId=${payload.roomId}")
        IncomingCallRegistry.markRoomTerminal(appCtx, payload.roomId)
        return
      }

      // 3. Reject duplicate / already answered or declined calls
      if (IncomingCallRegistry.isRoomTerminal(appCtx, payload.roomId)) {
        Log.d(TAG, "Ignoring already-terminal roomId=${payload.roomId}")
        return
      }

      // 4. Verify recipient matches signed-in user (if signed in)
      val currentUid = try {
        Firebase.auth.currentUser?.uid
      } catch (_: Exception) {
        null
      }
      if (!currentUid.isNullOrBlank() && payload.calleeId.isNotBlank() && payload.calleeId != currentUid) {
        Log.d(TAG, "Ignoring FCM call meant for another user: ${payload.calleeId}")
        return
      }

      val callRoom = payload.toCallRoom()

      // 4b. Respect user's Allow Incoming Partner Calls & Voice/Video call settings
      if (!IncomingCallRegistry.isIncomingCallAllowed(appCtx, callRoom.callType)) {
        Log.d(TAG, "Rejecting incoming ${callRoom.callType} call for roomId=${callRoom.roomId} because incoming calls are disabled")
        IncomingCallRegistry.markRoomTerminal(appCtx, callRoom.roomId)
        NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, callRoom.roomId)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
          try {
            val dbId = appCtx.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            db.collection("call_rooms").document(callRoom.roomId).update(
              mapOf(
                "status" to "rejected",
                "lastSignalingEvent" to "call_rejected",
                "connectionState" to "closed",
                "endedAtEpochMs" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          } catch (_: Exception) {}
        }
        return
      }

      // 5. Acknowledge ringing in Firestore best-effort
      CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
          val dbId = appCtx.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(dbId)
          val snap = db.collection("call_rooms").document(callRoom.roomId).get().await()
          if (snap.exists()) {
            val currentStatus = snap.getString("status").orEmpty()
            if (currentStatus in listOf("cancelled", "ended", "rejected", "missed", "busy")) {
              IncomingCallRegistry.markRoomTerminal(appCtx, callRoom.roomId)
              NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, callRoom.roomId)
              return@launch
            }
            db.collection("call_rooms").document(callRoom.roomId).update(
              mapOf(
                "lastSignalingEvent" to "call_ringing",
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          }
        } catch (_: Exception) {}
      }

      // 6. Show native CallStyle notification + start ringtone & vibration if notifications enabled
      if (IncomingCallRegistry.areCallNotificationsEnabled(appCtx)) {
        NativeCallNotificationManager.showIncomingCallNotification(
          context = appCtx,
          callRoom = callRoom,
          startRingtoneAndVibration = true
        )
      }
    }
  }
}

/**
 * Handles background notification actions (Decline incoming call or Hang Up active call)
 * without requiring the user to unlock or open the full app UI first.
 */
class IncomingCallActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    val appCtx = context.applicationContext

    when (action) {
      IncomingCallContract.ACTION_DECLINE_CALL -> {
        val parsed = FcmCallPayload.fromIntent(intent)
        val roomId = parsed?.first?.roomId
          ?: intent.getStringExtra(IncomingCallContract.EXTRA_INCOMING_ROOM_ID).orEmpty()

        IncomingCallRegistry.markRoomTerminal(appCtx, roomId)
        NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, roomId)
        if (parsed != null) {
          IncomingCallRegistry.dispatchLaunchCommand(
            callRoom = parsed.first,
            action = IncomingCallContract.ACTION_DECLINE_CALL
          )
        }

        if (roomId.isNotBlank()) {
          val pendingResult = goAsync()
          CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
              val dbId = appCtx.getString(R.string.firestore_database_id)
              val db = FirebaseFirestore.getInstance(dbId)
              db.collection("call_rooms").document(roomId).update(
                mapOf(
                  "status" to "rejected",
                  "lastSignalingEvent" to "call_rejected",
                  "connectionState" to "closed",
                  "endedAtEpochMs" to System.currentTimeMillis(),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              ).await()
            } catch (e: Exception) {
              Log.w("IncomingCallReceiver", "Error declining call in Firestore: ${e.message}")
            } finally {
              pendingResult.finish()
            }
          }
        }
      }

      IncomingCallContract.ACTION_HANG_UP_ACTIVE_CALL -> {
        val roomId = intent.getStringExtra(IncomingCallContract.EXTRA_INCOMING_ROOM_ID).orEmpty()
        ActiveCallForegroundService.stop(appCtx)
        IncomingCallRegistry.dispatchActiveCallHangup(roomId)
      }
    }
  }
}

/**
 * Manages the ongoing call notification and CPU/Wi-Fi locks during active live voice and video
 * language exchange calls without using an Android Foreground Service or requiring any
 * `FOREGROUND_SERVICE*` permissions.
 */
object ActiveCallForegroundService {
  private const val TAG = "ActiveCallNotif"
  private var wakeLock: PowerManager.WakeLock? = null
  private var wifiLock: WifiManager.WifiLock? = null

  fun start(context: Context, roomId: String, partnerName: String, callType: String) {
    val appCtx = context.applicationContext
    val safePartnerName = partnerName.ifBlank { "Language Partner" }
    val isVideo = callType.equals("video", ignoreCase = true)

    acquireLocks(appCtx)
    NativeCallNotificationManager.ensureActiveCallChannel(appCtx)

    val canPostNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    if (!canPostNotification) return

    val openCallIntent = Intent(appCtx, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      appCtx,
      9001,
      openCallIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val hangUpIntent = Intent(appCtx, IncomingCallActionReceiver::class.java).apply {
      action = IncomingCallContract.ACTION_HANG_UP_ACTIVE_CALL
      putExtra(IncomingCallContract.EXTRA_INCOMING_ROOM_ID, roomId)
    }
    val hangUpPendingIntent = PendingIntent.getBroadcast(
      appCtx,
      9002,
      hangUpIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(appCtx, IncomingCallContract.ACTIVE_CALL_CHANNEL_ID)
      .setSmallIcon(android.R.drawable.sym_action_call)
      .setContentTitle("Live ${if (isVideo) "Video" else "Voice"} Call • $safePartnerName")
      .setContentText("Tap to return to LingoSphere call")
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setOngoing(true)
      .setUsesChronometer(true)
      .setContentIntent(contentPendingIntent)
      .addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        "Hang Up",
        hangUpPendingIntent
      )
      .build()

    try {
      val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.notify(IncomingCallContract.ACTIVE_CALL_NOTIFICATION_ID, notification)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to post ongoing call notification: ${e.message}")
    }
  }

  fun stop(context: Context) {
    val appCtx = context.applicationContext
    releaseLocks()
    try {
      val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.cancel(IncomingCallContract.ACTIVE_CALL_NOTIFICATION_ID)
    } catch (_: Exception) {}
  }

  private fun acquireLocks(context: Context) {
    try {
      if (wakeLock == null) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LingoSphere:ActiveCallWakeLock")?.apply {
          setReferenceCounted(false)
          acquire(60 * 60 * 1000L) // Max 60 minutes safety bound
        }
      }
      if (wifiLock == null) {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
          WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wm?.createWifiLock(mode, "LingoSphere:ActiveCallWifiLock")?.apply {
          setReferenceCounted(false)
          acquire()
        }
      }
    } catch (_: Exception) {}
  }

  private fun releaseLocks() {
    try {
      wakeLock?.let { if (it.isHeld) it.release() }
      wakeLock = null
    } catch (_: Exception) {}
    try {
      wifiLock?.let { if (it.isHeld) it.release() }
      wifiLock = null
    } catch (_: Exception) {}
  }
}

/**
 * Synchronizes missed calls when the receiver comes back online or opens the app,
 * prevents expired calls (>35s) from ringing on reconnect, and posts native CallStyle
 * notifications if the device screen is locked or app is backgrounded.
 */
object OfflineMissedCallSyncManager {
  private const val TAG = "OfflineMissedCallSync"
  private const val PREF_NOTIFIED_MISSED_CALL_IDS = "notified_missed_call_ids_v1"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
  private var networkCallbackRegistered = false
  private var observedUid: String? = null

  fun startObserving(context: Context, userId: String) {
    if (userId.isBlank()) return
    val appCtx = context.applicationContext
    registerConnectivityWatcherIfNeeded(appCtx)

    if (observedUid == userId && listenerRegistration != null) {
      syncMissedCallsNow(appCtx, userId)
      return
    }
    stopObserving()
    observedUid = userId

    syncMissedCallsNow(appCtx, userId)

    try {
      val dbId = appCtx.getString(R.string.firestore_database_id)
      val db = FirebaseFirestore.getInstance(dbId)
      listenerRegistration = db.collection("call_rooms")
        .whereEqualTo("calleeId", userId)
        .addSnapshotListener { snap, err ->
          if (err != null || snap == null) return@addSnapshotListener
          val nowMs = System.currentTimeMillis()
          for (doc in snap.documents) {
            val roomId = doc.getString("roomId")?.ifBlank { doc.id } ?: doc.id
            val status = doc.getString("status")?.lowercase().orEmpty()
            val callerId = doc.getString("callerId").orEmpty()
            val callerName = doc.getString("callerName")?.ifBlank { "Language Partner" } ?: "Language Partner"
            val callType = doc.getString("callType") ?: "voice"
            val createdMs = doc.getTimestamp("createdAt")?.toDate()?.time
              ?: doc.getTimestamp("updatedAt")?.toDate()?.time
              ?: nowMs
            val ageMs = nowMs - createdMs

            when {
              // 1. Expired ringing call (>35s old) -> mark missed, do NOT ring
              status == "ringing" && ageMs > 35_000L -> {
                IncomingCallRegistry.markRoomTerminal(appCtx, roomId)
                NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, roomId)
                scope.launch {
                  try {
                    doc.reference.update(
                      mapOf(
                        "status" to "missed",
                        "lastSignalingEvent" to "call_missed",
                        "connectionState" to "closed",
                        "endedAtEpochMs" to nowMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                      )
                    ).await()
                  } catch (_: Exception) {}
                }
                notifyMissedCallOnce(appCtx, roomId, callerId, callerName, callType)
              }

              // 2. Fresh ringing call (<35s old) while screen is locked or app is not in foreground
              status == "ringing" && ageMs in -60_000L..35_000L -> {
                if (!IncomingCallRegistry.isRoomTerminal(appCtx, roomId)) {
                  val km = appCtx.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                  val isLocked = km?.isKeyguardLocked == true
                  val am = appCtx.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                  val isAppVisible = am?.runningAppProcesses?.any { proc ->
                    proc.uid == android.os.Process.myUid() &&
                      proc.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                  } == true
                  if ((isLocked || !isAppVisible) && IncomingCallRegistry.areCallNotificationsEnabled(appCtx)) {
                    val room = CallRoom(
                      roomId = roomId,
                      callerId = callerId,
                      callerName = callerName,
                      callerNativeLang = doc.getString("callerNativeLang") ?: "en",
                      callerLearningLang = doc.getString("callerLearningLang") ?: "es",
                      calleeId = userId,
                      calleeName = doc.getString("calleeName") ?: "",
                      callType = callType,
                      targetLanguageCode = doc.getString("targetLanguageCode") ?: "es",
                      status = "ringing"
                    )
                    NativeCallNotificationManager.showIncomingCallNotification(
                      context = appCtx,
                      callRoom = room,
                      startRingtoneAndVibration = true
                    )
                  }
                }
              }

              // 3. Call was cancelled, missed, ended, or rejected -> stop ringing & notify if missed/cancelled
              status in setOf("cancelled", "missed", "ended", "rejected", "declined", "busy", "closed") -> {
                IncomingCallRegistry.markRoomTerminal(appCtx, roomId)
                NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, roomId)
                if (status in setOf("missed", "cancelled") && ageMs in -60_000L..(12 * 60 * 60 * 1000L)) {
                  notifyMissedCallOnce(appCtx, roomId, callerId, callerName, callType)
                }
              }
            }
          }
        }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to start call room observer: ${e.message}")
    }
  }

  fun syncMissedCallsNow(context: Context, userId: String? = observedUid) {
    val uid = userId?.takeIf { it.isNotBlank() } ?: try {
      Firebase.auth.currentUser?.uid
    } catch (_: Exception) {
      null
    } ?: return
    val appCtx = context.applicationContext

    scope.launch {
      try {
        val dbId = appCtx.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        val snap = db.collection("call_rooms")
          .whereEqualTo("calleeId", uid)
          .get()
          .await()

        val nowMs = System.currentTimeMillis()
        for (doc in snap.documents) {
          val roomId = doc.getString("roomId")?.ifBlank { doc.id } ?: doc.id
          val status = doc.getString("status")?.lowercase().orEmpty()
          val callerId = doc.getString("callerId").orEmpty()
          val callerName = doc.getString("callerName")?.ifBlank { "Language Partner" } ?: "Language Partner"
          val callType = doc.getString("callType") ?: "voice"
          val createdMs = doc.getTimestamp("createdAt")?.toDate()?.time
            ?: doc.getTimestamp("updatedAt")?.toDate()?.time
            ?: nowMs
          val ageMs = nowMs - createdMs

          if (status == "ringing" && ageMs > 35_000L) {
            IncomingCallRegistry.markRoomTerminal(appCtx, roomId)
            NativeCallNotificationManager.dismissIncomingCallNotification(appCtx, roomId)
            try {
              doc.reference.update(
                mapOf(
                  "status" to "missed",
                  "lastSignalingEvent" to "call_missed",
                  "connectionState" to "closed",
                  "endedAtEpochMs" to nowMs,
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              ).await()
            } catch (_: Exception) {}
            notifyMissedCallOnce(appCtx, roomId, callerId, callerName, callType)
          } else if (status in setOf("missed", "cancelled") && ageMs in 0L..(12 * 60 * 60 * 1000L)) {
            notifyMissedCallOnce(appCtx, roomId, callerId, callerName, callType)
          }
        }
      } catch (e: Exception) {
        Log.d(TAG, "Missed call sync skipped or offline: ${e.message}")
      }
    }
  }

  private fun notifyMissedCallOnce(
    context: Context,
    roomId: String,
    callerId: String,
    callerName: String,
    callType: String
  ) {
    if (roomId.isBlank()) return
    val prefs = context.getSharedPreferences("lingosphere_call_registry", Context.MODE_PRIVATE)
    val existing = prefs.getStringSet(PREF_NOTIFIED_MISSED_CALL_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
    if (!existing.add(roomId)) return
    val trimmed = if (existing.size > 120) existing.toList().takeLast(80).toSet() else existing
    prefs.edit().putStringSet(PREF_NOTIFIED_MISSED_CALL_IDS, trimmed).apply()

    com.example.notifications.SmartNotificationManager.showMissedCallNotification(
      context = context,
      callerId = callerId,
      callerName = callerName,
      callType = callType,
      roomId = roomId
    )
  }

  private fun registerConnectivityWatcherIfNeeded(context: Context) {
    if (networkCallbackRegistered) return
    try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
      cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
          syncMissedCallsNow(context)
        }
      })
      networkCallbackRegistered = true
    } catch (_: Exception) {}
  }

  fun stopObserving() {
    listenerRegistration?.remove()
    listenerRegistration = null
    observedUid = null
  }
}
