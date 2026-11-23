package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.CallRoom
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.LingoAvatar
import com.example.webrtc.WebRtcSignalingManager
import kotlinx.coroutines.launch

@Composable
fun SpeakingRoomsScreen(
  userProfile: UserProfile,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val activeRooms by viewModel.activeSpeakingRoomsState.collectAsState()
  var showCreateRoomDialog by remember { mutableStateOf(false) }
  var selectedActiveRoom by remember { mutableStateOf<CallRoom?>(null) }

  var newRoomTitle by remember(userProfile.displayName) {
    mutableStateOf("${userProfile.displayName.ifBlank { "Live" }}'s Speaking Room")
  }
  var newRoomTopic by remember { mutableStateOf("Daily Conversations & Cultural Exchange") }
  var newRoomLanguage by remember(userProfile.targetLanguageCode) {
    mutableStateOf(userProfile.targetLanguageCode.ifBlank { "en" })
  }
  var isPublicRoom by remember { mutableStateOf(true) }
  var maxParticipants by remember { mutableIntStateOf(6) }
  var isCreatingOrJoining by remember { mutableStateOf(false) }

  BackHandler {
    if (selectedActiveRoom != null) {
      val roomId = selectedActiveRoom!!.roomId
      viewModel.leaveSpeakingRoom(roomId)
      selectedActiveRoom = null
    } else {
      onBack()
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("speaking_rooms_screen"),
    color = Color(0xFF060D1D)
  ) {
    if (selectedActiveRoom != null) {
      val currentRoom = selectedActiveRoom!!
      LiveVoiceRoomSession(
        initialRoom = currentRoom,
        userProfile = userProfile,
        viewModel = viewModel,
        speechManager = speechManager,
        onLeaveRoom = {
          viewModel.leaveSpeakingRoom(currentRoom.roomId)
          selectedActiveRoom = null
        }
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF060D1D))
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                text = "Speaking Rooms",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Real live group audio conversations",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }
        }

        // Create Speaking Room Button (Hosting is Pro-only; joining public rooms is Free)
        Button(
          onClick = {
            if (!userProfile.isEntitlementVerified) {
              com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
            } else {
              showCreateRoomDialog = true
            }
          },
          enabled = !isCreatingOrJoining,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("create_speaking_room_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (userProfile.isEntitlementVerified) "Create Speaking Room" else "Unlock Hosting Speaking Rooms",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 14.sp
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Active Live Rooms (${activeRooms.size})",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        if (activeRooms.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No live speaking rooms yet. Create the first room.",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(activeRooms, key = { it.roomId }) { room ->
              ActiveRoomCard(
                room = room,
                enabled = !isCreatingOrJoining,
                onJoin = {
                  if (isCreatingOrJoining) return@ActiveRoomCard
                  isCreatingOrJoining = true
                  coroutineScope.launch {
                    try {
                      val joinedRoom = viewModel.joinSpeakingRoom(room.roomId)
                      selectedActiveRoom = joinedRoom
                    } catch (e: Exception) {
                      Toast.makeText(
                        context,
                        com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
                          e.localizedMessage,
                          isPro = userProfile.isEntitlementVerified,
                          fallback = "Unable to join room right now."
                        ),
                        Toast.LENGTH_SHORT
                      ).show()
                    } finally {
                      isCreatingOrJoining = false
                    }
                  }
                }
              )
            }
          }
        }
      }
    }

    if (showCreateRoomDialog) {
      val participantLimits = listOf(2, 4, 6, 8, 12)
      AlertDialog(
        onDismissRequest = { if (!isCreatingOrJoining) showCreateRoomDialog = false },
        containerColor = Color(0xFF0F1A36),
        title = { Text("Create Speaking Room", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
              value = newRoomTitle,
              onValueChange = { newRoomTitle = it },
              label = { Text("Room Name", color = Color(0xFF94A3B8)) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF06B6D4),
                unfocusedBorderColor = Color(0xFF1E2F5A)
              ),
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = newRoomTopic,
              onValueChange = { newRoomTopic = it },
              label = { Text("Conversation Topic", color = Color(0xFF94A3B8)) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF06B6D4),
                unfocusedBorderColor = Color(0xFF1E2F5A)
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Text("Language", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Language.SUPPORTED_LANGUAGES.forEach { lang ->
                val selected = newRoomLanguage == lang.code
                FilterChip(
                  selected = selected,
                  onClick = { newRoomLanguage = lang.code },
                  label = { Text("${lang.flagEmoji} ${lang.name}", fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.22f),
                    selectedLabelColor = Color(0xFF06B6D4),
                    containerColor = Color(0xFF142247),
                    labelColor = Color.White
                  )
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Visibility", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                  text = if (isPublicRoom) "Public — Open to all learners" else "Private — Invite / Direct Join",
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp
                )
              }
              Switch(
                checked = isPublicRoom,
                onCheckedChange = { isPublicRoom = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.Black,
                  checkedTrackColor = Color(0xFF06B6D4)
                )
              )
            }

            Text("Participant Limit", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              participantLimits.forEach { limit ->
                val selected = maxParticipants == limit
                Surface(
                  onClick = { maxParticipants = limit },
                  shape = RoundedCornerShape(8.dp),
                  color = if (selected) Color(0xFF06B6D4) else Color(0xFF142247),
                  border = BorderStroke(1.dp, if (selected) Color(0xFF06B6D4) else Color(0xFF1E2F5A)),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                      text = "$limit",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (selected) Color.Black else Color.White
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (isCreatingOrJoining) return@Button
              isCreatingOrJoining = true
              coroutineScope.launch {
                try {
                  val createdRoom = viewModel.createSpeakingRoom(
                    roomTitle = newRoomTitle.trim().ifBlank { "${userProfile.displayName}'s Speaking Room" },
                    targetLanguageCode = newRoomLanguage,
                    conversationTopic = newRoomTopic.trim().ifBlank { "Live Language Practice" },
                    isPublic = isPublicRoom,
                    participantLimit = maxParticipants
                  )
                  showCreateRoomDialog = false
                  selectedActiveRoom = createdRoom
                } catch (e: Exception) {
                  Toast.makeText(
                    context,
                    com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(
                      e.localizedMessage,
                      isPro = userProfile.isEntitlementVerified,
                      fallback = "Failed to create room right now."
                    ),
                    Toast.LENGTH_SHORT
                  ).show()
                } finally {
                  isCreatingOrJoining = false
                }
              }
            },
            enabled = !isCreatingOrJoining,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
          ) {
            Text(
              if (isCreatingOrJoining) "Creating..." else "Create & Join",
              color = Color.Black,
              fontWeight = FontWeight.Bold
            )
          }
        },
        dismissButton = {
          TextButton(
            onClick = { if (!isCreatingOrJoining) showCreateRoomDialog = false },
            enabled = !isCreatingOrJoining
          ) {
            Text("Cancel", color = Color(0xFF94A3B8))
          }
        }
      )
    }
  }
}

