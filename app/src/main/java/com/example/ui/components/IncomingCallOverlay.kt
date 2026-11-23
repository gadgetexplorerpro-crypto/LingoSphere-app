package com.example.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.CallRoom
import com.example.data.model.Language
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.webrtc.CallRingtoneHelper
import kotlinx.coroutines.delay

object IncomingCallNotificationHelper {
  fun showIncomingCallNotification(context: Context, callRoom: CallRoom) {
    com.example.webrtc.NativeCallNotificationManager.showIncomingCallNotification(
      context = context,
      callRoom = callRoom,
      startRingtoneAndVibration = false
    )
  }

  fun cancelIncomingCallNotification(context: Context, roomId: String? = null) {
    com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, roomId)
  }
}

/**
 * Full-Screen Incoming Call Screen with Ringtone, Vibration, Caller Profile Avatar,
 * Language Badges, 30-Second Timeout, and Accept/Decline Actions.
 */
@Composable
fun IncomingCallOverlay(
  callRoom: CallRoom,
  callerAvatarUrl: String = "",
  onRingingStarted: (String) -> Unit = {},
  onAccept: (CallRoom) -> Unit,
  onDecline: () -> Unit,
  onTimeout: () -> Unit = onDecline
) {
  val context = LocalContext.current
  val isVideo = callRoom.callType == "video"
  val targetLang = Language.findByCode(callRoom.targetLanguageCode)
  val callerNativeLang = Language.findByCode(callRoom.callerNativeLang.ifBlank { "en" })
  val callerLearningLang = Language.findByCode(callRoom.callerLearningLang.ifBlank { callRoom.targetLanguageCode })

  val ringtoneHelper = remember(context) { CallRingtoneHelper(context) }
  var remainingSeconds by remember(callRoom.roomId) { mutableIntStateOf(30) }

  DisposableEffect(callRoom.roomId) {
    onRingingStarted(callRoom.roomId)
    CallRingtoneHelper.startRinging(context)
    IncomingCallNotificationHelper.showIncomingCallNotification(context, callRoom)
    onDispose {
      ringtoneHelper.stopAll()
      CallRingtoneHelper.stopRinging(context)
      IncomingCallNotificationHelper.cancelIncomingCallNotification(context, callRoom.roomId)
    }
  }

  // 30-second incoming call timeout -> Missed Call
  LaunchedEffect(callRoom.roomId) {
    while (remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds--
    }
    com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, callRoom.roomId)
    ringtoneHelper.stopAll()
    CallRingtoneHelper.stopRinging(context)
    IncomingCallNotificationHelper.cancelIncomingCallNotification(context, callRoom.roomId)
    onTimeout()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
  val ringScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.24f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "ringScale"
  )

  val callerInitials = remember(callRoom.callerName) {
    callRoom.callerName
      .trim()
      .split(" ")
      .filter { it.isNotEmpty() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "LP" }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("incoming_call_overlay"),
    color = Color(0xFF040A18)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF081A3A),
              Color(0xFF030A18),
              Color(0xFF020610)
            )
          )
        )
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 28.dp)
      ) {
        // Top Security & Caller Info Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(Color(0xFF0B1F44))
              .border(1.dp, Color(0xFF1E428A), RoundedCornerShape(50))
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = LingoCyan,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "End-to-end encrypted WebRTC Call",
              color = LingoCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (isVideo) "Incoming LingoSphere Video Call..." else "Incoming LingoSphere Voice Call...",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = callRoom.callerName.ifBlank { "Language Partner" },
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2248)),
              border = BorderStroke(1.dp, Color(0xFF1E3A8A))
            ) {
              Text(
                text = "🗣️ Native: ${callerNativeLang.flagEmoji} ${callerNativeLang.name}",
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }

            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2248)),
              border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.5f))
            ) {
              Text(
                text = "🎯 Learning: ${callerLearningLang.flagEmoji} ${callerLearningLang.name}",
                color = LingoCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
        }

        // Center Pulsing Caller Profile Avatar (photo when available, initials fallback)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(184.dp)
        ) {
          Box(
            modifier = Modifier
              .size(172.dp)
              .scale(ringScale)
              .clip(CircleShape)
              .background(LingoEmerald.copy(alpha = 0.15f))
          )
          Box(
            modifier = Modifier
              .size(146.dp)
              .scale(ringScale * 0.95f)
              .clip(CircleShape)
              .background(LingoCyan.copy(alpha = 0.25f))
          )

          if (callerAvatarUrl.isNotBlank()) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(3.dp, LingoEmerald, CircleShape)
            ) {
              LingoAvatar(
                name = callRoom.callerName.ifBlank { "Language Partner" },
                userId = callRoom.callerId,
                size = 114.dp,
                customImageUrl = callerAvatarUrl,
                isOnline = true
              )
            }
          } else {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(LingoElectricBlue, Color(0xFF0284C7))
                  )
                )
                .border(3.dp, LingoEmerald, CircleShape)
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = callerInitials,
                  color = Color.White,
                  fontSize = 34.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = targetLang.flagEmoji,
                  fontSize = 18.sp
                )
              }
            }
          }
        }

        // Bottom Controls: Decline (Red) and Accept (Green)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp)
          ) {
            // Decline Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(68.dp)
                  .clip(CircleShape)
                  .background(LingoCoral)
                  .clickable {
                    com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, callRoom.roomId)
                    ringtoneHelper.stopAll()
                    CallRingtoneHelper.stopRinging(context)
                    IncomingCallNotificationHelper.cancelIncomingCallNotification(context, callRoom.roomId)
                    onDecline()
                  }
                  .testTag("decline_call_button")
              ) {
                Icon(
                  imageVector = Icons.Default.CallEnd,
                  contentDescription = "Decline Call",
                  tint = Color.White,
                  modifier = Modifier.size(32.dp)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Decline",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Accept Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(68.dp)
                  .clip(CircleShape)
                  .background(LingoEmerald)
                  .clickable {
                    com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, callRoom.roomId)
                    ringtoneHelper.stopAll()
                    CallRingtoneHelper.stopRinging(context)
                    IncomingCallNotificationHelper.cancelIncomingCallNotification(context, callRoom.roomId)
                    onAccept(callRoom)
                  }
                  .testTag("accept_call_button")
              ) {
                Icon(
                  imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                  contentDescription = "Accept Call",
                  tint = Color.White,
                  modifier = Modifier.size(32.dp)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (isVideo) "Accept Video" else "Accept Voice",
                color = LingoEmerald,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Ringing • Auto-timeout in ${remainingSeconds}s",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
          )
        }
      }
    }
  }
}
