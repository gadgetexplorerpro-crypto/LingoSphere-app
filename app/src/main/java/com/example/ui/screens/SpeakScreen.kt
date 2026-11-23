package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ai.GeminiTutorService
import com.example.data.model.AiConversationPartner
import com.example.data.model.CallMode
import com.example.data.model.ChatMessage
import com.example.data.model.CorrectionMode
import com.example.data.model.InterviewEvaluationResult
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.RoleplayEvaluationReport
import com.example.data.model.RoleplayScenario
import com.example.data.model.SavedAiConversationRecord
import com.example.data.model.SpeakingAssessmentResult
import com.example.data.model.SpeakingSession
import com.example.data.model.TutorVoice
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoListRow
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTutorOrb
import com.example.ui.components.PartnerSpeakingWaveform
import com.example.ui.components.formatCefrLevelLabel
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.components.springTap
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoEmerald
import kotlinx.coroutines.launch
import java.util.UUID

enum class SpeakActiveSection {
  HOME,
  CONVERSATION,
  MISSIONS,
  EXAM,
  INTERVIEW,
  PHONE_MODES,
  LABS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onChangeTargetLanguage: (String) -> Unit = {},
  onSaveSession: (SpeakingSession) -> Unit,
  onStartVoiceCall: () -> Unit = {},
  onStartVideoTutorCall: () -> Unit = {},
  speakingSessions: List<SpeakingSession> = emptyList(),
  onRecordGrammarMistake: (MistakeNotebookEntry) -> Unit = {},
  onOpenImmersionMode: () -> Unit = {},
  onOpenConversationReplay: () -> Unit = {},
  onOpenCameraLens: () -> Unit = {},
  onOpenAccentStudio: () -> Unit = {},
  onOpenLanguageEmergencyMode: () -> Unit = {},
  onOpenAiDebatePartner: () -> Unit = {},
  onOpenExplainItThreeWays: () -> Unit = {},
  selectedPartner: AiConversationPartner = AiConversationPartner.ALL_PARTNERS.first(),
  onSelectPartner: (AiConversationPartner) -> Unit = {},
  onStartConfiguredVoiceConversation: (
    partner: AiConversationPartner,
    groupPartners: List<AiConversationPartner>,
    mode: CallMode,
    voice: TutorVoice,
    textMode: Boolean,
    correctionMode: CorrectionMode,
    cefrLevel: String,
    resumedRecord: SavedAiConversationRecord?
  ) -> Unit = { _, _, _, _, _, _, _, _ -> onStartVoiceCall() },
  savedAiConversations: List<SavedAiConversationRecord> = emptyList()
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  var currentLanguageCode by remember(userProfile.targetLanguageCode) {
    mutableStateOf(userProfile.targetLanguageCode.ifBlank { "es" })
  }
  val targetLang = Language.findByCode(currentLanguageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)
  val isVerifiedPro = userProfile.isEntitlementVerified

  var activeSection by remember { mutableStateOf(SpeakActiveSection.HOME) }
  var showLanguagePicker by remember { mutableStateOf(false) }
  var showPartnersDirectorySheet by remember { mutableStateOf(false) }
  var showVoiceSelectionSheet by remember { mutableStateOf(false) }
  var showGroupPartnersSheet by remember { mutableStateOf(false) }
  var showHistorySheet by remember { mutableStateOf(false) }
  var showMoreOptionsSheet by remember { mutableStateOf(false) }

  val prefs = remember {
    context.applicationContext.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
  }

  var currentSelectedPartner by remember(selectedPartner.id) {
    val savedPartnerId = prefs.getString("selected_ai_partner_id", null)
    val initial = if (selectedPartner.id == AiConversationPartner.DEFAULT.id && !savedPartnerId.isNullOrBlank()) {
      AiConversationPartner.findById(savedPartnerId)
    } else {
      selectedPartner
    }
    mutableStateOf(initial)
  }
  var detailPartner by remember { mutableStateOf<AiConversationPartner?>(null) }
  var partnerSearchQuery by remember { mutableStateOf("") }
  var selectedLevelFilter by remember { mutableStateOf("All") }
  var selectedAccentFilter by remember { mutableStateOf("All") }
  var selectedTopicMode by remember { mutableStateOf(CallMode.FREE_TALK) }
  var selectedVoice by remember {
    val savedVoiceId = prefs.getString("selected_tutor_voice_id", null)
    val initialVoice = if (!savedVoiceId.isNullOrBlank()) {
      TutorVoice.findById(savedVoiceId)
    } else {
      currentSelectedPartner.voice
    }
    mutableStateOf(initialVoice)
  }
  var selectedCorrectionMode by remember { mutableStateOf(CorrectionMode.IMMEDIATE) }
  val cefrBands = remember { listOf("A1", "A2", "B1", "B2", "C1", "C2") }
  var selectedCefrBand by remember(userProfile.proficiencyLevel) {
    mutableStateOf(
      when (userProfile.proficiencyLevel) {
        "Beginner" -> "A1"
        "Advanced" -> "C1"
        else -> "B1"
      }
    )
  }

  LaunchedEffect(selectedVoice.id) {
    speechManager.setPartnerVoice(selectedVoice)
  }

  val persistPartnerSelection: (AiConversationPartner, TutorVoice) -> Unit = { partner, voice ->
    currentSelectedPartner = partner
    selectedVoice = voice
    speechManager.setPartnerVoice(voice)
    try {
      prefs.edit()
        .putString("selected_ai_partner_id", partner.id)
        .putString("selected_tutor_voice_id", voice.id)
        .apply()
    } catch (_: Exception) {}
    onSelectPartner(partner)
  }

  var selectedGroupPartnerIds by remember {
    mutableStateOf(listOf("partner_emma", "partner_james"))
  }

  BackHandler(enabled = activeSection != SpeakActiveSection.HOME) {
    activeSection = SpeakActiveSection.HOME
  }

  val conversationMessages = remember(currentLanguageCode, currentSelectedPartner.id) {
    mutableStateListOf<ChatMessage>()
  }
  var activeConversationMode by remember { mutableStateOf(com.example.ai.TutorConversationMode.FREE_CONVERSATION) }
  var isAiGenerating by remember { mutableStateOf(false) }
  var conversationErrorMessage by remember { mutableStateOf<String?>(null) }
  var lastFailedUserMessage by remember { mutableStateOf<String?>(null) }
  var conversationInputText by remember { mutableStateOf("") }
  var autoCorrectionsEnabled by remember { mutableStateOf(true) }
  var showRescueSheet by remember { mutableStateOf(false) }
  var rescueSheetContent by remember { mutableStateOf<String?>(null) }
  var isGeneratingRescue by remember { mutableStateOf(false) }
  val conversationListState = rememberLazyListState()

  var activeMission by remember { mutableStateOf<RoleplayScenario?>(null) }
  var missionSearch by remember { mutableStateOf("") }
  var selectedMissionCategory by remember { mutableStateOf("All") }
  val missionMessages = remember(activeMission?.id) { mutableStateListOf<ChatMessage>() }
  var missionInputText by remember { mutableStateOf("") }
  var missionEvaluationReport by remember { mutableStateOf<RoleplayEvaluationReport?>(null) }
  var isEvaluatingMission by remember { mutableStateOf(false) }