@Composable
fun ActiveRoomCard(
  room: CallRoom,
  enabled: Boolean = true,
  onJoin: () -> Unit
) {
  val participantCount = room.participantIds.distinct().size.coerceAtLeast(1)
  val limit = room.participantLimit.coerceAtLeast(2)
  val isFull = participantCount >= limit
  val title = room.roomTitle.ifBlank { room.conversationTopic.ifBlank { "Live Speaking Room" } }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
    border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(Color(0xFF10B981))
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF142247)
          ) {
            Text(
              text = if (room.isPublic) "PUBLIC" else "PRIVATE",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (room.isPublic) Color(0xFF10B981) else Color(0xFFF59E0B),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF06B6D4).copy(alpha = 0.2f)
          ) {
            Text(
              text = room.targetLanguageCode.uppercase(),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF06B6D4),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      if (room.conversationTopic.isNotBlank() && room.conversationTopic != title) {
        Text(
          text = "Topic: ${room.conversationTopic}",
          fontSize = 12.sp,
          color = Color(0xFFCBD5E1),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f, fill = false)
            .padding(end = 8.dp)
        ) {
          LingoAvatar(name = room.callerName, userId = room.callerId, size = 28.dp, isOnline = true)
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
              text = "Host: ${room.callerName}",
              fontSize = 12.sp,
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "$participantCount / $limit participants live",
              fontSize = 11.sp,
              color = Color(0xFF10B981)
            )
          }
        }

        Button(
          onClick = onJoin,
          enabled = enabled && !isFull,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isFull) Color(0xFF1E293B) else Color(0xFF06B6D4)
          ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = if (isFull) "Room Full" else "Join Room",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFull) Color(0xFF94A3B8) else Color.Black
          )
        }
      }
    }
  }
}

