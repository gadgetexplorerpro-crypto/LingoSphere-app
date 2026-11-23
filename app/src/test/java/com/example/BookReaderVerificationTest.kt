package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.AudiobookBookmark
import com.example.data.model.BookContentType
import com.example.data.model.FullBookChapter
import com.example.data.model.LibriVoxEdition
import com.example.data.model.LibriVoxSection
import com.example.data.model.TutorVoice
import com.example.data.repository.AudiobookCatalogRepository
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.speech.AudiobookMediaController
import com.example.speech.SpeechManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookReaderVerificationTest {

  private lateinit var context: Context
  private lateinit var speechManager: SpeechManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    speechManager = SpeechManager(context)
    GutendexAndLibriVoxService.clearAllCaches()
    AudiobookMediaController.stopAndClearMiniPlayer()
    AudiobookMediaController.attach(context, speechManager, "test_user_book_reader")
  }

  @After
  fun tearDown() {
    AudiobookMediaController.stopAndClearMiniPlayer()
    speechManager.release()
  }

  private fun findCatalogBook(id: String): AudiobookBook {
    return AudiobookCatalogRepository.FAMOUS_FULL_BOOKS.first { it.id == id }
  }

  @Test
  fun testSevenRealBooksMetadataAndCompleteChapterSynchronization() {
    data class ExpectedBookSpec(
      val id: String,
      val expectedTitleContains: String,
      val expectedAuthorContains: String,
      val expectedMinChapters: Int,
      val firstChapterTitleContains: String,
      val lastChapterTitleContains: String,
      val expectedUrlKeyword: String,
      val uniqueBodyPhrase: String
    )

    val specs = listOf(
      ExpectedBookSpec(
        id = "wattles_science_getting_rich",
        expectedTitleContains = "The Science of Getting Rich",
        expectedAuthorContains = "Wattles",
        expectedMinChapters = 17,
        firstChapterTitleContains = "Chapter 1",
        lastChapterTitleContains = "Chapter 17",
        expectedUrlKeyword = "science_getting_rich_librivox",
        uniqueBodyPhrase = "right to be rich"
      ),
      ExpectedBookSpec(
        id = "gutenberg_1342",
        expectedTitleContains = "Pride and Prejudice",
        expectedAuthorContains = "Austen",
        expectedMinChapters = 61,
        firstChapterTitleContains = "Chapter 1",
        lastChapterTitleContains = "Chapter 61",
        expectedUrlKeyword = "prideandprejudice_1005_librivox",
        uniqueBodyPhrase = "single man in possession of a good fortune"
      ),
      ExpectedBookSpec(
        id = "gutenberg_1661",
        expectedTitleContains = "Sherlock Holmes",
        expectedAuthorContains = "Doyle",
        expectedMinChapters = 12,
        firstChapterTitleContains = "Scandal in Bohemia",
        lastChapterTitleContains = "Copper Beeches",
        expectedUrlKeyword = "adventures_sherlockholmes_1007_librivox",
        uniqueBodyPhrase = "To Sherlock Holmes she is always"
      ),
      ExpectedBookSpec(
        id = "gutenberg_11",
        expectedTitleContains = "Alice's Adventures in Wonderland",
        expectedAuthorContains = "Carroll",
        expectedMinChapters = 12,
        firstChapterTitleContains = "Rabbit-Hole",
        lastChapterTitleContains = "Alice's Evidence",
        expectedUrlKeyword = "alices_adventures_1005_librivox",
        uniqueBodyPhrase = "beginning to get very tired of sitting by her sister"
      ),
      ExpectedBookSpec(
        id = "gutenberg_84",
        expectedTitleContains = "Frankenstein",
        expectedAuthorContains = "Shelley",
        expectedMinChapters = 24,
        firstChapterTitleContains = "Chapter 1",
        lastChapterTitleContains = "Chapter 24",
        expectedUrlKeyword = "frankenstein_shelley",
        uniqueBodyPhrase = "I am by birth a Genevese"
      ),
      ExpectedBookSpec(
        id = "gutenberg_4507",
        expectedTitleContains = "As a Man Thinketh",
        expectedAuthorContains = "Allen",
        expectedMinChapters = 7,
        firstChapterTitleContains = "Thought and Character",
        lastChapterTitleContains = "Serenity",
        expectedUrlKeyword = "as_a_man_thinketh_librivox",
        uniqueBodyPhrase = "As a man thinketh in his heart"
      ),
      ExpectedBookSpec(
        id = "gutenberg_132",
        expectedTitleContains = "The Art of War",
        expectedAuthorContains = "Sun Tzu",
        expectedMinChapters = 13,
        firstChapterTitleContains = "Laying Plans",
        lastChapterTitleContains = "Spies",
        expectedUrlKeyword = "art_of_war_librivox",
        uniqueBodyPhrase = "art of war is of vital importance to the State"
      )
    )

    val seenIds = mutableSetOf<String>()
    for (spec in specs) {
      val book = findCatalogBook(spec.id)

      // 1. Unique book ID, title, author, cover
      assertTrue("Duplicate book ID: ${book.id}", seenIds.add(book.id))
      assertTrue(book.title.contains(spec.expectedTitleContains, ignoreCase = true))
      assertTrue(book.author.contains(spec.expectedAuthorContains, ignoreCase = true))
      assertTrue("Missing cover URL for ${book.title}", book.coverArtUrl.startsWith("http"))

      // 2. Complete chapters in original order
      val chapters = book.parsedChapters
      assertTrue(
        "${book.title} expected >= ${spec.expectedMinChapters} chapters but got ${chapters.size}",
        chapters.size >= spec.expectedMinChapters
      )
      assertTrue(
        "${book.title} first chapter '${chapters.first().title}' should contain '${spec.firstChapterTitleContains}'",
        chapters.first().title.contains(spec.firstChapterTitleContains, ignoreCase = true)
      )
      assertTrue(
        "${book.title} last chapter '${chapters.last().title}' should contain '${spec.lastChapterTitleContains}'",
        chapters.last().title.contains(spec.lastChapterTitleContains, ignoreCase = true)
      )

      // 3. Text & Narration synchronization across EVERY chapter from Chapter 1 to Final Chapter
      val joinedFullText = chapters.joinToString("\n") { it.fullText }
      assertTrue(
        "${book.title} should contain '${spec.uniqueBodyPhrase}'",
        joinedFullText.contains(spec.uniqueBodyPhrase, ignoreCase = true)
      )
      assertFalse(
        "${book.title} must NEVER contain Adam and Eve text",
        joinedFullText.contains("Adam and Eve", ignoreCase = true)
      )

      for (idx in chapters.indices) {
        val ch = chapters[idx]
        assertEquals(idx, ch.chapterIndex)

        // Verify AI Voice sentences come directly from the displayed chapter text
        val aiSentences = AudiobookMediaController.getChapterSentences(book, idx)
        assertTrue("${book.title} ch $idx AI sentences must not be empty", aiSentences.isNotEmpty())
        assertTrue(
          "${book.title} ch $idx AI sentence '${aiSentences.first()}' must match chapter fullText",
          ch.fullText.contains(aiSentences.first().take(40))
        )

        // Verify Human Narrator MP3 URL belongs strictly to this book and chapter
        val mp3Url = AudiobookMediaController.getChapterMp3Url(book, idx)
        assertNotNull("${book.title} ch $idx MP3 URL must exist", mp3Url)
        assertTrue(
          "${book.title} ch $idx MP3 URL ($mp3Url) must belong to '${spec.expectedUrlKeyword}'",
          mp3Url!!.contains(spec.expectedUrlKeyword, ignoreCase = true)
        )
      }
    }
  }

  @Test
  fun testFrankensteinChaptersDoNotShiftAudioMapping() {
    val frankenstein = findCatalogBook("gutenberg_84")
    val chapters = frankenstein.parsedChapters
    assertEquals(24, chapters.size)

    // Chapters 1..24 (indices 0..23) MUST map strictly to _01_shelley .. _24_shelley
    for (chNum in 1..24) {
      val idx = chNum - 1
      assertTrue(
        "Index $idx should be Chapter $chNum, got ${chapters[idx].title}",
        chapters[idx].title.contains("Chapter $chNum", ignoreCase = true)
      )
      val expectedToken = "_${chNum.toString().padStart(2, '0')}_shelley"
      val url = AudiobookMediaController.getChapterMp3Url(frankenstein, idx)
      assertNotNull(url)
      assertTrue(
        "Chapter $chNum must pair with $expectedToken, got $url",
        url!!.contains(expectedToken)
      )
    }
  }

  @Test
  fun testMismatchedAdamAndEveTextRejectedForOtherBooks() {
    val wattlesBook = findCatalogBook("wattles_science_getting_rich")
    val adamAndEveSample = """
      The Project Gutenberg eBook of The First Book of Adam and Eve
      Title: The First Book of Adam and Eve
      Author: Rutherford H. Platt
      *** START OF THE PROJECT GUTENBERG EBOOK THE FIRST BOOK OF ADAM AND EVE ***
      When God made Adam and Eve and placed them in the garden.
    """.trimIndent()

    assertFalse(
      "Must reject Adam and Eve text for The Science of Getting Rich",
      GutendexAndLibriVoxService.isDownloadedTextMatchingBook(adamAndEveSample, wattlesBook)
    )

    val validWattlesSample = """
      The Project Gutenberg EBook of The Science of Getting Rich
      Title: The Science of Getting Rich
      Author: Wallace D. Wattles
      *** START OF THE PROJECT GUTENBERG EBOOK THE SCIENCE OF GETTING RICH ***
      PREFACE: This book is pragmatical, not philosophical.
    """.trimIndent()

    assertTrue(
      "Must accept genuine Science of Getting Rich text",
      GutendexAndLibriVoxService.isDownloadedTextMatchingBook(validWattlesSample, wattlesBook)
    )

    assertFalse(
      "Must reject cross-book author/title match in LibriVox verification",
      GutendexAndLibriVoxService.isSameBookTitleAndAuthor(
        candidateTitle = "The First Book of Adam and Eve",
        candidateAuthor = "Rutherford H. Platt",
        expectedTitle = wattlesBook.title,
        expectedAuthor = wattlesBook.author
      )
    )
  }

  @Test
  fun testSwitchingBooksResetsAudioAndPreventsStaleMetadataOverwrite() {
    val pride = findCatalogBook("gutenberg_1342")
    val wattles = findCatalogBook("wattles_science_getting_rich")
    val sherlock = findCatalogBook("gutenberg_1661")

    // Open Pride and Prejudice at Chapter 15 (index 14)
    AudiobookMediaController.onBookOpened(pride, 14)
    AudiobookMediaController.loadBookAndChapter(pride, chapterIndex = 14, sentenceIndex = 2, autoPlay = false)
    assertEquals(pride.id, AudiobookMediaController.activeBook.value?.id)
    assertEquals(14, AudiobookMediaController.activeChapterIndex.value)
    assertTrue(
      AudiobookMediaController.getChapterMp3Url(pride, 14)!!.contains("prideandprejudice")
    )

    // Switch to The Science of Getting Rich at Chapter 1 (index 0)
    AudiobookMediaController.onBookOpened(wattles, 0)
    AudiobookMediaController.loadBookAndChapter(wattles, chapterIndex = 0, sentenceIndex = 0, autoPlay = false)
    assertEquals(wattles.id, AudiobookMediaController.activeBook.value?.id)
    assertEquals(0, AudiobookMediaController.activeChapterIndex.value)
    assertEquals(0, AudiobookMediaController.currentSentenceIndex.value)
    assertEquals(0L, AudiobookMediaController.currentPositionMs.value)

    // Late async response from Pride and Prejudice MUST NOT overwrite active Science of Getting Rich book
    AudiobookMediaController.updateActiveBookMetadata(pride.copy(shortDescription = "Stale overwrite attempt"))
    assertEquals(wattles.id, AudiobookMediaController.activeBook.value?.id)

    // Next / Previous chapter stay strictly inside The Science of Getting Rich
    AudiobookMediaController.nextChapter()
    assertEquals(wattles.id, AudiobookMediaController.activeBook.value?.id)
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)
    assertTrue(
      AudiobookMediaController.getChapterTitle(wattles, 1).contains("Science of Getting Rich", ignoreCase = true)
    )
    AudiobookMediaController.previousChapter()
    assertEquals(0, AudiobookMediaController.activeChapterIndex.value)

    // Switch to Sherlock Holmes
    AudiobookMediaController.onBookOpened(sherlock, 0)
    AudiobookMediaController.loadBookAndChapter(sherlock, chapterIndex = 0, sentenceIndex = 0, autoPlay = false)
    assertEquals(sherlock.id, AudiobookMediaController.activeBook.value?.id)
    assertEquals(0, AudiobookMediaController.activeChapterIndex.value)
    assertNotEquals(pride.id, AudiobookMediaController.activeBook.value?.id)
    assertTrue(
      AudiobookMediaController.getChapterSentences(sherlock, 0).first()
        .contains("To Sherlock Holmes", ignoreCase = true)
    )
  }

  @Test
  fun testHumanNarrationUnavailableDoesNotSubstituteAnotherBookRecording() {
    val unrecordedBook = AudiobookBook(
      id = "test_unrecorded_book_999",
      title = "Unrecorded Original Essay",
      author = "Test Author",
      primaryCategory = "Essays",
      subCategory = "Philosophy",
      shortDescription = "An unrecorded original essay used to verify unavailable human narration behavior.",
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      hasFullTextAvailable = true,
      parsedChapters = listOf(
        FullBookChapter(
          id = "ch_1",
          chapterIndex = 0,
          chapterNumber = 1,
          title = "Chapter 1: Original Text",
          fullText = "This chapter only has written text and no LibriVox recording.",
          paragraphs = listOf("This chapter only has written text and no LibriVox recording."),
          sentences = listOf("This chapter only has written text and no LibriVox recording."),
          librivoxAudioUrl = null
        )
      ),
      librivoxEditions = emptyList()
    )

    // Load unrecorded book cleanly
    AudiobookMediaController.onBookOpened(unrecordedBook, 0)
    AudiobookMediaController.loadBookAndChapter(unrecordedBook, chapterIndex = 0, autoPlay = false)
    assertNull(AudiobookMediaController.getChapterMp3Url(unrecordedBook, 0))

    // Attempt to select Human Narrator -> must report unavailable and never substitute another recording
    AudiobookMediaController.setNarrationEditionMode(useHumanLibriVox = true)
    assertFalse(AudiobookMediaController.useHumanNarrator.value)
    assertEquals("Human narration unavailable", AudiobookMediaController.humanNarrationStatusMessage.value)

    // Switching to AI Voice clears unavailable message and prepares exact displayed chapter sentences
    AudiobookMediaController.setNarrationEditionMode(useHumanLibriVox = false)
    assertNull(AudiobookMediaController.humanNarrationStatusMessage.value)
    assertEquals(
      listOf("This chapter only has written text and no LibriVox recording."),
      AudiobookMediaController.getChapterSentences(unrecordedBook, 0)
    )
  }

  @Test
  fun testNarrationTextCleanerStripsUnderscoresMarkdownAndHtmlWithoutAlteringMeaning() {
    val dirtyInput = """
      ## **Chapter_One** ___
      <p>Hello&nbsp;<b>my_friend</b>! Let&rsquo;s read <i>the_quick_story</i> &mdash; `code_snippet` at 100% + 2 = 3.</p>
      - Check [Project Gutenberg](https://example.org) and ___bold___ ~~old~~ _italic_.
    """.trimIndent()

    val cleaned = com.example.speech.NarrationTextCleaner.cleanForTts(dirtyInput)

    assertFalse("Underscores must be removed from TTS text: $cleaned", cleaned.contains("_"))
    assertFalse("Asterisks must be removed from TTS text: $cleaned", cleaned.contains("*"))
    assertFalse("Backticks must be removed from TTS text: $cleaned", cleaned.contains("`"))
    assertFalse("HTML tags must be removed from TTS text: $cleaned", cleaned.contains("<p>") || cleaned.contains("<b>"))
    assertFalse("Markdown headings must be removed from TTS text: $cleaned", cleaned.contains("##"))
    assertTrue("Snake case words should become separated spoken words: $cleaned", cleaned.contains("Chapter One"))
    assertTrue("Snake case in body should preserve readable words: $cleaned", cleaned.contains("my friend"))
    assertTrue("Inline code text should remain readable: $cleaned", cleaned.contains("code snippet"))
    assertTrue("Markdown link label should stay readable: $cleaned", cleaned.contains("Project Gutenberg"))
    assertTrue("URL from Markdown link should be stripped: $cleaned", !cleaned.contains("https://example.org"))
    assertTrue("Math and punctuation should be preserved: $cleaned", cleaned.contains("100% + 2 = 3."))
  }

  @Test
  fun testExactSentenceSyncAvailableOnlyForAiVoiceAndNotHumanAudio() {
    val wattles = findCatalogBook("wattles_science_getting_rich")
    AudiobookMediaController.onBookOpened(wattles, 0)
    AudiobookMediaController.loadBookAndChapter(wattles, chapterIndex = 0, autoPlay = false)

    AudiobookMediaController.setNarrationEditionMode(useHumanLibriVox = false)
    assertFalse(AudiobookMediaController.useHumanNarrator.value)
    assertTrue(AudiobookMediaController.isExactSentenceSyncAvailable)

    AudiobookMediaController.setNarrationEditionMode(useHumanLibriVox = true)
    assertTrue(AudiobookMediaController.useHumanNarrator.value)
    assertFalse(AudiobookMediaController.isExactSentenceSyncAvailable)
  }

  @Test
  fun testPrideAndPrejudiceChapter2IsCompleteUnabridgedAndAll61ChaptersValidate() {
    val pride = findCatalogBook("gutenberg_1342")
    val baselineCh1 = pride.parsedChapters[0]
    val baselineCh2 = pride.parsedChapters[1]

    // 1. Verify Chapter 1 has all original paragraphs down to its final sentence
    assertTrue(baselineCh1.paragraphs.size >= 25)
    assertTrue(baselineCh1.fullText.startsWith("It is a truth universally acknowledged"))
    assertTrue(
      "Chapter 1 must end at its true final sentence",
      baselineCh1.fullText.trimEnd().endsWith("The business of her life was to get her daughters married; its solace was visiting and news.")
    )

    // 2. Verify Chapter 2 has all 28 original paragraphs down to its actual final sentence (not truncated at paragraph 3!)
    assertTrue("Chapter 2 must have >= 25 paragraphs, got ${baselineCh2.paragraphs.size}", baselineCh2.paragraphs.size >= 25)
    assertTrue("Chapter 2 must have >= 650 words, got ${baselineCh2.wordCount}", baselineCh2.wordCount >= 650)
    assertTrue(baselineCh2.fullText.startsWith("Mr. Bennet was among the earliest of those who waited on Mr. Bingley."))
    assertTrue(
      "Chapter 2 must contain middle dialogue with Kitty, Mary, and Mrs. Bennet",
      baselineCh2.fullText.contains("What say you, Mary? For you are a young lady of deep reflection")
    )
    assertTrue(
      "Chapter 2 must end at its actual final sentence",
      baselineCh2.fullText.trimEnd().endsWith("The rest of the evening was spent in conjecturing how soon he would return Mr. Bennet's visit, and determining when they should ask him to dinner.")
    )
    assertTrue(
      "Chapter 2 must pass isChapterCompleteEnough",
      GutendexAndLibriVoxService.isChapterCompleteEnough(baselineCh2, isFullBook = true)
    )

    // 3. Verify Project Gutenberg 61-chapter parser handles [Illustration: ... CHAPTER I.], VOLUME II/III restarts,
    //    and mid-chapter sentences starting with "Letter I..." or "Part I..." without losing a single paragraph!
    val ch1Text = baselineCh1.fullText
    val ch2Text = baselineCh2.fullText
    val full61GutenbergSample = buildString {
      appendLine("*** START OF THE PROJECT GUTENBERG EBOOK PRIDE AND PREJUDICE ***")
      appendLine("CONTENTS")
      for (i in 1..61) {
        appendLine("Chapter $i")
      }
      appendLine()
      for (ch in 1..61) {
        if (ch == 1) {
          appendLine("VOLUME I")
          appendLine()
          appendLine("[Illustration:")
          appendLine()
          appendLine("  PRIDE & PREJUDICE.")
          appendLine()
          appendLine("  CHAPTER I.]")
          appendLine()
          appendLine(ch1Text)
          appendLine()
          appendLine("Letter I wrote to my sister at Longbourn was answered immediately with warm affection.")
          appendLine()
          appendLine("Part I had in the conversation was small, yet Mr. Darcy listened attentively.")
        } else if (ch == 2) {
          appendLine("[Illustration:")
          appendLine()
          appendLine("  CHAPTER II.")
          appendLine("]")
          appendLine()
          appendLine(ch2Text)
        } else {
          if (ch == 24) {
            appendLine("VOLUME II")
            appendLine()
          } else if (ch == 43) {
            appendLine("VOLUME III")
            appendLine()
          }
          val volRelativeNum = when {
            ch <= 23 -> ch
            ch <= 42 -> ch - 23
            else -> ch - 42
          }
          appendLine("[Illustration: Caption for chapter $ch]")
          appendLine()
          appendLine("CHAPTER $volRelativeNum.")
          appendLine()
          appendLine(ch1Text)
          appendLine()
          appendLine(ch2Text)
        }
        appendLine()
      }
      appendLine("*** END OF THE PROJECT GUTENBERG EBOOK PRIDE AND PREJUDICE ***")
    }

    val cleanedBody = GutendexAndLibriVoxService.stripGutenbergHeaderAndFooter(full61GutenbergSample)
    val parsed61 = GutendexAndLibriVoxService.parseAuthenticChapters(
      bookBody = cleanedBody,
      bookTitle = "Pride and Prejudice",
      gutendexId = 1342
    )
    assertEquals("Must extract all 61 chapters without mid-chapter splits or TOC duplicates", 61, parsed61.size)

    val aligned61 = GutendexAndLibriVoxService.alignChaptersWithLibriVox(
      rawChapters = parsed61,
      sections = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(1342).first().sections,
      bookTitle = "Pride and Prejudice"
    )
    assertEquals(61, aligned61.size)
    for (i in 0 until 61) {
      assertEquals(i + 1, aligned61[i].chapterNumber)
      val expectedTrack = "prideandprejudice_${(i + 1).toString().padStart(2, '0')}_austen_64kb.mp3"
      assertTrue(
        "Chapter ${i + 1} must align to $expectedTrack, got ${aligned61[i].librivoxAudioUrl}",
        aligned61[i].librivoxAudioUrl.orEmpty().contains(expectedTrack)
      )
    }

    val validationReport = GutendexAndLibriVoxService.validatePrideAndPrejudiceAll61Chapters(aligned61)
    assertTrue(
      "All 61 chapters of Pride and Prejudice must validate as complete: $validationReport",
      validationReport.isComplete
    )
    assertEquals(61, validationReport.chapterCount)
    assertTrue(validationReport.stubChapters.isEmpty())
    assertTrue(validationReport.shortOrTruncatedChapters.isEmpty())
    assertTrue(validationReport.totalWords >= 90_000)
  }

  @Test
  fun testUnverifiedDurationsAreHiddenWhileVerifiedDurationsFormatAccurately() {
    val unverifiedChapter = FullBookChapter(
      id = "ch_unverified",
      chapterIndex = 0,
      chapterNumber = 1,
      title = "Chapter 1",
      fullText = "Original paragraph text.",
      librivoxDurationSeconds = 680,
      isAudioDurationVerified = false
    )
    assertEquals("", unverifiedChapter.formattedDuration)

    val unverifiedSection = LibriVoxSection(
      id = "sec_unverified",
      sectionNumber = 1,
      title = "Chapter 1",
      listenUrl = "https://example.org/ch1.mp3",
      durationSeconds = 920,
      isDurationVerified = false
    )
    assertEquals("", unverifiedSection.formattedDuration)

    val verifiedChapter = FullBookChapter(
      id = "ch_verified",
      chapterIndex = 1,
      chapterNumber = 2,
      title = "Chapter 2",
      fullText = "Original paragraph text.",
      librivoxDurationSeconds = 469,
      isAudioDurationVerified = true
    )
    assertEquals("7:49", verifiedChapter.formattedDuration)

    val scienceEditions = GutendexAndLibriVoxService.getScienceOfGettingRichLibriVoxEditions()
    assertTrue(scienceEditions.first().sections.all { it.isDurationVerified && it.formattedDuration.isNotBlank() })
    val pridePreverified = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(1342)
    assertTrue(pridePreverified.first().sections.none { it.formattedDuration.contains("~") })
  }

  @Test
  fun testStrictChapterMp3UrlAlignmentNeverFallsBackToChapterOne() {
    val sections = listOf(
      LibriVoxSection(
        id = "s0",
        sectionNumber = 0,
        title = "Preface",
        listenUrl = "https://archive.org/download/test/book_00.mp3",
        durationSeconds = 120,
        isDurationVerified = true
      ),
      LibriVoxSection(
        id = "s1",
        sectionNumber = 1,
        title = "Chapter 1: First Steps",
        listenUrl = "https://archive.org/download/test/book_01.mp3",
        durationSeconds = 300,
        isDurationVerified = true
      ),
      LibriVoxSection(
        id = "s2",
        sectionNumber = 2,
        title = "Chapter 2: Deeper Into Town",
        listenUrl = "https://archive.org/download/test/book_02.mp3",
        durationSeconds = 350,
        isDurationVerified = true
      )
    )
    val testBook = AudiobookBook(
      id = "strict_align_test",
      title = "Test Novel",
      author = "Test Author",
      primaryCategory = "Classic Literature",
      subCategory = "Novels",
      shortDescription = "Test",
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      librivoxEditions = listOf(
        LibriVoxEdition(
          id = "ed_1",
          title = "Test Novel",
          authorName = "Test Author",
          narratorName = "Human Narrator",
          sections = sections
        )
      ),
      parsedChapters = listOf(
        FullBookChapter(
          id = "c0",
          chapterIndex = 0,
          chapterNumber = 0,
          title = "Preface",
          fullText = "Preface content",
          sectionType = "PREFACE"
        ),
        FullBookChapter(
          id = "c1",
          chapterIndex = 1,
          chapterNumber = 1,
          title = "Chapter 1: First Steps",
          fullText = "Chapter one content",
          sectionType = "CHAPTER"
        ),
        FullBookChapter(
          id = "c2",
          chapterIndex = 2,
          chapterNumber = 2,
          title = "Chapter 2: Deeper Into Town",
          fullText = "Chapter two content",
          sectionType = "CHAPTER"
        ),
        FullBookChapter(
          id = "c3",
          chapterIndex = 3,
          chapterNumber = 5,
          title = "Chapter 5: Unrecorded Missing Chapter",
          fullText = "Chapter five content",
          sectionType = "CHAPTER"
        )
      )
    )

    assertEquals("https://archive.org/download/test/book_00.mp3", AudiobookMediaController.getChapterMp3Url(testBook, 0))
    assertEquals("https://archive.org/download/test/book_01.mp3", AudiobookMediaController.getChapterMp3Url(testBook, 1))
    assertEquals("https://archive.org/download/test/book_02.mp3", AudiobookMediaController.getChapterMp3Url(testBook, 2))
    assertNull("Missing chapter must return null rather than Chapter 1", AudiobookMediaController.getChapterMp3Url(testBook, 3))
  }

  @Test
  fun testPriorityChapterLoadingEmitsImmediatelyAndRejectsStubText() = runBlocking {
    val stubChapter = FullBookChapter(
      id = "stub_1",
      chapterIndex = 0,
      chapterNumber = 1,
      title = "Chapter 1",
      fullText = "Listen to the complete audio chapter stream. Tap Play below to hear it.",
      wordCount = 12
    )
    assertTrue(GutendexAndLibriVoxService.isStubOrSummaryChapter(stubChapter))

    val wattles = findCatalogBook("wattles_science_getting_rich")
    assertFalse(GutendexAndLibriVoxService.isStubOrSummaryChapter(wattles.parsedChapters.first()))

    var callbackTriggered = false
    val result = GutendexAndLibriVoxService.loadBookWithPriorityChapter(
      context = context,
      book = wattles,
      priorityChapterIndex = 2
    ) { ready ->
      if (ready.chapters.isNotEmpty()) {
        callbackTriggered = true
      }
    }
    assertTrue("Priority chapter callback must fire promptly", callbackTriggered)
    assertTrue(result.chapters.size >= 17)
    assertNull(result.errorMessage)
  }

  @Test
  fun testAllAiTutorVoicesProduceDistinctAcousticProfiles() {
    val profileSummaries = mutableSetOf<String>()
    val tupleSignatures = mutableSetOf<String>()
    for (voice in TutorVoice.entries) {
      val pitch = speechManager.computeEffectivePitchForVoice(voice)
      if (voice.gender.equals("Male", ignoreCase = true)) {
        assertTrue("Male voice ${voice.id} pitch $pitch too high", pitch <= 0.91f)
      } else {
        assertTrue("Female voice ${voice.id} pitch $pitch too low", pitch >= 0.94f)
      }
      val summary = speechManager.resolveDistinctVoiceProfileSummary(voice, "en")
      assertTrue("Duplicate voice profile summary for ${voice.id}: $summary", profileSummaries.add(summary))

      val acousticSig = "${voice.gender}|${voice.preferredRegion}|$pitch|${voice.speed}"
      assertTrue("Duplicate acoustic tuple for ${voice.id}: $acousticSig", tupleSignatures.add(acousticSig))
    }
    assertEquals(TutorVoice.entries.size, profileSummaries.size)
  }

  @Test
  fun testBookmarkPersistenceRoundtripAddRetrieveAndRemove() {
    val uid = "bookmark_test_user"
    val bm1 = AudiobookBookmark(
      id = "bm_1",
      bookId = "wattles_science_getting_rich",
      bookTitle = "The Science of Getting Rich",
      chapterIndex = 1,
      chapterTitle = "Chapter 1",
      sentenceIndex = 4,
      positionMs = 18500L,
      snippetText = "There is a science of getting rich."
    )
    val bm2 = AudiobookBookmark(
      id = "bm_2",
      bookId = "wattles_science_getting_rich",
      bookTitle = "The Science of Getting Rich",
      chapterIndex = 3,
      chapterTitle = "Chapter 3",
      sentenceIndex = 9,
      positionMs = 42000L,
      snippetText = "No man is kept poor because opportunity has been taken away."
    )
    AudiobookLearningService.addBookmark(context, uid, bm1)
    AudiobookLearningService.addBookmark(context, uid, bm2)

    val loaded = AudiobookLearningService.getBookmarksForBook(context, uid, "wattles_science_getting_rich")
    assertEquals(2, loaded.size)
    assertEquals("bm_2", loaded[0].id)
    assertEquals(3, loaded[0].chapterIndex)
    assertEquals(42000L, loaded[0].positionMs)

    AudiobookLearningService.removeBookmark(context, uid, "bm_2")
    val afterRemove = AudiobookLearningService.getBookmarksForBook(context, uid, "wattles_science_getting_rich")
    assertEquals(1, afterRemove.size)
    assertEquals("bm_1", afterRemove[0].id)
    assertEquals(18500L, afterRemove[0].positionMs)
  }
}
