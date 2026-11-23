package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.speech.SpeechManager
import kotlin.math.roundToInt

/**
 * Universal 8-state 3D Emoji Voice states extracted directly from the user's reference design.
 */
enum class EmojiVoiceState(
  val label: String,
  val statusDescription: String,
  @DrawableRes val drawableRes: Int,
  val accentColor: Color
) {
  IDLE(
    label = "Idle",
    statusDescription = "Ready to speak",
    drawableRes = R.drawable.emoji_voice_idle,
    accentColor = Color(0xFF3B82F6)
  ),
  SPEAKING(
    label = "Speaking",
    statusDescription = "Capturing your voice...",
    drawableRes = R.drawable.emoji_voice_speaking,
    accentColor = Color(0xFFFBBF24)
  ),
  LISTENING(
    label = "Listening",
    statusDescription = "Listening for your voice...",
    drawableRes = R.drawable.emoji_voice_listening,
    accentColor = Color(0xFF38BDF8)
  ),
  THINKING(
    label = "Thinking",
    statusDescription = "Preparing response...",
    drawableRes = R.drawable.emoji_voice_thinking,
    accentColor = Color(0xFF60A5FA)
  ),
  REPLYING(
    label = "Replying",
    statusDescription = "Tutor is speaking...",
    drawableRes = R.drawable.emoji_voice_replying,
    accentColor = Color(0xFF38BDF8)
  ),
  PROCESSING(
    label = "Processing",
    statusDescription = "Processing speech...",
    drawableRes = R.drawable.emoji_voice_processing,
    accentColor = Color(0xFF06B6D4)
  ),
  DONE(
    label = "Done",
    statusDescription = "Completed!",
    drawableRes = R.drawable.emoji_voice_done,
    accentColor = Color(0xFF10B981)
  ),
  ERROR(
    label = "Error",
    statusDescription = "Tap to retry",
    drawableRes = R.drawable.emoji_voice_error,
    accentColor = Color(0xFFEF4444)
  )
}

/**
 * Resolves the exact [EmojiVoiceState] from real system microphone, speech recognition,
 * AI processing, and TTS playback states. Never invents fake speaking or listening states.
 */
fun resolveEmojiVoiceState(
  isListening: Boolean,
  isUserSpeaking: Boolean = false,
  isProcessingSpeech: Boolean = false,
  isThinking: Boolean = false,
  isReplying: Boolean = false,
  isDone: Boolean = false,
  hasError: Boolean = false,
  audioLevel: Float = 0f
): EmojiVoiceState {
  return when {
    hasError -> EmojiVoiceState.ERROR
    isProcessingSpeech -> EmojiVoiceState.PROCESSING
    isThinking -> EmojiVoiceState.THINKING
    isReplying -> EmojiVoiceState.REPLYING
    isListening && (isUserSpeaking || audioLevel >= 0.16f) -> EmojiVoiceState.SPEAKING
    isListening -> EmojiVoiceState.LISTENING
    isDone -> EmojiVoiceState.DONE
    else -> EmojiVoiceState.IDLE
  }
}

/**
 * Convenience composable helper that observes [SpeechManager] flows directly and returns
 * the live [EmojiVoiceState] and current audio level.
 */
@Composable
fun rememberLiveEmojiVoiceState(
  speechManager: SpeechManager?,
  isThinking: Boolean = false,
  isDone: Boolean = false,
  hasErrorOverride: Boolean = false
): Pair<EmojiVoiceState, Float> {
  val isListening by speechManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }
  val isUserSpeaking by speechManager?.isUserSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
  val isProcessing by speechManager?.isProcessingSpeech?.collectAsState() ?: remember { mutableStateOf(false) }
  val isSpeaking by speechManager?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
  val rmsLevel by speechManager?.rmsLevel?.collectAsState() ?: remember { mutableFloatStateOf(0f) }
  val speechAperture by speechManager?.speechAperture?.collectAsState() ?: remember { mutableFloatStateOf(0f) }
  val speechError by speechManager?.speechError?.collectAsState() ?: remember { mutableStateOf(null) }

  val effectiveLevel = when {
    isSpeaking -> speechAperture.coerceIn(0.05f, 1f)
    isListening -> rmsLevel.coerceIn(0f, 1f)
    else -> 0f
  }

  val state = resolveEmojiVoiceState(
    isListening = isListening,
    isUserSpeaking = isUserSpeaking,
    isProcessingSpeech = isProcessing,
    isThinking = isThinking,
    isReplying = isSpeaking,
    isDone = isDone,
    hasError = hasErrorOverride || !speechError.isNullOrBlank(),
    audioLevel = effectiveLevel
  )

  return state to effectiveLevel
}

/**
 * Universal reusable EmojiVoice component used across all voice-enabled pages in LingoSphere.
 *
 * Uses ONLY the 8 individual 3D emoji assets extracted from the user's provided design.
 * Animates smoothly based on real microphone amplitude, TTS playback cadence, and processing state.
 */
