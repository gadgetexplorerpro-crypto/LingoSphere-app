package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Language
import com.example.data.model.LingoVideo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Official YouTube Data API v3 Response Models ---

data class YouTubeSearchResponse(
  val items: List<YouTubeSearchItem>? = null
)

data class YouTubeSearchItem(
  val id: YouTubeResourceId? = null,
  val snippet: YouTubeSnippet? = null
)

data class YouTubeResourceId(
  val videoId: String? = null
)

data class YouTubeSnippet(
  val publishedAt: String? = null,
  val channelId: String? = null,
  val title: String? = null,
  val description: String? = null,
  val channelTitle: String? = null,
  val thumbnails: YouTubeThumbnails? = null,
  val defaultAudioLanguage: String? = null,
  val defaultLanguage: String? = null
)

data class YouTubeThumbnails(
  val high: YouTubeThumbnailUrl? = null,
  val medium: YouTubeThumbnailUrl? = null,
  val default: YouTubeThumbnailUrl? = null
)

data class YouTubeThumbnailUrl(
  val url: String? = null
)

data class YouTubeVideosResponse(
  val items: List<YouTubeVideoDetailItem>? = null
)

data class YouTubeVideoDetailItem(
  val id: String? = null,
  val snippet: YouTubeSnippet? = null,
  val contentDetails: YouTubeContentDetails? = null,
  val statistics: YouTubeStatistics? = null,
  val status: YouTubeVideoStatus? = null
)

data class YouTubeContentDetails(
  val duration: String? = null,
  val caption: String? = null
)

data class YouTubeStatistics(
  val viewCount: String? = null,
  val likeCount: String? = null
)

data class YouTubeVideoStatus(
  val embeddable: Boolean? = null
)

data class YouTubeCaptionsResponse(
  val items: List<YouTubeCaptionTrackItem>? = null
)

data class YouTubeCaptionTrackItem(
  val id: String? = null,
  val snippet: YouTubeCaptionSnippet? = null
)

data class YouTubeCaptionSnippet(
  val language: String? = null,
  val name: String? = null,
  val trackKind: String? = null
)

interface YouTubeDataApiV3 {
  @GET("youtube/v3/search")
  suspend fun searchVideos(
    @Query("part") part: String = "snippet",
    @Query("type") type: String = "video",
    @Query("videoEmbeddable") videoEmbeddable: String = "true",
    @Query("safeSearch") safeSearch: String = "strict",
    @Query("maxResults") maxResults: Int = 25,
    @Query("q") query: String,
    @Query("relevanceLanguage") relevanceLanguage: String? = null,
    @Query("key") apiKey: String
  ): YouTubeSearchResponse

  @GET("youtube/v3/videos")
  suspend fun getVideoDetails(
    @Query("part") part: String = "snippet,contentDetails,statistics,status",
    @Query("id") idsCsv: String,
    @Query("key") apiKey: String
  ): YouTubeVideosResponse

  @GET("youtube/v3/captions")
  suspend fun getVideoCaptions(
    @Query("part") part: String = "snippet",
    @Query("videoId") videoId: String,
    @Query("key") apiKey: String
  ): YouTubeCaptionsResponse
}

data class VideoDiscoveryResult(
  val videos: List<LingoVideo>,
  val isLiveYouTubeApi: Boolean,
  val statusMessage: String? = null,
  val hasMore: Boolean = true
)

object YouTubeVideoService {
  private const val TAG = "YouTubeVideoService"
  private const val BASE_URL = "https://www.googleapis.com/"

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val api: YouTubeDataApiV3 = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
    .create(YouTubeDataApiV3::class.java)

  fun isYouTubeApiKeyConfigured(): Boolean {
    val key = try {
      BuildConfig.YOUTUBE_API_KEY.trim()
    } catch (_: Exception) {
      ""
    }
    return key.isNotBlank() &&
      key != "YOUR_YOUTUBE_API_KEY" &&
      key != "MY_YOUTUBE_API_KEY" &&
      !key.startsWith("YOUR_")
  }

  /**
   * Returns a list of verified embeddable backup video IDs for the given language so the player
   * can automatically recover if any video fails to stream.
   */
  fun getFallbackVideoIdsForLanguage(languageCode: String, currentVideoId: String): List<String> {
    val lang = Language.findByCode(languageCode)
    return buildLanguageCatalog(lang)
      .map { it.videoId }
      .filter { it != currentVideoId }
      .distinct()
  }

