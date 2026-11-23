package com.example.avatar.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.avatar.TutorPersona
import com.example.avatar.TutorViseme
import com.example.avatar.VideoCallStatus
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Distinct visual character styling per AI Video Tutor persona.
 * Every persona has a bespoke studio palette, hair silhouette, outfit, and facial accessories
 * rendered natively in Jetpack Compose Canvas—never static human stock photos.
 */
data class TutorAvatarVisualStyle(
  val hairPrimary: Color,
  val hairHighlight: Color,
  val eyeIrisColor: Color,
  val outfitPrimary: Color,
  val outfitAccent: Color,
  val shirtInner: Color,
  val studioTopBg: Color,
  val studioMidBg: Color,
  val studioAccentGlow: Color,
  val hairStyle: TutorHairStyle,
  val hasGlasses: Boolean = false,
  val hasEarrings: Boolean = false,
  val hasBeard: Boolean = false,
  val cheekBlushAlpha: Float = 0.24f
)

enum class TutorHairStyle {
  LONG_WAVY_SIDE_PART,
  CHIC_PARISIAN_BOB,
  VOLUMINOUS_CURLS,
  SLEEK_STRAIGHT_BANGS,
  HALF_UP_CLASSIC,
  SHORT_FADE_TEXTURED,
  EXECUTIVE_SWEPT,
  NEAT_MODERN_PART
}

fun visualStyleForTutor(tutor: TutorPersona): TutorAvatarVisualStyle {
  return when (tutor.id) {
    "tutor_elena" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF2B1814),
      hairHighlight = Color(0xFF543027),
      eyeIrisColor = Color(0xFF5D3A24),
      outfitPrimary = Color(0xFF1D4ED8),
      outfitAccent = Color(0xFF38BDF8),
      shirtInner = Color(0xFFE0F2FE),
      studioTopBg = Color(0xFF0A1633),
      studioMidBg = Color(0xFF132752),
      studioAccentGlow = Color(0xFF06B6D4),
      hairStyle = TutorHairStyle.LONG_WAVY_SIDE_PART,
      hasEarrings = true,
      cheekBlushAlpha = 0.26f
    )
    "tutor_chloe" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF4A2B1D),
      hairHighlight = Color(0xFF7B4B33),
      eyeIrisColor = Color(0xFF3A7D7C),
      outfitPrimary = Color(0xFF0F766E),
      outfitAccent = Color(0xFFF472B6),
      shirtInner = Color(0xFFFDF2F8),
      studioTopBg = Color(0xFF0B1930),
      studioMidBg = Color(0xFF162C4E),
      studioAccentGlow = Color(0xFF2DD4BF),
      hairStyle = TutorHairStyle.CHIC_PARISIAN_BOB,
      hasEarrings = true,
      cheekBlushAlpha = 0.28f
    )
    "tutor_sophia" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF261410),
      hairHighlight = Color(0xFF5B3124),
      eyeIrisColor = Color(0xFF6B4226),
      outfitPrimary = Color(0xFF9F1239),
      outfitAccent = Color(0xFFFBBF24),
      shirtInner = Color(0xFFFFFBEB),
      studioTopBg = Color(0xFF121530),
      studioMidBg = Color(0xFF22224B),
      studioAccentGlow = Color(0xFFF59E0B),
      hairStyle = TutorHairStyle.VOLUMINOUS_CURLS,
      hasEarrings = true,
      cheekBlushAlpha = 0.25f
    )
    "tutor_mei" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF141622),
      hairHighlight = Color(0xFF323954),
      eyeIrisColor = Color(0xFF2D221E),
      outfitPrimary = Color(0xFF3730A3),
      outfitAccent = Color(0xFF38BDF8),
      shirtInner = Color(0xFFEEF2FF),
      studioTopBg = Color(0xFF09142B),
      studioMidBg = Color(0xFF14244C),
      studioAccentGlow = Color(0xFF6366F1),
      hairStyle = TutorHairStyle.SLEEK_STRAIGHT_BANGS,
      hasEarrings = true,
      cheekBlushAlpha = 0.22f
    )
    "tutor_emma" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF6A4024),
      hairHighlight = Color(0xFF9E6B42),
      eyeIrisColor = Color(0xFF2E6F9E),
      outfitPrimary = Color(0xFF6D28D9),
      outfitAccent = Color(0xFF22D3EE),
      shirtInner = Color(0xFFF5F3FF),
      studioTopBg = Color(0xFF0D1533),
      studioMidBg = Color(0xFF1A2552),
      studioAccentGlow = Color(0xFF8B5CF6),
      hairStyle = TutorHairStyle.HALF_UP_CLASSIC,
      hasGlasses = false,
      hasEarrings = true,
      cheekBlushAlpha = 0.26f
    )
    "tutor_carlos" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF1E1715),
      hairHighlight = Color(0xFF3D302B),
      eyeIrisColor = Color(0xFF4B2E1E),
      outfitPrimary = Color(0xFF0369A1),
      outfitAccent = Color(0xFF10B981),
      shirtInner = Color(0xFFF0F9FF),
      studioTopBg = Color(0xFF08182E),
      studioMidBg = Color(0xFF0F2B4D),
      studioAccentGlow = Color(0xFF10B981),
      hairStyle = TutorHairStyle.SHORT_FADE_TEXTURED,
      hasBeard = true,
      cheekBlushAlpha = 0.14f
    )
    "tutor_alexander" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF473322),
      hairHighlight = Color(0xFF7A5A3E),
      eyeIrisColor = Color(0xFF336B87),
      outfitPrimary = Color(0xFF1E293B),
      outfitAccent = Color(0xFF3B82F6),
      shirtInner = Color(0xFFF8FAFC),
      studioTopBg = Color(0xFF08142B),
      studioMidBg = Color(0xFF12264A),
      studioAccentGlow = Color(0xFF3B82F6),
      hairStyle = TutorHairStyle.EXECUTIVE_SWEPT,
      hasBeard = false,
      cheekBlushAlpha = 0.12f
    )
    "tutor_kenji" -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF161925),
      hairHighlight = Color(0xFF363D57),
      eyeIrisColor = Color(0xFF2B211B),
      outfitPrimary = Color(0xFF0F172A),
      outfitAccent = Color(0xFF06B6D4),
      shirtInner = Color(0xFFE2E8F0),
      studioTopBg = Color(0xFF08142A),
      studioMidBg = Color(0xFF10254A),
      studioAccentGlow = Color(0xFF06B6D4),
      hairStyle = TutorHairStyle.NEAT_MODERN_PART,
      hasGlasses = true,
      cheekBlushAlpha = 0.12f
    )
    else -> TutorAvatarVisualStyle(
      hairPrimary = Color(0xFF2C1B15),
      hairHighlight = Color(0xFF57382B),
      eyeIrisColor = Color(0xFF4A3123),
      outfitPrimary = Color(0xFF1D4ED8),
      outfitAccent = Color(0xFF06B6D4),
      shirtInner = Color(0xFFE0F2FE),
      studioTopBg = Color(0xFF0A1633),
      studioMidBg = Color(0xFF132752),
      studioAccentGlow = Color(0xFF06B6D4),
      hairStyle = if (tutor.gender.equals("Male", ignoreCase = true)) {
        TutorHairStyle.NEAT_MODERN_PART
      } else {
        TutorHairStyle.LONG_WAVY_SIDE_PART
      },
      hasEarrings = !tutor.gender.equals("Male", ignoreCase = true)
    )
  }
}

