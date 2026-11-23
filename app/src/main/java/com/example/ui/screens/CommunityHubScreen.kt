package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.components.LingoAvatar
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiTutorService
import com.example.data.model.CallHistoryRecord
import com.example.data.model.CallRoom
import com.example.data.model.CommunityProfile
import com.example.data.model.CountryInfo
import com.example.data.model.DirectChatMessage
import com.example.data.model.FriendRequest
import com.example.data.model.Language
import com.example.data.model.UserBlock
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoSkeletonCard
import com.example.ui.components.LingoTokens
import com.example.ui.components.lingoAuroraBackdrop
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.components.rememberReducedMotionEnabled
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Calculates a real compatibility percentage (0..99) only when real profile attributes exist.
 * Returns null if partner or user language metadata is missing.
 */
internal fun calculateRealMatchScore(
  userProfile: UserProfile,
  myCommunityProfile: CommunityProfile?,
  partner: CommunityProfile
): Int? {
  if (partner.nativeLanguageCode.isBlank() || partner.targetLanguageCode.isBlank()) return null
  if (userProfile.nativeLanguageCode.isBlank() || userProfile.targetLanguageCode.isBlank()) return null

  var score = 35 // Base verified learner compatibility

  val isExactTandem = partner.nativeLanguageCode == userProfile.targetLanguageCode &&
    partner.targetLanguageCode == userProfile.nativeLanguageCode
  val teachesMyTarget = partner.nativeLanguageCode == userProfile.targetLanguageCode
  val learnsMyTarget = partner.targetLanguageCode == userProfile.targetLanguageCode
  val learnsMyNative = partner.targetLanguageCode == userProfile.nativeLanguageCode

  score += when {
    isExactTandem -> 35
    teachesMyTarget && learnsMyNative -> 35
    teachesMyTarget -> 24
    learnsMyNative -> 18
    learnsMyTarget -> 14
    else -> 4
  }

  // Proficiency alignment
  val levels = listOf("Beginner", "Intermediate", "Advanced", "Native")
  val myLevelIdx = levels.indexOfFirst { it.equals(userProfile.proficiencyLevel, ignoreCase = true) }.coerceAtLeast(0)
  val partnerLevelIdx = levels.indexOfFirst { it.equals(partner.proficiencyLevel, ignoreCase = true) }.coerceAtLeast(0)
  val levelDiff = kotlin.math.abs(myLevelIdx - partnerLevelIdx)
  score += when (levelDiff) {
    0 -> 10
    1 -> 8
    else -> 4
  }

  // Shared interests overlap
  val myInterests = (myCommunityProfile?.interests ?: listOf("Travel", "Culture", "Music"))
    .map { it.lowercase().trim() }
    .toSet()
  val sharedInterestsCount = partner.interests.count { it.lowercase().trim() in myInterests }
  score += (sharedInterestsCount * 5).coerceAtMost(12)

  // Real-time availability bonus
  if (partner.isOnline) score += 5
  if (partner.allowCalls) score += 2

  return score.coerceIn(42, 99)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityHubScreen(
  userProfile: UserProfile,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  initialTab: Int = 0,
  initialPartnerId: String = "",
  initialOpenPartnerProfile: Boolean = false,
  onStartLiveCall: (CallRoom) -> Unit,
  onStartAiVoiceCall: () -> Unit
) {
  val context = LocalContext.current
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()

  val communityProfiles by viewModel.communityProfilesState.collectAsState()
  val myCommunityProfile by viewModel.myCommunityProfileState.collectAsState()
  val friendRequests by viewModel.friendRequestsState.collectAsState()
  val blockedUsers by viewModel.blockedUsersState.collectAsState()
  val callHistory by viewModel.callHistoryState.collectAsState()
  val mutedPartnerIds by viewModel.mutedPartnerIds.collectAsState()
  val accountSettings by viewModel.accountSettingsState.collectAsState()
  val communityLoadError by viewModel.communityProfilesLoadError.collectAsState()
  val partnersQuotaExhausted by viewModel.partnersQuotaExhausted.collectAsState()
  val isPartnersLoading by viewModel.isPartnersLoading.collectAsState()
  val hasMorePartners by viewModel.hasMorePartners.collectAsState()
  val topXpLeaderboard by viewModel.topXpLeaderboardState.collectAsState()
  val localPresence by com.example.presence.PresenceManager.localStatus.collectAsState()
  val presenceTickerMs by com.example.presence.PresenceManager.presenceTicker.collectAsState()

  var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(if (initialTab == 1) 0 else initialTab.coerceIn(0, 4)) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var filterLanguageCode by rememberSaveable { mutableStateOf("ALL") }
  var filterCountry by rememberSaveable { mutableStateOf("ALL") }
  var filterLevel by rememberSaveable { mutableStateOf("ALL") }
  var onlineOnlyFilter by rememberSaveable { mutableStateOf(false) }
  var visiblePartnerLimit by rememberSaveable { mutableIntStateOf(10) }

  var selectedCountryMatch by rememberSaveable { mutableStateOf(CountryInfo.COUNTRIES.first().name) }
  var selectedInterestMatch by rememberSaveable { mutableStateOf(CountryInfo.INTEREST_TOPICS.first()) }
  var activeMatchModeBusy by remember { mutableStateOf<String?>(null) }

  var activeChatPartner by remember { mutableStateOf<CommunityProfile?>(null) }
  var inspectedPartner by remember { mutableStateOf<CommunityProfile?>(null) }
  var reportingPartner by remember { mutableStateOf<CommunityProfile?>(null) }
  var inviteRequiredPartner by remember { mutableStateOf<Pair<CommunityProfile, String>?>(null) }
  var showPrivacySafetyDialog by remember { mutableStateOf(false) }
  var showFiltersSheet by remember { mutableStateOf(false) }
  var showMatchPreferencesSheet by remember { mutableStateOf(false) }
  var showSignInPromptDialog by remember { mutableStateOf(false) }
  var pendingRespectfulAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var callbackSheetRecord by remember { mutableStateOf<CallHistoryRecord?>(null) }
  var friendsSearchQuery by rememberSaveable { mutableStateOf("") }
  var filterInterest by rememberSaveable { mutableStateOf("ALL") }
  var notifyWhenOnlineEnabled by remember {
    mutableStateOf(
      context.getSharedPreferences("lingosphere_connect_prefs", Context.MODE_PRIVATE)
        .getBoolean("notify_when_online", false)
    )
  }
  var hasAcceptedRespectfulNotice by remember {
    mutableStateOf(
      context.getSharedPreferences("lingosphere_connect_prefs", Context.MODE_PRIVATE)
        .getBoolean("respectful_notice_accepted", false)
    )
  }
  var smartMatchChoice by remember { mutableStateOf("anyone") }
  var smartMatchManualExpand by remember { mutableStateOf(false) }

  val isSignedInUser = userProfile.userId.isNotBlank() && !userProfile.userId.startsWith("guest_")

  fun runWithCallSafetyGuards(isCallAction: Boolean = true, action: () -> Unit) {
    if (!isSignedInUser) {
      showSignInPromptDialog = true
      return
    }
    if (isCallAction && accountSettings.isUnder13CallingDisabled) {
      Toast.makeText(
        context,
        "Live calling is unavailable for accounts under 13.",
        Toast.LENGTH_LONG
      ).show()
      return
    }
    if (isCallAction && !hasAcceptedRespectfulNotice) {
      pendingRespectfulAction = action
      return
    }
    action()
  }

  LaunchedEffect(filterLanguageCode, filterCountry) {
    viewModel.loadPartnersDirectory(
      filterLanguageCode = filterLanguageCode,
      filterCountry = filterCountry
    )
  }

  LaunchedEffect(selectedTab) {
    if (selectedTab == 4) {
      viewModel.loadTopXpLeaderboard()
    }
  }

  LaunchedEffect(initialPartnerId, initialOpenPartnerProfile, communityProfiles) {
    if (initialPartnerId.isNotBlank()) {
      val match = communityProfiles.firstOrNull { it.userId == initialPartnerId }
      if (match != null) {
        if (initialOpenPartnerProfile) {
          if (inspectedPartner == null) inspectedPartner = match
        } else if (activeChatPartner == null) {
          activeChatPartner = match
        }
      } else {
        viewModel.getOrFetchPartnerProfile(initialPartnerId) { fetched ->
          if (fetched != null) {
            if (initialOpenPartnerProfile) {
              inspectedPartner = fetched
            } else {
              activeChatPartner = fetched
            }
          }
        }
      }
    }
  }

  // Independent scroll states per tab so switching tabs never causes scrolling conflicts
  val findPartnersListState = rememberLazyListState()
  val liveMatchListState = rememberLazyListState()
  val friendsListState = rememberLazyListState()
  val callsListState = rememberLazyListState()
  val leaderboardListState = rememberLazyListState()

  val blockedIds = remember(blockedUsers) {
    blockedUsers.map { it.blockedUserId }.toSet()
  }

  fun initiatePartnerCall(partner: CommunityProfile, friendStatus: String, callType: String) {
    if (blockedIds.contains(partner.userId)) {
      Toast.makeText(context, "Unblock ${partner.displayName} before calling.", Toast.LENGTH_SHORT).show()
      return
    }
    if (friendStatus != "accepted") {
      inviteRequiredPartner = partner to callType
      return
    }
    val partnerAvailable = partner.allowCalls && partner.isEffectivelyOnline(System.currentTimeMillis())
    if (!partnerAvailable) {
      Toast.makeText(context, "Partner is not available", Toast.LENGTH_SHORT).show()
      return
    }
    runWithCallSafetyGuards(isCallAction = true) {
      scope.launch {
        try {
          val room = viewModel.startOrJoinCallRoom(
            calleeId = partner.userId,
            calleeName = partner.displayName,
            callType = callType,
            matchMode = "direct",
            calleeNativeLang = partner.nativeLanguageCode,
            calleeLearningLang = partner.targetLanguageCode
          )
          onStartLiveCall(room)
        } catch (e: Exception) {
          val rawMsg = e.localizedMessage.orEmpty()
          val friendlyMsg = if (
            rawMsg.contains("busy", ignoreCase = true) ||
            rawMsg.contains("offline", ignoreCase = true) ||
            rawMsg.contains("unavailable", ignoreCase = true) ||
            rawMsg.contains("not accepting", ignoreCase = true)
          ) {
            "Partner is not available"
          } else {
            com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
              rawMsg,
              isPro = userProfile.isEntitlementVerified,
              fallback = "Partner is not available"
            )
          }
          Toast.makeText(context, friendlyMsg, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  // Deduplicate real profiles by userId and exclude self & blocked users
  val visiblePartners = remember(communityProfiles, userProfile.userId, blockedIds) {
    communityProfiles
      .filter { it.userId.isNotBlank() && it.userId != userProfile.userId && !blockedIds.contains(it.userId) }
      .distinctBy { it.userId }
  }

  val filteredPartners = remember(
    visiblePartners,
    searchQuery,
    filterLanguageCode,
    filterCountry,
    filterLevel,
    filterInterest,
    onlineOnlyFilter,
    userProfile,
    myCommunityProfile,
    presenceTickerMs
  ) {
    val q = searchQuery.trim().lowercase()
    val base = visiblePartners.filter { p ->
      val matchesQuery = q.isEmpty() ||
        p.displayName.lowercase().contains(q) ||
        p.country.lowercase().contains(q) ||
        p.bio.lowercase().contains(q) ||
        p.interests.any { it.lowercase().contains(q) }

      val matchesLang = when (filterLanguageCode) {
        "ALL" -> true
        "TANDEM" -> (p.nativeLanguageCode == userProfile.targetLanguageCode && p.targetLanguageCode == userProfile.nativeLanguageCode) ||
          p.nativeLanguageCode == userProfile.targetLanguageCode
        else -> p.targetLanguageCode == filterLanguageCode || p.nativeLanguageCode == filterLanguageCode
      }

      val matchesCountry = filterCountry == "ALL" || p.country.equals(filterCountry, ignoreCase = true)
      val matchesLevel = filterLevel == "ALL" || p.proficiencyLevel.contains(filterLevel, ignoreCase = true)
      val matchesInterest = filterInterest == "ALL" || p.interests.any { it.contains(filterInterest, ignoreCase = true) }
      val matchesOnline = !onlineOnlyFilter || p.isEffectivelyOnline(presenceTickerMs)

      matchesQuery && matchesLang && matchesCountry && matchesLevel && matchesInterest && matchesOnline
    }

    base.sortedWith(
      compareByDescending<CommunityProfile> { it.isEffectivelyOnline(presenceTickerMs) }
        .thenByDescending { calculateRealMatchScore(userProfile, myCommunityProfile, it) ?: 0 }
        .thenByDescending { it.xpPoints }
    )
  }

  val paginatedPartners = remember(filteredPartners, visiblePartnerLimit) {
    filteredPartners.take(visiblePartnerLimit)
  }

  val acceptedFriendIds = remember(friendRequests, userProfile.userId) {
    friendRequests
      .filter { it.status == "accepted" }
      .map { if (it.senderId == userProfile.userId) it.receiverId else it.senderId }
      .toSet()
  }

  val outgoingPendingRequests = remember(friendRequests, userProfile.userId) {
    friendRequests.filter { it.senderId == userProfile.userId && it.status == "pending" }
  }

  val incomingRequests = remember(friendRequests, userProfile.userId) {
    friendRequests.filter { it.receiverId == userProfile.userId && it.status == "pending" }
  }

  val friendsList = remember(visiblePartners, acceptedFriendIds) {
    visiblePartners.filter { acceptedFriendIds.contains(it.userId) }
  }

  val leaderboardList = remember(topXpLeaderboard, visiblePartners, userProfile, myCommunityProfile) {
    val myComm = CommunityProfile(
      userId = userProfile.userId,
      displayName = "${userProfile.displayName.ifBlank { "You" }} (You)",
      country = myCommunityProfile?.country ?: "Global",
      countryFlag = myCommunityProfile?.countryFlag ?: "🌟",
      nativeLanguageCode = userProfile.nativeLanguageCode,
      targetLanguageCode = userProfile.targetLanguageCode,
      proficiencyLevel = userProfile.proficiencyLevel,
      xpPoints = userProfile.xpPoints,
      level = userProfile.level,
      currentStreak = userProfile.currentStreak,
      isOnline = myCommunityProfile?.isOnline ?: true
    )
    (topXpLeaderboard + visiblePartners + myComm)
      .distinctBy { it.userId }
      .sortedByDescending { it.xpPoints }
  }

  val onlineCount = remember(visiblePartners, presenceTickerMs) {
    visiblePartners.count { it.isEffectivelyOnline(presenceTickerMs) }
  }

  if (activeChatPartner != null) {
    val livePartner = remember(activeChatPartner, communityProfiles, presenceTickerMs) {
      communityProfiles.firstOrNull { it.userId == activeChatPartner!!.userId } ?: activeChatPartner!!
    }
    DirectChatScreen(
      userProfile = userProfile,
      partner = livePartner,
      viewModel = viewModel,
      speechManager = speechManager,
      isMuted = mutedPartnerIds.contains(livePartner.userId),
      onToggleMute = { viewModel.toggleMutePartner(livePartner.userId) },
      onScheduleSession = { topic, timeSlot ->
        viewModel.scheduleExchangeSession(
          partnerId = livePartner.userId,
          partnerName = livePartner.displayName,
          topic = topic,
          timeSlot = timeSlot
        )
        viewModel.sendSpeakingInvitation(
          receiverId = livePartner.userId,
          receiverName = livePartner.displayName,
          topic = "$topic ($timeSlot)"
        )
        Toast.makeText(context, "Speaking invitation & exchange proposal sent to ${livePartner.displayName}!", Toast.LENGTH_SHORT).show()
      },
      onBack = { activeChatPartner = null },
      onStartCall = { callType ->
        val partner = activeChatPartner!!
        if (partner.userId.startsWith("ai_partner_")) {
          activeChatPartner = null
          onStartAiVoiceCall()
        } else if (!partner.allowCalls) {
          Toast.makeText(context, "${partner.displayName} has disabled incoming calls.", Toast.LENGTH_SHORT).show()
        } else {
          scope.launch {
            try {
              val room = viewModel.startOrJoinCallRoom(
                calleeId = partner.userId,
                calleeName = partner.displayName,
                callType = callType,
                matchMode = "direct",
                calleeNativeLang = partner.nativeLanguageCode,
                calleeLearningLang = partner.targetLanguageCode
              )
              onStartLiveCall(room)
            } catch (e: Exception) {
              Toast.makeText(
                context,
                com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
                  e.localizedMessage,
                  isPro = userProfile.isEntitlementVerified,
                  fallback = "Unable to start call right now."
                ),
                Toast.LENGTH_SHORT
              ).show()
            }
          }
        }
      }
    )
    return
  }

  val reducedMotion = rememberReducedMotionEnabled()
  val pulseAlpha = if (reducedMotion || onlineCount <= 0) {
    1f
  } else {
    val transition = rememberInfiniteTransition(label = "onlineDotPulse")
    val alpha by transition.animateFloat(
      initialValue = 0.45f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(1200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "onlinePulseAlpha"
    )
    alpha
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .lingoAuroraBackdrop(),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 840.dp)
    ) {
      // Calm Connect Header: "Connect" 28sp bold, ONE online count pill, and Safety icon (no 3-dot overflow menu)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Connect",
              color = LingoTokens.textPrimary,
              fontSize = 28.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )

            Surface(
              shape = RoundedCornerShape(999.dp),
              color = LingoTokens.bgGlassCard,
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier.testTag("online_users_badge")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                      if (onlineCount > 0) LingoTokens.statusOnline.copy(alpha = pulseAlpha)
                      else LingoTokens.textSecondary.copy(alpha = 0.5f)
                    )
                )
                Text(
                  text = "$onlineCount online",
                  color = if (onlineCount > 0) Color(0xFF6EE7B7) else LingoTokens.textSecondary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

          IconButton(
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              showPrivacySafetyDialog = true
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(LingoTokens.bgGlassCard)
              .border(1.dp, LingoTokens.borderSubtle, CircleShape)
              .testTag("connect_privacy_button")
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Safety and Privacy",
              tint = LingoTokens.textPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Underline Tab Bar: Partners | Friends | Calls
        val primaryTabs = listOf(
          0 to "Partners",
          2 to if (incomingRequests.isNotEmpty()) "Friends (${incomingRequests.size})" else "Friends",
          3 to "Calls"
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("connect_three_tab_bar"),
          horizontalArrangement = Arrangement.spacedBy(24.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          primaryTabs.forEach { (tabIndex, title) ->
            val isSelected = selectedTab == tabIndex || (tabIndex == 0 && selectedTab == 1)
            val labelColor by animateColorAsState(
              targetValue = if (isSelected) LingoTokens.textPrimary else LingoTokens.textSecondary,
              animationSpec = tween(200),
              label = "connectTabText_$tabIndex"
            )
            Column(
              modifier = Modifier
                .clickable {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  selectedTab = tabIndex
                }
                .padding(vertical = 8.dp)
                .testTag("connect_tab_$tabIndex"),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = title,
                color = labelColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                softWrap = false
              )
              Spacer(modifier = Modifier.height(6.dp))
              Box(
                modifier = Modifier
                  .width(if (isSelected) 36.dp else 0.dp)
                  .height(3.dp)
                  .clip(RoundedCornerShape(999.dp))
                  .background(LingoTokens.brandHorizontalGradient)
              )
            }
          }
        }
      }

      HorizontalDivider(color = LingoTokens.borderSubtle)

      // Tab Content Area
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        when (selectedTab) {
          // TAB 0 (and legacy TAB 1): COMPACT SMART MATCH + SLIM AI SPEAKING PARTNER + REAL DIRECTORY
          0, 1 -> {
            val activeFilterCount = (if (filterLanguageCode != "ALL") 1 else 0) +
              (if (filterCountry != "ALL") 1 else 0) +
              (if (filterLevel != "ALL") 1 else 0) +
              (if (filterInterest != "ALL") 1 else 0) +
              (if (onlineOnlyFilter) 1 else 0)

            val onlineAvailablePartners = remember(visiblePartners, presenceTickerMs) {
              visiblePartners.filter { it.isEffectivelyOnline(presenceTickerMs) }
            }
            val selectedCountryInfo = remember(selectedCountryMatch) {
              CountryInfo.COUNTRIES.firstOrNull { it.name == selectedCountryMatch } ?: CountryInfo.COUNTRIES.first()
            }
            val targetLangName = remember(userProfile.targetLanguageCode) {
              Language.findByCode(userProfile.targetLanguageCode).name
            }

            fun launchLiveMatch(modeChoice: String, callType: String) {
              if (activeMatchModeBusy != null) return
              runWithCallSafetyGuards(isCallAction = true) {
                val backendMode = when (modeChoice) {
                  "country" -> "country"
                  "interest" -> "interest"
                  else -> "random"
                }
                activeMatchModeBusy = backendMode
                val callableOnline = onlineAvailablePartners.filter { it.allowCalls }
                val candidate = when (backendMode) {
                  "country" -> callableOnline.firstOrNull {
                    it.country.equals(selectedCountryMatch, ignoreCase = true)
                  }
                  "interest" -> callableOnline.firstOrNull { p ->
                    p.interests.any { it.equals(selectedInterestMatch, ignoreCase = true) }
                  }
                  else -> callableOnline.firstOrNull {
                    it.nativeLanguageCode == userProfile.targetLanguageCode ||
                      it.targetLanguageCode == userProfile.targetLanguageCode
                  }
                } ?: callableOnline.firstOrNull()

                val fallbackName = when (backendMode) {
                  "country" -> "$selectedCountryMatch Speaker"
                  "interest" -> "$selectedInterestMatch Partner"
                  else -> "Language Partner"
                }
                val topic = when (backendMode) {
                  "country" -> "Country Practice: $selectedCountryMatch"
                  "interest" -> selectedInterestMatch
                  else -> ""
                }

                scope.launch {
                  try {
                    val room = viewModel.startOrJoinCallRoom(
                      calleeId = candidate?.userId ?: "",
                      calleeName = candidate?.displayName ?: fallbackName,
                      callType = callType,
                      matchMode = backendMode,
                      calleeNativeLang = candidate?.nativeLanguageCode ?: userProfile.targetLanguageCode,
                      calleeLearningLang = candidate?.targetLanguageCode ?: userProfile.nativeLanguageCode,
                      conversationTopic = topic
                    )
                    activeMatchModeBusy = null
                    onStartLiveCall(room)
                  } catch (e: Exception) {
                    activeMatchModeBusy = null
                    Toast.makeText(
                      context,
                      com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
                        e.localizedMessage,
                        isPro = userProfile.isEntitlementVerified,
                        fallback = "Couldn't start matching right now. Please try again."
                      ),
                      Toast.LENGTH_SHORT
                    ).show()
                  }
                }
              }
            }

            Box(modifier = Modifier.fillMaxSize()) {
              LazyColumn(
                state = findPartnersListState,
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("find_partners_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = LingoTokens.bottomBarClearance),
                verticalArrangement = Arrangement.spacedBy(24.dp)
              ) {
                // 1. Signature Gradient "Smart Match" Hero Card (ONE accent gradient on Connect)
                item(key = "smart_match_card") {
                  val isMatchBusy = activeMatchModeBusy != null
                  val matchSummaryLabel = when (smartMatchChoice) {
                    "country" -> "${selectedCountryInfo.flagEmoji} ${selectedCountryInfo.name}"
                    "interest" -> selectedInterestMatch
                    else -> "Anyone · $targetLangName · ${userProfile.proficiencyLevel}"
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(28.dp))
                      .background(LingoTokens.brandHeroGradient)
                      .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
                      .padding(20.dp)
                      .testTag("smart_match_card")
                  ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                          text = "Smart Match",
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          fontSize = 24.sp
                        )
                        Text(
                          text = "Practice live with someone now",
                          color = Color.White.copy(alpha = 0.88f),
                          fontSize = 15.sp
                        )
                      }

                      // Side-by-side Voice (white pill) & Video (frosted outline)
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                        Surface(
                          onClick = {
                            if (!isMatchBusy) {
                              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                              launchLiveMatch(smartMatchChoice, "voice")
                            }
                          },
                          shape = RoundedCornerShape(999.dp),
                          color = Color.White,
                          modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("smart_match_voice_btn")
                        ) {
                          Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                          ) {
                            if (isMatchBusy) {
                              CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF070B14)
                              )
                            } else {
                              Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF070B14), modifier = Modifier.size(18.dp))
                              Spacer(modifier = Modifier.width(8.dp))
                              Text("Voice", color = Color(0xFF070B14), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                          }
                        }

                        Surface(
                          onClick = {
                            if (!isMatchBusy) {
                              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                              launchLiveMatch(smartMatchChoice, "video")
                            }
                          },
                          shape = RoundedCornerShape(999.dp),
                          color = Color.White.copy(alpha = 0.14f),
                          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                          modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("smart_match_video_btn")
                        ) {
                          Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                          ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                          }
                        }
                      }

                      // Clean "Match preferences" trigger (hides chips into a sheet)
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(12.dp))
                          .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showMatchPreferencesSheet = true
                          }
                          .padding(vertical = 4.dp)
                          .testTag("smart_match_preferences_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text(
                          text = matchSummaryLabel,
                          color = Color.White.copy(alpha = 0.80f),
                          fontSize = 13.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "Match preferences",
                          color = Color.White,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1
                        )
                      }
                    }
                  }
                }

                // 2. Slim Glass "AI Speaking Partner" Row (Gold lock for Free)
                item(key = "ai_speaking_partner") {
                  LingoCleanCard(
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("ai_speaking_partner_card"),
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      onStartAiVoiceCall()
                    },
                    contentPadding = 16.dp
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(44.dp)
                          .clip(RoundedCornerShape(14.dp))
                          .background(LingoTokens.brandIndigo.copy(alpha = 0.18f))
                          .border(1.dp, LingoTokens.brandCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Default.SmartToy,
                          contentDescription = null,
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(22.dp)
                        )
                      }

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Text(
                            text = "AI Speaking Partner",
                            color = LingoTokens.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                          )
                          if (!userProfile.isEntitlementVerified) {
                            LingoGoldLockIcon(size = 16.dp)
                          }
                        }
                        Text(
                          text = "Practice anytime",
                          color = LingoTokens.textSecondary,
                          fontSize = 13.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }

                      Surface(
                        onClick = {
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          onStartAiVoiceCall()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = LingoTokens.bgGlassCard,
                        border = BorderStroke(
                          1.dp,
                          if (userProfile.isEntitlementVerified) LingoTokens.brandCyan.copy(alpha = 0.55f)
                          else LingoTokens.accentGold.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                          .heightIn(min = 44.dp)
                          .testTag("connect_ai_partner_btn")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          if (!userProfile.isEntitlementVerified) {
                            LingoGoldLockIcon(size = 14.dp)
                          }
                          Text(
                            text = if (userProfile.isEntitlementVerified) "Start" else "Unlock Pro",
                            color = if (userProfile.isEntitlementVerified) LingoTokens.brandCyan else LingoTokens.accentGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                          )
                        }
                      }
                    }
                  }
                }

                // 3. Language Partners Header + Search Bar + Filter Icon (Online Only moved into Filter sheet)
                item(key = "search_and_filters") {
                  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Language partners",
                        color = LingoTokens.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                      if (activeFilterCount > 0 || searchQuery.isNotBlank()) {
                        Text(
                          text = "Reset",
                          color = LingoTokens.brandCyan,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.SemiBold,
                          modifier = Modifier.clickable {
                            searchQuery = ""
                            filterLanguageCode = "ALL"
                            filterCountry = "ALL"
                            filterLevel = "ALL"
                            filterInterest = "ALL"
                            onlineOnlyFilter = false
                            visiblePartnerLimit = 4
                          }
                        )
                      }
                    }

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                          searchQuery = it
                          visiblePartnerLimit = 4
                        },
                        modifier = Modifier
                          .weight(1f)
                          .testTag("partner_search_input"),
                        placeholder = {
                          Text(
                            text = "Search partners, country...",
                            color = LingoTokens.textSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        },
                        leadingIcon = {
                          Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Partners",
                            tint = LingoTokens.textSecondary,
                            modifier = Modifier.size(20.dp)
                          )
                        },
                        trailingIcon = {
                          if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                              Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = LingoTokens.textSecondary,
                                modifier = Modifier.size(18.dp)
                              )
                            }
                          }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                          focusedTextColor = LingoTokens.textPrimary,
                          unfocusedTextColor = LingoTokens.textPrimary,
                          focusedContainerColor = LingoTokens.bgGlassCard,
                          unfocusedContainerColor = LingoTokens.bgGlassCard,
                          focusedBorderColor = LingoTokens.brandCyan.copy(alpha = 0.65f),
                          unfocusedBorderColor = LingoTokens.borderSubtle
                        )
                      )

                      Surface(
                        onClick = {
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          showFiltersSheet = true
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (activeFilterCount > 0) LingoTokens.brandCyan.copy(alpha = 0.16f) else LingoTokens.bgGlassCard,
                        border = BorderStroke(
                          1.dp,
                          if (activeFilterCount > 0) LingoTokens.brandCyan.copy(alpha = 0.55f) else LingoTokens.borderSubtle
                        ),
                        modifier = Modifier
                          .heightIn(min = 54.dp)
                          .testTag("connect_filter_button")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter partners",
                            tint = if (activeFilterCount > 0) LingoTokens.brandCyan else LingoTokens.textPrimary,
                            modifier = Modifier.size(20.dp)
                          )
                          if (activeFilterCount > 0) {
                            Text(
                              text = "$activeFilterCount",
                              color = LingoTokens.brandCyan,
                              fontSize = 13.sp,
                              fontWeight = FontWeight.Bold
                            )
                          }
                        }
                      }
                    }

                    Text(
                      text = "${filteredPartners.size} partner${if (filteredPartners.size == 1) "" else "s"} available",
                      color = LingoTokens.textSecondary,
                      fontSize = 13.sp,
                      modifier = Modifier.testTag("connect_available_count")
                    )
                  }
                }

              if (isPartnersLoading && visiblePartners.isEmpty()) {
                items(3, key = { "partner_skeleton_$it" }) {
                  LingoSkeletonCard(height = 112.dp)
                }
              }

              if (partnersQuotaExhausted && filteredPartners.isNotEmpty()) {
                item(key = "quota_banner_cached") {
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("quota_exhausted_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1C10)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f))
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "Cloud partner reads temporarily limited",
                          color = Color(0xFFFBBF24),
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.5.sp
                        )
                        Text(
                          text = "Showing cached partners. Auto-retries paused to protect quota.",
                          color = Color(0xFFCBD5E1),
                          fontSize = 11.sp
                        )
                      }
                      OutlinedButton(
                        onClick = { viewModel.retryPartnersAfterQuota() },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                      ) {
                        Text("Retry", color = Color(0xFFFBBF24), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              if (filteredPartners.isEmpty()) {
                item(key = "empty_partners") {
                  val hasActiveFilters = filterLanguageCode != "ALL" ||
                    filterCountry != "ALL" ||
                    filterLevel != "ALL" ||
                    filterInterest != "ALL" ||
                    onlineOnlyFilter ||
                    searchQuery.isNotBlank()
                  val isNetworkUnavailable = !localPresence.isNetworkConnected
                  val isQuotaLimited = !isNetworkUnavailable && partnersQuotaExhausted
                  val isLoadError = !isNetworkUnavailable &&
                    visiblePartners.isEmpty() &&
                    !communityLoadError.isNullOrBlank()

                  val emptyTitle = when {
                    isNetworkUnavailable -> "You're offline"
                    isLoadingPartnersOnly(isPartnersLoading, visiblePartners.isEmpty()) -> "Finding language partners..."
                    isQuotaLimited || isLoadError -> "Partners are temporarily unavailable"
                    visiblePartners.isNotEmpty() && hasActiveFilters -> "No partners match your filters"
                    else -> "No one is online right now"
                  }

                  val emptySubtitle = when {
                    isNetworkUnavailable ->
                      "Reconnect to the internet to browse and call language partners."
                    isPartnersLoading && visiblePartners.isEmpty() ->
                      "Checking who is available to practice right now."
                    isQuotaLimited || isLoadError ->
                      "Try again in a moment, or practice with our AI speaking partner."
                    visiblePartners.isNotEmpty() && hasActiveFilters ->
                      "Reset your filters to see all ${visiblePartners.size} available partner${if (visiblePartners.size == 1) "" else "s"}."
                    else ->
                      "Try again soon or practice with a partner from the list."
                  }

                  LingoCleanCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 24.dp
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      if (isPartnersLoading && visiblePartners.isEmpty()) {
                        CircularProgressIndicator(
                          color = LingoTokens.brandCyan,
                          strokeWidth = 2.5.dp,
                          modifier = Modifier.size(34.dp)
                        )
                      } else {
                        Box(
                          modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(LingoTokens.brandIndigo.copy(alpha = 0.16f)),
                          contentAlignment = Alignment.Center
                        ) {
                          Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = LingoTokens.brandCyan,
                            modifier = Modifier.size(28.dp)
                          )
                        }
                      }
                      Text(
                        text = emptyTitle,
                        color = LingoTokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                      )
                      Text(
                        text = emptySubtitle,
                        color = LingoTokens.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                      )
                      Spacer(modifier = Modifier.height(6.dp))
                      Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        if (visiblePartners.isNotEmpty() && hasActiveFilters) {
                          OutlinedButton(
                            onClick = {
                              searchQuery = ""
                              filterLanguageCode = "ALL"
                              filterCountry = "ALL"
                              filterLevel = "ALL"
                              filterInterest = "ALL"
                              onlineOnlyFilter = false
                              visiblePartnerLimit = 4
                            },
                            modifier = Modifier
                              .fillMaxWidth()
                              .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, LingoTokens.borderSubtle)
                          ) {
                            Text("Reset filters", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                          }
                        } else if (isNetworkUnavailable || isLoadError || isQuotaLimited) {
                          OutlinedButton(
                            onClick = {
                              if (isQuotaLimited) viewModel.retryPartnersAfterQuota()
                              else viewModel.refreshCommunityProfiles()
                            },
                            enabled = !isPartnersLoading,
                            modifier = Modifier
                              .fillMaxWidth()
                              .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, LingoTokens.borderSubtle)
                          ) {
                            Text("Try again", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                          }
                        } else {
                          OutlinedButton(
                            onClick = {
                              val next = !notifyWhenOnlineEnabled
                              notifyWhenOnlineEnabled = next
                              context.getSharedPreferences("lingosphere_connect_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("notify_when_online", next)
                                .apply()
                              Toast.makeText(
                                context,
                                if (next) "We'll notify you when a partner comes online." else "Online alerts turned off.",
                                Toast.LENGTH_SHORT
                              ).show()
                            },
                            modifier = Modifier
                              .fillMaxWidth()
                              .heightIn(min = 48.dp)
                              .testTag("connect_notify_online_btn"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                              1.dp,
                              if (notifyWhenOnlineEnabled) LingoTokens.brandCyan.copy(alpha = 0.6f) else LingoTokens.borderSubtle
                            )
                          ) {
                            Icon(
                              imageVector = if (notifyWhenOnlineEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                              contentDescription = null,
                              tint = if (notifyWhenOnlineEnabled) LingoTokens.brandCyan else LingoTokens.textPrimary,
                              modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                              text = if (notifyWhenOnlineEnabled) "Alert enabled" else "Notify me when someone is online",
                              color = if (notifyWhenOnlineEnabled) LingoTokens.brandCyan else LingoTokens.textPrimary,
                              fontSize = 14.sp,
                              fontWeight = FontWeight.SemiBold,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                          }
                        }

                        Surface(
                          onClick = onStartAiVoiceCall,
                          shape = RoundedCornerShape(16.dp),
                          color = LingoTokens.bgGlassCard,
                          border = BorderStroke(
                            1.dp,
                            if (userProfile.isEntitlementVerified) LingoTokens.brandCyan.copy(alpha = 0.55f)
                            else LingoTokens.accentGold.copy(alpha = 0.55f)
                          ),
                          modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("connect_empty_ai_btn")
                        ) {
                          Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                          ) {
                            if (!userProfile.isEntitlementVerified) {
                              LingoGoldLockIcon(size = 16.dp)
                              Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                              text = "Practice with AI",
                              color = if (userProfile.isEntitlementVerified) LingoTokens.brandCyan else LingoTokens.accentGold,
                              fontWeight = FontWeight.Bold,
                              fontSize = 14.sp
                            )
                          }
                        }
                      }
                    }
                  }
                }
              } else {
                items(paginatedPartners, key = { it.userId }) { partner ->
                  val compatibilityScore = remember(userProfile, myCommunityProfile, partner) {
                    calculateRealMatchScore(userProfile, myCommunityProfile, partner)
                  }
                  val friendStatus = remember(friendRequests, userProfile.userId, partner.userId) {
                    val existing = friendRequests.firstOrNull {
                      (it.senderId == userProfile.userId && it.receiverId == partner.userId) ||
                        (it.senderId == partner.userId && it.receiverId == userProfile.userId)
                    }
                    when {
                      existing == null -> "none"
                      existing.status == "accepted" -> "accepted"
                      existing.status == "pending" && existing.senderId == userProfile.userId -> "outgoing_pending"
                      existing.status == "pending" && existing.receiverId == userProfile.userId -> "incoming_pending"
                      else -> "none"
                    }
                  }
                  val myInterestsSet = remember(myCommunityProfile) {
                    (myCommunityProfile?.interests ?: listOf("Travel", "Culture", "Music"))
                      .map { it.lowercase().trim() }
                      .toSet()
                  }

                  PartnerCard(
                    partner = partner,
                    compatibilityPercent = compatibilityScore,
                    isExactTandem = partner.nativeLanguageCode == userProfile.targetLanguageCode &&
                      partner.targetLanguageCode == userProfile.nativeLanguageCode,
                    sharedInterestsLower = myInterestsSet,
                    friendStatus = friendStatus,
                    onOpenProfile = { inspectedPartner = partner },
                    onChat = {
                      if (blockedIds.contains(partner.userId)) {
                        Toast.makeText(context, "Unblock ${partner.displayName} before chatting.", Toast.LENGTH_SHORT).show()
                      } else {
                        runWithCallSafetyGuards(isCallAction = false) {
                          activeChatPartner = partner
                        }
                      }
                    },
                    onVoiceCall = {
                      initiatePartnerCall(partner, friendStatus, "voice")
                    },
                    onVideoCall = {
                      initiatePartnerCall(partner, friendStatus, "video")
                    },
                    onSendInvite = {
                      runWithCallSafetyGuards(isCallAction = false) {
                        viewModel.sendSpeakingInvitation(partner.userId, partner.displayName) {
                          Toast.makeText(context, "Practice invite sent to ${partner.displayName}.", Toast.LENGTH_SHORT).show()
                        }
                      }
                    },
                    onFriendAction = {
                      runWithCallSafetyGuards(isCallAction = false) {
                        val existingReq = friendRequests.firstOrNull {
                          (it.senderId == userProfile.userId && it.receiverId == partner.userId) ||
                            (it.senderId == partner.userId && it.receiverId == userProfile.userId)
                        }
                        when (friendStatus) {
                          "incoming_pending" -> {
                            if (existingReq != null) {
                              viewModel.respondToFriendRequest(existingReq.id, accept = true)
                              Toast.makeText(context, "Accepted ${partner.displayName}'s friend request!", Toast.LENGTH_SHORT).show()
                            }
                          }
                          "outgoing_pending" -> {
                            if (existingReq != null) {
                              viewModel.cancelOrRemoveFriendRequest(existingReq.id)
                              Toast.makeText(context, "Cancelled request to ${partner.displayName}", Toast.LENGTH_SHORT).show()
                            }
                          }
                          "accepted" -> {
                            Toast.makeText(context, "You and ${partner.displayName} are connected friends!", Toast.LENGTH_SHORT).show()
                          }
                          else -> {
                            viewModel.sendFriendRequest(partner.userId, partner.displayName) {
                              Toast.makeText(context, "Friend request sent to ${partner.displayName}!", Toast.LENGTH_SHORT).show()
                            }
                          }
                        }
                      }
                    },
                    onReport = { reportingPartner = partner },
                    onBlock = {
                      viewModel.blockUser(partner.userId, partner.displayName) {
                        Toast.makeText(context, "${partner.displayName} has been blocked.", Toast.LENGTH_SHORT).show()
                      }
                    }
                  )
                }

                // Progressive disclosure: Show 4 then "See all" / Load More
                if (filteredPartners.size > visiblePartnerLimit || hasMorePartners) {
                  item(key = "load_more_partners") {
                    val remainingLocal = (filteredPartners.size - visiblePartnerLimit).coerceAtLeast(0)
                    OutlinedButton(
                      onClick = {
                        visiblePartnerLimit = maxOf(filteredPartners.size, visiblePartnerLimit + 15)
                        if (hasMorePartners) {
                          viewModel.loadMorePartners()
                        }
                      },
                      enabled = !isPartnersLoading,
                      modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("load_more_partners_button"),
                      shape = RoundedCornerShape(14.dp),
                      border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.7f))
                    ) {
                      if (isPartnersLoading) {
                        CircularProgressIndicator(
                          color = LingoTokens.brandCyan,
                          strokeWidth = 2.dp,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                      }
                      Text(
                        text = when {
                          isPartnersLoading -> "Loading more partners..."
                          remainingLocal > 0 && hasMorePartners -> "See all (${filteredPartners.size}+)"
                          remainingLocal > 0 -> "See all (${filteredPartners.size})"
                          else -> "Load more partners"
                        },
                        color = LingoTokens.brandCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                }
              }
            }
          }
        }

          // TAB 2: FRIENDS & REQUESTS
          2 -> {
            val filteredFriends = remember(friendsList, friendsSearchQuery) {
              val q = friendsSearchQuery.trim().lowercase()
              if (q.isEmpty()) friendsList
              else friendsList.filter { f ->
                f.displayName.lowercase().contains(q) || f.country.lowercase().contains(q)
              }
            }
            LazyColumn(
              state = friendsListState,
              modifier = Modifier
                .fillMaxSize()
                .testTag("friends_list"),
              contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = LingoTokens.bottomBarClearance),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              item(key = "friends_search") {
                OutlinedTextField(
                  value = friendsSearchQuery,
                  onValueChange = { friendsSearchQuery = it },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friends_search_input"),
                  placeholder = {
                    Text(
                      text = "Search friends...",
                      color = LingoTokens.textSecondary,
                      fontSize = 14.sp
                    )
                  },
                  leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search friends", tint = LingoTokens.textSecondary, modifier = Modifier.size(20.dp))
                  },
                  trailingIcon = {
                    if (friendsSearchQuery.isNotEmpty()) {
                      IconButton(onClick = { friendsSearchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = LingoTokens.textSecondary, modifier = Modifier.size(18.dp))
                      }
                    }
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(18.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LingoTokens.textPrimary,
                    unfocusedTextColor = LingoTokens.textPrimary,
                    focusedContainerColor = LingoTokens.bgGlassCard,
                    unfocusedContainerColor = LingoTokens.bgGlassCard,
                    focusedBorderColor = LingoTokens.brandCyan.copy(alpha = 0.65f),
                    unfocusedBorderColor = LingoTokens.borderSubtle
                  )
                )
              }

              if (incomingRequests.isNotEmpty()) {
                item(key = "incoming_header") {
                  Text(
                    text = "Friend requests (${incomingRequests.size})",
                    color = LingoTokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                  )
                }
                items(incomingRequests, key = { it.id }) { req ->
                  LingoCleanCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = req.senderName.ifBlank { "Language Partner" },
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 15.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "Wants to practice together",
                          color = LingoTokens.textSecondary,
                          fontSize = 13.sp,
                          maxLines = 1
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                          onClick = { viewModel.respondToFriendRequest(req.id, true) },
                          modifier = Modifier.size(48.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(40.dp)
                              .clip(CircleShape)
                              .background(LingoTokens.statusOnline.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(Icons.Default.Check, contentDescription = "Accept Friend Request", tint = LingoTokens.statusOnline)
                          }
                        }
                        IconButton(
                          onClick = { viewModel.respondToFriendRequest(req.id, false) },
                          modifier = Modifier.size(48.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(40.dp)
                              .clip(CircleShape)
                              .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(Icons.Default.Close, contentDescription = "Decline Friend Request", tint = LingoTokens.textSecondary)
                          }
                        }
                      }
                    }
                  }
                }
              }

              if (outgoingPendingRequests.isNotEmpty()) {
                item(key = "outgoing_header") {
                  Text(
                    text = "Sent requests (${outgoingPendingRequests.size})",
                    color = LingoTokens.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                  )
                }
                items(outgoingPendingRequests, key = { "out_${it.id}" }) { req ->
                  LingoCleanCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = req.receiverName.ifBlank { "Language Partner" },
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 14.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "Pending response",
                          color = LingoTokens.textSecondary,
                          fontSize = 12.sp
                        )
                      }
                      TextButton(onClick = { viewModel.cancelOrRemoveFriendRequest(req.id) }) {
                        Text("Cancel", color = LingoTokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                      }
                    }
                  }
                }
              }

              item(key = "friends_header") {
                Text(
                  text = "Friends (${filteredFriends.size})",
                  color = LingoTokens.textPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 18.sp
                )
              }

              if (filteredFriends.isEmpty()) {
                item(key = "empty_friends") {
                  LingoCleanCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = LingoTokens.brandCyan,
                        modifier = Modifier.size(34.dp)
                      )
                      Text(
                        text = if (friendsSearchQuery.isNotBlank()) "No matching friends" else "No friends yet",
                        color = LingoTokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                      )
                      Text(
                        text = "Add partners you like to keep practicing together.",
                        color = LingoTokens.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                      )
                      OutlinedButton(
                        onClick = {
                          friendsSearchQuery = ""
                          selectedTab = 0
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                        modifier = Modifier.heightIn(min = 48.dp)
                      ) {
                        Text("Browse partners", color = LingoTokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                      }
                    }
                  }
                }
              } else {
                items(filteredFriends, key = { "friend_${it.userId}" }) { friend ->
                  val compatibilityScore = remember(userProfile, myCommunityProfile, friend) {
                    calculateRealMatchScore(userProfile, myCommunityProfile, friend)
                  }
                  val myInterestsSet = remember(myCommunityProfile) {
                    (myCommunityProfile?.interests ?: listOf("Travel", "Culture", "Music"))
                      .map { it.lowercase().trim() }
                      .toSet()
                  }

                  PartnerCard(
                    partner = friend,
                    compatibilityPercent = compatibilityScore,
                    isExactTandem = friend.nativeLanguageCode == userProfile.targetLanguageCode &&
                      friend.targetLanguageCode == userProfile.nativeLanguageCode,
                    sharedInterestsLower = myInterestsSet,
                    friendStatus = "accepted",
                    onOpenProfile = { inspectedPartner = friend },
                    onChat = {
                      if (blockedIds.contains(friend.userId)) {
                        Toast.makeText(context, "Unblock ${friend.displayName} before chatting.", Toast.LENGTH_SHORT).show()
                      } else {
                        runWithCallSafetyGuards(isCallAction = false) { activeChatPartner = friend }
                      }
                    },
                    onVoiceCall = {
                      initiatePartnerCall(friend, "accepted", "voice")
                    },
                    onVideoCall = {
                      initiatePartnerCall(friend, "accepted", "video")
                    },
                    onSendInvite = {
                      runWithCallSafetyGuards(isCallAction = false) {
                        viewModel.sendSpeakingInvitation(friend.userId, friend.displayName) {
                          Toast.makeText(context, "Practice invite sent to ${friend.displayName}.", Toast.LENGTH_SHORT).show()
                        }
                      }
                    },
                    onFriendAction = {
                      Toast.makeText(context, "${friend.displayName} is in your friends list", Toast.LENGTH_SHORT).show()
                    },
                    onReport = { reportingPartner = friend },
                    onBlock = {
                      viewModel.blockUser(friend.userId, friend.displayName)
                    }
                  )
                }
              }
            }
          }

          // TAB 3: CALL HISTORY
          3 -> {
            LazyColumn(
              state = callsListState,
              modifier = Modifier
                .fillMaxSize()
                .testTag("call_history_list"),
              contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = LingoTokens.bottomBarClearance),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              item(key = "calls_header") {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Recent calls",
                    color = LingoTokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                  )
                  if (callHistory.isNotEmpty()) {
                    Text(
                      text = "${callHistory.size} total",
                      color = LingoTokens.textSecondary,
                      fontSize = 13.sp
                    )
                  }
                }
              }

              if (callHistory.isEmpty()) {
                item(key = "empty_calls") {
                  LingoCleanCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = LingoTokens.textSecondary,
                        modifier = Modifier.size(34.dp)
                      )
                      Text(
                        text = "No calls yet.",
                        color = LingoTokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                      )
                      Text(
                        text = "Your voice and video calls with language partners will appear here.",
                        color = LingoTokens.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }
              } else {
                items(callHistory, key = { it.callId.ifBlank { it.id } }) { record ->
                  CallHistoryRowCard(
                    record = record,
                    currentUserId = userProfile.userId,
                    onClickRow = { callbackSheetRecord = record },
                    onRedial = { partnerId, partnerName, callType ->
                      if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
                        runWithCallSafetyGuards(isCallAction = true) {
                          scope.launch {
                            try {
                              val room = viewModel.startOrJoinCallRoom(
                                calleeId = partnerId,
                                calleeName = partnerName,
                                callType = callType,
                                matchMode = "direct"
                              )
                              onStartLiveCall(room)
                            } catch (e: Exception) {
                              Toast.makeText(
                                context,
                                com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
                                  e.localizedMessage,
                                  isPro = userProfile.isEntitlementVerified,
                                  fallback = "Unable to call right now."
                                ),
                                Toast.LENGTH_SHORT
                              ).show()
                            }
                          }
                        }
                      }
                    }
                  )
                }
              }
            }
          }

          // TAB 4: LEADERBOARD
          4 -> {
            LazyColumn(
              state = leaderboardListState,
              modifier = Modifier
                .fillMaxSize()
                .testTag("leaderboard_list"),
              contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = LingoTokens.bottomBarClearance),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              item(key = "leaderboard_header") {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = LingoTokens.accentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Top XP Leaderboard",
                      color = LingoTokens.textPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      maxLines = 2
                    )
                  }
                  TextButton(onClick = { selectedTab = 0 }) {
                    Text("Partners", color = LingoTokens.brandCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  }
                }
              }

              itemsIndexed(leaderboardList, key = { _, p -> p.userId }) { idx, p ->
                val isMe = p.userId == userProfile.userId
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isMe) Color(0xFF152C5B) else Color(0xFF101C36)
                  ),
                  border = if (isMe) BorderStroke(1.dp, Color(0xFF06B6D4)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = when (idx) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "#${idx + 1}"
                      },
                      color = Color(0xFFFBBF24),
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 15.sp,
                      modifier = Modifier.width(34.dp)
                    )
                    Text(
                      text = p.countryFlag,
                      fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = p.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "Lv.${p.level} • 🔥 ${p.currentStreak}d streak • Learning ${Language.findByCode(p.targetLanguageCode).name}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "${p.xpPoints} XP",
                      color = Color(0xFF38BDF8),
                      fontWeight = FontWeight.ExtraBold,
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
      }
    }
  }

  // Partner Profile Modal Dialog
  if (inspectedPartner != null) {
    val partner = inspectedPartner!!
    val realMatch = calculateRealMatchScore(userProfile, myCommunityProfile, partner)
    PartnerProfileDialog(
      partner = partner,
      compatibilityPercent = realMatch,
      onDismiss = { inspectedPartner = null },
      onChat = {
        inspectedPartner = null
        if (blockedIds.contains(partner.userId)) {
          Toast.makeText(context, "Unblock ${partner.displayName} before chatting.", Toast.LENGTH_SHORT).show()
        } else {
          activeChatPartner = partner
        }
      },
      onVoiceCall = {
        val partnerFriendStatus = friendRequests.firstOrNull {
          (it.senderId == userProfile.userId && it.receiverId == partner.userId) ||
            (it.senderId == partner.userId && it.receiverId == userProfile.userId)
        }?.status ?: "none"
        inspectedPartner = null
        initiatePartnerCall(partner, partnerFriendStatus, "voice")
      },
      onVideoCall = {
        val partnerFriendStatus = friendRequests.firstOrNull {
          (it.senderId == userProfile.userId && it.receiverId == partner.userId) ||
            (it.senderId == partner.userId && it.receiverId == userProfile.userId)
        }?.status ?: "none"
        inspectedPartner = null
        initiatePartnerCall(partner, partnerFriendStatus, "video")
      },
      onAddFriend = {
        viewModel.sendFriendRequest(partner.userId, partner.displayName) {
          Toast.makeText(context, "Friend request sent to ${partner.displayName}!", Toast.LENGTH_SHORT).show()
        }
        inspectedPartner = null
      },
      onSendSpeakingInvitation = {
        runWithCallSafetyGuards(isCallAction = false) {
          viewModel.sendSpeakingInvitation(partner.userId, partner.displayName) {
            Toast.makeText(context, "Speaking invitation sent to ${partner.displayName}!", Toast.LENGTH_SHORT).show()
          }
        }
        inspectedPartner = null
      },
      onReport = {
        inspectedPartner = null
        reportingPartner = partner
      },
      onBlock = {
        inspectedPartner = null
        viewModel.blockUser(partner.userId, partner.displayName) {
          Toast.makeText(context, "${partner.displayName} has been blocked.", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  // Invite Required Safety Gate Dialog before calling a non-friend partner
  inviteRequiredPartner?.let { (partner, _) ->
    AlertDialog(
      onDismissRequest = { inviteRequiredPartner = null },
      containerColor = Color(0xFF101828),
      titleContentColor = LingoTokens.textPrimary,
      textContentColor = LingoTokens.textSecondary,
      modifier = Modifier.testTag("call_invite_required_dialog"),
      title = {
        Text(
          text = "Connect before calling",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Text(
          text = "Send an invite first to start a call",
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            inviteRequiredPartner = null
            runWithCallSafetyGuards(isCallAction = false) {
              viewModel.sendFriendRequest(partner.userId, partner.displayName)
              viewModel.sendSpeakingInvitation(partner.userId, partner.displayName) {
                Toast.makeText(context, "Invite sent to ${partner.displayName}.", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoTokens.brandCyan,
            contentColor = LingoTokens.onGradientInk
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("call_invite_required_send_btn")
        ) {
          Text("Send invite", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { inviteRequiredPartner = null }) {
          Text("Cancel", color = LingoTokens.textSecondary)
        }
      }
    )
  }

  // Report User Modal Dialog
  if (reportingPartner != null) {
    val partner = reportingPartner!!
    ReportUserDialog(
      partnerName = partner.displayName,
      onDismiss = { reportingPartner = null },
      onSubmitReport = { reason, details ->
        viewModel.reportUser(partner.userId, reason, details) {
          Toast.makeText(context, "Report submitted. Thank you for keeping LingoSphere safe.", Toast.LENGTH_LONG).show()
        }
        reportingPartner = null
      }
    )
  }

  // Privacy & Blocked Users Modal Dialog
  if (showPrivacySafetyDialog) {
    PrivacyAndSafetyDialog(
      userProfile = userProfile,
      myCommunityProfile = myCommunityProfile,
      showLastSeen = accountSettings.showLastSeen,
      whoCanCallMe = accountSettings.whoCanCallMe,
      doNotDisturb = accountSettings.doNotDisturb,
      blockedUsers = blockedUsers,
      onUpdateSettings = { isOnline, allowCalls, showLastSeenFlag, whoCanCall, dnd ->
        viewModel.updateAccountSettings(
          accountSettings.copy(
            isOnlineVisible = isOnline,
            allowIncomingCalls = allowCalls && !dnd && whoCanCall != "nobody",
            showLastSeen = showLastSeenFlag,
            whoCanCallMe = whoCanCall,
            doNotDisturb = dnd
          )
        )
        viewModel.updateCommunitySettings(
          displayName = userProfile.displayName,
          country = myCommunityProfile?.country ?: "United States",
          countryFlag = myCommunityProfile?.countryFlag ?: "🇺🇸",
          bio = myCommunityProfile?.bio ?: "Practicing languages on LingoSphere",
          interests = myCommunityProfile?.interests ?: listOf("Travel", "Culture", "Music"),
          isOnline = isOnline,
          allowCalls = allowCalls && !dnd && whoCanCall != "nobody"
        )
        Toast.makeText(context, "Safety settings updated.", Toast.LENGTH_SHORT).show()
      },
      onUnblockUser = { blockId ->
        viewModel.unblockUser(blockId)
        Toast.makeText(context, "User unblocked.", Toast.LENGTH_SHORT).show()
      },
      onDismiss = { showPrivacySafetyDialog = false }
    )
  }

  // Smart Match Preferences Bottom Sheet
  if (showMatchPreferencesSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMatchPreferencesSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color(0xFF0E1525),
      contentColor = LingoTokens.textPrimary
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .padding(bottom = 28.dp)
          .testTag("smart_match_preferences_sheet"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Match preferences",
          color = LingoTokens.textPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("anyone" to "Anyone", "country" to "Country", "interest" to "Interest").forEach { (id, label) ->
            val selected = smartMatchChoice == id
            Surface(
              onClick = { smartMatchChoice = id },
              shape = RoundedCornerShape(14.dp),
              color = if (selected) LingoTokens.brandIndigo.copy(alpha = 0.28f) else LingoTokens.bgGlassCard,
              border = BorderStroke(1.dp, if (selected) LingoTokens.brandCyan else LingoTokens.borderSubtle),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .testTag("smart_match_segment_$id")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = label,
                  color = if (selected) LingoTokens.textPrimary else LingoTokens.textSecondary,
                  fontSize = 14.sp,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }
        }

        when (smartMatchChoice) {
          "country" -> {
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CountryInfo.COUNTRIES.take(10).forEach { c ->
                val isSel = selectedCountryMatch == c.name
                FilterChip(
                  selected = isSel,
                  onClick = { selectedCountryMatch = c.name },
                  label = { Text("${c.flagEmoji} ${c.name}", fontSize = 13.sp) }
                )
              }
            }
          }
          "interest" -> {
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CountryInfo.INTEREST_TOPICS.take(10).forEach { topic ->
                val isSel = selectedInterestMatch == topic
                FilterChip(
                  selected = isSel,
                  onClick = { selectedInterestMatch = topic },
                  label = { Text(topic, fontSize = 13.sp) }
                )
              }
            }
          }
          else -> {
            val targetName = Language.findByCode(userProfile.targetLanguageCode).name
            Text(
              text = "Global queue · Practicing $targetName · Level ${userProfile.proficiencyLevel}",
              color = LingoTokens.textSecondary,
              fontSize = 14.sp
            )
          }
        }

        LingoPrimaryGradientButton(
          text = "Done",
          onClick = { showMatchPreferencesSheet = false },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }

  // Call-Back Sheet when tapping a row in Calls history
  callbackSheetRecord?.let { rec ->
    val isOutgoing = rec.callerId == userProfile.userId
    val pId = if (isOutgoing) rec.receiverId else rec.callerId
    val pName = if (isOutgoing) rec.receiverName.ifBlank { "Language Partner" } else rec.callerName.ifBlank { "Language Partner" }
    ModalBottomSheet(
      onDismissRequest = { callbackSheetRecord = null },
      containerColor = Color(0xFF0E1525),
      contentColor = LingoTokens.textPrimary
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Call $pName back",
          color = LingoTokens.textPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          LingoPrimaryGradientButton(
            text = "Voice call",
            icon = Icons.Default.Call,
            onClick = {
              callbackSheetRecord = null
              if (pId.isNotBlank() && pId != "waiting_partner") {
                runWithCallSafetyGuards(isCallAction = true) {
                  scope.launch {
                    try {
                      val room = viewModel.startOrJoinCallRoom(calleeId = pId, calleeName = pName, callType = "voice", matchMode = "direct")
                      onStartLiveCall(room)
                    } catch (_: Exception) {
                      Toast.makeText(context, "Unable to call right now.", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              }
            },
            modifier = Modifier.weight(1f)
          )
          OutlinedButton(
            onClick = {
              callbackSheetRecord = null
              if (pId.isNotBlank() && pId != "waiting_partner") {
                runWithCallSafetyGuards(isCallAction = true) {
                  scope.launch {
                    try {
                      val room = viewModel.startOrJoinCallRoom(calleeId = pId, calleeName = pName, callType = "video", matchMode = "direct")
                      onStartLiveCall(room)
                    } catch (_: Exception) {
                      Toast.makeText(context, "Unable to call right now.", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              }
            },
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 50.dp)
          ) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = LingoTokens.textPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Video call", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }

  // First-time Respectful Behavior Notice before calling
  if (pendingRespectfulAction != null) {
    AlertDialog(
      onDismissRequest = { pendingRespectfulAction = null },
      containerColor = Color(0xFF0E1525),
      titleContentColor = LingoTokens.textPrimary,
      textContentColor = LingoTokens.textSecondary,
      title = {
        Text("Respectful community", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Text(
          text = "LingoSphere calls are for friendly language practice. Please be respectful, avoid sharing private personal details, and use Block or Report anytime you feel uncomfortable.",
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            hasAcceptedRespectfulNotice = true
            context.getSharedPreferences("lingosphere_connect_prefs", Context.MODE_PRIVATE)
              .edit()
              .putBoolean("respectful_notice_accepted", true)
              .apply()
            val action = pendingRespectfulAction
            pendingRespectfulAction = null
            action?.invoke()
          },
          colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.brandCyan, contentColor = LingoTokens.onGradientInk)
        ) {
          Text("I agree", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingRespectfulAction = null }) {
          Text("Not now", color = LingoTokens.textSecondary)
        }
      }
    )
  }

  // Logged-out Guard Dialog
  if (showSignInPromptDialog) {
    AlertDialog(
      onDismissRequest = { showSignInPromptDialog = false },
      containerColor = Color(0xFF0E1525),
      titleContentColor = LingoTokens.textPrimary,
      textContentColor = LingoTokens.textSecondary,
      title = {
        Text("Sign in to continue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Text(
          text = "Live voice calls, video calls, and partner chats are free once you sign in to your account.",
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { showSignInPromptDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.brandCyan, contentColor = LingoTokens.onGradientInk)
        ) {
          Text("Got it", fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  // Partner Filters Modal Bottom Sheet
  if (showFiltersSheet) {
    ModalBottomSheet(
      onDismissRequest = { showFiltersSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = LingoTokens.bgElevated,
      contentColor = LingoTokens.textPrimary
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = LingoTokens.screenMargin, vertical = 8.dp)
          .padding(bottom = 24.dp)
          .testTag("connect_filter_bottom_sheet"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Filter Partners",
            color = LingoTokens.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Reset all",
            color = LingoTokens.brandCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
              filterLanguageCode = "ALL"
              filterCountry = "ALL"
              filterLevel = "ALL"
              filterInterest = "ALL"
              onlineOnlyFilter = false
              visiblePartnerLimit = 4
            }
          )
        }

        // Online Only Toggle
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = LingoTokens.bgSubtle,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Online Only",
                color = LingoTokens.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "$onlineCount partner${if (onlineCount == 1) "" else "s"} active right now",
                color = LingoTokens.textSecondary,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = onlineOnlyFilter,
              onCheckedChange = {
                onlineOnlyFilter = it
                visiblePartnerLimit = 4
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = LingoTokens.onGradientInk,
                checkedTrackColor = LingoTokens.statusOnline
              )
            )
          }
        }

        // Language Filter
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "LANGUAGE",
            color = LingoTokens.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("language_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item(key = "sheet_lang_ALL") {
              FilterChip(
                selected = filterLanguageCode == "ALL",
                onClick = { filterLanguageCode = "ALL"; visiblePartnerLimit = 4 },
                label = { Text("All Languages", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandCyan,
                  selectedLabelColor = LingoTokens.onGradientInk,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textPrimary
                )
              )
            }
            item(key = "sheet_lang_TANDEM") {
              val targetLang = Language.findByCode(userProfile.targetLanguageCode)
              val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)
              FilterChip(
                selected = filterLanguageCode == "TANDEM",
                onClick = { filterLanguageCode = "TANDEM"; visiblePartnerLimit = 4 },
                label = { Text("Tandem (${targetLang.code.uppercase()} ⇄ ${nativeLang.code.uppercase()})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandCyan,
                  selectedLabelColor = LingoTokens.onGradientInk,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textPrimary
                )
              )
            }
            items(Language.SUPPORTED_LANGUAGES, key = { "sheet_lang_${it.code}" }) { lang ->
              FilterChip(
                selected = filterLanguageCode == lang.code,
                onClick = { filterLanguageCode = lang.code; visiblePartnerLimit = 4 },
                label = { Text(lang.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandCyan,
                  selectedLabelColor = LingoTokens.onGradientInk,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textPrimary
                )
              )
            }
          }
        }

        // Country Filter
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "COUNTRY",
            color = LingoTokens.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("country_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item(key = "sheet_country_ALL") {
              FilterChip(
                selected = filterCountry == "ALL",
                onClick = { filterCountry = "ALL"; visiblePartnerLimit = 4 },
                label = { Text("All Countries", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandIndigo,
                  selectedLabelColor = Color.White,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textSecondary
                )
              )
            }
            items(CountryInfo.COUNTRIES, key = { "sheet_country_${it.name}" }) { c ->
              FilterChip(
                selected = filterCountry.equals(c.name, ignoreCase = true),
                onClick = {
                  filterCountry = if (filterCountry == c.name) "ALL" else c.name
                  visiblePartnerLimit = 4
                },
                label = { Text("${c.flagEmoji} ${c.name}", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandIndigo,
                  selectedLabelColor = Color.White,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textPrimary
                )
              )
            }
          }
        }

        // Level Filter (Wrapping FlowRow so chips are never clipped)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "LEVEL",
            color = LingoTokens.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("level_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("ALL" to "All Levels", "Beginner" to "Beginner", "Intermediate" to "Intermediate", "Advanced" to "Advanced", "Native" to "Native").forEach { (key, label) ->
              val isSel = filterLevel == key
              FilterChip(
                selected = isSel,
                onClick = {
                  filterLevel = key
                  visiblePartnerLimit = 4
                },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandCyan,
                  selectedLabelColor = LingoTokens.onGradientInk,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textPrimary
                )
              )
            }
          }
        }

        // Interest Filter
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "INTEREST",
            color = LingoTokens.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("interest_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item(key = "sheet_interest_ALL") {
              FilterChip(
                selected = filterInterest == "ALL",
                onClick = { filterInterest = "ALL"; visiblePartnerLimit = 4 },
                label = { Text("All Interests", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandIndigo,
                  selectedLabelColor = Color.White,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textSecondary
                )
              )
            }
            items(CountryInfo.INTEREST_TOPICS, key = { "sheet_interest_$it" }) { topic ->
              FilterChip(
                selected = filterInterest.equals(topic, ignoreCase = true),
                onClick = {
                  filterInterest = if (filterInterest.equals(topic, ignoreCase = true)) "ALL" else topic
                  visiblePartnerLimit = 4
                },
                label = { Text(topic, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.brandIndigo,
                  selectedLabelColor = Color.White,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = LingoTokens.textPrimary
                )
              )
            }
          }
        }

        LingoPrimaryGradientButton(
          text = "Show ${filteredPartners.size} Partner${if (filteredPartners.size == 1) "" else "s"}",
          onClick = { showFiltersSheet = false },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

private fun isLoadingPartnersOnly(isLoading: Boolean, isEmpty: Boolean): Boolean = isLoading && isEmpty

@Composable
internal fun PartnerCard(
  partner: CommunityProfile,
  compatibilityPercent: Int?,
  isExactTandem: Boolean,
  sharedInterestsLower: Set<String>,
  friendStatus: String,
  onOpenProfile: () -> Unit,
  onChat: () -> Unit,
  onVoiceCall: () -> Unit,
  onVideoCall: () -> Unit,
  onSendInvite: () -> Unit = {},
  onFriendAction: () -> Unit,
  onReport: () -> Unit,
  onBlock: () -> Unit
) {
  val haptics = LocalHapticFeedback.current
  val nowMs = System.currentTimeMillis()
  val isPartnerOnline = partner.isEffectivelyOnline(nowMs)
  var showCardMenu by remember { mutableStateOf(false) }

  LingoCleanCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("partner_card_${partner.userId}"),
    onClick = onOpenProfile,
    contentPadding = 16.dp
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Top Row: Avatar with green/grey dot + 1-line Name + Flag/Level + Match % + Safety menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LingoAvatar(
          name = partner.displayName,
          userId = partner.userId,
          size = 48.dp,
          customImageUrl = partner.avatarUrl,
          isOnline = isPartnerOnline
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = partner.displayName.ifBlank { "Language Partner" },
            color = LingoTokens.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(2.dp))

          val subtitleParts = buildList {
            if (partner.country.isNotBlank()) add("${partner.countryFlag} ${partner.country}")
            if (partner.proficiencyLevel.isNotBlank()) add(partner.proficiencyLevel)
          }.joinToString(" · ")

          if (subtitleParts.isNotBlank()) {
            Text(
              text = subtitleParts,
              color = LingoTokens.textSecondary,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (compatibilityPercent != null) {
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = LingoTokens.bgGlassCard,
            border = BorderStroke(
              1.dp,
              if (isExactTandem) LingoTokens.brandCyan.copy(alpha = 0.5f) else LingoTokens.borderSubtle
            )
          ) {
            Text(
              text = "$compatibilityPercent%",
              color = LingoTokens.brandCyan,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
          }
        }

        Box {
          IconButton(
            onClick = { showCardMenu = true },
            modifier = Modifier
              .size(48.dp)
              .testTag("partner_menu_${partner.userId}")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Partner options for ${partner.displayName}",
              tint = LingoTokens.textSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
          DropdownMenu(
            expanded = showCardMenu,
            onDismissRequest = { showCardMenu = false },
            containerColor = Color(0xFF101828)
          ) {
            DropdownMenuItem(
              text = {
                Text("Report", color = LingoTokens.textPrimary, fontSize = 14.sp)
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Flag,
                  contentDescription = null,
                  tint = LingoAmber,
                  modifier = Modifier.size(18.dp)
                )
              },
              onClick = {
                showCardMenu = false
                onReport()
              },
              modifier = Modifier.testTag("partner_menu_report_${partner.userId}")
            )
            DropdownMenuItem(
              text = {
                Text("Block", color = LingoCoral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Block,
                  contentDescription = null,
                  tint = LingoCoral,
                  modifier = Modifier.size(18.dp)
                )
              },
              onClick = {
                showCardMenu = false
                onBlock()
              },
              modifier = Modifier.testTag("partner_menu_block_${partner.userId}")
            )
          }
        }
      }

      // 1-line Partner Bio (ellipsis)
      if (partner.bio.isNotBlank()) {
        Text(
          text = partner.bio,
          color = LingoTokens.textSecondary,
          fontSize = 13.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 19.sp
        )
      }

      // When not yet friends, show Send invite cleanly above the 3-action row so 360dp phones never overflow
      if (friendStatus != "accepted") {
        val inviteLabel = when (friendStatus) {
          "outgoing_pending" -> "Invite sent"
          "incoming_pending" -> "Accept invite"
          else -> "Send invite"
        }
        OutlinedButton(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (friendStatus == "incoming_pending") {
              onFriendAction()
            } else {
              onSendInvite()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("partner_invite_${partner.userId}"),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0E1B33)
          ),
          border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.55f))
        ) {
          Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = inviteLabel,
            color = LingoTokens.brandCyan,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Always show Chat + Voice + Video cleanly in 3 equal buttons on every Partner & Friend card
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChat()
          },
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("partner_chat_${partner.userId}"),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0E1729)
          ),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = "Chat with ${partner.displayName}",
            tint = LingoTokens.textPrimary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Chat",
            color = LingoTokens.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onVoiceCall()
          },
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("partner_call_${partner.userId}"),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0D1E36)
          ),
          border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.65f))
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Voice call ${partner.displayName}",
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Voice",
            color = LingoTokens.brandCyan,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onVideoCall()
          },
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("partner_video_${partner.userId}"),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0D1E36)
          ),
          border = BorderStroke(1.dp, LingoTokens.brandIndigo.copy(alpha = 0.72f))
        ) {
          Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = "Video call ${partner.displayName}",
            tint = Color(0xFF93C5FD),
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Video",
            color = Color(0xFF93C5FD),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun MatchModeCard(
  title: String,
  subtitle: String,
  badgeText: String,
  accentColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isBusy: Boolean,
  cardTestTag: String,
  voiceBtnTestTag: String,
  videoBtnTestTag: String,
  onStartVoice: () -> Unit,
  onStartVideo: () -> Unit,
  selectorContent: (@Composable () -> Unit)? = null
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(cardTestTag),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF101C36)),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.18f))
              .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(20.dp)
            )
          }
          Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            maxLines = 2
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = accentColor.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
          Text(
            text = badgeText,
            color = accentColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Text(
        text = subtitle,
        color = Color(0xFFCBD5E1),
        fontSize = 13.sp,
        lineHeight = 18.sp
      )

      if (selectorContent != null) {
        selectorContent()
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onStartVoice,
          enabled = !isBusy,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag(voiceBtnTestTag),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = Color(0xFF04101E)
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF04101E))
          } else {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Voice Match",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }
        OutlinedButton(
          onClick = onStartVideo,
          enabled = !isBusy,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag(videoBtnTestTag),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.85f)),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(Icons.Default.Videocam, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Video Match",
            color = accentColor,
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

@Composable
private fun CallHistoryRowCard(
  record: CallHistoryRecord,
  currentUserId: String,
  onClickRow: () -> Unit = {},
  onRedial: (partnerId: String, partnerName: String, callType: String) -> Unit
) {
  val isOutgoing = record.callerId == currentUserId
  val partnerId = if (isOutgoing) record.receiverId else record.callerId
  val partnerName = if (isOutgoing) {
    record.receiverName.ifBlank { "Language Partner" }
  } else {
    record.callerName.ifBlank { "Language Partner" }
  }

  // Soft amber for missed calls (calm, not alarming red)
  val statusColor = when (record.status) {
    "completed" -> LingoTokens.statusOnline
    "missed", "rejected", "busy" -> LingoTokens.accentGold
    else -> LingoTokens.textSecondary
  }

  val statusLabel = when (record.status) {
    "completed" -> if (isOutgoing) "Outgoing" else "Incoming"
    "missed" -> if (isOutgoing) "No answer" else "Missed"
    "rejected" -> "Declined"
    "busy" -> "Busy"
    "cancelled" -> "Cancelled"
    "failed" -> "Could not connect"
    else -> record.status.replaceFirstChar { it.uppercase() }
  }

  val durationFormatted = remember(record.durationSeconds) {
    val mins = record.durationSeconds / 60
    val secs = record.durationSeconds % 60
    if (record.durationSeconds > 0) String.format(Locale.US, "%dm %02ds", mins, secs) else "0s"
  }

  val timestampText = remember(record.startTimeEpochMs, record.createdAt) {
    val epochMs = record.startTimeEpochMs.takeIf { it > 0L }
      ?: ((record.createdAt?.seconds ?: 0L) * 1000L)
    if (epochMs > 0L) {
      SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(epochMs))
    } else {
      "Recent"
    }
  }

  LingoCleanCard(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClickRow,
    contentPadding = 16.dp
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      LingoAvatar(
        name = partnerName,
        userId = partnerId.ifBlank { partnerName },
        size = 44.dp
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = partnerName,
          color = LingoTokens.textPrimary,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = when {
              record.status == "missed" -> Icons.Default.CallMissed
              isOutgoing -> Icons.Default.CallMade
              else -> Icons.Default.CallReceived
            },
            contentDescription = statusLabel,
            tint = statusColor,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "$statusLabel · $durationFormatted · $timestampText",
            color = if (record.status == "missed") LingoTokens.accentGold else LingoTokens.textSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (partnerId.isNotBlank() && partnerId != "waiting_partner") {
        IconButton(
          onClick = { onRedial(partnerId, partnerName, record.callType) },
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(LingoTokens.bgGlassCard)
            .border(1.dp, LingoTokens.borderSubtle, CircleShape)
        ) {
          Icon(
            imageVector = if (record.callType == "video") Icons.Default.Videocam else Icons.Default.Call,
            contentDescription = "Call Again",
            tint = LingoTokens.brandCyan,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PartnerProfileDialog(
  partner: CommunityProfile,
  compatibilityPercent: Int?,
  onDismiss: () -> Unit,
  onChat: () -> Unit,
  onVoiceCall: () -> Unit,
  onVideoCall: () -> Unit,
  onAddFriend: () -> Unit,
  onSendSpeakingInvitation: () -> Unit = {},
  onReport: () -> Unit = {},
  onBlock: () -> Unit = {}
) {
  val nowMs = System.currentTimeMillis()
  val isOnlineNow = partner.isEffectivelyOnline(nowMs)
  val isAwayNow = partner.isEffectivelyAway(nowMs)
  val lastSeenText = partner.formatLastSeen(nowMs)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0E1525),
    titleContentColor = LingoTokens.textPrimary,
    textContentColor = LingoTokens.textPrimary,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          LingoAvatar(
            name = partner.displayName,
            userId = partner.userId,
            size = 44.dp,
            customImageUrl = partner.avatarUrl,
            isOnline = isOnlineNow
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = partner.displayName,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = buildString {
                if (partner.country.isNotBlank()) append("${partner.countryFlag} ${partner.country} · ")
                when {
                  isOnlineNow -> append("Online now")
                  isAwayNow -> append(if (!lastSeenText.isNullOrBlank()) "Away · $lastSeenText" else "Away")
                  else -> append(if (!lastSeenText.isNullOrBlank()) lastSeenText else "Offline")
                }
              },
              color = if (isOnlineNow) Color(0xFF34D399) else LingoTokens.textSecondary,
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        if (compatibilityPercent != null) {
          Text(
            text = "$compatibilityPercent%",
            color = LingoTokens.brandCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (partner.bio.isNotBlank()) {
          Text(
            text = partner.bio,
            color = LingoTokens.textPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
          )
        }
        HorizontalDivider(color = LingoTokens.borderSubtle)
        Text(
          text = "Speaks ${Language.findByCode(partner.nativeLanguageCode).name} · Learning ${Language.findByCode(partner.targetLanguageCode).name} (${partner.proficiencyLevel})",
          color = LingoTokens.textSecondary,
          fontSize = 13.sp
        )
        if (partner.interests.isNotEmpty()) {
          Text(
            text = "Interests: ${partner.interests.joinToString(", ")}",
            color = LingoTokens.textSecondary,
            fontSize = 13.sp
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onVoiceCall,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 44.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = LingoTokens.brandCyan,
              contentColor = LingoTokens.onGradientInk
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Voice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          OutlinedButton(
            onClick = onVideoCall,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 44.dp),
            border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Video", color = LingoTokens.brandCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onChat,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 44.dp),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Message", color = LingoTokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
          OutlinedButton(
            onClick = onAddFriend,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 44.dp),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Add friend", color = LingoTokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
        }
        OutlinedButton(
          onClick = onSendSpeakingInvitation,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .testTag("send_speaking_invitation_btn"),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Send practice invite", color = LingoTokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = LingoTokens.borderSubtle)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          TextButton(onClick = onReport) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = LingoTokens.accentGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Report", color = LingoTokens.accentGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
          TextButton(onClick = onBlock) {
            Icon(Icons.Default.Block, contentDescription = null, tint = LingoTokens.accentRose, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Block", color = LingoTokens.accentRose, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = LingoTokens.textSecondary)
      }
    }
  )
}

@Composable
private fun PrivacyAndSafetyDialog(
  userProfile: UserProfile,
  myCommunityProfile: CommunityProfile?,
  showLastSeen: Boolean = true,
  whoCanCallMe: String = "everyone",
  doNotDisturb: Boolean = false,
  blockedUsers: List<UserBlock>,
  onUpdateSettings: (isOnline: Boolean, allowCalls: Boolean, showLastSeen: Boolean, whoCanCall: String, dnd: Boolean) -> Unit,
  onUnblockUser: (blockId: String) -> Unit,
  onDismiss: () -> Unit
) {
  var appearOffline by remember(myCommunityProfile) { mutableStateOf(!(myCommunityProfile?.isOnline ?: true)) }
  var dndEnabled by remember(doNotDisturb) { mutableStateOf(doNotDisturb) }
  var selectedWhoCanCall by remember(whoCanCallMe) { mutableStateOf(whoCanCallMe) }
  var lastSeenVisible by remember(showLastSeen) { mutableStateOf(showLastSeen) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0E1525),
    titleContentColor = LingoTokens.textPrimary,
    textContentColor = LingoTokens.textPrimary,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Security, contentDescription = null, tint = LingoTokens.brandCyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Safety & Calling", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Appear offline", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Hide your green online status", color = LingoTokens.textSecondary, fontSize = 12.sp)
          }
          Switch(
            checked = appearOffline,
            onCheckedChange = {
              appearOffline = it
              onUpdateSettings(!appearOffline, selectedWhoCanCall != "nobody" && !dndEnabled, lastSeenVisible, selectedWhoCanCall, dndEnabled)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = LingoTokens.brandCyan)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Do not disturb", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Pause incoming call notifications", color = LingoTokens.textSecondary, fontSize = 12.sp)
          }
          Switch(
            checked = dndEnabled,
            onCheckedChange = {
              dndEnabled = it
              onUpdateSettings(!appearOffline, selectedWhoCanCall != "nobody" && !dndEnabled, lastSeenVisible, selectedWhoCanCall, dndEnabled)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = LingoTokens.brandCyan)
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Who can call me", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("everyone" to "Everyone", "friends" to "Friends", "nobody" to "Nobody").forEach { (key, label) ->
              val selected = selectedWhoCanCall == key
              Surface(
                onClick = {
                  selectedWhoCanCall = key
                  onUpdateSettings(!appearOffline, key != "nobody" && !dndEnabled, lastSeenVisible, key, dndEnabled)
                },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) LingoTokens.brandCyan.copy(alpha = 0.2f) else LingoTokens.bgGlassCard,
                border = BorderStroke(1.dp, if (selected) LingoTokens.brandCyan else LingoTokens.borderSubtle),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 40.dp)
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                  Text(
                    text = label,
                    color = if (selected) LingoTokens.brandCyan else LingoTokens.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Show last active", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Let partners see when you were last online", color = LingoTokens.textSecondary, fontSize = 12.sp)
          }
          Switch(
            checked = lastSeenVisible,
            onCheckedChange = {
              lastSeenVisible = it
              onUpdateSettings(!appearOffline, selectedWhoCanCall != "nobody" && !dndEnabled, lastSeenVisible, selectedWhoCanCall, dndEnabled)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = LingoTokens.brandCyan)
          )
        }

        HorizontalDivider(color = LingoTokens.borderSubtle)

        Text(
          text = "Blocked users (${blockedUsers.size})",
          color = LingoTokens.textPrimary,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        )

        if (blockedUsers.isEmpty()) {
          Text(
            text = "You haven't blocked anyone.",
            color = LingoTokens.textSecondary,
            fontSize = 13.sp
          )
        } else {
          blockedUsers.forEach { blocked ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = blocked.blockedUserName.ifBlank { "User" },
                color = LingoTokens.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              TextButton(onClick = { onUnblockUser(blocked.id) }) {
                Text("Unblock", color = LingoTokens.brandCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done", color = LingoTokens.brandCyan, fontWeight = FontWeight.Bold)
      }
    }
  )
}

@Composable
private fun DirectChatScreen(
  userProfile: UserProfile,
  partner: CommunityProfile,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  isMuted: Boolean = false,
  onToggleMute: () -> Unit = {},
  onScheduleSession: (topic: String, timeSlot: String) -> Unit = { _, _ -> },
  onBack: () -> Unit,
  onStartCall: (String) -> Unit
) {
  val nowMs by com.example.presence.PresenceManager.presenceTicker.collectAsState()
  com.example.ui.components.LanguageExchangeChatView(
    partner = partner,
    userProfile = userProfile,
    nowMs = nowMs,
    viewModel = viewModel,
    speechManager = speechManager,
    isMuted = isMuted,
    onToggleMute = onToggleMute,
    applyStatusBarInsets = false,
    applyNavigationBarInsets = false,
    onBack = onBack,
    onStartVoiceCall = { onStartCall("voice") },
    onStartVideoCall = { onStartCall("video") }
  )
}

@Composable
private fun ReportUserDialog(
  partnerName: String,
  onDismiss: () -> Unit,
  onSubmitReport: (String, String) -> Unit
) {
  val reasons = listOf("Spam or commercial", "Inappropriate language", "Harassment", "Fake profile")
  var selectedReason by remember { mutableStateOf(reasons.first()) }
  var details by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F1B33),
    titleContentColor = Color.White,
    textContentColor = Color.White,
    title = { Text("Report $partnerName", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select a reason for reporting:", color = Color(0xFF94A3B8), fontSize = 12.sp)
        reasons.forEach { r ->
          FilterChip(
            selected = selectedReason == r,
            onClick = { selectedReason = r },
            label = { Text(r, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFEF4444),
              selectedLabelColor = Color.White,
              containerColor = Color(0xFF142444),
              labelColor = Color.White
            )
          )
        }
        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text("Additional details (optional)") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmitReport(selectedReason, details) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
      ) {
        Text("Submit Report", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF94A3B8))
      }
    }
  )
}
