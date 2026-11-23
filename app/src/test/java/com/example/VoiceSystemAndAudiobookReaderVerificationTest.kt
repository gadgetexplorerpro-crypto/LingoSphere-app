package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AudiobookBook
import com.example.data.model.BookContentType
import com.example.data.model.FullBookChapter
import com.example.data.model.Language
import com.example.data.model.LibriVoxEdition
import com.example.data.model.LibriVoxSection
import com.example.data.model.TutorVoice
import com.example.speech.AudiobookMediaController
import com.example.speech.SpeechManager
import com.example.ui.components.AudiobookPlayerMode
import com.example.ui.components.AudiobookPlayerScreen
import com.example.ui.components.BookDetailScreen
import com.example.ui.components.SharedVoiceSelectorBottomSheet
import com.example.ui.theme.LingoSphereTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-mdpi")
class VoiceSystemAndAudiobookReaderVerificationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun catalogHas15UniqueProviderVoices_uniquePersonas_andConsistentPreviewSentence() {
    val voices = TutorVoice.entries
    assertEquals(15, voices.size)

    val uniqueProviderIds = voices.map { it.providerVoiceId }.toSet()
    assertEquals("Every voice must map 1-to-1 to a distinct provider voice ID", 15, uniqueProviderIds.size)

    val uniquePersonaNames = voices.map { it.personaName }.toSet()
    assertEquals("Every voice must have a unique persona name", 15, uniquePersonaNames.size)

    val femaleCount = voices.count { it.gender.equals("Female", ignoreCase = true) }
    val maleCount = voices.count { it.gender.equals("Male", ignoreCase = true) }
    assertTrue("Must include at least 6 Female voices", femaleCount >= 6)
    assertTrue("Must include at least 6 Male voices", maleCount >= 6)

