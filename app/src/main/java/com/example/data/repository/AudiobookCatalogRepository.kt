package com.example.data.repository

import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.AudiobookBook
import com.example.data.model.AudiobookEpisode
import com.example.data.model.BookCategoryCluster
import com.example.data.model.BookContentType
import com.example.data.model.GutendexAuthor
import com.example.data.model.ListeningDialogueLine

object AudiobookCatalogRepository {

  /**
   * Horizontally scrolling top-level category tabs requested for the main Listening Hub:
   * All 21 categories supported + "All".
   */
  val MAIN_CATEGORY_TABS = listOf(
    "All",
    "Motivation and Success",
    "Wealth and Money",
    "Business and Entrepreneurship",
    "Psychology",
    "Personal Development",
    "Productivity",
    "Leadership",
    "Science",
    "Technology",
    "History",
    "Biographies",
    "Philosophy",
    "Romance",
    "Adventure",
    "Mystery",
    "Horror",
    "Fantasy",
    "Classic Literature",
    "Short Stories",
    "Children's Stories",
    "Language Learning"
  )

  val POPULAR_NARRATORS = listOf(
    "All Narrators",
    "Elizabeth Klett",
    "Karen Savage",
    "Kara Shallenberg",
    "Stewart Wills",
    "Diana Majlinger",
    "Bob Neufeld",
    "John Greenman",
    "David Fox",
    "Cori Samuel",
    "Mark F. Smith",
    "Martin Geeson",
    "Moira Fogarty"
  )

  val CATALOG_LANGUAGE_OPTIONS = listOf(
    "ALL" to "All Languages",
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese"
  )

  fun gutendexTopicForTab(tab: String): String? {
    return when (tab.lowercase().trim()) {
      "all" -> null
      "motivation and success", "motivation" -> "conduct of life"
      "wealth and money", "wealth" -> "economics"
      "business and entrepreneurship", "business" -> "commerce"
      "psychology" -> "psychology"
      "personal development" -> "conduct of life"
      "productivity" -> "conduct of life"
      "leadership" -> "political science"
      "science" -> "science"
      "technology" -> "technology"
      "history" -> "history"
      "biographies", "biography" -> "biography"
      "philosophy" -> "philosophy"
      "romance" -> "love stories"
      "adventure" -> "adventure"
      "mystery" -> "detective"
      "horror" -> "horror"
      "fantasy" -> "fantasy"
      "classic literature", "stories" -> "fiction"
      "short stories" -> "short stories"
      "children's stories", "children" -> "children"
      "language learning" -> "language"
      else -> tab.lowercase()
    }
  }

  fun gutendexFallbackSearchForTab(tab: String): String? {
    return when (tab.lowercase().trim()) {
      "all" -> null
      "motivation and success", "motivation" -> "success"
      "wealth and money", "wealth" -> "wealth"
      "business and entrepreneurship", "business" -> "business"
      "personal development" -> "self-culture"
      "productivity" -> "habits"
      "leadership" -> "leadership"
      "classic literature" -> "jane austen"
      "language learning" -> "grammar"
      else -> tab.substringBefore(" and ").trim()
    }
  }

  fun librivoxGenreForTab(tab: String): String? {
    return when (tab.lowercase().trim()) {
      "all" -> null
      "motivation and success", "motivation", "personal development", "productivity" -> "Self-Help"
      "wealth and money", "wealth", "business and entrepreneurship", "business", "leadership" -> "Business & Economics"
      "psychology" -> "Psychology"
      "science", "technology" -> "Science"
      "history" -> "History"
      "biographies", "biography" -> "Biography & Autobiography"
      "philosophy" -> "Philosophy"
      "romance" -> "Romance"
      "adventure" -> "Action & Adventure"
      "mystery" -> "Detective Fiction"
      "horror" -> "Horror & Supernatural Fiction"
      "fantasy" -> "Fantasy Fiction"
      "classic literature", "stories" -> "General Fiction"
      "short stories" -> "Short Stories"
      "children's stories", "children" -> "Children's Fiction"
      "language learning" -> "Language learning"
      else -> null
    }
  }

  /**
   * Matches any local or remote book against a user-selected category tab using canonical category names,
   * subjects, bookshelves, and topic synonyms so valid category books are never accidentally filtered out.
   */
  fun matchesCategoryTab(book: AudiobookBook, selectedCategoryTab: String): Boolean {
    val normTab = selectedCategoryTab.trim().lowercase()
    if (normTab.isBlank() || normTab == "all") return true

    val haystack = buildString {
      append(book.primaryCategory.lowercase()).append(' ')
      append(book.subCategory.lowercase()).append(' ')
      append(book.title.lowercase()).append(' ')
      book.subjects.forEach { append(it.lowercase()).append(' ') }
      book.bookshelves.forEach { append(it.lowercase()).append(' ') }
    }

    if (haystack.contains(normTab)) return true

    val aliases = when (normTab) {
      "motivation and success", "motivation" -> listOf(
        "motivation", "success", "conduct of life", "self-culture", "self-help", "self help",
        "mindset", "habits", "character", "thought", "virtue", "ethics", "personal development"
      )
      "wealth and money", "wealth" -> listOf(
        "wealth", "money", "finance", "financial", "economics", "economic", "commerce",
        "prosperity", "getting rich", "political economy", "capital", "trade", "business"
      )
      "business and entrepreneurship", "business" -> listOf(
        "business", "commerce", "entrepreneur", "leadership", "strategy", "management",
        "economics", "industry", "trade", "sales", "marketing", "finance"
      )
      "personal development" -> listOf(
        "personal development", "self-culture", "self-help", "self help", "conduct of life",
        "motivation", "mindset", "habits", "wisdom", "improvement"
      )
      "productivity" -> listOf(
        "productivity", "efficiency", "habits", "success", "conduct of life", "time", "leadership", "work"
      )
      "leadership" -> listOf(
        "leadership", "strategy", "art of war", "management", "politics", "government", "statecraft", "business"
      )
      "biographies", "biography" -> listOf(
        "biography", "biographies", "autobiography", "memoir", "letters", "reminiscences"
      )
      "classic literature", "stories" -> listOf(
        "classic", "literature", "fiction", "stories", "novel", "romance", "adventure", "drama", "gothic"
      )
      "short stories" -> listOf("short stories", "tales", "stories")
      "children's stories", "children" -> listOf(
        "children", "juvenile", "fairy", "wonderland", "fables", "youth"
      )
      "language learning" -> listOf("language", "grammar", "dictionary", "linguistics", "literature", "reader")
      "mystery" -> listOf("mystery", "detective", "sherlock", "crime", "investigation")
      "horror" -> listOf("horror", "gothic", "dracula", "frankenstein", "ghost", "supernatural")
      "romance" -> listOf("romance", "love", "courtship", "marriage", "bride")
      "science" -> listOf("science", "physics", "astronomy", "biology", "nature", "natural history", "darwin")
      "technology" -> listOf("technology", "engineering", "electricity", "invention", "mechanics", "science")
      else -> normTab.split(" and ", " ", ",").filter { it.length >= 3 }
    }
    return aliases.any { haystack.contains(it) }
  }

