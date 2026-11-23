package com.example.data.model

import kotlin.math.roundToInt

// ============================================================================
// LINGOSPHERE 3.0 — NEXT GENERATION INTELLIGENT LANGUAGE LEARNING MODELS
// ============================================================================

/**
 * 1. AI Language Memory
 * Remembers each learner's vocabulary weaknesses, repeated grammar mistakes,
 * pronunciation difficulties, conversation history, and learning goals.
 */
data class AiLanguageMemoryProfile(
    val targetLanguage: String = "Spanish",
    val cefrLevel: String = "A2",
    val learningGoals: List<String> = listOf(
        "Hold natural 5-minute conversations with native speakers",
        "Travel and order food confidently without switching to English",
        "Master past and subjunctive tenses in everyday stories"
    ),
    val vocabularyWeaknesses: List<String> = emptyList(),
    val repeatedGrammarMistakes: List<RepeatedMistakeSummary> = emptyList(),
    val pronunciationDifficulties: List<String> = emptyList(),
    val conversationHistorySummaries: List<String> = emptyList(),
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
) {
    /**
     * Generates a compact, structured context block injected into GeminiTutorService
     * prompts so future lessons and conversations are personalized automatically.
     */
    fun toPersonalizationPrompt(): String {
        val goalsText = if (learningGoals.isNotEmpty()) learningGoals.joinToString("; ") else "General fluency"
        val vocabText = if (vocabularyWeaknesses.isNotEmpty()) vocabularyWeaknesses.take(6).joinToString(", ") else "None recorded yet"
        val grammarText = if (repeatedGrammarMistakes.isNotEmpty()) {
            repeatedGrammarMistakes.take(4).joinToString("; ") { "${it.category} (${it.exampleWrong} -> ${it.exampleCorrected}, ${it.occurrenceCount}x)" }
        } else "None recorded yet"
        val pronText = if (pronunciationDifficulties.isNotEmpty()) pronunciationDifficulties.take(5).joinToString(", ") else "Clear baseline"
        val historyText = if (conversationHistorySummaries.isNotEmpty()) conversationHistorySummaries.take(3).joinToString(" | ") else "First session"

        return """
            [AI LANGUAGE MEMORY — PERSONALIZATION CONTEXT]
            Target Language: $targetLanguage ($cefrLevel)
            Learner Goals: $goalsText
            Vocabulary Weaknesses to Reinforce: $vocabText
            Repeated Grammar Mistakes to Gently Correct: $grammarText
            Pronunciation / Phoneme Difficulties: $pronText
            Recent Conversation Memory: $historyText
            Instruction: Weave 1-2 of the learner's weak vocabulary items or grammar structures naturally into your response and align examples with their stated goals.
        """.trimIndent()
    }
}

data class RepeatedMistakeSummary(
    val id: String,
    val category: String,
    val ruleSummary: String,
    val exampleWrong: String,
    val exampleCorrected: String,
    val occurrenceCount: Int = 1,
    val recoveryAttempts: Int = 0,
    val masteryScore: Int = 0, // 0..100 tracking whether learner has actually improved
    val improved: Boolean = false,
    val targetLanguage: String = ""
)

/**
 * 2. Live Conversation Assistant
 * During language exchange calls, provides optional live translated captions,
 * unfamiliar vocabulary explanations, and suggested phrases.
 */
data class LiveCallCaptionItem(
    val id: String = "cap_${System.currentTimeMillis()}",
    val speakerName: String,
    val originalText: String,
    val translatedCaption: String,
    val language: String,
    val timestampLabel: String = "Just now"
)

data class LiveCallVocabExplanation(
    val term: String,
    val translation: String,
    val phoneticHint: String,
    val contextNote: String,
    val exampleSentence: String
)

data class LiveCallAssistantState(
    val captionsEnabled: Boolean = true,
    val liveCaptions: List<LiveCallCaptionItem> = emptyList(),
    val unfamiliarVocabulary: List<LiveCallVocabExplanation> = emptyList(),
    val suggestedPhrases: List<SuggestedCallPhrase> = emptyList(),
    val savedExpressions: Set<String> = emptySet(),
    val isAnalyzing: Boolean = false
)

data class SuggestedCallPhrase(
    val targetPhrase: String,
    val nativeMeaning: String,
    val pronunciationGuide: String,
    val usageTip: String
)

/**
 * 3. Speak From Real Life
 * Extends camera recognition with practical speaking exercises and real speech evaluation.
 */
data class RealLifeSpeakingExercise(
    val id: String,
    val itemTerm: String,
    val promptInstruction: String,
    val targetSentenceToSpeak: String,
    val phoneticGuide: String,
    val englishTranslation: String,
    val contextualExplanation: String
)

data class RealLifeSpeakingEvaluation(
    val exerciseId: String,
    val spokenTranscript: String,
    val overallScore: Int,
    val pronunciationScore: Int,
    val contextualAccuracyScore: Int,
    val feedbackSummary: String,
    val improvedNativeVersion: String,
    val passed: Boolean
)

/**
 * 4. AI Immersion Mode
 * Simulated everyday situations entirely in the target language where AI characters
 * respond dynamically to user speech and decisions.
 */
data class ImmersionScenario(
    val id: String,
    val title: String,
    val cityAndCountry: String,
    val targetLanguage: String,
    val cefrLevel: String,
    val characterName: String,
    val characterRole: String,
    val settingDescriptionInTargetLang: String,
    val openingLineInTargetLang: String,
    val openingLinePhonetic: String,
    val missionGoalInTargetLang: String,
    val decisionBranchesInTargetLang: List<String>,
    val culturalContextNote: String
)

data class ImmersionDialogueTurn(
    val id: String = "imm_${System.currentTimeMillis()}",
    val speaker: String, // "character" or "user"
    val targetLanguageText: String,
    val phoneticHint: String = "",
    val decisionImpactNote: String = "",
    val naturalnessScore: Int? = null,
    val subtleCorrectionInTargetLang: String? = null
)

data class ImmersionDynamicResponse(
    val characterReplyInTargetLang: String,
    val phoneticHint: String,
    val sceneProgressionNoteInTargetLang: String,
    val nextDecisionOptionsInTargetLang: List<String>,
    val userNaturalnessScore: Int,
    val subtleCorrectionInTargetLang: String,
    val goalProgressPercent: Int
)

/**
 * 5. Intelligent Mistake Recovery
 * Personalized exercises targeting repeated mistakes + improvement tracking.
 */
data class MistakeRecoveryExercise(
    val id: String,
    val mistakeId: String,
    val mistakeCategory: String,
    val targetLanguage: String,
    val promptQuestion: String,
    val incorrectHabitHint: String,
    val expectedCorrectSentence: String,
    val alternateAcceptedAnswers: List<String> = emptyList(),
    val explanation: String
)

data class MistakeRecoveryResult(
    val mistakeId: String,
    val userResponse: String,
    val isCorrect: Boolean,
    val score: Int,
    val coachingFeedback: String,
    val previousMasteryScore: Int,
    val updatedMasteryScore: Int,
    val hasImproved: Boolean
)

/**
 * 6. Daily Life Language Missions
 * Practical missions based on user's language level assessed via real speech or written responses.
 */
data class DailyLifeMission(
    val id: String,
    val title: String,
    val promptDescription: String,
    val cefrLevel: String,
    val targetLanguage: String,
    val recommendedDurationSeconds: Int,
    val requiredElements: List<String>,
    val starterPhrasesInTargetLang: List<String>,
    val xpReward: Int = 60
)

data class DailyLifeMissionEvaluation(
    val missionId: String,
    val missionTitle: String,
    val targetLanguage: String,
    val submissionMode: String, // "Spoken" or "Written"
    val userResponseText: String,
    val overallScore: Int,
    val fluencyAndCoherenceScore: Int,
    val grammarAndVocabScore: Int,
    val stayedInTargetLanguage: Boolean,
    val completedSuccessfully: Boolean,
    val aiFeedback: String,
    val improvedModelResponse: String,
    val keyCorrections: List<String>,
    val completedAtEpochMs: Long = System.currentTimeMillis(),
    val audioSampleFilePath: String? = null
)

/**
 * 7. AI Conversation Replay
 * Post-session replay with transcripts where users tap individual sentences,
 * hear improved pronunciation, understand corrections, and practice their own responses again.
 */
data class ConversationReplaySession(
    val id: String,
    val scenarioTitle: String,
    val targetLanguage: String,
    val cefrLevel: String,
    val timestampLabel: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val overallScore: Int,
    val turns: List<ReplayTurnItem>
)

data class ReplayTurnItem(
    val turnId: String,
    val speaker: String, // "user" or "tutor"
    val originalText: String,
    val improvedNativeVersion: String,
    val pronunciationIpaOrPhonetic: String,
    val correctionExplanation: String,
    val initialScore: Int = 82,
    val practicedRetryScore: Int? = null,
    val lastPracticedTranscript: String? = null
)

/**
 * 8. Accent Understanding Studio
 * Regional accents with authentic listening examples, adjustable playback speed,
 * comprehension questions, and regional vocabulary explanations.
 */
data class RegionalAccentSample(
    val id: String,
    val language: String,
    val regionName: String,
    val countryFlag: String,
    val accentCharacteristics: String,
    val respectNote: String,
    val transcriptInTargetLang: String,
    val englishTranslation: String,
    val phoneticFeatures: List<String>,
    val regionalVocabulary: List<RegionalVocabNote>,
    val comprehensionQuestion: String,
    val comprehensionOptions: List<String>,
    val correctOptionIndex: Int,
    val comprehensionExplanation: String
)

data class RegionalVocabNote(
    val regionalTerm: String,
    val standardEquivalent: String,
    val meaning: String,
    val exampleUsage: String
)

/**
 * 9. Personal Language Portfolio
 * Recorded speaking samples (with earlier vs recent comparison), writing improvements,
 * completed missions, vocabulary mastery, and privacy controls.
 */
data class PortfolioSpeakingSample(
    val id: String,
    val title: String,
    val targetLanguage: String,
    val cefrLevel: String,
    val spokenTranscript: String,
    val improvedVersion: String,
    val pronunciationScore: Int,
    val fluencyScore: Int,
    val durationSeconds: Int,
    val recordedAtEpochMs: Long,
    val dateLabel: String,
    val audioFilePath: String? = null,
    val isPrivate: Boolean = true
)

data class PortfolioWritingSample(
    val id: String,
    val promptTitle: String,
    val targetLanguage: String,
    val originalDraft: String,
    val improvedRevision: String,
    val grammarGainScore: Int,
    val keyImprovements: List<String>,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val dateLabel: String = "Today"
)

