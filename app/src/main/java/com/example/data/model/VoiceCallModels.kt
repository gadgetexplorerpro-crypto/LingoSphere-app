package com.example.data.model

enum class CallMode(
  val id: String,
  val title: String,
  val description: String,
  val emoji: String,
  val systemInstruction: String
) {
  FREE_TALK(
    id = "free_talk",
    title = "Free Conversation",
    description = "Casual open-ended conversation about anything you like.",
    emoji = "💬",
    systemInstruction = "Engage in an open, warm, natural chat. Ask thoughtful follow-up questions about the learner's day, ideas, and experiences. Speak like a close friend."
  ),
  INTRODUCING_YOURSELF(
    id = "introducing_yourself",
    title = "Introducing Yourself",
    description = "Share your background, hometown, interests, and goals naturally.",
    emoji = "👋",
    systemInstruction = "You are meeting the learner for the first time. Warmly ask about their name, where they are from, their hobbies, and why they are learning this language."
  ),
  MAKING_FRIENDS(
    id = "making_friends",
    title = "Making Friends",
    description = "Start friendly conversations, find common interests, and make plans.",
    emoji = "🤝",
    systemInstruction = "You are a friendly peer at a social gathering or coffee shop. Chat naturally about music, weekend plans, favorite places, and shared hobbies."
  ),
  DAILY(
    id = "daily",
    title = "Daily Routines",
    description = "Talk about morning habits, work/study schedules, weekend plans, and hobbies.",
    emoji = "☕",
    systemInstruction = "Talk casually about everyday life, morning routines, meals, weekend plans, weather, and current habits. Keep it lively and relatable."
  ),
  SHOPPING(
    id = "shopping",
    title = "Shopping",
    description = "Ask for sizes, compare prices, request refunds, and browse stores.",
    emoji = "🛍️",
    systemInstruction = "You are a helpful boutique and department store associate. Help the customer find items, check sizes and colors, discuss prices or discounts, and complete checkout."
  ),
  RESTAURANT(
    id = "restaurant",
    title = "Ordering Food",
    description = "Order delicious food, ask about specials, and handle dietary preferences.",
    emoji = "🍽️",
    systemInstruction = "You are an attentive restaurant server. Welcome the guest, present daily specials, take food and drink orders, answer ingredient questions, and handle the bill."
  ),
  JOB_INTERVIEW(
    id = "job_interview",
    title = "Job Interviews",
    description = "Practice professional interviews, elevator pitches, and behavioral questions.",
    emoji = "💼",
    systemInstruction = "You are an encouraging hiring manager. Ask professional behavioral and situational questions, evaluate problem solving, and give constructive feedback on tone and clarity."
  ),
  AIRPORT(
    id = "airport_conversations",
    title = "Airport Conversations",
    description = "Check in luggage, pass customs and immigration, and find boarding gates.",
    emoji = "🛫",
    systemInstruction = "You are an airline check-in agent and immigration officer. Ask for the traveler's passport and boarding pass, ask about luggage and trip purpose, and give gate directions."
  ),
  HOTEL_CONCIERGE(
    id = "hotel_concierge",
    title = "Hotel Booking",
    description = "Check into rooms, request amenities, and ask the front desk for local tips.",
    emoji = "🏨",
    systemInstruction = "You are a welcoming hotel receptionist and concierge. Assist the guest with checking in, room preferences, breakfast hours, and local recommendations."
  ),
  ASKING_DIRECTIONS(
    id = "asking_directions",
    title = "Asking for Directions",
    description = "Navigate city streets, metro stations, landmarks, and public transit.",
    emoji = "🗺️",
    systemInstruction = "You are a helpful local resident. Give clear street directions using landmarks, metro lines, walking times, and turns, and ask the learner to confirm the route."
  ),
  UNIVERSITY(
    id = "university_conversations",
    title = "University Conversations",
    description = "Discuss lectures, campus life, study groups, and professor office hours.",
    emoji = "🎓",
    systemInstruction = "You are an approachable university advisor and classmate. Discuss course registration, seminar topics, campus clubs, and research assignments."
  ),
  MEDICAL_CALL(
    id = "medical_call",
    title = "Doctor Appointments",
    description = "Describe symptoms clearly, understand prescriptions, and book clinic visits.",
    emoji = "🩺",
    systemInstruction = "You are a calm, caring doctor and clinic coordinator. Ask about the patient's symptoms, how long they have felt unwell, allergies, and explain recovery steps clearly."
  ),
  BUSINESS(
    id = "business",
    title = "Business Meetings",
    description = "Practice negotiations, project updates, presentations, and executive discussions.",
    emoji = "📈",
    systemInstruction = "You are a corporate colleague or business partner. Discuss quarterly goals, meeting agendas, project timelines, and collaborate professionally."
  ),
  FAMILY(
    id = "family_conversations",
    title = "Family Conversations",
    description = "Talk about family members, celebrations, traditions, and home life.",
    emoji = "🏡",
    systemInstruction = "You are a warm family friend. Chat about family traditions, relatives, childhood memories, holiday meals, and home life."
  ),
  TRAVEL(
    id = "travel",
    title = "Travel",
    description = "Explore destinations, book tours, order taxis, and discover cultural sights.",
    emoji = "✈️",
    systemInstruction = "You are a knowledgeable travel guide and local host. Help the traveler explore iconic sights, local transport, hidden gems, and cultural customs."
  ),
  BEGINNER(
    id = "beginner",
    title = "Beginner Practice",
    description = "Simple vocabulary, slow pacing, and immediate bilingual assistance.",
    emoji = "🌱",
    systemInstruction = "You are an exceptionally gentle language coach. Speak in short, simple sentences with clear pronunciation. If the user hesitates, suggest words and praise their effort."
  ),
  ACCENT_COACH(
    id = "accent_coach",
    title = "Accent & Phonetics Lab",
    description = "Intensive pronunciation correction, syllable rhythm, and native intonation training.",
    emoji = "🎙️",
    systemInstruction = "You are an expert native accent and phonetics coach. Listen closely to the learner's phrasing, model natural native intonation, point out vowel/consonant nuances, and ask them to repeat key phrases naturally."
  ),
  GRAMMAR_DRILL(
    id = "grammar_drill",
    title = "Live Grammar Feedback",
    description = "Active tense, verb conjugation, and sentence structure coaching in real time.",
    emoji = "📐",
    systemInstruction = "You are a supportive grammar specialist. Converse naturally while actively testing past, present, and future tenses. Gently reformulate any grammatical mistakes and explain why."
  ),
  CULTURAL_SLANG(
    id = "cultural_slang",
    title = "Local Slang & Traditions",
    description = "Learn authentic street idioms, regional expressions, and cultural customs.",
    emoji = "🎭",
    systemInstruction = "You are a charismatic local cultural guide. Teach authentic everyday idioms, polite slang, and local traditions in a fun conversation."
  );

  companion object {
    val CORE_15_TOPICS: List<CallMode>
      get() = listOf(
        INTRODUCING_YOURSELF,
        MAKING_FRIENDS,
        DAILY,
        SHOPPING,
        RESTAURANT,
        JOB_INTERVIEW,
        AIRPORT,
        HOTEL_CONCIERGE,
        ASKING_DIRECTIONS,
        UNIVERSITY,
        MEDICAL_CALL,
        BUSINESS,
        FAMILY,
        TRAVEL,
        FREE_TALK
      )

    val FREE_CONVERSATION: CallMode
      get() = FREE_TALK

    fun findById(id: String): CallMode {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        ?: entries.firstOrNull { it.title.equals(id, ignoreCase = true) }
        ?: FREE_TALK
    }
  }
}

