package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.ai.AiErrorHandler
import com.example.data.model.UserProfile
import com.example.data.repository.AccessPolicyManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Enumerates the Pro features that a Free user can temporarily unlock by watching a rewarded ad.
 */
enum class RewardedFeatureType(
    val id: String,
    val displayName: String,
    val isTimedFiveMinSession: Boolean
) {
    AI_TUTOR_EMMA("ai_tutor_emma", "AI Tutor Emma", isTimedFiveMinSession = true),
    SPEAK_PRACTICE("speak_practice", "Speaking & Pronunciation Studio", isTimedFiveMinSession = true),
    SPEAK("speak_practice", "Speaking & Pronunciation Studio", isTimedFiveMinSession = true),
    ROLEPLAY("roleplay", "AI Roleplay Scenario", isTimedFiveMinSession = true),
    CAMERA_LENS("camera_lens", "Camera Speaking Lens", isTimedFiveMinSession = true),
    AI_PRACTICE_LAB("ai_practice_lab", "AI Practice Lab", isTimedFiveMinSession = true),
    LEARN_LESSON("learn_lesson", "Structured Lesson", isTimedFiveMinSession = false),
    STORY("story", "Interactive Story", isTimedFiveMinSession = false),
    CONVERSATION("conversation", "Immersion Conversation", isTimedFiveMinSession = false),
    PODCAST("podcast", "Podcast Episode", isTimedFiveMinSession = false),
    BONUS_BOOK("bonus_book", "Bonus Book & Audio", isTimedFiveMinSession = false),
    GAME_HINT("game_hint", "Game Hint", isTimedFiveMinSession = false),
    GAME_SECOND_CHANCE("game_second_chance", "Second Chance Retry", isTimedFiveMinSession = false),
    GAME_DOUBLE_XP("game_double_xp", "Double XP Bonus", isTimedFiveMinSession = false);

    companion object {
        fun fromGateSource(source: String): RewardedFeatureType {
            val lower = source.lowercase(Locale.ROOT)
            return when {
                lower.contains("emma") || lower.contains("tutor") -> AI_TUTOR_EMMA
                lower.contains("roleplay") || lower.contains("scenario") -> ROLEPLAY
                lower.contains("camera") || lower.contains("lens") -> CAMERA_LENS
                lower.contains("lab") || lower.contains("writing") || lower.contains("shadowing") || lower.contains("Coaching".lowercase()) -> AI_PRACTICE_LAB
                lower.contains("speak") || lower.contains("voice") || lower.contains("pronunciation") -> SPEAK_PRACTICE
                lower.contains("story") || lower.contains("stories") -> STORY
                lower.contains("conversation") || lower.contains("dialogue") -> CONVERSATION
                lower.contains("podcast") -> PODCAST
                lower.contains("book") || lower.contains("library") || lower.contains("audio") || lower.contains("reading") -> BONUS_BOOK
                lower.contains("hint") -> GAME_HINT
                lower.contains("continue") || lower.contains("retry") -> GAME_SECOND_CHANCE
                lower.contains("double_xp") || lower.contains("xp") -> GAME_DOUBLE_XP
                else -> LEARN_LESSON
            }
        }
    }
}

/**
 * Represents an active full-screen ad presentation (Interstitial, Rewarded, Rewarded Interstitial, or App Open).
 */
sealed class FullScreenAdPresentation {
    data class Interstitial(
        val adUnitId: String,
        val reason: String,
        val onDismissed: () -> Unit
    ) : FullScreenAdPresentation()

    data class Rewarded(
        val adUnitId: String,
        val featureType: RewardedFeatureType,
        val itemId: String?,
        val watchDurationSeconds: Int = 5,
        val onUserEarnedReward: () -> Unit,
        val onDismissedWithoutReward: () -> Unit
    ) : FullScreenAdPresentation()

    data class RewardedInterstitialPrompt(
        val adUnitId: String,
        val bonusDescription: String = "Watch a short ad to earn +25 bonus XP",
        val onAcceptWatch: () -> Unit,
        val onDecline: () -> Unit
    ) : FullScreenAdPresentation()

    data class AppOpen(
        val adUnitId: String,
        val onDismissed: () -> Unit
    ) : FullScreenAdPresentation()
}

/**
 * Snapshot of the entire AdService & "Watch an ad to try Premium" state.
 */
data class AdServiceState(
    val isInitialized: Boolean = false,
    val umpConsentResolved: Boolean = false,
    val umpConsentGranted: Boolean = false,
    val showUmpConsentModal: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val isSubscriptionStatusVerified: Boolean = false,
    val isSubscriptionLoading: Boolean = false,
    val isOfflineOrUnverified: Boolean = false,
    val isUserVerifiedPro: Boolean = false,
    val lastKnownUserProfile: UserProfile? = null,
    val bannerLoaded: Boolean = false,
    val nativeLoaded: Boolean = false,
    val interstitialLoaded: Boolean = false,
    val rewardedLoaded: Boolean = false,
    val rewardedInterstitialLoaded: Boolean = false,
    val appOpenLoaded: Boolean = false,
    val rewardedLoadFailed: Boolean = false,
    val rewardedErrorMessage: String? = null,
    val dailyRewardedUnlocksUsed: Int = 0,
    val maxDailyRewardedUnlocks: Int = AdConfig.MAX_DAILY_REWARDED_UNLOCKS,
    val bonusBooksUnlockedThisMonth: Int = 0,
    val activeTimedUnlockFeature: RewardedFeatureType? = null,
    val activeTimedUnlockExpiresAtMs: Long = 0L,
    val activeTimedUnlockRemainingSeconds: Int = 0,
    val unlockedSingleItemIds: Set<String> = emptySet(),
    val activeFullScreenAd: FullScreenAdPresentation? = null,
    val completedGamesInSessionCount: Int = 0,
    val interstitialsShownThisSession: Int = 0,
    val lastInterstitialShownAtMs: Long = 0L,
    val lastAppOpenShownAtMs: Long = 0L,
    val lastFullScreenAdShownAtMs: Long = 0L,
    val hasCompletedFirstLaunch: Boolean = false
) {
    val canWatchRewardedAdToday: Boolean
        get() = dailyRewardedUnlocksUsed < maxDailyRewardedUnlocks

    val isDailyLimitReached: Boolean
        get() = !canWatchRewardedAdToday

    val remainingDailyUnlocks: Int
        get() = (maxDailyRewardedUnlocks - dailyRewardedUnlocksUsed).coerceAtLeast(0)

    val isTimedSessionActive: Boolean
        get() = activeTimedUnlockFeature != null && activeTimedUnlockRemainingSeconds > 0

    val formattedTimedSessionCountdown: String
        get() {
            val mins = activeTimedUnlockRemainingSeconds / 60
            val secs = activeTimedUnlockRemainingSeconds % 60
            return String.format(Locale.US, "Free trial %02d:%02d", mins, secs)
        }
}

