package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.AutoScrollLearningService
import com.example.ai.AutoScrollPageSource
import com.example.ai.GeminiTutorService
import com.example.data.model.CurriculumLesson
import com.example.data.model.Language
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.TutorVoice
import com.example.speech.SpeechManager
import com.example.ui.components.AutoScrollTeleprompterView
import com.example.ui.components.ConversationFollowControlChip
import com.example.ui.components.EmojiVoice
import com.example.ui.components.EmojiVoiceState
import com.example.ui.components.SynchronizedAiSpeechText
import com.example.ui.components.resolveEmojiVoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Premium Dark Design System Palette (#0B0F1A / #121826 / #182033)
private val RoyalBlueDark = Color(0xFF0B0F1A)
private val RoyalNavyCard = Color(0xFF121826)
private val RoyalNavyElevated = Color(0xFF182033)
private val RoyalNavyBorder = Color(0x14FFFFFF)
private val RoyalPrimary = Color(0xFF6C7BFF)
private val RoyalPrimaryBright = Color(0xFF6C7BFF)
private val RoyalAccentCyan = Color(0xFF2DD4FF)
private val RoyalAccentEmerald = Color(0xFF22C55E)
private val RoyalAccentAmber = Color(0xFFF5C451)
private val RoyalAccentCoral = Color(0xFFEF4444)
private val RoyalTextPrimary = Color(0xFFFFFFFF)
private val RoyalTextSecondary = Color(0xFFA8B3CF)
private val RoyalLearnerBubble = Color(0xFF182033)

data class RefConversationTurn(
  val sender: String, // "Emma" or "Learner"
  val text: String,
  val phonetic: String? = null,
  val translation: String? = null,
  val correctionTip: String? = null
)

/**
 * Complete conversation state machine for the Learn Page AI Tutor Voice Conversation.
 * Surfaces clear Connected, Listening, Speaking, and Processing states based on real system activity.
 */
enum class EmmaVoiceSessionState(val label: String, val badgeText: String) {
  IDLE("Connected • Tap mic to speak", "Connected"),
  TUTOR_SPEAKING("Emma is speaking...", "Speaking"),
  USER_LISTENING("Listening • Speak now", "Listening"),
  USER_SPEAKING("Speaking • Capturing voice...", "Speaking"),
  PROCESSING("Processing your speech...", "Processing"),
  THINKING("Processing • Emma is thinking...", "Processing"),
  DONE("Connected • Great response!", "Connected"),
  PAUSED("Microphone Paused • Tap to resume", "Paused"),
  ERROR("Voice issue • Tap Retry", "Error")
}

enum class EmmaLessonMode(val title: String, val icon: String) {
  LEARN("Learn Mode", "📘"),
  PRACTICE("Practice Mode", "🎯")
}

@Composable
fun LessonExerciseScreen(
  lesson: CurriculumLesson,
  speechManager: SpeechManager?,
  onFinishLesson: (scorePercent: Int, xpEarned: Int) -> Unit,
  onNavigateBack: () -> Unit,
  initialExerciseIndex: Int = 0,
  initialCorrectCount: Int = 0,
  onSaveCheckpoint: (currentExerciseIndex: Int, correctCount: Int, totalExercises: Int) -> Unit = { _, _, _ -> },
  isReplay: Boolean = false,
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit = { _, _, _ -> },
  onRecordMistake: (MistakeNotebookEntry) -> Unit = {}
) {
  val targetLang = Language.findByCode(lesson.targetLanguageCode)
  val nativeLang = Language.findByCode("en")
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  val listState = rememberLazyListState()

  var currentMode by remember { mutableStateOf(EmmaLessonMode.LEARN) }
  var isTypingMode by remember { mutableStateOf(false) }
  var secondsElapsed by remember { mutableIntStateOf(0) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

  // Auto-Read controls whether Emma automatically speaks her replies aloud
  var autoReadEnabled by remember { mutableStateOf(true) }

  // Live Voice Session state machine variables
  var voiceSessionActive by remember { mutableStateOf(true) }
  var isVoiceMuted by remember { mutableStateOf(false) }
  var voiceState by remember { mutableStateOf(EmmaVoiceSessionState.IDLE) }
  var liveVoicePartial by remember { mutableStateOf("") }
  var silenceDebounceJob by remember { mutableStateOf<Job?>(null) }
  var autoRelistenJob by remember { mutableStateOf<Job?>(null) }
  var turnDoneResetJob by remember { mutableStateOf<Job?>(null) }
  var showTurnDoneBadge by remember { mutableStateOf(false) }
  var consecutiveIdleTimeouts by remember { mutableIntStateOf(0) }
  var localErrorMessage by remember { mutableStateOf<String?>(null) }
  var lastFailedLessonPrompt by remember { mutableStateOf<String?>(null) }
  var lastFailedLessonFromVoice by remember { mutableStateOf(false) }
  var lastSubmittedSpeech by remember { mutableStateOf("") }
  var lastSubmittedAtMs by remember { mutableLongStateOf(0L) }
  var lastSpokenEmmaText by remember { mutableStateOf("") }
  var lastSpokenEmmaAtMs by remember { mutableLongStateOf(0L) }

  // Dynamic Smart Hint state
  var currentHintText by remember { mutableStateOf<String?>(null) }
  var isGeneratingHint by remember { mutableStateOf(false) }
  var showHintSheet by remember { mutableStateOf(false) }

  // Auto-Scroll Story/Dialogue Teleprompter & Conversation Follow state
  var showAutoScrollLessonReader by remember { mutableStateOf(false) }
  var isFollowingConversation by remember { mutableStateOf(true) }
  var isPausedByManualScroll by remember { mutableStateOf(false) }
  var isProgrammaticScroll by remember { mutableStateOf(false) }

  // Extract real lesson target phrases so Emma teaches actual A1-C2 lesson content
  val lessonKeyPhrases = remember(lesson.id) {
    lesson.exercises
      .mapNotNull { ex ->
        ex.targetAudioText.takeIf { it.isNotBlank() }
          ?: ex.acceptableAnswers.firstOrNull { it.isNotBlank() }
      }
      .distinct()
      .take(4)
  }

  val firstTargetPhrase = lessonKeyPhrases.firstOrNull()

  // Live conversation timer
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      secondsElapsed++
    }
  }

  val minutes = secondsElapsed / 60
  val seconds = secondsElapsed % 60
  val timerString = String.format("%02d:%02d", minutes, seconds)

  val openingLearnGreeting = remember(lesson.id, targetLang.code, firstTargetPhrase) {
    val phraseNote = if (!firstTargetPhrase.isNullOrBlank()) " \"$firstTargetPhrase\"" else ""
    when (targetLang.code.lowercase().substringBefore("-")) {
      "es" -> "¡Hola! Soy Emma, tu profesora de español. Hoy practicamos '${lesson.title}' (${lesson.cefrLevel}).${if (phraseNote.isNotEmpty()) " Empecemos con:$phraseNote." else ""} ¿Cómo estás hoy?"
      "fr" -> "Bonjour ! Je suis Emma, votre professeure de français. Aujourd'hui, nous travaillons '${lesson.title}' (${lesson.cefrLevel}).${if (phraseNote.isNotEmpty()) " Commençons par :$phraseNote." else ""} Comment allez-vous ?"
      "de" -> "Hallo! Ich bin Emma, deine Deutschlehrerin. Heute lernen wir '${lesson.title}' (${lesson.cefrLevel}).${if (phraseNote.isNotEmpty()) " Unser erster Ausdruck ist:$phraseNote." else ""} Wie geht es dir heute?"
      "it" -> "Ciao! Sono Emma, la tua insegnante. Oggi impariamo '${lesson.title}' (${lesson.cefrLevel}).${if (phraseNote.isNotEmpty()) " Iniziamo con:$phraseNote." else ""} Come stai oggi?"
      "pt" -> "Olá! Sou a Emma, sua professora de português. Hoje vamos aprender '${lesson.title}' (${lesson.cefrLevel}).${if (phraseNote.isNotEmpty()) " Vamos começar com:$phraseNote." else ""} Como você está hoje?"
      "ar" -> "مرحباً! أنا إيما، معلمتك للغة العربية. اليوم سنتعلم '${lesson.title}' (${lesson.cefrLevel}). كيف حالك اليوم؟"
      "ur" -> "السلام علیکم! میں ایما ہوں، آپ کی اردو ٹیچر۔ آج ہم '${lesson.title}' (${lesson.cefrLevel}) سیکھیں گے۔ آپ آج کیسے ہیں؟"
      "hi" -> "नमस्ते! मैं एम्मा हूँ, आपकी हिंदी शिक्षिका। आज हम '${lesson.title}' (${lesson.cefrLevel}) सीखेंगे। आज आप कैसे हैं?"
      "ja" -> "こんにちは！エマ先生です。今日は「${lesson.title}」（${lesson.cefrLevel}）を一緒に学びましょう。今日の調子はいかがですか？"
      "zh" -> "你好！我是艾玛老师。今天我们学习《${lesson.title}》（${lesson.cefrLevel}）。你今天感觉怎么样？"
      "ko" -> "안녕하세요! 저는 엠마 선생님이에요. 오늘은 '${lesson.title}'(${lesson.cefrLevel})을(를) 함께 배워볼까요? 오늘 기분이 어떠세요?"
      "tr" -> "Merhaba! Ben Türkçe öğretmenin Emma. Bugün '${lesson.title}' (${lesson.cefrLevel}) konusunu öğreneceğiz. Bugün nasılsın?"
      "ru" -> "Здравствуйте! Я Эмма, ваш преподаватель. Сегодня мы изучаем тему «${lesson.title}» (${lesson.cefrLevel}). Как ваши дела сегодня?"
      else -> "Hello! How are you doing today? I'm Emma, your ${targetLang.name} teacher. Today we're learning '${lesson.title}' (${lesson.cefrLevel}).${if (phraseNote.isNotEmpty()) " Let's start with:$phraseNote." else ""}"
    }
  }

  val openingPracticeGreeting = remember(lesson.id, targetLang.code) {
    when (targetLang.code.lowercase().substringBefore("-")) {
      "es" -> "¡Modo de práctica activado para '${lesson.title}' (${lesson.cefrLevel})! Conversemos de forma natural sobre '${lesson.description}'. ¿Cómo empezarías?"
      "fr" -> "Mode pratique activé pour '${lesson.title}' (${lesson.cefrLevel}) ! Discutons naturellement autour de '${lesson.description}'. Comment commenceriez-vous ?"
      "de" -> "Übungsmodus für '${lesson.title}' (${lesson.cefrLevel})! Lass uns ein echtes Gespräch führen. Wie würdest du beginnen?"
      "ja" -> "「${lesson.title}」（${lesson.cefrLevel}）の実践モードです！自然な会話を始めましょう。"
      "zh" -> "欢迎来到《${lesson.title}》（${lesson.cefrLevel}）对话练习模式！让我们开始自然交流吧。"
      else -> "Welcome to Practice Mode for '${lesson.title}' (${lesson.cefrLevel})! Let's roleplay '${lesson.description}'—how would you start our conversation?"
    }
  }

  // Separate conversation histories for Learn Mode and Practice Mode so switching never loses context
  var learnModeTurns by remember(lesson.id) {
    mutableStateOf(
      listOf(
        RefConversationTurn(
          sender = "Emma",
          text = openingLearnGreeting,
          translation = "Hello! How are you doing today? I'm Emma, your ${targetLang.name} teacher. Today we are learning '${lesson.title}' (${lesson.cefrLevel})."
        )
      )
    )
  }

  var practiceModeTurns by remember(lesson.id) {
    mutableStateOf(
      listOf(
        RefConversationTurn(
          sender = "Emma",
          text = openingPracticeGreeting,
          translation = "Welcome to Practice Mode for '${lesson.title}' (${lesson.cefrLevel})! Let's practice in a real conversation."
        )
      )
    )
  }

  val conversationTurns = if (currentMode == EmmaLessonMode.LEARN) learnModeTurns else practiceModeTurns

  fun appendTurnToCurrentMode(turn: RefConversationTurn) {
    if (currentMode == EmmaLessonMode.LEARN) {
      learnModeTurns = learnModeTurns + turn
    } else {
      practiceModeTurns = practiceModeTurns + turn
    }
  }

  var typedInput by remember { mutableStateOf("") }
  var isGeneratingResponse by remember { mutableStateOf(false) }
  var successfulExchanges by remember { mutableIntStateOf(initialCorrectCount.coerceAtLeast(0)) }
  var isLessonComplete by remember { mutableStateOf(false) }
  var showTranslationForIndex by remember { mutableStateOf<Int?>(null) }

  val isListening by speechManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }
  val isUserSpeaking by speechManager?.isUserSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
  val isProcessingSpeech by speechManager?.isProcessingSpeech?.collectAsState() ?: remember { mutableStateOf(false) }
  val isSpeaking by speechManager?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
  val rmsLevel by speechManager?.rmsLevel?.collectAsState() ?: remember { mutableFloatStateOf(0f) }
  val speechAperture by speechManager?.speechAperture?.collectAsState() ?: remember { mutableFloatStateOf(0f) }
  val selectedTutorVoice by speechManager?.selectedTutorVoice?.collectAsState() ?: remember { mutableStateOf(TutorVoice.AOEDE) }
  val managerError by speechManager?.speechError?.collectAsState() ?: remember { mutableStateOf(null) }

  val activeErrorMessage = localErrorMessage ?: managerError

  // Breathing & pulse animation for Emma's speaking avatar
  val infiniteTransition = rememberInfiniteTransition(label = "emma_pulse")
  val speakingGlowScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_scale"
  )

  // Echo filter: prevent Emma's own TTS output from being treated as learner speech
  val isEchoOfEmma: (String) -> Boolean = { candidate ->
    val normCandidate = candidate.trim().lowercase()
    val recentEmmaTexts = conversationTurns
      .filter { it.sender == "Emma" }
      .takeLast(3)
      .map { it.text }
    normCandidate.isBlank() ||
      GeminiTutorService.isLikelyEchoOfAiUtterance(candidate, recentEmmaTexts)
  }

  // Starts or re-arms microphone listening for the learner's next turn
  val startNextListeningTurn: () -> Unit = {
    silenceDebounceJob?.cancel()
    autoRelistenJob?.cancel()
    localErrorMessage = null
    speechManager?.clearError()
    val hasMicPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasMicPerm) {
      voiceSessionActive = false
      voiceState = EmmaVoiceSessionState.ERROR
      localErrorMessage = "Microphone permission is required to speak with Emma."
    } else if (voiceSessionActive && !isVoiceMuted && !isTypingMode && !isGeneratingResponse) {
      // Never start listening while TTS is still actively speaking
      if (speechManager?.isSpeaking?.value == true) {
        speechManager.stopSpeaking()
      }
      liveVoicePartial = ""
      speechManager?.startListening(lesson.targetLanguageCode)
    }
  }

  // Speaks Emma's response using the active voice (without duplicate overlapping playback) and transitions to Listening when done
  val speakEmmaAndResumeListening: (String, Boolean, Float?) -> Unit = { emmaText, forceSpeak, overrideSpeed ->
    silenceDebounceJob?.cancel()
    autoRelistenJob?.cancel()
    liveVoicePartial = ""
    speechManager?.stopListening()

    val cleanEmma = emmaText.trim()
    val nowMs = System.currentTimeMillis()
    val isDuplicateImmediateTts = !forceSpeak &&
      cleanEmma.equals(lastSpokenEmmaText, ignoreCase = true) &&
      (nowMs - lastSpokenEmmaAtMs) < 1500L

    if (cleanEmma.isNotBlank() && speechManager != null && (autoReadEnabled || forceSpeak) && !isDuplicateImmediateTts) {
      lastSpokenEmmaText = cleanEmma
      lastSpokenEmmaAtMs = nowMs
      voiceState = EmmaVoiceSessionState.TUTOR_SPEAKING
      val effectiveVoice = selectedTutorVoice
      val effectiveSpeed = overrideSpeed ?: playbackSpeed
      speechManager.speakWithTutorVoice(
        text = cleanEmma,
        voice = effectiveVoice,
        languageCode = lesson.targetLanguageCode,
        speedMultiplier = effectiveSpeed,
        onDone = {
          if (voiceSessionActive && !isVoiceMuted && !isTypingMode) {
            autoRelistenJob?.cancel()
            autoRelistenJob = coroutineScope.launch {
              // Acoustic damping buffer to prevent speaker reverb feedback
              delay(360L)
              if (voiceSessionActive && !isVoiceMuted && !isTypingMode && !isGeneratingResponse && speechManager.isSpeaking.value == false) {
                consecutiveIdleTimeouts = 0
                startNextListeningTurn()
              }
            }
          } else if (isVoiceMuted) {
            voiceState = EmmaVoiceSessionState.PAUSED
          } else {
            voiceState = EmmaVoiceSessionState.IDLE
          }
        }
      )
    } else if (voiceSessionActive && !isVoiceMuted && !isTypingMode) {
      consecutiveIdleTimeouts = 0
      startNextListeningTurn()
    } else {
      voiceState = if (isVoiceMuted) EmmaVoiceSessionState.PAUSED else EmmaVoiceSessionState.IDLE
    }
  }

  // Submits a verified learner message (from voice recognition or text input) to AI Tutor
  lateinit var dispatchLessonTurn: (String, Boolean, Boolean) -> Unit
  dispatchLessonTurn = { cleanedText, fromVoice, appendUserBubble ->
    if (cleanedText.isNotBlank() && !isGeneratingResponse) {
      consecutiveIdleTimeouts = 0
      localErrorMessage = null
      lastFailedLessonPrompt = null
      speechManager?.clearError()
      speechManager?.stopSpeaking()
      silenceDebounceJob?.cancel()
      autoRelistenJob?.cancel()
      liveVoicePartial = ""

      if (appendUserBubble) {
        if (fromVoice) {
          speechManager?.stopListening()
        } else {
          typedInput = ""
        }
        val userTurn = RefConversationTurn(sender = "Learner", text = cleanedText)
        appendTurnToCurrentMode(userTurn)
      }
      val activeHistory = if (currentMode == EmmaLessonMode.LEARN) learnModeTurns else practiceModeTurns

      coroutineScope.launch {
        isGeneratingResponse = true
        voiceState = EmmaVoiceSessionState.THINKING
        try {
          val historyPairs = activeHistory
            .dropLast(1)
            .takeLast(12)
            .map { turn ->
              val role = if (turn.sender == "Emma") "Emma" else "Learner"
              role to turn.text
            }

          val keyPhrasesContext = if (lessonKeyPhrases.isNotEmpty()) {
            " Target lesson expressions to weave in naturally: ${lessonKeyPhrases.joinToString("; ")}."
          } else {
            ""
          }

          val scenarioInstruction = if (currentMode == EmmaLessonMode.LEARN) {
            "LEARN MODE for lesson '${lesson.title}' (${lesson.description}, unit '${lesson.unitTitle}') at CEFR level ${lesson.cefrLevel}.$keyPhrasesContext " +
              "1) Answer what the learner directly said or asked first (\"$cleanedText\"). " +
              "2) Teach or reinforce one key phrase or grammar point from '${lesson.title}' in a natural step-by-step sequence. " +
              "3) Ask one short, encouraging follow-up question that stays strictly on '${lesson.title}'."
          } else {
            "PRACTICE MODE for lesson '${lesson.title}' (${lesson.description}, unit '${lesson.unitTitle}') at CEFR level ${lesson.cefrLevel}.$keyPhrasesContext " +
              "1) Stay in an authentic, interactive conversation focused on '${lesson.title}'. " +
              "2) Answer directly to \"$cleanedText\", correct ONLY genuine grammar or word choice mistakes, and ask a natural contextual follow-up."
          }

          val aiResponse = GeminiTutorService.chatWithTutor(
            userSpeechInput = cleanedText,
            targetLanguage = targetLang,
            nativeLanguage = nativeLang,
            proficiencyLevel = lesson.cefrLevel.ifBlank { "A1" },
            scenarioTitle = "${lesson.title} (${currentMode.title})",
            scenarioInstruction = scenarioInstruction,
            conversationHistory = historyPairs,
            partnerName = "Emma",
            conversationMode = if (currentMode == EmmaLessonMode.LEARN) {
              com.example.ai.TutorConversationMode.LEARN
            } else {
              com.example.ai.TutorConversationMode.PRACTICE
            },
            hasSpeechRecognitionTelemetry = fromVoice
          )

          if (aiResponse.isServiceError || aiResponse.replyInTargetLanguage.isBlank()) {
            isGeneratingResponse = false
            lastFailedLessonPrompt = cleanedText
            lastFailedLessonFromVoice = fromVoice
            localErrorMessage = aiResponse.errorMessage ?: "Could not reach AI Tutor. Tap Retry to continue."
            voiceState = EmmaVoiceSessionState.ERROR
            return@launch
          }

          successfulExchanges++
          onSaveCheckpoint(successfulExchanges, successfulExchanges, 5)
          showTurnDoneBadge = true
          turnDoneResetJob?.cancel()
          turnDoneResetJob = coroutineScope.launch {
            delay(650L)
            showTurnDoneBadge = false
          }

          val parsedCorrection = aiResponse.grammarFeedback
            ?.takeIf { it.isNotBlank() && !it.equals("NONE", ignoreCase = true) }

          if (!parsedCorrection.isNullOrBlank()) {
            onRecordMistake(
              MistakeNotebookEntry(
                languageCode = targetLang.code,
                originalText = cleanedText,
                correctedText = parsedCorrection.take(120),
                category = "Grammar",
                explanationNative = parsedCorrection
              )
            )
          }

          val emmaTurn = RefConversationTurn(
            sender = "Emma",
            text = aiResponse.replyInTargetLanguage,
            phonetic = aiResponse.phoneticGuide.takeIf { it.isNotBlank() && !it.contains("Listen to native") },
            translation = aiResponse.translationInNativeLanguage,
            correctionTip = parsedCorrection
          )
          appendTurnToCurrentMode(emmaTurn)
          isGeneratingResponse = false
          speakEmmaAndResumeListening(aiResponse.replyInTargetLanguage, false, null)
        } catch (_: Exception) {
          isGeneratingResponse = false
          lastFailedLessonPrompt = cleanedText
          lastFailedLessonFromVoice = fromVoice
          localErrorMessage = "Could not reach AI Tutor. Tap Retry to continue."
          voiceState = EmmaVoiceSessionState.ERROR
        }
      }
    }
  }

  val submitConversationMessage: (String, Boolean) -> Unit = { rawUserText, fromVoice ->
    val cleanedText = rawUserText
      .trim()
      .takeIf {
        it.length >= 2 &&
          !it.equals("Listening...", ignoreCase = true) &&
          !it.equals("Capturing your voice...", ignoreCase = true) &&
          !it.equals("SILENCE", ignoreCase = true)
      }
      .orEmpty()

    val now = System.currentTimeMillis()
    val isDuplicateSubmission = cleanedText.equals(lastSubmittedSpeech, ignoreCase = true) && (now - lastSubmittedAtMs) < 2200L

    if (cleanedText.isNotBlank() && !isGeneratingResponse && !isDuplicateSubmission && (!fromVoice || !isEchoOfEmma(cleanedText))) {
      lastSubmittedSpeech = cleanedText
      lastSubmittedAtMs = now
      dispatchLessonTurn(cleanedText, fromVoice, true)
    }
  }

  val currentSubmitMessage by rememberUpdatedState(submitConversationMessage)
  val currentStartNextListening by rememberUpdatedState(startNextListeningTurn)
  val currentIsEchoOfEmma by rememberUpdatedState(isEchoOfEmma)

  // Wire continuous speech callbacks for real two-way voice conversation
  DisposableEffect(speechManager, lesson.id) {
    speechManager?.setCallSpeechCallbacks(
      onPartial = { partialText ->
        val cleanPartial = partialText.trim()
        if (
          voiceSessionActive &&
          !isVoiceMuted &&
          !isTypingMode &&
          !isGeneratingResponse &&
          cleanPartial.isNotBlank() &&
          !cleanPartial.equals("Capturing your voice...", ignoreCase = true)
        ) {
          if (!currentIsEchoOfEmma(cleanPartial)) {
            liveVoicePartial = cleanPartial
            silenceDebounceJob?.cancel()
            silenceDebounceJob = coroutineScope.launch {
              delay(1500L)
              val readyText = liveVoicePartial.trim()
              if (readyText.isNotBlank() && voiceSessionActive && !isVoiceMuted && !isTypingMode && !isGeneratingResponse) {
                currentSubmitMessage(readyText, true)
              }
            }
          }
        }
      },
      onResult = { finalResult ->
        val cleanFinal = finalResult.trim()
        silenceDebounceJob?.cancel()
        if (voiceSessionActive && !isVoiceMuted && !isTypingMode && !isGeneratingResponse && cleanFinal.isNotBlank()) {
          if (!currentIsEchoOfEmma(cleanFinal)) {
            currentSubmitMessage(cleanFinal, true)
          } else {
            currentStartNextListening()
          }
        }
      },
      onSpeechStarted = {
        // Natural interruption: stop Emma if she is speaking when the learner starts talking
        if (speechManager.isSpeaking.value) {
          speechManager.stopSpeaking()
        }
        if (voiceSessionActive && !isVoiceMuted && !isTypingMode) {
          voiceState = EmmaVoiceSessionState.USER_SPEAKING
        }
      },
      onIdleTimeout = {
        if (voiceSessionActive && !isVoiceMuted && !isTypingMode && !isGeneratingResponse && !speechManager.isSpeaking.value) {
          consecutiveIdleTimeouts++
          if (consecutiveIdleTimeouts <= 1) {
            autoRelistenJob?.cancel()
            autoRelistenJob = coroutineScope.launch {
              delay(350L)
              if (voiceSessionActive && !isVoiceMuted && !isTypingMode && !isGeneratingResponse) {
                currentStartNextListening()
              }
            }
          } else {
            voiceState = EmmaVoiceSessionState.IDLE
          }
        }
      }
    )

    onDispose {
      silenceDebounceJob?.cancel()
      autoRelistenJob?.cancel()
      turnDoneResetJob?.cancel()
      speechManager?.setCallSpeechCallbacks(null, null, null, null)
      speechManager?.stopSpeaking()
      speechManager?.stopListening()
    }
  }

  // Audio permission launcher
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      localErrorMessage = null
      speechManager?.clearError()
      isTypingMode = false
      isVoiceMuted = false
      voiceSessionActive = true
      consecutiveIdleTimeouts = 0
      coroutineScope.launch {
        delay(120L)
        if (speechManager?.isSpeaking?.value == true) {
          speechManager.stopSpeaking()
        }
        startNextListeningTurn()
      }
    } else {
      voiceSessionActive = false
      localErrorMessage = "Microphone permission was denied. Tap Retry to grant permission or use Type mode."
      voiceState = EmmaVoiceSessionState.ERROR
    }
  }

  // On launch: check permission and speak Emma's opening greeting once
  LaunchedEffect(lesson.id) {
    val hasMicPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (hasMicPermission) {
      voiceSessionActive = true
      isVoiceMuted = false
    } else {
      voiceSessionActive = false
    }

    if (openingLearnGreeting.isNotBlank()) {
      speakEmmaAndResumeListening(openingLearnGreeting, false, null)
    } else if (hasMicPermission) {
      startNextListeningTurn()
    }
  }

  // Synchronize EmmaVoiceSessionState accurately with real system states
  LaunchedEffect(
    isSpeaking,
    isListening,
    isUserSpeaking,
    isProcessingSpeech,
    isGeneratingResponse,
    voiceSessionActive,
    isVoiceMuted,
    activeErrorMessage,
    liveVoicePartial,
    showTurnDoneBadge
  ) {
    voiceState = when {
      !activeErrorMessage.isNullOrBlank() -> EmmaVoiceSessionState.ERROR
      isProcessingSpeech -> EmmaVoiceSessionState.PROCESSING
      isGeneratingResponse -> EmmaVoiceSessionState.THINKING
      isSpeaking -> EmmaVoiceSessionState.TUTOR_SPEAKING
      isListening && (isUserSpeaking || liveVoicePartial.isNotBlank() || rmsLevel >= 0.16f) ->
        EmmaVoiceSessionState.USER_SPEAKING
      isListening -> EmmaVoiceSessionState.USER_LISTENING
      showTurnDoneBadge -> EmmaVoiceSessionState.DONE
      isVoiceMuted -> EmmaVoiceSessionState.PAUSED
      else -> EmmaVoiceSessionState.IDLE
    }
  }

  // Resolve the universal 8-state 3D EmojiVoiceState from the live session
  val currentEmojiVoiceState = resolveEmojiVoiceState(
    isListening = isListening,
    isUserSpeaking = isUserSpeaking || liveVoicePartial.isNotBlank(),
    isProcessingSpeech = isProcessingSpeech,
    isThinking = isGeneratingResponse,
    isReplying = isSpeaking,
    isDone = showTurnDoneBadge || isLessonComplete,
    hasError = !activeErrorMessage.isNullOrBlank(),
    audioLevel = if (isSpeaking) speechAperture else rmsLevel
  )

  val liveAudioLevel = when {
    isSpeaking -> speechAperture.coerceIn(0.08f, 1f)
    isListening -> rmsLevel.coerceIn(0.04f, 1f)
    else -> 0f
  }

  // Detect manual scrolling by learner to pause automatic conversation following
  LaunchedEffect(listState.isScrollInProgress) {
    if (listState.isScrollInProgress && !isProgrammaticScroll && isFollowingConversation) {
      isPausedByManualScroll = true
    }
  }

  // Auto-scroll to latest message when Follow Conversation is active
  LaunchedEffect(conversationTurns.size, liveVoicePartial, isFollowingConversation, isPausedByManualScroll, currentMode) {
    if (conversationTurns.isNotEmpty() && isFollowingConversation && !isPausedByManualScroll) {
      isProgrammaticScroll = true
      try {
        val lastIdx = (conversationTurns.size + 1).coerceAtLeast(0)
        listState.animateScrollToItem(lastIdx)
      } catch (_: Exception) {
      } finally {
        isProgrammaticScroll = false
      }
    }
  }

  val handleRetryVoiceAction: () -> Unit = {
    val failedPrompt = lastFailedLessonPrompt
    if (!failedPrompt.isNullOrBlank()) {
      localErrorMessage = null
      dispatchLessonTurn(failedPrompt, lastFailedLessonFromVoice, false)
    } else {
      localErrorMessage = null
      speechManager?.clearError()
      consecutiveIdleTimeouts = 0
      val hasPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
      if (!hasPerm) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      } else {
        voiceSessionActive = true
        isVoiceMuted = false
        startNextListeningTurn()
      }
    }
  }

  val handleBackAndSave: () -> Unit = {
    voiceSessionActive = false
    silenceDebounceJob?.cancel()
    autoRelistenJob?.cancel()
    turnDoneResetJob?.cancel()
    speechManager?.stopSpeaking()
    speechManager?.stopListening()
    onNavigateBack()
  }

  BackHandler {
    handleBackAndSave()
  }

  Scaffold(
    containerColor = RoyalBlueDark,
    topBar = {
      // =========================================================================
      // COMPACT TOP NAVIGATION BAR (Strictly Single-Line Horizontal, Zero Overflow)
      // =========================================================================
      Surface(
        color = Color(0xFF0B152E),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left: Compact Close Button
          IconButton(
            onClick = handleBackAndSave,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.10f))
              .testTag("lesson_close_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Lesson",
              tint = RoyalTextPrimary,
              modifier = Modifier.size(18.dp)
            )
          }

          // Center: Single-Line Lesson Title + Live Timer Pill (weight(1f) prevents squeezing right pills)
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = lesson.title,
              color = RoyalTextPrimary,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(
                    if (isListening || isSpeaking) RoyalAccentEmerald else RoyalAccentCyan
                  )
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "$timerString • ${lesson.cefrLevel}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = RoyalTextSecondary,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          // Right: Auto-Read Toggle + 1.0x Speed Selector (Fixed horizontal width, never vertical)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = if (autoReadEnabled) RoyalPrimary.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.08f),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(
                1.dp,
                if (autoReadEnabled) RoyalAccentCyan.copy(alpha = 0.7f) else RoyalNavyBorder
              ),
              modifier = Modifier
                .requiredHeight(30.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  val nextAutoRead = !autoReadEnabled
                  autoReadEnabled = nextAutoRead
                  if (!nextAutoRead && isSpeaking) {
                    speechManager?.stopSpeaking()
                  } else if (nextAutoRead && !isSpeaking && !isListening) {
                    val lastEmma = conversationTurns.lastOrNull { it.sender == "Emma" }?.text.orEmpty()
                    if (lastEmma.isNotBlank()) {
                      speakEmmaAndResumeListening(lastEmma, true, null)
                    }
                  }
                }
                .testTag("lesson_auto_read_toggle_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (autoReadEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                  contentDescription = "Auto-Read",
                  tint = if (autoReadEnabled) RoyalAccentCyan else RoyalTextSecondary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Auto-Read",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (autoReadEnabled) RoyalAccentCyan else RoyalTextSecondary,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            val speedLabel = when {
              playbackSpeed < 0.95f -> "0.85x"
              playbackSpeed > 1.15f -> "1.2x"
              else -> "1.0x"
            }

            Surface(
              color = RoyalNavyElevated,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, RoyalNavyBorder.copy(alpha = 0.8f)),
              modifier = Modifier
                .widthIn(min = 44.dp)
                .requiredHeight(30.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  playbackSpeed = when {
                    playbackSpeed < 0.95f -> 1.0f
                    playbackSpeed < 1.15f -> 1.2f
                    else -> 0.85f
                  }
                  speechManager?.setSpeechRate(playbackSpeed)
                }
                .testTag("lesson_speed_toggle_button")
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
              ) {
                Text(
                  text = speedLabel,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = RoyalTextPrimary,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    if (showAutoScrollLessonReader) {
      var lessonTeleprompterContent by remember(lesson.id) {
        mutableStateOf(AutoScrollLearningService.fromCurriculumLesson(lesson))
      }
      var selectedLearnCategory by remember {
        mutableStateOf(AutoScrollLearningService.LEARN_CATEGORIES.first())
      }
      var isGeneratingLessonStory by remember { mutableStateOf(false) }
      var lessonTeleprompterError by remember { mutableStateOf<String?>(null) }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        AutoScrollTeleprompterView(
          userId = "learner",
          content = lessonTeleprompterContent,
          categories = AutoScrollLearningService.LEARN_CATEGORIES,
          selectedCategory = selectedLearnCategory,
          selectedCefrLevel = lesson.cefrLevel.ifBlank { "A2" },
          isFullScreen = true,
          isGenerating = isGeneratingLessonStory,
          errorMessage = lessonTeleprompterError,
          speechManager = speechManager,
          onSelectCategory = { cat ->
            selectedLearnCategory = cat
            coroutineScope.launch {
              isGeneratingLessonStory = true
              lessonTeleprompterError = null
              val res = AutoScrollLearningService.generateFreshAutoScrollContent(
                context = context,
                userId = "learner",
                pageSource = AutoScrollPageSource.LEARN,
                targetLanguageCode = targetLang.code,
                nativeLanguageCode = "en",
                cefrLevel = lesson.cefrLevel.ifBlank { "A2" },
                category = cat,
                topic = "${lesson.title} ($cat)",
                isPremiumUser = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement
              )
              isGeneratingLessonStory = false
              when (res) {
                is com.example.ai.AutoScrollGenerationResult.Success -> {
                  lessonTeleprompterContent = res.content
                }
                is com.example.ai.AutoScrollGenerationResult.Error -> {
                  lessonTeleprompterError = res.userFriendlyMessage
                }
              }
            }
          },
          onSelectCefrLevel = {},
          onRequestGenerateNew = { cat, cefr, len, customHint ->
            selectedLearnCategory = cat
            coroutineScope.launch {
              isGeneratingLessonStory = true
              lessonTeleprompterError = null
              val res = AutoScrollLearningService.generateFreshAutoScrollContent(
                context = context,
                userId = "learner",
                pageSource = AutoScrollPageSource.LEARN,
                targetLanguageCode = targetLang.code,
                nativeLanguageCode = "en",
                cefrLevel = cefr.ifBlank { lesson.cefrLevel.ifBlank { "A2" } },
                category = cat,
                topic = customHint.takeIf { it.isNotBlank() } ?: lesson.title,
                contentLength = len,
                isPremiumUser = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement
              )
              isGeneratingLessonStory = false
              when (res) {
                is com.example.ai.AutoScrollGenerationResult.Success -> {
                  lessonTeleprompterContent = res.content
                }
                is com.example.ai.AutoScrollGenerationResult.Error -> {
                  lessonTeleprompterError = res.userFriendlyMessage
                }
              }
            }
          },
          onSaveVocabToFlashcard = onSaveVocabToFlashcard,
          onCompleteActivity = { _, _, _, _ ->
            successfulExchanges = (successfulExchanges + 2).coerceAtLeast(3)
          },
          onBack = {
            speechManager?.stopSpeaking()
            showAutoScrollLessonReader = false
          }
        )
      }
      return@Scaffold
    }

    if (isLessonComplete) {
      val awardedXp = if (isReplay) 25 else lesson.xpReward.coerceAtLeast(50)
      LessonCompleteSummaryView(
        lesson = lesson,
        xpEarned = awardedXp,
        successfulTurns = successfulExchanges,
        onClaimAndFinish = {
          onFinishLesson(100, awardedXp)
        },
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(
            Brush.verticalGradient(
              colors = listOf(
                RoyalBlueDark,
                Color(0xFF09163B),
                Color(0xFF030712)
              )
            )
          )
      ) {
        // =========================================================================
        // 1. COMPACT EMMA AI TUTOR PROFILE CARD + LIVE STATE + LEARN/PRACTICE TABS
        // =========================================================================
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("lesson_tutor_header_card"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = RoyalNavyCard),
          border = BorderStroke(1.dp, RoyalNavyBorder.copy(alpha = 0.85f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Left: Emma Avatar + Online Badge + Identity + Language/Progress
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (voiceState == EmmaVoiceSessionState.TUTOR_SPEAKING) {
                    Box(
                      modifier = Modifier
                        .size(54.dp)
                        .scale(speakingGlowScale)
                        .clip(CircleShape)
                        .background(RoyalAccentCyan.copy(alpha = 0.24f))
                    )
                  }

                  val emmaTutor = remember {
                    com.example.avatar.TutorPersona.ALL_TUTORS.firstOrNull { it.id == "tutor_emma" }
                      ?: com.example.avatar.TutorPersona.ALL_TUTORS.first()
                  }
                  com.example.avatar.ui.TutorPersonaAvatarBadge(
                    tutor = emmaTutor,
                    isSpeaking = voiceState == EmmaVoiceSessionState.TUTOR_SPEAKING,
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .border(
                        width = if (voiceState == EmmaVoiceSessionState.TUTOR_SPEAKING) 2.dp else 1.5.dp,
                        color = when (voiceState) {
                          EmmaVoiceSessionState.TUTOR_SPEAKING -> RoyalAccentCyan
                          EmmaVoiceSessionState.USER_LISTENING, EmmaVoiceSessionState.USER_SPEAKING -> RoyalAccentEmerald
                          else -> RoyalPrimaryBright.copy(alpha = 0.7f)
                        },
                        shape = CircleShape
                      )
                  )

                  // Online status indicator dot on Emma's portrait
                  Box(
                    modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .size(12.dp)
                      .clip(CircleShape)
                      .background(RoyalNavyCard)
                      .padding(2.dp)
                      .clip(CircleShape)
                      .background(RoyalAccentEmerald)
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "Emma",
                      color = RoyalTextPrimary,
                      fontSize = 15.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      maxLines = 1,
                      softWrap = false
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = RoyalPrimary.copy(alpha = 0.24f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "AI Tutor",
                        color = RoyalAccentCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${targetLang.flagEmoji} ${targetLang.name} • ${lesson.cefrLevel} • ${successfulExchanges}/4 turns",
                    color = RoyalTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Spacer(modifier = Modifier.width(6.dp))

              // Right: Live Status Pill (Connected / Listening / Speaking / Processing) + EmojiVoice
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                val statusDotColor = when (voiceState) {
                  EmmaVoiceSessionState.USER_LISTENING, EmmaVoiceSessionState.USER_SPEAKING -> RoyalAccentEmerald
                  EmmaVoiceSessionState.TUTOR_SPEAKING -> RoyalAccentCyan
                  EmmaVoiceSessionState.THINKING, EmmaVoiceSessionState.PROCESSING -> RoyalAccentAmber
                  EmmaVoiceSessionState.DONE, EmmaVoiceSessionState.IDLE -> RoyalAccentEmerald
                  EmmaVoiceSessionState.PAUSED -> RoyalAccentAmber
                  EmmaVoiceSessionState.ERROR -> RoyalAccentCoral
                }

                Surface(
                  color = statusDotColor.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, statusDotColor.copy(alpha = 0.55f)),
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      if (voiceState == EmmaVoiceSessionState.ERROR) {
                        handleRetryVoiceAction()
                      } else if (voiceState == EmmaVoiceSessionState.IDLE || voiceState == EmmaVoiceSessionState.PAUSED) {
                        isVoiceMuted = false
                        voiceSessionActive = true
                        startNextListeningTurn()
                      }
                    }
                    .testTag("lesson_live_status_badge")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.5.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = voiceState.badgeText,
                      color = RoyalTextPrimary,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }

                EmojiVoice(
                  state = currentEmojiVoiceState,
                  size = 34.dp,
                  animated = true,
                  audioLevel = liveAudioLevel,
                  isRecording = isListening,
                  isPlaying = isSpeaking,
                  onRetry = handleRetryVoiceAction,
                  onClick = {
                    if (voiceState == EmmaVoiceSessionState.ERROR) {
                      handleRetryVoiceAction()
                    } else if (isSpeaking) {
                      speechManager?.stopSpeaking()
                      startNextListeningTurn()
                    } else if (isListening) {
                      speechManager?.finishListeningNow()
                    } else {
                      startNextListeningTurn()
                    }
                  }
                )
              }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Subtle lesson mastery progress bar
            LinearProgressIndicator(
              progress = { (successfulExchanges / 4f).coerceIn(0.08f, 1f) },
              color = RoyalAccentCyan,
              trackColor = RoyalNavyElevated,
              modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(7.dp))

            // Mode Switcher: Learn Mode vs Practice Mode
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(RoyalNavyElevated, RoundedCornerShape(10.dp))
                .padding(2.5.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              EmmaLessonMode.entries.forEach { mode ->
                val isSelected = currentMode == mode
                val tabTag = if (mode == EmmaLessonMode.LEARN) "lesson_mode_learn_tab" else "lesson_mode_practice_tab"
                Surface(
                  color = if (isSelected) RoyalPrimary else Color.Transparent,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      if (currentMode != mode) {
                        currentMode = mode
                        val latestEmmaInMode = (if (mode == EmmaLessonMode.LEARN) learnModeTurns else practiceModeTurns)
                          .lastOrNull { it.sender == "Emma" }?.text.orEmpty()
                        if (latestEmmaInMode.isNotBlank() && autoReadEnabled) {
                          speakEmmaAndResumeListening(latestEmmaInMode, false, null)
                        }
                      }
                    }
                    .testTag(tabTag)
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = mode.icon, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = mode.title,
                      color = if (isSelected) Color.White else RoyalTextSecondary,
                      fontSize = 11.5.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
            }
          }
        }

        // =========================================================================
        // 2. CONVERSATION CONTROLS STRIP (Strictly Horizontal, Never Vertical)
        // =========================================================================
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(modifier = Modifier.weight(1f)) {
            ConversationFollowControlChip(
              isFollowing = isFollowingConversation,
              isPausedByManualScroll = isPausedByManualScroll,
              onToggleFollow = { newVal ->
                isPausedByManualScroll = false
                isFollowingConversation = newVal
              }
            )
          }

          Surface(
            onClick = {
              speechManager?.stopSpeaking()
              speechManager?.stopListening()
              showAutoScrollLessonReader = true
            },
            shape = RoundedCornerShape(20.dp),
            color = RoyalNavyElevated,
            border = BorderStroke(1.dp, RoyalAccentCyan.copy(alpha = 0.45f)),
            modifier = Modifier
              .widthIn(min = 136.dp)
              .requiredHeight(30.dp)
              .testTag("lesson_open_teleprompter_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Story Teleprompter",
                tint = RoyalAccentCyan,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Story Teleprompter",
                color = RoyalAccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Error / Retry Banner when microphone or network needs attention
        AnimatedVisibility(visible = !activeErrorMessage.isNullOrBlank()) {
          Surface(
            color = RoyalAccentCoral.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, RoyalAccentCoral.copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = activeErrorMessage.orEmpty(),
                color = RoyalTextPrimary,
                fontSize = 11.5.sp,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                color = RoyalAccentCoral,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { handleRetryVoiceAction() }
                  .testTag("lesson_error_retry_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Retry",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }
        }

        // =========================================================================
        // 3. SCROLLABLE CONVERSATION AREA (Natural Wrapping, Never Hidden by Mic)
        // =========================================================================
        LazyColumn(
          state = listState,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("lesson_conversation_list"),
          contentPadding = PaddingValues(top = 6.dp, bottom = 14.dp)
        ) {
          // Compact Lesson Goal Banner
          item(key = "lesson_immersion_banner") {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = RoyalNavyElevated.copy(alpha = 0.65f)),
              border = BorderStroke(1.dp, RoyalNavyBorder.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("lesson_goal_banner")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(RoyalPrimary.copy(alpha = 0.22f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.School, contentDescription = null, tint = RoyalAccentCyan, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (currentMode == EmmaLessonMode.LEARN)
                      "Lesson Goal: ${lesson.title} (${lesson.cefrLevel})"
                    else
                      "Practice Goal: ${lesson.title} (${lesson.cefrLevel})",
                    color = RoyalTextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (currentMode == EmmaLessonMode.LEARN)
                      lesson.description.ifBlank { "Emma teaches key phrases and guides your pronunciation step by step." }
                    else
                      "Interactive roleplay with live grammar & fluency feedback.",
                    color = RoyalTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.5.sp
                  )
                }
              }
            }
          }

          // Conversation Turns
          itemsIndexed(conversationTurns, key = { index, _ -> "${currentMode.name}_turn_$index" }) { idx, turn ->
            val isEmma = turn.sender == "Emma"
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = if (isEmma) Arrangement.Start else Arrangement.End
            ) {
              if (isEmma) {
                Surface(
                  color = RoyalNavyCard,
                  shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                  modifier = Modifier.fillMaxWidth(0.94f)
                ) {
                  Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = "Emma",
                          color = RoyalAccentCyan,
                          fontSize = 11.5.sp,
                          fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                          text = "• ${if (currentMode == EmmaLessonMode.LEARN) "AI Teacher" else "Practice Partner"}",
                          color = RoyalTextSecondary,
                          fontSize = 10.5.sp
                        )
                      }

                      // Compact Action Bar inside Emma's bubble (Replay, Translate, Bookmark)
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Surface(
                          shape = CircleShape,
                          color = RoyalNavyElevated,
                          modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { speakEmmaAndResumeListening(turn.text, true, null) }
                            .testTag("lesson_replay_audio_$idx")
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              Icons.AutoMirrored.Filled.VolumeUp,
                              contentDescription = "Replay Audio",
                              tint = RoyalAccentCyan,
                              modifier = Modifier.size(15.dp)
                            )
                          }
                        }

                        if (!turn.translation.isNullOrBlank() && !turn.translation.trim().equals(turn.text.trim(), ignoreCase = true)) {
                          Surface(
                            shape = CircleShape,
                            color = if (showTranslationForIndex == idx) RoyalPrimary.copy(alpha = 0.35f) else RoyalNavyElevated,
                            modifier = Modifier
                              .size(32.dp)
                              .clip(CircleShape)
                              .clickable {
                                showTranslationForIndex = if (showTranslationForIndex == idx) null else idx
                              }
                              .testTag("lesson_translate_turn_$idx")
                          ) {
                            Box(contentAlignment = Alignment.Center) {
                              Icon(
                                Icons.Default.Translate,
                                contentDescription = "Translate",
                                tint = if (showTranslationForIndex == idx) RoyalAccentCyan else RoyalTextSecondary,
                                modifier = Modifier.size(15.dp)
                              )
                            }
                          }
                        }

                        Surface(
                          shape = CircleShape,
                          color = RoyalNavyElevated,
                          modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                              onSaveVocabToFlashcard(
                                turn.text.take(60),
                                turn.translation ?: lesson.title,
                                turn.phonetic ?: ""
                              )
                            }
                            .testTag("lesson_bookmark_turn_$idx")
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              Icons.Default.BookmarkAdd,
                              contentDescription = "Save phrase",
                              tint = RoyalAccentAmber,
                              modifier = Modifier.size(15.dp)
                            )
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    SynchronizedAiSpeechText(
                      text = turn.text,
                      speechManager = speechManager,
                      baseTextColor = RoyalTextPrimary,
                      highlightColor = RoyalAccentCyan,
                      fontSizeSp = 15,
                      modifier = Modifier.fillMaxWidth()
                    )

                    if (!turn.phonetic.isNullOrBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "/${turn.phonetic}/",
                        color = RoyalTextSecondary,
                        fontSize = 11.sp
                      )
                    }

                    // Spoken Correction / Coaching Feedback if any
                    if (!turn.correctionTip.isNullOrBlank()) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Surface(
                        color = RoyalAccentEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalAccentEmerald, modifier = Modifier.size(13.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            text = turn.correctionTip,
                            color = RoyalAccentEmerald,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                          )
                        }
                      }
                    }

                    // Translation reveal
                    if (showTranslationForIndex == idx && !turn.translation.isNullOrBlank()) {
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(
                        text = "Translation: ${turn.translation}",
                        color = RoyalAccentCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                      )
                    }
                  }
                }
              } else {
                // Learner message on right in royal blue bubble
                Surface(
                  color = RoyalLearnerBubble,
                  shape = RoundedCornerShape(topStart = 16.dp, topEnd = 6.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                  modifier = Modifier.fillMaxWidth(0.84f)
                ) {
                  Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                    Text(
                      text = "You",
                      color = Color.White.copy(alpha = 0.8f),
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = turn.text,
                      color = Color.White,
                      fontSize = 14.sp,
                      lineHeight = 20.sp
                    )
                  }
                }
              }
            }
          }

          // Live partial voice preview bubble while learner is actively speaking
          if (liveVoicePartial.isNotBlank() && isListening && !isTypingMode) {
            item(key = "live_partial_bubble") {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                Surface(
                  color = RoyalLearnerBubble.copy(alpha = 0.78f),
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, RoyalAccentCyan.copy(alpha = 0.7f)),
                  modifier = Modifier.fillMaxWidth(0.84f)
                ) {
                  Column(modifier = Modifier.padding(11.dp)) {
                    Text(
                      text = liveVoicePartial,
                      color = Color.White,
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      CircularProgressIndicator(
                        color = RoyalAccentCyan,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(11.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Speaking... sends automatically when you pause",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.5.sp
                      )
                    }
                  }
                }
              }
            }
          }

          // Processing or Thinking Indicator with animated EmojiVoice
          if (isProcessingSpeech || isGeneratingResponse) {
            item(key = "emma_thinking_or_processing") {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Surface(
                  color = RoyalNavyCard,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, RoyalNavyBorder.copy(alpha = 0.6f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    EmojiVoice(
                      state = if (isProcessingSpeech) EmojiVoiceState.PROCESSING else EmojiVoiceState.THINKING,
                      size = 24.dp,
                      animated = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = if (isProcessingSpeech) "Processing your speech..." else "Emma is thinking...",
                      color = RoyalTextSecondary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            }
          }
        }

        // Smart Hint Card Banner directly above bottom controls when requested
        AnimatedVisibility(visible = showHintSheet && !isLessonComplete) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalNavyElevated),
            border = BorderStroke(1.dp, RoyalAccentAmber.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 4.dp)
              .testTag("lesson_hint_card")
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Lightbulb, contentDescription = null, tint = RoyalAccentAmber, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Suggested Response", color = RoyalAccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showHintSheet = false }, modifier = Modifier.size(22.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Close Hint", tint = RoyalTextSecondary, modifier = Modifier.size(14.dp))
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              if (isGeneratingHint) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(color = RoyalAccentAmber, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Emma is preparing a suggestion...", color = RoyalTextSecondary, fontSize = 11.5.sp)
                }
              } else {
                Text(
                  text = currentHintText ?: "Try saying: \"I'm doing great today, thank you!\"",
                  color = RoyalTextPrimary,
                  fontSize = 12.5.sp,
                  lineHeight = 17.sp
                )
              }
            }
          }
        }

        // =========================================================================
        // 4. COMPACT BOTTOM VOICE CONTROLS DOCK (Type, Mic, Audio/Speaker, Hint)
        // =========================================================================
        Surface(
          color = RoyalNavyCard,
          border = BorderStroke(1.dp, RoyalNavyBorder.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .imePadding()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Compact Live Waveform Visualizer ONLY when microphone is actively listening
            AnimatedVisibility(
              visible = isListening,
              enter = fadeIn(),
              exit = fadeOut()
            ) {
              LiveMicrophoneWaveformVisualizer(
                rmsLevel = rmsLevel,
                isUserSpeaking = isUserSpeaking || liveVoicePartial.isNotBlank()
              )
            }

            // Typing mode text field (compact, keyboard-safe)
            if (isTypingMode) {
              OutlinedTextField(
                value = typedInput,
                onValueChange = { typedInput = it },
                placeholder = {
                  Text(
                    "Type your message in ${targetLang.name}...",
                    color = RoyalTextSecondary,
                    fontSize = 12.5.sp
                  )
                },
                trailingIcon = {
                  IconButton(
                    onClick = {
                      if (typedInput.isNotBlank()) {
                        submitConversationMessage(typedInput, false)
                      }
                    },
                    modifier = Modifier.testTag("lesson_send_text_button")
                  ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = RoyalAccentCyan)
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = RoyalNavyElevated,
                  unfocusedContainerColor = RoyalNavyElevated,
                  focusedTextColor = RoyalTextPrimary,
                  unfocusedTextColor = RoyalTextPrimary,
                  focusedBorderColor = RoyalPrimaryBright,
                  unfocusedBorderColor = RoyalNavyBorder
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("lesson_text_input_field")
              )
              Spacer(modifier = Modifier.height(6.dp))
            }

            // Compact Bottom Controls Row: Type | Audio Status | Central Mic | Hint/Finish
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // 1. Type Mode Toggle
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    val next = !isTypingMode
                    isTypingMode = next
                    if (next) {
                      silenceDebounceJob?.cancel()
                      autoRelistenJob?.cancel()
                      liveVoicePartial = ""
                      speechManager?.stopListening()
                    } else if (voiceSessionActive && !isVoiceMuted && !isGeneratingResponse && !isSpeaking) {
                      startNextListeningTurn()
                    }
                  }
                  .padding(horizontal = 6.dp, vertical = 2.dp)
                  .testTag("lesson_toggle_keyboard_button")
              ) {
                Surface(
                  shape = CircleShape,
                  color = if (isTypingMode) RoyalPrimary else RoyalNavyElevated,
                  border = BorderStroke(1.dp, if (isTypingMode) RoyalAccentCyan else RoyalNavyBorder),
                  modifier = Modifier.size(38.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Keyboard,
                      contentDescription = "Type",
                      tint = RoyalTextPrimary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Type",
                  color = if (isTypingMode) RoyalAccentCyan else RoyalTextSecondary,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false
                )
              }

              // 2. Center Group: Mute/Speaker Status + Compact Mic Control + Status Label
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  // Mute / Audio Pause Button
                  IconButton(
                    onClick = {
                      val nextMuted = !isVoiceMuted
                      isVoiceMuted = nextMuted
                      if (nextMuted) {
                        silenceDebounceJob?.cancel()
                        autoRelistenJob?.cancel()
                        liveVoicePartial = ""
                        speechManager?.stopListening()
                        voiceState = EmmaVoiceSessionState.PAUSED
                      } else {
                        isTypingMode = false
                        voiceSessionActive = true
                        if (speechManager?.isSpeaking?.value == true) {
                          speechManager.stopSpeaking()
                        }
                        startNextListeningTurn()
                      }
                    },
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(if (isVoiceMuted) RoyalAccentAmber.copy(alpha = 0.22f) else RoyalNavyElevated)
                      .border(1.dp, if (isVoiceMuted) RoyalAccentAmber else RoyalNavyBorder, CircleShape)
                      .testTag("lesson_mute_voice_button")
                  ) {
                    Icon(
                      imageVector = if (isVoiceMuted) Icons.Default.MicOff else Icons.Default.Mic,
                      contentDescription = if (isVoiceMuted) "Unmute Microphone" else "Pause Microphone",
                      tint = if (isVoiceMuted) RoyalAccentAmber else RoyalTextPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  }

                  // Central Compact Microphone / EmojiVoice Action Button
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(54.dp)
                      .clip(CircleShape)
                      .background(
                        when (voiceState) {
                          EmmaVoiceSessionState.USER_LISTENING, EmmaVoiceSessionState.USER_SPEAKING ->
                            RoyalAccentEmerald.copy(alpha = 0.22f)
                          EmmaVoiceSessionState.TUTOR_SPEAKING ->
                            RoyalAccentCyan.copy(alpha = 0.22f)
                          else ->
                            RoyalPrimary.copy(alpha = 0.25f)
                        }
                      )
                      .border(
                        width = 1.5.dp,
                        color = when (voiceState) {
                          EmmaVoiceSessionState.USER_LISTENING, EmmaVoiceSessionState.USER_SPEAKING -> RoyalAccentEmerald
                          EmmaVoiceSessionState.TUTOR_SPEAKING -> RoyalAccentCyan
                          EmmaVoiceSessionState.ERROR -> RoyalAccentCoral
                          else -> RoyalPrimaryBright
                        },
                        shape = CircleShape
                      )
                      .clickable {
                        isTypingMode = false
                        localErrorMessage = null
                        speechManager?.clearError()
                        val hasPerm = ContextCompat.checkSelfPermission(
                          context,
                          Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPerm) {
                          audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (!voiceSessionActive || isVoiceMuted) {
                          voiceSessionActive = true
                          isVoiceMuted = false
                          if (speechManager?.isSpeaking?.value == true) {
                            speechManager.stopSpeaking()
                          }
                          startNextListeningTurn()
                        } else if (isSpeaking) {
                          // Interrupt Emma and start listening immediately
                          speechManager?.stopSpeaking()
                          startNextListeningTurn()
                        } else if (isListening && liveVoicePartial.isNotBlank()) {
                          submitConversationMessage(liveVoicePartial, true)
                        } else if (isListening) {
                          speechManager?.finishListeningNow()
                        } else {
                          consecutiveIdleTimeouts = 0
                          startNextListeningTurn()
                        }
                      }
                      .testTag("lesson_voice_mic_button")
                  ) {
                    EmojiVoice(
                      state = currentEmojiVoiceState,
                      size = 44.dp,
                      animated = true,
                      audioLevel = liveAudioLevel,
                      isRecording = isListening,
                      isPlaying = isSpeaking,
                      onRetry = handleRetryVoiceAction
                    )
                  }

                  // Speaker / Replay Last Emma Message or Finish Speaking Button
                  if (isListening) {
                    IconButton(
                      onClick = {
                        if (liveVoicePartial.isNotBlank()) {
                          submitConversationMessage(liveVoicePartial, true)
                        } else {
                          speechManager?.finishListeningNow()
                        }
                      },
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RoyalAccentEmerald.copy(alpha = 0.22f))
                        .border(1.dp, RoyalAccentEmerald, CircleShape)
                        .testTag("lesson_finish_speaking_button")
                    ) {
                      Icon(
                        imageVector = if (liveVoicePartial.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Stop,
                        contentDescription = "Finish speaking",
                        tint = RoyalAccentEmerald,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  } else {
                    IconButton(
                      onClick = {
                        if (isSpeaking) {
                          speechManager?.stopSpeaking()
                        } else {
                          val lastEmma = conversationTurns.lastOrNull { it.sender == "Emma" }?.text.orEmpty()
                          if (lastEmma.isNotBlank()) {
                            speakEmmaAndResumeListening(lastEmma, true, null)
                          }
                        }
                      },
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSpeaking) RoyalAccentCyan.copy(alpha = 0.22f) else RoyalNavyElevated)
                        .border(1.dp, if (isSpeaking) RoyalAccentCyan else RoyalNavyBorder, CircleShape)
                        .testTag("lesson_speaker_status_button")
                    ) {
                      Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSpeaking) "Stop Audio" else "Replay Emma Audio",
                        tint = if (isSpeaking) RoyalAccentCyan else RoyalTextPrimary,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = voiceState.label,
                  color = when (voiceState) {
                    EmmaVoiceSessionState.USER_LISTENING, EmmaVoiceSessionState.USER_SPEAKING -> RoyalAccentEmerald
                    EmmaVoiceSessionState.TUTOR_SPEAKING -> RoyalAccentCyan
                    EmmaVoiceSessionState.THINKING, EmmaVoiceSessionState.PROCESSING -> RoyalAccentAmber
                    EmmaVoiceSessionState.ERROR -> RoyalAccentCoral
                    EmmaVoiceSessionState.DONE -> RoyalAccentEmerald
                    else -> RoyalTextSecondary
                  },
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }

              // 3. Right: Hint / Finish Button
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    if (successfulExchanges >= 4) {
                      isLessonComplete = true
                    } else {
                      isGeneratingHint = true
                      showHintSheet = true
                      coroutineScope.launch {
                        try {
                          val lastEmma = conversationTurns.lastOrNull { it.sender == "Emma" }?.text.orEmpty()
                          val prompt = "Suggest one short, natural sentence the learner can say to Emma in response to: \"$lastEmma\" for lesson '${lesson.title}' (${lesson.cefrLevel}). Provide the sentence in ${targetLang.name} and its English translation."
                          val res = GeminiTutorService.generateGeminiText(
                            systemInstructionText = "You are a concise language learning hint assistant.",
                            userPromptText = prompt,
                            temperature = 0.5f
                          )
                          currentHintText = res?.trim() ?: "Say: \"I am doing great today! Let's practice '${lesson.title}'.\""
                        } catch (_: Exception) {
                          currentHintText = "Say: \"I am doing well, thank you! Could you give me an example?\""
                        } finally {
                          isGeneratingHint = false
                        }
                      }
                    }
                  }
                  .padding(horizontal = 6.dp, vertical = 2.dp)
                  .testTag("lesson_inspire_button")
              ) {
                Surface(
                  shape = CircleShape,
                  color = if (successfulExchanges >= 4) RoyalAccentEmerald.copy(alpha = 0.2f) else RoyalNavyElevated,
                  border = BorderStroke(1.dp, if (successfulExchanges >= 4) RoyalAccentEmerald else RoyalNavyBorder),
                  modifier = Modifier.size(38.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = if (successfulExchanges >= 4) Icons.Default.CheckCircle else Icons.Default.Lightbulb,
                      contentDescription = "Hint / Finish",
                      tint = if (successfulExchanges >= 4) RoyalAccentEmerald else RoyalAccentAmber,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = if (successfulExchanges >= 4) "Finish" else "Hint",
                  color = RoyalTextSecondary,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LiveMicrophoneWaveformVisualizer(
  rmsLevel: Float,
  isUserSpeaking: Boolean
) {
  val clamped = rmsLevel.coerceIn(0.08f, 1f)
  val barFactors = listOf(0.35f, 0.65f, 0.9f, 1.0f, 0.85f, 0.95f, 0.65f, 0.4f)
  val activeColor = if (isUserSpeaking) Color(0xFFFBBF24) else RoyalAccentEmerald
  Row(
    horizontalArrangement = Arrangement.spacedBy(3.5.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .padding(bottom = 5.dp)
      .height(22.dp)
      .background(RoyalNavyElevated, RoundedCornerShape(11.dp))
      .padding(horizontal = 10.dp, vertical = 3.dp)
  ) {
    barFactors.forEach { factor ->
      val h = (4f + (12f * clamped * factor)).dp
      Box(
        modifier = Modifier
          .width(3.dp)
          .height(h)
          .clip(RoundedCornerShape(2.dp))
          .background(activeColor)
      )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = if (isUserSpeaking) "Speaking • Capturing voice" else "Listening • Speak now",
      color = activeColor,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
private fun LessonCompleteSummaryView(
  lesson: CurriculumLesson,
  xpEarned: Int,
  successfulTurns: Int,
  onClaimAndFinish: () -> Unit,
  onPracticeMistakes: () -> Unit = onClaimAndFinish,
  modifier: Modifier = Modifier
) {
  var xpAnimationStarted by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    xpAnimationStarted = true
  }
  val animatedXp by androidx.compose.animation.core.animateIntAsState(
    targetValue = if (xpAnimationStarted) xpEarned else 0,
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "xp_count_up"
  )
  val newWordsCount = remember(lesson.id, successfulTurns) {
    maxOf(lesson.exercises.size.coerceAtLeast(3), successfulTurns + 2)
  }
  val accuracyPercent = remember(successfulTurns) {
    if (successfulTurns >= 3) 96 else 92
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(com.example.ui.components.LingoTokens.bgCanvas),
    contentAlignment = Alignment.Center
  ) {
    com.example.ui.components.LingoConfettiOverlay()

    com.example.ui.components.LingoCleanCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      contentPadding = 24.dp
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        com.example.ui.components.LingoTutorOrb(
          size = 84.dp,
          isSpeaking = true,
          audioLevel = 0.65f
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Lesson Complete",
          color = com.example.ui.components.LingoTokens.textPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Great work completing ${lesson.title}.",
          color = com.example.ui.components.LingoTokens.textSecondary,
          fontSize = 14.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(com.example.ui.components.LingoTokens.bgSubtle, RoundedCornerShape(20.dp))
            .border(1.dp, com.example.ui.components.LingoTokens.borderSubtle, RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "+$animatedXp XP",
              color = com.example.ui.components.LingoTokens.accentGold,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "XP Earned",
              color = com.example.ui.components.LingoTokens.textSecondary,
              fontSize = 12.sp
            )
          }
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(34.dp)
              .background(com.example.ui.components.LingoTokens.borderSubtle)
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$accuracyPercent%",
              color = com.example.ui.components.LingoTokens.brandCyan,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Accuracy",
              color = com.example.ui.components.LingoTokens.textSecondary,
              fontSize = 12.sp
            )
          }
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(34.dp)
              .background(com.example.ui.components.LingoTokens.borderSubtle)
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$newWordsCount",
              color = com.example.ui.components.LingoTokens.textPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "New Words",
              color = com.example.ui.components.LingoTokens.textSecondary,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        com.example.ui.components.LingoPrimaryGradientButton(
          text = "Next lesson",
          onClick = onClaimAndFinish,
          testTag = "claim_xp_button",
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        com.example.ui.components.LingoSecondaryChip(
          text = "Practice mistakes",
          onClick = onPracticeMistakes,
          testTag = "practice_mistakes_button",
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        )
      }
    }
  }
}
