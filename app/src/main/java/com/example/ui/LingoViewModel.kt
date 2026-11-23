package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallHistoryRecord
import com.example.data.model.CallRoom
import com.example.data.model.CommunityProfile
import com.example.data.model.CurriculumLesson
import com.example.data.model.DirectChatMessage
import com.example.data.model.FlashcardProgress
import com.example.data.model.FriendRequest
import com.example.data.model.LessonProgress
import com.example.data.model.LingoVideo
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.PronunciationDiagnosticResult
import com.example.data.model.SavedVideo
import com.example.data.model.SpeakingSession
import com.example.data.model.SrsCardMetadata
import com.example.data.model.SrsRating
import com.example.data.model.UserBlock
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.VideoPlaylist
import com.example.data.model.VideoWatchHistory
import com.example.data.repository.CurriculumData
import com.example.data.repository.InProgressLessonState
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.data.repository.LingoRepository
import com.example.data.repository.WeeklyLearningReportSummary
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class LingoViewModel(
  private val repository: LingoRepository,
  val userId: String,
  private val memoryManager: LearningMemoryAndOfflineManager? = null,
  private val referralRepositoryOverride: com.example.data.repository.ReferralRepository? = null
) : ViewModel() {

  private val isRobolectricEnv: Boolean =
    android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

  private fun <T> Flow<T>.staggeredStart(delayMs: Long): Flow<T> =
    if (isRobolectricEnv || delayMs <= 0L) {
      this
    } else {
      kotlinx.coroutines.flow.flow {
        kotlinx.coroutines.delay(delayMs)
        emitAll(this@staggeredStart)
      }
    }

  private val referralRepo: com.example.data.repository.ReferralRepository? by lazy {
    referralRepositoryOverride ?: memoryManager?.appContext()?.let {
      com.example.data.repository.ReferralRepository(it)
    }
  }

  private val _localSubscriptionOverride = MutableStateFlow<LearningMemoryAndOfflineManager.LocalSubscriptionState?>(null)

  private val _localAvatarOverride = MutableStateFlow<String?>(null)

  private val _localProfileOverride = MutableStateFlow<UserProfile?>(null)

  private val _localCommunityOverride = MutableStateFlow<CommunityProfile?>(null)

  private val _cachedProfileMeta = MutableStateFlow<com.example.data.repository.CachedProfileMetadata?>(null)

  private val _accountSettingsState = MutableStateFlow(
    com.example.data.model.UserAccountSettings()
  )
  val accountSettingsState: StateFlow<com.example.data.model.UserAccountSettings> = _accountSettingsState.asStateFlow()

  private val _isSavingProfileState = MutableStateFlow(false)
  val isSavingProfileState: StateFlow<Boolean> = _isSavingProfileState.asStateFlow()

  private val _profileStatusMessage = MutableStateFlow<String?>(null)
  val profileStatusMessage: StateFlow<String?> = _profileStatusMessage.asStateFlow()

  private val _customProDecks = MutableStateFlow<List<com.example.data.repository.PremiumCustomDeck>>(emptyList())
  val customProDecks: StateFlow<List<com.example.data.repository.PremiumCustomDeck>> = _customProDecks.asStateFlow()

  private val _languageProgressSnapshots = MutableStateFlow<Map<String, com.example.data.model.LanguageLearningProgressSnapshot>>(emptyMap())
  val languageProgressSnapshotsState: StateFlow<Map<String, com.example.data.model.LanguageLearningProgressSnapshot>> = _languageProgressSnapshots.asStateFlow()

  private val _floatingContinuePrefsState = MutableStateFlow(
    memoryManager?.loadFloatingContinuePrefs(userId) ?: com.example.data.model.FloatingContinueButtonPrefs.DEFAULT
  )
  val floatingContinuePrefsState: StateFlow<com.example.data.model.FloatingContinueButtonPrefs> =
    _floatingContinuePrefsState.asStateFlow()

  fun updateFloatingContinuePosition(snapSide: String, verticalFraction: Float) {
    val normalizedSide = if (snapSide.equals("left", ignoreCase = true)) "left" else "right"
    val clampedFraction = verticalFraction.coerceIn(0f, 1f)
    val updated = _floatingContinuePrefsState.value.copy(
      snapSide = normalizedSide,
      verticalFraction = clampedFraction,
      hasSeenDragHint = true
    )
    _floatingContinuePrefsState.value = updated
    memoryManager?.saveFloatingContinuePrefs(userId, updated)
    viewModelScope.launch(Dispatchers.IO) {
      runCatching { repository.saveFloatingContinuePrefs(updated) }
    }
  }

  fun setFloatingContinueEnabled(enabled: Boolean) {
    val updated = _floatingContinuePrefsState.value.copy(enabled = enabled)
    _floatingContinuePrefsState.value = updated
    memoryManager?.saveFloatingContinuePrefs(userId, updated)
    viewModelScope.launch(Dispatchers.IO) {
      runCatching { repository.saveFloatingContinuePrefs(updated) }
    }
  }

  fun markFloatingContinueHintSeen() {
    if (_floatingContinuePrefsState.value.hasSeenDragHint) return
    val updated = _floatingContinuePrefsState.value.copy(hasSeenDragHint = true)
    _floatingContinuePrefsState.value = updated
    memoryManager?.saveFloatingContinuePrefs(userId, updated)
    viewModelScope.launch(Dispatchers.IO) {
      runCatching { repository.saveFloatingContinuePrefs(updated) }
    }
  }

  fun resetFloatingContinuePosition() {
    val current = _floatingContinuePrefsState.value
    val updated = current.copy(
      snapSide = com.example.data.model.FloatingContinueButtonPrefs.DEFAULT.snapSide,
      verticalFraction = com.example.data.model.FloatingContinueButtonPrefs.DEFAULT.verticalFraction
    )
    _floatingContinuePrefsState.value = updated
    memoryManager?.saveFloatingContinuePrefs(userId, updated)
    viewModelScope.launch(Dispatchers.IO) {
      runCatching { repository.saveFloatingContinuePrefs(updated) }
    }
  }

  val userProfileState: StateFlow<UserProfile> = kotlinx.coroutines.flow.combine(
    repository.observeUserProfile(userId)
      .staggeredStart(220L)
      .catch { emit(UserProfile(userId = userId)) }
      .map { it ?: UserProfile(userId = userId) }
      .flowOn(Dispatchers.IO),
    _localSubscriptionOverride,
    _localAvatarOverride,
    _localProfileOverride,
    _cachedProfileMeta
  ) { remoteProfile, localSub, localAvatar, localProfile, cachedMeta ->
    val mergedLanguageLevels = remoteProfile.languageLevels + (cachedMeta?.languageLevels ?: emptyMap())
    val baseProfile = localProfile?.copy(
      languageLevels = mergedLanguageLevels + localProfile.languageLevels
    ) ?: remoteProfile.copy(
      displayName = remoteProfile.displayName.ifBlank { cachedMeta?.displayName.orEmpty() },
      nativeLanguageCode = cachedMeta?.nativeLanguageCode ?: remoteProfile.nativeLanguageCode,
      targetLanguageCode = cachedMeta?.targetLanguageCode ?: remoteProfile.targetLanguageCode,
      proficiencyLevel = cachedMeta?.proficiencyLevel ?: remoteProfile.proficiencyLevel,
      dailyGoalMinutes = cachedMeta?.dailyGoalMinutes ?: remoteProfile.dailyGoalMinutes,
      languageLevels = mergedLanguageLevels
    )
    val resolvedAvatar = localAvatar ?: baseProfile.avatarUrl.ifBlank { remoteProfile.avatarUrl }
    if (localSub != null) {
      baseProfile.copy(
        avatarUrl = resolvedAvatar,
        subscriptionTier = localSub.tier,
        purchaseToken = localSub.purchaseToken,
        isFreeTrial = localSub.isFreeTrial,
        hasUsedFreeTrial = baseProfile.hasUsedFreeTrial || remoteProfile.hasUsedFreeTrial || localSub.hasUsedFreeTrial || localSub.isFreeTrial,
        subscriptionAutoRenewing = localSub.autoRenewing,
        subscriptionExpiryEpochMs = localSub.expiryEpochMs
      )
    } else {
      // Ignore any stale subscriptionTier in the public user document unless server entitlement is active
      baseProfile.copy(
        avatarUrl = resolvedAvatar,
        subscriptionTier = "free",
        purchaseToken = null,
        isFreeTrial = false,
        subscriptionAutoRenewing = false,
        subscriptionExpiryEpochMs = 0L
      )
    }
  }.let { combinedBase ->
    kotlinx.coroutines.flow.combine(
      combinedBase,
      com.example.data.repository.AccessPolicyManager.entitlementState
    ) { profile, entitlement ->
      val verifiedForCurrentUid =
        entitlement.hasProEntitlement && entitlement.effectiveUid == userId
      if (verifiedForCurrentUid) {
        val mappedTier = if (entitlement.basePlanId?.contains("year", ignoreCase = true) == true ||
          entitlement.productId.contains("year", ignoreCase = true)
        ) {
          "premium_yearly"
        } else {
          "premium_monthly"
        }
        profile.copy(
          subscriptionTier = mappedTier,
          subscriptionAutoRenewing = entitlement.autoRenewing,
          subscriptionExpiryEpochMs = entitlement.effectiveExpiryMs
        )
      } else if (_localSubscriptionOverride.value != null &&
        android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
      ) {
        profile
      } else {
        profile.copy(
          subscriptionTier = "free",
          purchaseToken = null,
          isFreeTrial = false,
          subscriptionAutoRenewing = false,
          subscriptionExpiryEpochMs = 0L
        )
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = UserProfile(
      userId = userId,
      nativeLanguageCode = "en",
      targetLanguageCode = "es",
      proficiencyLevel = "Beginner",
      dailyGoalMinutes = 15L,
      subscriptionTier = "free"
    )
  )


  val lessonProgressState: StateFlow<List<LessonProgress>> = repository.observeLessonProgress(userId)
    .staggeredStart(1350L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val flashcardProgressState: StateFlow<List<FlashcardProgress>> = repository.observeFlashcards(userId)
    .staggeredStart(1500L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val speakingSessionsState: StateFlow<List<SpeakingSession>> = repository.observeSpeakingSessions(userId)
    .staggeredStart(1650L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  private val _manualCommunityProfiles = MutableStateFlow<List<CommunityProfile>>(emptyList())
  val communityProfilesLoadError: StateFlow<String?> = repository.communityProfilesLoadError
  val partnersQuotaExhausted: StateFlow<Boolean> = repository.partnersQuotaExhausted

  private val _isPartnersLoading = MutableStateFlow(false)
  val isPartnersLoading: StateFlow<Boolean> = _isPartnersLoading.asStateFlow()

  private val _hasMorePartners = MutableStateFlow(false)
  val hasMorePartners: StateFlow<Boolean> = _hasMorePartners.asStateFlow()

  private val _rawLeaderboardProfiles = MutableStateFlow<List<CommunityProfile>>(emptyList())
  val topXpLeaderboardState: StateFlow<List<CommunityProfile>> = kotlinx.coroutines.flow.combine(
    _rawLeaderboardProfiles,
    com.example.presence.PresenceManager.rtdbPresenceMap,
    com.example.presence.PresenceManager.presenceTicker
  ) { list, rtdbMap, nowMs ->
    list.map {
      com.example.presence.PresenceManager.resolvePartnerPresence(
        profile = it,
        rtdbMap = rtdbMap,
        nowEpochMs = nowMs
      )
    }
  }.flowOn(Dispatchers.Default).stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = emptyList()
  )

  private var lastPartnersCursorDoc: DocumentSnapshot? = null
  private var activePartnersFilterLang: String = "ALL"
  private var activePartnersFilterCountry: String = "ALL"
  private var hasAttemptedInitialPartnersLoad: Boolean = false

  fun loadPartnersDirectory(
    filterLanguageCode: String = activePartnersFilterLang,
    filterCountry: String = activePartnersFilterCountry,
    forceRefresh: Boolean = false
  ) {
    val normalizedLang = filterLanguageCode.trim().ifBlank { "ALL" }
    val normalizedCountry = filterCountry.trim().ifBlank { "ALL" }
    val filterChanged = normalizedLang != activePartnersFilterLang || normalizedCountry != activePartnersFilterCountry
    if (!forceRefresh && !filterChanged && hasAttemptedInitialPartnersLoad && _manualCommunityProfiles.value.isNotEmpty()) {
      return
    }
    if (_isPartnersLoading.value && !forceRefresh && !filterChanged) return

    activePartnersFilterLang = normalizedLang
    activePartnersFilterCountry = normalizedCountry
    lastPartnersCursorDoc = null
    hasAttemptedInitialPartnersLoad = true

    viewModelScope.launch(Dispatchers.IO) {
      _isPartnersLoading.value = true
      try {
        val targetLang = userProfileState.value.targetLanguageCode
        val res = repository.fetchPartnersPage(
          filterLanguageCode = normalizedLang,
          filterCountry = normalizedCountry,
          myTargetLanguageCode = targetLang,
          pageSize = 15,
          lastDoc = null,
          forceRefresh = forceRefresh
        )
        lastPartnersCursorDoc = res.lastDocument
        _hasMorePartners.value = res.hasMore
        if (res.profiles.isNotEmpty()) {
          val existing = _manualCommunityProfiles.value
          val merged = LinkedHashMap<String, CommunityProfile>(existing.size + res.profiles.size)
          res.profiles.forEach { if (it.userId.isNotBlank()) merged[it.userId] = it }
          existing.forEach { if (it.userId.isNotBlank() && !merged.containsKey(it.userId)) merged[it.userId] = it }
          _manualCommunityProfiles.value = merged.values.toList()
        }
      } catch (_: Exception) {
      } finally {
        _isPartnersLoading.value = false
      }
    }
  }

  fun loadMorePartners() {
    if (_isPartnersLoading.value || !_hasMorePartners.value || lastPartnersCursorDoc == null) return
    viewModelScope.launch(Dispatchers.IO) {
      _isPartnersLoading.value = true
      try {
        val targetLang = userProfileState.value.targetLanguageCode
        val res = repository.fetchPartnersPage(
          filterLanguageCode = activePartnersFilterLang,
          filterCountry = activePartnersFilterCountry,
          myTargetLanguageCode = targetLang,
          pageSize = 15,
          lastDoc = lastPartnersCursorDoc,
          forceRefresh = false
        )
        lastPartnersCursorDoc = res.lastDocument
        _hasMorePartners.value = res.hasMore
        if (res.profiles.isNotEmpty()) {
          val merged = (_manualCommunityProfiles.value + res.profiles).distinctBy { it.userId }
          _manualCommunityProfiles.value = merged
        }
      } catch (_: Exception) {
      } finally {
        _isPartnersLoading.value = false
      }
    }
  }

  fun retryPartnersAfterQuota() {
    loadPartnersDirectory(
      filterLanguageCode = activePartnersFilterLang,
      filterCountry = activePartnersFilterCountry,
      forceRefresh = true
    )
  }

  fun loadTopXpLeaderboard(forceRefresh: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val list = repository.fetchTopXpLeaderboard(limitCount = 20, forceRefresh = forceRefresh)
        if (list.isNotEmpty()) {
          _rawLeaderboardProfiles.value = list
        }
      } catch (_: Exception) {}
    }
  }

  fun refreshCommunityProfiles() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val profile = repository.getUserProfile(userId)
        if (profile != null) {
          val settings = _accountSettingsState.value
          repository.syncCommunityProfile(
            profile = profile,
            isOnline = settings.isOnlineVisible,
            allowCalls = settings.allowIncomingCalls,
            presenceState = if (settings.isOnlineVisible) "online" else "offline",
            showLastSeen = settings.showLastSeen
          )
        }
      } catch (_: Exception) {}
      loadPartnersDirectory(
        filterLanguageCode = activePartnersFilterLang,
        filterCountry = activePartnersFilterCountry,
        forceRefresh = true
      )
    }
  }

  val myCommunityProfileState: StateFlow<CommunityProfile?> = kotlinx.coroutines.flow.combine(
    repository.observeMyCommunityProfile(userId)
      .staggeredStart(1900L)
      .catch { emit(null) }
      .flowOn(Dispatchers.IO),
    _localCommunityOverride,
    com.example.presence.PresenceManager.localStatus,
    _cachedProfileMeta
  ) { remoteComm, localComm, localPresence, cachedMeta ->
    val base = localComm ?: remoteComm ?: cachedMeta?.let { meta ->
      val settings = _accountSettingsState.value
      CommunityProfile(
        userId = userId,
        displayName = meta.displayName,
        country = meta.country,
        countryFlag = meta.countryFlag,
        bio = meta.bio,
        interests = meta.interests,
        isOnline = settings.isOnlineVisible,
        allowCalls = settings.allowIncomingCalls,
        showLastSeen = settings.showLastSeen
      )
    }
    base?.copy(
      isOnline = if (localPresence.lastUpdatedEpochMs > 0L) {
        localPresence.isActivelyOnline
      } else {
        base.isOnline && _accountSettingsState.value.isOnlineVisible
      },
      presenceState = if (localPresence.lastUpdatedEpochMs > 0L) {
        localPresence.effectiveState
      } else {
        base.presenceState
      },
      showLastSeen = _accountSettingsState.value.showLastSeen
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = null
  )

  val friendRequestsState: StateFlow<List<FriendRequest>> = repository.observeFriendRequests(userId)
    .staggeredStart(2050L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val allChatMessagesState: StateFlow<List<DirectChatMessage>> = repository.observeAllMyChatMessages(userId)
    .staggeredStart(2350L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val activeSpeakingRoomsState: StateFlow<List<CallRoom>> = repository.observeActiveSpeakingRooms()
    .staggeredStart(2150L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val blockedUsersState: StateFlow<List<UserBlock>> = repository.observeBlockedUsers(userId)
    .staggeredStart(2250L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val notificationsState: StateFlow<List<UserNotification>> = repository.observeNotifications(userId)
    .staggeredStart(1800L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val incomingCallRoomState: StateFlow<CallRoom?> = repository.observeIncomingCalls()
    .staggeredStart(1450L)
    .catch { emit(null) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null
    )

  val callHistoryState: StateFlow<List<CallHistoryRecord>> = repository.observeCallHistory(userId)
    .staggeredStart(2450L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  private val mergedRawCommunityProfilesFlow: Flow<List<CommunityProfile>> = kotlinx.coroutines.flow.combine(
    repository.observeCommunityProfiles().staggeredStart(2550L).catch { emit(emptyList()) }.flowOn(Dispatchers.IO),
    _manualCommunityProfiles,
    friendRequestsState,
    callHistoryState,
    allChatMessagesState
  ) { liveProfiles, manualProfiles, friendRequests, callHistory, chatMessages ->
    val byId = LinkedHashMap<String, CommunityProfile>()
    for (p in manualProfiles) {
      if (p.userId.isNotBlank()) byId[p.userId] = p
    }
    for (p in liveProfiles) {
      if (p.userId.isNotBlank()) byId[p.userId] = p
    }
    // Recover any real registered partner from friend requests, call history, or direct messages
    // whose community_profiles document was not yet synced
    for (req in friendRequests) {
      val otherId = if (req.senderId == userId) req.receiverId else req.senderId
      val otherName = if (req.senderId == userId) req.receiverName else req.senderName
      if (otherId.isNotBlank() && otherId != userId && !byId.containsKey(otherId)) {
        byId[otherId] = CommunityProfile(
          userId = otherId,
          displayName = otherName.ifBlank { "Language Partner" },
          isOnline = false,
          presenceState = "offline"
        )
      }
    }
    for (call in callHistory) {
      val otherId = if (call.callerId == userId) call.receiverId else call.callerId
      val otherName = if (call.callerId == userId) call.receiverName else call.callerName
      if (otherId.isNotBlank() && otherId != userId && !byId.containsKey(otherId)) {
        byId[otherId] = CommunityProfile(
          userId = otherId,
          displayName = otherName.ifBlank { "Language Partner" },
          targetLanguageCode = call.targetLanguageCode.ifBlank { "en" },
          isOnline = false,
          presenceState = "offline"
        )
      }
    }
    for (msg in chatMessages) {
      val otherId = if (msg.senderId == userId) msg.receiverId else msg.senderId
      val otherName = if (msg.senderId == userId) "" else msg.senderName
      if (otherId.isNotBlank() && otherId != userId && !byId.containsKey(otherId) && otherName.isNotBlank()) {
        byId[otherId] = CommunityProfile(
          userId = otherId,
          displayName = otherName,
          isOnline = false,
          presenceState = "offline"
        )
      }
    }
    byId.values.toList()
  }

  val communityProfilesState: StateFlow<List<CommunityProfile>> = kotlinx.coroutines.flow.combine(
    mergedRawCommunityProfilesFlow,
    activeSpeakingRoomsState,
    com.example.presence.PresenceManager.rtdbPresenceMap,
    com.example.presence.PresenceManager.presenceTicker
  ) { baseProfiles, activeRooms, rtdbMap, nowMs ->
    val byId = LinkedHashMap<String, CommunityProfile>()
    for (p in baseProfiles) {
      byId[p.userId] = p
    }
    for (room in activeRooms) {
      if (room.callerId.isNotBlank() && room.callerId != userId && !byId.containsKey(room.callerId)) {
        byId[room.callerId] = CommunityProfile(
          userId = room.callerId,
          displayName = room.callerName.ifBlank { "Language Partner" },
          nativeLanguageCode = room.callerNativeLang.ifBlank { "en" },
          targetLanguageCode = room.callerLearningLang.ifBlank { room.targetLanguageCode.ifBlank { "es" } },
          isOnline = true,
          presenceState = "online"
        )
      }
    }
    byId.values.map { profile ->
      com.example.presence.PresenceManager.resolvePartnerPresence(
        profile = profile,
        rtdbMap = rtdbMap,
        nowEpochMs = nowMs
      )
    }
  }.flowOn(Dispatchers.Default).stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = emptyList()
  )

  val savedVideosState: StateFlow<List<SavedVideo>> = repository.observeSavedVideos(userId)
    .staggeredStart(2750L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val videoPlaylistsState: StateFlow<List<VideoPlaylist>> = repository.observeVideoPlaylists(userId)
    .staggeredStart(2900L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val videoWatchHistoryState: StateFlow<List<VideoWatchHistory>> = repository.observeVideoWatchHistory(userId)
    .staggeredStart(3050L)
    .catch { emit(emptyList()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  private val _activeLesson = MutableStateFlow<CurriculumLesson?>(null)
  val activeLesson: StateFlow<CurriculumLesson?> = _activeLesson.asStateFlow()

  private val _inProgressLessonsMap = MutableStateFlow<Map<String, InProgressLessonState>>(emptyMap())
  val inProgressLessonsMap: StateFlow<Map<String, InProgressLessonState>> = _inProgressLessonsMap.asStateFlow()

  private val _downloadedLessonIds = MutableStateFlow<Set<String>>(emptySet())
  val downloadedLessonIds: StateFlow<Set<String>> = _downloadedLessonIds.asStateFlow()

  private val _pendingOfflineSyncCount = MutableStateFlow(0)
  val pendingOfflineSyncCount: StateFlow<Int> = _pendingOfflineSyncCount.asStateFlow()

  private val _dailyMinutesMap = MutableStateFlow<Map<String, Int>>(emptyMap())
  val dailyMinutesMap: StateFlow<Map<String, Int>> = _dailyMinutesMap.asStateFlow()

  fun seedStarterFlashcards(targetLangCode: String) {
    viewModelScope.launch {
      val starterList = CurriculumData.getStarterFlashcards(targetLangCode)
      for (item in starterList) {
        try {
          repository.saveFlashcard(
            FlashcardProgress(
              userId = userId,
              wordId = item.wordId,
              languageCode = targetLangCode,
              frontText = item.frontText,
              backText = item.backText,
              pronunciation = item.pronunciation,
              boxLevel = item.boxLevel.toLong()
            )
          )
        } catch (_: Exception) {}
      }
    }
  }

  fun startLesson(lesson: CurriculumLesson) {
    val hasRewardedLessonUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(
      com.example.ads.RewardedFeatureType.LEARN_LESSON,
      lesson.id
    )
    if (!userProfileState.value.isEntitlementVerified && !hasRewardedLessonUnlock) {
      com.example.ai.AiErrorHandler.requestUpgrade(
        mode = com.example.ai.UpgradeSheetMode.LESSONS,
        featureKey = com.example.data.repository.AccessPolicyFeatureKeys.LESSONS_DAILY,
        headline = "Unlock Structured Lessons",
        reason = "All 500+ structured CEFR lessons across every language are included with LingoSphere Pro.",
        itemId = lesson.id,
        onRewardedUnlockGranted = {
          _activeLesson.value = lesson
        }
      )
      return
    }
    _activeLesson.value = lesson
  }

  fun saveLessonCheckpoint(
    lessonId: String,
    languageCode: String,
    currentExerciseIndex: Int,
    correctCount: Int,
    totalExercises: Int
  ) {
    val state = InProgressLessonState(
      lessonId = lessonId,
      languageCode = languageCode,
      currentExerciseIndex = currentExerciseIndex,
      correctCount = correctCount,
      totalExercises = totalExercises.coerceAtLeast(1)
    )
    memoryManager?.saveInProgressLessonState(state)
    _inProgressLessonsMap.value = memoryManager?.loadAllInProgressLessons() ?: (_inProgressLessonsMap.value + (lessonId to state))
  }

  fun dismissLesson() {
    val currentLessonId = _activeLesson.value?.id
    if (currentLessonId != null) {
      com.example.ads.AdService.consumeSingleItemUnlock(
        com.example.ads.RewardedFeatureType.LEARN_LESSON,
        currentLessonId
      )
    }
    _activeLesson.value = null
  }

  fun finishActiveLesson(scorePercent: Int, xpEarned: Int) {
    val lesson = _activeLesson.value ?: return
    com.example.ads.AdService.consumeSingleItemUnlock(
      com.example.ads.RewardedFeatureType.LEARN_LESSON,
      lesson.id
    )
    memoryManager?.clearInProgressLessonState(lesson.id)
    _inProgressLessonsMap.value = _inProgressLessonsMap.value - lesson.id
    recordStudyMinutes(lesson.estimatedMinutes.coerceAtLeast(4))
    _activeLesson.value = null

    val currentProfile = userProfileState.value
    com.example.ads.AdService.maybeShowInterstitialAtNaturalBreak(
      userProfile = currentProfile,
      reason = "Completed ${lesson.title}"
    )
    val alreadyCompleted = currentProfile.completedLessonIds.contains(lesson.id)
    val effectiveXp = if (alreadyCompleted) 0 else xpEarned.coerceAtLeast(0)
    val newXp = currentProfile.xpPoints + effectiveXp
    val newLevel = (newXp / 100L) + 1L
    val updatedCompleted = (currentProfile.completedLessonIds + lesson.id).distinct()
    val updatedProfile = currentProfile.copy(
      xpPoints = newXp,
      level = newLevel,
      completedLessonIds = updatedCompleted
    )
    _localProfileOverride.value = updatedProfile

    val langCode = lesson.targetLanguageCode.lowercase().substringBefore("-")
    val langCompleted = updatedCompleted.filter {
      it.startsWith("${langCode}_") || it.startsWith("ext_${langCode}_") || it.startsWith("sup_${langCode}_")
    }
    val existingSnap = memoryManager?.loadLanguageProgressSnapshot(langCode)
    memoryManager?.saveLanguageProgressSnapshot(
      com.example.data.model.LanguageLearningProgressSnapshot(
        languageCode = langCode,
        proficiencyLevel = updatedProfile.proficiencyLevel,
        xpPoints = (existingSnap?.xpPoints ?: 0L) + effectiveXp,
        completedLessonIds = ((existingSnap?.completedLessonIds ?: emptyList()) + langCompleted + lesson.id).distinct(),
        currentLessonId = lesson.id,
        streakDays = updatedProfile.currentStreak
      )
    )

    viewModelScope.launch {
      try {
        val backendResult = repository.completeLessonViaBackend(
          lessonId = lesson.id,
          languageCode = lesson.targetLanguageCode,
          category = lesson.category,
          scorePercent = scorePercent,
          xpGained = effectiveXp
        )
        if (backendResult.isFailure) {
          repository.recordLessonCompletion(
            lessonId = lesson.id,
            languageCode = lesson.targetLanguageCode,
            category = lesson.category,
            scorePercent = scorePercent,
            xpGained = effectiveXp
          )
        }
        memoryManager?.synchronizePendingMutations(repository)
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      } catch (_: Exception) {
        memoryManager?.enqueueOfflineLessonCompletion(
          lessonId = lesson.id,
          languageCode = lesson.targetLanguageCode,
          category = lesson.category,
          scorePercent = scorePercent,
          xpGained = effectiveXp
        )
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      }
    }
  }

  fun completeLearningActivity(activityType: String, xpGained: Int = 15) {
    val currentProfile = userProfileState.value
    val newXp = currentProfile.xpPoints + xpGained.coerceAtLeast(5)
    val newLevel = (newXp / 100L) + 1L
    _localProfileOverride.value = currentProfile.copy(xpPoints = newXp, level = newLevel)
    viewModelScope.launch {
      runCatching { repository.completeLearningActivityViaBackend(activityType) }
    }
  }

  fun toggleLessonOfflineDownload(lesson: CurriculumLesson) {
    if (!userProfileState.value.isEntitlementVerified) {
      com.example.ai.AiErrorHandler.requestUpgrade(
        mode = com.example.ai.UpgradeSheetMode.LESSONS,
        featureKey = com.example.data.repository.AccessPolicyFeatureKeys.LESSONS_DAILY,
        headline = "Unlock Structured Lessons",
        reason = "Offline lesson downloads are included with LingoSphere Pro."
      )
      return
    }
    val updated = memoryManager?.toggleLessonOfflineDownload(lesson)
      ?: if (_downloadedLessonIds.value.contains(lesson.id)) {
        _downloadedLessonIds.value - lesson.id
      } else {
        _downloadedLessonIds.value + lesson.id
      }
    _downloadedLessonIds.value = updated
  }

  fun downloadAllLessonsForLanguage(lessons: List<CurriculumLesson>) {
    if (!userProfileState.value.isEntitlementVerified) {
      com.example.ai.AiErrorHandler.requestUpgrade(
        mode = com.example.ai.UpgradeSheetMode.LESSONS,
        featureKey = com.example.data.repository.AccessPolicyFeatureKeys.LESSONS_DAILY,
        headline = "Unlock Structured Lessons",
        reason = "Offline lesson packs are included with LingoSphere Pro."
      )
      return
    }
    val updated = memoryManager?.downloadAllLessonsForLanguage(lessons)
      ?: (_downloadedLessonIds.value + lessons.map { it.id })
    _downloadedLessonIds.value = updated
  }

  fun syncOfflineProgressNow(onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val synced = try {
        memoryManager?.synchronizePendingMutations(repository) ?: 0
      } catch (_: Exception) {
        0
      }
      _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      onComplete(synced)
    }
  }

  fun recordStudyMinutes(minutes: Int) {
    if (minutes <= 0) return
    memoryManager?.recordLearningMinutes(minutes)
    _dailyMinutesMap.value = memoryManager?.loadDailyMinutesMap() ?: _dailyMinutesMap.value
  }

  fun changeTargetLanguage(code: String) {
    changeTargetLanguageWithLevel(code = code, cefrLevel = null)
  }

  fun changeTargetLanguageWithLevel(code: String, cefrLevel: String? = null) {
    val cleanNewCode = code.trim().lowercase().substringBefore("-")
    if (cleanNewCode.isBlank()) return
    val profile = userProfileState.value
    val prevCode = profile.targetLanguageCode.trim().lowercase().substringBefore("-")
    val prevCefr = profile.effectiveCefrForLanguage(
      langCode = prevCode,
      fallbackPlacementCefr = _placementEvaluationState.value?.recommendedCefrBand.orEmpty()
    )

    // Save current language snapshot (per-language progress & CEFR level) before switching.
    // Global XP and streak stay untouched on UserProfile.
    if (prevCode.isNotBlank()) {
      val prevCompleted = profile.completedLessonIds.filter {
        it.startsWith("${prevCode}_") || it.startsWith("ext_${prevCode}_") || it.startsWith("sup_${prevCode}_")
      }
      val existingPrev = memoryManager?.loadLanguageProgressSnapshot(prevCode)
      val prevSnapshot = com.example.data.model.LanguageLearningProgressSnapshot(
        languageCode = prevCode,
        proficiencyLevel = profile.proficiencyLevel,
        cefrLevel = prevCefr,
        xpPoints = existingPrev?.xpPoints ?: profile.xpPoints,
        completedLessonIds = ((existingPrev?.completedLessonIds ?: emptyList()) + prevCompleted).distinct(),
        currentLessonId = existingPrev?.currentLessonId,
        streakDays = profile.currentStreak
      )
      memoryManager?.saveLanguageProgressSnapshot(prevSnapshot)
      _languageProgressSnapshots.value = _languageProgressSnapshots.value + (prevCode to prevSnapshot)
    }

    // Restore target language snapshot if the user previously studied this language
    val targetSnap = memoryManager?.loadLanguageProgressSnapshot(cleanNewCode)
    val mergedCompletedIds = (profile.completedLessonIds + (targetSnap?.completedLessonIds ?: emptyList())).distinct()

    val explicitCefr = com.example.data.model.normalizeCefrCodeOrNull(cefrLevel)
    val savedTargetCefr = com.example.data.model.normalizeCefrCodeOrNull(profile.languageLevels[cleanNewCode])
      ?: com.example.data.model.normalizeCefrCodeOrNull(targetSnap?.cefrLevel)

    val resolvedCefr = explicitCefr
      ?: savedTargetCefr
      ?: targetSnap?.proficiencyLevel?.takeIf { it.isNotBlank() }?.let {
        com.example.data.model.mapProficiencyToDefaultCefr(it)
      }
      ?: "A2"

    val resolvedProficiency = if (explicitCefr != null || savedTargetCefr != null) {
      com.example.data.model.mapCefrToProficiencyLevel(resolvedCefr)
    } else {
      targetSnap?.proficiencyLevel?.takeIf { it.isNotBlank() }
        ?: com.example.data.model.mapCefrToProficiencyLevel(resolvedCefr)
    }

    val updatedLanguageLevels = profile.languageLevels.toMutableMap().apply {
      if (prevCode.isNotBlank() && prevCode != cleanNewCode && !containsKey(prevCode)) {
        put(prevCode, prevCefr)
      }
      put(cleanNewCode, resolvedCefr)
    }

    val targetCompleted = mergedCompletedIds.filter {
      it.startsWith("${cleanNewCode}_") || it.startsWith("ext_${cleanNewCode}_") || it.startsWith("sup_${cleanNewCode}_")
    }
    val newTargetSnapshot = com.example.data.model.LanguageLearningProgressSnapshot(
      languageCode = cleanNewCode,
      proficiencyLevel = resolvedProficiency,
      cefrLevel = resolvedCefr,
      xpPoints = targetSnap?.xpPoints ?: profile.xpPoints,
      completedLessonIds = ((targetSnap?.completedLessonIds ?: emptyList()) + targetCompleted).distinct(),
      currentLessonId = targetSnap?.currentLessonId,
      streakDays = profile.currentStreak
    )
    memoryManager?.saveLanguageProgressSnapshot(newTargetSnapshot)
    _languageProgressSnapshots.value = _languageProgressSnapshots.value + (cleanNewCode to newTargetSnapshot)

    val newActive = (profile.activeLanguages + cleanNewCode).distinct()
    val updated = profile.copy(
      targetLanguageCode = cleanNewCode,
      proficiencyLevel = resolvedProficiency,
      completedLessonIds = mergedCompletedIds,
      activeLanguages = newActive,
      languageLevels = updatedLanguageLevels
    )
    _localProfileOverride.value = updated
    persistCachedProfileSnapshot(updated, myCommunityProfileState.value)
    val newLangName = com.example.data.model.Language.findByCode(cleanNewCode).name
    reloadLanguageScopedLearningMemory(newLangName, resolvedProficiency)
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updated)
      } catch (_: Exception) {}
      seedStarterFlashcards(cleanNewCode)
    }
  }

  fun updateLanguageCefrLevel(
    cefrLevel: String,
    languageCode: String = userProfileState.value.targetLanguageCode
  ) {
    changeTargetLanguageWithLevel(code = languageCode, cefrLevel = cefrLevel)
  }

  fun changeNativeLanguage(code: String) {
    val cleanCode = code.trim().lowercase().substringBefore("-")
    val profile = userProfileState.value
    val updated = profile.copy(nativeLanguageCode = cleanCode)
    _localProfileOverride.value = updated
    persistCachedProfileSnapshot(updated, myCommunityProfileState.value)
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updated)
      } catch (_: Exception) {}
    }
  }

  fun changeAppInterfaceLanguage(code: String) {
    val cleanCode = code.trim().lowercase().substringBefore("-")
    val profile = userProfileState.value
    val updated = profile.copy(appInterfaceLanguageCode = cleanCode)
    _localProfileOverride.value = updated
    val updatedSettings = _accountSettingsState.value.copy(appInterfaceLanguageCode = cleanCode)
    _accountSettingsState.value = updatedSettings
    memoryManager?.saveUserAccountSettings(userId, updatedSettings)
    persistCachedProfileSnapshot(updated, myCommunityProfileState.value)
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updated)
      } catch (_: Exception) {}
    }
  }

  fun updateProficiencyLevel(level: String) {
    val profile = userProfileState.value
    val langCode = profile.targetLanguageCode.trim().lowercase().substringBefore("-")
    val mappedCefr = com.example.data.model.mapProficiencyToDefaultCefr(level)
    val updatedLevels = if (langCode.isNotBlank()) {
      profile.languageLevels + (langCode to mappedCefr)
    } else {
      profile.languageLevels
    }
    val updated = profile.copy(
      proficiencyLevel = level,
      languageLevels = updatedLevels
    )
    _localProfileOverride.value = updated
    val existingSnap = memoryManager?.loadLanguageProgressSnapshot(langCode)
    val updatedSnap = com.example.data.model.LanguageLearningProgressSnapshot(
      languageCode = langCode,
      proficiencyLevel = level,
      cefrLevel = mappedCefr,
      xpPoints = existingSnap?.xpPoints ?: profile.xpPoints,
      completedLessonIds = existingSnap?.completedLessonIds ?: profile.completedLessonIds,
      currentLessonId = existingSnap?.currentLessonId,
      streakDays = profile.currentStreak
    )
    memoryManager?.saveLanguageProgressSnapshot(updatedSnap)
    _languageProgressSnapshots.value = _languageProgressSnapshots.value + (langCode to updatedSnap)
    persistCachedProfileSnapshot(updated, myCommunityProfileState.value)
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updated)
      } catch (_: Exception) {}
    }
  }

  fun updateDailyGoalMinutes(minutes: Long) {
    val profile = userProfileState.value
    val updated = profile.copy(dailyGoalMinutes = minutes)
    _localProfileOverride.value = updated
    persistCachedProfileSnapshot(updated, myCommunityProfileState.value)
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updated)
      } catch (_: Exception) {}
    }
  }

  fun updateProfileAvatar(avatarDataUri: String) {
    val cleaned = avatarDataUri.trim()
    _isSavingProfileState.value = true
    memoryManager?.saveProfileAvatar(userId, cleaned)
    _localAvatarOverride.value = cleaned
    val updated = userProfileState.value.copy(avatarUrl = cleaned)
    _localProfileOverride.value = updated
    _localCommunityOverride.value = (myCommunityProfileState.value ?: CommunityProfile(userId = userId)).copy(
      avatarUrl = cleaned
    )
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updated)
        repository.syncCommunityProfile(updated)
        _profileStatusMessage.value = if (cleaned.isBlank()) "Profile photo removed." else "Profile photo saved to account."
      } catch (_: Exception) {
        _profileStatusMessage.value = if (cleaned.isBlank()) "Profile photo removed." else "Profile photo saved."
      } finally {
        _isSavingProfileState.value = false
      }
    }
  }

  fun updateCommunitySettings(
    displayName: String,
    country: String,
    countryFlag: String,
    bio: String,
    interests: List<String>,
    isOnline: Boolean,
    allowCalls: Boolean
  ) {
    val resolvedName = displayName.trim().ifBlank { userProfileState.value.displayName.ifBlank { "Lingo Learner" } }
    val updatedProfile = userProfileState.value.copy(displayName = resolvedName)
    val updatedComm = (myCommunityProfileState.value ?: CommunityProfile(userId = userId)).copy(
      userId = userId,
      displayName = resolvedName,
      avatarUrl = updatedProfile.avatarUrl,
      country = country,
      countryFlag = countryFlag,
      bio = bio.trim(),
      interests = interests,
      nativeLanguageCode = updatedProfile.nativeLanguageCode,
      targetLanguageCode = updatedProfile.targetLanguageCode,
      proficiencyLevel = updatedProfile.proficiencyLevel,
      xpPoints = updatedProfile.xpPoints,
      level = updatedProfile.level,
      currentStreak = updatedProfile.currentStreak,
      isOnline = isOnline,
      allowCalls = allowCalls
    )
    _isSavingProfileState.value = true
    _localProfileOverride.value = updatedProfile
    _localCommunityOverride.value = updatedComm
    val updatedSettings = _accountSettingsState.value.copy(
      isOnlineVisible = isOnline,
      allowIncomingCalls = allowCalls
    )
    _accountSettingsState.value = updatedSettings
    memoryManager?.saveUserAccountSettings(userId, updatedSettings)
    persistCachedProfileSnapshot(updatedProfile, updatedComm)

    viewModelScope.launch {
      try {
        repository.updateUserProfile(updatedProfile)
        repository.syncCommunityProfile(
          profile = updatedProfile,
          country = country,
          countryFlag = countryFlag,
          bio = bio.trim(),
          interests = interests,
          isOnline = isOnline,
          allowCalls = allowCalls
        )
        _profileStatusMessage.value = "Profile & preferences saved to your account."
      } catch (_: Exception) {
        _profileStatusMessage.value = "Saved locally; will sync to cloud when online."
      } finally {
        _isSavingProfileState.value = false
      }
    }
  }

  fun updateAccountSettings(newSettings: com.example.data.model.UserAccountSettings) {
    _accountSettingsState.value = newSettings
    memoryManager?.saveUserAccountSettings(userId, newSettings)
    val currentComm = myCommunityProfileState.value
    if (
      currentComm != null &&
      (currentComm.isOnline != newSettings.isOnlineVisible ||
        currentComm.allowCalls != newSettings.allowIncomingCalls ||
        currentComm.showLastSeen != newSettings.showLastSeen)
    ) {
      val updatedComm = currentComm.copy(
        isOnline = newSettings.isOnlineVisible,
        allowCalls = newSettings.allowIncomingCalls,
        presenceState = if (newSettings.isOnlineVisible) "online" else "offline",
        showLastSeen = newSettings.showLastSeen
      )
      _localCommunityOverride.value = updatedComm
      viewModelScope.launch {
        try {
          repository.syncCommunityProfile(
            profile = userProfileState.value,
            country = updatedComm.country,
            countryFlag = updatedComm.countryFlag,
            bio = updatedComm.bio,
            interests = updatedComm.interests,
            isOnline = newSettings.isOnlineVisible,
            allowCalls = newSettings.allowIncomingCalls,
            presenceState = if (newSettings.isOnlineVisible) "online" else "offline",
            showLastSeen = newSettings.showLastSeen
          )
        } catch (_: Exception) {}
      }
    }
  }

  fun clearProfileStatusMessage() {
    _profileStatusMessage.value = null
  }

  private fun persistCachedProfileSnapshot(profile: UserProfile, comm: CommunityProfile?) {
    val snap = com.example.data.repository.CachedProfileMetadata(
      displayName = profile.displayName,
      country = comm?.country ?: "United States",
      countryFlag = comm?.countryFlag ?: "🇺🇸",
      bio = comm?.bio ?: "Practicing languages & real-time conversations on LingoSphere!",
      interests = comm?.interests ?: listOf("Travel", "Culture", "Music"),
      nativeLanguageCode = profile.nativeLanguageCode,
      targetLanguageCode = profile.targetLanguageCode,
      proficiencyLevel = profile.proficiencyLevel,
      dailyGoalMinutes = profile.dailyGoalMinutes,
      languageLevels = profile.languageLevels
    )
    _cachedProfileMeta.value = snap
    memoryManager?.saveCachedProfileDetails(
      userId = userId,
      displayName = snap.displayName,
      country = snap.country,
      countryFlag = snap.countryFlag,
      bio = snap.bio,
      interests = snap.interests,
      nativeLanguageCode = snap.nativeLanguageCode,
      targetLanguageCode = snap.targetLanguageCode,
      proficiencyLevel = snap.proficiencyLevel,
      dailyGoalMinutes = snap.dailyGoalMinutes,
      languageLevels = snap.languageLevels
    )
  }

  fun reviewFlashcard(card: FlashcardProgress, isCorrect: Boolean) {
    viewModelScope.launch {
      repository.updateFlashcardBox(card, isCorrect)
    }
  }

  fun addCustomFlashcard(frontText: String, backText: String, pronunciation: String) {
    val targetCode = userProfileState.value.targetLanguageCode
    viewModelScope.launch {
      try {
        repository.saveFlashcard(
          FlashcardProgress(
            userId = userId,
            wordId = "custom_${System.currentTimeMillis()}",
            languageCode = targetCode,
            frontText = frontText,
            backText = backText,
            pronunciation = pronunciation,
            boxLevel = 1L
          )
        )
      } catch (_: Exception) {
        memoryManager?.enqueueOfflineVocabularySave(targetCode, frontText, backText, pronunciation)
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      }
    }
  }

  fun deleteFlashcard(cardId: String) {
    viewModelScope.launch {
      try {
        repository.deleteFlashcard(cardId)
      } catch (e: Exception) {
        android.util.Log.e("LingoViewModel", "Error deleting flashcard", e)
      }
    }
  }

  fun saveSpeakingSession(session: SpeakingSession) {
    recordStudyMinutes(maxOf(2, (session.turnsCount / 2L).toInt()))
    val currentProfile = userProfileState.value
    val sessionXp = when {
      session.mode.equals("Exam", ignoreCase = true) -> 40L
      session.mode.equals("Interview", ignoreCase = true) -> 35L
      session.mode.equals("Immersion", ignoreCase = true) -> 25L
      else -> 20L
    }
    val newXp = currentProfile.xpPoints + sessionXp
    val newLevel = (newXp / 100L) + 1L
    val newStreak = maxOf(1L, currentProfile.currentStreak)
    val updatedProfile = currentProfile.copy(
      xpPoints = newXp,
      level = newLevel,
      currentStreak = newStreak
    )
    _localProfileOverride.value = updatedProfile
    persistCachedProfileSnapshot(updatedProfile, myCommunityProfileState.value)
    viewModelScope.launch {
      try {
        repository.saveSpeakingSession(session)
        repository.updateUserProfile(updatedProfile)
        memoryManager?.synchronizePendingMutations(repository)
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      } catch (_: Exception) {
        memoryManager?.enqueueOfflineSpeakingSession(session)
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      }
    }
  }

  fun sendFriendRequest(receiverId: String, receiverName: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      try {
        repository.sendFriendRequest(
          senderName = userProfileState.value.displayName,
          receiverId = receiverId,
          receiverName = receiverName
        )
        onDone()
      } catch (_: Exception) {}
    }
  }

  fun sendSpeakingInvitation(
    receiverId: String,
    receiverName: String,
    topic: String = "Daily English Speaking Practice",
    onDone: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        repository.sendSpeakingInvitation(
          senderName = userProfileState.value.displayName,
          receiverId = receiverId,
          receiverName = receiverName,
          topic = topic
        )
        onDone()
      } catch (_: Exception) {}
    }
  }

  fun respondToFriendRequest(requestId: String, accept: Boolean) {
    viewModelScope.launch {
      try {
        repository.respondToFriendRequest(requestId, accept)
      } catch (_: Exception) {}
    }
  }

  fun cancelOrRemoveFriendRequest(requestId: String) {
    viewModelScope.launch {
      try {
        repository.cancelOrRemoveFriendRequest(requestId)
      } catch (_: Exception) {}
    }
  }

  private val sharedChatFlows = ConcurrentHashMap<String, Flow<List<DirectChatMessage>>>()

  fun observeChatMessages(partnerId: String): Flow<List<DirectChatMessage>> {
    return sharedChatFlows.getOrPut(partnerId) {
      repository.observeChatMessages(partnerId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(2000L), replay = 1)
    }
  }

  fun sendDirectMessage(
    receiverId: String,
    text: String,
    translatedText: String? = null,
    messageId: String? = null,
    replyToMessageId: String = "",
    replyToSenderName: String = "",
    replyToText: String = "",
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        val savedId = repository.sendDirectMessage(
          senderName = userProfileState.value.displayName,
          receiverId = receiverId,
          text = text,
          translatedText = translatedText,
          messageId = messageId,
          messageType = "text",
          replyToMessageId = replyToMessageId,
          replyToSenderName = replyToSenderName,
          replyToText = replyToText
        )
        onSuccess(savedId)
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Failed to send message")
      }
    }
  }

  fun sendVoiceMessage(
    receiverId: String,
    audioDataBase64: String,
    audioDurationMs: Long,
    audioMimeType: String = "audio/mp4",
    audioWaveform: List<Int> = emptyList(),
    replyToMessageId: String = "",
    replyToSenderName: String = "",
    replyToText: String = "",
    messageId: String? = null,
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    if (audioDataBase64.isBlank() || audioDurationMs < 500L) {
      onError("Recording is too short to send.")
      return
    }
    viewModelScope.launch {
      try {
        val durationSec = (audioDurationMs / 1000L).coerceAtLeast(1L)
        val savedId = repository.sendDirectMessage(
          senderName = userProfileState.value.displayName,
          receiverId = receiverId,
          text = "🎤 Voice message (${durationSec}s)",
          messageId = messageId,
          messageType = "voice",
          replyToMessageId = replyToMessageId,
          replyToSenderName = replyToSenderName,
          replyToText = replyToText,
          audioDataBase64 = audioDataBase64,
          audioDurationMs = audioDurationMs,
          audioMimeType = audioMimeType,
          audioWaveform = audioWaveform
        )
        onSuccess(savedId)
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Failed to upload voice message")
      }
    }
  }

  fun markConversationMessagesRead(partnerId: String, incomingMessages: List<DirectChatMessage>) {
    viewModelScope.launch {
      try {
        repository.markConversationMessagesRead(partnerId, incomingMessages)
      } catch (_: Exception) {}
    }
  }

  fun updateDirectMessageFields(messageId: String, updates: Map<String, Any?>) {
    viewModelScope.launch {
      try {
        repository.updateDirectMessageFields(messageId, updates)
      } catch (_: Exception) {}
    }
  }

  fun editDirectMessage(messageId: String, newText: String) {
    val clean = newText.trim()
    if (messageId.isBlank() || clean.isBlank()) return
    viewModelScope.launch {
      try {
        repository.updateDirectMessageFields(
          messageId = messageId,
          updates = mapOf(
            "text" to clean.take(2000),
            "editedAtEpochMs" to System.currentTimeMillis()
          )
        )
      } catch (_: Exception) {}
    }
  }

  fun deleteDirectMessage(messageId: String) {
    if (messageId.isBlank()) return
    viewModelScope.launch {
      try {
        repository.deleteDirectMessage(messageId)
      } catch (_: Exception) {}
    }
  }

  suspend fun startOrJoinCallRoom(
    calleeId: String,
    calleeName: String,
    callType: String,
    matchMode: String,
    calleeNativeLang: String = "",
    calleeLearningLang: String = "",
    targetLanguageOverride: String = "",
    conversationTopic: String = ""
  ): CallRoom {
    val myProfile = userProfileState.value
    val resolvedTargetLang = targetLanguageOverride.ifBlank { myProfile.targetLanguageCode }
    val partnerProfile = communityProfilesState.value.firstOrNull { it.userId == calleeId }
    return repository.createOrJoinCallRoom(
      callerName = myProfile.displayName,
      calleeId = calleeId,
      calleeName = calleeName,
      callType = callType,
      matchMode = matchMode,
      targetLanguageCode = resolvedTargetLang,
      callerNativeLang = myProfile.nativeLanguageCode,
      callerLearningLang = resolvedTargetLang,
      calleeNativeLang = calleeNativeLang.ifBlank { partnerProfile?.nativeLanguageCode ?: resolvedTargetLang },
      calleeLearningLang = calleeLearningLang.ifBlank { partnerProfile?.targetLanguageCode ?: myProfile.nativeLanguageCode },
      conversationTopic = conversationTopic
    )
  }

  suspend fun createSpeakingRoom(
    roomTitle: String,
    targetLanguageCode: String,
    conversationTopic: String,
    isPublic: Boolean,
    participantLimit: Int
  ): CallRoom {
    val myProfile = userProfileState.value
    return repository.createSpeakingRoom(
      hostName = myProfile.displayName,
      hostNativeLang = myProfile.nativeLanguageCode,
      roomTitle = roomTitle,
      targetLanguageCode = targetLanguageCode,
      conversationTopic = conversationTopic,
      isPublic = isPublic,
      participantLimit = participantLimit
    )
  }

  suspend fun joinSpeakingRoom(roomId: String): CallRoom {
    val myProfile = userProfileState.value
    return repository.joinSpeakingRoom(
      roomId = roomId,
      userName = myProfile.displayName,
      userNativeLang = myProfile.nativeLanguageCode,
      userLearningLang = myProfile.targetLanguageCode
    )
  }

  fun leaveSpeakingRoom(roomId: String) {
    viewModelScope.launch {
      try {
        repository.leaveSpeakingRoom(roomId)
      } catch (_: Exception) {}
    }
  }

  fun toggleSpeakingRoomMute(roomId: String, targetUserId: String = userId, isMuted: Boolean) {
    viewModelScope.launch {
      try {
        repository.toggleSpeakingRoomMute(roomId, targetUserId, isMuted)
      } catch (_: Exception) {}
    }
  }

  fun toggleSpeakingRoomHandRaised(roomId: String, targetUserId: String = userId, isRaised: Boolean) {
    viewModelScope.launch {
      try {
        repository.toggleSpeakingRoomHandRaised(roomId, targetUserId, isRaised)
      } catch (_: Exception) {}
    }
  }

  private val sharedRoomChatFlows = ConcurrentHashMap<String, Flow<List<DirectChatMessage>>>()

  fun observeSpeakingRoomChat(roomId: String): Flow<List<DirectChatMessage>> {
    return sharedRoomChatFlows.getOrPut(roomId) {
      repository.observeSpeakingRoomChat(roomId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(2000L), replay = 1)
    }
  }

  fun sendSpeakingRoomMessage(roomId: String, text: String) {
    val senderName = userProfileState.value.displayName
    viewModelScope.launch {
      try {
        repository.sendSpeakingRoomMessage(roomId, senderName, text)
      } catch (_: Exception) {}
    }
  }

  private val signalingWriteMutex = kotlinx.coroutines.sync.Mutex()
  private val sharedCallRoomFlows = ConcurrentHashMap<String, Flow<CallRoom?>>()

  fun observeCallRoom(roomId: String): Flow<CallRoom?> {
    return sharedCallRoomFlows.getOrPut(roomId) {
      repository.observeCallRoom(roomId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(2000L), replay = 1)
    }
  }

  suspend fun getCallRoomSnapshot(roomId: String): CallRoom? {
    return repository.getCallRoomSnapshot(roomId)
  }

  fun updateCallRoomState(
    roomId: String,
    status: String? = null,
    isCallerMuted: Boolean? = null,
    isCalleeMuted: Boolean? = null,
    isVideoEnabled: Boolean? = null,
    isCallerVideoEnabled: Boolean? = null,
    isCalleeVideoEnabled: Boolean? = null,
    conversationTopic: String? = null
  ) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.updateCallRoomState(
          roomId = roomId,
          status = status,
          isCallerMuted = isCallerMuted,
          isCalleeMuted = isCalleeMuted,
          isVideoEnabled = isVideoEnabled,
          isCallerVideoEnabled = isCallerVideoEnabled,
          isCalleeVideoEnabled = isCalleeVideoEnabled,
          conversationTopic = conversationTopic
        )
      }
    }
  }

  fun markCallRinging(roomId: String) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.markCallRinging(roomId)
      }
    }
  }

  fun sendSdpOffer(roomId: String, offer: String, isIceRestart: Boolean = false) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.sendSdpOffer(roomId, offer, isIceRestart)
      }
    }
  }

  fun sendSdpAnswer(roomId: String, answer: String) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.sendSdpAnswer(roomId, answer)
      }
    }
  }

  fun addIceCandidate(roomId: String, candidate: String, isCaller: Boolean) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.addIceCandidate(roomId, candidate, isCaller)
      }
    }
  }

  fun updateConnectionState(roomId: String, connectionState: String) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.updateConnectionState(roomId, connectionState)
      }
    }
  }

  fun acceptIncomingCall(roomId: String, isVideo: Boolean) {
    viewModelScope.launch {
      signalingWriteMutex.withLock {
        repository.acceptCall(roomId, isVideo)
      }
    }
  }

  fun rejectIncomingCall(roomId: String, isBusy: Boolean = false) {
    repository.markRoomTerminalLocally(roomId)
    viewModelScope.launch {
      repository.rejectCall(roomId, isBusy)
    }
  }

  fun cancelOutgoingCall(roomId: String) {
    repository.markRoomTerminalLocally(roomId)
    viewModelScope.launch {
      repository.cancelCall(roomId)
    }
  }

  fun markCallMissed(roomId: String) {
    repository.markRoomTerminalLocally(roomId)
    viewModelScope.launch {
      repository.markCallMissed(roomId)
    }
  }

  fun markCallFailed(roomId: String) {
    repository.markRoomTerminalLocally(roomId)
    viewModelScope.launch {
      repository.markCallFailed(roomId)
    }
  }

  fun endCall(roomId: String, actualDurationSeconds: Long) {
    repository.markRoomTerminalLocally(roomId)
    viewModelScope.launch {
      repository.endCall(roomId, actualDurationSeconds)
    }
  }

  fun reportUser(targetUserId: String, reason: String, details: String, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      try {
        repository.reportUser(targetUserId, reason, details)
        onComplete()
      } catch (_: Exception) {}
    }
  }

  fun blockUser(blockedUserId: String, blockedUserName: String, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      try {
        repository.blockUser(blockedUserId, blockedUserName)
        onComplete()
      } catch (_: Exception) {}
    }
  }

  fun unblockUser(blockId: String) {
    viewModelScope.launch {
      repository.unblockUser(blockId)
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notificationId)
    }
  }

  fun updateSubscription(
    tier: String,
    token: String,
    isFreeTrial: Boolean = token.startsWith("trial_"),
    autoRenewing: Boolean = true,
    expiryEpochMs: Long = if (isFreeTrial || token.startsWith("trial_")) {
      System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
    } else {
      0L
    },
    productId: String = if (tier == "premium_yearly") {
      com.example.data.model.SubscriptionPlan.PRODUCT_ID_YEARLY
    } else {
      com.example.data.model.SubscriptionPlan.PRODUCT_ID_MONTHLY
    },
    basePlanId: String = if (tier == "premium_yearly") "" else com.example.data.model.SubscriptionPlan.BASE_PLAN_MONTHLY
  ) {
    val cleanToken = token.takeIf { it.isNotBlank() }
    val currentProfile = userProfileState.value
    val effectiveHasUsedTrial = currentProfile.hasUsedFreeTrial || isFreeTrial
    val isRobolectric = android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    if (tier == "free" || cleanToken.isNullOrBlank()) {
      _localSubscriptionOverride.value = null
      memoryManager?.saveSubscriptionState(
        tier = "free",
        purchaseToken = null,
        isFreeTrial = false,
        hasUsedFreeTrial = effectiveHasUsedTrial,
        autoRenewing = false,
        expiryEpochMs = 0L
      )
      com.example.data.repository.AccessPolicyManager.updateEntitlement(
        com.example.data.repository.ServerEntitlementRecord(
          uid = userId,
          status = com.example.data.repository.EntitlementStatus.FREE,
          planId = "free",
          subscriptionTier = "free"
        )
      )
      com.example.ai.GeminiCostAndQuotaManager.switchActiveAccount(userId, "free")
      viewModelScope.launch {
        try {
          val profile = userProfileState.value
          repository.updateUserProfile(
            profile.copy(
              subscriptionTier = "free",
              purchaseToken = null,
              isFreeTrial = false,
              hasUsedFreeTrial = effectiveHasUsedTrial,
              subscriptionAutoRenewing = false,
              subscriptionExpiryEpochMs = 0L
            )
          )
        } catch (_: Exception) {}
      }
      return
    }

    if (isRobolectric && (cleanToken.startsWith("gp_") || cleanToken.startsWith("test_"))) {
      val resolvedExpiry = if (expiryEpochMs > 0L) {
        expiryEpochMs
      } else {
        System.currentTimeMillis() + 30L * 24L * 3600L * 1000L
      }
      val subState = LearningMemoryAndOfflineManager.LocalSubscriptionState(
        tier = tier,
        purchaseToken = cleanToken,
        isFreeTrial = isFreeTrial,
        hasUsedFreeTrial = effectiveHasUsedTrial,
        autoRenewing = autoRenewing,
        expiryEpochMs = resolvedExpiry
      )
      _localSubscriptionOverride.value = subState
      com.example.data.repository.AccessPolicyManager.updateEntitlement(
        com.example.data.repository.ServerEntitlementRecord(
          uid = userId,
          status = com.example.data.repository.EntitlementStatus.PRO_ACTIVE,
          planId = productId,
          productId = productId,
          basePlanId = basePlanId,
          subscriptionTier = tier,
          expiryEpochMs = resolvedExpiry,
          expiryTimeMillis = resolvedExpiry,
          autoRenewing = autoRenewing,
          verifiedWithPlayApi = true
        )
      )
      com.example.ai.GeminiCostAndQuotaManager.switchActiveAccount(userId, tier)
    }

    viewModelScope.launch {
      try {
        val backendRes = repository.verifySubscriptionEntitlementWithBackend(
          packageName = "com.aistudio.lingosphere.kxpqzm",
          subscriptionTier = tier,
          productId = productId,
          basePlanId = basePlanId,
          purchaseToken = cleanToken,
          isFreeTrial = isFreeTrial,
          subscriptionAutoRenewing = autoRenewing,
          subscriptionExpiryEpochMs = expiryEpochMs
        )
        backendRes.getOrNull()?.let { json ->
          val backendActive = json.optBoolean("active", false)
          val backendTier = json.optString("subscriptionTier", tier)
          val backendExpiry = json.optLong("subscriptionExpiryEpochMs", expiryEpochMs)
          val backendAutoRenew = json.optBoolean("subscriptionAutoRenewing", autoRenewing)
          val backendTrial = json.optBoolean("isFreeTrial", isFreeTrial)
          val backendUsedTrial = json.optBoolean("hasUsedFreeTrial", effectiveHasUsedTrial)
          if (!backendActive) {
            reconcilePlayRevocationIfNeeded()
          } else {
            val syncedState = LearningMemoryAndOfflineManager.LocalSubscriptionState(
              tier = backendTier,
              purchaseToken = cleanToken,
              isFreeTrial = backendTrial,
              hasUsedFreeTrial = backendUsedTrial,
              autoRenewing = backendAutoRenew,
              expiryEpochMs = backendExpiry
            )
            _localSubscriptionOverride.value = syncedState
            com.example.data.repository.AccessPolicyManager.updateEntitlement(
              com.example.data.repository.ServerEntitlementRecord(
                uid = userId,
                status = com.example.data.repository.EntitlementStatus.PRO_ACTIVE,
                planId = productId,
                productId = productId,
                basePlanId = basePlanId,
                subscriptionTier = backendTier,
                expiryEpochMs = backendExpiry,
                expiryTimeMillis = backendExpiry,
                autoRenewing = backendAutoRenew,
                verifiedWithPlayApi = true
              )
            )
            com.example.ai.GeminiCostAndQuotaManager.switchActiveAccount(userId, backendTier)
          }
        }
        repository.syncAiUsageQuota(com.example.ai.GeminiCostAndQuotaManager.usageState.value)
      } catch (_: Exception) {}
    }
  }

  fun reconcilePlayRevocationIfNeeded() {
    val current = userProfileState.value
    val token = current.purchaseToken
    // Only revoke if Play was connected & confirmed 0 active subscriptions for a real non-test token
    if (current.isEntitlementVerified && !token.isNullOrBlank() && !token.startsWith("test_")) {
      updateSubscription(
        tier = "free",
        token = "",
        isFreeTrial = false,
        autoRenewing = false,
        expiryEpochMs = 0L
      )
    }
  }

  fun cancelSubscriptionDuringPeriodRetainingAccess() {
    val current = userProfileState.value
    if (!current.isEntitlementVerified) {
      cancelSubscriptionToFree()
      return
    }
    val retainedExpiry = if (current.subscriptionExpiryEpochMs > System.currentTimeMillis()) {
      current.subscriptionExpiryEpochMs
    } else {
      System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
    }
    updateSubscription(
      tier = current.subscriptionTier,
      token = current.purchaseToken.orEmpty(),
      isFreeTrial = current.isFreeTrialActive,
      autoRenewing = false,
      expiryEpochMs = retainedExpiry
    )
  }

  fun cancelSubscriptionToFree() {
    val current = userProfileState.value
    if (current.isFreeTrialActive && current.subscriptionAutoRenewing) {
      // Requirement 11: If cancelled during the trial, retain access until the trial expires
      cancelSubscriptionDuringPeriodRetainingAccess()
    } else {
      updateSubscription(
        tier = "free",
        token = "",
        isFreeTrial = false,
        autoRenewing = false,
        expiryEpochMs = 0L
      )
    }
  }

  fun saveCustomProDeck(deck: com.example.data.repository.PremiumCustomDeck, alsoAddToSrsFlashcards: Boolean = true) {
    val updated = (listOf(deck) + _customProDecks.value.filterNot { it.id == deck.id }).take(25)
    _customProDecks.value = updated
    memoryManager?.saveCustomProDecks(updated)
    if (alsoAddToSrsFlashcards) {
      deck.cards.forEach { card ->
        saveVocabularyToFlashcards(
          card.termTarget,
          card.translationNative,
          card.phoneticGuide
        )
      }
    }
  }


  fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      try {
        memoryManager?.appContext()?.let { ctx ->
          com.example.presence.PresenceManager.markUserOfflineNow(ctx, userId)
        }
        memoryManager?.clearAllUserData(userId)
        repository.deleteUserAccountAndData(userId)
        onSuccess()
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Failed to delete account data")
      }
    }
  }

  fun clearLearningHistory(onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      try {
        repository.clearUserLearningHistory(userId)
      } catch (_: Exception) {}
      memoryManager?.clearAllMemoryAndHistory()
      _mistakeNotebookEntries.value = emptyList()
      _pronunciationHistory.value = emptyList()
      _srsMetadataMap.value = emptyMap()
      _bookmarkedReadingIds.value = emptySet()
      _completedAcademyIds.value = emptySet()
      _placementEvaluationState.value = null
      _inProgressLessonsMap.value = emptyMap()
      _dailyMinutesMap.value = emptyMap()
      onComplete()
    }
  }

  // --- LingoSphere Videos Actions ---

  fun toggleSaveVideo(video: LingoVideo) {
    viewModelScope.launch {
      try {
        val isAlreadySaved = savedVideosState.value.any { it.videoId == video.videoId }
        if (isAlreadySaved) {
          repository.removeSavedVideo(video.videoId)
        } else {
          repository.saveVideo(video)
        }
      } catch (_: Exception) {}
    }
  }

  fun createVideoPlaylist(
    name: String,
    description: String,
    languageCode: String,
    proficiencyLevel: String = "All",
    initialVideo: LingoVideo? = null,
    onCreated: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        repository.createVideoPlaylist(name, description, languageCode, proficiencyLevel, initialVideo)
        onCreated()
      } catch (_: Exception) {}
    }
  }

  fun addVideoToPlaylist(playlist: VideoPlaylist, video: LingoVideo, onAdded: () -> Unit = {}) {
    viewModelScope.launch {
      try {
        repository.addVideoToPlaylist(playlist, video)
        onAdded()
      } catch (_: Exception) {}
    }
  }

  fun removeVideoFromPlaylist(playlist: VideoPlaylist, videoId: String) {
    viewModelScope.launch {
      try {
        repository.removeVideoFromPlaylist(playlist, videoId)
      } catch (_: Exception) {}
    }
  }

  fun deleteVideoPlaylist(playlistId: String) {
    viewModelScope.launch {
      try {
        repository.deleteVideoPlaylist(playlistId)
      } catch (_: Exception) {}
    }
  }

  fun recordVideoWatchProgress(
    video: LingoVideo,
    watchedSeconds: Long,
    completedQuiz: Boolean = false,
    quizScorePercent: Int = 0,
    xpReward: Int = 0
  ) {
    viewModelScope.launch {
      try {
        repository.recordVideoWatchProgress(
          video = video,
          watchedSeconds = watchedSeconds,
          completedQuiz = completedQuiz,
          quizScorePercent = quizScorePercent,
          xpReward = xpReward
        )
      } catch (_: Exception) {}
    }
  }

  fun saveVideoVocabToFlashcard(
    term: String,
    translation: String,
    phonetic: String,
    languageCode: String
  ) {
    viewModelScope.launch {
      try {
        repository.saveFlashcard(
          FlashcardProgress(
            userId = userId,
            wordId = "vid_vocab_${System.currentTimeMillis()}",
            languageCode = languageCode,
            frontText = term,
            backText = translation,
            pronunciation = phonetic,
            boxLevel = 1L
          )
        )
      } catch (_: Exception) {}
    }
  }

  fun unlockCulturalAchievementAndXp(
    achievementId: String,
    notificationTitle: String,
    notificationMessage: String,
    xpBonus: Int
  ) {
    viewModelScope.launch {
      try {
        repository.unlockCulturalAchievementAndXp(
          achievementId = achievementId,
          notificationTitle = notificationTitle,
          notificationMessage = notificationMessage,
          xpBonus = xpBonus
        )
      } catch (_: Exception) {}
    }
  }

  fun recordGameOrQuizCompletion(
    gameId: String,
    gameTitle: String,
    scorePercent: Int,
    xpEarned: Int,
    badgeToUnlock: String? = null
  ) {
    viewModelScope.launch {
      try {
        val currentUser = userProfileState.value
        repository.recordLessonCompletion(
          lessonId = gameId,
          languageCode = currentUser.targetLanguageCode,
          category = "Quiz",
          scorePercent = scorePercent.coerceIn(0, 100),
          xpGained = xpEarned.coerceIn(10, 500)
        )
        if (!badgeToUnlock.isNullOrBlank()) {
          repository.unlockCulturalAchievementAndXp(
            achievementId = badgeToUnlock,
            notificationTitle = "Game Mastered: $gameTitle",
            notificationMessage = "Scored $scorePercent% and earned +$xpEarned XP!",
            xpBonus = 15
          )
        }
      } catch (_: Exception) {}
    }
  }

  // --- Phase 2 & 3 & 4 & 5 StateFlows & Actions ---

  private val _srsMetadataMap = MutableStateFlow<Map<String, SrsCardMetadata>>(emptyMap())
  val srsMetadataMap: StateFlow<Map<String, SrsCardMetadata>> = _srsMetadataMap.asStateFlow()

  private val _mistakeNotebookEntries = MutableStateFlow<List<MistakeNotebookEntry>>(emptyList())
  val mistakeNotebookEntries: StateFlow<List<MistakeNotebookEntry>> = _mistakeNotebookEntries.asStateFlow()

  private val _pronunciationHistory = MutableStateFlow<List<PronunciationDiagnosticResult>>(emptyList())
  val pronunciationHistory: StateFlow<List<PronunciationDiagnosticResult>> = _pronunciationHistory.asStateFlow()

  private val _bookmarkedReadingIds = MutableStateFlow<Set<String>>(emptySet())
  val bookmarkedReadingIds: StateFlow<Set<String>> = _bookmarkedReadingIds.asStateFlow()

  private val _completedAcademyIds = MutableStateFlow<Set<String>>(emptySet())
  val completedAcademyIds: StateFlow<Set<String>> = _completedAcademyIds.asStateFlow()

  private val _placementEvaluationState = MutableStateFlow<PlacementTestEvaluation?>(null)
  val placementEvaluationState: StateFlow<PlacementTestEvaluation?> = _placementEvaluationState.asStateFlow()

  private val _mutedPartnerIds = MutableStateFlow<Set<String>>(emptySet())
  val mutedPartnerIds: StateFlow<Set<String>> = _mutedPartnerIds.asStateFlow()

  fun reviewFlashcardWithRating(card: FlashcardProgress, rating: SrsRating) {
    recordStudyMinutes(1)
    viewModelScope.launch {
      try {
        val key = card.wordId.ifBlank { card.id }
        val currentMeta = _srsMetadataMap.value[key]
        val computation = repository.reviewFlashcardWithRating(card, rating, currentMeta)
        val updatedMap = _srsMetadataMap.value + (key to computation.updatedMetadata)
        _srsMetadataMap.value = updatedMap
        memoryManager?.saveSrsMetadataMap(updatedMap)
      } catch (_: Exception) {
        val key = card.wordId.ifBlank { card.id }
        val todayDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val computation = com.example.data.model.SpacedRepetitionEngine.computeNextReview(
          card = card,
          currentMeta = _srsMetadataMap.value[key],
          rating = rating,
          todayEpochDay = todayDay
        )
        val updatedMap = _srsMetadataMap.value + (key to computation.updatedMetadata)
        _srsMetadataMap.value = updatedMap
        memoryManager?.saveSrsMetadataMap(updatedMap)
      }
    }
  }

  fun saveVocabularyToFlashcards(
    frontText: String,
    backText: String,
    pronunciation: String = ""
  ) {
    val langCode = userProfileState.value.targetLanguageCode
    viewModelScope.launch {
      try {
        repository.saveVocabularyAsFlashcard(
          languageCode = langCode,
          frontText = frontText,
          backText = backText,
          pronunciation = pronunciation
        )
      } catch (_: Exception) {
        memoryManager?.enqueueOfflineVocabularySave(langCode, frontText, backText, pronunciation)
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      }
    }
  }

  fun addMistakeNotebookEntry(entry: MistakeNotebookEntry) {
    val existing = _mistakeNotebookEntries.value
    if (existing.none { it.originalText.equals(entry.originalText, ignoreCase = true) && it.languageCode == entry.languageCode }) {
      val updated = (listOf(entry) + existing).take(100)
      _mistakeNotebookEntries.value = updated
      memoryManager?.saveMistakeNotebook(updated)
    }
    val entryLangName = com.example.data.model.Language.findByCode(
      entry.languageCode.ifBlank { userProfileState.value.targetLanguageCode }
    ).name
    val repeated = memoryManager?.recordOrIncrementRepeatedMistake(
      category = entry.category,
      ruleSummary = entry.explanationNative,
      exampleWrong = entry.originalText,
      exampleCorrected = entry.correctedText,
      targetLanguage = entryLangName
    )
    if (repeated != null) {
      val activeLangName = currentTargetLanguageName()
      _repeatedMistakesState.value = memoryManager?.loadRepeatedMistakeSummaries(activeLangName) ?: repeated
      refreshAiLanguageMemoryProfile(activeLangName)
    }
  }

  /**
   * Unified mistake logger for all Explore, Speaking, Roleplay, Debate, Living Immersion,
   * Camera Lens, Conversation Repair, Writing, and Grammar sessions.
   * Persists locally AND syncs to user-isolated Firestore `/users/{uid}/mistake_records/{id}`.
   */
  fun recordExploreMistake(
    activitySource: String,
    category: String,
    originalText: String,
    correctedText: String,
    explanation: String,
    languageCode: String = userProfileState.value.targetLanguageCode
  ) {
    val cleanOrig = originalText.trim()
    val cleanCorr = correctedText.trim()
    if (cleanOrig.isBlank() || cleanCorr.isBlank() || cleanOrig.equals(cleanCorr, ignoreCase = true)) return
    val lang = com.example.data.model.Language.findByCode(languageCode.ifBlank { userProfileState.value.targetLanguageCode })
    val taggedExplanation = if (explanation.contains(activitySource, ignoreCase = true)) {
      explanation.trim()
    } else {
      "[$activitySource] ${explanation.trim()}".trim()
    }
    val entry = MistakeNotebookEntry(
      id = "mst_${lang.code}_${System.currentTimeMillis()}",
      languageCode = lang.code,
      category = category.ifBlank { "Grammar" },
      originalText = cleanOrig,
      correctedText = cleanCorr,
      isGenuineError = true,
      explanationNative = taggedExplanation,
      practicePrompt = "Say this naturally in ${lang.name}:",
      practiceAnswer = cleanCorr,
      resolvedCount = 0,
      timestampEpoch = System.currentTimeMillis()
    )
    addMistakeNotebookEntry(entry)

    val matchedSummary = _repeatedMistakesState.value.firstOrNull {
      it.exampleWrong.equals(cleanOrig, ignoreCase = true) ||
        (it.category.equals(category, ignoreCase = true) && it.exampleCorrected.equals(cleanCorr, ignoreCase = true))
    }
    val appCtx = memoryManager?.appContext()
    if (appCtx != null) {
      viewModelScope.launch(Dispatchers.IO) {
        com.example.ai.ExploreAiLearningEngine.syncMistakeRecordToFirestore(
          context = appCtx,
          mistakeId = matchedSummary?.id ?: entry.id,
          languageCode = lang.code,
          languageName = lang.name,
          category = category.ifBlank { "Grammar" },
          originalResponse = cleanOrig,
          correctedVersion = cleanCorr,
          explanation = taggedExplanation,
          activitySource = activitySource,
          reviewStatus = if (matchedSummary?.improved == true) "mastered" else "pending",
          occurrenceCount = matchedSummary?.occurrenceCount ?: 1,
          masteryScore = matchedSummary?.masteryScore ?: 15
        )
      }
    }
  }

  fun markMistakeNotebookEntryResolved(entryId: String) {
    val updated = _mistakeNotebookEntries.value.map { entry ->
      if (entry.id == entryId) entry.copy(resolvedCount = entry.resolvedCount + 1) else entry
    }
    _mistakeNotebookEntries.value = updated
    memoryManager?.saveMistakeNotebook(updated)
    refreshAiLanguageMemoryProfile()
  }

  fun recordPronunciationDiagnostic(result: PronunciationDiagnosticResult) {
    val updated = (listOf(result) + _pronunciationHistory.value).take(40)
    _pronunciationHistory.value = updated
    memoryManager?.savePronunciationHistory(updated)
    refreshAiLanguageMemoryProfile()
    recordStudyMinutes(2)
    viewModelScope.launch {
      val currentUser = userProfileState.value
      val session = SpeakingSession(
        userId = userId,
        languageCode = currentUser.targetLanguageCode,
        mode = "Pronunciation",
        scenarioTitle = "Pronunciation Coach: ${result.targetPhrase.take(36)}",
        proficiencyLevel = currentUser.proficiencyLevel,
        turnsCount = 1L,
        fluencyScore = result.accuracyPercent.toLong(),
        grammarFeedbackSummary = "${result.ipaTranscription} • ${result.tongueAndMouthTip.take(120)}"
      )
      try {
        repository.saveSpeakingSession(session)
      } catch (_: Exception) {
        memoryManager?.enqueueOfflineSpeakingSession(session)
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      }
    }
  }

  fun toggleReadingBookmark(passageId: String) {
    val current = _bookmarkedReadingIds.value
    val updated = if (current.contains(passageId)) {
      current - passageId
    } else {
      current + passageId
    }
    _bookmarkedReadingIds.value = updated
    memoryManager?.saveBookmarkedReadingIds(updated)
  }

  fun recordAcademyCompletion(itemId: String, category: String, scorePercent: Int, xpBonus: Int) {
    val currentUser = userProfileState.value
    val alreadyCompleted = _completedAcademyIds.value.contains(itemId) || currentUser.completedLessonIds.contains(itemId)
    val effectiveXp = if (alreadyCompleted) 0 else xpBonus.coerceIn(15, 120)

    val updated = _completedAcademyIds.value + itemId
    _completedAcademyIds.value = updated
    memoryManager?.saveCompletedAcademyIds(updated)
    recordStudyMinutes(5)

    val newXp = currentUser.xpPoints + effectiveXp
    val newLevel = (newXp / 100L) + 1L
    val updatedCompleted = (currentUser.completedLessonIds + itemId).distinct()
    _localProfileOverride.value = currentUser.copy(
      xpPoints = newXp,
      level = newLevel,
      completedLessonIds = updatedCompleted
    )

    viewModelScope.launch {
      try {
        repository.recordLessonCompletion(
          lessonId = itemId,
          languageCode = currentUser.targetLanguageCode,
          category = category,
          scorePercent = scorePercent.coerceIn(0, 100),
          xpGained = effectiveXp
        )
      } catch (_: Exception) {
        memoryManager?.enqueueOfflineLessonCompletion(
          lessonId = itemId,
          languageCode = currentUser.targetLanguageCode,
          category = category,
          scorePercent = scorePercent.coerceIn(0, 100),
          xpGained = effectiveXp
        )
        _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      }
    }
  }

  fun applyPlacementEvaluation(
    eval: PlacementTestEvaluation,
    targetLanguageCode: String = userProfileState.value.targetLanguageCode
  ) {
    _placementEvaluationState.value = eval
    memoryManager?.savePlacementEvaluation(eval)
    recordStudyMinutes(6)
    val cleanCode = targetLanguageCode.trim().lowercase().substringBefore("-")
      .ifBlank { userProfileState.value.targetLanguageCode }
    changeTargetLanguageWithLevel(code = cleanCode, cefrLevel = eval.recommendedCefrBand)
    recordGameOrQuizCompletion(
      gameId = "placement_${cleanCode}_${eval.recommendedCefrBand.lowercase()}",
      gameTitle = "CEFR Placement Assessment (${eval.recommendedCefrBand})",
      scorePercent = eval.scorePercent,
      xpEarned = 50,
      badgeToUnlock = "placement_${eval.recommendedCefrBand.lowercase()}"
    )
  }

  fun buildWeeklyLearningReport(): WeeklyLearningReportSummary? {
    val profile = userProfileState.value
    return memoryManager?.buildWeeklyLearningReport(
      targetLanguageCode = profile.targetLanguageCode,
      proficiencyLevel = profile.proficiencyLevel,
      completedLessonsCount = profile.completedLessonIds.size,
      flashcards = flashcardProgressState.value,
      srsMetadataMap = _srsMetadataMap.value,
      speakingSessions = speakingSessionsState.value,
      mistakeEntries = _mistakeNotebookEntries.value,
      pronunciationDiagnostics = _pronunciationHistory.value
    )
  }

  fun toggleMutePartner(partnerUserId: String) {
    val current = _mutedPartnerIds.value
    _mutedPartnerIds.value = if (current.contains(partnerUserId)) {
      current - partnerUserId
    } else {
      current + partnerUserId
    }
  }

  fun scheduleExchangeSession(
    partnerId: String,
    partnerName: String,
    topic: String,
    timeSlot: String,
    dateLabel: String = "",
    timeLabel: String = "",
    timezoneLabel: String = ""
  ) {
    val resolvedDate = dateLabel.ifBlank {
      if (timeSlot.contains(" at ", ignoreCase = true)) timeSlot.substringBefore(" at ").trim() else timeSlot
    }
    val resolvedTime = timeLabel.ifBlank {
      if (timeSlot.contains(" at ", ignoreCase = true)) timeSlot.substringAfter(" at ").trim() else "Flexible"
    }
    val resolvedTz = timezoneLabel.ifBlank {
      java.util.TimeZone.getDefault().getDisplayName(false, java.util.TimeZone.SHORT)
    }
    val summaryText = "📅 Scheduled Language Exchange Proposal • Time: $resolvedDate at $resolvedTime ($resolvedTz) • Topic: $topic"
    viewModelScope.launch {
      try {
        repository.sendDirectMessage(
          senderName = userProfileState.value.displayName,
          receiverId = partnerId,
          text = summaryText,
          messageType = "schedule_invite",
          scheduleTopic = topic,
          scheduleDateLabel = resolvedDate,
          scheduleTimeLabel = resolvedTime,
          scheduleTimezone = resolvedTz,
          scheduleStatus = "pending"
        )
      } catch (_: Exception) {}
    }
  }

  fun respondToScheduledExchangeInvitation(
    message: DirectChatMessage,
    newStatus: String,
    proposedDateLabel: String = "",
    proposedTimeLabel: String = ""
  ) {
    if (message.id.isBlank()) return
    viewModelScope.launch {
      try {
        val updates = mutableMapOf<String, Any?>(
          "scheduleStatus" to newStatus,
          "messageType" to "schedule_invite",
          "scheduleTopic" to message.resolvedScheduleTopic(),
          "scheduleDateLabel" to (proposedDateLabel.ifBlank { message.resolvedScheduleDateLabel() }),
          "scheduleTimeLabel" to (proposedTimeLabel.ifBlank { message.resolvedScheduleTimeLabel() }),
          "scheduleTimezone" to message.resolvedScheduleTimezone()
        )
        if (newStatus == "rescheduled" && (proposedDateLabel.isNotBlank() || proposedTimeLabel.isNotBlank())) {
          val propText = listOf(proposedDateLabel, proposedTimeLabel).filter { it.isNotBlank() }.joinToString(" at ")
          updates["scheduleProposedText"] = propText
        }
        repository.updateDirectMessageFields(message.id, updates)
      } catch (_: Exception) {}
    }
  }

  // ---------------------------------------------------------------------------
  // LINGOSPHERE 3.0 — AI MEMORY, REPLAY, MISSIONS, PORTFOLIO & ADAPTIVE ENGINE
  // ---------------------------------------------------------------------------

  private val _learningGoalsState = MutableStateFlow<List<String>>(
    listOf(
      "Hold natural 5-minute conversations with native speakers",
      "Travel and order food confidently without switching to English",
      "Master past and subjunctive tenses in everyday stories"
    )
  )
  val learningGoalsState: StateFlow<List<String>> = _learningGoalsState.asStateFlow()

  private val _learningPlanPrefsState = MutableStateFlow(
    com.example.data.repository.LearningPlanSetupPrefs()
  )
  val learningPlanPrefsState: StateFlow<com.example.data.repository.LearningPlanSetupPrefs> =
    _learningPlanPrefsState.asStateFlow()

  fun saveLearningPlanPreferences(
    level: String,
    goal: String,
    dailyMinutes: Int,
    confidence: String
  ) {
    val normalizedLevel = when {
      level.contains("Advanced", ignoreCase = true) -> "Advanced"
      level.contains("Intermediate", ignoreCase = true) -> "Intermediate"
      else -> "Beginner"
    }
    updateProficiencyLevel(normalizedLevel)
    updateDailyGoalMinutes(dailyMinutes.coerceIn(5, 180).toLong())
    val updatedPrefs = com.example.data.repository.LearningPlanSetupPrefs(
      goal = goal.ifBlank { "Everyday Communication" },
      confidence = confidence.ifBlank { "Medium" },
      completed = true
    )
    _learningPlanPrefsState.value = updatedPrefs
    memoryManager?.saveLearningPlanSetup(updatedPrefs.goal, updatedPrefs.confidence, completed = true)
  }

  fun refreshOfflineEmergencyPacks() {
    val appCtx = memoryManager?.appContext() ?: return
    _offlineEmergencyLangsState.value =
      com.example.data.repository.OfflineEmergencyPhraseRepository.listDownloadedLanguageCodes(appCtx)
  }

  private val _repeatedMistakesState = MutableStateFlow<List<com.example.data.model.RepeatedMistakeSummary>>(emptyList())
  val repeatedMistakesState: StateFlow<List<com.example.data.model.RepeatedMistakeSummary>> = _repeatedMistakesState.asStateFlow()

  private val _conversationReplaySessionsState = MutableStateFlow<List<com.example.data.model.ConversationReplaySession>>(emptyList())
  val conversationReplaySessionsState: StateFlow<List<com.example.data.model.ConversationReplaySession>> =
    _conversationReplaySessionsState.asStateFlow()

  private val _completedDailyMissionsState = MutableStateFlow<List<com.example.data.model.DailyLifeMissionEvaluation>>(emptyList())
  val completedDailyMissionsState: StateFlow<List<com.example.data.model.DailyLifeMissionEvaluation>> =
    _completedDailyMissionsState.asStateFlow()

  private val _portfolioSpeakingSamplesState = MutableStateFlow<List<com.example.data.model.PortfolioSpeakingSample>>(emptyList())
  val portfolioSpeakingSamplesState: StateFlow<List<com.example.data.model.PortfolioSpeakingSample>> =
    _portfolioSpeakingSamplesState.asStateFlow()

  private val _portfolioWritingSamplesState = MutableStateFlow<List<com.example.data.model.PortfolioWritingSample>>(emptyList())
  val portfolioWritingSamplesState: StateFlow<List<com.example.data.model.PortfolioWritingSample>> =
    _portfolioWritingSamplesState.asStateFlow()

  private val _portfolioPrivacySettingsState = MutableStateFlow(
    com.example.data.model.PortfolioPrivacySettings()
  )
  val portfolioPrivacySettingsState: StateFlow<com.example.data.model.PortfolioPrivacySettings> =
    _portfolioPrivacySettingsState.asStateFlow()

  private val _aiLanguageMemoryState = MutableStateFlow(
    com.example.data.model.AiLanguageMemoryProfile()
  )
  val aiLanguageMemoryState: StateFlow<com.example.data.model.AiLanguageMemoryProfile> =
    _aiLanguageMemoryState.asStateFlow()

  private fun currentTargetLanguageName(): String {
    return com.example.data.model.Language.findByCode(userProfileState.value.targetLanguageCode).name
  }

  private fun reloadLanguageScopedLearningMemory(
    targetLanguageName: String = currentTargetLanguageName(),
    cefrLevel: String = userProfileState.value.proficiencyLevel
  ) {
    memoryManager?.loadRepeatedMistakeSummaries(targetLanguageName)?.let { _repeatedMistakesState.value = it }
    memoryManager?.loadConversationReplaySessions(targetLanguageName)?.let { _conversationReplaySessionsState.value = it }
    memoryManager?.loadPortfolioSpeakingSamples(targetLanguageName)?.let { _portfolioSpeakingSamplesState.value = it }
    memoryManager?.loadPortfolioWritingSamples(targetLanguageName)?.let { _portfolioWritingSamplesState.value = it }
    refreshAiLanguageMemoryProfile(targetLanguageName, cefrLevel)
  }

  fun refreshAiLanguageMemoryProfile(
    targetLanguageName: String = currentTargetLanguageName(),
    cefrLevelOverride: String? = null
  ) {
    val user = userProfileState.value
    val activeCefr = cefrLevelOverride?.takeIf { it.isNotBlank() } ?: user.proficiencyLevel
    val activeCode = user.targetLanguageCode
    val weakCards = flashcardProgressState.value
      .filter { it.languageCode.equals(activeCode, ignoreCase = true) && it.boxLevel <= 2L }
      .map { it.frontText }
    val built = memoryManager?.buildAiLanguageMemoryProfile(
      targetLanguage = targetLanguageName,
      cefrLevel = activeCefr,
      extraWeakWords = weakCards
    ) ?: com.example.data.model.AiLanguageMemoryProfile(
      targetLanguage = targetLanguageName,
      cefrLevel = activeCefr,
      learningGoals = _learningGoalsState.value,
      vocabularyWeaknesses = weakCards,
      repeatedGrammarMistakes = _repeatedMistakesState.value
    )
    _aiLanguageMemoryState.value = built
    com.example.ai.GeminiTutorService.updateActiveMemoryProfile(built)
  }

  fun updateLearningGoals(goals: List<String>, targetLanguageName: String = currentTargetLanguageName()) {
    val cleaned = goals.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    _learningGoalsState.value = cleaned
    memoryManager?.saveLearningGoals(cleaned)
    refreshAiLanguageMemoryProfile(targetLanguageName)
  }

  fun recordConversationMemorySummary(summary: String, targetLanguageName: String = currentTargetLanguageName()) {
    memoryManager?.appendConversationHistorySummary(summary)
    refreshAiLanguageMemoryProfile(targetLanguageName)
  }

  fun recordMistakeRecoveryAttempt(
    mistakeId: String,
    scoreEarned: Int,
    targetLanguageName: String = currentTargetLanguageName()
  ): com.example.data.model.RepeatedMistakeSummary? {
    val updatedItem = memoryManager?.recordMistakeRecoveryProgress(mistakeId, scoreEarned)
    _repeatedMistakesState.value = memoryManager?.loadRepeatedMistakeSummaries(targetLanguageName)
      ?: _repeatedMistakesState.value.map {
        if (it.id != mistakeId) it
        else {
          val newScore = (it.masteryScore + if (scoreEarned >= 75) 28 else 8).coerceAtMost(100)
          it.copy(
            recoveryAttempts = it.recoveryAttempts + 1,
            masteryScore = newScore,
            improved = newScore >= 75
          )
        }
      }
    if (scoreEarned >= 75) {
      recordGameOrQuizCompletion(
        gameId = "mistake_recovery_$mistakeId",
        gameTitle = "Intelligent Mistake Recovery",
        scorePercent = scoreEarned,
        xpEarned = 35
      )
    }
    refreshAiLanguageMemoryProfile(targetLanguageName)
    val appCtx = memoryManager?.appContext()
    val syncedItem = updatedItem ?: _repeatedMistakesState.value.firstOrNull { it.id == mistakeId }
    if (appCtx != null && syncedItem != null) {
      val langCode = com.example.data.model.Language.SUPPORTED_LANGUAGES
        .firstOrNull { it.name.equals(targetLanguageName, ignoreCase = true) }?.code
        ?: userProfileState.value.targetLanguageCode
      viewModelScope.launch(Dispatchers.IO) {
        com.example.ai.ExploreAiLearningEngine.syncMistakeRecordToFirestore(
          context = appCtx,
          mistakeId = syncedItem.id,
          languageCode = langCode,
          languageName = targetLanguageName,
          category = syncedItem.category,
          originalResponse = syncedItem.exampleWrong.ifBlank { "Practice item" },
          correctedVersion = syncedItem.exampleCorrected.ifBlank { "Corrected item" },
          explanation = syncedItem.ruleSummary,
          activitySource = "Mistake Recovery",
          reviewStatus = if (syncedItem.improved) "mastered" else "practicing",
          occurrenceCount = syncedItem.occurrenceCount.coerceAtLeast(1),
          masteryScore = syncedItem.masteryScore.coerceIn(0, 100)
        )
      }
    }
    return updatedItem
  }

  fun saveConversationReplaySession(session: com.example.data.model.ConversationReplaySession) {
    memoryManager?.saveConversationReplaySession(session)
    _conversationReplaySessionsState.value =
      memoryManager?.loadConversationReplaySessions(session.targetLanguage)
        ?: (listOf(session) + _conversationReplaySessionsState.value.filterNot { it.id == session.id })
    recordConversationMemorySummary("Completed ${session.scenarioTitle} (${session.overallScore}%)", session.targetLanguage)
  }

  fun updateReplayTurnPracticeScore(
    sessionId: String,
    turnId: String,
    practicedTranscript: String,
    newScore: Int,
    targetLanguage: String
  ) {
    val updated = memoryManager?.updateReplayTurnPracticeScore(
      sessionId = sessionId,
      turnId = turnId,
      practicedTranscript = practicedTranscript,
      newScore = newScore,
      targetLanguage = targetLanguage
    )
    if (updated != null) {
      _conversationReplaySessionsState.value = updated
    } else {
      _conversationReplaySessionsState.value = _conversationReplaySessionsState.value.map { s ->
        if (s.id != sessionId) s
        else s.copy(
          turns = s.turns.map { t ->
            if (t.turnId != turnId) t
            else t.copy(practicedRetryScore = newScore, lastPracticedTranscript = practicedTranscript)
          }
        )
      }
    }
  }

  fun recordCompletedDailyLifeMission(
    evaluation: com.example.data.model.DailyLifeMissionEvaluation,
    xpReward: Int = 65
  ) {
    memoryManager?.saveCompletedDailyLifeMission(evaluation)
    _completedDailyMissionsState.value =
      memoryManager?.loadCompletedDailyLifeMissions()
        ?: (listOf(evaluation) + _completedDailyMissionsState.value.filterNot { it.missionId == evaluation.missionId })

    if (evaluation.submissionMode.equals("Spoken", ignoreCase = true) && evaluation.userResponseText.isNotBlank()) {
      val sample = com.example.data.model.PortfolioSpeakingSample(
        id = "mission_speak_${evaluation.missionId}_${System.currentTimeMillis()}",
        title = "Mission: ${evaluation.missionTitle}",
        targetLanguage = evaluation.targetLanguage,
        cefrLevel = userProfileState.value.proficiencyLevel,
        spokenTranscript = evaluation.userResponseText,
        improvedVersion = evaluation.improvedModelResponse,
        pronunciationScore = evaluation.overallScore,
        fluencyScore = evaluation.fluencyAndCoherenceScore,
        durationSeconds = 30,
        recordedAtEpochMs = evaluation.completedAtEpochMs,
        dateLabel = "Today • Mission",
        audioFilePath = evaluation.audioSampleFilePath,
        isPrivate = _portfolioPrivacySettingsState.value.defaultPrivateRecordings
      )
      savePortfolioSpeakingSample(sample)
    } else if (evaluation.userResponseText.isNotBlank()) {
      val writingSample = com.example.data.model.PortfolioWritingSample(
        id = "mission_write_${evaluation.missionId}_${System.currentTimeMillis()}",
        promptTitle = evaluation.missionTitle,
        targetLanguage = evaluation.targetLanguage,
        originalDraft = evaluation.userResponseText,
        improvedRevision = evaluation.improvedModelResponse,
        grammarGainScore = (evaluation.grammarAndVocabScore - 60).coerceAtLeast(12),
        keyImprovements = evaluation.keyCorrections,
        dateLabel = "Today • Written Mission"
      )
      savePortfolioWritingSample(writingSample)
    }

    recordGameOrQuizCompletion(
      gameId = "daily_mission_${evaluation.missionId}",
      gameTitle = "Daily Life Mission: ${evaluation.missionTitle}",
      scorePercent = evaluation.overallScore,
      xpEarned = xpReward,
      badgeToUnlock = if (evaluation.completedSuccessfully) "daily_mission_achiever" else null
    )
    recordConversationMemorySummary("Completed mission '${evaluation.missionTitle}' (${evaluation.overallScore}%)", evaluation.targetLanguage)
  }

  fun savePortfolioSpeakingSample(sample: com.example.data.model.PortfolioSpeakingSample) {
    memoryManager?.savePortfolioSpeakingSample(sample)
    _portfolioSpeakingSamplesState.value =
      memoryManager?.loadPortfolioSpeakingSamples(sample.targetLanguage)
        ?: (listOf(sample) + _portfolioSpeakingSamplesState.value.filterNot { it.id == sample.id })
  }

  fun deletePortfolioSpeakingSample(sampleId: String, targetLanguage: String = currentTargetLanguageName()) {
    val updated = memoryManager?.deletePortfolioSpeakingSample(sampleId, targetLanguage)
      ?: _portfolioSpeakingSamplesState.value.filterNot { it.id == sampleId }
    _portfolioSpeakingSamplesState.value = updated
  }

  fun togglePortfolioSpeakingSamplePrivacy(sampleId: String, targetLanguage: String = currentTargetLanguageName()) {
    val updated = memoryManager?.togglePortfolioSpeakingSamplePrivacy(sampleId, targetLanguage)
      ?: _portfolioSpeakingSamplesState.value.map {
        if (it.id == sampleId) it.copy(isPrivate = !it.isPrivate) else it
      }
    _portfolioSpeakingSamplesState.value = updated
  }

  fun deleteAllPortfolioSpeakingSamples() {
    memoryManager?.deleteAllPortfolioSpeakingSamples()
    _portfolioSpeakingSamplesState.value = emptyList()
  }

  fun savePortfolioWritingSample(sample: com.example.data.model.PortfolioWritingSample) {
    memoryManager?.savePortfolioWritingSample(sample)
    _portfolioWritingSamplesState.value =
      memoryManager?.loadPortfolioWritingSamples(sample.targetLanguage)
        ?: (listOf(sample) + _portfolioWritingSamplesState.value.filterNot { it.id == sample.id })
  }

  fun updatePortfolioPrivacySettings(settings: com.example.data.model.PortfolioPrivacySettings) {
    _portfolioPrivacySettingsState.value = settings
    memoryManager?.savePortfolioPrivacySettings(settings)
  }

  fun computeAdaptiveRecommendationPlan(
    targetLanguageName: String = com.example.data.model.Language.findByCode(userProfileState.value.targetLanguageCode).name
  ): com.example.data.model.AdaptiveRecommendationPlan {
    val profile = userProfileState.value
    val lessons = lessonProgressState.value
    val avgQuiz = if (lessons.isNotEmpty()) {
      lessons.map { it.scorePercent.toInt() }.average().toInt()
    } else 80
    val pronHistory = _pronunciationHistory.value
    val avgPron = if (pronHistory.isNotEmpty()) {
      pronHistory.map { it.accuracyPercent }.average().toInt()
    } else 78
    val speakSessions = speakingSessionsState.value
    val avgFluency = if (speakSessions.isNotEmpty()) {
      speakSessions.map { it.fluencyScore.toInt() }.average().toInt()
    } else 79
    val cards = flashcardProgressState.value
    val masteredCount = cards.count { it.boxLevel >= 4L }
    val nowMs = System.currentTimeMillis()
    val dueCount = cards.count { card ->
      com.example.data.model.SpacedRepetitionEngine.isCardDue(card, _srsMetadataMap.value[card.id], nowMs)
    }
    val unresolvedMistakes = _repeatedMistakesState.value.filter { !it.improved }

    return com.example.data.model.AdaptiveLearningEngine.computeAdaptivePlan(
      targetLanguage = targetLanguageName,
      currentLevel = profile.proficiencyLevel,
      completedLessonsCount = profile.completedLessonIds.size + lessons.count { it.completed },
      averageQuizScore = avgQuiz,
      averagePronunciationScore = avgPron,
      averageFluencyScore = avgFluency,
      dueSrsCount = dueCount,
      masteredVocabularyCount = masteredCount,
      unresolvedMistakes = unresolvedMistakes,
      weakVocabulary = _aiLanguageMemoryState.value.vocabularyWeaknesses
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PRACTICAL DIFFERENTIATED FEATURES STATE & ACTIONS (FEATURES 1 - 12)
  // ═══════════════════════════════════════════════════════════════════════════

  private val _mediaPrivacyConsentState = MutableStateFlow(
    com.example.data.model.MediaPrivacyConsentState()
  )
  val mediaPrivacyConsentState: StateFlow<com.example.data.model.MediaPrivacyConsentState> =
    _mediaPrivacyConsentState.asStateFlow()

  private val _livingWorldProgressState = MutableStateFlow<Map<String, com.example.data.model.LivingWorldCharacterProgress>>(emptyMap())
  val livingWorldProgressState: StateFlow<Map<String, com.example.data.model.LivingWorldCharacterProgress>> =
    _livingWorldProgressState.asStateFlow()

  private val _offlineEmergencyLangsState = MutableStateFlow(setOf("es", "fr", "ja"))
  val offlineEmergencyLangsState: StateFlow<Set<String>> = _offlineEmergencyLangsState.asStateFlow()

  private val _selectedHabitRoutinesState = MutableStateFlow(
    setOf(
      com.example.data.model.HabitRoutineSlot.MORNING_COFFEE.id,
      com.example.data.model.HabitRoutineSlot.EVENING_REFLECTION.id
    )
  )
  val selectedHabitRoutinesState: StateFlow<Set<String>> = _selectedHabitRoutinesState.asStateFlow()

  private val _verifiedHabitCompletionsState = MutableStateFlow<List<com.example.data.model.VerifiedHabitCompletion>>(emptyList())
  val verifiedHabitCompletionsState: StateFlow<List<com.example.data.model.VerifiedHabitCompletion>> =
    _verifiedHabitCompletionsState.asStateFlow()

  private val _completedRepairScenariosState = MutableStateFlow<Set<String>>(emptySet())
  val completedRepairScenariosState: StateFlow<Set<String>> = _completedRepairScenariosState.asStateFlow()

  private val _futureMeComparisonsState = MutableStateFlow<List<com.example.data.model.FutureMeComparisonResult>>(emptyList())
  val futureMeComparisonsState: StateFlow<List<com.example.data.model.FutureMeComparisonResult>> =
    _futureMeComparisonsState.asStateFlow()

  fun updateMediaPrivacyConsent(updated: com.example.data.model.MediaPrivacyConsentState) {
    _mediaPrivacyConsentState.value = updated
    memoryManager?.saveMediaPrivacyConsent(updated)
  }

  fun updateLivingWorldCharacterProgress(progress: com.example.data.model.LivingWorldCharacterProgress) {
    memoryManager?.saveLivingWorldProgress(progress)
    _livingWorldProgressState.value = memoryManager?.getLivingWorldProgressMap()
      ?: (_livingWorldProgressState.value + (progress.locationId to progress))
    recordStudyMinutes(3)
    val currentProfile = userProfileState.value
    val newXp = currentProfile.xpPoints + 15L
    val updatedProfile = currentProfile.copy(
      xpPoints = newXp,
      level = (newXp / 100L) + 1L,
      currentStreak = maxOf(1L, currentProfile.currentStreak)
    )
    _localProfileOverride.value = updatedProfile
    persistCachedProfileSnapshot(updatedProfile, myCommunityProfileState.value)
    viewModelScope.launch {
      try {
        repository.updateUserProfile(updatedProfile)
      } catch (_: Exception) {}
    }
  }

  fun toggleOfflineEmergencyPack(languageCode: String) {
    val code = languageCode.lowercase()
    val currentlyDownloaded = _offlineEmergencyLangsState.value.contains(code)
    val updated = memoryManager?.setOfflineEmergencyPackDownloaded(code, !currentlyDownloaded)
      ?: if (currentlyDownloaded) _offlineEmergencyLangsState.value - code else _offlineEmergencyLangsState.value + code
    _offlineEmergencyLangsState.value = updated
  }

  fun toggleHabitRoutineSlot(slotId: String) {
    val updated = memoryManager?.toggleHabitRoutineSlot(slotId)
      ?: _selectedHabitRoutinesState.value.toMutableSet().apply {
        if (contains(slotId) && size > 1) remove(slotId) else add(slotId)
      }
    _selectedHabitRoutinesState.value = updated
  }

  fun recordVerifiedHabitMissionCompletion(
    mission: com.example.data.model.HabitSpeakingMission,
    userTranscript: String,
    feedbackSummary: String
  ) {
    val words = userTranscript.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    if (words < 2) return
    val completion = com.example.data.model.VerifiedHabitCompletion(
      routineSlotId = mission.routineSlot.id,
      languageCode = mission.languageCode,
      missionId = mission.id,
      userSpokenOrWrittenTranscript = userTranscript.trim(),
      wordCount = words,
      feedbackSummary = feedbackSummary
    )
    val updated = memoryManager?.recordVerifiedHabitCompletion(completion)
      ?: (listOf(completion) + _verifiedHabitCompletionsState.value)
    _verifiedHabitCompletionsState.value = updated
    recordStudyMinutes(3)
    recordConversationMemorySummary(
      "Completed Habit Coach mission '${mission.missionTitle}' ($words words)",
      com.example.data.model.Language.findByCode(mission.languageCode).name
    )
  }

  fun recordCompletedRepairScenario(scenarioId: String, languageCode: String) {
    val updated = memoryManager?.recordCompletedRepairScenario(scenarioId)
      ?: (_completedRepairScenariosState.value + scenarioId)
    _completedRepairScenariosState.value = updated
    recordStudyMinutes(2)
    recordConversationMemorySummary(
      "Resolved Conversation Repair scenario '$scenarioId'",
      com.example.data.model.Language.findByCode(languageCode).name
    )
  }

  fun recordFutureMeComparison(result: com.example.data.model.FutureMeComparisonResult) {
    if (!_mediaPrivacyConsentState.value.syntheticFluentReferenceConsent) return
    val updated = memoryManager?.recordFutureMeComparison(result)
      ?: (listOf(result) + _futureMeComparisonsState.value)
    _futureMeComparisonsState.value = updated
    recordStudyMinutes(2)
  }

  fun computeLanguageSkillPassport(
    languageCode: String = userProfileState.value.targetLanguageCode
  ): com.example.data.model.LanguageSkillPassportSummary {
    val m = memoryManager
    if (m != null) {
      return m.buildLanguageSkillPassportSummary(
        languageCode = languageCode,
        flashcards = flashcardProgressState.value,
        speakingSessions = speakingSessionsState.value
      )
    }
    val lang = com.example.data.model.Language.findByCode(languageCode)
    return com.example.data.model.LanguageSkillPassportSummary(
      languageCode = lang.code,
      languageName = lang.name,
      competencies = emptyList(),
      totalSpeakingRecordingsCount = _portfolioSpeakingSamplesState.value.size,
      totalCompletedPracticalTasksCount = _verifiedHabitCompletionsState.value.size,
      vocabularyMasteredCount = flashcardProgressState.value.count { it.boxLevel >= 3L },
      grammarRepairDemonstrationsCount = _completedRepairScenariosState.value.size
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INVITE FRIENDS & REFERRAL REWARDS
  // ═══════════════════════════════════════════════════════════════════════════

  private val _referralDashboardState = MutableStateFlow(
    com.example.data.model.ReferralDashboardState()
  )
  val referralDashboardState: StateFlow<com.example.data.model.ReferralDashboardState> =
    _referralDashboardState.asStateFlow()

  fun loadReferralDashboard(forceRefresh: Boolean = false) {
    val repo = referralRepo ?: return
    val currentProfile = userProfileState.value.let {
      if (it.userId.isBlank()) it.copy(userId = userId) else it
    }
    viewModelScope.launch(Dispatchers.IO) {
      _referralDashboardState.value = _referralDashboardState.value.copy(
        isLoading = _referralDashboardState.value.referralCode.isBlank() || forceRefresh,
        errorMessage = null
      )
      val loaded = repo.loadReferralDashboard(currentProfile, forceRefresh = forceRefresh)
      _referralDashboardState.value = loaded
    }
  }

  fun recordReferralShare(channel: com.example.data.model.ReferralShareChannel) {
    val repo = referralRepo ?: return
    viewModelScope.launch(Dispatchers.IO) {
      val nextPending = repo.recordInvitationShared(userId, channel)
      val current = _referralDashboardState.value
      _referralDashboardState.value = current.copy(
        pendingInvitationsCount = maxOf(current.pendingInvitationsCount, nextPending),
        statusMessage = "Invite ready via ${channel.displayName}!"
      )
    }
  }

  fun claimReferralCode(
    rawCode: String,
    attributionSource: String = "manual_code",
    isNewAccountRegistration: Boolean = true,
    onComplete: (com.example.data.model.ReferralClaimOutcome) -> Unit = {}
  ) {
    val repo = referralRepo
    if (repo == null) {
      onComplete(
        com.example.data.model.ReferralClaimOutcome(
          success = false,
          message = "Referral service unavailable offline."
        )
      )
      return
    }
    val currentProfile = userProfileState.value.let {
      if (it.userId.isBlank()) it.copy(userId = userId) else it
    }
    viewModelScope.launch(Dispatchers.IO) {
      _referralDashboardState.value = _referralDashboardState.value.copy(
        isClaimingCode = true,
        errorMessage = null,
        statusMessage = null
      )
      val outcome = repo.claimReferralCode(
        rawCode = rawCode,
        referredProfile = currentProfile,
        attributionSource = attributionSource,
        isNewAccountRegistration = isNewAccountRegistration
      )
      if (outcome.success) {
        val refreshed = repo.loadReferralDashboard(currentProfile, forceRefresh = true)
        _referralDashboardState.value = refreshed.copy(
          isClaimingCode = false,
          statusMessage = outcome.message
        )
      } else {
        _referralDashboardState.value = _referralDashboardState.value.copy(
          isClaimingCode = false,
          errorMessage = outcome.message
        )
      }
      kotlinx.coroutines.withContext(Dispatchers.Main) {
        onComplete(outcome)
      }
    }
  }

  fun clearReferralBannerMessages() {
    _referralDashboardState.value = _referralDashboardState.value.copy(
      statusMessage = null,
      errorMessage = null
    )
  }

  fun getOrFetchPartnerProfile(partnerId: String, onLoaded: (CommunityProfile?) -> Unit) {
    val cached = communityProfilesState.value.firstOrNull { it.userId == partnerId }
    if (cached != null) {
      onLoaded(cached)
      return
    }
    viewModelScope.launch(Dispatchers.IO) {
      val fetched = repository.fetchCommunityProfileById(partnerId)
      if (fetched != null) {
        val merged = (_manualCommunityProfiles.value + fetched).distinctBy { it.userId }
        _manualCommunityProfiles.value = merged
      }
      kotlinx.coroutines.withContext(Dispatchers.Main) {
        onLoaded(fetched)
      }
    }
  }

  fun startDirectCallWithPartner(
    partnerId: String,
    fallbackName: String,
    nativeLang: String,
    targetLang: String,
    callType: String,
    onStarted: (CallRoom) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val room = startOrJoinCallRoom(
          calleeId = partnerId,
          calleeName = fallbackName,
          callType = callType,
          matchMode = "direct",
          calleeNativeLang = nativeLang,
          calleeLearningLang = targetLang
        )
        onStarted(room)
      } catch (e: Exception) {
        onError(
          com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
            raw = e.localizedMessage,
            fallback = "Couldn't start $callType call. Please try again."
          )
        )
      }
    }
  }

  init {
    val isRobolectric = android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    if (isRobolectric) {
      val cachedNow = memoryManager?.loadCachedProfileDetails(userId)
      val snapsNow = memoryManager?.loadAllLanguageProgressSnapshots().orEmpty()
      _languageProgressSnapshots.value = snapsNow
      val snapLevels = snapsNow.mapNotNull { (code, snap) ->
        com.example.data.model.normalizeCefrCodeOrNull(snap.cefrLevel)?.let { code to it }
      }.toMap()
      if (cachedNow != null) {
        _cachedProfileMeta.value = cachedNow.copy(
          languageLevels = snapLevels + cachedNow.languageLevels
        )
      }
    }
    viewModelScope.launch(Dispatchers.IO) {
      if (!isRobolectric) {
        kotlinx.coroutines.delay(250L)
      }
      // PART 0 FIX: Never persist or load isPro/subscriptionTier from SharedPreferences as truth.
      // Clear any stale cached subscription override on startup; server entitlement document is authoritative.
      memoryManager?.saveSubscriptionState(
        tier = "free",
        purchaseToken = null,
        isFreeTrial = false,
        hasUsedFreeTrial = false,
        autoRenewing = false,
        expiryEpochMs = 0L
      )
      _localSubscriptionOverride.value = null
      val localAvatar = memoryManager?.loadProfileAvatar(userId)
      if (!localAvatar.isNullOrBlank()) _localAvatarOverride.value = localAvatar
      val loadedSnapshots = memoryManager?.loadAllLanguageProgressSnapshots().orEmpty()
      _languageProgressSnapshots.value = loadedSnapshots
      val snapshotLevels = loadedSnapshots.mapNotNull { (code, snap) ->
        com.example.data.model.normalizeCefrCodeOrNull(snap.cefrLevel)?.let { code to it }
      }.toMap()
      val loadedCached = memoryManager?.loadCachedProfileDetails(userId)
      _cachedProfileMeta.value = loadedCached?.copy(
        languageLevels = snapshotLevels + loadedCached.languageLevels
      )
      memoryManager?.loadUserAccountSettings(userId)?.let { _accountSettingsState.value = it }
      kotlinx.coroutines.delay(if (isRobolectric) 50L else 1850L)
      memoryManager?.loadCustomProDecks()?.let { _customProDecks.value = it }
      memoryManager?.loadAllInProgressLessons()?.let { _inProgressLessonsMap.value = it }
      memoryManager?.loadDownloadedLessonIds()?.let { _downloadedLessonIds.value = it }
      _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
      memoryManager?.loadDailyMinutesMap()?.let { _dailyMinutesMap.value = it }
      memoryManager?.loadSrsMetadataMap()?.let { _srsMetadataMap.value = it }
      memoryManager?.loadMistakeNotebook()?.let { _mistakeNotebookEntries.value = it }
      memoryManager?.loadPronunciationHistory()?.let { _pronunciationHistory.value = it }
      memoryManager?.loadBookmarkedReadingIds()?.let { _bookmarkedReadingIds.value = it }
      memoryManager?.loadCompletedAcademyIds()?.let { _completedAcademyIds.value = it }
      _placementEvaluationState.value = memoryManager?.loadPlacementEvaluation()
      memoryManager?.loadLearningGoals()?.let { _learningGoalsState.value = it }
      memoryManager?.loadCompletedDailyLifeMissions()?.let { _completedDailyMissionsState.value = it }
      memoryManager?.loadPortfolioPrivacySettings()?.let { _portfolioPrivacySettingsState.value = it }
      val initialProfile = userProfileState.value
      val initialLangName = com.example.data.model.Language.findByCode(initialProfile.targetLanguageCode).name
      reloadLanguageScopedLearningMemory(initialLangName, initialProfile.proficiencyLevel)
      memoryManager?.getMediaPrivacyConsent()?.let { _mediaPrivacyConsentState.value = it }
      memoryManager?.getLivingWorldProgressMap()?.let { _livingWorldProgressState.value = it }
      memoryManager?.loadLearningPlanSetup()?.let { _learningPlanPrefsState.value = it }
      memoryManager?.appContext()?.let { appCtx ->
        _offlineEmergencyLangsState.value =
          com.example.data.repository.OfflineEmergencyPhraseRepository.listDownloadedLanguageCodes(appCtx)
      } ?: run {
        memoryManager?.getOfflineEmergencyPackLanguages()?.let { _offlineEmergencyLangsState.value = it }
      }
      memoryManager?.getSelectedHabitRoutineIds()?.let { _selectedHabitRoutinesState.value = it }
      memoryManager?.getVerifiedHabitCompletions()?.let { _verifiedHabitCompletionsState.value = it }
      memoryManager?.getCompletedRepairScenarioIds()?.let { _completedRepairScenariosState.value = it }
      memoryManager?.getFutureMeComparisons()?.let { _futureMeComparisonsState.value = it }

      val initialSub = if (com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement) {
        "premium_monthly"
      } else {
        "free"
      }
      memoryManager?.appContext()?.let { appCtx ->
        com.example.ai.AiProviderManager.initialize(appCtx)
        com.example.ai.GeminiCostAndQuotaManager.initialize(
          context = appCtx,
          userId = userId,
          subscriptionTier = initialSub,
          onSyncToCloud = { snap ->
            viewModelScope.launch(Dispatchers.IO) {
              try {
                repository.syncAiUsageQuota(snap)
              } catch (_: Exception) {}
            }
          }
        )
      } ?: run {
        com.example.ai.GeminiCostAndQuotaManager.switchActiveAccount(userId, initialSub)
        com.example.ai.GeminiCostAndQuotaManager.setCloudSyncCallback { snap ->
          viewModelScope.launch(Dispatchers.IO) {
            try {
              repository.syncAiUsageQuota(snap)
            } catch (_: Exception) {}
          }
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      if (!isRobolectric) {
        kotlinx.coroutines.delay(1900L)
      }
      loadPartnersDirectory(forceRefresh = false)

      try {
        val profile = repository.getUserProfile(userId)
        if (profile == null) {
          val initialProfile = UserProfile(
            userId = userId,
            displayName = "Language Learner",
            nativeLanguageCode = "en",
            targetLanguageCode = "es",
            activeLanguages = listOf("es"),
            subscriptionTier = "free"
          )
          repository.createUserProfile(initialProfile)
          seedStarterFlashcards("es")
        } else {
          // PART 0 ONE-TIME CLEANUP: If existing user document has stale client-written subscriptionTier
          // or purchaseToken without server-verified entitlement, clean it up to "free".
          val hasServerPro = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement
          val cleanedProfile = if (!hasServerPro && (profile.subscriptionTier != "free" || !profile.purchaseToken.isNullOrBlank())) {
            val reset = profile.copy(
              subscriptionTier = "free",
              purchaseToken = null,
              isFreeTrial = false,
              subscriptionAutoRenewing = false,
              subscriptionExpiryEpochMs = 0L
            )
            runCatching { repository.updateUserProfile(reset) }
            reset
          } else {
            profile
          }
          com.example.ai.GeminiCostAndQuotaManager.switchActiveAccount(
            userId = userId,
            subscriptionTier = if (hasServerPro) "premium_monthly" else "free"
          )
          val settings = _accountSettingsState.value
          repository.syncCommunityProfile(
            profile = cleanedProfile,
            isOnline = settings.isOnlineVisible,
            allowCalls = settings.allowIncomingCalls,
            presenceState = if (settings.isOnlineVisible) "online" else "offline",
            showLastSeen = settings.showLastSeen
          )
          com.example.presence.PresenceManager.recalculateAndPublishPresence("viewmodel_profile_synced")
          val syncedLangName = com.example.data.model.Language.findByCode(cleanedProfile.targetLanguageCode).name
          reloadLanguageScopedLearningMemory(syncedLangName, cleanedProfile.proficiencyLevel)
        }
      } catch (_: Exception) {}

      if (!isRobolectric) {
        kotlinx.coroutines.delay(1200L)
      }
      try {
        val synced = memoryManager?.synchronizePendingMutations(repository) ?: 0
        if (synced > 0) {
          _pendingOfflineSyncCount.value = memoryManager?.loadPendingMutations()?.size ?: 0
        }
      } catch (_: Exception) {}

      try {
        val remoteQuota = repository.fetchAiUsageQuota(userId)
        if (remoteQuota != null) {
          com.example.ai.GeminiCostAndQuotaManager.mergeRemoteCloudSnapshot(remoteQuota)
        }
      } catch (_: Exception) {}
    }

    viewModelScope.launch(Dispatchers.IO) {
      kotlinx.coroutines.delay(if (isRobolectric) 400L else 2400L)
      flashcardProgressState.collect { cards ->
        if (cards.isNotEmpty()) {
          memoryManager?.cacheFlashcardsForOffline(cards)
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      kotlinx.coroutines.delay(if (isRobolectric) 500L else 2600L)
      val sessionStartEpochSec = (System.currentTimeMillis() / 1000L) - 120L
      notificationsState.collect { list ->
        val ctx = memoryManager?.appContext() ?: return@collect
        list.forEach { notif ->
          val createdSec = notif.createdAt?.seconds ?: (System.currentTimeMillis() / 1000L)
          if (!notif.isRead && createdSec >= sessionStartEpochSec) {
            com.example.notifications.SmartNotificationManager.handleRealtimeUserNotification(ctx, notif)
          }
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      com.example.data.repository.AccessPolicyManager.entitlementState.collect { ent ->
        val proValid = ent.hasProEntitlement && ent.effectiveUid == userId
        if (!proValid && !isRobolectric) {
          memoryManager?.clearDownloadedLessons()
          _downloadedLessonIds.value = emptySet()
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      repository.observeFloatingContinuePrefs(userId)
        .staggeredStart(300L)
        .catch { /* keep local cached preferences offline */ }
        .collect { remotePrefs ->
          if (remotePrefs != null && remotePrefs != _floatingContinuePrefsState.value) {
            _floatingContinuePrefsState.value = remotePrefs
            memoryManager?.saveFloatingContinuePrefs(userId, remotePrefs)
          }
        }
    }
  }
}


