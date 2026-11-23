package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.BillingRepository
import com.example.data.repository.PlanPricingInfo
import com.example.ui.theme.LingoThemeController
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared LingoSphere Design System tokens & primitives.
 * - Background #070B14 with soft top indigo glow
 * - Glass cards rgba(255,255,255,0.05) + 1dp white 8% border, 24dp radius, 20dp padding
 * - Brand gradient #6C7BFF -> #2DD4FF strictly on primary actions/hero
 * - Gold #F5C451 for Pro, lock, streak, and XP
 * - White titles, #A8B3CF secondary, 15sp body, 13sp minimum labels
 */
object LingoTokens {
  val bgCanvas: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF070B14) else Color(0xFFF6F8FC)

  val bgElevated: Color
    get() = if (LingoThemeController.isDarkActive) Color.White.copy(alpha = 0.06f) else Color(0xFFFFFFFF)

  val bgGlassCard: Color
    get() = if (LingoThemeController.isDarkActive) Color.White.copy(alpha = 0.06f) else Color(0xFFFFFFFF)

  val bgSubtle: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF131A2A) else Color(0xFFEEF2F8)

  val bgInteractive: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF1A2236) else Color(0xFFE2E8F0)

  // 1px glass border (white at 10%)
  val borderSubtle: Color
    get() = if (LingoThemeController.isDarkActive) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)

  val borderFocus: Color
    get() = if (LingoThemeController.isDarkActive) Color.White.copy(alpha = 0.18f) else Color(0xFF94A3B8)

  // Signature Brand Gradient Endpoints (#6C63FF -> #22D3EE)
  val brandIndigo: Color = Color(0xFF6C63FF)
  val brandCyan: Color = Color(0xFF22D3EE)

  val brandGradientColors: List<Color> = listOf(brandIndigo, brandCyan)

  val brandGradient: Brush
    get() = Brush.linearGradient(colors = brandGradientColors)

  val brandHeroGradient: Brush
    get() = brandGradient

  val brandHorizontalGradient: Brush
    get() = Brush.horizontalGradient(colors = brandGradientColors)

  // Primary accents mapped cleanly to the brand palette
  val accentPrimary: Color
    get() = if (LingoThemeController.isDarkActive) brandCyan else Color(0xFF0284C7)

  val accentBlue: Color
    get() = if (LingoThemeController.isDarkActive) brandIndigo else Color(0xFF4F5FEF)

  val accentIndigo: Color
    get() = if (LingoThemeController.isDarkActive) brandIndigo else Color(0xFF4F5FEF)

  val accentEmerald: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF22C55E) else Color(0xFF059669)

  // Gold #F5C451 strictly for Pro, Streak, XP
  val accentGold: Color = Color(0xFFF5C451)

  val accentAmber: Color
    get() = if (LingoThemeController.isDarkActive) accentGold else Color(0xFFB45309)

  val brandAmber: Color
    get() = accentAmber

  val accentRose: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFFEF4444) else Color(0xFFDC2626)

  val textPrimary: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFFFFFFFF) else Color(0xFF0B0F1A)

  val textSecondary: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFFA8B3CF) else Color(0xFF475569)

  // Never dimmer than #A8B3CF on dark
  val textMuted: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFFA8B3CF) else Color(0xFF64748B)

  val textTertiary: Color
    get() = textMuted

  // Dark ink #0B0F1A for crisp contrast on gradients & gold badges
  val onGradientInk: Color = Color(0xFF0B0F1A)
  val onAccentDark: Color = Color(0xFF0B0F1A)

  val onAccent: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF0B0F1A) else Color.White

  val innerHighlight: Color
    get() = borderSubtle

  val statusOnline: Color = Color(0xFF22C55E)

  val canvasNavy: Color
    get() = bgCanvas

  val surfacePrimary: Color
    get() = bgElevated

  val surfaceCard: Color
    get() = bgElevated

  val surfaceElevated: Color
    get() = bgSubtle

  val goldAccent: Color
    get() = accentGold

  val cardRadius: Dp = 24.dp
  val screenMargin: Dp = 20.dp
  val screenPadding: Dp
    get() = screenMargin
  val borderGlass: Color
    get() = borderSubtle
  val sectionSpacing: Dp = 24.dp
  val bottomBarClearance: Dp = 112.dp
}

/**
 * Consistent level formatting across every screen: "A1 Beginner", "A2 Elementary", "B1 Intermediate", etc.
 */
fun formatCefrLevelLabel(cefrOrStage: String?, fallbackProficiency: String = "Beginner"): String {
  val normalized = cefrOrStage?.trim()?.uppercase().orEmpty()
  val code = when {
    normalized.startsWith("A1") -> "A1"
    normalized.startsWith("A2") -> "A2"
    normalized.startsWith("B1") -> "B1"
    normalized.startsWith("B2") -> "B2"
    normalized.startsWith("C1") -> "C1"
    normalized.startsWith("C2") -> "C2"
    else -> when (fallbackProficiency.trim().lowercase()) {
      "elementary" -> "A2"
      "intermediate" -> "B1"
      "upper-intermediate", "upper intermediate" -> "B2"
      "advanced" -> "C1"
      "proficient", "native", "fluent" -> "C2"
      else -> "A1"
    }
  }
  val title = when (code) {
    "A1" -> "Beginner"
    "A2" -> "Elementary"
    "B1" -> "Intermediate"
    "B2" -> "Upper-Intermediate"
    "C1" -> "Advanced"
    "C2" -> "Proficient"
    else -> "Beginner"
  }
  return "$code $title"
}

