package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.AiErrorHandler
import com.example.ai.GeminiTutorService
import com.example.ai.UpgradeSheetMode
import com.example.ai.VocabularyBuilderWordItem
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.SrsCardMetadata
import com.example.data.model.SrsRating
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.EmojiVoice
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.rememberLiveEmojiVoiceState
import com.example.ui.theme.LingoThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val VocabNavyTop = Color(0xFF060D1D)
private val VocabNavyBottom = Color(0xFF0D162B)
private val VocabElectricBlue = Color(0xFF3B82F6)
private val VocabCyan = Color(0xFF38BDF8)
private val VocabEmerald = Color(0xFF10B981)
private val VocabAmber = Color(0xFFF59E0B)
private val VocabRose = Color(0xFFF43F5E)
private val VocabGold = Color(0xFFFBBF24)

private data class VocabQuizQuestion(
  val word: String,
  val correctMeaning: String,
  val options: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyBuilderScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  flashcards: List<FlashcardProgress>,
  srsMetadataMap: Map<String, SrsCardMetadata> = emptyMap(),
  onReviewCardWithRating: (FlashcardProgress, SrsRating) -> Unit = { _, _ -> },
  onSaveWordToFlashcards: (front: String, back: String, phonetic: String) -> Unit = { _, _, _ -> },
  onDeleteFlashcard: (String) -> Unit = {},
  onOpenPronunciationForWord: (String) -> Unit = {},
  onAddXp: (Int) -> Unit = {},
  onBack: () -> Unit,
  onOpenSubscription: () -> Unit = {}
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isDark = LingoThemeController.isDarkActive
  val targetLang = remember(userProfile.targetLanguageCode) { Language.findByCode(userProfile.targetLanguageCode) }
  val nativeLang = remember(userProfile.nativeLanguageCode) { Language.findByCode(userProfile.nativeLanguageCode) }

  // 0: Learn, 1: Review, 2: Saved
  var activeTab by remember { mutableIntStateOf(0) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  // Vocabulary items state
  var wordsList by remember { mutableStateOf<List<VocabularyBuilderWordItem>>(emptyList()) }
  var currentWordIndex by remember { mutableIntStateOf(0) }
  var isLoadingWords by remember { mutableStateOf(false) }
  var vocabErrorMessage by remember { mutableStateOf<String?>(null) }
  var showQuizModal by remember { mutableStateOf(false) }
  var showPracticeMicModal by remember { mutableStateOf(false) }

  // Saved words bookmark cache
  val savedWordSet = remember(flashcards) {
    flashcards.map { it.frontText.trim().lowercase() }.toSet()
  }

  val knownWords = remember(flashcards) {
    flashcards.map { it.frontText.trim() }.filter { it.isNotBlank() }
  }

  val vocabTopics = remember {
    listOf(
      "Everyday Life & Conversational Fluency",
      "Travel, Airports & Navigation",
      "Café Culture, Food & Dining",
      "Professional Work & Networking",
      "Emotions, Personality & Friendships",
      "Shopping, Fashion & Modern Culture"
    )
  }
  var vocabTopicIndex by remember { mutableIntStateOf(0) }

  val isVerifiedPro = userProfile.isEntitlementVerified

  fun fetchDynamicVocabulary(advanceTopic: Boolean = false, userInitiated: Boolean = false) {
    if (!isVerifiedPro) {
      if (wordsList.isEmpty()) {
        wordsList = GeminiTutorService.getFallbackVocabularyBuilderWords(targetLang, nativeLang)
      }
      if (userInitiated) {
        AiErrorHandler.requestUpgrade(
          mode = UpgradeSheetMode.AI_UPGRADE,
          featureKey = "vocabulary_ai",
          headline = "Unlock AI Vocabulary Decks",
          reason = "Review and Saved flashcards are always Free. Upgrade to LingoSphere Pro to generate fresh custom AI vocabulary decks anytime."
        )
      }
      return
    }
    if (isLoadingWords) return
    isLoadingWords = true
    vocabErrorMessage = null
    if (advanceTopic) {
      vocabTopicIndex = (vocabTopicIndex + 1) % vocabTopics.size
    }
    val currentTopic = vocabTopics[vocabTopicIndex % vocabTopics.size]
    scope.launch {
      try {
        val aiWords = GeminiTutorService.generateVocabularyBuilderWords(
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          cefrLevel = userProfile.proficiencyLevel.ifBlank { "B1" },
          topic = currentTopic,
          excludeWords = knownWords
        )
        if (aiWords.isNotEmpty()) {
          wordsList = aiWords
          currentWordIndex = 0
        }
      } catch (e: Exception) {
        vocabErrorMessage = AiErrorHandler.handle(e, contextTag = "VocabularyBuilder").userMessage
        if (wordsList.isEmpty()) {
          wordsList = GeminiTutorService.getFallbackVocabularyBuilderWords(targetLang, nativeLang)
        }
      } finally {
        isLoadingWords = false
      }
    }
  }

  // Load initial words
  LaunchedEffect(targetLang.code, nativeLang.code, userProfile.proficiencyLevel, isVerifiedPro) {
    if (isVerifiedPro) {
      fetchDynamicVocabulary(advanceTopic = false, userInitiated = false)
    } else if (wordsList.isEmpty()) {
      wordsList = GeminiTutorService.getFallbackVocabularyBuilderWords(targetLang, nativeLang)
    }
  }

  val activeWord = wordsList.getOrNull(currentWordIndex)
    ?: GeminiTutorService.getFallbackVocabularyBuilderWords(targetLang, nativeLang).first()

  // Daily goal calculation
  val learnedTodayCount = (currentWordIndex + 1).coerceAtMost(wordsList.size.coerceAtLeast(5))
  val targetDailyCount = 5

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(VocabNavyTop)
      .windowInsetsPadding(WindowInsets.statusBars)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      // Top Navigation Bar
      VocabTopHeader(
        targetLanguage = targetLang,
        speechManager = speechManager,
        isLoadingWords = isLoadingWords,
        isVerifiedPro = isVerifiedPro,
        onOpenSubscription = onOpenSubscription,
        onBack = onBack,
        onSettingsClick = { showSettingsDialog = true }
      )

      // Hero Section: Stacked 3D flashcard illustration + Quote + Dual Stats Cards
      VocabHeroHeader(
        dailyLearned = learnedTodayCount,
        dailyTarget = targetDailyCount,
        streakDays = userProfile.currentStreak.toInt().coerceAtLeast(1)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // White / Elevated Curved Content Surface
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = if (isDark) Color(0xFF060D1D) else Color.White,
        shadowElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // 3 Mode Tabs: Learn, Review, Saved
          VocabTabSelector(
            activeTab = activeTab,
            onSelectTab = { activeTab = it }
          )

          Spacer(modifier = Modifier.height(16.dp))

          if (vocabErrorMessage != null) {
            LingoCalmAiErrorBanner(
              message = vocabErrorMessage,
              onRetry = { fetchDynamicVocabulary(advanceTopic = false, userInitiated = true) },
              onDismiss = { vocabErrorMessage = null }
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          when (activeTab) {
            0 -> {
              // ---------------- LEARN TAB ----------------
              VocabLearnView(
                word = activeWord,
                wordIndex = currentWordIndex,
                totalWords = wordsList.size.coerceAtLeast(5),
                isSaved = savedWordSet.contains(activeWord.word.trim().lowercase()),
                isVerifiedPro = isVerifiedPro,
                targetLangCode = targetLang.code,
                speechManager = speechManager,
                isLoading = isLoadingWords,
                onToggleSave = {
                  val isCurrentlySaved = savedWordSet.contains(activeWord.word.trim().lowercase())
                  if (isCurrentlySaved) {
                    val matching = flashcards.firstOrNull { it.frontText.equals(activeWord.word, ignoreCase = true) }
                    if (matching != null) {
                      onDeleteFlashcard(matching.id)
                      Toast.makeText(context, "Removed from Saved", Toast.LENGTH_SHORT).show()
                    }
                  } else {
                    onSaveWordToFlashcards(
                      activeWord.word,
                      activeWord.meaning,
                      activeWord.phonetic
                    )
                    Toast.makeText(context, "Saved to Vocabulary Decks!", Toast.LENGTH_SHORT).show()
                  }
                },
                onPreviousWord = {
                  if (currentWordIndex > 0) {
                    currentWordIndex--
                  }
                },
                onNextWord = {
                  if (currentWordIndex < wordsList.size - 1) {
                    currentWordIndex++
                  } else {
                    showQuizModal = true
                  }
                },
                onSelectWordIndex = { idx ->
                  if (idx in wordsList.indices) {
                    currentWordIndex = idx
                  }
                },
                onPracticeWord = {
                  showPracticeMicModal = true
                },
                onStartQuiz = {
                  showQuizModal = true
                },
                onGenerateFresh = {
                  if (!isVerifiedPro) {
                    AiErrorHandler.requestUpgrade(
                      mode = UpgradeSheetMode.AI_UPGRADE,
                      featureKey = "vocabulary_ai",
                      headline = "Unlock AI Vocabulary Decks",
                      reason = "Upgrade to LingoSphere Pro to generate fresh personalized vocabulary decks on demand."
                    )
                    return@VocabLearnView
                  }
                  scope.launch {
                    isLoadingWords = true
                    vocabErrorMessage = null
                    try {
                      val newWords = GeminiTutorService.generateVocabularyBuilderWords(
                        targetLanguage = targetLang,
                        nativeLanguage = nativeLang,
                        cefrLevel = userProfile.proficiencyLevel.ifBlank { "B1" },
                        topic = "Advanced Conversational Nuances & Idioms",
                        excludeWords = wordsList.map { it.word }
                      )
                      if (newWords.isNotEmpty()) {
                        wordsList = newWords
                        currentWordIndex = 0
                        Toast.makeText(context, "Generated 5 fresh vocabulary words!", Toast.LENGTH_SHORT).show()
                      }
                    } catch (e: Exception) {
                      vocabErrorMessage = AiErrorHandler.handle(e, contextTag = "VocabularyRefresh").userMessage
                    } finally {
                      isLoadingWords = false
                    }
                  }
                }
              )
              Spacer(modifier = Modifier.height(22.dp))
              VocabCategoriesExplorerSection(
                targetLangCode = targetLang.code,
                speechManager = speechManager,
                onSaveWordToFlashcards = { front, back, phonetic ->
                  onSaveWordToFlashcards(front, back, phonetic)
                  Toast.makeText(context, "Saved '$front' to Vocabulary Deck!", Toast.LENGTH_SHORT).show()
                },
                onAddXp = onAddXp
              )
            }
            1 -> {
              // ---------------- REVIEW TAB (Spaced Repetition) ----------------
              VocabReviewSrsView(
                flashcards = flashcards,
                srsMetadataMap = srsMetadataMap,
                targetLangCode = targetLang.code,
                speechManager = speechManager,
                onReviewCard = onReviewCardWithRating,
                onSwitchToLearn = { activeTab = 0 }
              )
            }
            2 -> {
              // ---------------- SAVED TAB ----------------
              VocabSavedView(
                flashcards = flashcards,
                targetLangCode = targetLang.code,
                speechManager = speechManager,
                onDelete = onDeleteFlashcard,
                onPractice = { wordText ->
                  onOpenPronunciationForWord(wordText)
                }
              )
            }
            3 -> {
              // ---------------- CATEGORIES TAB ----------------
              VocabCategoriesExplorerSection(
                targetLangCode = targetLang.code,
                speechManager = speechManager,
                onSaveWordToFlashcards = { front, back, phonetic ->
                  onSaveWordToFlashcards(front, back, phonetic)
                  Toast.makeText(context, "Saved '$front' to Vocabulary Deck!", Toast.LENGTH_SHORT).show()
                },
                onAddXp = onAddXp
              )
            }
          }

          Spacer(modifier = Modifier.height(28.dp))
        }
      }
    }

    // Modal: Quick Quiz after 5 words
    if (showQuizModal && wordsList.isNotEmpty()) {
      VocabQuickQuizSheet(
        words = wordsList.take(5),
        targetLanguageName = targetLang.name,
        onDismiss = { showQuizModal = false },
        onComplete = { earnedXp, missedWords ->
          showQuizModal = false
          onAddXp(earnedXp)
          missedWords.forEach { missed ->
            onSaveWordToFlashcards(missed.word, missed.meaning, missed.phonetic)
          }
          val reviewMsg = if (missedWords.isNotEmpty()) " (${missedWords.size} words scheduled for review)" else ""
          Toast.makeText(context, "Quiz Complete! +$earnedXp XP earned!$reviewMsg", Toast.LENGTH_LONG).show()
          fetchDynamicVocabulary(advanceTopic = true)
        }
      )
    }

    // Modal: Practice This Word Microphone Modal
    if (showPracticeMicModal) {
      VocabWordPracticeMicSheet(
        word = activeWord,
        targetLangCode = targetLang.code,
        speechManager = speechManager,
        onDismiss = { showPracticeMicModal = false },
        onOpenFullCoach = {
          showPracticeMicModal = false
          onOpenPronunciationForWord(activeWord.word)
        }
      )
    }

    // Settings / Options Dialog
    if (showSettingsDialog) {
      VocabSettingsDialog(
        currentCefr = userProfile.proficiencyLevel.ifBlank { "B1" },
        onDismiss = { showSettingsDialog = false }
      )
    }
  }
}

