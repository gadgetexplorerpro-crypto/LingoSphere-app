package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.FeatureQuotaStatus
import com.example.ai.GeminiCostAndQuotaManager
import com.example.ai.GeminiFeatureCategory
import com.example.ai.GeminiModelTier
import com.example.ai.GeminiTutorService
import com.example.ai.QuotaWarningLevel
import com.example.data.model.CurriculumLesson
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.SpeakingSession
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.repository.BillingAvailability
import com.example.data.repository.BillingPurchaseStatus
import com.example.data.repository.BillingRepository
import com.example.data.repository.PremiumCoachingPlanResult
import com.example.data.repository.PremiumCustomDeck
import com.example.data.repository.PremiumGeneratedVocabCard
import com.example.data.repository.WeeklyLearningReportSummary
import com.example.speech.SpeechManager
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoProBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.findActivity
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoGold
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

enum class PremiumHubSection {
  SUBSCRIPTION,
  USAGE_LIMITS,
  AI_STUDIO_DECKS,
  LEARNING_ROADMAP,
  OFFLINE_DOWNLOADS,
  ACHIEVEMENTS_CERTIFICATES,
  ADMIN_CONSOLE
}

// Unified LingoSphere Dark Palette
private val SubNavyBgTop = LingoTokens.bgCanvas
private val SubNavyBgMid = LingoTokens.bgCanvas
private val SubNavyBgBottom = LingoTokens.bgCanvas
private val SubCardSurface = LingoTokens.bgElevated
private val SubCardSurfaceElevated = LingoTokens.bgSubtle
private val SubCardBorder = LingoTokens.innerHighlight
private val SubTextPrimary = LingoTokens.textPrimary
private val SubTextSecondary = LingoTokens.textSecondary
private val SubTextMuted = LingoTokens.textSecondary

