package com.example.ui.screens

import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiTutorService
import com.example.data.model.FullBookChapter
import com.example.data.model.ReadingAcademyPassage
import com.example.data.model.ReadingWordGloss
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.data.repository.LibraryBookItem
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun isDuplicateReadingTranslation(original: String, translation: String?): Boolean {
  if (translation.isNullOrBlank()) return true
  val normOrig = original.trim().replace(Regex("\\s+"), " ").lowercase()
  val normTrans = translation.trim().replace(Regex("\\s+"), " ").lowercase()
  return normOrig == normTrans
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DedicatedStoryReaderScreen(
  passage: ReadingAcademyPassage,
  activeParagraphIndex: Int,
  isBookmarked: Boolean,
  isCompleted: Boolean,
  speechManager: SpeechManager?,
  onUpdateParagraphPosition: (Int) -> Unit,
  onToggleBookmark: () -> Unit,
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit,
  onOpenTeleprompter: () -> Unit,
  onStartQuiz: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler {
    speechManager?.stopSpeaking()
    onBack()
  }

  var fontScale by remember { mutableStateOf(ReadingFontScale.LARGE) }
  var lineSpacing by remember { mutableStateOf(ReadingLineSpacing.COMFORTABLE) }
  var readingWidthMode by remember { mutableStateOf(ReadingWidthMode.STANDARD) }
  var showTranslation by remember { mutableStateOf(true) }
  var autoScrollEnabled by remember { mutableStateOf(true) }
  var showSettingsSheet by remember { mutableStateOf(false) }

  var isNarrating by remember(passage.id) { mutableStateOf(false) }
  var selectedGloss by remember(passage.id) { mutableStateOf<ReadingWordGloss?>(null) }
  var savedGlossWords by remember { mutableStateOf(setOf<String>()) }

  val totalParagraphs = passage.paragraphs.size.coerceAtLeast(1)
  val safeIndex = activeParagraphIndex.coerceIn(0, totalParagraphs - 1)
  val progress = (safeIndex + 1).toFloat() / totalParagraphs.toFloat()
  val listState = rememberLazyListState()

  // Smooth auto-scroll to active paragraph during narration
  LaunchedEffect(safeIndex, isNarrating, autoScrollEnabled) {
    if (isNarrating && autoScrollEnabled) {
      listState.animateScrollToItem(index = safeIndex)
    }
  }

  // Paragraph-by-paragraph TTS playback
  LaunchedEffect(isNarrating, safeIndex, passage.id) {
    if (isNarrating) {
      val paragraphText = passage.paragraphs.getOrNull(safeIndex)
      if (!paragraphText.isNullOrBlank()) {
        speechManager?.speak(paragraphText, passage.languageCode, 0.95f)
        val words = paragraphText.split(Regex("\\s+")).size.coerceAtLeast(5)
        val waitMs = (words * 380L).coerceIn(2200L, 6500L)
        delay(waitMs)
        if (isNarrating) {
          if (safeIndex + 1 < passage.paragraphs.size) {
            onUpdateParagraphPosition(safeIndex + 1)
          } else {
            isNarrating = false
          }
        }
      } else {
        isNarrating = false
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      speechManager?.stopSpeaking()
    }
  }

  Scaffold(
    containerColor = LingoTokens.bgCanvas,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      Surface(color = LingoTokens.bgCanvas) {
        Column(modifier = Modifier.statusBarsPadding()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(
              onClick = {
                speechManager?.stopSpeaking()
                onBack()
              },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = LingoTokens.textPrimary
              )
            }

            Column(
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
            ) {
              val quizCount = passage.questions.size
              val quizLabel = if (quizCount == 1) "1 quiz question" else "$quizCount quiz questions"
              Text(
                text = cleanStoryTitle(passage.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${passage.cefrLevel} · Paragraph ${safeIndex + 1} of $totalParagraphs" +
                  if (quizCount > 0) " · $quizLabel" else "",
                style = MaterialTheme.typography.labelSmall,
                color = LingoTokens.textSecondary
              )
            }

            IconButton(
              onClick = onToggleBookmark,
              modifier = Modifier
                .size(48.dp)
                .testTag("reading_bookmark_button")
            ) {
              Icon(
                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Story",
                tint = if (isBookmarked) LingoTokens.accentAmber else LingoTokens.textSecondary
              )
            }
          }

          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(2.5.dp),
            color = LingoTokens.accentPrimary,
            trackColor = LingoTokens.bgSubtle
          )
        }
      }
    },
    bottomBar = {
      CompactReaderToolbar(
        isPlaying = isNarrating,
        showTranslation = showTranslation,
        autoScrollEnabled = autoScrollEnabled,
        canGoPrevious = safeIndex > 0,
        canGoNext = safeIndex < totalParagraphs - 1,
        onPrevious = {
          val prev = (safeIndex - 1).coerceAtLeast(0)
          onUpdateParagraphPosition(prev)
          speechManager?.speak(passage.paragraphs[prev], passage.languageCode, 0.95f)
        },
        onTogglePlay = {
          if (isNarrating) {
            isNarrating = false
            speechManager?.stopSpeaking()
          } else {
            isNarrating = true
          }
        },
        onNext = {
          val next = (safeIndex + 1).coerceAtMost(totalParagraphs - 1)
          onUpdateParagraphPosition(next)
          speechManager?.speak(passage.paragraphs[next], passage.languageCode, 0.95f)
        },
        onOpenSettings = { showSettingsSheet = true },
        onToggleTranslation = { showTranslation = !showTranslation },
        onToggleAutoScroll = { autoScrollEnabled = !autoScrollEnabled }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("dedicated_story_reader")
    ) {
      AnimatedVisibility(visible = selectedGloss != null) {
        selectedGloss?.let { gloss ->
          val isWordSaved = savedGlossWords.contains("${passage.id}_${gloss.word}")
          LingoCleanCard(
            modifier = Modifier
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag("reading_word_gloss_card"),
            containerColor = LingoTokens.bgSubtle,
            borderColor = LingoTokens.accentPrimary.copy(alpha = 0.45f),
            contentPadding = 12.dp
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = gloss.word,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.accentPrimary
                )
                LingoStatusBadge(text = gloss.partOfSpeech, color = LingoTokens.textSecondary)
              }
              Row {
                IconButton(
                  onClick = { speechManager?.speak(gloss.word, passage.languageCode) },
                  modifier = Modifier.size(40.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Pronounce word",
                    tint = LingoTokens.accentPrimary
                  )
                }
                IconButton(
                  onClick = { selectedGloss = null },
                  modifier = Modifier.size(40.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close definition",
                    tint = LingoTokens.textSecondary
                  )
                }
              }
            }
            Text(
              text = "${gloss.translation}${if (gloss.phonetic.isNotBlank()) " • /${gloss.phonetic}/" else ""}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = LingoTokens.textPrimary
            )
            if (gloss.definition.isNotBlank()) {
              Text(
                text = gloss.definition,
                style = MaterialTheme.typography.bodySmall,
                color = LingoTokens.textSecondary
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
              onClick = {
                if (!isWordSaved) {
                  onSaveVocabToFlashcard(gloss.word, gloss.translation, gloss.phonetic)
                  savedGlossWords = savedGlossWords + "${passage.id}_${gloss.word}"
                }
              },
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isWordSaved) LingoTokens.accentEmerald.copy(alpha = 0.18f) else LingoTokens.accentPrimary.copy(alpha = 0.18f),
                contentColor = if (isWordSaved) LingoTokens.accentEmerald else LingoTokens.accentPrimary
              ),
              modifier = Modifier.testTag("save_reading_gloss_word_btn")
            ) {
              Icon(
                imageVector = if (isWordSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (isWordSaved) "Saved to Flashcards ✓" else "Save to Flashcards", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      val hMargin = when (readingWidthMode) {
        ReadingWidthMode.COMPACT -> 28.dp
        ReadingWidthMode.STANDARD -> 20.dp
        ReadingWidthMode.WIDE -> 12.dp
      }

      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = hMargin),
        contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        itemsIndexed(passage.paragraphs) { idx, paragraph ->
          val isCurrent = idx == safeIndex
          val rawTranslation = passage.paragraphTranslations.getOrNull(idx)
          val validTranslation = rawTranslation?.takeIf { !isDuplicateReadingTranslation(paragraph, it) }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (isCurrent) LingoTokens.accentPrimary.copy(alpha = 0.08f) else Color.Transparent
              )
              .clickable {
                onUpdateParagraphPosition(idx)
              }
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("reading_paragraph_$idx")
          ) {
            val words = paragraph.split(" ").filter { it.isNotBlank() }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(lineSpacing.wordRowSpacingDp.dp)
            ) {
              words.forEach { rawWord ->
                val cleanKey = rawWord.lowercase().trim { !it.isLetterOrDigit() }
                val glossMatch = passage.glossary[cleanKey]
                val isSelectedWord = selectedGloss?.word?.equals(cleanKey, ignoreCase = true) == true
                Text(
                  text = rawWord,
                  fontSize = fontScale.bodySizeSp.sp,
                  lineHeight = (fontScale.bodySizeSp * lineSpacing.lineHeightMultiplier).sp,
                  fontWeight = if (glossMatch != null) FontWeight.SemiBold else FontWeight.Normal,
                  color = when {
                    isSelectedWord -> LingoTokens.onAccentDark
                    glossMatch != null -> LingoTokens.accentPrimary
                    else -> LingoTokens.textPrimary
                  },
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelectedWord) LingoTokens.accentPrimary else Color.Transparent)
                    .clickable {
                      onUpdateParagraphPosition(idx)
                      selectedGloss = glossMatch ?: ReadingWordGloss(
                        word = rawWord.trim { !it.isLetterOrDigit() }.ifBlank { rawWord },
                        translation = validTranslation ?: "Tap speaker icon to hear pronunciation",
                        partOfSpeech = "word in context",
                        definition = "From '${passage.title}'. Tap Save to add to Flashcards.",
                        phonetic = cleanKey
                      )
                      speechManager?.speak(cleanKey.ifBlank { rawWord }, passage.languageCode)
                    }
                    .padding(horizontal = 1.dp)
                )
              }
            }

            AnimatedVisibility(visible = showTranslation && validTranslation != null) {
              if (validTranslation != null) {
                Text(
                  text = validTranslation,
                  fontSize = (fontScale.bodySizeSp - 3).coerceAtLeast(13).sp,
                  lineHeight = ((fontScale.bodySizeSp - 3).coerceAtLeast(13) * 1.45f).sp,
                  fontStyle = FontStyle.Italic,
                  color = LingoTokens.textSecondary,
                  modifier = Modifier.padding(top = 8.dp)
                )
              }
            }
          }
        }

        if (passage.questions.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(6.dp))
            LingoCleanCard(
              containerColor = LingoTokens.bgElevated,
              borderColor = LingoTokens.borderSubtle
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "Ready to check your understanding?",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary
                )
                Text(
                  text = "${passage.questions.size} quick comprehension questions${if (isCompleted) " • Already completed ✓" else " • Earn +40 XP"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = LingoTokens.textSecondary
                )
                Button(
                  onClick = {
                    isNarrating = false
                    speechManager?.stopSpeaking()
                    onStartQuiz()
                  },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = LingoTokens.accentPrimary,
                    contentColor = LingoTokens.onAccentDark
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_reading_comprehension_quiz_btn")
                ) {
                  Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (isCompleted) "Review Comprehension Quiz" else "Take Comprehension Quiz",
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

  if (showSettingsSheet) {
    ReadingSettingsBottomSheet(
      fontScale = fontScale,
      lineSpacing = lineSpacing,
      readingWidthMode = readingWidthMode,
      onFontScaleChange = { fontScale = it },
      onLineSpacingChange = { lineSpacing = it },
      onReadingWidthChange = { readingWidthMode = it },
      onOpenTeleprompter = {
        showSettingsSheet = false
        isNarrating = false
        speechManager?.stopSpeaking()
        onOpenTeleprompter()
      },
      onDismiss = { showSettingsSheet = false }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DedicatedBookReaderScreen(
  book: LibraryBookItem,
  languageCode: String,
  isBookmarked: Boolean,
  isCompleted: Boolean,
  speechManager: SpeechManager?,
  onToggleBookmark: () -> Unit,
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit,
  onStartQuiz: () -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  BackHandler {
    speechManager?.stopSpeaking()
    onBack()
  }

  var fontScale by remember { mutableStateOf(ReadingFontScale.MEDIUM) }
  var lineSpacing by remember { mutableStateOf(ReadingLineSpacing.COMFORTABLE) }
  var readingWidthMode by remember { mutableStateOf(ReadingWidthMode.STANDARD) }
  var showTranslation by remember { mutableStateOf(false) }
  var autoScrollEnabled by remember { mutableStateOf(true) }
  var showSettingsSheet by remember { mutableStateOf(false) }

  var currentPageIndex by remember(book.id) { mutableIntStateOf(0) }
  var selectedChapterIndex by remember(book.id) { mutableIntStateOf(0) }
  var loadedFullChapters by remember(book.id) {
    mutableStateOf(book.audiobookRef?.parsedChapters.orEmpty())
  }
  var isLoadingChapters by remember(book.id) { mutableStateOf(false) }
  var isPlayingHumanAudio by remember(book.id, selectedChapterIndex) { mutableStateOf(false) }
  var isPlayingTts by remember(book.id, selectedChapterIndex) { mutableStateOf(false) }
  var audioNote by remember(book.id, selectedChapterIndex) { mutableStateOf<String?>(null) }
  var aiStudyInsight by remember(book.id, selectedChapterIndex) { mutableStateOf<String?>(null) }
  var isAnalyzingBook by remember(book.id) { mutableStateOf(false) }
  var savedWordsInBook by remember { mutableStateOf(setOf<String>()) }

  val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }
  var isMpPrepared by remember(book.id, selectedChapterIndex) { mutableStateOf(false) }
  var isMpPreparing by remember(book.id, selectedChapterIndex) { mutableStateOf(false) }
  val listState = rememberLazyListState()

  fun releaseMediaPlayerSafely() {
    isMpPrepared = false
    isMpPreparing = false
    isPlayingHumanAudio = false
    val mp = mediaPlayerRef.value
    mediaPlayerRef.value = null
    if (mp != null) {
      try {
        mp.setOnPreparedListener(null)
        mp.setOnCompletionListener(null)
        mp.setOnErrorListener(null)
      } catch (_: Exception) {}
      try {
        mp.reset()
      } catch (_: Exception) {}
      try {
        mp.release()
      } catch (_: Exception) {}
    }
  }

  LaunchedEffect(book.id) {
    val abRef = book.audiobookRef ?: return@LaunchedEffect
    isLoadingChapters = true
    val loadRes = GutendexAndLibriVoxService.loadCompleteBookChaptersAndAudio(
      context = context,
      book = abRef
    )
    if (loadRes.chapters.isNotEmpty()) {
      loadedFullChapters = loadRes.chapters
    }
    isLoadingChapters = false
  }

  DisposableEffect(book.id, selectedChapterIndex) {
    onDispose {
      releaseMediaPlayerSafely()
      speechManager?.stopSpeaking()
    }
  }

  val activeChapter: FullBookChapter? = loadedFullChapters.getOrNull(selectedChapterIndex)
  val pages: List<String> = if (activeChapter != null && activeChapter.paragraphs.isNotEmpty()) {
    activeChapter.paragraphs.chunked(3).map { it.joinToString("\n\n") }
  } else {
    book.pages
  }
  val safePage = currentPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
  val pageText = pages.getOrElse(safePage) { "" }
  val pageTranslation = book.translatedPages.getOrNull(safePage)
    ?.takeIf { !isDuplicateReadingTranslation(pageText, it) }
  val chapterAudioUrl = activeChapter?.librivoxAudioUrl
    ?: book.audiobookRef?.librivoxEditions?.firstOrNull()?.sections?.getOrNull(selectedChapterIndex)?.listenUrl

  val progress = (safePage + 1).toFloat() / pages.size.coerceAtLeast(1).toFloat()

  LaunchedEffect(safePage, autoScrollEnabled) {
    if (autoScrollEnabled) {
      listState.animateScrollToItem(0)
    }
  }

  Scaffold(
    containerColor = LingoTokens.bgCanvas,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      Surface(color = LingoTokens.bgCanvas) {
        Column(modifier = Modifier.statusBarsPadding()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                speechManager?.stopSpeaking()
                onBack()
              },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LingoTokens.textPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${book.author} • Page ${safePage + 1}/${pages.size.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelSmall,
                color = LingoTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            IconButton(
              onClick = onToggleBookmark,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Book",
                tint = if (isBookmarked) LingoTokens.accentAmber else LingoTokens.textSecondary
              )
            }
          }
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(2.5.dp),
            color = LingoTokens.accentPrimary,
            trackColor = LingoTokens.bgSubtle
          )
        }
      }
    },
    bottomBar = {
      CompactReaderToolbar(
        isPlaying = isPlayingTts || isPlayingHumanAudio,
        showTranslation = showTranslation,
        autoScrollEnabled = autoScrollEnabled,
        canGoPrevious = safePage > 0,
        canGoNext = safePage < pages.size - 1,
        onPrevious = { if (safePage > 0) currentPageIndex = safePage - 1 },
        onTogglePlay = {
          if (!chapterAudioUrl.isNullOrBlank()) {
            if (isPlayingHumanAudio || isMpPreparing) {
              if (isMpPrepared) {
                try { mediaPlayerRef.value?.pause() } catch (_: Exception) {}
                isPlayingHumanAudio = false
              } else {
                releaseMediaPlayerSafely()
              }
            } else {
              audioNote = null
              val existing = mediaPlayerRef.value
              if (existing != null && isMpPrepared) {
                try {
                  existing.start()
                  isPlayingHumanAudio = true
                } catch (_: Exception) {
                  releaseMediaPlayerSafely()
                }
              } else if (existing != null) {
                releaseMediaPlayerSafely()
              }
              if (mediaPlayerRef.value == null) {
                try {
                  isMpPreparing = true
                  isMpPrepared = false
                  val mp = MediaPlayer().apply {
                    setDataSource(chapterAudioUrl)
                    setOnPreparedListener { player ->
                      if (mediaPlayerRef.value === player) {
                        isMpPreparing = false
                        isMpPrepared = true
                        try {
                          player.start()
                          isPlayingHumanAudio = true
                        } catch (_: Exception) {
                          releaseMediaPlayerSafely()
                        }
                      }
                    }
                    setOnCompletionListener { isPlayingHumanAudio = false }
                    setOnErrorListener { _, _, _ ->
                      releaseMediaPlayerSafely()
                      audioNote = "Human stream unavailable; using AI Read Aloud."
                      isPlayingTts = true
                      speechManager?.speak(pageText, languageCode, 0.94f)
                      true
                    }
                    prepareAsync()
                  }
                  mediaPlayerRef.value = mp
                } catch (_: Exception) {
                  releaseMediaPlayerSafely()
                  audioNote = "Unable to stream chapter audio; using AI Read Aloud."
                  isPlayingTts = true
                  speechManager?.speak(pageText, languageCode, 0.94f)
                }
              }
            }
          } else {
            if (isPlayingTts) {
              isPlayingTts = false
              speechManager?.stopSpeaking()
            } else {
              isPlayingTts = true
              speechManager?.speak(pageText, languageCode, 0.94f)
            }
          }
        },
        onNext = { if (safePage < pages.size - 1) currentPageIndex = safePage + 1 },
        onOpenSettings = { showSettingsSheet = true },
        onToggleTranslation = { showTranslation = !showTranslation },
        onToggleAutoScroll = { autoScrollEnabled = !autoScrollEnabled },
        showHumanAudioBadge = !chapterAudioUrl.isNullOrBlank()
      )
    }
  ) { innerPadding ->
    val hMargin = when (readingWidthMode) {
      ReadingWidthMode.COMPACT -> 28.dp
      ReadingWidthMode.STANDARD -> 20.dp
      ReadingWidthMode.WIDE -> 12.dp
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = hMargin)
        .testTag("book_interactive_reader_card"),
      contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (isLoadingChapters) {
        item {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = LingoTokens.accentPrimary,
            trackColor = LingoTokens.bgSubtle
          )
        }
      }

      if (!audioNote.isNullOrBlank()) {
        item {
          LingoStatusBadge(text = audioNote.orEmpty(), color = LingoTokens.accentAmber)
        }
      }

      if (safePage == 0) {
        item {
          val formatBadge = when {
            book.hasHumanAudio && book.hasFullText -> "${book.sourceName} • Audio + Full Text"
            book.hasHumanAudio -> "${book.sourceName} • Audio"
            book.hasFullText -> "${book.sourceName} • Full Text"
            else -> "${book.sourceName} • Summary"
          }
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LingoStatusBadge(
              text = "${book.difficulty} • $formatBadge",
              color = if (book.hasHumanAudio) LingoTokens.accentAmber else LingoTokens.accentPrimary
            )
            Text(
              text = book.title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary
            )
            Text(
              text = "by ${book.author}",
              style = MaterialTheme.typography.bodyMedium,
              color = LingoTokens.textSecondary
            )
            HorizontalDivider(color = LingoTokens.borderSubtle)
          }
        }
      }

      if (loadedFullChapters.size > 1) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            loadedFullChapters.forEachIndexed { chIdx, ch ->
              val selected = selectedChapterIndex == chIdx
              FilterChip(
                selected = selected,
                onClick = {
                  selectedChapterIndex = chIdx
                  currentPageIndex = 0
                },
                label = {
                  Text(
                    text = ch.title.take(28),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.2f),
                  selectedLabelColor = LingoTokens.accentPrimary,
                  containerColor = LingoTokens.bgElevated,
                  labelColor = LingoTokens.textSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = LingoTokens.borderSubtle,
                  selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.5f)
                )
              )
            }
          }
        }
      }

      item {
        Text(
          text = pageText,
          fontSize = fontScale.bodySizeSp.sp,
          lineHeight = (fontScale.bodySizeSp * lineSpacing.lineHeightMultiplier).sp,
          color = LingoTokens.textPrimary
        )
      }

      if (showTranslation && pageTranslation != null) {
        item {
          HorizontalDivider(color = LingoTokens.borderSubtle)
          Text(
            text = pageTranslation,
            fontSize = (fontScale.bodySizeSp - 2).coerceAtLeast(13).sp,
            lineHeight = ((fontScale.bodySizeSp - 2).coerceAtLeast(13) * 1.45f).sp,
            fontStyle = FontStyle.Italic,
            color = LingoTokens.textSecondary
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              scope.launch {
                isAnalyzingBook = true
                val prompt = "Provide a concise 3-bullet CEFR ${book.difficulty} summary and 3 key vocabulary explanations for this excerpt from '${book.title}' by ${book.author}:\n\n${pageText.take(600)}"
                val res = GeminiTutorService.generateGeminiText(prompt, "READING_SUMMARY")
                aiStudyInsight = res ?: "Study Notes (${book.title}):\n• ${book.summaryTagline}\n• Key Vocabulary: ${book.glossary.entries.take(3).joinToString { "${it.key} (${it.value})" }}"
                isAnalyzingBook = false
              }
            },
            enabled = !isAnalyzingBook,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("book_ai_study_guide_btn")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoTokens.accentIndigo, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isAnalyzingBook) "Analyzing..." else "AI Study Notes",
              color = LingoTokens.accentIndigo,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1
            )
          }

          if (book.questions.isNotEmpty()) {
            Button(
              onClick = {
                speechManager?.stopSpeaking()
                onStartQuiz()
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = LingoTokens.accentPrimary,
                contentColor = LingoTokens.onAccentDark
              ),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("book_open_quiz_btn")
            ) {
              Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isCompleted) "Review Quiz" else "Comprehension",
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
            }
          }
        }
      }

      if (!aiStudyInsight.isNullOrBlank()) {
        item {
          LingoCleanCard(
            containerColor = LingoTokens.bgSubtle,
            borderColor = LingoTokens.accentIndigo.copy(alpha = 0.4f)
          ) {
            Text(
              text = "AI Study Notes",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.accentIndigo
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = aiStudyInsight.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = LingoTokens.textPrimary
            )
          }
        }
      }

      if (book.glossary.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Key Vocabulary (Tap to hear & save)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary
            )
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              book.glossary.forEach { (term, definition) ->
                val isSaved = savedWordsInBook.contains("${book.id}_$term")
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSaved) LingoTokens.accentEmerald.copy(alpha = 0.16f) else LingoTokens.bgElevated,
                  border = BorderStroke(
                    1.dp,
                    if (isSaved) LingoTokens.accentEmerald.copy(alpha = 0.6f) else LingoTokens.borderSubtle
                  ),
                  modifier = Modifier.clickable {
                    speechManager?.speak(term, languageCode)
                    if (!isSaved) {
                      onSaveVocabToFlashcard(term, definition, "")
                      savedWordsInBook = savedWordsInBook + "${book.id}_$term"
                    }
                  }
                ) {
                  Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                      text = if (isSaved) "$term ✓ Saved" else "+ $term",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = if (isSaved) LingoTokens.accentEmerald else LingoTokens.accentPrimary
                    )
                    Text(
                      text = definition,
                      style = MaterialTheme.typography.labelSmall,
                      color = LingoTokens.textSecondary
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

  if (showSettingsSheet) {
    ReadingSettingsBottomSheet(
      fontScale = fontScale,
      lineSpacing = lineSpacing,
      readingWidthMode = readingWidthMode,
      onFontScaleChange = { fontScale = it },
      onLineSpacingChange = { lineSpacing = it },
      onReadingWidthChange = { readingWidthMode = it },
      onOpenTeleprompter = null,
      onDismiss = { showSettingsSheet = false }
    )
  }
}

