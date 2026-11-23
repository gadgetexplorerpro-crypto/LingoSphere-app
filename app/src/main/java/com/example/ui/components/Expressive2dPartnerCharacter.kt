package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.avatar.TutorViseme
import com.example.data.model.AiConversationPartner
import kotlin.math.max
import kotlin.math.sin

enum class PartnerHairStyle2D {
  WAVY_SIDE_BOB,
  BRITISH_SIDE_PART,
  HIGH_BOUNCY_PONYTAIL,
  EXECUTIVE_FADE,
  SLEEK_HEADBAND_WAVES,
  TOUSLED_QUIFF,
  BEACH_WAVES_BERET,
  SOFT_CURLY_CROP,
  ELEGANT_UPDO_BUN,
  PROFESSOR_SILVER_SWEEP,
  YOUTHFUL_SPACE_BUNS,
  PARISIAN_WAVY_PART,
  CURTAIN_BANGS_BOB
}

enum class PartnerAccessory2D {
  PEARL_STUDS,
  TORTOISE_GLASSES,
  GOLD_HOOPS,
  EXECUTIVE_TIE,
  ROSE_GOLD_GLASSES_HEADBAND,
  LIGHT_STUBBLE,
  TRAVEL_BERET_SCARF,
  ROUND_SPECTACLES,
  GOLD_PENDANT,
  PROFESSOR_BEARD,
  PASTEL_HAIR_CLIPS,
  FRENCH_MUSTACHE_SCARF,
  CYAN_TUTOR_HEADSET_PIN
}

data class PartnerCharacterVisualSpec(
  val partnerId: String,
  val backdropTop: Color,
  val backdropBottom: Color,
  val skinTone: Color,
  val skinShadow: Color,
  val cheekBlush: Color,
  val hairColor: Color,
  val hairHighlight: Color,
  val eyeColor: Color,
  val lipColor: Color,
  val outfitPrimary: Color,
  val outfitSecondary: Color,
  val hairStyle: PartnerHairStyle2D,
  val accessory: PartnerAccessory2D,
  val isFemale: Boolean,
  val smileCurvature: Float = 1.0f
)

