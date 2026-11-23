package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloatingContinueButtonPrefs
import kotlinx.coroutines.launch

/**
 * Movable 56dp round floating "Continue" lesson button with:
 * - 60fps graphicsLayer translation during drag + subtle scale-up and shadow
 * - Smooth spring snap to nearest left or right screen edge on release
 * - Safe-area clamping (>= 8dp from edges) and automatic non-destructive shift around bottom banners/controls
 * - Physical LTR coordinates so RTL languages never mirror saved edge placement
 * - Clear tap vs. drag separation (dragging never triggers a tap on release)
 * - One-time first-use hint: "Hold and drag to move this button"
 */
@Composable
fun FloatingContinueLessonButton(
  prefs: FloatingContinueButtonPrefs,
  lessonTitle: String,
  onResumeLesson: () -> Unit,
  onPositionCommitted: (snapSide: String, verticalFraction: Float) -> Unit,
  onHintDismissed: () -> Unit,
  modifier: Modifier = Modifier,
  topAvoidanceDp: Dp = 8.dp,
  bottomAvoidanceDp: Dp = 8.dp,
  edgeMarginDp: Dp = 8.dp
) {
  val density = LocalDensity.current
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()

  val latestOnResume by rememberUpdatedState(onResumeLesson)
  val latestOnPositionCommitted by rememberUpdatedState(onPositionCommitted)
  val latestOnHintDismissed by rememberUpdatedState(onHintDismissed)
  val latestPrefs by rememberUpdatedState(prefs)

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    BoxWithConstraints(
      modifier = modifier
        .fillMaxSize()
        .testTag("floating_continue_overlay")
    ) {
      val containerWidthPx = with(density) { maxWidth.toPx() }
      val containerHeightPx = with(density) { maxHeight.toPx() }
      val buttonSizeDp = 56.dp
      val buttonSizePx = with(density) { buttonSizeDp.toPx() }
      val edgeMarginPx = with(density) { edgeMarginDp.coerceAtLeast(8.dp).toPx() }
      val topAvoidPx = with(density) { topAvoidanceDp.coerceAtLeast(8.dp).toPx() }
      val bottomAvoidPx = with(density) { bottomAvoidanceDp.coerceAtLeast(8.dp).toPx() }

      val minX = edgeMarginPx
      val maxX = (containerWidthPx - buttonSizePx - edgeMarginPx).coerceAtLeast(minX)
      val minY = topAvoidPx
      // Base vertical span (without temporary banner/bar avoidance) preserves the user's chosen fraction
      val baseMaxY = (containerHeightPx - buttonSizePx - edgeMarginPx).coerceAtLeast(minY)
      // Effective safe maxY auto-shifts the button just above temporary bottom banners/controls
      val effectiveMaxY = (containerHeightPx - buttonSizePx - bottomAvoidPx).coerceIn(minY, baseMaxY)

      val targetX = if (prefs.snapSide.equals("left", ignoreCase = true)) minX else maxX
      val rawSavedY = minY + (baseMaxY - minY) * prefs.verticalFraction.coerceIn(0f, 1f)
      val targetY = rawSavedY.coerceIn(minY, effectiveMaxY)

      val offsetX = remember { Animatable(targetX) }
      val offsetY = remember { Animatable(targetY) }
      var hasInitializedBounds by remember { mutableStateOf(false) }
      var isDragging by remember { mutableStateOf(false) }
      var showHintLocal by remember(prefs.hasSeenDragHint) { mutableStateOf(!prefs.hasSeenDragHint) }

      // Keep position synced cleanly on startup (instant, no jump), bounds resize, reset, or avoidance shifts
      LaunchedEffect(targetX, targetY, containerWidthPx, containerHeightPx) {
        if (containerWidthPx <= 0f || containerHeightPx <= 0f) return@LaunchedEffect
        if (!hasInitializedBounds) {
          offsetX.snapTo(targetX)
          offsetY.snapTo(targetY)
          hasInitializedBounds = true
        } else if (!isDragging) {
          launch {
            offsetX.animateTo(
              targetValue = targetX,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            )
          }
          launch {
            offsetY.animateTo(
              targetValue = targetY,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            )
          }
        }
      }

      val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.10f else 1.0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium
        ),
        label = "floating_continue_scale"
      )

      val buttonCenterX = offsetX.value + buttonSizePx / 2f
      val isOnLeftHalf = buttonCenterX < (containerWidthPx / 2f)

      // One-time first-use tooltip anchored beside the button, clamped cleanly inside 360dp screens
      AnimatedVisibility(
        visible = showHintLocal && !isDragging && containerWidthPx > 0f,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        val tooltipMaxWidthPx = with(density) { 216.dp.toPx() }
        val gapPx = with(density) { 10.dp.toPx() }
        val tooltipX = if (isOnLeftHalf) {
          (offsetX.value + buttonSizePx + gapPx).coerceAtMost(
            (containerWidthPx - tooltipMaxWidthPx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
          )
        } else {
          (offsetX.value - tooltipMaxWidthPx - gapPx).coerceAtLeast(edgeMarginPx)
        }
        val tooltipY = (offsetY.value + with(density) { 6.dp.toPx() }).coerceIn(minY, effectiveMaxY)

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xF0101A2F),
          border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.55f)),
          shadowElevation = 8.dp,
          modifier = Modifier
            .graphicsLayer {
              translationX = tooltipX
              translationY = tooltipY
            }
            .widthIn(max = 216.dp)
            .clickable {
              showHintLocal = false
              latestOnHintDismissed()
            }
            .testTag("floating_continue_hint")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Hold and drag to move this button",
              color = LingoTokens.textPrimary,
              fontSize = 12.sp,
              lineHeight = 16.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Movable 56dp round floating Continue button
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .graphicsLayer {
            translationX = offsetX.value
            translationY = offsetY.value
            scaleX = dragScale
            scaleY = dragScale
          }
          .size(buttonSizeDp)
          .shadow(
            elevation = if (isDragging) 18.dp else 10.dp,
            shape = CircleShape,
            ambientColor = LingoTokens.brandCyan.copy(alpha = if (isDragging) 0.65f else 0.42f),
            spotColor = LingoTokens.brandIndigo.copy(alpha = if (isDragging) 0.72f else 0.48f)
          )
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF06B6D4),
                Color(0xFF3B82F6),
                Color(0xFF6366F1)
              )
            )
          )
          .border(
            width = if (isDragging) 2.dp else 1.25.dp,
            brush = Brush.linearGradient(
              colors = listOf(
                Color.White.copy(alpha = if (isDragging) 0.72f else 0.44f),
                LingoTokens.brandCyan.copy(alpha = 0.6f)
              )
            ),
            shape = CircleShape
          )
          .semantics {
            role = Role.Button
            contentDescription = if (lessonTitle.isNotBlank()) {
              "Continue lesson: $lessonTitle"
            } else {
              "Continue lesson"
            }
          }
          .pointerInput(minX, maxX, minY, effectiveMaxY, baseMaxY) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              var didDrag = false
              val dragStart = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                didDrag = true
                isDragging = true
                if (showHintLocal) {
                  showHintLocal = false
                  latestOnHintDismissed()
                }
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                change.consume()
                val nextX = (offsetX.value + overSlop.x).coerceIn(minX, maxX)
                val nextY = (offsetY.value + overSlop.y).coerceIn(minY, effectiveMaxY)
                scope.launch {
                  offsetX.snapTo(nextX)
                  offsetY.snapTo(nextY)
                }
              }

              if (dragStart != null) {
                drag(dragStart.id) { change ->
                  val delta = change.positionChange()
                  if (delta.x != 0f || delta.y != 0f) {
                    change.consume()
                    val nextX = (offsetX.value + delta.x).coerceIn(minX, maxX)
                    val nextY = (offsetY.value + delta.y).coerceIn(minY, effectiveMaxY)
                    scope.launch {
                      offsetX.snapTo(nextX)
                      offsetY.snapTo(nextY)
                    }
                  }
                }
              }

              if (didDrag) {
                isDragging = false
                val centerX = offsetX.value + buttonSizePx / 2f
                val snapLeft = centerX < (containerWidthPx / 2f)
                val snapSide = if (snapLeft) "left" else "right"
                val snappedX = if (snapLeft) minX else maxX
                val clampedY = offsetY.value.coerceIn(minY, effectiveMaxY)
                val span = (baseMaxY - minY).coerceAtLeast(1f)
                val fraction = ((clampedY - minY) / span).coerceIn(0f, 1f)

                scope.launch {
                  launch {
                    offsetX.animateTo(
                      targetValue = snappedX,
                      animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                      )
                    )
                  }
                  launch {
                    offsetY.animateTo(
                      targetValue = clampedY,
                      animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                      )
                    )
                  }
                }
                latestOnPositionCommitted(snapSide, fraction)
              } else {
                // Clean tap — never fires if user dragged
                if (showHintLocal) {
                  showHintLocal = false
                  latestOnHintDismissed()
                }
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                latestOnResume()
              }
            }
          }
          .testTag("floating_continue_button")
      ) {
        // Subtle inner dark-glass ring glow for premium dark-theme depth
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x24071022))
        ) {
          Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  }
}