    // Same preview sentence for any two voices in a given language
    val enSentence = TutorVoice.consistentPreviewSentence("en")
    val esSentence = TutorVoice.consistentPreviewSentence("es")
    assertTrue(enSentence.isNotBlank())
    assertTrue(esSentence.isNotBlank())
    assertNotEquals(enSentence, esSentence)
  }

  @Test
  fun partnerAndAudiobookVoices_persistSeparately_andRestoreOnRestart() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("lingosphere_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    val manager1 = SpeechManager(context)
    manager1.setPartnerVoice(TutorVoice.CHARON)
    manager1.setAudiobookVoice(TutorVoice.SULAFAT)

    assertEquals(TutorVoice.CHARON, manager1.partnerVoice.value)
    assertEquals(TutorVoice.SULAFAT, manager1.audiobookVoice.value)

    // speakWithTutorVoice with persistAsPartnerPreference = false (used by audiobooks) must NOT overwrite partner voice
    manager1.speakWithTutorVoice(
      text = "Chapter two begins on a quiet morning.",
      voice = TutorVoice.FENRIR,
      languageCode = "en",
      persistAsPartnerPreference = false
    )
    assertEquals(TutorVoice.CHARON, manager1.partnerVoice.value)
    manager1.release()

    // Recreate SpeechManager (simulates cold app restart)
    val manager2 = SpeechManager(context)
    assertEquals(TutorVoice.CHARON, manager2.partnerVoice.value)
    assertEquals(TutorVoice.SULAFAT, manager2.audiobookVoice.value)
    manager2.release()
  }

  @Test
  fun cacheKeyIsolatesVoicesLanguagesPitchSpeedText_andInvalidationCleansFiles() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = SpeechManager(context)

    val k1 = manager.buildTtsCacheKey(TutorVoice.AOEDE, "en", "Hello world")
    val k2 = manager.buildTtsCacheKey(TutorVoice.CHARON, "en", "Hello world")
    val k3 = manager.buildTtsCacheKey(TutorVoice.AOEDE, "es", "Hello world")
    val k4 = manager.buildTtsCacheKey(TutorVoice.AOEDE, "en", "Hello world", speed = 1.25f)
    val k5 = manager.buildTtsCacheKey(TutorVoice.AOEDE, "en", "Different text")

    assertEquals(5, setOf(k1, k2, k3, k4, k5).size)

    val cacheDir = java.io.File(context.cacheDir, "tts_voice_cache").apply { mkdirs() }
    val aoedeFile = java.io.File(cacheDir, "tts_v_aoede_sample.wav").apply { writeText("test") }
    val charonFile = java.io.File(cacheDir, "tts_v_charon_sample.wav").apply { writeText("test") }
    assertTrue(aoedeFile.exists() && charonFile.exists())

    manager.invalidateVoiceAudioCache(TutorVoice.AOEDE.id)
    assertTrue(!aoedeFile.exists())
    assertTrue(charonFile.exists())

    manager.invalidateVoiceAudioCache(null)
    assertTrue(!charonFile.exists())
    manager.release()
  }

  @Test
  fun sharedVoiceSelectorBottomSheet_filtersGender_previewsSameSentence_andAppliesChoice() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val enLang = Language.findByCode("en")
    var appliedVoice: TutorVoice? = null

    composeTestRule.setContent {
      LingoSphereTheme {
        SharedVoiceSelectorBottomSheet(
          selectedVoice = TutorVoice.AOEDE,
          targetLang = enLang,
          speechManager = speechManager,
          onApplyVoice = { appliedVoice = it },
          onDismiss = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("consistent_preview_phrase_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_chip_${TutorVoice.AOEDE.id}").assertExists()

    // Filter to Male voices: Female voice Aoede disappears, Male voice Charon appears
    composeTestRule.onNodeWithTag("voice_filter_male").performClick()
    composeTestRule.waitForIdle()
    assertTrue(composeTestRule.onAllNodesWithTag("voice_chip_${TutorVoice.AOEDE.id}").fetchSemanticsNodes().isEmpty())
    composeTestRule.onNodeWithTag("voice_selector_list")
      .performScrollToNode(hasTestTag("voice_chip_${TutorVoice.CHARON.id}"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("voice_chip_${TutorVoice.CHARON.id}").assertExists()

    // Preview button & Apply button work cleanly
    composeTestRule.onNodeWithTag("voice_preview_btn_${TutorVoice.CHARON.id}").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("apply_voice_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(TutorVoice.CHARON, appliedVoice)
    speechManager.release()
  }

  @Test
  fun bookDetailScreen_collapsesCredits_andConnectsAiVoiceSelectorAndChapters() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val enLang = Language.findByCode("en")

    val book = AudiobookBook(
      id = "test_book_credits_collapse",
      title = "Pride and Prejudice",
      author = "Jane Austen",
      primaryCategory = "Classic Novels",
      subCategory = "Romance",
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      shortDescription = "A classic novel of manners by Jane Austen.",
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      hasFullTextAvailable = true,
      librivoxEditions = listOf(
        LibriVoxEdition(
          id = "ed1",
          title = "Pride and Prejudice (Version 1)",
          authorName = "Jane Austen",
          narratorName = "Karen Savage",
          totalTime = "11:35:00",
          sections = listOf(
            LibriVoxSection("s1", 1, "Chapter 1", "https://archive.org/ch1.mp3", 420, isDurationVerified = true, readerName = "Karen Savage"),
            LibriVoxSection("s2", 2, "Chapter 2", "https://archive.org/ch2.mp3", 480, isDurationVerified = true, readerName = "Karen Savage")
          )
        )
      ),
      parsedChapters = listOf(
        FullBookChapter(
          id = "c1", chapterIndex = 0, chapterNumber = 1, title = "Chapter 1",
          fullText = "It is a truth universally acknowledged.",
          paragraphs = listOf("It is a truth universally acknowledged."),
          sentences = listOf("It is a truth universally acknowledged."),
          librivoxAudioUrl = "https://archive.org/ch1.mp3"
        ),
        FullBookChapter(
          id = "c2", chapterIndex = 1, chapterNumber = 2, title = "Chapter 2",
          fullText = "Mr. Bennet was among the earliest.",
          paragraphs = listOf("Mr. Bennet was among the earliest."),
          sentences = listOf("Mr. Bennet was among the earliest."),
          librivoxAudioUrl = "https://archive.org/ch2.mp3"
        )
      )
    )

    var playedChapter = -1
    composeTestRule.setContent {
      LingoSphereTheme {
        BookDetailScreen(
          book = book,
          userId = "u_detail_test",
          targetLang = enLang,
          speechManager = speechManager,
          onPlayEpisode = { playedChapter = it },
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Credits & Book Info section is removed for clean Book Details layout
    assertTrue(composeTestRule.onAllNodesWithTag("book_detail_credits_toggle").fetchSemanticsNodes().isEmpty())
    assertTrue(composeTestRule.onAllNodesWithTag("book_detail_attribution_card").fetchSemanticsNodes().isEmpty())

    // AI Voice Selector button opens SharedVoiceSelectorBottomSheet & updates AudiobookMediaController
    composeTestRule.onNodeWithTag("book_detail_screen")
      .performScrollToNode(hasTestTag("book_detail_ai_voice_selector_btn"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("book_detail_ai_voice_selector_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("shared_voice_selector_sheet").assertExists()
    composeTestRule.onNodeWithTag("voice_selector_list")
      .performScrollToNode(hasTestTag("voice_chip_${TutorVoice.ORUS.id}"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("voice_chip_${TutorVoice.ORUS.id}").performClick()
    composeTestRule.onNodeWithTag("apply_voice_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(TutorVoice.ORUS, AudiobookMediaController.selectedVoice.value)

    // Selecting Chapter 2 opens Chapter 2 cleanly (never resets to Chapter 1)
    composeTestRule.onNodeWithTag("book_detail_screen").performScrollToNode(hasTestTag("book_chapter_item_1"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("book_chapter_item_1").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, playedChapter)
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)

    speechManager.release()
  }

  @Test
  fun audiobookPlayerScreen_opensSharedVoiceSelector_andPreservesChapter2Selection() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val speechManager = SpeechManager(context)
    val enLang = Language.findByCode("en")

    val book = AudiobookBook(
      id = "test_book_player_voice",
      title = "Moby Dick",
      author = "Herman Melville",
      primaryCategory = "Classic Novels",
      subCategory = "Adventure",
      shortDescription = "Herman Melville's epic maritime tale.",
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      hasFullTextAvailable = true,
      parsedChapters = listOf(
        FullBookChapter(
          id = "m1", chapterIndex = 0, chapterNumber = 1, title = "Loomings",
          fullText = "Call me Ishmael.", paragraphs = listOf("Call me Ishmael."),
          sentences = listOf("Call me Ishmael."),
          librivoxAudioUrl = "https://archive.org/moby_01.mp3"
        ),
        FullBookChapter(
          id = "m2", chapterIndex = 1, chapterNumber = 2, title = "The Carpet-Bag",
          fullText = "I stuffed a shirt or two into my old carpet-bag.",
          paragraphs = listOf("I stuffed a shirt or two into my old carpet-bag."),
          sentences = listOf("I stuffed a shirt or two into my old carpet-bag."),
          librivoxAudioUrl = "https://archive.org/moby_02.mp3"
        )
      )
    )

    composeTestRule.setContent {
      LingoSphereTheme {
        AudiobookPlayerScreen(
          book = book,
          initialEpisodeIndex = 1,
          initialPlayerMode = AudiobookPlayerMode.LISTEN_ONLY,
          autoStartPlayback = false,
          targetLang = enLang,
          speechManager = speechManager,
          userId = "u_player_test",
          onSaveVocabToFlashcard = { _, _, _ -> },
          onAddXp = {},
          onOpenSubscription = {},
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)
    assertEquals("https://archive.org/moby_02.mp3", AudiobookMediaController.currentAudioUrl.value)

    // Open unified voice selector bottom sheet from player control bar (visible immediately without scrolling)
    composeTestRule.onNodeWithTag("player_voice_selector_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("shared_voice_selector_sheet").assertExists()
    composeTestRule.onNodeWithTag("voice_option_human_narrator").assertExists()

    composeTestRule.onNodeWithTag("voice_selector_list")
      .performScrollToNode(hasTestTag("voice_chip_${TutorVoice.DESPINA.id}"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("voice_chip_${TutorVoice.DESPINA.id}").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("apply_voice_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(TutorVoice.DESPINA, AudiobookMediaController.selectedVoice.value)
    assertEquals(false, AudiobookMediaController.useHumanNarrator.value)
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)
    composeTestRule.onNodeWithText("AI: Clara (Despina)").assertIsDisplayed()

    speechManager.release()
  }
}