fun resolvePartnerCharacterVisualSpec(partnerId: String, partnerName: String = ""): PartnerCharacterVisualSpec {
  val key = partnerId.lowercase().ifBlank { partnerName.lowercase() }
  return when {
    key.contains("emma") -> PartnerCharacterVisualSpec(
      partnerId = "partner_emma",
      backdropTop = Color(0xFF1E3A5F),
      backdropBottom = Color(0xFF0F213C),
      skinTone = Color(0xFFF6C8A8),
      skinShadow = Color(0xFFE2AD8B),
      cheekBlush = Color(0xFFF48FB1),
      hairColor = Color(0xFF9C5227),
      hairHighlight = Color(0xFFC87A44),
      eyeColor = Color(0xFF2E7D5B),
      lipColor = Color(0xFFD96B75),
      outfitPrimary = Color(0xFFE15B64),
      outfitSecondary = Color(0xFFFFF3E0),
      hairStyle = PartnerHairStyle2D.WAVY_SIDE_BOB,
      accessory = PartnerAccessory2D.PEARL_STUDS,
      isFemale = true,
      smileCurvature = 1.1f
    )
    key.contains("james") -> PartnerCharacterVisualSpec(
      partnerId = "partner_james",
      backdropTop = Color(0xFF1A2E4C),
      backdropBottom = Color(0xFF0C182B),
      skinTone = Color(0xFFF3C6B1),
      skinShadow = Color(0xFFDCA992),
      cheekBlush = Color(0xFFE5989B),
      hairColor = Color(0xFF4A2E1B),
      hairHighlight = Color(0xFF6E472D),
      eyeColor = Color(0xFF2563EB),
      lipColor = Color(0xFFC47D7A),
      outfitPrimary = Color(0xFF1E3A8A),
      outfitSecondary = Color(0xFF93C5FD),
      hairStyle = PartnerHairStyle2D.BRITISH_SIDE_PART,
      accessory = PartnerAccessory2D.TORTOISE_GLASSES,
      isFemale = false,
      smileCurvature = 0.92f
    )
    key.contains("sophia") -> PartnerCharacterVisualSpec(
      partnerId = "partner_sophia",
      backdropTop = Color(0xFF2C2354),
      backdropBottom = Color(0xFF14112E),
      skinTone = Color(0xFFE6A97E),
      skinShadow = Color(0xFFCC8B60),
      cheekBlush = Color(0xFFF06292),
      hairColor = Color(0xFF231815),
      hairHighlight = Color(0xFF4A352F),
      eyeColor = Color(0xFF5D4037),
      lipColor = Color(0xFFE05A6D),
      outfitPrimary = Color(0xFFF59E0B),
      outfitSecondary = Color(0xFFFFFBEB),
      hairStyle = PartnerHairStyle2D.HIGH_BOUNCY_PONYTAIL,
      accessory = PartnerAccessory2D.GOLD_HOOPS,
      isFemale = true,
      smileCurvature = 1.2f
    )
    key.contains("daniel") -> PartnerCharacterVisualSpec(
      partnerId = "partner_daniel",
      backdropTop = Color(0xFF172B42),
      backdropBottom = Color(0xFF0B1524),
      skinTone = Color(0xFF8D5534),
      skinShadow = Color(0xFF6F3F22),
      cheekBlush = Color(0xFFA85746),
      hairColor = Color(0xFF171412),
      hairHighlight = Color(0xFF332D29),
      eyeColor = Color(0xFF2B1B12),
      lipColor = Color(0xFF774336),
      outfitPrimary = Color(0xFF1E293B),
      outfitSecondary = Color(0xFF38BDF8),
      hairStyle = PartnerHairStyle2D.EXECUTIVE_FADE,
      accessory = PartnerAccessory2D.EXECUTIVE_TIE,
      isFemale = false,
      smileCurvature = 0.9f
    )
    key.contains("olivia") -> PartnerCharacterVisualSpec(
      partnerId = "partner_olivia",
      backdropTop = Color(0xFF133848),
      backdropBottom = Color(0xFF091D28),
      skinTone = Color(0xFFF8D5C2),
      skinShadow = Color(0xFFE1B8A2),
      cheekBlush = Color(0xFFF48FB1),
      hairColor = Color(0xFF8D3B24),
      hairHighlight = Color(0xFFB85638),
      eyeColor = Color(0xFF0D9488),
      lipColor = Color(0xFFD86472),
      outfitPrimary = Color(0xFF06B6D4),
      outfitSecondary = Color(0xFFCFFAFE),
      hairStyle = PartnerHairStyle2D.SLEEK_HEADBAND_WAVES,
      accessory = PartnerAccessory2D.ROSE_GOLD_GLASSES_HEADBAND,
      isFemale = true,
      smileCurvature = 1.05f
    )
    key.contains("michael") -> PartnerCharacterVisualSpec(
      partnerId = "partner_michael",
      backdropTop = Color(0xFF183635),
      backdropBottom = Color(0xFF0B1C1C),
      skinTone = Color(0xFFEAC09E),
      skinShadow = Color(0xFFD1A17C),
      cheekBlush = Color(0xFFE07A5F),
      hairColor = Color(0xFF784B2A),
      hairHighlight = Color(0xFFA06A42),
      eyeColor = Color(0xFF854D0E),
      lipColor = Color(0xFFBF746B),
      outfitPrimary = Color(0xFF10B981),
      outfitSecondary = Color(0xFF064E3B),
      hairStyle = PartnerHairStyle2D.TOUSLED_QUIFF,
      accessory = PartnerAccessory2D.LIGHT_STUBBLE,
      isFemale = false,
      smileCurvature = 1.08f
    )
    key.contains("amelia") -> PartnerCharacterVisualSpec(
      partnerId = "partner_amelia",
      backdropTop = Color(0xFF2E2A4F),
      backdropBottom = Color(0xFF141229),
      skinTone = Color(0xFFEBB691),
      skinShadow = Color(0xFFD09871),
      cheekBlush = Color(0xFFEC7A7A),
      hairColor = Color(0xFFC9933B),
      hairHighlight = Color(0xFFE5B869),
      eyeColor = Color(0xFF0284C7),
      lipColor = Color(0xFFD96565),
      outfitPrimary = Color(0xFFF97316),
      outfitSecondary = Color(0xFFFEF3C7),
      hairStyle = PartnerHairStyle2D.BEACH_WAVES_BERET,
      accessory = PartnerAccessory2D.TRAVEL_BERET_SCARF,
      isFemale = true,
      smileCurvature = 1.12f
    )
    key.contains("noah") -> PartnerCharacterVisualSpec(
      partnerId = "partner_noah",
      backdropTop = Color(0xFF1A353B),
      backdropBottom = Color(0xFF0D1D22),
      skinTone = Color(0xFFC68B63),
      skinShadow = Color(0xFFA96E47),
      cheekBlush = Color(0xFFC9685B),
      hairColor = Color(0xFF1F1917),
      hairHighlight = Color(0xFF3E332E),
      eyeColor = Color(0xFF3E2723),
      lipColor = Color(0xFFA35D52),
      outfitPrimary = Color(0xFF34D399),
      outfitSecondary = Color(0xFFECFDF5),
      hairStyle = PartnerHairStyle2D.SOFT_CURLY_CROP,
      accessory = PartnerAccessory2D.ROUND_SPECTACLES,
      isFemale = false,
      smileCurvature = 1.15f
    )
    key.contains("isabella") -> PartnerCharacterVisualSpec(
      partnerId = "partner_isabella",
      backdropTop = Color(0xFF3B1D38),
      backdropBottom = Color(0xFF1C0D1B),
      skinTone = Color(0xFFDFA37C),
      skinShadow = Color(0xFFC4855E),
      cheekBlush = Color(0xFFE56B7E),
      hairColor = Color(0xFF1A1418),
      hairHighlight = Color(0xFF3D2E38),
      eyeColor = Color(0xFF4E342E),
      lipColor = Color(0xFFC93B54),
      outfitPrimary = Color(0xFFE11D48),
      outfitSecondary = Color(0xFFFDE68A),
      hairStyle = PartnerHairStyle2D.ELEGANT_UPDO_BUN,
      accessory = PartnerAccessory2D.GOLD_PENDANT,
      isFemale = true,
      smileCurvature = 1.0f
    )
    key.contains("ethan") -> PartnerCharacterVisualSpec(
      partnerId = "partner_ethan",
      backdropTop = Color(0xFF1E2942),
      backdropBottom = Color(0xFF0D1322),
      skinTone = Color(0xFFEEC2A7),
      skinShadow = Color(0xFFD2A387),
      cheekBlush = Color(0xFFD98A82),
      hairColor = Color(0xFF334155),
      hairHighlight = Color(0xFF94A3B8),
      eyeColor = Color(0xFF3B82F6),
      lipColor = Color(0xFFB36F6B),
      outfitPrimary = Color(0xFF4F46E5),
      outfitSecondary = Color(0xFF1E1B4B),
      hairStyle = PartnerHairStyle2D.PROFESSOR_SILVER_SWEEP,
      accessory = PartnerAccessory2D.PROFESSOR_BEARD,
      isFemale = false,
      smileCurvature = 0.88f
    )
    key.contains("mia") -> PartnerCharacterVisualSpec(
      partnerId = "partner_mia",
      backdropTop = Color(0xFF2D214E),
      backdropBottom = Color(0xFF150E28),
      skinTone = Color(0xFFF7CBAE),
      skinShadow = Color(0xFFDFAD8D),
      cheekBlush = Color(0xFFF472B6),
      hairColor = Color(0xFF2D1E2F),
      hairHighlight = Color(0xFF583B5C),
      eyeColor = Color(0xFF4A2545),
      lipColor = Color(0xFFEC4899),
      outfitPrimary = Color(0xFFA855F7),
      outfitSecondary = Color(0xFFF3E8FF),
      hairStyle = PartnerHairStyle2D.YOUTHFUL_SPACE_BUNS,
      accessory = PartnerAccessory2D.PASTEL_HAIR_CLIPS,
      isFemale = true,
      smileCurvature = 1.22f
    )
    key.contains("lucas") -> PartnerCharacterVisualSpec(
      partnerId = "partner_lucas",
      backdropTop = Color(0xFF1B3152),
      backdropBottom = Color(0xFF0C1829),
      skinTone = Color(0xFFE5B28F),
      skinShadow = Color(0xFFCA9470),
      cheekBlush = Color(0xFFE07A6B),
      hairColor = Color(0xFF3B2418),
      hairHighlight = Color(0xFF5E3B28),
      eyeColor = Color(0xFF65743A),
      lipColor = Color(0xFFB86B63),
      outfitPrimary = Color(0xFF2563EB),
      outfitSecondary = Color(0xFFFBBF24),
      hairStyle = PartnerHairStyle2D.PARISIAN_WAVY_PART,
      accessory = PartnerAccessory2D.FRENCH_MUSTACHE_SCARF,
      isFemale = false,
      smileCurvature = 1.1f
    )
    else -> PartnerCharacterVisualSpec(
      partnerId = "partner_chloe",
      backdropTop = Color(0xFF16325B),
      backdropBottom = Color(0xFF0A1931),
      skinTone = Color(0xFFF7CDB2),
      skinShadow = Color(0xFFE0B093),
      cheekBlush = Color(0xFFF48FB1),
      hairColor = Color(0xFF5C3324),
      hairHighlight = Color(0xFF8C523C),
      eyeColor = Color(0xFF0EA5E9),
      lipColor = Color(0xFFE05A70),
      outfitPrimary = Color(0xFF2563EB),
      outfitSecondary = Color(0xFF38BDF8),
      hairStyle = PartnerHairStyle2D.CURTAIN_BANGS_BOB,
      accessory = PartnerAccessory2D.CYAN_TUTOR_HEADSET_PIN,
      isFemale = true,
      smileCurvature = 1.12f
    )
  }
}