// ==============================================================================
// TOP HEADER
// ==============================================================================
@Composable
private fun VocabTopHeader(
  targetLanguage: Language,
  speechManager: SpeechManager,
  isLoadingWords: Boolean = false,
  isVerifiedPro: Boolean,
  onOpenSubscription: () -> Unit,
  onBack: () -> Unit,
  onSettingsClick: () -> Unit
) {
  val (vocabEmojiState, vocabAudioLevel) = rememberLiveEmojiVoiceState(
    speechManager = speechManager,
    isThinking = isLoadingWords
  )
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .size(44.dp)
        .testTag("vocabulary_back_button")
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = Color.White
      )
    }

    Text(
      text = "Vocabulary Builder",
      color = Color.White,
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 8.dp)
        .testTag("vocabulary_screen_title")
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      EmojiVoice(
        state = vocabEmojiState,
        size = 30.dp,
        audioLevel = vocabAudioLevel,
        showStateLabel = false
      )
      if (!isVerifiedPro) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = VocabGold.copy(alpha = 0.14f),
          border = androidx.compose.foundation.BorderStroke(1.dp, VocabGold.copy(alpha = 0.48f)),
          modifier = Modifier
            .clickable { onOpenSubscription() }
            .testTag("pro_subscription_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.WorkspacePremium,
              contentDescription = "Upgrade to Pro",
              tint = VocabGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Upgrade",
              color = VocabGold,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      IconButton(
        onClick = onSettingsClick,
        modifier = Modifier
          .size(40.dp)
          .testTag("vocabulary_settings_button")
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Vocabulary Settings",
          tint = Color.White.copy(alpha = 0.9f)
        )
      }
    }
  }
}