@Composable
fun EmojiVoice(
  state: EmojiVoiceState,
  size: Dp = 56.dp,
  animated: Boolean = true,
  audioLevel: Float = 0f,
  isRecording: Boolean = state == EmojiVoiceState.LISTENING || state == EmojiVoiceState.SPEAKING,
  isPlaying: Boolean = state == EmojiVoiceState.REPLYING,
  onRetry: (() -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  showStateLabel: Boolean = false,
  modifier: Modifier = Modifier
) {
  val clampedLevel = audioLevel.coerceIn(0f, 1f)
  val infiniteTransition = rememberInfiniteTransition(label = "emoji_voice_transition")

  val pulsePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_phase"
  )

  val spinnerAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "spinner_angle"
  )

  // Real audio-reactive and state-accurate scale
  val targetScale = when {
    !animated -> 1.0f
    state == EmojiVoiceState.SPEAKING && isRecording -> {
      1.0f + (clampedLevel * 0.14f) + (pulsePhase * 0.03f)
    }
    state == EmojiVoiceState.REPLYING && isPlaying -> {
      1.0f + (clampedLevel * 0.12f) + (pulsePhase * 0.04f)
    }
    state == EmojiVoiceState.LISTENING && isRecording -> {
      1.0f + (pulsePhase * 0.04f) + (clampedLevel * 0.05f)
    }
    state == EmojiVoiceState.THINKING || state == EmojiVoiceState.PROCESSING -> {
      0.98f + (pulsePhase * 0.05f)
    }
    state == EmojiVoiceState.DONE -> 1.02f
    else -> 1.0f
  }

  val animatedScale by animateFloatAsState(
    targetValue = targetScale,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "emoji_scale"
  )

  // Subtle vertical movement only when actively speaking, replying, or thinking
  val verticalOffsetPx = when {
    !animated -> 0f
    state == EmojiVoiceState.SPEAKING && isRecording -> -(clampedLevel * 6f)
    state == EmojiVoiceState.REPLYING && isPlaying -> -(clampedLevel * 5f)
    state == EmojiVoiceState.THINKING -> (pulsePhase - 0.5f) * 6f
    else -> 0f
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .semantics { contentDescription = "Voice status: ${state.label}" }
      .testTag("emoji_voice_${state.name}")
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(size)
        .then(
          if (state == EmojiVoiceState.ERROR && onRetry != null) {
            Modifier
              .clip(CircleShape)
              .clickable { onRetry() }
          } else if (onClick != null) {
            Modifier
              .clip(CircleShape)
              .clickable { onClick() }
          } else {
            Modifier
          }
        )
    ) {
      // Subtle audio-reactive ring halo only during active recording, playback, or processing
      if (animated && (
          (state == EmojiVoiceState.SPEAKING && isRecording) ||
            (state == EmojiVoiceState.LISTENING && isRecording) ||
            (state == EmojiVoiceState.REPLYING && isPlaying) ||
            state == EmojiVoiceState.PROCESSING
          )
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val radius = (this.size.minDimension / 2f) * (0.88f + clampedLevel * 0.10f)
          val ringAlpha = when (state) {
            EmojiVoiceState.SPEAKING -> (0.28f + clampedLevel * 0.45f).coerceIn(0.2f, 0.75f)
            EmojiVoiceState.REPLYING -> (0.25f + clampedLevel * 0.45f).coerceIn(0.2f, 0.75f)
            EmojiVoiceState.LISTENING -> (0.18f + pulsePhase * 0.22f)
            EmojiVoiceState.PROCESSING -> 0.45f
            else -> 0f
          }
          if (state == EmojiVoiceState.PROCESSING) {
            drawArc(
              color = state.accentColor.copy(alpha = ringAlpha),
              startAngle = spinnerAngle,
              sweepAngle = 110f,
              useCenter = false,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
              color = state.accentColor.copy(alpha = ringAlpha * 0.7f),
              startAngle = spinnerAngle + 180f,
              sweepAngle = 90f,
              useCenter = false,
              style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
          } else if (ringAlpha > 0.02f) {
            drawCircle(
              color = state.accentColor.copy(alpha = ringAlpha * 0.35f),
              radius = radius,
              style = Stroke(width = (2.dp + (clampedLevel * 3f).dp).toPx())
            )
          }
        }
      }

      Crossfade(
        targetState = state,
        animationSpec = tween(durationMillis = 180),
        label = "emoji_state_crossfade"
      ) { currentState ->
        Image(
          painter = painterResource(id = currentState.drawableRes),
          contentDescription = currentState.label,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .size(size)
            .scale(animatedScale)
            .offset { IntOffset(0, verticalOffsetPx.roundToInt()) }
            .graphicsLayer {
              if (currentState == EmojiVoiceState.SPEAKING && isRecording) {
                rotationZ = (clampedLevel - 0.3f).coerceIn(-0.2f, 0.4f) * 5f
              } else if (currentState == EmojiVoiceState.REPLYING && isPlaying) {
                rotationZ = (pulsePhase - 0.5f) * 4f * (0.4f + clampedLevel)
              }
            }
        )
      }
    }

    if (showStateLabel) {
      Spacer(modifier = Modifier.height(4.dp))
      Surface(
        color = Color(0xFF132347).copy(alpha = 0.92f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, state.accentColor.copy(alpha = 0.45f))
      ) {
        Text(
          text = state.label,
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
      }
    }

    if (state == EmojiVoiceState.ERROR && onRetry != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Surface(
        color = Color(0xFFEF4444).copy(alpha = 0.2f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .clickable { onRetry() }
          .testTag("emoji_voice_retry_badge")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Retry",
            tint = Color(0xFFFCA5A5),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Retry",
            color = Color(0xFFFCA5A5),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}