fun extractCefrCodeOnly(cefrOrStage: String?, fallbackProficiency: String = "Beginner"): String {
  return formatCefrLevelLabel(cefrOrStage, fallbackProficiency).substringBefore(" ")
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    val isRobolectric = android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    if (isRobolectric) {
      true
    } else {
      runCatching {
        val scale = android.provider.Settings.Global.getFloat(
          context.contentResolver,
          android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
          1f
        )
        scale == 0f
      }.getOrDefault(false)
    }
  }
}

/**
 * Full-screen canvas backdrop (#070B14) with a soft top indigo radial glow (#6C63FF).
 */
fun Modifier.lingoAmbientBackdrop(
  baseColor: Color? = null,
  showGlow: Boolean = true
): Modifier = composed {
  val isDark = LingoThemeController.isDarkActive
  val canvas = baseColor ?: LingoTokens.bgCanvas
  this
    .background(canvas)
    .drawBehind {
      if (isDark && showGlow) {
        val radius = size.width * 0.92f
        val center = Offset(size.width * 0.5f, -size.width * 0.14f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              LingoTokens.brandIndigo.copy(alpha = 0.22f),
              LingoTokens.brandCyan.copy(alpha = 0.05f),
              Color.Transparent
            ),
            center = center,
            radius = radius
          ),
          radius = radius,
          center = center
        )
      }
    }
}

/**
 * Subtle drifting aurora backdrop for Home (#070B14 + indigo top-left + cyan bottom-right, 20s loop).
 * Respects system reduced-motion preferences.
 */
fun Modifier.lingoAuroraBackdrop(
  baseColor: Color? = null
): Modifier = composed {
  val isDark = LingoThemeController.isDarkActive
  val reducedMotion = rememberReducedMotionEnabled()
  val canvas = baseColor ?: LingoTokens.bgCanvas
  val phase: Float = if (!reducedMotion && isDark) {
    val infinite = rememberInfiniteTransition(label = "home_aurora")
    val anim by infinite.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 20_000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "home_aurora_phase"
    )
    anim
  } else {
    0f
  }

  this
    .background(canvas)
    .drawBehind {
      if (isDark) {
        val driftX = cos(phase) * size.width * 0.06f
        val driftY = sin(phase) * size.height * 0.035f

        val topLeftCenter = Offset(
          x = size.width * 0.14f + driftX,
          y = size.height * 0.06f + driftY
        )
        val topLeftRadius = size.width * 0.95f
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              LingoTokens.brandIndigo.copy(alpha = 0.20f),
              LingoTokens.brandIndigo.copy(alpha = 0.05f),
              Color.Transparent
            ),
            center = topLeftCenter,
            radius = topLeftRadius
          ),
          radius = topLeftRadius,
          center = topLeftCenter
        )

        val bottomRightCenter = Offset(
          x = size.width * 0.88f - driftX,
          y = size.height * 0.86f - driftY
        )
        val bottomRightRadius = size.width * 0.88f
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              LingoTokens.brandCyan.copy(alpha = 0.14f),
              LingoTokens.brandCyan.copy(alpha = 0.03f),
              Color.Transparent
            ),
            center = bottomRightCenter,
            radius = bottomRightRadius
          ),
          radius = bottomRightRadius,
          center = bottomRightCenter
        )
      }
    }
}

/**
 * Staggered fade + gentle slide-in on load (40ms steps, 200ms transition), respecting reduced motion.
 */
fun Modifier.lingoStaggeredAppear(
  index: Int,
  stepMs: Int = 40
): Modifier = composed {
  val reducedMotion = rememberReducedMotionEnabled()
  var visible by remember { mutableStateOf(reducedMotion) }
  LaunchedEffect(reducedMotion) {
    if (!reducedMotion && !visible) {
      if (index > 0) kotlinx.coroutines.delay((index * stepMs).toLong())
      visible = true
    }
  }
  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 220, easing = FastOutSlowInEasing),
    label = "stagger_alpha_$index"
  )
  val translationY by animateFloatAsState(
    targetValue = if (visible) 0f else 18f,
    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 220, easing = FastOutSlowInEasing),
    label = "stagger_ty_$index"
  )
  this.graphicsLayer {
    this.alpha = alpha
    this.translationY = translationY
  }
}

/**
 * Spring scale animation + light haptic feedback on tap.
 */
fun Modifier.springTap(
  enabled: Boolean = true,
  scaleDown: Float = 0.97f,
  onClick: () -> Unit
): Modifier = composed {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) scaleDown else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "spring_tap_scale"
  )
  this
    .scale(scale)
    .clickable(
      interactionSource = interactionSource,
      indication = null,
      enabled = enabled
    ) {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    }
}

