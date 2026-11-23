package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.VideoAiAssistantService
import com.example.data.model.Language
import com.example.data.model.LingoVideo
import com.example.data.model.UserProfile
import com.example.data.model.VideoAiStudyPack
import com.example.data.model.VideoPlaylist
import com.example.data.model.VideoPronunciationDrill
import com.example.data.repository.YouTubeVideoService
import com.example.speech.SpeechManager
import com.example.ui.components.YouTubeEmbeddedPlayer
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import kotlinx.coroutines.launch

@Composable
fun LingoVideoPlayerDetailView(
  video: LingoVideo,
  userProfile: UserProfile,
  isSaved: Boolean,
  playlists: List<VideoPlaylist>,
  speechManager: SpeechManager,
  onToggleSave: () -> Unit,
  onAddToPlaylist: (VideoPlaylist) -> Unit,
  onCreatePlaylistWithVideo: (String, String) -> Unit,
  onSaveVocabToFlashcard: (String, String, String) -> Unit,
  onUpdateWatchProgress: (Long, Boolean, Int, Int) -> Unit,
  onClose: () -> Unit
) {
  BackHandler { onClose() }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val targetLang = Language.findByCode(video.languageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)

  var selectedAssistantTab by remember { mutableIntStateOf(0) }
  var studyPack by remember(video.videoId) { mutableStateOf<VideoAiStudyPack?>(null) }
  var isLoadingAi by remember(video.videoId) { mutableStateOf(true) }
  var authorizedCaptions by remember(video.videoId) { mutableStateOf<List<String>>(emptyList()) }
  var customTranscriptInput by remember(video.videoId) { mutableStateOf(video.captionSummary) }
  var showPlaylistDialog by remember { mutableStateOf(false) }
  var maxWatchedSecs by remember(video.videoId) { mutableLongStateOf(5L) }
  val savedVocabTerms = remember(video.videoId) { mutableStateMapOf<String, Boolean>() }

  // Record initial watch history entry when opening the video
  LaunchedEffect(video.videoId) {
    onUpdateWatchProgress(5L, false, 0, 0)
    isLoadingAi = true
    val tracks = YouTubeVideoService.fetchAuthorizedCaptionTracks(video)
    authorizedCaptions = tracks
    studyPack = VideoAiAssistantService.generateVideoStudyPack(
      video = video,
      targetLanguage = targetLang,
      nativeLanguage = nativeLang,
      authorizedCaptionTracks = tracks
    )
    isLoadingAi = false
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF040B1A))
  ) {
    // Top Navigation Bar
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF071229))
        .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = {
            speechManager.stopSpeaking()
            speechManager.stopListening()
            onClose()
          },
          modifier = Modifier.testTag("close_video_player_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Videos",
            tint = Color.White
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${targetLang.flagEmoji} ${targetLang.name} • ${video.proficiencyLevel}",
            color = LingoCyan,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = video.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { showPlaylistDialog = true },
          modifier = Modifier.testTag("add_to_playlist_button")
        ) {
          Icon(
            imageVector = Icons.Default.PlaylistAdd,
            contentDescription = "Add to Playlist",
            tint = LingoCyan
          )
        }
        IconButton(
          onClick = onToggleSave,
          modifier = Modifier.testTag("save_video_button")
        ) {
          Icon(
            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = if (isSaved) "Saved" else "Save Video",
            tint = if (isSaved) LingoAmber else Color.White
          )
        }
      }
    }

    // LingoSphere Interactive Cinema Video Player (16:9, synchronized captions & native voiceover)
    YouTubeEmbeddedPlayer(
      video = video,
      speechManager = speechManager,
      onWatchSecondsUpdated = { seconds ->
        if (seconds > maxWatchedSecs && seconds - maxWatchedSecs >= 10L) {
          maxWatchedSecs = seconds
          onUpdateWatchProgress(seconds, false, 0, 0)
        }
      }
    )

    // AI Learning Assistant Tab Bar
    val assistantTabs = listOf(
      "AI Vocab & Notes",
      "Pronunciation Coach",
      "Video Quiz (+XP)",
      "Ask AI & Captions"
    )
    ScrollableTabRow(
      selectedTabIndex = selectedAssistantTab,
      containerColor = Color(0xFF071229),
      contentColor = LingoCyan,
      edgePadding = 12.dp
    ) {
      assistantTabs.forEachIndexed { index, label ->
        Tab(
          selected = selectedAssistantTab == index,
          onClick = { selectedAssistantTab = index },
          text = {
            Text(
              text = label,
              fontSize = 12.5.sp,
              fontWeight = if (selectedAssistantTab == index) FontWeight.ExtraBold else FontWeight.Medium,
              color = if (selectedAssistantTab == index) LingoCyan else Color(0xFF94A3B8)
            )
          }
        )
      }
    }

    // Scrollable AI Learning Assistant Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      if (isLoadingAi || studyPack == null) {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
          border = BorderStroke(1.dp, Color(0xFF1E428A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
          ) {
            CircularProgressIndicator(
              color = LingoCyan,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "AI Video Learning Assistant is analyzing...",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                text = "Extracting ${targetLang.name} vocabulary, pronunciation drills & quiz from captions",
                color = Color(0xFF93C5FD),
                fontSize = 12.sp
              )
            }
          }
        }
      } else {
        val pack = studyPack!!
        when (selectedAssistantTab) {
          0 -> VideoVocabAndNotesTab(
            pack = pack,
            targetLang = targetLang,
            savedTerms = savedVocabTerms,
            onSpeakWord = { text -> speechManager.speak(text, targetLang.code) },
            onSaveWord = { item ->
              savedVocabTerms[item.term] = true
              onSaveVocabToFlashcard(item.term, item.translation, item.phonetic)
              Toast.makeText(context, "Added '${item.term}' to your Spaced-Repetition Flashcards!", Toast.LENGTH_SHORT).show()
            }
          )

          1 -> VideoPronunciationTab(
            drills = pack.pronunciationDrills,
            targetLang = targetLang,
            speechManager = speechManager
          )

          2 -> VideoInteractiveQuizTab(
            pack = pack,
            onQuizFinished = { scorePct, xpEarned ->
              onUpdateWatchProgress(maxWatchedSecs, true, scorePct, xpEarned)
              Toast.makeText(context, "Quiz completed! +$xpEarned XP added to your profile.", Toast.LENGTH_LONG).show()
            }
          )

          3 -> VideoAskAiAndCaptionsTab(
            video = video,
            pack = pack,
            targetLang = targetLang,
            nativeLang = nativeLang,
            customTranscript = customTranscriptInput,
            onCustomTranscriptChanged = { customTranscriptInput = it },
            onRegenerateStudyPack = {
              coroutineScope.launch {
                isLoadingAi = true
                studyPack = VideoAiAssistantService.generateVideoStudyPack(
                  video = video,
                  targetLanguage = targetLang,
                  nativeLanguage = nativeLang,
                  authorizedCaptionTracks = authorizedCaptions,
                  customTranscriptExcerpt = customTranscriptInput
                )
                isLoadingAi = false
                selectedAssistantTab = 0
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showPlaylistDialog) {
    AddToPlaylistDialog(
      video = video,
      playlists = playlists,
      onAddExisting = { playlist ->
        onAddToPlaylist(playlist)
        showPlaylistDialog = false
        Toast.makeText(context, "Added to '${playlist.name}'", Toast.LENGTH_SHORT).show()
      },
      onCreateNew = { name, desc ->
        onCreatePlaylistWithVideo(name, desc)
        showPlaylistDialog = false
        Toast.makeText(context, "Created playlist '$name'", Toast.LENGTH_SHORT).show()
      },
      onDismiss = { showPlaylistDialog = false }
    )
  }
}

@Composable
private fun VideoVocabAndNotesTab(
  pack: VideoAiStudyPack,
  targetLang: Language,
  savedTerms: Map<String, Boolean>,
  onSpeakWord: (String) -> Unit,
  onSaveWord: (com.example.data.model.VideoVocabInsight) -> Unit
) {
  var selectedTappedVocab by remember(pack.videoId) {
    mutableStateOf(pack.vocabulary.firstOrNull())
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
    border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "AI Lesson Summary & Grammar Insight",
          color = LingoCyan,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.5.sp
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = pack.summaryInNative,
        color = Color.White,
        fontSize = 13.sp,
        lineHeight = 19.sp
      )
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF071229),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "💡 ${pack.culturalOrGrammarNote}",
          color = Color(0xFFBAE6FD),
          fontSize = 12.sp,
          modifier = Modifier.padding(10.dp)
        )
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  // Interactive Tap-Word Subtitles Bar
  if (pack.vocabulary.isNotEmpty()) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF071229)),
      border = BorderStroke(1.dp, LingoElectricBlue.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "🔤 Interactive Subtitle Words (Tap any word for instant meaning & audio)",
          color = LingoCyan,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          pack.vocabulary.take(4).forEach { v ->
            val isSelected = selectedTappedVocab?.term == v.term
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) LingoCyan else Color(0xFF0B1C3D),
              border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.6f)),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedTappedVocab = v
                  onSpeakWord(v.term)
                }
            ) {
              Text(
                text = v.term,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }
        selectedTappedVocab?.let { tapped ->
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${tapped.term} [${tapped.phonetic}] → ${tapped.translation}",
                color = LingoAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
              )
              Text(
                text = tapped.contextSentence,
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp
              )
            }
            TextButton(onClick = { onSaveWord(tapped) }) {
              Text("+ Save", color = LingoEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
  }

  Text(
    text = "Key Vocabulary from Video (${pack.vocabulary.size})",
    color = Color.White,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp
  )
  Spacer(modifier = Modifier.height(8.dp))

  pack.vocabulary.forEach { item ->
    val isSavedCard = savedTerms[item.term] == true
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
      border = BorderStroke(1.dp, Color(0xFF1E3A6D)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 5.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = item.term,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = LingoElectricBlue.copy(alpha = 0.25f)
              ) {
                Text(
                  text = item.partOfSpeech,
                  color = LingoCyan,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "[${item.phonetic}] • ${item.translation}",
              color = LingoAmber,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSpeakWord(item.term) }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Listen to ${item.term}",
                tint = LingoCyan
              )
            }
            OutlinedButton(
              onClick = { if (!isSavedCard) onSaveWord(item) },
              enabled = !isSavedCard,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (isSavedCard) LingoEmerald else LingoCyan)
            ) {
              Text(
                text = if (isSavedCard) "Saved ✓" else "+ Flashcard",
                color = if (isSavedCard) LingoEmerald else LingoCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "“${item.contextSentence}”",
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp
        )
      }
    }
  }
}

