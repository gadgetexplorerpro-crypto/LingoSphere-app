package com.example.ads

/**
 * Centralized Google AdMob configuration for LingoSphere.
 *
 * Mirrors `/src/config/ads.ts`.
 * - Default [USE_TEST_ADS] is `true` for development and verification.
 * - Switch [USE_TEST_ADS] to `false` (and update `com.google.android.gms.ads.APPLICATION_ID`
 *   in `AndroidManifest.xml`) before building a production release APK/AAB.
 */
object AdConfig {

    /**
     * Master flag controlling whether Google Test Ad IDs (`true`) or
     * LingoSphere Production AdMob IDs (`false`) are used across the entire app.
     */
    @Volatile
    var USE_TEST_ADS: Boolean = true

    // =========================================================================
    // Production AdMob IDs (LingoSphere)
    // =========================================================================
    const val PROD_APP_ID = "ca-app-pub-8471293855339658~7525338937"
    const val PROD_BANNER_ID = "ca-app-pub-8471293855339658/8524799689"
    const val PROD_INTERSTITIAL_ID = "ca-app-pub-8471293855339658/6979336613"
    const val PROD_REWARDED_INTERSTITIAL_ID = "ca-app-pub-8471293855339658/5666254944"
    const val PROD_REWARDED_ID = "ca-app-pub-8471293855339658/1933847310"
    const val PROD_NATIVE_ADVANCED_ID = "ca-app-pub-8471293855339658/2376700449"
    const val PROD_APP_OPEN_ID = "ca-app-pub-8471293855339658/3080901315"

    // =========================================================================
    // Google Test IDs (for development)
    // =========================================================================
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_NATIVE_ADVANCED_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

    // =========================================================================
    // Active Computed Ad Unit IDs
    // =========================================================================
    val appId: String
        get() = if (USE_TEST_ADS) TEST_APP_ID else PROD_APP_ID

    val bannerAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_BANNER_ID else PROD_BANNER_ID

    val interstitialAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    val rewardedInterstitialAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_INTERSTITIAL_ID else PROD_REWARDED_INTERSTITIAL_ID

    val rewardedAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_ID else PROD_REWARDED_ID

    val nativeAdvancedAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_ADVANCED_ID else PROD_NATIVE_ADVANCED_ID

    val appOpenAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_APP_OPEN_ID else PROD_APP_OPEN_ID

    // =========================================================================
    // Frequency & Session Caps
    // =========================================================================
    /** Interstitial: at most once every 3 minutes */
    const val INTERSTITIAL_COOLDOWN_MS: Long = 3L * 60L * 1000L

    /** Interstitial: at most 3 per session */
    const val MAX_INTERSTITIALS_PER_SESSION: Int = 3

    /** App open: at most once every 4 hours */
    const val APP_OPEN_COOLDOWN_MS: Long = 4L * 60L * 60L * 1000L

    /** Minimum gap between any two full-screen ads so two full-screen ads never appear back-to-back */
    const val FULLSCREEN_BACK_TO_BACK_GUARD_MS: Long = 45_000L

    /** Rewarded trial session duration for timed features (AI Tutor / Speak / Roleplay): 5 minutes */
    const val REWARDED_SESSION_DURATION_MS: Long = 5L * 60L * 1000L

    /** Maximum rewarded ad unlocks per user per calendar day (stored in Firestore + local cache) */
    const val MAX_DAILY_REWARDED_UNLOCKS: Int = 5
}
