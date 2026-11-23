package com.example.ai

import android.content.Context
import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.CountryCultureJourney
import com.example.data.model.CountryJourneyStage
import com.example.data.model.CurriculumLesson
import com.example.data.model.Language
import com.example.data.model.ListeningAcademyItem
import com.example.data.model.ReadingAcademyPassage
import com.example.data.model.ReadingWordGloss
import com.example.data.model.TutorVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AutoScrollPageSource(val id: String, val displayTitle: String) {
  READING("reading", "Books & Listening Studio"),
  LISTENING("listening", "Books & Listening Studio"),
  LEARN("learn", "Guided Lesson"),
  AI_CONVERSATION("ai_conversation", "AI Conversation"),
  ROLEPLAY("roleplay", "Roleplay Studio"),
  CULTURE_JOURNEY("culture_journey", "Culture Journey")
}

enum class AutoScrollContentLength(
  val id: String,
  val label: String,
  val targetSentenceCount: IntRange,
  val estimatedMinutes: Int
) {
  SHORT("short", "Short", 6..9, 2),
  MEDIUM("medium", "Medium", 10..15, 4),
  LONG("long", "Long", 16..24, 7);

  companion object {
    fun fromId(id: String): AutoScrollContentLength {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
    }
  }
}

enum class AutoScrollSpeedPreset(
  val id: String,
  val label: String,
  val shortLabel: String,
  val pixelsPerSecond: Float
) {
  VERY_SLOW("very_slow", "Very Slow", "0.5x", 22f),
  SLOW("slow", "Slow", "0.75x", 36f),
  NORMAL("normal", "Normal", "1.0x", 54f),
  FAST("fast", "Fast", "1.35x", 78f),
  VERY_FAST("very_fast", "Very Fast", "1.75x", 110f);

  companion object {
    fun fromId(id: String): AutoScrollSpeedPreset {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NORMAL
    }
  }
}

enum class AutoScrollReadingMode(val id: String, val label: String) {
  AUTO_READ("auto_read", "Auto Read"),
  LISTEN_AND_FOLLOW("listen_and_follow", "Listen & Follow");

  companion object {
    fun fromId(id: String): AutoScrollReadingMode {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LISTEN_AND_FOLLOW
    }
  }
}

data class AutoScrollSentenceSegment(
  val index: Int,
  val text: String,
  val translation: String,
  val speaker: String = "",
  val estimatedDurationMs: Long = 3200L
)

data class AutoScrollVocabExplanation(
  val word: String,
  val phonetic: String,
  val partOfSpeech: String,
  val meaningInNative: String,
  val exampleSentence: String
)

