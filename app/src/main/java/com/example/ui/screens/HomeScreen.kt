package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiErrorHandler
import com.example.ai.UpgradeSheetMode
import com.example.data.model.AiConversationPartner
import com.example.data.model.CommunityProfile
import com.example.data.model.CurriculumLesson
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.LessonProgress
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.RepeatedMistakeSummary
import com.example.data.model.SpeakingSession
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.data.repository.AccessPolicyManager
import com.example.data.repository.BillingRepository
import com.example.data.repository.InProgressLessonState
import com.example.data.repository.PlanPricingInfo
import com.example.speech.SpeechManager
import com.example.ui.components.ChangeLearningLanguageBottomSheet
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoGradientAvatar
import com.example.ui.components.LingoGradientProgressRing
import com.example.ui.components.LingoSectionHeader
import com.example.ui.components.LingoSkeletonCard
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTutorOrb
import com.example.ui.components.lingoAuroraBackdrop
import com.example.ui.components.lingoStaggeredAppear
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.components.springTap
import java.text.SimpleDateFormat
import java.util.Locale

data class DailyBarPoint(
  val dayKey: String,
  val shortDayLabel: String,
  val minutes: Int,
  val isToday: Boolean
)

@Composable
fun HomeScreen(
  userProfile: UserProfile,
  nextLesson: CurriculumLesson?,
  unreadNotificationsCount: Int = 0,
  onNavigateToLearn: () -> Unit,
  onNavigateToSpeak: () -> Unit,
  onNavigateToPractice: () -> Unit,
  onStartLesson: (CurriculumLesson) -> Unit,
  onTargetLanguageChanged: (String) -> Unit,
  onStartVoiceCall: () -> Unit = onNavigateToSpeak,
  onStartVideoTutorCall: () -> Unit = onStartVoiceCall,
  onOpenExploreLanguages: () -> Unit = {},
  onOpenCommunityHub: (Int) -> Unit = {},
  onOpenPronunciation: () -> Unit = {},
  onOpenDailyChallenges: () -> Unit = {},
  onOpenProgressDashboard: () -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  onNavigateToVideos: () -> Unit = {},
  communityProfiles: List<CommunityProfile> = emptyList(),
  onOpenCulturePassport: (Int) -> Unit = {},
  onOpenCultureJourney: () -> Unit = { onOpenCulturePassport(0) },
  onOpenSpeakingRooms: () -> Unit = {},
  onOpenLanguageExchange: () -> Unit = {},
  onOpenSmartMatching: () -> Unit = {},
  dueFlashcardsCount: Int = 0,
  completedLessonsCount: Int = 0,
  weeklySessionsCount: Int = 0,
  cefrStageLabel: String = "",
  onOpenPremiumHub: () -> Unit = onOpenProgressDashboard,
  onOpenImmersionMode: () -> Unit = {},
  onOpenConversationReplay: () -> Unit = {},
  onOpenAccentStudio: () -> Unit = {},
  onOpenLanguagePortfolio: () -> Unit = {},
  onOpenMistakeRecovery: () -> Unit = {},
  onOpenCameraLens: () -> Unit = {},
  onOpenConversationRepairLab: () -> Unit = {},
  onOpenLanguageEmergencyMode: () -> Unit = {},
  onOpenAiDebatePartner: () -> Unit = {},
  onOpenExplainItThreeWays: () -> Unit = {},
  onOpenConversationDetective: () -> Unit = {},
  onOpenTeachTheAi: () -> Unit = {},
  onOpenVocabularyBuilder: () -> Unit = onNavigateToPractice,
  onOpenListeningPractice: () -> Unit = onNavigateToLearn,
  onOpenWritingStudio: () -> Unit = onNavigateToPractice,
  onOpenGrammarStudio: () -> Unit = onNavigateToPractice,
  onOpenRoleplayStudio: () -> Unit = onNavigateToSpeak,
  onOpenReadingStudio: () -> Unit = onNavigateToLearn,
  onOpenInviteFriends: () -> Unit = {},
  onOpenExploreCategoryRoute: ((ExploreCategoryRoute) -> Unit)? = null,
  onOpenProfile: () -> Unit = {},
  selectedPartner: AiConversationPartner = AiConversationPartner.ALL_PARTNERS.first(),
  speechManager: SpeechManager? = null,
  billingRepository: BillingRepository? = null,
  flashcards: List<FlashcardProgress> = emptyList(),
  mistakeNotebookEntries: List<MistakeNotebookEntry> = emptyList(),
  repeatedMistakes: List<RepeatedMistakeSummary> = emptyList(),
  dailyMinutesMap: Map<String, Int> = emptyMap(),
  speakingSessions: List<SpeakingSession> = emptyList(),
  lessonProgress: List<LessonProgress> = emptyList(),
  inProgressLessonsMap: Map<String, InProgressLessonState> = emptyMap(),
  onSaveVocabToFlashcard: (String, String, String) -> Unit = { _, _, _ -> },
  onOpenLanguageSelector: (() -> Unit)? = null,
  onLanguageAndLevelChanged: (String, String) -> Unit = { code, _ -> onTargetLanguageChanged(code) },
  onApplyPlacementEvaluation: (String, PlacementTestEvaluation) -> Unit = { code, eval ->
    onLanguageAndLevelChanged(code, eval.recommendedCefrBand)
  }
) {
  var showExploreSheet by remember { mutableStateOf(false) }
  var showTutorLimitDialog by remember { mutableStateOf(false) }
  var showLocalLanguageSheet by remember { mutableStateOf(false) }

  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val activeCefrCode = userProfile.effectiveCefrForLanguage(
    langCode = userProfile.targetLanguageCode,
    fallbackPlacementCefr = cefrStageLabel
  )
  val isEntitlementLoading by AccessPolicyManager.isEntitlementLoading.collectAsState()
  val entitlementState by AccessPolicyManager.entitlementState.collectAsState()
  val tutorUsage by AccessPolicyManager.tutorUsageState.collectAsState()
  val openedBookIdsThisMonth by AccessPolicyManager.openedBookIdsThisMonth.collectAsState()
  val policyState by AccessPolicyManager.policyState.collectAsState()
  val isVerifiedPro = AccessPolicyManager.isUserVerifiedPro(userProfile) && !isEntitlementLoading
  val booksRemainingThisMonth = (policyState.freeBooksPerMonth - openedBookIdsThisMonth.size).coerceAtLeast(0)
  val reducedMotion = rememberReducedMotionEnabled()

  val fullDisplayName = userProfile.displayName.trim().ifBlank { "Learner" }

  val todayKey = remember(entitlementState.timezone) {
    AccessPolicyManager.currentDayKeyInTimezone(entitlementState.timezone)
  }
  val validCompletedTodayCount = remember(lessonProgress, speakingSessions, todayKey, completedLessonsCount) {
    val todayStartMs = runCatching {
      SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(todayKey)?.time ?: 0L
    }.getOrDefault(0L)
    val validLessonsToday = lessonProgress.count { lp ->
      val tsMs = (lp.updatedAt?.seconds ?: lp.createdAt?.seconds ?: 0L) * 1000L
      lp.completed && lp.scorePercent >= 50 && tsMs >= todayStartMs
    }
    val validSessionsToday = speakingSessions.count { ss ->
      val tsMs = (ss.createdAt?.seconds ?: 0L) * 1000L
      ss.turnsCount >= 2L && tsMs >= todayStartMs
    }
    val totalToday = validLessonsToday + validSessionsToday
    if (totalToday > 0) totalToday else completedLessonsCount.coerceAtMost(3)
  }
  val dailyGoalTarget = ((userProfile.dailyGoalMinutes / 5L).toInt()).coerceIn(1, 5)
  val dailyGoalProgress = (validCompletedTodayCount.toFloat() / dailyGoalTarget.toFloat()).coerceIn(0f, 1f)

  val inProgressCheckpoint = nextLesson?.id?.let { inProgressLessonsMap[it] }
  val lessonProgressFraction = remember(inProgressCheckpoint, nextLesson) {
    when {
      inProgressCheckpoint != null && inProgressCheckpoint.totalExercises > 0 -> {
        ((inProgressCheckpoint.currentExerciseIndex + 1).toFloat() / inProgressCheckpoint.totalExercises.toFloat())
          .coerceIn(0.08f, 1f)
      }
      nextLesson != null -> 0.14f
      else -> 0.25f
    }
  }
  val animatedLessonProgress by animateFloatAsState(
    targetValue = lessonProgressFraction,
    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 600, easing = FastOutSlowInEasing),
    label = "hero_lesson_progress"
  )

  val ctaGlowAlpha: Float = if (!reducedMotion) {
    val infinite = rememberInfiniteTransition(label = "hero_cta_pulse")
    val alphaAnim by infinite.animateFloat(
      initialValue = 0.20f,
      targetValue = 0.44f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "hero_cta_glow_alpha"
    )
    alphaAnim
  } else {
    0.26f
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .lingoAuroraBackdrop()
      .testTag("home_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(
          start = LingoTokens.screenPadding,
          end = LingoTokens.screenPadding,
          top = 18.dp,
          bottom = LingoTokens.bottomBarClearance
        )
        .testTag("home_scroll_container"),
      verticalArrangement = Arrangement.spacedBy(LingoTokens.sectionSpacing)
    ) {
      // =====================================================================
      // SECTION 1: HEADER (Avatar + Name + Language Pill + Bell; LTR-locked)
      // =====================================================================
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .lingoStaggeredAppear(0)
        ) {
          val isTightWidth = maxWidth < 290.dp
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_header_section"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .springTap(onClick = onOpenProfile)
                .padding(vertical = 4.dp)
                .testTag("home_header_profile_trigger")
            ) {
              LingoGradientAvatar(
                name = fullDisplayName,
                userId = userProfile.userId,
                size = 46.dp,
                customImageUrl = userProfile.avatarUrl
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "Welcome back,",
                  fontSize = 13.sp,
                  lineHeight = 18.sp,
                  color = LingoTokens.textSecondary,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = fullDisplayName,
                  fontSize = 20.sp,
                  lineHeight = 26.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_user_display_name")
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Compact language selector pill (40dp visual height inside 48dp accessible touch target)
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .semantics(mergeDescendants = true) {
                  role = Role.Button
                  contentDescription = "Change learning language, ${targetLang.name} $activeCefrCode"
                }
                .clickable {
                  if (onOpenLanguageSelector != null) {
                    onOpenLanguageSelector()
                  } else {
                    showLocalLanguageSheet = true
                  }
                }
                .testTag("home_header_language_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                  .height(40.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(LingoTokens.bgGlassCard)
                  .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(16.dp))
                  .padding(horizontal = if (isTightWidth) 10.dp else 10.dp)
              ) {
                Text(
                  text = if (isTightWidth) targetLang.flagEmoji else "${targetLang.flagEmoji} $activeCefrCode",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = LingoTokens.textPrimary,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.testTag("home_header_language_label")
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box {
              IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(LingoTokens.bgGlassCard)
                  .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(16.dp))
                  .testTag("home_notifications_bell")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Notifications,
                  contentDescription = "Notifications",
                  tint = LingoTokens.textPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              if (unreadNotificationsCount > 0) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LingoTokens.brandCyan)
                    .border(1.5.dp, LingoTokens.bgCanvas, CircleShape)
                )
              }
            }
          }
        }
      }

      // =====================================================================
      // SECTION 2: SLIM STATS ROW (NO boxes, even spacing)
      // =====================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .lingoStaggeredAppear(1)
          .clip(RoundedCornerShape(16.dp))
          .springTap(onClick = onOpenProgressDashboard)
          .padding(vertical = 4.dp)
          .testTag("home_stats_grid"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        // Streak
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("home_streak_chip")
        ) {
          Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = "Streak",
            tint = Color(0xFFFF8A4C),
            modifier = Modifier.size(22.dp)
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${userProfile.currentStreak}",
              fontSize = 18.sp,
              lineHeight = 22.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Streak",
              fontSize = 12.sp,
              lineHeight = 16.sp,
              color = LingoTokens.textSecondary,
              maxLines = 1
            )
          }
        }

        // XP
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("home_xp_chip")
        ) {
          Icon(
            imageVector = Icons.Outlined.StarOutline,
            contentDescription = "XP",
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(22.dp)
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${userProfile.xpPoints}",
              fontSize = 18.sp,
              lineHeight = 22.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "XP",
              fontSize = 12.sp,
              lineHeight = 16.sp,
              color = LingoTokens.textSecondary,
              maxLines = 1
            )
          }
        }

        // Daily goal with animated progress ring
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .weight(1.15f)
            .testTag("home_level_chip")
        ) {
          LingoGradientProgressRing(
            progress = dailyGoalProgress.coerceAtLeast(0.06f),
            size = 38.dp,
            strokeWidth = 3.5.dp
          ) {
            Text(
              text = "$validCompletedTodayCount/$dailyGoalTarget",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary,
              maxLines = 1
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "$validCompletedTodayCount/$dailyGoalTarget",
              fontSize = 18.sp,
              lineHeight = 22.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary,
              maxLines = 1
            )
            Text(
              text = "Daily goal",
              fontSize = 12.sp,
              lineHeight = 16.sp,
              color = LingoTokens.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // =====================================================================
      // SECTION 3: HERO CARD (Signature gradient #6C63FF -> #22D3EE, radius 28)
      // =====================================================================
      if (isEntitlementLoading) {
        LingoSkeletonCard(
          height = 200.dp,
          modifier = Modifier
            .lingoStaggeredAppear(2)
            .testTag("home_entitlement_skeleton")
        )
      } else {
        val heroTitle = nextLesson?.title ?: "${targetLang.name} Foundations"
        val heroCategory = nextLesson?.category ?: targetLang.name
        val heroMinutes = nextLesson?.estimatedMinutes ?: 5
        val heroXp = nextLesson?.xpReward ?: 50
        val heroMeta = "$heroCategory · $heroMinutes min · +$heroXp XP"

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .lingoStaggeredAppear(2)
            .shadow(
              elevation = 20.dp,
              shape = RoundedCornerShape(28.dp),
              ambientColor = LingoTokens.brandIndigo.copy(alpha = 0.40f),
              spotColor = LingoTokens.brandCyan.copy(alpha = 0.34f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
              brush = Brush.linearGradient(
                colors = listOf(
                  LingoTokens.brandIndigo,
                  Color(0xFF4F7CFF),
                  LingoTokens.brandCyan
                )
              )
            )
            .border(
              width = 1.dp,
              color = Color.White.copy(alpha = 0.24f),
              shape = RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
            .testTag("home_next_lesson_card")
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "CONTINUE LEARNING",
              fontSize = 12.sp,
              lineHeight = 16.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.1.sp,
              color = Color.White.copy(alpha = 0.86f)
            )

            Text(
              text = heroTitle,
              fontSize = 24.sp,
              lineHeight = 30.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.fillMaxWidth()
            )

            // Thin animated progress bar
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.24f))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(animatedLessonProgress)
                  .height(5.dp)
                  .clip(RoundedCornerShape(50))
                  .background(Color.White)
              )
            }

            Text(
              text = heroMeta,
              fontSize = 14.sp,
              lineHeight = 20.sp,
              fontWeight = FontWeight.Medium,
              color = Color.White.copy(alpha = 0.90f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Large white pill button with gentle glow pulse
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .drawBehind {
                  drawRoundRect(
                    color = Color.White.copy(alpha = ctaGlowAlpha),
                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                      size.width + 8.dp.toPx(),
                      size.height + 8.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
                  )
                }
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .springTap {
                  val hasLessonUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
                    com.example.ads.RewardedFeatureType.LEARN_LESSON,
                    nextLesson?.id
                  )
                  if (isVerifiedPro || hasLessonUnlock) {
                    if (nextLesson != null) {
                      onStartLesson(nextLesson)
                    } else {
                      onNavigateToLearn()
                    }
                  } else {
                    AiErrorHandler.requestUpgrade(
                      mode = UpgradeSheetMode.LESSONS,
                      featureKey = AccessPolicyFeatureKeys.LESSONS_DAILY,
                      headline = "Unlock structured lessons",
                      reason = "Get full access to all 500+ guided CEFR lessons, AI coaching with Emma, and Roleplay Studio.",
                      itemId = nextLesson?.id,
                      onRewardedUnlockGranted = {
                        if (nextLesson != null) {
                          onStartLesson(nextLesson)
                        } else {
                          onNavigateToLearn()
                        }
                      }
                    )
                  }
                }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("home_continue_lesson_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.PlayArrow,
                  contentDescription = null,
                  tint = Color(0xFF0B0F1A),
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Continue lesson",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0B0F1A),
                  maxLines = 1
                )
                if (!isVerifiedPro) {
                  Spacer(modifier = Modifier.width(8.dp))
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Pro feature",
                    tint = LingoTokens.accentGold,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      // =====================================================================
      // SECTION 4: EMMA CARD (Glass, orb + green dot + outline gradient CTA)
      // =====================================================================
      val partnerName = selectedPartner.name.ifBlank { "Emma" }
      LingoCleanCard(
        contentPadding = 20.dp,
        modifier = Modifier
          .fillMaxWidth()
          .lingoStaggeredAppear(3)
          .testTag("hero_ai_voice_call_card")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          LingoTutorOrb(
            size = 56.dp,
            isSpeaking = false
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = partnerName,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(LingoTokens.statusOnline)
              )
            }
            Text(
              text = "Your AI speaking tutor",
              fontSize = 14.sp,
              lineHeight = 20.sp,
              color = LingoTokens.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LingoTokens.bgGlassCard)
            .border(
              width = 1.25.dp,
              brush = Brush.horizontalGradient(
                listOf(
                  LingoTokens.brandIndigo.copy(alpha = 0.72f),
                  LingoTokens.brandCyan.copy(alpha = 0.72f)
                )
              ),
              shape = RoundedCornerShape(18.dp)
            )
            .springTap {
              val hasEmmaUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
                com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA
              )
              if (isVerifiedPro) {
                if (tutorUsage.remainingSecondsToday <= 0) {
                  showTutorLimitDialog = true
                } else {
                  onStartVoiceCall()
                }
              } else if (hasEmmaUnlock) {
                onStartVoiceCall()
              } else {
                AiErrorHandler.requestUpgrade(
                  mode = UpgradeSheetMode.AI_UPGRADE,
                  featureKey = AccessPolicyFeatureKeys.AI_VOICE_SECONDS,
                  headline = "Talk live with $partnerName",
                  reason = "Practice natural conversations with real-time speaking feedback with LingoSphere Pro.",
                  onRewardedUnlockGranted = {
                    onStartVoiceCall()
                  }
                )
              }
            }
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("home_call_ai_tutor_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Mic,
              contentDescription = null,
              tint = LingoTokens.brandCyan,
              modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Talk to $partnerName",
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold,
              color = LingoTokens.textPrimary,
              maxLines = 1
            )
            if (!isVerifiedPro) {
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Pro feature",
                tint = LingoTokens.accentGold,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // =====================================================================
      // NATIVE ADVANCED AD CARD (Free users only; omitted completely for Pro)
      // =====================================================================
      com.example.ads.LingoNativeAdCard(
        userProfile = userProfile,
        placementKey = "home_between_sections",
        onCtaClick = onOpenPremiumHub
      )

      // =====================================================================
      // SECTION 5: PRACTICE (2x2 equal square glass tiles, 16dp gap)
      // =====================================================================
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .lingoStaggeredAppear(4),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        LingoSectionHeader(
          title = "Practice",
          actionLabel = "All tools",
          actionTestTag = "home_explore_all_card",
          onAction = { showExploreSheet = true }
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          HomePracticeSquareTile(
            title = "Flashcards",
            subtitle = if (dueFlashcardsCount > 0) "$dueFlashcardsCount due" else "Ready to review",
            icon = Icons.Outlined.Style,
            iconAccent = Color(0xFF60A5FA),
            locked = false,
            onClick = onOpenVocabularyBuilder,
            testTag = "home_quick_vocabulary",
            modifier = Modifier.weight(1f)
          )
          HomePracticeSquareTile(
            title = "Roleplay",
            subtitle = "Real scenarios",
            icon = Icons.Outlined.RecordVoiceOver,
            iconAccent = Color(0xFFA78BFA),
            locked = !isVerifiedPro,
            onClick = {
              val hasRoleplayUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
                com.example.ads.RewardedFeatureType.ROLEPLAY
              )
              if (isVerifiedPro || hasRoleplayUnlock) {
                onOpenRoleplayStudio()
              } else {
                AiErrorHandler.requestUpgrade(
                  mode = UpgradeSheetMode.ROLEPLAY,
                  featureKey = AccessPolicyFeatureKeys.AI_ROLEPLAY_SESSIONS,
                  headline = "Unlock Roleplay Studio",
                  reason = "Practice realistic café, travel, and work scenarios with LingoSphere Pro.",
                  onRewardedUnlockGranted = {
                    onOpenRoleplayStudio()
                  }
                )
              }
            },
            testTag = "home_quick_roleplay",
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          HomePracticeSquareTile(
            title = "Books & Audio",
            subtitle = if (isVerifiedPro) "Unlimited" else "$booksRemainingThisMonth of 5 free",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            iconAccent = Color(0xFF22D3EE),
            locked = false,
            onClick = onOpenListeningPractice,
            testTag = "home_open_books_listening_card",
            modifier = Modifier.weight(1f)
          )
          HomePracticeSquareTile(
            title = "Language Games",
            subtitle = "Daily drills",
            icon = Icons.Outlined.Extension,
            iconAccent = Color(0xFF34D399),
            locked = false,
            onClick = onNavigateToPractice,
            testTag = "home_quick_games",
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }

  if (showLocalLanguageSheet) {
    ChangeLearningLanguageBottomSheet(
      userProfile = userProfile,
      cefrStageLabel = activeCefrCode,
      onLanguageAndLevelSelected = { code, cefr ->
        onLanguageAndLevelChanged(code, cefr)
      },
      onApplyPlacementEvaluation = { code, eval ->
        onApplyPlacementEvaluation(code, eval)
      },
      onDismiss = { showLocalLanguageSheet = false }
    )
  }

  if (showTutorLimitDialog) {
    AiTutorLimitReachedModal(
      isPro = isVerifiedPro,
      isMonthlyExhausted = tutorUsage.isMonthlyExhausted,
      nextMonthResetDateLabel = tutorUsage.nextMonthResetDateLabel,
      billingRepository = billingRepository,
      onSeePro = {
        showTutorLimitDialog = false
        onOpenPremiumHub()
      },
      onDismiss = { showTutorLimitDialog = false }
    )
  }

  if (showExploreSheet) {
    HomeExploreBottomSheet(
      onNavigateToPractice = onNavigateToPractice,
      onOpenCultureJourney = onOpenCultureJourney,
      onNavigateToVideos = onNavigateToVideos,
      onOpenSpeakingRooms = onOpenSpeakingRooms,
      onOpenLanguageExchange = onOpenLanguageExchange,
      onOpenSmartMatching = onOpenSmartMatching,
      onOpenDailyChallenges = onOpenDailyChallenges,
      onOpenProgressDashboard = onOpenProgressDashboard,
      onOpenExploreLanguages = onOpenExploreLanguages,
      onOpenImmersionMode = onOpenImmersionMode,
      onOpenConversationReplay = onOpenConversationReplay,
      onOpenAccentStudio = onOpenAccentStudio,
      onOpenLanguagePortfolio = onOpenLanguagePortfolio,
      onOpenMistakeRecovery = onOpenMistakeRecovery,
      onOpenCameraLens = onOpenCameraLens,
      onOpenConversationRepairLab = onOpenConversationRepairLab,
      onOpenLanguageEmergencyMode = onOpenLanguageEmergencyMode,
      onOpenAiDebatePartner = onOpenAiDebatePartner,
      onOpenExplainItThreeWays = onOpenExplainItThreeWays,
      onOpenConversationDetective = onOpenConversationDetective,
      onOpenTeachTheAi = onOpenTeachTheAi,
      onOpenInviteFriends = onOpenInviteFriends,
      userProfile = userProfile,
      completedLessonsCount = completedLessonsCount,
      weeklySessionsCount = weeklySessionsCount,
      cefrStageLabel = cefrStageLabel,
      onOpenVocabularyBuilder = onOpenVocabularyBuilder,
      onOpenListeningPractice = onOpenListeningPractice,
      onOpenWritingStudio = onOpenWritingStudio,
      onOpenGrammarStudio = onOpenGrammarStudio,
      onOpenRoleplayStudio = onOpenRoleplayStudio,
      onOpenReadingStudio = onOpenReadingStudio,
      onOpenPronunciation = onOpenPronunciation,
      onOpenCategoryRoute = onOpenExploreCategoryRoute,
      onDismiss = { showExploreSheet = false }
    )
  }
}

@Composable
private fun HomePracticeSquareTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconAccent: Color,
  locked: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  LingoCleanCard(
    onClick = onClick,
    contentPadding = 18.dp,
    modifier = modifier
      .aspectRatio(1.06f)
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(44.dp)
            .drawBehind {
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(
                    iconAccent.copy(alpha = 0.28f),
                    Color.Transparent
                  ),
                  center = center,
                  radius = size.minDimension * 0.95f
                ),
                radius = size.minDimension * 0.95f,
                center = center
              )
            }
            .clip(RoundedCornerShape(14.dp))
            .background(iconAccent.copy(alpha = 0.14f))
            .border(1.dp, iconAccent.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconAccent,
            modifier = Modifier.size(21.dp)
          )
        }
        if (locked) {
          LingoGoldLockIcon(size = 16.dp)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = title,
          fontSize = 16.sp,
          lineHeight = 21.sp,
          fontWeight = FontWeight.SemiBold,
          color = LingoTokens.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          text = subtitle,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          color = LingoTokens.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
fun AiTutorLimitReachedModal(
  isPro: Boolean,
  isMonthlyExhausted: Boolean,
  nextMonthResetDateLabel: String,
  billingRepository: BillingRepository?,
  onSeePro: () -> Unit,
  onDismiss: () -> Unit
) {
  val pricingMap by (
    billingRepository?.planPricingMap
      ?: kotlinx.coroutines.flow.MutableStateFlow<Map<String, PlanPricingInfo>>(emptyMap())
    ).collectAsState()
  val monthlyPlanInfo = pricingMap[SubscriptionPlan.PRODUCT_ID_MONTHLY]
  val isPlayCatalogLoaded = monthlyPlanInfo?.isLiveFromPlayStore == true
  val monthlyPrice = monthlyPlanInfo?.formattedPrice.orEmpty()

  if (isPro) {
    val bodyMessage = if (isMonthlyExhausted) {
      "You've used this month's 150 minutes. It resets on $nextMonthResetDateLabel."
    } else {
      "You've used today's 15 minutes with Emma. It resets at midnight."
    }
    AlertDialog(
      onDismissRequest = onDismiss,
      containerColor = Color(0xFF101728),
      title = {
        Text(
          text = "AI Tutor limit reached",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary
        )
      },
      text = {
        Text(
          text = bodyMessage,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = LingoTokens.textSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.brandIndigo),
          modifier = Modifier.testTag("tutor_pro_limit_ok_button")
        ) {
          Text("OK", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    )
  } else {
    val seeProLabel = if (isPlayCatalogLoaded) {
      "Upgrade to Pro ($monthlyPrice)"
    } else {
      "Upgrade to Pro"
    }
    AlertDialog(
      onDismissRequest = onDismiss,
      containerColor = Color(0xFF101728),
      title = {
        Text(
          text = "Unlock AI Tutor with Pro",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary,
          modifier = Modifier.testTag("tutor_free_limit_title")
        )
      },
      text = {
        Text(
          text = "Live AI voice & video coaching, 500+ structured lessons, and Roleplay Studio are included with LingoSphere Pro.",
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = LingoTokens.textSecondary,
          modifier = Modifier.testTag("tutor_free_limit_body")
        )
      },
      confirmButton = {
        Button(
          onClick = onSeePro,
          colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.brandIndigo),
          modifier = Modifier.testTag("tutor_free_limit_see_pro_button")
        ) {
          Text(seeProLabel, fontWeight = FontWeight.Bold, color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Not now", color = LingoTokens.textSecondary)
        }
      }
    )
  }
}
