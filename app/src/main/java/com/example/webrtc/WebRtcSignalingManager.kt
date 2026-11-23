package com.example.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.data.model.CallRoom
import com.example.ui.LingoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Audio output route options during a live voice/video call.
 */
enum class CallAudioRoute(val label: String) {
  SPEAKER("Speaker"),
  EARPIECE("Earpiece"),
  BLUETOOTH("Bluetooth")
}

/**
 * Standard WebRTC ICE Server Configuration (STUN / Cloudflare TURN).
 */
data class IceServerConfig(
  val urls: List<String>,
  val username: String? = null,
  val credential: String? = null
)

/**
 * Real-Time WebRTC Call & Signaling Lifecycle States.
 */
enum class WebRtcState(val displayLabel: String) {
  PERMISSION_REQUIRED("Permission Required"),
  INITIALIZING("Initializing WebRTC Media..."),
  WAITING_FOR_PARTNER("Waiting for partner to join..."),
  RINGING("Ringing..."),
  NEGOTIATING("Negotiating SDP & ICE..."),
  CONNECTING("Connecting WebRTC Media..."),
  CONNECTED("Connected"),
  RECONNECTING("Reconnecting (ICE Restart)..."),
  MISSED("No Answer (Timed Out)"),
  REJECTED("Call Declined"),
  BUSY("User Busy on Another Call"),
  CANCELLED("Call Cancelled"),
  FAILED("Connection Failed"),
  CLOSED("Call Ended")
}

/**
 * Helper to play incoming ringtone & vibration (respecting device ringerMode) or outgoing ringback tone,
 * and release audio resources cleanly when ringing stops across both background FCM and foreground UI.
 */
class CallRingtoneHelper(private val context: Context) {
  private var ringtone: Ringtone? = null
  private var toneGenerator: ToneGenerator? = null
  @Suppress("DEPRECATION")
  private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  fun startIncomingRingtone() {
    stopAll()
    try {
      val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
      if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
        return
      }

      if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (uri != null) {
          ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
              isLooping = true
            }
            play()
          }
        }
      }

      if (ringerMode == AudioManager.RINGER_MODE_NORMAL || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0L, 600L, 800L, 600L, 800L), 0)
          )
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(longArrayOf(0L, 600L, 800L, 600L, 800L), 0)
        }
      }
    } catch (e: Exception) {
      Log.w("LingoWebRTC", "Unable to start incoming ringtone: ${e.message}")
    }
  }

  fun startOutgoingRingbackTone() {
    stopAll()
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 55).apply {
        startTone(ToneGenerator.TONE_SUP_RINGTONE)
      }
    } catch (e: Exception) {
      Log.w("LingoWebRTC", "Unable to start outgoing ringback tone: ${e.message}")
    }
  }

  fun stopAll() {
    try {
      ringtone?.stop()
    } catch (_: Exception) {}
    ringtone = null

    try {
      toneGenerator?.stopTone()
      toneGenerator?.release()
    } catch (_: Exception) {}
    toneGenerator = null

    try {
      vibrator?.cancel()
    } catch (_: Exception) {}

    try {
      sharedRingtoneInstance?.let { shared ->
        if (shared !== this) {
          shared.ringtone?.stop()
          shared.ringtone = null
          shared.vibrator?.cancel()
        }
      }
    } catch (_: Exception) {}
  }

  companion object {
    @Volatile
    private var sharedRingtoneInstance: CallRingtoneHelper? = null

    fun startRinging(context: Context) {
      val appCtx = context.applicationContext
      val helper = sharedRingtoneInstance ?: CallRingtoneHelper(appCtx).also {
        sharedRingtoneInstance = it
      }
      helper.startIncomingRingtone()
    }

    fun stopRinging(context: Context? = null) {
      try {
        sharedRingtoneInstance?.stopAll()
      } catch (_: Exception) {}
      if (context != null) {
        try {
          @Suppress("DEPRECATION")
          val vib = context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
          vib?.cancel()
        } catch (_: Exception) {}
      }
    }
  }
}

/**
 * Production WebRTC Signaling and Media Session Manager.
 * Coordinates real RTCPeerConnection media streams, getUserMedia capture,
 * Cloudflare STUN + short-lived Cloudflare Realtime TURN ICE candidate trickle exchange,
 * sequential SDP offer/answer negotiation, 30-second ringing timeout, and ICE restart recovery over Firebase Firestore.
 */
