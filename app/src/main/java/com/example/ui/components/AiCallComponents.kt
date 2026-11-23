package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VoiceCallStatus
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoViolet
import com.example.ui.theme.LingoVioletLight
import kotlin.math.sin

@Composable
fun AiCallAvatar(
  status: VoiceCallStatus,
  rmsLevel: Float,
  emoji: String,
  modifier: Modifier = Modifier,
  size: Dp = 150.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
  
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (status == VoiceCallStatus.TUTOR_SPEAKING) 1.32f else 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (status == VoiceCallStatus.TUTOR_SPEAKING) 600 else 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val ringColor = when (status) {
    VoiceCallStatus.TUTOR_SPEAKING -> LingoViolet
    VoiceCallStatus.USER_SPEAKING -> LingoEmerald
    VoiceCallStatus.LISTENING -> LingoCyan
    VoiceCallStatus.THINKING -> LingoElectricBlue
    VoiceCallStatus.MUTED -> LingoAmber
    else -> Color(0xFF64748B)
  }

  val sophiaTutor = com.example.avatar.TutorPersona.ALL_TUTORS.firstOrNull { it.id == "tutor_sophia" }
    ?: com.example.avatar.TutorPersona.ALL_TUTORS.first()

  Box(
    modifier = modifier.size(size * 1.6f),
    contentAlignment = Alignment.Center
  ) {
    // Dynamic glowing ripple rings with lip-sync / speaking pulse
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.toPx() * 0.8f, size.toPx() * 0.8f)
      val baseRadius = (size.toPx() / 2f)
      val dynamicMultiplier = if (status == VoiceCallStatus.TUTOR_SPEAKING || status == VoiceCallStatus.USER_SPEAKING) {
        1f + (rmsLevel * 0.6f) + (pulseScale - 1f) * 0.7f
      } else {
        1f + (pulseScale - 1f) * 0.2f
      }

      // Outer ripple ring
      drawCircle(
        color = ringColor.copy(alpha = if (status == VoiceCallStatus.TUTOR_SPEAKING) 0.35f else 0.15f),
        radius = baseRadius * dynamicMultiplier * 1.4f,
        center = center
      )

      // Middle ripple ring
      drawCircle(
        color = ringColor.copy(alpha = 0.5f),
        radius = baseRadius * dynamicMultiplier * 1.2f,
        center = center,
        style = Stroke(width = 3.5.dp.toPx())
      )

      // Inner glowing border
      drawCircle(
        color = ringColor.copy(alpha = 0.85f),
        radius = baseRadius * 1.06f,
        center = center,
        style = Stroke(width = 4.dp.toPx())
      )
    }

    // Core avatar circle with animated talking vector tutor avatar
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(Color(0xFF0F172A))
        .border(
          width = 3.5.dp,
          brush = Brush.sweepGradient(
            listOf(
              ringColor,
              ringColor.copy(alpha = 0.4f),
              ringColor
            )
          ),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      com.example.avatar.ui.TutorPersonaAvatarBadge(
        tutor = sophiaTutor,
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape),
        isSpeaking = status == VoiceCallStatus.TUTOR_SPEAKING,
        speechAperture = (rmsLevel * 1.4f).coerceIn(0f, 1f)
      )

      // Speaking / Talking indicator badge at bottom
      if (status == VoiceCallStatus.TUTOR_SPEAKING) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .background(Color(0xE67B2CBF))
            .padding(horizontal = 10.dp, vertical = 3.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "🗣️ Speaking...",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun LiveAudioWaveformBar(
  status: VoiceCallStatus,
  rmsLevel: Float,
  modifier: Modifier = Modifier,
  barCount: Int = 16,
  maxHeight: Dp = 48.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (Math.PI * 2).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  val barColor = when (status) {
    VoiceCallStatus.TUTOR_SPEAKING -> LingoVioletLight
    VoiceCallStatus.USER_SPEAKING -> LingoEmerald
    VoiceCallStatus.LISTENING -> LingoCyan
    VoiceCallStatus.THINKING -> LingoElectricBlue
    VoiceCallStatus.MUTED -> LingoAmber
    else -> Color.Gray
  }

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until barCount) {
      val normalizedIndex = i.toFloat() / barCount.toFloat()
      val sineComponent = ((sin(phase + normalizedIndex * 4f) + 1f) / 2f)

      val heightRatio = when (status) {
        VoiceCallStatus.TUTOR_SPEAKING -> {
          (0.3f + sineComponent * 0.7f).coerceIn(0.2f, 1f)
        }
        VoiceCallStatus.USER_SPEAKING -> {
          val audioIntensity = (rmsLevel * 1.5f).coerceIn(0.2f, 1f)
          (0.25f + sineComponent * audioIntensity).coerceIn(0.2f, 1f)
        }
        VoiceCallStatus.LISTENING -> {
          (0.15f + sineComponent * 0.25f)
        }
        VoiceCallStatus.THINKING -> {
          (0.2f + (sin(phase * 2f + i) + 1f) * 0.2f)
        }
        else -> 0.1f
      }

      Box(
        modifier = Modifier
          .width(4.dp)
          .height(maxHeight * heightRatio)
          .clip(CircleShape)
          .background(barColor)
      )
    }
  }
}