@Composable
fun LingoTopBar(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  backTestTag: String? = null,
  titleTestTag: String? = null,
  actions: @Composable RowScope.() -> Unit = {}
) {
  Surface(
    color = Color.Transparent,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (onBack != null) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(48.dp)
                .then(if (backTestTag != null) Modifier.testTag(backTestTag) else Modifier)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = LingoTokens.textPrimary
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          } else {
            Spacer(modifier = Modifier.width(8.dp))
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = LingoTokens.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier
            )
            if (!subtitle.isNullOrBlank()) {
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                color = LingoTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          content = actions
        )
      }
      HorizontalDivider(
        color = LingoTokens.borderSubtle,
        thickness = 1.dp
      )
    }
  }
}

@Composable
fun LingoSectionHeader(
  title: String,
  subtitle: String? = null,
  actionLabel: String? = null,
  actionTestTag: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = LingoTokens.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          fontSize = 14.sp,
          color = LingoTokens.textSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (!actionLabel.isNullOrBlank() && onAction != null) {
      TextButton(
        onClick = onAction,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .then(if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier)
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.accentPrimary,
          maxLines = 1
        )
      }
    }
  }
}

/**
 * Primary surface card: #121826, 24dp radius, 1px inner highlight (white at 8%), zero hard borders.
 */
@Composable
fun LingoCleanCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  containerColor: Color = LingoTokens.bgElevated,
  borderColor: Color = LingoTokens.borderSubtle,
  borderWidth: Dp = 1.dp,
  shape: Shape = RoundedCornerShape(LingoTokens.cardRadius),
  contentPadding: Dp = 20.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  val interactiveMod = if (onClick != null) {
    modifier.springTap(onClick = onClick)
  } else {
    modifier
  }
  val isDark = LingoThemeController.isDarkActive
  Card(
    modifier = interactiveMod,
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(borderWidth, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .drawBehind {
          if (isDark) {
            drawLine(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color.White.copy(alpha = 0.16f),
                  Color.Transparent
                )
              ),
              start = Offset(size.width * 0.12f, 1f),
              end = Offset(size.width * 0.88f, 1f),
              strokeWidth = 1.5f
            )
          }
        }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(contentPadding),
        content = content
      )
    }
  }
}

/**
 * Brand gradient primary CTA (#6C7BFF -> #2DD4FF) with dark ink #0B0F1A text for high contrast.
 */
@Composable
fun LingoPrimaryGradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String? = null
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.97f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "primary_btn_scale"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .scale(scale)
      .heightIn(min = 50.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(
        brush = if (enabled) LingoTokens.brandHorizontalGradient else Brush.horizontalGradient(
          listOf(LingoTokens.bgSubtle, LingoTokens.bgSubtle)
        )
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled
      ) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      }
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) LingoTokens.onGradientInk else LingoTokens.textSecondary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontSize = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (enabled) LingoTokens.onGradientInk else LingoTokens.textSecondary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Secondary button / mode chip (#182033 surface, 1px white 8% inner highlight, >= 44dp touch target).
 */
@Composable
fun LingoSecondaryChip(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  selected: Boolean = false,
  testTag: String? = null
) {
  val shape = RoundedCornerShape(18.dp)
  val bgModifier = if (selected) {
    Modifier.background(LingoTokens.brandHorizontalGradient, shape)
  } else {
    Modifier
      .background(LingoTokens.bgSubtle, shape)
      .border(1.dp, LingoTokens.borderSubtle, shape)
  }
  val contentColor = if (selected) LingoTokens.onGradientInk else LingoTokens.textPrimary

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .heightIn(min = 44.dp)
      .clip(shape)
      .then(bgModifier)
      .springTap(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Gold #F5C451 badge/chip strictly reserved for PRO badge, Streak, and XP.
 */
@Composable
fun LingoProBadge(
  modifier: Modifier = Modifier,
  label: String = "PRO",
  onClick: (() -> Unit)? = null
) {
  val mod = if (onClick != null) modifier.springTap(onClick = onClick) else modifier
  Surface(
    color = LingoTokens.accentGold,
    shape = RoundedCornerShape(50),
    modifier = mod
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.WorkspacePremium,
        contentDescription = label,
        tint = LingoTokens.onGradientInk,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = LingoTokens.onGradientInk,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
fun LingoGoldChip(
  text: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  val base = modifier
    .heightIn(min = 38.dp)
    .clip(RoundedCornerShape(50))
    .background(LingoTokens.accentGold.copy(alpha = 0.14f))
    .border(1.dp, LingoTokens.accentGold.copy(alpha = 0.28f), RoundedCornerShape(50))
  val finalMod = if (onClick != null) base.springTap(onClick = onClick) else base
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = finalMod.padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = LingoTokens.accentGold,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = LingoTokens.accentGold,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
fun LingoStatusBadge(
  text: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(50),
    modifier = modifier
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

/**
 * Gradient ring avatar with optional edit badge for Header & Profile.
 */
@Composable
fun LingoGradientAvatar(
  name: String,
  userId: String,
  size: Dp = 52.dp,
  customImageUrl: String = "",
  showEditBadge: Boolean = false,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .then(if (onClick != null) Modifier.springTap(onClick = onClick) else Modifier)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .clip(CircleShape)
        .background(LingoTokens.brandGradient)
        .padding(2.5.dp)
        .clip(CircleShape)
        .background(LingoTokens.bgCanvas)
        .padding(2.dp)
    ) {
      LingoAvatar(
        name = name,
        userId = userId,
        size = size - 9.dp,
        customImageUrl = customImageUrl,
        isOnline = false,
        isPro = false
      )
    }

    if (showEditBadge) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(26.dp)
          .clip(CircleShape)
          .background(LingoTokens.brandGradient)
          .border(2.dp, LingoTokens.bgElevated, CircleShape)
      ) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = "Edit photo",
          tint = LingoTokens.onGradientInk,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

/**
 * Circular gradient progress ring (#6C7BFF -> #2DD4FF) for Daily Goal.
 */
@Composable
fun LingoGradientProgressRing(
  progress: Float,
  size: Dp = 74.dp,
  strokeWidth: Dp = 8.dp,
  modifier: Modifier = Modifier,
  centerContent: @Composable BoxScope.() -> Unit
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    label = "gradient_ring_progress"
  )
  val trackColor = LingoTokens.bgSubtle
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = strokeWidth.toPx()
      val inset = strokePx / 2f
      val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )
      if (animatedProgress > 0.005f) {
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(
              LingoTokens.brandIndigo,
              LingoTokens.brandCyan,
              LingoTokens.brandIndigo
            )
          ),
          startAngle = -90f,
          sweepAngle = 360f * animatedProgress,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
      }
    }
    centerContent()
  }
}

/**
 * Slim gradient progress bar (#6C7BFF -> #2DD4FF).
 */
@Composable
fun LingoGradientProgressBar(
  progress: Float,
  modifier: Modifier = Modifier,
  height: Dp = 6.dp
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
    label = "gradient_bar_progress"
  )
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(50))
      .background(LingoTokens.bgSubtle)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(animatedProgress.coerceAtLeast(if (progress > 0f) 0.04f else 0f))
        .height(height)
        .clip(RoundedCornerShape(50))
        .background(LingoTokens.brandHorizontalGradient)
    )
  }
}