// ==============================================================================
// HERO HEADER (Stacked 3D card illustration + Dual Stats cards)
// ==============================================================================
@Composable
private fun VocabHeroHeader(
  dailyLearned: Int,
  dailyTarget: Int,
  streakDays: Int
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 4.dp)
  ) {
    // Top Row: Stacked 3D flashcard illustration + Motivating Quote
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_vocabulary_header),
          contentDescription = "Vocabulary 3D Illustration",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = "Build your word power daily and use it in real life.",
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 21.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Dual Stats: Daily Goal + Day Streak
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Daily Goal Card
      Card(
        modifier = Modifier.weight(1.3f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D162B))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = VocabCyan.copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = VocabCyan,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Daily Goal",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "$dailyLearned of $dailyTarget words",
              color = Color(0xFF94A3B8),
              fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { (dailyLearned.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
              color = VocabEmerald,
              trackColor = Color(0xFF1E293B)
            )
          }
        }
      }

      // Day Streak Card
      Card(
        modifier = Modifier.weight(0.9f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D162B))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = VocabAmber.copy(alpha = 0.2f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = VocabAmber,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Column {
            Text(
              text = "$streakDays",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "Day Streak",
              color = Color(0xFF94A3B8),
              fontSize = 11.5.sp
            )
          }
        }
      }
    }
  }
}