/**
 * Full-Screen Animated Talking Avatar Engine for LingoSphere Video Calls.
 *
 * Uses pure expressive vector character animation—never static human photos—with:
 * - 3D Perspective Head Kinematics: Organic breathing, speech stress bobbing, listening nods, thinking tilt
 * - Spontaneous Human Blinking Engine: Realistic non-periodic blinks and double blinks
 * - Real Audio-Synchronized Lip-Sync: Anatomical viseme morphing (AA, EE, OO, OH, BMP, FV, REST)
 * - Status-Reactive Studio Aura & Eyebrows: Reacts to TUTOR_SPEAKING, LISTENING, THINKING, and MUTED
 */
@Composable
fun RealisticTalkingAvatar(
  tutor: TutorPersona,
  callStatus: VideoCallStatus,
  currentViseme: TutorViseme,
  speechAperture: Float,
  isAudioPlaying: Boolean,
  userRmsLevel: Float = 0f,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val style = remember(tutor.id) { visualStyleForTutor(tutor) }

  // 1. HEAD KINEMATICS & ORGANIC BREATHING CLOCK
  val infiniteTransition = rememberInfiniteTransition(label = "avatar_life_cycle")

  val breathingPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.2831853f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "breathing_phase"
  )

  val listeningPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.2831853f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "listening_phase"
  )

  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "studio_glow_pulse"
  )

  // 2. STATEFUL 3D TRANSFORMS TIED TO CALL STATUS & SPEECH
  val targetTiltZ = when (callStatus) {
    VideoCallStatus.THINKING -> 2.8f
    VideoCallStatus.LISTENING -> -1.0f
    VideoCallStatus.TUTOR_SPEAKING -> if (speechAperture > 0.4f) 0.9f else -0.6f
    else -> 0f
  }
  val animatedTiltZ by animateFloatAsState(
    targetValue = targetTiltZ,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
    label = "head_tilt_z"
  )

  val targetPitchX = when (callStatus) {
    VideoCallStatus.THINKING -> -1.8f
    VideoCallStatus.LISTENING -> (sin(listeningPhase) * 2.0f)
    VideoCallStatus.TUTOR_SPEAKING -> (speechAperture * 1.6f)
    else -> 0f
  }
  val animatedPitchX by animateFloatAsState(
    targetValue = targetPitchX,
    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
    label = "head_pitch_x"
  )

  val breathingOffsetY = sin(breathingPhase) * with(density) { 3.5.dp.toPx() }
  val breathingSwayZ = cos(breathingPhase * 0.5f) * 0.45f

  // 3. SPONTANEOUS BLINKING ENGINE
  var blinkProgress by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    while (isActive) {
      val nextInterval = Random.nextLong(2400, 4600)
      delay(nextInterval)

      val blinkSteps = 6
      for (i in 1..blinkSteps) {
        blinkProgress = i.toFloat() / blinkSteps
        delay(14)
      }
      for (i in blinkSteps downTo 0) {
        blinkProgress = i.toFloat() / blinkSteps
        delay(14)
      }
      blinkProgress = 0f

      if (Random.nextFloat() < 0.25f) {
        delay(110)
        for (i in 1..blinkSteps) {
          blinkProgress = i.toFloat() / blinkSteps
          delay(12)
        }
        for (i in blinkSteps downTo 0) {
          blinkProgress = i.toFloat() / blinkSteps
          delay(12)
        }
        blinkProgress = 0f
      }
    }
  }

  // 4. AUDIO-SYNCHRONIZED SMOOTHED APERTURE & VISEME
  val effectiveAperture = if (isAudioPlaying && callStatus == VideoCallStatus.TUTOR_SPEAKING) {
    speechAperture.coerceIn(0f, 1f)
  } else {
    0f
  }

  val smoothedAperture by animateFloatAsState(
    targetValue = effectiveAperture,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = if (effectiveAperture > 0.05f) Spring.StiffnessHigh else Spring.StiffnessMedium
    ),
    label = "smoothed_aperture"
  )

  val activeViseme = if (effectiveAperture > 0.02f) currentViseme else TutorViseme.REST

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(style.studioTopBg, style.studioMidBg, Color(0xFF040914))
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    // A. ANIMATED STUDIO BACKDROP & REACTIVE HALO
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val haloColor = when (callStatus) {
        VideoCallStatus.TUTOR_SPEAKING -> LingoCyan
        VideoCallStatus.LISTENING -> LingoEmerald
        VideoCallStatus.THINKING -> LingoElectricBlue
        else -> style.studioAccentGlow
      }
      val haloIntensity = when (callStatus) {
        VideoCallStatus.TUTOR_SPEAKING -> 0.28f + smoothedAperture * 0.22f
        VideoCallStatus.LISTENING -> 0.20f + (userRmsLevel * 0.22f).coerceIn(0f, 0.22f)
        VideoCallStatus.THINKING -> 0.24f
        else -> 0.14f
      }
      val headCenter = Offset(w * 0.50f, h * 0.42f)

      // Ambient studio backlight
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            haloColor.copy(alpha = haloIntensity),
            style.studioAccentGlow.copy(alpha = haloIntensity * 0.45f),
            Color.Transparent
          ),
          center = headCenter,
          radius = (w.coerceAtMost(h) * 0.62f * glowPulse)
        ),
        radius = w.coerceAtMost(h) * 0.65f * glowPulse,
        center = headCenter
      )

      // Subtle architectural bokeh circles
      drawCircle(
        color = Color.White.copy(alpha = 0.03f),
        radius = w * 0.18f,
        center = Offset(w * 0.16f, h * 0.24f)
      )
      drawCircle(
        color = style.studioAccentGlow.copy(alpha = 0.06f),
        radius = w * 0.22f,
        center = Offset(w * 0.86f, h * 0.30f)
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.025f),
        radius = w * 0.14f,
        center = Offset(w * 0.80f, h * 0.68f)
      )
    }

    // B. 3D RIGGED VECTOR ANIMATED AVATAR WITH HEAD DYNAMICS & LIP-SYNC
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          rotationZ = animatedTiltZ + breathingSwayZ
          rotationX = animatedPitchX
          translationY = breathingOffsetY
          cameraDistance = 14f * density.density
        }
    ) {
      drawIllustratedStudioTutor(
        tutor = tutor,
        style = style,
        callStatus = callStatus,
        viseme = activeViseme,
        aperture = smoothedAperture,
        blinkProgress = blinkProgress,
        breathingPhase = breathingPhase,
        isCompactBadge = false
      )
    }

    // C. CINEMATIC EDGE VIGNETTE FOR TOP/BOTTOM OVERLAY READABILITY
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.42f),
              Color.Transparent,
              Color.Black.copy(alpha = 0.12f),
              Color.Black.copy(alpha = 0.82f)
            )
          )
        )
    )

    // D. ACTIVE CALL STATUS BOTTOM AMBIENT RIBBON
    when (callStatus) {
      VideoCallStatus.TUTOR_SPEAKING -> {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(12.dp)
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, LingoCyan.copy(alpha = 0.48f))
              )
            )
        )
      }
      VideoCallStatus.LISTENING -> {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(10.dp)
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, LingoEmerald.copy(alpha = (0.28f + userRmsLevel * 0.4f).coerceIn(0f, 0.7f)))
              )
            )
        )
      }
      VideoCallStatus.THINKING -> {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(8.dp)
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, LingoElectricBlue.copy(alpha = 0.36f))
              )
            )
        )
      }
      else -> Unit
    }
  }
}

