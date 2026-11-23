package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ai.AutoScrollContentLength
import com.example.ai.AutoScrollLearningContent
import com.example.ai.AutoScrollLearningService
import com.example.ai.AutoScrollPageSource
import com.example.ai.AutoScrollReadingMode
import com.example.ai.AutoScrollSpeedPreset
import com.example.ai.AutoScrollVocabExplanation
import com.example.ai.SavedReadingProgressState
import com.example.ai.UserAutoScrollPreferences
import com.example.data.model.TutorVoice
import com.example.speech.SpeechManager
import com.example.ui.theme.LingoThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Clean, distraction-free Teleprompter & Listen-and-Follow Studio inspired by smooth vertical
 * teleprompter reels. Supports both full-screen and embedded modes across Reading, Listening,
 * Learn, and Culture Journey.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutoScrollTeleprompterView(
  userId: String,
  content: AutoScrollLearningContent?,
  availableContents: List<AutoScrollLearningContent> = emptyList(),
  categories: List<String> = AutoScrollLearningService.READING_CATEGORIES,
  selectedCategory: String = categories.firstOrNull() ?: "Short Stories",
  selectedCefrLevel: String = "B1",
  isFullScreen: Boolean = true,
  isGenerating: Boolean = false,
  errorMessage: String? = null,
  speechManager: SpeechManager?,
  onSelectContent: (AutoScrollLearningContent) -> Unit = {},
  onSelectCategory: (String) -> Unit = {},
  onSelectCefrLevel: (String) -> Unit = {},
  onRequestGenerateNew: (category: String, cefr: String, length: AutoScrollContentLength, topic: String) -> Unit = { _, _, _, _ -> },
  onToggleFullScreen: (() -> Unit)? = null,
  onSaveVocabToFlashcard: (word: String, meaning: String, phonetic: String) -> Unit = { _, _, _ -> },
  onCompleteActivity: (contentId: String, title: String, scorePercent: Int, xpBonus: Int) -> Unit = { _, _, _, _ -> },
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  // Load persisted preferences for this user
  var prefs by remember(userId) {
    mutableStateOf(AutoScrollLearningService.loadPreferences(context, userId))
  }

  fun updatePreferences(transform: (UserAutoScrollPreferences) -> UserAutoScrollPreferences) {
    val updated = transform(prefs)
    prefs = updated
    AutoScrollLearningService.savePreferences(context, userId, updated)
  }

  val isDark = LingoThemeController.isDarkActive

  // Soft Mint / Serene Ivory in Light Mode, Deep Calm Slate/Emerald Gradient in Dark Mode
  val backgroundBrush = remember(isDark) {
    if (isDark) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFF071518),
          Color(0xFF0B2126),
          Color(0xFF07131A)
        )
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFFEAFBF3),
          Color(0xFFF5FCF9),
          Color(0xFFEFFAF5)
        )
      )
    }
  }

  val primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0D2622)
  val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF3E615B)
  val accentColor = if (isDark) Color(0xFF34D399) else Color(0xFF0D9488)
  val activeHighlightBg = if (isDark) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFF10B981).copy(alpha = 0.14f)
  val activeHighlightBorder = if (isDark) Color(0xFF34D399) else Color(0xFF0D9488)
  val surfaceCardColor = if (isDark) Color(0xFF0F2930).copy(alpha = 0.92f) else Color(0xFFFFFFFF).copy(alpha = 0.92f)
  val subtleBorderColor = if (isDark) Color(0xFF1E4047) else Color(0xFFC7E9DC)

  val scrollState = rememberScrollState()
  val sentenceYOffsets = remember(content?.id) { mutableStateMapOf<Int, Int>() }

  // Restore saved progress for this contentId
  val savedProgress = remember(userId, content?.id) {
    content?.id?.let { AutoScrollLearningService.loadReadingProgress(context, userId, it) }
  }

  var activeSentenceIndex by remember(content?.id) {
    mutableIntStateOf(
      (savedProgress?.activeSentenceIndex ?: 0).coerceIn(
        0,
        ((content?.sentences?.size ?: 1) - 1).coerceAtLeast(0)
      )
    )
  }
  var isPlaying by remember(content?.id) { mutableStateOf(false) }
  var pausedByManualTouch by remember(content?.id) { mutableStateOf(false) }
  var hasReachedEnd by remember(content?.id) { mutableStateOf(false) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showQuizAndVocabSheet by remember { mutableStateOf(false) }
  var selectedVocabWord by remember(content?.id) { mutableStateOf<AutoScrollVocabExplanation?>(null) }
  var savedFlashcardWords by remember { mutableStateOf(setOf<String>()) }
  var customTopicInput by remember { mutableStateOf("") }

  var quizAnswers by remember(content?.id) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
  var quizSubmitted by remember(content?.id) { mutableStateOf(savedProgress?.completed == true) }

  val isSpeaking by speechManager?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
  val speakingCharRange by speechManager?.speakingCharRange?.collectAsState() ?: remember { mutableStateOf(null) }
  val speakingUtteranceText by speechManager?.speakingUtteranceText?.collectAsState() ?: remember { mutableStateOf("") }

  // Restore scroll offset once layout is ready
  LaunchedEffect(content?.id) {
    val initialScroll = savedProgress?.scrollOffsetPx ?: 0
    if (initialScroll > 0) {
      delay(120L)
      try {
        scrollState.scrollTo(initialScroll.coerceAtMost(scrollState.maxValue))
      } catch (_: Exception) {}
    } else {
      try {
        scrollState.scrollTo(0)
      } catch (_: Exception) {}
    }
  }

  // Persist current reading position periodically and whenever paused
  val currentContentState by rememberUpdatedState(content)
  val currentSentenceIdxState by rememberUpdatedState(activeSentenceIndex)
  val currentQuizSubmittedState by rememberUpdatedState(quizSubmitted)

  fun persistCurrentPositionNow() {
    val c = currentContentState ?: return
    AutoScrollLearningService.saveReadingProgress(
      context = context,
      userId = userId,
      state = SavedReadingProgressState(
        contentId = c.id,
        pageSource = c.pageSource.id,
        scrollOffsetPx = scrollState.value,
        activeSentenceIndex = currentSentenceIdxState,
        completed = currentQuizSubmittedState || hasReachedEnd,
        quizScorePercent = savedProgress?.quizScorePercent ?: 100
      )
    )
  }

  // Pause automatically if app goes to background or screen is disposed
  DisposableEffect(lifecycleOwner, content?.id) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        if (isPlaying) {
          isPlaying = false
        }
        speechManager?.stopSpeaking()
        persistCurrentPositionNow()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      speechManager?.stopSpeaking()
      persistCurrentPositionNow()
    }
  }

  if (isFullScreen) {
    BackHandler {
      isPlaying = false
      speechManager?.stopSpeaking()
      persistCurrentPositionNow()
      onBack()
    }
  }

  // Helper to speak a specific sentence in LISTEN_AND_FOLLOW mode
  val currentPrefsState by rememberUpdatedState(prefs)
  val currentIsPlayingState by rememberUpdatedState(isPlaying)

  fun speakSentenceAtIndex(sentenceIdx: Int, autoAdvanceOnDone: Boolean) {
    val c = currentContentState ?: return
    val seg = c.sentences.getOrNull(sentenceIdx) ?: return
    val selectedVoice = TutorVoice.findById(currentPrefsState.preferredVoiceId)
    speechManager?.speakWithTutorVoice(
      text = seg.text,
      voice = selectedVoice,
      languageCode = c.languageCode,
      speedOverride = currentPrefsState.voiceSpeedMultiplier,
      onDone = {
        if (autoAdvanceOnDone &&
          currentIsPlayingState &&
          currentPrefsState.readingMode == AutoScrollReadingMode.LISTEN_AND_FOLLOW
        ) {
          val nextIdx = sentenceIdx + 1
          if (nextIdx < c.sentences.size) {
            activeSentenceIndex = nextIdx
          } else {
            isPlaying = false
            hasReachedEnd = true
            persistCurrentPositionNow()
          }
        }
      }
    )
  }

  // Trigger TTS when playing in LISTEN_AND_FOLLOW mode and activeSentenceIndex changes
  LaunchedEffect(isPlaying, activeSentenceIndex, prefs.readingMode, prefs.preferredVoiceId, prefs.voiceSpeedMultiplier, content?.id) {
    val c = content ?: return@LaunchedEffect
    if (isPlaying && prefs.readingMode == AutoScrollReadingMode.LISTEN_AND_FOLLOW) {
      hasReachedEnd = false
      speakSentenceAtIndex(activeSentenceIndex, autoAdvanceOnDone = true)
    } else if (!isPlaying) {
      speechManager?.stopSpeaking()
    }
  }

  // Smooth frame-based scrolling engine (60/120fps withFrameNanos, zero jump/flicker)
  LaunchedEffect(
    isPlaying,
    prefs.autoScrollEnabled,
    prefs.readingMode,
    prefs.customSpeedPxPerSec,
    activeSentenceIndex,
    content?.id
  ) {
    val c = content ?: return@LaunchedEffect
    if (!isPlaying || !prefs.autoScrollEnabled) return@LaunchedEffect

    var lastFrameNanos = 0L
    var subPixelRemainder = 0f

    while (isActive && isPlaying && prefs.autoScrollEnabled) {
      withFrameNanos { frameNanos ->
        if (lastFrameNanos == 0L) {
          lastFrameNanos = frameNanos
          return@withFrameNanos
        }
        val dtSeconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNanos = frameNanos

        if (prefs.readingMode == AutoScrollReadingMode.AUTO_READ) {
          // Mode A: Continuous upward teleprompter scroll at exact px/sec speed
          val exactDelta = (prefs.customSpeedPxPerSec * dtSeconds) + subPixelRemainder
          val intDelta = exactDelta.toInt()
          subPixelRemainder = exactDelta - intDelta

          if (intDelta > 0) {
            val consumed = scrollState.dispatchRawDelta(intDelta.toFloat())
            // Update activeSentenceIndex to match the sentence currently in the focal zone
            val focalY = scrollState.value + 160
            val closestIdx = sentenceYOffsets.entries
              .minByOrNull { abs(it.value - focalY) }
              ?.key
            if (closestIdx != null && closestIdx != activeSentenceIndex) {
              activeSentenceIndex = closestIdx
            }

            if (scrollState.value >= scrollState.maxValue && scrollState.maxValue > 0 && consumed < 0.5f) {
              isPlaying = false
              hasReachedEnd = true
              persistCurrentPositionNow()
            }
          }
        } else {
          // Mode B: Listen & Follow — smoothly glide toward the currently spoken sentence's Y position
          val targetY = (sentenceYOffsets[activeSentenceIndex] ?: 0) - 90
          val clampedTarget = targetY.coerceIn(0, scrollState.maxValue.coerceAtLeast(0))
          val diff = (clampedTarget - scrollState.value).toFloat()
          if (abs(diff) > 1.5f) {
            // Smooth proportional velocity bounded by user's scroll speed setting
            val maxVel = (prefs.customSpeedPxPerSec * 2.4f).coerceAtLeast(95f)
            val step = (diff * 4.2f * dtSeconds).coerceIn(-maxVel * dtSeconds, maxVel * dtSeconds) + subPixelRemainder
            val intStep = step.toInt()
            subPixelRemainder = step - intStep
            if (intStep != 0) {
              scrollState.dispatchRawDelta(intStep.toFloat())
            }
          }
        }
      }
    }
  }

  val totalSentences = content?.sentences?.size?.coerceAtLeast(1) ?: 1
  val progressFraction by remember(activeSentenceIndex, totalSentences, scrollState.value, scrollState.maxValue, prefs.readingMode) {
    derivedStateOf {
      if (prefs.readingMode == AutoScrollReadingMode.AUTO_READ && scrollState.maxValue > 0) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
      } else {
        ((activeSentenceIndex + 1).toFloat() / totalSentences.toFloat()).coerceIn(0f, 1f)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .then(
        if (isFullScreen) {
          Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
        } else {
          Modifier
        }
      )
      .testTag("autoscroll_teleprompter_root")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 760.dp)
        .align(Alignment.Center)
    ) {
      // =======================================================================
      // 1. MINIMAL TOP HEADER: Back Button | Content Title | Settings Control
      // =======================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = {
            isPlaying = false
            speechManager?.stopSpeaking()
            persistCurrentPositionNow()
            onBack()
          },
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(surfaceCardColor)
            .testTag("teleprompter_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = primaryTextColor
          )
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = content?.title ?: "Auto-Scrolling Studio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "${content?.cefrLevel ?: selectedCefrLevel} • ${content?.category ?: selectedCategory}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = accentColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (pausedByManualTouch) {
              Text(
                text = "• Manual Scroll",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (onToggleFullScreen != null) {
            IconButton(
              onClick = onToggleFullScreen,
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(surfaceCardColor)
                .testTag("teleprompter_fullscreen_button")
            ) {
              Icon(
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullScreen) "Exit full screen" else "Full screen reading",
                tint = primaryTextColor
              )
            }
          }

          IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(surfaceCardColor)
              .testTag("teleprompter_settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Reading settings",
              tint = accentColor
            )
          }
        }
      }

      // Subtle thin progress bar
      LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp),
        color = accentColor,
        trackColor = subtleBorderColor.copy(alpha = 0.45f)
      )

      // Loading or Retry Banner (clean, non-technical)
      AnimatedVisibility(visible = isGenerating || !errorMessage.isNullOrBlank()) {
        Surface(
          color = surfaceCardColor,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, subtleBorderColor),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          if (isGenerating) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = accentColor
              )
              Text(
                text = "Preparing a fresh ${selectedCategory.lowercase()} lesson for your level...",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor
              )
            }
          } else if (!errorMessage.isNullOrBlank()) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = primaryTextColor,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(8.dp))
              FilledTonalButtonOrPill(
                label = "Retry",
                accentColor = accentColor,
                onClick = {
                  onRequestGenerateNew(
                    selectedCategory,
                    selectedCefrLevel,
                    prefs.preferredLength,
                    customTopicInput
                  )
                }
              )
            }
          }
        }
      }

      // =======================================================================
      // 2. MAIN TELEPROMPTER SCROLLING AREA (TOUCH-TO-PAUSE + SENTENCE FOCUS)
      // =======================================================================
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          // Detect manual touch/drag on the text and pause auto-scroll immediately so user never fights it
          .pointerInput(Unit) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
              var totalDragY = 0f
              do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change != null) {
                  val dy = change.position.y - change.previousPosition.y
                  totalDragY += abs(dy)
                  if (totalDragY > 14f && isPlaying) {
                    isPlaying = false
                    pausedByManualTouch = true
                    speechManager?.stopSpeaking()
                  }
                }
              } while (event.changes.any { it.pressed })
            }
          }
      ) {
        if (content == null || content.sentences.isEmpty()) {
          // Empty state with one-tap generate button
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "Ready to Read & Listen",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = primaryTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Generate a fresh story or dialogue tailored to your language and proficiency level.",
              style = MaterialTheme.typography.bodyMedium,
              color = secondaryTextColor,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
              onClick = {
                onRequestGenerateNew(
                  selectedCategory,
                  selectedCefrLevel,
                  prefs.preferredLength,
                  customTopicInput
                )
              },
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
              ),
              modifier = Modifier.testTag("teleprompter_empty_generate_btn")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("New Lesson", fontWeight = FontWeight.ExtraBold)
            }
          }
        } else if (content.pageSource == AutoScrollPageSource.LISTENING && !prefs.showTranscriptInListening) {
          // Blind Listening Mode (Transcript Hidden by Learner Choice)
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = accentColor.copy(alpha = 0.16f),
              modifier = Modifier.size(88.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.GraphicEq,
                  contentDescription = null,
                  tint = accentColor,
                  modifier = Modifier.size(44.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = "Focused Audio Mode",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = primaryTextColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Sentence ${activeSentenceIndex + 1} of ${content.sentences.size}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = accentColor
            )
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedButton(
              onClick = {
                updatePreferences { it.copy(showTranscriptInListening = true) }
              },
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, accentColor)
            ) {
              Icon(Icons.Default.Visibility, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Show Synchronized Transcript", color = accentColor, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 22.dp, vertical = 28.dp)
              .testTag("teleprompter_scroll_column"),
            verticalArrangement = Arrangement.spacedBy((prefs.fontSizeSp * 0.65f).dp)
          ) {
            // Top breathing room for teleprompter entry
            Spacer(modifier = Modifier.height(24.dp))

            content.sentences.forEachIndexed { idx, segment ->
              val isFocused = idx == activeSentenceIndex
              val isCurrentlySpoken = isFocused &&
                isSpeaking &&
                speakingUtteranceText.trim() == segment.text.trim()

              val annotatedSentence = remember(
                segment.text,
                content.vocabulary,
                isCurrentlySpoken,
                speakingCharRange,
                primaryTextColor,
                accentColor
              ) {
                buildAnnotatedString {
                  val full = segment.text
                  val range = if (isCurrentlySpoken) speakingCharRange else null
                  if (range != null && range.first >= 0 && range.last < full.length) {
                    append(full.substring(0, range.first))
                    withStyle(
                      SpanStyle(
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        background = accentColor.copy(alpha = 0.18f)
                      )
                    ) {
                      append(full.substring(range.first, (range.last + 1).coerceAtMost(full.length)))
                    }
                    if (range.last + 1 < full.length) {
                      append(full.substring(range.last + 1))
                    }
                  } else {
                    append(full)
                  }
                }
              }

              Surface(
                color = if (isFocused) activeHighlightBg else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
                border = if (isFocused) BorderStroke(1.5.dp, activeHighlightBorder.copy(alpha = 0.65f)) else null,
                modifier = Modifier
                  .fillMaxWidth()
                  .onGloballyPositioned { coords ->
                    sentenceYOffsets[idx] = coords.positionInParent().y.toInt()
                  }
                  .clickable {
                    activeSentenceIndex = idx
                    pausedByManualTouch = false
                    if (prefs.readingMode == AutoScrollReadingMode.LISTEN_AND_FOLLOW) {
                      speakSentenceAtIndex(idx, autoAdvanceOnDone = isPlaying)
                    } else {
                      speechManager?.speakWithTutorVoice(
                        text = segment.text,
                        voice = TutorVoice.findById(prefs.preferredVoiceId),
                        languageCode = content.languageCode,
                        speedOverride = prefs.voiceSpeedMultiplier
                      )
                    }
                  }
                  .testTag("teleprompter_sentence_$idx")
              ) {
                Column(
                  modifier = Modifier.padding(
                    horizontal = if (isFocused) 16.dp else 8.dp,
                    vertical = if (isFocused) 14.dp else 6.dp
                  )
                ) {
                  if (segment.speaker.isNotBlank()) {
                    Text(
                      text = segment.speaker.uppercase(),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = accentColor,
                      modifier = Modifier.padding(bottom = 4.dp)
                    )
                  }

                  Text(
                    text = annotatedSentence,
                    fontSize = prefs.fontSizeSp.sp,
                    lineHeight = (prefs.fontSizeSp * prefs.lineSpacingMultiplier).sp,
                    fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isFocused) {
                      primaryTextColor
                    } else {
                      primaryTextColor.copy(alpha = 0.68f)
                    }
                  )

                  if (prefs.showTranslations && segment.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = segment.translation,
                      fontSize = (prefs.fontSizeSp - 5).coerceAtLeast(13).sp,
                      lineHeight = ((prefs.fontSizeSp - 5).coerceAtLeast(13) * 1.35f).sp,
                      fontWeight = FontWeight.Medium,
                      color = secondaryTextColor
                    )
                  }
                }
              }
            }

            // Difficult Words & Comprehension Quiz Trigger at the bottom of the teleprompter scroll
            Spacer(modifier = Modifier.height(16.dp))

            if (content.vocabulary.isNotEmpty()) {
              Surface(
                color = surfaceCardColor,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, subtleBorderColor),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    text = "Tap any key word to explain & hear pronunciation",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    content.vocabulary.forEach { vocab ->
                      val isSelected = selectedVocabWord?.word == vocab.word
                      Surface(
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) accentColor else subtleBorderColor),
                        modifier = Modifier.clickable {
                          selectedVocabWord = vocab
                          speechManager?.speak(vocab.word, content.languageCode)
                        }
                      ) {
                        Text(
                          text = vocab.word,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = primaryTextColor,
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                      }
                    }
                  }

                  AnimatedVisibility(visible = selectedVocabWord != null) {
                    selectedVocabWord?.let { vocab ->
                      val isSaved = savedFlashcardWords.contains("${content.id}_${vocab.word}")
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(top = 12.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(activeHighlightBg)
                          .padding(12.dp)
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "${vocab.word} ${if (vocab.phonetic.isNotBlank()) "/${vocab.phonetic}/" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryTextColor
                          )
                          IconButton(
                            onClick = { selectedVocabWord = null },
                            modifier = Modifier.size(28.dp)
                          ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = secondaryTextColor, modifier = Modifier.size(16.dp))
                          }
                        }
                        Text(
                          text = vocab.meaningInNative,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.SemiBold,
                          color = primaryTextColor
                        )
                        if (vocab.exampleSentence.isNotBlank()) {
                          Spacer(modifier = Modifier.height(4.dp))
                          Text(
                            text = "“${vocab.exampleSentence}”",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                          )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButtonOrPill(
                          label = if (isSaved) "Saved to Flashcards ✓" else "+ Save to Flashcards",
                          accentColor = accentColor,
                          onClick = {
                            if (!isSaved) {
                              onSaveVocabToFlashcard(vocab.word, vocab.meaningInNative, vocab.phonetic)
                              savedFlashcardWords = savedFlashcardWords + "${content.id}_${vocab.word}"
                            }
                          }
                        )
                      }
                    }
                  }
                }
              }
            }

            if (content.questions.isNotEmpty()) {
              Button(
                onClick = { showQuizAndVocabSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = accentColor,
                  contentColor = Color.White
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("teleprompter_open_quiz_btn")
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (quizSubmitted) "Review Comprehension Quiz ✓" else "Take Comprehension Quiz (+40 XP)",
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }

            // Generous bottom padding so the final sentences can scroll smoothly to the center of the screen
            Spacer(modifier = Modifier.height(220.dp))
          }
        }
      }

      // =======================================================================
      // 3. CLEAN BOTTOM CONTROL BAR: Play/Pause | Speed | Voice Toggle | Settings
      // =======================================================================
      Surface(
        color = surfaceCardColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, subtleBorderColor),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("teleprompter_bottom_controls")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // 1. Play / Pause / Resume + Restart
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                if (isPlaying) {
                  isPlaying = false
                  speechManager?.stopSpeaking()
                  persistCurrentPositionNow()
                } else {
                  pausedByManualTouch = false
                  if (hasReachedEnd ||
                    (content != null && activeSentenceIndex >= content.sentences.size - 1 && scrollState.value >= (scrollState.maxValue - 20).coerceAtLeast(1))
                  ) {
                    hasReachedEnd = false
                    activeSentenceIndex = 0
                    coroutineScope.launch { scrollState.scrollTo(0) }
                  }
                  isPlaying = true
                }
              },
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
              ),
              contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
              modifier = Modifier
                .height(48.dp)
                .testTag("teleprompter_play_pause_button")
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when {
                  isPlaying -> "Pause"
                  pausedByManualTouch || activeSentenceIndex > 0 || scrollState.value > 30 -> "Resume"
                  else -> "Play"
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
              )
            }

            // Compact Restart button
            IconButton(
              onClick = {
                isPlaying = false
                pausedByManualTouch = false
                hasReachedEnd = false
                speechManager?.stopSpeaking()
                activeSentenceIndex = 0
                coroutineScope.launch {
                  scrollState.scrollTo(0)
                  persistCurrentPositionNow()
                }
              },
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(subtleBorderColor.copy(alpha = 0.35f))
                .testTag("teleprompter_restart_button")
            ) {
              Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Restart from beginning",
                tint = primaryTextColor,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // 2. Speed Cycle Pill (Very Slow -> Slow -> Normal -> Fast -> Very Fast)
          Surface(
            color = subtleBorderColor.copy(alpha = 0.35f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
              .clickable {
                val presets = AutoScrollSpeedPreset.entries
                val nextIdx = (presets.indexOf(prefs.speedPreset) + 1) % presets.size
                val nextPreset = presets[nextIdx]
                val nextVoiceMult = when (nextPreset) {
                  AutoScrollSpeedPreset.VERY_SLOW -> 0.75f
                  AutoScrollSpeedPreset.SLOW -> 0.88f
                  AutoScrollSpeedPreset.NORMAL -> 1.0f
                  AutoScrollSpeedPreset.FAST -> 1.15f
                  AutoScrollSpeedPreset.VERY_FAST -> 1.3f
                }
                updatePreferences {
                  it.copy(
                    speedPreset = nextPreset,
                    customSpeedPxPerSec = nextPreset.pixelsPerSecond,
                    voiceSpeedMultiplier = nextVoiceMult
                  )
                }
              }
              .testTag("teleprompter_speed_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Scroll speed",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = prefs.speedPreset.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = primaryTextColor
              )
            }
          }

          // 3. Voice / Reading Mode Toggle (Listen & Follow vs Auto Read without audio)
          val isVoiceActive = prefs.readingMode == AutoScrollReadingMode.LISTEN_AND_FOLLOW
          Surface(
            color = if (isVoiceActive) accentColor.copy(alpha = 0.18f) else subtleBorderColor.copy(alpha = 0.35f),
            shape = RoundedCornerShape(20.dp),
            border = if (isVoiceActive) BorderStroke(1.dp, accentColor) else null,
            modifier = Modifier
              .clickable {
                val nextMode = if (isVoiceActive) {
                  speechManager?.stopSpeaking()
                  AutoScrollReadingMode.AUTO_READ
                } else {
                  AutoScrollReadingMode.LISTEN_AND_FOLLOW
                }
                updatePreferences { it.copy(readingMode = nextMode) }
              }
              .testTag("teleprompter_voice_mode_toggle")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isVoiceActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = "Toggle AI voice narration",
                tint = if (isVoiceActive) accentColor else secondaryTextColor,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = if (isVoiceActive) "Voice On" else "Read Only",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isVoiceActive) accentColor else primaryTextColor
              )
            }
          }
        }
      }
    }
  }

  // ===========================================================================
  // 4. SECONDARY CONTROLS & AI CONTENT GENERATOR SHEET (HIDDEN INSIDE SETTINGS)
  // ===========================================================================
  if (showSettingsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = if (isDark) Color(0xFF0E1D24) else Color(0xFFFFFFFF)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Reading & Auto-Scroll Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = primaryTextColor
          )
          IconButton(onClick = { showSettingsSheet = false }) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = secondaryTextColor)
          }
        }

        // Auto Scroll ON/OFF Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Auto Scroll",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              color = primaryTextColor
            )
            Text(
              text = "Automatically move text upward during reading or narration",
              style = MaterialTheme.typography.bodySmall,
              color = secondaryTextColor
            )
          }
          Switch(
            checked = prefs.autoScrollEnabled,
            onCheckedChange = { enabled ->
              updatePreferences { it.copy(autoScrollEnabled = enabled) }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
            modifier = Modifier.testTag("settings_autoscroll_switch")
          )
        }

        // Reading Mode Selection (Mode A: Auto Read vs Mode B: Listen & Follow)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Mode",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            AutoScrollReadingMode.entries.forEach { mode ->
              val selected = prefs.readingMode == mode
              FilterChip(
                selected = selected,
                onClick = {
                  if (mode == AutoScrollReadingMode.AUTO_READ) {
                    speechManager?.stopSpeaking()
                  }
                  updatePreferences { it.copy(readingMode = mode) }
                },
                label = {
                  Text(
                    text = if (mode == AutoScrollReadingMode.AUTO_READ) "Auto Read (Silent)" else "Listen & Follow (AI Voice)",
                    fontWeight = FontWeight.Bold
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = accentColor.copy(alpha = 0.2f),
                  selectedLabelColor = accentColor
                ),
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Scroll Speed Presets + Fine Slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Scrolling Speed",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = primaryTextColor
            )
            Text(
              text = "${prefs.speedPreset.label} (${prefs.customSpeedPxPerSec.toInt()} px/s)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = accentColor
            )
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AutoScrollSpeedPreset.entries.forEach { preset ->
              FilterChip(
                selected = prefs.speedPreset == preset,
                onClick = {
                  updatePreferences {
                    it.copy(
                      speedPreset = preset,
                      customSpeedPxPerSec = preset.pixelsPerSecond
                    )
                  }
                },
                label = { Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = accentColor.copy(alpha = 0.2f),
                  selectedLabelColor = accentColor
                )
              )
            }
          }
          Slider(
            value = prefs.customSpeedPxPerSec,
            onValueChange = { newPx ->
              updatePreferences { it.copy(customSpeedPxPerSec = newPx) }
            },
            valueRange = 16f..130f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
          )
        }

        // Adjustable Text Size (16sp to 34sp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Text Size",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = primaryTextColor
            )
            Text(
              text = "${prefs.fontSizeSp} sp",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = accentColor
            )
          }
          Slider(
            value = prefs.fontSizeSp.toFloat(),
            onValueChange = { sz ->
              updatePreferences { it.copy(fontSizeSp = sz.toInt()) }
            },
            valueRange = 16f..34f,
            steps = 8,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
          )
        }

        // Selectable Natural AI Voices (Male & Female) + Voice Speed
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "AI Narrator Voice",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = primaryTextColor
            )
            Text(
              text = "Voice Speed: ${String.format("%.2fx", prefs.voiceSpeedMultiplier)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor
            )
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TutorVoice.entries.forEach { voice ->
              val isSelected = prefs.preferredVoiceId == voice.id
              FilterChip(
                selected = isSelected,
                onClick = {
                  updatePreferences { it.copy(preferredVoiceId = voice.id) }
                  speechManager?.setAudiobookVoice(voice)
                  speechManager?.previewVoice(
                    voice = voice,
                    languageCode = content?.languageCode ?: "en"
                  )
                },
                label = {
                  Text(
                    text = "${voice.id} (${voice.gender})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = accentColor.copy(alpha = 0.2f),
                  selectedLabelColor = accentColor
                )
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(0.75f to "0.75x Slow", 0.9f to "0.9x Clear", 1.0f to "1.0x Natural", 1.2f to "1.2x Fast").forEach { (rate, label) ->
              FilterChip(
                selected = abs(prefs.voiceSpeedMultiplier - rate) < 0.04f,
                onClick = {
                  updatePreferences { it.copy(voiceSpeedMultiplier = rate) }
                },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = accentColor.copy(alpha = 0.2f),
                  selectedLabelColor = accentColor
                )
              )
            }
          }
        }

        // Parallel Translations & Listening Transcript Visibility
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Show Sentence Translations",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor
          )
          Switch(
            checked = prefs.showTranslations,
            onCheckedChange = { show -> updatePreferences { it.copy(showTranslations = show) } },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
          )
        }

        if (content?.pageSource == AutoScrollPageSource.LISTENING) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Show Listening Transcript",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = primaryTextColor
            )
            Switch(
              checked = prefs.showTranscriptInListening,
              onCheckedChange = { show -> updatePreferences { it.copy(showTranscriptInListening = show) } },
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
            )
          }
        }

        HorizontalDivider(color = subtleBorderColor)

        // Generate Fresh Content Section (Category, CEFR Level, Length Short/Medium/Long, Topic)
        Text(
          text = "Generate New Lesson",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = primaryTextColor
        )

        // Content Length (Short, Medium, Long)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AutoScrollContentLength.entries.forEach { len ->
            FilterChip(
              selected = prefs.preferredLength == len,
              onClick = { updatePreferences { it.copy(preferredLength = len) } },
              label = { Text("${len.label} (~${len.estimatedMinutes}m)", fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                selectedLabelColor = accentColor
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        // CEFR Difficulty Selector
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
            FilterChip(
              selected = selectedCefrLevel.equals(level, ignoreCase = true),
              onClick = { onSelectCefrLevel(level) },
              label = { Text(level, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                selectedLabelColor = accentColor
              )
            )
          }
        }

        // Category Selector
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory.equals(cat, ignoreCase = true),
              onClick = { onSelectCategory(cat) },
              label = { Text(cat, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                selectedLabelColor = accentColor
              )
            )
          }
        }

        OutlinedTextField(
          value = customTopicInput,
          onValueChange = { customTopicInput = it },
          label = { Text("Optional Topic or Situation") },
          placeholder = { Text("e.g., Ordering coffee in Paris, Morning motivation...") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = {
            showSettingsSheet = false
            isPlaying = false
            speechManager?.stopSpeaking()
            onRequestGenerateNew(
              selectedCategory,
              selectedCefrLevel,
              prefs.preferredLength,
              customTopicInput
            )
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("settings_generate_new_content_btn")
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Generate Fresh Lesson", fontWeight = FontWeight.ExtraBold)
        }

        // Saved / Recent Lessons Switcher
        if (availableContents.size > 1) {
          Text(
            text = "Saved & Recent Lessons (${availableContents.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            availableContents.take(10).forEach { item ->
              val isCurrent = item.id == content?.id
              Surface(
                color = if (isCurrent) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isCurrent) accentColor else subtleBorderColor),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    isPlaying = false
                    speechManager?.stopSpeaking()
                    onSelectContent(item)
                    showSettingsSheet = false
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = primaryTextColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${item.cefrLevel} • ${item.category} • ${item.sentences.size} sentences",
                      style = MaterialTheme.typography.labelSmall,
                      color = secondaryTextColor
                    )
                  }
                  if (isCurrent) {
                    Text(
                      text = "Active",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = accentColor
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

  // ===========================================================================
  // 5. COMPREHENSION QUIZ MODAL SHEET
  // ===========================================================================
  if (showQuizAndVocabSheet && content != null) {
    ModalBottomSheet(
      onDismissRequest = { showQuizAndVocabSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = if (isDark) Color(0xFF0E1D24) else Color(0xFFFFFFFF)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Comprehension Check • ${content.title}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = primaryTextColor
        )

        content.questions.forEachIndexed { qIdx, q ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceCardColor),
            border = BorderStroke(1.dp, subtleBorderColor),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "${qIdx + 1}. ${q.question}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
              )
              q.options.forEachIndexed { optIdx, opt ->
                val isChosen = quizAnswers[qIdx] == optIdx
                val isCorrect = optIdx == q.correctIndex
                val rowColor = when {
                  quizSubmitted && isCorrect -> Color(0xFF10B981).copy(alpha = 0.22f)
                  quizSubmitted && isChosen && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.2f)
                  isChosen -> accentColor.copy(alpha = 0.18f)
                  else -> Color.Transparent
                }
                Surface(
                  color = rowColor,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(
                    1.dp,
                    when {
                      quizSubmitted && isCorrect -> Color(0xFF10B981)
                      isChosen -> accentColor
                      else -> subtleBorderColor
                    }
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !quizSubmitted) {
                      quizAnswers = quizAnswers + (qIdx to optIdx)
                    }
                ) {
                  Text(
                    text = opt,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                    color = primaryTextColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                  )
                }
              }
              if (quizSubmitted && q.explanation.isNotBlank()) {
                Text(
                  text = q.explanation,
                  style = MaterialTheme.typography.labelSmall,
                  color = accentColor
                )
              }
            }
          }
        }

        Button(
          onClick = {
            if (!quizSubmitted) {
              quizSubmitted = true
              val correctCount = content.questions.indices.count { i ->
                quizAnswers[i] == content.questions[i].correctIndex
              }
              val scorePct = ((correctCount * 100.0) / content.questions.size.coerceAtLeast(1)).toInt()
              AutoScrollLearningService.saveReadingProgress(
                context = context,
                userId = userId,
                state = SavedReadingProgressState(
                  contentId = content.id,
                  pageSource = content.pageSource.id,
                  scrollOffsetPx = scrollState.value,
                  activeSentenceIndex = activeSentenceIndex,
                  completed = true,
                  quizScorePercent = scorePct
                )
              )
              onCompleteActivity(content.id, content.title, scorePct, 40)
            } else {
              showQuizAndVocabSheet = false
            }
          },
          enabled = quizSubmitted || quizAnswers.size == content.questions.size,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        ) {
          Text(
            text = if (quizSubmitted) "Done ✓" else "Submit Answers",
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }
  }
}