data class PortfolioPrivacySettings(
    val defaultPrivateRecordings: Boolean = true,
    val allowProgressSharingInCommunity: Boolean = false,
    val storeAudioOnDeviceOnly: Boolean = true
)

/**
 * 10. Adaptive Learning Engine
 * Automatically adjusts lesson difficulty and review frequency based on actual performance,
 * prioritizing weak skills without forcing users to repeat mastered content.
 */
data class AdaptiveRecommendationPlan(
    val targetLanguage: String,
    val currentCefrLevel: String,
    val recommendedCefrDifficulty: String,
    val difficultyAdjustmentReason: String,
    val reviewIntervalMultiplier: Float,
    val reviewFrequencyLabel: String,
    val weakestSkill: String,
    val strongestSkill: String,
    val skippedMasteredTopics: List<String>,
    val prioritizedNextActions: List<AdaptiveActionItem>,
    val skillScores: Map<String, Int>
)

data class AdaptiveActionItem(
    val id: String,
    val title: String,
    val skillCategory: String, // "Vocabulary", "Grammar Recovery", "Pronunciation", "Immersion Speaking", "Daily Mission"
    val targetDifficulty: String,
    val estimatedMinutes: Int,
    val reason: String,
    val actionRouteKey: String
)

object AdaptiveLearningEngine {
    fun computeAdaptivePlan(
        targetLanguage: String,
        currentLevel: String,
        completedLessonsCount: Int,
        averageQuizScore: Int,
        averagePronunciationScore: Int,
        averageFluencyScore: Int,
        dueSrsCount: Int,
        masteredVocabularyCount: Int,
        unresolvedMistakes: List<RepeatedMistakeSummary>,
        weakVocabulary: List<String>
    ): AdaptiveRecommendationPlan {
        val cefrOrder = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val normalizedCurrent = cefrOrder.firstOrNull { currentLevel.uppercase().startsWith(it) } ?: "A2"
        val currentIdx = cefrOrder.indexOf(normalizedCurrent).coerceAtLeast(0)

        val grammarScore = if (unresolvedMistakes.isEmpty()) {
            88
        } else {
            val avgMistakeMastery = unresolvedMistakes.map { it.masteryScore }.average().roundToInt()
            (62 + (avgMistakeMastery * 0.35f).roundToInt()).coerceIn(35, 95)
        }
        val vocabScore = when {
            dueSrsCount > 12 -> 58
            weakVocabulary.size >= 5 -> 66
            masteredVocabularyCount >= 20 -> 92
            else -> 80
        }
        val pronScore = if (averagePronunciationScore > 0) averagePronunciationScore else 78
        val speakingScore = if (averageFluencyScore > 0) averageFluencyScore else 76

        val compositePerformance = ((averageQuizScore.coerceAtLeast(75) * 0.25f) +
            (grammarScore * 0.25f) +
            (vocabScore * 0.25f) +
            (((pronScore + speakingScore) / 2f) * 0.25f)).roundToInt()

        val recommendedCefr = when {
            compositePerformance >= 86 && completedLessonsCount >= 2 ->
                cefrOrder.getOrElse((currentIdx + 1).coerceAtMost(cefrOrder.lastIndex)) { normalizedCurrent }
            compositePerformance < 62 && currentIdx > 0 ->
                cefrOrder[currentIdx - 1]
            else -> normalizedCurrent
        }

        val adjustmentReason = when {
            recommendedCefr != normalizedCurrent && compositePerformance >= 86 ->
                "Elevated from $normalizedCurrent to $recommendedCefr because your composite accuracy is $compositePerformance% across quizzes, speaking, and SRS retention."
            recommendedCefr != normalizedCurrent && compositePerformance < 62 ->
                "Consolidated to $recommendedCefr to reinforce foundational structures before advancing."
            else ->
                "Calibrated at $recommendedCefr with targeted micro-drills for your lowest-scoring skill dimension ($compositePerformance% overall mastery)."
        }

        val reviewMultiplier = when {
            compositePerformance >= 88 -> 1.35f
            compositePerformance >= 75 -> 1.0f
            else -> 0.65f
        }
        val reviewFrequencyLabel = when {
            reviewMultiplier > 1.15f -> "Accelerated Spacing (1.35x interval — skip redundant reviews)"
            reviewMultiplier < 0.85f -> "High-Retention Boost (0.65x interval — focus on lapsed items)"
            else -> "Balanced Spaced Repetition (1.0x optimal curve)"
        }

        val skillMap = linkedMapOf(
            "Vocabulary Retention" to vocabScore,
            "Grammar Precision" to grammarScore,
            "Pronunciation Clarity" to pronScore,
            "Spontaneous Fluency" to speakingScore
        )
        val sortedSkills = skillMap.entries.sortedBy { it.value }
        val weakestSkill = sortedSkills.first().key
        val strongestSkill = sortedSkills.last().key

        val skippedMastered = buildList {
            if (masteredVocabularyCount >= 5 || vocabScore >= 82) {
                add("$normalizedCurrent Core Greetings & Survival Phrases (Mastered)")
            }
            if (averageQuizScore >= 85 || completedLessonsCount >= 3) {
                add("$normalizedCurrent Basic Present Tense Recognition Drills (Mastered)")
            }
            if (pronScore >= 85) {
                add("Introductory Vowel & Syllable Isolation Drills (Mastered)")
            }
            if (isEmpty()) {
                add("Foundational Alphabet & Cognate Warmups (Mastered)")
            }
        }

        val actions = buildList {
            if (unresolvedMistakes.isNotEmpty()) {
                val topMistake = unresolvedMistakes.first()
                add(
                    AdaptiveActionItem(
                        id = "act_mistake_recovery",
                        title = "Fix Repeated Pattern: ${topMistake.category}",
                        skillCategory = "Grammar Recovery",
                        targetDifficulty = recommendedCefr,
                        estimatedMinutes = 4,
                        reason = "Occurred ${topMistake.occurrenceCount}x in recent sessions (${topMistake.exampleWrong} → ${topMistake.exampleCorrected}).",
                        actionRouteKey = "mistake_recovery"
                    )
                )
            }
            if (weakVocabulary.isNotEmpty() || dueSrsCount > 0) {
                add(
                    AdaptiveActionItem(
                        id = "act_weak_vocab",
                        title = "Reinforce Weak Vocabulary (${weakVocabulary.take(3).joinToString(", ").ifBlank { "$dueSrsCount due cards" }})",
                        skillCategory = "Vocabulary",
                        targetDifficulty = recommendedCefr,
                        estimatedMinutes = 5,
                        reason = "Prioritizes low-ease SRS cards while skipping $masteredVocabularyCount mastered words.",
                        actionRouteKey = "practice_srs"
                    )
                )
            }
            if (pronScore < 82) {
                add(
                    AdaptiveActionItem(
                        id = "act_pronunciation",
                        title = "Phoneme & Prosody Precision Drill",
                        skillCategory = "Pronunciation",
                        targetDifficulty = recommendedCefr,
                        estimatedMinutes = 5,
                        reason = "Your pronunciation clarity ($pronScore%) is currently below your grammar & reading level.",
                        actionRouteKey = "speak_pronunciation"
                    )
                )
            }
            add(
                AdaptiveActionItem(
                    id = "act_immersion",
                    title = "100% $targetLanguage Immersion Simulation",
                    skillCategory = "Immersion Speaking",
                    targetDifficulty = recommendedCefr,
                    estimatedMinutes = 6,
                    reason = "Applies your active vocabulary in a dynamic real-world scenario at $recommendedCefr difficulty.",
                    actionRouteKey = "immersion_mode"
                )
            )
            add(
                AdaptiveActionItem(
                    id = "act_daily_mission",
                    title = "Daily Life Spoken/Written Mission ($recommendedCefr)",
                    skillCategory = "Daily Mission",
                    targetDifficulty = recommendedCefr,
                    estimatedMinutes = 3,
                    reason = "Tests unscripted real-life output without relying on multiple-choice hints.",
                    actionRouteKey = "daily_mission"
                )
            )
        }

        return AdaptiveRecommendationPlan(
            targetLanguage = targetLanguage,
            currentCefrLevel = normalizedCurrent,
            recommendedCefrDifficulty = recommendedCefr,
            difficultyAdjustmentReason = adjustmentReason,
            reviewIntervalMultiplier = reviewMultiplier,
            reviewFrequencyLabel = reviewFrequencyLabel,
            weakestSkill = weakestSkill,
            strongestSkill = strongestSkill,
            skippedMasteredTopics = skippedMastered,
            prioritizedNextActions = actions,
            skillScores = skillMap
        )
    }
}

/**
 * Curated catalogs for Immersion Mode, Daily Life Missions, and Accent Understanding Studio
 * so every target language has rich, authentic content ready immediately.
 */
object LingoSphere3Catalog {

