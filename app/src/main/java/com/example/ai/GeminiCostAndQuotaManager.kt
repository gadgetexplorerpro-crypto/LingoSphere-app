package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Official Gemini-Only Model Tiers used across LingoSphere.
 * - FLASH_LITE ("gemini-3.5-flash-lite"): Used for ordinary lessons, vocabulary, grammar checks,
 *   quizzes, translations, and text chat for maximum cost efficiency.
 * - FLASH_STANDARD ("gemini-3.8-flash"): Used for Live AI Voice Tutor, AI Video Tutor,
 *   multimodal camera recognition, and complex oral evaluations.
 */
enum class GeminiModelTier(
  val modelId: String,
  val displayName: String,
  val inputCostPerMillionTokensUsd: Double,
  val outputCostPerMillionTokensUsd: Double,
  val audioCostPerMinuteUsd: Double
) {
  FLASH_LITE(
    modelId = "gemini-3.5-flash-lite",
    displayName = "Gemini 3.5 Flash-Lite (Cost-Efficient)",
    inputCostPerMillionTokensUsd = 0.075,
    outputCostPerMillionTokensUsd = 0.30,
    audioCostPerMinuteUsd = 0.0012
  ),
  FLASH_STANDARD(
    modelId = "gemini-3.8-flash",
    displayName = "Gemini 3.8 Flash (Live & Multimodal)",
    inputCostPerMillionTokensUsd = 0.15,
    outputCostPerMillionTokensUsd = 0.60,
    audioCostPerMinuteUsd = 0.0024
  )
}

/**
 * Categorizes every AI feature in LingoSphere for granular quota enforcement and cost attribution.
 */
enum class GeminiFeatureCategory(
  val id: String,
  val displayName: String,
  val preferredModelTier: GeminiModelTier,
  val isDailyQuota: Boolean
) {
  AI_VOICE_TUTOR(
    id = "ai_voice_tutor",
    displayName = "AI Voice Tutor",
    preferredModelTier = GeminiModelTier.FLASH_STANDARD,
    isDailyQuota = false // Monthly minutes
  ),
  AI_VIDEO_TUTOR(
    id = "ai_video_tutor",
    displayName = "AI Video Tutor",
    preferredModelTier = GeminiModelTier.FLASH_STANDARD,
    isDailyQuota = false // Monthly minutes
  ),
  GRAMMAR_CHECK(
    id = "grammar_check",
    displayName = "Grammar Checks",
    preferredModelTier = GeminiModelTier.FLASH_LITE,
    isDailyQuota = true // Daily count
  ),
  PRONUNCIATION_CHECK(
    id = "pronunciation_check",
    displayName = "Pronunciation Diagnostics",
    preferredModelTier = GeminiModelTier.FLASH_LITE,
    isDailyQuota = true // Daily count
  ),
  INTELLIGENT_STUDIO(
    id = "intelligent_studio",
    displayName = "Intelligent Studio & Coaching",
    preferredModelTier = GeminiModelTier.FLASH_LITE,
    isDailyQuota = true // Daily count
  ),
  BASIC_LESSON_QUIZ_CHAT(
    id = "basic_lesson_quiz_chat",
    displayName = "Lessons, Vocabulary & Text Chat",
    preferredModelTier = GeminiModelTier.FLASH_LITE,
    isDailyQuota = false // Unlimited on all tiers (rate-limited for safety)
  ),
  HUMAN_CALL(
    id = "human_call",
    displayName = "Human Peer Calls",
    preferredModelTier = GeminiModelTier.FLASH_LITE,
    isDailyQuota = true // Daily minutes
  )
}

/**
 * Defines the advertised allowances and financial parameters for each subscription plan.
 * Note: Advertised user allowances are NEVER silently reduced; if costs approach budget limits,
 * the administrator is alerted with recommendations for future billing periods.
 */
data class SubscriptionTierAllowances(
  val tierCode: String,
  val planName: String,
  val monthlyPriceUsd: Double,
  val monthlyAiVoiceMinutes: Int,
  val monthlyAiVideoMinutes: Int,
  val dailyGrammarChecks: Int,
  val dailyPronunciationChecks: Int,
  val dailyIntelligentStudioUses: Int,
  val dailyHumanCallMinutes: Int,
  val unlimitedBasicLessons: Boolean,
  val allPremiumGamesAndLessons: Boolean,
  val maxRequestsPerMinute: Int,
  val defaultMonthlyGeminiBudgetUsd: Double,
  val estimatedOperatingOverheadUsd: Double
) {
  val monthlyAiVoiceSeconds: Int get() = monthlyAiVoiceMinutes * 60
  val monthlyAiVideoSeconds: Int get() = monthlyAiVideoMinutes * 60
  val googlePlayFeeUsd: Double get() = (monthlyPriceUsd * 0.15 * 100.0).roundToInt() / 100.0
  val netRevenueAfterPlayFeeUsd: Double get() = ((monthlyPriceUsd - googlePlayFeeUsd) * 100.0).roundToInt() / 100.0
}

/**
 * Usage warning level when a user or plan approaches 80% or 95% of allowance/budget.
 */
enum class QuotaWarningLevel {
  NORMAL,
  WARNING_80_PERCENT,
  WARNING_95_PERCENT,
  LIMIT_REACHED
}

data class FeatureQuotaStatus(
  val feature: GeminiFeatureCategory,
  val usedAmount: Int,
  val maxAllowed: Int, // Int.MAX_VALUE for unlimited
  val unitLabel: String, // "mins/mo", "uses/day", "mins/day"
  val periodLabel: String // "Monthly", "Daily", "Unlimited"
) {
  val remainingAmount: Int
    get() = if (maxAllowed == Int.MAX_VALUE) Int.MAX_VALUE else max(0, maxAllowed - usedAmount)

  val usageFraction: Float
    get() = if (maxAllowed <= 0 || maxAllowed == Int.MAX_VALUE) 0f
    else (usedAmount.toFloat() / maxAllowed.toFloat()).coerceIn(0f, 1f)

  val usagePercent: Int
    get() = (usageFraction * 100f).roundToInt()

  val warningLevel: QuotaWarningLevel
    get() = when {
      maxAllowed == Int.MAX_VALUE -> QuotaWarningLevel.NORMAL
      usedAmount >= maxAllowed -> QuotaWarningLevel.LIMIT_REACHED
      usageFraction >= 0.95f -> QuotaWarningLevel.WARNING_95_PERCENT
      usageFraction >= 0.80f -> QuotaWarningLevel.WARNING_80_PERCENT
      else -> QuotaWarningLevel.NORMAL
    }

  val friendlyWarningMessage: String?
    get() = when (warningLevel) {
      QuotaWarningLevel.LIMIT_REACHED ->
        "You've reached 100% of your ${feature.displayName} allowance ($usedAmount/$maxAllowed $unitLabel). Upgrade your plan to continue immediately."
      QuotaWarningLevel.WARNING_95_PERCENT ->
        "Heads up! You've used $usagePercent% of your ${feature.displayName} allowance ($remainingAmount $unitLabel remaining)."
      QuotaWarningLevel.WARNING_80_PERCENT ->
        "Friendly reminder: You've reached $usagePercent% of your ${feature.displayName} allowance ($remainingAmount $unitLabel left)."
      QuotaWarningLevel.NORMAL -> null
    }
}