/**
 * Compact circular animated avatar badge for Tutor Picker sheets, Voice Call avatars,
 * Lesson Tutor headers, and Onboarding live previews—eliminating static human photos everywhere.
 */
@Composable
fun TutorPersonaAvatarBadge(
  tutor: TutorPersona,
  modifier: Modifier = Modifier,
  isSpeaking: Boolean = false,
  speechAperture: Float = 0f,
  viseme: TutorViseme = TutorViseme.REST,
  callStatus: VideoCallStatus = if (isSpeaking) VideoCallStatus.TUTOR_SPEAKING else VideoCallStatus.READY
) {
  val style = remember(tutor.id) { visualStyleForTutor(tutor) }
  var liveIdleAnimationReady by remember(tutor.id) { androidx.compose.runtime.mutableStateOf(false) }
  val breathingPhase: Float
  val talkCycle: Float

  if (isSpeaking) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_life_${tutor.id}")
    val animatedBreathing by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 6.2831853f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 3600, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "badge_breathing"
    )
    val animatedTalk by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 6.2831853f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 480, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "badge_talk_cycle"
    )
    breathingPhase = animatedBreathing
    talkCycle = animatedTalk
  } else if (liveIdleAnimationReady) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_idle_${tutor.id}")
    val animatedBreathing by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 6.2831853f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 4200, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "badge_idle_breathing"
    )
    breathingPhase = animatedBreathing
    talkCycle = 0f
  } else {
    breathingPhase = 0f
    talkCycle = 0f
  }

  var blinkProgress by remember(tutor.id) { mutableFloatStateOf(0f) }
  LaunchedEffect(tutor.id) {
    delay(1600L + (tutor.id.hashCode().and(0x7FFFFFFF) % 1200).toLong())
    liveIdleAnimationReady = true
    while (isActive) {
      delay(Random.nextLong(3200, 5400))
      for (i in 1..4) {
        blinkProgress = i / 4f
        delay(16)
      }
      for (i in 4 downTo 0) {
        blinkProgress = i / 4f
        delay(16)
      }
      blinkProgress = 0f
    }
  }

  val resolvedAperture = if (isSpeaking) {
    if (speechAperture > 0.04f) speechAperture.coerceIn(0f, 1f)
    else ((sin(talkCycle) + 1f) * 0.38f + 0.14f).coerceIn(0.12f, 0.85f)
  } else {
    0f
  }
  val resolvedViseme = when {
    !isSpeaking -> TutorViseme.REST
    viseme != TutorViseme.REST -> viseme
    sin(talkCycle) > 0.35f -> TutorViseme.AA
    cos(talkCycle) > 0.25f -> TutorViseme.EE
    else -> TutorViseme.NEUTRAL_TALK
  }

  Box(
    modifier = modifier.background(
      Brush.radialGradient(
        colors = listOf(style.studioMidBg, style.studioTopBg)
      )
    )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawIllustratedStudioTutor(
        tutor = tutor,
        style = style,
        callStatus = callStatus,
        viseme = resolvedViseme,
        aperture = resolvedAperture,
        blinkProgress = blinkProgress,
        breathingPhase = breathingPhase,
        isCompactBadge = true
      )
    }
  }
}

/**
 * Shared 2.5D illustrated studio character renderer used by both the full-bleed
 * video call stage and compact circular avatar badges.
 */