data class AutoScrollComprehensionQuestion(
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

data class AutoScrollLearningContent(
  val id: String,
  val pageSource: AutoScrollPageSource,
  val title: String,
  val category: String,
  val topic: String,
  val languageCode: String,
  val nativeLanguageCode: String,
  val cefrLevel: String,
  val contentLength: AutoScrollContentLength,
  val fullText: String,
  val sentences: List<AutoScrollSentenceSegment>,
  val vocabulary: List<AutoScrollVocabExplanation>,
  val questions: List<AutoScrollComprehensionQuestion>,
  val learningObjectives: List<String>,
  val estimatedReadingDurationSec: Int,
  val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class UserAutoScrollPreferences(
  val autoScrollEnabled: Boolean = true,
  val readingMode: AutoScrollReadingMode = AutoScrollReadingMode.LISTEN_AND_FOLLOW,
  val speedPreset: AutoScrollSpeedPreset = AutoScrollSpeedPreset.NORMAL,
  val customSpeedPxPerSec: Float = AutoScrollSpeedPreset.NORMAL.pixelsPerSecond,
  val fontSizeSp: Int = 22,
  val lineSpacingMultiplier: Float = 1.55f,
  val preferredVoiceId: String = TutorVoice.AOEDE.id,
  val voiceSpeedMultiplier: Float = 1.0f,
  val showTranslations: Boolean = true,
  val showTranscriptInListening: Boolean = true,
  val preferredLength: AutoScrollContentLength = AutoScrollContentLength.MEDIUM
)

data class SavedReadingProgressState(
  val contentId: String,
  val pageSource: String,
  val scrollOffsetPx: Int = 0,
  val activeSentenceIndex: Int = 0,
  val completed: Boolean = false,
  val quizScorePercent: Int = 0,
  val updatedAtEpochMs: Long = System.currentTimeMillis()
)

sealed interface AutoScrollGenerationResult {
  data class Success(val content: AutoScrollLearningContent) : AutoScrollGenerationResult
  data class Error(val userFriendlyMessage: String) : AutoScrollGenerationResult
}

object AutoScrollLearningService {

  private const val PREFS_PREFIX = "lingo_autoscroll_v1_"

  val READING_CATEGORIES = listOf(
    "Short Stories",
    "Long Stories",
    "Everyday Conversations",
    "Real-Life Situations",
    "Motivational Passages",
    "Educational Articles",
    "Travel Stories",
    "Cultural Stories",
    "Beginner Reading Lessons",
    "Intermediate & Advanced Reading"
  )

  val LISTENING_CATEGORIES = listOf(
    "Everyday Conversations",
    "Real-Life Situations",
    "Short Stories",
    "Travel & Directions",
    "Workplace & Interviews",
    "Cultural Stories",
    "Motivational Passages"
  )

  val CULTURE_CATEGORIES = listOf(
    "Countries & Traditions",
    "Food & Culture",
    "Festivals",
    "Travel Etiquette",
    "Greetings",
    "Everyday Customs",
    "Cultural Comparisons",
    "Real-World Communication"
  )

  val LEARN_CATEGORIES = listOf(
    "Real-Life Conversations",
    "Story-Based Lessons",
    "Longer Dialogue Exercises",
    "Practical Language Examples",
    "Sentence-Reading Practice"
  )

  private fun prefsForUser(context: Context, userId: String) =
    context.getSharedPreferences("$PREFS_PREFIX${userId.ifBlank { "default_user" }}", Context.MODE_PRIVATE)

  // ===========================================================================
  // 1. USER PREFERENCES & PROGRESS PERSISTENCE (PER AUTHENTICATED USER)
  // ===========================================================================

  fun loadPreferences(context: Context, userId: String): UserAutoScrollPreferences {
    val p = prefsForUser(context, userId)
    val preset = AutoScrollSpeedPreset.fromId(p.getString("speed_preset", AutoScrollSpeedPreset.NORMAL.id) ?: AutoScrollSpeedPreset.NORMAL.id)
    return UserAutoScrollPreferences(
      autoScrollEnabled = p.getBoolean("auto_scroll_enabled", true),
      readingMode = AutoScrollReadingMode.fromId(p.getString("reading_mode", AutoScrollReadingMode.LISTEN_AND_FOLLOW.id) ?: AutoScrollReadingMode.LISTEN_AND_FOLLOW.id),
      speedPreset = preset,
      customSpeedPxPerSec = p.getFloat("custom_speed_px", preset.pixelsPerSecond).coerceIn(14f, 150f),
      fontSizeSp = p.getInt("font_size_sp", 22).coerceIn(16, 34),
      lineSpacingMultiplier = p.getFloat("line_spacing_mult", 1.55f).coerceIn(1.25f, 2.0f),
      preferredVoiceId = p.getString("preferred_voice_id", TutorVoice.AOEDE.id) ?: TutorVoice.AOEDE.id,
      voiceSpeedMultiplier = p.getFloat("voice_speed_mult", 1.0f).coerceIn(0.65f, 1.45f),
      showTranslations = p.getBoolean("show_translations", true),
      showTranscriptInListening = p.getBoolean("show_transcript_listening", true),
      preferredLength = AutoScrollContentLength.fromId(p.getString("preferred_length", AutoScrollContentLength.MEDIUM.id) ?: AutoScrollContentLength.MEDIUM.id)
    )
  }

  fun savePreferences(context: Context, userId: String, prefs: UserAutoScrollPreferences) {
    prefsForUser(context, userId).edit()
      .putBoolean("auto_scroll_enabled", prefs.autoScrollEnabled)
      .putString("reading_mode", prefs.readingMode.id)
      .putString("speed_preset", prefs.speedPreset.id)
      .putFloat("custom_speed_px", prefs.customSpeedPxPerSec.coerceIn(14f, 150f))
      .putInt("font_size_sp", prefs.fontSizeSp.coerceIn(16, 34))
      .putFloat("line_spacing_mult", prefs.lineSpacingMultiplier.coerceIn(1.25f, 2.0f))
      .putString("preferred_voice_id", prefs.preferredVoiceId)
      .putFloat("voice_speed_mult", prefs.voiceSpeedMultiplier.coerceIn(0.65f, 1.45f))
      .putBoolean("show_translations", prefs.showTranslations)
      .putBoolean("show_transcript_listening", prefs.showTranscriptInListening)
      .putString("preferred_length", prefs.preferredLength.id)
      .apply()
  }

  fun saveReadingProgress(
    context: Context,
    userId: String,
    state: SavedReadingProgressState
  ) {
    val p = prefsForUser(context, userId)
    val raw = p.getString("reading_progress_map", "{}") ?: "{}"
    val root = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    val obj = JSONObject().apply {
      put("contentId", state.contentId)
      put("pageSource", state.pageSource)
      put("scrollOffsetPx", state.scrollOffsetPx.coerceAtLeast(0))
      put("activeSentenceIndex", state.activeSentenceIndex.coerceAtLeast(0))
      put("completed", state.completed)
      put("quizScorePercent", state.quizScorePercent)
      put("updatedAtEpochMs", state.updatedAtEpochMs)
    }
    root.put(state.contentId, obj)
    p.edit().putString("reading_progress_map", root.toString()).apply()
  }

  fun loadReadingProgress(
    context: Context,
    userId: String,
    contentId: String
  ): SavedReadingProgressState? {
    val p = prefsForUser(context, userId)
    val raw = p.getString("reading_progress_map", null) ?: return null
    return try {
      val root = JSONObject(raw)
      val obj = root.optJSONObject(contentId) ?: return null
      SavedReadingProgressState(
        contentId = obj.optString("contentId", contentId),
        pageSource = obj.optString("pageSource", AutoScrollPageSource.READING.id),
        scrollOffsetPx = obj.optInt("scrollOffsetPx", 0).coerceAtLeast(0),
        activeSentenceIndex = obj.optInt("activeSentenceIndex", 0).coerceAtLeast(0),
        completed = obj.optBoolean("completed", false),
        quizScorePercent = obj.optInt("quizScorePercent", 0),
        updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
      )
    } catch (_: Exception) {
      null
    }
  }

  fun loadAllProgressMap(context: Context, userId: String): Map<String, SavedReadingProgressState> {
    val p = prefsForUser(context, userId)
    val raw = p.getString("reading_progress_map", null) ?: return emptyMap()
    return try {
      val root = JSONObject(raw)
      val map = mutableMapOf<String, SavedReadingProgressState>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val obj = root.optJSONObject(k) ?: continue
        map[k] = SavedReadingProgressState(
          contentId = obj.optString("contentId", k),
          pageSource = obj.optString("pageSource", AutoScrollPageSource.READING.id),
          scrollOffsetPx = obj.optInt("scrollOffsetPx", 0).coerceAtLeast(0),
          activeSentenceIndex = obj.optInt("activeSentenceIndex", 0).coerceAtLeast(0),
          completed = obj.optBoolean("completed", false),
          quizScorePercent = obj.optInt("quizScorePercent", 0),
          updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
        )
      }
      map
    } catch (_: Exception) {
      emptyMap()
    }
  }

  // ===========================================================================
  // 2. GENERATED CONTENT CACHE (PER USER + PAGE + LANGUAGE)
  // ===========================================================================

  fun loadCachedContentList(
    context: Context,
    userId: String,
    pageSource: AutoScrollPageSource,
    languageCode: String
  ): List<AutoScrollLearningContent> {
    val p = prefsForUser(context, userId)
    val key = "cached_${pageSource.id}_${languageCode.lowercase()}"
    val raw = p.getString(key, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          deserializeContent(obj)?.let { add(it) }
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveGeneratedContentToCache(
    context: Context,
    userId: String,
    content: AutoScrollLearningContent
  ) {
    val p = prefsForUser(context, userId)
    val key = "cached_${content.pageSource.id}_${content.languageCode.lowercase()}"
    val existing = loadCachedContentList(context, userId, content.pageSource, content.languageCode)
      .filterNot { it.id == content.id || it.title.equals(content.title, ignoreCase = true) }
    val updated = (listOf(content) + existing).take(30)
    val arr = JSONArray()
    updated.forEach { item ->
      arr.put(serializeContent(item))
    }
    p.edit().putString(key, arr.toString()).apply()
  }

  // ===========================================================================
  // 3. DYNAMIC GEMINI CONTENT GENERATION FOR ALL 6 PAGES
  // ===========================================================================

  suspend fun generateFreshAutoScrollContent(
    context: Context,
    userId: String,
    pageSource: AutoScrollPageSource,
    targetLanguageCode: String,
    nativeLanguageCode: String,
    cefrLevel: String,
    category: String,
    topic: String = "",
    contentLength: AutoScrollContentLength = AutoScrollContentLength.MEDIUM,
    completedLessonIds: Set<String> = emptySet(),
    isPremiumUser: Boolean = false
  ): AutoScrollGenerationResult = withContext(Dispatchers.IO) {
    val hasTestHook = GeminiTutorService.testResponseInterceptor != null ||
      AiProviderManager.openAiBackendTestInterceptor != null ||
      GeminiService.testBypassDailyQuota
    val effectivePro = isPremiumUser ||
      com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement ||
      GeminiService.quotaUiState.value.isProUser
    if (!effectivePro && !hasTestHook) {
      val msg = "Unlock AI learning with LingoSphere Pro."
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.AI_UPGRADE,
        featureKey = "ai.autoscroll",
        headline = "Unlock AI learning",
        reason = msg
      )
      return@withContext AutoScrollGenerationResult.Error(msg)
    }
    val targetLang = Language.findByCode(targetLanguageCode)
    val nativeLang = Language.findByCode(nativeLanguageCode.ifBlank { "en" })
    val cleanCefr = cefrLevel.uppercase().takeIf { it in listOf("A1", "A2", "B1", "B2", "C1", "C2") } ?: "B1"

    val effectiveLength = when {
      category.contains("Short", ignoreCase = true) -> AutoScrollContentLength.SHORT
      category.contains("Long", ignoreCase = true) -> AutoScrollContentLength.LONG
      else -> contentLength
    }

    val effectiveCefr = when {
      category.contains("Beginner", ignoreCase = true) -> "A1"
      category.contains("Intermediate", ignoreCase = true) && cleanCefr in listOf("A1", "A2") -> "B1"
      category.contains("Advanced", ignoreCase = true) && cleanCefr in listOf("A1", "A2", "B1") -> "C1"
      else -> cleanCefr
    }

    val cachedItems = loadCachedContentList(context, userId, pageSource, targetLang.code)
    val recentTitles = cachedItems.take(10).map { it.title }
    val recentCompleted = completedLessonIds.take(12).joinToString(", ")

    val isDialogueStyle = category.contains("Conversation", ignoreCase = true) ||
      category.contains("Dialogue", ignoreCase = true) ||
      category.contains("Roleplay", ignoreCase = true) ||
      category.contains("Situation", ignoreCase = true) ||
      pageSource == AutoScrollPageSource.LISTENING ||
      pageSource == AutoScrollPageSource.ROLEPLAY

    val sentenceCountRange = effectiveLength.targetSentenceCount
    val specificTopicInstruction = if (topic.isNotBlank()) {
      "Specific user-requested topic/theme: \"$topic\"."
    } else {
      "Choose an engaging, fresh, practical topic within the \"$category\" category that has NOT been covered in recent titles."
    }

    val systemInstruction = """
      You are LingoSphere's educational content writer and language curriculum designer.
      Generate 100% original, culturally authentic, pedagogically accurate content in ${targetLang.name} (${targetLang.nativeName}, language code: ${targetLang.code}) tailored to CEFR level $effectiveCefr.
      Explanations, translations, and comprehension questions/options should help a learner whose explanation/native language is ${nativeLang.name} (${nativeLang.code}).
      Output ONLY valid raw JSON with no markdown code fences and no extra commentary.
    """.trimIndent()

    val prompt = """
      Generate a new ${effectiveLength.label} (${sentenceCountRange.first} to ${sentenceCountRange.last} sentences) language learning activity for page: "${pageSource.displayTitle}".
      - Target Learning Language: ${targetLang.name} (${targetLang.code})
      - Learner Explanation Language: ${nativeLang.name} (${nativeLang.code})
      - CEFR Proficiency Level: $effectiveCefr
      - Category: "$category"
      - $specificTopicInstruction
      - Style: ${if (isDialogueStyle) "Natural multi-turn conversation/dialogue with realistic speaker names" else "Engaging narrative story, article, or motivational passage"}
      - Previously completed lesson IDs: [${recentCompleted.ifBlank { "none" }}]
      - Recent titles to avoid repeating: [${recentTitles.joinToString("; ").ifBlank { "none" }}]

      Return a single JSON object with this exact structure:
      {
        "title": "Engaging title in ${targetLang.name}",
        "cefrLevel": "$effectiveCefr",
        "category": "$category",
        "estimatedReadingDurationSec": ${effectiveLength.estimatedMinutes * 60},
        "learningObjectives": [
          "Objective 1 in ${nativeLang.name}",
          "Objective 2 in ${nativeLang.name}"
        ],
        "sentences": [
          {
            "speaker": "${if (isDialogueStyle) "Speaker Name" else ""}",
            "text": "Sentence in ${targetLang.name}",
            "translation": "Natural translation in ${nativeLang.name}"
          }
        ],
        "vocabulary": [
          {
            "word": "Key word or phrase from the text in ${targetLang.name}",
            "phonetic": "Pronunciation guide",
            "partOfSpeech": "noun/verb/adjective/phrase",
            "meaningInNative": "Clear meaning and explanation in ${nativeLang.name}",
            "exampleSentence": "Example sentence in ${targetLang.name}"
          }
        ],
        "questions": [
          {
            "question": "Comprehension question about the passage",
            "options": ["Option A", "Option B", "Option C", "Option D"],
            "correctIndex": 0,
            "explanation": "Why this answer is correct in ${nativeLang.name}"
          }
        ]
      }

      Requirements:
      1. Provide between ${sentenceCountRange.first} and ${sentenceCountRange.last} sentences in "sentences".
      2. Every sentence "text" MUST be written in ${targetLang.name}.
      3. Provide 4 to 6 difficult or high-value vocabulary items in "vocabulary".
      4. Provide 3 comprehension questions in "questions", each with 4 options and a valid "correctIndex" (0 to 3).
    """.trimIndent()

    val aiResult = AiProviderManager.generateLessonContent(
      userPromptText = prompt,
      systemInstructionText = systemInstruction,
      taskType = AiTaskType.LESSON_GENERATION,
      targetLanguage = targetLang.name,
      nativeLanguage = nativeLang.name,
      cefrLevel = effectiveCefr,
      scenarioTitle = "$category - $topic",
      temperature = 0.72f,
      incrementQuotaOnSuccess = true
    )
    val rawResponse = aiResult?.outputText?.takeIf { it.isNotBlank() }
      ?: GeminiTutorService.generateGeminiText(
        systemInstructionText = systemInstruction,
        userPromptText = prompt,
        temperature = 0.72f,
        featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT,
        incrementQuotaOnSuccess = true
      )

    if (rawResponse.isNullOrBlank()) {
      return@withContext AutoScrollGenerationResult.Error(
        "Unable to generate a new lesson right now. Please check your connection and tap Retry."
      )
    }

    val parsed = parseAndValidateGeneratedJson(
      rawJson = rawResponse,
      pageSource = pageSource,
      defaultCategory = category,
      defaultTopic = topic,
      languageCode = targetLang.code,
      nativeLanguageCode = nativeLang.code,
      defaultCefr = effectiveCefr,
      contentLength = effectiveLength
    )

    if (parsed == null) {
      return@withContext AutoScrollGenerationResult.Error(
        "The new lesson could not be prepared completely. Please tap Retry to generate a fresh passage."
      )
    }

    saveGeneratedContentToCache(context, userId, parsed)
    AutoScrollGenerationResult.Success(parsed)
  }

  private fun parseAndValidateGeneratedJson(
    rawJson: String,
    pageSource: AutoScrollPageSource,
    defaultCategory: String,
    defaultTopic: String,
    languageCode: String,
    nativeLanguageCode: String,
    defaultCefr: String,
    contentLength: AutoScrollContentLength
  ): AutoScrollLearningContent? {
    return try {
      val cleaned = rawJson
        .replace("```json", "")
        .replace("```", "")
        .trim()
      val startIdx = cleaned.indexOf('{')
      val endIdx = cleaned.lastIndexOf('}')
      if (startIdx < 0 || endIdx <= startIdx) return null
      val jsonSlice = cleaned.substring(startIdx, endIdx + 1)
      val root = JSONObject(jsonSlice)

      val title = root.optString("title", "").trim()
      if (title.isBlank()) return null

      val cefr = root.optString("cefrLevel", defaultCefr).trim().ifBlank { defaultCefr }
      val category = root.optString("category", defaultCategory).trim().ifBlank { defaultCategory }

      val sentencesArr = root.optJSONArray("sentences") ?: return null
      val sentences = mutableListOf<AutoScrollSentenceSegment>()
      for (i in 0 until sentencesArr.length()) {
        val sObj = sentencesArr.optJSONObject(i) ?: continue
        val text = sObj.optString("text", "").trim()
        if (text.isBlank()) continue
        val translation = sObj.optString("translation", "").trim()
        val speaker = sObj.optString("speaker", "").trim()
        val wordCount = text.split(Regex("\\s+")).size.coerceAtLeast(2)
        val estMs = (wordCount * 410L).coerceIn(1800L, 9500L)
        sentences.add(
          AutoScrollSentenceSegment(
            index = sentences.size,
            text = text,
            translation = translation,
            speaker = speaker,
            estimatedDurationMs = estMs
          )
        )
      }

      if (sentences.size < 3) return null

      val vocabArr = root.optJSONArray("vocabulary")
      val vocabulary = mutableListOf<AutoScrollVocabExplanation>()
      if (vocabArr != null) {
        for (i in 0 until vocabArr.length()) {
          val vObj = vocabArr.optJSONObject(i) ?: continue
          val word = vObj.optString("word", "").trim()
          val meaning = vObj.optString("meaningInNative", vObj.optString("meaning", "")).trim()
          if (word.isNotBlank() && meaning.isNotBlank()) {
            vocabulary.add(
              AutoScrollVocabExplanation(
                word = word,
                phonetic = vObj.optString("phonetic", "").trim(),
                partOfSpeech = vObj.optString("partOfSpeech", "phrase").trim(),
                meaningInNative = meaning,
                exampleSentence = vObj.optString("exampleSentence", "").trim()
              )
            )
          }
        }
      }

      val questionsArr = root.optJSONArray("questions")
      val questions = mutableListOf<AutoScrollComprehensionQuestion>()
      if (questionsArr != null) {
        for (i in 0 until questionsArr.length()) {
          val qObj = questionsArr.optJSONObject(i) ?: continue
          val qText = qObj.optString("question", "").trim()
          val optsArr = qObj.optJSONArray("options")
          if (qText.isBlank() || optsArr == null || optsArr.length() < 2) continue
          val opts = buildList {
            for (j in 0 until optsArr.length()) {
              val opt = optsArr.optString(j, "").trim()
              if (opt.isNotBlank()) add(opt)
            }
          }
          if (opts.size >= 2) {
            val correctIdx = qObj.optInt("correctIndex", 0).coerceIn(0, opts.size - 1)
            val explanation = qObj.optString("explanation", "").trim()
            questions.add(
              AutoScrollComprehensionQuestion(
                question = qText,
                options = opts,
                correctIndex = correctIdx,
                explanation = explanation
              )
            )
          }
        }
      }

      val objArr = root.optJSONArray("learningObjectives")
      val objectives = buildList {
        if (objArr != null) {
          for (i in 0 until objArr.length()) {
            val item = objArr.optString(i, "").trim()
            if (item.isNotBlank()) add(item)
          }
        }
      }.ifEmpty {
        listOf(
          "Build reading and listening fluency in $category ($cefr)",
          "Master ${vocabulary.size.coerceAtLeast(3)} practical expressions in context"
        )
      }

      val fullText = sentences.joinToString(" ") { seg ->
        if (seg.speaker.isNotBlank()) "${seg.speaker}: ${seg.text}" else seg.text
      }
      val totalWords = sentences.sumOf { it.text.split(Regex("\\s+")).size }.coerceAtLeast(20)
      val estDurationSec = root.optInt("estimatedReadingDurationSec", (totalWords * 60) / 110)
        .coerceIn(45, 900)

      AutoScrollLearningContent(
        id = "autoscroll_${pageSource.id}_${languageCode}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
        pageSource = pageSource,
        title = title,
        category = category,
        topic = defaultTopic.ifBlank { category },
        languageCode = languageCode,
        nativeLanguageCode = nativeLanguageCode,
        cefrLevel = cefr,
        contentLength = contentLength,
        fullText = fullText,
        sentences = sentences,
        vocabulary = vocabulary,
        questions = questions,
        learningObjectives = objectives,
        estimatedReadingDurationSec = estDurationSec
      )
    } catch (_: Exception) {
      null
    }
  }

  // ===========================================================================
  // 4. CONVERTERS FROM EXISTING LINGOSPHERE MODELS TO AUTO-SCROLL CONTENT
  // ===========================================================================

  fun fromReadingPassage(
    passage: ReadingAcademyPassage,
    nativeLanguageCode: String = "en"
  ): AutoScrollLearningContent {
    val rawSentences = mutableListOf<AutoScrollSentenceSegment>()
    passage.paragraphs.forEachIndexed { pIdx, para ->
      val transPara = passage.paragraphTranslations.getOrNull(pIdx).orEmpty()
      val splitTarget = splitIntoSentences(para)
      val splitTrans = splitIntoSentences(transPara)
      splitTarget.forEachIndexed { sIdx, sentenceText ->
        val matchedTrans = splitTrans.getOrNull(sIdx) ?: transPara
        val words = sentenceText.split(Regex("\\s+")).size.coerceAtLeast(2)
        rawSentences.add(
          AutoScrollSentenceSegment(
            index = rawSentences.size,
            text = sentenceText,
            translation = matchedTrans,
            speaker = "",
            estimatedDurationMs = (words * 400L).coerceIn(1800L, 9000L)
          )
        )
      }
    }

    val vocab = passage.glossary.values.map { gloss ->
      AutoScrollVocabExplanation(
        word = gloss.word,
        phonetic = gloss.phonetic,
        partOfSpeech = gloss.partOfSpeech,
        meaningInNative = "${gloss.translation} — ${gloss.definition}",
        exampleSentence = ""
      )
    }

    val questions = passage.questions.map { q ->
      AutoScrollComprehensionQuestion(
        question = q.question,
        options = q.options,
        correctIndex = q.correctIndex.coerceIn(0, (q.options.size - 1).coerceAtLeast(0)),
        explanation = q.explanation
      )
    }

    val totalWords = rawSentences.sumOf { it.text.split(Regex("\\s+")).size }.coerceAtLeast(25)
    return AutoScrollLearningContent(
      id = passage.id,
      pageSource = AutoScrollPageSource.READING,
      title = passage.title,
      category = passage.category,
      topic = passage.category,
      languageCode = passage.languageCode,
      nativeLanguageCode = nativeLanguageCode,
      cefrLevel = passage.cefrLevel,
      contentLength = when {
        rawSentences.size <= 8 -> AutoScrollContentLength.SHORT
        rawSentences.size <= 15 -> AutoScrollContentLength.MEDIUM
        else -> AutoScrollContentLength.LONG
      },
      fullText = passage.paragraphs.joinToString("\n\n"),
      sentences = rawSentences,
      vocabulary = vocab,
      questions = questions,
      learningObjectives = listOf(
        "Read and follow '${passage.title}' (${passage.cefrLevel}) with smooth pacing",
        "Learn ${vocab.size} contextual vocabulary expressions"
      ),
      estimatedReadingDurationSec = ((totalWords * 60) / 110).coerceAtLeast(60)
    )
  }

  fun fromListeningItem(
    item: ListeningAcademyItem,
    nativeLanguageCode: String = "en"
  ): AutoScrollLearningContent {
    val segments = item.lines.mapIndexed { idx, line ->
      val words = line.targetText.split(Regex("\\s+")).size.coerceAtLeast(2)
      AutoScrollSentenceSegment(
        index = idx,
        text = line.targetText,
        translation = line.nativeTranslation,
        speaker = line.speaker,
        estimatedDurationMs = (words * 410L).coerceIn(1800L, 9000L)
      )
    }
    val vocab = item.lines.mapNotNull { line ->
      if (line.keyVocabWord.isNotBlank()) {
        AutoScrollVocabExplanation(
          word = line.keyVocabWord,
          phonetic = "",
          partOfSpeech = "phrase",
          meaningInNative = line.keyVocabMeaning,
          exampleSentence = line.targetText
        )
      } else null
    }
    val questions = item.questions.map { q ->
      AutoScrollComprehensionQuestion(
        question = q.question,
        options = q.options,
        correctIndex = q.correctIndex.coerceIn(0, (q.options.size - 1).coerceAtLeast(0)),
        explanation = q.explanation
      )
    }
    val totalWords = segments.sumOf { it.text.split(Regex("\\s+")).size }.coerceAtLeast(20)
    return AutoScrollLearningContent(
      id = item.id,
      pageSource = AutoScrollPageSource.LISTENING,
      title = item.title,
      category = item.category,
      topic = item.category,
      languageCode = item.languageCode,
      nativeLanguageCode = nativeLanguageCode,
      cefrLevel = item.cefrLevel,
      contentLength = when {
        segments.size <= 8 -> AutoScrollContentLength.SHORT
        segments.size <= 15 -> AutoScrollContentLength.MEDIUM
        else -> AutoScrollContentLength.LONG
      },
      fullText = segments.joinToString("\n") { "${it.speaker}: ${it.text}" },
      sentences = segments,
      vocabulary = vocab,
      questions = questions,
      learningObjectives = listOf(
        "Follow spoken dialogue '${item.title}' (${item.cefrLevel}) with synchronized audio",
        "Answer ${questions.size} listening comprehension questions"
      ),
      estimatedReadingDurationSec = ((totalWords * 60) / 110).coerceAtLeast(60)
    )
  }

  fun fromCurriculumLesson(
    lesson: CurriculumLesson,
    nativeLanguageCode: String = "en"
  ): AutoScrollLearningContent {
    val segments = mutableListOf<AutoScrollSentenceSegment>()
    lesson.exercises.forEach { ex ->
      val correctOpt = ex.options.getOrNull(ex.correctOptionIndex)
        ?: ex.acceptableAnswers.firstOrNull().orEmpty()
      val target = ex.targetAudioText.ifBlank { correctOpt }.trim()
      if (target.isNotBlank()) {
        val words = target.split(Regex("\\s+")).size.coerceAtLeast(2)
        segments.add(
          AutoScrollSentenceSegment(
            index = segments.size,
            text = target,
            translation = ex.explanationNative.ifBlank { ex.prompt },
            speaker = if (segments.size % 2 == 0) "Emma" else "Practice",
            estimatedDurationMs = (words * 420L).coerceIn(1800L, 8500L)
          )
        )
      }
    }
    val vocab = lesson.exercises.mapNotNull { ex ->
      val phrase = ex.targetAudioText.trim()
      if (phrase.isNotBlank()) {
        AutoScrollVocabExplanation(
          word = phrase,
          phonetic = ex.phoneticGuide,
          partOfSpeech = "lesson expression",
          meaningInNative = ex.explanationNative.ifBlank { ex.prompt },
          exampleSentence = phrase
        )
      } else null
    }.distinctBy { it.word.lowercase() }.take(6)

    val questions = lesson.exercises.take(3).mapNotNull { ex ->
      if (ex.options.size >= 2) {
        val cIdx = ex.correctOptionIndex.coerceIn(0, ex.options.size - 1)
        AutoScrollComprehensionQuestion(
          question = ex.prompt,
          options = ex.options,
          correctIndex = cIdx,
          explanation = ex.explanationNative.ifBlank { "Correct answer: ${ex.options[cIdx]}" }
        )
      } else null
    }
    return AutoScrollLearningContent(
      id = "lesson_autoscroll_${lesson.id}",
      pageSource = AutoScrollPageSource.LEARN,
      title = lesson.title,
      category = lesson.category,
      topic = lesson.description,
      languageCode = lesson.targetLanguageCode,
      nativeLanguageCode = nativeLanguageCode,
      cefrLevel = lesson.cefrLevel,
      contentLength = AutoScrollContentLength.MEDIUM,
      fullText = segments.joinToString("\n") { it.text },
      sentences = segments,
      vocabulary = vocab,
      questions = questions,
      learningObjectives = lesson.learningOutcomes.ifEmpty { listOf(lesson.description) },
      estimatedReadingDurationSec = (lesson.estimatedMinutes * 60).coerceAtLeast(90)
    )
  }

  fun fromCultureJourney(
    journey: CountryCultureJourney,
    stage: CountryJourneyStage? = null,
    nativeLanguageCode: String = "en"
  ): AutoScrollLearningContent {
    val activeStage = stage ?: journey.stages.firstOrNull()
    val segments = mutableListOf<AutoScrollSentenceSegment>()

    if (activeStage != null) {
      splitIntoSentences(activeStage.culturalContext).forEach { s ->
        segments.add(
          AutoScrollSentenceSegment(
            index = segments.size,
            text = s,
            translation = activeStage.culturalInsightTip,
            speaker = journey.countryName,
            estimatedDurationMs = (s.split(Regex("\\s+")).size * 400L).coerceIn(2000L, 8500L)
          )
        )
      }
      activeStage.keyPhrases.forEach { card ->
        segments.add(
          AutoScrollSentenceSegment(
            index = segments.size,
            text = card.phrase,
            translation = "${card.meaning} (${card.culturalNote})",
            speaker = "Local Expression",
            estimatedDurationMs = 3600L
          )
        )
      }
    }

    val vocab = (activeStage?.keyPhrases ?: journey.survivalVocab).map { card ->
      AutoScrollVocabExplanation(
        word = card.phrase,
        phonetic = card.phonetic,
        partOfSpeech = "cultural expression",
        meaningInNative = "${card.meaning} — ${card.culturalNote}",
        exampleSentence = card.phrase
      )
    }

    val questions = if (activeStage != null && activeStage.listeningExercise.options.isNotEmpty()) {
      listOf(
        AutoScrollComprehensionQuestion(
          question = activeStage.listeningExercise.question,
          options = activeStage.listeningExercise.options,
          correctIndex = activeStage.listeningExercise.correctIndex.coerceIn(
            0,
            activeStage.listeningExercise.options.size - 1
          ),
          explanation = activeStage.listeningExercise.culturalExplanation
        )
      )
    } else emptyList()

    return AutoScrollLearningContent(
      id = "culture_${journey.countryCode}_${activeStage?.stageNumber ?: 1}",
      pageSource = AutoScrollPageSource.CULTURE_JOURNEY,
      title = activeStage?.title ?: "${journey.countryName} Cultural Guide",
      category = activeStage?.subtitle ?: "Countries & Traditions",
      topic = journey.countryName,
      languageCode = journey.languageCode,
      nativeLanguageCode = nativeLanguageCode,
      cefrLevel = "B1",
      contentLength = AutoScrollContentLength.MEDIUM,
      fullText = segments.joinToString("\n\n") { it.text },
      sentences = segments,
      vocabulary = vocab,
      questions = questions,
      learningObjectives = listOf(
        "Discover ${journey.countryName} customs, etiquette, and real-world expressions"
      ),
      estimatedReadingDurationSec = 180
    )
  }

  fun toReadingAcademyPassage(content: AutoScrollLearningContent): ReadingAcademyPassage {
    return ReadingAcademyPassage(
      id = content.id,
      languageCode = content.languageCode,
      cefrLevel = content.cefrLevel,
      title = content.title,
      category = content.category,
      paragraphs = content.sentences.map { seg ->
        if (seg.speaker.isNotBlank()) "${seg.speaker}: ${seg.text}" else seg.text
      },
      paragraphTranslations = content.sentences.map { it.translation },
      glossary = content.vocabulary.associate { v ->
        v.word.lowercase() to ReadingWordGloss(
          word = v.word,
          phonetic = v.phonetic,
          partOfSpeech = v.partOfSpeech,
          translation = v.meaningInNative,
          definition = v.exampleSentence.ifBlank { v.meaningInNative }
        )
      },
      questions = content.questions.map { q ->
        AcademyComprehensionQuestion(
          question = q.question,
          options = q.options,
          correctIndex = q.correctIndex,
          explanation = q.explanation
        )
      }
    )
  }

  fun splitIntoSentences(text: String): List<String> {
    val clean = text.trim()
    if (clean.isBlank()) return emptyList()
    val parts = clean.split(Regex("(?<=[.!?。！？])\\s+"))
      .map { it.trim() }
      .filter { it.isNotBlank() }
    return if (parts.isEmpty()) listOf(clean) else parts
  }

  // ===========================================================================
  // 5. JSON SERIALIZATION HELPERS
  // ===========================================================================

  private fun serializeContent(c: AutoScrollLearningContent): JSONObject {
    return JSONObject().apply {
      put("id", c.id)
      put("pageSource", c.pageSource.id)
      put("title", c.title)
      put("category", c.category)
      put("topic", c.topic)
      put("languageCode", c.languageCode)
      put("nativeLanguageCode", c.nativeLanguageCode)
      put("cefrLevel", c.cefrLevel)
      put("contentLength", c.contentLength.id)
      put("fullText", c.fullText)
      put("estimatedReadingDurationSec", c.estimatedReadingDurationSec)
      put("createdAtEpochMs", c.createdAtEpochMs)

      val sArr = JSONArray()
      c.sentences.forEach { s ->
        sArr.put(
          JSONObject().apply {
            put("index", s.index)
            put("text", s.text)
            put("translation", s.translation)
            put("speaker", s.speaker)
            put("estimatedDurationMs", s.estimatedDurationMs)
          }
        )
      }
      put("sentences", sArr)

      val vArr = JSONArray()
      c.vocabulary.forEach { v ->
        vArr.put(
          JSONObject().apply {
            put("word", v.word)
            put("phonetic", v.phonetic)
            put("partOfSpeech", v.partOfSpeech)
            put("meaningInNative", v.meaningInNative)
            put("exampleSentence", v.exampleSentence)
          }
        )
      }
      put("vocabulary", vArr)

      val qArr = JSONArray()
      c.questions.forEach { q ->
        qArr.put(
          JSONObject().apply {
            put("question", q.question)
            val oArr = JSONArray()
            q.options.forEach { oArr.put(it) }
            put("options", oArr)
            put("correctIndex", q.correctIndex)
            put("explanation", q.explanation)
          }
        )
      }
      put("questions", qArr)

      val objArr = JSONArray()
      c.learningObjectives.forEach { objArr.put(it) }
      put("learningObjectives", objArr)
    }
  }

  private fun deserializeContent(obj: JSONObject): AutoScrollLearningContent? {
    return try {
      val id = obj.optString("id", "")
      val title = obj.optString("title", "")
      if (id.isBlank() || title.isBlank()) return null

      val pageSourceId = obj.optString("pageSource", AutoScrollPageSource.READING.id)
      val pageSource = AutoScrollPageSource.entries.firstOrNull { it.id == pageSourceId } ?: AutoScrollPageSource.READING

      val sArr = obj.optJSONArray("sentences") ?: return null
      val sentences = buildList {
        for (i in 0 until sArr.length()) {
          val s = sArr.optJSONObject(i) ?: continue
          val text = s.optString("text", "")
          if (text.isNotBlank()) {
            add(
              AutoScrollSentenceSegment(
                index = i,
                text = text,
                translation = s.optString("translation", ""),
                speaker = s.optString("speaker", ""),
                estimatedDurationMs = s.optLong("estimatedDurationMs", 3000L)
              )
            )
          }
        }
      }
      if (sentences.isEmpty()) return null

      val vArr = obj.optJSONArray("vocabulary")
      val vocab = buildList {
        if (vArr != null) {
          for (i in 0 until vArr.length()) {
            val v = vArr.optJSONObject(i) ?: continue
            add(
              AutoScrollVocabExplanation(
                word = v.optString("word", ""),
                phonetic = v.optString("phonetic", ""),
                partOfSpeech = v.optString("partOfSpeech", ""),
                meaningInNative = v.optString("meaningInNative", ""),
                exampleSentence = v.optString("exampleSentence", "")
              )
            )
          }
        }
      }

      val qArr = obj.optJSONArray("questions")
      val questions = buildList {
        if (qArr != null) {
          for (i in 0 until qArr.length()) {
            val q = qArr.optJSONObject(i) ?: continue
            val oArr = q.optJSONArray("options") ?: continue
            val opts = buildList {
              for (j in 0 until oArr.length()) {
                add(oArr.optString(j, ""))
              }
            }
            if (opts.isNotEmpty()) {
              add(
                AutoScrollComprehensionQuestion(
                  question = q.optString("question", ""),
                  options = opts,
                  correctIndex = q.optInt("correctIndex", 0).coerceIn(0, opts.size - 1),
                  explanation = q.optString("explanation", "")
                )
              )
            }
          }
        }
      }

      val objArr = obj.optJSONArray("learningObjectives")
      val objectives = buildList {
        if (objArr != null) {
          for (i in 0 until objArr.length()) {
            val o = objArr.optString(i, "")
            if (o.isNotBlank()) add(o)
          }
        }
      }

      AutoScrollLearningContent(
        id = id,
        pageSource = pageSource,
        title = title,
        category = obj.optString("category", "Story"),
        topic = obj.optString("topic", ""),
        languageCode = obj.optString("languageCode", "en"),
        nativeLanguageCode = obj.optString("nativeLanguageCode", "en"),
        cefrLevel = obj.optString("cefrLevel", "B1"),
        contentLength = AutoScrollContentLength.fromId(obj.optString("contentLength", "medium")),
        fullText = obj.optString("fullText", sentences.joinToString(" ") { it.text }),
        sentences = sentences,
        vocabulary = vocab,
        questions = questions,
        learningObjectives = objectives,
        estimatedReadingDurationSec = obj.optInt("estimatedReadingDurationSec", 180),
        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
      )
    } catch (_: Exception) {
      null
    }
  }
}
