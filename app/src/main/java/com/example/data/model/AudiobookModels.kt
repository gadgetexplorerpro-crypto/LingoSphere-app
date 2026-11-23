package com.example.data.model

/**
 * Represents the licensing and delivery classification of a book in LingoSphere.
 * Never falsely labels a summary as a complete book.
 */
enum class BookContentType(val label: String, val badgeColorHex: Long) {
  FULL_PUBLIC_DOMAIN_BOOK("Full Book Text", 0xFF10B981), // Emerald
  LIBRIVOX_HUMAN_AUDIOBOOK("Human-Narrated Audiobook", 0xFFF59E0B), // Amber
  AI_LEARNING_SUMMARY("Educational Summary", 0xFF06B6D4), // Cyan
  FULL_AUDIOBOOK("Full Book & Audio", 0xFF10B981) // Legacy alias
}

/**
 * Author metadata from Gutendex API.
 */
data class GutendexAuthor(
  val name: String,
  val birthYear: Int? = null,
  val deathYear: Int? = null
) {
  val formattedName: String
    get() {
      return if (name.contains(",")) {
        val parts = name.split(",", limit = 2).map { it.trim() }
        if (parts.size == 2 && parts[1].isNotBlank()) "${parts[1]} ${parts[0]}" else name
      } else {
        name
      }
    }
}

/**
 * Reading theme options for the Ebook Reader.
 */
enum class EbookReadingTheme(
  val id: String,
  val label: String,
  val backgroundHex: Long,
  val surfaceHex: Long,
  val textPrimaryHex: Long,
  val textSecondaryHex: Long,
  val accentHex: Long,
  val highlightBgHex: Long,
  val borderHex: Long
) {
  LIGHT(
    id = "LIGHT",
    label = "Light",
    backgroundHex = 0xFFFAF9F5,
    surfaceHex = 0xFFF1EFE8,
    textPrimaryHex = 0xFF1C1917,
    textSecondaryHex = 0xFF57534E,
    accentHex = 0xFF0284C7,
    highlightBgHex = 0xFFE0F2FE,
    borderHex = 0xFFE7E5E4
  ),
  SEPIA(
    id = "SEPIA",
    label = "Sepia",
    backgroundHex = 0xFFF5ECD9,
    surfaceHex = 0xFFEBE0C8,
    textPrimaryHex = 0xFF2C221E,
    textSecondaryHex = 0xFF6B574B,
    accentHex = 0xFFB45309,
    highlightBgHex = 0xFFFDE68A,
    borderHex = 0xFFDCCBB1
  ),
  DARK(
    id = "DARK",
    label = "Dark",
    backgroundHex = 0xFF0B1120,
    surfaceHex = 0xFF131C31,
    textPrimaryHex = 0xFFF1F5F9,
    textSecondaryHex = 0xFF94A3B8,
    accentHex = 0xFF38BDF8,
    highlightBgHex = 0xFF0C2D48,
    borderHex = 0xFF1E293B
  );

  companion object {
    fun fromId(id: String): EbookReadingTheme =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DARK
  }
}

/**
 * Reading layout modes: continuous vertical scrolling or paginated page-by-page turning.
 */
enum class EbookLayoutMode(val id: String, val label: String) {
  SCROLL("SCROLL", "Scroll"),
  PAGINATED("PAGINATED", "Pages")
}

/**
 * Verified start and end timestamps for actual book/chapter content within a LibriVox audio track,
 * used to skip introductory LibriVox recording disclaimers and closing "End of Chapter" credits
 * without ever using a fixed constant offset or skipping story content, chapter titles, or meaningful introductions.
 */
data class VerifiedTrackAnnouncementBoundaries(
  val trackUrl: String,
  val contentStartMs: Long = 0L,
  val contentEndMs: Long? = null,
  val isVerified: Boolean = false,
  val detectionMethod: String = "UNVERIFIED", // "VERIFIED_CATALOG_WAVEFORM", "ACOUSTIC_SILENCE_CADENCE", "UNVERIFIED"
  val introAnnouncementSummary: String = "",
  val outroAnnouncementSummary: String = ""
)

/**
 * Represents an actual parsed chapter or navigable reading section from a complete book.
 * Preserves 100% of the original wording without shortening or inventing chapters.
 */
