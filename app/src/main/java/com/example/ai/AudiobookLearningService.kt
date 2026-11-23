package com.example.ai

import android.content.Context
import com.example.R
import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.AudiobookBook
import com.example.data.model.AudiobookBookmark
import com.example.data.model.AudiobookEpisode
import com.example.data.model.BookContentType
import com.example.data.model.ChapterAiStudyPack
import com.example.data.model.Language
import com.example.data.model.ListeningDialogueLine
import com.example.data.model.UserAudiobookProgress
import com.example.data.repository.AudiobookCatalogRepository
import com.example.data.repository.GutendexAndLibriVoxService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class AudiobookGenerationResult {
  data class Success(val book: AudiobookBook) : AudiobookGenerationResult()
  data class Error(val userFriendlyMessage: String) : AudiobookGenerationResult()
}

/**
 * Service orchestrating:
 * 1. Gemini AI Smart Book Learning (chapter study packs, word/sentence explanations, speaking prompts)
 * 2. Gemini Dynamic Listening Generation ("Create My Listening")
 * 3. Complete User Library Persistence (Continue Listening, Saved Books, Recently Opened,
 *    Completed Books, Favorites, Bookmarks, Listening History)
 * 4. Smart Personalized Recommendations
 */
object AudiobookLearningService {

  private const val PREFS_NAME = "lingo_audiobook_library_prefs"
  private const val KEY_USER_LIBRARY_PREFIX = "user_audiobook_library_"
  private const val KEY_USER_FAVORITES_PREFIX = "user_audiobook_favorites_"
  private const val KEY_USER_RECENT_OPENED_PREFIX = "user_audiobook_recent_opened_"
  private const val KEY_USER_BOOKMARKS_PREFIX = "user_audiobook_bookmarks_"
  private const val KEY_USER_PROGRESS_PREFIX = "user_audiobook_progress_"
  private const val KEY_GENERATED_BOOKS_PREFIX = "user_generated_audiobooks_"
  private const val KEY_DISCOVERED_GUTENDEX_PREFIX = "discovered_gutendex_books_"
  private const val KEY_CHAPTER_AI_PACK_PREFIX = "chapter_ai_study_pack_"
  private const val KEY_SKIP_ANNOUNCEMENTS_PREFIX = "user_skip_recording_announcements_"

