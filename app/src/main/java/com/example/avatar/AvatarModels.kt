package com.example.avatar

import com.squareup.moshi.Json

enum class AvatarProvider(val displayName: String, val providerDescription: String) {
  HEYGEN("LingoSphere HD Stream", "Ultra-low latency streaming avatar with realistic lip-sync and human facial expressions"),
  TAVUS("Interactive Video Engine", "Interactive digital replica with conversational video streaming and natural head movement")
}

enum class VideoCallStatus {
  IDLE,
  CONNECTING_AVATAR,
  CONNECTING_GEMINI,
  READY,
  LISTENING,
  THINKING,
  TUTOR_SPEAKING,
  MUTED,
  ENDED,
  CREDENTIALS_REQUIRED
}

enum class CefrLevel(val code: String, val title: String, val speed: Float) {
  A1("A1", "Beginner", 0.75f),
  A2("A2", "Elementary", 0.85f),
  B1("B1", "Intermediate", 1.0f),
  B2("B2", "Upper Intermediate", 1.05f),
  C1("C1", "Advanced", 1.15f),
  C2("C2", "Mastery / Native", 1.25f)
}

enum class TutorViseme(
  val openness: Float,       // Vertical jaw opening [0.0 - 1.0]
  val widthRatio: Float,     // Horizontal lip stretch factor [0.6 - 1.3]
  val rounding: Float,       // Lip pursing / rounding [0.0 - 1.0]
  val upperLipLift: Float,   // Elevation of top lip exposing teeth [0.0 - 1.0]
  val lowerLipDepress: Float // Drop of lower lip [0.0 - 1.0]
) {
  REST(0.00f, 1.00f, 0.00f, 0.00f, 0.00f),
  AA(0.85f, 1.05f, 0.10f, 0.45f, 0.80f),      // "Ah", "father", "car"
  EE(0.35f, 1.25f, 0.00f, 0.35f, 0.40f),      // "See", "meet", "key"
  OO(0.50f, 0.65f, 0.90f, 0.20f, 0.40f),      // "You", "two", "blue"
  OH(0.70f, 0.85f, 0.65f, 0.40f, 0.65f),      // "Go", "home", "boat"
  BMP(0.01f, 0.98f, 0.00f, 0.00f, 0.00f),     // "B", "M", "P" (lips closed firmly)
  FV(0.20f, 1.10f, 0.10f, 0.30f, 0.15f),      // "F", "V" (upper teeth on lower lip)
  NEUTRAL_TALK(0.45f, 1.02f, 0.20f, 0.25f, 0.45f) // Syllable transitional state
}