    fun immersionScenariosForLanguage(language: String): List<ImmersionScenario> {
        return when (language.lowercase()) {
            "japanese" -> listOf(
                ImmersionScenario(
                    id = "imm_tokyo_shopping",
                    title = "Shopping in Tokyo (Shibuya Boutique)",
                    cityAndCountry = "Tokyo, Japan 🇯🇵",
                    targetLanguage = "Japanese",
                    cefrLevel = "A2-B1",
                    characterName = "Takahashi-san",
                    characterRole = "Shibuya Boutique Clerk",
                    settingDescriptionInTargetLang = "渋谷のセレクトショップ。店員の高橋さんが笑顔で近づいてきます。",
                    openingLineInTargetLang = "いらっしゃいませ！何か特別なものをお探しですか？春の新作ジャケットも入荷していますよ。",
                    openingLinePhonetic = "Irasshaimase! Nanika tokubetsu na mono o osagashi desu ka? Haru no shinsaku jaketto mo nyuuka shite imasu yo.",
                    missionGoalInTargetLang = "サイズを尋ねて、試着室を使えるか聞いて、値段を確認しましょう。",
                    decisionBranchesInTargetLang = listOf(
                        "このジャケットのMサイズはありますか？試着してもいいですか？",
                        "別の色（黒やネイビー）はありますか？",
                        "免税（タックスフリー）の手続きはできますか？"
                    ),
                    culturalContextNote = "In Japanese shops, always ask 'Shichaku shitemo ii desu ka?' before entering a fitting room and remove your shoes."
                ),
                ImmersionScenario(
                    id = "imm_kyoto_izakaya",
                    title = "Ordering at a Kyoto Izakaya",
                    cityAndCountry = "Kyoto, Japan 🇯🇵",
                    targetLanguage = "Japanese",
                    cefrLevel = "B1",
                    characterName = "Kenji-san",
                    characterRole = "Izakaya Master",
                    settingDescriptionInTargetLang = "京都・先斗町の賑やかな居酒屋のカウンター席です。",
                    openingLineInTargetLang = "いらっしゃい！まずはお飲み物、何にしましょうか？本日のおすすめは京野菜の天ぷらですよ。",
                    openingLinePhonetic = "Irasshai! Mazu wa onomimono, nani ni shimashou ka? Honjitsu no osusume wa Kyo-yasai no tenpura desu yo.",
                    missionGoalInTargetLang = "飲み物と料理を2品注文し、アレルギーや辛さの好みを伝えましょう。",
                    decisionBranchesInTargetLang = listOf(
                        "生ビールを一つと、京野菜の天ぷらをお願いします。",
                        "おすすめの焼き鳥盛り合わせはどれですか？",
                        "温かいお茶と、ベジタリアン向けの料理はありますか？"
                    ),
                    culturalContextNote = "Izakayas often serve a small appetizer ('otoshi') automatically as a table seating charge."
                ),
                ImmersionScenario(
                    id = "imm_tokyo_interview",
                    title = "Tech Studio Interview in Roppongi",
                    cityAndCountry = "Tokyo, Japan 🇯🇵",
                    targetLanguage = "Japanese",
                    cefrLevel = "B2",
                    characterName = "Sato-bucho",
                    characterRole = "Hiring Director",
                    settingDescriptionInTargetLang = "六本木のデザイン・IT企業の会議室での面接です。",
                    openingLineInTargetLang = "本日はお越しいただきありがとうございます。まずは1分ほどで自己紹介と、これまでのご経験を教えていただけますか？",
                    openingLinePhonetic = "Honjitsu wa okoshi itadaki arigatou gozaimasu. Mazu wa ippun hodo de jikoshoukai to, kore made no gokeiken o oshiete itadakemasu ka?",
                    missionGoalInTargetLang = "丁寧な敬語（です・ます調）で自己紹介と強みを伝えましょう。",
                    decisionBranchesInTargetLang = listOf(
                        "はい、私は3年間プロジェクト管理を担当し、国際チームの連携を強みとしています。",
                        "御社のグローバル製品開発に魅力を感じて応募いたしました。",
                        "新しい技術を素早く学び、チームで課題を解決することが得意です。"
                    ),
                    culturalContextNote = "Use polite 'desu/masu' and 'watashi' consistently during Japanese professional interviews."
                )
            )
            "french" -> listOf(
                ImmersionScenario(
                    id = "imm_paris_bistro",
                    title = "Ordering Food in Paris (Le Marais Bistro)",
                    cityAndCountry = "Paris, France 🇫🇷",
                    targetLanguage = "French",
                    cefrLevel = "A2-B1",
                    characterName = "Camille",
                    characterRole = "Serveuse au Bistro du Marais",
                    settingDescriptionInTargetLang = "Vous êtes installé en terrasse dans un bistro parisien animé du Marais.",
                    openingLineInTargetLang = "Bonjour ! Bienvenue au Petit Marais. Voici l'ardoise du jour : nous avons le magret de canard ou le risotto aux champignons. Qu'est-ce qui vous ferait plaisir ?",
                    openingLinePhonetic = "Bohn-zhoor! Byahn-vuh-new oh Puh-tee Mah-reh. Vwah-see lar-dwahz doo zhoor...",
                    missionGoalInTargetLang = "Commandez un plat principal, demandez une carafe d'eau et précisez une préférence alimentaire.",
                    decisionBranchesInTargetLang = listOf(
                        "Bonjour ! Je vais prendre le risotto aux champignons et une carafe d'eau, s'il vous plaît.",
                        "Quels sont les ingrédients de la sauce du magret de canard ?",
                        "Avez-vous une formule déjeuner avec entrée et plat ?"
                    ),
                    culturalContextNote = "Ask for 'une carafe d'eau' in France for complimentary tap water instead of bottled mineral water."
                ),
                ImmersionScenario(
                    id = "imm_lyon_boutique",
                    title = "Shopping at a Vintage Market in Lyon",
                    cityAndCountry = "Lyon, France 🇫🇷",
                    targetLanguage = "French",
                    cefrLevel = "B1",
                    characterName = "Lucas",
                    characterRole = "Marchand d'artisanat",
                    settingDescriptionInTargetLang = "Un marché artisanal sur les quais de Saône à Lyon.",
                    openingLineInTargetLang = "Bonjour ! Toutes ces écharpes en soie sont tissées dans la région lyonnaise. Vous cherchez un cadeau ou quelque chose pour vous ?",
                    openingLinePhonetic = "Bohn-zhoor! Toot sez ay-sharp ahn swah sohn tee-say dahn lah ray-zhyohn lee-oh-nez.",
                    missionGoalInTargetLang = "Demandez le prix, la matière et négociez poliment un coffret cadeau.",
                    decisionBranchesInTargetLang = listOf(
                        "Je cherche un cadeau pour ma sœur. Quel est le prix de l'écharpe bleue ?",
                        "Est-ce que c'est cent pour cent soie naturelle ?",
                        "Puis-je payer par carte bancaire avec un emballage cadeau ?"
                    ),
                    culturalContextNote = "Always begin interactions with French shopkeepers with a clear 'Bonjour, Monsieur/Madame'."
                ),
                ImmersionScenario(
                    id = "imm_paris_interview",
                    title = "Creative Agency Interview in Paris",
                    cityAndCountry = "Paris, France 🇫🇷",
                    targetLanguage = "French",
                    cefrLevel = "B2",
                    characterName = "Claire Moreau",
                    characterRole = "Directrice des Ressources Humaines",
                    settingDescriptionInTargetLang = "Entretien dans les bureaux d'une agence internationale près de l'Opéra à Paris.",
                    openingLineInTargetLang = "Bonjour et bienvenue. Pour commencer, pourriez-vous me présenter votre parcours et ce qui vous motive à rejoindre notre équipe à Paris ?",
                    openingLinePhonetic = "Bohn-zhoor ay byahn-vuh-new. Poor koh-mahn-say, poo-ryay voo muh pray-zahn-tay vohtr par-koor...",
                    missionGoalInTargetLang = "Présentez votre expérience professionnelle et posez une question pertinente sur le poste.",
                    decisionBranchesInTargetLang = listOf(
                        "Avec plaisir. J'ai travaillé sur plusieurs projets internationaux où j'ai développé ma capacité d'adaptation.",
                        "Ce qui me motive particulièrement, c'est votre approche innovante et multiculturelle.",
                        "Pourriez-vous m'en dire plus sur les priorités de l'équipe pour les six prochains mois ?"
                    ),
                    culturalContextNote = "Maintain formal 'vous' throughout French job interviews unless explicitly invited otherwise."
                )
            )
            "english" -> listOf(
                ImmersionScenario(
                    id = "imm_london_interview",
                    title = "Attending a Job Interview in London",
                    cityAndCountry = "London, UK 🇬🇧",
                    targetLanguage = "English",
                    cefrLevel = "B1-B2",
                    characterName = "Eleanor Vance",
                    characterRole = "Lead Hiring Manager (Shoreditch Tech Hub)",
                    settingDescriptionInTargetLang = "You are seated in a glass meeting room overlooking Old Street Roundabout in London.",
                    openingLineInTargetLang = "Lovely to meet you in person! Before we dive into the role specifics, could you walk me through a recent challenge you solved and how you approached it?",
                    openingLinePhonetic = "LUHV-lee tuh meet yoo in PUR-suhn! Bih-FOR wee dahyv in-tuh thuh rohl...",
                    missionGoalInTargetLang = "Explain a concrete challenge, the action you took, and the measurable result.",
                    decisionBranchesInTargetLang = listOf(
                        "Certainly! In my previous project, we faced a tight deadline, so I reorganized our daily priorities and delivered on schedule.",
                        "I'd love to share how I collaborated across departments to resolve a tricky customer issue.",
                        "I focused on clear communication and iterative testing to improve our team's workflow by twenty percent."
                    ),
                    culturalContextNote = "British interviewers value structured STAR answers (Situation, Task, Action, Result) delivered with collaborative tone."
                ),
                ImmersionScenario(
                    id = "imm_london_borough",
                    title = "Ordering Artisan Street Food at Borough Market",
                    cityAndCountry = "London, UK 🇬🇧",
                    targetLanguage = "English",
                    cefrLevel = "A2-B1",
                    characterName = "Alfie",
                    characterRole = "Borough Market Stall Chef",
                    settingDescriptionInTargetLang = "A bustling Saturday afternoon under the historic arches of Borough Market in London.",
                    openingLineInTargetLang = "Afternoon, mate! Fresh sourdough toasties coming right off the grill. Would you fancy the aged cheddar with caramelized onion, or our spicy mushroom special?",
                    openingLinePhonetic = "Ahf-tuh-NOON, mayt! Fresh SOW-uh-doh TOH-steez KUH-ming rayt awf thuh gril.",
                    missionGoalInTargetLang = "Order a meal, ask about ingredients, and confirm contactless payment.",
                    decisionBranchesInTargetLang = listOf(
                        "The aged cheddar with caramelized onion sounds brilliant! Does it come with any side salad?",
                        "How spicy is the mushroom special? I'd also love an iced coffee if you have one.",
                        "I'll go for the cheddar toastie, please. Can I tap with my card?"
                    ),
                    culturalContextNote = "'Would you fancy...?' is a common British way of asking 'Would you like...?'"
                )
            )
            "spanish" -> listOf(
                ImmersionScenario(
                    id = "imm_madrid_tapas",
                    title = "Ordering Tapas in Madrid (La Latina)",
                    cityAndCountry = "Madrid, Spain 🇪🇸",
                    targetLanguage = language,
                    cefrLevel = "A2-B1",
                    characterName = "Mateo",
                    characterRole = "Camarero en Taberna La Latina",
                    settingDescriptionInTargetLang = "Estás en una taberna tradicional en el barrio de La Latina en Madrid.",
                    openingLineInTargetLang = "¡Hola, buenas tardes! Hoy tenemos tortilla recién hecha, gambas al ajillo y croquetas caseras de jamón. ¿Qué os pongo para empezar?",
                    openingLinePhonetic = "OH-lah, BWEH-nahs TAR-dehs! Oy teh-NEH-mohs tor-TEE-yah reh-SYEN EH-chah...",
                    missionGoalInTargetLang = "Pide dos raciones para compartir, una bebida y pregunta si algún plato lleva frutos secos.",
                    decisionBranchesInTargetLang = listOf(
                        "¡Buenas! Nos pones una ración de croquetas y un pincho de tortilla, por favor.",
                        "¿Las gambas al ajillo pican mucho? También queríamos dos aguas con gas.",
                        "¿Alguna de las tapas lleva frutos secos? Tengo una pequeña alergia."
                    ),
                    culturalContextNote = "In Madrid, ordering 'una ración' is a full sharing plate, while 'una tapa' or 'un pincho' is an individual portion."
                ),
                ImmersionScenario(
                    id = "imm_mexico_mercado",
                    title = "Shopping at Coyoacán Market in Mexico City",
                    cityAndCountry = "Mexico City, Mexico 🇲🇽",
                    targetLanguage = language,
                    cefrLevel = "A2-B1",
                    characterName = "Doña Elena",
                    characterRole = "Artesana del Mercado de Coyoacán",
                    settingDescriptionInTargetLang = "Un puesto lleno de cerámica pintada a mano y textiles en Coyoacán.",
                    openingLineInTargetLang = "¡Pásele, pásele con confianza! Todo está hecho a mano en Oaxaca y Puebla. ¿Qué anda buscando hoy?",
                    openingLinePhonetic = "PAH-seh-leh kon kon-FYAHN-sah! TOH-doh ehs-TAH EH-choh ah MAH-noh...",
                    missionGoalInTargetLang = "Pregunta el precio de dos piezas, pide envolverlas para viaje y confirma el pago.",
                    decisionBranchesInTargetLang = listOf(
                        "¡Qué bonitas tazas! ¿En cuánto salen si me llevo el par?",
                        "¿Las puede envolver bien para llevarlas en la maleta del avión?",
                        "Me encantan los colores de este mantel. ¿De qué región es el bordado?"
                    ),
                    culturalContextNote = "'¿En cuánto sale?' is a natural Mexican Spanish way to ask 'How much does it cost?'"
                ),
                ImmersionScenario(
                    id = "imm_barcelona_interview",
                    title = "Design & Tech Interview in Barcelona",
                    cityAndCountry = "Barcelona, Spain 🇪🇸",
                    targetLanguage = language,
                    cefrLevel = "B2",
                    characterName = "Lucía Serra",
                    characterRole = "Directora de Producto en el 22@",
                    settingDescriptionInTargetLang = "Entrevista profesional en el distrito tecnológico 22@ de Barcelona.",
                    openingLineInTargetLang = "Hola, encantada de saludarte. Cuéntame en un minuto tu experiencia más relevante y cómo te organizas cuando trabajas con equipos internacionales.",
                    openingLinePhonetic = "OH-lah, ehn-kahn-TAH-dah deh sah-loo-DAR-teh. KWEN-tah-meh ehn oon mee-NOO-toh...",
                    missionGoalInTargetLang = "Explica tu experiencia, da un ejemplo concreto de trabajo en equipo y haz una pregunta final.",
                    decisionBranchesInTargetLang = listOf(
                        "Muchas gracias. Durante los últimos años he coordinado proyectos digitales priorizando la comunicación clara.",
                        "Me adapto rápido a equipos multiculturales y siempre documento los acuerdos clave.",
                        "¿Cuáles serían los principales objetivos del puesto durante el primer trimestre?"
                    ),
                    culturalContextNote = "In Spain, modern tech companies often use 'tú' even in interviews while keeping vocabulary professional."
                )
            )
            "german" -> listOf(
                ImmersionScenario(
                    id = "imm_berlin_cafe",
                    title = "Ordering Coffee & Pastries in Berlin",
                    cityAndCountry = "Berlin, Germany 🇩🇪",
                    targetLanguage = "German",
                    cefrLevel = "A2-B1",
                    characterName = "Lukas",
                    characterRole = "Barista im Café Kreuzberg",
                    settingDescriptionInTargetLang = "Du stehst in einem gemütlichen Café in Berlin-Kreuzberg vor der Kuchentheke.",
                    openingLineInTargetLang = "Guten Tag! Heute haben wir frischen Apfelstrudel und Käsekuchen. Was darf es sein?",
                    openingLinePhonetic = "GOO-ten tahk! HOY-teh HAH-ben veer FRISH-en AP-fel-shtroo-del...",
                    missionGoalInTargetLang = "Bestelle ein Getränk mit Hafermilch, frage nach dem Kuchen und bezahle mit Karte.",
                    decisionBranchesInTargetLang = listOf(
                        "Hallo! Ich hätte gern einen Cappuccino mit Hafermilch und ein Stück Apfelstrudel.",
                        "Ist der Käsekuchen heute frisch gebacken? Kann ich mit Karte zahlen?",
                        "Zum Mitnehmen oder zum Hiertrinken? Ich bleibe gern hier."
                    ),
                    culturalContextNote = "In Germany, 'Zum Hiertrinken oder zum Mitnehmen?' asks whether you are staying or taking your order to go."
                ),
                ImmersionScenario(
                    id = "imm_munich_station",
                    title = "Asking for Train Connections in Munich",
                    cityAndCountry = "Munich, Germany 🇩🇪",
                    targetLanguage = "German",
                    cefrLevel = "A2-B1",
                    characterName = "Frau Weber",
                    characterRole = "Reiseberatung am Hauptbahnhof",
                    settingDescriptionInTargetLang = "Am Informationsschalter im Münchner Hauptbahnhof.",
                    openingLineInTargetLang = "Grüß Gott! Wohin möchten Sie heute fahren und brauchen Sie eine schnelle Verbindung?",
                    openingLinePhonetic = "Gryoos got! Vo-HIN MEKH-ten zee HOY-teh FAH-ren...",
                    missionGoalInTargetLang = "Frage nach dem nächsten Zug, dem Gleis und ob du umsteigen musst.",
                    decisionBranchesInTargetLang = listOf(
                        "Entschuldigung, wann fährt der nächste Zug nach Salzburg und von welchem Gleis?",
                        "Muss ich unterwegs umsteigen oder fährt der Zug direkt?",
                        "Gilt mein Deutschlandticket für diese Verbindung?"
                    ),
                    culturalContextNote = "Always confirm the 'Gleis' (platform) and whether a train requires 'umsteigen' (transferring)."
                )
            )
            "italian" -> listOf(
                ImmersionScenario(
                    id = "imm_rome_trattoria",
                    title = "Dinner at a Neighborhood Trattoria in Rome",
                    cityAndCountry = "Rome, Italy 🇮🇹",
                    targetLanguage = "Italian",
                    cefrLevel = "A2-B1",
                    characterName = "Marco",
                    characterRole = "Oste della Trattoria Trastevere",
                    settingDescriptionInTargetLang = "Sei seduto a un tavolino all'aperto nel cuore di Trastevere a Roma.",
                    openingLineInTargetLang = "Buonasera! Benvenuto. Stasera abbiamo cacio e pepe fatta in casa e carciofi alla romana. Cosa desidera prendere?",
                    openingLinePhonetic = "Bwoh-nah-SEH-rah! Ben-veh-NOO-toh...",
                    missionGoalInTargetLang = "Ordina un primo piatto, chiedi acqua naturale e domanda un consiglio per il dolce.",
                    decisionBranchesInTargetLang = listOf(
                        "Buonasera! Prendo la cacio e pepe e una bottiglia d'acqua naturale, per favore.",
                        "Cosa mi consiglia tra i secondi piatti di oggi?",
                        "Scusi, questo piatto contiene frutta a guscio o latticini?"
                    ),
                    culturalContextNote = "In Italy, waiters will ask 'acqua naturale o frizzante?' (still or sparkling water) and bring 'il coperto' (table service)."
                )
            )
            "portuguese" -> listOf(
                ImmersionScenario(
                    id = "imm_lisbon_pastelaria",
                    title = "Morning Coffee at a Lisbon Pastelaria",
                    cityAndCountry = "Lisbon, Portugal 🇵🇹",
                    targetLanguage = "Portuguese",
                    cefrLevel = "A2-B1",
                    characterName = "Inês",
                    characterRole = "Atendente na Pastelaria do Chiado",
                    settingDescriptionInTargetLang = "Está numa pastelaria tradicional no Chiado em Lisboa.",
                    openingLineInTargetLang = "Bom dia! Acabaram de sair pastéis de nata quentinhos. Vai querer uma bica ou um galão?",
                    openingLinePhonetic = "Bohn DEE-ah! Ah-kah-BAH-rahm deh sah-EER...",
                    missionGoalInTargetLang = "Peça um café, dois pastéis e pergunte se pode pagar com cartão.",
                    decisionBranchesInTargetLang = listOf(
                        "Bom dia! Queria uma bica e dois pastéis de nata com canela, por favor.",
                        "Qual é a diferença entre uma bica e um café pingado?",
                        "Aceitam pagamento com cartão ou multibanco?"
                    ),
                    culturalContextNote = "In Lisbon, an espresso is called 'uma bica', while milky coffee in a tall glass is 'um galão'."
                )
            )
            "urdu" -> listOf(
                ImmersionScenario(
                    id = "imm_lahore_chai",
                    title = "Evening Chai & Conversation in Lahore",
                    cityAndCountry = "Lahore, Pakistan 🇵🇰",
                    targetLanguage = "Urdu",
                    cefrLevel = "A2-B1",
                    characterName = "Bilal",
                    characterRole = "Chai Café Host",
                    settingDescriptionInTargetLang = "آپ لاہور کے ایک پُرسکون کیفے میں بیٹھے ہیں اور چائے آرڈر کر رہے ہیں۔",
                    openingLineInTargetLang = "السلام علیکم! خوش آمدید۔ آج آپ سادہ دودھ پتی چائے پییں گے یا الائچی والی چائے؟",
                    openingLinePhonetic = "Assalam-o-Alaikum! Khush aamdeed. Aaj aap saada doodh patti chai piyenge ya ilaichi wali chai?",
                    missionGoalInTargetLang = "اردو میں چائے اور سموسے آرڈر کریں اور موسم یا اپنے دن کے بارے میں بات کریں۔",
                    decisionBranchesInTargetLang = listOf(
                        "وعلیکم السلام! مجھے ایک کپ الائچی والی چائے اور دو گرم سموسے دے دیں۔",
                        "براہِ کرم چینی تھوڑی کم رکھیے گا۔ آپ کا کیفے بہت اچھا ہے۔",
                        "آج لاہور کا موسم کیسا ہے؟ میں پہلی بار یہاں آیا ہوں۔"
                    ),
                    culturalContextNote = "Starting with 'Assalam-o-Alaikum' and using polite 'aap' + 'barah-e-karam' creates instant warmth in Urdu."
                )
            )
            "arabic" -> listOf(
                ImmersionScenario(
                    id = "imm_cairo_cafe",
                    title = "Ordering Mint Tea at a Neighborhood Café",
                    cityAndCountry = "Cairo, Egypt 🇪🇬",
                    targetLanguage = "Arabic",
                    cefrLevel = "A2-B1",
                    characterName = "Tariq",
                    characterRole = "Café Host",
                    settingDescriptionInTargetLang = "أنت تجلس في مقهى هادئ وتطلب مشروباً ساخناً.",
                    openingLineInTargetLang = "أهلاً وسهلاً! نورت المكان. تحب تشرب شاي بالنعناع أم قهوة عربية اليوم؟",
                    openingLinePhonetic = "Ahlan wa sahlan! Nawwart al-makaan. Tuhibb tashrab shay bin-na'na' am qahwa arabiyya al-yawm?",
                    missionGoalInTargetLang = "اطلب مشروباً، حدد كمية السكر، واسأل عن توصية لمكان جميل قريب.",
                    decisionBranchesInTargetLang = listOf(
                        "أهلاً بك! من فضلك كوب شاي بالنعناع بسكر قليل.",
                        "هل عندكم عصير مانجو طازج أو حلوى تقليدية؟",
                        "ما أفضل مكان قريب للمشي هذا المساء؟"
                    ),
                    culturalContextNote = "'Min fadlak' (male) / 'min fadlik' (female) is the classic polite way to say 'please'."
                )
            )
            "korean" -> listOf(
                ImmersionScenario(
                    id = "imm_seoul_cafe",
                    title = "Ordering at a Café in Hongdae, Seoul",
                    cityAndCountry = "Seoul, South Korea 🇰🇷",
                    targetLanguage = "Korean",
                    cefrLevel = "A2-B1",
                    characterName = "Minji",
                    characterRole = "Hongdae Café Barista",
                    settingDescriptionInTargetLang = "서울 홍대의 아늑한 카페 카운터 앞에 서 있습니다.",
                    openingLineInTargetLang = "안녕하세요! 어서 오세요. 오늘 따뜻한 아메리카노 드실래요, 아니면 아이스 라떼가 좋으세요?",
                    openingLinePhonetic = "Annyeonghaseyo! Eoseo oseyo. Oneul ttatteutan amerikano deusillaeyo, animyeon aiseu rattega joeuseyo?",
                    missionGoalInTargetLang = "음료와 디저트를 주문하고 매장 이용인지 포장인지 말해 보세요.",
                    decisionBranchesInTargetLang = listOf(
                        "아이스 바닐라 라떼 한 잔이랑 소금빵 하나 주세요. 여기서 먹고 갈게요.",
                        "혹시 디카페인 커피도 있나요? 포장해 갈게요.",
                        "카드 결제 되나요? 진동벨 여기서 기다리면 되죠?"
                    ),
                    culturalContextNote = "In Korean cafés, say '매장에서 먹고 갈게요' (for here) or '포장할게요' (to go)."
                )
            )
            "chinese" -> listOf(
                ImmersionScenario(
                    id = "imm_taipei_night_market",
                    title = "Ordering Street Food & Tea",
                    cityAndCountry = "Taipei / Beijing 🏮",
                    targetLanguage = "Chinese",
                    cefrLevel = "A2-B1",
                    characterName = "Lin",
                    characterRole = "Teahouse & Snack Stall Host",
                    settingDescriptionInTargetLang = "你在一家热闹的茶馆和点心店前点餐。",
                    openingLineInTargetLang = "你好，欢迎光临！今天我们有现做的小笼包和乌龙茶，请问你想点什么？",
                    openingLinePhonetic = "Nǐ hǎo, huānyíng guānglín! Jīntiān wǒmen yǒu xiàn zuò de xiǎolóngbāo hé wūlóngchá, qǐngwèn nǐ xiǎng diǎn shénme?",
                    missionGoalInTargetLang = "点一份点心和一杯茶，并询问少糖或推荐菜。",
                    decisionBranchesInTargetLang = listOf(
                        "你好！我要一份小笼包和一杯热乌龙茶，谢谢。",
                        "请问你们最推荐哪一道点心？可以做微甜吗？",
                        "请问这里可以手机支付还是刷卡？"
                    ),
                    culturalContextNote = "Using '请问 (qǐngwèn)' before questions is a natural polite opener in Mandarin."
                )
            )
            "hindi" -> listOf(
                ImmersionScenario(
                    id = "imm_jaipur_market",
                    title = "Ordering Chai & Snacks at a Local Market",
                    cityAndCountry = "Jaipur, India 🇮🇳",
                    targetLanguage = "Hindi",
                    cefrLevel = "A2-B1",
                    characterName = "Rohan",
                    characterRole = "Market Café Host",
                    settingDescriptionInTargetLang = "आप जयपुर के एक बाज़ार में चाय की दुकान पर खड़े हैं।",
                    openingLineInTargetLang = "नमस्ते जी! आइए, आज गरमा-गरम मसाला चाय और समोसे तैयार हैं। आप क्या लेना पसंद करेंगे?",
                    openingLinePhonetic = "Namaste ji! Aaiye, aaj garma-garam masala chai aur samose taiyar hain. Aap kya lena pasand karenge?",
                    missionGoalInTargetLang = "मसाला चाय और नाश्ता ऑर्डर करें और आस-पास की जगह के बारे में पूछें।",
                    decisionBranchesInTargetLang = listOf(
                        "नमस्ते! कृपया एक कप मसाला चाय और दो समोसे दीजिए।",
                        "चाय में चीनी थोड़ी कम रखिएगा, धन्यवाद।",
                        "यहाँ पास में कोई अच्छी किताबों की दुकान या बाज़ार है?"
                    ),
                    culturalContextNote = "Adding 'जी (ji)' and 'कृपया (kripya)' keeps Hindi conversation warm and respectful."
                )
            )
            else -> listOf(
                ImmersionScenario(
                    id = "imm_${language.lowercase().replace(" ", "_")}_cafe",
                    title = "Everyday Café & Neighborhood Chat ($language)",
                    cityAndCountry = "Local $language Neighborhood",
                    targetLanguage = language,
                    cefrLevel = "A2-B1",
                    characterName = "Alex",
                    characterRole = "Neighborhood Café Host ($language)",
                    settingDescriptionInTargetLang = "You are visiting a friendly neighborhood café where everyone speaks $language.",
                    openingLineInTargetLang = "Welcome! Start speaking in $language — tell me what you would like to drink and what you are doing today.",
                    openingLinePhonetic = "Speak naturally in $language",
                    missionGoalInTargetLang = "Greet the host, order a drink or snack, and ask one question in $language.",
                    decisionBranchesInTargetLang = listOf(
                        "Respond with a polite greeting and order coffee or tea in $language",
                        "Ask for today's fresh food recommendation in $language",
                        "Ask for the bill and thank the host in $language"
                    ),
                    culturalContextNote = "Tap the mic or type directly in $language — the AI character will reply naturally in $language."
                )
            )
        }
    }

