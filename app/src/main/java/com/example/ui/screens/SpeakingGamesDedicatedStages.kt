package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VocabFlashcard
import com.example.data.repository.DynamicAiGameEngine
import com.example.data.repository.DynamicLevelExercise
import com.example.data.repository.GameInteractionMechanic
import com.example.data.repository.LearningResponseEvaluation
import com.example.data.repository.RoleplayScenarioTemplate
import com.example.data.repository.SpeakingDetailedAssessment
import com.example.data.repository.SpeakingGamesContentAndEvaluationEngine
import com.example.data.repository.UltimateLanguageGame
import com.example.speech.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dispatcher Composable that renders one of the 10 genuinely distinct Speaking Game stages
 * (#24 Pronunciation Practice .. #33 Roleplay Mission), with real microphone permission handling,
 * live voice recognition state, audio cleanup on exit, and tailored evaluation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DedicatedSpeakingGameStageHost(
  game: UltimateLanguageGame,
  exercise: DynamicLevelExercise,
  languageCode: String,
  nativeLanguageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  isListening: Boolean,
  spokenText: String,
  onSaveFlashcard: (VocabFlashcard) -> Unit,
  onSpeakingEvaluationCompleted: (LearningResponseEvaluation, String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val partialSpokenText by speechManager.partialSpokenText.collectAsState()
  val isProcessingSpeech by speechManager.isProcessingSpeech.collectAsState()
  val rmsLevel by speechManager.rmsLevel.collectAsState()
  val speechError by speechManager.speechError.collectAsState()
  val isTtsSpeaking by speechManager.isSpeaking.collectAsState()

  // Track user-initiated voice sessions for this specific exercise so stale transcripts from earlier screens are ignored
  var micSessionActiveForExercise by remember(exercise.id) { mutableStateOf(false) }
  var typedOrRecognizedInput by remember(exercise.id) { mutableStateOf("") }
  var usedRealMicInAttempt by remember(exercise.id) { mutableStateOf(false) }
  var peakRmsObserved by remember(exercise.id) { mutableFloatStateOf(0.15f) }
  var recordingStartMs by remember(exercise.id) { mutableLongStateOf(0L) }
  var lastRecordingDurationSec by remember(exercise.id) { mutableIntStateOf(5) }
  var isEvaluating by remember(exercise.id) { mutableStateOf(false) }
  var detailedAssessment by remember(exercise.id) { mutableStateOf<SpeakingDetailedAssessment?>(null) }
  var permissionDeniedNotice by remember(exercise.id) { mutableStateOf<String?>(null) }

  // Game-specific interactive states
  // Game 3 (Articulation Sprint): multi-repetition streak counter (target 2-3 reps)
  var articulationRepsCompleted by remember(exercise.id) { mutableIntStateOf(0) }
  // Game 5 (Quick Answer): optional response timer (15s)
  var quickAnswerTimerEnabled by remember(exercise.id) { mutableStateOf(true) }
  var quickAnswerRemainingSec by remember(exercise.id) { mutableIntStateOf(15) }
  var quickAnswerTimerRunning by remember(exercise.id) { mutableStateOf(false) }
  // Game 6 (Speech Coach): targeted follow-up drill state
  var followUpDrillSpokenText by remember(exercise.id) { mutableStateOf("") }
  var followUpDrillCompleted by remember(exercise.id) { mutableStateOf(false) }
  // Game 7 (Fluency Sprint): selectable 15s / 30s / 60s continuous speaking timer
  var selectedFluencyDurationSec by remember(exercise.id) { mutableIntStateOf(30) }
  var fluencyRemainingSec by remember(exercise.id) { mutableIntStateOf(30) }
  var fluencySprintActive by remember(exercise.id) { mutableStateOf(false) }
  // Game 8 (Shadow Speaking): synchronized word highlight index & speed
  var shadowHighlightedWordIdx by remember(exercise.id) { mutableIntStateOf(-1) }
  var shadowPlaybackSpeed by remember(exercise.id) { mutableFloatStateOf(1.0f) }
  var isShadowingSimultaneously by remember(exercise.id) { mutableStateOf(false) }

  // Clean up audio & recognition when changing exercise or leaving screen
  DisposableEffect(exercise.id) {
    speechManager.clearError()
    onDispose {
      speechManager.stopListening()
      speechManager.stopSpeaking()
    }
  }

  // Track peak RMS while microphone is actively listening
  LaunchedEffect(isListening, rmsLevel) {
    if (isListening && rmsLevel > peakRmsObserved) {
      peakRmsObserved = rmsLevel
    }
  }

  fun evaluateCandidateResponse(candidateText: String, fromMic: Boolean) {
    val clean = candidateText.trim()
    if (clean.isBlank() || isEvaluating) return
    isEvaluating = true
    val durationSec = if (recordingStartMs > 0L) {
      ((System.currentTimeMillis() - recordingStartMs) / 1000L).toInt().coerceIn(2, 65)
    } else {
      lastRecordingDurationSec
    }
    lastRecordingDurationSec = durationSec

    scope.launch {
      val result = SpeakingGamesContentAndEvaluationEngine.evaluateSpeakingGameAttempt(
        exercise = exercise,
        spokenTranscript = clean,
        languageCode = languageCode,
        nativeLanguageCode = nativeLanguageCode,
        speakingDurationSeconds = durationSec,
        peakAudioRms = peakRmsObserved,
        usedRealMicrophoneAudio = fromMic
      )
      detailedAssessment = result
      isEvaluating = false
      if (exercise.mechanic == GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT && result.isPassed) {
        articulationRepsCompleted = (articulationRepsCompleted + 1).coerceAtMost(3)
      }
      // Convert to LearningResponseEvaluation so parent level engine tracks stars, XP, and level completion
      val engineEval = LearningResponseEvaluation(
        isPassed = result.isPassed,
        accuracyScore = result.overallScore,
        matchedWords = result.matchedWords,
        missingWords = result.focusWordsOrSounds,
        feedbackTitle = result.feedbackHeadline,
        detailedExplanation = "${result.grammarFeedback} ${result.vocabularyFeedback}".trim(),
        suggestedImprovement = "${result.pronunciationNote} • Model: \"${result.improvedSentence}\""
      )
      onSpeakingEvaluationCompleted(engineEval, clean)
    }
  }

  // Observe recognized speech ONLY when the learner initiated microphone recording on this exercise
  LaunchedEffect(spokenText, isListening) {
    if (micSessionActiveForExercise && spokenText.isNotBlank() && !isListening) {
      typedOrRecognizedInput = spokenText
      micSessionActiveForExercise = false
      fluencySprintActive = false
      quickAnswerTimerRunning = false
      isShadowingSimultaneously = false
      evaluateCandidateResponse(spokenText, fromMic = true)
    }
  }

  // Quick Answer optional countdown timer
  LaunchedEffect(quickAnswerTimerRunning, quickAnswerRemainingSec, quickAnswerTimerEnabled) {
    if (quickAnswerTimerRunning && quickAnswerTimerEnabled && detailedAssessment == null) {
      if (quickAnswerRemainingSec > 0) {
        delay(1000L)
        quickAnswerRemainingSec -= 1
      } else {
        quickAnswerTimerRunning = false
        if (isListening) {
          speechManager.finishListeningNow()
        }
      }
    }
  }

  // Fluency Sprint continuous timer (15s / 30s / 60s)
  LaunchedEffect(fluencySprintActive, fluencyRemainingSec) {
    if (fluencySprintActive && detailedAssessment == null) {
      if (fluencyRemainingSec > 0) {
        delay(1000L)
        fluencyRemainingSec -= 1
      } else {
        fluencySprintActive = false
        if (isListening) {
          speechManager.finishListeningNow()
        } else if (typedOrRecognizedInput.isNotBlank()) {
          evaluateCandidateResponse(typedOrRecognizedInput, fromMic = usedRealMicInAttempt)
        }
      }
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      permissionDeniedNotice = null
      micSessionActiveForExercise = true
      usedRealMicInAttempt = true
      peakRmsObserved = 0.1f
      recordingStartMs = System.currentTimeMillis()
      speechManager.clearSpokenText()
      speechManager.startListening(languageCode)
    } else {
      permissionDeniedNotice = "Microphone permission is required for voice assessment. Grant permission or use the practice input below."
    }
  }

  fun toggleMicrophoneRecording() {
    if (isListening) {
      speechManager.finishListeningNow()
      return
    }
    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (hasPerm) {
      permissionDeniedNotice = null
      micSessionActiveForExercise = true
      usedRealMicInAttempt = true
      peakRmsObserved = 0.1f
      recordingStartMs = System.currentTimeMillis()
      speechManager.clearSpokenText()
      speechManager.startListening(languageCode)
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  fun resetAttemptForRetry() {
    speechManager.stopListening()
    speechManager.clearSpokenText()
    detailedAssessment = null
    typedOrRecognizedInput = ""
    micSessionActiveForExercise = false
    fluencySprintActive = false
    fluencyRemainingSec = selectedFluencyDurationSec
    quickAnswerRemainingSec = 15
    quickAnswerTimerRunning = false
    shadowHighlightedWordIdx = -1
    isShadowingSimultaneously = false
  }

  // Game 10 (Roleplay Mission) has its own full multi-turn interactive scenario screen
  if (exercise.mechanic == GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION) {
    RoleplayMissionInteractiveStage(
      game = game,
      exercise = exercise,
      languageCode = languageCode,
      nativeLanguageCode = nativeLanguageCode,
      accentColor = accentColor,
      speechManager = speechManager,
      isListening = isListening,
      spokenText = spokenText,
      partialSpokenText = partialSpokenText,
      onToggleMic = { toggleMicrophoneRecording() },
      onMissionCompleted = { eval, summaryText ->
        onSpeakingEvaluationCompleted(eval, summaryText)
      }
    )
    return
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("speaking_game_stage_${exercise.mechanic.name}"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // ==================== 1. DEDICATED STAGE PER SPEAKING GAME (#24..#32) ====================
    when (exercise.mechanic) {
      // GAME 1 — PRONUNCIATION PRACTICE (Individual words + phonetics + difficult sound focus)
      GameInteractionMechanic.SPEAKING_MIC -> {
        PronunciationPracticeDedicatedCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager,
          isTtsSpeaking = isTtsSpeaking,
          onSaveFlashcard = onSaveFlashcard
        )
      }

      // GAME 2 — REPEAT AFTER ME (Complete spoken sentence + word-by-word comparison)
      GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE -> {
        RepeatAfterMeDedicatedCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager,
          detailedAssessment = detailedAssessment,
          onSaveFlashcard = onSaveFlashcard
        )
      }

      // GAME 3 — ARTICULATION SPRINT (Tongue twisters + consonant combinations + 3-rep tracker)
      GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT -> {
        ArticulationSprintDedicatedCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager,
          repsCompleted = articulationRepsCompleted,
          onSaveFlashcard = onSaveFlashcard
        )
      }

      // GAME 4 — SPEAK THE SCENE (Real scene visual + open picture question + vocab chips)
      GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE -> {
        SpeakTheSceneDedicatedCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager
        )
      }

      // GAME 5 — QUICK ANSWER (Practical everyday question + optional countdown timer)
      GameInteractionMechanic.SPEAKING_QUICK_ANSWER -> {
        QuickAnswerDedicatedCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager,
          timerEnabled = quickAnswerTimerEnabled,
          remainingSec = quickAnswerRemainingSec,
          timerRunning = quickAnswerTimerRunning,
          onToggleTimer = {
            quickAnswerTimerEnabled = !quickAnswerTimerEnabled
            quickAnswerRemainingSec = 15
          },
          onStartQuestionAudioAndTimer = {
            speechManager.speakWithRate(exercise.prompt, languageCode, 1.0f)
            if (quickAnswerTimerEnabled) {
              quickAnswerRemainingSec = 15
              quickAnswerTimerRunning = true
            }
          }
        )
      }

      // GAME 6 — SPEECH COACH (Free/guided speaking + grammar/vocab/pronunciation analysis + follow-up drill)
      GameInteractionMechanic.SPEAKING_SPEECH_COACH -> {
        SpeechCoachPromptCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager
        )
      }

      // GAME 7 — FLUENCY SPRINT (Continuous topic speaking + 15s / 30s / 60s timer + talking points)
      GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT -> {
        FluencySprintDedicatedCard(
          exercise = exercise,
          accentColor = accentColor,
          selectedDurationSec = selectedFluencyDurationSec,
          remainingSec = fluencyRemainingSec,
          isSprintActive = fluencySprintActive,
          onSelectDuration = { sec ->
            selectedFluencyDurationSec = sec
            fluencyRemainingSec = sec
            fluencySprintActive = false
          },
          onStartSprint = {
            fluencyRemainingSec = selectedFluencyDurationSec
            fluencySprintActive = true
            toggleMicrophoneRecording()
          }
        )
      }

      // GAME 8 — SHADOW SPEAKING (Synchronized word-by-word highlight + simultaneous shadowing)
      GameInteractionMechanic.SPEAKING_SHADOWING -> {
        val shadowWords = remember(exercise.targetPhrase) {
          exercise.targetPhrase.split(Regex("\\s+")).filter { it.isNotBlank() }
        }
        ShadowSpeakingDedicatedCard(
          exercise = exercise,
          shadowWords = shadowWords,
          highlightedWordIdx = shadowHighlightedWordIdx,
          playbackSpeed = shadowPlaybackSpeed,
          isShadowing = isShadowingSimultaneously,
          accentColor = accentColor,
          onSelectSpeed = { spd -> shadowPlaybackSpeed = spd },
          onListenWithKaraoke = {
            scope.launch {
              isShadowingSimultaneously = false
              speechManager.speakWithRate(exercise.targetPhrase, languageCode, shadowPlaybackSpeed)
              val perWordMs = (380L / shadowPlaybackSpeed).toLong().coerceAtLeast(180L)
              for (wIdx in shadowWords.indices) {
                shadowHighlightedWordIdx = wIdx
                delay(perWordMs)
              }
              shadowHighlightedWordIdx = -1
            }
          },
          onStartSimultaneousShadowing = {
            scope.launch {
              isShadowingSimultaneously = true
              toggleMicrophoneRecording()
              val perWordMs = (410L / shadowPlaybackSpeed).toLong().coerceAtLeast(200L)
              for (wIdx in shadowWords.indices) {
                shadowHighlightedWordIdx = wIdx
                delay(perWordMs)
              }
              shadowHighlightedWordIdx = -1
            }
          }
        )
      }

      // GAME 9 — DIALOGUE LINE (Two-person conversation + Speaker A audio + Speaker A continuation)
      GameInteractionMechanic.SPEAKING_DIALOGUE_LINE -> {
        DialogueLineDedicatedCard(
          exercise = exercise,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager,
          spokenReply = typedOrRecognizedInput,
          detailedAssessment = detailedAssessment
        )
      }

      else -> {}
    }

    // ==================== 2. SHARED MICROPHONE & LIVE TRANSCRIPT CAPTURE STAGE ====================
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
      border = BorderStroke(
        width = 1.dp,
        color = if (isListening) Color(0xFFFF5252) else accentColor.copy(alpha = 0.35f)
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f, fill = false)
              .padding(end = 8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                  if (isListening) Color(0xFFFF5252) else accentColor
                )
                .clickable {
                  if (exercise.mechanic == GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT && !isListening) {
                    fluencyRemainingSec = selectedFluencyDurationSec
                    fluencySprintActive = true
                  }
                  if (exercise.mechanic == GameInteractionMechanic.SPEAKING_QUICK_ANSWER && !isListening && quickAnswerTimerEnabled) {
                    quickAnswerRemainingSec = 15
                    quickAnswerTimerRunning = true
                  }
                  toggleMicrophoneRecording()
                }
                .testTag("speaking_game_mic_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Recording" else "Start Recording",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
            }

            Column(modifier = Modifier.weight(1f, fill = false)) {
              Text(
                text = when {
                  isListening -> "Listening... Tap Stop when finished"
                  isProcessingSpeech || isEvaluating -> "Analyzing your speech..."
                  typedOrRecognizedInput.isNotBlank() -> "Your Spoken Response:"
                  else -> micCallToActionForMechanic(exercise.mechanic)
                },
                color = if (isListening) Color(0xFFFF8A80) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              val liveText = when {
                isListening && partialSpokenText.isNotBlank() -> partialSpokenText
                typedOrRecognizedInput.isNotBlank() -> "\"$typedOrRecognizedInput\""
                else -> "Speak clearly into your microphone"
              }
              Text(
                text = liveText,
                color = if (typedOrRecognizedInput.isNotBlank() || partialSpokenText.isNotBlank()) {
                  Color(0xFF80D8FF)
                } else {
                  Color(0xFF94A3B8)
                },
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          if (detailedAssessment != null) {
            OutlinedButton(
              onClick = { resetAttemptForRetry() },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.5f)),
              modifier = Modifier.testTag("speaking_retry_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFF64B5F6), modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Retry", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Live audio level bar while listening
        if (isListening) {
          LinearProgressIndicator(
            progress = { rmsLevel.coerceIn(0.08f, 1.0f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFFF5252),
            trackColor = Color.White.copy(alpha = 0.10f)
          )
        }

        // Permission or recognition error notice
        val errorMsg = permissionDeniedNotice ?: speechError
        if (!errorMsg.isNullOrBlank()) {
          Surface(
            color = Color(0xFFFFB300).copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMsg,
              color = Color(0xFFFFE082),
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        // Accessible keyboard / simulated speech fallback input
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = typedOrRecognizedInput,
            onValueChange = { typedOrRecognizedInput = it },
            placeholder = {
              Text(
                text = placeholderForSpeakingMechanic(exercise.mechanic),
                color = Color(0xFF64748B),
                fontSize = 12.sp
              )
            },
            singleLine = exercise.mechanic != GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT,
            maxLines = if (exercise.mechanic == GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT) 3 else 1,
            modifier = Modifier
              .weight(1f)
              .testTag("speaking_fallback_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = accentColor,
              unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )
          Button(
            onClick = {
              if (typedOrRecognizedInput.isNotBlank()) {
                evaluateCandidateResponse(typedOrRecognizedInput, fromMic = usedRealMicInAttempt)
              }
            },
            enabled = typedOrRecognizedInput.isNotBlank() && !isEvaluating,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier
              .height(48.dp)
              .testTag("speaking_evaluate_button")
          ) {
            Text("Check", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // ==================== 3. GAME-SPECIFIC ASSESSMENT & COACHING FEEDBACK CARD ====================
    AnimatedVisibility(visible = detailedAssessment != null) {
      detailedAssessment?.let { assessment ->
        SpeakingGameDetailedFeedbackCard(
          exercise = exercise,
          assessment = assessment,
          languageCode = languageCode,
          accentColor = accentColor,
          speechManager = speechManager,
          followUpSpokenText = followUpDrillSpokenText,
          followUpCompleted = followUpDrillCompleted,
          onPracticeFollowUpPhrase = { phrase ->
            speechManager.speakWithRate(phrase, languageCode, 0.95f)
            followUpDrillSpokenText = phrase
            followUpDrillCompleted = true
          },
          onRetry = { resetAttemptForRetry() }
        )
      }
    }
  }
}

private fun micCallToActionForMechanic(mechanic: GameInteractionMechanic): String = when (mechanic) {
  GameInteractionMechanic.SPEAKING_MIC -> "Tap Mic & Pronounce the Word"
  GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE -> "Tap Mic & Repeat the Full Sentence"
  GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT -> "Tap Mic & Say the Tongue Twister"
  GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE -> "Tap Mic & Describe the Picture"
  GameInteractionMechanic.SPEAKING_QUICK_ANSWER -> "Tap Mic & Answer in Your Own Words"
  GameInteractionMechanic.SPEAKING_SPEECH_COACH -> "Tap Mic & Speak Your Sentence Freely"
  GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT -> "Tap Mic & Speak Continuously on the Topic"
  GameInteractionMechanic.SPEAKING_SHADOWING -> "Tap 'Shadow Along' or Mic to Speak in Sync"
  GameInteractionMechanic.SPEAKING_DIALOGUE_LINE -> "Tap Mic & Speak Your Reply in the Dialogue"
  else -> "Tap Mic to Speak"
}

private fun placeholderForSpeakingMechanic(mechanic: GameInteractionMechanic): String = when (mechanic) {
  GameInteractionMechanic.SPEAKING_MIC -> "Or type/edit recognized word..."
  GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE -> "Or type the repeated sentence..."
  GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT -> "Or type the articulation phrase..."
  GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE -> "Describe what is happening in the scene..."
  GameInteractionMechanic.SPEAKING_QUICK_ANSWER -> "Speak or type your personal answer..."
  GameInteractionMechanic.SPEAKING_SPEECH_COACH -> "Speak or type your sentence for coaching..."
  GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT -> "Speak continuously or enter your response..."
  GameInteractionMechanic.SPEAKING_SHADOWING -> "Shadowed sentence transcript..."
  GameInteractionMechanic.SPEAKING_DIALOGUE_LINE -> "Speak or type your next line in the conversation..."
  else -> "Speak or type response..."
}

// ============================================================================================
// GAME 1 — PRONUNCIATION PRACTICE CARD (Individual Word + Phonetics + Difficult Sound Focus)
// ============================================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PronunciationPracticeDedicatedCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  isTtsSpeaking: Boolean,
  onSaveFlashcard: (VocabFlashcard) -> Unit
) {
  val syllables = remember(exercise.targetPhrase) {
    SpeakingGamesContentAndEvaluationEngine.splitWordIntoDisplaySyllables(exercise.targetPhrase)
  }
  val difficultSound = exercise.clozeMaskedSentence.ifBlank {
    SpeakingGamesContentAndEvaluationEngine.detectDifficultSoundDescription(exercise.targetPhrase, languageCode)
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
    modifier = Modifier.testTag("pronunciation_practice_word_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "Focus: $difficultSound",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
        IconButton(
          onClick = {
            onSaveFlashcard(
              VocabFlashcard(
                id = "fc_${exercise.id}",
                word = exercise.targetPhrase,
                translation = exercise.translation,
                phonetic = exercise.phonetic,
                exampleSentence = exercise.secondaryAudioPhrase.ifBlank { exercise.explanation }
              )
            )
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Word", tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
        }
      }

      // Prominent Single Word
      Text(
        text = exercise.targetPhrase,
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
      )

      if (exercise.phonetic.isNotBlank()) {
        Text(
          text = exercise.phonetic,
          color = Color(0xFF80D8FF),
          fontSize = 15.sp,
          fontStyle = FontStyle.Italic
        )
      }

      Text(
        text = exercise.translation,
        color = Color(0xFFB0BEC5),
        fontSize = 14.sp,
        textAlign = TextAlign.Center
      )

      // Tappable Syllable Pills
      if (syllables.size > 1) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          syllables.forEachIndexed { idx, syl ->
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
              modifier = Modifier.clickable {
                speechManager.speakWithRate(syl, languageCode, 0.80f)
              }
            ) {
              Text(
                text = syl,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }
      }

      // Normal (1.0x) and Slow (0.75x) Model Pronunciation Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 1.0f) },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("pronunciation_model_audio_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isTtsSpeaking) "Playing..." else "Listen (1.0x)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        OutlinedButton(
          onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 0.72f) },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("pronunciation_slow_audio_btn"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color(0xFF80D8FF).copy(alpha = 0.5f))
        ) {
          Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Slow (0.75x)", color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================================
// GAME 2 — REPEAT AFTER ME CARD (Complete Spoken Sentence + Word-by-Word Comparison)
// ============================================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatAfterMeDedicatedCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  detailedAssessment: SpeakingDetailedAssessment?,
  onSaveFlashcard: (VocabFlashcard) -> Unit
) {
  val sentenceWords = remember(exercise.targetPhrase) {
    exercise.targetPhrase.split(Regex("\\s+")).filter { it.isNotBlank() }
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
    modifier = Modifier.testTag("repeat_after_me_sentence_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "COMPLETE SENTENCE • ${sentenceWords.size} WORDS",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
        IconButton(
          onClick = {
            onSaveFlashcard(
              VocabFlashcard(
                id = "fc_${exercise.id}",
                word = exercise.targetPhrase,
                translation = exercise.translation,
                phonetic = exercise.phonetic,
                exampleSentence = exercise.targetPhrase
              )
            )
          },
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Sentence", tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
        }
      }

      Text(
        text = exercise.targetPhrase,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 27.sp
      )

      Text(
        text = exercise.translation,
        color = Color(0xFF94A3B8),
        fontSize = 13.sp
      )

      // Word-by-Word Interactive Chips (highlights matched words in green and missed words in red after recording)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        sentenceWords.forEach { word ->
          val cleanW = word.replace(Regex("[.,!?;:\"'¡¿]"), "")
          val isMatched = detailedAssessment?.matchedWords?.any {
            it.replace(Regex("[.,!?;:\"'¡¿]"), "").equals(cleanW, ignoreCase = true)
          } == true
          val isMissing = detailedAssessment != null && !isMatched

          val chipBg = when {
            isMatched -> Color(0xFF00E676).copy(alpha = 0.20f)
            isMissing -> Color(0xFFFF5252).copy(alpha = 0.22f)
            else -> Color(0xFF1E293B)
          }
          val chipBorder = when {
            isMatched -> Color(0xFF00E676)
            isMissing -> Color(0xFFFF5252)
            else -> Color.White.copy(alpha = 0.14f)
          }
          Surface(
            color = chipBg,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, chipBorder),
            modifier = Modifier.clickable {
              speechManager.speakWithRate(cleanW, languageCode, 0.85f)
            }
          ) {
            Text(
              text = word,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 1.0f) },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("repeat_sentence_play_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Play Sentence", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        OutlinedButton(
          onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 0.75f) },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("repeat_sentence_slow_btn"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color(0xFF80D8FF).copy(alpha = 0.5f))
        ) {
          Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Slow (0.75x)", color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================================
// GAME 3 — ARTICULATION SPRINT CARD (Tongue Twisters + Consonant Cluster + Multi-Rep Tracker)
// ============================================================================================
@Composable
private fun ArticulationSprintDedicatedCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  repsCompleted: Int,
  onSaveFlashcard: (VocabFlashcard) -> Unit
) {
  val soundCluster = exercise.clozeMaskedSentence.ifBlank { "Consonant Cluster Agility" }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("articulation_sprint_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "SOUND DRILL • $soundCluster",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        // 3-Repetition Tracker Badges
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          for (rep in 1..3) {
            val done = rep <= repsCompleted
            Surface(
              color = if (done) Color(0xFF00E676).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, if (done) Color(0xFF00E676) else Color.White.copy(alpha = 0.18f))
            ) {
              Text(
                text = "Rep $rep",
                color = if (done) Color(0xFF00E676) else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }
      }

      Text(
        text = "\"${exercise.targetPhrase}\"",
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 27.sp
      )

      if (exercise.phonetic.isNotBlank()) {
        Text(
          text = exercise.phonetic,
          color = Color(0xFF80D8FF),
          fontSize = 13.sp,
          fontStyle = FontStyle.Italic
        )
      }

      Text(
        text = exercise.hint.ifBlank { exercise.translation },
        color = Color(0xFFB0BEC5),
        fontSize = 12.sp
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 0.70f) },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("articulation_slow_btn"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color(0xFF80D8FF).copy(alpha = 0.5f))
        ) {
          Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Slow (0.70x)", color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 1.05f) },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("articulation_normal_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Normal (1.0x)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================================
// GAME 4 — SPEAK THE SCENE CARD (Real Scene Image + Open Question + Helpful Scene Vocabulary)
// ============================================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeakTheSceneDedicatedCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("speak_the_scene_visual_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "SCENE • ${exercise.phonetic.ifBlank { "Describe the Picture" }}",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        IconButton(
          onClick = { speechManager.speakWithRate(exercise.prompt, languageCode, 0.95f) },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear Question", tint = Color(0xFF80D8FF))
        }
      }

      // Real Scene Illustration / Bundled Scene Photo
      VisualSceneAndClothingIllustration(
        kind = exercise.illustrationKind,
        modifier = Modifier
          .fillMaxWidth()
          .height(175.dp)
          .clip(RoundedCornerShape(14.dp))
          .testTag("speak_scene_image")
      )

      // Scene Question
      Text(
        text = exercise.prompt,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 23.sp
      )

      // Helpful Vocabulary Ideas to Include
      if (exercise.options.isNotEmpty()) {
        Text(
          text = "Helpful vocabulary you can use (tap to hear):",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          exercise.options.forEach { vocabWord ->
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
              modifier = Modifier.clickable {
                speechManager.speakWithRate(vocabWord, languageCode, 0.90f)
              }
            ) {
              Text(
                text = vocabWord,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

// ============================================================================================
// GAME 5 — QUICK ANSWER CARD (Practical Everyday Question + Optional Countdown Timer)
// ============================================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAnswerDedicatedCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  timerEnabled: Boolean,
  remainingSec: Int,
  timerRunning: Boolean,
  onToggleTimer: () -> Unit,
  onStartQuestionAudioAndTimer: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("quick_answer_question_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "EVERYDAY QUESTION • ANSWER NATURALLY",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        Surface(
          color = if (timerEnabled) Color(0xFFFFB300).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .clickable { onToggleTimer() }
            .testTag("quick_answer_timer_toggle")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              Icons.Default.Timer,
              contentDescription = null,
              tint = if (timerEnabled) Color(0xFFFFD54F) else Color(0xFF94A3B8),
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = if (timerEnabled) "${remainingSec}s Timer" else "Timer Off",
              color = if (timerEnabled) Color(0xFFFFD54F) else Color(0xFF94A3B8),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      if (timerEnabled && timerRunning) {
        LinearProgressIndicator(
          progress = { (remainingSec / 15f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (remainingSec <= 5) Color(0xFFFF5252) else Color(0xFFFFD54F),
          trackColor = Color.White.copy(alpha = 0.10f)
        )
      }

      Text(
        text = exercise.prompt,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 27.sp
      )

      if (exercise.translation.isNotBlank() && !exercise.translation.equals(exercise.prompt, ignoreCase = true)) {
        Text(
          text = exercise.translation,
          color = Color(0xFF94A3B8),
          fontSize = 13.sp
        )
      }

      Button(
        onClick = onStartQuestionAudioAndTimer,
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp)
          .testTag("quick_answer_hear_question_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.85f))
      ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Hear Question Aloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }

      if (exercise.options.isNotEmpty()) {
        Text(
          text = "Answer ideas (speak a complete sentence in your own words):",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          exercise.options.take(5).forEach { idea ->
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
              Text(
                text = idea,
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

// ============================================================================================
// GAME 6 — SPEECH COACH PROMPT CARD (Free/Guided Speaking + Personal Coaching)
// ============================================================================================
@Composable
private fun SpeechCoachPromptCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("speech_coach_prompt_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Surface(
        color = accentColor.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "PERSONAL SPEECH COACH • SPEAK FREELY",
          color = accentColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }

      Text(
        text = exercise.prompt,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 25.sp
      )

      Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Reference Expression:",
              color = Color(0xFF80D8FF),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "\"${exercise.targetPhrase}\"",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = exercise.translation,
              color = Color(0xFF94A3B8),
              fontSize = 12.sp
            )
          }
          IconButton(
            onClick = { speechManager.speakWithRate(exercise.targetPhrase, languageCode, 0.95f) }
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = accentColor)
          }
        }
      }
    }
  }
}

// ============================================================================================
// GAME 7 — FLUENCY SPRINT CARD (Everyday Topic + 15s / 30s / 60s Timer + Talking Points)
// ============================================================================================
@Composable
private fun FluencySprintDedicatedCard(
  exercise: DynamicLevelExercise,
  accentColor: Color,
  selectedDurationSec: Int,
  remainingSec: Int,
  isSprintActive: Boolean,
  onSelectDuration: (Int) -> Unit,
  onStartSprint: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("fluency_sprint_topic_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "FLUENCY SPRINT • CONTINUOUS SPEAKING",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        // 15s / 30s / 60s Duration Selector Pills
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(15, 30, 60).forEach { sec ->
            val selected = selectedDurationSec == sec
            Surface(
              color = if (selected) accentColor else Color(0xFF1E293B),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .clickable { onSelectDuration(sec) }
                .testTag("fluency_timer_${sec}s")
            ) {
              Text(
                text = "${sec}s",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      Text(
        text = exercise.prompt,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 24.sp
      )

      // Talking Points Checklist
      if (exercise.options.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Ideas to cover while speaking:",
            color = Color(0xFF80D8FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          exercise.options.forEachIndexed { idx, bullet ->
            Text(
              text = "${idx + 1}. $bullet",
              color = Color(0xFFE2E8F0),
              fontSize = 12.sp
            )
          }
        }
      }

      // Live Countdown Display
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isSprintActive) "Speaking Time Remaining" else "Selected Sprint Duration",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
          )
          Text(
            text = "${remainingSec}s / ${selectedDurationSec}s",
            color = if (isSprintActive) Color(0xFFFFD54F) else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }

        Button(
          onClick = onStartSprint,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor),
          modifier = Modifier.testTag("start_fluency_sprint_btn")
        ) {
          Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isSprintActive) "Sprint Running..." else "Start ${selectedDurationSec}s Sprint",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }

      LinearProgressIndicator(
        progress = { (remainingSec.toFloat() / selectedDurationSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = accentColor,
        trackColor = Color.White.copy(alpha = 0.10f)
      )
    }
  }
}

// ============================================================================================
// GAME 8 — SHADOW SPEAKING CARD (Synchronized Karaoke Word Highlight + Simultaneous Shadowing)
// ============================================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShadowSpeakingDedicatedCard(
  exercise: DynamicLevelExercise,
  shadowWords: List<String>,
  highlightedWordIdx: Int,
  playbackSpeed: Float,
  isShadowing: Boolean,
  accentColor: Color,
  onSelectSpeed: (Float) -> Unit,
  onListenWithKaraoke: () -> Unit,
  onStartSimultaneousShadowing: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("shadow_speaking_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = if (isShadowing) "SHADOWING LIVE NOW" else "SYNCHRONIZED SHADOW SPEAKING",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(0.75f to "0.75x", 1.0f to "1.0x").forEach { (spd, label) ->
            val selected = kotlin.math.abs(playbackSpeed - spd) < 0.05f
            Surface(
              color = if (selected) accentColor else Color(0xFF1E293B),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .clickable { onSelectSpeed(spd) }
                .testTag("shadow_speed_$label")
            ) {
              Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // Synchronized Word-by-Word Highlight Stage
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0B1226), RoundedCornerShape(14.dp))
          .padding(14.dp)
      ) {
        shadowWords.forEachIndexed { idx, word ->
          val isCurrent = idx == highlightedWordIdx
          val isPassedWord = highlightedWordIdx > idx
          Surface(
            color = when {
              isCurrent -> accentColor
              isPassedWord -> accentColor.copy(alpha = 0.25f)
              else -> Color.Transparent
            },
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = word,
              color = when {
                isCurrent -> Color.White
                isPassedWord -> Color(0xFF80D8FF)
                else -> Color(0xFFE2E8F0)
              },
              fontSize = 19.sp,
              fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }
      }

      Text(
        text = exercise.translation,
        color = Color(0xFF94A3B8),
        fontSize = 13.sp
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onListenWithKaraoke,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("shadow_listen_rhythm_btn"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color(0xFF80D8FF).copy(alpha = 0.5f))
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("1. Listen Rhythm", color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = onStartSimultaneousShadowing,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("shadow_along_now_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("2. Shadow Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================================
// GAME 9 — DIALOGUE LINE CARD (Multi-Turn Two-Speaker Conversation Completion)
// ============================================================================================
@Composable
private fun DialogueLineDedicatedCard(
  exercise: DynamicLevelExercise,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  spokenReply: String,
  detailedAssessment: SpeakingDetailedAssessment?
) {
  val partnerRole = exercise.conversationRole.ifBlank { "Conversation Partner" }
  val speakerALine = exercise.dialogueSpeakerALine.ifBlank { exercise.secondaryAudioPhrase }
  val followUpLine = exercise.dialogueSpeakerBLine

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.testTag("dialogue_line_conversation_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = exercise.prompt,
        color = Color(0xFF80D8FF),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )

      // Speaker A Opening Turn Bubble
      Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = partnerRole,
              color = accentColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold
            )
            IconButton(
              onClick = { speechManager.speakWithRate(speakerALine, languageCode, 0.95f) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear Speaker A", tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
            }
          }
          Text(
            text = "\"$speakerALine\"",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          if (exercise.clozeMaskedSentence.isNotBlank()) {
            Text(
              text = exercise.clozeMaskedSentence,
              color = Color(0xFF94A3B8),
              fontSize = 12.sp
            )
          }
        }
      }

      // Learner's Spoken Reply Bubble
      Surface(
        color = accentColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Your Next Line (Speak or Type):",
            color = Color(0xFF80D8FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (spokenReply.isNotBlank()) "\"$spokenReply\"" else "Tap the microphone below to reply naturally...",
            color = if (spokenReply.isNotBlank()) Color.White else Color(0xFF94A3B8),
            fontSize = 15.sp,
            fontWeight = if (spokenReply.isNotBlank()) FontWeight.Bold else FontWeight.Normal
          )
        }
      }

      // Speaker A Follow-Up Continuation Line (Revealed after learner responds!)
      if (detailedAssessment != null && followUpLine.isNotBlank()) {
        Surface(
          color = Color(0xFF00E676).copy(alpha = 0.12f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$partnerRole continues the conversation:",
                color = Color(0xFF00E676),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "\"$followUpLine\"",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            IconButton(
              onClick = { speechManager.speakWithRate(followUpLine, languageCode, 0.95f) }
            ) {
              Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear continuation", tint = Color(0xFF00E676))
            }
          }
        }
      }
    }
  }
}

// ============================================================================================
// DETAILED FEEDBACK & TARGETED FOLLOW-UP DRILL CARD FOR SPEAKING GAMES
// ============================================================================================
@Composable
private fun SpeakingGameDetailedFeedbackCard(
  exercise: DynamicLevelExercise,
  assessment: SpeakingDetailedAssessment,
  languageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  followUpSpokenText: String,
  followUpCompleted: Boolean,
  onPracticeFollowUpPhrase: (String) -> Unit,
  onRetry: () -> Unit
) {
  val statusColor = if (assessment.isPassed) Color(0xFF00E676) else Color(0xFFFFB300)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.11f)),
    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.50f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("speaking_detailed_feedback_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = assessment.feedbackHeadline,
          color = statusColor,
          fontSize = 15.sp,
          fontWeight = FontWeight.ExtraBold
        )
        Surface(
          color = statusColor.copy(alpha = 0.20f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "${assessment.overallScore}%",
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Text(
        text = assessment.grammarFeedback,
        color = Color.White,
        fontSize = 13.sp
      )

      if (assessment.vocabularyFeedback.isNotBlank()) {
        Text(
          text = assessment.vocabularyFeedback,
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp
        )
      }

      Text(
        text = assessment.pronunciationNote,
        color = Color(0xFF80D8FF),
        fontSize = 12.sp
      )

      // Improved / Model Version Row with Audio Playback
      Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Model / Polished Version:",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "\"${assessment.improvedSentence}\"",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(
            onClick = { speechManager.speakWithRate(assessment.improvedSentence, languageCode, 0.95f) }
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear Model", tint = accentColor)
          }
        }
      }

      // Speech Coach Targeted Follow-Up Exercise (Game 6)
      if (exercise.mechanic == GameInteractionMechanic.SPEAKING_SPEECH_COACH &&
        assessment.targetedFollowUpPhrase.isNotBlank()
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = assessment.targetedFollowUpPrompt.ifBlank { "Targeted Follow-Up Drill:" },
              color = Color(0xFF80D8FF),
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "\"${assessment.targetedFollowUpPhrase}\"",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { onPracticeFollowUpPhrase(assessment.targetedFollowUpPhrase) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.testTag("speech_coach_followup_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (followUpCompleted) "Follow-Up Practiced!" else "Hear & Practice Follow-Up",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}

// ============================================================================================
// GAME 10 — ROLEPLAY MISSION INTERACTIVE MULTI-TURN STAGE (10 Real-Life Scenarios)
// ============================================================================================
private data class RoleplayChatTurn(
  val speakerName: String,
  val isUser: Boolean,
  val text: String,
  val feedbackSubtext: String = ""
)

@Composable
private fun RoleplayMissionInteractiveStage(
  game: UltimateLanguageGame,
  exercise: DynamicLevelExercise,
  languageCode: String,
  nativeLanguageCode: String,
  accentColor: Color,
  speechManager: SpeechManager,
  isListening: Boolean,
  spokenText: String,
  partialSpokenText: String,
  onToggleMic: () -> Unit,
  onMissionCompleted: (LearningResponseEvaluation, String) -> Unit
) {
  val scope = rememberCoroutineScope()
  val allScenarios = SpeakingGamesContentAndEvaluationEngine.ROLEPLAY_10_SCENARIOS
  val initialScenario = remember(exercise.id) {
    allScenarios.firstOrNull { it.id == exercise.phonetic }
      ?: SpeakingGamesContentAndEvaluationEngine.getScenarioForLevel(exercise.levelNumber)
  }

  var activeScenario by remember(exercise.id) { mutableStateOf(initialScenario) }
  val targetKey = languageCode.lowercase().take(2)

  val chatHistory = remember(exercise.id, activeScenario.id) {
    val opening = activeScenario.openingsByLang[targetKey] ?: activeScenario.openingsByLang["en"]!!
    mutableStateListOf(
      RoleplayChatTurn(
        speakerName = activeScenario.partnerRole,
        isUser = false,
        text = opening,
        feedbackSubtext = activeScenario.openingTranslationEn
      )
    )
  }

  var draftReply by remember(exercise.id, activeScenario.id) { mutableStateOf("") }
  var isPartnerThinking by remember(exercise.id, activeScenario.id) { mutableStateOf(false) }
  var completedUserTurns by remember(exercise.id, activeScenario.id) { mutableIntStateOf(0) }
  var micInitiatedInRoleplay by remember(exercise.id, activeScenario.id) { mutableStateOf(false) }

  fun submitRoleplayTurn(userText: String) {
    val clean = userText.trim()
    if (clean.length < 2 || isPartnerThinking) return
    val lastPartnerLine = chatHistory.lastOrNull { !it.isUser }?.text ?: activeScenario.openingTranslationEn
    chatHistory.add(RoleplayChatTurn(speakerName = "You", isUser = true, text = clean))
    draftReply = ""
    isPartnerThinking = true

    scope.launch {
      val (eval, aiPartnerReply) = DynamicAiGameEngine.evaluateConversationTurnWithAi(
        game = game,
        languageCode = languageCode,
        nativeLanguageCode = nativeLanguageCode,
        partnerLine = lastPartnerLine,
        userMessage = clean,
        turnNumber = completedUserTurns + 1
      )
      val scriptedFollowUps = activeScenario.followUpTurnsByLang[targetKey]
        ?: activeScenario.followUpTurnsByLang["en"].orEmpty()
      val nextPartnerLine = aiPartnerReply.ifBlank {
        scriptedFollowUps.getOrNull(completedUserTurns) ?: scriptedFollowUps.lastOrNull().orEmpty()
      }

      if (nextPartnerLine.isNotBlank()) {
        chatHistory.add(
          RoleplayChatTurn(
            speakerName = activeScenario.partnerRole,
            isUser = false,
            text = nextPartnerLine,
            feedbackSubtext = "${eval.feedbackTitle}: ${eval.detailedExplanation}"
          )
        )
        speechManager.speakWithRate(nextPartnerLine.substringBefore("(").trim(), languageCode, 0.95f)
      }
      isPartnerThinking = false
      completedUserTurns += 1
      if (eval.isPassed || completedUserTurns >= 2) {
        onMissionCompleted(eval, clean)
      }
    }
  }

  // Auto-populate recognized speech when the user tapped the Roleplay microphone
  LaunchedEffect(spokenText, isListening) {
    if (micInitiatedInRoleplay && spokenText.isNotBlank() && !isListening) {
      micInitiatedInRoleplay = false
      draftReply = spokenText
      submitRoleplayTurn(spokenText)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("roleplay_mission_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 10 Real-Life Scenarios Horizontal Selector Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      allScenarios.forEach { scenario ->
        val isSelected = scenario.id == activeScenario.id
        Surface(
          color = if (isSelected) accentColor else Color(0xFF131D3B),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.14f)),
          modifier = Modifier
            .clickable { activeScenario = scenario }
            .testTag("roleplay_scenario_chip_${scenario.id}")
        ) {
          Text(
            text = scenario.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    // Mission Header & Objectives Checklist
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${activeScenario.title} • ${activeScenario.partnerRole}",
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Turn ${completedUserTurns.coerceAtMost(3)}/2",
            color = Color(0xFF00E676),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = activeScenario.settingSummary,
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp
        )
        activeScenario.missionObjectives.forEachIndexed { idx, obj ->
          val done = completedUserTurns > idx
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = if (done) Color(0xFF00E676) else Color(0xFF64748B),
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = obj,
              color = if (done) Color(0xFF00E676) else Color(0xFF94A3B8),
              fontSize = 11.sp,
              fontWeight = if (done) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }
    }

    // Conversation Turn Bubbles
    chatHistory.forEachIndexed { idx, turn ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
      ) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (turn.isUser) accentColor.copy(alpha = 0.24f) else Color(0xFF152042)
          ),
          border = BorderStroke(
            1.dp,
            if (turn.isUser) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
          ),
          modifier = Modifier.fillMaxWidth(0.90f)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = turn.speakerName,
                color = if (turn.isUser) accentColor else Color(0xFF80D8FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              if (!turn.isUser) {
                IconButton(
                  onClick = { speechManager.speakWithRate(turn.text.substringBefore("(").trim(), languageCode, 0.95f) },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF80D8FF), modifier = Modifier.size(16.dp))
                }
              }
            }
            Text(
              text = turn.text,
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold
            )
            if (turn.feedbackSubtext.isNotBlank()) {
              Text(
                text = turn.feedbackSubtext,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }

    if (isPartnerThinking) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
      ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 2.dp)
        Text("${activeScenario.partnerRole} is responding...", color = Color(0xFF94A3B8), fontSize = 12.sp)
      }
    }

    // Voice & Text Input Bar for Roleplay Mission
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = {
          micInitiatedInRoleplay = true
          onToggleMic()
        },
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(if (isListening) Color(0xFFFF5252) else accentColor.copy(alpha = 0.25f))
          .testTag("speaking_game_mic_button")
      ) {
        Icon(
          imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
          contentDescription = "Speak in Roleplay",
          tint = Color.White
        )
      }

      OutlinedTextField(
        value = if (isListening && partialSpokenText.isNotBlank()) partialSpokenText else draftReply,
        onValueChange = { draftReply = it },
        placeholder = {
          Text(
            "Speak or type your reply to ${activeScenario.partnerRole}...",
            color = Color(0xFF64748B),
            fontSize = 12.sp
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("roleplay_mission_input"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = accentColor,
          unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        )
      )

      Button(
        onClick = { submitRoleplayTurn(draftReply) },
        enabled = draftReply.trim().length >= 2 && !isPartnerThinking,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        modifier = Modifier
          .height(48.dp)
          .testTag("roleplay_mission_send_btn")
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
      }
    }
  }
}
