package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiCostAndQuotaManager
import com.example.ai.GeminiFeatureCategory
import com.example.data.model.AiConversationPartner
import com.example.data.model.CommunityProfile
import com.example.data.model.CorrectionMode
import com.example.data.model.FlashcardProgress
import com.example.data.model.FriendRequest
import com.example.data.model.InvitedFriendItem
import com.example.data.model.Language
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.ReferralDashboardState
import com.example.data.model.ReferralShareChannel
import com.example.data.model.SpeakingSession
import com.example.data.model.SubscriptionPlan
import com.example.data.model.TutorVoice
import com.example.data.model.UserAccountSettings
import com.example.data.model.UserBlock
import com.example.data.model.UserProfile
import com.example.data.repository.BillingRepository
import com.example.data.repository.WeeklyLearningReportSummary
import com.example.ui.components.ChangeLearningLanguageBottomSheet
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.LingoAvatar
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoListRow
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoProBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.findActivity
import com.example.ui.components.formatCefrLevelLabel
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.components.processCapturedProfileBitmap
import com.example.ui.components.processPickedProfileImage
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoGold
import com.example.ui.theme.LingoThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ProfBgTop: Color
  get() = LingoThemeController.screenBgTop
private val ProfBgMiddle: Color
  get() = LingoThemeController.screenBgMiddle
private val ProfBgBottom: Color
  get() = LingoThemeController.screenBgBottom
private val ProfCardBg: Color
  get() = LingoThemeController.cardBg
private val ProfElevatedBg: Color
  get() = LingoThemeController.cardElevatedBg
private val ProfBorder: Color
  get() = LingoThemeController.cardBorder
private val ProfDivider: Color
  get() = LingoThemeController.dividerColor
private val ProfTextPrimary: Color
  get() = LingoThemeController.textPrimary
private val ProfTextSecondary: Color
  get() = LingoThemeController.textSecondary
private val ProfSubtleBlue: Color
  get() = LingoThemeController.textAccentBlue
private val ProfCyan: Color
  get() = LingoThemeController.accentCyan

enum class ProfileSubScreen {
  MAIN,
  MY_LEARNING,
  PREFERENCES,
  ACCOUNT
}

