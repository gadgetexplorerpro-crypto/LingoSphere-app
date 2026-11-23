package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.avatar.AiVideoTutorManager
import com.example.avatar.AvatarProvider
import com.example.avatar.AvatarStreamingService
import com.example.avatar.CefrLevel
import com.example.avatar.TutorPersona
import com.example.avatar.TutorViseme
import com.example.avatar.VideoCallStatus
import com.example.avatar.ui.RealisticTalkingAvatar
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.AnimatedVoiceWaveform
import com.example.ui.components.EmojiVoice
import com.example.ui.components.EmojiVoiceState
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVideoTutorScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  pastMistakesMemory: List<String> = emptyList(),
  initialTutor: TutorPersona? = null,
  onSaveVocabToFlashcard: (term: String, translation: String, example: String) -> Unit,
  onRecordGrammarMistake: (com.example.data.model.MistakeNotebookEntry) -> Unit = {},
  onEndCallAndExit: () -> Unit,
  onSaveSession: (SpeakingSession) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)

  val tutorManager = remember(userProfile.userId, targetLang.code) {
    val tutor = initialTutor ?: TutorPersona.findTutorForLanguage(targetLang.code, preferFemale = true)
    AiVideoTutorManager(
      context = context,
      speechManager = speechManager,
      coroutineScope = coroutineScope,
      targetLanguage = targetLang,
      nativeLanguage = nativeLang,
      initialCefrLevel = CefrLevel.B1,
      initialTutor = tutor,
      isPremium = userProfile.isPremium,
      pastMistakesMemory = pastMistakesMemory.joinToString("\n"),
      onGrammarMistakeDetected = onRecordGrammarMistake,
      onCallReportGenerated = {}
    )
  }

  // Reactive state from AiVideoTutorManager
  val callStatus by tutorManager.callStatus.collectAsState()
  val callSeconds by tutorManager.callSeconds.collectAsState()
  val isMuted by tutorManager.isMuted.collectAsState()
  val isCameraOn by tutorManager.isCameraOn.collectAsState()
  val isSpeakerOn by tutorManager.isSpeakerOn.collectAsState()
  val currentTutor by tutorManager.currentTutor.collectAsState()
  val currentCefr by tutorManager.currentCefrLevel.collectAsState()
  val speakingSpeed by tutorManager.speakingSpeedMultiplier.collectAsState()
  val selectedTopic by tutorManager.selectedTopic.collectAsState()
  val liveTranscript by tutorManager.liveTranscript.collectAsState()
  val currentPartialSpeech by tutorManager.currentPartialSpeech.collectAsState()
  val latestTutorResponse by tutorManager.latestTutorResponse.collectAsState()
  val latestGrammarCorrection by tutorManager.latestGrammarCorrection.collectAsState()
  val livePronunciationScore by tutorManager.livePronunciationScore.collectAsState()
  val savedVocabTips by tutorManager.savedVocabularyTips.collectAsState()
  val heygenSession by tutorManager.heygenSession.collectAsState()
  val tavusConversation by tutorManager.tavusConversation.collectAsState()
  val isLiveAvatarStreaming by tutorManager.isLiveAvatarStreaming.collectAsState()
  val credentialsNotice by tutorManager.credentialsNotice.collectAsState()

  // Real-time lip-sync and audio reactive states
  val currentViseme by tutorManager.currentViseme.collectAsState()
  val speechAperture by tutorManager.speechAperture.collectAsState()
  val isTutorSpeakingAudio by tutorManager.isTutorSpeakingAudio.collectAsState()
  val userRmsLevel by speechManager.rmsLevel.collectAsState()

  // UI state
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showPostCallSummary by remember { mutableStateOf(false) }
  var showTranslationSubtitles by remember { mutableStateOf(true) }
  var showPhoneticHints by remember { mutableStateOf(true) }

  var showEndCallConfirm by remember { mutableStateOf(false) }
  var lastEndedSession by remember { mutableStateOf<SpeakingSession?>(null) }
  var manualSpeechInput by remember { mutableStateOf("") }
  var showManualInputModal by remember { mutableStateOf(false) }

  // Mic & Camera permission checks
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
  var useFrontCamera by remember { mutableStateOf(true) }
  var canSwitchCamera by remember { mutableStateOf(false) }
  var cameraUnavailable by remember { mutableStateOf(false) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val camOk = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (camOk != hasCameraPermission) {
          hasCameraPermission = camOk
          if (camOk) cameraUnavailable = false
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
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
    if (camGranted) {
      cameraUnavailable = false
    }
    if (micGranted) {
      tutorManager.startCall()
    } else {
      Toast.makeText(context, "Microphone access is needed for live speaking practice", Toast.LENGTH_LONG).show()
    }
  }

  LaunchedEffect(Unit) {
    if (hasMicPermission && hasCameraPermission) {
      tutorManager.startCall()
    } else if (hasMicPermission) {
      tutorManager.startCall()
      permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    } else {
      permissionLauncher.launch(
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
      )
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      tutorManager.release()
    }
  }

  BackHandler {
    if (callStatus != VideoCallStatus.ENDED && !showPostCallSummary) {
      showEndCallConfirm = true
    } else {
      onEndCallAndExit()
    }
  }

  val formattedTime = remember(callSeconds) {
    val mins = callSeconds / 60
    val secs = callSeconds % 60
    "%02d:%02d".format(mins, secs)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0F172A))
  ) {
    // 1. LIVE TUTOR VIDEO CANVAS / STREAM CONTAINER
    Box(
      modifier = Modifier
        .fillMaxSize()
        .testTag("tutor_video_canvas"),
      contentAlignment = Alignment.Center
    ) {
      val streamUrl = heygenSession?.url ?: tavusConversation?.conversationUrl ?: ""
      var webViewRendererCrashed by remember(streamUrl) { mutableStateOf(false) }
      val isRobolectricTutor = remember {
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
      }
      var isAvatarWebViewSurfaceReady by remember(streamUrl, isLiveAvatarStreaming) {
        mutableStateOf(isRobolectricTutor)
      }
      LaunchedEffect(streamUrl, isLiveAvatarStreaming) {
        if (isLiveAvatarStreaming && streamUrl.isNotBlank() && !isAvatarWebViewSurfaceReady) {
          androidx.compose.runtime.withFrameNanos { }
          kotlinx.coroutines.delay(90L)
          isAvatarWebViewSurfaceReady = true
        }
      }
      if (!webViewRendererCrashed && isLiveAvatarStreaming && streamUrl.isNotBlank() && isAvatarWebViewSurfaceReady) {
        // Real-time WebRTC / Streaming Video player WebView
        AndroidView(
          factory = { ctx ->
            WebView(ctx).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              setLayerType(android.view.View.LAYER_TYPE_NONE, null)
              settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadsImagesAutomatically = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                setSupportZoom(false)
              }
              webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                  request?.grant(request.resources)
                }
              }
              webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(
                  view: WebView?,
                  detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                  android.util.Log.w(
                    "AiVideoTutorScreen",
                    "WebView renderer process terminated (didCrash=${detail?.didCrash()}); falling back to avatar."
                  )
                  webViewRendererCrashed = true
                  runCatching {
                    (view?.parent as? ViewGroup)?.removeView(view)
                    view?.destroy()
                  }
                  return true
                }
              }
              if (streamUrl.isNotBlank()) {
                loadUrl(streamUrl)
              }
            }
          },
          update = { webView ->
            if (streamUrl.isNotBlank() && webView.url != streamUrl) {
              webView.loadUrl(streamUrl)
            }
          },
          onRelease = { webView ->
            runCatching {
              webView.stopLoading()
              webView.loadUrl("about:blank")
              webView.onPause()
              webView.removeAllViews()
              webView.destroy()
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        // High-fidelity realistic talking avatar with 3D kinematics, spontaneous human blinking, and audio-synchronized lip-sync
        RealisticTalkingAvatar(
          tutor = currentTutor,
          callStatus = callStatus,
          currentViseme = currentViseme,
          speechAperture = speechAperture,
          isAudioPlaying = isTutorSpeakingAudio,
          userRmsLevel = userRmsLevel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    // 2. TOP HEADER HUD (Tutor Info, CEFR Badge, Call Duration, Settings)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Tutor Persona Info Pill
      Surface(
        color = Color.Black.copy(alpha = 0.68f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.weight(1f, fill = false)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val hudEmojiState = when (callStatus) {
            VideoCallStatus.TUTOR_SPEAKING -> EmojiVoiceState.REPLYING
            VideoCallStatus.LISTENING -> if (currentPartialSpeech.isNotBlank() || userRmsLevel >= 0.16f) EmojiVoiceState.SPEAKING else EmojiVoiceState.LISTENING
            VideoCallStatus.THINKING -> EmojiVoiceState.THINKING
            else -> EmojiVoiceState.IDLE
          }
          EmojiVoice(
            state = hudEmojiState,
            size = 26.dp,
            animated = true,
            audioLevel = userRmsLevel,
            isRecording = callStatus == VideoCallStatus.LISTENING,
            isPlaying = callStatus == VideoCallStatus.TUTOR_SPEAKING
          )
          Column {
            Text(
              text = currentTutor.name,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${targetLang.name} • ${currentCefr.title} (${currentCefr.code})",
              color = Color.White.copy(alpha = 0.75f),
              fontSize = 10.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Live Time & Settings Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          color = Color.Black.copy(alpha = 0.68f),
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = LingoCoral,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = formattedTime,
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        IconButton(
          onClick = { showSettingsSheet = true },
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.68f))
            .testTag("video_tutor_settings_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Tutor & Video Settings",
            tint = Color.White,
            modifier = Modifier.size(19.dp)
          )
        }
      }
    }

    // 3. COMPACT USER FRONT CAMERA PICTURE-IN-PICTURE (PiP) IN TOP-RIGHT CORNER BELOW HEADER
    if (isCameraOn) {
      val isRobolectric = remember {
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
      }
      var isCameraSurfaceSettled by remember(hasCameraPermission, isCameraOn) {
        mutableStateOf(isRobolectric)
      }
      LaunchedEffect(hasCameraPermission, isCameraOn) {
        if (hasCameraPermission && isCameraOn && !isCameraSurfaceSettled) {
          androidx.compose.runtime.withFrameNanos { }
          kotlinx.coroutines.delay(140L)
          isCameraSurfaceSettled = true
        }
      }
      val showLiveCameraPreview = hasCameraPermission && isCameraSurfaceSettled && !cameraUnavailable && !isRobolectric &&
        !showPostCallSummary && callStatus != VideoCallStatus.ENDED

      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .statusBarsPadding()
          .padding(top = 64.dp, end = 12.dp)
          .size(width = 88.dp, height = 118.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF0B1324).copy(alpha = 0.9f))
          .border(1.5.dp, LingoCyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
          .clickable {
            if (!hasCameraPermission) {
              permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            } else if (canSwitchCamera) {
              useFrontCamera = !useFrontCamera
            }
          }
          .testTag("user_pip_camera"),
        contentAlignment = Alignment.Center
      ) {
        if (showLiveCameraPreview) {
          LiveTutorUserCameraPreview(
            useFrontCamera = useFrontCamera,
            onCameraCapabilities = { supportsSwitch ->
              canSwitchCamera = supportsSwitch
            },
            onCameraError = {
              cameraUnavailable = true
            },
            modifier = Modifier.fillMaxSize()
          )

          if (canSwitchCamera) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
              )
            }
          }

          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                )
              )
              .padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.align(Alignment.Center)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(
                    when (callStatus) {
                      VideoCallStatus.LISTENING -> LingoEmerald
                      VideoCallStatus.THINKING -> LingoCyan
                      else -> Color.White.copy(alpha = 0.7f)
                    }
                  )
              )
              Text(
                text = when (callStatus) {
                  VideoCallStatus.LISTENING -> "You • Live"
                  VideoCallStatus.THINKING -> "Thinking"
                  VideoCallStatus.TUTOR_SPEAKING -> "Tutor"
                  else -> "You"
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
          ) {
            Icon(
              imageVector = if (!hasCameraPermission) Icons.Default.VideocamOff else Icons.Default.AccountCircle,
              contentDescription = "User Camera Preview",
              tint = Color.White.copy(alpha = 0.85f),
              modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "You",
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = when {
                !hasCameraPermission -> "Tap for Cam"
                callStatus == VideoCallStatus.LISTENING -> "Listening"
                callStatus == VideoCallStatus.THINKING -> "Thinking"
                callStatus == VideoCallStatus.TUTOR_SPEAKING -> "Tutor"
                else -> "Ready"
              },
              color = when {
                !hasCameraPermission -> LingoCyan
                callStatus == VideoCallStatus.LISTENING -> LingoEmerald
                callStatus == VideoCallStatus.THINKING -> LingoCyan
                else -> Color.White.copy(alpha = 0.65f)
              },
              fontSize = 9.5.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }

    // 4. FLOATING GRAMMAR CORRECTION BANNER (Animated when mistake detected)
    AnimatedVisibility(
      visible = latestGrammarCorrection != null,
      enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopStart)
        .statusBarsPadding()
        .padding(top = 64.dp, start = 12.dp, end = if (isCameraOn) 108.dp else 12.dp)
    ) {
      val correction = latestGrammarCorrection
      if (correction != null) {
        Surface(
          color = Color(0xFF0F172A).copy(alpha = 0.95f),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, LingoCoral),
          shadowElevation = 8.dp
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AutoFixHigh,
              contentDescription = "Grammar Coach",
              tint = LingoCoral,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tip: \"${correction.first}\"",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Text(
                text = correction.second,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
            IconButton(
              onClick = { tutorManager.dismissGrammarCorrection() },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(15.dp)
              )
            }
          }
        }
      }
    }

    // 5. BOTTOM PANEL: SYNCHRONIZED VOICE STATE + QUICK ACTIONS + SUBTITLES + CALL DOCK
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(bottom = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      var showVideoCallRescue by remember { mutableStateOf(false) }

      com.example.ui.components.ConversationRescuePanel(
        visible = showVideoCallRescue,
        languageCode = targetLang.code,
        lastPartnerPrompt = latestTutorResponse?.replyInTargetLanguage ?: "",
        learnerStruggledDraft = currentPartialSpeech,
        sessionTitle = "Live AI Video Tutor ($selectedTopic)",
        onSpeakPreview = { phrase -> speechManager.speak(phrase, targetLang.code) },
        onRetryWithPhrase = { phrase ->
          showVideoCallRescue = false
          tutorManager.submitUserSpeech(phrase)
        },
        onDismiss = { showVideoCallRescue = false },
        modifier = Modifier.padding(horizontal = 12.dp)
      )

      // SINGLE UNIFIED LIVE STATE INDICATOR (Listening / Thinking / Speaking)
      when (callStatus) {
        VideoCallStatus.TUTOR_SPEAKING -> {
          Surface(
            onClick = { tutorManager.interruptTutor() },
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LingoCyan.copy(alpha = 0.6f)),
            modifier = Modifier
              .padding(horizontal = 14.dp)
              .align(Alignment.CenterHorizontally)
              .testTag("interrupt_tutor_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Speaking",
                tint = LingoCyan,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "${currentTutor.name} is speaking • Tap to interrupt",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
        VideoCallStatus.THINKING -> {
          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LingoElectricBlue.copy(alpha = 0.6f)),
            modifier = Modifier
              .padding(horizontal = 14.dp)
              .align(Alignment.CenterHorizontally)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = LingoCyan,
                strokeWidth = 2.dp
              )
              Text(
                text = "${currentTutor.name} is thinking...",
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
        VideoCallStatus.LISTENING -> {
          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.55f)),
            modifier = Modifier
              .padding(horizontal = 14.dp)
              .align(Alignment.CenterHorizontally)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              AnimatedVoiceWaveform(
                isListening = true,
                rmsLevel = userRmsLevel,
                modifier = Modifier
                  .width(48.dp)
                  .height(16.dp)
              )
              Text(
                text = if (currentPartialSpeech.isNotBlank()) "\"$currentPartialSpeech\"" else "Listening to you speak...",
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
        else -> Unit
      }

      // QUICK INTERACTION CHIPS (Conversation Rescue, Slow Down, Repeat, Explain, Save Word)
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        item {
          com.example.ui.components.ConversationRescueButton(
            isOpen = showVideoCallRescue,
            onToggle = { showVideoCallRescue = !showVideoCallRescue },
            testTag = "video_tutor_rescue_button"
          )
        }
        item {
          SuggestionChip(
            onClick = { tutorManager.requestSlowDown() },
            label = {
              Text(
                text = "Slow Down",
                fontSize = 11.5.sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            },
            icon = { Icon(Icons.Default.Speed, null, tint = LingoCyan, modifier = Modifier.size(15.dp)) },
            colors = SuggestionChipDefaults.suggestionChipColors(
              containerColor = Color.Black.copy(alpha = 0.65f)
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
              borderColor = Color.White.copy(alpha = 0.22f),
              enabled = true
            )
          )
        }
        item {
          SuggestionChip(
            onClick = { tutorManager.requestRepeat() },
            label = {
              Text(
                text = "Repeat",
                fontSize = 11.5.sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            },
            icon = { Icon(Icons.Default.Replay, null, tint = LingoEmerald, modifier = Modifier.size(15.dp)) },
            colors = SuggestionChipDefaults.suggestionChipColors(
              containerColor = Color.Black.copy(alpha = 0.65f)
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
              borderColor = Color.White.copy(alpha = 0.22f),
              enabled = true
            )
          )
        }
        item {
          SuggestionChip(
            onClick = { tutorManager.requestGrammarExplanation() },
            label = {
              Text(
                text = "Explain",
                fontSize = 11.5.sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            },
            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = LingoCoral, modifier = Modifier.size(15.dp)) },
            colors = SuggestionChipDefaults.suggestionChipColors(
              containerColor = Color.Black.copy(alpha = 0.65f)
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
              borderColor = Color.White.copy(alpha = 0.22f),
              enabled = true
            )
          )
        }
        if (latestTutorResponse?.vocabularyTip != null) {
          item {
            SuggestionChip(
              onClick = {
                val tip = latestTutorResponse?.vocabularyTip ?: ""
                val parts = tip.split(":")
                val term = parts.getOrNull(0)?.trim() ?: tip
                val meaning = parts.getOrNull(1)?.trim() ?: "Vocabulary item"
                onSaveVocabToFlashcard(term, meaning, latestTutorResponse?.replyInTargetLanguage ?: "")
                Toast.makeText(context, "Saved \"$term\" to Flashcards!", Toast.LENGTH_SHORT).show()
              },
              label = {
                Text(
                  text = "Save Word",
                  fontSize = 11.5.sp,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false
                )
              },
              icon = { Icon(Icons.Default.BookmarkAdd, null, tint = Color(0xFFFFD700), modifier = Modifier.size(15.dp)) },
              colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color.Black.copy(alpha = 0.65f)
              ),
              border = SuggestionChipDefaults.suggestionChipBorder(
                borderColor = Color(0xFFFFD700).copy(alpha = 0.5f),
                enabled = true
              )
            )
          }
        }
      }

      // LIVE BILINGUAL SUBTITLE CARD (Target Language + Native Translation)
      val currentResponse = latestTutorResponse
      if (currentResponse != null) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 125.dp)
            .padding(horizontal = 16.dp),
          color = Color.Black.copy(alpha = 0.78f),
          shape = RoundedCornerShape(18.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
          Column(
            modifier = Modifier
              .padding(14.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            // Target Language Spoken Line
            Text(
              text = currentResponse.replyInTargetLanguage,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              lineHeight = 21.sp
            )

            // Phonetic Pronunciation Guide
            if (showPhoneticHints && currentResponse.phoneticGuide.isNotBlank()) {
              Text(
                text = currentResponse.phoneticGuide,
                color = LingoCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            // Native Language Translation Subtitle
            if (showTranslationSubtitles && currentResponse.translationInNativeLanguage.isNotBlank()) {
              Text(
                text = currentResponse.translationInNativeLanguage,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
              )
            }
          }
        }
      }

      // VIDEO CALL CONTROL DOCK (Mute, Camera, Speaker, Type, End Call)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.92f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mute / Unmute Mic
          IconButton(
            onClick = { tutorManager.toggleMute() },
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(if (isMuted) LingoCoral else Color.White.copy(alpha = 0.15f))
              .testTag("video_tutor_mute_btn")
          ) {
            Icon(
              imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          // Camera PiP On / Off
          IconButton(
            onClick = {
              val turningOn = !isCameraOn
              tutorManager.toggleCamera()
              if (turningOn && !hasCameraPermission) {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
              }
            },
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(if (!isCameraOn) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f))
              .testTag("video_tutor_camera_btn")
          ) {
            Icon(
              imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
              contentDescription = "Toggle Front Camera",
              tint = if (isCameraOn) LingoCyan else Color.White.copy(alpha = 0.5f),
              modifier = Modifier.size(24.dp)
            )
          }

          // Speakerphone Toggle
          IconButton(
            onClick = { tutorManager.toggleSpeaker() },
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.15f))
              .testTag("video_tutor_speaker_btn")
          ) {
            Icon(
              imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
              contentDescription = "Toggle Speakerphone",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          // Manual Text Input Fallback (for noisy environments)
          IconButton(
            onClick = { showManualInputModal = true },
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.15f))
              .testTag("video_tutor_keyboard_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Keyboard,
              contentDescription = "Type to Tutor",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          // End Call Button (Red 52dp button)
          IconButton(
            onClick = { showEndCallConfirm = true },
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color(0xFFE11D48))
              .testTag("video_tutor_end_call_btn")
          ) {
            Icon(
              imageVector = Icons.Default.CallEnd,
              contentDescription = "End Call",
              tint = Color.White,
              modifier = Modifier.size(26.dp)
            )
          }
        }
      }
    }
  }

  // 8. TUTOR & VIDEO SETTINGS MODAL SHEET
  if (showSettingsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      containerColor = Color(0xFF0F172A),
      dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .navigationBarsPadding()
          .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "AI Video Tutor Settings",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )

        // Select Tutor Persona
        Text("Select Your Tutor Persona", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(TutorPersona.ALL_TUTORS) { tutor ->
            val isSelected = tutor.id == currentTutor.id
            Surface(
              onClick = { tutorManager.switchTutor(tutor) },
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) LingoElectricBlue.copy(alpha = 0.25f) else Color(0xFF1E293B),
              border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSelected) LingoCyan else Color.White.copy(alpha = 0.1f)
              )
            ) {
              Column(
                modifier = Modifier
                  .width(130.dp)
                  .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                com.example.avatar.ui.TutorPersonaAvatarBadge(
                  tutor = tutor,
                  modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
                  isSpeaking = isSelected
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = tutor.name,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${tutor.gender} • ${tutor.primaryLanguageCode.uppercase()}",
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 10.sp
                )
              }
            }
          }
        }

        // CEFR Proficiency Level Selector (A1 - C2)
        Text("CEFR Language Level", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          CefrLevel.values().forEach { level ->
            val isSel = currentCefr == level
            FilterChip(
              selected = isSel,
              onClick = { tutorManager.switchCefrLevel(level) },
              label = { Text(level.code, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LingoCyan,
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF1E293B),
                labelColor = Color.White
              )
            )
          }
        }

        // Speaking Speed Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Tutor Speaking Speed", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
          Text("%.2fx".format(speakingSpeed), color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
          value = speakingSpeed,
          onValueChange = { tutorManager.setSpeakingSpeed(it) },
          valueRange = 0.7f..1.3f,
          steps = 5,
          colors = SliderDefaults.colors(
            thumbColor = LingoCyan,
            activeTrackColor = LingoCyan,
            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
          )
        )

        // Subtitle & Phonetic Toggles
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Show Bilingual Subtitles", color = Color.White, fontSize = 14.sp)
          Switch(
            checked = showTranslationSubtitles,
            onCheckedChange = { showTranslationSubtitles = it },
            colors = SwitchDefaults.colors(checkedThumbColor = LingoCyan)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Show Phonetic Guide", color = Color.White, fontSize = 14.sp)
          Switch(
            checked = showPhoneticHints,
            onCheckedChange = { showPhoneticHints = it },
            colors = SwitchDefaults.colors(checkedThumbColor = LingoCyan)
          )
        }



        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }



  // 10. MANUAL TEXT INPUT MODAL (when in noisy environment)
  if (showManualInputModal) {
    AlertDialog(
      onDismissRequest = { showManualInputModal = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("Type to Your AI Tutor", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = manualSpeechInput,
          onValueChange = { manualSpeechInput = it },
          placeholder = { Text("Say something in ${targetLang.name}...", color = Color.White.copy(alpha = 0.4f)) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = LingoCyan
          )
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (manualSpeechInput.isNotBlank()) {
              tutorManager.submitUserSpeech(manualSpeechInput)
              manualSpeechInput = ""
              showManualInputModal = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LingoCyan)
        ) {
          Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showManualInputModal = false }) {
          Text("Cancel", color = Color.White.copy(alpha = 0.7f))
        }
      }
    )
  }

  // 11. CONFIRM END CALL DIALOG
  if (showEndCallConfirm) {
    AlertDialog(
      onDismissRequest = { showEndCallConfirm = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("End Video Tutor Call?", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Your conversation progress, vocabulary words, and fluency evaluation will be saved to your learning notebook.",
          color = Color.White.copy(alpha = 0.8f)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showEndCallConfirm = false
            val session = tutorManager.endCall()
            lastEndedSession = session
            onSaveSession(session)
            showPostCallSummary = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
        ) {
          Text("End & View Report", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEndCallConfirm = false }) {
          Text("Resume Call", color = Color.White)
        }
      }
    )
  }

  // 12. POST-CALL COMPREHENSIVE LEARNING REPORT MODAL
  if (showPostCallSummary) {
    val report by tutorManager.learningReport.collectAsState()
    Dialog(onDismissRequest = { onEndCallAndExit() }) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LingoCyan.copy(alpha = 0.4f))
      ) {
        LazyColumn(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(LingoEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.School, null, tint = LingoEmerald, modifier = Modifier.size(28.dp))
              }
              Column {
                Text(
                  text = "Lesson Completed!",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp
                )
                Text(
                  text = "Face-to-Face with ${currentTutor.name}",
                  color = Color.White.copy(alpha = 0.7f),
                  fontSize = 12.sp
                )
              }
            }
          }

          // Key metrics card
          item {
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(16.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Duration", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                  Text(formattedTime, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Fluency Score", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                  Text("$livePronunciationScore%", color = LingoEmerald, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Turns", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                  Text("${liveTranscript.size}", color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
              }
            }
          }

          // Vocabulary Learned
          if (savedVocabTips.isNotEmpty()) {
            item {
              Text("Key Vocabulary Learned", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                savedVocabTips.forEach { tip ->
                  Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(tip, color = Color.White, fontSize = 13.sp)
                    }
                  }
                }
              }
            }
          }

          // Action button
          item {
            Button(
              onClick = { onEndCallAndExit() },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = LingoCyan)
            ) {
              Text("Save & Return to Lessons", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LiveTutorUserCameraPreview(
  useFrontCamera: Boolean,
  onCameraCapabilities: (supportsCameraSwitch: Boolean) -> Unit,
  onCameraError: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember(context) {
    PreviewView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }

  DisposableEffect(lifecycleOwner, useFrontCamera, previewView) {
    var activeProvider: ProcessCameraProvider? = null
    var isDisposed = false
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        if (isDisposed) return@addListener
        try {
          val provider = cameraProviderFuture.get()
          activeProvider = provider
          val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
          val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
          onCameraCapabilities(hasFront && hasBack)

          val selector = when {
            useFrontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
            !useFrontCamera && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
            hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
            hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> null
          }

          if (selector == null) {
            onCameraError()
            return@addListener
          }

          val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
          }
          provider.unbindAll()
          provider.bindToLifecycle(lifecycleOwner, selector, preview)
        } catch (t: Throwable) {
          android.util.Log.w("AiVideoTutorScreen", "Unable to start CameraX front preview", t)
          onCameraError()
        }
      },
      ContextCompat.getMainExecutor(context)
    )

    onDispose {
      isDisposed = true
      runCatching { activeProvider?.unbindAll() }
    }
  }

  AndroidView(
    factory = { previewView },
    modifier = modifier
  )
}
