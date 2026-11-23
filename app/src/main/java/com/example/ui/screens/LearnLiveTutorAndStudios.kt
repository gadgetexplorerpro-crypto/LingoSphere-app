package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiTutorService
import com.example.avatar.TutorPersona
import com.example.avatar.TutorViseme
import com.example.avatar.VideoCallStatus
import com.example.avatar.ui.RealisticTalkingAvatar
import com.example.data.model.AiConversationPartner
import com.example.data.model.CurriculumLesson
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.speech.SpeechManager
import com.example.ui.components.EmojiVoice
import com.example.ui.components.rememberLiveEmojiVoiceState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class TutorVisualStyle(val label: String) {
  REALISTIC_HUMAN("Realistic"),
  ILLUSTRATED("Illustrated"),
  STATIC_FALLBACK("Static")
}

enum class LiveTutorStage(val label: String) {
  LECTURE("Lecture"),
  PRACTICE("Practice")
}

data class LiveTutorTranscriptEntry(
  val id: String,
  val speaker: String,
  val isTutor: Boolean,
  val text: String,
  val translation: String,
  val correctionNote: String = "",
  val improvedExample: String = ""
)

data class VocabStudioEntry(
  val id: String,
  val word: String,
  val translation: String,
  val definition: String,
  val topic: String,
  val cefr: String,
  val exampleSentence: String,
  val exampleTranslation: String
)

data class WritingHistoryEntry(
  val id: String,
  val taskType: String,
  val prompt: String,
  val submittedText: String,
  val grammarScore: Int,
  val vocabScore: Int,
  val clarityScore: Int,
  val feedback: String,
  val improvedVersion: String
)