@Composable
fun PremiumHubScreen(
  userProfile: UserProfile,
  billingRepository: BillingRepository?,
  speechManager: SpeechManager,
  curriculumLessons: List<CurriculumLesson> = emptyList(),
  downloadedLessonIds: Set<String> = emptySet(),
  pendingOfflineSyncCount: Int = 0,
  flashcards: List<FlashcardProgress> = emptyList(),
  speakingSessions: List<SpeakingSession> = emptyList(),
  mistakeEntries: List<MistakeNotebookEntry> = emptyList(),
  weeklyReport: WeeklyLearningReportSummary? = null,
  customProDecks: List<PremiumCustomDeck> = emptyList(),
  cefrStageLabel: String = "",
  initialSection: PremiumHubSection = PremiumHubSection.SUBSCRIPTION,
  onActivatePlan: (tier: String, token: String) -> Unit = { _, _ -> },
  onCancelSubscription: () -> Unit = {},
  onSaveCustomProDeck: (PremiumCustomDeck) -> Unit = {},
  onDownloadAllLessons: (List<CurriculumLesson>) -> Unit = {},
  onSyncOfflineNow: () -> Unit = {},
  onStartVoiceCall: () -> Unit = {},
  onStartVideoTutorCall: () -> Unit = {},
  onBack: () -> Unit = {}
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)

  val usageSnapshot by GeminiCostAndQuotaManager.usageState.collectAsState()
  val adminConfig by GeminiCostAndQuotaManager.adminConfigState.collectAsState()

  val isVerifiedPaid = userProfile.isEntitlementVerified
  val activeTierCode = if (isVerifiedPaid) userProfile.subscriptionTier else "free"

  val quotaStatuses = remember(usageSnapshot, activeTierCode, isVerifiedPaid) {
    GeminiCostAndQuotaManager.getAllFeatureQuotaStatuses(
      tierCode = activeTierCode,
      isEntitlementVerified = isVerifiedPaid
    )
  }

  val voiceQuota = quotaStatuses.first { it.feature == GeminiFeatureCategory.AI_VOICE_TUTOR }
  val videoQuota = quotaStatuses.first { it.feature == GeminiFeatureCategory.AI_VIDEO_TUTOR }
  val activeWarnings = remember(quotaStatuses) {
    quotaStatuses.mapNotNull { status ->
      status.friendlyWarningMessage?.let { msg -> status.warningLevel to msg }
    }
  }

  var selectedPlanId by remember(userProfile.subscriptionTier, isVerifiedPaid) {
    mutableStateOf(
      if (isVerifiedPaid && userProfile.subscriptionTier == "premium_yearly") {
        SubscriptionPlan.PRODUCT_ID_YEARLY
      } else {
        SubscriptionPlan.PRODUCT_ID_MONTHLY
      }
    )
  }

  // Expandable "My AI Usage" section & legal info dialogs
  var isMyAiUsageExpanded by remember(initialSection) {
    mutableStateOf(initialSection == PremiumHubSection.USAGE_LIMITS)
  }
  var activeLegalDialog by remember { mutableStateOf<String?>(null) }


  val isLearningToolSection = initialSection in setOf(
    PremiumHubSection.AI_STUDIO_DECKS,
    PremiumHubSection.LEARNING_ROADMAP,
    PremiumHubSection.OFFLINE_DOWNLOADS,
    PremiumHubSection.ACHIEVEMENTS_CERTIFICATES
  )

  val topBarTitle = when (initialSection) {
    PremiumHubSection.AI_STUDIO_DECKS -> "AI Intelligent Studio"
    PremiumHubSection.LEARNING_ROADMAP -> "My Learning Roadmap"
    PremiumHubSection.OFFLINE_DOWNLOADS -> "Offline Language Packs"
    PremiumHubSection.ACHIEVEMENTS_CERTIFICATES -> "Achievements & CEFR Progress"
    PremiumHubSection.USAGE_LIMITS -> "Subscription & AI Usage Limits"
    else -> "LingoSphere Subscriptions"
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("premium_hub_screen"),
    color = LingoTokens.bgCanvas
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .lingoAmbientBackdrop()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter
    ) {
      val horizontalPad = if (maxWidth < 360.dp) 16.dp else LingoTokens.screenMargin

      Column(
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxSize()
      ) {
        // Minimal Top App Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(48.dp)
                .testTag("premium_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = LingoTokens.textPrimary
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
              Text(
                text = if (initialSection != PremiumHubSection.SUBSCRIPTION) topBarTitle else "LingoSphere",
                color = LingoTokens.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = if (initialSection != PremiumHubSection.SUBSCRIPTION) {
                  if (isVerifiedPaid) userProfile.planDisplayName else "Free Plan"
                } else if (isVerifiedPaid) {
                  "Verified · ${userProfile.planDisplayName}"
                } else {
                  "Subscriptions · Free Plan"
                },
                color = if (isVerifiedPaid) LingoTokens.accentGold else LingoTokens.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          if (isVerifiedPaid) {
            LingoProBadge(label = "PRO ACTIVE")
          } else {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = LingoTokens.bgElevated,
              border = BorderStroke(1.dp, LingoTokens.innerHighlight)
            ) {
              Text(
                text = "FREE PLAN",
                color = LingoTokens.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }

        if (isLearningToolSection) {
          LearningToolsDedicatedView(
            section = initialSection,
            userProfile = userProfile,
            targetLang = targetLang,
            nativeLang = nativeLang,
            speechManager = speechManager,
            curriculumLessons = curriculumLessons,
            downloadedLessonIds = downloadedLessonIds,
            pendingOfflineSyncCount = pendingOfflineSyncCount,
            flashcards = flashcards,
            speakingSessions = speakingSessions,
            mistakeEntries = mistakeEntries,
            weeklyReport = weeklyReport,
            customProDecks = customProDecks,
            cefrStageLabel = cefrStageLabel,
            onSaveCustomProDeck = onSaveCustomProDeck,
            onDownloadAllLessons = onDownloadAllLessons,
            onSyncOfflineNow = onSyncOfflineNow,
            horizontalPad = horizontalPad
          )
        } else {
          // ===================================================================
          // CLEAN, MODERN GOOGLE PLAY SUBSCRIPTION PAGE
          // ===================================================================
          val purchaseStatus = billingRepository?.purchaseStatus?.collectAsState()?.value ?: BillingPurchaseStatus.IDLE
          val billingAvailability = billingRepository?.billingAvailability?.collectAsState()?.value
            ?: BillingAvailability.UNAVAILABLE_PREVIEW
          val billingStatusMsg = billingRepository?.statusMessage?.collectAsState()?.value
          val billingErrMsg = billingRepository?.billingError?.collectAsState()?.value
          val isAutoRenewing = (billingRepository?.isAutoRenewing?.collectAsState()?.value ?: true) &&
            userProfile.subscriptionAutoRenewing
          val planPricingMap = billingRepository?.planPricingMap?.collectAsState()?.value
          val isTrialEligible = userProfile.isEligibleForFreeTrial

          androidx.compose.runtime.LaunchedEffect(userProfile.hasUsedFreeTrial, userProfile.isFreeTrial) {
            billingRepository?.syncUserTrialEligibility(userProfile.hasUsedFreeTrial || userProfile.isFreeTrial)
          }

          val monthlyPricing = remember(planPricingMap, userProfile.hasUsedFreeTrial) {
            planPricingMap?.get(SubscriptionPlan.PRODUCT_ID_MONTHLY)
              ?: billingRepository?.getPlanPricingInfo(SubscriptionPlan.PRODUCT_ID_MONTHLY)
              ?: BillingRepository.defaultCatalogPricing(userProfile.hasUsedFreeTrial)[SubscriptionPlan.PRODUCT_ID_MONTHLY]
          }
          val yearlyPricing = remember(planPricingMap, userProfile.hasUsedFreeTrial) {
            planPricingMap?.get(SubscriptionPlan.PRODUCT_ID_YEARLY)
              ?: billingRepository?.getPlanPricingInfo(SubscriptionPlan.PRODUCT_ID_YEARLY)
              ?: BillingRepository.defaultCatalogPricing(userProfile.hasUsedFreeTrial)[SubscriptionPlan.PRODUCT_ID_YEARLY]
          }
          val yearlySavingsPct = (yearlyPricing?.savingsPercent ?: 33).coerceAtLeast(0)
          val yearlyEqFormatted = yearlyPricing?.equivalentMonthlyFormatted ?: "$6.67/month"

          val isMonthlyLiveTrialAvailable = false
          val isYearlyLiveTrialAvailable = false

          val isMonthlyActive = isVerifiedPaid && userProfile.subscriptionTier != "premium_yearly"
          val isYearlyActive = isVerifiedPaid && userProfile.subscriptionTier == "premium_yearly"
          val isPendingPurchase = purchaseStatus == BillingPurchaseStatus.PENDING

          val launchPlanPurchase: (String, Boolean) -> Unit = { targetProductId, _ ->
            val activity = context.findActivity()
            if (billingRepository != null) {
              billingRepository.launchBillingFlow(
                activity = activity,
                productId = targetProductId,
                isFreeTrial = false
              )
            } else {
              Toast.makeText(
                context,
                "Google Play Billing is required on a Play-certified device to subscribe.",
                Toast.LENGTH_LONG
              ).show()
            }
          }

          Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = horizontalPad, vertical = 16.dp)
          ) {
            // 1. PREMIUM HERO + 3 CONCISE BENEFITS
            Surface(
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              color = LingoTokens.bgElevated,
              border = BorderStroke(1.dp, LingoTokens.innerHighlight),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("subscription_hero_section")
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(20.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(LingoTokens.brandGradient)
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.AutoAwesome,
                      contentDescription = null,
                      tint = LingoTokens.onGradientInk,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                  LingoProBadge(label = "PRO")
                }

                Text(
                  text = "LingoSphere Pro",
                  color = LingoTokens.textPrimary,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 26.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag("subscription_hero_heading")
                )

                Text(
                  text = "Speak with confidence using unlimited AI tutoring, live feedback, and offline learning packs.",
                  color = LingoTokens.textSecondary,
                  fontSize = 14.sp,
                  lineHeight = 20.sp,
                  textAlign = TextAlign.Center
                )

                HorizontalDivider(color = LingoTokens.innerHighlight)

                Column(
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  listOf(
                    Triple(Icons.Outlined.GraphicEq, "180m/mo AI Voice & 45m/mo AI Video", "Natural live tutoring with Emma"),
                    Triple(Icons.Outlined.Verified, "40/day Grammar & Pronunciation", "Instant syllable and phrasing feedback"),
                    Triple(Icons.Outlined.CloudDownload, "Offline Packs & CEFR Certificates", "Study anywhere and export milestones")
                  ).forEach { (icon, title, sub) ->
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                          .size(36.dp)
                          .clip(RoundedCornerShape(10.dp))
                          .background(LingoTokens.bgSubtle)
                      ) {
                        Icon(
                          imageVector = icon,
                          contentDescription = null,
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = title,
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp
                        )
                        Text(
                          text = sub,
                          color = LingoTokens.textSecondary,
                          fontSize = 12.sp
                        )
                      }
                    }
                  }
                }
              }
            }

            // Play Billing Availability Notice (honest message in emulator/preview environments)
            if (billingRepository != null && billingAvailability == BillingAvailability.UNAVAILABLE_PREVIEW) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = LingoTokens.bgElevated,
                border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("play_billing_preview_notice")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = LingoTokens.brandCyan,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Showing US reference prices. Live localized prices & checkout load from Google Play on Play-certified devices.",
                    color = LingoTokens.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }

            // Friendly Quota Warning Banners (only shown when usage crosses 80% / 95%)
            activeWarnings.forEach { (level, message) ->
              val isCritical = level == QuotaWarningLevel.WARNING_95_PERCENT || level == QuotaWarningLevel.LIMIT_REACHED
              val accentColor = if (isCritical) LingoCoral else LingoTokens.accentGold
              Card(
                shape = RoundedCornerShape(LingoTokens.cardRadius),
                colors = CardDefaults.cardColors(containerColor = LingoTokens.bgElevated),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("quota_warning_banner")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(16.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Usage Warning",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = if (isCritical) "95% Usage Notice" else "80% Usage Reminder",
                      color = accentColor,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp
                    )
                    Text(
                      text = message,
                      color = LingoTokens.textPrimary,
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }

            // Billing Transaction Feedback Banner (Pending / Error / Restored)
            if (purchaseStatus == BillingPurchaseStatus.PENDING ||
              !billingStatusMsg.isNullOrBlank() ||
              !billingErrMsg.isNullOrBlank()
            ) {
              val isErr = purchaseStatus == BillingPurchaseStatus.FAILED || !billingErrMsg.isNullOrBlank()
              val statusTint = when {
                purchaseStatus == BillingPurchaseStatus.VERIFIED_ACTIVE -> LingoEmerald
                isErr -> LingoCoral
                else -> LingoTokens.brandCyan
              }
              Card(
                shape = RoundedCornerShape(LingoTokens.cardRadius),
                colors = CardDefaults.cardColors(containerColor = statusTint.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, statusTint.copy(alpha = 0.45f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("subscription_billing_feedback_card")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(14.dp)
                ) {
                  if (purchaseStatus == BillingPurchaseStatus.PENDING) {
                    CircularProgressIndicator(
                      color = statusTint,
                      strokeWidth = 2.dp,
                      modifier = Modifier.size(18.dp)
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Outlined.Info,
                      contentDescription = null,
                      tint = statusTint,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = billingStatusMsg ?: billingErrMsg ?: "Connecting to Google Play Billing...",
                    color = LingoTokens.textPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }

            // Phase 3 & 5: Grace Period / On-Hold Payment Issue Banner
            val serverEntitlement by com.example.data.repository.AccessPolicyManager.entitlementState.collectAsState()
            if (serverEntitlement.status == com.example.data.repository.EntitlementStatus.PRO_GRACE ||
              serverEntitlement.status == com.example.data.repository.EntitlementStatus.PRO_ON_HOLD
            ) {
              Card(
                shape = RoundedCornerShape(LingoTokens.cardRadius),
                colors = CardDefaults.cardColors(containerColor = LingoTokens.accentGold.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, LingoTokens.accentGold),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("pro_grace_payment_banner")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.padding(14.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Payment issue",
                    tint = LingoTokens.accentGold,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = "Payment issue — update your payment method in Google Play to keep Pro active",
                    color = LingoTokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                  )
                  OutlinedButton(
                    onClick = {
                      val activity = context.findActivity()
                      billingRepository?.openGooglePlaySubscriptionManagement(activity, selectedPlanId)
                    },
                    border = BorderStroke(1.dp, LingoTokens.accentGold),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text("Update", color = LingoTokens.accentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            // Phase 5: Monthly / Yearly Segmented Control + "Continue with Pro" Primary Button
            val isYearlySelected = selectedPlanId == SubscriptionPlan.PRODUCT_ID_YEARLY
            val selectedFormattedPrice = if (isYearlySelected) {
              yearlyPricing?.formattedPrice ?: "$79.99 / year"
            } else {
              monthlyPricing?.formattedPrice ?: "$9.99 / month"
            }
            Surface(
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              color = LingoTokens.bgElevated,
              border = BorderStroke(1.dp, LingoTokens.innerHighlight),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("subscription_plan_cycle_toggle")
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isYearlySelected) LingoTokens.brandCyan.copy(alpha = 0.18f) else LingoTokens.bgSubtle,
                    border = BorderStroke(
                      1.dp,
                      if (!isYearlySelected) LingoTokens.brandCyan else LingoTokens.innerHighlight
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .clickable { selectedPlanId = SubscriptionPlan.PRODUCT_ID_MONTHLY }
                      .testTag("toggle_monthly_cycle_btn")
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Text(
                        text = "Monthly",
                        color = if (!isYearlySelected) LingoTokens.brandCyan else LingoTokens.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                      )
                    }
                  }
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isYearlySelected) LingoTokens.accentGold.copy(alpha = 0.18f) else LingoTokens.bgSubtle,
                    border = BorderStroke(
                      1.dp,
                      if (isYearlySelected) LingoTokens.accentGold else LingoTokens.innerHighlight
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .clickable { selectedPlanId = SubscriptionPlan.PRODUCT_ID_YEARLY }
                      .testTag("toggle_yearly_cycle_btn")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center,
                      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                    ) {
                      Text(
                        text = "Yearly",
                        color = if (isYearlySelected) LingoTokens.accentGold else LingoTokens.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = LingoTokens.accentGold.copy(alpha = 0.22f)
                      ) {
                        Text(
                          text = "Save $yearlySavingsPct%",
                          color = LingoTokens.accentGold,
                          fontWeight = FontWeight.ExtraBold,
                          fontSize = 11.sp,
                          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                }

                LingoPrimaryGradientButton(
                  text = when {
                    isPendingPurchase -> "Purchase Pending..."
                    isVerifiedPaid && ((isYearlySelected && isYearlyActive) || (!isYearlySelected && isMonthlyActive)) ->
                      "LingoSphere Pro Active"
                    else -> "Continue with Pro ($selectedFormattedPrice)"
                  },
                  onClick = {
                    val activity = context.findActivity()
                    if (isYearlySelected) {
                      if (isYearlyActive) {
                        billingRepository?.openGooglePlaySubscriptionManagement(activity, SubscriptionPlan.PRODUCT_ID_YEARLY)
                      } else {
                        launchPlanPurchase(SubscriptionPlan.PRODUCT_ID_YEARLY, isYearlyLiveTrialAvailable)
                      }
                    } else {
                      if (isMonthlyActive) {
                        billingRepository?.openGooglePlaySubscriptionManagement(activity, SubscriptionPlan.PRODUCT_ID_MONTHLY)
                      } else {
                        launchPlanPurchase(SubscriptionPlan.PRODUCT_ID_MONTHLY, isMonthlyLiveTrialAvailable)
                      }
                    }
                  },
                  enabled = !isPendingPurchase,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_with_pro_btn")
                )
              }
            }

            // 2A. YEARLY PREMIUM – BEST VALUE CARD (Primary Brand Gradient CTA)
            val yearlyDisplayPrice = yearlyPricing?.formattedPrice ?: "$79.99 / year"
            Card(
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              colors = CardDefaults.cardColors(containerColor = LingoTokens.bgSubtle),
              border = BorderStroke(
                width = 1.dp,
                color = if (isYearlyActive) LingoEmerald.copy(alpha = 0.7f) else LingoTokens.accentGold.copy(alpha = 0.45f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedPlanId = SubscriptionPlan.PRODUCT_ID_YEARLY }
                .testTag("plan_card_${SubscriptionPlan.PRODUCT_ID_YEARLY}")
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(20.dp)
              ) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isYearlyActive) LingoEmerald.copy(alpha = 0.16f) else LingoTokens.accentGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, if (isYearlyActive) LingoEmerald else LingoTokens.accentGold.copy(alpha = 0.45f))
                  ) {
                    Text(
                      text = if (isYearlyActive) "CURRENT PLAN" else "BEST VALUE",
                      color = if (isYearlyActive) LingoEmerald else LingoTokens.accentGold,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 12.sp,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }

                  if (yearlySavingsPct > 0 && !isYearlyActive) {
                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = LingoTokens.accentGold.copy(alpha = 0.18f)
                    ) {
                      Text(
                        text = "Save $yearlySavingsPct%",
                        color = LingoTokens.accentGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                      )
                    }
                  }
                }

                Text(
                  text = "YEARLY PREMIUM – BEST VALUE",
                  color = LingoTokens.accentGold,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  letterSpacing = 0.4.sp
                )

                Text(
                  text = yearlyDisplayPrice,
                  color = LingoTokens.textPrimary,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 28.sp,
                  modifier = Modifier.testTag("yearly_plan_price_headline")
                )

                val equivalentBullet = if (yearlySavingsPct > 0) {
                  "Equivalent to $yearlyEqFormatted (Save $yearlySavingsPct% vs monthly)"
                } else {
                  "Equivalent to $yearlyEqFormatted"
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(
                    "Billed annually ($yearlyDisplayPrice)",
                    equivalentBullet,
                    "Full Premium access · Cancel anytime"
                  ).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = LingoTokens.brandCyan,
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = item,
                        color = LingoTokens.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                      )
                    }
                  }
                }

                val yearlyCtaText = when {
                  isPendingPurchase -> "Purchase Pending..."
                  isYearlyActive -> "Pro Active"
                  isVerifiedPaid -> if (yearlySavingsPct > 0) "Switch to Yearly ($yearlySavingsPct% Off)" else "Switch to Yearly"
                  else -> "Choose Yearly"
                }

                // ONE Primary Brand Gradient CTA on screen
                LingoPrimaryGradientButton(
                  text = yearlyCtaText,
                  onClick = {
                    selectedPlanId = SubscriptionPlan.PRODUCT_ID_YEARLY
                    val activity = context.findActivity()
                    if (isYearlyActive) {
                      billingRepository?.openGooglePlaySubscriptionManagement(activity, SubscriptionPlan.PRODUCT_ID_YEARLY)
                    } else {
                      launchPlanPurchase(SubscriptionPlan.PRODUCT_ID_YEARLY, false)
                    }
                  },
                  enabled = !isPendingPurchase,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("choose_yearly_btn")
                )
              }
            }

            // 2B. MONTHLY PREMIUM CARD (Secondary dark surface CTA)
            val monthlyDisplayPrice = monthlyPricing?.formattedPrice ?: "$9.99 / month"
            Card(
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              colors = CardDefaults.cardColors(containerColor = LingoTokens.bgElevated),
              border = BorderStroke(
                width = 1.dp,
                color = if (isMonthlyActive) LingoEmerald.copy(alpha = 0.7f) else LingoTokens.innerHighlight
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedPlanId = SubscriptionPlan.PRODUCT_ID_MONTHLY }
                .testTag("plan_card_${SubscriptionPlan.PRODUCT_ID_MONTHLY}")
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(20.dp)
              ) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "MONTHLY PREMIUM",
                    color = LingoTokens.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.4.sp
                  )
                  if (isMonthlyActive) {
                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = LingoEmerald.copy(alpha = 0.16f),
                      border = BorderStroke(1.dp, LingoEmerald)
                    ) {
                      Text(
                        text = "CURRENT PLAN",
                        color = LingoEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                      )
                    }
                  }
                }

                Text(
                  text = monthlyDisplayPrice,
                  color = LingoTokens.textPrimary,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 26.sp,
                  modifier = Modifier.testTag("monthly_plan_price_headline")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf("Billed monthly", "Full Premium access · Cancel anytime").forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = LingoTokens.textSecondary,
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(text = item, color = LingoTokens.textPrimary, fontSize = 14.sp)
                    }
                  }
                }

                val monthlyCtaText = when {
                  isPendingPurchase -> "Purchase Pending..."
                  isMonthlyActive -> "Pro Active"
                  isVerifiedPaid -> "Switch to Monthly"
                  else -> "Choose Monthly"
                }

                OutlinedButton(
                  onClick = {
                    selectedPlanId = SubscriptionPlan.PRODUCT_ID_MONTHLY
                    val activity = context.findActivity()
                    if (isMonthlyActive) {
                      billingRepository?.openGooglePlaySubscriptionManagement(activity, SubscriptionPlan.PRODUCT_ID_MONTHLY)
                    } else {
                      launchPlanPurchase(SubscriptionPlan.PRODUCT_ID_MONTHLY, false)
                    }
                  },
                  enabled = !isPendingPurchase,
                  colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = LingoTokens.bgSubtle,
                    contentColor = LingoTokens.textPrimary
                  ),
                  border = BorderStroke(1.dp, if (isMonthlyActive) LingoEmerald else LingoTokens.innerHighlight),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("choose_monthly_btn")
                ) {
                  Text(
                    text = monthlyCtaText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            // 3. ACCOUNT SUBSCRIPTION STATUS & MANAGEMENT
            val statusSubtext = remember(
              isVerifiedPaid,
              userProfile.subscriptionTier,
              userProfile.isFreeTrialActive,
              userProfile.subscriptionExpiryEpochMs,
              isAutoRenewing
            ) {
              if (!isVerifiedPaid) {
                "Free tier active · Upgrade anytime via Google Play"
              } else if (userProfile.subscriptionExpiryEpochMs > 0L) {
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US)
                  .format(Date(userProfile.subscriptionExpiryEpochMs))
                when {
                  !isAutoRenewing -> "Canceled · Access retained until $dateStr"
                  userProfile.isFreeTrialActive -> "Free trial active · Next billing on $dateStr"
                  else -> "Auto-renews on $dateStr"
                }
              } else {
                "Verified active via Google Play"
              }
            }

            Card(
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              colors = CardDefaults.cardColors(containerColor = LingoTokens.bgElevated),
              border = BorderStroke(1.dp, if (isVerifiedPaid) LingoEmerald.copy(alpha = 0.55f) else LingoTokens.innerHighlight),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("current_subscription_card")
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Current Plan",
                      color = LingoTokens.textSecondary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (isVerifiedPaid) userProfile.planDisplayName else "Free Plan",
                      color = LingoTokens.textPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 17.sp
                    )
                    Text(
                      text = statusSubtext,
                      color = if (isVerifiedPaid && !isAutoRenewing) LingoTokens.accentGold else LingoTokens.textSecondary,
                      fontSize = 13.sp,
                      modifier = Modifier.testTag("subscription_renewal_or_expiry_text")
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isVerifiedPaid) LingoEmerald.copy(alpha = 0.16f) else LingoTokens.bgSubtle
                  ) {
                    Text(
                      text = when {
                        isVerifiedPaid && !isAutoRenewing -> "CANCELED"
                        isVerifiedPaid -> "ACTIVE"
                        else -> "FREE"
                      },
                      color = if (isVerifiedPaid) LingoEmerald else LingoTokens.textSecondary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                  }
                }

                // Restore Purchases & Manage Subscription Actions
                Row(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  OutlinedButton(
                    onClick = {
                      val activity = context.findActivity()
                      if (billingRepository != null) {
                        billingRepository.openGooglePlaySubscriptionManagement(activity, selectedPlanId)
                      } else {
                        val uri = android.net.Uri.parse(
                          "https://play.google.com/store/account/subscriptions?sku=$selectedPlanId&package=${context.packageName}"
                        )
                        runCatching {
                          context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          )
                        }
                      }
                    },
                    border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                    colors = ButtonDefaults.outlinedButtonColors(
                      containerColor = LingoTokens.bgSubtle,
                      contentColor = LingoTokens.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("manage_google_play_subscription_btn")
                  ) {
                    Text(
                      text = "Manage Subscription",
                      color = LingoTokens.textPrimary,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 12.sp,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  OutlinedButton(
                    onClick = {
                      if (billingRepository != null) {
                        billingRepository.restorePurchases { _, msg ->
                          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                      } else {
                        Toast.makeText(
                          context,
                          "Google Play Billing is required on a Play-certified device to restore purchases.",
                          Toast.LENGTH_SHORT
                        ).show()
                      }
                    },
                    border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                    colors = ButtonDefaults.outlinedButtonColors(
                      containerColor = LingoTokens.bgSubtle,
                      contentColor = LingoTokens.textSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("restore_purchases_btn")
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Refresh,
                      contentDescription = null,
                      tint = LingoTokens.textSecondary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Restore Purchases",
                      color = LingoTokens.textSecondary,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 12.sp,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            // 3B. PHASE 5: TWO-COLUMN FREE VS LINGOSPHERE PRO COMPARISON TABLE (5 GROUPS)
            FreeVsProComparisonCard()

            // 4. EXPANDABLE "MY AI USAGE" DASHBOARD (QUOTAS & RESET TIMERS PRESERVED)
            val resetDates = remember { computeQuotaResetDates() }

            Card(
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              colors = CardDefaults.cardColors(containerColor = LingoTokens.bgElevated),
              border = BorderStroke(1.dp, LingoTokens.innerHighlight),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isMyAiUsageExpanded = !isMyAiUsageExpanded }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .testTag("toggle_my_ai_usage_btn")
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "My AI Usage",
                      color = LingoTokens.textPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp
                    )
                    Text(
                      text = "Track AI Voice, Video, Grammar, Pronunciation & Studio quotas",
                      color = LingoTokens.textSecondary,
                      fontSize = 13.sp
                    )
                  }
                  Icon(
                    imageVector = if (isMyAiUsageExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isMyAiUsageExpanded) "Collapse" else "Expand",
                    tint = LingoTokens.brandCyan
                  )
                }

                AnimatedVisibility(
                  visible = isMyAiUsageExpanded,
                  enter = expandVertically() + fadeIn(),
                  exit = shrinkVertically() + fadeOut()
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                      .testTag("my_ai_usage_content")
                  ) {
                    HorizontalDivider(color = LingoTokens.innerHighlight)

                    Row(
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LingoTokens.bgSubtle,
                        border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                          Text(
                            text = "Monthly Reset",
                            color = LingoTokens.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                          )
                          Text(
                            text = resetDates.first,
                            color = LingoTokens.brandCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LingoTokens.bgSubtle,
                        border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                          Text(
                            text = "Daily Reset",
                            color = LingoTokens.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                          )
                          Text(
                            text = resetDates.second,
                            color = LingoEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }
                    }

                    quotaStatuses.forEach { status ->
                      UsageMeterRow(status = status)
                    }

                    Row(
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      OutlinedButton(
                        onClick = onStartVoiceCall,
                        colors = ButtonDefaults.outlinedButtonColors(
                          containerColor = LingoTokens.bgSubtle,
                          contentColor = LingoTokens.textPrimary
                        ),
                        border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .weight(1f)
                          .height(48.dp)
                      ) {
                        Text(
                          text = "AI Voice (${voiceQuota.remainingAmount}m)",
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                      OutlinedButton(
                        onClick = onStartVideoTutorCall,
                        colors = ButtonDefaults.outlinedButtonColors(
                          containerColor = LingoTokens.bgSubtle,
                          contentColor = LingoTokens.textPrimary
                        ),
                        border = BorderStroke(1.dp, LingoTokens.innerHighlight),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .weight(1f)
                          .height(48.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.VideoCall,
                          contentDescription = null,
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "AI Video (${videoQuota.remainingAmount}m)",
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                }
              }
            }

            // 5. SUBSCRIPTION RENEWAL TERMS + LEGAL LINKS
            Text(
              text = "Subscriptions are billed securely through Google Play and automatically renew monthly or yearly unless canceled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play Subscriptions.",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp,
              lineHeight = 18.sp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("trial_renewal_terms_text")
            )

            Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              TextButton(
                onClick = { activeLegalDialog = "privacy" },
                modifier = Modifier.testTag("subscription_privacy_policy_btn")
              ) {
                Text("Privacy Policy", color = LingoTokens.brandCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
              Text("·", color = LingoTokens.textSecondary, fontSize = 13.sp)
              TextButton(
                onClick = { activeLegalDialog = "terms" },
                modifier = Modifier.testTag("subscription_terms_of_service_btn")
              ) {
                Text("Terms of Service", color = LingoTokens.brandCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }
    }
  }

  activeLegalDialog?.let { kind ->
    val isPrivacy = kind == "privacy"
    AlertDialog(
      onDismissRequest = { activeLegalDialog = null },
      containerColor = SubCardSurface,
      title = {
        Text(
          text = if (isPrivacy) "LingoSphere Privacy Policy" else "LingoSphere Terms of Service",
          color = SubTextPrimary,
          fontWeight = FontWeight.ExtraBold
        )
      },
      text = {
        Text(
          text = if (isPrivacy) {
            "LingoSphere stores your learning progress, flashcard history, and subscription entitlement status securely using Firebase Authentication and Cloud Firestore. Payment processing is handled exclusively by Google Play Billing — LingoSphere never accesses or stores payment card numbers."
          } else {
            "Monthly and Yearly LingoSphere Premium subscriptions auto-renew through your Google Play account unless canceled in Google Play Subscriptions before renewal. Both Monthly and Yearly plans unlock the exact same Premium tutoring, studio, and offline features."
          },
          color = SubTextSecondary,
          fontSize = 13.sp,
          lineHeight = 19.sp
        )
      },
      confirmButton = {
        TextButton(onClick = { activeLegalDialog = null }) {
          Text("Close", color = LingoCyan, fontWeight = FontWeight.Bold)
        }
      }
    )
  }
}

@Composable
private fun UsageMeterRow(status: FeatureQuotaStatus) {
  val barColor = when (status.warningLevel) {
    QuotaWarningLevel.LIMIT_REACHED, QuotaWarningLevel.WARNING_95_PERCENT -> LingoCoral
    QuotaWarningLevel.WARNING_80_PERCENT -> LingoAmber
    QuotaWarningLevel.NORMAL -> LingoCyan
  }
  val friendlyTitle = when (status.feature) {
    GeminiFeatureCategory.AI_VOICE_TUTOR -> "AI Voice Tutor"
    GeminiFeatureCategory.AI_VIDEO_TUTOR -> "AI Video Tutor"
    GeminiFeatureCategory.GRAMMAR_CHECK -> "Grammar"
    GeminiFeatureCategory.PRONUNCIATION_CHECK -> "Pronunciation"
    GeminiFeatureCategory.INTELLIGENT_STUDIO -> "Intelligent Studio"
    GeminiFeatureCategory.HUMAN_CALL -> "Human Calls"
    else -> status.feature.displayName
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF071229))
      .padding(12.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "$friendlyTitle (${status.periodLabel})",
        color = SubTextPrimary,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
          .weight(1f, fill = false)
          .padding(end = 8.dp)
      )
      Text(
        text = "${status.remainingAmount} ${status.unitLabel} left (${status.usedAmount}/${status.maxAllowed})",
        color = barColor,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold
      )
    }
    LinearProgressIndicator(
      progress = { status.usageFraction },
      color = barColor,
      trackColor = Color(0xFF1E293B),
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(4.dp))
    )
  }
}

private fun computeQuotaResetDates(): Pair<String, String> {
  val now = Calendar.getInstance()
  val nextMonth = (now.clone() as Calendar).apply {
    add(Calendar.MONTH, 1)
    set(Calendar.DAY_OF_MONTH, 1)
  }
  val monthResetStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(nextMonth.time)
  return monthResetStr to "Tonight at 12:00 AM"
}

// ============================================================================
// DEDICATED LEARNING TOOLS VIEWS (OPENED FROM PROFILE OR LEARNING PAGES)
// ============================================================================
@Composable
private fun LearningToolsDedicatedView(
  section: PremiumHubSection,
  userProfile: UserProfile,
  targetLang: Language,
  nativeLang: Language,
  speechManager: SpeechManager,
  curriculumLessons: List<CurriculumLesson>,
  downloadedLessonIds: Set<String>,
  pendingOfflineSyncCount: Int,
  flashcards: List<FlashcardProgress>,
  speakingSessions: List<SpeakingSession>,
  mistakeEntries: List<MistakeNotebookEntry>,
  weeklyReport: WeeklyLearningReportSummary?,
  customProDecks: List<PremiumCustomDeck>,
  cefrStageLabel: String,
  onSaveCustomProDeck: (PremiumCustomDeck) -> Unit,
  onDownloadAllLessons: (List<CurriculumLesson>) -> Unit,
  onSyncOfflineNow: () -> Unit,
  horizontalPad: androidx.compose.ui.unit.Dp
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val topicPresets = remember {
    listOf(
      "Business & Executive Negotiations",
      "Airport, Flights & Hotel Check-In",
      "Hospital & Medical Consultations",
      "Software Engineering & Tech Interviews",
      "Restaurants & Gastronomy"
    )
  }
  var customDeckTopic by remember { mutableStateOf(topicPresets.first()) }
  var isGeneratingDeck by remember { mutableStateOf(false) }
  var latestGeneratedCards by remember { mutableStateOf<List<PremiumGeneratedVocabCard>>(emptyList()) }

  var isGeneratingCoachingPlan by remember { mutableStateOf(false) }
  var coachingPlanResult by remember { mutableStateOf<PremiumCoachingPlanResult?>(null) }

  val hasCompletedValidatedAssessment = cefrStageLabel.isNotBlank()
  val effectiveCefr = cefrStageLabel.ifBlank {
    when (userProfile.proficiencyLevel) {
      "Advanced" -> "C1"
      "Intermediate" -> "B1"
      else -> "A2"
    }
  }
  val avgFluency = remember(speakingSessions) {
    if (speakingSessions.isNotEmpty()) {
      speakingSessions.map { it.fluencyScore }.average().toInt()
    } else 0
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = horizontalPad, vertical = 16.dp)
  ) {
    // 1. AI CUSTOM VOCABULARY DECK STUDIO
    if (section == PremiumHubSection.AI_STUDIO_DECKS) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SubCardSurface),
        border = BorderStroke(1.2.dp, LingoCyan),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ai_custom_vocab_deck_studio_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "AI Custom Vocabulary Deck Studio",
            color = SubTextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp
          )
          Text(
            text = "Generate tailored ${targetLang.name} flashcards for any profession, trip, or exam using Intelligent AI.",
            color = SubTextSecondary,
            fontSize = 12.5.sp
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            topicPresets.forEach { preset ->
              FilterChip(
                selected = customDeckTopic == preset,
                onClick = { customDeckTopic = preset },
                label = { Text(preset, fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoElectricBlue,
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          OutlinedTextField(
            value = customDeckTopic,
            onValueChange = { customDeckTopic = it },
            label = { Text("Custom Topic, Industry, or Scenario (${targetLang.name})") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_deck_topic_input")
          )

          Button(
            onClick = {
              if (!userProfile.isEntitlementVerified) {
                com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                return@Button
              }
              if (!isGeneratingDeck) {
                isGeneratingDeck = true
                coroutineScope.launch {
                  try {
                    val cards = GeminiTutorService.generateCustomPremiumVocabDeck(
                      topic = customDeckTopic,
                      targetLanguage = targetLang,
                      nativeLanguage = nativeLang,
                      cefrLevel = effectiveCefr
                    )
                    latestGeneratedCards = cards
                  } catch (e: Exception) {
                    val friendly = com.example.ai.AiErrorHandler.handle(e, userProfile.isEntitlementVerified)
                    Toast.makeText(context, friendly.message, Toast.LENGTH_SHORT).show()
                  } finally {
                    isGeneratingDeck = false
                  }
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LingoCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("generate_custom_deck_btn")
          ) {
            if (isGeneratingDeck) {
              CircularProgressIndicator(
                color = Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Generating ${targetLang.name} Deck...", fontWeight = FontWeight.Bold)
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Generate 6-Card AI Vocab Deck", fontWeight = FontWeight.ExtraBold)
            }
          }

          if (latestGeneratedCards.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Generated Deck (${latestGeneratedCards.size} cards):",
                  color = LingoEmerald,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                )
                Button(
                  onClick = {
                    val deck = PremiumCustomDeck(
                      id = "pro_deck_${UUID.randomUUID().toString().take(8)}",
                      topicTitle = customDeckTopic.ifBlank { "Custom Deck" },
                      languageCode = targetLang.code,
                      cefrLevel = effectiveCefr,
                      cards = latestGeneratedCards
                    )
                    onSaveCustomProDeck(deck)
                    Toast.makeText(
                      context,
                      "Saved ${latestGeneratedCards.size} cards to Flashcards!",
                      Toast.LENGTH_SHORT
                    ).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color.Black),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("save_generated_deck_btn")
                ) {
                  Text("Save All to Flashcards", fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp)
                }
              }

              latestGeneratedCards.forEach { card ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF071229)),
                  border = BorderStroke(1.dp, SubCardBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${card.termTarget} — ${card.translationNative}",
                        color = SubTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                      )
                      Text(
                        text = "[${card.phoneticGuide}]",
                        color = LingoCyan,
                        fontSize = 11.5.sp
                      )
                      Text(
                        text = card.exampleSentenceTarget,
                        color = SubTextSecondary,
                        fontSize = 12.sp
                      )
                    }
                    IconButton(
                      onClick = {
                        speechManager.speak(card.termTarget, targetLang.code, speechRate = 0.92f)
                      }
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen pronunciation",
                        tint = LingoCyan
                      )
                    }
                  }
                }
              }
            }
          }

          if (customProDecks.isNotEmpty()) {
            Text(
              text = "Saved Custom Decks (${customProDecks.size}): ${customProDecks.take(3).joinToString(" • ") { it.topicTitle }}",
              color = SubTextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    // 2. AI 7-DAY PERSONALIZED ROADMAP
    if (section == PremiumHubSection.LEARNING_ROADMAP) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SubCardSurface),
        border = BorderStroke(1.2.dp, LingoEmerald),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ai_learning_roadmap_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "AI 7-Day Personalized Learning Roadmap",
            color = SubTextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp
          )
          Text(
            text = "Analyzes your Mistake Notebook (${mistakeEntries.size} items), pronunciation diagnostics, and study pace to build a 7-day intensive roadmap.",
            color = SubTextSecondary,
            fontSize = 12.5.sp
          )

          Button(
            onClick = {
              if (!userProfile.isEntitlementVerified) {
                com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
                return@Button
              }
              if (!isGeneratingCoachingPlan) {
                isGeneratingCoachingPlan = true
                coroutineScope.launch {
                  try {
                    val topMistakes = mistakeEntries.take(4).map { "${it.originalText} -> ${it.correctedText}" }
                    val diffSounds = weeklyReport?.difficultSounds ?: emptyList()
                    val weekMins = weeklyReport?.thisWeekMinutes ?: 35
                    coachingPlanResult = GeminiTutorService.generatePersonalizedProCoachingPlan(
                      targetLanguage = targetLang,
                      nativeLanguage = nativeLang,
                      proficiencyLevel = userProfile.proficiencyLevel,
                      topMistakes = topMistakes,
                      difficultSounds = diffSounds,
                      weeklyMinutes = weekMins
                    )
                  } catch (e: Exception) {
                    val friendly = com.example.ai.AiErrorHandler.handle(e, userProfile.isEntitlementVerified)
                    Toast.makeText(context, friendly.message, Toast.LENGTH_SHORT).show()
                  } finally {
                    isGeneratingCoachingPlan = false
                  }
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("generate_coaching_plan_btn")
          ) {
            if (isGeneratingCoachingPlan) {
              CircularProgressIndicator(
                color = Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Building Personalized 7-Day Roadmap...", fontWeight = FontWeight.Bold)
            } else {
              Text("Generate My 7-Day AI Roadmap", fontWeight = FontWeight.ExtraBold)
            }
          }

          coachingPlanResult?.let { plan ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF071229)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.headlineSummary, color = SubTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Progress Velocity: ${plan.estimatedCefrVelocity}", color = LingoCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text("Grammar Focus: ${plan.focusGrammarRule}", color = LingoAmber, fontSize = 12.sp)
                Text("Recommended Mission: ${plan.recommendedMissionTitle}", color = LingoEmerald, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                plan.dailySchedule.forEach { (dayLabel, taskDesc) ->
                  Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(
                      text = "$dayLabel:",
                      color = LingoCyan,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      modifier = Modifier.width(56.dp)
                    )
                    Text(
                      text = taskDesc,
                      color = SubTextSecondary,
                      fontSize = 12.sp,
                      modifier = Modifier.weight(1f)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. OFFLINE LANGUAGE PACKS
    if (section == PremiumHubSection.OFFLINE_DOWNLOADS) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SubCardSurface),
        border = BorderStroke(1.dp, SubCardBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("offline_language_packs_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Offline Language Packs & Cloud Sync",
            color = SubTextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp
          )
          Text(
            text = "Downloaded: ${downloadedLessonIds.size} / ${curriculumLessons.size} ${targetLang.name} lessons • Pending cloud sync: $pendingOfflineSyncCount",
            color = SubTextSecondary,
            fontSize = 12.5.sp
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = { onDownloadAllLessons(curriculumLessons) },
              colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("download_all_offline_pack_btn")
            ) {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Download All (${curriculumLessons.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = onSyncOfflineNow,
              border = BorderStroke(1.dp, LingoCyan),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            ) {
              Icon(Icons.Default.CloudSync, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync Cloud Now", color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. CEFR PROGRESS, ACHIEVEMENTS & STUDY RECORD
    if (section == PremiumHubSection.ACHIEVEMENTS_CERTIFICATES) {
      val certId = remember(userProfile.userId, targetLang.code) {
        "LS-${targetLang.code.uppercase()}-${(userProfile.userId.hashCode() and 0xFFFF).toString(16).uppercase().padStart(4, '0')}"
      }
      val statusDescriptor = if (hasCompletedValidatedAssessment) {
        "Assessment-Validated LingoSphere CEFR Stage ($effectiveCefr)"
      } else {
        "Self-Study CEFR Progress Summary (Estimated $effectiveCefr — Complete Placement Assessment in Learn Tab to Validate)"
      }
      val certificateSummaryText = remember(userProfile, targetLang, effectiveCefr, avgFluency, flashcards.size, hasCompletedValidatedAssessment) {
        "LingoSphere CEFR Study Record ($certId)\n" +
          "Status: $statusDescriptor\n" +
          "Learner: ${userProfile.displayName.ifBlank { "LingoSphere Learner" }}\n" +
          "Language: ${targetLang.name} (${targetLang.flagEmoji}) • CEFR Stage: $effectiveCefr (${userProfile.proficiencyLevel})\n" +
          "Total XP: ${userProfile.xpPoints} • Completed Lessons: ${userProfile.completedLessonIds.size} • Vocabulary: ${flashcards.size} words" +
          (if (avgFluency > 0) " • Speaking Score: $avgFluency%" else "")
      }

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SubCardSurface),
        border = BorderStroke(1.2.dp, LingoCyan),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("cefr_progress_certificates_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = if (hasCompletedValidatedAssessment) Icons.Default.Verified else Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = LingoCyan,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (hasCompletedValidatedAssessment) {
                  "Validated CEFR Assessment Record"
                } else {
                  "CEFR Progress & Study Record"
                },
                color = SubTextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
              )
            }
            Text(
              text = certId,
              color = LingoCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF071229),
            border = BorderStroke(1.dp, SubCardBorder)
          ) {
            Text(
              text = if (hasCompletedValidatedAssessment) {
                "Validated via LingoSphere Placement & CEFR Assessment Exam (Record ID: $certId)."
              } else {
                "Note: This reflects your internal LingoSphere study progress. Complete the Placement & CEFR Assessment in the Learn tab to validate your CEFR band."
              },
              color = SubTextSecondary,
              fontSize = 11.5.sp,
              modifier = Modifier.padding(10.dp)
            )
          }

          Text(
            text = certificateSummaryText,
            color = SubTextPrimary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("LingoSphere Study Record", certificateSummaryText))
                Toast.makeText(context, "Study record copied to clipboard!", Toast.LENGTH_SHORT).show()
              },
              border = BorderStroke(1.dp, LingoCyan),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("export_cefr_certificate_btn")
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Copy Record", color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "LingoSphere ${targetLang.name} Study Record ($certId)")
                  putExtra(Intent.EXTRA_TEXT, certificateSummaryText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share CEFR Progress Record"))
              },
              colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue, contentColor = Color.White),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share Progress", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// PHASE 5: CLEAN 2-COLUMN FREE VS LINGOSPHERE PRO COMPARISON CARD (5 GROUPS)
// ============================================================================
private data class PlanComparisonRow(
  val featureTitle: String,
  val freeValue: String,
  val proValue: String
)

private data class PlanComparisonCategory(
  val categoryTitle: String,
  val rows: List<PlanComparisonRow>
)

@Composable
fun FreeVsProComparisonCard() {
  val categories = remember {
    listOf(
      PlanComparisonCategory(
        categoryTitle = "1. Curriculum, Vocabulary & Culture",
        rows = listOf(
          PlanComparisonRow("Structured Lessons", "Pro only", "Unlimited (500+)"),
          PlanComparisonRow("Vocabulary & SRS", "Review & Saved", "Full SRS + AI Decks"),
          PlanComparisonRow("Culture Journey", "1 chapter / country", "All 25 countries")
        )
      ),
      PlanComparisonCategory(
        categoryTitle = "2. AI Tutor, Roleplay & Voices",
        rows = listOf(
          PlanComparisonRow("Live AI Voice Tutor", "Pro only", "15m/day · 150m/mo"),
          PlanComparisonRow("AI Roleplay & Chat", "Pro only", "Full Roleplay & Chat"),
          PlanComparisonRow("Deep AI Corrections", "Pro only", "50 / day"),
          PlanComparisonRow("Natural Voices", "Standard playback", "All 15 natural voices")
        )
      ),
      PlanComparisonCategory(
        categoryTitle = "3. Books, Audiobooks & Listening",
        rows = listOf(
          PlanComparisonRow("Books & Audiobooks", "5 books / month", "Unlimited + AI summary"),
          PlanComparisonRow("Reading Studio (Gutendex)", "Included in 5 / mo", "Full catalog + AI narration"),
          PlanComparisonRow("Listening Practice", "Standard playback", "Unlimited + AI packs")
        )
      ),
      PlanComparisonCategory(
        categoryTitle = "4. Speaking Labs, Studio & Games",
        rows = listOf(
          PlanComparisonRow("Speaking & Writing Labs", "Pro only", "Unlimited + AI feedback"),
          PlanComparisonRow("AI Intelligent Studio", "Pro only", "40 runs / day"),
          PlanComparisonRow("Interactive Games", "5 levels / game", "Unlimited levels"),
          PlanComparisonRow("Personalized AI Material", "Pro only", "10 generations / day")
        )
      ),
      PlanComparisonCategory(
        categoryTitle = "5. Exchange, Calls & Career",
        rows = listOf(
          PlanComparisonRow("Language Exchange", "Browse, text & calls", "Smart Match + AI Rescue"),
          PlanComparisonRow("1:1 Audio & Video Calls", "Included", "Extended + Live AI"),
          PlanComparisonRow("Speaking Rooms", "Join public rooms", "Join, create & host"),
          PlanComparisonRow("Analytics & Interview Prep", "Pro only", "Full CEFR & Mock Interviews")
        )
      )
    )
  }
  val tutorUsage by com.example.data.repository.AccessPolicyManager.tutorUsageState.collectAsState()

  Card(
    shape = RoundedCornerShape(LingoTokens.cardRadius),
    colors = CardDefaults.cardColors(containerColor = LingoTokens.bgElevated),
    border = BorderStroke(1.dp, LingoTokens.innerHighlight),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("free_vs_pro_comparison_card")
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = LingoTokens.bgSubtle,
        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("subscription_tutor_usage_summary")
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(12.dp)
        ) {
          Text(
            text = "AI Tutor (Emma) Usage",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = LingoTokens.textPrimary
          )
          if (tutorUsage.isPro) {
            val todayMinUsed = tutorUsage.dailyUsedSeconds / 60
            val todayMinLimit = tutorUsage.dailyLimitSeconds / 60
            val monthMinUsed = tutorUsage.monthlyUsedSeconds / 60
            val monthMinLimit = tutorUsage.monthlyLimitSeconds / 60
            Text(
              text = "Today: $todayMinUsed of $todayMinLimit min used (resets at midnight)",
              fontSize = 13.sp,
              color = LingoTokens.textSecondary
            )
            Text(
              text = "This month: $monthMinUsed of $monthMinLimit min used (resets on ${tutorUsage.nextMonthResetDateLabel})",
              fontSize = 13.sp,
              color = LingoTokens.textSecondary
            )
            if (tutorUsage.isNearMonthlyLimit) {
              Text(
                text = "Note: You've used 80%+ of this month's 150 AI Tutor minutes. Resets on ${tutorUsage.nextMonthResetDateLabel}.",
                fontSize = 12.sp,
                color = LingoTokens.brandAmber,
                fontWeight = FontWeight.Medium
              )
            }
          } else {
            Text(
              text = "AI Tutor, structured lessons & Roleplay Studio unlock with LingoSphere Pro.",
              fontSize = 13.sp,
              color = LingoTokens.textSecondary
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Free vs LingoSphere Pro",
          color = LingoTokens.textPrimary,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 16.sp,
          modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "FREE",
            color = LingoTokens.textSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
          LingoProBadge(label = "PRO")
        }
      }

      categories.forEachIndexed { idx, category ->
        if (idx > 0) {
          HorizontalDivider(color = LingoTokens.innerHighlight)
        }
        Text(
          text = category.categoryTitle,
          color = LingoTokens.brandCyan,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          category.rows.forEach { row ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = row.featureTitle,
                color = LingoTokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1.15f)
              )
              Text(
                text = row.freeValue,
                color = LingoTokens.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.85f)
              )
              Text(
                text = row.proValue,
                color = LingoTokens.accentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// PHASE 2 & 5: CONTEXTUAL UPGRADE DIALOG & FRIENDLY LIMIT REACHED SHEET
// ============================================================================
@Composable
fun ContextualUpgradeSheetDialog(
  featureKey: String,
  featureTitle: String,
  featureBenefitDescription: String,
  onUpgradeToPro: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SubCardSurface,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        LingoProBadge(label = "PRO")
        Text(
          text = featureTitle,
          color = SubTextPrimary,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = featureBenefitDescription,
          color = SubTextSecondary,
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
        Text(
          text = "Unlock with LingoSphere Pro ($9.99/mo or $79.99/yr — Save 33%). Cancel anytime in Google Play.",
          color = LingoTokens.accentGold,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onUpgradeToPro,
        colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentGold, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("contextual_upgrade_confirm_btn_$featureKey")
      ) {
        Text("Unlock with Pro", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp)
      ) {
        Text("Not now", color = SubTextSecondary, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}

@Composable
fun FriendlyLimitReachedDialog(
  featureTitle: String,
  usedSummary: String,
  resetTimeFormatted: String,
  isFreeTier: Boolean,
  onUnlockPro: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SubCardSurface,
    title = {
      Text(
        text = "Daily Limit Reached · $featureTitle",
        color = SubTextPrimary,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "You've used $usedSummary for $featureTitle.",
          color = SubTextPrimary,
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
        Text(
          text = "Your quota automatically resets at $resetTimeFormatted.",
          color = LingoTokens.brandCyan,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        if (isFreeTier) {
          Text(
            text = "Want to keep practicing right now? Upgrade to LingoSphere Pro for higher daily & monthly allowances.",
            color = SubTextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
          )
        }
      }
    },
    confirmButton = {
      if (isFreeTier) {
        Button(
          onClick = onUnlockPro,
          colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentGold, contentColor = Color.Black),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .height(48.dp)
            .testTag("friendly_limit_unlock_pro_btn")
        ) {
          Text("Unlock LingoSphere Pro", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
      } else {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.height(48.dp)
        ) {
          Text("Got it", color = LingoCyan, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      if (isFreeTier) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.height(48.dp)
        ) {
          Text("Wait until reset", color = SubTextSecondary)
        }
      }
    }
  )
}