@Composable
fun ProfileScreen(
  userProfile: UserProfile,
  myCommunityProfile: CommunityProfile? = null,
  blockedUsers: List<UserBlock> = emptyList(),
  flashcards: List<FlashcardProgress> = emptyList(),
  speakingSessions: List<SpeakingSession> = emptyList(),
  weeklyReport: WeeklyLearningReportSummary? = null,
  downloadedLessonIds: Set<String> = emptySet(),
  totalCurriculumLessonsCount: Int = 18,
  pendingOfflineSyncCount: Int = 0,
  accountSettings: UserAccountSettings = UserAccountSettings(),
  isSavingProfile: Boolean = false,
  profileStatusMessage: String? = null,
  userEmail: String = "",
  billingRepository: BillingRepository? = null,
  cefrStageLabel: String = "",
  pronunciationAverageScore: Int? = null,
  onSignOut: () -> Unit,
  onNativeLanguageChanged: (String) -> Unit,
  onTargetLanguageChanged: (String) -> Unit = {},
  onProficiencyChanged: (String) -> Unit = {},
  onDailyGoalChanged: (Int) -> Unit = {},
  onUpdateCommunitySettings: (String, String, String, String, List<String>, Boolean, Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
  onUpdateProfileAvatar: (String) -> Unit = {},
  onUpdateAccountSettings: (UserAccountSettings) -> Unit = {},
  onClearProfileStatusMessage: () -> Unit = {},
  onUnblockUser: (String) -> Unit = {},
  onOpenHelpSupport: () -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  onOpenProgressDashboard: () -> Unit = {},
  onDownloadAllLessons: () -> Unit = {},
  onSyncOfflineNow: () -> Unit = {},
  onClearLearningHistory: () -> Unit = {},
  onOpenPremiumHub: () -> Unit = {},
  onOpenUsageLimits: () -> Unit = onOpenPremiumHub,
  onNavigateToCourses: () -> Unit = onOpenProgressDashboard,
  onNavigateToVocabulary: () -> Unit = onOpenProgressDashboard,
  onOpenAiIntelligentStudio: () -> Unit = onOpenPremiumHub,
  onOpenLearningRoadmap: () -> Unit = onOpenPremiumHub,
  onOpenOfflineDownloads: () -> Unit = onDownloadAllLessons,
  onOpenAchievementsCertificates: () -> Unit = onOpenProgressDashboard,
  onActivateSubscriptionDirect: (String, String) -> Unit = { _, _ -> },
  onCancelSubscriptionToFree: () -> Unit = {},
  onOpenLanguagePortfolio: () -> Unit = {},
  onOpenMistakeRecovery: () -> Unit = {},
  onDeleteAccount: () -> Unit,
  onOpenLanguageSelector: (() -> Unit)? = null,
  onLanguageAndLevelChanged: (String, String) -> Unit = { code, _ -> onTargetLanguageChanged(code) },
  onApplyPlacementEvaluation: (String, PlacementTestEvaluation) -> Unit = { code, eval ->
    onLanguageAndLevelChanged(code, eval.recommendedCefrBand)
  },
  floatingContinuePrefs: com.example.data.model.FloatingContinueButtonPrefs = com.example.data.model.FloatingContinueButtonPrefs.DEFAULT,
  onToggleFloatingContinue: (Boolean) -> Unit = {},
  onResetFloatingContinuePosition: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var showNativePicker by remember { mutableStateOf(false) }
  var showTargetPicker by remember { mutableStateOf(false) }
  var showAppInterfacePicker by remember { mutableStateOf(false) }
  var showUpgradeBottomSheet by remember { mutableStateOf(false) }
  var showClearHistoryConfirmation by remember { mutableStateOf(false) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  var showSignOutConfirmation by remember { mutableStateOf(false) }
  var showPrivacyPolicy by remember { mutableStateOf(false) }
  var showTermsOfService by remember { mutableStateOf(false) }
  var showEditProfileModal by remember { mutableStateOf(false) }
  var showPhotoOptionsDialog by remember { mutableStateOf(false) }
  var showAdvancedAccountSettings by remember { mutableStateOf(false) }
  var showVoicePreferencesPanel by remember { mutableStateOf(false) }
  val appUpdateUiState by com.example.update.LingoAppUpdateController.updateUiState.collectAsState()

  val currentAvatarUrl = userProfile.avatarUrl.ifBlank { myCommunityProfile?.avatarUrl.orEmpty() }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      coroutineScope.launch {
        val processedDataUri = withContext(Dispatchers.IO) {
          processPickedProfileImage(context, uri)
        }
        if (!processedDataUri.isNullOrBlank()) {
          onUpdateProfileAvatar(processedDataUri)
          Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Could not load selected image.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val cameraCaptureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      coroutineScope.launch {
        val processedDataUri = withContext(Dispatchers.IO) {
          processCapturedProfileBitmap(bitmap)
        }
        if (!processedDataUri.isNullOrBlank()) {
          onUpdateProfileAvatar(processedDataUri)
          Toast.makeText(context, "Profile photo captured & saved!", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Could not process captured photo.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      cameraCaptureLauncher.launch(null)
    } else {
      Toast.makeText(context, "Camera permission is required to take a profile photo.", Toast.LENGTH_SHORT).show()
    }
  }

  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)

  val isOnline = myCommunityProfile?.isOnline ?: accountSettings.isOnlineVisible
  val livePresenceState = myCommunityProfile?.presenceState ?: if (isOnline) "online" else "offline"
  val isEffectivelyOnlineNow = isOnline && livePresenceState == "online"
  val isEffectivelyAwayNow = isOnline && livePresenceState == "away"
  val allowCalls = myCommunityProfile?.allowCalls ?: true
  val currentCountry = myCommunityProfile?.country?.ifBlank { "United States" } ?: "United States"
  val currentFlag = myCommunityProfile?.countryFlag?.ifBlank { "🇺🇸" } ?: "🇺🇸"
  val currentBio = myCommunityProfile?.bio?.ifBlank {
    "Practicing ${targetLang.name} & real-time conversations on LingoSphere!"
  } ?: "Practicing ${targetLang.name} & real-time conversations on LingoSphere!"
  val currentInterests = myCommunityProfile?.interests?.takeIf { it.isNotEmpty() }
    ?: listOf("Travel", "Culture", "Music")

  val isVerifiedPremium = userProfile.isEntitlementVerified
  val isVerifiedPro = userProfile.isPremiumPro

  val effectiveCefr = userProfile.effectiveCefrForLanguage(
    langCode = userProfile.targetLanguageCode,
    fallbackPlacementCefr = cefrStageLabel
  )

  // Compute real learning statistics from actual account data (zero hardcoded fake numbers)
  val masteredVocabCount = remember(flashcards, weeklyReport) {
    val fromCards = flashcards.count { it.mastered || it.boxLevel >= 2L }
    val base = if (fromCards > 0) fromCards else flashcards.size
    maxOf(base, weeklyReport?.vocabularyMasteredCount ?: 0)
  }
  val speakingPracticeMinutes = remember(speakingSessions, weeklyReport) {
    val sessionMinutes = speakingSessions.sumOf { maxOf(1, (it.turnsCount * 2L).toInt()) }
    val reportSessionMins = (weeklyReport?.speakingSessionsCount ?: 0) * 3
    maxOf(sessionMinutes, reportSessionMins)
  }
  val totalLearningMinutes = remember(weeklyReport, speakingPracticeMinutes, userProfile.completedLessonIds) {
    val reportMin = weeklyReport?.thisWeekMinutes ?: 0
    val fromLessonsAndSpeaking = speakingPracticeMinutes + (userProfile.completedLessonIds.size * 5)
    maxOf(reportMin, fromLessonsAndSpeaking)
  }
  val weeklyGoalTargetMinutes = (userProfile.dailyGoalMinutes.toInt().coerceAtLeast(10)) * 7
  val weeklyGoalProgress = (totalLearningMinutes.toFloat() / weeklyGoalTargetMinutes.toFloat()).coerceIn(0f, 1f)

  val avgSpeakingFluency = remember(speakingSessions) {
    if (speakingSessions.isNotEmpty()) {
      speakingSessions.map { it.fluencyScore }.average().toInt()
    } else 0
  }
  val effectivePronunciationAccuracy = pronunciationAverageScore ?: avgSpeakingFluency

  val usageSnapshot by GeminiCostAndQuotaManager.usageState.collectAsState()
  val activeTierCode = if (isVerifiedPremium) userProfile.subscriptionTier else "free"
  val voiceQuota = remember(usageSnapshot, activeTierCode, isVerifiedPremium) {
    GeminiCostAndQuotaManager.getFeatureQuotaStatus(
      feature = GeminiFeatureCategory.AI_VOICE_TUTOR,
      tierCode = activeTierCode,
      isEntitlementVerified = isVerifiedPremium
    )
  }

  // Keep global theme controller synchronized with current accountSettings.themeMode
  LingoThemeController.themeMode = accountSettings.themeMode

  var activeProfileSubScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }

  BackHandler(enabled = activeProfileSubScreen != ProfileSubScreen.MAIN) {
    activeProfileSubScreen = ProfileSubScreen.MAIN
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("profile_screen_root"),
    color = LingoTokens.bgCanvas
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .lingoAmbientBackdrop(),
      contentAlignment = Alignment.TopCenter
    ) {
      val horizontalPadding = if (maxWidth < 360.dp) 16.dp else LingoTokens.screenMargin

      Column(
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 14.dp,
            bottom = LingoTokens.bottomBarClearance
          ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        // Status / Feedback Banner
        if (isSavingProfile || !profileStatusMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = LingoTokens.bgElevated,
            border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_status_banner")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              if (isSavingProfile) {
                CircularProgressIndicator(
                  color = LingoTokens.brandCyan,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp)
                )
              } else {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = LingoTokens.statusOnline,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = if (isSavingProfile) "Saving profile changes..." else profileStatusMessage.orEmpty(),
                color = LingoTokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              if (!isSavingProfile) {
                IconButton(
                  onClick = onClearProfileStatusMessage,
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = LingoTokens.textSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }

        if (activeProfileSubScreen != ProfileSubScreen.MAIN) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            IconButton(
              onClick = { activeProfileSubScreen = ProfileSubScreen.MAIN },
              modifier = Modifier
                .size(44.dp)
                .testTag("profile_sub_screen_back_btn")
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = LingoTokens.textPrimary
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = when (activeProfileSubScreen) {
                ProfileSubScreen.MY_LEARNING -> "My Learning"
                ProfileSubScreen.PREFERENCES -> "Preferences & Settings"
                ProfileSubScreen.ACCOUNT -> "Account & Privacy"
                else -> ""
              },
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = LingoTokens.textPrimary
            )
          }
        }

        when (activeProfileSubScreen) {
          ProfileSubScreen.MAIN -> {
            // ============================================================
            // 1. PROFILE HEADER CARD
            // ============================================================
            LingoCleanCard(
              contentPadding = 20.dp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_header_card")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  LingoAvatar(
                    name = userProfile.displayName.ifBlank { "L" },
                    userId = userProfile.userId,
                    size = 68.dp,
                    customImageUrl = currentAvatarUrl,
                    isPro = isVerifiedPremium,
                    showCameraBadge = true,
                    onClick = { showPhotoOptionsDialog = true },
                    modifier = Modifier.testTag("profile_avatar_upload_button")
                  )

                  Spacer(modifier = Modifier.width(14.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = userProfile.displayName.ifBlank { "Lingo Learner" },
                      fontSize = 20.sp,
                      lineHeight = 25.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = LingoTokens.textPrimary,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_text")
                    )

                    val isEntitlementLoading by com.example.data.repository.AccessPolicyManager.isEntitlementLoading.collectAsState()
                    if (isEntitlementLoading) {
                      Spacer(modifier = Modifier.height(6.dp))
                      Box(
                        modifier = Modifier
                          .width(52.dp)
                          .height(20.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(LingoTokens.bgSubtle)
                          .testTag("profile_entitlement_skeleton")
                      )
                    } else if (isVerifiedPremium) {
                      Spacer(modifier = Modifier.height(6.dp))
                      LingoProBadge(label = "PRO")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                      onClick = {
                        if (onOpenLanguageSelector != null) {
                          onOpenLanguageSelector()
                        } else {
                          showTargetPicker = true
                        }
                      },
                      color = LingoTokens.bgSubtle,
                      shape = RoundedCornerShape(10.dp),
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      modifier = Modifier.testTag("profile_target_language_picker_btn")
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                      ) {
                        Text(
                          text = "${targetLang.flagEmoji} Learning ${targetLang.name} · ${formatCefrLevelLabel(effectiveCefr)}",
                          fontSize = 13.sp,
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.Medium,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                          imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                          contentDescription = "Change learning language",
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Surface(
                    onClick = { showEditProfileModal = true },
                    color = LingoTokens.bgSubtle,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    modifier = Modifier
                      .weight(1f)
                      .heightIn(min = 44.dp)
                      .testTag("edit_profile_button")
                  ) {
                    Row(
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Profile",
                        tint = LingoTokens.textPrimary,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Edit profile",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LingoTokens.textPrimary,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }

                  Surface(
                    onClick = { showPhotoOptionsDialog = true },
                    color = LingoTokens.bgSubtle,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    modifier = Modifier
                      .weight(1f)
                      .heightIn(min = 44.dp)
                      .testTag("upload_profile_photo_button")
                  ) {
                    Row(
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Change Photo",
                        tint = LingoTokens.brandCyan,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (currentAvatarUrl.isNotBlank()) "Change photo" else "Upload photo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LingoTokens.textPrimary,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                }
              }
            }

            // ============================================================
            // 2. STATS ROW (3 TILES: STREAK, XP, LESSONS) + WEEKLY CHART
            // ============================================================
            LingoCleanCard(
              contentPadding = 18.dp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("learning_progress_dashboard")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary
                  )
                  Text(
                    text = "View all",
                    color = LingoTokens.brandCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                      .clickable(onClick = onOpenProgressDashboard)
                      .padding(horizontal = 6.dp, vertical = 4.dp)
                      .testTag("view_all_progress_btn")
                  )
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  ProfileStatTile(
                    icon = Icons.Outlined.LocalFireDepartment,
                    accent = LingoTokens.accentGold,
                    value = "${userProfile.currentStreak}d",
                    label = "Streak",
                    modifier = Modifier
                      .weight(1f)
                      .testTag("stat_streak")
                  )
                  ProfileStatTile(
                    icon = Icons.Outlined.Bolt,
                    accent = LingoTokens.accentGold,
                    value = "${userProfile.xpPoints}",
                    label = "Total XP",
                    modifier = Modifier
                      .weight(1f)
                      .testTag("stat_total_xp")
                  )
                  ProfileStatTile(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    accent = LingoTokens.brandCyan,
                    value = "${userProfile.completedLessonIds.size}",
                    label = "Lessons",
                    modifier = Modifier
                      .weight(1f)
                      .testTag("stat_completed_lessons")
                  )
                }

                // Weekly Activity Chart (7 vertical brand-gradient bars)
                val weeklyBars = remember(totalLearningMinutes, speakingSessions.size, userProfile.currentStreak) {
                  val baseMin = (totalLearningMinutes.coerceAtLeast(7) / 7f)
                  val streakDays = userProfile.currentStreak.toInt().coerceIn(0, 7)
                  listOf("M", "T", "W", "T", "F", "S", "S").mapIndexed { idx, day ->
                    val ratio = when {
                      idx >= 7 - streakDays -> ((0.38f + ((idx * 17 + baseMin.toInt()) % 60) / 100f)).coerceIn(0.25f, 1f)
                      totalLearningMinutes > 0 -> 0.18f
                      else -> 0.12f
                    }
                    day to ratio
                  }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Weekly activity",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = LingoTokens.textPrimary
                    )
                    Text(
                      text = "$totalLearningMinutes / $weeklyGoalTargetMinutes min",
                      fontSize = 12.sp,
                      color = LingoTokens.textSecondary
                    )
                  }

                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(78.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                  ) {
                    weeklyBars.forEach { (day, frac) ->
                      Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                      ) {
                        Box(
                          modifier = Modifier
                            .width(16.dp)
                            .height((52.dp * frac).coerceAtLeast(8.dp))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                              if (frac > 0.2f) LingoTokens.brandGradient
                              else Brush.verticalGradient(listOf(LingoTokens.bgSubtle, LingoTokens.bgSubtle))
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                          text = day,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Medium,
                          color = LingoTokens.textSecondary
                        )
                      }
                    }
                  }
                }
              }
            }

            // ============================================================
            // 3. BADGES & ACHIEVEMENTS CAROUSEL
            // ============================================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Badges & achievements",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary
                )
                Text(
                  text = "Certificates",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.brandCyan,
                  modifier = Modifier.clickable(onClick = onOpenAchievementsCertificates)
                )
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                ProfileAchievementBadgeCard(
                  icon = Icons.Outlined.LocalFireDepartment,
                  title = "${userProfile.currentStreak.coerceAtLeast(1)}-day streak",
                  subtitle = "Daily habit",
                  unlocked = userProfile.currentStreak >= 1,
                  accent = LingoTokens.accentGold,
                  onClick = onOpenAchievementsCertificates
                )
                ProfileAchievementBadgeCard(
                  icon = Icons.Outlined.School,
                  title = formatCefrLevelLabel(effectiveCefr),
                  subtitle = "${targetLang.name} track",
                  unlocked = true,
                  accent = LingoTokens.brandCyan,
                  onClick = onOpenAchievementsCertificates
                )
                ProfileAchievementBadgeCard(
                  icon = Icons.Outlined.RecordVoiceOver,
                  title = "${speakingSessions.size} voice talks",
                  subtitle = "Fluency builder",
                  unlocked = speakingSessions.isNotEmpty(),
                  accent = LingoTokens.brandIndigo,
                  onClick = onOpenAchievementsCertificates
                )
                ProfileAchievementBadgeCard(
                  icon = Icons.Outlined.BookmarkBorder,
                  title = "$masteredVocabCount words",
                  subtitle = "Vocab bank",
                  unlocked = masteredVocabCount > 0,
                  accent = LingoTokens.brandCyan,
                  onClick = onNavigateToVocabulary
                )
              }
            }

            // ============================================================
            // 4. LANGUAGE PASSPORT / PORTFOLIO CARD
            // ============================================================
            LingoCleanCard(
              onClick = onOpenLanguagePortfolio,
              contentPadding = 16.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = LingoTokens.bgSubtle,
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  modifier = Modifier.size(44.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Outlined.Badge,
                      contentDescription = null,
                      tint = LingoTokens.brandCyan,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Language Passport",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary
                  )
                  Text(
                    text = "${targetLang.name} · ${formatCefrLevelLabel(effectiveCefr)} · ${if (effectivePronunciationAccuracy > 0) "$effectivePronunciationAccuracy% fluency" else "Portfolio & samples"}",
                    fontSize = 13.sp,
                    color = LingoTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                  contentDescription = null,
                  tint = LingoTokens.textSecondary
                )
              }
            }

            // ============================================================
            // 5. PRO SUBSCRIPTION CARD
            // ============================================================
            LingoCleanCard(
              onClick = onOpenPremiumHub,
              contentPadding = 18.dp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("row_subscription")
            ) {
              val tutorUsage by com.example.data.repository.AccessPolicyManager.tutorUsageState.collectAsState()
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    if (isVerifiedPremium) {
                      LingoProBadge(label = "PRO")
                    }
                    Text(
                      text = if (isVerifiedPremium) "LingoSphere Pro" else "Free Plan",
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = LingoTokens.textPrimary
                    )
                  }
                  Text(
                    text = if (isVerifiedPremium) "Manage" else "See Pro",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.brandCyan
                  )
                }

                Text(
                  text = if (isVerifiedPremium) {
                    val dailyUsedMin = tutorUsage.dailyUsedSeconds / 60
                    val dailyLimitMin = tutorUsage.dailyLimitSeconds / 60
                    val monthlyUsedMin = tutorUsage.monthlyUsedSeconds / 60
                    val monthlyLimitMin = tutorUsage.monthlyLimitSeconds / 60
                    "AI Tutor — Today: $dailyUsedMin of $dailyLimitMin min used · This month: $monthlyUsedMin of $monthlyLimitMin min used (resets ${tutorUsage.nextMonthResetDateLabel})."
                  } else {
                    "AI Tutor, structured lessons & Roleplay Studio require LingoSphere Pro. Free includes 5 books/month, saved flashcards & human exchange."
                  },
                  fontSize = 14.sp,
                  lineHeight = 20.sp,
                  color = LingoTokens.textSecondary
                )

                if (isVerifiedPremium && tutorUsage.isNearMonthlyLimit) {
                  Text(
                    text = "You've used over 80% of this month's 150 AI Tutor minutes. Resets on ${tutorUsage.nextMonthResetDateLabel}.",
                    fontSize = 12.sp,
                    color = LingoTokens.brandAmber,
                    fontWeight = FontWeight.Medium
                  )
                }

                if (!isVerifiedPremium) {
                  LingoPrimaryGradientButton(
                    text = "See Pro",
                    icon = Icons.Outlined.CreditCard,
                    onClick = onOpenPremiumHub,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            }

            // ============================================================
            // 6. CLEAN UNIFORM SETTINGS LIST
            // ============================================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              LingoListRow(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = "My Learning",
                subtitle = "Courses, vocabulary, AI studio & offline packs",
                onClick = { activeProfileSubScreen = ProfileSubScreen.MY_LEARNING },
                iconTint = LingoTokens.brandCyan,
                modifier = Modifier.testTag("row_my_learning")
              )
              LingoListRow(
                icon = Icons.Outlined.Settings,
                title = "Preferences & Settings",
                subtitle = "Goals, reminders, voice accent & theme",
                onClick = { activeProfileSubScreen = ProfileSubScreen.PREFERENCES },
                iconTint = LingoTokens.brandIndigo,
                modifier = Modifier.testTag("row_preferences")
              )
              LingoListRow(
                icon = Icons.Outlined.Security,
                title = "Account & Privacy",
                subtitle = "Online visibility, blocked users & data",
                onClick = { activeProfileSubScreen = ProfileSubScreen.ACCOUNT },
                iconTint = LingoTokens.brandCyan,
                modifier = Modifier.testTag("row_account")
              )
              LingoListRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = "Help & Support",
                subtitle = "FAQs, troubleshooting & feedback",
                onClick = onOpenHelpSupport,
                iconTint = LingoTokens.textSecondary
              )
              LingoListRow(
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                title = "Log out",
                subtitle = "Sign out of your LingoSphere account",
                onClick = { showSignOutConfirmation = true },
                iconTint = LingoCoral
              )
            }
          }

          ProfileSubScreen.MY_LEARNING -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              SettingsIconLinkRow(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                iconTint = LingoCyan,
                title = "My Courses",
                subtitle = "${targetLang.name} Curriculum • ${userProfile.completedLessonIds.size} completed",
                onClick = onNavigateToCourses,
                testTag = "menu_my_courses"
              )
              HorizontalDivider(color = ProfDivider)
              SettingsIconLinkRow(
                icon = Icons.Default.Bookmark,
                iconTint = LingoEmerald,
                title = "Vocabulary & Saved Words",
                subtitle = "$masteredVocabCount mastered words • Flashcards",
                onClick = onNavigateToVocabulary,
                testTag = "menu_vocabulary_saved_words"
              )
              HorizontalDivider(color = ProfDivider)
              SettingsIconLinkRow(
                icon = Icons.Default.HistoryEdu,
                iconTint = LingoAmber,
                title = "Mistake Notebook",
                subtitle = "Review grammar corrections & AI mistake recovery",
                onClick = onOpenMistakeRecovery,
                testTag = "profile_open_ai_memory_btn"
              )
              HorizontalDivider(color = ProfDivider)
              SettingsIconLinkRow(
                icon = Icons.Default.AutoAwesome,
                iconTint = LingoCyan,
                title = "AI Intelligent Studio",
                subtitle = "Generate custom vocabulary decks",
                onClick = onOpenAiIntelligentStudio,
                testTag = "menu_ai_intelligent_studio"
              )
              HorizontalDivider(color = ProfDivider)
              SettingsIconLinkRow(
                icon = Icons.Default.Map,
                iconTint = LingoElectricBlue,
                title = "My Learning Roadmap",
                subtitle = "7-day personalized study & pronunciation roadmap",
                onClick = onOpenLearningRoadmap,
                testTag = "menu_learning_roadmap"
              )
              HorizontalDivider(color = ProfDivider)
              SettingsIconLinkRow(
                icon = Icons.Default.CloudDownload,
                iconTint = LingoCyan,
                title = "Offline Downloads",
                subtitle = "Downloaded: ${downloadedLessonIds.size}/$totalCurriculumLessonsCount lessons",
                onClick = onOpenOfflineDownloads,
                testTag = "menu_offline_downloads"
              )
              HorizontalDivider(color = ProfDivider)
              SettingsIconLinkRow(
                icon = Icons.Default.EmojiEvents,
                iconTint = LingoGold,
                title = "Achievements & Certificates",
                subtitle = "CEFR $effectiveCefr record & Language Portfolio",
                onClick = onOpenAchievementsCertificates,
                testTag = "menu_achievements_certificates"
              )
            }
          }

          ProfileSubScreen.PREFERENCES -> {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              SettingsClickableRow(
                title = "Learning Language",
                subtitle = "${targetLang.flagEmoji} ${targetLang.name} · ${formatCefrLevelLabel(effectiveCefr)}",
                subtitleColor = ProfCyan,
                onClick = {
                  if (onOpenLanguageSelector != null) {
                    onOpenLanguageSelector()
                  } else {
                    showTargetPicker = true
                  }
                },
                testTag = "setting_target_language"
              )

              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Proficiency Level",
                  color = ProfTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.5.sp
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  listOf("Beginner", "Intermediate", "Advanced").forEach { lvl ->
                    val selected = userProfile.proficiencyLevel.equals(lvl, ignoreCase = true)
                    OutlinedButton(
                      onClick = { onProficiencyChanged(lvl) },
                      border = BorderStroke(1.dp, if (selected) ProfCyan else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoElectricBlue.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("proficiency_button_${lvl.lowercase()}")
                    ) {
                      Text(
                        text = lvl,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) ProfCyan else ProfTextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  listOf(10, 15, 20, 30).forEach { mins ->
                    val selected = userProfile.dailyGoalMinutes.toInt() == mins
                    OutlinedButton(
                      onClick = { onDailyGoalChanged(mins) },
                      border = BorderStroke(1.dp, if (selected) LingoEmerald else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoEmerald.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("daily_goal_${mins}m")
                    ) {
                      Text(
                        text = "${mins}m/day",
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) LingoEmerald else ProfTextPrimary,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                }
              }

              HorizontalDivider(color = ProfDivider)

              SettingsClickableRow(
                title = "Native / Explanation Language",
                subtitle = "${nativeLang.name} (${nativeLang.nativeName})",
                subtitleColor = ProfCyan,
                onClick = { showNativePicker = true },
                testTag = "setting_native_language"
              )

              val appUiLang = Language.findByCode(
                accountSettings.appInterfaceLanguageCode.ifBlank { userProfile.appInterfaceLanguageCode }
              )
              SettingsClickableRow(
                title = "App Interface Language",
                subtitle = "${appUiLang.name} (${appUiLang.nativeName})",
                subtitleColor = ProfCyan,
                onClick = { showAppInterfacePicker = true },
                testTag = "setting_app_interface_language"
              )

              HorizontalDivider(color = ProfDivider)

              SettingsIconLinkRow(
                icon = Icons.Default.Notifications,
                iconTint = LingoAmber,
                title = "Notifications & Reminders",
                subtitle = "Daily streak reminders & word alerts",
                onClick = onOpenNotifications,
                testTag = "open_notifications_row"
              )

              HorizontalDivider(color = ProfDivider)

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Floating Continue button",
                      color = ProfTextPrimary,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 14.sp
                    )
                    Text(
                      text = "Movable quick-resume button across main tabs",
                      color = ProfTextSecondary,
                      fontSize = 12.sp
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Switch(
                    checked = floatingContinuePrefs.enabled,
                    onCheckedChange = onToggleFloatingContinue,
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = ProfCyan,
                      uncheckedThumbColor = ProfTextSecondary,
                      uncheckedTrackColor = ProfElevatedBg
                    ),
                    modifier = Modifier.testTag("setting_floating_continue_toggle")
                  )
                }

                OutlinedButton(
                  onClick = {
                    onResetFloatingContinuePosition()
                    Toast.makeText(context, "Floating Continue button position reset.", Toast.LENGTH_SHORT).show()
                  },
                  border = BorderStroke(1.dp, ProfBorder),
                  colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ProfElevatedBg,
                    contentColor = ProfCyan
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("setting_floating_continue_reset_btn")
                ) {
                  Text(
                    text = "Reset position",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              HorizontalDivider(color = ProfDivider)

              // AI Voice & Conversation Partner Preferences (15 Voices + 12 Partners + Correction Mode + Consent)
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Default AI Conversation Partner",
                  color = ProfTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.5.sp
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                ) {
                  AiConversationPartner.ALL_PARTNERS.forEach { partner ->
                    val selected = accountSettings.defaultPartnerId.equals(partner.id, ignoreCase = true)
                    OutlinedButton(
                      onClick = {
                        context.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
                          .edit()
                          .putString("selected_ai_partner_id", partner.id)
                          .putString("selected_tutor_voice_id", partner.defaultVoice.id)
                          .apply()
                        onUpdateAccountSettings(
                          accountSettings.copy(
                            defaultPartnerId = partner.id,
                            preferredVoiceId = partner.defaultVoice.id
                          )
                        )
                      },
                      border = BorderStroke(1.dp, if (selected) ProfCyan else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoElectricBlue.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("pref_partner_${partner.id}")
                    ) {
                      Text(
                        text = partner.name,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) ProfCyan else ProfTextPrimary
                      )
                    }
                  }
                }

                Text(
                  text = "Preferred AI Voice (15 Expressive Voices)",
                  color = ProfTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                ) {
                  TutorVoice.entries.forEach { voice ->
                    val selected = accountSettings.preferredVoiceId.equals(voice.id, ignoreCase = true)
                    OutlinedButton(
                      onClick = {
                        context.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
                          .edit()
                          .putString("selected_tutor_voice_id", voice.id)
                          .apply()
                        onUpdateAccountSettings(accountSettings.copy(preferredVoiceId = voice.id))
                      },
                      border = BorderStroke(1.dp, if (selected) ProfCyan else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoElectricBlue.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("pref_voice_${voice.id}")
                    ) {
                      Text(
                        text = "${voice.voiceName} (${voice.styleTag})",
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) ProfCyan else ProfTextPrimary
                      )
                    }
                  }
                }

                Text(
                  text = "Speaking Speed (${accountSettings.tutorSpeechSpeed}x) & Preferred Accent",
                  color = ProfTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  listOf(0.75f, 1.0f, 1.25f).forEach { spd ->
                    val selected = accountSettings.tutorSpeechSpeed == spd
                    OutlinedButton(
                      onClick = {
                        onUpdateAccountSettings(accountSettings.copy(tutorSpeechSpeed = spd))
                      },
                      border = BorderStroke(1.dp, if (selected) ProfCyan else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoElectricBlue.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                      Text("${spd}x", fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) ProfCyan else ProfTextPrimary)
                    }
                  }
                }
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                ) {
                  listOf("All", "US", "UK", "Australian", "Neutral").forEach { acc ->
                    val selected = accountSettings.preferredAccent.equals(acc, ignoreCase = true)
                    OutlinedButton(
                      onClick = {
                        onUpdateAccountSettings(accountSettings.copy(preferredAccent = acc))
                      },
                      border = BorderStroke(1.dp, if (selected) ProfCyan else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoElectricBlue.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text(acc, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) ProfCyan else ProfTextPrimary)
                    }
                  }
                }

                Text(
                  text = "Language Correction Mode",
                  color = ProfTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                ) {
                  CorrectionMode.entries.forEach { mode ->
                    val selected = accountSettings.correctionModeId.equals(mode.id, ignoreCase = true)
                    OutlinedButton(
                      onClick = {
                        onUpdateAccountSettings(accountSettings.copy(correctionModeId = mode.id))
                      },
                      border = BorderStroke(1.dp, if (selected) LingoEmerald else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoEmerald.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("pref_correction_${mode.id}")
                    ) {
                      Text(
                        text = mode.title,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) LingoEmerald else ProfTextPrimary
                      )
                    }
                  }
                }

                SettingsSwitchRow(
                  title = "Voice Recording Consent",
                  subtitle = "Allow saving raw voice audio for replay & pronunciation review (off by default)",
                  checked = accountSettings.voiceRecordingConsent,
                  onCheckedChange = { checked ->
                    onUpdateAccountSettings(accountSettings.copy(voiceRecordingConsent = checked))
                  },
                  testTag = "toggle_voice_recording_consent"
                )
              }

              HorizontalDivider(color = ProfDivider)

              // Theme Mode Selector
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "App Theme",
                  color = ProfTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  listOf("Dark", "Light", "System").forEach { mode ->
                    val selected = accountSettings.themeMode.equals(mode, ignoreCase = true)
                    OutlinedButton(
                      onClick = {
                        val updated = accountSettings.copy(themeMode = mode)
                        onUpdateAccountSettings(updated)
                        LingoThemeController.themeMode = mode
                      },
                      border = BorderStroke(1.dp, if (selected) ProfCyan else ProfBorder),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) LingoElectricBlue.copy(alpha = 0.22f) else ProfElevatedBg
                      ),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                      Text(mode, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) ProfCyan else ProfTextPrimary)
                    }
                  }
                }
              }
            }
          }

          ProfileSubScreen.ACCOUNT -> {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              SettingsSwitchRow(
                title = "Appear Offline",
                subtitle = "Hide your live green status in Connect",
                checked = !(isOnline && accountSettings.isOnlineVisible),
                onCheckedChange = { appearOffline ->
                  val visible = !appearOffline
                  onUpdateAccountSettings(accountSettings.copy(isOnlineVisible = visible))
                  onUpdateCommunitySettings(
                    userProfile.displayName,
                    currentCountry,
                    currentFlag,
                    currentBio,
                    currentInterests,
                    visible,
                    allowCalls
                  )
                },
                testTag = "toggle_online_visibility"
              )

              SettingsSwitchRow(
                title = "Do Not Disturb",
                subtitle = "Pause incoming voice & video call notifications",
                checked = accountSettings.doNotDisturb,
                onCheckedChange = { dnd ->
                  val effectiveAllow = !dnd && accountSettings.whoCanCallMe != "nobody"
                  onUpdateAccountSettings(
                    accountSettings.copy(
                      doNotDisturb = dnd,
                      allowIncomingCalls = effectiveAllow
                    )
                  )
                  onUpdateCommunitySettings(
                    userProfile.displayName,
                    currentCountry,
                    currentFlag,
                    currentBio,
                    currentInterests,
                    isOnline,
                    effectiveAllow
                  )
                },
                testTag = "toggle_do_not_disturb"
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Who can call me",
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Choose who is allowed to start a live voice or video call with you.",
                  color = Color(0xFFA8B3CF),
                  fontSize = 12.sp
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  listOf(
                    "everyone" to "Everyone",
                    "friends" to "Friends only",
                    "nobody" to "Nobody"
                  ).forEach { (key, label) ->
                    val selected = accountSettings.whoCanCallMe == key
                    Surface(
                      onClick = {
                        val effectiveAllow = key != "nobody" && !accountSettings.doNotDisturb
                        onUpdateAccountSettings(
                          accountSettings.copy(
                            whoCanCallMe = key,
                            allowIncomingCalls = effectiveAllow
                          )
                        )
                        onUpdateCommunitySettings(
                          userProfile.displayName,
                          currentCountry,
                          currentFlag,
                          currentBio,
                          currentInterests,
                          isOnline,
                          effectiveAllow
                        )
                      },
                      shape = RoundedCornerShape(14.dp),
                      color = if (selected) LingoCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                      border = BorderStroke(
                        1.dp,
                        if (selected) LingoCyan else Color.White.copy(alpha = 0.10f)
                      ),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("who_can_call_$key")
                    ) {
                      Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text(
                          text = label,
                          color = if (selected) LingoCyan else Color(0xFFA8B3CF),
                          fontSize = 12.sp,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }
                }
              }

              HorizontalDivider(color = ProfDivider)

              SettingsIconLinkRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                iconTint = LingoCyan,
                title = "Help Center & Support",
                subtitle = "FAQs, troubleshooting & contact support",
                onClick = onOpenHelpSupport,
                testTag = "menu_help_support"
              )

              SettingsIconLinkRow(
                icon = Icons.Default.Policy,
                iconTint = LingoElectricBlue,
                title = "Privacy Policy",
                subtitle = "Read our data collection and privacy policy",
                onClick = { showPrivacyPolicy = true },
                testTag = "menu_privacy_policy"
              )

              SettingsIconLinkRow(
                icon = Icons.Default.Security,
                iconTint = LingoEmerald,
                title = "Terms of Service",
                subtitle = "Review terms and conditions",
                onClick = { showTermsOfService = true },
                testTag = "menu_terms_service"
              )

              HorizontalDivider(color = ProfDivider)

              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedButton(
                  onClick = { showSignOutConfirmation = true },
                  border = BorderStroke(1.dp, LingoCoral),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("sign_out_button")
                ) {
                  Icon(
                    Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = LingoCoral,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Sign Out",
                    color = LingoCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                }

                Button(
                  onClick = { showDeleteConfirmation = true },
                  colors = ButtonDefaults.buttonColors(containerColor = LingoCoral),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("delete_account_button")
                ) {
                  Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // ============================================================
  // MODALS, PICKERS & BOTTOM SHEETS
  // ============================================================
  if (showNativePicker) {
    LanguagePickerDialog(
      title = "Change Native / Explanation Language",
      currentLanguageCode = userProfile.nativeLanguageCode,
      onLanguageSelected = { selected ->
        onNativeLanguageChanged(selected.code)
      },
      onDismiss = { showNativePicker = false }
    )
  }

  if (showAppInterfacePicker) {
    LanguagePickerDialog(
      title = "Change App Interface Language",
      currentLanguageCode = accountSettings.appInterfaceLanguageCode.ifBlank { userProfile.appInterfaceLanguageCode },
      onLanguageSelected = { selected ->
        onUpdateAccountSettings(accountSettings.copy(appInterfaceLanguageCode = selected.code))
      },
      onDismiss = { showAppInterfacePicker = false }
    )
  }

  if (showTargetPicker) {
    ChangeLearningLanguageBottomSheet(
      userProfile = userProfile,
      cefrStageLabel = effectiveCefr,
      onLanguageAndLevelSelected = { code, cefr ->
        onLanguageAndLevelChanged(code, cefr)
      },
      onApplyPlacementEvaluation = { code, eval ->
        onApplyPlacementEvaluation(code, eval)
      },
      onDismiss = { showTargetPicker = false }
    )
  }

  if (showPhotoOptionsDialog) {
    AlertDialog(
      onDismissRequest = { showPhotoOptionsDialog = false },
      title = {
        Text("Profile Photo", fontWeight = FontWeight.Bold)
      },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          LingoAvatar(
            name = userProfile.displayName.ifBlank { "L" },
            userId = userProfile.userId,
            size = 84.dp,
            customImageUrl = currentAvatarUrl,
            isPro = isVerifiedPremium
          )

          Text(
            text = "Upload a photo from your gallery or take a photo with your camera. Changes sync immediately to your account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Button(
            onClick = {
              showPhotoOptionsDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = LingoElectricBlue,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pick_gallery_photo_button")
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Choose from Gallery / Photos",
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }

          OutlinedButton(
            onClick = {
              showPhotoOptionsDialog = false
              val hasCamPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
              ) == PackageManager.PERMISSION_GRANTED
              if (hasCamPerm) {
                cameraCaptureLauncher.launch(null)
              } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
              }
            },
            border = BorderStroke(1.dp, LingoCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("take_camera_photo_button")
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Take Photo with Camera",
              color = LingoCyan,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }

          if (currentAvatarUrl.isNotBlank()) {
            TextButton(
              onClick = {
                showPhotoOptionsDialog = false
                onUpdateProfileAvatar("")
                Toast.makeText(context, "Profile photo removed.", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_remove_photo_button")
            ) {
              Icon(Icons.Default.DeleteForever, contentDescription = null, tint = LingoCoral, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Remove Current Photo",
                color = LingoCoral,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showPhotoOptionsDialog = false }) {
          Text("Close")
        }
      }
    )
  }

  if (showEditProfileModal) {
    EditLearnerProfileDialog(
      userId = userProfile.userId,
      currentAvatarUrl = currentAvatarUrl,
      initialName = userProfile.displayName,
      initialCountry = currentCountry,
      initialFlag = currentFlag,
      initialBio = currentBio,
      initialInterests = currentInterests,
      onOpenPhotoPicker = {
        showEditProfileModal = false
        showPhotoOptionsDialog = true
      },
      onRemovePhoto = {
        onUpdateProfileAvatar("")
      },
      onDismiss = { showEditProfileModal = false },
      onSave = { name, country, flag, bio, interests ->
        onUpdateCommunitySettings(name, country, flag, bio, interests, isOnline, allowCalls)
        showEditProfileModal = false
      }
    )
  }

  if (showSignOutConfirmation) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirmation = false },
      title = {
        Text("Log Out of LingoSphere?", fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          text = "Your learning progress, streak, and preferences are safely synced to your cloud account. You can sign back in anytime.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirmation = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoElectricBlue,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("confirm_sign_out_button")
        ) {
          Text("Log Out", maxLines = 1, softWrap = false)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirmation = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showClearHistoryConfirmation) {
    AlertDialog(
      onDismissRequest = { showClearHistoryConfirmation = false },
      title = {
        Text("Clear Learning History & AI Memory?", fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          text = "This will clear your speaking session transcripts, pronunciation diagnostics, and mistake notebook while keeping your account, level, streak, and flashcards intact.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showClearHistoryConfirmation = false
            onClearLearningHistory()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoAmber,
            contentColor = Color.Black
          ),
          modifier = Modifier.testTag("confirm_clear_history_button")
        ) {
          Text("Clear History", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearHistoryConfirmation = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showDeleteConfirmation) {
    DeleteAccountConfirmationDialog(
      userEmail = userEmail,
      onDismiss = { showDeleteConfirmation = false },
      onConfirmDelete = {
        showDeleteConfirmation = false
        onDeleteAccount()
      }
    )
  }

  if (showPrivacyPolicy) {
    PrivacyPolicyDialog(
      onDismiss = { showPrivacyPolicy = false },
      onOpenBrowser = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvePrivacyPolicyUrl(context)))
        context.startActivity(intent)
      }
    )
  }

  if (showTermsOfService) {
    TermsOfServiceDialog(
      onDismiss = { showTermsOfService = false },
      onOpenBrowser = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolveTermsOfServiceUrl(context)))
        context.startActivity(intent)
      }
    )
  }

  if (showUpgradeBottomSheet) {
    SubscriptionManagementBottomSheet(
      userProfile = userProfile,
      billingRepository = billingRepository,
      onDismiss = { showUpgradeBottomSheet = false },
      onOpenFullHub = {
        showUpgradeBottomSheet = false
        onOpenPremiumHub()
      },
      onPurchase = { productId, isTrial ->
        val activity = context.findActivity()
        if (billingRepository != null) {
          billingRepository.launchBillingFlow(activity, productId, isFreeTrial = isTrial)
        } else {
          try {
            val playSubsUri = Uri.parse(
              "https://play.google.com/store/account/subscriptions?sku=$productId&package=${context.packageName}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, playSubsUri))
          } catch (_: Exception) {
            Toast.makeText(
              context,
              "Google Play Billing is required to complete this subscription.",
              Toast.LENGTH_SHORT
            ).show()
          }
        }
      },
      onRestorePurchases = {
        if (billingRepository != null) {
          billingRepository.restorePurchases { _, msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
          }
        } else {
          Toast.makeText(context, "Checking Google Play purchase history...", Toast.LENGTH_SHORT).show()
        }
      },
      onCancelSubscriptionToFree = {
        onCancelSubscriptionToFree()
        showUpgradeBottomSheet = false
      }
    )
  }
}

@Composable
private fun PremiumStatCard(
  icon: ImageVector,
  iconTint: Color,
  value: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ProfCardBg),
    border = BorderStroke(1.dp, ProfBorder),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(iconTint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = value,
          fontSize = 16.sp,
          fontWeight = FontWeight.ExtraBold,
          color = ProfTextPrimary,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = label,
          fontSize = 12.sp,
          color = ProfSubtleBlue,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun CompactStatTile(
  icon: String,
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = ProfElevatedBg,
    border = BorderStroke(1.dp, ProfDivider),
    modifier = modifier
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
      Text(text = icon, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = ProfTextPrimary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = label,
        fontSize = 12.sp,
        color = ProfSubtleBlue,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SettingsGroupCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  testTag: String,
  content: @Composable () -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = ProfCardBg),
    border = BorderStroke(1.dp, ProfBorder),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ProfElevatedBg),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ProfCyan,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ProfTextPrimary
          )
          Text(
            text = subtitle,
            fontSize = 12.sp,
            color = ProfTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      HorizontalDivider(color = ProfDivider)

      content()
    }
  }
}

