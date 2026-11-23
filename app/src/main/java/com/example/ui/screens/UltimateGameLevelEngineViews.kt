package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Language
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.VocabFlashcard
import com.example.data.repository.DynamicAiGameEngine
import com.example.data.repository.DynamicLevelExercise
import com.example.data.repository.GameInteractionMechanic
import com.example.data.repository.GameLevelProgressState
import com.example.data.repository.LearningResponseEvaluation
import com.example.data.repository.LevelProgressionBand
import com.example.data.repository.UltimateGameCategory
import com.example.data.repository.UltimateLanguageGame
import com.example.data.repository.ValidatedAnswerOption
import com.example.speech.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val UgDeepNavy = Color(0xFF071329)
private val UgCardNavy = Color(0xFF102342)
private val UgElevatedNavy = Color(0xFF162F57)
private val UgCyan = Color(0xFF14C8E8)
private val UgGreen = Color(0xFF00E676)
private val UgAmber = Color(0xFFFFB300)
private val UgCoral = Color(0xFFFF5252)
private val UgPurple = Color(0xFF7C4DFF)

/**
 * Clean 500-Level Selection Screen for any of the 70 LingoSphere games.
 * Displays Current Level, Completed Levels, Level Curriculum Objective, and 25 levels per page.
 */
@Composable
fun UltimateGameLevelSelectorView(
  game: UltimateLanguageGame,
  languageCode: String,
  isProSubscriber: Boolean,
  progressState: GameLevelProgressState,
  onSelectLevel: (Int) -> Unit,
  onOpenPremiumPaywall: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val lang = Language.findByCode(languageCode)
  val currentPlayableLevel = progressState.highestUnlockedLevel.coerceIn(1, game.maxLevels)
  val currentSpec = remember(game.id, currentPlayableLevel) {
    DynamicAiGameEngine.getCurriculumObjective(game, currentPlayableLevel)
  }

  val totalPages = (game.maxLevels + 24) / 25
  var selectedPageIndex by remember(currentPlayableLevel) {
    mutableIntStateOf(((currentPlayableLevel - 1) / 25).coerceIn(0, totalPages - 1))
  }

  val pageStartLevel = selectedPageIndex * 25 + 1
  val pageEndLevel = (pageStartLevel + 24).coerceAtMost(game.maxLevels)
  val pageBand = LevelProgressionBand.forLevel(pageStartLevel)
  val accentColor = Color(game.accentHex)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(UgDeepNavy)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("ultimate_level_selector_screen")
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(UgCardNavy)
          .testTag("level_selector_back_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = game.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${lang.flagEmoji} ${game.category.shortTabLabel} • ${game.mechanic.label}",
          style = MaterialTheme.typography.labelSmall,
          color = UgCyan
        )
      }
      if (progressState.totalStarsEarned > 0) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = UgAmber.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, UgAmber.copy(alpha = 0.45f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = UgAmber, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${progressState.totalStarsEarned}",
              color = UgAmber,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Current Level Summary & Continue Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = UgCardNavy),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = accentColor.copy(alpha = 0.2f),
              modifier = Modifier.size(46.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(game.iconEmoji, fontSize = 22.sp)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Current Level: $currentPlayableLevel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Text(
                text = currentSpec.objectiveTitle,
                style = MaterialTheme.typography.labelMedium,
                color = UgCyan,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = UgElevatedNavy
          ) {
            Text(
              text = "${progressState.totalCompletedCount} Completed",
              color = Color.White.copy(alpha = 0.9f),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
          progress = {
            (progressState.totalCompletedCount.toFloat() / game.maxLevels.toFloat()).coerceIn(0.01f, 1f)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp)),
          color = UgCyan,
          trackColor = Color.White.copy(alpha = 0.12f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            val requiresPro = currentPlayableLevel > game.freeLevelsLimit && !isProSubscriber
            if (requiresPro) onOpenPremiumPaywall() else onSelectLevel(currentPlayableLevel)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("continue_playing_level_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UgCyan)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = UgDeepNavy)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Continue • Level $currentPlayableLevel",
            color = UgDeepNavy,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Level Group Pagination Selector (25 levels per page)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Levels $pageStartLevel–$pageEndLevel",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
      )
      Text(
        text = pageBand.bandName,
        style = MaterialTheme.typography.labelMedium,
        color = UgCyan,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 2.dp)
    ) {
      items(totalPages) { pageIdx ->
        val start = pageIdx * 25 + 1
        val end = (start + 24).coerceAtMost(game.maxLevels)
        val isSelected = pageIdx == selectedPageIndex
        FilterChip(
          selected = isSelected,
          onClick = { selectedPageIndex = pageIdx },
          label = {
            Text(
              text = "$start–$end",
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
              fontSize = 12.sp
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = UgCyan.copy(alpha = 0.24f),
            selectedLabelColor = UgCyan,
            containerColor = UgCardNavy,
            labelColor = Color.White.copy(alpha = 0.8f)
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 25 Level Tiles Grid (5 rows x 5 columns)
    val levelsInPage = (pageStartLevel..pageEndLevel).toList()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      for (rowStart in levelsInPage.indices step 5) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (col in 0 until 5) {
            val idx = rowStart + col
            if (idx < levelsInPage.size) {
              val lvl = levelsInPage[idx]
              val stars = progressState.completedLevels[lvl] ?: 0
              val isCompleted = stars > 0
              val isCurrent = lvl == currentPlayableLevel
              val isUnlocked = lvl <= currentPlayableLevel || isCompleted
              val isProGated = lvl > game.freeLevelsLimit && !isProSubscriber

              val tileBg = when {
                isCurrent -> UgElevatedNavy
                isCompleted -> Color(0xFF0E332E)
                else -> UgCardNavy.copy(alpha = 0.65f)
              }
              val borderColor = when {
                isCurrent -> UgCyan
                isCompleted -> UgGreen.copy(alpha = 0.7f)
                else -> Color.White.copy(alpha = 0.1f)
              }

              Surface(
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 64.dp)
                  .clickable(enabled = isUnlocked) {
                    if (isProGated) onOpenPremiumPaywall() else onSelectLevel(lvl)
                  }
                  .testTag("level_tile_$lvl"),
                shape = RoundedCornerShape(14.dp),
                color = tileBg,
                border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Text(
                      text = "$lvl",
                      color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.38f),
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 14.sp
                    )
                    if (isCompleted) {
                      Spacer(modifier = Modifier.width(3.dp))
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = UgGreen,
                        modifier = Modifier.size(12.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(3.dp))

                  when {
                    isCompleted -> {
                      Row(horizontalArrangement = Arrangement.Center) {
                        for (s in 1..3) {
                          Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (s <= stars) UgAmber else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(10.dp)
                          )
                        }
                      }
                    }
                    isCurrent -> {
                      Text(
                        text = "CURRENT",
                        color = UgCyan,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                      )
                    }
                    isProGated -> {
                      Text(
                        text = "PRO",
                        color = UgAmber,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                    else -> {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.28f),
                        modifier = Modifier.size(12.dp)
                      )
                    }
                  }
                }
              }
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

/**
 * Dynamic Interactive Gameplay View for all 70 games and 500 levels.
 * Renders genuine mechanic-specific gameplay (Wordigo grid, Memory card flip, Word Search grid,
 * Grammar repair, Shuffled Word Match, Audio-first Listening, SpellBee audio typing, Tile builder,
 * Real Microphone Speaking, Interactive Conversation Mission, and Escape Room Vault locks).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UltimateDynamicLevelGameplayView(
  game: UltimateLanguageGame,
  levelNumber: Int,
  languageCode: String,
  nativeLanguageCode: String,
  speechManager: SpeechManager,
  onSaveFlashcard: (VocabFlashcard) -> Unit,
  onAddMistakeEntry: (MistakeNotebookEntry) -> Unit,
  onLevelFinished: (levelNumber: Int, accuracyPercent: Int, xpEarned: Int, missedWords: List<String>, learnedWords: List<String>) -> Unit,
  onPlayNextLevel: (Int) -> Unit,
  onBackToLevelSelect: () -> Unit,
  onNavigateBack: () -> Unit = onBackToLevelSelect,
  userId: String = "default_learner",
  userProfile: com.example.data.model.UserProfile? = null,
  onGrantBonusXp: (Int) -> Unit = {},
  onOpenPremiumPaywall: () -> Unit = {}
) {
  BackHandler(onBack = onNavigateBack)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val lang = Language.findByCode(languageCode)
  val nativeLang = Language.findByCode(nativeLanguageCode)
  val levelSpec = remember(game.id, levelNumber) {
    DynamicAiGameEngine.getCurriculumObjective(game, levelNumber)
  }

  var exercises by remember(game.id, languageCode, levelNumber) {
    mutableStateOf<List<DynamicLevelExercise>>(emptyList())
  }
  var isLoadingExercises by remember(game.id, languageCode, levelNumber) { mutableStateOf(true) }

  var currentIndex by remember(exercises) { mutableIntStateOf(0) }
  var selectedOptionId by remember(currentIndex, exercises) { mutableStateOf<String?>(null) }
  var typedAnswer by remember(currentIndex, exercises) { mutableStateOf("") }
  // Track tile indices (tokenIndex to tokenString) so duplicate words in a sentence work cleanly!
  var pickedTileEntries by remember(currentIndex, exercises) { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
  var isFlashcardFlipped by remember(currentIndex, exercises) { mutableStateOf(false) }
  var showHint by remember(currentIndex, exercises) { mutableStateOf(false) }
  var hintUnlockedByRewardedAd by remember(currentIndex, exercises) { mutableStateOf(false) }

  var evaluationResult by remember(currentIndex, exercises) { mutableStateOf<LearningResponseEvaluation?>(null) }
  var submittedConversationMessage by remember(currentIndex, exercises) { mutableStateOf<String?>(null) }
  var aiConversationReply by remember(currentIndex, exercises) { mutableStateOf<String?>(null) }
  var isEvaluatingAiTurn by remember { mutableStateOf(false) }

  var correctCount by remember(exercises) { mutableIntStateOf(0) }
  var missedWordsInLevel by remember(exercises) { mutableStateOf<List<String>>(emptyList()) }
  var learnedWordsInLevel by remember(exercises) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
  var unlockedEscapeCodes by remember(exercises) { mutableStateOf<List<String>>(emptyList()) }
  var isLevelCompleted by remember(exercises) { mutableStateOf(false) }
  var doubleXpClaimed by remember(exercises) { mutableStateOf(false) }
  var savedWordBanner by remember(currentIndex, exercises) { mutableStateOf<String?>(null) }
  var rewardStatusBanner by remember(currentIndex, exercises, isLevelCompleted) { mutableStateOf<String?>(null) }

  var timeLeftSeconds by remember(currentIndex, exercises) { mutableIntStateOf(25) }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  val partialSpokenText by speechManager.partialSpokenText.collectAsState()

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasMicPermission = granted
    if (granted) {
      speechManager.startListening(languageCode)
    }
  }

  LaunchedEffect(game.id, languageCode, levelNumber) {
    isLoadingExercises = true
    isLevelCompleted = false
    exercises = DynamicAiGameEngine.generateGameQuestions(
      context = context,
      game = game,
      languageCode = languageCode,
      nativeLanguageCode = nativeLanguageCode,
      levelNumber = levelNumber,
      forceRefresh = false,
      userId = userId
    )
    isLoadingExercises = false
  }

  val currentExercise = exercises.getOrNull(currentIndex)
  var lastConsumedSpokenSnapshot by remember(currentExercise?.id) {
    mutableStateOf(speechManager.spokenText.value)
  }

  // Clear stale voice state & auto-play audio on exercise entry for audio-driven mechanics
  LaunchedEffect(currentExercise?.id) {
    speechManager.stopSpeaking()
    speechManager.clearSessionSpeechState()
    lastConsumedSpokenSnapshot = ""
    val ex = currentExercise ?: return@LaunchedEffect
    when (ex.mechanic) {
      GameInteractionMechanic.SPELLING_INPUT,
      GameInteractionMechanic.LISTEN_AND_TYPE,
      GameInteractionMechanic.LISTENING_COMPREHENSION,
      GameInteractionMechanic.SOUND_DETECTIVE -> {
        speechManager.speakWithRate(ex.targetPhrase, languageCode, 0.96f)
      }
      GameInteractionMechanic.FAST_SPEECH_CHALLENGE -> {
        speechManager.speakWithRate(ex.targetPhrase, languageCode, 1.25f)
      }
      GameInteractionMechanic.LISTENING_STORY_QUIZ,
      GameInteractionMechanic.MISSING_AUDIO_WORD -> {
        val clip = ex.secondaryAudioPhrase.ifBlank { ex.targetPhrase }
        speechManager.speakWithRate(clip, languageCode, 0.96f)
      }
      GameInteractionMechanic.CONVERSATION_LISTENING -> {
        val exchange = listOf(ex.dialogueSpeakerALine, ex.dialogueSpeakerBLine)
          .filter { it.isNotBlank() }
          .joinToString(" ... ")
          .ifBlank { ex.targetPhrase }
        speechManager.speakWithRate(exchange, languageCode, 0.96f)
      }
      GameInteractionMechanic.AI_CONVERSATION_MISSION -> {
        if (ex.conversationStarter.isNotBlank()) {
          speechManager.speak(ex.conversationStarter, languageCode)
        }
      }
      else -> {}
    }
  }

  // Handle fresh voice transcript capture in Conversation modes (Speaking modes use DedicatedSpeakingGameStageHost)
  LaunchedEffect(spokenText, currentExercise?.id) {
    if (spokenText.isNotBlank() && spokenText != lastConsumedSpokenSnapshot && currentExercise != null) {
      lastConsumedSpokenSnapshot = spokenText
      if (!currentExercise.mechanic.isSpeakingMechanic) {
        typedAnswer = spokenText
      }
    }
  }

  // Countdown timer for SPEED_RUN_TIMER
  LaunchedEffect(currentIndex, currentExercise, selectedOptionId, evaluationResult, isLoadingExercises) {
    if (!isLoadingExercises && currentExercise != null &&
      currentExercise.mechanic == GameInteractionMechanic.SPEED_RUN_TIMER &&
      selectedOptionId == null && evaluationResult == null
    ) {
      timeLeftSeconds = currentExercise.timeLimitSeconds.coerceIn(10, 40)
      while (timeLeftSeconds > 0 && selectedOptionId == null && evaluationResult == null) {
        delay(1000L)
        timeLeftSeconds--
      }
      if (timeLeftSeconds == 0 && selectedOptionId == null && evaluationResult == null) {
        selectedOptionId = "__timeout__"
        missedWordsInLevel = (missedWordsInLevel + currentExercise.targetPhrase).distinct()
      }
    }
  }

  if (isLoadingExercises) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(UgDeepNavy),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = UgCyan)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "${game.title} • Level $levelNumber",
          color = Color.White,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = levelSpec.objectiveTitle,
          color = UgCyan,
          fontSize = 12.sp
        )
      }
    }
    return
  }

  if (isLevelCompleted || currentExercise == null) {
    val total = exercises.size.coerceAtLeast(1)
    val accuracy = ((correctCount.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
    val passed = accuracy >= 70
    val stars = when {
      accuracy >= 95 -> 3
      accuracy >= 80 -> 2
      accuracy >= 70 -> 1
      else -> 0
    }
    val xpEarned = if (passed) game.baseXpPerLevel + (stars * 5) else 10

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(UgDeepNavy)
        .verticalScroll(rememberScrollState())
        .padding(18.dp)
        .testTag("ultimate_level_complete_screen"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = UgCardNavy),
        border = BorderStroke(1.5.dp, if (passed) UgGreen else UgAmber)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = if (passed) "🎉" else "💪", fontSize = 44.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (passed) "Level $levelNumber Complete!" else "Level $levelNumber Finished",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
          )
          Text(
            text = "${game.title} • ${levelSpec.objectiveTitle}",
            style = MaterialTheme.typography.bodySmall,
            color = UgCyan,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in 1..3) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (s <= stars) UgAmber else Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$accuracy%", color = if (passed) UgGreen else UgAmber, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
              Text("Accuracy", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$correctCount/$total", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
              Text("Completed", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("+$xpEarned XP", color = UgAmber, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
              Text("Reward", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
          }

          if (!passed) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Score 70% or higher to unlock Level ${(levelNumber + 1).coerceAtMost(game.maxLevels)}.",
              color = UgAmber,
              fontSize = 12.sp,
              textAlign = TextAlign.Center
            )
          }

          if (learnedWordsInLevel.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Practiced in Level $levelNumber",
              style = MaterialTheme.typography.labelLarge,
              color = UgCyan,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            learnedWordsInLevel.take(5).forEach { (word, trans) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(word, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(trans, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                  onClick = {
                    onSaveFlashcard(VocabFlashcard(id = "ug_${word.hashCode()}", word = word, translation = trans, phonetic = word))
                    savedWordBanner = "Saved \"$word\" to vocabulary"
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.BookmarkAdd, contentDescription = "Save word", tint = UgCyan, modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          if (savedWordBanner != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(savedWordBanner!!, color = UgGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          if (rewardStatusBanner != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(rewardStatusBanner!!, color = UgGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Free users only: optional Rewarded Ad actions on the results screen
          val canShowResultRewardedAds = com.example.ads.AdService.shouldShowAds(userProfile)
          if (canShowResultRewardedAds) {
            if (!passed) {
              OutlinedButton(
                onClick = {
                  val started = com.example.ads.AdService.showRewardedAdForGameBonus(
                    userProfile = userProfile,
                    rewardType = com.example.ads.RewardedFeatureType.GAME_SECOND_CHANCE,
                    itemId = "${game.id}_lvl_${levelNumber}_continue",
                    onRewardEarned = {
                      // Grant second chance ONLY inside reward callback
                      val retryIdx = (exercises.size - 1).coerceAtLeast(0)
                      currentIndex = retryIdx
                      selectedOptionId = null
                      evaluationResult = null
                      typedAnswer = ""
                      pickedTileEntries = emptyList()
                      correctCount = retryIdx
                      isLevelCompleted = false
                    },
                    onFailedOrCancelled = { reason ->
                      rewardStatusBanner = reason
                    }
                  )
                  if (started) {
                    com.example.ads.AdService.completeActiveRewardedAdWithReward()
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("watch_ad_to_continue_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, UgAmber)
              ) {
                Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = UgAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Watch an ad to continue (Second Chance)",
                  color = UgAmber,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
            }

            if (!doubleXpClaimed) {
              OutlinedButton(
                onClick = {
                  val started = com.example.ads.AdService.showRewardedAdForGameBonus(
                    userProfile = userProfile,
                    rewardType = com.example.ads.RewardedFeatureType.GAME_DOUBLE_XP,
                    itemId = "${game.id}_lvl_${levelNumber}_double_xp",
                    onRewardEarned = {
                      // Grant Double XP ONLY inside reward callback
                      doubleXpClaimed = true
                      onGrantBonusXp(xpEarned)
                      rewardStatusBanner = "Double XP earned! +$xpEarned Bonus XP added."
                    },
                    onFailedOrCancelled = { reason ->
                      rewardStatusBanner = reason
                    }
                  )
                  if (started) {
                    com.example.ads.AdService.completeActiveRewardedAdWithReward()
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("watch_ad_double_xp_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, UgCyan)
              ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = UgCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Watch an ad for double XP (+$xpEarned XP)",
                  color = UgCyan,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
            }
          }

          if (passed && levelNumber < game.maxLevels) {
            Button(
              onClick = { onPlayNextLevel(levelNumber + 1) },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("play_next_level_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = UgGreen)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = UgDeepNavy)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Next Level (Level ${levelNumber + 1})",
                color = UgDeepNavy,
                fontWeight = FontWeight.ExtraBold
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onBackToLevelSelect,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
              Text("Levels", color = Color.White)
            }
            Button(
              onClick = {
                currentIndex = 0
                correctCount = 0
                missedWordsInLevel = emptyList()
                learnedWordsInLevel = emptyList()
                unlockedEscapeCodes = emptyList()
                isLevelCompleted = false
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = UgCyan)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = UgDeepNavy, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Replay", color = UgDeepNavy, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
    return
  }

  val isExerciseAnswered = selectedOptionId != null || evaluationResult != null

  fun handleEvaluationCompleted(eval: LearningResponseEvaluation) {
    evaluationResult = eval
    if (eval.isPassed) {
      correctCount++
      learnedWordsInLevel = (learnedWordsInLevel + (currentExercise.targetPhrase to currentExercise.translation)).distinctBy { it.first }
    } else {
      missedWordsInLevel = (missedWordsInLevel + currentExercise.targetPhrase).distinct()
    }
  }

  fun handleOptionSelection(
    opt: ValidatedAnswerOption,
    recordMistakeOnWrong: Boolean = false,
    speakTargetOnCorrect: Boolean = false,
    unlockEscapeClueOnCorrect: Boolean = false
  ) {
    if (selectedOptionId != null) return
    selectedOptionId = opt.id
    val isRight = currentExercise.isOptionCorrect(opt.id)
    if (isRight) {
      correctCount++
      if (unlockEscapeClueOnCorrect) {
        val codeDigit = currentExercise.escapeRoomClueCode.ifBlank { "K${currentIndex + 1}" }
        unlockedEscapeCodes = unlockedEscapeCodes + codeDigit
      }
      learnedWordsInLevel = (learnedWordsInLevel + (currentExercise.targetPhrase to currentExercise.translation)).distinctBy { it.first }
      if (speakTargetOnCorrect) {
        speechManager.speak(currentExercise.targetPhrase, languageCode)
      }
    } else {
      missedWordsInLevel = (missedWordsInLevel + currentExercise.targetPhrase).distinct()
      if (recordMistakeOnWrong) {
        onAddMistakeEntry(
          MistakeNotebookEntry(
            id = "mistake_${System.currentTimeMillis()}",
            languageCode = languageCode,
            originalText = opt.text,
            correctedText = currentExercise.targetPhrase,
            category = game.category.shortTabLabel,
            explanationNative = currentExercise.explanation
          )
        )
      }
    }
  }

  fun advanceOrCompleteLevel() {
    speechManager.stopListening()
    if (currentIndex + 1 < exercises.size) {
      currentIndex++
    } else {
      val total = exercises.size.coerceAtLeast(1)
      val acc = ((correctCount.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
      val stars = if (acc >= 95) 3 else if (acc >= 80) 2 else if (acc >= 70) 1 else 0
      val xp = if (acc >= 70) game.baseXpPerLevel + (stars * 5) else 10
      onLevelFinished(
        levelNumber,
        acc,
        xp,
        missedWordsInLevel,
        learnedWordsInLevel.map { it.first }
      )
      // Trigger interstitial BEFORE showing results screen (only after every 3rd completed game,
      // max once every 3 minutes and max 3 per session; Free users only)
      com.example.ads.AdService.onGameCompletedBeforeResults(
        userProfile = userProfile,
        gameTitle = "${game.title} Level $levelNumber"
      ) {
        isLevelCompleted = true
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(UgDeepNavy)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("ultimate_dynamic_gameplay_screen")
  ) {
    // Top Gameplay Header: Back, Title + Level + Objective, Levels Button / Timer
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      IconButton(
        onClick = {
          speechManager.stopListening()
          onNavigateBack()
        },
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(UgCardNavy)
          .testTag("gameplay_back_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${game.title} • Level $levelNumber",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${currentIndex + 1}/${exercises.size} • ${currentExercise.objectiveLabel.ifBlank { levelSpec.objectiveTitle }}",
          style = MaterialTheme.typography.labelSmall,
          color = UgCyan,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (currentExercise.mechanic == GameInteractionMechanic.SPEED_RUN_TIMER) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (timeLeftSeconds <= 5) UgCoral.copy(alpha = 0.25f) else UgAmber.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, if (timeLeftSeconds <= 5) UgCoral else UgAmber)
          ) {
            Text(
              text = "⏱ ${timeLeftSeconds}s",
              color = if (timeLeftSeconds <= 5) UgCoral else UgAmber,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.5.sp,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
          }
        }
        if (exercises.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = UgGreen.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, UgGreen.copy(alpha = 0.45f)),
            modifier = Modifier.testTag("gameplay_live_score_badge")
          ) {
            Text(
              text = "$correctCount/${exercises.size}",
              color = UgGreen,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = UgCardNavy,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
          modifier = Modifier
            .clickable {
              speechManager.stopListening()
              onBackToLevelSelect()
            }
            .testTag("open_level_selector_chip")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.GridView, contentDescription = "Levels", tint = UgCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Levels", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LinearProgressIndicator(
      progress = { ((currentIndex + 1).toFloat() / exercises.size.coerceAtLeast(1)).coerceIn(0.08f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(999.dp)),
      color = UgCyan,
      trackColor = Color.White.copy(alpha = 0.12f)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Main Exercise Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = UgCardNavy),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Prompt Row (Speaking mechanics render their own dedicated prompt/word/scene/dialogue header)
        if (!currentExercise.mechanic.isSpeakingMechanic) {
          val showHeaderSpeaker = currentExercise.mechanic == GameInteractionMechanic.FLASHCARD_FLIP
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = currentExercise.prompt,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              modifier = Modifier.weight(1f)
            )
            if (showHeaderSpeaker) {
              Spacer(modifier = Modifier.width(8.dp))
              FilledTonalIconButton(
                onClick = {
                  speechManager.speakWithRate(currentExercise.targetPhrase, languageCode, 0.96f)
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = UgCyan.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("play_prompt_audio_button")
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play audio", tint = UgCyan)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
        }

        // ==================== MECHANIC-SPECIFIC INTERACTIVE STAGE ====================
        when (currentExercise.mechanic) {
          GameInteractionMechanic.PICTURE_IDENTIFICATION -> {
            VerifiedConceptVisualCard(
              visualConceptId = currentExercise.visualConceptId,
              targetPhrase = currentExercise.targetPhrase,
              englishMeaning = currentExercise.translation,
              height = 170.dp,
              resolvedImageUrl = currentExercise.resolvedImageUrl,
              visualIconKey = currentExercise.visualIconKey,
              categoryHint = currentExercise.objectiveLabel
            )
            Spacer(modifier = Modifier.height(12.dp))
            LingoPandaHorizontalAnswerGrid(
              options = currentExercise.resolvedAnswerOptions(),
              correctOptionId = currentExercise.resolvedCorrectOptionId(),
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(
                  opt = opt,
                  recordMistakeOnWrong = true,
                  speakTargetOnCorrect = true
                )
              }
            )
          }

          GameInteractionMechanic.WORDIGO_GRID -> {
            WordigoGridMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.MEMORY_CARD_FLIP -> {
            MemoryCardFlipMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.WORD_MATCH_PAIRS -> {
            ShuffledWordMatchMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.WORD_SEARCH_GRID -> {
            InteractiveWordSearchStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.GRAMMAR_REPAIR -> {
            GrammarRepairMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.SPELLING_INPUT -> {
            val targetClean = currentExercise.targetPhrase.trim()
            val blankSlots = remember(targetClean) {
              targetClean.map { ch -> if (ch == ' ') "  " else "_" }.joinToString(" ")
            }

            CompactHorizontalAudioPlayer(
              spokenText = targetClean,
              languageCode = languageCode,
              speechManager = speechManager,
              labelBadge = "Spelling Audio"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = UgElevatedNavy,
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Meaning: ${currentExercise.translation}",
                  color = Color.White.copy(alpha = 0.88f),
                  fontSize = 12.5.sp,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "${targetClean.count { it != ' ' }} letters:  $blankSlots",
                  color = UgCyan,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = typedAnswer,
              onValueChange = { typedAnswer = it },
              enabled = !isExerciseAnswered,
              label = { Text("Type the word you hear (${lang.name})") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("spelling_input_field"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = UgCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
              )
            )

            if (!isExerciseAnswered) {
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = {
                  val eval = DynamicAiGameEngine.evaluateLearningResponse(
                    userResponse = typedAnswer,
                    targetPhrase = currentExercise.targetPhrase,
                    mechanic = currentExercise.mechanic
                  )
                  handleEvaluationCompleted(eval)
                  if (eval.isPassed) {
                    speechManager.speak(currentExercise.targetPhrase, languageCode)
                  }
                },
                enabled = typedAnswer.isNotBlank(),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("check_spelling_button"),
                colors = ButtonDefaults.buttonColors(containerColor = UgCyan)
              ) {
                Text("Check Spelling", color = UgDeepNavy, fontWeight = FontWeight.ExtraBold)
              }
            }
          }

          GameInteractionMechanic.SCRAMBLE_TILES -> {
            Text(
              text = "Meaning: \"${currentExercise.translation}\"",
              color = UgCyan,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val indexedBank = remember(currentExercise.id) {
              val raw = currentExercise.scrambledTokens.ifEmpty {
                currentExercise.targetPhrase.split(Regex("\\s+")).filter { it.isNotBlank() }
              }
              raw.mapIndexed { i, tok -> i to tok }
            }
            val pickedIds = pickedTileEntries.map { it.first }.toSet()

            // Assembled tiles box — tap any placed tile to remove it back to bank!
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = UgElevatedNavy,
              border = BorderStroke(1.dp, UgCyan.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .testTag("sentence_builder_assembled_box")
            ) {
              if (pickedTileEntries.isEmpty()) {
                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.CenterStart) {
                  Text(
                    text = "Tap the tiles below in order...",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 14.sp
                  )
                }
              } else {
                FlowRow(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  pickedTileEntries.forEachIndexed { posIdx, (tokId, tokStr) ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = UgCyan.copy(alpha = 0.24f),
                      border = BorderStroke(1.dp, UgCyan),
                      modifier = Modifier.clickable(enabled = !isExerciseAnswered) {
                        pickedTileEntries = pickedTileEntries.filterIndexed { i, _ -> i != posIdx }
                      }
                    ) {
                      Text(
                        text = tokStr,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              indexedBank.forEach { (tokId, tokStr) ->
                val isUsed = tokId in pickedIds
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isUsed) UgCardNavy else UgElevatedNavy,
                  border = BorderStroke(1.dp, if (isUsed) Color.White.copy(alpha = 0.08f) else UgCyan.copy(alpha = 0.45f)),
                  modifier = Modifier
                    .clickable(enabled = !isUsed && !isExerciseAnswered) {
                      pickedTileEntries = pickedTileEntries + (tokId to tokStr)
                    }
                    .testTag("scramble_tile_$tokId")
                ) {
                  Text(
                    text = tokStr,
                    color = if (isUsed) Color.White.copy(alpha = 0.22f) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                  )
                }
              }
            }

            if (!isExerciseAnswered) {
              Spacer(modifier = Modifier.height(12.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { pickedTileEntries = emptyList() },
                  enabled = pickedTileEntries.isNotEmpty(),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Clear", color = Color.White)
                }
                Button(
                  onClick = {
                    val separator = if (currentExercise.targetPhrase.contains(" ")) " " else ""
                    val attempt = pickedTileEntries.joinToString(separator) { it.second }
                    val eval = DynamicAiGameEngine.evaluateLearningResponse(
                      userResponse = attempt,
                      targetPhrase = currentExercise.targetPhrase,
                      mechanic = currentExercise.mechanic
                    )
                    handleEvaluationCompleted(eval)
                    if (eval.isPassed) {
                      speechManager.speak(currentExercise.targetPhrase, languageCode)
                    }
                  },
                  enabled = pickedTileEntries.isNotEmpty(),
                  modifier = Modifier
                    .weight(1.4f)
                    .testTag("check_sentence_order_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = UgCyan)
                ) {
                  Text("Check Sentence", color = UgDeepNavy, fontWeight = FontWeight.ExtraBold)
                }
              }
            }
          }

          GameInteractionMechanic.LISTENING_COMPREHENSION -> {
            CompactHorizontalAudioPlayer(
              spokenText = currentExercise.targetPhrase,
              languageCode = languageCode,
              speechManager = speechManager,
              labelBadge = "Listen & Choose"
            )

            if (isExerciseAnswered) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = UgElevatedNavy,
                border = BorderStroke(1.dp, UgCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Spoken Transcript: \"${currentExercise.targetPhrase}\"",
                  color = UgCyan,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LingoPandaHorizontalAnswerGrid(
              options = currentExercise.resolvedAnswerOptions(),
              correctOptionId = currentExercise.resolvedCorrectOptionId(),
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.AUDIO_WORD_MATCH -> {
            AudioWordMatchMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.LISTEN_AND_TYPE -> {
            ListenAndTypeMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              typedAnswer = typedAnswer,
              onTypedAnswerChange = { typedAnswer = it },
              isAnswered = isExerciseAnswered,
              onCompleted = { handleEvaluationCompleted(it) }
            )
          }

          GameInteractionMechanic.SOUND_DETECTIVE -> {
            SoundDetectiveMechanicStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.LISTENING_STORY_QUIZ -> {
            ListeningStoryQuizStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.MISSING_AUDIO_WORD -> {
            MissingAudioWordStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.CONVERSATION_LISTENING -> {
            DialogueListeningStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.FAST_SPEECH_CHALLENGE -> {
            FastSpeechChallengeStage(
              exercise = currentExercise,
              languageCode = languageCode,
              speechManager = speechManager,
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.FLASHCARD_FLIP -> {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .clickable {
                  isFlashcardFlipped = !isFlashcardFlipped
                  speechManager.speak(currentExercise.targetPhrase, languageCode)
                },
              shape = RoundedCornerShape(18.dp),
              color = if (isFlashcardFlipped) UgElevatedNavy else Color(0xFF132D54),
              border = BorderStroke(1.dp, UgCyan.copy(alpha = 0.55f))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = if (isFlashcardFlipped) "MEANING (${nativeLang.name.uppercase()})" else "${lang.name.uppercase()} • TAP TO FLIP",
                  color = UgCyan,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = if (isFlashcardFlipped) currentExercise.translation else currentExercise.targetPhrase,
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 19.sp,
                  textAlign = TextAlign.Center
                )
                if (!isFlashcardFlipped && currentExercise.phonetic.isNotBlank() && languageCode != "en") {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(text = "[${currentExercise.phonetic}]", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LingoPandaHorizontalAnswerGrid(
              options = currentExercise.resolvedAnswerOptions(),
              correctOptionId = currentExercise.resolvedCorrectOptionId(),
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(opt = opt, recordMistakeOnWrong = true)
              }
            )
          }

          GameInteractionMechanic.SPEAKING_MIC,
          GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE,
          GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT,
          GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE,
          GameInteractionMechanic.SPEAKING_QUICK_ANSWER,
          GameInteractionMechanic.SPEAKING_SPEECH_COACH,
          GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT,
          GameInteractionMechanic.SPEAKING_SHADOWING,
          GameInteractionMechanic.SPEAKING_DIALOGUE_LINE,
          GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION -> {
            DedicatedSpeakingGameStageHost(
              game = game,
              exercise = currentExercise,
              languageCode = languageCode,
              nativeLanguageCode = nativeLanguageCode,
              accentColor = Color(game.accentHex),
              speechManager = speechManager,
              isListening = isListening,
              spokenText = spokenText,
              onSaveFlashcard = { card ->
                onSaveFlashcard(card)
                savedWordBanner = "Saved \"${card.word}\""
              },
              onSpeakingEvaluationCompleted = { eval, _ ->
                handleEvaluationCompleted(eval)
              }
            )
          }

          GameInteractionMechanic.AI_CONVERSATION_MISSION -> {
            // Genuine Interactive Partner Chat — NEVER shows fake user messages!
            val partnerPromptLine = currentExercise.conversationStarter.ifBlank { currentExercise.prompt }
            Surface(
              shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
              color = UgElevatedNavy,
              border = BorderStroke(1.dp, UgCyan.copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("conversation_partner_bubble")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${currentExercise.conversationRole} (Turn ${currentIndex + 1}/${exercises.size})",
                    color = UgCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                  IconButton(
                    onClick = { speechManager.speak(partnerPromptLine, languageCode) },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to partner", tint = UgCyan, modifier = Modifier.size(16.dp))
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = partnerPromptLine,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
              }
            }

            if (submittedConversationMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                color = UgCyan.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, UgCyan),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("You", color = UgCyan, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                  Text(submittedConversationMessage!!, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }

            if (aiConversationReply != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = UgElevatedNavy,
                border = BorderStroke(1.dp, UgGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "${currentExercise.conversationRole}: $aiConversationReply",
                  color = Color.White,
                  fontSize = 13.5.sp,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }

            if (!isExerciseAnswered) {
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = {
                    if (isListening) {
                      speechManager.finishListeningNow()
                    } else if (hasMicPermission) {
                      speechManager.startListening(languageCode)
                    } else {
                      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = if (isListening) UgCoral else UgPurple),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(48.dp)
                ) {
                  Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Speak reply",
                    tint = Color.White
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isListening) "Done" else "Speak", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                  value = typedAnswer,
                  onValueChange = { typedAnswer = it },
                  placeholder = { Text("Reply in ${lang.name}...", fontSize = 13.sp) },
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("conversation_mission_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = UgCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                  )
                )
              }

              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = {
                  val msg = typedAnswer.trim()
                  submittedConversationMessage = msg
                  scope.launch {
                    isEvaluatingAiTurn = true
                    val (eval, partnerNext) = DynamicAiGameEngine.evaluateConversationTurnWithAi(
                      game = game,
                      languageCode = languageCode,
                      nativeLanguageCode = nativeLanguageCode,
                      partnerLine = partnerPromptLine,
                      userMessage = msg,
                      turnNumber = currentIndex + 1
                    )
                    aiConversationReply = partnerNext.ifBlank { null }
                    isEvaluatingAiTurn = false
                    handleEvaluationCompleted(eval)
                  }
                },
                enabled = typedAnswer.isNotBlank() && !isEvaluatingAiTurn,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("send_conversation_turn_button"),
                colors = ButtonDefaults.buttonColors(containerColor = UgCyan)
              ) {
                Text(
                  text = if (isEvaluatingAiTurn) "Partner is replying..." else "Send Reply",
                  color = UgDeepNavy,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }

          GameInteractionMechanic.ESCAPE_ROOM_PUZZLE -> {
            EscapeRoomVaultHeader(
              currentStage = (currentIndex + 1).coerceAtMost(exercises.size),
              totalStages = exercises.size.coerceAtLeast(4),
              unlockedCodes = unlockedEscapeCodes
            )
            Spacer(modifier = Modifier.height(12.dp))
            LingoPandaHorizontalAnswerGrid(
              options = currentExercise.resolvedAnswerOptions(),
              correctOptionId = currentExercise.resolvedCorrectOptionId(),
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(
                  opt = opt,
                  recordMistakeOnWrong = true,
                  speakTargetOnCorrect = true,
                  unlockEscapeClueOnCorrect = true
                )
              }
            )
          }

          else -> {
            LingoPandaHorizontalAnswerGrid(
              options = currentExercise.resolvedAnswerOptions(),
              correctOptionId = currentExercise.resolvedCorrectOptionId(),
              selectedOptionId = selectedOptionId,
              onSelectOption = { opt ->
                handleOptionSelection(
                  opt = opt,
                  recordMistakeOnWrong = true,
                  speakTargetOnCorrect = true
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hint & Save Word Row
        // In Brain Games, Spelling and Vocabulary (when not in a speaking/listening/active timer exercise),
        // Free users can optionally tap "Watch an ad for a hint" to unlock the hint via the reward callback.
        // Pro users get immediate hints with zero ads.
        val isRewardedHintEligibleCategory = game.category == UltimateGameCategory.BRAIN_CHALLENGE ||
          game.category == UltimateGameCategory.SPELLING ||
          game.category == UltimateGameCategory.VOCABULARY
        val isSpeakingOrListeningOrTimedMechanic = currentExercise.mechanic.isSpeakingMechanic ||
          game.category == UltimateGameCategory.LISTENING ||
          currentExercise.mechanic == GameInteractionMechanic.LISTENING_COMPREHENSION ||
          currentExercise.mechanic == GameInteractionMechanic.LISTENING_STORY_QUIZ ||
          currentExercise.mechanic == GameInteractionMechanic.CONVERSATION_LISTENING ||
          currentExercise.mechanic == GameInteractionMechanic.SOUND_DETECTIVE ||
          currentExercise.mechanic == GameInteractionMechanic.MISSING_AUDIO_WORD ||
          currentExercise.mechanic == GameInteractionMechanic.FAST_SPEECH_CHALLENGE ||
          (currentExercise.mechanic == GameInteractionMechanic.SPEED_RUN_TIMER && !isExerciseAnswered && timeLeftSeconds > 0)
        val requiresRewardedAdForHint = com.example.ads.AdService.shouldShowAds(userProfile) &&
          isRewardedHintEligibleCategory &&
          !isSpeakingOrListeningOrTimedMechanic &&
          !hintUnlockedByRewardedAd

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (requiresRewardedAdForHint) {
            TextButton(
              onClick = {
                val started = com.example.ads.AdService.showRewardedAdForGameBonus(
                  userProfile = userProfile,
                  rewardType = com.example.ads.RewardedFeatureType.GAME_HINT,
                  itemId = "${game.id}_${levelNumber}_${currentIndex}_hint",
                  onRewardEarned = {
                    // Grant hint ONLY in the reward callback
                    hintUnlockedByRewardedAd = true
                    showHint = true
                  },
                  onFailedOrCancelled = { reason ->
                    rewardStatusBanner = reason
                  }
                )
                if (started) {
                  com.example.ads.AdService.completeActiveRewardedAdWithReward()
                }
              },
              modifier = Modifier.testTag("watch_ad_for_hint_button")
            ) {
              Icon(Icons.Default.Lightbulb, contentDescription = null, tint = UgAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Watch an ad for a hint", color = UgAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            TextButton(
              onClick = { showHint = !showHint },
              modifier = Modifier.testTag("toggle_hint_button")
            ) {
              Icon(Icons.Default.Lightbulb, contentDescription = null, tint = UgAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (showHint) "Hide Hint" else "Hint", color = UgAmber, fontSize = 12.sp)
            }
          }

          TextButton(
            onClick = {
              onSaveFlashcard(
                VocabFlashcard(
                  id = "ug_${currentExercise.targetPhrase.hashCode()}",
                  word = currentExercise.targetPhrase,
                  translation = currentExercise.translation,
                  phonetic = currentExercise.phonetic
                )
              )
              savedWordBanner = "Saved \"${currentExercise.targetPhrase}\""
            }
          ) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = UgCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Word", color = UgCyan, fontSize = 12.sp)
          }
        }

        if (rewardStatusBanner != null) {
          Text(
            text = rewardStatusBanner!!,
            color = UgAmber,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        AnimatedVisibility(visible = showHint, enter = fadeIn(), exit = fadeOut()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = UgAmber.copy(alpha = 0.14f),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp)
              .testTag("game_hint_banner")
          ) {
            Text(
              text = currentExercise.hint,
              color = UgAmber,
              fontSize = 12.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        // Feedback & Continue Banner
        AnimatedVisibility(visible = isExerciseAnswered, enter = fadeIn(), exit = fadeOut()) {
          val resolvedCorrectText = currentExercise.resolvedAnswerOptions()
            .firstOrNull { it.id == currentExercise.resolvedCorrectOptionId() }
            ?.text
            ?: currentExercise.targetPhrase
          val isRight = currentExercise.isOptionCorrect(selectedOptionId) ||
            (evaluationResult?.isPassed == true)
          Column(modifier = Modifier.padding(top = 8.dp)) {
            if (!currentExercise.mechanic.isSpeakingMechanic) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRight) UgGreen.copy(alpha = 0.14f) else UgCoral.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, if (isRight) UgGreen.copy(alpha = 0.85f) else UgCoral.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(
                    text = evaluationResult?.feedbackTitle ?: if (isRight) "Correct!" else "Answer: $resolvedCorrectText",
                    color = if (isRight) UgGreen else UgCoral,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = evaluationResult?.detailedExplanation ?: currentExercise.explanation,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
              onClick = { advanceOrCompleteLevel() },
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("continue_next_exercise_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = UgCyan)
            ) {
              Text(
                text = if (currentIndex + 1 < exercises.size) "Continue" else "Finish Level $levelNumber",
                color = UgDeepNavy,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
              )
            }
          }
        }
      }
    }

    // Generous bottom clearance ensuring Continue is comfortably visible above bottom navigation
    Spacer(modifier = Modifier.height(36.dp))
  }
}
