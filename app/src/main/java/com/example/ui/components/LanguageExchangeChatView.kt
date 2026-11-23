package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ai.GeminiTutorService
import com.example.data.model.CommunityProfile
import com.example.data.model.CountryInfo
import com.example.data.model.DirectChatMessage
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.speech.RecordedVoiceDraft
import com.example.speech.SpeechManager
import com.example.speech.VoiceMessageController
import com.example.speech.VoicePlaybackState
import com.example.speech.VoiceRecordingMode
import com.example.ui.LingoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OutgoingPendingMessage(
  val messageId: String,
  val messageType: String, // "text" | "voice"
  val text: String,
  val replyToMessageId: String = "",
  val replyToSenderName: String = "",
  val replyToText: String = "",
  val voiceDraft: RecordedVoiceDraft? = null,
  val isSending: Boolean = true,
  val errorMessage: String? = null
)

/**
 * Unified, production-grade Language Exchange Private Chat Screen used by both
 * LanguageExchangeScreen and CommunityHubScreen.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LanguageExchangeChatView(
  partner: CommunityProfile,
  userProfile: UserProfile,
  nowMs: Long,
  viewModel: LingoViewModel,
  speechManager: SpeechManager? = null,
  isMuted: Boolean = false,
  onToggleMute: () -> Unit = {},
  applyStatusBarInsets: Boolean = true,
  applyNavigationBarInsets: Boolean = true,
  onBack: () -> Unit,
  onStartVoiceCall: () -> Unit,
  onStartVideoCall: () -> Unit
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val lifecycleOwner = LocalLifecycleOwner.current

  val isAiPartner = remember(partner.userId) { partner.userId.startsWith("ai_partner_") }
  val chatFlow = remember(partner.userId) { viewModel.observeChatMessages(partner.userId) }
  val firestoreMessages by chatFlow.collectAsState(initial = emptyList())
  val aiLocalMessages = remember(partner.userId) {
    mutableStateListOf<DirectChatMessage>().apply {
      if (isAiPartner) {
        add(
          DirectChatMessage(
            id = "ai_welcome_${partner.userId}",
            senderId = partner.userId,
            senderName = partner.displayName,
            receiverId = userProfile.userId,
            text = "Hi! I'm ${partner.displayName} (AI partner). Let's practice ${Language.findByCode(userProfile.targetLanguageCode).name} together — write anything to start!",
            createdAt = com.google.firebase.Timestamp.now()
          )
        )
      }
    }
  }
  val messages = if (isAiPartner) aiLocalMessages else firestoreMessages
  val listState = rememberLazyListState()

  // Voice message controller (real MediaRecorder AAC .m4a + AudioRecord fallback + MediaPlayer)
  val voiceController = remember(context) { VoiceMessageController(context) }
  val recordingMode by voiceController.recordingMode.collectAsState()
  val recordingDurationMs by voiceController.recordingDurationMs.collectAsState()
  val liveWaveform by voiceController.liveWaveform.collectAsState()
  val previewDraft by voiceController.previewDraft.collectAsState()
  val playbackState by voiceController.playbackState.collectAsState()
  val voiceError by voiceController.errorMessage.collectAsState()

  DisposableEffect(lifecycleOwner, voiceController) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        if (voiceController.recordingMode.value == VoiceRecordingMode.HOLD_RECORDING ||
          voiceController.recordingMode.value == VoiceRecordingMode.LOCKED_RECORDING
        ) {
          voiceController.stopRecordingToPreview()
        }
        voiceController.stopPlayback()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      voiceController.release()
    }
  }

  LaunchedEffect(voiceError) {
    val err = voiceError
    if (!err.isNullOrBlank()) {
      Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
      voiceController.clearError()
    }
  }

  var pendingMicActionLock by remember { mutableStateOf(true) }
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      voiceController.startRecording(lockImmediately = pendingMicActionLock)
    } else {
      Toast.makeText(
        context,
        "Microphone permission is required to record voice messages.",
        Toast.LENGTH_LONG
      ).show()
    }
  }

  var inputText by rememberSaveable { mutableStateOf("") }
  var replyingToMessage by remember { mutableStateOf<DirectChatMessage?>(null) }
  var editingMessage by remember { mutableStateOf<DirectChatMessage?>(null) }
  var selectedActionMessage by remember { mutableStateOf<DirectChatMessage?>(null) }
  var reschedulingInviteMessage by remember { mutableStateOf<DirectChatMessage?>(null) }

  var showMoreMenu by remember { mutableStateOf(false) }
  var showScheduleDialog by remember { mutableStateOf(false) }
  var showIcebreakersBar by rememberSaveable { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var showBlockConfirmDialog by remember { mutableStateOf(false) }

  // 50/50 Language Exchange Timer
  var showExchangeTimerBar by rememberSaveable { mutableStateOf(false) }
  var exchangeTimerRunning by remember { mutableStateOf(false) }
  var exchangeSecondsElapsed by remember { mutableIntStateOf(0) }

  LaunchedEffect(exchangeTimerRunning) {
    while (exchangeTimerRunning) {
      delay(1000L)
      exchangeSecondsElapsed += 1
    }
  }

  // Local pending & failed outgoing messages for instant feedback + duplicate-free retry
  val pendingOutgoing = remember { mutableStateListOf<OutgoingPendingMessage>() }

  // Local busy states for per-message AI Learning Coach tools
  val processingMessageActions = remember { mutableStateMapOf<String, String>() }

  // Mark incoming messages as read whenever conversation is open
  LaunchedEffect(messages, partner.userId) {
    if (messages.isNotEmpty()) {
      viewModel.markConversationMessagesRead(partner.userId, messages)
      // Remove any pending outgoing item that has now arrived from Firestore
      val arrivedIds = messages.map { it.id }.toSet()
      pendingOutgoing.removeAll { it.messageId in arrivedIds }
    }
  }

  // Scroll to bottom when new messages arrive or when keyboard opens
  val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
  LaunchedEffect(messages.size, pendingOutgoing.size, imeBottomPx) {
    val totalCount = messages.size + pendingOutgoing.size
    if (totalCount > 0) {
      listState.animateScrollToItem(totalCount - 1)
    }
  }

  val isOnline = partner.isEffectivelyOnline(nowMs)
  val isAway = partner.isEffectivelyAway(nowMs)
  val lastSeenFormatted = partner.formatLastSeen(nowMs)
  val statusText = remember(isOnline, isAway, lastSeenFormatted) {
    when {
      isOnline -> "Online"
      isAway -> if (!lastSeenFormatted.isNullOrBlank()) "Away • $lastSeenFormatted" else "Away"
      else -> lastSeenFormatted ?: "Offline"
    }
  }

  val partnerNativeLang = remember(partner.nativeLanguageCode) {
    Language.findByCode(partner.nativeLanguageCode)
  }
  val partnerTargetLang = remember(partner.targetLanguageCode) {
    Language.findByCode(partner.targetLanguageCode)
  }
  val userTargetLang = remember(userProfile.targetLanguageCode) {
    Language.findByCode(userProfile.targetLanguageCode)
  }
  val userNativeLang = remember(userProfile.nativeLanguageCode) {
    Language.findByCode(userProfile.nativeLanguageCode)
  }

  fun submitTextOrEdit() {
    val trimmed = inputText.trim()
    if (trimmed.isEmpty()) return

    val currentEdit = editingMessage
    if (currentEdit != null) {
      inputText = ""
      editingMessage = null
      viewModel.editDirectMessage(currentEdit.id, trimmed)
      return
    }

    val msgId = "msg_${UUID.randomUUID()}"
    val replyTarget = replyingToMessage
    inputText = ""
    replyingToMessage = null

    if (isAiPartner) {
      if (!userProfile.isEntitlementVerified) {
        inputText = trimmed
        com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
        return
      }
      val userMsg = DirectChatMessage(
        id = msgId,
        senderId = userProfile.userId,
        senderName = userProfile.displayName.ifBlank { "You" },
        receiverId = partner.userId,
        text = trimmed,
        createdAt = com.google.firebase.Timestamp.now()
      )
      aiLocalMessages.add(userMsg)
      val historySummary = aiLocalMessages.takeLast(8).joinToString("\n") { m ->
        val who = if (m.senderId == userProfile.userId) "Learner" else partner.displayName
        "$who: ${m.text}"
      }
      scope.launch {
        val result = com.example.ai.GeminiService.generateText(
          userContext = com.example.ai.GeminiUserContext(
            targetLanguageCode = userProfile.targetLanguageCode,
            nativeLanguageCode = userProfile.nativeLanguageCode,
            cefrLevel = userProfile.proficiencyLevel
          ),
          roleInstruction = "You are ${partner.displayName}, a friendly AI language exchange partner (${partner.bio}). Reply naturally in ${userTargetLang.name} (1-3 concise sentences) and keep the conversation engaging.",
          userPrompt = "Conversation so far:\n$historySummary\n\nReply naturally to the learner's latest message.",
          featureCategory = com.example.ai.GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT
        )
        val replyText = when (result) {
          is com.example.ai.GeminiResult.Success -> result.data
          is com.example.ai.GeminiResult.Offline -> result.message
          is com.example.ai.GeminiResult.QuotaExceeded -> result.message
          is com.example.ai.GeminiResult.Error -> result.message
        }
        aiLocalMessages.add(
          DirectChatMessage(
            id = "ai_reply_${UUID.randomUUID()}",
            senderId = partner.userId,
            senderName = partner.displayName,
            receiverId = userProfile.userId,
            text = replyText,
            createdAt = com.google.firebase.Timestamp.now()
          )
        )
      }
      return
    }

    val pendingItem = OutgoingPendingMessage(
      messageId = msgId,
      messageType = "text",
      text = trimmed,
      replyToMessageId = replyTarget?.id.orEmpty(),
      replyToSenderName = replyTarget?.senderName.orEmpty(),
      replyToText = replyTarget?.text.orEmpty(),
      isSending = true
    )
    pendingOutgoing.add(pendingItem)

    viewModel.sendDirectMessage(
      receiverId = partner.userId,
      text = trimmed,
      messageId = msgId,
      replyToMessageId = pendingItem.replyToMessageId,
      replyToSenderName = pendingItem.replyToSenderName,
      replyToText = pendingItem.replyToText,
      onSuccess = { savedId ->
        pendingOutgoing.removeAll { it.messageId == savedId }
      },
      onError = { err ->
        val idx = pendingOutgoing.indexOfFirst { it.messageId == msgId }
        if (idx >= 0) {
          pendingOutgoing[idx] = pendingOutgoing[idx].copy(
            isSending = false,
            errorMessage = err
          )
        }
      }
    )
  }

  fun submitVoiceDraft(draft: RecordedVoiceDraft) {
    val msgId = "msg_${UUID.randomUUID()}"
    val replyTarget = replyingToMessage
    replyingToMessage = null
    val durSec = (draft.durationMs / 1000L).coerceAtLeast(1L)

    val pendingItem = OutgoingPendingMessage(
      messageId = msgId,
      messageType = "voice",
      text = "🎤 Voice message (${durSec}s)",
      replyToMessageId = replyTarget?.id.orEmpty(),
      replyToSenderName = replyTarget?.senderName.orEmpty(),
      replyToText = replyTarget?.text.orEmpty(),
      voiceDraft = draft,
      isSending = true
    )
    pendingOutgoing.add(pendingItem)

    viewModel.sendVoiceMessage(
      receiverId = partner.userId,
      audioDataBase64 = draft.audioBase64,
      audioDurationMs = draft.durationMs,
      audioMimeType = draft.mimeType,
      audioWaveform = draft.waveform,
      replyToMessageId = pendingItem.replyToMessageId,
      replyToSenderName = pendingItem.replyToSenderName,
      replyToText = pendingItem.replyToText,
      messageId = msgId,
      onSuccess = { savedId ->
        pendingOutgoing.removeAll { it.messageId == savedId }
      },
      onError = { err ->
        val idx = pendingOutgoing.indexOfFirst { it.messageId == msgId }
        if (idx >= 0) {
          pendingOutgoing[idx] = pendingOutgoing[idx].copy(
            isSending = false,
            errorMessage = err
          )
        }
      }
    )
  }

  fun retryFailedOutgoing(item: OutgoingPendingMessage) {
    val idx = pendingOutgoing.indexOfFirst { it.messageId == item.messageId }
    if (idx >= 0) {
      pendingOutgoing[idx] = item.copy(isSending = true, errorMessage = null)
    }
    if (item.messageType == "voice" && item.voiceDraft != null) {
      viewModel.sendVoiceMessage(
        receiverId = partner.userId,
        audioDataBase64 = item.voiceDraft.audioBase64,
        audioDurationMs = item.voiceDraft.durationMs,
        audioMimeType = item.voiceDraft.mimeType,
        audioWaveform = item.voiceDraft.waveform,
        replyToMessageId = item.replyToMessageId,
        replyToSenderName = item.replyToSenderName,
        replyToText = item.replyToText,
        messageId = item.messageId,
        onSuccess = { savedId ->
          pendingOutgoing.removeAll { it.messageId == savedId }
        },
        onError = { err ->
          val i = pendingOutgoing.indexOfFirst { it.messageId == item.messageId }
          if (i >= 0) {
            pendingOutgoing[i] = pendingOutgoing[i].copy(isSending = false, errorMessage = err)
          }
        }
      )
    } else {
      viewModel.sendDirectMessage(
        receiverId = partner.userId,
        text = item.text,
        messageId = item.messageId,
        replyToMessageId = item.replyToMessageId,
        replyToSenderName = item.replyToSenderName,
        replyToText = item.replyToText,
        onSuccess = { savedId ->
          pendingOutgoing.removeAll { it.messageId == savedId }
        },
        onError = { err ->
          val i = pendingOutgoing.indexOfFirst { it.messageId == item.messageId }
          if (i >= 0) {
            pendingOutgoing[i] = pendingOutgoing[i].copy(isSending = false, errorMessage = err)
          }
        }
      )
    }
  }

  fun triggerTranscribeVoiceMessage(msg: DirectChatMessage) {
    if (!userProfile.isEntitlementVerified) {
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
      return
    }
    if (msg.id.isBlank() || msg.audioDataBase64.isBlank()) return
    processingMessageActions[msg.id] = "Transcribing audio..."
    scope.launch {
      try {
        val transcript = GeminiTutorService.transcribeVoiceMessageAudio(
          audioBase64 = msg.audioDataBase64,
          mimeType = msg.audioMimeType,
          targetLanguage = userTargetLang,
          nativeLanguage = userNativeLang
        )
        if (transcript.isNotBlank()) {
          viewModel.updateDirectMessageFields(
            messageId = msg.id,
            updates = mapOf("transcriptText" to transcript)
          )
        } else {
          Toast.makeText(context, "Could not detect clear speech in this recording.", Toast.LENGTH_SHORT).show()
        }
      } catch (e: Exception) {
        val friendly = com.example.ai.AiErrorHandler.handle(e, userProfile.isEntitlementVerified)
        Toast.makeText(context, friendly.message, Toast.LENGTH_SHORT).show()
      } finally {
        processingMessageActions.remove(msg.id)
      }
    }
  }

  fun triggerTranslateMessage(msg: DirectChatMessage) {
    if (!userProfile.isEntitlementVerified) {
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
      return
    }
    if (msg.id.isBlank()) return
    processingMessageActions[msg.id] = "Translating..."
    scope.launch {
      try {
        val sourceText = if (msg.isVoiceMessage()) {
          msg.transcriptText?.takeIf { it.isNotBlank() } ?: run {
            val t = GeminiTutorService.transcribeVoiceMessageAudio(
              audioBase64 = msg.audioDataBase64,
              mimeType = msg.audioMimeType,
              targetLanguage = userTargetLang,
              nativeLanguage = userNativeLang
            )
            if (t.isNotBlank()) {
              viewModel.updateDirectMessageFields(msg.id, mapOf("transcriptText" to t))
            }
            t
          }
        } else {
          msg.text
        }
        if (sourceText.isNotBlank()) {
          val translated = GeminiTutorService.translateChatMessage(
            text = sourceText,
            targetLanguage = userTargetLang,
            nativeLanguage = userNativeLang
          )
          viewModel.updateDirectMessageFields(
            messageId = msg.id,
            updates = mapOf("translatedText" to translated)
          )
        }
      } catch (e: Exception) {
        val friendly = com.example.ai.AiErrorHandler.handle(e, userProfile.isEntitlementVerified)
        Toast.makeText(context, friendly.message, Toast.LENGTH_SHORT).show()
      } finally {
        processingMessageActions.remove(msg.id)
      }
    }
  }

  fun triggerGrammarCheckMessage(msg: DirectChatMessage) {
    if (!userProfile.isEntitlementVerified) {
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
      return
    }
    if (msg.id.isBlank()) return
    processingMessageActions[msg.id] = "Checking speech & grammar..."
    scope.launch {
      try {
        val sourceText = if (msg.isVoiceMessage()) {
          msg.transcriptText?.takeIf { it.isNotBlank() } ?: run {
            val t = GeminiTutorService.transcribeVoiceMessageAudio(
              audioBase64 = msg.audioDataBase64,
              mimeType = msg.audioMimeType,
              targetLanguage = userTargetLang,
              nativeLanguage = userNativeLang
            )
            if (t.isNotBlank()) {
              viewModel.updateDirectMessageFields(msg.id, mapOf("transcriptText" to t))
            }
            t
          }
        } else {
          msg.text
        }
        if (sourceText.isNotBlank()) {
          val feedback = GeminiTutorService.checkChatMessageGrammar(
            text = sourceText,
            targetLanguage = userTargetLang,
            nativeLanguage = userNativeLang
          )
          viewModel.updateDirectMessageFields(
            messageId = msg.id,
            updates = mapOf("grammarCorrectionText" to feedback)
          )
        }
      } catch (e: Exception) {
        val friendly = com.example.ai.AiErrorHandler.handle(e, userProfile.isEntitlementVerified)
        Toast.makeText(context, friendly.message, Toast.LENGTH_SHORT).show()
      } finally {
        processingMessageActions.remove(msg.id)
      }
    }
  }

  fun triggerSaveUsefulWords(msg: DirectChatMessage) {
    if (!userProfile.isEntitlementVerified) {
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
      return
    }
    if (msg.id.isBlank()) return
    processingMessageActions[msg.id] = "Saving useful vocabulary..."
    scope.launch {
      try {
        val sourceText = if (msg.isVoiceMessage()) {
          msg.transcriptText?.takeIf { it.isNotBlank() } ?: GeminiTutorService.transcribeVoiceMessageAudio(
            audioBase64 = msg.audioDataBase64,
            mimeType = msg.audioMimeType,
            targetLanguage = userTargetLang,
            nativeLanguage = userNativeLang
          )
        } else {
          msg.text
        }
        if (sourceText.isNotBlank()) {
          val words = GeminiTutorService.extractChatMessageVocab(
            text = sourceText,
            targetLanguage = userTargetLang,
            nativeLanguage = userNativeLang
          )
          words.forEach { (term, meaning, phonetic) ->
            viewModel.saveVocabularyToFlashcards(
              frontText = term,
              backText = meaning,
              pronunciation = phonetic
            )
          }
          Toast.makeText(
            context,
            "Saved ${words.size} vocabulary item${if (words.size == 1) "" else "s"} to your Flashcards!",
            Toast.LENGTH_SHORT
          ).show()
        }
      } catch (e: Exception) {
        val friendly = com.example.ai.AiErrorHandler.handle(e, userProfile.isEntitlementVerified)
        Toast.makeText(context, friendly.message, Toast.LENGTH_SHORT).show()
      } finally {
        processingMessageActions.remove(msg.id)
      }
    }
  }

  val isKeyboardOpen = imeBottomPx > 0
  val bottomInsets = if (isKeyboardOpen) {
    WindowInsets.ime
  } else if (applyNavigationBarInsets) {
    WindowInsets.navigationBars
  } else {
    WindowInsets(0, 0, 0, 0)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF050B18))
      .then(
        if (applyStatusBarInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier
      )
  ) {
    // =========================================================================
    // 1. COMPACT STICKY HEADER (Remains visible while scrolling messages)
    // =========================================================================
    Surface(
      color = Color(0xFF0A1328),
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(40.dp)
                .testTag("chat_header_back_btn")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }

            LingoAvatar(
              name = partner.displayName,
              userId = partner.userId,
              size = 38.dp,
              customImageUrl = partner.avatarUrl,
              isOnline = isOnline
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = partner.displayName.ifBlank { "Language Partner" },
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false)
                )
                // Language Pair Badge
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF142444),
                  border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f))
                ) {
                  Text(
                    text = "${partnerNativeLang.flagEmoji} ${partner.nativeLanguageCode.uppercase()} ⇄ ${partnerTargetLang.flagEmoji} ${partner.targetLanguageCode.uppercase()}",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(2.dp))

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        isOnline -> Color(0xFF10B981)
                        isAway -> Color(0xFFF59E0B)
                        else -> Color(0xFF64748B)
                      }
                    )
                )
                Text(
                  text = statusText,
                  color = when {
                    isOnline -> Color(0xFF34D399)
                    isAway -> Color(0xFFFBBF24)
                    else -> Color(0xFF94A3B8)
                  },
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Voice Call, Video Call, and More Options Buttons
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = onStartVoiceCall,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF06B6D4).copy(alpha = 0.14f))
                .testTag("chat_header_voice_call_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Voice Call",
                tint = Color(0xFF06B6D4),
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = onStartVideoCall,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6).copy(alpha = 0.16f))
                .testTag("chat_header_video_call_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Call",
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(19.dp)
              )
            }

            Box {
              IconButton(
                onClick = { showMoreMenu = true },
                modifier = Modifier
                  .size(38.dp)
                  .testTag("chat_header_more_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "More Options",
                  tint = Color(0xFFCBD5E1),
                  modifier = Modifier.size(20.dp)
                )
              }

              DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                modifier = Modifier.background(Color(0xFF0F1B33))
              ) {
                DropdownMenuItem(
                  text = { Text("Schedule Exchange Session", color = Color.White, fontSize = 13.sp) },
                  leadingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF06B6D4))
                  },
                  onClick = {
                    showMoreMenu = false
                    showScheduleDialog = true
                  }
                )
                DropdownMenuItem(
                  text = {
                    Text(
                      if (showExchangeTimerBar) "Hide 50/50 Exchange Timer" else "Show 50/50 Exchange Timer",
                      color = Color.White,
                      fontSize = 13.sp
                    )
                  },
                  leadingIcon = {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF10B981))
                  },
                  onClick = {
                    showMoreMenu = false
                    showExchangeTimerBar = !showExchangeTimerBar
                  }
                )
                DropdownMenuItem(
                  text = {
                    Text(
                      if (showIcebreakersBar) "Hide Conversation Starters" else "Show Conversation Starters",
                      color = Color.White,
                      fontSize = 13.sp
                    )
                  },
                  leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF38BDF8))
                  },
                  onClick = {
                    showMoreMenu = false
                    showIcebreakersBar = !showIcebreakersBar
                  }
                )
                DropdownMenuItem(
                  text = {
                    Text(
                      if (isMuted) "Unmute Notifications" else "Mute Notifications",
                      color = Color.White,
                      fontSize = 13.sp
                    )
                  },
                  leadingIcon = {
                    Icon(
                      if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                      contentDescription = null,
                      tint = Color(0xFFFBBF24)
                    )
                  },
                  onClick = {
                    showMoreMenu = false
                    onToggleMute()
                  }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                DropdownMenuItem(
                  text = { Text("Report ${partner.displayName}", color = Color(0xFFF87171), fontSize = 13.sp) },
                  leadingIcon = {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFF87171))
                  },
                  onClick = {
                    showMoreMenu = false
                    showReportDialog = true
                  }
                )
                DropdownMenuItem(
                  text = { Text("Block ${partner.displayName}", color = Color(0xFFEF4444), fontSize = 13.sp) },
                  leadingIcon = {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444))
                  },
                  onClick = {
                    showMoreMenu = false
                    showBlockConfirmDialog = true
                  }
                )
              }
            }
          }
        }

        // Optional Collapsible 50/50 Language Exchange Timer Bar
        AnimatedVisibility(visible = showExchangeTimerBar) {
          val isFirstHalfNative = (exchangeSecondsElapsed / 300) % 2 == 0
          val activeExchangeLang = if (isFirstHalfNative) userTargetLang else partnerTargetLang
          val remainingPhaseSec = 300 - (exchangeSecondsElapsed % 300)

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF0E1A35))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (exchangeTimerRunning) Color(0xFF10B981) else Color(0xFF38BDF8),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (exchangeTimerRunning) {
                  "50/50 Turn: Speak ${activeExchangeLang.flagEmoji} ${activeExchangeLang.name} (${remainingPhaseSec / 60}:${(remainingPhaseSec % 60).toString().padStart(2, '0')})"
                } else {
                  "50/50 Timer: 5m ${userTargetLang.name} ↔ 5m ${partnerTargetLang.name}"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (exchangeTimerRunning) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                modifier = Modifier.clickable { exchangeTimerRunning = !exchangeTimerRunning }
              ) {
                Text(
                  text = if (exchangeTimerRunning) "Pause" else "Start",
                  color = if (exchangeTimerRunning) Color(0xFFF87171) else Color(0xFF34D399),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              IconButton(onClick = { showExchangeTimerBar = false }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Timer", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        // Optional Collapsible Icebreaker Prompts Bar
        AnimatedVisibility(visible = showIcebreakersBar) {
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF091224))
              .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(CountryInfo.CONVERSATION_STARTERS, key = { it }) { starter ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF111F3D),
                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.3f)),
                modifier = Modifier.clickable {
                  inputText = starter
                  showIcebreakersBar = false
                }
              ) {
                Text(
                  text = "💬 $starter",
                  color = Color(0xFFE2E8F0),
                  fontSize = 11.sp,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
              }
            }
          }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
      }
    }

    // =========================================================================
    // 2. MESSAGE STREAM (Compact Bubbles, Date Headers, Voice Notes, Schedule Cards)
    // =========================================================================
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      val maxBubbleWidth = maxWidth * 0.80f

      if (messages.isEmpty() && pendingOutgoing.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Surface(
            shape = CircleShape,
            color = Color(0xFF06B6D4).copy(alpha = 0.12f),
            modifier = Modifier.size(56.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = Color(0xFF06B6D4),
                modifier = Modifier.size(28.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Chat with ${partner.displayName}",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Send a message, record a real voice note, or schedule a 50/50 language exchange session.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { showIcebreakersBar = true },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("Icebreakers", color = Color(0xFF38BDF8), fontSize = 12.sp)
            }
            OutlinedButton(
              onClick = { showScheduleDialog = true },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Schedule Session", color = Color(0xFF06B6D4), fontSize = 12.sp)
            }
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .testTag("exchange_chat_messages_list"),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
          itemsIndexed(
            items = messages,
            key = { index, msg -> msg.id.ifBlank { "${msg.senderId}_${msg.createdAt?.seconds ?: index}_$index" } }
          ) { index, msg ->
            val prevMsg = messages.getOrNull(index - 1)
            val nextMsg = messages.getOrNull(index + 1)

            val showDateHeader = remember(msg.createdAt, prevMsg?.createdAt) {
              shouldShowDateDivider(prevMsg?.createdAt?.toDate(), msg.createdAt?.toDate())
            }
            val isSameSenderAsPrev = prevMsg != null && prevMsg.senderId == msg.senderId && !showDateHeader
            val isSameSenderAsNext = nextMsg != null && nextMsg.senderId == msg.senderId &&
              !shouldShowDateDivider(msg.createdAt?.toDate(), nextMsg.createdAt?.toDate())

            val topSpacing = when {
              index == 0 -> 0.dp
              showDateHeader -> 8.dp
              isSameSenderAsPrev -> 3.dp
              else -> 10.dp
            }

            if (showDateHeader) {
              DateHeaderChip(date = msg.createdAt?.toDate())
            }

            Spacer(modifier = Modifier.height(topSpacing))

            val isMine = msg.senderId == userProfile.userId
            val actionStatus = processingMessageActions[msg.id]

            when {
              msg.isScheduledInvitation() -> {
                ScheduledExchangeInviteBubble(
                  msg = msg,
                  isMine = isMine,
                  maxBubbleWidth = maxBubbleWidth,
                  onAccept = {
                    viewModel.respondToScheduledExchangeInvitation(msg, newStatus = "accepted")
                    Toast.makeText(context, "Exchange session accepted!", Toast.LENGTH_SHORT).show()
                  },
                  onDecline = {
                    viewModel.respondToScheduledExchangeInvitation(msg, newStatus = "declined")
                  },
                  onSuggestAnotherTime = {
                    reschedulingInviteMessage = msg
                  }
                )
              }

              msg.isVoiceMessage() -> {
                VoiceMessageBubble(
                  msg = msg,
                  isMine = isMine,
                  isLastInGroup = !isSameSenderAsNext,
                  maxBubbleWidth = maxBubbleWidth,
                  playbackState = playbackState,
                  processingLabel = actionStatus,
                  onTogglePlay = {
                    voiceController.togglePlayVoiceMessage(
                      messageId = msg.id,
                      audioBase64 = msg.audioDataBase64,
                      mimeType = msg.audioMimeType,
                      fallbackDurationMs = msg.audioDurationMs
                    )
                  },
                  onSeekFraction = { frac ->
                    voiceController.seekToFraction(msg.id, frac)
                  },
                  onCycleSpeed = {
                    voiceController.cyclePlaybackSpeed()
                  },
                  onTranscribe = { triggerTranscribeVoiceMessage(msg) },
                  onTranslate = { triggerTranslateMessage(msg) },
                  onCorrectSpeech = { triggerGrammarCheckMessage(msg) },
                  onSaveWords = { triggerSaveUsefulWords(msg) },
                  onSpeakTranslation = { textToSpeak ->
                    speechManager?.speak(textToSpeak, userProfile.targetLanguageCode)
                  },
                  onLongPress = { selectedActionMessage = msg }
                )
              }

              else -> {
                CompactTextMessageBubble(
                  msg = msg,
                  isMine = isMine,
                  isLastInGroup = !isSameSenderAsNext,
                  maxBubbleWidth = maxBubbleWidth,
                  processingLabel = actionStatus,
                  onSpeakTranslation = { textToSpeak ->
                    speechManager?.speak(textToSpeak, userProfile.targetLanguageCode)
                  },
                  onLongPress = { selectedActionMessage = msg }
                )
              }
            }
          }

          // Pending / Failed Outgoing Messages (with Retry & Cancel)
          items(
            items = pendingOutgoing,
            key = { "pending_${it.messageId}" }
          ) { pending ->
            Spacer(modifier = Modifier.height(6.dp))
            PendingOutgoingBubble(
              pending = pending,
              maxBubbleWidth = maxBubbleWidth,
              onRetry = { retryFailedOutgoing(pending) },
              onCancel = { pendingOutgoing.removeAll { it.messageId == pending.messageId } }
            )
          }
        }
      }
    }

    // =========================================================================
    // 3. BOTTOM COMPOSER AREA (Respects Navigation Bar + Keyboard IME Insets)
    // =========================================================================
    Surface(
      color = Color(0xFF0A1328),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(bottomInsets)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)
      ) {
        // Reply or Edit Context Banner
        AnimatedVisibility(visible = replyingToMessage != null || editingMessage != null) {
          val bannerTitle = if (editingMessage != null) {
            "Editing message"
          } else {
            "Replying to ${replyingToMessage?.senderName.orEmpty().ifBlank { partner.displayName }}"
          }
          val bannerBody = editingMessage?.text ?: replyingToMessage?.text.orEmpty()

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF122142),
            border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = bannerTitle,
                  color = Color(0xFF38BDF8),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
                Text(
                  text = bannerBody,
                  color = Color(0xFFCBD5E1),
                  fontSize = 11.5.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              IconButton(
                onClick = {
                  replyingToMessage = null
                  if (editingMessage != null) {
                    editingMessage = null
                    inputText = ""
                  }
                },
                modifier = Modifier.size(26.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Cancel reply or edit",
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(15.dp)
                )
              }
            }
          }
        }

        // Composer Mode Switch: Recording / Preview / Standard Input
        when (recordingMode) {
          VoiceRecordingMode.HOLD_RECORDING,
          VoiceRecordingMode.LOCKED_RECORDING,
          VoiceRecordingMode.PAUSED -> {
            VoiceRecordingActiveBar(
              recordingMode = recordingMode,
              durationMs = recordingDurationMs,
              liveWaveform = liveWaveform,
              onPause = { voiceController.pauseRecording() },
              onResume = { voiceController.resumeRecording() },
              onPreview = { voiceController.stopRecordingToPreview() },
              onCancel = { voiceController.cancelRecording() },
              onSendNow = {
                val draft = voiceController.finishRecordingForSend()
                if (draft != null) {
                  submitVoiceDraft(draft)
                }
              }
            )
          }

          VoiceRecordingMode.PREVIEW_READY -> {
            val draft = previewDraft
            if (draft != null) {
              VoicePreviewComposerBar(
                draft = draft,
                playbackState = playbackState,
                onTogglePlayPreview = { voiceController.togglePlayPreviewDraft() },
                onSeekFraction = { frac ->
                  voiceController.seekToFraction(VoiceMessageController.PREVIEW_DRAFT_ID, frac)
                },
                onDeleteDraft = { voiceController.discardPreviewDraft() },
                onSendDraft = {
                  val finalized = voiceController.finishRecordingForSend()
                  if (finalized != null) {
                    submitVoiceDraft(finalized)
                  }
                }
              )
            }
          }

          VoiceRecordingMode.IDLE -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Bottom,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              // Schedule / Attachment Quick Button
              IconButton(
                onClick = { showScheduleDialog = true },
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF12203D))
                  .testTag("chat_schedule_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = "Schedule Language Exchange",
                  tint = Color(0xFF38BDF8),
                  modifier = Modifier.size(20.dp)
                )
              }

              // Compact Multiline Input Field (Expands naturally up to 4 lines)
              OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                  Text(
                    text = "Message ${partner.displayName}...",
                    color = Color(0xFF64748B),
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                },
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitTextOrEdit() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color(0xFF101D38),
                  unfocusedContainerColor = Color(0xFF101D38),
                  focusedBorderColor = Color(0xFF06B6D4),
                  unfocusedBorderColor = Color(0xFF1E325C),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 44.dp, max = 112.dp)
                  .testTag("direct_chat_input")
              )

              val canSend = inputText.trim().isNotEmpty()

              // Voice Recording Mic Button (Always accessible; Tap for hands-free or Hold to record)
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(11.dp))
                  .background(if (canSend) Color(0xFF101E3A) else Color(0xFF122448))
                  .border(
                    1.dp,
                    if (canSend) Color(0xFF06B6D4).copy(alpha = 0.35f) else Color(0xFF06B6D4).copy(alpha = 0.65f),
                    RoundedCornerShape(11.dp)
                  )
                  .testTag("chat_voice_record_btn")
                  .pointerInput(Unit) {
                    detectTapGestures(
                      onTap = {
                        if (voiceController.hasRecordPermission()) {
                          voiceController.startRecording(lockImmediately = true)
                        } else {
                          pendingMicActionLock = true
                          micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                      },
                      onLongPress = {
                        if (voiceController.hasRecordPermission()) {
                          voiceController.startRecording(lockImmediately = false)
                        } else {
                          pendingMicActionLock = false
                          micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                      }
                    )
                  }
                  .pointerInput(Unit) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    detectDragGesturesAfterLongPress(
                      onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                      },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        if (totalDragX < -110f) {
                          voiceController.cancelRecording()
                        } else if (totalDragY < -85f) {
                          voiceController.lockRecording()
                        }
                      },
                      onDragEnd = {
                        if (voiceController.recordingMode.value == VoiceRecordingMode.HOLD_RECORDING) {
                          val draft = voiceController.finishRecordingForSend()
                          if (draft != null) {
                            submitVoiceDraft(draft)
                          }
                        }
                      },
                      onDragCancel = {
                        if (voiceController.recordingMode.value == VoiceRecordingMode.HOLD_RECORDING) {
                          voiceController.cancelRecording()
                        }
                      }
                    )
                  }
              ) {
                Icon(
                  imageVector = Icons.Default.Mic,
                  contentDescription = "Tap or hold to record voice message",
                  tint = if (canSend) Color(0xFF38BDF8) else Color(0xFF06B6D4),
                  modifier = Modifier.size(19.dp)
                )
              }

              // Send Button (Prioritized in bright cyan when text is entered)
              IconButton(
                onClick = { submitTextOrEdit() },
                enabled = canSend,
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(11.dp))
                  .background(
                    if (canSend) Color(0xFF06B6D4) else Color(0xFF162442)
                  )
                  .testTag("direct_chat_send_btn")
              ) {
                Icon(
                  imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send Message",
                  tint = if (canSend) Color(0xFF03111F) else Color(0xFF475569),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // ===========================================================================
  // DIALOGS: Message Context Actions, Schedule Session, Reschedule, Block, Report
  // ===========================================================================

  if (selectedActionMessage != null) {
    val targetMsg = selectedActionMessage!!
    val isMyMsg = targetMsg.senderId == userProfile.userId
    AlertDialog(
      onDismissRequest = { selectedActionMessage = null },
      containerColor = Color(0xFF0F1B33),
      titleContentColor = Color.White,
      textContentColor = Color.White,
      title = {
        Text(
          text = if (targetMsg.isVoiceMessage()) "Voice Message Actions" else "Message Options",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          MessageOptionRow(
            icon = Icons.AutoMirrored.Filled.Reply,
            label = "Reply to Message",
            tint = Color(0xFF38BDF8),
            onClick = {
              replyingToMessage = targetMsg
              selectedActionMessage = null
            }
          )
          if (!targetMsg.isVoiceMessage()) {
            MessageOptionRow(
              icon = Icons.Default.ContentCopy,
              label = "Copy Text",
              tint = Color(0xFFCBD5E1),
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("LingoSphere Message", targetMsg.text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                selectedActionMessage = null
              }
            )
          } else {
            MessageOptionRow(
              icon = Icons.Default.Subtitles,
              label = "Transcribe Audio",
              tint = Color(0xFF06B6D4),
              onClick = {
                selectedActionMessage = null
                triggerTranscribeVoiceMessage(targetMsg)
              }
            )
          }
          MessageOptionRow(
            icon = Icons.Default.Translate,
            label = "Translate (${userTargetLang.name} ⇄ ${userNativeLang.name})",
            tint = Color(0xFF38BDF8),
            onClick = {
              selectedActionMessage = null
              triggerTranslateMessage(targetMsg)
            }
          )
          MessageOptionRow(
            icon = Icons.Default.Spellcheck,
            label = if (targetMsg.isVoiceMessage()) "Correct My Speech" else "Grammar & Phrasing Check",
            tint = Color(0xFF10B981),
            onClick = {
              selectedActionMessage = null
              triggerGrammarCheckMessage(targetMsg)
            }
          )
          MessageOptionRow(
            icon = Icons.Default.BookmarkAdd,
            label = "Save Useful Words to Flashcards",
            tint = Color(0xFFFBBF24),
            onClick = {
              selectedActionMessage = null
              triggerSaveUsefulWords(targetMsg)
            }
          )
          if (isMyMsg && !targetMsg.isVoiceMessage() && !targetMsg.isScheduledInvitation()) {
            MessageOptionRow(
              icon = Icons.Default.Edit,
              label = "Edit Message",
              tint = Color(0xFF60A5FA),
              onClick = {
                editingMessage = targetMsg
                inputText = targetMsg.text
                selectedActionMessage = null
              }
            )
          }
          if (isMyMsg) {
            MessageOptionRow(
              icon = Icons.Default.Delete,
              label = "Delete Message",
              tint = Color(0xFFEF4444),
              onClick = {
                viewModel.deleteDirectMessage(targetMsg.id)
                selectedActionMessage = null
                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
              }
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedActionMessage = null }) {
          Text("Close", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  if (showScheduleDialog) {
    ScheduleExchangeSessionDialog(
      partnerName = partner.displayName,
      onDismiss = { showScheduleDialog = false },
      onConfirm = { topic, dateLabel, timeLabel, tzLabel ->
        showScheduleDialog = false
        viewModel.scheduleExchangeSession(
          partnerId = partner.userId,
          partnerName = partner.displayName,
          topic = topic,
          timeSlot = "$dateLabel at $timeLabel",
          dateLabel = dateLabel,
          timeLabel = timeLabel,
          timezoneLabel = tzLabel
        )
        Toast.makeText(context, "Scheduled exchange invitation sent!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  if (reschedulingInviteMessage != null) {
    val inviteMsg = reschedulingInviteMessage!!
    SuggestAnotherTimeDialog(
      currentTopic = inviteMsg.resolvedScheduleTopic(),
      onDismiss = { reschedulingInviteMessage = null },
      onConfirm = { newDateLabel, newTimeLabel ->
        reschedulingInviteMessage = null
        viewModel.respondToScheduledExchangeInvitation(
          message = inviteMsg,
          newStatus = "rescheduled",
          proposedDateLabel = newDateLabel,
          proposedTimeLabel = newTimeLabel
        )
        Toast.makeText(context, "Proposed new time: $newDateLabel at $newTimeLabel", Toast.LENGTH_SHORT).show()
      }
    )
  }

  if (showBlockConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showBlockConfirmDialog = false },
      containerColor = Color(0xFF0F1B33),
      titleContentColor = Color.White,
      textContentColor = Color.White,
      title = { Text("Block ${partner.displayName}?", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Blocking ${partner.displayName} will hide them from your partner feed and prevent direct calls or messages.",
          color = Color(0xFFCBD5E1),
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showBlockConfirmDialog = false
            viewModel.blockUser(partner.userId, partner.displayName) {
              Toast.makeText(context, "Blocked ${partner.displayName}", Toast.LENGTH_SHORT).show()
              onBack()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Block User", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showBlockConfirmDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  if (showReportDialog) {
    val reasons = listOf("Spam or commercial", "Inappropriate language", "Harassment", "Fake profile")
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var reportDetails by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      containerColor = Color(0xFF0F1B33),
      titleContentColor = Color.White,
      textContentColor = Color.White,
      title = { Text("Report ${partner.displayName}", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Select a reason:", color = Color(0xFF94A3B8), fontSize = 12.sp)
          reasons.forEach { reason ->
            FilterChip(
              selected = selectedReason == reason,
              onClick = { selectedReason = reason },
              label = { Text(reason, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFEF4444),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF142444),
                labelColor = Color.White
              )
            )
          }
          OutlinedTextField(
            value = reportDetails,
            onValueChange = { reportDetails = it },
            label = { Text("Details (optional)") },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showReportDialog = false
            viewModel.reportUser(partner.userId, selectedReason, reportDetails) {
              Toast.makeText(context, "Report submitted. Thank you for keeping LingoSphere safe.", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }
}

// =============================================================================
// SUB-COMPONENTS: Compact Text Bubble, Scheduled Exchange Card, Voice Bubble
// =============================================================================

private fun cleanLearningNoteText(raw: String): String {
  var cleaned = raw
    .replace("**", "")
    .replace("__", "")
    .replace("###", "")
    .replace("##", "")
    .replace("`", "")
    .trim()

  val prefixesToRemove = listOf(
    "English:", "english:", "Note:", "note:", "Explanation:", "explanation:",
    "Correction:", "correction:", "Grammar:", "grammar:", "Feedback:", "feedback:",
    "Natural phrasing:", "Natural Phrasing:", "Natural:", "natural:"
  )
  for (prefix in prefixesToRemove) {
    if (cleaned.startsWith(prefix, ignoreCase = true)) {
      cleaned = cleaned.substring(prefix.length).trim()
    }
  }
  return cleaned
}

@Composable
private fun MessageLearningAssistanceSection(
  translatedText: String?,
  grammarCorrectionText: String?,
  isMine: Boolean,
  onSpeakTranslation: (String) -> Unit
) {
  val hasTranslation = !translatedText.isNullOrBlank()
  val hasGrammar = !grammarCorrectionText.isNullOrBlank()
  if (!hasTranslation && !hasGrammar) return

  var isExpanded by remember { mutableStateOf(false) }

  val cleanTranslation = remember(translatedText) {
    cleanLearningNoteText(translatedText.orEmpty())
  }
  val cleanGrammar = remember(grammarCorrectionText) {
    cleanLearningNoteText(grammarCorrectionText.orEmpty())
  }

  Spacer(modifier = Modifier.height(5.dp))

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isMine) Color(0xFF03111F).copy(alpha = 0.16f) else Color(0xFF091428),
    border = BorderStroke(
      1.dp,
      if (isMine) Color(0xFF03111F).copy(alpha = 0.28f) else Color(0xFF1E3A6D)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      // Toggle Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = if (isMine) Color(0xFF03111F) else Color(0xFF38BDF8),
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = if (hasGrammar && hasTranslation) "Learning Notes & Translation"
                   else if (hasGrammar) "Language Correction"
                   else "Translation",
            color = if (isMine) Color(0xFF03111F) else Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = if (isExpanded) "Hide" else "Show",
            color = if (isMine) Color(0xFF03111F).copy(alpha = 0.75f) else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
          )
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = if (isMine) Color(0xFF03111F).copy(alpha = 0.75f) else Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
          )
        }
      }

      // Expandable Body
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier.padding(top = 6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (cleanTranslation.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = if (isMine) Color(0xFF03111F).copy(alpha = 0.85f) else Color(0xFF38BDF8),
                modifier = Modifier
                  .size(13.dp)
                  .padding(top = 2.dp)
              )
              Text(
                text = cleanTranslation,
                color = if (isMine) Color(0xFF03111F) else Color(0xFFBAE6FD),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { onSpeakTranslation(cleanTranslation) },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = "Listen to translation",
                  tint = if (isMine) Color(0xFF03111F) else Color.White,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
          }

          if (cleanGrammar.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Spellcheck,
                contentDescription = null,
                tint = if (isMine) Color(0xFF03111F) else Color(0xFF34D399),
                modifier = Modifier
                  .size(13.dp)
                  .padding(top = 2.dp)
              )
              Text(
                text = cleanGrammar,
                color = if (isMine) Color(0xFF03111F) else Color(0xFFD1FAE5),
                fontSize = 11.5.sp,
                lineHeight = 15.5.sp,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTextMessageBubble(
  msg: DirectChatMessage,
  isMine: Boolean,
  isLastInGroup: Boolean,
  maxBubbleWidth: androidx.compose.ui.unit.Dp,
  processingLabel: String?,
  onSpeakTranslation: (String) -> Unit,
  onLongPress: () -> Unit
) {
  val bubbleShape = RoundedCornerShape(
    topStart = 15.dp,
    topEnd = 15.dp,
    bottomStart = if (isMine || !isLastInGroup) 15.dp else 4.dp,
    bottomEnd = if (!isMine || !isLastInGroup) 15.dp else 4.dp
  )

  val bubbleBg = if (isMine) Color(0xFF06B6D4) else Color(0xFF101E3B)
  val primaryTextColor = if (isMine) Color(0xFF03111F) else Color.White
  val metaTextColor = if (isMine) Color(0xFF03111F).copy(alpha = 0.68f) else Color(0xFF94A3B8)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      shape = bubbleShape,
      color = bubbleBg,
      border = if (isMine) null else BorderStroke(1.dp, Color(0xFF1E325C)),
      modifier = Modifier
        .widthIn(min = 72.dp, max = maxBubbleWidth)
        .clip(bubbleShape)
        .combinedClickable(
          onClick = {},
          onLongClick = onLongPress
        )
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
      ) {
        // Reply quote inside bubble if replying to a message
        if (msg.replyToText.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isMine) Color(0xFF03111F).copy(alpha = 0.14f) else Color(0xFF091224),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 5.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
              Text(
                text = msg.replyToSenderName.ifBlank { "Message" },
                color = if (isMine) Color(0xFF03111F) else Color(0xFF38BDF8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
              Text(
                text = msg.replyToText,
                color = if (isMine) Color(0xFF03111F).copy(alpha = 0.85f) else Color(0xFFCBD5E1),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Main Message Text
        Text(
          text = msg.text,
          color = primaryTextColor,
          fontSize = 14.sp,
          lineHeight = 18.5.sp
        )

        // Compact Expandable Language Learning Corrections & Translation
        MessageLearningAssistanceSection(
          translatedText = msg.translatedText,
          grammarCorrectionText = msg.grammarCorrectionText,
          isMine = isMine,
          onSpeakTranslation = onSpeakTranslation
        )

        if (!processingLabel.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = processingLabel,
            color = metaTextColor,
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Compact Timestamp & Delivery / Read Status Row
        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (msg.editedAtEpochMs > 0L) {
            Text(
              text = "edited",
              color = metaTextColor,
              fontSize = 9.5.sp,
              fontStyle = FontStyle.Italic
            )
          }
          Text(
            text = formatMessageTime(msg.createdAt?.toDate()),
            color = metaTextColor,
            fontSize = 10.sp
          )
          if (isMine) {
            DeliveryStatusIcon(
              status = msg.deliveryStatus,
              onCyanBackground = true
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ScheduledExchangeInviteBubble(
  msg: DirectChatMessage,
  isMine: Boolean,
  maxBubbleWidth: androidx.compose.ui.unit.Dp,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onSuggestAnotherTime: () -> Unit
) {
  val status = msg.scheduleStatus.lowercase()
  val (statusLabel, statusColor) = when (status) {
    "accepted" -> "Accepted" to Color(0xFF10B981)
    "declined" -> "Declined" to Color(0xFFEF4444)
    "rescheduled" -> "Time Proposed" to Color(0xFFF59E0B)
    else -> "Pending" to Color(0xFF38BDF8)
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF0E1D3A),
      border = BorderStroke(
        width = 1.dp,
        color = if (status == "accepted") Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF06B6D4).copy(alpha = 0.45f)
      ),
      modifier = Modifier
        .widthIn(min = 220.dp, max = maxBubbleWidth)
        .testTag("scheduled_exchange_card")
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Header Row: Icon + Title + Status Pill
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF06B6D4).copy(alpha = 0.16f),
              modifier = Modifier.size(26.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.EventAvailable,
                  contentDescription = null,
                  tint = Color(0xFF06B6D4),
                  modifier = Modifier.size(15.dp)
                )
              }
            }
            Text(
              text = "Language Exchange Invite",
              color = Color.White,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = statusColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
          ) {
            Text(
              text = statusLabel,
              color = statusColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }

        // Structured Metadata Grid
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF081226)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ScheduleMetaRow(label = "Topic", value = msg.resolvedScheduleTopic())
            ScheduleMetaRow(label = "Date", value = msg.resolvedScheduleDateLabel())
            ScheduleMetaRow(
              label = "Time",
              value = "${msg.resolvedScheduleTimeLabel()} (${msg.resolvedScheduleTimezone()})"
            )
            ScheduleMetaRow(label = "Host", value = msg.senderName.ifBlank { "Partner" })
            if (msg.scheduleProposedText.isNotBlank()) {
              ScheduleMetaRow(
                label = "Proposed",
                value = msg.scheduleProposedText,
                highlightColor = Color(0xFFFBBF24)
              )
            }
          }
        }

        // Interactive Actions for Pending / Rescheduled Invites
        val showActions = status == "pending" || status == "rescheduled"
        if (showActions) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = onAccept,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color(0xFF03111F)
              ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .testTag("schedule_accept_btn")
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            OutlinedButton(
              onClick = onDecline,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .testTag("schedule_decline_btn")
            ) {
              Text("Decline", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
          }

          OutlinedButton(
            onClick = onSuggestAnotherTime,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 36.dp)
              .testTag("schedule_reschedule_btn")
          ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Suggest Another Time", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        // Timestamp + Delivery Row
        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = formatMessageTime(msg.createdAt?.toDate()),
            color = Color(0xFF64748B),
            fontSize = 10.sp
          )
          if (isMine) {
            DeliveryStatusIcon(status = msg.deliveryStatus, onCyanBackground = false)
          }
        }
      }
    }
  }
}

@Composable
private fun ScheduleMetaRow(
  label: String,
  value: String,
  highlightColor: Color = Color.White
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "$label:",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.width(58.dp)
    )
    Text(
      text = value,
      color = highlightColor,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun VoiceMessageBubble(
  msg: DirectChatMessage,
  isMine: Boolean,
  isLastInGroup: Boolean,
  maxBubbleWidth: androidx.compose.ui.unit.Dp,
  playbackState: VoicePlaybackState,
  processingLabel: String?,
  onTogglePlay: () -> Unit,
  onSeekFraction: (Float) -> Unit,
  onCycleSpeed: () -> Unit,
  onTranscribe: () -> Unit,
  onTranslate: () -> Unit,
  onCorrectSpeech: () -> Unit,
  onSaveWords: () -> Unit,
  onSpeakTranslation: (String) -> Unit,
  onLongPress: () -> Unit
) {
  val isThisPlaying = playbackState.activeMessageId == msg.id && playbackState.isPlaying
  val isThisActive = playbackState.activeMessageId == msg.id
  val totalDurMs = (if (isThisActive && playbackState.durationMs > 0L) playbackState.durationMs else msg.audioDurationMs)
    .coerceAtLeast(1000L)
  val currentPosMs = if (isThisActive) playbackState.currentPositionMs.coerceIn(0L, totalDurMs) else 0L
  val progressFraction = if (totalDurMs > 0L) (currentPosMs.toFloat() / totalDurMs.toFloat()).coerceIn(0f, 1f) else 0f

  val bars = remember(msg.audioWaveform) {
    if (msg.audioWaveform.size >= 12) {
      msg.audioWaveform.take(32)
    } else {
      List(28) { idx -> 20 + ((idx * 17) % 55) }
    }
  }

  val bubbleShape = RoundedCornerShape(
    topStart = 15.dp,
    topEnd = 15.dp,
    bottomStart = if (isMine || !isLastInGroup) 15.dp else 4.dp,
    bottomEnd = if (!isMine || !isLastInGroup) 15.dp else 4.dp
  )

  val containerColor = if (isMine) Color(0xFF0A2E46) else Color(0xFF101E3B)
  val borderColor = if (isMine) Color(0xFF06B6D4).copy(alpha = 0.55f) else Color(0xFF1E325C)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      shape = bubbleShape,
      color = containerColor,
      border = BorderStroke(1.dp, borderColor),
      modifier = Modifier
        .widthIn(min = 215.dp, max = maxBubbleWidth)
        .clip(bubbleShape)
        .combinedClickable(
          onClick = {},
          onLongClick = onLongPress
        )
        .testTag("voice_message_bubble")
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Player Row: Play/Pause + Interactive Waveform + Speed Pill
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF06B6D4))
              .testTag("voice_bubble_play_btn")
          ) {
            Icon(
              imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isThisPlaying) "Pause voice message" else "Play voice message",
              tint = Color(0xFF03111F),
              modifier = Modifier.size(20.dp)
            )
          }

          // Interactive Waveform Seek Bar
          Column(modifier = Modifier.weight(1f)) {
            WaveformSeekStrip(
              bars = bars,
              progressFraction = progressFraction,
              activeColor = Color(0xFF06B6D4),
              inactiveColor = Color.White.copy(alpha = 0.25f),
              onSeek = onSeekFraction
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isThisActive && (isThisPlaying || currentPosMs > 0L)) {
                  "${formatDurationClock(currentPosMs)} / ${formatDurationClock(totalDurMs)}"
                } else {
                  formatDurationClock(totalDurMs)
                },
                color = Color(0xFFCBD5E1),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = formatMessageTime(msg.createdAt?.toDate()),
                  color = Color(0xFF94A3B8),
                  fontSize = 10.sp
                )
                if (isMine) {
                  DeliveryStatusIcon(status = msg.deliveryStatus, onCyanBackground = false)
                }
              }
            }
          }

          // Playback Speed Pill (1x / 1.5x / 2x)
          val speedLabel = when {
            kotlin.math.abs(playbackState.playbackSpeed - 1.5f) < 0.05f -> "1.5x"
            kotlin.math.abs(playbackState.playbackSpeed - 2.0f) < 0.05f -> "2x"
            else -> "1x"
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF071529),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
            modifier = Modifier
              .clickable { onCycleSpeed() }
              .testTag("voice_bubble_speed_btn")
          ) {
            Text(
              text = speedLabel,
              color = Color(0xFF38BDF8),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
          }
        }

        // Optional Language Learning Tools Strip (Transcribe, Translate, Speech Coach, Save Words)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          VoiceCoachChip(label = "Transcribe", onClick = onTranscribe)
          VoiceCoachChip(label = "Translate", onClick = onTranslate)
          VoiceCoachChip(label = "Correct Speech", onClick = onCorrectSpeech)
          VoiceCoachChip(label = "Save Words", onClick = onSaveWords)
        }

        if (!processingLabel.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            CircularProgressIndicator(
              color = Color(0xFF06B6D4),
              strokeWidth = 1.5.dp,
              modifier = Modifier.size(11.dp)
            )
            Text(
              text = processingLabel,
              color = Color(0xFF38BDF8),
              fontSize = 10.5.sp
            )
          }
        }

        // Transcript Section (Displayed separately without altering the original audio)
        if (!msg.transcriptText.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF071326)
          ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
              Text(
                text = "Transcript",
                color = Color(0xFF38BDF8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = msg.transcriptText,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }
        }

        // Compact Expandable Language Learning Corrections & Translation
        MessageLearningAssistanceSection(
          translatedText = msg.translatedText,
          grammarCorrectionText = msg.grammarCorrectionText,
          isMine = isMine,
          onSpeakTranslation = onSpeakTranslation
        )
      }
    }
  }
}

@Composable
private fun VoiceCoachChip(
  label: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = Color(0xFF08162B),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    modifier = Modifier.clickable { onClick() }
  ) {
    Text(
      text = label,
      color = Color(0xFFCBD5E1),
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
    )
  }
}

@Composable
private fun WaveformSeekStrip(
  bars: List<Int>,
  progressFraction: Float,
  activeColor: Color,
  inactiveColor: Color,
  onSeek: (Float) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(24.dp)
      .pointerInput(bars.size) {
        detectTapGestures { offset ->
          val width = size.width.toFloat().coerceAtLeast(1f)
          onSeek((offset.x / width).coerceIn(0f, 1f))
        }
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    val count = bars.size.coerceAtLeast(1)
    bars.forEachIndexed { idx, amp ->
      val barProgress = idx.toFloat() / count.toFloat()
      val isPlayed = barProgress <= progressFraction
      val barHeightDp = (5f + (amp.coerceIn(8, 100) / 100f) * 18f).dp
      Box(
        modifier = Modifier
          .weight(1f)
          .height(barHeightDp)
          .clip(RoundedCornerShape(50))
          .background(if (isPlayed) activeColor else inactiveColor)
      )
    }
  }
}

@Composable
private fun VoiceRecordingActiveBar(
  recordingMode: VoiceRecordingMode,
  durationMs: Long,
  liveWaveform: List<Int>,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onPreview: () -> Unit,
  onCancel: () -> Unit,
  onSendNow: () -> Unit
) {
  val isPaused = recordingMode == VoiceRecordingMode.PAUSED
  val isHoldMode = recordingMode == VoiceRecordingMode.HOLD_RECORDING

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF101F3E),
    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("voice_recording_active_bar")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444))
        )
        Text(
          text = formatDurationClock(durationMs),
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )

        // Live Microphone Waveform Bars
        Row(
          modifier = Modifier
            .weight(1f)
            .height(22.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          val displayBars = if (liveWaveform.isEmpty()) List(24) { 12 } else liveWaveform.takeLast(24)
          displayBars.forEach { amp ->
            val h = (4f + (amp.coerceIn(6, 100) / 100f) * 17f).dp
            Box(
              modifier = Modifier
                .width(3.dp)
                .height(h)
                .clip(RoundedCornerShape(50))
                .background(if (isPaused) Color(0xFFFBBF24) else Color(0xFF06B6D4))
            )
          }
        }

        if (isHoldMode) {
          Text(
            text = "◀ Slide to cancel • ▲ Lock",
            color = Color(0xFF94A3B8),
            fontSize = 10.5.sp,
            maxLines = 1
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = onCancel,
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.16f))
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Cancel Recording", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { if (isPaused) onResume() else onPause() },
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(
                imageVector = if (isPaused) Icons.Default.Mic else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = onPreview,
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.16f))
            ) {
              Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop and Preview",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = onSendNow,
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF06B6D4))
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Voice Message",
                tint = Color(0xFF03111F),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VoicePreviewComposerBar(
  draft: RecordedVoiceDraft,
  playbackState: VoicePlaybackState,
  onTogglePlayPreview: () -> Unit,
  onSeekFraction: (Float) -> Unit,
  onDeleteDraft: () -> Unit,
  onSendDraft: () -> Unit
) {
  val isPreviewPlaying = playbackState.activeMessageId == VoiceMessageController.PREVIEW_DRAFT_ID && playbackState.isPlaying
  val isPreviewActive = playbackState.activeMessageId == VoiceMessageController.PREVIEW_DRAFT_ID
  val totalMs = draft.durationMs.coerceAtLeast(1000L)
  val currentMs = if (isPreviewActive) playbackState.currentPositionMs.coerceIn(0L, totalMs) else 0L
  val progress = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF10203E),
    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("voice_preview_composer_bar")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      IconButton(
        onClick = onDeleteDraft,
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(Color(0xFFEF4444).copy(alpha = 0.16f))
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete voice draft",
          tint = Color(0xFFF87171),
          modifier = Modifier.size(17.dp)
        )
      }

      IconButton(
        onClick = onTogglePlayPreview,
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(Color(0xFF06B6D4).copy(alpha = 0.2f))
      ) {
        Icon(
          imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isPreviewPlaying) "Pause preview" else "Play preview",
          tint = Color(0xFF06B6D4),
          modifier = Modifier.size(19.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        WaveformSeekStrip(
          bars = draft.waveform,
          progressFraction = progress,
          activeColor = Color(0xFF06B6D4),
          inactiveColor = Color.White.copy(alpha = 0.25f),
          onSeek = onSeekFraction
        )
        Text(
          text = "Preview • ${formatDurationClock(if (isPreviewActive && currentMs > 0L) currentMs else totalMs)}",
          color = Color(0xFF94A3B8),
          fontSize = 10.5.sp
        )
      }

      Button(
        onClick = onSendDraft,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF06B6D4),
          contentColor = Color(0xFF03111F)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.heightIn(min = 36.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Send", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun PendingOutgoingBubble(
  pending: OutgoingPendingMessage,
  maxBubbleWidth: androidx.compose.ui.unit.Dp,
  onRetry: () -> Unit,
  onCancel: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = if (pending.errorMessage != null) Color(0xFF2A121D) else Color(0xFF0C2A44),
      border = BorderStroke(
        1.dp,
        if (pending.errorMessage != null) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFF06B6D4).copy(alpha = 0.4f)
      ),
      modifier = Modifier.widthIn(max = maxBubbleWidth)
    ) {
      Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
        Text(
          text = pending.text,
          color = Color.White,
          fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (pending.isSending) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            CircularProgressIndicator(
              color = Color(0xFF06B6D4),
              strokeWidth = 1.5.dp,
              modifier = Modifier.size(11.dp)
            )
            Text(
              text = "Sending...",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp
            )
          }
        } else if (pending.errorMessage != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = Color(0xFFF87171),
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "Failed to send",
              color = Color(0xFFF87171),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Retry",
              color = Color(0xFF38BDF8),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable { onRetry() }
            )
            Text(
              text = "Cancel",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp,
              modifier = Modifier.clickable { onCancel() }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DateHeaderChip(date: Date?) {
  val label = remember(date) { formatDateHeaderLabel(date) }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(50),
      color = Color(0xFF0E1B34),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
      Text(
        text = label,
        color = Color(0xFF94A3B8),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
      )
    }
  }
}

@Composable
private fun DeliveryStatusIcon(
  status: String,
  onCyanBackground: Boolean
) {
  val normalized = status.lowercase()
  val isRead = normalized == "read"
  val icon = if (normalized == "sent") Icons.Default.Check else Icons.Default.DoneAll
  val tint = when {
    isRead && onCyanBackground -> Color(0xFF03111F)
    isRead && !onCyanBackground -> Color(0xFF38BDF8)
    onCyanBackground -> Color(0xFF03111F).copy(alpha = 0.65f)
    else -> Color(0xFF94A3B8)
  }
  Icon(
    imageVector = icon,
    contentDescription = status,
    tint = tint,
    modifier = Modifier.size(13.dp)
  )
}

@Composable
private fun MessageOptionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    Text(label, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleExchangeSessionDialog(
  partnerName: String,
  onDismiss: () -> Unit,
  onConfirm: (topic: String, dateLabel: String, timeLabel: String, timezoneLabel: String) -> Unit
) {
  val dateOptions = listOf("Today", "Tomorrow", "This Saturday", "This Sunday")
  val timeOptions = listOf("10:00 AM", "2:00 PM", "6:00 PM", "8:00 PM")
  val defaultTz = remember { TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT) }

  var selectedDate by remember { mutableStateOf(dateOptions[1]) }
  var selectedTime by remember { mutableStateOf(timeOptions[2]) }
  var topicText by remember { mutableStateOf("Travel, Culture & Daily Conversation") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F1B33),
    titleContentColor = Color.White,
    textContentColor = Color.White,
    title = {
      Text("Schedule Language Exchange", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Propose a structured 50/50 speaking session with $partnerName:",
          color = Color(0xFF94A3B8),
          fontSize = 12.sp
        )

        OutlinedTextField(
          value = topicText,
          onValueChange = { topicText = it },
          label = { Text("Conversation Topic") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF06B6D4),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Text("Select Day:", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          dateOptions.forEach { d ->
            FilterChip(
              selected = selectedDate == d,
              onClick = { selectedDate = d },
              label = { Text(d, fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF06B6D4),
                selectedLabelColor = Color(0xFF03111F),
                containerColor = Color(0xFF142444),
                labelColor = Color.White
              )
            )
          }
        }

        Text("Select Time ($defaultTz):", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          timeOptions.forEach { t ->
            FilterChip(
              selected = selectedTime == t,
              onClick = { selectedTime = t },
              label = { Text(t, fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF06B6D4),
                selectedLabelColor = Color(0xFF03111F),
                containerColor = Color(0xFF142444),
                labelColor = Color.White
              )
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            topicText.trim().ifBlank { "50/50 Language Exchange" },
            selectedDate,
            selectedTime,
            defaultTz
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF06B6D4),
          contentColor = Color(0xFF03111F)
        )
      ) {
        Text("Send Invitation", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF94A3B8))
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestAnotherTimeDialog(
  currentTopic: String,
  onDismiss: () -> Unit,
  onConfirm: (dateLabel: String, timeLabel: String) -> Unit
) {
  val dateOptions = listOf("Today", "Tomorrow", "Saturday", "Sunday", "Next Week")
  val timeOptions = listOf("9:00 AM", "12:00 PM", "4:00 PM", "7:30 PM", "9:00 PM")
  var selectedDate by remember { mutableStateOf(dateOptions[1]) }
  var selectedTime by remember { mutableStateOf(timeOptions[3]) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F1B33),
    titleContentColor = Color.White,
    textContentColor = Color.White,
    title = {
      Text("Suggest Another Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Topic: $currentTopic",
          color = Color(0xFF38BDF8),
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text("New Day:", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          dateOptions.forEach { d ->
            FilterChip(
              selected = selectedDate == d,
              onClick = { selectedDate = d },
              label = { Text(d, fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF06B6D4),
                selectedLabelColor = Color(0xFF03111F),
                containerColor = Color(0xFF142444),
                labelColor = Color.White
              )
            )
          }
        }
        Text("New Time:", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          timeOptions.forEach { t ->
            FilterChip(
              selected = selectedTime == t,
              onClick = { selectedTime = t },
              label = { Text(t, fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF06B6D4),
                selectedLabelColor = Color(0xFF03111F),
                containerColor = Color(0xFF142444),
                labelColor = Color.White
              )
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedDate, selectedTime) },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF06B6D4),
          contentColor = Color(0xFF03111F)
        )
      ) {
        Text("Propose Time", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF94A3B8))
      }
    }
  )
}

// =============================================================================
// FORMATTING HELPERS
// =============================================================================

private fun formatMessageTime(date: Date?): String {
  if (date == null) return "Now"
  return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

private fun formatDurationClock(durationMs: Long): String {
  val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
  val mins = totalSec / 60L
  val secs = totalSec % 60L
  return String.format(Locale.US, "%d:%02d", mins, secs)
}

private fun shouldShowDateDivider(prev: Date?, current: Date?): Boolean {
  if (current == null) return false
  if (prev == null) return true
  val calPrev = Calendar.getInstance().apply { time = prev }
  val calCurr = Calendar.getInstance().apply { time = current }
  return calPrev.get(Calendar.YEAR) != calCurr.get(Calendar.YEAR) ||
    calPrev.get(Calendar.DAY_OF_YEAR) != calCurr.get(Calendar.DAY_OF_YEAR)
}

private fun formatDateHeaderLabel(date: Date?): String {
  if (date == null) return "Today"
  val nowCal = Calendar.getInstance()
  val msgCal = Calendar.getInstance().apply { time = date }
  return when {
    nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
      nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> "Today"
    nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
      nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
    else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
  }
}