    fun dailyLifeMissionsForLanguage(language: String, cefrLevel: String): List<DailyLifeMission> {
        val normLevel = cefrLevel.ifBlank { "A2" }
        return listOf(
            DailyLifeMission(
                id = "mission_intro_30s",
                title = "Introduce Yourself Naturally in 30 Seconds",
                promptDescription = "Introduce your name, where you are from, what you do or study, and why you are learning $language — completely in $language.",
                cefrLevel = normLevel,
                targetLanguage = language,
                recommendedDurationSeconds = 30,
                requiredElements = listOf("Greeting & name", "Origin or city", "Motivation or hobby in $language"),
                starterPhrasesInTargetLang = when (language.lowercase()) {
                    "spanish" -> listOf("Hola, me llamo...", "Soy de...", "Aprendo español porque me encanta...")
                    "french" -> listOf("Bonjour, je m'appelle...", "J'habite à...", "J'apprends le français parce que...")
                    "japanese" -> listOf("はじめまして、私は…です。", "…から来ました。", "日本語を勉強している理由は…")
                    "german" -> listOf("Hallo, ich heiße...", "Ich komme aus...", "Ich lerne Deutsch, weil...")
                    "italian" -> listOf("Ciao, mi chiamo...", "Vengo da...", "Studio italiano perché...")
                    "portuguese" -> listOf("Olá, chamo-me...", "Sou de...", "Aprendo português porque...")
                    "korean" -> listOf("안녕하세요, 저는 ...입니다.", "...에서 왔어요.", "한국어를 배우는 이유는...")
                    "chinese", "mandarin" -> listOf("你好，我叫……", "我来自……", "我学中文是因为……")
                    "arabic" -> listOf("مرحباً، اسمي...", "أنا من...", "أتعلم العربية لأنني...")
                    "urdu" -> listOf("السلام علیکم، میرا نام ... ہے", "میں ... سے ہوں", "میں اردو اس لیے سیکھ رہا ہوں کیونکہ...")
                    "hindi" -> listOf("नमस्ते, मेरा नाम ... है", "मैं ... से हूँ", "मैं हिंदी इसलिए सीख रहा हूँ क्योंकि...")
                    "russian" -> listOf("Привет, меня зовут...", "Я из...", "Я учу русский язык, потому что...")
                    "turkish" -> listOf("Merhaba, benim adım...", "Ben ...'den geliyorum", "Türkçe öğreniyorum çünkü...")
                    "english" -> listOf("Hi, my name is...", "I am from...", "I am learning English because...")
                    else -> listOf("Greet the listener in $language", "State your name and city in $language", "Share why you are learning $language")
                },
                xpReward = 65
            ),
            DailyLifeMission(
                id = "mission_daily_routine",
                title = "Explain Your Daily Routine",
                promptDescription = "Walk through your typical morning, afternoon, and evening habits in $language using time connectors (first, then, after work/study).",
                cefrLevel = normLevel,
                targetLanguage = language,
                recommendedDurationSeconds = 40,
                requiredElements = listOf("Morning activity", "Work or study routine", "Sequence connector (then / after)"),
                starterPhrasesInTargetLang = when (language.lowercase()) {
                    "spanish" -> listOf("Normalmente me despierto a las...", "Después desayuno y...", "Por la noche suelo...")
                    "french" -> listOf("D'habitude, je me lève à...", "Ensuite, je travaille sur...", "Le soir, j'aime...")
                    "japanese" -> listOf("毎朝、七時に起きます。", "それから、仕事をします。", "夜はゆっくり本を読みます。")
                    "german" -> listOf("Normalerweise stehe ich um ... auf.", "Danach arbeite ich...", "Abends koche ich gern...")
                    "italian" -> listOf("Di solito mi sveglio alle...", "Poi lavoro o studio...", "La sera mi piace...")
                    "portuguese" -> listOf("Normalmente acordo às...", "Depois trabalho até...", "À noite gosto de...")
                    "korean" -> listOf("보통 아침 7시에 일어나요.", "그 다음에 일이나 공부를 해요.", "저녁에는 쉬면서 책을 읽어요.")
                    "chinese", "mandarin" -> listOf("通常我早上七点起床。", "然后我去工作或学习。", "晚上我喜欢休息和看书。")
                    "arabic" -> listOf("عادةً أستيقظ في الساعة...", "بعد ذلك أعمل أو أدرس...", "في المساء أحب أن...")
                    "urdu" -> listOf("عام طور پر میں صبح ... بجے اٹھتا ہوں", "اس کے بعد میں کام یا پڑھائی کرتا ہوں", "شام کو مجھے ... پسند ہے")
                    "hindi" -> listOf("आमतौर पर मैं सुबह ... बजे उठता हूँ", "उसके बाद मैं काम करता हूँ", "शाम को मुझे ... पसंद है")
                    "english" -> listOf("Usually I wake up at...", "Then I work on...", "In the evening I enjoy...")
                    else -> listOf("Describe your morning in $language", "Add a time connector (first / then) in $language", "Share your evening routine in $language")
                },
                xpReward = 70
            ),
            DailyLifeMission(
                id = "mission_describe_scene",
                title = "Describe Something You See Right Now",
                promptDescription = "Look around your room, street, or desk and describe at least 3 objects, their colors or positions, and the atmosphere in $language.",
                cefrLevel = normLevel,
                targetLanguage = language,
                recommendedDurationSeconds = 35,
                requiredElements = listOf("At least 2-3 objects", "Descriptive adjectives (color/size)", "Location preposition"),
                starterPhrasesInTargetLang = when (language.lowercase()) {
                    "spanish" -> listOf("Ahora mismo frente a mí veo...", "En la mesa hay...", "El ambiente es bastante tranquilo...")
                    "french" -> listOf("Autour de moi, je vois...", "Sur la table, il y a...", "L'ambiance est très calme...")
                    "japanese" -> listOf("今、机の上には…があります。", "窓の外には…が見えます。", "とても静かな部屋です。")
                    "german" -> listOf("Vor mir auf dem Tisch liegt...", "Ich sehe auch...", "Die Atmosphäre ist ruhig...")
                    "italian" -> listOf("Davanti a me vedo...", "Sul tavolo c'è...", "L'atmosfera è molto calma...")
                    "portuguese" -> listOf("À minha frente vejo...", "Em cima da mesa há...", "O ambiente está tranquilo...")
                    "korean" -> listOf("지금 내 앞에는 ...이 있어요.", "책상 위에 ...이 놓여 있어요.", "분위기가 아주 조용해요.")
                    "chinese", "mandarin" -> listOf("我现在看到……", "桌子上放着……", "这里的环境很安静。")
                    "arabic" -> listOf("أمامي الآن أرى...", "على الطاولة يوجد...", "الجو هنا هادئ ومريح...")
                    "urdu" -> listOf("اس وقت میرے سامنے ... ہے", "میز پر ... رکھا ہے", "یہاں کا ماحول پُرسکون ہے")
                    "hindi" -> listOf("मेरे सामने ... दिखाई दे रहा है", "मेज़ पर ... रखा है", "यहाँ का माहौल शांत है")
                    "english" -> listOf("Right in front of me I see...", "On the desk there is...", "The atmosphere feels calm...")
                    else -> listOf("Name 2-3 nearby objects in $language", "Describe colors or sizes in $language", "Explain where they are located in $language")
                },
                xpReward = 75
            ),
            DailyLifeMission(
                id = "mission_unexpected_conv",
                title = "Handle an Unexpected Conversation",
                promptDescription = "Your train has just been delayed by 45 minutes and you have a dinner reservation. Explain the situation politely and propose a new arrival time in $language.",
                cefrLevel = normLevel,
                targetLanguage = language,
                recommendedDurationSeconds = 45,
                requiredElements = listOf("Polite apology", "Reason for delay", "Proposed solution or new time"),
                starterPhrasesInTargetLang = when (language.lowercase()) {
                    "spanish" -> listOf("Disculpa, mi tren lleva retraso...", "¿Podríamos cambiar la reserva para las...?", "Llegaré en unos cuarenta minutos.")
                    "french" -> listOf("Excusez-moi, mon train a du retard...", "Serait-il possible de décaler...", "J'arriverai vers...")
                    "japanese" -> listOf("すみません、電車が遅れています。", "予約の時間を30分変更できますか？", "…頃に到着します。")
                    "german" -> listOf("Entschuldigung, mein Zug hat Verspätung...", "Könnten wir die Reservierung verschieben?", "Ich bin gegen ... da.")
                    "italian" -> listOf("Mi scusi, il treno è in ritardo...", "Possiamo spostare la prenotazione alle...?", "Arriverò tra circa...")
                    "portuguese" -> listOf("Desculpe, o meu comboio está atrasado...", "Podemos adiar a reserva para as...?", "Chego por volta das...")
                    "korean" -> listOf("죄송하지만 기차가 지연되고 있어요.", "예약 시간을 조금 늦출 수 있을까요?", "...쯤 도착할 것 같아요.")
                    "chinese", "mandarin" -> listOf("不好意思，我的火车晚点了。", "请问可以把预订推迟一下吗？", "我大概……分钟后到。")
                    "arabic" -> listOf("عذراً، القطار تأخر قليلاً...", "هل يمكننا تأجيل الحجز إلى الساعة...؟", "سأصل خلال أربعين دقيقة.")
                    "urdu" -> listOf("معاف کیجیے، میری ٹرین لیٹ ہو گئی ہے", "کیا ہم ریزرویشن کا وقت آگے بڑھا سکتے ہیں؟", "میں تقریباً چالیس منٹ میں پہنچ جاؤں گا")
                    "hindi" -> listOf("माफ़ कीजिए, मेरी ट्रेन लेट हो गई है", "क्या हम बुकिंग का समय आगे बढ़ा सकते हैं?", "मैं लगभग ... बजे पहुँचूँगा")
                    "english" -> listOf("Excuse me, my train is delayed...", "Could we move our reservation to...?", "I should arrive in about 40 minutes.")
                    else -> listOf("Apologize politely for the delay in $language", "Explain the train problem in $language", "Propose a new arrival time in $language")
                },
                xpReward = 85
            ),
            DailyLifeMission(
                id = "mission_short_story",
                title = "Tell a Short Story Without Switching Languages",
                promptDescription = "Tell a short memorable story (a trip, a funny misunderstanding, or a great meal) from start to finish in $language without using any English filler words.",
                cefrLevel = normLevel,
                targetLanguage = language,
                recommendedDurationSeconds = 50,
                requiredElements = listOf("Past setting", "Main event or surprise", "How it ended or felt"),
                starterPhrasesInTargetLang = when (language.lowercase()) {
                    "spanish" -> listOf("El año pasado, durante un viaje...", "De repente encontramos...", "Al final fue una experiencia increíble.")
                    "french" -> listOf("L'année dernière, quand j'étais à...", "Tout à coup, j'ai découvert...", "C'était un souvenir inoubliable.")
                    "japanese" -> listOf("去年、旅行に行った時のことです。", "偶然、素敵なカフェを見つけました。", "とても楽しい思い出になりました。")
                    "german" -> listOf("Letztes Jahr war ich in...", "Plötzlich haben wir ... entdeckt.", "Am Ende war es ein toller Tag.")
                    "italian" -> listOf("L'anno scorso durante un viaggio...", "All'improvviso abbiamo trovato...", "Alla fine è stata una giornata bellissima.")
                    "portuguese" -> listOf("No ano passado, numa viagem...", "De repente descobrimos...", "No final foi inesquecível.")
                    "korean" -> listOf("작년에 여행을 갔을 때 일이에요.", "갑자기 멋진 가게를 발견했어요.", "결국 정말 좋은 추억이 되었어요.")
                    "chinese", "mandarin" -> listOf("去年我去旅行的时候……", "突然我们发现了一家小店……", "最后那成了难忘的经历。")
                    "arabic" -> listOf("في العام الماضي أثناء رحلتي...", "فجأة اكتشفنا مكاناً جميلاً...", "في النهاية كانت تجربة رائعة.")
                    "urdu" -> listOf("پچھلے سال سفر کے دوران...", "اچانک ہمیں ایک خوبصورت جگہ ملی", "آخر میں یہ بہت اچھا تجربہ رہا")
                    "hindi" -> listOf("पिछले साल सफ़र के दौरान...", "अचानक हमें एक शानदार जगह मिली", "अंत में यह बहुत यादगार दिन था")
                    "english" -> listOf("Last year while traveling...", "Suddenly we discovered...", "In the end it was unforgettable.")
                    else -> listOf("Set the scene in the past in $language", "Describe the main surprise in $language", "Share how it ended in $language")
                },
                xpReward = 95
            )
        )
    }