data class FullBookChapter(
  val id: String,
  val chapterIndex: Int,
  val chapterNumber: Int,
  val title: String,
  val subtitle: String = "",
  val fullText: String,
  val paragraphs: List<String> = emptyList(),
  val sentences: List<String> = emptyList(),
  val wordCount: Int = 0,
  val estimatedMinutes: Int = 5,
  val librivoxAudioUrl: String? = null,
  val librivoxDurationSeconds: Int = 0,
  val isAudioDurationVerified: Boolean = false,
  val librivoxReaderName: String = "",
  val sectionType: String = "CHAPTER", // FRONT_MATTER, PREFACE, INTRODUCTION, PROLOGUE, LETTER, PART, CHAPTER, EPILOGUE, APPENDIX
  val verifiedContentStartMs: Long = 0L,
  val verifiedContentEndMs: Long? = null,
  val hasVerifiedAnnouncementBoundaries: Boolean = false
) {
  val formattedDuration: String
    get() {
      if (isAudioDurationVerified && librivoxDurationSeconds > 0) {
        val hrs = librivoxDurationSeconds / 3600
        val mins = (librivoxDurationSeconds % 3600) / 60
        val secs = librivoxDurationSeconds % 60
        return if (hrs > 0) {
          String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
          String.format("%d:%02d", mins, secs)
        }
      }
      return ""
    }
}

/**
 * Represents a human-narrated chapter section.
 */
data class LibriVoxSection(
  val id: String,
  val sectionNumber: Int,
  val title: String,
  val listenUrl: String,
  val durationSeconds: Int = 0,
  val isDurationVerified: Boolean = false,
  val readerName: String = "Human Narrator",
  val verifiedContentStartMs: Long = 0L,
  val verifiedContentEndMs: Long? = null,
  val hasVerifiedAnnouncementBoundaries: Boolean = false
) {
  val formattedDuration: String
    get() {
      if (!isDurationVerified || durationSeconds <= 0) return ""
      val hrs = durationSeconds / 3600
      val mins = (durationSeconds % 3600) / 60
      val secs = durationSeconds % 60
      return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
      } else {
        String.format("%d:%02d", mins, secs)
      }
    }
}

/**
 * Represents a human-narrated audiobook edition from LibriVox.
 */
data class LibriVoxEdition(
  val id: String,
  val title: String,
  val authorName: String,
  val narratorName: String,
  val totalTime: String = "",
  val language: String = "English",
  val archiveUrl: String = "",
  val sections: List<LibriVoxSection> = emptyList()
)

/**
 * Structured educational episode or chapter for AI Learning Summaries & Custom Lessons.
 */
data class AudiobookEpisode(
  val id: String,
  val episodeNumber: Int,
  val title: String,
  val subtitle: String,
  val estimatedMinutes: Int,
  val narrationLines: List<ListeningDialogueLine> = emptyList(),
  val comprehensionQuestions: List<AcademyComprehensionQuestion> = emptyList(),
  val speakingPracticePrompt: String = "",
  val practicalApplicationTip: String = "",
  val audioStreamUrl: String? = null,
  val narratorName: String = "",
  val durationSeconds: Int = 0
)

/**
 * Gemini AI Smart Book Learning Pack generated on-demand for a specific book chapter or paragraph.
 */
data class ChapterAiStudyPack(
  val bookId: String,
  val chapterIndex: Int,
  val chapterTitle: String,
  val summaryExplanation: String,
  val difficultWords: List<ListeningDialogueLine> = emptyList(),
  val grammarNotes: List<String> = emptyList(),
  val comprehensionQuestions: List<AcademyComprehensionQuestion> = emptyList(),
  val speakingPrompt: String = "",
  val conversationStarter: String = ""
)

/**
 * User-saved bookmark inside a book chapter.
 */