/**
 * Account-based AI usage and Gemini API cost snapshot persisted locally and synced to Firestore
 * so reinstalling the app or switching devices cannot bypass quotas.
 */
data class AccountAiUsageSnapshot(
  val userId: String = "anonymous",
  val subscriptionTier: String = "free",
  val currentMonthKey: String = "", // yyyy-MM
  val currentDayKey: String = "",   // yyyy-MM-dd
  val monthlyAiVoiceSecondsUsed: Int = 0,
  val monthlyAiVideoSecondsUsed: Int = 0,
  val dailyGrammarChecksUsed: Int = 0,
  val dailyPronunciationChecksUsed: Int = 0,
  val dailyIntelligentStudioUsed: Int = 0,
  val dailyHumanCallSecondsUsed: Int = 0,
  val monthlyInputTokens: Long = 0L,
  val monthlyOutputTokens: Long = 0L,
  val monthlyAudioInputSeconds: Int = 0,
  val monthlyAudioOutputSeconds: Int = 0,
  val monthlyFlashLiteCalls: Int = 0,
  val monthlyFlashStandardCalls: Int = 0,
  val monthlyEstimatedCostUsd: Double = 0.0,
  val featureCostBreakdownUsd: Map<String, Double> = emptyMap(),
  val compressedHistoryTokensSaved: Long = 0L,
  val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
  val monthlyAiVoiceMinutesUsed: Int
    get() = ceil(monthlyAiVoiceSecondsUsed / 60.0).toInt()

  val monthlyAiVideoMinutesUsed: Int
    get() = ceil(monthlyAiVideoSecondsUsed / 60.0).toInt()

  val dailyHumanCallMinutesUsed: Int
    get() = ceil(dailyHumanCallSecondsUsed / 60.0).toInt()
}

/**
 * Administrator-configurable Smart Profit Protection settings.
 */
data class AdminProfitProtectionConfig(
  val freePlanMaxGeminiBudgetUsd: Double = 0.0,
  val premiumPlanMaxGeminiBudgetUsd: Double = 2.85,
  val proPlanMaxGeminiBudgetUsd: Double = 2.85,
  val globalMonthlyGeminiBudgetUsd: Double = 250.0,
  val alertThresholdPercent: Int = 80,
  val criticalThresholdPercent: Int = 95,
  val contextCompressionEnabled: Boolean = true,
  val smartModelRoutingEnabled: Boolean = true,
  val maxHistoryTurnsKeptVerbatim: Int = 4
)

/**
 * Profitability & Margin report per subscription plan for the Smart Profit Protection Engine.
 */
data class PlanProfitabilityReport(
  val tierCode: String,
  val planName: String,
  val retailPriceUsd: Double,
  val googlePlayFeeUsd: Double,
  val netRevenueAfterPlayFeeUsd: Double,
  val worstCaseMaxGeminiCostUsd: Double,
  val typicalUserGeminiCostUsd: Double,
  val configuredGeminiBudgetUsd: Double,
  val operatingOverheadUsd: Double,
  val worstCaseNetProfitUsd: Double,
  val typicalNetProfitUsd: Double,
  val worstCaseProfitMarginPercent: Int,
  val typicalProfitMarginPercent: Int,
  val isProfitableAtMaxUsage: Boolean,
  val adminAlertMessage: String?,
  val futureBillingRecommendation: String
)

/**
 * Central Gemini-Only Subscription Quota, Token/Audio Cost Tracker, Rate Limiter,
 * Context Window Compressor, and Smart Profit Protection Engine.
 */
object GeminiCostAndQuotaManager {

  val FREE_ALLOWANCES = SubscriptionTierAllowances(
    tierCode = "free",
    planName = "Free",
    monthlyPriceUsd = 0.00,
    monthlyAiVoiceMinutes = 0,
    monthlyAiVideoMinutes = 0,
    dailyGrammarChecks = 0,
    dailyPronunciationChecks = 0,
    dailyIntelligentStudioUses = 0,
    dailyHumanCallMinutes = 20,
    unlimitedBasicLessons = false,
    allPremiumGamesAndLessons = false,
    maxRequestsPerMinute = 0,
    defaultMonthlyGeminiBudgetUsd = 0.0,
    estimatedOperatingOverheadUsd = 0.0
  )

  val PREMIUM_MONTHLY_ALLOWANCES = SubscriptionTierAllowances(
    tierCode = "premium_monthly",
    planName = "LingoSphere Pro ($9.99/mo)",
    monthlyPriceUsd = 9.99,
    monthlyAiVoiceMinutes = 150,
    monthlyAiVideoMinutes = 45,
    dailyGrammarChecks = 50,
    dailyPronunciationChecks = 50,
    dailyIntelligentStudioUses = 40,
    dailyHumanCallMinutes = 120,
    unlimitedBasicLessons = true,
    allPremiumGamesAndLessons = true,
    maxRequestsPerMinute = 80,
    defaultMonthlyGeminiBudgetUsd = 2.85,
    estimatedOperatingOverheadUsd = 0.35
  )

  val PREMIUM_PRO_999_ALLOWANCES = SubscriptionTierAllowances(
    tierCode = "premium_pro",
    planName = "LingoSphere Pro ($9.99/mo)",
    monthlyPriceUsd = 9.99,
    monthlyAiVoiceMinutes = 150,
    monthlyAiVideoMinutes = 45,
    dailyGrammarChecks = 50,
    dailyPronunciationChecks = 50,
    dailyIntelligentStudioUses = 40,
    dailyHumanCallMinutes = 120,
    unlimitedBasicLessons = true,
    allPremiumGamesAndLessons = true,
    maxRequestsPerMinute = 80,
    defaultMonthlyGeminiBudgetUsd = 2.85,
    estimatedOperatingOverheadUsd = 0.30
  )

  // Internal alias only — no $4.99 plan exists
  val PREMIUM_499_ALLOWANCES = PREMIUM_PRO_999_ALLOWANCES