/**
 * Glowing animated orb for Emma / AI tutor — pulses gently when idle and responds smoothly when speaking.
 * Zero emojis.
 */
@Composable
fun LingoTutorOrb(
  size: Dp = 64.dp,
  isSpeaking: Boolean = false,
  audioLevel: Float = 0f,
  modifier: Modifier = Modifier
) {
  val isRobolectric = remember {
    android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
  }
  var animateOrb by remember { mutableStateOf(isRobolectric || isSpeaking) }
  LaunchedEffect(isSpeaking) {
    if (isSpeaking) {
      animateOrb = true
    } else if (!animateOrb) {
      kotlinx.coroutines.delay(850L)
      animateOrb = true
    }
  }

  val idlePulse: Float
  val orbitAngle: Float
  if (animateOrb) {
    val infiniteTransition = rememberInfiniteTransition(label = "tutor_orb")
    val animatedPulse by infiniteTransition.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.08f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = if (isSpeaking) 520 else 2200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "orb_pulse"
    )
    val animatedOrbit by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = if (isSpeaking) 2600 else 6400, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "orb_orbit"
    )
    idlePulse = animatedPulse
    orbitAngle = animatedOrbit
  } else {
    idlePulse = 1.0f
    orbitAngle = 0f
  }

  val activeBoost = if (isSpeaking) (0.08f + audioLevel.coerceIn(0f, 1f) * 0.14f) else 0f
  val outerScale = (idlePulse + activeBoost).coerceIn(0.88f, 1.24f)

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val baseR = minOf(cx, cy)

      // Outer ambient halo
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            LingoTokens.brandCyan.copy(alpha = if (isSpeaking) 0.44f else 0.26f),
            LingoTokens.brandIndigo.copy(alpha = if (isSpeaking) 0.24f else 0.12f),
            Color.Transparent
          ),
          center = Offset(cx, cy),
          radius = baseR * outerScale
        ),
        radius = baseR * outerScale,
        center = Offset(cx, cy)
      )

      // Core glowing sphere
      val coreR = baseR * 0.68f * (if (isSpeaking) (1f + activeBoost * 0.35f) else 1f)
      rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
        drawCircle(
          brush = Brush.linearGradient(
            colors = listOf(
              LingoTokens.brandIndigo,
              LingoTokens.brandCyan,
              Color(0xFFA5F3FC)
            ),
            start = Offset(cx - coreR, cy - coreR),
            end = Offset(cx + coreR, cy + coreR)
          ),
          radius = coreR,
          center = Offset(cx, cy)
        )
      }

      // Soft glass specular highlight
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.60f),
            Color.White.copy(alpha = 0.0f)
          ),
          center = Offset(cx - coreR * 0.28f, cy - coreR * 0.28f),
          radius = coreR * 0.58f
        ),
        radius = coreR * 0.58f,
        center = Offset(cx - coreR * 0.28f, cy - coreR * 0.28f)
      )
    }
  }
}