data class AudiobookBookmark(
  val id: String,
  val bookId: String,
  val bookTitle: String,
  val chapterIndex: Int,
  val chapterTitle: String,
  val sentenceIndex: Int,
  val positionMs: Long = 0L,
  val snippetText: String,
  val note: String = "",
  val createdAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Top-level book model for LingoSphere's Complete Book Library & Smart Audiobook System.
 * Supports both complete Public Domain books (Gutendex + Gutenberg + LibriVox)
 * and clearly-labeled Educational Summaries for modern copyrighted titles.
 */
data class AudiobookBook(
  val id: String,
  val gutendexId: Int? = null,
  val title: String,
  val author: String,
  val authorsList: List<GutendexAuthor> = emptyList(),
  val primaryCategory: String,
  val subCategory: String,
  val coverArtUrl: String = "",
  val coverGradientColors: List<Long> = listOf(0xFF0F172A, 0xFF1E293B),
  val shortDescription: String,
  val subjects: List<String> = emptyList(),
  val bookshelves: List<String> = emptyList(),
  val languages: List<String> = listOf("en"),
  val copyright: Boolean? = false,
  val formats: Map<String, String> = emptyMap(),
  val downloadCount: Int = 0,
  val fullTextUrl: String? = null,
  val hasFullTextAvailable: Boolean = false,
  val parsedChapters: List<FullBookChapter> = emptyList(),
  val librivoxEditions: List<LibriVoxEdition> = emptyList(),
  val keyTakeaways: List<String> = emptyList(),
  val contentType: BookContentType = BookContentType.AI_LEARNING_SUMMARY,
  val targetLanguageCode: String = "en",
  val defaultCefrLevel: String = "B1",
  val estimatedTotalMinutes: Int = 18,
  val episodes: List<AudiobookEpisode> = emptyList(),
  val publicationYear: String = "",
  val isFeatured: Boolean = false,
  val isPopular: Boolean = false,
  val isCommunityFavorite: Boolean = false
) {
  val isFullBookEligible: Boolean
    get() = contentType == BookContentType.FULL_PUBLIC_DOMAIN_BOOK ||
      contentType == BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK ||
      contentType == BookContentType.FULL_AUDIOBOOK ||
      hasFullTextAvailable ||
      gutendexId != null

  val hasHumanNarration: Boolean
    get() = hasVerifiedHumanAudio ||
      librivoxEditions.isNotEmpty() ||
      contentType == BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK

  val hasVerifiedHumanAudio: Boolean
    get() = librivoxEditions.any { ed -> ed.sections.any { it.listenUrl.startsWith("http", ignoreCase = true) } } ||
      parsedChapters.any { !it.librivoxAudioUrl.isNullOrBlank() && it.librivoxAudioUrl.startsWith("http", ignoreCase = true) } ||
      episodes.any { !it.audioStreamUrl.isNullOrBlank() && it.audioStreamUrl.startsWith("http", ignoreCase = true) }

  val hasVerifiedFullText: Boolean
    get() = parsedChapters.any { ch ->
      ch.paragraphs.isNotEmpty() &&
        ch.wordCount >= 30 &&
        !ch.fullText.contains("listen to the complete audio chapter stream", ignoreCase = true) &&
        !ch.fullText.contains("use the audio controls below to listen", ignoreCase = true)
    }

  val isCompleteBookAndAudioVerified: Boolean
    get() = hasVerifiedFullText && hasVerifiedHumanAudio

  val totalChapterCount: Int
    get() = maxOf(
      parsedChapters.size,
      librivoxEditions.maxOfOrNull { it.sections.size } ?: 0,
      episodes.size
    )
}

/**
 * Saved user progress, reading position, listening position, and library state for a book.
 */
data class UserAudiobookProgress(
  val bookId: String,
  val lastEpisodeIndex: Int = 0,
  val lastSentenceIndex: Int = 0,
  val lastPlaybackPositionMs: Long = 0L,
  val readingChapterIndex: Int = 0,
  val readingScrollOffset: Int = 0,
  val readingPageIndex: Int = 0,
  val readingPercent: Float = 0f,
  val listeningPercent: Float = 0f,
  val isCompleted: Boolean = false,
  val isFavorite: Boolean = false,
  val preferredEditionId: String = "auto", // "auto", "librivox", "tts"
  val autoPlayNextChapter: Boolean = true,
  val skipRecordingAnnouncements: Boolean = true,
  val readingTheme: String = "DARK", // "LIGHT", "SEPIA", "DARK"
  val readingFontSizeSp: Float = 18f,
  val isPageMode: Boolean = false,
  val completedChapterIndices: Set<Int> = emptySet(),
  val completedReadingChapterIndices: Set<Int> = emptySet(),
  val chapterProgressMap: Map<Int, Float> = emptyMap(),
  val chapterAudioPositionMsMap: Map<Int, Long> = emptyMap(),
  val chapterSentenceIndexMap: Map<Int, Int> = emptyMap(),
  val completedExercisesCount: Int = 0,
  val lastReadAtTimestamp: Long = 0L,
  val lastListenedAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Category hierarchy definition supporting top-level horizontal filters and expandable subcategories.
 */
data class BookCategoryCluster(
  val id: String,
  val title: String,
  val shortChipLabel: String = title,
  val gutendexTopicQuery: String = "",
  val iconEmoji: String,
  val accentColorHex: Long,
  val subCategories: List<String>
)
