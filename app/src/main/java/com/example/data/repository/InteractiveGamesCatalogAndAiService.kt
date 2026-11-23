package com.example.data.repository

import com.example.ai.GeminiTutorService
import com.example.data.model.AiCustomLesson
import com.example.data.model.ReadingQuestion
import com.example.data.model.ReadingStory
import com.example.data.model.VocabFlashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class InteractiveGameType(
  val id: String,
  val title: String,
  val subtitle: String,
  val badgeLabel: String,
  val primaryHex: Long,
  val secondaryHex: Long,
  val xpReward: Int
) {
  PICTURE_WORD_GAME(
    id = "picture_word_game",
    title = "Picture Word Game",
    subtitle = "Match scenes, clothing & objects to vocabulary",
    badgeLabel = "Visual Quiz",
    primaryHex = 0xFF7C4DFF,
    secondaryHex = 0xFF14C8E8,
    xpReward = 35
  ),
  SPELL_BEE(
    id = "spell_bee",
    title = "SpellBee",
    subtitle = "Spell words letter by letter from audio",
    badgeLabel = "Spelling",
    primaryHex = 0xFFFFB300,
    secondaryHex = 0xFFFF6F00,
    xpReward = 35
  ),
  WORDIGO(
    id = "wordigo",
    title = "Wordigo",
    subtitle = "Guess the 5-letter word in 6 tries",
    badgeLabel = "Daily Word",
    primaryHex = 0xFF00C853,
    secondaryHex = 0xFF14C8E8,
    xpReward = 40
  ),
  DAILY_CHALLENGE(
    id = "daily_challenge_hub",
    title = "Daily Challenge",
    subtitle = "5 words, 2 dialogues, 3 spoken lines & mini quiz",
    badgeLabel = "Streak +50 XP",
    primaryHex = 0xFFFF6D00,
    secondaryHex = 0xFFFFD600,
    xpReward = 50
  ),
  WORD_MATCH(
    id = "word_match_game",
    title = "Word Match",
    subtitle = "Pair target words with meanings against the clock",
    badgeLabel = "Speed Match",
    primaryHex = 0xFF14C8E8,
    secondaryHex = 0xFF2979FF,
    xpReward = 30
  ),
  LISTENING_CHALLENGE(
    id = "listening_challenge_game",
    title = "Listening Challenge",
    subtitle = "Hear native phrases and pick the right meaning",
    badgeLabel = "Ear Training",
    primaryHex = 0xFF00BFA5,
    secondaryHex = 0xFF14C8E8,
    xpReward = 35
  ),
  SENTENCE_BUILDER(
    id = "sentence_builder_game",
    title = "Sentence Builder",
    subtitle = "Assemble natural sentences in the right order",
    badgeLabel = "Grammar Flow",
    primaryHex = 0xFFAA00FF,
    secondaryHex = 0xFF651FFF,
    xpReward = 35
  ),
  PRONUNCIATION_CHALLENGE(
    id = "pronunciation_challenge_game",
    title = "Pronunciation Challenge",
    subtitle = "Speak tricky words aloud with instant clarity scoring",
    badgeLabel = "Voice Lab",
    primaryHex = 0xFFFF4081,
    secondaryHex = 0xFF7C4DFF,
    xpReward = 40
  ),
  CONVERSATION_CHALLENGE(
    id = "conversation_challenge_game",
    title = "Conversation Challenge",
    subtitle = "Complete real-life mini dialogues with your tutor",
    badgeLabel = "Live Reply",
    primaryHex = 0xFF00E5FF,
    secondaryHex = 0xFF3D5AFE,
    xpReward = 45
  ),
  VOCABULARY_MEMORY(
    id = "vocabulary_memory_game",
    title = "Vocabulary Memory Game",
    subtitle = "Flip cards to match words with visual icons",
    badgeLabel = "Memory Grid",
    primaryHex = 0xFF64DD17,
    secondaryHex = 0xFF00BFA5,
    xpReward = 35
  )
}

enum class PictureWordMode(val id: String, val label: String, val instruction: String) {
  IDENTIFY_CLOTHING("clothing", "1. Fashion & Clothing", "Select the clothing or accessory shown:"),
  IDENTIFY_OBJECT("objects", "2. School & Objects", "Select the object shown in the scene:"),
  MATCH_IMAGE_VOCABULARY("buildings", "3. Buildings & Places", "Match the place or building to the word:"),
  IDENTIFY_ACTIONS("work_actions", "4. Work & Actions", "Identify the action or profession shown:"),
  SELECT_CORRECT_WORD("people", "5. People & Daily Life", "Select the correct vocabulary word:"),
  FIND_MISSING_WORD("missing_word", "6. Find Missing Word", "Choose the word that completes the scene sentence:"),
  CHOOSE_CORRECT_SENTENCE("correct_sentence", "7. Choose Sentence", "Select the sentence that describes the picture:")
}

enum class VisualIllustrationKind {
  CROP_TOP_PORTRAIT,
  SUMMER_STRIPED_TSHIRT,
  WINTER_PINK_HOODIE,
  BLUE_SNEAKERS_FOOTWEAR,
  PENDANT_NECKLACE_ACCESSORY,
  SUNGLASSES_AND_CAP,
  YELLOW_SWEATER,
  SCHOOL_TROPHY,
  CITY_BUILDINGS,
  BUSINESS_RUNNER_WORK,
  FRIENDS_PEACE_PEOPLE,
  COFFEE_SHOP_SCENE,
  AIRPORT_TERMINAL_SCENE,
  RESTAURANT_DINING_SCENE,
  JOB_INTERVIEW_SCENE
}

data class PictureWordQuestion(
  val id: String,
  val mode: PictureWordMode,
  val categoryTitle: String,
  val promptLabel: String,
  val targetWord: String,
  val targetTranslation: String,
  val phonetic: String,
  val options: List<String>,
  val hintText: String,
  val exampleSentence: String,
  val exampleTranslation: String,
  val explanation: String,
  val illustrationKind: VisualIllustrationKind,
  val drawableResName: String? = null,
  val visualConceptId: String = "",
  val answerOptions: List<ValidatedAnswerOption> = emptyList(),
  val correctOptionId: String = ""
) {
  fun resolvedAnswerOptions(): List<ValidatedAnswerOption> {
    if (answerOptions.size == 4 && correctOptionId.isNotBlank()) return answerOptions
    val distractors = options.filterNot { it.equals(targetWord, ignoreCase = true) }
    return DynamicAiGameEngine.buildValidatedAnswerOptions(
      exerciseId = id,
      verifiedCorrectText = targetWord,
      candidateOptions = options,
      fallbackDistractors = distractors,
      rotationSeed = (id.hashCode() and 0x7FFFFFFF) % 4
    ).first
  }

  fun resolvedCorrectOptionId(): String {
    if (answerOptions.size == 4 && correctOptionId.isNotBlank()) return correctOptionId
    val distractors = options.filterNot { it.equals(targetWord, ignoreCase = true) }
    return DynamicAiGameEngine.buildValidatedAnswerOptions(
      exerciseId = id,
      verifiedCorrectText = targetWord,
      candidateOptions = options,
      fallbackDistractors = distractors,
      rotationSeed = (id.hashCode() and 0x7FFFFFFF) % 4
    ).second
  }
}

data class VocabCategoryItem(
  val id: String,
  val word: String,
  val translation: String,
  val phonetic: String,
  val exampleSentence: String,
  val exampleTranslation: String,
  val listeningPrompt: String,
  val speakingPrompt: String,
  val quizQuestion: String,
  val quizOptions: List<String>,
  val correctOptionIndex: Int,
  val illustrationKind: VisualIllustrationKind
)

data class VocabSubCategory(
  val id: String,
  val codeNumber: String,
  val title: String,
  val subtitle: String,
  val illustrationKind: VisualIllustrationKind,
  val items: List<VocabCategoryItem>
)

data class VocabMainCategory(
  val id: String,
  val numberIndex: Int,
  val title: String,
  val subtitle: String,
  val accentHex: Long,
  val illustrationKind: VisualIllustrationKind,
  val subcategories: List<VocabSubCategory>
)

enum class SpellBeeDifficulty(val label: String, val xpPerWord: Int) {
  BEGINNER("Beginner", 15),
  INTERMEDIATE("Intermediate", 25),
  ADVANCED("Advanced", 35)
}

data class SpellBeeChallengeWord(
  val id: String,
  val word: String,
  val translation: String,
  val phonetic: String,
  val hint: String,
  val exampleSentence: String,
  val difficulty: SpellBeeDifficulty,
  val scrambledPool: List<String>
)

data class WordigoPuzzle(
  val id: String,
  val targetWord: String,
  val translation: String,
  val phonetic: String,
  val definition: String,
  val exampleSentence: String,
  val difficulty: String,
  val isDaily: Boolean
)

data class MispronouncedWordItem(
  val id: String,
  val word: String,
  val phonetic: String,
  val commonMistakeNote: String,
  val tip: String,
  val exampleSentence: String,
  val translation: String,
  val difficulty: String
)

data class SpeechCoachDetailedAnalysis(
  val overallScore: Int,
  val clarityScore: Int,
  val matchedWords: List<String>,
  val missingWords: List<String>,
  val substitutedWords: List<Pair<String, String>>,
  val syllableBreakdown: List<Pair<String, Boolean>>,
  val coachSummary: String,
  val actionableTip: String
)

data class LibraryBookItem(
  val id: String,
  val title: String,
  val author: String,
  val category: String,
  val difficulty: String,
  val estimatedMinutes: Int,
  val coverPrimaryHex: Long,
  val coverSecondaryHex: Long,
  val coverAccentHex: Long,
  val coverIconType: String,
  val isPublicDomainAdaptation: Boolean,
  val summaryTagline: String,
  val pages: List<String>,
  val translatedPages: List<String>,
  val keyTakeaways: List<String>,
  val glossary: Map<String, String>,
  val questions: List<ReadingQuestion>,
  val sourceName: String = "Project Gutenberg",
  val coverImageUrl: String? = null,
  val hasFullText: Boolean = true,
  val hasHumanAudio: Boolean = false,
  val audiobookRef: com.example.data.model.AudiobookBook? = null,
  val externalPreviewUrl: String? = null
)

data class CustomInteractiveLessonData(
  val id: String,
  val title: String,
  val promptRequest: String,
  val languageCode: String,
  val proficiencyLevel: String,
  val topic: String,
  val durationMinutes: Int,
  val interactionMode: String,
  val vocabulary: List<VocabCategoryItem>,
  val exampleSentences: List<Pair<String, String>>,
  val listeningPrompt: String,
  val listeningQuestion: String,
  val listeningOptions: List<String>,
  val listeningAnswerIndex: Int,
  val quizQuestions: List<ReadingQuestion>,
  val starterTutorMessage: String,
  val starterTutorTranslation: String,
  val suggestedUserReplies: List<String>
)

object InteractiveGamesCatalogAndAiService {

  val BOOK_LIBRARY_CATEGORIES = listOf(
    "All Books",
    "Personal Development",
    "Self-Help",
    "Motivation",
    "Business",
    "Fiction",
    "Short Stories",
    "Everyday Language",
    "Beginner Reading",
    "Intermediate Reading",
    "Advanced Reading"
  )

  private data class LocalizedTerm(
    val word: String,
    val phonetic: String,
    val sentence: String
  )

