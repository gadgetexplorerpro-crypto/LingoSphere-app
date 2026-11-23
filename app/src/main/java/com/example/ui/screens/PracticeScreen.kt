package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ai.DynamicQuizQuestion
import com.example.ai.GeminiTutorService
import com.example.data.model.AiCustomLesson
import com.example.data.model.CommunityProfile
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.SrsCardMetadata
import com.example.data.model.SrsRating
import com.example.data.model.UserProfile
import com.example.data.repository.DailyGameMissionsState
import com.example.data.repository.DynamicAiGameEngine
import com.example.data.repository.InteractiveGameType
import com.example.data.repository.LanguageGamesData
import com.example.data.repository.LanguageMiniGame
import com.example.data.repository.MiniGameCategory
import com.example.data.repository.UltimateGameCategory
import com.example.data.repository.UltimateLanguageGame
import com.example.data.repository.ValidatedAnswerOption
import com.example.speech.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.material.icons.automirrored.filled.MenuBook

private val GamesDeepNavy = Color(0xFF060D1D)
private val GamesCardNavy = Color(0xFF0D162B)
private val GamesSubtleNavy = Color(0xFF13203D)
private val GamesBorder = Color(0xFF1D2E52)
private val GamesCyan = Color(0xFF38BDF8)
private val GamesGreen = Color(0xFF10B981)
private val GamesAmber = Color(0xFFF59E0B)
private val GamesOrange = Color(0xFFFF6D00)

private sealed interface GameZoneScreen {
  data object Home : GameZoneScreen
  data class UltimateCategoryDetail(val categoryKey: String) : GameZoneScreen
  data class UltimateLevelSelect(
    val game: UltimateLanguageGame,
    val returnScreen: GameZoneScreen = Home
  ) : GameZoneScreen
  data class UltimateLevelGameplay(
    val game: UltimateLanguageGame,
    val levelNumber: Int,
    val returnScreen: GameZoneScreen = Home
  ) : GameZoneScreen
  data object PictureWord : GameZoneScreen
  data object SpellBee : GameZoneScreen
  data object Wordigo : GameZoneScreen
  data object DailyChallengeHub : GameZoneScreen
  data object WordMatchInteractive : GameZoneScreen
  data object VocabMemoryInteractive : GameZoneScreen
  data object CustomLessonCreator : GameZoneScreen
  data class CategoryDetail(val category: MiniGameCategory) : GameZoneScreen
  data class GamePlay(val game: LanguageMiniGame) : GameZoneScreen
  data class GameResults(val game: LanguageMiniGame, val correctCount: Int, val total: Int) : GameZoneScreen
  data object BattleArena : GameZoneScreen
  data object AiQuiz : GameZoneScreen
  data object AllGamesList : GameZoneScreen
}