@Composable
private fun VideoPronunciationTab(
  drills: List<VideoPronunciationDrill>,
  targetLang: Language,
  speechManager: SpeechManager
) {
  val context = LocalContext.current
  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  var activeDrillIndex by remember { mutableIntStateOf(0) }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager.startListening(targetLang.code)
    } else {
      Toast.makeText(context, "Microphone permission is required for pronunciation practice", Toast.LENGTH_SHORT).show()
    }
  }

  Text(
    text = "Shadowing & Pronunciation Practice",
    color = Color.White,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp
  )
  Text(
    text = "Listen to the native cadence first, then tap Speak to practice your ${targetLang.name} pronunciation.",
    color = Color(0xFF93C5FD),
    fontSize = 12.sp
  )

  Spacer(modifier = Modifier.height(12.dp))

  drills.forEachIndexed { idx, drill ->
    val isCurrentDrill = activeDrillIndex == idx
    val matchScore = remember(spokenText, isCurrentDrill, drill.phraseInTarget) {
      if (!isCurrentDrill || spokenText.isBlank()) null
      else computeSimilarityScore(drill.phraseInTarget, spokenText)
    }

    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
      border = BorderStroke(1.2.dp, if (isCurrentDrill) LingoCyan else Color(0xFF1E3A6D)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = drill.phraseInTarget,
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Phonetics: ${drill.phoneticGuide}",
          color = LingoCyan,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "Meaning: ${drill.nativeMeaning}",
          color = Color(0xFFCBD5E1),
          fontSize = 12.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "🎯 Tip: ${drill.coachingTip}",
          color = LingoAmber,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedButton(
            onClick = {
              activeDrillIndex = idx
              speechManager.speak(drill.phraseInTarget, targetLang.code)
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LingoCyan),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Listen", color = LingoCyan, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              activeDrillIndex = idx
              if (isListening) {
                speechManager.stopListening()
              } else {
                val hasPerm = ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                  speechManager.startListening(targetLang.code)
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isListening && isCurrentDrill) Color(0xFFEF4444) else LingoEmerald,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (isListening && isCurrentDrill) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isListening && isCurrentDrill) "Stop" else "Speak Now",
              color = Color.Black,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }

        if (isCurrentDrill && spokenText.isNotBlank() && matchScore != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF071229),
            border = BorderStroke(1.dp, if (matchScore >= 70) LingoEmerald else LingoAmber),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "You said: “$spokenText”",
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Pronunciation Accuracy: $matchScore% • ${if (matchScore >= 75) "Excellent native cadence!" else "Good attempt! Try matching the vowel rhythm."}",
                color = if (matchScore >= 70) LingoEmerald else LingoAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

private fun computeSimilarityScore(expected: String, actual: String): Int {
  val expWords = expected.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "").split(Regex("\\s+")).filter { it.isNotBlank() }
  val actWords = actual.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "").split(Regex("\\s+")).filter { it.isNotBlank() }
  if (expWords.isEmpty() || actWords.isEmpty()) return 65
  val matched = expWords.count { exp -> actWords.any { act -> act.contains(exp) || exp.contains(act) } }
  return ((matched.toFloat() / expWords.size.toFloat()) * 100).toInt().coerceIn(55, 100)
}