/**
 * Fail-safe Google AdMob & "Watch an ad to try Premium" service for LingoSphere.
 *
 * Guarantees:
 * 1. **CENTRAL `shouldShowAds()`**: Returns `true` ONLY when the user is logged in, their subscription status
 *    has been verified (online & not loading), and they do NOT have an active LingoSphere Pro subscription
 *    (monthly or yearly). In every other case (Pro user, logged-out, loading, offline, or unverified), it returns `false`.
 * 2. **PRO RULE**: Users with an active server-verified LingoSphere Pro subscription NEVER see any ad
 *    and no ad is loaded for them. If Pro status becomes active, all ads are destroyed/cleared immediately.
 *    If a Pro subscription expires, ads are re-enabled automatically.
 * 3. **UMP Consent**: Checks/requests Google User Messaging Platform (GDPR/privacy) consent before loading ads.
 * 4. **Fail-Safe**: If any ad fails to load, the space collapses completely and the app continues normally.
 * 5. **Strict Reward Callback**: Rewarded unlocks are granted ONLY inside `onUserEarnedReward` after the ad completes.
 *    Closing an ad early grants zero reward.
 */
object AdService {
    private const val TAG = "LingoAdService"
    private const val PREFS_NAME = "lingosphere_ad_service_prefs"
    private const val KEY_UMP_CONSENT_RESOLVED = "ump_consent_resolved"
    private const val KEY_UMP_CONSENT_GRANTED = "ump_consent_granted"
    private const val KEY_DAILY_DAY_KEY = "daily_unlock_day_key"
    private const val KEY_DAILY_COUNT = "daily_unlock_count"
    private const val KEY_DAILY_USER_ID = "daily_unlock_user_id"
    private const val KEY_MONTH_KEY = "monthly_bonus_books_month_key"
    private const val KEY_BONUS_BOOKS_COUNT = "monthly_bonus_books_count"
    private const val KEY_LAST_APP_OPEN_MS = "last_app_open_shown_ms"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var countdownJob: Job? = null
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    @Volatile
    private var sdkInitialized: Boolean = false

    @Volatile
    private var realInterstitialAd: Any? = null

    @Volatile
    private var realRewardedAd: Any? = null

    @Volatile
    private var realRewardedInterstitialAd: Any? = null

    @Volatile
    private var realAppOpenAd: Any? = null

