package com.example.data.repository

import android.content.Context
import com.example.ai.GeminiTutorService
import com.example.data.model.Language
import com.example.data.model.MistakeNotebookEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The 7 primary categories for the 70+ LingoSphere Ultimate Language Games World.
 */
enum class UltimateGameCategory(
  val id: String,
  val label: String,
  val shortTabLabel: String,
  val emoji: String,
  val accentHex: Long,
  val subtitle: String
) {
  VOCABULARY(
    id = "vocabulary",
    label = "Vocabulary",
    shortTabLabel = "Vocabulary",
    emoji = "🖼️",
    accentHex = 0xFF14C8E8,
    subtitle = "Visual quizzes, word matching & flashcards"
  ),
  SPELLING(
    id = "spelling",
    label = "Spelling",
    shortTabLabel = "Spelling",
    emoji = "🐝",
    accentHex = 0xFFFFB300,
    subtitle = "SpellBee, Wordigo & audio dictation"
  ),
  SPEAKING(
    id = "speaking",
    label = "Speaking",
    shortTabLabel = "Speaking",
    emoji = "🎙️",
    accentHex = 0xFF7C4DFF,
    subtitle = "Voice practice, pronunciation & shadowing"
  ),
  LISTENING(
    id = "listening",
    label = "Listening",
    shortTabLabel = "Listening",
    emoji = "🎧",
    accentHex = 0xFF00E676,
    subtitle = "Audio comprehension, stories & fast speech"
  ),
  GRAMMAR(
    id = "grammar",
    label = "Grammar",
    shortTabLabel = "Grammar",
    emoji = "🧩",
    accentHex = 0xFFFF4081,
    subtitle = "Sentence builder, grammar repair & tenses"
  ),
  CONVERSATION(
    id = "conversation",
    label = "Conversations",
    shortTabLabel = "Conversations",
    emoji = "💬",
    accentHex = 0xFF00B0FF,
    subtitle = "Interactive shopping, travel & café dialogues"
  ),
  BRAIN_CHALLENGE(
    id = "brain_challenge",
    label = "Brain Games",
    shortTabLabel = "Brain Games",
    emoji = "🧠",
    accentHex = 0xFFFF6D00,
    subtitle = "Escape room, word search & logic puzzles"
  )
}

/**
 * Defines the distinct interactive gameplay mechanic rendered by the level engine.
 */
enum class GameInteractionMechanic(val label: String) {
  PICTURE_IDENTIFICATION("Visual Identification"),
  WORD_MATCH_PAIRS("Match Pairs"),
  MEMORY_CARD_FLIP("Memory Card Grid"),
  FLASHCARD_FLIP("Interactive Flashcards"),
  SPELLING_INPUT("Audio & Letter Spelling"),
  WORDIGO_GRID("Word Guessing Grid"),
  WORD_SEARCH_GRID("Interactive Word Search"),
  SCRAMBLE_TILES("Tile Assembly"),
  GRAMMAR_REPAIR("Grammar Repair"),
  SPEAKING_MIC("Pronunciation Practice"),
  SPEAKING_REPEAT_SENTENCE("Repeat After Me"),
  SPEAKING_ARTICULATION_SPRINT("Articulation Sprint"),
  SPEAKING_DESCRIBE_SCENE("Speak the Scene"),
  SPEAKING_QUICK_ANSWER("Quick Answer"),
  SPEAKING_SPEECH_COACH("Speech Coach"),
  SPEAKING_FLUENCY_SPRINT("Fluency Sprint"),
  SPEAKING_SHADOWING("Shadow Speaking"),
  SPEAKING_DIALOGUE_LINE("Dialogue Line"),
  SPEAKING_ROLEPLAY_MISSION("Roleplay Mission"),
  LISTENING_COMPREHENSION("Audio Comprehension"),
  AUDIO_WORD_MATCH("Audio Word Match"),
  LISTEN_AND_TYPE("Listen & Type"),
  SOUND_DETECTIVE("Sound Detective"),
  LISTENING_STORY_QUIZ("Listening Story Quiz"),
  MISSING_AUDIO_WORD("Key Audio Detail"),
  CONVERSATION_LISTENING("Dialogue Listening"),
  FAST_SPEECH_CHALLENGE("Fast Speech"),
  GRAMMAR_CORRECTION("Contextual Choice"),
  AI_CONVERSATION_MISSION("Interactive Dialogue"),
  ESCAPE_ROOM_PUZZLE("Escape Room Locks"),
  BRAIN_PUZZLE_CHALLENGE("Language Logic Puzzle"),
  SPEED_RUN_TIMER("Timed Speed Challenge");

  val isAudioFirstListeningMechanic: Boolean
    get() = this in setOf(
      LISTENING_COMPREHENSION,
      AUDIO_WORD_MATCH,
      LISTEN_AND_TYPE,
      SOUND_DETECTIVE,
      LISTENING_STORY_QUIZ,
      MISSING_AUDIO_WORD,
      CONVERSATION_LISTENING,
      FAST_SPEECH_CHALLENGE
    )

  val isSpeakingMechanic: Boolean
    get() = this in setOf(
      SPEAKING_MIC,
      SPEAKING_REPEAT_SENTENCE,
      SPEAKING_ARTICULATION_SPRINT,
      SPEAKING_DESCRIBE_SCENE,
      SPEAKING_QUICK_ANSWER,
      SPEAKING_SPEECH_COACH,
      SPEAKING_FLUENCY_SPRINT,
      SPEAKING_SHADOWING,
      SPEAKING_DIALOGUE_LINE,
      SPEAKING_ROLEPLAY_MISSION
    )
}

/**
 * 500-Level Difficulty Progression Structure.
 */
enum class LevelProgressionBand(
  val minLevel: Int,
  val maxLevel: Int,
  val bandName: String,
  val cefrEquivalent: String,
  val complexityHint: String
) {
  ABSOLUTE_BEGINNER(1, 50, "Foundations", "A1-", "core survival words, everyday objects, greetings, and simple concrete nouns"),
  BEGINNER(51, 100, "Basic Practical Language", "A1", "clothing, food, daily actions, places around town, and simple present statements"),
  ELEMENTARY(101, 175, "Everyday Communication", "A2", "routine conversations, shopping, directions, questions, and past/future phrases"),
  PRE_INTERMEDIATE(176, 250, "Intermediate Language Skills", "A2+", "travel situations, workplace essentials, descriptive language, and connectors"),
  INTERMEDIATE(251, 325, "Complex Language Use", "B1", "opinions, storytelling, job interviews, multi-clause sentences, and common idioms"),
  UPPER_INTERMEDIATE(326, 400, "Advanced Communication", "B2", "nuanced discussions, professional communication, subtle grammar repairs, and fast speech"),
  ADVANCED(401, 450, "Advanced Accuracy & Fluency", "C1", "academic and business precision, idiomatic nuance, and complex transformations"),
  EXPERT(451, 500, "Expert Challenges", "C2", "near-native mastery, subtle register shifts, literary expressions, and high-speed recall");

  companion object {
    fun forLevel(level: Int): LevelProgressionBand {
      val clamped = level.coerceIn(1, 500)
      return entries.firstOrNull { clamped in it.minLevel..it.maxLevel } ?: ABSOLUTE_BEGINNER
    }
  }
}

/**
 * Definition of one of the 70 functional games in LingoSphere.
 */
data class UltimateLanguageGame(
  val id: String,
  val gameNumber: Int,
  val title: String,
  val oneLineDescription: String,
  val category: UltimateGameCategory,
  val mechanic: GameInteractionMechanic,
  val iconEmoji: String,
  val accentHex: Long,
  val maxLevels: Int = 500,
  val baseXpPerLevel: Int = 35,
  val topicFocus: String = "General Fluency",
  val freeLevelsLimit: Int = 50
)

/**
 * Persisted per-user, per-language, per-game 500-level progression record.
 */
data class GameLevelProgressState(
  val gameId: String,
  val languageCode: String,
  val highestUnlockedLevel: Int = 1,
  val completedLevels: Map<Int, Int> = emptyMap(), // level -> stars (1..3)
  val bestAccuracyByLevel: Map<Int, Int> = emptyMap(), // level -> 0..100
  val recentMistakeWords: List<String> = emptyList(),
  val lastPlayedEpochMs: Long = 0L
) {
  val totalCompletedCount: Int get() = completedLevels.size
  val totalStarsEarned: Int get() = completedLevels.values.sum()
}

/**
 * Persisted daily missions state for the Games Hub.
 */
data class DailyGameMissionsState(
  val dateKey: String,
  val vocabGamesCompleted: Int = 0,
  val speakingMinutesCompleted: Int = 0,
  val listeningChallengesCompleted: Int = 0,
  val newWordsLearned: Int = 0,
  val claimedMissionIds: Set<String> = emptySet()
)

/**
 * Single validated exercise within a game level, supporting all distinct game mechanics.
 */
data class DynamicLevelExercise(
  val id: String,
  val gameId: String,
  val levelNumber: Int,
  val mechanic: GameInteractionMechanic,
  val prompt: String,
  val targetPhrase: String,
  val translation: String,
  val phonetic: String,
  val options: List<String>,
  val correctIndex: Int,
  val answerOptions: List<ValidatedAnswerOption> = emptyList(),
  val correctOptionId: String = "",
  val visualConceptId: String = "",
  val secondaryAudioPhrase: String = "",
  val dialogueSpeakerALine: String = "",
  val dialogueSpeakerBLine: String = "",
  val clozeMaskedSentence: String = "",
  val scrambledTokens: List<String> = emptyList(),
  val matchingPairs: List<Pair<String, String>> = emptyList(),
  val illustrationKind: VisualIllustrationKind = VisualIllustrationKind.CROP_TOP_PORTRAIT,
  val hint: String = "",
  val explanation: String = "",
  val conversationStarter: String = "",
  val conversationRole: String = "Language Partner",
  val timeLimitSeconds: Int = 30,
  val incorrectSentence: String = "",
  val errorWord: String = "",
  val correctedWord: String = "",
  val wordSearchGrid: List<String> = emptyList(),
  val wordSearchTargets: List<Pair<String, String>> = emptyList(),
  val escapeStageNumber: Int = 1,
  val escapeRoomClueCode: String = "",
  val objectiveLabel: String = "",
  val contentHash: String = "",
  val resolvedImageUrl: String = "",
  val visualIconKey: String = ""
) {
  fun resolvedAnswerOptions(): List<ValidatedAnswerOption> {
    if (answerOptions.isNotEmpty()) return answerOptions
    return options.mapIndexed { idx, text ->
      ValidatedAnswerOption(
        id = "${id}_ans_${idx}_${abs(text.lowercase().hashCode()) % 99991}",
        text = text
      )
    }
  }

  fun resolvedCorrectOptionId(): String {
    if (correctOptionId.isNotBlank()) return correctOptionId
    val resolved = resolvedAnswerOptions()
    return resolved.getOrNull(correctIndex)?.id ?: resolved.firstOrNull()?.id.orEmpty()
  }

  fun isOptionCorrect(optionId: String?): Boolean {
    return !optionId.isNullOrBlank() && optionId == resolvedCorrectOptionId()
  }
}

/**
 * Result of deterministic or AI-grounded learning response evaluation.
 */
data class LearningResponseEvaluation(
  val isPassed: Boolean,
  val accuracyScore: Int,
  val matchedWords: List<String>,
  val missingWords: List<String>,
  val feedbackTitle: String,
  val detailedExplanation: String,
  val suggestedImprovement: String
)

/**
 * Centralized AI Game Engine & 500-Level Progression Manager for all 70+ LingoSphere games.
 */
object DynamicAiGameEngine {

