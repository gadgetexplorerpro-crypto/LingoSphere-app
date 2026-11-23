package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.AiConversationPartner
import com.example.data.model.CallLearningReport
import com.example.data.model.CallMode
import com.example.data.model.CorrectionMode
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.SavedAiConversationRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.TutorVoice
import com.example.data.model.UserProfile
import com.example.data.model.VoiceCallStatus
import com.example.speech.AiVoiceCallManager
import com.example.speech.SpeechManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.ui.components.ConversationFollowControlChip
import com.example.ui.components.Expressive2dPartnerAvatar
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTutorOrb
import com.example.ui.components.LiveAudioWaveformBar
import com.example.ui.components.SynchronizedAiSpeechText
import com.example.ui.components.formatCefrLevelLabel
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoNavyDeep
import com.example.ui.theme.LingoViolet
import com.example.ui.theme.LingoVioletLight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiVoiceCallScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  initialMode: CallMode = CallMode.FREE_TALK,
  initialVoice: TutorVoice = TutorVoice.AOEDE,
  initialPartner: AiConversationPartner = AiConversationPartner.ALL_PARTNERS.first(),
  initialGroupPartners: List<AiConversationPartner> = emptyList(),
  initialTextMode: Boolean = false,
  initialCorrectionMode: CorrectionMode = CorrectionMode.IMMEDIATE,
  initialCefrLevel: String = "B1",
  resumedRecord: SavedAiConversationRecord? = null,
  pastMistakesMemory: List<String> = emptyList(),
  onSaveVocabToFlashcard: (term: String, translation: String, example: String) -> Unit = { _, _, _ -> },
  onRecordGrammarMistake: (com.example.data.model.MistakeNotebookEntry) -> Unit = {},
  onConversationRecordSaved: (SavedAiConversationRecord) -> Unit = {},
  onEndCallAndExit: () -> Unit,
  onSaveSession: (SpeakingSession) -> Unit,
  billingRepository: com.example.data.repository.BillingRepository? = null,
  onOpenSubscription: () -> Unit = onEndCallAndExit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasMicPermission = isGranted
  }

  var completedReport by remember { mutableStateOf<CallLearningReport?>(null) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showTranscriptSheet by remember { mutableStateOf(false) }
  var showTypeToSpeakDialog by remember { mutableStateOf(false) }
  var typedInputText by remember { mutableStateOf("") }
  var textModeDraft by remember { mutableStateOf("") }
  var showVoiceCallRescue by remember { mutableStateOf(false) }

  val quickPhrases = remember(targetLang.code) {
    when (targetLang.code) {
      "es" -> listOf("¡Hola! ¿Cómo estás?", "¿Qué recomiendas visitar?", "¿Cómo se dice esto en español?", "Háblame de tu cultura", "Corrige mi pronunciación")
      "fr" -> listOf("Bonjour ! Comment allez-vous ?", "Pouvez-vous répéter ?", "Parlez-moi de votre ville", "Corrigez ma prononciation", "Je veux pratiquer le français")
      "de" -> listOf("Hallo! Wie geht's?", "Kannst du das wiederholen?", "Erzähl mir von deiner Stadt", "Korrigiere bitte meine Aussprache", "Ich lerne Deutsch")
      "it" -> listOf("Ciao! Come va?", "Puoi ripetere per favore?", "Parlami della tua cultura", "Correggi la mia pronuncia", "Voglio imparare l'italiano")
      "ja" -> listOf("こんにちは！お元気ですか？", "もう一度言ってください", "発音を直してください", "おすすめの場所は？", "日常会話を練習したいです")
      "pt" -> listOf("Olá! Como vai você?", "Pode repetir por favor?", "Fale-me sobre sua cidade", "Corrija minha pronúncia", "Vamos praticar conversação")
      "zh" -> listOf("你好！最近怎么样？", "请再说一遍", "请纠正我的发音", "我想练习日常对话", "请用简单的词语")
      "ko" -> listOf("안녕하세요! 잘 지내시죠?", "다시 한번 말해줄래요?", "발음 좀 교정해 주세요", "한국 문화에 대해 알려주세요", "일상 회화 연습하고 싶어요")
      "ar" -> listOf("مرحباً! كيف حالك؟", "هل يمكنك إعادة ذلك؟", "صحح نطقي من فضلك", "أريد ممارسة المحادثة اليومية", "حدثني عن ثقافتك")
      else -> listOf("Hello! How are you?", "Can you explain that in simple terms?", "Tell me about your culture", "Please correct my pronunciation", "Let's practice a conversation")
    }
  }

  val callManager = remember(userProfile.userId, targetLang.code, initialPartner.id) {
    AiVoiceCallManager(
      context = context,
      speechManager = speechManager,
      coroutineScope = coroutineScope,
      targetLanguage = targetLang,
      nativeLanguage = nativeLang,
      proficiencyLevel = userProfile.proficiencyLevel,
      initialMode = initialMode,
      initialVoice = initialVoice,
      initialPartner = initialPartner,
      initialGroupPartners = initialGroupPartners,
      initialTextMode = initialTextMode,
      initialCorrectionMode = initialCorrectionMode,
      initialCefrLevel = initialCefrLevel,
      isPremium = userProfile.isEntitlementVerified,
      pastMistakesMemory = pastMistakesMemory.joinToString("; "),
      onGrammarMistakeDetected = onRecordGrammarMistake,
      onConversationRecordSaved = onConversationRecordSaved,
      onCallReportGenerated = { report ->
        completedReport = report
      }
    )
  }

  val status by callManager.callStatus.collectAsState()
  val elapsedSeconds by callManager.callSeconds.collectAsState()
  val remainingSeconds by callManager.remainingSeconds.collectAsState()
  val limitReachedReason by callManager.limitReachedReason.collectAsState()
  val userSpokenSeconds by callManager.userSpokenSeconds.collectAsState()
  val tutorUsage by com.example.data.repository.AccessPolicyManager.tutorUsageState.collectAsState()
  val isQuotaWarning by callManager.isQuotaWarning.collectAsState()
  val isMuted by callManager.isMuted.collectAsState()
  val isSpeakerOn by callManager.isSpeakerOn.collectAsState()
  val isTextMode by callManager.isTextMode.collectAsState()
  val currentMode by callManager.currentMode.collectAsState()
  val currentPartner by callManager.currentPartner.collectAsState()
  val groupPartners by callManager.groupPartners.collectAsState()
  val activeSpeakingPartner by callManager.activeSpeakingPartner.collectAsState()
  val currentVoice by callManager.currentVoice.collectAsState()
  val correctionMode by callManager.correctionMode.collectAsState()
  val cefrLevel by callManager.cefrLevel.collectAsState()
  val speakingSpeed by callManager.speakingSpeedMultiplier.collectAsState()
  val regionalAccent by callManager.regionalAccent.collectAsState()
  val selectedTopic by callManager.selectedTopic.collectAsState()
  val transcript by callManager.liveTranscript.collectAsState()
  val livePartialSpeech by callManager.currentPartialSpeech.collectAsState()
  val silencePrompt by callManager.silencePromptMessage.collectAsState()
  val aiErrorMessage by callManager.aiErrorMessage.collectAsState()
  val sessionCorrections by callManager.sessionCorrections.collectAsState()
  val rmsLevel by speechManager.rmsLevel.collectAsState()
  val isTtsSpeaking by speechManager.isSpeaking.collectAsState()
  val speechAperture by speechManager.speechAperture.collectAsState()
  val currentViseme by speechManager.currentViseme.collectAsState()
  val previewingVoiceId by speechManager.previewingVoiceId.collectAsState()
  val recordingConsent by speechManager.voiceRecordingConsent.collectAsState()
  var expandedTipTurnIds by remember { mutableStateOf<Set<String>>(emptySet()) }

  val textModeListState = rememberLazyListState()
  val transcriptSheetListState = rememberLazyListState()
  var isFollowingConversation by remember { mutableStateOf(true) }
  var isPausedByManualScroll by remember { mutableStateOf(false) }
  var isProgrammaticScroll by remember { mutableStateOf(false) }

  val isTextListAtBottom = remember(textModeListState, transcript.size) {
    if (transcript.isEmpty()) true
    else {
      val lastVisible = textModeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      lastVisible >= (transcript.size - 1)
    }
  }

  LaunchedEffect(textModeListState.isScrollInProgress, isTextListAtBottom) {
    if (textModeListState.isScrollInProgress && !isProgrammaticScroll) {
      isPausedByManualScroll = !isTextListAtBottom
    } else if (isTextListAtBottom && !textModeListState.isScrollInProgress) {
      isPausedByManualScroll = false
    }
  }

  LaunchedEffect(transcriptSheetListState.isScrollInProgress) {
    if (transcriptSheetListState.isScrollInProgress && !isProgrammaticScroll && isFollowingConversation) {
      isPausedByManualScroll = true
    }
  }

  LaunchedEffect(transcript.size, isTextMode, isFollowingConversation, isPausedByManualScroll) {
    if (transcript.isNotEmpty() && isFollowingConversation && !isPausedByManualScroll) {
      isProgrammaticScroll = true
      try {
        if (isTextMode) {
          textModeListState.animateScrollToItem(transcript.size - 1)
        }
        if (showTranscriptSheet) {
          transcriptSheetListState.animateScrollToItem(transcript.size - 1)
        }
      } finally {
        isProgrammaticScroll = false
      }
    }
  }

  BackHandler(enabled = status != VoiceCallStatus.ENDED) {
    callManager.endCall()
  }

  LaunchedEffect(hasMicPermission, initialTextMode) {
    if (initialTextMode || hasMicPermission) {
      callManager.startCall(resumedRecord)
    } else {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      when (event) {
        androidx.lifecycle.Lifecycle.Event.ON_STOP -> callManager.onAppBackgrounded()
        androidx.lifecycle.Lifecycle.Event.ON_START -> callManager.onAppForegrounded()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      callManager.release()
    }
  }

  val learnerFullDisplayName = userProfile.displayName.trim().ifBlank { "You" }
  val aiOneLineSubtitle = remember(currentPartner.name, currentVoice.displayName, targetLang.name, groupPartners) {
    if (groupPartners.size >= 2) {
      "${groupPartners.joinToString(", ") { it.name }} · Voice: ${currentVoice.displayName}"
    } else {
      "${currentPartner.name} · Voice: ${currentVoice.displayName} · ${targetLang.name}"
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("ai_voice_call_screen"),
    color = LingoTokens.bgCanvas
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .lingoAmbientBackdrop()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // --- 1. Top Header Bar ---
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onEndCallAndExit,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LingoTokens.bgElevated)
                .border(1.dp, LingoTokens.borderSubtle, CircleShape)
                .testTag("call_back_minimize_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Call",
                tint = LingoTokens.textPrimary,
                modifier = Modifier.size(20.dp)
              )
            }

            // Topic & Settings Badge ("Daily Routines · B1" with settings icon)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 8.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LingoTokens.bgElevated)
                .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(24.dp))
                .clickable { showSettingsSheet = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("call_settings_pill")
            ) {
              Text(
                text = "${currentMode.title} · $cefrLevel",
                color = LingoTokens.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Conversation Settings",
                tint = LingoTokens.brandCyan,
                modifier = Modifier.size(16.dp)
              )
            }

            // Call Remaining Countdown Pill ("0:42 left", turning amber at <= 10 seconds)
            val remMinutes = remainingSeconds.coerceAtLeast(0) / 60
            val remSecs = remainingSeconds.coerceAtLeast(0) % 60
            val countdownText = String.format(java.util.Locale.US, "%d:%02d left", remMinutes, remSecs)
            val isAmberAlert = remainingSeconds <= 10

            Surface(
              shape = RoundedCornerShape(20.dp),
              color = LingoTokens.bgElevated,
              border = BorderStroke(1.dp, if (isAmberAlert) LingoAmber else LingoTokens.borderSubtle),
              modifier = Modifier.testTag("call_timer_pill")
            ) {
              Text(
                text = countdownText,
                color = if (isAmberAlert) LingoAmber else LingoTokens.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // One-line subtitle showing AI's name and voice setting with ellipsis (not stacked)
          Text(
            text = aiOneLineSubtitle,
            color = LingoTokens.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp)
              .testTag("ai_partner_one_line_subtitle")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Seamless Voice Mode / Text Mode Switcher
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(LingoTokens.bgElevated)
              .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(50))
              .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .then(
                  if (!isTextMode) Modifier.background(LingoTokens.brandHorizontalGradient)
                  else Modifier
                )
                .clickable {
                  if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                  callManager.setTextMode(false)
                }
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .testTag("voice_mode_toggle_btn")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Mic,
                  contentDescription = null,
                  tint = if (!isTextMode) LingoTokens.onGradientInk else LingoTokens.textSecondary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  "Voice",
                  color = if (!isTextMode) LingoTokens.onGradientInk else LingoTokens.textSecondary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .then(
                  if (isTextMode) Modifier.background(LingoTokens.brandHorizontalGradient)
                  else Modifier
                )
                .clickable { callManager.setTextMode(true) }
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .testTag("text_mode_toggle_btn")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Keyboard,
                  contentDescription = null,
                  tint = if (isTextMode) LingoTokens.onGradientInk else LingoTokens.textSecondary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  "Text",
                  color = if (isTextMode) LingoTokens.onGradientInk else LingoTokens.textSecondary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // Slim Offline / Connection Banner shown ONLY when an error is active, with Retry button
          AnimatedVisibility(visible = !aiErrorMessage.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LingoCoral.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("call_ai_error_banner")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = aiErrorMessage.orEmpty(),
                  color = LingoTokens.textPrimary,
                  fontSize = 12.sp,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                  onClick = { callManager.retryLastTurn() },
                  shape = RoundedCornerShape(10.dp),
                  color = LingoTokens.brandCyan,
                  modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("call_ai_retry_button")
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Text(
                      text = "Retry",
                      color = LingoTokens.onGradientInk,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }

          AnimatedVisibility(visible = isQuotaWarning && !userProfile.isEntitlementVerified) {
            Box(
              modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LingoTokens.accentGold.copy(alpha = 0.16f))
                .border(1.dp, LingoTokens.accentGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = "Under 1 min remaining in your AI Tutor session. Upgrade to LingoSphere Pro for extended voice minutes.",
                color = LingoTokens.accentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
              )
            }
          }
        }

        // --- 2. Main Body: Either Voice Mode Stage OR Text Mode Chat ---
        if (isTextMode) {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(vertical = 8.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            ) {
              LazyColumn(
                state = textModeListState,
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("call_transcript_list"),
                contentPadding = PaddingValues(bottom = if (isPausedByManualScroll && !isTextListAtBottom) 48.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(transcript, key = { it.id }) { turn ->
                  val isUser = turn.speaker == MessageSender.USER
                  val turnTimeFormatted = remember(turn.timestampEpoch) {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                      .format(java.util.Date(turn.timestampEpoch))
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                  ) {
                    Card(
                      shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 18.dp
                      ),
                      colors = CardDefaults.cardColors(
                        containerColor = if (isUser) LingoTokens.brandCyan else LingoTokens.bgElevated
                      ),
                      border = BorderStroke(
                        1.dp,
                        if (isUser) LingoTokens.brandCyan else LingoTokens.innerHighlight
                      ),
                      modifier = Modifier.fillMaxWidth(0.90f)
                    ) {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(14.dp)
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = if (isUser) learnerFullDisplayName else (turn.partnerName ?: currentPartner.name),
                            color = if (isUser) LingoTokens.onGradientInk else LingoTokens.brandCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                          ) {
                            Text(
                              text = turnTimeFormatted,
                              color = if (isUser) LingoTokens.onGradientInk.copy(alpha = 0.72f) else LingoTokens.textMuted,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Medium
                            )
                            if (!isUser) {
                              IconButton(
                                onClick = { callManager.speakTurnAloud(turn) },
                                modifier = Modifier.size(36.dp)
                              ) {
                                Icon(
                                  Icons.AutoMirrored.Outlined.VolumeUp,
                                  contentDescription = "Read Aloud",
                                  tint = LingoTokens.brandCyan,
                                  modifier = Modifier.size(18.dp)
                                )
                              }
                            }
                          }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isUser) {
                          Text(
                            text = turn.text,
                            color = LingoTokens.onGradientInk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                          )
                        } else {
                          SynchronizedAiSpeechText(
                            text = turn.text,
                            speechManager = speechManager,
                            baseTextColor = LingoTokens.textPrimary,
                            highlightColor = LingoTokens.brandCyan,
                            fontSizeSp = 14
                          )
                        }
                        if (!isUser && !turn.phoneticGuide.isNullOrBlank()) {
                          Spacer(Modifier.height(4.dp))
                          Text(
                            text = "Pronunciation: ${turn.phoneticGuide}",
                            color = LingoTokens.brandCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                          )
                        }
                        val showTranslation = !isUser &&
                          !turn.translation.isNullOrBlank() &&
                          !targetLang.code.equals(nativeLang.code, ignoreCase = true) &&
                          !turn.translation.trim().equals(turn.text.trim(), ignoreCase = true)
                        if (showTranslation) {
                          Spacer(Modifier.height(2.dp))
                          Text(
                            text = turn.translation.orEmpty(),
                            color = LingoTokens.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                          )
                        }
                        if (!turn.inlineCorrection.isNullOrBlank()) {
                          val isFreeTalkCollapsed = currentMode == CallMode.FREE_TALK && turn.id !in expandedTipTurnIds
                          Spacer(Modifier.height(8.dp))
                          if (isFreeTalkCollapsed) {
                            Surface(
                              onClick = { expandedTipTurnIds = expandedTipTurnIds + turn.id },
                              shape = RoundedCornerShape(10.dp),
                              color = LingoEmerald.copy(alpha = 0.14f),
                              border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.35f))
                            ) {
                              Text(
                                text = "Quick tip · Tap to view",
                                color = LingoEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                              )
                            }
                          } else {
                            Surface(
                              shape = RoundedCornerShape(10.dp),
                              color = LingoEmerald.copy(alpha = 0.15f),
                              border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.35f))
                            ) {
                              Text(
                                text = "Tip: ${turn.inlineCorrection}",
                                color = LingoEmerald,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }

              // Small "Resume auto-scroll" pill shown ONLY when the user scrolls up, floating above controls
              androidx.compose.animation.AnimatedVisibility(
                visible = isPausedByManualScroll && !isTextListAtBottom,
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .padding(bottom = 6.dp)
              ) {
                Surface(
                  onClick = {
                    isPausedByManualScroll = false
                    isFollowingConversation = true
                    coroutineScope.launch {
                      if (transcript.isNotEmpty()) {
                        isProgrammaticScroll = true
                        try {
                          textModeListState.animateScrollToItem(transcript.size - 1)
                        } finally {
                          isProgrammaticScroll = false
                        }
                      }
                    }
                  },
                  shape = RoundedCornerShape(999.dp),
                  color = LingoTokens.bgElevated,
                  border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.6f)),
                  shadowElevation = 6.dp,
                  modifier = Modifier.testTag("resume_autoscroll_pill")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.ArrowDownward,
                      contentDescription = null,
                      tint = LingoTokens.brandCyan,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = "Resume auto-scroll",
                      color = LingoTokens.textPrimary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }

            Spacer(Modifier.height(8.dp))

            // Text Mode Input Bar: Input + Mic Button + Clear Labeled 56px Send Button
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = Color(0xFF131D36),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                IconButton(
                  onClick = {
                    if (!hasMicPermission) {
                      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (speechManager.isRecognitionAvailable()) {
                      callManager.setTextMode(false)
                      speechManager.startListening(targetLang.code)
                    } else {
                      showTypeToSpeakDialog = true
                    }
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LingoTokens.bgElevated)
                    .testTag("text_mode_mic_btn")
                ) {
                  Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = LingoTokens.brandCyan,
                    modifier = Modifier.size(20.dp)
                  )
                }

                OutlinedTextField(
                  value = textModeDraft,
                  onValueChange = { textModeDraft = it },
                  placeholder = {
                    Text(
                      "Message ${currentPartner.name}...",
                      color = Color(0xFF94A3B8),
                      fontSize = 14.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                  ),
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("voice_call_text_input")
                )

                Surface(
                  onClick = {
                    val msg = textModeDraft.trim()
                    if (msg.isNotBlank()) {
                      textModeDraft = ""
                      callManager.submitUserSpeech(msg)
                    }
                  },
                  shape = RoundedCornerShape(18.dp),
                  color = LingoCyan,
                  modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 72.dp)
                    .testTag("voice_call_text_send_btn")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.Send,
                      contentDescription = "Send",
                      tint = Color(0xFF07101E),
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Send",
                      color = Color(0xFF07101E),
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        } else {
          // --- Voice Mode Center Stage (Big Animated Orb + Waveform + Live Captions) ---
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
          ) {
            if (groupPartners.size >= 2) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                groupPartners.forEach { partner ->
                  val isActiveSpeaker = activeSpeakingPartner.id == partner.id && (status == VoiceCallStatus.TUTOR_SPEAKING || isTtsSpeaking)
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                      modifier = Modifier.clickable {
                        if (status == VoiceCallStatus.TUTOR_SPEAKING) {
                          callManager.interruptTutor()
                        }
                      }
                    ) {
                      Expressive2dPartnerAvatar(
                        partner = partner,
                        size = if (isActiveSpeaker) 88.dp else 74.dp,
                        isSpeaking = isActiveSpeaker,
                        speechAperture = if (isActiveSpeaker) speechAperture else 0f,
                        viseme = if (isActiveSpeaker) currentViseme else com.example.avatar.TutorViseme.REST,
                        showWaveformBadge = isActiveSpeaker,
                        borderColor = if (isActiveSpeaker) LingoEmerald else LingoCyan.copy(alpha = 0.5f),
                        borderWidth = if (isActiveSpeaker) 3.dp else 1.5.dp
                      )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                      text = partner.name,
                      color = if (isActiveSpeaker) LingoEmerald else Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                    )
                  }
                }
              }
            } else {
              val isOrbActive = status == VoiceCallStatus.TUTOR_SPEAKING ||
                isTtsSpeaking ||
                status == VoiceCallStatus.USER_SPEAKING
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .clickable {
                    if (status == VoiceCallStatus.TUTOR_SPEAKING) {
                      callManager.interruptTutor()
                    }
                  }
                  .testTag("ai_call_avatar")
              ) {
                LingoTutorOrb(
                  isSpeaking = isOrbActive,
                  size = 148.dp
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Learner full name & live state subtitle
            Text(
              text = when (status) {
                VoiceCallStatus.TUTOR_SPEAKING -> "${activeSpeakingPartner.name} is speaking · Tap orb to interrupt"
                VoiceCallStatus.USER_SPEAKING -> "Listening to $learnerFullDisplayName..."
                VoiceCallStatus.THINKING -> "${activeSpeakingPartner.name} is thinking..."
                VoiceCallStatus.MUTED -> "Microphone muted"
                else -> "Speaking with $learnerFullDisplayName"
              },
              color = LingoTokens.textSecondary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LiveAudioWaveformBar(
              status = status,
              rmsLevel = rmsLevel,
              modifier = Modifier.height(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!silencePrompt.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = LingoTokens.bgElevated,
                border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 8.dp)
                  .testTag("silence_prompt_banner")
              ) {
                Text(
                  text = silencePrompt.orEmpty(),
                  color = LingoTokens.brandCyan,
                  fontSize = 12.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
              }
            }

            // Live Captions Card with Tap-to-Translate on Words + Timestamps
            val latestTurn = transcript.lastOrNull()
            var selectedCaptionWord by remember(latestTurn?.id) { mutableStateOf<String?>(null) }

            LingoCleanCard(
              contentPadding = 16.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = when {
                      livePartialSpeech.isNotBlank() -> "$learnerFullDisplayName (speaking)"
                      latestTurn != null && latestTurn.speaker == MessageSender.USER -> learnerFullDisplayName
                      latestTurn != null -> latestTurn.partnerName ?: currentPartner.name
                      else -> currentPartner.name
                    },
                    color = LingoTokens.brandCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                  if (latestTurn != null) {
                    val captionTime = remember(latestTurn.timestampEpoch) {
                      java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(latestTurn.timestampEpoch))
                    }
                    Text(
                      text = "$captionTime · ${transcript.size} turns",
                      color = LingoTokens.textSecondary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.clickable { showTranscriptSheet = true }
                    )
                  }
                }

                if (livePartialSpeech.isNotBlank()) {
                  Text(
                    text = "$livePartialSpeech...",
                    color = LingoTokens.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                  )
                } else if (latestTurn != null) {
                  val words = remember(latestTurn.id, latestTurn.text) {
                    latestTurn.text.split(Regex("\\s+")).filter { it.isNotBlank() }.take(36)
                  }
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    words.forEach { word ->
                      val cleanWord = word.trim(',', '.', '!', '?', ';', ':', '"', '¿', '¡')
                      val isSelectedWord = selectedCaptionWord == cleanWord && cleanWord.isNotBlank()
                      Text(
                        text = word,
                        color = if (isSelectedWord) LingoTokens.onGradientInk else LingoTokens.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (isSelectedWord) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                          .clip(RoundedCornerShape(6.dp))
                          .background(
                            if (isSelectedWord) LingoTokens.brandCyan else Color.Transparent
                          )
                          .clickable {
                            if (cleanWord.isNotBlank()) {
                              selectedCaptionWord = if (selectedCaptionWord == cleanWord) null else cleanWord
                            }
                          }
                          .padding(horizontal = if (isSelectedWord) 4.dp else 0.dp)
                      )
                    }
                  }

                  Text(
                    text = "Tap any word to translate or save",
                    color = LingoTokens.textSecondary,
                    fontSize = 12.sp
                  )
                } else {
                  Text(
                    text = "Say hello to start practicing ${targetLang.name} with ${currentPartner.name}.",
                    color = LingoTokens.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                  )
                }

                selectedCaptionWord?.let { tappedWord ->
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LingoTokens.bgSubtle,
                    border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = tappedWord,
                          color = LingoTokens.textPrimary,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold
                        )
                        Text(
                          text = latestTurn?.translation?.takeIf { it.isNotBlank() }
                            ?: "Tap Save to add to your ${targetLang.name} flashcards",
                          color = LingoTokens.textSecondary,
                          fontSize = 12.sp,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Surface(
                        onClick = {
                          onSaveVocabToFlashcard(
                            tappedWord,
                            latestTurn?.translation?.takeIf { it.isNotBlank() } ?: tappedWord,
                            latestTurn?.text.orEmpty()
                          )
                          selectedCaptionWord = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = LingoTokens.bgElevated,
                        border = BorderStroke(1.dp, LingoTokens.borderSubtle)
                      ) {
                        Text(
                          text = "Save",
                          color = LingoTokens.brandCyan,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                      }
                    }
                  }
                }

                if (latestTurn != null && latestTurn.speaker == MessageSender.AI_TUTOR &&
                  (latestTurn.pronunciationScore != null ||
                    !latestTurn.inlineCorrection.isNullOrBlank() ||
                    !latestTurn.naturalAlternative.isNullOrBlank())
                ) {
                  com.example.ui.components.GeminiPostTurnFeedbackCard(
                    pronunciationScore = latestTurn.pronunciationScore,
                    grammarFix = latestTurn.inlineCorrection,
                    naturalPhrase = latestTurn.naturalAlternative,
                    onListenNaturalPhrase = { phrase ->
                      speechManager.speak(phrase, targetLang.code)
                    }
                  )
                }
              }
            }

            // Quick Conversation Starters + Rescue
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .horizontalScroll(rememberScrollState()),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              com.example.ui.components.ConversationRescueButton(
                isOpen = showVoiceCallRescue,
                onToggle = { showVoiceCallRescue = !showVoiceCallRescue },
                testTag = "voice_call_rescue_button"
              )
              quickPhrases.forEach { phrase ->
                Surface(
                  onClick = { callManager.submitUserSpeech(phrase) },
                  shape = RoundedCornerShape(20.dp),
                  color = LingoTokens.bgElevated,
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle)
                ) {
                  Text(
                    text = phrase,
                    color = LingoTokens.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                  )
                }
              }
            }

            com.example.ui.components.ConversationRescuePanel(
              visible = showVoiceCallRescue,
              languageCode = targetLang.code,
              lastPartnerPrompt = latestTurn?.text ?: "",
              learnerStruggledDraft = livePartialSpeech,
              sessionTitle = "Live AI Voice Call ($selectedTopic)",
              onSpeakPreview = { phrase -> speechManager.speakWithTutorVoice(phrase, currentVoice, targetLang.code) },
              onRetryWithPhrase = { phrase ->
                showVoiceCallRescue = false
                callManager.submitUserSpeech(phrase)
              },
              onDismiss = { showVoiceCallRescue = false },
              modifier = Modifier.padding(top = 6.dp)
            )
          }

          // ONE Primary Brand Gradient Push-to-Speak Action Button (56dp)
          LingoPrimaryGradientButton(
            text = when (status) {
              VoiceCallStatus.USER_SPEAKING -> "Listening to you..."
              VoiceCallStatus.TUTOR_SPEAKING -> "Tap to interrupt ${activeSpeakingPartner.name}"
              VoiceCallStatus.THINKING -> "${activeSpeakingPartner.name} is thinking..."
              else -> "Tap to speak"
            },
            icon = if (status == VoiceCallStatus.USER_SPEAKING) Icons.Default.GraphicEq else Icons.Default.Mic,
            onClick = {
              if (status == VoiceCallStatus.TUTOR_SPEAKING) {
                callManager.interruptTutor()
              } else if (status == VoiceCallStatus.MUTED) {
                callManager.toggleMute()
              } else {
                if (speechManager.isRecognitionAvailable()) {
                  speechManager.startListening(targetLang.code)
                } else {
                  showTypeToSpeakDialog = true
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("push_to_speak_button")
          )
        }

        Spacer(Modifier.height(10.dp))

        // --- 3. Bottom Call Controls: mute, speaker, captions, settings, end call (red), evenly spaced with labels ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { callManager.toggleMute() },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isMuted) Color(0xFFEF4444).copy(alpha = 0.22f) else LingoTokens.bgElevated)
                .border(1.dp, if (isMuted) Color(0xFFEF4444) else LingoTokens.borderSubtle, CircleShape)
                .testTag("call_mute_button")
            ) {
              Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mute",
                tint = if (isMuted) Color(0xFFEF4444) else LingoTokens.textPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isMuted) "Unmute" else "Mute",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp,
              maxLines = 1
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { callManager.toggleSpeaker() },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LingoTokens.bgElevated)
                .border(1.dp, LingoTokens.borderSubtle, CircleShape)
                .testTag("call_speaker_button")
            ) {
              Icon(
                imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "Speaker",
                tint = if (isSpeakerOn) LingoTokens.brandCyan else LingoTokens.textSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Speaker",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp,
              maxLines = 1
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { showTranscriptSheet = !showTranscriptSheet },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LingoTokens.bgElevated)
                .border(1.dp, if (showTranscriptSheet) LingoTokens.brandCyan else LingoTokens.borderSubtle, CircleShape)
                .testTag("call_captions_button")
            ) {
              Icon(
                imageVector = Icons.Default.ClosedCaption,
                contentDescription = "Captions",
                tint = if (showTranscriptSheet) LingoTokens.brandCyan else LingoTokens.textPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Captions",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp,
              maxLines = 1
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { showSettingsSheet = true },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LingoTokens.bgElevated)
                .border(1.dp, LingoTokens.borderSubtle, CircleShape)
                .testTag("call_help_button")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = LingoTokens.textPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Settings",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp,
              maxLines = 1
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { callManager.endCall() },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .testTag("call_end_button")
            ) {
              Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "End call",
              color = Color(0xFFEF4444),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          }
        }
      }

      // Dialog for Typing In-Call Speech
      if (showTypeToSpeakDialog) {
        Dialog(onDismissRequest = { showTypeToSpeakDialog = false }) {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "Say Something to ${currentPartner.name}",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Type in ${targetLang.name} or English. ${currentPartner.name} will respond naturally.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(14.dp))
              OutlinedTextField(
                value = typedInputText,
                onValueChange = { typedInputText = it },
                placeholder = { Text("e.g. Hello! Let's practice together.", color = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(16.dp))
              Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
              ) {
                TextButton(onClick = { showTypeToSpeakDialog = false }) {
                  Text("Cancel", color = Color(0xFF94A3B8))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    if (typedInputText.isNotBlank()) {
                      val msg = typedInputText
                      typedInputText = ""
                      showTypeToSpeakDialog = false
                      callManager.submitUserSpeech(msg)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue)
                ) {
                  Text("Speak", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // --- Free / Pro Daily & Monthly Limit Reached Modal ---
      if (limitReachedReason == "free_daily_limit" ||
        limitReachedReason == "pro_daily_limit" ||
        limitReachedReason == "pro_monthly_limit"
      ) {
        AiTutorLimitReachedModal(
          isPro = userProfile.isEntitlementVerified,
          isMonthlyExhausted = limitReachedReason == "pro_monthly_limit" || tutorUsage.isMonthlyExhausted,
          nextMonthResetDateLabel = tutorUsage.nextMonthResetDateLabel,
          billingRepository = billingRepository,
          onSeePro = {
            onOpenSubscription()
          },
          onDismiss = {
            if (completedReport == null) {
              onEndCallAndExit()
            }
          }
        )
      }

      // --- Post-Call Learning Report Dialog ---
      if (completedReport != null &&
        limitReachedReason != "free_daily_limit" &&
        limitReachedReason != "pro_daily_limit" &&
        limitReachedReason != "pro_monthly_limit"
      ) {
        PostCallReportDialog(
          report = completedReport!!,
          targetLang = targetLang,
          onSaveVocabToFlashcard = onSaveVocabToFlashcard,
          onSaveAndClose = {
            // Reward integrity: require at least 30 seconds of actual user speech to earn XP/streak/score
            if (userSpokenSeconds >= 30 || completedReport!!.turnsCount >= 3) {
              onSaveSession(
                SpeakingSession(
                  userId = userProfile.userId,
                  languageCode = targetLang.code,
                  mode = completedReport!!.mode.title,
                  scenarioTitle = "${completedReport!!.partnerName}: ${completedReport!!.mode.title}",
                  proficiencyLevel = userProfile.proficiencyLevel,
                  turnsCount = completedReport!!.turnsCount.toLong(),
                  fluencyScore = completedReport!!.fluencyScore.toLong(),
                  grammarFeedbackSummary = completedReport!!.overallFeedback
                )
              )
            }
            onEndCallAndExit()
          }
        )
      }

      // --- In-Call Voice, Partner, Correction Mode, CEFR & Consent Settings Sheet ---
      var showCallVoiceSheet by remember { mutableStateOf(false) }

      if (showSettingsSheet) {
        val speeds = listOf(0.75f to "0.75x Slow", 0.90f to "0.9x Clear", 1.0f to "1.0x Natural", 1.15f to "1.15x Fast")
        val cefrBands = listOf("A1", "A2", "B1", "B2", "C1", "C2")

        ModalBottomSheet(
          onDismissRequest = { showSettingsSheet = false },
          containerColor = Color(0xFF0F172A),
          sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 12.dp)
          ) {
            Text(
              text = "AI Voice & Coaching Settings",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Intelligent Language Correction Mode
            Text(
              text = "Correction Mode",
              color = LingoCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            CorrectionMode.entries.forEach { mode ->
              val selected = correctionMode == mode
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (selected) LingoCyan.copy(alpha = 0.16f) else Color(0xFF1E293B))
                  .border(1.dp, if (selected) LingoCyan else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                  .clickable { callManager.setCorrectionMode(mode) }
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(mode.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(mode.subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                if (selected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = LingoCyan)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. CEFR Level Adaptation (A1–C2) & Speaking Speed
            Text(
              text = "CEFR Level Adaptation & Speed",
              color = LingoCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
            ) {
              cefrBands.forEach { band ->
                val active = cefrLevel.equals(band, ignoreCase = true)
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) LingoEmerald else Color(0xFF1E293B))
                    .clickable { callManager.setCefrLevel(band) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = band,
                    color = if (active) Color.Black else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
            ) {
              speeds.forEach { (rate, label) ->
                val active = kotlin.math.abs(speakingSpeed - rate) < 0.03f
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) LingoCyan else Color(0xFF1E293B))
                    .clickable { callManager.setSpeakingSpeed(rate) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = label,
                    color = if (active) Color.Black else Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. AI Partner Voice Selector (Shared Bottom Sheet)
            Text(
              text = "AI Partner Voice",
              color = LingoCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              onClick = { showCallVoiceSheet = true },
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1E293B),
              border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("call_settings_voice_btn")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.RecordVoiceOver,
                  contentDescription = "AI Partner Voice",
                  tint = LingoCyan,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${currentVoice.personaName} (${currentVoice.id})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                  Text(
                    text = "${currentVoice.gender} • ${currentVoice.accentLabel} • ${currentVoice.styleTag}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.5.sp
                  )
                }
                Text(
                  text = "Change & Preview",
                  color = LingoCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. 15 Real Conversation Topics
            Text(
              text = "15 Real-Life Conversation Topics",
              color = LingoCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            CallMode.CORE_15_TOPICS.forEach { mode ->
              val isSelected = mode == currentMode
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isSelected) LingoViolet.copy(alpha = 0.25f) else Color(0xFF1E293B))
                  .border(1.dp, if (isSelected) LingoViolet else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                  .clickable {
                    callManager.switchMode(mode)
                    showSettingsSheet = false
                  }
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = mode.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = mode.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                  )
                  Text(
                    text = mode.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                  )
                }
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = LingoVioletLight
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Voice Recording Consent Toggle
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF1E293B),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "Voice Audio Recording Consent",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Text(
                    "Allow LingoSphere to store your voice recordings on-device for pronunciation replay.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                  )
                }
                Switch(
                  checked = recordingConsent,
                  onCheckedChange = { speechManager.setVoiceRecordingConsent(it) },
                  colors = SwitchDefaults.colors(checkedThumbColor = LingoCyan)
                )
              }
            }

            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }

      if (showCallVoiceSheet) {
        com.example.ui.components.SharedVoiceSelectorBottomSheet(
          title = "AI Partner Voice",
          subtitle = "Switch live voice cleanly without losing your conversation",
          selectedVoice = currentVoice,
          targetLang = targetLang,
          speechManager = speechManager,
          onApplyVoice = { chosenVoice ->
            callManager.switchVoice(chosenVoice)
            showCallVoiceSheet = false
          },
          onDismiss = { showCallVoiceSheet = false }
        )
      }

      // --- Full Transcript & Post-Turn Corrections Bottom Sheet ---
      if (showTranscriptSheet) {
        ModalBottomSheet(
          onDismissRequest = { showTranscriptSheet = false },
          containerColor = Color(0xFF0F172A),
          sheetState = rememberModalBottomSheetState()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Live Conversation Transcript & Coaching",
                  color = Color.White,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${transcript.size} turns exchanged in ${currentMode.title} • ${sessionCorrections.size} coaching notes",
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 12.sp
                )
              }
              ConversationFollowControlChip(
                isFollowing = isFollowingConversation,
                isPausedByManualScroll = isPausedByManualScroll,
                onToggleFollow = { newVal ->
                  isPausedByManualScroll = false
                  isFollowingConversation = newVal
                }
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
              state = transcriptSheetListState,
              modifier = Modifier.height(380.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(transcript) { turn ->
                val isUser = turn.speaker == MessageSender.USER
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = if (isUser) LingoElectricBlue.copy(alpha = 0.25f) else Color(0xFF1E293B)
                    ),
                    border = BorderStroke(
                      1.dp,
                      if (isUser) LingoElectricBlue.copy(alpha = 0.6f) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = if (isUser) "You" else (turn.partnerName ?: currentPartner.name),
                          color = if (isUser) LingoCyan else LingoVioletLight,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp
                        )
                        if (!isUser) {
                          IconButton(
                            onClick = { callManager.speakTurnAloud(turn) },
                            modifier = Modifier.size(48.dp)
                          ) {
                            Icon(
                              Icons.AutoMirrored.Filled.VolumeUp,
                              contentDescription = "Listen",
                              tint = LingoCyan,
                              modifier = Modifier.size(18.dp)
                            )
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      if (isUser) {
                        Text(
                          text = turn.text,
                          color = Color.White,
                          fontSize = 14.sp
                        )
                      } else {
                        SynchronizedAiSpeechText(
                          text = turn.text,
                          speechManager = speechManager,
                          baseTextColor = Color.White,
                          highlightColor = LingoCyan,
                          fontSizeSp = 14
                        )
                      }
                      if (!turn.phoneticGuide.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = "🗣️ ${turn.phoneticGuide}",
                          color = LingoCyan.copy(alpha = 0.8f),
                          fontSize = 11.sp
                        )
                      }
                      val showSheetTranslation = !isUser &&
                        !turn.translation.isNullOrBlank() &&
                        !targetLang.code.equals(nativeLang.code, ignoreCase = true) &&
                        !turn.translation.trim().equals(turn.text.trim(), ignoreCase = true)
                      if (showSheetTranslation) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = "🌐 ${turn.translation}",
                          color = Color.White.copy(alpha = 0.5f),
                          fontSize = 11.sp
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
}

@Composable
fun PostCallReportDialog(
  report: CallLearningReport,
  targetLang: Language,
  onSaveVocabToFlashcard: (term: String, translation: String, example: String) -> Unit = { _, _, _ -> },
  onSaveAndClose: () -> Unit
) {
  var savedTerms by remember { mutableStateOf<Set<String>>(emptySet()) }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xEE090D16))
      .padding(18.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = BorderStroke(1.dp, LingoViolet.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("post_call_report_card")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "🎉 Great Conversation!",
          color = Color.White,
          fontSize = 21.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "You practiced ${report.mode.title.lowercase()} with ${report.partnerName} and learned ${report.vocabularyLearned.size} new words.",
          color = LingoCyan,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          MetricItem(title = "Talk Time", value = "${report.durationSeconds / 60}m ${report.durationSeconds % 60}s")
          MetricItem(title = "Turns", value = "${report.turnsCount}")
          MetricItem(title = "Fluency", value = "${report.fluencyScore}%", highlightColor = LingoEmerald)
          MetricItem(title = "XP Gained", value = "+${report.xpEarned}", highlightColor = LingoAmber)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "${report.partnerName}'s Coaching Summary (CEFR ${report.cefrLevel})",
              color = LingoVioletLight,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = report.overallFeedback,
              color = Color.White.copy(alpha = 0.85f),
              fontSize = 12.5.sp,
              lineHeight = 18.sp
            )
          }
        }

        if (report.grammarCorrections.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "✏️ Corrected Sentences",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
          )
          Spacer(modifier = Modifier.height(6.dp))
          report.grammarCorrections.take(3).forEach { corr ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF1E293B),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("You said: \"${corr.originalPhrase}\"", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                Text("Better: \"${corr.correctedPhrase}\"", color = LingoEmerald, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Text(corr.explanation, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
              }
            }
          }
        }

        if (report.vocabularyLearned.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "📚 New Vocabulary Learned (Tap +Card to Save)",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
          )
          Spacer(modifier = Modifier.height(6.dp))

          report.vocabularyLearned.take(4).forEach { vocab ->
            val isSaved = savedTerms.contains(vocab.term)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${vocab.term} — ${vocab.translation}",
                  color = LingoCyan,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp
                )
                Text(
                  text = vocab.exampleSentence,
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 11.sp
                )
              }
              TextButton(
                onClick = {
                  if (!isSaved) {
                    savedTerms = savedTerms + vocab.term
                    onSaveVocabToFlashcard(vocab.term, vocab.translation, vocab.exampleSentence)
                  }
                }
              ) {
                Text(
                  text = if (isSaved) "Saved ✓" else "+Card",
                  color = if (isSaved) LingoEmerald else LingoAmber,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        if (report.practiceRecommendations.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "🎯 Next Practice Recommendations",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
          )
          Spacer(modifier = Modifier.height(4.dp))
          report.practiceRecommendations.forEach { rec ->
            Text(
              text = "• $rec",
              color = Color(0xFFCBD5E1),
              fontSize = 11.5.sp,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = onSaveAndClose,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("save_call_history_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = LingoViolet)
        ) {
          Text(
            text = "Save Session & Return",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }
    }
  }
}

@Composable
fun MetricItem(
  title: String,
  value: String,
  highlightColor: Color = Color.White
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF1E293B))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(
      text = value,
      color = highlightColor,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp
    )
    Text(
      text = title,
      color = Color.White.copy(alpha = 0.6f),
      fontSize = 11.sp
    )
  }
}
