package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Curriculum specification for a specific `(game, levelNumber)` in the 1..500 progression.
 */
data class LevelCurriculumSpec(
  val levelNumber: Int,
  val band: LevelProgressionBand,
  val objectiveTitle: String,
  val grammarOrLexicalFocus: String,
  val scenarioContext: String,
  val targetWordCountRange: IntRange,
  val difficultyIndex: Int
)

/**
 * Stored level content record for anti-repetition and auditability.
 */
data class LevelContentHistoryRecord(
  val userId: String,
  val languageCode: String,
  val gameId: String,
  val levelNumber: Int,
  val objective: String,
  val topic: String,
  val vocabularyUsed: List<String>,
  val questionIds: List<String>,
  val contentHashes: List<String>,
  val difficulty: String,
  val completionStatus: Boolean
)

/**
 * Curated multilingual vocabulary/sentence entry organized by thematic domain.
 */
data class ThematicLexiconItem(
  val domainIndex: Int,
  val domainLabel: String,
  val enWord: String,
  val enSentence: String,
  val enFlawedSentence: String,
  val enErrorWord: String,
  val enCorrectedWord: String,
  val translations: Map<String, Pair<String, String>>, // langCode -> (word, sentence)
  val phonetics: Map<String, String>,
  val illustrationKind: VisualIllustrationKind,
  val visualConceptId: String = ""
)

/**
 * Developer-only diagnostics logger. Never surfaces internal logs in user UI.
 */
object GameEngineDiagnostics {
  private const val TAG = "GameEngineDiag"

  fun log(event: String, details: String) {
    if (BuildConfig.DEBUG) {
      try {
        Log.d(TAG, "[$event] $details")
      } catch (_: Throwable) {
        // Safe no-op in plain JVM tests where android.util.Log may not be mocked
      }
    }
  }
}

/**
 * Provides distinct per-level curriculum objectives (Levels 1..500), global anti-repetition
 * tracking across levels and games, deterministic Word Search grid construction, and a rich
 * thematic multilingual bank so consecutive levels never repeat exercises.
 */
object GameCurriculumAndAntiRepetitionEngine {

  private const val HISTORY_PREFS = "lingosphere_game_anti_repetition_prefs"

  private val PICTURE_QUIZ_BAND_1_TOPICS = listOf(
    "Everyday household objects",
    "Basic clothing & footwear",
    "Simple food & drinks",
    "Animals & nature basics",
    "Kitchen & dining basics",
    "Daily actions & routines",
    "Around the home & rooms",
    "Weather & sky",
    "Everyday personal items",
    "Simple city transport"
  )

  private val PICTURE_QUIZ_BAND_2_TOPICS = listOf(
    "Food, bakery & fresh produce",
    "Travel, luggage & transport",
    "Family life & home meals",
    "Café ordering & beverages",
    "Seasons, parks & outdoor walks",
    "Airport & train journeys",
    "Hotel stays & daily errands",
    "Hobbies & weekend activities",
    "Market shopping & ingredients",
    "Daily routines & schedules"
  )

  private val PICTURE_QUIZ_BAND_3_TOPICS = listOf(
    "Workplace & office essentials",
    "Shopping, receipts & payment",
    "Education, books & campus",
    "Technology & digital devices",
    "Restaurants & conversations",
    "Fashion, tailoring & accessories",
    "Career tools & meeting rooms",
    "Music, cinema & arts",
    "Transit stations & city errands",
    "Sports & active hobbies"
  )

  private val PICTURE_QUIZ_BAND_4_TOPICS = listOf(
    "Health, clinics & pharmacy",
    "Landscapes, mountains & weather",
    "Travel navigation & landmarks",
    "Museums, theater & culture",
    "Outdoor adventure & camping",
    "Tools, instruments & equipment",
    "Urban architecture & bridges",
    "Media, journalism & audio",
    "Festivals & social situations",
    "Intermediate science & discovery"
  )

  private val PICTURE_QUIZ_BAND_5_TOPICS = listOf(
    "Architecture, cathedrals & civic halls",
    "Scientific laboratories & observatories",
    "Renewable energy & environment",
    "Aviation, maritime & space exploration",
    "Classical orchestra & fine arts",
    "Precision engineering & optics",
    "Geography, glaciers & archipelagos",
    "Historical artifacts & heritage",
    "Gastronomy, vineyards & botanicals",
    "Global diplomacy & civic institutions"
  )

  private val PICTURE_QUIZ_TOPICS = PICTURE_QUIZ_BAND_1_TOPICS + PICTURE_QUIZ_BAND_2_TOPICS

  private val SENTENCE_BUILDER_TOPICS = listOf(
    "Basic greetings & polite expressions",
    "Subject + verb core statements",
    "Simple present everyday habits",
    "Negative sentences & polite refusals",
    "Basic questions (who, what, where)",
    "Daily routine & time expressions",
    "Present continuous ongoing actions",
    "Past events & yesterday's activities",
    "Future plans & intentions",
    "Practical real-world conversations",
    "Requests & ordering politely",
    "Comparisons & preferences",
    "Giving directions & locations",
    "Expressing reasons with connectors",
    "Describing experiences & travel",
    "Making invitations & arrangements",
    "Conditional situations (if / when)",
    "Workplace updates & teamwork",
    "Problem solving & polite complaints",
    "Expressing nuanced opinions"
  )

  private val SPELLBEE_TOPICS = listOf(
    "Short everyday words",
    "Common household & personal objects",
    "Basic action verbs",
    "Longer everyday vocabulary",
    "Commonly confused words",
    "Workplace & career vocabulary",
    "Difficult spelling patterns & silent letters",
    "Advanced multi-syllable vocabulary",
    "Food, culinary & dining words",
    "Travel, transit & geography words",
    "Descriptive adjectives & feelings",
    "Health, science & environment terms",
    "Double consonants & vowel pairs",
    "Prefixes, suffixes & compound words",
    "Academic & formal communication words",
    "Idiomatic & cultural expressions"
  )

  private val GENERAL_ROTATING_THEMES = listOf(
    "Greetings & Personal Introductions",
    "Home & Everyday Objects",
    "Clothing, Style & Accessories",
    "Food, Drinks & Café Ordering",
    "Daily Actions & Morning Routines",
    "City Places, Directions & Transit",
    "People, Family & Descriptions",
    "Weather, Seasons & Nature",
    "Airport, Hotel & Travel Check-in",
    "Feelings, Moods & Reactions",
    "Shopping, Prices & Markets",
    "Work, Meetings & Career Skills",
    "Health, Doctor Visits & Wellness",
    "Hobbies, Music & Sports",
    "Technology, Calls & Messages",
    "Time, Schedules & Appointments",
    "Social Plans & Making Friends",
    "Restaurants, Ingredients & Dining",
    "Study, Reading & Campus Life",
    "Culture, Traditions & Etiquette"
  )

  fun tierForLevel(levelNumber: Int): Int = when {
    levelNumber <= 10 -> 1
    levelNumber <= 30 -> 2
    levelNumber <= 60 -> 3
    levelNumber <= 100 -> 4
    else -> 5
  }

  /**
   * Computes a unique, non-repeating curriculum specification for `(game, levelNumber)`.
   * Aligns topics with the 5 progressive difficulty bands (`1–10`, `11–30`, `31–60`, `61–100`, `101–500+`)
   * and guarantees consecutive levels never share the same objective or thematic focus.
   */
  fun getCurriculumSpec(game: UltimateLanguageGame, levelNumber: Int): LevelCurriculumSpec {
    val clamped = levelNumber.coerceIn(1, 500)
    val band = LevelProgressionBand.forLevel(clamped)

    if (game.id == "ug_01_picture_word_quiz" || game.id == "ug_14_picture_description") {
      val (bandTopics, bandStart, bandLabel) = when {
        clamped <= 10 -> Triple(PICTURE_QUIZ_BAND_1_TOPICS, 1, "Easy Everyday")
        clamped <= 30 -> Triple(PICTURE_QUIZ_BAND_2_TOPICS, 11, "Food, Travel, Family & Daily Life")
        clamped <= 60 -> Triple(PICTURE_QUIZ_BAND_3_TOPICS, 31, "Work, Shopping & Education")
        clamped <= 100 -> Triple(PICTURE_QUIZ_BAND_4_TOPICS, 61, "Intermediate Situations")
        else -> Triple(PICTURE_QUIZ_BAND_5_TOPICS, 101, "Advanced Situations")
      }
      val offsetInBand = (clamped - bandStart).coerceAtLeast(0)
      val baseTopic = bandTopics[offsetInBand % bandTopics.size]
      val cycleInBand = offsetInBand / bandTopics.size
      val objectiveTitle = if (cycleInBand == 0) {
        baseTopic
      } else {
        "$baseTopic — Stage ${cycleInBand + 1}"
      }
      return LevelCurriculumSpec(
        levelNumber = clamped,
        band = band,
        objectiveTitle = objectiveTitle,
        grammarOrLexicalFocus = "$bandLabel ($objectiveTitle, ${band.cefrEquivalent})",
        scenarioContext = "${game.title} • Level $clamped: $objectiveTitle",
        targetWordCountRange = if (game.id == "ug_14_picture_description") 4..10 else 1..3,
        difficultyIndex = clamped
      )
    }

    val topicList = when (game.id) {
      "ug_42_sentence_builder", "ug_47_question_builder" -> SENTENCE_BUILDER_TOPICS
      "ug_16_spellbee", "ug_18_spelling_race", "ug_19_fix_the_spelling", "ug_23_spelling_ladder" -> SPELLBEE_TOPICS
      "ug_24_pronunciation_battle" -> SpeakingGamesContentAndEvaluationEngine.PRONUNCIATION_PRACTICE_TOPICS
      "ug_25_repeat_after_me" -> SpeakingGamesContentAndEvaluationEngine.REPEAT_AFTER_ME_TOPICS
      "ug_26_tongue_twister" -> SpeakingGamesContentAndEvaluationEngine.ARTICULATION_SPRINT_TOPICS
      "ug_27_speak_the_picture" -> SpeakingGamesContentAndEvaluationEngine.SPEAK_THE_SCENE_TOPICS
      "ug_28_quick_answer" -> SpeakingGamesContentAndEvaluationEngine.QUICK_ANSWER_TOPICS
      "ug_29_speech_coach_challenge" -> SpeakingGamesContentAndEvaluationEngine.SPEECH_COACH_TOPICS
      "ug_30_fluency_sprint" -> SpeakingGamesContentAndEvaluationEngine.FLUENCY_SPRINT_TOPICS
      "ug_31_shadow_speaking" -> SpeakingGamesContentAndEvaluationEngine.SHADOW_SPEAKING_TOPICS
      "ug_32_dialogue_completion" -> SpeakingGamesContentAndEvaluationEngine.DIALOGUE_LINE_TOPICS
      "ug_33_roleplay_mission" -> SpeakingGamesContentAndEvaluationEngine.ROLEPLAY_10_SCENARIOS.map { "${it.title}: ${it.partnerRole}" }
      else -> GENERAL_ROTATING_THEMES
    }

    val indexOffset = when (game.id) {
      "ug_42_sentence_builder", "ug_16_spellbee",
      "ug_24_pronunciation_battle", "ug_25_repeat_after_me", "ug_26_tongue_twister",
      "ug_27_speak_the_picture", "ug_28_quick_answer", "ug_29_speech_coach_challenge",
      "ug_30_fluency_sprint", "ug_31_shadow_speaking", "ug_32_dialogue_completion",
      "ug_33_roleplay_mission" -> 0
      else -> (game.gameNumber * 3)
    }
    val rawTopic = topicList[(clamped - 1 + indexOffset) % topicList.size]
    val cycleTier = (clamped - 1) / topicList.size

    val tierQualifier = when (cycleTier) {
      0 -> ""
      1 -> " — Practical Contexts"
      2 -> " — Expanded Fluency"
      3 -> " — Nuanced Usage"
      else -> " — Mastery Tier ${cycleTier + 1}"
    }

    val wordCountRange = when {
      clamped <= 10 -> 1..3
      clamped <= 30 -> 2..5
      clamped <= 60 -> 3..7
      clamped <= 100 -> 4..9
      clamped <= 250 -> 5..11
      clamped <= 400 -> 7..14
      else -> 8..16
    }

    return LevelCurriculumSpec(
      levelNumber = clamped,
      band = band,
      objectiveTitle = "$rawTopic$tierQualifier",
      grammarOrLexicalFocus = "${band.bandName} (${band.cefrEquivalent}): $rawTopic — ${band.complexityHint}",
      scenarioContext = "${game.title} • Level $clamped: $rawTopic",
      targetWordCountRange = wordCountRange,
      difficultyIndex = clamped
    )
  }