  private const val PREFS_NAME = "lingosphere_ultimate_games_500_prefs"
  private const val ENGINE_SCHEMA_VERSION = "v8_dynamic_tiered_picture_and_zero_repeat"
  private val memoryExerciseCache = mutableMapOf<String, List<DynamicLevelExercise>>()
  private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /**
   * Complete catalog of all 70 functional games across Categories A–G.
   */
  val ALL_70_GAMES: List<UltimateLanguageGame> = listOf(
    // ==================== CATEGORY A — VOCABULARY GAMES (1–15) ====================
    UltimateLanguageGame(
      id = "ug_01_picture_word_quiz",
      gameNumber = 1,
      title = "Picture Word Quiz",
      oneLineDescription = "Identify vocabulary from illustrated scenes",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.PICTURE_IDENTIFICATION,
      iconEmoji = "🖼️",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Visual Objects, Clothing & Everyday Scenes"
    ),
    UltimateLanguageGame(
      id = "ug_02_word_match",
      gameNumber = 2,
      title = "Word Match",
      oneLineDescription = "Pair target words with their meanings",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.WORD_MATCH_PAIRS,
      iconEmoji = "🔗",
      accentHex = 0xFF14C8E8,
      topicFocus = "Core Vocabulary & Rapid Translation"
    ),
    UltimateLanguageGame(
      id = "ug_03_flashcard_flip",
      gameNumber = 3,
      title = "Flashcard Flip",
      oneLineDescription = "Flip interactive cards for active recall",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.FLASHCARD_FLIP,
      iconEmoji = "🃏",
      accentHex = 0xFF00E676,
      topicFocus = "High-Frequency Words & Examples"
    ),
    UltimateLanguageGame(
      id = "ug_04_memory_pairs",
      gameNumber = 4,
      title = "Memory Pairs",
      oneLineDescription = "Flip face-down cards to match word pairs",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.MEMORY_CARD_FLIP,
      iconEmoji = "🧠",
      accentHex = 0xFFFF4081,
      topicFocus = "Spatial & Semantic Memory Pairs"
    ),
    UltimateLanguageGame(
      id = "ug_05_missing_letters",
      gameNumber = 5,
      title = "Missing Letters",
      oneLineDescription = "Type the missing letters to complete words",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.SPELLING_INPUT,
      iconEmoji = "🔤",
      accentHex = 0xFFFFB300,
      topicFocus = "Orthography & Word Completion"
    ),
    UltimateLanguageGame(
      id = "ug_06_word_scramble",
      gameNumber = 6,
      title = "Word Scramble",
      oneLineDescription = "Unscramble letter tiles into target words",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "🔀",
      accentHex = 0xFF00B0FF,
      topicFocus = "Anagram & Word Structure"
    ),
    UltimateLanguageGame(
      id = "ug_07_word_search",
      gameNumber = 7,
      title = "Word Search",
      oneLineDescription = "Find hidden target words inside a letter grid",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.WORD_SEARCH_GRID,
      iconEmoji = "🔍",
      accentHex = 0xFF26A69A,
      topicFocus = "Pattern Recognition & Thematic Words"
    ),
    UltimateLanguageGame(
      id = "ug_08_category_sort",
      gameNumber = 8,
      title = "Category Sort",
      oneLineDescription = "Group vocabulary words by topic and meaning",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.WORD_MATCH_PAIRS,
      iconEmoji = "🗂️",
      accentHex = 0xFFAB47BC,
      topicFocus = "Semantic Categorization"
    ),
    UltimateLanguageGame(
      id = "ug_09_odd_one_out",
      gameNumber = 9,
      title = "Odd One Out",
      oneLineDescription = "Spot the word that does not belong",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.BRAIN_PUZZLE_CHALLENGE,
      iconEmoji = "🎯",
      accentHex = 0xFFFF7043,
      topicFocus = "Semantic Distinction & Nuance"
    ),
    UltimateLanguageGame(
      id = "ug_10_word_association",
      gameNumber = 10,
      title = "Word Association",
      oneLineDescription = "Connect related words and natural collocations",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.WORD_MATCH_PAIRS,
      iconEmoji = "🌐",
      accentHex = 0xFF42A5F5,
      topicFocus = "Native Collocations & Word Families"
    ),
    UltimateLanguageGame(
      id = "ug_11_synonym_challenge",
      gameNumber = 11,
      title = "Synonym Challenge",
      oneLineDescription = "Match vocabulary with close synonyms",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.WORD_MATCH_PAIRS,
      iconEmoji = "🪞",
      accentHex = 0xFF66BB6A,
      topicFocus = "Synonyms & Expressive Variety"
    ),
    UltimateLanguageGame(
      id = "ug_12_antonym_challenge",
      gameNumber = 12,
      title = "Antonym Challenge",
      oneLineDescription = "Pair words with their opposite meanings",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.WORD_MATCH_PAIRS,
      iconEmoji = "⚖️",
      accentHex = 0xFFEC407A,
      topicFocus = "Antonyms & Contrastive Vocabulary"
    ),
    UltimateLanguageGame(
      id = "ug_13_vocabulary_speed_run",
      gameNumber = 13,
      title = "Vocabulary Blitz",
      oneLineDescription = "Answer rapid vocabulary prompts before time runs out",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.SPEED_RUN_TIMER,
      iconEmoji = "⚡",
      accentHex = 0xFFFFCA28,
      topicFocus = "Instant Recall Under Time Pressure"
    ),
    UltimateLanguageGame(
      id = "ug_14_picture_description",
      gameNumber = 14,
      title = "Picture Description",
      oneLineDescription = "Choose the sentence that fits the visual scene",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.PICTURE_IDENTIFICATION,
      iconEmoji = "🎨",
      accentHex = 0xFF5C6BC0,
      topicFocus = "Visual Scene Sentences & Actions"
    ),
    UltimateLanguageGame(
      id = "ug_15_word_builder",
      gameNumber = 15,
      title = "Word Builder",
      oneLineDescription = "Assemble complete words from character tiles",
      category = UltimateGameCategory.VOCABULARY,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "🧱",
      accentHex = 0xFF29B6F6,
      topicFocus = "Prefixes, Roots & Word Assembly"
    ),

    // ==================== CATEGORY B — SPELLING GAMES (16–23) ====================
    UltimateLanguageGame(
      id = "ug_16_spellbee",
      gameNumber = 16,
      title = "SpellBee",
      oneLineDescription = "Listen to spoken audio and spell each word",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.SPELLING_INPUT,
      iconEmoji = "🐝",
      accentHex = 0xFFFFB300,
      topicFocus = "Audio-to-Spelling Precision"
    ),
    UltimateLanguageGame(
      id = "ug_17_wordigo",
      gameNumber = 17,
      title = "Wordigo",
      oneLineDescription = "Guess the hidden word in 6 tries with tile feedback",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.WORDIGO_GRID,
      iconEmoji = "🟩",
      accentHex = 0xFF00C853,
      topicFocus = "Word Deduction & Letter Position"
    ),
    UltimateLanguageGame(
      id = "ug_18_spelling_race",
      gameNumber = 18,
      title = "Spelling Race",
      oneLineDescription = "Spell words accurately against the clock",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.SPELLING_INPUT,
      iconEmoji = "🏎️",
      accentHex = 0xFFFF6D00,
      topicFocus = "Fast Accurate Orthography"
    ),
    UltimateLanguageGame(
      id = "ug_19_fix_the_spelling",
      gameNumber = 19,
      title = "Fix the Spelling",
      oneLineDescription = "Spot and repair misspelled words",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.GRAMMAR_REPAIR,
      iconEmoji = "🛠️",
      accentHex = 0xFFEC407A,
      topicFocus = "Common Spelling Pitfalls & Accents"
    ),
    UltimateLanguageGame(
      id = "ug_20_letter_order",
      gameNumber = 20,
      title = "Letter Order",
      oneLineDescription = "Tap letters in exact order to build the word",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "🔠",
      accentHex = 0xFF14C8E8,
      topicFocus = "Sequential Character Ordering"
    ),
    UltimateLanguageGame(
      id = "ug_21_dictation_challenge",
      gameNumber = 21,
      title = "Dictation Challenge",
      oneLineDescription = "Listen carefully and type the spoken expression",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.SPELLING_INPUT,
      iconEmoji = "✍️",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Full Phrase & Word Dictation"
    ),
    UltimateLanguageGame(
      id = "ug_22_missing_vowels",
      gameNumber = 22,
      title = "Missing Vowels",
      oneLineDescription = "Restore missing vowels and accents to words",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.SPELLING_INPUT,
      iconEmoji = "🅰️",
      accentHex = 0xFF00E676,
      topicFocus = "Vowel Patterns & Accent Marks"
    ),
    UltimateLanguageGame(
      id = "ug_23_spelling_ladder",
      gameNumber = 23,
      title = "Spelling Ladder",
      oneLineDescription = "Progress from short words to complex vocabulary",
      category = UltimateGameCategory.SPELLING,
      mechanic = GameInteractionMechanic.SPELLING_INPUT,
      iconEmoji = "🪜",
      accentHex = 0xFF29B6F6,
      topicFocus = "Progressive Length Spelling Mastery"
    ),

    // ==================== CATEGORY C — SPEAKING GAMES (24–33) ====================
    UltimateLanguageGame(
      id = "ug_24_pronunciation_battle",
      gameNumber = 24,
      title = "Pronunciation Practice",
      oneLineDescription = "Pronounce individual words with phonetic & sound feedback",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_MIC,
      iconEmoji = "🎙️",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Individual Word Phonemes & Syllable Clarity"
    ),
    UltimateLanguageGame(
      id = "ug_25_repeat_after_me",
      gameNumber = 25,
      title = "Repeat After Me",
      oneLineDescription = "Listen and repeat complete spoken sentences",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE,
      iconEmoji = "🗣️",
      accentHex = 0xFF14C8E8,
      topicFocus = "Complete Sentence Repetition & Word Accuracy"
    ),
    UltimateLanguageGame(
      id = "ug_26_tongue_twister",
      gameNumber = 26,
      title = "Articulation Sprint",
      oneLineDescription = "Master tongue twisters and difficult sound combinations",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT,
      iconEmoji = "🌪️",
      accentHex = 0xFFFF4081,
      topicFocus = "Consonant Combinations & Tongue Twisters"
    ),
    UltimateLanguageGame(
      id = "ug_27_speak_the_picture",
      gameNumber = 27,
      title = "Speak the Scene",
      oneLineDescription = "Describe what is happening in an illustrated scene",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE,
      iconEmoji = "📸",
      accentHex = 0xFFFFB300,
      topicFocus = "Visual Scene Description & Vocabulary"
    ),
    UltimateLanguageGame(
      id = "ug_28_quick_answer",
      gameNumber = 28,
      title = "Quick Answer",
      oneLineDescription = "Answer practical everyday questions in your own words",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_QUICK_ANSWER,
      iconEmoji = "⚡",
      accentHex = 0xFF00E676,
      topicFocus = "Spontaneous Everyday Question Answering"
    ),
    UltimateLanguageGame(
      id = "ug_29_speech_coach_challenge",
      gameNumber = 29,
      title = "Speech Coach",
      oneLineDescription = "Speak freely for personalized grammar & pronunciation coaching",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_SPEECH_COACH,
      iconEmoji = "🎯",
      accentHex = 0xFF00B0FF,
      topicFocus = "Personalized Speech Analysis & Targeted Drills"
    ),
    UltimateLanguageGame(
      id = "ug_30_fluency_sprint",
      gameNumber = 30,
      title = "Fluency Sprint",
      oneLineDescription = "Speak continuously on everyday topics with a 15s–60s timer",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT,
      iconEmoji = "🏃",
      accentHex = 0xFFFF6D00,
      topicFocus = "Timed Continuous Topic Speaking"
    ),
    UltimateLanguageGame(
      id = "ug_31_shadow_speaking",
      gameNumber = 31,
      title = "Shadow Speaking",
      oneLineDescription = "Speak simultaneously with synchronized highlighted audio",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_SHADOWING,
      iconEmoji = "👥",
      accentHex = 0xFFAB47BC,
      topicFocus = "Simultaneous Audio Shadowing & Rhythm"
    ),
    UltimateLanguageGame(
      id = "ug_32_dialogue_completion",
      gameNumber = 32,
      title = "Dialogue Line",
      oneLineDescription = "Speak the next natural reply in a two-person conversation",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_DIALOGUE_LINE,
      iconEmoji = "💭",
      accentHex = 0xFF26A69A,
      topicFocus = "Natural Multi-Turn Dialogue Completion"
    ),
    UltimateLanguageGame(
      id = "ug_33_roleplay_mission",
      gameNumber = 33,
      title = "Roleplay Mission",
      oneLineDescription = "Complete multi-turn real-life scenarios by voice",
      category = UltimateGameCategory.SPEAKING,
      mechanic = GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION,
      iconEmoji = "🎭",
      accentHex = 0xFF5C6BC0,
      topicFocus = "10 Real-Life Interactive Voice Scenarios"
    ),

    // ==================== CATEGORY D — LISTENING GAMES (34–41) ====================
    UltimateLanguageGame(
      id = "ug_34_listen_and_choose",
      gameNumber = 34,
      title = "Listen & Choose",
      oneLineDescription = "Hear the audio clip and select its meaning",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.LISTENING_COMPREHENSION,
      iconEmoji = "🎧",
      accentHex = 0xFF00E676,
      topicFocus = "Spoken Phrase Comprehension"
    ),
    UltimateLanguageGame(
      id = "ug_35_audio_word_match",
      gameNumber = 35,
      title = "Audio Word Match",
      oneLineDescription = "Play audio clips and match each to its meaning",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.AUDIO_WORD_MATCH,
      iconEmoji = "🔊",
      accentHex = 0xFF14C8E8,
      topicFocus = "Auditory Word Recognition"
    ),
    UltimateLanguageGame(
      id = "ug_36_listen_and_type",
      gameNumber = 36,
      title = "Listen & Type",
      oneLineDescription = "Transcribe spoken words and short phrases",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.LISTEN_AND_TYPE,
      iconEmoji = "⌨️",
      accentHex = 0xFFFFB300,
      topicFocus = "Listening Transcription Accuracy"
    ),
    UltimateLanguageGame(
      id = "ug_37_sound_detective",
      gameNumber = 37,
      title = "Sound Detective",
      oneLineDescription = "Compare spoken clips and identify the target sound",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.SOUND_DETECTIVE,
      iconEmoji = "🕵️",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Minimal Pairs & Ear Training"
    ),
    UltimateLanguageGame(
      id = "ug_38_listening_story_quiz",
      gameNumber = 38,
      title = "Listening Story Quiz",
      oneLineDescription = "Listen to short stories and answer questions",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.LISTENING_STORY_QUIZ,
      iconEmoji = "📖",
      accentHex = 0xFFFF4081,
      topicFocus = "Narrative Listening & Key Details"
    ),
    UltimateLanguageGame(
      id = "ug_39_missing_audio_word",
      gameNumber = 39,
      title = "Key Audio Detail",
      oneLineDescription = "Catch the missing word from a spoken sentence",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.MISSING_AUDIO_WORD,
      iconEmoji = "🧩",
      accentHex = 0xFF00B0FF,
      topicFocus = "Contextual Keyword Extraction"
    ),
    UltimateLanguageGame(
      id = "ug_40_conversation_listening",
      gameNumber = 40,
      title = "Dialogue Listening",
      oneLineDescription = "Listen to 2-speaker exchanges and answer",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.CONVERSATION_LISTENING,
      iconEmoji = "👥",
      accentHex = 0xFF26A69A,
      topicFocus = "Real-World Dialogue Comprehension"
    ),
    UltimateLanguageGame(
      id = "ug_41_fast_speech_challenge",
      gameNumber = 41,
      title = "Fast Speech",
      oneLineDescription = "Understand natural brisk-paced speech",
      category = UltimateGameCategory.LISTENING,
      mechanic = GameInteractionMechanic.FAST_SPEECH_CHALLENGE,
      iconEmoji = "⏩",
      accentHex = 0xFFFF6D00,
      topicFocus = "High-Speed Native Cadence Comprehension"
    ),

    // ==================== CATEGORY E — GRAMMAR & SENTENCE GAMES (42–51) ====================
    UltimateLanguageGame(
      id = "ug_42_sentence_builder",
      gameNumber = 42,
      title = "Sentence Builder",
      oneLineDescription = "Arrange word tiles into natural sentences",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "🧩",
      accentHex = 0xFFFF4081,
      topicFocus = "Word Order & Syntax Mastery"
    ),
    UltimateLanguageGame(
      id = "ug_43_grammar_detective",
      gameNumber = 43,
      title = "Grammar Repair",
      oneLineDescription = "Spot and fix grammatical mistakes in sentences",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.GRAMMAR_REPAIR,
      iconEmoji = "🔎",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Error Detection & Grammatical Repair"
    ),
    UltimateLanguageGame(
      id = "ug_44_fill_the_blank",
      gameNumber = 44,
      title = "Fill the Blank",
      oneLineDescription = "Complete sentences with the right verb or particle",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.GRAMMAR_CORRECTION,
      iconEmoji = "✏️",
      accentHex = 0xFF14C8E8,
      topicFocus = "Cloze Grammar & Prepositions"
    ),
    UltimateLanguageGame(
      id = "ug_45_tense_challenge",
      gameNumber = 45,
      title = "Tense Challenge",
      oneLineDescription = "Repair past, present, and future verb forms",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.GRAMMAR_REPAIR,
      iconEmoji = "⏳",
      accentHex = 0xFFFFB300,
      topicFocus = "Verb Tenses & Time Expressions"
    ),
    UltimateLanguageGame(
      id = "ug_46_sentence_correction",
      gameNumber = 46,
      title = "Sentence Correction",
      oneLineDescription = "Edit flawed sentences into natural phrasing",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.GRAMMAR_REPAIR,
      iconEmoji = "✅",
      accentHex = 0xFF00E676,
      topicFocus = "Agreement, Word Choice & Register"
    ),
    UltimateLanguageGame(
      id = "ug_47_question_builder",
      gameNumber = 47,
      title = "Question Builder",
      oneLineDescription = "Build natural questions from word tiles",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "❓",
      accentHex = 0xFF00B0FF,
      topicFocus = "Interrogative Word Order & Polite Questions"
    ),
    UltimateLanguageGame(
      id = "ug_48_translation_challenge",
      gameNumber = 48,
      title = "Translation Builder",
      oneLineDescription = "Assemble accurate translations tile by tile",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "🌐",
      accentHex = 0xFFAB47BC,
      topicFocus = "Idiomatic & Accurate Sentence Translation"
    ),
    UltimateLanguageGame(
      id = "ug_49_sentence_transformation",
      gameNumber = 49,
      title = "Sentence Transform",
      oneLineDescription = "Rewrite statements with updated tense or politeness",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.GRAMMAR_REPAIR,
      iconEmoji = "🔄",
      accentHex = 0xFF26A69A,
      topicFocus = "Tense Shifts, Negation & Formality"
    ),
    UltimateLanguageGame(
      id = "ug_50_grammar_speed_quiz",
      gameNumber = 50,
      title = "Grammar Blitz",
      oneLineDescription = "Solve timed grammar prompts against the clock",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.SPEED_RUN_TIMER,
      iconEmoji = "⏱️",
      accentHex = 0xFFFF6D00,
      topicFocus = "Fast Instinctive Grammar Reflexes"
    ),
    UltimateLanguageGame(
      id = "ug_51_complete_the_story",
      gameNumber = 51,
      title = "Complete the Story",
      oneLineDescription = "Select cohesive lines that connect a short story",
      category = UltimateGameCategory.GRAMMAR,
      mechanic = GameInteractionMechanic.GRAMMAR_CORRECTION,
      iconEmoji = "📚",
      accentHex = 0xFF5C6BC0,
      topicFocus = "Narrative Cohesion & Connectors"
    ),

    // ==================== CATEGORY F — CONVERSATION GAMES (52–62) ====================
    UltimateLanguageGame(
      id = "ug_52_shopping_mission",
      gameNumber = 52,
      title = "Shopping Mission",
      oneLineDescription = "Ask about sizes, colors, and prices in a shop",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "🛍️",
      accentHex = 0xFF00B0FF,
      topicFocus = "Retail Shopping, Sizes, Prices & Payment"
    ),
    UltimateLanguageGame(
      id = "ug_53_restaurant_challenge",
      gameNumber = 53,
      title = "Restaurant Table",
      oneLineDescription = "Order dishes, ask about ingredients, and pay",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "🍽️",
      accentHex = 0xFFFF7043,
      topicFocus = "Restaurant Ordering & Dining Etiquette"
    ),
    UltimateLanguageGame(
      id = "ug_54_airport_mission",
      gameNumber = 54,
      title = "Airport Check-in",
      oneLineDescription = "Handle luggage, boarding passes, and gates",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "✈️",
      accentHex = 0xFF14C8E8,
      topicFocus = "Airport Check-in, Security & Boarding"
    ),
    UltimateLanguageGame(
      id = "ug_55_job_interview_simulator",
      gameNumber = 55,
      title = "Job Interview",
      oneLineDescription = "Discuss your experience and career strengths",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "💼",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Professional Interviews & Career Pitch"
    ),
    UltimateLanguageGame(
      id = "ug_56_hotel_checkin",
      gameNumber = 56,
      title = "Hotel Reception",
      oneLineDescription = "Confirm room bookings, breakfast, and local tips",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "🏨",
      accentHex = 0xFFFFB300,
      topicFocus = "Hotel Reception, Room Keys & Local Tips"
    ),
    UltimateLanguageGame(
      id = "ug_57_making_friends",
      gameNumber = 57,
      title = "Making Friends",
      oneLineDescription = "Chat naturally about hobbies, travel, and plans",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "🤝",
      accentHex = 0xFF00E676,
      topicFocus = "Introductions, Hobbies & Socializing"
    ),
    UltimateLanguageGame(
      id = "ug_58_doctor_visit",
      gameNumber = 58,
      title = "Clinic Visit",
      oneLineDescription = "Describe symptoms and understand pharmacy advice",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "🩺",
      accentHex = 0xFFEC407A,
      topicFocus = "Healthcare, Symptoms & Pharmacy"
    ),
    UltimateLanguageGame(
      id = "ug_59_telephone_conversation",
      gameNumber = 59,
      title = "Phone Booking",
      oneLineDescription = "Schedule appointments and confirm times clearly",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "📞",
      accentHex = 0xFF26A69A,
      topicFocus = "Phone Etiquette, Bookings & Clarifications"
    ),
    UltimateLanguageGame(
      id = "ug_60_daily_life_roleplay",
      gameNumber = 60,
      title = "Café & Errands",
      oneLineDescription = "Navigate everyday neighborhood conversations",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "☕",
      accentHex = 0xFFAB47BC,
      topicFocus = "Everyday Errands & Community Chat"
    ),
    UltimateLanguageGame(
      id = "ug_61_conversation_choices",
      gameNumber = 61,
      title = "Polite Replies",
      oneLineDescription = "Choose the most natural reply for social moments",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.GRAMMAR_CORRECTION,
      iconEmoji = "🔀",
      accentHex = 0xFF42A5F5,
      topicFocus = "Pragmatics, Politeness & Natural Replies"
    ),
    UltimateLanguageGame(
      id = "ug_62_mystery_conversation",
      gameNumber = 62,
      title = "Mystery Interview",
      oneLineDescription = "Ask smart questions in dialogue to uncover clues",
      category = UltimateGameCategory.CONVERSATION,
      mechanic = GameInteractionMechanic.AI_CONVERSATION_MISSION,
      iconEmoji = "🕵️‍♂️",
      accentHex = 0xFFFF6D00,
      topicFocus = "Investigative Questions & Clue Deduction"
    ),

    // ==================== CATEGORY G — BRAIN & CHALLENGE GAMES (63–70) ====================
    UltimateLanguageGame(
      id = "ug_63_vocabulary_puzzle",
      gameNumber = 63,
      title = "Word Riddle",
      oneLineDescription = "Solve semantic riddles in your target language",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.WORDIGO_GRID,
      iconEmoji = "🧠",
      accentHex = 0xFFFF6D00,
      topicFocus = "Contextual Riddles & Word Deduction"
    ),
    UltimateLanguageGame(
      id = "ug_64_language_trivia",
      gameNumber = 64,
      title = "Culture & Idioms",
      oneLineDescription = "Explore expressions, customs, and regional usage",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.BRAIN_PUZZLE_CHALLENGE,
      iconEmoji = "🌍",
      accentHex = 0xFF14C8E8,
      topicFocus = "Cultural Etiquette, Idioms & Regional Trivia"
    ),
    UltimateLanguageGame(
      id = "ug_65_word_connections",
      gameNumber = 65,
      title = "Word Connections",
      oneLineDescription = "Pair words that share a hidden thematic link",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.MEMORY_CARD_FLIP,
      iconEmoji = "🔗",
      accentHex = 0xFF7C4DFF,
      topicFocus = "Thematic Word Grouping & Associations"
    ),
    UltimateLanguageGame(
      id = "ug_66_word_chain",
      gameNumber = 66,
      title = "Word Search Grid",
      oneLineDescription = "Locate hidden vocabulary across letter matrices",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.WORD_SEARCH_GRID,
      iconEmoji = "⛓️",
      accentHex = 0xFF00E676,
      topicFocus = "Pattern Search & Lexical Recall"
    ),
    UltimateLanguageGame(
      id = "ug_67_language_escape_room",
      gameNumber = 67,
      title = "Language Escape Room",
      oneLineDescription = "Solve 4 connected language locks to unlock the exit",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.ESCAPE_ROOM_PUZZLE,
      iconEmoji = "🔐",
      accentHex = 0xFFFFB300,
      topicFocus = "Multi-Stage Codebreaking & Comprehension"
    ),
    UltimateLanguageGame(
      id = "ug_68_story_sequencing",
      gameNumber = 68,
      title = "Story Order",
      oneLineDescription = "Arrange phrases into logical narrative order",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.SCRAMBLE_TILES,
      iconEmoji = "📜",
      accentHex = 0xFFFF4081,
      topicFocus = "Chronological & Logical Story Order"
    ),
    UltimateLanguageGame(
      id = "ug_69_word_maze",
      gameNumber = 69,
      title = "Vault Checkpoints",
      oneLineDescription = "Unlock each checkpoint by solving language clues",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.ESCAPE_ROOM_PUZZLE,
      iconEmoji = "🧭",
      accentHex = 0xFF00B0FF,
      topicFocus = "Decision Path Vocabulary & Context"
    ),
    UltimateLanguageGame(
      id = "ug_70_daily_mixed_challenge",
      gameNumber = 70,
      title = "Mixed Skill Sprint",
      oneLineDescription = "Timed mix of vocabulary, grammar, and comprehension",
      category = UltimateGameCategory.BRAIN_CHALLENGE,
      mechanic = GameInteractionMechanic.SPEED_RUN_TIMER,
      iconEmoji = "🏆",
      accentHex = 0xFFFFCA28,
      topicFocus = "Comprehensive Multi-Skill Mastery"
    )
  )