@Composable
private fun FilledTonalButtonOrPill(
  label: String,
  accentColor: Color,
  onClick: () -> Unit
) {
  Surface(
    color = accentColor.copy(alpha = 0.18f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accentColor),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.ExtraBold,
      color = accentColor,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
  }
}

// =============================================================================
// REUSABLE CONVERSATION AUTO-FOLLOW & SYNCHRONIZED SENTENCE HIGHLIGHTING
// (Used by AI Conversation, Roleplay, and Learn)
// =============================================================================

@Composable
fun ConversationFollowControlChip(
  isFollowing: Boolean,
  isPausedByManualScroll: Boolean,
  onToggleFollow: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val accent = Color(0xFF10B981)
  val amber = Color(0xFFF59E0B)
  Surface(
    color = when {
      isPausedByManualScroll -> amber.copy(alpha = 0.2f)
      isFollowing -> accent.copy(alpha = 0.18f)
      else -> Color.White.copy(alpha = 0.08f)
    },
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(
      1.dp,
      when {
        isPausedByManualScroll -> amber
        isFollowing -> accent
        else -> Color.White.copy(alpha = 0.22f)
      }
    ),
    modifier = modifier
      .clickable { onToggleFollow(!isFollowing || isPausedByManualScroll) }
      .testTag("follow_conversation_toggle_chip")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = if (isFollowing && !isPausedByManualScroll) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
        contentDescription = null,
        tint = when {
          isPausedByManualScroll -> amber
          isFollowing -> accent
          else -> Color.White.copy(alpha = 0.75f)
        },
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = when {
          isPausedByManualScroll -> "Follow Paused • Tap to Resume"
          isFollowing -> "Follow Conversation: ON"
          else -> "Follow Conversation: OFF"
        },
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        color = when {
          isPausedByManualScroll -> amber
          isFollowing -> accent
          else -> Color.White.copy(alpha = 0.85f)
        }
      )
    }
  }
}