/**
 * ONE unified tutor card used across Home, Learn, and Speak for strict cross-screen consistency.
 */
@Composable
fun LingoUnifiedTutorCard(
  tutorName: String,
  roleSubtitle: String,
  statusText: String = "Online",
  isSpeaking: Boolean = false,
  audioLevel: Float = 0f,
  chips: List<String> = emptyList(),
  onPlayVoicePreview: (() -> Unit)? = null,
  playVoiceTestTag: String? = null,
  primaryButtonLabel: String? = null,
  primaryButtonIcon: ImageVector? = Icons.Outlined.Mic,
  primaryButtonTestTag: String? = null,
  onPrimaryClick: (() -> Unit)? = null,
  secondaryButtonLabel: String? = null,
  secondaryButtonIcon: ImageVector? = Icons.Outlined.Videocam,
  secondaryButtonTestTag: String? = null,
  onSecondaryClick: (() -> Unit)? = null,
  cardTestTag: String? = null,
  modifier: Modifier = Modifier,
  extraFooter: (@Composable ColumnScope.() -> Unit)? = null
) {
  LingoCleanCard(
    modifier = modifier
      .fillMaxWidth()
      .then(if (cardTestTag != null) Modifier.testTag(cardTestTag) else Modifier),
    contentPadding = 20.dp
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      LingoTutorOrb(
        size = 64.dp,
        isSpeaking = isSpeaking,
        audioLevel = audioLevel
      )

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = tutorName,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LingoTokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(LingoTokens.accentEmerald.copy(alpha = 0.14f))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(LingoTokens.accentEmerald)
            )
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.accentEmerald,
              maxLines = 1
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = roleSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          fontSize = 14.sp,
          color = LingoTokens.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (onPlayVoicePreview != null) {
        IconButton(
          onClick = onPlayVoicePreview,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(LingoTokens.bgSubtle)
            .border(1.dp, LingoTokens.borderSubtle, CircleShape)
            .then(if (playVoiceTestTag != null) Modifier.testTag(playVoiceTestTag) else Modifier)
        ) {
          Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = "Play voice preview",
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }

    if (chips.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        chips.forEach { chip ->
          Surface(
            color = LingoTokens.bgSubtle,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle)
          ) {
            Text(
              text = chip,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = LingoTokens.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }
    }

    if (primaryButtonLabel != null && onPrimaryClick != null) {
      Spacer(modifier = Modifier.height(16.dp))
      if (secondaryButtonLabel != null && onSecondaryClick != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          LingoPrimaryGradientButton(
            text = primaryButtonLabel,
            icon = primaryButtonIcon,
            onClick = onPrimaryClick,
            testTag = primaryButtonTestTag,
            modifier = Modifier.weight(1f)
          )
          LingoSecondaryChip(
            text = secondaryButtonLabel,
            icon = secondaryButtonIcon,
            onClick = onSecondaryClick,
            testTag = secondaryButtonTestTag,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 50.dp)
          )
        }
      } else {
        LingoPrimaryGradientButton(
          text = primaryButtonLabel,
          icon = primaryButtonIcon,
          onClick = onPrimaryClick,
          testTag = primaryButtonTestTag,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    if (extraFooter != null) {
      Spacer(modifier = Modifier.height(12.dp))
      extraFooter()
    }
  }
}

/**
 * Uniform grouped list row: outline icon tile, white bold title, #A8B3CF subtitle, chevron.
 */
@Composable
fun LingoListRow(
  icon: ImageVector,
  title: String,
  subtitle: String? = null,
  onClick: () -> Unit,
  iconTint: Color = LingoTokens.brandCyan,
  trailingBadge: String? = null,
  trailingBadgeColor: Color = LingoTokens.accentGold,
  testTag: String? = null,
  modifier: Modifier = Modifier
) {
  LingoCleanCard(
    onClick = onClick,
    contentPadding = 16.dp,
    shape = RoundedCornerShape(22.dp),
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(LingoTokens.bgSubtle)
          .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(14.dp))
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = LingoTokens.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (!trailingBadge.isNullOrBlank()) {
        Spacer(modifier = Modifier.width(8.dp))
        LingoStatusBadge(text = trailingBadge, color = trailingBadgeColor)
      }

      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = "Open $title",
        tint = LingoTokens.textSecondary,
        modifier = Modifier.size(22.dp)
      )
    }
  }
}

/**
 * Shimmer skeleton placeholder card for loading states.
 */
@Composable
fun LingoSkeletonCard(
  height: Dp = 96.dp,
  modifier: Modifier = Modifier
) {
  val infinite = rememberInfiniteTransition(label = "skeleton")
  val alpha by infinite.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "skeleton_alpha"
  )
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(LingoTokens.cardRadius))
      .background(LingoTokens.bgSubtle.copy(alpha = alpha))
      .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(LingoTokens.cardRadius))
      .padding(18.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.55f)
          .height(14.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(LingoTokens.bgSubtle.copy(alpha = alpha))
      )
      Box(
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .height(12.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(LingoTokens.bgSubtle.copy(alpha = alpha * 0.7f))
      )
    }
  }
}

