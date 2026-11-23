package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.UserAccountSettings
import com.example.notifications.SmartEnglishContentCatalog
import com.example.notifications.SmartNotificationManager
import com.example.notifications.SmartNotificationScheduler
import java.util.Locale

private val ReminderCardBg = Color(0xFF0B1C3D)
private val ReminderCardElevated = Color(0xFF07132B)
private val ReminderBorder = Color(0xFF1E428A)
private val ReminderCyan = Color(0xFF38BDF8)
private val ReminderElectricBlue = Color(0xFF2563EB)
private val ReminderEmerald = Color(0xFF34D399)
private val ReminderAmber = Color(0xFFFBBF24)
private val ReminderTextSecondary = Color(0xFFCBD5E1)

private data class ReminderTimePreset(
  val label: String,
  val hourOfDay: Int,
  val minute: Int
)

private val DEFAULT_TIME_PRESETS = listOf(
  ReminderTimePreset("8:00 AM", 8, 0),
  ReminderTimePreset("12:30 PM", 12, 30),
  ReminderTimePreset("7:00 PM", 19, 0),
  ReminderTimePreset("9:00 PM", 21, 0)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyStreakReminderCard(
  accountSettings: UserAccountSettings,
  onUpdateAccountSettings: (UserAccountSettings) -> Unit,
  streakDays: Int = 0,
  userName: String = "",
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showTimePickerDialog by remember { mutableStateOf(false) }

  val isReminderEnabled = accountSettings.studyRemindersEnabled && accountSettings.streakAlertsEnabled
  val formattedTime = accountSettings.formattedDailyReminderTime()
  val scheduleStatusLabel = remember(
    isReminderEnabled,
    accountSettings.dailyReminderHour,
    accountSettings.dailyReminderMinute
  ) {
    SmartNotificationScheduler.formatNextReminderScheduleLabel(
      enabled = isReminderEnabled,
      hourOfDay = accountSettings.dailyReminderHour,
      minute = accountSettings.dailyReminderMinute
    )
  }

  val previewEntry = remember(streakDays, userName) {
    SmartEnglishContentCatalog.getTodayStreakReminder(streakDays = streakDays, userName = userName)
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    val updated = accountSettings.copy(
      studyRemindersEnabled = true,
      streakAlertsEnabled = true
    )
    onUpdateAccountSettings(updated)
    SmartNotificationManager.syncSettingsToPreferences(context, updated)
    SmartNotificationScheduler.scheduleUserDailyStreakReminder(
      context = context,
      hourOfDay = updated.dailyReminderHour,
      minute = updated.dailyReminderMinute
    )
    if (!granted) {
      Toast.makeText(
        context,
        "Daily streak reminder saved! Enable system notifications to receive push alerts.",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  val applyReminderSettings: (UserAccountSettings) -> Unit = { updated ->
    onUpdateAccountSettings(updated)
    SmartNotificationManager.syncSettingsToPreferences(context, updated)
    SmartNotificationScheduler.scheduleUserDailyStreakReminder(
      context = context,
      hourOfDay = updated.dailyReminderHour,
      minute = updated.dailyReminderMinute
    )
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = ReminderCardBg),
    border = BorderStroke(1.2.dp, if (isReminderEnabled) ReminderCyan else ReminderBorder),
    modifier = modifier
      .fillMaxWidth()
      .testTag("daily_streak_reminder_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header Row with Toggle Switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = ReminderAmber.copy(alpha = 0.18f),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak Reminder",
                tint = ReminderAmber,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Daily Streak Reminder",
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.5.sp
            )
            Text(
              text = scheduleStatusLabel,
              color = if (isReminderEnabled) ReminderEmerald else ReminderTextSecondary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp,
              modifier = Modifier.testTag("daily_streak_reminder_status_text")
            )
          }
        }

        Switch(
          checked = isReminderEnabled,
          onCheckedChange = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
              ) == PackageManager.PERMISSION_GRANTED
              if (!hasPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@Switch
              }
            }
            applyReminderSettings(
              accountSettings.copy(
                studyRemindersEnabled = enabled,
                streakAlertsEnabled = enabled
              )
            )
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ReminderCyan,
            uncheckedThumbColor = Color(0xFF94A3B8),
            uncheckedTrackColor = ReminderCardElevated
          ),
          modifier = Modifier.testTag("daily_streak_reminder_toggle")
        )
      }

      // Active Time & Custom Time Selector Row
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = ReminderCardElevated,
        border = BorderStroke(1.dp, ReminderBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f, fill = false)
              .padding(end = 8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AccessTime,
              contentDescription = null,
              tint = ReminderCyan,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Reminder Time",
                color = ReminderTextSecondary,
                fontSize = 11.sp
              )
              Text(
                text = formattedTime,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.testTag("daily_streak_reminder_time_badge")
              )
            }
          }

          OutlinedButton(
            onClick = { showTimePickerDialog = true },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ReminderCyan),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("change_reminder_time_btn")
          ) {
            Text(
              text = "Change Time",
              color = ReminderCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Quick Preset Time Chips
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DEFAULT_TIME_PRESETS.forEach { preset ->
          val isSelected = accountSettings.dailyReminderHour == preset.hourOfDay &&
            accountSettings.dailyReminderMinute == preset.minute
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) ReminderElectricBlue.copy(alpha = 0.38f) else ReminderCardElevated,
            border = BorderStroke(
              1.dp,
              if (isSelected) ReminderCyan else ReminderBorder
            ),
            modifier = Modifier
              .clickable {
                applyReminderSettings(
                  accountSettings.copy(
                    studyRemindersEnabled = true,
                    streakAlertsEnabled = true,
                    dailyReminderHour = preset.hourOfDay,
                    dailyReminderMinute = preset.minute
                  )
                )
              }
              .testTag("reminder_preset_${preset.hourOfDay}_${preset.minute}")
          ) {
            Text(
              text = preset.label,
              color = if (isSelected) ReminderCyan else Color.White,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 11.5.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }

      // Friendly Reminder Preview & Test Button
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = ReminderCardElevated.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, ReminderBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = previewEntry.title,
            color = ReminderAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp
          )
          Text(
            text = previewEntry.friendlyMessage,
            color = ReminderTextSecondary,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = {
              val delivered = SmartNotificationManager.showDailyStreakReminderNotification(
                context = context,
                streakDays = streakDays,
                userName = userName,
                uniqueKey = "manual_streak_${System.currentTimeMillis() / 5_000L}",
                bypassCooldown = true
              )
              Toast.makeText(
                context,
                if (delivered) {
                  "Friendly streak reminder sent!"
                } else {
                  "Enable notification permission & Daily Streak Reminder first."
                },
                Toast.LENGTH_SHORT
              ).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ReminderElectricBlue),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("send_streak_reminder_test_btn")
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Send Friendly Reminder Preview",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }

  if (showTimePickerDialog) {
    DailyReminderTimePickerDialog(
      initialHourOfDay = accountSettings.dailyReminderHour,
      initialMinute = accountSettings.dailyReminderMinute,
      onDismiss = { showTimePickerDialog = false },
      onConfirm = { newHour, newMinute ->
        showTimePickerDialog = false
        applyReminderSettings(
          accountSettings.copy(
            studyRemindersEnabled = true,
            streakAlertsEnabled = true,
            dailyReminderHour = newHour,
            dailyReminderMinute = newMinute
          )
        )
        Toast.makeText(
          context,
          "Daily streak reminder set for ${SmartNotificationManager.formatReminderTime(newHour, newMinute)}",
          Toast.LENGTH_SHORT
        ).show()
      }
    )
  }
}

@Composable
fun DailyReminderTimePickerDialog(
  initialHourOfDay: Int,
  initialMinute: Int,
  onDismiss: () -> Unit,
  onConfirm: (hourOfDay: Int, minute: Int) -> Unit
) {
  val safeInitHour = initialHourOfDay.coerceIn(0, 23)
  val safeInitMin = initialMinute.coerceIn(0, 59)

  var isPm by remember { mutableStateOf(safeInitHour >= 12) }
  var hour12 by remember {
    mutableIntStateOf(
      when {
        safeInitHour == 0 -> 12
        safeInitHour > 12 -> safeInitHour - 12
        else -> safeInitHour
      }
    )
  }
  var minute by remember { mutableIntStateOf(safeInitMin) }

  val resolvedHour24 = when {
    !isPm && hour12 == 12 -> 0
    isPm && hour12 < 12 -> hour12 + 12
    else -> hour12
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ReminderCardBg,
    titleContentColor = Color.White,
    textContentColor = ReminderTextSecondary,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.AccessTime,
          contentDescription = null,
          tint = ReminderCyan,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Set Daily Reminder Time",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp
        )
      }
    },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("daily_reminder_time_picker_dialog")
      ) {
        Text(
          text = "Choose when LingoSphere should send a friendly nudge to help keep your learning streak alive.",
          color = ReminderTextSecondary,
          fontSize = 12.5.sp
        )

        // Interactive Hour : Minute + AM/PM Selector
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Hour Column
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { hour12 = if (hour12 >= 12) 1 else hour12 + 1 },
              modifier = Modifier.testTag("reminder_picker_hour_up")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase hour", tint = ReminderCyan)
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ReminderCardElevated,
              border = BorderStroke(1.dp, ReminderCyan)
            ) {
              Text(
                text = String.format(Locale.US, "%02d", hour12),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier
                  .padding(horizontal = 16.dp, vertical = 10.dp)
                  .testTag("reminder_picker_hour_value")
              )
            }
            IconButton(
              onClick = { hour12 = if (hour12 <= 1) 12 else hour12 - 1 },
              modifier = Modifier.testTag("reminder_picker_hour_down")
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease hour", tint = ReminderCyan)
            }
          }

          Text(
            text = ":",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          // Minute Column (steps by 5 minutes for fast selection)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { minute = (minute + 5) % 60 },
              modifier = Modifier.testTag("reminder_picker_min_up")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase minute", tint = ReminderCyan)
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ReminderCardElevated,
              border = BorderStroke(1.dp, ReminderCyan)
            ) {
              Text(
                text = String.format(Locale.US, "%02d", minute),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier
                  .padding(horizontal = 16.dp, vertical = 10.dp)
                  .testTag("reminder_picker_min_value")
              )
            }
            IconButton(
              onClick = { minute = if (minute - 5 < 0) 55 else minute - 5 },
              modifier = Modifier.testTag("reminder_picker_min_down")
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease minute", tint = ReminderCyan)
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          // AM / PM Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { isPm = false },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (!isPm) ReminderCyan else ReminderBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (!isPm) ReminderElectricBlue.copy(alpha = 0.35f) else ReminderCardElevated
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("reminder_picker_am_btn")
            ) {
              Text(
                text = "AM",
                color = if (!isPm) ReminderCyan else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
            OutlinedButton(
              onClick = { isPm = true },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (isPm) ReminderCyan else ReminderBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isPm) ReminderElectricBlue.copy(alpha = 0.35f) else ReminderCardElevated
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("reminder_picker_pm_btn")
            ) {
              Text(
                text = "PM",
                color = if (isPm) ReminderCyan else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }

        Text(
          text = SmartNotificationScheduler.formatNextReminderScheduleLabel(
            enabled = true,
            hourOfDay = resolvedHour24,
            minute = minute
          ),
          color = ReminderEmerald,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(resolvedHour24, minute) },
        colors = ButtonDefaults.buttonColors(containerColor = ReminderCyan),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("save_reminder_time_btn")
      ) {
        Text(
          text = "Save Reminder",
          color = Color(0xFF04101E),
          fontWeight = FontWeight.ExtraBold
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_reminder_time_btn")
      ) {
        Text("Cancel", color = ReminderTextSecondary)
      }
    }
  )
}