  /**
   * Computes a stable SHA-256 short content hash for anti-duplication tracking.
   */
  fun computeContentHash(languageCode: String, mechanic: GameInteractionMechanic, primaryText: String): String {
    val normalized = "${languageCode.lowercase()}|${mechanic.name}|${primaryText.trim().lowercase().replace(Regex("\\s+"), " ")}"
    return try {
      val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
      digest.take(8).joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
      normalized.hashCode().toUInt().toString(16)
    }
  }

  private fun userScopeKey(userId: String, languageCode: String): String {
    val safeUser = userId.ifBlank { "default_learner" }.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
    return "${safeUser}_${languageCode.lowercase()}"
  }

  /**
   * Returns the progressive vocabulary band description for dynamic AI generation:
   * - Levels 1–10: Easy everyday vocabulary
   * - Levels 11–30: Food, travel, family, and daily activities
   * - Levels 31–60: Work, shopping, education, and conversations
   * - Levels 61–100: Intermediate vocabulary and situations
   * - Levels 101–500+: Progressively advanced vocabulary and real-world situations
   */
  fun getVocabularyCurriculumBandDescription(levelNumber: Int): String = when {
    levelNumber <= 10 -> "Levels 1–10: Easy everyday vocabulary"
    levelNumber <= 30 -> "Levels 11–30: Food, travel, family, and daily activities"
    levelNumber <= 60 -> "Levels 31–60: Work, shopping, education, and conversations"
    levelNumber <= 100 -> "Levels 61–100: Intermediate vocabulary and situations"
    else -> "Levels 101–500+: Progressively advanced vocabulary and real-world situations"
  }

  private val LEADING_ARTICLES = setOf(
    "a", "an", "the", "el", "la", "los", "las", "un", "una", "unos", "unas",
    "le", "les", "du", "des", "der", "die", "das", "ein", "eine"
  )

  private val SYNONYM_CANONICAL_MAP: Map<String, String> = mapOf(
    "sofa" to "sofa", "couch" to "sofa", "settee" to "sofa", "canape" to "sofa",
    "car" to "car", "automobile" to "car", "sedan" to "car", "auto" to "car", "coche" to "car", "voiture" to "car",
    "phone" to "phone", "smartphone" to "phone", "telephone" to "phone", "cellphone" to "phone", "mobile" to "phone",
    "bread" to "bread", "loaf" to "bread", "baguette" to "bread",
    "bicycle" to "bicycle", "bike" to "bicycle", "cycle" to "bicycle",
    "airplane" to "airplane", "plane" to "airplane", "aeroplane" to "airplane", "aircraft" to "airplane", "jet" to "airplane",
    "suitcase" to "suitcase", "luggage" to "suitcase", "baggage" to "suitcase", "valise" to "suitcase",
    "jacket" to "jacket", "coat" to "jacket",
    "sweater" to "sweater", "pullover" to "sweater", "jumper" to "sweater",
    "sneakers" to "sneakers", "trainers" to "sneakers", "running shoe" to "sneakers",
    "backpack" to "backpack", "rucksack" to "backpack", "knapsack" to "backpack",
    "tshirt" to "tshirt", "t shirt" to "tshirt", "tee" to "tshirt",
    "sunglasses" to "sunglasses", "shades" to "sunglasses",
    "eyeglasses" to "eyeglasses", "spectacles" to "eyeglasses",
    "cup" to "cup", "mug" to "cup", "teacup" to "cup",
    "ship" to "ship", "boat" to "ship", "ferry" to "ship", "vessel" to "ship",
    "laptop" to "laptop", "notebook computer" to "laptop",
    "wallet" to "wallet", "billfold" to "wallet", "pocketbook" to "wallet",
    "watch" to "watch", "wristwatch" to "watch",
    "clock" to "clock", "wall clock" to "clock", "timepiece" to "clock",
    "store" to "store", "shop" to "store", "market" to "store",
    "doctor" to "doctor", "physician" to "doctor",
    "movie" to "cinema", "film" to "cinema", "cinema" to "cinema",
    "forest" to "forest", "woods" to "forest", "woodland" to "forest",
    "ocean" to "ocean", "sea" to "ocean",
    "street" to "street", "road" to "street", "avenue" to "street",
    "house" to "house", "home" to "house",
    "gift" to "gift", "present" to "gift",
    "garbage" to "trash", "trash" to "trash", "rubbish" to "trash",
    "photo" to "camera", "photograph" to "photograph", "picture" to "photograph",
    "table" to "table", "dining table" to "table",
    "sun" to "sun", "sunshine" to "sun", "sunlight" to "sun"
  )

  private val PROMPT_STOPWORDS = setOf(
    "what", "which", "who", "where", "how", "is", "are", "was", "were", "do", "does", "did",
    "the", "a", "an", "in", "on", "at", "for", "of", "to", "with", "by", "from", "you", "your",
    "object", "item", "thing", "word", "picture", "image", "shown", "shows", "used", "use",
    "select", "choose", "identify", "correctly", "accurately", "usually", "often", "can"
  )

  /**
   * Produces a canonical normalized meaning key for a word/phrase so exact words, plurals,
   * leading articles, accented variants, and synonyms map to the same key.
   */
  fun canonicalConceptKey(raw: String): String {
    val stripped = java.text.Normalizer.normalize(raw.trim().lowercase(), java.text.Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() && it !in LEADING_ARTICLES }
      .joinToString(" ")
    if (stripped.isBlank()) return ""
    SYNONYM_CANONICAL_MAP[stripped]?.let { return it }
    val singular = when {
      stripped.endsWith("ies") && stripped.length > 4 -> stripped.dropLast(3) + "y"
      stripped.endsWith("ches") || stripped.endsWith("shes") || stripped.endsWith("xes") || stripped.endsWith("sses") -> stripped.dropLast(2)
      stripped.endsWith("s") && stripped.length > 3 && !stripped.endsWith("ss") && !stripped.endsWith("glasses") && !stripped.endsWith("gloves") && !stripped.endsWith("sneakers") && !stripped.endsWith("boots") -> stripped.dropLast(1)
      else -> stripped
    }
    return SYNONYM_CANONICAL_MAP[singular] ?: singular
  }

  /**
   * Computes a normalized semantic signature for a question prompt so paraphrased questions
   * with the same meaning (e.g., "What object provides light?" vs "Which item gives light in a room?")
   * are detected as duplicates.
   */
  fun promptMeaningSignature(prompt: String): String {
    val cleanTokens = java.text.Normalizer.normalize(prompt.trim().lowercase(), java.text.Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace(Regex("\\([^)]*\\)"), " ") // ignore "(Spanish)" language tag
      .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
      .split(Regex("\\s+"))
      .filter { it.length > 2 && it !in PROMPT_STOPWORDS }
      .map { tok ->
        when {
          tok.endsWith("ing") && tok.length > 5 -> tok.dropLast(3)
          tok.endsWith("ed") && tok.length > 4 -> tok.dropLast(2)
          tok.endsWith("s") && tok.length > 3 && !tok.endsWith("ss") -> tok.dropLast(1)
          else -> tok
        }
      }
      .distinct()
      .sorted()
    return if (cleanTokens.isEmpty()) "" else "qsig:${cleanTokens.joinToString("_")}"
  }

  /**
   * Expands a raw exclusion set with canonical synonym keys so semantic collisions are blocked automatically.
   */
  fun expandExcludedWithSemanticKeys(rawExcluded: Set<String>): Set<String> {
    val expanded = LinkedHashSet<String>(rawExcluded.size * 2)
    rawExcluded.forEach { item ->
      val lower = item.trim().lowercase()
      if (lower.isNotBlank()) {
        expanded.add(lower)
        val canon = canonicalConceptKey(lower)
        if (canon.isNotBlank()) expanded.add("sem:$canon")
      }
    }
    return expanded
  }

  /**
   * Checks whether candidate content collides with anything in `excludedSet` (exact text, concept ID,
   * canonical synonym meaning, or question meaning signature).
   */
  fun isDuplicateContent(
    targetPhrase: String,
    englishMeaning: String,
    conceptId: String,
    prompt: String,
    excludedSet: Set<String>
  ): Boolean {
    val phraseLower = targetPhrase.trim().lowercase()
    val engLower = englishMeaning.trim().lowercase()
    val cidLower = conceptId.trim().lowercase()
    val pLower = prompt.trim().lowercase()
    if (phraseLower in excludedSet) return true
    if (cidLower.isNotBlank() && (cidLower in excludedSet || "cid:$cidLower" in excludedSet)) return true
    if (engLower.isNotBlank() && engLower in excludedSet) return true
    if (pLower.isNotBlank() && pLower in excludedSet) return true

    val phraseCanon = canonicalConceptKey(phraseLower)
    if (phraseCanon.isNotBlank() && (phraseCanon in excludedSet || "sem:$phraseCanon" in excludedSet)) return true

    val engCanon = canonicalConceptKey(engLower)
    if (engCanon.isNotBlank() && (engCanon in excludedSet || "sem:$engCanon" in excludedSet)) return true

    val cidCanon = canonicalConceptKey(cidLower)
    if (cidCanon.isNotBlank() && (cidCanon in excludedSet || "sem:$cidCanon" in excludedSet)) return true

    val pSig = promptMeaningSignature(pLower)
    if (pSig.isNotBlank() && pSig in excludedSet) return true
    return false
  }

  /**
   * Returns vocabulary, visual concept IDs, canonical semantic keys, questions, and prompt signatures
   * previously used across ALL recorded levels of this game (Level 1..500+) AND recent cross-game sessions.
   */
  fun getExcludedContentForLevel(
    context: Context,
    userId: String = "default_learner",
    languageCode: String,
    gameId: String,
    levelNumber: Int
  ): Set<String> {
    val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
    val scope = userScopeKey(userId, languageCode)
    val excluded = LinkedHashSet<String>()

    val recordedLevelsCsv = prefs.getString("recorded_lvls_${scope}_$gameId", "") ?: ""
    val recordedLevels = recordedLevelsCsv
      .split(",")
      .mapNotNull { it.trim().toIntOrNull() }
      .toMutableSet()

    // Scan ALL recorded levels for this game + user + language (persisted across restarts!)
    for (prevLvl in recordedLevels.sorted()) {
      if (prevLvl == levelNumber) continue
      val recJson = prefs.getString("rec_${scope}_${gameId}_$prevLvl", null) ?: continue
      try {
        val obj = JSONObject(recJson)
        listOf("vocabularyUsed", "conceptsUsed", "meaningsUsed", "promptsUsed", "semanticKeysUsed", "promptSignaturesUsed").forEach { field ->
          val arr = obj.optJSONArray(field)
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val w = arr.optString(i).trim().lowercase()
              if (w.isNotBlank()) {
                excluded.add(w)
                if (field == "vocabularyUsed" || field == "conceptsUsed" || field == "meaningsUsed") {
                  val canon = canonicalConceptKey(w)
                  if (canon.isNotBlank()) excluded.add("sem:$canon")
                }
                if (field == "promptsUsed") {
                  val pSig = promptMeaningSignature(w)
                  if (pSig.isNotBlank()) excluded.add(pSig)
                }
              }
            }
          }
        }
      } catch (_: Exception) {}
    }