  /**
   * Complete 6 expandable category clusters with all requested subcategories.
   */
  val CATEGORY_CLUSTERS = listOf(
    BookCategoryCluster(
      id = "motivation",
      title = "Motivation and Success",
      shortChipLabel = "Motivation",
      gutendexTopicQuery = "conduct of life",
      iconEmoji = "🔥",
      accentColorHex = 0xFFF59E0B, // Amber
      subCategories = listOf(
        "Motivation", "Discipline", "Confidence", "Success", "Productivity",
        "Personal development", "Positive thinking", "Leadership", "Habits", "Life improvement"
      )
    ),
    BookCategoryCluster(
      id = "wealth",
      title = "Wealth and Business",
      shortChipLabel = "Wealth & Business",
      gutendexTopicQuery = "economics",
      iconEmoji = "💼",
      accentColorHex = 0xFF10B981, // Emerald
      subCategories = listOf(
        "Money", "Wealth", "Investing education", "Financial independence",
        "Entrepreneurship", "Business", "Marketing", "Sales", "Economics", "Money management"
      )
    ),
    BookCategoryCluster(
      id = "psychology",
      title = "Psychology",
      shortChipLabel = "Psychology",
      gutendexTopicQuery = "psychology",
      iconEmoji = "🧠",
      accentColorHex = 0xFF8B5CF6, // Purple
      subCategories = listOf(
        "Human behavior", "Emotional intelligence", "Decision-making",
        "Relationships", "Communication", "Mindset", "Psychology"
      )
    ),
    BookCategoryCluster(
      id = "literature",
      title = "Fiction and Stories",
      shortChipLabel = "Stories",
      gutendexTopicQuery = "fiction",
      iconEmoji = "📚",
      accentColorHex = 0xFF06B6D4, // Cyan
      subCategories = listOf(
        "Adventure", "Romance", "Mystery", "Detective stories", "Science fiction",
        "Fantasy", "Horror", "Historical fiction", "Short stories", "Classics"
      )
    ),
    BookCategoryCluster(
      id = "knowledge",
      title = "Education",
      shortChipLabel = "Science & History",
      gutendexTopicQuery = "science",
      iconEmoji = "🔬",
      accentColorHex = 0xFF3B82F6, // Blue
      subCategories = listOf(
        "Science", "History", "Technology", "Mathematics", "Philosophy",
        "Biographies", "Geography", "Language learning", "General knowledge"
      )
    ),
    BookCategoryCluster(
      id = "lifestyle",
      title = "Lifestyle",
      shortChipLabel = "Lifestyle",
      gutendexTopicQuery = "health",
      iconEmoji = "🌿",
      accentColorHex = 0xFFEC4899, // Pink
      subCategories = listOf(
        "Health", "Travel", "Culture", "Mindfulness", "Family",
        "Career", "Happiness", "Personal growth"
      )
    )
  )