enum class CorrectionMode(
  val id: String,
  val title: String,
  val subtitle: String,
  val promptInstruction: String
) {
  IMMEDIATE(
    id = "immediate",
    title = "Correct me immediately",
    subtitle = "Get gentle grammar, vocabulary, and natural phrasing tips right inside each turn.",
    promptInstruction = "Provide immediate, gentle language coaching on any grammar, vocabulary, or phrasing improvement from the learner's turn."
  ),
  AFTER_SPEAKING(
    id = "after_speaking",
    title = "Correct me after speaking",
    subtitle = "Keep the conversation flowing first, then review corrections in your speaking feedback panel.",
    promptInstruction = "Respond naturally to keep the conversation flowing, and attach structured grammar/phrasing notes for post-turn review without interrupting your spoken reply."
  ),
  CONVERSATION_ONLY(
    id = "conversation_only",
    title = "Conversation only",
    subtitle = "Pure natural conversation with no grammar interruptions unless you ask.",
    promptInstruction = "Focus 100% on natural, authentic human conversation. Do not interrupt with grammar explanations unless the learner explicitly asks for help."
  );

  companion object {
    fun findById(id: String): CorrectionMode {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IMMEDIATE
    }
  }
}

enum class TutorVoice(
  val id: String,
  val displayName: String,
  val styleTag: String,
  val gender: String,
  val description: String,
  val pitch: Float = 1.0f,
  val speed: Float = 0.95f,
  val tier: String = "Free", // "Free", "Pro", "Premium"
  val preferredRegion: String = "US",
  val voiceSlotIndex: Int = 0,
  val personaName: String = id,
  val accentLabel: String = "American (US)",
  val styleCategory: String = "Conversational",
  val samplePhrase: String = "Hello! I'm excited to practice speaking with you today."
) {
  AOEDE(
    id = "Aoede",
    displayName = "Aoede — Breezy",
    styleTag = "Breezy",
    gender = "Female",
    description = "Breezy, warm, encouraging & melodic American mezzo voice",
    pitch = 1.07f,
    speed = 0.96f,
    tier = "Free",
    preferredRegion = "US",
    voiceSlotIndex = 0,
    personaName = "Emma",
    accentLabel = "American (US)",
    styleCategory = "Conversational",
    samplePhrase = "Hi there! I'm Emma with the Aoede voice. Let's have a relaxed, breezy conversation about your day."
  ),
  KORE(
    id = "Kore",
    displayName = "Kore — Firm",
    styleTag = "Firm",
    gender = "Female",
    description = "Firm, articulate, confident & structured bilingual alto voice",
    pitch = 1.00f,
    speed = 0.99f,
    tier = "Free",
    preferredRegion = "ES",
    voiceSlotIndex = 4,
    personaName = "Isabella",
    accentLabel = "Spanish / Neutral Bilingual",
    styleCategory = "Coach & Clear",
    samplePhrase = "Hello, I'm Isabella with the Kore voice. I'll help you speak with clarity, precision, and strong grammar."
  ),
  ZEPHYR(
    id = "Zephyr",
    displayName = "Zephyr — Bright",
    styleTag = "Bright",
    gender = "Female",
    description = "Bright, cheerful, uplifting & crystal-clear soprano voice",
    pitch = 1.21f,
    speed = 1.04f,
    tier = "Free",
    preferredRegion = "AU",
    voiceSlotIndex = 1,
    personaName = "Sophia",
    accentLabel = "Australian (AU)",
    styleCategory = "Conversational",
    samplePhrase = "Hey! I'm Sophia with the Zephyr voice. Ready to brighten up your fluency with fun, everyday topics?"
  ),
  PUCK(
    id = "Puck",
    displayName = "Puck — Upbeat",
    styleTag = "Upbeat",
    gender = "Male",
    description = "Upbeat, energetic, friendly & lively continental tenor voice",
    pitch = 0.89f,
    speed = 1.04f,
    tier = "Free",
    preferredRegion = "FR",
    voiceSlotIndex = 5,
    personaName = "Lucas",
    accentLabel = "French / European",
    styleCategory = "Conversational",
    samplePhrase = "Bonjour and hey friend! I'm Lucas with the Puck voice. Let's jump right into an upbeat roleplay!"
  ),
  CHARON(
    id = "Charon",
    displayName = "Charon — Informative",
    styleTag = "Informative",
    gender = "Male",
    description = "Informative, calm, thoughtful & resonant British baritone voice",
    pitch = 0.74f,
    speed = 0.92f,
    tier = "Free",
    preferredRegion = "GB",
    voiceSlotIndex = 0,
    personaName = "James",
    accentLabel = "British (UK)",
    styleCategory = "Audiobook Narrator",
    samplePhrase = "Greetings from London! I'm James with the Charon voice. Shall we explore articulate British expression?"
  ),
  FENRIR(
    id = "Fenrir",
    displayName = "Fenrir — Excitable",
    styleTag = "Excitable",
    gender = "Male",
    description = "Excitable, dynamic, expressive & motivating interview tenor voice",
    pitch = 0.84f,
    speed = 1.06f,
    tier = "Pro",
    preferredRegion = "IE",
    voiceSlotIndex = 2,
    personaName = "Michael",
    accentLabel = "Irish (IE)",
    styleCategory = "Expressive",
    samplePhrase = "Welcome! I'm Michael with the Fenrir voice. Let's make your interview answers energetic and unforgettable!"
  ),
  LEDA(
    id = "Leda",
    displayName = "Leda — Youthful",
    styleTag = "Youthful",
    gender = "Female",
    description = "Youthful, fresh, bubbly & modern high-soprano peer voice",
    pitch = 1.28f,
    speed = 1.07f,
    tier = "Pro",
    preferredRegion = "US",
    voiceSlotIndex = 5,
    personaName = "Mia",
    accentLabel = "American (Youthful US)",
    styleCategory = "Conversational",
    samplePhrase = "Hi! I'm Mia with the Leda voice! Let's chat like close friends about campus life, music, and weekends."
  ),
  ORUS(
    id = "Orus",
    displayName = "Orus — Firm",
    styleTag = "Firm",
    gender = "Male",
    description = "Firm, executive, poised & authoritative bass-baritone voice",
    pitch = 0.68f,
    speed = 0.95f,
    tier = "Pro",
    preferredRegion = "US",
    voiceSlotIndex = 1,
    personaName = "Daniel",
    accentLabel = "American (Executive US)",
    styleCategory = "Coach & Clear",
    samplePhrase = "Good day, I'm Daniel with the Orus voice. Let's sharpen your executive communication and leadership presence."
  ),
  ACHERNAR(
    id = "Achernar",
    displayName = "Achernar — Soft",
    styleTag = "Soft",
    gender = "Female",
    description = "Soft, gentle, patient & reassuring voice for beginners and calm reading",
    pitch = 1.11f,
    speed = 0.84f,
    tier = "Pro",
    preferredRegion = "US",
    voiceSlotIndex = 6,
    personaName = "Grace",
    accentLabel = "American (Gentle US)",
    styleCategory = "Audiobook Narrator",
    samplePhrase = "Hello, I'm Grace with the Achernar voice. Take your time—we'll practice gently step by step at your pace."
  ),
  ACHIRD(
    id = "Achird",
    displayName = "Achird — Friendly",
    styleTag = "Friendly",
    gender = "Male",
    description = "Friendly, patient, slow-paced & reassuring beginner male voice",
    pitch = 0.79f,
    speed = 0.85f,
    tier = "Pro",
    preferredRegion = "CA",
    voiceSlotIndex = 3,
    personaName = "Noah",
    accentLabel = "Canadian (CA)",
    styleCategory = "Conversational",
    samplePhrase = "Hi there! I'm Noah with the Achird voice. No rush at all—we will practice slowly step by step."
  ),
  GACRUX(
    id = "Gacrux",
    displayName = "Gacrux — Mature",
    styleTag = "Mature",
    gender = "Male",
    description = "Mature, seasoned, deep & distinguished academic bass storyteller voice",
    pitch = 0.63f,
    speed = 0.89f,
    tier = "Premium",
    preferredRegion = "GB",
    voiceSlotIndex = 4,
    personaName = "Ethan",
    accentLabel = "British (Academic UK)",
    styleCategory = "Audiobook Narrator",
    samplePhrase = "Welcome, I'm Ethan with the Gacrux voice. Let's refine your advanced C1–C2 rhetoric and nuanced debate."
  ),
  SULAFAT(
    id = "Sulafat",
    displayName = "Sulafat — Warm",
    styleTag = "Warm",
    gender = "Female",
    description = "Warm, empathetic, expressive & rich Australian contralto storyteller",
    pitch = 0.95f,
    speed = 0.93f,
    tier = "Premium",
    preferredRegion = "AU",
    voiceSlotIndex = 3,
    personaName = "Amelia",
    accentLabel = "Australian (Warm AU)",
    styleCategory = "Audiobook Narrator",
    samplePhrase = "G'day traveler! I'm Amelia with the Sulafat voice. Where in the world are we exploring today?"
  ),
  DESPINA(
    id = "Despina",
    displayName = "Despina — Smooth",
    styleTag = "Smooth",
    gender = "Female",
    description = "Smooth, polished, melodic & effortless studio audiobook voice",
    pitch = 1.03f,
    speed = 0.97f,
    tier = "Premium",
    preferredRegion = "GB",
    voiceSlotIndex = 7,
    personaName = "Clara",
    accentLabel = "British (Studio UK)",
    styleCategory = "Audiobook Narrator",
    samplePhrase = "Hi, I'm Clara with the Despina voice. Together we'll make your pronunciation and rhythm sound effortless."
  ),
  ERINOME(
    id = "Erinome",
    displayName = "Erinome — Clear",
    styleTag = "Clear",
    gender = "Female",
    description = "Clear, precise, phonetic-focused & broadcast-grade coach voice",
    pitch = 1.14f,
    speed = 0.90f,
    tier = "Premium",
    preferredRegion = "CA",
    voiceSlotIndex = 2,
    personaName = "Olivia",
    accentLabel = "Canadian (Neutral Clear)",
    styleCategory = "Coach & Clear",
    samplePhrase = "Hello! I'm Olivia with the Erinome voice. Notice every syllable as we polish your pronunciation together."
  ),
  UMBRIEL(
    id = "Umbriel",
    displayName = "Umbriel — Easy-going",
    styleTag = "Easy-going",
    gender = "Male",
    description = "Easy-going, relaxed, casual & stress-free partner voice",
    pitch = 0.76f,
    speed = 0.94f,
    tier = "Premium",
    preferredRegion = "AU",
    voiceSlotIndex = 6,
    personaName = "Alexander",
    accentLabel = "Australian (Casual AU)",
    styleCategory = "Audiobook Narrator",
    samplePhrase = "Hey! I'm Alexander with the Umbriel voice. No pressure at all—let's just hang out and practice naturally."
  );

  val voiceName: String
    get() = id

  val providerVoiceId: String
    get() = id

  val openAiVoiceId: String
    get() = com.example.ai.AiProviderManager.mapTutorVoiceToOpenAiVoice(this)

  val distinctVocalSignature: String
    get() = "$providerVoiceId|$openAiVoiceId|$gender|$preferredRegion|$pitch|$speed|$voiceSlotIndex"

  companion object {
    fun findById(id: String): TutorVoice {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        ?: entries.firstOrNull { it.personaName.equals(id, ignoreCase = true) }
        ?: entries.firstOrNull { it.displayName.contains(id, ignoreCase = true) }
        ?: AOEDE
    }

    /**
     * Consistent sample sentence per language so previewing any two voices compares
     * the exact same words under identical speaking text.
     */
    fun consistentPreviewSentence(languageCode: String): String {
      return when (languageCode.lowercase().trim()) {
        "es" -> "Hola, esta es una prueba de voz para conversar con naturalidad y escuchar audiolibros."
        "fr" -> "Bonjour, ceci est un aperçu vocal pour parler naturellement et écouter des livres audio."
        "de" -> "Hallo, dies ist eine Stimmprobe für natürliche Gespräche und entspanntes Hörbuchlesen."
        "it" -> "Ciao, questa è un'anteprima vocale per conversare con naturalezza e ascoltare audiolibri."
        "pt" -> "Olá, esta é uma prévia de voz para conversar naturalmente e ouvir audiolivros."
        "ja" -> "こんにちは。自然な会話とオーディオブック朗読のための音声プレビューです。"
        "ko" -> "안녕하세요. 자연스러운 대화와 오디오북 낭독을 위한 음성 미리듣기입니다."
        "zh" -> "你好，这是用于自然对话和有声书朗读的声音试听。"
        "ar" -> "مرحباً، هذه معاينة صوتية للمحادثة الطبيعية والاستماع إلى الكتب الصوتية."
        "hi" -> "नमस्ते, यह स्वाभाविक बातचीत और ऑडियोबुक सुनने के लिए एक आवाज़ का नमूना है।"
        "ru" -> "Привет, это пример голоса для живого общения и спокойного чтения аудиокниг."
        "tr" -> "Merhaba, bu doğal sohbet ve sesli kitap dinleme için bir ses önizlemesidir."
        else -> "Hello! This is a voice preview for natural conversation and calm audiobook storytelling."
      }
    }
  }
}