// ==============================================================================
// 3 TAB SELECTOR: Learn, Review, Saved
// ==============================================================================
@Composable
private fun VocabTabSelector(
  activeTab: Int,
  onSelectTab: (Int) -> Unit
) {
  val isDark = LingoThemeController.isDarkActive
  Surface(
    shape = RoundedCornerShape(50),
    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Tab 0: Learn
      VocabTabPill(
        icon = Icons.Default.Bookmark,
        label = "Learn",
        isSelected = activeTab == 0,
        onClick = { onSelectTab(0) },
        modifier = Modifier
          .weight(1f)
          .testTag("tab_vocab_learn")
      )

      // Tab 1: Review
      VocabTabPill(
        icon = Icons.Default.Refresh,
        label = "Review",
        isSelected = activeTab == 1,
        onClick = { onSelectTab(1) },
        modifier = Modifier
          .weight(1f)
          .testTag("tab_vocab_review")
      )

      // Tab 2: Saved
      VocabTabPill(
        icon = Icons.Default.FavoriteBorder,
        label = "Saved",
        isSelected = activeTab == 2,
        onClick = { onSelectTab(2) },
        modifier = Modifier
          .weight(1f)
          .testTag("tab_vocab_saved")
      )

      // Tab 3: Categories
      VocabTabPill(
        icon = Icons.Default.AutoAwesome,
        label = "Categories",
        isSelected = activeTab == 3,
        onClick = { onSelectTab(3) },
        modifier = Modifier
          .weight(1f)
          .testTag("tab_vocab_categories")
      )
    }
  }
}

