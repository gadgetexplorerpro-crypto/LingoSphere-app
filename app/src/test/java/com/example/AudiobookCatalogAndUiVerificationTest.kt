package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.ai.AiProviderManager
import com.example.ai.GeminiModelTier
import com.example.data.model.AudiobookBook
import com.example.data.model.BookContentType
import com.example.data.model.Language
import com.example.data.repository.AudiobookCatalogRepository
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.ui.components.AudiobooksCategoryScreen
import com.example.ui.theme.LingoSphereTheme
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h850dp-mdpi")
class AudiobookCatalogAndUiVerificationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private var httpServer: HttpServer? = null

  @Before
  fun setUp() {
    GutendexAndLibriVoxService.resetTestingOverrides()
    GutendexAndLibriVoxService.clearAllCaches()
  }

  @After
  fun tearDown() {
    httpServer?.stop(0)
    httpServer = null
    GutendexAndLibriVoxService.resetTestingOverrides()
    GutendexAndLibriVoxService.clearAllCaches()
  }

  @Test
  fun categoryMatcherMapsAllKeyCategoriesWithoutZeroResults() {
    val allBooks = AudiobookCatalogRepository.CURATED_BOOKS
    val criticalTabs = listOf(
      "Motivation and Success",
      "Wealth and Money",
      "Business and Entrepreneurship",
      "Personal Development",
      "Productivity",
      "Leadership",
      "Psychology",
      "Classic Literature",
      "Children's Stories"
    )

    for (tab in criticalTabs) {
      val matches = allBooks.filter { AudiobookCatalogRepository.matchesCategoryTab(it, tab) }
      assertTrue("Category tab '$tab' must match at least 1 curated book", matches.isNotEmpty())
    }
  }

  @Test
  fun libivoxLightweightSearchBookMarksHumanNarrationAvailableAndExtractsNarrator() {
    val lvJson = JSONObject().apply {
      put("id", "99999")
      put("title", "The Wealth of Nations")
      put("totaltime", "12:00:00")
      put("totaltimesecs", 43200)
      put("language", "English")
      put("url_iarchive", "https://archive.org/details/wealth_nations_lv")
      put("url_text_source", "https://www.gutenberg.org/ebooks/3300")
      put(
        "authors",
        JSONArray().put(
          JSONObject().apply {
            put("first_name", "Adam")
            put("last_name", "Smith")
          }
        )
      )
      put(
        "genres",
        JSONArray().put(JSONObject().apply { put("name", "Business & Economics") })
      )
      put(
        "sections",
        JSONArray().put(
          JSONObject().apply {
            put(
              "readers",
              JSONArray().put(
                JSONObject().apply {
                  put("display_name", "Stephen Escalera")
                }
              )
            )
          }
        )
      )
    }

    val parsed = GutendexAndLibriVoxService.parseLibriVoxBookJson(lvJson)
    assertNotNull(parsed)
    assertEquals("gutenberg_3300", parsed!!.id)
    assertEquals(3300, parsed.gutendexId)
    assertTrue("Lightweight LibriVox entry must report hasHumanNarration = true", parsed.hasHumanNarration)
    assertTrue("Linked Gutenberg ID must expose full text availability", parsed.hasFullTextAvailable)
    assertEquals("Stephen Escalera", parsed.librivoxEditions.first().narratorName)
    assertTrue(
      "Wealth & Economics book should match 'Wealth and Money' tab",
      AudiobookCatalogRepository.matchesCategoryTab(parsed, "Wealth and Money")
    )
  }

  @Test
  fun gutenbergTextVerifierRejectsHtmlErrorsAndAcceptsShortTitles() {
    val artOfWar = AudiobookBook(
      id = "gutenberg_132",
      gutendexId = 132,
      title = "The Art of War",
      author = "Sun Tzu",
      primaryCategory = "Business",
      subCategory = "Leadership",
      shortDescription = "Classic treatise on strategy",
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      hasFullTextAvailable = true
    )

    val html404 = """
      <!DOCTYPE html>
      <html><head><title>404 Not Found</title></head>
      <body>Page not found for The Art of War</body></html>
    """.trimIndent()
    assertFalse(
      "Must reject 404 HTML error page even if title appears in error message",
      GutendexAndLibriVoxService.isDownloadedTextMatchingBook(html404, artOfWar)
    )

    val validShortTitle = """
      The Project Gutenberg EBook of The Art of War, by Sun Tzu
      Title: The Art of War
      Author: Sun Tzu
      *** START OF THE PROJECT GUTENBERG EBOOK THE ART OF WAR ***
      I. Laying Plans.
      Sun Tzu said: The art of war is of vital importance to the State.
    """.trimIndent()
    assertTrue(
      "Must accept genuine Project Gutenberg text for short title 'The Art of War'",
      GutendexAndLibriVoxService.isDownloadedTextMatchingBook(validShortTitle, artOfWar)
    )
  }

  @Test
  fun bookAndAudioLibraryRemovesAllAZButtonsAndShowsCompleteCatalogImmediately() {
    val allCurated = AudiobookCatalogRepository.CURATED_BOOKS
    assertTrue("Catalog must include 'A Room with a View'", allCurated.any { it.title == "A Room with a View" })
    assertTrue("Catalog must include 'Alice's Adventures in Wonderland'", allCurated.any { it.title.startsWith("Alice") })
    assertTrue("Catalog must include 'Meditations'", allCurated.any { it.title == "Meditations" })
    assertTrue("Catalog must include 'Zadig; Or, The Book of Fate'", allCurated.any { it.title.startsWith("Zadig") })

    val enLang = Language.findByCode("en")
    composeTestRule.setContent {
      LingoSphereTheme {
        AudiobooksCategoryScreen(
          userId = "u_catalog_no_az_test",
          targetLang = enLang,
          selectedCefrLevel = "B1",
          onSelectBook = {},
          onOpenSearch = {},
          onOpenCreateListening = {},
          onBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Heading is "Book & Audio Library" and NO "A-Z" / "A–Z" buttons or text exist
    assertTrue(
      "Heading must be 'Book & Audio Library'",
      composeTestRule.onAllNodesWithText("Book & Audio Library", substring = false).fetchSemanticsNodes().isNotEmpty()
    )
    assertTrue(
      "Must NOT display old 'A–Z Book & Audio Library' heading",
      composeTestRule.onAllNodesWithText("A–Z Book & Audio Library", substring = true).fetchSemanticsNodes().isEmpty()
    )
    assertTrue(
      "Alphabet bar must be completely removed",
      composeTestRule.onAllNodesWithText("A-Z", substring = true).fetchSemanticsNodes().isEmpty()
    )
    assertEquals(
      "az_letter_A button must not exist",
      0,
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("az_letter_A")).fetchSemanticsNodes().size
    )
    assertEquals(
      "az_navigation_bar must not exist",
      0,
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("az_navigation_bar")).fetchSemanticsNodes().size
    )

    // 2. Complete catalog loads immediately with books across the alphabet
    assertTrue(
      "Unified catalog must immediately display 'A Room with a View'",
      composeTestRule.onAllNodesWithText("A Room with a View", substring = true).fetchSemanticsNodes().isNotEmpty()
    )
  }

  @Test
  fun multiPageCatalogPaginationAndDeduplicationCombinesGutendexAndLibriVoxCleanly() = runBlocking {
    val server = HttpServer.create(InetSocketAddress(0), 0)
    val port = server.address.port
    val baseUrl = "http://127.0.0.1:$port"

    server.createContext("/books") { exchange ->
      val query = exchange.requestURI.rawQuery ?: ""
      val body = if (query.contains("page=2")) {
        """
        {
          "count": 2,
          "next": null,
          "previous": "$baseUrl/books?page=1",
          "results": [
            {
              "id": 9102,
              "title": "Abroad in the Alps",
              "authors": [{"name": "Forster, E. M."}],
              "languages": ["en"],
              "subjects": ["Travel", "Classic Literature"],
              "formats": {"text/plain; charset=utf-8": "$baseUrl/files/9102.txt"}
            }
          ]
        }
        """.trimIndent()
      } else {
        """
        {
          "count": 2,
          "next": "$baseUrl/books?page=2",
          "previous": null,
          "results": [
            {
              "id": 2641,
              "title": "A Room with a View",
              "authors": [{"name": "Forster, E. M."}],
              "languages": ["en"],
              "subjects": ["Romance", "Classic Literature"],
              "formats": {"text/plain; charset=utf-8": "$baseUrl/files/2641.txt"}
            }
          ]
        }
        """.trimIndent()
      }
      val bytes = body.toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }

    server.createContext("/librivox") { exchange ->
      // Returns the same book (Gutenberg #2641) with human narration + Elizabeth Klett narrator
      val body = """
        {
          "books": [
            {
              "id": "26410",
              "title": "Room with a View",
              "totaltime": "06:45:00",
              "totaltimesecs": 24300,
              "language": "English",
              "url_iarchive": "https://archive.org/details/room_with_a_view_lv",
              "url_text_source": "https://www.gutenberg.org/ebooks/2641",
              "authors": [{"first_name": "E. M.", "last_name": "Forster"}],
              "genres": [{"name": "Romance"}],
              "sections": [
                {
                  "id": "1",
                  "section_number": "1",
                  "title": "Chapter I - The Bertolini",
                  "listen_url": "$baseUrl/audio/ch1.mp3",
                  "playtime": "910",
                  "readers": [{"display_name": "Elizabeth Klett"}]
                }
              ]
            }
          ]
        }
      """.trimIndent()
      val bytes = body.toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    httpServer = server

    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$baseUrl/books"
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$baseUrl/librivox"

    // Fetch Page 1 with Letter A
    val page1 = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "",
      languageCode = "en",
      selectedLetter = "A",
      sortAlphabetically = true
    )

    // Verify deduplication: Gutenberg #2641 from Gutendex and LibriVox #26410 must merge into 1 entry with BOTH full text and human narration
    val roomMatches = page1.books.filter { it.gutendexId == 2641 }
    assertEquals("Gutendex and LibriVox entries for PG #2641 must be deduplicated into 1 book", 1, roomMatches.size)
    val mergedRoom = roomMatches.first()
    assertTrue("Merged book must have full text available", mergedRoom.hasFullTextAvailable)
    assertTrue("Merged book must have human narration available", mergedRoom.hasHumanNarration)
    assertEquals("Elizabeth Klett", mergedRoom.librivoxEditions.first().narratorName)
    assertNotNull("Page 1 must expose gutendexNextPageUrl for pagination", page1.gutendexNextPageUrl)

    // Fetch Page 2 using gutendexPageUrl
    val page2 = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "",
      languageCode = "en",
      gutendexPageUrl = page1.gutendexNextPageUrl,
      selectedLetter = "A",
      sortAlphabetically = true
    )
    assertTrue(
      "Page 2 must return 'Abroad in the Alps'",
      page2.books.any { it.title == "Abroad in the Alps" }
    )
  }

  @Test
  fun audiobooksCategoryScreenFiltersCategoriesNarratorsLanguagesFormatsAndSearchTogether() {
    val esLang = Language.findByCode("es") // Non-English learner must still see classics & categories!

    composeTestRule.setContent {
      LingoSphereTheme {
        AudiobooksCategoryScreen(
          userId = "u_catalog_test",
          targetLang = esLang,
          selectedCefrLevel = "B1",
          onSelectBook = {},
          onOpenSearch = {},
          onOpenCreateListening = {},
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Tap "Wealth and Money" category chip -> must display "The Science of Getting Rich", NOT "0 books available"
    composeTestRule.onNodeWithTag("category_chip_wealth_and_money")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    assertTrue(
      "Wealth and Money category must display 'The Science of Getting Rich'",
      composeTestRule.onAllNodesWithText("The Science of Getting Rich", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    )
    assertTrue(
      "Wealth and Money category must NEVER display '0 books available'",
      composeTestRule.onAllNodesWithText("0 books available", substring = false)
        .fetchSemanticsNodes().isEmpty()
    )

    // Tap "Motivation and Success" chip -> must display "As a Man Thinketh"
    composeTestRule.onNodeWithTag("category_chip_motivation_and_success")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    assertTrue(
      "Motivation and Success category must display 'As a Man Thinketh'",
      composeTestRule.onAllNodesWithText("As a Man Thinketh", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    )

    // Combine Genre 'Romance' + Language 'English' + Narrator 'Elizabeth Klett' + Human Audio (AND logic)
    composeTestRule.onNodeWithTag("category_chip_romance").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("lang_chip_en").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("narrator_chip_elizabeth_klett").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("filter_only_audio").performClick()
    composeTestRule.waitForIdle()

    assertTrue(
      "Combined Romance + English + Narrator Elizabeth Klett + Human Audio must display 'A Room with a View'",
      composeTestRule.onAllNodesWithText("A Room with a View", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    )
    assertTrue(
      "Book card must display narrator 'Elizabeth Klett'",
      composeTestRule.onAllNodesWithText("Elizabeth Klett", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    )
    assertTrue(
      "Book card must display 'Human Audio' availability badge",
      composeTestRule.onAllNodesWithText("Human Audio", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    )

    // Use Clear filters chip to reset active filters in one tap, then search "Alice"
    composeTestRule.onNodeWithTag("audiobooks_clear_filters_chip").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audiobooks_category_search_input").performTextInput("Alice")
    composeTestRule.waitForIdle()

    assertTrue(
      "Search 'Alice' must match 'Alice's Adventures in Wonderland'",
      composeTestRule.onAllNodesWithText("Alice's Adventures in Wonderland", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    )
  }

  @Test
  fun aiProviderManagerDefaultsPreserveGeminiAndOpenAiCapabilities() {
    assertTrue(GeminiModelTier.FLASH_LITE.modelId.isNotBlank())
    assertTrue(GeminiModelTier.FLASH_STANDARD.modelId.isNotBlank())
    assertEquals("gpt-4o-mini", AiProviderManager.OPENAI_TEXT_DEFAULT_MODEL)
    assertEquals("gpt-4o-mini-realtime-preview", AiProviderManager.OPENAI_REALTIME_DEFAULT_MODEL)
  }

  @Test
  fun unlimitedCatalogLoadsWellBeyond23BooksAcrossMultiplePagesAndIndexesMetadataOnly() = runBlocking {
    val server = HttpServer.create(InetSocketAddress(0), 0)
    val port = server.address.port
    val baseUrl = "http://127.0.0.1:$port"

    server.createContext("/books") { exchange ->
      val query = exchange.requestURI.rawQuery ?: ""
      val pageNum = when {
        query.contains("page=3") -> 3
        query.contains("page=2") -> 2
        else -> 1
      }
      val resultsArray = JSONArray()
      val startId = 5000 + (pageNum - 1) * 15
      for (i in 0 until 15) {
        val bookId = startId + i
        resultsArray.put(
          JSONObject().apply {
            put("id", bookId)
            put("title", "Book Catalog Volume ${String.format("%03d", (pageNum - 1) * 15 + i + 1)}")
            put("authors", JSONArray().put(JSONObject().put("name", "Author Number $i")))
            put("languages", JSONArray().put("en"))
            put("subjects", JSONArray().put("Classic Literature").put("Adventure"))
            put(
              "formats",
              JSONObject()
                .put("text/plain; charset=utf-8", "$baseUrl/files/$bookId.txt")
                .put("image/jpeg", "$baseUrl/covers/$bookId.jpg")
            )
          }
        )
      }
      val nextUrl = if (pageNum < 3) "$baseUrl/books?page=${pageNum + 1}" else JSONObject.NULL
      val responseJson = JSONObject().apply {
        put("count", 45)
        put("next", nextUrl)
        put("previous", if (pageNum > 1) "$baseUrl/books?page=${pageNum - 1}" else JSONObject.NULL)
        put("results", resultsArray)
      }
      val bytes = responseJson.toString().toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }

    server.createContext("/librivox") { exchange ->
      val query = exchange.requestURI.rawQuery ?: ""
      val offset = Regex("offset=(\\d+)").find(query)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
      val booksArray = JSONArray()
      if (offset < 60) {
        for (i in 0 until 5) {
          val lvId = 80000 + offset + i
          booksArray.put(
            JSONObject().apply {
              put("id", lvId.toString())
              put("title", "Broadcast Audiobook ${offset + i + 1}")
              put("totaltime", "03:20:00")
              put("totaltimesecs", 12000)
              put("language", "English")
              put("url_iarchive", "https://archive.org/details/broadcast_audio_$lvId")
              put("authors", JSONArray().put(JSONObject().put("first_name", "Clara").put("last_name", "Narrator")))
              put("genres", JSONArray().put(JSONObject().put("name", "Literature")))
              put(
                "sections",
                JSONArray().put(
                  JSONObject().put("readers", JSONArray().put(JSONObject().put("display_name", "Mark Nelson")))
                )
              )
            }
          )
        }
      }
      val responseJson = JSONObject().put("books", booksArray)
      val bytes = responseJson.toString().toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }

    server.start()
    httpServer = server
    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$baseUrl/books"
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$baseUrl/librivox"

    // Fetch 3 consecutive pages dynamically (no artificial 23-book cap)
    val page1 = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "",
      languageCode = "en",
      librivoxOffset = 0,
      librivoxLimit = 24
    )
    assertNotNull(page1.gutendexNextPageUrl)

    val page2 = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "",
      languageCode = "en",
      gutendexPageUrl = page1.gutendexNextPageUrl,
      librivoxOffset = page1.librivoxNextOffset ?: 0,
      librivoxLimit = 24
    )
    assertNotNull(page2.gutendexNextPageUrl)

    val page3 = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "",
      languageCode = "en",
      gutendexPageUrl = page2.gutendexNextPageUrl,
      librivoxOffset = page2.librivoxNextOffset ?: 0,
      librivoxLimit = 24
    )

    val combinedBooks = (AudiobookCatalogRepository.CURATED_BOOKS + page1.books + page2.books + page3.books)
      .distinctBy { it.id }

    assertTrue(
      "Unlimited catalog must load far more than 23 books across pages (loaded=${combinedBooks.size})",
      combinedBooks.size >= 70
    )

    // Verify metadata-only CatalogMetadataIndex contains all discovered remote books
    val indexedEntries = GutendexAndLibriVoxService.getIndexedCatalogEntries().filter { it.firstLetter == "B" }
    assertTrue("Catalog metadata index must store letter 'B' entries", indexedEntries.size >= 45)
    assertTrue(indexedEntries.all { it.sourceId.isNotBlank() && it.title.isNotBlank() && it.author.isNotBlank() })
  }

  @Test
  fun searchFindsRemoteBookNotOnFirstPageAndRecoversFromNetworkFailureOnRetry() = runBlocking {
    var shouldFailNetwork = true
    val server = HttpServer.create(InetSocketAddress(0), 0)
    val port = server.address.port
    val baseUrl = "http://127.0.0.1:$port"

    server.createContext("/books") { exchange ->
      if (shouldFailNetwork) {
        exchange.sendResponseHeaders(503, -1)
        exchange.close()
        return@createContext
      }
      val query = exchange.requestURI.rawQuery ?: ""
      val results = JSONArray()
      if (query.contains("Monte") || query.contains("Dumas")) {
        results.put(
          JSONObject().apply {
            put("id", 1184)
            put("title", "The Count of Monte Cristo")
            put("authors", JSONArray().put(JSONObject().put("name", "Dumas, Alexandre")))
            put("languages", JSONArray().put("en"))
            put("subjects", JSONArray().put("Adventure stories").put("Revenge -- Fiction"))
            put("formats", JSONObject().put("text/plain; charset=utf-8", "$baseUrl/files/1184.txt"))
          }
        )
      }
      val body = JSONObject().put("count", results.length()).put("next", JSONObject.NULL).put("results", results).toString()
      val bytes = body.toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }

    server.createContext("/librivox") { exchange ->
      if (shouldFailNetwork) {
        exchange.sendResponseHeaders(503, -1)
        exchange.close()
        return@createContext
      }
      val body = JSONObject().put("books", JSONArray()).toString()
      val bytes = body.toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }

    server.start()
    httpServer = server
    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$baseUrl/books"
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$baseUrl/librivox"

    // 1. Network failure reported cleanly so UI can offer Retry
    val failedAttempt = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "Count of Monte Cristo",
      languageCode = "en"
    )
    assertNotNull("Must report errorMessage when remote APIs fail", failedAttempt.errorMessage)

    // 2. Recover when user taps Retry and find remote book not in the local 23 curated list
    shouldFailNetwork = false
    val retryAttempt = GutendexAndLibriVoxService.searchCatalogPaginated(
      query = "Count of Monte Cristo",
      languageCode = "en"
    )
    assertTrue("Retry must clear errorMessage", retryAttempt.errorMessage == null)
    assertTrue(
      "Remote search must find 'The Count of Monte Cristo' (Gutenberg #1184)",
      retryAttempt.books.any { it.gutendexId == 1184 && it.title.contains("Count of Monte Cristo") }
    )
  }

  @Test
  fun catalogSessionStatePreservesLanguageFilterAndScrollPositionAndAdMobRewardedTrialEnforcesRules() {
    // 1. Verify AudiobooksCatalogSessionState preserves loaded books, language filter, and scroll position when opening a book and returning
    val sessionState = com.example.ui.components.AudiobooksCatalogSessionState()
    sessionState.selectedLanguageCode = "en"
    sessionState.savedScrollIndex = 14
    sessionState.savedScrollOffset = 128

    var selectedBookFromCatalog: AudiobookBook? = null
    val enLang = Language.findByCode("en")

    composeTestRule.setContent {
      LingoSphereTheme {
        AudiobooksCategoryScreen(
          userId = "u_scroll_restore",
          targetLang = enLang,
          selectedCefrLevel = "B1",
          sessionState = sessionState,
          onSelectBook = { book -> selectedBookFromCatalog = book },
          onOpenSearch = {},
          onOpenCreateListening = {},
          onBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    assertEquals("en", sessionState.selectedLanguageCode)
    assertTrue(
      "English catalog must show 'A Room with a View'",
      composeTestRule.onAllNodesWithText("A Room with a View", substring = true).fetchSemanticsNodes().isNotEmpty()
    )

    composeTestRule.onNodeWithTag("book_item_gutenberg_2641").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertNotNull("Clicking A Room with a View must invoke onSelectBook", selectedBookFromCatalog)
    assertEquals("gutenberg_2641", selectedBookFromCatalog?.id)

    // 2. Verify AdConfig test vs production IDs
    com.example.ads.AdService.resetForTesting()
    assertTrue("USE_TEST_ADS must default to true", com.example.ads.AdConfig.USE_TEST_ADS)
    assertEquals("ca-app-pub-3940256099942544/5224354917", com.example.ads.AdConfig.rewardedAdUnitId)
    com.example.ads.AdConfig.USE_TEST_ADS = false
    assertEquals("ca-app-pub-8471293855339658~7525338937", com.example.ads.AdConfig.appId)
    assertEquals("ca-app-pub-8471293855339658/1933847310", com.example.ads.AdConfig.rewardedAdUnitId)
    com.example.ads.AdConfig.USE_TEST_ADS = true

    // 3. Verify Pro users never load or see ads
    val proUser = com.example.data.model.UserProfile(
      userId = "u_pro_ad_test",
      subscriptionTier = "premium_monthly",
      purchaseToken = "gp_verified_pro_token"
    )
    com.example.ads.AdService.initialize(context = null, userProfile = proUser)
    assertTrue(com.example.ads.AdService.state.value.isUserVerifiedPro)
    assertFalse("Pro user must never load banner ads", com.example.ads.AdService.state.value.bannerLoaded)
    assertFalse("Pro user must never load rewarded ads", com.example.ads.AdService.state.value.rewardedLoaded)

    // 4. Verify Free user rewarded trial unlock happens ONLY in reward callback, never on early close or load failure
    val freeUser = com.example.data.model.UserProfile(
      userId = "u_free_ad_test",
      subscriptionTier = "free"
    )
    com.example.ads.AdService.initialize(context = null, userProfile = freeUser)
    assertFalse(com.example.ads.AdService.state.value.isUserVerifiedPro)
    assertTrue("Free user must preload rewarded ad", com.example.ads.AdService.state.value.rewardedLoaded)

    // Early close -> zero reward
    var rewardCallbackCount = 0
    var earlyClosedCount = 0
    com.example.ads.AdService.showRewardedAdForFeatureUnlock(
      userProfile = freeUser,
      featureType = com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA,
      onUserEarnedReward = { rewardCallbackCount++ },
      onDismissedWithoutReward = { earlyClosedCount++ }
    )
    assertEquals("Must not grant reward before ad completes", 0, rewardCallbackCount)
    com.example.ads.AdService.dismissActiveRewardedAdEarly()
    assertEquals("Closing ad early must give no reward", 0, rewardCallbackCount)
    assertEquals(1, earlyClosedCount)
    assertFalse(com.example.ads.AdService.hasActiveRewardedUnlock(com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA))

    // Ad load failure -> error message, never unlock for free
    com.example.ads.AdService.simulateAdLoadFailure = true
    var loadFailedMessage: String? = null
    com.example.ads.AdService.showRewardedAdForFeatureUnlock(
      userProfile = freeUser,
      featureType = com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA,
      onUserEarnedReward = { rewardCallbackCount++ },
      onAdFailedToLoad = { msg -> loadFailedMessage = msg }
    )
    assertEquals("No ad available right now, try again in a moment", loadFailedMessage)
    assertEquals("Failed ad must never unlock for free", 0, rewardCallbackCount)
    com.example.ads.AdService.simulateAdLoadFailure = false
    com.example.ads.AdService.retryLoadRewardedAd(freeUser)

    // Full watch -> reward callback fires and grants 5-minute session (up to 5/day limit)
    for (attempt in 1..5) {
      com.example.ads.AdService.showRewardedAdForFeatureUnlock(
        userProfile = freeUser,
        featureType = com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA,
        onUserEarnedReward = { rewardCallbackCount++ }
      )
      com.example.ads.AdService.completeActiveRewardedAdWithReward()
    }
    assertEquals(5, rewardCallbackCount)
    assertTrue(com.example.ads.AdService.hasActiveRewardedUnlock(com.example.ads.RewardedFeatureType.AI_TUTOR_EMMA))
    assertTrue("5th unlock must hit daily cap of 5/day", com.example.ads.AdService.state.value.isDailyLimitReached)

    // 6th attempt in same day is blocked with daily limit message
    var sixthAttemptError: String? = null
    com.example.ads.AdService.showRewardedAdForFeatureUnlock(
      userProfile = freeUser,
      featureType = com.example.ads.RewardedFeatureType.SPEAK,
      onUserEarnedReward = { rewardCallbackCount++ },
      onAdFailedToLoad = { msg -> sixthAttemptError = msg }
    )
    assertEquals("Daily free limit reached, go Pro for unlimited", sixthAttemptError)
    assertEquals(5, rewardCallbackCount)
    com.example.ads.AdService.resetForTesting()
  }

  @Test
  fun verifyShouldShowAdsCentralGateAndHomeAndListeningAdPlacements() {
    com.example.ads.AdService.resetForTesting()

    val freeUser = com.example.data.model.UserProfile(
      userId = "u_free_verified_01",
      subscriptionTier = "free"
    )
    val proMonthlyUser = com.example.data.model.UserProfile(
      userId = "u_pro_monthly_01",
      subscriptionTier = "premium_monthly",
      purchaseToken = "gp_verified_monthly_token",
      isSubscriptionExpired = false
    )
    val proYearlyUser = com.example.data.model.UserProfile(
      userId = "u_pro_yearly_01",
      subscriptionTier = "premium_yearly",
      purchaseToken = "gp_verified_yearly_token",
      isSubscriptionExpired = false
    )
    val expiredProUser = com.example.data.model.UserProfile(
      userId = "u_pro_expired_01",
      subscriptionTier = "premium_monthly",
      purchaseToken = "gp_expired_token",
      isSubscriptionExpired = true
    )
    val guestUnauthenticatedUser = com.example.data.model.UserProfile(
      userId = "guest",
      subscriptionTier = "free"
    )

    // 1. Unauthenticated / guest user -> shouldShowAds() is FALSE
    com.example.ads.AdService.initialize(context = null, userProfile = guestUnauthenticatedUser)
    assertFalse(
      "Unauthenticated/guest user must not pass shouldShowAds()",
      com.example.ads.AdService.shouldShowAds(guestUnauthenticatedUser)
    )

    // 2. Logged-in verified Free account -> shouldShowAds() is TRUE and test ads load
    com.example.ads.AdService.initialize(context = null, userProfile = freeUser)
    assertTrue(
      "Logged-in verified Free user must see test ads",
      com.example.ads.AdService.shouldShowAds(freeUser)
    )
    assertTrue(com.example.ads.AdService.state.value.bannerLoaded)
    assertTrue(com.example.ads.AdService.state.value.nativeLoaded)
    assertEquals("ca-app-pub-3940256099942544/2247696110", com.example.ads.AdConfig.nativeAdvancedAdUnitId)
    assertEquals("ca-app-pub-3940256099942544/1033173712", com.example.ads.AdConfig.interstitialAdUnitId)

    // 3. Offline or unverified/loading subscription status -> shouldShowAds() is FALSE (no ads)
    com.example.ads.AdService.setSubscriptionVerificationStatus(
      isLoading = true,
      isVerified = false,
      userProfile = freeUser
    )
    assertFalse(
      "While subscription status is loading/unverified, shouldShowAds() must be false",
      com.example.ads.AdService.shouldShowAds(freeUser)
    )
    assertFalse("Must not flash banner while unverified", com.example.ads.AdService.state.value.bannerLoaded)

    // Restore verified online status
    com.example.ads.AdService.setSubscriptionVerificationStatus(
      isLoading = false,
      isVerified = true,
      isOffline = false,
      userProfile = freeUser
    )
    assertTrue(com.example.ads.AdService.shouldShowAds(freeUser))

    // Simulate offline state
    com.example.ads.AdService.setSubscriptionVerificationStatus(
      isLoading = false,
      isVerified = true,
      isOffline = true,
      userProfile = freeUser
    )
    assertFalse(
      "Offline user must not see ads when subscription cannot be verified online",
      com.example.ads.AdService.shouldShowAds(freeUser)
    )
    com.example.ads.AdService.setSubscriptionVerificationStatus(
      isLoading = false,
      isVerified = true,
      isOffline = false,
      userProfile = freeUser
    )
    assertTrue(com.example.ads.AdService.shouldShowAds(freeUser))

    // 4. Real-time upgrade: Free user upgrades to Pro -> all ads destroyed immediately
    com.example.ads.AdService.onUserProfileUpdated(proMonthlyUser)
    assertFalse(
      "Pro monthly user must NEVER pass shouldShowAds()",
      com.example.ads.AdService.shouldShowAds(proMonthlyUser)
    )
    assertFalse(
      "Pro yearly user must NEVER pass shouldShowAds()",
      com.example.ads.AdService.shouldShowAds(proYearlyUser)
    )
    assertFalse("Banner ad must be destroyed immediately on upgrade", com.example.ads.AdService.state.value.bannerLoaded)
    assertFalse("Native ad must be destroyed immediately on upgrade", com.example.ads.AdService.state.value.nativeLoaded)
    assertFalse("Interstitial ad must be cleared on upgrade", com.example.ads.AdService.state.value.interstitialLoaded)
    assertFalse("Rewarded ad must be cleared on upgrade", com.example.ads.AdService.state.value.rewardedLoaded)
    assertFalse("App open ad must be cleared on upgrade", com.example.ads.AdService.state.value.appOpenLoaded)

    // Returning from background as Pro user shows zero ads
    com.example.ads.AdService.onAppReturnedFromBackground(
      userProfile = proMonthlyUser,
      isInRestrictedActivity = false
    )
    assertFalse(com.example.ads.AdService.shouldShowAds(proMonthlyUser))
    assertTrue(com.example.ads.AdService.state.value.activeFullScreenAd == null)

    // 5. Pro user with expired subscription -> ads come back
    com.example.ads.AdService.onUserProfileUpdated(expiredProUser)
    assertTrue(
      "Expired Pro subscription must revert to Free and show ads again",
      com.example.ads.AdService.shouldShowAds(expiredProUser)
    )
    assertTrue(com.example.ads.AdService.state.value.bannerLoaded)
    assertTrue(com.example.ads.AdService.state.value.nativeLoaded)

    // 6. UI verification: Book Library native ad every 8 books for Free user, collapsed on failure, and zero ads for Pro user
    val sessionState = com.example.ui.components.AudiobooksCatalogSessionState().apply {
      selectedLanguageCode = "All"
    }
    var currentTestUser by androidx.compose.runtime.mutableStateOf(freeUser)
    com.example.ads.AdService.onUserProfileUpdated(freeUser)

    composeTestRule.setContent {
      LingoSphereTheme {
        AudiobooksCategoryScreen(
          userId = currentTestUser.userId,
          userProfile = currentTestUser,
          targetLang = Language.findByCode("en"),
          selectedCefrLevel = "B1",
          sessionState = sessionState,
          onSelectBook = {},
          onOpenSearch = {},
          onOpenCreateListening = {},
          onBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Scroll to the 8th book (index 7 -> slotIndex 1) and verify admob_book_native_card_1 exists for Free user
    val eighthBookId = AudiobookCatalogRepository.CURATED_BOOKS[7].id
    composeTestRule.onNodeWithTag("book_item_$eighthBookId").performScrollTo()
    composeTestRule.waitForIdle()
    assertTrue(
      "Free user must see native book ad card after 8th book",
      composeTestRule.onAllNodesWithTag("admob_book_native_card_1").fetchSemanticsNodes().isNotEmpty()
    )
    // Verify no bottom banner stacks on the Book Library screen
    assertTrue(
      "Book Library screen must NOT show bottom banner so it does not stack with native ads",
      composeTestRule.onAllNodesWithTag("admob_banner_library").fetchSemanticsNodes().isEmpty()
    )

    // When ad fails to load, space collapses completely (zero native ad nodes)
    com.example.ads.AdService.simulateAdLoadFailure = true
    composeTestRule.waitForIdle()
    assertTrue(
      "Failed native ad must collapse space completely",
      composeTestRule.onAllNodesWithTag("admob_book_native_card_1").fetchSemanticsNodes().isEmpty()
    )
    com.example.ads.AdService.simulateAdLoadFailure = false
    com.example.ads.AdService.onUserProfileUpdated(freeUser)
    composeTestRule.waitForIdle()

    // Upgrade to Pro in real time -> native ad card disappears immediately from book list
    currentTestUser = proMonthlyUser
    com.example.ads.AdService.onUserProfileUpdated(proMonthlyUser)
    composeTestRule.waitForIdle()
    assertTrue(
      "Pro user must see ZERO ads in the Book Library list",
      composeTestRule.onAllNodesWithTag("admob_book_native_card_1").fetchSemanticsNodes().isEmpty()
    )

    // 7. Verify App Version 32 & Play & Learn (Games) Ad Rules:
    // - versionCode == 32, versionName == "32.0"
    assertEquals(32, BuildConfig.VERSION_CODE)
    assertEquals("32.0", BuildConfig.VERSION_NAME)

    // - Interstitial in Play & Learn: ONLY after every 3rd completed game before results screen,
    //   at most once every 3 minutes and max 3 per session; never for Pro users.
    com.example.ads.AdService.resetForTesting()
    com.example.ads.AdService.initialize(context = null, userProfile = freeUser)

    var resultsReachedCount = 0
    // Game #1 -> no interstitial, continues immediately to results
    val game1Shown = com.example.ads.AdService.onGameCompletedBeforeResults(
      userProfile = freeUser,
      gameTitle = "Wordigo Level 1"
    ) { resultsReachedCount++ }
    assertFalse("1st completed game must NOT show interstitial", game1Shown)
    assertEquals(1, resultsReachedCount)
    assertEquals(1, com.example.ads.AdService.state.value.completedGamesInSessionCount)

    // Game #2 -> no interstitial, continues immediately to results
    val game2Shown = com.example.ads.AdService.onGameCompletedBeforeResults(
      userProfile = freeUser,
      gameTitle = "SpellBee Level 1"
    ) { resultsReachedCount++ }
    assertFalse("2nd completed game must NOT show interstitial", game2Shown)
    assertEquals(2, resultsReachedCount)
    assertEquals(2, com.example.ads.AdService.state.value.completedGamesInSessionCount)

    // Game #3 -> 3rd completed game triggers interstitial BEFORE results screen
    val game3Shown = com.example.ads.AdService.onGameCompletedBeforeResults(
      userProfile = freeUser,
      gameTitle = "Vocab Master Level 1"
    ) { resultsReachedCount++ }
    assertTrue("3rd completed game MUST trigger interstitial before results", game3Shown)
    assertEquals("Results callback waits until interstitial is dismissed", 2, resultsReachedCount)
    com.example.ads.AdService.dismissActiveFullScreenAd()
    assertEquals("Dismissing interstitial proceeds to results screen", 3, resultsReachedCount)
    assertEquals(1, com.example.ads.AdService.state.value.interstitialSessionCount)

    // Games #4, #5, #6 within 3 minutes -> 6th game is blocked by 3-minute cooldown and continues immediately
    com.example.ads.AdService.onGameCompletedBeforeResults(freeUser, "Game 4") { resultsReachedCount++ }
    com.example.ads.AdService.onGameCompletedBeforeResults(freeUser, "Game 5") { resultsReachedCount++ }
    val game6WithinCooldown = com.example.ads.AdService.onGameCompletedBeforeResults(freeUser, "Game 6") { resultsReachedCount++ }
    assertFalse("6th game within 3-minute cooldown must NOT show interstitial", game6WithinCooldown)
    assertEquals(6, resultsReachedCount)

    // Pro user completing 3 games NEVER triggers an interstitial
    com.example.ads.AdService.onUserProfileUpdated(proMonthlyUser)
    var proResultsCount = 0
    repeat(3) { idx ->
      val shownForPro = com.example.ads.AdService.onGameCompletedBeforeResults(
        userProfile = proMonthlyUser,
        gameTitle = "Pro Game ${idx + 1}"
      ) { proResultsCount++ }
      assertFalse("Pro user must NEVER see interstitial after any game", shownForPro)
    }
    assertEquals(3, proResultsCount)

    // - Rewarded ads in Play & Learn (Hint, Continue/Second Chance, Double XP):
    //   Grant reward ONLY in reward callback, max 5 per day, never for Pro users.
    com.example.ads.AdService.resetForTesting()
    com.example.ads.AdService.initialize(context = null, userProfile = freeUser)

    var hintGranted = false
    var hintCancelledReason: String? = null
    val hintAdStarted = com.example.ads.AdService.showRewardedAdForGameBonus(
      userProfile = freeUser,
      rewardType = com.example.ads.RewardedFeatureType.GAME_HINT,
      itemId = "brain_game_lvl1_q1",
      onRewardEarned = { hintGranted = true },
      onFailedOrCancelled = { reason -> hintCancelledReason = reason }
    )
    assertTrue("Rewarded hint ad must start for Free user", hintAdStarted)
    assertFalse("Hint must NOT be granted before reward callback fires", hintGranted)
    com.example.ads.AdService.dismissActiveRewardedAdEarly()
    assertFalse("Early dismissal must NOT grant hint", hintGranted)
    assertNotNull(hintCancelledReason)

    // Watching full ad grants hint in callback
    com.example.ads.AdService.showRewardedAdForGameBonus(
      userProfile = freeUser,
      rewardType = com.example.ads.RewardedFeatureType.GAME_HINT,
      itemId = "brain_game_lvl1_q1",
      onRewardEarned = { hintGranted = true }
    )
    com.example.ads.AdService.completeActiveRewardedAdWithReward()
    assertTrue("Watching full rewarded ad MUST grant hint in callback", hintGranted)

    // Pro user calling showRewardedAdForGameBonus never loads or shows an ad
    var proHintAdStarted = false
    proHintAdStarted = com.example.ads.AdService.showRewardedAdForGameBonus(
      userProfile = proMonthlyUser,
      rewardType = com.example.ads.RewardedFeatureType.GAME_HINT,
      itemId = "pro_hint",
      onRewardEarned = {}
    )
    assertFalse("Pro user must never start a rewarded ad", proHintAdStarted)

    com.example.ads.AdService.resetForTesting()
  }
}