  private val localizedVocabularyMap: Map<String, Map<String, LocalizedTerm>> = mapOf(
    "es" to mapOf(
      "Crop Top" to LocalizedTerm("Top corto", "/top ˈkoɾ.to/", "Ella lleva un top corto azul en verano."),
      "T-shirt" to LocalizedTerm("Camiseta", "/ka.miˈse.ta/", "Compré una camiseta de algodón muy cómoda."),
      "Hoodie" to LocalizedTerm("Sudadera con capucha", "/su.ðaˈðe.ɾa/", "Necesito una sudadera cálida para el invierno."),
      "Sneakers" to LocalizedTerm("Zapatillas deportivas", "/θa.paˈti.ʎas/", "Estas zapatillas azules son ideales para caminar."),
      "Necklace" to LocalizedTerm("Collar", "/koˈʎaɾ/", "El collar tiene una piedra violeta brillante."),
      "Sunglasses" to LocalizedTerm("Gafas de sol", "/ˈɡa.fas ðe sol/", "No olvides tus gafas de sol para la playa."),
      "Sweater" to LocalizedTerm("Suéter", "/ˈswe.teɾ/", "El suéter amarillo es muy suave."),
      "Trophy" to LocalizedTerm("Trofeo", "/tɾoˈfe.o/", "Nuestro equipo ganó el trofeo escolar."),
      "Building" to LocalizedTerm("Edificio", "/e.ðiˈfi.θjo/", "El edificio moderno está en el centro."),
      "Briefcase" to LocalizedTerm("Maletín", "/ma.leˈtin/", "Él corre al trabajo con su maletín."),
      "Friends" to LocalizedTerm("Amigos", "/aˈmi.ɣos/", "Mis amigos sonríen para la foto."),
      "Coffee" to LocalizedTerm("Café", "/kaˈfe/", "Tomamos un café caliente por la mañana."),
      "Airport" to LocalizedTerm("Aeropuerto", "/a.e.ɾoˈpweɾ.to/", "Llegamos temprano al aeropuerto internacional.")
    ),
    "fr" to mapOf(
      "Crop Top" to LocalizedTerm("Haut court", "/o kuʁ/", "Elle porte un haut court léger aujourd'hui."),
      "T-shirt" to LocalizedTerm("T-shirt rayé", "/ti.ʃœʁt/", "Ce t-shirt d'été est très confortable."),
      "Hoodie" to LocalizedTerm("Sweat à capuche", "/swɛt a ka.pyʃ/", "Je mets un sweat à capuche quand il fait froid."),
      "Sneakers" to LocalizedTerm("Baskets", "/bas.kɛt/", "Mes nouvelles baskets bleues sont légères."),
      "Necklace" to LocalizedTerm("Collier", "/kɔ.lje/", "Ce collier élégant complète sa tenue."),
      "Sunglasses" to LocalizedTerm("Lunettes de soleil", "/ly.nɛt də sɔ.lɛj/", "Je porte des lunettes de soleil en été."),
      "Sweater" to LocalizedTerm("Pull-over", "/pyl.ɔ.vɛʁ/", "Ce pull jaune garde bien la chaleur."),
      "Trophy" to LocalizedTerm("Trophée", "/tʁɔ.fe/", "L'école a remporté le grand trophée."),
      "Building" to LocalizedTerm("Immeuble", "/i.mœbl/", "Cet immeuble de bureaux a cinq étages."),
      "Briefcase" to LocalizedTerm("Mallette", "/ma.lɛt/", "Il marche vite vers le bureau avec sa mallette."),
      "Friends" to LocalizedTerm("Amis", "/a.mi/", "Les amis discutent ensemble au parc."),
      "Coffee" to LocalizedTerm("Café", "/ka.fe/", "Je voudrais un café au lait, s'il vous plaît."),
      "Airport" to LocalizedTerm("Aéroport", "/a.e.ʁɔ.pɔʁ/", "Le train va directement à l'aéroport.")
    ),
    "de" to mapOf(
      "Crop Top" to LocalizedTerm("Bauchfreies Top", "/ˈbaʊx.fʁaɪ.əs tɔp/", "Im Sommer trägt sie ein hellblaues Top."),
      "T-shirt" to LocalizedTerm("T-Shirt", "/ˈtiː.ʃœːɐ̯t/", "Das gestreifte T-Shirt passt perfekt."),
      "Hoodie" to LocalizedTerm("Kapuzenpullover", "/kaˈpuː.t͡sn̩.pʊˌloː.vɐ/", "Der rosa Kapuzenpullover ist sehr warm."),
      "Sneakers" to LocalizedTerm("Turnschuhe", "/ˈtʊʁnˌʃuː.ə/", "Ich trage beim Laufen blaue Turnschuhe."),
      "Necklace" to LocalizedTerm("Halskette", "/ˈhalsˌkɛ.tə/", "Die Halskette hat einen schönen Anhänger."),
      "Sunglasses" to LocalizedTerm("Sonnenbrille", "/ˈzɔ.nənˌbʁɪ.lə/", "Wo ist meine Sonnenbrille?"),
      "Sweater" to LocalizedTerm("Pullover", "/pʊˈloː.vɐ/", "Dieser gelbe Pullover ist aus Wolle."),
      "Trophy" to LocalizedTerm("Pokal", "/poˈkaːl/", "Die Schüler haben den goldenen Pokal gewonnen."),
      "Building" to LocalizedTerm("Gebäude", "/ɡəˈbɔɪ.də/", "Das neue Gebäude steht in der Stadtmitte."),
      "Briefcase" to LocalizedTerm("Aktentasche", "/ˈak.tn̩ˌta.ʃə/", "Er eilt mit seiner Aktentasche ins Büro."),
      "Friends" to LocalizedTerm("Freunde", "/ˈfʁɔɪn.də/", "Gute Freunde helfen einander jeden Tag."),
      "Coffee" to LocalizedTerm("Kaffee", "/ˈka.fe/", "Wir trinken zusammen eine Tasse Kaffee."),
      "Airport" to LocalizedTerm("Flughafen", "/ˈfluːkˌhaː.fn̩/", "Wir sind pünktlich am Flughafen angekommen.")
    ),
    "ar" to mapOf(
      "Crop Top" to LocalizedTerm("قميص قصير", "/qamiːsˤ qasˤiːr/", "ترتدي قميصاً قصيراً مريحاً في الصيف."),
      "T-shirt" to LocalizedTerm("قميص قطني", "/qamiːsˤ qutˤniː/", "هذا القميص المخطط مناسب للطقس الدافئ."),
      "Hoodie" to LocalizedTerm("سترة بغطاء رأس", "/sutra/", "أرتدي سترة دافئة في فصل الشتاء."),
      "Sneakers" to LocalizedTerm("حذاء رياضي", "/ħiðaːʔ rijaːdˤiː/", "هذا الحذاء الرياضي الأزرق مريح للمشي."),
      "Necklace" to LocalizedTerm("قلادة", "/qilaːda/", "القلادة ذات الحجر البنفسجي جميلة جداً."),
      "Sweater" to LocalizedTerm("كنزة صوفية", "/kanza sˤuːfijja/", "الكنزة الصفراء تحميني من البرد."),
      "Trophy" to LocalizedTerm("كأس الفوز", "/kaʔs al-fawz/", "حصلت المدرسة على كأس البطولة."),
      "Building" to LocalizedTerm("مبنى", "/mabnaː/", "يقع المبنى الحديث في وسط المدينة."),
      "Friends" to LocalizedTerm("أصدقاء", "/ʔasˤdiqaːʔ/", "يلتقي الأصدقاء في عطلة نهاية الأسبوع.")
    ),
    "ja" to mapOf(
      "Crop Top" to LocalizedTerm("クロップトップ", "/kuroppu toppu/", "彼女は夏に水色のクロップトップを着ています。"),
      "T-shirt" to LocalizedTerm("Tシャツ", "/tīshatsu/", "この縞模様のTシャツはとても涼しいです。"),
      "Hoodie" to LocalizedTerm("パーカー", "/pākā/", "冬は暖かいピンクのパーカーを着ます。"),
      "Sneakers" to LocalizedTerm("スニーカー", "/sunīkā/", "青いスニーカーで公園を歩きます。"),
      "Necklace" to LocalizedTerm("ネックレス", "/nekkuresu/", "紫の宝石がついたネックレスです。"),
      "Sweater" to LocalizedTerm("セーター", "/sētā/", "黄色いセーターはとても暖かいです。"),
      "Trophy" to LocalizedTerm("トロフィー", "/torofī/", "私たちの学校が金色のトロフィーを獲得しました。"),
      "Building" to LocalizedTerm("建物", "/tatemono/", "駅の近くに新しい建物があります。"),
      "Friends" to LocalizedTerm("友達", "/tomodachi/", "友達と一緒に写真を撮りました。")
    )
  )

  private fun localizeTerm(englishWord: String, defaultPhonetic: String, defaultSentence: String, languageCode: String): LocalizedTerm {
    val code = languageCode.lowercase().take(2)
    if (code == "en") return LocalizedTerm(englishWord, defaultPhonetic, defaultSentence)
    val langMap = localizedVocabularyMap[code]
    return langMap?.get(englishWord) ?: LocalizedTerm(englishWord, defaultPhonetic, defaultSentence)
  }

  private val recentLegacyPictureWords = mutableSetOf<String>()
  private val legacyPictureHistoryLock = Any()

  fun getPictureWordQuestions(
    languageCode: String,
    modeFilter: PictureWordMode? = null
  ): List<PictureWordQuestion> {
    fun fromConcept(
      id: String,
      conceptId: String,
      distractorIds: List<String>,
      mode: PictureWordMode,
      categoryTitle: String,
      slot: Int
    ): PictureWordQuestion? {
      val concept = VerifiedVisualConceptRegistry.getById(conceptId) ?: return null
      val target = concept.wordForLanguage(languageCode)
      val targetCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(target)
      val distractors = distractorIds.mapNotNull { distId ->
        VerifiedVisualConceptRegistry.getById(distId)?.wordForLanguage(languageCode)
      }.filter {
        !it.equals(target, ignoreCase = true) &&
          GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(it) != targetCanon
      }
      val extraPool = VerifiedVisualConceptRegistry.ALL_CONCEPTS
        .filter { it.conceptId != concept.conceptId }
        .map { it.wordForLanguage(languageCode) }
      val (validatedOpts, correctId, _) = DynamicAiGameEngine.buildValidatedAnswerOptions(
        exerciseId = "${id}_${languageCode.lowercase()}",
        verifiedCorrectText = target,
        candidateOptions = listOf(target) + distractors,
        fallbackDistractors = distractors + extraPool,
        rotationSeed = (4 - (slot % 4)) % 4
      )
      return PictureWordQuestion(
        id = id,
        mode = mode,
        categoryTitle = categoryTitle,
        promptLabel = concept.definitionQuestionEn,
        targetWord = target,
        targetTranslation = concept.englishWord,
        phonetic = concept.phoneticForLanguage(languageCode),
        options = validatedOpts.map { it.text },
        hintText = concept.definitionQuestionEn,
        exampleSentence = concept.sentenceForLanguage(languageCode),
        exampleTranslation = concept.exampleSentenceEn,
        explanation = "\"$target\" (${concept.englishWord}) matches: ${concept.definitionQuestionEn}",
        illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE,
        visualConceptId = concept.conceptId,
        answerOptions = validatedOpts,
        correctOptionId = correctId
      )
    }

    val core = listOfNotNull(
      fromConcept("pw_home_table", "table", listOf("door", "chair", "window"), PictureWordMode.IDENTIFY_OBJECT, "1. Home • Everyday Objects", 0),
      fromConcept("pw_home_lamp", "lamp", listOf("clock", "bed", "key"), PictureWordMode.IDENTIFY_OBJECT, "1. Home • Everyday Objects", 1),
      fromConcept("pw_clothing_summer_tshirt", "tshirt", listOf("jacket", "sneakers", "sunglasses"), PictureWordMode.IDENTIFY_CLOTHING, "2. Fashion • Summerwear", 2),
      fromConcept("pw_clothing_winter_jacket", "jacket", listOf("tshirt", "hat", "umbrella"), PictureWordMode.IDENTIFY_CLOTHING, "2. Fashion • Outerwear", 3),
      fromConcept("pw_clothing_footwear_sneakers", "sneakers", listOf("watch", "jacket", "tshirt"), PictureWordMode.IDENTIFY_CLOTHING, "2. Fashion • Footwear", 0),
      fromConcept("pw_clothing_accessory_sunglasses", "sunglasses", listOf("watch", "hat", "backpack"), PictureWordMode.IDENTIFY_CLOTHING, "2. Fashion • Accessories", 1),
      fromConcept("pw_object_school_trophy", "trophy", listOf("book", "laptop", "backpack"), PictureWordMode.SELECT_CORRECT_WORD, "3. Achievements & Study", 2),
      fromConcept("pw_building_city", "building", listOf("restaurant", "airplane", "train"), PictureWordMode.MATCH_IMAGE_VOCABULARY, "4. Buildings • Urban Places", 3),
      fromConcept("pw_work_laptop", "laptop", listOf("phone", "camera", "clock"), PictureWordMode.IDENTIFY_ACTIONS, "5. Work & Technology", 0),
      fromConcept("pw_missing_word_umbrella", "umbrella", listOf("sunglasses", "sneakers", "key"), PictureWordMode.FIND_MISSING_WORD, "6. Weather Essentials", 1),
      fromConcept("pw_sentence_coffee_shop", "coffee", listOf("juice", "soup", "salad"), PictureWordMode.CHOOSE_CORRECT_SENTENCE, "7. Café & Drinks", 2)
    )

    val usedCoreIds = core.map { it.visualConceptId }.toSet()
    val modeCycle = PictureWordMode.entries
    val expanded = VerifiedVisualConceptRegistry.ALL_CONCEPTS
      .filter { it.conceptId !in usedCoreIds && VerifiedVisualConceptRegistry.validateImageUrlMatchesConcept(it) }
      .take(28)
      .mapIndexedNotNull { idx, concept ->
        val distIds = VerifiedVisualConceptRegistry.ALL_CONCEPTS
          .filter { it.conceptId != concept.conceptId && it.domainIndex == concept.domainIndex }
          .plus(VerifiedVisualConceptRegistry.ALL_CONCEPTS.filter { it.conceptId != concept.conceptId })
          .distinctBy { it.conceptId }
          .take(4)
          .map { it.conceptId }
        val mode = when (concept.domainIndex) {
          1 -> PictureWordMode.IDENTIFY_CLOTHING
          else -> modeCycle[(idx + 2) % modeCycle.size]
        }
        fromConcept(
          id = "pw_ext_${concept.conceptId}",
          conceptId = concept.conceptId,
          distractorIds = distIds,
          mode = mode,
          categoryTitle = concept.domainLabel,
          slot = idx + 3
        )
      }

    val all = core + expanded
    return if (modeFilter == null) all else all.filter { it.mode == modeFilter }.ifEmpty { all }
  }