  fun isSkipRecordingAnnouncementsEnabled(context: Context, userId: String = "guest"): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_SKIP_ANNOUNCEMENTS_PREFIX + userId.ifBlank { "guest" }, true)
  }

  fun setSkipRecordingAnnouncementsEnabled(context: Context, userId: String = "guest", enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_SKIP_ANNOUNCEMENTS_PREFIX + userId.ifBlank { "guest" }, enabled).apply()
  }

  // =========================================================================
  // 1. GEMINI AI SMART BOOK LEARNING FOR REAL BOOK CHAPTERS (SECTION 8)
  // =========================================================================

  /**
   * Generates or loads a cached Gemini AI Smart Learning Pack for a specific book chapter.
   * Never sends an entire book to Gemini; uses a bounded excerpt of the selected chapter.
   */
  suspend fun generateChapterAiStudyPack(
    context: Context,
    bookTitle: String,
    bookAuthor: String,
    bookId: String,
    chapterIndex: Int,
    chapterTitle: String,
    chapterText: String,
    targetLanguage: Language,
    nativeLanguage: Language,
    cefrLevel: String
  ): ChapterAiStudyPack = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val cacheKey = "${KEY_CHAPTER_AI_PACK_PREFIX}${bookId}_${chapterIndex}_${targetLanguage.code}_$cefrLevel"
    prefs.getString(cacheKey, null)?.let { cachedJson ->
      parseChapterAiStudyPackJson(cachedJson, bookId, chapterIndex, chapterTitle)?.let {
        return@withContext it
      }
    }

    val hasTestHook = GeminiTutorService.testResponseInterceptor != null ||
      AiProviderManager.openAiBackendTestInterceptor != null ||
      GeminiService.testBypassDailyQuota
    val isPro = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement ||
      GeminiService.quotaUiState.value.isProUser
    if (!isPro && !hasTestHook) {
      val excerptFree = chapterText.take(2800).trim()
      return@withContext buildFallbackChapterStudyPack(
        bookId, chapterIndex, bookTitle, chapterTitle, excerptFree, targetLanguage, nativeLanguage
      )
    }

    // Take a clean, bounded excerpt from the chapter (max ~2,800 characters)
    val excerpt = chapterText.take(2800).trim()
    val cleanCefr = cefrLevel.uppercase().takeIf { it in listOf("A1", "A2", "B1", "B2", "C1", "C2") } ?: "B1"

    val prompt = """
      You are LingoSphere's AI Literature & Language Coach.
      The learner is reading and listening to "$bookTitle" by $bookAuthor, specifically "$chapterTitle".
      Target Learning Language: ${targetLanguage.name} (${targetLanguage.code}), CEFR Level: $cleanCefr.
      Learner's Native Explanation Language: ${nativeLanguage.name} (${nativeLanguage.code}).

      Chapter Excerpt:
      \"\"\"
      $excerpt
      \"\"\"

      Create an interactive language-learning study pack based directly on this chapter excerpt.
      Do NOT rewrite or replace the original book chapter.
      Return ONLY a raw JSON object (no markdown formatting) with this exact schema:
      {
        "summaryExplanation": "Clear 2-3 sentence explanation of what happens in this chapter in ${nativeLanguage.name} and ${targetLanguage.name}",
        "difficultWords": [
          {
            "word": "Vocabulary word from the chapter",
            "sentence": "Short example sentence from or inspired by the chapter in ${targetLanguage.name}",
            "translation": "Translation in ${nativeLanguage.name}",
            "meaning": "Clear explanation of the word in ${nativeLanguage.name}"
          }
        ],
        "grammarNotes": [
          "Grammar explanation 1 highlighting a structure used in this chapter",
          "Grammar explanation 2 suitable for CEFR $cleanCefr"
        ],
        "questions": [
          {
            "question": "Comprehension question about this chapter in ${targetLanguage.name}",
            "options": ["Correct Answer", "Distractor B", "Distractor C", "Distractor D"],
            "correctIndex": 0,
            "explanation": "Explanation in ${nativeLanguage.name}"
          }
        ],
        "speakingPrompt": "A speaking exercise asking the learner to describe what happened in this chapter in ${targetLanguage.name}",
        "conversationStarter": "An interactive roleplay question based on this chapter's events"
      }
    """.trimIndent()

    val rawOutput = GeminiTutorService.generateGeminiText(
      systemInstructionText = "You are a literary language tutor. Output valid raw JSON only.",
      userPromptText = prompt,
      temperature = 0.65f,
      featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT,
      incrementQuotaOnSuccess = false
    )

    if (!rawOutput.isNullOrBlank()) {
      val parsed = parseChapterAiStudyPackJson(rawOutput, bookId, chapterIndex, chapterTitle)
      if (parsed != null) {
        prefs.edit().putString(cacheKey, rawOutput).apply()
        return@withContext parsed
      }
    }

    // Resilient fallback study pack derived directly from the chapter text
    buildFallbackChapterStudyPack(bookId, chapterIndex, bookTitle, chapterTitle, excerpt, targetLanguage, nativeLanguage)
  }

  /**
   * Explains a tapped word, sentence, or paragraph from the book using Gemini AI.
   */
  suspend fun explainBookPassageWithAi(
    selectedText: String,
    bookTitle: String,
    chapterTitle: String,
    mode: String, // "translate", "grammar", "meaning", "vocabulary"
    targetLanguage: Language,
    nativeLanguage: Language,
    cefrLevel: String
  ): String = withContext(Dispatchers.IO) {
    val hasTestHook = GeminiTutorService.testResponseInterceptor != null ||
      AiProviderManager.openAiBackendTestInterceptor != null ||
      GeminiService.testBypassDailyQuota
    val isPro = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement ||
      GeminiService.quotaUiState.value.isProUser
    if (!isPro && !hasTestHook) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.AI_UPGRADE,
        featureKey = "ai.books.explain",
        headline = "Unlock AI learning",
        reason = "AI passage explanations and grammar breakdowns are included with LingoSphere Pro."
      )
      return@withContext "Unlock AI learning with LingoSphere Pro for instant translations, grammar notes, and vocabulary breakdowns."
    }
    val bounded = selectedText.take(650).trim()
    val taskInstruction = when (mode.lowercase()) {
      "translate" -> "Translate this passage accurately into ${nativeLanguage.name} and explain key nuances."
      "grammar" -> "Break down the grammar structure, verb tenses, and phrasing of this passage for a $cefrLevel learner."
      "vocabulary" -> "Identify the 3 most useful or challenging words in this passage, give their phonetic pronunciation, meaning in ${nativeLanguage.name}, and a simple example."
      else -> "Explain the meaning and literary context of this passage from '$bookTitle' ($chapterTitle) in simple terms."
    }

    val prompt = """
      Book: "$bookTitle" — $chapterTitle
      Passage: "$bounded"
      Task: $taskInstruction
      Keep your response concise, clear, and encouraging (3 to 5 bullet points or short sentences).
    """.trimIndent()

    val response = GeminiTutorService.generateGeminiText(
      systemInstructionText = "You are a helpful multilingual reading & listening coach.",
      userPromptText = prompt,
      temperature = 0.6f,
      featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT,
      incrementQuotaOnSuccess = false
    )

    response?.trim()?.takeIf { it.isNotBlank() }
      ?: "Passage from $bookTitle ($chapterTitle): \"$bounded\" — Tap 'Save to Flashcards' to review key vocabulary from this sentence."
  }

  private fun parseChapterAiStudyPackJson(
    rawJson: String,
    bookId: String,
    chapterIndex: Int,
    chapterTitle: String
  ): ChapterAiStudyPack? {
    return try {
      val cleaned = rawJson.replace("```json", "").replace("```", "").trim()
      val start = cleaned.indexOf('{')
      val end = cleaned.lastIndexOf('}')
      if (start < 0 || end <= start) return null
      val root = JSONObject(cleaned.substring(start, end + 1))

      val summary = root.optString("summaryExplanation", "").trim()
      val words = mutableListOf<ListeningDialogueLine>()
      val wArr = root.optJSONArray("difficultWords")
      if (wArr != null) {
        for (i in 0 until wArr.length()) {
          val wObj = wArr.optJSONObject(i) ?: continue
          val word = wObj.optString("word", "").trim()
          val sent = wObj.optString("sentence", word).trim()
          val trans = wObj.optString("translation", "").trim()
          val mean = wObj.optString("meaning", "").trim()
          if (word.isNotBlank()) {
            words.add(
              ListeningDialogueLine(
                speaker = "Vocabulary",
                targetText = sent,
                nativeTranslation = trans,
                keyVocabWord = word,
                keyVocabMeaning = mean.ifBlank { trans }
              )
            )
          }
        }
      }

      val grammar = mutableListOf<String>()
      val gArr = root.optJSONArray("grammarNotes")
      if (gArr != null) {
        for (i in 0 until gArr.length()) {
          val g = gArr.optString(i, "").trim()
          if (g.isNotBlank()) grammar.add(g)
        }
      }

      val questions = mutableListOf<AcademyComprehensionQuestion>()
      val qArr = root.optJSONArray("questions")
      if (qArr != null) {
        for (i in 0 until qArr.length()) {
          val qObj = qArr.optJSONObject(i) ?: continue
          val qText = qObj.optString("question", "").trim()
          val optsArr = qObj.optJSONArray("options")
          if (qText.isBlank() || optsArr == null || optsArr.length() < 2) continue
          val opts = buildList {
            for (j in 0 until optsArr.length()) {
              val o = optsArr.optString(j, "").trim()
              if (o.isNotBlank()) add(o)
            }
          }
          if (opts.size >= 2) {
            val cIdx = qObj.optInt("correctIndex", 0).coerceIn(0, opts.size - 1)
            val expl = qObj.optString("explanation", "").trim()
            questions.add(AcademyComprehensionQuestion(qText, opts, cIdx, expl))
          }
        }
      }

      val speakPrompt = root.optString("speakingPrompt", "Describe in your own words what happened in $chapterTitle.").trim()
      val convStarter = root.optString("conversationStarter", "What would you have done in this situation?").trim()

      ChapterAiStudyPack(
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        summaryExplanation = summary.ifBlank { "Key themes and events from $chapterTitle." },
        difficultWords = words,
        grammarNotes = grammar,
        comprehensionQuestions = questions,
        speakingPrompt = speakPrompt,
        conversationStarter = convStarter
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun buildFallbackChapterStudyPack(
    bookId: String,
    chapterIndex: Int,
    bookTitle: String,
    chapterTitle: String,
    excerpt: String,
    targetLanguage: Language,
    nativeLanguage: Language
  ): ChapterAiStudyPack {
    // Extract distinctive longer words from the actual chapter excerpt
    val candidateWords = excerpt
      .split(Regex("[^A-Za-zÀ-ÿ]+"))
      .map { it.trim() }
      .filter { it.length >= 7 }
      .distinctBy { it.lowercase() }
      .take(5)

    val vocabLines = candidateWords.map { w ->
      ListeningDialogueLine(
        speaker = "Chapter Word",
        targetText = "Featured in $chapterTitle of $bookTitle.",
        nativeTranslation = "Key literary term in ${targetLanguage.name}",
        keyVocabWord = w,
        keyVocabMeaning = "Literary vocabulary from $chapterTitle (${nativeLanguage.name} study note)"
      )
    }

    return ChapterAiStudyPack(
      bookId = bookId,
      chapterIndex = chapterIndex,
      chapterTitle = chapterTitle,
      summaryExplanation = "In $chapterTitle of $bookTitle, focus on the narrative progression, character dialogue, and descriptive vocabulary.",
      difficultWords = vocabLines,
      grammarNotes = listOf(
        "Notice how narrative past tenses establish the sequence of events in $chapterTitle.",
        "Pay attention to descriptive clauses and connecting phrases that link paragraphs smoothly."
      ),
      comprehensionQuestions = listOf(
        AcademyComprehensionQuestion(
          question = "What is the primary focus of $chapterTitle in $bookTitle?",
          options = listOf(
            "Advancing the central storyline and character perspective",
            "Providing a technical list of mathematical equations",
            "Unrelated modern sports commentary",
            "A culinary recipe guide"
          ),
          correctIndex = 0,
          explanation = "$chapterTitle advances the narrative and develops the key characters of $bookTitle."
        )
      ),
      speakingPrompt = "Summarize what happened in $chapterTitle of $bookTitle in 2 or 3 spoken sentences.",
      conversationStarter = "Which character or idea stood out to you most in $chapterTitle, and why?"
    )
  }

  // =========================================================================
  // 2. GEMINI DYNAMIC CONTENT GENERATOR ("CREATE MY LISTENING" - SECTION 11)
  // =========================================================================

  suspend fun generateFreshAudiobook(
    context: Context,
    userId: String,
    targetLanguage: Language,
    nativeLanguage: Language,
    cefrLevel: String,
    primaryCategory: String,
    subCategory: String,
    topicOrBookTitle: String,
    contentStyle: String = "Lesson", // Story, Lesson, Conversation, Speech
    durationMinutes: Int = 10,
    isPremiumUser: Boolean = false
  ): AudiobookGenerationResult = withContext(Dispatchers.IO) {
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
        featureKey = "ai.audiobook.generate",
        headline = "Unlock AI learning",
        reason = msg
      )
      return@withContext AudiobookGenerationResult.Error(msg)
    }
    val cleanCefr = cefrLevel.uppercase().takeIf { it in listOf("A1", "A2", "B1", "B2", "C1", "C2") } ?: "B1"

    val prompt = """
      You are LingoSphere's master educational audiobook producer and language pedagogue.
      Create an original, inspiring educational audiobook learning package in ${targetLanguage.name} (${targetLanguage.code}) at CEFR level $cleanCefr.
      Target Audience Explanation Language: ${nativeLanguage.name} (${nativeLanguage.code}).
      Primary Category: "$primaryCategory"
      Subcategory / Theme: "$subCategory"
      Specific Requested Topic: "$topicOrBookTitle"
      Content Style: $contentStyle
      Target Duration: $durationMinutes minutes
      Unique Generation Seed: ${System.currentTimeMillis()}
      
      IMPORTANT EDUCATIONAL & COPYRIGHT COMPLIANCE:
      - If "$topicOrBookTitle" refers to a copyrighted non-fiction book, do NOT reproduce or fabricate full book text.
      - Instead, create an original "Educational Summary" breaking down key philosophical insights, practical habits, real-world examples, and language-learning vocabulary inspired by those general ideas.
      - If it is an original theme or topic (e.g., "How to become successful", "How to manage money", "A mystery story"), generate a fresh, original educational listening experience.
      
      Return ONLY a single valid raw JSON object (NO markdown backticks, NO extra commentary) with this exact schema:
      {
        "title": "Engaging Title in ${targetLanguage.name} or English",
        "author": "LingoSphere Original Audio",
        "primaryCategory": "$primaryCategory",
        "subCategory": "$subCategory",
        "shortDescription": "2-3 sentence overview of this listening journey in ${nativeLanguage.name}",
        "keyTakeaways": [
          "Key Takeaway 1 in ${nativeLanguage.name}",
          "Key Takeaway 2 in ${nativeLanguage.name}",
          "Key Takeaway 3 in ${nativeLanguage.name}"
        ],
        "episodes": [
          {
            "episodeNumber": 1,
            "title": "Episode 1: Title in ${targetLanguage.name}",
            "subtitle": "Subtitle explaining the episode focus in ${nativeLanguage.name}",
            "estimatedMinutes": 3,
            "lines": [
              {
                "speaker": "Narrator",
                "targetText": "Spoken sentence in ${targetLanguage.name}.",
                "nativeTranslation": "Accurate translation in ${nativeLanguage.name}.",
                "keyVocabWord": "Key word from sentence",
                "keyVocabMeaning": "Clear definition in ${nativeLanguage.name}"
              }
            ],
            "questions": [
              {
                "question": "Comprehension question in ${nativeLanguage.name}",
                "options": ["Correct Answer", "Option B", "Option C", "Option D"],
                "correctIndex": 0,
                "explanation": "Why this answer is correct in ${nativeLanguage.name}"
              }
            ],
            "speakingPracticePrompt": "A speaking challenge prompt for the learner in ${nativeLanguage.name}",
            "practicalApplicationTip": "A practical life action tip in ${nativeLanguage.name}"
          }
        ]
      }
      
      Requirements:
      1. Provide between 2 and 4 structured episodes (e.g. Introduction, Core Ideas, Real-World Practice, Final Review).
      2. Each episode must have 5 to 9 narration lines with natural, authentic ${targetLanguage.name} phrasing suitable for CEFR $cleanCefr.
      3. Provide at least 1 comprehension question with 4 options per episode.
      4. Include an interactive speaking practice prompt and a practical application tip.
    """.trimIndent()

    val rawOutput = GeminiTutorService.generateGeminiText(
      systemInstructionText = "You are an elite educational audiobook narrator and curriculum designer. Output raw JSON only.",
      userPromptText = prompt,
      temperature = 0.75f,
      featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT,
      incrementQuotaOnSuccess = true
    )

    if (rawOutput.isNullOrBlank()) {
      return@withContext AudiobookGenerationResult.Error("Unable to create your audiobook right now. Please check your connection and tap Retry.")
    }

    val parsedBook = parseGeneratedAudiobookJson(
      rawJson = rawOutput,
      defaultTitle = topicOrBookTitle,
      defaultCategory = primaryCategory,
      defaultSubCategory = subCategory,
      targetLangCode = targetLanguage.code,
      cefrLevel = cleanCefr
    )

    if (parsedBook == null || parsedBook.episodes.isEmpty()) {
      return@withContext AudiobookGenerationResult.Error("The audio script could not be completed. Please tap Retry to generate a fresh lesson.")
    }

    saveGeneratedBook(context, userId, parsedBook)
    addBookToLibrary(context, userId, parsedBook.id)
    recordBookOpened(context, userId, parsedBook)

    AudiobookGenerationResult.Success(parsedBook)
  }

  private fun parseGeneratedAudiobookJson(
    rawJson: String,
    defaultTitle: String,
    defaultCategory: String,
    defaultSubCategory: String,
    targetLangCode: String,
    cefrLevel: String
  ): AudiobookBook? {
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

      val title = root.optString("title", defaultTitle).trim().ifBlank { defaultTitle }
      val author = root.optString("author", "LingoSphere Original Edition").trim()
      val primaryCategory = root.optString("primaryCategory", defaultCategory).trim().ifBlank { defaultCategory }
      val subCategory = root.optString("subCategory", defaultSubCategory).trim().ifBlank { defaultSubCategory }
      val shortDescription = root.optString("shortDescription", "An original educational audio lesson.").trim()

      val takeaways = mutableListOf<String>()
      val tArr = root.optJSONArray("keyTakeaways")
      if (tArr != null) {
        for (i in 0 until tArr.length()) {
          val item = tArr.optString(i, "").trim()
          if (item.isNotBlank()) takeaways.add(item)
        }
      }

      val episodesArr = root.optJSONArray("episodes") ?: return null
      val episodes = mutableListOf<AudiobookEpisode>()

      for (i in 0 until episodesArr.length()) {
        val epObj = episodesArr.optJSONObject(i) ?: continue
        val epNum = epObj.optInt("episodeNumber", i + 1)
        val epTitle = epObj.optString("title", "Episode ${i + 1}").trim()
        val epSubtitle = epObj.optString("subtitle", "").trim()
        val epMins = epObj.optInt("estimatedMinutes", 4).coerceIn(2, 20)
        val speakPrompt = epObj.optString("speakingPracticePrompt", "").trim()
        val tip = epObj.optString("practicalApplicationTip", "").trim()

        val linesArr = epObj.optJSONArray("lines")
        val lines = mutableListOf<ListeningDialogueLine>()
        if (linesArr != null) {
          for (j in 0 until linesArr.length()) {
            val lObj = linesArr.optJSONObject(j) ?: continue
            val spk = lObj.optString("speaker", "Narrator").trim()
            val tgt = lObj.optString("targetText", "").trim()
            if (tgt.isBlank()) continue
            val trans = lObj.optString("nativeTranslation", "").trim()
            val vocab = lObj.optString("keyVocabWord", "").trim()
            val meaning = lObj.optString("keyVocabMeaning", "").trim()
            lines.add(ListeningDialogueLine(spk, tgt, trans, vocab, meaning))
          }
        }

        val qArr = epObj.optJSONArray("questions")
        val questions = mutableListOf<AcademyComprehensionQuestion>()
        if (qArr != null) {
          for (k in 0 until qArr.length()) {
            val qObj = qArr.optJSONObject(k) ?: continue
            val qText = qObj.optString("question", "").trim()
            val optsArr = qObj.optJSONArray("options")
            if (qText.isBlank() || optsArr == null || optsArr.length() < 2) continue
            val opts = buildList {
              for (m in 0 until optsArr.length()) {
                val opt = optsArr.optString(m, "").trim()
                if (opt.isNotBlank()) add(opt)
              }
            }
            if (opts.size >= 2) {
              val cIdx = qObj.optInt("correctIndex", 0).coerceIn(0, opts.size - 1)
              val expl = qObj.optString("explanation", "").trim()
              questions.add(AcademyComprehensionQuestion(qText, opts, cIdx, expl))
            }
          }
        }

        if (lines.isNotEmpty()) {
          episodes.add(
            AudiobookEpisode(
              id = "ep_${System.currentTimeMillis()}_${i + 1}",
              episodeNumber = epNum,
              title = epTitle,
              subtitle = epSubtitle,
              estimatedMinutes = epMins,
              narrationLines = lines,
              comprehensionQuestions = questions,
              speakingPracticePrompt = speakPrompt,
              practicalApplicationTip = tip
            )
          )
        }
      }

      val totalMins = episodes.sumOf { it.estimatedMinutes }.coerceAtLeast(6)
      val colors = when {
        primaryCategory.contains("Money", ignoreCase = true) || primaryCategory.contains("Wealth", ignoreCase = true) ->
          listOf(0xFF064E3B, 0xFF047857)
        primaryCategory.contains("Psychology", ignoreCase = true) ->
          listOf(0xFF3B0764, 0xFF7C3AED)
        primaryCategory.contains("Knowledge", ignoreCase = true) || primaryCategory.contains("Science", ignoreCase = true) ->
          listOf(0xFF1E3A8A, 0xFF2563EB)
        primaryCategory.contains("Literature", ignoreCase = true) || primaryCategory.contains("Stories", ignoreCase = true) ->
          listOf(0xFF164E63, 0xFF0891B2)
        else ->
          listOf(0xFF1C1917, 0xFFD97706)
      }

      AudiobookBook(
        id = "gen_book_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
        title = title,
        author = author,
        primaryCategory = primaryCategory,
        subCategory = subCategory,
        coverGradientColors = colors,
        shortDescription = shortDescription,
        keyTakeaways = takeaways,
        contentType = BookContentType.AI_LEARNING_SUMMARY,
        targetLanguageCode = targetLangCode,
        defaultCefrLevel = cefrLevel,
        estimatedTotalMinutes = totalMins,
        episodes = episodes
      )
    } catch (_: Exception) {
      null
    }
  }

  // =========================================================================
  // 3. COMPLETE MY LIBRARY & USER PROGRESS PERSISTENCE (SECTION 13)
  // =========================================================================

  fun getAllAvailableBooks(context: Context, userId: String): List<AudiobookBook> {
    val curated = AudiobookCatalogRepository.CURATED_BOOKS
    val generated = loadGeneratedBooks(context, userId)
    val discovered = loadDiscoveredGutendexBooks(context, userId)
    val indexed = GutendexAndLibriVoxService.getIndexedCatalogBooks()
    return (generated + curated + discovered + indexed).distinctBy { it.id }
  }

  fun getBookById(context: Context, userId: String, bookId: String): AudiobookBook? {
    val normalizedId = if (bookId == "gutenberg_398") "wattles_science_getting_rich" else bookId
    return getAllAvailableBooks(context, userId).firstOrNull { it.id == normalizedId }
  }

  /**
   * Caches a Gutendex book metadata entry locally so it persists in My Library / History across restarts.
   */
  fun cacheDiscoveredBook(context: Context, userId: String, book: AudiobookBook) {
    GutendexAndLibriVoxService.indexDiscoveredBooks(listOf(book))
    if (book.gutendexId == null && !book.id.startsWith("librivox_")) return
    if (book.gutendexId == 398 && book.title.contains("Science of Getting Rich", ignoreCase = true)) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = loadDiscoveredGutendexBooks(context, userId).toMutableList()
    existing.removeAll { it.id == book.id }
    existing.add(0, book)

    val arr = JSONArray()
    existing.take(250).forEach { b ->
      val obj = JSONObject().apply {
        put("id", b.id)
        put("gutendexId", b.gutendexId ?: 0)
        put("title", b.title)
        put("author", b.author)
        put("primaryCategory", b.primaryCategory)
        put("subCategory", b.subCategory)
        put("coverArtUrl", b.coverArtUrl)
        put("shortDescription", b.shortDescription)
        put("fullTextUrl", b.fullTextUrl ?: "")
        put("downloadCount", b.downloadCount)
        put("targetLanguageCode", b.targetLanguageCode)
        put("defaultCefrLevel", b.defaultCefrLevel)
        put("contentType", b.contentType.name)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_DISCOVERED_GUTENDEX_PREFIX + userId, arr.toString()).apply()
  }

  private fun loadDiscoveredGutendexBooks(context: Context, userId: String): List<AudiobookBook> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_DISCOVERED_GUTENDEX_PREFIX + userId, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val gId = obj.optInt("gutendexId", 0).takeIf { it > 0 } ?: continue
          val savedTitle = obj.optString("title", "Classic Book")
          // Skip legacy mismatched entry where Gutenberg 398 was paired with The Science of Getting Rich
          if (gId == 398 && savedTitle.contains("Science of Getting Rich", ignoreCase = true)) continue
          val cType = try {
            BookContentType.valueOf(obj.optString("contentType", BookContentType.FULL_PUBLIC_DOMAIN_BOOK.name))
          } catch (_: Exception) {
            BookContentType.FULL_PUBLIC_DOMAIN_BOOK
          }
          add(
            AudiobookBook(
              id = obj.optString("id", "gutenberg_$gId"),
              gutendexId = gId,
              title = savedTitle,
              author = obj.optString("author", "Classic Author"),
              primaryCategory = obj.optString("primaryCategory", "Stories"),
              subCategory = obj.optString("subCategory", "Classics"),
              coverArtUrl = obj.optString("coverArtUrl", "https://www.gutenberg.org/cache/epub/$gId/pg$gId.cover.medium.jpg"),
              shortDescription = obj.optString("shortDescription", ""),
              fullTextUrl = obj.optString("fullTextUrl", "https://www.gutenberg.org/ebooks/$gId.txt.utf-8"),
              hasFullTextAvailable = true,
              downloadCount = obj.optInt("downloadCount", 0),
              targetLanguageCode = obj.optString("targetLanguageCode", "en"),
              defaultCefrLevel = obj.optString("defaultCefrLevel", "B2"),
              contentType = cType
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // --- Saved Books ---
  fun getUserLibraryBookIds(context: Context, userId: String): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(KEY_USER_LIBRARY_PREFIX + userId, null)
    return set ?: setOf("gutenberg_1342", "gutenberg_1661", "book_atomic_habits", "book_rich_dad_poor_dad")
  }

  fun addBookToLibrary(context: Context, userId: String, bookId: String, book: AudiobookBook? = null) {
    book?.let { cacheDiscoveredBook(context, userId, it) }
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = getUserLibraryBookIds(context, userId).toMutableSet()
    current.add(bookId)
    prefs.edit().putStringSet(KEY_USER_LIBRARY_PREFIX + userId, current).apply()
  }

  fun removeBookFromLibrary(context: Context, userId: String, bookId: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = getUserLibraryBookIds(context, userId).toMutableSet()
    current.remove(bookId)
    prefs.edit().putStringSet(KEY_USER_LIBRARY_PREFIX + userId, current).apply()
  }

  fun isBookInLibrary(context: Context, userId: String, bookId: String): Boolean {
    return getUserLibraryBookIds(context, userId).contains(bookId)
  }

  fun getSavedBooks(context: Context, userId: String): List<AudiobookBook> {
    val ids = getUserLibraryBookIds(context, userId)
    return getAllAvailableBooks(context, userId).filter { it.id in ids }
  }

  // --- Favorites ---
  fun getFavoriteBookIds(context: Context, userId: String): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_USER_FAVORITES_PREFIX + userId, emptySet()) ?: emptySet()
  }

  fun toggleFavoriteBook(context: Context, userId: String, book: AudiobookBook): Boolean {
    cacheDiscoveredBook(context, userId, book)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = getFavoriteBookIds(context, userId).toMutableSet()
    val nowFav = if (current.contains(book.id)) {
      current.remove(book.id)
      false
    } else {
      current.add(book.id)
      true
    }
    prefs.edit().putStringSet(KEY_USER_FAVORITES_PREFIX + userId, current).apply()
    return nowFav
  }

  fun isBookFavorite(context: Context, userId: String, bookId: String): Boolean {
    return getFavoriteBookIds(context, userId).contains(bookId)
  }

  fun getFavoriteBooks(context: Context, userId: String): List<AudiobookBook> {
    val favIds = getFavoriteBookIds(context, userId)
    return getAllAvailableBooks(context, userId).filter { it.id in favIds }
  }

  // --- Recently Opened ---
  fun recordBookOpened(context: Context, userId: String, book: AudiobookBook) {
    cacheDiscoveredBook(context, userId, book)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_USER_RECENT_OPENED_PREFIX + userId, "") ?: ""
    val current = raw.split(",").map { it.trim() }.filter { it.isNotBlank() && it != book.id && it != "gutenberg_398" }.toMutableList()
    current.add(0, book.id)
    prefs.edit().putString(KEY_USER_RECENT_OPENED_PREFIX + userId, current.take(30).joinToString(",")).apply()
  }

  fun getRecentlyOpenedBooks(context: Context, userId: String): List<AudiobookBook> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_USER_RECENT_OPENED_PREFIX + userId, "") ?: ""
    val ids = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.map {
      if (it == "gutenberg_398") "wattles_science_getting_rich" else it
    }.distinct()
    val allMap = getAllAvailableBooks(context, userId).associateBy { it.id }
    val list = ids.mapNotNull { allMap[it] }
    return list.ifEmpty { AudiobookCatalogRepository.FAMOUS_FULL_BOOKS.take(4) }
  }

  // --- Bookmarks ---
  fun addBookmark(context: Context, userId: String, bookmark: AudiobookBookmark) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = getBookmarksForUser(context, userId).toMutableList()
    existing.removeAll { it.bookId == bookmark.bookId && it.chapterIndex == bookmark.chapterIndex && it.sentenceIndex == bookmark.sentenceIndex }
    existing.add(0, bookmark)
    saveBookmarksList(prefs, userId, existing)
    syncBookmarkToFirebase(context, userId, bookmark)
  }

  fun removeBookmark(context: Context, userId: String, bookmarkId: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = getBookmarksForUser(context, userId).filterNot { it.id == bookmarkId }
    saveBookmarksList(prefs, userId, existing)
    deleteBookmarkFromFirebase(context, userId, bookmarkId)
  }

  fun getBookmarksForUser(context: Context, userId: String): List<AudiobookBookmark> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_USER_BOOKMARKS_PREFIX + userId, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            AudiobookBookmark(
              id = obj.optString("id", "bm_$i"),
              bookId = obj.optString("bookId", ""),
              bookTitle = obj.optString("bookTitle", ""),
              chapterIndex = obj.optInt("chapterIndex", 0),
              chapterTitle = obj.optString("chapterTitle", ""),
              sentenceIndex = obj.optInt("sentenceIndex", 0),
              positionMs = obj.optLong("positionMs", 0L),
              snippetText = obj.optString("snippetText", ""),
              note = obj.optString("note", ""),
              createdAtTimestamp = obj.optLong("createdAtTimestamp", 0L)
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun getBookmarksForBook(context: Context, userId: String, bookId: String): List<AudiobookBookmark> {
    return getBookmarksForUser(context, userId).filter { it.bookId == bookId }
  }

  private fun saveBookmarksList(
    prefs: android.content.SharedPreferences,
    userId: String,
    bookmarks: List<AudiobookBookmark>
  ) {
    val arr = JSONArray()
    bookmarks.take(100).forEach { bm ->
      val obj = JSONObject().apply {
        put("id", bm.id)
        put("bookId", bm.bookId)
        put("bookTitle", bm.bookTitle)
        put("chapterIndex", bm.chapterIndex)
        put("chapterTitle", bm.chapterTitle)
        put("sentenceIndex", bm.sentenceIndex)
        put("positionMs", bm.positionMs)
        put("snippetText", bm.snippetText)
        put("note", bm.note)
        put("createdAtTimestamp", bm.createdAtTimestamp)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_USER_BOOKMARKS_PREFIX + userId, arr.toString()).apply()
  }

  private val firebaseSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private fun sanitizeFirestoreDocId(raw: String): String {
    val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_')
    return cleaned.take(120).ifBlank { "book_item" }
  }

  private fun getSignedInFirestoreOrNull(context: Context, userId: String): Pair<FirebaseFirestore, String>? {
    if (userId.isBlank() || userId == "guest") return null
    return try {
      val uid = Firebase.auth.currentUser?.uid
      if (uid.isNullOrBlank() || uid != userId) return null
      val dbId = context.applicationContext.getString(R.string.firestore_database_id)
      val db = FirebaseFirestore.getInstance(dbId)
      db to uid
    } catch (_: Throwable) {
      null
    }
  }

  fun syncBookmarkToFirebase(context: Context, userId: String, bookmark: AudiobookBookmark) {
    val pair = getSignedInFirestoreOrNull(context, userId) ?: return
    val (db, uid) = pair
    val docId = sanitizeFirestoreDocId(bookmark.id)
    firebaseSyncScope.launch {
      try {
        val payload = mapOf(
          "userId" to uid,
          "bookId" to sanitizeFirestoreDocId(bookmark.bookId),
          "bookTitle" to bookmark.bookTitle.take(240).ifBlank { "Book" },
          "chapterIndex" to bookmark.chapterIndex.coerceIn(0, 5000),
          "chapterTitle" to bookmark.chapterTitle.take(240),
          "sentenceIndex" to bookmark.sentenceIndex.coerceIn(0, 1_000_000),
          "positionMs" to bookmark.positionMs.coerceIn(0L, 86_400_000L),
          "snippetText" to bookmark.snippetText.take(600),
          "note" to bookmark.note.take(500),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
          .collection("book_bookmarks").document(docId)
          .set(payload, SetOptions.merge())
          .await()
      } catch (ce: CancellationException) {
        throw ce
      } catch (_: Throwable) {}
    }
  }

  fun deleteBookmarkFromFirebase(context: Context, userId: String, bookmarkId: String) {
    val pair = getSignedInFirestoreOrNull(context, userId) ?: return
    val (db, uid) = pair
    val docId = sanitizeFirestoreDocId(bookmarkId)
    firebaseSyncScope.launch {
      try {
        db.collection("users").document(uid)
          .collection("book_bookmarks").document(docId)
          .delete()
          .await()
      } catch (ce: CancellationException) {
        throw ce
      } catch (_: Throwable) {}
    }
  }

  /**
   * Saves READING progress independently of listening progress (locally + Firebase Firestore
   * under users/{uid}/book_reading_progress/{bookId}). Never overwrites listening chapter/position.
   */
  fun saveReadingProgress(
    context: Context,
    userId: String,
    bookId: String,
    bookTitle: String = "",
    chapterIndex: Int,
    scrollOffset: Int,
    pageIndex: Int,
    progressPercent: Float,
    readingTheme: String,
    fontSizeSp: Float,
    isPageMode: Boolean,
    completedReadingChapters: Set<Int>? = null
  ): UserAudiobookProgress {
    val existing = loadUserProgress(context, userId, bookId)
    val clampedPercent = progressPercent.coerceIn(0f, 1f)
    val mergedCompletedReading = completedReadingChapters ?: existing.completedReadingChapterIndices
    val now = System.currentTimeMillis()
    val updated = existing.copy(
      readingChapterIndex = chapterIndex.coerceAtLeast(0),
      readingScrollOffset = scrollOffset.coerceAtLeast(0),
      readingPageIndex = pageIndex.coerceAtLeast(0),
      readingPercent = maxOf(existing.readingPercent, clampedPercent),
      readingTheme = readingTheme,
      readingFontSizeSp = fontSizeSp.coerceIn(13f, 28f),
      isPageMode = isPageMode,
      completedReadingChapterIndices = mergedCompletedReading,
      lastReadAtTimestamp = now
    )
    saveUserProgress(context, userId, updated)

    val pair = getSignedInFirestoreOrNull(context, userId)
    if (pair != null) {
      val (db, uid) = pair
      val docId = sanitizeFirestoreDocId(bookId)
      firebaseSyncScope.launch {
        try {
          val payload = buildMap<String, Any> {
            put("userId", uid)
            put("bookId", docId)
            if (bookTitle.isNotBlank()) put("bookTitle", bookTitle.take(240))
            put("chapterIndex", updated.readingChapterIndex.coerceIn(0, 5000))
            put("scrollOffset", updated.readingScrollOffset.coerceIn(0, 1_000_000))
            put("pageIndex", updated.readingPageIndex.coerceIn(0, 50_000))
            put("progressPercent", updated.readingPercent.toDouble().coerceIn(0.0, 1.0))
            put("readingTheme", updated.readingTheme.takeIf { it in listOf("LIGHT", "SEPIA", "DARK") } ?: "DARK")
            put("fontSizeSp", updated.readingFontSizeSp.toDouble().coerceIn(12.0, 32.0))
            put("isPageMode", updated.isPageMode)
            put("completedChapters", updated.completedReadingChapterIndices.sorted().take(1000))
            put("updatedAt", FieldValue.serverTimestamp())
          }
          db.collection("users").document(uid)
            .collection("book_reading_progress").document(docId)
            .set(payload, SetOptions.merge())
            .await()
        } catch (ce: CancellationException) {
          throw ce
        } catch (_: Throwable) {}
      }
    }
    return updated
  }

  /**
   * Saves LISTENING progress independently of reading progress (locally + Firebase Firestore
   * under users/{uid}/book_listening_progress/{bookId}). Never overwrites reading chapter/page/scroll.
   */
  fun saveListeningProgress(
    context: Context,
    userId: String,
    bookId: String,
    bookTitle: String = "",
    chapterIndex: Int,
    sentenceIndex: Int,
    positionMs: Long,
    progressPercent: Float,
    preferredEditionId: String,
    autoPlayNextChapter: Boolean,
    skipRecordingAnnouncements: Boolean,
    completedChapterIndices: Set<Int>,
    chapterProgressMap: Map<Int, Float>,
    chapterAudioPositionMsMap: Map<Int, Long>,
    chapterSentenceIndexMap: Map<Int, Int>,
    isCompleted: Boolean
  ): UserAudiobookProgress {
    val existing = loadUserProgress(context, userId, bookId)
    val clampedPercent = progressPercent.coerceIn(0f, 1f)
    val now = System.currentTimeMillis()
    val updated = existing.copy(
      lastEpisodeIndex = chapterIndex.coerceAtLeast(0),
      lastSentenceIndex = sentenceIndex.coerceAtLeast(0),
      lastPlaybackPositionMs = positionMs.coerceAtLeast(0L),
      listeningPercent = if (isCompleted) 1f else maxOf(existing.listeningPercent, clampedPercent),
      preferredEditionId = preferredEditionId,
      autoPlayNextChapter = autoPlayNextChapter,
      skipRecordingAnnouncements = skipRecordingAnnouncements,
      completedChapterIndices = completedChapterIndices,
      chapterProgressMap = chapterProgressMap,
      chapterAudioPositionMsMap = chapterAudioPositionMsMap,
      chapterSentenceIndexMap = chapterSentenceIndexMap,
      isCompleted = existing.isCompleted || isCompleted,
      lastListenedAtTimestamp = now
    )
    saveUserProgress(context, userId, updated)

    val pair = getSignedInFirestoreOrNull(context, userId)
    if (pair != null) {
      val (db, uid) = pair
      val docId = sanitizeFirestoreDocId(bookId)
      firebaseSyncScope.launch {
        try {
          val payload = buildMap<String, Any> {
            put("userId", uid)
            put("bookId", docId)
            if (bookTitle.isNotBlank()) put("bookTitle", bookTitle.take(240))
            put("chapterIndex", updated.lastEpisodeIndex.coerceIn(0, 5000))
            put("sentenceIndex", updated.lastSentenceIndex.coerceIn(0, 1_000_000))
            put("positionMs", updated.lastPlaybackPositionMs.coerceIn(0L, 86_400_000L))
            put("progressPercent", updated.listeningPercent.toDouble().coerceIn(0.0, 1.0))
            put("preferredEditionId", updated.preferredEditionId.take(64))
            put("autoPlayNextChapter", updated.autoPlayNextChapter)
            put("isCompleted", updated.isCompleted)
            put("completedChapters", updated.completedChapterIndices.sorted().take(1000))
            put("updatedAt", FieldValue.serverTimestamp())
          }
          db.collection("users").document(uid)
            .collection("book_listening_progress").document(docId)
            .set(payload, SetOptions.merge())
            .await()
        } catch (ce: CancellationException) {
          throw ce
        } catch (_: Throwable) {}
      }
    }
    return updated
  }

  /**
   * Hydrates separate reading & listening progress from Firebase Firestore and merges any newer
   * remote state into local storage without ever erasing existing local progress.
   */
  suspend fun hydrateBookProgressFromFirebase(
    context: Context,
    userId: String,
    bookId: String
  ): UserAudiobookProgress = withContext(Dispatchers.IO) {
    var merged = loadUserProgress(context, userId, bookId)
    val pair = getSignedInFirestoreOrNull(context, userId) ?: return@withContext merged
    val (db, uid) = pair
    val docId = sanitizeFirestoreDocId(bookId)

    try {
      val readDoc = db.collection("users").document(uid)
        .collection("book_reading_progress").document(docId)
        .get().await()
      if (readDoc.exists()) {
        val remoteReadTs = readDoc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        if (remoteReadTs >= merged.lastReadAtTimestamp) {
          val remoteCompleted = (readDoc.get("completedChapters") as? List<*>)
            ?.mapNotNull { (it as? Number)?.toInt() }
            ?.toSet() ?: emptySet()
          merged = merged.copy(
            readingChapterIndex = (readDoc.get("chapterIndex") as? Number)?.toInt() ?: merged.readingChapterIndex,
            readingScrollOffset = (readDoc.get("scrollOffset") as? Number)?.toInt() ?: merged.readingScrollOffset,
            readingPageIndex = (readDoc.get("pageIndex") as? Number)?.toInt() ?: merged.readingPageIndex,
            readingPercent = (readDoc.get("progressPercent") as? Number)?.toFloat()?.coerceIn(0f, 1f) ?: merged.readingPercent,
            readingTheme = readDoc.getString("readingTheme") ?: merged.readingTheme,
            readingFontSizeSp = (readDoc.get("fontSizeSp") as? Number)?.toFloat()?.coerceIn(13f, 28f) ?: merged.readingFontSizeSp,
            isPageMode = readDoc.getBoolean("isPageMode") ?: merged.isPageMode,
            completedReadingChapterIndices = merged.completedReadingChapterIndices + remoteCompleted,
            lastReadAtTimestamp = maxOf(merged.lastReadAtTimestamp, remoteReadTs)
          )
        }
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (_: Throwable) {}

    try {
      val listenDoc = db.collection("users").document(uid)
        .collection("book_listening_progress").document(docId)
        .get().await()
      if (listenDoc.exists()) {
        val remoteListenTs = listenDoc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        if (remoteListenTs >= merged.lastListenedAtTimestamp || (merged.lastPlaybackPositionMs == 0L && merged.lastSentenceIndex == 0)) {
          val remoteCompleted = (listenDoc.get("completedChapters") as? List<*>)
            ?.mapNotNull { (it as? Number)?.toInt() }
            ?.toSet() ?: emptySet()
          merged = merged.copy(
            lastEpisodeIndex = (listenDoc.get("chapterIndex") as? Number)?.toInt() ?: merged.lastEpisodeIndex,
            lastSentenceIndex = (listenDoc.get("sentenceIndex") as? Number)?.toInt() ?: merged.lastSentenceIndex,
            lastPlaybackPositionMs = (listenDoc.get("positionMs") as? Number)?.toLong() ?: merged.lastPlaybackPositionMs,
            listeningPercent = (listenDoc.get("progressPercent") as? Number)?.toFloat()?.coerceIn(0f, 1f) ?: merged.listeningPercent,
            preferredEditionId = listenDoc.getString("preferredEditionId") ?: merged.preferredEditionId,
            autoPlayNextChapter = listenDoc.getBoolean("autoPlayNextChapter") ?: merged.autoPlayNextChapter,
            isCompleted = merged.isCompleted || (listenDoc.getBoolean("isCompleted") == true),
            completedChapterIndices = merged.completedChapterIndices + remoteCompleted,
            lastListenedAtTimestamp = maxOf(merged.lastListenedAtTimestamp, remoteListenTs)
          )
        }
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (_: Throwable) {}

    saveUserProgress(context, userId, merged)
    merged
  }

  // --- Progress, Continue Listening & Completed Books ---
  fun saveUserProgress(context: Context, userId: String, progress: UserAudiobookProgress) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = JSONObject().apply {
      put("bookId", progress.bookId)
      put("lastEpisodeIndex", progress.lastEpisodeIndex)
      put("lastSentenceIndex", progress.lastSentenceIndex)
      put("lastPlaybackPositionMs", progress.lastPlaybackPositionMs)
      put("readingChapterIndex", progress.readingChapterIndex)
      put("readingScrollOffset", progress.readingScrollOffset)
      put("readingPageIndex", progress.readingPageIndex)
      put("readingPercent", progress.readingPercent.toDouble())
      put("listeningPercent", progress.listeningPercent.toDouble())
      put("isCompleted", progress.isCompleted)
      put("isFavorite", progress.isFavorite)
      put("preferredEditionId", progress.preferredEditionId)
      put("autoPlayNextChapter", progress.autoPlayNextChapter)
      put("skipRecordingAnnouncements", progress.skipRecordingAnnouncements)
      put("readingTheme", progress.readingTheme)
      put("readingFontSizeSp", progress.readingFontSizeSp.toDouble())
      put("isPageMode", progress.isPageMode)
      put("completedExercisesCount", progress.completedExercisesCount)
      put("lastReadAtTimestamp", progress.lastReadAtTimestamp)
      put("lastListenedAtTimestamp", progress.lastListenedAtTimestamp)
      val compArr = JSONArray()
      progress.completedChapterIndices.forEach { compArr.put(it) }
      put("completedChapterIndices", compArr)
      val compReadArr = JSONArray()
      progress.completedReadingChapterIndices.forEach { compReadArr.put(it) }
      put("completedReadingChapterIndices", compReadArr)
      val progMapObj = JSONObject()
      progress.chapterProgressMap.forEach { (chIdx, frac) ->
        progMapObj.put(chIdx.toString(), frac.toDouble())
      }
      put("chapterProgressMap", progMapObj)
      val audioPosObj = JSONObject()
      progress.chapterAudioPositionMsMap.forEach { (chIdx, posMs) ->
        audioPosObj.put(chIdx.toString(), posMs)
      }
      put("chapterAudioPositionMsMap", audioPosObj)
      val sentIdxObj = JSONObject()
      progress.chapterSentenceIndexMap.forEach { (chIdx, sentIdx) ->
        sentIdxObj.put(chIdx.toString(), sentIdx)
      }
      put("chapterSentenceIndexMap", sentIdxObj)
    }
    prefs.edit().putString(KEY_USER_PROGRESS_PREFIX + userId + "_" + progress.bookId, json.toString()).apply()
  }

  fun loadUserProgress(context: Context, userId: String, bookId: String): UserAudiobookProgress {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_USER_PROGRESS_PREFIX + userId + "_" + bookId, null) ?: return UserAudiobookProgress(bookId)
    return try {
      val obj = JSONObject(raw)
      val compSet = mutableSetOf<Int>()
      val compArr = obj.optJSONArray("completedChapterIndices")
      if (compArr != null) {
        for (i in 0 until compArr.length()) {
          compSet.add(compArr.optInt(i, 0))
        }
      }
      val chProgMap = mutableMapOf<Int, Float>()
      val progMapObj = obj.optJSONObject("chapterProgressMap")
      if (progMapObj != null) {
        val keys = progMapObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val kInt = k.toIntOrNull()
          if (kInt != null) {
            chProgMap[kInt] = progMapObj.optDouble(k, 0.0).toFloat().coerceIn(0f, 1f)
          }
        }
      }
      val chAudioPosMap = mutableMapOf<Int, Long>()
      val audioPosObj = obj.optJSONObject("chapterAudioPositionMsMap")
      if (audioPosObj != null) {
        val keys = audioPosObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val kInt = k.toIntOrNull()
          if (kInt != null) {
            chAudioPosMap[kInt] = audioPosObj.optLong(k, 0L).coerceAtLeast(0L)
          }
        }
      }
      val chSentIdxMap = mutableMapOf<Int, Int>()
      val sentIdxObj = obj.optJSONObject("chapterSentenceIndexMap")
      if (sentIdxObj != null) {
        val keys = sentIdxObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val kInt = k.toIntOrNull()
          if (kInt != null) {
            chSentIdxMap[kInt] = sentIdxObj.optInt(k, 0).coerceAtLeast(0)
          }
        }
      }
      val compReadSet = mutableSetOf<Int>()
      val compReadArr = obj.optJSONArray("completedReadingChapterIndices")
      if (compReadArr != null) {
        for (i in 0 until compReadArr.length()) {
          compReadSet.add(compReadArr.optInt(i, 0))
        }
      }
      val defaultSkip = isSkipRecordingAnnouncementsEnabled(context, userId)
      UserAudiobookProgress(
        bookId = obj.optString("bookId", bookId),
        lastEpisodeIndex = obj.optInt("lastEpisodeIndex", 0),
        lastSentenceIndex = obj.optInt("lastSentenceIndex", 0),
        lastPlaybackPositionMs = obj.optLong("lastPlaybackPositionMs", 0L),
        readingChapterIndex = obj.optInt("readingChapterIndex", 0),
        readingScrollOffset = obj.optInt("readingScrollOffset", 0),
        readingPageIndex = obj.optInt("readingPageIndex", 0),
        readingPercent = obj.optDouble("readingPercent", 0.0).toFloat().coerceIn(0f, 1f),
        listeningPercent = obj.optDouble("listeningPercent", 0.0).toFloat().coerceIn(0f, 1f),
        isCompleted = obj.optBoolean("isCompleted", false),
        isFavorite = obj.optBoolean("isFavorite", false),
        preferredEditionId = obj.optString("preferredEditionId", "auto"),
        autoPlayNextChapter = obj.optBoolean("autoPlayNextChapter", true),
        skipRecordingAnnouncements = obj.optBoolean("skipRecordingAnnouncements", defaultSkip),
        readingTheme = obj.optString("readingTheme", "DARK"),
        readingFontSizeSp = obj.optDouble("readingFontSizeSp", 18.0).toFloat().coerceIn(13f, 28f),
        isPageMode = obj.optBoolean("isPageMode", false),
        completedChapterIndices = compSet,
        completedReadingChapterIndices = compReadSet,
        chapterProgressMap = chProgMap,
        chapterAudioPositionMsMap = chAudioPosMap,
        chapterSentenceIndexMap = chSentIdxMap,
        completedExercisesCount = obj.optInt("completedExercisesCount", 0),
        lastReadAtTimestamp = obj.optLong("lastReadAtTimestamp", 0L),
        lastListenedAtTimestamp = obj.optLong("lastListenedAtTimestamp", System.currentTimeMillis())
      )
    } catch (_: Exception) {
      UserAudiobookProgress(
        bookId = bookId,
        skipRecordingAnnouncements = isSkipRecordingAnnouncementsEnabled(context, userId)
      )
    }
  }

  fun getContinueListeningBooks(context: Context, userId: String): List<AudiobookBook> {
    val all = getAllAvailableBooks(context, userId)
    val inProgress = all.map { book ->
      book to loadUserProgress(context, userId, book.id)
    }.filter { (_, prog) ->
      (prog.lastSentenceIndex > 0 || prog.lastEpisodeIndex > 0 || prog.lastPlaybackPositionMs > 0L) && !prog.isCompleted
    }.sortedByDescending { it.second.lastListenedAtTimestamp }
      .map { it.first }

    return inProgress.ifEmpty {
      listOfNotNull(
        all.firstOrNull { it.gutendexId == 1342 },
        all.firstOrNull { it.gutendexId == 1661 }
      )
    }
  }

  fun getCompletedBooks(context: Context, userId: String): List<AudiobookBook> {
    val all = getAllAvailableBooks(context, userId)
    return all.filter { loadUserProgress(context, userId, it.id).isCompleted }
  }

  fun getRecentlyListenedBooks(context: Context, userId: String): List<AudiobookBook> {
    val all = getAllAvailableBooks(context, userId)
    val withProgress = all.map { book ->
      val prog = loadUserProgress(context, userId, book.id)
      book to prog
    }.filter { it.second.lastSentenceIndex > 0 || it.second.lastEpisodeIndex > 0 || it.second.lastPlaybackPositionMs > 0L || it.second.isCompleted }
      .sortedByDescending { it.second.lastListenedAtTimestamp }

    return if (withProgress.isNotEmpty()) {
      withProgress.map { it.first }
    } else {
      getRecentlyOpenedBooks(context, userId)
    }
  }

  private fun saveGeneratedBook(context: Context, userId: String, book: AudiobookBook) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = loadGeneratedBooks(context, userId).toMutableList()
    existing.removeAll { it.id == book.id }
    existing.add(0, book)

    val arr = JSONArray()
    existing.take(40).forEach { b ->
      val obj = JSONObject().apply {
        put("id", b.id)
        put("title", b.title)
        put("author", b.author)
        put("primaryCategory", b.primaryCategory)
        put("subCategory", b.subCategory)
        put("shortDescription", b.shortDescription)
        put("contentType", b.contentType.name)
        put("targetLanguageCode", b.targetLanguageCode)
        put("defaultCefrLevel", b.defaultCefrLevel)
        put("estimatedTotalMinutes", b.estimatedTotalMinutes)

        val tArr = JSONArray()
        b.keyTakeaways.forEach { tArr.put(it) }
        put("keyTakeaways", tArr)

        val epArr = JSONArray()
        b.episodes.forEach { ep ->
          val epObj = JSONObject().apply {
            put("id", ep.id)
            put("episodeNumber", ep.episodeNumber)
            put("title", ep.title)
            put("subtitle", ep.subtitle)
            put("estimatedMinutes", ep.estimatedMinutes)
            put("speakingPracticePrompt", ep.speakingPracticePrompt)
            put("practicalApplicationTip", ep.practicalApplicationTip)

            val lArr = JSONArray()
            ep.narrationLines.forEach { line ->
              val lObj = JSONObject().apply {
                put("speaker", line.speaker)
                put("targetText", line.targetText)
                put("nativeTranslation", line.nativeTranslation)
                put("keyVocabWord", line.keyVocabWord)
                put("keyVocabMeaning", line.keyVocabMeaning)
              }
              lArr.put(lObj)
            }
            put("narrationLines", lArr)

            val qArr = JSONArray()
            ep.comprehensionQuestions.forEach { q ->
              val qObj = JSONObject().apply {
                put("question", q.question)
                put("correctIndex", q.correctIndex)
                put("explanation", q.explanation)
                val optArr = JSONArray()
                q.options.forEach { optArr.put(it) }
                put("options", optArr)
              }
              qArr.put(qObj)
            }
            put("comprehensionQuestions", qArr)
          }
          epArr.put(epObj)
        }
        put("episodes", epArr)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_GENERATED_BOOKS_PREFIX + userId, arr.toString()).apply()
  }

  private fun loadGeneratedBooks(context: Context, userId: String): List<AudiobookBook> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_GENERATED_BOOKS_PREFIX + userId, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val bId = obj.optString("id", "")
          if (bId.isBlank()) continue
          val title = obj.optString("title", "")
          val author = obj.optString("author", "")
          val cat = obj.optString("primaryCategory", "Motivation")
          val sub = obj.optString("subCategory", "Habits")
          val desc = obj.optString("shortDescription", "")
          val cType = try {
            BookContentType.valueOf(obj.optString("contentType", BookContentType.AI_LEARNING_SUMMARY.name))
          } catch (_: Exception) {
            BookContentType.AI_LEARNING_SUMMARY
          }
          val lang = obj.optString("targetLanguageCode", "en")
          val cefr = obj.optString("defaultCefrLevel", "B1")
          val mins = obj.optInt("estimatedTotalMinutes", 15)

          val tList = mutableListOf<String>()
          val tArr = obj.optJSONArray("keyTakeaways")
          if (tArr != null) {
            for (j in 0 until tArr.length()) {
              val itm = tArr.optString(j, "")
              if (itm.isNotBlank()) tList.add(itm)
            }
          }

          val epList = mutableListOf<AudiobookEpisode>()
          val epArr = obj.optJSONArray("episodes")
          if (epArr != null) {
            for (k in 0 until epArr.length()) {
              val epObj = epArr.optJSONObject(k) ?: continue
              val epId = epObj.optString("id", "ep_$k")
              val epNum = epObj.optInt("episodeNumber", k + 1)
              val epTitle = epObj.optString("title", "Episode ${k + 1}")
              val epSub = epObj.optString("subtitle", "")
              val epMins = epObj.optInt("estimatedMinutes", 3)
              val spkPrompt = epObj.optString("speakingPracticePrompt", "")
              val tip = epObj.optString("practicalApplicationTip", "")

              val lines = mutableListOf<ListeningDialogueLine>()
              val lArr = epObj.optJSONArray("narrationLines")
              if (lArr != null) {
                for (l in 0 until lArr.length()) {
                  val lObj = lArr.optJSONObject(l) ?: continue
                  lines.add(
                    ListeningDialogueLine(
                      speaker = lObj.optString("speaker", "Narrator"),
                      targetText = lObj.optString("targetText", ""),
                      nativeTranslation = lObj.optString("nativeTranslation", ""),
                      keyVocabWord = lObj.optString("keyVocabWord", ""),
                      keyVocabMeaning = lObj.optString("keyVocabMeaning", "")
                    )
                  )
                }
              }

              val questions = mutableListOf<AcademyComprehensionQuestion>()
              val qArr = epObj.optJSONArray("comprehensionQuestions")
              if (qArr != null) {
                for (q in 0 until qArr.length()) {
                  val qObj = qArr.optJSONObject(q) ?: continue
                  val optList = mutableListOf<String>()
                  val oArr = qObj.optJSONArray("options")
                  if (oArr != null) {
                    for (o in 0 until oArr.length()) {
                      val opt = oArr.optString(o, "")
                      if (opt.isNotBlank()) optList.add(opt)
                    }
                  }
                  questions.add(
                    AcademyComprehensionQuestion(
                      question = qObj.optString("question", ""),
                      options = optList,
                      correctIndex = qObj.optInt("correctIndex", 0),
                      explanation = qObj.optString("explanation", "")
                    )
                  )
                }
              }

              epList.add(
                AudiobookEpisode(
                  id = epId,
                  episodeNumber = epNum,
                  title = epTitle,
                  subtitle = epSub,
                  estimatedMinutes = epMins,
                  narrationLines = lines,
                  comprehensionQuestions = questions,
                  speakingPracticePrompt = spkPrompt,
                  practicalApplicationTip = tip
                )
              )
            }
          }

          add(
            AudiobookBook(
              id = bId,
              title = title,
              author = author,
              primaryCategory = cat,
              subCategory = sub,
              coverGradientColors = listOf(0xFF0F172A, 0xFF1E293B),
              shortDescription = desc,
              keyTakeaways = tList,
              contentType = cType,
              targetLanguageCode = lang,
              defaultCefrLevel = cefr,
              estimatedTotalMinutes = mins,
              episodes = epList
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // =========================================================================
  // 4. SMART RECOMMENDATIONS ENGINE
  // =========================================================================

  fun getPersonalizedRecommendations(
    context: Context,
    userId: String,
    targetLanguageCode: String,
    userLevel: String
  ): Map<String, List<AudiobookBook>> {
    val all = getAllAvailableBooks(context, userId)
    val recent = getRecentlyListenedBooks(context, userId)
    val favoriteCategories = recent.map { it.primaryCategory }.distinct()

    val becauseYouListened = if (favoriteCategories.isNotEmpty()) {
      all.filter { it.primaryCategory in favoriteCategories && it.id !in recent.map { r -> r.id } }
        .take(6)
    } else {
      AudiobookCatalogRepository.FAMOUS_FULL_BOOKS.take(6)
    }

    val recommendedForLevel = all.filter {
      it.defaultCefrLevel.equals(userLevel, ignoreCase = true) ||
        (userLevel.startsWith("A") && it.defaultCefrLevel.startsWith("A")) ||
        (userLevel.startsWith("B") && it.defaultCefrLevel.startsWith("B"))
    }.take(8)

    val modernSummaries = AudiobookCatalogRepository.MODERN_EDUCATIONAL_SUMMARIES

    return mapOf(
      "Famous Full Books" to AudiobookCatalogRepository.FAMOUS_FULL_BOOKS,
      "Recommended for Your Level" to recommendedForLevel.ifEmpty { all.take(6) },
      "Popular Audiobooks" to all.filter { it.isPopular || it.isFeatured }.take(8),
      "Educational Book Summaries" to modernSummaries,
      "Because You Listened" to becauseYouListened.ifEmpty { all.take(6) }
    )
  }
}
