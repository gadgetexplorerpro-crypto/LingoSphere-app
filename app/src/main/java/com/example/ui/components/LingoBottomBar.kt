package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LingoThemeController

enum class LingoScreen(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String
) {
  HOME("Home", Icons.Outlined.Home, Icons.Outlined.Home, "nav_home"),
  LEARN("Learn", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_learn"),
  SPEAK("Speak", Icons.Outlined.RecordVoiceOver, Icons.Outlined.RecordVoiceOver, "nav_speak"),
  COMMUNITY("Connect", Icons.Outlined.People, Icons.Outlined.People, "nav_community"),
  PRACTICE("Games", Icons.Outlined.SportsEsports, Icons.Outlined.SportsEsports, "nav_practice"),
  VIDEOS("Videos", Icons.Outlined.PlayCircleOutline, Icons.Outlined.PlayCircleOutline, "nav_videos"),
  EXPLORE("Passport", Icons.Outlined.Explore, Icons.Outlined.Explore, "nav_explore"),
  PROFILE("Profile", Icons.Outlined.Person, Icons.Outlined.Person, "nav_profile"),
  ADMIN("Admin", Icons.Outlined.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin")
}

/**
 * Floating glass pill bottom navigation bar:
 * - 16dp horizontal margins, pill shape, translucent glass surface with 1px white 10% inner highlight
 * - Consistent outline icon set + 12sp readable labels
 * - Brand gradient (#6C7BFF -> #2DD4FF) indicator dot under the active tab
 * - Spring tap motion + haptic feedback + safe navigationBarsPadding()
 */
@Composable
fun LingoBottomBar(
  currentScreen: LingoScreen,
  onScreenSelected: (LingoScreen) -> Unit
) {
  val items = remember {
    listOf(
      LingoScreen.HOME,
      LingoScreen.LEARN,
      LingoScreen.SPEAK,
      LingoScreen.COMMUNITY,
      LingoScreen.PROFILE
    )
  }

  val isDark = LingoThemeController.isDarkActive
  val haptic = LocalHapticFeedback.current
  val glassBg = if (isDark) Color(0xEB0E1525) else Color(0xF5FFFFFF)
  val glassHighlight = if (isDark) Color.White.copy(alpha = 0.11f) else Color(0xFFE2E8F0)
  val activeColor = if (isDark) Color.White else Color(0xFF0B0F1A)
  val inactiveColor = LingoTokens.textSecondary

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = glassBg,
      shadowElevation = if (isDark) 16.dp else 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, glassHighlight, RoundedCornerShape(30.dp))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        items.forEach { screen ->
          val selected = currentScreen == screen
          val interactionSource = remember { MutableInteractionSource() }
          val isPressed by interactionSource.collectIsPressedAsState()
          val scale by animateFloatAsState(
            targetValue = when {
              isPressed -> 0.97f
              selected -> 1.02f
              else -> 1f
            },
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessMedium
            ),
            label = "nav_scale_${screen.name}"
          )

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 54.dp)
              .scale(scale)
              .clip(RoundedCornerShape(22.dp))
              .then(
                if (selected) {
                  Modifier
                    .background(
                      brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                          LingoTokens.brandIndigo.copy(alpha = 0.24f),
                          LingoTokens.brandCyan.copy(alpha = 0.20f)
                        )
                      )
                    )
                    .border(
                      width = 1.dp,
                      color = LingoTokens.brandCyan.copy(alpha = 0.32f),
                      shape = RoundedCornerShape(22.dp)
                    )
                } else {
                  Modifier
                }
              )
              .clickable(
                interactionSource = interactionSource,
                indication = null
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onScreenSelected(screen)
              }
              .padding(vertical = 6.dp, horizontal = 4.dp)
              .testTag(screen.tag)
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = screen.label,
                tint = if (selected) LingoTokens.brandCyan else inactiveColor,
                modifier = Modifier.size(21.dp)
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = screen.label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else inactiveColor
              )
            }
          }
        }
      }
    }
  }
}