private data class ExploreCategoryCardSpec(
  val key: String,
  val title: String,
  val subtitle: String,
  val emoji: String,
  val accentHex: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
  userProfile: UserProfile,
  flashcards: List<FlashcardProgress>,
  speechManager: SpeechManager,
  onReviewCard: (FlashcardProgress, Boolean) -> Unit,
  communityProfiles: List<CommunityProfile> = emptyList(),
  onCompleteGame: (gameId: String, title: String, scorePercent: Int, xpEarned: Int, badgeId: String?) -> Unit = { _, _, _, _, _ -> },
  onAddCustomCard: (front: String, back: String, phonetic: String) -> Unit = { _, _, _ -> },
  srsMetadataMap: Map<String, SrsCardMetadata> = emptyMap(),
  onReviewCardWithRating: (FlashcardProgress, SrsRating) -> Unit = { card, rating ->
    onReviewCard(card, rating != SrsRating.AGAIN)
  },
  mistakeNotebookEntries: List<MistakeNotebookEntry> = emptyList(),
  onAddMistakeEntry: (MistakeNotebookEntry) -> Unit = {},
  onResolveMistakeEntry: (String) -> Unit = {},
  onOpenMistakeRecovery: () -> Unit = {},
  onOpenCameraLens: () -> Unit = {},
  onOpenVocabularyBuilder: () -> Unit = {},
  onOpenPronunciationCoach: () -> Unit = {},
  onOpenBooksLibrary: () -> Unit = {},
  onOpenPremiumHub: () -> Unit = {},
  savedCustomLessons: List<AiCustomLesson> = emptyList(),
  onSaveCustomLesson: (AiCustomLesson) -> Unit = {}
) {
  val context = LocalContext.current
  val lang = Language.findByCode(userProfile.targetLanguageCode)
  val scope = rememberCoroutineScope()
  val activeUserId = userProfile.userId.ifBlank { "default_learner" }

  var currentScreen by remember { mutableStateOf<GameZoneScreen>(GameZoneScreen.Home) }
  var progressRefreshTick by remember { mutableIntStateOf(0) }

  var lastPlayedUltimateGame by remember(userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
    mutableStateOf(DynamicAiGameEngine.getLastPlayedGame(context, userProfile.targetLanguageCode, activeUserId))
  }

  val localCustomLessons = remember { mutableStateListOf<AiCustomLesson>() }
  val effectiveCustomLessons = remember(savedCustomLessons, localCustomLessons.size) {
    (savedCustomLessons + localCustomLessons).distinctBy { it.id }
  }

  val allGames = remember(userProfile.targetLanguageCode) {
    LanguageGamesData.getGamesForLanguage(userProfile.targetLanguageCode)
  }

  // Legacy arcade game state
  var gameRoundIndex by remember { mutableIntStateOf(0) }
  var gameSelectedOptionId by remember { mutableStateOf<String?>(null) }
  var gameCorrectCount by remember { mutableIntStateOf(0) }
  var gameStreak by remember { mutableIntStateOf(0) }

  // Daily mission state
  var dailyMissionsState by remember(progressRefreshTick) {
    mutableStateOf(DynamicAiGameEngine.getDailyMissionsState(context))
  }
  var gamesCompletedToday by rememberSaveable { mutableIntStateOf(0) }
  var questionsAnsweredToday by rememberSaveable { mutableIntStateOf(0) }
  var dailyMissionClaimed by rememberSaveable { mutableStateOf(false) }

  // Multiplayer Battle Arena state
  var battleActive by remember { mutableStateOf(false) }
  var battleOpponentName by remember { mutableStateOf("Elena R.") }
  var battleOpponentFlag by remember { mutableStateOf(lang.flagEmoji) }
  var battleRoundIndex by remember { mutableIntStateOf(0) }
  var myBattleScore by remember { mutableIntStateOf(0) }
  var opponentBattleScore by remember { mutableIntStateOf(0) }
  var battleSelectedOptionId by remember { mutableStateOf<String?>(null) }
  var battleFinished by remember { mutableStateOf(false) }
  var battleTimeLeft by remember { mutableIntStateOf(12) }

  val battleRounds = remember(allGames) {
    allGames.flatMap { it.rounds }.shuffled().take(6)
  }

  LaunchedEffect(battleActive, battleRoundIndex, battleSelectedOptionId, battleFinished) {
    if (battleActive && !battleFinished && battleSelectedOptionId == null) {
      battleTimeLeft = 12
      while (battleTimeLeft > 0 && battleSelectedOptionId == null) {
        delay(1000L)
        battleTimeLeft--
      }
      if (battleTimeLeft == 0 && battleSelectedOptionId == null) {
        battleSelectedOptionId = "__timeout__"
        opponentBattleScore += 120
      }
    }
  }

  // Adaptive Quiz state
  var quizQuestions by remember { mutableStateOf<List<DynamicQuizQuestion>>(emptyList()) }
  var isGeneratingQuiz by remember { mutableStateOf(false) }
  var quizIndex by remember { mutableIntStateOf(0) }
  var quizSelectedOptionId by remember { mutableStateOf<String?>(null) }
  var quizScore by remember { mutableIntStateOf(0) }
  var quizCompleted by remember { mutableStateOf(false) }
  val selectedQuizTopic by remember { mutableStateOf("Real-World Travel & Conversation") }

  // Direct launch flow: Games -> Category -> Game -> Play current playable level immediately
  fun launchUltimateGame(game: UltimateLanguageGame, fromScreen: GameZoneScreen = currentScreen) {
    lastPlayedUltimateGame = game
    val prog = DynamicAiGameEngine.getGameProgress(
      context = context,
      languageCode = userProfile.targetLanguageCode,
      gameId = game.id,
      userId = activeUserId
    )
    val playableLevel = prog.highestUnlockedLevel.coerceIn(1, game.maxLevels)
    val origin = when (fromScreen) {
      is GameZoneScreen.UltimateCategoryDetail -> fromScreen
      else -> GameZoneScreen.Home
    }
    if (!userProfile.isPremium && playableLevel > game.freeLevelsLimit) {
      currentScreen = GameZoneScreen.UltimateLevelSelect(game, returnScreen = origin)
    } else {
      currentScreen = GameZoneScreen.UltimateLevelGameplay(
        game = game,
        levelNumber = playableLevel,
        returnScreen = origin
      )
    }
  }

  fun launchLegacyGameDirect(game: LanguageMiniGame) {
    gameRoundIndex = 0
    gameSelectedOptionId = null
    gameCorrectCount = 0
    gameStreak = 0
    currentScreen = GameZoneScreen.GamePlay(game)
  }

  fun launchInteractiveGame(type: InteractiveGameType) {
    when (type) {
      InteractiveGameType.PICTURE_WORD_GAME -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
        launchUltimateGame(g)
      }
      InteractiveGameType.SPELL_BEE -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_16_spellbee" }
        launchUltimateGame(g)
      }
      InteractiveGameType.WORDIGO -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_17_wordigo" }
        launchUltimateGame(g)
      }
      InteractiveGameType.DAILY_CHALLENGE -> currentScreen = GameZoneScreen.DailyChallengeHub
      InteractiveGameType.WORD_MATCH -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_02_word_match" }
        launchUltimateGame(g)
      }
      InteractiveGameType.VOCABULARY_MEMORY -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_04_memory_pairs" }
        launchUltimateGame(g)
      }
      InteractiveGameType.LISTENING_CHALLENGE -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.LISTENING }
        launchUltimateGame(g)
      }
      InteractiveGameType.SENTENCE_BUILDER -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_42_sentence_builder" }
        launchUltimateGame(g)
      }
      InteractiveGameType.PRONUNCIATION_CHALLENGE -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.SPEAKING }
        launchUltimateGame(g)
      }
      InteractiveGameType.CONVERSATION_CHALLENGE -> {
        val g = DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.CONVERSATION }
        launchUltimateGame(g)
      }
    }
  }

  // Dedicated full-screen gameplay and level selector views
  when (val activeScreen = currentScreen) {
    is GameZoneScreen.UltimateLevelSelect -> {
      val gameProgress = remember(activeScreen.game.id, userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
        DynamicAiGameEngine.getGameProgress(
          context = context,
          languageCode = userProfile.targetLanguageCode,
          gameId = activeScreen.game.id,
          userId = activeUserId
        )
      }
      UltimateGameLevelSelectorView(
        game = activeScreen.game,
        languageCode = userProfile.targetLanguageCode,
        isProSubscriber = userProfile.isPremium,
        progressState = gameProgress,
        onSelectLevel = { levelNum ->
          currentScreen = GameZoneScreen.UltimateLevelGameplay(
            game = activeScreen.game,
            levelNumber = levelNum,
            returnScreen = activeScreen.returnScreen
          )
        },
        onOpenPremiumPaywall = onOpenPremiumHub,
        onBack = { currentScreen = activeScreen.returnScreen }
      )
      return
    }
    is GameZoneScreen.UltimateLevelGameplay -> {
      UltimateDynamicLevelGameplayView(
        game = activeScreen.game,
        levelNumber = activeScreen.levelNumber,
        languageCode = userProfile.targetLanguageCode,
        nativeLanguageCode = userProfile.nativeLanguageCode,
        speechManager = speechManager,
        userProfile = userProfile,
        onSaveFlashcard = { card ->
          onAddCustomCard(card.word, card.translation, card.phonetic)
        },
        onAddMistakeEntry = onAddMistakeEntry,
        onGrantBonusXp = { bonusXp ->
          if (bonusXp > 0) {
            onCompleteGame(
              "${activeScreen.game.id}_lvl_${activeScreen.levelNumber}_double_xp",
              "${activeScreen.game.title} (Double XP Bonus)",
              100,
              bonusXp,
              null
            )
          }
        },
        onOpenPremiumPaywall = onOpenPremiumHub,
        onLevelFinished = { completedLevel, accuracyPct, xpEarned, missedWords, learnedWords ->
          val wasCompletedBefore = DynamicAiGameEngine.wasLevelCompletedBefore(
            context = context,
            languageCode = userProfile.targetLanguageCode,
            gameId = activeScreen.game.id,
            levelNumber = completedLevel,
            userId = activeUserId
          )
          DynamicAiGameEngine.saveGameProgress(
            context = context,
            languageCode = userProfile.targetLanguageCode,
            gameId = activeScreen.game.id,
            levelNumber = completedLevel,
            accuracyPercent = accuracyPct,
            missedWords = missedWords,
            wordsLearnedCount = learnedWords.size.coerceAtLeast(2),
            category = activeScreen.game.category,
            userId = activeUserId
          )
          val effectiveXp = if (accuracyPct >= 70 && !wasCompletedBefore) xpEarned else 0
          if (accuracyPct >= 70 && !wasCompletedBefore) {
            gamesCompletedToday++
          }
          progressRefreshTick++
          onCompleteGame(
            "${activeScreen.game.id}_lvl_$completedLevel",
            "${activeScreen.game.title} (Level $completedLevel)",
            accuracyPct,
            effectiveXp,
            if (accuracyPct >= 70) "ultimate_${activeScreen.game.id}" else null
          )
        },
        onPlayNextLevel = { nextLevel ->
          currentScreen = GameZoneScreen.UltimateLevelGameplay(
            game = activeScreen.game,
            levelNumber = nextLevel,
            returnScreen = activeScreen.returnScreen
          )
        },
        onBackToLevelSelect = {
          progressRefreshTick++
          currentScreen = GameZoneScreen.UltimateLevelSelect(
            game = activeScreen.game,
            returnScreen = activeScreen.returnScreen
          )
        },
        onNavigateBack = {
          progressRefreshTick++
          currentScreen = activeScreen.returnScreen
        },
        userId = activeUserId
      )
      return
    }
    GameZoneScreen.PictureWord -> {
      PictureWordGameView(
        languageName = lang.name,
        languageCode = userProfile.targetLanguageCode,
        cefrLevel = userProfile.proficiencyLevel,
        speechManager = speechManager,
        onSaveFlashcard = { card -> onAddCustomCard(card.word, card.translation, card.phonetic) },
        onCompleteGame = { id, pct, xp ->
          gamesCompletedToday++
          DynamicAiGameEngine.recordDailyMissionProgress(context, UltimateGameCategory.VOCABULARY, 5)
          progressRefreshTick++
          com.example.ads.AdService.onGameCompletedBeforeResults(
            userProfile = userProfile,
            gameTitle = "Picture Word Quiz"
          )
          onCompleteGame(id, "Picture Word Quiz", pct, xp, "picture_word_master")
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    GameZoneScreen.SpellBee -> {
      SpellBeeGameView(
        languageName = lang.name,
        languageCode = userProfile.targetLanguageCode,
        speechManager = speechManager,
        onCompleteGame = { id, pct, xp ->
          gamesCompletedToday++
          DynamicAiGameEngine.recordDailyMissionProgress(context, UltimateGameCategory.SPELLING, 3)
          progressRefreshTick++
          com.example.ads.AdService.onGameCompletedBeforeResults(
            userProfile = userProfile,
            gameTitle = "SpellBee"
          )
          onCompleteGame(id, "SpellBee", pct, xp, "spellbee_champion")
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    GameZoneScreen.Wordigo -> {
      WordigoGameView(
        languageName = lang.name,
        languageCode = userProfile.targetLanguageCode,
        speechManager = speechManager,
        onCompleteGame = { id, pct, xp ->
          gamesCompletedToday++
          DynamicAiGameEngine.recordDailyMissionProgress(context, UltimateGameCategory.SPELLING, 2)
          progressRefreshTick++
          com.example.ads.AdService.onGameCompletedBeforeResults(
            userProfile = userProfile,
            gameTitle = "Wordigo"
          )
          onCompleteGame(id, "Wordigo", pct, xp, "wordigo_solver")
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    GameZoneScreen.DailyChallengeHub -> {
      DailyLanguageChallengeView(
        languageName = lang.name,
        languageCode = userProfile.targetLanguageCode,
        speechManager = speechManager,
        isAlreadyCompletedToday = dailyMissionClaimed,
        userProfile = userProfile,
        onGrantBonusXp = { bonusXp ->
          onCompleteGame("daily_challenge_double_xp", "Daily Challenge Double XP", 100, bonusXp, null)
        },
        onCompleteDailyChallenge = {
          if (!dailyMissionClaimed) {
            dailyMissionClaimed = true
            gamesCompletedToday++
            DynamicAiGameEngine.recordDailyMissionProgress(context, UltimateGameCategory.SPEAKING, 5)
            progressRefreshTick++
            onCompleteGame("daily_challenge_hub", "Daily Language Challenge", 100, 50, "daily_streak_star")
          }
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    GameZoneScreen.WordMatchInteractive -> {
      WordMatchInteractiveGameView(
        languageCode = userProfile.targetLanguageCode,
        speechManager = speechManager,
        onCompleteGame = { id, pct, xp ->
          gamesCompletedToday++
          DynamicAiGameEngine.recordDailyMissionProgress(context, UltimateGameCategory.VOCABULARY, 4)
          progressRefreshTick++
          onCompleteGame(id, "Word Match", pct, xp, "word_match_speed")
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    GameZoneScreen.VocabMemoryInteractive -> {
      VocabularyMemoryGridGameView(
        languageCode = userProfile.targetLanguageCode,
        speechManager = speechManager,
        onCompleteGame = { id, pct, xp ->
          gamesCompletedToday++
          DynamicAiGameEngine.recordDailyMissionProgress(context, UltimateGameCategory.VOCABULARY, 4)
          progressRefreshTick++
          onCompleteGame(id, "Memory Pairs", pct, xp, "vocab_memory_pro")
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    GameZoneScreen.CustomLessonCreator -> {
      CustomLessonCreatorView(
        languageName = lang.name,
        languageCode = userProfile.targetLanguageCode,
        cefrLevel = userProfile.proficiencyLevel,
        speechManager = speechManager,
        savedCustomLessons = effectiveCustomLessons,
        onSaveCustomLesson = { lesson ->
          localCustomLessons.removeAll { l -> l.id == lesson.id }
          localCustomLessons.add(0, lesson)
          onSaveCustomLesson(lesson)
        },
        onSaveFlashcard = { card -> onAddCustomCard(card.word, card.translation, card.phonetic) },
        onCompleteLesson = { id, pct, xp ->
          gamesCompletedToday++
          progressRefreshTick++
          onCompleteGame(id, "Custom Lesson", pct, xp, "custom_lesson_creator")
        },
        onBack = { currentScreen = GameZoneScreen.Home }
      )
      return
    }
    else -> {}
  }

  if (currentScreen != GameZoneScreen.Home) {
    BackHandler {
      currentScreen = GameZoneScreen.Home
    }
  }

  val completedMissionTasks = remember(dailyMissionsState) {
    listOf(
      dailyMissionsState.vocabGamesCompleted >= 2,
      dailyMissionsState.speakingMinutesCompleted >= 3,
      dailyMissionsState.listeningChallengesCompleted >= 1,
      dailyMissionsState.newWordsLearned >= 5
    ).count { it }
  }
  val dailyGoalTarget = 4
  val dailyGoalCompleted = (completedMissionTasks + if (dailyMissionClaimed) 1 else 0).coerceAtMost(dailyGoalTarget)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(GamesDeepNavy)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("practice_games_hub_screen")
  ) {
    if (currentScreen == GameZoneScreen.Home) {
      // Clean Top Header: Play & Learn + Subtitle + Compact XP / Streak / Daily Goal Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = lang.flagEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Play & Learn",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              modifier = Modifier.testTag("play_and_learn_title")
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Practice a little. Improve every day.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f)
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = GamesOrange.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, GamesOrange.copy(alpha = 0.38f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = GamesOrange,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${userProfile.currentStreak.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = GamesOrange
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = GamesCyan.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, GamesCyan.copy(alpha = 0.35f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Bolt,
                contentDescription = "XP",
                tint = GamesCyan,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${userProfile.xpPoints} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GamesCyan
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Compact Daily Goal Progress Strip
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = GamesCardNavy,
        border = BorderStroke(1.dp, GamesBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Daily Goal",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(12.dp))
          LinearProgressIndicator(
            progress = { (dailyGoalCompleted.toFloat() / dailyGoalTarget.toFloat()).coerceIn(0.08f, 1f) },
            color = GamesCyan,
            trackColor = Color.White.copy(alpha = 0.1f),
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .clip(RoundedCornerShape(999.dp))
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "$dailyGoalCompleted/$dailyGoalTarget",
            color = GamesCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    } else {
      // Clean Sub-Screen Header (No redundant "Play & Learn Home" or "Game Briefing" labels)
      val subHeaderTitle = when (val s = currentScreen) {
        is GameZoneScreen.UltimateCategoryDetail -> {
          if (s.categoryKey == "DAILY_CHALLENGES") {
            "Daily Challenges"
          } else {
            UltimateGameCategory.entries.firstOrNull { it.name == s.categoryKey }?.label ?: "Games"
          }
        }
        is GameZoneScreen.CategoryDetail -> s.category.label
        is GameZoneScreen.GamePlay -> s.game.title
        is GameZoneScreen.GameResults -> s.game.title
        GameZoneScreen.BattleArena -> "Battle Arena"
        GameZoneScreen.AiQuiz -> "Practice Quiz"
        GameZoneScreen.AllGamesList -> "Search Games"
        else -> "Games"
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { currentScreen = GameZoneScreen.Home },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(GamesCardNavy)
              .testTag("category_detail_back_button")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = subHeaderTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Main Content Router
    when (val screen = currentScreen) {
      GameZoneScreen.Home -> {
        val lastGameProgress = remember(
          lastPlayedUltimateGame.id,
          userProfile.targetLanguageCode,
          activeUserId,
          progressRefreshTick
        ) {
          DynamicAiGameEngine.getGameProgress(
            context = context,
            languageCode = userProfile.targetLanguageCode,
            gameId = lastPlayedUltimateGame.id,
            userId = activeUserId
          )
        }
        val currentPlayableLvl = lastGameProgress.highestUnlockedLevel.coerceIn(1, lastPlayedUltimateGame.maxLevels)
        val lastGameSpec = remember(lastPlayedUltimateGame.id, currentPlayableLvl) {
          DynamicAiGameEngine.getCurriculumObjective(lastPlayedUltimateGame, currentPlayableLvl)
        }
        val smartRecommendations = remember(
          userProfile.targetLanguageCode,
          mistakeNotebookEntries.size,
          activeUserId,
          progressRefreshTick
        ) {
          DynamicAiGameEngine.getSmartRecommendations(
            context = context,
            languageCode = userProfile.targetLanguageCode,
            mistakeEntries = mistakeNotebookEntries,
            userId = activeUserId
          ).take(3)
        }

        val exploreCategories = remember {
          listOf(
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.VOCABULARY.name,
              title = "Vocabulary",
              subtitle = "Picture quizzes, matching & flashcards",
              emoji = "🖼️",
              accentHex = UltimateGameCategory.VOCABULARY.accentHex
            ),
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.SPELLING.name,
              title = "Spelling",
              subtitle = "SpellBee, Wordigo & dictation",
              emoji = "🐝",
              accentHex = UltimateGameCategory.SPELLING.accentHex
            ),
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.SPEAKING.name,
              title = "Speaking",
              subtitle = "Pronunciation & voice fluency",
              emoji = "🎙️",
              accentHex = UltimateGameCategory.SPEAKING.accentHex
            ),
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.LISTENING.name,
              title = "Listening",
              subtitle = "Audio comprehension & dialogues",
              emoji = "🎧",
              accentHex = UltimateGameCategory.LISTENING.accentHex
            ),
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.GRAMMAR.name,
              title = "Grammar",
              subtitle = "Sentence builder & grammar repair",
              emoji = "🧩",
              accentHex = UltimateGameCategory.GRAMMAR.accentHex
            ),
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.CONVERSATION.name,
              title = "Conversations",
              subtitle = "Real-world shops, travel & cafés",
              emoji = "💬",
              accentHex = UltimateGameCategory.CONVERSATION.accentHex
            ),
            ExploreCategoryCardSpec(
              key = UltimateGameCategory.BRAIN_CHALLENGE.name,
              title = "Brain Games",
              subtitle = "Word search, escape locks & riddles",
              emoji = "🧠",
              accentHex = UltimateGameCategory.BRAIN_CHALLENGE.accentHex
            ),
            ExploreCategoryCardSpec(
              key = "DAILY_CHALLENGES",
              title = "Daily Challenges",
              subtitle = "Missions, custom lessons & duels",
              emoji = "🔥",
              accentHex = 0xFFFF6D00L
            )
          )
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 20.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          // 1. CONTINUE PLAYING — One Compact Card
          item {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { launchUltimateGame(lastPlayedUltimateGame, GameZoneScreen.Home) }
                .testTag("continue_playing_card"),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
              border = BorderStroke(1.2.dp, GamesCyan.copy(alpha = 0.45f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "CONTINUE PLAYING",
                    color = GamesCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                  )
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                      .clickable {
                        currentScreen = GameZoneScreen.UltimateLevelSelect(
                          game = lastPlayedUltimateGame,
                          returnScreen = GameZoneScreen.Home
                        )
                      }
                      .testTag("continue_playing_levels_button")
                  ) {
                    Text(
                      text = "Levels",
                      color = Color.White.copy(alpha = 0.85f),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(lastPlayedUltimateGame.accentHex).copy(alpha = 0.18f),
                      modifier = Modifier.size(44.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text(lastPlayedUltimateGame.iconEmoji, fontSize = 22.sp)
                      }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = lastPlayedUltimateGame.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "Level $currentPlayableLvl • ${lastGameSpec.objectiveTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  Button(
                    onClick = { launchUltimateGame(lastPlayedUltimateGame, GameZoneScreen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = GamesCyan),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("continue_playing_action_button")
                  ) {
                    Icon(
                      Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = GamesDeepNavy,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Play",
                      color = GamesDeepNavy,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 13.sp
                    )
                  }
                }
              }
            }
          }

          // 2. DAILY CHALLENGE — One Compact Card (Expandable Missions Optional)
          item {
            CompactDailyChallengeHubCard(
              missionsState = dailyMissionsState,
              isDailyChallengeDone = dailyMissionClaimed,
              onOpenDailyChallenge = { currentScreen = GameZoneScreen.DailyChallengeHub },
              onStartMissionAction = { missionIndex ->
                when (missionIndex) {
                  1 -> launchUltimateGame(
                    DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.VOCABULARY }
                  )
                  2 -> launchUltimateGame(
                    DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.SPEAKING }
                  )
                  3 -> launchUltimateGame(
                    DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.LISTENING }
                  )
                  else -> launchUltimateGame(
                    DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
                  )
                }
              },
              onClaimReward = { missionIndex ->
                val missionId = "mission_$missionIndex"
                if (!dailyMissionsState.claimedMissionIds.contains(missionId)) {
                  dailyMissionsState = DynamicAiGameEngine.claimDailyMissionReward(context, missionId)
                  progressRefreshTick++
                  val xp = if (missionIndex == 2) 30 else if (missionIndex == 4) 20 else 25
                  onCompleteGame(
                    "daily_mission_${missionIndex}_${dailyMissionsState.dateKey}",
                    "Daily Mission #$missionIndex",
                    100,
                    xp,
                    "daily_mission_star"
                  )
                }
              }
            )
          }

          // 3. RECOMMENDED FOR YOU — Only 3 Personalized Games
          item {
            Text(
              text = "Recommended for You",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recommended_games_row")
            ) {
              items(smartRecommendations, key = { "rec_${it.first.id}" }) { (recGame, reason) ->
                val prog = remember(recGame.id, userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
                  DynamicAiGameEngine.getGameProgress(
                    context = context,
                    languageCode = userProfile.targetLanguageCode,
                    gameId = recGame.id,
                    userId = activeUserId
                  )
                }
                SmartRecommendationGameChipCard(
                  game = recGame,
                  recommendationReason = reason,
                  currentLevel = prog.highestUnlockedLevel,
                  totalStars = prog.totalStarsEarned,
                  onClick = { launchUltimateGame(recGame, GameZoneScreen.Home) }
                )
              }
            }
          }

          // 4. EXPLORE GAMES — Clean 2-Column Grid of the 8 Categories
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Explore Games",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              TextButton(
                onClick = { currentScreen = GameZoneScreen.AllGamesList },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("see_all_games_button")
              ) {
                Icon(
                  Icons.Default.Search,
                  contentDescription = null,
                  tint = GamesCyan,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Search",
                  color = GamesCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("explore_games_category_grid")
            ) {
              for (i in exploreCategories.indices step 2) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  val cat1 = exploreCategories[i]
                  ExploreCategoryGridCard(
                    modifier = Modifier.weight(1f),
                    spec = cat1,
                    onClick = {
                      currentScreen = GameZoneScreen.UltimateCategoryDetail(cat1.key)
                    }
                  )
                  if (i + 1 < exploreCategories.size) {
                    val cat2 = exploreCategories[i + 1]
                    ExploreCategoryGridCard(
                      modifier = Modifier.weight(1f),
                      spec = cat2,
                      onClick = {
                        currentScreen = GameZoneScreen.UltimateCategoryDetail(cat2.key)
                      }
                    )
                  } else {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
                // After the 4th card (i == 2 -> cards 1..4), show ONE full-width native ad card
                // so it appears after 4 cards and never renders two ads in the same viewport.
                if (i == 2) {
                  com.example.ads.LingoGameGridNativeAdCard(
                    userProfile = userProfile,
                    slotIndex = 1,
                    onGoProClick = onOpenPremiumHub
                  )
                }
              }
            }
          }
        }
      }

      // Dedicated Category Screen: Displays ONLY the games inside the chosen category
      is GameZoneScreen.UltimateCategoryDetail -> {
        if (screen.categoryKey == "DAILY_CHALLENGES") {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier
              .fillMaxSize()
              .testTag("daily_challenges_category_screen")
          ) {
            item {
              Text(
                text = "Complete daily practice goals, custom scenarios, and live vocabulary duels.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f)
              )
            }

            item {
              PracticeChallengeFeatureRowCard(
                title = "Daily Language Challenge",
                subtitle = "Vocabulary, dialogue, speaking & quick review",
                badgeText = if (dailyMissionClaimed) "Completed ✓" else "+50 XP",
                accentColor = GamesOrange,
                iconType = "DAILY_CHALLENGE",
                testTag = "practice_card_daily_challenge",
                onClick = { currentScreen = GameZoneScreen.DailyChallengeHub }
              )
            }

            item {
              TodaysMissionsDetailCard(
                missionsState = dailyMissionsState,
                onStartMissionAction = { missionIndex ->
                  when (missionIndex) {
                    1 -> launchUltimateGame(
                      DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.VOCABULARY },
                      screen
                    )
                    2 -> launchUltimateGame(
                      DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.SPEAKING },
                      screen
                    )
                    3 -> launchUltimateGame(
                      DynamicAiGameEngine.ALL_70_GAMES.first { it.category == UltimateGameCategory.LISTENING },
                      screen
                    )
                    else -> launchUltimateGame(
                      DynamicAiGameEngine.ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" },
                      screen
                    )
                  }
                },
                onClaimReward = { missionIndex ->
                  val missionId = "mission_$missionIndex"
                  if (!dailyMissionsState.claimedMissionIds.contains(missionId)) {
                    dailyMissionsState = DynamicAiGameEngine.claimDailyMissionReward(context, missionId)
                    progressRefreshTick++
                    val xp = if (missionIndex == 2) 30 else if (missionIndex == 4) 20 else 25
                    onCompleteGame(
                      "daily_mission_${missionIndex}_${dailyMissionsState.dateKey}",
                      "Daily Mission #$missionIndex",
                      100,
                      xp,
                      "daily_mission_star"
                    )
                  }
                }
              )
            }

            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Card(
                  modifier = Modifier
                    .weight(1f)
                    .clickable { currentScreen = GameZoneScreen.BattleArena },
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
                  border = BorderStroke(1.dp, GamesAmber.copy(alpha = 0.4f))
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text("⚔️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Battle Arena", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("6-round timed duel", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                  }
                }

                Card(
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      currentScreen = GameZoneScreen.AiQuiz
                      if (quizQuestions.isEmpty()) {
                        scope.launch {
                          isGeneratingQuiz = true
                          try {
                            quizQuestions = GeminiTutorService.generateDynamicQuiz(
                              targetLanguage = lang,
                              nativeLanguage = Language.findByCode(userProfile.nativeLanguageCode),
                              proficiencyLevel = userProfile.proficiencyLevel,
                              topic = selectedQuizTopic
                            )
                          } catch (_: Exception) {}
                          isGeneratingQuiz = false
                        }
                      }
                    },
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
                  border = BorderStroke(1.dp, GamesCyan.copy(alpha = 0.4f))
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text("✨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Practice Quiz", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("Personalized review", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                  }
                }
              }
            }

            item {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeChallengeFeatureRowCard(
                  title = "Custom Lesson",
                  subtitle = "Create personalized dialogue & vocabulary sessions",
                  badgeText = "Create",
                  accentColor = GamesGreen,
                  iconType = "CUSTOM_LESSON",
                  testTag = "practice_card_custom_lesson",
                  onClick = { currentScreen = GameZoneScreen.CustomLessonCreator }
                )
                PracticeChallengeFeatureRowCard(
                  title = "Speech Coach",
                  subtitle = "Practice tricky words with voice feedback",
                  badgeText = "Voice",
                  accentColor = GamesCyan,
                  iconType = "SPEECH_COACH",
                  testTag = "practice_card_speech_coach",
                  onClick = onOpenPronunciationCoach
                )
                PracticeChallengeFeatureRowCard(
                  title = "Vocabulary Decks",
                  subtitle = "Browse themed flashcard collections",
                  badgeText = "Decks",
                  accentColor = Color(0xFFFF4081),
                  iconType = "VOCAB_CATEGORIES",
                  testTag = "practice_card_vocabulary_categories",
                  onClick = onOpenVocabularyBuilder
                )
                PracticeChallengeFeatureRowCard(
                  title = "Books & Audiobooks",
                  subtitle = "Read and listen to full classic books",
                  badgeText = "Library",
                  accentColor = GamesAmber,
                  iconType = "BOOKS_LIBRARY",
                  testTag = "practice_card_books_library",
                  onClick = onOpenBooksLibrary
                )
              }
            }
          }
        } else {
          val category = UltimateGameCategory.entries.firstOrNull { it.name == screen.categoryKey }
            ?: UltimateGameCategory.VOCABULARY
          val gamesInCategory = remember(category) {
            DynamicAiGameEngine.getGamesByCategory(category)
          }
          val catListState = androidx.compose.foundation.lazy.rememberLazyListState()
          val activeAdSlotInCatViewport by remember(catListState) {
            derivedStateOf {
              val firstVisible = catListState.firstVisibleItemIndex
              if (firstVisible <= 2) 1 else ((firstVisible / 2) + 1)
            }
          }

          LazyColumn(
            state = catListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier
              .fillMaxSize()
              .testTag("category_games_screen_${category.name}")
          ) {
            item {
              Text(
                text = category.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f)
              )
              Spacer(modifier = Modifier.height(4.dp))
            }

            items((gamesInCategory.indices step 2).toList(), key = { "row_${category.name}_$it" }) { i ->
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  val g1 = gamesInCategory[i]
                  val prog1 = remember(g1.id, userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
                    DynamicAiGameEngine.getGameProgress(
                      context = context,
                      languageCode = userProfile.targetLanguageCode,
                      gameId = g1.id,
                      userId = activeUserId
                    )
                  }
                  UltimateLanguageGameCompactCard(
                    modifier = Modifier.weight(1f),
                    game = g1,
                    currentLevel = prog1.highestUnlockedLevel,
                    totalStars = prog1.totalStarsEarned,
                    onClick = { launchUltimateGame(g1, screen) }
                  )

                  if (i + 1 < gamesInCategory.size) {
                    val g2 = gamesInCategory[i + 1]
                    val prog2 = remember(g2.id, userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
                      DynamicAiGameEngine.getGameProgress(
                        context = context,
                        languageCode = userProfile.targetLanguageCode,
                        gameId = g2.id,
                        userId = activeUserId
                      )
                    }
                    UltimateLanguageGameCompactCard(
                      modifier = Modifier.weight(1f),
                      game = g2,
                      currentLevel = prog2.highestUnlockedLevel,
                      totalStars = prog2.totalStarsEarned,
                      onClick = { launchUltimateGame(g2, screen) }
                    )
                  } else {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
                val cardsCount = i + 2
                if (cardsCount <= gamesInCategory.size && cardsCount % 4 == 0) {
                  val slotIdx = cardsCount / 4
                  if (slotIdx == activeAdSlotInCatViewport) {
                    com.example.ads.LingoGameGridNativeAdCard(
                      userProfile = userProfile,
                      slotIndex = slotIdx,
                      onGoProClick = onOpenPremiumHub
                    )
                  }
                }
              }
            }
          }
        }
      }

      is GameZoneScreen.CategoryDetail -> {
        val cat = screen.category
        val catGames = allGames.filter { it.category == cat }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(catGames, key = { it.id }) { game ->
            val isCompleted = userProfile.completedLessonIds.contains(game.id)
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { launchLegacyGameDirect(game) },
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = GamesCyan.copy(alpha = 0.16f),
                  modifier = Modifier.size(44.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(cat.emoji, fontSize = 20.sp)
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = game.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                if (isCompleted) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = GamesGreen,
                    modifier = Modifier.size(18.dp)
                  )
                } else {
                  Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = GamesCyan,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }

      is GameZoneScreen.GamePlay -> {
        val game = screen.game
        if (gameRoundIndex >= game.rounds.size) {
          LaunchedEffect(Unit) {
            currentScreen = GameZoneScreen.GameResults(game, gameCorrectCount, game.rounds.size)
          }
        } else {
          val round = game.rounds[gameRoundIndex]
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GamesCardNavy)
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Question ${gameRoundIndex + 1} of ${game.rounds.size}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = GamesCyan
                )
                if (gameStreak > 1) {
                  Text(
                    text = "🔥 ${gameStreak}x",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GamesOrange
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              LinearProgressIndicator(
                progress = { (gameRoundIndex + 1).toFloat() / game.rounds.size.coerceAtLeast(1) },
                color = GamesCyan,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp))
              )

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = round.prompt,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.weight(1f)
                )
                if (round.audioPhrase.isNotBlank()) {
                  FilledTonalIconButton(
                    onClick = { speechManager.speak(round.audioPhrase, userProfile.targetLanguageCode) }
                  ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen")
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              val roundAnswerOptions = remember(game.id, gameRoundIndex, round.options) {
                round.options.mapIndexed { idx, txt ->
                  ValidatedAnswerOption(id = "lg_${game.id}_${gameRoundIndex}_$idx", text = txt)
                }
              }
              val roundCorrectOptionId = roundAnswerOptions
                .getOrNull(round.correctIndex.coerceIn(0, (roundAnswerOptions.size - 1).coerceAtLeast(0)))
                ?.id ?: ""

              LingoPandaHorizontalAnswerGrid(
                options = roundAnswerOptions,
                correctOptionId = roundCorrectOptionId,
                selectedOptionId = gameSelectedOptionId,
                tagPrefix = "arcade_round_option",
                onSelectOption = { opt ->
                  gameSelectedOptionId = opt.id
                  if (opt.id == roundCorrectOptionId) {
                    gameCorrectCount++
                    gameStreak++
                    if (round.audioPhrase.isNotBlank()) {
                      speechManager.speak(round.audioPhrase, userProfile.targetLanguageCode)
                    }
                  } else {
                    gameStreak = 0
                  }
                }
              )

              AnimatedVisibility(visible = gameSelectedOptionId != null) {
                Column {
                  Spacer(modifier = Modifier.height(12.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GamesSubtleNavy
                  ) {
                    Text(
                      text = round.explanation,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White.copy(alpha = 0.85f),
                      modifier = Modifier.padding(12.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(12.dp))
                  Button(
                    onClick = {
                      if (gameRoundIndex + 1 < game.rounds.size) {
                        gameRoundIndex++
                        gameSelectedOptionId = null
                      } else {
                        val total = game.rounds.size.coerceAtLeast(1)
                        val pct = ((gameCorrectCount.toFloat() / total) * 100).toInt()
                        val alreadyDone = userProfile.completedLessonIds.contains(game.id)
                        gamesCompletedToday++
                        questionsAnsweredToday += gameCorrectCount
                        onCompleteGame(
                          game.id,
                          game.title,
                          pct,
                          if (pct >= 70 && !alreadyDone) game.xpReward else 0,
                          "game_master_${game.category.name.lowercase()}"
                        )
                        // Show Interstitial before the results screen (every 3rd completed game)
                        com.example.ads.AdService.onGameCompletedBeforeResults(
                          userProfile = userProfile,
                          gameTitle = game.title
                        )
                        currentScreen = GameZoneScreen.GameResults(game, gameCorrectCount, total)
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GamesCyan),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = if (gameRoundIndex + 1 < game.rounds.size) "Next" else "Finish",
                      color = GamesDeepNavy,
                      fontWeight = FontWeight.ExtraBold
                    )
                  }
                }
              }
            }
          }
        }
      }

      is GameZoneScreen.GameResults -> {
        val game = screen.game
        val correct = screen.correctCount
        val total = screen.total.coerceAtLeast(1)
        val pct = ((correct.toFloat() / total) * 100).toInt()
        val passed = pct >= 70
        var doubleXpClaimed by remember(game.id, correct) { mutableStateOf(false) }
        var bonusRewardMessage by remember(game.id, correct) { mutableStateOf<String?>(null) }
        val adState by com.example.ads.AdService.state.collectAsState()
        val showFreeUserRewardOptions = remember(userProfile, adState) {
          com.example.ads.AdService.shouldShowAds(userProfile)
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = GamesCardNavy)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(game.category.emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Session Complete",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Accuracy: $pct% ($correct / $total)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = GamesCyan
            )
            if (bonusRewardMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = bonusRewardMessage!!,
                color = GamesGreen,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
            if (showFreeUserRewardOptions) {
              Spacer(modifier = Modifier.height(12.dp))
              if (!passed) {
                OutlinedButton(
                  onClick = {
                    com.example.ads.AdService.showRewardedAdForGameBonus(
                      userProfile = userProfile,
                      rewardType = com.example.ads.RewardedFeatureType.GAME_SECOND_CHANCE,
                      itemId = "${game.id}_continue",
                      onRewardEarned = {
                        // Second chance retry: restore round to retry missed question
                        gameRoundIndex = (game.rounds.size - 1).coerceAtLeast(0)
                        gameSelectedOptionId = null
                        currentScreen = GameZoneScreen.GamePlay(game)
                      },
                      onFailedOrCancelled = { err ->
                        if (err != null) bonusRewardMessage = err
                      }
                    )
                  },
                  border = BorderStroke(1.dp, GamesAmber),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_ad_to_continue_button")
                ) {
                  Icon(Icons.Default.PlayCircle, contentDescription = null, tint = GamesAmber, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Watch an ad to continue", color = GamesAmber, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
              }
              if (!doubleXpClaimed) {
                OutlinedButton(
                  onClick = {
                    com.example.ads.AdService.showRewardedAdForGameBonus(
                      userProfile = userProfile,
                      rewardType = com.example.ads.RewardedFeatureType.GAME_DOUBLE_XP,
                      itemId = "${game.id}_double_xp",
                      onRewardEarned = {
                        doubleXpClaimed = true
                        val bonusXp = game.xpReward.coerceAtLeast(15)
                        bonusRewardMessage = "Double XP unlocked! +$bonusXp Bonus XP awarded"
                        onCompleteGame("${game.id}_double_xp", "${game.title} (Double XP)", pct, bonusXp, null)
                      },
                      onFailedOrCancelled = { err ->
                        if (err != null) bonusRewardMessage = err
                      }
                    )
                  },
                  border = BorderStroke(1.dp, GamesCyan),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_ad_double_xp_button")
                ) {
                  Icon(Icons.Default.Bolt, contentDescription = null, tint = GamesCyan, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Watch an ad for double XP", color = GamesCyan, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = { currentScreen = GameZoneScreen.Home },
                modifier = Modifier.weight(1f)
              ) {
                Text("Back to Games", color = Color.White)
              }
              Button(
                onClick = { launchLegacyGameDirect(game) },
                colors = ButtonDefaults.buttonColors(containerColor = GamesCyan),
                modifier = Modifier.weight(1f)
              ) {
                Text("Replay", color = GamesDeepNavy, fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }

      GameZoneScreen.BattleArena -> {
        if (!battleActive) {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
                border = BorderStroke(1.dp, GamesAmber.copy(alpha = 0.4f))
              ) {
                Column(modifier = Modifier.padding(20.dp)) {
                  Text(
                    text = "Vocabulary Battle Arena",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "6 timed rounds against a community challenger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  Button(
                    onClick = {
                      val partner = communityProfiles.firstOrNull { it.userId != userProfile.userId }
                      battleOpponentName = partner?.displayName?.ifBlank { "Mateo S." } ?: "Mateo S."
                      battleOpponentFlag = lang.flagEmoji
                      battleRoundIndex = 0
                      myBattleScore = 0
                      opponentBattleScore = 0
                      battleSelectedOptionId = null
                      battleFinished = false
                      battleActive = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GamesCyan),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Start Match", color = GamesDeepNavy, fontWeight = FontWeight.ExtraBold)
                  }
                }
              }
            }
          }
        } else if (battleFinished) {
          val won = myBattleScore >= opponentBattleScore
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GamesCardNavy)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(if (won) "🏆" else "⚔️", fontSize = 46.sp)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (won) "Victory!" else "Good Match!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "You: $myBattleScore pts • $battleOpponentName: $opponentBattleScore pts",
                style = MaterialTheme.typography.bodyMedium,
                color = GamesCyan
              )
              Spacer(modifier = Modifier.height(18.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { battleActive = false; currentScreen = GameZoneScreen.Home }) {
                  Text("Done", color = Color.White)
                }
                Button(
                  onClick = {
                    battleRoundIndex = 0
                    myBattleScore = 0
                    opponentBattleScore = 0
                    battleSelectedOptionId = null
                    battleFinished = false
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GamesCyan)
                ) {
                  Text("Rematch", color = GamesDeepNavy, fontWeight = FontWeight.ExtraBold)
                }
              }
            }
          }
        } else if (battleRounds.isNotEmpty()) {
          val round = battleRounds[battleRoundIndex % battleRounds.size]
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GamesCardNavy)
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("You", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                  Text("$myBattleScore pts", fontWeight = FontWeight.ExtraBold, color = GamesCyan)
                }
                Text(
                  text = "⏱ ${battleTimeLeft}s • ${battleRoundIndex + 1}/6",
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Column(horizontalAlignment = Alignment.End) {
                  Text("$battleOpponentFlag $battleOpponentName", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                  Text("$opponentBattleScore pts", fontWeight = FontWeight.ExtraBold, color = GamesAmber)
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
              Text(round.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
              Spacer(modifier = Modifier.height(12.dp))

              val battleOptions = remember(battleRoundIndex, round.options) {
                round.options.mapIndexed { idx, txt ->
                  ValidatedAnswerOption(id = "battle_${battleRoundIndex}_$idx", text = txt)
                }
              }
              val battleCorrectOptionId = battleOptions
                .getOrNull(round.correctIndex.coerceIn(0, (battleOptions.size - 1).coerceAtLeast(0)))
                ?.id ?: ""

              LingoPandaHorizontalAnswerGrid(
                options = battleOptions,
                correctOptionId = battleCorrectOptionId,
                selectedOptionId = battleSelectedOptionId,
                tagPrefix = "battle_option",
                onSelectOption = { opt ->
                  battleSelectedOptionId = opt.id
                  if (opt.id == battleCorrectOptionId) {
                    myBattleScore += 100 + (battleTimeLeft * 5)
                    opponentBattleScore += 85
                    if (round.audioPhrase.isNotBlank()) {
                      speechManager.speak(round.audioPhrase, userProfile.targetLanguageCode)
                    }
                  } else {
                    opponentBattleScore += 110
                  }
                }
              )

              if (battleSelectedOptionId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = {
                    if (battleRoundIndex + 1 < 6) {
                      battleRoundIndex++
                      battleSelectedOptionId = null
                    } else {
                      val won = myBattleScore >= opponentBattleScore
                      onCompleteGame(
                        "vocab_battle_${userProfile.targetLanguageCode}_${System.currentTimeMillis() / 60000}",
                        "Battle Arena vs $battleOpponentName",
                        if (won) 100 else 75,
                        if (won) 90 else 45,
                        if (won) "vocab_battle_champion" else null
                      )
                      battleFinished = true
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GamesCyan),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    if (battleRoundIndex + 1 < 6) "Next Round" else "View Score",
                    color = GamesDeepNavy,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
            }
          }
        }
      }

      GameZoneScreen.AiQuiz -> {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = GamesCardNavy)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Personalized Practice Quiz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              TextButton(
                onClick = {
                  scope.launch {
                    isGeneratingQuiz = true
                    quizCompleted = false
                    quizIndex = 0
                    quizScore = 0
                    try {
                      quizQuestions = GeminiTutorService.generateDynamicQuiz(
                        targetLanguage = lang,
                        nativeLanguage = Language.findByCode(userProfile.nativeLanguageCode),
                        proficiencyLevel = userProfile.proficiencyLevel,
                        topic = selectedQuizTopic
                      )
                    } catch (_: Exception) {}
                    isGeneratingQuiz = false
                  }
                }
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = GamesCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Set", color = GamesCyan)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isGeneratingQuiz) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(180.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  CircularProgressIndicator(color = GamesCyan)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("Preparing practice questions...", color = Color.White.copy(alpha = 0.8f))
                }
              }
            } else if (quizCompleted) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("🎉", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Quiz Complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Score: $quizScore / ${quizQuestions.size}", style = MaterialTheme.typography.bodyLarge, color = GamesCyan)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                  onClick = {
                    quizCompleted = false
                    quizIndex = 0
                    quizScore = 0
                    quizSelectedOptionId = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GamesCyan)
                ) {
                  Text("Practice Again", color = GamesDeepNavy, fontWeight = FontWeight.ExtraBold)
                }
              }
            } else if (quizQuestions.isNotEmpty()) {
              val q = quizQuestions[quizIndex]
              val adaptiveOptions = remember(quizIndex, q.options) {
                q.options.mapIndexed { idx, txt ->
                  ValidatedAnswerOption(id = "aiq_${quizIndex}_$idx", text = txt)
                }
              }
              val adaptiveCorrectId = adaptiveOptions
                .getOrNull(q.correctIndex.coerceIn(0, (adaptiveOptions.size - 1).coerceAtLeast(0)))
                ?.id ?: ""

              Text(
                text = "Question ${quizIndex + 1} of ${quizQuestions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = GamesCyan
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = q.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(12.dp))

              LingoPandaHorizontalAnswerGrid(
                options = adaptiveOptions,
                correctOptionId = adaptiveCorrectId,
                selectedOptionId = quizSelectedOptionId,
                tagPrefix = "adaptive_quiz_option",
                onSelectOption = { opt ->
                  quizSelectedOptionId = opt.id
                  if (opt.id == adaptiveCorrectId) {
                    quizScore++
                  }
                }
              )

              if (quizSelectedOptionId != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = GamesSubtleNavy
                ) {
                  Text(
                    text = q.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(12.dp)
                  )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = {
                    if (quizIndex + 1 < quizQuestions.size) {
                      quizIndex++
                      quizSelectedOptionId = null
                    } else {
                      quizCompleted = true
                      onCompleteGame(
                        "ai_quiz_${System.currentTimeMillis()}",
                        "Practice Quiz",
                        ((quizScore.toFloat() / quizQuestions.size.coerceAtLeast(1)) * 100).toInt(),
                        40,
                        "ai_quiz_expert"
                      )
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GamesCyan),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    if (quizIndex + 1 < quizQuestions.size) "Next Question" else "Finish Quiz",
                    color = GamesDeepNavy,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
            } else {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(140.dp),
                contentAlignment = Alignment.Center
              ) {
                Button(
                  onClick = {
                    scope.launch {
                      isGeneratingQuiz = true
                      try {
                        quizQuestions = GeminiTutorService.generateDynamicQuiz(
                          targetLanguage = lang,
                          nativeLanguage = Language.findByCode(userProfile.nativeLanguageCode),
                          proficiencyLevel = userProfile.proficiencyLevel,
                          topic = selectedQuizTopic
                        )
                      } catch (_: Exception) {}
                      isGeneratingQuiz = false
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GamesCyan)
                ) {
                  Text("Start Practice Quiz", color = GamesDeepNavy, fontWeight = FontWeight.ExtraBold)
                }
              }
            }
          }
        }
      }

      GameZoneScreen.AllGamesList -> {
        var searchQuery by remember { mutableStateOf("") }
        val searchedUltimate = remember(searchQuery) {
          DynamicAiGameEngine.ALL_70_GAMES.filter { g ->
            searchQuery.isBlank() ||
              g.title.contains(searchQuery, ignoreCase = true) ||
              g.oneLineDescription.contains(searchQuery, ignoreCase = true) ||
              g.category.label.contains(searchQuery, ignoreCase = true)
          }
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 20.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search games by name or skill...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GamesCyan) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GamesCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.18f)
              ),
              modifier = Modifier.fillMaxWidth()
            )
          }

          items((searchedUltimate.indices step 2).toList(), key = { "search_row_$it" }) { i ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              val g1 = searchedUltimate[i]
              val prog1 = remember(g1.id, userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
                DynamicAiGameEngine.getGameProgress(context, userProfile.targetLanguageCode, g1.id, activeUserId)
              }
              UltimateLanguageGameCompactCard(
                modifier = Modifier.weight(1f),
                game = g1,
                currentLevel = prog1.highestUnlockedLevel,
                totalStars = prog1.totalStarsEarned,
                onClick = { launchUltimateGame(g1, GameZoneScreen.Home) }
              )
              if (i + 1 < searchedUltimate.size) {
                val g2 = searchedUltimate[i + 1]
                val prog2 = remember(g2.id, userProfile.targetLanguageCode, activeUserId, progressRefreshTick) {
                  DynamicAiGameEngine.getGameProgress(context, userProfile.targetLanguageCode, g2.id, activeUserId)
                }
                UltimateLanguageGameCompactCard(
                  modifier = Modifier.weight(1f),
                  game = g2,
                  currentLevel = prog2.highestUnlockedLevel,
                  totalStars = prog2.totalStarsEarned,
                  onClick = { launchUltimateGame(g2, GameZoneScreen.Home) }
                )
              } else {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }

      else -> {}
    }
  }
}

@Composable
private fun CompactDailyChallengeHubCard(
  missionsState: DailyGameMissionsState,
  isDailyChallengeDone: Boolean,
  onOpenDailyChallenge: () -> Unit,
  onStartMissionAction: (Int) -> Unit,
  onClaimReward: (Int) -> Unit
) {
  var showMissions by rememberSaveable { mutableStateOf(false) }
  val completedCount = listOf(
    missionsState.vocabGamesCompleted >= 2,
    missionsState.speakingMinutesCompleted >= 3,
    missionsState.listeningChallengesCompleted >= 1,
    missionsState.newWordsLearned >= 5
  ).count { it }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("daily_challenge_main_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
    border = BorderStroke(1.dp, GamesAmber.copy(alpha = 0.42f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = GamesAmber.copy(alpha = 0.18f),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("⚡", fontSize = 20.sp)
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Daily Challenge",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
            Text(
              text = if (isDailyChallengeDone) {
                "Today's challenge completed • $completedCount/4 tasks"
              } else {
                "5-minute mixed language session • $completedCount/4 tasks"
              },
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.72f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { showMissions = !showMissions },
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = if (showMissions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Missions",
              tint = Color.White.copy(alpha = 0.75f)
            )
          }
          Button(
            onClick = onOpenDailyChallenge,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDailyChallengeDone) GamesGreen.copy(alpha = 0.25f) else GamesAmber
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("daily_challenge_start_button")
          ) {
            Text(
              text = if (isDailyChallengeDone) "Done ✓" else "Start",
              color = if (isDailyChallengeDone) GamesGreen else GamesDeepNavy,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp
            )
          }
        }
      }

      AnimatedVisibility(visible = showMissions) {
        Column(
          modifier = Modifier
            .padding(top = 12.dp)
            .testTag("todays_missions_card"),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MissionItemRow(
            title = "Complete 2 vocabulary games",
            progressText = "${missionsState.vocabGamesCompleted.coerceAtMost(2)}/2",
            isDone = missionsState.vocabGamesCompleted >= 2,
            isClaimed = missionsState.claimedMissionIds.contains("mission_1"),
            xpReward = 25,
            onGo = { onStartMissionAction(1) },
            onClaim = { onClaimReward(1) }
          )
          MissionItemRow(
            title = "Practice speaking for 3 minutes",
            progressText = "${missionsState.speakingMinutesCompleted.coerceAtMost(3)}/3m",
            isDone = missionsState.speakingMinutesCompleted >= 3,
            isClaimed = missionsState.claimedMissionIds.contains("mission_2"),
            xpReward = 30,
            onGo = { onStartMissionAction(2) },
            onClaim = { onClaimReward(2) }
          )
          MissionItemRow(
            title = "Finish 1 listening challenge",
            progressText = "${missionsState.listeningChallengesCompleted.coerceAtMost(1)}/1",
            isDone = missionsState.listeningChallengesCompleted >= 1,
            isClaimed = missionsState.claimedMissionIds.contains("mission_3"),
            xpReward = 25,
            onGo = { onStartMissionAction(3) },
            onClaim = { onClaimReward(3) }
          )
          MissionItemRow(
            title = "Learn 5 new words",
            progressText = "${missionsState.newWordsLearned.coerceAtMost(5)}/5",
            isDone = missionsState.newWordsLearned >= 5,
            isClaimed = missionsState.claimedMissionIds.contains("mission_4"),
            xpReward = 20,
            onGo = { onStartMissionAction(4) },
            onClaim = { onClaimReward(4) }
          )
        }
      }
    }
  }
}