@Composable
private fun CompactReaderToolbar(
  isPlaying: Boolean,
  showTranslation: Boolean,
  autoScrollEnabled: Boolean,
  canGoPrevious: Boolean,
  canGoNext: Boolean,
  onPrevious: () -> Unit,
  onTogglePlay: () -> Unit,
  onNext: () -> Unit,
  onOpenSettings: () -> Unit,
  onToggleTranslation: () -> Unit,
  onToggleAutoScroll: () -> Unit,
  showHumanAudioBadge: Boolean = false
) {
  Surface(
    color = LingoTokens.bgElevated,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.navigationBarsPadding()) {
      HorizontalDivider(color = LingoTokens.borderSubtle, thickness = 0.8.dp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier
              .size(48.dp)
              .testTag("reading_prev_para_button")
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = "Previous",
              tint = if (canGoPrevious) LingoTokens.textPrimary else LingoTokens.textMuted
            )
          }

          Surface(
            color = if (isPlaying) LingoTokens.accentAmber else LingoTokens.accentPrimary,
            shape = CircleShape,
            modifier = Modifier
              .size(48.dp)
              .clickable(onClick = onTogglePlay)
              .testTag("reading_play_pause_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = when {
                  isPlaying -> Icons.Default.Pause
                  showHumanAudioBadge -> Icons.Default.Headphones
                  else -> Icons.Default.PlayArrow
                },
                contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                tint = LingoTokens.onAccentDark,
                modifier = Modifier.size(22.dp)
              )
            }
          }

          IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier
              .size(48.dp)
              .testTag("reading_next_para_button")
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next",
              tint = if (canGoNext) LingoTokens.textPrimary else LingoTokens.textMuted
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
              .size(48.dp)
              .testTag("reading_settings_menu_button")
          ) {
            Icon(
              imageVector = Icons.Default.TextFields,
              contentDescription = "Font Size & Reading Width Settings",
              tint = LingoTokens.textPrimary
            )
          }

          IconButton(
            onClick = onToggleTranslation,
            modifier = Modifier
              .size(48.dp)
              .testTag("reading_parallel_translation_toggle")
          ) {
            Icon(
              imageVector = Icons.Default.Translate,
              contentDescription = if (showTranslation) "Turn off Translation" else "Turn on Translation",
              tint = if (showTranslation) LingoTokens.accentEmerald else LingoTokens.textSecondary
            )
          }

          IconButton(
            onClick = onToggleAutoScroll,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VerticalAlignCenter,
              contentDescription = if (autoScrollEnabled) "Disable Auto-Scroll" else "Enable Auto-Scroll",
              tint = if (autoScrollEnabled) LingoTokens.accentPrimary else LingoTokens.textSecondary
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingSettingsBottomSheet(
  fontScale: ReadingFontScale,
  lineSpacing: ReadingLineSpacing,
  readingWidthMode: ReadingWidthMode,
  onFontScaleChange: (ReadingFontScale) -> Unit,
  onLineSpacingChange: (ReadingLineSpacing) -> Unit,
  onReadingWidthChange: (ReadingWidthMode) -> Unit,
  onOpenTeleprompter: (() -> Unit)?,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = LingoTokens.bgElevated,
    contentColor = LingoTokens.textPrimary,
    modifier = Modifier.testTag("reading_settings_panel")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Reading Settings",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary
        )
        Text(
          text = "Done",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.accentPrimary,
          modifier = Modifier
            .clickable(onClick = onDismiss)
            .padding(8.dp)
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Font Size", style = MaterialTheme.typography.labelMedium, color = LingoTokens.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReadingFontScale.entries.forEach { scale ->
            val selected = fontScale == scale
            Surface(
              color = if (selected) LingoTokens.accentPrimary.copy(alpha = 0.2f) else LingoTokens.bgSubtle,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (selected) LingoTokens.accentPrimary else LingoTokens.borderSubtle),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clickable { onFontScaleChange(scale) }
                .testTag("reading_font_${scale.name.lowercase()}")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = scale.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = if (selected) LingoTokens.accentPrimary else LingoTokens.textSecondary
                )
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Line Spacing", style = MaterialTheme.typography.labelMedium, color = LingoTokens.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReadingLineSpacing.entries.forEach { spacing ->
            val selected = lineSpacing == spacing
            Surface(
              color = if (selected) LingoTokens.accentPrimary.copy(alpha = 0.2f) else LingoTokens.bgSubtle,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (selected) LingoTokens.accentPrimary else LingoTokens.borderSubtle),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clickable { onLineSpacingChange(spacing) }
                .testTag("reading_spacing_${spacing.name.lowercase()}")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = spacing.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = if (selected) LingoTokens.accentPrimary else LingoTokens.textSecondary
                )
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reading Width", style = MaterialTheme.typography.labelMedium, color = LingoTokens.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReadingWidthMode.entries.forEach { widthOpt ->
            val selected = readingWidthMode == widthOpt
            Surface(
              color = if (selected) LingoTokens.accentPrimary.copy(alpha = 0.2f) else LingoTokens.bgSubtle,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (selected) LingoTokens.accentPrimary else LingoTokens.borderSubtle),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clickable { onReadingWidthChange(widthOpt) }
                .testTag("reading_width_${widthOpt.name.lowercase()}")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = widthOpt.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = if (selected) LingoTokens.accentPrimary else LingoTokens.textSecondary
                )
              }
            }
          }
        }
      }

      if (onOpenTeleprompter != null) {
        OutlinedButton(
          onClick = onOpenTeleprompter,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Text("Open Full-Screen Teleprompter", fontWeight = FontWeight.SemiBold, color = LingoTokens.accentPrimary)
        }
      }
    }
  }
}