/**
 * Compact animated speaking waveform indicator that dances smoothly while an AI partner speaks.
 */
@Composable
fun PartnerSpeakingWaveform(
  isSpeaking: Boolean,
  modifier: Modifier = Modifier,
  activeColor: Color = Color(0xFF38BDF8),
  idleColor: Color = Color(0xFF64748B),
  barCount: Int = 5,
  maxHeight: Dp = 14.dp
) {
  val transition = rememberInfiniteTransition(label = "partner_waveform")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 6.283185f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 680, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  Row(
    modifier = modifier.height(maxHeight),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until barCount) {
      val fraction = if (isSpeaking) {
        val wave = ((sin(phase + i * 1.15f) + 1f) * 0.5f)
        (0.28f + wave * 0.72f).coerceIn(0.25f, 1f)
      } else {
        0.24f
      }
      Box(
        modifier = Modifier
          .width(2.5.dp)
          .height(maxHeight * fraction)
          .clip(RoundedCornerShape(2.dp))
          .background(if (isSpeaking) activeColor else idleColor.copy(alpha = 0.45f))
      )
    }
  }
}

/**
 * Renders an attractive, expressive 2D emoji-style human character for each of the 12 AI Partners
 * (plus Chloe), complete with distinct facial features, hairstyle, hair color, skin tone, clothing,
 * accessories, and smooth real-time mouth animation synchronized with speech audio.
 */
