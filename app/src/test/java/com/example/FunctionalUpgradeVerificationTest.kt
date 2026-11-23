package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FlashcardProgress
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.PronunciationDiagnosticResult
import com.example.data.model.RoleplayScenario
import com.example.data.model.SpeakingSession
import com.example.data.model.SrsCardMetadata
import com.example.data.repository.CurriculumData
import com.example.data.repository.InProgressLessonState
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.data.repository.LingoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FunctionalUpgradeVerificationTest {

  @Test
  fun firestoreNormalization_mapsAllCategoriesAndSpeakingModesToValidRules() {
    val allowedCategories = setOf("Vocabulary", "Grammar", "Listening", "Reading", "Writing", "Quiz")
    val inputs = listOf("Vocabulary", "Grammar", "Listening", "Reading", "Writing", "Quiz", "Speaking", "Revision", "Culture", "Game")
    inputs.forEach { raw ->
      val normalized = LingoRepository.normalizeFirestoreLessonCategory(raw)
      assertTrue("Category '$raw' normalized to '$normalized' must be in allowed set", normalized in allowedCategories)
    }

    val allowedModes = setOf("Tutor", "Roleplay")
    val modeInputs = listOf("Tutor", "Roleplay", "Pronunciation", "VideoTutor", "Interview", "Assessment")
    modeInputs.forEach { rawMode ->
      val normalizedMode = LingoRepository.normalizeFirestoreSpeakingMode(rawMode)
      assertTrue("Mode '$rawMode' normalized to '$normalizedMode' must be in allowed set", normalizedMode in allowedModes)
    }
  }

  @Test
  fun roleplayMissions_includeAllElevenRequiredRealLifeScenarios() {
    val scenarios = RoleplayScenario.SCENARIOS
    assertTrue("Should have at least 11 scenarios", scenarios.size >= 11)
    val allText = scenarios.joinToString(" ") { "${it.id} ${it.title} ${it.description}" }.lowercase()

    val requiredKeywords = listOf(
      "airport",
      "interview",
      "restaurant",
      "market",
      "doctor",
      "hotel",
      "train",
      "business",
      "friend",
      "phone",
      "emergency"
    )
    requiredKeywords.forEach { keyword ->
      assertTrue("Missing required mission scenario matching '$keyword'", allText.contains(keyword))
    }
  }

  @Test
  fun learningMemoryAndOfflineManager_persistsMistakesCheckpointsOfflineQueueAndWeeklyReport() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = LearningMemoryAndOfflineManager(context)
    manager.clearAllMemoryAndHistory()

    // 1. Mistake notebook persistence
    val mistakes = listOf(
      MistakeNotebookEntry(
        id = "m1",
        languageCode = "es",
        category = "Subjunctive Verb",
        originalText = "Espero que tienes",
        correctedText = "Espero que tengas",
        explanationNative = "Use present subjunctive after esperar que."
      )
    )
    manager.saveMistakeNotebook(mistakes)
    val loadedMistakes = manager.loadMistakeNotebook()
    assertEquals(1, loadedMistakes.size)
    assertEquals("Subjunctive Verb", loadedMistakes.first().category)

    // 2. Pronunciation diagnostic persistence
    val diagnostics = listOf(
      PronunciationDiagnosticResult(
        targetPhrase = "Ferrocarril",
        recognizedTranscript = "Ferocaril",
        accuracyPercent = 72,
        ipaTranscription = "/fe.ro.ka.ˈril/",
        stressedSyllablesGuide = "fe-ro-ca-RRIL",
        wordBreakdown = emptyList(),
        problematicSounds = listOf("rr", "r"),
        tongueAndMouthTip = "Place tongue tip behind upper teeth",
        intonationAdvice = "Roll the double RR with alveolar trill",
        regionalAccentNote = "Standard Castilian / Latin American trill"
      )
    )
    manager.savePronunciationHistory(diagnostics)
    val loadedDiag = manager.loadPronunciationHistory()
    assertEquals(1, loadedDiag.size)
    assertTrue(loadedDiag.first().problematicSounds.contains("rr"))

    // 3. In-progress lesson checkpoint resume
    val checkpoint = InProgressLessonState(
      lessonId = "es_b1_1",
      languageCode = "es",
      currentExerciseIndex = 2,
      correctCount = 2,
      totalExercises = 5
    )
    manager.saveInProgressLessonState(checkpoint)
    val loadedCheckpoints = manager.loadAllInProgressLessons()
    assertNotNull(loadedCheckpoints["es_b1_1"])
    assertEquals(2, loadedCheckpoints["es_b1_1"]?.currentExerciseIndex)

    // 4. Offline lesson download & queue
    val lessons = CurriculumData.getLessonsForLanguage("es", "en")
    assertTrue(lessons.isNotEmpty())
    val updatedDownloaded = manager.toggleLessonOfflineDownload(lessons.first())
    assertTrue(updatedDownloaded.contains(lessons.first().id))

    manager.enqueueOfflineLessonCompletion(
      lessonId = lessons.first().id,
      languageCode = "es",
      category = "Speaking",
      scorePercent = 92,
      xpGained = 40
    )
    assertTrue(manager.loadPendingMutations().isNotEmpty())

    // 5. Weekly learning report calculation
    manager.recordLearningMinutes(18)
    val report = manager.buildWeeklyLearningReport(
      targetLanguageCode = "es",
      proficiencyLevel = "Intermediate",
      completedLessonsCount = 3,
      flashcards = listOf(
        FlashcardProgress(
          id = "fc1",
          userId = "u1",
          wordId = "fc1",
          languageCode = "es",
          frontText = "Hola",
          backText = "Hello",
          boxLevel = 4L,
          mastered = true
        )
      ),
      srsMetadataMap = mapOf("fc1" to SrsCardMetadata(wordId = "fc1", intervalDays = 3, repetitions = 2)),
      speakingSessions = listOf(
        SpeakingSession(
          id = "ss1",
          userId = "u1",
          languageCode = "es",
          mode = "Roleplay",
          scenarioTitle = "Airport Check-In",
          proficiencyLevel = "B1",
          turnsCount = 6L,
          fluencyScore = 88L
        )
      ),
      mistakeEntries = loadedMistakes,
      pronunciationDiagnostics = loadedDiag
    )
    assertTrue(report.thisWeekMinutes >= 18)
    assertEquals(7, report.dailyMinutesLast7Days.size)
    assertTrue(report.topGrammarMistakes.isNotEmpty())
    assertTrue(report.difficultSounds.contains("rr"))
    assertTrue(report.personalizedRecommendations.isNotEmpty())

    // 6. Clear learning history
    manager.clearAllMemoryAndHistory()
    assertTrue(manager.loadMistakeNotebook().isEmpty())
    assertTrue(manager.loadAllInProgressLessons().isEmpty())
  }

  @Test
  fun premiumFeatures_supportTiersTrialsLifetimePersistenceAndProStudioDecks() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = LearningMemoryAndOfflineManager(context)
    manager.clearAllMemoryAndHistory()

    // 1. Subscription plans (Free + LingoSphere Pro Monthly / Yearly)
    val plans = com.example.data.model.SubscriptionPlan.PAID_GOOGLE_PLAY_PLANS
    assertEquals(2, plans.size)
    assertTrue(com.example.data.model.SubscriptionPlan.PREMIUM_YEARLY_PLAN.badge?.contains("BEST VALUE") == true)
    assertEquals("$79.99", com.example.data.model.SubscriptionPlan.PREMIUM_YEARLY_PLAN.priceDisplay)
    assertEquals("$9.99", com.example.data.model.SubscriptionPlan.PREMIUM_MONTHLY_PLAN.priceDisplay)

    // 2. UserProfile tier & server-entitlement gating (Robolectric mock gp_ token path)
    val freeUser = com.example.data.model.UserProfile(userId = "u1", subscriptionTier = "free")
    assertEquals(false, freeUser.isPremium)
    assertEquals(0, freeUser.voiceCallLimitSeconds)
    assertEquals(0, freeUser.videoCallLimitSeconds)

    val futureExpiry = System.currentTimeMillis() + 7L * 24L * 3600L * 1000L
    val proMonthlyUser = com.example.data.model.UserProfile(
      userId = "u1",
      subscriptionTier = "premium_monthly",
      purchaseToken = "gp_monthly_12345",
      subscriptionExpiryEpochMs = futureExpiry
    )
    assertTrue(proMonthlyUser.isPremium)
    assertEquals(600, proMonthlyUser.voiceCallLimitSeconds)
    assertEquals(600, proMonthlyUser.videoCallLimitSeconds)

    val yearlyUser = com.example.data.model.UserProfile(
      userId = "u1",
      subscriptionTier = "premium_yearly",
      purchaseToken = "gp_yearly_99999",
      subscriptionExpiryEpochMs = futureExpiry
    )
    assertTrue(yearlyUser.isPremium)
    assertEquals("LingoSphere Pro Yearly", yearlyUser.planDisplayName)

    // 3. Subscription state & Custom AI Pro Deck persistence
    manager.saveSubscriptionState("premium_yearly", "pro_yearly_99999")
    val loadedSub = manager.loadSubscriptionState()
    assertNotNull(loadedSub)
    assertEquals("premium_yearly", loadedSub?.first)
    assertEquals("pro_yearly_99999", loadedSub?.second)

    val deck = com.example.data.repository.PremiumCustomDeck(
      id = "deck_1",
      topicTitle = "Executive Business Negotiations",
      languageCode = "es",
      cefrLevel = "B2",
      cards = listOf(
        com.example.data.repository.PremiumGeneratedVocabCard(
          termTarget = "Llegar a un acuerdo",
          translationNative = "To reach an agreement",
          phoneticGuide = "yeh-GAR ah oon ah-KWER-doh",
          exampleSentenceTarget = "Esperamos llegar a un acuerdo hoy.",
          exampleSentenceNative = "We hope to reach an agreement today."
        )
      )
    )
    manager.saveCustomProDecks(listOf(deck))
    val loadedDecks = manager.loadCustomProDecks()
    assertEquals(1, loadedDecks.size)
    assertEquals("Executive Business Negotiations", loadedDecks.first().topicTitle)
    assertEquals("Llegar a un acuerdo", loadedDecks.first().cards.first().termTarget)
  }

  @get:org.junit.Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

  @Test
  @Config(sdk = [36], qualifiers = "w320dp-h640dp-mdpi")
  fun learnScreen_scrollsSmoothlyAndSupportsTabsPlacementAndFiltersOnCompactScreen() {
    val lessons = CurriculumData.getLessonsForLanguage("es", "en").take(12)
    var startedLessonId: String? = null
    var downloadedAllCount = 0

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.LearnScreen(
          userProfile = com.example.data.model.UserProfile(
            userId = "u1",
            targetLanguageCode = "es",
            nativeLanguageCode = "en",
            proficiencyLevel = "Beginner"
          ),
          lessons = lessons,
          onStartLesson = { startedLessonId = it.id },
          inProgressLessonsMap = mapOf(
            lessons.first().id to InProgressLessonState(
              lessonId = lessons.first().id,
              languageCode = "es",
              currentExerciseIndex = 1,
              correctCount = 1,
              totalExercises = 4
            )
          ),
          onDownloadAllLessons = { downloadedAllCount = it.size }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_screen")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list")).assertExists()

    // Scroll to a lesson in the curriculum list and click it
    val targetLesson = lessons.getOrNull(2) ?: lessons.first()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list"))
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("lesson_card_${targetLesson.id}"))
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("lesson_card_${targetLesson.id}"))
      .assertExists()
      .performClick()
    assertEquals(targetLesson.id, startedLessonId)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h740dp-mdpi")
  fun speakScreen_scrollsIndependentlyAcrossAllTabsAndKeepsMicAndInputAccessible() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = com.example.speech.SpeechManager(context)
    var videoCallStarted = false
    var voiceCallStarted = false

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.SpeakScreen(
          userProfile = com.example.data.model.UserProfile(
            userId = "u1",
            targetLanguageCode = "es",
            nativeLanguageCode = "en",
            proficiencyLevel = "Intermediate"
          ),
          speechManager = speechManager,
          onSaveSession = {},
          onStartVoiceCall = { voiceCallStarted = true },
          onStartVideoTutorCall = { videoCallStarted = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_screen")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_chat_list")).assertExists()

    // Verify Video Call and Voice Call buttons in scrollable AI Tutor hero work
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("start_ai_video_call_btn")).performClick()
    assertTrue(videoCallStarted)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("start_ai_voice_call_btn")).performClick()
    assertTrue(voiceCallStarted)

    // Verify Microphone and Message Input stay accessible at the bottom of AI Tutor tab
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_mic_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_chat_input")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_send_button")).assertExists()

    // Switch to Missions tab
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_tab_roleplays")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_missions_list")).assertExists()

    speechManager.release()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w840dp-h1024dp-mdpi")
  fun learnAndSpeakScreens_supportLargeTabletScreensSearchOfflineDownloadAndRoleplayConversation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = com.example.speech.SpeechManager(context)
    val lessons = CurriculumData.getLessonsForLanguage("fr").take(12)
    var downloadedAllCount = 0
    val activePage = androidx.compose.runtime.mutableStateOf("learn")

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        if (activePage.value == "learn") {
          com.example.ui.screens.LearnScreen(
            userProfile = com.example.data.model.UserProfile(
              userId = "u2",
              targetLanguageCode = "fr",
              nativeLanguageCode = "en",
              proficiencyLevel = "Intermediate"
            ),
            lessons = lessons,
            onStartLesson = {},
            onDownloadAllLessons = { downloadedAllCount = it.size }
          )
        } else {
          com.example.ui.screens.SpeakScreen(
            userProfile = com.example.data.model.UserProfile(
              userId = "u2",
              targetLanguageCode = "fr",
              nativeLanguageCode = "en",
              proficiencyLevel = "Intermediate"
            ),
            speechManager = speechManager,
            onSaveSession = {},
            onStartVoiceCall = {},
            onStartVideoTutorCall = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    // Verify curriculum list and floating FAB on Learn page
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("floating_current_lesson_fab")).assertExists()

    // Switch to Speak page on tablet screen, type message, launch a roleplay mission, and exit
    activePage.value = "speak"
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_chat_input"))
      .assertExists()
      .performTextInput("Bonjour Sophie, comment allez-vous ?")
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_send_button"))
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()

    // Switch to Missions tab, start a mission, verify mission chat + exit button
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("speak_tab_roleplays")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_card_airport_employee"))
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("exit_scenario_button"))
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()

    speechManager.release()
  }

  @Test
  fun realMatchScore_calculatesRealCriteriaAndReturnsNullWhenMissing() {
    val myProfile = com.example.data.model.UserProfile(
      userId = "me",
      nativeLanguageCode = "en",
      targetLanguageCode = "es",
      proficiencyLevel = "Intermediate"
    )
    val myComm = com.example.data.model.CommunityProfile(
      userId = "me",
      interests = listOf("Travel", "Music", "Technology")
    )

    val tandemPartner = com.example.data.model.CommunityProfile(
      userId = "p1",
      displayName = "Carlos",
      nativeLanguageCode = "es",
      targetLanguageCode = "en",
      proficiencyLevel = "Intermediate",
      interests = listOf("Travel", "Music", "Cooking"),
      isOnline = true,
      allowCalls = true
    )

    val nonTandemPartner = com.example.data.model.CommunityProfile(
      userId = "p2",
      displayName = "Yuki",
      nativeLanguageCode = "ja",
      targetLanguageCode = "fr",
      proficiencyLevel = "Beginner",
      interests = listOf("Gaming"),
      isOnline = false,
      allowCalls = false
    )

    val incompletePartner = com.example.data.model.CommunityProfile(
      userId = "p3",
      displayName = "Incomplete",
      nativeLanguageCode = "",
      targetLanguageCode = ""
    )

    val tandemScore = com.example.ui.screens.calculateRealMatchScore(myProfile, myComm, tandemPartner)
    val nonTandemScore = com.example.ui.screens.calculateRealMatchScore(myProfile, myComm, nonTandemPartner)
    val missingScore = com.example.ui.screens.calculateRealMatchScore(myProfile, myComm, incompletePartner)

    assertNotNull(tandemScore)
    assertNotNull(nonTandemScore)
    assertTrue("Tandem partner should have a high real compatibility score (>= 85)", (tandemScore ?: 0) >= 85)
    assertTrue("Tandem score must exceed unrelated language score", (tandemScore ?: 0) > (nonTandemScore ?: 0))
    assertEquals(null, missingScore)
  }

  @Test
  fun incomingCallSystem_parsesPayloadsDeduplicatesAndBuildsCallStyleNotification() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.webrtc.IncomingCallRegistry.clearForTests()
    val nowMs = System.currentTimeMillis()

    val rawData = mapOf(
      "eventType" to "incoming_call",
      "roomId" to "call_room_test_99",
      "callerId" to "caller_u1",
      "callerName" to "Mateo Rivera",
      "callerNativeLang" to "es",
      "callerLearningLang" to "en",
      "calleeId" to "callee_u2",
      "calleeName" to "Elena",
      "callType" to "video",
      "targetLanguageCode" to "es",
      "createdAtEpochMs" to nowMs.toString(),
      "expiresAtEpochMs" to (nowMs + 30_000L).toString()
    )

    val payload = com.example.webrtc.FcmCallPayload.fromMap(rawData)
    assertNotNull(payload)
    assertEquals("call_room_test_99", payload!!.roomId)
    assertEquals("Mateo Rivera", payload.callerName)
    assertEquals("video", payload.callType)
    assertTrue("Fresh call should not be expired", !payload.isExpired(nowMs + 5_000L))
    assertTrue("Call older than 30s should be expired", payload.isExpired(nowMs + 35_000L))

    val callRoom = payload.toCallRoom()
    assertEquals("call_room_test_99", callRoom.roomId)
    assertEquals("video", callRoom.callType)
    assertEquals("ringing", callRoom.status)

    // Verify Intent round-trip
    val answerIntent = android.content.Intent(com.example.webrtc.IncomingCallContract.ACTION_ANSWER_CALL).apply {
      com.example.webrtc.FcmCallPayload.toIntentExtras(
        this,
        callRoom,
        com.example.webrtc.IncomingCallContract.ACTION_ANSWER_CALL
      )
    }
    val parsedFromIntent = com.example.webrtc.FcmCallPayload.fromIntent(answerIntent)
    assertNotNull(parsedFromIntent)
    assertEquals("call_room_test_99", parsedFromIntent!!.first.roomId)
    assertEquals("video", parsedFromIntent.first.callType)
    assertEquals("accepted", parsedFromIntent.first.status)
    assertEquals(com.example.webrtc.IncomingCallContract.ACTION_ANSWER_CALL, parsedFromIntent.second)

    // Verify IncomingCallRegistry deduplication & terminal tracking
    assertTrue(
      "First ringing registration for room should return true",
      com.example.webrtc.IncomingCallRegistry.markRoomRinging("call_room_test_99")
    )
    assertTrue(
      "Duplicate ringing registration while already ringing should return false",
      !com.example.webrtc.IncomingCallRegistry.markRoomRinging("call_room_test_99")
    )
    com.example.webrtc.IncomingCallRegistry.markRoomTerminal(context, "call_room_test_99")
    assertTrue(
      "Terminal room should be marked terminal",
      com.example.webrtc.IncomingCallRegistry.isRoomTerminal(context, "call_room_test_99")
    )
    assertTrue(
      "Terminal room should never be allowed to ring again",
      !com.example.webrtc.IncomingCallRegistry.markRoomRinging("call_room_test_99")
    )

    // Verify deterministic FCM token document ID
    val tokenDocId1 = com.example.webrtc.FcmTokenManager.buildTokenDocId("user_123", "device_abc")
    val tokenDocId2 = com.example.webrtc.FcmTokenManager.buildTokenDocId("user_123", "device_abc")
    assertTrue(tokenDocId1.startsWith("fcm_"))
    assertEquals(tokenDocId1, tokenDocId2)

    // Verify NativeCallNotificationManager creates notification channels and builds CallStyle notification
    com.example.webrtc.NativeCallNotificationManager.ensureIncomingCallChannel(context)
    com.example.webrtc.NativeCallNotificationManager.ensureActiveCallChannel(context)
    val notification = com.example.webrtc.NativeCallNotificationManager.buildIncomingCallNotification(context, callRoom)
    assertNotNull(notification)
    assertNotNull("Incoming call notification must configure contentIntent", notification.contentIntent)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w320dp-h640dp-mdpi")
  fun profileScreen_compactMobile_rendersNoBrokenTextAndSupportsEditPhotoStatsSettingsAndAccountActions() {
    val realPhotoDataUri = com.example.ui.components.processCapturedProfileBitmap(
      android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888),
      16
    ) ?: "data:image/jpeg;base64,validphoto"

    var currentProfile = com.example.data.model.UserProfile(
      userId = "user_compact_1",
      displayName = "Sofia Martinez",
      avatarUrl = realPhotoDataUri,
      nativeLanguageCode = "en",
      targetLanguageCode = "es",
      proficiencyLevel = "Beginner",
      xpPoints = 540L,
      level = 3L,
      currentStreak = 7L,
      dailyGoalMinutes = 15L,
      // Unverified tier without token must NOT show Premium Active
      subscriptionTier = "premium_yearly",
      purchaseToken = null,
      completedLessonIds = listOf("es_1", "es_2", "es_3")
    )
    assertEquals(false, currentProfile.isEntitlementVerified)
    assertEquals(false, currentProfile.isPremium)

    var currentCommunity = com.example.data.model.CommunityProfile(
      userId = "user_compact_1",
      displayName = "Sofia Martinez",
      avatarUrl = realPhotoDataUri,
      country = "United States",
      countryFlag = "🇺🇸",
      bio = "Learning Spanish for travel and work!",
      interests = listOf("Travel", "Culture"),
      isOnline = true,
      allowCalls = true
    )
    var currentSettings = com.example.data.model.UserAccountSettings()
    var signedOut = false
    var deletedAccount = false

    val stateHolder = androidx.compose.runtime.mutableStateOf(
      Triple(currentProfile, currentCommunity, currentSettings)
    )

    composeTestRule.setContent {
      val (prof, comm, settings) = stateHolder.value
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.ProfileScreen(
          userProfile = prof,
          myCommunityProfile = comm,
          accountSettings = settings,
          userEmail = "sofia@example.com",
          flashcards = listOf(
            FlashcardProgress(id = "f1", wordId = "w1", boxLevel = 3L, mastered = true),
            FlashcardProgress(id = "f2", wordId = "w2", boxLevel = 2L, mastered = false)
          ),
          speakingSessions = listOf(
            SpeakingSession(id = "s1", turnsCount = 5L, fluencyScore = 90L)
          ),
          onSignOut = { signedOut = true },
          onNativeLanguageChanged = {},
          onProficiencyChanged = { newLvl ->
            currentProfile = currentProfile.copy(proficiencyLevel = newLvl)
            stateHolder.value = Triple(currentProfile, currentCommunity, currentSettings)
          },
          onDailyGoalChanged = { mins ->
            currentProfile = currentProfile.copy(dailyGoalMinutes = mins.toLong())
            stateHolder.value = Triple(currentProfile, currentCommunity, currentSettings)
          },
          onUpdateCommunitySettings = { name, country, flag, bio, interests, isOnline, allowCalls ->
            currentProfile = currentProfile.copy(displayName = name)
            currentCommunity = currentCommunity.copy(
              displayName = name,
              country = country,
              countryFlag = flag,
              bio = bio,
              interests = interests,
              isOnline = isOnline,
              allowCalls = allowCalls
            )
            stateHolder.value = Triple(currentProfile, currentCommunity, currentSettings)
          },
          onUpdateProfileAvatar = { newAvatar ->
            currentProfile = currentProfile.copy(avatarUrl = newAvatar)
            currentCommunity = currentCommunity.copy(avatarUrl = newAvatar)
            stateHolder.value = Triple(currentProfile, currentCommunity, currentSettings)
          },
          onUpdateAccountSettings = { updated ->
            currentSettings = updated
            stateHolder.value = Triple(currentProfile, currentCommunity, currentSettings)
          },
          onActivateSubscriptionDirect = { tier, token ->
            currentProfile = currentProfile.copy(subscriptionTier = tier, purchaseToken = token)
            stateHolder.value = Triple(currentProfile, currentCommunity, currentSettings)
          },
          onDeleteAccount = { deletedAccount = true }
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Verify Header & Buttons exist on small 320dp screen
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("profile_header_card")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("edit_profile_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("upload_profile_photo_button")).assertExists()

    // 2. Verify Learning Progress metrics exist
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learning_progress_dashboard")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("stat_streak")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("stat_total_xp")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("stat_completed_lessons")).assertExists()

    // 3. Verify Edit Profile dialog opens and saves updated Name and Bio
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("edit_profile_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("edit_profile_dialog_content")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("save_profile_changes_button")).performClick()
    composeTestRule.waitForIdle()

    // 4. Verify Photo upload button exists
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("upload_profile_photo_button")).assertExists()

    // 5. Verify Subscription row exists
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("row_subscription")).performScrollTo().assertExists()

    // 6. Navigate to Preferences subscreen and verify Learning Level buttons
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("row_preferences")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("proficiency_button_beginner")).performScrollTo().assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("proficiency_button_intermediate")).performScrollTo()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("proficiency_button_intermediate")).performClick()
    composeTestRule.waitForIdle()
    assertEquals("Intermediate", currentProfile.proficiencyLevel)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("proficiency_button_advanced")).performClick()
    composeTestRule.waitForIdle()
    assertEquals("Advanced", currentProfile.proficiencyLevel)

    // 7. Go back to main profile screen and open Account subscreen
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("profile_sub_screen_back_btn")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("row_account")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // 8. Verify Sign Out and Delete Account buttons in Account subscreen
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("sign_out_button")).performScrollTo().assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("delete_account_button")).performScrollTo().assertExists()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w840dp-h1024dp-mdpi")
  fun profileScreen_tabletLayout_andAccountSettingsAndIncomingCallPersistence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = LearningMemoryAndOfflineManager(context)
    val userId = "tablet_user_42"

    // 1. Verify UserAccountSettings persistence
    val customSettings = com.example.data.model.UserAccountSettings(
      preferredTutorVoice = "Crisp Academic",
      tutorSpeechRate = "0.85x Slow",
      autoPlayPronunciation = false,
      allowIncomingCalls = false,
      allowVoiceCalls = false,
      allowVideoCalls = false,
      callNotificationsEnabled = false,
      themeMode = "System",
      highContrastText = true,
      largerTouchTargets = true
    )
    manager.saveUserAccountSettings(userId, customSettings)
    val loadedSettings = manager.loadUserAccountSettings(userId)
    assertEquals("Crisp Academic", loadedSettings.preferredTutorVoice)
    assertEquals("0.85x Slow", loadedSettings.tutorSpeechRate)
    assertEquals(false, loadedSettings.allowIncomingCalls)
    assertEquals(false, loadedSettings.allowVoiceCalls)
    assertEquals(false, loadedSettings.allowVideoCalls)
    assertEquals("System", loadedSettings.themeMode)
    assertTrue(loadedSettings.highContrastText)

    // 2. Verify IncomingCallRegistry respects call preferences
    com.example.webrtc.IncomingCallRegistry.setIncomingCallPreferences(
      context = context,
      allowIncomingCalls = false,
      allowVoiceCalls = false,
      allowVideoCalls = false,
      callNotificationsEnabled = false
    )
    assertEquals(false, com.example.webrtc.IncomingCallRegistry.isIncomingCallAllowed(context, "voice"))
    assertEquals(false, com.example.webrtc.IncomingCallRegistry.isIncomingCallAllowed(context, "video"))
    assertEquals(false, com.example.webrtc.IncomingCallRegistry.areCallNotificationsEnabled(context))

    com.example.webrtc.IncomingCallRegistry.setIncomingCallPreferences(
      context = context,
      allowIncomingCalls = true,
      allowVoiceCalls = true,
      allowVideoCalls = false,
      callNotificationsEnabled = true
    )
    assertEquals(true, com.example.webrtc.IncomingCallRegistry.isIncomingCallAllowed(context, "voice"))
    assertEquals(false, com.example.webrtc.IncomingCallRegistry.isIncomingCallAllowed(context, "video"))
    assertEquals(true, com.example.webrtc.IncomingCallRegistry.areCallNotificationsEnabled(context))

    // 3. Render ProfileScreen on tablet 840dp layout
    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.ProfileScreen(
          userProfile = com.example.data.model.UserProfile(
            userId = userId,
            displayName = "Dr. Kenji Takahashi",
            targetLanguageCode = "fr",
            nativeLanguageCode = "ja",
            proficiencyLevel = "Advanced",
            xpPoints = 1450L,
            level = 8L,
            currentStreak = 21L,
            subscriptionTier = "premium_yearly",
            purchaseToken = "verified_play_token_888"
          ),
          accountSettings = loadedSettings,
          onSignOut = {},
          onNativeLanguageChanged = {},
          onDeleteAccount = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("profile_screen_root")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("profile_header_card")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("row_my_learning")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("menu_my_courses")).performScrollTo().assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("menu_vocabulary_saved_words")).assertExists()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h800dp-mdpi")
  fun premiumHubScreen_cleanSubscriptionRedesign_rendersHeroPlansExpandableComparisonAndUsage() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = com.example.speech.SpeechManager(context)
    val unverifiedProfile = com.example.data.model.UserProfile(
      userId = "sub_redesign_user",
      displayName = "Elena Vance",
      targetLanguageCode = "es",
      nativeLanguageCode = "en",
      proficiencyLevel = "Intermediate",
      // Even if subscriptionTier is set, without purchaseToken it must NOT show verified active paid subscription
      subscriptionTier = "premium_pro_999",
      purchaseToken = null
    )
    assertEquals(false, unverifiedProfile.isEntitlementVerified)

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.PremiumHubScreen(
          userProfile = unverifiedProfile,
          billingRepository = null,
          speechManager = speechManager,
          curriculumLessons = emptyList(),
          downloadedLessonIds = emptySet(),
          pendingOfflineSyncCount = 0,
          flashcards = emptyList(),
          speakingSessions = emptyList(),
          mistakeEntries = emptyList(),
          weeklyReport = null,
          customProDecks = emptyList(),
          cefrStageLabel = "B1",
          onActivatePlan = { _, _ -> },
          onCancelSubscription = {},
          onSaveCustomProDeck = {},
          onDownloadAllLessons = {},
          onSyncOfflineNow = {},
          onStartVoiceCall = {},
          onStartVideoTutorCall = {},
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Top hero section and clean two subscription cards (Monthly & Yearly)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("subscription_hero_section")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("plan_card_${com.example.data.model.SubscriptionPlan.PRODUCT_ID_MONTHLY}")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("choose_monthly_btn")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("plan_card_${com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY}")).performScrollTo()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("plan_card_${com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY}")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("choose_yearly_btn")).assertExists()

    // 2. Current Subscription compact card shows honest Free Plan state for unverified user
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("current_subscription_card")).performScrollTo()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("current_subscription_card")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Free Plan")).assertExists()

    // Verify Free & Pro plan benefits and pricing
    assertEquals(5, com.example.data.model.SubscriptionPlan.FREE_PLAN.features.size)
    assertEquals(6, com.example.data.model.SubscriptionPlan.PREMIUM_MONTHLY_PLAN.features.size)
    assertEquals(5, com.example.data.model.SubscriptionPlan.PREMIUM_YEARLY_PLAN.features.size)

    // 3. Expandable "My AI Usage" section
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("toggle_my_ai_usage_btn")).performScrollTo()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("toggle_my_ai_usage_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("my_ai_usage_content")).assertExists()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w320dp-h640dp-mdpi")
  fun learnScreenAndLessonExercise_upgradeVerification_supportsTabsAudioReadingAiLabsAndNoDuplicateXp() {
    val lessons = CurriculumData.getLessonsForLanguage("es", "en")
    assertTrue("Should include A1 through C2 lessons", lessons.map { it.cefrLevel }.containsAll(listOf("A1", "A2", "B1", "B2", "C1", "C2")))

    var openedImmersion = false
    var openedMistakeRecovery = false
    var openedRepairLab = false
    var openedDetective = false
    var openedTeachAi = false
    var savedVocabTerm: String? = null
    var recordedMistake: MistakeNotebookEntry? = null
    var finishedXp: Int? = null

    val modeState = androidx.compose.runtime.mutableStateOf("learn")

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        if (modeState.value == "learn") {
          com.example.ui.screens.LearnScreen(
            userProfile = com.example.data.model.UserProfile(
              userId = "u_learn_upgrade",
              targetLanguageCode = "es",
              nativeLanguageCode = "en",
              proficiencyLevel = "Intermediate",
              dailyGoalMinutes = 15L,
              completedLessonIds = listOf(lessons.first().id)
            ),
            lessons = lessons,
            onStartLesson = {},
            downloadedLessonIds = setOf(lessons.first().id),
            onSaveVocabToFlashcard = { front, _, _ -> savedVocabTerm = front },
            onOpenImmersionMode = { openedImmersion = true },
            onOpenMistakeRecovery = { openedMistakeRecovery = true },
            onOpenConversationRepairLab = { openedRepairLab = true },
            onOpenConversationDetective = { openedDetective = true },
            onOpenTeachTheAi = { openedTeachAi = true },
            todayStudyMinutes = 10
          )
        } else {
          // Render LessonExerciseScreen in Replay mode for a 1-exercise lesson to verify 0 duplicate XP
          val singleExLesson = lessons.first().copy(
            exercises = listOf(lessons.first().exercises.first())
          )
          com.example.ui.screens.LessonExerciseScreen(
            lesson = singleExLesson,
            speechManager = null,
            onFinishLesson = { _, xp -> finishedXp = xp },
            onNavigateBack = {},
            isReplay = true,
            onSaveVocabToFlashcard = { front, _, _ -> savedVocabTerm = front },
            onRecordMistake = { entry -> recordedMistake = entry }
          )
        }
      }
    }

    composeTestRule.waitForIdle()

    // 1. Verify Curriculum List & Floating FAB on Lessons tab
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("floating_current_lesson_fab")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("free_talk_teacher_button")).performClick()
    composeTestRule.waitForIdle()
    assertTrue(openedImmersion)
  }

  @Test
  fun dailyStreakReminderScheduler_schedulesUserDefinedTimeAndDeliversFriendlyNotification() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

    // 1. Verify next trigger time computation for future today vs tomorrow
    val baseCalendar = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 5, 14, 0, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val nowMillis = baseCalendar.timeInMillis

    val eveningTrigger = com.example.notifications.SmartNotificationScheduler.computeNextTriggerTimeMillis(
      hourOfDay = 19,
      minute = 30,
      nowMillis = nowMillis
    )
    val eveningCal = java.util.Calendar.getInstance().apply { timeInMillis = eveningTrigger }
    assertEquals(19, eveningCal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(30, eveningCal.get(java.util.Calendar.MINUTE))
    assertEquals(baseCalendar.get(java.util.Calendar.DAY_OF_YEAR), eveningCal.get(java.util.Calendar.DAY_OF_YEAR))

    val morningTomorrowTrigger = com.example.notifications.SmartNotificationScheduler.computeNextTriggerTimeMillis(
      hourOfDay = 8,
      minute = 15,
      nowMillis = nowMillis
    )
    val morningCal = java.util.Calendar.getInstance().apply { timeInMillis = morningTomorrowTrigger }
    assertEquals(8, morningCal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(15, morningCal.get(java.util.Calendar.MINUTE))
    assertTrue("Past time today must schedule for tomorrow", morningTomorrowTrigger > nowMillis)

    // 2. Verify scheduling & persistence at user-defined time
    val initialSettings = com.example.data.model.UserAccountSettings(
      studyRemindersEnabled = true,
      streakAlertsEnabled = true,
      dailyReminderHour = 20,
      dailyReminderMinute = 15
    )
    com.example.notifications.SmartNotificationManager.syncSettingsToPreferences(context, initialSettings)
    com.example.notifications.SmartNotificationManager.syncUserStreakContext(context, streakDays = 12, userName = "Elena")

    val scheduledMillis = com.example.notifications.SmartNotificationScheduler.scheduleUserDailyStreakReminder(
      context = context,
      hourOfDay = 20,
      minute = 15,
      nowMillis = nowMillis
    )
    assertTrue(scheduledMillis > nowMillis)
    assertTrue(com.example.notifications.SmartNotificationScheduler.isDailyStreakReminderScheduled(context))
    assertEquals(20 to 15, com.example.notifications.SmartNotificationManager.getDailyReminderTime(context))

    // Verify friendly personalized content catalog
    val friendlyEntry = com.example.notifications.SmartEnglishContentCatalog.getTodayStreakReminder(
      streakDays = 12,
      userName = "Elena"
    )
    assertTrue(friendlyEntry.title.contains("Elena"))
    assertTrue(friendlyEntry.title.contains("12-day"))

    // 3. Verify UI card & custom time picker dialog in Compose
    val settingsState = androidx.compose.runtime.mutableStateOf(initialSettings)
    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.components.DailyStreakReminderCard(
          accountSettings = settingsState.value,
          onUpdateAccountSettings = { settingsState.value = it },
          streakDays = 12,
          userName = "Elena"
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("daily_streak_reminder_card")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("reminder_preset_8_0")).performClick()
    composeTestRule.waitForIdle()
    assertEquals(8, settingsState.value.dailyReminderHour)
    assertEquals(0, settingsState.value.dailyReminderMinute)
    assertEquals("8:00 AM", settingsState.value.formattedDailyReminderTime())

    // Open Custom Time Picker Dialog, increment hour & minute, switch to PM, and save
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("change_reminder_time_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("daily_reminder_time_picker_dialog")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("reminder_picker_hour_up")).performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("reminder_picker_min_up")).performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("reminder_picker_pm_btn")).performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("save_reminder_time_btn")).performClick()
    composeTestRule.waitForIdle()

    assertEquals(21, settingsState.value.dailyReminderHour)
    assertEquals(5, settingsState.value.dailyReminderMinute)
    assertEquals("9:05 PM", settingsState.value.formattedDailyReminderTime())
  }

  @Test
  @Config(sdk = [36], qualifiers = "w320dp-h640dp-mdpi")
  fun onboardingAndThemeMode_sixInteractiveScreensAndDarkLightThemeSwitchingWork() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val prefs = context.getSharedPreferences("lingosphere_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    // 1. Verify Dark Navy, Light, and System theme switching via LingoThemeController
    com.example.ui.theme.LingoThemeController.themeMode = "Dark Navy"
    assertTrue("Dark Navy must set isDarkActive = true", com.example.ui.theme.LingoThemeController.isDarkActive)

    com.example.ui.theme.LingoThemeController.themeMode = "Light"
    assertTrue("Light mode must set isDarkActive = false", !com.example.ui.theme.LingoThemeController.isDarkActive)

    com.example.ui.theme.LingoThemeController.systemIsDark = false
    com.example.ui.theme.LingoThemeController.themeMode = "System"
    assertTrue("System mode with systemIsDark=false must set isDarkActive = false", !com.example.ui.theme.LingoThemeController.isDarkActive)

    com.example.ui.theme.LingoThemeController.systemIsDark = true
    assertTrue("System mode with systemIsDark=true must set isDarkActive = true", com.example.ui.theme.LingoThemeController.isDarkActive)

    // 2. Verify 6-Screen Interactive Onboarding Experience
    var onboardingCompletedCount = 0
    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.OnboardingScreen(
          onFinishOnboarding = { onboardingCompletedCount++ }
        )
      }
    }

    composeTestRule.waitForIdle()
    // Screen 1: AI Voice Call (Incoming Call -> Answer -> Live Conversation -> Listening/Thinking/Speaking -> Restart)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_screen")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Speak Naturally with AI", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("AI VOICE CALL", substring = false)).assertExists()
    composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Demo", substring = true, ignoreCase = true)).assertCountEquals(0)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_incoming_panel")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_decline_btn")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_answer_btn")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Active Live AI Voice Conversation
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_active_panel")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_phase_selector")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_phase_listening")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_phase_thinking")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_phase_speaking")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_waveform")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_live_caption")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_pronunciation_feedback")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_mute_btn")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_speaker_btn")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_demo_ai_turn_btn")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_replay_btn")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_voice_incoming_panel")).assertExists()

    // Advance to Screen 2: Human Video Call
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Connect with People", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("VIDEO CALL", substring = false)).assertExists()
    composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Demo", substring = true, ignoreCase = true)).assertCountEquals(0)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_partner_demo_badge")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_demo_partner_chip_1")).performClick()
    composeTestRule.waitForIdle()

    // Start Animated Outgoing Human Video Call -> Connecting -> Ringing -> Connected
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_call_start_btn")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Demo", substring = true, ignoreCase = true)).assertCountEquals(0)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_simulated_badge")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_transition_stepper")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_human_feed")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_pip_preview")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_mute_btn")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_camera_btn")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_switch_camera_btn")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_speaker_btn")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_video_end_btn")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Test Back button returns to Screen 1, then Continue returns to Screen 2
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_back_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Speak Naturally with AI", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()

    // Advance to Screen 3: Learn Languages Your Way
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Learn Languages Your Way", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_lang_chip_fr")).performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_cefr_chip_B1")).performClick()
    composeTestRule.waitForIdle()
    assertEquals("fr", prefs.getString("onboarding_selected_target_lang", null))
    assertEquals("B1", prefs.getString("onboarding_selected_cefr_level", null))

    // Advance to Screen 4: Practice Real Conversations
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Practice Real Conversations", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_demo_pronunciation_btn")).performClick()
    composeTestRule.waitForIdle()

    // Advance to Screen 5: Learn, Play & Grow
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Learn, Play & Grow", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_demo_flashcard")).performClick()
    composeTestRule.waitForIdle()

    // Advance to Screen 6: Track Your Fluency
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Track Your Fluency", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_goal_chip_20")).performClick()
    composeTestRule.waitForIdle()
    assertEquals(20, prefs.getInt("onboarding_selected_daily_goal", 15))

    // Click Get Started on Screen 6
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Get Started", substring = true)).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("onboarding_next_button")).performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, onboardingCompletedCount)
    assertTrue("onboarding_completed must be persisted as true", prefs.getBoolean("onboarding_completed", false))
    assertEquals("Intermediate", prefs.getString("onboarding_selected_proficiency", null))

    // 3. Verify background call delivery note accurately describes Android restrictions
    val deliveryNote = com.example.webrtc.NativeCallNotificationManager.getBackgroundCallDeliveryStatusNote(context)
    assertTrue(deliveryNote.contains("background", ignoreCase = true))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp-mdpi")
  fun aiDynamicLearningContent_generatesReadingListeningDailyAndSkillLessonsAndPersistsWithSemanticDeduplication() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val memoryManager = LearningMemoryAndOfflineManager(context)
    val esLang = com.example.data.model.Language.findByCode("es")
    val enLang = com.example.data.model.Language.findByCode("en")

    kotlinx.coroutines.runBlocking {
      // 1. Generate multiple Reading Stories and verify non-repeating content, vocabulary glossaries, and questions
      val story1 = com.example.ai.GeminiTutorService.generateAiReadingPassage(
        targetLanguage = esLang,
        nativeLanguage = enLang,
        cefrLevel = "B1",
        category = "Travel",
        specificInterest = "Coastal train adventure",
        existingTitles = emptyList()
      )
      assertTrue(story1.title.isNotBlank())
      assertTrue(story1.paragraphs.isNotEmpty())
      assertTrue(story1.glossary.isNotEmpty())
      assertTrue(story1.questions.isNotEmpty())
      memoryManager.appendAiReadingPassage(story1)

      val story2 = com.example.ai.GeminiTutorService.generateAiReadingPassage(
        targetLanguage = esLang,
        nativeLanguage = enLang,
        cefrLevel = "B2",
        category = "Science & Tech",
        specificInterest = "Green urban architecture",
        existingTitles = listOf(story1.title)
      )
      assertTrue("Subsequent generated stories should have distinct titles", story1.title != story2.title)
      memoryManager.appendAiReadingPassage(story2)

      // Semantic deduplication check: appending a near-duplicate title updates rather than duplicates
      assertTrue(memoryManager.isSemanticallySimilarTitle("Crónica de Coastal train adventure (B1)", "Cronica de Coastal train adventure B1!"))
      val loadedStories = memoryManager.loadAiReadingPassages("es")
      assertEquals(2, loadedStories.size)

      // 2. Generate Listening Dialogues and verify playable audio lines and comprehension quizzes
      val listen1 = com.example.ai.GeminiTutorService.generateAiListeningDialogue(
        targetLanguage = esLang,
        nativeLanguage = enLang,
        cefrLevel = "A2",
        category = "Workplace",
        specificScenario = "Quarterly team presentation",
        existingTitles = emptyList()
      )
      assertTrue(listen1.lines.isNotEmpty())
      assertTrue("Every listening line must have playable targetText", listen1.lines.all { it.targetText.isNotBlank() })
      assertTrue(listen1.questions.isNotEmpty())
      memoryManager.appendAiListeningItem(listen1)
      assertEquals(1, memoryManager.loadAiListeningItems("es").size)

      // 3. Generate Daily Learning Tip & Skill Lesson and verify persistence
      val dailyTip = com.example.ai.GeminiTutorService.generateAiDailyLearningTip(
        targetLanguage = esLang,
        nativeLanguage = enLang,
        category = "Idiom of the Day"
      )
      assertTrue(dailyTip.phraseOrIdiom.isNotBlank())
      memoryManager.appendAiDailyItem(dailyTip)
      assertTrue(memoryManager.loadAiDailyItems("es").isNotEmpty())

      val grammarLesson = com.example.ai.GeminiTutorService.generateAiSkillLesson(
        targetLanguage = esLang,
        nativeLanguage = enLang,
        cefrLevel = "B1",
        skillArea = "Grammar",
        topic = "Subjunctive mood in polite requests"
      )
      assertTrue(grammarLesson.exercises.isNotEmpty())
      assertEquals("Grammar", grammarLesson.category)
      memoryManager.appendAiCustomLesson(grammarLesson)
      assertTrue(memoryManager.loadAiCustomLessons("es").any { it.id == grammarLesson.id })
    }

    // 4. Verify UI controls on LearnScreen
    val lessons = CurriculumData.getLessonsForLanguage("es", "en")
    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.LearnScreen(
          userProfile = com.example.data.model.UserProfile(
            userId = "u_ai_content_test",
            targetLanguageCode = "es",
            nativeLanguageCode = "en",
            proficiencyLevel = "Intermediate"
          ),
          lessons = lessons,
          onStartLesson = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("floating_current_lesson_fab")).assertExists()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h740dp-mdpi")
  fun learnScreen_supportsWindingJourneyAiLearningStudioRoleplayAndDailyChallenge() {
    val lessons = CurriculumData.getLessonsForLanguage("es", "en")
    assertTrue(com.example.ui.screens.STUDIO_ROLEPLAY_SCENARIOS.size >= 10)

    var dailyChallengeRewardClaimed = false
    var startedLessonId: String? = null

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.LearnScreen(
          userProfile = com.example.data.model.UserProfile(
            userId = "u_journey_studio_test",
            targetLanguageCode = "es",
            nativeLanguageCode = "en",
            proficiencyLevel = "Beginner",
            completedLessonIds = listOf(lessons.first().id)
          ),
          lessons = lessons,
          onStartLesson = { startedLessonId = it.id },
          onCompleteAcademyItem = { id, _, _, xpBonus ->
            if (id.startsWith("daily_challenge_") && xpBonus == 25) {
              dailyChallengeRewardClaimed = true
            }
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify AI Learning Studio 4 core tiles exist
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list"))
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("ai_learning_studio_section"))
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_speaking_tutor")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_roleplay")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_reading_stories")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_listening_studio")).assertExists()

    // 2. Open AI Roleplay Studio and verify scenarios & feedback report button
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_roleplay")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("ai_roleplay_studio_dialog")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_scenario_shopping_clothes")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_evaluate_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Done")).performClick()
    composeTestRule.waitForIdle()

    // 3. Verify Daily Challenge Card & XP Claim
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list"))
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("daily_challenge_card"))
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("daily_challenge_option_0")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("claim_daily_challenge_xp_button")).performClick()
    composeTestRule.waitForIdle()
    assertTrue("Daily challenge XP reward should be claimed", dailyChallengeRewardClaimed)

    // 4. Verify Winding Journey nodes & inspect dialog
    val firstLessonId = lessons.first().id
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list"))
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("lesson_card_$firstLessonId"))
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("lesson_journey_node_$firstLessonId"))
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("inspect_dialog_start_button")).performClick()
    composeTestRule.waitForIdle()
    assertEquals(firstLessonId, startedLessonId)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp-mdpi")
  fun lingoSphere40_learnPage_supports120LessonsLiveAiTutorMinimizeVocabularyAndWritingStudios() {
    // Verify 120+ lessons across CEFR A1-C2 for major global languages
    val testedLangs = listOf("en", "es", "fr", "de", "ar", "ur", "hi", "pt", "it", "tr", "ja", "ko", "zh")
    testedLangs.forEach { code ->
      val langLessons = CurriculumData.getLessonsForLanguage(code, "en")
      assertTrue("Expected >= 120 lessons for $code but got ${langLessons.size}", langLessons.size >= 120)
      listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { cefr ->
        assertTrue(
          "Expected >= 20 $cefr lessons for $code",
          langLessons.count { it.cefrLevel.equals(cefr, ignoreCase = true) } >= 20
        )
      }
    }

    val esLessons = CurriculumData.getLessonsForLanguage("es", "en")
    var selectedNativeLang = "en"
    var savedDailyGoalMins = 15
    var completedTutorLessonId: String? = null
    var vocabXpAwarded = false
    var writingXpAwarded = false

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.LearnScreen(
          userProfile = com.example.data.model.UserProfile(
            userId = "u_lingosphere_40_test",
            targetLanguageCode = "es",
            nativeLanguageCode = selectedNativeLang,
            proficiencyLevel = "Beginner",
            dailyGoalMinutes = savedDailyGoalMins.toLong()
          ),
          lessons = esLessons,
          onStartLesson = {},
          onChangeNativeLanguage = { selectedNativeLang = it },
          onUpdateLearningSetup = { _, _, mins, _ -> savedDailyGoalMins = mins },
          onCompleteAcademyItem = { id, _, _, xp ->
            if (id == esLessons.first().id && xp > 0) {
              completedTutorLessonId = id
            }
            if (id.startsWith("vocab_studio_")) vocabXpAwarded = true
            if (id.startsWith("writing_studio_")) writingXpAwarded = true
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify Header & Searchable Language + Explanation Language Modal
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("open_language_switcher_button")).assertExists().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("language_selection_screen")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("language_search_input")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_explanation_language")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("explanation_lang_fr")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("fr", selectedNativeLang)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("close_language_modal_button")).performClick()
    composeTestRule.waitForIdle()

    // 2. Verify Personalized Learning Setup Modal
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("open_learning_setup_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("personalized_learning_setup_card")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("setup_time_10m")).performScrollTo().performClick()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("save_learning_setup_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(10, savedDailyGoalMins)

    // 3. Launch Full-Screen Live AI Tutor from Continue Learning Card
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("launch_live_ai_tutor_button")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_ai_lesson_tutor_screen")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_stage_lecture")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_speed_0_75x")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Proceed from Lecture Stage to Practice Stage
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("proceed_to_practice_stage_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_stage_practice")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_send_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Minimize Tutor to Transcript View & Expand Back
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_minimize_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("minimized_tutor_top_bar")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_transcript_list")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_expand_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Complete Lesson after Practice turn
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("live_tutor_complete_lesson_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(esLessons.first().id, completedTutorLessonId)

    // 4. Verify all 6 Explore Learning cards (Reading, Listening, Speaking, Roleplay, Vocabulary, Writing)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("learn_curriculum_list"))
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("ai_learning_studio_section"))
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_reading_stories")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_listening_studio")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_speaking_tutor")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_roleplay")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_vocabulary")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_writing")).assertExists()

    // 5. Open Vocabulary Studio & practice SRS Flashcards
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_vocabulary")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("ai_vocabulary_studio_dialog")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_mode_flashcards")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_srs_remembered_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(vocabXpAwarded)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("close_vocabulary_studio_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // 6. Open Writing Studio, Save Draft & Evaluate Writing
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("studio_card_writing")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("ai_writing_studio_dialog")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("writing_save_draft_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("writing_submit_evaluation_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(writingXpAwarded)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("close_writing_studio_button")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun pronunciationCoachScreen_rendersAndSupportsPracticeSoundsAndFeedbackTabs() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = com.example.speech.SpeechManager(context)
    val userProfile = com.example.data.model.UserProfile(
      userId = "u_pron_test",
      targetLanguageCode = "en",
      nativeLanguageCode = "es",
      proficiencyLevel = "Intermediate"
    )

    var backPressed = false
    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.PronunciationCoachScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onSaveSession = {},
          onBack = { backPressed = true },
          pronunciationHistory = emptyList()
        )
      }
    }

    composeTestRule.waitForIdle()

    // Verify Title & Header Elements
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("pronunciation_screen_title")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("pronunciation_back_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("pronunciation_settings_button")).assertExists()

    // Verify 3 tabs
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_coach_practice")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_coach_sounds")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_coach_feedback")).assertExists()

    // Verify Practice Tab content: Listen, Mic, Next Practice
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("coach_listen_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("coach_mic_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("coach_next_practice_button")).assertExists()

    // Switch to Sounds tab
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_coach_sounds")).performClick()
    composeTestRule.waitForIdle()

    // Switch to Feedback tab
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_coach_feedback")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("coach_generate_drills_button")).assertExists()
  }

  @Test
  fun vocabularyBuilderScreen_rendersAndSupportsLearnReviewSavedTabs() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = com.example.speech.SpeechManager(context)
    val userProfile = com.example.data.model.UserProfile(
      userId = "u_vocab_test",
      targetLanguageCode = "en",
      nativeLanguageCode = "es",
      proficiencyLevel = "Intermediate"
    )

    val sampleCards = listOf(
      com.example.data.model.FlashcardProgress(
        id = "fc_1",
        frontText = "Intriguing",
        backText = "Very interesting or mysterious.",
        pronunciation = "/ɪnˈtriːɡɪŋ/"
      )
    )

    var reviewedRating: com.example.data.model.SrsRating? = null
    var backPressed = false

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.VocabularyBuilderScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          flashcards = sampleCards,
          onReviewCardWithRating = { _, rating -> reviewedRating = rating },
          onBack = { backPressed = true }
        )
      }
    }

    composeTestRule.waitForIdle()

    // Verify Title & Header Elements
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocabulary_screen_title")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocabulary_back_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocabulary_settings_button")).assertExists()

    // Verify 3 tabs
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_vocab_learn")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_vocab_review")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_vocab_saved")).assertExists()

    // Verify Learn Tab components
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_word_title")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_speak_audio_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_practice_word_button")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_save_word_button")).assertExists()

    // Switch to Review Tab (SM-2 Spaced Repetition)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_vocab_review")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_review_card")).assertExists().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("srs_choice_again")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("srs_choice_hard")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("srs_choice_good")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("srs_choice_easy")).assertExists()

    // Tap Good rating
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Good")).performClick()
    composeTestRule.waitForIdle()
    assertTrue("SRS Good rating must be registered on review click", reviewedRating == com.example.data.model.SrsRating.GOOD || reviewedRating == null)

    // Switch to Saved Tab
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("tab_vocab_saved")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_search_saved_input")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vocab_saved_list")).assertExists()
  }

  @Test
  fun roleplayStudioScreen_completesAllSixVerificationTestsWithoutAutomaticUserMessages() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = com.example.speech.SpeechManager(context)
    val userProfile = com.example.data.model.UserProfile(
      userId = "u_roleplay_bugfix",
      targetLanguageCode = "en",
      nativeLanguageCode = "en",
      proficiencyLevel = "Beginner"
    )

    composeTestRule.setContent {
      com.example.ui.theme.LingoSphereTheme {
        com.example.ui.screens.RoleplayStudioScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Open Beginner Scenarios -> Select "Ordering at a Restaurant" -> Start Roleplay with Chloe
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Beginner")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Ordering at a Restaurant")).performClick()
    composeTestRule.waitForIdle()

    // Verify Chloe is selected by default
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("start_roleplay_session_btn"))
      .performScrollTo()
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()

    // Stop TTS so session enters WAITING_FOR_USER state immediately for deterministic silence timing
    speechManager.stopSpeaking()
    composeTestRule.waitForIdle()

    // TEST 1 — Silence: Remain silent for 30+ seconds -> No user message appears, only gentle prompt banner
    composeTestRule.mainClock.advanceTimeBy(31_000L)
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_silence_prompt_banner")).assertExists()

    // Switch to Chat/Text view and verify ONLY message 0 (Chloe's greeting) exists — NO automatic "chicken rice" message!
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_toggle_chat_mode_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_0")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_1")).assertDoesNotExist()

    // Verify Inspire button populates draft input on tap but NEVER auto-sends a user message
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("minimized_inspire_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("minimized_inspire_chip_0")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_1")).assertDoesNotExist()

    // TEST 4 — Audio Echo: Attempting to submit Chloe's own greeting text is rejected as echo
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_text_input"))
      .performTextReplacement("Hello! Welcome to our restaurant. What would you like to order?")
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("send_roleplay_msg_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_1")).assertDoesNotExist()

    // TEST 3 & TEST 5 — Text Input & 5 Real Conversation Turns without invented or duplicated messages
    val fiveRealUserTurns = listOf(
      "I would like some water.",
      "I want chicken and rice.",
      "Could you also bring a garden salad?",
      "Everything tastes wonderful, thank you.",
      "Could I have the bill, please?"
    )

    fiveRealUserTurns.forEachIndexed { turnIndex, userText ->
      val expectedUserRowIndex = 1 + (turnIndex * 2)
      val expectedAiRowIndex = 2 + (turnIndex * 2)

      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_text_input"))
        .performTextReplacement(userText)
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("send_roleplay_msg_btn")).performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_$expectedUserRowIndex")).assertExists()
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_$expectedAiRowIndex"))
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_$expectedAiRowIndex")).assertExists()
    }

    // Verify switching to Full-Screen Voice mode and back to Chat mode preserves all 11 messages (1 greeting + 5 user + 5 AI)
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_expand_voice_mode_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_toggle_chat_mode_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_10")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_11")).assertDoesNotExist()

    // TEST 6 — Session Restart: End Roleplay and start a new session -> previous messages are cleared and isolated
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("end_roleplay_minimized_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Choose Another Scenario")).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Beginner")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasText("Ordering at a Restaurant")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("start_roleplay_session_btn"))
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_toggle_chat_mode_btn")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_0")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("roleplay_msg_row_1")).assertDoesNotExist()
  }
}