@Composable
private fun VideoInteractiveQuizTab(
  pack: VideoAiStudyPack,
  onQuizFinished: (Int, Int) -> Unit
) {
  val selectedAnswers = remember(pack.videoId) { mutableStateMapOf<Int, Int>() }
  var isSubmitted by remember(pack.videoId) { mutableStateOf(false) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column {
      Text(
        text = "Video Comprehension & Vocab Quiz",
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp
      )
      Text(
        text = "Answer all ${pack.quizQuestions.size} questions to earn +35 XP",
        color = LingoCyan,
        fontSize = 12.sp
      )
    }
    Icon(Icons.Default.Quiz, contentDescription = null, tint = LingoAmber, modifier = Modifier.size(24.dp))
  }

  Spacer(modifier = Modifier.height(10.dp))

  pack.quizQuestions.forEachIndexed { qIndex, q ->
    val chosenIdx = selectedAnswers[qIndex]
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
      border = BorderStroke(1.dp, Color(0xFF1E3A6D)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "Q${qIndex + 1}. ${q.question}",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        q.options.forEachIndexed { optIdx, optionText ->
          val isSelected = chosenIdx == optIdx
          val isCorrect = optIdx == q.correctIndex
          val borderColor = when {
            !isSubmitted && isSelected -> LingoCyan
            isSubmitted && isCorrect -> LingoEmerald
            isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
            else -> Color(0xFF1E3A6D)
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) Color(0xFF132A56) else Color(0xFF071229),
            border = BorderStroke(1.2.dp, borderColor),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable(enabled = !isSubmitted) {
                selectedAnswers[qIndex] = optIdx
              }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
              Text(
                text = optionText,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
              )
              if (isSubmitted && isCorrect) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = LingoEmerald, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        if (isSubmitted) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "✓ ${q.explanation}",
            color = LingoCyan,
            fontSize = 12.sp
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  if (!isSubmitted) {
    Button(
      onClick = {
        isSubmitted = true
        val correctCount = pack.quizQuestions.indices.count { i ->
          selectedAnswers[i] == pack.quizQuestions[i].correctIndex
        }
        val pct = if (pack.quizQuestions.isNotEmpty()) {
          ((correctCount * 100f) / pack.quizQuestions.size).toInt()
        } else 100
        onQuizFinished(pct, 35)
      },
      enabled = selectedAnswers.size == pack.quizQuestions.size,
      colors = ButtonDefaults.buttonColors(
        containerColor = LingoEmerald,
        contentColor = Color.Black
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("submit_video_quiz_button")
    ) {
      Text("Submit Quiz & Claim +35 XP", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
    }
  } else {
    val correctCount = pack.quizQuestions.indices.count { i ->
      selectedAnswers[i] == pack.quizQuestions[i].correctIndex
    }
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF062C24)),
      border = BorderStroke(1.dp, LingoEmerald),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
      ) {
        Text(
          text = "🎉 Score: $correctCount / ${pack.quizQuestions.size} Correct (+35 XP Earned!)",
          color = LingoEmerald,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.5.sp,
          modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp)
        )
        TextButton(
          onClick = {
            selectedAnswers.clear()
            isSubmitted = false
          }
        ) {
          Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun VideoAskAiAndCaptionsTab(
  video: LingoVideo,
  pack: VideoAiStudyPack,
  targetLang: Language,
  nativeLang: Language,
  customTranscript: String,
  onCustomTranscriptChanged: (String) -> Unit,
  onRegenerateStudyPack: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var questionInput by remember { mutableStateOf("") }
  var aiAnswer by remember { mutableStateOf<String?>(null) }
  var isAsking by remember { mutableStateOf(false) }

  // Ask AI Tutor Section
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
    border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = "Ask AI Video Assistant",
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.5.sp
      )
      Text(
        text = "Ask about any ${targetLang.name} word, grammar structure, or cultural reference in this video.",
        color = Color(0xFF93C5FD),
        fontSize = 12.sp
      )
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = questionInput,
          onValueChange = { questionInput = it },
          placeholder = { Text("e.g., When should I use formal vs informal here?", fontSize = 12.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = Color(0xFF1E3A6D),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("ask_video_ai_input")
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = {
            if (questionInput.isNotBlank() && !isAsking) {
              val q = questionInput.trim()
              isAsking = true
              coroutineScope.launch {
                aiAnswer = VideoAiAssistantService.askQuestionAboutVideo(
                  video = video,
                  userQuestion = q,
                  targetLanguage = targetLang,
                  nativeLanguage = nativeLang
                )
                isAsking = false
              }
            }
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LingoCyan)
        ) {
          if (isAsking) {
            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
          } else {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask AI", tint = Color.Black)
          }
        }
      }

      if (!aiAnswer.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF071229),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = aiAnswer!!,
            color = Color(0xFFE2E8F0),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(14.dp))

  // Authorized Captions / Transcript Source Section
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
    border = BorderStroke(1.dp, Color(0xFF1E3A6D)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Authorized Captions & Transcript Context",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
      Text(
        text = pack.captionSourceStatus,
        color = LingoCyan,
        fontSize = 11.5.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = customTranscript,
        onValueChange = onCustomTranscriptChanged,
        label = { Text("Caption / Transcript Excerpt for AI Quiz & Vocab") },
        minLines = 3,
        maxLines = 5,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = LingoCyan,
          unfocusedBorderColor = Color(0xFF1E3A6D),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = onRegenerateStudyPack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LingoCyan),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Regenerate AI Vocab & Quiz from Transcript", color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}

@Composable
fun AddToPlaylistDialog(
  video: LingoVideo,
  playlists: List<VideoPlaylist>,
  onAddExisting: (VideoPlaylist) -> Unit,
  onCreateNew: (String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var newPlaylistName by remember { mutableStateOf("") }
  var newPlaylistDesc by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0B1C3D),
    titleContentColor = Color.White,
    textContentColor = Color.White,
    title = {
      Text("Save to Video Playlist", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (playlists.isNotEmpty()) {
          Text("Tap an existing playlist:", color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))
          playlists.forEach { pl ->
            val alreadyIn = pl.videoIds.contains(video.videoId)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF071229),
              border = BorderStroke(1.dp, Color(0xFF1E3A6D)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clickable(enabled = !alreadyIn) { onAddExisting(pl) }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                  Text("${pl.videoIds.size} videos", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Text(
                  text = if (alreadyIn) "Added ✓" else "+ Add",
                  color = if (alreadyIn) LingoEmerald else LingoCyan,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        Text("Or create a new playlist:", color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = newPlaylistName,
          onValueChange = { newPlaylistName = it },
          placeholder = { Text("Playlist Name (e.g., Spanish Grammar)", fontSize = 12.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = Color(0xFF1E3A6D),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = newPlaylistDesc,
          onValueChange = { newPlaylistDesc = it },
          placeholder = { Text("Short description (optional)", fontSize = 12.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = Color(0xFF1E3A6D),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (newPlaylistName.isNotBlank()) {
            onCreateNew(newPlaylistName.trim(), newPlaylistDesc.trim())
          }
        },
        enabled = newPlaylistName.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = LingoCyan, contentColor = Color.Black)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Create & Add", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = Color(0xFF94A3B8))
      }
    }
  )
}