@Composable
fun Expressive2dPartnerAvatar(
  partner: AiConversationPartner,
  modifier: Modifier = Modifier,
  size: Dp = 58.dp,
  isSpeaking: Boolean = false,
  speechAperture: Float = 0f,
  viseme: TutorViseme = TutorViseme.REST,
  showOnlineBadge: Boolean = false,
  showWaveformBadge: Boolean = false,
  borderColor: Color = Color(0xFF38BDF8).copy(alpha = 0.65f),
  borderWidth: Dp = 1.5.dp,
  badgeBorderColor: Color = Color(0xFF0E1B38)
) {
  val spec = remember(partner.id, partner.name) {
    resolvePartnerCharacterVisualSpec(partner.id, partner.name)
  }

  val infiniteTransition = rememberInfiniteTransition(label = "partner_2d_anim_${spec.partnerId}")
  val talkCycle by infiniteTransition.animateFloat(
    initialValue = 0.18f,
    targetValue = 0.92f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 190, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "mouth_talk_cycle"
  )
  val headBob by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "head_bob"
  )

  val targetMouthOpen = if (isSpeaking) {
    max(speechAperture.coerceIn(0.15f, 1f), talkCycle)
  } else {
    0f
  }
  val animatedMouthOpen by animateFloatAsState(
    targetValue = targetMouthOpen,
    animationSpec = tween(durationMillis = 90),
    label = "smooth_mouth_open"
  )

  Box(
    modifier = modifier
      .size(size)
      .semantics { contentDescription = partner.name },
    contentAlignment = Alignment.Center
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .clip(CircleShape)
        .border(borderWidth, borderColor, CircleShape)
    ) {
      drawExpressive2dCharacter(
        spec = spec,
        mouthOpenness = animatedMouthOpen,
        viseme = if (isSpeaking) viseme else TutorViseme.REST,
        isSpeaking = isSpeaking,
        headBobOffset = if (isSpeaking) headBob * (this.size.height * 0.012f) else 0f
      )
    }

    if (showOnlineBadge && !showWaveformBadge) {
      val dotSize = (size.value * 0.22f).coerceIn(10f, 16f).dp
      Box(
        modifier = Modifier
          .size(dotSize)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(if (isSpeaking) Color(0xFF38BDF8) else Color(0xFF10B981))
          .border(2.dp, badgeBorderColor, CircleShape)
      )
    }

    if (showWaveformBadge && isSpeaking) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF081226).copy(alpha = 0.92f))
          .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
          .padding(horizontal = 4.dp, vertical = 2.dp)
      ) {
        PartnerSpeakingWaveform(
          isSpeaking = true,
          barCount = 4,
          maxHeight = 10.dp
        )
      }
    }
  }
}