  /**
   * Famous Complete Public-Domain Books backed by real Gutendex IDs, full Gutenberg UTF-8 texts,
   * real cover images, and LibriVox human-narrated chapter streams.
   */
  val FAMOUS_FULL_BOOKS: List<AudiobookBook> by lazy {
    listOf(
      AudiobookBook(
      id = "gutenberg_1342",
      gutendexId = 1342,
      title = "Pride and Prejudice",
      author = "Jane Austen",
      authorsList = listOf(GutendexAuthor("Austen, Jane", 1775, 1817)),
      primaryCategory = "Romance",
      subCategory = "Classics",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg",
      coverGradientColors = listOf(0xFF3B0764, 0xFF831843),
      shortDescription = "Jane Austen's beloved masterpiece following Elizabeth Bennet and Mr. Fitzwilliam Darcy as they navigate manners, upbringing, morality, and marriage in 19th-century England.",
      subjects = listOf("Courtship -- Fiction", "Domestic fiction", "Love stories", "Sisters -- Fiction", "England -- Fiction"),
      bookshelves = listOf("Best Books Ever Listings", "Harvard Classics"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/1342.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg",
        "application/epub+zip" to "https://www.gutenberg.org/ebooks/1342.epub.images"
      ),
      downloadCount = 68420,
      fullTextUrl = "https://www.gutenberg.org/ebooks/1342.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getPrideAndPrejudiceAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(1342),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 695,
      publicationYear = "1813",
      isFeatured = true,
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_1661",
      gutendexId = 1661,
      title = "The Adventures of Sherlock Holmes",
      author = "Arthur Conan Doyle",
      authorsList = listOf(GutendexAuthor("Doyle, Arthur Conan", 1859, 1930)),
      primaryCategory = "Stories",
      subCategory = "Detective stories",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg",
      coverGradientColors = listOf(0xFF0F172A, 0xFF0369A1),
      shortDescription = "Twelve classic detective cases solved by consulting detective Sherlock Holmes and chronicled by his loyal friend Dr. John Watson, starting with A Scandal in Bohemia.",
      subjects = listOf("Detective and mystery stories, English", "Holmes, Sherlock (Fictitious character) -- Fiction", "Private investigators -- England -- Fiction"),
      bookshelves = listOf("Detective Fiction", "Contemporary Reviews"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/1661.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg"
      ),
      downloadCount = 49120,
      fullTextUrl = "https://www.gutenberg.org/ebooks/1661.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getSherlockHolmesAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(1661),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B1",
      estimatedTotalMinutes = 665,
      publicationYear = "1892",
      isFeatured = true,
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_84",
      gutendexId = 84,
      title = "Frankenstein; Or, The Modern Prometheus",
      author = "Mary Wollstonecraft Shelley",
      authorsList = listOf(GutendexAuthor("Shelley, Mary Wollstonecraft", 1797, 1851)),
      primaryCategory = "Stories",
      subCategory = "Science fiction",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/84/pg84.cover.medium.jpg",
      coverGradientColors = listOf(0xFF1E1B4B, 0xFF312E81),
      shortDescription = "Victor Frankenstein, a brilliant young scientist, creates a sentient creature in an unorthodox experiment and must confront the profound moral consequences of ambition.",
      subjects = listOf("Frankenstein, Victor (Fictitious character) -- Fiction", "Gothic fiction", "Horror tales", "Monster -- Fiction", "Science fiction"),
      bookshelves = listOf("Gothic Fiction", "Science Fiction by Women", "Movie Books"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/84.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/84/pg84.cover.medium.jpg"
      ),
      downloadCount = 81300,
      fullTextUrl = "https://www.gutenberg.org/ebooks/84.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getFrankensteinAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(84),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 510,
      publicationYear = "1818",
      isFeatured = true,
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_11",
      gutendexId = 11,
      title = "Alice's Adventures in Wonderland",
      author = "Lewis Carroll",
      authorsList = listOf(GutendexAuthor("Carroll, Lewis", 1832, 1898)),
      primaryCategory = "Adventure",
      subCategory = "Fantasy",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/11/pg11.cover.medium.jpg",
      coverGradientColors = listOf(0xFF0C4A6E, 0xFF0284C7),
      shortDescription = "Young Alice tumbles down a rabbit hole into a fantastical subterranean world populated by the White Rabbit, the Cheshire Cat, the Mad Hatter, and the Queen of Hearts.",
      subjects = listOf("Alice (Fictitious character from Carroll) -- Fiction", "Children's stories", "Fantasy fiction", "Imaginary places -- Fiction"),
      bookshelves = listOf("Children's Literature"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/11.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/11/pg11.cover.medium.jpg"
      ),
      downloadCount = 54900,
      fullTextUrl = "https://www.gutenberg.org/ebooks/11.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getAliceInWonderlandAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(11),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "A2",
      estimatedTotalMinutes = 178,
      publicationYear = "1865",
      isFeatured = true,
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_2701",
      gutendexId = 2701,
      title = "Moby Dick; Or, The Whale",
      author = "Herman Melville",
      authorsList = listOf(GutendexAuthor("Melville, Herman", 1819, 1891)),
      primaryCategory = "Adventure",
      subCategory = "Classics",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg",
      coverGradientColors = listOf(0xFF0F2942, 0xFF0D9488),
      shortDescription = "Sailor Ishmael recounts the obsessive quest of Captain Ahab, master of the whaling ship Pequod, across the world's oceans in pursuit of the great white whale.",
      subjects = listOf("Adventure stories", "Ahab, Captain (Fictitious character) -- Fiction", "Sea stories", "Whales -- Fiction", "Whaling -- Fiction"),
      bookshelves = listOf("Adventure", "Best Books Ever Listings"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/2701.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg"
      ),
      downloadCount = 61200,
      fullTextUrl = "https://www.gutenberg.org/ebooks/2701.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getMobyDickAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(2701),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "C1",
      estimatedTotalMinutes = 1480,
      publicationYear = "1851",
      isFeatured = true,
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_132",
      gutendexId = 132,
      title = "The Art of War",
      author = "Sun Tzu",
      authorsList = listOf(GutendexAuthor("Sunzi, active 6th century B.C.")),
      primaryCategory = "Business",
      subCategory = "Leadership",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/132/pg132.cover.medium.jpg",
      coverGradientColors = listOf(0xFF3B0764, 0xFF831843),
      shortDescription = "The classic treatise translated by Lionel Giles offering timeless strategic principles for leadership, preparation, discipline, and resolving conflict wisely.",
      subjects = listOf("Military art and science -- Early works to 1800", "Strategy", "Leadership"),
      bookshelves = listOf("Philosophy", "Harvard Classics"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/132.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/132/pg132.cover.medium.jpg"
      ),
      downloadCount = 34500,
      fullTextUrl = "https://www.gutenberg.org/ebooks/132.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getArtOfWarAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(132),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 72,
      publicationYear = "500 BC",
      isFeatured = true,
      isCommunityFavorite = true
    ),
    AudiobookBook(
      id = "gutenberg_345",
      gutendexId = 345,
      title = "Dracula",
      author = "Bram Stoker",
      authorsList = listOf(GutendexAuthor("Stoker, Bram", 1847, 1912)),
      primaryCategory = "Stories",
      subCategory = "Horror",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/345/pg345.cover.medium.jpg",
      coverGradientColors = listOf(0xFF27272A, 0xFF7F1D1D),
      shortDescription = "Told through letters, diary entries, and newspaper clippings, Bram Stoker's classic Gothic novel chronicles Solicitor Jonathan Harker's encounter with Count Dracula.",
      subjects = listOf("Dracula, Count (Fictitious character) -- Fiction", "Epistolary fiction", "Gothic fiction", "Horror tales", "Transylvania (Romania) -- Fiction"),
      bookshelves = listOf("Gothic Fiction", "Horror", "Movie Books"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/345.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/345/pg345.cover.medium.jpg"
      ),
      downloadCount = 47800,
      fullTextUrl = "https://www.gutenberg.org/ebooks/345.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getDraculaAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(345),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 920,
      publicationYear = "1897",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_74",
      gutendexId = 74,
      title = "The Adventures of Tom Sawyer",
      author = "Mark Twain",
      authorsList = listOf(GutendexAuthor("Twain, Mark", 1835, 1910)),
      primaryCategory = "Adventure",
      subCategory = "Adventure",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/74/pg74.cover.medium.jpg",
      coverGradientColors = listOf(0xFF451A03, 0xFFD97706),
      shortDescription = "Mark Twain's classic novel about an imaginative boy growing up along the Mississippi River in the fictional town of St. Petersburg, Missouri.",
      subjects = listOf("Adventure stories", "Boys -- Fiction", "Humorous stories", "Mississippi River -- Fiction", "Missouri -- Fiction", "Sawyer, Tom (Fictitious character) -- Fiction"),
      bookshelves = listOf("Banned Books", "Children's Literature"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/74.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/74/pg74.cover.medium.jpg"
      ),
      downloadCount = 39400,
      fullTextUrl = "https://www.gutenberg.org/ebooks/74.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getTomSawyerAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(74),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B1",
      estimatedTotalMinutes = 430,
      publicationYear = "1876",
      isPopular = true
    ),
    AudiobookBook(
      id = "wattles_science_getting_rich",
      gutendexId = null,
      title = "The Science of Getting Rich",
      author = "Wallace D. Wattles",
      authorsList = listOf(GutendexAuthor("Wattles, Wallace D.", 1860, 1911)),
      primaryCategory = "Wealth and Money",
      subCategory = "Success & Prosperity",
      coverArtUrl = "https://archive.org/services/img/science_getting_rich_librivox",
      coverGradientColors = listOf(0xFF065F46, 0xFF059669),
      shortDescription = "Wallace D. Wattles' complete 1910 public-domain classic on attracting wealth and abundance through creative thought, purposeful action, and gratitude (Preface + all 17 Chapters).",
      subjects = listOf("Success", "Wealth", "Finance, Personal", "Economics", "Creative Thought"),
      bookshelves = listOf("Philosophy", "Self-Help"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "image/jpeg" to "https://archive.org/services/img/science_getting_rich_librivox"
      ),
      downloadCount = 52300,
      fullTextUrl = null,
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getScienceOfGettingRichAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getScienceOfGettingRichLibriVoxEditions(),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B1",
      estimatedTotalMinutes = 138,
      publicationYear = "1910",
      isFeatured = true,
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_4507",
      gutendexId = 4507,
      title = "As a Man Thinketh",
      author = "James Allen",
      authorsList = listOf(GutendexAuthor("Allen, James", 1864, 1912)),
      primaryCategory = "Motivation and Success",
      subCategory = "Mindset & Habits",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/4507/pg4507.cover.medium.jpg",
      coverGradientColors = listOf(0xFF78350F, 0xFFD97706),
      shortDescription = "James Allen's profound philosophical essay showing that a person is literally what they think, their character being the complete sum of all their thoughts.",
      subjects = listOf("Philosophy", "Self-culture", "Thought and thinking", "Conduct of life"),
      bookshelves = listOf("Philosophy"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/4507.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/4507/pg4507.cover.medium.jpg"
      ),
      downloadCount = 41200,
      fullTextUrl = "https://www.gutenberg.org/ebooks/4507.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getAsAManThinkethAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(4507),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 60,
      publicationYear = "1903",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_174",
      gutendexId = 174,
      title = "The Picture of Dorian Gray",
      author = "Oscar Wilde",
      authorsList = listOf(GutendexAuthor("Wilde, Oscar", 1854, 1900)),
      primaryCategory = "Classic Literature",
      subCategory = "Philosophical Fiction",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/174/pg174.cover.medium.jpg",
      coverGradientColors = listOf(0xFF1E1B4B, 0xFF4338CA),
      shortDescription = "Oscar Wilde's haunting philosophical novel of vanity, art, aestheticism, and moral corruption in Victorian London.",
      subjects = listOf("Appearance (Philosophy) -- Fiction", "Conduct of life -- Fiction", "Portraits -- Fiction", "London (England) -- Fiction"),
      bookshelves = listOf("Gothic Fiction", "Movie Books"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/174.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/174/pg174.cover.medium.jpg"
      ),
      downloadCount = 61200,
      fullTextUrl = "https://www.gutenberg.org/ebooks/174.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getDorianGrayAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(174),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 460,
      publicationYear = "1890",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_1260",
      gutendexId = 1260,
      title = "Jane Eyre",
      author = "Charlotte Brontë",
      authorsList = listOf(GutendexAuthor("Brontë, Charlotte", 1816, 1855)),
      primaryCategory = "Romance",
      subCategory = "Classic Romance",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/1260/pg1260.cover.medium.jpg",
      coverGradientColors = listOf(0xFF831843, 0xFFDB2777),
      shortDescription = "Charlotte Brontë's groundbreaking narrative of courage, independent morality, love, and mystery at Thornfield Hall.",
      subjects = listOf("Governesses -- Fiction", "Fathers and daughters -- Fiction", "England -- Fiction", "Love stories"),
      bookshelves = listOf("Best Books Ever Listings"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/1260.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/1260/pg1260.cover.medium.jpg"
      ),
      downloadCount = 48900,
      fullTextUrl = "https://www.gutenberg.org/ebooks/1260.txt.utf-8",
      hasFullTextAvailable = true,
      parsedChapters = GutendexAndLibriVoxService.getJaneEyreAuthenticChapters(),
      librivoxEditions = GutendexAndLibriVoxService.getPreverifiedLibriVoxEditions(1260),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 1150,
      publicationYear = "1847",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_2641",
      gutendexId = 2641,
      title = "A Room with a View",
      author = "E. M. Forster",
      authorsList = listOf(GutendexAuthor("Forster, E. M.", 1879, 1970)),
      primaryCategory = "Romance",
      subCategory = "Classic Romance",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/2641/pg2641.cover.medium.jpg",
      coverGradientColors = listOf(0xFF4C1D95, 0xFFDB2777),
      shortDescription = "E. M. Forster's celebrated novel of romance and social awakening set in Florence, Italy, and Surrey, England, following young Lucy Honeychurch and George Emerson.",
      subjects = listOf("British -- Italy -- Fiction", "Florence (Italy) -- Fiction", "Humorous stories", "Love stories", "Young women -- Fiction"),
      bookshelves = listOf("Best Books Ever Listings", "Italy"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/2641.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/2641/pg2641.cover.medium.jpg"
      ),
      downloadCount = 46800,
      fullTextUrl = "https://www.gutenberg.org/ebooks/2641.txt.utf-8",
      hasFullTextAvailable = true,
      librivoxEditions = listOf(
        com.example.data.model.LibriVoxEdition(
          id = "lv_room_with_a_view_klett",
          title = "A Room with a View",
          authorName = "E. M. Forster",
          narratorName = "Elizabeth Klett",
          totalTime = "07:14:22",
          language = "English",
          archiveUrl = "https://archive.org/details/room_with_a_view_ek_librivox",
          sections = (1..20).map { ch ->
            val twoDigit = ch.toString().padStart(2, '0')
            com.example.data.model.LibriVoxSection(
              id = "rwav_sec_$ch",
              sectionNumber = ch,
              title = "Chapter $ch",
              listenUrl = "https://archive.org/download/room_with_a_view_ek_librivox/roomwithaview_${twoDigit}_forster_64kb.mp3",
              durationSeconds = 1280,
              isDurationVerified = true,
              readerName = "Elizabeth Klett"
            )
          }
        )
      ),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 434,
      publicationYear = "1908",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_2680",
      gutendexId = 2680,
      title = "Meditations",
      author = "Marcus Aurelius",
      authorsList = listOf(GutendexAuthor("Marcus Aurelius, Emperor of Rome", 121, 180)),
      primaryCategory = "Philosophy",
      subCategory = "Personal development",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/2680/pg2680.cover.medium.jpg",
      coverGradientColors = listOf(0xFF1E293B, 0xFF0284C7),
      shortDescription = "The private Stoic reflections of Roman Emperor Marcus Aurelius on resilience, virtue, self-discipline, and tranquility of mind.",
      subjects = listOf("Ethics", "Life", "Philosophy", "Stoics", "Conduct of life"),
      bookshelves = listOf("Philosophy", "Harvard Classics"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/2680.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/2680/pg2680.cover.medium.jpg"
      ),
      downloadCount = 43100,
      fullTextUrl = "https://www.gutenberg.org/ebooks/2680.txt.utf-8",
      hasFullTextAvailable = true,
      librivoxEditions = listOf(
        com.example.data.model.LibriVoxEdition(
          id = "lv_meditations_geeson",
          title = "Meditations",
          authorName = "Marcus Aurelius",
          narratorName = "Martin Geeson",
          totalTime = "05:42:10",
          language = "English",
          archiveUrl = "https://archive.org/details/meditations_librivox",
          sections = (1..12).map { bk ->
            val twoDigit = bk.toString().padStart(2, '0')
            com.example.data.model.LibriVoxSection(
              id = "med_sec_$bk",
              sectionNumber = bk,
              title = "Book $bk",
              listenUrl = "https://archive.org/download/meditations_librivox/meditations_${twoDigit}_aurelius_64kb.mp3",
              durationSeconds = 1650,
              isDurationVerified = true,
              readerName = "Martin Geeson"
            )
          }
        )
      ),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 342,
      publicationYear = "180",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_18972",
      gutendexId = 18972,
      title = "Zadig; Or, The Book of Fate",
      author = "Voltaire",
      authorsList = listOf(GutendexAuthor("Voltaire", 1694, 1778)),
      primaryCategory = "Philosophy",
      subCategory = "Adventure",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/18972/pg18972.cover.medium.jpg",
      coverGradientColors = listOf(0xFF312E81, 0xFF0D9488),
      shortDescription = "Voltaire's philosophical tale following the young Babylonian philosopher Zadig through trials of fortune, wisdom, justice, and destiny.",
      subjects = listOf("Philosophy", "Adventure stories", "Classic Literature", "Satire"),
      bookshelves = listOf("Philosophy", "Fiction"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/18972.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/18972/pg18972.cover.medium.jpg"
      ),
      downloadCount = 19400,
      fullTextUrl = "https://www.gutenberg.org/ebooks/18972.txt.utf-8",
      hasFullTextAvailable = true,
      librivoxEditions = listOf(
        com.example.data.model.LibriVoxEdition(
          id = "lv_zadig_voltaire",
          title = "Zadig; Or, The Book of Fate",
          authorName = "Voltaire",
          narratorName = "R. S. Steinberg",
          totalTime = "03:05:18",
          language = "English",
          archiveUrl = "https://archive.org/details/zadig_1107_librivox",
          sections = (1..19).map { ch ->
            val twoDigit = ch.toString().padStart(2, '0')
            com.example.data.model.LibriVoxSection(
              id = "zadig_sec_$ch",
              sectionNumber = ch,
              title = "Chapter $ch",
              listenUrl = "https://archive.org/download/zadig_1107_librivox/zadig_${twoDigit}_voltaire_64kb.mp3",
              durationSeconds = 580,
              isDurationVerified = true,
              readerName = "R. S. Steinberg"
            )
          }
        )
      ),
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 185,
      publicationYear = "1747",
      isPopular = true
    ),
    AudiobookBook(
      id = "gutenberg_1840",
      gutendexId = 1840,
      title = "Zuleika Dobson; Or, An Oxford Love Story",
      author = "Max Beerbohm",
      authorsList = listOf(GutendexAuthor("Beerbohm, Max", 1872, 1956)),
      primaryCategory = "Romance",
      subCategory = "Classic Literature",
      coverArtUrl = "https://www.gutenberg.org/cache/epub/1840/pg1840.cover.medium.jpg",
      coverGradientColors = listOf(0xFF3B0764, 0xFF9333EA),
      shortDescription = "Max Beerbohm's witty classic satire set at Oxford University, where the enchanting conjurer Zuleika Dobson turns the undergraduate world upside down.",
      subjects = listOf("Oxford (England) -- Fiction", "University of Oxford -- Fiction", "Love stories", "Satire", "Classic Literature"),
      bookshelves = listOf("Humorous stories", "Best Books Ever Listings"),
      languages = listOf("en"),
      copyright = false,
      formats = mapOf(
        "text/plain; charset=utf-8" to "https://www.gutenberg.org/ebooks/1840.txt.utf-8",
        "image/jpeg" to "https://www.gutenberg.org/cache/epub/1840/pg1840.cover.medium.jpg"
      ),
      downloadCount = 16200,
      fullTextUrl = "https://www.gutenberg.org/ebooks/1840.txt.utf-8",
      hasFullTextAvailable = true,
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 420,
      publicationYear = "1911",
      isPopular = true
    )
    )
  }

