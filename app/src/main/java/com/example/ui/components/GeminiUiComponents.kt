package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiQuotaUiState

/**
 * Compact dark pill showing remaining daily Gemini AI calls for Free users
 * (or gold Pro Unlimited indicator for Pro users). Tapping opens the Pro paywall.
 */
@Composable
fun GeminiDailyQuotaPill(
  quotaState: GeminiQuotaUiState,
  onOpenPaywall: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isLow = !quotaState.isProUser
  val accentColor = when {
    !quotaState.isOnline -> Color(0xFFEF4444)
    quotaState.isProUser -> LingoTokens.accentGold
    else -> LingoTokens.brandCyan
  }
  val label = when {
    !quotaState.isOnline -> "Offline · Local mode"
    quotaState.isProUser -> "Pro · Unlimited AI"
    else -> "Upgrade to unlock AI"
  }

  Surface(
    onClick = onOpenPaywall,
    shape = RoundedCornerShape(999.dp),
    color = LingoTokens.bgElevated,
    border = BorderStroke(1.dp, if (isLow || quotaState.isProUser) accentColor.copy(alpha = 0.4f) else LingoTokens.innerHighlight),
    modifier = modifier.testTag("gemini_daily_quota_pill")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = when {
          !quotaState.isOnline -> Icons.Outlined.CloudOff
          quotaState.isProUser -> Icons.Outlined.WorkspacePremium
          else -> Icons.Outlined.AutoAwesome
        },
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = label,
        color = if (quotaState.isProUser || isLow) accentColor else LingoTokens.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Small post-turn speaking feedback card shown after every voice/speaking turn:
 * - Pronunciation score (0..100)
 * - 1 concise grammar fix (auto-saved to Mistake Recovery)
 * - 1 more natural native phrase with tap-to-listen audio
 */
@Composable
fun GeminiPostTurnFeedbackCard(
  pronunciationScore: Int?,
  grammarFix: String?,
  naturalPhrase: String?,
  onListenNaturalPhrase: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  if (pronunciationScore == null && grammarFix.isNullOrBlank() && naturalPhrase.isNullOrBlank()) return

  val scoreVal = (pronunciationScore ?: 88).coerceIn(0, 100)
  val scoreColor = when {
    scoreVal >= 85 -> LingoTokens.statusOnline
    scoreVal >= 70 -> LingoTokens.accentGold
    else -> Color(0xFFEF4444)
  }

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = LingoTokens.bgSubtle,
    border = BorderStroke(1.dp, LingoTokens.innerHighlight),
    modifier = modifier
      .fillMaxWidth()
      .testTag("post_turn_feedback_card")
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Turn coaching",
          color = LingoTokens.brandCyan,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(scoreColor.copy(alpha = 0.16f))
            .border(1.dp, scoreColor.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
          Text(
            text = "Pronunciation $scoreVal/100",
            color = scoreColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      if (!grammarFix.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Grammar fix · Saved to Mistake Recovery",
            color = LingoTokens.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = grammarFix,
            color = LingoTokens.textPrimary,
            fontSize = 15.sp,
            lineHeight = 22.sp
          )
        }
      }

      if (!naturalPhrase.isNullOrBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LingoTokens.bgElevated)
            .clickable(enabled = onListenNaturalPhrase != null) {
              onListenNaturalPhrase?.invoke(naturalPhrase)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "More natural phrase",
              color = LingoTokens.brandCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = naturalPhrase,
              color = LingoTokens.textPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium,
              lineHeight = 22.sp
            )
          }
          if (onListenNaturalPhrase != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LingoTokens.brandCyan.copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = "Listen to natural phrase",
                tint = LingoTokens.brandCyan,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Unified dark-theme friendly error / offline / upgrade card with one-tap Retry and Upgrade actions.
 */
@Composable
fun GeminiErrorOrOfflineCard(
  message: String,
  isOffline: Boolean = false,
  isQuotaExceeded: Boolean = false,
  onRetry: (() -> Unit)? = null,
  onOpenPaywall: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = LingoTokens.bgElevated,
    border = BorderStroke(
      1.dp,
      if (isQuotaExceeded) LingoTokens.accentGold.copy(alpha = 0.4f) else LingoTokens.innerHighlight
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("gemini_error_retry_card")
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = when {
            isOffline -> Icons.Outlined.CloudOff
            isQuotaExceeded -> Icons.Outlined.WorkspacePremium
            else -> Icons.Outlined.Refresh
          },
          contentDescription = null,
          tint = if (isQuotaExceeded) LingoTokens.accentGold else LingoTokens.brandCyan,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = when {
            isOffline -> "You're offline"
            isQuotaExceeded -> "LingoSphere Pro feature"
            else -> "Temporarily unavailable"
          },
          color = LingoTokens.textPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
      }

      Text(
        text = com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
          raw = message,
          fallback = if (isOffline) {
            "You're offline. Check your connection and try again."
          } else {
            "Taking a moment — let's try that again."
          }
        ),
        color = LingoTokens.textSecondary,
        fontSize = 15.sp,
        lineHeight = 22.sp
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (isQuotaExceeded && onOpenPaywall != null) {
          LingoPrimaryGradientButton(
            text = "Unlock AI learning",
            onClick = onOpenPaywall,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("gemini_quota_upgrade_btn")
          )
        }
        if (onRetry != null && !isQuotaExceeded) {
          LingoPrimaryGradientButton(
            text = "Retry",
            onClick = onRetry,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("gemini_error_retry_btn")
          )
        }
      }
    }
  }
}