private fun DrawScope.drawIllustratedStudioTutor(
  tutor: TutorPersona,
  style: TutorAvatarVisualStyle,
  callStatus: VideoCallStatus,
  viseme: TutorViseme,
  aperture: Float,
  blinkProgress: Float,
  breathingPhase: Float,
  isCompactBadge: Boolean
) {
  val w = size.width
  val h = size.height
  val minDim = w.coerceAtMost(h)

  // Center character proportionally whether tall phone screen (9:16) or square circular badge (1:1)
  val cx = w * 0.50f
  val faceWidth = if (isCompactBadge) minDim * 0.50f else minDim * 0.44f
  val faceHeight = faceWidth * 1.24f
  val headCy = if (isCompactBadge) h * 0.45f else h * 0.42f
  val headTop = headCy - faceHeight * 0.5f
  val headBottom = headCy + faceHeight * 0.5f

  val skinColor = Color(tutor.skinToneHex)
  val skinShadow = Color(
    red = (skinColor.red * 0.84f).coerceIn(0f, 1f),
    green = (skinColor.green * 0.81f).coerceIn(0f, 1f),
    blue = (skinColor.blue * 0.80f).coerceIn(0f, 1f),
    alpha = 1f
  )
  val lipColor = Color(tutor.lipToneHex)
  val isMale = tutor.gender.equals("Male", ignoreCase = true)

  // 1. BACK HAIR LAYER (for long/medium female silhouettes)
  drawBackHairLayer(
    cx = cx,
    headCy = headCy,
    faceWidth = faceWidth,
    faceHeight = faceHeight,
    style = style
  )

  // 2. SHOULDERS, BLAZER / OUTFIT & NECK
  val shoulderTop = headBottom + faceHeight * 0.08f
  val shoulderWidth = faceWidth * (if (isMale) 2.25f else 2.05f)
  val shoulderBreathY = sin(breathingPhase) * (faceHeight * 0.015f)
  val torsoBottom = h * 1.08f

  val blazerPath = Path().apply {
    moveTo(cx - shoulderWidth * 0.55f, torsoBottom)
    lineTo(cx - shoulderWidth * 0.50f, shoulderTop + shoulderBreathY + faceHeight * 0.34f)
    quadraticTo(
      cx - shoulderWidth * 0.44f, shoulderTop + shoulderBreathY + faceHeight * 0.06f,
      cx - faceWidth * 0.25f, shoulderTop + shoulderBreathY
    )
    lineTo(cx + faceWidth * 0.25f, shoulderTop + shoulderBreathY)
    quadraticTo(
      cx + shoulderWidth * 0.44f, shoulderTop + shoulderBreathY + faceHeight * 0.06f,
      cx + shoulderWidth * 0.50f, shoulderTop + shoulderBreathY + faceHeight * 0.34f
    )
    lineTo(cx + shoulderWidth * 0.55f, torsoBottom)
    close()
  }
  drawPath(
    path = blazerPath,
    brush = Brush.verticalGradient(
      colors = listOf(style.outfitPrimary, style.outfitPrimary.copy(alpha = 0.82f), Color(0xFF0A1020)),
      startY = shoulderTop,
      endY = torsoBottom
    )
  )

  // Inner shirt / V-neck & tailored lapels
  val neckWidth = faceWidth * (if (isMale) 0.42f else 0.34f)
  val neckPath = Path().apply {
    moveTo(cx - neckWidth * 0.5f, headBottom - faceHeight * 0.12f)
    lineTo(cx - neckWidth * 0.55f, shoulderTop + shoulderBreathY + faceHeight * 0.16f)
    quadraticTo(cx, shoulderTop + shoulderBreathY + faceHeight * 0.32f, cx + neckWidth * 0.55f, shoulderTop + shoulderBreathY + faceHeight * 0.16f)
    lineTo(cx + neckWidth * 0.5f, headBottom - faceHeight * 0.12f)
    close()
  }
  drawPath(path = neckPath, color = skinColor)

  // Under-chin shadow on neck
  drawOval(
    color = skinShadow.copy(alpha = 0.58f),
    topLeft = Offset(cx - neckWidth * 0.48f, headBottom - faceHeight * 0.07f),
    size = Size(neckWidth * 0.96f, faceHeight * 0.17f)
  )

  // Shirt neckline & blazer lapels
  val vNeckPath = Path().apply {
    moveTo(cx - neckWidth * 0.58f, shoulderTop + shoulderBreathY + faceHeight * 0.04f)
    lineTo(cx, shoulderTop + shoulderBreathY + faceHeight * 0.36f)
    lineTo(cx + neckWidth * 0.58f, shoulderTop + shoulderBreathY + faceHeight * 0.04f)
    close()
  }
  drawPath(path = vNeckPath, color = style.shirtInner)

  val lapelLeft = Path().apply {
    moveTo(cx - neckWidth * 0.65f, shoulderTop + shoulderBreathY + faceHeight * 0.03f)
    lineTo(cx - neckWidth * 0.95f, shoulderTop + shoulderBreathY + faceHeight * 0.20f)
    lineTo(cx, shoulderTop + shoulderBreathY + faceHeight * 0.40f)
    close()
  }
  val lapelRight = Path().apply {
    moveTo(cx + neckWidth * 0.65f, shoulderTop + shoulderBreathY + faceHeight * 0.03f)
    lineTo(cx + neckWidth * 0.95f, shoulderTop + shoulderBreathY + faceHeight * 0.20f)
    lineTo(cx, shoulderTop + shoulderBreathY + faceHeight * 0.40f)
    close()
  }
  drawPath(path = lapelLeft, color = style.outfitAccent.copy(alpha = 0.36f))
  drawPath(path = lapelRight, color = style.outfitAccent.copy(alpha = 0.36f))

  // 3. EARS & OPTIONAL EARRINGS
  val earY = headCy + faceHeight * 0.03f
  val earRx = faceWidth * 0.095f
  val earRy = faceHeight * 0.145f
  listOf(-1f, 1f).forEach { dir ->
    val ex = cx + dir * (faceWidth * 0.495f)
    drawOval(
      color = skinShadow,
      topLeft = Offset(ex - earRx, earY - earRy),
      size = Size(earRx * 2f, earRy * 2f)
    )
    drawOval(
      color = skinColor,
      topLeft = Offset(ex - earRx * 0.85f, earY - earRy * 0.85f),
      size = Size(earRx * 1.7f, earRy * 1.7f)
    )
    if (style.hasEarrings) {
      drawCircle(
        color = style.outfitAccent.copy(alpha = 0.92f),
        radius = earRx * 0.42f,
        center = Offset(ex + dir * earRx * 0.15f, earY + earRy * 0.82f)
      )
    }
  }

  // 4. HEAD / FACE SCULPTED CONTOUR
  val jawHalf = if (isMale) faceWidth * 0.44f else faceWidth * 0.40f
  val headPath = Path().apply {
    moveTo(cx, headTop)
    // Right temple to cheek
    cubicTo(
      cx + faceWidth * 0.45f, headTop,
      cx + faceWidth * 0.51f, headCy - faceHeight * 0.14f,
      cx + faceWidth * 0.49f, headCy + faceHeight * 0.08f
    )
    // Right jaw to chin
    cubicTo(
      cx + jawHalf, headBottom - faceHeight * 0.10f,
      cx + faceWidth * 0.19f, headBottom,
      cx, headBottom
    )
    // Chin to left jaw
    cubicTo(
      cx - faceWidth * 0.19f, headBottom,
      cx - jawHalf, headBottom - faceHeight * 0.10f,
      cx - faceWidth * 0.49f, headCy + faceHeight * 0.08f
    )
    // Left cheek to crown
    cubicTo(
      cx - faceWidth * 0.51f, headCy - faceHeight * 0.14f,
      cx - faceWidth * 0.45f, headTop,
      cx, headTop
    )
    close()
  }
  drawPath(
    path = headPath,
    brush = Brush.verticalGradient(
      colors = listOf(skinColor, skinColor, skinShadow.copy(alpha = 0.95f)),
      startY = headTop,
      endY = headBottom
    )
  )

  // Subtle cheekbone warmth & blush
  val eyeY = headCy - faceHeight * 0.045f
  val eyeSpacing = faceWidth * 0.215f
  val blushY = eyeY + faceHeight * 0.165f
  listOf(-1f, 1f).forEach { dir ->
    val bx = cx + dir * (eyeSpacing * 1.15f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFE86C7C).copy(alpha = style.cheekBlushAlpha), Color.Transparent),
        center = Offset(bx, blushY),
        radius = faceWidth * 0.15f
      ),
      radius = faceWidth * 0.15f,
      center = Offset(bx, blushY)
    )
  }

  // Optional stubble/beard shading for male personas like Carlos
  if (style.hasBeard) {
    val beardPath = Path().apply {
      moveTo(cx - faceWidth * 0.43f, headCy + faceHeight * 0.13f)
      cubicTo(
        cx - jawHalf * 0.92f, headBottom - faceHeight * 0.06f,
        cx - faceWidth * 0.18f, headBottom + faceHeight * 0.02f,
        cx, headBottom + faceHeight * 0.02f
      )
      cubicTo(
        cx + faceWidth * 0.18f, headBottom + faceHeight * 0.02f,
        cx + jawHalf * 0.92f, headBottom - faceHeight * 0.06f,
        cx + faceWidth * 0.43f, headCy + faceHeight * 0.13f
      )
      cubicTo(
        cx + faceWidth * 0.26f, headBottom - faceHeight * 0.18f,
        cx - faceWidth * 0.26f, headBottom - faceHeight * 0.18f,
        cx - faceWidth * 0.43f, headCy + faceHeight * 0.13f
      )
      close()
    }
    drawPath(path = beardPath, color = style.hairPrimary.copy(alpha = 0.42f))
  }

  // 5. DETAILED EYES + BLINKING ENGINE
  val eyeRx = faceWidth * 0.098f
  val eyeRy = faceHeight * 0.062f * (1f - blinkProgress.coerceIn(0f, 0.95f))
  listOf(-1f, 1f).forEach { dir ->
    val ex = cx + dir * eyeSpacing
    drawIllustratedEye(
      ex = ex,
      ey = eyeY,
      eyeRx = eyeRx,
      eyeRy = eyeRy,
      irisColor = style.eyeIrisColor,
      skinColor = skinColor,
      lashColor = Color(0xFF171110),
      blinkProgress = blinkProgress,
      isMale = isMale
    )
  }

  // Optional stylish glasses (e.g., Kenji)
  if (style.hasGlasses) {
    val frameColor = Color(0xFF94A3B8)
    val lensRx = eyeRx * 1.48f
    val lensRy = faceHeight * 0.095f
    listOf(-1f, 1f).forEach { dir ->
      val ex = cx + dir * eyeSpacing
      drawRoundRect(
        color = Color.White.copy(alpha = 0.08f),
        topLeft = Offset(ex - lensRx, eyeY - lensRy),
        size = Size(lensRx * 2f, lensRy * 2f),
        cornerRadius = CornerRadius(lensRy * 0.45f, lensRy * 0.45f)
      )
      drawRoundRect(
        color = frameColor,
        topLeft = Offset(ex - lensRx, eyeY - lensRy),
        size = Size(lensRx * 2f, lensRy * 2f),
        cornerRadius = CornerRadius(lensRy * 0.45f, lensRy * 0.45f),
        style = Stroke(width = (faceWidth * 0.018f).coerceAtLeast(1.8f))
      )
    }
    // Bridge
    drawLine(
      color = frameColor,
      start = Offset(cx - eyeSpacing + lensRx, eyeY - lensRy * 0.15f),
      end = Offset(cx + eyeSpacing - lensRx, eyeY - lensRy * 0.15f),
      strokeWidth = (faceWidth * 0.018f).coerceAtLeast(1.8f)
    )
  }

  // 6. EXPRESSIVE EYEBROWS (React to speaking / thinking / listening)
  val browBaseY = eyeY - faceHeight * 0.115f
  val browLift = when (callStatus) {
    VideoCallStatus.TUTOR_SPEAKING -> -aperture * faceHeight * 0.025f
    VideoCallStatus.THINKING -> -faceHeight * 0.022f
    VideoCallStatus.LISTENING -> -faceHeight * 0.012f
    else -> 0f
  }
  val browStroke = (faceWidth * (if (isMale) 0.036f else 0.025f)).coerceAtLeast(2.2f)
  listOf(-1f, 1f).forEach { dir ->
    val innerX = cx + dir * (eyeSpacing * 0.42f)
    val outerX = cx + dir * (eyeSpacing * 1.48f)
    val thinkAsym = if (callStatus == VideoCallStatus.THINKING && dir > 0) -faceHeight * 0.016f else 0f
    val browPath = Path().apply {
      moveTo(innerX, browBaseY + browLift + thinkAsym)
      quadraticTo(
        cx + dir * (eyeSpacing * 0.98f),
        browBaseY + browLift + thinkAsym - faceHeight * 0.028f,
        outerX,
        browBaseY + browLift * 0.6f + faceHeight * 0.008f
      )
    }
    drawPath(
      path = browPath,
      color = style.hairPrimary,
      style = Stroke(width = browStroke, cap = StrokeCap.Round)
    )
  }

  // 7. SCULPTED NOSE
  val noseTopY = eyeY + faceHeight * 0.04f
  val noseTipY = headCy + faceHeight * 0.135f
  val nosePath = Path().apply {
    moveTo(cx - faceWidth * 0.015f, noseTopY)
    lineTo(cx - faceWidth * 0.042f, noseTipY)
    quadraticTo(cx, noseTipY + faceHeight * 0.028f, cx + faceWidth * 0.046f, noseTipY)
  }
  drawPath(
    path = nosePath,
    color = skinShadow.copy(alpha = 0.72f),
    style = Stroke(width = (faceWidth * 0.018f).coerceAtLeast(1.6f), cap = StrokeCap.Round)
  )

  // 8. ANATOMICAL VISEME MOUTH & RESTING SMILE
  val mouthCenterY = headCy + faceHeight * 0.275f
  val mouthBaseWidth = faceWidth * 0.36f
  drawAvatarMouth(
    cx = cx,
    mouthCenterY = mouthCenterY,
    baseMouthWidth = mouthBaseWidth,
    lipColor = lipColor,
    viseme = viseme,
    aperture = aperture,
    callStatus = callStatus
  )

  // 9. FRONT HAIR / CROWN LAYER
  drawFrontHairLayer(
    cx = cx,
    headTop = headTop,
    headCy = headCy,
    faceWidth = faceWidth,
    faceHeight = faceHeight,
    style = style
  )
}

