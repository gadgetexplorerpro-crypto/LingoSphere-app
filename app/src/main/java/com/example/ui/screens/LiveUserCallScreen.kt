package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ai.GeminiTutorService
import com.example.data.model.CallRoom
import com.example.data.model.CountryInfo
import com.example.data.model.Language
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.components.LingoTokens
import com.example.webrtc.CallAudioRoute
import com.example.webrtc.WebRtcHtmlTemplate
import com.example.webrtc.WebRtcSignalingManager
import com.example.webrtc.WebRtcState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SelfViewPipCorner {
  TOP_LEFT,
  TOP_RIGHT,
  BOTTOM_LEFT,
  BOTTOM_RIGHT
}

/**
 * Production Real-Time WebRTC Voice & Video Calling Screen.
 * Includes a draggable Picture-in-Picture self-view (4-corner 200ms snap, single-tap view swap,
 * double-tap size toggle, long-press menu, collapse to edge, and avatar fallback when camera is off),
 * slim header with language pair codes, unified Tools bottom sheet, 56dp control bar, live captions,
 * and valid-call (30s+) end summary dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveUserCallScreen(
  initialRoom: CallRoom,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onEndCall: () -> Unit,
  onSwitchToAiTutor: () -> Unit,
  onCallAgain: (CallRoom) -> Unit = {}
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  val liveRoomFlow = remember(initialRoom.roomId) { viewModel.observeCallRoom(initialRoom.roomId) }
  val liveRoom by liveRoomFlow.collectAsState(initial = initialRoom)
  val room = liveRoom ?: initialRoom
  val userProfile by viewModel.userProfileState.collectAsState()
  val targetLang = Language.findByCode(room.targetLanguageCode)

  // Keep screen awake during active call
  DisposableEffect(activity) {
    val window = activity?.window
    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  val isCaller = remember(room.callerId, viewModel.userId) { room.callerId == viewModel.userId }
  val isInitialVideo = remember(room.callType) { room.callType == "video" }

  // Partner identity & language metadata (real data from CallRoom, never hardcoded dummy names)
  val partnerId = if (isCaller) room.calleeId else room.callerId
  val partnerDisplayName = remember(room.callerName, room.calleeName, room.status, isCaller) {
    val rawName = if (isCaller) room.calleeName else room.callerName
    if (rawName.isNotBlank() && rawName != "waiting_partner") {
      rawName
    } else if (room.status == "waiting") {
      "Searching for Partner..."
    } else {
      "Language Partner"
    }
  }
  val partnerNativeLang = Language.findByCode(
    if (isCaller) room.calleeNativeLang.ifBlank { room.targetLanguageCode }
    else room.callerNativeLang.ifBlank { "en" }
  )
  val partnerLearningLang = Language.findByCode(
    if (isCaller) room.calleeLearningLang.ifBlank { "en" }
    else room.callerLearningLang.ifBlank { room.targetLanguageCode }
  )
  val isSameLanguagePair = remember(partnerNativeLang.code, partnerLearningLang.code) {
    partnerNativeLang.code.equals(partnerLearningLang.code, ignoreCase = true)
  }
  val languagePairLabel = remember(partnerNativeLang.code, partnerLearningLang.code, isSameLanguagePair) {
    if (isSameLanguagePair) {
      "Practice partner"
    } else {
      "${partnerNativeLang.code.uppercase()} ⇄ ${partnerLearningLang.code.uppercase()}"
    }
  }

  val userDisplayName = remember(userProfile.displayName, room.callerName, room.calleeName, isCaller) {
    val fromRoom = if (isCaller) room.callerName else room.calleeName
    userProfile.displayName.trim().ifBlank { fromRoom.trim().ifBlank { "Learner" } }
  }
  val userInitials = remember(userDisplayName) {
    userDisplayName
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotEmpty() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "ME" }
  }

  val isPartnerMuted = if (isCaller) room.isCalleeMuted else room.isCallerMuted
  val isPartnerVideoEnabled = if (isCaller) room.isCalleeVideoEnabled else room.isCallerVideoEnabled

  // Initialize WebRTC Signaling & Media Manager
  val rtcManager = remember(room.roomId) {
    WebRtcSignalingManager(
      context = context,
      roomId = room.roomId,
      isCaller = isCaller,
      isVideo = isInitialVideo,
      viewModel = viewModel,
      coroutineScope = coroutineScope
    )
  }

  val rtcState by rtcManager.webrtcState.collectAsState()
  val isMuted by rtcManager.isAudioMuted.collectAsState()
  val isVideoMuted by rtcManager.isVideoMuted.collectAsState()
  val isSpeakerOn by rtcManager.isSpeakerphoneOn.collectAsState()
  val activeAudioRoute by rtcManager.activeAudioRoute.collectAsState()
  val isBluetoothAvailable by rtcManager.isBluetoothAvailable.collectAsState()
  val useFrontCamera by rtcManager.useFrontCamera.collectAsState()
  val isLocalVideoMirrored by rtcManager.isLocalVideoMirrored.collectAsState()
  val areViewsSwapped by rtcManager.areViewsSwapped.collectAsState()
  val isSelfViewHidden by rtcManager.isSelfViewHidden.collectAsState()
  val latencyMs by rtcManager.connectionLatencyMs.collectAsState()
  val activeCandidateType by rtcManager.activeCandidateType.collectAsState()
  val mediaErrorMessage by rtcManager.mediaErrorMessage.collectAsState()
  val isRemoteStreamAvailable by rtcManager.isRemoteStreamAvailable.collectAsState()
  val activeCallHangupSignal by com.example.webrtc.IncomingCallRegistry.activeCallHangupFlow.collectAsState()

  // Persistent PiP preferences (corner + size)
  val pipPrefs = remember {
    context.applicationContext.getSharedPreferences("lingosphere_call_pip_prefs", Context.MODE_PRIVATE)
  }
  var pipCorner by remember {
    val saved = pipPrefs.getString("pip_corner", SelfViewPipCorner.TOP_RIGHT.name)
    val parsed = runCatching { SelfViewPipCorner.valueOf(saved ?: SelfViewPipCorner.TOP_RIGHT.name) }
      .getOrDefault(SelfViewPipCorner.TOP_RIGHT)
    mutableStateOf(parsed)
  }
  var isLargePip by remember {
    mutableStateOf(pipPrefs.getBoolean("pip_large", false))
  }
  var isDraggingPip by remember { mutableStateOf(false) }
  var showPipContextMenu by remember { mutableStateOf(false) }

  fun savePipPreferences(corner: SelfViewPipCorner, large: Boolean) {
    pipCorner = corner
    isLargePip = large
    runCatching {
      pipPrefs.edit()
        .putString("pip_corner", corner.name)
        .putBoolean("pip_large", large)
        .apply()
    }
  }

  var elapsedSeconds by remember { mutableLongStateOf(0L) }
  var sessionStarted by remember { mutableStateOf(false) }
  var showToolsSheet by remember { mutableStateOf(false) }
  var showAudioRouteSheet by remember { mutableStateOf(false) }
  var showTopicSelector by remember { mutableStateOf(false) }
  var showInCallChat by remember { mutableStateOf(false) }
  var showLiveAssistant by remember { mutableStateOf(false) }
  var liveCaptionsEnabled by remember { mutableStateOf(true) }
  var translateCaptionsEnabled by remember { mutableStateOf(true) }
  var transcriptionConsentGranted by remember { mutableStateOf(false) }
  var showCallEndSummaryDialog by remember { mutableStateOf(false) }
  var chatInputText by remember { mutableStateOf("") }
  var isTranslatingMsgId by remember { mutableStateOf<String?>(null) }
  var translatedMessages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

  // Hardware & Permission checks
  val hasMicHardware = remember {
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
  }
  val hasCameraHardware = remember {
    try {
      val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
      cm?.cameraIdList?.isNotEmpty() == true
    } catch (_: Exception) {
      false
    }
  }

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var permissionDeniedOnce by remember { mutableStateOf(false) }
  var permissionPermanentlyBlocked by remember { mutableStateOf(false) }
  var hardwareCheckError by remember { mutableStateOf<String?>(null) }
  var downgradedToAudioOnly by remember { mutableStateOf(false) }
  var showWeakNetworkAudioBanner by remember { mutableStateOf(false) }
  var didOutgoingTimeout by remember { mutableStateOf(false) }
  var didReconnectTimeout by remember { mutableStateOf(false) }
  var isVoiceTopicCardExpanded by remember { mutableStateOf(false) }
  var areVideoControlsVisible by remember { mutableStateOf(true) }
  var lastInteractionTick by remember { mutableLongStateOf(0L) }
  var postCallRating by remember { mutableIntStateOf(0) }
  var postCallFriendSent by remember { mutableStateOf(false) }
  var showPostCallReportDialog by remember { mutableStateOf(false) }
  var postCallReportSubmitted by remember { mutableStateOf(false) }
  var showInCallSafetyMenu by remember { mutableStateOf(false) }
  var showInCallBlockConfirm by remember { mutableStateOf(false) }

  val effectiveVideoCall = isInitialVideo && !downgradedToAudioOnly

  fun checkHardwareAndStartIfPermitted() {
    if (!hasMicHardware) {
      hardwareCheckError = "No microphone hardware detected on this device. Voice and video calls require a working microphone."
      return
    }
    if (effectiveVideoCall && !hasCameraHardware) {
      hardwareCheckError = "No camera hardware detected on this device."
      return
    }
    hardwareCheckError = null
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    hasMicPermission = micGranted
    hasCameraPermission = camGranted

    if (micGranted && (!effectiveVideoCall || camGranted)) {
      if (!sessionStarted) {
        sessionStarted = true
        com.example.webrtc.ActiveCallForegroundService.start(
          context = context,
          roomId = room.roomId,
          partnerName = partnerDisplayName,
          callType = if (downgradedToAudioOnly) "voice" else room.callType
        )
        rtcManager.startSession()
        if (downgradedToAudioOnly && !isVideoMuted) {
          rtcManager.toggleVideo()
        }
      }
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val micGranted = permissions[Manifest.permission.RECORD_AUDIO]
      ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    val camGranted = permissions[Manifest.permission.CAMERA]
      ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

    hasMicPermission = micGranted
    hasCameraPermission = camGranted

    val requiredGranted = micGranted && (!isInitialVideo || camGranted)
    if (requiredGranted) {
      permissionDeniedOnce = false
      permissionPermanentlyBlocked = false
      checkHardwareAndStartIfPermitted()
    } else {
      permissionDeniedOnce = true
      val micBlocked = !micGranted && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
      val camBlocked = isInitialVideo && !camGranted && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
      permissionPermanentlyBlocked = micBlocked || camBlocked
    }
  }

  fun requestRequiredPermissions() {
    val needed = mutableListOf<String>()
    if (!hasMicPermission) needed.add(Manifest.permission.RECORD_AUDIO)
    if (isInitialVideo && !hasCameraPermission) needed.add(Manifest.permission.CAMERA)
    if (needed.isNotEmpty()) {
      permissionLauncher.launch(needed.toTypedArray())
    } else {
      checkHardwareAndStartIfPermitted()
    }
  }

  LaunchedEffect(Unit) {
    speechManager.stopListening()
    speechManager.stopSpeaking()
    requestRequiredPermissions()
  }

  var hasTerminatedCall by remember { mutableStateOf(false) }

  // Live Call Duration Timer: ONLY ticks when the real WebRTC connection is established
  val isCallConnected = rtcState == WebRtcState.CONNECTED && !room.isTerminalStatus()
  LaunchedEffect(isCallConnected) {
    if (isCallConnected) {
      didOutgoingTimeout = false
      didReconnectTimeout = false
      while (true) {
        delay(1000L)
        elapsedSeconds++
      }
    }
  }

  // Outgoing 45-second timeout: if partner never answers, cancel cleanly and show calm "No answer" card
  LaunchedEffect(isCaller, isCallConnected, room.status, hasTerminatedCall, hasMicPermission, hasCameraPermission, downgradedToAudioOnly) {
    val permsOk = hasMicPermission && (!effectiveVideoCall || hasCameraPermission) && hardwareCheckError == null
    if (!isCaller || !permsOk || isCallConnected || hasTerminatedCall) return@LaunchedEffect
    if (room.status !in listOf("ringing", "waiting")) return@LaunchedEffect
    delay(45_000L)
    if (!isCallConnected && !hasTerminatedCall) {
      hasTerminatedCall = true
      com.example.webrtc.ActiveCallForegroundService.stop(context)
      com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, room.roomId)
      rtcManager.closeSession()
      viewModel.cancelOutgoingCall(room.roomId)
      didOutgoingTimeout = true
    }
  }

  // 15-second reconnect watcher: if reconnecting takes too long, offer calm Try again / End call card
  LaunchedEffect(rtcState, hasTerminatedCall) {
    if (rtcState == WebRtcState.CONNECTED) {
      didReconnectTimeout = false
      return@LaunchedEffect
    }
    if (rtcState == WebRtcState.RECONNECTING && !hasTerminatedCall) {
      delay(15_000L)
      if (rtcState == WebRtcState.RECONNECTING && !hasTerminatedCall) {
        didReconnectTimeout = true
      }
    }
  }

  fun finalizeAndExitCall() {
    if (hasTerminatedCall) {
      onEndCall()
      return
    }
    hasTerminatedCall = true
    com.example.webrtc.ActiveCallForegroundService.stop(context)
    com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, room.roomId)
    rtcManager.closeSession()
    when {
      isCaller && !isCallConnected && (room.status == "ringing" || room.status == "waiting") && elapsedSeconds == 0L -> {
        viewModel.cancelOutgoingCall(room.roomId)
      }
      !isCaller && !isCallConnected && room.status == "ringing" && elapsedSeconds == 0L -> {
        viewModel.rejectIncomingCall(room.roomId, isBusy = false)
      }
      else -> {
        viewModel.endCall(room.roomId, elapsedSeconds)
      }
    }
    onEndCall()
  }

  fun endCallSession() {
    if (hasTerminatedCall) return
    // Show valid-call summary dialog only when the user talked for 30+ seconds
    if (elapsedSeconds >= 30L && !showCallEndSummaryDialog) {
      com.example.webrtc.ActiveCallForegroundService.stop(context)
      com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, room.roomId)
      rtcManager.closeSession()
      viewModel.endCall(room.roomId, elapsedSeconds)
      hasTerminatedCall = true
      showCallEndSummaryDialog = true
    } else {
      finalizeAndExitCall()
    }
  }

  // Handle auto-close if call is rejected, busy, cancelled, missed, or ended remotely
  LaunchedEffect(room.status, room.connectionState, room.endedAtEpochMs, rtcState) {
    if (hasTerminatedCall) return@LaunchedEffect
    val isRoomTerminal = room.isTerminalStatus() && room.status.lowercase() != "failed"
    val isRtcTerminal = rtcState in listOf(
      WebRtcState.CLOSED,
      WebRtcState.REJECTED,
      WebRtcState.BUSY,
      WebRtcState.CANCELLED,
      WebRtcState.MISSED
    )
    if (isRoomTerminal || isRtcTerminal) {
      com.example.webrtc.ActiveCallForegroundService.stop(context)
      com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, room.roomId)
      rtcManager.closeSession()
      hasTerminatedCall = true

      val statusLower = room.status.lowercase()
      val toastMsg = when {
        statusLower in listOf("rejected", "declined") || rtcState == WebRtcState.REJECTED -> "Partner is not available"
        statusLower == "busy" || rtcState == WebRtcState.BUSY -> "Partner is not available"
        statusLower == "missed" || rtcState == WebRtcState.MISSED -> "Partner is not available"
        statusLower == "cancelled" || rtcState == WebRtcState.CANCELLED -> "Call cancelled"
        else -> "$partnerDisplayName left the call"
      }
      android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()

      if (elapsedSeconds >= 30L) {
        showCallEndSummaryDialog = true
      } else {
        val isImmediateClose = statusLower in listOf("ended", "cancelled", "closed") ||
          rtcState in listOf(WebRtcState.CLOSED, WebRtcState.CANCELLED)
        if (!isImmediateClose) {
          delay(600L)
        }
        onEndCall()
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      com.example.webrtc.ActiveCallForegroundService.stop(context)
      com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, room.roomId)
      rtcManager.closeSession()
    }
  }

  LaunchedEffect(activeCallHangupSignal) {
    if (activeCallHangupSignal != null) {
      com.example.webrtc.IncomingCallRegistry.consumeActiveCallHangup()
      endCallSession()
    }
  }

  BackHandler { endCallSession() }

  val formattedDuration = remember(elapsedSeconds) {
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    String.format("%02d:%02d", mins, secs)
  }

  val partnerInitials = remember(partnerDisplayName) {
    partnerDisplayName
      .trim()
      .split(" ")
      .filter { it.isNotEmpty() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "LP" }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "callPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (isCallConnected && !isPartnerMuted) 1.07f else 1.02f,
    animationSpec = infiniteRepeatable(
      animation = tween(850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  val permissionsReady = hasMicPermission && (!effectiveVideoCall || hasCameraPermission) && hardwareCheckError == null
  val isRobolectricEnv = remember {
    android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
  }

  // Weak-network auto-downgrade: drop video to audio-only when latency stays high (>650ms)
  LaunchedEffect(isCallConnected, latencyMs, isVideoMuted, downgradedToAudioOnly) {
    if (isCallConnected && !isRobolectricEnv && latencyMs > 650 && !isVideoMuted && !downgradedToAudioOnly) {
      delay(2_500L)
      if (isCallConnected && latencyMs > 650 && !isVideoMuted) {
        rtcManager.toggleVideo()
        downgradedToAudioOnly = true
        showWeakNetworkAudioBanner = true
      }
    }
  }

  // 4-second auto-hide controls during active live video calls (tap screen to restore)
  val isLiveFullVideoMode = isCallConnected &&
    !isRobolectricEnv &&
    !isVideoMuted &&
    !downgradedToAudioOnly &&
    !showToolsSheet &&
    !showInCallChat &&
    !showLiveAssistant &&
    !showTopicSelector &&
    !didOutgoingTimeout &&
    !didReconnectTimeout

  LaunchedEffect(isLiveFullVideoMode, areVideoControlsVisible, lastInteractionTick) {
    if (!isLiveFullVideoMode) {
      areVideoControlsVisible = true
    } else if (areVideoControlsVisible) {
      delay(4_000L)
      areVideoControlsVisible = false
    }
  }

  var isWebViewSurfaceReady by remember(permissionsReady) {
    mutableStateOf(isRobolectricEnv && permissionsReady)
  }
  LaunchedEffect(permissionsReady) {
    if (permissionsReady && !isWebViewSurfaceReady) {
      androidx.compose.runtime.withFrameNanos { }
      delay(90L)
      isWebViewSurfaceReady = true
    }
  }

  // Live Assistant & Captions Data
  var isAnalyzingAssistant by remember { mutableStateOf(false) }
  var liveCaptionsList by remember {
    mutableStateOf(
      listOf(
        com.example.data.model.LiveCallCaptionItem(
          speakerName = partnerDisplayName,
          originalText = "¡Hola! Qué alegría practicar contigo hoy. ¿Hablamos sobre ${room.conversationTopic.ifBlank { "viajes y cultura" }}?",
          translatedCaption = "Hello! Great practicing with you today. Shall we talk about ${room.conversationTopic.ifBlank { "travel and culture" }}?",
          language = targetLang.name
        )
      )
    )
  }
  var liveVocabExplanations by remember {
    mutableStateOf(
      listOf(
        com.example.data.model.LiveCallVocabExplanation(
          term = "a propósito",
          translation = "by the way / speaking of which",
          phoneticHint = "ah proh-POH-see-toh",
          contextNote = "Smooth connector to bring up a new angle during a call",
          exampleSentence = "A propósito, ¿qué recomiendas visitar en tu ciudad?"
        ),
        com.example.data.model.LiveCallVocabExplanation(
          term = "hoy en día",
          translation = "nowadays / these days",
          phoneticHint = "oy ehn DEE-ah",
          contextNote = "High-frequency conversational time expression",
          exampleSentence = "Hoy en día es muy fácil practicar idiomas en vivo."
        )
      )
    )
  }
  var suggestedCallPhrases by remember {
    mutableStateOf(
      listOf(
        com.example.data.model.SuggestedCallPhrase(
          targetPhrase = "¡Qué interesante! Cuéntame un poco más sobre tu experiencia.",
          nativeMeaning = "How interesting! Tell me a bit more about your experience.",
          pronunciationGuide = "KEH een-teh-reh-SAHN-teh! KWEN-tah-meh oon POH-koh mahs...",
          usageTip = "Encourages your partner to elaborate naturally"
        ),
        com.example.data.model.SuggestedCallPhrase(
          targetPhrase = "¿Cómo se expresa esa idea de forma coloquial en tu país?",
          nativeMeaning = "How is that idea expressed colloquially in your country?",
          pronunciationGuide = "KOH-moh seh eks-PREH-sah EH-sah ee-DEH-ah...",
          usageTip = "Great for learning authentic local idioms"
        )
      )
    )
  }
  var savedCallExpressions by remember { mutableStateOf<Set<String>>(emptySet()) }

  val inCallChatFlow = remember(partnerId) {
    if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
      viewModel.observeChatMessages(partnerId)
    } else {
      null
    }
  }
  val inCallMessages = inCallChatFlow?.collectAsState(initial = emptyList())?.value ?: emptyList()

  // Automatically translate & analyze incoming partner chat messages for the Live Assistant
  LaunchedEffect(inCallMessages.size) {
    val latestMsg = inCallMessages.lastOrNull()
    if (latestMsg != null && liveCaptionsEnabled) {
      isAnalyzingAssistant = true
      val speakerLabel = if (latestMsg.senderId == viewModel.userId) userDisplayName else partnerDisplayName
      val (caption, vocab, phrases) = GeminiTutorService.analyzeLiveCallUtterance(
        speakerName = speakerLabel,
        utteranceText = latestMsg.text,
        targetLanguage = targetLang.name,
        nativeLanguage = partnerLearningLang.name,
        conversationTopic = room.conversationTopic.ifBlank { "Language Exchange" }
      )
      liveCaptionsList = (liveCaptionsList + caption).takeLast(6)
      if (vocab.isNotEmpty()) liveVocabExplanations = vocab
      if (phrases.isNotEmpty()) suggestedCallPhrases = phrases
      isAnalyzingAssistant = false
    }
  }

  val isPoorConnection = (isCallConnected && latencyMs > 350) || rtcState == WebRtcState.RECONNECTING

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("live_user_call_screen"),
    color = LingoTokens.canvasNavy
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF0A1428),
              Color(0xFF0B0F1A),
              Color(0xFF060A12)
            )
          )
        )
    ) {
      val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(320f)
      val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(520f)

      val pipWidthDp = if (isLargePip) 150.dp else 110.dp
      val pipHeightDp = if (isLargePip) 204.dp else 150.dp
      val pipWidthPx = with(density) { pipWidthDp.toPx() }
      val pipHeightPx = with(density) { pipHeightDp.toPx() }

      // Safe bounds: below slim top bar (84.dp) and above bottom controls + captions (210.dp)
      val edgeMarginPx = with(density) { 16.dp.toPx() }
      val safeTopPx = with(density) { 88.dp.toPx() }
      val safeBottomReservePx = with(density) {
        if (liveCaptionsEnabled) 224.dp.toPx() else 148.dp.toPx()
      }

      val minX = edgeMarginPx
      val maxX = (containerWidthPx - pipWidthPx - edgeMarginPx).coerceAtLeast(minX)
      val minY = safeTopPx
      val maxY = (containerHeightPx - pipHeightPx - safeBottomReservePx).coerceAtLeast(minY)

      fun cornerTargetX(corner: SelfViewPipCorner): Float = when (corner) {
        SelfViewPipCorner.TOP_LEFT, SelfViewPipCorner.BOTTOM_LEFT -> minX
        SelfViewPipCorner.TOP_RIGHT, SelfViewPipCorner.BOTTOM_RIGHT -> maxX
      }

      fun cornerTargetY(corner: SelfViewPipCorner): Float = when (corner) {
        SelfViewPipCorner.TOP_LEFT, SelfViewPipCorner.TOP_RIGHT -> minY
        SelfViewPipCorner.BOTTOM_LEFT, SelfViewPipCorner.BOTTOM_RIGHT -> maxY
      }

      var dragOffsetX by remember { mutableFloatStateOf(cornerTargetX(pipCorner)) }
      var dragOffsetY by remember { mutableFloatStateOf(cornerTargetY(pipCorner)) }

      // Re-clamp on rotation, window resize, size toggle, or corner snap
      LaunchedEffect(containerWidthPx, containerHeightPx, pipWidthPx, pipHeightPx, pipCorner, safeBottomReservePx) {
        if (!isDraggingPip) {
          dragOffsetX = cornerTargetX(pipCorner).coerceIn(minX, maxX)
          dragOffsetY = cornerTargetY(pipCorner).coerceIn(minY, maxY)
        }
      }

      val animatedPipX by animateFloatAsState(
        targetValue = if (isDraggingPip) dragOffsetX.coerceIn(minX, maxX) else cornerTargetX(pipCorner).coerceIn(minX, maxX),
        animationSpec = tween(durationMillis = if (isDraggingPip) 0 else 200, easing = FastOutSlowInEasing),
        label = "pipX"
      )
      val animatedPipY by animateFloatAsState(
        targetValue = if (isDraggingPip) dragOffsetY.coerceIn(minY, maxY) else cornerTargetY(pipCorner).coerceIn(minY, maxY),
        animationSpec = tween(durationMillis = if (isDraggingPip) 0 else 200, easing = FastOutSlowInEasing),
        label = "pipY"
      )
      val pipDragScale by animateFloatAsState(
        targetValue = if (isDraggingPip) 1.06f else 1f,
        animationSpec = tween(150),
        label = "pipScale"
      )

      // Sync WebView local/remote video geometry with Compose PiP coordinates
      LaunchedEffect(
        animatedPipX,
        animatedPipY,
        pipWidthDp,
        pipHeightDp,
        isLocalVideoMirrored,
        areViewsSwapped,
        isSelfViewHidden,
        isWebViewSurfaceReady
      ) {
        if (isWebViewSurfaceReady) {
          val leftCss = with(density) { animatedPipX.toDp().value }
          val topCss = with(density) { animatedPipY.toDp().value }
          rtcManager.syncSelfViewLayout(
            leftCssPx = leftCss,
            topCssPx = topCss,
            widthCssPx = pipWidthDp.value,
            heightCssPx = pipHeightDp.value,
            mirrored = isLocalVideoMirrored,
            swapped = areViewsSwapped,
            hidden = isSelfViewHidden
          )
        }
      }

      // 1. Underlying Real WebRTC Media Engine WebView (renders remote video + local PiP + plays WebRTC audio)
      if (permissionsReady && isWebViewSurfaceReady) {
        AndroidView(
          modifier = Modifier.fillMaxSize(),
          factory = { ctx ->
            WebView(ctx).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              setBackgroundColor(android.graphics.Color.TRANSPARENT)
              setLayerType(android.view.View.LAYER_TYPE_NONE, null)
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.mediaPlaybackRequiresUserGesture = false
              settings.allowFileAccess = false
              settings.cacheMode = WebSettings.LOAD_NO_CACHE

              addJavascriptInterface(rtcManager, "AndroidBridge")

              webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): android.graphics.Bitmap {
                  return android.graphics.Bitmap.createBitmap(
                    1,
                    1,
                    android.graphics.Bitmap.Config.ARGB_8888
                  )
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                  request ?: return
                  post {
                    try {
                      val micOk = ContextCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.RECORD_AUDIO
                      ) == PackageManager.PERMISSION_GRANTED
                      val camOk = ContextCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.CAMERA
                      ) == PackageManager.PERMISSION_GRANTED
                      val allowed = mutableListOf<String>()
                      for (res in request.resources) {
                        when (res) {
                          PermissionRequest.RESOURCE_AUDIO_CAPTURE -> if (micOk) allowed.add(res)
                          PermissionRequest.RESOURCE_VIDEO_CAPTURE -> if (camOk) allowed.add(res)
                          else -> allowed.add(res)
                        }
                      }
                      if (allowed.isNotEmpty()) {
                        request.grant(allowed.toTypedArray())
                      } else {
                        request.deny()
                      }
                    } catch (_: Exception) {}
                  }
                }
              }

              webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(
                  view: WebView?,
                  detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                  android.util.Log.w(
                    "LiveUserCallScreen",
                    "WebView renderer process terminated (didCrash=${detail?.didCrash()}); detaching safely."
                  )
                  rtcManager.detachWebView(view)
                  runCatching {
                    (view?.parent as? ViewGroup)?.removeView(view)
                    view?.destroy()
                  }
                  return true
                }
              }

              rtcManager.attachWebView(this)
              loadDataWithBaseURL(
                "https://lingosphere.aistudio.local",
                WebRtcHtmlTemplate.buildHtml(),
                "text/html",
                "UTF-8",
                null
              )
            }
          },
          onRelease = { wv ->
            rtcManager.detachWebView(wv)
            runCatching {
              wv.stopLoading()
              wv.removeJavascriptInterface("AndroidBridge")
              wv.loadUrl("about:blank")
              wv.onPause()
              wv.removeAllViews()
              wv.destroy()
            }
          }
        )
      }

      // 2. Foreground Call Interface Overlay (Slim Top Bar, Center Stage, Captions, Tools Button, 56dp Control Bar)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // --- Slim Top Bar (auto-hides after 4s in active video call; tap stage to show) ---
        AnimatedVisibility(
          visible = areVideoControlsVisible || !isLiveFullVideoMode,
          enter = fadeIn(tween(200)),
          exit = fadeOut(tween(200))
        ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color(0xFF121826).copy(alpha = 0.88f),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("live_call_top_bar")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = { endCallSession() },
              modifier = Modifier
                .size(48.dp)
                .testTag("live_call_minimize_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Minimize or leave call",
                tint = LingoTokens.textPrimary
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = partnerDisplayName,
                  color = LingoTokens.textPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("live_call_partner_name")
                )
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = LingoTokens.surfaceElevated,
                  border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.4f)),
                  modifier = Modifier.testTag("live_call_language_pair_badge")
                ) {
                  Text(
                    text = languagePairLabel,
                    color = LingoTokens.brandCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = "You: $userDisplayName",
                color = LingoTokens.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("live_call_user_display_name")
              )
            }

            val statusText = when {
              !permissionsReady -> "Permissions needed"
              mediaErrorMessage != null -> "Device error"
              room.status == "rejected" || room.status == "declined" || rtcState == WebRtcState.REJECTED -> "Declined"
              room.status == "busy" || rtcState == WebRtcState.BUSY -> "Busy"
              room.status == "cancelled" || rtcState == WebRtcState.CANCELLED -> "Cancelled"
              room.status == "missed" || rtcState == WebRtcState.MISSED -> "No answer"
              room.status == "ended" || room.status == "closed" || rtcState == WebRtcState.CLOSED -> "Ended"
              rtcState == WebRtcState.RECONNECTING -> "Reconnecting..."
              rtcState == WebRtcState.FAILED || room.status == "failed" -> "Reconnect"
              isCallConnected -> formattedDuration
              room.status == "accepted" || rtcState == WebRtcState.NEGOTIATING || rtcState == WebRtcState.CONNECTING -> "Connecting..."
              room.status == "ringing" && room.lastSignalingEvent == "call_ringing" -> "Ringing..."
              room.status == "ringing" -> if (isCaller) "Calling..." else "Ringing..."
              room.status == "waiting" -> "Calling..."
              else -> rtcState.displayLabel
            }

            val statusColor = when {
              isCallConnected -> LingoEmerald
              rtcState == WebRtcState.RECONNECTING -> LingoAmber
              rtcState == WebRtcState.FAILED || mediaErrorMessage != null ||
                room.status in listOf("rejected", "busy", "missed", "failed") -> LingoCoral
              else -> LingoCyan
            }

            Surface(
              shape = RoundedCornerShape(999.dp),
              color = statusColor.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
              modifier = Modifier
                .clickable(enabled = rtcState == WebRtcState.FAILED || room.status == "failed") {
                  rtcManager.triggerConnectionRecovery()
                }
                .testTag("live_call_status_pill")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                )
                Text(
                  text = statusText,
                  color = statusColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            if (partnerId.isNotBlank()) {
              Box {
                IconButton(
                  onClick = { showInCallSafetyMenu = true },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("live_call_safety_menu_btn")
                ) {
                  Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Safety options",
                    tint = LingoTokens.textSecondary
                  )
                }
                DropdownMenu(
                  expanded = showInCallSafetyMenu,
                  onDismissRequest = { showInCallSafetyMenu = false },
                  containerColor = Color(0xFF101828)
                ) {
                  DropdownMenuItem(
                    text = { Text("Report user", color = LingoTokens.textPrimary, fontSize = 14.sp) },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = LingoAmber,
                        modifier = Modifier.size(18.dp)
                      )
                    },
                    onClick = {
                      showInCallSafetyMenu = false
                      showPostCallReportDialog = true
                    },
                    modifier = Modifier.testTag("live_call_report_action")
                  )
                  DropdownMenuItem(
                    text = { Text("Block user", color = LingoCoral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = LingoCoral,
                        modifier = Modifier.size(18.dp)
                      )
                    },
                    onClick = {
                      showInCallSafetyMenu = false
                      showInCallBlockConfirm = true
                    },
                    modifier = Modifier.testTag("live_call_block_action")
                  )
                }
              }
            }
          }
        }
        }

        // Weak-network automatic audio-only downgrade banner
        AnimatedVisibility(visible = showWeakNetworkAudioBanner) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF121826).copy(alpha = 0.94f),
            border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.55f)),
            modifier = Modifier
              .padding(top = 6.dp)
              .fillMaxWidth()
              .testTag("call_weak_network_audio_banner")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = null,
                tint = LingoAmber,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Switched to voice only due to weak connection",
                color = LingoTokens.textPrimary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
              )
              TextButton(
                onClick = {
                  downgradedToAudioOnly = false
                  showWeakNetworkAudioBanner = false
                  if (isVideoMuted && hasCameraPermission) {
                    rtcManager.toggleVideo()
                  }
                }
              ) {
                Text("Video on", color = LingoTokens.brandCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Show "Poor connection" pill ONLY when latency is high or reconnecting
        AnimatedVisibility(visible = isPoorConnection) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = LingoAmber.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.6f)),
            modifier = Modifier
              .padding(top = 6.dp)
              .testTag("call_poor_connection_pill")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = LingoAmber,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = if (rtcState == WebRtcState.RECONNECTING) "Reconnecting..." else "Weak connection",
                color = LingoAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        // Active conversation topic pill (slim, only if chosen)
        if (room.conversationTopic.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF101C36).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.5f)),
            modifier = Modifier
              .padding(top = 6.dp)
              .fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = LingoAmber,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Topic: ${room.conversationTopic}",
                color = LingoTokens.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // --- Center Stage: Timeout / Reconnect / Permission Cards OR Full-Screen Video / Partner Avatar Stage ---
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clickable(
              enabled = isLiveFullVideoMode,
              indication = null,
              interactionSource = remember { MutableInteractionSource() }
            ) {
              areVideoControlsVisible = !areVideoControlsVisible
              lastInteractionTick++
            },
          contentAlignment = Alignment.Center
        ) {
          when {
            // Case 0: Outgoing 45s timeout -> calm "No answer" card
            didOutgoingTimeout -> {
              Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101828)),
                border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp)
                  .testTag("call_no_answer_card")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.padding(24.dp)
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(72.dp)
                      .clip(CircleShape)
                      .background(LingoTokens.bgSubtle)
                      .border(1.5.dp, LingoTokens.borderSubtle, CircleShape)
                  ) {
                    Text(
                      text = partnerInitials,
                      color = LingoTokens.textPrimary,
                      fontSize = 24.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Text(
                    text = "No answer",
                    color = LingoTokens.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "$partnerDisplayName didn't pick up right now.",
                    color = LingoTokens.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    OutlinedButton(
                      onClick = {
                        if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
                          viewModel.sendDirectMessage(
                            receiverId = partnerId,
                            text = "Hey! I tried calling to practice together. Message me when you're free!"
                          )
                          android.widget.Toast.makeText(
                            context,
                            "Message sent to $partnerDisplayName",
                            android.widget.Toast.LENGTH_SHORT
                          ).show()
                        }
                        onEndCall()
                      },
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("call_no_answer_message_btn")
                    ) {
                      Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = LingoTokens.textPrimary)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Message", color = LingoTokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                      onClick = {
                        coroutineScope.launch {
                          runCatching {
                            val retryRoom = viewModel.startOrJoinCallRoom(
                              calleeId = partnerId,
                              calleeName = partnerDisplayName,
                              callType = room.callType,
                              matchMode = "direct",
                              targetLanguageOverride = room.targetLanguageCode,
                              conversationTopic = room.conversationTopic
                            )
                            onCallAgain(retryRoom)
                          }.onFailure {
                            onEndCall()
                          }
                        }
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = LingoTokens.brandCyan,
                        contentColor = LingoTokens.onGradientInk
                      ),
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("call_no_answer_retry_btn")
                    ) {
                      Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Try again", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            // Case 0b: 15s reconnect timeout -> calm "Connection lost" card
            didReconnectTimeout -> {
              Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101828)),
                border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.55f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp)
                  .testTag("call_connection_lost_card")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.padding(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LingoAmber,
                    modifier = Modifier.size(36.dp)
                  )
                  Text(
                    text = "Connection lost",
                    color = LingoTokens.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "We couldn't reconnect automatically. Check your internet and try again.",
                    color = LingoTokens.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                  )
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    OutlinedButton(
                      onClick = { endCallSession() },
                      border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.65f)),
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                    ) {
                      Text("End call", color = LingoCoral, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                      onClick = {
                        didReconnectTimeout = false
                        rtcManager.triggerConnectionRecovery()
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = LingoTokens.brandCyan,
                        contentColor = LingoTokens.onGradientInk
                      ),
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                    ) {
                      Text("Try again", fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            // Case A: Hardware Missing or Permission Denied / Blocked
            !permissionsReady || mediaErrorMessage != null -> {
              Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LingoTokens.surfaceCard),
                border = BorderStroke(1.5.dp, LingoCoral),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp)
                  .testTag("call_error_state_card")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(20.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LingoCoral,
                    modifier = Modifier.size(38.dp)
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = when {
                      hardwareCheckError != null -> "Video calls are not available yet"
                      mediaErrorMessage != null -> "Microphone / Camera Error"
                      permissionPermanentlyBlocked -> "Permissions Blocked"
                      else -> "Microphone & Camera Access Needed"
                    },
                    color = LingoTokens.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = hardwareCheckError
                      ?: mediaErrorMessage
                      ?: if (permissionPermanentlyBlocked) {
                        "Microphone or camera permission was denied. Enable permissions in App Settings to join the call."
                      } else {
                        "Allow microphone${if (effectiveVideoCall) " and camera" else ""} access to start your live call."
                      },
                    color = LingoTokens.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                  )
                  Spacer(modifier = Modifier.height(16.dp))

                  Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    if (permissionPermanentlyBlocked) {
                      Button(
                        onClick = {
                          try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                              data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                          } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                      ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                      }
                    }

                    Button(
                      onClick = {
                        if (mediaErrorMessage != null) {
                          rtcManager.retryMediaAndConnection()
                        } else {
                          requestRequiredPermissions()
                        }
                      },
                      enabled = hardwareCheckError == null,
                      colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color.Black),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("call_error_retry_button")
                    ) {
                      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  // Camera denied fallback: let the user continue immediately as a voice call when mic is granted
                  if (effectiveVideoCall && hasMicPermission && !hasCameraPermission && hasMicHardware) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                      onClick = {
                        downgradedToAudioOnly = true
                        hardwareCheckError = null
                        if (!sessionStarted) {
                          sessionStarted = true
                          com.example.webrtc.ActiveCallForegroundService.start(
                            context = context,
                            roomId = room.roomId,
                            partnerName = partnerDisplayName,
                            callType = "voice"
                          )
                          rtcManager.startSession()
                          if (!isVideoMuted) {
                            rtcManager.toggleVideo()
                          }
                        }
                      },
                      border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.6f)),
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("call_fallback_voice_only_btn")
                    ) {
                      Icon(Icons.Default.Call, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Continue with voice only", color = LingoTokens.brandCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            // Case B: When views are swapped (Self is full screen) and self camera is off, show Self Avatar full-screen
            areViewsSwapped && (isVideoMuted || !hasCameraPermission) -> {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("full_screen_self_avatar_stage")
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(LingoTokens.brandIndigo, LingoTokens.brandCyan)))
                    .border(2.5.dp, LingoTokens.brandCyan, CircleShape)
                ) {
                  Text(
                    text = userInitials,
                    color = LingoTokens.onGradientInk,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "$userDisplayName (You)",
                  color = LingoTokens.textPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = Color.Black.copy(alpha = 0.6f),
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle)
                ) {
                  Text(
                    text = "Camera off · Tap PiP to swap back",
                    color = LingoTokens.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
            }

            // Case C: Show Partner Avatar Stage ONLY while Calling/Ringing or when remote camera is off
            !areViewsSwapped && (!isRemoteStreamAvailable || !isPartnerVideoEnabled) -> {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("remote_partner_avatar_stage")
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(124.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                      Brush.linearGradient(
                        if (isCallConnected) listOf(LingoCyan, LingoElectricBlue)
                        else listOf(Color(0xFF1E3A8A), Color(0xFF0F2952))
                      )
                    )
                    .border(
                      2.5.dp,
                      if (isCallConnected) LingoEmerald else LingoCyan,
                      CircleShape
                    )
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = partnerInitials,
                      color = Color.White,
                      fontSize = 36.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                      text = targetLang.flagEmoji,
                      fontSize = 18.sp
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                  text = partnerDisplayName,
                  color = LingoTokens.textPrimary,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = when {
                    !isCallConnected -> "Calling..."
                    !isPartnerVideoEnabled -> "Partner camera off"
                    isPartnerMuted -> "Partner microphone muted"
                    else -> "Voice connected"
                  },
                  color = LingoTokens.textSecondary,
                  fontSize = 13.sp
                )

                if (rtcState == WebRtcState.FAILED) {
                  Spacer(modifier = Modifier.height(12.dp))
                  Button(
                    onClick = { rtcManager.triggerConnectionRecovery() },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = LingoEmerald,
                      contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Reconnect Call",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                // Voice Call Mode: collapsed-by-default "Conversation topic" glass card with 3 curated topics + starters
                if (!effectiveVideoCall || isVideoMuted) {
                  Spacer(modifier = Modifier.height(18.dp))
                  val curatedVoiceTopics = remember {
                    listOf(
                      "Travel & dream trip" to "Where would you travel next if you had a free week?",
                      "Daily routine & food" to "What is a favorite local meal you could eat every week?",
                      "Movies, music & hobbies" to "What song, show, or book have you enjoyed lately?"
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 8.dp)
                      .testTag("voice_call_topic_toggle_card")
                  ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable { isVoiceTopicCardExpanded = !isVoiceTopicCardExpanded }
                          .padding(vertical = 4.dp)
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                          modifier = Modifier.weight(1f)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = LingoTokens.brandCyan,
                            modifier = Modifier.size(18.dp)
                          )
                          Text(
                            text = if (room.conversationTopic.isNotBlank()) "Topic: ${room.conversationTopic}" else "Need a conversation topic?",
                            color = LingoTokens.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                        Icon(
                          imageVector = if (isVoiceTopicCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                          contentDescription = if (isVoiceTopicCardExpanded) "Collapse topics" else "Expand topics",
                          tint = LingoTokens.textSecondary,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                      AnimatedVisibility(visible = isVoiceTopicCardExpanded) {
                        Column(
                          verticalArrangement = Arrangement.spacedBy(8.dp),
                          modifier = Modifier.padding(top = 8.dp)
                        ) {
                          curatedVoiceTopics.forEach { (title, starter) ->
                            val isSelected = room.conversationTopic == title
                            Surface(
                              onClick = {
                                viewModel.updateCallRoomState(room.roomId, conversationTopic = title)
                                isVoiceTopicCardExpanded = false
                              },
                              shape = RoundedCornerShape(12.dp),
                              color = if (isSelected) LingoTokens.brandIndigo.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                              border = BorderStroke(1.dp, if (isSelected) LingoTokens.brandCyan else LingoTokens.borderSubtle),
                              modifier = Modifier.fillMaxWidth()
                            ) {
                              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                  text = title,
                                  color = LingoTokens.textPrimary,
                                  fontSize = 13.sp,
                                  fontWeight = FontWeight.Bold
                                )
                                Text(
                                  text = starter,
                                  color = LingoTokens.textSecondary,
                                  fontSize = 12.sp,
                                  lineHeight = 16.sp
                                )
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // Topic Picker Overlay Sheet
          this@Column.AnimatedVisibility(visible = showTopicSelector) {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF101C36).copy(alpha = 0.98f)),
              border = BorderStroke(1.dp, LingoCyan),
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "Select Conversation Topic",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                  )
                  IconButton(onClick = { showTopicSelector = false }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                CountryInfo.CONVERSATION_STARTERS.forEach { topic ->
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(
                        if (room.conversationTopic == topic) LingoElectricBlue else Color(0xFF162447)
                      )
                      .clickable {
                        viewModel.updateCallRoomState(room.roomId, conversationTopic = topic)
                        showTopicSelector = false
                      }
                      .padding(horizontal = 12.dp, vertical = 10.dp)
                  ) {
                    Text(
                      text = topic,
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            }
          }

          // In-Call Real-Time Text Chat Overlay
          if (showInCallChat && partnerId.isNotBlank() && partnerId != "waiting_partner") {
            val chatFlow = remember(partnerId) { viewModel.observeChatMessages(partnerId) }
            val chatMessages by chatFlow.collectAsState(initial = emptyList())
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1528).copy(alpha = 0.98f)),
              border = BorderStroke(1.dp, LingoCyan),
              modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
            ) {
              Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "In-Call Chat · $partnerDisplayName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  )
                  IconButton(onClick = { showInCallChat = false }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = Color.White)
                  }
                }

                LazyColumn(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  contentPadding = PaddingValues(vertical = 8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(chatMessages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == viewModel.userId
                    val translated = translatedMessages[msg.id] ?: msg.translatedText
                    Column(
                      horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(14.dp))
                          .background(if (isMine) LingoElectricBlue else Color(0xFF162447))
                          .padding(horizontal = 12.dp, vertical = 8.dp)
                      ) {
                        Column {
                          Text(msg.text, color = Color.White, fontSize = 14.sp)
                          if (!translated.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                              text = translated,
                              color = LingoCyan,
                              fontSize = 12.sp,
                              fontWeight = FontWeight.Medium
                            )
                          }
                        }
                      }
                      if (translated.isNullOrBlank()) {
                        Text(
                          text = if (isTranslatingMsgId == msg.id) "Translating..." else "Translate",
                          color = LingoTokens.textSecondary,
                          fontSize = 11.sp,
                          modifier = Modifier
                            .padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            .clickable {
                              if (isTranslatingMsgId == null) {
                                isTranslatingMsgId = msg.id
                                coroutineScope.launch {
                                  val resp = GeminiTutorService.chatWithTutor(
                                    userSpeechInput = "Translate this phrase into ${partnerLearningLang.name}: ${msg.text}",
                                    targetLanguage = targetLang,
                                    nativeLanguage = partnerLearningLang,
                                    proficiencyLevel = "Intermediate"
                                  )
                                  val result = resp.translationInNativeLanguage
                                  if (result.isNotBlank()) {
                                    translatedMessages = translatedMessages + (msg.id to result)
                                  }
                                  isTranslatingMsgId = null
                                }
                              }
                            }
                        )
                      }
                    }
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text("Write a phrase or note...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  IconButton(
                    onClick = {
                      val trimmed = chatInputText.trim()
                      if (trimmed.isNotEmpty()) {
                        viewModel.sendDirectMessage(partnerId, trimmed)
                        chatInputText = ""
                      }
                    },
                    modifier = Modifier.size(48.dp)
                  ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = LingoCyan)
                  }
                }
              }
            }
          }
        }

        // Live Conversation Assistant Panel (when toggled on via Tools)
        AnimatedVisibility(visible = showLiveAssistant) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101C36).copy(alpha = 0.96f)),
            border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.55f)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 260.dp)
              .padding(bottom = 8.dp)
              .testTag("live_conversation_assistant_panel")
          ) {
            Column(
              modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Live Conversation Assistant",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  FilterChip(
                    selected = liveCaptionsEnabled,
                    onClick = { liveCaptionsEnabled = !liveCaptionsEnabled },
                    label = {
                      Text(
                        if (liveCaptionsEnabled) "Captions ON" else "Captions OFF",
                        fontSize = 11.sp
                      )
                    },
                    modifier = Modifier.testTag("toggle_live_captions_chip")
                  )
                  IconButton(
                    onClick = { showLiveAssistant = false },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Close assistant", tint = LingoTokens.textSecondary, modifier = Modifier.size(18.dp))
                  }
                }
              }

              // Unfamiliar Vocabulary Explanations + Save to Collection
              Text(
                text = "VOCABULARY & PHRASES",
                color = LingoAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              liveVocabExplanations.forEach { vocab ->
                val isSaved = savedCallExpressions.contains(vocab.term)
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFF162447),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(8.dp)
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${vocab.term} — ${vocab.translation}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                      )
                      Text(
                        text = "/${vocab.phoneticHint}/ · ${vocab.contextNote}",
                        color = LingoTokens.textSecondary,
                        fontSize = 11.sp
                      )
                    }
                    OutlinedButton(
                      onClick = {
                        viewModel.saveVocabularyToFlashcards(
                          frontText = vocab.term,
                          backText = "${vocab.translation} (${vocab.contextNote})",
                          pronunciation = vocab.phoneticHint
                        )
                        savedCallExpressions = savedCallExpressions + vocab.term
                      },
                      enabled = !isSaved,
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.testTag("save_call_vocab_${vocab.term}")
                    ) {
                      Text(if (isSaved) "Saved ✓" else "+ Save", fontSize = 11.sp, color = if (isSaved) LingoEmerald else LingoCyan)
                    }
                  }
                }
              }

              suggestedCallPhrases.forEachIndexed { idx, sug ->
                val isSaved = savedCallExpressions.contains(sug.targetPhrase)
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFF162447),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = sug.targetPhrase,
                      color = Color.White,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 12.sp
                    )
                    Text(
                      text = "${sug.nativeMeaning} · /${sug.pronunciationGuide}/",
                      color = LingoTokens.textSecondary,
                      fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      TextButton(
                        onClick = {
                          viewModel.saveVocabularyToFlashcards(
                            frontText = sug.targetPhrase,
                            backText = sug.nativeMeaning,
                            pronunciation = sug.pronunciationGuide
                          )
                          savedCallExpressions = savedCallExpressions + sug.targetPhrase
                        },
                        enabled = !isSaved,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("save_call_phrase_btn_$idx")
                      ) {
                        Text(
                          if (isSaved) "Saved ✓" else "Save Expression",
                          fontSize = 11.sp,
                          color = if (isSaved) LingoEmerald else LingoCyan
                        )
                      }
                      if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
                        TextButton(
                          onClick = { viewModel.sendDirectMessage(partnerId, sug.targetPhrase) },
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                          Text("Send in Call", fontSize = 11.sp, color = LingoAmber)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // --- Live Captions with Translation Bar (directly above Tools & Control Bar) ---
        AnimatedVisibility(visible = liveCaptionsEnabled && liveCaptionsList.isNotEmpty()) {
          val latestCaption = liveCaptionsList.last()
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121826).copy(alpha = 0.90f),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
              .testTag("live_call_captions_bar")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${latestCaption.speakerName}: ${latestCaption.originalText}",
                  color = LingoTokens.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                if (translateCaptionsEnabled && latestCaption.translatedCaption.isNotBlank()) {
                  Text(
                    text = latestCaption.translatedCaption,
                    color = LingoTokens.brandCyan,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
              IconButton(
                onClick = { translateCaptionsEnabled = !translateCaptionsEnabled },
                modifier = Modifier
                  .size(40.dp)
                  .testTag("toggle_caption_translation_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.Translate,
                  contentDescription = "Toggle caption translation",
                  tint = if (translateCaptionsEnabled) LingoTokens.brandCyan else LingoTokens.textSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }

        // --- Bottom Tools & Control Bar (auto-hides after 4s in active video call; tap stage to show) ---
        AnimatedVisibility(
          visible = areVideoControlsVisible || !isLiveFullVideoMode,
          enter = fadeIn(tween(200)),
          exit = fadeOut(tween(200))
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
        // --- Single "Tools" Row (replaces the two crowded rows of action chips) ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              lastInteractionTick++
              showToolsSheet = true
            },
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFF121826).copy(alpha = 0.88f),
              contentColor = LingoTokens.textPrimary
            ),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("live_call_tools_button")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = LingoTokens.brandCyan,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Tools",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Quick toggle for Live AI Assistant (keeps testTag "toggle_live_call_assistant_btn" directly accessible)
          OutlinedButton(
            onClick = {
              lastInteractionTick++
              showLiveAssistant = !showLiveAssistant
            },
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, if (showLiveAssistant) LingoEmerald else LingoTokens.borderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFF121826).copy(alpha = 0.88f),
              contentColor = LingoTokens.textPrimary
            ),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("toggle_live_call_assistant_btn")
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = if (showLiveAssistant) LingoEmerald else LingoTokens.brandCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (showLiveAssistant) "Hide Assistant" else "AI Assistant",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // --- Bottom Control Bar: Mute, Speaker/Bluetooth, Video on/off, Flip camera, End call (56dp buttons, evenly spaced, with labels) ---
        Card(
          shape = RoundedCornerShape(28.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF121826).copy(alpha = 0.96f)),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("live_call_bottom_bar")
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 6.dp)
          ) {
            // 1. Mute
            CallControlButton(
              icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              label = if (isMuted) "Unmute" else "Mute",
              active = !isMuted,
              onClick = {
                lastInteractionTick++
                rtcManager.toggleMuteAudio()
              },
              testTag = "mute_audio_button"
            )

            // 2. Speaker / Bluetooth audio output picker
            CallControlButton(
              icon = when (activeAudioRoute) {
                CallAudioRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                CallAudioRoute.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                CallAudioRoute.EARPIECE -> Icons.AutoMirrored.Filled.VolumeDown
              },
              label = activeAudioRoute.label,
              active = activeAudioRoute != CallAudioRoute.EARPIECE,
              onClick = {
                lastInteractionTick++
                if (isBluetoothAvailable) {
                  showAudioRouteSheet = true
                } else {
                  rtcManager.cycleAudioRoute()
                }
              },
              testTag = "toggle_speaker_button"
            )

            // 3. Video on/off
            CallControlButton(
              icon = if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
              label = if (isVideoMuted) "Video Off" else "Video On",
              active = !isVideoMuted && hasCameraPermission,
              onClick = {
                lastInteractionTick++
                if (isVideoMuted && !hasCameraPermission) {
                  permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                } else {
                  downgradedToAudioOnly = false
                  showWeakNetworkAudioBanner = false
                  rtcManager.toggleVideo()
                }
              },
              testTag = "toggle_video_button"
            )

            // 4. Flip camera
            CallControlButton(
              icon = Icons.Default.Cameraswitch,
              label = "Flip Cam",
              active = !isVideoMuted && hasCameraPermission,
              onClick = {
                lastInteractionTick++
                rtcManager.switchCamera()
              },
              testTag = "flip_camera_button"
            )

            // 5. End call (red 56.dp button with label)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(LingoCoral)
                  .clickable { endCallSession() }
                  .testTag("end_user_call_button")
              ) {
                Icon(
                  imageVector = Icons.Default.CallEnd,
                  contentDescription = "End Call",
                  tint = Color.White,
                  modifier = Modifier.size(26.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "End Call",
                color = LingoCoral,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
          }
        }
      }

      // 3. Draggable Self-View Picture-in-Picture Window (or Edge Restore Button when hidden)
      if (!isSelfViewHidden) {
        val isShowingSelfInPip = !areViewsSwapped
        val isPipCameraOff = if (isShowingSelfInPip) {
          isVideoMuted || !hasCameraPermission
        } else {
          !isRemoteStreamAvailable || !isPartnerVideoEnabled
        }

        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                x = animatedPipX.roundToInt(),
                y = animatedPipY.roundToInt()
              )
            }
            .size(width = pipWidthDp, height = pipHeightDp)
            .scale(pipDragScale)
            .shadow(
              elevation = if (isDraggingPip) 18.dp else 8.dp,
              shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
              if (isPipCameraOff) {
                Brush.verticalGradient(listOf(Color(0xFF162447), Color(0xFF0E172A)))
              } else {
                Brush.verticalGradient(
                  listOf(
                    Color(0xFF0F1E3A).copy(alpha = 0.35f),
                    Color(0xFF091224).copy(alpha = 0.65f)
                  )
                )
              }
            )
            .border(2.dp, LingoTokens.brandCyan, RoundedCornerShape(16.dp))
            .pointerInput(minX, maxX, minY, maxY, pipCorner) {
              detectDragGestures(
                onDragStart = {
                  dragOffsetX = animatedPipX
                  dragOffsetY = animatedPipY
                  isDraggingPip = true
                },
                onDragEnd = {
                  isDraggingPip = false
                  val midX = (minX + maxX) / 2f
                  val midY = (minY + maxY) / 2f
                  val snappedCorner = when {
                    dragOffsetX <= midX && dragOffsetY <= midY -> SelfViewPipCorner.TOP_LEFT
                    dragOffsetX > midX && dragOffsetY <= midY -> SelfViewPipCorner.TOP_RIGHT
                    dragOffsetX <= midX && dragOffsetY > midY -> SelfViewPipCorner.BOTTOM_LEFT
                    else -> SelfViewPipCorner.BOTTOM_RIGHT
                  }
                  savePipPreferences(snappedCorner, isLargePip)
                },
                onDragCancel = {
                  isDraggingPip = false
                },
                onDrag = { change, dragAmount ->
                  change.consume()
                  dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(minX, maxX)
                  dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(minY, maxY)
                }
              )
            }
            .combinedClickable(
              onClick = {
                // Single tap: swap views (self full screen, partner in small window)
                rtcManager.toggleSwapViews()
              },
              onDoubleClick = {
                // Double tap: toggle small/large size
                savePipPreferences(pipCorner, !isLargePip)
              },
              onLongClick = {
                // Long press: open small menu (Hide self-view, Flip camera, Mirror on/off)
                showPipContextMenu = true
              }
            )
            .testTag("draggable_self_view_pip")
        ) {
          // Content inside floating PiP window
          if (isPipCameraOff) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(if (isLargePip) 56.dp else 44.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      listOf(LingoTokens.brandIndigo, LingoTokens.brandCyan)
                    )
                  )
              ) {
                Text(
                  text = if (isShowingSelfInPip) userInitials else partnerInitials,
                  color = LingoTokens.onGradientInk,
                  fontSize = if (isLargePip) 18.sp else 15.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("self_view_camera_off_badge")
              ) {
                Text(
                  text = "Camera off",
                  color = LingoCoral,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
              }
            }
          } else {
            // Live camera stream is active underneath in WebView; display subtle mirrored/camera badge
            Box(
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = if (isShowingSelfInPip && isLocalVideoMirrored) -1f else 1f
                }
            )
          }

          // Top-right quick collapse button + Top-left label badge
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter)
              .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color.Black.copy(alpha = 0.6f)
            ) {
              Text(
                text = if (isShowingSelfInPip) {
                  if (isLocalVideoMirrored) "You · Mirrored" else "You"
                } else {
                  partnerDisplayName.split(" ").firstOrNull() ?: "Partner"
                },
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { rtcManager.setSelfViewHidden(true) }
                .testTag("hide_self_view_button")
            ) {
              Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Hide self-view",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
              )
            }
          }

          // Bottom quick controls inside PiP (Swap / Menu trigger)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color.Black.copy(alpha = 0.58f),
              modifier = Modifier
                .clickable { rtcManager.toggleSwapViews() }
                .testTag("pip_swap_view_badge")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.SwapHoriz,
                  contentDescription = "Swap views",
                  tint = LingoTokens.brandCyan,
                  modifier = Modifier.size(12.dp)
                )
                Text(
                  text = "Swap",
                  color = Color.White,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color.Black.copy(alpha = 0.58f),
              modifier = Modifier
                .clickable { showPipContextMenu = true }
                .testTag("pip_options_menu_trigger")
            ) {
              Text(
                text = "•••",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          // Long-press / options menu (Hide self-view, Flip camera, Mirror on/off, Size toggle)
          DropdownMenu(
            expanded = showPipContextMenu,
            onDismissRequest = { showPipContextMenu = false },
            modifier = Modifier
              .background(Color(0xFF121826))
              .testTag("self_view_long_press_menu")
          ) {
            DropdownMenuItem(
              text = { Text("Hide self-view", color = LingoTokens.textPrimary, fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(18.dp))
              },
              onClick = {
                showPipContextMenu = false
                rtcManager.setSelfViewHidden(true)
              },
              modifier = Modifier.testTag("pip_menu_hide")
            )
            DropdownMenuItem(
              text = { Text("Flip camera", color = LingoTokens.textPrimary, fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.Cameraswitch, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(18.dp))
              },
              onClick = {
                showPipContextMenu = false
                rtcManager.switchCamera()
              },
              modifier = Modifier.testTag("pip_menu_flip_camera")
            )
            DropdownMenuItem(
              text = {
                Text(
                  text = if (isLocalVideoMirrored) "Mirror: ON" else "Mirror: OFF",
                  color = LingoTokens.textPrimary,
                  fontSize = 13.sp
                )
              },
              leadingIcon = {
                Icon(Icons.Default.Flip, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(18.dp))
              },
              onClick = {
                showPipContextMenu = false
                rtcManager.toggleLocalMirror()
              },
              modifier = Modifier.testTag("pip_menu_mirror_toggle")
            )
            DropdownMenuItem(
              text = {
                Text(
                  text = if (isLargePip) "Size: Standard (110×150)" else "Size: Large (150×204)",
                  color = LingoTokens.textPrimary,
                  fontSize = 13.sp
                )
              },
              leadingIcon = {
                Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(18.dp))
              },
              onClick = {
                showPipContextMenu = false
                savePipPreferences(pipCorner, !isLargePip)
              },
              modifier = Modifier.testTag("pip_menu_size_toggle")
            )
          }
        }
      } else {
        // Collapsed small camera icon tab at the screen edge; tap to restore self-view
        val isRightEdge = pipCorner == SelfViewPipCorner.TOP_RIGHT || pipCorner == SelfViewPipCorner.BOTTOM_RIGHT
        Surface(
          shape = if (isRightEdge) {
            RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
          } else {
            RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
          },
          color = Color(0xFF121826).copy(alpha = 0.94f),
          border = BorderStroke(1.5.dp, LingoTokens.brandCyan),
          modifier = Modifier
            .align(if (isRightEdge) Alignment.CenterEnd else Alignment.CenterStart)
            .clickable { rtcManager.setSelfViewHidden(false) }
            .testTag("restore_self_view_button")
        ) {
          Row(
            modifier = Modifier
              .heightIn(min = 48.dp)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = "Restore self-view",
              tint = LingoTokens.brandCyan,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Self",
              color = LingoTokens.textPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }

  // --- Unified "Tools" Bottom Sheet ---
  if (showToolsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showToolsSheet = false },
      containerColor = Color(0xFF121826),
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      modifier = Modifier.testTag("live_call_tools_sheet")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Call Tools",
          color = LingoTokens.textPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold
        )

        // 1. Live AI Assistant
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.surfaceElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              showLiveAssistant = !showLiveAssistant
              showToolsSheet = false
            }
            .testTag("tools_sheet_ai_assistant_item")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Live AI Assistant", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Real-time vocabulary explanations and next-phrase suggestions", color = LingoTokens.textSecondary, fontSize = 12.sp)
              }
            }
            Text(
              text = if (showLiveAssistant) "ON" else "OFF",
              color = if (showLiveAssistant) LingoEmerald else LingoTokens.textSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // 2. Conversation Topics
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.surfaceElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              showTopicSelector = true
              showToolsSheet = false
            }
            .testTag("tools_sheet_topics_item")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = LingoAmber, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Conversation Topics", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text(
                text = room.conversationTopic.ifBlank { "Pick a shared topic prompt for your call" },
                color = LingoTokens.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // 3. In-Call Chat
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.surfaceElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              showInCallChat = !showInCallChat
              showToolsSheet = false
            }
            .testTag("tools_sheet_chat_item")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("In-Call Chat", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Send text notes and translate messages during the call", color = LingoTokens.textSecondary, fontSize = 12.sp)
            }
          }
        }

        // 4. Notes Consent (with switch and explanation)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.surfaceElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tools_sheet_notes_consent_item")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = if (transcriptionConsentGranted) LingoEmerald else LingoTokens.textSecondary, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Notes Consent", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                  text = "Allow AI to transcribe and save vocabulary notes from this call for your post-call review.",
                  color = LingoTokens.textSecondary,
                  fontSize = 12.sp,
                  lineHeight = 16.sp
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
              checked = transcriptionConsentGranted,
              onCheckedChange = { transcriptionConsentGranted = it },
              colors = SwitchDefaults.colors(checkedThumbColor = LingoEmerald),
              modifier = Modifier.testTag("tools_sheet_notes_consent_switch")
            )
          }
        }

        // 5. Captions Toggle
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.surfaceElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tools_sheet_captions_item")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Captions", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Show live subtitles above the control bar", color = LingoTokens.textSecondary, fontSize = 12.sp)
              }
            }
            Switch(
              checked = liveCaptionsEnabled,
              onCheckedChange = { liveCaptionsEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = LingoTokens.brandCyan),
              modifier = Modifier.testTag("tools_sheet_captions_switch")
            )
          }
        }

        // 6. Translate Captions Toggle
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.surfaceElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tools_sheet_translate_item")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Translate, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Translate", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Show instant translation under live captions", color = LingoTokens.textSecondary, fontSize = 12.sp)
              }
            }
            Switch(
              checked = translateCaptionsEnabled,
              onCheckedChange = { translateCaptionsEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = LingoTokens.brandCyan),
              modifier = Modifier.testTag("tools_sheet_translate_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }

  // --- Audio Output Picker Bottom Sheet (Speaker / Bluetooth / Earpiece) ---
  if (showAudioRouteSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAudioRouteSheet = false },
      containerColor = Color(0xFF121826)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Audio Output",
          color = LingoTokens.textPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold
        )
        listOf(
          CallAudioRoute.SPEAKER to Icons.AutoMirrored.Filled.VolumeUp,
          CallAudioRoute.EARPIECE to Icons.Default.PhoneInTalk,
          CallAudioRoute.BLUETOOTH to Icons.Default.BluetoothAudio
        ).forEach { (route, icon) ->
          val isAvailable = route != CallAudioRoute.BLUETOOTH || isBluetoothAvailable
          if (isAvailable) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (activeAudioRoute == route) LingoTokens.brandCyan.copy(alpha = 0.16f) else LingoTokens.surfaceElevated,
              border = BorderStroke(
                1.dp,
                if (activeAudioRoute == route) LingoTokens.brandCyan else LingoTokens.borderSubtle
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  rtcManager.selectAudioRoute(route)
                  showAudioRouteSheet = false
                }
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(icon, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(22.dp))
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(route.label, color = LingoTokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (activeAudioRoute == route) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }

  // --- Valid Call-End Summary Dialog (Shown ONLY when call duration >= 30 seconds) ---
  if (showCallEndSummaryDialog) {
    val earnedXp = remember(elapsedSeconds) {
      ((elapsedSeconds / 30L).toInt().coerceAtLeast(1) * 15).coerceAtMost(120)
    }
    AlertDialog(
      onDismissRequest = {
        showCallEndSummaryDialog = false
        onEndCall()
      },
      containerColor = Color(0xFF101828),
      titleContentColor = LingoTokens.textPrimary,
      textContentColor = LingoTokens.textSecondary,
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LingoEmerald, modifier = Modifier.size(24.dp))
          Text("Call Summary", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
      },
      text = {
        Column(
          modifier = Modifier.testTag("call_end_summary_dialog"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Practice session with $partnerDisplayName ($languagePairLabel)",
            color = LingoTokens.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
          HorizontalDivider(color = LingoTokens.borderSubtle)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Talk time", fontSize = 13.sp)
            Text(formattedDuration, color = LingoTokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Saved expressions", fontSize = 13.sp)
            Text("${savedCallExpressions.size}", color = LingoTokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("XP earned", fontSize = 13.sp)
            Text("+$earnedXp XP", color = LingoTokens.accentGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
          }

          HorizontalDivider(color = LingoTokens.borderSubtle)

          // 5-star conversation rating
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Rate this conversation",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              for (star in 1..5) {
                IconButton(
                  onClick = { postCallRating = star },
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("call_summary_star_$star")
                ) {
                  Icon(
                    imageVector = if (star <= postCallRating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Rate $star stars",
                    tint = if (star <= postCallRating) LingoTokens.accentGold else LingoTokens.textSecondary,
                    modifier = Modifier.size(26.dp)
                  )
                }
              }
            }
          }

          // Quick post-call actions: Add friend, Report, Call again
          if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  if (!postCallFriendSent) {
                    viewModel.sendFriendRequest(partnerId, partnerDisplayName) {
                      postCallFriendSent = true
                    }
                    postCallFriendSent = true
                  }
                },
                enabled = !postCallFriendSent,
                border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 44.dp)
                  .testTag("call_summary_add_friend_btn")
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = LingoTokens.brandCyan)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (postCallFriendSent) "Added" else "Add friend",
                  color = LingoTokens.textPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1
                )
              }

              OutlinedButton(
                onClick = { showPostCallReportDialog = true },
                enabled = !postCallReportSubmitted,
                border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 44.dp)
                  .testTag("call_summary_report_btn")
              ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(15.dp), tint = LingoCoral)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (postCallReportSubmitted) "Reported" else "Report",
                  color = LingoCoral,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1
                )
              }
            }
          }
        }
      },
      dismissButton = {
        if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
          TextButton(
            onClick = {
              showCallEndSummaryDialog = false
              coroutineScope.launch {
                runCatching {
                  val newRoom = viewModel.startOrJoinCallRoom(
                    calleeId = partnerId,
                    calleeName = partnerDisplayName,
                    callType = room.callType,
                    matchMode = "direct",
                    targetLanguageOverride = room.targetLanguageCode,
                    conversationTopic = room.conversationTopic
                  )
                  onCallAgain(newRoom)
                }.onFailure {
                  onEndCall()
                }
              }
            },
            modifier = Modifier.testTag("call_summary_call_again_btn")
          ) {
            Text("Call again", color = LingoTokens.brandCyan, fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showCallEndSummaryDialog = false
            onEndCall()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoTokens.brandCyan,
            contentColor = LingoTokens.onGradientInk
          ),
          modifier = Modifier.testTag("call_summary_done_button")
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  if (showPostCallReportDialog) {
    val reportReasons = listOf("Disrespectful behavior", "Spam or off-topic", "Inappropriate audio or video")
    AlertDialog(
      onDismissRequest = { showPostCallReportDialog = false },
      containerColor = Color(0xFF101828),
      titleContentColor = LingoTokens.textPrimary,
      textContentColor = LingoTokens.textSecondary,
      title = { Text("Report $partnerDisplayName", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Select a reason. Reports are reviewed by our safety team.", fontSize = 13.sp)
          reportReasons.forEach { reason ->
            Surface(
              onClick = {
                viewModel.reportUser(partnerId, reason, "Reported from post-call summary (${room.roomId})")
                postCallReportSubmitted = true
                showPostCallReportDialog = false
                android.widget.Toast.makeText(context, "Report submitted. Thank you.", android.widget.Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(12.dp),
              color = LingoTokens.bgSubtle,
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = reason,
                color = LingoTokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showPostCallReportDialog = false }) {
          Text("Cancel", color = LingoTokens.textSecondary)
        }
      }
    )
  }

  if (showInCallBlockConfirm) {
    AlertDialog(
      onDismissRequest = { showInCallBlockConfirm = false },
      containerColor = Color(0xFF101828),
      titleContentColor = LingoTokens.textPrimary,
      textContentColor = LingoTokens.textSecondary,
      modifier = Modifier.testTag("live_call_block_confirm_dialog"),
      title = { Text("Block $partnerDisplayName?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
      text = {
        Text(
          text = "This will immediately end the call and prevent $partnerDisplayName from calling or messaging you.",
          fontSize = 13.5.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showInCallBlockConfirm = false
            if (partnerId.isNotBlank()) {
              viewModel.blockUser(partnerId, partnerDisplayName)
            }
            android.widget.Toast.makeText(context, "$partnerDisplayName has been blocked.", android.widget.Toast.LENGTH_SHORT).show()
            finalizeAndExitCall()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoCoral,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("live_call_block_confirm_btn")
        ) {
          Text("Block & End Call", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showInCallBlockConfirm = false }) {
          Text("Cancel", color = LingoTokens.textSecondary)
        }
      }
    )
  }
}

@Composable
private fun CallControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(if (active) Color(0xFF1E3A8A) else Color(0xFF162035))
        .border(
          1.dp,
          if (active) LingoTokens.brandCyan.copy(alpha = 0.55f) else LingoTokens.borderSubtle,
          CircleShape
        )
        .clickable { onClick() }
        .testTag(testTag)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) Color.White else LingoTokens.textSecondary,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      color = LingoTokens.textSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1
    )
  }
}