data class TutorPersona(
  val id: String,
  val name: String,
  val gender: String, // "Female" or "Male"
  val primaryLanguageCode: String,
  val accentDescription: String,
  val specialty: String,
  val teachingStyle: String,
  val bio: String,
  val heygenAvatarId: String,
  val heygenVoiceId: String,
  val tavusReplicaId: String,
  val avatarPreviewUrl: String,
  val defaultGreeting: String,
  val defaultGreetingPhonetics: String,
  val defaultGreetingTranslation: String,
  val skinToneHex: Long = 0xFFD89B7E,
  val lipToneHex: Long = 0xFFBD5E6A,
  val mouthNormalizedY: Float = 0.63f,
  val mouthNormalizedX: Float = 0.50f,
  val mouthNormalizedWidth: Float = 0.18f,
  val eyesNormalizedY: Float = 0.41f,
  val eyeDistanceNormalized: Float = 0.17f
) {
  companion object {
    val ALL_TUTORS = listOf(
      TutorPersona(
        id = "tutor_elena",
        name = "Elena Morales",
        gender = "Female",
        primaryLanguageCode = "es",
        accentDescription = "Madrid & Latin America Neutral",
        specialty = "Conversational Fluency, Accent Polish & Everyday Idioms",
        teachingStyle = "Warm, encouraging, and natural conversationalist",
        bio = "Certified language teacher with 7+ years of experience helping thousands of learners speak Spanish and English with native-level confidence.",
        heygenAvatarId = "Kristin_public_3_20240108",
        heygenVoiceId = "131a436e60e649a997323b6560b10223",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "¡Hola! Qué alegría conocerte hoy. ¿Qué te gustaría practicar en nuestra conversación?",
        defaultGreetingPhonetics = "oh-lah! keh ah-leh-gree-ah koh-noh-sehr-teh oy. keh teh goos-tah-ree-ah prahk-tee-kahr?",
        defaultGreetingTranslation = "Hello! Such a joy to meet you today. What would you like to practice in our conversation?",
        skinToneHex = 0xFFE8BCA7,
        lipToneHex = 0xFFC75D68,
        mouthNormalizedY = 0.62f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.18f,
        eyesNormalizedY = 0.40f,
        eyeDistanceNormalized = 0.16f
      ),
      TutorPersona(
        id = "tutor_chloe",
        name = "Chloé Laurent",
        gender = "Female",
        primaryLanguageCode = "fr",
        accentDescription = "Parisian French & Standard International",
        specialty = "French Liaisons, Pronunciation & Cultural Nuances",
        teachingStyle = "Patient, elegant, and attentive to fine pronunciation details",
        bio = "Parisian native passionate about making French feel effortless, melodic, and culturally authentic for learners of every stage.",
        heygenAvatarId = "ann_doctor_standing_front",
        heygenVoiceId = "fr_female_standard_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "Bonjour ! C'est un réel plaisir de vous rencontrer. Quel sujet vous passionne aujourd'hui ?",
        defaultGreetingPhonetics = "bohn-zhoor! seh tun ray-el pleh-zeer duh voo rahn-kohn-tray. kel soo-zheh voo pah-syon oh-zhoor-dwee?",
        defaultGreetingTranslation = "Hello! It is a real pleasure to meet you. What topic are you passionate about exploring today?",
        skinToneHex = 0xFFEAC5B0,
        lipToneHex = 0xFFC96E7E,
        mouthNormalizedY = 0.62f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.17f,
        eyesNormalizedY = 0.40f,
        eyeDistanceNormalized = 0.16f
      ),
      TutorPersona(
        id = "tutor_sophia",
        name = "Sophia Bianchi",
        gender = "Female",
        primaryLanguageCode = "it",
        accentDescription = "Florentine & Tuscan Italian",
        specialty = "Natural Speech Rhythm, Travel Italian & Gastronomy",
        teachingStyle = "Expressive, vibrant, and energetic communication coach",
        bio = "Florence native dedicated to teaching expressive Italian through food, travel scenarios, and lively discussions.",
        heygenAvatarId = "Angela-inblackskirt-20220820",
        heygenVoiceId = "it_female_standard_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "Ciao! Benvenuto. Sono felicissima di fare questa lezione faccia a faccia con te!",
        defaultGreetingPhonetics = "chow! ben-veh-noo-toh. soh-noh feh-lee-chee-see-mah dee fah-reh kwehs-tah leh-tsyoh-neh?",
        defaultGreetingTranslation = "Hi! Welcome. I am so thrilled to have this face-to-face lesson with you!",
        skinToneHex = 0xFFE0B296,
        lipToneHex = 0xFFBD5864,
        mouthNormalizedY = 0.64f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.175f,
        eyesNormalizedY = 0.42f,
        eyeDistanceNormalized = 0.17f
      ),
      TutorPersona(
        id = "tutor_mei",
        name = "Mei Lin",
        gender = "Female",
        primaryLanguageCode = "zh",
        accentDescription = "Standard Putonghua (Beijing/Shanghai)",
        specialty = "Tone Precision, Pinyin & Real-world Business Mandarin",
        teachingStyle = "Systematic, encouraging, and clear acoustic modeling",
        bio = "Master of Linguistics from Shanghai with a clear, standard Mandarin accent and structured pedagogical approach.",
        heygenAvatarId = "Dexter_Lawyer_Sitting_front",
        heygenVoiceId = "zh_female_standard_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "你好！很高兴能和你面对面交流。今天想聊些什么呢？",
        defaultGreetingPhonetics = "Nǐ hǎo! Hěn gāoxìng néng hé nǐ miànduìmiàn jiāoliú. Jīntiān xiǎng liáo xiē shénme ne?",
        defaultGreetingTranslation = "Hello! Very happy to converse face-to-face with you. What would you like to chat about today?",
        skinToneHex = 0xFFEAD2C1,
        lipToneHex = 0xFFC76974,
        mouthNormalizedY = 0.63f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.17f,
        eyesNormalizedY = 0.41f,
        eyeDistanceNormalized = 0.16f
      ),
      TutorPersona(
        id = "tutor_emma",
        name = "Emma Watson-Smith",
        gender = "Female",
        primaryLanguageCode = "en",
        accentDescription = "Received Pronunciation (British English)",
        specialty = "Advanced Academic English, Interview Prep & Intonation",
        teachingStyle = "Refined, precise, and articulate speaking coach",
        bio = "Cambridge-educated phonetics specialist helping global learners sound natural, polished, and confident in professional English.",
        heygenAvatarId = "Kristin_public_3_20240108",
        heygenVoiceId = "en_uk_female_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "Hello! It is wonderful to meet you. Shall we dive into today's conversation topic?",
        defaultGreetingPhonetics = "he-loh! it iz wun-duh-fuhl too meet yoo. shal wee dyve in-too tuh-dayz kon-vuh-say-shuhn?",
        defaultGreetingTranslation = "Hello! It is wonderful to meet you. Shall we dive into today's conversation topic?",
        skinToneHex = 0xFFECCAB7,
        lipToneHex = 0xFFC06B78,
        mouthNormalizedY = 0.63f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.18f,
        eyesNormalizedY = 0.40f,
        eyeDistanceNormalized = 0.17f
      ),
      TutorPersona(
        id = "tutor_carlos",
        name = "Carlos Rivera",
        gender = "Male",
        primaryLanguageCode = "es",
        accentDescription = "Latin American Conversational",
        specialty = "Practical Travel Spanish, Slang & Daily Socializing",
        teachingStyle = "Relaxed, humorous, and focused on spontaneous speech",
        bio = "Bilingual coach passionate about breaking the fear barrier so students can speak freely in any social setting.",
        heygenAvatarId = "Wayne_20240711",
        heygenVoiceId = "es_male_standard_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "¡Qué tal, amigo! Es un gusto saludarte. Cuéntame, ¿cómo ha estado tu día?",
        defaultGreetingPhonetics = "keh tahl, ah-mee-goh! ehs oon goos-toh sah-loo-dahr-teh. kwehn-tah-meh, koh-moh ah ehs-tah-doh?",
        defaultGreetingTranslation = "What's up, my friend! Great to greet you. Tell me, how has your day been?",
        skinToneHex = 0xFFCE9B7A,
        lipToneHex = 0xFFA05556,
        mouthNormalizedY = 0.65f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.19f,
        eyesNormalizedY = 0.42f,
        eyeDistanceNormalized = 0.18f
      ),
      TutorPersona(
        id = "tutor_alexander",
        name = "Alexander Vance",
        gender = "Male",
        primaryLanguageCode = "en",
        accentDescription = "General American (North American)",
        specialty = "American Idioms, Tech Discussions & Business Fluency",
        teachingStyle = "Direct, engaging, and highly practical feedback",
        bio = "Former broadcast journalist based in Chicago specializing in American colloquialisms and executive presentation skills.",
        heygenAvatarId = "josh_lite3_20230714",
        heygenVoiceId = "en_us_male_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "Hey there! Great to connect with you. Let's make today's practice fun and productive.",
        defaultGreetingPhonetics = "hay dhayr! grayt too kuh-nekt with yoo. lets mayk tuh-dayz prak-tis fun and pruh-duhk-tiv.",
        defaultGreetingTranslation = "Hey there! Great to connect with you. Let's make today's practice fun and productive.",
        skinToneHex = 0xFFDFC0A8,
        lipToneHex = 0xFFB36968,
        mouthNormalizedY = 0.64f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.19f,
        eyesNormalizedY = 0.41f,
        eyeDistanceNormalized = 0.17f
      ),
      TutorPersona(
        id = "tutor_kenji",
        name = "Kenji Sato",
        gender = "Male",
        primaryLanguageCode = "ja",
        accentDescription = "Standard Tokyo Japanese",
        specialty = "Keigo, Daily Conversational Politeness & Cultural Etiquette",
        teachingStyle = "Structured, polite, and encouraging with clear examples",
        bio = "Tokyo native and language mentor with 8 years of teaching Japanese for travel, work, and JLPT conversational proficiency.",
        heygenAvatarId = "Dexter_Lawyer_Sitting_front",
        heygenVoiceId = "ja_male_standard_01",
        tavusReplicaId = "r79e1c033f",
        avatarPreviewUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80",
        defaultGreeting = "こんにちは！お会いできて嬉しいです。今日はどんなことについて話しましょうか？",
        defaultGreetingPhonetics = "Konnichiwa! Oaidekite ureshii desu. Kyō wa donna koto ni tsuite hanashimashō ka?",
        defaultGreetingTranslation = "Hello! It is a pleasure to meet you. What would you like to speak about today?",
        skinToneHex = 0xFFD8B399,
        lipToneHex = 0xFFA45C60,
        mouthNormalizedY = 0.65f,
        mouthNormalizedX = 0.50f,
        mouthNormalizedWidth = 0.18f,
        eyesNormalizedY = 0.42f,
        eyeDistanceNormalized = 0.17f
      )
    )

    fun findTutorForLanguage(languageCode: String, preferFemale: Boolean = true): TutorPersona {
      val code = languageCode.lowercase()
      val matched = ALL_TUTORS.filter {
        it.primaryLanguageCode.equals(code, ignoreCase = true) &&
        (if (preferFemale) it.gender == "Female" else it.gender == "Male")
      }
      return matched.firstOrNull()
        ?: ALL_TUTORS.firstOrNull { it.primaryLanguageCode.equals(code, ignoreCase = true) }
        ?: ALL_TUTORS.first { it.id == "tutor_elena" }
    }
  }
}