    // Also include recent cross-game vocabulary so unrelated games don't echo the exact same items
    val globalRaw = prefs.getString("global_recent_$scope", null)
    if (!globalRaw.isNullOrBlank()) {
      try {
        val arr = JSONArray(globalRaw)
        for (i in 0 until arr.length().coerceAtMost(80)) {
          val w = arr.optString(i).trim().lowercase()
          if (w.isNotBlank()) {
            excluded.add(w)
            val canon = canonicalConceptKey(w)
            if (canon.isNotBlank()) excluded.add("sem:$canon")
          }
        }
      } catch (_: Exception) {}
    }

    return excluded
  }

  /**
   * Records a generated or completed level's content history in persistent storage for each user.
   * Tracks vocabulary, visualConceptIds, canonical semantic keys, prompts, and prompt signatures
   * so questions never repeat across levels or restarts.
   */
  fun recordLevelHistory(
    context: Context,
    userId: String = "default_learner",
    languageCode: String,
    gameId: String,
    levelNumber: Int,
    spec: LevelCurriculumSpec,
    exercises: List<DynamicLevelExercise>,
    completed: Boolean
  ) {
    try {
      val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
      val scope = userScopeKey(userId, languageCode)
      val vocab = exercises.flatMap { ex ->
        if (ex.mechanic == GameInteractionMechanic.WORD_SEARCH_GRID && ex.wordSearchTargets.isNotEmpty()) {
          ex.wordSearchTargets.map { it.first.trim() }
        } else if (ex.mechanic == GameInteractionMechanic.WORD_SEARCH_GRID) {
          ex.targetPhrase.split(",").map { it.trim() }
        } else {
          listOf(ex.targetPhrase.trim())
        }
      }.filter { it.isNotBlank() }.distinct()
      val wsOnlyWords = exercises
        .filter { it.mechanic == GameInteractionMechanic.WORD_SEARCH_GRID }
        .flatMap { ex ->
          if (ex.wordSearchTargets.isNotEmpty()) ex.wordSearchTargets.map { it.first.trim() }
          else ex.targetPhrase.split(",").map { it.trim() }
        }
        .filter { it.isNotBlank() }
      if (wsOnlyWords.isNotEmpty()) {
        WordSearchDifficultyAndTopicEngine.recordUsedWordSearchWords(
          context = context,
          userId = userId,
          languageCode = languageCode,
          words = wsOnlyWords
        )
      }
      val concepts = exercises.map { it.visualConceptId.trim() }.filter { it.isNotBlank() }.distinct()
      val meanings = exercises.map { it.translation.trim() }.filter { it.isNotBlank() }.distinct()
      val prompts = exercises.map { it.prompt.trim() }.filter { it.isNotBlank() }.distinct()
      val semanticKeys = (vocab + concepts + meanings)
        .map { canonicalConceptKey(it) }
        .filter { it.isNotBlank() }
        .map { "sem:$it" }
        .distinct()
      val promptSigs = prompts.map { promptMeaningSignature(it) }.filter { it.isNotBlank() }.distinct()
      val qIds = exercises.map { it.id }
      val hashes = exercises.map {
        it.contentHash.ifBlank { computeContentHash(languageCode, it.mechanic, it.targetPhrase) }
      }

      val obj = JSONObject().apply {
        put("userId", userId)
        put("languageCode", languageCode)
        put("gameId", gameId)
        put("levelNumber", levelNumber)
        put("objective", spec.objectiveTitle)
        put("topic", spec.grammarOrLexicalFocus)
        put("difficulty", spec.band.cefrEquivalent)
        put("completionStatus", completed)
        put("vocabularyUsed", JSONArray(vocab))
        put("conceptsUsed", JSONArray(concepts))
        put("meaningsUsed", JSONArray(meanings))
        put("promptsUsed", JSONArray(prompts))
        put("semanticKeysUsed", JSONArray(semanticKeys))
        put("promptSignaturesUsed", JSONArray(promptSigs))
        put("questionIds", JSONArray(qIds))
        put("contentHashes", JSONArray(hashes))
      }

      val existingLvls = (prefs.getString("recorded_lvls_${scope}_$gameId", "") ?: "")
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .toMutableSet()
      existingLvls.add(levelNumber)

      val existingGlobal = mutableListOf<String>()
      prefs.getString("global_recent_$scope", null)?.let { raw ->
        runCatching {
          val arr = JSONArray(raw)
          for (i in 0 until arr.length()) {
            val item = arr.optString(i)
            if (item.isNotBlank()) existingGlobal.add(item)
          }
        }
      }
      val mergedGlobal = (vocab.map { it.lowercase() } + existingGlobal).distinct().take(120)

      prefs.edit()
        .putString("rec_${scope}_${gameId}_$levelNumber", obj.toString())
        .putString("recorded_lvls_${scope}_$gameId", existingLvls.sorted().joinToString(","))
        .putString("global_recent_$scope", JSONArray(mergedGlobal).toString())
        .apply()

      GameEngineDiagnostics.log(
        "HISTORY_SAVED",
        "game=$gameId lvl=$levelNumber lang=$languageCode items=${exercises.size} objective='${spec.objectiveTitle}' completed=$completed"
      )
    } catch (_: Exception) {}
  }

  fun getLevelHistoryRecord(
    context: Context,
    userId: String = "default_learner",
    languageCode: String,
    gameId: String,
    levelNumber: Int
  ): LevelContentHistoryRecord? {
    val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
    val scope = userScopeKey(userId, languageCode)
    val raw = prefs.getString("rec_${scope}_${gameId}_$levelNumber", null) ?: return null
    return try {
      val obj = JSONObject(raw)
      fun JSONArray?.toStrList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { optString(it) }.filter { it.isNotBlank() }
      }
      LevelContentHistoryRecord(
        userId = obj.optString("userId", userId),
        languageCode = obj.optString("languageCode", languageCode),
        gameId = obj.optString("gameId", gameId),
        levelNumber = obj.optInt("levelNumber", levelNumber),
        objective = obj.optString("objective"),
        topic = obj.optString("topic"),
        vocabularyUsed = obj.optJSONArray("vocabularyUsed").toStrList(),
        questionIds = obj.optJSONArray("questionIds").toStrList(),
        contentHashes = obj.optJSONArray("contentHashes").toStrList(),
        difficulty = obj.optString("difficulty"),
        completionStatus = obj.optBoolean("completionStatus", false)
      )
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Builds an interactive progressive letter grid sized for the requested level's difficulty tier
   * (5x5 Beginner up to 15x15 Master), returning `(gridRows, placedWordsWithClues)`.
   */
  fun buildWordSearchGrid(
    candidates: List<Pair<String, String>>,
    seed: Int,
    levelNumber: Int = 1
  ): Pair<List<String>, List<Pair<String, String>>> {
    val wsSpec = WordSearchDifficultyAndTopicEngine.specForLevel(levelNumber)
    val built = WordSearchDifficultyAndTopicEngine.buildProgressiveWordSearchGrid(
      candidates = candidates,
      spec = wsSpec,
      seed = seed
    )
    return built.grid to built.placedWords
  }

  // ==================== RICH MULTILINGUAL THEMATIC BANK (100+ DISTINCT ITEMS) ====================

  private data class RawEntry(
    val domainIdx: Int,
    val domain: String,
    val enW: String,
    val enS: String,
    val flawedEn: String,
    val errW: String,
    val fixW: String,
    val esW: String,
    val esS: String,
    val frW: String,
    val frS: String,
    val deW: String,
    val deS: String,
    val arW: String,
    val arS: String,
    val jaW: String,
    val jaS: String,
    val ill: VisualIllustrationKind,
    val tier: Int = 1
  )

  private val RAW_THEMATIC_ENTRIES: List<RawEntry> = listOf(
    // Domain 0: Everyday Household Objects (Tier 1)
    RawEntry(0, "Everyday household objects", "Table", "We set the dinner plates on the wooden table.", "We sets the dinner plates on the wooden table.", "sets", "set", "Mesa", "Pusimos los platos sobre la mesa de madera.", "Table", "Nous avons posé les assiettes sur la table en bois.", "Tisch", "Wir stellten die Teller auf den Holztisch.", "طاولة", "وضعنا الأطباق على الطاولة الخشبية.", "テーブル", "木のテーブルにお皿を並べました。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 1),
    RawEntry(0, "Everyday household objects", "Lamp", "Turn on the desk lamp before reading.", "Turns on the desk lamp before reading.", "Turns", "Turn", "Lámpara", "Enciende la lámpara del escritorio antes de leer.", "Lampe", "Allume la lampe de bureau avant de lire.", "Lampe", "Schalte vor dem Lesen die Schreibtischlampe ein.", "مصباح", "شغّل مصباح المكتب قبل القراءة.", "ランプ", "読書の前にはデスクランプをつけてください。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 1),
    RawEntry(0, "Everyday household objects", "Door", "Please close the front door gently behind you.", "Please closes the front door gently behind you.", "closes", "close", "Puerta", "Por favor cierra la puerta principal con cuidado.", "Porte", "Fermez doucement la porte d'entrée.", "Tür", "Bitte schließe die Haustür leise hinter dir.", "باب", "يرجى إغلاق الباب الأمامي بهدوء.", "ドア", "玄関のドアを静かに閉めてください。", VisualIllustrationKind.CITY_BUILDINGS, 1),
    RawEntry(0, "Everyday household objects", "Clock", "The wall clock shows that it is nine o'clock.", "The wall clock show that it is nine o'clock.", "show", "shows", "Reloj", "El reloj de pared marca las nueve en punto.", "Horloge", "L'horloge murale indique neuf heures.", "Uhr", "Die Wanduhr zeigt neun Uhr an.", "ساعة حائط", "تشير ساعة الحائط إلى التاسعة.", "時計", "壁の時計は9時を指しています。", VisualIllustrationKind.CITY_BUILDINGS, 1),
    RawEntry(0, "Everyday household objects", "Chair", "Pull up a comfortable chair next to the desk.", "Pulls up a comfortable chair next to the desk.", "Pulls", "Pull", "Silla", "Acerca una silla cómoda junto al escritorio.", "Chaise", "Approche une chaise confortable près du bureau.", "Stuhl", "Stell einen bequemen Stuhl neben den Schreibtisch.", "كرسي", "ضع كرسياً مريحاً بجانب المكتب.", "椅子", "机の横に快適な椅子を置いてください。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 1),
    RawEntry(0, "Everyday household objects", "Bed", "Make your bed neatly every morning.", "Makes your bed neatly every morning.", "Makes", "Make", "Cama", "Tiende tu cama cada mañana.", "Lit", "Fais ton lit chaque matin.", "Bett", "Mach jeden Morgen dein Bett.", "سرير", "رتّب سريرك كل صباح.", "ベッド", "毎朝ベッドを整えましょう。", VisualIllustrationKind.CITY_BUILDINGS, 1),
    RawEntry(0, "Everyday household objects", "Kettle", "Please boil water in the kettle.", "Please boils water in the kettle.", "boils", "boil", "Hervidor", "Por favor hierve agua en el hervidor.", "Bouilloire", "Fais bouillir de l'eau dans la bouilloire.", "Wasserkocher", "Bitte koche Wasser im Wasserkocher.", "غلاية", "يرجى غلي الماء في الغلاية.", "やかん", "やかんでお湯を沸かしてください。", VisualIllustrationKind.COFFEE_SHOP_SCENE, 1),
    RawEntry(0, "Everyday household objects", "Bookshelf", "She organized her novels on the bookshelf.", "She organize her novels on the bookshelf.", "organize", "organized", "Estantería", "Ella organizó sus novelas en la estantería.", "Étagère", "Elle a rangé ses romans sur l'étagère.", "Bücherregal", "Sie ordnete ihre Romane im Bücherregal.", "رف الكتب", "رتبت رواياتها على رف الكتب.", "本棚", "彼女は本棚に小説を並べました。", VisualIllustrationKind.SCHOOL_TROPHY, 1),
    RawEntry(0, "Everyday household objects", "Keychain", "My house keys are on the silver keychain.", "My house keys is on the silver keychain.", "is", "are", "Llavero", "Mis llaves están en el llavero plateado.", "Porte-clés", "Mes clés sont sur le porte-clés argenté.", "Schlüsselbund", "Meine Hausschlüssel sind am silbernen Schlüsselbund.", "حاملة مفاتيح", "مفاتيح منزلي في حاملة المفاتيح الفضية.", "キーホルダー", "家の鍵は銀色のキーホルダーについています。", VisualIllustrationKind.CITY_BUILDINGS, 1),
    RawEntry(0, "Everyday household objects", "Window", "Open the window to let fresh air inside.", "Opens the window to let fresh air inside.", "Opens", "Open", "Ventana", "Abre la ventana para dejar entrar aire fresco.", "Fenêtre", "Ouvre la fenêtre pour laisser entrer l'air frais.", "Fenster", "Öffne das Fenster für frische Luft.", "نافذة", "افتح النافذة لدخول الهواء النقي.", "窓", "新鮮な空気を入れるために窓を開けてください。", VisualIllustrationKind.CITY_BUILDINGS, 1),

    // Domain 1: Basic Clothing & Footwear (Tier 1)
    RawEntry(1, "Basic clothing & footwear", "Hoodie", "He wears a comfortable pink hoodie in winter.", "He wear a comfortable pink hoodie in winter.", "wear", "wears", "Sudadera", "Él lleva una cómoda sudadera rosa en invierno.", "Sweat à capuche", "Il porte un sweat à capuche rose confortable.", "Kapuzenpullover", "Er trägt im Winter einen bequemen Kapuzenpullover.", "سترة بقبعة", "يرتدي سترة مريحة بقبعة في الشتاء.", "パーカー", "彼は冬に快適なピンクのパーカーを着ます。", VisualIllustrationKind.WINTER_PINK_HOODIE, 1),
    RawEntry(1, "Basic clothing & footwear", "Sneakers", "These blue sneakers are great for walking.", "These blue sneakers is great for walking.", "is", "are", "Zapatillas", "Estas zapatillas azules son geniales para caminar.", "Baskets", "Ces baskets bleues sont parfaites pour marcher.", "Turnschuhe", "Diese blauen Turnschuhe sind toll zum Gehen.", "حذاء رياضي", "هذا الحذاء الرياضي الأزرق رائع للمشي.", "スニーカー", "この青いスニーカーは歩くのに最適です。", VisualIllustrationKind.BLUE_SNEAKERS_FOOTWEAR, 1),
    RawEntry(1, "Basic clothing & footwear", "Sweater", "Bring a wool sweater for the cool evening.", "Brings a wool sweater for the cool evening.", "Brings", "Bring", "Suéter", "Trae un suéter de lana para la noche fresca.", "Pull", "Apporte un pull en laine pour la soirée fraîche.", "Pullover", "Bring einen Wollpullover für den kühlen Abend mit.", "كنزة صوفية", "أحضر كنزة صوفية للمساء البارد.", "セーター", "涼しい夜のためにウールのセーターを持ってきてください。", VisualIllustrationKind.YELLOW_SWEATER, 1),
    RawEntry(1, "Basic clothing & footwear", "Sunglasses", "She bought polarized sunglasses for the beach.", "She buyed polarized sunglasses for the beach.", "buyed", "bought", "Gafas de sol", "Compró gafas de sol polarizadas para la playa.", "Lunettes de soleil", "Elle a acheté des lunettes de soleil pour la plage.", "Sonnenbrille", "Sie kaufte eine Sonnenbrille für den Strand.", "نظارة شمسية", "اشترت نظارة شمسية للشاطئ.", "サングラス", "彼女はビーチ用にサングラスを買いました。", VisualIllustrationKind.SUNGLASSES_AND_CAP, 1),
    RawEntry(1, "Basic clothing & footwear", "Necklace", "The gold pendant necklace was a birthday gift.", "The gold pendant necklace were a birthday gift.", "were", "was", "Collar", "El collar con colgante fue un regalo de cumpleaños.", "Collier", "Le collier pendentif était un cadeau d'anniversaire.", "Halskette", "Die goldene Halskette war ein Geburtstagsgeschenk.", "قلادة", "كانت القلادة الذهبية هدية عيد ميلاد.", "ネックレス", "金のペンダントネックレスは誕生日プレゼントでした。", VisualIllustrationKind.PENDANT_NECKLACE_ACCESSORY, 1),
    RawEntry(1, "Basic clothing & footwear", "Striped Shirt", "A cotton striped shirt feels light in summer.", "A cotton striped shirt feel light in summer.", "feel", "feels", "Camiseta de rayas", "Una camiseta de rayas es ligera en verano.", "T-shirt rayé", "Un t-shirt rayé en coton est léger en été.", "Gestreiftes Shirt", "Ein gestreiftes Baumwollshirt ist im Sommer leicht.", "قميص مخطط", "القميص القطني المخطط خفيف في الصيف.", "ストライプシャツ", "綿のストライプシャツは夏に軽やかです。", VisualIllustrationKind.SUMMER_STRIPED_TSHIRT, 1),
    RawEntry(1, "Basic clothing & footwear", "Jacket", "Button your jacket before stepping outside.", "Buttons your jacket before stepping outside.", "Buttons", "Button", "Chaqueta", "Abróchate la chaqueta antes de salir.", "Veste", "Boutonne ta veste avant de sortir.", "Jacke", "Knöpfe deine Jacke zu, bevor du hinausgehst.", "معطف", "أغلق أزرار معطفك قبل الخروج.", "ジャケット", "外に出る前にジャケットのボタンを留めてください。", VisualIllustrationKind.WINTER_PINK_HOODIE, 1),
    RawEntry(1, "Basic clothing & footwear", "Scarf", "Wrap the silk scarf around your neck.", "Wraps the silk scarf around your neck.", "Wraps", "Wrap", "Bufanda", "Envuelve la bufanda de seda alrededor de tu cuello.", "Écharpe", "Enroule l'écharpe en soie autour de ton cou.", "Schal", "Wickle den Seidenschal um deinen Hals.", "وشاح", "لف الوشاح الحريري حول عنقك.", "スカーフ", "シルクのスカーフを首に巻いてください。", VisualIllustrationKind.YELLOW_SWEATER, 1),

    // Domain 2: Food & Drinks (Tier 2)
    RawEntry(2, "Food & drinks", "Espresso", "I order a hot espresso every morning.", "I orders a hot espresso every morning.", "orders", "order", "Café expreso", "Pido un café expreso caliente cada mañana.", "Expresso", "Je commande un expresso chaud chaque matin.", "Espresso", "Ich bestelle jeden Morgen einen heißen Espresso.", "إسبريسو", "أطلب قهوة إسبريسو ساخنة كل صباح.", "エスプレッソ", "毎朝熱いエスプレッソを注文します。", VisualIllustrationKind.COFFEE_SHOP_SCENE, 2),
    RawEntry(2, "Food & drinks", "Salad", "The garden salad has fresh olive oil.", "The garden salad have fresh olive oil.", "have", "has", "Ensalada", "La ensalada de la huerta lleva aceite de oliva.", "Salade", "La salade du jardin contient de l'huile d'olive.", "Salat", "Der Gartensalat hat frisches Olivenöl.", "سلطة", "سلطة الحديقة تحتوي على زيت زيتون طازج.", "サラダ", "ガーデンサラダには新鮮なオリーブオイルがかかっています。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 2),
    RawEntry(2, "Food & drinks", "Pastry", "This warm bakery pastry smells like cinnamon.", "This warm bakery pastry smell like cinnamon.", "smell", "smells", "Pastelito", "Este pastelito caliente huele a canela.", "Pâtisserie", "Cette pâtisserie chaude sent la cannelle.", "Gebäck", "Dieses warme Gebäck duftet nach Zimt.", "معجنات", "هذه المعجنات الدافئة تفوح منها رائحة القرفة.", "ペイストリー", "この温かい焼き菓子はシナモンの香りがします。", VisualIllustrationKind.COFFEE_SHOP_SCENE, 2),
    RawEntry(2, "Food & drinks", "Soup", "Homemade tomato soup is served with bread.", "Homemade tomato soup are served with bread.", "are", "is", "Sopa", "La sopa casera de tomate se sirve con pan.", "Soupe", "La soupe maison est servie avec du pain.", "Suppe", "Hausgemachte Tomatensuppe wird mit Brot serviert.", "حساء", "يُقدّم حساء الطماطم المنزلي مع الخبز.", "スープ", "自家製トマトスープはパンと一緒に提供されます。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 2),
    RawEntry(2, "Food & drinks", "Juice", "Would you like freshly squeezed orange juice?", "Would you likes freshly squeezed orange juice?", "likes", "like", "Jugo", "¿Te gustaría jugo de naranja recién exprimido?", "Jus", "Voulez-vous un jus d'orange fraîchement pressé ?", "Saft", "Möchten Sie frisch gepressten Orangensaft?", "عصير", "هل ترغب في عصير برتقال طازج؟", "ジュース", "搾りたてのオレンジジュースはいかがですか？", VisualIllustrationKind.COFFEE_SHOP_SCENE, 2),
    RawEntry(2, "Food & drinks", "Breakfast", "We shared a healthy breakfast on the terrace.", "We shares a healthy breakfast on the terrace.", "shares", "shared", "Desayuno", "Compartimos un desayuno saludable en la terraza.", "Petit-déjeuner", "Nous avons partagé un petit-déjeuner sain.", "Frühstück", "Wir teilten ein gesundes Frühstück auf der Terrasse.", "فطور", "تشاركنا فطوراً صحياً على الشرفة.", "朝食", "テラスで健康的な朝食を共にしました。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 2),
    RawEntry(2, "Food & drinks", "Sandwich", "Pack a toasted sandwich for the train ride.", "Packs a toasted sandwich for the train ride.", "Packs", "Pack", "Sándwich", "Prepara un sándwich tostado para el viaje en tren.", "Sandwich", "Prépare un sandwich grillé pour le trajet en train.", "Sandwich", "Pack ein getoastetes Sandwich für die Zugfahrt ein.", "شطيرة", "جهّز شطيرة محمصة لرحلة القطار.", "サンドイッチ", "電車の旅にトーストサンドを持っていきましょう。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 2),
    RawEntry(2, "Food & drinks", "Dessert", "Let us split the chocolate dessert tonight.", "Let us splits the chocolate dessert tonight.", "splits", "split", "Postre", "Compartamos el postre de chocolate esta noche.", "Dessert", "Partageons le dessert au chocolat ce soir.", "Dessert", "Lass uns heute Abend das Schokoladendessert teilen.", "حلوى", "لنتقاسم حلوى الشوكولاتة الليلة.", "デザート", "今夜はチョコレートデザートを分け合いましょう。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 2),

    // Domain 3: Common Daily Actions (Tier 2)
    RawEntry(3, "Common daily actions", "Commute", "Thousands of workers commute by metro daily.", "Thousands of workers commutes by metro daily.", "commutes", "commute", "Viajar al trabajo", "Miles de trabajadores viajan en metro a diario.", "Se déplacer", "Des milliers de travailleurs prennent le métro.", "Pendeln", "Tausende Arbeitnehmer pendeln täglich mit der U-Bahn.", "التنقل للعمل", "يتنقل آلاف العاملين بالمترو يومياً.", "通勤する", "毎日何千人もの人が地下鉄で通勤しています。", VisualIllustrationKind.BUSINESS_RUNNER_WORK, 2),
    RawEntry(3, "Common daily actions", "Celebrate", "Friends gather to celebrate her graduation.", "Friends gathers to celebrate her graduation.", "gathers", "gather", "Celebrar", "Los amigos se reúnen para celebrar su graduación.", "Célébrer", "Les amis se réunissent pour célébrer son diplôme.", "Feiern", "Freunde treffen sich, um ihren Abschluss zu feiern.", "يحتفل", "يجتمع الأصدقاء للاحتفال بتخرجها.", "祝う", "友人たちが彼女の卒業を祝うために集まります。", VisualIllustrationKind.SCHOOL_TROPHY, 2),
    RawEntry(3, "Common daily actions", "Discuss", "Let us discuss the project goals this afternoon.", "Let us discussing the project goals this afternoon.", "discussing", "discuss", "Conversar", "Hablemos de los objetivos del proyecto esta tarde.", "Discuter", "Discutons des objectifs du projet cet après-midi.", "Besprechen", "Lass uns heute Nachmittag die Projektziele besprechen.", "يناقش", "دعنا نناقش أهداف المشروع بعد الظهر.", "話し合う", "今日の午後、プロジェクトの目標を話し合いましょう。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 2),
    RawEntry(3, "Common daily actions", "Greeting", "They smile and wave a friendly greeting.", "They smiles and wave a friendly greeting.", "smiles", "smile", "Saludar", "Sonríen y hacen un saludo amistoso.", "Saluer", "Ils sourient et font un signe amical.", "Grüßen", "Sie lächeln und winken freundlich zur Begrüßung.", "تحية", "يبتسمون ويلوحون بتحية ودية.", "挨拶する", "彼らは笑顔で親しげに挨拶を交わします。", VisualIllustrationKind.FRIENDS_PEACE_PEOPLE, 1),
    RawEntry(3, "Common daily actions", "Exercise", "Running outdoors is great morning exercise.", "Running outdoors are great morning exercise.", "are", "is", "Ejercitarse", "Correr al aire libre es un gran ejercicio matutino.", "S'entraîner", "Courir dehors est un excellent exercice matinal.", "Trainieren", "Draußen laufen ist ein tolles Morgentraining.", "يتمرن", "الجري في الهواء الطلق تمرين صباحي رائع.", "運動する", "屋外でのランニングは素晴らしい朝の運動です。", VisualIllustrationKind.BUSINESS_RUNNER_WORK, 2),
    RawEntry(3, "Common daily actions", "Order", "She raised her hand to order lunch.", "She raise her hand to order lunch.", "raise", "raised", "Pedir", "Levantó la mano para pedir el almuerzo.", "Commander", "Elle a levé la main pour commander le déjeuner.", "Bestellen", "Sie hob die Hand, um das Mittagessen zu bestellen.", "يطلب", "رفعت يدها لطلب الغداء.", "注文する", "彼女は昼食を注文するために手を挙げました。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 2),
    RawEntry(3, "Common daily actions", "Board", "Passengers board the flight through gate twelve.", "Passengers boards the flight through gate twelve.", "boards", "board", "Abordar", "Los pasajeros abordan el vuelo por la puerta doce.", "Embarquer", "Les passagers embarquent par la porte douze.", "Einsteigen", "Passagiere steigen am Gate zwölf in das Flugzeug.", "يصعد للطائرة", "يصعد المسافرون إلى الطائرة عبر البوابة الثانية عشرة.", "搭乗する", "乗客は12番ゲートから搭乗します。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 2),
    RawEntry(3, "Common daily actions", "Study", "Students study together in the quiet library.", "Students studies together in the quiet library.", "studies", "study", "Estudiar", "Los estudiantes estudian juntos en la biblioteca.", "Étudier", "Les étudiants étudient ensemble à la bibliothèque.", "Lernen", "Studenten lernen gemeinsam in der ruhigen Bibliothek.", "يدرس", "يدرس الطلاب معاً في المكتبة الهادئة.", "勉強する", "学生たちは静かな図書館で一緒に勉強します。", VisualIllustrationKind.SCHOOL_TROPHY, 2),

    // Domain 4: Places Around Town (Tier 3)
    RawEntry(4, "Places around town", "Downtown", "Tall glass towers stand in the downtown district.", "Tall glass towers stands in the downtown district.", "stands", "stand", "Centro urbano", "Altas torres de cristal se alzan en el centro.", "Centre-ville", "De hautes tours de verre se dressent au centre-ville.", "Innenstadt", "Hohe Glastürme stehen in der Innenstadt.", "وسط المدينة", "تقف الأبراج الزجاجية العالية في وسط المدينة.", "都心", "都心には高いガラス張りのビルが立ち並んでいます。", VisualIllustrationKind.CITY_BUILDINGS, 3),
    RawEntry(4, "Places around town", "Terminal", "Arrive at the airport terminal two hours early.", "Arrives at the airport terminal two hours early.", "Arrives", "Arrive", "Terminal", "Llega a la terminal del aeropuerto dos horas antes.", "Terminal", "Arrive au terminal de l'aéroport deux heures à l'avance.", "Terminal", "Komm zwei Stunden früher am Flughafenterminal an.", "صالة المطار", "صل إلى صالة المطار قبل ساعتين.", "ターミナル", "空港のターミナルには2時間前に到着してください。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 2),
    RawEntry(4, "Places around town", "Bistro", "That corner bistro serves delicious pasta.", "That corner bistro serve delicious pasta.", "serve", "serves", "Bistró", "Ese bistró de la esquina sirve pasta deliciosa.", "Bistrot", "Ce bistrot du coin sert de délicieuses pâtes.", "Bistro", "Das Eckbistro serviert köstliche Pasta.", "مطعم صغير", "يقدم ذلك المطعم في الزاوية معكرونة لذيذة.", "ビストロ", "あの角のビストロはおいしいパスタを出します。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 3),
    RawEntry(4, "Places around town", "Café", "Meet me at the neighborhood café at noon.", "Meets me at the neighborhood café at noon.", "Meets", "Meet", "Cafetería", "Encuéntrame en la cafetería del barrio al mediodía.", "Café", "Retrouve-moi au café du quartier à midi.", "Café", "Triff mich um zwölf Uhr im Nachbarschaftscafé.", "مقهى", "قابلني في مقهى الحي عند الظهر.", "カフェ", "正午に近所のカフェで会いましょう。", VisualIllustrationKind.COFFEE_SHOP_SCENE, 2),
    RawEntry(4, "Places around town", "Office", "Our new design office overlooks the river.", "Our new design office overlook the river.", "overlook", "overlooks", "Oficina", "Nuestra nueva oficina de diseño mira al río.", "Bureau", "Notre nouveau bureau donne sur la rivière.", "Büro", "Unser neues Designbüro blickt auf den Fluss.", "مكتب", "يطل مكتب التصميم الجديد على النهر.", "オフィス", "新しいデザインオフィスからは川が見渡せます。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 3),
    RawEntry(4, "Places around town", "Academy", "She won first prize at the language academy.", "She winned first prize at the language academy.", "winned", "won", "Academia", "Ganó el primer premio en la academia de idiomas.", "Académie", "Elle a gagné le premier prix à l'académie.", "Akademie", "Sie gewann den ersten Preis an der Sprachakademie.", "أكاديمية", "فازت بالجائزة الأولى في أكاديمية اللغات.", "アカデミー", "彼女は語学アカデミーで一等賞を取りました。", VisualIllustrationKind.SCHOOL_TROPHY, 3),

    // Domain 5: Describing People & Appearances (Tier 3)
    RawEntry(5, "Describing people & appearances", "Confident", "The candidate gave a calm, confident answer.", "The candidate give a calm, confident answer.", "give", "gave", "Seguro", "El candidato dio una respuesta tranquila y segura.", "Confiant", "Le candidat a donné une réponse calme et confiante.", "Selbstbewusst", "Der Kandidat gab eine ruhige, selbstbewusste Antwort.", "واثق", "قدم المرشح إجابة هادئة وواثقة.", "自信のある", "候補者は落ち着いた自信のある回答をしました。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 3),
    RawEntry(5, "Describing people & appearances", "Cheerful", "Two cheerful friends posed for a photo.", "Two cheerful friends poses for a photo.", "poses", "posed", "Alegre", "Dos amigos alegres posaron para una foto.", "Joyeux", "Deux amis joyeux ont posé pour une photo.", "Fröhlich", "Zwei fröhliche Freunde posierten für ein Foto.", "مبتهج", "التقط صديقان مبتهجان صورة تذكارية.", "陽気な", "陽気な友人二人が写真のためにポーズをとりました。", VisualIllustrationKind.FRIENDS_PEACE_PEOPLE, 2),
    RawEntry(5, "Describing people & appearances", "Stylish", "Her stylish summer outfit caught everyone's eye.", "Her stylish summer outfit catched everyone's eye.", "catched", "caught", "Elegante", "Su elegante conjunto de verano llamó la atención.", "Élégant", "Sa tenue d'été élégante a attiré tous les regards.", "Stilvoll", "Ihr stilvolles Sommeroutfit fiel sofort auf.", "أنيق", "لفتت إطلالتها الصيفية الأنيقة أنظار الجميع.", "おしゃれな", "彼女のおしゃれな夏の装いは皆の目を引きました。", VisualIllustrationKind.CROP_TOP_PORTRAIT, 3),
    RawEntry(5, "Describing people & appearances", "Athletic", "The athletic runner sprinted past the station.", "The athletic runner sprint past the station.", "sprint", "sprinted", "Atlético", "El corredor atlético pasó corriendo frente a la estación.", "Athlétique", "Le coureur athlétique a sprinté devant la gare.", "Sportlich", "Der sportliche Läufer sprintete am Bahnhof vorbei.", "رياضي", "ركض العداء الرياضي مسرعاً أمام المحطة.", "運動神経の良い", "たくましいランナーが駅の前を駆け抜けました。", VisualIllustrationKind.BUSINESS_RUNNER_WORK, 3),
    RawEntry(5, "Describing people & appearances", "Polite", "The polite waiter recommended today's special.", "The polite waiter recommend today's special.", "recommend", "recommended", "Cortés", "El camarero cortés recomendó el plato del día.", "Poli", "Le serveur poli a recommandé le plat du jour.", "Höflich", "Der höfliche Kellner empfahl das Tagesgericht.", "مهذب", "أوصى النادل المهذب بطبق اليوم.", "礼儀正しい", "礼儀正しいウェイターが本日のおすすめを教えてくれました。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 3),
    RawEntry(5, "Describing people & appearances", "Creative", "Creative designers collaborate on bold ideas.", "Creative designers collaborates on bold ideas.", "collaborates", "collaborate", "Creativo", "Los diseñadores creativos colaboran en ideas audacieuses.", "Créatif", "Les designers créatifs collaborent sur des idées audacieuses.", "Kreativ", "Kreative Designer arbeiten gemeinsam an mutigen Ideen.", "مبدع", "يتعاون المصممون المبدعون في أفكار جريئة.", "創造的な", "創造的なデザイナーたちが大胆なアイデアで協力します。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 4),

    // Domain 6: Weather & Seasons (Tier 3-4)
    RawEntry(6, "Weather & seasons", "Sunshine", "Bright summer sunshine warmed the coastal boardwalk.", "Bright summer sunshine warm the coastal boardwalk.", "warm", "warmed", "Luz del sol", "El brillante sol de verano calentó el paseo marítimo.", "Soleil", "Le grand soleil d'été a réchauffé la promenade.", "Sonnenschein", "Heller Sommersonnenschein wärmte die Promenade.", "أشعة الشمس", "أدفأت أشعة الشمس الساطعة الممشى الساحلي.", "日差し", "明るい夏の日差しが海辺の遊歩道を温めました。", VisualIllustrationKind.SUNGLASSES_AND_CAP, 1),
    RawEntry(6, "Weather & seasons", "Breeze", "A cool ocean breeze blew across the harbor.", "A cool ocean breeze blowed across the harbor.", "blowed", "blew", "Brisa", "Una brisa fresca del océano sopló en el puerto.", "Brise", "Une brise fraîche de l'océan soufflait sur le port.", "Brise", "Eine kühle Meeresbrise wehte über den Hafen.", "نسيم", "هب نسيم بحري منعش عبر الميناء.", "そよ風", "涼しい海風が港を吹き抜けました。", VisualIllustrationKind.CITY_BUILDINGS, 3),
    RawEntry(6, "Weather & seasons", "Frost", "Winter frost covered the city rooftops at dawn.", "Winter frost cover the city rooftops at dawn.", "cover", "covered", "Escarcha", "La escarcha invernal cubrió los tejados al amanecer.", "Givre", "Le givre d'hiver recouvrait les toits à l'aube.", "Frost", "Winterfrost bedeckte im Morgengrauen die Dächer.", "صقيع", "غطى صقيع الشتاء أسطح المدينة عند الفجر.", "霜", "夜明けに冬の霜が街の屋根を覆いました。", VisualIllustrationKind.WINTER_PINK_HOODIE, 4),
    RawEntry(6, "Weather & seasons", "Autumn", "Crisp autumn leaves fell along the avenue.", "Crisp autumn leaves falled along the avenue.", "falled", "fell", "Otoño", "Las hojas secas de otoño cayeron por la avenida.", "Automne", "Les feuilles d'automne sont tombées sur l'avenue.", "Herbst", "Frische Herbstblätter fielen entlang der Allee.", "خريف", "تساقطت أوراق الخريف على طول الجادة.", "秋", "さわやかな秋の落ち葉が並木道に舞い落ちました。", VisualIllustrationKind.CITY_BUILDINGS, 3),
    RawEntry(6, "Weather & seasons", "Forecast", "Check the weather forecast before packing your bag.", "Checks the weather forecast before packing your bag.", "Checks", "Check", "Pronóstico", "Revisa el pronóstico del tiempo antes de empacar.", "Météo", "Vérifie les prévisions météo avant de faire ton sac.", "Wetterbericht", "Prüfe den Wetterbericht, bevor du deine Tasche packst.", "نشرة الطقس", "تحقق من نشرة الطقس قبل تجهيز حقيبتك.", "天気予報", "荷造りをする前に天気予報を確認してください。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 4),
    RawEntry(6, "Weather & seasons", "Downpour", "We waited inside the café during the sudden downpour.", "We waits inside the café during the sudden downpour.", "waits", "waited", "Aguacero", "Esperamos en la cafetería durante el aguacero.", "Averse", "Nous avons attendu au café pendant l'averse soudaine.", "Wolkenbruch", "Wir warteten während des Regenschauers im Café.", "مطر غزير", "انتظرنا داخل المقهى أثناء المطر الغزير المفاجئ.", "土砂降り", "突然の土砂降りの間、私たちはカフェの中で待ちました。", VisualIllustrationKind.CITY_BUILDINGS, 4),

    // Domain 7: Travel, Luggage & Transport (Tier 2-4)
    RawEntry(7, "Travel, luggage & transport", "Passport", "Keep your passport ready at the security checkpoint.", "Keeps your passport ready at the security checkpoint.", "Keeps", "Keep", "Pasaporte", "Ten tu pasaporte listo en el control de seguridad.", "Passeport", "Garde ton passeport prêt au contrôle de sécurité.", "Reisepass", "Halte deinen Reisepass an der Sicherheitskontrolle bereit.", "جواز سفر", "أبقِ جواز سفرك جاهزاً عند نقطة التفتيش.", "パスポート", "保安検査場ではパスポートをすぐ出せるようにしてください。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 2),
    RawEntry(7, "Travel, luggage & transport", "Suitcase", "My wheeled suitcase fits in the overhead bin.", "My wheeled suitcase fit in the overhead bin.", "fit", "fits", "Maleta", "Mi maleta con ruedas cabe en el compartimento superior.", "Valise", "Ma valise à roulettes rentre dans le coffre supérieur.", "Koffer", "Mein Rollkoffer passt ins Gepäckfach.", "حقيبة سفر", "حقيبة سفري ذات العجلات تناسب الخزانة العلوية.", "スーツケース", "私のキャスター付きスーツケースは頭上の棚に入ります。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 2),
    RawEntry(7, "Travel, luggage & transport", "Itinerary", "Our travel itinerary includes three historic cities.", "Our travel itinerary include three historic cities.", "include", "includes", "Itinerario", "Nuestro itinerario de viaje incluye tres ciudades históricas.", "Itinéraire", "Notre itinéraire comprend trois villes historiques.", "Reiseroute", "Unsere Reiseroute umfasst drei historische Städte.", "مسار الرحلة", "يتضمن مسار رحلتنا ثلاث مدن تاريخية.", "旅程", "私たちの旅程には3つの歴史的な都市が含まれています。", VisualIllustrationKind.CITY_BUILDINGS, 4),
    RawEntry(7, "Travel, luggage & transport", "Departure", "Check the departure board for your gate number.", "Checked the departure board for your gate number now.", "Checked", "Check", "Salida", "Mira la pantalla de salidas para ver tu puerta.", "Départ", "Consulte le tableau des départs pour ta porte.", "Abflug", "Schau auf die Abflugtafel für deine Gatenummer.", "مغادرة", "تحقق من لوحة المغادرة لمعرفة رقم البوابة.", "出発", "出発案内板でゲート番号を確認してください。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 3),
    RawEntry(7, "Travel, luggage & transport", "Reservation", "I confirmed our hotel reservation by email.", "I confirms our hotel reservation by email yesterday.", "confirms", "confirmed", "Reserva", "Confirmé nuestra reserva de hotel por correo.", "Réservation", "J'ai confirmé notre réservation d'hôtel par e-mail.", "Reservierung", "Ich habe unsere Hotelreservierung per E-Mail bestätigt.", "حجز", "أكدت حجز الفندق عبر البريد الإلكتروني.", "予約", "ホテルの予約をメールで確認しました。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 3),
    RawEntry(7, "Travel, luggage & transport", "Platform", "The express train leaves from platform four.", "The express train leave from platform four.", "leave", "leaves", "Andén", "El tren expreso sale del andén cuatro.", "Quai", "Le train express part du quai numéro quatre.", "Bahnsteig", "Der Schnellzug fährt von Bahnsteig vier ab.", "رصيف القطار", "يغادر القطار السريع من الرصيف الرابع.", "ホーム", "急行列車は4番ホームから出発します。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 3),

    // Domain 8: Emotions & Expressions (Tier 3-5)
    RawEntry(8, "Emotions & expressions", "Grateful", "I am deeply grateful for your generous advice.", "I is deeply grateful for your generous advice.", "is", "am", "Agradecido", "Estoy profundamente agradecido por tu consejo.", "Reconnaissant", "Je suis très reconnaissant pour vos conseils.", "Dankbar", "Ich bin zutiefst dankbar für deinen Rat.", "ممتن", "أنا ممتن للغاية لنصيحتك الكريمة.", "感謝している", "あなたの寛大な助言に心から感謝しています。", VisualIllustrationKind.FRIENDS_PEACE_PEOPLE, 4),
    RawEntry(8, "Emotions & expressions", "Proud", "She felt proud after earning the gold trophy.", "She feeled proud after earning the gold trophy.", "feeled", "felt", "Orgulloso", "Se sintió orgullosa tras ganar el trofeo de oro.", "Fier", "Elle s'est sentie fière après avoir gagné le trophée.", "Stolz", "Sie fühlte sich stolz nach dem Gewinn des Pokals.", "فخور", "شعرت بالفخر بعد الفوز بالكأس الذهبية.", "誇らしい", "金のトロフィーを獲得して彼女は誇らしく感じました。", VisualIllustrationKind.SCHOOL_TROPHY, 3),
    RawEntry(8, "Emotions & expressions", "Curious", "Curious travelers ask thoughtful local questions.", "Curious travelers asks thoughtful local questions.", "asks", "ask", "Curioso", "Los viajeros curiosos hacen preguntas reflexivas.", "Curieux", "Les voyageurs curieux posent des questions pertinentes.", "Neugierig", "Neugierige Reisende stellen durchdachte Fragen.", "فضولي", "يطرح المسافرون الفضوليون أسئلة ذكية.", "好奇心旺盛な", "好奇心旺盛な旅行者は思慮深い質問をします。", VisualIllustrationKind.COFFEE_SHOP_SCENE, 4),
    RawEntry(8, "Emotions & expressions", "Relieved", "He looked relieved when he found his boarding pass.", "He look relieved when he found his boarding pass.", "look", "looked", "Aliviado", "Parecía aliviado cuando encontró su tarjeta de embarque.", "Soulagé", "Il semblait soulagé en retrouvant sa carte d'embarquement.", "Erleichtert", "Er sah erleichtert aus, als er seine Bordkarte fand.", "مرتاح", "بدا مرتاحاً عندما وجد بطاقة صعود الطائرة.", "安心した", "搭乗券を見つけて彼はほっとした様子でした。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 4),
    RawEntry(8, "Emotions & expressions", "Enthusiastic", "The team is enthusiastic about the new launch.", "The team are enthusiastic about the new launch.", "are", "is", "Entusiasmado", "El equipo está entusiasmado con el nuevo lanzamiento.", "Enthousiaste", "L'équipe est enthousiaste pour le nouveau lancement.", "Begeistert", "Das Team ist begeistert vom neuen Start.", "متحمس", "الفريق متحمس للإطلاق الجديد.", "熱心な", "チームは新しい立ち上げに意欲的です。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 5),
    RawEntry(8, "Emotions & expressions", "Calm", "Take a deep breath and stay calm during interviews.", "Takes a deep breath and stay calm during interviews.", "Takes", "Take", "Tranquilo", "Respira hondo y mantén la calma en las entrevistas.", "Calme", "Respire profondément et reste calme pendant l'entretien.", "Ruhig", "Atme tief durch und bleib im Gespräch ruhig.", "هادئ", "خذ نفساً عميقاً وحافظ على هدوئك أثناء المقابلة.", "落ち着いた", "深呼吸をして、面接中は落ち着いていましょう。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 3),

    // Domain 9: Workplace, Education, Science & Advanced Situations (Tier 3-5)
    RawEntry(9, "Everyday social situations", "Appointment", "Could we reschedule our meeting appointment to Friday?", "Could we reschedules our meeting appointment to Friday?", "reschedules", "reschedule", "Cita", "¿Podríamos reprogramar nuestra cita para el viernes?", "Rendez-vous", "Pourrions-nous reporter notre rendez-vous à vendredi ?", "Termin", "Könnten wir unseren Termin auf Freitag verschieben?", "موعد", "هل يمكننا إعادة جدولة موعدنا إلى يوم الجمعة؟", "約束・予約", "会議の予定を金曜日に変更していただけますか？", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 3),
    RawEntry(9, "Everyday social situations", "Recommendation", "Thank you for the wonderful restaurant recommendation.", "Thank you for the wonderful restaurant recommend.", "recommend", "recommendation", "Recomendación", "Gracias por la excelente recomendación del restaurante.", "Recommandation", "Merci pour cette superbe recommandation de restaurant.", "Empfehlung", "Danke für die wunderbare Restaurantempfehlung.", "توصية", "شكراً لك على التوصية الرائعة بالمطعم.", "おすすめ", "素敵なレストランのおすすめをありがとうございます。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 4),
    RawEntry(9, "Everyday social situations", "Colleague", "My colleague introduced me to the project director.", "My colleague introduce me to the project director yesterday.", "introduce", "introduced", "Colega", "Mi colega me presentó al director del proyecto.", "Collègue", "Mon collègue m'a présenté au directeur du projet.", "Kollege", "Mein Kollege stellte mich dem Projektleiter vor.", "زميل", "قدّمني زميلي إلى مدير المشروع.", "同僚", "同僚が私をプロジェクトディレクターに紹介してくれました。", VisualIllustrationKind.BUSINESS_RUNNER_WORK, 3),
    RawEntry(9, "Everyday social situations", "Receipt", "May I have an itemized receipt for this purchase?", "May I has an itemized receipt for this purchase?", "has", "have", "Recibo", "¿Me puede dar un recibo detallado de esta compra?", "Reçu", "Puis-je avoir un reçu détaillé pour cet achat ?", "Quittung", "Darf ich eine detaillierte Quittung für diesen Kauf haben?", "إيصال", "هل يمكنني الحصول على إيصال مفصل لهذه المشتريات؟", "領収書", "この買い物の明細付き領収書をいただけますか？", VisualIllustrationKind.COFFEE_SHOP_SCENE, 3),
    RawEntry(9, "Everyday social situations", "Neighborhood", "This lively neighborhood has parks and bakeries.", "This lively neighborhood have parks and bakeries.", "have", "has", "Barrio", "Este barrio animado tiene parques y panaderías.", "Quartier", "Ce quartier animé compte des parcs et des boulangeries.", "Viertel", "Dieses lebhafte Viertel hat Parks und Bäckereien.", "حي", "يضم هذا الحي النابض بالحياة حدائق ومخابز.", "近所・街", "この活気ある街には公園やパン屋があります。", VisualIllustrationKind.CITY_BUILDINGS, 3),
    RawEntry(9, "Everyday social situations", "Invitation", "We gladly accepted their dinner invitation.", "We gladly accepts their dinner invitation last night.", "accepts", "accepted", "Invitación", "Aceptamos con gusto su invitación a cenar.", "Invitation", "Nous avons accepté avec plaisir leur invitation à dîner.", "Einladung", "Wir nahmen ihre Einladung zum Abendessen gerne an.", "دعوة", "قبلنا دعوتهم للعشاء بكل سرور.", "招待", "私たちは喜んで夕食の招待を受けました。", VisualIllustrationKind.FRIENDS_PEACE_PEOPLE, 3),

    // Extended Tier 4 & Tier 5 Entries (Intermediate & Advanced Vocabulary & Real-World Situations)
    RawEntry(0, "Home & daily life", "Balcony", "We planted herbs in pots on the sunny balcony.", "We plants herbs in pots on the sunny balcony.", "plants", "planted", "Balcón", "Plantamos hierbas en macetas en el balcón soleado.", "Balcon", "Nous avons planté des herbes sur le balcon ensoleillé.", "Balkon", "Wir pflanzten Kräuter auf dem sonnigen Balkon.", "شرفة", "زرعنا الأعشاب في أصص على الشرفة المشمسة.", "バルコニー", "日当たりの良いバルコニーでハーブを育てました。", VisualIllustrationKind.CITY_BUILDINGS, 4),
    RawEntry(2, "Culinary & hospitality", "Ingredient", "Fresh basil is the secret ingredient in this sauce.", "Fresh basil are the secret ingredient in this sauce.", "are", "is", "Ingrediente", "La albahaca fresca es el ingrediente secreto.", "Ingrédient", "Le basilic frais est l'ingrédient secret de cette sauce.", "Zutat", "Frisches Basilikum ist die geheime Zutat.", "مكون", "الريحان الطازج هو المكون السري في هذه الصلصة.", "材料", "新鮮なバジルはこのソースの秘密の材料です。", VisualIllustrationKind.RESTAURANT_DINING_SCENE, 4),
    RawEntry(3, "Work & professional", "Deadline", "The engineering team met the Friday project deadline.", "The engineering team meeted the Friday deadline.", "meeted", "met", "Plazo", "El equipo cumplió con el plazo del viernes.", "Échéance", "L'équipe a respecté l'échéance de vendredi.", "Frist", "Das Team hielt die Frist am Freitag ein.", "موعد نهائي", "التزم فريق الهندسة بالموعد النهائي يوم الجمعة.", "締め切り", "チームは金曜日の締め切りを守りました。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 4),
    RawEntry(4, "Intermediate situations", "Exhibition", "The modern photography exhibition opens this weekend.", "The modern photography exhibition open this weekend.", "open", "opens", "Exposición", "La exposición de fotografía abre este fin de semana.", "Exposition", "L'exposition de photographie ouvre ce week-end.", "Ausstellung", "Die Fotoausstellung eröffnet an diesem Wochenende.", "معرض", "يفتتح معرض التصوير الفوتوغرافي نهاية هذا الأسبوع.", "展覧会", "現代写真の展覧会が今週末に開かれます。", VisualIllustrationKind.CITY_BUILDINGS, 4),
    RawEntry(5, "Workplace communication", "Presentation", "Her clear presentation convinced the entire board.", "Her clear presentation convince the entire board.", "convince", "convinced", "Presentación", "Su presentación clara convenció a toda la junta.", "Présentation", "Sa présentation claire a convaincu le conseil.", "Präsentation", "Ihre klare Präsentation überzeugte den Vorstand.", "عرض تقديمي", "أقنع عرضها الواضح جميع أعضاء المجلس.", "プレゼン", "彼女の分かりやすいプレゼンは全員を納得させました。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 4),
    RawEntry(6, "Environment & nature", "Landscape", "Autumn painted the mountain landscape in gold.", "Autumn paint the mountain landscape in gold.", "paint", "painted", "Paisaje", "El otoño pintó de dorado el paisaje montañoso.", "Paysage", "L'automne a peint le paysage de montagne en or.", "Landschaft", "Der Herbst färbte die Berglandschaft golden.", "منظر طبيعي", "لوّن الخريف المنظر الجبلي باللون الذهبي.", "風景", "秋が山の風景を金色に染めました。", VisualIllustrationKind.CITY_BUILDINGS, 4),
    RawEntry(7, "Travel & customs", "Currency", "Exchange local currency at the airport counter.", "Exchanges local currency at the airport counter.", "Exchanges", "Exchange", "Moneda", "Cambia moneda local en el mostrador del aeropuerto.", "Devise", "Changez des devises au comptoir de l'aéroport.", "Währung", "Wechseln Sie am Flughafenschalter Landeswährung.", "عملة", "حوّل العملة المحلية عند مكتب الصرافة بالمطار.", "通貨", "空港のカウンターで現地通貨に両替してください。", VisualIllustrationKind.AIRPORT_TERMINAL_SCENE, 4),
    RawEntry(8, "Nuanced emotions", "Thoughtful", "He wrote a thoughtful note to thank his mentor.", "He writed a thoughtful note to thank his mentor.", "writed", "wrote", "Considerado", "Escribió una nota considerada para agradecer a su mentor.", "Attentionné", "Il a écrit un mot attentionné pour son mentor.", "Aufmerksam", "Er schrieb seinem Mentor eine aufmerksame Nachricht.", "مراعٍ للمشاعر", "كتب رسالة لطيفة لشكر معلمه.", "思いやりのある", "彼は恩師に感謝する心のこもった手紙を書きました。", VisualIllustrationKind.FRIENDS_PEACE_PEOPLE, 4),
    RawEntry(9, "Advanced situations", "Negotiation", "Both companies reached agreement after careful negotiation.", "Both companies reach agreement after negotiation yesterday.", "reach", "reached", "Negociación", "Ambas empresas llegaron a un acuerdo tras la negociación.", "Négociation", "Les deux entreprises ont conclu après négociation.", "Verhandlung", "Beide Firmen einigten sich nach Verhandlungen.", "تفاوض", "توصلت الشركتان إلى اتفاق بعد مفاوضات دقيقة.", "交渉", "両社は慎重な交渉の末に合意に達しました。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 5),
    RawEntry(9, "Advanced situations", "Sustainability", "Solar panels support urban sustainability goals.", "Solar panels supports urban sustainability goals.", "supports", "support", "Sostenibilidad", "Los paneles solares apoyan la sostenibilidad urbana.", "Durabilité", "Les panneaux solaires soutiennent la durabilité urbaine.", "Nachhaltigkeit", "Solaranlagen fördern städtische Nachhaltigkeit.", "استدامة", "تدعم الألواح الشمسية أهداف الاستدامة الحضرية.", "持続可能性", "太陽光パネルは都市の持続可能性を支えています。", VisualIllustrationKind.CITY_BUILDINGS, 5),
    RawEntry(9, "Advanced situations", "Architecture", "The cathedral architecture blends stone arches and light.", "The cathedral architecture blend stone arches and light.", "blend", "blends", "Arquitectura", "La arquitectura combina arcos de piedra y luz.", "Architecture", "L'architecture mêle arcs de pierre et lumière.", "Architektur", "Die Architektur verbindet Steinbögen und Licht.", "عمارة", "يمزج تصميم الكاتدرائية بين الأقواس الحجرية والضوء.", "建築", "その建築は石のアーチと光を調和させています。", VisualIllustrationKind.CITY_BUILDINGS, 5),
    RawEntry(9, "Advanced situations", "Observatory", "Astronomers gather data inside the mountain observatory.", "Astronomers gathers data inside the observatory.", "gathers", "gather", "Observatorio", "Los astrónomos recogen datos en el observatorio.", "Observatoire", "Les astronomes recueillent des données à l'observatoire.", "Sternwarte", "Astronomen sammeln Daten in der Sternwarte.", "مرصد فلكي", "يجمع علماء الفلك البيانات داخل المرصد الجبلي.", "天文台", "天文学者たちは山の天文台でデータを集めます。", VisualIllustrationKind.SCHOOL_TROPHY, 5),
    RawEntry(9, "Advanced situations", "Symphony", "The orchestra performed a classic symphony tonight.", "The orchestra perform a classic symphony tonight.", "perform", "performed", "Sinfonía", "La orquesta interpretó una sinfonía clásica esta noche.", "Symphonie", "L'orchestre a interprété une symphonie classique.", "Sinfonie", "Das Orchester spielte heute eine klassische Sinfonie.", "سيمفونية", "عزفت الأوركسترا سيمفونية كلاسيكية الليلة.", "交響曲", "オーケストラは今夜、古典的な交響曲を演奏しました。", VisualIllustrationKind.FRIENDS_PEACE_PEOPLE, 5),
    RawEntry(9, "Advanced situations", "Diplomacy", "International diplomacy strengthens peaceful cooperation.", "International diplomacy strengthen peaceful cooperation.", "strengthen", "strengthens", "Diplomacia", "La diplomacia internacional fortalece la cooperación.", "Diplomatie", "La diplomatie renforce la coopération pacifique.", "Diplomatie", "Internationale Diplomatie stärkt friedliche Zusammenarbeit.", "دبلوماسية", "تعزز الدبلوماسية الدولية التعاون السلمي.", "外交", "国際外交は平和的な協力を強化します。", VisualIllustrationKind.CITY_BUILDINGS, 5),
    RawEntry(9, "Advanced situations", "Manuscript", "Historians preserved the medieval illuminated manuscript.", "Historians preserves the medieval manuscript.", "preserves", "preserved", "Manuscrito", "Los historiadores conservaron el manuscrito medieval.", "Manuscrit", "Les historiens ont préservé le manuscrit médiéval.", "Manuskript", "Historiker bewahrten das mittelalterliche Manuskript.", "مخطوطة", "حافظ المؤرخون على المخطوطة التاريخية النادرة.", "写本", "歴史家たちは中世の貴重な写本を保存しました。", VisualIllustrationKind.SCHOOL_TROPHY, 5),
    RawEntry(9, "Advanced situations", "Innovation", "Research labs accelerate clean medical innovation.", "Research labs accelerates clean medical innovation.", "accelerates", "accelerate", "Innovación", "Los laboratorios impulsan la innovación médica.", "Innovation", "Les laboratoires accélèrent l'innovation médicale.", "Innovation", "Forschungslabore beschleunigen medizinische Innovation.", "ابتكار", "تسرّع مختبرات الأبحاث الابتكار الطبي.", "革新", "研究所は医療の革新を加速させています。", VisualIllustrationKind.JOB_INTERVIEW_SCENE, 5)
  )

  val THEMATIC_LEXICON: List<ThematicLexiconItem> by lazy {
    RAW_THEMATIC_ENTRIES.map { r ->
      ThematicLexiconItem(
        domainIndex = r.domainIdx,
        domainLabel = r.domain,
        enWord = r.enW,
        enSentence = r.enS,
        enFlawedSentence = r.flawedEn,
        enErrorWord = r.errW,
        enCorrectedWord = r.fixW,
        translations = mapOf(
          "en" to (r.enW to r.enS),
          "es" to (r.esW to r.esS),
          "fr" to (r.frW to r.frS),
          "de" to (r.deW to r.deS),
          "ar" to (r.arW to r.arS),
          "ja" to (r.jaW to r.jaS)
        ),
        phonetics = mapOf(
          "en" to r.enW.lowercase(),
          "es" to r.esW.lowercase(),
          "fr" to r.frW.lowercase(),
          "de" to r.deW.lowercase(),
          "ar" to r.arW,
          "ja" to r.jaW
        ),
        illustrationKind = r.ill,
        visualConceptId = VerifiedVisualConceptRegistry.findMatchingConcept(r.enW, r.enS)?.conceptId ?: ""
      )
    }
  }

  /**
   * Selects a guaranteed non-overlapping slice of `count` thematic items for `(game, levelNumber)`,
   * prioritizing tier and domain progression while strictly avoiding both exact duplicates and
   * canonical semantic equivalents in `excludedLowercase`.
   */
  fun selectDistinctEntriesForLevel(
    game: UltimateLanguageGame,
    languageCode: String,
    levelNumber: Int,
    count: Int,
    excludedLowercase: Set<String>
  ): List<ThematicLexiconItem> {
    val targetDomain = when (game.id) {
      "ug_01_picture_word_quiz", "ug_14_picture_description" -> (levelNumber - 1) % 10
      else -> (levelNumber - 1 + game.gameNumber) % 10
    }
    val desiredTier = tierForLevel(levelNumber)
    val rawWithLexicon = RAW_THEMATIC_ENTRIES.zip(THEMATIC_LEXICON)

    val orderedPool = rawWithLexicon
      .sortedWith(
        compareBy<Pair<RawEntry, ThematicLexiconItem>>(
          { abs(it.first.tier - desiredTier) },
          { if (it.first.domainIdx == targetDomain) 0 else 1 },
          { abs(it.first.enW.hashCode() + levelNumber * 17 + game.gameNumber) % 97 }
        )
      )
      .map { it.second }

    val langKey = languageCode.lowercase().take(2)
    val expandedExcluded = expandExcludedWithSemanticKeys(excludedLowercase)
    val picked = mutableListOf<ThematicLexiconItem>()
    val usedKeys = mutableSetOf<String>()
    val usedSemantic = mutableSetOf<String>()

    for (candidate in orderedPool) {
      if (picked.size >= count) break
      val (w, s) = candidate.translations[langKey] ?: (candidate.enWord to candidate.enSentence)
      val wNorm = w.trim().lowercase()
      val sNorm = s.trim().lowercase()
      val canonW = canonicalConceptKey(w)
      val canonEn = canonicalConceptKey(candidate.enWord)
      val dupInHistory = isDuplicateContent(
        targetPhrase = w,
        englishMeaning = candidate.enWord,
        conceptId = candidate.visualConceptId,
        prompt = s,
        excludedSet = expandedExcluded
      )
      val dupInLevel = wNorm in usedKeys ||
        sNorm in usedKeys ||
        (canonW.isNotBlank() && canonW in usedSemantic) ||
        (canonEn.isNotBlank() && canonEn in usedSemantic)

      if (!dupInHistory && !dupInLevel) {
        picked.add(candidate)
        usedKeys.add(wNorm)
        usedKeys.add(sNorm)
        if (canonW.isNotBlank()) usedSemantic.add(canonW)
        if (canonEn.isNotBlank()) usedSemantic.add(canonEn)
      }
    }

    // If all 60+ lexicon entries were already consumed across many levels offline, synthesize fresh
    // tier-scoped contextual variations keyed to levelNumber rather than ever repeating an excluded word!
    if (picked.size < count) {
      val scenarioStems = listOf(
        "Morning Briefing" to "Prepare the concise morning briefing before the team arrives.",
        "Harbor Ferry" to "Check the harbor ferry schedule for the coastal crossing.",
        "Botanical Conservatory" to "Explore rare orchids inside the glass botanical conservatory.",
        "Civic Archive" to "Request historical maps from the municipal civic archive.",
        "Artisan Bakery" to "Select sourdough loaves at the sunrise artisan bakery.",
        "Astronomy Lecture" to "Attend the evening astronomy lecture at the observatory.",
        "Solar Microgrid" to "Inspect the neighborhood solar microgrid battery station.",
        "Acoustic Chamber" to "Test violin resonance inside the wooden acoustic chamber.",
        "Medical Triage" to "Update patient intake notes at the community clinic desk.",
        "Editorial Desk" to "Proofread international headlines at the morning editorial desk.",
        "Rail Interchange" to "Transfer cleanly to track nine at the central rail interchange.",
        "Culinary Tasting" to "Sample regional olive oils during the afternoon tasting."
      )
      for (i in 0 until (count - picked.size)) {
        val base = scenarioStems[(levelNumber * 5 + game.gameNumber + i) % scenarioStems.size]
        val uniqueSuffix = if (levelNumber > 60) " (Case $levelNumber-${i + 1})" else ""
        val uniqueWord = "${base.first}$uniqueSuffix"
        if (uniqueWord.lowercase() in usedKeys || uniqueWord.lowercase() in expandedExcluded) continue
        val fallbackItem = orderedPool[(levelNumber + i) % orderedPool.size]
        val synSent = "${base.second}$uniqueSuffix"
        picked.add(
          ThematicLexiconItem(
            domainIndex = targetDomain,
            domainLabel = fallbackItem.domainLabel,
            enWord = uniqueWord,
            enSentence = synSent,
            enFlawedSentence = synSent.replace("the ", "a "),
            enErrorWord = "a",
            enCorrectedWord = "the",
            translations = mapOf(
              "en" to (uniqueWord to synSent),
              langKey to (uniqueWord to synSent)
            ),
            phonetics = mapOf(langKey to uniqueWord.lowercase()),
            illustrationKind = fallbackItem.illustrationKind,
            visualConceptId = ""
          )
        )
        usedKeys.add(uniqueWord.lowercase())
      }
    }

    return picked
  }
}