class WebRtcSignalingManager(
  private val context: Context,
  private val roomId: String,
  private val isCaller: Boolean,
  private val isVideo: Boolean,
  private val viewModel: LingoViewModel,
  private val coroutineScope: CoroutineScope
) {
  companion object {
    private const val TAG = "LingoWebRTC"
    const val CALL_RING_TIMEOUT_MS = 30_000L
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
  private val ringtoneHelper = CallRingtoneHelper(context)
  private var audioFocusRequest: AudioFocusRequest? = null
  private var audioDeviceCallback: AudioDeviceCallback? = null
  private var networkCallback: ConnectivityManager.NetworkCallback? = null
  private var lastActiveNetworkHandle: Long? = null

  private val _webrtcState = MutableStateFlow(WebRtcState.INITIALIZING)
  val webrtcState = _webrtcState.asStateFlow()

  private val _isAudioMuted = MutableStateFlow(false)
  val isAudioMuted = _isAudioMuted.asStateFlow()

  private val _isVideoMuted = MutableStateFlow(!isVideo)
  val isVideoMuted = _isVideoMuted.asStateFlow()

  // Default speakerphone to true so both voice and video calls are immediately audible out loud
  private val _isSpeakerphoneOn = MutableStateFlow(true)
  val isSpeakerphoneOn = _isSpeakerphoneOn.asStateFlow()

  private val _activeAudioRoute = MutableStateFlow(CallAudioRoute.SPEAKER)
  val activeAudioRoute = _activeAudioRoute.asStateFlow()

  private val _isBluetoothAvailable = MutableStateFlow(false)
  val isBluetoothAvailable = _isBluetoothAvailable.asStateFlow()

  private val _useFrontCamera = MutableStateFlow(true)
  val useFrontCamera = _useFrontCamera.asStateFlow()

  private val _isLocalVideoMirrored = MutableStateFlow(true)
  val isLocalVideoMirrored = _isLocalVideoMirrored.asStateFlow()

  private val _areViewsSwapped = MutableStateFlow(false)
  val areViewsSwapped = _areViewsSwapped.asStateFlow()

  private val _isSelfViewHidden = MutableStateFlow(false)
  val isSelfViewHidden = _isSelfViewHidden.asStateFlow()

  private val _connectionLatencyMs = MutableStateFlow(0)
  val connectionLatencyMs = _connectionLatencyMs.asStateFlow()

  private val _activeCandidateType = MutableStateFlow("stun")
  val activeCandidateType = _activeCandidateType.asStateFlow()

  private val _turnProviderStatus = MutableStateFlow(
    if (CloudflareTurnService.isAnyTurnConfigured()) "TURN Relay Ready" else "Direct STUN"
  )
  val turnProviderStatus = _turnProviderStatus.asStateFlow()

  private val _mediaErrorMessage = MutableStateFlow<String?>(null)
  val mediaErrorMessage = _mediaErrorMessage.asStateFlow()

  private val _isRemoteStreamAvailable = MutableStateFlow(false)
  val isRemoteStreamAvailable = _isRemoteStreamAvailable.asStateFlow()

  private val _isRemoteAudioAvailable = MutableStateFlow(false)
  val isRemoteAudioAvailable = _isRemoteAudioAvailable.asStateFlow()

  private var observeRoomJob: Job? = null
  private var ringTimeoutJob: Job? = null
  private var disconnectRecoveryJob: Job? = null
  private var offerRetryJob: Job? = null

  private var webViewRef: WebView? = null
  private var isWebEngineReady = false
  private var isSessionStarted = false
  private var hasStartedMediaInit = false
  private var isLocalMediaReady = false
  private var offerCreated = false
  private var hasAutoRetriedFailedIce = false
  private var lastProcessedOfferSdp: String? = null
  private var lastProcessedAnswerSdp: String? = null
  private val processedRemoteCandidates = mutableSetOf<String>()
  private var latestRoomSnapshot: CallRoom? = null
  private var resolvedIceServers: List<IceServerConfig> = listOf(
    IceServerConfig(urls = CloudflareTurnService.defaultStunServers)
  )

  /**
   * True when Cloudflare Realtime TURN or fallback TURN relay is configured.
   */
  val isTurnConfigured: Boolean
    get() = CloudflareTurnService.isAnyTurnConfigured()

  fun buildIceServersJson(servers: List<IceServerConfig> = resolvedIceServers): String {
    val array = JSONArray()
    for (server in servers) {
      val obj = JSONObject()
      val urlsArray = JSONArray()
      server.urls.forEach { urlsArray.put(it) }
      obj.put("urls", urlsArray)
      if (!server.username.isNullOrBlank()) {
        obj.put("username", server.username)
      }
      if (!server.credential.isNullOrBlank()) {
        obj.put("credential", server.credential)
      }
      array.put(obj)
    }
    return array.toString()
  }

  private fun refreshBluetoothAvailability() {
    try {
      val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager?.availableCommunicationDevices.orEmpty()
      } else {
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList().orEmpty()
      }
      val hasBt = devices.any {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
          it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
          (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
      }
      _isBluetoothAvailable.value = hasBt
    } catch (_: Exception) {
      _isBluetoothAvailable.value = false
    }
  }

  private fun registerAudioAndNetworkWatchers() {
    try {
      refreshBluetoothAvailability()
      if (audioDeviceCallback == null) {
        val cb = object : AudioDeviceCallback() {
          override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshBluetoothAvailability()
            if (_isBluetoothAvailable.value && _activeAudioRoute.value != CallAudioRoute.BLUETOOTH) {
              selectAudioRoute(CallAudioRoute.BLUETOOTH)
            }
          }

          override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshBluetoothAvailability()
            if (!_isBluetoothAvailable.value && _activeAudioRoute.value == CallAudioRoute.BLUETOOTH) {
              selectAudioRoute(CallAudioRoute.SPEAKER)
            }
          }
        }
        audioDeviceCallback = cb
        audioManager?.registerAudioDeviceCallback(cb, null)
      }
    } catch (e: Exception) {
      Log.w(TAG, "AudioDeviceCallback registration warning: ${e.message}")
    }

    try {
      if (networkCallback == null) {
        lastActiveNetworkHandle = connectivityManager?.activeNetwork?.networkHandle
        val netCb = object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            val prevHandle = lastActiveNetworkHandle
            val newHandle = network.networkHandle
            lastActiveNetworkHandle = newHandle
            if (prevHandle != null && prevHandle != newHandle && isSessionStarted && isLocalMediaReady) {
              Log.i(TAG, "Active network switched ($prevHandle -> $newHandle); auto-renegotiating WebRTC ICE")
              coroutineScope.launch(Dispatchers.Main) {
                if (_webrtcState.value == WebRtcState.CONNECTED ||
                  _webrtcState.value == WebRtcState.CONNECTING ||
                  _webrtcState.value == WebRtcState.FAILED
                ) {
                  triggerConnectionRecovery()
                }
              }
            }
          }
        }
        networkCallback = netCb
        connectivityManager?.registerDefaultNetworkCallback(netCb)
      }
    } catch (e: Exception) {
      Log.w(TAG, "NetworkCallback registration warning: ${e.message}")
    }
  }

  private fun unregisterAudioAndNetworkWatchers() {
    try {
      audioDeviceCallback?.let { audioManager?.unregisterAudioDeviceCallback(it) }
    } catch (_: Exception) {}
    audioDeviceCallback = null

    try {
      networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
    } catch (_: Exception) {}
    networkCallback = null
  }

  private fun configureAudioHardware(enableSpeaker: Boolean) {
    try {
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
      audioManager?.isMicrophoneMute = _isAudioMuted.value

      // Ensure voice call and media volumes are audible
      audioManager?.let { am ->
        for (stream in intArrayOf(AudioManager.STREAM_VOICE_CALL, AudioManager.STREAM_MUSIC)) {
          val curVol = am.getStreamVolume(stream)
          val maxVol = am.getStreamMaxVolume(stream)
          if (curVol == 0 && maxVol > 0) {
            am.setStreamVolume(stream, (maxVol * 0.65).toInt().coerceAtLeast(1), 0)
          }
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .build()
        audioFocusRequest = req
        audioManager?.requestAudioFocus(req)
      }
      refreshBluetoothAvailability()
      if (_isBluetoothAvailable.value && _activeAudioRoute.value == CallAudioRoute.BLUETOOTH) {
        selectAudioRoute(CallAudioRoute.BLUETOOTH)
      } else {
        selectAudioRoute(if (enableSpeaker) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE)
      }

      if (AcousticEchoCanceler.isAvailable()) {
        Log.d(TAG, "Hardware AcousticEchoCanceler supported on device")
      }
      if (NoiseSuppressor.isAvailable()) {
        Log.d(TAG, "Hardware NoiseSuppressor supported on device")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to configure hardware audio routing: ${e.message}")
    }
  }

  fun selectAudioRoute(route: CallAudioRoute) {
    _activeAudioRoute.value = route
    _isSpeakerphoneOn.value = (route == CallAudioRoute.SPEAKER)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val devices = audioManager?.availableCommunicationDevices.orEmpty()
        val targetDevice = when (route) {
          CallAudioRoute.BLUETOOTH -> devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
              it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
              it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
          }
          CallAudioRoute.EARPIECE -> devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ||
              it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
              it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
          }
          CallAudioRoute.SPEAKER -> devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
          }
        }
        if (targetDevice != null) {
          audioManager?.setCommunicationDevice(targetDevice)
          return
        }
      }

      @Suppress("DEPRECATION")
      when (route) {
        CallAudioRoute.BLUETOOTH -> {
          audioManager?.isSpeakerphoneOn = false
          audioManager?.startBluetoothSco()
          audioManager?.isBluetoothScoOn = true
        }
        CallAudioRoute.EARPIECE -> {
          audioManager?.stopBluetoothSco()
          audioManager?.isBluetoothScoOn = false
          audioManager?.isSpeakerphoneOn = false
        }
        CallAudioRoute.SPEAKER -> {
          audioManager?.stopBluetoothSco()
          audioManager?.isBluetoothScoOn = false
          audioManager?.isSpeakerphoneOn = true
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Audio route switch ($route) failed: ${e.message}")
    }
  }

  fun cycleAudioRoute() {
    refreshBluetoothAvailability()
    val nextRoute = when (_activeAudioRoute.value) {
      CallAudioRoute.SPEAKER -> if (_isBluetoothAvailable.value) CallAudioRoute.BLUETOOTH else CallAudioRoute.EARPIECE
      CallAudioRoute.BLUETOOTH -> CallAudioRoute.EARPIECE
      CallAudioRoute.EARPIECE -> CallAudioRoute.SPEAKER
    }
    selectAudioRoute(nextRoute)
  }

  private fun setSpeakerphoneHardware(enable: Boolean) {
    selectAudioRoute(if (enable) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE)
  }

  fun attachWebView(webView: WebView) {
    webViewRef = webView
  }

  fun detachWebView(webView: WebView? = null) {
    if (webView == null || webViewRef === webView) {
      webViewRef = null
    }
  }

  private fun evalJs(script: String) {
    val wv = webViewRef ?: return
    wv.post {
      try {
        wv.evaluateJavascript(script, null)
      } catch (e: Exception) {
        Log.e(TAG, "JS evaluation error: ${e.message}")
      }
    }
  }

  /**
   * Starts the call session once microphone (and camera for video calls) permissions are granted.
   */
  fun startSession() {
    Log.i(
      TAG,
      "Starting WebRTC session: roomId=$roomId, isCaller=$isCaller, isVideo=$isVideo, turnConfigured=$isTurnConfigured"
    )
    isSessionStarted = true
    _mediaErrorMessage.value = null
    _webrtcState.value = WebRtcState.INITIALIZING
    com.example.presence.PresenceManager.setInCallStatus(true)
    NativeCallNotificationManager.dismissIncomingCallNotification(context, roomId)
    registerAudioAndNetworkWatchers()
    configureAudioHardware(enableSpeaker = _isSpeakerphoneOn.value)

    maybeResolveTurnAndInitWebMedia(forceRefreshTurn = false)

    // Start 30-second ringing timeout if caller is ringing a partner
    startRingingTimeoutWatcher()

    // Observe remote signaling updates in real time via Firestore
    observeRoomJob?.cancel()
    observeRoomJob = coroutineScope.launch {
      var hasSeenRoomOnce = false
      viewModel.observeCallRoom(roomId).collect { room ->
        if (room == null) {
          if (hasSeenRoomOnce && isSessionStarted) {
            Log.i(TAG, "Call room document removed; terminating session for roomId=$roomId")
            ringtoneHelper.stopAll()
            ringTimeoutJob?.cancel()
            offerRetryJob?.cancel()
            disconnectRecoveryJob?.cancel()
            _webrtcState.value = WebRtcState.CLOSED
          }
          return@collect
        }
        hasSeenRoomOnce = true
        latestRoomSnapshot = room
        handleRoomSignalingUpdate(room)
      }
    }
  }

  private fun maybeResolveTurnAndInitWebMedia(forceRefreshTurn: Boolean) {
    if (!isSessionStarted || !isWebEngineReady || hasStartedMediaInit) return
    hasStartedMediaInit = true

    coroutineScope.launch {
      val resolution = CloudflareTurnService.resolveIceServers(forceRefresh = forceRefreshTurn)
      resolvedIceServers = resolution.iceServers
      _turnProviderStatus.value = resolution.providerLabel
      Log.i(
        TAG,
        "Resolved ICE servers (${resolution.providerLabel}, turnActive=${resolution.isTurnRelayActive}, count=${resolution.iceServers.size})"
      )

      val iceJson = JSONObject.quote(buildIceServersJson(resolution.iceServers))
      val facing = if (_useFrontCamera.value) "user" else "environment"
      val wantVideo = !_isVideoMuted.value
      evalJs("window.startWebRtcMedia($iceJson, $isCaller, $wantVideo, '$facing');")
    }
  }

  private fun startRingingTimeoutWatcher() {
    ringTimeoutJob?.cancel()
    ringTimeoutJob = coroutineScope.launch {
      delay(CALL_RING_TIMEOUT_MS)
      val snap = latestRoomSnapshot
      if (snap?.matchMode == "speaking_room") return@launch
      val current = _webrtcState.value
      val status = snap?.status ?: "ringing"
      if (
        (current == WebRtcState.RINGING || current == WebRtcState.WAITING_FOR_PARTNER || current == WebRtcState.INITIALIZING) &&
        (status == "ringing" || status == "waiting")
      ) {
        Log.w(TAG, "Call timed out after 30s without answer: roomId=$roomId")
        ringtoneHelper.stopAll()
        _webrtcState.value = WebRtcState.MISSED
        viewModel.markCallMissed(roomId)
      }
    }
  }

  private fun scheduleCallerOfferRetryIfNeeded(delayMs: Long = 16_000L) {
    offerRetryJob?.cancel()
    if (!isCaller) return
    offerRetryJob = coroutineScope.launch {
      delay(delayMs)
      val room = latestRoomSnapshot ?: return@launch
      if (room.isTerminalStatus()) return@launch
      if (
        isLocalMediaReady &&
        (room.status == "accepted" || room.status == "connected") &&
        room.sdpAnswer.isNullOrBlank() &&
        _webrtcState.value != WebRtcState.CONNECTED &&
        _webrtcState.value != WebRtcState.CONNECTING
      ) {
        Log.i(TAG, "No SDP answer received after ${delayMs}ms; re-sending WebRTC SDP Offer for roomId=$roomId")
        lastProcessedAnswerSdp = null
        processedRemoteCandidates.clear()
        evalJs("window.createAndSendOffer(true);")
      }
    }
  }

  private fun handleRoomSignalingUpdate(room: CallRoom) {
    Log.d(
      TAG,
      "Signaling update: roomId=${room.roomId}, status=${room.status}, event=${room.lastSignalingEvent}, conn=${room.connectionState}"
    )

    // 1. Check terminal states (including remote peer ending or cancelling the call)
    if (room.isTerminalStatus()) {
      ringtoneHelper.stopAll()
      ringTimeoutJob?.cancel()
      offerRetryJob?.cancel()
      disconnectRecoveryJob?.cancel()
      when (room.status.lowercase()) {
        "rejected", "declined" -> {
          _webrtcState.value = WebRtcState.REJECTED
          return
        }
        "busy" -> {
          _webrtcState.value = WebRtcState.BUSY
          return
        }
        "cancelled" -> {
          _webrtcState.value = WebRtcState.CANCELLED
          return
        }
        "missed" -> {
          _webrtcState.value = WebRtcState.MISSED
          return
        }
        "failed" -> {
          _webrtcState.value = WebRtcState.FAILED
          return
        }
        else -> {
          _webrtcState.value = WebRtcState.CLOSED
          return
        }
      }
    }

    // 2. Waiting or Ringing state before callee accepts
    if (room.status == "waiting") {
      if (_webrtcState.value == WebRtcState.INITIALIZING) {
        _webrtcState.value = WebRtcState.WAITING_FOR_PARTNER
      }
    } else if (room.status == "ringing") {
      if (isCaller && _webrtcState.value != WebRtcState.RINGING && _webrtcState.value != WebRtcState.CONNECTED) {
        _webrtcState.value = WebRtcState.RINGING
        ringtoneHelper.startOutgoingRingbackTone()
      }
    }

    // 2a. Pre-generate Caller SDP Offer as soon as local media is ready so it is immediately available in Firestore when Callee accepts
    if (
      isCaller &&
      isLocalMediaReady &&
      !offerCreated &&
      room.sdpOffer.isNullOrBlank() &&
      room.matchMode != "speaking_room" &&
      room.status in listOf("ringing", "waiting")
    ) {
      offerCreated = true
      Log.i(TAG, "Pre-generating initial WebRTC SDP Offer while ringing for roomId=$roomId")
      evalJs("window.createAndSendOffer(false);")
    }

    // 2b. If remote peer requested an ICE Restart ("reconnecting"), Caller generates a fresh ICE-restart Offer
    if (
      isCaller &&
      room.connectionState == "reconnecting" &&
      !room.isTerminalStatus() &&
      _webrtcState.value != WebRtcState.RECONNECTING &&
      isLocalMediaReady
    ) {
      triggerConnectionRecovery()
      return
    }

    // 3. Callee accepted (or matched in room) -> Caller creates/ensures WebRTC SDP Offer
    if (room.status == "accepted" || room.status == "connected") {
      ringtoneHelper.stopAll()
      ringTimeoutJob?.cancel()

      val newParticipantJoined = room.matchMode == "speaking_room" &&
        room.lastSignalingEvent == "participant_joined" &&
        room.sdpAnswer.isNullOrBlank()

      if (isCaller && isLocalMediaReady) {
        if (!offerCreated || newParticipantJoined || room.sdpOffer.isNullOrBlank()) {
          offerCreated = true
          if (_webrtcState.value != WebRtcState.CONNECTED) {
            _webrtcState.value = WebRtcState.NEGOTIATING
          }
          Log.i(TAG, "Peer accepted/joined call; creating WebRTC SDP Offer for roomId=$roomId")
          evalJs("window.createAndSendOffer(${newParticipantJoined});")
          scheduleCallerOfferRetryIfNeeded(delayMs = 16_000L)
        } else if (room.sdpAnswer.isNullOrBlank() && _webrtcState.value != WebRtcState.CONNECTED) {
          _webrtcState.value = WebRtcState.NEGOTIATING
          scheduleCallerOfferRetryIfNeeded(delayMs = 16_000L)
        }
      }
    }

    // 4. Callee receives Caller's SDP Offer -> sets remote description and creates SDP Answer
    val remoteOffer = room.sdpOffer
    if (!isCaller && !remoteOffer.isNullOrBlank() && remoteOffer != lastProcessedOfferSdp && isLocalMediaReady) {
      lastProcessedOfferSdp = remoteOffer
      processedRemoteCandidates.clear()
      ringtoneHelper.stopAll()
      ringTimeoutJob?.cancel()
      if (_webrtcState.value != WebRtcState.CONNECTED) {
        _webrtcState.value = WebRtcState.NEGOTIATING
      }
      Log.i(TAG, "Received remote SDP Offer; generating SDP Answer for roomId=$roomId")
      val quotedOffer = JSONObject.quote(remoteOffer)
      evalJs("window.receiveOfferAndCreateAnswer($quotedOffer);")
    }

    // 5. Caller receives Callee's SDP Answer -> sets remote description
    val remoteAnswer = room.sdpAnswer
    if (isCaller && !remoteAnswer.isNullOrBlank() && remoteAnswer != lastProcessedAnswerSdp && isLocalMediaReady) {
      lastProcessedAnswerSdp = remoteAnswer
      offerRetryJob?.cancel()
      ringtoneHelper.stopAll()
      ringTimeoutJob?.cancel()
      if (_webrtcState.value != WebRtcState.CONNECTED) {
        _webrtcState.value = WebRtcState.CONNECTING
      }
      Log.i(TAG, "Received remote SDP Answer; setting remote description for roomId=$roomId")
      val quotedAnswer = JSONObject.quote(remoteAnswer)
      evalJs("window.receiveAnswer($quotedAnswer);")
    }

    // 6. Trickle ICE Candidates from remote peer
    val remoteCandidates = if (isCaller) room.calleeIceCandidates else room.callerIceCandidates
    if (isLocalMediaReady && remoteCandidates.isNotEmpty()) {
      for (candidateJson in remoteCandidates) {
        if (processedRemoteCandidates.add(candidateJson)) {
          Log.d(TAG, "Adding remote ICE candidate (total=${processedRemoteCandidates.size})")
          val quotedCandidate = JSONObject.quote(candidateJson)
          evalJs("window.addRemoteCandidate($quotedCandidate);")
        }
      }
    }
  }

  /**
   * JavascriptInterface methods invoked by the embedded Chromium WebRTC engine.
   */
  @JavascriptInterface
  fun onWebEngineLoaded() {
    Log.d(TAG, "WebRTC WebView Engine loaded")
    isWebEngineReady = true
    coroutineScope.launch(Dispatchers.Main) {
      maybeResolveTurnAndInitWebMedia(forceRefreshTurn = false)
    }
  }

  @JavascriptInterface
  fun onLocalMediaReady(hasAudio: Boolean, hasVideo: Boolean) {
    Log.i(TAG, "Local getUserMedia stream ready: audio=$hasAudio, video=$hasVideo")
    isLocalMediaReady = true
    _mediaErrorMessage.value = null
    coroutineScope.launch(Dispatchers.Main) {
      configureAudioHardware(enableSpeaker = _isSpeakerphoneOn.value)
      latestRoomSnapshot?.let { handleRoomSignalingUpdate(it) }
    }
  }

  @JavascriptInterface
  fun onMediaError(errorName: String, errorMessage: String) {
    Log.e(TAG, "getUserMedia / WebRTC error: $errorName - $errorMessage")
    coroutineScope.launch(Dispatchers.Main) {
      val friendlyMessage = when {
        errorName.contains("NotFound", ignoreCase = true) ||
          errorName.contains("DevicesNotFound", ignoreCase = true) ->
          "No microphone or camera hardware detected on this device."
        errorName.contains("NotReadable", ignoreCase = true) ||
          errorName.contains("TrackStart", ignoreCase = true) ->
          "Microphone or camera is already in use by another application. Please close other apps and tap Retry."
        errorName.contains("NotAllowed", ignoreCase = true) ||
          errorName.contains("Permission", ignoreCase = true) ->
          "Microphone or camera permission was denied. Please grant access and tap Retry."
        else -> "Media device error ($errorName): $errorMessage"
      }
      _mediaErrorMessage.value = friendlyMessage
      _webrtcState.value = WebRtcState.FAILED
    }
  }

  @JavascriptInterface
  fun onLocalSdpOffer(sdp: String, isIceRestart: Boolean) {
    Log.i(TAG, "Local WebRTC SDP Offer generated (length=${sdp.length}, iceRestart=$isIceRestart)")
    viewModel.sendSdpOffer(roomId, sdp, isIceRestart = isIceRestart)
  }

  @JavascriptInterface
  fun onLocalSdpAnswer(sdp: String) {
    Log.i(TAG, "Local WebRTC SDP Answer generated (length=${sdp.length})")
    viewModel.sendSdpAnswer(roomId, sdp)
  }

  @JavascriptInterface
  fun onLocalIceCandidate(candidateJson: String) {
    Log.d(TAG, "Local ICE candidate gathered")
    viewModel.addIceCandidate(roomId, candidateJson, isCaller = isCaller)
  }

  @JavascriptInterface
  fun onRemoteAudioTrackAdded() {
    Log.i(TAG, "Remote WebRTC audio track attached and playing")
    _isRemoteAudioAvailable.value = true
  }

  @JavascriptInterface
  fun onRemoteStreamAdded() {
    if (!_isRemoteStreamAvailable.value) {
      Log.i(TAG, "Remote WebRTC video stream actively rendering frames")
      _isRemoteStreamAvailable.value = true
    }
  }

  @JavascriptInterface
  fun onRemoteStreamRemoved() {
    Log.i(TAG, "Remote WebRTC video stream stopped or hidden")
    _isRemoteStreamAvailable.value = false
  }

  @JavascriptInterface
  fun onRemotePeerHungUp() {
    Log.i(TAG, "Received instant DataChannel hangup signal from remote peer: roomId=$roomId")
    coroutineScope.launch(Dispatchers.Main) {
      ringtoneHelper.stopAll()
      ringTimeoutJob?.cancel()
      offerRetryJob?.cancel()
      disconnectRecoveryJob?.cancel()
      _webrtcState.value = WebRtcState.CLOSED
    }
  }

  @JavascriptInterface
  fun onIceConnectionStateChanged(state: String) {
    Log.i(TAG, "WebRTC ICE/Peer connection state changed: $state")
    coroutineScope.launch(Dispatchers.Main) {
      if (latestRoomSnapshot?.isTerminalStatus() == true || _webrtcState.value == WebRtcState.CLOSED) {
        return@launch
      }
      when (state.lowercase()) {
        "checking", "connecting" -> {
          if (_webrtcState.value != WebRtcState.CONNECTED) {
            _webrtcState.value = WebRtcState.CONNECTING
          }
          viewModel.updateConnectionState(roomId, "checking")
        }
        "connected", "completed" -> {
          hasAutoRetriedFailedIce = false
          ringtoneHelper.stopAll()
          ringTimeoutJob?.cancel()
          offerRetryJob?.cancel()
          disconnectRecoveryJob?.cancel()
          if (_webrtcState.value != WebRtcState.CONNECTED) {
            _webrtcState.value = WebRtcState.CONNECTED
            configureAudioHardware(enableSpeaker = _isSpeakerphoneOn.value)
            viewModel.updateConnectionState(roomId, "connected")
          }
        }
        "disconnected" -> {
          Log.w(TAG, "WebRTC ICE disconnected; verifying whether remote peer ended call before reconnecting")
          disconnectRecoveryJob?.cancel()
          disconnectRecoveryJob = coroutineScope.launch {
            delay(1800L)
            val freshSnap = viewModel.getCallRoomSnapshot(roomId) ?: latestRoomSnapshot
            if (freshSnap == null || freshSnap.isTerminalStatus() || _webrtcState.value == WebRtcState.CLOSED) {
              _webrtcState.value = WebRtcState.CLOSED
              return@launch
            }
            _webrtcState.value = WebRtcState.RECONNECTING
            viewModel.updateConnectionState(roomId, "reconnecting")
            delay(3500L)
            val secondCheck = viewModel.getCallRoomSnapshot(roomId) ?: latestRoomSnapshot
            if (secondCheck == null || secondCheck.isTerminalStatus() || _webrtcState.value == WebRtcState.CLOSED) {
              _webrtcState.value = WebRtcState.CLOSED
              return@launch
            }
            if (_webrtcState.value == WebRtcState.RECONNECTING) {
              triggerConnectionRecovery()
            }
          }
        }
        "failed" -> {
          val freshSnap = viewModel.getCallRoomSnapshot(roomId) ?: latestRoomSnapshot
          if (freshSnap != null && freshSnap.isTerminalStatus()) {
            _webrtcState.value = WebRtcState.CLOSED
            return@launch
          }
          if (!hasAutoRetriedFailedIce) {
            hasAutoRetriedFailedIce = true
            Log.w(TAG, "Initial ICE check failed; automatically triggering ICE Restart with TURN relay")
            triggerConnectionRecovery()
          } else {
            Log.e(TAG, "WebRTC ICE connection failed after automatic retry")
            _webrtcState.value = WebRtcState.FAILED
            viewModel.updateConnectionState(roomId, "failed")
          }
        }
        "closed" -> {
          _webrtcState.value = WebRtcState.CLOSED
        }
      }
    }
  }

  @JavascriptInterface
  fun onStatsUpdated(rttMs: Int, candidateType: String) {
    if (rttMs > 0) {
      _connectionLatencyMs.value = rttMs
    }
    if (candidateType.isNotBlank()) {
      _activeCandidateType.value = candidateType
    }
  }

  /**
   * Triggers an ICE restart renegotiation (refreshing temporary Cloudflare TURN credentials)
   * when network changes or glitches occur, from either Caller or Callee.
   */
  fun triggerConnectionRecovery() {
    if (latestRoomSnapshot?.isTerminalStatus() == true || _webrtcState.value == WebRtcState.CLOSED) return
    Log.i(TAG, "Triggering WebRTC ICE Restart for roomId=$roomId")
    _mediaErrorMessage.value = null
    _webrtcState.value = WebRtcState.RECONNECTING
    processedRemoteCandidates.clear()
    viewModel.updateConnectionState(roomId, "reconnecting")
    coroutineScope.launch {
      val resolution = CloudflareTurnService.resolveIceServers(forceRefresh = true)
      resolvedIceServers = resolution.iceServers
      _turnProviderStatus.value = resolution.providerLabel
      val iceJson = JSONObject.quote(buildIceServersJson(resolution.iceServers))
      evalJs("window.updateIceServers($iceJson);")
      if (isCaller) {
        lastProcessedAnswerSdp = null
        evalJs("window.createAndSendOffer(true);")
        scheduleCallerOfferRetryIfNeeded()
      } else {
        lastProcessedOfferSdp = null
      }
    }
  }

  fun retryMediaAndConnection() {
    offerCreated = false
    hasAutoRetriedFailedIce = false
    hasStartedMediaInit = false
    isLocalMediaReady = false
    lastProcessedOfferSdp = null
    lastProcessedAnswerSdp = null
    processedRemoteCandidates.clear()
    startSession()
  }

  fun toggleMuteAudio() {
    val nextMuted = !_isAudioMuted.value
    _isAudioMuted.value = nextMuted
    try {
      audioManager?.isMicrophoneMute = nextMuted
    } catch (_: Exception) {}
    evalJs("window.setAudioEnabled(${!nextMuted});")
    viewModel.updateCallRoomState(
      roomId = roomId,
      isCallerMuted = if (isCaller) nextMuted else null,
      isCalleeMuted = if (!isCaller) nextMuted else null
    )
  }

  fun toggleVideo() {
    val nextVideoMuted = !_isVideoMuted.value
    _isVideoMuted.value = nextVideoMuted
    val enabled = !nextVideoMuted
    val facing = if (_useFrontCamera.value) "user" else "environment"
    evalJs("window.setVideoEnabled($enabled, '$facing');")
    viewModel.updateCallRoomState(
      roomId = roomId,
      isVideoEnabled = enabled,
      isCallerVideoEnabled = if (isCaller) enabled else null,
      isCalleeVideoEnabled = if (!isCaller) enabled else null
    )
  }

  fun switchCamera() {
    val nextFront = !_useFrontCamera.value
    _useFrontCamera.value = nextFront
    _isLocalVideoMirrored.value = nextFront
    val facing = if (nextFront) "user" else "environment"
    Log.i(TAG, "Switching camera to facingMode=$facing")
    evalJs("window.switchCameraFacing('$facing');")
  }

  fun setLocalMirror(mirrored: Boolean) {
    _isLocalVideoMirrored.value = mirrored
    evalJs("if (window.setLocalVideoMirrored) window.setLocalVideoMirrored($mirrored);")
  }

  fun toggleLocalMirror() {
    setLocalMirror(!_isLocalVideoMirrored.value)
  }

  fun setViewsSwapped(swapped: Boolean) {
    _areViewsSwapped.value = swapped
    evalJs("if (window.setViewsSwapped) window.setViewsSwapped($swapped);")
  }

  fun toggleSwapViews() {
    setViewsSwapped(!_areViewsSwapped.value)
  }

  fun setSelfViewHidden(hidden: Boolean) {
    _isSelfViewHidden.value = hidden
    evalJs("if (window.setSelfViewHidden) window.setSelfViewHidden($hidden);")
  }

  fun syncSelfViewLayout(
    leftCssPx: Float,
    topCssPx: Float,
    widthCssPx: Float,
    heightCssPx: Float,
    mirrored: Boolean = _isLocalVideoMirrored.value,
    swapped: Boolean = _areViewsSwapped.value,
    hidden: Boolean = _isSelfViewHidden.value
  ) {
    evalJs(
      "if (window.syncSelfViewLayout) window.syncSelfViewLayout(${leftCssPx.toInt()}, ${topCssPx.toInt()}, ${widthCssPx.toInt()}, ${heightCssPx.toInt()}, $mirrored, $swapped, $hidden);"
    )
  }

  fun toggleSpeakerphone() {
    cycleAudioRoute()
  }

  fun closeSession() {
    Log.i(TAG, "Closing WebRTC session and stopping all media tracks: roomId=$roomId")
    isSessionStarted = false
    ringtoneHelper.stopAll()
    com.example.presence.PresenceManager.setInCallStatus(false)
    IncomingCallRegistry.markRoomTerminal(context, roomId)
    NativeCallNotificationManager.dismissIncomingCallNotification(context, roomId)
    ActiveCallForegroundService.stop(context)
    unregisterAudioAndNetworkWatchers()
    observeRoomJob?.cancel()
    ringTimeoutJob?.cancel()
    offerRetryJob?.cancel()
    disconnectRecoveryJob?.cancel()
    evalJs("window.closeWebRtcSession();")
    _webrtcState.value = WebRtcState.CLOSED
    try {
      audioManager?.isMicrophoneMute = false
      @Suppress("DEPRECATION")
      audioManager?.stopBluetoothSco()
      @Suppress("DEPRECATION")
      audioManager?.isBluetoothScoOn = false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager?.clearCommunicationDevice()
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
      }
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (_: Exception) {}
  }
}

