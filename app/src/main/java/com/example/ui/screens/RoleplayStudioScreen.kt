package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.GeminiTutorService
import com.example.data.model.AiConversationPartner
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.ConversationFollowControlChip
import com.example.ui.components.EmojiVoice
import com.example.ui.components.EmojiVoiceState
import com.example.ui.components.SynchronizedAiSpeechText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavyBackground = Color(0xFF060D1D)
private val NavyCardBg = Color(0xFF0D162B)
private val NavyCardElevated = Color(0xFF13203D)
private val NavyBorder = Color(0xFF1D2E52)
private val ElectricBlue = Color(0xFF3B82F6)
private val CyanAccent = Color(0xFF38BDF8)
private val GoldAccent = Color(0xFFFBBF24)
private val EmeraldAccent = Color(0xFF10B981)
private val CoralAccent = Color(0xFFF43F5E)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val TextLight = Color(0xFFCBD5E1)

private enum class RoleplayViewMode {
  HOME,
  SCENARIO_LIST,
  SETUP,
  CHAT,
  FEEDBACK
}

enum class RoleplayConversationState(val displayLabel: String) {
  CONNECTING("Connecting"),
  CHLOE_SPEAKING("Speaking"),
  WAITING_FOR_USER("Waiting for User"),
  LISTENING("Listening"),
  PROCESSING_USER_SPEECH("Processing User Speech"),
  GENERATING_AI_RESPONSE("Generating AI Response"),
  ERROR("Error")
}

enum class RoleplayMessageOrigin {
  USER_TYPED,
  USER_VOICE,
  AI_GREETING,
  AI_REPLY
}

data class ChatMessage(
  val sender: String, // "AI" or "User"
  val text: String,
  val id: String = java.util.UUID.randomUUID().toString(),
  val origin: RoleplayMessageOrigin = if (sender == "User") RoleplayMessageOrigin.USER_TYPED else RoleplayMessageOrigin.AI_REPLY,
  val sessionId: String = ""
)