private fun DrawScope.drawIllustratedEye(
  ex: Float,
  ey: Float,
  eyeRx: Float,
  eyeRy: Float,
  irisColor: Color,
  skinColor: Color,
  lashColor: Color,
  blinkProgress: Float,
  isMale: Boolean
) {
  if (eyeRy > 1.2f && blinkProgress < 0.85f) {
    // Sclera (white of eye)
    drawOval(
      color = Color(0xFFFDFCFB),
      topLeft = Offset(ex - eyeRx, ey - eyeRy),
      size = Size(eyeRx * 2f, eyeRy * 2f)
    )
    // Colored iris
    val irisR = (eyeRy * 0.92f).coerceAtMost(eyeRx * 0.60f)
    drawCircle(
      color = irisColor,
      radius = irisR,
      center = Offset(ex, ey)
    )
    // Pupil
    drawCircle(
      color = Color(0xFF0E1117),
      radius = irisR * 0.50f,
      center = Offset(ex, ey)
    )
    // Studio catchlights (makes avatar feel alive)
    drawCircle(
      color = Color.White.copy(alpha = 0.92f),
      radius = (irisR * 0.26f).coerceAtLeast(1.3f),
      center = Offset(ex - irisR * 0.30f, ey - irisR * 0.30f)
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.55f),
      radius = (irisR * 0.14f).coerceAtLeast(0.8f),
      center = Offset(ex + irisR * 0.28f, ey + irisR * 0.24f)
    )
  } else {
    // Closed eyelid skin patch during peak blink
    drawOval(
      color = skinColor,
      topLeft = Offset(ex - eyeRx, ey - eyeRx * 0.25f),
      size = Size(eyeRx * 2f, eyeRx * 0.5f)
    )
  }

  // Upper lash / lid contour
  val lidPath = Path().apply {
    moveTo(ex - eyeRx * 1.05f, ey + blinkProgress * eyeRx * 0.12f)
    cubicTo(
      ex - eyeRx * 0.45f, ey - eyeRy * 1.15f,
      ex + eyeRx * 0.45f, ey - eyeRy * 1.15f,
      ex + eyeRx * 1.05f, ey + blinkProgress * eyeRx * 0.12f
    )
  }
  drawPath(
    path = lidPath,
    color = lashColor,
    style = Stroke(width = (eyeRx * (if (isMale) 0.16f else 0.22f)).coerceAtLeast(1.8f), cap = StrokeCap.Round)
  )
}