  val PREMIUM_YEARLY_ALLOWANCES = SubscriptionTierAllowances(
    tierCode = "premium_yearly",
    planName = "LingoSphere Pro ($79.99/yr)",
    monthlyPriceUsd = 6.67,
    monthlyAiVoiceMinutes = 150,
    monthlyAiVideoMinutes = 45,
    dailyGrammarChecks = 50,
    dailyPronunciationChecks = 50,
    dailyIntelligentStudioUses = 40,
    dailyHumanCallMinutes = 120,
    unlimitedBasicLessons = true,
    allPremiumGamesAndLessons = true,
    maxRequestsPerMinute = 80,
    defaultMonthlyGeminiBudgetUsd = 2.85,
    estimatedOperatingOverheadUsd = 0.30
  )

  private const val PREFS_NAME = "lingosphere_gemini_quota_and_profit_prefs"
  private const val KEY_USAGE_PREFIX = "account_ai_usage_"
  private const val KEY_ADMIN_CONFIG = "admin_profit_protection_config"

  private var prefs: SharedPreferences? = null
  private var cloudSyncCallback: ((AccountAiUsageSnapshot) -> Unit)? = null

  private val _usageState = MutableStateFlow(
    AccountAiUsageSnapshot(
      currentMonthKey = currentMonthIso(),
      currentDayKey = currentDayIso()
    )
  )
  val usageState: StateFlow<AccountAiUsageSnapshot> = _usageState.asStateFlow()

  private val _adminConfigState = MutableStateFlow(AdminProfitProtectionConfig())
  val adminConfigState: StateFlow<AdminProfitProtectionConfig> = _adminConfigState.asStateFlow()

  // Sliding window timestamps (epoch ms) for per-minute API rate limiting
  private val recentRequestTimestampsMs = ArrayDeque<Long>()