@Composable
fun LiveVoiceRoomSession(
  initialRoom: CallRoom,
  userProfile: UserProfile,
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onLeaveRoom: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val liveRoomFlow = remember(initialRoom.roomId) { viewModel.observeCallRoom(initialRoom.roomId) }
  val liveRoomState by liveRoomFlow.collectAsState(initial = initialRoom)
  val liveRoom = liveRoomState ?: initialRoom
  val communityProfiles by viewModel.communityProfilesState.collectAsState()
  val roomChatFlow = remember(initialRoom.roomId) { viewModel.observeSpeakingRoomChat(initialRoom.roomId) }
  val roomMessages by roomChatFlow.collectAsState(initial = emptyList())

  var showChatPanel by remember { mutableStateOf(false) }
  var chatInput by remember { mutableStateOf("") }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val rtcManager = remember(initialRoom.roomId) {
    WebRtcSignalingManager(
      context = context,
      roomId = initialRoom.roomId,
      isCaller = initialRoom.callerId == userProfile.userId,
      isVideo = false,
      viewModel = viewModel,
      coroutineScope = coroutineScope
    )
  }
  val connectionState by rtcManager.webrtcState.collectAsState()
  val isLocalMuted by rtcManager.isAudioMuted.collectAsState()
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasAudioPermission = granted
    if (granted) {
      webViewInstance?.let { rtcManager.attachWebView(it) }
    } else {
      Toast.makeText(context, "Microphone permission is required to speak in the room.", Toast.LENGTH_LONG).show()
    }
  }

  LaunchedEffect(Unit) {
    rtcManager.startSession()
    if (!hasAudioPermission) {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  DisposableEffect(initialRoom.roomId) {
    onDispose {
      rtcManager.closeSession()
    }
  }

  val isHandRaised = liveRoom.handRaisedParticipantIds.contains(userProfile.userId)
  val isMutedInRoom = isLocalMuted || liveRoom.mutedParticipantIds.contains(userProfile.userId)

  // Real participants list from Firestore room state
  val participantIds = remember(liveRoom.participantIds, liveRoom.callerId, userProfile.userId) {
    (liveRoom.participantIds + listOf(liveRoom.callerId, userProfile.userId))
      .filter { it.isNotBlank() && it != "waiting_partner" }
      .distinct()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF040817))
      .padding(16.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    val isRobolectricRoom = remember {
      android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }
    var isRoomWebViewReady by remember { mutableStateOf(isRobolectricRoom) }
    LaunchedEffect(Unit) {
      if (!isRoomWebViewReady) {
        androidx.compose.runtime.withFrameNanos { }
        kotlinx.coroutines.delay(90L)
        isRoomWebViewReady = true
      }
    }

    // Hidden WebRTC audio engine WebView
    if (isRoomWebViewReady) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).also { wv ->
            wv.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
            wv.webViewClient = object : android.webkit.WebViewClient() {
              override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
              ): Boolean {
                android.util.Log.w(
                  "SpeakingRoomsScreen",
                  "WebView renderer process terminated (didCrash=${detail?.didCrash()}); detaching safely."
                )
                rtcManager.detachWebView(view)
                if (webViewInstance === view) webViewInstance = null
                runCatching {
                  (view?.parent as? android.view.ViewGroup)?.removeView(view)
                  view?.destroy()
                }
                return true
              }
            }
            webViewInstance = wv
            if (hasAudioPermission) {
              rtcManager.attachWebView(wv)
            }
          }
        },
        onRelease = { wv ->
          rtcManager.detachWebView(wv)
          if (webViewInstance === wv) webViewInstance = null
          runCatching {
            wv.stopLoading()
            wv.removeJavascriptInterface("AndroidBridge")
            wv.loadUrl("about:blank")
            wv.onPause()
            wv.removeAllViews()
            wv.destroy()
          }
        },
        modifier = Modifier.size(1.dp)
      )
    }

    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = {
          rtcManager.closeSession()
          onLeaveRoom()
        }
      ) {
        Icon(Icons.Default.Close, contentDescription = "Leave", tint = Color.White)
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(
          text = liveRoom.roomTitle.ifBlank { liveRoom.conversationTopic.ifBlank { "Speaking Room" } },
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "🔴 Live Audio (${participantIds.size}/${liveRoom.participantLimit}) • ${liveRoom.targetLanguageCode.uppercase()} • ${connectionState.displayLabel}",
          fontSize = 11.sp,
          color = Color(0xFF10B981)
        )
      }
      BadgedBox(
        badge = {
          if (roomMessages.isNotEmpty()) {
            Badge(containerColor = Color(0xFF06B6D4)) {
              Text("${roomMessages.size}", color = Color.Black, fontSize = 10.sp)
            }
          }
        }
      ) {
        IconButton(onClick = { showChatPanel = !showChatPanel }) {
          Icon(
            Icons.AutoMirrored.Filled.Chat,
            contentDescription = "Room Chat",
            tint = if (showChatPanel) Color(0xFF06B6D4) else Color.White
          )
        }
      }
    }

    if (liveRoom.conversationTopic.isNotBlank()) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F1A36),
        border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Text(
          text = "Topic: ${liveRoom.conversationTopic}",
          fontSize = 12.sp,
          color = Color(0xFFCBD5E1),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
      }
    }

    // Center Stage: Speakers Grid + Optional Live Room Chat
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
          .fillMaxWidth()
          .weight(if (showChatPanel) 0.55f else 1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(participantIds, key = { it }) { pid ->
          val profile = communityProfiles.firstOrNull { it.userId == pid }
          val idx = liveRoom.participantIds.indexOf(pid)
          val fallbackName = when {
            pid == userProfile.userId -> userProfile.displayName.ifBlank { "You" }
            pid == liveRoom.callerId -> liveRoom.callerName.ifBlank { "Host" }
            idx >= 0 && idx < liveRoom.participantNames.size -> liveRoom.participantNames[idx]
            else -> profile?.displayName ?: "Speaker"
          }
          val displayName = profile?.displayName?.ifBlank { fallbackName } ?: fallbackName
          val isHost = pid == liveRoom.callerId
          val isParticipantMuted = if (pid == userProfile.userId) {
            isMutedInRoom
          } else {
            liveRoom.mutedParticipantIds.contains(pid)
          }
          val isParticipantHandRaised = liveRoom.handRaisedParticipantIds.contains(pid)

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
            border = BorderStroke(
              width = 1.5.dp,
              color = when {
                isParticipantHandRaised -> Color(0xFFF59E0B)
                !isParticipantMuted -> Color(0xFF10B981)
                else -> Color(0xFF1E2F5A)
              }
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(contentAlignment = Alignment.BottomEnd) {
                LingoAvatar(
                  name = displayName,
                  userId = pid,
                  size = 64.dp,
                  customImageUrl = profile?.avatarUrl ?: "",
                  isOnline = true
                )
                Surface(
                  shape = CircleShape,
                  color = if (isParticipantMuted) Color(0xFFEF4444) else Color(0xFF10B981),
                  modifier = Modifier.size(22.dp)
                ) {
                  Icon(
                    imageVector = if (isParticipantMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                  )
                }
              }

              Text(
                text = if (pid == userProfile.userId) "$displayName (You)" else displayName,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isHost) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF06B6D4).copy(alpha = 0.2f)
                  ) {
                    Text(
                      text = "HOST",
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF06B6D4),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                if (isParticipantHandRaised) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                  ) {
                    Text(
                      text = "✋ Hand Raised",
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFF59E0B),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                } else {
                  Text(
                    text = if (isParticipantMuted) "Muted" else "Speaking Live 🎙️",
                    fontSize = 11.sp,
                    color = if (isParticipantMuted) Color(0xFF94A3B8) else Color(0xFF10B981)
                  )
                }
              }
            }
          }
        }
      }

      if (showChatPanel) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A36)),
          border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
          modifier = Modifier
            .fillMaxWidth()
            .weight(0.45f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Room Live Chat",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF06B6D4)
            )
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (roomMessages.isEmpty()) {
                item {
                  Text(
                    text = "No messages in this room yet. Say hello!",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                  )
                }
              } else {
                items(roomMessages, key = { it.id.ifBlank { "${it.senderId}_${it.createdAt?.seconds ?: 0L}" } }) { msg ->
                  Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                      text = "${msg.senderName}: ",
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF06B6D4),
                      fontSize = 12.sp
                    )
                    Text(
                      text = msg.text,
                      color = Color.White,
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Message room...", color = Color(0xFF64748B), fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedBorderColor = Color(0xFF06B6D4),
                  unfocusedBorderColor = Color(0xFF1E2F5A)
                ),
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = {
                  val trimmed = chatInput.trim()
                  if (trimmed.isNotEmpty()) {
                    viewModel.sendSpeakingRoomMessage(liveRoom.roomId, trimmed)
                    chatInput = ""
                  }
                },
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF06B6D4))
              ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Bottom Control Dock
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF0F1A36),
      border = BorderStroke(1.dp, Color(0xFF1E2F5A)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 14.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Mute / Unmute real microphone + sync state in Firestore
        IconButton(
          onClick = {
            if (!hasAudioPermission) {
              permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
              val nextMuted = !isMutedInRoom
              rtcManager.toggleMuteAudio()
              viewModel.toggleSpeakingRoomMute(liveRoom.roomId, userProfile.userId, nextMuted)
            }
          },
          modifier = Modifier
            .size(48.dp)
            .background(
              if (isMutedInRoom) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF142247),
              CircleShape
            )
        ) {
          Icon(
            imageVector = if (isMutedInRoom) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isMutedInRoom) "Unmute Microphone" else "Mute Microphone",
            tint = if (isMutedInRoom) Color(0xFFEF4444) else Color.White
          )
        }

        // Raise / Lower Hand synced in Firestore
        IconButton(
          onClick = {
            val nextRaised = !isHandRaised
            viewModel.toggleSpeakingRoomHandRaised(liveRoom.roomId, userProfile.userId, nextRaised)
          },
          modifier = Modifier
            .size(48.dp)
            .background(
              if (isHandRaised) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF142247),
              CircleShape
            )
        ) {
          Icon(
            imageVector = Icons.Default.PanTool,
            contentDescription = "Raise Hand",
            tint = if (isHandRaised) Color(0xFFF59E0B) else Color.White
          )
        }

        // Toggle Room Chat
        IconButton(
          onClick = { showChatPanel = !showChatPanel },
          modifier = Modifier
            .size(48.dp)
            .background(
              if (showChatPanel) Color(0xFF06B6D4).copy(alpha = 0.2f) else Color(0xFF142247),
              CircleShape
            )
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = "Room Chat",
            tint = if (showChatPanel) Color(0xFF06B6D4) else Color.White
          )
        }

        // Leave Room
        Button(
          onClick = {
            rtcManager.closeSession()
            onLeaveRoom()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
          Text("Leave", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