private fun DrawScope.drawAvatarMouth(
  cx: Float,
  mouthCenterY: Float,
  baseMouthWidth: Float,
  lipColor: Color,
  viseme: TutorViseme,
  aperture: Float,
  callStatus: VideoCallStatus
) {
  if (aperture <= 0.02f) {
    // Illustrated smiling closed lips at rest
    val halfW = baseMouthWidth * 0.50f
    val cornerLift = if (callStatus == VideoCallStatus.LISTENING) halfW * 0.14f else halfW * 0.10f
    val smileCurve = halfW * 0.14f

    // Upper lip curve
    val upperPath = Path().apply {
      moveTo(cx - halfW, mouthCenterY - cornerLift * 0.4f)
      cubicTo(
        cx - halfW * 0.35f, mouthCenterY - halfW * 0.18f,
        cx - halfW * 0.12f, mouthCenterY - halfW * 0.20f,
        cx, mouthCenterY - halfW * 0.12f
      )
      cubicTo(
        cx + halfW * 0.12f, mouthCenterY - halfW * 0.20f,
        cx + halfW * 0.35f, mouthCenterY - halfW * 0.18f,
        cx + halfW, mouthCenterY - cornerLift * 0.4f
      )
      cubicTo(
        cx + halfW * 0.4f, mouthCenterY + smileCurve,
        cx - halfW * 0.4f, mouthCenterY + smileCurve,
        cx - halfW, mouthCenterY - cornerLift * 0.4f
      )
      close()
    }
    drawPath(path = upperPath, color = lipColor.copy(alpha = 0.92f))

    // Lower lip fullness
    val lowerPath = Path().apply {
      moveTo(cx - halfW * 0.88f, mouthCenterY - cornerLift * 0.2f)
      cubicTo(
        cx - halfW * 0.4f, mouthCenterY + smileCurve,
        cx + halfW * 0.4f, mouthCenterY + smileCurve,
        cx + halfW * 0.88f, mouthCenterY - cornerLift * 0.2f
      )
      cubicTo(
        cx + halfW * 0.45f, mouthCenterY + halfW * 0.28f,
        cx - halfW * 0.45f, mouthCenterY + halfW * 0.28f,
        cx - halfW * 0.88f, mouthCenterY - cornerLift * 0.2f
      )
      close()
    }
    drawPath(path = lowerPath, color = lipColor.copy(alpha = 0.84f))

    // Smile line
    val linePath = Path().apply {
      moveTo(cx - halfW, mouthCenterY - cornerLift * 0.4f)
      cubicTo(
        cx - halfW * 0.35f, mouthCenterY + smileCurve,
        cx + halfW * 0.35f, mouthCenterY + smileCurve,
        cx + halfW, mouthCenterY - cornerLift * 0.4f
      )
    }
    drawPath(
      path = linePath,
      color = Color(0xFF4B151D).copy(alpha = 0.65f),
      style = Stroke(width = (baseMouthWidth * 0.036f).coerceAtLeast(1.4f), cap = StrokeCap.Round)
    )
    return
  }

  // Active talking viseme rendering
  val scaledWidth = baseMouthWidth * viseme.widthRatio
  val halfWidth = scaledWidth / 2f
  val openHeight = (scaledWidth * 0.58f * aperture * viseme.openness).coerceIn(3f, scaledWidth * 0.68f)
  val halfOpen = openHeight / 2f
  val upperLipThickness = (scaledWidth * 0.14f * (1f - viseme.upperLipLift * 0.25f)).coerceAtLeast(2.5f)
  val lowerLipThickness = (scaledWidth * 0.18f * (1f + viseme.lowerLipDepress * 0.2f)).coerceAtLeast(3.2f)

  // Oral cavity void
  val oralCavityPath = Path().apply {
    moveTo(cx - halfWidth * 0.85f, mouthCenterY)
    cubicTo(
      cx - halfWidth * 0.4f, mouthCenterY - halfOpen,
      cx + halfWidth * 0.4f, mouthCenterY - halfOpen,
      cx + halfWidth * 0.85f, mouthCenterY
    )
    cubicTo(
      cx + halfWidth * 0.4f, mouthCenterY + halfOpen,
      cx - halfWidth * 0.4f, mouthCenterY + halfOpen,
      cx - halfWidth * 0.85f, mouthCenterY
    )
    close()
  }
  drawPath(
    path = oralCavityPath,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF1E080C), Color(0xFF381017), Color(0xFF150407)),
      startY = mouthCenterY - halfOpen,
      endY = mouthCenterY + halfOpen
    ),
    style = Fill
  )

  // Upper teeth arc
  if (aperture > 0.18f && viseme != TutorViseme.BMP) {
    val teethHeight = (openHeight * 0.42f).coerceIn(2f, 14f)
    val teethWidth = halfWidth * 0.64f
    val teethPath = Path().apply {
      moveTo(cx - teethWidth, mouthCenterY - halfOpen + 1f)
      lineTo(cx + teethWidth, mouthCenterY - halfOpen + 1f)
      cubicTo(
        cx + teethWidth * 0.5f, mouthCenterY - halfOpen + teethHeight,
        cx - teethWidth * 0.5f, mouthCenterY - halfOpen + teethHeight,
        cx - teethWidth, mouthCenterY - halfOpen + 1f
      )
      close()
    }
    drawPath(
      path = teethPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFFFAF7F2), Color(0xFFE5DFD4)),
        startY = mouthCenterY - halfOpen,
        endY = mouthCenterY - halfOpen + teethHeight
      ),
      style = Fill
    )
  }

  // Tongue for wide open vowels
  if (aperture > 0.42f && (viseme == TutorViseme.AA || viseme == TutorViseme.OH)) {
    val tongueHeight = openHeight * 0.36f
    val tongueWidth = halfWidth * 0.54f
    val tonguePath = Path().apply {
      moveTo(cx - tongueWidth, mouthCenterY + halfOpen)
      cubicTo(
        cx - tongueWidth * 0.5f, mouthCenterY + halfOpen - tongueHeight,
        cx + tongueWidth * 0.5f, mouthCenterY + halfOpen - tongueHeight,
        cx + tongueWidth, mouthCenterY + halfOpen
      )
      close()
    }
    drawPath(path = tonguePath, color = Color(0xFFC7646F), style = Fill)
  }

  // Upper lip
  val upperLipTopY = mouthCenterY - halfOpen - upperLipThickness
  val upperLipPath = Path().apply {
    moveTo(cx - halfWidth, mouthCenterY)
    cubicTo(
      cx - halfWidth * 0.55f, upperLipTopY,
      cx - halfWidth * 0.18f, upperLipTopY - 1.2f,
      cx - halfWidth * 0.08f, upperLipTopY
    )
    lineTo(cx, upperLipTopY + upperLipThickness * 0.25f)
    lineTo(cx + halfWidth * 0.08f, upperLipTopY)
    cubicTo(
      cx + halfWidth * 0.18f, upperLipTopY - 1.2f,
      cx + halfWidth * 0.55f, upperLipTopY,
      cx + halfWidth, mouthCenterY
    )
    cubicTo(
      cx + halfWidth * 0.4f, mouthCenterY - halfOpen,
      cx - halfWidth * 0.4f, mouthCenterY - halfOpen,
      cx - halfWidth, mouthCenterY
    )
    close()
  }
  drawPath(path = upperLipPath, color = lipColor.copy(alpha = 0.96f), style = Fill)

  // Lower lip
  val lowerLipBottomY = mouthCenterY + halfOpen + lowerLipThickness
  val lowerLipPath = Path().apply {
    moveTo(cx - halfWidth, mouthCenterY)
    cubicTo(
      cx - halfWidth * 0.4f, mouthCenterY + halfOpen,
      cx + halfWidth * 0.4f, mouthCenterY + halfOpen,
      cx + halfWidth, mouthCenterY
    )
    cubicTo(
      cx + halfWidth * 0.6f, lowerLipBottomY,
      cx - halfWidth * 0.6f, lowerLipBottomY,
      cx - halfWidth, mouthCenterY
    )
    close()
  }
  drawPath(path = lowerLipPath, color = lipColor.copy(alpha = 0.94f), style = Fill)
}

