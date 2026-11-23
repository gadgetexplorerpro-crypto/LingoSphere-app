package com.example.data.model

import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════════
// PRIVACY, CONSENT & LANGUAGE CAPABILITY METADATA
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Explicit user privacy and consent controls for camera capture, microphone audio,
 * stored speaking recordings, and synthetic fluent reference comparisons.
 */
data class MediaPrivacyConsentState(
    val cameraCaptureConsent: Boolean = true,
    val microphoneSpeakingConsent: Boolean = true,
    val storeRecordingsOnDevice: Boolean = true,
    val syntheticFluentReferenceConsent: Boolean = true,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

/**
 * Language-specific technical support and limitation notice so learners get transparent
 * guidance on offline TTS, speech recognition availability, script/romanization, and RTL.
 */
data class LanguageCapabilityNotice(
    val languageCode: String,
    val languageName: String,
    val offlineTtsGenerallyAvailable: Boolean,
    val speechRecognitionSupportNote: String,
    val usesNonLatinScript: Boolean,
    val isRightToLeft: Boolean,
    val emergencyOfflinePackIncluded: Boolean = true
)

// ═══════════════════════════════════════════════════════════════════════════════
// 1. SPEAK WHAT YOU SEE (Contextual Camera Speaking Lesson)
// ═══════════════════════════════════════════════════════════════════════════════

data class RealLifeDetectedItem(
    val targetWord: String,
    val nativeWord: String,
    val pronunciationGuide: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val contextualExplanation: String
)

data class RealLifeObjectRecognition(
    val sceneTitle: String,
    val sceneDescription: String,
    val targetLanguage: String,
    val detectedItems: List<RealLifeDetectedItem>
)

data class SpeakWhatYouSeeQuestion(
    val id: String = UUID.randomUUID().toString(),
    val detectedObject: String,
    val detectedObjectTranslation: String,
    val questionTargetLanguage: String,
    val questionNativeTranslation: String,
    val suggestedVocabulary: List<String>,
    val sampleAnswerTargetLanguage: String,
    val sampleAnswerRomanization: String = ""
)

data class SpeakWhatYouSeeEvaluation(
    val questionId: String,
    val learnerSpokenAnswer: String,
    val answeredQuestionDirectly: Boolean,
    val correctedSentenceTargetLanguage: String,
    val grammarAndWordChoiceFeedback: String,
    val naturalFollowUpPrompt: String,
    val detectedStrengths: List<String>,
    val retryRecommended: Boolean,
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 2. CONVERSATION RESCUE (In-Session Speaking Assistance without Restarting)
// ═══════════════════════════════════════════════════════════════════════════════

data class ConversationRescueSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val targetPhrase: String,
    val romanization: String = "",
    val nativeMeaning: String,
    val RegisterAndNuanceNote: String,
    val whenToUseExplanation: String
)

data class ConversationRescueResult(
    val languageCode: String,
    val conversationContextSummary: String,
    val whyThisMomentIsTricky: String,
    val suggestions: List<ConversationRescueSuggestion>,
    val grammarBridgeTip: String,
    val retryPromptInPlace: String,
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 3. LIVING LANGUAGE WORLD (Persistent Virtual World with Evolving AI Characters)
// ═══════════════════════════════════════════════════════════════════════════════

data class LivingWorldTask(
    val id: String,
    val title: String,
    val communicativeGoal: String,
    val requiredSpokenIntentKeywords: List<String>,
    val minSpokenWordsRequired: Int = 4,
    val isCompleted: Boolean = false
)

data class LivingWorldLocation(
    val id: String,
    val languageCode: String,
    val nameTargetLanguage: String,
    val nameEnglish: String,
    val districtDescription: String,
    val characterName: String,
    val characterRole: String,
    val characterPersonality: String,
    val openingGreetingTarget: String,
    val openingGreetingEnglish: String,
    val tasks: List<LivingWorldTask>,
    val unlockedByDefault: Boolean = true
)

data class LivingWorldCharacterProgress(
    val locationId: String,
    val characterName: String,
    val trustScore: Int = 20, // 0..100 based on meaningful spoken task completion
    val completedTaskIds: List<String> = emptyList(),
    val rememberedLearnerFacts: List<String> = emptyList(),
    val lastConversationSummary: String = "",
    val totalSpokenTurns: Int = 0
)

data class LivingWorldTurnOutcome(
    val characterReplyTarget: String,
    val characterReplyTranslation: String,
    val taskCompletedThisTurn: LivingWorldTask? = null,
    val newlyRememberedFact: String? = null,
    val trustDelta: Int = 0,
    val coachingFeedback: String,
    val suggestedCorrectionIfNeeded: String = "",
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 4. FUTURE ME SPEAKING (Consent-Preserving Synthetic Fluent Comparison)
// ═══════════════════════════════════════════════════════════════════════════════

data class FutureMePracticeDrill(
    val id: String = UUID.randomUUID().toString(),
    val focusArea: String, // e.g., "Linking & Rhythm", "Sentence Stress", "Vowel Clarity"
    val drillInstruction: String,
    val targetPhrase: String,
    val romanization: String = ""
)

data class FutureMeComparisonResult(
    val id: String = UUID.randomUUID().toString(),
    val languageCode: String,
    val learnerTranscript: String,
    val syntheticFluentReferenceText: String,
    val syntheticFluentRomanization: String = "",
    val explicitSyntheticLabel: String = "Synthetic Fluent Reference — Not Voice Cloning",
    val consentVerified: Boolean = true,
    val pacingAndFlowComparison: String,
    val phrasingUpgradeExplanation: String,
    val actionableDrills: List<FutureMePracticeDrill>,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 5. LANGUAGE EMERGENCY MODE (Fast Situation Practice + Offline Phrase Packs)
// ═══════════════════════════════════════════════════════════════════════════════

enum class EmergencySituationCategory(val title: String, val iconLabel: String) {
    DIRECTIONS_TRANSIT("Directions & Transit", "Transit"),
    LOST_BELONGINGS("Lost Passport or Belongings", "Lost Item"),
    MEDICAL_PHARMACY("Medical & Pharmacy Help", "Medical"),
    TRAVEL_DISRUPTION("Hotel & Flight Problems", "Travel"),
    URGENT_ASSISTANCE("Police & Embassy Assistance", "Urgent")
}

data class EmergencyPhraseItem(
    val id: String,
    val languageCode: String,
    val category: EmergencySituationCategory,
    val situationTrigger: String,
    val targetPhrase: String,
    val romanization: String,
    val nativeMeaning: String,
    val expectedLocalReplyTarget: String,
    val expectedLocalReplyMeaning: String,
    val practiceFollowUpPrompt: String
)

data class OfflineEmergencyPackStatus(
    val languageCode: String,
    val isDownloadedOffline: Boolean,
    val phraseCount: Int,
    val lastSyncedEpochMs: Long
)

// ═══════════════════════════════════════════════════════════════════════════════
// 6. AI DEBATE PARTNER (Adaptive Spoken Debates — Neutral, Non-Judgmental Rubric)
// ═══════════════════════════════════════════════════════════════════════════════

data class DebateTopic(
    val id: String,
    val title: String,
    val neutralPromptQuestion: String,
    val suggestedVocabularyByLanguage: Map<String, List<String>>,
    val level: String = "Intermediate"
)

data class DebateTurnEvaluation(
    val aiCounterArgumentTarget: String,
    val aiCounterArgumentTranslation: String,
    val clarityAssessment: String,
    val grammarAssessment: String,
    val vocabularyAssessment: String,
    val listeningAndRebuttalAssessment: String,
    val upgradedExpressionSuggestion: String,
    val neutralityStatement: String = "Feedback evaluates language clarity, structure, and rebuttal responsiveness only — never personal opinions.",
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 7. EXPLAIN IT THREE WAYS (Simultaneous Beginner, Intermediate, Advanced + Check)
// ═══════════════════════════════════════════════════════════════════════════════

data class LevelSpecificExplanation(
    val levelName: String, // "Beginner", "Intermediate", "Advanced"
    val coreExplanation: String,
    val exampleTargetLanguage: String,
    val exampleRomanization: String = "",
    val exampleNativeTranslation: String,
    val keyTakeaway: String
)

data class ThreeWayComprehensionCheck(
    val questionPrompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanationOfAnswer: String
)

data class ThreeWayExplanationResult(
    val queryConcept: String,
    val languageCode: String,
    val beginnerLevel: LevelSpecificExplanation,
    val intermediateLevel: LevelSpecificExplanation,
    val advancedLevel: LevelSpecificExplanation,
    val comprehensionCheck: ThreeWayComprehensionCheck,
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 8. CONVERSATION DETECTIVE (Pragmatics, Indirect Meaning & Multi-Interpretation)
// ═══════════════════════════════════════════════════════════════════════════════

data class CulturalInterpretationOption(
    val id: String,
    val label: String,
    val isPlausibleInterpretation: Boolean,
    val pragmaticNuanceExplanation: String,
    val regionalOrSituationalCaveat: String
)

data class ConversationDetectiveCase(
    val id: String,
    val languageCode: String,
    val title: String,
    val socialSetting: String,
    val speakerUtteranceTarget: String,
    val speakerUtteranceRomanization: String,
    val literalTranslation: String,
    val toneAndContextClues: String,
    val interpretations: List<CulturalInterpretationOption>,
    val multiPerspectiveNote: String,
    val politeResponseOptionsTarget: List<String>
)

// ═══════════════════════════════════════════════════════════════════════════════
// 9. LANGUAGE HABIT COACH (Routine-Bound Speaking Missions & Verified Completion)
// ═══════════════════════════════════════════════════════════════════════════════

enum class HabitRoutineSlot(val id: String, val label: String, val defaultTimeWindow: String) {
    MORNING_COFFEE("morning_coffee", "Morning Coffee", "07:30 - 09:00"),
    DAILY_COMMUTE("daily_commute", "Daily Commute", "08:30 - 10:00"),
    LUNCH_BREAK("lunch_break", "Lunch Break", "12:00 - 13:30"),
    EVENING_REFLECTION("evening_reflection", "Evening Wind-Down", "19:30 - 21:30")
}

data class HabitSpeakingMission(
    val id: String,
    val routineSlot: HabitRoutineSlot,
    val languageCode: String,
    val missionTitle: String,
    val speakingPromptTarget: String,
    val speakingPromptEnglish: String,
    val targetKeywords: List<String>,
    val estimatedSeconds: Int = 45
)

data class VerifiedHabitCompletion(
    val id: String = UUID.randomUUID().toString(),
    val routineSlotId: String,
    val languageCode: String,
    val missionId: String,
    val userSpokenOrWrittenTranscript: String,
    val wordCount: Int,
    val feedbackSummary: String,
    val completedAtEpochMs: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════════════════════
// 10. TEACH THE AI (Curious AI Student Persona Evaluating Explanation Clarity)
// ═══════════════════════════════════════════════════════════════════════════════

data class TeachAiTopicOption(
    val id: String,
    val title: String,
    val description: String,
    val starterQuestionFromAiByLanguage: Map<String, String>
)

data class TeachAiTurnEvaluation(
    val aiStudentFollowUpQuestionTarget: String,
    val aiStudentFollowUpTranslation: String,
    val whatAiStudentUnderstoodSummary: String,
    val clarityAndCoherenceFeedback: String,
    val suggestedClearerPhrasingTarget: String,
    val conceptClarityReached: Boolean,
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 11. CONVERSATION REPAIR LAB (Real-Time Misunderstanding & Clarification Drills)
// ═══════════════════════════════════════════════════════════════════════════════

enum class RepairStrategyType(val label: String) {
    ASK_TO_REPEAT("Ask to Repeat / Slow Down"),
    PARAPHRASE_MEANING("Paraphrase with Simpler Words"),
    CONFIRM_UNDERSTANDING("Clarify & Confirm Intent"),
    SELF_CORRECT_SLIP("Recover from a Word Mix-Up")
}

data class ConversationRepairScenario(
    val id: String,
    val languageCode: String,
    val title: String,
    val strategyType: RepairStrategyType,
    val breakdownContext: String,
    val partnerConfusingLineTarget: String,
    val partnerConfusingLineRomanization: String,
    val partnerConfusingLineMeaning: String,
    val repairGoalDescription: String,
    val modelRepairPhrasesTarget: List<String>
)

data class ConversationRepairTurnResult(
    val scenarioId: String,
    val learnerRepairAttempt: String,
    val repairSucceeded: Boolean,
    val partnerResolvedReplyTarget: String,
    val partnerResolvedReplyMeaning: String,
    val strategyFeedback: String,
    val recommendedPoliteRepairPhrase: String,
    val aiErrorMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// 12. LANGUAGE SKILL PASSPORT (Evidence-Based Practical Portfolio, Non-Certified)
// ═══════════════════════════════════════════════════════════════════════════════

data class SkillPassportCompetency(
    val id: String,
    val domainTitle: String,
    val practicalCanDoStatement: String,
    val verifiedEvidenceCount: Int,
    val latestEvidenceSummary: String,
    val measurableGrowthNote: String,
    val isDemonstrated: Boolean
)

data class LanguageSkillPassportSummary(
    val languageCode: String,
    val languageName: String,
    val competencies: List<SkillPassportCompetency>,
    val totalSpeakingRecordingsCount: Int,
    val totalCompletedPracticalTasksCount: Int,
    val vocabularyMasteredCount: Int,
    val grammarRepairDemonstrationsCount: Int,
    val officialDisclaimer: String = "Practical Self-Assessment Portfolio — Based on in-app speaking recordings, completed communication tasks, and practice history. This passport does not constitute an official certification or CEFR-accredited examination."
)

// ═══════════════════════════════════════════════════════════════════════════════
// CURATED MULTI-LANGUAGE CATALOGS FOR ALL 12 PRACTICAL FEATURES
// ═══════════════════════════════════════════════════════════════════════════════

object PracticalFeaturesCatalog {

    fun capabilityNoticeForLanguage(languageCode: String): LanguageCapabilityNotice {
        val code = languageCode.lowercase()
        val lang = Language.findByCode(code)
        val nonLatin = code in setOf("ja", "zh", "ko", "ar", "ur", "ps", "fa", "hi", "ru", "th", "el", "bn", "pa")
        val rtl = code in setOf("ar", "ur", "ps", "fa", "he")
        val ttsNote = when (code) {
            "es", "fr", "de", "it", "pt", "ja", "ko", "zh" ->
                "On-device TTS & speech recognition are widely supported on Android. Download the local voice pack in Android Text-to-Speech settings for full airplane-mode playback."
            "ar", "ur", "ps", "fa", "hi", "ru", "tr" ->
                "Romanization and phonetic guides are displayed alongside native script. Offline speech recognition depends on installed Google Voice Typing language packs."
            else ->
                "Phonetic pronunciation and offline text cards are always available even if your device lacks an offline TTS voice for ${lang.name}."
        }
        return LanguageCapabilityNotice(
            languageCode = code,
            languageName = lang.name,
            offlineTtsGenerallyAvailable = true,
            speechRecognitionSupportNote = ttsNote,
            usesNonLatinScript = nonLatin,
            isRightToLeft = rtl,
            emergencyOfflinePackIncluded = true
        )
    }

    fun livingWorldLocationsForLanguage(languageCode: String): List<LivingWorldLocation> {
        return when (languageCode.lowercase()) {
            "es" -> listOf(
                LivingWorldLocation(
                    id = "lw_es_mercado",
                    languageCode = "es",
                    nameTargetLanguage = "Mercado de San Miguel",
                    nameEnglish = "San Miguel Neighborhood Market",
                    districtDescription = "A bustling Madrid food hall where vendors remember regular customers and negotiate seasonal ingredients.",
                    characterName = "Doña Carmen",
                    characterRole = "Artisanal Olive & Cheese Vendor",
                    characterPersonality = "Warm, observant, appreciates polite greetings and specific culinary questions.",
                    openingGreetingTarget = "¡Hola! Qué alegría verte por el puesto hoy. ¿Qué vas a preparar de rico esta semana?",
                    openingGreetingEnglish = "Hello! How lovely to see you at the stall today. What delicious dish are you preparing this week?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_es_mercado_t1",
                            title = "Explain Your Dish & Ask for a Recommendation",
                            communicativeGoal = "Tell Doña Carmen what meal you are cooking and ask which cheese or olive pairs best.",
                            requiredSpokenIntentKeywords = listOf("cocinar", "preparar", "queso", "aceitunas", "recomienda", "hacer", "cena", "comida")
                        ),
                        LivingWorldTask(
                            id = "lw_es_mercado_t2",
                            title = "Ask About Origin or Dietary Preference",
                            communicativeGoal = "Ask where a product comes from or request a specific portion weight in grams.",
                            requiredSpokenIntentKeywords = listOf("gramos", "dónde", "origen", "fresco", "probar", "mitad", "cuánto", "cuesta")
                        )
                    )
                ),
                LivingWorldLocation(
                    id = "lw_es_cowork",
                    languageCode = "es",
                    nameTargetLanguage = "Estudio Creativo Chamberí",
                    nameEnglish = "Chamberí Community Co-Working Hub",
                    districtDescription = "A neighborhood studio where local designers, writers, and newcomers collaborate on community events.",
                    characterName = "Mateo",
                    characterRole = "Community Event Coordinator",
                    characterPersonality = "Energetic, collaborative, curious about your skills and schedule.",
                    openingGreetingTarget = "¡Buenas! Estamos organizando el taller cultural del viernes. ¿Cómo te gustaría participar?",
                    openingGreetingEnglish = "Hi there! We're organizing Friday's cultural workshop. How would you like to get involved?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_es_cowork_t1",
                            title = "Pitch an Idea or Skill for the Workshop",
                            communicativeGoal = "Introduce a topic or activity you can help with and state when you are free.",
                            requiredSpokenIntentKeywords = listOf("puedo", "ayudar", "viernes", "tarde", "mañana", "idea", "taller", "libre", "gustaría")
                        ),
                        LivingWorldTask(
                            id = "lw_es_cowork_t2",
                            title = "Coordinate Logistics & Confirm Time",
                            communicativeGoal = "Confirm what equipment is needed and agree on a meeting time.",
                            requiredSpokenIntentKeywords = listOf("hora", "necesitamos", "mesa", "proyector", "equipo", "vemos", "confirmar")
                        )
                    )
                )
            )
            "fr" -> listOf(
                LivingWorldLocation(
                    id = "lw_fr_librairie",
                    languageCode = "fr",
                    nameTargetLanguage = "Librairie du Quartier Latin",
                    nameEnglish = "Latin Quarter Independent Bookshop",
                    districtDescription = "A cozy Parisian bookshop hosting evening discussion circles with regulars.",
                    characterName = "Claire",
                    characterRole = "Bookshop Owner & Reading Circle Host",
                    characterPersonality = "Thoughtful, articulate, loves hearing what topics inspire visitors.",
                    openingGreetingTarget = "Bonjour ! Bienvenue à la librairie. Quel genre d'histoires ou de sujets vous passionne en ce moment ?",
                    openingGreetingEnglish = "Hello! Welcome to the bookshop. What kind of stories or subjects fascinate you right now?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_fr_lib_t1",
                            title = "Describe Your Reading Interests",
                            communicativeGoal = "Explain what themes you enjoy reading and ask Claire for a French recommendation.",
                            requiredSpokenIntentKeywords = listOf("aime", "lire", "livre", "histoire", "roman", "conseillez", "cherche", "sujet")
                        ),
                        LivingWorldTask(
                            id = "lw_fr_lib_t2",
                            title = "Join the Weekly Discussion Circle",
                            communicativeGoal = "Ask about the next book club evening and explain why you'd like to attend.",
                            requiredSpokenIntentKeywords = listOf("soirée", "quand", "participer", "rencontre", "heure", "prochain", "discuter")
                        )
                    )
                ),
                LivingWorldLocation(
                    id = "lw_fr_boulangerie",
                    languageCode = "fr",
                    nameTargetLanguage = "Atelier Boulanger Montmartre",
                    nameEnglish = "Montmartre Neighborhood Bakery",
                    districtDescription = "A neighborhood bakery where neighbors place custom orders for weekend gatherings.",
                    characterName = "Monsieur Laurent",
                    characterRole = "Master Baker",
                    characterPersonality = "Proud of his sourdough craft, helpful when customers explain their occasion.",
                    openingGreetingTarget = "Bonjour ! Le fournil tourne à plein régime ce matin. Que puis-je vous préparer ?",
                    openingGreetingEnglish = "Good morning! The bakehouse is in full swing this morning. What can I prepare for you?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_fr_boul_t1",
                            title = "Order for a Weekend Gathering",
                            communicativeGoal = "Order pastries or bread for several guests and ask about ingredients.",
                            requiredSpokenIntentKeywords = listOf("voudrais", "croissant", "pain", "personnes", "amis", "beurre", "samedi", "commande")
                        )
                    )
                )
            )
            "ja" -> listOf(
                LivingWorldLocation(
                    id = "lw_ja_kissaten",
                    languageCode = "ja",
                    nameTargetLanguage = "純喫茶「木漏れ日」 (Komorebi Café)",
                    nameEnglish = "Komorebi Neighborhood Kissaten",
                    districtDescription = "A quiet Tokyo coffeehouse in Yanaka where the master brews hand-drip coffee and chats with neighbors.",
                    characterName = "Takahashi-san",
                    characterRole = "Kissaten Master",
                    characterPersonality = "Calm, attentive, remembers how guests take their coffee and their neighborhood routines.",
                    openingGreetingTarget = "いらっしゃいませ。今日はどんなコーヒーの気分ですか？ゆっくりしていってくださいね。",
                    openingGreetingEnglish = "Welcome. What kind of coffee are you in the mood for today? Please take your time.",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_ja_kissa_t1",
                            title = "Order Coffee & Share Your Day's Plan",
                            communicativeGoal = "Order a drink and tell Takahashi-san what you are doing in Tokyo today.",
                            requiredSpokenIntentKeywords = listOf("コーヒー", "お願いします", "今日", "行く", "散歩", "仕事", "勉強", "ホット", "アイス", "coffee", "onegai")
                        ),
                        LivingWorldTask(
                            id = "lw_ja_kissa_t2",
                            title = "Ask About a Local Neighborhood Spot",
                            communicativeGoal = "Ask Takahashi-san for a quiet place or local shop recommendation nearby.",
                            requiredSpokenIntentKeywords = listOf("おすすめ", "近く", "どこ", "場所", "静か", "お店", "osusume", "chikaku", "doko")
                        )
                    )
                )
            )
            "de" -> listOf(
                LivingWorldLocation(
                    id = "lw_de_markt",
                    languageCode = "de",
                    nameTargetLanguage = "Viktualienmarkt München",
                    nameEnglish = "Munich Neighborhood Market",
                    districtDescription = "A lively Bavarian open-air food market where regular customers chat with stall owners about seasonal specialties.",
                    characterName = "Frau Weber",
                    characterRole = "Bäckerei & Käseverkäuferin",
                    characterPersonality = "Friendly, attentive, values polite 'Guten Tag' greetings and curiosity about local food.",
                    openingGreetingTarget = "Guten Tag! Schön, Sie heute am Stand zu sehen. Was möchten Sie denn heute kochen?",
                    openingGreetingEnglish = "Good day! Nice to see you at the stall today. What are you planning to cook today?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_de_markt_t1",
                            title = "Describe Your Meal & Ask for Advice",
                            communicativeGoal = "Tell Frau Weber what dish you are making and ask which cheese or bread pairs well.",
                            requiredSpokenIntentKeywords = listOf("kochen", "käse", "brot", "empfehlen", "möchte", "essen", "abend", "frisch")
                        ),
                        LivingWorldTask(
                            id = "lw_de_markt_t2",
                            title = "Order a Weight & Ask the Price",
                            communicativeGoal = "Ask for 200 grams or a small piece and confirm the price.",
                            requiredSpokenIntentKeywords = listOf("gramm", "kostet", "preis", "bitte", "stück", "karte", "zahlen")
                        )
                    )
                )
            )
            "ur" -> listOf(
                LivingWorldLocation(
                    id = "lw_ur_bazaar",
                    languageCode = "ur",
                    nameTargetLanguage = "لاہور فوڈ اسٹریٹ اور چائے خانہ",
                    nameEnglish = "Lahore Tea House & Market",
                    districtDescription = "A welcoming neighborhood tea stall where regulars share tea, snacks, and stories about their week.",
                    characterName = "بلال (Bilal)",
                    characterRole = "Chai Café Host",
                    characterPersonality = "Warm, hospitable, remembers how guests like their chai.",
                    openingGreetingTarget = "السلام علیکم! خوش آمدید۔ آج آپ کیسی چائے پینا پسند کریں گے؟",
                    openingGreetingEnglish = "Assalam-o-Alaikum! Welcome. What kind of tea would you like to drink today?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_ur_chai_t1",
                            title = "Order Chai & Share Your Day",
                            communicativeGoal = "Greet Bilal politely in Urdu, order tea with less sugar, and mention what you are doing today.",
                            requiredSpokenIntentKeywords = listOf("چائے", "سلام", "چینی", "کم", "آج", "پینا", "chai", "salam", "chahiye")
                        )
                    )
                )
            )
            "ar" -> listOf(
                LivingWorldLocation(
                    id = "lw_ar_souq",
                    languageCode = "ar",
                    nameTargetLanguage = "مقهى وسوق الحي",
                    nameEnglish = "Neighborhood Café & Spice Market",
                    districtDescription = "A welcoming café beside an aromatic local market where hosts greet neighbors warmly.",
                    characterName = "طارق (Tariq)",
                    characterRole = "Café Host",
                    characterPersonality = "Generous, patient, enjoys helping learners practice Arabic.",
                    openingGreetingTarget = "أهلاً وسهلاً! كيف حالك اليوم؟ ماذا تحب أن تطلب؟",
                    openingGreetingEnglish = "Welcome! How are you today? What would you like to order?",
                    tasks = listOf(
                        LivingWorldTask(
                            id = "lw_ar_souq_t1",
                            title = "Greet & Order a Drink in Arabic",
                            communicativeGoal = "Greet Tariq, ask for mint tea or Arabic coffee, and say thank you.",
                            requiredSpokenIntentKeywords = listOf("أهلا", "شاي", "قهوة", "من فضلك", "شكراً", "اليوم", "ahlan", "shay", "qahwa")
                        )
                    )
                )
            )
            else -> {
                val lang = Language.findByCode(languageCode)
                listOf(
                    LivingWorldLocation(
                        id = "lw_${languageCode}_plaza",
                        languageCode = languageCode,
                        nameTargetLanguage = "${lang.name} Community Hub & Café",
                        nameEnglish = "${lang.name} Cultural Café & Market",
                        districtDescription = "A lively local gathering place where residents discuss daily plans, local food, and community events in ${lang.name}.",
                        characterName = "Elena",
                        characterRole = "Local Host & Barista",
                        characterPersonality = "Welcoming, patient, remembers your goals and encourages natural conversation.",
                        openingGreetingTarget = "Welcome! Tell me in ${lang.name} what brings you to our neighborhood today and what you'd like to order.",
                        openingGreetingEnglish = "Welcome! Tell me what brings you to our neighborhood today and what you'd like to order.",
                        tasks = listOf(
                            LivingWorldTask(
                                id = "lw_${languageCode}_t1",
                                title = "Introduce Yourself & Place an Order",
                                communicativeGoal = "Greet Elena in ${lang.name}, order a drink or snack, and mention one goal for your day.",
                                requiredSpokenIntentKeywords = listOf("coffee", "tea", "please", "today", "like", "order", "ciao", "olá", "안녕하세요", "你好", "नमस्ते")
                            ),
                            LivingWorldTask(
                                id = "lw_${languageCode}_t2",
                                title = "Ask for a Local Recommendation",
                                communicativeGoal = "Ask Elena in ${lang.name} what cultural spot or event she recommends visiting this weekend.",
                                requiredSpokenIntentKeywords = listOf("recommend", "where", "weekend", "visit", "place", "event")
                            )
                        )
                    )
                )
            }
        }
    }

    fun emergencyPhrasesForLanguage(languageCode: String): List<EmergencyPhraseItem> {
        return when (languageCode.lowercase()) {
            "es" -> listOf(
                EmergencyPhraseItem(
                    id = "em_es_1",
                    languageCode = "es",
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "You missed the last train or aren't sure which platform goes to the city center.",
                    targetPhrase = "Disculpe, perdí mi tren. ¿Cómo puedo llegar al centro desde aquí de forma segura?",
                    romanization = "Dis-KOOL-peh, per-DEE mee tren. KOH-moh PWEH-doh yeh-GAR al SEN-troh DES-deh ah-KEE deh FOR-mah seh-GOO-rah?",
                    nativeMeaning = "Excuse me, I missed my train. How can I get to the city center safely from here?",
                    expectedLocalReplyTarget = "Puede tomar el autobús nocturno en la salida norte o pedir un taxi oficial allí.",
                    expectedLocalReplyMeaning = "You can take the night bus at the north exit or get an official taxi over there.",
                    practiceFollowUpPrompt = "Confirm which exit they mentioned and ask how long the ride takes."
                ),
                EmergencyPhraseItem(
                    id = "em_es_2",
                    languageCode = "es",
                    category = EmergencySituationCategory.LOST_BELONGINGS,
                    situationTrigger = "You left your backpack or passport in a café or train car.",
                    targetPhrase = "Necesito ayuda urgente: olvidé mi mochila negra con mi pasaporte hace diez minutos.",
                    romanization = "Neh-seh-SEE-toh ah-YOO-dah oor-HEN-teh: ol-vee-DEH mee moh-CHEE-lah NEH-grah kohn mee pah-sah-POR-teh AH-seh dyehs mee-NOO-tohs.",
                    nativeMeaning = "I need urgent help: I forgot my black backpack with my passport ten minutes ago.",
                    expectedLocalReplyTarget = "Tranquilo, vamos a llamar a objetos perdidos. ¿Tiene algún número de contacto?",
                    expectedLocalReplyMeaning = "Stay calm, let's call lost and found. Do you have a contact number?",
                    practiceFollowUpPrompt = "Describe one item inside the bag and state how they can reach you."
                ),
                EmergencyPhraseItem(
                    id = "em_es_3",
                    languageCode = "es",
                    category = EmergencySituationCategory.MEDICAL_PHARMACY,
                    situationTrigger = "You have an allergic reaction or sudden fever and need a pharmacist.",
                    targetPhrase = "Soy alérgico a la penicilina y necesito hablar con un farmacéutico o médico ahora mismo.",
                    romanization = "Soy ah-LER-hee-koh ah lah peh-nee-see-LEE-nah ee neh-seh-SEE-toh ah-BLAR kohn oon far-mah-SEW-tee-koh oh MEH-dee-koh ah-OH-rah MEES-moh.",
                    nativeMeaning = "I am allergic to penicillin and I need to speak with a pharmacist or doctor right now.",
                    expectedLocalReplyTarget = "Pase por aquí, por favor. ¿Desde cuándo tiene estos síntomas?",
                    expectedLocalReplyMeaning = "Come this way, please. Since when have you had these symptoms?",
                    practiceFollowUpPrompt = "Explain when your symptoms started in Spanish."
                ),
                EmergencyPhraseItem(
                    id = "em_es_4",
                    languageCode = "es",
                    category = EmergencySituationCategory.URGENT_ASSISTANCE,
                    situationTrigger = "Your phone battery is dying and you need to contact your hotel or embassy.",
                    targetPhrase = "Me estoy quedando sin batería. ¿Podría ayudarme a llamar a mi hotel o a mi embajada, por favor?",
                    romanization = "Meh es-TOY keh-DAN-doh seen bah-teh-REE-ah. Poh-DREE-ah ah-yoo-DAR-meh ah yah-MAR ah mee oh-TEL oh ah mee em-bah-HAH-dah?",
                    nativeMeaning = "My battery is running out. Could you help me call my hotel or embassy, please?",
                    expectedLocalReplyTarget = "Claro, aquí tiene un cargador y puede usar el teléfono de recepción.",
                    expectedLocalReplyMeaning = "Of course, here is a charger and you can use the reception phone.",
                    practiceFollowUpPrompt = "Thank them politely and state the name of your hotel."
                )
            )
            "fr" -> listOf(
                EmergencyPhraseItem(
                    id = "em_fr_1",
                    languageCode = "fr",
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "A metro line is closed and you need an alternative route.",
                    targetPhrase = "Excusez-moi, la ligne est fermée. Quel est le moyen le plus sûr pour rentrer à mon hôtel ?",
                    romanization = "Ex-kew-zay MWAH, lah leeny eh fer-MAY. Kel eh luh mwa-yen luh plew SEWR poor rahn-TRAY ah mohn oh-TEL?",
                    nativeMeaning = "Excuse me, the line is closed. What is the safest way to get back to my hotel?",
                    expectedLocalReplyTarget = "Prenez le bus de remplacement devant la gare jusqu'à Châtelet.",
                    expectedLocalReplyMeaning = "Take the replacement bus in front of the station to Châtelet.",
                    practiceFollowUpPrompt = "Repeat the destination station to confirm you understood."
                ),
                EmergencyPhraseItem(
                    id = "em_fr_2",
                    languageCode = "fr",
                    category = EmergencySituationCategory.LOST_BELONGINGS,
                    situationTrigger = "Your wallet or passport was lost and you need to file a report.",
                    targetPhrase = "J'ai perdu mon portefeuille et mon passeport. Où se trouve le bureau des objets trouvés ou le commissariat ?",
                    romanization = "Zhay per-DEW mohn port-fuh-yuh eh mohn pahs-POR. Oo suh troov luh bew-ROH dayz ob-zhay troo-VAY oo luh koh-mee-sah-RYAH?",
                    nativeMeaning = "I lost my wallet and passport. Where is the lost-and-found office or police station?",
                    expectedLocalReplyTarget = "Le bureau est au bout du couloir à droite, je vous accompagne.",
                    expectedLocalReplyMeaning = "The office is at the end of the hall on the right, I'll walk you there.",
                    practiceFollowUpPrompt = "Thank them and mention when you last saw your wallet."
                ),
                EmergencyPhraseItem(
                    id = "em_fr_3",
                    languageCode = "fr",
                    category = EmergencySituationCategory.MEDICAL_PHARMACY,
                    situationTrigger = "You need urgent help at a pharmacy.",
                    targetPhrase = "J'ai besoin d'un médecin ou d'un pharmacien rapidement, je ne me sens pas bien.",
                    romanization = "Zhay buh-ZWAN duhn mayd-SAN oo duhn far-mah-SYAN rah-peed-MAHN, zhuh nuh muh sahn pah BYAN.",
                    nativeMeaning = "I need a doctor or pharmacist quickly, I don't feel well.",
                    expectedLocalReplyTarget = "Asseyez-vous ici, le pharmacien arrive tout de suite.",
                    expectedLocalReplyMeaning = "Sit down here, the pharmacist is coming right away.",
                    practiceFollowUpPrompt = "Describe your main symptom or allergy clearly."
                )
            )
            "ja" -> listOf(
                EmergencyPhraseItem(
                    id = "em_ja_1",
                    languageCode = "ja",
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "You are lost inside a large train station and need the right platform.",
                    targetPhrase = "すみません、道に迷いました。東京駅へ行くには何番線に乗ればいいですか？",
                    romanization = "Sumimasen, michi ni mayoimashita. Tokyo-eki e iku ni wa nan-bansen ni noreba ii desu ka?",
                    nativeMeaning = "Excuse me, I'm lost. Which platform should I take to go to Tokyo Station?",
                    expectedLocalReplyTarget = "3番線ですよ。あの階段を上がってください。",
                    expectedLocalReplyMeaning = "It's platform 3. Please go up those stairs.",
                    practiceFollowUpPrompt = "Confirm 'Platform 3' and thank the station attendant."
                ),
                EmergencyPhraseItem(
                    id = "em_ja_2",
                    languageCode = "ja",
                    category = EmergencySituationCategory.LOST_BELONGINGS,
                    situationTrigger = "You left your bag on the train and are speaking to the Koban or station office.",
                    targetPhrase = "電車の中に黒いカバンを忘れました。中にパスポートが入っています。",
                    romanization = "Densha no naka ni kuroi kaban o wasuremashita. Naka ni pasupooto ga haitte imasu.",
                    nativeMeaning = "I left my black bag on the train. My passport is inside.",
                    expectedLocalReplyTarget = "何時ごろの電車ですか？すぐ確認しますね。",
                    expectedLocalReplyMeaning = "Roughly what time was the train? I'll check right away.",
                    practiceFollowUpPrompt = "Tell the attendant what time you got off the train."
                ),
                EmergencyPhraseItem(
                    id = "em_ja_3",
                    languageCode = "ja",
                    category = EmergencySituationCategory.MEDICAL_PHARMACY,
                    situationTrigger = "You need medical attention or a hospital that can assist travelers.",
                    targetPhrase = "具合が悪いです。近くに病院か薬局はありますか？",
                    romanization = "Guai ga warui desu. Chikaku ni byouin ka yakkyoku wa arimasu ka?",
                    nativeMeaning = "I feel unwell. Is there a hospital or pharmacy nearby?",
                    expectedLocalReplyTarget = "駅の隣にクリニックがあります。案内しましょうか？",
                    expectedLocalReplyMeaning = "There is a clinic next to the station. Shall I guide you there?",
                    practiceFollowUpPrompt = "Accept their help politely and explain what hurts."
                )
            )
            "de" -> listOf(
                EmergencyPhraseItem(
                    id = "em_de_1",
                    languageCode = "de",
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "You missed your train or aren't sure which platform goes to the city center.",
                    targetPhrase = "Entschuldigung, ich habe meinen Zug verpasst. Wie komme ich sicher ins Stadtzentrum?",
                    romanization = "Ent-SHOOL-dee-goong, ikh HAH-beh MY-nen tsook fer-PAHST. Vee KOM-meh ikh ZIKH-er ins SHTAT-tsen-troom?",
                    nativeMeaning = "Excuse me, I missed my train. How can I get to the city center safely?",
                    expectedLocalReplyTarget = "Nehmen Sie die S-Bahn von Gleis 3 oder ein offizielles Taxi am Ausgang.",
                    expectedLocalReplyMeaning = "Take the S-Bahn from platform 3 or an official taxi at the exit.",
                    practiceFollowUpPrompt = "Confirm the platform number in German."
                ),
                EmergencyPhraseItem(
                    id = "em_de_2",
                    languageCode = "de",
                    category = EmergencySituationCategory.LOST_BELONGINGS,
                    situationTrigger = "You misplaced your bag or passport at a station.",
                    targetPhrase = "Ich brauche dringend Hilfe: Ich habe meinen Rucksack mit meinem Reisepass verloren.",
                    romanization = "Ikh BROW-kheh DRING-ent HIL-feh: ikh HAH-beh MY-nen ROOK-zak mit MY-nem RY-zeh-pas fer-LOH-ren.",
                    nativeMeaning = "I need urgent help: I lost my backpack with my passport.",
                    expectedLocalReplyTarget = "Gehen Sie bitte sofort zum Fundbüro gegenüber dem Haupteingang.",
                    expectedLocalReplyMeaning = "Please go immediately to the lost-and-found office across from the main entrance.",
                    practiceFollowUpPrompt = "Ask if they can call the lost-and-found desk."
                ),
                EmergencyPhraseItem(
                    id = "em_de_3",
                    languageCode = "de",
                    category = EmergencySituationCategory.MEDICAL_PHARMACY,
                    situationTrigger = "You need urgent help at a pharmacy.",
                    targetPhrase = "Ich fühle mich nicht wohl und habe eine Allergie. Kann ich bitte mit einem Apotheker sprechen?",
                    romanization = "Ikh FYOO-leh mikh nikht vohl oont HAH-beh Y-neh al-er-GEE...",
                    nativeMeaning = "I don't feel well and I have an allergy. May I speak with a pharmacist please?",
                    expectedLocalReplyTarget = "Natürlich, kommen Sie bitte herein und erklären Sie Ihre Symptome.",
                    expectedLocalReplyMeaning = "Of course, please come inside and explain your symptoms.",
                    practiceFollowUpPrompt = "Describe your allergy clearly in German."
                )
            )
            "ur" -> listOf(
                EmergencyPhraseItem(
                    id = "em_ur_1",
                    languageCode = "ur",
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "You need directions back to your hotel or main station.",
                    targetPhrase = "معاف کیجیے، میں راستہ بھول گیا ہوں۔ کیا آپ مجھے مرکزی اسٹیشن یا ہوٹل کا راستہ بتا سکتے ہیں؟",
                    romanization = "Maaf kijiye, main rasta bhool gaya hoon. Kya aap mujhe markazi station ya hotel ka rasta bata sakte hain?",
                    nativeMeaning = "Excuse me, I lost my way. Could you tell me the way to the main station or my hotel?",
                    expectedLocalReplyTarget = "جی بالکل، سیدھے جائیے اور اگلے چوک سے بائیں مڑ جائیے۔",
                    expectedLocalReplyMeaning = "Yes of course, go straight and turn left at the next intersection.",
                    practiceFollowUpPrompt = "Thank them and confirm if you should turn left or right."
                ),
                EmergencyPhraseItem(
                    id = "em_ur_2",
                    languageCode = "ur",
                    category = EmergencySituationCategory.MEDICAL_PHARMACY,
                    situationTrigger = "You feel unwell and need a doctor or pharmacy quickly.",
                    targetPhrase = "میری طبیعت ٹھیک نہیں ہے، کیا یہاں قریب کوئی ڈاکٹر یا میڈیکل اسٹور ہے؟",
                    romanization = "Meri tabiyat theek nahi hai, kya yahan qareeb koi doctor ya medical store hai?",
                    nativeMeaning = "I don't feel well; is there a doctor or pharmacy nearby?",
                    expectedLocalReplyTarget = "جی ہاں، سامنے سڑک پار چوبیس گھنٹے کھلا میڈیکل اسٹور ہے۔",
                    expectedLocalReplyMeaning = "Yes, right across the street there is a 24-hour pharmacy.",
                    practiceFollowUpPrompt = "Ask how to explain a fever or allergy."
                )
            )
            "ar" -> listOf(
                EmergencyPhraseItem(
                    id = "em_ar_1",
                    languageCode = "ar",
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "You need directions to the central station or your hotel.",
                    targetPhrase = "لو سمحت، أين تقع محطة القطار الرئيسية؟ أحتاج للعودة إلى فندقي.",
                    romanization = "Law samaht, ayna taqa' mahattat al-qitar al-ra'isiyya? Ahtaj lil-'awda ila funduqi.",
                    nativeMeaning = "Excuse me, where is the main train station? I need to get back to my hotel.",
                    expectedLocalReplyTarget = "اتجه للأمام ثم انعطف يميناً عند الإشارة، المحطة قريبة جداً.",
                    expectedLocalReplyMeaning = "Head straight then turn right at the traffic light; the station is very close.",
                    practiceFollowUpPrompt = "Thank them politely and repeat the direction."
                ),
                EmergencyPhraseItem(
                    id = "em_ar_2",
                    languageCode = "ar",
                    category = EmergencySituationCategory.MEDICAL_PHARMACY,
                    situationTrigger = "You need medical or pharmacy assistance quickly.",
                    targetPhrase = "لا أشعر أنني بخير ولدي حساسية، هل يوجد صيدلي أو طبيب قريب؟",
                    romanization = "La ash'ur annani bi-khayr wa ladayya hasasiyya, hal yujad saydali aw tabib qarib?",
                    nativeMeaning = "I don't feel well and I have an allergy; is there a pharmacist or doctor nearby?",
                    expectedLocalReplyTarget = "نعم، هناك صيدلية مفتوحة على الجانب الآخر من الشارع.",
                    expectedLocalReplyMeaning = "Yes, there is an open pharmacy on the other side of the street.",
                    practiceFollowUpPrompt = "State your symptom clearly."
                )
            )
            else -> listOf(
                EmergencyPhraseItem(
                    id = "em_${languageCode}_1",
                    languageCode = languageCode,
                    category = EmergencySituationCategory.DIRECTIONS_TRANSIT,
                    situationTrigger = "You need safe directions back to your hotel or central station.",
                    targetPhrase = "Excuse me, I lost my way. How can I get to the central station safely?",
                    romanization = "Phonetic guide generated dynamically for ${Language.findByCode(languageCode).name}",
                    nativeMeaning = "Excuse me, I lost my way. How can I get to the central station safely?",
                    expectedLocalReplyTarget = "Follow the main avenue two blocks and take the metro at the square.",
                    expectedLocalReplyMeaning = "Follow the main avenue two blocks and take the metro at the square.",
                    practiceFollowUpPrompt = "Repeat the directions back to confirm you understood."
                ),
                EmergencyPhraseItem(
                    id = "em_${languageCode}_2",
                    languageCode = languageCode,
                    category = EmergencySituationCategory.LOST_BELONGINGS,
                    situationTrigger = "You misplaced your bag or passport and need the lost-and-found desk.",
                    targetPhrase = "I need help: I left my bag with my passport ten minutes ago. Where is lost and found?",
                    romanization = "Clear emergency statement in ${Language.findByCode(languageCode).name}",
                    nativeMeaning = "I need help: I left my bag with my passport ten minutes ago. Where is lost and found?",
                    expectedLocalReplyTarget = "Please come to the service desk; we will call security immediately.",
                    expectedLocalReplyMeaning = "Please come to the service desk; we will call security immediately.",
                    practiceFollowUpPrompt = "Describe the color of your bag and give your contact info."
                )
            )
        }
    }

    fun debateTopics(): List<DebateTopic> = listOf(
        DebateTopic(
            id = "debate_remote_work",
            title = "Remote Work vs. In-Person Collaboration",
            neutralPromptQuestion = "Does working remotely help people balance life better, or does working together in person build stronger teams?",
            suggestedVocabularyByLanguage = mapOf(
                "es" to listOf("Sin embargo", "Por un lado", "Productividad", "Equilibrio laboral", "Colaboración"),
                "fr" to listOf("En revanche", "D'une part", "Télétravail", "Cohésion d'équipe", "Équilibre"),
                "ja" to listOf("一方で (Ippou de)", "効率 (Kouritsu)", "コミュニケーション", "メリット", "柔軟性 (Juunansei)"),
                "de" to listOf("Einerseits", "Andererseits", "Teamarbeit", "Flexibilität", "Produktivität"),
                "it" to listOf("Tuttavia", "Da un lato", "Lavoro agile", "Collaborazione", "Equilibrio"),
                "pt" to listOf("Por outro lado", "No entanto", "Produtividade", "Equilíbrio", "Equipa"),
                "ur" to listOf("تاہم (Taham)", "ایک طرف", "ٹیم ورک", "توازن", "کارکردگی"),
                "ar" to listOf("من ناحية أخرى", "مع ذلك", "العمل عن بُعد", "التعاون", "التوازن"),
                "ko" to listOf("반면에", "한편으로", "재택근무", "팀워크", "균형"),
                "zh" to listOf("另一方面", "然而", "远程办公", "团队合作", "平衡"),
                "hi" to listOf("हालाँकि", "दूसरी ओर", "टीमवर्क", "संतुलन", "उत्पादकता"),
                "en" to listOf("However", "On the one hand", "Productivity", "Work-life balance", "Collaboration")
            ),
            level = "Intermediate"
        ),
        DebateTopic(
            id = "debate_city_cars",
            title = "Car-Free City Centers",
            neutralPromptQuestion = "Should historic city centers ban private cars in favor of public transit and bicycles, or do residents and shops still depend on car access?",
            suggestedVocabularyByLanguage = mapOf(
                "es" to listOf("Transporte público", "Peatonal", "Comercio local", "Contaminación", "Accesibilidad"),
                "fr" to listOf("Transports en commun", "Piétonnisation", "Commerçants", "Environnement", "Mobilité"),
                "ja" to listOf("公共交通機関", "歩行者天国", "環境", "地域の店", "利便性"),
                "de" to listOf("Öffentlicher Verkehr", "Fußgängerzone", "Einzelhandel", "Umweltschutz", "Erreichbarkeit"),
                "it" to listOf("Trasporto pubblico", "Zona pedonale", "Commercio", "Inquinamento", "Mobilità"),
                "pt" to listOf("Transporte público", "Zona pedonal", "Comércio local", "Ambiente", "Acessibilidade"),
                "ur" to listOf("عوامی ٹرانسپورٹ", "پیدل چلنے والے", "ماحولیات", "مقامی دکانیں", "سہولت"),
                "ar" to listOf("النقل العام", "منطقة للمشاة", "التلوث", "التجارة المحلية", "سهولة الوصول"),
                "ko" to listOf("대중교통", "보행자 전용", "환경", "지역 상점", "접근성"),
                "zh" to listOf("公共交通", "步行街", "环境保护", "当地商店", "便利性"),
                "hi" to listOf("सार्वजनिक परिवहन", "पैदल क्षेत्र", "प्रदूषण", "स्थानीय बाज़ार", "सुविधा"),
                "en" to listOf("Public transit", "Pedestrian zone", "Local shops", "Pollution", "Accessibility")
            ),
            level = "Intermediate"
        ),
        DebateTopic(
            id = "debate_paper_digital",
            title = "Printed Books vs. Digital Learning",
            neutralPromptQuestion = "Do people remember and enjoy ideas more deeply from printed books, or do digital tools make knowledge far more accessible?",
            suggestedVocabularyByLanguage = mapOf(
                "es" to listOf("Concentración", "Acceso inmediato", "Lectura profunda", "Pantalla", "Memoria"),
                "fr" to listOf("Lecture sur papier", "Numérique", "Accessibilité", "Attention", "Support"),
                "ja" to listOf("紙の本", "電子書籍", "集中力", "便利さ", "記憶"),
                "de" to listOf("Gedruckte Bücher", "Digitalisierung", "Konzentration", "Zugänglichkeit", "Gedächtnis"),
                "it" to listOf("Libri cartacei", "Digitale", "Concentrazione", "Accessibilità", "Memoria"),
                "pt" to listOf("Livros impressos", "Digital", "Concentração", "Acessibilidade", "Memória"),
                "ur" to listOf("مطبوعہ کتابیں", "ڈیجیٹل ذرائع", "توجہ", "رسائی", "یادداشت"),
                "ar" to listOf("الكتب الورقية", "التعلم الرقمي", "التركيز", "سهولة الوصول", "الذاكرة"),
                "ko" to listOf("종이책", "디지털 학습", "집중력", "접근성", "기억력"),
                "zh" to listOf("纸质书", "数字学习", "专注力", "便利性", "记忆"),
                "hi" to listOf("मुद्रित पुस्तकें", "डिजिटल शिक्षा", "एकाग्रता", "पहुँच", "स्मृति"),
                "en" to listOf("Focus", "Instant access", "Deep reading", "Retention", "Accessibility")
            ),
            level = "Beginner-Intermediate"
        )
    )

    fun detectiveCasesForLanguage(languageCode: String): List<ConversationDetectiveCase> {
        return when (languageCode.lowercase()) {
            "es" -> listOf(
                ConversationDetectiveCase(
                    id = "det_es_1",
                    languageCode = "es",
                    title = "The Polite 'Ya veremos' Invitation Response",
                    socialSetting = "You invite a colleague in Madrid or Mexico City to a Sunday hike, and they smile warmly and say:",
                    speakerUtteranceTarget = "«¡Uf, suena genial! Déjame ver cómo ando de tiempo el domingo y te aviso, ¿vale?»",
                    speakerUtteranceRomanization = "Oof, SWEH-nah heh-NYAL! DEH-hah-meh ver KOH-moh AN-doh deh TYEM-poh el doh-MEEN-goh ee teh ah-VEE-soh, VAH-leh?",
                    literalTranslation = "Ooh, sounds great! Let me see how I am for time on Sunday and I'll let you know, okay?",
                    toneAndContextClues = "Warm smile, friendly intonation, no concrete time proposed.",
                    interpretations = listOf(
                        CulturalInterpretationOption(
                            id = "opt_1",
                            label = "A gentle, face-saving 'probably not' that preserves warmth without flatly refusing",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "In many Spanish-speaking contexts, a direct 'No' to a friendly invitation can feel overly blunt. Leaving the door open preserves rapport while signaling low likelihood.",
                            regionalOrSituationalCaveat = "Common across Spain and Latin America when someone wants to stay friendly without committing."
                        ),
                        CulturalInterpretationOption(
                            id = "opt_2",
                            label = "They genuinely have an uncertain Sunday schedule and may text you Saturday night",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "If the colleague is a close friend with family obligations, they may truly need to check their weekend plans first.",
                            regionalOrSituationalCaveat = "Context matters: if they follow up with '¿A qué hora salen?' they are genuinely interested."
                        ),
                        CulturalInterpretationOption(
                            id = "opt_3",
                            label = "They are offended by the invitation and demanding an apology",
                            isPlausibleInterpretation = false,
                            pragmaticNuanceExplanation = "The phrase '¡Suena genial!' and warm diminutive/tag '¿vale?' signal friendliness, not hostility.",
                            regionalOrSituationalCaveat = "Not supported by tone or phrasing."
                        )
                    ),
                    multiPerspectiveNote = "Cultural expressions rarely have just one rigid meaning: depending on closeness, region, and follow-up questions, this can be either a warm soft decline or a genuine tentative check.",
                    politeResponseOptionsTarget = listOf(
                        "¡Claro, sin presión! Si te animas el sábado me escribes.",
                        "¡Perfecto! Nosotros salimos a las nueve por si te viene bien."
                    )
                ),
                ConversationDetectiveCase(
                    id = "det_es_2",
                    languageCode = "es",
                    title = "Offering Food Twice ('¿Seguro que no quieres?')",
                    socialSetting = "Visiting a friend's family home; you politely declined a second serving once, and the host asks again:",
                    speakerUtteranceTarget = "«¿Pero seguro que no quieres probar aunque sea un poquito más? ¡Que sobra mucho!»",
                    speakerUtteranceRomanization = "PEH-roh seh-GOO-roh keh noh KYEH-res proh-BAR own-keh SEH-ah oon poh-KEE-toh mahs? Keh SOH-brah MOO-choh!",
                    literalTranslation = "Are you sure you don't want to try even a tiny bit more? There's plenty left over!",
                    toneAndContextClues = "Affectionate hospitality; offering more is a way of showing care.",
                    interpretations = listOf(
                        CulturalInterpretationOption(
                            id = "opt_es2_1",
                            label = "Warm hospitality ritual where accepting a small portion or praising the dish delights the host",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "Insisting gently is a classic expression of generosity (`confianza`), making sure the guest isn't holding back out of shyness.",
                            regionalOrSituationalCaveat = "Especially common at family meals in Spain and Latin America."
                        ),
                        CulturalInterpretationOption(
                            id = "opt_es2_2",
                            label = "You can still decline warmly if full, as long as you compliment the cooking enthusiastically",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "You are never forced to eat if full or allergic—praising how delicious it was ('Estaba buenísimo, pero estoy llenísimo') honors the host's warmth.",
                            regionalOrSituationalCaveat = "Works well in all regions when paired with genuine appreciation."
                        )
                    ),
                    multiPerspectiveNote = "Both accepting a tiny bit and declining with warm praise for the cook are culturally natural responses.",
                    politeResponseOptionsTarget = listOf(
                        "Bueno, solo un poquito porque está delicioso, ¡muchas gracias!",
                        "Estaba riquísimo de verdad, pero ya no puedo más. ¡Qué buena mano tienes!"
                    )
                )
            )
            "ja" -> listOf(
                ConversationDetectiveCase(
                    id = "det_ja_1",
                    languageCode = "ja",
                    title = "Understanding 'Chotto...' (ちょっと…)",
                    socialSetting = "You ask a coworker in Tokyo if Friday at 6 PM works for a project review meeting, and they tilt their head slightly:",
                    speakerUtteranceTarget = "「金曜日の6時は、ちょっと…」",
                    speakerUtteranceRomanization = "Kin'youbi no roku-ji wa, chotto...",
                    literalTranslation = "Friday at 6 o'clock is a little...",
                    toneAndContextClues = "Trailing off softly at the end of the sentence (`...`) with a slight apologetic nod.",
                    interpretations = listOf(
                        CulturalInterpretationOption(
                            id = "opt_ja1_1",
                            label = "An indirect, considerate way of saying that Friday at 6 PM is inconvenient",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "Leaving the sentence unfinished (`chotto...`) allows the listener to infer the difficulty without forcing a harsh 'No, I can't'.",
                            regionalOrSituationalCaveat = "Standard polite workplace and social communication across Japan."
                        ),
                        CulturalInterpretationOption(
                            id = "opt_ja1_2",
                            label = "They might be open to a different day or earlier time if you offer an alternative",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "Because the refusal targets 'Friday at 6 PM' (`金曜日の6時は`), proposing Monday morning smoothly resolves the situation.",
                            regionalOrSituationalCaveat = "Look for whether they immediately suggest another slot."
                        ),
                        CulturalInterpretationOption(
                            id = "opt_ja1_3",
                            label = "They are asking for a 'little' 5-minute meeting at 6 PM",
                            isPlausibleInterpretation = false,
                            pragmaticNuanceExplanation = "While `chotto` literally means 'a little', trailing off with `wa chotto...` indicates difficulty/inconvenience, not a short meeting duration.",
                            regionalOrSituationalCaveat = "Classic literal-translation trap for learners."
                        )
                    ),
                    multiPerspectiveNote = "Rather than pressing them to explain why Friday evening is hard, offering an alternative time shows high pragmatic sensitivity.",
                    politeResponseOptionsTarget = listOf(
                        "そうですか。では、来週の月曜日の午前中はいかがでしょうか？",
                        "すみません！いつごろがご都合よろしいですか？"
                    )
                )
            )
            else -> listOf(
                ConversationDetectiveCase(
                    id = "det_${languageCode}_1",
                    languageCode = languageCode,
                    title = "Reading Between the Lines in Polite Conversation",
                    socialSetting = "You propose a last-minute change of plans to a local acquaintance, and they reply with hesitation:",
                    speakerUtteranceTarget = "That's an interesting idea... let's see how the afternoon goes first.",
                    speakerUtteranceRomanization = "Polite hedging phrase in ${Language.findByCode(languageCode).name}",
                    literalTranslation = "That's an interesting idea... let's see how the afternoon goes first.",
                    toneAndContextClues = "Measured pace, polite smile, deferring commitment.",
                    interpretations = listOf(
                        CulturalInterpretationOption(
                            id = "opt_gen_1",
                            label = "Polite hedging to avoid a blunt refusal while keeping the conversation harmonious",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "Indirect phrasing cushions disagreement and gives both speakers room to adjust plans gracefully.",
                            regionalOrSituationalCaveat = "Particularly common in professional or newly formed social relationships."
                        ),
                        CulturalInterpretationOption(
                            id = "opt_gen_2",
                            label = "Genuine open-mindedness contingent on how fast the current activity finishes",
                            isPlausibleInterpretation = true,
                            pragmaticNuanceExplanation = "Depending on time constraints, the speaker may truly want to wait before committing.",
                            regionalOrSituationalCaveat = "Check whether they ask follow-up logistics questions."
                        )
                    ),
                    multiPerspectiveNote = "Pragmatic expressions depend on relationship closeness, setting, and tone—multiple interpretations can be valid at once.",
                    politeResponseOptionsTarget = listOf(
                        "No problem at all—we can stick to the original plan if that's easier!",
                        "Sounds good, let's check in around 3 PM and decide together."
                    )
                )
            )
        }
    }

    fun habitMissionsForLanguage(languageCode: String): List<HabitSpeakingMission> {
        val langName = Language.findByCode(languageCode).name
        return when (languageCode.lowercase()) {
            "es" -> listOf(
                HabitSpeakingMission(
                    id = "hab_es_morning",
                    routineSlot = HabitRoutineSlot.MORNING_COFFEE,
                    languageCode = "es",
                    missionTitle = "Morning Coffee Priority Check",
                    speakingPromptTarget = "Di en voz alta qué estás tomando esta mañana y cuáles son tus dos prioridades principales para hoy.",
                    speakingPromptEnglish = "Say out loud what you are drinking this morning and what your top two priorities are for today.",
                    targetKeywords = listOf("hoy", "mañana", "café", "té", "primero", "quiero", "hacer", "trabajo", "estudio")
                ),
                HabitSpeakingMission(
                    id = "hab_es_commute",
                    routineSlot = HabitRoutineSlot.DAILY_COMMUTE,
                    languageCode = "es",
                    missionTitle = "Commute Observation Narration",
                    speakingPromptTarget = "Describe dos cosas que ves a tu alrededor ahora mismo y a dónde te diriges.",
                    speakingPromptEnglish = "Describe two things you see around you right now and where you are heading.",
                    targetKeywords = listOf("veo", "calle", "voy", "camino", "gente", "tiempo", "hacia")
                ),
                HabitSpeakingMission(
                    id = "hab_es_lunch",
                    routineSlot = HabitRoutineSlot.LUNCH_BREAK,
                    languageCode = "es",
                    missionTitle = "Midday Meal & Mood Check-In",
                    speakingPromptTarget = "Cuenta qué vas a almorzar hoy y cómo va tu día hasta el momento.",
                    speakingPromptEnglish = "Share what you're having for lunch today and how your day is going so far.",
                    targetKeywords = listOf("comer", "almuerzo", "día", "bien", "rico", "descanso", "tarde")
                ),
                HabitSpeakingMission(
                    id = "hab_es_evening",
                    routineSlot = HabitRoutineSlot.EVENING_REFLECTION,
                    languageCode = "es",
                    missionTitle = "Evening Wind-Down Gratitude",
                    speakingPromptTarget = "Menciona un logro o momento positivo de hoy y qué planeas hacer mañana.",
                    speakingPromptEnglish = "Mention one positive moment from today and what you plan to do tomorrow.",
                    targetKeywords = listOf("hoy", "mejor", "aprendí", "mañana", "descansar", "contento", "gracias")
                )
            )
            else -> listOf(
                HabitSpeakingMission(
                    id = "hab_${languageCode}_morning",
                    routineSlot = HabitRoutineSlot.MORNING_COFFEE,
                    languageCode = languageCode,
                    missionTitle = "Morning Coffee Intention ($langName)",
                    speakingPromptTarget = "Speak for 30 seconds in $langName about your morning drink and two goals for today.",
                    speakingPromptEnglish = "Say what you are drinking this morning and state two goals for today in $langName.",
                    targetKeywords = listOf("today", "morning", "goal", "work", "study")
                ),
                HabitSpeakingMission(
                    id = "hab_${languageCode}_commute",
                    routineSlot = HabitRoutineSlot.DAILY_COMMUTE,
                    languageCode = languageCode,
                    missionTitle = "On-the-Go Scene Description ($langName)",
                    speakingPromptTarget = "Describe the weather and where you are going right now in $langName.",
                    speakingPromptEnglish = "Describe the weather and where you are heading right now in $langName.",
                    targetKeywords = listOf("weather", "going", "street", "city", "today")
                ),
                HabitSpeakingMission(
                    id = "hab_${languageCode}_lunch",
                    routineSlot = HabitRoutineSlot.LUNCH_BREAK,
                    languageCode = languageCode,
                    missionTitle = "Lunch Break Recap ($langName)",
                    speakingPromptTarget = "Describe what you are eating for lunch and how your morning went in $langName.",
                    speakingPromptEnglish = "Describe your lunch and how your morning went in $langName.",
                    targetKeywords = listOf("lunch", "eat", "morning", "good", "food")
                ),
                HabitSpeakingMission(
                    id = "hab_${languageCode}_evening",
                    routineSlot = HabitRoutineSlot.EVENING_REFLECTION,
                    languageCode = languageCode,
                    missionTitle = "Evening Wind-Down Reflection ($langName)",
                    speakingPromptTarget = "Share one highlight from today and one plan for tomorrow in $langName.",
                    speakingPromptEnglish = "Share one highlight from today and one plan for tomorrow in $langName.",
                    targetKeywords = listOf("today", "tomorrow", "good", "learned", "rest")
                )
            )
        }
    }

    fun teachAiTopics(): List<TeachAiTopicOption> = listOf(
        TeachAiTopicOption(
            id = "teach_favorite_recipe",
            title = "How to Cook Your Favorite Dish",
            description = "Teach the curious AI student step-by-step how to prepare a dish you love.",
            starterQuestionFromAiByLanguage = mapOf(
                "es" to "¡Hola, profe! Quiero aprender a cocinar tu plato favorito. ¿Cuáles son los ingredientes principales y qué hago primero?",
                "fr" to "Bonjour professeur ! J'aimerais apprendre à cuisiner votre plat préféré. Quels sont les ingrédients et par quoi commence-t-on ?",
                "ja" to "先生、こんにちは！あなたの一番好きな料理の作り方を教えてください。まず何を用意すればいいですか？",
                "de" to "Hallo! Ich möchte lernen, wie man dein Lieblingsgericht kocht. Welche Zutaten brauchen wir und was mache ich zuerst?",
                "it" to "Ciao! Vorrei imparare a cucinare il tuo piatto preferito. Quali sono gli ingredienti principali?",
                "pt" to "Olá! Quero aprender a cozinhar o teu prato favorito. Quais são os ingredientes e o que faço primeiro?",
                "ur" to "سلام! میں آپ کا پسندیدہ کھانا بنانا سیکھنا چاہتا ہوں۔ اہم اجزاء کیا ہیں اور میں سب سے پہلے کیا کروں؟",
                "ar" to "مرحباً! أريد أن أتعلم طريقة تحضير طبقك المفضل. ما المكونات الأساسية وما الخطوة الأولى؟",
                "ko" to "선생님, 안녕하세요! 가장 좋아하는 요리 만드는 법을 배우고 싶어요. 재료가 뭐고 먼저 무엇을 하나요?",
                "zh" to "老师你好！我想学做你最喜欢的一道菜。主要食材有哪些，第一步该怎么做？",
                "hi" to "नमस्ते! मैं आपका पसंदीदा व्यंजन बनाना सीखना चाहता हूँ। मुख्य सामग्री क्या है और सबसे पहले क्या करना होगा?"
            )
        ),
        TeachAiTopicOption(
            id = "teach_local_tradition",
            title = "A Local Tradition or Holiday",
            description = "Explain how people celebrate a favorite tradition in your hometown and why it matters.",
            starterQuestionFromAiByLanguage = mapOf(
                "es" to "Tengo mucha curiosidad por las tradiciones de tu ciudad. ¿Qué fiesta o costumbre me vas a explicar hoy y cómo se celebra?",
                "fr" to "Je suis très curieux de connaître vos traditions locales. Quelle fête allez-vous m'expliquer aujourd'hui ?",
                "ja" to "あなたの街の伝統や行事について知りたいです！どんなお祝いなのか説明してくれますか？",
                "de" to "Ich bin sehr neugierig auf die Traditionen in deiner Heimatstadt. Welches Fest erklärst du mir heute?",
                "it" to "Sono molto curioso delle tradizioni della tua città. Quale festa mi racconti oggi?",
                "pt" to "Tenho muita curiosidade sobre as tradições da tua cidade. Que celebração me vais explicar hoje?",
                "ur" to "مجھے آپ کے شہر کی روایات جاننے کا بہت شوق ہے۔ آج آپ مجھے کس تہوار کے بارے میں بتائیں گے؟",
                "ar" to "أنا فضولي جداً للتعرف على تقاليد مدينتك. ما الاحتفال الذي ستشرحه لي اليوم؟",
                "ko" to "고향의 전통이나 명절이 궁금해요! 어떤 날을 어떻게 기념하나요?",
                "zh" to "我很好奇你们家乡的传统节日！今天你想给我介绍哪一个风俗？",
                "hi" to "मैं आपके शहर की परंपराओं के बारे में जानना चाहता हूँ! आज आप कौन-सा त्योहार समझाएँगे?"
            )
        ),
        TeachAiTopicOption(
            id = "teach_hobby_skill",
            title = "How a Hobby, Sport, or Work Skill Works",
            description = "Explain the basics of a hobby, game, or professional skill to a complete beginner.",
            starterQuestionFromAiByLanguage = mapOf(
                "es" to "¡Soy todo oídos! Imagina que no sé nada sobre tu pasatiempo o profesión. ¿Cómo me explicarías lo más básico?",
                "fr" to "Je vous écoute ! Imaginez que je sois totalement débutant dans votre passion. Comment m'expliqueriez-vous les bases ?",
                "ja" to "あなたの趣味や特技について全く知らない初心者に教えるつもりで、基本から説明してもらえますか？",
                "de" to "Ich bin ganz Ohr! Stell dir vor, ich bin kompletter Anfänger in deinem Hobby. Wie erklärst du mir die Grundlagen?",
                "it" to "Sono tutto orecchi! Immagina che io sia un principiante assoluto nel tuo hobby. Da dove iniziamo?",
                "pt" to "Sou todo ouvidos! Imagina que sou totalmente iniciante no teu passatempo. Como me explicas o básico?",
                "ur" to "میں غور سے سن رہا ہوں! فرض کریں میں اس شوق یا ہنر میں بالکل نیا ہوں، آپ مجھے بنیادی باتیں کیسے سمجھائیں گے؟",
                "ar" to "أنا كلي آذان صاغية! تخيل أنني مبتدئ تماماً في هوايتك. كيف تشرح لي الأساسيات؟",
                "ko" to "완전 초보자에게 설명한다고 생각하고 취미나 일의 기본 원리를 알려주시겠어요?",
                "zh" to "假设我对你的爱好或技能完全不了解，你能用简单的话教我最基础的部分吗？",
                "hi" to "मान लीजिए मैं आपके शौक या कौशल में बिल्कुल नया हूँ—आप मुझे इसकी बुनियादी बातें कैसे समझाएँगे?"
            )
        )
    )

    fun repairScenariosForLanguage(languageCode: String): List<ConversationRepairScenario> {
        return when (languageCode.lowercase()) {
            "es" -> listOf(
                ConversationRepairScenario(
                    id = "rep_es_1",
                    languageCode = "es",
                    title = "Fast Train Announcement & Platform Change",
                    strategyType = RepairStrategyType.ASK_TO_REPEAT,
                    breakdownContext = "A station clerk speaks very quickly with local slang about a platform change.",
                    partnerConfusingLineTarget = "«El cercanías de las cuatro y pico ya no sale de la vía dos, lo han pasao al andén del fondo junto a taquilla.»",
                    partnerConfusingLineRomanization = "El ser-kah-NEE-ahs deh lahs KWAH-troh ee PEE-koh yah noh SAH-leh deh lah VEE-ah dohs...",
                    partnerConfusingLineMeaning = "The 4-something commuter train no longer leaves from track 2; they moved it to the back platform by the ticket office.",
                    repairGoalDescription = "Politely ask them to repeat more slowly and confirm which platform number you should walk to.",
                    modelRepairPhrasesTarget = listOf(
                        "Perdone, ¿podría repetirlo un poco más despacio? ¿A qué andén debo ir?",
                        "Disculpe, no entendí bien el cambio de vía. ¿Me confirma qué andén es, por favor?"
                    )
                ),
                ConversationRepairScenario(
                    id = "rep_es_2",
                    languageCode = "es",
                    title = "Paraphrasing an Unknown Word ('Sacacorchos' / 'Adaptador')",
                    strategyType = RepairStrategyType.PARAPHRASE_MEANING,
                    breakdownContext = "You need a power plug adapter at a hotel desk, but you forgot the exact word.",
                    partnerConfusingLineTarget = "«Buenas tardes, recepción. ¿En qué le puedo ayudar exactamente con su habitación?»",
                    partnerConfusingLineRomanization = "BWEH-nahs TAR-des, reh-sep-SYON. En keh leh PWEH-doh ah-yoo-DAR?",
                    partnerConfusingLineMeaning = "Good afternoon, front desk. How can I help you with your room?",
                    repairGoalDescription = "Describe the object's function using simpler words ('the small piece to connect my phone charger to the wall') so they understand you.",
                    modelRepairPhrasesTarget = listOf(
                        "No recuerdo la palabra exacta, pero necesito esa pieza pequeña para conectar mi cargador al enchufe de la pared.",
                        "Busco el objeto que sirve para enchufar un aparato de otro país a la corriente."
                    )
                ),
                ConversationRepairScenario(
                    id = "rep_es_3",
                    languageCode = "es",
                    title = "Recovering from a False Friend ('Embarazada' vs. 'Avergonzado')",
                    strategyType = RepairStrategyType.SELF_CORRECT_SLIP,
                    breakdownContext = "You accidentally said 'Estoy embarazada' when you meant you felt shy/embarrassed speaking in a meeting.",
                    partnerConfusingLineTarget = "«¿¡De verdad!? ¡Qué sorpresa, enhorabuena! No sabía nada.»",
                    partnerConfusingLineRomanization = "Deh ver-DAD!? Keh sor-PREH-sah, en-oh-rah-BWEH-nah!",
                    partnerConfusingLineMeaning = "Really!? What a surprise, congratulations! I had no idea.",
                    repairGoalDescription = "Laugh it off naturally, correct yourself ('Quise decir que me dio vergüenza'), and keep the conversation flowing.",
                    modelRepairPhrasesTarget = listOf(
                        "¡Perdón, me equivoqué de palabra! Quise decir que estaba un poco avergonzado al hablar.",
                        "¡Uy, no! Quería decir que me dio vergüenza, todavía confundo esa palabra en español."
                    )
                )
            )
            "fr" -> listOf(
                ConversationRepairScenario(
                    id = "rep_fr_1",
                    languageCode = "fr",
                    title = "Fast Spoken Delivery Window Instruction",
                    strategyType = RepairStrategyType.ASK_TO_REPEAT,
                    breakdownContext = "A courier calls rapidly to explain where they left your package.",
                    partnerConfusingLineTarget = "«Bonjour, j'ai déposé le colis chez la gardienne au rez-de-chaussée à côté du digicode.»",
                    partnerConfusingLineRomanization = "Bohn-ZHOOR, zhay day-poh-ZAY luh koh-LEE shay lah gar-DYEN oh ray-duh-shoh-SAY...",
                    partnerConfusingLineMeaning = "Hello, I dropped the package with the concierge on the ground floor next to the keypad.",
                    repairGoalDescription = "Ask them politely to repeat more slowly and confirm who has the package.",
                    modelRepairPhrasesTarget = listOf(
                        "Pardon, pourriez-vous répéter un peu plus lentement ? Qui a le colis exactement ?",
                        "Excusez-moi, je n'ai pas tout compris. Vous l'avez laissé au rez-de-chaussée ?"
                    )
                ),
                ConversationRepairScenario(
                    id = "rep_fr_2",
                    languageCode = "fr",
                    title = "Paraphrasing When a Word Escapes You",
                    strategyType = RepairStrategyType.PARAPHRASE_MEANING,
                    breakdownContext = "You need an umbrella or raincoat at a shop as it starts pouring, but forgot the noun.",
                    partnerConfusingLineTarget = "«Bonjour ! Vous cherchez quelque chose de précis aujourd'hui ?»",
                    partnerConfusingLineRomanization = "Bohn-ZHOOR! Voo sher-SHAY kel-kuh shohz duh pray-SEE oh-zhoor-DWEE?",
                    partnerConfusingLineMeaning = "Hello! Are you looking for something specific today?",
                    repairGoalDescription = "Explain what the object does ('something to protect me from the rain') so the clerk understands.",
                    modelRepairPhrasesTarget = listOf(
                        "Le mot m'échappe, mais je cherche quelque chose pour me protéger de la pluie.",
                        "J'ai oublié le nom en français, c'est l'objet qu'on ouvre quand il pleut dehors."
                    )
                )
            )
            "de" -> listOf(
                ConversationRepairScenario(
                    id = "rep_de_1",
                    languageCode = "de",
                    title = "Fast Train Platform Announcement",
                    strategyType = RepairStrategyType.ASK_TO_REPEAT,
                    breakdownContext = "A station attendant speaks rapidly about a sudden platform change.",
                    partnerConfusingLineTarget = "«Der Regionalexpress nach Augsburg fährt heute abweichend von Gleis dreizehn ganz hinten ab.»",
                    partnerConfusingLineRomanization = "Der Re-gyo-NAL-ex-press nakh OWGS-boork fert HOY-teh AB-vy-khent fon glys DRY-tsayn ab.",
                    partnerConfusingLineMeaning = "The regional express to Augsburg departs today as a change from platform 13 at the very back.",
                    repairGoalDescription = "Politely ask them to repeat slowly and confirm which platform number you should go to.",
                    modelRepairPhrasesTarget = listOf(
                        "Entschuldigung, könnten Sie das bitte etwas langsamer wiederholen? Auf welches Gleis muss ich?",
                        "Verzeihung, ich habe die Gleisnummer nicht verstanden. Gleis dreizehn?"
                    )
                )
            )
            "ur" -> listOf(
                ConversationRepairScenario(
                    id = "rep_ur_1",
                    languageCode = "ur",
                    title = "Asking Someone to Speak Slower",
                    strategyType = RepairStrategyType.ASK_TO_REPEAT,
                    breakdownContext = "A local speaker gives fast directions in Urdu with several landmarks.",
                    partnerConfusingLineTarget = "«آگے گول چکر سے دائیں مڑ کر پُرانی کتابوں کی گلی میں چلے جائیں، وہیں اسٹاپ ہے۔»",
                    partnerConfusingLineRomanization = "Aage gol chakkar se daayein mur kar purani kitabon ki gali mein chale jayein, wahin stop hai.",
                    partnerConfusingLineMeaning = "Turn right at the roundabout ahead into the old book lane; the stop is right there.",
                    repairGoalDescription = "Politely ask them to speak a bit slower and confirm whether to turn right or left.",
                    modelRepairPhrasesTarget = listOf(
                        "معاف کیجیے، کیا آپ ذرا آہستہ دوبارہ بتا سکتے ہیں؟ گول چکر سے دائیں مڑنا ہے؟",
                        "براہِ کرم آسان الفاظ میں سمجھا دیں، میں ابھی اردو سیکھ رہا ہوں۔"
                    )
                )
            )
            else -> listOf(
                ConversationRepairScenario(
                    id = "rep_${languageCode}_1",
                    languageCode = languageCode,
                    title = "Asking to Slow Down & Clarify Details",
                    strategyType = RepairStrategyType.ASK_TO_REPEAT,
                    breakdownContext = "Your conversation partner gave rapid directions with several street names.",
                    partnerConfusingLineTarget = "Take the second exit past the tram bridge and turn right by the old pharmacy before the square.",
                    partnerConfusingLineRomanization = "Rapid natural speech in ${Language.findByCode(languageCode).name}",
                    partnerConfusingLineMeaning = "Take the second exit past the tram bridge and turn right by the old pharmacy before the square.",
                    repairGoalDescription = "Politely ask them to slow down in ${Language.findByCode(languageCode).name} and confirm the landmark where you should turn right.",
                    modelRepairPhrasesTarget = listOf(
                        "Ask politely in ${Language.findByCode(languageCode).name} to repeat more slowly.",
                        "Confirm whether to turn right at the pharmacy in ${Language.findByCode(languageCode).name}."
                    )
                ),
                ConversationRepairScenario(
                    id = "rep_${languageCode}_2",
                    languageCode = languageCode,
                    title = "Paraphrasing an Unknown Object",
                    strategyType = RepairStrategyType.PARAPHRASE_MEANING,
                    breakdownContext = "You forgot the exact word for a charger/adapter and need to describe its purpose.",
                    partnerConfusingLineTarget = "How can I help you at the front desk today?",
                    partnerConfusingLineRomanization = "Front desk inquiry in ${Language.findByCode(languageCode).name}",
                    partnerConfusingLineMeaning = "How can I help you at the front desk today?",
                    repairGoalDescription = "Describe what the item is used for in ${Language.findByCode(languageCode).name} using words you already know.",
                    modelRepairPhrasesTarget = listOf(
                        "Explain in ${Language.findByCode(languageCode).name} that you need something to charge your phone.",
                        "Ask in ${Language.findByCode(languageCode).name} for a cable or adapter for the wall outlet."
                    )
                )
            )
        }
    }
}
