package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.WorkspacePremium
import com.example.ui.components.LingoUnderlineTabBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.GeminiTutorService
import com.example.ai.VideoAiAssistantService
import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.Language
import com.example.data.model.LingoVideo
import com.example.data.model.ListeningAcademyItem
import com.example.data.model.ListeningDialogueLine
import com.example.data.model.UserProfile
import com.example.data.model.VideoAiStudyPack
import com.example.data.repository.CurriculumData
import com.example.data.repository.LearningMemoryAndOfflineManager
import com.example.data.repository.YouTubeVideoService
import com.example.speech.SpeechManager
import com.example.ui.components.EmojiVoice
import com.example.ui.components.EmojiVoiceState
import com.example.ui.components.YouTubeEmbeddedPlayer
import com.example.ai.AutoScrollGenerationResult
import com.example.ai.AutoScrollLearningContent
import com.example.ai.AutoScrollLearningService
import com.example.ai.AutoScrollPageSource
import com.example.data.model.ReadingAcademyPassage
import com.example.data.model.ReadingWordGloss
import com.example.ui.components.AutoScrollTeleprompterView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.mutableStateMapOf
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.BookCategoryCluster
import com.example.data.repository.AudiobookCatalogRepository
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.speech.AudiobookMediaController
import com.example.ui.components.AudiobookCarouselCard
import com.example.ui.components.AudiobookCompactListItem
import com.example.ui.components.AudiobookMiniPlayerBar
import com.example.ui.components.AudiobookPlayerMode
import com.example.ui.components.AudiobookPlayerScreen
import com.example.ui.components.AudiobookSearchDiscoveryScreen
import com.example.ui.components.AudiobooksCatalogSessionState
import com.example.ui.components.AudiobooksCategoryScreen
import com.example.ui.components.BookDetailScreen
import com.example.ui.components.CategoryBooksExplorerScreen
import com.example.ui.components.CreateMyListeningSheet
import com.example.ai.AiErrorHandler
import com.example.ai.UpgradeSheetMode
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.data.repository.AccessPolicyManager
import com.example.data.repository.LingoRepository
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoSecondaryChip
import com.example.ui.components.LingoSectionHeader
import com.example.ui.components.LingoTokens
import com.example.ui.components.MyLibraryDedicatedScreen
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.components.springTap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavyBackground = Color(0xFF060D1D)
private val NavyCardBg = Color(0xFF0D162B)
private val NavyCardElevated = Color(0xFF13203D)
private val ElectricBlue = Color(0xFF3B82F6)
private val ElectricBlueLight = Color(0xFF60A5FA)
private val CyanAccent = Color(0xFF38BDF8)
private val GoldAccent = Color(0xFFFBBF24)
private val EmeraldAccent = Color(0xFF10B981)
private val CoralAccent = Color(0xFFF43F5E)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val TextLight = Color(0xFFCBD5E1)

enum class BooksListeningInitialSection {
  MAIN,
  BOOKS,
  READING_ACADEMY
}

private enum class ListeningViewMode {
  MAIN,
  CATEGORY_STORIES,
  CATEGORY_CONVERSATIONS,
  CATEGORY_PODCASTS,
  CATEGORY_AUDIOBOOKS,
  READING_ACADEMY,
  READING_STORY_READER,
  READING_STORY_QUIZ,
  MY_LIBRARY,
  LISTENING_CHALLENGE,
  CREATE_LISTENING,
  PLAYER,
  TRANSCRIPT,
  QUIZ,
  RESULTS,
  WATCH_AND_LEARN,
  AUTO_SCROLL_TELEPROMPTER,
  BOOK_DETAIL,
  BOOK_PLAYER,
  CATEGORY_BOOKS_EXPLORER,
  SEARCH_DISCOVERY
}

private enum class TranscriptTab {
  TRANSCRIPT,
  VOCABULARY,
  NOTES
}