  var selectedExamLevel by remember { mutableStateOf("Placement") }
  var examAttemptSeed by remember { mutableIntStateOf(0) }
  var recentExamPromptIds by remember {
    val raw = prefs.getString("recent_exam_prompt_ids", "") ?: ""
    val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.takeLast(10)
    mutableStateOf(list)
  }
  val examPromptsPair = remember(selectedExamLevel, targetLang.name, examAttemptSeed) {
    com.example.data.model.SpeakingExamPromptBank.selectFourPrompts(
      level = selectedExamLevel,
      languageName = targetLang.name,
      recentPromptIds = recentExamPromptIds
    )
  }
  val examPromptIds = examPromptsPair.first
  val examPrompts = examPromptsPair.second
  var currentExamQuestionIndex by remember { mutableIntStateOf(0) }
  val examAnswersByIndex = remember { androidx.compose.runtime.mutableStateMapOf<Int, String>() }
  val examVoiceUsedByIndex = remember { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>() }
  val examSpeechSecsByIndex = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
  var examResult by remember { mutableStateOf<SpeakingAssessmentResult?>(null) }
  var isEvaluatingExam by remember { mutableStateOf(false) }

  var interviewIndustry by remember { mutableStateOf("Technology & Software") }
  var interviewRole by remember { mutableStateOf("Software Engineer") }
  var interviewDifficulty by remember { mutableStateOf("Standard") }
  var isInterviewStarted by remember { mutableStateOf(false) }
  val interviewMessages = remember { mutableStateListOf<ChatMessage>() }
  var interviewAnswerText by remember { mutableStateOf("") }
  var interviewResultReport by remember { mutableStateOf<InterviewEvaluationResult?>(null) }
  var isEvaluatingInterview by remember { mutableStateOf(false) }

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val isListening by speechManager.isListening.collectAsState()
  val isSpeaking by speechManager.isSpeaking.collectAsState()
  val speechAperture by speechManager.speechAperture.collectAsState()
  val currentViseme by speechManager.currentViseme.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  val partialSpokenText by speechManager.partialSpokenText.collectAsState()
  val previewingVoiceId by speechManager.previewingVoiceId.collectAsState()

  LaunchedEffect(Unit) {
    speechManager.clearSpokenText()
  }