/**
 * Lightweight animated confetti celebration overlay for lesson completion.
 */
@Composable
fun LingoConfettiOverlay(
  modifier: Modifier = Modifier
) {
  val infinite = rememberInfiniteTransition(label = "confetti")
  val progress by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "confetti_fall"
  )

  val colors = remember {
    listOf(
      LingoTokens.brandIndigo,
      LingoTokens.brandCyan,
      LingoTokens.accentGold,
      Color.White
    )
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val count = 28
    for (i in 0 until count) {
      val seed = (i * 73 + 19) % 100
      val xFrac = ((seed / 100f) + sin(progress * 2f * PI.toFloat() + i) * 0.04f).coerceIn(0.03f, 0.97f)
      val yFrac = ((progress + (i * 0.037f)) % 1f)
      val cx = size.width * xFrac
      val cy = size.height * yFrac
      val w = (6f + (i % 4) * 2.5f).dp.toPx()
      val h = (3.5f + (i % 3) * 1.5f).dp.toPx()
      val angle = (progress * 360f * (if (i % 2 == 0) 1f else -1f) + i * 24f) % 360f
      val alpha = (1f - yFrac).coerceIn(0.15f, 0.9f)
      rotate(degrees = angle, pivot = Offset(cx, cy)) {
        drawRect(
          color = colors[i % colors.size].copy(alpha = alpha),
          topLeft = Offset(cx - w / 2f, cy - h / 2f),
          size = Size(w, h)
        )
      }
    }
  }
}

@Composable
fun LingoEmptyOrErrorCard(
  title: String,
  message: String,
  isError: Boolean = false,
  isLoading: Boolean = false,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  LingoCleanCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = if (isError) LingoTokens.accentRose.copy(alpha = 0.08f) else LingoTokens.bgElevated,
    borderColor = if (isError) LingoTokens.accentRose.copy(alpha = 0.25f) else LingoTokens.borderSubtle
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (isLoading) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50)),
          color = LingoTokens.brandCyan,
          trackColor = LingoTokens.bgSubtle
        )
      }
      Surface(
        shape = CircleShape,
        color = (if (isError) LingoTokens.accentRose else LingoTokens.brandCyan).copy(alpha = 0.14f),
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.Inbox,
            contentDescription = null,
            tint = if (isError) LingoTokens.accentRose else LingoTokens.brandCyan,
            modifier = Modifier.size(24.dp)
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = LingoTokens.textPrimary,
        textAlign = TextAlign.Center
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 14.sp,
        color = LingoTokens.textSecondary,
        textAlign = TextAlign.Center
      )
      if (!actionLabel.isNullOrBlank() && onAction != null) {
        OutlinedButton(
          onClick = onAction,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, LingoTokens.borderFocus),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.brandCyan),
          modifier = Modifier.heightIn(min = 48.dp)
        ) {
          Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(actionLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Gold lock indicator shown on Pro-only items for Free/signed-out learners.
 */
@Composable
fun LingoGoldLockIcon(
  modifier: Modifier = Modifier,
  size: Dp = 18.dp,
  contentDescription: String = "LingoSphere Pro feature"
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size + 10.dp)
      .clip(CircleShape)
      .background(LingoTokens.accentGold.copy(alpha = 0.16f))
      .border(1.dp, LingoTokens.accentGold.copy(alpha = 0.35f), CircleShape)
  ) {
    Icon(
      imageVector = Icons.Outlined.Lock,
      contentDescription = contentDescription,
      tint = LingoTokens.accentGold,
      modifier = Modifier.size(size * 0.78f)
    )
  }
}

/**
 * Clean underline tab bar (used for Review/Saved/AI tabs in Vocabulary Builder and similar screens).
 */
@Composable
fun LingoUnderlineTabBar(
  tabs: List<String>,
  selectedIndex: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier,
  lockedIndices: Set<Int> = emptySet()
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    tabs.forEachIndexed { idx, label ->
      val selected = idx == selectedIndex
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable { onSelectTab(idx) }
          .padding(top = 10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) LingoTokens.textPrimary else LingoTokens.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (lockedIndices.contains(idx)) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = "Pro",
              tint = LingoTokens.accentGold,
              modifier = Modifier.size(14.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(
              if (selected) LingoTokens.brandHorizontalGradient
              else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
        )
      }
    }
  }
}

/**
 * Persistent calm banner shown at the top of the app whenever connectivity is offline.
 */