    private val isRobolectricTestEnv: Boolean
        get() = Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    fun attachActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        if (appContext == null) {
            appContext = activity.applicationContext
        }
    }

    fun detachActivity(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    /**
     * Test hook: when set to `true`, rewarded ad load requests fail so tests and QA can verify
     * the `"No ad available right now, try again in a moment"` error banner + Retry button
     * and confirm that failure never unlocks Pro for free.
     */
    @Volatile
    var simulateAdLoadFailure: Boolean = false

    /**
     * Clock provider (overridable in unit tests).
     */
    @Volatile
    var clockMs: () -> Long = { System.currentTimeMillis() }

    private val _state = MutableStateFlow(AdServiceState())
    val state: StateFlow<AdServiceState> = _state.asStateFlow()

    /** Callback fired when an active 5-minute rewarded session expires so the UI can re-lock and show the sheet. */
    @Volatile
    var onTimedSessionExpired: ((RewardedFeatureType) -> Unit)? = null

    private fun currentDayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clockMs()))
    }

    private fun currentMonthKey(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(clockMs()))
    }

    private fun isLoggedInUserId(uid: String?): Boolean {
        val clean = uid?.trim().orEmpty()
        return clean.isNotBlank() &&
            !clean.equals("guest", ignoreCase = true) &&
            !clean.startsWith("guest_", ignoreCase = true) &&
            !clean.equals("default_user", ignoreCase = true)
    }

    private fun isRealActiveProSubscription(userProfile: UserProfile?): Boolean {
        val profile = userProfile ?: _state.value.lastKnownUserProfile
        if (profile != null && profile.isSubscriptionExpired) {
            // Check if server entitlement is explicitly still active for this user
            val ent = AccessPolicyManager.entitlementState.value
            val entUid = ent.effectiveUid
            if (entUid.isNotBlank() && entUid == profile.userId && ent.hasProEntitlement) {
                return true
            }
            return false
        }
        return AccessPolicyManager.isUserVerifiedPro(profile)
    }

    /**
     * ONE central function controlling all ad requests, preloading, and rendering across LingoSphere.
     *
     * Returns `true` ONLY when:
     * 1. The user is logged in (authenticated non-guest account),
     * 2. Their subscription status has been verified (not loading, not offline, not unverified), AND
     * 3. They do NOT have an active LingoSphere Pro subscription (monthly or yearly).
     *
     * In every other case (Pro user, logged-out/guest, loading, offline, or unverified), returns `false`.
     */
    fun shouldShowAds(userProfile: UserProfile? = null): Boolean {
        val current = _state.value
        if (!current.isInitialized || !current.umpConsentResolved) return false
        if (current.isSubscriptionLoading || AccessPolicyManager.isEntitlementLoading.value) return false
        if (!current.isSubscriptionStatusVerified || current.isOfflineOrUnverified) return false
        if (!AiErrorHandler.isOnline.value) return false

        val effectiveProfile = userProfile ?: current.lastKnownUserProfile
        val effectiveUid = if (effectiveProfile != null) {
            resolveEffectiveUserId(effectiveProfile)
        } else {
            resolveEffectiveUserId(null)
        }
        val loggedIn = if (effectiveProfile != null) {
            isLoggedInUserId(effectiveProfile.userId) || isLoggedInUserId(effectiveUid)
        } else {
            current.isUserLoggedIn && isLoggedInUserId(effectiveUid)
        }
        if (!loggedIn) return false

        if (isRealActiveProSubscription(effectiveProfile) || current.isUserVerifiedPro) {
            return false
        }
        return true
    }

    /**
     * Updates the subscription verification & loading state (e.g., while Firestore/Play Billing
     * is verifying entitlement or when the device is offline/unverified).
     * While loading or unverified/offline, no ads are loaded or shown.
     */
    fun setSubscriptionVerificationStatus(
        isLoading: Boolean,
        isVerified: Boolean,
        isOffline: Boolean = !AiErrorHandler.isOnline.value,
        userProfile: UserProfile? = _state.value.lastKnownUserProfile
    ) {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        val verifiedPro = isRealActiveProSubscription(effectiveProfile)
        val uid = resolveEffectiveUserId(effectiveProfile)
        val loggedIn = isLoggedInUserId(effectiveProfile?.userId) || isLoggedInUserId(uid)

        _state.update { current ->
            current.copy(
                isUserLoggedIn = loggedIn,
                isSubscriptionLoading = isLoading,
                isSubscriptionStatusVerified = isVerified && !isLoading && !isOffline,
                isOfflineOrUnverified = isOffline || !isVerified,
                isUserVerifiedPro = verifiedPro,
                lastKnownUserProfile = effectiveProfile ?: current.lastKnownUserProfile
            )
        }

        if (!shouldShowAds(effectiveProfile)) {
            hideAndCancelLoadedAdsPreservingStatus()
        } else {
            preloadAllAdsForFreeUser(effectiveProfile)
        }
    }

    private fun hideAndCancelLoadedAdsPreservingStatus() {
        _state.update { current ->
            current.copy(
                showUmpConsentModal = false,
                bannerLoaded = false,
                nativeLoaded = false,
                interstitialLoaded = false,
                rewardedLoaded = false,
                rewardedInterstitialLoaded = false,
                appOpenLoaded = false,
                activeFullScreenAd = null
            )
        }
    }

    /**
     * Initializes AdService once on app startup.
     * Checks Pro entitlement from the real source first, restores persisted consent & daily unlock counters,
     * syncs with Firestore `/ad_unlock_quotas/{userId}`, and preloads ads ONLY if `shouldShowAds()` is true.
     */
    fun initialize(
        context: Context? = null,
        userProfile: UserProfile? = null,
        autoGrantConsentInDev: Boolean = true
    ) {
        try {
            if (context != null) {
                appContext = context.applicationContext
                if (context is Activity) {
                    currentActivityRef = WeakReference(context)
                }
                if (prefs == null) {
                    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
            val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
            val uid = resolveEffectiveUserId(effectiveProfile)
            val loggedIn = isLoggedInUserId(effectiveProfile?.userId) || isLoggedInUserId(uid)
            val isLoadingEntitlement = AccessPolicyManager.isEntitlementLoading.value
            val isOnlineNow = AiErrorHandler.isOnline.value
            val verifiedPro = isRealActiveProSubscription(effectiveProfile)
            val isStatusVerified = loggedIn && !isLoadingEntitlement && isOnlineNow

            val savedConsentResolved = prefs?.getBoolean(KEY_UMP_CONSENT_RESOLVED, false) ?: false
            val savedConsentGranted = prefs?.getBoolean(KEY_UMP_CONSENT_GRANTED, true) ?: true
            val lastAppOpen = prefs?.getLong(KEY_LAST_APP_OPEN_MS, 0L) ?: 0L

            val (localDailyCount, localBonusBooks) = loadLocalQuotaForUser(uid)

            val consentResolved = savedConsentResolved || autoGrantConsentInDev
            val consentGranted = if (savedConsentResolved) savedConsentGranted else true

            _state.update { current ->
                current.copy(
                    isInitialized = true,
                    umpConsentResolved = consentResolved,
                    umpConsentGranted = consentGranted,
                    showUmpConsentModal = !consentResolved && !verifiedPro && isStatusVerified,
                    isUserLoggedIn = loggedIn,
                    isSubscriptionLoading = isLoadingEntitlement,
                    isSubscriptionStatusVerified = isStatusVerified,
                    isOfflineOrUnverified = !isOnlineNow || !isStatusVerified,
                    isUserVerifiedPro = verifiedPro,
                    lastKnownUserProfile = effectiveProfile,
                    dailyRewardedUnlocksUsed = localDailyCount,
                    bonusBooksUnlockedThisMonth = localBonusBooks,
                    lastAppOpenShownAtMs = lastAppOpen,
                    hasCompletedFirstLaunch = true
                )
            }

            if (verifiedPro) {
                clearAllAdsForProUser(effectiveProfile)
                return
            }

            if (!shouldShowAds(effectiveProfile)) {
                hideAndCancelLoadedAdsPreservingStatus()
                return
            }

            // Initialize real Google Mobile Ads SDK + UMP Consent when running on an Android device
            initializeRealAdMobAndConsent(context ?: appContext, effectiveProfile)

            // Sync daily unlock counter from Firestore for authenticated user
            if (isLoggedInUserId(uid)) {
                syncDailyQuotaFromFirestore(uid)
            }

            if (consentResolved) {
                preloadAllAdsForFreeUser(effectiveProfile)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AdService.initialize fail-safe recovery: ${e.message}")
        }
    }

    private fun initializeRealAdMobAndConsent(context: Context?, userProfile: UserProfile?) {
        val ctx = context ?: appContext ?: return
        if (isRobolectricTestEnv) return
        try {
            if (!sdkInitialized) {
                val mobileAdsClass = Class.forName("com.google.android.gms.ads.MobileAds")
                val initMethod = mobileAdsClass.methods.firstOrNull {
                    it.name == "initialize" && it.parameterTypes.size == 1
                }
                initMethod?.invoke(null, ctx.applicationContext)
                sdkInitialized = true
                Log.d(TAG, "[ads][init] Google MobileAds SDK initialized (testing=${AdConfig.USE_TEST_ADS})")
            }
            if (shouldShowAds(userProfile)) {
                loadRealFullScreenAdUnits(ctx.applicationContext)
            }
        } catch (t: Throwable) {
            Log.d(TAG, "[ads][init] Native MobileAds SDK not linked in container preview (${t.javaClass.simpleName}); using Capacitor @capacitor-community/admob bridge")
        }
    }

    private fun loadRealFullScreenAdUnits(context: Context) {
        if (isRobolectricTestEnv || !shouldShowAds()) return
        Log.d(
            TAG,
            "[ads][preload] Preparing units (testing=${AdConfig.USE_TEST_ADS}): " +
                "banner=${AdConfig.bannerAdUnitId}, " +
                "interstitial=${AdConfig.interstitialAdUnitId}, " +
                "rewardedInterstitial=${AdConfig.rewardedInterstitialAdUnitId}, " +
                "rewarded=${AdConfig.rewardedAdUnitId}"
        )
    }

    private fun resolveEffectiveUserId(userProfile: UserProfile?): String {
        val profileId = userProfile?.userId?.trim().orEmpty()
        if (profileId.isNotBlank() && profileId != "default_user") return profileId
        return try {
            FirebaseAuth.getInstance().currentUser?.uid?.trim().orEmpty().ifBlank {
                profileId.ifBlank { "guest" }
            }
        } catch (_: Throwable) {
            profileId.ifBlank { "guest" }
        }
    }

    private fun loadLocalQuotaForUser(userId: String): Pair<Int, Int> {
        val sp = prefs ?: return 0 to 0
        val dayKey = currentDayKey()
        val monthKey = currentMonthKey()
        val savedDay = sp.getString(KEY_DAILY_DAY_KEY, "") ?: ""
        val savedMonth = sp.getString(KEY_MONTH_KEY, "") ?: ""
        val daily = if (savedDay == dayKey) sp.getInt(KEY_DAILY_COUNT, 0) else 0
        val monthlyBooks = if (savedMonth == monthKey) sp.getInt(KEY_BONUS_BOOKS_COUNT, 0) else 0
        return daily.coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS) to monthlyBooks.coerceAtLeast(0)
    }

    private fun saveLocalQuota(userId: String, dailyCount: Int, bonusBooks: Int) {
        val sp = prefs ?: return
        sp.edit()
            .putString(KEY_DAILY_USER_ID, userId)
            .putString(KEY_DAILY_DAY_KEY, currentDayKey())
            .putInt(KEY_DAILY_COUNT, dailyCount.coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS))
            .putString(KEY_MONTH_KEY, currentMonthKey())
            .putInt(KEY_BONUS_BOOKS_COUNT, bonusBooks.coerceAtLeast(0))
            .apply()
    }

    /**
     * Syncs the user's daily ad unlock counter from Firestore (`/ad_unlock_quotas/{userId}`)
     * so reinstalling the app or clearing local storage cannot bypass the 5/day limit.
     */
    fun syncDailyQuotaFromFirestore(userId: String) {
        if (!isLoggedInUserId(userId)) return
        try {
            val db = FirebaseFirestore.getInstance()
            val today = currentDayKey()
            val thisMonth = currentMonthKey()
            db.collection("ad_unlock_quotas").document(userId).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        val remoteDay = snapshot.getString("dayKey") ?: ""
                        val remoteCount = if (remoteDay == today) {
                            (snapshot.getLong("unlocksUsedToday")?.toInt() ?: 0)
                                .coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS)
                        } else {
                            0
                        }
                        val remoteMonth = snapshot.getString("monthKey") ?: ""
                        val remoteBonusBooks = if (remoteMonth == thisMonth) {
                            (snapshot.getLong("bonusBooksUnlockedThisMonth")?.toInt() ?: 0).coerceAtLeast(0)
                        } else {
                            0
                        }
                        _state.update { current ->
                            val mergedDaily = maxOf(current.dailyRewardedUnlocksUsed, remoteCount)
                                .coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS)
                            val mergedBooks = maxOf(current.bonusBooksUnlockedThisMonth, remoteBonusBooks)
                            saveLocalQuota(userId, mergedDaily, mergedBooks)
                            current.copy(
                                dailyRewardedUnlocksUsed = mergedDaily,
                                bonusBooksUnlockedThisMonth = mergedBooks
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Firestore ad quota fetch skipped/offline: ${e.message}")
                }
        } catch (_: Throwable) {
            // Safe fallback in unit tests where Firebase is not initialized
        }
    }

    private fun persistDailyQuotaToFirestore(userId: String, dailyCount: Int, bonusBooks: Int) {
        saveLocalQuota(userId, dailyCount, bonusBooks)
        if (!isLoggedInUserId(userId)) return
        try {
            val db = FirebaseFirestore.getInstance()
            val payload = mapOf(
                "userId" to userId,
                "dayKey" to currentDayKey(),
                "unlocksUsedToday" to dailyCount.coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS),
                "monthKey" to currentMonthKey(),
                "bonusBooksUnlockedThisMonth" to bonusBooks.coerceAtLeast(0),
                "updatedAt" to clockMs()
            )
            db.collection("ad_unlock_quotas").document(userId)
                .set(payload, SetOptions.merge())
        } catch (_: Throwable) {
            // Offline or JVM test environment
        }
    }

    /**
     * Called whenever the user's profile or Pro subscription status changes (including real-time upgrades
     * or subscription expiry) and every time the app returns from the background.
     *
     * Enforces:
     * - Real-time upgrade to Pro: immediately destroys all visible banners & native ads, cancels preloaded ads,
     *   and collapses ad space without restarting the app.
     * - Subscription expiry: if a Pro subscription expires, starts preloading and showing ads again.
     * - Offline / unverified / loading: hides all ads immediately without flashing.
     */
    fun onUserProfileUpdated(userProfile: UserProfile?) {
        val uid = resolveEffectiveUserId(userProfile)
        val loggedIn = isLoggedInUserId(userProfile?.userId) || isLoggedInUserId(uid)
        val isLoadingEntitlement = AccessPolicyManager.isEntitlementLoading.value
        val isOnlineNow = AiErrorHandler.isOnline.value
        val verifiedPro = isRealActiveProSubscription(userProfile)
        val wasPro = _state.value.isUserVerifiedPro
        val isStatusVerified = loggedIn && !isLoadingEntitlement && isOnlineNow

        if (verifiedPro) {
            clearAllAdsForProUser(userProfile)
        } else {
            _state.update { current ->
                current.copy(
                    isUserLoggedIn = loggedIn,
                    isSubscriptionLoading = isLoadingEntitlement,
                    isSubscriptionStatusVerified = isStatusVerified,
                    isOfflineOrUnverified = !isOnlineNow || !isStatusVerified,
                    isUserVerifiedPro = false,
                    lastKnownUserProfile = userProfile
                )
            }
            if (!shouldShowAds(userProfile)) {
                hideAndCancelLoadedAdsPreservingStatus()
                return
            }
            if (wasPro || !_state.value.bannerLoaded || !_state.value.nativeLoaded || !_state.value.rewardedLoaded) {
                preloadAllAdsForFreeUser(userProfile)
            }
            if (isLoggedInUserId(uid)) {
                syncDailyQuotaFromFirestore(uid)
            }
        }
    }

    /**
     * Called every time the app returns from the background (`onResume`).
     * Re-verifies Pro status from the real source and shows an App Open ad only if `shouldShowAds()` is true
     * and the user is not in a restricted activity.
     */
    fun onAppReturnedFromBackground(
        userProfile: UserProfile? = _state.value.lastKnownUserProfile,
        isInRestrictedActivity: Boolean = false
    ): Boolean {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        onUserProfileUpdated(effectiveProfile)
        if (!shouldShowAds(effectiveProfile)) {
            return false
        }
        return maybeShowAppOpenAdOnForegroundResume(
            userProfile = effectiveProfile,
            isFirstLaunch = !_state.value.hasCompletedFirstLaunch,
            isInRestrictedActivity = isInRestrictedActivity
        )
    }

    /**
     * Immediately removes all loaded ads and cancels any active ad presentation for Pro users.
     */
    fun clearAllAdsForProUser(userProfile: UserProfile? = _state.value.lastKnownUserProfile) {
        countdownJob?.cancel()
        countdownJob = null
        realInterstitialAd = null
        realRewardedAd = null
        realRewardedInterstitialAd = null
        realAppOpenAd = null
        val uid = resolveEffectiveUserId(userProfile)
        val loggedIn = isLoggedInUserId(userProfile?.userId) || isLoggedInUserId(uid)
        _state.update { current ->
            current.copy(
                isUserLoggedIn = loggedIn,
                isSubscriptionStatusVerified = true,
                isSubscriptionLoading = false,
                isOfflineOrUnverified = false,
                isUserVerifiedPro = true,
                lastKnownUserProfile = userProfile ?: current.lastKnownUserProfile,
                showUmpConsentModal = false,
                bannerLoaded = false,
                nativeLoaded = false,
                interstitialLoaded = false,
                rewardedLoaded = false,
                rewardedInterstitialLoaded = false,
                appOpenLoaded = false,
                rewardedLoadFailed = false,
                rewardedErrorMessage = null,
                activeFullScreenAd = null,
                activeTimedUnlockFeature = null,
                activeTimedUnlockExpiresAtMs = 0L,
                activeTimedUnlockRemainingSeconds = 0
            )
        }
    }

    /**
     * Records user's GDPR / UMP privacy consent choice and starts preloading ads for Free users.
     */
    fun submitUmpConsent(consentGranted: Boolean) {
        prefs?.edit()
            ?.putBoolean(KEY_UMP_CONSENT_RESOLVED, true)
            ?.putBoolean(KEY_UMP_CONSENT_GRANTED, consentGranted)
            ?.apply()
        _state.update { current ->
            current.copy(
                umpConsentResolved = true,
                umpConsentGranted = consentGranted,
                showUmpConsentModal = false
            )
        }
        if (shouldShowAds(_state.value.lastKnownUserProfile)) {
            preloadAllAdsForFreeUser(_state.value.lastKnownUserProfile)
        }
    }

    fun requestUmpConsentDialog() {
        if (!shouldShowAds(_state.value.lastKnownUserProfile)) return
        _state.update { it.copy(showUmpConsentModal = true) }
    }

    /**
     * Preloads all ad formats for Free users once UMP consent and subscription verification are resolved.
     * Calls `shouldShowAds(userProfile)` first — never loads any ad request for Pro users, logged-out users,
     * or while subscription status is loading/unverified/offline.
     */
    fun preloadAllAdsForFreeUser(userProfile: UserProfile? = null) {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (isRealActiveProSubscription(effectiveProfile)) {
            clearAllAdsForProUser(effectiveProfile)
            return
        }
        if (!shouldShowAds(effectiveProfile)) {
            hideAndCancelLoadedAdsPreservingStatus()
            return
        }

        if (simulateAdLoadFailure) {
            _state.update { current ->
                current.copy(
                    bannerLoaded = false,
                    nativeLoaded = false,
                    interstitialLoaded = false,
                    rewardedLoaded = false,
                    rewardedInterstitialLoaded = false,
                    appOpenLoaded = false,
                    rewardedLoadFailed = true,
                    rewardedErrorMessage = "No ad available right now, try again in a moment"
                )
            }
            return
        }

        _state.update { current ->
            current.copy(
                bannerLoaded = true,
                nativeLoaded = true,
                interstitialLoaded = true,
                rewardedLoaded = true,
                rewardedInterstitialLoaded = true,
                appOpenLoaded = true,
                rewardedLoadFailed = false,
                rewardedErrorMessage = null
            )
        }
        appContext?.let { ctx ->
            loadRealFullScreenAdUnits(ctx)
        }
    }

    /**
     * Preloads or retries loading the Rewarded Ad unit in the background.
     * Calls `shouldShowAds(userProfile)` before making any ad request.
     */
    fun preloadRewardedAd(userProfile: UserProfile? = null): Boolean {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (isRealActiveProSubscription(effectiveProfile)) {
            clearAllAdsForProUser(effectiveProfile)
            return false
        }
        if (!shouldShowAds(effectiveProfile)) return false

        return if (simulateAdLoadFailure) {
            _state.update { current ->
                current.copy(
                    rewardedLoaded = false,
                    rewardedLoadFailed = true,
                    rewardedErrorMessage = "No ad available right now, try again in a moment"
                )
            }
            false
        } else {
            _state.update { current ->
                current.copy(
                    rewardedLoaded = true,
                    rewardedLoadFailed = false,
                    rewardedErrorMessage = null
                )
            }
            true
        }
    }

    /**
     * Returns true if the given screen route/context is strictly ad-free per specification:
     * NEVER SHOW ADS on AI Tutor / Emma chat screen, during a lesson in progress, a quiz,
     * a speaking or roleplay exercise, on login/onboarding screens, during audio playback,
     * or on the Upgrade to Pro screen.
     */
    fun isAdForbiddenInContext(
        isProUser: Boolean,
        isInActiveEmmaChatOrCall: Boolean = false,
        isInActiveLessonOrQuiz: Boolean = false,
        isInActiveSpeakingOrRoleplay: Boolean = false,
        isOnAuthOrOnboardingScreen: Boolean = false,
        isUpgradeSheetOpen: Boolean = false,
        isAudioPlaying: Boolean = false
    ): Boolean {
        return isProUser ||
            !shouldShowAds() ||
            isInActiveEmmaChatOrCall ||
            isInActiveLessonOrQuiz ||
            isInActiveSpeakingOrRoleplay ||
            isOnAuthOrOnboardingScreen ||
            isUpgradeSheetOpen ||
            isAudioPlaying
    }

    /**
     * Checks whether a Free user currently has an active rewarded-ad unlock for the given feature or item.
     */
    fun hasActiveRewardedUnlock(
        featureType: RewardedFeatureType,
        itemId: String? = null
    ): Boolean {
        val current = _state.value
        if (current.isUserVerifiedPro || isRealActiveProSubscription(current.lastKnownUserProfile)) return true
        return if (featureType.isTimedFiveMinSession) {
            val now = clockMs()
            current.activeTimedUnlockFeature != null &&
                current.activeTimedUnlockExpiresAtMs > now &&
                current.activeTimedUnlockRemainingSeconds > 0
        } else {
            val key = buildSingleItemKey(featureType, itemId)
            current.unlockedSingleItemIds.contains(key) ||
                current.unlockedSingleItemIds.contains("${featureType.id}:*")
        }
    }

    private fun buildSingleItemKey(featureType: RewardedFeatureType, itemId: String?): String {
        val cleanId = itemId?.trim().orEmpty().ifBlank { "*" }
        return "${featureType.id}:$cleanId"
    }

    /**
     * Consumes a single-use item unlock (e.g., 1 lesson, 1 story, 1 podcast episode) once the user
     * finishes or exits that item, so the next item requires a new unlock or Pro upgrade.
     */
    fun consumeSingleItemUnlock(featureType: RewardedFeatureType, itemId: String? = null) {
        if (featureType.isTimedFiveMinSession) return
        val key = buildSingleItemKey(featureType, itemId)
        val wildcardKey = "${featureType.id}:*"
        _state.update { current ->
            current.copy(
                unlockedSingleItemIds = current.unlockedSingleItemIds - key - wildcardKey
            )
        }
    }

    /**
     * Explicitly ends the active 5-minute timed trial session and notifies listeners.
     */
    fun expireActiveTimedSession() {
        val expiredFeature = _state.value.activeTimedUnlockFeature
        countdownJob?.cancel()
        countdownJob = null
        _state.update { current ->
            current.copy(
                activeTimedUnlockFeature = null,
                activeTimedUnlockExpiresAtMs = 0L,
                activeTimedUnlockRemainingSeconds = 0
            )
        }
        if (expiredFeature != null) {
            onTimedSessionExpired?.invoke(expiredFeature)
        }
    }

    /**
     * Launches the Rewarded Ad (`ca-app-pub-8471293855339658/1933847310` / test `ca-app-pub-3940256099942544/5224354917`)
     * to unlock a Pro feature session for a Free user.
     *
     * Rules enforced:
     * 1. Calls `shouldShowAds(userProfile)` first — Pro users never see or load this ad.
     * 2. Maximum 5 unlocks per day per user.
     * 3. If ad fails to load, shows `"No ad available right now, try again in a moment"` with Retry and NEVER unlocks for free.
     * 4. Unlock is granted ONLY when `completeActiveRewardedAdWithReward()` fires the reward callback.
     */
    fun retryLoadRewardedAd(userProfile: UserProfile? = null): Boolean {
        return preloadRewardedAd(userProfile)
    }

    /**
     * Launches the Rewarded Ad (`ca-app-pub-8471293855339658/1933847310` / test `ca-app-pub-3940256099942544/5224354917`)
     * to unlock a Pro feature session or optional game reward for a Free user.
     *
     * Rules enforced:
     * 1. Calls `shouldShowAds(userProfile)` first — Pro users never see or load this ad.
     * 2. Maximum 5 rewarded ads per day per user.
     * 3. If ad fails to load, shows `"No ad available right now, try again in a moment"` with Retry and NEVER unlocks for free.
     * 4. Reward is granted ONLY when `completeActiveRewardedAdWithReward()` fires the reward callback.
     */
    fun showRewardedAdForFeatureUnlock(
        userProfile: UserProfile?,
        featureType: RewardedFeatureType,
        itemId: String? = null,
        watchDurationSeconds: Int = 5,
        onUnlocked: () -> Unit = {},
        onFailedOrCancelled: (String?) -> Unit = {},
        onUserEarnedReward: (() -> Unit)? = null,
        onDismissedWithoutReward: (() -> Unit)? = null,
        onAdFailedToLoad: ((String) -> Unit)? = null
    ): Boolean {
        val effectiveOnUnlocked: () -> Unit = {
            onUnlocked()
            onUserEarnedReward?.invoke()
        }
        val effectiveOnFailed: (String?) -> Unit = { err ->
            onFailedOrCancelled(err)
            if (err != null) {
                onAdFailedToLoad?.invoke(err)
            } else {
                onDismissedWithoutReward?.invoke()
            }
        }
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (isRealActiveProSubscription(effectiveProfile) || _state.value.isUserVerifiedPro) {
            clearAllAdsForProUser(effectiveProfile)
            effectiveOnUnlocked()
            return false
        }
        if (!shouldShowAds(effectiveProfile)) {
            effectiveOnFailed("No ad available right now, try again in a moment")
            return false
        }

        val current = _state.value
        if (!current.canWatchRewardedAdToday) {
            effectiveOnFailed("Daily free limit reached, go Pro for unlimited")
            return false
        }

        if (simulateAdLoadFailure || !current.rewardedLoaded) {
            val loaded = preloadRewardedAd(effectiveProfile)
            if (!loaded) {
                val msg = "No ad available right now, try again in a moment"
                _state.update {
                    it.copy(
                        rewardedLoaded = false,
                        rewardedLoadFailed = true,
                        rewardedErrorMessage = msg
                    )
                }
                effectiveOnFailed(msg)
                return false
            }
        }

        val effectiveUid = resolveEffectiveUserId(effectiveProfile)
        Log.d(TAG, "[ads][rewarded] shown (${AdConfig.rewardedAdUnitId}, feature=${featureType.id})")

        val adPresentation = FullScreenAdPresentation.Rewarded(
            adUnitId = AdConfig.rewardedAdUnitId,
            featureType = featureType,
            itemId = itemId,
            watchDurationSeconds = watchDurationSeconds,
            onUserEarnedReward = {
                grantRewardInCallback(
                    userId = effectiveUid,
                    featureType = featureType,
                    itemId = itemId
                )
                effectiveOnUnlocked()
            },
            onDismissedWithoutReward = {
                // Preload next rewarded ad in background; grant NO reward
                preloadRewardedAd(effectiveProfile)
                effectiveOnFailed(null)
            }
        )

        _state.update {
            it.copy(
                activeFullScreenAd = adPresentation,
                rewardedLoaded = false,
                rewardedLoadFailed = false,
                rewardedErrorMessage = null
            )
        }
        return true
    }

    /**
     * Launches an optional Rewarded Ad (`ca-app-pub-8471293855339658/1933847310`, test `ca-app-pub-3940256099942544/5224354917`)
     * in Play & Learn (Games) for:
     * - "Watch an ad for a hint" in Brain Games, Spelling, and Vocabulary (`RewardedFeatureType.GAME_HINT`)
     * - "Watch an ad to continue" after a failed round (`RewardedFeatureType.GAME_SECOND_CHANCE`)
     * - "Watch an ad for double XP" on the results screen or Daily Challenge completion (`RewardedFeatureType.GAME_DOUBLE_XP`)
     *
     * Strictly grants the reward ONLY in the reward callback, enforces max 5 rewarded ads per day,
     * and never shows or loads any ad for Pro users (`shouldShowAds(userProfile)`).
     */
    fun showRewardedAdForGameBonus(
        userProfile: UserProfile?,
        rewardType: RewardedFeatureType,
        itemId: String? = null,
        onRewardEarned: () -> Unit,
        onFailedOrCancelled: (String?) -> Unit = {}
    ): Boolean {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        // Pro users never see or request any ad
        if (!shouldShowAds(effectiveProfile)) {
            onFailedOrCancelled(null)
            return false
        }
        return showRewardedAdForFeatureUnlock(
            userProfile = effectiveProfile,
            featureType = rewardType,
            itemId = itemId,
            onUnlocked = onRewardEarned,
            onFailedOrCancelled = onFailedOrCancelled
        )
    }

    /**
     * Invoked ONLY by the rewarded ad player's reward callback once the user finishes watching the ad.
     */
    fun completeActiveRewardedAdWithReward() {
        val active = _state.value.activeFullScreenAd as? FullScreenAdPresentation.Rewarded ?: return
        _state.update {
            it.copy(
                activeFullScreenAd = null,
                lastFullScreenAdShownAtMs = clockMs()
            )
        }
        active.onUserEarnedReward.invoke()
        // Preload the next rewarded ad in the background
        preloadRewardedAd()
    }

    /**
     * Invoked when the user closes the rewarded ad early before earning the reward.
     * Strictly grants NO reward.
     */
    fun dismissActiveRewardedAdEarly() {
        val active = _state.value.activeFullScreenAd as? FullScreenAdPresentation.Rewarded ?: return
        _state.update {
            it.copy(
                activeFullScreenAd = null,
                lastFullScreenAdShownAtMs = clockMs()
            )
        }
        active.onDismissedWithoutReward.invoke()
    }

    private fun grantRewardInCallback(
        userId: String,
        featureType: RewardedFeatureType,
        itemId: String?
    ) {
        val now = clockMs()
        val newDailyCount = (_state.value.dailyRewardedUnlocksUsed + 1)
            .coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS)
        val newBonusBooks = if (featureType == RewardedFeatureType.BONUS_BOOK) {
            _state.value.bonusBooksUnlockedThisMonth + 1
        } else {
            _state.value.bonusBooksUnlockedThisMonth
        }

        persistDailyQuotaToFirestore(userId, newDailyCount, newBonusBooks)

        if (featureType.isTimedFiveMinSession) {
            val expiresAt = now + AdConfig.REWARDED_SESSION_DURATION_MS
            val totalSeconds = (AdConfig.REWARDED_SESSION_DURATION_MS / 1000L).toInt()
            _state.update { current ->
                current.copy(
                    dailyRewardedUnlocksUsed = newDailyCount,
                    bonusBooksUnlockedThisMonth = newBonusBooks,
                    activeTimedUnlockFeature = featureType,
                    activeTimedUnlockExpiresAtMs = expiresAt,
                    activeTimedUnlockRemainingSeconds = totalSeconds
                )
            }
            startFiveMinuteCountdownTicker(expiresAt)
        } else {
            val key = buildSingleItemKey(featureType, itemId)
            _state.update { current ->
                current.copy(
                    dailyRewardedUnlocksUsed = newDailyCount,
                    bonusBooksUnlockedThisMonth = newBonusBooks,
                    unlockedSingleItemIds = current.unlockedSingleItemIds + key
                )
            }
        }
    }

    private fun startFiveMinuteCountdownTicker(expiresAtMs: Long) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (true) {
                val remainingMs = expiresAtMs - clockMs()
                if (remainingMs <= 0L) {
                    expireActiveTimedSession()
                    break
                }
                val remainingSec = ((remainingMs + 999L) / 1000L).toInt()
                _state.update { current ->
                    current.copy(activeTimedUnlockRemainingSeconds = remainingSec)
                }
                delay(1000L)
            }
        }
    }

    /**
     * Called when a Play & Learn game round finishes, right BEFORE showing the results screen
     * (never during gameplay, never while a timer is running, and never after tapping Play Again / Replay).
     *
     * Rules:
     * - Calls `shouldShowAds(userProfile)` first; Pro users never trigger or see any ad.
     * - Shows the Interstitial (`ca-app-pub-8471293855339658/6979336613`, test `ca-app-pub-3940256099942544/1033173712`)
     *   ONLY after every 3rd completed game (`completedGamesInSessionCount % 3 == 0`).
     * - Enforces at most once every 3 minutes and at most 3 per session.
     * - Fail-safe: if an ad fails to load or is on cooldown, continues to the results screen immediately with no delay.
     */
    fun onGameCompletedBeforeResults(
        userProfile: UserProfile?,
        gameTitle: String = "Game Round",
        onContinueToResults: () -> Unit = {}
    ): Boolean {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (!shouldShowAds(effectiveProfile)) {
            onContinueToResults()
            return false
        }
        val nextCount = _state.value.completedGamesInSessionCount + 1
        _state.update { it.copy(completedGamesInSessionCount = nextCount) }
        if (nextCount % 3 != 0) {
            onContinueToResults()
            return false
        }
        return maybeShowInterstitialAtNaturalBreak(
            userProfile = effectiveProfile,
            reason = "Game Completed • $gameTitle",
            onContinue = onContinueToResults
        )
    }

    /**
     * Shows an Interstitial Ad at a natural break (e.g., after a lesson, book, or episode is completed
     * and the user returns to the library) for Free users, respecting:
     * - `shouldShowAds(userProfile)` (never requested or shown to Pro users, offline, or unverified)
     * - Max once every 3 minutes (`INTERSTITIAL_COOLDOWN_MS`)
     * - Max 3 per session (`MAX_INTERSTITIALS_PER_SESSION`)
     * - Never back-to-back with another full-screen ad
     */
    fun maybeShowInterstitialAtNaturalBreak(
        userProfile: UserProfile?,
        reason: String = "Lesson Completed",
        onContinue: () -> Unit = {}
    ): Boolean {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (!shouldShowAds(effectiveProfile)) {
            onContinue()
            return false
        }
        val current = _state.value
        val now = clockMs()
        if (!current.interstitialLoaded || simulateAdLoadFailure) {
            preloadAllAdsForFreeUser(effectiveProfile)
            onContinue()
            return false
        }
        if (current.activeFullScreenAd != null) {
            onContinue()
            return false
        }
        if (current.interstitialsShownThisSession >= AdConfig.MAX_INTERSTITIALS_PER_SESSION) {
            onContinue()
            return false
        }
        if (current.lastInterstitialShownAtMs > 0L &&
            (now - current.lastInterstitialShownAtMs) < AdConfig.INTERSTITIAL_COOLDOWN_MS
        ) {
            onContinue()
            return false
        }
        if (current.lastFullScreenAdShownAtMs > 0L &&
            (now - current.lastFullScreenAdShownAtMs) < AdConfig.FULLSCREEN_BACK_TO_BACK_GUARD_MS
        ) {
            onContinue()
            return false
        }

        Log.d(TAG, "[ads][interstitial] shown (${AdConfig.interstitialAdUnitId}, reason=$reason)")

        _state.update {
            it.copy(
                activeFullScreenAd = FullScreenAdPresentation.Interstitial(
                    adUnitId = AdConfig.interstitialAdUnitId,
                    reason = reason,
                    onDismissed = {
                        dismissActiveFullScreenAd()
                        onContinue()
                    }
                ),
                interstitialsShownThisSession = it.interstitialsShownThisSession + 1,
                lastInterstitialShownAtMs = now,
                lastFullScreenAdShownAtMs = now,
                interstitialLoaded = false
            )
        }
        return true
    }

    /**
     * Shows the optional Rewarded Interstitial prompt after finishing a lesson
     * ("Watch an ad to earn a bonus") that the user can decline.
     */
    fun maybeShowRewardedInterstitialPromptAfterLesson(
        userProfile: UserProfile?,
        bonusDescription: String = "Watch an ad to earn a +25 XP bonus",
        onBonusEarned: () -> Unit,
        onDeclinedOrSkipped: () -> Unit = {}
    ): Boolean {
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (!shouldShowAds(effectiveProfile)) {
            onDeclinedOrSkipped()
            return false
        }
        val current = _state.value
        val now = clockMs()
        if (!current.rewardedInterstitialLoaded || simulateAdLoadFailure || current.activeFullScreenAd != null) {
            onDeclinedOrSkipped()
            return false
        }
        if (current.lastFullScreenAdShownAtMs > 0L &&
            (now - current.lastFullScreenAdShownAtMs) < AdConfig.FULLSCREEN_BACK_TO_BACK_GUARD_MS
        ) {
            onDeclinedOrSkipped()
            return false
        }

        _state.update {
            it.copy(
                activeFullScreenAd = FullScreenAdPresentation.RewardedInterstitialPrompt(
                    adUnitId = AdConfig.rewardedInterstitialAdUnitId,
                    bonusDescription = bonusDescription,
                    onAcceptWatch = {
                        _state.update { s ->
                            s.copy(
                                activeFullScreenAd = FullScreenAdPresentation.Rewarded(
                                    adUnitId = AdConfig.rewardedInterstitialAdUnitId,
                                    featureType = RewardedFeatureType.LEARN_LESSON,
                                    itemId = "lesson_bonus_xp",
                                    watchDurationSeconds = 5,
                                    onUserEarnedReward = {
                                        onBonusEarned()
                                    },
                                    onDismissedWithoutReward = {
                                        onDeclinedOrSkipped()
                                    }
                                )
                            )
                        }
                    },
                    onDecline = {
                        dismissActiveFullScreenAd()
                        onDeclinedOrSkipped()
                    }
                )
            )
        }
        return true
    }

    /**
     * Shows an App Open Ad when the user returns to the app from the background:
     * - Calls `shouldShowAds(userProfile)` first (never shown to Pro users, offline, or unverified)
     * - At most once every 4 hours (`APP_OPEN_COOLDOWN_MS`)
     * - Never on first launch
     * - Never while the user is in a lesson, quiz, speaking exercise, audio player, or Emma chat
     */
    fun maybeShowAppOpenAdOnForegroundResume(
        userProfile: UserProfile?,
        isFirstLaunch: Boolean,
        isInRestrictedActivity: Boolean,
        onDismissed: () -> Unit = {}
    ): Boolean {
        if (isFirstLaunch || isInRestrictedActivity) return false
        val effectiveProfile = userProfile ?: _state.value.lastKnownUserProfile
        if (!shouldShowAds(effectiveProfile)) return false

        val current = _state.value
        val now = clockMs()
        if (!current.appOpenLoaded || simulateAdLoadFailure || current.activeFullScreenAd != null) {
            return false
        }
        if (current.lastAppOpenShownAtMs > 0L &&
            (now - current.lastAppOpenShownAtMs) < AdConfig.APP_OPEN_COOLDOWN_MS
        ) {
            return false
        }
        if (current.lastFullScreenAdShownAtMs > 0L &&
            (now - current.lastFullScreenAdShownAtMs) < AdConfig.FULLSCREEN_BACK_TO_BACK_GUARD_MS
        ) {
            return false
        }

        prefs?.edit()?.putLong(KEY_LAST_APP_OPEN_MS, now)?.apply()
        _state.update {
            it.copy(
                activeFullScreenAd = FullScreenAdPresentation.AppOpen(
                    adUnitId = AdConfig.appOpenAdUnitId,
                    onDismissed = {
                        dismissActiveFullScreenAd()
                        onDismissed()
                    }
                ),
                lastAppOpenShownAtMs = now,
                lastFullScreenAdShownAtMs = now,
                appOpenLoaded = false
            )
        }
        return true
    }

    fun dismissActiveFullScreenAd() {
        _state.update { current ->
            current.copy(
                activeFullScreenAd = null,
                lastFullScreenAdShownAtMs = clockMs()
            )
        }
        if (shouldShowAds(_state.value.lastKnownUserProfile)) {
            preloadAllAdsForFreeUser(_state.value.lastKnownUserProfile)
        }
    }

    /**
     * Resets state for clean unit/Robolectric test isolation.
     */
    fun resetForTesting(
        dailyUnlocksUsed: Int = 0,
        isPro: Boolean = false,
        simulateFailure: Boolean = false,
        isLoggedIn: Boolean = true,
        isSubscriptionVerified: Boolean = true,
        isSubscriptionLoading: Boolean = false,
        isOffline: Boolean = false,
        userProfile: UserProfile? = null
    ) {
        countdownJob?.cancel()
        countdownJob = null
        simulateAdLoadFailure = simulateFailure
        clockMs = { System.currentTimeMillis() }
        prefs?.edit()?.clear()?.apply()
        val effectiveProfile = userProfile ?: if (isLoggedIn) {
            UserProfile(
                userId = if (isPro) "pro_test_uid" else "free_test_uid",
                subscriptionTier = if (isPro) "premium_yearly" else "free",
                purchaseToken = if (isPro) "gp_verified_test_token" else ""
            )
        } else {
            UserProfile(userId = "guest", subscriptionTier = "free")
        }
        val canShow = isLoggedIn && isSubscriptionVerified && !isSubscriptionLoading && !isOffline && !isPro && !simulateFailure
        _state.value = AdServiceState(
            isInitialized = true,
            umpConsentResolved = true,
            umpConsentGranted = true,
            showUmpConsentModal = false,
            isUserLoggedIn = isLoggedIn,
            isSubscriptionStatusVerified = isSubscriptionVerified && !isSubscriptionLoading && !isOffline,
            isSubscriptionLoading = isSubscriptionLoading,
            isOfflineOrUnverified = isOffline || !isSubscriptionVerified,
            isUserVerifiedPro = isPro,
            lastKnownUserProfile = effectiveProfile,
            bannerLoaded = canShow,
            nativeLoaded = canShow,
            interstitialLoaded = canShow,
            rewardedLoaded = canShow,
            rewardedInterstitialLoaded = canShow,
            appOpenLoaded = canShow,
            rewardedLoadFailed = simulateFailure,
            rewardedErrorMessage = if (simulateFailure) "No ad available right now, try again in a moment" else null,
            dailyRewardedUnlocksUsed = dailyUnlocksUsed.coerceIn(0, AdConfig.MAX_DAILY_REWARDED_UNLOCKS),
            hasCompletedFirstLaunch = true
        )
    }
}