  val micLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasMicPermission = granted
    if (granted) {
      speechManager.startListening(targetLang.code)
    } else {
      Toast.makeText(context, "Microphone permission is required for voice practice.", Toast.LENGTH_SHORT).show()
    }
  }

  val toggleMic: () -> Unit = {
    if (isListening) {
      speechManager.finishListeningNow()
    } else if (hasMicPermission) {
      speechManager.startListening(targetLang.code)
    } else {
      micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  LaunchedEffect(currentLanguageCode, currentSelectedPartner.id) {
    if (conversationMessages.isEmpty()) {
      val pName = currentSelectedPartner.name
      val greeting = when (targetLang.code.lowercase()) {
        "es" -> "¡Hola! Soy $pName (${currentSelectedPartner.roleTitle}). ¿De qué te gustaría hablar hoy?"
        "fr" -> "Bonjour ! Je suis $pName (${currentSelectedPartner.roleTitle}). De quoi aimeriez-vous parler aujourd'hui ?"
        "de" -> "Hallo! Ich bin $pName (${currentSelectedPartner.roleTitle}). Worüber möchtest du heute sprechen?"
        "it" -> "Ciao! Sono $pName (${currentSelectedPartner.roleTitle}). Di cosa vorresti parlare oggi?"
        "ja" -> "こんにちは！${pName}です。日本語で楽しくお話ししましょう。今日はどんな話題にしますか？"
        "zh" -> "你好！我是$pName，你的口语伙伴。今天想聊些什么呢？"
        else -> currentSelectedPartner.sampleGreeting
      }
      conversationMessages.add(
        ChatMessage(
          id = UUID.randomUUID().toString(),
          sender = MessageSender.AI_TUTOR,
          text = greeting,
          translation = "Hi! I'm $pName, your ${currentSelectedPartner.roleTitle}. What would you like to talk about today?"
        )
      )
    }
  }

  LaunchedEffect(activeSection, currentLanguageCode, currentSelectedPartner.id) {
    if (activeSection == SpeakActiveSection.CONVERSATION && conversationMessages.size == 1) {
      val opening = conversationMessages.firstOrNull()
      if (opening != null && opening.sender == MessageSender.AI_TUTOR) {
        speechManager.speakWithTutorVoice(
          text = opening.text,
          voice = selectedVoice,
          languageCode = targetLang.code
        )
      }
    }
  }

  LaunchedEffect(conversationMessages.size) {
    if (conversationMessages.isNotEmpty()) {
      conversationListState.animateScrollToItem(conversationMessages.size - 1)
    }
  }

  LaunchedEffect(activeMission?.id, targetLang.code) {
    activeMission?.let { mission ->
      missionMessages.clear()
      val openingText = when (targetLang.code.lowercase()) {
        "es" -> "¡Hola! Soy ${currentSelectedPartner.name}. Empecemos la misión '${mission.title}'. ¿Qué te gustaría decir primero?"
        "fr" -> "Bonjour ! Je suis ${currentSelectedPartner.name}. Commençons la mission « ${mission.title} ». Que souhaitez-vous dire ?"
        "de" -> "Hallo! Ich bin ${currentSelectedPartner.name}. Beginnen wir mit '${mission.title}'. Was möchtest du zuerst sagen?"
        "it" -> "Ciao! Sono ${currentSelectedPartner.name}. Iniziamo la missione '${mission.title}'. Cosa vorresti dire?"
        "ja" -> "こんにちは！${currentSelectedPartner.name}です。「${mission.title}」のミッションを始めましょう。"
        "zh" -> "你好！我是${currentSelectedPartner.name}。让我们开始“${mission.title}”任务吧。"
        else -> mission.openingGreetingTemplate
      }
      missionMessages.add(
        ChatMessage(
          id = UUID.randomUUID().toString(),
          sender = MessageSender.AI_TUTOR,
          text = openingText,
          translation = mission.openingGreetingTemplate
        )
      )
      speechManager.speakWithTutorVoice(
        text = openingText,
        voice = selectedVoice,
        languageCode = targetLang.code
      )
    }
  }

  val dispatchConversationTurn: (String, Boolean) -> Unit = { textToSend, appendUserBubble ->
    if (!isVerifiedPro) {
      conversationInputText = textToSend
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
    } else if (textToSend.isNotBlank() && !isAiGenerating) {
      conversationErrorMessage = null
      lastFailedUserMessage = null
      speechManager.stopSpeaking()
      if (appendUserBubble) {
        conversationMessages.add(
          ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = textToSend
          )
        )
      }
      isAiGenerating = true
      coroutineScope.launch {
        try {
          val history = conversationMessages.dropLast(1).map {
            (if (it.sender == MessageSender.USER) "USER" else "MODEL") to it.text
          }
          val scenarioTitleForMode = when (activeConversationMode) {
            com.example.ai.TutorConversationMode.FREE_CONVERSATION -> null
            com.example.ai.TutorConversationMode.LEARN -> selectedTopicMode.title
            com.example.ai.TutorConversationMode.PRACTICE -> selectedTopicMode.title
            com.example.ai.TutorConversationMode.ROLEPLAY -> selectedTopicMode.title
          }
          val tutorResponse = GeminiTutorService.chatWithTutor(
            userSpeechInput = textToSend,
            targetLanguage = targetLang,
            nativeLanguage = nativeLang,
            proficiencyLevel = selectedCefrBand.ifBlank { userProfile.proficiencyLevel },
            scenarioTitle = scenarioTitleForMode,
            scenarioInstruction = if (activeConversationMode == com.example.ai.TutorConversationMode.FREE_CONVERSATION) null else selectedTopicMode.systemInstruction,
            conversationHistory = history,
            partnerName = currentSelectedPartner.name,
            conversationMode = activeConversationMode,
            hasSpeechRecognitionTelemetry = false
          )
          if (tutorResponse.isServiceError || tutorResponse.replyInTargetLanguage.isBlank()) {
            conversationErrorMessage = com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
              tutorResponse.errorMessage,
              isPro = isVerifiedPro
            )
            lastFailedUserMessage = textToSend
          } else {
            val cleanFeedback = if (selectedCorrectionMode == CorrectionMode.CONVERSATION_ONLY) {
              null
            } else {
              tutorResponse.grammarFeedback
            }
            conversationMessages.add(
              ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AI_TUTOR,
                text = tutorResponse.replyInTargetLanguage,
                phoneticGuide = tutorResponse.phoneticGuide,
                translation = tutorResponse.translationInNativeLanguage,
                grammarFeedback = cleanFeedback
              )
            )
            speechManager.speakWithTutorVoice(
              text = tutorResponse.replyInTargetLanguage,
              voice = selectedVoice,
              languageCode = targetLang.code
            )
            onSaveSession(
              SpeakingSession(
                languageCode = targetLang.code,
                mode = activeConversationMode.label,
                scenarioTitle = "${currentSelectedPartner.name}: ${selectedTopicMode.title}",
                proficiencyLevel = selectedCefrBand.ifBlank { userProfile.proficiencyLevel },
                turnsCount = conversationMessages.size.toLong(),
                fluencyScore = 92L,
                grammarFeedbackSummary = cleanFeedback ?: "Natural conversational flow."
              )
            )
          }
        } catch (e: Exception) {
          conversationErrorMessage = com.example.ai.AiErrorHandler.handle(e, isVerifiedPro).message
          lastFailedUserMessage = textToSend
        } finally {
          isAiGenerating = false
        }
      }
    }
  }

  val sendConversationMessage: (String) -> Unit = { rawText ->
    val textToSend = rawText.trim()
    if (textToSend.isNotBlank() && !isAiGenerating) {
      conversationInputText = ""
      focusManager.clearFocus()
      dispatchConversationTurn(textToSend, true)
    }
  }

  val sendMissionMessage: (String) -> Unit = { rawText ->
    val textToSend = rawText.trim()
    val mission = activeMission
    if (!isVerifiedPro) {
      missionInputText = textToSend
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.ROLEPLAY)
    } else if (textToSend.isNotBlank() && mission != null && !isAiGenerating) {
      missionInputText = ""
      focusManager.clearFocus()
      conversationErrorMessage = null
      speechManager.stopSpeaking()
      missionMessages.add(
        ChatMessage(
          id = UUID.randomUUID().toString(),
          sender = MessageSender.USER,
          text = textToSend
        )
      )
      isAiGenerating = true
      coroutineScope.launch {
        try {
          val history = missionMessages.dropLast(1).map {
            (if (it.sender == MessageSender.USER) "USER" else "MODEL") to it.text
          }
          val tutorResponse = GeminiTutorService.chatWithTutor(
            userSpeechInput = textToSend,
            targetLanguage = targetLang,
            nativeLanguage = nativeLang,
            proficiencyLevel = userProfile.proficiencyLevel,
            scenarioTitle = mission.title,
            scenarioInstruction = mission.systemPromptInstruction,
            conversationHistory = history,
            partnerName = currentSelectedPartner.name,
            conversationMode = com.example.ai.TutorConversationMode.ROLEPLAY,
            hasSpeechRecognitionTelemetry = false
          )
          if (tutorResponse.isServiceError || tutorResponse.replyInTargetLanguage.isBlank()) {
            conversationErrorMessage = com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
              tutorResponse.errorMessage,
              isPro = isVerifiedPro
            )
            missionInputText = textToSend
          } else {
            missionMessages.add(
              ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AI_TUTOR,
                text = tutorResponse.replyInTargetLanguage,
                phoneticGuide = tutorResponse.phoneticGuide,
                translation = tutorResponse.translationInNativeLanguage,
                grammarFeedback = tutorResponse.grammarFeedback
              )
            )
            speechManager.speakWithTutorVoice(
              text = tutorResponse.replyInTargetLanguage,
              voice = selectedVoice,
              languageCode = targetLang.code
            )
          }
        } catch (e: Exception) {
          conversationErrorMessage = com.example.ai.AiErrorHandler.handle(e, isVerifiedPro).message
          missionInputText = textToSend
        } finally {
          isAiGenerating = false
        }
      }
    }
  }

  LaunchedEffect(spokenText) {
    val clean = spokenText.trim()
    if (clean.isNotBlank() && !clean.equals("SILENCE", ignoreCase = true) && !clean.equals("Listening...", ignoreCase = true)) {
      speechManager.clearSpokenText()
      when {
        activeSection == SpeakActiveSection.MISSIONS && activeMission != null -> {
          if (!isAiGenerating) {
            sendMissionMessage(clean)
          } else {
            missionInputText = if (missionInputText.isBlank()) clean else "$missionInputText $clean"
          }
        }
        activeSection == SpeakActiveSection.EXAM -> {
          val existing = examAnswersByIndex[currentExamQuestionIndex].orEmpty()
          examAnswersByIndex[currentExamQuestionIndex] = if (existing.isBlank()) clean else "$existing $clean"
          examVoiceUsedByIndex[currentExamQuestionIndex] = true
        }
        activeSection == SpeakActiveSection.INTERVIEW && isInterviewStarted -> {
          interviewAnswerText = if (interviewAnswerText.isBlank()) clean else "$interviewAnswerText $clean"
        }
        else -> {
          if (!isAiGenerating) {
            if (activeSection == SpeakActiveSection.HOME) {
              activeSection = SpeakActiveSection.CONVERSATION
            }
            sendConversationMessage(clean)
          } else {
            conversationInputText = if (conversationInputText.isBlank()) clean else "$conversationInputText $clean"
          }
        }
      }
    }
  }

  val filteredPartners = remember(partnerSearchQuery, selectedLevelFilter, selectedAccentFilter) {
    AiConversationPartner.ALL_PARTNERS.filter { partner ->
      val matchesSearch = partnerSearchQuery.isBlank() ||
        partner.name.contains(partnerSearchQuery, ignoreCase = true) ||
        partner.roleTitle.contains(partnerSearchQuery, ignoreCase = true) ||
        partner.shortDescription.contains(partnerSearchQuery, ignoreCase = true) ||
        partner.specialty.contains(partnerSearchQuery, ignoreCase = true)
      val matchesLevel = selectedLevelFilter == "All" ||
        partner.recommendedLevels.any { it.equals(selectedLevelFilter, ignoreCase = true) }
      val matchesAccent = selectedAccentFilter == "All" ||
        partner.accent.contains(selectedAccentFilter, ignoreCase = true)
      matchesSearch && matchesLevel && matchesAccent
    }
  }

  val startGroupSessionAction: () -> Unit = {
    val groupList = selectedGroupPartnerIds.map { AiConversationPartner.findById(it) }.ifEmpty {
      listOf(AiConversationPartner.ALL_PARTNERS[0], AiConversationPartner.ALL_PARTNERS[1])
    }
    onStartConfiguredVoiceConversation(
      groupList.first(),
      groupList,
      selectedTopicMode,
      groupList.first().voice,
      false,
      selectedCorrectionMode,
      selectedCefrBand,
      null
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .imePadding()
      .testTag("speak_screen")
  ) {
    if (activeSection != SpeakActiveSection.HOME) {
      SpeakSubScreenQuickBar(
        activeSection = activeSection,
        onSelectSection = { next ->
          activeMission = null
          activeSection = next
        },
        onOpenMoreSheet = { showMoreOptionsSheet = true }
      )
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (activeSection) {
        SpeakActiveSection.HOME -> {
          Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("speak_chat_list"),
              contentPadding = PaddingValues(horizontal = LingoTokens.screenMargin, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              // Header: Title + History + Language Pill
              item(key = "speak_home_header") {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("speak_home_screen"),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Speak",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LingoTokens.textPrimary
                  )

                  Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (savedAiConversations.isNotEmpty() || speakingSessions.isNotEmpty()) {
                      Surface(
                        onClick = { showHistorySheet = true },
                        color = LingoTokens.bgElevated,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                        modifier = Modifier
                          .size(44.dp)
                          .testTag("speak_history_btn")
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Session History",
                            tint = LingoTokens.textSecondary,
                            modifier = Modifier.size(20.dp)
                          )
                        }
                      }
                    }

                    Surface(
                      onClick = { showLanguagePicker = true },
                      color = LingoTokens.bgElevated,
                      shape = RoundedCornerShape(50),
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("speak_language_selector")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.Language,
                          contentDescription = null,
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(16.dp)
                        )
                        Text(
                          text = "${targetLang.name} · $selectedCefrBand",
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                }
              }

              // Clean 3-Segment Control (Practice, Exams, Interview) + More Sheet
              item(key = "speak_home_segmented_bar") {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("speak_home_segmented_bar"),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(16.dp))
                      .background(LingoTokens.bgElevated)
                      .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(16.dp))
                      .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    val homeSegments = listOf(
                      Triple("Practice", true, "speak_segment_practice") to { activeSection = SpeakActiveSection.HOME },
                      Triple("Exams", false, "speak_segment_exams") to {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.EXAM
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                      },
                      Triple("Interview", false, "speak_segment_interview") to {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.INTERVIEW
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                      }
                    )
                    homeSegments.forEach { (meta, onTap) ->
                      val (label, selected, tag) = meta
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(12.dp))
                          .then(
                            if (selected) Modifier
                              .background(LingoTokens.brandCyan.copy(alpha = 0.18f))
                              .border(1.dp, LingoTokens.brandCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            else Modifier
                          )
                          .clickable { onTap() }
                          .padding(vertical = 10.dp, horizontal = 4.dp)
                          .testTag(tag),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = label,
                          color = if (selected) LingoTokens.brandCyan else LingoTokens.textSecondary,
                          fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          softWrap = false,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }

                  Surface(
                    onClick = { showMoreOptionsSheet = true },
                    color = LingoTokens.bgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    modifier = Modifier
                      .heightIn(min = 44.dp)
                      .testTag("speak_more_sheet_btn")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "More speaking tools",
                        tint = LingoTokens.textPrimary,
                        modifier = Modifier.size(18.dp)
                      )
                      Text(
                        text = "More",
                        color = LingoTokens.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                }
              }

              // ===============================================================
              // AREA 1 — UNIFIED TUTOR HERO CARD (Glowing Orb + 1 Primary CTA)
              // ===============================================================
              item(key = "main_ai_tutor_card") {
                val isCurrentPartnerSpeaking = isSpeaking &&
                  (previewingVoiceId == null || previewingVoiceId == selectedVoice.id)

                LingoCleanCard(
                  contentPadding = 20.dp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("main_ai_tutor_card")
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .clickable { detailPartner = currentSelectedPartner }
                          .testTag("main_ai_tutor_avatar")
                      ) {
                        LingoTutorOrb(
                          isSpeaking = isCurrentPartnerSpeaking || isListening,
                          size = 64.dp
                        )
                      }

                      Spacer(modifier = Modifier.width(14.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                          Text(
                            text = currentSelectedPartner.name,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LingoTokens.textPrimary,
                            maxLines = 1,
                            softWrap = false
                          )
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                          ) {
                            Box(
                              modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(LingoTokens.statusOnline)
                            )
                            Text(
                              text = "Online",
                              fontSize = 12.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = LingoTokens.statusOnline
                            )
                          }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                          text = "Friendly ${targetLang.name} tutor · ${currentSelectedPartner.accent}",
                          color = LingoTokens.textSecondary,
                          fontSize = 14.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }

                    // Level, Voice & Preview Row
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Surface(
                        onClick = {
                          val nextIdx = (cefrBands.indexOf(selectedCefrBand) + 1) % cefrBands.size
                          selectedCefrBand = cefrBands[nextIdx]
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = LingoTokens.bgSubtle,
                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                        modifier = Modifier
                          .heightIn(min = 44.dp)
                          .testTag("speak_cefr_level_selector")
                      ) {
                        Box(
                          contentAlignment = Alignment.Center,
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                          Text(
                            text = formatCefrLevelLabel(selectedCefrBand),
                            color = LingoTokens.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                          )
                        }
                      }

                      Surface(
                        onClick = { showVoiceSelectionSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        color = LingoTokens.bgSubtle,
                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 44.dp)
                          .testTag("speak_voice_selector_btn")
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Center,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                          Icon(
                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = "Select Voice",
                            tint = LingoTokens.brandCyan,
                            modifier = Modifier.size(16.dp)
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            text = selectedVoice.displayName,
                            color = LingoTokens.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }

                      Surface(
                        onClick = {
                          speechManager.setTutorVoice(selectedVoice)
                          speechManager.previewVoice(
                            voice = selectedVoice,
                            languageCode = targetLang.code,
                            customSampleText = if (targetLang.code.equals("en", ignoreCase = true)) {
                              currentSelectedPartner.sampleGreeting
                            } else {
                              null
                            }
                          )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = LingoTokens.bgSubtle,
                        border = BorderStroke(
                          1.dp,
                          if (isCurrentPartnerSpeaking) LingoTokens.brandCyan else LingoTokens.borderSubtle
                        ),
                        modifier = Modifier
                          .heightIn(min = 44.dp)
                          .testTag("main_tutor_preview_voice_btn")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          if (isCurrentPartnerSpeaking) {
                            PartnerSpeakingWaveform(
                              isSpeaking = true,
                              activeColor = LingoTokens.brandCyan,
                              barCount = 4,
                              maxHeight = 12.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = "Playing",
                              color = LingoTokens.brandCyan,
                              fontSize = 12.sp,
                              fontWeight = FontWeight.Bold,
                              maxLines = 1,
                              softWrap = false
                            )
                          } else {
                            Icon(
                              imageVector = Icons.Outlined.PlayArrow,
                              contentDescription = "Play voice",
                              tint = LingoTokens.brandCyan,
                              modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                              text = "Play voice",
                              color = LingoTokens.textPrimary,
                              fontSize = 12.sp,
                              fontWeight = FontWeight.Bold,
                              maxLines = 1,
                              softWrap = false
                            )
                          }
                        }
                      }
                    }

                    // ONE Primary Brand Gradient Button on the Screen
                    LingoPrimaryGradientButton(
                      text = if (isVerifiedPro) "Start speaking" else "Unlock AI Tutor",
                      icon = Icons.Outlined.Mic,
                      onClick = {
                        if (!isVerifiedPro) {
                          com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                        } else {
                          onStartVoiceCall()
                          onStartConfiguredVoiceConversation(
                            currentSelectedPartner,
                            emptyList(),
                            selectedTopicMode,
                            selectedVoice,
                            false,
                            selectedCorrectionMode,
                            selectedCefrBand,
                            null
                          )
                        }
                      },
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_ai_voice_call_btn")
                    )

                    // Secondary Mode Chips: Text chat, Video call, Change tutor
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      SpeakTutorQuickControl(
                        label = "Text chat",
                        icon = Icons.Outlined.Keyboard,
                        iconTint = LingoTokens.textPrimary,
                        testTag = "start_conversation_btn",
                        modifier = Modifier.weight(1f),
                        onClick = {
                          if (isVerifiedPro) {
                            activeSection = SpeakActiveSection.CONVERSATION
                          } else {
                            com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                          }
                        }
                      )
                      SpeakTutorQuickControl(
                        label = "Video call",
                        icon = Icons.Outlined.Videocam,
                        iconTint = LingoTokens.textPrimary,
                        testTag = "start_ai_video_call_btn",
                        modifier = Modifier.weight(1f),
                        onClick = onStartVideoTutorCall
                      )
                      SpeakTutorQuickControl(
                        label = "Change tutor",
                        icon = Icons.Outlined.SwapHoriz,
                        iconTint = LingoTokens.textSecondary,
                        testTag = "change_tutor_btn",
                        modifier = Modifier.weight(1f),
                        onClick = { showPartnersDirectorySheet = true }
                      )
                    }
                  }
                }
              }

              // ===============================================================
              // AREA 2 — SPEAKING ACTIVITIES (Clean 2-Column Grid)
              // ===============================================================
              item(key = "speaking_activities_section") {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("speaking_activities_grid"),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Text(
                    text = "Activities",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    SpeakCompactActivityCard(
                      title = "AI Partners",
                      subtitle = "12 tutors & voices",
                      icon = Icons.Outlined.RecordVoiceOver,
                      accentColor = LingoTokens.brandCyan,
                      testTag = "speak_tab_tutor",
                      modifier = Modifier.weight(1f),
                      onClick = { showPartnersDirectorySheet = true }
                    )
                    SpeakCompactActivityCard(
                      title = "Missions",
                      subtitle = "Real-life scenarios",
                      icon = Icons.Outlined.Explore,
                      accentColor = LingoTokens.brandIndigo,
                      showProLock = !isVerifiedPro,
                      testTag = "speak_tab_roleplays",
                      modifier = Modifier.weight(1f),
                      onClick = {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.MISSIONS
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.ROLEPLAY)
                      }
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    SpeakCompactActivityCard(
                      title = "Exams",
                      subtitle = "CEFR A1–C2 check",
                      icon = Icons.Outlined.EmojiEvents,
                      accentColor = LingoTokens.brandCyan,
                      showProLock = !isVerifiedPro,
                      testTag = "speak_tab_assessment",
                      modifier = Modifier.weight(1f),
                      onClick = {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.EXAM
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                      }
                    )
                    SpeakCompactActivityCard(
                      title = "Interview Coach",
                      subtitle = "Mock job practice",
                      icon = Icons.Outlined.WorkOutline,
                      accentColor = LingoTokens.brandIndigo,
                      showProLock = !isVerifiedPro,
                      testTag = "speak_tab_interview",
                      modifier = Modifier.weight(1f),
                      onClick = {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.INTERVIEW
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                      }
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    SpeakCompactActivityCard(
                      title = "Phone Calls",
                      subtitle = "15 call topics",
                      icon = Icons.Outlined.Phone,
                      accentColor = LingoTokens.brandCyan,
                      showProLock = !isVerifiedPro,
                      testTag = "speak_tab_phone_calls",
                      modifier = Modifier.weight(1f),
                      onClick = {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.PHONE_MODES
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                      }
                    )
                    SpeakCompactActivityCard(
                      title = "Speaking Lab",
                      subtitle = "Fluency studios",
                      icon = Icons.Outlined.AutoAwesome,
                      accentColor = LingoTokens.brandIndigo,
                      showProLock = !isVerifiedPro,
                      testTag = "speak_tab_labs",
                      modifier = Modifier.weight(1f),
                      onClick = {
                        if (isVerifiedPro) activeSection = SpeakActiveSection.LABS
                        else com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                      }
                    )
                  }
                }
              }

              // ===============================================================
              // AREA 3 — GROUP CONVERSATION
              // ===============================================================
              item(key = "group_ai_conversation_card") {
                val selectedGroupPartners = remember(selectedGroupPartnerIds) {
                  selectedGroupPartnerIds.map { AiConversationPartner.findById(it) }
                }
                val groupNamesLabel = remember(selectedGroupPartners) {
                  selectedGroupPartners.joinToString(", ") { it.name }
                }

                LingoCleanCard(
                  onClick = { showGroupPartnersSheet = true },
                  contentPadding = 16.dp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("group_ai_conversation_card")
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = LingoTokens.bgSubtle,
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      modifier = Modifier.size(44.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Outlined.Groups,
                          contentDescription = null,
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(22.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Group Conversation",
                        color = LingoTokens.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = "With $groupNamesLabel",
                        color = LingoTokens.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                      onClick = startGroupSessionAction,
                      color = LingoTokens.bgSubtle,
                      shape = RoundedCornerShape(14.dp),
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("start_group_conversation_btn")
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                      ) {
                        Text(
                          text = "Start group",
                          color = LingoTokens.textPrimary,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                }
              }

              item(key = "speak_bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
              }
            }

            // Bottom Quick Message Input, Outline Mic & Send Bar
            HorizontalDivider(color = LingoTokens.borderSubtle, thickness = 1.dp)
            Surface(
              color = LingoTokens.bgCanvas,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = LingoTokens.screenMargin, vertical = 10.dp)
              ) {
                if (isListening || isAiGenerating) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isAiGenerating) LingoTokens.brandCyan else LingoTokens.statusOnline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = when {
                        isAiGenerating -> "${currentSelectedPartner.name} is thinking..."
                        partialSpokenText.isNotBlank() -> "Hearing: \"$partialSpokenText\""
                        else -> "Listening... tap mic when done"
                      },
                      color = LingoTokens.textSecondary,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    onClick = toggleMic,
                    shape = CircleShape,
                    color = if (isListening) LingoCoral.copy(alpha = 0.2f) else LingoTokens.bgElevated,
                    border = BorderStroke(
                      1.dp,
                      if (isListening) LingoCoral else LingoTokens.borderSubtle
                    ),
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("speak_mic_button")
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = if (isListening) Icons.Outlined.Stop else Icons.Outlined.Mic,
                        contentDescription = "Speak into Microphone",
                        tint = if (isListening) LingoCoral else LingoTokens.brandCyan,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  OutlinedTextField(
                    value = conversationInputText,
                    onValueChange = { conversationInputText = it },
                    placeholder = {
                      Text(
                        text = "Message ${currentSelectedPartner.name}...",
                        color = LingoTokens.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = LingoTokens.textPrimary,
                      unfocusedTextColor = LingoTokens.textPrimary,
                      focusedBorderColor = LingoTokens.brandCyan.copy(alpha = 0.6f),
                      unfocusedBorderColor = LingoTokens.borderSubtle,
                      focusedContainerColor = LingoTokens.bgElevated,
                      unfocusedContainerColor = LingoTokens.bgElevated
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .heightIn(min = 48.dp)
                      .testTag("speak_chat_input")
                  )

                  Spacer(modifier = Modifier.width(10.dp))

                  val hasText = conversationInputText.isNotBlank()
                  Surface(
                    onClick = {
                      if (conversationInputText.isNotBlank()) {
                        sendConversationMessage(conversationInputText)
                        activeSection = SpeakActiveSection.CONVERSATION
                      }
                    },
                    shape = CircleShape,
                    color = if (hasText) LingoTokens.brandCyan else LingoTokens.bgElevated,
                    border = BorderStroke(
                      1.dp,
                      if (hasText) LingoTokens.brandCyan else LingoTokens.borderSubtle
                    ),
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("speak_send_button")
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (hasText) LingoTokens.onGradientInk else LingoTokens.textSecondary,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        SpeakActiveSection.CONVERSATION -> {
          SpeakConversationSection(
            currentSelectedPartner = currentSelectedPartner,
            targetLang = targetLang,
            nativeLangCode = nativeLang.code,
            selectedVoice = selectedVoice,
            selectedCorrectionMode = selectedCorrectionMode,
            activeConversationMode = activeConversationMode,
            onSelectConversationMode = { activeConversationMode = it },
            autoCorrectionsEnabled = autoCorrectionsEnabled,
            onToggleAutoCorrections = { autoCorrectionsEnabled = it },
            conversationMessages = conversationMessages,
            conversationListState = conversationListState,
            isAiGenerating = isAiGenerating,
            conversationErrorMessage = conversationErrorMessage,
            onRetryLastMessage = {
              val retryText = lastFailedUserMessage
              if (!retryText.isNullOrBlank()) {
                dispatchConversationTurn(retryText, false)
              }
            },
            isListening = isListening,
            partialSpokenText = partialSpokenText,
            conversationInputText = conversationInputText,
            onConversationInputChange = { conversationInputText = it },
            onSendMessage = sendConversationMessage,
            onToggleMic = toggleMic,
            onOpenRescueSheet = { showRescueSheet = true },
            onOpenVoiceSheet = { showVoiceSelectionSheet = true },
            onRecordGrammarMistake = onRecordGrammarMistake,
            speechManager = speechManager,
            onBack = { activeSection = SpeakActiveSection.HOME }
          )
        }

        SpeakActiveSection.MISSIONS -> {
          SpeakMissionsSection(
            activeMission = activeMission,
            onSelectMission = { activeMission = it },
            missionSearch = missionSearch,
            onMissionSearchChange = { missionSearch = it },
            selectedMissionCategory = selectedMissionCategory,
            onSelectMissionCategory = { selectedMissionCategory = it },
            missionMessages = missionMessages,
            missionInputText = missionInputText,
            onMissionInputChange = { missionInputText = it },
            onSendMissionMessage = sendMissionMessage,
            missionEvaluationReport = missionEvaluationReport,
            onUpdateMissionEvaluationReport = { missionEvaluationReport = it },
            isEvaluatingMission = isEvaluatingMission,
            onSetEvaluatingMission = { isEvaluatingMission = it },
            isAiGenerating = isAiGenerating,
            isListening = isListening,
            onToggleMic = toggleMic,
            currentSelectedPartner = currentSelectedPartner,
            targetLang = targetLang,
            nativeLang = nativeLang,
            userProfile = userProfile,
            onSaveSession = onSaveSession,
            onBackHome = { activeSection = SpeakActiveSection.HOME }
          )
        }

        SpeakActiveSection.EXAM -> {
          val currentAnswer = examAnswersByIndex[currentExamQuestionIndex].orEmpty()
          SpeakExamSection(
            examPrompts = examPrompts,
            selectedExamLevel = selectedExamLevel,
            onSelectExamLevel = { newLevel ->
              if (selectedExamLevel != newLevel) {
                selectedExamLevel = newLevel
                currentExamQuestionIndex = 0
                examAnswersByIndex.clear()
                examVoiceUsedByIndex.clear()
                examSpeechSecsByIndex.clear()
                examResult = null
              }
            },
            currentExamQuestionIndex = currentExamQuestionIndex,
            onSelectExamQuestionIndex = { idx ->
              currentExamQuestionIndex = idx.coerceIn(0, (examPrompts.size - 1).coerceAtLeast(0))
            },
            onPrevExamQuestion = {
              if (currentExamQuestionIndex > 0) {
                currentExamQuestionIndex--
              }
            },
            onNextExamQuestion = {
              if (currentExamQuestionIndex < examPrompts.size - 1) {
                currentExamQuestionIndex++
              }
            },
            examAnswerText = currentAnswer,
            onExamAnswerChange = { updated ->
              examAnswersByIndex[currentExamQuestionIndex] = updated
              if (updated.isBlank()) {
                examVoiceUsedByIndex[currentExamQuestionIndex] = false
                examSpeechSecsByIndex[currentExamQuestionIndex] = 0
              }
            },
            examAnswersByIndex = examAnswersByIndex,
            examVoiceUsedByIndex = examVoiceUsedByIndex,
            examSpeechSecsByIndex = examSpeechSecsByIndex,
            onRecordSpeechDuration = { idx, secs ->
              examSpeechSecsByIndex[idx] = (examSpeechSecsByIndex[idx] ?: 0) + secs
              examVoiceUsedByIndex[idx] = true
            },
            examResult = examResult,
            onUpdateExamResult = { res ->
              examResult = res
              if (res != null && res.isScored) {
                val updatedRecent = (recentExamPromptIds + examPromptIds).distinct().takeLast(10)
                recentExamPromptIds = updatedRecent
                runCatching {
                  prefs.edit().putString("recent_exam_prompt_ids", updatedRecent.joinToString(",")).apply()
                }
              }
            },
            onRetakeExam = {
              val updatedRecent = (recentExamPromptIds + examPromptIds).distinct().takeLast(10)
              recentExamPromptIds = updatedRecent
              runCatching {
                prefs.edit().putString("recent_exam_prompt_ids", updatedRecent.joinToString(",")).apply()
              }
              examAnswersByIndex.clear()
              examVoiceUsedByIndex.clear()
              examSpeechSecsByIndex.clear()
              currentExamQuestionIndex = 0
              examResult = null
              examAttemptSeed++
            },
            onOpenLabById = { labId ->
              when (labId.lowercase()) {
                "immersion" -> onOpenImmersionMode()
                "camera" -> onOpenCameraLens()
                "replay" -> onOpenConversationReplay()
                "accent" -> onOpenAccentStudio()
                "emergency" -> onOpenLanguageEmergencyMode()
                "debate" -> onOpenAiDebatePartner()
                "explain" -> onOpenExplainItThreeWays()
                else -> onOpenImmersionMode()
              }
            },
            isEvaluatingExam = isEvaluatingExam,
            onSetEvaluatingExam = { isEvaluatingExam = it },
            isListening = isListening,
            onToggleMic = toggleMic,
            selectedVoice = selectedVoice,
            targetLang = targetLang,
            nativeLang = nativeLang,
            speechManager = speechManager,
            onSaveSession = onSaveSession,
            onBackHome = { activeSection = SpeakActiveSection.HOME }
          )
        }

        SpeakActiveSection.INTERVIEW -> {
          SpeakInterviewSection(
            interviewIndustry = interviewIndustry,
            onSelectIndustry = { interviewIndustry = it },
            interviewRole = interviewRole,
            interviewDifficulty = interviewDifficulty,
            onSelectDifficulty = { interviewDifficulty = it },
            isInterviewStarted = isInterviewStarted,
            onStartInterview = {
              isInterviewStarted = true
              interviewResultReport = null
              interviewAnswerText = ""
              interviewMessages.clear()
              val opener = when (interviewDifficulty) {
                "Friendly" -> "Hi! Great to meet you for this $interviewRole chat in $interviewIndustry. To warm up, could you share a bit about your background in ${targetLang.name}?"
                "Rigorous" -> "Let's begin your $interviewRole evaluation for $interviewIndustry. In ${targetLang.name}, walk me through the most impactful project you've led and its measurable results."
                else -> "Welcome to your interview for $interviewRole in $interviewIndustry. Could you tell me about yourself and your relevant experience in ${targetLang.name}?"
              }
              interviewMessages.add(
                ChatMessage(
                  id = UUID.randomUUID().toString(),
                  sender = MessageSender.AI_TUTOR,
                  text = opener
                )
              )
            },
            onResetInterview = {
              isInterviewStarted = false
              interviewResultReport = null
              interviewAnswerText = ""
              interviewMessages.clear()
            },
            speakingSessions = speakingSessions,
            interviewMessages = interviewMessages,
            interviewAnswerText = interviewAnswerText,
            onInterviewAnswerChange = { interviewAnswerText = it },
            interviewResultReport = interviewResultReport,
            onUpdateInterviewReport = { interviewResultReport = it },
            isEvaluatingInterview = isEvaluatingInterview,
            onSetEvaluatingInterview = { isEvaluatingInterview = it },
            isListening = isListening,
            onToggleMic = toggleMic,
            targetLang = targetLang,
            nativeLang = nativeLang,
            userProfile = userProfile,
            speechManager = speechManager,
            onSaveSession = onSaveSession,
            onBackHome = { activeSection = SpeakActiveSection.HOME }
          )
        }

        SpeakActiveSection.PHONE_MODES -> {
          SpeakPhoneModesSection(
            currentSelectedPartner = currentSelectedPartner,
            onSelectPhoneMode = { mode ->
              selectedTopicMode = mode
              onStartVoiceCall()
              onStartConfiguredVoiceConversation(
                currentSelectedPartner,
                emptyList(),
                mode,
                selectedVoice,
                false,
                selectedCorrectionMode,
                selectedCefrBand,
                null
              )
            },
            onBackHome = { activeSection = SpeakActiveSection.HOME }
          )
        }

        SpeakActiveSection.LABS -> {
          SpeakLabsSection(
            onOpenImmersionMode = onOpenImmersionMode,
            onOpenCameraLens = onOpenCameraLens,
            onOpenConversationReplay = onOpenConversationReplay,
            onOpenAccentStudio = onOpenAccentStudio,
            onOpenLanguageEmergencyMode = onOpenLanguageEmergencyMode,
            onOpenAiDebatePartner = onOpenAiDebatePartner,
            onOpenExplainItThreeWays = onOpenExplainItThreeWays,
            onBackHome = { activeSection = SpeakActiveSection.HOME }
          )
        }
      }
    }
  }

  // Sheets & Dialogs
  detailPartner?.let { partner ->
    SpeakTutorDetailSheet(
      partner = partner,
      targetLang = targetLang,
      selectedVoice = selectedVoice,
      previewingVoiceId = previewingVoiceId,
      selectedTopicMode = selectedTopicMode,
      onSelectTopicMode = { selectedTopicMode = it },
      selectedCorrectionMode = selectedCorrectionMode,
      onSelectCorrectionMode = { selectedCorrectionMode = it },
      speechManager = speechManager,
      onOpenVoicePicker = {
        detailPartner = null
        showVoiceSelectionSheet = true
      },
      onStartVoiceConversation = { chosen ->
        persistPartnerSelection(chosen, selectedVoice)
        detailPartner = null
        onStartVoiceCall()
        onStartConfiguredVoiceConversation(
          chosen,
          emptyList(),
          selectedTopicMode,
          selectedVoice,
          false,
          selectedCorrectionMode,
          selectedCefrBand,
          null
        )
      },
      onStartTextConversation = { chosen ->
        persistPartnerSelection(chosen, selectedVoice)
        detailPartner = null
        onStartConfiguredVoiceConversation(
          chosen,
          emptyList(),
          selectedTopicMode,
          selectedVoice,
          true,
          selectedCorrectionMode,
          selectedCefrBand,
          null
        )
      },
      onDismiss = { detailPartner = null }
    )
  }

  if (showRescueSheet) {
    val lastTutorText = conversationMessages.lastOrNull { it.sender == MessageSender.AI_TUTOR }?.text.orEmpty()
    SpeakConversationRescueSheet(
      currentSelectedPartner = currentSelectedPartner,
      targetLang = targetLang,
      lastTutorPrompt = lastTutorText,
      isGeneratingRescue = isGeneratingRescue,
      onSetGeneratingRescue = { isGeneratingRescue = it },
      rescueSheetContent = rescueSheetContent,
      onUpdateRescueContent = { rescueSheetContent = it },
      onUsePhrase = { phrase ->
        conversationInputText = phrase
        showRescueSheet = false
      },
      onDismiss = { showRescueSheet = false }
    )
  }

  if (showLanguagePicker) {
    SpeakLanguagePickerDialog(
      currentLanguageCode = currentLanguageCode,
      onSelectLanguageCode = { code ->
        currentLanguageCode = code
        onChangeTargetLanguage(code)
        conversationMessages.clear()
        showLanguagePicker = false
      },
      onDismiss = { showLanguagePicker = false }
    )
  }

  if (showPartnersDirectorySheet) {
    SpeakPartnersDirectoryDialog(
      filteredPartners = filteredPartners,
      currentSelectedPartner = currentSelectedPartner,
      selectedVoice = selectedVoice,
      partnerSearchQuery = partnerSearchQuery,
      onSearchQueryChange = { partnerSearchQuery = it },
      selectedLevelFilter = selectedLevelFilter,
      onSelectLevelFilter = { selectedLevelFilter = it },
      selectedAccentFilter = selectedAccentFilter,
      onSelectAccentFilter = { selectedAccentFilter = it },
      previewingVoiceId = previewingVoiceId,
      targetLang = targetLang,
      speechManager = speechManager,
      onSelectPartner = { partner ->
        persistPartnerSelection(partner, partner.voice)
        showPartnersDirectorySheet = false
      },
      onInspectPartner = { partner ->
        showPartnersDirectorySheet = false
        detailPartner = partner
      },
      onOpenVoicesDialog = {
        showPartnersDirectorySheet = false
        showVoiceSelectionSheet = true
      },
      onDismiss = { showPartnersDirectorySheet = false }
    )
  }

  if (showVoiceSelectionSheet) {
    SpeakVoicesSelectionDialog(
      selectedVoice = selectedVoice,
      previewingVoiceId = previewingVoiceId,
      targetLang = targetLang,
      speechManager = speechManager,
      onSelectVoice = { voice ->
        val updatedPartner = currentSelectedPartner.copy(voice = voice)
        persistPartnerSelection(updatedPartner, voice)
        showVoiceSelectionSheet = false
      },
      onDismiss = { showVoiceSelectionSheet = false }
    )
  }

  if (showGroupPartnersSheet) {
    SpeakGroupPartnersDialog(
      selectedGroupPartnerIds = selectedGroupPartnerIds,
      onToggleGroupPartner = { partnerId ->
        selectedGroupPartnerIds = if (partnerId in selectedGroupPartnerIds) {
          if (selectedGroupPartnerIds.size > 2) selectedGroupPartnerIds - partnerId else selectedGroupPartnerIds
        } else {
          if (selectedGroupPartnerIds.size >= 3) selectedGroupPartnerIds.drop(1) + partnerId else selectedGroupPartnerIds + partnerId
        }
      },
      onStartGroupCall = {
        showGroupPartnersSheet = false
        startGroupSessionAction()
      },
      onDismiss = { showGroupPartnersSheet = false }
    )
  }

  if (showHistorySheet) {
    SpeakSavedSessionsDialog(
      savedAiConversations = savedAiConversations,
      speakingSessions = speakingSessions,
      onResumeSavedRecord = { record ->
        showHistorySheet = false
        val partner = AiConversationPartner.findById(record.partnerId)
        val groupPartners = record.groupPartnerIds.map { AiConversationPartner.findById(it) }
        onStartConfiguredVoiceConversation(
          partner,
          groupPartners,
          CallMode.findById(record.topicId),
          partner.voice,
          false,
          CorrectionMode.findById(record.correctionModeId),
          record.cefrLevel,
          record
        )
      },
      onDismiss = { showHistorySheet = false }
    )
  }

  if (showMoreOptionsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMoreOptionsSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = LingoTokens.bgElevated,
      contentColor = LingoTokens.textPrimary
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = LingoTokens.screenMargin, vertical = 8.dp)
          .padding(bottom = 24.dp)
          .testTag("speak_more_bottom_sheet"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "More Speaking Tools",
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          color = LingoTokens.textPrimary
        )
        Text(
          text = "Jump directly into calls, roleplays, labs or history",
          fontSize = 13.sp,
          color = LingoTokens.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        LingoListRow(
          title = "Phone Calls",
          subtitle = "Simulated voice calls for travel & daily life",
          icon = Icons.Outlined.Phone,
          iconTint = LingoTokens.brandCyan,
          onClick = {
            showMoreOptionsSheet = false
            activeSection = SpeakActiveSection.PHONE_MODES
          }
        )
        LingoListRow(
          title = "Speaking Missions",
          subtitle = "Guided real-life roleplay goals & feedback",
          icon = Icons.Outlined.Explore,
          iconTint = LingoTokens.brandIndigo,
          onClick = {
            showMoreOptionsSheet = false
            activeSection = SpeakActiveSection.MISSIONS
          }
        )
        LingoListRow(
          title = "Speaking Labs",
          subtitle = "Immersion mode, camera lens & accent studio",
          icon = Icons.Outlined.AutoAwesome,
          iconTint = LingoTokens.accentEmerald,
          onClick = {
            showMoreOptionsSheet = false
            activeSection = SpeakActiveSection.LABS
          }
        )
        LingoListRow(
          title = "Group Conversation",
          subtitle = "Practice speaking with multiple AI partners",
          icon = Icons.Outlined.Groups,
          iconTint = LingoTokens.brandCyan,
          onClick = {
            showMoreOptionsSheet = false
            showGroupPartnersSheet = true
          }
        )
        LingoListRow(
          title = "AI Partners Directory",
          subtitle = "Browse all ${AiConversationPartner.ALL_PARTNERS.size} tutors & voices",
          icon = Icons.Outlined.RecordVoiceOver,
          iconTint = LingoTokens.brandIndigo,
          onClick = {
            showMoreOptionsSheet = false
            showPartnersDirectorySheet = true
          }
        )
        LingoListRow(
          title = "Session History",
          subtitle = "Resume past voice chats & speaking reviews",
          icon = Icons.Outlined.History,
          iconTint = LingoTokens.accentGold,
          onClick = {
            showMoreOptionsSheet = false
            showHistorySheet = true
          }
        )
      }
    }
  }
}

@Composable
private fun SpeakTutorQuickControl(
  label: String,
  icon: ImageVector,
  iconTint: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = LingoTokens.bgSubtle,
    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
    modifier = modifier
      .heightIn(min = 44.dp)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        color = LingoTokens.textPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SpeakCompactActivityCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  showProLock: Boolean = false,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  LingoCleanCard(
    onClick = onClick,
    contentPadding = 16.dp,
    modifier = modifier.testTag(testTag)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = LingoTokens.bgSubtle,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(20.dp)
            )
          }
        }
        if (showProLock) {
          com.example.ui.components.LingoGoldLockIcon(size = 16.dp)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          color = LingoTokens.textPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          color = LingoTokens.textSecondary,
          fontSize = 13.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
