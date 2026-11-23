package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.BookContentType
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.data.model.UserAudiobookProgress
import com.example.data.repository.BookDownloadFailureReason
import com.example.data.repository.BookDownloadProgress
import com.example.data.repository.GutendexAndLibriVoxService
import com.example.speech.AudiobookMediaController
import com.example.speech.NarrationTextCleaner
import com.example.speech.SpeechManager
import com.example.ui.components.BookDetailScreen
import com.example.ui.theme.LingoSphereTheme
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-mdpi")
class ListeningBookDownloadAndAudioVerificationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var speechManager: SpeechManager
  private val activeServers = mutableListOf<HttpServer>()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    speechManager = SpeechManager(context)
    GutendexAndLibriVoxService.resetTestingOverrides()
    GutendexAndLibriVoxService.clearAllCaches()
    File(context.filesDir, "gutenberg_fulltext").deleteRecursively()
    AudiobookMediaController.stopAndClearMiniPlayer()
    AudiobookMediaController.attach(context, speechManager, "test_listening_user")
    GutendexAndLibriVoxService.httpClientOverride = OkHttpClient.Builder()
      .connectTimeout(3, TimeUnit.SECONDS)
      .readTimeout(4, TimeUnit.SECONDS)
      .writeTimeout(4, TimeUnit.SECONDS)
      .callTimeout(8, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()
  }

  @After
  fun tearDown() {
    AudiobookMediaController.stopAndClearMiniPlayer()
    GutendexAndLibriVoxService.resetTestingOverrides()
    GutendexAndLibriVoxService.clearAllCaches()
    File(context.filesDir, "gutenberg_fulltext").deleteRecursively()
    activeServers.forEach {
      try { it.stop(0) } catch (_: Throwable) {}
    }
    activeServers.clear()
    speechManager.release()
  }

  private fun startServer(configure: (HttpServer, String) -> Unit): Pair<HttpServer, String> {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val baseUrl = "http://127.0.0.1:${server.address.port}"
    configure(server, baseUrl)
    server.start()
    activeServers.add(server)
    return server to baseUrl
  }

  private fun buildRoomWithAViewFullGutenbergText(): String {
    val chapterSpecs = listOf(
      "PART ONE" to listOf(
        "Chapter I" to "The Bertolini",
        "Chapter II" to "In Santa Croce with No Baedeker",
        "Chapter III" to "Music, Violets, and the Letter “S”",
        "Chapter IV" to "Fourth Chapter",
        "Chapter V" to "Possibilities of a Pleasant Outing",
        "Chapter VI" to "The Reverend Arthur Beebe, the Reverend Cuthbert Eager, Mr.\nEmerson, Mr. George Emerson, Miss Eleanor Lavish, Miss Charlotte\nBartlett, and Miss Lucy Honeychurch Drive Out in Carriages to See\na View; Italians Drive Them",
        "Chapter VII" to "They Return"
      ),
      "PART TWO" to listOf(
        "Chapter VIII" to "Medieval",
        "Chapter IX" to "Lucy As a Work of Art",
        "Chapter X" to "Cecil as a Humourist",
        "Chapter XI" to "In Mrs. Vyse’s Well-Appointed Flat",
        "Chapter XII" to "Twelfth Chapter",
        "Chapter XIII" to "How Miss Bartlett’s Boiler\nWas So Tiresome",
        "Chapter XIV" to "How Lucy Faced the External Situation Bravely",
        "Chapter XV" to "The Disaster Within",
        "Chapter XVI" to "Lying to George",
        "Chapter XVII" to "Lying to Cecil",
        "Chapter XVIII" to "Lying to Mr. Beebe, Mrs. Honeychurch,\nFreddy, and the Servants",
        "Chapter XIX" to "Lying to Mr. Emerson",
        "Chapter XX" to "The End of the Middle Ages"
      )
    )

    val sb = StringBuilder()
    sb.append("*** START OF THE PROJECT GUTENBERG EBOOK A ROOM WITH A VIEW ***\n\n")
    sb.append("A ROOM WITH A VIEW\n\nby E. M. Forster\n\nContents\n\n")
    chapterSpecs.forEach { (part, chs) ->
      sb.append("$part\n")
      chs.forEach { (num, sub) ->
        sb.append(" $num. ${sub.replace("\n", " ")}\n")
      }
      sb.append("\n")
    }
    var ordinal = 1
    chapterSpecs.forEach { (part, chs) ->
      sb.append("\n$part\n\n")
      chs.forEach { (num, sub) ->
        sb.append("\n$num\n$sub\n\n")
        val uniqueSample = if (ordinal == 1) {
          "“The Signora had no business to do it,” said Miss Bartlett, “no business at all. She promised us south rooms with a view close together, instead of which here are north rooms, looking into a courtyard, and a long way apart.” "
        } else if (ordinal == 2) {
          "It was pleasant to wake up in Florence, to open the eyes upon a bright bare room, with a floor of red tiles which look clean though they are not. "
        } else {
          "Lucy walked across the terrace as the Italian sunshine warmed the valley and Mr. Beebe observed the company carefully. "
        }
        sb.append(uniqueSample)
        sb.append("In section $ordinal of A Room with a View by E. M. Forster, the guests reflect on Florence, Windy Corner, truth, music, and society with lively dialogue and clear observation.\n\n")
        ordinal++
      }
    }
    sb.append("\n*** END OF THE PROJECT GUTENBERG EBOOK A ROOM WITH A VIEW ***\n")
    return sb.toString()
  }

  private fun buildRoomWithAViewLibriVoxJson(baseUrl: String): String {
    val sections = JSONArray()
    for (i in 1..20) {
      val padded = i.toString().padStart(2, '0')
      sections.put(
        JSONObject().apply {
          put("id", "${93800 + i}")
          put("section_number", i.toString())
          put("title", "Chapter $padded")
          put("listen_url", "$baseUrl/audio/roomwithaview_${padded}_forster.mp3")
          put("playtime", (420 + i * 15).toString())
          put("readers", JSONArray().put(JSONObject().put("display_name", "Elizabeth Klett")))
        }
      )
    }
    val bookObj = JSONObject().apply {
      put("id", "938")
      put("title", "Room with a View") // Notice omitted leading "A " just like real LibriVox
      put("language", "English")
      put("totaltimesecs", "12400")
      put("totaltime", "3:26:40")
      put("url_iarchive", "$baseUrl/archive/room_view_938")
      put("authors", JSONArray().put(JSONObject().apply {
        put("first_name", "E. M.")
        put("last_name", "Forster")
      }))
      put("sections", sections)
    }
    return JSONObject().put("books", JSONArray().put(bookObj)).toString()
  }

  private fun createRoomWithAViewSeedBook(id: String = "gutenberg_2641_test"): AudiobookBook {
    return AudiobookBook(
      id = id,
      gutendexId = 2641,
      title = "A Room with a View",
      author = "E. M. Forster",
      primaryCategory = "Classic Novels",
      subCategory = "British Classics",
      shortDescription = "E. M. Forster's classic novel set in Florence and Surrey.",
      targetLanguageCode = "en",
      defaultCefrLevel = "B2",
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      hasFullTextAvailable = false,
      fullTextUrl = null
    )
  }

  @Test
  fun roomWithAView_redirectsThroughMirrors_parsesAll20Chapters_andMatchesLibriVoxWithoutLeadingArticle() = runBlocking {
    val bookText = buildRoomWithAViewFullGutenbergText()
    val bookBytes = bookText.toByteArray(Charsets.UTF_8)

    val (_, targetHost) = startServer { server, base ->
      server.createContext("/cache/epub/2641/pg2641.txt") { ex ->
        ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
        ex.sendResponseHeaders(200, bookBytes.size.toLong())
        ex.responseBody.use { it.write(bookBytes) }
      }
      server.createContext("/api/feed/audiobooks/") { ex ->
        val json = buildRoomWithAViewLibriVoxJson(base).toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, json.size.toLong())
        ex.responseBody.use { it.write(json) }
      }
      server.createContext("/api/feed/audiotracks") { ex ->
        val json = buildRoomWithAViewLibriVoxJson(base).toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, json.size.toLong())
        ex.responseBody.use { it.write(json) }
      }
    }

    // Mirror 1 always returns 503; Mirror 2 issues a manual 302 redirect to targetHost
    val (_, unhealthyMirror) = startServer { server, _ ->
      server.createContext("/") { ex ->
        ex.sendResponseHeaders(503, 0)
        ex.responseBody.close()
      }
    }
    val (_, redirectMirror) = startServer { server, _ ->
      server.createContext("/") { ex ->
        val path = ex.requestURI.rawPath
        ex.responseHeaders.add("Location", "$targetHost$path")
        ex.sendResponseHeaders(302, -1)
        ex.close()
      }
    }

    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$targetHost/books"
    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = listOf(unhealthyMirror, redirectMirror)
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$targetHost/api/feed/audiobooks/"
    GutendexAndLibriVoxService.librivoxAudiotracksUrlOverride = "$targetHost/api/feed/audiotracks"

    val seedBook = createRoomWithAViewSeedBook()
    val progressEvents = mutableListOf<BookDownloadProgress>()
    val textRes = GutendexAndLibriVoxService.downloadBookTextOnly(
      context = context,
      book = seedBook,
      priorityChapterIndex = 5,
      onProgress = { progressEvents.add(it) }
    )

    assertEquals(BookDownloadFailureReason.NONE, textRes.failureReason)
    assertNull(textRes.errorMessage)
    assertEquals(20, textRes.chapters.size)
    assertEquals(1, textRes.chapters[0].chapterNumber)
    assertTrue(textRes.chapters[0].title.contains("The Bertolini", ignoreCase = true))
    assertTrue(textRes.chapters[5].title.contains("Drive Out in Carriages", ignoreCase = true))
    assertTrue(textRes.chapters[12].title.contains("Boiler Was So Tiresome", ignoreCase = true))
    assertTrue(textRes.chapters[17].title.contains("Freddy, and the Servants", ignoreCase = true))
    assertTrue(textRes.chapters[19].title.contains("End of the Middle Ages", ignoreCase = true))
    assertEquals(targetHost + "/cache/epub/2641/pg2641.txt", textRes.finalUrl)
    assertTrue(progressEvents.isNotEmpty())
    assertTrue(progressEvents.last().downloadedBytes == bookBytes.size.toLong())

    // Verify telemetry logs contain requested URL, final redirected URL, HTTP status, size, and duration
    val logs = GutendexAndLibriVoxService.recentDownloadLogs.joinToString("\n")
    assertTrue("Expected 302 redirect log in:\n$logs", logs.contains("Redirect hop #1 (302)"))
    assertTrue("Expected status=200 log in:\n$logs", logs.contains("status=200"))
    assertTrue("Expected hops=1 log in:\n$logs", logs.contains("hops=1"))
    assertTrue("Expected size log in:\n$logs", logs.contains("size=${bookBytes.size} bytes"))

    // Verify independent LibriVox audio discovery matches "Room with a View" (without leading "A ")
    val audioRes = GutendexAndLibriVoxService.fetchBookAudioOnly(textRes.book)
    assertEquals(BookDownloadFailureReason.NONE, audioRes.failureReason)
    assertEquals(1, audioRes.editions.size)
    assertEquals(20, audioRes.editions.first().sections.size)
    assertTrue(audioRes.book.hasVerifiedHumanAudio)
    assertTrue(audioRes.book.hasVerifiedFullText)
    assertTrue(audioRes.book.isCompleteBookAndAudioVerified)

    // Subsequent call should hit cache cleanly
    val cachedRes = GutendexAndLibriVoxService.downloadBookTextOnly(context, seedBook, 0)
    assertTrue(cachedRes.isFromCache)
    assertEquals(20, cachedRes.chapters.size)
  }

  @Test
  fun epubAndHtmlFallbacks_extractAndParseCleanChapters_andDecodeEntities() = runBlocking {
    // 1. Build a valid minimal EPUB ZIP on disk/memory
    val xhtmlBody = """
      <?xml version="1.0" encoding="UTF-8"?>
      <html><head><title>A Tale</title></head><body>
      <h1>Chapter I</h1>
      <h2>Dawn over Paris &#8212; &ldquo;Hope&rdquo;</h2>
      <p>It was the best of times, it was the worst of times in &lsquo;London&rsquo; and Paris.</p>
      <p>${"Morning mist drifted above the slate roofs while carriages rolled slowly down the avenue. ".repeat(14)}</p>
      <h1>Chapter II</h1>
      <h2>The Mail Coach</h2>
      <p>Jerry Cruncher rode up Shooter&#39;s Hill with letters sealed in red wax.</p>
      <p>${"The horses steamed in the chill autumn night air as lanterns swung along the road. ".repeat(14)}</p>
      </body></html>
    """.trimIndent()

    val epubBytes = ByteArrayOutputStream().use { baos ->
      ZipOutputStream(baos).use { zip ->
        zip.putNextEntry(ZipEntry("OEBPS/text.xhtml"))
        zip.write(xhtmlBody.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }
      baos.toByteArray()
    }

    val htmlFallback = """
      <!DOCTYPE html><html><body>
      <h2>CHAPTER 1</h2>
      <p>${"First chapter opening in the garden where Elizabeth read her sister&rsquo;s letter carefully. ".repeat(14)}</p>
      <h2>CHAPTER 2</h2>
      <p>${"Second chapter afternoon tea with unexpected guests arriving along the gravel drive. ".repeat(14)}</p>
      </body></html>
    """.trimIndent().toByteArray(Charsets.UTF_8)

    val (_, serverUrl) = startServer { server, _ ->
      server.createContext("/book.txt") { ex ->
        ex.sendResponseHeaders(404, 0)
        ex.responseBody.close()
      }
      server.createContext("/book.epub") { ex ->
        ex.responseHeaders.add("Content-Type", "application/epub+zip")
        ex.sendResponseHeaders(200, epubBytes.size.toLong())
        ex.responseBody.use { it.write(epubBytes) }
      }
      server.createContext("/book.html") { ex ->
        ex.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        ex.sendResponseHeaders(200, htmlFallback.size.toLong())
        ex.responseBody.use { it.write(htmlFallback) }
      }
    }

    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = emptyList()

    val epubBook = AudiobookBook(
      id = "epub_test_book",
      gutendexId = 999901,
      title = "A Tale",
      author = "Charles Dickens",
      primaryCategory = "Classic Novels",
      subCategory = "Historical",
      shortDescription = "Classic historical novel.",
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      hasFullTextAvailable = true,
      fullTextUrl = "$serverUrl/book.txt",
      formats = mapOf(
        "text/plain; charset=utf-8" to "$serverUrl/book.txt",
        "application/epub+zip" to "$serverUrl/book.epub"
      )
    )

    val epubRes = GutendexAndLibriVoxService.downloadBookTextOnly(context, epubBook, 0)
    assertEquals(BookDownloadFailureReason.NONE, epubRes.failureReason)
    assertEquals(2, epubRes.chapters.size)
    assertTrue(epubRes.chapters[0].fullText.contains("“Hope”"))
    assertTrue(epubRes.chapters[0].fullText.contains("—"))
    assertTrue(epubRes.chapters[1].fullText.contains("Shooter's Hill"))

    val htmlBook = AudiobookBook(
      id = "html_test_book",
      gutendexId = 999902,
      title = "Garden Letters",
      author = "Jane Austen",
      primaryCategory = "Classic Novels",
      subCategory = "Romance",
      shortDescription = "Classic letters.",
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      hasFullTextAvailable = true,
      formats = mapOf("text/html" to "$serverUrl/book.html")
    )

    val htmlRes = GutendexAndLibriVoxService.downloadBookTextOnly(context, htmlBook, 0)
    assertEquals(BookDownloadFailureReason.NONE, htmlRes.failureReason)
    assertEquals(2, htmlRes.chapters.size)
    assertTrue(htmlRes.chapters[0].fullText.contains("sister’s letter"))
  }

  @Test
  fun partialDownload_sendsRangeHeaderOnlyWhenServerSupportsAcceptRangesBytes() = runBlocking {
    val body = buildRoomWithAViewFullGutenbergText().toByteArray(Charsets.UTF_8)
    val firstAttemptRangeSeen = AtomicBoolean(false)
    val secondAttemptRangeHeader = mutableListOf<String?>()
    val rangeCalls = AtomicInteger(0)

    val (_, rangeServer) = startServer { server, _ ->
      server.createContext("/cache/epub/2641/pg2641.txt") { ex ->
        val call = rangeCalls.incrementAndGet()
        val reqRange = ex.requestHeaders.getFirst("Range")
        if (call > 1) secondAttemptRangeHeader.add(reqRange)
        if (reqRange == null && call == 1) {
          // Advertise Accept-Ranges: bytes, send half, then close connection abruptly
          ex.responseHeaders.add("Accept-Ranges", "bytes")
          ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
          val half = body.size / 2
          ex.sendResponseHeaders(200, body.size.toLong())
          ex.responseBody.write(body, 0, half)
          ex.responseBody.flush()
          ex.close()
        } else if (reqRange != null && reqRange.startsWith("bytes=")) {
          firstAttemptRangeSeen.set(true)
          val start = reqRange.removePrefix("bytes=").substringBefore("-").toIntOrNull() ?: 0
          val slice = body.copyOfRange(start, body.size)
          ex.responseHeaders.add("Accept-Ranges", "bytes")
          ex.responseHeaders.add("Content-Range", "bytes $start-${body.size - 1}/${body.size}")
          ex.sendResponseHeaders(206, slice.size.toLong())
          ex.responseBody.use { it.write(slice) }
        } else {
          ex.sendResponseHeaders(200, body.size.toLong())
          ex.responseBody.use { it.write(body) }
        }
      }
    }

    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = listOf(rangeServer)
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$rangeServer/no_lv"
    val res1 = GutendexAndLibriVoxService.downloadBookTextOnly(context, createRoomWithAViewSeedBook("range_yes_2641"), 0)
    assertEquals(BookDownloadFailureReason.NONE, res1.failureReason)
    assertTrue("Expected Range resume header when server supports Accept-Ranges: bytes", firstAttemptRangeSeen.get())
    assertEquals(20, res1.chapters.size)

    // Now test a server WITHOUT Accept-Ranges: bytes
    GutendexAndLibriVoxService.clearAllCaches()
    val noRangeCalls = AtomicInteger(0)
    val noRangeSawHeader = AtomicBoolean(false)
    val (_, noRangeServer) = startServer { server, _ ->
      server.createContext("/cache/epub/2641/pg2641.txt") { ex ->
        val call = noRangeCalls.incrementAndGet()
        val reqRange = ex.requestHeaders.getFirst("Range")
        if (reqRange != null) noRangeSawHeader.set(true)
        if (call == 1) {
          // Do NOT set Accept-Ranges
          ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
          val half = body.size / 2
          ex.sendResponseHeaders(200, body.size.toLong())
          ex.responseBody.write(body, 0, half)
          ex.responseBody.flush()
          ex.close()
        } else {
          ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
          ex.sendResponseHeaders(200, body.size.toLong())
          ex.responseBody.use { it.write(body) }
        }
      }
    }
    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = listOf(noRangeServer)
    val res2 = GutendexAndLibriVoxService.downloadBookTextOnly(context, createRoomWithAViewSeedBook("range_no_2641"), 0)
    assertEquals(BookDownloadFailureReason.NONE, res2.failureReason)
    assertFalse("Must NEVER send Range header to server without Accept-Ranges: bytes", noRangeSawHeader.get())
    assertEquals(20, res2.chapters.size)
  }

  @Test
  fun failureReasons_reportNotFoundUnsupportedMime_andCancelCleanly(): Unit = runBlocking {
    val (_, errServer) = startServer { server, _ ->
      server.createContext("/missing.txt") { ex ->
        ex.sendResponseHeaders(404, 0)
        ex.responseBody.close()
      }
      server.createContext("/binary.pdf") { ex ->
        val pdfBytes = "%PDF-1.7 fake binary content".toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/pdf")
        ex.sendResponseHeaders(200, pdfBytes.size.toLong())
        ex.responseBody.use { it.write(pdfBytes) }
      }
      server.createContext("/slow.txt") { ex ->
        ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
        val chunk = "A Room with a View by E. M. Forster chapter line. ".repeat(400).toByteArray(Charsets.UTF_8)
        ex.sendResponseHeaders(200, (chunk.size * 12).toLong())
        try {
          for (i in 0 until 12) {
            ex.responseBody.write(chunk)
            ex.responseBody.flush()
            Thread.sleep(250)
          }
          ex.responseBody.close()
        } catch (_: Throwable) {}
      }
    }

    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$errServer/books"
    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = emptyList()

    val notFoundBook = AudiobookBook(
      id = "err_404",
      gutendexId = 999911,
      title = "Missing Book",
      author = "Test Author",
      primaryCategory = "Classic",
      subCategory = "Fiction",
      shortDescription = "Missing.",
      contentType = BookContentType.FULL_PUBLIC_DOMAIN_BOOK,
      hasFullTextAvailable = true,
      formats = mapOf("text/plain; charset=utf-8" to "$errServer/missing.txt")
    )
    val notFoundRes = GutendexAndLibriVoxService.downloadBookTextOnly(context, notFoundBook, 0)
    assertEquals(BookDownloadFailureReason.NOT_FOUND, notFoundRes.failureReason)
    assertTrue(notFoundRes.chapters.isEmpty())

    val pdfBook = notFoundBook.copy(
      id = "err_pdf",
      gutendexId = 999912,
      formats = mapOf("text/plain; charset=utf-8" to "$errServer/binary.pdf")
    )
    val pdfRes = GutendexAndLibriVoxService.downloadBookTextOnly(context, pdfBook, 0)
    assertEquals(BookDownloadFailureReason.UNSUPPORTED_FORMAT, pdfRes.failureReason)
    assertTrue(pdfRes.chapters.isEmpty())

    val slowBook = notFoundBook.copy(
      id = "err_cancel",
      gutendexId = 999913,
      title = "A Room with a View",
      formats = mapOf("text/plain; charset=utf-8" to "$errServer/slow.txt")
    )
    val firstProgressSeen = AtomicBoolean(false)
    val cancelJob = launch(Dispatchers.IO) {
      GutendexAndLibriVoxService.downloadBookTextOnly(
        context = context,
        book = slowBook,
        priorityChapterIndex = 0,
        onProgress = {
          if (it.downloadedBytes > 0L) firstProgressSeen.set(true)
        }
      )
    }
    withTimeout(5000) {
      while (!firstProgressSeen.get()) {
        kotlinx.coroutines.delay(10)
      }
    }
    cancelJob.cancel()
    cancelJob.join()
    val cacheFile = File(context.filesDir, "gutenberg_fulltext/verified_book_err_cancel_v5.txt")
    assertFalse("Cancelled partial download must not promote incomplete file to cache", cacheFile.exists())
  }

  @Test
  fun chapterSelection_voiceSwitchingMidBook_cleaner_andSavedProgressWorkAcross20Chapters() = runBlocking {
    val bookText = buildRoomWithAViewFullGutenbergText()
    val chapters = GutendexAndLibriVoxService.parseAuthenticChapters(
      GutendexAndLibriVoxService.stripGutenbergHeaderAndFooter(bookText),
      "A Room with a View",
      2641
    )
    assertEquals(20, chapters.size)

    val fakeJson = JSONObject(buildRoomWithAViewLibriVoxJson("https://archive.org/download/room_view_0711_librivox"))
    val edition = GutendexAndLibriVoxService.parseLibriVoxBookJson(fakeJson.getJSONArray("books").getJSONObject(0))!!
      .librivoxEditions.first()

    val aligned = GutendexAndLibriVoxService.alignChaptersWithLibriVox(
      rawChapters = chapters,
      sections = edition.sections,
      bookTitle = "A Room with a View"
    )
    val book = createRoomWithAViewSeedBook("room_2641_full").copy(
      contentType = BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
      hasFullTextAvailable = true,
      parsedChapters = aligned,
      librivoxEditions = listOf(edition)
    )

    // Selecting Chapter 2 (index 1) MUST queue Track 02, never Track 01
    AudiobookMediaController.loadBookAndChapter(book, chapterIndex = 1, sentenceIndex = 0, autoPlay = false, preferHumanAudio = true)
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)
    assertTrue(AudiobookMediaController.useHumanNarrator.value)
    assertEquals(
      "https://archive.org/download/room_view_0711_librivox/audio/roomwithaview_02_forster.mp3",
      AudiobookMediaController.currentAudioUrl.value
    )

    // Switch mid-book to Mia (TutorVoice.LEDA), then to Clara (TutorVoice.DESPINA) preserving chapter 2 (index 1)
    AudiobookMediaController.setTutorVoice(TutorVoice.LEDA)
    AudiobookMediaController.setNarrationEditionMode(useHumanLibriVox = false, autoStartPlayback = false)
    assertEquals(TutorVoice.LEDA, AudiobookMediaController.selectedVoice.value)
    assertEquals(false, AudiobookMediaController.useHumanNarrator.value)
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)

    AudiobookMediaController.setTutorVoice(TutorVoice.DESPINA)
    assertEquals(TutorVoice.DESPINA, AudiobookMediaController.selectedVoice.value)
    assertEquals(1, AudiobookMediaController.activeChapterIndex.value)

    // Clean punctuation/markup strictly for TTS speech
    val cleaned = NarrationTextCleaner.cleanForTts(
      "**Chapter VI** -- _Miss Honeychurch_ looked at [12] *Santa Croce* &mdash; `hope`."
    )
    assertFalse(cleaned.contains("*"))
    assertFalse(cleaned.contains("_"))
    assertFalse(cleaned.contains("[12]"))
    assertFalse(cleaned.contains("--"))
    assertTrue(cleaned.contains("Miss Honeychurch"))

    // Saved chapter progress persists and resumes on Chapter 6 (index 5) without resetting to Chapter 1
    AudiobookLearningService.saveUserProgress(
      context,
      "u_resume_2641",
      UserAudiobookProgress(
        bookId = book.id,
        lastEpisodeIndex = 5,
        lastSentenceIndex = 4,
        readingChapterIndex = 5,
        chapterSentenceIndexMap = mapOf(5 to 4),
        completedChapterIndices = setOf(0, 1, 2, 3, 4)
      )
    )
    AudiobookMediaController.attach(context, speechManager, "u_resume_2641")
    AudiobookMediaController.stopAndClearMiniPlayer()
    AudiobookMediaController.loadBookAndChapter(book, chapterIndex = 5, sentenceIndex = 0, autoPlay = false)
    assertEquals(5, AudiobookMediaController.activeChapterIndex.value)
    assertEquals(4, AudiobookMediaController.currentSentenceIndex.value)
  }

  @Test
  fun bookDetailScreen_separatesTextRetryFromAudioDiscovery_andShowsCompleteBadgeOnlyWhenBothVerified() {
    val textBytes = buildRoomWithAViewFullGutenbergText().toByteArray(Charsets.UTF_8)
    val shouldServeText = AtomicBoolean(false)

    val (_, serverUrl) = startServer { server, base ->
      server.createContext("/cache/epub/2641/pg2641.txt") { ex ->
        if (!shouldServeText.get()) {
          ex.sendResponseHeaders(503, 0)
          ex.responseBody.close()
        } else {
          ex.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
          ex.sendResponseHeaders(200, textBytes.size.toLong())
          ex.responseBody.use { it.write(textBytes) }
        }
      }
      server.createContext("/api/feed/audiobooks/") { ex ->
        val json = buildRoomWithAViewLibriVoxJson(base).toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, json.size.toLong())
        ex.responseBody.use { it.write(json) }
      }
      server.createContext("/api/feed/audiotracks") { ex ->
        val json = buildRoomWithAViewLibriVoxJson(base).toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, json.size.toLong())
        ex.responseBody.use { it.write(json) }
      }
    }

    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$serverUrl/books"
    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = listOf(serverUrl)
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$serverUrl/api/feed/audiobooks/"
    GutendexAndLibriVoxService.librivoxAudiotracksUrlOverride = "$serverUrl/api/feed/audiotracks"

    val book = createRoomWithAViewSeedBook("room_detail_retry_2641")

    composeTestRule.setContent {
      LingoSphereTheme {
        BookDetailScreen(
          book = book,
          userId = "detail_retry_user",
          targetLang = Language.findByCode("en"),
          speechManager = speechManager,
          onPlayEpisode = {},
          onBack = {}
        )
      }
    }

    // Wait for audio discovery to succeed while text download fails with 503
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule.onAllNodesWithTag("book_detail_retry_text_btn").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("book_detail_retry_text_btn").assertIsDisplayed()
    // Must show Human Audiobook (because audio succeeded), NEVER falsely show "Complete Book & Audio" yet
    composeTestRule.onNodeWithText("Human Audiobook").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodesWithText("Complete Book & Audio").fetchSemanticsNodes().isEmpty())

    // Now heal the text mirror and tap "Retry Text"
    shouldServeText.set(true)
    composeTestRule.onNodeWithTag("book_detail_retry_text_btn").performClick()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule.onAllNodesWithText("Complete Book & Audio").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Complete Book & Audio").assertIsDisplayed()
    composeTestRule.onNodeWithText("20 Chapters • Classic Novels • B2").assertIsDisplayed()
  }

  @Test
  fun bookDetailScreen_whenNoTextAndNoAudio_displaysZeroChaptersAndHumanUnavailable_never1ChapterOrCompleteBadge() {
    val (_, emptyServer) = startServer { server, _ ->
      server.createContext("/cache/epub/2641/pg2641.txt") { ex ->
        ex.sendResponseHeaders(404, 0)
        ex.responseBody.close()
      }
      server.createContext("/api/feed/audiobooks/") { ex ->
        val json = """{"error":"Audiobooks could not be found"}""".toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, json.size.toLong())
        ex.responseBody.use { it.write(json) }
      }
    }

    GutendexAndLibriVoxService.gutendexBaseUrlOverride = "$emptyServer/books"
    GutendexAndLibriVoxService.gutenbergMirrorHostsOverride = listOf(emptyServer)
    GutendexAndLibriVoxService.librivoxApiUrlOverride = "$emptyServer/api/feed/audiobooks/"
    GutendexAndLibriVoxService.librivoxAudiotracksUrlOverride = "$emptyServer/api/feed/audiotracks"

    val book = createRoomWithAViewSeedBook("room_empty_2641")

    composeTestRule.setContent {
      LingoSphereTheme {
        BookDetailScreen(
          book = book,
          userId = "detail_empty_user",
          targetLang = Language.findByCode("en"),
          speechManager = speechManager,
          onPlayEpisode = {},
          onBack = {}
        )
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule.onAllNodesWithTag("book_detail_retry_text_btn").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithTag("book_detail_retry_audio_btn").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("0 Chapters • Classic Novels • B2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Human audiobook unavailable • AI narration available").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodesWithText("1 Chapters • Classic Novels • B2").fetchSemanticsNodes().isEmpty())
    assertTrue(composeTestRule.onAllNodesWithText("Complete Book & Audio").fetchSemanticsNodes().isEmpty())
  }
}