// HeyGen Streaming API Request & Response Models
data class HeyGenNewSessionRequest(
  val quality: String = "medium",
  @Json(name = "avatar_name") val avatarName: String,
  @Json(name = "voice") val voice: HeyGenVoiceConfig? = null,
  val version: String = "v2"
)

data class HeyGenVoiceConfig(
  @Json(name = "voice_id") val voiceId: String,
  val rate: Float = 1.0f
)

data class HeyGenNewSessionResponse(
  val code: Int = 100,
  val data: HeyGenSessionData? = null,
  val message: String? = null
)

data class HeyGenSessionData(
  @Json(name = "session_id") val sessionId: String,
  val sdp: HeyGenSdpData? = null,
  @Json(name = "ice_servers") val iceServers: List<HeyGenIceServer>? = null,
  @Json(name = "access_token") val accessToken: String? = null,
  val url: String? = null
)

data class HeyGenSdpData(
  val type: String,
  val sdp: String
)

data class HeyGenIceServer(
  val urls: List<String>,
  val username: String? = null,
  val credential: String? = null
)

data class HeyGenStartSessionRequest(
  @Json(name = "session_id") val sessionId: String,
  val sdp: HeyGenSdpData
)

data class HeyGenTaskRequest(
  @Json(name = "session_id") val sessionId: String,
  val text: String,
  @Json(name = "task_type") val taskType: String = "repeat"
)

data class HeyGenStopSessionRequest(
  @Json(name = "session_id") val sessionId: String
)

data class TavusConversationRequest(
  @Json(name = "replica_id") val replicaId: String,
  @Json(name = "persona_id") val personaId: String? = null,
  @Json(name = "conversational_context") val conversationalContext: String? = null
)

data class TavusConversationResponse(
  @Json(name = "conversation_id") val conversationId: String? = null,
  @Json(name = "conversation_url") val conversationUrl: String? = null,
  val status: String? = null,
  val message: String? = null
)