data class AiConversationPartner(
  val id: String,
  val name: String,
  val roleTitle: String,
  val shortDescription: String,
  val personality: String,
  val specialty: String,
  val teachingStyle: String,
  val primaryLanguageCode: String,
  val supportedLanguageCodes: List<String>,
  val accent: String,
  val recommendedLevels: List<String>, // "Beginner", "Intermediate", "Advanced"
  val cefrRange: String,
  val voice: TutorVoice,
  val avatarUrl: String,
  val gender: String,
  val favoriteTopics: List<CallMode>,
  val sampleGreeting: String
) {
  val defaultVoice: TutorVoice
    get() = voice

  val photoUrl: String
    get() = avatarUrl

  val accentFlag: String
    get() = when {
      accent.contains("British", ignoreCase = true) || accent.contains("UK", ignoreCase = true) -> "🇬🇧"
      accent.contains("Australian", ignoreCase = true) -> "🇦🇺"
      accent.contains("Canadian", ignoreCase = true) -> "🇨🇦"
      accent.contains("Irish", ignoreCase = true) -> "🇮🇪"
      accent.contains("Spanish", ignoreCase = true) -> "🇪🇸"
      accent.contains("European", ignoreCase = true) || accent.contains("French", ignoreCase = true) -> "🇫🇷"
      else -> "🇺🇸"
    }

  companion object {
    val DEFAULT: AiConversationPartner
      get() = ALL_PARTNERS.first()

    val CHLOE_PARTNER = AiConversationPartner(
      id = "partner_chloe",
      name = "Chloe",
      roleTitle = "AI Roleplay & Conversation Partner",
      shortDescription = "Interactive restaurant, hotel, shopping, and daily life roleplay specialist.",
      personality = "Warm, natural, expressive, and attentive conversation partner",
      specialty = "Immersive Real-Life Roleplay, Dining, Travel & Everyday Situations",
      teachingStyle = "Natural interactive roleplay where you speak or type at your own pace",
      primaryLanguageCode = "en",
      supportedLanguageCodes = listOf("en", "es", "fr", "de", "ja", "ko", "zh", "it", "pt", "ar", "hi", "ru", "tr", "nl"),
      accent = "American / Neutral International",
      recommendedLevels = listOf("Beginner", "Intermediate", "Advanced"),
      cefrRange = "A1–C2",
      voice = TutorVoice.AOEDE,
      avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
      gender = "Female",
      favoriteTopics = listOf(CallMode.RESTAURANT, CallMode.HOTEL_CONCIERGE, CallMode.SHOPPING, CallMode.DAILY),
      sampleGreeting = "Hello! I'm Chloe. Let's jump into a realistic roleplay conversation whenever you're ready."
    )

    val ALL_PARTNERS: List<AiConversationPartner> = listOf(
      AiConversationPartner(
        id = "partner_emma",
        name = "Emma",
        roleTitle = "Friendly English Tutor",
        shortDescription = "Warm, supportive tutor for everyday fluency and confidence.",
        personality = "Warm, patient, uplifting, and attentive to every learner's goals",
        specialty = "Everyday Fluency, Natural Phrasing & Confidence Building",
        teachingStyle = "Encouraging guided conversation with gentle, actionable tips",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "es", "fr", "de", "it", "pt"),
        accent = "American (US)",
        recommendedLevels = listOf("Beginner", "Intermediate", "Advanced"),
        cefrRange = "A1–C2",
        voice = TutorVoice.AOEDE,
        avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=800&auto=format&fit=crop&q=80",
        gender = "Female",
        favoriteTopics = listOf(CallMode.INTRODUCING_YOURSELF, CallMode.DAILY, CallMode.MAKING_FRIENDS, CallMode.FREE_TALK),
        sampleGreeting = "Hi! I'm Emma, your friendly language tutor. What would you love to talk about today?"
      ),
      AiConversationPartner(
        id = "partner_james",
        name = "James",
        roleTitle = "British Conversation Coach",
        shortDescription = "Refined British phrasing, idioms, and articulate expression.",
        personality = "Witty, polite, cultured, and perceptive British conversationalist",
        specialty = "British English Nuances, Polite Idioms & Articulate Discussion",
        teachingStyle = "Refined dialogue modeling natural UK intonation and vocabulary",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "fr", "de", "es"),
        accent = "British (UK)",
        recommendedLevels = listOf("Intermediate", "Advanced"),
        cefrRange = "B1–C2",
        voice = TutorVoice.CHARON,
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&auto=format&fit=crop&q=80",
        gender = "Male",
        favoriteTopics = listOf(CallMode.TRAVEL, CallMode.UNIVERSITY, CallMode.CULTURAL_SLANG, CallMode.FREE_TALK),
        sampleGreeting = "Hello there! I'm James from London. Shall we have a proper chat and polish your expression?"
      ),
      AiConversationPartner(
        id = "partner_sophia",
        name = "Sophia",
        roleTitle = "Daily Conversation Partner",
        shortDescription = "Lively chats about hobbies, routines, social life, and culture.",
        personality = "Cheerful, curious, energetic, and easy to talk to like a best friend",
        specialty = "Daily Life, Socializing, Hobbies & Spontaneous Chat",
        teachingStyle = "Relaxed, natural back-and-forth with everyday expressions",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "it", "es", "fr", "pt"),
        accent = "Australian / International",
        recommendedLevels = listOf("Beginner", "Intermediate"),
        cefrRange = "A2–B2",
        voice = TutorVoice.ZEPHYR,
        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&auto=format&fit=crop&q=80",
        gender = "Female",
        favoriteTopics = listOf(CallMode.DAILY, CallMode.MAKING_FRIENDS, CallMode.FAMILY, CallMode.SHOPPING),
        sampleGreeting = "Hey! I'm Sophia. Tell me about your day—what have you been up to lately?"
      ),
      AiConversationPartner(
        id = "partner_daniel",
        name = "Daniel",
        roleTitle = "Business English Coach",
        shortDescription = "Executive meetings, presentations, negotiations, and leadership.",
        personality = "Strategic, composed, articulate, and results-oriented mentor",
        specialty = "Business Meetings, Negotiations, Presentations & Workplace Fluency",
        teachingStyle = "Executive roleplay with professional vocabulary upgrades",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "de", "zh", "ja", "es"),
        accent = "American (Executive)",
        recommendedLevels = listOf("Intermediate", "Advanced"),
        cefrRange = "B1–C2",
        voice = TutorVoice.ORUS,
        avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80",
        gender = "Male",
        favoriteTopics = listOf(CallMode.BUSINESS, CallMode.JOB_INTERVIEW, CallMode.UNIVERSITY, CallMode.HOTEL_CONCIERGE),
        sampleGreeting = "Good to meet you! I'm Daniel. Let's sharpen your professional communication for meetings and leadership."
      ),
      AiConversationPartner(
        id = "partner_olivia",
        name = "Olivia",
        roleTitle = "Pronunciation Coach",
        shortDescription = "Crystal-clear phonetics, syllable stress, rhythm, and accent polish.",
        personality = "Attentive, encouraging, precise, and musical ear for speech rhythm",
        specialty = "Pronunciation, Intonation, Connected Speech & Vowel Clarity",
        teachingStyle = "Acoustic modeling with clear phonetic breakdowns and rhythm drills",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "fr", "es", "it", "de"),
        accent = "Canadian / Clear Neutral",
        recommendedLevels = listOf("Beginner", "Intermediate", "Advanced"),
        cefrRange = "A1–C2",
        voice = TutorVoice.ERINOME,
        avatarUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=800&auto=format&fit=crop&q=80",
        gender = "Female",
        favoriteTopics = listOf(CallMode.ACCENT_COACH, CallMode.INTRODUCING_YOURSELF, CallMode.DAILY, CallMode.FREE_TALK),
        sampleGreeting = "Hello! I'm Olivia, your pronunciation coach. We'll focus on clear sounds and natural rhythm together."
      ),
      AiConversationPartner(
        id = "partner_michael",
        name = "Michael",
        roleTitle = "Interview Coach",
        shortDescription = "Mock job interviews, behavioral STAR answers, and career confidence.",
        personality = "Constructive, insightful, motivating hiring specialist",
        specialty = "Job Interviews, Elevator Pitches, Career Storytelling & Confidence",
        teachingStyle = "Realistic interview questioning with instant phrasing upgrades",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "es", "fr", "de", "ja"),
        accent = "Irish / International",
        recommendedLevels = listOf("Intermediate", "Advanced"),
        cefrRange = "B1–C2",
        voice = TutorVoice.FENRIR,
        avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=800&auto=format&fit=crop&q=80",
        gender = "Male",
        favoriteTopics = listOf(CallMode.JOB_INTERVIEW, CallMode.BUSINESS, CallMode.UNIVERSITY, CallMode.INTRODUCING_YOURSELF),
        sampleGreeting = "Welcome! I'm Michael. Ready to practice interview questions and make your answers stand out?"
      ),
      AiConversationPartner(
        id = "partner_amelia",
        name = "Amelia",
        roleTitle = "Travel Conversation Partner",
        shortDescription = "Airports, hotels, directions, and cultural adventures worldwide.",
        personality = "Adventurous, worldly, helpful, and full of cultural curiosity",
        specialty = "Airports, Hotel Booking, Asking Directions & Cultural Travel",
        teachingStyle = "Immersive real-world travel scenarios and survival phrases",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "es", "fr", "it", "pt", "ja"),
        accent = "Australian",
        recommendedLevels = listOf("Beginner", "Intermediate", "Advanced"),
        cefrRange = "A1–C1",
        voice = TutorVoice.SULAFAT,
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
        gender = "Female",
        favoriteTopics = listOf(CallMode.TRAVEL, CallMode.AIRPORT, CallMode.HOTEL_CONCIERGE, CallMode.ASKING_DIRECTIONS),
        sampleGreeting = "G'day traveler! I'm Amelia. Where in the world are we flying to in our practice session today?"
      ),
      AiConversationPartner(
        id = "partner_noah",
        name = "Noah",
        roleTitle = "Beginner Speaking Partner",
        shortDescription = "Slow, patient, step-by-step speaking practice with zero pressure.",
        personality = "Exceptionally patient, gentle, reassuring, and supportive",
        specialty = "First Conversations, Core Vocabulary & Building Speaking Courage",
        teachingStyle = "Slow, clear sentences with helpful hints whenever you pause",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "es", "fr", "de", "it", "ja", "ko", "zh"),
        accent = "Canadian (Slow & Clear)",
        recommendedLevels = listOf("Beginner"),
        cefrRange = "A1–A2",
        voice = TutorVoice.ACHIRD,
        avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80",
        gender = "Male",
        favoriteTopics = listOf(CallMode.INTRODUCING_YOURSELF, CallMode.BEGINNER, CallMode.DAILY, CallMode.FAMILY),
        sampleGreeting = "Hello! I'm Noah. Take your time—we will speak slowly and simply step by step."
      ),
      AiConversationPartner(
        id = "partner_isabella",
        name = "Isabella",
        roleTitle = "Grammar and Speaking Coach",
        shortDescription = "Master verb tenses, sentence structure, and accurate fluency live.",
        personality = "Structured, articulate, insightful, and encouraging educator",
        specialty = "Live Grammar Mastery, Sentence Structure & Complex Tenses",
        teachingStyle = "Natural conversation woven with clear, memorable grammar explanations",
        primaryLanguageCode = "es",
        supportedLanguageCodes = listOf("es", "en", "fr", "it", "pt"),
        accent = "Spanish / Neutral Bilingual",
        recommendedLevels = listOf("Beginner", "Intermediate", "Advanced"),
        cefrRange = "A2–C1",
        voice = TutorVoice.KORE,
        avatarUrl = "https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?w=800&auto=format&fit=crop&q=80",
        gender = "Female",
        favoriteTopics = listOf(CallMode.GRAMMAR_DRILL, CallMode.DAILY, CallMode.UNIVERSITY, CallMode.DOCTOR_APPOINTMENTS_ALIAS),
        sampleGreeting = "Hello! I'm Isabella. Let's turn grammar rules into natural, confident spoken sentences."
      ),
      AiConversationPartner(
        id = "partner_ethan",
        name = "Ethan",
        roleTitle = "Advanced English Coach",
        shortDescription = "Debates, nuanced idioms, academic topics, and C1–C2 mastery.",
        personality = "Intellectual, sharp, charismatic, and deeply engaging",
        specialty = "C1–C2 Fluency, Nuanced Debate, Academic & Idiomatic Mastery",
        teachingStyle = "Thought-provoking questions that challenge you to express complex ideas",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "de", "fr", "es"),
        accent = "British / Transatlantic",
        recommendedLevels = listOf("Advanced"),
        cefrRange = "B2–C2",
        voice = TutorVoice.GACRUX,
        avatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=800&auto=format&fit=crop&q=80",
        gender = "Male",
        favoriteTopics = listOf(CallMode.UNIVERSITY, CallMode.BUSINESS, CallMode.FREE_TALK, CallMode.CULTURAL_SLANG),
        sampleGreeting = "Greetings, I'm Ethan. What complex idea, cultural shift, or debate shall we unpack today?"
      ),
      AiConversationPartner(
        id = "partner_mia",
        name = "Mia",
        roleTitle = "Friendly Everyday Conversation",
        shortDescription = "Relaxed coffee-shop chats, making friends, and family stories.",
        personality = "Bubbly, empathetic, youthful, and genuinely interested in you",
        specialty = "Making Friends, Coffee Chats, Weekend Plans & Campus Life",
        teachingStyle = "Stress-free, upbeat peer conversation with natural expressions",
        primaryLanguageCode = "en",
        supportedLanguageCodes = listOf("en", "es", "fr", "ko", "ja"),
        accent = "American (Youthful)",
        recommendedLevels = listOf("Beginner", "Intermediate"),
        cefrRange = "A1–B2",
        voice = TutorVoice.LEDA,
        avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80",
        gender = "Female",
        favoriteTopics = listOf(CallMode.MAKING_FRIENDS, CallMode.DAILY, CallMode.FAMILY, CallMode.FREE_TALK),
        sampleGreeting = "Hi! I'm Mia! It's so nice to meet you—what do you like doing for fun on weekends?"
      ),
      AiConversationPartner(
        id = "partner_lucas",
        name = "Lucas",
        roleTitle = "Shopping and Restaurant Partner",
        shortDescription = "Real-world dining, cafés, boutiques, markets, and doctor visits.",
        personality = "Charismatic, practical, humorous, and great at roleplay",
        specialty = "Ordering Food, Shopping, Doctor Appointments & City Errands",
        teachingStyle = "Interactive real-life roleplay for dining, shopping, and daily services",
        primaryLanguageCode = "fr",
        supportedLanguageCodes = listOf("fr", "en", "es", "it", "de"),
        accent = "European / French Bilingual",
        recommendedLevels = listOf("Beginner", "Intermediate", "Advanced"),
        cefrRange = "A1–C1",
        voice = TutorVoice.PUCK,
        avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&auto=format&fit=crop&q=80",
        gender = "Male",
        favoriteTopics = listOf(CallMode.RESTAURANT, CallMode.SHOPPING, CallMode.MEDICAL_CALL, CallMode.ASKING_DIRECTIONS),
        sampleGreeting = "Bonjour and hello! I'm Lucas. Ready to order at a café, go shopping, or run city errands together?"
      )
    )

    fun findById(id: String?): AiConversationPartner {
      if (id.isNullOrBlank()) return ALL_PARTNERS.first()
      if (id.equals(CHLOE_PARTNER.id, ignoreCase = true) || id.equals(CHLOE_PARTNER.name, ignoreCase = true)) {
        return CHLOE_PARTNER
      }
      return ALL_PARTNERS.firstOrNull { it.id.equals(id, ignoreCase = true) }
        ?: ALL_PARTNERS.firstOrNull { it.name.equals(id, ignoreCase = true) }
        ?: ALL_PARTNERS.first()
    }

    fun forLanguage(languageCode: String): List<AiConversationPartner> {
      val code = languageCode.lowercase()
      val matching = ALL_PARTNERS.filter {
        it.primaryLanguageCode.equals(code, ignoreCase = true) ||
          it.supportedLanguageCodes.any { lang -> lang.equals(code, ignoreCase = true) }
      }
      return matching.ifEmpty { ALL_PARTNERS }
    }
  }
}

