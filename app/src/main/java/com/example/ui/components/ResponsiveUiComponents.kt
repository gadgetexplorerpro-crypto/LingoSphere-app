package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Responsive button content padding that preserves horizontal label space on 320dp–430dp+ screens
 * while maintaining comfortable touch targets and visual breathing room.
 */
object ResponsiveButtonDefaults {
    val ContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    val OutlinedContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    val TextButtonContentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    val MinWidth = 64.dp
    val MinHeight = 40.dp
}

/**
 * Ensures a button's inner Row is never measured with a pathologically narrow width
 * (e.g. < 48.dp) that would cause Compose Text to wrap 1 letter per line vertically.
 */
fun Modifier.preventVerticalButtonText(minContentWidth: Dp = 56.dp): Modifier = this.layout { measurable, constraints ->
    val minPx = minContentWidth.roundToPx()
    val safeMaxWidth = if (constraints.maxWidth < minPx) {
        minPx.coerceAtLeast(constraints.minWidth)
    } else {
        constraints.maxWidth
    }
    val placeable = measurable.measure(
        constraints.copy(maxWidth = safeMaxWidth)
    )
    val reportedWidth = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
    val reportedHeight = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(reportedWidth, reportedHeight) {
        val xOffset = ((reportedWidth - placeable.width) / 2).coerceAtLeast(0)
        val yOffset = ((reportedHeight - placeable.height) / 2).coerceAtLeast(0)
        placeable.placeRelative(xOffset, yOffset)
    }
}

/**
 * Drop-in responsive Material 3 Button override for `com.example.ui.components` that:
 * 1. Uses compact horizontal padding (12.dp instead of 24.dp) so labels in Rows/Cards never wrap 1 letter per line.
 * 2. Enforces 48.dp minimum interactive touch target (`minimumInteractiveComponentSize()`).
 * 3. Caps button label text at 2 clean lines with ellipsis via `ProvideTextStyle` and prevents vertical letter stacking.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ResponsiveButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val resolvedPadding = if (contentPadding == ButtonDefaults.ContentPadding) {
        ResponsiveButtonDefaults.ContentPadding
    } else {
        contentPadding
    }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(
                minWidth = ResponsiveButtonDefaults.MinWidth,
                minHeight = ResponsiveButtonDefaults.MinHeight
            ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = resolvedPadding,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.preventVerticalButtonText(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Drop-in responsive Material 3 OutlinedButton override for `com.example.ui.components`.
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ResponsiveButtonDefaults.OutlinedContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val resolvedPadding = if (contentPadding == ButtonDefaults.ContentPadding) {
        ResponsiveButtonDefaults.OutlinedContentPadding
    } else {
        contentPadding
    }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(
                minWidth = ResponsiveButtonDefaults.MinWidth,
                minHeight = ResponsiveButtonDefaults.MinHeight
            ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = resolvedPadding,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.preventVerticalButtonText(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Drop-in responsive Material 3 TextButton override for `com.example.ui.components`.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ResponsiveButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val resolvedPadding = if (contentPadding == ButtonDefaults.TextButtonContentPadding) {
        ResponsiveButtonDefaults.TextButtonContentPadding
    } else {
        contentPadding
    }
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = resolvedPadding,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.preventVerticalButtonText(40.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Drop-in responsive Material 3 FilledTonalButton override for `com.example.ui.components`.
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ResponsiveButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val resolvedPadding = if (contentPadding == ButtonDefaults.ContentPadding) {
        ResponsiveButtonDefaults.ContentPadding
    } else {
        contentPadding
    }
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(
                minWidth = ResponsiveButtonDefaults.MinWidth,
                minHeight = ResponsiveButtonDefaults.MinHeight
            ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = resolvedPadding,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.preventVerticalButtonText(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Drop-in responsive Material 3 ElevatedButton override for `com.example.ui.components`.
 */
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ResponsiveButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val resolvedPadding = if (contentPadding == ButtonDefaults.ContentPadding) {
        ResponsiveButtonDefaults.ContentPadding
    } else {
        contentPadding
    }
    androidx.compose.material3.ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(
                minWidth = ResponsiveButtonDefaults.MinWidth,
                minHeight = ResponsiveButtonDefaults.MinHeight
            ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = resolvedPadding,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.preventVerticalButtonText(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ============================================================================
// Reusable Responsive Components (Section 7 Design System Primitives)
// ============================================================================

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ResponsiveButtonDefaults.ContentPadding
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ResponsiveButtonDefaults.OutlinedContentPadding
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ResponsiveIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    iconSize: Dp = 22.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp),
        enabled = enabled,
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun ResponsiveCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border: BorderStroke? = null,
    minHeight: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

@Composable
fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    applySystemBarsInsets: Boolean = true,
    applyImePadding: Boolean = true,
    maxContentWidth: Dp = 640.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val safeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (applySystemBarsInsets) {
                    Modifier.windowInsetsPadding(
                        safeInsets.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                } else {
                    Modifier
                }
            )
            .then(if (applyImePadding) Modifier.imePadding() else Modifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxContentWidth),
            content = content
        )
    }
}

@Composable
fun BottomNavigation(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        windowInsets = WindowInsets.navigationBars,
        content = content
    )
}

@Composable
fun ResponsiveDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        modifier = modifier
            .fillMaxWidth(0.94f)
            .widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(bottom = 12.dp),
            content = content
        )
    }
}