private fun DrawScope.drawExpressive2dCharacter(
  spec: PartnerCharacterVisualSpec,
  mouthOpenness: Float,
  viseme: TutorViseme,
  isSpeaking: Boolean,
  headBobOffset: Float
) {
  val w = size.width
  val h = size.height
  val cx = w * 0.5f
  val cy = h * 0.5f + headBobOffset

  // 1. Rich Circular Backdrop Gradient + Subtle Halo Glow
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(spec.backdropTop, spec.backdropBottom)
    )
  )
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        spec.outfitPrimary.copy(alpha = if (isSpeaking) 0.36f else 0.20f),
        Color.Transparent
      ),
      center = Offset(cx, h * 0.45f),
      radius = w * 0.48f
    ),
    center = Offset(cx, h * 0.45f),
    radius = w * 0.48f
  )

  // 2. Back Hair Layer (for long/wavy/bob/ponytail styles)
  drawBackHairLayer(spec, cx, cy, w, h)

  // 3. Shoulders, Outfit & Collar
  val shoulderPath = Path().apply {
    moveTo(w * 0.14f, h * 1.02f)
    quadraticTo(w * 0.18f, h * 0.74f, cx, h * 0.74f)
    quadraticTo(w * 0.82f, h * 0.74f, w * 0.86f, h * 1.02f)
    close()
  }
  drawPath(shoulderPath, color = spec.outfitPrimary)

  // Inner shirt / collar V-neck or scoop neck
  val collarPath = Path().apply {
    moveTo(w * 0.37f, h * 0.75f)
    lineTo(cx, h * 0.91f)
    lineTo(w * 0.63f, h * 0.75f)
    close()
  }
  drawPath(collarPath, color = spec.outfitSecondary)

  // Outfit accessories (Tie, Scarf, Pendant, Lapel Pin)
  when (spec.accessory) {
    PartnerAccessory2D.EXECUTIVE_TIE -> {
      val tiePath = Path().apply {
        moveTo(cx - w * 0.03f, h * 0.80f)
        lineTo(cx + w * 0.03f, h * 0.80f)
        lineTo(cx + w * 0.045f, h * 0.98f)
        lineTo(cx, h * 1.02f)
        lineTo(cx - w * 0.045f, h * 0.98f)
        close()
      }
      drawPath(tiePath, color = Color(0xFF0284C7))
    }
    PartnerAccessory2D.TORTOISE_GLASSES -> {
      val tiePath = Path().apply {
        moveTo(cx - w * 0.028f, h * 0.81f)
        lineTo(cx + w * 0.028f, h * 0.81f)
        lineTo(cx + w * 0.038f, h * 0.98f)
        lineTo(cx - w * 0.038f, h * 0.98f)
        close()
      }
      drawPath(tiePath, color = Color(0xFF991B1B))
    }
    PartnerAccessory2D.FRENCH_MUSTACHE_SCARF,
    PartnerAccessory2D.TRAVEL_BERET_SCARF -> {
      drawRoundRect(
        color = spec.outfitSecondary,
        topLeft = Offset(w * 0.33f, h * 0.73f),
        size = Size(w * 0.34f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
      )
    }
    PartnerAccessory2D.GOLD_PENDANT -> {
      drawCircle(
        color = Color(0xFFFBBF24),
        radius = w * 0.026f,
        center = Offset(cx, h * 0.84f)
      )
    }
    PartnerAccessory2D.CYAN_TUTOR_HEADSET_PIN -> {
      drawCircle(
        color = Color(0xFF38BDF8),
        radius = w * 0.028f,
        center = Offset(w * 0.64f, h * 0.86f)
      )
    }
    else -> {}
  }

  // 4. Neck & Chin Shadow
  drawRoundRect(
    color = spec.skinShadow,
    topLeft = Offset(cx - w * 0.09f, h * 0.62f),
    size = Size(w * 0.18f, h * 0.17f),
    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
  )

  // 5. Ears & Earrings
  val earY = cy - h * 0.01f
  val earW = w * 0.085f
  val earH = h * 0.12f
  drawOval(
    color = spec.skinShadow,
    topLeft = Offset(cx - w * 0.285f, earY - earH * 0.5f),
    size = Size(earW, earH)
  )
  drawOval(
    color = spec.skinShadow,
    topLeft = Offset(cx + w * 0.20f, earY - earH * 0.5f),
    size = Size(earW, earH)
  )

  if (spec.accessory == PartnerAccessory2D.PEARL_STUDS) {
    drawCircle(Color(0xFFFFFBEB), radius = w * 0.022f, center = Offset(cx - w * 0.255f, earY + earH * 0.32f))
    drawCircle(Color(0xFFFFFBEB), radius = w * 0.022f, center = Offset(cx + w * 0.255f, earY + earH * 0.32f))
  } else if (spec.accessory == PartnerAccessory2D.GOLD_HOOPS) {
    drawCircle(
      color = Color(0xFFFBBF24),
      radius = w * 0.035f,
      center = Offset(cx - w * 0.255f, earY + earH * 0.38f),
      style = Stroke(width = w * 0.015f)
    )
    drawCircle(
      color = Color(0xFFFBBF24),
      radius = w * 0.035f,
      center = Offset(cx + w * 0.255f, earY + earH * 0.38f),
      style = Stroke(width = w * 0.015f)
    )
  }

  // 6. Head Shape (Friendly Modern 2D Emoji Proportion)
  val headW = w * (if (spec.isFemale) 0.48f else 0.50f)
  val headH = h * 0.54f
  val headTop = cy - h * 0.28f
  val headCorner = if (spec.isFemale) w * 0.23f else w * 0.20f
  drawRoundRect(
    color = spec.skinTone,
    topLeft = Offset(cx - headW * 0.5f, headTop),
    size = Size(headW, headH),
    cornerRadius = CornerRadius(headCorner, headCorner)
  )

  // Soft Cheek Blush
  val cheekY = cy + h * 0.02f
  drawOval(
    color = spec.cheekBlush.copy(alpha = if (isSpeaking) 0.38f else 0.26f),
    topLeft = Offset(cx - w * 0.19f, cheekY),
    size = Size(w * 0.095f, h * 0.055f)
  )
  drawOval(
    color = spec.cheekBlush.copy(alpha = if (isSpeaking) 0.38f else 0.26f),
    topLeft = Offset(cx + w * 0.095f, cheekY),
    size = Size(w * 0.095f, h * 0.055f)
  )

  // 7. Facial Hair (Beard / Stubble / Mustache for Male Partners)
  drawFacialHairIfPresent(spec, cx, cy, w, h)

  // 8. Eyes, Irises, Catchlights & Expressive Eyebrows
  val eyeY = cy - h * 0.045f
  val eyeOffsetX = w * 0.105f
  val eyeRadiusX = w * 0.044f
  val eyeRadiusY = h * 0.048f

  // Sclera
  drawOval(
    color = Color.White,
    topLeft = Offset(cx - eyeOffsetX - eyeRadiusX, eyeY - eyeRadiusY),
    size = Size(eyeRadiusX * 2f, eyeRadiusY * 2f)
  )
  drawOval(
    color = Color.White,
    topLeft = Offset(cx + eyeOffsetX - eyeRadiusX, eyeY - eyeRadiusY),
    size = Size(eyeRadiusX * 2f, eyeRadiusY * 2f)
  )

  // Iris
  val irisR = w * 0.030f
  drawCircle(color = spec.eyeColor, radius = irisR, center = Offset(cx - eyeOffsetX, eyeY))
  drawCircle(color = spec.eyeColor, radius = irisR, center = Offset(cx + eyeOffsetX, eyeY))

  // Pupil
  val pupilR = w * 0.016f
  drawCircle(color = Color(0xFF0F172A), radius = pupilR, center = Offset(cx - eyeOffsetX, eyeY))
  drawCircle(color = Color(0xFF0F172A), radius = pupilR, center = Offset(cx + eyeOffsetX, eyeY))

  // Sparkling Catchlight
  val catchR = w * 0.009f
  drawCircle(
    color = Color.White,
    radius = catchR,
    center = Offset(cx - eyeOffsetX - irisR * 0.32f, eyeY - irisR * 0.32f)
  )
  drawCircle(
    color = Color.White,
    radius = catchR,
    center = Offset(cx + eyeOffsetX - irisR * 0.32f, eyeY - irisR * 0.32f)
  )

  // Upper Eyelash Line
  val lashStroke = w * (if (spec.isFemale) 0.018f else 0.013f)
  drawArc(
    color = Color(0xFF1E1B18),
    startAngle = 195f,
    sweepAngle = 150f,
    useCenter = false,
    topLeft = Offset(cx - eyeOffsetX - eyeRadiusX, eyeY - eyeRadiusY),
    size = Size(eyeRadiusX * 2f, eyeRadiusY * 2f),
    style = Stroke(width = lashStroke, cap = StrokeCap.Round)
  )
  drawArc(
    color = Color(0xFF1E1B18),
    startAngle = 195f,
    sweepAngle = 150f,
    useCenter = false,
    topLeft = Offset(cx + eyeOffsetX - eyeRadiusX, eyeY - eyeRadiusY),
    size = Size(eyeRadiusX * 2f, eyeRadiusY * 2f),
    style = Stroke(width = lashStroke, cap = StrokeCap.Round)
  )

  // Eyebrows (lift slightly when speaking!)
  val browLift = if (isSpeaking) h * 0.014f * mouthOpenness else 0f
  val browY = eyeY - h * 0.075f - browLift
  val browStroke = w * (if (spec.isFemale) 0.018f else 0.024f)
  val leftBrowPath = Path().apply {
    moveTo(cx - eyeOffsetX - w * 0.048f, browY + h * 0.01f)
    quadraticTo(cx - eyeOffsetX, browY - h * 0.018f, cx - eyeOffsetX + w * 0.045f, browY + h * 0.005f)
  }
  val rightBrowPath = Path().apply {
    moveTo(cx + eyeOffsetX - w * 0.045f, browY + h * 0.005f)
    quadraticTo(cx + eyeOffsetX, browY - h * 0.018f, cx + eyeOffsetX + w * 0.048f, browY + h * 0.01f)
  }
  drawPath(leftBrowPath, color = spec.hairColor, style = Stroke(width = browStroke, cap = StrokeCap.Round))
  drawPath(rightBrowPath, color = spec.hairColor, style = Stroke(width = browStroke, cap = StrokeCap.Round))

  // 9. Cute 2D Button Nose
  val noseY = cy + h * 0.012f
  val nosePath = Path().apply {
    moveTo(cx - w * 0.018f, noseY)
    quadraticTo(cx, noseY + h * 0.024f, cx + w * 0.018f, noseY)
  }
  drawPath(
    path = nosePath,
    color = spec.skinShadow,
    style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
  )

  // 10. Animated Mouth (Idle Smile vs Live Viseme Speech Articulation)
  drawAnimatedMouth(
    spec = spec,
    cx = cx,
    mouthCenterY = cy + h * 0.115f,
    w = w,
    h = h,
    mouthOpenness = mouthOpenness,
    viseme = viseme,
    isSpeaking = isSpeaking
  )

  // 11. Glasses (Tortoise, Rose-Gold, Round Spectacles)
  drawGlassesIfPresent(spec, cx, eyeY, eyeOffsetX, w, h)

  // 12. Front Hair Bangs, Crown, Buns & Headwear
  drawFrontHairAndHeadwear(spec, cx, cy, headTop, w, h)
}