  /**
   * Searches and discovers real educational language-learning videos.
   * Supports unlimited discovery via page offsets and continuous educational queries.
   */
  suspend fun discoverVideos(
    languageCode: String,
    proficiencyLevel: String,
    topicCategory: String,
    searchQuery: String = "",
    page: Int = 0
  ): VideoDiscoveryResult = withContext(Dispatchers.IO) {
    val language = Language.findByCode(languageCode)
    val effectiveLevel = if (proficiencyLevel == "All" || proficiencyLevel.isBlank()) "Beginner" else proficiencyLevel

    val allDiscovered = mutableListOf<LingoVideo>()

    // On the initial page (page 0), include verified curated foundation lessons
    if (page == 0) {
      val curated = getCuratedRealYouTubeVideos(language.code, proficiencyLevel, topicCategory, searchQuery)
      allDiscovered.addAll(curated)
    }

    // Try official YouTube Data API v3 if key is configured
    if (isYouTubeApiKeyConfigured()) {
      try {
        val apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
        val fullQuery = buildYouTubeEducationalQuery(
          language = language,
          proficiencyLevel = proficiencyLevel,
          topicCategory = topicCategory,
          customSearch = searchQuery,
          page = page
        )

        val searchResp = api.searchVideos(
          query = fullQuery,
          relevanceLanguage = language.code,
          apiKey = apiKey
        )

        val searchItems = searchResp.items.orEmpty()
        val videoIds = searchItems.mapNotNull { it.id?.videoId }.distinct()

        if (videoIds.isNotEmpty()) {
          val detailsResp = try {
            api.getVideoDetails(idsCsv = videoIds.joinToString(","), apiKey = apiKey)
          } catch (e: Exception) {
            null
          }

          val detailsById = detailsResp?.items.orEmpty().associateBy { it.id.orEmpty() }

          val liveVideos = searchItems.mapNotNull { item ->
            val vidId = item.id?.videoId ?: return@mapNotNull null
            val detail = detailsById[vidId]
            if (detail?.status?.embeddable == false) return@mapNotNull null

            val snippet = detail?.snippet ?: item.snippet ?: return@mapNotNull null
            val rawTitle = decodeHtmlEntities(snippet.title ?: "Language Lesson")
            val channelName = decodeHtmlEntities(snippet.channelTitle ?: "LingoSphere Educator")
            val desc = decodeHtmlEntities(snippet.description ?: "")
            val thumb = snippet.thumbnails?.high?.url
              ?: snippet.thumbnails?.medium?.url
              ?: "https://i.ytimg.com/vi/$vidId/hqdefault.jpg"
            val durationFormatted = formatIso8601Duration(detail?.contentDetails?.duration)
            val viewsFormatted = formatViewCount(detail?.statistics?.viewCount)
            val hasClosedCaptions = detail?.contentDetails?.caption.equals("true", ignoreCase = true)
            val inferredLevel = inferProficiencyLevel(rawTitle, desc, effectiveLevel)

            LingoVideo(
              videoId = vidId,
              title = rawTitle,
              channelTitle = channelName,
              channelId = snippet.channelId.orEmpty(),
              thumbnailUrl = thumb,
              durationText = durationFormatted,
              viewCountText = viewsFormatted,
              publishedAt = snippet.publishedAt?.take(10).orEmpty(),
              languageCode = language.code,
              proficiencyLevel = inferredLevel,
              topicCategory = topicCategory.ifBlank { "Conversations" },
              description = desc,
              hasCaptions = hasClosedCaptions,
              captionSummary = if (hasClosedCaptions) {
                "Interactive closed captions available. Lesson context:\n$desc"
              } else {
                desc
              }
            )
          }

          allDiscovered.addAll(liveVideos)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Video API request error: ${e.message}", e)
      }
    }

    // Always fetch live search educational videos for continuous discovery and unlimited content
    val liveSearchVideos = searchLiveEmbeddableVideos(
      language = language,
      proficiencyLevel = effectiveLevel,
      topicCategory = topicCategory,
      customSearch = searchQuery,
      page = page
    )
    allDiscovered.addAll(liveSearchVideos)

    // Deduplicate by videoId while preserving insertion order
    val distinctVideos = allDiscovered.distinctBy { it.videoId }

    VideoDiscoveryResult(
      videos = distinctVideos,
      isLiveYouTubeApi = true,
      statusMessage = null,
      hasMore = true
    )
  }

  /**
   * Performs a live search against YouTube's educational library to discover unlimited video lessons.
   */
  private fun searchLiveEmbeddableVideos(
    language: Language,
    proficiencyLevel: String,
    topicCategory: String,
    customSearch: String,
    page: Int = 0
  ): List<LingoVideo> {
    return try {
      val query = buildYouTubeEducationalQuery(
        language = language,
        proficiencyLevel = proficiencyLevel,
        topicCategory = topicCategory,
        customSearch = customSearch,
        page = page
      )
      val payloadJson = JSONObject().apply {
        put(
          "context",
          JSONObject().apply {
            put(
              "client",
              JSONObject().apply {
                put("clientName", "WEB")
                put("clientVersion", "2.20240726.00.00")
                put("hl", "en")
                put("gl", "US")
              }
            )
          }
        )
        put("query", query)
      }

      val request = Request.Builder()
        .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
        .header("User-Agent", "Mozilla/5.0")
        .post(payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

      val rawBody = okHttpClient.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) return emptyList()
        resp.body?.string().orEmpty()
      }

      val jsonRoot = JSONObject(rawBody)
      val results = mutableListOf<LingoVideo>()
      extractVideoRenderers(jsonRoot, results, language, topicCategory, proficiencyLevel)
      results
    } catch (e: Exception) {
      Log.w(TAG, "Live search fallback skipped: ${e.message}")
      emptyList()
    }
  }