private fun DrawScope.drawBackHairLayer(
  cx: Float,
  headCy: Float,
  faceWidth: Float,
  faceHeight: Float,
  style: TutorAvatarVisualStyle
) {
  when (style.hairStyle) {
    TutorHairStyle.LONG_WAVY_SIDE_PART,
    TutorHairStyle.HALF_UP_CLASSIC -> {
      val backPath = Path().apply {
        moveTo(cx - faceWidth * 0.66f, headCy - faceHeight * 0.16f)
        cubicTo(
          cx - faceWidth * 0.80f, headCy + faceHeight * 0.24f,
          cx - faceWidth * 0.72f, headCy + faceHeight * 0.74f,
          cx - faceWidth * 0.48f, headCy + faceHeight * 0.86f
        )
        lineTo(cx + faceWidth * 0.48f, headCy + faceHeight * 0.86f)
        cubicTo(
          cx + faceWidth * 0.72f, headCy + faceHeight * 0.74f,
          cx + faceWidth * 0.80f, headCy + faceHeight * 0.24f,
          cx + faceWidth * 0.66f, headCy - faceHeight * 0.16f
        )
        close()
      }
      drawPath(
        path = backPath,
        brush = Brush.verticalGradient(
          listOf(style.hairHighlight, style.hairPrimary, style.hairPrimary.copy(alpha = 0.95f))
        )
      )
    }
    TutorHairStyle.CHIC_PARISIAN_BOB,
    TutorHairStyle.SLEEK_STRAIGHT_BANGS -> {
      val bobPath = Path().apply {
        moveTo(cx - faceWidth * 0.62f, headCy - faceHeight * 0.20f)
        cubicTo(
          cx - faceWidth * 0.70f, headCy + faceHeight * 0.20f,
          cx - faceWidth * 0.60f, headCy + faceHeight * 0.56f,
          cx - faceWidth * 0.38f, headCy + faceHeight * 0.58f
        )
        lineTo(cx + faceWidth * 0.38f, headCy + faceHeight * 0.58f)
        cubicTo(
          cx + faceWidth * 0.60f, headCy + faceHeight * 0.56f,
          cx + faceWidth * 0.70f, headCy + faceHeight * 0.20f,
          cx + faceWidth * 0.62f, headCy - faceHeight * 0.20f
        )
        close()
      }
      drawPath(path = bobPath, color = style.hairPrimary)
    }
    TutorHairStyle.VOLUMINOUS_CURLS -> {
      listOf(-0.56f, 0.56f).forEach { dx ->
        listOf(0.05f, 0.26f, 0.45f).forEach { dy ->
          drawCircle(
            color = style.hairPrimary,
            radius = faceWidth * 0.23f,
            center = Offset(cx + faceWidth * dx, headCy + faceHeight * dy)
          )
        }
      }
    }
    TutorHairStyle.SHORT_FADE_TEXTURED,
    TutorHairStyle.EXECUTIVE_SWEPT,
    TutorHairStyle.NEAT_MODERN_PART -> {
      drawOval(
        color = style.hairPrimary,
        topLeft = Offset(cx - faceWidth * 0.53f, headCy - faceHeight * 0.54f),
        size = Size(faceWidth * 1.06f, faceHeight * 0.60f)
      )
    }
  }
}