data class RoleplayScenarioItem(
  val id: String,
  val title: String,
  val category: String,
  val difficulty: String,
  val description: String,
  val goal: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayStudioScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  initialPartnerId: String = "partner_chloe",
  onAddXp: (Long) -> Unit = {},
  onOpenSubscription: () -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val targetLang = remember(userProfile.targetLanguageCode) { Language.findByCode(userProfile.targetLanguageCode) }
  val nativeLang = remember(userProfile.nativeLanguageCode) { Language.findByCode(userProfile.nativeLanguageCode) }

  var viewMode by remember { mutableStateOf(RoleplayViewMode.HOME) }
  var selectedLevelCategory by remember { mutableStateOf("Beginner") }
  var selectedFilterCategory by remember { mutableStateOf("All") }
  var selectedScenario by remember {
    mutableStateOf(
      RoleplayScenarioItem(
        id = "sc_1",
        title = "Ordering at a Restaurant",
        category = "Restaurant",
        difficulty = "Beginner (A1)",
        description = "You are visiting a local restaurant. Order food and ask questions about the menu.",
        goal = "Order a meal, ask about recommendations, and request the bill."
      )
    )
  }

  var selectedPartner by remember(initialPartnerId) {
    mutableStateOf(AiConversationPartner.findById(initialPartnerId))
  }
  var selectedVoice by remember(selectedPartner.id) {
    mutableStateOf(selectedPartner.defaultVoice)
  }

  var isMinimizedView by remember { mutableStateOf(false) }
  var speechSpeed by remember { mutableStateOf(1.0f) }
  var sessionTimerSeconds by remember { mutableIntStateOf(0) }
  var waitingSilenceSeconds by remember { mutableIntStateOf(0) }
  var silencePromptText by remember { mutableStateOf<String?>(null) }
  var activeSessionId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
  var conversationState by remember { mutableStateOf(RoleplayConversationState.WAITING_FOR_USER) }
  var activeTurnJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

  var chatMessages by remember(selectedScenario.id, targetLang.code) {
    val initialSession = activeSessionId
    val opening = buildRoleplayOpeningGreeting(selectedScenario, targetLang)
    mutableStateOf(
      listOf(
        ChatMessage(
          sender = "AI",
          text = opening,
          origin = RoleplayMessageOrigin.AI_GREETING,
          sessionId = initialSession
        )
      )
    )
  }
  var userInputText by remember { mutableStateOf("") }
  var isAiThinking by remember { mutableStateOf(false) }
  var aiServiceError by remember { mutableStateOf<String?>(null) }
  var lastFailedRoleplayInput by remember { mutableStateOf<String?>(null) }

  var showHelpDrawer by remember { mutableStateOf(false) }
  var customScenarioInput by remember { mutableStateOf("") }
  var showCreateCustomDialog by remember { mutableStateOf(false) }

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val isListening by speechManager.isListening.collectAsState()
  val isProcessingSpeech by speechManager.isProcessingSpeech.collectAsState()
  val partialSpokenText by speechManager.partialSpokenText.collectAsState()
  val isSpeaking by speechManager.isSpeaking.collectAsState()
  val rmsLevel by speechManager.rmsLevel.collectAsState()
  var isContinuousVoiceActive by remember { mutableStateOf(false) }
  var lastSubmittedTurnTimestamp by remember { mutableLongStateOf(0L) }

  // Roleplay Automatic Dialogue Following state
  val chatScrollState = rememberScrollState()
  var isFollowingConversation by remember { mutableStateOf(true) }
  var isPausedByManualScroll by remember { mutableStateOf(false) }
  var isProgrammaticScroll by remember { mutableStateOf(false) }

  LaunchedEffect(chatScrollState.isScrollInProgress) {
    if (chatScrollState.isScrollInProgress && !isProgrammaticScroll && isFollowingConversation) {
      isPausedByManualScroll = true
    }
  }

  LaunchedEffect(chatMessages.size, isAiThinking, isSpeaking, isFollowingConversation, isPausedByManualScroll) {
    if (chatMessages.isNotEmpty() && isFollowingConversation && !isPausedByManualScroll) {
      isProgrammaticScroll = true
      try {
        chatScrollState.animateScrollTo(chatScrollState.maxValue)
      } finally {
        isProgrammaticScroll = false
      }
    }
  }

  // Clear any stale speech transcript on entry so old speech from other screens never auto-sends
  LaunchedEffect(Unit) {
    speechManager.clearSessionSpeechState()
  }

  // Keep explicit RoleplayConversationState synchronized with live speech/thinking states
  LaunchedEffect(isSpeaking, isListening, isProcessingSpeech, isAiThinking) {
    conversationState = when {
      isSpeaking -> RoleplayConversationState.CHLOE_SPEAKING
      isProcessingSpeech -> RoleplayConversationState.PROCESSING_USER_SPEECH
      isAiThinking -> RoleplayConversationState.GENERATING_AI_RESPONSE
      isListening -> RoleplayConversationState.LISTENING
      conversationState == RoleplayConversationState.ERROR -> RoleplayConversationState.ERROR
      else -> RoleplayConversationState.WAITING_FOR_USER
    }
  }

  // Session clock + 30-second user silence monitor (never invents or auto-sends a user message)
  LaunchedEffect(viewMode, activeSessionId) {
    if (viewMode == RoleplayViewMode.CHAT) {
      sessionTimerSeconds = 0
      waitingSilenceSeconds = 0
      silencePromptText = null
      while (true) {
        kotlinx.coroutines.delay(1000L)
        sessionTimerSeconds++
        val isWaitingForLearner = !isAiThinking &&
          !speechManager.isSpeaking.value &&
          !speechManager.isListening.value &&
          !speechManager.isProcessingSpeech.value
        if (isWaitingForLearner) {
          waitingSilenceSeconds++
          if (waitingSilenceSeconds >= 30 && silencePromptText == null) {
            silencePromptText = "${selectedPartner.name}: \"Take your time! Tap the microphone or type whenever you're ready to reply.\""
          }
        } else {
          waitingSilenceSeconds = 0
        }
      }
    } else {
      waitingSilenceSeconds = 0
      silencePromptText = null
    }
  }

  val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasMicPermission = granted
    if (granted) {
      isContinuousVoiceActive = true
      if (viewMode == RoleplayViewMode.CHAT && !speechManager.isSpeaking.value && !isAiThinking) {
        speechManager.clearSpokenText()
        conversationState = RoleplayConversationState.LISTENING
        speechManager.startListening(targetLang.code)
      }
    } else {
      Toast.makeText(context, "Microphone permission is required for voice conversation. You can also type your replies below.", Toast.LENGTH_LONG).show()
    }
  }

  val currentChatMessages by rememberUpdatedState(chatMessages)
  val currentScenario by rememberUpdatedState(selectedScenario)
  val currentSpeechSpeed by rememberUpdatedState(speechSpeed)
  val currentViewMode by rememberUpdatedState(viewMode)
  val currentPartner by rememberUpdatedState(selectedPartner)
  val currentVoice by rememberUpdatedState(selectedVoice)
  val currentSessionId by rememberUpdatedState(activeSessionId)

  fun resetAndStartNewRoleplaySession() {
    activeTurnJob?.cancel()
    activeTurnJob = null
    speechManager.stopListening()
    speechManager.stopSpeaking()
    speechManager.clearSessionSpeechState()

    val newSessionId = java.util.UUID.randomUUID().toString()
    activeSessionId = newSessionId
    isAiThinking = false
    aiServiceError = null
    lastFailedRoleplayInput = null
    waitingSilenceSeconds = 0
    silencePromptText = null
    userInputText = ""
    showHelpDrawer = false
    conversationState = RoleplayConversationState.CONNECTING

    val openingGreeting = buildRoleplayOpeningGreeting(selectedScenario, targetLang)
    speechManager.registerAiUtteranceForEchoFilter(openingGreeting)
    chatMessages = listOf(
      ChatMessage(
        sender = "AI",
        text = openingGreeting,
        origin = RoleplayMessageOrigin.AI_GREETING,
        sessionId = newSessionId
      )
    )
    isMinimizedView = false
    isContinuousVoiceActive = false
    viewMode = RoleplayViewMode.CHAT
    conversationState = RoleplayConversationState.CHLOE_SPEAKING
    speechManager.speakWithTutorVoice(
      text = openingGreeting,
      languageCode = targetLang.code,
      voice = selectedVoice,
      speedOverride = speechSpeed,
      onDone = {
        if (currentSessionId == newSessionId && currentViewMode == RoleplayViewMode.CHAT) {
          conversationState = RoleplayConversationState.WAITING_FOR_USER
        }
      }
    )
  }

  fun processTextRoleplayTurn(userMsg: String, appendUserTurn: Boolean = true) {
    val trimmed = userMsg.trim()
    val turnSessionId = currentSessionId
    if (trimmed.isBlank() || isAiThinking) return

    // Reject if text matches AI's own recent utterances (echo guard)
    val recentAiTexts = currentChatMessages.filter { it.sender == "AI" }.map { it.text }
    if (GeminiTutorService.isLikelyEchoOfAiUtterance(trimmed, recentAiTexts)) {
      return
    }

    if (appendUserTurn) {
      val now = System.currentTimeMillis()
      val lastUserMsg = currentChatMessages.lastOrNull { it.sender == "User" }
      if (now - lastSubmittedTurnTimestamp < 1200L && lastUserMsg?.text.equals(trimmed, ignoreCase = true)) {
        return
      }
      lastSubmittedTurnTimestamp = now
    }
    waitingSilenceSeconds = 0
    silencePromptText = null
    aiServiceError = null

    if (speechManager.isSpeaking.value) {
      speechManager.stopSpeaking()
    }
    if (speechManager.isListening.value) {
      speechManager.stopListening()
    }

    if (appendUserTurn) {
      val verifiedUserMessage = ChatMessage(
        sender = "User",
        text = trimmed,
        origin = RoleplayMessageOrigin.USER_TYPED,
        sessionId = turnSessionId
      )
      chatMessages = currentChatMessages + verifiedUserMessage
    }
    isAiThinking = true
    conversationState = RoleplayConversationState.GENERATING_AI_RESPONSE
    speechManager.setProcessingSpeech(false)

    activeTurnJob?.cancel()
    activeTurnJob = scope.launch {
      val history = chatMessages
        .filter { it.sessionId == turnSessionId }
        .map { Pair(it.sender, it.text) }
      try {
        val tutorRes = GeminiTutorService.chatWithTutor(
          userSpeechInput = trimmed,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = userProfile.proficiencyLevel.take(2),
          scenarioTitle = "${currentScenario.title} (Roleplay with ${currentPartner.name} - ${currentPartner.roleTitle})",
          scenarioInstruction = "${currentScenario.goal}. Stay in character as ${currentPartner.name} (${currentPartner.personality}). Respond directly to what the learner said. Never invent user messages.",
          conversationHistory = history,
          partnerName = currentPartner.name,
          conversationMode = com.example.ai.TutorConversationMode.ROLEPLAY,
          hasSpeechRecognitionTelemetry = false
        )
        if (currentSessionId != turnSessionId || currentViewMode != RoleplayViewMode.CHAT) {
          isAiThinking = false
          return@launch
        }
        val replyText = tutorRes.replyInTargetLanguage.trim()
        if (tutorRes.isServiceError || replyText.isBlank()) {
          lastFailedRoleplayInput = trimmed
          aiServiceError = tutorRes.errorMessage
            ?: "Roleplay partner is temporarily unavailable. Check your connection and tap Retry."
          isAiThinking = false
          conversationState = RoleplayConversationState.ERROR
          return@launch
        }
        lastFailedRoleplayInput = null
        speechManager.registerAiUtteranceForEchoFilter(replyText)
        chatMessages = chatMessages + ChatMessage(
          sender = "AI",
          text = replyText,
          origin = RoleplayMessageOrigin.AI_REPLY,
          sessionId = turnSessionId
        )
        isAiThinking = false
        conversationState = RoleplayConversationState.CHLOE_SPEAKING
        speechManager.speakWithTutorVoice(
          text = replyText,
          languageCode = targetLang.code,
          voice = currentVoice,
          speedOverride = currentSpeechSpeed,
          onDone = {
            if (currentSessionId == turnSessionId && currentViewMode == RoleplayViewMode.CHAT) {
              conversationState = RoleplayConversationState.WAITING_FOR_USER
            }
          }
        )
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        if (currentSessionId != turnSessionId || currentViewMode != RoleplayViewMode.CHAT) {
          isAiThinking = false
          return@launch
        }
        lastFailedRoleplayInput = trimmed
        aiServiceError = "Unable to reach your roleplay partner right now. Check your internet connection and tap Retry."
        isAiThinking = false
        conversationState = RoleplayConversationState.ERROR
      }
    }
  }

  DisposableEffect(viewMode, activeSessionId, selectedScenario.id, targetLang.code, selectedPartner.id) {
    val boundSessionId = activeSessionId
    if (viewMode == RoleplayViewMode.CHAT) {
      speechManager.clearSpokenText()
      speechManager.setAudioTurnCallback { wavBytes, hintText ->
        // Guard 1: Reject stale session callbacks or turns while AI is speaking/thinking
        if (
          currentSessionId != boundSessionId ||
          currentViewMode != RoleplayViewMode.CHAT ||
          isAiThinking ||
          speechManager.isSpeaking.value
        ) {
          speechManager.setProcessingSpeech(false)
          return@setAudioTurnCallback
        }

        val cleanHint = hintText.trim().takeIf {
          it.length >= 2 &&
            !it.equals("Listening...", ignoreCase = true) &&
            !it.equals("SILENCE", ignoreCase = true) &&
            !it.equals("NONE", ignoreCase = true)
        }.orEmpty()

        val hasValidAudio = GeminiTutorService.hasMeaningfulSpeechPcm(wavBytes)
        if (!hasValidAudio && cleanHint.isBlank()) {
          speechManager.setProcessingSpeech(false)
          conversationState = RoleplayConversationState.WAITING_FOR_USER
          return@setAudioTurnCallback
        }

        val recentAiTexts = currentChatMessages.filter { it.sender == "AI" }.map { it.text }
        if (cleanHint.isNotBlank() && GeminiTutorService.isLikelyEchoOfAiUtterance(cleanHint, recentAiTexts)) {
          speechManager.setProcessingSpeech(false)
          conversationState = RoleplayConversationState.WAITING_FOR_USER
          return@setAudioTurnCallback
        }

        isAiThinking = true
        conversationState = RoleplayConversationState.PROCESSING_USER_SPEECH
        speechManager.setProcessingSpeech(true)

        activeTurnJob?.cancel()
        activeTurnJob = scope.launch {
          try {
            val history = currentChatMessages
              .filter { it.sessionId == boundSessionId }
              .map { Pair(it.sender, it.text) }
            val audioTurnResult = GeminiTutorService.chatWithTutorAudio(
              wavBytes = wavBytes,
              fallbackTextIfAny = cleanHint,
              targetLanguage = targetLang,
              nativeLanguage = nativeLang,
              proficiencyLevel = userProfile.proficiencyLevel.take(2),
              scenarioTitle = "${currentScenario.title} (Roleplay with ${currentPartner.name})",
              scenarioInstruction = currentScenario.goal,
              conversationHistory = history,
              partnerName = currentPartner.name
            )
            speechManager.setProcessingSpeech(false)

            if (currentSessionId != boundSessionId || currentViewMode != RoleplayViewMode.CHAT) {
              isAiThinking = false
              return@launch
            }

            val userSpoken = audioTurnResult?.userTranscript?.trim().orEmpty()
            val isEcho = GeminiTutorService.isLikelyEchoOfAiUtterance(userSpoken, recentAiTexts) ||
              speechManager.isEchoOfAiSpeech(userSpoken)

            // Strict guard: never create a user turn unless genuine non-echo user words were transcribed
            if (audioTurnResult != null && userSpoken.length >= 2 && !isEcho) {
              val aiReply = audioTurnResult.tutorResponse.replyInTargetLanguage.trim()
              waitingSilenceSeconds = 0
              silencePromptText = null
              lastSubmittedTurnTimestamp = System.currentTimeMillis()
              chatMessages = currentChatMessages +
                ChatMessage(
                  sender = "User",
                  text = userSpoken,
                  origin = RoleplayMessageOrigin.USER_VOICE,
                  sessionId = boundSessionId
                )
              if (audioTurnResult.tutorResponse.isServiceError || aiReply.isBlank()) {
                lastFailedRoleplayInput = userSpoken
                aiServiceError = audioTurnResult.tutorResponse.errorMessage
                  ?: "Roleplay partner is temporarily unavailable. Tap Retry to continue."
                isAiThinking = false
                conversationState = RoleplayConversationState.ERROR
              } else {
                lastFailedRoleplayInput = null
                aiServiceError = null
                speechManager.registerAiUtteranceForEchoFilter(aiReply)
                chatMessages = chatMessages +
                  ChatMessage(
                    sender = "AI",
                    text = aiReply,
                    origin = RoleplayMessageOrigin.AI_REPLY,
                    sessionId = boundSessionId
                  )
                isAiThinking = false
                conversationState = RoleplayConversationState.CHLOE_SPEAKING
                speechManager.speakWithTutorVoice(
                  text = aiReply,
                  languageCode = targetLang.code,
                  voice = currentVoice,
                  speedOverride = currentSpeechSpeed,
                  onDone = {
                    if (currentSessionId == boundSessionId && currentViewMode == RoleplayViewMode.CHAT) {
                      conversationState = RoleplayConversationState.WAITING_FOR_USER
                    }
                  }
                )
              }
            } else {
              // Audio contained only silence, ambient noise, or speaker echo: remain WAITING_FOR_USER
              isAiThinking = false
              conversationState = RoleplayConversationState.WAITING_FOR_USER
            }
          } catch (e: kotlinx.coroutines.CancellationException) {
            speechManager.setProcessingSpeech(false)
            isAiThinking = false
            throw e
          } catch (e: Exception) {
            // Never append a fake follow-up message if audio transcription failed
            speechManager.setProcessingSpeech(false)
            isAiThinking = false
            aiServiceError = "Unable to process voice turn right now. Tap the microphone or type below."
            conversationState = RoleplayConversationState.ERROR
          }
        }
      }

      speechManager.setCallSpeechCallbacks(
        onPartial = null,
        onResult = null, // Handled exclusively by setAudioTurnCallback to prevent duplicate user turns
        onSpeechStarted = {
          waitingSilenceSeconds = 0
          silencePromptText = null
        },
        onIdleTimeout = {
          // Wait patiently for the learner to tap the mic or type — never auto-send on silence
          speechManager.setProcessingSpeech(false)
          if (!isSpeaking && !isAiThinking) {
            conversationState = RoleplayConversationState.WAITING_FOR_USER
          }
        }
      )
    }

    onDispose {
      if (viewMode == RoleplayViewMode.CHAT) {
        speechManager.setAudioTurnCallback(null)
        speechManager.setCallSpeechCallbacks(null, null, null, null)
      }
    }
  }

  val allScenarios = listOf(
    RoleplayScenarioItem("sc_1", "Ordering at a Restaurant", "Restaurant", "Beginner (A1)", "Order food and ask questions about the daily specials.", "Order a meal and request the bill."),
    RoleplayScenarioItem("sc_2", "Ordering Drinks & Coffee", "Restaurant", "Beginner (A1)", "Order beverages at a bustling coffee shop.", "Specify size, milk type, and payment."),
    RoleplayScenarioItem("sc_3", "Checking into a Hotel", "Hotel", "Beginner (A1)", "Arrive at reception and confirm your room reservation.", "Provide passport and ask about breakfast time."),
    RoleplayScenarioItem("sc_4", "Buying Groceries at a Supermarket", "Shopping", "Beginner (A1)", "Ask an assistant where to find fresh produce and pay.", "Inquire about prices and fruit ripeness."),
    RoleplayScenarioItem("sc_5", "Asking for Directions in the City", "Travel", "Beginner (A1)", "Stop a passerby to ask for the nearest metro station.", "Understand right, left, and street names."),
    RoleplayScenarioItem("sc_6", "Complaining Politely about an Order", "Restaurant", "Intermediate (B1)", "Explain to the waiter that your meal is cold or incorrect.", "Request a replacement politely."),
    RoleplayScenarioItem("sc_7", "Job Interview Introduction", "Business", "Intermediate (B1)", "Introduce yourself and talk about your past experience.", "Highlight professional strengths."),
    RoleplayScenarioItem("sc_8", "Visiting the Doctor's Office", "Healthcare", "Intermediate (B1)", "Describe symptoms and understand the doctor's advice.", "Explain where it hurts and ask about medication.")
  )

  BackHandler {
    when (viewMode) {
      RoleplayViewMode.HOME -> onBack()
      RoleplayViewMode.SCENARIO_LIST -> viewMode = RoleplayViewMode.HOME
      RoleplayViewMode.SETUP -> viewMode = RoleplayViewMode.SCENARIO_LIST
      RoleplayViewMode.CHAT -> viewMode = RoleplayViewMode.SETUP
      RoleplayViewMode.FEEDBACK -> viewMode = RoleplayViewMode.HOME
    }
  }

  if (showCreateCustomDialog) {
    AlertDialog(
      onDismissRequest = { showCreateCustomDialog = false },
      containerColor = NavyCardElevated,
      title = { Text("Create Your Own Scenario", color = TextWhite, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Describe the situation you want to practice in ${targetLang.name}:", color = TextLight, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = customScenarioInput,
            onValueChange = { customScenarioInput = it },
            placeholder = { Text("e.g. Talking to a museum guide", color = TextMuted) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextWhite,
              unfocusedTextColor = TextWhite,
              focusedBorderColor = CyanAccent,
              unfocusedBorderColor = Color(0xFF334155)
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customScenarioInput.isNotBlank()) {
              selectedScenario = RoleplayScenarioItem(
                id = "custom_${System.currentTimeMillis()}",
                title = customScenarioInput.trim(),
                category = "Custom",
                difficulty = "Intermediate (B1)",
                description = "Custom practice session: $customScenarioInput",
                goal = "Successfully complete the conversation."
              )
              showCreateCustomDialog = false
              customScenarioInput = ""
              viewMode = RoleplayViewMode.SETUP
            } else {
              Toast.makeText(context, "Please enter a scenario description", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
          Text("Start Practice", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateCustomDialog = false }) {
          Text("Cancel", color = TextMuted)
        }
      }
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyBackground)
  ) {
    when (viewMode) {
      RoleplayViewMode.HOME -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 36.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.08f))
                  .testTag("roleplay_back_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text("AI Roleplay", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (!userProfile.isEntitlementVerified) {
                Surface(
                  onClick = onOpenSubscription,
                  shape = RoundedCornerShape(20.dp),
                  color = Color(0xFF1E293B),
                  border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                  modifier = Modifier.padding(end = 8.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upgrade", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(targetLang.flagEmoji, fontSize = 14.sp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(targetLang.name, color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text("Practice real conversations in any language.", color = TextMuted, fontSize = 14.sp)

          Spacer(modifier = Modifier.height(20.dp))

          // Language Level Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Card(
              onClick = {
                selectedLevelCategory = "Beginner"
                viewMode = RoleplayViewMode.SCENARIO_LIST
              },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 110.dp),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, NavyBorder)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(shape = RoundedCornerShape(8.dp), color = EmeraldAccent.copy(alpha = 0.16f)) {
                    Text("A1-A2", color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                  }
                  Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                Column {
                  Text("Beginner", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  Text("${allScenarios.count { it.difficulty.contains("A1") }} Scenarios", color = TextMuted, fontSize = 11.sp)
                }
              }
            }

            Card(
              onClick = {
                selectedLevelCategory = "Intermediate"
                viewMode = RoleplayViewMode.SCENARIO_LIST
              },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 110.dp),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, NavyBorder)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(shape = RoundedCornerShape(8.dp), color = PurpleAccent.copy(alpha = 0.16f)) {
                    Text("B1-B2", color = PurpleAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                  }
                  Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                Column {
                  Text("Intermediate", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  Text("${allScenarios.count { it.difficulty.contains("B1") }} Scenarios", color = TextMuted, fontSize = 11.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Quick Action Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val randomItem = allScenarios.random()
                selectedScenario = randomItem
                viewMode = RoleplayViewMode.SETUP
              },
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = NavyCardElevated),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            ) {
              Icon(Icons.Default.Shuffle, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Random", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = { showCreateCustomDialog = true },
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = NavyCardElevated),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Create Custom", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Scenario Collections
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Scenario Categories",
              color = TextWhite,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
              selectedLevelCategory = "All"
              viewMode = RoleplayViewMode.SCENARIO_LIST
            }) {
              Text("See All", color = CyanAccent, fontSize = 13.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val categories = listOf(
            Triple("Restaurant", Icons.Default.Restaurant, Color(0xFF3B82F6)),
            Triple("Travel", Icons.Default.Flight, Color(0xFF10B981)),
            Triple("Shopping", Icons.Default.ShoppingBag, Color(0xFFF59E0B)),
            Triple("Hotel", Icons.Default.Hotel, Color(0xFF8B5CF6)),
            Triple("Business", Icons.Default.Work, Color(0xFFEC4899)),
            Triple("Healthcare", Icons.Default.LocalHospital, Color(0xFF06B6D4))
          )

          val rowScrollState = rememberScrollState()
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rowScrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            categories.forEach { (catName, catIcon, accentCol) ->
              Card(
                onClick = {
                  selectedFilterCategory = catName
                  selectedLevelCategory = "All"
                  viewMode = RoleplayViewMode.SCENARIO_LIST
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardBg),
                modifier = Modifier.width(130.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(accentCol.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(catIcon, contentDescription = null, tint = accentCol, modifier = Modifier.size(22.dp))
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(catName, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("${allScenarios.count { it.category == catName }.coerceAtLeast(2)} tasks", color = TextMuted, fontSize = 10.sp)
                }
              }
            }
          }
        }
      }

      RoleplayViewMode.SCENARIO_LIST -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { viewMode = RoleplayViewMode.HOME }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(selectedLevelCategory, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
          }

          val filters = listOf("All", "Restaurant", "Shopping", "Hotel", "Travel", "Business", "Healthcare")
          val filterScroll = rememberScrollState()
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(filterScroll)
              .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            filters.forEach { filt ->
              val isSelected = selectedFilterCategory == filt
              Surface(
                onClick = { selectedFilterCategory = filt },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) ElectricBlue else NavyCardBg,
                border = BorderStroke(1.dp, if (isSelected) CyanAccent else Color(0xFF1E293B))
              ) {
                Text(
                  text = filt,
                  color = if (isSelected) TextWhite else TextMuted,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 13.sp,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          val displayedScenarios = allScenarios.filter { item ->
            val matchesLevel = when (selectedLevelCategory) {
              "Beginner" -> item.difficulty.contains("A1")
              "Intermediate" -> item.difficulty.contains("B1")
              else -> true
            }
            val matchesFilter = selectedFilterCategory == "All" || item.category == selectedFilterCategory
            matchesLevel && matchesFilter
          }

          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp)
              .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            if (displayedScenarios.isEmpty()) {
              Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text("No scenarios found for this filter.", color = TextMuted, fontSize = 14.sp)
              }
            } else {
              displayedScenarios.forEach { sc ->
                Card(
                  onClick = {
                    selectedScenario = sc
                    viewMode = RoleplayViewMode.SETUP
                  },
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = NavyCardBg),
                  border = BorderStroke(1.dp, Color(0xFF1E293B)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlue.copy(alpha = 0.2f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sc.title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E293B)) {
                          Text(sc.difficulty.take(2), color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(sc.description, color = TextMuted, fontSize = 12.sp, maxLines = 2, lineHeight = 17.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      RoleplayViewMode.SETUP -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 36.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { viewMode = RoleplayViewMode.SCENARIO_LIST },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Preparation", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(16.dp))

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Surface(shape = RoundedCornerShape(8.dp), color = PurpleAccent.copy(alpha = 0.2f)) {
                Text(selectedScenario.category, color = PurpleAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(selectedScenario.title, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))
              Text(selectedScenario.description, color = TextLight, fontSize = 14.sp, lineHeight = 20.sp)

              Spacer(modifier = Modifier.height(16.dp))
              HorizontalDivider(color = NavyBorder)
              Spacer(modifier = Modifier.height(16.dp))

              Text("🎯 Goal:", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(selectedScenario.goal, color = TextWhite, fontSize = 14.sp)
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Select AI Roleplay Character & Voice
          Text(
            text = "Choose AI Roleplay Partner (${AiConversationPartner.ALL_PARTNERS.size} Characters)",
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Selected: ${selectedPartner.name} • ${selectedPartner.roleTitle} • Voice: ${selectedVoice.voiceName} (${selectedVoice.styleTag})",
            color = CyanAccent,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            AiConversationPartner.ALL_PARTNERS.forEach { partner ->
              val isSelected = selectedPartner.id == partner.id
              Card(
                onClick = {
                  selectedPartner = partner
                  selectedVoice = partner.defaultVoice
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.28f) else NavyCardBg
                ),
                border = BorderStroke(1.2.dp, if (isSelected) CyanAccent else Color(0xFF1E293B)),
                modifier = Modifier
                  .width(138.dp)
                  .testTag("roleplay_partner_${partner.id}")
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  com.example.ui.components.Expressive2dPartnerAvatar(
                    partner = partner,
                    size = 50.dp,
                    isSpeaking = false,
                    borderColor = if (isSelected) CyanAccent else Color(0xFF334155),
                    borderWidth = 1.5.dp
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "${partner.accentFlag} ${partner.name}",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = partner.roleTitle,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Surface(
                    onClick = {
                      speechManager.previewVoice(partner.defaultVoice, targetLang.code)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Preview Voice", tint = CyanAccent, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = partner.defaultVoice.voiceName,
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(22.dp))

          Button(
            onClick = {
              val permGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
              ) == PackageManager.PERMISSION_GRANTED
              hasMicPermission = permGranted
              resetAndStartNewRoleplaySession()
              if (!permGranted) {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("start_roleplay_session_btn")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Roleplay with ${selectedPartner.name}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      }

      RoleplayViewMode.CHAT -> {
        if (!isMinimizedView) {
          // =========================================================================
          // FULL-SCREEN IMMERSIVE AI TUTOR VIEW
          // =========================================================================
          BoxWithConstraints(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(NavyBackground, Color(0xFF0F1E42), Color(0xFF030712))
                )
              )
              .windowInsetsPadding(WindowInsets.statusBars)
              .windowInsetsPadding(WindowInsets.navigationBars)
          ) {
            val isCompactHeight = maxHeight < 680.dp
            val outerRingSize = if (isCompactHeight) 180.dp else 220.dp
            val avatarSize = if (isCompactHeight) 140.dp else 170.dp

            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = {
                    activeTurnJob?.cancel()
                    activeTurnJob = null
                    isContinuousVoiceActive = false
                    speechManager.clearSessionSpeechState()
                    speechManager.stopSpeaking()
                    activeSessionId = java.util.UUID.randomUUID().toString()
                    viewMode = RoleplayViewMode.SETUP
                  },
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .testTag("close_roleplay_session_btn")
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                }

                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = Color(0xFF1E293B)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val mins = sessionTimerSeconds / 60
                    val secs = sessionTimerSeconds % 60
                    Text(
                      text = String.format("%02d:%02d", mins, secs),
                      color = TextWhite,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                Surface(
                  onClick = {
                    speechSpeed = when (speechSpeed) {
                      0.75f -> 1.0f
                      1.0f -> 1.25f
                      else -> 0.75f
                    }
                  },
                  shape = RoundedCornerShape(20.dp),
                  color = Color(0xFF1E293B)
                ) {
                  Text(
                    text = "${speechSpeed}x",
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                  )
                }
              }

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.size(outerRingSize)
                ) {
                  Box(
                    modifier = Modifier
                      .size(outerRingSize - 16.dp)
                      .clip(CircleShape)
                      .background(
                        Brush.radialGradient(
                          colors = listOf(CyanAccent.copy(alpha = 0.3f), Color.Transparent)
                        )
                      )
                  )

                  com.example.ui.components.Expressive2dPartnerAvatar(
                    partner = selectedPartner,
                    size = avatarSize,
                    isSpeaking = isSpeaking,
                    showWaveformBadge = isSpeaking,
                    borderColor = if (isSpeaking) EmeraldAccent else CyanAccent,
                    borderWidth = 3.dp
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                  text = "${selectedPartner.name} • ${selectedPartner.roleTitle}",
                  color = TextWhite,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${selectedScenario.title} • Voice: ${selectedVoice.voiceName}",
                  color = CyanAccent,
                  fontSize = 13.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                val roleplayEmojiState = when (conversationState) {
                  RoleplayConversationState.CONNECTING -> EmojiVoiceState.PROCESSING
                  RoleplayConversationState.CHLOE_SPEAKING -> EmojiVoiceState.REPLYING
                  RoleplayConversationState.WAITING_FOR_USER -> EmojiVoiceState.IDLE
                  RoleplayConversationState.LISTENING -> if (rmsLevel > 0.04f) EmojiVoiceState.SPEAKING else EmojiVoiceState.LISTENING
                  RoleplayConversationState.PROCESSING_USER_SPEECH -> EmojiVoiceState.PROCESSING
                  RoleplayConversationState.GENERATING_AI_RESPONSE -> EmojiVoiceState.THINKING
                  RoleplayConversationState.ERROR -> EmojiVoiceState.ERROR
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  EmojiVoice(
                    state = roleplayEmojiState,
                    size = 34.dp,
                    audioLevel = rmsLevel,
                    isRecording = isListening,
                    isPlaying = isSpeaking,
                    showStateLabel = false
                  )
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("roleplay_conversation_state_badge")
                  ) {
                    val stateLabel = when (conversationState) {
                      RoleplayConversationState.CONNECTING -> "Connecting"
                      RoleplayConversationState.CHLOE_SPEAKING -> "${selectedPartner.name} Speaking"
                      RoleplayConversationState.WAITING_FOR_USER -> "Waiting for User"
                      RoleplayConversationState.LISTENING -> "Listening"
                      RoleplayConversationState.PROCESSING_USER_SPEECH -> "Processing User Speech"
                      RoleplayConversationState.GENERATING_AI_RESPONSE -> "Generating AI Response"
                      RoleplayConversationState.ERROR -> "Error — Tap Mic or Type to Retry"
                    }
                    Text(
                      text = stateLabel,
                      color = CyanAccent,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }

                val latestMessage = chatMessages.lastOrNull()
                if (latestMessage != null) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NavyCardBg.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("roleplay_latest_message_card")
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = if (latestMessage.sender == "User") "You" else selectedPartner.name,
                          color = if (latestMessage.sender == "User") EmeraldAccent else CyanAccent,
                          fontSize = 11.5.sp,
                          fontWeight = FontWeight.Bold
                        )
                        if (latestMessage.sender != "User") {
                          IconButton(
                            onClick = {
                              speechManager.speakWithTutorVoice(
                                text = latestMessage.text,
                                languageCode = targetLang.code,
                                voice = selectedVoice,
                                speedOverride = speechSpeed
                              )
                            },
                            modifier = Modifier.size(22.dp)
                          ) {
                            Icon(
                              Icons.AutoMirrored.Filled.VolumeUp,
                              contentDescription = "Replay AI response",
                              tint = CyanAccent,
                              modifier = Modifier.size(14.dp)
                            )
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      if (latestMessage.sender != "User") {
                        SynchronizedAiSpeechText(
                          text = latestMessage.text,
                          speechManager = speechManager,
                          baseTextColor = TextLight,
                          highlightColor = CyanAccent,
                          fontSizeSp = 14
                        )
                      } else {
                        Text(
                          text = latestMessage.text,
                          color = TextLight,
                          fontSize = 13.sp,
                          maxLines = 3,
                          overflow = TextOverflow.Ellipsis,
                          textAlign = TextAlign.Center
                        )
                      }
                    }
                  }
                }

                if (!aiServiceError.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoralAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("roleplay_ai_error_banner")
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = aiServiceError.orEmpty(),
                        color = TextWhite,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                      )
                      val retryText = lastFailedRoleplayInput
                      if (!retryText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                          onClick = { processTextRoleplayTurn(retryText, appendUserTurn = false) },
                          shape = RoundedCornerShape(10.dp),
                          color = CyanAccent,
                          modifier = Modifier.testTag("roleplay_ai_retry_button")
                        ) {
                          Text(
                            text = "Retry",
                            color = NavyBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                          )
                        }
                      }
                    }
                  }
                }

                if (!silencePromptText.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("roleplay_silence_prompt_banner")
                  ) {
                    Text(
                      text = silencePromptText!!,
                      color = GoldAccent,
                      fontSize = 12.sp,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                  }
                }
              }

              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  IconButton(
                    onClick = { isMinimizedView = true },
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF1E293B))
                      .testTag("roleplay_toggle_chat_mode_btn")
                  ) {
                    Icon(Icons.Default.UnfoldLess, contentDescription = "Switch to Chat & Type View", tint = TextLight)
                  }

                  IconButton(
                    onClick = { showHelpDrawer = !showHelpDrawer },
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF1E293B))
                      .testTag("roleplay_inspire_help_btn")
                  ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Need Help", tint = GoldAccent)
                  }

                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .weight(1f)
                      .padding(horizontal = 4.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      if (isListening) {
                        val pulseSize = 72.dp + (rmsLevel.coerceIn(0f, 1f) * 22f).dp
                        Box(
                          modifier = Modifier
                            .size(pulseSize)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.25f))
                        )
                      }
                      IconButton(
                        onClick = {
                          if (isListening) {
                            speechManager.finishListeningNow()
                          } else {
                            if (isSpeaking) {
                              speechManager.stopSpeaking()
                            }
                            isContinuousVoiceActive = true
                            waitingSilenceSeconds = 0
                            silencePromptText = null
                            if (hasMicPermission) {
                              speechManager.clearSpokenText()
                              conversationState = RoleplayConversationState.LISTENING
                              speechManager.startListening(targetLang.code)
                            } else {
                              micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                          }
                        },
                        modifier = Modifier
                          .size(72.dp)
                          .clip(CircleShape)
                          .background(
                            Brush.linearGradient(
                              when {
                                isListening -> listOf(EmeraldAccent, CyanAccent)
                                isProcessingSpeech || isAiThinking -> listOf(PurpleAccent, ElectricBlue)
                                else -> listOf(ElectricBlue, CyanAccent)
                              }
                            )
                          )
                          .testTag("full_screen_mic_btn")
                      ) {
                        if (isProcessingSpeech || isAiThinking) {
                          CircularProgressIndicator(
                            color = TextWhite,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                          )
                        } else {
                          Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = if (isListening) "Listening - Tap to Send Turn" else "Speak Now",
                            tint = TextWhite,
                            modifier = Modifier.size(34.dp)
                          )
                        }
                      }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = when {
                        isProcessingSpeech -> "Processing your speech..."
                        isAiThinking -> "${selectedPartner.name} is generating response..."
                        isListening && partialSpokenText.isNotBlank() && partialSpokenText != "Listening..." ->
                          "Hearing: \"$partialSpokenText\""
                        isListening -> "Listening to your voice..."
                        isSpeaking -> "${selectedPartner.name} is speaking..."
                        else -> "Waiting for you — tap mic or type"
                      },
                      color = when {
                        isProcessingSpeech || isAiThinking -> GoldAccent
                        isListening -> EmeraldAccent
                        isSpeaking -> CyanAccent
                        else -> TextMuted
                      },
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  IconButton(
                    onClick = {
                      activeTurnJob?.cancel()
                      activeTurnJob = null
                      isContinuousVoiceActive = false
                      speechManager.clearSessionSpeechState()
                      speechManager.stopSpeaking()
                      activeSessionId = java.util.UUID.randomUUID().toString()
                      onAddXp(35L)
                      viewMode = RoleplayViewMode.FEEDBACK
                    },
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(CoralAccent.copy(alpha = 0.2f))
                      .testTag("end_roleplay_session_btn")
                  ) {
                    Icon(Icons.Default.Stop, contentDescription = "End Session", tint = CoralAccent)
                  }
                }

                if (showHelpDrawer) {
                  val suggestions = remember(selectedScenario.id, targetLang.code) {
                    buildSuggestedRoleplayPhrases(selectedScenario, targetLang)
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("roleplay_inspire_drawer_card"),
                    colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Text(
                        text = "💡 Inspire Suggestions (Tap to copy to input — never auto-sent)",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                      Spacer(modifier = Modifier.height(6.dp))
                      suggestions.forEachIndexed { idx, phrase ->
                        Surface(
                          onClick = {
                            userInputText = phrase
                            isMinimizedView = true
                          },
                          shape = RoundedCornerShape(10.dp),
                          color = Color(0xFF1E293B),
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("inspire_suggestion_chip_$idx")
                        ) {
                          Text(
                            text = "• \"$phrase\"",
                            color = TextLight,
                            fontSize = 12.5.sp,
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
        } else {
          // =========================================================================
          // MINIMIZED TUTOR & CONVERSATION VIEW (Responsive for all screen widths)
          // =========================================================================
          Column(
            modifier = Modifier
              .fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
              .windowInsetsPadding(WindowInsets.navigationBars)
              .imePadding()
          ) {
            Surface(
              color = NavyCardElevated,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  com.example.ui.components.Expressive2dPartnerAvatar(
                    partner = selectedPartner,
                    size = 40.dp,
                    isSpeaking = isSpeaking,
                    borderColor = CyanAccent,
                    borderWidth = 1.5.dp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = selectedScenario.title,
                      color = TextWhite,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${selectedPartner.name} • ${selectedVoice.voiceName} Voice",
                      color = CyanAccent,
                      fontSize = 11.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Button(
                    onClick = { isMinimizedView = false },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                      .heightIn(min = 36.dp)
                      .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                      .testTag("roleplay_expand_voice_mode_btn")
                  ) {
                    Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expand", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }

                  Button(
                    onClick = {
                      activeTurnJob?.cancel()
                      activeTurnJob = null
                      isContinuousVoiceActive = false
                      speechManager.clearSessionSpeechState()
                      speechManager.stopSpeaking()
                      activeSessionId = java.util.UUID.randomUUID().toString()
                      onAddXp(35L)
                      viewMode = RoleplayViewMode.FEEDBACK
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                      .heightIn(min = 36.dp)
                      .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                      .testTag("end_roleplay_minimized_btn")
                  ) {
                    Text("End", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }
                }
              }
            }

            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(chatScrollState)
                .padding(16.dp)
                .testTag("roleplay_chat_messages_list"),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // Scenario Goal Card + Follow Conversation Toggle
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = ElectricBlue.copy(alpha = 0.2f),
                      modifier = Modifier.size(36.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                      }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = selectedScenario.title,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = "Goal: ${selectedScenario.goal}",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  ConversationFollowControlChip(
                    isFollowing = isFollowingConversation,
                    isPausedByManualScroll = isPausedByManualScroll,
                    onToggleFollow = { newVal ->
                      isPausedByManualScroll = false
                      isFollowingConversation = newVal
                    }
                  )
                }
              }

              chatMessages.forEachIndexed { index, msg ->
                val isUser = msg.sender == "User"
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("roleplay_msg_row_$index"),
                  horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                  Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUser) ElectricBlue else NavyCardBg),
                    modifier = Modifier.fillMaxWidth(0.84f)
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isUser) "You" else selectedPartner.name, color = if (isUser) TextWhite else CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (!isUser) {
                          IconButton(
                            onClick = {
                              try {
                                speechManager.speakWithTutorVoice(
                                  text = msg.text,
                                  languageCode = targetLang.code,
                                  voice = selectedVoice,
                                  speedOverride = speechSpeed
                                )
                              } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(48.dp)
                          ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = CyanAccent, modifier = Modifier.size(18.dp))
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      if (isUser) {
                        Text(msg.text, color = TextWhite, fontSize = 14.sp, lineHeight = 20.sp)
                      } else {
                        SynchronizedAiSpeechText(
                          text = msg.text,
                          speechManager = speechManager,
                          baseTextColor = TextWhite,
                          highlightColor = CyanAccent,
                          fontSizeSp = 14
                        )
                      }
                    }
                  }
                }
              }

              if (isAiThinking) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                  Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCardBg)
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("${selectedPartner.name} is thinking...", color = TextMuted, fontSize = 13.sp)
                    }
                  }
                }
              }

              if (!aiServiceError.isNullOrBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = CoralAccent.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.45f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("roleplay_ai_error_banner")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = aiServiceError.orEmpty(),
                      color = TextWhite,
                      fontSize = 12.sp,
                      modifier = Modifier.weight(1f)
                    )
                    val retryText = lastFailedRoleplayInput
                    if (!retryText.isNullOrBlank()) {
                      Spacer(modifier = Modifier.width(8.dp))
                      Surface(
                        onClick = { processTextRoleplayTurn(retryText, appendUserTurn = false) },
                        shape = RoundedCornerShape(10.dp),
                        color = CyanAccent,
                        modifier = Modifier.testTag("roleplay_ai_retry_button")
                      ) {
                        Text(
                          text = "Retry",
                          color = NavyBackground,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                      }
                    }
                  }
                }
              }
            }

            if (showHelpDrawer) {
              val suggestions = remember(selectedScenario.id, targetLang.code) {
                buildSuggestedRoleplayPhrases(selectedScenario, targetLang)
              }
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 4.dp)
                  .testTag("roleplay_minimized_inspire_drawer"),
                colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("💡 Helpful Phrase (Tap to fill input)", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showHelpDrawer = false }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  suggestions.forEachIndexed { idx, phrase ->
                    Surface(
                      onClick = { userInputText = phrase },
                      shape = RoundedCornerShape(8.dp),
                      color = Color(0xFF1E293B),
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .testTag("minimized_inspire_chip_$idx")
                    ) {
                      Text(
                        text = "• \"$phrase\"",
                        color = TextLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                      )
                    }
                  }
                }
              }
            }

            Surface(
              color = NavyCardElevated,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { showHelpDrawer = !showHelpDrawer },
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("minimized_inspire_btn")
                ) {
                  Icon(Icons.Default.Lightbulb, contentDescription = "Help", tint = GoldAccent, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                  value = userInputText,
                  onValueChange = { userInputText = it },
                  placeholder = {
                    Text(
                      text = "Type in ${targetLang.name}...",
                      color = TextMuted,
                      fontSize = 13.5.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(22.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor = CyanAccent
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("roleplay_text_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                  onClick = {
                    if (isListening) {
                      speechManager.finishListeningNow()
                    } else {
                      if (isSpeaking) {
                        speechManager.stopSpeaking()
                      }
                      isContinuousVoiceActive = true
                      waitingSilenceSeconds = 0
                      silencePromptText = null
                      if (hasMicPermission) {
                        speechManager.clearSpokenText()
                        speechManager.startListening(targetLang.code)
                      } else {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                      }
                    }
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) EmeraldAccent else Color(0xFF1E293B))
                    .testTag("minimized_mic_btn")
                ) {
                  Icon(
                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = if (isListening) "Listening" else "Speak",
                    tint = if (isListening) TextWhite else CyanAccent,
                    modifier = Modifier.size(20.dp)
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                  onClick = {
                    if (userInputText.isBlank()) return@IconButton
                    val userMsg = userInputText.trim()
                    userInputText = ""
                    processTextRoleplayTurn(userMsg)
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue)
                    .testTag("send_roleplay_msg_btn")
                ) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = TextWhite, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }
      }

      RoleplayViewMode.FEEDBACK -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 36.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(modifier = Modifier.height(20.dp))
          Box(
            modifier = Modifier
              .size(90.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(GoldAccent, Color(0xFFD97706)))),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = TextWhite, modifier = Modifier.size(50.dp))
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text("Practice Complete!", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          Text("You successfully completed ${selectedScenario.title}.", color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

          Spacer(modifier = Modifier.height(24.dp))

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text("What You Did Well", color = CyanAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(8.dp))
              Text("• Excellent vocabulary selection and sentence structure.\n• Natural conversation flow and appropriate register.", color = TextLight, fontSize = 13.sp, lineHeight = 20.sp)

              Spacer(modifier = Modifier.height(14.dp))
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldAccent.copy(alpha = 0.2f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("+35 XP Earned", color = EmeraldAccent, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(30.dp))

          Button(
            onClick = { viewMode = RoleplayViewMode.HOME },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
          ) {
            Text("Choose Another Scenario", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      }
    }
  }
}

private fun buildRoleplayOpeningGreeting(
  scenario: RoleplayScenarioItem,
  targetLang: Language
): String {
  val titleLower = scenario.title.lowercase()
  val isRestaurant = titleLower.contains("restaurant") || titleLower.contains("order") || scenario.category.equals("Restaurant", ignoreCase = true)
  val isHotel = titleLower.contains("hotel") || scenario.category.equals("Hotel", ignoreCase = true)
  val isShopping = titleLower.contains("grocer") || titleLower.contains("shop") || scenario.category.equals("Shopping", ignoreCase = true)
  val isTravel = titleLower.contains("direction") || scenario.category.equals("Travel", ignoreCase = true)
  val isDoctor = titleLower.contains("doctor") || scenario.category.equals("Healthcare", ignoreCase = true)
  val isJob = titleLower.contains("interview") || scenario.category.equals("Business", ignoreCase = true)

  return when (targetLang.code.lowercase()) {
    "es" -> when {
      isRestaurant -> "¡Hola! Bienvenido a nuestro restaurante. ¿Qué te gustaría pedir hoy?"
      isHotel -> "¡Hola! Bienvenido al hotel. ¿Tienes una reservación a tu nombre?"
      isShopping -> "¡Hola! Bienvenido al mercado. ¿Qué productos estás buscando hoy?"
      isTravel -> "¡Hola! Claro que puedo ayudarte. ¿A qué lugar de la ciudad quieres llegar?"
      isDoctor -> "Hola, pasa por favor. Cuéntame, ¿cómo te sientes hoy?"
      isJob -> "Hola, mucho gusto. Para comenzar la entrevista, ¿puedes contarme un poco sobre ti?"
      else -> "¡Hola! Comencemos con ${scenario.title}. ¿Qué te gustaría decir primero?"
    }
    "fr" -> when {
      isRestaurant -> "Bonjour ! Bienvenue dans notre restaurant. Que souhaitez-vous commander ?"
      isHotel -> "Bonjour ! Bienvenue à l'hôtel. Avez-vous une réservation ?"
      isShopping -> "Bonjour ! Que cherchez-vous aujourd'hui au supermarché ?"
      isTravel -> "Bonjour ! Je peux vous aider à trouver votre chemin. Où allez-vous ?"
      isDoctor -> "Bonjour, asseyez-vous je vous en prie. Comment vous sentez-vous aujourd'hui ?"
      isJob -> "Bonjour, enchanté ! Pour commencer l'entretien, parlez-moi un peu de votre parcours."
      else -> "Bonjour ! Commençons la conversation pour ${scenario.title}. Je vous écoute !"
    }
    "de" -> when {
      isRestaurant -> "Hallo! Willkommen in unserem Restaurant. Was möchten Sie bestellen?"
      isHotel -> "Hallo! Willkommen im Hotel. Haben Sie eine Reservierung?"
      isShopping -> "Hallo! Was suchen Sie heute?"
      isTravel -> "Hallo! Gerne helfe ich Ihnen weiter. Wohin möchten Sie fahren?"
      else -> "Hallo! Lassen Sie uns mit ${scenario.title} beginnen. Wie kann ich Ihnen helfen?"
    }
    "it" -> when {
      isRestaurant -> "Ciao! Benvenuto nel nostro ristorante. Cosa desidera ordinare?"
      isHotel -> "Benvenuto in hotel! Ha una prenotazione a suo nome?"
      else -> "Ciao! Iniziamo lo scenario ${scenario.title}. Come posso aiutarti?"
    }
    "pt" -> when {
      isRestaurant -> "Olá! Bem-vindo ao nosso restaurante. O que gostaria de pedir?"
      isHotel -> "Olá! Bem-vindo ao hotel. Você tem uma reserva?"
      else -> "Olá! Vamos começar ${scenario.title}. Como posso ajudar?"
    }
    "ja" -> when {
      isRestaurant -> "いらっしゃいませ！レストランへようこそ。ご注文はお決まりですか？"
      isHotel -> "ようこそホテルへ！ご予約はお済みですか？"
      else -> "こんにちは！「${scenario.title}」の練習を始めましょう。"
    }
    "zh" -> when {
      isRestaurant -> "您好，欢迎光临！请问今天想点什么菜？"
      isHotel -> "您好，欢迎入住酒店！请问您有预订吗？"
      else -> "你好！我们开始“${scenario.title}”的对话练习吧。"
    }
    else -> when {
      isRestaurant -> "Hello! Welcome to our restaurant. What would you like to order?"
      isHotel -> "Hello! Welcome to our hotel. Do you have a reservation for check-in today?"
      isShopping -> "Hello! Welcome to the supermarket. What items are you looking for today?"
      isTravel -> "Hello! I'd be happy to help with directions. Where are you trying to go?"
      isDoctor -> "Hello, please have a seat. How are you feeling today, and what symptoms do you have?"
      isJob -> "Hello, it's great to meet you! To start our interview, could you tell me a little about your background?"
      else -> "Hello! Welcome to ${scenario.title}. How can I help you today?"
    }
  }
}

private fun buildSuggestedRoleplayPhrases(
  scenario: RoleplayScenarioItem,
  targetLang: Language
): List<String> {
  val titleLower = scenario.title.lowercase()
  val isRestaurant = titleLower.contains("restaurant") || titleLower.contains("order") || scenario.category.equals("Restaurant", ignoreCase = true)
  val isHotel = titleLower.contains("hotel") || scenario.category.equals("Hotel", ignoreCase = true)
  val isTravel = titleLower.contains("direction") || scenario.category.equals("Travel", ignoreCase = true)
  val isShopping = titleLower.contains("grocer") || titleLower.contains("shop") || scenario.category.equals("Shopping", ignoreCase = true)
  return when (targetLang.code.lowercase()) {
    "es" -> when {
      isRestaurant -> listOf(
        "Me gustaría pedir pollo con arroz, por favor.",
        "¿Qué plato me recomienda hoy?",
        "¿Nos trae la cuenta, por favor?"
      )
      isHotel -> listOf("Tengo una reservación a mi nombre.", "¿A qué hora sirven el desayuno?")
      isTravel -> listOf("Disculpe, ¿cómo llego a la estación central?", "¿Está muy lejos caminando?")
      isShopping -> listOf("¿Cuánto cuesta esto, por favor?", "¿Tiene una talla más grande?")
      else -> listOf("Hola, ¿me podría ayudar por favor?", "Muchas gracias por la información.")
    }
    "fr" -> when {
      isRestaurant -> listOf(
        "Je voudrais commander le plat du jour, s'il vous plaît.",
        "Puis-je avoir une carafe d'eau ?",
        "L'addition, s'il vous plaît."
      )
      isHotel -> listOf("J'ai une réservation à mon nom.", "À quelle heure est le petit-déjeuner ?")
      else -> listOf("Bonjour, pouvez-vous m'aider s'il vous plaît ?", "Merci beaucoup pour votre aide.")
    }
    "de" -> when {
      isRestaurant -> listOf("Ich möchte gerne das Tagesgericht bestellen.", "Die Rechnung, bitte.")
      isHotel -> listOf("Ich habe eine Reservierung auf meinen Namen.")
      else -> listOf("Hallo, können Sie mir bitte helfen?")
    }
    else -> when {
      isRestaurant -> listOf(
        "I would like some water, please.",
        "Could you tell me today's specials?",
        "I'd like to order the grilled salmon with salad."
      )
      isHotel -> listOf(
        "I have a room reservation under my name.",
        "What time is breakfast served in the morning?"
      )
      isTravel -> listOf(
        "Excuse me, how do I get to the central station?",
        "How long does it take to walk there?"
      )
      isShopping -> listOf(
        "Excuse me, how much does this cost?",
        "Do you have this in a medium size?"
      )
      else -> listOf(
        "Hello! Could you help me with this, please?",
        "Thank you! Could you explain that one more time?"
      )
    }
  }
}


