package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CallRoom
import com.example.data.model.Language
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.repository.BillingRepository
import com.example.data.repository.CurriculumData
import com.example.data.repository.LingoRepository
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.IncomingCallOverlay
import com.example.ui.components.LingoBottomBar
import com.example.ui.components.LingoScreen
import com.example.ui.components.appSignOut
import com.example.ui.screens.AccentUnderstandingStudioScreen
import com.example.ui.screens.AiCameraLanguageScreen
import com.example.ui.screens.AiConversationReplayScreen
import com.example.ui.screens.AiImmersionModeScreen
import com.example.ui.screens.AiVideoTutorScreen
import com.example.ui.screens.AiVoiceCallScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.CulturePassportScreen
import com.example.ui.screens.DailyChallengesScreen
import com.example.ui.screens.ExploreLanguagesScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntelligentMistakeRecoveryScreen
import com.example.ui.screens.LanguageExchangeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.LessonExerciseScreen
import com.example.ui.screens.LingoVideosScreen
import com.example.ui.screens.LiveUserCallScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PersonalLanguagePortfolioScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.PremiumHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressDashboardScreen
import com.example.ui.screens.PronunciationPracticeScreen
import com.example.ui.screens.SmartMatchingScreen
import com.example.ui.screens.SpeakScreen
import com.example.ui.screens.SpeakingRoomsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VocabularyBuilderScreen
import com.example.ui.theme.LingoSphereTheme
import com.example.ui.theme.LingoThemeController
import com.example.ai.AiErrorHandler
import com.example.ai.UpgradePromptRequest
import com.example.ai.UpgradeSheetMode
import com.example.data.repository.AccessPolicyManager
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoGlobalUpgradeSheet
import com.example.ui.components.LingoOfflineTopBanner
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StartupBootstrapState(
  val hasCompletedOnboarding: Boolean,
  val currentUser: FirebaseUser?
)

internal fun loadLightweightStartupBootstrap(appContext: Context): StartupBootstrapState {
  runCatching { LingoThemeController.initialize(appContext) }
  // Pre-warm custom TTF typeface into Android's process-wide Typeface cache on IO thread.
  runCatching {
    androidx.core.content.res.ResourcesCompat.getFont(appContext, R.font.plus_jakarta_sans)
  }
  val prefs = appContext.getSharedPreferences("lingosphere_prefs", Context.MODE_PRIVATE)
  val onboarded = prefs.getBoolean("onboarding_completed", false)
  if (FirebaseApp.getApps(appContext).isEmpty()) {
    runCatching { FirebaseApp.initializeApp(appContext) }
  }
  val authUser = runCatching { Firebase.auth.currentUser }.getOrNull()
  return StartupBootstrapState(
    hasCompletedOnboarding = onboarded,
    currentUser = authUser
  )
}

internal fun attachLingoComposeRootView(
  activity: ComponentActivity,
  hostContainer: FrameLayout,
  bootstrapState: StartupBootstrapState?,
  onComposeReady: () -> Unit = {},
  speechManagerProvider: () -> SpeechManager,
  onInitializeBilling: (
    onEntitlementVerified: (String, String) -> Unit,
    onSubscriptionDetailsVerified: ((com.example.data.repository.VerifiedSubscriptionDetails) -> Unit)?
  ) -> BillingRepository?
) {
  val composeView = ComposeView(activity).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity.lifecycle))
    setContent {
      LingoAppRoot(
        initialBootstrapState = bootstrapState,
        onComposeReady = onComposeReady,
        speechManagerProvider = speechManagerProvider,
        onInitializeBilling = onInitializeBilling
      )
    }
  }
  hostContainer.addView(
    composeView,
    FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
  )
}

@Volatile
private var appCheckConfigured = false

private val appCheckLock = Any()

@Volatile
private var pendingAppCheckContext: Context? = null

@Volatile
private var pendingAppCheckToken: String? = null

internal fun rememberPendingAppCheckIntent(context: Context, intent: Intent?) {
  pendingAppCheckContext = context.applicationContext
  val token = intent?.getStringExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")
    ?: intent?.getStringExtra("app_check_debug_token")
  if (!token.isNullOrEmpty()) {
    pendingAppCheckToken = token
  }
}

internal fun ensureAppCheckConfiguredIfPending() {
  if (appCheckConfigured) return
  val ctx = pendingAppCheckContext ?: return
  configureAppCheckSafely(ctx, null)
}

fun configureAppCheck(context: Context, intent: Intent?) {
  rememberPendingAppCheckIntent(context, intent)
  configureAppCheckSafely(context, intent)
}

internal fun configureAppCheckSafely(context: Context, intent: Intent?) {
  synchronized(appCheckLock) {
    try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      if (BuildConfig.DEBUG) {
        val debugToken = intent?.getStringExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")
          ?: intent?.getStringExtra("app_check_debug_token")
          ?: pendingAppCheckToken
        if (!debugToken.isNullOrEmpty()) {
          val persistenceKey = FirebaseApp.getInstance().persistenceKey
          val prefs = context.getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.$persistenceKey",
            Context.MODE_PRIVATE
          )
          // CRITICAL: MUST use commit() instead of apply() so the secret is written
          // synchronously before DebugAppCheckProviderFactory reads it.
          prefs.edit()
            .putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", debugToken)
            .commit()
          android.util.Log.d("AppCheckInit", "App Check debug secret configured successfully.")
        } else {
          android.util.Log.w("AppCheckInit", "No debug token found in Intent extra.")
        }

        if (!appCheckConfigured) {
          appCheckConfigured = true
          val appCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
          appCheck.setTokenAutoRefreshEnabled(true)
          appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
          )
          android.util.Log.d("AppCheckInit", "DebugAppCheckProviderFactory installed with auto-refresh.")
        }
      } else {
        if (appCheckConfigured) return
        appCheckConfigured = true
        val appCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
        appCheck.setTokenAutoRefreshEnabled(true)
        appCheck.installAppCheckProviderFactory(
          PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        android.util.Log.d("AppCheckInit", "PlayIntegrityAppCheckProviderFactory installed with auto-refresh.")
      }
    } catch (e: Exception) {
      android.util.Log.e("AppCheckInit", "Failed to initialize Firebase App Check", e)
    }
  }
}

enum class ActiveSubScreen {
  NONE,
  EXPLORE_LANGUAGES,
  DAILY_CHALLENGES,
  PRONUNCIATION_COACH,
  VOCABULARY_BUILDER,
  LISTENING_PRACTICE,
  WRITING_STUDIO,
  GRAMMAR_STUDIO,
  ROLEPLAY_STUDIO,
  PROGRESS_DASHBOARD,
  NOTIFICATIONS,
  HELP_SUPPORT,
  CULTURE_PASSPORT,
  SPEAKING_ROOMS,
  LANGUAGE_EXCHANGE,
  SMART_MATCHING,
  AI_CAMERA_LENS,
  PREMIUM_HUB,
  AI_IMMERSION_MODE,
  AI_CONVERSATION_REPLAY,
  MISTAKE_RECOVERY_MEMORY,
  ACCENT_UNDERSTANDING_STUDIO,
  LANGUAGE_PORTFOLIO,
  CONVERSATION_REPAIR_LAB,
  LANGUAGE_EMERGENCY_MODE,
  AI_DEBATE_PARTNER,
  EXPLAIN_IT_THREE_WAYS,
  CONVERSATION_DETECTIVE,
  TEACH_THE_AI,
  EXPLORE_COMMUNITY_CULTURE,
  EXPLORE_AI_LABS,
  EXPLORE_PROGRESS_LANGUAGES,
  EXPLORE_LEARNING_TOOLS
}

@Composable
fun LingoAppRoot(
  initialBootstrapState: StartupBootstrapState? = null,
  onComposeReady: () -> Unit = {},
  speechManagerProvider: () -> SpeechManager,
  onInitializeBilling: (
    onEntitlementVerified: (String, String) -> Unit,
    onSubscriptionDetailsVerified: ((com.example.data.repository.VerifiedSubscriptionDetails) -> Unit)?
  ) -> BillingRepository?
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isRobolectric = remember {
    android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
  }

  var hasCompletedOnboarding by remember {
    mutableStateOf(initialBootstrapState?.hasCompletedOnboarding ?: false)
  }
  var currentUser by remember {
    mutableStateOf(initialBootstrapState?.currentUser)
  }
  // Always let ComposeView commit its initial attach/surface-sync frame with a 1-node Box on real
  // devices before mounting the full screen tree (even when initialBootstrapState was preloaded).
  var isBootstrapLoaded by remember {
    mutableStateOf(isRobolectric && initialBootstrapState != null)
  }

  val fontFamilyResolver = LocalFontFamilyResolver.current

  LaunchedEffect(Unit) {
    if (!isBootstrapLoaded) {
      if (!isRobolectric) {
        // Commit initial frames on a bare 1-node dark Box so WindowManager & VRI SurfaceSyncGroups
        // finish cleanly before Typography, MaterialTheme, and Firebase initialization run.
        withFrameNanos { }
        withFrameNanos { }
        delay(250L)
      }
      val loaded = withContext(Dispatchers.IO) {
        val state = initialBootstrapState ?: loadLightweightStartupBootstrap(context.applicationContext)
        runCatching { fontFamilyResolver.preload(com.example.ui.theme.PlusJakartaSansFontFamily) }
        state
      }
      hasCompletedOnboarding = loaded.hasCompletedOnboarding
      currentUser = loaded.currentUser
      if (!isRobolectric) {
        withFrameNanos { }
      }
      isBootstrapLoaded = true
    } else if (isRobolectric) {
      onComposeReady()
    }
  }

  if (!isBootstrapLoaded) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF070B14))
    )
    return
  }

  LingoSphereTheme {
    LingoAppSyncedBody(
      context = context,
      coroutineScope = coroutineScope,
      hasCompletedOnboardingInitial = hasCompletedOnboarding,
      currentUserInitial = currentUser,
      isAuthenticatedSessionReadyInitial = false,
      speechManagerProvider = speechManagerProvider,
      onFirstInteractiveReady = onComposeReady,
      onInitializeBilling = onInitializeBilling
    )
  }
}