private fun DrawScope.drawAnimatedMouth(
  spec: PartnerCharacterVisualSpec,
  cx: Float,
  mouthCenterY: Float,
  w: Float,
  h: Float,
  mouthOpenness: Float,
  viseme: TutorViseme,
  isSpeaking: Boolean
) {
  if (!isSpeaking || mouthOpenness <= 0.06f) {
    // Calm, friendly expressive smile in idle state
    val smileHalfW = w * 0.078f * spec.smileCurvature.coerceIn(0.85f, 1.25f)
    val smileDepth = h * 0.042f * spec.smileCurvature.coerceIn(0.85f, 1.25f)
    val smilePath = Path().apply {
      moveTo(cx - smileHalfW, mouthCenterY - h * 0.008f)
      quadraticTo(cx, mouthCenterY + smileDepth, cx + smileHalfW, mouthCenterY - h * 0.008f)
    }
    drawPath(
      path = smilePath,
      color = spec.lipColor,
      style = Stroke(width = w * (if (spec.isFemale) 0.022f else 0.018f), cap = StrokeCap.Round)
    )
    return
  }

  // Speaking state: shape mouth width & height according to current viseme + openness
  val widthFactor = when (viseme) {
    TutorViseme.OO -> 0.58f
    TutorViseme.OH -> 0.72f
    TutorViseme.EE -> 1.15f
    TutorViseme.AA -> 1.05f
    TutorViseme.BMP -> 0.85f
    else -> 0.95f
  }
  val heightFactor = when (viseme) {
    TutorViseme.AA -> 1.18f
    TutorViseme.OH -> 1.10f
    TutorViseme.OO -> 0.92f
    TutorViseme.EE -> 0.75f
    TutorViseme.BMP -> 0.35f
    else -> 0.95f
  }

  val mouthW = w * 0.145f * widthFactor
  val mouthH = (h * 0.095f * mouthOpenness * heightFactor).coerceIn(h * 0.024f, h * 0.115f)
  val mouthRect = Rect(
    left = cx - mouthW * 0.5f,
    top = mouthCenterY - mouthH * 0.42f,
    right = cx + mouthW * 0.5f,
    bottom = mouthCenterY + mouthH * 0.58f
  )
  val mouthClipPath = Path().apply {
    addRoundRect(
      RoundRect(
        rect = mouthRect,
        cornerRadius = CornerRadius(mouthW * 0.48f, mouthH * 0.55f)
      )
    )
  }

  // Oral cavity
  drawPath(path = mouthClipPath, color = Color(0xFF591527))

  clipPath(mouthClipPath) {
    // Upper teeth
    drawRect(
      color = Color(0xFFF8FAFC),
      topLeft = Offset(mouthRect.left, mouthRect.top),
      size = Size(mouthRect.width, mouthH * 0.28f)
    )
    // Warm tongue
    drawOval(
      color = Color(0xFFE15B64),
      topLeft = Offset(cx - mouthW * 0.38f, mouthRect.bottom - mouthH * 0.52f),
      size = Size(mouthW * 0.76f, mouthH * 0.65f)
    )
  }

  // Lip contour
  drawPath(
    path = mouthClipPath,
    color = spec.lipColor,
    style = Stroke(width = w * 0.016f)
  )
}

