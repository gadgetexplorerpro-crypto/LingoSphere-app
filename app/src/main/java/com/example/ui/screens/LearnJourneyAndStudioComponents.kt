package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiTutorService
import com.example.data.model.CurriculumLesson
import com.example.data.model.Language
import com.example.data.model.RoleplayEvaluationReport
import com.example.data.repository.InProgressLessonState
import com.example.speech.SpeechManager
import com.example.ui.components.LingoTokens
import com.example.ui.theme.LingoThemeController
import kotlinx.coroutines.launch

/**
 * Refined Premium Learn Page Palette:
 * Light mode: Premium white (#FFFFFF) and soft ivory (#FAF9F5) backgrounds
 * Dark mode: Rich dark navy (#070C1A) and elevated slate (#0F172A)
 * Royal Blue (#2563EB / #3B82F6) & Elegant Violet (#7C3AED / #8B5CF6) accents
 * Soft Green (#10B981) for completed lessons
 * Warm Gold (#F59E0B / #FBBF24) for achievements and rewards
 * Soft Gray (#94A3B8) for locked content
 */
internal val PremiumLearnBg: Color
  get() = LingoTokens.bgCanvas

internal val PremiumLearnSurface: Color
  get() = LingoTokens.bgElevated

internal val PremiumLearnElevated: Color
  get() = LingoTokens.bgSubtle

internal val PremiumLearnBorder: Color
  get() = LingoTokens.borderSubtle

internal val PremiumRoyalBlue: Color
  get() = LingoTokens.accentBlue

internal val PremiumViolet: Color
  get() = LingoTokens.accentIndigo

internal val PremiumSoftGreen: Color
  get() = LingoTokens.accentEmerald

internal val PremiumWarmGold: Color
  get() = LingoTokens.accentAmber

internal val PremiumLockedGray: Color
  get() = LingoTokens.textMuted

internal val PremiumTextPrimary: Color
  get() = LingoTokens.textPrimary

internal val PremiumTextSecondary: Color
  get() = LingoTokens.textSecondary

enum class JourneyNodeState(val label: String) {
  COMPLETED("Completed"),
  CURRENT("Current Lesson"),
  LOCKED("Upcoming"),
  REVIEW("Review Ready"),
  ASSESSMENT("Unit Assessment")
}

data class StudioRoleplayScenario(
  val id: String,
  val title: String,
  val category: String,
  val objective: String,
  val openingPrompt: String,
  val suggestedCefr: String
)

val STUDIO_ROLEPLAY_SCENARIOS = listOf(
  StudioRoleplayScenario(
    id = "shopping_clothes",
    title = "Shopping for Clothes",
    category = "Everyday Life",
    objective = "Ask for a different size, inquire about fabric or color options, and ask about the return policy.",
    openingPrompt = "Welcome to our boutique! Are you looking for a specific item or size today?",
    suggestedCefr = "A2"
  ),
  StudioRoleplayScenario(
    id = "ordering_restaurant",
    title = "Ordering at a Restaurant",
    category = "Food & Dining",
    objective = "Ask about the daily special, mention a dietary preference, and order a main course and drink.",
    openingPrompt = "Good evening! Here is the menu. Would you like to hear today's chef specials before ordering?",
    suggestedCefr = "A1"
  ),
  StudioRoleplayScenario(
    id = "airport_checkin",
    title = "Airport Check-in",
    category = "Travel",
    objective = "Check in your luggage, request an aisle or window seat, and ask which boarding gate to go to.",
    openingPrompt = "Hello! May I see your passport and booking confirmation? How many bags are you checking today?",
    suggestedCefr = "A2"
  ),
  StudioRoleplayScenario(
    id = "hotel_reception",
    title = "Hotel Reception",
    category = "Travel",
    objective = "Confirm your reservation, ask about breakfast hours and Wi-Fi, and request a quiet room.",
    openingPrompt = "Welcome to the Grand Hotel! Do you have a reservation under your name for tonight?",
    suggestedCefr = "A2"
  ),
  StudioRoleplayScenario(
    id = "job_interview",
    title = "Job Interview",
    category = "Career & Work",
    objective = "Introduce your professional background, describe a key strength, and ask a thoughtful question about the team.",
    openingPrompt = "Thank you for joining us today. To begin, could you tell me a bit about your background and what excites you about this role?",
    suggestedCefr = "B2"
  ),
  StudioRoleplayScenario(
    id = "visiting_doctor",
    title = "Visiting a Doctor",
    category = "Health & Emergency",
    objective = "Describe your symptoms clearly, state how long you have felt unwell, and ask about recovery instructions.",
    openingPrompt = "Hello, please have a seat. What brings you into the clinic today, and when did the symptoms start?",
    suggestedCefr = "B1"
  ),
  StudioRoleplayScenario(
    id = "asking_directions",
    title = "Asking for Directions",
    category = "Everyday Navigation",
    objective = "Politely ask a local how to reach the central train station and confirm how many minutes it takes on foot.",
    openingPrompt = "Excuse me! You look like you're looking for a landmark—can I help you find your way around the neighborhood?",
    suggestedCefr = "A1"
  ),
  StudioRoleplayScenario(
    id = "meeting_new_people",
    title = "Meeting New People",
    category = "Social & Culture",
    objective = "Introduce yourself, share your hobbies or why you are learning the language, and ask about their favorite local spot.",
    openingPrompt = "Hi there! I don't think we've met yet—I'm Alex. What brings you to the event today?",
    suggestedCefr = "A1"
  ),
  StudioRoleplayScenario(
    id = "workplace_conversations",
    title = "Workplace Conversations",
    category = "Career & Work",
    objective = "Give a brief project update, align on next week's deadline, and offer help to a teammate.",
    openingPrompt = "Good morning! Before our standup starts, how is the timeline looking for your current milestone?",
    suggestedCefr = "B2"
  ),
  StudioRoleplayScenario(
    id = "university_conversations",
    title = "University Conversations",
    category = "Academic",
    objective = "Discuss a seminar reading with a classmate, propose a study group topic, and agree on a meeting time.",
    openingPrompt = "Hey! What did you think of today's lecture? Do you want to team up for the upcoming seminar presentation?",
    suggestedCefr = "B1"
  ),
  StudioRoleplayScenario(
    id = "phone_conversation",
    title = "Phone Conversation",
    category = "Everyday Life",
    objective = "Introduce yourself on a phone call, confirm an appointment time, and clarify contact details clearly.",
    openingPrompt = "Hello, thank you for calling customer support! How may I assist you with your booking today?",
    suggestedCefr = "B1"
  )
)