@Composable
private fun LingoAppSyncedBody(
  context: Context,
  coroutineScope: kotlinx.coroutines.CoroutineScope,
  hasCompletedOnboardingInitial: Boolean,
  currentUserInitial: FirebaseUser?,
  isAuthenticatedSessionReadyInitial: Boolean,
  speechManagerProvider: () -> SpeechManager,
  onFirstInteractiveReady: () -> Unit = {},
  onInitializeBilling: (
    onEntitlementVerified: (String, String) -> Unit,
    onSubscriptionDetailsVerified: ((com.example.data.repository.VerifiedSubscriptionDetails) -> Unit)?
  ) -> BillingRepository?
) {
  var hasCompletedOnboarding by remember { mutableStateOf(hasCompletedOnboardingInitial) }
  var currentUser by remember { mutableStateOf(currentUserInitial) }
  var isAuthenticatedSessionReady by remember { mutableStateOf(isAuthenticatedSessionReadyInitial) }
  var didNotifyInteractiveReady by remember { mutableStateOf(false) }

  val notifyReadyOnce: suspend () -> Unit = {
    if (!didNotifyInteractiveReady) {
      withFrameNanos { }
      didNotifyInteractiveReady = true
      onFirstInteractiveReady()
    }
  }

  val launchCommand by com.example.webrtc.IncomingCallRegistry.launchCommandFlow.collectAsState()
  var showSplash by remember { mutableStateOf(false) }

  LaunchedEffect(launchCommand) {
    if (launchCommand != null) {
      showSplash = false
    }
  }

  LaunchedEffect(Unit) {
    if (!android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      withFrameNanos { }
      delay(300L)
    }
    val authInstance = runCatching { withContext(Dispatchers.IO) { Firebase.auth } }.getOrNull() ?: return@LaunchedEffect
    val authListener = FirebaseAuth.AuthStateListener { auth ->
      val nextUser = auth.currentUser
      if (currentUser?.uid != nextUser?.uid) {
        com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
        currentUser = nextUser
      }
    }
    runCatching { authInstance.addAuthStateListener(authListener) }
    try {
      kotlinx.coroutines.awaitCancellation()
    } finally {
      runCatching { authInstance.removeAuthStateListener(authListener) }
    }
  }

  if (showSplash && launchCommand == null) {
    SplashScreen(
      onSplashFinished = { showSplash = false }
    )
    LaunchedEffect(Unit) { notifyReadyOnce() }
    return
  }

  val user = currentUser
  if (user == null) {
    LaunchedEffect(Unit) {
      com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
    }
    LaunchedEffect(hasCompletedOnboarding) {
      notifyReadyOnce()
    }
    if (!hasCompletedOnboarding) {
      OnboardingScreen(
        onFinishOnboarding = {
          coroutineScope.launch(Dispatchers.IO) {
            runCatching {
              context.getSharedPreferences("lingosphere_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_completed", true)
                .apply()
            }
          }
          hasCompletedOnboarding = true
        }
      )
    } else {
      AuthScreen(
        onSignInSuccess = { _, displayName, email, nativeLang, targetLang ->
          val authUser = Firebase.auth.currentUser
          currentUser = authUser
          if (authUser != null) {
            coroutineScope.launch(Dispatchers.IO) {
              try {
                val prefs = context.getSharedPreferences("lingosphere_prefs", Context.MODE_PRIVATE)
                val repository = LingoRepository(context)
                val existing = repository.getUserProfile(authUser.uid)
                if (existing == null) {
                  val onboardingTargetLang = prefs.getString("onboarding_selected_target_lang", null)?.takeIf { it.isNotBlank() } ?: targetLang
                  val onboardingProficiency = prefs.getString("onboarding_selected_proficiency", null)?.takeIf { it.isNotBlank() } ?: "Beginner"
                  val onboardingGoalMins = prefs.getInt("onboarding_selected_daily_goal", 15).toLong()
                  val newUserProfile = UserProfile(
                    userId = authUser.uid,
                    displayName = displayName.ifBlank { authUser.displayName ?: "Language Learner" },
                    nativeLanguageCode = nativeLang,
                    targetLanguageCode = onboardingTargetLang,
                    proficiencyLevel = onboardingProficiency,
                    dailyGoalMinutes = onboardingGoalMins,
                    activeLanguages = listOf(onboardingTargetLang)
                  )
                  repository.createUserProfile(newUserProfile)
                  repository.saveUserPrivateEmail(email.ifBlank { authUser.email ?: "" })
                  repository.saveSpeakingSession(
                    SpeakingSession(
                      userId = authUser.uid,
                      languageCode = targetLang,
                      mode = "Tutor",
                      scenarioTitle = "Welcome Assessment",
                      proficiencyLevel = "Beginner",
                      turnsCount = 1L,
                      fluencyScore = 90L,
                      grammarFeedbackSummary = "Welcome to LingoSphere! Your account is initialized."
                    )
                  )
                  val pendingRefCode = com.example.data.repository.ReferralInstallAttributionStore
                    .getPendingReferralCode(context)
                  if (!pendingRefCode.isNullOrBlank()) {
                    val refSource = com.example.data.repository.ReferralInstallAttributionStore
                      .getPendingReferralSource(context)
                    runCatching {
                      com.example.data.repository.ReferralRepository(context)
                        .claimReferralCode(
                          rawCode = pendingRefCode,
                          referredProfile = newUserProfile,
                          attributionSource = refSource,
                          isNewAccountRegistration = true
                        )
                    }
                  }
                } else {
                  repository.syncCommunityProfile(existing)
                  com.example.data.repository.ReferralInstallAttributionStore
                    .clearPendingReferralCode(context)
                }
              } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error creating initial profile: ${e.message}", e)
              }
            }
          }
        },
        onSignInError = { errorMsg ->
          Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        }
      )
    }
  } else {
    var sessionDeps by remember(user.uid) {
      mutableStateOf<Triple<LingoRepository, com.example.data.repository.LearningMemoryAndOfflineManager, SpeechManager>?>(null)
    }
    LaunchedEffect(user.uid) {
      val built = withContext(Dispatchers.IO) {
        runCatching {
          AccessPolicyManager.syncPolicyAndEntitlementFromFirestore(
            com.google.firebase.firestore.FirebaseFirestore.getInstance(
              context.applicationContext.getString(R.string.firestore_database_id)
            )
          )
        }
        Triple(
          LingoRepository(context),
          com.example.data.repository.LearningMemoryAndOfflineManager(context),
          speechManagerProvider()
        )
      }
      sessionDeps = built
      withFrameNanos { }
      isAuthenticatedSessionReady = true
      delay(1200L)
      com.example.webrtc.FcmTokenManager.syncCurrentUserTokenAsync(context)
      com.example.webrtc.OfflineMissedCallSyncManager.startObserving(context, user.uid)
    }

    val deps = sessionDeps
    if (deps == null || !isAuthenticatedSessionReady) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF070B14))
      )
      return
    }

    val (repository, memoryManager, speechManager) = deps
    val viewModel: LingoViewModel = viewModel(
      key = user.uid,
      factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return LingoViewModel(repository, user.uid, memoryManager) as T
        }
      }
    )

    val isRobolectric = remember {
      android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }
    var isHomeShellReady by remember(user.uid) { mutableStateOf(isRobolectric) }
    LaunchedEffect(user.uid) {
      if (!isHomeShellReady) {
        withFrameNanos { }
        withFrameNanos { }
        isHomeShellReady = true
      }
      notifyReadyOnce()
    }

    if (!isHomeShellReady) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF070B14))
      )
      return
    }

    var billingRepo by remember(user.uid) { mutableStateOf<BillingRepository?>(null) }
    LaunchedEffect(user.uid) {
      delay(1500L)
      billingRepo = onInitializeBilling(
        { tier, token ->
          viewModel.updateSubscription(tier, token)
        },
        { details ->
          viewModel.updateSubscription(
            tier = details.tier,
            token = details.purchaseToken,
            isFreeTrial = details.isFreeTrial,
            autoRenewing = details.isAutoRenewing,
            expiryEpochMs = details.expiryEpochMs,
            productId = details.productId,
            basePlanId = details.basePlanId
          )
          Toast.makeText(
            context,
            "LingoSphere Pro Activated! Enjoy full access.",
            Toast.LENGTH_LONG
          ).show()
        }
      )
    }

    AuthenticatedMainContent(
      viewModel = viewModel,
      speechManager = speechManager,
      billingRepository = billingRepo,
      onSignOut = {
        com.example.data.repository.AccessPolicyManager.clearEntitlementAndUsageOnAuthChange()
        coroutineScope.launch {
          com.example.presence.PresenceManager.markUserOfflineNow(context, user.uid)
        }
        com.example.webrtc.OfflineMissedCallSyncManager.stopObserving()
        com.example.webrtc.FcmTokenManager.unregisterCurrentDeviceTokenAsync(context, user.uid)
        appSignOut(
          context = context,
          credentialManager = CredentialManager.create(context),
          onSignOutComplete = { currentUser = null },
          scope = coroutineScope
        )
      }
    )
  }
}