private fun DrawScope.drawBackHairLayer(
  spec: PartnerCharacterVisualSpec,
  cx: Float,
  cy: Float,
  w: Float,
  h: Float
) {
  when (spec.hairStyle) {
    PartnerHairStyle2D.WAVY_SIDE_BOB,
    PartnerHairStyle2D.SLEEK_HEADBAND_WAVES,
    PartnerHairStyle2D.BEACH_WAVES_BERET,
    PartnerHairStyle2D.CURTAIN_BANGS_BOB -> {
      drawRoundRect(
        color = spec.hairColor,
        topLeft = Offset(cx - w * 0.31f, cy - h * 0.28f),
        size = Size(w * 0.62f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.24f, w * 0.24f)
      )
    }
    PartnerHairStyle2D.HIGH_BOUNCY_PONYTAIL -> {
      // High bouncy ponytail puff at top-right
      drawCircle(
        color = spec.hairColor,
        radius = w * 0.16f,
        center = Offset(cx + w * 0.18f, cy - h * 0.30f)
      )
      drawCircle(
        color = spec.hairHighlight,
        radius = w * 0.06f,
        center = Offset(cx + w * 0.21f, cy - h * 0.33f)
      )
    }
    PartnerHairStyle2D.ELEGANT_UPDO_BUN -> {
      // Top crown bun
      drawCircle(
        color = spec.hairColor,
        radius = w * 0.145f,
        center = Offset(cx, cy - h * 0.34f)
      )
    }
    PartnerHairStyle2D.YOUTHFUL_SPACE_BUNS -> {
      // Twin space buns + shoulder bob
      drawCircle(
        color = spec.hairColor,
        radius = w * 0.115f,
        center = Offset(cx - w * 0.21f, cy - h * 0.29f)
      )
      drawCircle(
        color = spec.hairColor,
        radius = w * 0.115f,
        center = Offset(cx + w * 0.21f, cy - h * 0.29f)
      )
      drawRoundRect(
        color = spec.hairColor,
        topLeft = Offset(cx - w * 0.29f, cy - h * 0.24f),
        size = Size(w * 0.58f, h * 0.48f),
        cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
      )
    }
    else -> {}
  }
}

private fun DrawScope.drawFrontHairAndHeadwear(
  spec: PartnerCharacterVisualSpec,
  cx: Float,
  cy: Float,
  headTop: Float,
  w: Float,
  h: Float
) {
  val hairCapPath = Path().apply {
    moveTo(cx - w * 0.26f, cy - h * 0.08f)
    quadraticTo(cx - w * 0.27f, headTop - h * 0.06f, cx, headTop - h * 0.06f)
    quadraticTo(cx + w * 0.27f, headTop - h * 0.06f, cx + w * 0.26f, cy - h * 0.08f)
    when (spec.hairStyle) {
      PartnerHairStyle2D.WAVY_SIDE_BOB,
      PartnerHairStyle2D.SLEEK_HEADBAND_WAVES,
      PartnerHairStyle2D.BEACH_WAVES_BERET -> {
        quadraticTo(cx + w * 0.08f, headTop + h * 0.07f, cx - w * 0.12f, headTop + h * 0.12f)
        quadraticTo(cx - w * 0.22f, headTop + h * 0.15f, cx - w * 0.26f, cy - h * 0.08f)
      }
      PartnerHairStyle2D.CURTAIN_BANGS_BOB,
      PartnerHairStyle2D.YOUTHFUL_SPACE_BUNS -> {
        quadraticTo(cx + w * 0.12f, headTop + h * 0.13f, cx, headTop + h * 0.05f)
        quadraticTo(cx - w * 0.12f, headTop + h * 0.13f, cx - w * 0.26f, cy - h * 0.08f)
      }
      PartnerHairStyle2D.EXECUTIVE_FADE -> {
        lineTo(cx + w * 0.24f, headTop + h * 0.08f)
        lineTo(cx - w * 0.24f, headTop + h * 0.08f)
        close()
      }
      PartnerHairStyle2D.TOUSLED_QUIFF,
      PartnerHairStyle2D.PROFESSOR_SILVER_SWEEP -> {
        quadraticTo(cx + w * 0.10f, headTop + h * 0.04f, cx - w * 0.24f, headTop + h * 0.11f)
      }
      else -> {
        quadraticTo(cx, headTop + h * 0.09f, cx - w * 0.26f, cy - h * 0.08f)
      }
    }
    close()
  }
  drawPath(hairCapPath, color = spec.hairColor)

  // Extra curls for Noah's curly crop
  if (spec.hairStyle == PartnerHairStyle2D.SOFT_CURLY_CROP) {
    val curlCenters = listOf(-0.16f, -0.08f, 0.0f, 0.08f, 0.16f)
    curlCenters.forEach { offsetFactor ->
      drawCircle(
        color = spec.hairColor,
        radius = w * 0.065f,
        center = Offset(cx + w * offsetFactor, headTop + h * 0.02f)
      )
    }
  }

  // Silver temple highlights for Ethan
  if (spec.hairStyle == PartnerHairStyle2D.PROFESSOR_SILVER_SWEEP) {
    drawArc(
      color = spec.hairHighlight,
      startAngle = 200f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(cx - w * 0.22f, headTop - h * 0.03f),
      size = Size(w * 0.44f, h * 0.18f),
      style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
    )
  }

  // Studio Headband for Olivia
  if (spec.accessory == PartnerAccessory2D.ROSE_GOLD_GLASSES_HEADBAND) {
    drawArc(
      color = Color(0xFF22D3EE),
      startAngle = 195f,
      sweepAngle = 150f,
      useCenter = false,
      topLeft = Offset(cx - w * 0.26f, headTop - h * 0.03f),
      size = Size(w * 0.52f, h * 0.24f),
      style = Stroke(width = w * 0.032f, cap = StrokeCap.Round)
    )
  }

  // Travel Beret for Amelia
  if (spec.accessory == PartnerAccessory2D.TRAVEL_BERET_SCARF) {
    val beretPath = Path().apply {
      moveTo(cx - w * 0.26f, headTop + h * 0.04f)
      quadraticTo(cx - w * 0.10f, headTop - h * 0.14f, cx + w * 0.28f, headTop + h * 0.01f)
      quadraticTo(cx + w * 0.10f, headTop + h * 0.07f, cx - w * 0.26f, headTop + h * 0.04f)
      close()
    }
    drawPath(beretPath, color = Color(0xFFEA580C))
  }

  // Pastel Hair Clip for Mia
  if (spec.accessory == PartnerAccessory2D.PASTEL_HAIR_CLIPS) {
    drawRoundRect(
      color = Color(0xFFF472B6),
      topLeft = Offset(cx - w * 0.20f, headTop + h * 0.06f),
      size = Size(w * 0.08f, h * 0.028f),
      cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
    )
  }
}