  suspend fun generateAiPictureWordBatch(
    languageName: String,
    languageCode: String,
    cefrLevel: String,
    categoryFocus: String,
    excludedConcepts: Set<String> = emptySet()
  ): List<PictureWordQuestion> = withContext(Dispatchers.IO) {
    val recentSnapshot = synchronized(legacyPictureHistoryLock) {
      recentLegacyPictureWords.toSet()
    }
    val expandedExcluded = GameCurriculumAndAntiRepetitionEngine.expandExcludedWithSemanticKeys(
      excludedConcepts + recentSnapshot
    )
    val avoidListText = (excludedConcepts + recentSnapshot)
      .filter { !it.startsWith("sem:") && !it.startsWith("qsig:") }
      .distinct()
      .take(45)
      .joinToString(", ")

    val prompt = """
      Create 4 fresh, accurate visual vocabulary quiz questions for a learner studying $languageName (code: $languageCode, CEFR level: $cefrLevel) focused on "$categoryFocus".
      Choose concrete, easily photographed objects, foods, animals, places, or tools matching "$categoryFocus".
      Do NOT repeat or paraphrase any of these previously used concepts: [$avoidListText].
      NEVER include the answer word or its translation inside "promptLabel".
      Return ONLY a valid JSON array of 4 objects with keys:
      - "englishConcept": concise English noun for the visual object (e.g. "Croissant", "Telescope", "Bridge")
      - "imageCategory": short category hint (e.g. "food", "science", "architecture")
      - "visualConceptId": optional snake_case id
      - "promptLabel": clear question asking to identify that exact object/concept (without giving away the answer)
      - "targetWord": exact word in $languageName matching englishConcept
      - "targetTranslation": clear English meaning matching englishConcept
      - "phonetic": pronunciation guide
      - "distractors": array of 3 wrong, plausible vocabulary strings in $languageName (all 3 must be distinct and NOT synonyms of targetWord)
      - "hintText": short helpful clue in English
      - "exampleSentence": natural example sentence in $languageName using targetWord
      - "exampleTranslation": English translation of the example sentence
      - "explanation": 1-sentence explanation of why targetWord is correct
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(prompt)
    val parsed = mutableListOf<PictureWordQuestion>()
    val batchSemanticKeys = mutableSetOf<String>()
    val batchPromptSigs = mutableSetOf<String>()

    if (!raw.isNullOrBlank()) {
      try {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val arr = JSONArray(clean)
        for (i in 0 until arr.length()) {
          if (parsed.size >= 4) break
          val obj = arr.optJSONObject(i) ?: continue
          val rawConceptId = obj.optString("visualConceptId").trim().lowercase()
          val englishConcept = obj.optString("englishConcept").trim()
          val imageCategory = obj.optString("imageCategory", categoryFocus).trim()
          val targetWord = obj.optString("targetWord").trim()
          val targetTrans = obj.optString("targetTranslation", englishConcept.ifBlank { targetWord }).trim()
          if (targetWord.isBlank()) continue

          val resolvedEnglish = englishConcept.ifBlank { targetTrans }
          val promptLabel = obj.optString("promptLabel").trim()
          val isDupHistory = GameCurriculumAndAntiRepetitionEngine.isDuplicateContent(
            targetPhrase = targetWord,
            englishMeaning = resolvedEnglish,
            conceptId = rawConceptId,
            prompt = promptLabel,
            excludedSet = expandedExcluded
          )
          val canonTarget = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(targetWord)
          val canonEng = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(resolvedEnglish)
          val promptSig = GameCurriculumAndAntiRepetitionEngine.promptMeaningSignature(promptLabel)
          if (isDupHistory ||
            (canonTarget.isNotBlank() && canonTarget in batchSemanticKeys) ||
            (canonEng.isNotBlank() && canonEng in batchSemanticKeys) ||
            (promptSig.isNotBlank() && promptSig in batchPromptSigs)
          ) {
            continue
          }

          val curatedMatch = VerifiedVisualConceptRegistry.getById(rawConceptId)
            ?.takeIf { VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(it.conceptId, targetWord, resolvedEnglish) }
            ?: VerifiedVisualConceptRegistry.findMatchingConcept(resolvedEnglish, targetWord, targetTrans)
              ?.takeIf { VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(it.conceptId, targetWord, resolvedEnglish) }

          val resolvedConcept = if (curatedMatch != null) {
            curatedMatch
          } else {
            val verifiedUrl = VerifiedVisualConceptRegistry.resolveAndVerifyDynamicImageUrl(
              englishConcept = resolvedEnglish,
              categoryHint = imageCategory
            )
            val dynSlug = "dyn:${resolvedEnglish.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}_${System.currentTimeMillis() % 10000}"
            VerifiedVisualConceptRegistry.registerDynamicConcept(
              conceptId = dynSlug,
              englishWord = resolvedEnglish,
              targetWord = targetWord,
              languageCode = languageCode,
              domainLabel = categoryFocus,
              definitionQuestionEn = promptLabel.ifBlank { "Which $languageName word matches this picture?" },
              exampleSentence = obj.optString("exampleSentence", targetWord),
              imageUrl = verifiedUrl,
              iconKey = imageCategory,
              levelBand = when (cefrLevel.uppercase()) {
                "A1" -> 1
                "A2" -> 2
                "B1" -> 3
                "B2" -> 4
                else -> 5
              }
            )
          }

          val finalPrompt = promptLabel
            .takeIf { it.isNotBlank() && !it.contains(targetWord, ignoreCase = true) }
            ?: resolvedConcept.definitionQuestionEn
              .takeIf { !it.contains(targetWord, ignoreCase = true) }
            ?: "Which word in $languageName matches this picture ($resolvedEnglish)?"

          val distractors = mutableListOf<String>()
          val distArr = obj.optJSONArray("distractors") ?: obj.optJSONArray("options")
          if (distArr != null) {
            for (j in 0 until distArr.length()) {
              val item = distArr.optString(j).trim()
              val dCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(item)
              if (item.isNotBlank() &&
                !item.equals(targetWord, ignoreCase = true) &&
                dCanon != canonTarget &&
                dCanon != canonEng
              ) {
                distractors.add(item)
              }
            }
          }
          val fallbackDistractors = VerifiedVisualConceptRegistry.ALL_CONCEPTS
            .filter {
              val w = it.wordForLanguage(languageCode)
              val wCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(w)
              !w.equals(targetWord, ignoreCase = true) && wCanon != canonTarget && wCanon != canonEng
            }
            .map { it.wordForLanguage(languageCode) }

          val qId = "ai_pw_${resolvedConcept.conceptId}_$i"
          val (validatedOpts, correctId, _) = DynamicAiGameEngine.buildValidatedAnswerOptions(
            exerciseId = qId,
            verifiedCorrectText = targetWord,
            candidateOptions = listOf(targetWord) + distractors,
            fallbackDistractors = distractors + fallbackDistractors,
            rotationSeed = i % 4
          )

          parsed.add(
            PictureWordQuestion(
              id = qId,
              mode = PictureWordMode.SELECT_CORRECT_WORD,
              categoryTitle = categoryFocus,
              promptLabel = finalPrompt,
              targetWord = targetWord,
              targetTranslation = targetTrans,
              phonetic = obj.optString("phonetic", ""),
              options = validatedOpts.map { it.text },
              hintText = obj.optString("hintText", resolvedConcept.definitionQuestionEn),
              exampleSentence = obj.optString("exampleSentence", targetWord),
              exampleTranslation = obj.optString("exampleTranslation", ""),
              explanation = obj.optString("explanation", "'$targetWord' means $targetTrans."),
              illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE,
              visualConceptId = resolvedConcept.conceptId,
              answerOptions = validatedOpts,
              correctOptionId = correctId
            )
          )
          if (canonTarget.isNotBlank()) batchSemanticKeys.add(canonTarget)
          if (canonEng.isNotBlank()) batchSemanticKeys.add(canonEng)
          if (promptSig.isNotBlank()) batchPromptSigs.add(promptSig)
        }
      } catch (_: Exception) {}
    }

    val finalBatch = if (parsed.isNotEmpty()) {
      parsed
    } else {
      val curatedPool = getPictureWordQuestions(languageCode)
      val nonRepeated = curatedPool.filterNot { q ->
        GameCurriculumAndAntiRepetitionEngine.isDuplicateContent(
          targetPhrase = q.targetWord,
          englishMeaning = q.targetTranslation,
          conceptId = q.visualConceptId,
          prompt = q.promptLabel,
          excludedSet = expandedExcluded
        )
      }
      if (nonRepeated.isNotEmpty()) nonRepeated.take(4) else curatedPool.take(4)
    }

    synchronized(legacyPictureHistoryLock) {
      finalBatch.forEach { q ->
        recentLegacyPictureWords.add(q.targetWord.lowercase())
        recentLegacyPictureWords.add(q.targetTranslation.lowercase())
        if (q.visualConceptId.isNotBlank()) recentLegacyPictureWords.add(q.visualConceptId.lowercase())
      }
      if (recentLegacyPictureWords.size > 160) {
        val keep = recentLegacyPictureWords.toList().takeLast(120)
        recentLegacyPictureWords.clear()
        recentLegacyPictureWords.addAll(keep)
      }
    }
    finalBatch
  }

  fun getVocabularyCategories(languageCode: String): List<VocabMainCategory> {
    val cropTop = localizeTerm("Crop Top", "/krɒp tɒp/", "She wears a light blue crop top in summer.", languageCode)
    val tshirt = localizeTerm("T-shirt", "/ˈtiː.ʃɜːt/", "This striped cotton T-shirt feels cool and soft.", languageCode)
    val hoodie = localizeTerm("Hoodie", "/ˈhʊd.i/", "Put on your warm hoodie before going outside.", languageCode)
    val sweater = localizeTerm("Sweater", "/ˈswet.ər/", "My yellow wool sweater is very cozy.", languageCode)
    val sneakers = localizeTerm("Sneakers", "/ˈsniː.kərz/", "I bought comfortable blue sneakers for running.", languageCode)
    val necklace = localizeTerm("Necklace", "/ˈnek.ləs/", "That silver and purple necklace looks elegant.", languageCode)
    val sunglasses = localizeTerm("Sunglasses", "/ˈsʌnˌɡlɑː.sɪz/", "Sunglasses protect your eyes on bright days.", languageCode)
    val trophy = localizeTerm("Trophy", "/ˈtrəʊ.fi/", "Our class won the first-place school trophy.", languageCode)
    val briefcase = localizeTerm("Briefcase", "/ˈbriːf.keɪs/", "She carries her laptop inside a leather briefcase.", languageCode)
    val building = localizeTerm("Building", "/ˈbɪl.dɪŋ/", "The library is inside the tall blue building.", languageCode)
    val friends = localizeTerm("Friends", "/frendz/", "Good friends support each other every day.", languageCode)
    val coffee = localizeTerm("Coffee", "/ˈkɒf.i/", "Let's meet for morning coffee at the corner café.", languageCode)
    val airport = localizeTerm("Airport", "/ˈeə.pɔːt/", "Please arrive at the airport two hours before departure.", languageCode)

    return listOf(
      VocabMainCategory(
        id = "cat_fashion",
        numberIndex = 1,
        title = "1. FASHION",
        subtitle = "Summerwear, Winterwear, Footwear, Accessories & Styles",
        accentHex = 0xFF00C853,
        illustrationKind = VisualIllustrationKind.SUMMER_STRIPED_TSHIRT,
        subcategories = listOf(
          VocabSubCategory(
            id = "sub_1_1_summerwear",
            codeNumber = "1.1",
            title = "1.1. Summerwear",
            subtitle = "Light tops, T-shirts, shorts & sunny outfits",
            illustrationKind = VisualIllustrationKind.SUMMER_STRIPED_TSHIRT,
            items = listOf(
              VocabCategoryItem(
                id = "v_summer_tshirt",
                word = tshirt.word,
                translation = "Striped T-shirt",
                phonetic = tshirt.phonetic,
                exampleSentence = tshirt.sentence,
                exampleTranslation = "This striped cotton T-shirt feels cool and soft.",
                listeningPrompt = tshirt.word,
                speakingPrompt = tshirt.sentence,
                quizQuestion = "Which item is a casual short-sleeved shirt for warm weather?",
                quizOptions = listOf(tshirt.word, hoodie.word, sneakers.word, briefcase.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.SUMMER_STRIPED_TSHIRT
              ),
              VocabCategoryItem(
                id = "v_summer_croptop",
                word = cropTop.word,
                translation = "Crop Top",
                phonetic = cropTop.phonetic,
                exampleSentence = cropTop.sentence,
                exampleTranslation = "She wears a light blue crop top in summer.",
                listeningPrompt = cropTop.word,
                speakingPrompt = cropTop.sentence,
                quizQuestion = "Select the word for a short summer top:",
                quizOptions = listOf(cropTop.word, sweater.word, necklace.word, building.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.CROP_TOP_PORTRAIT
              )
            )
          ),
          VocabSubCategory(
            id = "sub_1_2_winterwear",
            codeNumber = "1.2",
            title = "1.2. Winterwear",
            subtitle = "Hoodies, sweaters, jackets & cold-weather layers",
            illustrationKind = VisualIllustrationKind.WINTER_PINK_HOODIE,
            items = listOf(
              VocabCategoryItem(
                id = "v_winter_hoodie",
                word = hoodie.word,
                translation = "Hooded Sweatshirt (Hoodie)",
                phonetic = hoodie.phonetic,
                exampleSentence = hoodie.sentence,
                exampleTranslation = "Put on your warm hoodie before going outside.",
                listeningPrompt = hoodie.word,
                speakingPrompt = hoodie.sentence,
                quizQuestion = "Which garment has a warm hood and front pocket?",
                quizOptions = listOf(hoodie.word, sunglasses.word, sneakers.word, cropTop.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.WINTER_PINK_HOODIE
              ),
              VocabCategoryItem(
                id = "v_winter_sweater",
                word = sweater.word,
                translation = "Knitted Sweater",
                phonetic = sweater.phonetic,
                exampleSentence = sweater.sentence,
                exampleTranslation = "My yellow wool sweater is very cozy.",
                listeningPrompt = sweater.word,
                speakingPrompt = sweater.sentence,
                quizQuestion = "Select the warm knitted top worn in autumn and winter:",
                quizOptions = listOf(sweater.word, tshirt.word, necklace.word, trophy.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.YELLOW_SWEATER
              )
            )
          ),
          VocabSubCategory(
            id = "sub_1_3_footwear",
            codeNumber = "1.3",
            title = "1.3. Footwear",
            subtitle = "Sneakers, boots, sandals & everyday shoes",
            illustrationKind = VisualIllustrationKind.BLUE_SNEAKERS_FOOTWEAR,
            items = listOf(
              VocabCategoryItem(
                id = "v_footwear_sneakers",
                word = sneakers.word,
                translation = "Athletic Sneakers",
                phonetic = sneakers.phonetic,
                exampleSentence = sneakers.sentence,
                exampleTranslation = "I bought comfortable blue sneakers for running.",
                listeningPrompt = sneakers.word,
                speakingPrompt = sneakers.sentence,
                quizQuestion = "What do you wear on your feet for walking or running?",
                quizOptions = listOf(sneakers.word, necklace.word, hoodie.word, briefcase.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.BLUE_SNEAKERS_FOOTWEAR
              )
            )
          ),
          VocabSubCategory(
            id = "sub_1_4_accessories",
            codeNumber = "1.4",
            title = "1.4. Accessories",
            subtitle = "Necklaces, sunglasses, hats & watches",
            illustrationKind = VisualIllustrationKind.PENDANT_NECKLACE_ACCESSORY,
            items = listOf(
              VocabCategoryItem(
                id = "v_acc_necklace",
                word = necklace.word,
                translation = "Pendant Necklace",
                phonetic = necklace.phonetic,
                exampleSentence = necklace.sentence,
                exampleTranslation = "That silver and purple necklace looks elegant.",
                listeningPrompt = necklace.word,
                speakingPrompt = necklace.sentence,
                quizQuestion = "Which accessory is worn around the neck?",
                quizOptions = listOf(necklace.word, sneakers.word, sweater.word, building.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.PENDANT_NECKLACE_ACCESSORY
              ),
              VocabCategoryItem(
                id = "v_acc_sunglasses",
                word = sunglasses.word,
                translation = "Sunglasses",
                phonetic = sunglasses.phonetic,
                exampleSentence = sunglasses.sentence,
                exampleTranslation = "Sunglasses protect your eyes on bright days.",
                listeningPrompt = sunglasses.word,
                speakingPrompt = sunglasses.sentence,
                quizQuestion = "Which accessory protects your eyes from sunlight?",
                quizOptions = listOf(sunglasses.word, hoodie.word, trophy.word, briefcase.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.SUNGLASSES_AND_CAP
              )
            )
          ),
          VocabSubCategory(
            id = "sub_1_5_styles",
            codeNumber = "1.5",
            title = "1.5. Clothing Styles",
            subtitle = "Casual, formal, sportswear & seasonal outfits",
            illustrationKind = VisualIllustrationKind.YELLOW_SWEATER,
            items = listOf(
              VocabCategoryItem(
                id = "v_style_sweater",
                word = sweater.word,
                translation = "Smart Casual Sweater",
                phonetic = sweater.phonetic,
                exampleSentence = sweater.sentence,
                exampleTranslation = "My yellow wool sweater is very cozy.",
                listeningPrompt = sweater.word,
                speakingPrompt = sweater.sentence,
                quizQuestion = "Choose the smart-casual knitwear item:",
                quizOptions = listOf(sweater.word, sneakers.word, trophy.word, airport.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.YELLOW_SWEATER
              )
            )
          )
        )
      ),
      VocabMainCategory(
        id = "cat_education_work",
        numberIndex = 2,
        title = "2. EDUCATION & WORK",
        subtitle = "School, University, Office, Professions & Interviews",
        accentHex = 0xFFFFB300,
        illustrationKind = VisualIllustrationKind.SCHOOL_TROPHY,
        subcategories = listOf(
          VocabSubCategory(
            id = "sub_2_1_school",
            codeNumber = "2.1",
            title = "2.1. School & Achievements",
            subtitle = "Classrooms, exams, study tools & awards",
            illustrationKind = VisualIllustrationKind.SCHOOL_TROPHY,
            items = listOf(
              VocabCategoryItem(
                id = "v_school_trophy",
                word = trophy.word,
                translation = "School Trophy",
                phonetic = trophy.phonetic,
                exampleSentence = trophy.sentence,
                exampleTranslation = "Our class won the first-place school trophy.",
                listeningPrompt = trophy.word,
                speakingPrompt = trophy.sentence,
                quizQuestion = "What golden cup is awarded to the winning school team?",
                quizOptions = listOf(trophy.word, briefcase.word, necklace.word, sneakers.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.SCHOOL_TROPHY
              )
            )
          ),
          VocabSubCategory(
            id = "sub_2_2_office_work",
            codeNumber = "2.2",
            title = "2.2. Office, Professions & Job Interviews",
            subtitle = "Meetings, career skills, commuting & workplace expressions",
            illustrationKind = VisualIllustrationKind.BUSINESS_RUNNER_WORK,
            items = listOf(
              VocabCategoryItem(
                id = "v_work_briefcase",
                word = briefcase.word,
                translation = "Business Briefcase",
                phonetic = briefcase.phonetic,
                exampleSentence = briefcase.sentence,
                exampleTranslation = "She carries her laptop inside a leather briefcase.",
                listeningPrompt = briefcase.word,
                speakingPrompt = briefcase.sentence,
                quizQuestion = "What does a professional carry documents in on the way to work?",
                quizOptions = listOf(briefcase.word, trophy.word, hoodie.word, coffee.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.BUSINESS_RUNNER_WORK
              )
            )
          )
        )
      ),
      VocabMainCategory(
        id = "cat_travel_places",
        numberIndex = 3,
        title = "3. TRAVEL & PLACES",
        subtitle = "Buildings, Airport, Hotels, Transportation & Directions",
        accentHex = 0xFF2979FF,
        illustrationKind = VisualIllustrationKind.CITY_BUILDINGS,
        subcategories = listOf(
          VocabSubCategory(
            id = "sub_3_1_buildings",
            codeNumber = "3.1",
            title = "3.1. Buildings & City Places",
            subtitle = "Apartments, offices, museums & landmarks",
            illustrationKind = VisualIllustrationKind.CITY_BUILDINGS,
            items = listOf(
              VocabCategoryItem(
                id = "v_place_building",
                word = building.word,
                translation = "City Building",
                phonetic = building.phonetic,
                exampleSentence = building.sentence,
                exampleTranslation = "The library is inside the tall blue building.",
                listeningPrompt = building.word,
                speakingPrompt = building.sentence,
                quizQuestion = "Which word describes a multi-story structure in a city?",
                quizOptions = listOf(building.word, friends.word, necklace.word, tshirt.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.CITY_BUILDINGS
              )
            )
          ),
          VocabSubCategory(
            id = "sub_3_2_airport_hotels",
            codeNumber = "3.2",
            title = "3.2. Airport, Hotels & Transportation",
            subtitle = "Boarding passes, check-in, trains & asking directions",
            illustrationKind = VisualIllustrationKind.AIRPORT_TERMINAL_SCENE,
            items = listOf(
              VocabCategoryItem(
                id = "v_place_airport",
                word = airport.word,
                translation = "International Airport",
                phonetic = airport.phonetic,
                exampleSentence = airport.sentence,
                exampleTranslation = "Please arrive at the airport two hours before departure.",
                listeningPrompt = airport.word,
                speakingPrompt = airport.sentence,
                quizQuestion = "Where do travelers board commercial flights?",
                quizOptions = listOf(airport.word, coffee.word, sweater.word, trophy.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.AIRPORT_TERMINAL_SCENE
              )
            )
          )
        )
      ),
      VocabMainCategory(
        id = "cat_people_communication",
        numberIndex = 4,
        title = "4. PEOPLE & COMMUNICATION",
        subtitle = "Emotions, Personality, Relationships & Social Conversations",
        accentHex = 0xFFFF4081,
        illustrationKind = VisualIllustrationKind.FRIENDS_PEACE_PEOPLE,
        subcategories = listOf(
          VocabSubCategory(
            id = "sub_4_1_relationships",
            codeNumber = "4.1",
            title = "4.1. Friends, Family & Relationships",
            subtitle = "Close friends, teamwork, emotions & social expressions",
            illustrationKind = VisualIllustrationKind.FRIENDS_PEACE_PEOPLE,
            items = listOf(
              VocabCategoryItem(
                id = "v_people_friends",
                word = friends.word,
                translation = "Friends",
                phonetic = friends.phonetic,
                exampleSentence = friends.sentence,
                exampleTranslation = "Good friends support each other every day.",
                listeningPrompt = friends.word,
                speakingPrompt = friends.sentence,
                quizQuestion = "Which word refers to people you trust and spend happy time with?",
                quizOptions = listOf(friends.word, building.word, briefcase.word, sneakers.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.FRIENDS_PEACE_PEOPLE
              )
            )
          )
        )
      ),
      VocabMainCategory(
        id = "cat_everyday_life",
        numberIndex = 5,
        title = "5. EVERYDAY LIFE",
        subtitle = "Greetings, Family, Home & Daily Routine",
        accentHex = 0xFF14C8E8,
        illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE,
        subcategories = listOf(
          VocabSubCategory(
            id = "sub_5_1_daily_routine",
            codeNumber = "5.1",
            title = "5.1. Greetings, Home & Daily Routine",
            subtitle = "Morning habits, polite greetings & home life",
            illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE,
            items = listOf(
              VocabCategoryItem(
                id = "v_daily_coffee",
                word = coffee.word,
                translation = "Morning Coffee",
                phonetic = coffee.phonetic,
                exampleSentence = coffee.sentence,
                exampleTranslation = "Let's meet for morning coffee at the corner café.",
                listeningPrompt = coffee.word,
                speakingPrompt = coffee.sentence,
                quizQuestion = "Which warm drink do many people enjoy in the morning?",
                quizOptions = listOf(coffee.word, sneakers.word, trophy.word, necklace.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE
              )
            )
          )
        )
      ),
      VocabMainCategory(
        id = "cat_food_shopping",
        numberIndex = 6,
        title = "6. FOOD & SHOPPING",
        subtitle = "Fruits, Vegetables, Restaurants, Grocery & Ordering Food",
        accentHex = 0xFFFF6D00,
        illustrationKind = VisualIllustrationKind.RESTAURANT_DINING_SCENE,
        subcategories = listOf(
          VocabSubCategory(
            id = "sub_6_1_restaurants",
            codeNumber = "6.1",
            title = "6.1. Restaurants, Grocery & Ordering Food",
            subtitle = "Menus, fresh ingredients, table reservations & checkout",
            illustrationKind = VisualIllustrationKind.RESTAURANT_DINING_SCENE,
            items = listOf(
              VocabCategoryItem(
                id = "v_food_order",
                word = coffee.word,
                translation = "Café Beverage & Menu",
                phonetic = coffee.phonetic,
                exampleSentence = coffee.sentence,
                exampleTranslation = "Let's meet for morning coffee at the corner café.",
                listeningPrompt = coffee.word,
                speakingPrompt = coffee.sentence,
                quizQuestion = "Select the item commonly ordered at a café:",
                quizOptions = listOf(coffee.word, briefcase.word, hoodie.word, building.word),
                correctOptionIndex = 0,
                illustrationKind = VisualIllustrationKind.RESTAURANT_DINING_SCENE
              )
            )
          )
        )
      )
    )
  }

  fun getSpellBeeWords(
    languageCode: String,
    difficulty: SpellBeeDifficulty
  ): List<SpellBeeChallengeWord> {
    val code = languageCode.lowercase().take(2)
    val baseWords = when (code) {
      "es" -> listOf(
        Triple("CAMISETA", "T-shirt", "Camiseta de algodón para el verano"),
        Triple("EDIFICIO", "Building", "Un edificio alto en la ciudad"),
        Triple("TROFEO", "Trophy", "Premio ganado en la escuela"),
        Triple("AMIGOS", "Friends", "Personas cercanas con las que compartes"),
        Triple("COLLAR", "Necklace", "Joya que se lleva en el cuello"),
        Triple("AEROPUERTO", "Airport", "Lugar de donde salen los aviones")
      )
      "fr" -> listOf(
        Triple("BASKETS", "Sneakers", "Chaussures de sport confortables"),
        Triple("COLLIER", "Necklace", "Bijou porté autour du cou"),
        Triple("TROPHÉE", "Trophy", "Coupe gagnée lors d'un concours"),
        Triple("IMMEUBLE", "Building", "Grand bâtiment en ville"),
        Triple("MALLETTE", "Briefcase", "Sac rectangulaire pour le travail"),
        Triple("AÉROPORT", "Airport", "Terminal pour prendre l'avion")
      )
      "de" -> listOf(
        Triple("PULLOVER", "Sweater", "Warmer Strickpullover für den Winter"),
        Triple("GEBÄUDE", "Building", "Ein großes Bauwerk in der Stadt"),
        Triple("FREUNDE", "Friends", "Gute Bekannte, denen man vertraut"),
        Triple("POKAL", "Trophy", "Goldener Preis beim Wettbewerb"),
        Triple("KAFFEE", "Coffee", "Beliebtes warmes Getränk am Morgen"),
        Triple("FLUGHAFEN", "Airport", "Ort, an dem Flugzeuge starten")
      )
      else -> when (difficulty) {
        SpellBeeDifficulty.BEGINNER -> listOf(
          Triple("SHIRT", "T-shirt / Casual top", "A comfortable cotton garment for the upper body"),
          Triple("SHOES", "Footwear", "Worn on your feet when walking outside"),
          Triple("SMILE", "Friendly expression", "What happy friends do in a photo"),
          Triple("HOTEL", "Travel accommodation", "A place where travelers stay overnight"),
          Triple("TRAIN", "Rail transport", "Fast transport between city stations"),
          Triple("DRESS", "One-piece garment", "Elegant clothing worn for special occasions")
        )
        SpellBeeDifficulty.INTERMEDIATE -> listOf(
          Triple("SWEATER", "Knitted warm top", "Worn over a shirt in cooler seasons"),
          Triple("SNEAKERS", "Athletic shoes", "Comfortable laced footwear for walking or sports"),
          Triple("NECKLACE", "Pendant jewelry", "An accessory worn around the neck"),
          Triple("BUILDING", "City structure", "A multi-story office or apartment tower"),
          Triple("TROPHY", "Victory cup", "A golden award for winning a school contest"),
          Triple("AIRPORT", "Flight terminal", "Where passengers check in for flights")
        )
        SpellBeeDifficulty.ADVANCED -> listOf(
          Triple("BRIEFCASE", "Professional bag", "Carried by office workers for documents"),
          Triple("SUNGLASSES", "Eye protection", "Tinted lenses worn on bright summer days"),
          Triple("COMFORTABLE", "Relaxed and cozy", "Feeling pleasant and free from stress"),
          Triple("ENTREPRENEUR", "Business founder", "Someone who launches and leads a new venture"),
          Triple("PARTICULARLY", "Especially", "To a higher degree than usual"),
          Triple("CONVERSATION", "Spoken dialogue", "An interactive exchange of ideas between people")
        )
      }
    }

    return baseWords.mapIndexed { idx, (word, meaning, hint) ->
      val upper = word.uppercase()
      val pool = (upper.map { it.toString() } + listOf("A", "E", "I", "O", "R", "S", "T", "L", "N")
        .filter { !upper.contains(it) }
        .take(3))
        .shuffled()
      SpellBeeChallengeWord(
        id = "spell_${code}_${difficulty.name.lowercase()}_$idx",
        word = upper,
        translation = meaning,
        phonetic = "/${word.lowercase()}/",
        hint = hint,
        exampleSentence = "Practice spelling '$word' ($meaning) clearly.",
        difficulty = difficulty,
        scrambledPool = pool
      )
    }
  }

  fun getWordigoPuzzles(languageCode: String): List<WordigoPuzzle> {
    val code = languageCode.lowercase().take(2)
    return when (code) {
      "es" -> listOf(
        WordigoPuzzle("wd_es_1", "AMIGO", "Friend", "/aˈmi.ɣo/", "A close companion you trust.", "Mi mejor amigo vive en Madrid.", "Beginner", true),
        WordigoPuzzle("wd_es_2", "HOTEL", "Hotel", "/oˈtel/", "A place to stay when traveling.", "Reservamos un hotel cerca de la playa.", "Beginner", false),
        WordigoPuzzle("wd_es_3", "LIBRO", "Book", "/ˈli.βɾo/", "Pages bound together for reading.", "Estoy leyendo un libro muy interesante.", "Beginner", false),
        WordigoPuzzle("wd_es_4", "FELIZ", "Happy", "/feˈliθ/", "Feeling joy and satisfaction.", "Estamos muy felices por tu éxito.", "Intermediate", false),
        WordigoPuzzle("wd_es_5", "MUNDO", "World", "/ˈmun.do/", "The earth and all its people.", "Viajar abre tu mente al mundo.", "Intermediate", false)
      )
      "fr" -> listOf(
        WordigoPuzzle("wd_fr_1", "LIVRE", "Book", "/livʁ/", "An object you read to learn stories or ideas.", "Ce livre est passionnant.", "Beginner", true),
        WordigoPuzzle("wd_fr_2", "TRAIN", "Train", "/tʁɛ̃/", "Fast rail vehicle between cities.", "Le train part à huit heures.", "Beginner", false),
        WordigoPuzzle("wd_fr_3", "MONDE", "World", "/mɔ̃d/", "The planet and everyone on it.", "Elle voyage dans le monde entier.", "Intermediate", false),
        WordigoPuzzle("wd_fr_4", "AVION", "Airplane", "/a.vjɔ̃/", "Aircraft for long-distance travel.", "Nous prenons l'avion demain matin.", "Beginner", false)
      )
      "de" -> listOf(
        WordigoPuzzle("wd_de_1", "SCHUH", "Shoe", "/ʃuː/", "Footwear for walking.", "Dieser blaue Schuh passt perfekt.", "Beginner", true),
        WordigoPuzzle("wd_de_2", "STADT", "City", "/ʃtat/", "A large urban settlement.", "Berlin ist eine lebendige Stadt.", "Beginner", false),
        WordigoPuzzle("wd_de_3", "REISE", "Journey / Trip", "/ˈʁaɪ.zə/", "Traveling from one place to another.", "Gute Reise und viel Spaß!", "Intermediate", false),
        WordigoPuzzle("wd_de_4", "BRIEF", "Letter", "/bʁiːf/", "A written message sent to someone.", "Ich habe einen Brief geschrieben.", "Beginner", false)
      )
      else -> listOf(
        WordigoPuzzle("wd_en_1", "SHIRT", "Casual garment for the upper body", "/ʃɜːt/", "A piece of clothing worn on the upper body.", "He wore a clean cotton shirt to the meeting.", "Beginner", true),
        WordigoPuzzle("wd_en_2", "SMILE", "Happy facial expression", "/smaɪl/", "Turning up the corners of the mouth when pleased.", "Her warm smile welcomed everyone in the room.", "Beginner", false),
        WordigoPuzzle("wd_en_3", "SPEAK", "To talk or express aloud", "/spiːk/", "Uttering words to communicate in a language.", "I practice every day so I can speak fluently.", "Beginner", false),
        WordigoPuzzle("wd_en_4", "SMART", "Intelligent or well-dressed", "/smɑːt/", "Showing quick-witted intelligence or neat style.", "Setting daily goals is a smart way to learn.", "Intermediate", false),
        WordigoPuzzle("wd_en_5", "WORLD", "The earth and all countries", "/wɜːld/", "The planet and all human society.", "Language connects people across the whole world.", "Intermediate", false),
        WordigoPuzzle("wd_en_6", "HABIT", "A regular daily routine", "/ˈhæb.ɪt/", "A settled practice repeated consistently.", "Reading ten minutes a day builds a strong habit.", "Advanced", false)
      )
    }
  }

  fun getTopMispronouncedWords(languageCode: String): List<MispronouncedWordItem> {
    val code = languageCode.lowercase().take(2)
    return when (code) {
      "es" -> listOf(
        MispronouncedWordItem(
          id = "mp_es_1",
          word = "Ferrocarril",
          phonetic = "/fe.ro.kaˈril/",
          commonMistakeNote = "Learners often soften the rolled 'rr' sound in both syllables.",
          tip = "Tap the tip of your tongue firmly behind your upper teeth for the double 'rr'.",
          exampleSentence = "El ferrocarril cruza las montañas rápidamente.",
          translation = "Railway / Train line",
          difficulty = "Intermediate"
        ),
        MispronouncedWordItem(
          id = "mp_es_2",
          word = "Aeropuerto",
          phonetic = "/a.e.ɾoˈpweɾ.to/",
          commonMistakeNote = "Skipping the 'a-e' vowel transition at the start.",
          tip = "Pronounce both 'A' and 'E' clearly: A-e-ro-puer-to.",
          exampleSentence = "Nos vemos en la entrada del aeropuerto.",
          translation = "Airport",
          difficulty = "Beginner"
        ),
        MispronouncedWordItem(
          id = "mp_es_3",
          word = "Desafortunadamente",
          phonetic = "/de.sa.foɾ.tuˌna.ðaˈmen.te/",
          commonMistakeNote = "Rushing the middle syllables in long adverbs.",
          tip = "Break it into rhythm blocks: des-a-for-tu-na-da-men-te.",
          exampleSentence = "Desafortunadamente, el vuelo se retrasó diez minutos.",
          translation = "Unfortunately",
          difficulty = "Advanced"
        )
      )
      "fr" -> listOf(
        MispronouncedWordItem(
          id = "mp_fr_1",
          word = "Serrurerie",
          phonetic = "/sɛ.ʁy.ʁə.ʁi/",
          commonMistakeNote = "Multiple French uvular 'r' sounds in a row.",
          tip = "Keep the tongue tip down and let the 'u' vowel stay rounded.",
          exampleSentence = "La serrurerie du quartier est ouverte.",
          translation = "Locksmith shop",
          difficulty = "Advanced"
        ),
        MispronouncedWordItem(
          id = "mp_fr_2",
          word = "Grenouille",
          phonetic = "/ɡʁə.nuj/",
          commonMistakeNote = "Pronouncing the final '-ille' as an 'l' instead of '/j/'.",
          tip = "Glide the end smoothly like 'gre-noo-y'.",
          exampleSentence = "Une petite grenouille verte saute près de l'eau.",
          translation = "Frog",
          difficulty = "Intermediate"
        )
      )
      else -> listOf(
        MispronouncedWordItem(
          id = "mp_en_comfortable",
          word = "Comfortable",
          phonetic = "/ˈkʌm.fə.tə.bəl/ (KUMF-ter-bul)",
          commonMistakeNote = "Often pronounced with 4 full syllables ('com-for-ta-ble') instead of 3 natural beats.",
          tip = "Blend the middle smoothly into 3 beats: KUMF-ter-bul.",
          exampleSentence = "These blue sneakers are very comfortable for walking all day.",
          translation = "Providing physical ease and relaxation",
          difficulty = "Beginner"
        ),
        MispronouncedWordItem(
          id = "mp_en_vegetable",
          word = "Vegetable",
          phonetic = "/ˈvedʒ.tə.bəl/ (VEJ-tuh-bul)",
          commonMistakeNote = "Adding an extra vowel in the middle ('ve-ge-ta-ble').",
          tip = "Drop the second 'e' completely: say VEJ-tuh-bul in 3 beats.",
          exampleSentence = "Fresh vegetable soup is my favorite healthy lunch.",
          translation = "A plant or part of a plant used as food",
          difficulty = "Beginner"
        ),
        MispronouncedWordItem(
          id = "mp_en_particularly",
          word = "Particularly",
          phonetic = "/pəˈtɪk.jə.lə.li/ (per-TIK-yuh-ler-lee)",
          commonMistakeNote = "Tripping over the '-cularly' cluster near the end.",
          tip = "Stress the second syllable 'TIK', then glide gently through 'yuh-ler-lee'.",
          exampleSentence = "I am particularly excited about our speaking practice today.",
          translation = "Especially; more than usual",
          difficulty = "Intermediate"
        ),
        MispronouncedWordItem(
          id = "mp_en_wednesday",
          word = "Wednesday",
          phonetic = "/ˈwenz.deɪ/ (WENZ-day)",
          commonMistakeNote = "Pronouncing the silent 'd' andsecond 'e' ('wed-nes-day').",
          tip = "The 'd' is completely silent! Say WENZ-day in 2 clean beats.",
          exampleSentence = "Our language exchange group meets every Wednesday evening.",
          translation = "The day of the week between Tuesday and Thursday",
          difficulty = "Beginner"
        ),
        MispronouncedWordItem(
          id = "mp_en_entrepreneur",
          word = "Entrepreneur",
          phonetic = "/ˌɒn.trə.prəˈnɜːr/ (on-truh-pruh-NUR)",
          commonMistakeNote = "Misplacing the primary stress or pronouncing 'en-' like 'enter'.",
          tip = "Start with an open 'ON' sound and place the main stress on the final syllable '-NUR'.",
          exampleSentence = "She became a successful tech entrepreneur after university.",
          translation = "A person who sets up a business taking on financial risks",
          difficulty = "Advanced"
        ),
        MispronouncedWordItem(
          id = "mp_en_pronunciation",
          word = "Pronunciation",
          phonetic = "/prəˌnʌn.siˈeɪ.ʃən/ (pruh-NUN-see-AY-shun)",
          commonMistakeNote = "Saying 'pro-NOUN-ciation' with an 'ou' sound in the second syllable.",
          tip = "Notice it is spelled '-NUN-', not '-noun-': pruh-NUN-see-AY-shun.",
          exampleSentence = "Consistent daily practice improves your pronunciation quickly.",
          translation = "The way in which a word or language is spoken",
          difficulty = "Intermediate"
        )
      )
    }
  }

  /**
   * Word-level and syllable/phoneme-level speech comparison for Speech Coach.
   * Detects matched words, missing words, substituted words, and clarity score.
   */
  fun evaluateWordAndPhonemeAccuracy(
    expectedText: String,
    spokenTranscript: String
  ): SpeechCoachDetailedAnalysis {
    val cleanExpected = expectedText
      .replace(Regex("[^\\p{L}\\p{N}\\s']"), " ")
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() }

    val cleanSpoken = spokenTranscript
      .replace(Regex("[^\\p{L}\\p{N}\\s']"), " ")
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() }

    if (cleanExpected.isEmpty() || cleanSpoken.isEmpty()) {
      return SpeechCoachDetailedAnalysis(
        overallScore = 0,
        clarityScore = 0,
        matchedWords = emptyList(),
        missingWords = cleanExpected,
        substitutedWords = emptyList(),
        syllableBreakdown = cleanExpected.map { it to false },
        coachSummary = "We didn't catch your voice clearly. Tap the microphone and speak at a comfortable pace.",
        actionableTip = "Hold your device about 20 cm from your mouth and pronounce each word distinctly."
      )
    }

    val matched = mutableListOf<String>()
    val missing = mutableListOf<String>()
    val substituted = mutableListOf<Pair<String, String>>()
    val breakdown = mutableListOf<Pair<String, Boolean>>()

    val spokenUsed = BooleanArray(cleanSpoken.size)

    for (expIdx in cleanExpected.indices) {
      val expWord = cleanExpected[expIdx]
      val expLower = expWord.lowercase()

      val exactIdx = cleanSpoken.indexOfFirst { !spokenUsed[cleanSpoken.indexOf(it)] && it.lowercase() == expLower }
      if (exactIdx >= 0) {
        spokenUsed[exactIdx] = true
        matched.add(expWord)
        breakdown.add(expWord to true)
        continue
      }

      // Check close phonetic/character similarity
      var bestCandidateIdx = -1
      var bestSimilarity = 0f
      for (sIdx in cleanSpoken.indices) {
        if (spokenUsed[sIdx]) continue
        val sim = computeWordSimilarity(expLower, cleanSpoken[sIdx].lowercase())
        if (sim > bestSimilarity) {
          bestSimilarity = sim
          bestCandidateIdx = sIdx
        }
      }

      if (bestCandidateIdx >= 0 && bestSimilarity >= 0.72f) {
        spokenUsed[bestCandidateIdx] = true
        matched.add(expWord)
        breakdown.add(expWord to true)
      } else if (bestCandidateIdx >= 0 && bestSimilarity >= 0.38f) {
        spokenUsed[bestCandidateIdx] = true
        substituted.add(expWord to cleanSpoken[bestCandidateIdx])
        breakdown.add(expWord to false)
      } else {
        missing.add(expWord)
        breakdown.add(expWord to false)
      }
    }

    val ratio = matched.size.toFloat() / cleanExpected.size.coerceAtLeast(1)
    val partialCredit = (substituted.size * 0.45f) / cleanExpected.size.coerceAtLeast(1)
    val rawScore = ((ratio + partialCredit) * 100f).toInt().coerceIn(18, 100)
    val clarity = (rawScore + if (substituted.isEmpty()) 4 else -4).coerceIn(20, 100)

    val summary = when {
      rawScore >= 90 -> "Excellent pronunciation! Your rhythm and word articulation were crisp and natural."
      rawScore >= 75 -> "Great effort! Most words were clear, with minor syllable adjustments needed."
      rawScore >= 55 -> "Good start! You pronounced ${matched.size} of ${cleanExpected.size} words accurately."
      else -> "Keep practicing! Listen to the tutor voice first, then focus on one word at a time."
    }

    val tip = when {
      substituted.isNotEmpty() -> {
        val (target, heard) = substituted.first()
        "For '$target', we heard '$heard'. Slow down slightly on the stressed syllable of '$target'."
      }
      missing.isNotEmpty() -> {
        "Make sure to pronounce '${missing.first()}' clearly without dropping its ending sound."
      }
      else -> "Try repeating the full sentence once more at natural conversational speed to lock in muscle memory."
    }

    return SpeechCoachDetailedAnalysis(
      overallScore = rawScore,
      clarityScore = clarity,
      matchedWords = matched,
      missingWords = missing,
      substitutedWords = substituted,
      syllableBreakdown = breakdown,
      coachSummary = summary,
      actionableTip = tip
    )
  }

  private fun computeWordSimilarity(a: String, b: String): Float {
    if (a == b) return 1f
    if (a.isEmpty() || b.isEmpty()) return 0f
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
      for (j in 1..b.length) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[i][j] = minOf(
          dp[i - 1][j] + 1,
          dp[i][j - 1] + 1,
          dp[i - 1][j - 1] + cost
        )
      }
    }
    val maxLen = maxOf(a.length, b.length)
    return (1f - dp[a.length][b.length].toFloat() / maxLen).coerceIn(0f, 1f)
  }

  fun getLibraryBooksCatalog(languageCode: String): List<LibraryBookItem> {
    return listOf(
      LibraryBookItem(
        id = "book_think_grow_rich",
        title = "Think and Grow Rich",
        author = "Napoleon Hill",
        category = "Personal Development",
        difficulty = "Intermediate",
        estimatedMinutes = 8,
        coverPrimaryHex = 0xFF145369,
        coverSecondaryHex = 0xFF0B3142,
        coverAccentHex = 0xFFFFD54F,
        coverIconType = "COINS_MINDSET",
        isPublicDomainAdaptation = true,
        summaryTagline = "Classic principles of definite purpose, desire, and persistence.",
        pages = listOf(
          "Every great achievement begins with a definite purpose backed by burning desire. When you know exactly what skill or goal you want to master, your daily habits align naturally around that vision.",
          "Persistence turns temporary obstacles into stepping stones. Learners who practice a new language for fifteen focused minutes every day build lasting fluency through steady, compounding effort.",
          "Surround yourself with encouraging partners and constructive feedback. A clear plan, combined with daily action and belief in your ability to grow, transforms ambition into real-world mastery."
        ),
        translatedPages = listOf(
          "Chapter 1: Definite Purpose — Clarity of intention is the starting point of all achievement.",
          "Chapter 2: Daily Persistence — Consistent small steps overcome any initial difficulty.",
          "Chapter 3: Mastermind Habits — Positive environment and focused practice accelerate growth."
        ),
        keyTakeaways = listOf(
          "Write down a clear, specific daily learning goal.",
          "Replace self-doubt with consistent 15-minute practice habits.",
          "Treat every mistake as feedback that brings you closer to fluency."
        ),
        glossary = mapOf(
          "achievement" to "something accomplished successfully through effort",
          "definite" to "clearly stated and decided; not vague",
          "persistence" to "continuing firmly in a course of action despite difficulty",
          "obstacles" to "challenges that block or slow progress",
          "fluency" to "the ability to express oneself easily and articulately"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "According to this classic text, what is the starting point of every great achievement?",
            options = listOf(
              "A definite purpose backed by strong desire",
              "Waiting for luck without a plan",
              "Avoiding all challenging tasks",
              "Practicing only once a month"
            ),
            correctIndex = 0,
            explanation = "Napoleon Hill emphasizes that a definite purpose and strong desire guide all productive habits."
          ),
          ReadingQuestion(
            question = "How does persistence help language learners?",
            options = listOf(
              "It turns temporary obstacles into steady, compounding progress",
              "It eliminates the need to listen or speak",
              "It replaces vocabulary with numbers",
              "It shortens the alphabet"
            ),
            correctIndex = 0,
            explanation = "Consistent daily effort compounds into lasting mastery."
          )
        )
      ),
      LibraryBookItem(
        id = "book_daily_micro_habits",
        title = "The Power of Micro-Habits",
        author = "LingoSphere Editorial",
        category = "Personal Development",
        difficulty = "Beginner",
        estimatedMinutes = 6,
        coverPrimaryHex = 0xFFF5F3EB,
        coverSecondaryHex = 0xFFE6DEC8,
        coverAccentHex = 0xFF1A1A1A,
        coverIconType = "BRAIN_CIRCUIT",
        isPublicDomainAdaptation = false,
        summaryTagline = "How tiny 1% daily improvements build unstoppable language fluency.",
        pages = listOf(
          "Big goals rarely fail from a lack of motivation; they fail when the daily routine feels too heavy to start. A micro-habit is an action so small that you can complete it even on your busiest day.",
          "Instead of promising to study for three hours on Sunday, read two pages over morning coffee or speak three sentences aloud before dinner. Every completed session casts a vote for your new identity as a multilingual speaker.",
          "When you link a new habit to an existing cue—like reviewing five flashcards right after unlocking your phone—consistency becomes automatic rather than exhausting."
        ),
        translatedPages = listOf(
          "Part 1: Why Small Wins Matter — Tiny actions lower friction and build momentum.",
          "Part 2: Identity-Based Learning — Every practice session reinforces confidence.",
          "Part 3: Habit Stacking — Attach language practice to a daily routine you already do."
        ),
        keyTakeaways = listOf(
          "Start with a habit that takes less than five minutes to begin.",
          "Stack your language practice onto a morning or evening routine.",
          "Focus on never missing twice rather than chasing perfection."
        ),
        glossary = mapOf(
          "motivation" to "enthusiasm or reason for doing something",
          "routine" to "a sequence of actions regularly followed",
          "identity" to "who you believe yourself to be through your actions",
          "consistency" to "steady adherence to the same practice over time"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "What makes a micro-habit effective on busy days?",
            options = listOf(
              "It is small enough to start without friction",
              "It requires four hours of uninterrupted silence",
              "It only happens once a year",
              "It avoids practicing real words"
            ),
            correctIndex = 0,
            explanation = "Small, repeatable actions remove the barrier to starting."
          )
        )
      ),
      LibraryBookItem(
        id = "book_richest_man_babylon",
        title = "The Richest Man in Babylon",
        author = "George S. Clason",
        category = "Self-Help",
        difficulty = "Intermediate",
        estimatedMinutes = 7,
        coverPrimaryHex = 0xFF281159,
        coverSecondaryHex = 0xFF160836,
        coverAccentHex = 0xFFFFCA28,
        coverIconType = "BABYLON_CITY",
        isPublicDomainAdaptation = true,
        summaryTagline = "Timeless parables on wisdom, discipline, and investing in yourself.",
        pages = listOf(
          "In ancient Babylon lived Arkad, a scribe who became renowned for his wisdom and generosity. When younger citizens asked for his secret, he explained that true wealth begins with disciplined habits and lifelong learning.",
          "Just as gold grows when invested wisely, your knowledge and communication skills multiply each time you practice and apply them in real conversations.",
          "Seek counsel from experienced teachers, protect your time from distraction, and invest in your own skill every single day."
        ),
        translatedPages = listOf(
          "Parable 1: The Scribe's Lesson — Discipline and patience build lasting value.",
          "Parable 2: Investing in Skill — Knowledge grows when practiced consistently.",
          "Parable 3: Wise Counsel — Learn from mentors who practice what they teach."
        ),
        keyTakeaways = listOf(
          "Invest a portion of your time every day into improving your skills.",
          "Wisdom comes from applying what you learn in real situations.",
          "Small, disciplined choices compound over months and years."
        ),
        glossary = mapOf(
          "renowned" to "known and admired by many people",
          "generosity" to "the quality of being kind and plentiful in giving",
          "parable" to "a simple story used to illustrate a moral or lesson",
          "counsel" to "advice given formally or thoughtfully"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "What did Arkad teach the citizens of Babylon about building lasting value?",
            options = listOf(
              "Practice disciplined daily habits and invest in your own wisdom",
              "Spend everything immediately without planning",
              "Avoid learning from experienced mentors",
              "Wait for fortune without effort"
            ),
            correctIndex = 0,
            explanation = "Arkad taught that disciplined habits and continuous learning create enduring growth."
          )
        )
      ),
      LibraryBookItem(
        id = "book_art_of_war",
        title = "The Art of War",
        author = "Sun Tzu",
        category = "Self-Help",
        difficulty = "Advanced",
        estimatedMinutes = 8,
        coverPrimaryHex = 0xFF9E1B1B,
        coverSecondaryHex = 0xFF5E0D0D,
        coverAccentHex = 0xFFFFFFFF,
        coverIconType = "HORSE_RIDER",
        isPublicDomainAdaptation = true,
        summaryTagline = "Strategic preparation, focus, and adaptability for life and leadership.",
        pages = listOf(
          "Sun Tzu wrote that preparation and self-awareness decide the outcome long before any challenge begins. Knowing your strengths and recognizing the areas you need to practice gives you calm confidence.",
          "Water shapes its course according to the ground over which it flows. In the same way, a skilled communicator adapts their tone, vocabulary, and listening focus to the person and situation in front of them.",
          "Great results are achieved not through reckless haste, but through clear strategy, patient preparation, and decisive focus."
        ),
        translatedPages = listOf(
          "Section 1: Know Yourself — Self-awareness is the foundation of strategy.",
          "Section 2: Flow Like Water — Adapt your communication to every context.",
          "Section 3: Calm Preparation — Preparation removes anxiety before high-stakes moments."
        ),
        keyTakeaways = listOf(
          "Prepare before interviews, presentations, or exams so you can speak calmly.",
          "Adapt your vocabulary and tone to match your listener.",
          "Review your weak spots honestly so they become your strengths."
        ),
        glossary = mapOf(
          "strategy" to "a careful plan of action designed to achieve a long-term goal",
          "outcome" to "the final result or consequence of an action",
          "adapt" to "adjust smoothly to new conditions",
          "decisive" to "settling an issue clearly and effectively"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "Why does Sun Tzu compare effective strategy to flowing water?",
            options = listOf(
              "Because it adapts smoothly to the environment and situation",
              "Because it never moves forward",
              "Because it ignores preparation",
              "Because it avoids listening"
            ),
            correctIndex = 0,
            explanation = "Water adapts its path to the terrain, just as a skilled speaker adapts to the conversation."
          )
        )
      ),
      LibraryBookItem(
        id = "book_as_a_man_thinketh",
        title = "As a Man Thinketh",
        author = "James Allen",
        category = "Motivation",
        difficulty = "Intermediate",
        estimatedMinutes = 6,
        coverPrimaryHex = 0xFFE65100,
        coverSecondaryHex = 0xFFBF360C,
        coverAccentHex = 0xFFFFF8E1,
        coverIconType = "COMPASS_STAR",
        isPublicDomainAdaptation = true,
        summaryTagline = "How purposeful thoughts shape confidence, speech, and character.",
        pages = listOf(
          "A person's mind may be likened to a garden, which may be intelligently cultivated or allowed to run wild. When you plant constructive, curious thoughts, you harvest confidence and clarity.",
          "Calmness of mind is one of the beautiful jewels of wisdom. It is the result of long and patient effort in self-control and thoughtful reflection.",
          "Speak with encouragement, listen with patience, and let every day's study strengthen your voice."
        ),
        translatedPages = listOf(
          "Chapter 1: The Mind as a Garden — Cultivate curiosity and purposeful focus.",
          "Chapter 2: Calm Confidence — Patience builds poise in speaking and life."
        ),
        keyTakeaways = listOf(
          "Cultivate encouraging self-talk when practicing a new language.",
          "Patience and calm focus improve both listening and speaking."
        ),
        glossary = mapOf(
          "cultivated" to "prepared and nurtured for growth",
          "constructive" to "serving a useful and positive purpose",
          "reflection" to "serious and calm thought"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "What metaphor does James Allen use to describe the human mind?",
            options = listOf(
              "A garden that can be intelligently cultivated",
              "A locked door without a key",
              "A storm at sea",
              "An empty desert"
            ),
            correctIndex = 0,
            explanation = "Allen compares the mind to a garden where intentional thoughts grow into habits."
          )
        )
      ),
      LibraryBookItem(
        id = "book_global_business_conversations",
        title = "Confident Business Conversations",
        author = "LingoSphere Business Press",
        category = "Business",
        difficulty = "Intermediate",
        estimatedMinutes = 7,
        coverPrimaryHex = 0xFF0D47A1,
        coverSecondaryHex = 0xFF072659,
        coverAccentHex = 0xFF00E5FF,
        coverIconType = "BRIEFCASE_GLOBE",
        isPublicDomainAdaptation = false,
        summaryTagline = "Practical frameworks for meetings, negotiations, and interviews.",
        pages = listOf(
          "In international meetings, clarity is always more impressive than overly complicated jargon. Start your point with a clear headline, support it with one concrete example, and invite questions.",
          "When answering an interview question, use the Situation-Action-Result structure: describe the context briefly, explain the action you took, and share the measurable outcome.",
          "Polite transitions such as 'Building on that point' or 'Could we explore an alternative?' show collaborative leadership in any language."
        ),
        translatedPages = listOf(
          "Module 1: Clear Meeting Headlines — Lead with your main takeaway.",
          "Module 2: Interview Storytelling — Situation, Action, and Result.",
          "Module 3: Collaborative Phrases — Diplomatic agreement and questions."
        ),
        keyTakeaways = listOf(
          "Use simple, direct sentences in cross-cultural meetings.",
          "Structure interview answers with Situation, Action, and Result."
        ),
        glossary = mapOf(
          "jargon" to "special words used by a group that are difficult for others to understand",
          "concrete" to "specific and definite rather than abstract",
          "measurable" to "able to be quantified or clearly observed",
          "collaborative" to "working together toward a shared goal"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "What 3-part structure helps deliver clear interview answers?",
            options = listOf(
              "Situation, Action, Result",
              "Greeting, Apology, Goodbye",
              "Question, Silence, Guess",
              "Quote, Poem, Riddle"
            ),
            correctIndex = 0,
            explanation = "Situation-Action-Result keeps professional answers structured and memorable."
          )
        )
      ),
      LibraryBookItem(
        id = "book_aesop_fables",
        title = "Aesop's Timeless Tales",
        author = "Aesop",
        category = "Short Stories",
        difficulty = "Beginner",
        estimatedMinutes = 5,
        coverPrimaryHex = 0xFF1B5E20,
        coverSecondaryHex = 0xFF0D3311,
        coverAccentHex = 0xFFFFD54F,
        coverIconType = "TORTOISE_CROWN",
        isPublicDomainAdaptation = true,
        summaryTagline = "Short, memorable fables for beginner and everyday vocabulary.",
        pages = listOf(
          "A swift hare once laughed at a slow tortoise for walking one careful step at a time. Confident of an easy victory, the hare stopped to nap beside the road during their race.",
          "Meanwhile, the tortoise never stopped moving forward. Step after steady step, he crossed the finish line while the hare was still asleep.",
          "In language learning, just as on the racecourse, steady daily practice always wins over occasional bursts of speed."
        ),
        translatedPages = listOf(
          "Scene 1: The Challenge — The hare boasts of his speed.",
          "Scene 2: The Race — Steady steps outlast overconfidence.",
          "Moral: Slow and steady wins the race."
        ),
        keyTakeaways = listOf(
          "Consistent daily steps beat irregular cramming.",
          "Never underestimate the power of steady progress."
        ),
        glossary = mapOf(
          "swift" to "moving very fast",
          "tortoise" to "a slow-moving land reptile with a dome shell",
          "steady" to "regular, even, and continuous in movement"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "Why did the tortoise win the race?",
            options = listOf(
              "He kept moving forward steadily without stopping",
              "He took a shortcut by train",
              "The race was canceled",
              "He ran faster than the wind"
            ),
            correctIndex = 0,
            explanation = "Uninterrupted, steady progress carried the tortoise across the finish line."
          )
        )
      ),
      LibraryBookItem(
        id = "book_midnight_express_fiction",
        title = "The Lantern on Platform Nine",
        author = "Clara Vance",
        category = "Fiction",
        difficulty = "Intermediate",
        estimatedMinutes = 7,
        coverPrimaryHex = 0xFF311B92,
        coverSecondaryHex = 0xFF1A0E52,
        coverAccentHex = 0xFF14C8E8,
        coverIconType = "TRAIN_LANTERN",
        isPublicDomainAdaptation = false,
        summaryTagline = "An atmospheric mystery aboard an overnight European express train.",
        pages = listOf(
          "Rain tapped softly against the tall glass roof of the central station as Mateo checked his ticket. The midnight express to Vienna was waiting on Platform Nine, its brass lamps glowing through the mist.",
          "In the dining car, a violinist left a leather notebook on the table with a map of three historic cafés and a polite note written in four languages.",
          "By speaking with the conductor and fellow passengers, Mateo pieced together each clue before sunrise."
        ),
        translatedPages = listOf(
          "Chapter 1: Platform Nine — Boarding the overnight express.",
          "Chapter 2: The Multilingual Notebook — Clues in the dining car.",
          "Chapter 3: Sunrise in Vienna — Solving the mystery through conversation."
        ),
        keyTakeaways = listOf(
          "Travel vocabulary comes alive through story context.",
          "Asking polite questions opens doors in every city."
        ),
        glossary = mapOf(
          "platform" to "the raised area beside a railway track where passengers board trains",
          "conductor" to "an official in charge of a train and its passengers",
          "clue" to "a piece of evidence that helps solve a mystery"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "Where did Mateo find the leather notebook?",
            options = listOf(
              "In the dining car of the midnight express",
              "At the airport baggage claim",
              "Inside a shoe shop",
              "On a beach umbrella"
            ),
            correctIndex = 0,
            explanation = "The violinist left the notebook on a table in the train's dining car."
          )
        )
      ),
      LibraryBookItem(
        id = "book_everyday_conversations",
        title = "Everyday Café & Travel Dialogues",
        author = "LingoSphere Studio",
        category = "Everyday Language",
        difficulty = "Beginner",
        estimatedMinutes = 5,
        coverPrimaryHex = 0xFF00695C,
        coverSecondaryHex = 0xFF003D33,
        coverAccentHex = 0xFF64FFDA,
        coverIconType = "CAFE_CUP",
        isPublicDomainAdaptation = false,
        summaryTagline = "Essential real-life phrases for cafés, hotels, shopping, and making friends.",
        pages = listOf(
          "When entering a neighborhood café, a warm greeting sets a friendly tone. Start with 'Good morning! Could I have a medium coffee and a warm pastry, please?'",
          "When asking for directions in a new city, people appreciate a polite opener: 'Excuse me, could you tell me how to reach the central museum from here?'",
          "Practicing these short, natural sentences aloud builds instant confidence for real-world travel."
        ),
        translatedPages = listOf(
          "Dialogue 1: Ordering at a Café",
          "Dialogue 2: Asking for Directions Politely",
          "Dialogue 3: Checking into a Hotel"
        ),
        keyTakeaways = listOf(
          "Start requests with a warm greeting and 'Could I have...'",
          "Use 'Excuse me' before asking a stranger for directions."
        ),
        glossary = mapOf(
          "pastry" to "a baked food made of dough, such as a croissant",
          "opener" to "a polite phrase used to begin a conversation",
          "directions" to "instructions on how to reach a destination"
        ),
        questions = listOf(
          ReadingQuestion(
            question = "What is a natural way to ask for directions politely?",
            options = listOf(
              "Excuse me, could you tell me how to reach the central museum?",
              "Give me the map right now!",
              "Where is building?",
              "Silence"
            ),
            correctIndex = 0,
            explanation = "Starting with 'Excuse me, could you tell me...' is polite and natural."
          )
        )
      )
    )
  }

  fun filterLibraryBooks(
    books: List<LibraryBookItem>,
    selectedCategory: String
  ): List<LibraryBookItem> {
    if (selectedCategory.equals("All", ignoreCase = true) ||
      selectedCategory.equals("All Books", ignoreCase = true)
    ) {
      return books
    }
    return when (selectedCategory) {
      "Beginner Reading" -> books.filter { it.difficulty.equals("Beginner", ignoreCase = true) || it.difficulty.startsWith("A", ignoreCase = true) }
      "Intermediate Reading" -> books.filter { it.difficulty.equals("Intermediate", ignoreCase = true) || it.difficulty.startsWith("B", ignoreCase = true) }
      "Advanced Reading" -> books.filter { it.difficulty.equals("Advanced", ignoreCase = true) || it.difficulty.startsWith("C", ignoreCase = true) }
      "Human Audio", "Audiobooks" -> books.filter { it.hasHumanAudio }
      "Full Text" -> books.filter { it.hasFullText }
      "Classic Literature" -> books.filter {
        it.category.contains("Fiction", ignoreCase = true) ||
          it.category.contains("Romance", ignoreCase = true) ||
          it.category.contains("Stories", ignoreCase = true) ||
          it.category.contains("Classic", ignoreCase = true) ||
          it.category.contains("Adventure", ignoreCase = true)
      }
      "Philosophy" -> books.filter {
        it.category.contains("Philosophy", ignoreCase = true) ||
          it.category.contains("Motivation", ignoreCase = true) ||
          it.category.contains("Psychology", ignoreCase = true)
      }
      else -> books.filter {
        it.category.equals(selectedCategory, ignoreCase = true) ||
          it.category.contains(selectedCategory, ignoreCase = true)
      }
    }.ifEmpty { books }
  }

  private val multiSourcePageCache = java.util.concurrent.ConcurrentHashMap<String, List<LibraryBookItem>>()

  fun convertAudiobookToLibraryItem(book: com.example.data.model.AudiobookBook): LibraryBookItem {
    val primaryHex = book.coverGradientColors.firstOrNull() ?: 0xFF1E293B
    val secondaryHex = book.coverGradientColors.getOrElse(1) { 0xFF0F172A }
    val accentHex = when {
      book.hasHumanNarration -> 0xFFFBBF24L
      book.hasFullTextAvailable -> 0xFF34D399L
      else -> 0xFF38BDF8L
    }
    val initialPages = when {
      book.parsedChapters.isNotEmpty() -> book.parsedChapters.flatMap { ch ->
        if (ch.paragraphs.isNotEmpty()) ch.paragraphs.take(6) else listOf(ch.fullText)
      }.ifEmpty { listOf(book.shortDescription) }
      book.episodes.isNotEmpty() -> book.episodes.map { ep ->
        ep.narrationLines.joinToString("\n\n") { it.targetText }.ifBlank { book.shortDescription }
      }
      else -> listOf(book.shortDescription)
    }
    val glossaryMap = buildMap {
      book.episodes.flatMap { it.narrationLines }.forEach { line ->
        if (line.keyVocabWord.isNotBlank() && line.keyVocabMeaning.isNotBlank()) {
          put(line.keyVocabWord, line.keyVocabMeaning)
        }
      }
      if (isEmpty()) {
        put("classic", "a work of enduring excellence and cultural value")
        put("chapter", "a main division of a book")
        put("narrative", "a spoken or written account of connected events")
      }
    }
    val questionsList = book.episodes.flatMap { it.comprehensionQuestions }.map { q ->
      ReadingQuestion(
        question = q.question,
        options = q.options,
        correctIndex = q.correctIndex,
        explanation = q.explanation
      )
    }.ifEmpty {
      listOf(
        ReadingQuestion(
          question = "Who is the author of '${book.title}'?",
          options = listOf(book.author, "Unknown Narrator", "Modern Anonymous", "Editorial Board"),
          correctIndex = 0,
          explanation = "'${book.title}' was written by ${book.author}."
        )
      )
    }
    val sourceLabel = when {
      book.hasHumanNarration && (book.gutendexId != null || book.hasFullTextAvailable) -> "Gutenberg + LibriVox"
      book.hasHumanNarration -> "LibriVox Audio"
      book.gutendexId != null || book.hasFullTextAvailable -> "Project Gutenberg"
      else -> "Study Summary"
    }
    return LibraryBookItem(
      id = book.id,
      title = book.title,
      author = book.author,
      category = book.primaryCategory.ifBlank { "Classic Literature" },
      difficulty = book.defaultCefrLevel.ifBlank { "B1" },
      estimatedMinutes = book.estimatedTotalMinutes.coerceAtLeast(10),
      coverPrimaryHex = primaryHex,
      coverSecondaryHex = secondaryHex,
      coverAccentHex = accentHex,
      coverIconType = "CLASSIC_BOOK",
      isPublicDomainAdaptation = book.copyright != true,
      summaryTagline = book.shortDescription.take(120),
      pages = initialPages,
      translatedPages = initialPages,
      keyTakeaways = book.keyTakeaways.ifEmpty {
        listOf(
          "Authentic edition of ${book.title} by ${book.author}.",
          "Tap any chapter or paragraph to listen and save vocabulary."
        )
      },
      glossary = glossaryMap,
      questions = questionsList,
      sourceName = sourceLabel,
      coverImageUrl = book.coverArtUrl.takeIf { it.isNotBlank() },
      hasFullText = book.hasFullTextAvailable || book.gutendexId != null || book.parsedChapters.isNotEmpty(),
      hasHumanAudio = book.hasHumanNarration,
      audiobookRef = book
    )
  }

  /**
   * Returns the immediate local + pre-verified classic library (never capped at 9 books).
   * Combines the 9 graded reading books with all 23+ full public-domain classics & audiobooks
   * from [AudiobookCatalogRepository.FAMOUS_FULL_BOOKS].
   */
  fun getExpandedLibraryBooksCatalog(languageCode: String): List<LibraryBookItem> {
    val curatedNine = getLibraryBooksCatalog(languageCode)
    val famousClassics = AudiobookCatalogRepository.FAMOUS_FULL_BOOKS.map { convertAudiobookToLibraryItem(it) }
    val seenKeys = mutableSetOf<String>()
    val merged = mutableListOf<LibraryBookItem>()
    for (item in (curatedNine + famousClassics)) {
      val normKey = "${item.title.lowercase().trim()}::${item.author.lowercase().trim()}"
      if (seenKeys.add(normKey)) {
        merged.add(item)
      }
    }
    return merged
  }

  /**
   * Dynamically fetches and merges real books from:
   * 1. Gutendex (Project Gutenberg JSON API: https://gutendex.com/books/)
   * 2. LibriVox API (https://librivox.org/api/feed/audiobooks)
   * 3. Open Library API (https://openlibrary.org/search.json)
   * 4. Google Books API (https://www.googleapis.com/books/v1/volumes)
   */
  suspend fun fetchMultiSourceBooksPage(
    languageCode: String,
    searchQuery: String = "",
    category: String = "All",
    page: Int = 1
  ): List<LibraryBookItem> = withContext(Dispatchers.IO) {
    val cleanQuery = searchQuery.trim()
    val effectiveTopic = when {
      cleanQuery.isNotBlank() -> cleanQuery
      category.equals("All", ignoreCase = true) || category.equals("All Books", ignoreCase = true) -> ""
      else -> category
    }
    val cacheKey = "${languageCode.lowercase()}_${effectiveTopic.lowercase()}_p$page"
    multiSourcePageCache[cacheKey]?.let { cached ->
      if (cached.isNotEmpty()) return@withContext cached
    }

    val discovered = mutableListOf<LibraryBookItem>()
    val httpClient = okhttp3.OkHttpClient.Builder()
      .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
      .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
      .build()

    // 1. Gutendex / Project Gutenberg API
    try {
      val gutendexRes = GutendexAndLibriVoxService.fetchBooksPage(
        search = cleanQuery.takeIf { it.isNotBlank() },
        languageCode = languageCode.take(2),
        topic = effectiveTopic.takeIf { it.isNotBlank() && cleanQuery.isBlank() }
      )
      gutendexRes.books.forEach { ab ->
        discovered.add(convertAudiobookToLibraryItem(ab))
      }
    } catch (_: Exception) {}

    // 2. LibriVox Audiobook API (https://librivox.org/api/feed/audiobooks)
    try {
      val offset = ((page - 1).coerceAtLeast(0)) * 12
      val lvUrl = buildString {
        append("https://librivox.org/api/feed/audiobooks/?format=json&extended=1&limit=12&offset=$offset")
        if (cleanQuery.isNotBlank()) {
          append("&title=%5E")
          append(java.net.URLEncoder.encode(cleanQuery, "UTF-8"))
        }
      }
      val req = okhttp3.Request.Builder()
        .url(lvUrl)
        .header("User-Agent", "LingoSphereLanguageApp/1.0")
        .get()
        .build()
      httpClient.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) {
          val body = resp.body?.string().orEmpty()
          if (body.trimStart().startsWith("{")) {
            val booksArr = JSONObject(body).optJSONArray("books")
            if (booksArr != null) {
              for (i in 0 until booksArr.length()) {
                val bObj = booksArr.optJSONObject(i) ?: continue
                val lvId = bObj.optString("id", "").trim()
                val title = bObj.optString("title", "").trim()
                if (lvId.isBlank() || title.isBlank()) continue
                val desc = bObj.optString("description", "")
                  .replace(Regex("<[^>]*>"), " ")
                  .replace(Regex("\\s+"), " ")
                  .trim()
                val totalTime = bObj.optString("totaltime", "45:00")
                val urlIarchive = bObj.optString("url_iarchive", "").trim()
                val authorsArr = bObj.optJSONArray("authors")
                val authorName = if (authorsArr != null && authorsArr.length() > 0) {
                  val a0 = authorsArr.optJSONObject(0)
                  "${a0?.optString("first_name", "").orEmpty()} ${a0?.optString("last_name", "").orEmpty()}".trim()
                } else "LibriVox Author"

                val sections = mutableListOf<com.example.data.model.LibriVoxSection>()
                val secArr = bObj.optJSONArray("sections")
                if (secArr != null) {
                  for (s in 0 until secArr.length()) {
                    val sObj = secArr.optJSONObject(s) ?: continue
                    val listenUrl = sObj.optString("listen_url", "").trim()
                    if (listenUrl.startsWith("http", ignoreCase = true)) {
                      sections.add(
                        com.example.data.model.LibriVoxSection(
                          id = sObj.optString("id", "${lvId}_$s"),
                          sectionNumber = s + 1,
                          title = sObj.optString("title", "Chapter ${s + 1}"),
                          listenUrl = listenUrl,
                          durationSeconds = sObj.optString("playtime", "0").toIntOrNull() ?: 0,
                          isDurationVerified = true
                        )
                      )
                    }
                  }
                }
                val edition = com.example.data.model.LibriVoxEdition(
                  id = "lv_$lvId",
                  title = title,
                  authorName = authorName.ifBlank { "Classic Author" },
                  narratorName = "LibriVox Narrator",
                  totalTime = totalTime,
                  archiveUrl = urlIarchive,
                  sections = sections
                )
                val ab = com.example.data.model.AudiobookBook(
                  id = "librivox_$lvId",
                  title = title,
                  author = authorName.ifBlank { "Classic Author" },
                  primaryCategory = "Audiobooks",
                  subCategory = "LibriVox",
                  shortDescription = desc.ifBlank { "Human-narrated public-domain audiobook from LibriVox ($totalTime)." },
                  librivoxEditions = listOf(edition),
                  contentType = com.example.data.model.BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK,
                  hasFullTextAvailable = false
                )
                discovered.add(
                  convertAudiobookToLibraryItem(ab).copy(
                    sourceName = "LibriVox Audio",
                    hasHumanAudio = true,
                    pages = if (sections.isNotEmpty()) {
                      sections.map { sec ->
                        "${sec.title}\n\n${desc.ifBlank { "Human-narrated LibriVox audio recording." }}\n\nAudio Stream: ${sec.listenUrl}"
                      }
                    } else {
                      listOf(desc.ifBlank { "Human-narrated LibriVox audiobook edition of $title." })
                    }
                  )
                )
              }
            }
          }
        }
      }
    } catch (_: Exception) {}

    // 3. Open Library Search API (https://openlibrary.org/search.json)
    try {
      val olQuery = effectiveTopic.ifBlank { "classic literature" }
      val olUrl = "https://openlibrary.org/search.json?q=${java.net.URLEncoder.encode(olQuery, "UTF-8")}&page=$page&limit=10"
      val req = okhttp3.Request.Builder()
        .url(olUrl)
        .header("User-Agent", "LingoSphereLanguageApp/1.0")
        .get()
        .build()
      httpClient.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) {
          val body = resp.body?.string().orEmpty()
          if (body.trimStart().startsWith("{")) {
            val docs = JSONObject(body).optJSONArray("docs")
            if (docs != null) {
              for (i in 0 until docs.length()) {
                val d = docs.optJSONObject(i) ?: continue
                val title = d.optString("title", "").trim()
                if (title.isBlank()) continue
                val key = d.optString("key", "/works/ol_${page}_$i").substringAfterLast("/")
                val authorArr = d.optJSONArray("author_name")
                val author = authorArr?.optString(0, "Open Library Author")?.trim().orEmpty().ifBlank { "Open Library Author" }
                val coverId = d.optInt("cover_i", -1)
                val coverUrl = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-M.jpg" else null
                val firstSentenceObj = d.optJSONArray("first_sentence")
                val firstSentence = firstSentenceObj?.optString(0, "")?.trim().orEmpty()
                val subjArr = d.optJSONArray("subject")
                val subjects = buildList {
                  if (subjArr != null) {
                    for (s in 0 until minOf(4, subjArr.length())) {
                      val sub = subjArr.optString(s, "").trim()
                      if (sub.isNotBlank()) add(sub)
                    }
                  }
                }
                val summary = buildString {
                  if (firstSentence.isNotBlank()) append(firstSentence).append("\n\n")
                  append("Cataloged on Open Library by $author.")
                  if (subjects.isNotEmpty()) append(" Subjects: ${subjects.joinToString(", ")}.")
                }
                discovered.add(
                  LibraryBookItem(
                    id = "openlibrary_$key",
                    title = title,
                    author = author,
                    category = subjects.firstOrNull() ?: "Open Library",
                    difficulty = "B1",
                    estimatedMinutes = 15,
                    coverPrimaryHex = 0xFF1E3A8A,
                    coverSecondaryHex = 0xFF0F172A,
                    coverAccentHex = 0xFF60A5FA,
                    coverIconType = "CLASSIC_BOOK",
                    isPublicDomainAdaptation = true,
                    summaryTagline = summary.take(110),
                    pages = listOf(
                      summary,
                      "Key Themes & Subjects:\n${subjects.joinToString("\n• ", prefix = "• ").ifBlank { "• Literature & Cultural Reading" }}"
                    ),
                    translatedPages = listOf(summary),
                    keyTakeaways = subjects.ifEmpty { listOf("Open Library catalog edition of $title") },
                    glossary = mapOf(
                      "edition" to "a particular form or version of a published text",
                      "catalog" to "a systematic list of books and literary works"
                    ),
                    questions = listOf(
                      ReadingQuestion(
                        question = "Who is the author of '$title'?",
                        options = listOf(author, "Anonymous", "Editorial Staff", "Unknown"),
                        correctIndex = 0,
                        explanation = "'$title' is authored by $author."
                      )
                    ),
                    sourceName = "Open Library",
                    coverImageUrl = coverUrl,
                    hasFullText = firstSentence.isNotBlank(),
                    hasHumanAudio = false,
                    externalPreviewUrl = "https://openlibrary.org/works/$key"
                  )
                )
              }
            }
          }
        }
      }
    } catch (_: Exception) {}

    // 4. Google Books API (https://www.googleapis.com/books/v1/volumes)
    try {
      val gbQuery = effectiveTopic.ifBlank { "language learning literature" }
      val startIndex = ((page - 1).coerceAtLeast(0)) * 10
      val gbUrl = "https://www.googleapis.com/books/v1/volumes?q=${java.net.URLEncoder.encode(gbQuery, "UTF-8")}&startIndex=$startIndex&maxResults=10&langRestrict=${languageCode.take(2)}"
      val req = okhttp3.Request.Builder()
        .url(gbUrl)
        .header("User-Agent", "LingoSphereLanguageApp/1.0")
        .get()
        .build()
      httpClient.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) {
          val body = resp.body?.string().orEmpty()
          if (body.trimStart().startsWith("{")) {
            val items = JSONObject(body).optJSONArray("items")
            if (items != null) {
              for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val gId = item.optString("id", "gb_${page}_$i")
                val vol = item.optJSONObject("volumeInfo") ?: continue
                val title = vol.optString("title", "").trim()
                if (title.isBlank()) continue
                val authorsArr = vol.optJSONArray("authors")
                val author = authorsArr?.optString(0, "Google Books Author")?.trim().orEmpty().ifBlank { "Google Books Author" }
                val desc = vol.optString("description", "").replace(Regex("<[^>]*>"), " ").trim()
                val catsArr = vol.optJSONArray("categories")
                val cat = catsArr?.optString(0, "Books")?.trim().orEmpty().ifBlank { "Books" }
                val imgLinks = vol.optJSONObject("imageLinks")
                val thumb = imgLinks?.optString("thumbnail", "")?.replace("http://", "https://")?.takeIf { it.isNotBlank() }
                val previewLink = vol.optString("previewLink", "").takeIf { it.isNotBlank() }
                val cleanOverview = desc.ifBlank { "Google Books volume '$title' by $author in category $cat." }
                val pagesList = cleanOverview.chunked(650).ifEmpty { listOf(cleanOverview) }
                discovered.add(
                  LibraryBookItem(
                    id = "googlebooks_$gId",
                    title = title,
                    author = author,
                    category = cat,
                    difficulty = "B2",
                    estimatedMinutes = 12,
                    coverPrimaryHex = 0xFF065F46,
                    coverSecondaryHex = 0xFF064E3B,
                    coverAccentHex = 0xFF34D399,
                    coverIconType = "CLASSIC_BOOK",
                    isPublicDomainAdaptation = false,
                    summaryTagline = cleanOverview.take(110),
                    pages = pagesList,
                    translatedPages = pagesList,
                    keyTakeaways = listOf("Category: $cat", "Author: $author"),
                    glossary = mapOf(
                      "synopsis" to "a brief summary or general survey of a book",
                      "volume" to "a book forming part of a work or series"
                    ),
                    questions = listOf(
                      ReadingQuestion(
                        question = "Which category best describes '$title' by $author?",
                        options = listOf(cat, "Culinary Recipes Only", "Automotive Repair", "Unrelated Manual"),
                        correctIndex = 0,
                        explanation = "'$title' is categorized under $cat."
                      )
                    ),
                    sourceName = "Google Books",
                    coverImageUrl = thumb,
                    hasFullText = desc.length > 180,
                    hasHumanAudio = false,
                    externalPreviewUrl = previewLink
                  )
                )
              }
            }
          }
        }
      }
    } catch (_: Exception) {}

    if (discovered.isNotEmpty()) {
      multiSourcePageCache[cacheKey] = discovered
    }
    discovered
  }

  fun filterLibraryBooksByLetterAndSearch(
    books: List<LibraryBookItem>,
    selectedCategory: String,
    selectedLetter: String,
    searchQuery: String
  ): List<LibraryBookItem> {
    val categoryFiltered = filterLibraryBooks(books, selectedCategory)
    val q = searchQuery.trim()
    return categoryFiltered.filter { book ->
      val cleanTitle = book.title.replace(Regex("^(?:the|a|an)\\s+", RegexOption.IGNORE_CASE), "").trim()
      val matchesLetter = selectedLetter == "All" ||
        cleanTitle.firstOrNull()?.uppercaseChar()?.toString() == selectedLetter
      val matchesSearch = q.isBlank() ||
        book.title.contains(q, ignoreCase = true) ||
        book.author.contains(q, ignoreCase = true) ||
        book.category.contains(q, ignoreCase = true) ||
        book.sourceName.contains(q, ignoreCase = true)
      matchesLetter && matchesSearch
    }
  }

  suspend fun generateCustomInteractiveLesson(
    userPrompt: String,
    languageName: String,
    languageCode: String,
    proficiencyLevel: String,
    topic: String,
    durationMinutes: Int,
    interactionMode: String
  ): CustomInteractiveLessonData = withContext(Dispatchers.IO) {
    val cleanGoal = userPrompt.trim().ifBlank { "I want to practice $topic" }
    val aiPrompt = """
      Create a complete interactive language lesson for a learner studying $languageName (code: $languageCode, level: $proficiencyLevel).
      Learner request: "$cleanGoal"
      Topic: "$topic"
      Duration: $durationMinutes minutes
      Mode: $interactionMode

      Return ONLY a valid JSON object with:
      - "title": engaging lesson title
      - "vocabulary": array of 4 objects, each with:
          "word" ($languageName), "translation" (English), "phonetic", "exampleSentence" ($languageName), "exampleTranslation" (English)
      - "listeningPrompt": 1-2 natural spoken sentences in $languageName related to "$cleanGoal"
      - "listeningQuestion": comprehension question in English
      - "listeningOptions": array of 4 choices in English (index 0 is the correct answer)
      - "starterTutorMessage": the tutor's opening line in $languageName to start a real conversation with the learner
      - "starterTutorTranslation": English translation of starterTutorMessage
      - "suggestedUserReplies": array of 3 natural responses in $languageName that the learner can say or type
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(aiPrompt)
    if (!raw.isNullOrBlank()) {
      try {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val title = obj.optString("title", "Custom Lesson: $topic").ifBlank { "Custom Lesson: $topic" }
        val vocabArr = obj.optJSONArray("vocabulary")
        val vocabList = mutableListOf<VocabCategoryItem>()
        val sentencePairs = mutableListOf<Pair<String, String>>()
        if (vocabArr != null) {
          for (i in 0 until vocabArr.length()) {
            val v = vocabArr.optJSONObject(i) ?: continue
            val w = v.optString("word").trim()
            val tr = v.optString("translation").trim()
            val ph = v.optString("phonetic", "/${w.lowercase()}/")
            val ex = v.optString("exampleSentence", w)
            val exTr = v.optString("exampleTranslation", tr)
            if (w.isNotBlank()) {
              sentencePairs.add(ex to exTr)
              vocabList.add(
                VocabCategoryItem(
                  id = "custom_v_$i",
                  word = w,
                  translation = tr.ifBlank { w },
                  phonetic = ph,
                  exampleSentence = ex,
                  exampleTranslation = exTr,
                  listeningPrompt = ex,
                  speakingPrompt = ex,
                  quizQuestion = "What does '$w' mean?",
                  quizOptions = listOf(tr.ifBlank { "Correct meaning" }, "Opposite meaning", "Unrelated place", "Different action"),
                  correctOptionIndex = 0,
                  illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE
                )
              )
            }
          }
        }

        val lOptionsArr = obj.optJSONArray("listeningOptions")
        val lOptions = mutableListOf<String>()
        if (lOptionsArr != null) {
          for (i in 0 until lOptionsArr.length()) {
            val opt = lOptionsArr.optString(i).trim()
            if (opt.isNotBlank()) lOptions.add(opt)
          }
        }
        if (lOptions.size < 2) {
          lOptions.clear()
          lOptions.addAll(
            listOf(
              "A practical conversation about $topic",
              "Canceling a flight due to snow",
              "Buying winter boots at a shoe store",
              "Fixing a computer screen"
            )
          )
        }

        val repliesArr = obj.optJSONArray("suggestedUserReplies")
        val replies = mutableListOf<String>()
        if (repliesArr != null) {
          for (i in 0 until repliesArr.length()) {
            val r = repliesArr.optString(i).trim()
            if (r.isNotBlank()) replies.add(r)
          }
        }

        if (vocabList.isNotEmpty()) {
          val quizQuestions = vocabList.take(3).map { item ->
            ReadingQuestion(
              question = "Which expression means '${item.translation}'?",
              options = (listOf(item.word) + vocabList.filter { it.word != item.word }.map { it.word } + listOf("Other phrase")).distinct().take(4),
              correctIndex = 0,
              explanation = "'${item.word}' means '${item.translation}'. Example: ${item.exampleSentence}"
            )
          }
          return@withContext CustomInteractiveLessonData(
            id = "custom_lesson_${System.currentTimeMillis()}",
            title = title,
            promptRequest = cleanGoal,
            languageCode = languageCode,
            proficiencyLevel = proficiencyLevel,
            topic = topic,
            durationMinutes = durationMinutes,
            interactionMode = interactionMode,
            vocabulary = vocabList,
            exampleSentences = sentencePairs,
            listeningPrompt = obj.optString("listeningPrompt", vocabList.first().exampleSentence),
            listeningQuestion = obj.optString("listeningQuestion", "What is the main topic of this spoken passage?"),
            listeningOptions = lOptions,
            listeningAnswerIndex = 0,
            quizQuestions = quizQuestions,
            starterTutorMessage = obj.optString("starterTutorMessage", "Welcome! Let's practice $topic together. How would you start this conversation?"),
            starterTutorTranslation = obj.optString("starterTutorTranslation", "Welcome! Let's practice $topic together."),
            suggestedUserReplies = replies.ifEmpty {
              listOf(
                vocabList.first().exampleSentence,
                "Could you help me with $topic, please?",
                "I would like to practice this scenario step by step."
              )
            }
          )
        }
      } catch (_: Exception) {}
    }

    // Immediate structured fallback tailored to the user's request
    val fallbackVocab = getVocabularyCategories(languageCode).flatMap { it.subcategories }.flatMap { it.items }.take(4)
    val pairs = fallbackVocab.map { it.exampleSentence to it.exampleTranslation }
    CustomInteractiveLessonData(
      id = "custom_lesson_${System.currentTimeMillis()}",
      title = "Personalized Lesson: ${cleanGoal.take(36)}",
      promptRequest = cleanGoal,
      languageCode = languageCode,
      proficiencyLevel = proficiencyLevel,
      topic = topic,
      durationMinutes = durationMinutes,
      interactionMode = interactionMode,
      vocabulary = fallbackVocab,
      exampleSentences = pairs,
      listeningPrompt = fallbackVocab.firstOrNull()?.exampleSentence ?: "Welcome to your personalized language session.",
      listeningQuestion = "What is the speaker expressing in this sentence?",
      listeningOptions = listOf(
        fallbackVocab.firstOrNull()?.exampleTranslation ?: "A polite everyday expression",
        "Asking for a train refund",
        "Closing a bank account",
        "Postponing a concert"
      ),
      listeningAnswerIndex = 0,
      quizQuestions = fallbackVocab.take(3).map { item ->
        ReadingQuestion(
          question = "Select the word or phrase for '${item.translation}':",
          options = listOf(item.word, "Option B", "Option C", "Option D"),
          correctIndex = 0,
          explanation = "'${item.word}' means '${item.translation}'."
        )
      },
      starterTutorMessage = "Hello! We are practicing '$cleanGoal' ($proficiencyLevel). Say or type your first sentence whenever you're ready!",
      starterTutorTranslation = "Say or type your first sentence to begin our live roleplay.",
      suggestedUserReplies = listOf(
        fallbackVocab.firstOrNull()?.exampleSentence ?: "Hello, I would like to practice.",
        "Could we practice a real-life dialogue about $topic?",
        "Please correct my grammar and pronunciation as we talk."
      )
    )
  }

