package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRoom
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.presence.PresenceManager
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.LingoAvatar
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoTokens
import com.example.ui.components.lingoAuroraBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SmartMatchingScreen(
  userProfile: UserProfile,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onBack: () -> Unit,
  onStartLiveCall: (CallRoom) -> Unit = {},
  onPracticeWithAi: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val connectPrefs = remember(context) {
    context.getSharedPreferences("lingosphere_connect_prefs", Context.MODE_PRIVATE)
  }

  var selectedMatchMode by remember { mutableStateOf("quick") } // quick, language, interest
  var selectedCallType by remember { mutableStateOf("voice") } // voice, video
  var selectedLanguage by remember(userProfile.targetLanguageCode) {
    mutableStateOf(userProfile.targetLanguageCode.ifBlank { "en" })
  }
  var selectedInterest by remember { mutableStateOf("Travel & Culture") }

  var isSearching by remember { mutableStateOf(false) }
  var noMatchFoundState by remember { mutableStateOf(false) }
  var notifyWhenOnlineEnabled by remember {
    mutableStateOf(connectPrefs.getBoolean("notify_when_online", false))
  }
  var activeMatchRoom by remember { mutableStateOf<CallRoom?>(null) }

  val observedRoomState = activeMatchRoom?.roomId?.let { roomId ->
    remember(roomId) { viewModel.observeCallRoom(roomId) }
      .collectAsState(initial = activeMatchRoom).value
  } ?: activeMatchRoom

  val communityProfiles by viewModel.communityProfilesState.collectAsState()
  val interestsList = listOf(
    "Travel & Culture",
    "Music & Movies",
    "Technology",
    "Food & Dining",
    "Business",
    "Daily Life"
  )

  fun cancelCurrentMatchmaking() {
    PresenceManager.setAvailableToMatchStatus(false)
    activeMatchRoom?.roomId?.let { roomId ->
      viewModel.cancelOutgoingCall(roomId)
    }
    activeMatchRoom = null
    isSearching = false
  }

  fun beginRealMatchSearch(matchModeKey: String, languageCode: String, topic: String) {
    noMatchFoundState = false
    isSearching = true
    PresenceManager.setAvailableToMatchStatus(true)
    coroutineScope.launch {
      try {
        val room = viewModel.startOrJoinCallRoom(
          calleeId = "waiting_partner",
          calleeName = "Available Partner",
          callType = selectedCallType,
          matchMode = matchModeKey,
          targetLanguageOverride = languageCode,
          conversationTopic = topic
        )
        activeMatchRoom = room
      } catch (e: Exception) {
        PresenceManager.setAvailableToMatchStatus(false)
        isSearching = false
        Toast.makeText(
          context,
          "Unable to start search right now. Please try again.",
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  // Real-time search timeout watcher: if no real user joins our waiting room within 30s, show calm fallback
  LaunchedEffect(isSearching, activeMatchRoom?.roomId) {
    val room = activeMatchRoom ?: return@LaunchedEffect
    if (!isSearching) return@LaunchedEffect
    delay(30_000L)
    val latest = observedRoomState ?: room
    val hasRealPartner = latest.calleeId.isNotBlank() &&
      latest.calleeId != "waiting_partner" &&
      latest.callerId != latest.calleeId
    if (isSearching && !hasRealPartner) {
      PresenceManager.setAvailableToMatchStatus(false)
      viewModel.cancelOutgoingCall(room.roomId)
      activeMatchRoom = null
      isSearching = false
      noMatchFoundState = true
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      PresenceManager.setAvailableToMatchStatus(false)
    }
  }

  BackHandler {
    if (isSearching || activeMatchRoom != null) {
      cancelCurrentMatchmaking()
    } else {
      onBack()
    }
  }

  val matchedRoom = observedRoomState?.takeIf {
    it.calleeId.isNotBlank() &&
      it.calleeId != "waiting_partner" &&
      it.callerId != it.calleeId &&
      it.status != "ended" &&
      it.status != "cancelled" &&
      it.status != "rejected"
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("smart_matching_screen"),
    color = LingoTokens.bgCanvas
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .lingoAuroraBackdrop()
    ) {
      when {
        matchedRoom != null -> {
          val partnerId = if (matchedRoom.callerId == userProfile.userId) matchedRoom.calleeId else matchedRoom.callerId
          val partnerName = if (matchedRoom.callerId == userProfile.userId) {
            matchedRoom.calleeName.ifBlank { "Language Partner" }
          } else {
            matchedRoom.callerName.ifBlank { "Language Partner" }
          }
          val partnerProfile = communityProfiles.firstOrNull { it.userId == partnerId }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(LingoTokens.screenMargin),
            contentAlignment = Alignment.Center
          ) {
            LingoCleanCard(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_match_ready_card"),
              contentPadding = 24.dp
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = LingoTokens.accentEmerald.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, LingoTokens.accentEmerald.copy(alpha = 0.45f))
                ) {
                  Text(
                    text = "Ready?",
                    color = LingoTokens.accentEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                  )
                }

                LingoAvatar(
                  name = partnerProfile?.displayName ?: partnerName,
                  userId = partnerId,
                  size = 80.dp,
                  customImageUrl = partnerProfile?.avatarUrl ?: "",
                  isOnline = true
                )
                Text(
                  text = partnerProfile?.displayName ?: partnerName,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary
                )
                if (partnerProfile != null) {
                  Text(
                    text = "${partnerProfile.countryFlag} ${partnerProfile.country} · Speaks ${partnerProfile.nativeLanguageCode.uppercase()} · Learning ${partnerProfile.targetLanguageCode.uppercase()}",
                    fontSize = 13.sp,
                    color = LingoTokens.brandCyan,
                    textAlign = TextAlign.Center
                  )
                }
                Text(
                  text = "Topic: ${matchedRoom.conversationTopic.ifBlank { "Live Language Exchange" }} · ${matchedRoom.targetLanguageCode.uppercase()}",
                  fontSize = 14.sp,
                  color = LingoTokens.textSecondary,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  OutlinedButton(
                    onClick = { cancelCurrentMatchmaking() },
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                      .weight(1f)
                      .heightIn(min = 52.dp)
                      .testTag("smart_match_decline_btn")
                  ) {
                    Text("Decline", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                  }

                  LingoPrimaryGradientButton(
                    text = "Accept & Call",
                    onClick = {
                      PresenceManager.setAvailableToMatchStatus(false)
                      isSearching = false
                      val roomToJoin = matchedRoom
                      activeMatchRoom = null
                      onStartLiveCall(roomToJoin)
                    },
                    modifier = Modifier
                      .weight(1.3f)
                      .heightIn(min = 52.dp)
                      .testTag("smart_match_accept_btn")
                  )
                }
              }
            }
          }
        }

        isSearching -> {
          val pulseTransition = rememberInfiniteTransition(label = "smartMatchPulse")
          val ringScale by pulseTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
              animation = tween(1300, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse
            ),
            label = "ringScale"
          )

          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(LingoTokens.screenMargin),
            contentAlignment = Alignment.Center
          ) {
            LingoCleanCard(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_match_searching_card"),
              contentPadding = 28.dp
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.size(104.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(96.dp)
                      .scale(ringScale)
                      .clip(CircleShape)
                      .background(LingoTokens.brandIndigo.copy(alpha = 0.14f))
                      .border(1.5.dp, LingoTokens.brandCyan.copy(alpha = 0.45f), CircleShape)
                  )
                  CircularProgressIndicator(
                    color = LingoTokens.brandCyan,
                    trackColor = LingoTokens.borderSubtle,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(64.dp)
                  )
                  Icon(
                    imageVector = if (selectedCallType == "video") Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = LingoTokens.textPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Text(
                  text = "Looking for a partner...",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "${Language.findByCode(selectedLanguage).name} · ${if (selectedCallType == "video") "Video call" else "Voice call"}",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = LingoTokens.brandCyan,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "Usually under 30 seconds. You can cancel anytime.",
                  fontSize = 13.sp,
                  color = LingoTokens.textSecondary,
                  textAlign = TextAlign.Center
                )
                OutlinedButton(
                  onClick = { cancelCurrentMatchmaking() },
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("smart_match_cancel_btn")
                ) {
                  Text("Cancel", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }

        noMatchFoundState -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(LingoTokens.screenMargin),
            contentAlignment = Alignment.Center
          ) {
            LingoCleanCard(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_match_timeout_card"),
              contentPadding = 24.dp
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LingoTokens.bgSubtle)
                    .border(1.dp, LingoTokens.borderSubtle, CircleShape)
                ) {
                  Icon(
                    imageVector = Icons.Default.PersonSearch,
                    contentDescription = null,
                    tint = LingoTokens.brandCyan,
                    modifier = Modifier.size(30.dp)
                  )
                }
                Text(
                  text = "No one is available right now",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "No learners practicing ${Language.findByCode(selectedLanguage).name} joined in the last 30 seconds.",
                  fontSize = 14.sp,
                  color = LingoTokens.textSecondary,
                  textAlign = TextAlign.Center,
                  lineHeight = 20.sp
                )

                LingoPrimaryGradientButton(
                  text = "Try again",
                  onClick = {
                    val modeKey = when (selectedMatchMode) {
                      "language" -> "country"
                      "interest" -> "interest"
                      else -> "random"
                    }
                    val topic = if (selectedMatchMode == "interest") selectedInterest else "Live Language Exchange"
                    beginRealMatchSearch(modeKey, selectedLanguage, topic)
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("smart_match_retry_btn")
                )

                OutlinedButton(
                  onClick = {
                    notifyWhenOnlineEnabled = true
                    connectPrefs.edit().putBoolean("notify_when_online", true).apply()
                    Toast.makeText(
                      context,
                      "We'll notify you when a partner comes online.",
                      Toast.LENGTH_SHORT
                    ).show()
                  },
                  border = BorderStroke(
                    1.dp,
                    if (notifyWhenOnlineEnabled) LingoTokens.accentEmerald.copy(alpha = 0.6f) else LingoTokens.borderSubtle
                  ),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("smart_match_notify_btn")
                ) {
                  Icon(
                    imageVector = if (notifyWhenOnlineEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = if (notifyWhenOnlineEnabled) LingoTokens.accentEmerald else LingoTokens.textPrimary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (notifyWhenOnlineEnabled) "Alert enabled" else "Notify me",
                    color = LingoTokens.textPrimary,
                    fontWeight = FontWeight.SemiBold
                  )
                }

                OutlinedButton(
                  onClick = {
                    noMatchFoundState = false
                    onPracticeWithAi()
                  },
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("smart_match_ai_fallback_btn")
                ) {
                  Text(
                    text = "Practice with AI",
                    color = LingoTokens.textPrimary,
                    fontWeight = FontWeight.SemiBold
                  )
                  if (!userProfile.isEntitlementVerified) {
                    Spacer(modifier = Modifier.width(8.dp))
                    LingoGoldLockIcon(size = 16.dp)
                  }
                }

                TextButton(
                  onClick = { noMatchFoundState = false },
                  modifier = Modifier.heightIn(min = 48.dp)
                ) {
                  Text("Back", color = LingoTokens.textSecondary, fontWeight = FontWeight.Medium)
                }
              }
            }
          }
        }

        else -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = LingoTokens.screenMargin, vertical = 16.dp)
              .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            // Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(LingoTokens.bgGlassCard)
                  .border(1.dp, LingoTokens.borderSubtle, CircleShape)
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LingoTokens.textPrimary)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Smart Match",
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary
                )
                Text(
                  text = "Connect live with an online learner · Free",
                  fontSize = 13.sp,
                  color = LingoTokens.textSecondary
                )
              }
            }

            // Mode Selector Tabs
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MatchModeTab("Anyone", selectedMatchMode == "quick", modifier = Modifier.weight(1f)) {
                selectedMatchMode = "quick"
              }
              MatchModeTab("Language", selectedMatchMode == "language", modifier = Modifier.weight(1f)) {
                selectedMatchMode = "language"
              }
              MatchModeTab("Interest", selectedMatchMode == "interest", modifier = Modifier.weight(1f)) {
                selectedMatchMode = "interest"
              }
            }

            // Call Type Selector (Voice / Video)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                onClick = { selectedCallType = "voice" },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedCallType == "voice") LingoTokens.brandIndigo.copy(alpha = 0.2f) else LingoTokens.bgGlassCard,
                border = BorderStroke(1.dp, if (selectedCallType == "voice") LingoTokens.brandCyan else LingoTokens.borderSubtle),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 12.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = if (selectedCallType == "voice") LingoTokens.brandCyan else LingoTokens.textSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    "Voice",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCallType == "voice") LingoTokens.textPrimary else LingoTokens.textSecondary
                  )
                }
              }

              Surface(
                onClick = { selectedCallType = "video" },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedCallType == "video") LingoTokens.brandIndigo.copy(alpha = 0.2f) else LingoTokens.bgGlassCard,
                border = BorderStroke(1.dp, if (selectedCallType == "video") LingoTokens.brandCyan else LingoTokens.borderSubtle),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 12.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (selectedCallType == "video") LingoTokens.brandCyan else LingoTokens.textSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    "Video",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCallType == "video") LingoTokens.textPrimary else LingoTokens.textSecondary
                  )
                }
              }
            }

            if (selectedMatchMode == "quick") {
              LingoCleanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 24.dp
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(72.dp)
                      .clip(CircleShape)
                      .background(LingoTokens.brandIndigo.copy(alpha = 0.18f))
                      .border(1.dp, LingoTokens.brandCyan.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(32.dp))
                  }
                  Text(
                    text = "Instant Quick Match",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary
                  )
                  Text(
                    text = "Pair with an available learner practicing ${Language.findByCode(userProfile.targetLanguageCode).name}.",
                    fontSize = 14.sp,
                    color = LingoTokens.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                  )
                  LingoPrimaryGradientButton(
                    text = "Find a Partner",
                    onClick = {
                      selectedLanguage = userProfile.targetLanguageCode.ifBlank { "en" }
                      beginRealMatchSearch(
                        matchModeKey = "random",
                        languageCode = selectedLanguage,
                        topic = "Quick Language Practice"
                      )
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 52.dp)
                  )
                }
              }
            } else if (selectedMatchMode == "language") {
              LingoCleanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 20.dp
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                  Text("Target language", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textPrimary)
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Language.SUPPORTED_LANGUAGES.forEach { lang ->
                      val selected = selectedLanguage == lang.code
                      FilterChip(
                        selected = selected,
                        onClick = { selectedLanguage = lang.code },
                        label = { Text("${lang.flagEmoji} ${lang.name}", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = LingoTokens.brandCyan,
                          selectedLabelColor = LingoTokens.onGradientInk,
                          containerColor = LingoTokens.bgSubtle,
                          labelColor = LingoTokens.textPrimary
                        )
                      )
                    }
                  }
                  Text(
                    text = "Your level: ${userProfile.proficiencyLevel} · Native: ${Language.findByCode(userProfile.nativeLanguageCode).name}",
                    fontSize = 13.sp,
                    color = LingoTokens.textSecondary
                  )
                  LingoPrimaryGradientButton(
                    text = "Search ${Language.findByCode(selectedLanguage).name} Partners",
                    onClick = {
                      beginRealMatchSearch(
                        matchModeKey = "country",
                        languageCode = selectedLanguage,
                        topic = "Language Practice (${selectedLanguage.uppercase()})"
                      )
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 50.dp)
                  )
                }
              }
            } else {
              LingoCleanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 20.dp
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text("Choose a topic", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textPrimary)
                  interestsList.forEach { interest ->
                    val isSel = selectedInterest == interest
                    Surface(
                      onClick = { selectedInterest = interest },
                      shape = RoundedCornerShape(14.dp),
                      color = if (isSel) LingoTokens.brandIndigo.copy(alpha = 0.22f) else LingoTokens.bgSubtle,
                      border = BorderStroke(1.dp, if (isSel) LingoTokens.brandCyan else LingoTokens.borderSubtle),
                      modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                    ) {
                      Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                      ) {
                        Text(
                          text = interest,
                          fontSize = 14.sp,
                          fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                          color = if (isSel) LingoTokens.brandCyan else LingoTokens.textPrimary
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  LingoPrimaryGradientButton(
                    text = "Find Partner in $selectedInterest",
                    onClick = {
                      beginRealMatchSearch(
                        matchModeKey = "interest",
                        languageCode = selectedLanguage,
                        topic = selectedInterest
                      )
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 50.dp)
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

@Composable
fun MatchModeTab(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) LingoTokens.brandIndigo.copy(alpha = 0.22f) else LingoTokens.bgGlassCard,
    border = BorderStroke(1.dp, if (isSelected) LingoTokens.brandCyan else LingoTokens.borderSubtle),
    modifier = modifier.heightIn(min = 48.dp)
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isSelected) LingoTokens.textPrimary else LingoTokens.textSecondary
      )
    }
  }
}