@Composable
private fun VocabTabPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = LingoThemeController.isDarkActive
  val bgBrush = if (isSelected) {
    Brush.horizontalGradient(listOf(VocabElectricBlue, Color(0xFF1D4ED8)))
  } else {
    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
  }

  Surface(
    modifier = modifier.clickable { onClick() },
    shape = RoundedCornerShape(50),
    color = Color.Transparent
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(bgBrush)
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
          fontSize = 13.5.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

// ==============================================================================
// TAB 0: LEARN VIEW
// ==============================================================================
@Composable
private fun VocabLearnView(
  word: VocabularyBuilderWordItem,
  wordIndex: Int,
  totalWords: Int,
  isSaved: Boolean,
  isVerifiedPro: Boolean,
  targetLangCode: String,
  speechManager: SpeechManager,
  isLoading: Boolean,
  onToggleSave: () -> Unit,
  onPreviousWord: () -> Unit,
  onNextWord: () -> Unit,
  onSelectWordIndex: (Int) -> Unit,
  onPracticeWord: () -> Unit,
  onStartQuiz: () -> Unit,
  onGenerateFresh: () -> Unit
) {
  val isDark = LingoThemeController.isDarkActive
  val scope = rememberCoroutineScope()
  var isAudioPlaying by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
        shape = RoundedCornerShape(26.dp)
      ),
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp)
    ) {
      // Top row: "Word 1 of 5" chip + Bookmark/Heart Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isDark) Color(0xFF334155) else Color(0xFFEFF6FF)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              tint = VocabElectricBlue,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (wordIndex == 0) "Word of the Day" else "Word ${wordIndex + 1} of $totalWords",
              color = if (isDark) Color(0xFF93C5FD) else VocabElectricBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        IconButton(
          onClick = onToggleSave,
          modifier = Modifier
            .size(36.dp)
            .testTag("vocab_save_word_button")
        ) {
          Icon(
            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isSaved) "Unsave Word" else "Save Word",
            tint = if (isSaved) VocabRose else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Word Title and Audio Pronunciation
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = word.word,
          color = if (isDark) Color.White else Color(0xFF0F172A),
          fontSize = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier.testTag("vocab_word_title")
        )

        IconButton(
          onClick = {
            isAudioPlaying = true
            speechManager.speak(word.word, targetLangCode)
            scope.launch {
              delay(1200L)
              isAudioPlaying = false
            }
          },
          modifier = Modifier
            .size(42.dp)
            .testTag("vocab_speak_audio_button")
        ) {
          Surface(
            shape = CircleShape,
            color = if (isAudioPlaying) VocabElectricBlue.copy(alpha = 0.2f) else (if (isDark) Color(0xFF334155) else Color(0xFFEFF6FF)),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Speak Pronunciation",
                tint = VocabElectricBlue,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Phonetic pronunciation
      if (word.phonetic.isNotBlank()) {
        Text(
          text = word.phonetic,
          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Part of Speech Pill
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFDBEAFE),
        modifier = Modifier.testTag("vocab_part_of_speech_badge")
      ) {
        Text(
          text = word.partOfSpeech.ifBlank { "Vocabulary" },
          color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
          fontSize = 12.5.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Definition / Meaning
      Text(
        text = word.meaning,
        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Example sentence quote card
      if (word.exampleSentence.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isDark) Color(0xFF0F172A) else Color(0xFFF0F7FF),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "❝",
              color = VocabElectricBlue,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 24.sp
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = word.exampleSentence,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp
              )
              if (word.exampleTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = word.exampleTranslation,
                  color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                  fontSize = 12.5.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Pagination Controls: < 5 Dots >
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Prev button
        Surface(
          shape = CircleShape,
          color = if (wordIndex > 0) (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)) else Color.Transparent,
          modifier = Modifier
            .size(40.dp)
            .clickable(enabled = wordIndex > 0) { onPreviousWord() }
            .testTag("vocab_prev_word_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = "Previous Word",
              tint = if (wordIndex > 0) (if (isDark) Color.White else Color(0xFF0F172A)) else Color(0xFFCBD5E1)
            )
          }
        }

        // 5 Dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(totalWords.coerceAtMost(5)) { dotIdx ->
            val isDotActive = dotIdx == wordIndex
            Box(
              modifier = Modifier
                .size(if (isDotActive) 8.dp else 6.dp)
                .clip(CircleShape)
                .background(
                  if (isDotActive) VocabElectricBlue else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                )
                .clickable { onSelectWordIndex(dotIdx) }
            )
          }
        }

        // Next button
        Surface(
          shape = CircleShape,
          color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
          modifier = Modifier
            .size(40.dp)
            .clickable { onNextWord() }
            .testTag("vocab_next_word_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = "Next Word",
              tint = if (isDark) Color.White else Color(0xFF0F172A)
            )
          }
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(18.dp))

  // Primary Action Button: [🎙 Practice This Word]
  Button(
    onClick = onPracticeWord,
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp)
      .testTag("vocab_practice_word_button"),
    shape = RoundedCornerShape(50),
    colors = ButtonDefaults.buttonColors(containerColor = VocabElectricBlue)
  ) {
    Icon(
      imageVector = Icons.Default.Mic,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "Practice This Word",
      color = Color.White,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
  }

  Spacer(modifier = Modifier.height(14.dp))

  // Secondary Action Row: Quick Quiz + Generate 5 Fresh
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OutlinedButton(
      onClick = onStartQuiz,
      modifier = Modifier
        .weight(1f)
        .height(46.dp)
        .testTag("vocab_start_quiz_button"),
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
      )
    ) {
      Icon(
        imageVector = Icons.Default.Quiz,
        contentDescription = null,
        tint = VocabElectricBlue,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Quick Quiz (+35 XP)",
        color = if (isDark) Color(0xFF93C5FD) else VocabElectricBlue,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    OutlinedButton(
      onClick = onGenerateFresh,
      enabled = !isLoading,
      modifier = Modifier
        .weight(1f)
        .height(46.dp)
        .testTag("vocab_generate_new_words_button"),
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
      )
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          color = VocabElectricBlue,
          strokeWidth = 2.dp
        )
      } else if (!isVerifiedPro) {
        LingoGoldLockIcon(size = 15.dp)
      } else {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = VocabAmber,
          modifier = Modifier.size(16.dp)
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (isVerifiedPro) "Fresh Words" else "Unlock Fresh Words",
        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// ==============================================================================
// TAB 1: REVIEW VIEW (Spaced Repetition SM-2 with Flip & 4-Choice Ratings)
// ==============================================================================
@Composable
private fun VocabReviewSrsView(
  flashcards: List<FlashcardProgress>,
  srsMetadataMap: Map<String, SrsCardMetadata>,
  targetLangCode: String,
  speechManager: SpeechManager,
  onReviewCard: (FlashcardProgress, SrsRating) -> Unit,
  onSwitchToLearn: () -> Unit
) {
  val isDark = LingoThemeController.isDarkActive
  var reviewIndex by remember { mutableIntStateOf(0) }
  var isFlipped by remember { mutableStateOf(false) }

  if (flashcards.isEmpty()) {
    // Empty state
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = VocabEmerald,
          modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "All Caught Up!",
          color = if (isDark) Color.White else Color(0xFF0F172A),
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "You've reviewed all your saved cards for today. Save new words from the Learn tab to expand your deck.",
          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
          fontSize = 13.5.sp,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onSwitchToLearn,
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(containerColor = VocabElectricBlue)
        ) {
          Text("Go to Learn Tab", color = Color.White)
        }
      }
    }
    return
  }

  val activeCard = flashcards.getOrNull(reviewIndex) ?: flashcards.first()

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Review Progress Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Card ${reviewIndex + 1} of ${flashcards.size}",
        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = "Tap card to flip",
        color = VocabElectricBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Flip Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 230.dp)
        .clickable { isFlipped = !isFlipped }
        .testTag("vocab_review_card"),
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isFlipped) {
          if (isDark) Color(0xFF0F172A) else Color(0xFFF0F7FF)
        } else {
          if (isDark) Color(0xFF1E293B) else Color.White
        }
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.5.dp,
        if (isFlipped) VocabElectricBlue.copy(alpha = 0.5f) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        if (!isFlipped) {
          // FRONT: Target Word
          Text(
            text = "TARGET WORD",
            color = VocabCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = activeCard.frontText,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
          )
          if (activeCard.pronunciation.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = activeCard.pronunciation,
              color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
              fontSize = 14.sp
            )
          }
          Spacer(modifier = Modifier.height(18.dp))
          Surface(
            shape = CircleShape,
            color = VocabElectricBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.clickable {
                speechManager.speak(activeCard.frontText, targetLangCode)
              }
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Listen",
                tint = VocabElectricBlue,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        } else {
          // BACK: Meaning, Translation, and Example
          Text(
            text = "MEANING & TRANSLATION",
            color = VocabEmerald,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = activeCard.backText,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4 SM-2 Rating Choices (Again, Hard, Good, Easy)
    Text(
      text = "How well did you recall this word?",
      color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Again (< 1d)
      VocabSrsChoiceButton(
        label = "Again",
        interval = "< 1d",
        color = VocabRose,
        modifier = Modifier
          .weight(1f)
          .testTag("srs_choice_again"),
        onClick = {
          onReviewCard(activeCard, SrsRating.AGAIN)
          isFlipped = false
          if (reviewIndex < flashcards.size - 1) reviewIndex++ else reviewIndex = 0
        }
      )

      // Hard (1-2d)
      VocabSrsChoiceButton(
        label = "Hard",
        interval = "1-2d",
        color = VocabAmber,
        modifier = Modifier
          .weight(1f)
          .testTag("srs_choice_hard"),
        onClick = {
          onReviewCard(activeCard, SrsRating.HARD)
          isFlipped = false
          if (reviewIndex < flashcards.size - 1) reviewIndex++ else reviewIndex = 0
        }
      )

      // Good (3-7d)
      VocabSrsChoiceButton(
        label = "Good",
        interval = "3-7d",
        color = VocabElectricBlue,
        modifier = Modifier
          .weight(1f)
          .testTag("srs_choice_good"),
        onClick = {
          onReviewCard(activeCard, SrsRating.GOOD)
          isFlipped = false
          if (reviewIndex < flashcards.size - 1) reviewIndex++ else reviewIndex = 0
        }
      )

      // Easy (7-21d)
      VocabSrsChoiceButton(
        label = "Easy",
        interval = "7-21d",
        color = VocabEmerald,
        modifier = Modifier
          .weight(1f)
          .testTag("srs_choice_easy"),
        onClick = {
          onReviewCard(activeCard, SrsRating.EASY)
          isFlipped = false
          if (reviewIndex < flashcards.size - 1) reviewIndex++ else reviewIndex = 0
        }
      )
    }
  }
}