  /**
   * Generates a real, contextual tutor reply to the learner's actual message during a Custom Lesson
   * or Conversation Challenge. Never fabricates fake user messages.
   */
  suspend fun generateContextualPracticeReply(
    languageName: String,
    languageCode: String,
    proficiencyLevel: String,
    scenarioTitle: String,
    conversationHistory: List<Pair<String, String>>,
    latestUserMessage: String
  ): Triple<String, String, String> = withContext(Dispatchers.IO) {
    val historyBlock = conversationHistory.takeLast(6).joinToString("\n") { (role, text) -> "$role: $text" }
    val prompt = """
      You are a warm, encouraging $languageName language tutor in a scenario called "$scenarioTitle" (Learner level: $proficiencyLevel).
      Conversation so far:
      $historyBlock
      Learner just said: "$latestUserMessage"

      Respond directly to what the learner said. Keep the conversation moving naturally with 1-2 sentences and a follow-up question.
      Return ONLY a JSON object with:
      - "tutorReply": your response in $languageName
      - "translation": English translation of your response
      - "feedback": 1 short constructive tip or grammar/vocabulary praise in English about the learner's message "$latestUserMessage"
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(prompt)
    if (!raw.isNullOrBlank()) {
      try {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val reply = obj.optString("tutorReply").trim()
        val trans = obj.optString("translation").trim()
        val tip = obj.optString("feedback").trim()
        if (reply.isNotBlank()) {
          return@withContext Triple(
            reply,
            trans.ifBlank { reply },
            tip.ifBlank { "Clear communication! Keep building on your sentence." }
          )
        }
      } catch (_: Exception) {}
    }

    Triple(
      "That's a great point about '$latestUserMessage'! Could you tell me a little more about what you would do next in this situation?",
      "Great point! Could you tell me what you would do next in this situation?",
      "Nice job expressing your thought clearly. Try adding an adjective or time phrase on your next turn!"
    )
  }

  fun toPersistedAiCustomLesson(lesson: CustomInteractiveLessonData): AiCustomLesson {
    return AiCustomLesson(
      id = lesson.id,
      title = lesson.title,
      languageCode = lesson.languageCode,
      cefrLevel = lesson.proficiencyLevel,
      userGoalPrompt = "${lesson.promptRequest} • ${lesson.topic} • ${lesson.durationMinutes}m (${lesson.interactionMode})",
      vocabularyList = lesson.vocabulary.map { "${it.word} — ${it.translation} (${it.exampleSentence})" },
      grammarNotes = lesson.exampleSentences.map { "${it.first} → ${it.second}" },
      speakingExercises = listOf(lesson.starterTutorMessage) + lesson.suggestedUserReplies,
      quizQuestions = lesson.quizQuestions
    )
  }

  fun fromPersistedAiCustomLesson(saved: AiCustomLesson): CustomInteractiveLessonData {
    val vocabItems = saved.vocabularyList.mapIndexed { idx, line ->
      val parts = line.split("—")
      val word = parts.getOrNull(0)?.trim() ?: line
      val rest = parts.getOrNull(1)?.trim() ?: word
      val meaning = rest.substringBefore("(").trim().ifBlank { rest }
      val example = rest.substringAfter("(", "").substringBeforeLast(")").trim().ifBlank { word }
      VocabCategoryItem(
        id = "${saved.id}_v_$idx",
        word = word,
        translation = meaning,
        phonetic = "/${word.lowercase()}/",
        exampleSentence = example,
        exampleTranslation = meaning,
        listeningPrompt = example,
        speakingPrompt = example,
        quizQuestion = "What does '$word' mean?",
        quizOptions = listOf(meaning, "Different option", "Other meaning", "None of these"),
        correctOptionIndex = 0,
        illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE
      )
    }.ifEmpty {
      getVocabularyCategories(saved.languageCode).first().subcategories.first().items
    }

    val sentences = saved.grammarNotes.map { note ->
      val p = note.split("→")
      (p.getOrNull(0)?.trim() ?: note) to (p.getOrNull(1)?.trim() ?: "")
    }

    return CustomInteractiveLessonData(
      id = saved.id,
      title = saved.title,
      promptRequest = saved.userGoalPrompt,
      languageCode = saved.languageCode,
      proficiencyLevel = saved.cefrLevel,
      topic = "Custom Practice",
      durationMinutes = 10,
      interactionMode = "Voice & Text",
      vocabulary = vocabItems,
      exampleSentences = sentences,
      listeningPrompt = vocabItems.first().exampleSentence,
      listeningQuestion = "Which meaning matches the spoken sentence?",
      listeningOptions = listOf(
        vocabItems.first().translation,
        "Different topic",
        "Travel delay",
        "Weather forecast"
      ),
      listeningAnswerIndex = 0,
      quizQuestions = saved.quizQuestions,
      starterTutorMessage = saved.speakingExercises.firstOrNull()
        ?: "Welcome back to '${saved.title}'! Share your first sentence to continue practicing.",
      starterTutorTranslation = "Share your first sentence to continue practicing.",
      suggestedUserReplies = saved.speakingExercises.drop(1).ifEmpty {
        listOf(vocabItems.first().exampleSentence)
      }
    )
  }

  fun toFlashcard(item: VocabCategoryItem, categoryName: String): VocabFlashcard {
    return VocabFlashcard(
      id = "fc_${item.id}",
      word = item.word,
      translation = item.translation,
      phonetic = item.phonetic,
      partOfSpeech = "Vocabulary",
      cefrLevel = "A2",
      category = categoryName,
      exampleSentence = item.exampleSentence,
      exampleTranslation = item.exampleTranslation,
      synonyms = emptyList(),
      masteryScore = 1,
      nextReviewLabel = "Saved from $categoryName"
    )
  }
}
