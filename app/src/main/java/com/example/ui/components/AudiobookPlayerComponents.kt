package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.AudiobookBookmark
import com.example.data.model.BookContentType
import com.example.data.model.ChapterAiStudyPack
import com.example.data.model.EbookReadingTheme
import com.example.data.model.FullBookChapter
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.data.repository.BookDownloadFailureReason
import com.example.data.repository.BookDownloadProgress
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.speech.AudiobookMediaController
import com.example.speech.SpeechManager
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Four unified book experience modes within Books & Listening Studio:
 * - READ_ONLY: Pure distraction-free ebook reading (saves reading progress separately)
 * - LISTEN_ONLY: Pure audio listening (saves listening progress separately)
 * - READ_AND_LISTEN: Synchronized ebook paragraphs + audio player
 * - LEARN_MODE: Learn with AI (Explain words, translate, grammar, quiz, speaking, ask AI)
 */
enum class AudiobookPlayerMode(val label: String, val icon: String) {
  READ_ONLY("Read", ""),
  LISTEN_ONLY("Listen", ""),
  READ_AND_LISTEN("Read & Listen", ""),
  LEARN_MODE("AI Study", "")
}

enum class ContinuousScrollSpeed(val label: String, val pxPerSecond: Float) {
  SLOW("Slow", 22f),
  MEDIUM("Med", 38f),
  FAST("Fast", 58f);

  fun next(): ContinuousScrollSpeed = when (this) {
    SLOW -> MEDIUM
    MEDIUM -> FAST
    FAST -> SLOW
  }
}

private data class MappedBookParagraph(
  val paragraphIndex: Int,
  val text: String,
  val startSentenceIndex: Int,
  val endSentenceIndex: Int,
  val sentenceList: List<Pair<Int, String>>,
  val nativeTranslation: String = "",
  val keyVocabWord: String = "",
  val keyVocabMeaning: String = ""
)