private fun DrawScope.drawFacialHairIfPresent(
  spec: PartnerCharacterVisualSpec,
  cx: Float,
  cy: Float,
  w: Float,
  h: Float
) {
  when (spec.accessory) {
    PartnerAccessory2D.EXECUTIVE_TIE,
    PartnerAccessory2D.PROFESSOR_BEARD -> {
      // Neatly groomed short beard along jawline
      val beardPath = Path().apply {
        moveTo(cx - w * 0.22f, cy + h * 0.04f)
        quadraticTo(cx - w * 0.20f, cy + h * 0.26f, cx, cy + h * 0.27f)
        quadraticTo(cx + w * 0.20f, cy + h * 0.26f, cx + w * 0.22f, cy + h * 0.04f)
        quadraticTo(cx + w * 0.14f, cy + h * 0.20f, cx, cy + h * 0.21f)
        quadraticTo(cx - w * 0.14f, cy + h * 0.20f, cx - w * 0.22f, cy + h * 0.04f)
        close()
      }
      drawPath(beardPath, color = spec.hairColor.copy(alpha = 0.85f))
    }
    PartnerAccessory2D.FRENCH_MUSTACHE_SCARF -> {
      // Stylish French mustache + chin soul patch
      val mustacheY = cy + h * 0.065f
      val leftMustache = Path().apply {
        moveTo(cx, mustacheY)
        quadraticTo(cx - w * 0.05f, mustacheY - h * 0.018f, cx - w * 0.09f, mustacheY + h * 0.008f)
      }
      val rightMustache = Path().apply {
        moveTo(cx, mustacheY)
        quadraticTo(cx + w * 0.05f, mustacheY - h * 0.018f, cx + w * 0.09f, mustacheY + h * 0.008f)
      }
      drawPath(leftMustache, color = spec.hairColor, style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))
      drawPath(rightMustache, color = spec.hairColor, style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))
    }
    PartnerAccessory2D.LIGHT_STUBBLE -> {
      drawArc(
        color = spec.hairColor.copy(alpha = 0.25f),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - w * 0.19f, cy + h * 0.02f),
        size = Size(w * 0.38f, h * 0.22f),
        style = Stroke(width = w * 0.032f, cap = StrokeCap.Round)
      )
    }
    else -> {}
  }
}

private fun DrawScope.drawGlassesIfPresent(
  spec: PartnerCharacterVisualSpec,
  cx: Float,
  eyeY: Float,
  eyeOffsetX: Float,
  w: Float,
  h: Float
) {
  val frameColor = when (spec.accessory) {
    PartnerAccessory2D.TORTOISE_GLASSES -> Color(0xFF78350F)
    PartnerAccessory2D.ROSE_GOLD_GLASSES_HEADBAND -> Color(0xFFFDA4AF)
    PartnerAccessory2D.ROUND_SPECTACLES -> Color(0xFF38BDF8)
    else -> return
  }
  val lensR = w * 0.072f
  val strokeW = w * 0.016f
  drawCircle(
    color = frameColor,
    radius = lensR,
    center = Offset(cx - eyeOffsetX, eyeY),
    style = Stroke(width = strokeW)
  )
  drawCircle(
    color = frameColor,
    radius = lensR,
    center = Offset(cx + eyeOffsetX, eyeY),
    style = Stroke(width = strokeW)
  )
  drawLine(
    color = frameColor,
    start = Offset(cx - eyeOffsetX + lensR, eyeY),
    end = Offset(cx + eyeOffsetX - lensR, eyeY),
    strokeWidth = strokeW
  )
}