@Composable
fun WindingJourneyUnitHeader(
  unitTitle: String,
  unitIndex: Int,
  completedCount: Int,
  totalCount: Int,
  onScrollToCurrentLesson: () -> Unit,
  showBackToCurrentButton: Boolean
) {
  val unitProgress = (completedCount.toFloat() / totalCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
  val isAllDone = completedCount == totalCount && totalCount > 0

  com.example.ui.components.LingoCleanCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp, bottom = 2.dp),
    contentPadding = 16.dp
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            color = LingoTokens.bgSubtle,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle)
          ) {
            Text(
              text = "UNIT ${unitIndex + 1}",
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold,
              color = LingoTokens.brandCyan,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = unitTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LingoTokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (showBackToCurrentButton) {
            Surface(
              color = LingoTokens.bgSubtle,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier
                .clickable(onClick = onScrollToCurrentLesson)
                .testTag("scroll_to_current_lesson_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.MyLocation,
                  contentDescription = "Back to Current Lesson",
                  tint = LingoTokens.brandCyan,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Current",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.brandCyan
                )
              }
            }
          }

          Text(
            text = "$completedCount/$totalCount",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAllDone) LingoTokens.brandCyan else LingoTokens.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      com.example.ui.components.LingoGradientProgressBar(
        progress = unitProgress,
        height = 5.dp
      )
    }
  }
}