private val DeepNavyBg = Color(0xFF060D1E)
private val DeepNavyCard = Color(0xFF0F1B38)
private val DeepNavyElevated = Color(0xFF14244A)
private val DeepNavyBorder = Color(0xFF1E305C)
private val CyanAccent = Color(0xFF06B6D4)
private val EmeraldAccent = Color(0xFF10B981)
private val AmberAccent = Color(0xFFF59E0B)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextLight = Color(0xFFCBD5E1)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun AudiobookPlayerScreen(
  book: AudiobookBook,
  initialEpisodeIndex: Int = 0,
  initialPlayerMode: AudiobookPlayerMode = AudiobookPlayerMode.READ_AND_LISTEN,
  autoStartPlayback: Boolean = false,
  targetLang: Language,
  nativeLang: Language = targetLang,
  cefrLevel: String = "B1",
  speechManager: SpeechManager,
  userId: String,
  isProUnlocked: Boolean = false,
  onSaveVocabToFlashcard: (word: String, meaning: String, phonetic: String) -> Unit,
  onAddXp: (Long) -> Unit,
  onOpenSubscription: () -> Unit,
  onChapterChanged: (Int) -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var activeBook by remember(book.id) { mutableStateOf(book) }
  var isLoadingText by remember(book.id) { mutableStateOf(false) }
  var textDownloadProgress by remember(book.id) { mutableStateOf<BookDownloadProgress?>(null) }
  var textLoadError by remember(book.id) { mutableStateOf<String?>(null) }
  var textLoadJob by remember(book.id) { mutableStateOf<Job?>(null) }

  var isLoadingAudio by remember(book.id) { mutableStateOf(false) }
  var audioLoadError by remember(book.id) { mutableStateOf<String?>(null) }
  var audioLoadJob by remember(book.id) { mutableStateOf<Job?>(null) }

  var playerMode by remember { mutableStateOf(initialPlayerMode) }
  var autoScrollEnabled by remember { mutableStateOf(true) }

  // Load saved user reading preferences & progress
  val initialSavedProgress = remember(book.id) {
    AudiobookLearningService.loadUserProgress(context, userId, book.id)
  }
  var readingTheme by remember(book.id) {
    mutableStateOf(EbookReadingTheme.fromId(initialSavedProgress.readingTheme))
  }
  var readerFontSizeSp by remember(book.id) {
    mutableFloatStateOf(initialSavedProgress.readingFontSizeSp.coerceIn(14f, 26f))
  }
  var isPageByPageMode by remember(book.id) {
    mutableStateOf(initialSavedProgress.isPageMode)
  }
  var currentPageIndex by remember(book.id) {
    mutableIntStateOf(initialSavedProgress.readingPageIndex.coerceAtLeast(0))
  }

  // Bind controller states
  val controllerBook by AudiobookMediaController.activeBook.collectAsState()
  val controllerChapterIndex by AudiobookMediaController.activeChapterIndex.collectAsState()
  val isPlaying by AudiobookMediaController.isPlaying.collectAsState()
  val isBuffering by AudiobookMediaController.isBuffering.collectAsState()
  val currentSentenceIndex by AudiobookMediaController.currentSentenceIndex.collectAsState()
  val currentPositionMs by AudiobookMediaController.currentPositionMs.collectAsState()
  val durationMs by AudiobookMediaController.durationMs.collectAsState()
  val playbackSpeed by AudiobookMediaController.playbackSpeed.collectAsState()
  val selectedVoice by AudiobookMediaController.selectedVoice.collectAsState()
  val useHumanNarrator by AudiobookMediaController.useHumanNarrator.collectAsState()
  val sleepTimerMinutes by AudiobookMediaController.sleepTimerMinutes.collectAsState()
  val autoPlayNextChapter by AudiobookMediaController.autoPlayNextChapter.collectAsState()
  val skipRecordingAnnouncements by AudiobookMediaController.skipRecordingAnnouncements.collectAsState()
  val activeTrackBoundaries by AudiobookMediaController.activeTrackBoundaries.collectAsState()
  val introAnnouncementSkipped by AudiobookMediaController.introAnnouncementSkippedForChapter.collectAsState()
  val humanNarrationStatusMessage by AudiobookMediaController.humanNarrationStatusMessage.collectAsState()

  val currentChapterIndex = if (controllerBook?.id == activeBook.id) {
    controllerChapterIndex
  } else {
    initialEpisodeIndex.coerceAtLeast(0)
  }

  var continuousScrollSpeed by remember { mutableStateOf(ContinuousScrollSpeed.MEDIUM) }
  var autoScrollPausedByUser by remember(activeBook.id, currentChapterIndex) { mutableStateOf(false) }

  var showSpeedMenu by remember { mutableStateOf(false) }
  var showSleepTimerMenu by remember { mutableStateOf(false) }
  var showVoiceMenu by remember { mutableStateOf(false) }
  var showChaptersDrawer by remember { mutableStateOf(false) }
  var showBookmarksDrawer by remember { mutableStateOf(false) }
  var bookBookmarks by remember(book.id) {
    mutableStateOf(AudiobookLearningService.getBookmarksForBook(context, userId, book.id))
  }

  // Selected passage for instant Gemini explanation / translation / Q&A
  var selectedPassageForAi by remember { mutableStateOf<String?>(null) }
  var aiPassageExplanation by remember { mutableStateOf<String?>(null) }
  var isExplainingPassage by remember { mutableStateOf(false) }
  var customPassageQuestion by remember { mutableStateOf("") }

  // Chapter AI Study Pack state (Mode 3: Learn with AI)
  var chapterStudyPack by remember(activeBook.id, currentChapterIndex) { mutableStateOf<ChapterAiStudyPack?>(null) }
  var isLoadingStudyPack by remember { mutableStateOf(false) }
  var selectedQuizAnswers by remember(activeBook.id, currentChapterIndex) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
  var isListeningForSpeakingPrompt by remember { mutableStateOf(false) }
  val userSpokenResponse by speechManager.spokenText.collectAsState()

  val listState = rememberLazyListState()

  // Hydrate separate reading & listening progress from Firebase Firestore on open
  LaunchedEffect(book.id, userId) {
    val hydrated = AudiobookLearningService.hydrateBookProgressFromFirebase(context, userId, book.id)
    readingTheme = EbookReadingTheme.fromId(hydrated.readingTheme)
    readerFontSizeSp = hydrated.readingFontSizeSp.coerceIn(14f, 26f)
    isPageByPageMode = hydrated.isPageMode
    if (currentPageIndex == 0 && hydrated.readingPageIndex > 0) {
      currentPageIndex = hydrated.readingPageIndex
    }
  }

  // Helper to persist reading preferences & reading position independently to local + Firebase
  fun saveReaderPreferences(
    chIndex: Int = currentChapterIndex,
    scrollIdx: Int = listState.firstVisibleItemIndex,
    pageIdx: Int = currentPageIndex
  ) {
    val totalChCount = activeBook.totalChapterCount.coerceAtLeast(1)
    val inChapterFrac = if (isPageByPageMode) {
      val pages = (pageIdx + 1).toFloat() / maxOf(1, pageIdx + 1).toFloat()
      pages.coerceIn(0f, 1f)
    } else {
      val paraTotal = activeBook.parsedChapters.getOrNull(chIndex)?.paragraphs?.size?.coerceAtLeast(1) ?: 1
      ((scrollIdx + 1).toFloat() / paraTotal.toFloat()).coerceIn(0f, 1f)
    }
    val overallReadPercent = ((chIndex.toFloat() + inChapterFrac) / totalChCount.toFloat()).coerceIn(0f, 1f)
    val existing = AudiobookLearningService.loadUserProgress(context, userId, activeBook.id)
    val updatedCompletedRead = if (inChapterFrac >= 0.85f) {
      existing.completedReadingChapterIndices + chIndex
    } else {
      existing.completedReadingChapterIndices
    }
    AudiobookLearningService.saveReadingProgress(
      context = context,
      userId = userId,
      bookId = activeBook.id,
      bookTitle = activeBook.title,
      chapterIndex = chIndex,
      scrollOffset = scrollIdx,
      pageIndex = pageIdx,
      progressPercent = overallReadPercent,
      readingTheme = readingTheme.id,
      fontSizeSp = readerFontSizeSp,
      isPageMode = isPageByPageMode,
      completedReadingChapters = updatedCompletedRead
    )
  }

  // Keep parent screen's selectedEpisodeIndex in sync whenever chapter changes via Next/Prev/Drawer/Autoplay
  LaunchedEffect(currentChapterIndex) {
    onChapterChanged(currentChapterIndex)
  }

  fun mergeTextChaptersPreservingAudio(
    current: AudiobookBook,
    incomingBook: AudiobookBook,
    incomingChapters: List<FullBookChapter>
  ): AudiobookBook {
    val existingEditions = current.librivoxEditions.ifEmpty { incomingBook.librivoxEditions }
    val existingSections = existingEditions.firstOrNull { it.sections.isNotEmpty() }?.sections.orEmpty()
    val aligned = if (existingSections.isNotEmpty()) {
      GutendexAndLibriVoxService.alignChaptersWithLibriVox(incomingChapters, existingSections, incomingBook.title)
    } else if (current.parsedChapters.any { !it.librivoxAudioUrl.isNullOrBlank() }) {
      incomingChapters.mapIndexed { idx, ch ->
        val oldCh = current.parsedChapters.getOrNull(idx)
        if (ch.librivoxAudioUrl.isNullOrBlank() && oldCh != null && !oldCh.librivoxAudioUrl.isNullOrBlank()) {
          ch.copy(
            librivoxAudioUrl = oldCh.librivoxAudioUrl,
            librivoxDurationSeconds = oldCh.librivoxDurationSeconds,
            isAudioDurationVerified = oldCh.isAudioDurationVerified,
            librivoxReaderName = oldCh.librivoxReaderName
          )
        } else ch
      }
    } else {
      incomingChapters
    }
    val hasHumanNow = existingEditions.any { ed -> ed.sections.any { it.listenUrl.startsWith("http", ignoreCase = true) } } ||
      aligned.any { !it.librivoxAudioUrl.isNullOrBlank() }
    return incomingBook.copy(
      parsedChapters = aligned,
      librivoxEditions = existingEditions,
      hasFullTextAvailable = true,
      contentType = if (hasHumanNow) BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK else incomingBook.contentType
    )
  }

  fun launchTextDownload(priorityChapterIndex: Int, hadChaptersAtOpen: Boolean) {
    textLoadJob?.cancel()
    textLoadJob = scope.launch {
      isLoadingText = true
      textLoadError = null
      textDownloadProgress = null
      try {
        val textRes = GutendexAndLibriVoxService.downloadBookTextOnly(
          context = context,
          book = activeBook,
          priorityChapterIndex = priorityChapterIndex,
          onProgress = { prog -> textDownloadProgress = prog },
          onPriorityChapterReady = { ready ->
            val matchesSame = ready.book.id == book.id ||
              ready.book.id == activeBook.id ||
              GutendexAndLibriVoxService.isSameBookTitleAndAuthor(ready.book.title, ready.book.author, book.title, book.author)
            if (matchesSame && ready.chapters.isNotEmpty()) {
              val merged = mergeTextChaptersPreservingAudio(activeBook, ready.book, ready.chapters)
              activeBook = merged
              AudiobookMediaController.updateActiveBookMetadata(merged)
            }
          }
        )
        isLoadingText = false
        textDownloadProgress = null
        val matchesFinal = textRes.book.id == book.id ||
          textRes.book.id == activeBook.id ||
          GutendexAndLibriVoxService.isSameBookTitleAndAuthor(textRes.book.title, textRes.book.author, book.title, book.author)
        if (matchesFinal) {
          if (textRes.chapters.isNotEmpty()) {
            val merged = mergeTextChaptersPreservingAudio(activeBook, textRes.book, textRes.chapters)
            activeBook = merged
            AudiobookMediaController.updateActiveBookMetadata(merged)
            if (!hadChaptersAtOpen) {
              val targetCh = initialEpisodeIndex.coerceIn(0, (merged.totalChapterCount - 1).coerceAtLeast(0))
              val savedProgNow = AudiobookLearningService.loadUserProgress(context, userId, merged.id)
              val preferHumanOnReady = if (
                savedProgNow.preferredEditionId != "tts" &&
                AudiobookMediaController.getChapterMp3Url(merged, targetCh) != null
              ) true else null
              AudiobookMediaController.loadBookAndChapter(
                book = merged,
                chapterIndex = targetCh,
                sentenceIndex = 0,
                autoPlay = autoStartPlayback,
                preferHumanAudio = preferHumanOnReady
              )
            }
          }
          textLoadError = textRes.errorMessage
        }
      } catch (_: CancellationException) {
        isLoadingText = false
        textDownloadProgress = null
        textLoadError = "Book text download cancelled. Tap Retry Text anytime."
      }
    }
  }

  fun launchAudioDiscovery() {
    audioLoadJob?.cancel()
    audioLoadJob = scope.launch {
      isLoadingAudio = true
      audioLoadError = null
      try {
        val audioRes = GutendexAndLibriVoxService.fetchBookAudioOnly(activeBook)
        isLoadingAudio = false
        val matchesFinal = audioRes.book.id == book.id ||
          audioRes.book.id == activeBook.id ||
          GutendexAndLibriVoxService.isSameBookTitleAndAuthor(audioRes.book.title, audioRes.book.author, book.title, book.author)
        if (matchesFinal) {
          val verifiedEditions = audioRes.editions.filter { ed ->
            ed.sections.any { it.listenUrl.startsWith("http", ignoreCase = true) }
          }
          if (verifiedEditions.isNotEmpty()) {
            val primarySections = verifiedEditions.first().sections
            val existingCh = activeBook.parsedChapters
            val mergedChapters = if (existingCh.isNotEmpty()) {
              GutendexAndLibriVoxService.alignChaptersWithLibriVox(existingCh, primarySections, activeBook.title)
            } else {
              GutendexAndLibriVoxService.alignChaptersWithLibriVox(emptyList(), primarySections, activeBook.title)
            }
            val merged = activeBook.copy(
              librivoxEditions = verifiedEditions,
              parsedChapters = mergedChapters,
              contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK
            )
            activeBook = merged
            AudiobookMediaController.updateActiveBookMetadata(merged)
            val savedProgNow = AudiobookLearningService.loadUserProgress(context, userId, merged.id)
            val currentChNow = AudiobookMediaController.activeChapterIndex.value
            if (savedProgNow.preferredEditionId != "tts" &&
              !AudiobookMediaController.useHumanNarrator.value &&
              AudiobookMediaController.getChapterMp3Url(merged, currentChNow) != null
            ) {
              AudiobookMediaController.setNarrationEditionMode(
                useHumanLibriVox = true,
                autoStartPlayback = autoStartPlayback || isPlaying
              )
            }
            audioLoadError = null
          } else if (!activeBook.hasVerifiedHumanAudio) {
            audioLoadError = audioRes.errorMessage
          }
        }
      } catch (_: CancellationException) {
        isLoadingAudio = false
        audioLoadError = "Audio lookup cancelled. Tap Retry Audio anytime."
      }
    }
  }

  DisposableEffect(book.id) {
    onDispose {
      textLoadJob?.cancel()
      audioLoadJob?.cancel()
    }
  }

  // Attach controller and prioritize loading the requested chapter without blocking or overwriting selection
  LaunchedEffect(book.id, initialEpisodeIndex) {
    AudiobookMediaController.attach(context, speechManager, userId)
    val initialTotalCh = AudiobookMediaController.getTotalChaptersCount(activeBook)
    val startCh = initialEpisodeIndex.coerceIn(0, (initialTotalCh - 1).coerceAtLeast(0))
    val initialSaved = AudiobookLearningService.loadUserProgress(context, userId, activeBook.id)
    val startSent = initialSaved.chapterSentenceIndexMap[startCh]
      ?: if (startCh == initialSaved.lastEpisodeIndex) initialSaved.lastSentenceIndex else 0

    val hadChaptersInitially = activeBook.parsedChapters.isNotEmpty() || activeBook.librivoxEditions.isNotEmpty()

    // Immediately bind and load the requested chapter so audio and chapter navigation match right away
    AudiobookMediaController.loadBookAndChapter(
      book = activeBook,
      chapterIndex = startCh,
      sentenceIndex = startSent,
      autoPlay = autoStartPlayback && hadChaptersInitially
    )

    if (activeBook.isFullBookEligible) {
      if (activeBook.parsedChapters.isEmpty() || !GutendexAndLibriVoxService.isBookChaptersFullUnabridged(activeBook)) {
        launchTextDownload(
          priorityChapterIndex = initialEpisodeIndex.coerceAtLeast(0),
          hadChaptersAtOpen = hadChaptersInitially
        )
      }
      if (!activeBook.hasVerifiedHumanAudio && activeBook.librivoxEditions.isEmpty()) {
        launchAudioDiscovery()
      }
    }
  }

  val sentences = remember(activeBook, currentChapterIndex) {
    AudiobookMediaController.getChapterSentences(activeBook, currentChapterIndex)
  }
  val totalChapters = remember(activeBook) {
    AudiobookMediaController.getTotalChaptersCount(activeBook)
  }
  val chapterTitle = remember(activeBook, currentChapterIndex) {
    AudiobookMediaController.getChapterTitle(activeBook, currentChapterIndex)
  }

  // Build natural book paragraphs mapped to sentence indices so we NEVER render fake stub blurbs as real book prose
  val mappedParagraphs: List<MappedBookParagraph> = remember(activeBook, currentChapterIndex, sentences) {
    val fullChapter = activeBook.parsedChapters.getOrNull(currentChapterIndex)
    val isStubChapter = fullChapter != null && GutendexAndLibriVoxService.isStubOrSummaryChapter(fullChapter)
    val rawParas = if (isStubChapter) {
      emptyList()
    } else {
      fullChapter?.paragraphs?.filter { it.isNotBlank() }.orEmpty()
    }

    if (rawParas.isNotEmpty()) {
      var sentCursor = 0
      val sentenceSplitRegex = Regex("(?<=[.!?\"”])\\s+(?=[A-Z“\"‘'0-9])")
      rawParas.mapIndexed { pIdx, pText ->
        val approxParts = pText.split(sentenceSplitRegex).filter { it.isNotBlank() }
        val startIdx = sentCursor.coerceAtMost((sentences.size - 1).coerceAtLeast(0))
        val matchedPairs = mutableListOf<Pair<Int, String>>()

        // Match sentences belonging to this paragraph
        var consumed = 0
        while (sentCursor < sentences.size && (consumed < approxParts.size || pText.contains(sentences[sentCursor].take(24)))) {
          matchedPairs.add(sentCursor to sentences[sentCursor])
          sentCursor++
          consumed++
          if (consumed >= approxParts.size + 2) break
        }
        if (matchedPairs.isEmpty() && sentences.isNotEmpty()) {
          matchedPairs.add(startIdx to pText)
        }
        val endIdx = (matchedPairs.lastOrNull()?.first ?: startIdx).coerceAtLeast(startIdx)
        MappedBookParagraph(
          paragraphIndex = pIdx,
          text = pText,
          startSentenceIndex = startIdx,
          endSentenceIndex = endIdx,
          sentenceList = matchedPairs
        )
      }
    } else {
      val ep = activeBook.episodes.getOrNull(currentChapterIndex)
      if (ep != null && ep.narrationLines.isNotEmpty()) {
        // Group educational lines into natural paragraphs of 3 sentences while preserving translations
        ep.narrationLines.chunked(3).mapIndexed { pIdx, chunk ->
          val startSent = pIdx * 3
          val endSent = startSent + chunk.size - 1
          val pairs = chunk.mapIndexed { offset, line -> (startSent + offset) to line.targetText }
          val firstVocab = chunk.firstOrNull { it.keyVocabWord.isNotBlank() }
          MappedBookParagraph(
            paragraphIndex = pIdx,
            text = chunk.joinToString(" ") { it.targetText },
            startSentenceIndex = startSent,
            endSentenceIndex = endSent,
            sentenceList = pairs,
            nativeTranslation = chunk.mapNotNull { it.nativeTranslation.takeIf { t -> t.isNotBlank() } }.joinToString(" "),
            keyVocabWord = firstVocab?.keyVocabWord.orEmpty(),
            keyVocabMeaning = firstVocab?.keyVocabMeaning.orEmpty()
          )
        }
      } else {
        sentences.chunked(4).mapIndexed { pIdx, chunk ->
          val startSent = pIdx * 4
          val endSent = startSent + chunk.size - 1
          val pairs = chunk.mapIndexed { offset, s -> (startSent + offset) to s }
          MappedBookParagraph(
            paragraphIndex = pIdx,
            text = chunk.joinToString(" "),
            startSentenceIndex = startSent,
            endSentenceIndex = endSent,
            sentenceList = pairs
          )
        }
      }
    }
  }

  // Group paragraphs into comfortable ebook pages for Page-by-Page reading mode (~1,150 chars per page)
  val ebookPages: List<List<MappedBookParagraph>> = remember(mappedParagraphs, readerFontSizeSp) {
    if (mappedParagraphs.isEmpty()) {
      listOf(emptyList())
    } else {
      val targetCharsPerPage = if (readerFontSizeSp >= 21f) 750 else if (readerFontSizeSp >= 18f) 1050 else 1350
      val pages = mutableListOf<List<MappedBookParagraph>>()
      val currentBuf = mutableListOf<MappedBookParagraph>()
      var currentChars = 0
      for (para in mappedParagraphs) {
        currentBuf.add(para)
        currentChars += para.text.length
        if (currentChars >= targetCharsPerPage) {
          pages.add(currentBuf.toList())
          currentBuf.clear()
          currentChars = 0
        }
      }
      if (currentBuf.isNotEmpty()) {
        pages.add(currentBuf.toList())
      }
      pages.ifEmpty { listOf(mappedParagraphs) }
    }
  }

  val activeParagraphIndex by remember(mappedParagraphs, currentSentenceIndex) {
    derivedStateOf {
      mappedParagraphs.indexOfFirst { currentSentenceIndex in it.startSentenceIndex..it.endSentenceIndex }
        .coerceAtLeast(0)
    }
  }

  // Detect genuine user touch drag on the reader list (never triggered by programmatic scroll)
  LaunchedEffect(listState) {
    listState.interactionSource.interactions.collect { interaction ->
      if (interaction is DragInteraction.Start) {
        autoScrollPausedByUser = true
      }
    }
  }

  // Keep page index valid and reset scroll pause when switching chapters
  LaunchedEffect(currentChapterIndex) {
    autoScrollPausedByUser = false
    currentPageIndex = 0
    try {
      listState.scrollToItem(0)
    } catch (_: Exception) {}
  }

  // AI Voice Narration: keep active spoken paragraph centered vertically in viewport (or turn page in Page mode)
  LaunchedEffect(
    activeParagraphIndex,
    currentSentenceIndex,
    playerMode,
    autoScrollEnabled,
    isPlaying,
    isPageByPageMode,
    useHumanNarrator,
    autoScrollPausedByUser
  ) {
    if (playerMode == AudiobookPlayerMode.READ_AND_LISTEN &&
      !useHumanNarrator &&
      autoScrollEnabled &&
      isPlaying &&
      !autoScrollPausedByUser &&
      mappedParagraphs.isNotEmpty()
    ) {
      if (isPageByPageMode) {
        val targetPage = ebookPages.indexOfFirst { page ->
          page.any { it.paragraphIndex == activeParagraphIndex }
        }
        if (targetPage >= 0 && targetPage != currentPageIndex) {
          currentPageIndex = targetPage
        }
      } else {
        try {
          // +1 offset for the chapter title header item in LazyColumn
          val targetItemIdx = (activeParagraphIndex + 1).coerceAtLeast(0)
          val layoutInfo = listState.layoutInfo
          val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
          var itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetItemIdx }

          if (itemInfo == null) {
            listState.animateScrollToItem((targetItemIdx - 1).coerceAtLeast(0))
            val updatedLayout = listState.layoutInfo
            itemInfo = updatedLayout.visibleItemsInfo.firstOrNull { it.index == targetItemIdx }
          }

          if (itemInfo != null) {
            val activePara = mappedParagraphs.getOrNull(activeParagraphIndex)
            val sentenceCount = activePara?.let { (it.endSentenceIndex - it.startSentenceIndex + 1).coerceAtLeast(1) } ?: 1
            val sentenceFraction = if (activePara != null && sentenceCount > 1 && itemInfo.size > (viewportHeight * 0.45f)) {
              val localIdx = (currentSentenceIndex - activePara.startSentenceIndex).coerceIn(0, sentenceCount - 1)
              (localIdx + 0.5f) / sentenceCount.toFloat()
            } else {
              0.5f
            }
            val focalYInItem = (itemInfo.size * sentenceFraction).toInt()
            val desiredScreenY = (viewportHeight * 0.42f).toInt()
            val deltaPx = (itemInfo.offset + focalYInItem - desiredScreenY).toFloat()
            if (kotlin.math.abs(deltaPx) > 12f) {
              listState.animateScrollBy(deltaPx)
            }
          }
        } catch (_: Exception) {}
      }
    }
  }

  // Human LibriVox Audio: honest continuous vertical scrolling synced with playback speed
  LaunchedEffect(
    playerMode,
    useHumanNarrator,
    autoScrollEnabled,
    isPlaying,
    isPageByPageMode,
    autoScrollPausedByUser,
    continuousScrollSpeed,
    playbackSpeed,
    currentChapterIndex
  ) {
    if (playerMode == AudiobookPlayerMode.READ_AND_LISTEN &&
      useHumanNarrator &&
      autoScrollEnabled &&
      isPlaying &&
      !isPageByPageMode &&
      !autoScrollPausedByUser &&
      mappedParagraphs.isNotEmpty()
    ) {
      var lastFrameNanos = System.nanoTime()
      while (isActive) {
        delay(16L)
        val nowNanos = System.nanoTime()
        val dtSec = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastFrameNanos = nowNanos
        if (listState.canScrollForward) {
          val speedMult = playbackSpeed.coerceIn(0.6f, 2.0f)
          val delta = continuousScrollSpeed.pxPerSecond * speedMult * dtSec
          if (delta > 0f) {
            try {
              listState.scrollBy(delta)
            } catch (_: Exception) {}
          }
        }
      }
    }
  }

  // Save reading scroll position periodically when scrolling stops
  LaunchedEffect(listState.firstVisibleItemIndex, currentChapterIndex) {
    if (mappedParagraphs.isNotEmpty()) {
      saveReaderPreferences(
        chIndex = currentChapterIndex,
        scrollIdx = listState.firstVisibleItemIndex,
        pageIdx = currentPageIndex
      )
    }
  }

  // Load AI Study Pack when switching to Learn Mode (Pro-only AI feature)
  LaunchedEffect(playerMode, currentChapterIndex, activeBook.id, isProUnlocked) {
    if (playerMode == AudiobookPlayerMode.LEARN_MODE && isProUnlocked && chapterStudyPack == null && !isLoadingStudyPack) {
      val fullText = activeBook.parsedChapters.getOrNull(currentChapterIndex)?.fullText
        ?: activeBook.episodes.getOrNull(currentChapterIndex)?.narrationLines?.joinToString(" ") { it.targetText }
        ?: activeBook.shortDescription
      if (fullText.isNotBlank()) {
        isLoadingStudyPack = true
        chapterStudyPack = AudiobookLearningService.generateChapterAiStudyPack(
          context = context,
          bookTitle = activeBook.title,
          bookAuthor = activeBook.author,
          bookId = activeBook.id,
          chapterIndex = currentChapterIndex,
          chapterTitle = chapterTitle,
          chapterText = fullText,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          cefrLevel = cefrLevel
        )
        isLoadingStudyPack = false
      }
    }
  }

  val grad = activeBook.coverGradientColors.map { Color(it) }.ifEmpty { listOf(DeepNavyCard, DeepNavyElevated) }
  val savedProg = remember(activeBook.id, currentChapterIndex, isPlaying) {
    AudiobookLearningService.loadUserProgress(context, userId, activeBook.id)
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(grad.first().copy(alpha = 0.65f), DeepNavyBg, Color(0xFF030712))))
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
      .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
      .testTag("audiobook_player_screen")
  ) {
    // 1. TOP BAR: Back, Book & Chapter Title, All Chapters Button, Bookmark Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = {
          saveReaderPreferences()
          onBack()
        },
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
          .testTag("player_back_btn")
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
          .clickable { showChaptersDrawer = !showChaptersDrawer },
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = activeBook.title,
          color = TextWhite,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Ch. ${currentChapterIndex + 1}/$totalChapters • $chapterTitle",
          color = CyanAccent,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Explicit "All Chapters" button opening the complete ordered chapter list
        Surface(
          onClick = {
            showBookmarksDrawer = false
            showChaptersDrawer = !showChaptersDrawer
          },
          shape = RoundedCornerShape(10.dp),
          color = if (showChaptersDrawer) CyanAccent else Color.White.copy(alpha = 0.1f),
          border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
          modifier = Modifier.testTag("player_all_chapters_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.AutoMirrored.Filled.FormatListBulleted,
              contentDescription = "All Chapters",
              tint = if (showChaptersDrawer) Color(0xFF041220) else CyanAccent,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Chapters ($totalChapters)",
              color = if (showChaptersDrawer) Color(0xFF041220) else TextWhite,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Open saved bookmarks drawer
        IconButton(
          onClick = {
            showChaptersDrawer = false
            bookBookmarks = AudiobookLearningService.getBookmarksForBook(context, userId, activeBook.id)
            showBookmarksDrawer = !showBookmarksDrawer
          },
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (showBookmarksDrawer) AmberAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .testTag("player_bookmarks_drawer_btn")
        ) {
          Icon(
            Icons.Default.Bookmarks,
            contentDescription = "Bookmarks (${bookBookmarks.size})",
            tint = if (bookBookmarks.isNotEmpty()) AmberAccent else TextLight,
            modifier = Modifier.size(18.dp)
          )
        }

        // Bookmark current position button
        IconButton(
          onClick = {
            val snippet = sentences.getOrNull(currentSentenceIndex)?.take(120) ?: chapterTitle
            val bm = AudiobookBookmark(
              id = "bm_${UUID.randomUUID().toString().take(8)}",
              bookId = activeBook.id,
              bookTitle = activeBook.title,
              chapterIndex = currentChapterIndex,
              chapterTitle = chapterTitle,
              sentenceIndex = currentSentenceIndex,
              positionMs = currentPositionMs,
              snippetText = snippet
            )
            AudiobookLearningService.addBookmark(context, userId, bm)
            bookBookmarks = AudiobookLearningService.getBookmarksForBook(context, userId, activeBook.id)
            Toast.makeText(context, "Bookmark saved in $chapterTitle", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .testTag("player_add_bookmark_btn")
        ) {
          Icon(Icons.Default.BookmarkAdd, contentDescription = "Bookmark Position", tint = AmberAccent, modifier = Modifier.size(18.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 2. MODE SWITCHER TABS: Read | Listen | Read & Listen | AI Study
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DeepNavyCard)
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      AudiobookPlayerMode.entries.forEach { mode ->
        val selected = playerMode == mode
        Surface(
          onClick = {
            if (mode == AudiobookPlayerMode.READ_ONLY && isPlaying) {
              AudiobookMediaController.pausePlayback()
            }
            playerMode = mode
          },
          shape = RoundedCornerShape(9.dp),
          color = if (selected) CyanAccent else Color.Transparent,
          modifier = Modifier
            .weight(if (mode == AudiobookPlayerMode.READ_AND_LISTEN) 1.3f else 1f)
            .testTag("player_mode_${mode.name.lowercase()}")
        ) {
          Text(
            text = mode.label,
            color = if (selected) Color(0xFF041220) else TextLight,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
          )
        }
      }
    }

    // Complete Ordered Chapter List Drawer (1 .. Final Chapter with Title, Progress, and Duration)
    if (showChaptersDrawer) {
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyElevated),
        border = BorderStroke(1.2.dp, CyanAccent.copy(alpha = 0.6f)),
        modifier = Modifier
          .fillMaxWidth()
          .height(290.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "All Chapters (1 – $totalChapters)",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "${activeBook.title} • Complete Book Order",
                color = EmeraldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            IconButton(onClick = { showChaptersDrawer = false }, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close Chapters", tint = TextLight, modifier = Modifier.size(18.dp))
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = DeepNavyBorder)
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(totalChapters, key = { idx -> "drawer_ch_$idx" }) { idx ->
              val chObj = activeBook.parsedChapters.getOrNull(idx)
              val lvSec = activeBook.librivoxEditions.firstOrNull()?.sections?.getOrNull(idx)
              val title = chObj?.title ?: lvSec?.title ?: AudiobookMediaController.getChapterTitle(activeBook, idx)
              val durationLabel = chObj?.formattedDuration?.takeIf { it.isNotBlank() }
                ?: lvSec?.formattedDuration?.takeIf { it.isNotBlank() }
                ?: if (chObj != null && chObj.wordCount > 0 && !GutendexAndLibriVoxService.isStubOrSummaryChapter(chObj)) "${"%,d".format(chObj.wordCount)} words" else ""
              val isCurr = idx == currentChapterIndex
              val isDone = savedProg.completedChapterIndices.contains(idx)
              val chFrac = savedProg.chapterProgressMap[idx] ?: 0f

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (isCurr) CyanAccent.copy(alpha = 0.16f) else Color.Transparent)
                  .clickable {
                    showChaptersDrawer = false
                    onChapterChanged(idx)
                    AudiobookMediaController.loadBookAndChapter(activeBook, idx, 0, autoPlay = isPlaying)
                  }
                  .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        isCurr -> CyanAccent
                        isDone -> EmeraldAccent.copy(alpha = 0.25f)
                        else -> Color.White.copy(alpha = 0.08f)
                      }
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (isDone && !isCurr) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(15.dp))
                  } else {
                    Text(
                      text = "${idx + 1}",
                      color = if (isCurr) Color(0xFF041220) else TextWhite,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = title,
                    color = if (isCurr) CyanAccent else TextWhite,
                    fontSize = 13.sp,
                    fontWeight = if (isCurr) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (durationLabel.isNotBlank()) {
                      Text(durationLabel, color = TextMuted, fontSize = 11.sp)
                    }
                    if (isDone) {
                      Text(if (durationLabel.isNotBlank()) "• Completed" else "Completed", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    } else if (chFrac > 0.02f) {
                      Text(if (durationLabel.isNotBlank()) "• ${(chFrac * 100).toInt()}% read" else "${(chFrac * 100).toInt()}% read", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                  }
                }
                Icon(
                  Icons.Default.PlayArrow,
                  contentDescription = "Jump to $title",
                  tint = if (isCurr) CyanAccent else TextMuted,
                  modifier = Modifier.size(18.dp)
                )
              }
              HorizontalDivider(color = DeepNavyBorder.copy(alpha = 0.5f))
            }
          }
        }
      }
    }

    // Saved Bookmarks Drawer (View, Jump to exact position, or Delete)
    if (showBookmarksDrawer) {
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyElevated),
        border = BorderStroke(1.2.dp, AmberAccent.copy(alpha = 0.6f)),
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .testTag("player_bookmarks_drawer")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Saved Bookmarks (${bookBookmarks.size})",
              color = TextWhite,
              fontSize = 14.sp,
              fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = { showBookmarksDrawer = false }, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close Bookmarks", tint = TextLight, modifier = Modifier.size(18.dp))
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = DeepNavyBorder)
          if (bookBookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = "Tap the Bookmark+ icon above to save your spot in any chapter.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
              )
            }
          } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
              itemsIndexed(bookBookmarks, key = { _, bm -> bm.id }) { _, bm ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      showBookmarksDrawer = false
                      onChapterChanged(bm.chapterIndex)
                      AudiobookMediaController.loadBookAndChapter(
                        book = activeBook,
                        chapterIndex = bm.chapterIndex,
                        sentenceIndex = bm.sentenceIndex,
                        autoPlay = isPlaying
                      )
                      if (bm.positionMs > 0L && useHumanNarrator) {
                        AudiobookMediaController.seekToPositionMs(bm.positionMs)
                      }
                    }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Bookmark, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = bm.chapterTitle,
                      color = TextWhite,
                      fontSize = 12.5.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    if (bm.snippetText.isNotBlank()) {
                      Text(
                        text = bm.snippetText,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                  IconButton(
                    onClick = {
                      AudiobookLearningService.removeBookmark(context, userId, bm.id)
                      bookBookmarks = AudiobookLearningService.getBookmarksForBook(context, userId, activeBook.id)
                    },
                    modifier = Modifier.size(30.dp)
                  ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete bookmark", tint = TextMuted, modifier = Modifier.size(16.dp))
                  }
                }
                HorizontalDivider(color = DeepNavyBorder.copy(alpha = 0.45f))
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Non-blocking separated Text & Audio status/retry banners
    if (isLoadingText || !textLoadError.isNullOrBlank()) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (!textLoadError.isNullOrBlank()) Color(0xFF2A1215) else DeepNavyCard,
        border = BorderStroke(1.dp, if (!textLoadError.isNullOrBlank()) AmberAccent.copy(alpha = 0.6f) else CyanAccent.copy(alpha = 0.35f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (isLoadingText) {
              val prog = textDownloadProgress
              val progText = if (prog != null && prog.downloadedBytes > 0L) {
                "Downloading book text: ${prog.formattedProgress}"
              } else {
                "Loading complete book text..."
              }
              CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
              Text(
                text = progText,
                color = TextLight,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
              )
              Surface(
                onClick = {
                  textLoadJob?.cancel()
                  isLoadingText = false
                  textDownloadProgress = null
                  textLoadError = "Book text download cancelled. Tap Retry Text anytime."
                },
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.testTag("player_cancel_download_btn")
              ) {
                Text("Cancel", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
            } else {
              Text(
                text = textLoadError!!,
                color = AmberAccent,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
              )
              Surface(
                onClick = {
                  launchTextDownload(
                    priorityChapterIndex = currentChapterIndex,
                    hadChaptersAtOpen = activeBook.parsedChapters.isNotEmpty() || activeBook.librivoxEditions.isNotEmpty()
                  )
                },
                shape = RoundedCornerShape(8.dp),
                color = CyanAccent,
                modifier = Modifier.testTag("player_retry_text_btn")
              ) {
                Text("Retry Text", color = Color(0xFF041220), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
              }
            }
          }
          if (isLoadingText) {
            val frac = textDownloadProgress?.fraction
            Spacer(modifier = Modifier.height(4.dp))
            if (frac != null) {
              LinearProgressIndicator(
                progress = { frac },
                color = CyanAccent,
                trackColor = DeepNavyBorder,
                modifier = Modifier.fillMaxWidth().height(3.dp)
              )
            } else {
              LinearProgressIndicator(
                color = CyanAccent,
                trackColor = DeepNavyBorder,
                modifier = Modifier.fillMaxWidth().height(3.dp)
              )
            }
          }
        }
      }
    }

    if ((isLoadingAudio || !audioLoadError.isNullOrBlank()) && !activeBook.hasVerifiedHumanAudio) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DeepNavyCard,
        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.35f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (isLoadingAudio) {
            CircularProgressIndicator(color = AmberAccent, strokeWidth = 2.dp, modifier = Modifier.size(13.dp))
            Text(
              text = "Checking human audiobook recordings...",
              color = TextLight,
              fontSize = 11.sp,
              modifier = Modifier.weight(1f)
            )
          } else {
            Text(
              text = audioLoadError ?: "Human audiobook unavailable. AI voice is ready.",
              color = TextLight,
              fontSize = 11.sp,
              modifier = Modifier.weight(1f)
            )
            Surface(
              onClick = { launchAudioDiscovery() },
              shape = RoundedCornerShape(8.dp),
              color = AmberAccent.copy(alpha = 0.2f),
              modifier = Modifier.testTag("player_retry_audio_btn")
            ) {
              Text("Retry Audio", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
          }
        }
      }
    }

    // 3. MAIN CONTENT AREA BY MODE
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (isLoadingText && mappedParagraphs.isEmpty() && (playerMode == AudiobookPlayerMode.READ_AND_LISTEN || playerMode == AudiobookPlayerMode.READ_ONLY)) {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CircularProgressIndicator(color = CyanAccent)
          Text(
            text = textDownloadProgress?.let { prog ->
              if (prog.downloadedBytes > 0L) "Downloading $chapterTitle (${prog.formattedProgress})..." else "Loading $chapterTitle..."
            } ?: "Loading $chapterTitle...",
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
          val frac = textDownloadProgress?.fraction
          if (frac != null) {
            LinearProgressIndicator(
              progress = { frac },
              color = CyanAccent,
              trackColor = DeepNavyBorder,
              modifier = Modifier.fillMaxWidth(0.75f).height(4.dp)
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = {
                textLoadJob?.cancel()
                isLoadingText = false
                textDownloadProgress = null
                textLoadError = "Book text download cancelled. Tap Retry Text anytime."
              },
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Cancel Download", color = AmberAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (activeBook.hasVerifiedHumanAudio) {
              OutlinedButton(
                onClick = { playerMode = AudiobookPlayerMode.LISTEN_ONLY },
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Listen in Audio Mode", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        when (playerMode) {
          // ===================================================================
          // MODE 1: LISTEN ONLY (Book Cover, Chapter Info, Narrator Info, Edition Switcher)
          // ===================================================================
          AudiobookPlayerMode.LISTEN_ONLY -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Top
            ) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = grad.first(),
                border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.45f)),
                modifier = Modifier.size(width = 168.dp, height = 228.dp)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(grad)),
                  contentAlignment = Alignment.Center
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp)
                  ) {
                    Icon(Icons.Default.Headphones, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = activeBook.title,
                      color = TextWhite,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.ExtraBold,
                      textAlign = TextAlign.Center,
                      maxLines = 3
                    )
                  }
                  if (activeBook.coverArtUrl.isNotBlank()) {
                    AsyncImage(
                      model = activeBook.coverArtUrl,
                      contentDescription = activeBook.title,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Chapter ${currentChapterIndex + 1} of $totalChapters",
                color = EmeraldAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = chapterTitle,
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "${activeBook.author} • ${AudiobookMediaController.getActiveNarratorDisplay(activeBook)}",
                color = TextLight,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
              )

              // Edition toggle (Human LibriVox vs AI Voice) + status notice
              Spacer(modifier = Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                  selected = useHumanNarrator,
                  onClick = {
                    AudiobookMediaController.setNarrationEditionMode(
                      useHumanLibriVox = true,
                      autoStartPlayback = true
                    )
                  },
                  label = { Text("Human Narrator", fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
                    selectedLabelColor = AmberAccent,
                    containerColor = DeepNavyCard,
                    labelColor = TextMuted
                  )
                )
                FilterChip(
                  selected = !useHumanNarrator,
                  onClick = {
                    if (useHumanNarrator) {
                      AudiobookMediaController.setNarrationEditionMode(
                        useHumanLibriVox = false,
                        autoStartPlayback = isPlaying
                      )
                    } else {
                      showVoiceMenu = true
                    }
                  },
                  label = { Text("AI Voice (${selectedVoice.personaName} • ${selectedVoice.id})", fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
                    selectedLabelColor = CyanAccent,
                    containerColor = DeepNavyCard,
                    labelColor = TextMuted
                  ),
                  modifier = Modifier.testTag("listen_mode_ai_voice_btn")
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              FilterChip(
                selected = false,
                onClick = { showVoiceMenu = true },
                label = {
                  Text(
                    text = "Choose AI Voice: ${selectedVoice.personaName} (${selectedVoice.id})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                },
                leadingIcon = {
                  Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = DeepNavyCard,
                  labelColor = CyanAccent,
                  iconColor = CyanAccent
                ),
                modifier = Modifier.testTag("listen_mode_choose_voice_btn")
              )
              if (useHumanNarrator) {
                Spacer(modifier = Modifier.height(6.dp))
                FilterChip(
                  selected = skipRecordingAnnouncements,
                  onClick = {
                    AudiobookMediaController.setSkipRecordingAnnouncements(!skipRecordingAnnouncements)
                  },
                  label = {
                    Text(
                      text = if (skipRecordingAnnouncements) {
                        "Skip Recording Announcements: ON"
                      } else {
                        "Skip Recording Announcements: OFF"
                      },
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldAccent.copy(alpha = 0.20f),
                    selectedLabelColor = EmeraldAccent,
                    containerColor = DeepNavyCard,
                    labelColor = TextMuted
                  ),
                  modifier = Modifier.testTag("listen_mode_skip_announcements_chip")
                )
                if (skipRecordingAnnouncements && introAnnouncementSkipped && activeTrackBoundaries?.isVerified == true) {
                  Text(
                    text = activeTrackBoundaries?.introAnnouncementSummary?.takeIf { it.isNotBlank() }
                      ?: "Opening announcement skipped",
                    color = EmeraldAccent,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                  )
                }
              }
              if (!humanNarrationStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = humanNarrationStatusMessage!!,
                  color = AmberAccent,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          // ===================================================================
          // MODE 2: READ / READ & LISTEN (Premium Ebook Reader: Natural Paragraphs,
          // Light/Sepia/Dark Themes, Scroll or Page-by-Page Mode, Inline Highlight)
          // ===================================================================
          AudiobookPlayerMode.READ_ONLY, AudiobookPlayerMode.READ_AND_LISTEN -> {
            val themeBg = Color(readingTheme.backgroundHex)
            val themeSurface = Color(readingTheme.surfaceHex)
            val themeTextPrimary = Color(readingTheme.textPrimaryHex)
            val themeTextSecondary = Color(readingTheme.textSecondaryHex)
            val themeAccent = Color(readingTheme.accentHex)
            val themeHighlightBg = Color(readingTheme.highlightBgHex)
            val themeBorder = Color(readingTheme.borderHex)

            Column(modifier = Modifier.fillMaxSize()) {
              // Compact Ebook Reader Control Bar: Theme (Light/Sepia/Dark), Font A-/A+, Scroll/Pages, Auto-Scroll
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Reading Theme Selector: Light | Sepia | Dark
                EbookReadingTheme.entries.forEach { t ->
                  val isThemeSelected = readingTheme == t
                  Surface(
                    onClick = {
                      readingTheme = t
                      saveReaderPreferences()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(t.backgroundHex),
                    border = BorderStroke(
                      width = if (isThemeSelected) 1.5.dp else 1.dp,
                      color = if (isThemeSelected) CyanAccent else DeepNavyBorder
                    )
                  ) {
                    Text(
                      text = t.label,
                      color = Color(t.textPrimaryHex),
                      fontSize = 11.sp,
                      fontWeight = if (isThemeSelected) FontWeight.ExtraBold else FontWeight.Medium,
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                  }
                }

                // Font Size A- / A+
                Surface(
                  onClick = {
                    readerFontSizeSp = (readerFontSizeSp - 1.5f).coerceAtLeast(14f)
                    saveReaderPreferences()
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = DeepNavyCard,
                  border = BorderStroke(1.dp, DeepNavyBorder)
                ) {
                  Text("A-", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(
                  onClick = {
                    readerFontSizeSp = (readerFontSizeSp + 1.5f).coerceAtMost(26f)
                    saveReaderPreferences()
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = DeepNavyCard,
                  border = BorderStroke(1.dp, DeepNavyBorder)
                ) {
                  Text("A+", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                // Scroll vs Page-by-Page Reading Mode Toggle
                Surface(
                  onClick = {
                    isPageByPageMode = !isPageByPageMode
                    saveReaderPreferences()
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isPageByPageMode) CyanAccent.copy(alpha = 0.2f) else DeepNavyCard,
                  border = BorderStroke(1.dp, if (isPageByPageMode) CyanAccent else DeepNavyBorder)
                ) {
                  Text(
                    text = if (isPageByPageMode) "Mode: Pages" else "Mode: Scroll",
                    color = if (isPageByPageMode) CyanAccent else TextLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                  )
                }

                // Auto-Scroll / Sync Scroll Toggle
                Surface(
                  onClick = {
                    autoScrollEnabled = !autoScrollEnabled
                    if (autoScrollEnabled) {
                      autoScrollPausedByUser = false
                    }
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = if (autoScrollEnabled) EmeraldAccent.copy(alpha = 0.18f) else DeepNavyCard,
                  border = BorderStroke(1.dp, if (autoScrollEnabled) EmeraldAccent else DeepNavyBorder),
                  modifier = Modifier.testTag("auto_scroll_toggle_chip")
                ) {
                  val scrollLabel = if (useHumanNarrator) {
                    if (autoScrollEnabled) "Auto Scroll: ON" else "Auto Scroll: OFF"
                  } else {
                    if (autoScrollEnabled) "Sync Scroll: ON" else "Sync Scroll: OFF"
                  }
                  Text(
                    text = scrollLabel,
                    color = if (autoScrollEnabled) EmeraldAccent else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                  )
                }

                // Continuous Scroll Speed selector for Human LibriVox audio
                if (useHumanNarrator && !isPageByPageMode && autoScrollEnabled) {
                  Surface(
                    onClick = { continuousScrollSpeed = continuousScrollSpeed.next() },
                    shape = RoundedCornerShape(8.dp),
                    color = CyanAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("continuous_scroll_speed_chip")
                  ) {
                    Text(
                      text = "Scroll: ${continuousScrollSpeed.label}",
                      color = CyanAccent,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                  }
                }
              }

              // Interactive Selected Passage Coach & Dictionary/Translation Bar (when user taps a paragraph)
              if (selectedPassageForAi != null) {
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF142446)),
                  border = BorderStroke(1.dp, CyanAccent),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Selected Passage • AI Learning Tools",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                      )
                      IconButton(
                        onClick = {
                          selectedPassageForAi = null
                          aiPassageExplanation = null
                        },
                        modifier = Modifier.size(22.dp)
                      ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextLight, modifier = Modifier.size(15.dp))
                      }
                    }

                    Text(
                      text = "\"${selectedPassageForAi!!.take(160)}${if (selectedPassageForAi!!.length > 160) "..." else ""}\"",
                      color = TextLight,
                      fontSize = 11.5.sp,
                      fontStyle = FontStyle.Italic,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                      modifier = Modifier.horizontalScroll(rememberScrollState()),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      listOf(
                        "Translate" to "translate",
                        "Explain Meaning" to "meaning",
                        "Grammar" to "grammar",
                        "Key Vocabulary" to "vocabulary"
                      ).forEach { (lbl, modeKey) ->
                        Surface(
                          onClick = {
                            if (!isProUnlocked) {
                              com.example.ai.AiErrorHandler.requestUpgrade(
                                mode = com.example.ai.UpgradeSheetMode.AI_UPGRADE,
                                featureKey = "book_passage_ai",
                                headline = "Unlock AI Book Coach",
                                reason = "Upgrade to LingoSphere Pro for instant passage translations, grammar breakdowns, and vocabulary notes."
                              )
                              return@Surface
                            }
                            val passage = selectedPassageForAi ?: return@Surface
                            scope.launch {
                              isExplainingPassage = true
                              aiPassageExplanation = AudiobookLearningService.explainBookPassageWithAi(
                                selectedText = passage,
                                bookTitle = activeBook.title,
                                chapterTitle = chapterTitle,
                                mode = modeKey,
                                targetLanguage = targetLang,
                                nativeLanguage = nativeLang,
                                cefrLevel = cefrLevel
                              )
                              isExplainingPassage = false
                            }
                          },
                          shape = RoundedCornerShape(8.dp),
                          color = DeepNavyCard,
                          border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          ) {
                            if (!isProUnlocked) {
                              LingoGoldLockIcon(size = 11.dp)
                              Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(lbl, color = CyanAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }

                      // Save notable word from passage to flashcards
                      val notableWord = selectedPassageForAi!!
                        .split(Regex("[^A-Za-zÀ-ÿ]+"))
                        .firstOrNull { it.length >= 6 }
                        .orEmpty()
                      if (notableWord.isNotBlank()) {
                        Surface(
                          onClick = {
                            onSaveVocabToFlashcard(notableWord, selectedPassageForAi!!.take(120), "")
                            Toast.makeText(context, "Saved '$notableWord' to Flashcards!", Toast.LENGTH_SHORT).show()
                          },
                          shape = RoundedCornerShape(8.dp),
                          color = EmeraldAccent.copy(alpha = 0.2f),
                          border = BorderStroke(1.dp, EmeraldAccent)
                        ) {
                          Text(
                            text = "+ Save '$notableWord'",
                            color = EmeraldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      }
                    }

                    // Ask a custom question about the selected passage
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      OutlinedTextField(
                        value = customPassageQuestion,
                        onValueChange = { customPassageQuestion = it },
                        placeholder = { Text("Ask AI about this passage...", fontSize = 11.5.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                          focusedTextColor = TextWhite,
                          unfocusedTextColor = TextWhite,
                          focusedBorderColor = CyanAccent,
                          unfocusedBorderColor = DeepNavyBorder
                        ),
                        modifier = Modifier.weight(1f)
                      )
                      IconButton(
                        onClick = {
                          if (!isProUnlocked) {
                            com.example.ai.AiErrorHandler.requestUpgrade(
                              mode = com.example.ai.UpgradeSheetMode.AI_UPGRADE,
                              featureKey = "book_passage_ai",
                              headline = "Unlock AI Book Coach",
                              reason = "Upgrade to LingoSphere Pro to ask AI questions about any book passage."
                            )
                            return@IconButton
                          }
                          val passage = selectedPassageForAi ?: return@IconButton
                          val q = customPassageQuestion.trim()
                          if (q.isNotBlank()) {
                            scope.launch {
                              isExplainingPassage = true
                              aiPassageExplanation = AudiobookLearningService.explainBookPassageWithAi(
                                selectedText = "$passage\n\nReader Question: $q",
                                bookTitle = activeBook.title,
                                chapterTitle = chapterTitle,
                                mode = "meaning",
                                targetLanguage = targetLang,
                                nativeLanguage = nativeLang,
                                cefrLevel = cefrLevel
                              )
                              isExplainingPassage = false
                              customPassageQuestion = ""
                            }
                          }
                        },
                        modifier = Modifier
                          .size(38.dp)
                          .clip(CircleShape)
                          .background(CyanAccent)
                      ) {
                        Icon(Icons.Default.Send, contentDescription = "Ask AI", tint = Color(0xFF041220), modifier = Modifier.size(17.dp))
                      }
                    }

                    if (isExplainingPassage) {
                      Spacer(modifier = Modifier.height(6.dp))
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing passage with AI...", color = TextLight, fontSize = 12.sp)
                      }
                    } else if (!aiPassageExplanation.isNullOrBlank()) {
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(aiPassageExplanation!!, color = TextWhite, fontSize = 12.5.sp, lineHeight = 18.sp)
                    }
                  }
                }
              }

              // REAL EBOOK PAGE SURFACE (No bordered sentence cards!)
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = themeBg,
                border = BorderStroke(1.dp, themeBorder),
                modifier = Modifier.fillMaxSize()
              ) {
                if (isPageByPageMode) {
                  val safePageIdx = currentPageIndex.coerceIn(0, (ebookPages.size - 1).coerceAtLeast(0))
                  val pageParagraphs = ebookPages.getOrElse(safePageIdx) { emptyList() }

                  Column(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(horizontal = 20.dp, vertical = 14.dp)
                  ) {
                    // Page Top Header
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = chapterTitle,
                        color = themeTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                      )
                      Text(
                        text = "Page ${safePageIdx + 1} of ${ebookPages.size}",
                        color = themeAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = themeBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Natural Ebook Paragraphs on Current Page
                    Column(
                      modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                      verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                      if (safePageIdx == 0) {
                        Text(
                          text = chapterTitle,
                          color = themeTextPrimary,
                          fontSize = (readerFontSizeSp + 4f).sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Serif,
                          lineHeight = ((readerFontSizeSp + 4f) * 1.3f).sp
                        )
                      }

                      if (pageParagraphs.isEmpty()) {
                        Column(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                          if (isLoadingText) {
                            CircularProgressIndicator(
                              color = themeAccent,
                              strokeWidth = 2.5.dp,
                              modifier = Modifier.size(26.dp)
                            )
                            Text(
                              text = textDownloadProgress?.let { prog ->
                                if (prog.downloadedBytes > 0L) "Downloading book text (${prog.formattedProgress})..." else "Loading text for $chapterTitle..."
                              } ?: "Loading text for $chapterTitle...",
                              color = themeTextSecondary,
                              fontSize = 13.sp,
                              fontFamily = FontFamily.Serif,
                              textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                              onClick = {
                                textLoadJob?.cancel()
                                isLoadingText = false
                                textDownloadProgress = null
                                textLoadError = "Book text download cancelled. Tap Retry Text anytime."
                              },
                              shape = RoundedCornerShape(10.dp)
                            ) {
                              Text("Cancel", color = AmberAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                          } else {
                            Text(
                              text = textLoadError ?: "Original chapter text for $chapterTitle is not loaded yet.",
                              color = themeTextSecondary,
                              fontSize = 13.sp,
                              fontFamily = FontFamily.Serif,
                              textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                              onClick = {
                                launchTextDownload(
                                  priorityChapterIndex = currentChapterIndex,
                                  hadChaptersAtOpen = activeBook.parsedChapters.isNotEmpty() || activeBook.librivoxEditions.isNotEmpty()
                                )
                              },
                              shape = RoundedCornerShape(10.dp)
                            ) {
                              Icon(Icons.Default.Refresh, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                              Spacer(modifier = Modifier.width(6.dp))
                              Text("Retry Text", color = themeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (activeBook.hasVerifiedHumanAudio) {
                              OutlinedButton(
                                onClick = { playerMode = AudiobookPlayerMode.LISTEN_ONLY },
                                shape = RoundedCornerShape(10.dp)
                              ) {
                                Text("Listen in Audio Mode", color = themeAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                              }
                            }
                          }
                        }
                      } else {
                        pageParagraphs.forEach { para ->
                          NaturalBookParagraphItem(
                            para = para,
                            currentSentenceIndex = currentSentenceIndex,
                            useHumanNarrator = useHumanNarrator,
                            isPlaying = isPlaying,
                            fontSizeSp = readerFontSizeSp,
                            themeTextPrimary = themeTextPrimary,
                            themeTextSecondary = themeTextSecondary,
                            themeAccent = themeAccent,
                            themeHighlightBg = themeHighlightBg,
                            onParagraphClick = {
                              selectedPassageForAi = para.text
                              if (!useHumanNarrator) {
                                AudiobookMediaController.jumpToSentence(para.startSentenceIndex, playImmediately = isPlaying)
                              }
                            }
                          )
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = themeBorder)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Page Turn Controls at Bottom of Ebook Page
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedButton(
                        onClick = {
                          if (safePageIdx > 0) {
                            currentPageIndex = safePageIdx - 1
                            saveReaderPreferences(pageIdx = currentPageIndex)
                          } else if (currentChapterIndex > 0) {
                            AudiobookMediaController.previousChapter()
                          }
                        },
                        enabled = safePageIdx > 0 || currentChapterIndex > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Text(
                          text = if (safePageIdx > 0) "‹ Prev Page" else "‹ Prev Chapter",
                          color = themeTextPrimary,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.SemiBold
                        )
                      }

                      Text(
                        text = "Ch. ${currentChapterIndex + 1}/$totalChapters • p. ${safePageIdx + 1}/${ebookPages.size}",
                        color = themeTextSecondary,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                          .weight(1f)
                          .padding(horizontal = 4.dp)
                      )

                      OutlinedButton(
                        onClick = {
                          if (safePageIdx + 1 < ebookPages.size) {
                            currentPageIndex = safePageIdx + 1
                            saveReaderPreferences(pageIdx = currentPageIndex)
                          } else if (currentChapterIndex + 1 < totalChapters) {
                            AudiobookMediaController.nextChapter()
                          }
                        },
                        enabled = safePageIdx + 1 < ebookPages.size || currentChapterIndex + 1 < totalChapters,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Text(
                          text = if (safePageIdx + 1 < ebookPages.size) "Next Page ›" else "Next Chapter ›",
                          color = themeTextPrimary,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.SemiBold
                        )
                      }
                    }
                  }
                } else {
                  // Continuous Vertical Scroll Mode (Natural Book Layout)
                  Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                      state = listState,
                      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 44.dp),
                      verticalArrangement = Arrangement.spacedBy(18.dp),
                      modifier = Modifier.fillMaxSize()
                    ) {
                      // Chapter Heading inside the Book Page
                      item(key = "chapter_page_header_$currentChapterIndex") {
                        Column {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "CHAPTER ${currentChapterIndex + 1} OF $totalChapters",
                              color = themeAccent,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.ExtraBold,
                              letterSpacing = 1.sp
                            )
                            val syncNote = if (useHumanNarrator) {
                              "Human Audio • Continuous Scroll • Tap for AI"
                            } else {
                              "AI Voice Sync • Tap paragraph to play or explain"
                            }
                            Text(
                              text = syncNote,
                              color = themeTextSecondary,
                              fontSize = 10.5.sp
                            )
                          }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                          text = chapterTitle,
                          color = themeTextPrimary,
                          fontSize = (readerFontSizeSp + 5f).sp,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Serif,
                          lineHeight = ((readerFontSizeSp + 5f) * 1.3f).sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = themeBorder)
                      }
                    }

                    // Natural Book Paragraphs (NO cards or borders around sentences!)
                    itemsIndexed(
                      items = mappedParagraphs,
                      key = { pIdx, _ -> "para_${currentChapterIndex}_$pIdx" }
                    ) { _, para ->
                      NaturalBookParagraphItem(
                        para = para,
                        currentSentenceIndex = currentSentenceIndex,
                        useHumanNarrator = useHumanNarrator,
                        isPlaying = isPlaying,
                        fontSizeSp = readerFontSizeSp,
                        themeTextPrimary = themeTextPrimary,
                        themeTextSecondary = themeTextSecondary,
                        themeAccent = themeAccent,
                        themeHighlightBg = themeHighlightBg,
                        onParagraphClick = {
                          selectedPassageForAi = para.text
                          if (!useHumanNarrator) {
                            AudiobookMediaController.jumpToSentence(para.startSentenceIndex, playImmediately = isPlaying)
                          }
                        }
                      )
                    }

                    // End-of-Chapter Navigation Footer: only shown after the COMPLETE chapter text is loaded
                    item(key = "chapter_page_footer_$currentChapterIndex") {
                      val currentChapterObj = activeBook.parsedChapters.getOrNull(currentChapterIndex)
                      val isCurrentChapterComplete = !isLoadingText &&
                        mappedParagraphs.isNotEmpty() &&
                        (!activeBook.isFullBookEligible || (
                          currentChapterObj != null &&
                            GutendexAndLibriVoxService.isChapterCompleteEnough(currentChapterObj, true)
                          ))

                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(top = 12.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                        HorizontalDivider(color = themeBorder)
                        Spacer(modifier = Modifier.height(14.dp))
                        if (isCurrentChapterComplete) {
                          Text(
                            text = "End of $chapterTitle (${currentChapterIndex + 1} of $totalChapters)",
                            color = themeTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif
                          )
                          Spacer(modifier = Modifier.height(10.dp))
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                          ) {
                            OutlinedButton(
                              onClick = { AudiobookMediaController.previousChapter() },
                              enabled = currentChapterIndex > 0,
                              shape = RoundedCornerShape(10.dp)
                            ) {
                              Text("‹ Previous Chapter", color = themeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                              onClick = { AudiobookMediaController.nextChapter() },
                              enabled = currentChapterIndex + 1 < totalChapters,
                              colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.White),
                              shape = RoundedCornerShape(10.dp)
                            ) {
                              Text("Next Chapter ›", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        } else if (isLoadingText) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                          ) {
                            CircularProgressIndicator(
                              color = themeAccent,
                              strokeWidth = 2.dp,
                              modifier = Modifier.size(18.dp)
                            )
                            Text(
                              text = textDownloadProgress?.let { prog ->
                                if (prog.downloadedBytes > 0L) "Downloading book text (${prog.formattedProgress})..." else "Loading text for $chapterTitle..."
                              } ?: "Loading text for $chapterTitle...",
                              color = themeTextSecondary,
                              fontSize = 12.sp,
                              fontFamily = FontFamily.Serif
                            )
                          }
                        } else {
                          if (mappedParagraphs.isEmpty()) {
                            Text(
                              text = textLoadError ?: "Original chapter text for $chapterTitle is not loaded yet.",
                              color = themeTextSecondary,
                              fontSize = 13.sp,
                              fontFamily = FontFamily.Serif,
                              textAlign = TextAlign.Center,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                          }
                          OutlinedButton(
                            onClick = {
                              launchTextDownload(
                                priorityChapterIndex = currentChapterIndex,
                                hadChaptersAtOpen = activeBook.parsedChapters.isNotEmpty() || activeBook.librivoxEditions.isNotEmpty()
                              )
                            },
                            shape = RoundedCornerShape(10.dp)
                          ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = if (mappedParagraphs.isEmpty()) "Retry Text" else "Load Complete Chapter Text",
                              color = themeTextPrimary,
                              fontSize = 12.sp,
                              fontWeight = FontWeight.Bold
                            )
                          }
                          if (mappedParagraphs.isEmpty() && activeBook.hasVerifiedHumanAudio) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                              onClick = { playerMode = AudiobookPlayerMode.LISTEN_ONLY },
                              shape = RoundedCornerShape(10.dp)
                            ) {
                              Text("Listen in Audio Mode", color = themeAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }

                  if (autoScrollPausedByUser && autoScrollEnabled && isPlaying) {
                    Surface(
                      onClick = { autoScrollPausedByUser = false },
                      shape = RoundedCornerShape(24.dp),
                      color = DeepNavyElevated.copy(alpha = 0.95f),
                      border = BorderStroke(1.dp, CyanAccent),
                      shadowElevation = 6.dp,
                      modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .testTag("resume_auto_scroll_btn")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.PlayArrow,
                          contentDescription = "Resume Auto-Scroll",
                          tint = CyanAccent,
                          modifier = Modifier.size(16.dp)
                        )
                        Text(
                          text = "Resume Auto-Scroll",
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

          // ===================================================================
          // MODE 3: LEARN WITH AI (Smart Book Learning)
          // ===================================================================
          AudiobookPlayerMode.LEARN_MODE -> {
            LazyColumn(
              contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              item {
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF122449)),
                  border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (!isProUnlocked) {
                        LingoGoldLockIcon(size = 18.dp)
                      } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "AI Smart Book Coach • $chapterTitle",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                      )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!isProUnlocked) {
                      Text(
                        text = "Unlock AI chapter summaries, key vocabulary packs, and guided comprehension drills with LingoSphere Pro.",
                        color = TextLight,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                      )
                      Spacer(modifier = Modifier.height(10.dp))
                      Button(
                        onClick = {
                          com.example.ai.AiErrorHandler.requestUpgrade(
                            mode = com.example.ai.UpgradeSheetMode.AI_UPGRADE,
                            featureKey = "book_study_pack",
                            headline = "Unlock AI Smart Book Coach",
                            reason = "Upgrade to LingoSphere Pro for chapter AI study packs, instant passage explanations, and quizzes."
                          )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                      ) {
                        Text("Unlock AI Book Coach", fontWeight = FontWeight.ExtraBold)
                      }
                    } else if (isLoadingStudyPack) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Generating vocabulary, grammar & exercises for $chapterTitle...", color = TextLight, fontSize = 12.5.sp)
                      }
                    } else {
                      Text(
                        text = chapterStudyPack?.summaryExplanation
                          ?: "Explore key vocabulary, grammar structures, comprehension questions, and speaking exercises tailored to $chapterTitle.",
                        color = TextLight,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                      )
                    }
                  }
                }
              }

              // Difficult Words & Vocabulary Practice
              val vocabList = chapterStudyPack?.difficultWords.orEmpty()
              if (vocabList.isNotEmpty()) {
                item {
                  Text(
                    text = "Chapter Vocabulary Practice",
                    color = AmberAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
                itemsIndexed(vocabList) { _, vLine ->
                  Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                    border = BorderStroke(1.dp, DeepNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(vLine.keyVocabWord, color = CyanAccent, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
                        Text(vLine.keyVocabMeaning, color = TextWhite, fontSize = 12.5.sp)
                        if (vLine.targetText.isNotBlank()) {
                          Spacer(modifier = Modifier.height(2.dp))
                          Text("\"${vLine.targetText}\"", color = TextMuted, fontSize = 11.5.sp)
                        }
                      }
                      IconButton(
                        onClick = {
                          speechManager.speakWithTutorVoice(
                            text = vLine.keyVocabWord,
                            voice = selectedVoice,
                            languageCode = targetLang.code,
                            persistAsPartnerPreference = false
                          )
                        }
                      ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce", tint = CyanAccent)
                      }
                      Button(
                        onClick = {
                          onSaveVocabToFlashcard(vLine.keyVocabWord, vLine.keyVocabMeaning, "")
                          onAddXp(5L)
                          Toast.makeText(context, "Saved '${vLine.keyVocabWord}' to Flashcards (+5 XP)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color(0xFF041220)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                      ) {
                        Text("Save", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              // Grammar Explanations
              val grammarNotes = chapterStudyPack?.grammarNotes.orEmpty()
              if (grammarNotes.isNotEmpty()) {
                item {
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                      Text("Grammar in this Chapter", color = PurpleAccent, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                      Spacer(modifier = Modifier.height(6.dp))
                      grammarNotes.forEach { note ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                          Text("•", color = PurpleAccent, fontSize = 13.sp)
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(note, color = TextLight, fontSize = 12.5.sp, lineHeight = 17.sp)
                        }
                      }
                    }
                  }
                }
              }

              // Chapter Comprehension Questions
              val questions = chapterStudyPack?.comprehensionQuestions
                ?.ifEmpty { activeBook.episodes.getOrNull(currentChapterIndex)?.comprehensionQuestions.orEmpty() }
                .orEmpty()

              if (questions.isNotEmpty()) {
                item {
                  Text(
                    text = "Chapter Comprehension Check",
                    color = CyanAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
                itemsIndexed(questions) { qIdx, q ->
                  val chosenOpt = selectedQuizAnswers[qIdx]
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                    border = BorderStroke(1.dp, DeepNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                      Text(
                        text = "Q${qIdx + 1}: ${q.question}",
                        color = TextWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      q.options.forEachIndexed { optIdx, optText ->
                        val isSelected = chosenOpt == optIdx
                        val isCorrect = optIdx == q.correctIndex
                        val bg = when {
                          chosenOpt == null -> DeepNavyElevated
                          isCorrect -> EmeraldAccent.copy(alpha = 0.22f)
                          isSelected -> Color(0xFFEF4444).copy(alpha = 0.22f)
                          else -> DeepNavyElevated
                        }
                        Surface(
                          onClick = {
                            if (chosenOpt == null) {
                              selectedQuizAnswers = selectedQuizAnswers + (qIdx to optIdx)
                              if (optIdx == q.correctIndex) {
                                onAddXp(15L)
                              }
                            }
                          },
                          shape = RoundedCornerShape(10.dp),
                          color = bg,
                          border = BorderStroke(
                            1.dp,
                            if (chosenOpt != null && isCorrect) EmeraldAccent else DeepNavyBorder
                          ),
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                        ) {
                          Text(
                            text = optText,
                            color = TextWhite,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(10.dp)
                          )
                        }
                      }
                      if (chosenOpt != null && q.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                          text = q.explanation,
                          color = EmeraldAccent,
                          fontSize = 12.sp
                        )
                      }
                    }
                  }
                }
              }

              // Interactive Speaking & Book Conversation Exercise
              item {
                val speakPrompt = chapterStudyPack?.speakingPrompt?.ifBlank {
                  "Describe in your own words what happened in $chapterTitle of ${activeBook.title}."
                } ?: "Describe in your own words what happened in $chapterTitle."

                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                  border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                      text = "Speaking Practice Based on This Chapter",
                      color = EmeraldAccent,
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = speakPrompt,
                      color = TextWhite,
                      fontSize = 13.sp,
                      lineHeight = 18.sp
                    )
                    if (!chapterStudyPack?.conversationStarter.isNullOrBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "Discussion Prompt: ${chapterStudyPack?.conversationStarter}",
                        color = TextMuted,
                        fontSize = 12.sp
                      )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Button(
                        onClick = {
                          if (isListeningForSpeakingPrompt) {
                            speechManager.stopListening()
                            isListeningForSpeakingPrompt = false
                            onAddXp(20L)
                            Toast.makeText(context, "Great speaking practice! +20 XP", Toast.LENGTH_SHORT).show()
                          } else {
                            isListeningForSpeakingPrompt = true
                            speechManager.startListening(targetLang.code)
                          }
                        },
                        colors = ButtonDefaults.buttonColors(
                          containerColor = if (isListeningForSpeakingPrompt) Color(0xFFEF4444) else EmeraldAccent,
                          contentColor = Color(0xFF041220)
                        ),
                        shape = RoundedCornerShape(12.dp)
                      ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = if (isListeningForSpeakingPrompt) "Finish Speaking" else "Practice Speaking",
                          fontSize = 12.5.sp,
                          fontWeight = FontWeight.ExtraBold
                        )
                      }
                    }
                    if (userSpokenResponse.isNotBlank()) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Text(
                        text = "Your response: \"$userSpokenResponse\"",
                        color = CyanAccent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
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

    Spacer(modifier = Modifier.height(6.dp))

    // 4. BOTTOM CONTROLS: Distraction-free Reading Footer in READ_ONLY, Full Audio Dock in Listen / Read & Listen / AI Study
    if (playerMode == AudiobookPlayerMode.READ_ONLY) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxWidth()
          .testTag("audiobook_read_only_footer_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              if (currentChapterIndex > 0) {
                val prev = currentChapterIndex - 1
                onChapterChanged(prev)
                AudiobookMediaController.loadBookAndChapter(activeBook, prev, 0, autoPlay = false)
                saveReaderPreferences(chIndex = prev, scrollIdx = 0, pageIdx = 0)
              }
            },
            enabled = currentChapterIndex > 0,
            modifier = Modifier.size(44.dp).testTag("read_only_prev_chapter_btn")
          ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous chapter", tint = if (currentChapterIndex > 0) TextWhite else TextMuted)
          }

          Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Ch. ${currentChapterIndex + 1} / $totalChapters",
              color = TextWhite,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
            Text(
              text = "Reading progress saved separately",
              color = EmeraldAccent,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Surface(
            onClick = { playerMode = AudiobookPlayerMode.READ_AND_LISTEN },
            shape = RoundedCornerShape(10.dp),
            color = CyanAccent.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.55f)),
            modifier = Modifier.testTag("read_only_switch_audio_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Headphones, contentDescription = "Add audio", tint = CyanAccent, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("Listen", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          IconButton(
            onClick = {
              if (currentChapterIndex + 1 < totalChapters) {
                val next = currentChapterIndex + 1
                onChapterChanged(next)
                AudiobookMediaController.loadBookAndChapter(activeBook, next, 0, autoPlay = false)
                saveReaderPreferences(chIndex = next, scrollIdx = 0, pageIdx = 0)
                onAddXp(10L)
              }
            },
            enabled = currentChapterIndex + 1 < totalChapters,
            modifier = Modifier.size(44.dp).testTag("read_only_next_chapter_btn")
          ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next chapter", tint = if (currentChapterIndex + 1 < totalChapters) TextWhite else TextMuted)
          }
        }
      }
    } else {
    val secondaryRowScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
      border = BorderStroke(1.dp, DeepNavyBorder),
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxWidth()
        .testTag("audiobook_bottom_player_card")
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompactWidth = maxWidth < 340.dp
        val controlPaddingH = if (isCompactWidth) 10.dp else 12.dp
        val sideBtnSize = if (isCompactWidth) 36.dp else 42.dp
        val playBtnSize = if (isCompactWidth) 44.dp else 48.dp

        Column(modifier = Modifier.padding(horizontal = controlPaddingH, vertical = 8.dp)) {
          if (!humanNarrationStatusMessage.isNullOrBlank()) {
            Text(
              text = humanNarrationStatusMessage!!,
              color = AmberAccent,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
            )
          }

          // Progress Slider & Time / Sentence Counter
          val progressFraction = if (useHumanNarrator) {
            if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
          } else {
            if (sentences.isNotEmpty()) (currentSentenceIndex.toFloat() / (sentences.size - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) else 0f
          }

          Slider(
            value = progressFraction,
            onValueChange = { frac ->
              AudiobookMediaController.seekToFraction(frac)
            },
            colors = SliderDefaults.colors(
              thumbColor = CyanAccent,
              activeTrackColor = CyanAccent,
              inactiveTrackColor = DeepNavyBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(20.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            val leftLabel = if (useHumanNarrator) {
              formatDurationMs(currentPositionMs)
            } else {
              "Sentence ${currentSentenceIndex + 1} / ${sentences.size.coerceAtLeast(1)}"
            }
            val rightLabel = if (useHumanNarrator) {
              if (durationMs > 0L) formatDurationMs(durationMs)
              else activeBook.parsedChapters.getOrNull(currentChapterIndex)?.formattedDuration?.takeIf { it.isNotBlank() } ?: "--:--"
            } else {
              activeBook.parsedChapters.getOrNull(currentChapterIndex)?.formattedDuration?.takeIf { it.isNotBlank() }
                ?: "${sentences.size.coerceAtLeast(1)} sentences"
            }
            Text(leftLabel, color = TextMuted, fontSize = 10.5.sp, maxLines = 1)
            Text(rightLabel, color = TextMuted, fontSize = 10.5.sp, maxLines = 1)
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Main Transport Row: Prev Chapter | -10s | Play/Pause | +10s | Next Chapter (always visible & unclipped)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("player_transport_controls_row"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { AudiobookMediaController.previousChapter() },
              enabled = currentChapterIndex > 0,
              modifier = Modifier
                .size(sideBtnSize)
                .testTag("player_prev_chapter_btn")
            ) {
              Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous Chapter",
                tint = if (currentChapterIndex > 0) TextWhite else TextMuted,
                modifier = Modifier.size(if (isCompactWidth) 22.dp else 24.dp)
              )
            }

            IconButton(
              onClick = { AudiobookMediaController.skipSeconds(forward = false) },
              modifier = Modifier
                .size(sideBtnSize)
                .testTag("player_rewind_10_btn")
            ) {
              Icon(
                Icons.Default.Replay10,
                contentDescription = "Backward 10 seconds",
                tint = TextWhite,
                modifier = Modifier.size(if (isCompactWidth) 22.dp else 24.dp)
              )
            }

            Surface(
              onClick = { AudiobookMediaController.togglePlayPause() },
              shape = CircleShape,
              color = CyanAccent,
              modifier = Modifier
                .size(playBtnSize)
                .testTag("player_play_pause_btn")
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (isBuffering) {
                  CircularProgressIndicator(
                    color = Color(0xFF041220),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                  )
                } else {
                  Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF041220),
                    modifier = Modifier.size(26.dp)
                  )
                }
              }
            }

            IconButton(
              onClick = { AudiobookMediaController.skipSeconds(forward = true) },
              modifier = Modifier
                .size(sideBtnSize)
                .testTag("player_forward_10_btn")
            ) {
              Icon(
                Icons.Default.Forward10,
                contentDescription = "Forward 10 seconds",
                tint = TextWhite,
                modifier = Modifier.size(if (isCompactWidth) 22.dp else 24.dp)
              )
            }

            IconButton(
              onClick = { AudiobookMediaController.nextChapter() },
              enabled = currentChapterIndex + 1 < totalChapters,
              modifier = Modifier
                .size(sideBtnSize)
                .testTag("player_next_chapter_btn")
            ) {
              Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next Chapter",
                tint = if (currentChapterIndex + 1 < totalChapters) TextWhite else TextMuted,
                modifier = Modifier.size(if (isCompactWidth) 22.dp else 24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Secondary Controls Row: Smooth horizontal scroll so Human/AI Voice, Speed, Autoplay, Skip Intros, Sleep are never clipped
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier
                .weight(1f)
                .horizontalScroll(secondaryRowScrollState)
                .testTag("player_secondary_controls_row"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // 1. Human / AI Voice Selector
              Surface(
                onClick = { showVoiceMenu = true },
                shape = RoundedCornerShape(8.dp),
                color = if (useHumanNarrator) DeepNavyElevated else CyanAccent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, if (useHumanNarrator) CyanAccent.copy(alpha = 0.65f) else CyanAccent),
                modifier = Modifier.testTag("player_voice_selector_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Choose AI Voice or Human Narrator",
                    tint = if (useHumanNarrator) AmberAccent else CyanAccent,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = if (useHumanNarrator) "Human • AI Voice" else "AI: ${selectedVoice.personaName}",
                    color = if (useHumanNarrator) TextWhite else CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }

              // 2. Speed Picker (0.75x to 2.0x)
              Box {
                Surface(
                  onClick = { showSpeedMenu = true },
                  shape = RoundedCornerShape(8.dp),
                  color = DeepNavyElevated,
                  border = BorderStroke(1.dp, DeepNavyBorder),
                  modifier = Modifier.testTag("player_speed_btn")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Speed, contentDescription = "Playback speed", tint = CyanAccent, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${playbackSpeed}x", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                  }
                }
                DropdownMenu(
                  expanded = showSpeedMenu,
                  onDismissRequest = { showSpeedMenu = false },
                  modifier = Modifier.background(DeepNavyElevated)
                ) {
                  listOf(0.75f, 0.9f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                    DropdownMenuItem(
                      text = { Text("${spd}x Speed", color = if (spd == playbackSpeed) CyanAccent else TextWhite) },
                      onClick = {
                        showSpeedMenu = false
                        AudiobookMediaController.setPlaybackSpeed(spd)
                      }
                    )
                  }
                }
              }

              // 3. Autoplay Next Chapter Toggle
              Surface(
                onClick = {
                  AudiobookMediaController.setAutoPlayNextChapter(!autoPlayNextChapter)
                },
                shape = RoundedCornerShape(8.dp),
                color = if (autoPlayNextChapter) EmeraldAccent.copy(alpha = 0.18f) else DeepNavyElevated,
                border = BorderStroke(1.dp, if (autoPlayNextChapter) EmeraldAccent else DeepNavyBorder),
                modifier = Modifier.testTag("player_autoplay_btn")
              ) {
                Text(
                  text = if (autoPlayNextChapter) "Autoplay: ON" else "Autoplay: OFF",
                  color = if (autoPlayNextChapter) EmeraldAccent else TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }

              // 4. Skip Recording Announcements Toggle
              Surface(
                onClick = {
                  AudiobookMediaController.setSkipRecordingAnnouncements(!skipRecordingAnnouncements)
                },
                shape = RoundedCornerShape(8.dp),
                color = if (skipRecordingAnnouncements) EmeraldAccent.copy(alpha = 0.18f) else DeepNavyElevated,
                border = BorderStroke(1.dp, if (skipRecordingAnnouncements) EmeraldAccent else DeepNavyBorder),
                modifier = Modifier
                  .semantics {
                    contentDescription = if (skipRecordingAnnouncements) {
                      "Skip Recording Announcements: ON"
                    } else {
                      "Skip Recording Announcements: OFF"
                    }
                  }
                  .testTag("toggle_skip_announcements_chip")
              ) {
                Text(
                  text = if (skipRecordingAnnouncements) "Skip Intros: ON" else "Skip Intros: OFF",
                  color = if (skipRecordingAnnouncements) EmeraldAccent else TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }

              // 5. Sleep Timer Picker
              Box {
                Surface(
                  onClick = { showSleepTimerMenu = true },
                  shape = RoundedCornerShape(8.dp),
                  color = DeepNavyElevated,
                  border = BorderStroke(1.dp, DeepNavyBorder),
                  modifier = Modifier.testTag("player_sleep_timer_btn")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Timer, contentDescription = "Sleep timer", tint = AmberAccent, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Sleep",
                      color = if (sleepTimerMinutes > 0) AmberAccent else TextWhite,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
                DropdownMenu(
                  expanded = showSleepTimerMenu,
                  onDismissRequest = { showSleepTimerMenu = false },
                  modifier = Modifier.background(DeepNavyElevated)
                ) {
                  listOf(0, 5, 10, 15, 30, 45, 60).forEach { mins ->
                    DropdownMenuItem(
                      text = {
                        Text(
                          text = if (mins == 0) "Off" else "$mins minutes",
                          color = if (mins == sleepTimerMinutes) AmberAccent else TextWhite
                        )
                      },
                      onClick = {
                        showSleepTimerMenu = false
                        AudiobookMediaController.setSleepTimer(mins)
                      }
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.width(4.dp))
            }

            if (secondaryRowScrollState.canScrollForward) {
              IconButton(
                onClick = {
                  coroutineScope.launch {
                    secondaryRowScrollState.animateScrollBy(180f)
                  }
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Scroll for more audio settings",
                  tint = CyanAccent,
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

  if (showVoiceMenu) {
    val chapterHasHuman = AudiobookMediaController.getChapterMp3Url(activeBook, currentChapterIndex) != null
    SharedVoiceSelectorBottomSheet(
      title = "Audiobook Narrator Voice",
      subtitle = "Compare & switch between Human and AI voices",
      selectedVoice = selectedVoice,
      isHumanNarratorSelected = useHumanNarrator,
      includeHumanNarratorOption = activeBook.hasHumanNarration || chapterHasHuman,
      humanNarratorAvailable = chapterHasHuman,
      targetLang = targetLang,
      speechManager = speechManager,
      onSelectHumanNarrator = {
        showVoiceMenu = false
        AudiobookMediaController.setNarrationEditionMode(
          useHumanLibriVox = true,
          autoStartPlayback = isPlaying || true
        )
      },
      onApplyVoice = { chosenVoice ->
        showVoiceMenu = false
        AudiobookMediaController.setTutorVoice(chosenVoice)
        if (useHumanNarrator) {
          AudiobookMediaController.setNarrationEditionMode(
            useHumanLibriVox = false,
            autoStartPlayback = isPlaying
          )
        }
      },
      onDismiss = { showVoiceMenu = false }
    )
  }
}

/**
 * Renders a natural ebook paragraph (without any card or box border!) with inline sentence
 * highlighting when synchronized AI voice narration is active.
 */
@Composable
private fun NaturalBookParagraphItem(
  para: MappedBookParagraph,
  currentSentenceIndex: Int,
  useHumanNarrator: Boolean,
  isPlaying: Boolean,
  fontSizeSp: Float,
  themeTextPrimary: Color,
  themeTextSecondary: Color,
  themeAccent: Color,
  themeHighlightBg: Color,
  onParagraphClick: () -> Unit
) {
  val isActiveParagraph = currentSentenceIndex in para.startSentenceIndex..para.endSentenceIndex

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onParagraphClick() }
  ) {
    if (!useHumanNarrator && isActiveParagraph && para.sentenceList.size > 1) {
      // Inline sentence highlight within the natural paragraph flow
      val annotated = buildAnnotatedString {
        para.sentenceList.forEachIndexed { idx, (sIdx, sText) ->
          if (sIdx == currentSentenceIndex) {
            withStyle(
              SpanStyle(
                background = themeHighlightBg,
                color = themeTextPrimary,
                fontWeight = FontWeight.SemiBold
              )
            ) {
              append(sText)
            }
          } else {
            withStyle(SpanStyle(color = themeTextPrimary)) {
              append(sText)
            }
          }
          if (idx < para.sentenceList.lastIndex) {
            append(" ")
          }
        }
      }
      Text(
        text = annotated,
        fontSize = fontSizeSp.sp,
        fontFamily = FontFamily.Serif,
        lineHeight = (fontSizeSp * 1.62f).sp
      )
    } else {
      val annotated = buildAnnotatedString {
        if (!useHumanNarrator && isActiveParagraph) {
          withStyle(
            SpanStyle(
              background = themeHighlightBg,
              color = themeTextPrimary,
              fontWeight = FontWeight.SemiBold
            )
          ) {
            append(para.text)
          }
        } else {
          withStyle(SpanStyle(color = themeTextPrimary)) {
            append(para.text)
          }
        }
      }
      Text(
        text = annotated,
        fontSize = fontSizeSp.sp,
        fontFamily = FontFamily.Serif,
        lineHeight = (fontSizeSp * 1.62f).sp
      )
    }

    if (para.nativeTranslation.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = para.nativeTranslation,
        color = themeTextSecondary,
        fontSize = (fontSizeSp * 0.82f).sp,
        fontStyle = FontStyle.Italic,
        lineHeight = (fontSizeSp * 1.3f).sp
      )
    }
  }
}

/**
 * Persistent Mini Player Bar displayed when an audiobook is active in the background.
 */
@Composable
fun AudiobookMiniPlayerBar(
  onExpandPlayer: (AudiobookBook, Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val activeBook by AudiobookMediaController.activeBook.collectAsState()
  val activeChapterIndex by AudiobookMediaController.activeChapterIndex.collectAsState()
  val isPlaying by AudiobookMediaController.isPlaying.collectAsState()
  val isBuffering by AudiobookMediaController.isBuffering.collectAsState()

  val book = activeBook ?: return
  val chapterTitle = AudiobookMediaController.getChapterTitle(book, activeChapterIndex)

  Surface(
    onClick = { onExpandPlayer(book, activeChapterIndex) },
    shape = RoundedCornerShape(22.dp),
    color = Color(0xF00E1525),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    shadowElevation = 14.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("audiobook_mini_player_bar")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DeepNavyElevated,
        modifier = Modifier.size(width = 40.dp, height = 52.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.Headphones, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
          if (book.coverArtUrl.isNotBlank()) {
            AsyncImage(
              model = book.coverArtUrl,
              contentDescription = book.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = book.title,
          color = TextWhite,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = chapterTitle,
          color = Color(0xFFA8B3CF),
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      IconButton(
        onClick = { AudiobookMediaController.togglePlayPause() },
        modifier = Modifier.size(44.dp)
      ) {
        if (isBuffering) {
          CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = TextWhite,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      IconButton(
        onClick = { AudiobookMediaController.stopAndClearMiniPlayer() },
        modifier = Modifier.size(44.dp)
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close mini player", tint = Color(0xFFA8B3CF), modifier = Modifier.size(18.dp))
      }
    }
  }
}

private fun formatDurationMs(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val mins = totalSec / 60L
  val secs = totalSec % 60L
  return "%02d:%02d".format(mins, secs)
}