private fun DrawScope.drawFrontHairLayer(
  cx: Float,
  headTop: Float,
  headCy: Float,
  faceWidth: Float,
  faceHeight: Float,
  style: TutorAvatarVisualStyle
) {
  val hairBrush = Brush.verticalGradient(
    colors = listOf(style.hairHighlight, style.hairPrimary),
    startY = headTop - faceHeight * 0.14f,
    endY = headCy
  )

  when (style.hairStyle) {
    TutorHairStyle.LONG_WAVY_SIDE_PART,
    TutorHairStyle.HALF_UP_CLASSIC,
    TutorHairStyle.CHIC_PARISIAN_BOB -> {
      val crownPath = Path().apply {
        moveTo(cx - faceWidth * 0.54f, headCy - faceHeight * 0.04f)
        cubicTo(
          cx - faceWidth * 0.62f, headTop - faceHeight * 0.10f,
          cx - faceWidth * 0.18f, headTop - faceHeight * 0.18f,
          cx + faceWidth * 0.04f, headTop - faceHeight * 0.15f
        )
        cubicTo(
          cx + faceWidth * 0.36f, headTop - faceHeight * 0.17f,
          cx + faceWidth * 0.62f, headTop - faceHeight * 0.06f,
          cx + faceWidth * 0.54f, headCy - faceHeight * 0.02f
        )
        // Side-swept hairline
        cubicTo(
          cx + faceWidth * 0.46f, headTop + faceHeight * 0.16f,
          cx + faceWidth * 0.16f, headTop + faceHeight * 0.06f,
          cx - faceWidth * 0.08f, headTop + faceHeight * 0.12f
        )
        cubicTo(
          cx - faceWidth * 0.34f, headTop + faceHeight * 0.22f,
          cx - faceWidth * 0.48f, headTop + faceHeight * 0.28f,
          cx - faceWidth * 0.54f, headCy - faceHeight * 0.04f
        )
        close()
      }
      drawPath(path = crownPath, brush = hairBrush)
    }
    TutorHairStyle.SLEEK_STRAIGHT_BANGS -> {
      val bangsPath = Path().apply {
        moveTo(cx - faceWidth * 0.54f, headCy - faceHeight * 0.06f)
        cubicTo(
          cx - faceWidth * 0.58f, headTop - faceHeight * 0.12f,
          cx + faceWidth * 0.58f, headTop - faceHeight * 0.12f,
          cx + faceWidth * 0.54f, headCy - faceHeight * 0.06f
        )
        lineTo(cx + faceWidth * 0.44f, headTop + faceHeight * 0.21f)
        quadraticTo(cx, headTop + faceHeight * 0.18f, cx - faceWidth * 0.44f, headTop + faceHeight * 0.21f)
        close()
      }
      drawPath(path = bangsPath, brush = hairBrush)
    }
    TutorHairStyle.VOLUMINOUS_CURLS -> {
      val puffCenters = listOf(
        -0.36f to -0.44f,
        -0.12f to -0.52f,
        0.16f to -0.51f,
        0.38f to -0.42f,
        -0.48f to -0.26f,
        0.48f to -0.26f
      )
      puffCenters.forEach { (dx, dy) ->
        drawCircle(
          brush = hairBrush,
          radius = faceWidth * 0.22f,
          center = Offset(cx + faceWidth * dx, headCy + faceHeight * dy)
        )
      }
    }
    TutorHairStyle.SHORT_FADE_TEXTURED,
    TutorHairStyle.EXECUTIVE_SWEPT,
    TutorHairStyle.NEAT_MODERN_PART -> {
      val maleCrown = Path().apply {
        moveTo(cx - faceWidth * 0.50f, headCy - faceHeight * 0.12f)
        cubicTo(
          cx - faceWidth * 0.54f, headTop - faceHeight * 0.12f,
          cx - faceWidth * 0.10f, headTop - faceHeight * 0.22f,
          cx + faceWidth * 0.14f, headTop - faceHeight * 0.17f
        )
        cubicTo(
          cx + faceWidth * 0.46f, headTop - faceHeight * 0.14f,
          cx + faceWidth * 0.55f, headTop - faceHeight * 0.02f,
          cx + faceWidth * 0.50f, headCy - faceHeight * 0.12f
        )
        cubicTo(
          cx + faceWidth * 0.38f, headTop + faceHeight * 0.08f,
          cx + faceWidth * 0.08f, headTop + faceHeight * 0.04f,
          cx - faceWidth * 0.18f, headTop + faceHeight * 0.11f
        )
        cubicTo(
          cx - faceWidth * 0.38f, headTop + faceHeight * 0.14f,
          cx - faceWidth * 0.46f, headTop + faceHeight * 0.10f,
          cx - faceWidth * 0.50f, headCy - faceHeight * 0.12f
        )
        close()
      }
      drawPath(path = maleCrown, brush = hairBrush)
    }
  }
}