@Composable
fun WindingJourneyLessonRow(
  lesson: CurriculumLesson,
  indexInUnit: Int,
  totalInUnit: Int,
  nodeState: JourneyNodeState,
  isCompleted: Boolean,
  inProgressState: InProgressLessonState?,
  isDownloaded: Boolean,
  isVerifiedPro: Boolean = true,
  onStart: () -> Unit,
  onToggleOffline: () -> Unit,
  onInspectLesson: (CurriculumLesson, JourneyNodeState) -> Unit
) {
  val isCurrent = nodeState == JourneyNodeState.CURRENT
  val isLockedState = nodeState == JourneyNodeState.LOCKED || !isVerifiedPro
  val progressFraction = when {
    isCompleted -> 1f
    inProgressState != null -> (inProgressState.currentExerciseIndex.toFloat() / inProgressState.totalExercises.coerceAtLeast(1).toFloat()).coerceIn(0.1f, 0.95f)
    else -> 0f
  }

  val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "current_node_pulse")
  val pulseScale by infinite.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.08f,
    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
      animation = androidx.compose.animation.core.tween(durationMillis = 1200),
      repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
    ),
    label = "node_pulse_scale"
  )

  // High-contrast dark premium palette (>= 4.5:1 text contrast on dark navy glass)
  val cardNavyBase = Color(0xFF0F172A)
  val cardNavyLocked = Color(0xFF0D1425)
  val cardNavyCompleted = Color(0xFF0F1B2D)
  val titleNearWhite = Color(0xFFF8FAFC)
  val subtitleLightGreyBlue = Color(0xFFB8C5E0)
  val xpAccentAmber = Color(0xFFFBBF24)
  val completedGreen = Color(0xFF22C55E)
  val cloudSoftAccent = Color(0xFF7DD3FC)
  val lockedBorderDim = Color(0xFF23304A)
  val lockedCircleBg = Color(0xFF18223A)

  val pathColor = when {
    isCompleted -> completedGreen.copy(alpha = 0.65f)
    isCurrent -> LingoTokens.brandCyan.copy(alpha = 0.65f)
    else -> Color(0xFF253352)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("lesson_card_${lesson.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Vertical roadmap connector + node column (dark premium theme)
    Box(
      modifier = Modifier
        .width(44.dp)
        .height(if (isCurrent) 128.dp else 92.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val cx = size.width / 2f
        if (indexInUnit > 0) {
          drawLine(
            color = pathColor,
            start = Offset(cx, 0f),
            end = Offset(cx, size.height * 0.34f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
        if (indexInUnit < totalInUnit - 1) {
          drawLine(
            color = pathColor,
            start = Offset(cx, size.height * 0.66f),
            end = Offset(cx, size.height),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }

      // Circular roadmap node
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(if (isCurrent) 40.dp else 36.dp)
          .clip(CircleShape)
          .then(
            when {
              isCompleted -> Modifier
                .background(Color(0xFF102A22))
                .border(1.5.dp, completedGreen, CircleShape)
              isCurrent -> Modifier
                .background(LingoTokens.brandGradient)
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
              else -> Modifier
                .background(lockedCircleBg)
                .border(1.dp, lockedBorderDim, CircleShape)
            }
          )
          .clickable { onInspectLesson(lesson, nodeState) }
          .testTag("lesson_journey_node_${lesson.id}")
      ) {
        when (nodeState) {
          JourneyNodeState.COMPLETED -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Completed lesson",
            tint = completedGreen,
            modifier = Modifier.size(20.dp)
          )
          JourneyNodeState.CURRENT -> Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Current lesson",
            tint = LingoTokens.onGradientInk,
            modifier = Modifier.size((22f * pulseScale).dp)
          )
          JourneyNodeState.ASSESSMENT -> Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Assessment lesson",
            tint = xpAccentAmber,
            modifier = Modifier.size(18.dp)
          )
          JourneyNodeState.REVIEW -> Icon(
            imageVector = Icons.Default.Replay,
            contentDescription = "Review lesson",
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(18.dp)
          )
          JourneyNodeState.LOCKED -> Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked lesson",
            tint = xpAccentAmber,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    // Lesson Card — dark navy glass across ALL states (zero light grey/white backgrounds)
    if (isCurrent) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(22.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF131F3A),
                cardNavyBase
              )
            )
          )
          .border(
            width = 1.5.dp,
            brush = LingoTokens.brandGradient,
            shape = RoundedCornerShape(22.dp)
          )
          .clickable(onClick = onStart)
          .padding(18.dp)
          .testTag("lesson_current_card_${lesson.id}")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = "CURRENT LESSON · ${lesson.cefrLevel}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
                color = LingoTokens.brandCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (!isVerifiedPro) {
                Surface(
                  color = Color(0xFF2B2110),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, xpAccentAmber.copy(alpha = 0.55f)),
                  modifier = Modifier.testTag("lesson_locked_badge_${lesson.id}")
                ) {
                  Text(
                    text = "Pro",
                    color = xpAccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }
            IconButton(
              onClick = onToggleOffline,
              modifier = Modifier
                .size(44.dp)
                .testTag("offline_toggle_${lesson.id}")
            ) {
              Icon(
                imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                contentDescription = if (isDownloaded) "Saved offline" else "Download offline",
                tint = if (isDownloaded) LingoTokens.brandCyan else cloudSoftAccent,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Text(
            text = lesson.title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = titleNearWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("lesson_title_${lesson.id}")
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "${lesson.category} · ${lesson.estimatedMinutes}m",
              style = MaterialTheme.typography.bodyMedium,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Medium,
              color = subtitleLightGreyBlue,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            Surface(
              color = Color(0xFF281F0E),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, xpAccentAmber.copy(alpha = 0.45f))
            ) {
              Text(
                text = "+${lesson.xpReward} XP",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = xpAccentAmber,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(LingoTokens.brandGradient)
              .clickable(onClick = onStart)
              .heightIn(min = 44.dp)
              .padding(horizontal = 18.dp, vertical = 10.dp)
              .testTag("lesson_action_btn_${lesson.id}")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (!isVerifiedPro) Icons.Default.Lock else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = LingoTokens.onGradientInk,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when {
                  !isVerifiedPro -> "Unlock Pro"
                  inProgressState != null -> "Continue"
                  else -> "Start"
                },
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.onGradientInk
              )
            }
          }
        }
      }
    } else {
      val cardBg = if (isCompleted) cardNavyCompleted else cardNavyLocked
      val cardBorder = if (isCompleted) completedGreen.copy(alpha = 0.45f) else lockedBorderDim
      com.example.ui.components.LingoCleanCard(
        onClick = onStart,
        containerColor = cardBg,
        borderColor = cardBorder,
        shape = RoundedCornerShape(20.dp),
        contentPadding = 16.dp,
        modifier = Modifier
          .weight(1f)
          .testTag(if (isCompleted) "lesson_completed_card_${lesson.id}" else "lesson_locked_card_${lesson.id}")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = lesson.title,
              style = MaterialTheme.typography.titleSmall,
              fontSize = 15.5.sp,
              fontWeight = FontWeight.Bold,
              color = titleNearWhite,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("lesson_title_${lesson.id}")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "${lesson.category} · ${lesson.estimatedMinutes}m",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = subtitleLightGreyBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              if (isCompleted) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = completedGreen,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "Completed",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = completedGreen,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              } else {
                Surface(
                  color = Color(0xFF281F0E),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, xpAccentAmber.copy(alpha = 0.45f))
                ) {
                  Text(
                    text = "+${lesson.xpReward} XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = xpAccentAmber,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          if (isLockedState && !isCompleted) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (!isVerifiedPro) {
                Surface(
                  color = Color(0xFF2B2110),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, xpAccentAmber.copy(alpha = 0.55f)),
                  modifier = Modifier.testTag("lesson_locked_badge_${lesson.id}")
                ) {
                  Text(
                    text = "Pro",
                    color = xpAccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                  )
                }
              }
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(lockedCircleBg)
                  .border(1.dp, lockedBorderDim, CircleShape)
                  .testTag("lesson_lock_icon_${lesson.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Locked lesson",
                  tint = xpAccentAmber,
                  modifier = Modifier.size(15.dp)
                )
              }
            }
          }

          IconButton(
            onClick = onToggleOffline,
            modifier = Modifier
              .size(44.dp)
              .testTag("offline_toggle_${lesson.id}")
          ) {
            Icon(
              imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
              contentDescription = if (isDownloaded) "Saved offline" else "Download lesson offline",
              tint = if (isDownloaded) LingoTokens.brandCyan else cloudSoftAccent,
              modifier = Modifier.size(20.dp)
            )
          }

          if (isCompleted) {
            Surface(
              onClick = onStart,
              color = Color(0xFF17233A),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, completedGreen.copy(alpha = 0.45f)),
              modifier = Modifier
                .heightIn(min = 44.dp)
                .testTag("lesson_action_btn_${lesson.id}")
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
              ) {
                Text(
                  text = "Review",
                  style = MaterialTheme.typography.labelMedium,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = titleNearWhite
                )
              }
            }
          }
        }

        if (isCompleted) {
          Spacer(modifier = Modifier.height(10.dp))
          com.example.ui.components.LingoGradientProgressBar(
            progress = progressFraction,
            height = 4.dp
          )
        }
      }
    }
  }
}