@Composable
private fun TodaysMissionsDetailCard(
  missionsState: DailyGameMissionsState,
  onStartMissionAction: (Int) -> Unit,
  onClaimReward: (Int) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "Today's Missions",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
      )
      MissionItemRow(
        title = "Complete 2 vocabulary games",
        progressText = "${missionsState.vocabGamesCompleted.coerceAtMost(2)}/2",
        isDone = missionsState.vocabGamesCompleted >= 2,
        isClaimed = missionsState.claimedMissionIds.contains("mission_1"),
        xpReward = 25,
        onGo = { onStartMissionAction(1) },
        onClaim = { onClaimReward(1) }
      )
      MissionItemRow(
        title = "Practice speaking for 3 minutes",
        progressText = "${missionsState.speakingMinutesCompleted.coerceAtMost(3)}/3m",
        isDone = missionsState.speakingMinutesCompleted >= 3,
        isClaimed = missionsState.claimedMissionIds.contains("mission_2"),
        xpReward = 30,
        onGo = { onStartMissionAction(2) },
        onClaim = { onClaimReward(2) }
      )
      MissionItemRow(
        title = "Finish 1 listening challenge",
        progressText = "${missionsState.listeningChallengesCompleted.coerceAtMost(1)}/1",
        isDone = missionsState.listeningChallengesCompleted >= 1,
        isClaimed = missionsState.claimedMissionIds.contains("mission_3"),
        xpReward = 25,
        onGo = { onStartMissionAction(3) },
        onClaim = { onClaimReward(3) }
      )
      MissionItemRow(
        title = "Learn 5 new words",
        progressText = "${missionsState.newWordsLearned.coerceAtMost(5)}/5",
        isDone = missionsState.newWordsLearned >= 5,
        isClaimed = missionsState.claimedMissionIds.contains("mission_4"),
        xpReward = 20,
        onGo = { onStartMissionAction(4) },
        onClaim = { onClaimReward(4) }
      )
    }
  }
}