/**
 * Generates the self-contained HTML5 + W3C WebRTC (RTCPeerConnection + getUserMedia)
 * media engine page loaded into the call screen's WebView under https://lingosphere.aistudio.local.
 */
object WebRtcHtmlTemplate {
  fun buildHtml(): String = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      width: 100%;
      height: 100%;
      background-color: #040A18;
      overflow: hidden;
    }
    video::-webkit-media-controls,
    video::-webkit-media-controls-enclosure,
    video::-webkit-media-controls-panel,
    video::-webkit-media-controls-overlay-play-button,
    video::-webkit-media-controls-start-playback-button {
      display: none !important;
      -webkit-appearance: none !important;
      opacity: 0 !important;
    }
    #remoteContainer {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      background: radial-gradient(circle at center, #0E2248 0%, #040A18 100%);
    }
    #remoteVideo {
      width: 100%;
      height: 100%;
      object-fit: cover;
      background-color: transparent;
      display: block;
      opacity: 0;
      transition: opacity 0.2s ease-in-out;
    }
    #localVideo {
      position: fixed;
      bottom: 196px;
      right: 14px;
      width: 84px;
      height: 116px;
      object-fit: cover;
      border-radius: 14px;
      border: 2px solid #38BDF8;
      background-color: #071229;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.72);
      transform: scaleX(-1);
      z-index: 30;
      display: none;
    }
  </style>