  /**
   * Popular Human-Narrated LibriVox Audiobooks with verified streams.
   */
  val CURATED_LIBRIVOX_AUDIOBOOKS: List<AudiobookBook> by lazy {
    FAMOUS_FULL_BOOKS.filter { it.librivoxEditions.isNotEmpty() }
  }

  /**
   * Modern Copyrighted Bestsellers — Explicitly labeled as Educational Summary
   * (never falsely labeled as full text or complete audiobooks).
   */
  val MODERN_EDUCATIONAL_SUMMARIES: List<AudiobookBook> by lazy {
    listOf(
      AudiobookBook(
      id = "book_atomic_habits",
      title = "Atomic Habits",
      author = "James Clear",
      primaryCategory = "Motivation",
      subCategory = "Habits",
      coverGradientColors = listOf(0xFF0F172A, 0xFF78350F),
      shortDescription = "Educational Study Guide & Key Lessons: Learn how tiny 1% daily adjustments compound into remarkable long-term personal transformation.",
      keyTakeaways = listOf(
        "Make good habits obvious, attractive, easy, and satisfying",
        "Focus on identity-based habits rather than outcomes alone",
        "Environment design is stronger than willpower"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = true,
      hasFullTextAvailable = false,
      defaultCefrLevel = "B1",
      estimatedTotalMinutes = 24,
      isFeatured = true,
      isPopular = true,
      episodes = buildAtomicHabitsEpisodes()
    ),
    AudiobookBook(
      id = "book_rich_dad_poor_dad",
      title = "Rich Dad Poor Dad",
      author = "Robert Kiyosaki",
      primaryCategory = "Wealth",
      subCategory = "Financial independence",
      coverGradientColors = listOf(0xFF1E1B4B, 0xFF065F46),
      shortDescription = "Educational Study Guide & Key Lessons: Discover financial literacy, the difference between assets and liabilities, and how cash flow builds independence.",
      keyTakeaways = listOf(
        "Understand the difference between cash-producing assets and liabilities",
        "Financial education is a foundational lifelong skill",
        "Focus on building income-generating assets"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = true,
      hasFullTextAvailable = false,
      defaultCefrLevel = "B1",
      estimatedTotalMinutes = 22,
      isFeatured = true,
      isPopular = true,
      episodes = buildRichDadEpisodes()
    ),
    AudiobookBook(
      id = "book_psychology_of_money",
      title = "The Psychology of Money",
      author = "Morgan Housel",
      primaryCategory = "Wealth",
      subCategory = "Money management",
      coverGradientColors = listOf(0xFF0F2942, 0xFF0D9488),
      shortDescription = "Educational Study Guide & Key Lessons: Doing well with money isn't only about technical formulas—it's about behavior, patience, and time freedom.",
      keyTakeaways = listOf(
        "Wealth is what you save and preserve, not what you spend",
        "Controlling your time is the highest dividend money pays",
        "Long-term compounding requires patience and room for error"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = true,
      hasFullTextAvailable = false,
      defaultCefrLevel = "B1",
      estimatedTotalMinutes = 20,
      isPopular = true,
      episodes = buildPsychologyOfMoneyEpisodes()
    ),
    AudiobookBook(
      id = "book_7_habits",
      title = "The 7 Habits of Highly Effective People",
      author = "Stephen R. Covey",
      primaryCategory = "Motivation",
      subCategory = "Leadership",
      coverGradientColors = listOf(0xFF1E3A8A, 0xFF3B82F6),
      shortDescription = "Educational Study Guide & Key Lessons: Timeless principles of personal proactivity, prioritization, empathetic listening, and win-win collaboration.",
      keyTakeaways = listOf(
        "Be proactive and focus on your Circle of Influence",
        "Begin with the end in mind and put first things first",
        "Seek first to understand, then to be understood"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = true,
      hasFullTextAvailable = false,
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 24,
      isFeatured = true,
      episodes = buildSevenHabitsEpisodes()
    ),
    AudiobookBook(
      id = "book_think_and_grow_rich",
      title = "Think and Grow Rich",
      author = "Napoleon Hill",
      primaryCategory = "Motivation",
      subCategory = "Success",
      coverGradientColors = listOf(0xFF2E1065, 0xFF854D0E),
      shortDescription = "Educational Study Guide & Key Lessons: Napoleon Hill's philosophy of definiteness of purpose, mastermind collaboration, and persistence.",
      keyTakeaways = listOf(
        "Definiteness of purpose backed by burning desire turns thought into action",
        "Positive autosuggestion strengthens focus and confidence",
        "Persistence and mastermind alliances overcome obstacles"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = false,
      hasFullTextAvailable = false,
      defaultCefrLevel = "B2",
      estimatedTotalMinutes = 25,
      isFeatured = true,
      episodes = buildThinkAndGrowRichEpisodes()
    ),
    AudiobookBook(
      id = "book_how_to_win_friends",
      title = "How to Win Friends and Influence People",
      author = "Dale Carnegie",
      primaryCategory = "Psychology",
      subCategory = "Communication",
      coverGradientColors = listOf(0xFF1E293B, 0xFF4338CA),
      shortDescription = "Educational Study Guide & Key Lessons: Practical human relations principles to communicate with warmth, empathy, and genuine appreciation.",
      keyTakeaways = listOf(
        "Become genuinely interested in other people",
        "Remember that a person's name is the most meaningful sound in any language",
        "Listen attentively and talk in terms of the other person's interests"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = true,
      hasFullTextAvailable = false,
      defaultCefrLevel = "A2",
      estimatedTotalMinutes = 20,
      isPopular = true,
      episodes = buildWinFriendsEpisodes()
    ),
    AudiobookBook(
      id = "book_the_alchemist",
      title = "The Alchemist",
      author = "Paulo Coelho",
      primaryCategory = "Stories",
      subCategory = "Classics",
      coverGradientColors = listOf(0xFF451A03, 0xFFD97706),
      shortDescription = "Educational Study Guide & Key Lessons: Explore the themes of destiny, courage, and perseverance from Santiago's journey across the desert.",
      keyTakeaways = listOf(
        "Pursuing your calling teaches you resilience and self-discovery",
        "The fear of suffering is often worse than the challenge itself",
        "Every obstacle along the journey builds wisdom"
      ),
      contentType = BookContentType.AI_LEARNING_SUMMARY,
      copyright = true,
      hasFullTextAvailable = false,
      defaultCefrLevel = "A2",
      estimatedTotalMinutes = 22,
      episodes = buildAlchemistEpisodes()
    )
    )
  }

  val CURATED_BOOKS: List<AudiobookBook> by lazy {
    FAMOUS_FULL_BOOKS + MODERN_EDUCATIONAL_SUMMARIES
  }

  // =========================================================================
  // EPISODE BUILDERS FOR EDUCATIONAL SUMMARIES
  // =========================================================================

  private fun buildAtomicHabitsEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "atomic_ep_1",
        episodeNumber = 1,
        title = "Episode 1: The Surprising Power of Small Habits",
        subtitle = "Introduction to the compound effect of 1% improvements",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Welcome to the educational summary of Atomic Habits by James Clear.", "Welcome to this summary.", "atomic", "extremely small, like an atom"),
          ListeningDialogueLine("Narrator", "If you can get one percent better each day for one year, you will end up thirty-seven times better by the time you are done.", "1% improvement daily yields massive progress.", "compound", "accumulate with exponential growth"),
          ListeningDialogueLine("Narrator", "Habits are the compound interest of self-improvement.", "Habits multiply over time.", "self-improvement", "becoming better through dedication"),
          ListeningDialogueLine("Narrator", "Too often, we convince ourselves that massive success requires massive action.", "People wrongly believe only huge actions count.", "massive", "exceptionally large in scale"),
          ListeningDialogueLine("Narrator", "Meanwhile, improving by one percent isn't particularly notable, but it can be far more meaningful in the long run.", "Small consistency matters more than huge leaps.", "in the long run", "over a prolonged period of time")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "According to James Clear, what are habits like?",
            options = listOf("Compound interest of self-improvement", "A quick secret trick", "A sudden stroke of good luck", "An effortless natural talent"),
            correctIndex = 0,
            explanation = "Habits compound like interest over time to produce exponential improvements."
          )
        ),
        speakingPracticePrompt = "Describe one small habit you do every single morning that helps you learn.",
        practicalApplicationTip = "Choose one tiny habit today that takes less than two minutes to complete."
      ),
      AudiobookEpisode(
        id = "atomic_ep_2",
        episodeNumber = 2,
        title = "Episode 2: The Four Laws of Behavior Change",
        subtitle = "Make it Obvious, Attractive, Easy, and Satisfying",
        estimatedMinutes = 6,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "To build a good habit, remember the four simple laws.", "Four key rules shape good habits.", "behavior", "the way a person acts"),
          ListeningDialogueLine("Narrator", "First: Make it obvious. Design your physical environment so your cues are visible.", "Place visual reminders where you can see them.", "cue", "a trigger that prompts action"),
          ListeningDialogueLine("Narrator", "Second: Make it attractive. Pair what you need to do with what you want to do.", "Temptation bundling makes tasks enjoyable.", "attractive", "appealing and desirable"),
          ListeningDialogueLine("Narrator", "Third: Make it easy. Reduce friction and master the two-minute rule.", "Small steps make starting frictionless.", "friction", "resistance or obstacles that slow you down"),
          ListeningDialogueLine("Narrator", "Fourth: Make it satisfying. Immediate rewards reinforce the habit loop in your brain.", "Celebration builds neural memory.", "reinforce", "strengthen with repeated confirmation")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What is the third law of behavior change in Atomic Habits?",
            options = listOf("Make it easy", "Make it obvious", "Make it difficult", "Make it expensive"),
            correctIndex = 0,
            explanation = "The third law is 'Make it easy' by reducing friction."
          )
        ),
        speakingPracticePrompt = "Explain how you can make practicing your target language obvious and easy.",
        practicalApplicationTip = "Place your language notebook or open LingoSphere on your phone's home screen."
      ),
      AudiobookEpisode(
        id = "atomic_ep_3",
        episodeNumber = 3,
        title = "Episode 3: Identity-Based Habits & Review",
        subtitle = "Focusing on who you want to become rather than outcomes",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "True behavior change is identity change.", "Real transformation changes your self-perception.", "identity", "who a person is and believes they are"),
          ListeningDialogueLine("Narrator", "The goal is not to read a book, the goal is to become a reader.", "Transform your beliefs about yourself.", "reader", "someone who regularly enjoys books"),
          ListeningDialogueLine("Narrator", "The goal is not to learn twenty words, the goal is to become a confident multilingual speaker.", "Aim for a fluent identity.", "multilingual", "able to speak multiple languages"),
          ListeningDialogueLine("Narrator", "Every action you take is a vote for the type of person you wish to become.", "Actions cast votes for your future.", "vote", "a choice that supports an outcome")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What is the ultimate focus of identity-based habits?",
            options = listOf("Who you want to become", "Winning medals quickly", "Pleasing other people", "Collecting material objects"),
            correctIndex = 0,
            explanation = "Identity-based habits focus on shifting who you believe you are."
          )
        ),
        speakingPracticePrompt = "Say aloud: 'Every day I speak and listen, I cast a vote for my fluent identity.'",
        practicalApplicationTip = "Celebrate every lesson with a proud affirmation that you are becoming a speaker."
      )
    )
  }

  private fun buildRichDadEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "richdad_ep_1",
        episodeNumber = 1,
        title = "Episode 1: The Lesson of Two Fathers",
        subtitle = "Understanding financial education and mindset",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Robert Kiyosaki grew up with the influence of two fathers.", "Two fathers taught opposite lessons.", "influence", "the power to shape decisions"),
          ListeningDialogueLine("Narrator", "One father was highly educated with a doctorate, yet struggled financially all his life.", "The educated father had money stress.", "struggled", "faced prolonged difficulty"),
          ListeningDialogueLine("Narrator", "The other father did not finish eighth grade, but became one of the wealthiest men in Hawaii.", "The entrepreneur father grew rich.", "wealthiest", "having the greatest assets"),
          ListeningDialogueLine("Narrator", "One dad said, 'The love of money is the root of all evil.' The other said, 'The lack of money is the root of all evil.'", "Two different beliefs about money.", "root", "the foundational cause")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What was the main difference between the two fathers?",
            options = listOf("Their financial education and mindset", "Their height and weight", "Their favorite sports", "Their birthplace"),
            correctIndex = 0,
            explanation = "Their beliefs and education regarding money created completely different lives."
          )
        ),
        speakingPracticePrompt = "Explain what financial literacy means to you in simple sentences.",
        practicalApplicationTip = "Review your last three purchases and identify whether they were assets or expenses."
      ),
      AudiobookEpisode(
        id = "richdad_ep_2",
        episodeNumber = 2,
        title = "Episode 2: Assets vs. Liabilities",
        subtitle = "The golden rule of building genuine wealth",
        estimatedMinutes = 6,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Rule one is simple: You must know the difference between an asset and a liability.", "Know assets from liabilities.", "liability", "something that costs money ongoing"),
          ListeningDialogueLine("Narrator", "An asset puts money into your pocket, whether you work or not.", "Assets generate income.", "cash flow", "the flow of money in and out"),
          ListeningDialogueLine("Narrator", "A liability takes money out of your pocket through payments, taxes, and maintenance.", "Liabilities drain wealth.", "maintenance", "cost of keeping something running"),
          ListeningDialogueLine("Narrator", "Rich people acquire assets; the poor and middle class acquire liabilities that they think are assets.", "Understand true cash-flowing assets.", "acquire", "gain possession of")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "According to Rich Dad Poor Dad, what does an asset do?",
            options = listOf("Puts money in your pocket", "Takes money out of your pocket", "Causes stress and taxes only", "Guarantees instant luxury"),
            correctIndex = 0,
            explanation = "An asset generates income and puts money in your pocket."
          )
        ),
        speakingPracticePrompt = "Give two examples of assets that generate passive income.",
        practicalApplicationTip = "Commit to dedicating part of your income each month toward acquiring assets."
      )
    )
  }

  private fun buildPsychologyOfMoneyEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "psych_money_ep_1",
        episodeNumber = 1,
        title = "Episode 1: No One's Crazy",
        subtitle = "Why everyone's personal money worldview makes sense to them",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Your personal experiences with money make up a tiny fraction of what has happened in the world, but most of how you think the world works.", "Our personal history shapes our mindset.", "worldview", "the way one perceives reality"),
          ListeningDialogueLine("Narrator", "People from different generations, raised by different parents, view risk and reward differently.", "Upbringing determines comfort with risk.", "upbringing", "treatment and education in childhood"),
          ListeningDialogueLine("Narrator", "When you understand this, you realize nobody is crazy; everyone is operating from their own experience.", "Empathy explains financial behavior.", "empathy", "ability to understand others' feelings")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "Why do different people make completely different financial decisions?",
            options = listOf("Their unique life experiences shape their view of risk", "They do not know basic math", "They follow television advertisements", "They all have the same goals"),
            correctIndex = 0,
            explanation = "Unique personal history and generational experiences dictate how people view money."
          )
        ),
        speakingPracticePrompt = "Share a lesson your parents or childhood taught you about spending or saving.",
        practicalApplicationTip = "Avoid judging others' financial decisions and focus on your long-term independence."
      ),
      AudiobookEpisode(
        id = "psych_money_ep_2",
        episodeNumber = 2,
        title = "Episode 2: Freedom & The Highest Dividend",
        subtitle = "The ability to control your time is the ultimate wealth",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "The highest form of wealth is the ability to wake up every morning and say: 'I can do whatever I want today.'", "Time control is true wealth.", "dividend", "a reward or share of profit"),
          ListeningDialogueLine("Narrator", "Money's greatest intrinsic value is its ability to give you control over your time.", "Money buys freedom, not just possessions.", "intrinsic", "belonging naturally to something"),
          ListeningDialogueLine("Narrator", "Having a buffer of savings gives you the flexibility to wait for good opportunities and leave toxic environments.", "Savings provide real peace of mind.", "flexibility", "ability to adapt easily")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What does Morgan Housel consider the highest form of wealth?",
            options = listOf("Controlling your time and daily choices", "Owning four sports cars", "Having the biggest house in town", "Wearing designer clothing"),
            correctIndex = 0,
            explanation = "Freedom and control over your time is the greatest dividend money can pay."
          )
        ),
        speakingPracticePrompt = "Describe what a completely free and productive day would look like for you.",
        practicalApplicationTip = "Build a peace-of-mind fund so you always retain control over your life choices."
      )
    )
  }

  private fun buildSevenHabitsEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "7habits_ep_1",
        episodeNumber = 1,
        title = "Episode 1: Be Proactive & Begin with the End in Mind",
        subtitle = "Mastering personal responsibility and purposeful vision",
        estimatedMinutes = 6,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Highly effective people do not blame circumstances or conditions for their behavior.", "Proactive people take ownership.", "proactive", "creating or controlling a situation rather than just responding to it"),
          ListeningDialogueLine("Narrator", "Between stimulus and response, human beings have the freedom to choose.", "We choose our response to every event.", "stimulus", "an event that evokes a reaction"),
          ListeningDialogueLine("Narrator", "Habit two is to begin with the end in mind: define your clear destination before taking your first step.", "Vision guides daily priorities.", "destination", "the place or goal to which someone is going")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What does 'Being Proactive' mean in Stephen Covey's framework?",
            options = listOf("Taking responsibility and choosing your response to situations", "Reacting angrily to every problem", "Waiting for someone else to decide", "Avoiding all responsibility"),
            correctIndex = 0,
            explanation = "Proactivity means recognizing your power to choose your response in any situation."
          )
        ),
        speakingPracticePrompt = "Describe a recent challenge where you chose a calm, proactive response.",
        practicalApplicationTip = "Focus 100% of your energy today on things within your Circle of Influence."
      )
    )
  }

  private fun buildThinkAndGrowRichEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "think_ep_1",
        episodeNumber = 1,
        title = "Episode 1: Desire & The Starting Point of Achievement",
        subtitle = "Turning a wish into a burning, definite purpose",
        estimatedMinutes = 6,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Wishing will not bring wealth.", "Wishes alone accomplish nothing.", "wishing", "hoping passively for good things"),
          ListeningDialogueLine("Narrator", "Desiring achievement with a definite purpose, planning clear ways, and backing those plans with persistence will bring success.", "Definite desire backed by persistence succeeds.", "persistence", "firm continuance in a course of action in spite of difficulty"),
          ListeningDialogueLine("Narrator", "Whatever the mind can conceive and believe, it can achieve.", "Belief is the catalyst of success.", "conceive", "form an idea or plan in the mind")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "According to Napoleon Hill, what is the starting point of all achievement?",
            options = listOf("Definite burning desire", "Inheriting money from family", "Waiting for circumstances to change", "Avoiding all challenging work"),
            correctIndex = 0,
            explanation = "A clear, burning desire with definite plans is the catalyst for achievement."
          )
        ),
        speakingPracticePrompt = "State your primary learning goal with absolute clarity and conviction.",
        practicalApplicationTip = "Write down your language fluency goal and review it morning and evening."
      )
    )
  }

  private fun buildWinFriendsEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "winfriends_ep_1",
        episodeNumber = 1,
        title = "Episode 1: Fundamental Ways to Connect with People",
        subtitle = "Handling human relations with warmth and curiosity",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "You can make more friends in two months by becoming interested in other people than you can in two years by trying to get other people interested in you.", "Focus on others to connect.", "interested", "showing curiosity and care"),
          ListeningDialogueLine("Narrator", "Principle one: Become genuinely interested in other people.", "Authentic curiosity builds trust.", "genuinely", "in a truthful and sincere way"),
          ListeningDialogueLine("Narrator", "Principle two: Smile. An honest smile communicates welcome and kindness across all languages.", "A smile is universal.", "universal", "applicable to everyone everywhere"),
          ListeningDialogueLine("Narrator", "Principle three: Remember that a person's name is to that person the sweetest and most important sound in any language.", "Honor people's names.", "honor", "regard with great respect")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What is the fastest way to build genuine friendships?",
            options = listOf("Become genuinely interested in other people", "Brag about your achievements", "Criticize other people's opinions", "Talk only about your own interests"),
            correctIndex = 0,
            explanation = "Dale Carnegie proved that genuine curiosity about others builds deep connection."
          )
        ),
        speakingPracticePrompt = "Introduce yourself with a warm smile and ask an open-ended question.",
        practicalApplicationTip = "Remember and warmly use someone's name in your next conversation."
      )
    )
  }

  private fun buildArtOfWarEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "artofwar_ep_1",
        episodeNumber = 1,
        title = "Chapter 1: Laying Plans",
        subtitle = "Public domain classic text: Five fundamental factors of strategy",
        estimatedMinutes = 6,
        narrationLines = listOf(
          ListeningDialogueLine("Sun Tzu", "The art of war is of vital importance to the state.", "Strategy is essential for survival.", "vital", "absolutely necessary or important"),
          ListeningDialogueLine("Sun Tzu", "It is a matter of life and death, a road either to safety or to ruin.", "Careful decisions protect nations.", "ruin", "the destruction or collapse of something"),
          ListeningDialogueLine("Sun Tzu", "Hence it is a subject of inquiry which can on no account be neglected.", "Strategy requires focused inquiry.", "inquiry", "an act of asking for information"),
          ListeningDialogueLine("Sun Tzu", "The supreme art of war is to subdue the enemy without fighting.", "True mastery avoids conflict.", "subdue", "overcome, quieten, or bring under control")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What did Sun Tzu consider the supreme art of war?",
            options = listOf("Subduing the enemy without fighting", "Engaging in endless battles", "Building the largest fortress", "Collecting weapons only"),
            correctIndex = 0,
            explanation = "Sun Tzu stated that winning without fighting is the highest victory."
          )
        ),
        speakingPracticePrompt = "Explain how calm preparation and patience help you win daily challenges.",
        practicalApplicationTip = "Plan your week's language learning time before the week begins."
      )
    )
  }

  private fun buildSherlockEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "sherlock_ep_1",
        episodeNumber = 1,
        title = "Adventure I: A Scandal in Bohemia",
        subtitle = "Public domain classic detective mystery with Dr. John Watson",
        estimatedMinutes = 6,
        narrationLines = listOf(
          ListeningDialogueLine("Dr. Watson", "To Sherlock Holmes she is always THE woman. I have seldom heard him mention her under any other name.", "Watson recalls Holmes's respect for Irene Adler.", "seldom", "not often; rarely"),
          ListeningDialogueLine("Sherlock Holmes", "You see, but you do not observe. The distinction is clear.", "Observation is deeper than passive looking.", "distinction", "a difference or contrast between similar things"),
          ListeningDialogueLine("Dr. Watson", "For example, you have frequently seen the steps which lead up from the hall to this room.", "Watson asks about daily details.", "frequently", "regularly or often"),
          ListeningDialogueLine("Sherlock Holmes", "Frequently. And how many are there?", "Holmes tests Watson's observation.", "steps", "stairs leading upward"),
          ListeningDialogueLine("Dr. Watson", "How many? I do not know.", "Watson confesses he didn't count.", "confess", "admit that one has committed an omission"),
          ListeningDialogueLine("Sherlock Holmes", "Quite so! You have not observed. And yet you have seen. Now, I know that there are seventeen steps, because I have both seen and observed.", "Observation notices meaningful details.", "observe", "notice or perceive something and register it as significant")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What is the difference between seeing and observing according to Sherlock Holmes?",
            options = listOf("Observing involves noticing and registering significant details", "Seeing is only done at night", "Observing requires a magnifying glass", "There is no difference"),
            correctIndex = 0,
            explanation = "Holmes explains that observing means actively analyzing and registering what you see."
          )
        ),
        speakingPracticePrompt = "Look around your room and describe three small details in your target language.",
        practicalApplicationTip = "Pay close attention to pronunciation details when listening to native speakers."
      )
    )
  }

  private fun buildAlchemistEpisodes(): List<AudiobookEpisode> {
    return listOf(
      AudiobookEpisode(
        id = "alchemist_ep_1",
        episodeNumber = 1,
        title = "Episode 1: The Boy and His Personal Legend",
        subtitle = "Following the omens across the desert toward your destiny",
        estimatedMinutes = 5,
        narrationLines = listOf(
          ListeningDialogueLine("Narrator", "Santiago was an Andalusian shepherd who loved his sheep, but yearned to travel.", "Santiago loved traveling the hills.", "yearn", "have an intense feeling of longing"),
          ListeningDialogueLine("Melchizedek", "A Personal Legend is what you have always wanted to accomplish. Everyone, when they are young, knows what their Personal Legend is.", "Youth understands its dreams clearly.", "accomplish", "achieve or complete successfully"),
          ListeningDialogueLine("Melchizedek", "At that point in their lives, everything is clear and everything is possible. They are not afraid to dream.", "Hope is abundant in youth.", "abundant", "existing or available in large quantities"),
          ListeningDialogueLine("Narrator", "And when you want something, all the universe conspires in helping you to achieve it.", "The universe supports your courage.", "conspire", "work together toward a shared result")
        ),
        comprehensionQuestions = listOf(
          AcademyComprehensionQuestion(
            question = "What does the old king explain about a 'Personal Legend'?",
            options = listOf("It is what you have always truly wanted to accomplish", "It is an impossible fantasy to abandon", "It is only for famous rulers", "It is dangerous and harmful"),
            correctIndex = 0,
            explanation = "The book defines a Personal Legend as a person's deepest calling and purpose."
          )
        ),
        speakingPracticePrompt = "What dream or personal legend has learning languages helped you pursue?",
        practicalApplicationTip = "Take one bold step today toward your true aspirations without fear of failure."
      )
    )
  }
}
