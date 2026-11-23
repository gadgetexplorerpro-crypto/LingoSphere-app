package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AudiobookGenerationResult
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.BookContentType
import com.example.data.model.FullBookChapter
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.data.repository.AudiobookCatalogRepository
import com.example.data.repository.BookDownloadFailureReason
import com.example.data.repository.BookDownloadProgress
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.speech.AudiobookMediaController
import com.example.speech.SpeechManager
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val DeepNavyBg = Color(0xFF060D1E)
private val DeepNavyCard = Color(0xFF0F1B38)
private val DeepNavyElevated = Color(0xFF14244A)
private val DeepNavyBorder = Color(0xFF1E305C)
private val CyanAccent = Color(0xFF06B6D4)
private val EmeraldAccent = Color(0xFF10B981)
private val AmberAccent = Color(0xFFF59E0B)
private val PurpleAccent = Color(0xFF8B5CF6)
private val RoseAccent = Color(0xFFF43F5E)
private val TextWhite = Color(0xFFFFFFFF)
private val TextLight = Color(0xFFCBD5E1)
private val TextMuted = Color(0xFF94A3B8)

/**
 * Dedicated Book Detail Screen:
 * - Displays book cover and title at the top.
 * - Shows real total chapter count and overall listening/reading progress.
 * - Uses LazyColumn for smooth lazy loading of all chapters from first to final (supporting 100-200+ chapters).
 * - Each chapter row displays its number, title (Preface, Introduction, Chapter 1..N, Epilogue, Appendix),
 *   progress status, duration, and play/read controls in a clean, uncluttered layout.
 */
