package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AudiobookLearningService
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.ui.components.AudiobookPlayerMode
import com.example.ui.screens.BooksListeningInitialSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BooksAndListeningStudioVerificationTest {

  @Test
  fun separateReadingAndListeningProgress_doNotOverwriteEachOther() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "test_books_user_1"
    val bookId = "gutenberg_1342"

    // 1. Save reading progress at Chapter 5, page 3, scroll 1420
    val afterRead = AudiobookLearningService.saveReadingProgress(
      context = context,
      userId = userId,
      bookId = bookId,
      bookTitle = "Pride and Prejudice",
      chapterIndex = 4,
      scrollOffset = 1420,
      pageIndex = 3,
      progressPercent = 0.45f,
      readingTheme = "SEPIA",
      fontSizeSp = 19f,
      isPageMode = true,
      completedReadingChapters = setOf(0, 1, 2, 3)
    )
    assertEquals(4, afterRead.readingChapterIndex)
    assertEquals(3, afterRead.readingPageIndex)
    assertEquals(1420, afterRead.readingScrollOffset)
    assertEquals(0.45f, afterRead.readingPercent, 0.001f)
    assertEquals(setOf(0, 1, 2, 3), afterRead.completedReadingChapterIndices)

    // 2. Save listening progress at Chapter 2, 95 seconds -> must NOT overwrite reading state
    val afterListen = AudiobookLearningService.saveListeningProgress(
      context = context,
      userId = userId,
      bookId = bookId,
      bookTitle = "Pride and Prejudice",
      chapterIndex = 1,
      sentenceIndex = 18,
      positionMs = 95_000L,
      progressPercent = 0.18f,
      preferredEditionId = "librivox_edition_1",
      autoPlayNextChapter = true,
      skipRecordingAnnouncements = true,
      completedChapterIndices = setOf(0),
      chapterProgressMap = mapOf(0 to 1f, 1 to 0.35f),
      chapterAudioPositionMsMap = mapOf(1 to 95_000L),
      chapterSentenceIndexMap = mapOf(1 to 18),
      isCompleted = false
    )

    // Listening fields updated
    assertEquals(1, afterListen.lastEpisodeIndex)
    assertEquals(18, afterListen.lastSentenceIndex)
    assertEquals(95_000L, afterListen.lastPlaybackPositionMs)
    assertEquals(0.18f, afterListen.listeningPercent, 0.001f)
    assertEquals(setOf(0), afterListen.completedChapterIndices)

    // Reading fields preserved intact
    assertEquals(4, afterListen.readingChapterIndex)
    assertEquals(3, afterListen.readingPageIndex)
    assertEquals(1420, afterListen.readingScrollOffset)
    assertEquals(0.45f, afterListen.readingPercent, 0.001f)
    assertEquals("SEPIA", afterListen.readingTheme)
    assertEquals(true, afterListen.isPageMode)
    assertEquals(setOf(0, 1, 2, 3), afterListen.completedReadingChapterIndices)

    // 3. Advance reading progress again -> must NOT overwrite listening state
    val afterSecondRead = AudiobookLearningService.saveReadingProgress(
      context = context,
      userId = userId,
      bookId = bookId,
      bookTitle = "Pride and Prejudice",
      chapterIndex = 6,
      scrollOffset = 2200,
      pageIndex = 5,
      progressPercent = 0.62f,
      readingTheme = "DARK",
      fontSizeSp = 20f,
      isPageMode = false,
      completedReadingChapters = setOf(0, 1, 2, 3, 4, 5)
    )
    assertEquals(6, afterSecondRead.readingChapterIndex)
    assertEquals(0.62f, afterSecondRead.readingPercent, 0.001f)
    assertEquals(1, afterSecondRead.lastEpisodeIndex)
    assertEquals(95_000L, afterSecondRead.lastPlaybackPositionMs)
    assertEquals(0.18f, afterSecondRead.listeningPercent, 0.001f)
  }

  @Test
  fun normalizeSecureBookUrl_upgradesExternalHttpAndPreservesLocalTestHosts() {
    assertEquals(
      "https://www.gutenberg.org/cache/epub/1342/pg1342.txt",
      GutendexAndLibriVoxService.normalizeSecureBookUrl("http://www.gutenberg.org/cache/epub/1342/pg1342.txt")
    )
    assertEquals(
      "https://archive.org/download/pride_prejudice/chapter1.mp3",
      GutendexAndLibriVoxService.normalizeSecureBookUrl("http://archive.org/download/pride_prejudice/chapter1.mp3")
    )
    assertEquals(
      "https://www.archive.org/stream/test.mp3",
      GutendexAndLibriVoxService.normalizeSecureBookUrl("//www.archive.org/stream/test.mp3")
    )
    assertEquals(
      "http://127.0.0.1:44556/books/1342.txt",
      GutendexAndLibriVoxService.normalizeSecureBookUrl("http://127.0.0.1:44556/books/1342.txt")
    )
    assertEquals(
      "http://localhost:8080/audio.mp3",
      GutendexAndLibriVoxService.normalizeSecureBookUrl("http://localhost:8080/audio.mp3")
    )
  }

  @Test
  fun parseAuthenticChapters_splitsMonolithicSingleHeadingBookIntoMultipleChapters() {
    val paragraph = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife. ".repeat(12)
    val hugeBook = buildString {
      append("CHAPTER I\n\n")
      repeat(24) { idx ->
        append("Paragraph $idx. ")
        append(paragraph)
        append("\n\n")
      }
    }
    assertTrue("Fixture should exceed 18,000 chars", hugeBook.length > 18_000)

    val chapters = GutendexAndLibriVoxService.parseAuthenticChapters(hugeBook, "Pride and Prejudice")
    assertTrue("Monolithic >18k-char book should split into multiple navigable chapters, got ${chapters.size}", chapters.size >= 2)
    assertTrue("Every chapter must have real non-blank text", chapters.all { it.fullText.length > 500 })
  }

  @Test
  fun playerModesAndStudioEntrySections_coverReadListenReadAndListenAndAcademy() {
    val modes = AudiobookPlayerMode.entries.toSet()
    assertTrue(AudiobookPlayerMode.READ_ONLY in modes)
    assertTrue(AudiobookPlayerMode.LISTEN_ONLY in modes)
    assertTrue(AudiobookPlayerMode.READ_AND_LISTEN in modes)
    assertTrue(AudiobookPlayerMode.LEARN_MODE in modes)

    val sections = BooksListeningInitialSection.entries.toSet()
    assertTrue(BooksListeningInitialSection.MAIN in sections)
    assertTrue(BooksListeningInitialSection.BOOKS in sections)
    assertTrue(BooksListeningInitialSection.READING_ACADEMY in sections)
  }
}