@Composable
private fun MissionItemRow(
  title: String,
  progressText: String,
  isDone: Boolean,
  isClaimed: Boolean,
  xpReward: Int,
  onGo: () -> Unit,
  onClaim: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = GamesSubtleNavy,
    border = BorderStroke(
      1.dp,
      if (isDone) GamesGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
          contentDescription = null,
          tint = if (isDone) GamesGreen else GamesCyan,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "$progressText • +$xpReward XP",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 10.sp
          )
        }
      }
      when {
        isClaimed -> {
          Text(
            text = "Claimed ✓",
            color = GamesGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
        isDone -> {
          Button(
            onClick = onClaim,
            colors = ButtonDefaults.buttonColors(containerColor = GamesAmber),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.heightIn(min = 36.dp)
          ) {
            Text("Claim", color = GamesDeepNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
          }
        }
        else -> {
          TextButton(
            onClick = onGo,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.heightIn(min = 36.dp)
          ) {
            Text("Play", color = GamesCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun ExploreCategoryGridCard(
  modifier: Modifier = Modifier,
  spec: ExploreCategoryCardSpec,
  onClick: () -> Unit
) {
  val accent = Color(spec.accentHex)
  Card(
    modifier = modifier
      .heightIn(min = 104.dp)
      .clickable(onClick = onClick)
      .testTag("category_card_${spec.key}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
    border = BorderStroke(1.dp, GamesBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = accent.copy(alpha = 0.18f),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(spec.emoji, fontSize = 18.sp)
          }
        }
        Icon(
          Icons.Default.ChevronRight,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.45f),
          modifier = Modifier.size(18.dp)
        )
      }

      Column {
        Text(
          text = spec.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = spec.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.68f),
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

private fun resolveGameVectorIcon(game: UltimateLanguageGame): androidx.compose.ui.graphics.vector.ImageVector {
  return when (game.id) {
    "ug_24_pronunciation_battle" -> Icons.Default.Mic
    "ug_25_repeat_after_me" -> Icons.Default.RecordVoiceOver
    "ug_26_tongue_twister" -> Icons.Default.GraphicEq
    "ug_27_speak_the_picture" -> Icons.Default.PhotoCamera
    "ug_28_quick_answer" -> Icons.Default.Bolt
    "ug_29_speech_coach_challenge" -> Icons.Default.Psychology
    "ug_30_fluency_sprint" -> Icons.Default.Timer
    "ug_31_shadow_speaking" -> Icons.Default.SurroundSound
    "ug_32_dialogue_completion" -> Icons.Default.Forum
    "ug_33_roleplay_mission" -> Icons.Default.TheaterComedy
    else -> when (game.category) {
      UltimateGameCategory.SPEAKING -> Icons.Default.Mic
      UltimateGameCategory.VOCABULARY -> Icons.Default.Style
      UltimateGameCategory.SPELLING -> Icons.Default.Spellcheck
      UltimateGameCategory.LISTENING -> Icons.Default.Headphones
      UltimateGameCategory.GRAMMAR -> Icons.Default.Extension
      UltimateGameCategory.CONVERSATION -> Icons.Default.Forum
      UltimateGameCategory.BRAIN_CHALLENGE -> Icons.Default.Psychology
    }
  }
}

@Composable
private fun SmartRecommendationGameChipCard(
  game: UltimateLanguageGame,
  recommendationReason: String,
  currentLevel: Int,
  totalStars: Int,
  onClick: () -> Unit
) {
  val accent = Color(game.accentHex)
  val iconVector = remember(game.id) { resolveGameVectorIcon(game) }
  Card(
    modifier = Modifier
      .width(210.dp)
      .clickable(onClick = onClick)
      .testTag("smart_rec_card_${game.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(9.dp),
          color = accent.copy(alpha = 0.18f),
          modifier = Modifier.size(30.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = iconVector,
              contentDescription = null,
              tint = accent,
              modifier = Modifier.size(17.dp)
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Lv. $currentLevel",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
          if (totalStars > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "★ $totalStars",
              color = GamesAmber,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = game.title,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = recommendationReason,
        color = Color.White.copy(alpha = 0.68f),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun UltimateLanguageGameCompactCard(
  modifier: Modifier = Modifier,
  game: UltimateLanguageGame,
  currentLevel: Int,
  totalStars: Int,
  onClick: () -> Unit
) {
  val accent = Color(game.accentHex)
  val iconVector = remember(game.id) { resolveGameVectorIcon(game) }
  val milestoneProgress = ((currentLevel - 1).coerceAtLeast(0) % 25 + 1) / 25f
  Card(
    modifier = modifier
      .heightIn(min = 118.dp)
      .clickable(onClick = onClick)
      .testTag("ultimate_game_card_${game.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(9.dp),
          color = accent.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
          modifier = Modifier.size(32.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = iconVector,
              contentDescription = game.title,
              tint = accent,
              modifier = Modifier.size(17.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = accent.copy(alpha = 0.14f)
          ) {
            Text(
              text = "Lv. $currentLevel",
              color = accent,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
          }
          if (totalStars > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "★$totalStars",
              color = GamesAmber,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = game.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          fontSize = 13.sp,
          lineHeight = 16.sp,
          maxLines = 2
        )
        Text(
          text = game.oneLineDescription,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.72f),
          fontSize = 11.sp,
          lineHeight = 14.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      LinearProgressIndicator(
        progress = { milestoneProgress.coerceIn(0.08f, 1f) },
        color = accent,
        trackColor = Color.White.copy(alpha = 0.08f),
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(999.dp))
      )
    }
  }
}

@Composable
private fun PracticeChallengeFeatureRowCard(
  title: String,
  subtitle: String,
  badgeText: String,
  accentColor: Color,
  iconType: String,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = GamesCardNavy),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = accentColor.copy(alpha = 0.18f)
          ) {
            Text(
              text = badgeText,
              color = accentColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.72f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.16f),
        modifier = Modifier.size(44.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          when (iconType) {
            "SPEECH_COACH" -> Icon(Icons.Default.Mic, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            "CUSTOM_LESSON" -> Icon(Icons.Default.EditNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            "BOOKS_LIBRARY" -> Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            "VOCAB_CATEGORIES" -> Icon(Icons.Default.Category, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            else -> Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
          }
        }
      }
    }
  }
}