@Composable
fun LessonInspectDialog(
  lesson: CurriculumLesson,
  nodeState: JourneyNodeState,
  isCompleted: Boolean,
  onDismiss: () -> Unit,
  onStartLesson: () -> Unit,
  onOpenLiveTutor: () -> Unit = {}
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag("lesson_inspect_dialog"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PremiumLearnSurface),
    border = BorderStroke(1.5.dp, PremiumRoyalBlue.copy(alpha = 0.65f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Surface(
            color = PremiumRoyalBlue.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "CEFR ${lesson.cefrLevel}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = PremiumRoyalBlue,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Surface(
            color = PremiumViolet.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = nodeState.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = PremiumViolet,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Text(
            text = "• ~${lesson.estimatedMinutes} min",
            style = MaterialTheme.typography.labelSmall,
            color = PremiumTextSecondary,
            fontWeight = FontWeight.SemiBold
          )
        }
        TextButton(onClick = onDismiss, modifier = Modifier.testTag("inspect_dialog_back_button")) {
          Text("Back", color = PremiumTextSecondary, fontWeight = FontWeight.Bold)
        }
      }

      Text(
        text = lesson.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = PremiumTextPrimary
      )

      Text(
        text = "Objective: ${lesson.description}",
        style = MaterialTheme.typography.bodyMedium,
        color = PremiumTextSecondary
      )

      Surface(
        color = PremiumLearnElevated,
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Unit: ${lesson.unitTitle}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PremiumTextPrimary
          )
          Text(
            text = "Skills Covered: ${lesson.category}, Pronunciation, Active Recall & Speaking • +${lesson.xpReward} XP",
            style = MaterialTheme.typography.labelSmall,
            color = PremiumTextSecondary
          )
          if (lesson.learningOutcomes.isNotEmpty()) {
            Text(
              text = "Expected Outcomes: ${lesson.learningOutcomes.joinToString(" • ")}",
              style = MaterialTheme.typography.labelSmall,
              color = PremiumRoyalBlue,
              fontWeight = FontWeight.SemiBold
            )
          }
          if (nodeState == JourneyNodeState.LOCKED) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Prerequisite: Recommended after completing your current ${lesson.cefrLevel} module, or tap Jump In Early to preview now.",
              style = MaterialTheme.typography.labelSmall,
              color = PremiumWarmGold,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenLiveTutor()
          },
          border = BorderStroke(1.5.dp, PremiumRoyalBlue),
          modifier = Modifier
            .weight(1f)
            .testTag("inspect_dialog_live_tutor_button")
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PremiumRoyalBlue, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Meet Live AI Tutor", color = PremiumRoyalBlue, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }

        Button(
          onClick = {
            onDismiss()
            onStartLesson()
          },
          colors = ButtonDefaults.buttonColors(containerColor = PremiumRoyalBlue, contentColor = Color.White),
          modifier = Modifier
            .weight(1f)
            .testTag("inspect_dialog_start_button")
        ) {
          Text(
            text = when {
              isCompleted -> "Replay Lesson"
              nodeState == JourneyNodeState.LOCKED -> "Jump In Early"
              else -> "Start Lesson"
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

@Composable
fun AiLearningStudioSection(
  targetLanguageName: String,
  isVerifiedPro: Boolean = true,
  onOpenSpeakingTutor: () -> Unit,
  onOpenRoleplayStudio: () -> Unit,
  onOpenReadingStories: () -> Unit,
  onOpenListeningStudio: () -> Unit,
  onOpenVocabularyStudio: () -> Unit = {},
  onOpenWritingStudio: () -> Unit = {}
) {
  com.example.ui.components.LingoCleanCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("ai_learning_studio_section"),
    contentPadding = 18.dp
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Practice Studios",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LingoTokens.textPrimary
          )
          Text(
            text = "Focused $targetLanguageName skill labs",
            fontSize = 14.sp,
            color = LingoTokens.textSecondary
          )
        }
        Surface(
          color = LingoTokens.bgSubtle,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle)
        ) {
          Text(
            text = "6 Studios",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LingoTokens.brandCyan,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StudioFeatureTile(
          modifier = Modifier.weight(1f),
          title = "Reading",
          subtitle = "Stories & books",
          icon = Icons.AutoMirrored.Filled.MenuBook,
          accentColor = LingoTokens.brandCyan,
          testTag = "studio_card_reading_stories",
          onClick = onOpenReadingStories
        )
        StudioFeatureTile(
          modifier = Modifier.weight(1f),
          title = "Listening",
          subtitle = "Audio & scripts",
          icon = Icons.Default.Headphones,
          accentColor = LingoTokens.brandCyan,
          testTag = "studio_card_listening_studio",
          onClick = onOpenListeningStudio
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StudioFeatureTile(
          modifier = Modifier.weight(1f),
          title = "Speaking",
          subtitle = "Fluency coach",
          icon = Icons.Default.RecordVoiceOver,
          accentColor = LingoTokens.brandCyan,
          showProLock = !isVerifiedPro,
          testTag = "studio_card_speaking_tutor",
          onClick = onOpenSpeakingTutor
        )
        StudioFeatureTile(
          modifier = Modifier.weight(1f),
          title = "Roleplay",
          subtitle = "Real scenarios",
          icon = Icons.Default.Psychology,
          accentColor = LingoTokens.brandIndigo,
          showProLock = !isVerifiedPro,
          testTag = "studio_card_roleplay",
          onClick = onOpenRoleplayStudio
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StudioFeatureTile(
          modifier = Modifier.weight(1f),
          title = "Vocabulary",
          subtitle = "SRS flashcards",
          icon = Icons.Default.School,
          accentColor = LingoTokens.brandCyan,
          testTag = "studio_card_vocabulary",
          onClick = onOpenVocabularyStudio
        )
        StudioFeatureTile(
          modifier = Modifier.weight(1f),
          title = "Writing",
          subtitle = "AI corrections",
          icon = Icons.Default.AutoAwesome,
          accentColor = LingoTokens.brandIndigo,
          showProLock = !isVerifiedPro,
          testTag = "studio_card_writing",
          onClick = onOpenWritingStudio
        )
      }
    }
  }
}

@Composable
private fun StudioFeatureTile(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  showProLock: Boolean = false,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = LingoTokens.bgSubtle),
    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = LingoTokens.bgElevated
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          fontSize = 13.sp,
          color = LingoTokens.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      if (showProLock) {
        com.example.ui.components.LingoGoldLockIcon(size = 16.dp)
      }
    }
  }
}