private data class CategoryTopicOption(
  val title: String,
  val subtitle: String,
  val promptScenario: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningPracticeScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  initialSection: BooksListeningInitialSection = BooksListeningInitialSection.MAIN,
  bookmarkedReadingIds: Set<String> = emptySet(),
  completedAcademyIds: Set<String> = emptySet(),
  onToggleReadingBookmark: (String) -> Unit = {},
  onCompleteReadingItem: (itemId: String, title: String, scorePercent: Int, xpReward: Int) -> Unit = { _, _, _, _ -> },
  onSaveVocabToFlashcard: (word: String, meaning: String, phonetic: String) -> Unit = { _, _, _ -> },
  onAddXp: (Long) -> Unit = {},
  onCompleteLesson: (String) -> Unit = {},
  onOpenSubscription: () -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val memoryManager = remember { LearningMemoryAndOfflineManager(context) }

  val targetLang = remember(userProfile.targetLanguageCode) {
    Language.findByCode(userProfile.targetLanguageCode)
  }
  val nativeLang = remember(userProfile.nativeLanguageCode) {
    Language.findByCode(userProfile.nativeLanguageCode)
  }

  // Active view state and backstack memory
  var currentViewMode by remember(initialSection) {
    mutableStateOf(
      when (initialSection) {
        BooksListeningInitialSection.BOOKS -> ListeningViewMode.CATEGORY_AUDIOBOOKS
        BooksListeningInitialSection.READING_ACADEMY -> ListeningViewMode.READING_ACADEMY
        BooksListeningInitialSection.MAIN -> ListeningViewMode.MAIN
      }
    )
  }
  var previousViewMode by remember { mutableStateOf(ListeningViewMode.MAIN) }

  // Merged Reading Academy (A1-C2 learning stories) state
  var readingAcademyPassages by remember(targetLang.code) {
    mutableStateOf(CurriculumData.getReadingAcademyPassages(targetLang.code))
  }
  var selectedReadingPassageId by remember(targetLang.code) {
    mutableStateOf(readingAcademyPassages.firstOrNull()?.id.orEmpty())
  }
  val savedReadingParagraphPositions = remember { mutableStateMapOf<String, Int>() }
  var isGeneratingReadingStory by remember { mutableStateOf(false) }
  var readingStoryGenerationError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(targetLang.code, userProfile.userId) {
    val cachedPassages = withContext(Dispatchers.IO) {
      memoryManager.loadAiReadingPassages(targetLang.code)
    }
    val combinedPassages = (cachedPassages + CurriculumData.getReadingAcademyPassages(targetLang.code))
      .distinctBy { it.id }
    readingAcademyPassages = combinedPassages
    if (selectedReadingPassageId.isBlank() || combinedPassages.none { it.id == selectedReadingPassageId }) {
      selectedReadingPassageId = combinedPassages.firstOrNull()?.id.orEmpty()
    }
  }

  fun triggerGenerateReadingAcademyStory(cefr: String, category: String, topic: String) {
    if (isGeneratingReadingStory) return
    isGeneratingReadingStory = true
    readingStoryGenerationError = null
    scope.launch {
      val prefs = AutoScrollLearningService.loadPreferences(context, userProfile.userId)
      val result = AutoScrollLearningService.generateFreshAutoScrollContent(
        context = context,
        userId = userProfile.userId,
        pageSource = AutoScrollPageSource.READING,
        targetLanguageCode = targetLang.code,
        nativeLanguageCode = nativeLang.code,
        cefrLevel = cefr,
        category = category,
        topic = topic,
        contentLength = prefs.preferredLength,
        completedLessonIds = completedAcademyIds,
        isPremiumUser = userProfile.isPremium
      )
      isGeneratingReadingStory = false
      when (result) {
        is AutoScrollGenerationResult.Success -> {
          val generated = result.content
          val paragraphs = generated.sentences
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(generated.fullText.trim()).filter { it.isNotBlank() } }
          val translations = generated.sentences
            .map { it.translation.trim() }
            .ifEmpty { paragraphs.map { "" } }
          val glossaryMap = generated.vocabulary
            .filter { it.word.isNotBlank() }
            .associate { vocab ->
              vocab.word.lowercase().trim() to ReadingWordGloss(
                word = vocab.word.trim(),
                translation = vocab.meaningInNative.trim(),
                partOfSpeech = vocab.partOfSpeech.ifBlank { "word" },
                definition = vocab.exampleSentence.ifBlank { vocab.meaningInNative.trim() },
                phonetic = vocab.phonetic
              )
            }
          val quizQuestions = generated.questions.map { q ->
            AcademyComprehensionQuestion(
              question = q.question,
              options = q.options,
              correctIndex = q.correctIndex,
              explanation = q.explanation
            )
          }
          val newPassage = ReadingAcademyPassage(
            id = generated.id,
            languageCode = generated.languageCode,
            cefrLevel = generated.cefrLevel,
            category = generated.category,
            title = generated.title,
            paragraphs = paragraphs,
            paragraphTranslations = translations,
            glossary = glossaryMap,
            questions = quizQuestions
          )
          withContext(Dispatchers.IO) {
            memoryManager.appendAiReadingPassage(newPassage)
          }
          readingAcademyPassages = (listOf(newPassage) + readingAcademyPassages).distinctBy { it.id }
          selectedReadingPassageId = newPassage.id
          currentViewMode = ListeningViewMode.READING_STORY_READER
        }
        is AutoScrollGenerationResult.Error -> {
          readingStoryGenerationError = result.userFriendlyMessage
        }
      }
    }
  }

  // Audiobook Selected Item States
  var selectedAudiobook by remember { mutableStateOf<AudiobookBook?>(null) }
  var selectedEpisodeIndex by remember { mutableIntStateOf(0) }
  var selectedBookPlayerMode by remember { mutableStateOf(AudiobookPlayerMode.READ_AND_LISTEN) }
  var autoStartBookPlayback by remember { mutableStateOf(false) }
  var selectedCategoryCluster by remember { mutableStateOf<BookCategoryCluster?>(null) }
  var initialCatalogLetter by remember { mutableStateOf("All") }
  var bookDetailOriginViewMode by remember { mutableStateOf(ListeningViewMode.MAIN) }
  val audiobooksCatalogSessionState = remember { AudiobooksCatalogSessionState(initialCatalogLetter) }
  var showCreateListeningSheet by remember { mutableStateOf(false) }

  fun navigateTo(mode: ListeningViewMode) {
    if (currentViewMode != mode) {
      if (mode == ListeningViewMode.PLAYER || mode == ListeningViewMode.TRANSCRIPT || mode == ListeningViewMode.QUIZ) {
        if (currentViewMode in listOf(
            ListeningViewMode.MAIN,
            ListeningViewMode.CATEGORY_STORIES,
            ListeningViewMode.CATEGORY_CONVERSATIONS,
            ListeningViewMode.CATEGORY_PODCASTS,
            ListeningViewMode.LISTENING_CHALLENGE
          )
        ) {
          previousViewMode = currentViewMode
        }
      }
      currentViewMode = mode
    }
  }

  // CEFR level filter
  val cefrLevels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
  var selectedCefrLevel by remember {
    mutableStateOf(
      when (userProfile.proficiencyLevel.lowercase()) {
        "advanced" -> "B2"
        "intermediate" -> "B1"
        else -> "A2"
      }
    )
  }
  var showCefrMenu by remember { mutableStateOf(false) }

  // Seeded + dynamic lessons strictly matched to targetLang.code
  var dynamicItems by remember(userProfile.targetLanguageCode) {
    mutableStateOf(createCategorySeedLessons(targetLang, selectedCefrLevel))
  }
  var isGeneratingNewDialogue by remember { mutableStateOf(false) }
  val savedLessonIds = remember { mutableStateListOf<String>() }

  // Load cached AI listening items for this exact language
  LaunchedEffect(userProfile.targetLanguageCode) {
    withContext(Dispatchers.IO) {
      val cached = memoryManager.loadAiListeningItems(userProfile.targetLanguageCode)
        .filter { it.languageCode.equals(userProfile.targetLanguageCode, ignoreCase = true) }
        .map { normalizeLessonMetadata(it, targetLang) }
      val combined = buildCompleteListeningCatalog(
        targetLang = targetLang,
        defaultCefr = selectedCefrLevel,
        cachedItems = cached
      )
      withContext(Dispatchers.Main) {
        dynamicItems = combined
      }
    }
  }

  // Active lesson for Continue Listening & Player
  var activeItem by remember(dynamicItems, userProfile.targetLanguageCode) {
    mutableStateOf(
      dynamicItems.firstOrNull {
        it.languageCode.equals(targetLang.code, ignoreCase = true) &&
          it.cefrLevel.equals(selectedCefrLevel, ignoreCase = true)
      }
        ?: dynamicItems.firstOrNull { it.languageCode.equals(targetLang.code, ignoreCase = true) }
        ?: createFallbackListeningItem(targetLang.code, selectedCefrLevel)
    )
  }

  // Audio Player State
  var isPlaying by remember { mutableStateOf(false) }
  var currentLineIndex by remember { mutableIntStateOf(0) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var isMuted by remember { mutableStateOf(false) }
  var showSpeedMenu by remember { mutableStateOf(false) }

  // Transcript view states
  var activeTranscriptTab by remember { mutableStateOf(TranscriptTab.TRANSCRIPT) }
  var showTranslations by remember { mutableStateOf(true) }

  // Word bottom sheet state
  var selectedWordForSheet by remember { mutableStateOf<Pair<String, String>?>(null) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Quiz states
  var quizQuestions by remember(activeItem) { mutableStateOf(activeItem.questions) }
  var currentQuestionIndex by remember { mutableIntStateOf(0) }
  var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
  var isQuestionEvaluated by remember { mutableStateOf(false) }
  var correctAnswersCount by remember { mutableIntStateOf(0) }

  // Daily progress tracking
  val completedListeningCount = remember(userProfile.completedLessonIds) {
    userProfile.completedLessonIds.count { it.startsWith("listen_") || it.startsWith("ai_listen_") }.coerceAtLeast(2)
  }

  // Stop TTS speech on leave
  DisposableEffect(Unit) {
    onDispose {
      try {
        speechManager.stopSpeaking()
      } catch (_: Exception) {}
    }
  }

  fun stopAudio() {
    isPlaying = false
    try {
      speechManager.stopSpeaking()
    } catch (_: Exception) {}
  }

  // Back handling across all views
  BackHandler {
    when (currentViewMode) {
      ListeningViewMode.MAIN -> {
        stopAudio()
        onBack()
      }
      ListeningViewMode.TRANSCRIPT -> currentViewMode = ListeningViewMode.PLAYER
      ListeningViewMode.QUIZ -> {
        stopAudio()
        currentViewMode = ListeningViewMode.PLAYER
      }
      ListeningViewMode.RESULTS -> {
        com.example.ads.AdService.maybeShowInterstitialAtNaturalBreak(
          userProfile = userProfile,
          reason = "Finished Listening Episode"
        )
        currentViewMode = ListeningViewMode.MAIN
      }
      ListeningViewMode.PLAYER -> {
        stopAudio()
        currentViewMode = previousViewMode
      }
      ListeningViewMode.CATEGORY_STORIES,
      ListeningViewMode.CATEGORY_CONVERSATIONS,
      ListeningViewMode.CATEGORY_PODCASTS,
      ListeningViewMode.CATEGORY_AUDIOBOOKS,
      ListeningViewMode.READING_ACADEMY,
      ListeningViewMode.MY_LIBRARY,
      ListeningViewMode.LISTENING_CHALLENGE,
      ListeningViewMode.CREATE_LISTENING,
      ListeningViewMode.WATCH_AND_LEARN,
      ListeningViewMode.AUTO_SCROLL_TELEPROMPTER,
      ListeningViewMode.BOOK_DETAIL,
      ListeningViewMode.CATEGORY_BOOKS_EXPLORER,
      ListeningViewMode.SEARCH_DISCOVERY -> {
        stopAudio()
        currentViewMode = ListeningViewMode.MAIN
      }
      ListeningViewMode.READING_STORY_READER -> {
        stopAudio()
        currentViewMode = ListeningViewMode.READING_ACADEMY
      }
      ListeningViewMode.READING_STORY_QUIZ -> {
        stopAudio()
        currentViewMode = ListeningViewMode.READING_STORY_READER
      }
      ListeningViewMode.BOOK_PLAYER -> {
        stopAudio()
        val book = selectedAudiobook
        val totalCh = book?.totalChapterCount?.coerceAtLeast(1) ?: 1
        if (book != null && selectedEpisodeIndex >= totalCh - 1) {
          com.example.ads.AdService.maybeShowInterstitialAtNaturalBreak(
            userProfile = userProfile,
            reason = "Finished Book or Episode"
          )
        }
        currentViewMode = if (selectedAudiobook != null) ListeningViewMode.BOOK_DETAIL else ListeningViewMode.MAIN
      }
    }
  }

  fun playCurrentLine(lineIdx: Int) {
    if (lineIdx >= activeItem.lines.size) {
      isPlaying = false
      currentLineIndex = 0
      return
    }
    currentLineIndex = lineIdx
    isPlaying = true
    if (isMuted) return

    val currentLine = activeItem.lines[lineIdx]
    val speakerPitch = if (lineIdx % 2 == 0) 1.06f else 0.94f

    speechManager.speakWithVoice(
      text = currentLine.targetText,
      languageCode = targetLang.code,
      pitch = speakerPitch,
      speed = playbackSpeed,
      onDone = {
        if (isPlaying) {
          scope.launch {
            delay(450L)
            if (isPlaying) {
              if (currentLineIndex + 1 < activeItem.lines.size) {
                playCurrentLine(currentLineIndex + 1)
              } else {
                isPlaying = false
                currentLineIndex = 0
              }
            }
          }
        }
      }
    )
  }

  fun togglePlayPause() {
    if (isPlaying) {
      stopAudio()
    } else {
      playCurrentLine(currentLineIndex)
    }
  }

  fun seekRelative(direction: Int) {
    val newIndex = (currentLineIndex + direction).coerceIn(0, (activeItem.lines.size - 1).coerceAtLeast(0))
    currentLineIndex = newIndex
    if (isPlaying) {
      stopAudio()
      playCurrentLine(newIndex)
    }
  }

  // Create fresh lesson in specific category and open directly in the dedicated Player
  fun generateFreshListeningLesson(
    categoryLabel: String,
    topicScenario: String,
    openInPlayerWhenReady: Boolean = true
  ) {
    if (isGeneratingNewDialogue) return
    isGeneratingNewDialogue = true
    stopAudio()
    scope.launch {
      try {
        val rawItem = GeminiTutorService.generateAiListeningDialogue(
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          cefrLevel = selectedCefrLevel,
          category = categoryLabel,
          specificScenario = topicScenario,
          existingTitles = dynamicItems.map { it.title }
        )
        val normalizedItem = normalizeLessonMetadata(
          rawItem.copy(
            languageCode = targetLang.code,
            category = categoryLabel,
            cefrLevel = selectedCefrLevel
          ),
          targetLang
        )
        withContext(Dispatchers.IO) {
          memoryManager.appendAiListeningItem(normalizedItem)
        }
        dynamicItems = listOf(normalizedItem) + dynamicItems.filter { it.id != normalizedItem.id }
        activeItem = normalizedItem
        if (!savedLessonIds.contains(normalizedItem.id)) {
          savedLessonIds.add(0, normalizedItem.id)
        }
        currentLineIndex = 0
        quizQuestions = normalizedItem.questions
        currentQuestionIndex = 0
        selectedOptionIndex = null
        isQuestionEvaluated = false
        if (openInPlayerWhenReady) {
          navigateTo(ListeningViewMode.PLAYER)
        }
        Toast.makeText(context, "Ready: ${normalizedItem.title}", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Could not create lesson: ${e.message}", Toast.LENGTH_SHORT).show()
      } finally {
        isGeneratingNewDialogue = false
      }
    }
  }

  // Vocabulary lookup Bottom Sheet
  if (selectedWordForSheet != null) {
    ModalBottomSheet(
      onDismissRequest = { selectedWordForSheet = null },
      sheetState = sheetState,
      containerColor = NavyCardElevated,
      dragHandle = {
        Box(
          modifier = Modifier
            .padding(vertical = 12.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(TextMuted.copy(alpha = 0.5f))
        )
      }
    ) {
      val (word, meaning) = selectedWordForSheet!!
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 36.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = word,
              color = TextWhite,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Key Vocabulary • ${targetLang.name}",
              color = CyanAccent,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(
            onClick = {
              speechManager.speak(word, targetLang.code, 0.9f)
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(ElectricBlue.copy(alpha = 0.2f))
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Listen to word",
              tint = CyanAccent
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
          colors = CardDefaults.cardColors(containerColor = NavyCardBg),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Meaning",
              color = TextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = meaning.ifBlank { "Essential expression used in this ${targetLang.name} lesson." },
              color = TextLight,
              fontSize = 15.sp,
              lineHeight = 22.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            onSaveVocabToFlashcard(word, meaning, "")
            Toast.makeText(context, "Saved '$word' to Flashcards!", Toast.LENGTH_SHORT).show()
            selectedWordForSheet = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_vocab_flashcard_button")
        ) {
          Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = TextWhite)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Save to Flashcards", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }
    }
  }

  // Create My Listening Modal Bottom Sheet (Pro-only AI generator)
  if (showCreateListeningSheet && userProfile.isEntitlementVerified) {
    ModalBottomSheet(
      onDismissRequest = { showCreateListeningSheet = false },
      containerColor = Color(0xFF060D1E)
    ) {
      CreateMyListeningSheet(
        userId = userProfile.userId,
        targetLang = targetLang,
        nativeLang = nativeLang,
        userLevel = selectedCefrLevel,
        isPremium = userProfile.isEntitlementVerified,
        speechManager = speechManager,
        onBookCreatedAndPlay = { createdBook ->
          showCreateListeningSheet = false
          AudiobookMediaController.onBookOpened(createdBook, 0)
          selectedAudiobook = createdBook
          selectedEpisodeIndex = 0
          currentViewMode = ListeningViewMode.BOOK_PLAYER
        },
        onDismiss = { showCreateListeningSheet = false }
      )
    }
  }

  val bookRepo = remember(context) { LingoRepository(context.applicationContext) }
  val isSignedInUser = userProfile.userId.isNotBlank() && !userProfile.userId.startsWith("guest", ignoreCase = true)

  fun openBookWithQuotaCheck(book: AudiobookBook, originMode: ListeningViewMode) {
    if (!isSignedInUser) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.BOOKS,
        featureKey = AccessPolicyFeatureKeys.BOOKS_READING,
        headline = "Sign in to continue",
        reason = "Sign in to open books and enjoy 5 free books every month."
      )
      return
    }
    val isPro = AccessPolicyManager.isUserVerifiedPro(userProfile)
    if (!isPro) {
      val decision = AccessPolicyManager.canOpenBookThisMonth(userProfile, book.id, book.gutendexId)
      if (!decision.allowed) {
        AiErrorHandler.requestUpgrade(
          mode = UpgradeSheetMode.BOOKS,
          featureKey = AccessPolicyFeatureKeys.BOOKS_READING,
          headline = "Unlock all stories and audio",
          reason = "You've used all 5 free books this month. Watch a short ad to unlock this book or upgrade to LingoSphere Pro for unlimited books and audio.",
          itemId = book.id,
          onRewardedUnlockGranted = {
            AccessPolicyManager.recordOpenedBookLocallyForMonth(book.id, book.gutendexId)
            AudiobookMediaController.onBookOpened(book)
            selectedAudiobook = book
            bookDetailOriginViewMode = originMode
            navigateTo(ListeningViewMode.BOOK_DETAIL)
          }
        )
        return
      }
      AccessPolicyManager.recordOpenedBookLocallyForMonth(book.id, book.gutendexId)
      scope.launch {
        val serverResult = bookRepo.recordOrCheckBookAccessViaBackend(book.id, book.gutendexId)
        val errMsg = serverResult.exceptionOrNull()?.message.orEmpty()
        if (errMsg.contains("PRO_REQUIRED", ignoreCase = true) || errMsg.contains("SIGN_IN_REQUIRED", ignoreCase = true)) {
          currentViewMode = originMode
        }
      }
    }
    AudiobookMediaController.onBookOpened(book)
    selectedAudiobook = book
    bookDetailOriginViewMode = originMode
    navigateTo(ListeningViewMode.BOOK_DETAIL)
  }

  fun openProLockedSection(mode: ListeningViewMode) {
    val isProOnlyMode = mode == ListeningViewMode.CATEGORY_STORIES ||
      mode == ListeningViewMode.CATEGORY_CONVERSATIONS ||
      mode == ListeningViewMode.CATEGORY_PODCASTS ||
      mode == ListeningViewMode.READING_ACADEMY ||
      mode == ListeningViewMode.LISTENING_CHALLENGE ||
      mode == ListeningViewMode.AUTO_SCROLL_TELEPROMPTER ||
      mode == ListeningViewMode.CREATE_LISTENING

    if (!isSignedInUser && mode != ListeningViewMode.CATEGORY_AUDIOBOOKS) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.BOOKS,
        featureKey = AccessPolicyFeatureKeys.LISTENING,
        headline = "Sign in to continue",
        reason = "Sign in to access stories, conversations, podcasts, and your saved library."
      )
      return
    }

    val rewardedFeatureForMode = when (mode) {
      ListeningViewMode.CATEGORY_CONVERSATIONS -> com.example.ads.RewardedFeatureType.CONVERSATION
      ListeningViewMode.CATEGORY_PODCASTS -> com.example.ads.RewardedFeatureType.PODCAST
      else -> com.example.ads.RewardedFeatureType.STORY
    }
    val hasModeAdUnlock = com.example.ads.AdService.hasActiveRewardedUnlock(rewardedFeatureForMode, mode.name)
    val isProUser = AccessPolicyManager.isUserVerifiedPro(userProfile)

    if (isProOnlyMode && !isProUser && !hasModeAdUnlock) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.BOOKS,
        featureKey = rewardedFeatureForMode.id,
        headline = "Unlock all stories and audio",
        reason = "Stories, conversations, podcasts, and Reading Academy are included with LingoSphere Pro.",
        itemId = mode.name,
        onRewardedUnlockGranted = {
          navigateTo(mode)
        }
      )
      return
    }

    if (mode == ListeningViewMode.CATEGORY_AUDIOBOOKS) {
      initialCatalogLetter = "All"
      audiobooksCatalogSessionState.selectedLetter = "All"
    }
    navigateTo(mode)
  }

  fun openCreateListeningIfPro() {
    if (!AccessPolicyManager.isUserVerifiedPro(userProfile)) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.BOOKS,
        featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL,
        headline = "Unlock all stories and audio",
        reason = "Upgrade to LingoSphere Pro to create custom stories, dialogues, and audio lessons.",
        onRewardedUnlockGranted = {
          showCreateListeningSheet = true
        }
      )
      return
    }
    showCreateListeningSheet = true
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyBackground)
  ) {
    when (currentViewMode) {
      ListeningViewMode.MAIN -> {
        ListeningMainHubScreen(
          userProfile = userProfile,
          targetLang = targetLang,
          nativeLang = nativeLang,
          selectedCefrLevel = selectedCefrLevel,
          showCefrMenu = showCefrMenu,
          cefrLevels = cefrLevels,
          completedListeningCount = completedListeningCount,
          activeItem = activeItem,
          allItems = dynamicItems,
          savedLessonIds = savedLessonIds,
          isPlaying = isPlaying,
          currentLineIndex = currentLineIndex,
          isGenerating = isGeneratingNewDialogue,
          onToggleCefrMenu = { showCefrMenu = !showCefrMenu },
          onSelectCefr = { cefr ->
            selectedCefrLevel = cefr
            showCefrMenu = false
            val matching = dynamicItems.firstOrNull {
              it.languageCode.equals(targetLang.code, ignoreCase = true) &&
                it.cefrLevel.equals(cefr, ignoreCase = true)
            }
            if (matching != null) {
              activeItem = matching
              currentLineIndex = 0
              stopAudio()
            }
          },
          onOpenPlayerForActive = {
            navigateTo(ListeningViewMode.PLAYER)
          },
          onTogglePlayPause = { togglePlayPause() },
          onOpenTranscript = {
            navigateTo(ListeningViewMode.TRANSCRIPT)
          },
          onStartQuiz = {
            stopAudio()
            quizQuestions = activeItem.questions
            currentQuestionIndex = 0
            selectedOptionIndex = null
            isQuestionEvaluated = false
            correctAnswersCount = 0
            navigateTo(ListeningViewMode.QUIZ)
          },
          onOpenCategory = { mode ->
            openProLockedSection(mode)
          },
          onOpenBookLibraryWithLetter = { letter ->
            initialCatalogLetter = letter
            audiobooksCatalogSessionState.selectedLetter = letter
            navigateTo(ListeningViewMode.CATEGORY_AUDIOBOOKS)
          },
          onSelectLessonToPlay = { item ->
            stopAudio()
            activeItem = item
            currentLineIndex = 0
            navigateTo(ListeningViewMode.PLAYER)
          },
          onOpenSearch = {
            navigateTo(ListeningViewMode.SEARCH_DISCOVERY)
          },
          onOpenBookDetail = { book ->
            openBookWithQuotaCheck(book, ListeningViewMode.MAIN)
          },
          onOpenClusterExplorer = { cluster ->
            selectedCategoryCluster = cluster
            navigateTo(ListeningViewMode.CATEGORY_BOOKS_EXPLORER)
          },
          onOpenCreateListening = {
            openCreateListeningIfPro()
          },
          onOpenSubscription = onOpenSubscription,
          onBack = {
            stopAudio()
            onBack()
          }
        )
      }

      ListeningViewMode.BOOK_DETAIL -> {
        val currentBook = selectedAudiobook ?: AudiobookCatalogRepository.CURATED_BOOKS.first()
        val isProUser = AccessPolicyManager.isUserVerifiedPro(userProfile)
        val canReadCurrentBook = isProUser ||
          AccessPolicyManager.canOpenBookThisMonth(userProfile, currentBook.id, currentBook.gutendexId).allowed
        BookDetailScreen(
          book = currentBook,
          userId = userProfile.userId,
          targetLang = targetLang,
          speechManager = speechManager,
          isProUnlocked = isProUser,
          isBookAllowanceUnlocked = canReadCurrentBook,
          onOpenSubscription = onOpenSubscription,
          onBookLoadedWithChapters = { enrichedBook ->
            selectedAudiobook = enrichedBook
          },
          onPlayEpisode = { epIdx ->
            if (!canReadCurrentBook) {
              AiErrorHandler.requestUpgrade(mode = UpgradeSheetMode.BOOKS, featureKey = AccessPolicyFeatureKeys.BOOKS_READING)
            } else {
              selectedEpisodeIndex = epIdx
              selectedBookPlayerMode = AudiobookPlayerMode.READ_AND_LISTEN
              autoStartBookPlayback = true
              currentViewMode = ListeningViewMode.BOOK_PLAYER
            }
          },
          onReadChapter = { chIdx ->
            if (!canReadCurrentBook) {
              AiErrorHandler.requestUpgrade(mode = UpgradeSheetMode.BOOKS, featureKey = AccessPolicyFeatureKeys.BOOKS_READING)
            } else {
              selectedEpisodeIndex = chIdx
              selectedBookPlayerMode = AudiobookPlayerMode.READ_AND_LISTEN
              autoStartBookPlayback = false
              currentViewMode = ListeningViewMode.BOOK_PLAYER
            }
          },
          onReadOnlyChapter = { chIdx ->
            if (!canReadCurrentBook) {
              AiErrorHandler.requestUpgrade(mode = UpgradeSheetMode.BOOKS, featureKey = AccessPolicyFeatureKeys.BOOKS_READING)
            } else {
              selectedEpisodeIndex = chIdx
              selectedBookPlayerMode = AudiobookPlayerMode.READ_ONLY
              autoStartBookPlayback = false
              currentViewMode = ListeningViewMode.BOOK_PLAYER
            }
          },
          onListenOnlyChapter = { chIdx ->
            if (!canReadCurrentBook) {
              AiErrorHandler.requestUpgrade(mode = UpgradeSheetMode.BOOKS, featureKey = AccessPolicyFeatureKeys.BOOKS_READING)
            } else {
              selectedEpisodeIndex = chIdx
              selectedBookPlayerMode = AudiobookPlayerMode.LISTEN_ONLY
              autoStartBookPlayback = true
              currentViewMode = ListeningViewMode.BOOK_PLAYER
            }
          },
          onBack = { currentViewMode = bookDetailOriginViewMode }
        )
      }

      ListeningViewMode.BOOK_PLAYER -> {
        val currentBook = selectedAudiobook ?: AudiobookCatalogRepository.CURATED_BOOKS.first()
        AudiobookPlayerScreen(
          book = currentBook,
          initialEpisodeIndex = selectedEpisodeIndex,
          initialPlayerMode = selectedBookPlayerMode,
          autoStartPlayback = autoStartBookPlayback,
          targetLang = targetLang,
          nativeLang = nativeLang,
          cefrLevel = selectedCefrLevel,
          speechManager = speechManager,
          userId = userProfile.userId,
          isProUnlocked = userProfile.isEntitlementVerified,
          onSaveVocabToFlashcard = onSaveVocabToFlashcard,
          onAddXp = onAddXp,
          onOpenSubscription = onOpenSubscription,
          onChapterChanged = { updatedChIdx ->
            selectedEpisodeIndex = updatedChIdx
          },
          onBack = {
            val totalCh = currentBook.totalChapterCount.coerceAtLeast(1)
            if (selectedEpisodeIndex >= totalCh - 1) {
              com.example.ads.AdService.maybeShowInterstitialAtNaturalBreak(
                userProfile = userProfile,
                reason = "Finished Book or Episode"
              )
            }
            currentViewMode = ListeningViewMode.BOOK_DETAIL
          }
        )
      }

      ListeningViewMode.CATEGORY_BOOKS_EXPLORER -> {
        val cluster = selectedCategoryCluster ?: AudiobookCatalogRepository.CATEGORY_CLUSTERS.first()
        CategoryBooksExplorerScreen(
          cluster = cluster,
          userId = userProfile.userId,
          targetLang = targetLang,
          isProUnlocked = AccessPolicyManager.isUserVerifiedPro(userProfile),
          userProfile = userProfile,
          onOpenSubscription = onOpenSubscription,
          onSelectBook = { book ->
            openBookWithQuotaCheck(book, ListeningViewMode.CATEGORY_BOOKS_EXPLORER)
          },
          onOpenCreateListening = {
            openCreateListeningIfPro()
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.SEARCH_DISCOVERY -> {
        AudiobookSearchDiscoveryScreen(
          userId = userProfile.userId,
          targetLang = targetLang,
          isProUnlocked = AccessPolicyManager.isUserVerifiedPro(userProfile),
          userProfile = userProfile,
          onOpenSubscription = onOpenSubscription,
          onSelectBook = { book ->
            openBookWithQuotaCheck(book, ListeningViewMode.SEARCH_DISCOVERY)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.CATEGORY_AUDIOBOOKS -> {
        AudiobooksCategoryScreen(
          userId = userProfile.userId,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          initialSelectedLetter = initialCatalogLetter,
          sessionState = audiobooksCatalogSessionState,
          isProUnlocked = AccessPolicyManager.isUserVerifiedPro(userProfile),
          userProfile = userProfile,
          onOpenSubscription = onOpenSubscription,
          onSelectBook = { book ->
            openBookWithQuotaCheck(book, ListeningViewMode.CATEGORY_AUDIOBOOKS)
          },
          onOpenSearch = {
            currentViewMode = ListeningViewMode.SEARCH_DISCOVERY
          },
          onOpenCreateListening = {
            openCreateListeningIfPro()
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.MY_LIBRARY -> {
        MyLibraryDedicatedScreen(
          userProfile = userProfile,
          targetLang = targetLang,
          allBooks = AudiobookCatalogRepository.FAMOUS_FULL_BOOKS + AudiobookCatalogRepository.CURATED_LIBRIVOX_AUDIOBOOKS,
          onSelectBook = { book ->
            openBookWithQuotaCheck(book, ListeningViewMode.MY_LIBRARY)
          },
          onSelectLesson = { lesson ->
            stopAudio()
            activeItem = lesson
            currentLineIndex = 0
            navigateTo(ListeningViewMode.PLAYER)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.CATEGORY_STORIES -> {
        DedicatedCategoryScreen(
          categoryTitle = "Stories",
          categorySubtitle = "Listen and imagine with engaging narratives in ${targetLang.name}.",
          categoryKey = "Stories",
          heroDrawableRes = R.drawable.img_listening_stories,
          accentColor = PurpleAccent,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          topics = listOf(
            CategoryTopicOption("Mystery", "A missing painting at the city museum", "A gripping mystery story about a missing painting at a gallery"),
            CategoryTopicOption("Adventure", "A secret map in a coastal town", "An exciting adventure story discovering a hidden path"),
            CategoryTopicOption("Comedy", "A hilarious mix-up at a train station", "A lighthearted comedy story about swapped suitcases"),
            CategoryTopicOption("Friendship", "Two childhood friends reuniting", "A warm story about friendship and shared memories"),
            CategoryTopicOption("Romance", "An unexpected meeting at a bookshop", "A charming story set in an old neighborhood bookshop"),
            CategoryTopicOption("Life Lessons", "Learning patience from a master chef", "An inspiring story with a meaningful life lesson"),
            CategoryTopicOption("Fantasy", "The clockmaker's enchanted watch", "An imaginative fantasy tale with vivid descriptions"),
            CategoryTopicOption("Surprise Me", "A fresh original story with a twist", "An original story with an unexpected ending")
          ),
          lessons = dynamicItems.filter { isLessonInCategory(it, "Stories") },
          isGenerating = isGeneratingNewDialogue,
          onGenerateTopic = { scenario ->
            generateFreshListeningLesson("Stories", scenario, openInPlayerWhenReady = true)
          },
          onSelectLesson = { item ->
            stopAudio()
            activeItem = item
            currentLineIndex = 0
            navigateTo(ListeningViewMode.PLAYER)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.CATEGORY_CONVERSATIONS -> {
        DedicatedCategoryScreen(
          categoryTitle = "Conversations",
          categorySubtitle = "Everyday language and practical dialogues for real-world situations.",
          categoryKey = "Conversations",
          heroDrawableRes = R.drawable.img_listening_coffee_shop,
          accentColor = CyanAccent,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          topics = listOf(
            CategoryTopicOption("Coffee Shop", "Ordering drinks and pastries", "Ordering coffee and pastries at a busy neighborhood café"),
            CategoryTopicOption("Airport & Travel", "Boarding gates and ticket changes", "Checking in at the airport and asking about gate changes"),
            CategoryTopicOption("Job Interview", "Discussing strengths and experience", "A professional job interview discussing career goals"),
            CategoryTopicOption("Restaurant", "Asking for recommendations", "Reserving a table and ordering regional dishes at a restaurant"),
            CategoryTopicOption("Hotel Reception", "Checking in and local tips", "Checking into a boutique hotel and asking about sightseeing"),
            CategoryTopicOption("Shopping", "Finding sizes and returns", "Shopping for clothes and asking about sizes and prices"),
            CategoryTopicOption("Healthcare", "Describing symptoms at a clinic", "Visiting a local pharmacy and asking for advice"),
            CategoryTopicOption("Weekend Plans", "Catching up with friends", "Two friends making plans for a weekend concert and dinner")
          ),
          lessons = dynamicItems.filter { isLessonInCategory(it, "Conversations") },
          isGenerating = isGeneratingNewDialogue,
          onGenerateTopic = { scenario ->
            generateFreshListeningLesson("Conversations", scenario, openInPlayerWhenReady = true)
          },
          onSelectLesson = { item ->
            stopAudio()
            activeItem = item
            currentLineIndex = 0
            navigateTo(ListeningViewMode.PLAYER)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.CATEGORY_PODCASTS -> {
        DedicatedCategoryScreen(
          categoryTitle = "Podcasts",
          categorySubtitle = "Interesting ideas, culture, and short discussions in ${targetLang.name}.",
          categoryKey = "Podcasts",
          heroDrawableRes = R.drawable.img_listening_podcast,
          accentColor = GoldAccent,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          userProfile = userProfile,
          onOpenSubscription = onOpenSubscription,
          topics = listOf(
            CategoryTopicOption("Culture & Cities", "Hidden traditions and local life", "A podcast episode exploring local cultural traditions and historic neighborhoods"),
            CategoryTopicOption("Science & Nature", "Curiosities of the natural world", "An engaging podcast discussing fascinating science discoveries"),
            CategoryTopicOption("Technology", "How digital tools shape daily life", "A podcast conversation about modern technology and remote work"),
            CategoryTopicOption("Food & History", "The story behind famous dishes", "A culinary podcast about the origins of traditional cuisine"),
            CategoryTopicOption("Wellbeing", "Building mindful daily habits", "A thoughtful podcast on morning routines and balanced living"),
            CategoryTopicOption("Arts & Music", "Creative movements and cinema", "A cultural podcast discussing cinema, art galleries, and music")
          ),
          lessons = dynamicItems.filter { isLessonInCategory(it, "Podcasts") },
          isGenerating = isGeneratingNewDialogue,
          onGenerateTopic = { scenario ->
            generateFreshListeningLesson("Podcasts", scenario, openInPlayerWhenReady = true)
          },
          onSelectLesson = { item ->
            stopAudio()
            activeItem = item
            currentLineIndex = 0
            navigateTo(ListeningViewMode.PLAYER)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.LISTENING_CHALLENGE -> {
        DedicatedCategoryScreen(
          categoryTitle = "Listening Challenge",
          categorySubtitle = "Sharpen your ear with focused comprehension challenges and quizzes.",
          categoryKey = "Listening Challenge",
          heroDrawableRes = R.drawable.img_listening_interview,
          accentColor = EmeraldAccent,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          topics = listOf(
            CategoryTopicOption("Detail Detective", "Catch numbers, times, and platforms", "A fast-paced station announcement and travel dialogue with specific times and numbers"),
            CategoryTopicOption("Workplace Challenge", "Key decisions in a team meeting", "A workplace meeting where speakers agree on deadlines and priorities"),
            CategoryTopicOption("Mystery Clues", "Spot the contradiction in the story", "A short detective dialogue where listeners must catch key clues"),
            CategoryTopicOption("Everyday Speed", "Natural spoken expressions", "A natural conversation with idioms and polite requests")
          ),
          lessons = dynamicItems,
          isGenerating = isGeneratingNewDialogue,
          onGenerateTopic = { scenario ->
            generateFreshListeningLesson("Listening Challenge", scenario, openInPlayerWhenReady = true)
          },
          onSelectLesson = { item ->
            stopAudio()
            activeItem = item
            currentLineIndex = 0
            navigateTo(ListeningViewMode.PLAYER)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.CREATE_LISTENING -> {
        CreateCustomListeningScreen(
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          isGenerating = isGeneratingNewDialogue,
          onCreateLesson = { category, customPrompt ->
            generateFreshListeningLesson(category, customPrompt, openInPlayerWhenReady = true)
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.PLAYER -> {
        DedicatedAudioPlayerScreen(
          activeItem = activeItem,
          targetLang = targetLang,
          isPlaying = isPlaying,
          currentLineIndex = currentLineIndex,
          playbackSpeed = playbackSpeed,
          isMuted = isMuted,
          showSpeedMenu = showSpeedMenu,
          showTranslations = showTranslations,
          isSaved = savedLessonIds.contains(activeItem.id),
          onToggleSave = {
            if (savedLessonIds.contains(activeItem.id)) {
              savedLessonIds.remove(activeItem.id)
              Toast.makeText(context, "Removed from My Library", Toast.LENGTH_SHORT).show()
            } else {
              savedLessonIds.add(0, activeItem.id)
              Toast.makeText(context, "Saved to My Library", Toast.LENGTH_SHORT).show()
            }
          },
          onTogglePlayPause = { togglePlayPause() },
          onSeekRelative = { delta -> seekRelative(delta) },
          onPlaySpecificLine = { lineIdx ->
            stopAudio()
            playCurrentLine(lineIdx)
          },
          onToggleMute = { isMuted = !isMuted },
          onToggleSpeedMenu = { showSpeedMenu = !showSpeedMenu },
          onSelectSpeed = { spd ->
            playbackSpeed = spd
            showSpeedMenu = false
          },
          onToggleTranslations = { showTranslations = !showTranslations },
          onSelectWord = { word, meaning ->
            selectedWordForSheet = word to meaning
          },
          onOpenFullTranscript = {
            navigateTo(ListeningViewMode.TRANSCRIPT)
          },
          onOpenAutoScrollTeleprompter = {
            stopAudio()
            navigateTo(ListeningViewMode.AUTO_SCROLL_TELEPROMPTER)
          },
          onStartQuiz = {
            stopAudio()
            quizQuestions = activeItem.questions
            currentQuestionIndex = 0
            selectedOptionIndex = null
            isQuestionEvaluated = false
            correctAnswersCount = 0
            navigateTo(ListeningViewMode.QUIZ)
          },
          onNextLesson = {
            stopAudio()
            val currentIdx = dynamicItems.indexOfFirst { it.id == activeItem.id }
            val nextItem = if (currentIdx in 0 until dynamicItems.lastIndex) {
              dynamicItems[currentIdx + 1]
            } else {
              dynamicItems.firstOrNull { it.id != activeItem.id }
            }
            if (nextItem != null) {
              activeItem = nextItem
              currentLineIndex = 0
            }
          },
          onBack = {
            stopAudio()
            currentViewMode = previousViewMode
          }
        )
      }

      ListeningViewMode.WATCH_AND_LEARN -> {
        WatchAndLearnVideoScreen(
          userProfile = userProfile,
          speechManager = speechManager,
          targetLang = targetLang,
          nativeLang = nativeLang,
          selectedCefrLevel = selectedCefrLevel,
          onSaveVocabToFlashcard = onSaveVocabToFlashcard,
          onAddXp = onAddXp,
          onCompleteLesson = onCompleteLesson,
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.AUTO_SCROLL_TELEPROMPTER -> {
        var teleprompterContent by remember(activeItem.id, targetLang.code, selectedCefrLevel) {
          mutableStateOf(
            AutoScrollLearningService.loadCachedContentList(
              context = context,
              userId = userProfile.userId,
              pageSource = AutoScrollPageSource.LISTENING,
              languageCode = targetLang.code
            ).firstOrNull { it.cefrLevel.equals(selectedCefrLevel, ignoreCase = true) }
              ?: AutoScrollLearningService.fromListeningItem(activeItem, nativeLang.code)
          )
        }
        var selectedTeleprompterCategory by remember {
          mutableStateOf(AutoScrollLearningService.LISTENING_CATEGORIES.first())
        }
        var isGeneratingTeleprompter by remember { mutableStateOf(false) }
        var teleprompterError by remember { mutableStateOf<String?>(null) }

        AutoScrollTeleprompterView(
          userId = userProfile.userId,
          content = teleprompterContent,
          availableContents = dynamicItems.map {
            AutoScrollLearningService.fromListeningItem(it, nativeLang.code)
          },
          categories = AutoScrollLearningService.LISTENING_CATEGORIES,
          selectedCategory = selectedTeleprompterCategory,
          selectedCefrLevel = selectedCefrLevel,
          isFullScreen = true,
          isGenerating = isGeneratingTeleprompter,
          errorMessage = teleprompterError,
          speechManager = speechManager,
          onSelectContent = { chosen ->
            teleprompterError = null
            teleprompterContent = chosen
          },
          onSelectCategory = { cat ->
            selectedTeleprompterCategory = cat
            val cachedMatch = AutoScrollLearningService.loadCachedContentList(
              context = context,
              userId = userProfile.userId,
              pageSource = AutoScrollPageSource.LISTENING,
              languageCode = targetLang.code
            ).firstOrNull {
              it.category.equals(cat, ignoreCase = true) &&
                it.cefrLevel.equals(selectedCefrLevel, ignoreCase = true)
            }
            if (cachedMatch != null) {
              teleprompterContent = cachedMatch
            } else if (!userProfile.isEntitlementVerified) {
              AiErrorHandler.requestUpgrade(
                mode = UpgradeSheetMode.AI_UPGRADE,
                featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL,
                headline = "Unlock AI Reading & Listening Packs",
                reason = "Upgrade to LingoSphere Pro to generate custom Listen & Read passages on any topic."
              )
            } else {
              scope.launch {
                isGeneratingTeleprompter = true
                teleprompterError = null
                val generated = AutoScrollLearningService.generateFreshAutoScrollContent(
                  context = context,
                  userId = userProfile.userId,
                  pageSource = AutoScrollPageSource.LISTENING,
                  targetLanguageCode = targetLang.code,
                  nativeLanguageCode = nativeLang.code,
                  cefrLevel = selectedCefrLevel,
                  category = cat,
                  isPremiumUser = userProfile.isEntitlementVerified
                )
                isGeneratingTeleprompter = false
                when (generated) {
                  is com.example.ai.AutoScrollGenerationResult.Success -> {
                    teleprompterContent = generated.content
                  }
                  is com.example.ai.AutoScrollGenerationResult.Error -> {
                    teleprompterError = AiErrorHandler.sanitizeUserFacingMessage(generated.userFriendlyMessage)
                  }
                }
              }
            }
          },
          onSelectCefrLevel = { cefr ->
            selectedCefrLevel = cefr
          },
          onRequestGenerateNew = { cat, cefr, len, customTopic ->
            if (!userProfile.isEntitlementVerified) {
              AiErrorHandler.requestUpgrade(
                mode = UpgradeSheetMode.AI_UPGRADE,
                featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL,
                headline = "Unlock AI Reading & Listening Packs",
                reason = "Upgrade to LingoSphere Pro to generate fresh custom Listen & Read passages."
              )
            } else {
              selectedTeleprompterCategory = cat
              selectedCefrLevel = cefr
              scope.launch {
                isGeneratingTeleprompter = true
                teleprompterError = null
                val generated = AutoScrollLearningService.generateFreshAutoScrollContent(
                  context = context,
                  userId = userProfile.userId,
                  pageSource = AutoScrollPageSource.LISTENING,
                  targetLanguageCode = targetLang.code,
                  nativeLanguageCode = nativeLang.code,
                  cefrLevel = cefr,
                  category = cat,
                  topic = customTopic,
                  contentLength = len,
                  isPremiumUser = userProfile.isEntitlementVerified
                )
                isGeneratingTeleprompter = false
                when (generated) {
                  is com.example.ai.AutoScrollGenerationResult.Success -> {
                    teleprompterContent = generated.content
                  }
                  is com.example.ai.AutoScrollGenerationResult.Error -> {
                    teleprompterError = AiErrorHandler.sanitizeUserFacingMessage(generated.userFriendlyMessage)
                  }
                }
              }
            }
          },
          onSaveVocabToFlashcard = { word, meaning, phonetic ->
            onSaveVocabToFlashcard(word, meaning, phonetic)
            Toast.makeText(context, "Saved '$word' to Flashcards!", Toast.LENGTH_SHORT).show()
          },
          onCompleteActivity = { contentId, _, _, xp ->
            onAddXp(xp.toLong())
            onCompleteLesson(contentId)
          },
          onBack = {
            speechManager.stopSpeaking()
            currentViewMode = ListeningViewMode.MAIN
          }
        )
      }

      ListeningViewMode.TRANSCRIPT -> {
        ListeningTranscriptContent(
          userProfile = userProfile,
          activeItem = activeItem,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          activeTab = activeTranscriptTab,
          showTranslations = showTranslations,
          isPlaying = isPlaying,
          currentLineIndex = currentLineIndex,
          playbackSpeed = playbackSpeed,
          onSelectTab = { activeTranscriptTab = it },
          onToggleTranslations = { showTranslations = !showTranslations },
          onPlayLine = { lineIdx ->
            stopAudio()
            playCurrentLine(lineIdx)
          },
          onTogglePlayPause = { togglePlayPause() },
          onSelectWord = { word, meaning ->
            selectedWordForSheet = word to meaning
          },
          onStartQuiz = {
            stopAudio()
            quizQuestions = activeItem.questions
            currentQuestionIndex = 0
            selectedOptionIndex = null
            isQuestionEvaluated = false
            correctAnswersCount = 0
            navigateTo(ListeningViewMode.QUIZ)
          },
          onOpenSubscription = onOpenSubscription,
          onBack = { currentViewMode = ListeningViewMode.PLAYER }
        )
      }

      ListeningViewMode.QUIZ -> {
        ListeningQuizContent(
          userProfile = userProfile,
          activeItem = activeItem,
          targetLang = targetLang,
          selectedCefrLevel = selectedCefrLevel,
          questions = quizQuestions,
          currentQuestionIndex = currentQuestionIndex,
          selectedOptionIndex = selectedOptionIndex,
          isEvaluated = isQuestionEvaluated,
          isPlayingAudio = isPlaying,
          speechManager = speechManager,
          onPlayPromptAudio = {
            if (isPlaying) {
              stopAudio()
            } else {
              val promptSpeech = activeItem.lines.joinToString(". ") { it.targetText }
              speechManager.speakWithVoice(promptSpeech, targetLang.code, 1.0f, 0.95f) {
                isPlaying = false
              }
              isPlaying = true
            }
          },
          onSelectOption = { idx ->
            if (!isQuestionEvaluated) {
              selectedOptionIndex = idx
            }
          },
          onCheckAnswer = {
            if (selectedOptionIndex != null) {
              isQuestionEvaluated = true
              val curQ = quizQuestions.getOrNull(currentQuestionIndex)
              if (curQ != null && selectedOptionIndex == curQ.correctIndex) {
                correctAnswersCount++
              }
            }
          },
          onNextQuestion = {
            if (currentQuestionIndex + 1 < quizQuestions.size) {
              currentQuestionIndex++
              selectedOptionIndex = null
              isQuestionEvaluated = false
            } else {
              onAddXp(15L)
              onCompleteLesson(activeItem.id)
              if (!savedLessonIds.contains(activeItem.id)) {
                savedLessonIds.add(0, activeItem.id)
              }
              currentViewMode = ListeningViewMode.RESULTS
            }
          },
          onBack = {
            stopAudio()
            currentViewMode = ListeningViewMode.PLAYER
          }
        )
      }

      ListeningViewMode.READING_ACADEMY -> {
        StoryBrowserScreen(
          passages = readingAcademyPassages,
          bookmarkedIds = bookmarkedReadingIds,
          completedIds = completedAcademyIds,
          savedParagraphPositions = savedReadingParagraphPositions,
          initialCefr = selectedCefrLevel,
          isGeneratingStory = isGeneratingReadingStory,
          generationErrorMessage = readingStoryGenerationError,
          isProUnlocked = userProfile.isEntitlementVerified,
          onGenerateNewStory = { cefr, cat, topic ->
            selectedCefrLevel = cefr
            triggerGenerateReadingAcademyStory(cefr, cat, topic)
          },
          onToggleBookmark = onToggleReadingBookmark,
          onSelectStory = { passage ->
            if (!userProfile.isEntitlementVerified) {
              AiErrorHandler.requestUpgrade(
                mode = UpgradeSheetMode.BOOKS,
                featureKey = AccessPolicyFeatureKeys.BOOKS_READING,
                headline = "Unlock all stories and audio",
                reason = "Stories, word coaching, and quizzes are included with LingoSphere Pro."
              )
            } else {
              selectedReadingPassageId = passage.id
              currentViewMode = ListeningViewMode.READING_STORY_READER
            }
          },
          onBack = { currentViewMode = ListeningViewMode.MAIN }
        )
      }

      ListeningViewMode.READING_STORY_READER -> {
        val passage = readingAcademyPassages.firstOrNull { it.id == selectedReadingPassageId }
          ?: readingAcademyPassages.firstOrNull()
        if (passage != null) {
          DedicatedStoryReaderScreen(
            passage = passage,
            activeParagraphIndex = savedReadingParagraphPositions[passage.id] ?: 0,
            isBookmarked = bookmarkedReadingIds.contains(passage.id),
            isCompleted = completedAcademyIds.contains(passage.id),
            speechManager = speechManager,
            onUpdateParagraphPosition = { idx -> savedReadingParagraphPositions[passage.id] = idx },
            onToggleBookmark = { onToggleReadingBookmark(passage.id) },
            onSaveVocabToFlashcard = onSaveVocabToFlashcard,
            onOpenTeleprompter = { currentViewMode = ListeningViewMode.AUTO_SCROLL_TELEPROMPTER },
            onStartQuiz = { currentViewMode = ListeningViewMode.READING_STORY_QUIZ },
            onBack = { currentViewMode = ListeningViewMode.READING_ACADEMY }
          )
        } else {
          currentViewMode = ListeningViewMode.READING_ACADEMY
        }
      }

      ListeningViewMode.READING_STORY_QUIZ -> {
        val passage = readingAcademyPassages.firstOrNull { it.id == selectedReadingPassageId }
          ?: readingAcademyPassages.firstOrNull()
        if (passage != null) {
          ReadingQuizScreen(
            itemTitle = passage.title,
            questions = passage.questions.map { q ->
              ComprehensionQuestionItem(
                prompt = q.question,
                options = q.options,
                correctIndex = q.correctIndex,
                explanation = q.explanation
              )
            },
            xpReward = 40,
            isAlreadyCompleted = completedAcademyIds.contains(passage.id),
            onQuizCompleted = { scorePct, earnedXp ->
              onCompleteReadingItem(passage.id, "Reading: ${passage.title}", scorePct, earnedXp)
              onAddXp(earnedXp.toLong())
            },
          onDoneBackToStudio = {
            com.example.ads.AdService.maybeShowInterstitialAtNaturalBreak(
              userProfile = userProfile,
              reason = "Finished Reading Story"
            )
            currentViewMode = ListeningViewMode.READING_ACADEMY
          },
          onBackToReader = { currentViewMode = ListeningViewMode.READING_STORY_READER }
        )
      } else {
        currentViewMode = ListeningViewMode.READING_ACADEMY
      }
    }

    ListeningViewMode.RESULTS -> {
      ListeningResultsContent(
        userProfile = userProfile,
        activeItem = activeItem,
        selectedCefrLevel = selectedCefrLevel,
        totalQuestions = quizQuestions.size.coerceAtLeast(1),
        correctAnswersCount = correctAnswersCount,
        onNextLesson = {
          val nextItem = dynamicItems.firstOrNull { it.id != activeItem.id }
          if (nextItem != null) {
            activeItem = nextItem
            currentLineIndex = 0
          }
          currentViewMode = ListeningViewMode.PLAYER
        },
        onReviewTranscript = {
          currentViewMode = ListeningViewMode.TRANSCRIPT
        },
        onBack = {
          com.example.ads.AdService.maybeShowInterstitialAtNaturalBreak(
            userProfile = userProfile,
            reason = "Finished Listening Episode"
          )
          currentViewMode = ListeningViewMode.MAIN
        }
      )
    }
    }
  }
}

// =================================================================================================
// 1. REDESIGNED MAIN LISTENING HUB SCREEN
// =================================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListeningMainHubScreen(
  userProfile: UserProfile,
  targetLang: Language,
  nativeLang: Language,
  selectedCefrLevel: String,
  showCefrMenu: Boolean,
  cefrLevels: List<String>,
  completedListeningCount: Int,
  activeItem: ListeningAcademyItem,
  allItems: List<ListeningAcademyItem>,
  savedLessonIds: List<String>,
  isPlaying: Boolean,
  currentLineIndex: Int,
  isGenerating: Boolean,
  onToggleCefrMenu: () -> Unit,
  onSelectCefr: (String) -> Unit,
  onOpenPlayerForActive: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onOpenTranscript: () -> Unit,
  onStartQuiz: () -> Unit,
  onOpenCategory: (ListeningViewMode) -> Unit,
  onOpenBookLibraryWithLetter: (String) -> Unit = {},
  onSelectLessonToPlay: (ListeningAcademyItem) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenBookDetail: (AudiobookBook) -> Unit,
  onOpenClusterExplorer: (BookCategoryCluster) -> Unit,
  onOpenCreateListening: () -> Unit,
  onOpenSubscription: () -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  val isPro = AccessPolicyManager.isUserVerifiedPro(userProfile)
  val activeMiniBook by AudiobookMediaController.activeBook.collectAsState()
  val isAudiobookPlaying by AudiobookMediaController.isPlaying.collectAsState()
  val openedIdsThisMonth by AccessPolicyManager.openedBookIdsThisMonth.collectAsState()
  val policyState by AccessPolicyManager.policyState.collectAsState()
  val booksLeftThisMonth = (policyState.freeBooksPerMonth - openedIdsThisMonth.size).coerceAtLeast(0)

  var allAudiobooks by remember(targetLang.code) {
    mutableStateOf<List<AudiobookBook>>(
      AudiobookCatalogRepository.FAMOUS_FULL_BOOKS + AudiobookCatalogRepository.CURATED_LIBRIVOX_AUDIOBOOKS
    )
  }
  var recommendations by remember(targetLang.code) {
    mutableStateOf<Map<String, List<AudiobookBook>>>(emptyMap())
  }
  var continueListeningBooks by remember {
    mutableStateOf<List<AudiobookBook>>(emptyList())
  }
  var lastBookProgress by remember {
    mutableStateOf(
      com.example.data.model.UserAudiobookProgress(
        bookId = AudiobookCatalogRepository.FAMOUS_FULL_BOOKS.first().id
      )
    )
  }

  LaunchedEffect(userProfile.userId, targetLang.code, selectedCefrLevel) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val books = AudiobookLearningService.getAllAvailableBooks(context, userProfile.userId)
      val recs = AudiobookLearningService.getPersonalizedRecommendations(
        context = context,
        userId = userProfile.userId,
        targetLanguageCode = targetLang.code,
        userLevel = selectedCefrLevel
      )
      val cont = AudiobookLearningService.getContinueListeningBooks(context, userProfile.userId)
      val active = cont.firstOrNull() ?: AudiobookCatalogRepository.FAMOUS_FULL_BOOKS.first()
      val prog = AudiobookLearningService.loadUserProgress(context, userProfile.userId, active.id)
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        allAudiobooks = books
        recommendations = recs
        continueListeningBooks = cont
        lastBookProgress = prog
      }
    }
  }

  // Exactly ONE unfinished item; hidden if nothing in progress
  val unfinishedBook = continueListeningBooks.firstOrNull()
  val hasUnfinishedBook = unfinishedBook != null && (lastBookProgress.lastSentenceIndex > 0 || lastBookProgress.lastEpisodeIndex > 0)
  val hasUnfinishedLesson = !hasUnfinishedBook && isPro && (currentLineIndex > 0 || completedListeningCount > 0)
  val showContinueSection = hasUnfinishedBook || hasUnfinishedLesson

  val recommendedBooks = remember(recommendations, allAudiobooks) {
    val recs = recommendations["Recommended for Your Level"] ?: emptyList()
    if (recs.isNotEmpty()) recs.take(8) else allAudiobooks.take(8)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .verticalScroll(scrollState)
        .padding(
          top = 16.dp,
          bottom = if (activeMiniBook != null) 188.dp else LingoTokens.bottomBarClearance
        ),
      verticalArrangement = Arrangement.spacedBy(LingoTokens.sectionSpacing)
    ) {
      // =========================================================================
      // 1. HEADER: Back, "Listen & Read" (28sp bold), Search, Filter (Level sheet)
      // =========================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = LingoTokens.screenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(LingoTokens.bgGlassCard)
              .border(1.dp, LingoTokens.borderSubtle, CircleShape)
              .testTag("listening_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              tint = LingoTokens.textPrimary
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Listen & Read",
            color = LingoTokens.textPrimary,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(
            onClick = onOpenSearch,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(LingoTokens.bgGlassCard)
              .border(1.dp, LingoTokens.borderSubtle, CircleShape)
              .testTag("listening_search_icon_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = "Search",
              tint = LingoTokens.textPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onToggleCefrMenu,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(LingoTokens.bgGlassCard)
              .border(1.dp, LingoTokens.borderSubtle, CircleShape)
              .testTag("cefr_selector_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Tune,
              contentDescription = "Filter level ($selectedCefrLevel)",
              tint = LingoTokens.textPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // =========================================================================
      // 2. ONE "CONTINUE" HERO CARD (Gradient, hidden if nothing in progress)
      // =========================================================================
      if (showContinueSection) {
        val isBookContinue = hasUnfinishedBook && unfinishedBook != null
        val heroTitle = if (isBookContinue) unfinishedBook!!.title else activeItem.title
        val heroSubtitle = if (isBookContinue) {
          "${unfinishedBook!!.author} · Chapter ${lastBookProgress.lastEpisodeIndex + 1}"
        } else {
          "${formatCleanCategoryName(activeItem)} · $selectedCefrLevel"
        }
        val heroProgress = if (isBookContinue) {
          val totalEp = unfinishedBook!!.episodes.size.coerceAtLeast(1)
          ((lastBookProgress.lastEpisodeIndex + 1).toFloat() / totalEp.toFloat()).coerceIn(0.08f, 1f)
        } else {
          val totalLines = activeItem.lines.size.coerceAtLeast(1)
          ((currentLineIndex + 1).toFloat() / totalLines.toFloat()).coerceIn(0.12f, 1f)
        }
        val heroCoverUrl = if (isBookContinue) unfinishedBook!!.coverArtUrl else ""

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LingoTokens.screenPadding)
            .shadow(
              elevation = 18.dp,
              shape = RoundedCornerShape(28.dp),
              ambientColor = LingoTokens.brandIndigo.copy(alpha = 0.36f),
              spotColor = LingoTokens.brandCyan.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
              Brush.linearGradient(
                listOf(LingoTokens.brandIndigo, Color(0xFF4F7CFF), LingoTokens.brandCyan)
              )
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag(if (isBookContinue) "continue_listening_book_card" else "continue_listening_lesson_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.size(width = 58.dp, height = 78.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = if (isBookContinue) Icons.AutoMirrored.Outlined.MenuBook else Icons.Outlined.Headphones,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                  )
                  if (heroCoverUrl.isNotBlank()) {
                    AsyncImage(
                      model = heroCoverUrl,
                      contentDescription = heroTitle,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  }
                }
              }

              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = "CONTINUE",
                  color = Color.White.copy(alpha = 0.86f),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                )
                Text(
                  text = heroTitle,
                  color = Color.White,
                  fontSize = 19.sp,
                  lineHeight = 24.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = heroSubtitle,
                  color = Color.White.copy(alpha = 0.88f),
                  fontSize = 13.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.24f))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(heroProgress)
                  .height(4.dp)
                  .clip(RoundedCornerShape(50))
                  .background(Color.White)
              )
            }

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .springTap {
                  if (isBookContinue) {
                    onOpenBookDetail(unfinishedBook!!)
                  } else {
                    onOpenPlayerForActive()
                  }
                }
                .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.PlayArrow,
                  contentDescription = null,
                  tint = Color(0xFF0B0F1A),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Resume",
                  color = Color(0xFF0B0F1A),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // =========================================================================
      // 3. EXPLORE (2x2 equal square glass tiles, soft glow icons, NO photo bg)
      // =========================================================================
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = LingoTokens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        LingoSectionHeader(title = "Explore")

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          ListenExploreGlassTile(
            title = "Stories",
            subtitle = "Graded tales",
            icon = Icons.Outlined.AutoStories,
            accentColor = Color(0xFFA78BFA),
            locked = !isPro,
            onClick = { onOpenCategory(ListeningViewMode.CATEGORY_STORIES) },
            testTag = "category_stories_card",
            modifier = Modifier.weight(1f)
          )
          ListenExploreGlassTile(
            title = "Conversations",
            subtitle = "Real dialogues",
            icon = Icons.Outlined.Forum,
            accentColor = Color(0xFF22D3EE),
            locked = !isPro,
            onClick = { onOpenCategory(ListeningViewMode.CATEGORY_CONVERSATIONS) },
            testTag = "category_conversations_card",
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          ListenExploreGlassTile(
            title = "Books & Audio",
            subtitle = if (isPro) "Full library" else "$booksLeftThisMonth of ${policyState.freeBooksPerMonth} free",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            accentColor = Color(0xFF60A5FA),
            locked = false,
            onClick = { onOpenCategory(ListeningViewMode.CATEGORY_AUDIOBOOKS) },
            testTag = "category_audiobooks_card",
            modifier = Modifier.weight(1f)
          )
          ListenExploreGlassTile(
            title = "Podcasts",
            subtitle = "Ideas & culture",
            icon = Icons.Outlined.Podcasts,
            accentColor = Color(0xFFF472B6),
            locked = !isPro,
            onClick = { onOpenCategory(ListeningViewMode.CATEGORY_PODCASTS) },
            testTag = "category_podcasts_card",
            modifier = Modifier.weight(1f)
          )
        }

        // =========================================================================
        // 4. READING ACADEMY (One slim glass row, book icon, A1-C2, chevron, lock)
        // =========================================================================
        LingoCleanCard(
          onClick = { onOpenCategory(ListeningViewMode.READING_ACADEMY) },
          contentPadding = 16.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("category_reading_academy_card")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF34D399).copy(alpha = 0.14f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(21.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Reading Academy",
                color = LingoTokens.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "A1–C2",
                color = LingoTokens.textSecondary,
                fontSize = 13.sp,
                maxLines = 1
              )
            }
            if (!isPro) {
              LingoGoldLockIcon(size = 16.dp)
              Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
              contentDescription = "Open Reading Academy",
              tint = LingoTokens.textSecondary,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }

      // =========================================================================
      // NATIVE AD CARD BETWEEN SECTIONS ON LISTENING HUB (Free users only)
      // =========================================================================
      if (!isPlaying && !isAudiobookPlaying) {
        com.example.ads.LingoNativeAdCard(
          userProfile = userProfile,
          placementKey = "listening_hub_between_sections",
          headline = "Unlock Unlimited Audiobooks, Podcasts & Stories",
          bodyText = "Listen & read without limits and enjoy a 100% ad-free experience with LingoSphere Pro.",
          ctaText = "Go Pro - No Ads",
          onCtaClick = onOpenSubscription,
          modifier = Modifier.padding(horizontal = LingoTokens.screenPadding)
        )
      }

      // =========================================================================
      // 5. RECOMMENDED FOR YOU (Horizontal covers 120x170dp + "See all")
      // =========================================================================
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        LingoSectionHeader(
          title = "Recommended for you",
          actionLabel = "See all",
          actionTestTag = "recommended_see_all_button",
          onAction = { onOpenCategory(ListeningViewMode.CATEGORY_AUDIOBOOKS) },
          modifier = Modifier.padding(horizontal = LingoTokens.screenPadding)
        )

        if (!isPro) {
          Text(
            text = "$booksLeftThisMonth of ${policyState.freeBooksPerMonth} free books left this month",
            color = LingoTokens.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = LingoTokens.screenPadding)
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = LingoTokens.screenPadding),
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          recommendedBooks.forEach { book ->
            AudiobookCarouselCard(
              book = book,
              targetLang = targetLang,
              onClick = { onOpenBookDetail(book) },
              isProUnlocked = isPro
            )
          }
        }
      }

      // Slim My Library row (Free saved items list)
      Column(modifier = Modifier.padding(horizontal = LingoTokens.screenPadding)) {
        LingoCleanCard(
          onClick = { onOpenCategory(ListeningViewMode.MY_LIBRARY) },
          contentPadding = 16.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("entry_my_library_card")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LingoTokens.brandCyan.copy(alpha = 0.14f))
                .border(1.dp, LingoTokens.brandCyan.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            ) {
              Icon(
                imageVector = Icons.Outlined.FolderSpecial,
                contentDescription = null,
                tint = LingoTokens.brandCyan,
                modifier = Modifier.size(21.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "My Library",
                color = LingoTokens.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
              )
              val savedCount = continueListeningBooks.size + savedLessonIds.size
              Text(
                text = if (savedCount > 0) "$savedCount saved items" else "Saved books & bookmarks",
                color = LingoTokens.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
              contentDescription = "Open My Library",
              tint = LingoTokens.textSecondary,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }

    // Persistent floating mini player docked above bottom navigation
    if (activeMiniBook != null) {
      AudiobookMiniPlayerBar(
        onExpandPlayer = { activeBook, _ -> onOpenBookDetail(activeBook) },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(bottom = 86.dp)
      )
    } else if (!isPlaying && !isAudiobookPlaying) {
      // Bottom banner ONLY on the Listening Hub home (never on Book Library or Player), with 8dp bottom spacing
      com.example.ads.LingoBannerAdBar(
        userProfile = userProfile,
        placementName = "listening_hub",
        onUpgradeToProClick = onOpenSubscription,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
      )
    }
  }

  // CEFR Level Filter Bottom Sheet (moved cleanly out of header)
  if (showCefrMenu) {
    ModalBottomSheet(
      onDismissRequest = onToggleCefrMenu,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color(0xFF0E1422),
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Filter by level",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary
        )
        Text(
          text = "Tailor recommendations for ${targetLang.name}",
          fontSize = 14.sp,
          color = LingoTokens.textSecondary
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          cefrLevels.forEach { cefr ->
            LingoSecondaryChip(
              text = cefr,
              selected = cefr == selectedCefrLevel,
              onClick = { onSelectCefr(cefr) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ListenExploreGlassTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  locked: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  LingoCleanCard(
    onClick = onClick,
    contentPadding = 18.dp,
    modifier = modifier
      .aspectRatio(1.06f)
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(46.dp)
            .drawBehind {
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
                  center = center,
                  radius = size.minDimension * 0.95f
                ),
                radius = size.minDimension * 0.95f,
                center = center
              )
            }
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
          )
        }
        if (locked) {
          LingoGoldLockIcon(size = 16.dp)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = title,
          color = LingoTokens.textPrimary,
          fontSize = 17.sp,
          lineHeight = 22.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          color = LingoTokens.textSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun ContinueListeningBannerCard(
  activeItem: ListeningAcademyItem,
  targetLang: Language,
  isPlaying: Boolean,
  currentLineIndex: Int,
  completedListeningCount: Int,
  onOpenPlayer: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onOpenTranscript: () -> Unit,
  onStartQuiz: () -> Unit
) {
  val imageRes = getContextualImageResource(activeItem)
  val totalLines = activeItem.lines.size.coerceAtLeast(1)
  val progressFraction = ((currentLineIndex + 1).toFloat() / totalLines.toFloat()).coerceIn(0.15f, 1f)
  val displayCategory = formatCleanCategoryName(activeItem)
  val descriptionText = buildLessonContextualDescription(activeItem, targetLang)

  Card(
    onClick = onOpenPlayer,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .testTag("continue_listening_card"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = NavyCardBg),
    border = BorderStroke(1.dp, Color(0xFF1E293B))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top label + Daily progress pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "CONTINUE LISTENING",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
          )
        }

        Text(
          text = "$completedListeningCount / 5 daily goal",
          color = TextMuted,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Lesson row with matching artwork, title, and Play button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AsyncImage(
          model = imageRes,
          contentDescription = activeItem.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = PurpleAccent.copy(alpha = 0.22f)
            ) {
              Text(
                text = displayCategory,
                color = Color(0xFFD8B4FE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF1E293B)
            ) {
              Text(
                text = "${activeItem.cefrLevel} • 3 min",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = activeItem.title,
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = descriptionText,
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(listOf(ElectricBlue, CyanAccent))
            )
            .testTag("featured_play_pause_button")
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = TextWhite,
            modifier = Modifier.size(26.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(CircleShape),
        color = CyanAccent,
        trackColor = Color(0xFF1E293B)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Responsive 2-column action buttons so labels never clip or wrap vertically
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onOpenTranscript,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF16254A),
            contentColor = TextWhite
          ),
          border = BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("transcript_toggle_button")
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Transcript",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Button(
          onClick = onStartQuiz,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("start_listening_quiz_button")
        ) {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = null,
            tint = TextWhite,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Listening Quiz",
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun PrimaryListeningCategoryCard(
  title: String,
  subtitle: String,
  imageRes: Int,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    onClick = onClick,
    modifier = modifier.heightIn(min = 164.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = NavyCardBg),
    border = BorderStroke(1.dp, Color(0xFF1E293B))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AsyncImage(
        model = imageRes,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      // Smooth dark gradient for crisp readability
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0x22000000),
                Color(0xAA06102B),
                Color(0xF206102B)
              )
            )
          )
      )

      // Accent pill top-left
      Box(
        modifier = Modifier
          .padding(12.dp)
          .size(10.dp)
          .clip(CircleShape)
          .background(accentColor)
          .align(Alignment.TopStart)
      )

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(14.dp)
      ) {
        Text(
          text = title,
          color = TextWhite,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          color = TextLight,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

// =================================================================================================
// 2. DEDICATED CATEGORY SCREEN (STORIES, CONVERSATIONS, PODCASTS, LISTENING CHALLENGE)
// =================================================================================================
@Composable
private fun DedicatedCategoryScreen(
  categoryTitle: String,
  categorySubtitle: String,
  categoryKey: String,
  heroDrawableRes: Int,
  accentColor: Color,
  targetLang: Language,
  selectedCefrLevel: String,
  userProfile: UserProfile? = null,
  onOpenSubscription: () -> Unit = {},
  topics: List<CategoryTopicOption>,
  lessons: List<ListeningAcademyItem>,
  isGenerating: Boolean,
  onGenerateTopic: (String) -> Unit,
  onSelectLesson: (ListeningAcademyItem) -> Unit,
  onBack: () -> Unit
) {
  val scrollState = rememberScrollState()
  var selectedDurationLabel by remember { mutableStateOf("Medium • 4 min") }
  var selectedConversationTab by remember { mutableIntStateOf(0) }
  val conversationTabs = listOf("Everyday", "Travel", "Work", "Social")
  val isConversations = categoryKey.equals("conversations", ignoreCase = true) ||
    categoryTitle.contains("Conversation", ignoreCase = true)
  val isPodcasts = categoryKey.equals("podcasts", ignoreCase = true) ||
    categoryTitle.contains("Podcast", ignoreCase = true)

  val visibleTopics = remember(topics, selectedConversationTab, isConversations) {
    if (!isConversations) {
      topics
    } else {
      val tabKey = conversationTabs.getOrElse(selectedConversationTab) { "Everyday" }
      val filtered = topics.filter { opt ->
        val haystack = "${opt.title} ${opt.subtitle} ${opt.promptScenario}".lowercase()
        when (tabKey) {
          "Travel" -> haystack.contains("travel") || haystack.contains("airport") || haystack.contains("hotel") || haystack.contains("train") || haystack.contains("direction")
          "Work" -> haystack.contains("work") || haystack.contains("job") || haystack.contains("interview") || haystack.contains("meeting") || haystack.contains("office") || haystack.contains("business")
          "Social" -> haystack.contains("friend") || haystack.contains("social") || haystack.contains("dinner") || haystack.contains("party") || haystack.contains("coffee") || haystack.contains("chat")
          else -> haystack.contains("cafe") || haystack.contains("daily") || haystack.contains("shop") || haystack.contains("restaurant") || haystack.contains("doctor") || haystack.contains("everyday")
        }
      }
      if (filtered.isNotEmpty()) filtered else topics
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = LingoTokens.bottomBarClearance)
  ) {
    // Clean Glass Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 14.dp, bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LingoTokens.bgGlassCard)
            .border(1.dp, LingoTokens.borderSubtle, CircleShape)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = LingoTokens.textPrimary
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = categoryTitle,
            color = LingoTokens.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${targetLang.name} · Level $selectedCefrLevel",
            color = LingoTokens.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
      if (isGenerating) {
        CircularProgressIndicator(
          color = LingoTokens.brandCyan,
          strokeWidth = 2.5.dp,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    if (isPodcasts) {
      // ONE Signature Gradient Featured Podcast Hero
      val featuredTopic = topics.firstOrNull()
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(LingoTokens.brandHeroGradient)
          .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
          .padding(20.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "FEATURED EPISODE",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
          )
          Text(
            text = featuredTopic?.title ?: "Ideas & Culture in Easy ${targetLang.name}",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = featuredTopic?.subtitle ?: categorySubtitle,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            onClick = {
              if (!isGenerating) {
                onGenerateTopic("${featuredTopic?.promptScenario ?: "Culture and everyday life"} ($selectedDurationLabel)")
              }
            },
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF070B14),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = if (isGenerating) "Preparing episode..." else "Start episode",
                color = Color(0xFF070B14),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }

    if (isConversations) {
      LingoUnderlineTabBar(
        tabs = conversationTabs,
        selectedIndex = selectedConversationTab,
        onSelectTab = { selectedConversationTab = it }
      )
      Spacer(modifier = Modifier.height(20.dp))
    }

    // Compact Duration Selector
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("Short • 2 min", "Medium • 4 min", "Deep • 6 min").forEach { lengthOpt ->
        val isSel = selectedDurationLabel == lengthOpt
        Surface(
          onClick = { selectedDurationLabel = lengthOpt },
          shape = RoundedCornerShape(14.dp),
          color = if (isSel) LingoTokens.brandIndigo.copy(alpha = 0.24f) else LingoTokens.bgGlassCard,
          border = BorderStroke(
            1.dp,
            if (isSel) LingoTokens.brandCyan.copy(alpha = 0.55f) else LingoTokens.borderSubtle
          ),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
        ) {
          Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = lengthOpt,
              color = if (isSel) LingoTokens.textPrimary else LingoTokens.textSecondary,
              fontSize = 13.sp,
              fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    if (isPodcasts) {
      com.example.ads.LingoNativeAdCard(
        userProfile = userProfile,
        placementKey = "podcasts_between_sections",
        headline = "Stream Every Podcast Episode Ad-Free with Pro",
        bodyText = "Unlock unlimited AI podcasts, interactive transcripts, and vocabulary coaching with LingoSphere Pro.",
        ctaText = "Go Pro - No Ads",
        onCtaClick = onOpenSubscription
      )
      Spacer(modifier = Modifier.height(20.dp))
    }

    Text(
      text = if (isPodcasts) "Topics" else "Choose a topic",
      color = LingoTokens.textPrimary,
      fontSize = 18.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (isPodcasts) {
      // Clean glass topic rows for Podcasts
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        visibleTopics.forEach { topic ->
          Surface(
            onClick = { if (!isGenerating) onGenerateTopic("${topic.promptScenario} ($selectedDurationLabel)") },
            shape = RoundedCornerShape(20.dp),
            color = LingoTokens.bgGlassCard,
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Podcasts,
                  contentDescription = null,
                  tint = accentColor,
                  modifier = Modifier.size(22.dp)
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = topic.title,
                  color = LingoTokens.textPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = topic.subtitle,
                  color = LingoTokens.textSecondary,
                  fontSize = 13.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LingoTokens.textSecondary
              )
            }
          }
        }
      }
    } else {
      // Calm 2-column glass cards for Conversations & other categories
      visibleTopics.chunked(2).forEach { rowPair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          rowPair.forEach { topic ->
            Surface(
              onClick = {
                if (!isGenerating) {
                  onGenerateTopic("${topic.promptScenario} ($selectedDurationLabel)")
                }
              },
              shape = RoundedCornerShape(20.dp),
              color = LingoTokens.bgGlassCard,
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 104.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = topic.title,
                  color = LingoTokens.textPrimary,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = topic.subtitle,
                  color = LingoTokens.textSecondary,
                  fontSize = 13.sp,
                  lineHeight = 18.sp,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
          if (rowPair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }

    if (lessons.isNotEmpty()) {
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "Ready to listen",
        color = LingoTokens.textPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(12.dp))
      lessons.forEach { lesson ->
        MoreLessonListItem(
          lesson = lesson,
          isCompleted = false,
          onClick = { onSelectLesson(lesson) }
        )
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}

// =================================================================================================
// 3. CREATE CUSTOM LISTENING SCREEN
// =================================================================================================
@Composable
private fun CreateCustomListeningScreen(
  targetLang: Language,
  selectedCefrLevel: String,
  isGenerating: Boolean,
  onCreateLesson: (category: String, customPrompt: String) -> Unit,
  onBack: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf("Stories") }
  var customTopicText by remember { mutableStateOf("") }
  val categories = listOf("Stories", "Conversations", "Podcasts", "Listening Challenge")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp)
      .padding(bottom = 40.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = TextWhite
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = "Create Listening",
          color = TextWhite,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${targetLang.flagEmoji} ${targetLang.name} • Level $selectedCefrLevel",
          color = CyanAccent,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "1. Choose Format",
      color = TextWhite,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 2x2 grid for format selection
    categories.chunked(2).forEach { pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        pair.forEach { cat ->
          val isSel = selectedCategory == cat
          Surface(
            onClick = { selectedCategory = cat },
            shape = RoundedCornerShape(16.dp),
            color = if (isSel) ElectricBlue else NavyCardBg,
            border = BorderStroke(1.dp, if (isSel) CyanAccent else Color(0xFF1E293B)),
            modifier = Modifier
              .weight(1f)
              .height(54.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.padding(horizontal = 12.dp)
            ) {
              Text(
                text = cat,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "2. Describe Your Topic or Situation",
      color = TextWhite,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = customTopicText,
      onValueChange = { customTopicText = it },
      placeholder = {
        Text(
          text = "e.g., A mystery about a lost violin in Vienna, or a job interview at a design studio...",
          color = TextMuted
        )
      },
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 120.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = NavyCardBg,
        unfocusedContainerColor = NavyCardBg,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = Color(0xFF1E293B),
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite
      )
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = {
        val topic = customTopicText.trim().ifBlank {
          "An engaging $selectedCategory lesson for ${targetLang.name} learners"
        }
        onCreateLesson(selectedCategory, topic)
      },
      enabled = !isGenerating,
      shape = RoundedCornerShape(24.dp),
      colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
    ) {
      if (isGenerating) {
        CircularProgressIndicator(
          color = TextWhite,
          strokeWidth = 2.dp,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("Creating Lesson...", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      } else {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Listening Lesson", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

// =================================================================================================
// 4. DEDICATED FULL-SCREEN AUDIO PLAYER
// =================================================================================================
@Composable
private fun DedicatedAudioPlayerScreen(
  activeItem: ListeningAcademyItem,
  targetLang: Language,
  isPlaying: Boolean,
  currentLineIndex: Int,
  playbackSpeed: Float,
  isMuted: Boolean,
  showSpeedMenu: Boolean,
  showTranslations: Boolean,
  isSaved: Boolean,
  onToggleSave: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekRelative: (Int) -> Unit,
  onPlaySpecificLine: (Int) -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeedMenu: () -> Unit,
  onSelectSpeed: (Float) -> Unit,
  onToggleTranslations: () -> Unit,
  onSelectWord: (String, String) -> Unit,
  onOpenFullTranscript: () -> Unit,
  onOpenAutoScrollTeleprompter: () -> Unit = {},
  onStartQuiz: () -> Unit,
  onNextLesson: () -> Unit,
  onBack: () -> Unit
) {
  val scrollState = rememberScrollState()
  val heroImageRes = getContextualImageResource(activeItem)
  val cleanCategory = formatCleanCategoryName(activeItem)
  val dynamicDescription = buildLessonContextualDescription(activeItem, targetLang)
  var showInlineTranscript by remember { mutableStateOf(true) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .verticalScroll(scrollState)
      .padding(bottom = 40.dp)
  ) {
    // Player Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = TextWhite
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
      ) {
        Text(
          text = "Now Playing",
          color = TextWhite,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${targetLang.flagEmoji} ${targetLang.name} • ${activeItem.cefrLevel}",
          color = CyanAccent,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      IconButton(
        onClick = onToggleSave,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
      ) {
        Icon(
          imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
          contentDescription = "Save to Library",
          tint = if (isSaved) GoldAccent else TextWhite
        )
      }
    }

    // Main Player Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = NavyCardBg),
      border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
      Column {
        // Contextual Hero Image
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
        ) {
          AsyncImage(
            model = heroImageRes,
            contentDescription = activeItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color(0x99000000), NavyCardBg)
                )
              )
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = PurpleAccent.copy(alpha = 0.9f),
            modifier = Modifier
              .padding(14.dp)
              .align(Alignment.TopStart)
          ) {
            Text(
              text = cleanCategory,
              color = TextWhite,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC000000),
            modifier = Modifier
              .padding(14.dp)
              .align(Alignment.TopEnd)
          ) {
            Text(
              text = "${activeItem.lines.size * 20 / 60 + 2} min • ${activeItem.cefrLevel}",
              color = TextWhite,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        // Title, Dynamic Description & Audio Controls
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
          Text(
            text = activeItem.title,
            color = TextWhite,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 27.sp
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = dynamicDescription,
            color = TextLight,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Active line subtitle banner
          val currentLine = activeItem.lines.getOrNull(currentLineIndex)
          if (currentLine != null) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF14244E),
              border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "${currentLine.speaker} (${currentLineIndex + 1}/${activeItem.lines.size})",
                  color = CyanAccent,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = currentLine.targetText,
                  color = TextWhite,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  lineHeight = 20.sp
                )
                if (showTranslations && currentLine.nativeTranslation.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = currentLine.nativeTranslation,
                    color = TextMuted,
                    fontSize = 12.sp
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }

          // Audio Controls Row: [-15s], [Play/Pause], [+15s], [Mute]
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { onSeekRelative(-1) },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
            ) {
              Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Previous sentence",
                tint = TextLight,
                modifier = Modifier.size(24.dp)
              )
            }

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
              initialValue = 1.0f,
              targetValue = if (isPlaying) 1.08f else 1.0f,
              animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
              ),
              label = "pulseScale"
            )

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(72.dp)
            ) {
              if (isPlaying) {
                Box(
                  modifier = Modifier
                    .size(66.dp * pulseScale)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.25f))
                )
              }

              IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                  .size(60.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      colors = listOf(ElectricBlue, CyanAccent)
                    )
                  )
                  .testTag("featured_play_pause_button")
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (isPlaying) "Pause" else "Play",
                  tint = TextWhite,
                  modifier = Modifier.size(34.dp)
                )
              }
            }

            IconButton(
              onClick = { onSeekRelative(1) },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
            ) {
              Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Next sentence",
                tint = TextLight,
                modifier = Modifier.size(24.dp)
              )
            }

            IconButton(
              onClick = onToggleMute,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
            ) {
              Icon(
                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Volume",
                tint = if (isMuted) CoralAccent else TextLight,
                modifier = Modifier.size(22.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Progress Slider & Timestamps
          val totalLines = activeItem.lines.size.coerceAtLeast(1)
          val progressFraction = (currentLineIndex.toFloat() / totalLines.toFloat()).coerceIn(0f, 1f)
          val elapsedFormatted = formatAudioTime(currentLineIndex * 22)
          val totalFormatted = formatAudioTime(totalLines * 22)

          Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
              value = progressFraction,
              onValueChange = {},
              colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = Color(0xFF1E293B)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = elapsedFormatted, color = TextMuted, fontSize = 11.sp)
              Text(text = totalFormatted, color = TextMuted, fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Sub-controls: [Transcript] and [1.0x Speed] with proper PaddingValues & single-line text
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onOpenFullTranscript,
              shape = RoundedCornerShape(16.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF1E293B),
                contentColor = TextWhite
              ),
              border = BorderStroke(1.dp, Color(0xFF334155)),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("transcript_toggle_button")
            ) {
              Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Transcript",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Box(modifier = Modifier.weight(1f)) {
              OutlinedButton(
                onClick = onToggleSpeedMenu,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = Color(0xFF1E293B),
                  contentColor = TextWhite
                ),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Speed,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${playbackSpeed}x Speed",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(14.dp)
                )
              }

              DropdownMenu(
                expanded = showSpeedMenu,
                onDismissRequest = onToggleSpeedMenu,
                modifier = Modifier.background(NavyCardElevated)
              ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speedOption ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = "${speedOption}x Normal",
                        color = if (playbackSpeed == speedOption) CyanAccent else TextWhite,
                        fontWeight = if (playbackSpeed == speedOption) FontWeight.Bold else FontWeight.Normal
                      )
                    },
                    onClick = { onSelectSpeed(speedOption) }
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedButton(
            onClick = onOpenAutoScrollTeleprompter,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFF0E2A38),
              contentColor = CyanAccent
            ),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.55f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("player_open_auto_scroll_btn")
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = CyanAccent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Open Full-Screen Auto-Scroll Teleprompter",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Primary Quiz & Next Lesson Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onStartQuiz,
              shape = RoundedCornerShape(20.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
              modifier = Modifier
                .weight(1.4f)
                .height(52.dp)
                .testTag("start_listening_quiz_button")
            ) {
              Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Listening Quiz",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            OutlinedButton(
              onClick = onNextLesson,
              shape = RoundedCornerShape(20.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF14244E),
                contentColor = TextWhite
              ),
              border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Next Lesson",
                color = TextWhite,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Expandable Synchronized Transcript & Vocabulary Section right below Player
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Interactive Transcript",
          color = TextWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Translation",
            color = TextMuted,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.width(6.dp))
          Switch(
            checked = showTranslations,
            onCheckedChange = { onToggleTranslations() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = TextWhite,
              checkedTrackColor = CyanAccent,
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = Color(0xFF1E293B)
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      AnimatedVisibility(visible = showInlineTranscript) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          activeItem.lines.forEachIndexed { idx, line ->
            val isLineActive = currentLineIndex == idx && isPlaying
            Card(
              onClick = { onPlaySpecificLine(idx) },
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isLineActive) Color(0xFF162A56) else NavyCardBg
              ),
              border = BorderStroke(
                width = if (isLineActive) 1.5.dp else 1.dp,
                color = if (isLineActive) CyanAccent else Color(0xFF1E293B)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = line.speaker,
                    color = if (idx % 2 == 0) CyanAccent else GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Tap to hear",
                    color = TextMuted,
                    fontSize = 11.sp
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = line.targetText,
                  color = TextWhite,
                  fontSize = 15.sp,
                  fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.Normal,
                  lineHeight = 21.sp
                )
                if (showTranslations && line.nativeTranslation.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = line.nativeTranslation,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                  )
                }
                if (line.keyVocabWord.isNotBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Surface(
                    onClick = { onSelectWord(line.keyVocabWord, line.keyVocabMeaning) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(12.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "${line.keyVocabWord}: ${line.keyVocabMeaning}",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
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
  }
}

// =================================================================================================
// MORE LESSONS LIST ITEM
// =================================================================================================
@Composable
private fun MoreLessonListItem(
  lesson: ListeningAcademyItem,
  isCompleted: Boolean = false,
  onClick: () -> Unit
) {
  val imageRes = getContextualImageResource(lesson)
  val cleanCat = formatCleanCategoryName(lesson)

  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = NavyCardBg),
    border = BorderStroke(1.dp, Color(0xFF1E293B))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = imageRes,
        contentDescription = lesson.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(12.dp))
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = lesson.title,
          color = TextWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "$cleanCat • ${lesson.lines.size * 20 / 60 + 2} min • ${lesson.cefrLevel}",
          color = TextMuted,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      if (isCompleted) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Completed",
          tint = EmeraldAccent,
          modifier = Modifier
            .padding(end = 6.dp)
            .size(20.dp)
        )
      }

      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(ElectricBlue.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play",
          tint = CyanAccent,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

// =================================================================================================
// SYNCHRONIZED TRANSCRIPT VIEW
// =================================================================================================
@Composable
private fun ListeningTranscriptContent(
  userProfile: UserProfile,
  activeItem: ListeningAcademyItem,
  targetLang: Language,
  selectedCefrLevel: String,
  activeTab: TranscriptTab,
  showTranslations: Boolean,
  isPlaying: Boolean,
  currentLineIndex: Int,
  playbackSpeed: Float,
  onSelectTab: (TranscriptTab) -> Unit,
  onToggleTranslations: () -> Unit,
  onPlayLine: (Int) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSelectWord: (String, String) -> Unit,
  onStartQuiz: () -> Unit,
  onOpenSubscription: () -> Unit,
  onBack: () -> Unit
) {
  val scrollState = rememberScrollState()
  val heroImageRes = getContextualImageResource(activeItem)
  val cleanCat = formatCleanCategoryName(activeItem)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextWhite
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Lesson Transcript",
          color = TextWhite,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
      ) {
        Text(
          text = "${targetLang.flagEmoji} ${activeItem.cefrLevel}",
          color = CyanAccent,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }
    }

    // Mini Lesson Header Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = NavyCardBg),
      border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AsyncImage(
          model = heroImageRes,
          contentDescription = activeItem.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PurpleAccent.copy(alpha = 0.8f)
          ) {
            Text(
              text = cleanCat,
              color = TextWhite,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = activeItem.title,
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${targetLang.name} • ${activeItem.cefrLevel}",
            color = TextMuted,
            fontSize = 12.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Segmented Tabs: [ Transcript ] [ Vocabulary ] [ Notes ]
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      TranscriptTab.values().forEach { tab ->
        val isSelected = activeTab == tab
        Surface(
          onClick = { onSelectTab(tab) },
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) ElectricBlue else Color(0xFF1E293B),
          border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = when (tab) {
                TranscriptTab.TRANSCRIPT -> "Transcript"
                TranscriptTab.VOCABULARY -> "Vocabulary"
                TranscriptTab.NOTES -> "Notes"
              },
              color = if (isSelected) TextWhite else TextLight,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      when (activeTab) {
        TranscriptTab.TRANSCRIPT -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 20.dp)
              .padding(bottom = 108.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Show Translation",
                color = TextLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
              Switch(
                checked = showTranslations,
                onCheckedChange = { onToggleTranslations() },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = TextWhite,
                  checkedTrackColor = CyanAccent,
                  uncheckedThumbColor = TextMuted,
                  uncheckedTrackColor = Color(0xFF1E293B)
                )
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            activeItem.lines.forEachIndexed { index, line ->
              val isLineActive = currentLineIndex == index && isPlaying
              val speakerColor = if (index % 2 == 0) PurpleAccent else ElectricBlue

              Card(
                onClick = { onPlayLine(index) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isLineActive) Color(0xFF162A56) else NavyCardBg
                ),
                border = BorderStroke(
                  width = if (isLineActive) 1.5.dp else 1.dp,
                  color = if (isLineActive) CyanAccent else Color(0xFF1E293B)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(28.dp)
                          .clip(CircleShape)
                          .background(speakerColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = line.speaker.take(1).uppercase(),
                          color = speakerColor,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = line.speaker,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (isLineActive) {
                        Icon(
                          imageVector = Icons.Default.GraphicEq,
                          contentDescription = null,
                          tint = CyanAccent,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                      }
                      Text(
                        text = formatAudioTime(index * 12),
                        color = if (isLineActive) CyanAccent else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Text(
                    text = line.targetText,
                    color = if (isLineActive) CyanAccent else TextWhite,
                    fontSize = 15.sp,
                    fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 22.sp
                  )

                  if (showTranslations && line.nativeTranslation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = line.nativeTranslation,
                      color = TextMuted,
                      fontSize = 13.sp,
                      lineHeight = 18.sp
                    )
                  }

                  if (line.keyVocabWord.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                      onClick = { onSelectWord(line.keyVocabWord, line.keyVocabMeaning) },
                      shape = RoundedCornerShape(10.dp),
                      color = Color(0xFF1E293B),
                      border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.Star,
                          contentDescription = null,
                          tint = GoldAccent,
                          modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "${line.keyVocabWord}: ${line.keyVocabMeaning}",
                          color = CyanAccent,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Medium
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        TranscriptTab.VOCABULARY -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 20.dp)
              .padding(bottom = 108.dp)
          ) {
            val keyWords = activeItem.lines.filter { it.keyVocabWord.isNotBlank() }
            if (keyWords.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 40.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "All key expressions are integrated into the transcript dialogue.",
                  color = TextMuted,
                  fontSize = 14.sp,
                  textAlign = TextAlign.Center
                )
              }
            } else {
              keyWords.forEach { line ->
                Card(
                  onClick = { onSelectWord(line.keyVocabWord, line.keyVocabMeaning) },
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = NavyCardBg),
                  border = BorderStroke(1.dp, Color(0xFF1E293B)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = line.keyVocabWord,
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = line.keyVocabMeaning,
                        color = CyanAccent,
                        fontSize = 13.sp
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "\"${line.targetText}\"",
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    IconButton(
                      onClick = { onSelectWord(line.keyVocabWord, line.keyVocabMeaning) }
                    ) {
                      Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save word",
                        tint = GoldAccent
                      )
                    }
                  }
                }
              }
            }
          }
        }

        TranscriptTab.NOTES -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 20.dp)
              .padding(bottom = 108.dp)
          ) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, Color(0xFF1E293B)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Regional Accents & Variation",
                  color = TextWhite,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                val accents = activeItem.regionalAccents.ifEmpty {
                  listOf("Standard Native Accent", "Conversational Cadence")
                }
                accents.forEach { accent ->
                  Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = "•", color = CyanAccent, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = accent, color = TextLight, fontSize = 13.sp)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, Color(0xFF1E293B)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Listening & Comprehension Focus",
                  color = TextWhite,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = buildLessonContextualDescription(activeItem, targetLang),
                  color = TextLight,
                  fontSize = 13.sp,
                  lineHeight = 19.sp
                )
              }
            }
          }
        }
      }

      // Bottom Sticky Mini Player Bar & Start Quiz
      Surface(
        color = NavyCardElevated,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(ElectricBlue)
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = TextWhite,
              modifier = Modifier.size(26.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isPlaying) "Speaking: ${activeItem.lines.getOrNull(currentLineIndex)?.speaker ?: "Speaker"}" else "Tap to Play Audio",
              color = TextWhite,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${currentLineIndex + 1} of ${activeItem.lines.size} sentences",
              color = TextMuted,
              fontSize = 11.sp
            )
          }

          Button(
            onClick = onStartQuiz,
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.height(42.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Headphones,
              contentDescription = null,
              tint = NavyBackground,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Take Quiz",
              color = NavyBackground,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

// =================================================================================================
// LISTENING QUIZ & IMMEDIATE FEEDBACK
// =================================================================================================
@Composable
private fun ListeningQuizContent(
  userProfile: UserProfile,
  activeItem: ListeningAcademyItem,
  targetLang: Language,
  selectedCefrLevel: String,
  questions: List<AcademyComprehensionQuestion>,
  currentQuestionIndex: Int,
  selectedOptionIndex: Int?,
  isEvaluated: Boolean,
  isPlayingAudio: Boolean,
  speechManager: SpeechManager,
  onPlayPromptAudio: () -> Unit,
  onSelectOption: (Int) -> Unit,
  onCheckAnswer: () -> Unit,
  onNextQuestion: () -> Unit,
  onBack: () -> Unit
) {
  val currentQuestion = questions.getOrNull(currentQuestionIndex)
    ?: createFallbackQuestion()
  val scrollState = rememberScrollState()
  val isCorrect = isEvaluated && selectedOptionIndex == currentQuestion.correctIndex

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .verticalScroll(scrollState)
      .padding(bottom = 36.dp)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .weight(1f, fill = false)
          .padding(end = 8.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextWhite
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Listening Quiz",
          color = TextWhite,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
      ) {
        Text(
          text = activeItem.cefrLevel,
          color = CyanAccent,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }
    }

    // Progress Bar
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
          color = TextLight,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "${currentQuestionIndex + 1} / ${questions.size}",
          color = CyanAccent,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      val progress = ((currentQuestionIndex + 1).toFloat() / questions.size.toFloat()).coerceIn(0f, 1f)
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(CircleShape),
        color = CyanAccent,
        trackColor = Color(0xFF1E293B)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Audio Prompt Mini-Player Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = NavyCardBg),
      border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Replay the audio anytime and choose the best answer.",
            color = TextLight,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF16254F))
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onPlayPromptAudio,
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(ElectricBlue)
          ) {
            Icon(
              imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Play prompt audio",
              tint = TextWhite,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeItem.title,
              color = TextWhite,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${activeItem.lines.size} sentences • ${targetLang.name}",
              color = TextMuted,
              fontSize = 11.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Question Text & Radio Options
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
      Text(
        text = "${currentQuestionIndex + 1}. ${currentQuestion.question}",
        color = TextWhite,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      val optionLetters = listOf("A", "B", "C", "D")
      currentQuestion.options.forEachIndexed { idx, optionText ->
        val isSelected = selectedOptionIndex == idx
        val isThisCorrect = isEvaluated && idx == currentQuestion.correctIndex
        val isThisWrong = isEvaluated && isSelected && !isCorrect

        val borderColor by animateColorAsState(
          targetValue = when {
            isThisCorrect -> EmeraldAccent
            isThisWrong -> CoralAccent
            isSelected -> CyanAccent
            else -> Color(0xFF1E293B)
          },
          label = "optionBorder"
        )

        val cardBgColor = when {
          isThisCorrect -> Color(0xFF063324)
          isThisWrong -> Color(0xFF3B1219)
          isSelected -> Color(0xFF122452)
          else -> NavyCardBg
        }

        Card(
          onClick = { onSelectOption(idx) },
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = cardBgColor),
          border = BorderStroke(if (isSelected || isThisCorrect) 1.5.dp else 1.dp, borderColor),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isThisCorrect -> EmeraldAccent
                    isThisWrong -> CoralAccent
                    isSelected -> CyanAccent
                    else -> Color(0xFF1E293B)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              when {
                isThisCorrect -> Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = TextWhite,
                  modifier = Modifier.size(14.dp)
                )
                isThisWrong -> Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  tint = TextWhite,
                  modifier = Modifier.size(14.dp)
                )
                isSelected -> Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(NavyBackground)
                )
                else -> Box(
                  modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(NavyCardBg)
                )
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            val letterPrefix = optionLetters.getOrNull(idx) ?: "${idx + 1}"
            Text(
              text = "$letterPrefix. $optionText",
              color = when {
                isThisCorrect -> EmeraldAccent
                isThisWrong -> CoralAccent
                isSelected -> TextWhite
                else -> TextLight
              },
              fontSize = 15.sp,
              fontWeight = if (isSelected || isThisCorrect) FontWeight.SemiBold else FontWeight.Normal
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (isEvaluated) {
      Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFF063324) else Color(0xFF3B1219)
          ),
          border = BorderStroke(
            1.dp,
            if (isCorrect) EmeraldAccent.copy(alpha = 0.5f) else CoralAccent.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (isCorrect) "Correct!" else "Not quite!",
              color = TextWhite,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = currentQuestion.explanation.ifBlank {
                "The speakers clarified this detail in the audio lesson."
              },
              color = TextLight,
              fontSize = 13.5.sp,
              textAlign = TextAlign.Center,
              lineHeight = 19.sp
            )
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }

    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
      if (!isEvaluated) {
        Button(
          onClick = onCheckAnswer,
          enabled = selectedOptionIndex != null,
          shape = RoundedCornerShape(24.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ElectricBlue,
            disabledContainerColor = Color(0xFF1E293B)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
        ) {
          Text(
            text = "Check Answer",
            color = if (selectedOptionIndex != null) TextWhite else TextMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      } else {
        val isLastQuestion = currentQuestionIndex + 1 >= questions.size
        Button(
          onClick = onNextQuestion,
          shape = RoundedCornerShape(24.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
        ) {
          Text(
            text = if (isLastQuestion) "See Results" else "Next Question",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

// =================================================================================================
// QUIZ RESULTS VIEW
// =================================================================================================
@Composable
private fun ListeningResultsContent(
  userProfile: UserProfile,
  activeItem: ListeningAcademyItem,
  selectedCefrLevel: String,
  totalQuestions: Int,
  correctAnswersCount: Int,
  onNextLesson: () -> Unit,
  onReviewTranscript: () -> Unit,
  onBack: () -> Unit
) {
  val scorePercentage = ((correctAnswersCount.toFloat() / totalQuestions.toFloat()) * 100).toInt()
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 36.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = TextWhite
        )
      }

      Text(
        text = "Quiz Results",
        color = TextWhite,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
      )

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
      ) {
        Text(
          text = activeItem.cefrLevel,
          color = CyanAccent,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Box(
      modifier = Modifier
        .size(92.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            colors = listOf(GoldAccent, Color(0xFFD97706))
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.WorkspacePremium,
        contentDescription = "Trophy",
        tint = TextWhite,
        modifier = Modifier.size(50.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = if (scorePercentage >= 70) "Great Job!" else "Keep Practicing!",
      color = TextWhite,
      fontSize = 26.sp,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "You completed ${activeItem.title}.",
      color = TextMuted,
      fontSize = 14.sp,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = NavyCardBg),
      border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(Color(0xFF14244E)),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              progress = { scorePercentage.toFloat() / 100f },
              modifier = Modifier.fillMaxSize(),
              color = CyanAccent,
              strokeWidth = 6.dp,
              trackColor = Color(0xFF1E293B)
            )
            Text(
              text = "$scorePercentage%",
              color = TextWhite,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.width(18.dp))

          Column {
            Text(
              text = "Correct Answers",
              color = TextMuted,
              fontSize = 13.sp
            )
            Text(
              text = "$correctAnswersCount / $totalQuestions",
              color = TextWhite,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(26.dp))

    Button(
      onClick = onNextLesson,
      shape = RoundedCornerShape(24.dp),
      colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
    ) {
      Text(
        text = "Next Lesson",
        color = TextWhite,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
      onClick = onReviewTranscript,
      shape = RoundedCornerShape(24.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
      border = BorderStroke(1.dp, Color(0xFF334155)),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
    ) {
      Text(
        text = "Review Transcript",
        color = TextLight,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

// =================================================================================================
// 5. DEDICATED WATCH & LEARN VIDEO SCREEN + FULL VIDEO LESSON PLAYER
// =================================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchAndLearnVideoScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  targetLang: Language,
  nativeLang: Language,
  selectedCefrLevel: String,
  onSaveVocabToFlashcard: (String, String, String) -> Unit,
  onAddXp: (Long) -> Unit,
  onCompleteLesson: (String) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Recommended") }
  var videoResults by remember { mutableStateOf<List<LingoVideo>>(emptyList()) }
  var isLoadingVideos by remember { mutableStateOf(false) }
  var selectedVideoForLesson by remember { mutableStateOf<LingoVideo?>(null) }
  val savedVideoIds = remember { mutableStateListOf<String>() }

  fun loadVideos(query: String = "") {
    isLoadingVideos = true
    scope.launch {
      val mappedTopic = when (selectedCategory) {
        "Recommended" -> "Educational"
        "Short Stories" -> "Stories"
        "Beginner" -> "Beginner A1 A2"
        "Intermediate" -> "Intermediate B1 B2"
        "Advanced" -> "Advanced C1"
        else -> selectedCategory
      }
      val res = YouTubeVideoService.discoverVideos(
        languageCode = targetLang.code,
        proficiencyLevel = selectedCefrLevel,
        topicCategory = mappedTopic,
        searchQuery = query
      )
      videoResults = res.videos
      isLoadingVideos = false
    }
  }

  LaunchedEffect(targetLang.code, selectedCefrLevel, selectedCategory) {
    loadVideos(searchQuery)
  }

  if (selectedVideoForLesson != null) {
    val video = selectedVideoForLesson!!
    DedicatedVideoLessonView(
      video = video,
      targetLang = targetLang,
      nativeLang = nativeLang,
      speechManager = speechManager,
      isSaved = savedVideoIds.contains(video.videoId),
      onToggleSave = {
        if (savedVideoIds.contains(video.videoId)) {
          savedVideoIds.remove(video.videoId)
          Toast.makeText(context, "Removed from Saved Videos", Toast.LENGTH_SHORT).show()
        } else {
          savedVideoIds.add(video.videoId)
          Toast.makeText(context, "Saved to Learning List", Toast.LENGTH_SHORT).show()
        }
      },
      onSaveVocabToFlashcard = onSaveVocabToFlashcard,
      onCompleteVideoQuiz = {
        onAddXp(20L)
        onCompleteLesson("video_${video.videoId}")
      },
      onBack = { selectedVideoForLesson = null }
    )
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .background(NavyBackground)
      .verticalScroll(rememberScrollState())
      .padding(bottom = 40.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Watch & Learn",
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Learn with Videos • ${targetLang.flagEmoji} ${targetLang.name} ($selectedCefrLevel)",
            color = CyanAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // Search bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            text = "Search ${targetLang.name} videos...",
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .height(54.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = NavyCardBg,
          unfocusedContainerColor = NavyCardBg,
          focusedBorderColor = ElectricBlue,
          unfocusedBorderColor = Color(0xFF1E293B),
          focusedTextColor = TextWhite,
          unfocusedTextColor = TextWhite
        )
      )
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = { loadVideos(searchQuery) },
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier.height(54.dp)
      ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextWhite)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Search", fontWeight = FontWeight.Bold, maxLines = 1)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Categories row
    val cats = listOf(
      "Recommended",
      "Conversations",
      "Short Stories",
      "Interviews",
      "Daily Life",
      "Podcasts",
      "Beginner",
      "Intermediate",
      "Advanced"
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      cats.forEach { cat ->
        val selected = selectedCategory == cat
        Surface(
          onClick = { selectedCategory = cat },
          shape = RoundedCornerShape(20.dp),
          color = if (selected) ElectricBlue else NavyCardBg,
          border = BorderStroke(1.dp, if (selected) CyanAccent else Color(0xFF1E293B))
        ) {
          Text(
            text = cat,
            color = if (selected) TextWhite else TextLight,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (isLoadingVideos) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = CyanAccent)
      }
    } else if (videoResults.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No videos found for this filter. Try another category or search term.",
          color = TextMuted,
          textAlign = TextAlign.Center
        )
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        videoResults.forEach { video ->
          Card(
            onClick = { selectedVideoForLesson = video },
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column {
              // Video Thumbnail with Duration & Level Badges
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(175.dp)
                  .background(NavyCardElevated)
              ) {
                if (video.thumbnailUrl.isNotBlank()) {
                  AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                } else {
                  AsyncImage(
                    model = R.drawable.img_listening_videos,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                }

                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xAA06102B))
                      )
                    )
                )

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = ElectricBlue.copy(alpha = 0.9f),
                  modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                ) {
                  Text(
                    text = "${targetLang.name} • ${video.proficiencyLevel.ifBlank { selectedCefrLevel }}",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xCC000000),
                  modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomEnd)
                ) {
                  Text(
                    text = video.durationText.ifBlank { "5:30" },
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }

              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = video.title,
                  color = TextWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = video.channelTitle.ifBlank { "Educational Creator" },
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ElectricBlue
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Watch & Learn",
                        color = TextWhite,
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
      }
    }
  }
}

@Composable
private fun DedicatedVideoLessonView(
  video: LingoVideo,
  targetLang: Language,
  nativeLang: Language,
  speechManager: SpeechManager,
  isSaved: Boolean,
  onToggleSave: () -> Unit,
  onSaveVocabToFlashcard: (String, String, String) -> Unit,
  onCompleteVideoQuiz: () -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showTranscript by remember { mutableStateOf(true) }
  var studyPack by remember(video.videoId) { mutableStateOf<VideoAiStudyPack?>(null) }
  var isLoadingPack by remember(video.videoId) { mutableStateOf(false) }
  var selectedQuizAnswers by remember(video.videoId) { mutableStateOf<Map<Int, Int>>(emptyMap()) }

  BackHandler { onBack() }

  LaunchedEffect(video.videoId) {
    isLoadingPack = true
    try {
      val tracks = YouTubeVideoService.fetchAuthorizedCaptionTracks(video)
      studyPack = VideoAiAssistantService.generateVideoStudyPack(
        video = video,
        targetLanguage = targetLang,
        nativeLanguage = nativeLang,
        authorizedCaptionTracks = tracks
      )
    } catch (_: Exception) {
    } finally {
      isLoadingPack = false
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .background(NavyBackground)
      .verticalScroll(rememberScrollState())
      .padding(bottom = 40.dp)
  ) {
    // Top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Video Lesson",
          color = TextWhite,
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      OutlinedButton(
        onClick = onToggleSave,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = NavyCardBg,
          contentColor = TextWhite
        ),
        border = BorderStroke(1.dp, if (isSaved) GoldAccent else Color(0xFF334155))
      ) {
        Icon(
          imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
          contentDescription = null,
          tint = if (isSaved) GoldAccent else CyanAccent,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isSaved) "Saved" else "Save to List",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1
        )
      }
    }

    // Embedded Video Player
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(18.dp))
    ) {
      YouTubeEmbeddedPlayer(
        video = video,
        speechManager = speechManager
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Video Title, Creator Attribution & Controls
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
      Text(
        text = video.title,
        color = TextWhite,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 25.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${video.channelTitle.ifBlank { "Educational Creator" }} • ${targetLang.name} (${video.proficiencyLevel.ifBlank { "B1" }})",
        color = CyanAccent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Show / Hide Transcript Toggle Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = { showTranscript = !showTranscript },
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NavyCardBg,
            contentColor = TextWhite
          ),
          border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Subtitles,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (showTranscript) "Hide Transcript & Captions" else "Show Transcript & Captions",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      AnimatedVisibility(visible = showTranscript) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavyCardBg),
          border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Transcript & Lesson Summary",
              color = CyanAccent,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            val transcriptText = video.captionSummary.ifBlank {
              video.description.ifBlank {
                "Use the CC button inside the video player to view synchronized captions while listening."
              }
            }
            Text(
              text = transcriptText,
              color = TextLight,
              fontSize = 14.sp,
              lineHeight = 20.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Vocabulary & Comprehension Exercises from VideoAiAssistantService
      if (isLoadingPack) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavyCardBg)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(
              color = CyanAccent,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Preparing vocabulary & comprehension questions...",
              color = TextLight,
              fontSize = 13.sp
            )
          }
        }
      } else if (studyPack != null) {
        val pack = studyPack!!

        if (pack.vocabulary.isNotEmpty()) {
          Text(
            text = "Key Vocabulary from Video",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(10.dp))

          pack.vocabulary.take(4).forEach { vocab ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = vocab.term,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = vocab.translation,
                    color = CyanAccent,
                    fontSize = 13.sp
                  )
                }
                IconButton(
                  onClick = {
                    speechManager.speak(vocab.term, targetLang.code, 0.92f)
                  }
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen",
                    tint = CyanAccent
                  )
                }
                IconButton(
                  onClick = {
                    onSaveVocabToFlashcard(vocab.term, vocab.translation, vocab.phonetic)
                    Toast.makeText(context, "Saved '${vocab.term}' to Flashcards", Toast.LENGTH_SHORT).show()
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Save to Flashcards",
                    tint = GoldAccent
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
        }

        if (pack.quizQuestions.isNotEmpty()) {
          Text(
            text = "Video Comprehension Quiz",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(10.dp))

          pack.quizQuestions.forEachIndexed { qIdx, q ->
            val selectedOpt = selectedQuizAnswers[qIdx]
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "${qIdx + 1}. ${q.question}",
                  color = TextWhite,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                q.options.forEachIndexed { optIdx, optText ->
                  val isPicked = selectedOpt == optIdx
                  val isCorrectOpt = selectedOpt != null && optIdx == q.correctIndex
                  val isWrongPick = isPicked && optIdx != q.correctIndex
                  Surface(
                    onClick = {
                      if (selectedOpt == null) {
                        val updated = selectedQuizAnswers + (qIdx to optIdx)
                        selectedQuizAnswers = updated
                        if (updated.size == pack.quizQuestions.size) {
                          onCompleteVideoQuiz()
                        }
                      }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                      isCorrectOpt -> Color(0xFF063324)
                      isWrongPick -> Color(0xFF3B1219)
                      else -> Color(0xFF14244E)
                    },
                    border = BorderStroke(
                      1.dp,
                      when {
                        isCorrectOpt -> EmeraldAccent
                        isWrongPick -> CoralAccent
                        else -> Color(0xFF334155)
                      }
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp)
                  ) {
                    Text(
                      text = optText,
                      color = TextWhite,
                      fontSize = 13.5.sp,
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                  }
                }
                if (selectedOpt != null && q.explanation.isNotBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = q.explanation,
                    color = TextMuted,
                    fontSize = 12.sp
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

// =================================================================================================
// HELPERS, CONTENT MATCHING, & MULTI-CATEGORY SEEDING
// =================================================================================================
private fun formatAudioTime(seconds: Int): String {
  val mins = seconds / 60
  val secs = seconds % 60
  return String.format("%02d:%02d", mins, secs)
}

private fun isLessonInCategory(item: ListeningAcademyItem, targetCategory: String): Boolean {
  val clean = formatCleanCategoryName(item)
  return clean.equals(targetCategory, ignoreCase = true)
}

private fun formatCleanCategoryName(item: ListeningAcademyItem): String {
  val title = item.title.lowercase()
  val cat = item.category.lowercase()
  return when {
    title.contains("gemälde") || title.contains("painting") || title.contains("mystery") ||
      title.contains("misterio") || title.contains("mystère") || title.contains("fall") ||
      title.contains("story") || title.contains("cuento") || title.contains("histoire") ||
      cat.contains("story") || cat.contains("stories") -> "Stories"
    title.contains("podcast") || title.contains("science") || title.contains("historic") ||
      title.contains("hybrid work") || title.contains("teletrabajo") || title.contains("tradition") ||
      cat.contains("podcast") || cat.contains("cultural") -> "Podcasts"
    cat.contains("challenge") -> "Listening Challenge"
    else -> "Conversations"
  }
}

private fun getContextualImageResource(item: ListeningAcademyItem): Int {
  val title = item.title.lowercase()
  val category = item.category.lowercase()
  return when {
    title.contains("gemälde") || title.contains("painting") || title.contains("mystery") ||
      title.contains("misterio") || title.contains("mystère") || title.contains("museum") ||
      title.contains("story") || title.contains("cuento") || title.contains("histoire") ||
      title.contains("fall für") || category.contains("stories") || category.contains("story") -> {
      R.drawable.img_listening_stories
    }
    title.contains("podcast") || title.contains("science") || title.contains("culture") ||
      title.contains("historic") || title.contains("hybrid") || title.contains("future") ||
      category.contains("podcast") -> {
      R.drawable.img_listening_podcast
    }
    title.contains("airport") || title.contains("train") || title.contains("ticket") ||
      title.contains("travel") || title.contains("hotel") || title.contains("gare") ||
      title.contains("bahnhof") || title.contains("tren") || title.contains("ferry") ||
      category.contains("travel") -> {
      R.drawable.img_listening_airport
    }
    title.contains("interview") || title.contains("work") || title.contains("job") ||
      title.contains("office") || title.contains("project") || title.contains("reunión") ||
      title.contains("challenge") || category.contains("challenge") -> {
      R.drawable.img_listening_interview
    }
    title.contains("restaurant") || title.contains("dinner") || title.contains("food") ||
      title.contains("menu") -> {
      R.drawable.img_listening_restaurant
    }
    else -> R.drawable.img_listening_coffee_shop
  }
}

private fun buildLessonContextualDescription(item: ListeningAcademyItem, targetLang: Language): String {
  val cleanCat = formatCleanCategoryName(item)
  val speakers = item.lines.map { it.speaker }.distinct().joinToString(" & ")
  val firstTranslation = item.lines.firstOrNull()?.nativeTranslation?.takeIf { it.isNotBlank() }
  return when (cleanCat) {
    "Stories" -> {
      if (firstTranslation != null) {
        "Story in ${targetLang.name} ($speakers): \"$firstTranslation\""
      } else {
        "Listen to this ${targetLang.name} story with $speakers and follow the plot clues."
      }
    }
    "Podcasts" -> {
      if (firstTranslation != null) {
        "Podcast discussion in ${targetLang.name}: \"$firstTranslation\""
      } else {
        "Explore ideas and cultural perspectives in ${targetLang.name} with $speakers."
      }
    }
    else -> {
      if (firstTranslation != null) {
        "Dialogue between $speakers: \"$firstTranslation\""
      } else {
        "Practical ${targetLang.name} conversation between $speakers."
      }
    }
  }
}

private fun normalizeLessonMetadata(
  item: ListeningAcademyItem,
  targetLang: Language
): ListeningAcademyItem {
  val normalizedCategory = formatCleanCategoryName(item)
  return item.copy(
    languageCode = targetLang.code,
    category = normalizedCategory
  )
}

private fun buildCompleteListeningCatalog(
  targetLang: Language,
  defaultCefr: String,
  cachedItems: List<ListeningAcademyItem>
): List<ListeningAcademyItem> {
  val curriculumItems = CurriculumData.getListeningAcademyItems(targetLang.code)
    .filter { it.languageCode.equals(targetLang.code, ignoreCase = true) }
    .map { normalizeLessonMetadata(it, targetLang) }

  val seededCategoryItems = createCategorySeedLessons(targetLang, defaultCefr)
  return (cachedItems + seededCategoryItems + curriculumItems).distinctBy { it.id }
}

private fun createCategorySeedLessons(
  targetLang: Language,
  cefr: String
): List<ListeningAcademyItem> {
  val code = targetLang.code.lowercase()
  return when (code) {
    "de" -> listOf(
      ListeningAcademyItem(
        id = "listen_de_story_painting",
        languageCode = "de",
        cefrLevel = cefr,
        category = "Stories",
        title = "Das verschwundene Gemälde: Ein Fall für zwei",
        regionalAccents = listOf("Germany (Berlin)", "Austria (Vienna)", "Switzerland (Zurich)"),
        lines = listOf(
          ListeningDialogueLine(
            "Kommissarin Weber",
            "Sehen Sie sich diesen leeren Rahmen im Nordflügel der Galerie genau an, Lukas.",
            "Take a close look at this empty frame in the north wing of the gallery, Lukas.",
            "Rahmen",
            "frame (picture frame)"
          ),
          ListeningDialogueLine(
            "Lukas",
            "Merkwürdig! Der Alarm wurde gestern Abend um zweiundzwanzig Uhr nicht ausgelöst.",
            "Strange! The alarm was not triggered last night at ten o'clock.",
            "ausgelöst",
            "triggered / set off"
          ),
          ListeningDialogueLine(
            "Kommissarin Weber",
            "Aber hier auf dem Parkett liegt eine Eintrittskarte für das heutige Kammerkonzert.",
            "But here on the parquet floor lies a ticket for today's chamber concert.",
            "Eintrittskarte",
            "admission ticket"
          ),
          ListeningDialogueLine(
            "Lukas",
            "Dann kennen wir unseren nächsten Schritt: Wir befragen den Restaurator vor Konzertbeginn!",
            "Then we know our next step: We question the restorer before the concert begins!",
            "befragen",
            "to question / interview"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "Was wurde aus dem Nordflügel der Galerie gestohlen?",
            options = listOf(
              "Ein wertvolles Gemälde (nur der leere Rahmen blieb zurück)",
              "Eine goldene Uhr",
              "Ein Musikinstrument",
              "Ein historisches Buch"
            ),
            correctIndex = 0,
            explanation = "Kommissarin Weber zeigt auf den leeren Rahmen im Nordflügel der Galerie."
          ),
          AcademyComprehensionQuestion(
            question = "Welchen Hinweis finden die beiden Ermittler auf dem Boden?",
            options = listOf(
              "Eine Eintrittskarte für das heutige Kammerkonzert",
              "Einen verlorenen Autoschlüssel",
              "Einen Kassenbon aus einem Café",
              "Einen Regenschirm"
            ),
            correctIndex = 0,
            explanation = "Auf dem Parkett liegt eine Eintrittskarte für das heutige Kammerkonzert."
          )
        )
      ),
      ListeningAcademyItem(
        id = "listen_de_conv_cafe",
        languageCode = "de",
        cefrLevel = cefr,
        category = "Conversations",
        title = "Frühstück in einem Berliner Café",
        regionalAccents = listOf("Germany (Berlin)", "Germany (Munich)"),
        lines = listOf(
          ListeningDialogueLine(
            "Kellner",
            "Guten Morgen! Was darf ich Ihnen heute zum Frühstück bringen?",
            "Good morning! What may I bring you for breakfast today?",
            "Frühstück",
            "breakfast"
          ),
          ListeningDialogueLine(
            "Gast",
            "Ich hätte gern einen Cappuccino mit Hafermilch und ein warmes Franzbrötchen.",
            "I would like a cappuccino with oat milk and a warm cinnamon pastry.",
            "Hafermilch",
            "oat milk"
          ),
          ListeningDialogueLine(
            "Kellner",
            "Sehr gern! Möchten Sie drinnen sitzen oder draußen auf der Sonnenterrasse?",
            "Gladly! Would you like to sit inside or outside on the sunny terrace?",
            "Sonnenterrasse",
            "sunny terrace"
          ),
          ListeningDialogueLine(
            "Gast",
            "Draußen auf der Terrasse, bitte. Das Wetter ist heute wunderbar.",
            "Outside on the terrace, please. The weather is wonderful today.",
            "wunderbar",
            "wonderful"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "Was bestellt der Gast zum Trinken?",
            options = listOf(
              "Einen Cappuccino mit Hafermilch",
              "Einen schwarzen Tee mit Zitrone",
              "Einen Orangensaft",
              "Ein Mineralwasser"
            ),
            correctIndex = 0,
            explanation = "Der Gast sagt: 'Ich hätte gern einen Cappuccino mit Hafermilch'."
          )
        )
      ),
      ListeningAcademyItem(
        id = "listen_de_podcast_cities",
        languageCode = "de",
        cefrLevel = cefr,
        category = "Podcasts",
        title = "Grüne Städte und Radwege der Zukunft",
        regionalAccents = listOf("Germany (Hamburg)", "Austria (Vienna)"),
        lines = listOf(
          ListeningDialogueLine(
            "Moderatorin",
            "Willkommen zu unserem Podcast! Heute sprechen wir über autofreie Viertel in europäischen Städten.",
            "Welcome to our podcast! Today we talk about car-free neighborhoods in European cities.",
            "autofrei",
            "car-free"
          ),
          ListeningDialogueLine(
            "Stadtplaner",
            "Immer mehr Städte bauen sichere Schnellradwege und begrünte Innenhöfe für Familien.",
            "More and more cities are building safe express bike lanes and green courtyards for families.",
            "Schnellradwege",
            "express bike lanes"
          ),
          ListeningDialogueLine(
            "Moderatorin",
            "Das verbessert nicht nur die Luftqualität, sondern belebt auch kleine Nachbarschaftsläden.",
            "That not only improves air quality, but also revitalizes small neighborhood shops.",
            "Luftqualität",
            "air quality"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "Worüber sprechen die Moderatorin und der Stadtplaner?",
            options = listOf(
              "Über autofreie Viertel, Radwege und grüne Städte",
              "Über Flugpreise im Sommer",
              "Über Rezepte für das Abendessen",
              "Über ein Fußballturnier"
            ),
            correctIndex = 0,
            explanation = "In der Folge geht es um autofreie Viertel, Schnellradwege und bessere Luftqualität."
          )
        )
      )
    )
    "es" -> listOf(
      ListeningAcademyItem(
        id = "listen_es_story_painting",
        languageCode = "es",
        cefrLevel = cefr,
        category = "Stories",
        title = "El Cuadro Desaparecido del Museo Real",
        regionalAccents = listOf("Spain (Madrid)", "Mexico (CDMX)", "Argentina (Buenos Aires)"),
        lines = listOf(
          ListeningDialogueLine(
            "Inspectora Vega",
            "Mira este marco vacío en la sala principal del museo, Mateo.",
            "Look at this empty frame in the main hall of the museo, Mateo.",
            "marco vacío",
            "empty frame"
          ),
          ListeningDialogueLine(
            "Mateo",
            "¡Qué extraño! Las cámaras de seguridad no registraron ningún movimiento después de las diez.",
            "How strange! The security cameras recorded no movement after ten o'clock.",
            "extraño",
            "strange / unusual"
          ),
          ListeningDialogueLine(
            "Inspectora Vega",
            "Sin embargo, alguien dejó un guante de seda junto a la puerta secreta del jardín.",
            "However, someone left a silk glove next to the secret garden door.",
            "guante de seda",
            "silk glove"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "What clue did the someone leave near the garden door?",
            options = listOf("A silk glove (un guante de seda)", "A train ticket", "A coffee cup", "A cellphone"),
            correctIndex = 0,
            explanation = "Inspectora Vega notices 'un guante de seda junto a la puerta secreta del jardín'."
          )
        )
      )
    )
    "fr" -> listOf(
      ListeningAcademyItem(
        id = "listen_fr_story_painting",
        languageCode = "fr",
        cefrLevel = cefr,
        category = "Stories",
        title = "Le Tableau Disparu de la Galerie d'Art",
        regionalAccents = listOf("France (Paris)", "Canada (Montréal)"),
        lines = listOf(
          ListeningDialogueLine(
            "Inspectrice Claire",
            "Regardez ce cadre vide dans le salon principal du musée, Hugo.",
            "Look at this empty frame in the main hall of the museum, Hugo.",
            "cadre vide",
            "empty frame"
          ),
          ListeningDialogueLine(
            "Hugo",
            "C'est incroyable ! L'alarme ne s'est pas déclenchée pendant la nuit.",
            "That's incredible! The alarm did not go off during the night.",
            "déclenchée",
            "triggered / set off"
          ),
          ListeningDialogueLine(
            "Inspectrice Claire",
            "Pourtant, voici un carnet de croquis oublié près de la fenêtre ouverte.",
            "Yet, here is a sketchbook forgotten near the open window.",
            "carnet de croquis",
            "sketchbook"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "What object was found near the open window?",
            options = listOf("A sketchbook (un carnet de croquis)", "A pair of glasses", "An umbrella", "A suitcase"),
            correctIndex = 0,
            explanation = "Inspectrice Claire finds 'un carnet de croquis oublié près de la fenêtre ouverte'."
          )
        )
      )
    )
    else -> listOf(
      ListeningAcademyItem(
        id = "listen_${code}_story_painting",
        languageCode = targetLang.code,
        cefrLevel = cefr,
        category = "Stories",
        title = "The Missing Painting at the Grand Gallery",
        regionalAccents = listOf("Standard Accent", "Metropolitan Cadence"),
        lines = listOf(
          ListeningDialogueLine(
            "Detective Clara",
            "Look closely at this empty golden frame in the north gallery wing, Leo.",
            "Look closely at this empty golden frame in the north gallery wing, Leo.",
            "golden frame",
            "decorative border around a painting"
          ),
          ListeningDialogueLine(
            "Leo",
            "That is puzzling! The security sensors were never triggered after midnight.",
            "That is puzzling! The security sensors were never triggered after midnight.",
            "puzzling",
            "confusing or mysterious"
          ),
          ListeningDialogueLine(
            "Detective Clara",
            "Wait—someone dropped a velvet concert program right beside the curator's desk.",
            "Wait—someone dropped a velvet concert program right beside the curator's desk.",
            "curator",
            "museum collection manager"
          ),
          ListeningDialogueLine(
            "Leo",
            "Then our suspect must be attending tonight's symphony across the square!",
            "Then our suspect must be attending tonight's symphony across the square!",
            "suspect",
            "person believed to be involved"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "What clue was found beside the curator's desk?",
            options = listOf(
              "A velvet concert program",
              "A train schedule",
              "A restaurant receipt",
              "A hotel keycard"
            ),
            correctIndex = 0,
            explanation = "Detective Clara spotted a velvet concert program beside the curator's desk."
          ),
          AcademyComprehensionQuestion(
            question = "Where do the detectives plan to look next?",
            options = listOf(
              "At tonight's symphony across the square",
              "At the airport terminal",
              "At a neighborhood bakery",
              "At the train station"
            ),
            correctIndex = 0,
            explanation = "Leo deduces the suspect is attending tonight's symphony across the square."
          )
        )
      ),
      ListeningAcademyItem(
        id = "listen_${code}_podcast_ideas",
        languageCode = targetLang.code,
        cefrLevel = cefr,
        category = "Podcasts",
        title = "How Music & Memory Shape Language Learning",
        regionalAccents = listOf("Podcast Host", "Guest Researcher"),
        lines = listOf(
          ListeningDialogueLine(
            "Host",
            "Welcome back! Today we are exploring why melodies help our brains remember new phrases so effortlessly.",
            "Welcome back! Today we are exploring why melodies help our brains remember new phrases so effortlessly.",
            "effortlessly",
            "naturally and without strain"
          ),
          ListeningDialogueLine(
            "Dr. Lin",
            "When we listen to rhythm and emotional stories, both hemispheres of the brain light up together.",
            "When we listen to rhythm and emotional stories, both hemispheres of the brain light up together.",
            "rhythm",
            "repeated pattern of sound"
          ),
          ListeningDialogueLine(
            "Host",
            "So even ten minutes of daily active listening builds lasting fluency over time.",
            "So even ten minutes of daily active listening builds lasting fluency over time.",
            "fluency",
            "smooth and natural expression"
          )
        ),
        questions = listOf(
          AcademyComprehensionQuestion(
            question = "What is the main topic of this podcast episode?",
            options = listOf(
              "How rhythm, stories, and daily listening strengthen memory and fluency",
              "How to repair musical instruments",
              "Booking tickets for an international flight",
              "Ordering lunch at a café"
            ),
            correctIndex = 0,
            explanation = "The host and Dr. Lin discuss how rhythm and stories help the brain retain new phrases."
          )
        )
      )
    )
  }
}

private fun createFallbackListeningItem(langCode: String, cefr: String): ListeningAcademyItem {
  return ListeningAcademyItem(
    id = "listen_fallback_$langCode",
    languageCode = langCode,
    cefrLevel = cefr,
    category = "Conversations",
    title = "Ordering at a Local Café",
    regionalAccents = listOf("Standard Native Accent", "Conversational Cadence"),
    lines = listOf(
      ListeningDialogueLine("Barista", "Hello! What can I get started for you today?", "Hello! What can I get started for you today?", "get started", "begin preparing"),
      ListeningDialogueLine("Customer", "Hi, I'd like a medium coffee and a warm croissant, please.", "Hi, I'd like a medium coffee and a warm croissant, please.", "croissant", "flaky pastry"),
      ListeningDialogueLine("Barista", "Sure thing! For here or to go?", "Sure thing! For here or to go?", "to go", "takeaway"),
      ListeningDialogueLine("Customer", "For here, please. Thank you so much!", "For here, please. Thank you so much!", "for here", "dine in")
    ),
    questions = listOf(
      AcademyComprehensionQuestion(
        question = "What does the customer order?",
        options = listOf("A medium coffee and a warm croissant", "An iced tea and a donut", "A cold sandwich", "A bottle of juice"),
        correctIndex = 0,
        explanation = "The customer asked for 'a medium coffee and a warm croissant, please.'"
      ),
      AcademyComprehensionQuestion(
        question = "Where will the customer have their order?",
        options = listOf("For here (dining in)", "To go (takeaway)", "Delivered to their office", "At the airport"),
        correctIndex = 0,
        explanation = "The customer explicitly answered 'For here, please.'"
      )
    )
  )
}

private fun createFallbackQuestion(): AcademyComprehensionQuestion {
  return AcademyComprehensionQuestion(
    question = "What was the main topic discussed by the speakers?",
    options = listOf("Confirming plans and details clearly", "Booking flight tickets", "Interviewing for a new job", "Talking about severe weather"),
    correctIndex = 0,
    explanation = "The speakers exchanged key details and confirmed their next steps."
  )
}