    fun regionalAccentsForLanguage(language: String): List<RegionalAccentSample> {
        return when (language.lowercase()) {
            "french" -> listOf(
                RegionalAccentSample(
                    id = "acc_fr_paris",
                    language = "French",
                    regionName = "Paris & Île-de-France",
                    countryFlag = "🇫🇷",
                    accentCharacteristics = "Crisp uvular 'r', merged nasal vowels (brin/brun), rapid syllable-timed elision in everyday speech.",
                    respectNote = "All Francophone accents are equally valid varieties of French shaped by regional history.",
                    transcriptInTargetLang = "On se retrouve au métro Châtelet vers dix-neuf heures ? J'ai réservé une petite table sympa juste à côté.",
                    englishTranslation = "Shall we meet at Châtelet metro around 7 PM? I booked a nice little table right nearby.",
                    phoneticFeatures = listOf("Drop of 'ne' in casual speech ('je sais pas')", "Fast liaison between 'dix-neuf_heures'"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("sympa", "agréable", "Nice / friendly", "Un quartier très sympa"),
                        RegionalVocabNote("boulot", "travail", "Work / job", "Je sors du boulot")
                    ),
                    comprehensionQuestion = "Where and when does the speaker propose meeting?",
                    comprehensionOptions = listOf(
                        "At Châtelet metro around 7:00 PM",
                        "At Gare du Nord at 9:00 AM",
                        "Inside a museum at noon"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'Dix-neuf heures' is 19:00 (7:00 PM) at the Châtelet metro station."
                ),
                RegionalAccentSample(
                    id = "acc_fr_quebec",
                    language = "French",
                    regionName = "Montréal & Québec",
                    countryFlag = "🇨🇦",
                    accentCharacteristics = "Affrication of /t/ and /d/ before /i/ and /u/ ('tsi', 'dzu'), distinct nasal vowels, rich historical vocabulary.",
                    respectNote = "Québécois French preserves classical phoneme distinctions while innovating vibrant North American expressions.",
                    transcriptInTargetLang = "Il fait frette ce matin à Montréal ! Viens prendre un déjeuner chaud avant de partir travailler.",
                    englishTranslation = "It's freezing cold this morning in Montreal! Come have a warm breakfast before heading to work.",
                    phoneticFeatures = listOf("Affricated 't' in 'partir' -> 'partsir'", "Expressive vowel lengthening"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("frette", "très froid", "Bitterly cold", "Il fait frette dehors"),
                        RegionalVocabNote("déjeuner", "petit-déjeuner", "Breakfast (in Québec, lunch is 'dîner')", "Prendre le déjeuner le matin")
                    ),
                    comprehensionQuestion = "In Québec French, what meal does 'déjeuner' refer to in this clip?",
                    comprehensionOptions = listOf(
                        "Breakfast in the morning",
                        "Late-night supper",
                        "Afternoon snack only"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "In Québec, Belgian, and Swiss French, 'déjeuner' means breakfast, 'dîner' is lunch, and 'souper' is dinner."
                ),
                RegionalAccentSample(
                    id = "acc_fr_marseille",
                    language = "French",
                    regionName = "Marseille & Provence (Midi)",
                    countryFlag = "🇫🇷",
                    accentCharacteristics = "Melodic Southern prosody, pronounced final schwas ('e muet'), open mid-vowels with warm Mediterranean cadence.",
                    respectNote = "Southern French ('l'accent du Midi') is celebrated for its clarity and musical syllable rhythm.",
                    transcriptInTargetLang = "Ce midi on descend sur le Vieux-Port chercher du poisson frais, et après on boit le café au soleil.",
                    englishTranslation = "This noon we're heading down to the Old Port to get fresh fish, and afterwards we'll drink coffee in the sun.",
                    phoneticFeatures = listOf("Pronounced final schwa in 'table / petite'", "Clear, unreduced nasal vowels followed by subtle consonant"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("pitchoun", "enfant / petit", "Little child / kid (Occitan origin)", "Les pitchouns jouent au parc"),
                        RegionalVocabNote("peuchère", "hélas / le pauvre", "Expresses sympathy or surprise", "Oh peuchère, quelle chaleur !")
                    ),
                    comprehensionQuestion = "What are the speaker's plans at the Vieux-Port?",
                    comprehensionOptions = listOf(
                        "Buy fresh fish at noon and have coffee in the sun",
                        "Catch an overnight ferry to Corsica",
                        "Attend an indoor evening concert"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "They are heading to the Vieux-Port at noon ('ce midi') for fresh fish and coffee in the sun."
                )
            )
            "japanese" -> listOf(
                RegionalAccentSample(
                    id = "acc_jp_tokyo",
                    language = "Japanese",
                    regionName = "Tokyo (Kanto / Hyojungo)",
                    countryFlag = "🇯🇵",
                    accentCharacteristics = "Low-high pitch accent start on standard mora, crisp vowel devoicing between voiceless consonants (e.g., 'suki', 'desu').",
                    respectNote = "Tokyo pitch patterns are widely heard in national media, while regional dialects ('hougen') carry deep cultural identity.",
                    transcriptInTargetLang = "次の山手線は三番線から出ます。乗り換えの時間は五分ぐらいあるので大丈夫ですよ。",
                    englishTranslation = "The next Yamanote Line train departs from Track 3. We have about five minutes to transfer, so we'll be fine.",
                    phoneticFeatures = listOf("Devoiced 'u' at the end of 'desu' -> 'des'", "Standard Kanto downstep pitch accent"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("大丈夫 (daijoubu)", "問題ない", "All good / no problem", "五分あるので大丈夫です"),
                        RegionalVocabNote("山手線 (Yamanote-sen)", "環状線", "Tokyo's main loop line", "山手線に乗り換える")
                    ),
                    comprehensionQuestion = "How much time do they have for the train transfer?",
                    comprehensionOptions = listOf(
                        "About 5 minutes from Track 3",
                        "30 minutes at the ticket gate",
                        "The last train has already left"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'五分ぐらい (gofun gurai)' means about five minutes, departing from Track 3 ('三番線')."
                ),
                RegionalAccentSample(
                    id = "acc_jp_osaka",
                    language = "Japanese",
                    regionName = "Osaka & Kansai (Kansai-ben)",
                    countryFlag = "🇯🇵",
                    accentCharacteristics = "Distinct Kansai pitch contour (often high-initial), expressive vowel retention, warm conversational endings ('~yan', '~hen', 'ookini').",
                    respectNote = "Kansai-ben is spoken by over 20 million people in Osaka, Kyoto, and Kobe with a rich literary and comedic heritage.",
                    transcriptInTargetLang = "ここのたこ焼き、めっちゃ美味しいやん！熱いうちに食べんと冷めてまうで。おおきに！",
                    englishTranslation = "The takoyaki here is seriously delicious! If you don't eat it while it's hot, it'll get cold. Thank you!",
                    phoneticFeatures = listOf("Negative verb suffix '~hen / ~n' instead of '~nai'", "Sentence-final emphatic particle 'yan' and 'de'"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("めっちゃ (metcha)", "とても / すごく", "Very / super", "めっちゃ美味しい"),
                        RegionalVocabNote("おおきに (ookini)", "ありがとう", "Thank you (traditional Osaka/Kyoto)", "毎度おおきに")
                    ),
                    comprehensionQuestion = "What does the speaker advise doing with the takoyaki?",
                    comprehensionOptions = listOf(
                        "Eat it while it's still hot so it doesn't get cold",
                        "Put it in the refrigerator until tomorrow",
                        "Share it only after adding extra rice"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'熱いうちに食べんと冷めてまうで' means 'If you don't eat it while it's hot, it'll cool down!'"
                )
            )
            "english" -> listOf(
                RegionalAccentSample(
                    id = "acc_en_london",
                    language = "English",
                    regionName = "London & Southern England",
                    countryFlag = "🇬🇧",
                    accentCharacteristics = "Non-rhotic 'r' after vowels ('car' -> 'cah'), long broad 'a' in 'bath/chance', crisp intonation rises.",
                    respectNote = "English is a pluricentric global language; British, American, Irish, Australian, and other varieties are equally valid.",
                    transcriptInTargetLang = "I'm just popping round the corner to grab a flat white before the tube gets packed at half eight.",
                    englishTranslation = "I'm quickly going around the corner to get an espresso with steamed milk before the subway gets crowded at 8:30.",
                    phoneticFeatures = listOf("Non-rhotic syllable endings ('corner' -> 'cawn-uh')", "'Half eight' means 8:30"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("the tube", "subway / metro", "London Underground railway", "Take the tube to Victoria"),
                        RegionalVocabNote("half eight", "eight-thirty (8:30)", "Thirty minutes past eight", "Meet me at half eight")
                    ),
                    comprehensionQuestion = "What time is 'half eight' in British English?",
                    comprehensionOptions = listOf("8:30", "7:30", "4:00"),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "In British and Irish English, 'half eight' is short for 'half past eight' (8:30)."
                ),
                RegionalAccentSample(
                    id = "acc_en_sydney",
                    language = "English",
                    regionName = "Sydney & Coastal Australia",
                    countryFlag = "🇦🇺",
                    accentCharacteristics = "Non-rhotic with distinctive diphthong shifts, friendly diminutive nouns ('arvo', 'brekkie'), smooth connected phrasing.",
                    respectNote = "Australian English features rich colloquialisms that foster relaxed, egalitarian social warmth.",
                    transcriptInTargetLang = "No worries at all! Let's catch up this arvo by the harbour after we finish up the project.",
                    englishTranslation = "No problem at all! Let's meet this afternoon by the harbor after we finish the project.",
                    phoneticFeatures = listOf("High-rising terminal contour in casual speech", "Smooth linking 'r' between vowels"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("this arvo", "this afternoon", "Afternoon", "See you this arvo"),
                        RegionalVocabNote("no worries", "you're welcome / no problem", "Reassurance that everything is fine", "No worries, mate")
                    ),
                    comprehensionQuestion = "When does the speaker suggest catching up?",
                    comprehensionOptions = listOf(
                        "This afternoon ('this arvo') by the harbour",
                        "Tomorrow morning at dawn",
                        "Next week at the airport"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'Arvo' is standard Australian conversational shorthand for 'afternoon'."
                )
            )
            "spanish" -> listOf(
                RegionalAccentSample(
                    id = "acc_es_madrid",
                    language = language,
                    regionName = "Madrid & Central Spain",
                    countryFlag = "🇪🇸",
                    accentCharacteristics = "Distinction between 's' and interdental 'z/ce/ci' (/θ/), vibrant apical 's', dynamic use of 'vosotros'.",
                    respectNote = "Spanish has over 500 million speakers across 20+ nations; no single regional accent is more 'correct' than another.",
                    transcriptInTargetLang = "¡Qué ilusión verte por Madrid! Si os apetece, luego damos un paseo por el Retiro y tomamos unas cañas.",
                    englishTranslation = "How exciting to see you in Madrid! If you all feel like it, later we can take a walk through Retiro Park and grab some small draft beers.",
                    phoneticFeatures = listOf("Interdental /θ/ on 'ilusión' and 'apetece' (distinción)", "Plural informal pronoun 'os / vosotros'"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("¿os apetece?", "¿quieren / tienen ganas?", "Do you all feel like / fancy?", "¿Os apetece ir al cine?"),
                        RegionalVocabNote("una caña", "un vaso pequeño de cerveza", "Small glass of draft beer or soft drink", "Tomar unas cañas con tapas")
                    ),
                    comprehensionQuestion = "What does the speaker suggest doing later?",
                    comprehensionOptions = listOf(
                        "Walking through Retiro Park and getting drinks together",
                        "Taking a high-speed train to Seville immediately",
                        "Staying home to study for an exam"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'Damos un paseo por el Retiro y tomamos unas cañas' means strolling through Retiro Park and having drinks."
                ),
                RegionalAccentSample(
                    id = "acc_es_cdmx",
                    language = language,
                    regionName = "Mexico City & Central Mexico",
                    countryFlag = "🇲🇽",
                    accentCharacteristics = "Clear syllable-timed consonants, full vowel retention, seseo ('s/c/z' pronounced /s/), warm melodic cadence.",
                    respectNote = "Mexican Spanish is the largest national variety of Spanish globally, celebrated for its expressive courtesy and Nahuatl-enriched lexicon.",
                    transcriptInTargetLang = "¡Qué padre que ya llegaste! Ahorita pedimos unos tacos al pastor y platicamos con calma, ¿sale?",
                    englishTranslation = "How awesome that you've arrived! Right now we'll order some al pastor tacos and chat calmly, sounds good?",
                    phoneticFeatures = listOf("Clear final 's' and full unstressed vowels", "Seseo: 'pedimos' and 'tacos' use crisp alveolar /s/"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("¡qué padre!", "¡qué genial / qué bueno!", "How cool / awesome!", "¡Qué padre estuvo el concierto!"),
                        RegionalVocabNote("platicar", "conversar / charlar", "To chat / converse", "Platicamos toda la tarde")
                    ),
                    comprehensionQuestion = "What does 'platicamos con calma' mean in Mexican Spanish?",
                    comprehensionOptions = listOf(
                        "We'll chat / talk calmly",
                        "We'll pay the bill with silver coins",
                        "We'll drive to the beach quickly"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'Platicar' is the standard verb in Mexico and Central America for 'to chat / converse'."
                ),
                RegionalAccentSample(
                    id = "acc_es_buenos_aires",
                    language = language,
                    regionName = "Buenos Aires (Rioplatense)",
                    countryFlag = "🇦🇷",
                    accentCharacteristics = "Zheísmo/sheísmo ('ll' and 'y' pronounced like 'sh' in 'calle'/'yo'), use of 'vos' with final-syllable verb stress ('tenés', 'vení'), Italianate intonation contour.",
                    respectNote = "Rioplatense Spanish in Argentina and Uruguay has a distinctive grammatical 'voseo' standard across both casual and educated speech.",
                    transcriptInTargetLang = "Che, ¿a qué hora llegás a Palermo? Yo ya estoy en la cafetería de la esquina, avisame cuando estés cerca.",
                    englishTranslation = "Hey, what time are you arriving in Palermo? I'm already at the corner café, let me know when you're close.",
                    phoneticFeatures = listOf("Sheísmo: 'llegás' and 'yo' begin with postalveolar /ʃ/", "Voseo verb endings: 'llegás', 'avisame'"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("che", "oye / amigo", "Friendly vocative ('hey / mate')", "Che, ¿cómo andás?"),
                        RegionalVocabNote("llegás / avisame", "llegas / avísame (tú)", "Vos conjugation with stress on final syllable", "Avisame cuando llegués")
                    ),
                    comprehensionQuestion = "Which regional pronoun and pronunciation feature appear in '¿a qué hora llegás? Yo ya estoy...'?",
                    comprehensionOptions = listOf(
                        "Rioplatense 'voseo' (llegás) and 'sheísmo' on 'll / y'",
                        "Castilian 'vosotros' plural imperative",
                        "Silent initial consonants"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "Buenos Aires uses 'vos llegás' and pronounces 'll' and 'y' as a postalveolar fricative (/ʃ/ or /ʒ/)."
                )
            )
            "german" -> listOf(
                RegionalAccentSample(
                    id = "acc_de_berlin",
                    language = "German",
                    regionName = "Berlin & Northern Germany",
                    countryFlag = "🇩🇪",
                    accentCharacteristics = "Crisp consonant endings, occasional softening of 'g' to 'j' ('jut' for 'gut'), direct urban rhythm.",
                    respectNote = "German spans Germany, Austria, Switzerland, and Liechtenstein with vibrant regional standards.",
                    transcriptInTargetLang = "Treffen wir uns um halb sieben am Alexanderplatz? Danach können wir noch gemütlich essen gehen.",
                    englishTranslation = "Shall we meet at 6:30 at Alexanderplatz? Afterwards we can go eat somewhere cozy.",
                    phoneticFeatures = listOf("'Halb sieben' means 6:30 (half before seven)", "Uvular 'r' softened at syllable end ('wir' -> 'via')"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("halb sieben", "6:30 Uhr", "Half past six (literally 'half to seven')", "Um halb sieben am Bahnhof"),
                        RegionalVocabNote("Schrippe", "Brötchen", "Bread roll (Berlin regional term)", "Zwei frische Schrippen bitte")
                    ),
                    comprehensionQuestion = "What time is 'halb sieben' in German?",
                    comprehensionOptions = listOf("6:30", "7:30", "5:30"),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'Halb sieben' means half an hour before seven — 6:30."
                ),
                RegionalAccentSample(
                    id = "acc_de_vienna",
                    language = "German",
                    regionName = "Vienna & Austria",
                    countryFlag = "🇦🇹",
                    accentCharacteristics = "Warm melodic vowel lengthening, softened voiceless stops, distinct Austrian culinary and everyday vocabulary.",
                    respectNote = "Austrian Standard German is an equal pluricentric standard with its own official dictionary.",
                    transcriptInTargetLang = "Grüß Gott! Möchten Sie eine Melange und ein Stück Sachertorte hier im Kaffeehaus genießen?",
                    englishTranslation = "Hello! Would you like to enjoy a Viennese milk coffee and a slice of Sachertorte here in the coffeehouse?",
                    phoneticFeatures = listOf("Rounded melodic intonation", "Softened initial 'p/t/k'"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("Melange", "Cappuccino / Milchkaffee", "Classic Viennese coffee with steamed milk", "Eine Melange, bitte"),
                        RegionalVocabNote("Servus / Grüß Gott", "Hallo / Guten Tag", "Warm Austrian greeting", "Grüß Gott miteinander")
                    ),
                    comprehensionQuestion = "What is a 'Melange' in Vienna?",
                    comprehensionOptions = listOf("A Viennese espresso with steamed milk", "A cold fruit juice", "A train ticket"),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "A 'Wiener Melange' is Vienna's signature coffeehouse drink topped with steamed milk foam."
                )
            )
            "urdu" -> listOf(
                RegionalAccentSample(
                    id = "acc_ur_lahore",
                    language = "Urdu",
                    regionName = "Lahore & Punjab",
                    countryFlag = "🇵🇰",
                    accentCharacteristics = "Warm expressive cadence, clear retroflex consonants (ٹ، ڈ، ڑ), affectionate conversational fillers.",
                    respectNote = "Urdu is spoken with rich regional music across Lahore, Karachi, Islamabad, Lucknow, and Hyderabad.",
                    transcriptInTargetLang = "آج موسم بہت خوشگوار ہے، چلیں شام کو بادشاہی مسجد کے پاس چائے پینے چلتے ہیں۔",
                    englishTranslation = "The weather is very pleasant today; let's go drink tea near Badshahi Mosque this evening.",
                    phoneticFeatures = listOf("Clear nasalized vowels (ہیں / چلیں)", "Gentle aspiration on 'پھ / کھ / تھ'"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("خوشگوار (khushgawar)", "بہت اچھا", "Pleasant / delightful", "موسم بہت خوشگوار ہے"),
                        RegionalVocabNote("چلیں (chalein)", "آئیے", "Let's go / shall we", "شام کو باہر چلتے ہیں")
                    ),
                    comprehensionQuestion = "What does the speaker suggest doing this evening?",
                    comprehensionOptions = listOf(
                        "Going out for tea near Badshahi Mosque",
                        "Studying indoors until midnight",
                        "Boarding a morning flight"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "They suggest enjoying the pleasant weather with tea near Badshahi Mosque."
                )
            )
            "arabic" -> listOf(
                RegionalAccentSample(
                    id = "acc_ar_cairo",
                    language = "Arabic",
                    regionName = "Cairo (Egyptian Arabic)",
                    countryFlag = "🇪🇬",
                    accentCharacteristics = "Hard 'g' sound for ج (jeem), warm rhythmic tempo, widely understood across the Arab world.",
                    respectNote = "Arabic encompasses Modern Standard Arabic (Fusha) alongside cherished regional spoken varieties.",
                    transcriptInTargetLang = "أهلاً بيك في القاهرة! الجو النهارده جميل جداً، تحب نتمشى على النيل؟",
                    englishTranslation = "Welcome to Cairo! The weather today is very nice; would you like to walk along the Nile?",
                    phoneticFeatures = listOf("Pronounced hard /g/ in 'جميل' (gameel)", "Question intonation at the end"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("النهارده (en-naharda)", "اليوم", "Today", "الجو جميل النهارده"),
                        RegionalVocabNote("أهلاً بيك (ahlan beek)", "أهلاً بك", "Welcome to you", "أهلاً بيك في بيتنا")
                    ),
                    comprehensionQuestion = "What does the speaker invite you to do?",
                    comprehensionOptions = listOf(
                        "Take a walk along the Nile",
                        "Visit a train museum",
                        "Stay indoors because of rain"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "'نتمشى على النيل' means 'let's walk along the Nile'."
                )
            )
            else -> listOf(
                RegionalAccentSample(
                    id = "acc_${language.lowercase().replace(" ", "_")}_standard",
                    language = language,
                    regionName = "$language Core Spoken Standard",
                    countryFlag = "🌍",
                    accentCharacteristics = "Clear articulation, natural phrase stress, and polite everyday cadence in $language.",
                    respectNote = "Every language has vibrant regional varieties shaped by history, geography, and community.",
                    transcriptInTargetLang = "Listen carefully to native prosody, stressed syllables, and polite connectors in $language.",
                    englishTranslation = "Clear everyday $language pronunciation and regional variation listening practice.",
                    phoneticFeatures = listOf("Notice sentence stress and rising/falling intonation", "Compare slow (0.75x) vs. natural speed"),
                    regionalVocabulary = listOf(
                        RegionalVocabNote("Natural Greeting", "Polite opener", "Used to open warm everyday conversations in $language", "Speak a greeting in $language"),
                        RegionalVocabNote("Time Connector", "Then / afterwards", "Connects ideas smoothly without pausing", "Link two sentences in $language")
                    ),
                    comprehensionQuestion = "What should you focus on when listening at 0.75x then 1.0x speed?",
                    comprehensionOptions = listOf(
                        "Phrase-level rhythm, connected words, and natural sentence stress",
                        "Ignoring pronunciation entirely",
                        "Translating word-by-word without context"
                    ),
                    correctOptionIndex = 0,
                    comprehensionExplanation = "Listening at 0.75x then 1.0x trains your ear to recognize natural word boundaries and rhythm."
                )
            )
        }
    }
}