@Composable
fun LingoOfflineTopBanner(
  visible: Boolean,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
    exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
    modifier = modifier
  ) {
    Surface(
      color = LingoTokens.bgSubtle,
      border = BorderStroke(1.dp, LingoTokens.borderFocus),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("lingo_offline_top_banner"),
      shape = RoundedCornerShape(16.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.CloudOff,
          contentDescription = null,
          tint = LingoTokens.accentGold,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "You're offline — saved flashcards and downloaded books remain available.",
          color = LingoTokens.textPrimary,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * Calm inline AI error banner with Retry action and automatic 5s dismissal.
 */
@Composable
fun LingoCalmAiErrorBanner(
  message: String?,
  onRetry: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null,
  autoDismissMillis: Long = 5000L,
  modifier: Modifier = Modifier
) {
  val sanitized = remember(message) {
    message?.let { com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(it) }
  }
  LaunchedEffect(sanitized) {
    if (!sanitized.isNullOrBlank() && onDismiss != null && autoDismissMillis > 0L) {
      kotlinx.coroutines.delay(autoDismissMillis)
      onDismiss()
    }
  }
  AnimatedVisibility(
    visible = !sanitized.isNullOrBlank(),
    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
    exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
    modifier = modifier
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = LingoTokens.bgElevated,
      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("lingo_calm_ai_error_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Refresh,
          contentDescription = null,
          tint = LingoTokens.brandCyan,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = sanitized.orEmpty(),
          color = LingoTokens.textSecondary,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          modifier = Modifier.weight(1f)
        )
        if (onRetry != null) {
          TextButton(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text(
              text = "Retry",
              color = LingoTokens.brandCyan,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        if (onDismiss != null) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Dismiss",
              tint = LingoTokens.textSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Global LingoSphere Pro Upgrade bottom sheet triggered cleanly by PRO_REQUIRED / locked items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LingoGlobalUpgradeSheet(
  request: com.example.ai.UpgradePromptRequest,
  isSignedIn: Boolean,
  userProfile: com.example.data.model.UserProfile? = null,
  billingRepository: BillingRepository? = null,
  onDismiss: () -> Unit,
  onSelectPlan: (productId: String) -> Unit,
  onOpenFullComparison: () -> Unit,
  onSignInRequested: () -> Unit = onDismiss
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val pricingMap by (
    billingRepository?.planPricingMap
      ?: kotlinx.coroutines.flow.MutableStateFlow<Map<String, PlanPricingInfo>>(emptyMap())
    ).collectAsState()
  val monthlyPrice = pricingMap[com.example.data.model.SubscriptionPlan.PRODUCT_ID_MONTHLY]
    ?.formattedPrice?.takeIf { it.isNotBlank() } ?: "$9.99/month"
  val yearlyPrice = pricingMap[com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY]
    ?.formattedPrice?.takeIf { it.isNotBlank() } ?: "$79.99/year"
  var selectedPlanId by remember {
    mutableStateOf(com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY)
  }

  val adState by com.example.ads.AdService.state.collectAsState()
  val isVerifiedPro = remember(userProfile, adState.isUserVerifiedPro) {
    com.example.data.repository.AccessPolicyManager.isUserVerifiedPro(userProfile) || adState.isUserVerifiedPro
  }
  if (isVerifiedPro) {
    LaunchedEffect(Unit) {
      onDismiss()
      request.onRewardedUnlockGranted?.invoke()
    }
    return
  }
  val rewardedFeatureType = remember(request.featureKey, request.mode) {
    when (request.mode) {
      com.example.ai.UpgradeSheetMode.ROLEPLAY -> com.example.ads.RewardedFeatureType.ROLEPLAY
      com.example.ai.UpgradeSheetMode.LESSONS -> com.example.ads.RewardedFeatureType.LEARN_LESSON
      com.example.ai.UpgradeSheetMode.BOOKS -> {
        if (request.featureKey.contains("books", ignoreCase = true)) {
          com.example.ads.RewardedFeatureType.BONUS_BOOK
        } else {
          com.example.ads.RewardedFeatureType.STORY
        }
      }
      com.example.ai.UpgradeSheetMode.AI_UPGRADE ->
        com.example.ads.RewardedFeatureType.fromGateSource(request.featureKey)
    }
  }
  var localAdError by remember(request) { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF0E1422),
    contentColor = LingoTokens.textPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .testTag("global_upgrade_sheet"),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          LingoGoldLockIcon(size = 18.dp, contentDescription = "LingoSphere Pro")
          Text(
            text = "LingoSphere Pro",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LingoTokens.accentGold
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close",
            tint = LingoTokens.textSecondary
          )
        }
      }

      val dailyLimitReached = !adState.canWatchRewardedAdToday
      val primaryTitle = if (dailyLimitReached) {
        "Daily free limit reached, go Pro for unlimited"
      } else {
        "Try this Premium feature free"
      }
      val primarySubtext = if (dailyLimitReached) {
        "You've used all ${adState.maxDailyRewardedUnlocks} free ad unlocks for today. Upgrade to LingoSphere Pro for unlimited access with zero ads."
      } else {
        "Watch a short ad to unlock it for this session."
      }

      Text(
        text = primaryTitle,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = LingoTokens.textPrimary,
        modifier = Modifier.testTag("upgrade_sheet_title")
      )

      Text(
        text = primarySubtext,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = LingoTokens.textSecondary,
        modifier = Modifier.testTag("upgrade_sheet_subtitle")
      )

      if (request.headline.isNotBlank() && request.headline != primaryTitle) {
        Text(
          text = "${request.headline} · ${request.reason}",
          fontSize = 13.sp,
          lineHeight = 18.sp,
          color = LingoTokens.textSecondary.copy(alpha = 0.85f)
        )
      }

      // Rewarded Ad Load Error + Retry Banner (never unlocks for free on failure)
      val effectiveAdError = localAdError ?: adState.rewardedErrorMessage.takeIf { adState.rewardedLoadFailed }
      if (!effectiveAdError.isNullOrBlank() && !dailyLimitReached) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0x26EF4444),
          border = BorderStroke(1.dp, Color(0x66EF4444)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("rewarded_ad_error_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "No ad available right now, try again in a moment",
              fontSize = 13.sp,
              color = LingoTokens.textPrimary,
              modifier = Modifier.weight(1f)
            )
            TextButton(
              onClick = {
                val ok = com.example.ads.AdService.preloadRewardedAd(userProfile)
                localAdError = if (ok) null else "No ad available right now, try again in a moment"
              },
              modifier = Modifier.testTag("rewarded_ad_retry_btn")
            ) {
              Text(
                text = "Retry",
                color = LingoTokens.brandCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      if (!isSignedIn) {
        LingoPrimaryGradientButton(
          text = "Sign in to continue",
          onClick = onSignInRequested,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("global_upgrade_sign_in_btn")
        )
      } else {
        // Button 1 (Primary): "Watch ad & unlock" (when under the 5/day limit)
        if (!dailyLimitReached) {
          LingoPrimaryGradientButton(
            text = "Watch ad & unlock",
            onClick = {
              localAdError = null
              val launched = com.example.ads.AdService.showRewardedAdForFeatureUnlock(
                userProfile = userProfile,
                featureType = rewardedFeatureType,
                itemId = request.itemId,
                onUnlocked = {
                  onDismiss()
                  request.onRewardedUnlockGranted?.invoke()
                },
                onFailedOrCancelled = { err ->
                  if (!err.isNullOrBlank()) {
                    localAdError = err
                  }
                }
              )
              if (!launched && (com.example.ads.AdService.simulateAdLoadFailure || adState.rewardedLoadFailed)) {
                localAdError = "No ad available right now, try again in a moment"
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("watch_ad_unlock_btn")
          )
        }

        // Button 2: "Go Pro, no ads" (opens the Upgrade to Pro screen)
        Surface(
          onClick = onOpenFullComparison,
          shape = RoundedCornerShape(999.dp),
          color = LingoTokens.bgGlassCard,
          border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.horizontalGradient(listOf(LingoTokens.brandIndigo, LingoTokens.brandCyan))
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("go_pro_no_ads_btn")
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp)
          ) {
            Text(
              text = "Go Pro, no ads",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary
            )
          }
        }

        // Direct Plan Selector & Billing Upgrade Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val yearlySelected = selectedPlanId == com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY
          Surface(
            onClick = { selectedPlanId = com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY },
            shape = RoundedCornerShape(18.dp),
            color = if (yearlySelected) LingoTokens.brandIndigo.copy(alpha = 0.16f) else LingoTokens.bgGlassCard,
            border = BorderStroke(
              width = if (yearlySelected) 1.5.dp else 1.dp,
              color = if (yearlySelected) LingoTokens.brandCyan else LingoTokens.borderSubtle
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 64.dp)
              .testTag("global_upgrade_yearly_option")
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = "Yearly · Save 33%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LingoTokens.brandCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = yearlyPrice,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary,
                maxLines = 1
              )
            }
          }

          val monthlySelected = selectedPlanId == com.example.data.model.SubscriptionPlan.PRODUCT_ID_MONTHLY
          Surface(
            onClick = { selectedPlanId = com.example.data.model.SubscriptionPlan.PRODUCT_ID_MONTHLY },
            shape = RoundedCornerShape(18.dp),
            color = if (monthlySelected) LingoTokens.brandIndigo.copy(alpha = 0.16f) else LingoTokens.bgGlassCard,
            border = BorderStroke(
              width = if (monthlySelected) 1.5.dp else 1.dp,
              color = if (monthlySelected) LingoTokens.brandCyan else LingoTokens.borderSubtle
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 64.dp)
              .testTag("global_upgrade_monthly_option")
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = "Monthly",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LingoTokens.textSecondary,
                maxLines = 1
              )
              Text(
                text = monthlyPrice,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary,
                maxLines = 1
              )
            }
          }
        }

        TextButton(
          onClick = { onSelectPlan(selectedPlanId) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("global_upgrade_yearly_btn")
        ) {
          Text(
            text = if (selectedPlanId == com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY) {
              "Upgrade to Pro · $yearlyPrice"
            } else {
              "Upgrade to Pro · $monthlyPrice"
            },
            color = LingoTokens.brandCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${adState.remainingDailyUnlocks} of ${adState.maxDailyRewardedUnlocks} free trials left today",
          color = LingoTokens.textSecondary,
          fontSize = 12.sp
        )
        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("global_upgrade_not_now_btn")
        ) {
          Text(
            text = "Not now",
            color = LingoTokens.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
