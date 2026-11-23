package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommunityProfile
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.LingoAvatar

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.example.ai.GeminiTutorService
import com.example.data.model.CallRoom
import com.example.data.model.DirectChatMessage
import com.example.data.model.FriendRequest
import com.example.data.model.Language
import kotlinx.coroutines.launch

@Composable
fun LanguageExchangeScreen(
  userProfile: UserProfile,
  communityProfiles: List<CommunityProfile>,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onStartLiveCall: (CallRoom) -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val friendRequests by viewModel.friendRequestsState.collectAsState()
  val blockedUsers by viewModel.blockedUsersState.collectAsState()
  val allChatMessages by viewModel.allChatMessagesState.collectAsState()
  val partnersQuotaExhausted by viewModel.partnersQuotaExhausted.collectAsState()
  val isPartnersLoading by viewModel.isPartnersLoading.collectAsState()
  val communityLoadError by viewModel.communityProfilesLoadError.collectAsState()
  val nowMs by com.example.presence.PresenceManager.presenceTicker.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.loadPartnersDirectory()
  }

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Partners, 1: Messages, 2: Requests
  var selectedPartner by remember { mutableStateOf<CommunityProfile?>(null) }
  var activeChatPartner by remember { mutableStateOf<CommunityProfile?>(null) }
  var callChoicePartner by remember { mutableStateOf<CommunityProfile?>(null) }
  var isStartingCall by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  val myUid = userProfile.userId.ifBlank { viewModel.userId }
  val blockedIds = remember(blockedUsers) { blockedUsers.map { it.blockedUserId }.toSet() }

  // Strictly real registered accounts: exclude self, exclude blocked users, deduplicate by userId
  val deduplicatedPartners = remember(communityProfiles, myUid, blockedIds) {
    communityProfiles
      .filter { it.userId.isNotBlank() && it.userId != myUid && it.userId !in blockedIds }
      .distinctBy { it.userId }
  }

  val filteredPartners = remember(deduplicatedPartners, searchQuery, nowMs) {
    deduplicatedPartners
      .filter {
        searchQuery.isBlank() ||
          it.displayName.contains(searchQuery, ignoreCase = true) ||
          it.country.contains(searchQuery, ignoreCase = true) ||
          it.targetLanguageCode.contains(searchQuery, ignoreCase = true) ||
          Language.findByCode(it.nativeLanguageCode).name.contains(searchQuery, ignoreCase = true) ||
          Language.findByCode(it.targetLanguageCode).name.contains(searchQuery, ignoreCase = true)
      }
      .sortedWith(
        compareByDescending<CommunityProfile> { it.isEffectivelyOnline(nowMs) }
          .thenByDescending { it.nativeLanguageCode == userProfile.targetLanguageCode }
          .thenByDescending { it.lastHeartbeatEpochMs }
      )
  }

  fun initiateCallWithPartner(partner: CommunityProfile, callType: String) {
    if (isStartingCall) return
    isStartingCall = true
    scope.launch {
      try {
        val room = viewModel.startOrJoinCallRoom(
          calleeId = partner.userId,
          calleeName = partner.displayName.ifBlank { "Language Partner" },
          callType = callType,
          matchMode = "direct",
          calleeNativeLang = partner.nativeLanguageCode,
          calleeLearningLang = partner.targetLanguageCode
        )
        callChoicePartner = null
        onStartLiveCall(room)
      } catch (e: Exception) {
        Toast.makeText(
          context,
          com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
            e.localizedMessage,
            isPro = userProfile.isEntitlementVerified,
            fallback = "Unable to start call right now."
          ),
          Toast.LENGTH_LONG
        ).show()
      } finally {
        isStartingCall = false
      }
    }
  }

  BackHandler {
    when {
      activeChatPartner != null -> activeChatPartner = null
      selectedPartner != null -> selectedPartner = null
      else -> onBack()
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("language_exchange_screen"),
    color = Color(0xFF040817)
  ) {
    if (activeChatPartner != null) {
      val livePartner = deduplicatedPartners.firstOrNull { it.userId == activeChatPartner!!.userId }
        ?: activeChatPartner!!
      val mutedIds by viewModel.mutedPartnerIds.collectAsState()
      com.example.ui.components.LanguageExchangeChatView(
        partner = livePartner,
        userProfile = userProfile,
        nowMs = nowMs,
        viewModel = viewModel,
        speechManager = speechManager,
        isMuted = mutedIds.contains(livePartner.userId),
        onToggleMute = { viewModel.toggleMutePartner(livePartner.userId) },
        applyStatusBarInsets = true,
        applyNavigationBarInsets = true,
        onBack = { activeChatPartner = null },
        onStartVoiceCall = { initiateCallWithPartner(livePartner, "voice") },
        onStartVideoCall = { initiateCallWithPartner(livePartner, "video") }
      )
    } else if (selectedPartner != null) {
      val livePartner = deduplicatedPartners.firstOrNull { it.userId == selectedPartner!!.userId }
        ?: selectedPartner!!
      val existingReq = friendRequests.firstOrNull {
        (it.senderId == myUid && it.receiverId == livePartner.userId) ||
          (it.senderId == livePartner.userId && it.receiverId == myUid)
      }
      PartnerDetailView(
        partner = livePartner,
        userProfile = userProfile,
        nowMs = nowMs,
        existingFriendRequest = existingReq,
        onBack = { selectedPartner = null },
        onStartChat = {
          activeChatPartner = livePartner
          selectedPartner = null
        },
        onRequestCall = { callChoicePartner = livePartner },
        onFriendAction = {
          when {
            existingReq == null -> {
              viewModel.sendFriendRequest(livePartner.userId, livePartner.displayName) {
                Toast.makeText(context, "Partnership request sent to ${livePartner.displayName}", Toast.LENGTH_SHORT).show()
              }
            }
            existingReq.status == "pending" && existingReq.receiverId == myUid -> {
              viewModel.respondToFriendRequest(existingReq.id, accept = true)
            }
            existingReq.status == "pending" && existingReq.senderId == myUid -> {
              viewModel.cancelOrRemoveFriendRequest(existingReq.id)
            }
          }
        }
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF060D1D))
          .windowInsetsPadding(WindowInsets.statusBars)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF13203D))
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Language Exchange",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              val onlineCount = deduplicatedPartners.count { it.isEffectivelyOnline(nowMs) }
              Text(
                text = "Meet people who speak the language you're learning • $onlineCount online",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }
        }

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by name, country, or language...", color = Color(0xFF64748B)) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF06B6D4)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF0F1A36),
            unfocusedContainerColor = Color(0xFF0F1A36),
            focusedBorderColor = Color(0xFF06B6D4),
            unfocusedBorderColor = Color(0xFF1E2F5A)
          ),
          modifier = Modifier.fillMaxWidth()
        )

        // Tabs Row
        val pendingCount = friendRequests.count { it.receiverId == myUid && it.status == "pending" }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TabButton("Explore Partners (${filteredPartners.size})", selectedTab == 0) { selectedTab = 0 }
          TabButton("Messages", selectedTab == 1) { selectedTab = 1 }
          TabButton(if (pendingCount > 0) "Requests ($pendingCount)" else "Requests", selectedTab == 2) { selectedTab = 2 }
        }

        if (selectedTab == 0) {
          if (filteredPartners.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
              ) {
                if (isPartnersLoading) {
                  CircularProgressIndicator(color = Color(0xFF06B6D4), strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                  Spacer(modifier = Modifier.height(12.dp))
                  Text("Loading language partners...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                } else {
                  Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = if (partnersQuotaExhausted) Color(0xFFFBBF24) else Color(0xFF64748B),
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = when {
                      partnersQuotaExhausted -> "Cloud read quota reached. Auto-retries paused."
                      !communityLoadError.isNullOrBlank() -> "Unable to load language partners right now."
                      else -> "No language partners found right now."
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                  )
                  if (partnersQuotaExhausted || !communityLoadError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                      onClick = { viewModel.retryPartnersAfterQuota() },
                      shape = RoundedCornerShape(10.dp),
                      border = BorderStroke(1.dp, Color(0xFF06B6D4))
                    ) {
                      Text("Retry", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(filteredPartners, key = { it.userId }) { partner ->
                PartnerCardItem(
                  partner = partner,
                  nowMs = nowMs,
                  onClick = { selectedPartner = partner },
                  onMessage = { activeChatPartner = partner },
                  onCall = { callChoicePartner = partner }
                )
              }
            }
          }
        } else if (selectedTab == 1) {
          val conversationThreads = remember(allChatMessages, deduplicatedPartners, myUid) {
            val grouped = allChatMessages.groupBy { msg ->
              if (msg.senderId == myUid) msg.receiverId else msg.senderId
            }.filterKeys { it.isNotBlank() && it != myUid && it != "PUBLIC_ROOM" }

            grouped.entries.map { (partnerId, msgs) ->
              val latest = msgs.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: msgs.first()
              val partnerProfile = deduplicatedPartners.firstOrNull { it.userId == partnerId }
                ?: CommunityProfile(
                  userId = partnerId,
                  displayName = if (latest.senderId == partnerId) {
                    latest.senderName.ifBlank { "Language Partner" }
                  } else {
                    msgs.firstOrNull { it.senderId == partnerId }?.senderName?.ifBlank { "Language Partner" } ?: "Language Partner"
                  },
                  isOnline = false,
                  presenceState = "offline"
                )
              partnerProfile to latest
            }.sortedByDescending { (_, latest) -> latest.createdAt?.seconds ?: 0L }
          }

          if (conversationThreads.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No active conversations yet. Tap 'Explore Partners' to start chatting!",
                  color = Color(0xFF94A3B8),
                  fontSize = 13.sp,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(conversationThreads, key = { it.first.userId }) { (partner, lastMsg) ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
                  border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeChatPartner = partner }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    LingoAvatar(
                      name = partner.displayName,
                      userId = partner.userId,
                      size = 46.dp,
                      customImageUrl = partner.avatarUrl,
                      isOnline = partner.isEffectivelyOnline(nowMs)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${partner.displayName} ${partner.countryFlag}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                      Text(
                        text = if (lastMsg.senderId == myUid) "You: ${lastMsg.text}" else lastMsg.text,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    IconButton(onClick = { callChoicePartner = partner }) {
                      Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF06B6D4))
                    }
                  }
                }
              }
            }
          }
        } else {
          val myRequests = remember(friendRequests, myUid) {
            friendRequests.filter {
              (it.receiverId == myUid || it.senderId == myUid) && it.status != "declined"
            }
          }
          if (myRequests.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No pending partnership requests.",
                  color = Color(0xFF94A3B8),
                  fontSize = 13.sp
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(myRequests, key = { it.id }) { req ->
                val isIncoming = req.receiverId == myUid
                val partnerId = if (isIncoming) req.senderId else req.receiverId
                val partnerName = if (isIncoming) req.senderName else req.receiverName
                val partner = deduplicatedPartners.firstOrNull { it.userId == partnerId }
                  ?: CommunityProfile(userId = partnerId, displayName = partnerName.ifBlank { "Learner" }, isOnline = false)

                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
                  border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPartner = partner }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    LingoAvatar(
                      name = partner.displayName,
                      userId = partner.userId,
                      size = 44.dp,
                      customImageUrl = partner.avatarUrl,
                      isOnline = partner.isEffectivelyOnline(nowMs)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(partner.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                      Text(
                        text = when {
                          req.status == "accepted" -> "Connected Partner"
                          isIncoming -> "Sent you a partner request"
                          else -> "Request pending"
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                      )
                    }
                    when {
                      req.status == "accepted" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                          IconButton(onClick = { activeChatPartner = partner }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Color(0xFF06B6D4))
                          }
                          IconButton(onClick = { callChoicePartner = partner }) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF10B981))
                          }
                        }
                      }
                      isIncoming -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          Button(
                            onClick = { viewModel.respondToFriendRequest(req.id, accept = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                          ) {
                            Text("Accept", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                          }
                          OutlinedButton(
                            onClick = { viewModel.respondToFriendRequest(req.id, accept = false) },
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                          ) {
                            Text("Decline", color = Color(0xFFEF4444), fontSize = 11.sp)
                          }
                        }
                      }
                      else -> {
                        OutlinedButton(
                          onClick = { viewModel.cancelOrRemoveFriendRequest(req.id) },
                          border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                          modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                          Text("Cancel", color = Color(0xFFF59E0B), fontSize = 11.sp)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    if (callChoicePartner != null) {
      val target = callChoicePartner!!
      AlertDialog(
        onDismissRequest = { if (!isStartingCall) callChoicePartner = null },
        containerColor = Color(0xFF0F1A36),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Call ${target.displayName}", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Choose how to connect with ${target.displayName} (${target.countryFlag}):",
              color = Color(0xFF94A3B8),
              fontSize = 13.sp
            )
            Button(
              onClick = { initiateCallWithPartner(target, "voice") },
              enabled = !isStartingCall,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
              Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Voice Call", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { initiateCallWithPartner(target, "video") },
              enabled = !isStartingCall,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Video Call", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { if (!isStartingCall) callChoicePartner = null }) {
            Text("Cancel", color = Color(0xFF94A3B8))
          }
        }
      )
    }
  }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) Color(0xFF06B6D4).copy(alpha = 0.2f) else Color(0xFF0F1A36),
    border = BorderStroke(1.dp, if (isSelected) Color(0xFF06B6D4) else Color(0xFF1E2F5A)),
    modifier = Modifier.height(36.dp)
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 14.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Color(0xFF06B6D4) else Color(0xFF94A3B8)
      )
    }
  }
}