@Composable
private fun VocabSrsChoiceButton(
  label: String,
  interval: String,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = color.copy(alpha = 0.12f),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = label,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = interval,
        color = color.copy(alpha = 0.8f),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

// ==============================================================================
// TAB 2: SAVED VIEW (Search, Filter, List, Audio, Delete)
// ==============================================================================
@Composable
private fun VocabSavedView(
  flashcards: List<FlashcardProgress>,
  targetLangCode: String,
  speechManager: SpeechManager,
  onDelete: (String) -> Unit,
  onPractice: (String) -> Unit
) {
  val isDark = LingoThemeController.isDarkActive
  var searchQuery by remember { mutableStateOf("") }

  val filteredCards = remember(flashcards, searchQuery) {
    if (searchQuery.isBlank()) {
      flashcards
    } else {
      flashcards.filter {
        it.frontText.contains(searchQuery, ignoreCase = true) ||
          it.backText.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    // Search input
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vocab_search_saved_input"),
      placeholder = {
        Text("Search saved words or definitions...", fontSize = 13.5.sp)
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
      },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Close, contentDescription = "Clear")
          }
        }
      },
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VocabElectricBlue,
        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
      ),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "${filteredCards.size} Saved Words",
      color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
      fontSize = 12.5.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 4.dp)
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (filteredCards.isEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = if (searchQuery.isBlank()) "No saved words yet" else "No matches found",
            color = if (isDark) Color.White else Color(0xFF0F172A),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap the heart icon on any word in the Learn tab to save it here.",
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vocab_saved_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        filteredCards.forEach { card ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = card.frontText,
                  color = if (isDark) Color.White else Color(0xFF0F172A),
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                )
                if (card.pronunciation.isNotBlank()) {
                  Text(
                    text = card.pronunciation,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 12.sp
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = card.backText,
                  color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                  fontSize = 13.5.sp
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                IconButton(
                  onClick = { speechManager.speak(card.frontText, targetLangCode) },
                  modifier = Modifier.size(34.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen",
                    tint = VocabElectricBlue,
                    modifier = Modifier.size(18.dp)
                  )
                }

                IconButton(
                  onClick = { onPractice(card.frontText) },
                  modifier = Modifier.size(34.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Practice",
                    tint = VocabEmerald,
                    modifier = Modifier.size(18.dp)
                  )
                }

                IconButton(
                  onClick = { onDelete(card.id) },
                  modifier = Modifier.size(34.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = VocabRose.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
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

// ==============================================================================
// MODAL: QUICK QUIZ SHEET (3 Questions, +35 XP)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabQuickQuizSheet(
  words: List<VocabularyBuilderWordItem>,
  targetLanguageName: String,
  onDismiss: () -> Unit,
  onComplete: (earnedXp: Int, missedWords: List<VocabularyBuilderWordItem>) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isDark = LingoThemeController.isDarkActive
  var questionIndex by remember { mutableIntStateOf(0) }
  var correctCount by remember { mutableIntStateOf(0) }
  var selectedOption by remember { mutableStateOf<Int?>(null) }
  var isAnswerSubmitted by remember { mutableStateOf(false) }
  val missedList = remember { mutableListOf<VocabularyBuilderWordItem>() }

  val questions = remember(words) {
    words.take(3).map { targetWord ->
      val distractors = words.filter { it.id != targetWord.id }.map { it.meaning }.shuffled().take(2)
      val options = (distractors + targetWord.meaning).shuffled()
      VocabQuizQuestion(
        word = targetWord.word,
        correctMeaning = targetWord.meaning,
        options = options
      )
    }
  }

  val currentQ = questions.getOrNull(questionIndex)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (currentQ != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Vocabulary Quiz (${questionIndex + 1}/3)",
            color = VocabElectricBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Surface(
            shape = RoundedCornerShape(50),
            color = VocabAmber.copy(alpha = 0.18f)
          ) {
            Text(
              text = "+35 XP",
              color = VocabAmber,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "What is the meaning of:",
          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = currentQ.word,
          color = if (isDark) Color.White else Color(0xFF0F172A),
          fontSize = 28.sp,
          fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Options
        currentQ.options.forEachIndexed { optIdx, optionText ->
          val isCorrect = optionText == currentQ.correctMeaning
          val isSelected = selectedOption == optIdx
          val borderColor = when {
            isAnswerSubmitted && isCorrect -> VocabEmerald
            isAnswerSubmitted && isSelected && !isCorrect -> VocabRose
            isSelected -> VocabElectricBlue
            else -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
          }

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 5.dp)
              .clickable(enabled = !isAnswerSubmitted) {
                selectedOption = optIdx
                isAnswerSubmitted = true
                if (isCorrect) {
                  correctCount++
                } else {
                  val item = words.firstOrNull { it.word == currentQ.word }
                  if (item != null && !missedList.any { it.id == item.id }) {
                    missedList.add(item)
                  }
                }
              },
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = optionText,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
              )
              if (isAnswerSubmitted && isCorrect) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = VocabEmerald,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isAnswerSubmitted) {
          Button(
            onClick = {
              if (questionIndex < 2) {
                questionIndex++
                selectedOption = null
                isAnswerSubmitted = false
              } else {
                onComplete(35, missedList)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = VocabElectricBlue)
          ) {
            Text(
              text = if (questionIndex < 2) "Next Question →" else "Finish & Claim 35 XP 🎉",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

// ==============================================================================
// MODAL: QUICK PRACTICE MIC SHEET FOR THIS SPECIFIC WORD
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabWordPracticeMicSheet(
  word: VocabularyBuilderWordItem,
  targetLangCode: String,
  speechManager: SpeechManager,
  onDismiss: () -> Unit,
  onOpenFullCoach: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val isDark = LingoThemeController.isDarkActive

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    if (granted) {
      speechManager.startListening(targetLangCode)
    } else {
      Toast.makeText(context, "Microphone permission required for speaking", Toast.LENGTH_SHORT).show()
    }
  }

  // Instant pronunciation score comparison
  val matchRatio = remember(spokenText, word.word) {
    if (spokenText.isBlank()) 0
    else {
      val target = word.word.trim().lowercase()
      val user = spokenText.trim().lowercase()
      if (user.contains(target) || target.contains(user)) 95
      else {
        val commonChars = target.toSet().intersect(user.toSet()).size
        ((commonChars.toFloat() / target.toSet().size.coerceAtLeast(1)) * 100).toInt().coerceIn(30, 92)
      }
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Speak This Word",
        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = word.word,
        color = if (isDark) Color.White else Color(0xFF0F172A),
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold
      )

      if (word.phonetic.isNotBlank()) {
        Text(
          text = word.phonetic,
          color = VocabElectricBlue,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Big Circular Mic Button
      Box(
        modifier = Modifier
          .size(86.dp)
          .clip(CircleShape)
          .background(
            if (isListening) Brush.radialGradient(listOf(VocabRose, Color(0xFFDC2626)))
            else Brush.radialGradient(listOf(VocabCyan, VocabElectricBlue))
          )
          .clickable {
            if (isListening) {
              speechManager.stopListening()
            } else {
              if (hasPermission) {
                speechManager.startListening(targetLangCode)
              } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            }
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
          contentDescription = "Mic",
          tint = Color.White,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = if (isListening) "Listening... speak clearly" else "Tap and speak now",
        color = if (isListening) VocabRose else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
      )

      if (spokenText.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, VocabEmerald.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Heard: \"$spokenText\"",
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (matchRatio >= 80) "Great pronunciation!" else "Keep practicing to improve articulation",
                color = if (matchRatio >= 80) VocabEmerald else VocabAmber,
                fontSize = 12.sp
              )
            }
            Surface(
              shape = RoundedCornerShape(50),
              color = if (matchRatio >= 80) VocabEmerald.copy(alpha = 0.2f) else VocabAmber.copy(alpha = 0.2f)
            ) {
              Text(
                text = "$matchRatio%",
                color = if (matchRatio >= 80) VocabEmerald else VocabAmber,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      OutlinedButton(
        onClick = onOpenFullCoach,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50)
      ) {
        Icon(
          imageVector = Icons.Default.FitnessCenter,
          contentDescription = null,
          tint = VocabElectricBlue,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Open Full Pronunciation Coach", color = VocabElectricBlue)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

// ==============================================================================
// SETTINGS DIALOG
// ==============================================================================
@Composable
private fun VocabSettingsDialog(
  currentCefr: String,
  onDismiss: () -> Unit
) {
  val isDark = LingoThemeController.isDarkActive
  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
      ) {
        Text(
          text = "Vocabulary Builder Settings",
          color = if (isDark) Color.White else Color(0xFF0F172A),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "• Level: $currentCefr",
          color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
          fontSize = 13.5.sp
        )
        Text(
          text = "• Algorithm: SuperMemo SM-2 Spaced Repetition",
          color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
          fontSize = 13.5.sp
        )
        Text(
          text = "• Daily Goal: 5 words per day with quick memory quiz",
          color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
          fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(containerColor = VocabElectricBlue)
        ) {
          Text("Done", color = Color.White)
        }
      }
    }
  }
}

// ==============================================================================
// VOCABULARY CATEGORIES & SUBCATEGORIES (1. Fashion, 2. School, 3. Buildings, 4. Work, 5. People, 6. Food)
// ==============================================================================
@Composable
fun VocabCategoriesExplorerSection(
  targetLangCode: String,
  speechManager: SpeechManager,
  onSaveWordToFlashcards: (front: String, back: String, phonetic: String) -> Unit,
  onAddXp: (Int) -> Unit
) {
  val context = LocalContext.current
  val isDark = LingoThemeController.isDarkActive
  val categories = remember(targetLangCode) {
    com.example.data.repository.InteractiveGamesCatalogAndAiService.getVocabularyCategories(targetLangCode)
  }

  var selectedMainCategory by remember { mutableStateOf<com.example.data.repository.VocabMainCategory?>(null) }
  var selectedSubCategory by remember { mutableStateOf<com.example.data.repository.VocabSubCategory?>(null) }
  var activeItemIndex by remember(selectedSubCategory) { mutableIntStateOf(0) }
  var quizChoice by remember(selectedSubCategory, activeItemIndex) { mutableIntStateOf(-1) }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) speechManager.startListening(targetLangCode)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("vocab_categories_explorer_section")
  ) {
    when {
      selectedSubCategory != null -> {
        val sub = selectedSubCategory!!
        val item = sub.items.getOrNull(activeItemIndex) ?: sub.items.first()
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TextButton(onClick = { selectedSubCategory = null }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Back to ${selectedMainCategory?.title ?: "Categories"}")
          }
          Text(
            text = "${activeItemIndex + 1}/${sub.items.size}",
            fontWeight = FontWeight.Bold,
            color = VocabElectricBlue
          )
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("vocab_subcategory_lesson_card"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = sub.title,
              color = VocabEmerald,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Visual Illustration
            VisualSceneAndClothingIllustration(
              kind = item.illustrationKind,
              height = 170.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Word & Translation + Audio Pronunciation + Save
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.word,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                  text = "${item.phonetic} • ${item.translation}",
                  fontSize = 14.sp,
                  color = VocabElectricBlue,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Row {
                IconButton(onClick = { speechManager.speak(item.word, targetLangCode) }) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = VocabElectricBlue)
                }
                IconButton(onClick = {
                  onSaveWordToFlashcards(item.word, item.translation, item.phonetic)
                }) {
                  Icon(Icons.Default.Bookmark, contentDescription = "Save to flashcards", tint = VocabAmber)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Example Sentence
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = item.exampleSentence,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isDark) Color.White else Color(0xFF1E293B),
                  fontSize = 14.sp
                )
                Text(
                  text = item.exampleTranslation,
                  color = Color(0xFF64748B),
                  fontSize = 12.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Listening & Speaking Practice Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { speechManager.speak(item.exampleSentence, targetLangCode) },
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Listen Sentence", fontSize = 12.sp)
              }
              Button(
                onClick = {
                  val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                  if (hasPerm) {
                    if (isListening) speechManager.finishListeningNow() else speechManager.startListening(targetLangCode)
                  } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) VocabRose else VocabElectricBlue),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isListening) "Listening..." else "Speak Word", fontSize = 12.sp)
              }
            }

            if (spokenText.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "You said: \"$spokenText\"",
                color = VocabEmerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Short Interactive Quiz
            Text(
              text = item.quizQuestion,
              fontWeight = FontWeight.Bold,
              color = if (isDark) Color.White else Color(0xFF0F172A),
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            item.quizOptions.forEachIndexed { idx, opt ->
              val isChosen = quizChoice == idx
              val isCorrect = idx == item.correctOptionIndex
              val borderCol = when {
                quizChoice < 0 -> Color(0xFFCBD5E1)
                isCorrect -> VocabEmerald
                isChosen -> VocabRose
                else -> Color(0xFFCBD5E1)
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isChosen && isCorrect) VocabEmerald.copy(alpha = 0.16f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, borderCol),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp)
                  .clickable {
                    quizChoice = idx
                    if (isCorrect) speechManager.speak(item.word, targetLangCode)
                  }
              ) {
                Text(
                  text = opt,
                  color = if (isDark) Color.White else Color(0xFF1E293B),
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                if (activeItemIndex + 1 < sub.items.size) {
                  activeItemIndex++
                } else {
                  onAddXp(25)
                  selectedSubCategory = null
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = VocabEmerald),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = if (activeItemIndex + 1 < sub.items.size) "Next Word in ${sub.title}" else "Complete ${sub.title} (+25 XP)",
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      selectedMainCategory != null -> {
        val mainCat = selectedMainCategory!!
        // Subcategory List (matching Screenshot 2: "1. FASHION" -> "1.1. Summerwear", "1.2. Winterwear", "1.3. Footwear", "1.4. Accessories")
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { selectedMainCategory = null }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Categories", tint = VocabEmerald)
          }
          Text(
            text = mainCat.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = VocabEmerald,
            modifier = Modifier.testTag("vocab_main_category_header")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        mainCat.subcategories.forEach { sub ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .clickable { selectedSubCategory = sub }
              .testTag("vocab_subcategory_card_${sub.id}"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = sub.title,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = sub.subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              VisualSceneAndClothingIllustration(
                kind = sub.illustrationKind,
                height = 145.dp
              )
            }
          }
        }
      }

      else -> {
        // Main Categories List (matching Screenshot 3: "1. Fashion", "2. School", "3. Buildings", "4. Work", "5. People", "6. Food & Shopping")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Vocabulary Categories",
              fontSize = 18.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
              text = "Tap a category to explore illustrated topics & sub-lessons",
              fontSize = 12.sp,
              color = Color(0xFF64748B)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        categories.forEach { cat ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 7.dp)
              .clickable { selectedMainCategory = cat }
              .testTag("vocab_category_card_${cat.id}"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = cat.title,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
              )
              Text(
                text = cat.subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              VisualSceneAndClothingIllustration(
                kind = cat.illustrationKind,
                height = 140.dp
              )
            }
          }
        }
      }
    }
  }
}