</head>
<body>
  <div id="remoteContainer">
    <video id="remoteVideo" autoplay playsinline muted poster="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"></video>
    <audio id="remoteAudio" autoplay playsinline></audio>
  </div>
  <video id="localVideo" autoplay playsinline muted poster="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"></video>

  <script>
    let pc = null;
    let controlChannel = null;
    let localStream = null;
    let remoteAudioStream = null;
    let remoteVideoStream = null;
    let pendingCandidates = [];
    let bufferedLocalCandidates = [];
    let isHoldingLocalCandidates = false;
    let isSettingRemoteDesc = false;
    let signalingQueue = Promise.resolve();
    let statsTimer = null;
    let currentIceServers = [];
    let isCallerRole = false;

    const remoteVideo = document.getElementById('remoteVideo');
    const remoteAudio = document.getElementById('remoteAudio');
    const localVideo = document.getElementById('localVideo');

    function revealRemoteVideoWhenReady() {
      if (!remoteVideo) return;
      const hasActiveVideoTrack = remoteVideoStream &&
        remoteVideoStream.getVideoTracks().some(function(t) { return t.readyState === 'live' && !t.muted; });
      if ((remoteVideo.videoWidth > 0 && remoteVideo.videoHeight > 0) || hasActiveVideoTrack) {
        remoteVideo.style.display = 'block';
        remoteVideo.style.opacity = '1';
        if (window.AndroidBridge && window.AndroidBridge.onRemoteStreamAdded) {
          window.AndroidBridge.onRemoteStreamAdded();
        }
      }
    }

    remoteVideo.onloadedmetadata = revealRemoteVideoWhenReady;
    remoteVideo.onloadeddata = revealRemoteVideoWhenReady;
    remoteVideo.onplaying = revealRemoteVideoWhenReady;
    remoteVideo.ontimeupdate = revealRemoteVideoWhenReady;
    remoteVideo.onresize = revealRemoteVideoWhenReady;

    function enqueueSignalingTask(taskFn) {
      signalingQueue = signalingQueue.then(taskFn).catch(function(err) {
        console.error('WebRTC signaling task error:', err);
      });
      return signalingQueue;
    }

    window.updateIceServers = function(iceServersJsonStr) {
      try {
        currentIceServers = JSON.parse(iceServersJsonStr);
        if (pc && typeof pc.setConfiguration === 'function') {
          pc.setConfiguration({
            iceServers: currentIceServers,
            iceTransportPolicy: 'all',
            iceCandidatePoolSize: 8,
            bundlePolicy: 'max-bundle',
            rtcpMuxPolicy: 'require'
          });
        }
      } catch (e) {}
    };

    async function acquireUserMediaWithFallback(enableVideo, facingMode) {
      const audioConstraints = {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      };
      if (!enableVideo) {
        try {
          return await navigator.mediaDevices.getUserMedia({ audio: audioConstraints, video: false });
        } catch (e) {
          return await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        }
      }

      const attempts = [
        {
          audio: audioConstraints,
          video: {
            facingMode: { ideal: facingMode || 'user' },
            width: { ideal: 1280 },
            height: { ideal: 720 }
          }
        },
        {
          audio: true,
          video: { facingMode: facingMode || 'user' }
        },
        {
          audio: true,
          video: true
        }
      ];

      let lastErr = null;
      for (let i = 0; i < attempts.length; i++) {
        try {
          return await navigator.mediaDevices.getUserMedia(attempts[i]);
        } catch (err) {
          lastErr = err;
        }
      }
      throw lastErr || new Error('Unable to capture microphone and camera');
    }

    window.startWebRtcMedia = async function(iceServersJsonStr, isCaller, enableVideo, facingMode) {
      try {
        isCallerRole = !!isCaller;
        currentIceServers = JSON.parse(iceServersJsonStr);

        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
          if (window.AndroidBridge) {
            window.AndroidBridge.onMediaError('UnsupportedDeviceError', 'WebRTC getUserMedia is not supported on this device WebView.');
          }
          return;
        }

        localStream = await acquireUserMediaWithFallback(enableVideo, facingMode);
        const hasAudio = localStream.getAudioTracks().length > 0;
        const hasVideo = localStream.getVideoTracks().length > 0;

        localStream.getAudioTracks().forEach(function(t) {
          t.enabled = true;
        });

        if (hasVideo) {
          localStream.getVideoTracks().forEach(function(t) {
            t.enabled = true;
          });
          localVideo.srcObject = null;
          localVideo.srcObject = localStream;
          localVideo.muted = true;
          localVideo.style.display = 'block';
          localVideo.style.transform = (facingMode === 'environment') ? 'scaleX(1)' : 'scaleX(-1)';
          localVideo.play().catch(function() {});
        } else {
          localVideo.style.display = 'none';
        }

        initPeerConnection();

        if (window.AndroidBridge) {
          window.AndroidBridge.onLocalMediaReady(hasAudio, hasVideo);
        }
      } catch (err) {
        if (window.AndroidBridge) {
          window.AndroidBridge.onMediaError(err.name || 'MediaError', err.message || String(err));
        }
      }
    };

    function attachAndPlayAudio() {
      if (!remoteAudioStream || remoteAudioStream.getAudioTracks().length === 0) return;
      if (remoteAudio.srcObject !== remoteAudioStream) {
        remoteAudio.srcObject = remoteAudioStream;
      }
      remoteAudio.muted = false;
      remoteAudio.volume = 1.0;
      const p = remoteAudio.play();
      if (p !== undefined) {
        p.catch(function() {
          setTimeout(function() {
            remoteAudio.play().catch(function() {});
          }, 250);
        });
      }
      if (window.AndroidBridge && window.AndroidBridge.onRemoteAudioTrackAdded) {
        window.AndroidBridge.onRemoteAudioTrackAdded();
      }
    }

    function attachAndPlayVideo(streamOverride) {
      const targetStream = (streamOverride && streamOverride.getVideoTracks && streamOverride.getVideoTracks().length > 0)
        ? streamOverride
        : remoteVideoStream;
      if (!targetStream || targetStream.getVideoTracks().length === 0) return;
      if (remoteVideo.srcObject !== targetStream) {
        remoteVideo.srcObject = targetStream;
      }
      remoteVideo.style.display = 'block';
      remoteVideo.style.opacity = '1';
      remoteVideo.muted = true;
      const p = remoteVideo.play();
      if (p !== undefined) {
        p.then(function() {
          revealRemoteVideoWhenReady();
        }).catch(function() {
          setTimeout(function() {
            remoteVideo.play().then(revealRemoteVideoWhenReady).catch(function() {});
          }, 200);
        });
      }
      if (window.AndroidBridge && window.AndroidBridge.onRemoteStreamAdded) {
        window.AndroidBridge.onRemoteStreamAdded();
      }
      revealRemoteVideoWhenReady();
    }

    function ensureLocalTracksAdded() {
      if (!pc || !localStream) return;
      const existingSenders = pc.getSenders ? pc.getSenders() : [];
      localStream.getTracks().forEach(function(track) {
        const alreadySent = existingSenders.some(function(s) {
          return s.track && s.track.id === track.id;
        });
        if (!alreadySent) {
          const matchingKindSender = existingSenders.find(function(s) {
            return s.track && s.track.kind === track.kind;
          });
          if (matchingKindSender) {
            matchingKindSender.replaceTrack(track).catch(function() {});
          } else {
            try {
              pc.addTrack(track, localStream);
            } catch (e) {}
          }
        }
      });
    }

    async function bindLocalTracksToTransceivers() {
      if (!pc || !localStream || !pc.getTransceivers) return;
      const transceivers = pc.getTransceivers();
      const tracks = localStream.getTracks();
      for (let i = 0; i < tracks.length; i++) {
        const track = tracks[i];
        let matchedTr = transceivers.find(function(tr) {
          return (tr.sender && tr.sender.track && tr.sender.track.kind === track.kind) ||
                 (tr.receiver && tr.receiver.track && tr.receiver.track.kind === track.kind);
        });
        if (matchedTr && matchedTr.sender) {
          if (!matchedTr.sender.track || matchedTr.sender.track.id !== track.id) {
            try {
              await matchedTr.sender.replaceTrack(track);
            } catch (e) {}
          }
          try {
            matchedTr.direction = 'sendrecv';
          } catch (e) {}
        } else {
          try {
            pc.addTrack(track, localStream);
          } catch (e) {}
        }
      }
    }

    function waitForInitialIceGathering(timeoutMs) {
      return new Promise(function(resolve) {
        if (!pc || pc.iceGatheringState === 'complete') {
          resolve();
          return;
        }
        let done = false;
        const finish = function() {
          if (done) return;
          done = true;
          resolve();
        };
        const timer = setTimeout(finish, timeoutMs || 650);
        const checkState = function() {
          if (pc && pc.iceGatheringState === 'complete') {
            clearTimeout(timer);
            finish();
          }
        };
        pc.addEventListener('icegatheringstatechange', checkState);
      });
    }

    function flushBufferedLocalCandidates() {
      isHoldingLocalCandidates = false;
      if (!window.AndroidBridge) {
        bufferedLocalCandidates = [];
        return;
      }
      while (bufferedLocalCandidates.length > 0) {
        const payload = bufferedLocalCandidates.shift();
        window.AndroidBridge.onLocalIceCandidate(payload);
      }
    }

    function setupControlChannel(channel) {
      if (!channel) return;
      controlChannel = channel;
      controlChannel.onmessage = function(ev) {
        try {
          const msg = JSON.parse(ev.data);
          if (msg && msg.type === 'hangup') {
            if (window.AndroidBridge && window.AndroidBridge.onRemotePeerHungUp) {
              window.AndroidBridge.onRemotePeerHungUp();
            }
          }
        } catch (e) {}
      };
    }

    function initPeerConnection() {
      if (pc) {
        try { pc.close(); } catch (e) {}
      }
      remoteAudioStream = new MediaStream();
      remoteVideoStream = new MediaStream();

      pc = new RTCPeerConnection({
        iceServers: currentIceServers,
        iceTransportPolicy: 'all',
        iceCandidatePoolSize: 8,
        bundlePolicy: 'max-bundle',
        rtcpMuxPolicy: 'require'
      });

      if (isCallerRole && typeof pc.createDataChannel === 'function') {
        try {
          setupControlChannel(pc.createDataChannel('lingo_control', { ordered: true }));
        } catch (e) {}
      }
      pc.ondatachannel = function(event) {
        if (event && event.channel) {
          setupControlChannel(event.channel);
        }
      };

      ensureLocalTracksAdded();

      pc.onicecandidate = function(event) {
        if (event.candidate && event.candidate.candidate && window.AndroidBridge) {
          const payload = JSON.stringify({
            candidate: event.candidate.candidate,
            sdpMid: event.candidate.sdpMid,
            sdpMLineIndex: event.candidate.sdpMLineIndex
          });
          if (isHoldingLocalCandidates) {
            bufferedLocalCandidates.push(payload);
          } else {
            window.AndroidBridge.onLocalIceCandidate(payload);
          }
        }
      };

      pc.ontrack = function(event) {
        const track = event.track;
        if (!track) return;
        const eventStream = (event.streams && event.streams.length > 0) ? event.streams[0] : null;

        if (track.kind === 'audio') {
          if (!remoteAudioStream) remoteAudioStream = new MediaStream();
          remoteAudioStream.getAudioTracks().forEach(function(old) {
            if (old.id !== track.id) remoteAudioStream.removeTrack(old);
          });
          if (!remoteAudioStream.getAudioTracks().some(function(t) { return t.id === track.id; })) {
            remoteAudioStream.addTrack(track);
          }
          attachAndPlayAudio();
          track.onunmute = function() {
            attachAndPlayAudio();
          };
        } else if (track.kind === 'video') {
          if (!remoteVideoStream) remoteVideoStream = new MediaStream();
          remoteVideoStream.getVideoTracks().forEach(function(old) {
            if (old.id !== track.id) remoteVideoStream.removeTrack(old);
          });
          if (!remoteVideoStream.getVideoTracks().some(function(t) { return t.id === track.id; })) {
            remoteVideoStream.addTrack(track);
          }
          attachAndPlayVideo(eventStream);
          track.onunmute = function() {
            attachAndPlayVideo(eventStream);
          };
          track.onended = function() {
            remoteVideo.style.opacity = '0';
            if (window.AndroidBridge && window.AndroidBridge.onRemoteStreamRemoved) {
              window.AndroidBridge.onRemoteStreamRemoved();
            }
          };
        }
      };

      function handlePeerStateUpdate(state) {
        if (!state) return;
        if (window.AndroidBridge) {
          window.AndroidBridge.onIceConnectionStateChanged(state);
        }
        if (state === 'connected' || state === 'completed') {
          attachAndPlayAudio();
          if (remoteVideoStream && remoteVideoStream.getVideoTracks().length > 0) {
            attachAndPlayVideo();
          }
          startStatsPolling();
        } else if (state === 'closed' || state === 'failed') {
          remoteVideo.style.opacity = '0';
          if (window.AndroidBridge && window.AndroidBridge.onRemoteStreamRemoved) {
            window.AndroidBridge.onRemoteStreamRemoved();
          }
          stopStatsPolling();
        }
      }

      pc.oniceconnectionstatechange = function() {
        if (!pc) return;
        handlePeerStateUpdate(pc.iceConnectionState);
      };

      pc.onconnectionstatechange = function() {
        if (!pc) return;
        if (pc.connectionState === 'connected' || pc.connectionState === 'failed' || pc.connectionState === 'closed') {
          handlePeerStateUpdate(pc.connectionState);
        }
      };
    }

    window.createAndSendOffer = function(iceRestart) {
      return enqueueSignalingTask(async function() {
        try {
          if (!pc) initPeerConnection();
          await bindLocalTracksToTransceivers();
          bufferedLocalCandidates = [];
          isHoldingLocalCandidates = true;
          const offer = await pc.createOffer({
            offerToReceiveAudio: true,
            offerToReceiveVideo: true,
            iceRestart: !!iceRestart
          });
          await pc.setLocalDescription(offer);
          await waitForInitialIceGathering(650);
          const finalSdp = (pc.localDescription && pc.localDescription.sdp) ? pc.localDescription.sdp : offer.sdp;
          if (window.AndroidBridge) {
            window.AndroidBridge.onLocalSdpOffer(finalSdp, !!iceRestart);
          }
          setTimeout(flushBufferedLocalCandidates, 100);
        } catch (err) {
          isHoldingLocalCandidates = false;
          if (window.AndroidBridge) {
            window.AndroidBridge.onMediaError('CreateOfferError', err.message || String(err));
          }
        }
      });
    };

    window.receiveOfferAndCreateAnswer = function(sdpOffer) {
      isSettingRemoteDesc = true;
      return enqueueSignalingTask(async function() {
        try {
          if (!pc) initPeerConnection();
          ensureLocalTracksAdded();
          bufferedLocalCandidates = [];
          isHoldingLocalCandidates = true;
          isSettingRemoteDesc = true;
          if (pc.signalingState && pc.signalingState !== 'stable') {
            try {
              await pc.setLocalDescription({ type: 'rollback' });
            } catch (e) {}
          }
          await pc.setRemoteDescription(new RTCSessionDescription({ type: 'offer', sdp: sdpOffer }));
          isSettingRemoteDesc = false;

          await bindLocalTracksToTransceivers();
          await flushPendingCandidates();

          const answer = await pc.createAnswer();
          await pc.setLocalDescription(answer);
          await waitForInitialIceGathering(650);
          const finalSdp = (pc.localDescription && pc.localDescription.sdp) ? pc.localDescription.sdp : answer.sdp;
          if (window.AndroidBridge) {
            window.AndroidBridge.onLocalSdpAnswer(finalSdp);
          }
          setTimeout(flushBufferedLocalCandidates, 100);
        } catch (err) {
          isSettingRemoteDesc = false;
          isHoldingLocalCandidates = false;
          if (window.AndroidBridge) {
            window.AndroidBridge.onMediaError('CreateAnswerError', err.message || String(err));
          }
        }
      });
    };

    window.receiveAnswer = function(sdpAnswer) {
      isSettingRemoteDesc = true;
      return enqueueSignalingTask(async function() {
        try {
          if (!pc) {
            isSettingRemoteDesc = false;
            return;
          }
          isSettingRemoteDesc = true;
          await pc.setRemoteDescription(new RTCSessionDescription({ type: 'answer', sdp: sdpAnswer }));
          isSettingRemoteDesc = false;
          await flushPendingCandidates();
        } catch (err) {
          isSettingRemoteDesc = false;
          if (window.AndroidBridge) {
            window.AndroidBridge.onMediaError('SetRemoteAnswerError', err.message || String(err));
          }
        }
      });
    };

    window.addRemoteCandidate = function(candidateJsonStr) {
      try {
        let parsed = null;
        if (candidateJsonStr.trim().startsWith('{')) {
          parsed = JSON.parse(candidateJsonStr);
        } else {
          parsed = { candidate: candidateJsonStr, sdpMid: '0', sdpMLineIndex: 0 };
        }
        if (pc && !isSettingRemoteDesc && pc.remoteDescription && pc.remoteDescription.type) {
          pc.addIceCandidate(new RTCIceCandidate(parsed)).catch(function() {
            pendingCandidates.push(parsed);
          });
        } else {
          pendingCandidates.push(parsed);
        }
      } catch (e) {}
    };

    async function flushPendingCandidates() {
      if (!pc || !pc.remoteDescription || !pc.remoteDescription.type) return;
      while (pendingCandidates.length > 0) {
        const c = pendingCandidates.shift();
        try {
          await pc.addIceCandidate(new RTCIceCandidate(c));
        } catch (e) {}
      }
    }

    window.setAudioEnabled = function(enabled) {
      if (!localStream) return;
      localStream.getAudioTracks().forEach(function(t) {
        t.enabled = !!enabled;
      });
    };

    window.setVideoEnabled = async function(enabled, facingMode) {
      if (!localStream) return;
      const videoTracks = localStream.getVideoTracks();
      if (videoTracks.length > 0) {
        videoTracks.forEach(function(t) {
          t.enabled = !!enabled;
        });
        localVideo.style.display = enabled ? 'block' : 'none';
        if (enabled) {
          localVideo.play().catch(function() {});
        }
      } else if (enabled) {
        try {
          const camStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: facingMode || 'user', width: { ideal: 1280 }, height: { ideal: 720 } }
          });
          const newTrack = camStream.getVideoTracks()[0];
          if (newTrack) {
            localStream.addTrack(newTrack);
            if (pc) {
              await bindLocalTracksToTransceivers();
              if (isCallerRole) {
                await window.createAndSendOffer(false);
              }
            }
            localVideo.srcObject = null;
            localVideo.srcObject = localStream;
            localVideo.style.display = 'block';
            localVideo.play().catch(function() {});
          }
        } catch (e) {}
      }
    };

    window.switchCameraFacing = async function(facingMode) {
      if (!localStream) return;
      try {
        const oldTracks = localStream.getVideoTracks();
        oldTracks.forEach(function(t) {
          t.stop();
          localStream.removeTrack(t);
        });
        let newVideoStream = null;
        try {
          newVideoStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: { exact: facingMode }, width: { ideal: 1280 }, height: { ideal: 720 } }
          });
        } catch (e) {
          newVideoStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: { ideal: facingMode }, width: { ideal: 1280 }, height: { ideal: 720 } }
          });
        }
        const newVideoTrack = newVideoStream.getVideoTracks()[0];
        if (newVideoTrack) {
          localStream.addTrack(newVideoTrack);
          if (pc) {
            await bindLocalTracksToTransceivers();
          }
          localVideo.srcObject = null;
          localVideo.srcObject = localStream;
          localVideo.style.transform = (facingMode === 'environment') ? 'scaleX(1)' : 'scaleX(-1)';
          localVideo.play().catch(function() {});
        }
      } catch (e) {}
    };

    let currentPipLeft = 240;
    let currentPipTop = 96;
    let currentPipWidth = 110;
    let currentPipHeight = 150;
    let currentLocalMirrored = true;
    let currentViewsSwapped = false;
    let currentSelfHidden = false;

    function applyVideoLayoutState() {
      if (!localVideo || !remoteVideo) return;
      localVideo.style.transform = currentLocalMirrored ? 'scaleX(-1)' : 'scaleX(1)';
      if (currentSelfHidden) {
        localVideo.style.opacity = '0';
        localVideo.style.pointerEvents = 'none';
      } else {
        localVideo.style.opacity = '1';
      }
      if (currentViewsSwapped && !currentSelfHidden) {
        localVideo.style.position = 'fixed';
        localVideo.style.left = '0px';
        localVideo.style.top = '0px';
        localVideo.style.right = 'auto';
        localVideo.style.bottom = 'auto';
        localVideo.style.width = '100%';
        localVideo.style.height = '100%';
        localVideo.style.borderRadius = '0px';
        localVideo.style.border = 'none';
        localVideo.style.zIndex = '10';

        remoteVideo.style.position = 'fixed';
        remoteVideo.style.left = currentPipLeft + 'px';
        remoteVideo.style.top = currentPipTop + 'px';
        remoteVideo.style.width = currentPipWidth + 'px';
        remoteVideo.style.height = currentPipHeight + 'px';
        remoteVideo.style.borderRadius = '16px';
        remoteVideo.style.zIndex = '30';
      } else {
        remoteVideo.style.position = 'static';
        remoteVideo.style.width = '100%';
        remoteVideo.style.height = '100%';
        remoteVideo.style.borderRadius = '0px';
        remoteVideo.style.zIndex = '10';

        localVideo.style.position = 'fixed';
        localVideo.style.left = currentPipLeft + 'px';
        localVideo.style.top = currentPipTop + 'px';
        localVideo.style.right = 'auto';
        localVideo.style.bottom = 'auto';
        localVideo.style.width = currentPipWidth + 'px';
        localVideo.style.height = currentPipHeight + 'px';
        localVideo.style.borderRadius = '16px';
        localVideo.style.border = '2px solid #2DD4FF';
        localVideo.style.zIndex = '30';
      }
    }

    window.setLocalVideoMirrored = function(mirrored) {
      currentLocalMirrored = !!mirrored;
      applyVideoLayoutState();
    };

    window.setViewsSwapped = function(swapped) {
      currentViewsSwapped = !!swapped;
      applyVideoLayoutState();
    };

    window.setSelfViewHidden = function(hidden) {
      currentSelfHidden = !!hidden;
      applyVideoLayoutState();
    };

    window.syncSelfViewLayout = function(leftPx, topPx, widthPx, heightPx, mirrored, swapped, hidden) {
      currentPipLeft = leftPx;
      currentPipTop = topPx;
      currentPipWidth = widthPx;
      currentPipHeight = heightPx;
      currentLocalMirrored = !!mirrored;
      currentViewsSwapped = !!swapped;
      currentSelfHidden = !!hidden;
      applyVideoLayoutState();
    };

    function startStatsPolling() {
      stopStatsPolling();
      statsTimer = setInterval(async function() {
        if (!pc) return;
        try {
          const stats = await pc.getStats();
          let rttMs = 0;
          let candidateType = 'stun';
          let activeLocalCandidateId = null;
          let inboundVideoBytes = 0;
          let inboundVideoFrames = 0;

          stats.forEach(function(report) {
            if (report.type === 'candidate-pair' && (report.nominated || report.selected || report.state === 'succeeded')) {
              if (report.currentRoundTripTime) {
                rttMs = Math.round(report.currentRoundTripTime * 1000);
              }
              if (report.localCandidateId) {
                activeLocalCandidateId = report.localCandidateId;
              }
            } else if (report.type === 'inbound-rtp' && (report.kind === 'video' || report.mediaType === 'video')) {
              inboundVideoBytes += (report.bytesReceived || 0);
              inboundVideoFrames += (report.framesDecoded || 0);
            }
          });

          if (activeLocalCandidateId && stats.get) {
            const localCand = stats.get(activeLocalCandidateId);
            if (localCand && localCand.candidateType) {
              candidateType = localCand.candidateType;
            }
          }

          if (inboundVideoBytes > 0 || inboundVideoFrames > 0 || (remoteVideo && remoteVideo.videoWidth > 0)) {
            remoteVideo.style.display = 'block';
            remoteVideo.style.opacity = '1';
            if (remoteVideo.paused) {
              remoteVideo.play().catch(function() {});
            }
            if (window.AndroidBridge && window.AndroidBridge.onRemoteStreamAdded) {
              window.AndroidBridge.onRemoteStreamAdded();
            }
          }

          if (window.AndroidBridge) {
            window.AndroidBridge.onStatsUpdated(rttMs, candidateType);
          }
        } catch (e) {}
      }, 1500);
    }

    function stopStatsPolling() {
      if (statsTimer) {
        clearInterval(statsTimer);
        statsTimer = null;
      }
    }

    window.closeWebRtcSession = function() {
      stopStatsPolling();
      if (controlChannel && controlChannel.readyState === 'open') {
        try {
          controlChannel.send(JSON.stringify({ type: 'hangup' }));
        } catch (e) {}
      }
      if (controlChannel) {
        try { controlChannel.close(); } catch (e) {}
        controlChannel = null;
      }
      if (localStream) {
        localStream.getTracks().forEach(function(t) {
          try { t.stop(); } catch (e) {}
        });
        localStream = null;
      }
      if (remoteAudioStream) {
        remoteAudioStream.getTracks().forEach(function(t) {
          try { t.stop(); } catch (e) {}
        });
        remoteAudioStream = null;
      }
      if (remoteVideoStream) {
        remoteVideoStream.getTracks().forEach(function(t) {
          try { t.stop(); } catch (e) {}
        });
        remoteVideoStream = null;
      }
      if (pc) {
        try { pc.close(); } catch (e) {}
        pc = null;
      }
      remoteVideo.srcObject = null;
      remoteAudio.srcObject = null;
      localVideo.srcObject = null;
    };

    if (window.AndroidBridge) {
      window.AndroidBridge.onWebEngineLoaded();
    }
  </script>
</body>
</html>
  """.trimIndent()
}