private val CallMode.Companion.DOCTOR_APPOINTMENTS_ALIAS: CallMode
  get() = CallMode.MEDICAL_CALL

enum class VoiceCallStatus {
  IDLE,
  CONNECTING,
  LISTENING,
  USER_SPEAKING,
  THINKING,
  TUTOR_SPEAKING,
  MUTED,
  INTERRUPTED,
  ENDED
}

data class VoiceCallTurn(
  val id: String,
  val speaker: MessageSender,
  val text: String,
  val phoneticGuide: String? = null,
  val translation: String? = null,
  val timestampEpoch: Long = System.currentTimeMillis(),
  val partnerId: String? = null,
  val partnerName: String? = null,
  val partnerVoiceId: String? = null,
  val inlineCorrection: String? = null,
  val naturalAlternative: String? = null,
  val pronunciationScore: Int? = null
)

data class CallLearningReport(
  val sessionId: String,
  val languageCode: String,
  val mode: CallMode,
  val durationSeconds: Int,
  val turnsCount: Int,
  val fluencyScore: Int,
  val vocabularyLearned: List<VocabItem> = emptyList(),
  val grammarCorrections: List<GrammarCorrection> = emptyList(),
  val overallFeedback: String = "",
  val xpEarned: Int = 50,
  val partnerId: String = "partner_emma",
  val partnerName: String = "Emma",
  val groupPartnerNames: List<String> = emptyList(),
  val cefrLevel: String = "B1",
  val practiceRecommendations: List<String> = emptyList()
)

data class SavedAiConversationRecord(
  val id: String,
  val partnerId: String,
  val partnerName: String,
  val groupPartnerIds: List<String> = emptyList(),
  val groupPartnerNames: List<String> = emptyList(),
  val languageCode: String,
  val languageName: String,
  val topicId: String,
  val topicTitle: String,
  val cefrLevel: String,
  val correctionModeId: String,
  val durationSeconds: Int,
  val timestampEpoch: Long,
  val turns: List<VoiceCallTurn>,
  val corrections: List<GrammarCorrection>,
  val vocabularyLearned: List<VocabItem>,
  val practiceRecommendations: List<String>,
  val summaryHeadline: String,
  val audioRecordingConsented: Boolean = false
)

data class VocabItem(
  val term: String,
  val phonetic: String,
  val translation: String,
  val exampleSentence: String
)

data class GrammarCorrection(
  val originalPhrase: String,
  val correctedPhrase: String,
  val explanation: String
)
