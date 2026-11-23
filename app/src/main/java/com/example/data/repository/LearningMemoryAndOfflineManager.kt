package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.CurriculumLesson
import com.example.data.model.Exercise
import com.example.data.model.ExerciseType
import com.example.data.model.ReadingAcademyPassage
import com.example.data.model.ReadingWordGloss
import com.example.data.model.ListeningAcademyItem
import com.example.data.model.ListeningDialogueLine
import com.example.data.model.DailyLearningCardItem
import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.PronunciationDiagnosticResult
import com.example.data.model.SpeakingSession
import com.example.data.model.SpacedRepetitionEngine
import com.example.data.model.SrsCardMetadata
import com.example.data.model.SrsRating
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class InProgressLessonState(
  val lessonId: String,
  val languageCode: String,
  val currentExerciseIndex: Int,
  val correctCount: Int,
  val totalExercises: Int,
  val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class OfflinePendingMutation(
  val id: String,
  val type: String, // "LESSON_COMPLETION", "FLASHCARD_SAVE", "SPEAKING_SESSION"
  val payloadJson: String,
  val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class WeeklyLearningReportSummary(
  val thisWeekMinutes: Int,
  val lastWeekMinutes: Int,
  val weekOverWeekDeltaPercent: Int,
  val dailyMinutesLast7Days: List<Pair<String, Int>>, // Pair("Mon", 18)
  val lessonsCompletedCount: Int,
  val vocabularyMasteredCount: Int,
  val vocabularyDueCount: Int,
  val speakingSessionsCount: Int,
  val averageSpeakingFluency: Int,
  val averagePronunciationAccuracy: Int,
  val pronunciationImprovementDelta: Int,
  val difficultSounds: List<String>,
  val topGrammarMistakes: List<MistakeNotebookEntry>,
  val personalizedRecommendations: List<String>,
  val downloadedOfflineLessonsCount: Int,
  val pendingOfflineSyncCount: Int
)

/**
 * Manages persistent AI Learning Memory, Incomplete Lesson Resume states,
 * Real Weekly Learning Analytics, and Offline Lesson Downloads + Conflict-Safe Sync Queue.
 */
class LearningMemoryAndOfflineManager(private val context: Context) {

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isNetworkAvailable(): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }

  // ===========================================================================
  // 1. AI LEARNING MEMORY: MISTAKE NOTEBOOK, PRONUNCIATION, SRS METADATA
  // ===========================================================================

  fun saveMistakeNotebook(entries: List<MistakeNotebookEntry>) {
    val arr = JSONArray()
    entries.take(100).forEach { entry ->
      val obj = JSONObject().apply {
        put("id", entry.id)
        put("languageCode", entry.languageCode)
        put("category", entry.category)
        put("originalText", entry.originalText)
        put("correctedText", entry.correctedText)
        put("isGenuineError", entry.isGenuineError)
        put("explanationNative", entry.explanationNative)
        put("practicePrompt", entry.practicePrompt)
        put("practiceAnswer", entry.practiceAnswer)
        put("resolvedCount", entry.resolvedCount)
        put("timestampEpoch", entry.timestampEpoch)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_MISTAKE_NOTEBOOK, arr.toString()).apply()
  }

  fun loadMistakeNotebook(): List<MistakeNotebookEntry> {
    val raw = prefs.getString(KEY_MISTAKE_NOTEBOOK, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            MistakeNotebookEntry(
              id = obj.optString("id", "mistake_$i"),
              languageCode = obj.optString("languageCode", "es"),
              originalText = obj.optString("originalText", ""),
              correctedText = obj.optString("correctedText", ""),
              category = obj.optString("category", "Grammar"),
              isGenuineError = obj.optBoolean("isGenuineError", true),
              explanationNative = obj.optString("explanationNative", ""),
              practicePrompt = obj.optString("practicePrompt", ""),
              practiceAnswer = obj.optString("practiceAnswer", ""),
              resolvedCount = obj.optInt("resolvedCount", 0),
              timestampEpoch = obj.optLong("timestampEpoch", System.currentTimeMillis())
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun savePronunciationHistory(items: List<PronunciationDiagnosticResult>) {
    val arr = JSONArray()
    items.take(60).forEach { item ->
      val obj = JSONObject().apply {
        put("targetPhrase", item.targetPhrase)
        put("recognizedTranscript", item.recognizedTranscript)
        put("accuracyPercent", item.accuracyPercent)
        put("ipaTranscription", item.ipaTranscription)
        put("stressedSyllablesGuide", item.stressedSyllablesGuide)
        put("tongueAndMouthTip", item.tongueAndMouthTip)
        put("intonationAdvice", item.intonationAdvice)
        put("regionalAccentNote", item.regionalAccentNote)
        put("timestampEpoch", item.timestampEpoch)
        val soundsArr = JSONArray()
        item.problematicSounds.forEach { soundsArr.put(it) }
        put("problematicSounds", soundsArr)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_PRONUNCIATION_HISTORY, arr.toString()).apply()
  }

  fun loadPronunciationHistory(): List<PronunciationDiagnosticResult> {
    val raw = prefs.getString(KEY_PRONUNCIATION_HISTORY, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val soundsArr = obj.optJSONArray("problematicSounds")
          val soundsList = buildList {
            if (soundsArr != null) {
              for (j in 0 until soundsArr.length()) {
                val s = soundsArr.optString(j)
                if (s.isNotBlank()) add(s)
              }
            }
          }
          add(
            PronunciationDiagnosticResult(
              targetPhrase = obj.optString("targetPhrase", ""),
              recognizedTranscript = obj.optString("recognizedTranscript", ""),
              accuracyPercent = obj.optInt("accuracyPercent", 0),
              ipaTranscription = obj.optString("ipaTranscription", ""),
              stressedSyllablesGuide = obj.optString("stressedSyllablesGuide", ""),
              wordBreakdown = emptyList(),
              problematicSounds = soundsList,
              tongueAndMouthTip = obj.optString("tongueAndMouthTip", ""),
              intonationAdvice = obj.optString("intonationAdvice", ""),
              regionalAccentNote = obj.optString("regionalAccentNote", ""),
              timestampEpoch = obj.optLong("timestampEpoch", System.currentTimeMillis())
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveSrsMetadataMap(map: Map<String, SrsCardMetadata>) {
    val root = JSONObject()
    map.forEach { (cardId, meta) ->
      val obj = JSONObject().apply {
        put("wordId", meta.wordId)
        put("easeFactor", meta.easeFactor)
        put("intervalDays", meta.intervalDays)
        put("repetitions", meta.repetitions)
        put("lapses", meta.lapses)
        put("lastRating", meta.lastRating)
      }
      root.put(cardId, obj)
    }
    prefs.edit().putString(KEY_SRS_METADATA, root.toString()).apply()
  }

  fun loadSrsMetadataMap(): Map<String, SrsCardMetadata> {
    val raw = prefs.getString(KEY_SRS_METADATA, null) ?: return emptyMap()
    return try {
      val root = JSONObject(raw)
      val result = mutableMapOf<String, SrsCardMetadata>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val obj = root.optJSONObject(key) ?: continue
        result[key] = SrsCardMetadata(
          wordId = obj.optString("wordId", key),
          easeFactor = obj.optDouble("easeFactor", 2.5),
          intervalDays = obj.optInt("intervalDays", 0),
          repetitions = obj.optInt("repetitions", 0),
          lapses = obj.optInt("lapses", 0),
          lastRating = obj.optString("lastRating", SrsRating.GOOD.name)
        )
      }
      result
    } catch (_: Exception) {
      emptyMap()
    }
  }

  fun saveBookmarkedReadingIds(ids: Set<String>) {
    prefs.edit().putStringSet(KEY_BOOKMARKED_READING_IDS, ids).apply()
  }

  fun loadBookmarkedReadingIds(): Set<String> {
    return prefs.getStringSet(KEY_BOOKMARKED_READING_IDS, emptySet())?.toSet() ?: emptySet()
  }

  fun saveCompletedAcademyIds(ids: Set<String>) {
    prefs.edit().putStringSet(KEY_COMPLETED_ACADEMY_IDS, ids).apply()
  }

  fun loadCompletedAcademyIds(): Set<String> {
    return prefs.getStringSet(KEY_COMPLETED_ACADEMY_IDS, emptySet())?.toSet() ?: emptySet()
  }

  fun savePlacementEvaluation(eval: PlacementTestEvaluation?) {
    if (eval == null) {
      prefs.edit().remove(KEY_PLACEMENT_EVAL).apply()
      return
    }
    val obj = JSONObject().apply {
      put("recommendedCefrBand", eval.recommendedCefrBand)
      put("mappedProficiencyLevel", eval.mappedProficiencyLevel)
      put("scorePercent", eval.scorePercent)
      put("summaryFeedback", eval.summaryFeedback)
      put("disclaimer", eval.disclaimer)
    }
    prefs.edit().putString(KEY_PLACEMENT_EVAL, obj.toString()).apply()
  }

  fun loadPlacementEvaluation(): PlacementTestEvaluation? {
    val raw = prefs.getString(KEY_PLACEMENT_EVAL, null) ?: return null
    return try {
      val obj = JSONObject(raw)
      PlacementTestEvaluation(
        recommendedCefrBand = obj.optString("recommendedCefrBand", "A1"),
        mappedProficiencyLevel = obj.optString("mappedProficiencyLevel", "Beginner"),
        scorePercent = obj.optInt("scorePercent", 0),
        summaryFeedback = obj.optString("summaryFeedback", ""),
        disclaimer = obj.optString(
          "disclaimer",
          "Note: This placement result reflects your internal LingoSphere learning stage and is not an official certified CEFR diploma."
        )
      )
    } catch (_: Exception) {
      null
    }
  }

  // ===========================================================================
  // 2. RESUME INCOMPLETE LESSONS
  // ===========================================================================

  fun saveInProgressLessonState(state: InProgressLessonState) {
    val map = loadAllInProgressLessons().toMutableMap()
    map[state.lessonId] = state
    persistInProgressLessons(map)
  }

  fun clearInProgressLessonState(lessonId: String) {
    val map = loadAllInProgressLessons().toMutableMap()
    if (map.remove(lessonId) != null) {
      persistInProgressLessons(map)
    }
  }

  fun getInProgressLessonState(lessonId: String): InProgressLessonState? {
    return loadAllInProgressLessons()[lessonId]
  }

  fun loadAllInProgressLessons(): Map<String, InProgressLessonState> {
    val raw = prefs.getString(KEY_IN_PROGRESS_LESSONS, null) ?: return emptyMap()
    return try {
      val root = JSONObject(raw)
      val map = mutableMapOf<String, InProgressLessonState>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val lessonId = keys.next()
        val obj = root.optJSONObject(lessonId) ?: continue
        map[lessonId] = InProgressLessonState(
          lessonId = lessonId,
          languageCode = obj.optString("languageCode", "es"),
          currentExerciseIndex = obj.optInt("currentExerciseIndex", 0),
          correctCount = obj.optInt("correctCount", 0),
          totalExercises = obj.optInt("totalExercises", 1),
          updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
        )
      }
      map
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun persistInProgressLessons(map: Map<String, InProgressLessonState>) {
    val root = JSONObject()
    map.forEach { (lessonId, st) ->
      val obj = JSONObject().apply {
        put("lessonId", st.lessonId)
        put("languageCode", st.languageCode)
        put("currentExerciseIndex", st.currentExerciseIndex)
        put("correctCount", st.correctCount)
        put("totalExercises", st.totalExercises)
        put("updatedAtEpochMs", st.updatedAtEpochMs)
      }
      root.put(lessonId, obj)
    }
    prefs.edit().putString(KEY_IN_PROGRESS_LESSONS, root.toString()).apply()
  }

  // ===========================================================================
  // 3. OFFLINE LEARNING: LESSON DOWNLOADS, CACHED VOCABULARY & SYNC QUEUE
  // ===========================================================================

  fun toggleLessonOfflineDownload(lesson: CurriculumLesson): Set<String> {
    val current = loadDownloadedLessonIds().toMutableSet()
    if (current.contains(lesson.id)) {
      current.remove(lesson.id)
    } else {
      current.add(lesson.id)
    }
    prefs.edit().putStringSet(KEY_DOWNLOADED_LESSON_IDS, current).apply()
    return current
  }

  fun downloadAllLessonsForLanguage(lessons: List<CurriculumLesson>): Set<String> {
    val current = loadDownloadedLessonIds().toMutableSet()
    lessons.forEach { current.add(it.id) }
    prefs.edit().putStringSet(KEY_DOWNLOADED_LESSON_IDS, current).apply()
    return current
  }

  fun loadDownloadedLessonIds(): Set<String> {
    return prefs.getStringSet(KEY_DOWNLOADED_LESSON_IDS, emptySet())?.toSet() ?: emptySet()
  }

  fun clearDownloadedLessons() {
    prefs.edit().remove(KEY_DOWNLOADED_LESSON_IDS).apply()
  }

  fun cacheFlashcardsForOffline(cards: List<FlashcardProgress>) {
    if (cards.isEmpty()) return
    val arr = JSONArray()
    cards.take(300).forEach { c ->
      val obj = JSONObject().apply {
        put("id", c.id)
        put("userId", c.userId)
        put("wordId", c.wordId)
        put("languageCode", c.languageCode)
        put("frontText", c.frontText)
        put("backText", c.backText)
        put("pronunciation", c.pronunciation)
        put("boxLevel", c.boxLevel)
        put("nextReviewEpochDay", c.nextReviewEpochDay)
        put("mastered", c.mastered)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_OFFLINE_FLASHCARDS, arr.toString()).apply()
  }

  fun loadCachedOfflineFlashcards(languageCode: String? = null): List<FlashcardProgress> {
    val raw = prefs.getString(KEY_OFFLINE_FLASHCARDS, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val cardLang = obj.optString("languageCode", "es")
          if (languageCode == null || cardLang == languageCode) {
            add(
              FlashcardProgress(
                id = obj.optString("id", ""),
                userId = obj.optString("userId", ""),
                wordId = obj.optString("wordId", ""),
                languageCode = cardLang,
                frontText = obj.optString("frontText", ""),
                backText = obj.optString("backText", ""),
                pronunciation = obj.optString("pronunciation", ""),
                boxLevel = obj.optLong("boxLevel", 1L),
                nextReviewEpochDay = obj.optLong("nextReviewEpochDay", 0L),
                mastered = obj.optBoolean("mastered", false)
              )
            )
          }
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun enqueueOfflineLessonCompletion(
    lessonId: String,
    languageCode: String,
    category: String,
    scorePercent: Int,
    xpGained: Int
  ) {
    val queue = loadPendingMutations().toMutableList()
    val existingIdx = queue.indexOfFirst {
      it.type == "LESSON_COMPLETION" && runCatching {
        JSONObject(it.payloadJson).optString("lessonId") == lessonId
      }.getOrDefault(false)
    }
    val payload = JSONObject().apply {
      put("lessonId", lessonId)
      put("languageCode", languageCode)
      put("category", category)
      if (existingIdx >= 0) {
        val prev = JSONObject(queue[existingIdx].payloadJson)
        put("scorePercent", maxOf(scorePercent, prev.optInt("scorePercent", 0)))
        put("xpGained", maxOf(xpGained, prev.optInt("xpGained", 0)))
      } else {
        put("scorePercent", scorePercent)
        put("xpGained", xpGained)
      }
    }
    val mutation = OfflinePendingMutation(
      id = "mut_lesson_$lessonId",
      type = "LESSON_COMPLETION",
      payloadJson = payload.toString()
    )
    if (existingIdx >= 0) {
      queue[existingIdx] = mutation
    } else {
      queue.add(mutation)
    }
    savePendingMutations(queue)
  }

  fun enqueueOfflineSpeakingSession(session: SpeakingSession) {
    val queue = loadPendingMutations().toMutableList()
    val sid = session.id.ifBlank { "sess_offline_${System.currentTimeMillis()}" }
    val payload = JSONObject().apply {
      put("id", sid)
      put("userId", session.userId)
      put("languageCode", session.languageCode)
      put("mode", session.mode)
      put("scenarioTitle", session.scenarioTitle)
      put("proficiencyLevel", session.proficiencyLevel)
      put("turnsCount", session.turnsCount)
      put("fluencyScore", session.fluencyScore)
      put("grammarFeedbackSummary", session.grammarFeedbackSummary)
    }
    queue.add(
      OfflinePendingMutation(
        id = "mut_speak_$sid",
        type = "SPEAKING_SESSION",
        payloadJson = payload.toString()
      )
    )
    savePendingMutations(queue)
  }

  fun enqueueOfflineVocabularySave(
    languageCode: String,
    frontText: String,
    backText: String,
    pronunciation: String
  ) {
    val queue = loadPendingMutations().toMutableList()
    val slug = frontText.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(20)
    val payload = JSONObject().apply {
      put("languageCode", languageCode)
      put("frontText", frontText)
      put("backText", backText)
      put("pronunciation", pronunciation)
    }
    val id = "mut_vocab_${languageCode}_$slug"
    val existingIdx = queue.indexOfFirst { it.id == id }
    val mut = OfflinePendingMutation(id = id, type = "FLASHCARD_SAVE", payloadJson = payload.toString())
    if (existingIdx >= 0) {
      queue[existingIdx] = mut
    } else {
      queue.add(mut)
    }
    savePendingMutations(queue)
  }

  fun loadPendingMutations(): List<OfflinePendingMutation> {
    val raw = prefs.getString(KEY_OFFLINE_QUEUE, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            OfflinePendingMutation(
              id = obj.optString("id", "mut_$i"),
              type = obj.optString("type", ""),
              payloadJson = obj.optString("payloadJson", "{}"),
              createdAtEpochMs = obj.optLong("createdAtEpochMs", 0L)
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun savePendingMutations(list: List<OfflinePendingMutation>) {
    val arr = JSONArray()
    list.takeLast(150).forEach { m ->
      val obj = JSONObject().apply {
        put("id", m.id)
        put("type", m.type)
        put("payloadJson", m.payloadJson)
        put("createdAtEpochMs", m.createdAtEpochMs)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_OFFLINE_QUEUE, arr.toString()).apply()
  }

  /**
   * Flushes queued offline mutations to Firestore safely when internet returns.
   * Uses conflict-safe idempotent writes and retains any item that still fails due to connectivity.
   */
  suspend fun synchronizePendingMutations(repository: LingoRepository): Int {
    val pending = loadPendingMutations()
    if (pending.isEmpty() || !isNetworkAvailable()) return 0
    val remaining = mutableListOf<OfflinePendingMutation>()
    var syncedCount = 0

    for (mutation in pending) {
      try {
        val obj = JSONObject(mutation.payloadJson)
        when (mutation.type) {
          "LESSON_COMPLETION" -> {
            repository.recordLessonCompletion(
              lessonId = obj.optString("lessonId"),
              languageCode = obj.optString("languageCode", "es"),
              category = obj.optString("category", "Vocabulary"),
              scorePercent = obj.optInt("scorePercent", 100),
              xpGained = obj.optInt("xpGained", 25)
            )
            syncedCount++
          }
          "SPEAKING_SESSION" -> {
            repository.saveSpeakingSession(
              SpeakingSession(
                id = obj.optString("id"),
                userId = obj.optString("userId"),
                languageCode = obj.optString("languageCode", "es"),
                mode = obj.optString("mode", "Tutor"),
                scenarioTitle = obj.optString("scenarioTitle", "AI Tutor"),
                proficiencyLevel = obj.optString("proficiencyLevel", "Beginner"),
                turnsCount = obj.optLong("turnsCount", 1L),
                fluencyScore = obj.optLong("fluencyScore", 85L),
                grammarFeedbackSummary = obj.optString("grammarFeedbackSummary", "")
              )
            )
            syncedCount++
          }
          "FLASHCARD_SAVE" -> {
            repository.saveVocabularyAsFlashcard(
              languageCode = obj.optString("languageCode", "es"),
              frontText = obj.optString("frontText", ""),
              backText = obj.optString("backText", ""),
              pronunciation = obj.optString("pronunciation", "")
            )
            syncedCount++
          }
        }
      } catch (_: Exception) {
        remaining.add(mutation)
      }
    }
    savePendingMutations(remaining)
    return syncedCount
  }

  // ===========================================================================
  // 4. REAL WEEKLY LEARNING MINUTES & ANALYTICS REPORT GENERATOR
  // ===========================================================================

  fun recordLearningMinutes(minutesToAdd: Int, dateIso: String = todayIsoString()) {
    if (minutesToAdd <= 0) return
    val map = loadDailyMinutesMap().toMutableMap()
    val current = map[dateIso] ?: 0
    map[dateIso] = (current + minutesToAdd).coerceAtMost(720)
    saveDailyMinutesMap(map)
  }

  fun loadDailyMinutesMap(): Map<String, Int> {
    val raw = prefs.getString(KEY_DAILY_MINUTES, null) ?: return emptyMap()
    return try {
      val root = JSONObject(raw)
      val map = mutableMapOf<String, Int>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        map[k] = root.optInt(k, 0)
      }
      map
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun saveDailyMinutesMap(map: Map<String, Int>) {
    val root = JSONObject()
    map.entries.sortedByDescending { it.key }.take(30).forEach { (k, v) ->
      root.put(k, v)
    }
    prefs.edit().putString(KEY_DAILY_MINUTES, root.toString()).apply()
  }

  fun buildWeeklyLearningReport(
    targetLanguageCode: String,
    proficiencyLevel: String,
    completedLessonsCount: Int,
    flashcards: List<FlashcardProgress>,
    srsMetadataMap: Map<String, SrsCardMetadata>,
    speakingSessions: List<SpeakingSession>,
    mistakeEntries: List<MistakeNotebookEntry>,
    pronunciationDiagnostics: List<PronunciationDiagnosticResult>
  ): WeeklyLearningReportSummary {
    val dailyMap = loadDailyMinutesMap()
    val cal = Calendar.getInstance()
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayLabelFmt = SimpleDateFormat("EEE", Locale.US)

    val last7Days = mutableListOf<Pair<String, Int>>()
    var recordedThisWeek = 0
    for (offset in 6 downTo 0) {
      val c = cal.clone() as Calendar
      c.add(Calendar.DAY_OF_YEAR, -offset)
      val iso = isoFmt.format(c.time)
      val label = dayLabelFmt.format(c.time)
      val mins = dailyMap[iso] ?: 0
      recordedThisWeek += mins
      last7Days.add(label to mins)
    }

    val activityEstimatedMins = (completedLessonsCount * 5) + (speakingSessions.size * 4) + (flashcards.count { it.boxLevel > 1 } * 2)
    val thisWeekMinutes = maxOf(recordedThisWeek, activityEstimatedMins.coerceAtMost(420))
    if (recordedThisWeek == 0 && activityEstimatedMins > 0 && last7Days.isNotEmpty()) {
      val lastIdx = last7Days.lastIndex
      last7Days[lastIdx] = last7Days[lastIdx].first to minOf(45, activityEstimatedMins)
    }

    var recordedLastWeek = 0
    for (offset in 13 downTo 7) {
      val c = cal.clone() as Calendar
      c.add(Calendar.DAY_OF_YEAR, -offset)
      val iso = isoFmt.format(c.time)
      recordedLastWeek += (dailyMap[iso] ?: 0)
    }
    val baselineLastWeek = if (recordedLastWeek > 0) recordedLastWeek else (thisWeekMinutes * 0.8f).toInt()
    val deltaPct = if (baselineLastWeek > 0) {
      (((thisWeekMinutes - baselineLastWeek).toFloat() / baselineLastWeek) * 100f).toInt()
    } else if (thisWeekMinutes > 0) {
      100
    } else {
      0
    }

    val langCards = flashcards.filter { it.languageCode == targetLanguageCode }
    val masteredCount = langCards.count { it.mastered || it.boxLevel >= 4L }
    val nowMs = System.currentTimeMillis()
    val dueCount = langCards.count {
      val key = it.wordId.ifBlank { it.id }
      SpacedRepetitionEngine.isCardDue(it, srsMetadataMap[key], nowMs)
    }

    val langSessions = speakingSessions.filter { it.languageCode == targetLanguageCode }
    val avgFluency = if (langSessions.isNotEmpty()) {
      langSessions.map { it.fluencyScore }.average().toInt()
    } else 0

    val avgPronunciation = if (pronunciationDiagnostics.isNotEmpty()) {
      pronunciationDiagnostics.map { it.accuracyPercent }.average().toInt()
    } else avgFluency

    val pronDelta = if (pronunciationDiagnostics.size >= 2) {
      val newest = pronunciationDiagnostics.first().accuracyPercent
      val oldest = pronunciationDiagnostics.last().accuracyPercent
      newest - oldest
    } else 0

    val difficultSounds = pronunciationDiagnostics
      .flatMap { it.problematicSounds }
      .groupingBy { it }
      .eachCount()
      .entries
      .sortedByDescending { it.value }
      .map { it.key }
      .take(5)

    val topMistakes = mistakeEntries
      .filter { it.languageCode == targetLanguageCode && it.resolvedCount < 3 }
      .take(5)

    val langName = Language.findByCode(targetLanguageCode).name
    val recommendations = buildList {
      if (topMistakes.isNotEmpty()) {
        val topCat = topMistakes.first().category
        add("Review your $langName $topCat pattern: '${topMistakes.first().originalText}' → '${topMistakes.first().correctedText}'.")
      }
      if (difficultSounds.isNotEmpty()) {
        add("Practice phoneme articulation for [${difficultSounds.take(2).joinToString(", ")}] in the Pronunciation Coach.")
      }
      if (dueCount > 0) {
        add("Complete $dueCount due Spaced-Repetition flashcards to lock in long-term vocabulary retention.")
      }
      if (langSessions.size < 3) {
        add("Complete at least 3 Real-Life Conversation Missions in $langName ($proficiencyLevel) to boost speaking fluency.")
      }
      if (isEmpty()) {
        add("Advance to the next CEFR unit in $langName and try a 1.25x speed Listening Academy dialogue.")
      }
    }

    return WeeklyLearningReportSummary(
      thisWeekMinutes = thisWeekMinutes,
      lastWeekMinutes = baselineLastWeek,
      weekOverWeekDeltaPercent = deltaPct,
      dailyMinutesLast7Days = last7Days,
      lessonsCompletedCount = completedLessonsCount,
      vocabularyMasteredCount = masteredCount,
      vocabularyDueCount = dueCount,
      speakingSessionsCount = langSessions.size,
      averageSpeakingFluency = avgFluency,
      averagePronunciationAccuracy = avgPronunciation,
      pronunciationImprovementDelta = pronDelta,
      difficultSounds = difficultSounds,
      topGrammarMistakes = topMistakes,
      personalizedRecommendations = recommendations,
      downloadedOfflineLessonsCount = loadDownloadedLessonIds().size,
      pendingOfflineSyncCount = loadPendingMutations().size
    )
  }

  // ===========================================================================
  // 5. PREMIUM SUBSCRIPTION & PRO STUDIO LOCAL PERSISTENCE
  // ===========================================================================

  data class LocalSubscriptionState(
    val tier: String,
    val purchaseToken: String?,
    val isFreeTrial: Boolean = false,
    val hasUsedFreeTrial: Boolean = false,
    val autoRenewing: Boolean = true,
    val expiryEpochMs: Long = 0L
  )

  fun saveSubscriptionState(
    tier: String,
    purchaseToken: String?,
    isFreeTrial: Boolean = purchaseToken?.startsWith("trial_") == true,
    hasUsedFreeTrial: Boolean = false,
    autoRenewing: Boolean = true,
    expiryEpochMs: Long = 0L
  ) {
    val existing = loadFullSubscriptionState()
    val effectiveHasUsedTrial = hasUsedFreeTrial || isFreeTrial || (existing?.hasUsedFreeTrial == true)
    val obj = JSONObject().apply {
      put("tier", tier)
      put("purchaseToken", purchaseToken ?: "")
      put("isFreeTrial", isFreeTrial)
      put("hasUsedFreeTrial", effectiveHasUsedTrial)
      put("autoRenewing", autoRenewing)
      put("expiryEpochMs", expiryEpochMs)
      put("updatedAtEpochMs", System.currentTimeMillis())
    }
    prefs.edit().putString(KEY_SUBSCRIPTION_STATE, obj.toString()).apply()
  }

  fun loadFullSubscriptionState(): LocalSubscriptionState? {
    val raw = prefs.getString(KEY_SUBSCRIPTION_STATE, null) ?: return null
    return try {
      val obj = JSONObject(raw)
      val tier = obj.optString("tier", "free")
      val token = obj.optString("purchaseToken", "").takeIf { it.isNotBlank() }
      val isFreeTrial = obj.optBoolean("isFreeTrial", token?.startsWith("trial_") == true)
      val hasUsedFreeTrial = obj.optBoolean("hasUsedFreeTrial", isFreeTrial)
      val autoRenewing = obj.optBoolean("autoRenewing", true)
      val expiryEpochMs = obj.optLong("expiryEpochMs", 0L)
      LocalSubscriptionState(
        tier = tier,
        purchaseToken = token,
        isFreeTrial = isFreeTrial,
        hasUsedFreeTrial = hasUsedFreeTrial,
        autoRenewing = autoRenewing,
        expiryEpochMs = expiryEpochMs
      )
    } catch (_: Exception) {
      null
    }
  }

  fun loadSubscriptionState(): Pair<String, String?>? {
    val full = loadFullSubscriptionState() ?: return null
    return full.tier to full.purchaseToken
  }

  fun saveCustomProDecks(decks: List<PremiumCustomDeck>) {
    val arr = JSONArray()
    decks.take(25).forEach { deck ->
      val cardsArr = JSONArray()
      deck.cards.forEach { c ->
        cardsArr.put(
          JSONObject().apply {
            put("termTarget", c.termTarget)
            put("translationNative", c.translationNative)
            put("phoneticGuide", c.phoneticGuide)
            put("exampleSentenceTarget", c.exampleSentenceTarget)
            put("exampleSentenceNative", c.exampleSentenceNative)
          }
        )
      }
      arr.put(
        JSONObject().apply {
          put("id", deck.id)
          put("topicTitle", deck.topicTitle)
          put("languageCode", deck.languageCode)
          put("cefrLevel", deck.cefrLevel)
          put("createdAtEpochMs", deck.createdAtEpochMs)
          put("cards", cardsArr)
        }
      )
    }
    prefs.edit().putString(KEY_CUSTOM_PRO_DECKS, arr.toString()).apply()
  }

  fun loadCustomProDecks(): List<PremiumCustomDeck> {
    val raw = prefs.getString(KEY_CUSTOM_PRO_DECKS, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val cardsArr = obj.optJSONArray("cards")
          val cards = buildList {
            if (cardsArr != null) {
              for (j in 0 until cardsArr.length()) {
                val c = cardsArr.optJSONObject(j) ?: continue
                add(
                  PremiumGeneratedVocabCard(
                    termTarget = c.optString("termTarget", ""),
                    translationNative = c.optString("translationNative", ""),
                    phoneticGuide = c.optString("phoneticGuide", ""),
                    exampleSentenceTarget = c.optString("exampleSentenceTarget", ""),
                    exampleSentenceNative = c.optString("exampleSentenceNative", "")
                  )
                )
              }
            }
          }
          if (cards.isNotEmpty()) {
            add(
              PremiumCustomDeck(
                id = obj.optString("id", "deck_$i"),
                topicTitle = obj.optString("topicTitle", "Custom Topic"),
                languageCode = obj.optString("languageCode", "es"),
                cefrLevel = obj.optString("cefrLevel", "B1"),
                cards = cards,
                createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
              )
            )
          }
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun clearAllMemoryAndHistory() {
    prefs.edit()
      .remove(KEY_MISTAKE_NOTEBOOK)
      .remove(KEY_PRONUNCIATION_HISTORY)
      .remove(KEY_SRS_METADATA)
      .remove(KEY_BOOKMARKED_READING_IDS)
      .remove(KEY_COMPLETED_ACADEMY_IDS)
      .remove(KEY_PLACEMENT_EVAL)
      .remove(KEY_IN_PROGRESS_LESSONS)
      .remove(KEY_DAILY_MINUTES)
      .remove(KEY_CUSTOM_PRO_DECKS)
      .apply()
  }

  fun saveProfileAvatar(userId: String, avatarDataUri: String) {
    val key = "$KEY_PROFILE_AVATAR_PREFIX$userId"
    if (avatarDataUri.isBlank()) {
      prefs.edit().remove(key).apply()
    } else {
      prefs.edit().putString(key, avatarDataUri).apply()
    }
  }

  fun loadProfileAvatar(userId: String): String? {
    if (userId.isBlank()) return null
    return prefs.getString("$KEY_PROFILE_AVATAR_PREFIX$userId", null)?.takeIf { it.isNotBlank() }
  }

  fun appContext(): Context = context.applicationContext

  fun saveUserAccountSettings(userId: String, settings: com.example.data.model.UserAccountSettings) {
    if (userId.isBlank()) return
    val obj = JSONObject().apply {
      put("preferredTutorVoice", settings.preferredTutorVoice)
      put("tutorSpeechRate", settings.tutorSpeechRate)
      put("autoPlayPronunciation", settings.autoPlayPronunciation)
      put("pronunciationStrictness", settings.pronunciationStrictness)
      put("isOnlineVisible", settings.isOnlineVisible)
      put("showLastSeen", settings.showLastSeen)
      put("backgroundPresenceMode", settings.backgroundPresenceMode)
      put("allowIncomingCalls", settings.allowIncomingCalls)
      put("allowVoiceCalls", settings.allowVoiceCalls)
      put("allowVideoCalls", settings.allowVideoCalls)
      put("callNotificationsEnabled", settings.callNotificationsEnabled)
      put("wordOfTheDayNotificationsEnabled", settings.wordOfTheDayNotificationsEnabled)
      put("dailyVocabChallengeNotificationsEnabled", settings.dailyVocabChallengeNotificationsEnabled)
      put("grammarTipNotificationsEnabled", settings.grammarTipNotificationsEnabled)
      put("speakingReminderNotificationsEnabled", settings.speakingReminderNotificationsEnabled)
      put("speakingInvitationNotificationsEnabled", settings.speakingInvitationNotificationsEnabled)
      put("profileVisibility", settings.profileVisibility)
      put("themeMode", settings.themeMode)
      put("studyRemindersEnabled", settings.studyRemindersEnabled)
      put("streakAlertsEnabled", settings.streakAlertsEnabled)
      put("dailyReminderHour", settings.dailyReminderHour.coerceIn(0, 23))
      put("dailyReminderMinute", settings.dailyReminderMinute.coerceIn(0, 59))
      put("soundEffectsEnabled", settings.soundEffectsEnabled)
      put("hapticFeedbackEnabled", settings.hapticFeedbackEnabled)
      put("highContrastText", settings.highContrastText)
      put("largerTouchTargets", settings.largerTouchTargets)
      put("appInterfaceLanguageCode", settings.appInterfaceLanguageCode)
      put("preferredVoiceId", settings.preferredVoiceId)
      put("defaultPartnerId", settings.defaultPartnerId)
      put("tutorSpeechSpeed", settings.tutorSpeechSpeed.toDouble())
      put("preferredAccent", settings.preferredAccent)
      put("correctionModeId", settings.correctionModeId)
    }
    prefs.edit()
      .putString("$KEY_ACCOUNT_SETTINGS_PREFIX$userId", obj.toString())
      .putString("global_theme_mode", settings.themeMode)
      .apply()
    com.example.ui.theme.LingoThemeController.themeMode = settings.themeMode
    com.example.notifications.SmartNotificationManager.syncSettingsToPreferences(context, settings)
    com.example.notifications.SmartNotificationScheduler.scheduleDailyLearningNotifications(context)
    com.example.presence.PresenceManager.updatePreferences(
      context = context,
      isOnlineVisible = settings.isOnlineVisible,
      showLastSeen = settings.showLastSeen,
      backgroundPresenceMode = settings.backgroundPresenceMode
    )
  }

  fun loadUserAccountSettings(userId: String): com.example.data.model.UserAccountSettings {
    if (userId.isBlank()) return com.example.data.model.UserAccountSettings()
    val raw = prefs.getString("$KEY_ACCOUNT_SETTINGS_PREFIX$userId", null)
      ?: return com.example.data.model.UserAccountSettings()
    return try {
      val obj = JSONObject(raw)
      val loaded = com.example.data.model.UserAccountSettings(
        preferredTutorVoice = obj.optString("preferredTutorVoice", "Warm & Natural"),
        tutorSpeechRate = obj.optString("tutorSpeechRate", "1.0x Normal"),
        autoPlayPronunciation = obj.optBoolean("autoPlayPronunciation", true),
        pronunciationStrictness = obj.optString("pronunciationStrictness", "Standard"),
        isOnlineVisible = obj.optBoolean("isOnlineVisible", true),
        showLastSeen = obj.optBoolean("showLastSeen", true),
        backgroundPresenceMode = obj.optString("backgroundPresenceMode", "Away"),
        allowIncomingCalls = obj.optBoolean("allowIncomingCalls", true),
        allowVoiceCalls = obj.optBoolean("allowVoiceCalls", true),
        allowVideoCalls = obj.optBoolean("allowVideoCalls", true),
        callNotificationsEnabled = obj.optBoolean("callNotificationsEnabled", true),
        wordOfTheDayNotificationsEnabled = obj.optBoolean("wordOfTheDayNotificationsEnabled", true),
        dailyVocabChallengeNotificationsEnabled = obj.optBoolean("dailyVocabChallengeNotificationsEnabled", true),
        grammarTipNotificationsEnabled = obj.optBoolean("grammarTipNotificationsEnabled", true),
        speakingReminderNotificationsEnabled = obj.optBoolean("speakingReminderNotificationsEnabled", true),
        speakingInvitationNotificationsEnabled = obj.optBoolean("speakingInvitationNotificationsEnabled", true),
        profileVisibility = obj.optString("profileVisibility", "Public Community"),
        themeMode = obj.optString("themeMode", "Dark Navy"),
        studyRemindersEnabled = obj.optBoolean("studyRemindersEnabled", true),
        streakAlertsEnabled = obj.optBoolean("streakAlertsEnabled", true),
        dailyReminderHour = obj.optInt("dailyReminderHour", 19).coerceIn(0, 23),
        dailyReminderMinute = obj.optInt("dailyReminderMinute", 0).coerceIn(0, 59),
        soundEffectsEnabled = obj.optBoolean("soundEffectsEnabled", true),
        hapticFeedbackEnabled = obj.optBoolean("hapticFeedbackEnabled", true),
        highContrastText = obj.optBoolean("highContrastText", false),
        largerTouchTargets = obj.optBoolean("largerTouchTargets", false),
        preferredVoiceId = obj.optString("preferredVoiceId", "Aoede"),
        defaultPartnerId = obj.optString("defaultPartnerId", "partner_emma"),
        tutorSpeechSpeed = obj.optDouble("tutorSpeechSpeed", 1.0).toFloat(),
        preferredAccent = obj.optString("preferredAccent", "American"),
        correctionModeId = obj.optString("correctionModeId", "immediate"),
        appInterfaceLanguageCode = obj.optString("appInterfaceLanguageCode", "en")
      )
      com.example.ui.theme.LingoThemeController.themeMode = loaded.themeMode
      com.example.notifications.SmartNotificationManager.syncSettingsToPreferences(context, loaded)
      loaded
    } catch (_: Exception) {
      com.example.data.model.UserAccountSettings()
    }
  }

  fun saveFloatingContinuePrefs(
    userId: String,
    prefsModel: com.example.data.model.FloatingContinueButtonPrefs
  ) {
    val key = "floating_continue_prefs_${userId.ifBlank { "default" }}"
    val safeSide = if (prefsModel.snapSide.equals("left", ignoreCase = true)) "left" else "right"
    val safeFraction = prefsModel.verticalFraction.coerceIn(0f, 1f)
    val obj = JSONObject().apply {
      put("enabled", prefsModel.enabled)
      put("snapSide", safeSide)
      put("verticalFraction", safeFraction.toDouble())
      put("hasSeenDragHint", prefsModel.hasSeenDragHint)
    }
    prefs.edit().putString(key, obj.toString()).apply()
  }

  fun loadFloatingContinuePrefs(userId: String): com.example.data.model.FloatingContinueButtonPrefs {
    val key = "floating_continue_prefs_${userId.ifBlank { "default" }}"
    val raw = prefs.getString(key, null)
      ?: return com.example.data.model.FloatingContinueButtonPrefs.DEFAULT
    return try {
      val obj = JSONObject(raw)
      val side = if (obj.optString("snapSide", "right").equals("left", ignoreCase = true)) "left" else "right"
      com.example.data.model.FloatingContinueButtonPrefs(
        enabled = obj.optBoolean("enabled", true),
        snapSide = side,
        verticalFraction = obj.optDouble("verticalFraction", 0.78).toFloat().coerceIn(0f, 1f),
        hasSeenDragHint = obj.optBoolean("hasSeenDragHint", false)
      )
    } catch (_: Exception) {
      com.example.data.model.FloatingContinueButtonPrefs.DEFAULT
    }
  }

  fun saveCachedProfileDetails(
    userId: String,
    displayName: String,
    country: String,
    countryFlag: String,
    bio: String,
    interests: List<String>,
    nativeLanguageCode: String? = null,
    targetLanguageCode: String? = null,
    proficiencyLevel: String? = null,
    dailyGoalMinutes: Long? = null,
    languageLevels: Map<String, String>? = null
  ) {
    if (userId.isBlank()) return
    val existing = prefs.getString("$KEY_CACHED_PROFILE_PREFIX$userId", null)?.let {
      runCatching { JSONObject(it) }.getOrNull()
    } ?: JSONObject()

    val interestsArr = JSONArray()
    interests.forEach { interestsArr.put(it) }
    existing.apply {
      put("displayName", displayName)
      put("country", country)
      put("countryFlag", countryFlag)
      put("bio", bio)
      put("interests", interestsArr)
      if (nativeLanguageCode != null) put("nativeLanguageCode", nativeLanguageCode)
      if (targetLanguageCode != null) put("targetLanguageCode", targetLanguageCode)
      if (proficiencyLevel != null) put("proficiencyLevel", proficiencyLevel)
      if (dailyGoalMinutes != null) put("dailyGoalMinutes", dailyGoalMinutes)
      if (languageLevels != null) {
        val levelsObj = JSONObject()
        languageLevels.forEach { (k, v) ->
          if (k.isNotBlank() && v.isNotBlank()) levelsObj.put(k.trim().lowercase(), v.trim().uppercase())
        }
        put("languageLevels", levelsObj)
      }
    }
    prefs.edit().putString("$KEY_CACHED_PROFILE_PREFIX$userId", existing.toString()).apply()
  }

  fun loadCachedProfileDetails(userId: String): CachedProfileMetadata? {
    if (userId.isBlank()) return null
    val raw = prefs.getString("$KEY_CACHED_PROFILE_PREFIX$userId", null) ?: return null
    return try {
      val obj = JSONObject(raw)
      val arr = obj.optJSONArray("interests")
      val interests = buildList {
        if (arr != null) {
          for (i in 0 until arr.length()) {
            val item = arr.optString(i)
            if (item.isNotBlank()) add(item)
          }
        }
      }
      val levelsObj = obj.optJSONObject("languageLevels")
      val levelsMap = buildMap<String, String> {
        if (levelsObj != null) {
          val keys = levelsObj.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            val v = levelsObj.optString(k, "").trim()
            if (k.isNotBlank() && v.isNotBlank()) put(k.trim().lowercase(), v.uppercase())
          }
        }
      }
      CachedProfileMetadata(
        displayName = obj.optString("displayName", ""),
        country = obj.optString("country", "United States"),
        countryFlag = obj.optString("countryFlag", "🇺🇸"),
        bio = obj.optString("bio", "Practicing languages & real-time conversations on LingoSphere!"),
        interests = interests.ifEmpty { listOf("Travel", "Culture", "Music") },
        nativeLanguageCode = obj.optString("nativeLanguageCode", "").takeIf { it.isNotBlank() },
        targetLanguageCode = obj.optString("targetLanguageCode", "").takeIf { it.isNotBlank() },
        proficiencyLevel = obj.optString("proficiencyLevel", "").takeIf { it.isNotBlank() },
        dailyGoalMinutes = obj.optLong("dailyGoalMinutes", -1L).takeIf { it > 0L },
        languageLevels = levelsMap
      )
    } catch (_: Exception) {
      null
    }
  }

  fun clearAllUserData(userId: String) {
    clearAllMemoryAndHistory()
    prefs.edit()
      .remove(KEY_SUBSCRIPTION_STATE)
      .remove(KEY_DOWNLOADED_LESSON_IDS)
      .remove(KEY_OFFLINE_FLASHCARDS)
      .remove(KEY_OFFLINE_QUEUE)
      .remove(KEY_LEARNING_GOALS)
      .remove(KEY_CONVERSATION_SUMMARIES)
      .remove(KEY_REPEATED_MISTAKES)
      .remove(KEY_CONVERSATION_REPLAYS)
      .remove(KEY_COMPLETED_MISSIONS)
      .remove(KEY_PORTFOLIO_SPEAKING)
      .remove(KEY_PORTFOLIO_WRITING)
      .remove(KEY_PORTFOLIO_PRIVACY)
      .remove("$KEY_PROFILE_AVATAR_PREFIX$userId")
      .remove("$KEY_ACCOUNT_SETTINGS_PREFIX$userId")
      .remove("$KEY_CACHED_PROFILE_PREFIX$userId")
      .apply()
  }

  // ---------------------------------------------------------------------------
  // LINGOSPHERE 3.0 — AI LANGUAGE MEMORY, MISTAKE RECOVERY, REPLAY & PORTFOLIO
  // ---------------------------------------------------------------------------

  fun loadLearningGoals(): List<String> {
    val raw = prefs.getString(KEY_LEARNING_GOALS, null)
    if (raw.isNullOrBlank()) {
      return listOf(
        "Hold natural 5-minute conversations with native speakers",
        "Travel and order food confidently without switching languages",
        "Speak smoothly in everyday travel and work scenarios"
      )
    }
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val g = arr.optString(i).trim()
          if (g.isNotBlank()) add(g)
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveLearningGoals(goals: List<String>) {
    val arr = JSONArray()
    goals.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { arr.put(it) }
    prefs.edit().putString(KEY_LEARNING_GOALS, arr.toString()).apply()
  }

  fun loadConversationHistorySummaries(): List<String> {
    val raw = prefs.getString(KEY_CONVERSATION_SUMMARIES, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val item = arr.optString(i).trim()
          if (item.isNotBlank()) add(item)
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun appendConversationHistorySummary(summary: String) {
    if (summary.isBlank()) return
    val updated = (listOf(summary.trim()) + loadConversationHistorySummaries())
      .distinct()
      .take(15)
    val arr = JSONArray()
    updated.forEach { arr.put(it) }
    prefs.edit().putString(KEY_CONVERSATION_SUMMARIES, arr.toString()).apply()
  }

  private fun loadAllRepeatedMistakeSummariesInternal(): List<com.example.data.model.RepeatedMistakeSummary> {
    val raw = prefs.getString(KEY_REPEATED_MISTAKES, null)
    if (raw.isNullOrBlank()) {
      val notebook = loadMistakeNotebook()
      return notebook.mapIndexed { idx, entry ->
        val isMastered = entry.resolvedCount > 0
        com.example.data.model.RepeatedMistakeSummary(
          id = entry.id,
          category = entry.category,
          ruleSummary = entry.explanationNative,
          exampleWrong = entry.originalText,
          exampleCorrected = entry.correctedText,
          occurrenceCount = if (idx == 0) 2 else 1,
          recoveryAttempts = entry.resolvedCount,
          masteryScore = if (isMastered) 90 else 42,
          improved = isMastered,
          targetLanguage = Language.findByCode(entry.languageCode).name
        )
      }
    }
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            com.example.data.model.RepeatedMistakeSummary(
              id = obj.optString("id", "rm_$i"),
              category = obj.optString("category", "Grammar & Syntax"),
              ruleSummary = obj.optString("ruleSummary", ""),
              exampleWrong = obj.optString("exampleWrong", ""),
              exampleCorrected = obj.optString("exampleCorrected", ""),
              occurrenceCount = obj.optInt("occurrenceCount", 1),
              recoveryAttempts = obj.optInt("recoveryAttempts", 0),
              masteryScore = obj.optInt("masteryScore", 40),
              improved = obj.optBoolean("improved", false),
              targetLanguage = obj.optString("targetLanguage", "")
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun loadRepeatedMistakeSummaries(targetLanguage: String? = null): List<com.example.data.model.RepeatedMistakeSummary> {
    val all = loadAllRepeatedMistakeSummariesInternal()
    if (targetLanguage.isNullOrBlank()) return all
    return all.filter { it.targetLanguage.equals(targetLanguage, ignoreCase = true) }
  }

  fun saveRepeatedMistakeSummaries(items: List<com.example.data.model.RepeatedMistakeSummary>) {
    val arr = JSONArray()
    items.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("category", item.category)
          put("ruleSummary", item.ruleSummary)
          put("exampleWrong", item.exampleWrong)
          put("exampleCorrected", item.exampleCorrected)
          put("occurrenceCount", item.occurrenceCount)
          put("recoveryAttempts", item.recoveryAttempts)
          put("masteryScore", item.masteryScore)
          put("improved", item.improved)
          put("targetLanguage", item.targetLanguage)
        }
      )
    }
    prefs.edit().putString(KEY_REPEATED_MISTAKES, arr.toString()).apply()
  }

  fun recordOrIncrementRepeatedMistake(
    category: String,
    ruleSummary: String,
    exampleWrong: String,
    exampleCorrected: String,
    targetLanguage: String = ""
  ): List<com.example.data.model.RepeatedMistakeSummary> {
    val current = loadAllRepeatedMistakeSummariesInternal().toMutableList()
    val existingIdx = current.indexOfFirst { item ->
      val sameLang = targetLanguage.isBlank() || item.targetLanguage.isBlank() || item.targetLanguage.equals(targetLanguage, ignoreCase = true)
      sameLang && (item.category.equals(category, ignoreCase = true) ||
        item.exampleCorrected.equals(exampleCorrected, ignoreCase = true))
    }
    if (existingIdx >= 0) {
      val existing = current[existingIdx]
      current[existingIdx] = existing.copy(
        occurrenceCount = existing.occurrenceCount + 1,
        exampleWrong = exampleWrong.ifBlank { existing.exampleWrong },
        exampleCorrected = exampleCorrected.ifBlank { existing.exampleCorrected },
        masteryScore = (existing.masteryScore - 10).coerceAtLeast(15),
        improved = false,
        targetLanguage = targetLanguage.ifBlank { existing.targetLanguage }
      )
    } else {
      current.add(
        0,
        com.example.data.model.RepeatedMistakeSummary(
          id = "rm_${System.currentTimeMillis()}",
          category = category,
          ruleSummary = ruleSummary,
          exampleWrong = exampleWrong,
          exampleCorrected = exampleCorrected,
          occurrenceCount = 2,
          recoveryAttempts = 0,
          masteryScore = 35,
          improved = false,
          targetLanguage = targetLanguage
        )
      )
    }
    saveRepeatedMistakeSummaries(current)
    return if (targetLanguage.isNotBlank()) {
      current.filter { it.targetLanguage.equals(targetLanguage, ignoreCase = true) }
    } else current
  }

  fun recordMistakeRecoveryProgress(
    mistakeId: String,
    scoreEarned: Int
  ): com.example.data.model.RepeatedMistakeSummary? {
    val current = loadAllRepeatedMistakeSummariesInternal().toMutableList()
    val idx = current.indexOfFirst { it.id == mistakeId }
    if (idx < 0) return null
    val existing = current[idx]
    val newMastery = if (scoreEarned >= 75) {
      (existing.masteryScore + 28).coerceAtMost(100)
    } else {
      (existing.masteryScore + 8).coerceAtMost(100)
    }
    val updated = existing.copy(
      recoveryAttempts = existing.recoveryAttempts + 1,
      masteryScore = newMastery,
      improved = newMastery >= 75
    )
    current[idx] = updated
    saveRepeatedMistakeSummaries(current)
    if (updated.improved) {
      val notebook = loadMistakeNotebook().map {
        if (it.id == mistakeId || it.category.equals(updated.category, ignoreCase = true)) {
          it.copy(resolvedCount = it.resolvedCount + 1)
        } else it
      }
      saveMistakeNotebook(notebook)
    }
    return updated
  }

  fun buildAiLanguageMemoryProfile(
    targetLanguage: String,
    cefrLevel: String,
    extraWeakWords: List<String> = emptyList()
  ): com.example.data.model.AiLanguageMemoryProfile {
    val langCode = Language.SUPPORTED_LANGUAGES.firstOrNull {
      it.name.equals(targetLanguage, ignoreCase = true) || it.code.equals(targetLanguage, ignoreCase = true)
    }?.code
    val srsWeak = loadSrsMetadataMap().values
      .filter { it.easeFactor < 2.25 || it.lapses > 0 || it.intervalDays <= 1 }
      .map { it.wordId.removePrefix("custom_") }
    val mistakeWords = loadMistakeNotebook()
      .filter { it.resolvedCount == 0 && (langCode == null || it.languageCode.equals(langCode, ignoreCase = true)) }
      .map { it.correctedText }
    val weakVocab = (extraWeakWords + srsWeak + mistakeWords)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .take(10)

    val pronProblems = loadPronunciationHistory()
      .flatMap { it.problematicSounds }
      .distinct()
      .take(6)

    val repeatedMistakes = loadAllRepeatedMistakeSummariesInternal().filter {
      it.targetLanguage.equals(targetLanguage, ignoreCase = true) || it.targetLanguage.isBlank()
    }

    return com.example.data.model.AiLanguageMemoryProfile(
      targetLanguage = targetLanguage,
      cefrLevel = cefrLevel,
      learningGoals = loadLearningGoals(),
      vocabularyWeaknesses = weakVocab,
      repeatedGrammarMistakes = repeatedMistakes,
      pronunciationDifficulties = pronProblems,
      conversationHistorySummaries = loadConversationHistorySummaries(),
      lastUpdatedEpochMs = System.currentTimeMillis()
    )
  }

  private fun loadAllConversationReplaySessionsInternal(): List<com.example.data.model.ConversationReplaySession> {
    val raw = prefs.getString(KEY_CONVERSATION_REPLAYS, null)
    if (raw.isNullOrBlank()) return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val turnsArr = obj.optJSONArray("turns") ?: JSONArray()
          val turns = buildList {
            for (j in 0 until turnsArr.length()) {
              val tObj = turnsArr.optJSONObject(j) ?: continue
              add(
                com.example.data.model.ReplayTurnItem(
                  turnId = tObj.optString("turnId", "turn_${i}_$j"),
                  speaker = tObj.optString("speaker", "user"),
                  originalText = tObj.optString("originalText", ""),
                  improvedNativeVersion = tObj.optString("improvedNativeVersion", ""),
                  pronunciationIpaOrPhonetic = tObj.optString("pronunciationIpaOrPhonetic", ""),
                  correctionExplanation = tObj.optString("correctionExplanation", ""),
                  initialScore = tObj.optInt("initialScore", 80),
                  practicedRetryScore = if (tObj.has("practicedRetryScore")) tObj.optInt("practicedRetryScore") else null,
                  lastPracticedTranscript = tObj.optString("lastPracticedTranscript", "").takeIf { it.isNotBlank() }
                )
              )
            }
          }
          add(
            com.example.data.model.ConversationReplaySession(
              id = obj.optString("id", "rep_$i"),
              scenarioTitle = obj.optString("scenarioTitle", "AI Speaking Session"),
              targetLanguage = obj.optString("targetLanguage", ""),
              cefrLevel = obj.optString("cefrLevel", "A2"),
              timestampLabel = obj.optString("timestampLabel", "Recent Session"),
              createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
              overallScore = obj.optInt("overallScore", 84),
              turns = turns
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun loadConversationReplaySessions(targetLanguage: String = "Spanish"): List<com.example.data.model.ConversationReplaySession> {
    return loadAllConversationReplaySessionsInternal().filter {
      it.targetLanguage.equals(targetLanguage, ignoreCase = true)
    }
  }

  fun saveConversationReplaySession(session: com.example.data.model.ConversationReplaySession) {
    val updated = (listOf(session) + loadAllConversationReplaySessionsInternal().filterNot { it.id == session.id }).take(30)
    persistConversationReplaySessions(updated)
  }

  fun updateReplayTurnPracticeScore(
    sessionId: String,
    turnId: String,
    practicedTranscript: String,
    newScore: Int,
    targetLanguage: String
  ): List<com.example.data.model.ConversationReplaySession> {
    val all = loadAllConversationReplaySessionsInternal().map { session ->
      if (session.id != sessionId) session
      else {
        val updatedTurns = session.turns.map { turn ->
          if (turn.turnId != turnId) turn
          else turn.copy(
            practicedRetryScore = newScore,
            lastPracticedTranscript = practicedTranscript
          )
        }
        session.copy(turns = updatedTurns)
      }
    }
    persistConversationReplaySessions(all)
    return all.filter { it.targetLanguage.equals(targetLanguage, ignoreCase = true) }
  }

  private fun persistConversationReplaySessions(sessions: List<com.example.data.model.ConversationReplaySession>) {
    val arr = JSONArray()
    sessions.forEach { s ->
      val turnsArr = JSONArray()
      s.turns.forEach { t ->
        turnsArr.put(
          JSONObject().apply {
            put("turnId", t.turnId)
            put("speaker", t.speaker)
            put("originalText", t.originalText)
            put("improvedNativeVersion", t.improvedNativeVersion)
            put("pronunciationIpaOrPhonetic", t.pronunciationIpaOrPhonetic)
            put("correctionExplanation", t.correctionExplanation)
            put("initialScore", t.initialScore)
            t.practicedRetryScore?.let { put("practicedRetryScore", it) }
            t.lastPracticedTranscript?.let { put("lastPracticedTranscript", it) }
          }
        )
      }
      arr.put(
        JSONObject().apply {
          put("id", s.id)
          put("scenarioTitle", s.scenarioTitle)
          put("targetLanguage", s.targetLanguage)
          put("cefrLevel", s.cefrLevel)
          put("timestampLabel", s.timestampLabel)
          put("createdAtEpochMs", s.createdAtEpochMs)
          put("overallScore", s.overallScore)
          put("turns", turnsArr)
        }
      )
    }
    prefs.edit().putString(KEY_CONVERSATION_REPLAYS, arr.toString()).apply()
  }

  fun loadCompletedDailyLifeMissions(): List<com.example.data.model.DailyLifeMissionEvaluation> {
    val raw = prefs.getString(KEY_COMPLETED_MISSIONS, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val corrArr = obj.optJSONArray("keyCorrections") ?: JSONArray()
          val corrections = buildList {
            for (j in 0 until corrArr.length()) {
              val c = corrArr.optString(j)
              if (c.isNotBlank()) add(c)
            }
          }
          add(
            com.example.data.model.DailyLifeMissionEvaluation(
              missionId = obj.optString("missionId", ""),
              missionTitle = obj.optString("missionTitle", ""),
              targetLanguage = obj.optString("targetLanguage", "Spanish"),
              submissionMode = obj.optString("submissionMode", "Spoken"),
              userResponseText = obj.optString("userResponseText", ""),
              overallScore = obj.optInt("overallScore", 85),
              fluencyAndCoherenceScore = obj.optInt("fluencyAndCoherenceScore", 84),
              grammarAndVocabScore = obj.optInt("grammarAndVocabScore", 86),
              stayedInTargetLanguage = obj.optBoolean("stayedInTargetLanguage", true),
              completedSuccessfully = obj.optBoolean("completedSuccessfully", true),
              aiFeedback = obj.optString("aiFeedback", ""),
              improvedModelResponse = obj.optString("improvedModelResponse", ""),
              keyCorrections = corrections,
              completedAtEpochMs = obj.optLong("completedAtEpochMs", System.currentTimeMillis()),
              audioSampleFilePath = obj.optString("audioSampleFilePath", "").takeIf { it.isNotBlank() }
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveCompletedDailyLifeMission(evaluation: com.example.data.model.DailyLifeMissionEvaluation) {
    val updated = (listOf(evaluation) + loadCompletedDailyLifeMissions().filterNot { it.missionId == evaluation.missionId }).take(30)
    val arr = JSONArray()
    updated.forEach { ev ->
      val corrArr = JSONArray()
      ev.keyCorrections.forEach { corrArr.put(it) }
      arr.put(
        JSONObject().apply {
          put("missionId", ev.missionId)
          put("missionTitle", ev.missionTitle)
          put("targetLanguage", ev.targetLanguage)
          put("submissionMode", ev.submissionMode)
          put("userResponseText", ev.userResponseText)
          put("overallScore", ev.overallScore)
          put("fluencyAndCoherenceScore", ev.fluencyAndCoherenceScore)
          put("grammarAndVocabScore", ev.grammarAndVocabScore)
          put("stayedInTargetLanguage", ev.stayedInTargetLanguage)
          put("completedSuccessfully", ev.completedSuccessfully)
          put("aiFeedback", ev.aiFeedback)
          put("improvedModelResponse", ev.improvedModelResponse)
          put("keyCorrections", corrArr)
          put("completedAtEpochMs", ev.completedAtEpochMs)
          ev.audioSampleFilePath?.let { put("audioSampleFilePath", it) }
        }
      )
    }
    prefs.edit().putString(KEY_COMPLETED_MISSIONS, arr.toString()).apply()
  }

  private fun loadAllPortfolioSpeakingSamplesInternal(): List<com.example.data.model.PortfolioSpeakingSample> {
    val raw = prefs.getString(KEY_PORTFOLIO_SPEAKING, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            com.example.data.model.PortfolioSpeakingSample(
              id = obj.optString("id", "ps_$i"),
              title = obj.optString("title", "Speaking Sample"),
              targetLanguage = obj.optString("targetLanguage", ""),
              cefrLevel = obj.optString("cefrLevel", "A2"),
              spokenTranscript = obj.optString("spokenTranscript", ""),
              improvedVersion = obj.optString("improvedVersion", ""),
              pronunciationScore = obj.optInt("pronunciationScore", 82),
              fluencyScore = obj.optInt("fluencyScore", 80),
              durationSeconds = obj.optInt("durationSeconds", 18),
              recordedAtEpochMs = obj.optLong("recordedAtEpochMs", System.currentTimeMillis()),
              dateLabel = obj.optString("dateLabel", "Today"),
              audioFilePath = obj.optString("audioFilePath", "").takeIf { it.isNotBlank() },
              isPrivate = obj.optBoolean("isPrivate", true)
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun loadPortfolioSpeakingSamples(targetLanguage: String = "Spanish"): List<com.example.data.model.PortfolioSpeakingSample> {
    return loadAllPortfolioSpeakingSamplesInternal().filter {
      it.targetLanguage.equals(targetLanguage, ignoreCase = true)
    }
  }

  fun savePortfolioSpeakingSample(sample: com.example.data.model.PortfolioSpeakingSample) {
    val current = loadAllPortfolioSpeakingSamplesInternal().filterNot { it.id == sample.id }
    val updated = (listOf(sample) + current).sortedByDescending { it.recordedAtEpochMs }
    persistPortfolioSpeakingSamples(updated)
  }

  fun deletePortfolioSpeakingSample(sampleId: String, targetLanguage: String = "Spanish"): List<com.example.data.model.PortfolioSpeakingSample> {
    val updated = loadAllPortfolioSpeakingSamplesInternal().filterNot { it.id == sampleId }
    persistPortfolioSpeakingSamples(updated)
    return updated.filter { it.targetLanguage.equals(targetLanguage, ignoreCase = true) }
  }

  fun togglePortfolioSpeakingSamplePrivacy(sampleId: String, targetLanguage: String = "Spanish"): List<com.example.data.model.PortfolioSpeakingSample> {
    val updated = loadAllPortfolioSpeakingSamplesInternal().map {
      if (it.id == sampleId) it.copy(isPrivate = !it.isPrivate) else it
    }
    persistPortfolioSpeakingSamples(updated)
    return updated.filter { it.targetLanguage.equals(targetLanguage, ignoreCase = true) }
  }

  fun deleteAllPortfolioSpeakingSamples() {
    prefs.edit().putString(KEY_PORTFOLIO_SPEAKING, JSONArray().toString()).apply()
  }

  private fun persistPortfolioSpeakingSamples(samples: List<com.example.data.model.PortfolioSpeakingSample>) {
    val arr = JSONArray()
    samples.forEach { s ->
      arr.put(
        JSONObject().apply {
          put("id", s.id)
          put("title", s.title)
          put("targetLanguage", s.targetLanguage)
          put("cefrLevel", s.cefrLevel)
          put("spokenTranscript", s.spokenTranscript)
          put("improvedVersion", s.improvedVersion)
          put("pronunciationScore", s.pronunciationScore)
          put("fluencyScore", s.fluencyScore)
          put("durationSeconds", s.durationSeconds)
          put("recordedAtEpochMs", s.recordedAtEpochMs)
          put("dateLabel", s.dateLabel)
          s.audioFilePath?.let { put("audioFilePath", it) }
          put("isPrivate", s.isPrivate)
        }
      )
    }
    prefs.edit().putString(KEY_PORTFOLIO_SPEAKING, arr.toString()).apply()
  }

  private fun loadAllPortfolioWritingSamplesInternal(): List<com.example.data.model.PortfolioWritingSample> {
    val raw = prefs.getString(KEY_PORTFOLIO_WRITING, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val impArr = obj.optJSONArray("keyImprovements") ?: JSONArray()
          val imps = buildList {
            for (j in 0 until impArr.length()) {
              val s = impArr.optString(j)
              if (s.isNotBlank()) add(s)
            }
          }
          add(
            com.example.data.model.PortfolioWritingSample(
              id = obj.optString("id", "wr_$i"),
              promptTitle = obj.optString("promptTitle", "Writing Entry"),
              targetLanguage = obj.optString("targetLanguage", ""),
              originalDraft = obj.optString("originalDraft", ""),
              improvedRevision = obj.optString("improvedRevision", ""),
              grammarGainScore = obj.optInt("grammarGainScore", 18),
              keyImprovements = imps,
              createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
              dateLabel = obj.optString("dateLabel", "Today")
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun loadPortfolioWritingSamples(targetLanguage: String = "Spanish"): List<com.example.data.model.PortfolioWritingSample> {
    return loadAllPortfolioWritingSamplesInternal().filter {
      it.targetLanguage.equals(targetLanguage, ignoreCase = true)
    }
  }

  fun savePortfolioWritingSample(sample: com.example.data.model.PortfolioWritingSample) {
    val updated = (listOf(sample) + loadAllPortfolioWritingSamplesInternal().filterNot { it.id == sample.id }).take(40)
    val arr = JSONArray()
    updated.forEach { w ->
      val impArr = JSONArray()
      w.keyImprovements.forEach { impArr.put(it) }
      arr.put(
        JSONObject().apply {
          put("id", w.id)
          put("promptTitle", w.promptTitle)
          put("targetLanguage", w.targetLanguage)
          put("originalDraft", w.originalDraft)
          put("improvedRevision", w.improvedRevision)
          put("grammarGainScore", w.grammarGainScore)
          put("keyImprovements", impArr)
          put("createdAtEpochMs", w.createdAtEpochMs)
          put("dateLabel", w.dateLabel)
        }
      )
    }
    prefs.edit().putString(KEY_PORTFOLIO_WRITING, arr.toString()).apply()
  }

  fun loadPortfolioPrivacySettings(): com.example.data.model.PortfolioPrivacySettings {
    val raw = prefs.getString(KEY_PORTFOLIO_PRIVACY, null) ?: return com.example.data.model.PortfolioPrivacySettings()
    return try {
      val obj = JSONObject(raw)
      com.example.data.model.PortfolioPrivacySettings(
        defaultPrivateRecordings = obj.optBoolean("defaultPrivateRecordings", true),
        allowProgressSharingInCommunity = obj.optBoolean("allowProgressSharingInCommunity", false),
        storeAudioOnDeviceOnly = obj.optBoolean("storeAudioOnDeviceOnly", true)
      )
    } catch (_: Exception) {
      com.example.data.model.PortfolioPrivacySettings()
    }
  }

  fun savePortfolioPrivacySettings(settings: com.example.data.model.PortfolioPrivacySettings) {
    val obj = JSONObject().apply {
      put("defaultPrivateRecordings", settings.defaultPrivateRecordings)
      put("allowProgressSharingInCommunity", settings.allowProgressSharingInCommunity)
      put("storeAudioOnDeviceOnly", settings.storeAudioOnDeviceOnly)
    }
    prefs.edit().putString(KEY_PORTFOLIO_PRIVACY, obj.toString()).apply()
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PRACTICAL DIFFERENTIATED FEATURES PERSISTENCE (FEATURES 1 - 12)
  // ═══════════════════════════════════════════════════════════════════════════

  fun getMediaPrivacyConsent(): com.example.data.model.MediaPrivacyConsentState {
    val raw = prefs.getString(KEY_MEDIA_PRIVACY_CONSENT, null)
      ?: return com.example.data.model.MediaPrivacyConsentState()
    return runCatching {
      val obj = JSONObject(raw)
      com.example.data.model.MediaPrivacyConsentState(
        cameraCaptureConsent = obj.optBoolean("cameraCaptureConsent", true),
        microphoneSpeakingConsent = obj.optBoolean("microphoneSpeakingConsent", true),
        storeRecordingsOnDevice = obj.optBoolean("storeRecordingsOnDevice", true),
        syntheticFluentReferenceConsent = obj.optBoolean("syntheticFluentReferenceConsent", true),
        lastUpdatedEpochMs = obj.optLong("lastUpdatedEpochMs", System.currentTimeMillis())
      )
    }.getOrDefault(com.example.data.model.MediaPrivacyConsentState())
  }

  fun saveMediaPrivacyConsent(state: com.example.data.model.MediaPrivacyConsentState) {
    val obj = JSONObject().apply {
      put("cameraCaptureConsent", state.cameraCaptureConsent)
      put("microphoneSpeakingConsent", state.microphoneSpeakingConsent)
      put("storeRecordingsOnDevice", state.storeRecordingsOnDevice)
      put("syntheticFluentReferenceConsent", state.syntheticFluentReferenceConsent)
      put("lastUpdatedEpochMs", System.currentTimeMillis())
    }
    prefs.edit().putString(KEY_MEDIA_PRIVACY_CONSENT, obj.toString()).apply()
  }

  fun getLivingWorldProgressMap(): Map<String, com.example.data.model.LivingWorldCharacterProgress> {
    val raw = prefs.getString(KEY_LIVING_WORLD_PROGRESS, null) ?: return emptyMap()
    return runCatching {
      val root = JSONObject(raw)
      val result = mutableMapOf<String, com.example.data.model.LivingWorldCharacterProgress>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val locId = keys.next()
        val obj = root.optJSONObject(locId) ?: continue
        val completedArr = obj.optJSONArray("completedTaskIds") ?: JSONArray()
        val factsArr = obj.optJSONArray("rememberedLearnerFacts") ?: JSONArray()
        result[locId] = com.example.data.model.LivingWorldCharacterProgress(
          locationId = locId,
          characterName = obj.optString("characterName"),
          trustScore = obj.optInt("trustScore", 20),
          completedTaskIds = (0 until completedArr.length()).mapNotNull { completedArr.optString(it).takeIf(String::isNotBlank) },
          rememberedLearnerFacts = (0 until factsArr.length()).mapNotNull { factsArr.optString(it).takeIf(String::isNotBlank) },
          lastConversationSummary = obj.optString("lastConversationSummary"),
          totalSpokenTurns = obj.optInt("totalSpokenTurns", 0)
        )
      }
      result
    }.getOrDefault(emptyMap())
  }

  fun saveLivingWorldProgress(progress: com.example.data.model.LivingWorldCharacterProgress) {
    val current = getLivingWorldProgressMap().toMutableMap()
    current[progress.locationId] = progress
    val root = JSONObject()
    current.forEach { (locId, item) ->
      val obj = JSONObject().apply {
        put("locationId", item.locationId)
        put("characterName", item.characterName)
        put("trustScore", item.trustScore.coerceIn(0, 100))
        put("completedTaskIds", JSONArray(item.completedTaskIds.distinct()))
        put("rememberedLearnerFacts", JSONArray(item.rememberedLearnerFacts.distinct().takeLast(12)))
        put("lastConversationSummary", item.lastConversationSummary)
        put("totalSpokenTurns", item.totalSpokenTurns)
      }
      root.put(locId, obj)
    }
    prefs.edit().putString(KEY_LIVING_WORLD_PROGRESS, root.toString()).apply()
  }

  fun getOfflineEmergencyPackLanguages(): Set<String> {
    return prefs.getStringSet(KEY_OFFLINE_EMERGENCY_LANGS, setOf("es", "fr", "ja")) ?: setOf("es", "fr", "ja")
  }

  fun setOfflineEmergencyPackDownloaded(languageCode: String, downloaded: Boolean): Set<String> {
    val updated = getOfflineEmergencyPackLanguages().toMutableSet()
    val code = languageCode.lowercase()
    if (downloaded) updated.add(code) else updated.remove(code)
    prefs.edit().putStringSet(KEY_OFFLINE_EMERGENCY_LANGS, updated).apply()
    return updated
  }

  fun getSelectedHabitRoutineIds(): Set<String> {
    return prefs.getStringSet(
      KEY_HABIT_ROUTINE_IDS,
      setOf(
        com.example.data.model.HabitRoutineSlot.MORNING_COFFEE.id,
        com.example.data.model.HabitRoutineSlot.EVENING_REFLECTION.id
      )
    ) ?: setOf(com.example.data.model.HabitRoutineSlot.MORNING_COFFEE.id)
  }

  fun toggleHabitRoutineSlot(slotId: String): Set<String> {
    val current = getSelectedHabitRoutineIds().toMutableSet()
    if (current.contains(slotId)) {
      if (current.size > 1) current.remove(slotId)
    } else {
      current.add(slotId)
    }
    prefs.edit().putStringSet(KEY_HABIT_ROUTINE_IDS, current).apply()
    return current
  }

  fun getVerifiedHabitCompletions(): List<com.example.data.model.VerifiedHabitCompletion> {
    val raw = prefs.getString(KEY_VERIFIED_HABIT_COMPLETIONS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            com.example.data.model.VerifiedHabitCompletion(
              id = obj.optString("id"),
              routineSlotId = obj.optString("routineSlotId"),
              languageCode = obj.optString("languageCode", "es"),
              missionId = obj.optString("missionId"),
              userSpokenOrWrittenTranscript = obj.optString("userSpokenOrWrittenTranscript"),
              wordCount = obj.optInt("wordCount", 0),
              feedbackSummary = obj.optString("feedbackSummary"),
              completedAtEpochMs = obj.optLong("completedAtEpochMs", System.currentTimeMillis())
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun recordVerifiedHabitCompletion(completion: com.example.data.model.VerifiedHabitCompletion): List<com.example.data.model.VerifiedHabitCompletion> {
    val updated = (listOf(completion) + getVerifiedHabitCompletions().filterNot { it.id == completion.id }).take(60)
    val arr = JSONArray()
    updated.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("routineSlotId", item.routineSlotId)
          put("languageCode", item.languageCode)
          put("missionId", item.missionId)
          put("userSpokenOrWrittenTranscript", item.userSpokenOrWrittenTranscript)
          put("wordCount", item.wordCount)
          put("feedbackSummary", item.feedbackSummary)
          put("completedAtEpochMs", item.completedAtEpochMs)
        }
      )
    }
    prefs.edit().putString(KEY_VERIFIED_HABIT_COMPLETIONS, arr.toString()).apply()
    return updated
  }

  fun getCompletedRepairScenarioIds(): Set<String> {
    return prefs.getStringSet(KEY_COMPLETED_REPAIR_SCENARIOS, emptySet()) ?: emptySet()
  }

  fun recordCompletedRepairScenario(scenarioId: String): Set<String> {
    val updated = getCompletedRepairScenarioIds().toMutableSet().apply { add(scenarioId) }
    prefs.edit().putStringSet(KEY_COMPLETED_REPAIR_SCENARIOS, updated).apply()
    return updated
  }

  fun getFutureMeComparisons(): List<com.example.data.model.FutureMeComparisonResult> {
    val raw = prefs.getString(KEY_FUTURE_ME_COMPARISONS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val drillsArr = obj.optJSONArray("drills") ?: JSONArray()
          val drills = (0 until drillsArr.length()).mapNotNull { idx ->
            val d = drillsArr.optJSONObject(idx) ?: return@mapNotNull null
            com.example.data.model.FutureMePracticeDrill(
              id = d.optString("id"),
              focusArea = d.optString("focusArea"),
              drillInstruction = d.optString("drillInstruction"),
              targetPhrase = d.optString("targetPhrase"),
              romanization = d.optString("romanization")
            )
          }
          add(
            com.example.data.model.FutureMeComparisonResult(
              id = obj.optString("id"),
              languageCode = obj.optString("languageCode", "es"),
              learnerTranscript = obj.optString("learnerTranscript"),
              syntheticFluentReferenceText = obj.optString("syntheticFluentReferenceText"),
              syntheticFluentRomanization = obj.optString("syntheticFluentRomanization"),
              explicitSyntheticLabel = "Synthetic Fluent Reference — Not Voice Cloning",
              consentVerified = obj.optBoolean("consentVerified", true),
              pacingAndFlowComparison = obj.optString("pacingAndFlowComparison"),
              phrasingUpgradeExplanation = obj.optString("phrasingUpgradeExplanation"),
              actionableDrills = drills,
              createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun recordFutureMeComparison(result: com.example.data.model.FutureMeComparisonResult): List<com.example.data.model.FutureMeComparisonResult> {
    if (!getMediaPrivacyConsent().storeRecordingsOnDevice) {
      return getFutureMeComparisons()
    }
    val updated = (listOf(result) + getFutureMeComparisons().filterNot { it.id == result.id }).take(25)
    val arr = JSONArray()
    updated.forEach { item ->
      val drillsArr = JSONArray()
      item.actionableDrills.forEach { d ->
        drillsArr.put(
          JSONObject().apply {
            put("id", d.id)
            put("focusArea", d.focusArea)
            put("drillInstruction", d.drillInstruction)
            put("targetPhrase", d.targetPhrase)
            put("romanization", d.romanization)
          }
        )
      }
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("languageCode", item.languageCode)
          put("learnerTranscript", item.learnerTranscript)
          put("syntheticFluentReferenceText", item.syntheticFluentReferenceText)
          put("syntheticFluentRomanization", item.syntheticFluentRomanization)
          put("consentVerified", item.consentVerified)
          put("pacingAndFlowComparison", item.pacingAndFlowComparison)
          put("phrasingUpgradeExplanation", item.phrasingUpgradeExplanation)
          put("drills", drillsArr)
          put("createdAtEpochMs", item.createdAtEpochMs)
        }
      )
    }
    prefs.edit().putString(KEY_FUTURE_ME_COMPARISONS, arr.toString()).apply()
    return updated
  }

  fun buildLanguageSkillPassportSummary(
    languageCode: String,
    flashcards: List<FlashcardProgress>,
    speakingSessions: List<SpeakingSession>
  ): com.example.data.model.LanguageSkillPassportSummary {
    val code = languageCode.lowercase()
    val lang = Language.findByCode(code)
    val portfolioSamples = loadPortfolioSpeakingSamples(lang.name)
    val langLocations = com.example.data.model.PracticalFeaturesCatalog.livingWorldLocationsForLanguage(code).map { it.id }.toSet()
    val livingWorld = getLivingWorldProgressMap().values.filter { it.locationId.startsWith("lw_${code}_") || it.locationId in langLocations }
    val completedLivingTasks = livingWorld.sumOf { it.completedTaskIds.size }
    val habitCompletions = getVerifiedHabitCompletions().filter { it.languageCode.equals(code, ignoreCase = true) }
    val repairCount = getCompletedRepairScenarioIds().count { it.startsWith("rep_${code}_") }
    val missionsDone = loadCompletedDailyLifeMissions().count { it.targetLanguage.equals(lang.name, ignoreCase = true) }
    val vocabMastered = flashcards.count { it.languageCode.equals(code, ignoreCase = true) && it.boxLevel >= 3L }
    val futureMeCount = getFutureMeComparisons().count { it.languageCode.equals(code, ignoreCase = true) }
    val totalSpeakingCount = portfolioSamples.size + speakingSessions.count { it.languageCode.equals(code, ignoreCase = true) }
    val improvedMistakesCount = loadRepeatedMistakeSummaries(lang.name).count { it.improved }

    val competencies = listOf(
      com.example.data.model.SkillPassportCompetency(
        id = "comp_real_world_tasks",
        domainTitle = "Real-World Situated Communication",
        practicalCanDoStatement = "Can complete unscripted neighborhood and daily-life speaking tasks (ordering, inquiring, coordinating plans) in ${lang.name}.",
        verifiedEvidenceCount = completedLivingTasks + missionsDone + habitCompletions.size,
        latestEvidenceSummary = if (completedLivingTasks + missionsDone + habitCompletions.size > 0) {
          "$completedLivingTasks Living World tasks, $missionsDone daily missions, and ${habitCompletions.size} habit speaking check-ins completed."
        } else {
          "Complete a Living Language World task or Habit Coach speaking mission to log evidence."
        },
        measurableGrowthNote = "Tracks verified multi-word spoken task completions rather than passive screen taps.",
        isDemonstrated = (completedLivingTasks + missionsDone + habitCompletions.size) > 0
      ),
      com.example.data.model.SkillPassportCompetency(
        id = "comp_conversation_repair",
        domainTitle = "Conversation Repair & Clarification",
        practicalCanDoStatement = "Can politely ask speakers to slow down, paraphrase unknown words, and recover from misunderstandings in real time.",
        verifiedEvidenceCount = repairCount,
        latestEvidenceSummary = if (repairCount > 0) {
          "$repairCount Conversation Repair Lab scenarios successfully resolved."
        } else {
          "Practice a scenario in the Conversation Repair Lab to demonstrate recovery skills."
        },
        measurableGrowthNote = "Demonstrates ability to keep conversations going without switching back to native language.",
        isDemonstrated = repairCount > 0
      ),
      com.example.data.model.SkillPassportCompetency(
        id = "comp_spoken_fluency",
        domainTitle = "Recorded Speaking & Prosody Progression",
        practicalCanDoStatement = "Can produce intelligible multi-word spoken responses and compare rhythm against fluent reference models.",
        verifiedEvidenceCount = totalSpeakingCount + futureMeCount,
        latestEvidenceSummary = if (portfolioSamples.size >= 2) {
          val earliest = portfolioSamples.last()
          val latest = portfolioSamples.first()
          val delta = latest.pronunciationScore - earliest.pronunciationScore
          val sign = if (delta >= 0) "+$delta%" else "$delta%"
          "$totalSpeakingCount recorded sessions ($sign articulation change across saved samples)."
        } else {
          "$totalSpeakingCount speaking recordings logged in ${lang.name}."
        },
        measurableGrowthNote = "Compares your earliest vs. most recent recorded speaking samples.",
        isDemonstrated = totalSpeakingCount > 0
      ),
      com.example.data.model.SkillPassportCompetency(
        id = "comp_active_vocabulary",
        domainTitle = "Active Contextual Vocabulary & Grammar Recovery",
        practicalCanDoStatement = "Can retain and apply core vocabulary and recover from repeated grammar patterns.",
        verifiedEvidenceCount = vocabMastered + improvedMistakesCount,
        latestEvidenceSummary = "$vocabMastered mastered vocabulary items and $improvedMistakesCount targeted mistake recovery drills.",
        measurableGrowthNote = "Combines spaced-repetition mastery with mistake-recovery practice.",
        isDemonstrated = vocabMastered > 0 || improvedMistakesCount > 0
      )
    )

    return com.example.data.model.LanguageSkillPassportSummary(
      languageCode = code,
      languageName = lang.name,
      competencies = competencies,
      totalSpeakingRecordingsCount = totalSpeakingCount,
      totalCompletedPracticalTasksCount = completedLivingTasks + missionsDone + habitCompletions.size,
      vocabularyMasteredCount = vocabMastered,
      grammarRepairDemonstrationsCount = repairCount + improvedMistakesCount
    )
  }

  // ===========================================================================
  // 17. AI CONTINUOUS LEARNING CONTENT REPOSITORY & CACHE
  // ===========================================================================

  fun saveAiReadingPassages(targetLangCode: String, passages: List<ReadingAcademyPassage>) {
    val arr = JSONArray()
    passages.forEach { p ->
      val obj = JSONObject().apply {
        put("id", p.id)
        put("languageCode", p.languageCode)
        put("cefrLevel", p.cefrLevel)
        put("category", p.category)
        put("title", p.title)
        put("paragraphs", JSONArray(p.paragraphs))
        put("paragraphTranslations", JSONArray(p.paragraphTranslations))
        val glossObj = JSONObject()
        p.glossary.forEach { (k, g) ->
          val gObj = JSONObject().apply {
            put("word", g.word)
            put("translation", g.translation)
            put("partOfSpeech", g.partOfSpeech)
            put("definition", g.definition)
            put("phonetic", g.phonetic)
          }
          glossObj.put(k, gObj)
        }
        put("glossary", glossObj)
        val qArr = JSONArray()
        p.questions.forEach { q ->
          val qObj = JSONObject().apply {
            put("question", q.question)
            put("options", JSONArray(q.options))
            put("correctIndex", q.correctIndex)
            put("explanation", q.explanation)
          }
          qArr.put(qObj)
        }
        put("questions", qArr)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_AI_READING_PASSAGES_PREFIX + targetLangCode.lowercase(), arr.toString()).apply()
  }

  fun loadAiReadingPassages(targetLangCode: String): List<ReadingAcademyPassage> {
    val raw = prefs.getString(KEY_AI_READING_PASSAGES_PREFIX + targetLangCode.lowercase(), null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val pArr = obj.optJSONArray("paragraphs")
          val paragraphs = buildList {
            if (pArr != null) {
              for (j in 0 until pArr.length()) add(pArr.optString(j, ""))
            }
          }
          val trArr = obj.optJSONArray("paragraphTranslations")
          val translations = buildList {
            if (trArr != null) {
              for (j in 0 until trArr.length()) add(trArr.optString(j, ""))
            }
          }
          val glossObj = obj.optJSONObject("glossary")
          val glossary = mutableMapOf<String, ReadingWordGloss>()
          if (glossObj != null) {
            val keys = glossObj.keys()
            while (keys.hasNext()) {
              val k = keys.next()
              val gObj = glossObj.optJSONObject(k) ?: continue
              glossary[k] = ReadingWordGloss(
                word = gObj.optString("word", k),
                translation = gObj.optString("translation", ""),
                partOfSpeech = gObj.optString("partOfSpeech", ""),
                definition = gObj.optString("definition", ""),
                phonetic = gObj.optString("phonetic", "")
              )
            }
          }
          val qArr = obj.optJSONArray("questions")
          val questions = buildList {
            if (qArr != null) {
              for (j in 0 until qArr.length()) {
                val qObj = qArr.optJSONObject(j) ?: continue
                val optArr = qObj.optJSONArray("options")
                val options = buildList {
                  if (optArr != null) {
                    for (k in 0 until optArr.length()) add(optArr.optString(k, ""))
                  }
                }
                add(
                  AcademyComprehensionQuestion(
                    question = qObj.optString("question", ""),
                    options = options,
                    correctIndex = qObj.optInt("correctIndex", 0),
                    explanation = qObj.optString("explanation", "")
                  )
                )
              }
            }
          }
          add(
            ReadingAcademyPassage(
              id = obj.optString("id", "ai_read_${System.currentTimeMillis()}"),
              languageCode = obj.optString("languageCode", targetLangCode),
              cefrLevel = obj.optString("cefrLevel", "A1"),
              category = obj.optString("category", "Graded Story"),
              title = obj.optString("title", "AI Reading Passage"),
              paragraphs = paragraphs,
              paragraphTranslations = translations,
              glossary = glossary,
              questions = questions
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun isSemanticallySimilarTitle(titleA: String, titleB: String): Boolean {
    val normA = java.text.Normalizer.normalize(titleA.lowercase(), java.text.Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
      .trim()
    val normB = java.text.Normalizer.normalize(titleB.lowercase(), java.text.Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
      .trim()
    if (normA == normB) return true
    val tokensA = normA.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
    val tokensB = normB.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
    if (tokensA.isEmpty() || tokensB.isEmpty()) return false
    val intersection = tokensA.intersect(tokensB).size
    val union = tokensA.union(tokensB).size
    return union > 0 && (intersection.toFloat() / union.toFloat()) >= 0.70f
  }

  fun appendAiReadingPassage(passage: ReadingAcademyPassage) {
    if (passage.title.isBlank() || passage.paragraphs.isEmpty() || passage.questions.isEmpty()) return
    val existing = loadAiReadingPassages(passage.languageCode).toMutableList()
    val idx = existing.indexOfFirst {
      it.id == passage.id || isSemanticallySimilarTitle(it.title, passage.title)
    }
    if (idx >= 0) {
      existing[idx] = passage
    } else {
      existing.add(0, passage) // Prepend newest first
    }
    saveAiReadingPassages(passage.languageCode, existing.take(50))
  }

  fun saveAiListeningItems(targetLangCode: String, items: List<ListeningAcademyItem>) {
    val arr = JSONArray()
    items.forEach { item ->
      val obj = JSONObject().apply {
        put("id", item.id)
        put("languageCode", item.languageCode)
        put("cefrLevel", item.cefrLevel)
        put("category", item.category)
        put("title", item.title)
        put("regionalAccents", JSONArray(item.regionalAccents))
        val linesArr = JSONArray()
        item.lines.forEach { l ->
          val lObj = JSONObject().apply {
            put("speaker", l.speaker)
            put("targetText", l.targetText)
            put("nativeTranslation", l.nativeTranslation)
            put("keyVocabWord", l.keyVocabWord)
            put("keyVocabMeaning", l.keyVocabMeaning)
          }
          linesArr.put(lObj)
        }
        put("lines", linesArr)
        val qArr = JSONArray()
        item.questions.forEach { q ->
          val qObj = JSONObject().apply {
            put("question", q.question)
            put("options", JSONArray(q.options))
            put("correctIndex", q.correctIndex)
            put("explanation", q.explanation)
          }
          qArr.put(qObj)
        }
        put("questions", qArr)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_AI_LISTENING_ITEMS_PREFIX + targetLangCode.lowercase(), arr.toString()).apply()
  }

  fun loadAiListeningItems(targetLangCode: String): List<ListeningAcademyItem> {
    val raw = prefs.getString(KEY_AI_LISTENING_ITEMS_PREFIX + targetLangCode.lowercase(), null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val accArr = obj.optJSONArray("regionalAccents")
          val accents = buildList {
            if (accArr != null) {
              for (j in 0 until accArr.length()) add(accArr.optString(j, ""))
            }
          }
          val linesArr = obj.optJSONArray("lines")
          val lines = buildList {
            if (linesArr != null) {
              for (j in 0 until linesArr.length()) {
                val lObj = linesArr.optJSONObject(j) ?: continue
                add(
                  ListeningDialogueLine(
                    speaker = lObj.optString("speaker", "Speaker"),
                    targetText = lObj.optString("targetText", ""),
                    nativeTranslation = lObj.optString("nativeTranslation", ""),
                    keyVocabWord = lObj.optString("keyVocabWord", ""),
                    keyVocabMeaning = lObj.optString("keyVocabMeaning", "")
                  )
                )
              }
            }
          }
          val qArr = obj.optJSONArray("questions")
          val questions = buildList {
            if (qArr != null) {
              for (j in 0 until qArr.length()) {
                val qObj = qArr.optJSONObject(j) ?: continue
                val optArr = qObj.optJSONArray("options")
                val options = buildList {
                  if (optArr != null) {
                    for (k in 0 until optArr.length()) add(optArr.optString(k, ""))
                  }
                }
                add(
                  AcademyComprehensionQuestion(
                    question = qObj.optString("question", ""),
                    options = options,
                    correctIndex = qObj.optInt("correctIndex", 0),
                    explanation = qObj.optString("explanation", "")
                  )
                )
              }
            }
          }
          add(
            ListeningAcademyItem(
              id = obj.optString("id", "ai_listen_${System.currentTimeMillis()}"),
              languageCode = obj.optString("languageCode", targetLangCode),
              cefrLevel = obj.optString("cefrLevel", "A1"),
              category = obj.optString("category", "Short Conversation"),
              title = obj.optString("title", "AI Listening Dialogue"),
              regionalAccents = accents.ifEmpty { listOf("Standard") },
              lines = lines,
              questions = questions
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun appendAiListeningItem(item: ListeningAcademyItem) {
    if (item.title.isBlank() || item.lines.isEmpty() || item.lines.any { it.targetText.isBlank() } || item.questions.isEmpty()) return
    val existing = loadAiListeningItems(item.languageCode).toMutableList()
    val idx = existing.indexOfFirst {
      it.id == item.id || isSemanticallySimilarTitle(it.title, item.title)
    }
    if (idx >= 0) {
      existing[idx] = item
    } else {
      existing.add(0, item)
    }
    saveAiListeningItems(item.languageCode, existing.take(50))
  }

  fun saveAiDailyItems(targetLangCode: String, items: List<DailyLearningCardItem>) {
    val arr = JSONArray()
    items.forEach { item ->
      val obj = JSONObject().apply {
        put("id", item.id)
        put("languageCode", item.languageCode)
        put("phraseOrIdiom", item.phraseOrIdiom)
        put("translation", item.translation)
        put("explanation", item.explanation)
        put("phoneticGuide", item.phoneticGuide)
        put("category", item.category)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_AI_DAILY_ITEMS_PREFIX + targetLangCode.lowercase(), arr.toString()).apply()
  }

  fun loadAiDailyItems(targetLangCode: String): List<DailyLearningCardItem> {
    val raw = prefs.getString(KEY_AI_DAILY_ITEMS_PREFIX + targetLangCode.lowercase(), null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            DailyLearningCardItem(
              id = obj.optString("id", "ai_daily_${System.currentTimeMillis()}"),
              languageCode = obj.optString("languageCode", targetLangCode),
              phraseOrIdiom = obj.optString("phraseOrIdiom", ""),
              translation = obj.optString("translation", ""),
              explanation = obj.optString("explanation", ""),
              phoneticGuide = obj.optString("phoneticGuide", ""),
              category = obj.optString("category", "Phrase of the Day")
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun appendAiDailyItem(item: DailyLearningCardItem) {
    if (item.phraseOrIdiom.isBlank() || item.translation.isBlank()) return
    val existing = loadAiDailyItems(item.languageCode).toMutableList()
    val idx = existing.indexOfFirst {
      it.id == item.id || isSemanticallySimilarTitle(it.phraseOrIdiom, item.phraseOrIdiom)
    }
    if (idx >= 0) {
      existing[idx] = item
    } else {
      existing.add(0, item)
    }
    saveAiDailyItems(item.languageCode, existing.take(30))
  }

  fun saveAiCustomLessons(targetLangCode: String, lessons: List<CurriculumLesson>) {
    val arr = JSONArray()
    lessons.forEach { l ->
      val obj = JSONObject().apply {
        put("id", l.id)
        put("title", l.title)
        put("description", l.description)
        put("category", l.category)
        put("proficiencyLevel", l.proficiencyLevel)
        put("targetLanguageCode", l.targetLanguageCode)
        put("estimatedMinutes", l.estimatedMinutes)
        put("xpReward", l.xpReward)
        put("cefrLevel", l.cefrLevel)
        put("unitTitle", l.unitTitle)
        put("learningOutcomes", JSONArray(l.learningOutcomes))
        put("isRevision", l.isRevision)
        val exArr = JSONArray()
        l.exercises.forEach { ex ->
          val exObj = JSONObject().apply {
            put("id", ex.id)
            put("type", ex.type.name)
            put("prompt", ex.prompt)
            put("explanationNative", ex.explanationNative)
            put("targetAudioText", ex.targetAudioText)
            put("phoneticGuide", ex.phoneticGuide)
            put("options", JSONArray(ex.options))
            put("correctOptionIndex", ex.correctOptionIndex)
            put("acceptableAnswers", JSONArray(ex.acceptableAnswers))
          }
          exArr.put(exObj)
        }
        put("exercises", exArr)
      }
      arr.put(obj)
    }
    prefs.edit().putString(KEY_AI_CUSTOM_LESSONS_PREFIX + targetLangCode.lowercase(), arr.toString()).apply()
  }

  fun loadAiCustomLessons(targetLangCode: String): List<CurriculumLesson> {
    val raw = prefs.getString(KEY_AI_CUSTOM_LESSONS_PREFIX + targetLangCode.lowercase(), null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val outcomesArr = obj.optJSONArray("learningOutcomes")
          val outcomes = buildList {
            if (outcomesArr != null) {
              for (j in 0 until outcomesArr.length()) add(outcomesArr.optString(j, ""))
            }
          }
          val exArr = obj.optJSONArray("exercises")
          val exercises = buildList {
            if (exArr != null) {
              for (j in 0 until exArr.length()) {
                val exObj = exArr.optJSONObject(j) ?: continue
                val optArr = exObj.optJSONArray("options")
                val options = buildList {
                  if (optArr != null) {
                    for (k in 0 until optArr.length()) add(optArr.optString(k, ""))
                  }
                }
                val ansArr = exObj.optJSONArray("acceptableAnswers")
                val acceptable = buildList {
                  if (ansArr != null) {
                    for (k in 0 until ansArr.length()) add(ansArr.optString(k, ""))
                  }
                }
                val typeStr = exObj.optString("type", ExerciseType.VOCABULARY.name)
                val exType = runCatching { ExerciseType.valueOf(typeStr) }.getOrDefault(ExerciseType.VOCABULARY)
                add(
                  Exercise(
                    id = exObj.optString("id", "ex_${System.currentTimeMillis()}_$j"),
                    type = exType,
                    prompt = exObj.optString("prompt", ""),
                    explanationNative = exObj.optString("explanationNative", ""),
                    targetAudioText = exObj.optString("targetAudioText", ""),
                    phoneticGuide = exObj.optString("phoneticGuide", ""),
                    options = options,
                    correctOptionIndex = exObj.optInt("correctOptionIndex", 0),
                    acceptableAnswers = acceptable
                  )
                )
              }
            }
          }
          add(
            CurriculumLesson(
              id = obj.optString("id", "ai_lesson_${System.currentTimeMillis()}"),
              title = obj.optString("title", "AI Lesson"),
              description = obj.optString("description", ""),
              category = obj.optString("category", "Vocabulary"),
              proficiencyLevel = obj.optString("proficiencyLevel", "Beginner"),
              targetLanguageCode = obj.optString("targetLanguageCode", targetLangCode),
              estimatedMinutes = obj.optInt("estimatedMinutes", 5),
              xpReward = obj.optInt("xpReward", 30),
              exercises = exercises,
              cefrLevel = obj.optString("cefrLevel", "A1"),
              unitTitle = obj.optString("unitTitle", "AI Custom Lessons"),
              learningOutcomes = outcomes,
              isRevision = obj.optBoolean("isRevision", false)
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun appendAiCustomLesson(lesson: CurriculumLesson) {
    if (lesson.title.isBlank() || lesson.exercises.isEmpty()) return
    val existing = loadAiCustomLessons(lesson.targetLanguageCode).toMutableList()
    val idx = existing.indexOfFirst {
      it.id == lesson.id || isSemanticallySimilarTitle(it.title, lesson.title)
    }
    if (idx >= 0) {
      existing[idx] = lesson
    } else {
      existing.add(0, lesson)
    }
    saveAiCustomLessons(lesson.targetLanguageCode, existing.take(50))
  }

  fun saveContentRefreshDate(targetLangCode: String, dateStr: String) {
    prefs.edit().putString(KEY_AI_LAST_REFRESH_PREFIX + targetLangCode.lowercase(), dateStr).apply()
  }

  fun loadContentRefreshDate(targetLangCode: String): String? {
    return prefs.getString(KEY_AI_LAST_REFRESH_PREFIX + targetLangCode.lowercase(), null)
  }

  // ===========================================================================
  // PER-LANGUAGE PROGRESS ISOLATION SNAPSHOTS
  // ===========================================================================

  fun saveLanguageProgressSnapshot(snapshot: com.example.data.model.LanguageLearningProgressSnapshot) {
    val cleanCode = snapshot.languageCode.trim().lowercase().substringBefore("-")
    if (cleanCode.isBlank()) return
    val all = loadAllLanguageProgressSnapshots().toMutableMap()
    all[cleanCode] = snapshot.copy(languageCode = cleanCode, updatedAtEpochMs = System.currentTimeMillis())
    val root = JSONObject()
    all.forEach { (code, snap) ->
      val completedArr = JSONArray()
      snap.completedLessonIds.distinct().forEach { completedArr.put(it) }
      root.put(
        code,
        JSONObject().apply {
          put("languageCode", code)
          put("proficiencyLevel", snap.proficiencyLevel)
          put("cefrLevel", snap.cefrLevel)
          put("xpPoints", snap.xpPoints)
          put("completedLessonIds", completedArr)
          put("currentLessonId", snap.currentLessonId ?: "")
          put("streakDays", snap.streakDays)
          put("updatedAtEpochMs", snap.updatedAtEpochMs)
        }
      )
    }
    prefs.edit().putString(KEY_LANGUAGE_PROGRESS_SNAPSHOTS, root.toString()).apply()
  }

  fun loadLanguageProgressSnapshot(languageCode: String): com.example.data.model.LanguageLearningProgressSnapshot? {
    val cleanCode = languageCode.trim().lowercase().substringBefore("-")
    if (cleanCode.isBlank()) return null
    return loadAllLanguageProgressSnapshots()[cleanCode]
  }

  fun loadAllLanguageProgressSnapshots(): Map<String, com.example.data.model.LanguageLearningProgressSnapshot> {
    val raw = prefs.getString(KEY_LANGUAGE_PROGRESS_SNAPSHOTS, null) ?: return emptyMap()
    return try {
      val root = JSONObject(raw)
      val map = mutableMapOf<String, com.example.data.model.LanguageLearningProgressSnapshot>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val code = keys.next()
        val obj = root.optJSONObject(code) ?: continue
        val arr = obj.optJSONArray("completedLessonIds")
        val completed = buildList {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val id = arr.optString(i)
              if (id.isNotBlank()) add(id)
            }
          }
        }
        map[code] = com.example.data.model.LanguageLearningProgressSnapshot(
          languageCode = obj.optString("languageCode", code),
          proficiencyLevel = obj.optString("proficiencyLevel", "Beginner"),
          cefrLevel = obj.optString("cefrLevel", ""),
          xpPoints = obj.optLong("xpPoints", 0L),
          completedLessonIds = completed,
          currentLessonId = obj.optString("currentLessonId", "").takeIf { it.isNotBlank() },
          streakDays = obj.optLong("streakDays", 1L),
          updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
        )
      }
      map
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun todayIsoString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }

  fun saveLearningPlanSetup(goal: String, confidence: String, completed: Boolean = true) {
    prefs.edit()
      .putString(KEY_LEARNING_PLAN_GOAL, goal.ifBlank { "Everyday Communication" })
      .putString(KEY_LEARNING_PLAN_CONFIDENCE, confidence.ifBlank { "Medium" })
      .putBoolean(KEY_LEARNING_PLAN_COMPLETED, completed)
      .apply()
  }

  fun loadLearningPlanSetup(): LearningPlanSetupPrefs {
    return LearningPlanSetupPrefs(
      goal = prefs.getString(KEY_LEARNING_PLAN_GOAL, "Everyday Communication") ?: "Everyday Communication",
      confidence = prefs.getString(KEY_LEARNING_PLAN_CONFIDENCE, "Medium") ?: "Medium",
      completed = prefs.getBoolean(KEY_LEARNING_PLAN_COMPLETED, false)
    )
  }

  companion object {
    private const val PREFS_NAME = "lingosphere_learning_memory_and_offline"
    private const val KEY_LEARNING_PLAN_GOAL = "learning_plan_primary_goal"
    private const val KEY_LEARNING_PLAN_CONFIDENCE = "learning_plan_confidence"
    private const val KEY_LEARNING_PLAN_COMPLETED = "learning_plan_setup_completed"
    private const val KEY_MISTAKE_NOTEBOOK = "mistake_notebook_json"
    private const val KEY_PRONUNCIATION_HISTORY = "pronunciation_history_json"
    private const val KEY_SRS_METADATA = "srs_metadata_json"
    private const val KEY_BOOKMARKED_READING_IDS = "bookmarked_reading_ids"
    private const val KEY_COMPLETED_ACADEMY_IDS = "completed_academy_ids"
    private const val KEY_PLACEMENT_EVAL = "placement_eval_json"
    private const val KEY_IN_PROGRESS_LESSONS = "in_progress_lessons_json"
    private const val KEY_DOWNLOADED_LESSON_IDS = "downloaded_lesson_ids"
    private const val KEY_OFFLINE_FLASHCARDS = "offline_flashcards_json"
    private const val KEY_OFFLINE_QUEUE = "offline_mutation_queue_json"
    private const val KEY_DAILY_MINUTES = "daily_learning_minutes_json"
    private const val KEY_SUBSCRIPTION_STATE = "subscription_state_json"
    private const val KEY_CUSTOM_PRO_DECKS = "custom_pro_decks_json"
    private const val KEY_PROFILE_AVATAR_PREFIX = "profile_avatar_data_uri_"
    private const val KEY_ACCOUNT_SETTINGS_PREFIX = "account_settings_json_"
    private const val KEY_CACHED_PROFILE_PREFIX = "cached_profile_metadata_"
    private const val KEY_LEARNING_GOALS = "lingo3_learning_goals_json"
    private const val KEY_CONVERSATION_SUMMARIES = "lingo3_conv_summaries_json"
    private const val KEY_REPEATED_MISTAKES = "lingo3_repeated_mistakes_json"
    private const val KEY_CONVERSATION_REPLAYS = "lingo3_conversation_replays_json"
    private const val KEY_COMPLETED_MISSIONS = "lingo3_completed_missions_json"
    private const val KEY_PORTFOLIO_SPEAKING = "lingo3_portfolio_speaking_json"
    private const val KEY_PORTFOLIO_WRITING = "lingo3_portfolio_writing_json"
    private const val KEY_PORTFOLIO_PRIVACY = "lingo3_portfolio_privacy_json"
    private const val KEY_MEDIA_PRIVACY_CONSENT = "lingo_practical_media_privacy_consent_json"
    private const val KEY_LIVING_WORLD_PROGRESS = "lingo_practical_living_world_progress_json"
    private const val KEY_OFFLINE_EMERGENCY_LANGS = "lingo_practical_offline_emergency_langs"
    private const val KEY_HABIT_ROUTINE_IDS = "lingo_practical_habit_routine_ids"
    private const val KEY_VERIFIED_HABIT_COMPLETIONS = "lingo_practical_verified_habit_completions_json"
    private const val KEY_COMPLETED_REPAIR_SCENARIOS = "lingo_practical_completed_repair_scenarios"
    private const val KEY_FUTURE_ME_COMPARISONS = "lingo_practical_future_me_comparisons_json"
    private const val KEY_AI_READING_PASSAGES_PREFIX = "ai_reading_passages_json_"
    private const val KEY_AI_LISTENING_ITEMS_PREFIX = "ai_listening_items_json_"
    private const val KEY_AI_DAILY_ITEMS_PREFIX = "ai_daily_items_json_"
    private const val KEY_AI_CUSTOM_LESSONS_PREFIX = "ai_custom_lessons_json_"
    private const val KEY_AI_LAST_REFRESH_PREFIX = "ai_last_refresh_date_"
    private const val KEY_LANGUAGE_PROGRESS_SNAPSHOTS = "language_progress_snapshots_json"
  }
}

data class CachedProfileMetadata(
  val displayName: String,
  val country: String,
  val countryFlag: String,
  val bio: String,
  val interests: List<String>,
  val nativeLanguageCode: String? = null,
  val targetLanguageCode: String? = null,
  val proficiencyLevel: String? = null,
  val dailyGoalMinutes: Long? = null,
  val languageLevels: Map<String, String> = emptyMap()
)

data class PremiumGeneratedVocabCard(
  val termTarget: String,
  val translationNative: String,
  val phoneticGuide: String,
  val exampleSentenceTarget: String,
  val exampleSentenceNative: String
)

data class PremiumCustomDeck(
  val id: String,
  val topicTitle: String,
  val languageCode: String,
  val cefrLevel: String,
  val cards: List<PremiumGeneratedVocabCard>,
  val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class PremiumCoachingPlanResult(
  val headlineSummary: String,
  val estimatedCefrVelocity: String,
  val dailySchedule: List<Pair<String, String>>,
  val focusGrammarRule: String,
  val recommendedMissionTitle: String
)

data class LearningPlanSetupPrefs(
  val goal: String = "Everyday Communication",
  val confidence: String = "Medium",
  val completed: Boolean = false
)