  fun initialize(
    context: Context,
    userId: String,
    subscriptionTier: String,
    onSyncToCloud: ((AccountAiUsageSnapshot) -> Unit)? = null
  ) {
    if (prefs == null) {
      prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    if (onSyncToCloud != null) {
      cloudSyncCallback = onSyncToCloud
    }
    loadAdminConfig()
    switchActiveAccount(userId, subscriptionTier)
  }

  fun setCloudSyncCallback(callback: (AccountAiUsageSnapshot) -> Unit) {
    cloudSyncCallback = callback
  }

  fun switchActiveAccount(userId: String, subscriptionTier: String) {
    val cleanUserId = userId.ifBlank { "anonymous" }
    val loaded = loadSnapshotFromPrefs(cleanUserId, subscriptionTier)
    val normalized = normalizePeriodResets(loaded.copy(userId = cleanUserId, subscriptionTier = subscriptionTier))
    _usageState.value = normalized
    saveSnapshotToPrefs(normalized)
  }

  /**
   * Merges cloud-persisted usage from Firestore with local usage using `maxOf` for the current
   * billing month and day. This prevents quota bypasses via app reinstall or device switching.
   */
  fun mergeRemoteCloudSnapshot(remote: AccountAiUsageSnapshot) {
    val current = normalizePeriodResets(_usageState.value)
    if (remote.userId.isBlank() || (current.userId != "anonymous" && remote.userId != current.userId)) {
      return
    }
    val sameMonth = remote.currentMonthKey == current.currentMonthKey
    val sameDay = remote.currentDayKey == current.currentDayKey

    val mergedBreakdown = current.featureCostBreakdownUsd.toMutableMap()
    if (sameMonth) {
      remote.featureCostBreakdownUsd.forEach { (k, v) ->
        mergedBreakdown[k] = max(mergedBreakdown[k] ?: 0.0, v)
      }
    }

    val merged = current.copy(
      userId = remote.userId.ifBlank { current.userId },
      subscriptionTier = if (remote.subscriptionTier != "free") remote.subscriptionTier else current.subscriptionTier,
      monthlyAiVoiceSecondsUsed = if (sameMonth) max(current.monthlyAiVoiceSecondsUsed, remote.monthlyAiVoiceSecondsUsed) else current.monthlyAiVoiceSecondsUsed,
      monthlyAiVideoSecondsUsed = if (sameMonth) max(current.monthlyAiVideoSecondsUsed, remote.monthlyAiVideoSecondsUsed) else current.monthlyAiVideoSecondsUsed,
      dailyGrammarChecksUsed = if (sameDay) max(current.dailyGrammarChecksUsed, remote.dailyGrammarChecksUsed) else current.dailyGrammarChecksUsed,
      dailyPronunciationChecksUsed = if (sameDay) max(current.dailyPronunciationChecksUsed, remote.dailyPronunciationChecksUsed) else current.dailyPronunciationChecksUsed,
      dailyIntelligentStudioUsed = if (sameDay) max(current.dailyIntelligentStudioUsed, remote.dailyIntelligentStudioUsed) else current.dailyIntelligentStudioUsed,
      dailyHumanCallSecondsUsed = if (sameDay) max(current.dailyHumanCallSecondsUsed, remote.dailyHumanCallSecondsUsed) else current.dailyHumanCallSecondsUsed,
      monthlyInputTokens = if (sameMonth) max(current.monthlyInputTokens, remote.monthlyInputTokens) else current.monthlyInputTokens,
      monthlyOutputTokens = if (sameMonth) max(current.monthlyOutputTokens, remote.monthlyOutputTokens) else current.monthlyOutputTokens,
      monthlyAudioInputSeconds = if (sameMonth) max(current.monthlyAudioInputSeconds, remote.monthlyAudioInputSeconds) else current.monthlyAudioInputSeconds,
      monthlyAudioOutputSeconds = if (sameMonth) max(current.monthlyAudioOutputSeconds, remote.monthlyAudioOutputSeconds) else current.monthlyAudioOutputSeconds,
      monthlyFlashLiteCalls = if (sameMonth) max(current.monthlyFlashLiteCalls, remote.monthlyFlashLiteCalls) else current.monthlyFlashLiteCalls,
      monthlyFlashStandardCalls = if (sameMonth) max(current.monthlyFlashStandardCalls, remote.monthlyFlashStandardCalls) else current.monthlyFlashStandardCalls,
      monthlyEstimatedCostUsd = if (sameMonth) max(current.monthlyEstimatedCostUsd, remote.monthlyEstimatedCostUsd) else current.monthlyEstimatedCostUsd,
      featureCostBreakdownUsd = mergedBreakdown,
      compressedHistoryTokensSaved = if (sameMonth) max(current.compressedHistoryTokensSaved, remote.compressedHistoryTokensSaved) else current.compressedHistoryTokensSaved,
      updatedAtEpochMs = System.currentTimeMillis()
    )
    _usageState.value = merged
    saveSnapshotToPrefs(merged)
  }

  fun allowancesForTier(tierCode: String, isEntitlementVerified: Boolean = true): SubscriptionTierAllowances {
    if (!isEntitlementVerified && tierCode != "free") {
      return FREE_ALLOWANCES
    }
    return when (tierCode.lowercase(Locale.US)) {
      "premium_pro" -> PREMIUM_PRO_999_ALLOWANCES
      "premium_yearly" -> PREMIUM_YEARLY_ALLOWANCES
      "premium_monthly", "premium_499" -> PREMIUM_MONTHLY_ALLOWANCES
      else -> FREE_ALLOWANCES
    }
  }

  /**
   * Selects the optimal Gemini model ID for a given feature, honoring feature compatibility
   * (multimodal and live voice/video always use `gemini-3.8-flash`; high-frequency text tasks
   * use `gemini-3.5-flash-lite` when smart routing is enabled).
   */
  fun selectOptimalGeminiModel(
    feature: GeminiFeatureCategory,
    requiresMultimodalImage: Boolean = false
  ): GeminiModelTier {
    if (requiresMultimodalImage ||
      feature == GeminiFeatureCategory.AI_VOICE_TUTOR ||
      feature == GeminiFeatureCategory.AI_VIDEO_TUTOR
    ) {
      return GeminiModelTier.FLASH_STANDARD
    }
    return if (_adminConfigState.value.smartModelRoutingEnabled) {
      feature.preferredModelTier
    } else {
      GeminiModelTier.FLASH_STANDARD
    }
  }

  /**
   * Compresses multi-turn conversation history so older turns are condensed into a compact digest
   * while the most recent `maxHistoryTurnsKeptVerbatim` turns remain verbatim.
   * Returns Pair(formattedContextBlock, tokensSavedEstimate).
   */
  fun compressConversationHistory(
    historyTurns: List<Pair<String, String>>
  ): Pair<String, Int> {
    if (historyTurns.isEmpty()) return "" to 0
    val config = _adminConfigState.value
    val keepVerbatim = config.maxHistoryTurnsKeptVerbatim.coerceIn(2, 8)

    if (!config.contextCompressionEnabled || historyTurns.size <= keepVerbatim) {
      val verbatim = buildString {
        append("Recent Conversation Context:\n")
        historyTurns.takeLast(keepVerbatim).forEach { (role, text) ->
          append("$role: ${text.trim().take(260)}\n")
        }
      }
      return verbatim to 0
    }

    val rawChars = historyTurns.sumOf { it.first.length + it.second.length + 4 }
    val olderTurns = historyTurns.dropLast(keepVerbatim)
    val recentTurns = historyTurns.takeLast(keepVerbatim)

    val compressedDigest = olderTurns.takeLast(6).joinToString(" | ") { (role, text) ->
      val shortRole = if (role.lowercase(Locale.US).contains("user")) "U" else "T"
      val snippet = text.trim().replace(Regex("\\s+"), " ").take(65)
      "$shortRole:$snippet"
    }

    val optimizedBlock = buildString {
      append("Compressed Earlier Context: [$compressedDigest]\n")
      append("Recent Conversation Turns:\n")
      recentTurns.forEach { (role, text) ->
        append("$role: ${text.trim().take(240)}\n")
      }
    }

    val savedChars = max(0, rawChars - optimizedBlock.length)
    val savedTokens = savedChars / 4
    return optimizedBlock to savedTokens
  }

  /**
   * Checks sliding-window API rate limit for the active subscription tier.
   */
  @Synchronized
  fun checkAndRecordRateLimit(tierCode: String = _usageState.value.subscriptionTier): Boolean {
    val now = System.currentTimeMillis()
    val windowStart = now - 60_000L
    while (recentRequestTimestampsMs.isNotEmpty() && recentRequestTimestampsMs.first() < windowStart) {
      recentRequestTimestampsMs.removeFirst()
    }
    val maxRpm = allowancesForTier(tierCode).maxRequestsPerMinute
    if (recentRequestTimestampsMs.size >= maxRpm) {
      return false
    }
    recentRequestTimestampsMs.addLast(now)
    return true
  }

  /**
   * Checks whether the user has remaining quota for the specified feature before making an API call.
   */
  fun canConsumeFeature(
    feature: GeminiFeatureCategory,
    tierCode: String = _usageState.value.subscriptionTier,
    isEntitlementVerified: Boolean = tierCode != "free"
  ): Boolean {
    val status = getFeatureQuotaStatus(feature, tierCode, isEntitlementVerified)
    return status.remainingAmount > 0
  }

  fun getFeatureQuotaStatus(
    feature: GeminiFeatureCategory,
    tierCode: String = _usageState.value.subscriptionTier,
    isEntitlementVerified: Boolean = tierCode != "free"
  ): FeatureQuotaStatus {
    val current = normalizePeriodResets(_usageState.value)
    val allowances = allowancesForTier(tierCode, isEntitlementVerified)
    return when (feature) {
      GeminiFeatureCategory.AI_VOICE_TUTOR -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.monthlyAiVoiceMinutesUsed,
        maxAllowed = allowances.monthlyAiVoiceMinutes,
        unitLabel = "mins",
        periodLabel = "Monthly"
      )
      GeminiFeatureCategory.AI_VIDEO_TUTOR -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.monthlyAiVideoMinutesUsed,
        maxAllowed = allowances.monthlyAiVideoMinutes,
        unitLabel = "mins",
        periodLabel = "Monthly"
      )
      GeminiFeatureCategory.GRAMMAR_CHECK -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.dailyGrammarChecksUsed,
        maxAllowed = allowances.dailyGrammarChecks,
        unitLabel = "checks",
        periodLabel = "Daily"
      )
      GeminiFeatureCategory.PRONUNCIATION_CHECK -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.dailyPronunciationChecksUsed,
        maxAllowed = allowances.dailyPronunciationChecks,
        unitLabel = "checks",
        periodLabel = "Daily"
      )
      GeminiFeatureCategory.INTELLIGENT_STUDIO -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.dailyIntelligentStudioUsed,
        maxAllowed = allowances.dailyIntelligentStudioUses,
        unitLabel = "uses",
        periodLabel = "Daily"
      )
      GeminiFeatureCategory.HUMAN_CALL -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.dailyHumanCallMinutesUsed,
        maxAllowed = allowances.dailyHumanCallMinutes,
        unitLabel = "mins",
        periodLabel = "Daily"
      )
      GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT -> FeatureQuotaStatus(
        feature = feature,
        usedAmount = current.monthlyFlashLiteCalls,
        maxAllowed = Int.MAX_VALUE,
        unitLabel = "lessons",
        periodLabel = "Unlimited"
      )
    }
  }

  fun getAllFeatureQuotaStatuses(
    tierCode: String = _usageState.value.subscriptionTier,
    isEntitlementVerified: Boolean = tierCode != "free"
  ): List<FeatureQuotaStatus> {
    return listOf(
      getFeatureQuotaStatus(GeminiFeatureCategory.AI_VOICE_TUTOR, tierCode, isEntitlementVerified),
      getFeatureQuotaStatus(GeminiFeatureCategory.AI_VIDEO_TUTOR, tierCode, isEntitlementVerified),
      getFeatureQuotaStatus(GeminiFeatureCategory.GRAMMAR_CHECK, tierCode, isEntitlementVerified),
      getFeatureQuotaStatus(GeminiFeatureCategory.PRONUNCIATION_CHECK, tierCode, isEntitlementVerified),
      getFeatureQuotaStatus(GeminiFeatureCategory.INTELLIGENT_STUDIO, tierCode, isEntitlementVerified),
      getFeatureQuotaStatus(GeminiFeatureCategory.HUMAN_CALL, tierCode, isEntitlementVerified)
    )
  }

  /**
   * Remaining AI Voice call seconds for the current month.
   */
  fun getRemainingAiVoiceSeconds(
    tierCode: String = _usageState.value.subscriptionTier,
    isEntitlementVerified: Boolean = tierCode != "free"
  ): Int {
    val current = normalizePeriodResets(_usageState.value)
    val allowances = allowancesForTier(tierCode, isEntitlementVerified)
    return max(0, allowances.monthlyAiVoiceSeconds - current.monthlyAiVoiceSecondsUsed)
  }

  /**
   * Remaining AI Video Tutor seconds for the current month.
   */
  fun getRemainingAiVideoSeconds(
    tierCode: String = _usageState.value.subscriptionTier,
    isEntitlementVerified: Boolean = tierCode != "free"
  ): Int {
    val current = normalizePeriodResets(_usageState.value)
    val allowances = allowancesForTier(tierCode, isEntitlementVerified)
    return max(0, allowances.monthlyAiVideoSeconds - current.monthlyAiVideoSecondsUsed)
  }

  /**
   * Remaining Human Peer Call seconds for the current day.
   */
  fun getRemainingHumanCallSeconds(
    tierCode: String = _usageState.value.subscriptionTier,
    isEntitlementVerified: Boolean = tierCode != "free"
  ): Int {
    val current = normalizePeriodResets(_usageState.value)
    val allowances = allowancesForTier(tierCode, isEntitlementVerified)
    return max(0, allowances.dailyHumanCallMinutes * 60 - current.dailyHumanCallSecondsUsed)
  }

  /**
   * Records a SUCCESSFULLY completed Gemini API text/multimodal call.
   * Failed, timed-out, or empty responses must NEVER call this method.
   */
  @Synchronized
  fun recordSuccessfulGeminiCall(
    feature: GeminiFeatureCategory,
    modelTier: GeminiModelTier,
    promptText: String,
    responseText: String,
    audioInputSeconds: Int = 0,
    audioOutputSeconds: Int = 0,
    compressedTokensSaved: Int = 0,
    incrementFeatureCounter: Boolean = true
  ) {
    if (responseText.isBlank()) return
    val current = normalizePeriodResets(_usageState.value)

    val estimatedInputTokens = max(1, promptText.length / 4).toLong()
    val estimatedOutputTokens = max(1, responseText.length / 4).toLong()

    val tokenCostUsd =
      (estimatedInputTokens / 1_000_000.0) * modelTier.inputCostPerMillionTokensUsd +
        (estimatedOutputTokens / 1_000_000.0) * modelTier.outputCostPerMillionTokensUsd
    val audioMinutes = (audioInputSeconds + audioOutputSeconds) / 60.0
    val audioCostUsd = audioMinutes * modelTier.audioCostPerMinuteUsd
    val callCostUsd = tokenCostUsd + audioCostUsd

    val updatedBreakdown = current.featureCostBreakdownUsd.toMutableMap()
    updatedBreakdown[feature.id] = (updatedBreakdown[feature.id] ?: 0.0) + callCostUsd

    val updated = current.copy(
      dailyGrammarChecksUsed = if (incrementFeatureCounter && feature == GeminiFeatureCategory.GRAMMAR_CHECK) {
        current.dailyGrammarChecksUsed + 1
      } else current.dailyGrammarChecksUsed,
      dailyPronunciationChecksUsed = if (incrementFeatureCounter && feature == GeminiFeatureCategory.PRONUNCIATION_CHECK) {
        current.dailyPronunciationChecksUsed + 1
      } else current.dailyPronunciationChecksUsed,
      dailyIntelligentStudioUsed = if (incrementFeatureCounter && feature == GeminiFeatureCategory.INTELLIGENT_STUDIO) {
        current.dailyIntelligentStudioUsed + 1
      } else current.dailyIntelligentStudioUsed,
      monthlyInputTokens = current.monthlyInputTokens + estimatedInputTokens,
      monthlyOutputTokens = current.monthlyOutputTokens + estimatedOutputTokens,
      monthlyAudioInputSeconds = current.monthlyAudioInputSeconds + audioInputSeconds,
      monthlyAudioOutputSeconds = current.monthlyAudioOutputSeconds + audioOutputSeconds,
      monthlyFlashLiteCalls = if (modelTier == GeminiModelTier.FLASH_LITE) current.monthlyFlashLiteCalls + 1 else current.monthlyFlashLiteCalls,
      monthlyFlashStandardCalls = if (modelTier == GeminiModelTier.FLASH_STANDARD) current.monthlyFlashStandardCalls + 1 else current.monthlyFlashStandardCalls,
      monthlyEstimatedCostUsd = current.monthlyEstimatedCostUsd + callCostUsd,
      featureCostBreakdownUsd = updatedBreakdown,
      compressedHistoryTokensSaved = current.compressedHistoryTokensSaved + compressedTokensSaved.coerceAtLeast(0),
      updatedAtEpochMs = System.currentTimeMillis()
    )

    _usageState.value = updated
    saveSnapshotToPrefs(updated)
    cloudSyncCallback?.invoke(updated)
  }

  /**
   * Records actively connected AI Voice Tutor, AI Video Tutor, or Human Peer Call duration in seconds.
   * Only called while a session is genuinely active and connected.
   */
  @Synchronized
  fun recordConsumedCallSeconds(
    feature: GeminiFeatureCategory,
    secondsConsumed: Int,
    audioInputSeconds: Int = secondsConsumed / 2,
    audioOutputSeconds: Int = secondsConsumed / 2
  ) {
    if (secondsConsumed <= 0) return
    val current = normalizePeriodResets(_usageState.value)
    val modelTier = feature.preferredModelTier
    val audioCostUsd = if (feature == GeminiFeatureCategory.HUMAN_CALL) {
      0.0
    } else {
      (secondsConsumed / 60.0) * modelTier.audioCostPerMinuteUsd
    }

    val updatedBreakdown = current.featureCostBreakdownUsd.toMutableMap()
    if (audioCostUsd > 0.0) {
      updatedBreakdown[feature.id] = (updatedBreakdown[feature.id] ?: 0.0) + audioCostUsd
    }

    val updated = current.copy(
      monthlyAiVoiceSecondsUsed = if (feature == GeminiFeatureCategory.AI_VOICE_TUTOR) {
        current.monthlyAiVoiceSecondsUsed + secondsConsumed
      } else current.monthlyAiVoiceSecondsUsed,
      monthlyAiVideoSecondsUsed = if (feature == GeminiFeatureCategory.AI_VIDEO_TUTOR) {
        current.monthlyAiVideoSecondsUsed + secondsConsumed
      } else current.monthlyAiVideoSecondsUsed,
      dailyHumanCallSecondsUsed = if (feature == GeminiFeatureCategory.HUMAN_CALL) {
        current.dailyHumanCallSecondsUsed + secondsConsumed
      } else current.dailyHumanCallSecondsUsed,
      monthlyAudioInputSeconds = if (feature != GeminiFeatureCategory.HUMAN_CALL) {
        current.monthlyAudioInputSeconds + audioInputSeconds
      } else current.monthlyAudioInputSeconds,
      monthlyAudioOutputSeconds = if (feature != GeminiFeatureCategory.HUMAN_CALL) {
        current.monthlyAudioOutputSeconds + audioOutputSeconds
      } else current.monthlyAudioOutputSeconds,
      monthlyEstimatedCostUsd = current.monthlyEstimatedCostUsd + audioCostUsd,
      featureCostBreakdownUsd = updatedBreakdown,
      updatedAtEpochMs = System.currentTimeMillis()
    )

    _usageState.value = updated
    saveSnapshotToPrefs(updated)
    cloudSyncCallback?.invoke(updated)
  }

  /**
   * Updates administrator profit-protection configuration.
   */
  fun updateAdminProfitProtectionConfig(newConfig: AdminProfitProtectionConfig) {
    _adminConfigState.value = newConfig
    val p = prefs ?: return
    val obj = JSONObject().apply {
      put("freePlanMaxGeminiBudgetUsd", newConfig.freePlanMaxGeminiBudgetUsd)
      put("premiumPlanMaxGeminiBudgetUsd", newConfig.premiumPlanMaxGeminiBudgetUsd)
      put("proPlanMaxGeminiBudgetUsd", newConfig.proPlanMaxGeminiBudgetUsd)
      put("globalMonthlyGeminiBudgetUsd", newConfig.globalMonthlyGeminiBudgetUsd)
      put("alertThresholdPercent", newConfig.alertThresholdPercent)
      put("criticalThresholdPercent", newConfig.criticalThresholdPercent)
      put("contextCompressionEnabled", newConfig.contextCompressionEnabled)
      put("smartModelRoutingEnabled", newConfig.smartModelRoutingEnabled)
      put("maxHistoryTurnsKeptVerbatim", newConfig.maxHistoryTurnsKeptVerbatim)
    }
    p.edit().putString(KEY_ADMIN_CONFIG, obj.toString()).apply()
  }

  /**
   * Computes a comprehensive profitability verification report for a given subscription tier,
   * comparing retail price, 15% Google Play fee, worst-case 100% allowance Gemini API cost,
   * typical (42% utilization) Gemini cost, and administrator budget thresholds.
   */
  fun evaluatePlanProfitability(allowances: SubscriptionTierAllowances): PlanProfitabilityReport {
    val adminCfg = _adminConfigState.value
    val configuredBudget = when (allowances.tierCode) {
      "free" -> adminCfg.freePlanMaxGeminiBudgetUsd
      "premium_monthly" -> adminCfg.premiumPlanMaxGeminiBudgetUsd
      else -> adminCfg.proPlanMaxGeminiBudgetUsd
    }

    // 1. Voice Tutor worst-case monthly cost (Gemini 3.8 Flash + audio turns)
    // ~3.5 turns/min, ~450 input tokens + 120 output tokens/turn after compression + audio cost
    val voiceCostPerMinUsd = 0.0028
    val maxVoiceCostUsd = allowances.monthlyAiVoiceMinutes * voiceCostPerMinUsd

    // 2. Video Tutor worst-case monthly cost (Gemini 3.8 Flash + lip-sync phonetic cues)
    val videoCostPerMinUsd = 0.0045
    val maxVideoCostUsd = allowances.monthlyAiVideoMinutes * videoCostPerMinUsd

    // 3. Daily features (Grammar, Pronunciation, Intelligent Studio) over 30 days using Gemini 3.5 Flash-Lite
    // ~650 input tokens + 220 output tokens per check = ~$0.00012 per check
    val singleCheckCostUsd = 0.00018
    val maxGrammarMonthlyCostUsd = allowances.dailyGrammarChecks * 30 * singleCheckCostUsd
    val maxPronunciationMonthlyCostUsd = allowances.dailyPronunciationChecks * 30 * singleCheckCostUsd
    val maxStudioMonthlyCostUsd = allowances.dailyIntelligentStudioUses * 30 * 0.00028

    // 4. Unlimited basic lessons/quizzes/chat (~150 calls/mo average ceiling on Flash-Lite)
    val lessonsMonthlyCostUsd = if (allowances.unlimitedBasicLessons) 0.018 else 0.0

    val worstCaseGeminiCostUsd =
      maxVoiceCostUsd + maxVideoCostUsd + maxGrammarMonthlyCostUsd +
        maxPronunciationMonthlyCostUsd + maxStudioMonthlyCostUsd + lessonsMonthlyCostUsd

    val typicalGeminiCostUsd = worstCaseGeminiCostUsd * 0.42

    val netRev = allowances.netRevenueAfterPlayFeeUsd
    val worstCaseProfit = netRev - worstCaseGeminiCostUsd - allowances.estimatedOperatingOverheadUsd
    val typicalProfit = netRev - typicalGeminiCostUsd - allowances.estimatedOperatingOverheadUsd

    val worstCaseMarginPct = if (allowances.monthlyPriceUsd > 0.0) {
      ((worstCaseProfit / allowances.monthlyPriceUsd) * 100.0).roundToInt()
    } else 0

    val typicalMarginPct = if (allowances.monthlyPriceUsd > 0.0) {
      ((typicalProfit / allowances.monthlyPriceUsd) * 100.0).roundToInt()
    } else 0

    val isProfitable = allowances.monthlyPriceUsd == 0.0 || worstCaseProfit > 0.50

    val budgetUtilizationPct = if (configuredBudget > 0.0) {
      ((worstCaseGeminiCostUsd / configuredBudget) * 100.0).roundToInt()
    } else 0

    val adminAlert = when {
      allowances.monthlyPriceUsd > 0.0 && worstCaseProfit <= 0.0 ->
        "CRITICAL PROFIT ALERT: ${allowances.planName} worst-case Gemini + operating cost ($${formatUsd(worstCaseGeminiCostUsd + allowances.estimatedOperatingOverheadUsd)}) exceeds net Play revenue ($${formatUsd(netRev)}). Current user allowances are preserved; adjust future billing period allowances."
      budgetUtilizationPct >= adminCfg.criticalThresholdPercent ->
        "ADMIN WARNING (95%+): ${allowances.planName} max Gemini spend ($${formatUsd(worstCaseGeminiCostUsd)}/mo) is at $budgetUtilizationPct% of the $${formatUsd(configuredBudget)} admin budget."
      budgetUtilizationPct >= adminCfg.alertThresholdPercent ->
        "ADMIN NOTICE (80%+): ${allowances.planName} max Gemini spend ($${formatUsd(worstCaseGeminiCostUsd)}/mo) is at $budgetUtilizationPct% of the $${formatUsd(configuredBudget)} admin budget."
      else -> null
    }

    val recommendation = when (allowances.tierCode) {
      "free" ->
        "Zero-cost Free tier: $0.00 AI spend per Free user. AI features, structured lessons, and roleplay require LingoSphere Pro."
      "premium_monthly" ->
        "Verified profitable ($${formatUsd(worstCaseProfit)} min net profit, $worstCaseMarginPct%–$typicalMarginPct% margin): $9.99 gross → $${formatUsd(netRev)} after 15% Google Play fee vs. $${formatUsd(worstCaseGeminiCostUsd)} max Gemini cost."
      else ->
        "Verified high-margin Pro tier ($${formatUsd(worstCaseProfit)} min net profit, $worstCaseMarginPct%–$typicalMarginPct% margin): net $${formatUsd(netRev)} after 15% Google Play fee vs. $${formatUsd(worstCaseGeminiCostUsd)} max Gemini cost."
    }

    return PlanProfitabilityReport(
      tierCode = allowances.tierCode,
      planName = allowances.planName,
      retailPriceUsd = allowances.monthlyPriceUsd,
      googlePlayFeeUsd = allowances.googlePlayFeeUsd,
      netRevenueAfterPlayFeeUsd = netRev,
      worstCaseMaxGeminiCostUsd = worstCaseGeminiCostUsd,
      typicalUserGeminiCostUsd = typicalGeminiCostUsd,
      configuredGeminiBudgetUsd = configuredBudget,
      operatingOverheadUsd = allowances.estimatedOperatingOverheadUsd,
      worstCaseNetProfitUsd = worstCaseProfit,
      typicalNetProfitUsd = typicalProfit,
      worstCaseProfitMarginPercent = worstCaseMarginPct,
      typicalProfitMarginPercent = typicalMarginPct,
      isProfitableAtMaxUsage = isProfitable,
      adminAlertMessage = adminAlert,
      futureBillingRecommendation = recommendation
    )
  }

  fun evaluateAllPlansProfitability(): List<PlanProfitabilityReport> {
    return listOf(
      evaluatePlanProfitability(FREE_ALLOWANCES),
      evaluatePlanProfitability(PREMIUM_MONTHLY_ALLOWANCES),
      evaluatePlanProfitability(PREMIUM_PRO_999_ALLOWANCES)
    )
  }

  /**
   * Checks if the active user's actual Gemini API spend has reached 80% or 95% of their plan's
   * configured Gemini budget.
   */
  fun getUserBudgetWarningMessage(): String? {
    val snap = _usageState.value
    val cfg = _adminConfigState.value
    val budget = when (snap.subscriptionTier) {
      "free" -> cfg.freePlanMaxGeminiBudgetUsd
      "premium_monthly" -> cfg.premiumPlanMaxGeminiBudgetUsd
      else -> cfg.proPlanMaxGeminiBudgetUsd
    }.coerceAtLeast(0.01)

    val pct = ((snap.monthlyEstimatedCostUsd / budget) * 100.0).roundToInt()
    return when {
      pct >= cfg.criticalThresholdPercent ->
        "Admin Cost Guard: Account Gemini spend ($${formatUsd(snap.monthlyEstimatedCostUsd)}) is at $pct% of the $${formatUsd(budget)} plan budget. Advertised user allowances remain active; review next-cycle thresholds."
      pct >= cfg.alertThresholdPercent ->
        "Admin Cost Guard: Account Gemini spend ($${formatUsd(snap.monthlyEstimatedCostUsd)}) reached $pct% of the $${formatUsd(budget)} plan budget."
      else -> null
    }
  }

  private fun normalizePeriodResets(snapshot: AccountAiUsageSnapshot): AccountAiUsageSnapshot {
    val monthIso = currentMonthIso()
    val dayIso = currentDayIso()
    var result = snapshot
    if (result.currentMonthKey != monthIso) {
      result = result.copy(
        currentMonthKey = monthIso,
        monthlyAiVoiceSecondsUsed = 0,
        monthlyAiVideoSecondsUsed = 0,
        monthlyInputTokens = 0L,
        monthlyOutputTokens = 0L,
        monthlyAudioInputSeconds = 0,
        monthlyAudioOutputSeconds = 0,
        monthlyFlashLiteCalls = 0,
        monthlyFlashStandardCalls = 0,
        monthlyEstimatedCostUsd = 0.0,
        featureCostBreakdownUsd = emptyMap(),
        compressedHistoryTokensSaved = 0L
      )
    }
    if (result.currentDayKey != dayIso) {
      result = result.copy(
        currentDayKey = dayIso,
        dailyGrammarChecksUsed = 0,
        dailyPronunciationChecksUsed = 0,
        dailyIntelligentStudioUsed = 0,
        dailyHumanCallSecondsUsed = 0
      )
    }
    return result
  }

  private fun loadSnapshotFromPrefs(userId: String, tier: String): AccountAiUsageSnapshot {
    val p = prefs ?: return AccountAiUsageSnapshot(
      userId = userId,
      subscriptionTier = tier,
      currentMonthKey = currentMonthIso(),
      currentDayKey = currentDayIso()
    )
    val raw = p.getString("$KEY_USAGE_PREFIX$userId", null)
      ?: return AccountAiUsageSnapshot(
        userId = userId,
        subscriptionTier = tier,
        currentMonthKey = currentMonthIso(),
        currentDayKey = currentDayIso()
      )
    return runCatching {
      val obj = JSONObject(raw)
      val breakdownObj = obj.optJSONObject("featureCostBreakdownUsd")
      val breakdownMap = mutableMapOf<String, Double>()
      if (breakdownObj != null) {
        val keys = breakdownObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          breakdownMap[k] = breakdownObj.optDouble(k, 0.0)
        }
      }
      AccountAiUsageSnapshot(
        userId = obj.optString("userId", userId),
        subscriptionTier = tier.ifBlank { obj.optString("subscriptionTier", "free") },
        currentMonthKey = obj.optString("currentMonthKey", currentMonthIso()),
        currentDayKey = obj.optString("currentDayKey", currentDayIso()),
        monthlyAiVoiceSecondsUsed = obj.optInt("monthlyAiVoiceSecondsUsed", 0),
        monthlyAiVideoSecondsUsed = obj.optInt("monthlyAiVideoSecondsUsed", 0),
        dailyGrammarChecksUsed = obj.optInt("dailyGrammarChecksUsed", 0),
        dailyPronunciationChecksUsed = obj.optInt("dailyPronunciationChecksUsed", 0),
        dailyIntelligentStudioUsed = obj.optInt("dailyIntelligentStudioUsed", 0),
        dailyHumanCallSecondsUsed = obj.optInt("dailyHumanCallSecondsUsed", 0),
        monthlyInputTokens = obj.optLong("monthlyInputTokens", 0L),
        monthlyOutputTokens = obj.optLong("monthlyOutputTokens", 0L),
        monthlyAudioInputSeconds = obj.optInt("monthlyAudioInputSeconds", 0),
        monthlyAudioOutputSeconds = obj.optInt("monthlyAudioOutputSeconds", 0),
        monthlyFlashLiteCalls = obj.optInt("monthlyFlashLiteCalls", 0),
        monthlyFlashStandardCalls = obj.optInt("monthlyFlashStandardCalls", 0),
        monthlyEstimatedCostUsd = obj.optDouble("monthlyEstimatedCostUsd", 0.0),
        featureCostBreakdownUsd = breakdownMap,
        compressedHistoryTokensSaved = obj.optLong("compressedHistoryTokensSaved", 0L),
        updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
      )
    }.getOrDefault(
      AccountAiUsageSnapshot(
        userId = userId,
        subscriptionTier = tier,
        currentMonthKey = currentMonthIso(),
        currentDayKey = currentDayIso()
      )
    )
  }

  private fun saveSnapshotToPrefs(snapshot: AccountAiUsageSnapshot) {
    val p = prefs ?: return
    val breakdownObj = JSONObject()
    snapshot.featureCostBreakdownUsd.forEach { (k, v) -> breakdownObj.put(k, v) }
    val obj = JSONObject().apply {
      put("userId", snapshot.userId)
      put("subscriptionTier", snapshot.subscriptionTier)
      put("currentMonthKey", snapshot.currentMonthKey)
      put("currentDayKey", snapshot.currentDayKey)
      put("monthlyAiVoiceSecondsUsed", snapshot.monthlyAiVoiceSecondsUsed)
      put("monthlyAiVideoSecondsUsed", snapshot.monthlyAiVideoSecondsUsed)
      put("dailyGrammarChecksUsed", snapshot.dailyGrammarChecksUsed)
      put("dailyPronunciationChecksUsed", snapshot.dailyPronunciationChecksUsed)
      put("dailyIntelligentStudioUsed", snapshot.dailyIntelligentStudioUsed)
      put("dailyHumanCallSecondsUsed", snapshot.dailyHumanCallSecondsUsed)
      put("monthlyInputTokens", snapshot.monthlyInputTokens)
      put("monthlyOutputTokens", snapshot.monthlyOutputTokens)
      put("monthlyAudioInputSeconds", snapshot.monthlyAudioInputSeconds)
      put("monthlyAudioOutputSeconds", snapshot.monthlyAudioOutputSeconds)
      put("monthlyFlashLiteCalls", snapshot.monthlyFlashLiteCalls)
      put("monthlyFlashStandardCalls", snapshot.monthlyFlashStandardCalls)
      put("monthlyEstimatedCostUsd", snapshot.monthlyEstimatedCostUsd)
      put("featureCostBreakdownUsd", breakdownObj)
      put("compressedHistoryTokensSaved", snapshot.compressedHistoryTokensSaved)
      put("updatedAtEpochMs", snapshot.updatedAtEpochMs)
    }
    p.edit().putString("$KEY_USAGE_PREFIX${snapshot.userId}", obj.toString()).apply()
  }

  private fun loadAdminConfig() {
    val p = prefs ?: return
    val raw = p.getString(KEY_ADMIN_CONFIG, null) ?: return
    runCatching {
      val obj = JSONObject(raw)
      _adminConfigState.value = AdminProfitProtectionConfig(
        freePlanMaxGeminiBudgetUsd = obj.optDouble("freePlanMaxGeminiBudgetUsd", 0.12),
        premiumPlanMaxGeminiBudgetUsd = obj.optDouble("premiumPlanMaxGeminiBudgetUsd", 1.25),
        proPlanMaxGeminiBudgetUsd = obj.optDouble("proPlanMaxGeminiBudgetUsd", 2.85),
        globalMonthlyGeminiBudgetUsd = obj.optDouble("globalMonthlyGeminiBudgetUsd", 250.0),
        alertThresholdPercent = obj.optInt("alertThresholdPercent", 80),
        criticalThresholdPercent = obj.optInt("criticalThresholdPercent", 95),
        contextCompressionEnabled = obj.optBoolean("contextCompressionEnabled", true),
        smartModelRoutingEnabled = obj.optBoolean("smartModelRoutingEnabled", true),
        maxHistoryTurnsKeptVerbatim = obj.optInt("maxHistoryTurnsKeptVerbatim", 4)
      )
    }
  }

  private fun currentMonthIso(): String {
    return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
  }

  private fun currentDayIso(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }

  fun formatUsd(amount: Double): String {
    return String.format(Locale.US, "%.3f", amount)
  }
}