@Composable
fun LearnDailyChallengeCard(
  targetLang: Language,
  cefrLabel: String,
  dailyPhrase: String,
  dailyTranslation: String,
  isCompletedToday: Boolean,
  onCompleteChallenge: () -> Unit,
  speechManager: SpeechManager?
) {
  var selectedAnswerIndex by rememberSaveable(dailyPhrase) { mutableIntStateOf(-1) }
  var challengeAnswered by rememberSaveable(dailyPhrase) { mutableStateOf(isCompletedToday) }

  val options = remember(dailyTranslation, targetLang.code) {
    listOf(
      dailyTranslation,
      "Could you please repeat that more slowly?",
      "Where is the nearest metro entrance?"
    )
  }

  com.example.ui.components.LingoCleanCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("daily_challenge_card"),
    contentPadding = 18.dp
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
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
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = LingoTokens.accentGold,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Daily ${targetLang.name} Challenge",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LingoTokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Surface(
          color = LingoTokens.accentGold.copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, LingoTokens.accentGold.copy(alpha = 0.32f))
        ) {
          Text(
            text = if (challengeAnswered) "Completed +25 XP" else "+25 XP",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LingoTokens.accentGold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "What does “$dailyPhrase” mean?",
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = LingoTokens.textPrimary,
          modifier = Modifier.weight(1f)
        )
        IconButton(
          onClick = { speechManager?.speak(dailyPhrase, targetLang.code, 1.0f) },
          modifier = Modifier.size(44.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Listen to challenge phrase",
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      options.forEachIndexed { idx, optionText ->
        val isSelected = selectedAnswerIndex == idx
        val isCorrectOption = idx == 0
        val borderCol = when {
          challengeAnswered && isCorrectOption -> LingoTokens.brandCyan
          isSelected -> LingoTokens.brandIndigo
          else -> LingoTokens.borderSubtle
        }
        Surface(
          color = when {
            challengeAnswered && isCorrectOption -> LingoTokens.brandCyan.copy(alpha = 0.14f)
            isSelected -> LingoTokens.brandIndigo.copy(alpha = 0.16f)
            else -> LingoTokens.bgSubtle
          },
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, borderCol),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !challengeAnswered) {
              selectedAnswerIndex = idx
            }
            .testTag("daily_challenge_option_$idx")
        ) {
          Text(
            text = optionText,
            fontSize = 14.sp,
            fontWeight = if (isSelected || (challengeAnswered && isCorrectOption)) FontWeight.Bold else FontWeight.Medium,
            color = LingoTokens.textPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
          )
        }
      }

      if (!challengeAnswered) {
        Surface(
          onClick = {
            if (selectedAnswerIndex != -1) {
              challengeAnswered = true
              onCompleteChallenge()
            }
          },
          shape = RoundedCornerShape(16.dp),
          color = if (selectedAnswerIndex != -1) LingoTokens.bgSubtle else LingoTokens.bgSubtle.copy(alpha = 0.55f),
          border = BorderStroke(
            1.dp,
            if (selectedAnswerIndex != -1) LingoTokens.accentGold.copy(alpha = 0.5f) else LingoTokens.borderSubtle
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("claim_daily_challenge_xp_button")
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
              text = "Check Answer · +25 XP",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedAnswerIndex != -1) LingoTokens.accentGold else LingoTokens.textSecondary
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiRoleplayStudioDialog(
  targetLang: Language,
  nativeLang: Language,
  initialCefr: String,
  speechManager: SpeechManager?,
  onDismiss: () -> Unit,
  onCompleteRoleplayReward: (scenarioTitle: String, scorePercent: Int) -> Unit
) {
  var selectedScenario by remember { mutableStateOf(STUDIO_ROLEPLAY_SCENARIOS.first()) }
  var selectedCefr by remember { mutableStateOf(initialCefr.take(2).ifBlank { "A2" }) }

  fun localizedOpening(scenario: StudioRoleplayScenario): String {
    return when (targetLang.code.lowercase()) {
      "en" -> when {
        scenario.title.contains("Café", ignoreCase = true) || scenario.title.contains("Restaurant", ignoreCase = true) ->
          "Hello! Welcome in. What would you like to order today?"
        scenario.title.contains("Hotel", ignoreCase = true) ->
          "Welcome to our hotel! Do you have a reservation for check-in today?"
        scenario.title.contains("Airport", ignoreCase = true) ->
          "Good morning! May I see your passport and boarding pass?"
        scenario.title.contains("Doctor", ignoreCase = true) ->
          "Hello, please come in. How are you feeling today?"
        scenario.title.contains("Job", ignoreCase = true) ->
          "Hello, thanks for coming in! Tell me a bit about your background."
        else -> "Hello! Let's practice ${scenario.title}. How can I help you today?"
      }
      "fr" -> "Bonjour ! Commençons le scénario « ${scenario.title} ». Que souhaitez-vous dire ?"
      "de" -> "Hallo! Beginnen wir mit „${scenario.title}“. Wie kann ich Ihnen helfen?"
      "it" -> "Ciao! Iniziamo lo scenario « ${scenario.title} ». Come posso aiutarti?"
      "ja" -> "こんにちは！「${scenario.title}」のロールプレイを始めましょう。"
      "zh" -> "你好！我们开始“${scenario.title}”情景对话吧。"
      else -> scenario.openingPrompt
    }
  }

  val conversationTurns = remember {
    mutableStateListOf(
      "AI (${selectedScenario.title})" to localizedOpening(selectedScenario)
    )
  }
  val userSubmittedTurns = remember { mutableStateListOf<String>() }
  var userInput by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }
  var isEvaluating by remember { mutableStateOf(false) }
  var evaluationReport by remember { mutableStateOf<RoleplayEvaluationReport?>(null) }
  var lastCorrection by remember { mutableStateOf<String?>(null) }

  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  val isListening by speechManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }
  val spokenText by speechManager?.spokenText?.collectAsState() ?: remember { mutableStateOf("") }

  fun submitRoleplayTurn(rawText: String) {
    val textToSend = rawText.trim()
    if (textToSend.isEmpty() || isSending) return
    userInput = ""
    conversationTurns.add("You" to textToSend)
    userSubmittedTurns.add(textToSend)
    isSending = true
    coroutineScope.launch {
      try {
        val aiReply = GeminiTutorService.chatWithTutor(
          userSpeechInput = textToSend,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = "CEFR $selectedCefr",
          scenarioTitle = selectedScenario.title,
          scenarioInstruction = selectedScenario.objective,
          conversationHistory = conversationTurns.toList()
        )
        conversationTurns.add("AI (${selectedScenario.title})" to aiReply.replyInTargetLanguage)
        lastCorrection = aiReply.grammarFeedback
        speechManager?.speak(aiReply.replyInTargetLanguage, targetLang.code, 1.0f)
      } catch (_: Exception) {
      } finally {
        isSending = false
      }
    }
  }

  LaunchedEffect(spokenText) {
    val clean = spokenText.trim()
    if (clean.isNotBlank() && !isSending) {
      speechManager?.clearSpokenText()
      submitRoleplayTurn(clean)
    }
  }

  LaunchedEffect(selectedScenario.id, targetLang.code) {
    val opening = localizedOpening(selectedScenario)
    speechManager?.speak(opening, targetLang.code, 1.0f)
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager?.clearSpokenText()
      speechManager?.startListening(targetLang.code)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag("ai_roleplay_studio_dialog"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PremiumLearnSurface),
    border = BorderStroke(1.5.dp, PremiumViolet.copy(alpha = 0.6f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            tint = PremiumViolet,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "AI Roleplay Studio (${targetLang.name})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = PremiumTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            color = PremiumViolet.copy(alpha = 0.18f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "CEFR $selectedCefr",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = PremiumViolet,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
          }
          TextButton(onClick = onDismiss) {
            Text("Done", color = PremiumRoyalBlue, fontWeight = FontWeight.ExtraBold)
          }
        }
      }
        // Scenario Picker Horizontal Row (10 Scenarios)
        Text(
          text = "Select Real-Life Scenario (${STUDIO_ROLEPLAY_SCENARIOS.size}):",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = PremiumTextSecondary
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          STUDIO_ROLEPLAY_SCENARIOS.forEach { scenario ->
            val isSelected = selectedScenario.id == scenario.id
            FilterChip(
              selected = isSelected,
              onClick = {
                selectedScenario = scenario
                selectedCefr = scenario.suggestedCefr
                val nextOpening = localizedOpening(scenario)
                conversationTurns.clear()
                conversationTurns.add("AI (${scenario.title})" to nextOpening)
                userSubmittedTurns.clear()
                evaluationReport = null
                lastCorrection = null
              },
              label = {
                Text(
                  text = scenario.title,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PremiumViolet.copy(alpha = 0.2f),
                selectedLabelColor = PremiumViolet
              ),
              modifier = Modifier.testTag("roleplay_scenario_${scenario.id}")
            )
          }
        }

        // Goal Banner + Quick Support Controls (Hint, Translation, Restart)
        Surface(
          color = PremiumLearnElevated,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, PremiumLearnBorder)
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Scenario Goal: ${selectedScenario.objective}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = PremiumTextPrimary
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedButton(
                onClick = {
                  userInput = when (targetLang.code.lowercase()) {
                    "en" -> "Hello, could you help me with ${selectedScenario.title.lowercase()}, please?"
                    "fr" -> "Bonjour, je voudrais de l'aide pour ${selectedScenario.title.lowercase()}, s'il vous plaît."
                    "de" -> "Hallo, könnten Sie mir bei ${selectedScenario.title.lowercase()} bitte helfen?"
                    "it" -> "Ciao, vorrei informazioni per ${selectedScenario.title.lowercase()}, per favore."
                    else -> "Hola, me gustaría información sobre ${selectedScenario.title.lowercase()}, por favor."
                  }
                },
                modifier = Modifier.testTag("roleplay_hint_button")
              ) {
                Text("Hint", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = {
                  lastCorrection = "Translation (${nativeLang.name}): ${localizedOpening(selectedScenario)}"
                },
                modifier = Modifier.testTag("roleplay_translate_toggle")
              ) {
                Text("Translate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = {
                  val opening = localizedOpening(selectedScenario)
                  conversationTurns.clear()
                  conversationTurns.add("AI (${selectedScenario.title})" to opening)
                  userSubmittedTurns.clear()
                  evaluationReport = null
                  lastCorrection = null
                  speechManager?.speak(opening, targetLang.code, 1.0f)
                },
                modifier = Modifier.testTag("roleplay_restart_button")
              ) {
                Text("Restart", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Conversation Transcript
        conversationTurns.forEach { (speaker, message) ->
          val isAi = speaker.startsWith("AI")
          Surface(
            color = if (isAi) PremiumRoyalBlue.copy(alpha = 0.12f) else PremiumViolet.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
              1.dp,
              if (isAi) PremiumRoyalBlue.copy(alpha = 0.35f) else PremiumViolet.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = speaker,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isAi) PremiumRoyalBlue else PremiumViolet
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = message,
                  style = MaterialTheme.typography.bodySmall,
                  color = PremiumTextPrimary
                )
              }
              if (isAi) {
                IconButton(
                  onClick = { speechManager?.speak(message, targetLang.code, 1.0f) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen to AI reply",
                    tint = PremiumRoyalBlue,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }

        lastCorrection?.let { correction ->
          if (correction.isNotBlank()) {
            Surface(
              color = PremiumWarmGold.copy(alpha = 0.14f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, PremiumWarmGold.copy(alpha = 0.4f))
            ) {
              Text(
                text = "Coach Tip: $correction",
                style = MaterialTheme.typography.labelSmall,
                color = PremiumWarmGold,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }

        // Input Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            placeholder = {
              Text(
                "Reply in ${targetLang.name}...",
                style = MaterialTheme.typography.bodySmall
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("roleplay_user_input")
          )

          IconButton(
            onClick = {
              if (isListening) {
                speechManager?.finishListeningNow()
              } else {
                speechManager?.stopSpeaking()
                speechManager?.clearSpokenText()
                val hasAudio = ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasAudio) {
                  speechManager?.startListening(targetLang.code)
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (isListening) PremiumSoftGreen.copy(alpha = 0.25f) else Color.Transparent)
          ) {
            Icon(
              imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = if (isListening) "Stop listening" else "Speak reply",
              tint = if (isListening) PremiumSoftGreen else PremiumRoyalBlue
            )
          }

          Button(
            onClick = { submitRoleplayTurn(userInput) },
            enabled = userInput.isNotBlank() && !isSending,
            modifier = Modifier.testTag("roleplay_send_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumRoyalBlue, contentColor = Color.White)
          ) {
            if (isSending) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
              Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
            }
          }
        }

        // Evaluate Roleplay Feedback Report Button
        OutlinedButton(
          onClick = {
            if (isEvaluating) return@OutlinedButton
            isEvaluating = true
            coroutineScope.launch {
              try {
                val report = GeminiTutorService.evaluateRoleplayCompletion(
                  scenarioId = selectedScenario.id,
                  scenarioTitle = selectedScenario.title,
                  taskObjective = selectedScenario.objective,
                  userTurns = if (userSubmittedTurns.isEmpty()) listOf("Hola, me gustaría practicar este escenario.") else userSubmittedTurns.toList(),
                  targetLanguage = targetLang,
                  nativeLanguage = nativeLang
                )
                evaluationReport = report
                onCompleteRoleplayReward(selectedScenario.title, report.scorePercent)
              } catch (e: Exception) {
                lastCorrection = com.example.ai.AiErrorHandler.handle(e).message
              } finally {
                isEvaluating = false
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("roleplay_evaluate_button"),
          border = BorderStroke(1.dp, PremiumViolet)
        ) {
          if (isEvaluating) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PremiumViolet)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generating AI Roleplay Report...", color = PremiumViolet, fontWeight = FontWeight.Bold)
          } else {
            Text("Finish & Get AI Roleplay Feedback Report", color = PremiumViolet, fontWeight = FontWeight.ExtraBold)
          }
        }

        // Evidence-Based Roleplay Evaluation Report Card
        evaluationReport?.let { report ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("roleplay_feedback_report_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumLearnElevated),
            border = BorderStroke(1.dp, PremiumSoftGreen.copy(alpha = 0.5f))
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "AI Roleplay Feedback Report • ${report.scorePercent}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PremiumSoftGreen
              )
              Text(
                text = "AI Assessment Notice: Scores reflect communication clarity, grammar, and task completion across your submitted turns.",
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = PremiumTextSecondary
              )
              Text(
                text = if (report.objectiveAchieved) "Task Objective: Achieved ✓" else "Task Objective: Partially Completed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PremiumTextPrimary
              )
              Text(
                text = report.taskFeedback,
                style = MaterialTheme.typography.bodySmall,
                color = PremiumTextPrimary
              )
              if (report.grammarAndStyleNotes.isNotEmpty()) {
                Text(
                  text = "Grammar & Style Notes: ${report.grammarAndStyleNotes.joinToString(" • ")}",
                  style = MaterialTheme.typography.labelSmall,
                  color = PremiumWarmGold
                )
              }
              if (report.suggestedUpgrades.isNotEmpty()) {
                Text(
                  text = "Suggested Upgrades: ${report.suggestedUpgrades.joinToString(" • ")}",
                  style = MaterialTheme.typography.labelSmall,
                  color = PremiumSoftGreen
                )
              }
            }
          }
        }
    }
  }
}

@Composable
fun AiSpeakingTutorQuickDialog(
  targetLang: Language,
  nativeLang: Language,
  initialCefr: String,
  speechManager: SpeechManager?,
  onDismiss: () -> Unit
) {
  var selectedDifficulty by remember { mutableStateOf(initialCefr.take(2).ifBlank { "A2" }) }
  var showTranslations by remember { mutableStateOf(true) }
  var userInput by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }

  data class TutorTurn(
    val speaker: String,
    val targetText: String,
    val translation: String,
    val correction: String,
    val followUpQuestion: String
  )

  val localizedIntro = when (targetLang.code.lowercase()) {
    "es" -> "¡Hola! Practiquemos español hablando. Cuéntame cómo estuvo tu día o hazme una pregunta."
    "fr" -> "Bonjour ! Pratiquons le français ensemble. Parlez-moi de votre journée ou posez-moi une question !"
    "de" -> "Hallo! Lass uns Deutsch sprechen üben. Erzähl mir von deinem Tag oder stell mir eine Frage!"
    "it" -> "Ciao! Pratichiamo l'italiano insieme. Raccontami la tua giornata o fammi una domanda!"
    "ja" -> "こんにちは！日本語で話す練習をしましょう。今日あったことを教えてください。"
    "zh" -> "你好！让我们一起练习中文口语吧。告诉我你今天过得怎么样！"
    else -> "Hello! Let's practice speaking ${targetLang.name}. Tell me about your day or ask me any question!"
  }

  val turns = remember(targetLang.code) {
    mutableStateListOf(
      TutorTurn(
        speaker = "AI Tutor",
        targetText = localizedIntro,
        translation = "Hello! Let's practice speaking ${targetLang.name}.",
        correction = "",
        followUpQuestion = "What would you like to talk about today?"
      )
    )
  }

  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  val isListening by speechManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }
  val spokenText by speechManager?.spokenText?.collectAsState() ?: remember { mutableStateOf("") }

  fun submitSpeakingTurn(rawMsg: String) {
    val msg = rawMsg.trim()
    if (msg.isEmpty() || isSending) return
    val prevInput = userInput
    userInput = ""
    turns.add(TutorTurn("You", msg, "", "", ""))
    isSending = true
    coroutineScope.launch {
      try {
        val resp = GeminiTutorService.chatWithTutor(
          userSpeechInput = msg,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = "CEFR $selectedDifficulty",
          scenarioTitle = "Personal Speaking Coach",
          scenarioInstruction = "Provide natural conversation, gentle grammar corrections, and vocabulary tips.",
          conversationHistory = turns.map { it.speaker to it.targetText }
        )
        turns.add(
          TutorTurn(
            speaker = "AI Tutor",
            targetText = resp.replyInTargetLanguage,
            translation = resp.translationInNativeLanguage,
            correction = resp.grammarFeedback.orEmpty(),
            followUpQuestion = resp.phoneticGuide
          )
        )
        speechManager?.speak(resp.replyInTargetLanguage, targetLang.code, 1.0f)
      } catch (e: Exception) {
        val friendly = com.example.ai.AiErrorHandler.handle(e)
        if (turns.lastOrNull()?.speaker == "You") {
          turns.removeAt(turns.lastIndex)
        }
        userInput = prevInput.ifBlank { msg }
        turns.add(
          TutorTurn(
            speaker = "AI Tutor",
            targetText = friendly.message,
            translation = "",
            correction = "",
            followUpQuestion = ""
          )
        )
      } finally {
        isSending = false
      }
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager?.clearSpokenText()
      speechManager?.startListening(targetLang.code)
    }
  }

  LaunchedEffect(spokenText) {
    val clean = spokenText.trim()
    if (clean.isNotBlank() && !isSending) {
      speechManager?.clearSpokenText()
      submitSpeakingTurn(clean)
    }
  }

  LaunchedEffect(targetLang.code) {
    speechManager?.speak(localizedIntro, targetLang.code, 1.0f)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag("ai_speaking_tutor_dialog"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PremiumLearnSurface),
    border = BorderStroke(1.5.dp, PremiumRoyalBlue.copy(alpha = 0.6f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Default.RecordVoiceOver,
            contentDescription = null,
            tint = PremiumRoyalBlue,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "AI Speaking Tutor (${targetLang.name})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = PremiumTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { showTranslations = !showTranslations },
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Translate,
              contentDescription = "Toggle translation",
              tint = if (showTranslations) PremiumRoyalBlue else PremiumTextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
          TextButton(onClick = onDismiss) {
            Text("Close", color = PremiumRoyalBlue, fontWeight = FontWeight.ExtraBold)
          }
        }
      }
        // Difficulty Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("A1", "A2", "B1", "B2", "C1").forEach { level ->
            FilterChip(
              selected = selectedDifficulty == level,
              onClick = { selectedDifficulty = level },
              label = { Text(level, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            )
          }
        }

        turns.forEach { turn ->
          val isAi = turn.speaker == "AI Tutor"
          Surface(
            color = if (isAi) PremiumRoyalBlue.copy(alpha = 0.12f) else PremiumSoftGreen.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
              1.dp,
              if (isAi) PremiumRoyalBlue.copy(alpha = 0.35f) else PremiumSoftGreen.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = turn.speaker,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isAi) PremiumRoyalBlue else PremiumSoftGreen
                )
                if (isAi) {
                  IconButton(
                    onClick = { speechManager?.speak(turn.targetText, targetLang.code, 1.0f) },
                    modifier = Modifier.size(26.dp)
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = "Hear AI Tutor",
                      tint = PremiumRoyalBlue,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
              Text(
                text = turn.targetText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = PremiumTextPrimary
              )
              if (showTranslations && turn.translation.isNotBlank()) {
                Text(
                  text = turn.translation,
                  style = MaterialTheme.typography.labelSmall,
                  fontStyle = FontStyle.Italic,
                  color = PremiumTextSecondary
                )
              }
              if (turn.correction.isNotBlank()) {
                Text(
                  text = "Grammar Note: ${turn.correction}",
                  style = MaterialTheme.typography.labelSmall,
                  color = PremiumWarmGold
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            placeholder = { Text("Speak or type in ${targetLang.name}...", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("speaking_tutor_input")
          )

          IconButton(
            onClick = {
              if (isListening) {
                speechManager?.finishListeningNow()
              } else {
                speechManager?.stopSpeaking()
                speechManager?.clearSpokenText()
                val hasAudio = ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasAudio) {
                  speechManager?.startListening(targetLang.code)
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (isListening) PremiumSoftGreen.copy(alpha = 0.25f) else Color.Transparent)
              .testTag("speaking_tutor_mic_button")
          ) {
            Icon(
              imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = if (isListening) "Stop listening" else "Speak to tutor",
              tint = if (isListening) PremiumSoftGreen else PremiumRoyalBlue
            )
          }

          Button(
            onClick = { submitSpeakingTurn(userInput) },
            enabled = userInput.isNotBlank() && !isSending,
            modifier = Modifier.testTag("speaking_tutor_send_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumRoyalBlue, contentColor = Color.White)
          ) {
            if (isSending) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
              Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
            }
          }
        }
    }
  }
}