@Composable
fun BookDetailScreen(
  book: AudiobookBook,
  userId: String,
  targetLang: Language,
  speechManager: SpeechManager? = null,
  isProUnlocked: Boolean = true,
  isBookAllowanceUnlocked: Boolean = true,
  onOpenSubscription: () -> Unit = {},
  onBookLoadedWithChapters: (AudiobookBook) -> Unit = {},
  onPlayEpisode: (episodeIndex: Int) -> Unit,
  onReadChapter: (chapterIndex: Int) -> Unit = { onPlayEpisode(it) },
  onReadOnlyChapter: (chapterIndex: Int) -> Unit = { onReadChapter(it) },
  onListenOnlyChapter: (chapterIndex: Int) -> Unit = { onPlayEpisode(it) },
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val ownedSpeechManager = remember(speechManager) {
    if (speechManager == null) SpeechManager(context.applicationContext) else null
  }
  val activeSpeechManager = speechManager ?: ownedSpeechManager!!
  DisposableEffect(ownedSpeechManager) {
    onDispose {
      ownedSpeechManager?.release()
    }
  }

  var activeBook by remember(book.id) { mutableStateOf(book) }
  val isBookUnlockedForCurrentUser = isProUnlocked || isBookAllowanceUnlocked
  var isLoadingText by remember(book.id) { mutableStateOf(false) }
  var textDownloadProgress by remember(book.id) { mutableStateOf<BookDownloadProgress?>(null) }
  var textLoadError by remember(book.id) { mutableStateOf<String?>(null) }
  var textLoadJob by remember(book.id) { mutableStateOf<Job?>(null) }

  var isLoadingAudio by remember(book.id) { mutableStateOf(false) }
  var audioChecked by remember(book.id) { mutableStateOf(book.hasVerifiedHumanAudio) }
  var audioLoadError by remember(book.id) { mutableStateOf<String?>(null) }
  var audioLoadJob by remember(book.id) { mutableStateOf<Job?>(null) }

  var showVoiceSelectorSheet by remember(book.id) { mutableStateOf(false) }

  var isSaved by remember(book.id) {
    mutableStateOf(AudiobookLearningService.isBookInLibrary(context, userId, book.id))
  }
  var isFavorite by remember(book.id) {
    mutableStateOf(AudiobookLearningService.isBookFavorite(context, userId, book.id))
  }
  val controllerBook by AudiobookMediaController.activeBook.collectAsState()
  val controllerChapterIdx by AudiobookMediaController.activeChapterIndex.collectAsState()
  val skipRecordingAnnouncements by AudiobookMediaController.skipRecordingAnnouncements.collectAsState()
  val selectedAudiobookVoice by AudiobookMediaController.selectedVoice.collectAsState()

  var hydratedProgress by remember(book.id) {
    mutableStateOf(AudiobookLearningService.loadUserProgress(context, userId, activeBook.id))
  }
  LaunchedEffect(book.id, userId, activeBook.parsedChapters.size, controllerBook?.id, controllerChapterIdx) {
    hydratedProgress = AudiobookLearningService.hydrateBookProgressFromFirebase(context, userId, activeBook.id)
  }
  val savedProg = hydratedProgress

  var preferHumanNarrator by remember(book.id, activeBook.hasVerifiedHumanAudio) {
    mutableStateOf(
      if (savedProg.preferredEditionId == "tts") false
      else activeBook.hasVerifiedHumanAudio
    )
  }

  fun launchTextDownload(priorityChapterIndex: Int) {
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
            val matches = ready.book.id == book.id ||
              ready.book.id == activeBook.id ||
              GutendexAndLibriVoxService.isSameBookTitleAndAuthor(ready.book.title, ready.book.author, book.title, book.author)
            if (matches && ready.chapters.isNotEmpty()) {
              val existingSections = activeBook.librivoxEditions.firstOrNull { it.sections.isNotEmpty() }?.sections.orEmpty()
              val mergedCh = if (existingSections.isNotEmpty()) {
                GutendexAndLibriVoxService.alignChaptersWithLibriVox(ready.chapters, existingSections, ready.book.title)
              } else ready.chapters
              val updated = ready.book.copy(
                parsedChapters = mergedCh,
                librivoxEditions = if (activeBook.librivoxEditions.isNotEmpty()) activeBook.librivoxEditions else ready.book.librivoxEditions
              )
              activeBook = updated
              AudiobookMediaController.updateActiveBookMetadata(updated)
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
            val existingSections = activeBook.librivoxEditions.firstOrNull { it.sections.isNotEmpty() }?.sections.orEmpty()
            val mergedCh = if (existingSections.isNotEmpty()) {
              GutendexAndLibriVoxService.alignChaptersWithLibriVox(textRes.chapters, existingSections, textRes.book.title)
            } else textRes.chapters
            val merged = textRes.book.copy(
              parsedChapters = mergedCh,
              librivoxEditions = if (activeBook.librivoxEditions.isNotEmpty()) activeBook.librivoxEditions else textRes.book.librivoxEditions,
              contentType = if (activeBook.hasVerifiedHumanAudio) BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK else textRes.book.contentType
            )
            activeBook = merged
            AudiobookMediaController.updateActiveBookMetadata(merged)
            onBookLoadedWithChapters(merged)
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
        audioChecked = true
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
            if (savedProg.preferredEditionId != "tts") {
              preferHumanNarrator = true
            }
            AudiobookMediaController.updateActiveBookMetadata(merged)
            onBookLoadedWithChapters(merged)
            audioLoadError = null
          } else {
            audioLoadError = audioRes.errorMessage ?: "Human audiobook unavailable for this title. AI narration is ready."
            preferHumanNarrator = false
          }
        }
      } catch (_: CancellationException) {
        isLoadingAudio = false
        audioLoadError = "Audio check cancelled. Tap Retry Audio anytime."
      }
    }
  }

  DisposableEffect(book.id) {
    onDispose {
      textLoadJob?.cancel()
      audioLoadJob?.cancel()
    }
  }

  // Run independent Text download & Audio lookup concurrently so neither blocks or hides the other
  LaunchedEffect(book.id) {
    AudiobookMediaController.attach(context, activeSpeechManager, userId)
    AudiobookMediaController.onBookOpened(book)
    AudiobookLearningService.recordBookOpened(context, userId, book)
    if (book.isFullBookEligible) {
      val targetChIdx = savedProg.lastEpisodeIndex.coerceAtLeast(0)
      launchTextDownload(targetChIdx)
      launchAudioDiscovery()
    }
  }

  val grad = activeBook.coverGradientColors.map { Color(it) }.ifEmpty { listOf(DeepNavyCard, DeepNavyElevated) }

  // Build unified ordered chapter list from parsedChapters or verified audio sections
  val displayChapters: List<FullBookChapter> = remember(activeBook) {
    if (activeBook.parsedChapters.isNotEmpty()) {
      activeBook.parsedChapters
    } else {
      val lvSections = activeBook.librivoxEditions.firstOrNull { it.sections.isNotEmpty() }?.sections.orEmpty()
      lvSections.mapIndexed { idx, sec ->
        FullBookChapter(
          id = "lv_sec_${sec.id}",
          chapterIndex = idx,
          chapterNumber = idx + 1,
          title = sec.title.ifBlank { "Chapter ${idx + 1}" },
          fullText = "",
          estimatedMinutes = if (sec.isDurationVerified && sec.durationSeconds > 0) (sec.durationSeconds / 60).coerceAtLeast(1) else 0,
          librivoxAudioUrl = sec.listenUrl,
          librivoxDurationSeconds = sec.durationSeconds,
          isAudioDurationVerified = sec.isDurationVerified,
          librivoxReaderName = sec.readerName
        )
      }
    }
  }

  val totalChaptersCount = if (displayChapters.isNotEmpty()) {
    displayChapters.size
  } else {
    activeBook.episodes.size
  }

  val completedCount = savedProg.completedChapterIndices.count { it in 0 until totalChaptersCount }
  val activeChapterIdx = savedProg.lastEpisodeIndex.coerceIn(0, (totalChaptersCount - 1).coerceAtLeast(0))
  val currentChapterFraction = savedProg.chapterProgressMap[activeChapterIdx] ?: 0f
  val overallProgressFraction = if (totalChaptersCount > 0) {
    ((completedCount.toFloat() + (if (activeChapterIdx !in savedProg.completedChapterIndices) currentChapterFraction else 0f)) / totalChaptersCount.toFloat())
      .coerceIn(0f, 1f)
  } else 0f

  val hasStartedBook = savedProg.lastEpisodeIndex > 0 ||
    savedProg.lastSentenceIndex > 0 ||
    savedProg.lastPlaybackPositionMs > 0L ||
    completedCount > 0

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(grad.first().copy(alpha = 0.75f), DeepNavyBg, Color(0xFF030712))
        )
      )
      .testTag("book_detail_screen"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
  ) {
    // 1. Top Navigation Bar
    item(key = "top_bar") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .testTag("book_detail_back_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          IconButton(
            onClick = {
              isFavorite = AudiobookLearningService.toggleFavoriteBook(context, userId, activeBook)
            },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.08f))
              .testTag("book_detail_favorite_btn")
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favorite Book",
              tint = if (isFavorite) RoseAccent else TextWhite
            )
          }

          IconButton(
            onClick = {
              if (isSaved) {
                AudiobookLearningService.removeBookFromLibrary(context, userId, activeBook.id)
                isSaved = false
              } else {
                AudiobookLearningService.addBookToLibrary(context, userId, activeBook.id, activeBook)
                isSaved = true
              }
            },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.08f))
              .testTag("book_detail_bookmark_btn")
          ) {
            Icon(
              imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Save to Library",
              tint = if (isSaved) AmberAccent else TextWhite
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    // 2. Book Cover, Title, Author & Availability Badges
    item(key = "book_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = grad.first(),
          border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.18f)),
          modifier = Modifier.size(width = 112.dp, height = 162.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Brush.linearGradient(grad)),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(8.dp)
            ) {
              Icon(Icons.Default.MenuBook, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(28.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = activeBook.title,
                color = TextWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
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

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          // Availability Tags: only show "Complete Book & Audio" when BOTH full text and human audio are verified
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            if (activeBook.isCompleteBookAndAudioVerified) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = EmeraldAccent.copy(alpha = 0.16f),
                border = BorderStroke(0.8.dp, EmeraldAccent.copy(alpha = 0.5f))
              ) {
                Text(
                  text = "Complete Book & Audio",
                  color = EmeraldAccent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
              }
            } else {
              if (activeBook.hasVerifiedFullText || (!activeBook.isFullBookEligible && activeBook.episodes.isNotEmpty())) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = EmeraldAccent.copy(alpha = 0.16f),
                  border = BorderStroke(0.8.dp, EmeraldAccent.copy(alpha = 0.5f))
                ) {
                  Text(
                    text = if (activeBook.hasVerifiedFullText) "Full Ebook Text" else activeBook.contentType.label,
                    color = EmeraldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
              if (activeBook.hasVerifiedHumanAudio) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = AmberAccent.copy(alpha = 0.16f),
                  border = BorderStroke(0.8.dp, AmberAccent.copy(alpha = 0.5f))
                ) {
                  Text(
                    text = "Human Audiobook",
                    color = AmberAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              } else if (audioChecked && !isLoadingAudio) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = CyanAccent.copy(alpha = 0.16f),
                  border = BorderStroke(0.8.dp, CyanAccent.copy(alpha = 0.5f))
                ) {
                  Text(
                    text = "AI Narration Ready",
                    color = CyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = activeBook.title,
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 25.sp
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = activeBook.author,
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Total Chapter Count & Category Summary
          Text(
            text = "$totalChaptersCount Chapters • ${activeBook.primaryCategory} • ${activeBook.defaultCefrLevel}",
            color = CyanAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )

          val lvEdition = activeBook.librivoxEditions.firstOrNull { it.sections.isNotEmpty() }
          if (lvEdition != null) {
            Spacer(modifier = Modifier.height(3.dp))
            val timeInfo = if (lvEdition.totalTime.isNotBlank()) " • ${lvEdition.totalTime}" else ""
            Text(
              text = "Narrated by ${lvEdition.narratorName} • ${lvEdition.sections.size} episodes$timeInfo",
              color = TextMuted,
              fontSize = 11.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          } else if (audioChecked && !isLoadingAudio && activeBook.isFullBookEligible) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Human audiobook unavailable • AI narration available",
              color = TextMuted,
              fontSize = 11.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Listening & Reading Progress Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color.White.copy(alpha = 0.05f))
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val statusLabel = when {
            totalChaptersCount == 0 && isLoadingText -> "Downloading book chapters..."
            totalChaptersCount == 0 -> "0 chapters loaded yet"
            hasStartedBook -> "Current: Chapter ${activeChapterIdx + 1} of $totalChaptersCount"
            else -> "Ready to Start • $totalChaptersCount Chapters"
          }
          Text(
            text = statusLabel,
            color = TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "${(overallProgressFraction * 100).toInt()}%",
            color = EmeraldAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { overallProgressFraction },
          color = EmeraldAccent,
          trackColor = Color.White.copy(alpha = 0.1f),
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(50))
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Separate Reading vs Listening progress indicators (saved independently in Firebase)
      val hasReadingProg = savedProg.readingChapterIndex > 0 || savedProg.readingScrollOffset > 0 || savedProg.readingPercent > 0.01f
      val hasListeningProg = savedProg.lastEpisodeIndex > 0 || savedProg.lastPlaybackPositionMs > 0L || savedProg.lastSentenceIndex > 0 || savedProg.listeningPercent > 0.01f
      if (hasReadingProg || hasListeningProg) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (hasReadingProg) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = EmeraldAccent.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
              modifier = Modifier.weight(1f).testTag("book_detail_reading_progress_badge")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val pct = (savedProg.readingPercent * 100).toInt().coerceIn(0, 100)
                Text(
                  text = "Read: Ch. ${savedProg.readingChapterIndex + 1}${if (pct > 0) " ($pct%)" else ""}",
                  color = EmeraldAccent,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
          if (hasListeningProg) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CyanAccent.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
              modifier = Modifier.weight(1f).testTag("book_detail_listening_progress_badge")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val pct = (savedProg.listeningPercent * 100).toInt().coerceIn(0, 100)
                Text(
                  text = "Audio: Ch. ${savedProg.lastEpisodeIndex + 1}${if (pct > 0) " ($pct%)" else ""}",
                  color = CyanAccent,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Public-Domain & AI-Generated Attribution Banner
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DeepNavyCard,
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp)
          .testTag("book_detail_attribution_banner")
      ) {
        Text(
          text = if (activeBook.contentType == BookContentType.AI_LEARNING_SUMMARY) {
            "AI-generated Educational Study Guide & Key Lessons • Cached for instant access"
          } else {
            "Public-domain text via Project Gutenberg (Gutendex) • Human volunteer audiobook recordings via LibriVox"
          },
          color = TextMuted,
          fontSize = 11.sp,
          lineHeight = 15.sp,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
      }

      // Primary Action Buttons: Read & Listen (primary), or Unlock with Pro when locked for a Free user
      if (!isBookUnlockedForCurrentUser) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = AmberAccent.copy(alpha = 0.14f),
          border = BorderStroke(1.2.dp, AmberAccent.copy(alpha = 0.55f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("locked_book_preview_card")
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              com.example.ui.components.LingoGoldLockIcon(size = 16.dp)
              Text(
                text = "Monthly Free Book Limit Reached",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }
            Text(
              text = activeBook.shortDescription.ifBlank {
                "Previewing ${activeBook.title} by ${activeBook.author} ($totalChaptersCount chapters). Free includes 5 books per month. Unlock unlimited books, AI summaries, and narration with LingoSphere Pro."
              },
              color = TextLight,
              fontSize = 13.sp,
              lineHeight = 19.sp
            )
            Button(
              onClick = onOpenSubscription,
              colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color(0xFF041220)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("unlock_book_with_pro_btn")
            ) {
              Text("Unlock Unlimited Books", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      } else if (activeBook.isFullBookEligible) {
        val readTargetCh = savedProg.readingChapterIndex.coerceIn(0, (totalChaptersCount - 1).coerceAtLeast(0))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              AudiobookMediaController.loadBookAndChapter(
                book = activeBook,
                chapterIndex = activeChapterIdx,
                sentenceIndex = 0,
                autoPlay = false,
                preferHumanAudio = preferHumanNarrator
              )
              onPlayEpisode(activeChapterIdx)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("book_detail_listen_full_btn")
          ) {
            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            val combinedLabel = if (hasStartedBook) {
              "Read & Listen · Ch. ${activeChapterIdx + 1}"
            } else {
              "Read & Listen"
            }
            Text(combinedLabel, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = {
                AudiobookMediaController.loadBookAndChapter(
                  book = activeBook,
                  chapterIndex = readTargetCh,
                  sentenceIndex = 0,
                  autoPlay = false,
                  preferHumanAudio = preferHumanNarrator
                )
                onReadOnlyChapter(readTargetCh)
              },
              border = BorderStroke(1.2.dp, EmeraldAccent),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("book_detail_read_full_btn")
            ) {
              Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(6.dp))
              val readLabel = if (savedProg.readingChapterIndex > 0) {
                "Read · Ch. ${savedProg.readingChapterIndex + 1}"
              } else {
                "Read Only"
              }
              Text(readLabel, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }

            OutlinedButton(
              onClick = {
                AudiobookMediaController.loadBookAndChapter(
                  book = activeBook,
                  chapterIndex = activeChapterIdx,
                  sentenceIndex = 0,
                  autoPlay = false,
                  preferHumanAudio = preferHumanNarrator
                )
                onListenOnlyChapter(activeChapterIdx)
              },
              border = BorderStroke(1.2.dp, CyanAccent.copy(alpha = 0.7f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("book_detail_listen_only_btn")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(4.dp))
              val listenLabel = if (hasListeningProg) {
                "Listen · Ch. ${activeChapterIdx + 1}"
              } else {
                "Listen Only"
              }
              Text(listenLabel, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
          }
        }
      } else {
        Button(
          onClick = {
            AudiobookMediaController.loadBookAndChapter(
              book = activeBook,
              chapterIndex = activeChapterIdx,
              sentenceIndex = 0,
              autoPlay = false,
              preferHumanAudio = preferHumanNarrator
            )
            onPlayEpisode(activeChapterIdx)
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("book_detail_start_listening_btn")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          val btnText = if (hasStartedBook) {
            "Continue Listening (Ep. ${activeChapterIdx + 1})"
          } else {
            "Start Listening"
          }
          Text(btnText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
      }

      // Narration Mode & Shared AI Voice Selector
      Spacer(modifier = Modifier.height(10.dp))
      if (activeBook.hasVerifiedHumanAudio) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = preferHumanNarrator,
            onClick = {
              preferHumanNarrator = true
              AudiobookMediaController.setNarrationEditionMode(true)
            },
            label = { Text("Human Narration", fontSize = 11.5.sp) },
            leadingIcon = {
              Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(15.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
              selectedLabelColor = AmberAccent,
              selectedLeadingIconColor = AmberAccent,
              containerColor = DeepNavyCard,
              labelColor = TextMuted
            ),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("book_detail_human_narration_chip")
          )
          FilterChip(
            selected = !preferHumanNarrator,
            onClick = {
              preferHumanNarrator = false
              AudiobookMediaController.setNarrationEditionMode(false)
            },
            label = { Text("AI Voice (Sentence Sync)", fontSize = 11.5.sp) },
            leadingIcon = {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
              selectedLabelColor = CyanAccent,
              selectedLeadingIconColor = CyanAccent,
              containerColor = DeepNavyCard,
              labelColor = TextMuted
            ),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("book_detail_ai_mode_chip")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      // Unified AI Voice Selector Button (opens SharedVoiceSelectorBottomSheet)
      Surface(
        onClick = { showVoiceSelectorSheet = true },
        shape = RoundedCornerShape(12.dp),
        color = DeepNavyCard,
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
        modifier = Modifier
          .fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("book_detail_ai_voice_selector_btn")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = "AI Voice", tint = CyanAccent, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "AI Voice: ${selectedAudiobookVoice.personaName} (${selectedAudiobookVoice.id})",
              color = TextWhite,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${selectedAudiobookVoice.gender} • ${selectedAudiobookVoice.accentLabel} • Tap to preview & change",
              color = TextMuted,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = "Change",
            color = CyanAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }

      if (activeBook.hasVerifiedHumanAudio) {
        // Skip Recording Announcements Setting (Enabled by default)
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
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EmeraldAccent.copy(alpha = 0.20f),
            selectedLabelColor = EmeraldAccent,
            containerColor = DeepNavyCard,
            labelColor = TextMuted
          ),
          modifier = Modifier.testTag("book_detail_skip_announcements_chip")
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    // 3. Separate Non-Blocking Text Download & Audio Discovery Status / Cancel / Retry Banners
    if (isLoadingText) {
      item(key = "loading_text_banner") {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepNavyElevated)
            .padding(14.dp)
            .testTag("book_detail_text_loading_card")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              CircularProgressIndicator(
                color = CyanAccent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Downloading complete book text...",
                  color = TextWhite,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = textDownloadProgress?.formattedProgress ?: "Connecting to book mirror...",
                  color = CyanAccent,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
            OutlinedButton(
              onClick = { textLoadJob?.cancel() },
              border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.7f)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("book_detail_cancel_text_btn")
            ) {
              Text("Cancel", color = RoseAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          val progFraction = textDownloadProgress?.fraction
          if (progFraction != null) {
            LinearProgressIndicator(
              progress = { progFraction },
              color = CyanAccent,
              trackColor = Color.White.copy(alpha = 0.12f),
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
            )
          } else {
            LinearProgressIndicator(
              color = CyanAccent,
              trackColor = Color.White.copy(alpha = 0.12f),
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }

    if (isLoadingAudio) {
      item(key = "loading_audio_banner") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepNavyCard)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("book_detail_audio_loading_card"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            CircularProgressIndicator(
              color = AmberAccent,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Checking human audiobook recordings...",
              color = TextLight,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(
            onClick = { audioLoadJob?.cancel() },
            modifier = Modifier
              .size(32.dp)
              .testTag("book_detail_cancel_audio_btn")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel Audio Check", tint = TextMuted, modifier = Modifier.size(16.dp))
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }

    if (textLoadError != null && !isLoadingText) {
      item(key = "text_error_banner") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A1215))
            .padding(12.dp)
            .testTag("book_detail_text_error_card"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = textLoadError!!,
            color = TextLight,
            fontSize = 12.sp,
            modifier = Modifier
              .weight(1f)
              .padding(end = 8.dp)
          )
          OutlinedButton(
            onClick = { launchTextDownload(savedProg.lastEpisodeIndex.coerceAtLeast(0)) },
            border = BorderStroke(1.dp, CyanAccent),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("book_detail_retry_text_btn")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retry Text", color = CyanAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }

    if (audioLoadError != null && !isLoadingAudio && !activeBook.hasVerifiedHumanAudio) {
      item(key = "audio_status_banner") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepNavyCard)
            .padding(12.dp)
            .testTag("book_detail_audio_status_card"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = audioLoadError!!,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier
              .weight(1f)
              .padding(end = 8.dp)
          )
          OutlinedButton(
            onClick = { launchAudioDiscovery() },
            border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("book_detail_retry_audio_btn")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retry Audio", color = AmberAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }

    // 4. Complete Ordered Chapters Header
    item(key = "chapters_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (displayChapters.isNotEmpty()) "All Chapters (1 – ${displayChapters.size})" else "Episodes",
          color = TextWhite,
          fontSize = 16.sp,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = if (displayChapters.isNotEmpty()) {
            "${displayChapters.size} chapters • Original Order"
          } else {
            "${activeBook.episodes.size} episodes"
          },
          color = EmeraldAccent,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = DeepNavyBorder)
    }

    // 5. Lazy-Loaded Complete Chapter List (Supports 100, 200+ chapters smoothly)
    if (displayChapters.isNotEmpty()) {
      itemsIndexed(
        items = displayChapters,
        key = { idx, ch -> "ch_${idx}_${ch.id}" }
      ) { chIdx, chapter ->
        val isCurrent = chIdx == activeChapterIdx && hasStartedBook
        val isCompleted = savedProg.completedChapterIndices.contains(chIdx)
        val chProgress = savedProg.chapterProgressMap[chIdx] ?: 0f

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              if (isCurrent) CyanAccent.copy(alpha = 0.1f) else Color.Transparent
            )
            .clickable {
              AudiobookMediaController.loadBookAndChapter(
                book = activeBook,
                chapterIndex = chIdx,
                sentenceIndex = 0,
                autoPlay = false,
                preferHumanAudio = preferHumanNarrator
              )
              onPlayEpisode(chIdx)
            }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag("book_chapter_item_$chIdx")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Chapter Index / Completion Badge
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isCurrent -> CyanAccent
                    isCompleted -> EmeraldAccent.copy(alpha = 0.2f)
                    else -> Color.White.copy(alpha = 0.07f)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              if (isCompleted && !isCurrent) {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = "Completed",
                  tint = EmeraldAccent,
                  modifier = Modifier.size(18.dp)
                )
              } else {
                Text(
                  text = "${chIdx + 1}",
                  color = if (isCurrent) Color(0xFF041220) else TextWhite,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Chapter Title & Duration / Metadata
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = chapter.title,
                color = if (isCurrent) CyanAccent else TextWhite,
                fontSize = 14.5.sp,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(3.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (chapter.formattedDuration.isNotBlank()) {
                  Text(
                    text = chapter.formattedDuration,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
                if (chapter.wordCount > 0 && !GutendexAndLibriVoxService.isStubOrSummaryChapter(chapter)) {
                  if (chapter.formattedDuration.isNotBlank()) {
                    Text("•", color = TextMuted, fontSize = 11.sp)
                  }
                  Text(
                    text = "${"%,d".format(chapter.wordCount)} words",
                    color = TextMuted,
                    fontSize = 11.5.sp
                  )
                }
                if (chProgress > 0.02f && !isCompleted) {
                  if (chapter.formattedDuration.isNotBlank() || (chapter.wordCount > 0 && !GutendexAndLibriVoxService.isStubOrSummaryChapter(chapter))) {
                    Text("•", color = CyanAccent, fontSize = 11.sp)
                  }
                  Text(
                    text = "${(chProgress * 100).toInt()}% read",
                    color = CyanAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            // Read & Play Buttons
            IconButton(
              onClick = {
                AudiobookMediaController.loadBookAndChapter(
                  book = activeBook,
                  chapterIndex = chIdx,
                  sentenceIndex = 0,
                  autoPlay = false,
                  preferHumanAudio = preferHumanNarrator
                )
                onReadChapter(chIdx)
              },
              modifier = Modifier.size(38.dp)
            ) {
              Icon(
                Icons.Default.MenuBook,
                contentDescription = "Read ${chapter.title}",
                tint = EmeraldAccent,
                modifier = Modifier.size(19.dp)
              )
            }

            IconButton(
              onClick = {
                AudiobookMediaController.loadBookAndChapter(
                  book = activeBook,
                  chapterIndex = chIdx,
                  sentenceIndex = 0,
                  autoPlay = false,
                  preferHumanAudio = preferHumanNarrator
                )
                onPlayEpisode(chIdx)
              },
              modifier = Modifier.size(38.dp)
            ) {
              Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play ${chapter.title}",
                tint = CyanAccent,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
        HorizontalDivider(color = DeepNavyBorder.copy(alpha = 0.6f))
      }
    } else if (activeBook.episodes.isNotEmpty()) {
      itemsIndexed(
        items = activeBook.episodes,
        key = { idx, ep -> "ep_${idx}_${ep.id}" }
      ) { epIdx, episode ->
        val isCurrent = epIdx == activeChapterIdx && hasStartedBook
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayEpisode(epIdx) }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag("book_episode_item_$epIdx")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isCurrent) CyanAccent else Color.White.copy(alpha = 0.07f)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${episode.episodeNumber}",
                color = if (isCurrent) Color(0xFF041220) else TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = episode.title,
                color = TextWhite,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${episode.estimatedMinutes} min • ${episode.narrationLines.size} lines",
                color = TextMuted,
                fontSize = 12.sp
              )
            }

            IconButton(
              onClick = { onPlayEpisode(epIdx) },
              modifier = Modifier.size(38.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Play Episode", tint = CyanAccent)
            }
          }
        }
        HorizontalDivider(color = DeepNavyBorder.copy(alpha = 0.6f))
      }
    }

    item(key = "bottom_spacer") {
      Spacer(modifier = Modifier.height(36.dp))
    }
  }

  if (showVoiceSelectorSheet) {
    SharedVoiceSelectorBottomSheet(
      title = "Audiobook AI Voice",
      subtitle = "Preview & choose the AI narrator voice for this book",
      selectedVoice = selectedAudiobookVoice,
      isHumanNarratorSelected = preferHumanNarrator && activeBook.hasVerifiedHumanAudio,
      includeHumanNarratorOption = activeBook.hasVerifiedHumanAudio,
      humanNarratorAvailable = activeBook.hasVerifiedHumanAudio,
      targetLang = targetLang,
      speechManager = activeSpeechManager,
      onSelectHumanNarrator = {
        preferHumanNarrator = true
        AudiobookMediaController.setNarrationEditionMode(true)
        showVoiceSelectorSheet = false
      },
      onApplyVoice = { chosenVoice ->
        preferHumanNarrator = false
        AudiobookMediaController.setTutorVoice(chosenVoice)
        AudiobookMediaController.setNarrationEditionMode(false)
        showVoiceSelectorSheet = false
      },
      onDismiss = { showVoiceSelectorSheet = false }
    )
  }
}

/**
 * Interactive Create My Listening modal allowing users to choose a topic:
 * - How to become successful
 * - How to manage money
 * - How to build confidence
 * - A mystery story
 * - A motivational speech
 * - A business conversation
 * - An interesting science lesson
 * And select Language, Difficulty (CEFR), Duration, Narrator Voice, and Content Type.
 */
@Composable
fun CreateMyListeningSheet(
  userId: String,
  targetLang: Language,
  nativeLang: Language,
  userLevel: String,
  isPremium: Boolean,
  speechManager: SpeechManager? = null,
  onBookCreatedAndPlay: (AudiobookBook) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val ownedSpeechManager = remember(speechManager) {
    if (speechManager == null) SpeechManager(context.applicationContext) else null
  }
  val activeSpeechManager = speechManager ?: ownedSpeechManager!!
  DisposableEffect(ownedSpeechManager) {
    onDispose {
      ownedSpeechManager?.release()
    }
  }

  var topicInput by remember { mutableStateOf("") }
  var selectedCluster by remember { mutableStateOf(AudiobookCatalogRepository.CATEGORY_CLUSTERS.first()) }
  var selectedSubCat by remember { mutableStateOf(selectedCluster.subCategories.first()) }
  var selectedStyle by remember { mutableStateOf("Lesson") } // Story, Lesson, Conversation, Speech
  var selectedCefr by remember { mutableStateOf(userLevel.ifBlank { "B1" }) }
  var selectedDurationMins by remember { mutableIntStateOf(5) }
  var selectedVoice by remember { mutableStateOf(activeSpeechManager.audiobookVoice.value) }
  var showVoiceSelectorSheet by remember { mutableStateOf(false) }
  var isGenerating by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val quickPrompts = listOf(
    "How to become successful",
    "How to manage money",
    "How to build confidence",
    "A mystery story",
    "A motivational speech",
    "A business conversation",
    "An interesting science lesson"
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(DeepNavyBg)
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .verticalScroll(rememberScrollState())
      .testTag("create_my_listening_sheet")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Create My Listening",
          color = TextWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
      IconButton(onClick = onDismiss) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextLight)
      }
    }

    Text(
      text = "Generate original audio lessons, speeches, or stories in ${targetLang.flagEmoji} ${targetLang.name}.",
      color = TextMuted,
      fontSize = 12.5.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
      value = topicInput,
      onValueChange = { topicInput = it },
      label = { Text("Choose or type a topic", color = TextLight) },
      placeholder = { Text("e.g. How to become successful", color = TextMuted) },
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = DeepNavyBorder,
        focusedContainerColor = DeepNavyCard,
        unfocusedContainerColor = DeepNavyCard
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("custom_listening_topic_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text("Suggested Topics:", color = AmberAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      quickPrompts.forEach { prompt ->
        FilterChip(
          selected = topicInput == prompt,
          onClick = { topicInput = prompt },
          label = { Text(prompt, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
            selectedLabelColor = CyanAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Difficulty (CEFR Level)
    Text("Difficulty (${targetLang.name}):", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { cefr ->
        val isSelected = selectedCefr == cefr
        Surface(
          onClick = { selectedCefr = cefr },
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) CyanAccent else DeepNavyCard,
          border = BorderStroke(1.dp, if (isSelected) CyanAccent else DeepNavyBorder),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = cefr,
            color = if (isSelected) Color(0xFF041220) else TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 7.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Content Type Selector
    Text("Content Type:", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("Lesson", "Story", "Conversation", "Speech").forEach { style ->
        val isSelected = selectedStyle == style
        Surface(
          onClick = { selectedStyle = style },
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) CyanAccent else DeepNavyCard,
          border = BorderStroke(1.dp, if (isSelected) CyanAccent else DeepNavyBorder),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = style,
            color = if (isSelected) Color(0xFF041220) else TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Narrator Voice Selector (Unified AI Voice Catalog)
    Text("Narrator Voice:", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
      onClick = { showVoiceSelectorSheet = true },
      shape = RoundedCornerShape(12.dp),
      color = DeepNavyCard,
      border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag("create_listening_voice_selector_btn")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Narrator Voice", tint = CyanAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${selectedVoice.personaName} (${selectedVoice.id})",
            color = TextWhite,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${selectedVoice.gender} • ${selectedVoice.accentLabel} • ${selectedVoice.styleTag}",
            color = TextMuted,
            fontSize = 11.5.sp
          )
        }
        Text("Change", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
      }
    }

    if (showVoiceSelectorSheet) {
      SharedVoiceSelectorBottomSheet(
        title = "Select AI Narrator Voice",
        subtitle = "Compare and choose an AI narrator for your generated audio",
        selectedVoice = selectedVoice,
        targetLang = targetLang,
        speechManager = activeSpeechManager,
        onApplyVoice = { chosenVoice ->
          selectedVoice = chosenVoice
          AudiobookMediaController.setTutorVoice(chosenVoice)
          showVoiceSelectorSheet = false
        },
        onDismiss = { showVoiceSelectorSheet = false }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Duration Selector
    Text("Target Duration:", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(2, 5, 10, 15, 30).forEach { mins ->
        val isSelected = selectedDurationMins == mins
        Surface(
          onClick = { selectedDurationMins = mins },
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) EmeraldAccent else DeepNavyCard,
          border = BorderStroke(1.dp, if (isSelected) EmeraldAccent else DeepNavyBorder),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = "${mins}m",
            color = if (isSelected) Color(0xFF041220) else TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }
      }
    }

    if (errorMessage != null) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp)
    }

    Spacer(modifier = Modifier.height(18.dp))

    Button(
      onClick = {
        if (topicInput.isBlank()) {
          errorMessage = "Please enter a topic or select one of the suggested topics above."
          return@Button
        }
        isGenerating = true
        errorMessage = null
        scope.launch {
          val result = AudiobookLearningService.generateFreshAudiobook(
            context = context,
            userId = userId,
            targetLanguage = targetLang,
            nativeLanguage = nativeLang,
            cefrLevel = selectedCefr,
            primaryCategory = selectedCluster.title,
            subCategory = selectedSubCat,
            topicOrBookTitle = topicInput.trim(),
            contentStyle = selectedStyle,
            durationMinutes = selectedDurationMins,
            isPremiumUser = isPremium
          )
          isGenerating = false
          when (result) {
            is AudiobookGenerationResult.Success -> {
              AudiobookMediaController.setTutorVoice(selectedVoice)
              onBookCreatedAndPlay(result.book)
            }
            is AudiobookGenerationResult.Error -> {
              errorMessage = com.example.ai.AiErrorHandler.sanitizeUserFacingMessage(result.userFriendlyMessage)
            }
          }
        }
      },
      enabled = !isGenerating,
      colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("submit_create_listening_btn")
    ) {
      if (isGenerating) {
        CircularProgressIndicator(color = Color(0xFF041220), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Generating Audio Lesson...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
      } else {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Generate & Listen", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
      }
    }
  }
}