@Composable
fun PartnerCardItem(
  partner: CommunityProfile,
  nowMs: Long,
  onClick: () -> Unit,
  onMessage: () -> Unit,
  onCall: () -> Unit
) {
  val presenceLabel = partner.effectivePresenceLabel(nowMs)
  val isOnline = partner.isEffectivelyOnline(nowMs)
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
    border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LingoAvatar(
          name = partner.displayName,
          userId = partner.userId,
          size = 46.dp,
          customImageUrl = partner.avatarUrl,
          isOnline = isOnline
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = partner.displayName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = partner.countryFlag, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFF64748B).copy(alpha = 0.16f)
            ) {
              Text(
                text = presenceLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Speaks: ${partner.nativeLanguageCode.uppercase()} • Learning: ${partner.targetLanguageCode.uppercase()} (${partner.proficiencyLevel})",
            fontSize = 11.5.sp,
            color = Color(0xFF06B6D4)
          )
          if (partner.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = partner.bio,
              fontSize = 11.sp,
              color = Color(0xFF94A3B8),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          onClick = onClick,
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF142247),
          border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
          modifier = Modifier.weight(1f)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
            Text(
              text = "View Profile",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        Surface(
          onClick = onMessage,
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF06B6D4).copy(alpha = 0.18f),
          border = BorderStroke(1.dp, Color(0xFF06B6D4)),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 7.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Message",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF06B6D4)
            )
          }
        }

        Surface(
          onClick = onCall,
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF10B981).copy(alpha = 0.18f),
          border = BorderStroke(1.dp, Color(0xFF10B981)),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 7.dp)
          ) {
            Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Call",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10B981)
            )
          }
        }
      }
    }
  }
}