  fun getAll70Games(): List<UltimateLanguageGame> = ALL_70_GAMES

  fun getGamesByCategory(category: UltimateGameCategory?): List<UltimateLanguageGame> {
    if (category == null) return ALL_70_GAMES
    return ALL_70_GAMES.filter { it.category == category }
  }

  fun getLevelBand(levelNumber: Int): LevelProgressionBand {
    return LevelProgressionBand.forLevel(levelNumber)
  }

  fun getCurriculumObjective(game: UltimateLanguageGame, levelNumber: Int): LevelCurriculumSpec {
    return GameCurriculumAndAntiRepetitionEngine.getCurriculumSpec(game, levelNumber)
  }

  // ==================== PERSISTENT 500-LEVEL PROGRESS ENGINE ====================

  private fun progressKey(languageCode: String, gameId: String, userId: String = "default_learner"): String {
    val cleanLang = languageCode.lowercase()
    return if (userId.isBlank() || userId == "default_learner") {
      "lvl_prog_${cleanLang}_$gameId"
    } else {
      "lvl_prog_${userId.lowercase()}_${cleanLang}_$gameId"
    }
  }

  fun getGameProgress(
    context: Context,
    languageCode: String,
    gameId: String,
    userId: String = "default_learner"
  ): GameLevelProgressState {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(progressKey(languageCode, gameId, userId), null)
      ?: if (userId != "default_learner") prefs.getString(progressKey(languageCode, gameId, "default_learner"), null) else null
    if (raw.isNullOrBlank()) {
      return GameLevelProgressState(gameId = gameId, languageCode = languageCode, highestUnlockedLevel = 1)
    }
    return try {
      val obj = JSONObject(raw)
      val highest = obj.optInt("highestUnlockedLevel", 1).coerceIn(1, 500)
      val completedMap = mutableMapOf<Int, Int>()
      val starsObj = obj.optJSONObject("completedLevels")
      if (starsObj != null) {
        val keys = starsObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val lvl = k.toIntOrNull() ?: continue
          completedMap[lvl] = starsObj.optInt(k, 1).coerceIn(1, 3)
        }
      }
      val accMap = mutableMapOf<Int, Int>()
      val accObj = obj.optJSONObject("bestAccuracyByLevel")
      if (accObj != null) {
        val keys = accObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val lvl = k.toIntOrNull() ?: continue
          accMap[lvl] = accObj.optInt(k, 70).coerceIn(0, 100)
        }
      }
      val mistakes = mutableListOf<String>()
      val mArr = obj.optJSONArray("recentMistakeWords")
      if (mArr != null) {
        for (i in 0 until mArr.length()) {
          val w = mArr.optString(i).trim()
          if (w.isNotBlank()) mistakes.add(w)
        }
      }
      GameLevelProgressState(
        gameId = gameId,
        languageCode = languageCode,
        highestUnlockedLevel = highest,
        completedLevels = completedMap,
        bestAccuracyByLevel = accMap,
        recentMistakeWords = mistakes,
        lastPlayedEpochMs = obj.optLong("lastPlayedEpochMs", 0L)
      )
    } catch (_: Exception) {
      GameLevelProgressState(gameId = gameId, languageCode = languageCode, highestUnlockedLevel = 1)
    }
  }

  fun getNextGameLevel(
    context: Context,
    languageCode: String,
    gameId: String,
    userId: String = "default_learner"
  ): Int {
    return getGameProgress(context, languageCode, gameId, userId).highestUnlockedLevel.coerceIn(1, 500)
  }

  fun wasLevelCompletedBefore(
    context: Context,
    languageCode: String,
    gameId: String,
    levelNumber: Int,
    userId: String = "default_learner"
  ): Boolean {
    return getGameProgress(context, languageCode, gameId, userId)
      .completedLevels.containsKey(levelNumber.coerceIn(1, 500))
  }

  /**
   * Saves completed level results, awards 1–3 stars ONLY when accuracy >= 70%,
   * unlocks the next level up to 500, updates anti-repetition completion status,
   * and triggers background prefetch of `levelNumber + 1`.
   */
  fun saveGameProgress(
    context: Context,
    languageCode: String,
    gameId: String,
    levelNumber: Int,
    accuracyPercent: Int,
    missedWords: List<String> = emptyList(),
    wordsLearnedCount: Int = 0,
    category: UltimateGameCategory? = null,
    userId: String = "default_learner"
  ): GameLevelProgressState {
    val current = getGameProgress(context, languageCode, gameId, userId)
    val clampedLevel = levelNumber.coerceIn(1, 500)
    val clampedAcc = accuracyPercent.coerceIn(0, 100)
    val passed = clampedAcc >= 70
    val wasAlreadyCompleted = current.completedLevels.containsKey(clampedLevel)

    val starsEarned = when {
      clampedAcc >= 95 -> 3
      clampedAcc >= 80 -> 2
      clampedAcc >= 70 -> 1
      else -> 0
    }

    val updatedCompleted = current.completedLevels.toMutableMap()
    if (passed) {
      val prevStars = updatedCompleted[clampedLevel] ?: 0
      updatedCompleted[clampedLevel] = max(prevStars, starsEarned)
    }

    val updatedAcc = current.bestAccuracyByLevel.toMutableMap()
    val prevAcc = updatedAcc[clampedLevel] ?: 0
    updatedAcc[clampedLevel] = max(prevAcc, clampedAcc)

    val newHighest = if (passed && clampedLevel >= current.highestUnlockedLevel) {
      (clampedLevel + 1).coerceAtMost(500)
    } else {
      current.highestUnlockedLevel
    }

    val updatedMistakes = (missedWords + current.recentMistakeWords)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .take(25)

    val updated = GameLevelProgressState(
      gameId = gameId,
      languageCode = languageCode,
      highestUnlockedLevel = newHighest,
      completedLevels = updatedCompleted,
      bestAccuracyByLevel = updatedAcc,
      recentMistakeWords = updatedMistakes,
      lastPlayedEpochMs = System.currentTimeMillis()
    )

    try {
      val obj = JSONObject().apply {
        put("highestUnlockedLevel", updated.highestUnlockedLevel)
        put("lastPlayedEpochMs", updated.lastPlayedEpochMs)
        val starsJson = JSONObject()
        updated.completedLevels.forEach { (k, v) -> starsJson.put(k.toString(), v) }
        put("completedLevels", starsJson)
        val accJson = JSONObject()
        updated.bestAccuracyByLevel.forEach { (k, v) -> accJson.put(k.toString(), v) }
        put("bestAccuracyByLevel", accJson)
        val mArr = JSONArray()
        updated.recentMistakeWords.forEach { mArr.put(it) }
        put("recentMistakeWords", mArr)
      }
      val lastPlayedKey = if (userId.isBlank() || userId == "default_learner") {
        "last_played_game_${languageCode.lowercase()}"
      } else {
        "last_played_game_${userId.lowercase()}_${languageCode.lowercase()}"
      }
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(progressKey(languageCode, gameId, userId), obj.toString())
        .putString(lastPlayedKey, gameId)
        .apply()
    } catch (_: Exception) {}

    if (passed && !wasAlreadyCompleted) {
      recordDailyMissionProgress(
        context = context,
        category = category,
        wordsLearnedDelta = wordsLearnedCount.coerceAtLeast(2)
      )
    }

    val gameObj = ALL_70_GAMES.firstOrNull { it.id == gameId }
    if (passed && gameObj != null && clampedLevel < gameObj.maxLevels) {
      prefetchNextLevelInBackground(
        context = context,
        game = gameObj,
        languageCode = languageCode,
        completedLevel = clampedLevel,
        userId = userId
      )
    }

    GameEngineDiagnostics.log(
      "PROGRESS_SAVED",
      "game=$gameId lang=$languageCode lvl=$clampedLevel acc=$clampedAcc passed=$passed firstTime=${!wasAlreadyCompleted} unlocked=$newHighest"
    )

    return updated
  }

  fun getLastPlayedGame(
    context: Context,
    languageCode: String,
    userId: String = "default_learner"
  ): UltimateLanguageGame {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val scopedKey = "last_played_game_${userId.lowercase()}_${languageCode.lowercase()}"
    val lastId = prefs.getString(scopedKey, null)
      ?: prefs.getString("last_played_game_${languageCode.lowercase()}", null)
    return ALL_70_GAMES.firstOrNull { it.id == lastId } ?: ALL_70_GAMES.first()
  }

  fun prefetchNextLevelInBackground(
    context: Context,
    game: UltimateLanguageGame,
    languageCode: String,
    nativeLanguageCode: String = "en",
    completedLevel: Int,
    userId: String = "default_learner"
  ) {
    val nextLvl = (completedLevel + 1).coerceIn(1, game.maxLevels)
    if (nextLvl <= completedLevel) return
    val appCtx = context.applicationContext ?: context
    prefetchScope.launch {
      runCatching {
        GameEngineDiagnostics.log("PREFETCH_START", "game=${game.id} nextLvl=$nextLvl lang=$languageCode")
        generateGameQuestions(
          context = appCtx,
          game = game,
          languageCode = languageCode,
          nativeLanguageCode = nativeLanguageCode,
          levelNumber = nextLvl,
          forceRefresh = false,
          userId = userId
        )
      }
    }
  }

  // ==================== DAILY MISSIONS & REWARDS ====================

  private fun todayKey(): String {
    val dayNumber = System.currentTimeMillis() / (1000L * 60L * 60L * 24L)
    return "day_$dayNumber"
  }

  fun getDailyMissionsState(context: Context): DailyGameMissionsState {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val today = todayKey()
    val raw = prefs.getString("daily_missions_$today", null) ?: return DailyGameMissionsState(dateKey = today)
    return try {
      val obj = JSONObject(raw)
      val claimed = mutableSetOf<String>()
      val arr = obj.optJSONArray("claimedMissionIds")
      if (arr != null) {
        for (i in 0 until arr.length()) {
          claimed.add(arr.optString(i))
        }
      }
      DailyGameMissionsState(
        dateKey = today,
        vocabGamesCompleted = obj.optInt("vocabGamesCompleted", 0),
        speakingMinutesCompleted = obj.optInt("speakingMinutesCompleted", 0),
        listeningChallengesCompleted = obj.optInt("listeningChallengesCompleted", 0),
        newWordsLearned = obj.optInt("newWordsLearned", 0),
        claimedMissionIds = claimed
      )
    } catch (_: Exception) {
      DailyGameMissionsState(dateKey = today)
    }
  }

  fun recordDailyMissionProgress(
    context: Context,
    category: UltimateGameCategory?,
    wordsLearnedDelta: Int = 2
  ): DailyGameMissionsState {
    val current = getDailyMissionsState(context)
    val updated = current.copy(
      vocabGamesCompleted = current.vocabGamesCompleted + if (category == UltimateGameCategory.VOCABULARY || category == UltimateGameCategory.SPELLING) 1 else 0,
      speakingMinutesCompleted = current.speakingMinutesCompleted + if (category == UltimateGameCategory.SPEAKING || category == UltimateGameCategory.CONVERSATION) 1 else 0,
      listeningChallengesCompleted = current.listeningChallengesCompleted + if (category == UltimateGameCategory.LISTENING) 1 else 0,
      newWordsLearned = current.newWordsLearned + wordsLearnedDelta.coerceAtLeast(1)
    )
    saveDailyMissionsState(context, updated)
    return updated
  }

  fun claimDailyMissionReward(context: Context, missionId: String): DailyGameMissionsState {
    val current = getDailyMissionsState(context)
    if (current.claimedMissionIds.contains(missionId)) return current
    val updated = current.copy(claimedMissionIds = current.claimedMissionIds + missionId)
    saveDailyMissionsState(context, updated)
    return updated
  }

  private fun saveDailyMissionsState(context: Context, state: DailyGameMissionsState) {
    try {
      val obj = JSONObject().apply {
        put("vocabGamesCompleted", state.vocabGamesCompleted)
        put("speakingMinutesCompleted", state.speakingMinutesCompleted)
        put("listeningChallengesCompleted", state.listeningChallengesCompleted)
        put("newWordsLearned", state.newWordsLearned)
        val arr = JSONArray()
        state.claimedMissionIds.forEach { arr.put(it) }
        put("claimedMissionIds", arr)
      }
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("daily_missions_${state.dateKey}", obj.toString())
        .apply()
    } catch (_: Exception) {}
  }

  // ==================== SMART GAME RECOMMENDATIONS ====================

  /**
   * Returns 3 concise personalized game recommendations based on mistakes, language, and history.
   */
  fun getSmartRecommendations(
    context: Context,
    languageCode: String,
    mistakeEntries: List<MistakeNotebookEntry>,
    userId: String = "default_learner"
  ): List<Pair<UltimateLanguageGame, String>> {
    val results = mutableListOf<Pair<UltimateLanguageGame, String>>()
    val mistakeTypes = mistakeEntries.map { it.category.lowercase() }

    val spellBee = ALL_70_GAMES.first { it.id == "ug_16_spellbee" }
    val sentenceBuilder = ALL_70_GAMES.first { it.id == "ug_42_sentence_builder" }
    val listeningQuiz = ALL_70_GAMES.first { it.id == "ug_34_listen_and_choose" }
    val speechCoach = ALL_70_GAMES.first { it.id == "ug_29_speech_coach_challenge" }
    val pictureQuiz = ALL_70_GAMES.first { it.id == "ug_01_picture_word_quiz" }
    val wordigo = ALL_70_GAMES.first { it.id == "ug_17_wordigo" }
    val shoppingMission = ALL_70_GAMES.first { it.id == "ug_52_shopping_mission" }

    if (mistakeTypes.any { it.contains("spell") || it.contains("vocab") }) {
      results.add(spellBee to "Strengthen spelling accuracy")
    }
    if (mistakeTypes.any { it.contains("grammar") || it.contains("word order") || it.contains("sentence") }) {
      results.add(sentenceBuilder to "Build natural sentences")
    }
    if (mistakeTypes.any { it.contains("listen") || it.contains("ear") }) {
      results.add(listeningQuiz to "Sharpen spoken comprehension")
    }
    if (mistakeTypes.any { it.contains("pronun") || it.contains("speak") }) {
      results.add(speechCoach to "Practice clear pronunciation")
    }

    val defaults = listOf(
      pictureQuiz to "Visual everyday vocabulary",
      sentenceBuilder to "Build sentences step by step",
      spellBee to "Listen and spell key words",
      wordigo to "Deduce the secret word in 6 tries",
      shoppingMission to "Practice real-life dialogues"
    )
    for (item in defaults) {
      if (results.size >= 3) break
      if (results.none { it.first.id == item.first.id }) {
        results.add(item)
      }
    }
    return results.take(3)
  }

  // ==================== DYNAMIC AI GENERATION WITH MECHANIC SCHEMAS & ANTI-DUPLICATION ====================

  private fun buildCacheKey(userId: String, gameId: String, languageCode: String, levelNumber: Int): String {
    val safeUser = userId.ifBlank { "default_learner" }.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
    return "${ENGINE_SCHEMA_VERSION}_${safeUser}_${gameId}_${languageCode.lowercase()}_lvl_$levelNumber"
  }

  /**
   * Primary entry point: retrieves cached or dynamically generated exercises for a specific
   * `(userId, game, languageCode, levelNumber)` in the 1..500 progression system.
   * Ensures every level has a unique curriculum objective and zero repeated consecutive exercises.
   */
  suspend fun generateGameQuestions(
    context: Context,
    game: UltimateLanguageGame,
    languageCode: String,
    nativeLanguageCode: String = "en",
    levelNumber: Int,
    forceRefresh: Boolean = false,
    userId: String = "default_learner"
  ): List<DynamicLevelExercise> = withContext(Dispatchers.IO) {
    val clampedLevel = levelNumber.coerceIn(1, 500)
    val cacheKey = buildCacheKey(userId, game.id, languageCode, clampedLevel)

    if (!forceRefresh) {
      memoryExerciseCache[cacheKey]?.takeIf { list ->
        list.isNotEmpty() && (
          game.mechanic != GameInteractionMechanic.WORD_SEARCH_GRID ||
            list.all { WordSearchDifficultyAndTopicEngine.validateWordSearchExercise(it, clampedLevel) }
          )
      }?.let {
        GameEngineDiagnostics.log("CACHE_HIT_MEMORY", "key=$cacheKey count=${it.size}")
        return@withContext it
      }
      loadCachedLevelExercises(context, cacheKey, languageCode)?.takeIf { list ->
        list.isNotEmpty() && (
          game.mechanic != GameInteractionMechanic.WORD_SEARCH_GRID ||
            list.all { WordSearchDifficultyAndTopicEngine.validateWordSearchExercise(it, clampedLevel) }
          )
      }?.let {
        memoryExerciseCache[cacheKey] = it
        GameEngineDiagnostics.log("CACHE_HIT_DISK", "key=$cacheKey count=${it.size}")
        return@withContext it
      }
    }

    GameEngineDiagnostics.log("CACHE_MISS", "key=$cacheKey generating fresh exercises")
    val spec = GameCurriculumAndAntiRepetitionEngine.getCurriculumSpec(game, clampedLevel)
    val baseExcludedSet = GameCurriculumAndAntiRepetitionEngine.getExcludedContentForLevel(
      context = context,
      userId = userId,
      languageCode = languageCode,
      gameId = game.id,
      levelNumber = clampedLevel
    )
    val excludedSet = if (game.mechanic == GameInteractionMechanic.WORD_SEARCH_GRID) {
      baseExcludedSet + WordSearchDifficultyAndTopicEngine.getUsedWordSearchWords(context, userId, languageCode)
    } else {
      baseExcludedSet
    }
    val progress = getGameProgress(context, languageCode, game.id, userId)

    val targetCount = when (game.mechanic) {
      GameInteractionMechanic.SPELLING_INPUT -> 8
      GameInteractionMechanic.LISTEN_AND_TYPE -> 6
      GameInteractionMechanic.WORD_SEARCH_GRID -> 1 // 1 rich 8x8 board containing 5 hidden target words
      GameInteractionMechanic.MEMORY_CARD_FLIP -> 2 // 2 memory boards x 6 pairs = 12 pairs
      GameInteractionMechanic.WORD_MATCH_PAIRS -> 2 // 2 match boards x 6 pairs = 12 pairs
      GameInteractionMechanic.AUDIO_WORD_MATCH -> 2 // 2 audio-match boards x 5 spoken pairs
      GameInteractionMechanic.WORDIGO_GRID -> 2     // 2 full 6-try Wordigo puzzles per level
      GameInteractionMechanic.ESCAPE_ROOM_PUZZLE -> 4 // 4 connected Escape Room stages
      GameInteractionMechanic.SPEAKING_MIC,
      GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE,
      GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT,
      GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE,
      GameInteractionMechanic.SPEAKING_QUICK_ANSWER,
      GameInteractionMechanic.SPEAKING_SPEECH_COACH,
      GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT,
      GameInteractionMechanic.SPEAKING_SHADOWING,
      GameInteractionMechanic.SPEAKING_DIALOGUE_LINE,
      GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION -> 5
      GameInteractionMechanic.LISTENING_COMPREHENSION,
      GameInteractionMechanic.SOUND_DETECTIVE,
      GameInteractionMechanic.LISTENING_STORY_QUIZ,
      GameInteractionMechanic.MISSING_AUDIO_WORD,
      GameInteractionMechanic.CONVERSATION_LISTENING,
      GameInteractionMechanic.FAST_SPEECH_CHALLENGE -> 5
      GameInteractionMechanic.AI_CONVERSATION_MISSION -> 6
      else -> 6
    }

    val expandedExcluded = GameCurriculumAndAntiRepetitionEngine.expandExcludedWithSemanticKeys(excludedSet)

    fun filterUniqueExercises(
      candidates: List<DynamicLevelExercise>,
      seenPhrases: MutableSet<String>,
      seenConcepts: MutableSet<String>,
      seenSemantic: MutableSet<String>,
      seenPromptSigs: MutableSet<String>
    ): List<DynamicLevelExercise> {
      var rejectedDuplicates = 0
      val accepted = candidates.filter { ex ->
        if (!validateExerciseIntegrity(ex)) {
          GameEngineDiagnostics.log("INTEGRITY_REJECTED", "game=${game.id} id=${ex.id}")
          return@filter false
        }
        val key = ex.targetPhrase.trim().lowercase()
        val conceptKey = ex.visualConceptId.trim().lowercase()
        val meaningKey = ex.translation.trim().lowercase()
        val canonPhrase = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(ex.targetPhrase)
        val canonMeaning = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(ex.translation)
        val promptSig = GameCurriculumAndAntiRepetitionEngine.promptMeaningSignature(ex.prompt)

        val dupInHistory = GameCurriculumAndAntiRepetitionEngine.isDuplicateContent(
          targetPhrase = ex.targetPhrase,
          englishMeaning = ex.translation,
          conceptId = ex.visualConceptId,
          prompt = ex.prompt,
          excludedSet = expandedExcluded
        )
        val dupInLevel = key.isBlank() ||
          key in seenPhrases ||
          (conceptKey.isNotBlank() && conceptKey in seenConcepts) ||
          (canonPhrase.isNotBlank() && canonPhrase in seenSemantic) ||
          (canonMeaning.isNotBlank() && canonMeaning in seenSemantic && game.mechanic == GameInteractionMechanic.PICTURE_IDENTIFICATION) ||
          (promptSig.isNotBlank() && promptSig in seenPromptSigs)

        if (dupInHistory || dupInLevel) {
          rejectedDuplicates++
          false
        } else {
          seenPhrases.add(key)
          if (conceptKey.isNotBlank()) seenConcepts.add(conceptKey)
          if (canonPhrase.isNotBlank()) seenSemantic.add(canonPhrase)
          if (canonMeaning.isNotBlank() && game.mechanic == GameInteractionMechanic.PICTURE_IDENTIFICATION) {
            seenSemantic.add(canonMeaning)
          }
          if (promptSig.isNotBlank()) seenPromptSigs.add(promptSig)
          if (meaningKey.isNotBlank()) seenPhrases.add("m:$meaningKey")
          true
        }
      }
      if (rejectedDuplicates > 0) {
        GameEngineDiagnostics.log("DUPLICATE_REJECTED", "game=${game.id} lvl=$clampedLevel rejected=$rejectedDuplicates")
      }
      return accepted
    }

    val rawAiExercises = generateMechanicSpecificWithGemini(
      context = context,
      game = game,
      spec = spec,
      languageCode = languageCode,
      nativeLanguageCode = nativeLanguageCode,
      targetCount = targetCount,
      excludedSet = expandedExcluded,
      recentMistakes = progress.recentMistakeWords,
      userId = userId
    )

    val seenInLevel = mutableSetOf<String>()
    val seenConceptsInLevel = mutableSetOf<String>()
    val seenSemanticInLevel = mutableSetOf<String>()
    val seenPromptSigsInLevel = mutableSetOf<String>()
    val acceptedAi = filterUniqueExercises(
      candidates = rawAiExercises,
      seenPhrases = seenInLevel,
      seenConcepts = seenConceptsInLevel,
      seenSemantic = seenSemanticInLevel,
      seenPromptSigs = seenPromptSigsInLevel
    ).toMutableList()

    // If Gemini returned exercises but duplicates trimmed them below targetCount, do 1 targeted top-up call
    if (rawAiExercises.isNotEmpty() && acceptedAi.size < targetCount) {
      val extraNeeded = targetCount - acceptedAi.size
      val retryExcluded = expandedExcluded + seenInLevel + seenConceptsInLevel + seenSemanticInLevel
      val retryAi = generateMechanicSpecificWithGemini(
        context = context,
        game = game,
        spec = spec,
        languageCode = languageCode,
        nativeLanguageCode = nativeLanguageCode,
        targetCount = extraNeeded,
        excludedSet = retryExcluded,
        recentMistakes = progress.recentMistakeWords,
        userId = userId
      )
      val acceptedRetry = filterUniqueExercises(
        candidates = retryAi,
        seenPhrases = seenInLevel,
        seenConcepts = seenConceptsInLevel,
        seenSemantic = seenSemanticInLevel,
        seenPromptSigs = seenPromptSigsInLevel
      )
      acceptedAi.addAll(acceptedRetry)
    }

    val finalExercises = if (acceptedAi.size >= targetCount) {
      acceptedAi.take(targetCount)
    } else {
      val fallback = buildDistinctCuratedLevelExercises(
        game = game,
        spec = spec,
        languageCode = languageCode,
        nativeLanguageCode = nativeLanguageCode,
        targetCount = targetCount,
        excludedSet = expandedExcluded + seenInLevel + seenConceptsInLevel + seenSemanticInLevel + seenPromptSigsInLevel,
        context = context,
        userId = userId
      ).filter { validateExerciseIntegrity(it) }
      (acceptedAi + fallback)
        .distinctBy { "${it.targetPhrase.trim().lowercase()}|${it.visualConceptId.trim().lowercase()}" }
        .take(targetCount)
    }

    GameCurriculumAndAntiRepetitionEngine.recordLevelHistory(
      context = context,
      userId = userId,
      languageCode = languageCode,
      gameId = game.id,
      levelNumber = clampedLevel,
      spec = spec,
      exercises = finalExercises,
      completed = progress.completedLevels.containsKey(clampedLevel)
    )

    memoryExerciseCache[cacheKey] = finalExercises
    saveCachedLevelExercises(context, cacheKey, finalExercises)
    finalExercises
  }

  /**
   * Generates mechanic-specific exercises via Gemini using tailored JSON schemas per game type.
   */
  private suspend fun generateMechanicSpecificWithGemini(
    context: Context? = null,
    game: UltimateLanguageGame,
    spec: LevelCurriculumSpec,
    languageCode: String,
    nativeLanguageCode: String,
    targetCount: Int,
    excludedSet: Set<String>,
    recentMistakes: List<String>,
    userId: String = "default_learner"
  ): List<DynamicLevelExercise> {
    val langName = Language.findByCode(languageCode).name
    val nativeName = Language.findByCode(nativeLanguageCode).name
    val cleanAvoidList = excludedSet
      .filter { !it.startsWith("sem:") && !it.startsWith("qsig:") && !it.startsWith("dyn:") && !it.startsWith("m:") }
      .take(70)
    val avoidClause = if (cleanAvoidList.isNotEmpty()) {
      "CRITICAL ANTI-REPETITION RULE: DO NOT reuse any of these previously practiced words, synonyms, concepts, or questions: ${cleanAvoidList.joinToString(", ")}."
    } else ""
    val mistakeClause = if (recentMistakes.isNotEmpty()) {
      "Optionally reinforce concepts related to learner weak spots: ${recentMistakes.take(3).joinToString(", ")}."
    } else ""

    val schemaInstructions = when (game.mechanic) {
      GameInteractionMechanic.SPELLING_INPUT -> """
        Generate $targetCount distinct $langName words for audio spelling practice.
        Level Objective: "${spec.objectiveTitle}" (${spec.band.bandName} / ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"targetPhrase":"word in $langName","translation":"meaning in $nativeName","phonetic":"pronunciation","hint":"brief meaning/first-letter clue","explanation":"example sentence in $langName"}]
      """.trimIndent()

      GameInteractionMechanic.WORDIGO_GRID -> """
        Generate $targetCount single-word secret target words (4 to 6 letters only, alphabetic, no spaces) in $langName for a 6-try Wordle-style letter-position game.
        Level Objective: "${spec.objectiveTitle}" (${spec.band.bandName}).
        Return ONLY a JSON array of $targetCount objects:
        [{"targetPhrase":"5-letter word in $langName","translation":"clue/meaning in $nativeName","phonetic":"pronunciation","hint":"meaning clue","explanation":"usage example in $langName"}]
      """.trimIndent()

      GameInteractionMechanic.WORD_MATCH_PAIRS,
      GameInteractionMechanic.MEMORY_CARD_FLIP -> """
        Generate $targetCount boards of 6 distinct vocabulary pairs each in $langName and $nativeName.
        Level Objective: "${spec.objectiveTitle}" (${spec.band.bandName}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Match all 6 ${spec.objectiveTitle} pairs","pairs":[{"left":"word in $langName","right":"meaning in $nativeName"}],"explanation":"summary note"}]
      """.trimIndent()

      GameInteractionMechanic.WORD_SEARCH_GRID -> {
        val wsSpec = WordSearchDifficultyAndTopicEngine.specForLevel(spec.levelNumber)
        val requestWordCount = wsSpec.wordCount + 4
        """
          Generate $requestWordCount strictly topic-relevant single-word vocabulary items (3 to ${wsSpec.gridSize} alphabetic characters, no spaces, hyphens, or numbers) in $langName for a Level ${spec.levelNumber} (${wsSpec.tier.label}) Word Search grid strictly focused on "${spec.objectiveTitle}".
          IMPORTANT: Every single word MUST belong authentically to "${spec.objectiveTitle}" — never include unrelated words (such as transport/money/office words in a health lesson).
          Return ONLY a JSON array of 1 object:
          [{"prompt":"Find all ${wsSpec.wordCount} hidden words for ${spec.objectiveTitle}","pairs":[{"left":"WORD","right":"short clue in $nativeName"}],"explanation":"thematic vocabulary summary"}]
        """.trimIndent()
      }

      GameInteractionMechanic.GRAMMAR_REPAIR -> """
        Generate $targetCount grammar repair exercises in $langName for Level ${spec.levelNumber} ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Each item MUST include an incorrect sentence with ONE clear grammar/spelling error and its corrected version.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Tap or edit the mistake to repair the sentence","incorrectSentence":"flawed sentence in $langName","errorWord":"the wrong word","correctedWord":"the fixed word","targetPhrase":"complete corrected sentence in $langName","translation":"meaning in $nativeName","hint":"grammar rule clue","explanation":"why the correction is needed"}]
      """.trimIndent()

      GameInteractionMechanic.SCRAMBLE_TILES -> """
        Generate $targetCount distinct, natural sentences in $langName (${spec.targetWordCountRange.first} to ${spec.targetWordCountRange.last} words) for Sentence Builder.
        Exact Level Objective: "${spec.objectiveTitle}" (${spec.band.bandName} / ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Arrange the words to express this in $langName","targetPhrase":"complete natural sentence in $langName","translation":"meaning in $nativeName","phonetic":"pronunciation","hint":"word order tip","explanation":"grammar structure note"}]
      """.trimIndent()

      GameInteractionMechanic.AI_CONVERSATION_MISSION -> """
        Generate $targetCount sequential interactive dialogue turns for "${game.title}" (${spec.objectiveTitle}, Level ${spec.levelNumber}, ${spec.band.cefrEquivalent}).
        "conversationStarter" MUST be what the ${game.title} partner says first in $langName.
        "targetPhrase" is ONE example natural response in $langName (used only for guidance/evaluation, never sent automatically).
        Return ONLY a JSON array of $targetCount objects:
        [{"conversationStarter":"partner line in $langName","prompt":"Native translation of partner line + communicative goal in $nativeName","targetPhrase":"example good reply in $langName","translation":"meaning of example reply in $nativeName","hint":"helpful phrase idea","explanation":"cultural/conversational tip"}]
      """.trimIndent()

      GameInteractionMechanic.ESCAPE_ROOM_PUZZLE -> """
        Generate 4 connected Language Escape Room lock puzzles in $langName for Level ${spec.levelNumber} ("${spec.objectiveTitle}").
        Each stage unlocks 1 code symbol. Return ONLY a JSON array of 4 objects:
        [{"prompt":"Escape Stage riddle or clue","targetPhrase":"correct answer in $langName","translation":"meaning in $nativeName","options":["correct answer","distractor 1","distractor 2","distractor 3"],"codeSymbol":"A7","hint":"clue","explanation":"why this unlocks the door"}]
      """.trimIndent()

      GameInteractionMechanic.AUDIO_WORD_MATCH -> """
        Generate $targetCount boards of 5 spoken vocabulary pairs in $langName and $nativeName for Audio Word Match.
        Level Objective: "${spec.objectiveTitle}" (${spec.band.bandName}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Tap each audio clip and match it with its meaning","pairs":[{"left":"spoken word in $langName","right":"meaning in $nativeName"}],"explanation":"summary note"}]
      """.trimIndent()

      GameInteractionMechanic.LISTEN_AND_TYPE -> """
        Generate $targetCount spoken dictation expressions in $langName for Listen & Type ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Listen carefully and type exactly what you hear","targetPhrase":"spoken word or short phrase in $langName","translation":"meaning in $nativeName","phonetic":"pronunciation","hint":"first-word or meaning clue","explanation":"usage note"}]
      """.trimIndent()

      GameInteractionMechanic.SOUND_DETECTIVE -> """
        Generate $targetCount minimal-pair / phonetic ear-training items in $langName for Sound Detective ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Listen to the Mystery Clip and pick the exact word spoken","targetPhrase":"correct spoken word in $langName","contrastPhrase":"close-sounding contrast word in $langName","translation":"meaning of targetPhrase in $nativeName","correctOption":"exact same string as targetPhrase","options":["correct spoken word in $langName","close-sounding contrast word","distractor 2","distractor 3"],"hint":"listen to the vowel/consonant contrast","explanation":"phonetic distinction note"}]
      """.trimIndent()

      GameInteractionMechanic.LISTENING_STORY_QUIZ -> """
        Generate $targetCount 2-sentence mini-stories in $langName with comprehension questions for Listening Story Quiz ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"comprehension question in $nativeName about the spoken story","storyPassage":"2-sentence mini story in $langName","targetPhrase":"key sentence from story in $langName","translation":"correct comprehension answer in $nativeName","correctOption":"exact correct comprehension answer","options":["exact correct comprehension answer","plausible distractor 1","plausible distractor 2","plausible distractor 3"],"hint":"story detail clue","explanation":"why this answer matches the spoken story"}]
      """.trimIndent()

      GameInteractionMechanic.MISSING_AUDIO_WORD -> """
        Generate $targetCount cloze listening items in $langName for Key Audio Detail ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        "fullSpokenSentence" is spoken aloud in $langName; "clozeMaskedSentence" replaces ONE key word (`targetPhrase`) with "[ _____ ]".
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Listen to the full sentence and select the missing word","fullSpokenSentence":"complete sentence in $langName","clozeMaskedSentence":"sentence with [ _____ ] in $langName","targetPhrase":"the exact missing word in $langName","translation":"meaning of full sentence in $nativeName","correctOption":"the exact missing word in $langName","options":["the exact missing word in $langName","distractor word 1","distractor word 2","distractor word 3"],"hint":"meaning of the missing word","explanation":"complete sentence breakdown"}]
      """.trimIndent()

      GameInteractionMechanic.CONVERSATION_LISTENING -> """
        Generate $targetCount 2-speaker short audio dialogues in $langName for Dialogue Listening ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Listen to Speaker A and Speaker B. What does Speaker B mean?","speakerA":"Speaker A line in $langName","speakerB":"Speaker B reply in $langName","targetPhrase":"Speaker B reply in $langName","translation":"correct meaning of Speaker B in $nativeName","correctOption":"exact correct meaning in $nativeName","options":["exact correct meaning in $nativeName","distractor 1","distractor 2","distractor 3"],"hint":"dialogue context clue","explanation":"full dialogue translation"}]
      """.trimIndent()

      GameInteractionMechanic.LISTENING_COMPREHENSION,
      GameInteractionMechanic.FAST_SPEECH_CHALLENGE -> """
        Generate $targetCount spoken listening comprehension items in $langName for "${game.title}" ("${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        IMPORTANT: "targetPhrase" will be spoken via TTS audio and HIDDEN from text until answered. "prompt" asks a comprehension question in $nativeName without giving away the answer!
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Listen to the audio. What is the speaker saying?","targetPhrase":"spoken sentence in $langName","translation":"meaning in $nativeName","correctOption":"exact correct comprehension answer in $nativeName","options":["exact correct comprehension answer in $nativeName","plausible distractor 1","plausible distractor 2","plausible distractor 3"],"hint":"listening clue","explanation":"full breakdown"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_MIC -> """
        Generate $targetCount individual word pronunciation items in $langName for "Pronunciation Practice" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Expected response type: Single word with tricky sound focus and syllable breakdown.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Pronounce this word clearly","targetPhrase":"single target word in $langName","translation":"meaning in $nativeName","phonetic":"IPA/phonetic transcription","clozeMaskedSentence":"difficult sound focus (e.g. rolled /rr/, vowel contrast)","options":["syl1","syl2"],"hint":"articulation tip","explanation":"example sentence in $langName"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE -> """
        Generate $targetCount complete, natural spoken sentences in $langName (${spec.targetWordCountRange.first.coerceAtLeast(4)} to ${spec.targetWordCountRange.last.coerceAtLeast(6)} words) for "Repeat After Me" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        CRITICAL: NEVER generate isolated single words! Every item MUST be a complete conversational sentence.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Listen to the complete sentence and repeat it aloud","targetPhrase":"complete natural sentence in $langName","translation":"meaning in $nativeName","phonetic":"pronunciation guide","hint":"rhythm & linking tip","explanation":"grammar/usage note"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT -> """
        Generate $targetCount articulation exercises / tongue twisters in $langName for "Articulation Sprint" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Expected response type: Short articulation drill or tongue twister focusing on specific consonant combinations.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Articulation Sprint drill","targetPhrase":"tongue twister or consonant cluster phrase in $langName","translation":"meaning and target sound in $nativeName","phonetic":"phonetic guide","clozeMaskedSentence":"target sound combination (e.g. /r/ vs /l/, /s/ vs /sh/)","hint":"tongue & lip placement tip","explanation":"why this drill improves clarity"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE -> """
        Generate $targetCount visual scene description exercises in $langName for "Speak the Scene" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Each item asks a question about a real-world scene and provides key vocabulary words plus an example descriptive sentence.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Question about what is happening in the scene in $langName","targetPhrase":"complete model description sentence in $langName","translation":"English meaning of the model description","phonetic":"Scene title","options":["key_word_1","key_word_2","key_word_3","key_word_4"],"hint":"helpful vocabulary to describe the picture","explanation":"why this description works well"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_QUICK_ANSWER -> """
        Generate $targetCount practical everyday questions in $langName for "Quick Answer" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        CRITICAL: "prompt" MUST be an everyday question in $langName (e.g. "What do you usually eat for breakfast?"), and "targetPhrase" MUST be a natural sample answer in $langName (NEVER the question itself!).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"practical everyday question in $langName","speakerA":"same question in $langName","targetPhrase":"natural sample answer in $langName","translation":"question meaning in $nativeName","phonetic":"sample answer meaning in $nativeName","options":["useful_word_1","useful_word_2","useful_word_3","useful_word_4"],"hint":"answer idea in $nativeName","explanation":"grammar tip for answering this question"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_SPEECH_COACH -> """
        Generate $targetCount open/guided speaking prompts in $langName for "Speech Coach" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"speaking prompt inviting the learner to form a complete sentence in $langName","targetPhrase":"polished model sentence in $langName","translation":"meaning in $nativeName","phonetic":"focus vocabulary word in $langName","clozeMaskedSentence":"pronunciation focus note","hint":"grammar & structure tip","explanation":"why this phrasing sounds natural"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT -> """
        Generate $targetCount continuous speaking topic prompts in $langName for "Fluency Sprint" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        CRITICAL: NEVER generate isolated words. Each item is an everyday topic for 15s–60s continuous speaking with 3 talking points in "options".
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Everyday speaking topic prompt in $langName","targetPhrase":"2-sentence model fluent response in $langName","translation":"model response meaning in $nativeName","phonetic":"Short topic title","options":["Talking point 1","Talking point 2","Talking point 3"],"hint":"connectors to use while speaking","explanation":"fluency coaching tip"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_SHADOWING -> """
        Generate $targetCount natural cadence sentences in $langName (5 to 12 words) for "Shadow Speaking" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Shadow this sentence simultaneously with the speaker","targetPhrase":"complete natural sentence in $langName","translation":"meaning in $nativeName","phonetic":"rhythm & stress guide","hint":"connected speech note","explanation":"prosody breakdown"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_DIALOGUE_LINE -> """
        Generate $targetCount 2-speaker conversation exchanges in $langName for "Dialogue Line" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        CRITICAL: "speakerA" is what Speaker A says first in $langName; "targetPhrase" is a complete natural reply by the learner (NEVER a single word); "speakerB" is Speaker A's follow-up continuation line.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Communicative goal for your reply in $nativeName","speakerA":"Speaker A opening line in $langName","targetPhrase":"model natural reply in $langName","speakerB":"Speaker A follow-up continuation line in $langName","translation":"meaning of model reply in $nativeName","clozeMaskedSentence":"meaning of Speaker A line in $nativeName","options":["model reply","alternative valid reply 1","alternative valid reply 2"],"hint":"polite expression tip","explanation":"conversation flow note"}]
      """.trimIndent()

      GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION -> """
        Generate $targetCount interactive roleplay mission scenarios in $langName for "Roleplay Mission" (Level ${spec.levelNumber}, "${spec.objectiveTitle}", ${spec.band.cefrEquivalent}).
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"Scenario title & mission brief in $nativeName","conversationStarter":"Partner opening line in $langName","speakerA":"Partner opening line in $langName","targetPhrase":"example complete learner response in $langName","speakerB":"Partner second turn in $langName","translation":"meaning of partner opening in $nativeName","options":["Objective 1","Objective 2","Objective 3"],"hint":"helpful phrase for this mission","explanation":"cultural & situational tip"}]
      """.trimIndent()

      GameInteractionMechanic.PICTURE_IDENTIFICATION -> {
        val topicConcepts = VerifiedVisualConceptRegistry.getConceptsForTopic(spec.objectiveTitle, spec.levelNumber)
        val curatedSampleIds = topicConcepts.take(8).joinToString(", ") { it.conceptId }
        val progressionTier = GameCurriculumAndAntiRepetitionEngine.getVocabularyCurriculumBandDescription(spec.levelNumber)
        """
          Generate $targetCount fresh visual vocabulary quiz questions for "${game.title}" (Level ${spec.levelNumber}, Theme: "${spec.objectiveTitle}", Tier: "$progressionTier", CEFR: ${spec.band.cefrEquivalent}).
          Target Language: $langName ($languageCode). Learner Native Language: $nativeName ($nativeLanguageCode).
          CRITICAL QUALITY & IMAGE VERIFICATION RULES:
          1. Pick concrete, clearly photographable objects, foods, places, animals, instruments, tools, vehicles, or real-world items appropriate for Level ${spec.levelNumber} ("${spec.objectiveTitle}"). Do NOT repeat common Level 1 words or anything in the avoid list.
          2. Provide "englishConcept" as an exact 1–2 word concrete English noun (so a verified exact photograph of that object can be fetched), plus "imageCategory". If matching one of these optional curated concepts [$curatedSampleIds], you may set "visualConceptId"; otherwise leave "visualConceptId" as "".
          3. NEVER spoil or reveal the answer word inside "prompt"!
          4. "targetPhrase" and "correctOption" must be the exact same answer string in $langName.
          5. "options" MUST have 4 distinct, non-duplicated, non-synonymous choices in $langName from the same semantic theme.
          Return ONLY a JSON array of $targetCount objects:
          [{"englishConcept":"exact 1-2 word English noun","imageCategory":"${spec.objectiveTitle}","visualConceptId":"","prompt":"clear question in $nativeName without revealing the answer","targetPhrase":"exact answer in $langName","translation":"English meaning","phonetic":"pronunciation","correctOption":"exact same string as targetPhrase","options":["exact same string as targetPhrase","distinct distractor 1","distinct distractor 2","distinct distractor 3"],"hint":"helpful clue","explanation":"concise example & explanation"}]
        """.trimIndent()
      }


      else -> """
        Generate $targetCount distinct exercises for "${game.title}" on "${spec.objectiveTitle}" (Level ${spec.levelNumber} of 500, ${spec.band.bandName} / ${spec.band.cefrEquivalent}).
        Target Language: $langName ($languageCode), Explanation Language: $nativeName.
        CRITICAL VALIDATION RULE: "correctOption" MUST exactly match the true answer to "prompt" and MUST appear in "options" alongside 3 distinct wrong distractors.
        Return ONLY a JSON array of $targetCount objects:
        [{"prompt":"clear question","targetPhrase":"verified correct answer in $langName","translation":"meaning in $nativeName","phonetic":"pronunciation","correctOption":"verified correct answer in $langName","options":["verified correct answer in $langName","distractor 1","distractor 2","distractor 3"],"hint":"helpful clue","explanation":"concise explanation"}]
      """.trimIndent()
    }

    val progressionBandGuide = GameCurriculumAndAntiRepetitionEngine.getVocabularyCurriculumBandDescription(spec.levelNumber)
    val fullPrompt = "$schemaInstructions\nCurriculum Progression Tier: $progressionBandGuide.\n$avoidClause\n$mistakeClause"
    val rawJson = GeminiTutorService.generateStructuredJson(fullPrompt)
    if (rawJson.isNullOrBlank()) {
      GameEngineDiagnostics.log("GEMINI_FALLBACK", "game=${game.id} lvl=${spec.levelNumber} reason=empty_or_offline")
      return emptyList()
    }

    val parsed = parseMechanicSpecificJson(rawJson, game, spec, languageCode, context, userId)
    GameEngineDiagnostics.log("GEMINI_PARSED", "game=${game.id} lvl=${spec.levelNumber} parsed=${parsed.size}")
    return parsed
  }

  /**
   * Builds 4 validated, distinct options with content-bound stable IDs BEFORE rotating
   * positions, guaranteeing that `correctOptionId` and `correctIndex` always point to
   * `verifiedCorrectText` regardless of option order in raw JSON or rotation position A/B/C/D.
   */
  fun buildValidatedAnswerOptions(
    exerciseId: String,
    verifiedCorrectText: String,
    candidateOptions: List<String>,
    fallbackDistractors: List<String>,
    rotationSeed: Int
  ): Triple<List<ValidatedAnswerOption>, String, Int> {
    val cleanCorrect = verifiedCorrectText.trim()
    val correctCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(cleanCorrect)
    val distractors = (candidateOptions + fallbackDistractors)
      .map { it.trim() }
      .filter { opt ->
        if (opt.isBlank() || opt.equals(cleanCorrect, ignoreCase = true)) return@filter false
        val optCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(opt)
        optCanon.isBlank() || correctCanon.isBlank() || optCanon != correctCanon
      }
      .distinctBy { opt ->
        val canon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(opt)
        canon.ifBlank { opt.lowercase() }
      }
      .take(3)
      .toMutableList()

    val emergencyPool = listOf("Window", "Chair", "Book", "Kettle", "Jacket", "Train", "Apple", "Camera")
    for (em in emergencyPool) {
      if (distractors.size >= 3) break
      val emCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(em)
      if (!em.equals(cleanCorrect, ignoreCase = true) &&
        emCanon != correctCanon &&
        distractors.none { it.equals(em, ignoreCase = true) || GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(it) == emCanon }
      ) {
        distractors.add(em)
      }
    }

    val unshuffled = listOf(cleanCorrect) + distractors.take(3)
    val taggedBeforeRotation = unshuffled.mapIndexed { origIdx, optText ->
      ValidatedAnswerOption(
        id = "${exerciseId}_ans_${origIdx}_${abs(optText.lowercase().hashCode()) % 99991}",
        text = optText
      )
    }
    val verifiedCorrectId = taggedBeforeRotation.first().id

    val size = taggedBeforeRotation.size
    val shift = abs(rotationSeed) % size
    val rotated = List(size) { pos ->
      taggedBeforeRotation[(pos + shift) % size]
    }
    val verifiedCorrectIdx = rotated.indexOfFirst { it.id == verifiedCorrectId }.coerceAtLeast(0)
    return Triple(rotated, verifiedCorrectId, verifiedCorrectIdx)
  }

  /**
   * Deterministically verifies that an exercise has a clear prompt, 4 distinct non-empty options,
   * an unambiguous `correctOptionId` matching the true answer, and (for visual games) a verified
   * `visualConceptId` that genuinely depicts the answer.
   */
  fun validateExerciseIntegrity(ex: DynamicLevelExercise): Boolean {
    if (ex.prompt.isBlank() || ex.targetPhrase.isBlank()) return false

    val multipleChoiceMechanics = setOf(
      GameInteractionMechanic.PICTURE_IDENTIFICATION,
      GameInteractionMechanic.LISTENING_COMPREHENSION,
      GameInteractionMechanic.SOUND_DETECTIVE,
      GameInteractionMechanic.LISTENING_STORY_QUIZ,
      GameInteractionMechanic.MISSING_AUDIO_WORD,
      GameInteractionMechanic.CONVERSATION_LISTENING,
      GameInteractionMechanic.FAST_SPEECH_CHALLENGE,
      GameInteractionMechanic.GRAMMAR_CORRECTION,
      GameInteractionMechanic.BRAIN_PUZZLE_CHALLENGE,
      GameInteractionMechanic.ESCAPE_ROOM_PUZZLE,
      GameInteractionMechanic.SPEED_RUN_TIMER,
      GameInteractionMechanic.FLASHCARD_FLIP
    )

    if (ex.mechanic in multipleChoiceMechanics) {
      val opts = ex.resolvedAnswerOptions()
      if (opts.size < 4) return false
      if (opts.any { it.text.isBlank() || it.id.isBlank() }) return false
      if (opts.map { it.text.trim().lowercase() }.distinct().size != opts.size) return false
      if (opts.map { it.id }.distinct().size != opts.size) return false

      val cid = ex.resolvedCorrectOptionId()
      val matchingOpts = opts.filter { it.id == cid }
      if (matchingOpts.size != 1) return false

      val expectedIdx = opts.indexOfFirst { it.id == cid }
      if (ex.correctIndex != expectedIdx) return false

      val targetBoundMechanics = setOf(
        GameInteractionMechanic.PICTURE_IDENTIFICATION,
        GameInteractionMechanic.SOUND_DETECTIVE,
        GameInteractionMechanic.MISSING_AUDIO_WORD,
        GameInteractionMechanic.GRAMMAR_CORRECTION,
        GameInteractionMechanic.ESCAPE_ROOM_PUZZLE
      )
      if (ex.mechanic in targetBoundMechanics) {
        if (!matchingOpts.first().text.trim().equals(ex.targetPhrase.trim(), ignoreCase = true)) {
          return false
        }
      }
    }

    if (ex.mechanic == GameInteractionMechanic.PICTURE_IDENTIFICATION) {
      if (ex.visualConceptId.isBlank()) return false
      if (!VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
          conceptId = ex.visualConceptId,
          targetPhrase = ex.targetPhrase,
          englishMeaning = ex.translation,
          prompt = ex.prompt
        )
      ) {
        return false
      }
    }

    // Sentence, Fluency, Scene, Question-Answer & Dialogue speaking games must NEVER degenerate into isolated single words!
    val multiWordSpeakingMechanics = setOf(
      GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE,
      GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT,
      GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE,
      GameInteractionMechanic.SPEAKING_QUICK_ANSWER,
      GameInteractionMechanic.SPEAKING_SPEECH_COACH,
      GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT,
      GameInteractionMechanic.SPEAKING_SHADOWING,
      GameInteractionMechanic.SPEAKING_DIALOGUE_LINE,
      GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION
    )
    if (ex.mechanic in multiWordSpeakingMechanics) {
      val wordCount = ex.targetPhrase.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
      val isCompactScript = ex.targetPhrase.any { Character.UnicodeScript.of(it.code) in setOf(Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA) }
      if (!isCompactScript && wordCount < 3) {
        return false
      }
      if (isCompactScript && ex.targetPhrase.trim().length < 6) {
        return false
      }
    }

    if (ex.mechanic == GameInteractionMechanic.SPEAKING_DIALOGUE_LINE ||
      ex.mechanic == GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION
    ) {
      if (ex.dialogueSpeakerALine.isBlank() && ex.conversationStarter.isBlank()) {
        return false
      }
    }

    return true
  }

  private suspend fun parseMechanicSpecificJson(
    rawJson: String,
    game: UltimateLanguageGame,
    spec: LevelCurriculumSpec,
    languageCode: String,
    context: Context? = null,
    userId: String = "default_learner"
  ): List<DynamicLevelExercise> {
    val result = mutableListOf<DynamicLevelExercise>()
    try {
      val clean = rawJson.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
      val arr = JSONArray(clean)
      val illustrations = VisualIllustrationKind.entries
      val fallbackWords = GameCurriculumAndAntiRepetitionEngine.THEMATIC_LEXICON
        .take(12)
        .map { it.translations[languageCode.lowercase().take(2)]?.first ?: it.enWord }

      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val exerciseId = "${game.id}_${languageCode}_${spec.levelNumber}_$i"
        val pairsList = mutableListOf<Pair<String, String>>()
        val pairsArr = obj.optJSONArray("pairs")
        if (pairsArr != null) {
          for (p in 0 until pairsArr.length()) {
            val po = pairsArr.optJSONObject(p) ?: continue
            val l = po.optString("left").trim()
            val r = po.optString("right").trim()
            if (l.isNotBlank() && r.isNotBlank()) pairsList.add(l to r)
          }
        }

        if (game.mechanic == GameInteractionMechanic.WORD_SEARCH_GRID && pairsList.size >= 3) {
          val wsSpec = WordSearchDifficultyAndTopicEngine.specForLevel(spec.levelNumber)
          val built = WordSearchDifficultyAndTopicEngine.generateValidatedWordSearchBoard(
            context = context,
            userId = userId,
            languageCode = languageCode,
            levelNumber = spec.levelNumber,
            objectiveTitle = spec.objectiveTitle,
            geminiCandidates = pairsList,
            persistUsedWords = false
          )
          val placed = built.placedWords
          if (placed.isNotEmpty()) {
            val joinedTargets = placed.joinToString(", ") { it.first }
            result.add(
              DynamicLevelExercise(
                id = exerciseId,
                gameId = game.id,
                levelNumber = spec.levelNumber,
                mechanic = GameInteractionMechanic.WORD_SEARCH_GRID,
                prompt = "Find all ${placed.size} hidden words for ${spec.objectiveTitle}",
                targetPhrase = joinedTargets,
                translation = placed.joinToString(", ") { it.second },
                phonetic = "",
                options = placed.map { it.first },
                correctIndex = 0,
                wordSearchGrid = built.grid,
                wordSearchTargets = placed,
                hint = "${wsSpec.tier.label} (${wsSpec.gridSize}×${wsSpec.gridSize}) • ${wsSpec.directionSummary}. Tap start & end letters!",
                explanation = obj.optString("explanation", "Hidden words: $joinedTargets"),
                objectiveLabel = spec.objectiveTitle,
                contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, joinedTargets)
              )
            )
            continue
          }
        }

        if ((game.mechanic == GameInteractionMechanic.WORD_MATCH_PAIRS ||
            game.mechanic == GameInteractionMechanic.MEMORY_CARD_FLIP ||
            game.mechanic == GameInteractionMechanic.AUDIO_WORD_MATCH) && pairsList.size >= 4
        ) {
          val pairLimit = if (game.mechanic == GameInteractionMechanic.AUDIO_WORD_MATCH) 5 else 6
          val selectedPairs = pairsList.distinctBy { it.first.lowercase() }.take(pairLimit)
          val leadKey = selectedPairs.joinToString("|") { it.first }
          result.add(
            DynamicLevelExercise(
              id = exerciseId,
              gameId = game.id,
              levelNumber = spec.levelNumber,
              mechanic = game.mechanic,
              prompt = obj.optString("prompt", "Match all ${selectedPairs.size} pairs — ${spec.objectiveTitle}"),
              targetPhrase = selectedPairs.first().first,
              translation = selectedPairs.first().second,
              phonetic = "",
              options = selectedPairs.map { it.first }.take(4),
              correctIndex = 0,
              matchingPairs = selectedPairs,
              hint = "Pair \"${selectedPairs.first().first}\" with \"${selectedPairs.first().second}\".",
              explanation = obj.optString("explanation", selectedPairs.joinToString(" • ") { "${it.first} = ${it.second}" }),
              objectiveLabel = spec.objectiveTitle,
              contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, leadKey)
            )
          )
          continue
        }

        val targetPhrase = obj.optString("targetPhrase").trim()
        if (targetPhrase.isBlank()) continue

        val prompt = obj.optString("prompt").trim().ifBlank {
          when (game.mechanic) {
            GameInteractionMechanic.SPELLING_INPUT -> "Listen to the word and type its exact spelling:"
            GameInteractionMechanic.LISTEN_AND_TYPE -> "Listen carefully and type what you hear:"
            GameInteractionMechanic.WORDIGO_GRID -> "Guess the secret word in up to 6 tries:"
            GameInteractionMechanic.SCRAMBLE_TILES -> "Arrange the tiles into a natural sentence:"
            GameInteractionMechanic.GRAMMAR_REPAIR -> "Find and fix the mistake in this sentence:"
            else -> spec.objectiveTitle
          }
        }
        val translation = obj.optString("translation").trim().ifBlank { targetPhrase }
        val phonetic = obj.optString("phonetic").trim()

        val rawOpts = mutableListOf<String>()
        val optsArr = obj.optJSONArray("options")
        if (optsArr != null) {
          for (j in 0 until optsArr.length()) {
            val opt = optsArr.optString(j).trim()
            if (opt.isNotBlank()) rawOpts.add(opt)
          }
        }

        val explicitCorrect = obj.optString("correctOption").trim()
          .ifBlank { obj.optString("correctAnswer").trim() }

        val meaningBasedMechanics = setOf(
          GameInteractionMechanic.LISTENING_COMPREHENSION,
          GameInteractionMechanic.LISTENING_STORY_QUIZ,
          GameInteractionMechanic.CONVERSATION_LISTENING,
          GameInteractionMechanic.FAST_SPEECH_CHALLENGE,
          GameInteractionMechanic.FLASHCARD_FLIP
        )

        val verifiedCorrectChoice = if (game.mechanic in meaningBasedMechanics) {
          explicitCorrect.ifBlank {
            rawOpts.firstOrNull { it.equals(translation, ignoreCase = true) } ?: translation
          }
        } else {
          // NEVER trust rawOpts.first() — bind strictly to targetPhrase!
          rawOpts.firstOrNull { it.equals(targetPhrase, ignoreCase = true) }
            ?: explicitCorrect.takeIf { it.equals(targetPhrase, ignoreCase = true) }
            ?: targetPhrase
        }

        var resolvedConceptId = ""
        var resolvedImgUrl = ""
        var resolvedIcon = ""
        var resolvedTranslation = translation

        if (game.mechanic == GameInteractionMechanic.PICTURE_IDENTIFICATION) {
          // Reject prompts that spoil/leak the answer word itself in Picture Word Quiz
          if (game.id == "ug_01_picture_word_quiz" && targetPhrase.length >= 3) {
            val spoilerRegex = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(targetPhrase.lowercase())}(?![\\p{L}\\p{N}])")
            if (spoilerRegex.containsMatchIn(prompt.lowercase())) {
              continue
            }
          }

          val englishConcept = obj.optString("englishConcept").trim()
            .ifBlank { translation.substringAfter("•").trim() }
          val imageCategory = obj.optString("imageCategory").trim().ifBlank { spec.objectiveTitle }
          val explicitConceptId = obj.optString("visualConceptId").trim().lowercase()

          val curatedCandidate = VerifiedVisualConceptRegistry.getById(explicitConceptId)
            ?: VerifiedVisualConceptRegistry.findMatchingConcept(targetPhrase, englishConcept, translation, prompt)

          if (curatedCandidate != null &&
            VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
              conceptId = curatedCandidate.conceptId,
              targetPhrase = targetPhrase,
              englishMeaning = translation.ifBlank { englishConcept },
              prompt = prompt
            ) &&
            VerifiedVisualConceptRegistry.isConceptCompatibleWithTopic(curatedCandidate, spec.objectiveTitle, spec.levelNumber)
          ) {
            resolvedConceptId = curatedCandidate.conceptId
            resolvedImgUrl = curatedCandidate.wikimediaPhotoUrl
            resolvedIcon = curatedCandidate.iconKey
          } else {
            val cleanEngConcept = englishConcept.replace(Regex("\\([^)]*\\)"), "").trim()
            if (cleanEngConcept.isBlank()) continue
            val slug = cleanEngConcept.lowercase()
              .replace(Regex("[^a-z0-9]+"), "_")
              .trim('_')
              .ifBlank { "item_${spec.levelNumber}_$i" }
            val dynId = "dyn:$slug"
            val verifiedUrl = VerifiedVisualConceptRegistry.resolveAndVerifyDynamicImageUrl(
              englishConcept = cleanEngConcept,
              categoryHint = imageCategory,
              context = context
            )
            resolvedTranslation = if (translation.equals(cleanEngConcept, ignoreCase = true) ||
              translation.contains(cleanEngConcept, ignoreCase = true)
            ) {
              translation
            } else {
              "$cleanEngConcept • $translation"
            }
            val dynConcept = VerifiedVisualConceptRegistry.registerDynamicConcept(
              conceptId = dynId,
              englishWord = cleanEngConcept,
              targetWord = targetPhrase,
              languageCode = languageCode,
              domainLabel = imageCategory,
              definitionQuestionEn = prompt,
              exampleSentence = obj.optString("explanation", "$targetPhrase — $cleanEngConcept"),
              imageUrl = verifiedUrl,
              iconKey = slug,
              levelBand = GameCurriculumAndAntiRepetitionEngine.tierForLevel(spec.levelNumber)
            )
            if (!VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
                conceptId = dynConcept.conceptId,
                targetPhrase = targetPhrase,
                englishMeaning = resolvedTranslation,
                prompt = prompt
              )
            ) {
              continue
            }
            resolvedConceptId = dynConcept.conceptId
            resolvedImgUrl = dynConcept.wikimediaPhotoUrl
            resolvedIcon = dynConcept.iconKey
          }
        }


        val (validatedOptions, verifiedCorrectId, verifiedCorrectIdx) = buildValidatedAnswerOptions(
          exerciseId = exerciseId,
          verifiedCorrectText = verifiedCorrectChoice,
          candidateOptions = rawOpts,
          fallbackDistractors = fallbackWords,
          rotationSeed = spec.levelNumber + i
        )

        val tokens = if (targetPhrase.contains(" ")) {
          targetPhrase.split(Regex("\\s+")).filter { it.isNotBlank() }
        } else {
          targetPhrase.map { it.toString() }
        }

        val illIndex = (spec.levelNumber * 3 + i * 2) % illustrations.size
        val starter = obj.optString("conversationStarter").trim().ifBlank { prompt }
        val secondaryAudio = obj.optString("storyPassage").trim()
          .ifBlank { obj.optString("fullSpokenSentence").trim() }
          .ifBlank { obj.optString("contrastPhrase").trim() }
        val speakerA = obj.optString("speakerA").trim()
        val speakerB = obj.optString("speakerB").trim()
        val clozeSentence = obj.optString("clozeMaskedSentence").trim()

        result.add(
          DynamicLevelExercise(
            id = exerciseId,
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = game.mechanic,
            prompt = prompt,
            targetPhrase = targetPhrase,
            translation = resolvedTranslation,
            phonetic = phonetic,
            options = validatedOptions.map { it.text },
            correctIndex = verifiedCorrectIdx,
            answerOptions = validatedOptions,
            correctOptionId = verifiedCorrectId,
            visualConceptId = resolvedConceptId,
            secondaryAudioPhrase = secondaryAudio,
            dialogueSpeakerALine = speakerA,
            dialogueSpeakerBLine = speakerB,
            clozeMaskedSentence = clozeSentence,
            scrambledTokens = rotateTokensDeterministically(tokens, spec.levelNumber + i),
            matchingPairs = pairsList.ifEmpty { listOf(targetPhrase to resolvedTranslation) },
            illustrationKind = illustrations[illIndex],
            hint = obj.optString("hint", "Focus on ${spec.objectiveTitle.lowercase()}."),
            explanation = obj.optString("explanation", "$targetPhrase — $resolvedTranslation"),
            conversationStarter = starter,
            conversationRole = game.title,
            timeLimitSeconds = if (game.mechanic == GameInteractionMechanic.SPEED_RUN_TIMER) 16 else 30,
            incorrectSentence = obj.optString("incorrectSentence").trim(),
            errorWord = obj.optString("errorWord").trim(),
            correctedWord = obj.optString("correctedWord").trim(),
            escapeStageNumber = (i + 1).coerceIn(1, 4),
            escapeRoomClueCode = obj.optString("codeSymbol", "${('A' + (spec.levelNumber + i) % 26)}${(i + 1)}"),
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, targetPhrase),
            resolvedImageUrl = resolvedImgUrl,
            visualIconKey = resolvedIcon
          )
        )
      }
    } catch (_: Exception) {}
    return result
  }

  private fun rotateTokensDeterministically(tokens: List<String>, seed: Int): List<String> {
    if (tokens.size <= 1) return tokens
    val rotated = tokens.indices.map { idx -> tokens[(idx + (seed % (tokens.size - 1)) + 1) % tokens.size] }
    return if (rotated == tokens) tokens.reversed() else rotated
  }

  /**
   * Builds rich, curriculum-accurate, non-repeating exercises for every game and level (1..500)
   * using `GameCurriculumAndAntiRepetitionEngine.THEMATIC_LEXICON`, guaranteeing distinct content
   * across consecutive levels even when offline or in JVM unit tests.
   */
  private fun buildDistinctCuratedLevelExercises(
    game: UltimateLanguageGame,
    spec: LevelCurriculumSpec,
    languageCode: String,
    nativeLanguageCode: String,
    targetCount: Int,
    excludedSet: Set<String>,
    context: Context? = null,
    userId: String = "default_learner"
  ): List<DynamicLevelExercise> {
    val targetKey = languageCode.lowercase().take(2)
    val nativeKey = nativeLanguageCode.lowercase().take(2).let { if (it == targetKey) "en" else it }
    val lexiconAll = GameCurriculumAndAntiRepetitionEngine.THEMATIC_LEXICON

    // 0. PICTURE IDENTIFICATION (Picture Word Quiz & Picture Description) — 100% Concept-Verified Visuals!
    if (game.mechanic == GameInteractionMechanic.PICTURE_IDENTIFICATION) {
      val selectedConcepts = VerifiedVisualConceptRegistry.selectConceptsForPictureLevel(
        levelNumber = spec.levelNumber,
        languageCode = targetKey,
        count = targetCount,
        excludedLowercase = excludedSet,
        topic = spec.objectiveTitle
      )
      val allConcepts = VerifiedVisualConceptRegistry.ALL_CONCEPTS
      val allIlls = VisualIllustrationKind.entries

      return selectedConcepts.mapIndexed { idx, concept ->
        val exerciseId = "${game.id}_${languageCode}_${spec.levelNumber}_$idx"
        val isSentenceMode = game.id == "ug_14_picture_description"
        val verifiedAnswer = if (isSentenceMode) {
          concept.sentenceForLanguage(targetKey)
        } else {
          concept.wordForLanguage(targetKey)
        }
        val meaningForCard = when {
          isSentenceMode -> if (targetKey == "en") concept.definitionQuestionEn else concept.exampleSentenceEn
          concept.isDynamic -> concept.englishWord
          targetKey == "en" -> "${concept.domainLabel} • ${concept.exampleSentenceEn}"
          else -> concept.englishWord
        }

        // Avoid ambiguity: strictly exclude synonyms or objects that also answer the question (e.g. watch/phone for clock)
        val conflictingWords = when (concept.conceptId) {
          "clock" -> setOf("watch", "phone", "smartphone", "wristwatch", "reloj de pulsera")
          "watch" -> setOf("clock", "wall clock", "reloj de pared")
          "table" -> setOf("desk", "escritorio")
          else -> emptySet()
        }

        // Strictly prioritize same-domain distractors first, filtered against ambiguous/synonym words
        val sameDomainDistractors = allConcepts
          .filter { it.conceptId != concept.conceptId && it.domainIndex == concept.domainIndex }
          .filter { it.conceptId !in conflictingWords && it.englishWord.lowercase() !in conflictingWords }
        val otherDomainDistractors = allConcepts
          .filter { it.conceptId != concept.conceptId && it.domainIndex != concept.domainIndex }
          .filter { it.conceptId !in conflictingWords && it.englishWord.lowercase() !in conflictingWords }

        val orderedDistractorConcepts = run {
          val combined = if (sameDomainDistractors.size >= 4) sameDomainDistractors else sameDomainDistractors + otherDomainDistractors
          val offset = (spec.levelNumber * 2 + idx * 3) % combined.size.coerceAtLeast(1)
          combined.drop(offset) + combined.take(offset)
        }
        val distractorStrings = orderedDistractorConcepts
          .map { if (isSentenceMode) it.sentenceForLanguage(targetKey) else it.wordForLanguage(targetKey) }
          .distinctBy { GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(it).ifBlank { it.lowercase() } }

        val (validatedOptions, verifiedCorrectId, verifiedCorrectIdx) = buildValidatedAnswerOptions(
          exerciseId = exerciseId,
          verifiedCorrectText = verifiedAnswer,
          candidateOptions = distractorStrings.take(4),
          fallbackDistractors = distractorStrings.drop(4),
          rotationSeed = spec.levelNumber + idx // Deterministic A/B/C/D coverage across every level!
        )

        val questionPrompt = when {
          isSentenceMode -> "Which sentence accurately describes what is shown in the picture?"
          targetKey == "en" -> concept.definitionQuestionEn
          else -> "${concept.definitionQuestionEn} (${Language.findByCode(languageCode).name})"
        }

        // Keep consecutive illustrationKind distinct for legacy assertions while UI uses visualConceptId
        val illKind = allIlls[(spec.levelNumber + idx) % allIlls.size]

        DynamicLevelExercise(
          id = exerciseId,
          gameId = game.id,
          levelNumber = spec.levelNumber,
          mechanic = GameInteractionMechanic.PICTURE_IDENTIFICATION,
          prompt = questionPrompt,
          targetPhrase = verifiedAnswer,
          translation = meaningForCard,
          phonetic = concept.phoneticForLanguage(targetKey),
          options = validatedOptions.map { it.text },
          correctIndex = verifiedCorrectIdx,
          answerOptions = validatedOptions,
          correctOptionId = verifiedCorrectId,
          visualConceptId = concept.conceptId,
          matchingPairs = listOf(verifiedAnswer to concept.englishWord),
          illustrationKind = illKind,
          hint = if (targetKey == "en") {
            "Example clue: \"${concept.exampleSentenceEn.replace(concept.englishWord, "___", ignoreCase = true)}\""
          } else {
            "English meaning: ${concept.englishWord}"
          },
          explanation = "\"$verifiedAnswer\" — ${concept.sentenceForLanguage(targetKey)}",
          objectiveLabel = spec.objectiveTitle,
          contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, verifiedAnswer),
          resolvedImageUrl = concept.wikimediaPhotoUrl,
          visualIconKey = concept.iconKey
        )
      }
    }

    // 0b. ALL 10 SPEAKING GAMES (#24..#33) — Dedicated Speaking Content Engine!
    if (game.mechanic.isSpeakingMechanic) {
      return SpeakingGamesContentAndEvaluationEngine.buildSpeakingGameExercises(
        game = game,
        spec = spec,
        languageCode = languageCode,
        nativeLanguageCode = nativeLanguageCode,
        targetCount = targetCount,
        excludedSet = excludedSet
      )
    }

    // Select enough non-overlapping thematic items for this level
    val neededEntries = when (game.mechanic) {
      GameInteractionMechanic.WORD_MATCH_PAIRS,
      GameInteractionMechanic.MEMORY_CARD_FLIP,
      GameInteractionMechanic.AUDIO_WORD_MATCH -> targetCount * 6
      GameInteractionMechanic.WORD_SEARCH_GRID -> 6
      else -> targetCount
    }

    val entries = GameCurriculumAndAntiRepetitionEngine.selectDistinctEntriesForLevel(
      game = game,
      languageCode = languageCode,
      levelNumber = spec.levelNumber,
      count = neededEntries,
      excludedLowercase = excludedSet
    )

    fun wordIn(item: ThematicLexiconItem, lang: String): String =
      item.translations[lang]?.first ?: item.enWord

    fun sentenceIn(item: ThematicLexiconItem, lang: String): String =
      item.translations[lang]?.second ?: item.enSentence

    fun meaningForLearner(item: ThematicLexiconItem, useSentence: Boolean): String {
      return if (targetKey == "en" && nativeKey == "en") {
        if (useSentence) "Key idea: ${item.enWord} (${item.domainLabel})"
        else "${item.domainLabel} — used in: \"${item.enSentence}\""
      } else {
        if (useSentence) sentenceIn(item, nativeKey) else wordIn(item, nativeKey)
      }
    }

    // 1. WORD SEARCH GRID (Progressive 6-tier difficulty: 5x5 Beginner up to 15x15 Master)
    if (game.mechanic == GameInteractionMechanic.WORD_SEARCH_GRID) {
      val wsSpec = WordSearchDifficultyAndTopicEngine.specForLevel(spec.levelNumber)
      val lexiconHintCandidates = entries
        .filter { item ->
          WordSearchDifficultyAndTopicEngine.isCandidateTopicRelevant(
            word = if (targetKey in listOf("ar", "ja", "zh", "ko", "hi", "ur")) item.enWord else wordIn(item, targetKey),
            clue = "${wordIn(item, nativeKey)} (${item.domainLabel})",
            objectiveTitle = spec.objectiveTitle
          )
        }
        .map { item ->
          val w = if (targetKey in listOf("ar", "ja", "zh", "ko", "hi", "ur")) item.enWord else wordIn(item, targetKey)
          w to wordIn(item, nativeKey)
        }
      val built = WordSearchDifficultyAndTopicEngine.generateValidatedWordSearchBoard(
        context = context,
        userId = userId,
        languageCode = languageCode,
        levelNumber = spec.levelNumber,
        objectiveTitle = spec.objectiveTitle,
        geminiCandidates = lexiconHintCandidates,
        additionalExcluded = excludedSet,
        persistUsedWords = false
      )
      val placed = built.placedWords
      val summaryTarget = placed.joinToString(", ") { it.first }
      return listOf(
        DynamicLevelExercise(
          id = "${game.id}_${languageCode}_${spec.levelNumber}_0",
          gameId = game.id,
          levelNumber = spec.levelNumber,
          mechanic = GameInteractionMechanic.WORD_SEARCH_GRID,
          prompt = "Find all ${placed.size} hidden words for ${spec.objectiveTitle}:",
          targetPhrase = summaryTarget,
          translation = placed.joinToString(" • ") { it.second },
          phonetic = "",
          options = placed.map { it.first },
          correctIndex = 0,
          wordSearchGrid = built.grid,
          wordSearchTargets = placed,
          hint = "${wsSpec.tier.label} (${wsSpec.gridSize}×${wsSpec.gridSize}) • ${wsSpec.directionSummary}. Tap first & last letters!",
          explanation = "Completed ${spec.objectiveTitle} grid (${wsSpec.tier.label}): $summaryTarget",
          objectiveLabel = spec.objectiveTitle,
          contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, summaryTarget)
        )
      )
    }

    // 2. WORD MATCH PAIRS, MEMORY CARD FLIP & AUDIO WORD MATCH
    if (game.mechanic == GameInteractionMechanic.WORD_MATCH_PAIRS ||
      game.mechanic == GameInteractionMechanic.MEMORY_CARD_FLIP ||
      game.mechanic == GameInteractionMechanic.AUDIO_WORD_MATCH
    ) {
      val pairsPerBoard = if (game.mechanic == GameInteractionMechanic.AUDIO_WORD_MATCH) 5 else 6
      return (0 until targetCount).map { boardIdx ->
        val slice = entries.drop(boardIdx * pairsPerBoard).take(pairsPerBoard).ifEmpty { entries.take(pairsPerBoard) }
        val pairs = slice.map { item ->
          val left = wordIn(item, targetKey)
          val right = if (targetKey == nativeKey) {
            item.enSentence.replace(item.enWord, "___", ignoreCase = true)
          } else {
            wordIn(item, nativeKey)
          }
          left to right
        }
        val hashSeed = pairs.joinToString("|") { it.first }
        DynamicLevelExercise(
          id = "${game.id}_${languageCode}_${spec.levelNumber}_$boardIdx",
          gameId = game.id,
          levelNumber = spec.levelNumber,
          mechanic = game.mechanic,
          prompt = when (game.mechanic) {
            GameInteractionMechanic.MEMORY_CARD_FLIP ->
              "Flip cards to match all ${pairs.size} ${spec.objectiveTitle.lowercase()} pairs"
            GameInteractionMechanic.AUDIO_WORD_MATCH ->
              "Tap each speaker clip on the left and match it with its meaning on the right"
            else ->
              "Match each expression on the left with its meaning on the right"
          },
          targetPhrase = pairs.first().first,
          translation = pairs.first().second,
          phonetic = "",
          options = pairs.map { it.first }.take(4),
          correctIndex = 0,
          matchingPairs = pairs,
          hint = "Tap an audio or word card first, then tap its matching meaning.",
          explanation = pairs.joinToString(" • ") { "${it.first} → ${it.second}" },
          objectiveLabel = spec.objectiveTitle,
          contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, hashSeed)
        )
      }
    }

    // 3. ALL OTHER DISTINCT MECHANICS (including the 7 other Listening mechanics!)
    val results = mutableListOf<DynamicLevelExercise>()
    var previousIllustration: VisualIllustrationKind? = null

    for (idx in 0 until targetCount) {
      val exerciseId = "${game.id}_${languageCode}_${spec.levelNumber}_$idx"
      val item = entries[idx % entries.size]
      val companionItem = entries[(idx + 1) % entries.size]

      val useSentence = game.mechanic in listOf(
        GameInteractionMechanic.SCRAMBLE_TILES,
        GameInteractionMechanic.GRAMMAR_REPAIR,
        GameInteractionMechanic.LISTENING_COMPREHENSION,
        GameInteractionMechanic.LISTENING_STORY_QUIZ,
        GameInteractionMechanic.CONVERSATION_LISTENING,
        GameInteractionMechanic.FAST_SPEECH_CHALLENGE,
        GameInteractionMechanic.AI_CONVERSATION_MISSION
      ) || (spec.levelNumber > 75 && game.mechanic == GameInteractionMechanic.LISTEN_AND_TYPE) ||
        (spec.levelNumber > 100 && game.mechanic == GameInteractionMechanic.SPEAKING_MIC)

      val fullSentenceTarget = sentenceIn(item, targetKey)
      val singleWordTarget = wordIn(item, targetKey)

      // For MISSING_AUDIO_WORD: speak the full sentence, mask one word on screen, and test the missing word!
      val (missingWordAnswer, maskedClozeSentence) = if (game.mechanic == GameInteractionMechanic.MISSING_AUDIO_WORD) {
        if (fullSentenceTarget.contains(singleWordTarget, ignoreCase = true)) {
          singleWordTarget to fullSentenceTarget.replaceFirst(Regex(Regex.escape(singleWordTarget), RegexOption.IGNORE_CASE), "[ _____ ]")
        } else {
          val sentenceTokens = fullSentenceTarget.split(Regex("\\s+")).filter { it.isNotBlank() }
          val chosenTok = sentenceTokens.maxByOrNull { it.filter(Char::isLetter).length }?.trim(',', '.', '!', '?', ';', ':')
            ?: singleWordTarget
          chosenTok to fullSentenceTarget.replaceFirst(chosenTok, "[ _____ ]")
        }
      } else {
        "" to ""
      }

      val targetText = when (game.mechanic) {
        GameInteractionMechanic.WORDIGO_GRID -> {
          val candidate = singleWordTarget.filter { it.isLetter() }
          if (candidate.length in 4..7) candidate else item.enWord.filter { it.isLetter() }.take(6).padEnd(4, 'E')
        }
        GameInteractionMechanic.MISSING_AUDIO_WORD -> missingWordAnswer
        GameInteractionMechanic.SOUND_DETECTIVE -> singleWordTarget
        else -> if (useSentence) fullSentenceTarget else singleWordTarget
      }

      val meaningText = meaningForLearner(item, useSentence)
      val phonetic = item.phonetics[targetKey] ?: ""

      // Build verified distractors from OTHER items in the lexicon
      val distractors = lexiconAll
        .filter { it.enWord != item.enWord }
        .drop((spec.levelNumber * 3 + idx * 5) % (lexiconAll.size - 6).coerceAtLeast(1))
        .take(5)

      val contrastWord = wordIn(distractors.first(), targetKey)
      val storyPassage = "$fullSentenceTarget ${sentenceIn(companionItem, targetKey)}"
      val speakerALine = when (targetKey) {
        "es" -> "Disculpa, ¿tienes todo listo para ${item.domainLabel.lowercase()} hoy?"
        "fr" -> "Excusez-moi, avez-vous ce qu'il faut pour ${item.domainLabel.lowercase()} ?"
        "de" -> "Entschuldigung, ist heute alles bereit für ${item.domainLabel}?"
        else -> "Excuse me, what is your plan regarding ${item.domainLabel.lowercase()} today?"
      }
      val speakerBLine = fullSentenceTarget

      val comprehensionCorrectOption = if (targetKey == nativeKey) {
        "About ${item.enWord.lowercase()} (${item.domainLabel.lowercase()})"
      } else {
        sentenceIn(item, nativeKey)
      }
      val comprehensionDistractors = distractors.map { d ->
        if (targetKey == nativeKey) {
          "About ${d.enWord.lowercase()} (${d.domainLabel.lowercase()})"
        } else {
          sentenceIn(d, nativeKey)
        }
      }

      val (verifiedCorrectChoice, candidateDistractors) = when (game.mechanic) {
        GameInteractionMechanic.LISTENING_COMPREHENSION,
        GameInteractionMechanic.LISTENING_STORY_QUIZ,
        GameInteractionMechanic.CONVERSATION_LISTENING,
        GameInteractionMechanic.FAST_SPEECH_CHALLENGE -> {
          comprehensionCorrectOption to comprehensionDistractors
        }
        GameInteractionMechanic.FLASHCARD_FLIP -> {
          meaningText to distractors.map { meaningForLearner(it, useSentence) }
        }
        GameInteractionMechanic.MISSING_AUDIO_WORD,
        GameInteractionMechanic.SOUND_DETECTIVE -> {
          targetText to distractors.map { wordIn(it, targetKey) }
        }
        else -> {
          targetText to distractors.map { if (useSentence) sentenceIn(it, targetKey) else wordIn(it, targetKey) }
        }
      }

      val (validatedOptions, verifiedCorrectId, verifiedCorrectIdx) = buildValidatedAnswerOptions(
        exerciseId = exerciseId,
        verifiedCorrectText = verifiedCorrectChoice,
        candidateOptions = candidateDistractors.take(3),
        fallbackDistractors = candidateDistractors.drop(3),
        rotationSeed = spec.levelNumber + idx
      )

      val chosenIll = item.illustrationKind

      val (flawedSent, errTok, fixTok) = if (targetKey == "en") {
        Triple(item.enFlawedSentence, item.enErrorWord, item.enCorrectedWord)
      } else {
        val words = sentenceIn(item, targetKey).split(" ")
        if (words.size >= 3) {
          val wToSwap = words[1]
          val wrongWord = wordIn(distractors.first(), targetKey)
          val broken = listOf(words[0], wrongWord) + words.drop(2)
          Triple(broken.joinToString(" "), wrongWord, wToSwap)
        } else {
          Triple("${wordIn(distractors.first(), targetKey)} ${sentenceIn(item, targetKey)}", wordIn(distractors.first(), targetKey), words.first())
        }
      }

      val tokens = if (targetText.contains(" ")) {
        targetText.split(Regex("\\s+")).filter { it.isNotBlank() }
      } else {
        targetText.map { it.toString() }
      }

      val partnerOpening = when (targetKey) {
        "es" -> "¡Hola! Estamos hablando sobre ${item.domainLabel.lowercase()}. ¿Qué opinas o necesitas respecto a \"${wordIn(item, "es")}\"?"
        "fr" -> "Bonjour ! Parlons de ${item.domainLabel.lowercase()}. Que souhaitez-vous à propos de « ${wordIn(item, "fr")} » ?"
        "de" -> "Hallo! Wir sprechen über ${item.domainLabel}. Was möchten Sie bezüglich „${wordIn(item, "de")}“ sagen?"
        else -> "Welcome! Regarding ${item.domainLabel.lowercase()}, how can I help you with your ${item.enWord.lowercase()} today?"
      }

      val secondaryAudio = when (game.mechanic) {
        GameInteractionMechanic.SOUND_DETECTIVE -> contrastWord
        GameInteractionMechanic.LISTENING_STORY_QUIZ -> storyPassage
        GameInteractionMechanic.MISSING_AUDIO_WORD -> fullSentenceTarget
        else -> ""
      }

      val promptText = when (game.mechanic) {
        GameInteractionMechanic.SPELLING_INPUT ->
          "Listen to the spoken word and type its exact spelling:"
        GameInteractionMechanic.LISTEN_AND_TYPE ->
          "Listen carefully to the audio and type exactly what you hear:"
        GameInteractionMechanic.WORDIGO_GRID ->
          "Deduce the ${targetText.length}-letter word (${item.domainLabel}):"
        GameInteractionMechanic.SCRAMBLE_TILES ->
          "Build the sentence (${spec.objectiveTitle}):"
        GameInteractionMechanic.GRAMMAR_REPAIR ->
          "Tap the incorrect word or edit the sentence to fix the mistake:"
        GameInteractionMechanic.LISTENING_COMPREHENSION ->
          "Listen to the audio clip and select its meaning:"
        GameInteractionMechanic.SOUND_DETECTIVE ->
          "Compare Clip A & Clip B, play the Mystery Audio, and pick the exact word spoken:"
        GameInteractionMechanic.LISTENING_STORY_QUIZ ->
          "Listen to the short passage and choose the statement that matches the story:"
        GameInteractionMechanic.MISSING_AUDIO_WORD ->
          "Listen to the full sentence and pick the word that fills the blank:"
        GameInteractionMechanic.CONVERSATION_LISTENING ->
          "Listen to Speaker A and Speaker B. What is Speaker B saying?"
        GameInteractionMechanic.FAST_SPEECH_CHALLENGE ->
          "Listen at brisk native speed (1.25x) and select the accurate meaning:"
        GameInteractionMechanic.SPEAKING_MIC ->
          "Listen to the model, then tap the microphone and speak aloud:"
        GameInteractionMechanic.AI_CONVERSATION_MISSION ->
          "Respond naturally in ${Language.findByCode(languageCode).name} to continue the ${game.title.lowercase()}:"
        GameInteractionMechanic.ESCAPE_ROOM_PUZZLE ->
          "Lock Stage ${idx + 1}/4: Solve the clue for \"${item.enSentence.replace(item.enWord, "____", ignoreCase = true)}\" to reveal code digit #${idx + 1}:"
        GameInteractionMechanic.SPEED_RUN_TIMER ->
          if (targetKey == "en") "Quick recall! Which word fits: \"${item.enSentence.replace(item.enWord, "___", ignoreCase = true)}\"?"
          else "Quick recall! Choose the ${Language.findByCode(languageCode).name} expression for \"${item.enWord}\":"
        GameInteractionMechanic.FLASHCARD_FLIP ->
          "Tap the card to review, then select the matching meaning:"
        else ->
          if (targetKey == "en") "Complete the expression: \"${item.enSentence.replace(item.enWord, "___", ignoreCase = true)}\""
          else "Choose the accurate ${Language.findByCode(languageCode).name} expression for \"${item.enWord}\":"
      }

      results.add(
        DynamicLevelExercise(
          id = exerciseId,
          gameId = game.id,
          levelNumber = spec.levelNumber,
          mechanic = game.mechanic,
          prompt = promptText,
          targetPhrase = targetText,
          translation = meaningText,
          phonetic = phonetic,
          options = validatedOptions.map { it.text },
          correctIndex = verifiedCorrectIdx,
          answerOptions = validatedOptions,
          correctOptionId = verifiedCorrectId,
          visualConceptId = item.visualConceptId,
          secondaryAudioPhrase = secondaryAudio,
          dialogueSpeakerALine = if (game.mechanic == GameInteractionMechanic.CONVERSATION_LISTENING) speakerALine else "",
          dialogueSpeakerBLine = if (game.mechanic == GameInteractionMechanic.CONVERSATION_LISTENING) speakerBLine else "",
          clozeMaskedSentence = maskedClozeSentence,
          scrambledTokens = rotateTokensDeterministically(tokens, spec.levelNumber + idx),
          matchingPairs = listOf(singleWordTarget to meaningText),
          illustrationKind = chosenIll,
          hint = when (game.mechanic) {
            GameInteractionMechanic.SPELLING_INPUT,
            GameInteractionMechanic.LISTEN_AND_TYPE,
            GameInteractionMechanic.WORDIGO_GRID -> "Starts with '${targetText.take(1).uppercase()}' (${targetText.length} characters) • Domain: ${item.domainLabel}"
            GameInteractionMechanic.GRAMMAR_REPAIR -> "Look closely at \"$errTok\" — try \"$fixTok\"."
            GameInteractionMechanic.MISSING_AUDIO_WORD -> "Meaning clue: ${wordIn(item, nativeKey)} (${item.domainLabel})"
            else -> "Context clue: ${item.domainLabel}"
          },
          explanation = when (game.mechanic) {
            GameInteractionMechanic.MISSING_AUDIO_WORD -> "Full sentence: \"$fullSentenceTarget\" (missing word: \"$targetText\")."
            GameInteractionMechanic.LISTENING_STORY_QUIZ -> "Story transcript: \"$storyPassage\""
            GameInteractionMechanic.CONVERSATION_LISTENING -> "Speaker B said: \"$speakerBLine\" ($comprehensionCorrectOption)."
            else -> "\"$targetText\" — ${item.enSentence}"
          },
          conversationStarter = partnerOpening,
          conversationRole = game.title,
          timeLimitSeconds = if (game.mechanic == GameInteractionMechanic.SPEED_RUN_TIMER) 16 else 30,
          incorrectSentence = flawedSent,
          errorWord = errTok,
          correctedWord = fixTok,
          escapeStageNumber = (idx + 1).coerceIn(1, 4),
          escapeRoomClueCode = "${('A' + (spec.levelNumber + idx) % 9)}${(idx + 3)}",
          objectiveLabel = spec.objectiveTitle,
          contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, targetText)
        )
      )
    }

    return results
  }

  /**
   * Evaluates a learner's actual message in an interactive Conversation Mission and produces a
   * natural in-character partner reply via Gemini (with grounded contextual fallback).
   */
  suspend fun evaluateConversationTurnWithAi(
    game: UltimateLanguageGame,
    languageCode: String,
    nativeLanguageCode: String,
    partnerLine: String,
    userMessage: String,
    turnNumber: Int
  ): Pair<LearningResponseEvaluation, String> = withContext(Dispatchers.IO) {
    val cleanMsg = userMessage.trim()
    if (cleanMsg.length < 2) {
      val eval = LearningResponseEvaluation(
        isPassed = false,
        accuracyScore = 0,
        matchedWords = emptyList(),
        missingWords = emptyList(),
        feedbackTitle = "Please Reply First",
        detailedExplanation = "Speak or type your own response to your conversation partner.",
        suggestedImprovement = "Try answering with a short polite phrase."
      )
      return@withContext eval to ""
    }

    val langName = Language.findByCode(languageCode).name
    val nativeName = Language.findByCode(nativeLanguageCode).name
    val prompt = """
      You are roleplaying as the partner in "${game.title}" ($langName).
      You just said: "$partnerLine"
      The learner replied (Turn $turnNumber): "$cleanMsg"
      Evaluate whether the learner's message makes communicative sense in $langName and reply naturally in character (1–2 short sentences in $langName, plus $nativeName translation in parentheses).
      Return ONLY a JSON object:
      {"passed":true,"score":88,"feedback":"concise grammar/politeness note in $nativeName","improved":"more natural phrasing in $langName","partnerReply":"in-character next line in $langName ($nativeName translation)"}
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(prompt)
    if (!raw.isNullOrBlank()) {
      runCatching {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val passed = obj.optBoolean("passed", true)
        val score = obj.optInt("score", 85).coerceIn(0, 100)
        val feedback = obj.optString("feedback", "Good communicative response!")
        val improved = obj.optString("improved", cleanMsg)
        val reply = obj.optString("partnerReply", "¡Perfecto! Continuemos.")
        val eval = LearningResponseEvaluation(
          isPassed = passed && score >= 60,
          accuracyScore = score,
          matchedWords = cleanMsg.split(Regex("\\s+")).filter { it.isNotBlank() },
          missingWords = emptyList(),
          feedbackTitle = if (passed) "Natural Reply! ($score%)" else "Let's Refine That ($score%)",
          detailedExplanation = feedback,
          suggestedImprovement = "Polished phrasing: $improved"
        )
        return@withContext eval to reply
      }
    }

    // Offline / deterministic communicative evaluation (evaluates meaningful length & non-echo)
    val words = cleanMsg.split(Regex("\\s+")).filter { it.isNotBlank() }
    val isEcho = cleanMsg.equals(partnerLine.trim(), ignoreCase = true)
    val score = when {
      isEcho -> 35
      words.size >= 4 -> 92
      words.size >= 2 -> 82
      else -> 72
    }
    val passed = !isEcho && words.isNotEmpty()
    val fallbackReply = when (languageCode.lowercase()) {
      "es" -> "¡Entendido! Gracias por decir \"$cleanMsg\". ¿En qué más te puedo ayudar?"
      "fr" -> "Très bien ! Merci pour votre réponse. Souhaitez-vous autre chose ?"
      "de" -> "Verstanden! Vielen Dank. Kann ich Ihnen sonst noch weiterhelfen?"
      else -> "Got it! Thank you for sharing that. What else would you like to arrange today?"
    }
    val eval = LearningResponseEvaluation(
      isPassed = passed,
      accuracyScore = score,
      matchedWords = words,
      missingWords = emptyList(),
      feedbackTitle = if (passed) "Good Communicative Turn! ($score%)" else "Say It in Your Own Words",
      detailedExplanation = if (passed) "Your response moved the ${game.title.lowercase()} forward naturally." else "Avoid repeating the partner's exact line; answer in your own words.",
      suggestedImprovement = "Keep using complete polite sentences."
    )
    eval to fallbackReply
  }

  /**
   * Evaluates spoken or typed user responses against the target phrase with word-by-word diagnostics.
   */
  fun evaluateLearningResponse(
    userResponse: String,
    targetPhrase: String,
    mechanic: GameInteractionMechanic
  ): LearningResponseEvaluation {
    val cleanUser = userResponse.trim()
    val cleanTarget = targetPhrase.trim()
    if (cleanUser.isEmpty()) {
      return LearningResponseEvaluation(
        isPassed = false,
        accuracyScore = 0,
        matchedWords = emptyList(),
        missingWords = cleanTarget.split(Regex("\\s+")).filter { it.isNotBlank() },
        feedbackTitle = "No Input Detected",
        detailedExplanation = "Enter or speak your response to practice \"$cleanTarget\".",
        suggestedImprovement = "Listen to the audio first, then try again."
      )
    }

    // Spelling & Listen-and-Type require high orthographic accuracy
    if (mechanic == GameInteractionMechanic.SPELLING_INPUT ||
      mechanic == GameInteractionMechanic.LISTEN_AND_TYPE ||
      mechanic == GameInteractionMechanic.WORDIGO_GRID
    ) {
      val normUser = cleanUser.replace(Regex("[.,!?;:\"'¡¿]"), "").trim()
      val normTarget = cleanTarget.replace(Regex("[.,!?;:\"'¡¿]"), "").trim()
      val exact = normUser.equals(normTarget, ignoreCase = true)
      val sim = (characterSimilarity(normUser.lowercase(), normTarget.lowercase()) * 100f).toInt()
      val passed = if (mechanic == GameInteractionMechanic.LISTEN_AND_TYPE && normTarget.contains(" ")) {
        exact || sim >= 88
      } else {
        exact
      }
      return LearningResponseEvaluation(
        isPassed = passed,
        accuracyScore = if (exact) 100 else if (passed) sim else sim.coerceAtMost(65),
        matchedWords = if (passed) listOf(cleanTarget) else emptyList(),
        missingWords = if (passed) emptyList() else listOf(cleanTarget),
        feedbackTitle = if (passed) "Accurate Transcription! (${if (exact) 100 else sim}%)" else "Check the Letters ($sim%)",
        detailedExplanation = if (passed) "You accurately wrote \"$cleanTarget\"." else "Spoken expression: \"$cleanTarget\" (you typed \"$cleanUser\").",
        suggestedImprovement = if (passed) "Great listening and orthographic recall!" else "Replay at 0.75x speed and listen to each syllable."
      )
    }

    val normalize: (String) -> List<String> = { str ->
      str.lowercase()
        .replace(Regex("[.,!?;:\"'¡¿()\\[\\]]"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    }

    val targetWords = normalize(cleanTarget)
    val userWords = normalize(cleanUser)

    if (targetWords.isEmpty()) {
      val exact = cleanUser.equals(cleanTarget, ignoreCase = true)
      return LearningResponseEvaluation(
        isPassed = exact,
        accuracyScore = if (exact) 100 else 50,
        matchedWords = if (exact) listOf(cleanTarget) else emptyList(),
        missingWords = if (exact) emptyList() else listOf(cleanTarget),
        feedbackTitle = if (exact) "Spot On!" else "Keep Practicing",
        detailedExplanation = "Target: $cleanTarget",
        suggestedImprovement = "Focus on each character."
      )
    }

    // For SCRAMBLE_TILES, enforce word order accuracy!
    if (mechanic == GameInteractionMechanic.SCRAMBLE_TILES) {
      val exactOrder = userWords == targetWords
      val charSim = (characterSimilarity(userWords.joinToString(" "), targetWords.joinToString(" ")) * 100f).toInt()
      return LearningResponseEvaluation(
        isPassed = exactOrder,
        accuracyScore = if (exactOrder) 100 else charSim.coerceAtMost(60),
        matchedWords = if (exactOrder) targetWords else emptyList(),
        missingWords = if (exactOrder) emptyList() else targetWords,
        feedbackTitle = if (exactOrder) "Sentence Built!" else "Word Order Needs Adjustment",
        detailedExplanation = if (exactOrder) "\"$cleanTarget\" is grammatically natural." else "Natural order: \"$cleanTarget\"",
        suggestedImprovement = "Check subject, verb, and time/place placement."
      )
    }

    val matched = mutableListOf<String>()
    val missing = mutableListOf<String>()
    for (tw in targetWords) {
      if (userWords.any { uw -> uw == tw || levenshteinDistance(uw, tw) <= 1 }) {
        matched.add(tw)
      } else {
        missing.add(tw)
      }
    }

    val wordRatio = (matched.size.toFloat() / targetWords.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    val charSim = characterSimilarity(cleanUser.lowercase(), cleanTarget.lowercase())
    val score = ((wordRatio * 0.65f + charSim * 0.35f) * 100f).toInt().coerceIn(0, 100)
    val passed = score >= 70

    val title = when {
      score >= 92 -> "Excellent Clarity! ($score%)"
      score >= 78 -> "Great Job! ($score%)"
      passed -> "Good Attempt! ($score%)"
      else -> "Almost There ($score%)"
    }

    val detail = if (missing.isEmpty()) {
      "You accurately produced \"$cleanTarget\"."
    } else {
      "Matched: ${matched.ifEmpty { listOf("—") }.joinToString(", ")} • Focus on: ${missing.joinToString(", ")}"
    }

    return LearningResponseEvaluation(
      isPassed = passed,
      accuracyScore = score,
      matchedWords = matched,
      missingWords = missing,
      feedbackTitle = title,
      detailedExplanation = detail,
      suggestedImprovement = if (missing.isNotEmpty()) {
        "Practice pronouncing: ${missing.joinToString(", ")}"
      } else {
        "Great cadence and accuracy!"
      }
    )
  }

  private fun characterSimilarity(a: String, b: String): Float {
    if (a == b) return 1f
    val maxLen = max(a.length, b.length)
    if (maxLen == 0) return 1f
    val dist = levenshteinDistance(a, b)
    return (1f - (dist.toFloat() / maxLen.toFloat())).coerceIn(0f, 1f)
  }

  private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
    val lhsLen = lhs.length
    val rhsLen = rhs.length
    var cost = IntArray(lhsLen + 1) { it }
    var newCost = IntArray(lhsLen + 1)
    for (i in 1..rhsLen) {
      newCost[0] = i
      for (j in 1..lhsLen) {
        val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
        val costReplace = cost[j - 1] + match
        val costInsert = cost[j] + 1
        val costDelete = newCost[j - 1] + 1
        newCost[j] = min(min(costInsert, costDelete), costReplace)
      }
      val swap = cost
      cost = newCost
      newCost = swap
    }
    return cost[lhsLen]
  }

  // ==================== CACHE SERIALIZATION (PRESERVES ALL MECHANIC FIELDS) ====================

  private fun saveCachedLevelExercises(
    context: Context,
    cacheKey: String,
    exercises: List<DynamicLevelExercise>
  ) {
    try {
      val arr = JSONArray()
      exercises.forEach { ex ->
        val obj = JSONObject().apply {
          put("id", ex.id)
          put("gameId", ex.gameId)
          put("levelNumber", ex.levelNumber)
          put("mechanic", ex.mechanic.name)
          put("prompt", ex.prompt)
          put("targetPhrase", ex.targetPhrase)
          put("translation", ex.translation)
          put("phonetic", ex.phonetic)
          put("correctIndex", ex.correctIndex)
          put("correctOptionId", ex.correctOptionId)
          put("visualConceptId", ex.visualConceptId)
          put("secondaryAudioPhrase", ex.secondaryAudioPhrase)
          put("dialogueSpeakerALine", ex.dialogueSpeakerALine)
          put("dialogueSpeakerBLine", ex.dialogueSpeakerBLine)
          put("clozeMaskedSentence", ex.clozeMaskedSentence)
          put("illustrationKind", ex.illustrationKind.name)
          put("hint", ex.hint)
          put("explanation", ex.explanation)
          put("conversationStarter", ex.conversationStarter)
          put("conversationRole", ex.conversationRole)
          put("timeLimitSeconds", ex.timeLimitSeconds)
          put("incorrectSentence", ex.incorrectSentence)
          put("errorWord", ex.errorWord)
          put("correctedWord", ex.correctedWord)
          put("escapeStageNumber", ex.escapeStageNumber)
          put("escapeRoomClueCode", ex.escapeRoomClueCode)
          put("objectiveLabel", ex.objectiveLabel)
          put("contentHash", ex.contentHash)
          put("resolvedImageUrl", ex.resolvedImageUrl)
          put("visualIconKey", ex.visualIconKey)

          put("options", JSONArray(ex.options))
          val ansOptsJson = JSONArray()
          ex.resolvedAnswerOptions().forEach { ao ->
            ansOptsJson.put(JSONObject().put("id", ao.id).put("t", ao.text).put("s", ao.subtitle))
          }
          put("answerOptions", ansOptsJson)

          put("scrambledTokens", JSONArray(ex.scrambledTokens))
          put("wordSearchGrid", JSONArray(ex.wordSearchGrid))

          val pairsJson = JSONArray()
          ex.matchingPairs.forEach { (l, r) ->
            pairsJson.put(JSONObject().put("l", l).put("r", r))
          }
          put("matchingPairs", pairsJson)

          val wsJson = JSONArray()
          ex.wordSearchTargets.forEach { (w, c) ->
            wsJson.put(JSONObject().put("w", w).put("c", c))
          }
          put("wordSearchTargets", wsJson)
        }
        arr.put(obj)
      }
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("cache_$cacheKey", arr.toString())
        .apply()
    } catch (_: Exception) {}
  }

  private fun loadCachedLevelExercises(
    context: Context,
    cacheKey: String,
    languageCode: String = "en"
  ): List<DynamicLevelExercise>? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString("cache_$cacheKey", null) ?: return null
    return try {
      val arr = JSONArray(raw)
      val list = mutableListOf<DynamicLevelExercise>()
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        fun JSONArray?.toStrList(): List<String> {
          if (this == null) return emptyList()
          return List(length()) { optString(it) }
        }
        val ansOpts = mutableListOf<ValidatedAnswerOption>()
        obj.optJSONArray("answerOptions")?.let { aoArr ->
          for (j in 0 until aoArr.length()) {
            val ao = aoArr.optJSONObject(j) ?: continue
            val id = ao.optString("id")
            val t = ao.optString("t")
            if (id.isNotBlank() && t.isNotBlank()) {
              ansOpts.add(ValidatedAnswerOption(id = id, text = t, subtitle = ao.optString("s")))
            }
          }
        }
        val pairs = mutableListOf<Pair<String, String>>()
        obj.optJSONArray("matchingPairs")?.let { pArr ->
          for (j in 0 until pArr.length()) {
            val po = pArr.optJSONObject(j) ?: continue
            pairs.add(po.optString("l") to po.optString("r"))
          }
        }
        val wsTargets = mutableListOf<Pair<String, String>>()
        obj.optJSONArray("wordSearchTargets")?.let { wArr ->
          for (j in 0 until wArr.length()) {
            val wo = wArr.optJSONObject(j) ?: continue
            wsTargets.add(wo.optString("w") to wo.optString("c"))
          }
        }
        val mech = runCatching {
          GameInteractionMechanic.valueOf(obj.optString("mechanic"))
        }.getOrDefault(GameInteractionMechanic.GRAMMAR_CORRECTION)
        val ill = runCatching {
          VisualIllustrationKind.valueOf(obj.optString("illustrationKind"))
        }.getOrDefault(VisualIllustrationKind.CROP_TOP_PORTRAIT)

        val candidate = DynamicLevelExercise(
          id = obj.optString("id"),
          gameId = obj.optString("gameId"),
          levelNumber = obj.optInt("levelNumber", 1),
          mechanic = mech,
          prompt = obj.optString("prompt"),
          targetPhrase = obj.optString("targetPhrase"),
          translation = obj.optString("translation"),
          phonetic = obj.optString("phonetic"),
          options = obj.optJSONArray("options").toStrList(),
          correctIndex = obj.optInt("correctIndex", 0),
          answerOptions = ansOpts,
          correctOptionId = obj.optString("correctOptionId"),
          visualConceptId = obj.optString("visualConceptId"),
          secondaryAudioPhrase = obj.optString("secondaryAudioPhrase"),
          dialogueSpeakerALine = obj.optString("dialogueSpeakerALine"),
          dialogueSpeakerBLine = obj.optString("dialogueSpeakerBLine"),
          clozeMaskedSentence = obj.optString("clozeMaskedSentence"),
          scrambledTokens = obj.optJSONArray("scrambledTokens").toStrList(),
          matchingPairs = pairs,
          illustrationKind = ill,
          hint = obj.optString("hint"),
          explanation = obj.optString("explanation"),
          conversationStarter = obj.optString("conversationStarter"),
          conversationRole = obj.optString("conversationRole"),
          timeLimitSeconds = obj.optInt("timeLimitSeconds", 30),
          incorrectSentence = obj.optString("incorrectSentence"),
          errorWord = obj.optString("errorWord"),
          correctedWord = obj.optString("correctedWord"),
          wordSearchGrid = obj.optJSONArray("wordSearchGrid").toStrList(),
          wordSearchTargets = wsTargets,
          escapeStageNumber = obj.optInt("escapeStageNumber", 1),
          escapeRoomClueCode = obj.optString("escapeRoomClueCode"),
          objectiveLabel = obj.optString("objectiveLabel"),
          contentHash = obj.optString("contentHash"),
          resolvedImageUrl = obj.optString("resolvedImageUrl"),
          visualIconKey = obj.optString("visualIconKey")
        )
        if (candidate.visualConceptId.startsWith("dyn:", ignoreCase = true)) {
          VerifiedVisualConceptRegistry.restoreDynamicConcept(
            conceptId = candidate.visualConceptId,
            englishMeaning = candidate.translation,
            targetPhrase = candidate.targetPhrase,
            languageCode = languageCode,
            imageUrl = candidate.resolvedImageUrl,
            iconKey = candidate.visualIconKey,
            domainLabel = candidate.objectiveLabel,
            prompt = candidate.prompt,
            explanation = candidate.explanation,
            levelNumber = candidate.levelNumber
          )
        }
        if (validateExerciseIntegrity(candidate)) {
          list.add(candidate)
        }
      }
      list
    } catch (_: Exception) {
      null
    }
  }
}