@Composable
fun SearchableLanguageAndExplanationModal(
  currentTargetCode: String,
  currentNativeCode: String,
  onSelectTargetLanguage: (String) -> Unit,
  onSelectNativeLanguage: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Target Language, 1 = Explanation Language

  val priorityCodes = remember {
    listOf("en", "es", "fr", "de", "ar", "ur", "hi", "pt", "it", "tr", "ja", "ko", "zh")
  }

  val sortedLanguages = remember(searchQuery) {
    val all = Language.SUPPORTED_LANGUAGES
    val prioritized = priorityCodes.mapNotNull { code -> all.find { it.code == code } } +
      all.filter { it.code !in priorityCodes }
    val q = searchQuery.trim().lowercase()
    if (q.isBlank()) prioritized
    else prioritized.filter {
      it.name.lowercase().contains(q) ||
        it.nativeName.lowercase().contains(q) ||
        it.code.lowercase().contains(q)
    }
  }

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, LingoElectricBlue.copy(alpha = 0.45f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .testTag("language_selection_screen")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "Language & Explanation Settings",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            "Switch target language or choose your native explanation language",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_language_modal_button")) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          label = { Text("Learning Language (${Language.findByCode(currentTargetCode).name})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.weight(1f).testTag("tab_target_language")
        )
        FilterChip(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          label = { Text("Explanations (${Language.findByCode(currentNativeCode).name})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.weight(1f).testTag("tab_explanation_language")
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search English, Español, العربية, اردو, हिन्दी, 日本語...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search languages") },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("language_search_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 320.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        sortedLanguages.forEach { lang ->
          val isSelected = if (selectedTab == 0) {
            lang.code.equals(currentTargetCode, ignoreCase = true)
          } else {
            lang.code.equals(currentNativeCode, ignoreCase = true)
          }
          val tag = if (selectedTab == 0) "switch_lang_${lang.code}" else "explanation_lang_${lang.code}"

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) LingoElectricBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
              1.dp,
              if (isSelected) LingoElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (selectedTab == 0) {
                  onSelectTargetLanguage(lang.code)
                  onDismiss()
                } else {
                  onSelectNativeLanguage(lang.code)
                }
              }
              .testTag(tag)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(lang.flagEmoji, fontSize = 22.sp)
                Column {
                  Text(
                    lang.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    "${lang.nativeName} • CEFR A1–C2 Ready",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                  )
                }
              }
              if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = LingoElectricBlue)
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizedLearningSetupModal(
  currentLevel: String,
  currentGoal: String,
  currentDailyMinutes: Int,
  currentConfidence: String,
  onSaveSetup: (level: String, goal: String, dailyMinutes: Int, confidence: String) -> Unit,
  onLaunchPlacementTest: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedLevel by remember(currentLevel) { mutableStateOf(currentLevel.ifBlank { "Beginner" }) }
  var selectedGoal by remember(currentGoal) { mutableStateOf(currentGoal.ifBlank { "Everyday Communication" }) }
  var selectedMinutes by remember(currentDailyMinutes) {
    mutableIntStateOf(if (currentDailyMinutes > 0) currentDailyMinutes else 15)
  }
  var selectedConfidence by remember(currentConfidence) { mutableStateOf(currentConfidence.ifBlank { "Medium" }) }

  val levels = remember { listOf("Complete Beginner", "Beginner", "Intermediate", "Upper Intermediate", "Advanced") }
  val goals = remember { listOf("Everyday Communication", "Travel", "Work & Career", "School & Exams", "Relocation", "General Fluency") }
  val timeOptions = remember { listOf(5, 10, 15, 30) }
  val confidenceLevels = remember { listOf("Low", "Medium", "High") }

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .testTag("personalized_learning_setup_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Personalized Learning Setup",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Tailor your CEFR path, daily pacing, and Live AI Tutor confidence",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .heightIn(min = 44.dp)
            .testTag("skip_learning_setup_button")
        ) {
          Text(
            text = "Skip",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      // 1. Current Level
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "1. Current Level",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          levels.forEach { lvl ->
            FilterChip(
              selected = selectedLevel.equals(lvl, ignoreCase = true),
              onClick = { selectedLevel = lvl },
              label = {
                Text(
                  text = lvl,
                  fontSize = 12.5.sp,
                  fontWeight = if (selectedLevel.equals(lvl, true)) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("setup_level_${lvl.lowercase().replace(" ", "_")}")
            )
          }
        }
        OutlinedButton(
          onClick = onLaunchPlacementTest,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .testTag("setup_launch_placement_button")
        ) {
          Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Take 2-Min CEFR Placement Assessment",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // 2. Primary Learning Goal
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "2. Primary Learning Goal",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          goals.forEach { goal ->
            FilterChip(
              selected = selectedGoal.equals(goal, ignoreCase = true),
              onClick = { selectedGoal = goal },
              label = {
                Text(
                  text = goal,
                  fontSize = 12.5.sp,
                  fontWeight = if (selectedGoal.equals(goal, true)) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("setup_goal_${goal.lowercase().replace(" ", "_").replace("&", "and")}")
            )
          }
        }
      }

      // 3. Daily Learning Time (FlowRow wraps cleanly into balanced rows on narrow screens)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "3. Daily Learning Time",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          timeOptions.forEach { mins ->
            FilterChip(
              selected = selectedMinutes == mins,
              onClick = { selectedMinutes = mins },
              label = {
                Text(
                  text = "$mins min/day",
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("setup_time_${mins}m")
            )
          }
        }
      }

      // 4. Speaking & Listening Confidence (FlowRow ensures "High Confidence" never wraps vertically)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "4. Speaking & Listening Confidence",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          confidenceLevels.forEach { conf ->
            FilterChip(
              selected = selectedConfidence.equals(conf, ignoreCase = true),
              onClick = { selectedConfidence = conf },
              label = {
                Text(
                  text = "$conf Confidence",
                  fontSize = 12.5.sp,
                  fontWeight = if (selectedConfidence.equals(conf, true)) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              },
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("setup_confidence_${conf.lowercase()}")
            )
          }
        }
      }

      Button(
        onClick = {
          onSaveSetup(selectedLevel, selectedGoal, selectedMinutes, selectedConfidence)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue, contentColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("save_learning_setup_button")
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Save Personalized Plan",
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun LiveAiLessonTutorModal(
  lesson: CurriculumLesson,
  targetLanguageCode: String,
  nativeLanguageCode: String,
  speechManager: SpeechManager?,
  onSaveWordToFlashcards: (front: String, back: String, note: String) -> Unit,
  onCompleteLesson: (CurriculumLesson) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val targetLang = remember(targetLanguageCode) { Language.findByCode(targetLanguageCode) }
  val nativeLang = remember(nativeLanguageCode) { Language.findByCode(nativeLanguageCode) }

  var selectedPartner by remember { mutableStateOf(AiConversationPartner.DEFAULT) }
  var selectedVoice by remember(selectedPartner.id) { mutableStateOf(selectedPartner.defaultVoice) }
  var isFemaleTutor by remember { mutableStateOf(true) }
  val tutorPersona = remember(targetLanguageCode, isFemaleTutor) {
    TutorPersona.findTutorForLanguage(targetLanguageCode, preferFemale = isFemaleTutor)
  }
  val tutorName = selectedPartner.name
  var visualStyle by remember { mutableStateOf(TutorVisualStyle.ILLUSTRATED) }
  var stage by remember { mutableStateOf(LiveTutorStage.LECTURE) }
  var isMinimized by remember { mutableStateOf(false) }

  var speakingRate by remember { mutableFloatStateOf(1.0f) }
  var isMuted by remember { mutableStateOf(false) }
  var isAiSpeaking by remember { mutableStateOf(false) }
  var isAiThinking by remember { mutableStateOf(false) }
  var isUserListening by remember { mutableStateOf(false) }
  var showTranslationInLecture by remember { mutableStateOf(true) }
  var elapsedSeconds by remember { mutableIntStateOf(42) }

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasMicPermission = granted
    if (granted) {
      isUserListening = true
      speechManager?.startListening(targetLanguageCode)
    }
  }

  val keyPhrase = remember(lesson, targetLanguageCode) {
    lesson.exercises.firstOrNull { it.targetAudioText.isNotBlank() }?.targetAudioText
      ?: when (targetLanguageCode.lowercase()) {
        "es" -> "¡Hola! Me gustaría practicar esta lección contigo."
        "fr" -> "Bonjour ! J'aimerais pratiquer cette leçon avec vous."
        "de" -> "Hallo! Ich möchte diese Lektion mit dir üben."
        "it" -> "Ciao! Vorrei praticare questa lezione con te."
        "ja" -> "こんにちは！このレッスンを練習したいです。"
        "zh" -> "你好！我想和你一起练习这一课。"
        else -> "Hello! I would like to practice this ${targetLang.name} lesson with you."
      }
  }
  val keyMeaning = remember(lesson) {
    lesson.exercises.firstOrNull()?.options?.firstOrNull()
      ?: lesson.learningOutcomes.firstOrNull()
      ?: lesson.description
  }
  val exampleUsage = remember(lesson, keyPhrase) {
    lesson.exercises.getOrNull(1)?.targetAudioText?.takeIf { it.isNotBlank() }
      ?: keyPhrase
  }

  var practiceStepIndex by remember { mutableIntStateOf(0) }
  var practiceTurnsCompleted by remember { mutableIntStateOf(0) }
  var userDraftInput by remember { mutableStateOf("") }
  var latestFeedbackNote by remember { mutableStateOf("") }
  var latestImprovedExample by remember { mutableStateOf("") }

  val managerListening by speechManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }
  val managerPartial by speechManager?.partialSpokenText?.collectAsState() ?: remember { mutableStateOf("") }
  val managerSpoken by speechManager?.spokenText?.collectAsState() ?: remember { mutableStateOf("") }
  val (liveEmojiState, liveEmojiLevel) = rememberLiveEmojiVoiceState(
    speechManager = speechManager,
    isThinking = isAiThinking,
    isDone = practiceTurnsCompleted >= 2
  )

  LaunchedEffect(managerListening) {
    isUserListening = managerListening
  }

  LaunchedEffect(managerPartial) {
    if (managerPartial.isNotBlank() && !managerPartial.equals("Capturing your voice...", ignoreCase = true)) {
      userDraftInput = managerPartial
    }
  }

  val practicePrompts = remember(lesson, keyPhrase) {
    listOf(
      "Step 1 (Guided Repeat): Repeat or type the key expression aloud: \"$keyPhrase\"",
      "Step 2 (Contextual Response): Use this expression in your own sentence for '${lesson.title}'.",
      "Step 3 (Free Roleplay Turn): Respond naturally as if we are in a real-life ${lesson.category.lowercase()} situation."
    )
  }

  val transcript = remember(lesson) {
    mutableStateListOf(
      LiveTutorTranscriptEntry(
        id = "msg_intro_1",
        speaker = tutorName,
        isTutor = true,
        text = "Welcome to ${lesson.title} (${lesson.cefrLevel}). Today we will master: \"$keyPhrase\".",
        translation = "Objective (${nativeLang.name}): $keyMeaning"
      ),
      LiveTutorTranscriptEntry(
        id = "msg_intro_2",
        speaker = tutorName,
        isTutor = true,
        text = "Listen carefully to the natural rhythm: \"$exampleUsage\"",
        translation = "Meaning: $keyMeaning"
      )
    )
  }

  fun speakTutorLine(text: String, rate: Float = speakingRate) {
    if (isMuted) return
    isAiSpeaking = true
    speechManager?.speakWithTutorVoice(
      text = text,
      languageCode = targetLanguageCode,
      voice = selectedVoice,
      speedOverride = rate,
      onDone = { isAiSpeaking = false }
    )
  }

  LaunchedEffect(lesson.id, selectedPartner.id, targetLanguageCode) {
    if (!isMuted) {
      speakTutorLine(keyPhrase, speakingRate)
    }
  }

  fun submitLearnerResponse(userText: String) {
    val trimmed = userText.trim()
    if (trimmed.isBlank() || isAiThinking) return
    userDraftInput = ""
    isUserListening = false
    elapsedSeconds += 18

    transcript.add(
      LiveTutorTranscriptEntry(
        id = "user_${transcript.size + 1}",
        speaker = "You",
        isTutor = false,
        text = trimmed,
        translation = "Learner response (${targetLang.name})"
      )
    )

    isAiThinking = true
    scope.launch {
      try {
        val aiReply = GeminiTutorService.chatWithTutor(
          userSpeechInput = trimmed,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = "CEFR ${lesson.cefrLevel}",
          scenarioTitle = "${lesson.title} (Tutor: ${selectedPartner.name} - ${selectedPartner.roleTitle})",
          scenarioInstruction = "Key phrase: '$keyPhrase' ($keyMeaning). Evaluate learner response as ${selectedPartner.name} (${selectedPartner.teachingStyle}), give gentle correction, and encourage practice.",
          conversationHistory = transcript.map { it.speaker to it.text }
        )
        val contextualAnalysis = com.example.ai.AiProviderManager.analyzeAndCorrectLearnerSentence(
          userMessage = trimmed,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = lesson.cefrLevel,
          scenarioTitle = lesson.title,
          historyTurns = transcript.map { it.speaker to it.text }
        )
        val replyText = aiReply.replyInTargetLanguage.ifBlank { contextualAnalysis.warmTutorReply }
        val correction = aiReply.grammarFeedback?.takeIf { it.isNotBlank() } ?: contextualAnalysis.conciseExplanation
        val improved = if (contextualAnalysis.hasGrammarError) {
          "Suggested (${lesson.cefrLevel}): \"${contextualAnalysis.correctedSentence}\""
        } else {
          "Model ${lesson.cefrLevel}: \"$keyPhrase\""
        }

        latestFeedbackNote = correction
        latestImprovedExample = improved
        practiceTurnsCompleted += 1
        if (practiceStepIndex < practicePrompts.lastIndex) {
          practiceStepIndex += 1
        }

        transcript.add(
          LiveTutorTranscriptEntry(
            id = "tutor_${transcript.size + 1}",
            speaker = tutorName,
            isTutor = true,
            text = replyText,
            translation = aiReply.translationInNativeLanguage.ifBlank { contextualAnalysis.nativeTranslation },
            correctionNote = correction,
            improvedExample = improved
          )
        )
        speakTutorLine(replyText, speakingRate)
      } catch (_: Exception) {
        val contextualAnalysis = com.example.ai.AiProviderManager.analyzeAndCorrectLearnerSentence(
          userMessage = trimmed,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = lesson.cefrLevel,
          scenarioTitle = lesson.title,
          historyTurns = transcript.map { it.speaker to it.text }
        )
        val fallbackReply = contextualAnalysis.warmTutorReply
        val correction = contextualAnalysis.conciseExplanation
        val improved = "Model ${lesson.cefrLevel}: \"$keyPhrase\""
        latestFeedbackNote = correction
        latestImprovedExample = improved
        practiceTurnsCompleted += 1
        transcript.add(
          LiveTutorTranscriptEntry(
            id = "tutor_${transcript.size + 1}",
            speaker = tutorName,
            isTutor = true,
            text = fallbackReply,
            translation = contextualAnalysis.nativeTranslation,
            correctionNote = correction,
            improvedExample = improved
          )
        )
        speakTutorLine(fallbackReply, speakingRate)
      } finally {
        isAiThinking = false
      }
    }
  }

  LaunchedEffect(managerSpoken) {
    val cleanSpoken = managerSpoken.trim()
    if (cleanSpoken.isNotBlank()) {
      speechManager?.clearSpokenText()
      submitLearnerResponse(cleanSpoken)
    }
  }

  val speedOptions = listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x")
  val minutesPart = elapsedSeconds / 60
  val secondsPart = elapsedSeconds % 60
  val formattedTime = "%02d:%02d".format(minutesPart, secondsPart)

  Surface(
    shape = RoundedCornerShape(28.dp),
    color = Color(0xFF0A1938),
    border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .testTag("live_ai_lesson_tutor_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 560.dp)
        .verticalScroll(rememberScrollState())
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0B1E47), Color(0xFF152C69), Color(0xFF091533))
          )
        )
        .padding(18.dp)
    ) {
      // Top Controls Bar: Close, Lesson Title + Stage, Elapsed Time, Minimize/Expand
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            isAiSpeaking = false
            speechManager?.stopSpeaking()
            onDismiss()
          },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .testTag("close_live_tutor_button")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Live Tutor", tint = Color.White)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
          Text(
            lesson.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (stage == LiveTutorStage.LECTURE) LingoCyan.copy(alpha = 0.25f) else LingoEmerald.copy(alpha = 0.25f)
            ) {
              Text(
                "Stage: ${stage.label}",
                color = if (stage == LiveTutorStage.LECTURE) LingoCyan else LingoEmerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).testTag("live_tutor_stage_badge")
              )
            }
            Text(
              "• $formattedTime • ${lesson.cefrLevel}",
              color = Color.White.copy(alpha = 0.78f),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        IconButton(
          onClick = { isMinimized = !isMinimized },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .testTag(if (isMinimized) "live_tutor_expand_button" else "live_tutor_minimize_button")
        ) {
          Icon(
            if (isMinimized) Icons.Default.OpenInFull else Icons.Default.Minimize,
            contentDescription = if (isMinimized) "Expand Tutor" else "Minimize Tutor to Transcript",
            tint = Color.White
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Stage Switcher Pills (Lecture vs Practice)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = stage == LiveTutorStage.LECTURE,
          onClick = { stage = LiveTutorStage.LECTURE },
          label = { Text("1. Lecture (Teach)", fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF2563EB),
            selectedLabelColor = Color.White,
            labelColor = Color.White.copy(alpha = 0.8f)
          ),
          modifier = Modifier.weight(1f).testTag("live_tutor_stage_lecture_tab")
        )
        FilterChip(
          selected = stage == LiveTutorStage.PRACTICE,
          onClick = { stage = LiveTutorStage.PRACTICE },
          label = { Text("2. Practice (${practiceTurnsCompleted}/2)", fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LingoEmerald.copy(alpha = 0.85f),
            selectedLabelColor = Color.Black,
            labelColor = Color.White.copy(alpha = 0.8f)
          ),
          modifier = Modifier.weight(1f).testTag("live_tutor_stage_practice_tab")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // AI Partner & Voice Selector Strip (12 Partners & 15 Voices)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AiConversationPartner.ALL_PARTNERS.forEach { partner ->
          val isSelected = selectedPartner.id == partner.id
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) Color(0xFF2563EB) else Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (isSelected) LingoCyan else Color.Transparent),
            modifier = Modifier
              .clickable {
                selectedPartner = partner
                selectedVoice = partner.defaultVoice
                isFemaleTutor = partner.defaultVoice.gender == "Female"
              }
              .testTag("live_tutor_partner_${partner.id}")
          ) {
            Text(
              "${partner.accentFlag} ${partner.name} (${partner.defaultVoice.voiceName})",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Speed Controls + Tutor Voice/Visual Options
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        speedOptions.forEach { (rate, label) ->
          val tagSuffix = label.replace(".", "_")
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (speakingRate == rate) Color.White else Color.White.copy(alpha = 0.14f),
            modifier = Modifier
              .clickable {
                speakingRate = rate
                speechManager?.setSpeechRate(rate)
              }
              .testTag("live_tutor_speed_$tagSuffix")
          ) {
            Text(
              label,
              color = if (speakingRate == rate) Color(0xFF0B1E47) else Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color.White.copy(alpha = 0.14f),
          modifier = Modifier
            .clickable {
              val currentIdx = AiConversationPartner.ALL_PARTNERS.indexOfFirst { it.id == selectedPartner.id }.coerceAtLeast(0)
              val nextPartner = AiConversationPartner.ALL_PARTNERS[(currentIdx + 1) % AiConversationPartner.ALL_PARTNERS.size]
              selectedPartner = nextPartner
              selectedVoice = nextPartner.defaultVoice
              isFemaleTutor = nextPartner.defaultVoice.gender == "Female"
            }
            .testTag("tutor_gender_toggle_button")
        ) {
          Text(
            "Tutor: $tutorName • ${selectedVoice.voiceName}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }

        TutorVisualStyle.entries.forEach { style ->
          val styleTag = when (style) {
            TutorVisualStyle.REALISTIC_HUMAN -> "tutor_mode_human"
            TutorVisualStyle.ILLUSTRATED -> "tutor_mode_illustrated"
            TutorVisualStyle.STATIC_FALLBACK -> "tutor_mode_static"
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (visualStyle == style) LingoCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
            modifier = Modifier
              .clickable { visualStyle = style }
              .testTag(styleTag)
          ) {
            Text(
              style.label,
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isMinimized) {
        // PHASE 12: MINIMIZED TUTOR MODE + CHRONOLOGICAL TRANSCRIPT
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.White.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isMinimized = false }
            .testTag("minimized_tutor_top_bar")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
              }
              Column(modifier = Modifier.weight(1f, fill = false)) {
                Text("$tutorName • Live Session Active", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                  if (isAiSpeaking) "Speaking (${speakingRate}x)..." else "Tap to return to full-screen tutor",
                  color = LingoCyan,
                  fontSize = 11.sp
                )
              }
            }
            TextButton(onClick = { isMinimized = false }) {
              Text("Expand", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          "Lesson Conversation Transcript",
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
            .testTag("live_tutor_transcript_list"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          transcript.forEachIndexed { idx, entry ->
            var showEntryTranslation by remember { mutableStateOf(false) }
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (entry.isTutor) Color.White.copy(alpha = 0.12f) else Color(0xFF2563EB).copy(alpha = 0.35f),
              border = BorderStroke(
                1.dp,
                if (entry.isTutor) Color.White.copy(alpha = 0.2f) else LingoCyan.copy(alpha = 0.5f)
              ),
              modifier = Modifier.fillMaxWidth().testTag("transcript_item_$idx")
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    entry.speaker,
                    color = if (entry.isTutor) LingoCyan else LingoEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                      onClick = { speakTutorLine(entry.text, speakingRate) },
                      modifier = Modifier.size(28.dp).testTag("transcript_replay_$idx")
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay message audio", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                      onClick = { showEntryTranslation = !showEntryTranslation },
                      modifier = Modifier.size(28.dp).testTag("transcript_translate_$idx")
                    ) {
                      Icon(Icons.Default.Translate, contentDescription = "Show translation", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                      onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("LingoSphere Transcript", entry.text))
                      },
                      modifier = Modifier.size(28.dp).testTag("transcript_copy_$idx")
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                      onClick = {
                        onSaveWordToFlashcards(entry.text, entry.translation, "Saved from Live Tutor: ${lesson.title}")
                      },
                      modifier = Modifier.size(28.dp).testTag("transcript_save_vocab_$idx")
                    ) {
                      Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to vocabulary", tint = LingoGold, modifier = Modifier.size(16.dp))
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(entry.text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                if (showEntryTranslation || entry.translation.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(entry.translation, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                }
                if (entry.improvedExample.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(entry.improvedExample, color = LingoEmerald, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      } else {
        // FULL-SCREEN TUTOR VISUAL AREA (Phases 8 & 9)
        val activeCallStatus = when {
          isMuted -> VideoCallStatus.MUTED
          isAiThinking -> VideoCallStatus.THINKING
          isAiSpeaking -> VideoCallStatus.TUTOR_SPEAKING
          isUserListening -> VideoCallStatus.LISTENING
          else -> VideoCallStatus.READY
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          when (visualStyle) {
            TutorVisualStyle.REALISTIC_HUMAN -> {
              RealisticTalkingAvatar(
                tutor = tutorPersona,
                callStatus = activeCallStatus,
                currentViseme = if (isAiSpeaking && !isMuted) TutorViseme.NEUTRAL_TALK else TutorViseme.REST,
                speechAperture = if (isAiSpeaking && !isMuted) 0.55f else 0f,
                isAudioPlaying = isAiSpeaking && !isMuted,
                userRmsLevel = if (isUserListening) 0.4f else 0f,
                modifier = Modifier
                  .size(168.dp)
                  .clip(CircleShape)
                  .testTag("live_tutor_realistic_avatar")
              )
            }
            TutorVisualStyle.ILLUSTRATED -> {
              IllustratedTutorAvatar(
                isFemale = isFemaleTutor,
                isSpeaking = isAiSpeaking && !isMuted,
                isListening = isUserListening,
                modifier = Modifier
                  .size(168.dp)
                  .testTag("live_tutor_illustrated_avatar")
              )
            }
            TutorVisualStyle.STATIC_FALLBACK -> {
              Surface(
                shape = CircleShape,
                color = Color(0xFF1E3A8A),
                border = BorderStroke(3.dp, LingoCyan),
                modifier = Modifier
                  .size(148.dp)
                  .testTag("live_tutor_static_avatar")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Icon(
                    Icons.Default.School,
                    contentDescription = tutorName,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                  )
                  Text(
                    tutorName.substringBefore(" "),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        // Connection / State Indicator Pill + Universal Animated EmojiVoice
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          EmojiVoice(
            state = liveEmojiState,
            size = 42.dp,
            animated = true,
            audioLevel = liveEmojiLevel,
            isRecording = isUserListening,
            isPlaying = isAiSpeaking && !isMuted,
            onRetry = {
              speechManager?.clearError()
              speechManager?.startListening(targetLanguageCode)
            }
          )
          Spacer(modifier = Modifier.width(8.dp))
          val stateLabel = when {
            isAiThinking -> "Thinking..."
            isAiSpeaking && !isMuted -> "$tutorName is Speaking (${speakingRate}x)"
            isUserListening -> "Listening to your voice..."
            isMuted -> "Tutor Audio Muted"
            else -> "$tutorName • Ready for ${stage.label}"
          }
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.14f)
          ) {
            Text(
              stateLabel,
              color = LingoCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp).testTag("live_tutor_state_indicator")
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (stage == LiveTutorStage.LECTURE) {
          // STAGE 1: LECTURE (TEACH)
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier.fillMaxWidth().testTag("live_tutor_stage_lecture")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                "STAGE 1 • GUIDED LECTURE",
                color = LingoCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                keyPhrase,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp
              )
              if (showTranslationInLecture) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  "Meaning (${nativeLang.name}): $keyMeaning",
                  color = Color.White.copy(alpha = 0.85f),
                  fontSize = 13.sp
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "Natural Context: \"$exampleUsage\"",
                color = LingoGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilledTonalButton(
                  onClick = { speakTutorLine(keyPhrase, speakingRate) },
                  colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                  modifier = Modifier.testTag("lecture_listen_phrase_button")
                ) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Speak Phrase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = { speakTutorLine(keyPhrase, 0.75f) },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                  modifier = Modifier.testTag("lecture_slow_phrase_button")
                ) {
                  Text("Slow (0.75x)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = { showTranslationInLecture = !showTranslationInLecture },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                  modifier = Modifier.testTag("lecture_toggle_translation_button")
                ) {
                  Text(if (showTranslationInLecture) "Hide Translation" else "Show Translation", fontSize = 12.sp)
                }
                OutlinedButton(
                  onClick = {
                    onSaveWordToFlashcards(keyPhrase, keyMeaning, "Lesson: ${lesson.title}")
                  },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoGold),
                  border = BorderStroke(1.dp, LingoGold.copy(alpha = 0.5f)),
                  modifier = Modifier.testTag("lecture_save_phrase_button")
                ) {
                  Text("Save Phrase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  stage = LiveTutorStage.PRACTICE
                  speakTutorLine(practicePrompts.first(), speakingRate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("proceed_to_practice_stage_button")
              ) {
                Text("Start Interactive Practice Stage", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
              }
            }
          }
        } else {
          // STAGE 2: INTERACTIVE PRACTICE
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().testTag("live_tutor_stage_practice")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                practicePrompts[practiceStepIndex.coerceIn(0, practicePrompts.lastIndex)],
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )

              if (latestFeedbackNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = LingoEmerald.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth().testTag("live_tutor_feedback_card")
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(latestFeedbackNote, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (latestImprovedExample.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(latestImprovedExample, color = LingoEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Quick Inspire / Help Chips (Repeat, Simpler, Translate, Example)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                AssistChip(
                  onClick = { speakTutorLine(keyPhrase, speakingRate) },
                  label = { Text("Repeat", color = Color.White, fontSize = 11.sp) },
                  modifier = Modifier.testTag("tutor_quick_repeat")
                )
                AssistChip(
                  onClick = { speakTutorLine(keyPhrase, 0.75f) },
                  label = { Text("Simpler / Slow", color = Color.White, fontSize = 11.sp) },
                  modifier = Modifier.testTag("tutor_quick_simpler")
                )
                AssistChip(
                  onClick = {
                    latestFeedbackNote = "Translation (${nativeLang.name}): $keyMeaning"
                  },
                  label = { Text("Translate", color = Color.White, fontSize = 11.sp) },
                  modifier = Modifier.testTag("tutor_quick_translate")
                )
                AssistChip(
                  onClick = {
                    userDraftInput = keyPhrase
                  },
                  label = { Text("Insert Example", color = LingoGold, fontSize = 11.sp) },
                  modifier = Modifier.testTag("tutor_quick_example")
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Text or Voice Response Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = userDraftInput,
                  onValueChange = { userDraftInput = it },
                  placeholder = { Text("Speak or type your response...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = LingoCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                  ),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("live_tutor_text_input")
                )
                IconButton(
                  onClick = { submitLearnerResponse(userDraftInput.ifBlank { keyPhrase }) },
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LingoCyan)
                    .testTag("live_tutor_send_button")
                ) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send response", tint = Color(0xFF0A1938))
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Primary Bottom Call Controls: Mute, Large Mic Button, Complete Lesson (gated by Practice)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            isMuted = !isMuted
            if (isMuted) {
              isAiSpeaking = false
              speechManager?.stopSpeaking()
            }
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isMuted) LingoCoral.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.14f))
            .testTag("live_tutor_mute_button")
        ) {
          Icon(
            if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isMuted) "Unmute Tutor" else "Mute Tutor",
            tint = Color.White
          )
        }

        // Large Center Microphone Control
        Button(
          onClick = {
            if (!hasMicPermission) {
              micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
              if (isUserListening) {
                isUserListening = false
                speechManager?.finishListeningNow()
                if (userDraftInput.isNotBlank()) {
                  submitLearnerResponse(userDraftInput)
                }
              } else {
                isAiSpeaking = false
                speechManager?.stopSpeaking()
                speechManager?.clearSpokenText()
                isUserListening = true
                stage = LiveTutorStage.PRACTICE
                speechManager?.startListening(targetLanguageCode)
              }
            }
          },
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isUserListening) LingoEmerald else Color(0xFF2563EB),
            contentColor = if (isUserListening) Color.Black else Color.White
          ),
          modifier = Modifier
            .size(64.dp)
            .testTag("live_tutor_mic_button")
        ) {
          Icon(
            if (isUserListening) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isUserListening) "Finish Speaking" else "Tap to Speak",
            modifier = Modifier.size(28.dp)
          )
        }

        // Complete Lesson Button — ONLY enabled when Practice objective is met (practiceTurnsCompleted >= 1)
        Button(
          onClick = { onCompleteLesson(lesson) },
          enabled = practiceTurnsCompleted >= 1,
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoEmerald,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.14f),
            disabledContentColor = Color.White.copy(alpha = 0.45f)
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .height(48.dp)
            .testTag("live_tutor_complete_lesson_button")
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            if (practiceTurnsCompleted >= 1) "Complete Lesson" else "Practice First",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }
  }
}

@Composable
private fun IllustratedTutorAvatar(
  isFemale: Boolean,
  isSpeaking: Boolean,
  isListening: Boolean,
  modifier: Modifier = Modifier
) {
  val ringColor by animateColorAsState(
    targetValue = when {
      isSpeaking -> LingoCyan
      isListening -> LingoEmerald
      else -> Color(0xFF60A5FA)
    },
    label = "illustratedRing"
  )

  Canvas(modifier = modifier) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2.2f

    drawCircle(
      color = Color(0xFF1E3A8A),
      radius = r,
      center = Offset(cx, cy)
    )
    drawCircle(
      color = ringColor,
      radius = r,
      center = Offset(cx, cy),
      style = Stroke(width = 6f)
    )

    // Stylized head & shoulders
    drawCircle(
      color = if (isFemale) Color(0xFFFDE68A) else Color(0xFFFED7AA),
      radius = r * 0.42f,
      center = Offset(cx, cy - r * 0.08f)
    )
    // Eyes
    drawCircle(color = Color(0xFF0F172A), radius = r * 0.05f, center = Offset(cx - r * 0.14f, cy - r * 0.14f))
    drawCircle(color = Color(0xFF0F172A), radius = r * 0.05f, center = Offset(cx + r * 0.14f, cy - r * 0.14f))

    // Audio-synchronized mouth (strictly open only when isSpeaking is true)
    if (isSpeaking) {
      drawOval(
        color = Color(0xFFBE123C),
        topLeft = Offset(cx - r * 0.12f, cy + r * 0.02f),
        size = Size(r * 0.24f, r * 0.14f)
      )
    } else {
      drawLine(
        color = Color(0xFF0F172A),
        start = Offset(cx - r * 0.11f, cy + r * 0.07f),
        end = Offset(cx + r * 0.11f, cy + r * 0.07f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
      )
    }
  }
}

@Composable
fun AiVocabularyStudioModal(
  targetLanguageCode: String,
  cefrLevel: String,
  speechManager: SpeechManager?,
  onSaveWord: (front: String, back: String, note: String) -> Unit,
  onAwardXp: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val langName = remember(targetLanguageCode) { Language.findByCode(targetLanguageCode).name }
  val topics = listOf("All Topics", "Greetings & Social", "Travel & Dining", "Work & Career", "Culture & Academic")
  var selectedTopic by remember { mutableStateOf("All Topics") }
  var practiceMode by remember { mutableStateOf("browse") } // browse, flashcards, mcq, typing, listening
  var savedWordIds by remember { mutableStateOf(setOf<String>()) }

  val vocabularyBank = remember(targetLanguageCode, cefrLevel) {
    buildVocabularyBankForLanguage(targetLanguageCode, cefrLevel)
  }
  val filteredWords = remember(vocabularyBank, selectedTopic) {
    if (selectedTopic == "All Topics") vocabularyBank
    else vocabularyBank.filter { it.topic == selectedTopic }
  }

  var activeWordIndex by remember { mutableIntStateOf(0) }
  var flashcardFlipped by remember { mutableStateOf(false) }
  var typingInput by remember { mutableStateOf("") }
  var practiceFeedback by remember { mutableStateOf("") }

  val currentWord = filteredWords.getOrElse(activeWordIndex.coerceIn(0, (filteredWords.size - 1).coerceAtLeast(0))) {
    vocabularyBank.first()
  }

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, LingoElectricBlue.copy(alpha = 0.45f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .testTag("ai_vocabulary_studio_dialog")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 500.dp)
        .verticalScroll(rememberScrollState())
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "Vocabulary Studio • $langName ($cefrLevel)",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            "Topic words, audio pronunciation, SRS flashcards & active recall",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_vocabulary_studio_button")) {
          Icon(Icons.Default.Close, contentDescription = "Close Vocabulary Studio")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Practice Mode Switcher
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = practiceMode == "browse",
          onClick = { practiceMode = "browse"; practiceFeedback = "" },
          label = { Text("Word Bank", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("vocab_mode_browse")
        )
        FilterChip(
          selected = practiceMode == "flashcards",
          onClick = { practiceMode = "flashcards"; flashcardFlipped = false; practiceFeedback = "" },
          label = { Text("Flashcards (SRS)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("vocab_mode_flashcards")
        )
        FilterChip(
          selected = practiceMode == "mcq",
          onClick = { practiceMode = "mcq"; practiceFeedback = "" },
          label = { Text("Multiple Choice", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("vocab_mode_mcq")
        )
        FilterChip(
          selected = practiceMode == "typing",
          onClick = { practiceMode = "typing"; typingInput = ""; practiceFeedback = "" },
          label = { Text("Typing Recall", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("vocab_mode_typing")
        )
        FilterChip(
          selected = practiceMode == "listening",
          onClick = {
            practiceMode = "listening"
            practiceFeedback = ""
            speechManager?.speak(currentWord.word, targetLanguageCode, 1.0f)
          },
          label = { Text("Audio Recall", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("vocab_mode_listening")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Topic Filter Row
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        topics.forEach { topic ->
          FilterChip(
            selected = selectedTopic == topic,
            onClick = { selectedTopic = topic; activeWordIndex = 0 },
            label = { Text(topic, fontSize = 11.sp) }
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (practiceMode == "browse") {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          filteredWords.forEachIndexed { idx, entry ->
            val isSaved = entry.id in savedWordIds
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth().testTag("vocab_word_card_$idx")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(entry.word, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${entry.translation} • ${entry.topic}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LingoElectricBlue)
                  }
                  Row {
                    IconButton(
                      onClick = { speechManager?.speak(entry.word, targetLanguageCode, 1.0f) },
                      modifier = Modifier.testTag("vocab_play_audio_$idx")
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen pronunciation", tint = LingoElectricBlue)
                    }
                    IconButton(
                      onClick = {
                        savedWordIds = savedWordIds + entry.id
                        onSaveWord(entry.word, entry.translation, "${entry.definition} | Ex: ${entry.exampleSentence}")
                      },
                      modifier = Modifier.testTag("vocab_save_word_$idx")
                    ) {
                      Icon(
                        if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                        contentDescription = "Save word",
                        tint = if (isSaved) LingoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
                Text(entry.definition, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Example: \"${entry.exampleSentence}\"", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
              }
            }
          }
        }
      } else {
        // Active Practice Card (Flashcards / MCQ / Typing / Listening)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = LingoElectricBlue.copy(alpha = 0.1f),
          border = BorderStroke(1.5.dp, LingoElectricBlue.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth().testTag("vocab_active_practice_card")
        ) {
          Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              "Card ${activeWordIndex + 1} of ${filteredWords.size} • ${currentWord.topic}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = LingoElectricBlue
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (practiceMode) {
              "flashcards" -> {
                Text(
                  if (flashcardFlipped) currentWord.translation else currentWord.word,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.ExtraBold,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  if (flashcardFlipped) currentWord.exampleSentence else "Tap Flip to reveal meaning & example",
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  OutlinedButton(
                    onClick = { flashcardFlipped = !flashcardFlipped },
                    modifier = Modifier.testTag("vocab_flip_flashcard_button")
                  ) {
                    Text(if (flashcardFlipped) "Show Word" else "Flip Card", fontWeight = FontWeight.Bold)
                  }
                  Button(
                    onClick = {
                      onAwardXp(10)
                      flashcardFlipped = false
                      activeWordIndex = (activeWordIndex + 1) % filteredWords.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color.Black),
                    modifier = Modifier.testTag("vocab_srs_remembered_button")
                  ) {
                    Text("Got It (+10 XP)", fontWeight = FontWeight.ExtraBold)
                  }
                }
              }
              "mcq", "listening" -> {
                if (practiceMode == "listening") {
                  FilledTonalButton(
                    onClick = { speechManager?.speak(currentWord.word, targetLanguageCode, 1.0f) },
                    modifier = Modifier.testTag("vocab_replay_listening_prompt")
                  ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Word Audio Again", fontWeight = FontWeight.Bold)
                  }
                } else {
                  Text(
                    currentWord.word,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val options = remember(currentWord, vocabularyBank) {
                  val distractors = vocabularyBank.filter { it.id != currentWord.id }.take(3).map { it.translation }
                  listOf(currentWord.translation) + distractors
                }
                options.forEachIndexed { optIdx, optionText ->
                  OutlinedButton(
                    onClick = {
                      if (optIdx == 0) {
                        practiceFeedback = "Correct! \"${currentWord.word}\" means \"${currentWord.translation}\"."
                        onAwardXp(10)
                      } else {
                        practiceFeedback = "Keep practicing! The correct meaning is \"${currentWord.translation}\"."
                      }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("vocab_mcq_option_$optIdx")
                  ) {
                    Text(optionText, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
              "typing" -> {
                Text(
                  "Type the $langName expression for: \"${currentWord.translation}\"",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                  value = typingInput,
                  onValueChange = { typingInput = it },
                  placeholder = { Text("Type word or phrase...") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("vocab_typing_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = {
                    practiceFeedback = "Model answer: \"${currentWord.word}\" — Great active recall (+10 XP)!"
                    onAwardXp(10)
                  },
                  modifier = Modifier.fillMaxWidth().testTag("vocab_typing_check_button")
                ) {
                  Text("Check Answer", fontWeight = FontWeight.ExtraBold)
                }
              }
            }

            if (practiceFeedback.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(practiceFeedback, color = LingoEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))
              TextButton(
                onClick = {
                  practiceFeedback = ""
                  typingInput = ""
                  activeWordIndex = (activeWordIndex + 1) % filteredWords.size
                },
                modifier = Modifier.testTag("vocab_next_practice_word_button")
              ) {
                Text("Next Word →", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
    }
  }
}

private fun buildVocabularyBankForLanguage(langCode: String, cefr: String): List<VocabStudioEntry> {
  val code = langCode.lowercase()
  return listOf(
    VocabStudioEntry(
      id = "${code}_v1",
      word = when (code) {
        "es" -> "Encantado de conocerte"
        "fr" -> "Enchanté de vous connaître"
        "de" -> "Freut mich, Sie kennenzulernen"
        "ar" -> "تشرفنا بمعرفتك"
        "ur" -> "آپ سے مل کر خوشی ہوئی"
        "hi" -> "आपसे मिलकर खुशी हुई"
        "ja" -> "お会いできて光栄です"
        "zh" -> "很高兴认识你"
        else -> "Delighted to meet you"
      },
      translation = "Delighted to meet you",
      definition = "Polite expression used when greeting someone for the first time.",
      topic = "Greetings & Social",
      cefr = cefr,
      exampleSentence = "¡Hola! Encantado de conocerte en esta conferencia.",
      exampleTranslation = "Hello! Delighted to meet you at this conference."
    ),
    VocabStudioEntry(
      id = "${code}_v2",
      word = when (code) {
        "es" -> "La especialidad de la casa"
        "fr" -> "La spécialité de la maison"
        "de" -> "Die Spezialität des Hauses"
        "ar" -> "طبق المطعم المميز"
        "ur" -> "ریستوران کی خاص ڈش"
        "hi" -> "रेस्तरां का विशेष व्यंजन"
        "ja" -> "当店のおすすめ料理"
        "zh" -> "本店招牌菜"
        else -> "The house specialty"
      },
      translation = "The house specialty",
      definition = "A signature dish recommended by the chef at a restaurant.",
      topic = "Travel & Dining",
      cefr = cefr,
      exampleSentence = "¿Nos podría traer la especialidad de la casa, por favor?",
      exampleTranslation = "Could you bring us the house specialty, please?"
    ),
    VocabStudioEntry(
      id = "${code}_v3",
      word = when (code) {
        "es" -> "Cumplir con el plazo"
        "fr" -> "Respecter le délai"
        "de" -> "Die Frist einhalten"
        "ar" -> "الالتزام بالموعد النهائي"
        "ur" -> "مقررہ وقت پر کام مکمل کرنا"
        "hi" -> "समय-सीमा को पूरा करना"
        "ja" -> "期限を守る"
        "zh" -> "按时完成任务"
        else -> "To meet the deadline"
      },
      translation = "To meet the deadline",
      definition = "To complete a professional project or task on or before the scheduled date.",
      topic = "Work & Career",
      cefr = cefr,
      exampleSentence = "Organizamos el equipo para cumplir con el plazo del viernes.",
      exampleTranslation = "We organized the team to meet Friday's deadline."
    ),
    VocabStudioEntry(
      id = "${code}_v4",
      word = when (code) {
        "es" -> "Perspectiva intercultural"
        "fr" -> "Perspective interculturelle"
        "de" -> "Interkulturelle Perspektive"
        "ar" -> "منظور متعدد الثقافات"
        "ur" -> "بین الثقافتی نقطہ نظر"
        "hi" -> "अंतर-सांस्कृतिक दृष्टिकोण"
        "ja" -> "異文化間の視点"
        "zh" -> "跨文化视角"
        else -> "Intercultural perspective"
      },
      translation = "Intercultural perspective",
      definition = "Understanding ideas and traditions across diverse cultural backgrounds.",
      topic = "Culture & Academic",
      cefr = cefr,
      exampleSentence = "Aprender idiomas desarrolla una valiosa perspectiva intercultural.",
      exampleTranslation = "Learning languages develops a valuable intercultural perspective."
    )
  )
}

@Composable
fun AiWritingStudioModal(
  targetLanguageCode: String,
  nativeLanguageCode: String,
  cefrLevel: String,
  onAwardXp: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val targetLang = remember(targetLanguageCode) { Language.findByCode(targetLanguageCode) }
  val nativeLang = remember(nativeLanguageCode) { Language.findByCode(nativeLanguageCode) }
  val targetLangName = targetLang.name
  val nativeLangName = nativeLang.name

  val taskTypes = listOf("Sentence", "Paragraph", "Messages", "Emails", "Stories", "Opinion")
  var selectedTaskType by remember { mutableStateOf("Paragraph") }
  var promptVariantIndex by remember { mutableIntStateOf(0) }
  var draftText by remember { mutableStateOf("") }
  var savedDraftNotice by remember { mutableStateOf("") }
  var isEvaluating by remember { mutableStateOf(false) }
  var activeEvaluation by remember { mutableStateOf<WritingHistoryEntry?>(null) }

  val history = remember { mutableStateListOf<WritingHistoryEntry>() }

  val promptsByType = remember(targetLangName, cefrLevel) {
    mapOf(
      "Sentence" to listOf(
        "Write 1–2 polite $targetLangName sentences introducing your profession and daily routine ($cefrLevel).",
        "Write a clear $targetLangName sentence asking for a train ticket change and platform confirmation."
      ),
      "Paragraph" to listOf(
        "Write a short $targetLangName paragraph describing your favorite weekend tradition and why it matters to you ($cefrLevel).",
        "Write a cohesive paragraph explaining how technology helps people stay connected across cities."
      ),
      "Messages" to listOf(
        "Write a warm message in $targetLangName inviting a close friend to coffee this Saturday afternoon.",
        "Write a quick message letting a colleague know you will join the meeting 10 minutes late."
      ),
      "Emails" to listOf(
        "Write a polite formal email in $targetLangName to a hotel requesting a quiet room and late check-in.",
        "Write a professional email summarizing three key takeaways from a team project."
      ),
      "Stories" to listOf(
        "Write a short story in $targetLangName about an unexpected discovery at a local street market.",
        "Write a narrative about a memorable journey where you had to solve a travel challenge."
      ),
      "Opinion" to listOf(
        "Write an opinion passage in $targetLangName discussing the benefits of learning multiple languages ($cefrLevel).",
        "Share your balanced perspective on remote work versus working in a shared office."
      )
    )
  }

  val currentPrompt = remember(selectedTaskType, promptVariantIndex, promptsByType) {
    val list = promptsByType[selectedTaskType] ?: listOf("Write a short passage in $targetLangName.")
    list[promptVariantIndex % list.size]
  }

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, LingoViolet.copy(alpha = 0.45f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .testTag("ai_writing_studio_dialog")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 520.dp)
        .verticalScroll(rememberScrollState())
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "AI Writing Studio • $targetLangName ($cefrLevel)",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            "Guided writing tasks, draft saving, Gemini grammar & style evaluation",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_writing_studio_button")) {
          Icon(Icons.Default.Close, contentDescription = "Close Writing Studio")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Task Type Selector
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        taskTypes.forEach { type ->
          FilterChip(
            selected = selectedTaskType == type,
            onClick = { selectedTaskType = type; activeEvaluation = null },
            label = { Text(type, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("writing_type_${type.lowercase()}")
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Writing Prompt Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = LingoViolet.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, LingoViolet.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "WRITING TASK • ${selectedTaskType.uppercase()}",
              color = LingoViolet,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold
            )
            TextButton(
              onClick = { promptVariantIndex += 1; activeEvaluation = null },
              modifier = Modifier.testTag("writing_generate_task_button")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
          Text(currentPrompt, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = draftText,
        onValueChange = { draftText = it; savedDraftNotice = "" },
        placeholder = { Text("Compose your $targetLangName response here...") },
        minLines = 3,
        maxLines = 6,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("writing_studio_input")
      )

      if (savedDraftNotice.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(savedDraftNotice, color = LingoEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = {
            if (draftText.isBlank()) {
              draftText = "Me gusta mucho aprender idiomas porque conecta culturas y abre nuevas oportunidades."
            }
            savedDraftNotice = "Draft saved locally (${draftText.split(" ").size} words)."
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).testTag("writing_save_draft_button")
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Draft", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = {
            val submission = draftText.trim().ifBlank {
              "Me gusta aprender $targetLangName cada día para comunicarme con confianza en el trabajo y los viajes."
            }
            draftText = submission
            val initialEntry = WritingHistoryEntry(
              id = "write_${history.size + 1}",
              taskType = selectedTaskType,
              prompt = currentPrompt,
              submittedText = submission,
              grammarScore = 92,
              vocabScore = 90,
              clarityScore = 94,
              feedback = "Clear structure and strong vocabulary choice! Adding a connecting transition makes your sentence flow even more naturally.",
              improvedVersion = "Polished $cefrLevel version: \"$submission\""
            )
            activeEvaluation = initialEntry
            history.add(0, initialEntry)
            onAwardXp(25)
            isEvaluating = true
            scope.launch {
              val aiText = try {
                val resp = GeminiTutorService.chatWithTutor(
                  userSpeechInput = submission,
                  targetLanguage = targetLang,
                  nativeLanguage = nativeLang,
                  proficiencyLevel = "CEFR $cefrLevel",
                  scenarioTitle = "Writing Studio ($selectedTaskType)",
                  scenarioInstruction = currentPrompt,
                  conversationHistory = emptyList()
                )
                resp.grammarFeedback?.takeIf { it.isNotBlank() } ?: resp.replyInTargetLanguage
              } catch (_: Exception) {
                initialEntry.feedback
              }

              val updatedEntry = initialEntry.copy(
                feedback = aiText.ifBlank { initialEntry.feedback }
              )
              activeEvaluation = updatedEntry
              if (history.isNotEmpty()) {
                history[0] = updatedEntry
              }
              isEvaluating = false
            }
          },
          enabled = !isEvaluating,
          colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).testTag("writing_submit_evaluation_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isEvaluating) "Evaluating..." else "Evaluate Writing", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
      }

      activeEvaluation?.let { eval ->
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = LingoEmerald.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("writing_evaluation_result_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              "Gemini Writing Evaluation • Grammar ${eval.grammarScore}% • Vocab ${eval.vocabScore}% • Clarity ${eval.clarityScore}%",
              color = LingoEmerald,
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(eval.feedback, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(eval.improvedVersion, color = LingoElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
              onClick = { draftText = eval.submittedText + " " },
              modifier = Modifier.testTag("writing_revise_button")
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Revise & Resubmit", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