@Composable
fun AuthenticatedMainContent(
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  billingRepository: BillingRepository?,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val userProfile by viewModel.userProfileState.collectAsState()
  val activeLesson by viewModel.activeLesson.collectAsState()
  val flashcards by viewModel.flashcardProgressState.collectAsState()
  val lessonProgress by viewModel.lessonProgressState.collectAsState()
  val speakingSessions by viewModel.speakingSessionsState.collectAsState()
  val communityProfiles by viewModel.communityProfilesState.collectAsState()
  val myCommunityProfile by viewModel.myCommunityProfileState.collectAsState()
  val blockedUsers by viewModel.blockedUsersState.collectAsState()
  val notifications by viewModel.notificationsState.collectAsState()
  val incomingCallRoom by viewModel.incomingCallRoomState.collectAsState()
  val launchCommand by com.example.webrtc.IncomingCallRegistry.launchCommandFlow.collectAsState()
  val savedVideos by viewModel.savedVideosState.collectAsState()
  val videoPlaylists by viewModel.videoPlaylistsState.collectAsState()
  val videoWatchHistory by viewModel.videoWatchHistoryState.collectAsState()
  val srsMetadataMap by viewModel.srsMetadataMap.collectAsState()
  val mistakeNotebookEntries by viewModel.mistakeNotebookEntries.collectAsState()
  val pronunciationHistory by viewModel.pronunciationHistory.collectAsState()
  val bookmarkedReadingIds by viewModel.bookmarkedReadingIds.collectAsState()
  val completedAcademyIds by viewModel.completedAcademyIds.collectAsState()
  val placementEvaluation by viewModel.placementEvaluationState.collectAsState()
  val inProgressLessonsMap by viewModel.inProgressLessonsMap.collectAsState()
  val downloadedLessonIds by viewModel.downloadedLessonIds.collectAsState()
  val pendingOfflineSyncCount by viewModel.pendingOfflineSyncCount.collectAsState()
  val dailyMinutesMap by viewModel.dailyMinutesMap.collectAsState()
  val customProDecks by viewModel.customProDecks.collectAsState()
  val accountSettings by viewModel.accountSettingsState.collectAsState()
  val isSavingProfile by viewModel.isSavingProfileState.collectAsState()
  val profileStatusMessage by viewModel.profileStatusMessage.collectAsState()
  val completedDailyMissions by viewModel.completedDailyMissionsState.collectAsState()
  val repeatedMistakes by viewModel.repeatedMistakesState.collectAsState()
  val referralDashboardState by viewModel.referralDashboardState.collectAsState()
  val friendRequests by viewModel.friendRequestsState.collectAsState()
  val appUpdateUiState by com.example.update.LingoAppUpdateController.updateUiState.collectAsState()

  // Keep BillingRepository & GeminiService synchronized with server-verified Pro status
  LaunchedEffect(billingRepository, userProfile.hasUsedFreeTrial, userProfile.isFreeTrial, userProfile.isEntitlementVerified) {
    billingRepository?.syncUserTrialEligibility(userProfile.hasUsedFreeTrial || userProfile.isFreeTrial)
    com.example.ai.GeminiService.syncProStatus(userProfile.isEntitlementVerified)
  }

  val isRobolectricRuntime = remember {
    android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
  }

  // Reconcile installed version cleanly on IO after initial surface sync settles
  LaunchedEffect(Unit) {
    if (!isRobolectricRuntime) delay(2600L)
    withContext(Dispatchers.IO) {
      com.example.update.LingoAppUpdateController.reconcileInstalledVersionOnStartup(context)
    }
  }

  // Sync user's selected theme mode into the root LingoThemeController without redundant nested MaterialTheme trees
  LaunchedEffect(accountSettings.themeMode) {
    if (accountSettings.themeMode.isNotBlank() && LingoThemeController.themeMode != accountSettings.themeMode) {
      LingoThemeController.themeMode = accountSettings.themeMode
    }
  }

  var adaptivePlan by remember {
    mutableStateOf<com.example.data.model.AdaptiveRecommendationPlan?>(null)
  }
  LaunchedEffect(
    userProfile,
    lessonProgress,
    flashcards,
    speakingSessions,
    pronunciationHistory,
    repeatedMistakes,
    srsMetadataMap
  ) {
    delay(if (isRobolectricRuntime) 50L else 1800L)
    adaptivePlan = withContext(Dispatchers.Default) {
      viewModel.computeAdaptiveRecommendationPlan()
    }
  }

  LaunchedEffect(accountSettings, myCommunityProfile?.allowCalls, userProfile.currentStreak, userProfile.displayName) {
    delay(if (isRobolectricRuntime) 50L else 2000L)
    withContext(Dispatchers.IO) {
      val masterAllowCalls = myCommunityProfile?.allowCalls ?: accountSettings.allowIncomingCalls
      com.example.webrtc.IncomingCallRegistry.setIncomingCallPreferences(
        context = context,
        allowIncomingCalls = masterAllowCalls && accountSettings.allowIncomingCalls,
        allowVoiceCalls = accountSettings.allowVoiceCalls,
        allowVideoCalls = accountSettings.allowVideoCalls,
        callNotificationsEnabled = accountSettings.callNotificationsEnabled
      )
      com.example.notifications.SmartNotificationManager.syncSettingsToPreferences(context, accountSettings)
      com.example.notifications.SmartNotificationManager.syncUserStreakContext(
        context = context,
        streakDays = userProfile.currentStreak.toInt(),
        userName = userProfile.displayName
      )
      com.example.notifications.SmartNotificationScheduler.scheduleUserDailyStreakReminder(
        context = context,
        hourOfDay = accountSettings.dailyReminderHour,
        minute = accountSettings.dailyReminderMinute
      )
      speechManager.setVoiceRecordingConsent(accountSettings.voiceRecordingConsent)
    }
  }

  var weeklyReport by remember {
    mutableStateOf<com.example.data.repository.WeeklyLearningReportSummary?>(null)
  }
  LaunchedEffect(
    userProfile,
    lessonProgress,
    flashcards,
    speakingSessions,
    mistakeNotebookEntries,
    pronunciationHistory,
    dailyMinutesMap,
    pendingOfflineSyncCount
  ) {
    delay(if (isRobolectricRuntime) 50L else 2200L)
    weeklyReport = withContext(Dispatchers.IO) {
      viewModel.buildWeeklyLearningReport()
    }
  }

  val dueFlashcardsCount = remember(flashcards, srsMetadataMap) {
    val now = System.currentTimeMillis()
    flashcards.count { card ->
      com.example.data.model.SpacedRepetitionEngine.isCardDue(card, srsMetadataMap[card.id], now)
    }
  }
  val pastMistakesMemory = remember(mistakeNotebookEntries, speakingSessions) {
    val fromNotebook = mistakeNotebookEntries.map { "${it.originalText} -> ${it.correctedText}" }
    val fromSessions = speakingSessions.mapNotNull { it.grammarFeedbackSummary.takeIf { s -> s.isNotBlank() } }
    (fromNotebook + fromSessions).distinct().take(5)
  }

  var currentScreen by remember { mutableStateOf(LingoScreen.HOME) }
  var communityInitialTab by remember { mutableIntStateOf(0) }
  var communityInitialPartnerId by remember { mutableStateOf<String?>(null) }
  var communityInitialOpenPartnerProfile by remember { mutableStateOf(false) }
  var openInviteFriendsSignal by remember { mutableIntStateOf(0) }
  var selectedWordOfTheDay by remember {
    mutableStateOf<com.example.notifications.WordOfTheDayEntry?>(null)
  }
  var cultureInitialTab by remember { mutableIntStateOf(0) }
  var activeSubScreen by remember { mutableStateOf(ActiveSubScreen.NONE) }
  var booksListeningInitialSection by remember {
    mutableStateOf(com.example.ui.screens.BooksListeningInitialSection.MAIN)
  }
  var premiumHubInitialSection by remember {
    mutableStateOf(com.example.ui.screens.PremiumHubSection.SUBSCRIPTION)
  }
  var isVoiceCallActive by remember { mutableStateOf(false) }
  var isVideoTutorCallActive by remember { mutableStateOf(false) }

  val isOnline by AiErrorHandler.isOnline.collectAsState()
  var activeUpgradePrompt by remember { mutableStateOf<UpgradePromptRequest?>(null) }
  val adState by com.example.ads.AdService.state.collectAsState()

  val isEntitlementLoading by com.example.data.repository.AccessPolicyManager.isEntitlementLoading.collectAsState()
  val entitlementState by com.example.data.repository.AccessPolicyManager.entitlementState.collectAsState()

  LaunchedEffect(
    userProfile.userId,
    userProfile.isEntitlementVerified,
    userProfile.isSubscriptionExpired,
    isEntitlementLoading,
    isOnline,
    entitlementState.status
  ) {
    if (!isRobolectricRuntime && !adState.isInitialized) delay(1600L)
    withContext(Dispatchers.IO) {
      if (!com.example.ads.AdService.state.value.isInitialized) {
        com.example.ads.AdService.initialize(
          context = context,
          userProfile = userProfile
        )
      }
      com.example.ads.AdService.onUserProfileUpdated(userProfile)
    }
  }

  DisposableEffect(userProfile.isEntitlementVerified) {
    com.example.ads.AdService.onTimedSessionExpired = { expiredFeature ->
      if (!userProfile.isEntitlementVerified) {
        val wasInTutorCall = isVoiceCallActive || isVideoTutorCallActive
        val wasInSubScreen = activeSubScreen != ActiveSubScreen.NONE
        if (wasInTutorCall) {
          isVoiceCallActive = false
          isVideoTutorCallActive = false
        }
        if (wasInSubScreen) {
          activeSubScreen = ActiveSubScreen.NONE
        }
        AiErrorHandler.requestUpgrade(
          mode = if (expiredFeature == com.example.ads.RewardedFeatureType.ROLEPLAY) {
            UpgradeSheetMode.ROLEPLAY
          } else {
            UpgradeSheetMode.AI_UPGRADE
          },
          featureKey = expiredFeature.id,
          headline = "5-minute free session ended",
          reason = "Watch another short ad to continue or upgrade to LingoSphere Pro for unlimited access."
        )
      }
    }
    onDispose {
      com.example.ads.AdService.onTimedSessionExpired = null
    }
  }

  // Open global upgrade sheet cleanly whenever AI or Pro-only features require Pro
  LaunchedEffect(Unit) {
    com.example.ai.GeminiService.paywallRequests.collect { reason ->
      isVoiceCallActive = false
      isVideoTutorCallActive = false
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.AI_UPGRADE,
        featureKey = "ai.pro",
        reason = reason
      )
    }
  }

  LaunchedEffect(Unit) {
    AiErrorHandler.upgradeSheetEvents.collect { req ->
      if (!com.example.data.repository.AccessPolicyManager.isUserVerifiedPro(userProfile)) {
        activeUpgradePrompt = req
      }
    }
  }

  // Enforce server-verified Pro gate (or active 5-min rewarded ad unlock) on AI calls, structured lessons, Roleplay & AI sub-screens
  LaunchedEffect(
    isVoiceCallActive,
    isVideoTutorCallActive,
    userProfile.isEntitlementVerified,
    adState.activeTimedUnlockRemainingSeconds
  ) {
    val hasEmmaUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
      com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA
    )
    if ((isVoiceCallActive || isVideoTutorCallActive) && !userProfile.isEntitlementVerified && !hasEmmaUnlock) {
      val wasVideo = isVideoTutorCallActive
      isVoiceCallActive = false
      isVideoTutorCallActive = false
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.AI_UPGRADE,
        featureKey = "ai.voice.tutor",
        headline = "Unlock AI Tutor",
        reason = "Live AI voice & video coaching is included with LingoSphere Pro.",
        onRewardedUnlockGranted = {
          if (wasVideo) {
            isVideoTutorCallActive = true
          } else {
            isVoiceCallActive = true
          }
        }
      )
    }
  }

  LaunchedEffect(activeLesson?.id, userProfile.isEntitlementVerified, adState.unlockedSingleItemIds) {
    val currentLesson = activeLesson
    if (currentLesson != null && !userProfile.isEntitlementVerified) {
      val hasLessonUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
        com.example.ads.RewardedFeatureType.LEARN_LESSON,
        currentLesson.id
      )
      if (!hasLessonUnlock) {
        viewModel.dismissLesson()
        AiErrorHandler.requestUpgrade(
          mode = UpgradeSheetMode.LESSONS,
          featureKey = "lessons.daily",
          headline = "Unlock Structured Lessons",
          reason = "All 500+ structured CEFR lessons across every language are included with LingoSphere Pro.",
          itemId = currentLesson.id,
          onRewardedUnlockGranted = {
            viewModel.startLesson(currentLesson)
          }
        )
      }
    }
  }

  LaunchedEffect(
    activeSubScreen,
    userProfile.isEntitlementVerified,
    adState.activeTimedUnlockRemainingSeconds
  ) {
    if (!userProfile.isEntitlementVerified) {
      val modeForBlockedSubScreen = when (activeSubScreen) {
        ActiveSubScreen.ROLEPLAY_STUDIO -> UpgradeSheetMode.ROLEPLAY
        ActiveSubScreen.PRONUNCIATION_COACH,
        ActiveSubScreen.WRITING_STUDIO,
        ActiveSubScreen.GRAMMAR_STUDIO,
        ActiveSubScreen.AI_CAMERA_LENS,
        ActiveSubScreen.AI_IMMERSION_MODE,
        ActiveSubScreen.AI_CONVERSATION_REPLAY,
        ActiveSubScreen.MISTAKE_RECOVERY_MEMORY,
        ActiveSubScreen.ACCENT_UNDERSTANDING_STUDIO,
        ActiveSubScreen.CONVERSATION_REPAIR_LAB,
        ActiveSubScreen.AI_DEBATE_PARTNER,
        ActiveSubScreen.EXPLAIN_IT_THREE_WAYS,
        ActiveSubScreen.CONVERSATION_DETECTIVE,
        ActiveSubScreen.TEACH_THE_AI,
        ActiveSubScreen.EXPLORE_AI_LABS -> UpgradeSheetMode.AI_UPGRADE
        else -> null
      }
      if (modeForBlockedSubScreen != null) {
        val targetSubScreen = activeSubScreen
        val featureType = when (targetSubScreen) {
          ActiveSubScreen.ROLEPLAY_STUDIO -> com.example.ads.RewardedFeatureType.ROLEPLAY
          ActiveSubScreen.AI_CAMERA_LENS -> com.example.ads.RewardedFeatureType.CAMERA_LENS
          ActiveSubScreen.PRONUNCIATION_COACH -> com.example.ads.RewardedFeatureType.SPEAK_PRACTICE
          else -> com.example.ads.RewardedFeatureType.AI_PRACTICE_LAB
        }
        val hasTimedUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(featureType)
        if (!hasTimedUnlock) {
          val wasRoleplay = targetSubScreen == ActiveSubScreen.ROLEPLAY_STUDIO
          activeSubScreen = ActiveSubScreen.NONE
          AiErrorHandler.requestUpgrade(
            mode = modeForBlockedSubScreen,
            featureKey = if (wasRoleplay) "ai.roleplay.sessions" else featureType.id,
            headline = if (wasRoleplay) "Unlock Roleplay Studio" else "Unlock AI learning",
            reason = if (wasRoleplay) {
              "Roleplay Studio and interactive AI missions are included with LingoSphere Pro."
            } else {
              "AI coaching tools and smart labs are included with LingoSphere Pro."
            },
            onRewardedUnlockGranted = {
              activeSubScreen = targetSubScreen
            }
          )
        }
      }
    }
  }
  var selectedAiPartner by remember(accountSettings.defaultPartnerId) {
    mutableStateOf(
      com.example.data.model.AiConversationPartner.findById(accountSettings.defaultPartnerId)
    )
  }
  var configuredVoice by remember(accountSettings.preferredVoiceId, selectedAiPartner.id) {
    mutableStateOf(
      com.example.data.model.TutorVoice.findById(
        selectedAiPartner.voice.id.ifBlank { accountSettings.preferredVoiceId }
      )
    )
  }
  LaunchedEffect(accountSettings.defaultPartnerId, accountSettings.preferredVoiceId) {
    val (resolvedPartner, resolvedVoice) = withContext(Dispatchers.IO) {
      val prefs = context.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
      val savedPartnerId = prefs.getString("selected_ai_partner_id", null)?.takeIf { it.isNotBlank() }
      val partner = com.example.data.model.AiConversationPartner.findById(
        savedPartnerId ?: accountSettings.defaultPartnerId
      )
      val savedVoiceId = prefs.getString("selected_tutor_voice_id", null)?.takeIf { it.isNotBlank() }
      val voice = com.example.data.model.TutorVoice.findById(
        savedVoiceId ?: partner.voice.id.ifBlank { accountSettings.preferredVoiceId }
      )
      partner to voice
    }
    selectedAiPartner = resolvedPartner
    configuredVoice = resolvedVoice
    speechManager.setTutorVoice(resolvedVoice)
  }
  var configuredCallMode by remember {
    mutableStateOf(com.example.data.model.CallMode.FREE_CONVERSATION)
  }
  var configuredTextMode by remember { mutableStateOf(false) }
  var configuredGroupPartners by remember {
    mutableStateOf<List<com.example.data.model.AiConversationPartner>>(emptyList())
  }
  var resumedConversationRecord by remember {
    mutableStateOf<com.example.data.model.SavedAiConversationRecord?>(null)
  }
  var savedAiConversations by remember {
    mutableStateOf<List<com.example.data.model.SavedAiConversationRecord>>(emptyList())
  }
  var activeUserCallRoom by remember { mutableStateOf<CallRoom?>(null) }
  var pendingIntentIncomingRoom by remember { mutableStateOf<CallRoom?>(null) }
  val pendingNotificationDeepLink by com.example.notifications.SmartNotificationLaunchRegistry.deepLinkFlow.collectAsState()

  val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
  ) { _ ->
    com.example.webrtc.FcmTokenManager.syncCurrentUserTokenAsync(context)
  }

  LaunchedEffect(Unit) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      delay(5000L)
      val granted = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == android.content.pm.PackageManager.PERMISSION_GRANTED
      if (!granted) {
        try {
          notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) {}
      }
    }
  }

  val callActionScope = rememberCoroutineScope()

  LaunchedEffect(launchCommand) {
    val cmd = launchCommand ?: return@LaunchedEffect
    com.example.webrtc.IncomingCallRegistry.consumeLaunchCommand()
    when (cmd.action) {
      com.example.webrtc.IncomingCallContract.ACTION_ANSWER_CALL -> {
        com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, cmd.callRoom.roomId)
        com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, cmd.callRoom.roomId)
        pendingIntentIncomingRoom = null
        val latestSnap = viewModel.getCallRoomSnapshot(cmd.callRoom.roomId)
        if (latestSnap != null && latestSnap.isTerminalStatus()) {
          Toast.makeText(context, "Call has already ended", Toast.LENGTH_SHORT).show()
        } else {
          val activeRoom = latestSnap ?: cmd.callRoom
          viewModel.acceptIncomingCall(activeRoom.roomId, isVideo = (activeRoom.callType == "video"))
          activeUserCallRoom = activeRoom
        }
      }
      com.example.webrtc.IncomingCallContract.ACTION_DECLINE_CALL -> {
        com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, cmd.callRoom.roomId)
        com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, cmd.callRoom.roomId)
        pendingIntentIncomingRoom = null
        viewModel.rejectIncomingCall(cmd.callRoom.roomId, isBusy = false)
      }
      else -> {
        if (!com.example.webrtc.IncomingCallRegistry.isRoomTerminal(context, cmd.callRoom.roomId)) {
          val latestSnap = viewModel.getCallRoomSnapshot(cmd.callRoom.roomId)
          if (latestSnap != null && latestSnap.isTerminalStatus()) {
            com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, cmd.callRoom.roomId)
            com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, cmd.callRoom.roomId)
            pendingIntentIncomingRoom = null
          } else {
            pendingIntentIncomingRoom = latestSnap ?: cmd.callRoom
          }
        }
      }
    }
  }

  val pendingRoomId = pendingIntentIncomingRoom?.roomId
  LaunchedEffect(pendingRoomId) {
    if (pendingRoomId.isNullOrBlank()) return@LaunchedEffect
    viewModel.observeCallRoom(pendingRoomId).collect { liveRoom ->
      if (liveRoom == null || liveRoom.isTerminalStatus()) {
        com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, pendingRoomId)
        com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, pendingRoomId)
        pendingIntentIncomingRoom = null
      }
    }
  }

  val effectiveIncomingCallRoom = remember(incomingCallRoom, pendingIntentIncomingRoom) {
    val candidate = incomingCallRoom ?: pendingIntentIncomingRoom
    candidate?.takeIf {
      !it.isTerminalStatus() && !com.example.webrtc.IncomingCallRegistry.isRoomTerminal(context, it.roomId)
    }
  }

  val unreadNotificationsCount = remember(notifications) {
    notifications.count { !it.isRead }
  }

  var showChangeLearningLanguageSheet by remember { mutableStateOf(false) }
  val floatingContinuePrefs by viewModel.floatingContinuePrefsState.collectAsState()

  val currentTargetLang = Language.findByCode(userProfile.targetLanguageCode)
  val currentNativeLang = Language.findByCode(userProfile.nativeLanguageCode)
  val activeCefrStageLabel = userProfile.effectiveCefrForLanguage(
    langCode = userProfile.targetLanguageCode,
    fallbackPlacementCefr = placementEvaluation?.recommendedCefrBand.orEmpty()
  )
  val isRtlLanguage = currentTargetLang.isRtl || currentNativeLang.isRtl
  val layoutDirection = if (isRtlLanguage) LayoutDirection.Rtl else LayoutDirection.Ltr

  var curriculumLessons by remember(userProfile.targetLanguageCode) {
    mutableStateOf<List<com.example.data.model.CurriculumLesson>>(emptyList())
  }
  LaunchedEffect(userProfile.targetLanguageCode, userProfile.nativeLanguageCode) {
    if (!android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      delay(150L)
    }
    curriculumLessons = withContext(Dispatchers.Default) {
      CurriculumData.getLessonsForLanguage(userProfile.targetLanguageCode, userProfile.nativeLanguageCode)
    }
  }

  val nextLesson = remember(
    curriculumLessons,
    userProfile.completedLessonIds,
    userProfile.proficiencyLevel,
    activeCefrStageLabel
  ) {
    val uncompleted = curriculumLessons.filterNot { userProfile.completedLessonIds.contains(it.id) }
    uncompleted.firstOrNull { it.cefrLevel.equals(activeCefrStageLabel, ignoreCase = true) }
      ?: uncompleted.firstOrNull { it.proficiencyLevel.equals(userProfile.proficiencyLevel, ignoreCase = true) }
      ?: uncompleted.firstOrNull()
      ?: curriculumLessons.firstOrNull()
  }

  val navigateToNotificationDeepLink: (com.example.notifications.SmartNotificationDeepLink) -> Unit =
    remember(curriculumLessons, nextLesson) {
      { deepLink ->
        when (deepLink.route.lowercase()) {
          "word_of_the_day" -> {
            selectedWordOfTheDay = if (deepLink.word.isNotBlank()) {
              com.example.notifications.WordOfTheDayEntry(
                word = deepLink.word,
                meaning = deepLink.meaning.ifBlank { "Very interesting or mysterious." },
                pronunciation = deepLink.pronunciation.ifBlank { "/ɪnˈtriːɡɪŋ/ (in-TREE-ging)" },
                exampleSentence = deepLink.exampleSentence.ifBlank { "That's an intriguing idea!" },
                lessonId = deepLink.lessonId.ifBlank { "en_vocab_1" }
              )
            } else {
              com.example.notifications.SmartEnglishContentCatalog.findWordByText(deepLink.word)
                ?: com.example.notifications.SmartEnglishContentCatalog.getTodayWordOfTheDay(preferFeaturedIntriguing = true)
            }
            activeSubScreen = ActiveSubScreen.PRONUNCIATION_COACH
          }
          "vocab_challenge" -> {
            val matchingLesson = curriculumLessons.firstOrNull {
              it.id.equals(deepLink.lessonId, ignoreCase = true) ||
                it.category.contains("Vocabulary", ignoreCase = true)
            }
            if (deepLink.lessonId.isNotBlank() && matchingLesson != null && matchingLesson.id.equals(deepLink.lessonId, ignoreCase = true)) {
              activeSubScreen = ActiveSubScreen.NONE
              viewModel.startLesson(matchingLesson)
            } else {
              activeSubScreen = ActiveSubScreen.DAILY_CHALLENGES
            }
          }
          "grammar_tip" -> {
            val grammarLesson = curriculumLessons.firstOrNull {
              it.id.equals(deepLink.lessonId, ignoreCase = true) ||
                it.category.contains("Grammar", ignoreCase = true)
            } ?: nextLesson
            activeSubScreen = ActiveSubScreen.NONE
            if (grammarLesson != null) {
              viewModel.startLesson(grammarLesson)
            } else {
              currentScreen = LingoScreen.LEARN
            }
          }
          "speaking_reminder" -> {
            activeSubScreen = ActiveSubScreen.NONE
            currentScreen = LingoScreen.SPEAK
          }
          "streak_reminder" -> {
            activeSubScreen = ActiveSubScreen.NONE
            if (nextLesson != null) {
              viewModel.startLesson(nextLesson)
            } else {
              currentScreen = LingoScreen.LEARN
            }
          }
          "speaking_invitation",
          "missed_call" -> {
            activeSubScreen = ActiveSubScreen.NONE
            communityInitialTab = 0
            communityInitialPartnerId = deepLink.partnerId.takeIf { it.isNotBlank() }
            communityInitialOpenPartnerProfile = false
            currentScreen = LingoScreen.COMMUNITY
          }
          "partner_profile" -> {
            activeSubScreen = ActiveSubScreen.NONE
            communityInitialTab = 0
            communityInitialPartnerId = deepLink.partnerId.takeIf { it.isNotBlank() }
            communityInitialOpenPartnerProfile = true
            currentScreen = LingoScreen.COMMUNITY
          }
          "app_update" -> {
            activeSubScreen = ActiveSubScreen.NONE
          }
          else -> {
            activeSubScreen = ActiveSubScreen.NOTIFICATIONS
          }
        }
      }
    }

  LaunchedEffect(pendingNotificationDeepLink, curriculumLessons) {
    val deepLink = pendingNotificationDeepLink ?: return@LaunchedEffect
    com.example.notifications.SmartNotificationLaunchRegistry.consumeDeepLink()
    navigateToNotificationDeepLink(deepLink)
  }

  if (
    currentScreen != LingoScreen.HOME &&
    activeSubScreen == ActiveSubScreen.NONE &&
    activeLesson == null &&
    !isVoiceCallActive &&
    !isVideoTutorCallActive &&
    activeUserCallRoom == null &&
    effectiveIncomingCallRoom == null
  ) {
    BackHandler {
      currentScreen = LingoScreen.HOME
    }
  }

  LaunchedEffect(
    effectiveIncomingCallRoom?.roomId,
    activeUserCallRoom?.roomId,
    isVoiceCallActive,
    isVideoTutorCallActive,
    accountSettings.allowIncomingCalls,
    accountSettings.allowVoiceCalls,
    accountSettings.allowVideoCalls,
    myCommunityProfile?.allowCalls
  ) {
    val incoming = effectiveIncomingCallRoom
    if (incoming != null) {
      val masterAllowed = (myCommunityProfile?.allowCalls ?: true) && accountSettings.allowIncomingCalls
      val callTypeAllowed = if (incoming.callType == "video") accountSettings.allowVideoCalls else accountSettings.allowVoiceCalls
      val isBusyInOtherCall = isVoiceCallActive || isVideoTutorCallActive || (activeUserCallRoom != null && activeUserCallRoom?.roomId != incoming.roomId)
      if (!masterAllowed || !callTypeAllowed || isBusyInOtherCall) {
        com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, incoming.roomId)
        com.example.webrtc.NativeCallNotificationManager.dismissIncomingCallNotification(context, incoming.roomId)
        pendingIntentIncomingRoom = null
        viewModel.rejectIncomingCall(incoming.roomId, isBusy = isBusyInOtherCall)
      }
    }
  }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .then(
          if (
            activeSubScreen != ActiveSubScreen.NONE ||
            activeLesson != null ||
            isVoiceCallActive ||
            isVideoTutorCallActive ||
            activeUserCallRoom != null ||
            effectiveIncomingCallRoom != null
          ) {
            Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
          } else {
            Modifier
          }
        )
    ) {
    when {
      effectiveIncomingCallRoom != null && activeUserCallRoom == null && !isVoiceCallActive -> {
        val currentIncoming = effectiveIncomingCallRoom
        val callerPhotoUrl = remember(communityProfiles, currentIncoming.callerId) {
          communityProfiles.firstOrNull { it.userId == currentIncoming.callerId }?.avatarUrl.orEmpty()
        }
        IncomingCallOverlay(
          callRoom = currentIncoming,
          callerAvatarUrl = callerPhotoUrl,
          onRingingStarted = { roomId ->
            viewModel.markCallRinging(roomId)
          },
          onAccept = { acceptedRoom ->
            pendingIntentIncomingRoom = null
            callActionScope.launch {
              val latestSnap = viewModel.getCallRoomSnapshot(acceptedRoom.roomId)
              if (latestSnap != null && latestSnap.isTerminalStatus()) {
                Toast.makeText(context, "Call has already ended", Toast.LENGTH_SHORT).show()
              } else {
                val activeRoom = latestSnap ?: acceptedRoom
                viewModel.acceptIncomingCall(activeRoom.roomId, isVideo = (activeRoom.callType == "video"))
                activeUserCallRoom = activeRoom
              }
            }
          },
          onDecline = {
            pendingIntentIncomingRoom = null
            viewModel.rejectIncomingCall(currentIncoming.roomId, isBusy = false)
          },
          onTimeout = {
            pendingIntentIncomingRoom = null
            viewModel.markCallMissed(currentIncoming.roomId)
          }
        )
      }

      activeUserCallRoom != null -> {
        LiveUserCallScreen(
          initialRoom = activeUserCallRoom!!,
          viewModel = viewModel,
          speechManager = speechManager,
          onEndCall = { activeUserCallRoom = null },
          onSwitchToAiTutor = {
            activeUserCallRoom = null
            isVoiceCallActive = true
          },
          onCallAgain = { newRoom -> activeUserCallRoom = newRoom }
        )
      }

      isVoiceCallActive -> {
        AiVoiceCallScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          pastMistakesMemory = pastMistakesMemory,
          onSaveVocabToFlashcard = { term, translation, example ->
            viewModel.saveVocabularyToFlashcards(term, translation, example)
            Toast.makeText(context, "Saved '$term' to Flashcards!", Toast.LENGTH_SHORT).show()
          },
          onRecordGrammarMistake = { entry -> viewModel.addMistakeNotebookEntry(entry) },
          onEndCallAndExit = {
            resumedConversationRecord = null
            configuredGroupPartners = emptyList()
            configuredTextMode = false
            isVoiceCallActive = false
          },
          onSaveSession = { session -> viewModel.saveSpeakingSession(session) },
          initialPartner = selectedAiPartner,
          initialVoice = configuredVoice,
          initialMode = configuredCallMode,
          initialCorrectionMode = com.example.data.model.CorrectionMode.findById(accountSettings.correctionModeId),
          initialTextMode = configuredTextMode,
          initialGroupPartners = configuredGroupPartners,
          resumedRecord = resumedConversationRecord,
          onConversationRecordSaved = { record ->
            savedAiConversations = (listOf(record) + savedAiConversations.filterNot { it.id == record.id }).take(25)
          },
          billingRepository = billingRepository,
          onOpenSubscription = {
            resumedConversationRecord = null
            configuredGroupPartners = emptyList()
            configuredTextMode = false
            isVoiceCallActive = false
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          }
        )
      }

      isVideoTutorCallActive -> {
        AiVideoTutorScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          pastMistakesMemory = pastMistakesMemory,
          onSaveVocabToFlashcard = { term, translation, example ->
            viewModel.saveVocabularyToFlashcards(term, translation, example)
            Toast.makeText(context, "Saved '$term' to Flashcards!", Toast.LENGTH_SHORT).show()
          },
          onRecordGrammarMistake = { entry -> viewModel.addMistakeNotebookEntry(entry) },
          onEndCallAndExit = { isVideoTutorCallActive = false },
          onSaveSession = { session -> viewModel.saveSpeakingSession(session) }
        )
      }

      activeLesson != null -> {
        val currentLesson = activeLesson!!
        val savedCheckpoint = inProgressLessonsMap[currentLesson.id]
        val isReplayLesson = userProfile.completedLessonIds.contains(currentLesson.id)
        LessonExerciseScreen(
          lesson = currentLesson,
          speechManager = speechManager,
          onFinishLesson = { score, xp ->
            viewModel.finishActiveLesson(score, xp)
            val allLangLessons = CurriculumData.getLessonsForLanguage(currentLesson.targetLanguageCode)
            val curIdx = allLangLessons.indexOfFirst { it.id == currentLesson.id }
            val upcoming = allLangLessons.getOrNull(curIdx + 1)
            if (upcoming != null && userProfile.isEntitlementVerified) {
              com.example.ai.GeminiService.pregenerateNextLessonInBackground(
                userContext = com.example.ai.GeminiUserContext(
                  targetLanguageCode = currentLesson.targetLanguageCode,
                  nativeLanguageCode = userProfile.nativeLanguageCode,
                  cefrLevel = upcoming.cefrLevel
                ),
                nextLesson = upcoming
              )
            }
          },
          onNavigateBack = { viewModel.dismissLesson() },
          initialExerciseIndex = savedCheckpoint?.currentExerciseIndex ?: 0,
          initialCorrectCount = savedCheckpoint?.correctCount ?: 0,
          onSaveCheckpoint = { exerciseIdx, correctCnt, totalEx ->
            viewModel.saveLessonCheckpoint(
              lessonId = currentLesson.id,
              languageCode = currentLesson.targetLanguageCode,
              currentExerciseIndex = exerciseIdx,
              correctCount = correctCnt,
              totalExercises = totalEx
            )
          },
          isReplay = isReplayLesson,
          onSaveVocabToFlashcard = { front, back, phonetic ->
            viewModel.saveVocabularyToFlashcards(front, back, phonetic)
            Toast.makeText(context, "Saved '$front' to Flashcards!", Toast.LENGTH_SHORT).show()
          },
          onRecordMistake = { entry ->
            viewModel.addMistakeNotebookEntry(entry)
          }
        )
      }

      activeSubScreen == ActiveSubScreen.EXPLORE_LANGUAGES -> {
        ExploreLanguagesScreen(
          userProfile = userProfile,
          communityProfiles = communityProfiles,
          speechManager = speechManager,
          onSelectTargetLanguage = { code -> viewModel.changeTargetLanguage(code) },
          onSelectNativeLanguage = { code -> viewModel.changeNativeLanguage(code) },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.DAILY_CHALLENGES -> {
        DailyChallengesScreen(
          userProfile = userProfile,
          lessonProgress = lessonProgress,
          flashcards = flashcards,
          speakingSessions = speakingSessions,
          speechManager = speechManager,
          completedMissions = completedDailyMissions,
          onCompleteMission = { eval, xp ->
            viewModel.recordCompletedDailyLifeMission(eval, xp)
            Toast.makeText(context, "Daily Life Mission Completed! +$xp XP", Toast.LENGTH_SHORT).show()
          },
          onNavigateToLearn = {
            activeSubScreen = ActiveSubScreen.NONE
            currentScreen = LingoScreen.LEARN
          },
          onNavigateToSpeak = {
            activeSubScreen = ActiveSubScreen.NONE
            currentScreen = LingoScreen.SPEAK
          },
          onNavigateToPractice = {
            activeSubScreen = ActiveSubScreen.NONE
            currentScreen = LingoScreen.PRACTICE
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.PRONUNCIATION_COACH -> {
        PronunciationPracticeScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onSaveSession = { session -> viewModel.saveSpeakingSession(session) },
          onBack = {
            selectedWordOfTheDay = null
            activeSubScreen = ActiveSubScreen.NONE
          },
          pronunciationHistory = pronunciationHistory,
          onRecordDiagnostic = { diag -> viewModel.recordPronunciationDiagnostic(diag) },
          initialWordOfTheDay = selectedWordOfTheDay,
          onOpenSubscription = {
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          }
        )
      }

      activeSubScreen == ActiveSubScreen.VOCABULARY_BUILDER -> {
        VocabularyBuilderScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          flashcards = flashcards,
          srsMetadataMap = srsMetadataMap,
          onReviewCardWithRating = { card, rating ->
            viewModel.reviewFlashcardWithRating(card, rating)
          },
          onSaveWordToFlashcards = { front, back, phonetic ->
            viewModel.saveVocabularyToFlashcards(front, back, phonetic)
          },
          onDeleteFlashcard = { cardId ->
            viewModel.deleteFlashcard(cardId)
          },
          onOpenPronunciationForWord = { wordText ->
            selectedWordOfTheDay = com.example.notifications.WordOfTheDayEntry(
              word = wordText,
              meaning = "Vocabulary drill",
              pronunciation = "",
              exampleSentence = "",
              lessonId = ""
            )
            activeSubScreen = ActiveSubScreen.PRONUNCIATION_COACH
          },
          onAddXp = { xp ->
            viewModel.recordGameOrQuizCompletion("vocab_quiz", "Vocabulary Quiz", 100, xp, null)
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE },
          onOpenSubscription = {
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          }
        )
      }

      activeSubScreen == ActiveSubScreen.LISTENING_PRACTICE -> {
        com.example.ui.screens.ListeningPracticeScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          initialSection = booksListeningInitialSection,
          bookmarkedReadingIds = bookmarkedReadingIds,
          completedAcademyIds = completedAcademyIds,
          onToggleReadingBookmark = { id -> viewModel.toggleReadingBookmark(id) },
          onCompleteReadingItem = { id, title, pct, xp ->
            viewModel.recordAcademyCompletion(id, title, pct, xp)
            if (xp > 0) {
              Toast.makeText(context, "Completed '$title'! +$xp XP", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "Reviewed '$title'!", Toast.LENGTH_SHORT).show()
            }
          },
          onSaveVocabToFlashcard = { front, back, phonetic ->
            viewModel.saveVocabularyToFlashcards(front, back, phonetic)
          },
          onAddXp = { xp ->
            viewModel.recordGameOrQuizCompletion("listening_quiz", "Books & Listening Studio", 100, xp.toInt(), null)
          },
          onCompleteLesson = { lessonId ->
            viewModel.recordGameOrQuizCompletion(lessonId, "Books & Listening Studio", 100, 15, null)
          },
          onOpenSubscription = {
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          },
          onBack = {
            booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.MAIN
            activeSubScreen = ActiveSubScreen.NONE
          }
        )
      }

      activeSubScreen == ActiveSubScreen.WRITING_STUDIO -> {
        com.example.ui.screens.WritingStudioScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onAddXp = { xp ->
            viewModel.recordGameOrQuizCompletion("writing_studio", "Writing Studio", 100, xp.toInt(), null)
          },
          onOpenSubscription = {
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.GRAMMAR_STUDIO -> {
        com.example.ui.screens.GrammarStudioScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onAddXp = { xp ->
            viewModel.recordGameOrQuizCompletion("grammar_studio", "Grammar Studio", 100, xp.toInt(), null)
          },
          onOpenSubscription = {
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.ROLEPLAY_STUDIO -> {
        com.example.ui.screens.RoleplayStudioScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onAddXp = { xp ->
            viewModel.recordGameOrQuizCompletion("roleplay_studio", "AI Roleplay", 100, xp.toInt(), null)
          },
          onOpenSubscription = {
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.PROGRESS_DASHBOARD -> {
        Box(modifier = Modifier.fillMaxSize()) {
          ProgressDashboardScreen(
            userProfile = userProfile,
            lessonProgress = lessonProgress,
            flashcards = flashcards,
            speakingSessions = speakingSessions,
            onBack = { activeSubScreen = ActiveSubScreen.NONE },
            weeklyReport = weeklyReport,
            onSyncOfflineNow = {
              viewModel.syncOfflineProgressNow()
              Toast.makeText(context, "Offline progress synced with cloud!", Toast.LENGTH_SHORT).show()
            },
            onOpenLanguagePortfolio = { activeSubScreen = ActiveSubScreen.LANGUAGE_PORTFOLIO }
          )
          if (activeUpgradePrompt == null) {
            com.example.ads.LingoBannerAdBar(
              userProfile = userProfile,
              placementName = "progress",
              onUpgradeToProClick = {
                premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
                activeSubScreen = ActiveSubScreen.PREMIUM_HUB
              },
              modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
            )
          }
        }
      }

      activeSubScreen == ActiveSubScreen.NOTIFICATIONS -> {
        NotificationsScreen(
          notifications = notifications,
          onMarkRead = { notifId -> viewModel.markNotificationRead(notifId) },
          onBack = { activeSubScreen = ActiveSubScreen.NONE },
          accountSettings = accountSettings,
          onUpdateAccountSettings = { updated -> viewModel.updateAccountSettings(updated) },
          onOpenNotificationDeepLink = { deepLink ->
            navigateToNotificationDeepLink(deepLink)
          }
        )
      }

      activeSubScreen == ActiveSubScreen.HELP_SUPPORT -> {
        HelpSupportScreen(
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.CULTURE_PASSPORT -> {
        CulturePassportScreen(
          userProfile = userProfile,
          speakingSessions = speakingSessions,
          speechManager = speechManager,
          initialTab = cultureInitialTab,
          onSelectLanguageCode = { code -> viewModel.changeTargetLanguage(code) },
          onSaveSpeakingSession = { session -> viewModel.saveSpeakingSession(session) },
          onSaveVocabToFlashcard = { term, translation, phonetic, langCode ->
            viewModel.saveVideoVocabToFlashcard(term, translation, phonetic, langCode)
          },
          onUnlockAchievementAndXp = { achievementId, notifTitle, notifMsg, xp ->
            viewModel.unlockCulturalAchievementAndXp(achievementId, notifTitle, notifMsg, xp)
          },
          onOpenSpeakingRooms = { activeSubScreen = ActiveSubScreen.SPEAKING_ROOMS },
          onOpenLanguageExchange = { activeSubScreen = ActiveSubScreen.LANGUAGE_EXCHANGE },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.SPEAKING_ROOMS -> {
        SpeakingRoomsScreen(
          userProfile = userProfile,
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.LANGUAGE_EXCHANGE -> {
        LanguageExchangeScreen(
          userProfile = userProfile,
          communityProfiles = communityProfiles,
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE },
          onStartLiveCall = { callRoom -> activeUserCallRoom = callRoom }
        )
      }

      activeSubScreen == ActiveSubScreen.SMART_MATCHING -> {
        SmartMatchingScreen(
          userProfile = userProfile,
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE },
          onStartLiveCall = { callRoom -> activeUserCallRoom = callRoom },
          onPracticeWithAi = {
            activeSubScreen = ActiveSubScreen.NONE
            isVoiceCallActive = true
          }
        )
      }

      activeSubScreen == ActiveSubScreen.AI_CAMERA_LENS -> {
        AiCameraLanguageScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onSaveVocabToFlashcard = { term, translation, phonetic ->
            viewModel.saveVocabularyToFlashcards(term, translation, phonetic)
            Toast.makeText(context, "Saved '$term' to Flashcards!", Toast.LENGTH_SHORT).show()
          },
          onRecordMistake = { cat, orig, corr, exp ->
            viewModel.recordExploreMistake("Camera Lens", cat, orig, corr, exp)
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.PREMIUM_HUB -> {
        PremiumHubScreen(
          userProfile = userProfile,
          billingRepository = billingRepository,
          speechManager = speechManager,
          curriculumLessons = curriculumLessons,
          downloadedLessonIds = downloadedLessonIds,
          pendingOfflineSyncCount = pendingOfflineSyncCount,
          flashcards = flashcards,
          speakingSessions = speakingSessions,
          mistakeEntries = mistakeNotebookEntries,
          weeklyReport = weeklyReport,
          customProDecks = customProDecks,
          cefrStageLabel = activeCefrStageLabel,
          initialSection = premiumHubInitialSection,
          onActivatePlan = { tier, token ->
            viewModel.updateSubscription(tier, token)
          },
          onCancelSubscription = {
            viewModel.cancelSubscriptionToFree()
          },
          onSaveCustomProDeck = { deck ->
            viewModel.saveCustomProDeck(deck, alsoAddToSrsFlashcards = true)
          },
          onDownloadAllLessons = { allLessons ->
            viewModel.downloadAllLessonsForLanguage(allLessons)
            Toast.makeText(context, "All ${allLessons.size} lessons saved for offline learning!", Toast.LENGTH_SHORT).show()
          },
          onSyncOfflineNow = {
            viewModel.syncOfflineProgressNow()
            Toast.makeText(context, "Offline progress synced with cloud!", Toast.LENGTH_SHORT).show()
          },
          onStartVoiceCall = {
            activeSubScreen = ActiveSubScreen.NONE
            isVoiceCallActive = true
          },
          onStartVideoTutorCall = {
            activeSubScreen = ActiveSubScreen.NONE
            isVideoTutorCallActive = true
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.AI_IMMERSION_MODE -> {
        AiImmersionModeScreen(
          viewModel = viewModel,
          speechManager = speechManager,
          onOpenReplayStudio = { activeSubScreen = ActiveSubScreen.AI_CONVERSATION_REPLAY },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.AI_CONVERSATION_REPLAY -> {
        AiConversationReplayScreen(
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.MISTAKE_RECOVERY_MEMORY -> {
        IntelligentMistakeRecoveryScreen(
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.ACCENT_UNDERSTANDING_STUDIO -> {
        AccentUnderstandingStudioScreen(
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.LANGUAGE_PORTFOLIO -> {
        PersonalLanguagePortfolioScreen(
          viewModel = viewModel,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.CONVERSATION_REPAIR_LAB -> {
        val completedRepairs by viewModel.completedRepairScenariosState.collectAsState()
        com.example.ui.screens.ConversationRepairLabScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          completedScenarioIds = completedRepairs,
          onScenarioResolved = { scenarioId, langCode ->
            viewModel.recordCompletedRepairScenario(scenarioId, langCode)
          },
          onRecordMistake = { cat, orig, corr, exp ->
            viewModel.recordExploreMistake("Conversation Repair", cat, orig, corr, exp)
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.LANGUAGE_EMERGENCY_MODE -> {
        val offlineLangs by viewModel.offlineEmergencyLangsState.collectAsState()
        com.example.ui.screens.LanguageEmergencyModeScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          offlineDownloadedLangs = offlineLangs,
          onToggleOfflinePack = { langCode -> viewModel.toggleOfflineEmergencyPack(langCode) },
          onPacksChanged = { viewModel.refreshOfflineEmergencyPacks() },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.AI_DEBATE_PARTNER -> {
        com.example.ui.screens.AiDebatePartnerScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onRecordMistake = { cat, orig, corr, exp ->
            viewModel.recordExploreMistake("AI Debate Partner", cat, orig, corr, exp)
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.EXPLAIN_IT_THREE_WAYS -> {
        com.example.ui.screens.ExplainItThreeWaysScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onRecordMistake = { cat, orig, corr, exp ->
            viewModel.recordExploreMistake("Explain It Three Ways", cat, orig, corr, exp)
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.CONVERSATION_DETECTIVE -> {
        com.example.ui.screens.ConversationDetectiveScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onRecordMistake = { cat, orig, corr, exp ->
            viewModel.recordExploreMistake("Conversation Detective", cat, orig, corr, exp)
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.TEACH_THE_AI -> {
        com.example.ui.screens.TeachTheAiScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.EXPLORE_COMMUNITY_CULTURE -> {
        com.example.ui.screens.ExploreCommunityCultureScreen(
          onNavigateToPractice = {
            activeSubScreen = ActiveSubScreen.NONE
            currentScreen = LingoScreen.PRACTICE
          },
          onOpenCultureJourney = {
            cultureInitialTab = 0
            activeSubScreen = ActiveSubScreen.CULTURE_PASSPORT
          },
          onNavigateToVideos = {
            activeSubScreen = ActiveSubScreen.NONE
            currentScreen = LingoScreen.VIDEOS
          },
          onOpenSpeakingRooms = { activeSubScreen = ActiveSubScreen.SPEAKING_ROOMS },
          onOpenLanguageExchange = { activeSubScreen = ActiveSubScreen.LANGUAGE_EXCHANGE },
          onOpenSmartMatching = { activeSubScreen = ActiveSubScreen.SMART_MATCHING },
          onOpenInviteFriends = {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(android.content.Intent.EXTRA_SUBJECT, "Join me on LingoSphere!")
              putExtra(android.content.Intent.EXTRA_TEXT, "Come practice ${com.example.data.model.Language.findByCode(userProfile.targetLanguageCode).name} with me on LingoSphere AI Language Tutor!")
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Invite Friends to LingoSphere"))
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.EXPLORE_AI_LABS -> {
        com.example.ui.screens.ExploreAiPracticeLabsScreen(
          onOpenImmersionMode = { activeSubScreen = ActiveSubScreen.AI_IMMERSION_MODE },
          onOpenCameraLens = { activeSubScreen = ActiveSubScreen.AI_CAMERA_LENS },
          onOpenLanguageEmergencyMode = { activeSubScreen = ActiveSubScreen.LANGUAGE_EMERGENCY_MODE },
          onOpenAiDebatePartner = { activeSubScreen = ActiveSubScreen.AI_DEBATE_PARTNER },
          onOpenConversationRepairLab = { activeSubScreen = ActiveSubScreen.CONVERSATION_REPAIR_LAB },
          onOpenExplainItThreeWays = { activeSubScreen = ActiveSubScreen.EXPLAIN_IT_THREE_WAYS },
          onOpenConversationDetective = { activeSubScreen = ActiveSubScreen.CONVERSATION_DETECTIVE },
          onOpenMistakeRecovery = { activeSubScreen = ActiveSubScreen.MISTAKE_RECOVERY_MEMORY },
          onOpenTeachTheAi = { activeSubScreen = ActiveSubScreen.TEACH_THE_AI },
          onOpenAccentStudio = { activeSubScreen = ActiveSubScreen.ACCENT_UNDERSTANDING_STUDIO },
          onOpenConversationReplay = { activeSubScreen = ActiveSubScreen.AI_CONVERSATION_REPLAY },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.EXPLORE_PROGRESS_LANGUAGES -> {
        com.example.ui.screens.ExploreProgressAndLanguagesScreen(
          userProfile = userProfile,
          completedLessonsCount = userProfile.completedLessonIds.size,
          weeklySessionsCount = speakingSessions.size,
          cefrStageLabel = activeCefrStageLabel,
          onOpenDailyChallenges = { activeSubScreen = ActiveSubScreen.DAILY_CHALLENGES },
          onOpenProgressDashboard = { activeSubScreen = ActiveSubScreen.PROGRESS_DASHBOARD },
          onOpenLanguagePortfolio = { activeSubScreen = ActiveSubScreen.LANGUAGE_PORTFOLIO },
          onOpenExploreLanguages = { activeSubScreen = ActiveSubScreen.EXPLORE_LANGUAGES },
          onOpenInviteFriends = {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(android.content.Intent.EXTRA_SUBJECT, "Join me on LingoSphere!")
              putExtra(android.content.Intent.EXTRA_TEXT, "Come practice ${com.example.data.model.Language.findByCode(userProfile.targetLanguageCode).name} with me on LingoSphere AI Language Tutor!")
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Invite Friends to LingoSphere"))
          },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      activeSubScreen == ActiveSubScreen.EXPLORE_LEARNING_TOOLS -> {
        com.example.ui.screens.ExploreLearningToolsScreen(
          onOpenReadingStudio = {
            booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.READING_ACADEMY
            activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
          },
          onOpenListeningPractice = {
            booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.MAIN
            activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
          },
          onOpenVocabularyBuilder = { activeSubScreen = ActiveSubScreen.VOCABULARY_BUILDER },
          onOpenWritingStudio = { activeSubScreen = ActiveSubScreen.WRITING_STUDIO },
          onOpenGrammarStudio = { activeSubScreen = ActiveSubScreen.GRAMMAR_STUDIO },
          onOpenRoleplayStudio = { activeSubScreen = ActiveSubScreen.ROLEPLAY_STUDIO },
          onOpenPronunciation = { activeSubScreen = ActiveSubScreen.PRONUNCIATION_COACH },
          onBack = { activeSubScreen = ActiveSubScreen.NONE }
        )
      }

      else -> {
        val density = LocalDensity.current
        val imeInsets = WindowInsets.ime
        val isKeyboardOpen by remember(density) {
          androidx.compose.runtime.derivedStateOf { imeInsets.getBottom(density) > 0 }
        }
        var isMainScreenBodyMounted by remember { mutableStateOf(isRobolectricRuntime) }
        LaunchedEffect(Unit) {
          if (!isMainScreenBodyMounted) {
            withFrameNanos { }
            isMainScreenBodyMounted = true
          }
        }
        Scaffold(
          containerColor = com.example.ui.components.LingoTokens.bgCanvas,
          contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
          bottomBar = {
            if (!isKeyboardOpen) {
              Column(modifier = Modifier.fillMaxWidth()) {
                val showBannerOnCurrentScreen = when (currentScreen) {
                  LingoScreen.LEARN,
                  LingoScreen.PROFILE -> true
                  else -> false
                }
                if (showBannerOnCurrentScreen && activeUpgradePrompt == null && activeSubScreen != ActiveSubScreen.PREMIUM_HUB) {
                  com.example.ads.LingoBannerAdBar(
                    userProfile = userProfile,
                    placementName = currentScreen.name.lowercase(),
                    onUpgradeToProClick = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    }
                  )
                }
                LingoBottomBar(
                  currentScreen = currentScreen,
                  onScreenSelected = {
                    activeSubScreen = ActiveSubScreen.NONE
                    currentScreen = it
                  }
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .consumeWindowInsets(innerPadding)
          ) {
            if (!isMainScreenBodyMounted) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(com.example.ui.components.LingoTokens.bgCanvas)
              )
            } else {
            AnimatedContent(
              targetState = currentScreen,
              transitionSpec = {
                androidx.compose.animation.ContentTransform(
                  targetContentEnter = fadeIn(androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.scaleIn(
                      initialScale = 0.985f,
                      animationSpec = androidx.compose.animation.core.tween(220)
                    ),
                  initialContentExit = fadeOut(androidx.compose.animation.core.tween(140))
                )
              },
              label = "screen_transition"
            ) { targetScreen ->
              when (targetScreen) {
                LingoScreen.HOME -> {
                  HomeScreen(
                    userProfile = userProfile,
                    nextLesson = nextLesson,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onNavigateToLearn = { currentScreen = LingoScreen.LEARN },
                    onNavigateToSpeak = { currentScreen = LingoScreen.SPEAK },
                    onNavigateToPractice = { currentScreen = LingoScreen.PRACTICE },
                    onStartLesson = { lesson -> viewModel.startLesson(lesson) },
                    onTargetLanguageChanged = { newCode -> viewModel.changeTargetLanguage(newCode) },
                    onStartVoiceCall = { isVoiceCallActive = true },
                    onStartVideoTutorCall = { isVideoTutorCallActive = true },
                    onOpenExploreLanguages = { activeSubScreen = ActiveSubScreen.EXPLORE_LANGUAGES },
                    onOpenCommunityHub = { tabIdx ->
                      communityInitialTab = tabIdx
                      currentScreen = LingoScreen.COMMUNITY
                    },
                    onOpenPronunciation = { activeSubScreen = ActiveSubScreen.PRONUNCIATION_COACH },
                    onOpenDailyChallenges = { activeSubScreen = ActiveSubScreen.DAILY_CHALLENGES },
                    onOpenProgressDashboard = { activeSubScreen = ActiveSubScreen.PROGRESS_DASHBOARD },
                    onOpenNotifications = { activeSubScreen = ActiveSubScreen.NOTIFICATIONS },
                    onNavigateToVideos = { currentScreen = LingoScreen.VIDEOS },
                    communityProfiles = communityProfiles,
                    onOpenCulturePassport = { tabIdx ->
                      cultureInitialTab = tabIdx
                      activeSubScreen = ActiveSubScreen.CULTURE_PASSPORT
                    },
                    onOpenCultureJourney = {
                      cultureInitialTab = 0
                      activeSubScreen = ActiveSubScreen.CULTURE_PASSPORT
                    },
                    onOpenSpeakingRooms = {
                      activeSubScreen = ActiveSubScreen.SPEAKING_ROOMS
                    },
                    onOpenLanguageExchange = {
                      activeSubScreen = ActiveSubScreen.LANGUAGE_EXCHANGE
                    },
                    onOpenSmartMatching = {
                      activeSubScreen = ActiveSubScreen.SMART_MATCHING
                    },
                    dueFlashcardsCount = dueFlashcardsCount,
                    completedLessonsCount = userProfile.completedLessonIds.size,
                    weeklySessionsCount = speakingSessions.size,
                    cefrStageLabel = activeCefrStageLabel,
                    onOpenPremiumHub = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onOpenImmersionMode = { activeSubScreen = ActiveSubScreen.AI_IMMERSION_MODE },
                    onOpenConversationReplay = { activeSubScreen = ActiveSubScreen.AI_CONVERSATION_REPLAY },
                    onOpenAccentStudio = { activeSubScreen = ActiveSubScreen.ACCENT_UNDERSTANDING_STUDIO },
                    onOpenLanguagePortfolio = { activeSubScreen = ActiveSubScreen.LANGUAGE_PORTFOLIO },
                    onOpenMistakeRecovery = { activeSubScreen = ActiveSubScreen.MISTAKE_RECOVERY_MEMORY },
                    onOpenCameraLens = { activeSubScreen = ActiveSubScreen.AI_CAMERA_LENS },
                    onOpenConversationRepairLab = { activeSubScreen = ActiveSubScreen.CONVERSATION_REPAIR_LAB },
                    onOpenLanguageEmergencyMode = { activeSubScreen = ActiveSubScreen.LANGUAGE_EMERGENCY_MODE },
                    onOpenAiDebatePartner = { activeSubScreen = ActiveSubScreen.AI_DEBATE_PARTNER },
                    onOpenExplainItThreeWays = { activeSubScreen = ActiveSubScreen.EXPLAIN_IT_THREE_WAYS },
                    onOpenConversationDetective = { activeSubScreen = ActiveSubScreen.CONVERSATION_DETECTIVE },
                    onOpenTeachTheAi = { activeSubScreen = ActiveSubScreen.TEACH_THE_AI },
                    onOpenVocabularyBuilder = { activeSubScreen = ActiveSubScreen.VOCABULARY_BUILDER },
                    onOpenListeningPractice = {
                      booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.MAIN
                      activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
                    },
                    onOpenWritingStudio = { activeSubScreen = ActiveSubScreen.WRITING_STUDIO },
                    onOpenGrammarStudio = { activeSubScreen = ActiveSubScreen.GRAMMAR_STUDIO },
                    onOpenRoleplayStudio = { activeSubScreen = ActiveSubScreen.ROLEPLAY_STUDIO },
                    onOpenReadingStudio = {
                      booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.READING_ACADEMY
                      activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
                    },
                    onOpenInviteFriends = {
                      val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Join me on LingoSphere!")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Come practice ${com.example.data.model.Language.findByCode(userProfile.targetLanguageCode).name} with me on LingoSphere AI Language Tutor!")
                      }
                      context.startActivity(android.content.Intent.createChooser(shareIntent, "Invite Friends to LingoSphere"))
                    },
                    onOpenExploreCategoryRoute = { route ->
                      activeSubScreen = when (route) {
                        com.example.ui.screens.ExploreCategoryRoute.COMMUNITY_CULTURE -> ActiveSubScreen.EXPLORE_COMMUNITY_CULTURE
                        com.example.ui.screens.ExploreCategoryRoute.AI_PRACTICE_LABS -> ActiveSubScreen.EXPLORE_AI_LABS
                        com.example.ui.screens.ExploreCategoryRoute.PROGRESS_LANGUAGES -> ActiveSubScreen.EXPLORE_PROGRESS_LANGUAGES
                        com.example.ui.screens.ExploreCategoryRoute.LEARNING_TOOLS -> ActiveSubScreen.EXPLORE_LEARNING_TOOLS
                        com.example.ui.screens.ExploreCategoryRoute.OVERVIEW -> ActiveSubScreen.NONE
                      }
                    },
                    onOpenProfile = { currentScreen = LingoScreen.PROFILE },
                    selectedPartner = selectedAiPartner,
                    speechManager = speechManager,
                    billingRepository = billingRepository,
                    flashcards = flashcards,
                    mistakeNotebookEntries = mistakeNotebookEntries,
                    repeatedMistakes = repeatedMistakes,
                    dailyMinutesMap = dailyMinutesMap,
                    speakingSessions = speakingSessions,
                    lessonProgress = lessonProgress,
                    inProgressLessonsMap = inProgressLessonsMap,
                    onSaveVocabToFlashcard = { front, back, phonetic ->
                      viewModel.saveVocabularyToFlashcards(front, back, phonetic)
                      Toast.makeText(context, "Saved '$front' to Flashcards!", Toast.LENGTH_SHORT).show()
                    },
                    onOpenLanguageSelector = { showChangeLearningLanguageSheet = true },
                    onLanguageAndLevelChanged = { code, cefr ->
                      viewModel.changeTargetLanguageWithLevel(code, cefr)
                    },
                    onApplyPlacementEvaluation = { code, eval ->
                      viewModel.applyPlacementEvaluation(eval, code)
                    }
                  )
                }

                LingoScreen.VIDEOS -> {
                  LingoVideosScreen(
                    userProfile = userProfile,
                    speechManager = speechManager,
                    savedVideos = savedVideos,
                    playlists = videoPlaylists,
                    watchHistory = videoWatchHistory,
                    onSelectLanguageCode = { newCode ->
                      viewModel.changeTargetLanguage(newCode)
                    },
                    onToggleSaveVideo = { video ->
                      viewModel.toggleSaveVideo(video)
                    },
                    onCreatePlaylist = { name, desc, languageCode, initialVideo ->
                      viewModel.createVideoPlaylist(
                        name = name,
                        description = desc,
                        languageCode = languageCode,
                        proficiencyLevel = userProfile.proficiencyLevel,
                        initialVideo = initialVideo
                      )
                      Toast.makeText(context, "Playlist '$name' created!", Toast.LENGTH_SHORT).show()
                    },
                    onAddVideoToPlaylist = { playlist, video ->
                      viewModel.addVideoToPlaylist(playlist, video)
                      Toast.makeText(context, "Added to '${playlist.name}'", Toast.LENGTH_SHORT).show()
                    },
                    onRemoveVideoFromPlaylist = { playlist, videoId ->
                      viewModel.removeVideoFromPlaylist(playlist, videoId)
                    },
                    onDeletePlaylist = { playlistId ->
                      viewModel.deleteVideoPlaylist(playlistId)
                    },
                    onUpdateWatchProgress = { video, watchedSec, completedQuiz, quizScorePercent, xpReward ->
                      viewModel.recordVideoWatchProgress(
                        video = video,
                        watchedSeconds = watchedSec,
                        completedQuiz = completedQuiz,
                        quizScorePercent = quizScorePercent,
                        xpReward = xpReward
                      )
                    },
                    onSaveVocabToFlashcard = { term, translation, phonetic ->
                      viewModel.saveVideoVocabToFlashcard(
                        term = term,
                        translation = translation,
                        phonetic = phonetic,
                        languageCode = userProfile.targetLanguageCode
                      )
                      Toast.makeText(context, "Saved '$term' to Flashcards!", Toast.LENGTH_SHORT).show()
                    }
                  )
                }

                LingoScreen.COMMUNITY -> {
                  CommunityHubScreen(
                    userProfile = userProfile,
                    viewModel = viewModel,
                    speechManager = speechManager,
                    initialTab = communityInitialTab,
                    initialPartnerId = communityInitialPartnerId.orEmpty(),
                    initialOpenPartnerProfile = communityInitialOpenPartnerProfile,
                    onStartLiveCall = { callRoom -> activeUserCallRoom = callRoom },
                    onStartAiVoiceCall = { isVoiceCallActive = true }
                  )
                }

                LingoScreen.SPEAK -> {
                  SpeakScreen(
                    userProfile = userProfile,
                    speechManager = speechManager,
                    onChangeTargetLanguage = { code -> viewModel.changeTargetLanguage(code) },
                    onSaveSession = { session -> viewModel.saveSpeakingSession(session) },
                    onStartVoiceCall = {
                      resumedConversationRecord = null
                      configuredGroupPartners = emptyList()
                      configuredTextMode = false
                      isVoiceCallActive = true
                    },
                    onStartVideoTutorCall = { isVideoTutorCallActive = true },
                    speakingSessions = speakingSessions,
                    onRecordGrammarMistake = { entry -> viewModel.addMistakeNotebookEntry(entry) },
                    onOpenImmersionMode = { activeSubScreen = ActiveSubScreen.AI_IMMERSION_MODE },
                    onOpenConversationReplay = { activeSubScreen = ActiveSubScreen.AI_CONVERSATION_REPLAY },
                    onOpenCameraLens = { activeSubScreen = ActiveSubScreen.AI_CAMERA_LENS },
                    onOpenAccentStudio = { activeSubScreen = ActiveSubScreen.ACCENT_UNDERSTANDING_STUDIO },
                    onOpenLanguageEmergencyMode = { activeSubScreen = ActiveSubScreen.LANGUAGE_EMERGENCY_MODE },
                    onOpenAiDebatePartner = { activeSubScreen = ActiveSubScreen.AI_DEBATE_PARTNER },
                    onOpenExplainItThreeWays = { activeSubScreen = ActiveSubScreen.EXPLAIN_IT_THREE_WAYS },
                    selectedPartner = selectedAiPartner,
                    savedAiConversations = savedAiConversations,
                    onSelectPartner = { partner ->
                      selectedAiPartner = partner
                      configuredVoice = partner.defaultVoice
                      speechManager.setTutorVoice(partner.defaultVoice)
                      callActionScope.launch(Dispatchers.IO) {
                        context.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
                          .edit()
                          .putString("selected_ai_partner_id", partner.id)
                          .putString("selected_tutor_voice_id", partner.defaultVoice.id)
                          .apply()
                      }
                      viewModel.updateAccountSettings(
                        accountSettings.copy(
                          defaultPartnerId = partner.id,
                          preferredVoiceId = partner.defaultVoice.id
                        )
                      )
                    },
                    onStartConfiguredVoiceConversation = { partner, groupPartners, mode, voice, textMode, correctionMode, _, resumedRecord ->
                      selectedAiPartner = partner
                      configuredVoice = voice
                      speechManager.setTutorVoice(voice)
                      configuredCallMode = mode
                      configuredTextMode = textMode
                      configuredGroupPartners = groupPartners
                      resumedConversationRecord = resumedRecord
                      callActionScope.launch(Dispatchers.IO) {
                        context.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
                          .edit()
                          .putString("selected_ai_partner_id", partner.id)
                          .putString("selected_tutor_voice_id", voice.id)
                          .apply()
                      }
                      viewModel.updateAccountSettings(
                        accountSettings.copy(
                          defaultPartnerId = partner.id,
                          preferredVoiceId = voice.id,
                          correctionModeId = correctionMode.id
                        )
                      )
                      isVoiceCallActive = true
                    }
                  )
                }

                LingoScreen.LEARN -> {
                  val todayKey = remember {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                  }
                  val todayMinutes = dailyMinutesMap[todayKey] ?: 0
                  val learningPlanPrefs by viewModel.learningPlanPrefsState.collectAsState()
                  LearnScreen(
                    userProfile = userProfile,
                    lessons = curriculumLessons,
                    onStartLesson = { lesson -> viewModel.startLesson(lesson) },
                    onOpenCulturePassport = { tabIdx ->
                      cultureInitialTab = tabIdx
                      activeSubScreen = ActiveSubScreen.CULTURE_PASSPORT
                    },
                    speechManager = speechManager,
                    bookmarkedReadingIds = bookmarkedReadingIds,
                    completedAcademyIds = completedAcademyIds,
                    placementEvaluation = placementEvaluation,
                    onToggleReadingBookmark = { id -> viewModel.toggleReadingBookmark(id) },
                    onCompleteAcademyItem = { id, title, pct, xp ->
                      viewModel.recordAcademyCompletion(id, title, pct, xp)
                      if (xp > 0) {
                        Toast.makeText(context, "Completed '$title'! +$xp XP", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Reviewed '$title'!", Toast.LENGTH_SHORT).show()
                      }
                    },
                    onSaveVocabToFlashcard = { front, back, phonetic ->
                      viewModel.saveVocabularyToFlashcards(front, back, phonetic)
                      Toast.makeText(context, "Saved '$front' to Flashcards!", Toast.LENGTH_SHORT).show()
                    },
                    onApplyPlacementEvaluation = { eval ->
                      viewModel.applyPlacementEvaluation(eval)
                      Toast.makeText(context, "CEFR Level updated to ${eval.recommendedCefrBand} (${eval.mappedProficiencyLevel})!", Toast.LENGTH_LONG).show()
                    },
                    inProgressLessonsMap = inProgressLessonsMap,
                    downloadedLessonIds = downloadedLessonIds,
                    pendingOfflineSyncCount = pendingOfflineSyncCount,
                    onToggleLessonOfflineDownload = { lesson ->
                      viewModel.toggleLessonOfflineDownload(lesson)
                    },
                    onDownloadAllLessons = { allLessons ->
                      viewModel.downloadAllLessonsForLanguage(allLessons)
                      Toast.makeText(context, "All ${allLessons.size} lessons saved for offline learning!", Toast.LENGTH_SHORT).show()
                    },
                    onSyncOfflineNow = {
                      viewModel.syncOfflineProgressNow()
                      Toast.makeText(context, "Syncing queued offline progress...", Toast.LENGTH_SHORT).show()
                    },
                    adaptivePlan = adaptivePlan,
                    onOpenAccentStudio = { activeSubScreen = ActiveSubScreen.ACCENT_UNDERSTANDING_STUDIO },
                    onOpenMistakeRecovery = { activeSubScreen = ActiveSubScreen.MISTAKE_RECOVERY_MEMORY },
                    onOpenImmersionMode = { activeSubScreen = ActiveSubScreen.AI_IMMERSION_MODE },
                    onOpenConversationRepairLab = { activeSubScreen = ActiveSubScreen.CONVERSATION_REPAIR_LAB },
                    onOpenConversationDetective = { activeSubScreen = ActiveSubScreen.CONVERSATION_DETECTIVE },
                    onOpenTeachTheAi = { activeSubScreen = ActiveSubScreen.TEACH_THE_AI },
                    todayStudyMinutes = todayMinutes,
                    onChangeTargetLanguage = { langCode ->
                      viewModel.changeTargetLanguage(langCode)
                    },
                    onChangeNativeLanguage = { nativeCode ->
                      viewModel.changeNativeLanguage(nativeCode)
                    },
                    onUpdateLearningSetup = { level, goal, dailyMins, confidence ->
                      viewModel.saveLearningPlanPreferences(level, goal, dailyMins, confidence)
                    },
                    currentLearningGoal = learningPlanPrefs.goal,
                    currentConfidence = learningPlanPrefs.confidence,
                    hasCompletedLearningSetup = learningPlanPrefs.completed,
                    onOpenListeningPractice = {
                      booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.MAIN
                      activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
                    },
                    onOpenWritingStudio = { activeSubScreen = ActiveSubScreen.WRITING_STUDIO },
                    onOpenRoleplayStudio = { activeSubScreen = ActiveSubScreen.ROLEPLAY_STUDIO },
                    onOpenReadingStudio = {
                      booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.READING_ACADEMY
                      activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
                    },
                    onOpenVocabularyStudio = { activeSubScreen = ActiveSubScreen.VOCABULARY_BUILDER },
                    onOpenSpeakingCoach = { activeSubScreen = ActiveSubScreen.PRONUNCIATION_COACH },
                    showInlineContinueFab = !floatingContinuePrefs.enabled
                  )
                }

                LingoScreen.PRACTICE -> {
                  PracticeScreen(
                    userProfile = userProfile,
                    flashcards = flashcards,
                    speechManager = speechManager,
                    onReviewCard = { card, isCorrect -> viewModel.reviewFlashcard(card, isCorrect) },
                    communityProfiles = communityProfiles,
                    onCompleteGame = { gameId, title, pct, xp, badgeId ->
                      viewModel.recordGameOrQuizCompletion(gameId, title, pct, xp, badgeId)
                      Toast.makeText(context, "Completed '$title'! +$xp XP", Toast.LENGTH_SHORT).show()
                    },
                    onAddCustomCard = { front, back, phonetic ->
                      viewModel.addCustomFlashcard(front, back, phonetic)
                      Toast.makeText(context, "Saved '$front' to Flashcards!", Toast.LENGTH_SHORT).show()
                    },
                    srsMetadataMap = srsMetadataMap,
                    onReviewCardWithRating = { card, rating ->
                      viewModel.reviewFlashcardWithRating(card, rating)
                    },
                    mistakeNotebookEntries = mistakeNotebookEntries,
                    onAddMistakeEntry = { entry -> viewModel.addMistakeNotebookEntry(entry) },
                    onResolveMistakeEntry = { id -> viewModel.markMistakeNotebookEntryResolved(id) },
                    onOpenMistakeRecovery = { activeSubScreen = ActiveSubScreen.MISTAKE_RECOVERY_MEMORY },
                    onOpenCameraLens = { activeSubScreen = ActiveSubScreen.AI_CAMERA_LENS },
                    onOpenVocabularyBuilder = { activeSubScreen = ActiveSubScreen.VOCABULARY_BUILDER },
                    onOpenPronunciationCoach = { activeSubScreen = ActiveSubScreen.PRONUNCIATION_COACH },
                    onOpenBooksLibrary = {
                      booksListeningInitialSection = com.example.ui.screens.BooksListeningInitialSection.BOOKS
                      activeSubScreen = ActiveSubScreen.LISTENING_PRACTICE
                    }
                  )
                }

                LingoScreen.PROFILE -> {
                  val avgPronunciationScore = remember(pronunciationHistory) {
                    pronunciationHistory
                      .takeIf { it.isNotEmpty() }
                      ?.map { it.accuracyPercent }
                      ?.average()
                      ?.toInt()
                  }
                  ProfileScreen(
                    userProfile = userProfile,
                    myCommunityProfile = myCommunityProfile,
                    blockedUsers = blockedUsers,
                    billingRepository = billingRepository,
                    flashcards = flashcards,
                    speakingSessions = speakingSessions,
                    weeklyReport = weeklyReport,
                    downloadedLessonIds = downloadedLessonIds,
                    totalCurriculumLessonsCount = curriculumLessons.size,
                    pendingOfflineSyncCount = pendingOfflineSyncCount,
                    accountSettings = accountSettings,
                    isSavingProfile = isSavingProfile,
                    profileStatusMessage = profileStatusMessage,
                    userEmail = Firebase.auth.currentUser?.email.orEmpty(),
                    cefrStageLabel = activeCefrStageLabel,
                    pronunciationAverageScore = avgPronunciationScore,
                    onSignOut = onSignOut,
                    onNativeLanguageChanged = { newNative -> viewModel.changeNativeLanguage(newNative) },
                    onTargetLanguageChanged = { newTarget -> viewModel.changeTargetLanguage(newTarget) },
                    onProficiencyChanged = { newProf -> viewModel.updateProficiencyLevel(newProf) },
                    onDailyGoalChanged = { mins -> viewModel.updateDailyGoalMinutes(mins.toLong()) },
                    onUpdateCommunitySettings = { name, country, flag, bio, interests, isOnline, allowCalls ->
                      viewModel.updateCommunitySettings(name, country, flag, bio, interests, isOnline, allowCalls)
                    },
                    onUpdateAccountSettings = { newSettings ->
                      viewModel.updateAccountSettings(newSettings)
                    },
                    onClearProfileStatusMessage = {
                      viewModel.clearProfileStatusMessage()
                    },
                    onUpdateProfileAvatar = { newAvatar ->
                      viewModel.updateProfileAvatar(newAvatar)
                    },
                    onUnblockUser = { blockId -> viewModel.unblockUser(blockId) },
                    onOpenHelpSupport = { activeSubScreen = ActiveSubScreen.HELP_SUPPORT },
                    onOpenNotifications = { activeSubScreen = ActiveSubScreen.NOTIFICATIONS },
                    onOpenProgressDashboard = { activeSubScreen = ActiveSubScreen.PROGRESS_DASHBOARD },
                    onDownloadAllLessons = {
                      viewModel.downloadAllLessonsForLanguage(curriculumLessons)
                      Toast.makeText(context, "Downloaded ${curriculumLessons.size} lessons for offline study!", Toast.LENGTH_SHORT).show()
                    },
                    onSyncOfflineNow = {
                      viewModel.syncOfflineProgressNow { synced ->
                        Toast.makeText(context, "Synced $synced offline item(s) with cloud.", Toast.LENGTH_SHORT).show()
                      }
                    },
                    onClearLearningHistory = {
                      viewModel.clearLearningHistory {
                        Toast.makeText(context, "Learning history & AI memory cleared.", Toast.LENGTH_SHORT).show()
                      }
                    },
                    onOpenPremiumHub = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onOpenUsageLimits = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.USAGE_LIMITS
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onNavigateToCourses = {
                      currentScreen = LingoScreen.LEARN
                    },
                    onNavigateToVocabulary = {
                      currentScreen = LingoScreen.PRACTICE
                    },
                    onOpenAiIntelligentStudio = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.AI_STUDIO_DECKS
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onOpenLearningRoadmap = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.LEARNING_ROADMAP
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onOpenOfflineDownloads = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.OFFLINE_DOWNLOADS
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onOpenAchievementsCertificates = {
                      premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.ACHIEVEMENTS_CERTIFICATES
                      activeSubScreen = ActiveSubScreen.PREMIUM_HUB
                    },
                    onActivateSubscriptionDirect = { tier, token ->
                      viewModel.updateSubscription(tier, token)
                      Toast.makeText(context, "LingoSphere Pro Activated!", Toast.LENGTH_SHORT).show()
                    },
                    onCancelSubscriptionToFree = {
                      viewModel.cancelSubscriptionToFree()
                      Toast.makeText(context, "Subscription switched to Free plan.", Toast.LENGTH_SHORT).show()
                    },
                    onOpenLanguagePortfolio = { activeSubScreen = ActiveSubScreen.LANGUAGE_PORTFOLIO },
                    onOpenMistakeRecovery = { activeSubScreen = ActiveSubScreen.MISTAKE_RECOVERY_MEMORY },
                    onDeleteAccount = {
                      viewModel.deleteAccount(
                        onSuccess = {
                          Toast.makeText(context, "Account and all eligible data deleted.", Toast.LENGTH_LONG).show()
                          onSignOut()
                        },
                        onError = { err ->
                          Toast.makeText(context, "Delete failed: $err", Toast.LENGTH_LONG).show()
                        }
                      )
                    },
                    onOpenLanguageSelector = { showChangeLearningLanguageSheet = true },
                    onLanguageAndLevelChanged = { code, cefr ->
                      viewModel.changeTargetLanguageWithLevel(code, cefr)
                    },
                    onApplyPlacementEvaluation = { code, eval ->
                      viewModel.applyPlacementEvaluation(eval, code)
                    },
                    floatingContinuePrefs = floatingContinuePrefs,
                    onToggleFloatingContinue = { enabled ->
                      viewModel.setFloatingContinueEnabled(enabled)
                    },
                    onResetFloatingContinuePosition = {
                      viewModel.resetFloatingContinuePosition()
                    }
                  )
                }

                else -> {
                  ExploreLanguagesScreen(
                    userProfile = userProfile,
                    communityProfiles = communityProfiles,
                    speechManager = speechManager,
                    onSelectTargetLanguage = { newCode ->
                      viewModel.changeTargetLanguage(newCode)
                      currentScreen = LingoScreen.HOME
                    },
                    onSelectNativeLanguage = { newCode ->
                      viewModel.changeNativeLanguage(newCode)
                    },
                    onBack = { currentScreen = LingoScreen.HOME }
                  )
                }
              }
            }

            val showFloatingContinueOnTab = currentScreen in setOf(
              LingoScreen.HOME,
              LingoScreen.LEARN,
              LingoScreen.SPEAK,
              LingoScreen.COMMUNITY,
              LingoScreen.PROFILE
            )
            if (
              floatingContinuePrefs.enabled &&
              nextLesson != null &&
              showFloatingContinueOnTab &&
              !isKeyboardOpen &&
              activeUpgradePrompt == null &&
              !showChangeLearningLanguageSheet
            ) {
              val isEntitlementLoading by com.example.data.repository.AccessPolicyManager.isEntitlementLoading.collectAsState()
              val isVerifiedPro =
                com.example.data.repository.AccessPolicyManager.isUserVerifiedPro(userProfile) &&
                  !isEntitlementLoading
              val screenBottomAvoidanceDp = when (currentScreen) {
                LingoScreen.SPEAK,
                LingoScreen.COMMUNITY -> 72.dp
                else -> 12.dp
              }
              com.example.ui.components.FloatingContinueLessonButton(
                prefs = floatingContinuePrefs,
                lessonTitle = nextLesson.title,
                topAvoidanceDp = 12.dp,
                bottomAvoidanceDp = screenBottomAvoidanceDp,
                onResumeLesson = {
                  val hasLessonUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
                    com.example.ads.RewardedFeatureType.LEARN_LESSON,
                    nextLesson.id
                  )
                  if (isVerifiedPro || hasLessonUnlock) {
                    viewModel.startLesson(nextLesson)
                  } else {
                    com.example.ai.AiErrorHandler.requestUpgrade(
                      mode = com.example.ai.UpgradeSheetMode.LESSONS,
                      featureKey = com.example.data.repository.AccessPolicyFeatureKeys.LESSONS_DAILY,
                      headline = "Unlock structured lessons",
                      reason = "Get full access to all 500+ guided CEFR lessons, AI coaching with Emma, and Roleplay Studio.",
                      itemId = nextLesson.id,
                      onRewardedUnlockGranted = { viewModel.startLesson(nextLesson) }
                    )
                  }
                },
                onPositionCommitted = { snapSide, verticalFraction ->
                  viewModel.updateFloatingContinuePosition(snapSide, verticalFraction)
                },
                onHintDismissed = {
                  viewModel.markFloatingContinueHintSeen()
                }
              )
            }
            }
          }
        }
      }
    }
      LingoOfflineTopBanner(
        visible = !isOnline,
        modifier = Modifier
          .align(androidx.compose.ui.Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.statusBars)
      )

      com.example.ads.LingoRewardedTrialCountdownChip(
        modifier = Modifier
          .align(androidx.compose.ui.Alignment.TopEnd)
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(top = 12.dp, end = 16.dp)
      )

      if (showChangeLearningLanguageSheet) {
        com.example.ui.components.ChangeLearningLanguageBottomSheet(
          userProfile = userProfile,
          cefrStageLabel = activeCefrStageLabel,
          onLanguageAndLevelSelected = { code, cefr ->
            viewModel.changeTargetLanguageWithLevel(code, cefr)
          },
          onApplyPlacementEvaluation = { code, eval ->
            viewModel.applyPlacementEvaluation(eval, code)
          },
          onDismiss = { showChangeLearningLanguageSheet = false }
        )
      }

      activeUpgradePrompt?.let { upgradeReq ->
        val signedInUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }
        val effectivelySignedIn = signedInUser != null || userProfile.userId.isNotBlank()
        LingoGlobalUpgradeSheet(
          request = upgradeReq,
          isSignedIn = effectivelySignedIn,
          userProfile = userProfile,
          billingRepository = billingRepository,
          onDismiss = { activeUpgradePrompt = null },
          onSelectPlan = { productId ->
            billingRepository?.launchBillingFlow(
              activity = context as? android.app.Activity,
              productId = productId,
              isFreeTrial = false
            )
            activeUpgradePrompt = null
          },
          onOpenFullComparison = {
            activeUpgradePrompt = null
            premiumHubInitialSection = com.example.ui.screens.PremiumHubSection.SUBSCRIPTION
            activeSubScreen = ActiveSubScreen.PREMIUM_HUB
          },
          onSignInRequested = {
            activeUpgradePrompt = null
            onSignOut()
          }
        )
      }

      if (activeSubScreen != ActiveSubScreen.PREMIUM_HUB) {
        com.example.ads.LingoFullScreenAdOverlayHost(
          userProfile = userProfile
        )
      }
    }
  }
}