@Composable
fun PartnerDetailView(
  partner: CommunityProfile,
  userProfile: UserProfile,
  nowMs: Long,
  existingFriendRequest: FriendRequest?,
  onBack: () -> Unit,
  onStartChat: () -> Unit,
  onRequestCall: () -> Unit,
  onFriendAction: () -> Unit
) {
  val presenceLabel = partner.effectivePresenceLabel(nowMs)
  val isOnline = partner.isEffectivelyOnline(nowMs)
  val friendActionLabel = when {
    existingFriendRequest == null -> "Add Language Partner"
    existingFriendRequest.status == "accepted" -> "Connected Partner (Remove)"
    existingFriendRequest.receiverId == userProfile.userId -> "Accept Partner Request"
    else -> "Cancel Pending Request"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF060D1D))
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(16.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Partner Profile",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
        border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          LingoAvatar(
            name = partner.displayName,
            userId = partner.userId,
            size = 80.dp,
            customImageUrl = partner.avatarUrl,
            isOnline = isOnline
          )
          Text(
            text = partner.displayName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "${partner.country} ${partner.countryFlag} • $presenceLabel",
            fontSize = 13.sp,
            color = if (isOnline) Color(0xFF10B981) else Color(0xFF94A3B8)
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF06B6D4).copy(alpha = 0.2f)
          ) {
            Text(
              text = "Native: ${partner.nativeLanguageCode.uppercase()} ⇄ Learning: ${partner.targetLanguageCode.uppercase()} (${partner.proficiencyLevel})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF06B6D4),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          if (partner.interests.isNotEmpty()) {
            Text(
              text = "Interests: ${partner.interests.joinToString(", ")}",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
          if (partner.bio.isNotBlank()) {
            Text(
              text = partner.bio,
              fontSize = 13.sp,
              color = Color(0xFFCBD5E1),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          OutlinedButton(
            onClick = onFriendAction,
            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(friendActionLabel, color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onStartChat,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Message", fontWeight = FontWeight.Bold, color = Color.Black)
      }

      Button(
        onClick = onRequestCall,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
      ) {
        Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Call", fontWeight = FontWeight.Bold, color = Color.Black)
      }
    }
  }
}

@Composable
fun ExchangeDirectChatView(
  partner: CommunityProfile,
  userProfile: UserProfile,
  nowMs: Long,
  viewModel: LingoViewModel,
  onBack: () -> Unit,
  onRequestCall: () -> Unit
) {
  com.example.ui.components.LanguageExchangeChatView(
    partner = partner,
    userProfile = userProfile,
    nowMs = nowMs,
    viewModel = viewModel,
    applyStatusBarInsets = true,
    applyNavigationBarInsets = true,
    onBack = onBack,
    onStartVoiceCall = onRequestCall,
    onStartVideoCall = onRequestCall
  )
}