/**
 * Renders AI message text with live sentence/word highlighting synchronized with `SpeechManager`
 * while the AI tutor or roleplay partner is speaking.
 */
@Composable
fun SynchronizedAiSpeechText(
  text: String,
  speechManager: SpeechManager?,
  baseTextColor: Color = Color.White,
  highlightColor: Color = Color(0xFF34D399),
  fontSizeSp: Int = 15,
  fontWeight: FontWeight = FontWeight.Medium,
  modifier: Modifier = Modifier
) {
  val isSpeaking by speechManager?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
  val speakingUtterance by speechManager?.speakingUtteranceText?.collectAsState() ?: remember { mutableStateOf("") }
  val speakingRange by speechManager?.speakingCharRange?.collectAsState() ?: remember { mutableStateOf(null) }

  val isThisMessageSpeaking = isSpeaking &&
    speakingUtterance.isNotBlank() &&
    (speakingUtterance.trim() == text.trim() || text.contains(speakingUtterance.trim()))

  val annotated = remember(text, isThisMessageSpeaking, speakingRange, baseTextColor, highlightColor) {
    buildAnnotatedString {
      if (!isThisMessageSpeaking) {
        append(text)
      } else {
        val range = speakingRange
        if (range != null && range.first >= 0 && range.last < text.length) {
          // Highlight the entire sentence containing the current spoken word range
          val sentenceStart = text.lastIndexOfAny(charArrayOf('.', '!', '?', '。', '！', '？'), range.first)
            .let { if (it < 0) 0 else (it + 1).coerceAtMost(range.first) }
          val sentenceEnd = text.indexOfAny(charArrayOf('.', '!', '?', '。', '！', '？'), range.last)
            .let { if (it < 0) text.length else (it + 1).coerceAtMost(text.length) }

          append(text.substring(0, sentenceStart))
          withStyle(
            SpanStyle(
              color = highlightColor,
              fontWeight = FontWeight.ExtraBold,
              background = highlightColor.copy(alpha = 0.16f)
            )
          ) {
            append(text.substring(sentenceStart, sentenceEnd))
          }
          if (sentenceEnd < text.length) {
            append(text.substring(sentenceEnd))
          }
        } else {
          withStyle(
            SpanStyle(
              color = highlightColor,
              fontWeight = FontWeight.Bold
            )
          ) {
            append(text)
          }
        }
      }
    }
  }

  Text(
    text = annotated,
    color = baseTextColor,
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp * 1.45f).sp,
    fontWeight = fontWeight,
    modifier = modifier
  )
}