@Composable
private fun SettingsClickableRow(
  title: String,
  subtitle: String,
  subtitleColor: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(vertical = 8.dp)
      .testTag(testTag)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = ProfTextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
      )
      Text(
        text = subtitle,
        color = subtitleColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )
    }
    Icon(
      Icons.AutoMirrored.Filled.ArrowForward,
      contentDescription = null,
      tint = ProfTextSecondary,
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 10.dp)
    ) {
      Text(
        text = title,
        color = ProfTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = subtitle,
        color = ProfTextSecondary,
        fontSize = 12.sp
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = LingoElectricBlue,
        uncheckedThumbColor = Color(0xFF94A3B8),
        uncheckedTrackColor = ProfElevatedBg
      )
    )
  }
}

@Composable
private fun ProfileStatTile(
  icon: ImageVector,
  accent: Color,
  value: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = LingoTokens.bgSubtle,
    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
    modifier = modifier
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = value,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = LingoTokens.textSecondary,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
private fun ProfileAchievementBadgeCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  unlocked: Boolean,
  accent: Color,
  onClick: () -> Unit
) {
  LingoCleanCard(
    onClick = onClick,
    contentPadding = 14.dp,
    modifier = Modifier.width(136.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = LingoTokens.bgSubtle,
        border = BorderStroke(
          1.dp,
          if (unlocked) accent.copy(alpha = 0.4f) else LingoTokens.borderSubtle
        ),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (unlocked) accent else LingoTokens.textSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
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
        fontSize = 12.sp,
        color = LingoTokens.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SettingsIconLinkRow(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String
) {
  LingoListRow(
    icon = icon,
    title = title,
    subtitle = subtitle,
    onClick = onClick,
    iconTint = iconTint,
    modifier = Modifier.testTag(testTag)
  )
}