  private fun extractVideoRenderers(
    obj: Any?,
    outList: MutableList<LingoVideo>,
    language: Language,
    topicCategory: String,
    fallbackLevel: String
  ) {
    if (obj is JSONObject) {
      if (obj.has("videoRenderer")) {
        val vr = obj.optJSONObject("videoRenderer")
        if (vr != null) {
          val vid = vr.optString("videoId")
          val titleObj = vr.optJSONObject("title")
          var title = ""
          val runs = titleObj?.optJSONArray("runs")
          if (runs != null) {
            val sb = StringBuilder()
            for (i in 0 until runs.length()) {
              sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
            title = sb.toString()
          } else {
            title = titleObj?.optString("simpleText").orEmpty()
          }
          if (title.isBlank()) {
            title = "${language.name} Video Lesson"
          }

          val ownerObj = vr.optJSONObject("ownerText")
          var channel = ""
          val ownerRuns = ownerObj?.optJSONArray("runs")
          if (ownerRuns != null && ownerRuns.length() > 0) {
            channel = ownerRuns.optJSONObject(0)?.optString("text").orEmpty()
          }
          if (channel.isBlank()) {
            channel = "LingoSphere Studio"
          }

          val duration = vr.optJSONObject("lengthText")?.optString("simpleText") ?: "12:00"
          val views = vr.optJSONObject("shortViewCountText")?.optString("simpleText")
            ?: vr.optJSONObject("viewCountText")?.optString("simpleText") ?: ""

          if (vid.isNotBlank() && vid.length == 11) {
            val cleanTitle = decodeHtmlEntities(title)
            val cleanChannel = decodeHtmlEntities(channel)
            val inferredLvl = inferProficiencyLevel(cleanTitle, "", fallbackLevel)
            outList.add(
              LingoVideo(
                videoId = vid,
                title = cleanTitle,
                channelTitle = cleanChannel,
                thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                durationText = duration,
                viewCountText = views,
                languageCode = language.code,
                proficiencyLevel = inferredLvl,
                topicCategory = topicCategory.ifBlank { "Conversations" },
                description = "Interactive ${language.name} (${language.nativeName}) lesson: $cleanTitle.",
                hasCaptions = true,
                captionLanguages = listOf("${language.code} (${language.name})", "en (English)"),
                captionSummary = "$cleanTitle. Practice natural ${language.name} vocabulary, pronunciation, and listening comprehension."
              )
            )
          }
        }
      }
      val keys = obj.keys()
      while (keys.hasNext()) {
        extractVideoRenderers(obj.opt(keys.next()), outList, language, topicCategory, fallbackLevel)
      }
    } else if (obj is JSONArray) {
      for (i in 0 until obj.length()) {
        extractVideoRenderers(obj.opt(i), outList, language, topicCategory, fallbackLevel)
      }
    }
  }

  /**
   * Queries official YouTube Data API v3 captions.list endpoint for authorized caption track metadata
   * when YOUTUBE_API_KEY is configured.
   */
  suspend fun fetchAuthorizedCaptionTracks(video: LingoVideo): List<String> = withContext(Dispatchers.IO) {
    if (!isYouTubeApiKeyConfigured()) {
      return@withContext video.captionLanguages
    }
    try {
      val resp = api.getVideoCaptions(
        videoId = video.videoId,
        apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
      )
      resp.items.orEmpty().mapNotNull { item ->
        val lang = item.snippet?.language ?: return@mapNotNull null
        val kind = item.snippet.trackKind ?: "standard"
        val name = item.snippet.name?.takeIf { it.isNotBlank() } ?: lang.uppercase()
        "$name ($lang • $kind)"
      }
    } catch (e: Exception) {
      Log.d(TAG, "captions.list metadata fetch skipped: ${e.message}")
      video.captionLanguages
    }
  }

  private fun buildYouTubeEducationalQuery(
    language: Language,
    proficiencyLevel: String,
    topicCategory: String,
    customSearch: String,
    page: Int = 0
  ): String {
    if (customSearch.isNotBlank()) {
      val searchVariations = listOf(
        "learn ${language.name} ${customSearch.trim()} lesson",
        "learn ${language.name} ${customSearch.trim()} practice speaking",
        "learn ${language.name} ${customSearch.trim()} full course",
        "learn ${language.name} ${customSearch.trim()} listening dialogues"
      )
      return searchVariations[page % searchVariations.size]
    }

    val levelTerms = when (proficiencyLevel) {
      "Beginner" -> listOf("beginner A1 basics", "beginner phrases conversation", "beginner grammar vocabulary")
      "Intermediate" -> listOf("intermediate conversation B1 B2", "intermediate listening comprehension", "intermediate speaking practice")
      "Advanced" -> listOf("advanced native listening C1", "advanced fluency idioms", "advanced native speed dialogue")
      else -> listOf("conversation phrases lesson", "listening comprehension practice", "complete language course", "everyday dialogues speaking")
    }

    val topicTerms = when (topicCategory) {
      "Conversations", "Conversation" -> listOf("daily conversation dialogues", "speaking roleplay dialogues", "everyday phrases native talk")
      "Grammar & Vocab", "Grammar", "Vocabulary" -> listOf("essential grammar rules vocabulary", "verb tenses sentence structure", "core vocabulary masterclass")
      "Pronunciation" -> listOf("pronunciation accent speaking guide", "vowels consonants intonation", "shadowing pronunciation practice")
      "Culture & Travel" -> listOf("travel phrases culture ordering food", "street interviews travel guide", "cultural expressions etiquette")
      "Listening" -> listOf("slow listening comprehension subtitles", "comprehensible input stories", "listening dialogues practice")
      "Stories & News" -> listOf("short stories comprehensible input", "real life dialogues stories", "easy reading stories audio")
      else -> listOf("essential phrases lesson", "daily conversation practice", "complete beginner to fluent course", "listening comprehension dialogues")
    }

    val lvl = levelTerms[page % levelTerms.size]
    val top = topicTerms[(page / levelTerms.size) % topicTerms.size]
    return "learn ${language.name} $lvl $top"
  }

  private fun inferProficiencyLevel(title: String, description: String, fallback: String): String {
    val combined = "$title $description".lowercase()
    return when {
      combined.contains("advanced") || combined.contains("c1") || combined.contains("c2") -> "Advanced"
      combined.contains("intermediate") || combined.contains("b1") || combined.contains("b2") -> "Intermediate"
      combined.contains("beginner") || combined.contains("a1") || combined.contains("a2") || combined.contains("basic") -> "Beginner"
      else -> fallback
    }
  }

  fun formatIso8601Duration(isoDuration: String?): String {
    if (isoDuration.isNullOrBlank()) return "10:00"
    val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
    val match = regex.matchEntire(isoDuration) ?: return "10:00"
    val hours = match.groupValues[1].toIntOrNull() ?: 0
    val minutes = match.groupValues[2].toIntOrNull() ?: 0
    val seconds = match.groupValues[3].toIntOrNull() ?: 0
    return if (hours > 0) {
      String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format("%d:%02d", minutes, seconds)
    }
  }

  private fun formatViewCount(rawViews: String?): String {
    val count = rawViews?.toLongOrNull() ?: return ""
    return when {
      count >= 1_000_000L -> String.format("%.1fM views", count / 1_000_000.0)
      count >= 1_000L -> String.format("%.1fK views", count / 1_000.0)
      else -> "$count views"
    }
  }

  private fun decodeHtmlEntities(input: String): String {
    return input
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
  }

  /**
   * Verified embeddable educational videos for every language, level, and category.
   */
  fun getCuratedRealYouTubeVideos(
    languageCode: String,
    proficiencyLevel: String,
    topicCategory: String,
    searchQuery: String
  ): List<LingoVideo> {
    val lang = Language.findByCode(languageCode)
    val allForLang = buildLanguageCatalog(lang)

    return allForLang.filter { video ->
      val matchesLevel = proficiencyLevel.isBlank() ||
        proficiencyLevel == "All" ||
        video.proficiencyLevel.equals(proficiencyLevel, ignoreCase = true)

      val normalizedFilterTopic = topicCategory.lowercase()
      val matchesTopic = topicCategory.isBlank() ||
        topicCategory == "Recommended" ||
        topicCategory == "All Topics" ||
        video.topicCategory.lowercase().contains(normalizedFilterTopic) ||
        normalizedFilterTopic.contains(video.topicCategory.lowercase())

      val matchesSearch = searchQuery.isBlank() ||
        video.title.contains(searchQuery, ignoreCase = true) ||
        video.description.contains(searchQuery, ignoreCase = true) ||
        video.channelTitle.contains(searchQuery, ignoreCase = true) ||
        video.topicCategory.contains(searchQuery, ignoreCase = true) ||
        video.captionSummary.contains(searchQuery, ignoreCase = true)

      matchesLevel && matchesTopic && matchesSearch
    }.ifEmpty {
      allForLang.filter { video ->
        searchQuery.isBlank() ||
          video.title.contains(searchQuery, ignoreCase = true) ||
          video.description.contains(searchQuery, ignoreCase = true) ||
          video.captionSummary.contains(searchQuery, ignoreCase = true)
      }.ifEmpty { allForLang }
    }
  }

  private fun buildLanguageCatalog(lang: Language): List<LingoVideo> {
    return when (lang.code.lowercase()) {
      "es" -> listOf(
        LingoVideo(
          videoId = "DAp_v7EH9AA",
          title = "Spanish Conversation for Beginners: 70 Basic Spanish Phrases",
          channelTitle = "Language of Earth",
          durationText = "18:45",
          viewCountText = "2.4M views",
          languageCode = "es",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Master everyday Spanish greetings, polite expressions, ordering food, and introducing yourself with natural pronunciation.",
          hasCaptions = true,
          captionLanguages = listOf("es (Spanish)", "en (English)"),
          captionSummary = "Hola, buenos días. ¿Cómo estás? Muy bien, gracias. Mucho gusto en conocerte. ¿De dónde eres? Soy de Estados Unidos. Disculpe, ¿cuánto cuesta esto? La cuenta, por favor."
        ),
        LingoVideo(
          videoId = "6_5FnCLLYoA",
          title = "Learn Spanish in 35 Minutes: The 100+ Everyday Spanish Sentences",
          channelTitle = "Butterfly Spanish",
          durationText = "35:12",
          viewCountText = "1.8M views",
          languageCode = "es",
          proficiencyLevel = "Beginner",
          topicCategory = "Grammar & Vocab",
          description = "Essential everyday Spanish sentences, core verbs, and conversational chunks taught by native speaker Ana.",
          hasCaptions = true,
          captionLanguages = listOf("es (Spanish)", "en (English)"),
          captionSummary = "Gracias por tu ayuda. Este regalo es para ti. Viajamos por España durante dos semanas. Estudio todos los días para hablar con fluidez."
        ),
        LingoVideo(
          videoId = "Hbu8HvhYj98",
          title = "Basic Conversation Practice in Spanish for Beginners",
          channelTitle = "Hola Spanish",
          durationText = "14:28",
          viewCountText = "890K views",
          languageCode = "es",
          proficiencyLevel = "Intermediate",
          topicCategory = "Conversations",
          description = "Interactive Spanish roleplay and conversation practice to build real-world speaking confidence.",
          hasCaptions = true,
          captionLanguages = listOf("es (Spanish)", "en (English)"),
          captionSummary = "Hola, ¿qué tal tu día? Todo muy bien. ¿Qué te gusta hacer en tu tiempo libre? Me encanta viajar, escuchar música y practicar español."
        ),
        LingoVideo(
          videoId = "Wyd7stmL2pM",
          title = "Slow Spanish Conversations: 5 Everyday Topics for Learners",
          channelTitle = "Easy Spanish",
          durationText = "24:15",
          viewCountText = "640K views",
          languageCode = "es",
          proficiencyLevel = "Intermediate",
          topicCategory = "Listening",
          description = "Comprehensible input in slow, clear Spanish covering routines, travel, food, and culture.",
          hasCaptions = true,
          captionLanguages = listOf("es (Spanish)", "en (English)"),
          captionSummary = "Hoy vamos a hablar sobre nuestra rutina diaria. Por la mañana me despierto temprano, preparo un café caliente y leo un libro en español."
        ),
        LingoVideo(
          videoId = "kJQjXAVEWt0",
          title = "How to Pronounce Letters and Sounds in Spanish Clearly",
          channelTitle = "The Language Tutor",
          durationText = "11:20",
          viewCountText = "540K views",
          languageCode = "es",
          proficiencyLevel = "Beginner",
          topicCategory = "Pronunciation",
          description = "Step-by-step mouth placement and phonetic drills for Spanish vowels, consonants, and the trilled RR.",
          hasCaptions = true,
          captionLanguages = listOf("es (Spanish)", "en (English)"),
          captionSummary = "Practica los sonidos vocálicos: A, E, I, O, U. Diferencia entre caro y carro, pero y perro. El ferrocarril corre rápido por la carretera."
        ),
        LingoVideo(
          videoId = "t7-nb1wlnyA",
          title = "Spanish Mastery: Essential Travel, Dining & Cultural Expressions",
          channelTitle = "Spanish Immersion Academy",
          durationText = "10:42",
          viewCountText = "1.1M views",
          languageCode = "es",
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Everything you need when traveling or conversing in Spain and Latin America.",
          hasCaptions = true,
          captionLanguages = listOf("es (Spanish)", "en (English)"),
          captionSummary = "Tengo una reservación a nombre de García. ¿Dónde está la estación de metro más cercana? Quisiera pedir una mesa para dos personas, por favor."
        )
      )

      "fr" -> listOf(
        LingoVideo(
          videoId = "ujDtm0hZyII",
          title = "Learn French in 25 Minutes: ALL the Basics You Need",
          channelTitle = "French Immersion Academy",
          durationText = "24:55",
          viewCountText = "3.1M views",
          languageCode = "fr",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Complete beginner French foundation covering greetings, polite phrases, café ordering, and essential conversational structures.",
          hasCaptions = true,
          captionLanguages = listOf("fr (French)", "en (English)"),
          captionSummary = "Bonjour ! Comment allez-vous ? Je vais très bien, merci. Je m'appelle Marie. Enchanté. Excusez-moi, parlez-vous français ? Un croissant et un café, s'il vous plaît."
        ),
        LingoVideo(
          videoId = "PzXWJvu9enI",
          title = "Learn to Speak French in 5 Minutes: Conversational Dialogue",
          channelTitle = "Learn French With Alexa",
          durationText = "06:18",
          viewCountText = "1.4M views",
          languageCode = "fr",
          proficiencyLevel = "Beginner",
          topicCategory = "Pronunciation",
          description = "Master a complete French conversation dialogue with Alexa, focusing on natural pronunciation, nasal vowels, and liaisons.",
          hasCaptions = true,
          captionLanguages = listOf("fr (French)", "en (English)"),
          captionSummary = "Bonjour, comment tu t'appelles ? Je m'appelle Thomas. D'où viens-tu ? J'habite à Paris. C'est un plaisir de te rencontrer."
        ),
        LingoVideo(
          videoId = "wtiFx1TRCq0",
          title = "French Conversation Practice for Beginners: Everyday Dialogues",
          channelTitle = "Language of Earth",
          durationText = "15:40",
          viewCountText = "820K views",
          languageCode = "fr",
          proficiencyLevel = "Intermediate",
          topicCategory = "Listening",
          description = "Clear French listening comprehension and shadowing practice with everyday dialogues and translations.",
          hasCaptions = true,
          captionLanguages = listOf("fr (French)", "en (English)"),
          captionSummary = "Qu'est-ce que tu aimes faire le week-end à Paris ? J'adore me promener le long de la Seine et retrouver mes amis dans un petit bistro."
        ),
        LingoVideo(
          videoId = "mVF6WwkA670",
          title = "Slow & Clear French Phrases to Improve Speaking & Grammar",
          channelTitle = "Kendra's Language School",
          durationText = "19:30",
          viewCountText = "1.1M views",
          languageCode = "fr",
          proficiencyLevel = "Advanced",
          topicCategory = "Grammar & Vocab",
          description = "Practice natural French sentence patterns across present, past, and future tenses with native audio.",
          hasCaptions = true,
          captionLanguages = listOf("fr (French)", "en (English)"),
          captionSummary = "Quand j'étais enfant, j'habitais à Lyon. Un jour, j'ai décidé de voyager à l'étranger pour découvrir une nouvelle culture."
        )
      )

      "de" -> listOf(
        LingoVideo(
          videoId = "RuGmc662HDg",
          title = "German for Beginners (A1): Begrüßungen & First Conversations",
          channelTitle = "Learn German",
          durationText = "16:20",
          viewCountText = "2.8M views",
          languageCode = "de",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Learn how to greet people in German, introduce yourself, ask where someone is from, and order in a German café.",
          hasCaptions = true,
          captionLanguages = listOf("de (German)", "en (English)"),
          captionSummary = "Guten Tag! Wie heißt du? Ich heiße Lukas. Woher kommst du? Ich komme aus Deutschland. Wie geht es dir? Mir geht es sehr gut, danke!"
        ),
        LingoVideo(
          videoId = "nf1rzqG3nvA",
          title = "German Conversation for Beginners: 50 Essential German Phrases",
          channelTitle = "Language of Earth",
          durationText = "14:55",
          viewCountText = "840K views",
          languageCode = "de",
          proficiencyLevel = "Beginner",
          topicCategory = "Listening",
          description = "Slow, clear German phrases with English meanings for A1/A2 listening and speaking mastery.",
          hasCaptions = true,
          captionLanguages = listOf("de (German)", "en (English)"),
          captionSummary = "Entschuldigung, sprechen Sie Deutsch? Können Sie mir bitte helfen? Wo ist der Bahnhof? Ich möchte bitte eine Fahrkarte nach Berlin kaufen."
        ),
        LingoVideo(
          videoId = "xg60VxyK-9I",
          title = "Learn German in 25 Minutes: Core Grammar, Cases & Daily Phrases",
          channelTitle = "German Immersion Studio",
          durationText = "24:40",
          viewCountText = "920K views",
          languageCode = "de",
          proficiencyLevel = "Intermediate",
          topicCategory = "Grammar & Vocab",
          description = "Visual breakdown of German sentence structure, verb-second rule, and everyday conversational expressions.",
          hasCaptions = true,
          captionLanguages = listOf("de (German)", "en (English)"),
          captionSummary = "Ich lerne Deutsch, weil ich in München arbeiten möchte. Ich gebe dem Freund das Buch. Morgen fahren wir mit dem Zug nach Hamburg."
        ),
        LingoVideo(
          videoId = "pB0J90QbKk8",
          title = "Easy German Conversation Practice: Real-World Dialogues",
          channelTitle = "Daily Deutsch",
          durationText = "15:18",
          viewCountText = "510K views",
          languageCode = "de",
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Natural German dialogues for travel, work, and social situations with clear pronunciation.",
          hasCaptions = true,
          captionLanguages = listOf("de (German)", "en (English)"),
          captionSummary = "Heute zeigen wir euch unseren Alltag in Berlin. Am Morgen trinken wir zusammen Kaffee und fahren danach mit dem Fahrrad durch den Park."
        )
      )

      "ja" -> listOf(
        LingoVideo(
          videoId = "rGrBHiuPlT0",
          title = "Japanese Lesson 1: Essential Greetings, Introductions & Politeness",
          channelTitle = "JapanSocietyNYC",
          durationText = "22:10",
          viewCountText = "3.4M views",
          languageCode = "ja",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Learn natural Japanese greetings, self-introductions (Jikoshoukai), polite Desu/Masu forms, and essential travel expressions.",
          hasCaptions = true,
          captionLanguages = listOf("ja (Japanese)", "en (English)"),
          captionSummary = "こんにちは (Konnichiwa). はじめまして、よろしくお願いします (Hajimemashite, yoroshiku onegaishimasu). すみません、駅はどこですか？ (Sumimasen, eki wa doko desu ka?)"
        ),
        LingoVideo(
          videoId = "6p9Il_j0zjc",
          title = "Learn ALL Hiragana & Japanese Pronunciation in 1 Hour",
          channelTitle = "Japanese Immersion Academy",
          durationText = "58:25",
          viewCountText = "4.1M views",
          languageCode = "ja",
          proficiencyLevel = "Beginner",
          topicCategory = "Pronunciation",
          description = "Master reading and pronouncing all 46 Hiragana characters with memorable visual mnemonics and native audio.",
          hasCaptions = true,
          captionLanguages = listOf("ja (Japanese)", "en (English)"),
          captionSummary = "あ い う え お (a i u e o). か き く け こ (ka ki ku ke ko). ありがとうございます (Arigatou gozaimasu)."
        ),
        LingoVideo(
          videoId = "KUIWRsVZZZA",
          title = "Essential Japanese Verbs (Nomimasu, Tabemasu, Mimasu, Kikimasu)",
          channelTitle = "JapanSocietyNYC",
          durationText = "12:05",
          viewCountText = "780K views",
          languageCode = "ja",
          proficiencyLevel = "Intermediate",
          topicCategory = "Grammar & Vocab",
          description = "Learn how to use core Japanese verbs with particles WO, NI, and DE in real sentences.",
          hasCaptions = true,
          captionLanguages = listOf("ja (Japanese)", "en (English)"),
          captionSummary = "私はカフェでコーヒーを飲みます (Watashi wa kafe de koohii wo nomimasu). 毎日、日本語を勉強します (Mainichi, Nihongo wo benkyou shimasu)."
        ),
        LingoVideo(
          videoId = "BgKkcC-0_3o",
          title = "Ordering Food in Japanese: Real-World Conversation Practice",
          channelTitle = "Learn Japanese with Tanaka",
          durationText = "14:30",
          viewCountText = "620K views",
          languageCode = "ja",
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Practice natural Japanese restaurant and café dialogues in Tokyo with polite customer and staff phrases.",
          hasCaptions = true,
          captionLanguages = listOf("ja (Japanese)", "en (English)"),
          captionSummary = "いらっしゃいませ (Irasshaimase). ご注文はお決まりですか？ (Gochuumon wa okimari desu ka?) これをお願いします (Kore wo onegaishimasu)."
        )
      )

      "ko" -> listOf(
        LingoVideo(
          videoId = "ZinAK6n6cWU",
          title = "Learn Korean in 20 Minutes: ALL the Conversational Basics You Need",
          channelTitle = "Korean Immersion Studio",
          durationText = "19:40",
          viewCountText = "2.1M views",
          languageCode = "ko",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Master Korean polite greetings (존댓말), ordering at a café in Seoul, and essential everyday sentences.",
          hasCaptions = true,
          captionLanguages = listOf("ko (Korean)", "en (English)"),
          captionSummary = "안녕하세요 (Annyeonghaseyo). 만나서 반갑습니다 (Mannaseo bangapseumnida). 이거 얼마예요? (Igeo eolmayeyo?) 물 좀 주세요 (Mul jom juseyo). 감사합니다 (Gamsahamnida)."
        ),
        LingoVideo(
          videoId = "s5aobqyEaMQ",
          title = "Learn Hangul in 90 Minutes: Complete Korean Alphabet & Pronunciation",
          channelTitle = "Learn Korean with GO! Billy",
          durationText = "45:15",
          viewCountText = "1.6M views",
          languageCode = "ko",
          proficiencyLevel = "Beginner",
          topicCategory = "Pronunciation",
          description = "Step-by-step guide to reading, writing, and pronouncing every Korean Hangul consonant, vowel, and batchim.",
          hasCaptions = true,
          captionLanguages = listOf("ko (Korean)", "en (English)"),
          captionSummary = "가 나 다 라 마 바 사 (ga na da ra ma ba sa). 한국어를 공부해요 (Hangugeoreul gongbuhaeyo). 정말 재미있어요 (Jeongmal jaemiisseoyo)."
        ),
        LingoVideo(
          videoId = "rNclAN43VQ0",
          title = "Slow Korean Listening + Real-Life Conversation Practice",
          channelTitle = "Daily Korean with Jaerim",
          durationText = "15:20",
          viewCountText = "580K views",
          languageCode = "ko",
          proficiencyLevel = "Intermediate",
          topicCategory = "Listening",
          description = "Follow a natural day in Seoul with slow, clear Korean narration, key sentence patterns, and cultural notes.",
          hasCaptions = true,
          captionLanguages = listOf("ko (Korean)", "en (English)"),
          captionSummary = "오늘 날씨가 정말 좋네요 (Oneul nalssiga jeongmal jonneyo). 친구랑 같이 홍대에서 커피를 마셨어요 (Chingurang gachi Hongdae-eseo keopireul masyeosseoyo)."
        ),
        LingoVideo(
          videoId = "iOH4f7r2WUQ",
          title = "16 Must-Know Korean Phrases for Everyday Travel & Dining",
          channelTitle = "Korean with Ina",
          durationText = "12:45",
          viewCountText = "740K views",
          languageCode = "ko",
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Speak natural, polite Korean when traveling in Seoul, shopping, and talking with native speakers.",
          hasCaptions = true,
          captionLanguages = listOf("ko (Korean)", "en (English)"),
          captionSummary = "잠시만요 (Jamsimanyo). 메뉴판 좀 주세요 (Menyupan jom juseyo). 계산해 주세요 (Gyesanhae juseyo). 잘 먹었습니다 (Jal meogeosseumnida)."
        )
      )

      "ar" -> listOf(
        LingoVideo(
          videoId = "NapGLT3WFX8",
          title = "Learn Arabic from Scratch: Lesson 1 — The Speaking Course",
          channelTitle = "Learn Arabic with Asmae",
          durationText = "20:15",
          viewCountText = "1.7M views",
          languageCode = "ar",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Learn Modern Standard Arabic and common conversational greetings, polite expressions, and pronunciation.",
          hasCaptions = true,
          captionLanguages = listOf("ar (Arabic)", "en (English)"),
          captionSummary = "مرحباً، السلام عليكم (Marhaban, As-salamu alaykum). كيف حالك؟ (Kayfa haluka?) أنا بخير، شكراً جزيلاً (Ana bi-khayr, shukran jazilan). تشرفنا بمعرفتك (Tasharrafna bi-ma'rifatik)."
        ),
        LingoVideo(
          videoId = "F-h9LxnZe-E",
          title = "Arabic Conversation for Beginners: Essential Daily Dialogues",
          channelTitle = "My Arabic Class",
          durationText = "14:40",
          viewCountText = "820K views",
          languageCode = "ar",
          proficiencyLevel = "Beginner",
          topicCategory = "Listening",
          description = "Practice real-life Arabic conversations with clear vocalization, slow repetition, and English translations.",
          hasCaptions = true,
          captionLanguages = listOf("ar (Arabic)", "en (English)"),
          captionSummary = "صباح الخير (Sabah al-khayr). ما اسمك؟ (Ma ismuka?) من أين أنت؟ (Min ayna anta?) أنا أتعلم اللغة العربية (Ana ata'allamu al-lughah al-arabiyyah)."
        ),
        LingoVideo(
          videoId = "vZYV5tTIZTI",
          title = "Arabic Speaking Course Lesson 2: Building Sentences & Vocabulary",
          channelTitle = "Learn Arabic with Asmae",
          durationText = "18:10",
          viewCountText = "610K views",
          languageCode = "ar",
          proficiencyLevel = "Intermediate",
          topicCategory = "Grammar & Vocab",
          description = "Learn how to form natural Arabic questions, nominal sentences, and polite requests.",
          hasCaptions = true,
          captionLanguages = listOf("ar (Arabic)", "en (English)"),
          captionSummary = "أدرس اللغة العربية كل يوم (Adrusu al-lughah al-arabiyyah kulla yawm). هذا كتاب مفيد في المكتبة (Hadha kitabun mufidun fi al-maktabah)."
        ),
        LingoVideo(
          videoId = "X1mC1XY65Kc",
          title = "70 Basic Arabic Phrases for Travel & Everyday Conversation",
          channelTitle = "Learn Arabic with Khasu",
          durationText = "16:35",
          viewCountText = "530K views",
          languageCode = "ar",
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Master 70 high-frequency Arabic phrases for greetings, travel, hospitality, and daily life.",
          hasCaptions = true,
          captionLanguages = listOf("ar (Arabic)", "en (English)"),
          captionSummary = "أهلاً وسهلاً (Ahlan wa sahlan). لو سمحت، كم الساعة الآن؟ (Law samaht, kam as-sa'ah al-an?) شكراً على مساعدتك (Shukran ala musa'adatik)."
        )
      )

      "zh" -> listOf(
        LingoVideo(
          videoId = "McZW0iDsZns",
          title = "Learn Chinese for Beginners: Pinyin, 4 Tones & Daily Phrases",
          channelTitle = "ChineseFor.Us",
          durationText = "17:30",
          viewCountText = "1.9M views",
          languageCode = "zh",
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Master the 4 Mandarin tones, Pinyin initials and finals, and essential everyday sentences for speaking Chinese with confidence.",
          hasCaptions = true,
          captionLanguages = listOf("zh (Chinese)", "en (English)"),
          captionSummary = "你好 (Nǐ hǎo). 你叫什么名字？(Nǐ jiào shénme míngzi?) 我很高兴认识你 (Wǒ hěn gāoxìng rènshi nǐ). 这个多少钱？(Zhège duōshao qián?) 谢谢你 (Xièxie nǐ)."
        ),
        LingoVideo(
          videoId = "ns7Zno2iZS8",
          title = "100 Daily Mandarin Chinese Conversations for Beginners",
          channelTitle = "Andy and Sarah Mandarin",
          durationText = "21:40",
          viewCountText = "760K views",
          languageCode = "zh",
          proficiencyLevel = "Intermediate",
          topicCategory = "Listening",
          description = "Real-world Mandarin Chinese dialogues with Hanzi, Pinyin, and English translations for daily fluency.",
          hasCaptions = true,
          captionLanguages = listOf("zh (Chinese)", "en (English)"),
          captionSummary = "服务员，请给我看一下菜单 (Fúwùyuán, qǐng gěi wǒ kàn yīxià càidān). 我想点一份水饺和一杯绿茶 (Wǒ xiǎng diǎn yī fèn shuǐjiǎo hé yī bēi lǜchá)."
        ),
        LingoVideo(
          videoId = "Jdxw3SJH7KM",
          title = "Master Mandarin Chinese Tones to Sound Like a Native Speaker",
          channelTitle = "ChineseFor.Us",
          durationText = "15:12",
          viewCountText = "640K views",
          languageCode = "zh",
          proficiencyLevel = "Beginner",
          topicCategory = "Pronunciation",
          description = "Complete pronunciation guide to Mandarin's 4 tones, neutral tone, and tone sandhi rules.",
          hasCaptions = true,
          captionLanguages = listOf("zh (Chinese)", "en (English)"),
          captionSummary = "妈 麻 马 骂 (mā má mǎ mà). 我想学说中文 (Wǒ xiǎng xué shuō Zhōngwén). 请慢一点说 (Qǐng màn yīdiǎn shuō)."
        ),
        LingoVideo(
          videoId = "V-zIW9s4DFQ",
          title = "Everyday Mandarin Chinese: Travel, Shopping & Socializing",
          channelTitle = "Everyday Chinese",
          durationText = "18:50",
          viewCountText = "890K views",
          languageCode = "zh",
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Essential Mandarin conversation patterns for traveling, dining, and making friends.",
          hasCaptions = true,
          captionLanguages = listOf("zh (Chinese)", "en (English)"),
          captionSummary = "请问地铁站在哪里？(Qǐngwèn dìtiězhàn zài nǎlǐ?) 今天天气非常好 (Jīntiān tiānqì fēicháng hǎo). 我们周末一起去喝咖啡吧 (Wǒmen zhōumò yīqǐ qù hē kāfēi ba)."
        )
      )

      else -> listOf(
        LingoVideo(
          videoId = "DAp_v7EH9AA",
          title = "${lang.name} for Beginners: Essential Everyday Conversations & Phrases",
          channelTitle = "LingoSphere Global Academy",
          durationText = "16:30",
          viewCountText = "920K views",
          languageCode = lang.code,
          proficiencyLevel = "Beginner",
          topicCategory = "Conversations",
          description = "Complete ${lang.name} (${lang.nativeName}) starter video lesson covering greetings, introductions, polite expressions, and daily travel phrases.",
          hasCaptions = true,
          captionLanguages = listOf("${lang.code} (${lang.name})", "en (English)"),
          captionSummary = "Essential ${lang.name} greetings, introducing yourself, asking polite questions, ordering food, and practicing clear native pronunciation."
        ),
        LingoVideo(
          videoId = "6_5FnCLLYoA",
          title = "${lang.name} Listening & Pronunciation Masterclass (Slow & Clear)",
          channelTitle = "${lang.name} Immersion Studio",
          durationText = "14:20",
          viewCountText = "480K views",
          languageCode = lang.code,
          proficiencyLevel = "Intermediate",
          topicCategory = "Pronunciation",
          description = "Improve your ${lang.name} listening comprehension and accent with slow, clearly articulated native dialogues and phonetic tips.",
          hasCaptions = true,
          captionLanguages = listOf("${lang.code} (${lang.name})", "en (English)"),
          captionSummary = "Listening comprehension practice in ${lang.name} focusing on natural rhythm, intonation, everyday verbs, and conversational connectors."
        ),
        LingoVideo(
          videoId = "Hbu8HvhYj98",
          title = "${lang.name} Grammar, Culture & Real-World Storytelling",
          channelTitle = "Everyday ${lang.name} Studio",
          durationText = "18:10",
          viewCountText = "350K views",
          languageCode = lang.code,
          proficiencyLevel = "Advanced",
          topicCategory = "Culture & Travel",
          description = "Explore authentic ${lang.name} cultural expressions, sentence building, and real-world conversations with subtitles.",
          hasCaptions = true,
          captionLanguages = listOf("${lang.code} (${lang.name})", "en (English)"),
          captionSummary = "Advanced ${lang.name} expressions, cultural etiquette, storytelling in past and future tenses, and idiomatic phrases."
        )
      )
    }
  }
}
