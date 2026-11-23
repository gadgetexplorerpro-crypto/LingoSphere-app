package com.example.data.repository

import com.example.ai.GeminiTutorService
import com.example.data.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Rich feedback model returned by Speech Coach, Speak the Scene, Quick Answer, Fluency Sprint,
 * Articulation Sprint, Shadow Speaking, and Dialogue Line evaluations.
 */
data class SpeakingDetailedAssessment(
  val isPassed: Boolean,
  val overallScore: Int,
  val clarityScore: Int = overallScore,
  val grammarScore: Int = overallScore,
  val vocabularyScore: Int = overallScore,
  val fluencyScore: Int = overallScore,
  val matchedWords: List<String> = emptyList(),
  val focusWordsOrSounds: List<String> = emptyList(),
  val feedbackHeadline: String,
  val grammarFeedback: String,
  val vocabularyFeedback: String,
  val pronunciationNote: String,
  val improvedSentence: String,
  val targetedFollowUpPrompt: String = "",
  val targetedFollowUpPhrase: String = "",
  val partnerContinuationLine: String = "",
  val wordsPerMinute: Int = 0,
  val wordCount: Int = 0
)

/**
 * Definition of one of the 10 real-life Roleplay Mission scenarios.
 */
data class RoleplayScenarioTemplate(
  val id: String,
  val title: String,
  val partnerRole: String,
  val settingSummary: String,
  val missionObjectives: List<String>,
  val openingsByLang: Map<String, String>,
  val openingTranslationEn: String,
  val sampleRepliesByLang: Map<String, String>,
  val followUpTurnsByLang: Map<String, List<String>>
)

/**
 * Definition of a visual scene for Game 4 — Speak the Scene (`ug_27_speak_the_picture`).
 */
data class SpeakSceneTemplate(
  val sceneId: String,
  val title: String,
  val illustrationKind: VisualIllustrationKind,
  val visualConceptId: String,
  val questionEn: String,
  val questionsByLang: Map<String, String>,
  val keyVocabByLang: Map<String, List<String>>,
  val modelDescriptionsByLang: Map<String, String>,
  val modelDescriptionEn: String
)

/**
 * Dedicated content generation, validation, and AI + acoustic/communicative evaluation engine
 * for all 10 LingoSphere Speaking Games (#24–#33).
 */
object SpeakingGamesContentAndEvaluationEngine {

  // ==================== 10 SPEAKING GAMES CURRICULUM TOPICS ====================

  val PRONUNCIATION_PRACTICE_TOPICS = listOf(
    "Clear Vowel Contrasts & Core Words",
    "Consonant Clusters & Crisp Endings",
    "Multi-Syllable Word Stress Patterns",
    "Rolled, Trilled & Liquid Consonants",
    "Silent Letters & Nasal Vowels",
    "Palatal & Sibilant Sound Distinctions",
    "Travel & Hospitality Key Pronunciation",
    "Dining & Culinary Pronunciation",
    "Professional & Workplace Vocabulary",
    "Academic & Expressive Multi-Syllable Words"
  )

  val REPEAT_AFTER_ME_TOPICS = listOf(
    "Polite Greetings & Daily Courtesies",
    "Ordering Food & Drinks Naturally",
    "Asking for Travel & Transit Help",
    "Describing Daily Routines & Habits",
    "Making Plans with Friends & Colleagues",
    "Shopping, Sizes & Payment Sentences",
    "Sharing Past Experiences & Stories",
    "Expressing Future Plans & Goals",
    "Handling Appointments & Reservations",
    "Expressing Nuanced Opinions & Advice"
  )

  val ARTICULATION_SPRINT_TOPICS = listOf(
    "R vs L Liquid Consonant Agility",
    "S vs SH Sibilant Clarity Drills",
    "P, B, T, D Plosive Precision",
    "TR, DR, GR Consonant Cluster Sprints",
    "F, V & W Labial Articulation",
    "Rapid Vowel Alternation Twisters",
    "Double Consonant & Rhythm Twisters",
    "Connected Syllable Speed Drills",
    "High-Tempo Classic Tongue Twisters",
    "Master Articulation Endurance Sprint"
  )

  val SPEAK_THE_SCENE_TOPICS = listOf(
    "Family & Friends Sharing Dinner",
    "Busy Morning Coffee Shop Scene",
    "Airport Terminal & Boarding Gate",
    "Professional Job Interview Office",
    "Downtown City Street & Commuters",
    "Park Gathering & Outdoor Leisure",
    "Morning Kitchen Breakfast Preparation",
    "Doctor's Clinic & Health Checkup",
    "Hotel Reception & Check-In Desk",
    "University Campus & Study Group"
  )

  val QUICK_ANSWER_TOPICS = listOf(
    "Morning Routine & Breakfast Questions",
    "Weekend Hobbies & Free Time",
    "Favorite Foods, Cooking & Dining",
    "Daily Commute & Transportation",
    "Weather, Seasons & Clothing Choices",
    "Work, Study & Daily Priorities",
    "Travel Preferences & Dream Destinations",
    "Fitness, Health & Relaxation Habits",
    "Friends, Family & Social Celebrations",
    "Personal Goals & Everyday Decisions"
  )

  val SPEECH_COACH_TOPICS = listOf(
    "Introducing Yourself & Your Background",
    "Describing Your Ideal Day Off",
    "Talking About a Personal Goal",
    "Recommending a Favorite Local Place",
    "Explaining a Skill You Are Learning",
    "Sharing a Memorable Travel Moment",
    "Describing Your Work or Study Routine",
    "Comparing City Life and Nature",
    "Discussing Healthy Daily Habits",
    "Reflecting on an Achievement"
  )

  val FLUENCY_SPRINT_TOPICS = listOf(
    "Describe Your Morning Routine",
    "Talk About Your Favorite Meal",
    "Describe Your Hometown or Neighborhood",
    "Explain How You Spend Your Weekends",
    "Talk About a Trip You Enjoyed",
    "Describe a Person Who Inspires You",
    "Explain Your Favorite Way to Exercise",
    "Talk About a Book, Movie, or Show",
    "Describe Your Ideal Workspace",
    "Share Your Plans for the Coming Year"
  )

  val SHADOW_SPEAKING_TOPICS = listOf(
    "Natural Greeting Cadence & Linking",
    "Café & Restaurant Connected Speech",
    "Question Intonation & Rising Pitch",
    "Smooth Connector Phrases in Conversation",
    "Expressive Emotion & Emphasis Patterns",
    "Travel Announcements & Polite Requests",
    "Storytelling Pauses & Thought Groups",
    "Professional Meeting Rhythm & Pacing",
    "Fast Everyday Native Expressions",
    "Mastery Prosody & Natural Flow"
  )

  val DIALOGUE_LINE_TOPICS = listOf(
    "Greeting a New Neighbor",
    "Ordering at a Local Bakery & Café",
    "Asking a Colleague for Help",
    "Making Weekend Dinner Plans",
    "Checking In at a Train Station",
    "Returning an Item at a Store",
    "Scheduling a Doctor's Visit",
    "Welcoming a Guest to Your City",
    "Discussing a Team Project Update",
    "Resolving a Travel Delay Politely"
  )

  val ROLEPLAY_10_SCENARIOS: List<RoleplayScenarioTemplate> = listOf(
    RoleplayScenarioTemplate(
      id = "shopping",
      title = "Shopping",
      partnerRole = "Store Associate",
      settingSummary = "Find the right size, ask about the price, and choose how to pay.",
      missionObjectives = listOf(
        "Greet the associate and ask for an item or size",
        "Ask about the price or color options",
        "Confirm how you would like to pay"
      ),
      openingsByLang = mapOf(
        "en" to "Welcome to our boutique! Are you looking for a specific size or style today?",
        "es" to "¡Bienvenido a nuestra tienda! ¿Busca alguna talla o estilo en especial hoy?",
        "fr" to "Bienvenue dans notre boutique ! Cherchez-vous une taille ou un style particulier ?",
        "de" to "Willkommen in unserem Geschäft! Suchen Sie heute eine bestimmte Größe oder Farbe?",
        "ja" to "いらっしゃいませ！本日どのようなサイズやスタイルをお探しですか？",
        "ko" to "어서 오세요! 오늘 찾으시는 사이즈나 스타일이 있으신가요?",
        "it" to "Benvenuto nel nostro negozio! Cerca una taglia o uno stile particolare oggi?",
        "pt" to "Bem-vindo à nossa loja! Está procurando algum tamanho ou estilo específico hoje?"
      ),
      openingTranslationEn = "Welcome to our boutique! Are you looking for a specific size or style today?",
      sampleRepliesByLang = mapOf(
        "en" to "Hello! I am looking for a medium jacket in blue, please.",
        "es" to "¡Hola! Busco una chaqueta talla mediana en color azul, por favor.",
        "fr" to "Bonjour ! Je cherche une veste taille moyenne en bleu, s'il vous plaît.",
        "de" to "Hallo! Ich suche eine blaue Jacke in Größe M, bitte.",
        "ja" to "こんにちは！青いジャケットのMサイズを探しています。",
        "ko" to "안녕하세요! 파란색 중간 사이즈 자켓을 찾고 있어요.",
        "it" to "Ciao! Cerco una giacca blu taglia media, per favore.",
        "pt" to "Olá! Estou procurando uma jaqueta azul tamanho médio, por favor."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "We have that in medium right here! It is on sale for forty dollars. Would you like to try it on?",
          "It fits great! Would you like to pay by card or cash at the register?"
        ),
        "es" to listOf(
          "¡Tenemos esa talla aquí mismo! Está en oferta por cuarenta euros. ¿Quiere probársela?",
          "¡Le queda excelente! ¿Desea pagar con tarjeta o en efectivo?"
        ),
        "fr" to listOf(
          "Nous l'avons en taille moyenne juste ici ! Elle est à quarante euros. Voulez-vous l'essayer ?",
          "Elle vous va très bien ! Souhaitez-vous payer par carte ou en espèces ?"
        ),
        "de" to listOf(
          "Wir haben Größe M genau hier! Sie kostet vierzig Euro. Möchten Sie sie anprobieren?",
          "Sie passt wunderbar! Möchten Sie mit Karte oder bar bezahlen?"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "restaurant_ordering",
      title = "Restaurant Ordering",
      partnerRole = "Restaurant Server",
      settingSummary = "Order a main dish, choose a drink, and ask for the bill politely.",
      missionObjectives = listOf(
        "Order a meal or ask for today's recommendation",
        "Select a drink or side dish",
        "Ask for the bill at the end of the meal"
      ),
      openingsByLang = mapOf(
        "en" to "Good evening! Here is the menu. Would you like to hear today's specials or start with a drink?",
        "es" to "¡Buenas noches! Aquí tiene el menú. ¿Le gustaría conocer el plato del día o pedir una bebida?",
        "fr" to "Bonsoir ! Voici le menu. Souhaitez-vous connaître le plat du jour ou commencer par une boisson ?",
        "de" to "Guten Abend! Hier ist die Speisekarte. Möchten Sie zuerst etwas trinken oder das Tagesgericht hören?",
        "ja" to "こんばんは！メニューをどうぞ。まずお飲み物になさいますか、それとも本日のおすすめをご案内しましょうか？",
        "ko" to "안녕하세요! 메뉴판 여기 있습니다. 먼저 음료를 주문하시겠어요, 아니면 오늘의 추천 메뉴를 들어보시겠어요?",
        "it" to "Buonasera! Ecco il menu. Desidera iniziare con qualcosa da bere o scoprire il piatto del giorno?",
        "pt" to "Boa noite! Aqui está o cardápio. Gostaria de pedir uma bebida primeiro ou ouvir o prato do dia?"
      ),
      openingTranslationEn = "Good evening! Here is the menu. Would you like to hear today's specials or start with a drink?",
      sampleRepliesByLang = mapOf(
        "en" to "Good evening! I would like the grilled salmon with rice and a glass of sparkling water, please.",
        "es" to "¡Buenas noches! Quisiera el salmón a la parrilla con arroz y un vaso de agua con gas, por favor.",
        "fr" to "Bonsoir ! Je voudrais le saumon grillé avec du riz et une eau gazeuse, s'il vous plaît.",
        "de" to "Guten Abend! Ich hätte gern den gegrillten Lachs mit Reis und ein Mineralwasser, bitte.",
        "ja" to "こんばんは！焼き鮭とご飯、そして炭酸水を一杯お願いします。",
        "ko" to "안녕하세요! 구운 연어와 밥, 그리고 탄산수 한 잔 부탁합니다.",
        "it" to "Buonasera! Vorrei il salmone alla griglia con riso e un'acqua frizzante, per favore.",
        "pt" to "Boa noite! Eu gostaria do salmão grelhado com arroz e uma água com gás, por favor."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "Excellent choice! Would you like a fresh salad or soup on the side?",
          "Here is your meal. Please let me know when you are ready for dessert or the bill!"
        ),
        "es" to listOf(
          "¡Excelente elección! ¿Le gustaría acompañar su plato con ensalada o sopa?",
          "Aquí tiene su pedido. ¡Avíseme cuando desee el postre o la cuenta!"
        ),
        "fr" to listOf(
          "Excellent choix ! Voulez-vous une salade fraîche ou une soupe en accompagnement ?",
          "Voici votre plat. Dites-moi quand vous souhaiterez un dessert ou l'addition !"
        ),
        "de" to listOf(
          "Ausgezeichnete Wahl! Möchten Sie dazu einen frischen Salat oder eine Suppe?",
          "Hier ist Ihr Essen. Sagen Sie mir einfach Bescheid, wenn Sie die Rechnung wünschen!"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "job_interview",
      title = "Job Interview",
      partnerRole = "Hiring Manager",
      settingSummary = "Introduce your experience, share a key strength, and ask a question about the team.",
      missionObjectives = listOf(
        "Introduce your background and interest in the role",
        "Describe one key professional strength or project",
        "Ask a thoughtful question about the team or goals"
      ),
      openingsByLang = mapOf(
        "en" to "Thank you for joining us today! To begin, could you tell me a little about your background and why this role interests you?",
        "es" to "¡Gracias por acompañarnos hoy! Para empezar, ¿podría contarme sobre su experiencia y por qué le interesa este puesto?",
        "fr" to "Merci d'être avec nous aujourd'hui ! Pour commencer, pouvez-vous me parler de votre parcours et de votre intérêt pour ce poste ?",
        "de" to "Vielen Dank für Ihr Kommen! Können Sie mir zunächst etwas über Ihren Werdegang erzählen und warum Sie diese Stelle interessiert?",
        "ja" to "本日はお越しいただきありがとうございます。まずはご経歴と、この職種に関心を持たれた理由を教えていただけますか？",
        "ko" to "오늘 면접에 참석해 주셔서 감사합니다. 먼저 간단한 자기소개와 이 직무에 지원하신 동기를 말씀해 주시겠어요?"
      ),
      openingTranslationEn = "Thank you for joining us today! Could you tell me about your background and why this role interests you?",
      sampleRepliesByLang = mapOf(
        "en" to "Thank you for having me! I have three years of experience in project coordination and I love solving problems with collaborative teams.",
        "es" to "¡Gracias por recibirme! Tengo tres años de experiencia en coordinación de proyectos y me encanta colaborar en equipo.",
        "fr" to "Merci de me recevoir ! J'ai trois ans d'expérience en gestion de projet et j'aime travailler en équipe.",
        "de" to "Vielen Dank für die Einladung! Ich habe drei Jahre Erfahrung in der Projektkoordination und arbeite sehr gern im Team."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "That sounds very relevant. What would you say is your greatest strength when working under a tight deadline?",
          "We value that approach. Do you have any questions for me about our team or upcoming projects?"
        ),
        "es" to listOf(
          "Eso suena muy relevante. ¿Cuál diría que es su mayor fortaleza al trabajar con plazos ajustados?",
          "Valoramos mucho esa actitud. ¿Tiene alguna pregunta sobre nuestro equipo o los próximos proyectos?"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "doctor_appointment",
      title = "Doctor Appointment",
      partnerRole = "Clinic Doctor",
      settingSummary = "Explain how you are feeling, how long symptoms lasted, and ask about recovery.",
      missionObjectives = listOf(
        "Describe your main symptom clearly",
        "Mention when the symptom started",
        "Ask about medicine or rest recommendations"
      ),
      openingsByLang = mapOf(
        "en" to "Hello, please have a seat. What brings you to the clinic today, and how have you been feeling?",
        "es" to "Hola, tome asiento por favor. ¿Qué le trae a la consulta hoy y cómo se ha sentido?",
        "fr" to "Bonjour, asseyez-vous je vous prie. Qu'est-ce qui vous amène au cabinet aujourd'hui ?",
        "de" to "Guten Tag, bitte nehmen Sie Platz. Was führt Sie heute zu mir und wie fühlen Sie sich?"
      ),
      openingTranslationEn = "Hello, please have a seat. What brings you to the clinic today, and how have you been feeling?",
      sampleRepliesByLang = mapOf(
        "en" to "Hello doctor. I have had a sore throat and a mild headache since yesterday morning.",
        "es" to "Hola doctor. Tengo dolor de garganta y un leve dolor de cabeza desde ayer por la mañana.",
        "fr" to "Bonjour docteur. J'ai mal à la gorge et un léger mal de tête depuis hier matin.",
        "de" to "Guten Tag Doktor. Ich habe seit gestern Morgen Halsschmerzen und leichte Kopfschmerzen."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "I see. Have you had a fever or any trouble sleeping at night?",
          "I will prescribe a soothing throat spray. Make sure to drink warm fluids and rest for two days."
        ),
        "es" to listOf(
          "Entiendo. ¿Ha tenido fiebre o dificultad para descansar por la noche?",
          "Le recetaré un tratamiento suave. Beba líquidos tibios y descanse durante dos días."
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "airport",
      title = "Airport Check-In",
      partnerRole = "Airline Agent",
      settingSummary = "Check in for your flight, choose a window or aisle seat, and check your luggage.",
      missionObjectives = listOf(
        "Present your flight destination and passport",
        "Request a window or aisle seat",
        "Confirm how many bags you are checking"
      ),
      openingsByLang = mapOf(
        "en" to "Good morning! Where are you flying today, and may I see your passport and booking confirmation?",
        "es" to "¡Buenos días! ¿A dónde vuela hoy? ¿Me permite ver su pasaporte y su reserva?",
        "fr" to "Bonjour ! Quelle est votre destination aujourd'hui ? Puis-je voir votre passeport ?",
        "de" to "Guten Morgen! Wohin fliegen Sie heute? Darf ich bitte Ihren Reisepass sehen?"
      ),
      openingTranslationEn = "Good morning! Where are you flying today, and may I see your passport?",
      sampleRepliesByLang = mapOf(
        "en" to "Good morning! Here is my passport. I am flying to Madrid and I would love a window seat, please.",
        "es" to "¡Buenos días! Aquí está mi pasaporte. Vuelo a Madrid y me gustaría un asiento de ventanilla, por favor.",
        "fr" to "Bonjour ! Voici mon passeport. Je prends le vol pour Madrid et je préférerais un siège côté hublot.",
        "de" to "Guten Morgen! Hier ist mein Reisepass. Ich fliege nach Madrid und hätte gern einen Fensterplatz."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "Window seat 14A is available! Are you checking any suitcases today, or just carrying hand luggage?",
          "Here is your boarding pass for Gate B12. Boarding starts in forty minutes. Have a pleasant flight!"
        ),
        "es" to listOf(
          "¡El asiento de ventanilla 14A está disponible! ¿Va a facturar alguna maleta hoy?",
          "Aquí tiene su tarjeta de embarque para la puerta B12. ¡Que tenga un excelente vuelo!"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "hotel_check_in",
      title = "Hotel Check-In",
      partnerRole = "Hotel Receptionist",
      settingSummary = "Confirm your room reservation, ask about breakfast hours, and get your room key.",
      missionObjectives = listOf(
        "Give your name and confirm your reservation",
        "Ask what time breakfast is served",
        "Ask about Wi-Fi or elevator access"
      ),
      openingsByLang = mapOf(
        "en" to "Welcome to the Grand Harbor Hotel! Do you have a reservation checking in today?",
        "es" to "¡Bienvenido al Hotel Gran Puerto! ¿Tiene una reserva para registrarse hoy?",
        "fr" to "Bienvenue à l'Hôtel Grand Port ! Avez-vous une réservation pour aujourd'hui ?",
        "de" to "Willkommen im Grand Harbor Hotel! Haben Sie heute eine Reservierung bei uns?"
      ),
      openingTranslationEn = "Welcome to the Grand Harbor Hotel! Do you have a reservation checking in today?",
      sampleRepliesByLang = mapOf(
        "en" to "Hello! Yes, I have a reservation under my name for three nights with a quiet room.",
        "es" to "¡Hola! Sí, tengo una reserva a mi nombre por tres noches en una habitación tranquila.",
        "fr" to "Bonjour ! Oui, j'ai une réservation à mon nom pour trois nuits dans une chambre calme.",
        "de" to "Hallo! Ja, ich habe eine Reservierung auf meinen Namen für drei Nächte."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "I found your booking for a deluxe room on the fourth floor! Would you like to know our breakfast schedule?",
          "Breakfast is served from seven to ten on the terrace, and here is your key card. Enjoy your stay!"
        ),
        "es" to listOf(
          "¡Encontré su reserva en el cuarto piso! ¿Desea conocer el horario del desayuno?",
          "El desayuno se sirve de siete a diez en la terraza. Aquí tiene su llave. ¡Disfrute su estancia!"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "meeting_new_people",
      title = "Meeting New People",
      partnerRole = "Friendly Local at a Social Event",
      settingSummary = "Introduce yourself, talk about your hobbies, and exchange recommendations.",
      missionObjectives = listOf(
        "Introduce yourself and say where you are from",
        "Share a hobby or interest you enjoy",
        "Ask your new friend about their favorite local spot"
      ),
      openingsByLang = mapOf(
        "en" to "Hi there! I don't think we have met yet—I'm Clara. What brings you to the neighborhood gathering today?",
        "es" to "¡Hola! Creo que aún no nos conocemos, soy Clara. ¿Qué te trae hoy a este encuentro del barrio?",
        "fr" to "Salut ! Je crois qu'on ne se connaît pas encore, je m'appelle Clara. Qu'est-ce qui t'amène ici aujourd'hui ?",
        "de" to "Hallo! Ich glaube, wir kennen uns noch nicht—ich bin Clara. Was führt dich heute hierher?"
      ),
      openingTranslationEn = "Hi there! I'm Clara. What brings you to the gathering today?",
      sampleRepliesByLang = mapOf(
        "en" to "Nice to meet you, Clara! I just moved here recently and I love meeting people who enjoy music and coffee.",
        "es" to "¡Mucho gusto, Clara! Me mudé hace poco y me encanta conocer gente a la que le guste la música y el café.",
        "fr" to "Enchanté, Clara ! Je viens d'emménager ici et j'adore rencontrer des gens qui aiment la musique.",
        "de" to "Freut mich, Clara! Ich bin erst vor Kurzem hierhergezogen und lerne gern neue Leute kennen."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "Welcome to the city! What kind of activities do you usually enjoy doing on the weekend?",
          "That sounds awesome! We should grab coffee at the plaza café this Saturday."
        ),
        "es" to listOf(
          "¡Bienvenido a la ciudad! ¿Qué tipo de actividades sueles disfrutar los fines de semana?",
          "¡Qué genial! Deberíamos tomar un café en la plaza este sábado."
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "asking_for_directions",
      title = "Asking for Directions",
      partnerRole = "Helpful City Resident",
      settingSummary = "Ask how to reach the central museum or train station and confirm walking time.",
      missionObjectives = listOf(
        "Politely get the person's attention and ask where a landmark is",
        "Ask whether it is better to walk or take the metro",
        "Repeat the key direction and thank them"
      ),
      openingsByLang = mapOf(
        "en" to "Hello! You look like you are checking the city map—can I help you find a street or station?",
        "es" to "¡Hola! Veo que estás mirando el mapa de la ciudad, ¿te puedo ayudar a encontrar alguna calle o estación?",
        "fr" to "Bonjour ! Vous avez l'air de chercher votre chemin sur la carte, puis-je vous aider ?",
        "de" to "Hallo! Sie schauen gerade auf den Stadtplan—kann ich Ihnen helfen, den Weg zu finden?"
      ),
      openingTranslationEn = "Hello! Can I help you find a street or station on the map?",
      sampleRepliesByLang = mapOf(
        "en" to "Excuse me, yes please! Could you tell me how to get to the Central Art Museum from here?",
        "es" to "¡Disculpe, sí por favor! ¿Podría decirme cómo llegar al Museo Central de Arte desde aquí?",
        "fr" to "Excusez-moi, oui s'il vous plaît ! Pourriez-vous m'indiquer comment aller au Musée d'Art ?",
        "de" to "Entschuldigung, ja bitte! Können Sie mir sagen, wie ich von hier zum Kunstmuseum komme?"
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "Sure! Walk straight for two blocks, turn left at the fountain, and it is right across from the park.",
          "It only takes about seven minutes on foot. Enjoy the exhibition!"
        ),
        "es" to listOf(
          "¡Claro! Camina recto dos cuadras, gira a la izquierda en la fuente y está frente al parque.",
          "Solo toma unos siete minutos a pie. ¡Que disfrutes la exposición!"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "workplace_conversation",
      title = "Workplace Conversation",
      partnerRole = "Project Colleague",
      settingSummary = "Share a project update, coordinate a deadline, and schedule a quick review meeting.",
      missionObjectives = listOf(
        "Share your current progress on the project",
        "Suggest a time to review the final draft together",
        "Confirm the next step for the team"
      ),
      openingsByLang = mapOf(
        "en" to "Hi! Do you have a quick minute to update me on how the client presentation is coming along?",
        "es" to "¡Hola! ¿Tienes un minuto para contarme cómo avanza la presentación para el cliente?",
        "fr" to "Salut ! As-tu une minute pour me dire comment avance la présentation pour le client ?",
        "de" to "Hallo! Hast du kurz Zeit mir zu sagen, wie die Kundenpräsentation vorankommt?"
      ),
      openingTranslationEn = "Hi! Do you have a minute to update me on how the client presentation is coming along?",
      sampleRepliesByLang = mapOf(
        "en" to "Yes, of course! I finished the main slides this morning and I am reviewing the budget numbers now.",
        "es" to "¡Sí, claro! Terminé las diapositivas principales esta mañana y ahora estoy revisando los datos del presupuesto.",
        "fr" to "Oui, bien sûr ! J'ai terminé les diapositives principales ce matin et je vérifie les chiffres maintenant.",
        "de" to "Ja, natürlich! Ich habe die wichtigsten Folien heute Morgen fertiggestellt und prüfe jetzt die Zahlen."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "That is great progress! When would be a good time for us to review the slides together before tomorrow?",
          "Three o'clock works for me. I will send a calendar invite now—thanks for staying on top of it!"
        ),
        "es" to listOf(
          "¡Qué buen avance! ¿A qué hora te parece bien que revisemos juntos el borrador hoy?",
          "Las tres de la tarde me viene perfecto. ¡Ahora mismo envío la invitación al calendario!"
        )
      )
    ),
    RoleplayScenarioTemplate(
      id = "university_admission",
      title = "University Admission",
      partnerRole = "Academic Advisor",
      settingSummary = "Discuss your chosen field of study, academic goals, and campus course schedule.",
      missionObjectives = listOf(
        "Explain which subject or degree program you want to study",
        "Share your academic or career motivation",
        "Ask about seminar classes or campus resources"
      ),
      openingsByLang = mapOf(
        "en" to "Welcome to the university admissions office! Which program are you applying for, and what inspired your choice?",
        "es" to "¡Bienvenido a la oficina de admisiones de la universidad! ¿A qué programa postula y qué inspiró su elección?",
        "fr" to "Bienvenue au bureau des admissions de l'université ! Quel programme souhaitez-vous intégrer ?",
        "de" to "Willkommen in der Studienberatung der Universität! Für welchen Studiengang interessieren Sie sich?"
      ),
      openingTranslationEn = "Welcome to the university admissions office! Which program are you applying for?",
      sampleRepliesByLang = mapOf(
        "en" to "Thank you! I am applying for the International Communications program because I want to work in global media.",
        "es" to "¡Gracias! Postulo al programa de Comunicación Internacional porque quiero trabajar en medios globales.",
        "fr" to "Merci ! Je postule au programme de communication internationale car je souhaite travailler dans les médias.",
        "de" to "Vielen Dank! Ich bewerbe mich für Internationale Kommunikation, weil ich im Medienbereich arbeiten möchte."
      ),
      followUpTurnsByLang = mapOf(
        "en" to listOf(
          "That is a wonderful field! Are you interested in joining our research seminars or language exchange labs?",
          "We would love to have you in the fall cohort. Let's finalize your course registration!"
        ),
        "es" to listOf(
          "¡Es una carrera excelente! ¿Le interesaría participar en nuestros seminarios de investigación o intercambios?",
          "Nos encantará contar con usted este semestre. ¡Vamos a completar su inscripción!"
        )
      )
    )
  )

  fun getScenarioForLevel(levelNumber: Int): RoleplayScenarioTemplate {
    val idx = (levelNumber - 1).coerceAtLeast(0) % ROLEPLAY_10_SCENARIOS.size
    return ROLEPLAY_10_SCENARIOS[idx]
  }

  // ==================== RICH SCENE CATALOG FOR GAME 4 (SPEAK THE SCENE) ====================

  val SPEAK_THE_SCENE_TEMPLATES: List<SpeakSceneTemplate> = listOf(
    SpeakSceneTemplate(
      sceneId = "family_dinner",
      title = "Family & Friends Dinner Table",
      illustrationKind = VisualIllustrationKind.RESTAURANT_DINING_SCENE,
      visualConceptId = "plate",
      questionEn = "What are the people doing at the dinner table?",
      questionsByLang = mapOf(
        "en" to "What are the people doing at the dinner table?",
        "es" to "¿Qué están haciendo las personas en la mesa durante la cena?",
        "fr" to "Que font les personnes autour de la table du dîner ?",
        "de" to "Was machen die Menschen gemeinsam am Esstisch?",
        "ja" to "夕食のテーブルで人々は何をしていますか？",
        "ko" to "저녁 식탁에서 사람들이 무엇을 하고 있나요?",
        "it" to "Cosa stanno facendo le persone a tavola?",
        "pt" to "O que as pessoas estão fazendo na mesa de jantar?"
      ),
      keyVocabByLang = mapOf(
        "en" to listOf("eating dinner", "sharing food", "talking", "table", "plates", "smiling"),
        "es" to listOf("cenando", "compartiendo comida", "hablando", "mesa", "platos", "familia"),
        "fr" to listOf("dîner", "partager un repas", "discuter", "table", "assiettes", "famille"),
        "de" to listOf("Abendessen", "zusammen essen", "sprechen", "Tisch", "Teller", "Familie"),
        "ja" to listOf("夕食を食べる", "話す", "テーブル", "料理", "家族"),
        "ko" to listOf("저녁 식사", "이야기하다", "식탁", "음식", "가족"),
        "it" to listOf("cenare", "mangiare insieme", "parlare", "tavolo", "piatti"),
        "pt" to listOf("jantar", "conversar", "mesa", "pratos", "família")
      ),
      modelDescriptionsByLang = mapOf(
        "en" to "The family is sitting around the table eating a warm dinner and talking happily together.",
        "es" to "La familia está sentada alrededor de la mesa cenando y conversando alegremente.",
        "fr" to "La famille est assise autour de la table pour partager un bon dîner et discuter ensemble.",
        "de" to "Die Familie sitzt zusammen am Tisch, isst zu Abend und unterhält sich freundlich.",
        "ja" to "家族がテーブルを囲んで温かい夕食を食べながら楽しく話しています。",
        "ko" to "가족들이 식탁에 둘러앉아 따뜻한 저녁을 먹으며 즐겁게 이야기하고 있습니다.",
        "it" to "La famiglia è seduta intorno al tavolo a cenare e chiacchierare insieme.",
        "pt" to "A família está sentada ao redor da mesa jantando e conversando alegremente."
      ),
      modelDescriptionEn = "The family is sitting around the table eating a warm dinner and talking happily together."
    ),
    SpeakSceneTemplate(
      sceneId = "coffee_shop_conversation",
      title = "Morning Café & Coffee Counter",
      illustrationKind = VisualIllustrationKind.COFFEE_SHOP_SCENE,
      visualConceptId = "cup",
      questionEn = "Describe what is happening inside the coffee shop.",
      questionsByLang = mapOf(
        "en" to "Describe what is happening inside the coffee shop.",
        "es" to "Describe qué está pasando dentro de la cafetería.",
        "fr" to "Décrivez ce qui se passe dans le café.",
        "de" to "Beschreiben Sie, was im Café passiert.",
        "ja" to "カフェの中で何が起きているか説明してください。",
        "ko" to "카페 안에서 무슨 일이 일어나고 있는지 설명해 보세요.",
        "it" to "Descrivi cosa sta succedendo dentro la caffetteria.",
        "pt" to "Descreva o que está acontecendo dentro da cafeteria."
      ),
      keyVocabByLang = mapOf(
        "en" to listOf("ordering coffee", "barista", "warm cup", "counter", "chatting", "pastry"),
        "es" to listOf("pidiendo café", "barista", "taza caliente", "mostrador", "charlando", "panadería"),
        "fr" to listOf("commander un café", "serveur", "tasse chaude", "comptoir", "discuter", "croissant"),
        "de" to listOf("Kaffee bestellen", "Tasse", "Tresen", "trinken", "unterhalten", "Gebäck")
      ),
      modelDescriptionsByLang = mapOf(
        "en" to "Customers are ordering fresh coffee at the counter while friends drink warm espresso at a table.",
        "es" to "Los clientes están pidiendo café recién hecho en el mostrador mientras unos amigos conversan en la mesa.",
        "fr" to "Des clients commandent du café au comptoir pendant que des amis discutent autour d'une tasse chaude.",
        "de" to "Gäste bestellen frischen Kaffee am Tresen, während Freunde gemütlich an einem Tisch sitzen."
      ),
      modelDescriptionEn = "Customers are ordering fresh coffee at the counter while friends drink warm espresso at a table."
    ),
    SpeakSceneTemplate(
      sceneId = "airport_boarding",
      title = "Airport Departure Hall",
      illustrationKind = VisualIllustrationKind.AIRPORT_TERMINAL_SCENE,
      visualConceptId = "suitcase",
      questionEn = "What are the travelers doing at the airport terminal?",
      questionsByLang = mapOf(
        "en" to "What are the travelers doing at the airport terminal?",
        "es" to "¿Qué están haciendo los viajeros en la terminal del aeropuerto?",
        "fr" to "Que font les voyageurs dans le terminal de l'aéroport ?",
        "de" to "Was machen die Reisenden im Flughafenterminal?"
      ),
      keyVocabByLang = mapOf(
        "en" to listOf("travelers", "suitcase", "departure board", "boarding gate", "passport", "flight"),
        "es" to listOf("viajeros", "maleta", "pantalla de vuelos", "puerta de embarque", "pasaporte", "vuelo"),
        "fr" to listOf("voyageurs", "valise", "tableau des départs", "porte d'embarquement", "passeport", "vol"),
        "de" to listOf("Reisende", "Koffer", "Abflugtafel", "Gate", "Reisepass", "Flug")
      ),
      modelDescriptionsByLang = mapOf(
        "en" to "Travelers with suitcases are checking the flight departure board and walking toward their boarding gate.",
        "es" to "Los viajeros con maletas están mirando la pantalla de salidas y caminando hacia su puerta de embarque.",
        "fr" to "Les voyageurs avec leurs valises regardent le tableau des départs et marchent vers la porte d'embarquement.",
        "de" to "Reisende mit Koffern prüfen die Abflugtafel und gehen zu ihrem Flugsteig."
      ),
      modelDescriptionEn = "Travelers with suitcases are checking the flight departure board and walking toward their boarding gate."
    ),
    SpeakSceneTemplate(
      sceneId = "job_interview_office",
      title = "Office Meeting & Interview",
      illustrationKind = VisualIllustrationKind.JOB_INTERVIEW_SCENE,
      visualConceptId = "laptop",
      questionEn = "What are the two professionals doing in the office?",
      questionsByLang = mapOf(
        "en" to "What are the two professionals doing in the office?",
        "es" to "¿Qué están haciendo los dos profesionales en la oficina?",
        "fr" to "Que font les deux professionnels dans le bureau ?",
        "de" to "Was machen die beiden Berufstätigen im Büro?"
      ),
      keyVocabByLang = mapOf(
        "en" to listOf("interview", "discussing", "desk", "laptop", "resume", "answering questions"),
        "es" to listOf("entrevista", "conversando", "escritorio", "portátil", "currículum", "respondiendo"),
        "fr" to listOf("entretien", "discuter", "bureau", "ordinateur", "CV", "répondre aux questions"),
        "de" to listOf("Vorstellungsgespräch", "besprechen", "Schreibtisch", "Laptop", "Lebenslauf", "antworten")
      ),
      modelDescriptionsByLang = mapOf(
        "en" to "A manager and a candidate are sitting at a desk discussing a project during a job interview.",
        "es" to "Una gerente y un candidato están sentados frente al escritorio conversando durante una entrevista laboral.",
        "fr" to "Un responsable et un candidat discutent autour d'un bureau lors d'un entretien professionnel.",
        "de" to "Eine Managerin und ein Bewerber sitzen am Schreibtisch und führen ein Vorstellungsgespräch."
      ),
      modelDescriptionEn = "A manager and a candidate are sitting at a desk discussing a project during a job interview."
    ),
    SpeakSceneTemplate(
      sceneId = "park_friends_gathering",
      title = "Friends Meeting Outdoors",
      illustrationKind = VisualIllustrationKind.FRIENDS_PEACE_PEOPLE,
      visualConceptId = "hat",
      questionEn = "How are the friends spending their afternoon together?",
      questionsByLang = mapOf(
        "en" to "How are the friends spending their afternoon together?",
        "es" to "¿Cómo están pasando la tarde juntos los amigos?",
        "fr" to "Comment les amis passent-ils l'après-midi ensemble ?",
        "de" to "Wie verbringen die Freunde ihren Nachmittag zusammen?"
      ),
      keyVocabByLang = mapOf(
        "en" to listOf("friends", "smiling", "greeting", "outdoors", "talking", "sunny afternoon"),
        "es" to listOf("amigos", "sonriendo", "saludando", "aire libre", "conversando", "tarde soleada"),
        "fr" to listOf("amis", "sourire", "saluer", "plein air", "discuter", "après-midi"),
        "de" to listOf("Freunde", "lächeln", "begrüßen", "draußen", "unterhalten", "Nachmittag")
      ),
      modelDescriptionsByLang = mapOf(
        "en" to "Three friends are smiling, waving hello, and enjoying a sunny afternoon conversation outdoors.",
        "es" to "Tres amigos están sonriendo, saludándose y disfrutando de una charla al aire libre.",
        "fr" to "Des amis sourient, se saluent chaleureusement et profitent d'un moment convivial dehors.",
        "de" to "Gute Freunde lächeln, begrüßen sich herzlich und genießen das Gespräch im Freien."
      ),
      modelDescriptionEn = "Three friends are smiling, waving hello, and enjoying a sunny afternoon conversation outdoors."
    ),
    SpeakSceneTemplate(
      sceneId = "city_commute_street",
      title = "Downtown City Street & Transit",
      illustrationKind = VisualIllustrationKind.CITY_BUILDINGS,
      visualConceptId = "bus",
      questionEn = "What can you see happening on this busy city street?",
      questionsByLang = mapOf(
        "en" to "What can you see happening on this busy city street?",
        "es" to "¿Qué puedes ver que ocurre en esta calle de la ciudad?",
        "fr" to "Que voyez-vous se passer dans cette rue animée de la ville ?",
        "de" to "Was passiert auf dieser belebten Straße in der Stadt?"
      ),
      keyVocabByLang = mapOf(
        "en" to listOf("tall buildings", "commuters", "walking", "bus", "crosswalk", "downtown"),
        "es" to listOf("edificios altos", "personas caminando", "autobús", "cruce peatonal", "centro", "tráfico"),
        "fr" to listOf("immeubles", "piétons", "marcher", "bus", "centre-ville", "circulation"),
        "de" to listOf("Gebäude", "Fußgänger", "gehen", "Bus", "Innenstadt", "Straße")
      ),
      modelDescriptionsByLang = mapOf(
        "en" to "People are walking along a downtown street near tall office buildings and public transit.",
        "es" to "Las personas caminan por una avenida del centro junto a edificios altos y transporte público.",
        "fr" to "Les passants marchent dans le centre-ville près des grands immeubles et des transports.",
        "de" to "Menschen gehen durch die Innenstadt entlang hoher Bürogebäude und öffentlicher Verkehrsmittel."
      ),
      modelDescriptionEn = "People are walking along a downtown street near tall office buildings and public transit."
    )
  )

  // ==================== MULTILINGUAL TONGUE TWISTERS (GAME 3: ARTICULATION SPRINT) ====================

  data class ArticulationDrillItem(
    val focusSounds: String,
    val phraseByLang: Map<String, String>,
    val phoneticByLang: Map<String, String>,
    val tipEn: String
  )

  val ARTICULATION_DRILLS: List<ArticulationDrillItem> = listOf(
    ArticulationDrillItem(
      focusSounds = "/r/ vs /l/ Liquid Contrast",
      phraseByLang = mapOf(
        "en" to "Red leather, yellow leather, red lorry, yellow lorry.",
        "es" to "Erre con erre cigarro, erre con erre barril, rápido ruedan los carros.",
        "fr" to "Le riz tenta le rat, le rat tenté tâta le riz.",
        "de" to "Rote Rosen regnen reichlich über runde Rasenflächen.",
        "ja" to "隣の客はよく柿食う客だ (Tonari no kyaku wa yoku kaki kuu kyaku da).",
        "ko" to "저기 저 콩깍지는 깐 콩깍지인가 안 깐 콩깍지인가.",
        "it" to "Trentatré trentini entrarono a Trento tutti e trentatré trotterellando.",
        "pt" to "O rato roeu a roupa do rei de Roma e a rainha riu."
      ),
      phoneticByLang = mapOf(
        "en" to "/rɛd ˈlɛðər ˈjɛloʊ ˈlɛðər/",
        "es" to "/ˈere kon ˈere siˈɣaro/",
        "fr" to "/lə ʁi tɑ̃ta lə ʁa/",
        "de" to "/ˈʁoːtə ˈʁoːzn̩ ˈʁeːɡnən/"
      ),
      tipEn = "Keep the tip of your tongue nimble as you alternate rapidly between R and L positions."
    ),
    ArticulationDrillItem(
      focusSounds = "/s/ vs /ʃ/ Sibilant Precision",
      phraseByLang = mapOf(
        "en" to "She sells seashells by the seashore, and the shells she sells are seashells.",
        "es" to "Seis sabios silenciosos salen siempre al sol sin sombrero.",
        "fr" to "Un chasseur sachant chasser sait chasser sans son chien.",
        "de" to "Fischers Fritz fischt frische Fische, frische Fische fischt Fischers Fritz.",
        "ja" to "新春シャンソンショー (Shinshun shanson shō).",
        "ko" to "신진 샹송 가수의 신춘 샹송 쇼.",
        "it" to "Sopra la panca la capra campa, sotto la panca la capra crepa.",
        "pt" to "Sabia que a mãe do sabiá não sabia que o sabiá sabia assobiar?"
      ),
      phoneticByLang = mapOf(
        "en" to "/ʃiː sɛlz ˈsiːʃɛlz baɪ ðə ˈsiːʃɔːr/",
        "es" to "/sejs ˈsaβjos silenˈsjosos/",
        "fr" to "/œ̃ ʃasœʁ saʃɑ̃ ʃase/",
        "de" to "/ˈfɪʃɐs fʁɪts fɪʃt ˈfʁɪʃə ˈfɪʃə/"
      ),
      tipEn = "Avoid slurring S and SH together; retract the tongue slightly for SH and push it forward for S."
    ),
    ArticulationDrillItem(
      focusSounds = "/p/, /b/ & /f/ Labial Burst",
      phraseByLang = mapOf(
        "en" to "Peter Piper picked a peck of pickled peppers.",
        "es" to "Pablito clavó un clavito en la calva de un calvito.",
        "fr" to "Papa boit dans les bois, du bon thé bien chaud.",
        "de" to "Blaukraut bleibt Blaukraut und Brautkleid bleibt Brautkleid.",
        "ja" to "生麦生米生卵 (Namamugi namagome namatamago).",
        "ko" to "내가 그린 기린 그림은 잘 그린 기린 그림이다.",
        "it" to "Apelle figlio di Apollo fece una palla di pelle di pollo.",
        "pt" to "O peito do pé de Pedro é preto; quem disser que é branco perde."
      ),
      phoneticByLang = mapOf(
        "en" to "/ˈpiːtər ˈpaɪpər pɪkt ə pɛk/",
        "es" to "/paˈβlito klaˈβo un klaˈβito/",
        "fr" to "/papa bwa dɑ̃ le bwa/",
        "de" to "/ˈblaʊkʁaʊt blaɪpt ˈblaʊkʁaʊt/"
      ),
      tipEn = "Release each P and B cleanly with your lips without dropping the unstressed vowels."
    ),
    ArticulationDrillItem(
      focusSounds = "/tr/, /kr/ & /gr/ Clusters",
      phraseByLang = mapOf(
        "en" to "Fresh French fried fish, crisp crust, and three free throws.",
        "es" to "Tres tristes tigres comen trigo en un trigal tranquilo.",
        "fr" to "Trois très gros rats gris dans trois très gros trous creux.",
        "de" to "Dreiunddreißig durstige Drachen drucken dicke Dokumente.",
        "ja" to "東京特許許可局局長 (Tōkyō tokkyo kyokakyoku kyokuchō).",
        "ko" to "검찰청 쇠창살은 외철창살이냐 쌍철창살이냐.",
        "it" to "Tre tigri contro tre tigri mangiano grano nel granaio.",
        "pt" to "Três pratos de trigo para três tigres tristes."
      ),
      phoneticByLang = mapOf(
        "en" to "/frɛʃ frɛntʃ fraɪd fɪʃ/",
        "es" to "/tres ˈtristes ˈtiɣres ˈkomen ˈtriɣo/",
        "fr" to "/tʁwa tʁɛ ɡʁo ʁa ɡʁi/",
        "de" to "/ˈdʁaɪʊntˌdʁaɪsɪç ˈdʊʁstɪɡə ˈdʁaxn̩/"
      ),
      tipEn = "Articulate both consonants in every cluster clearly before moving to the vowel."
    ),
    ArticulationDrillItem(
      focusSounds = "/n/, /j/ & Vowel Transitions",
      phraseByLang = mapOf(
        "en" to "Unique New York, you know you need unique New York.",
        "es" to "El niño ñoño desayuna piña y castañas en otoño.",
        "fr" to "Les chaussettes de l'archiduchesse sont-elles sèches, archi-sèches ?",
        "de" to "Zwischen zwei Zwetschgenzweigen zwitschern zwei Schwalben.",
        "ja" to "赤巻紙青巻紙黄巻紙 (Akamakigami aomakigami kimakigami).",
        "ko" to "중앙청 창살은 쌍창살이고 시청 창살은 외창살이다.",
        "it" to "Ogni ragno tesse la sua ragnatela ogni giorno.",
        "pt" to "O ninho de mafagafos tem sete mafagafinhos."
      ),
      phoneticByLang = mapOf(
        "en" to "/juːˈniːk njuː jɔːrk/",
        "es" to "/el ˈniɲo ˈɲoɲo desaˈʝuna ˈpiɲa/",
        "fr" to "/le ʃosɛt də laʁʃidyʃɛs/",
        "de" to "/ˈtsvɪʃn̩ tsvaɪ ˈtsvɛtʃɡn̩ˌtsvaɪɡn̩/"
      ),
      tipEn = "Start slowly at 0.75x to lock in nasal and palatal transitions, then repeat at full speed."
    ),
    ArticulationDrillItem(
      focusSounds = "/s/, /w/ & Rhythmic Cadence",
      phraseByLang = mapOf(
        "en" to "Six sleek swans swam swiftly southwards across the quiet bay.",
        "es" to "Cuando cuentes cuentos, cuenta cuántos cuentos cuentas.",
        "fr" to "Ces six saucissons-ci sont si secs qu'on ne sait si c'en sont.",
        "de" to "Sieben Schneeschaufler schaufeln sieben Schaufeln Schnee.",
        "ja" to "バスガス爆発、ブスバスガイド (Basu gasu bakuhatsu).",
        "ko" to "상표 붙인 큰 깡통은 깐 깡통인가 안 깐 깡통인가.",
        "it" to "Sei savie sorelle suonano sette strumenti.",
        "pt" to "Se o papa papasse papa, se o papa papasse pão."
      ),
      phoneticByLang = mapOf(
        "en" to "/sɪks sliːk swɒnz swæm ˈswɪftli/",
        "es" to "/ˈkwando ˈkwentes ˈkwentos/",
        "fr" to "/se si sosisɔ̃ si sɔ̃ si sɛk/",
        "de" to "/ˈziːbn̩ ˈʃneːˌʃaʊflɐ ˈʃaʊfln̩/"
      ),
      tipEn = "Maintain steady breath support through the entire phrase without rushing the final words."
    )
  )

  // ==================== QUICK ANSWER PRACTICAL QUESTIONS (GAME 5) ====================

  data class QuickAnswerPromptItem(
    val questionByLang: Map<String, String>,
    val questionEn: String,
    val sampleAnswerByLang: Map<String, String>,
    val sampleAnswerEn: String,
    val expectedKeywordsByLang: Map<String, List<String>>
  )

  val QUICK_ANSWER_ITEMS: List<QuickAnswerPromptItem> = listOf(
    QuickAnswerPromptItem(
      questionByLang = mapOf(
        "en" to "What do you usually eat for breakfast?",
        "es" to "¿Qué sueles comer normalmente en el desayuno?",
        "fr" to "Que prenez-vous habituellement au petit-déjeuner ?",
        "de" to "Was essen Sie normalerweise zum Frühstück?",
        "ja" to "普段、朝ごはんに何を食べますか？",
        "ko" to "보통 아침 식사로 무엇을 드시나요?",
        "it" to "Cosa mangi di solito a colazione?",
        "pt" to "O que você costuma comer no café da manhã?"
      ),
      questionEn = "What do you usually eat for breakfast?",
      sampleAnswerByLang = mapOf(
        "en" to "I usually eat eggs and toasted bread with a cup of hot coffee.",
        "es" to "Normalmente desayuno huevos con pan tostado y una taza de café caliente.",
        "fr" to "D'habitude, je prends du pain grillé, des œufs et une tasse de café.",
        "de" to "Normalerweise esse ich Eier und Toastbrot mit einer Tasse Kaffee.",
        "ja" to "普段は卵とトーストを食べて、温かいコーヒーを飲みます。",
        "ko" to "저는 보통 계란과 토스트를 먹고 따뜻한 커피를 마십니다.",
        "it" to "Di solito mangio pane tostato con uova e bevo una tazza di caffè.",
        "pt" to "Eu costumo comer ovos com pão torrado e tomar uma xícara de café."
      ),
      sampleAnswerEn = "I usually eat eggs and toasted bread with a cup of hot coffee.",
      expectedKeywordsByLang = mapOf(
        "en" to listOf("eat", "have", "drink", "eggs", "bread", "coffee", "tea", "fruit", "cereal", "toast"),
        "es" to listOf("como", "desayuno", "tomo", "huevos", "pan", "café", "té", "fruta", "leche"),
        "fr" to listOf("mange", "prends", "bois", "pain", "café", "thé", "fruits", "œufs"),
        "de" to listOf("esse", "trinke", "Brot", "Kaffee", "Tee", "Eier", "Obst", "Müsli")
      )
    ),
    QuickAnswerPromptItem(
      questionByLang = mapOf(
        "en" to "How do you usually travel to work or school?",
        "es" to "¿Cómo sueles ir al trabajo o a la escuela cada día?",
        "fr" to "Comment allez-vous habituellement au travail ou à l'école ?",
        "de" to "Wie fahren Sie normalerweise zur Arbeit oder zur Schule?",
        "ja" to "普段どうやって仕事や学校に行きますか？",
        "ko" to "보통 직장이나 학교에 어떻게 가시나요?",
        "it" to "Come vai di solito al lavoro o a scuola?",
        "pt" to "Como você costuma ir para o trabalho ou para a escola?"
      ),
      questionEn = "How do you usually travel to work or school?",
      sampleAnswerByLang = mapOf(
        "en" to "I usually take the train in the morning, or I walk when the weather is sunny.",
        "es" to "Normalmente tomo el tren por la mañana o camino cuando hace buen tiempo.",
        "fr" to "Je prends généralement le métro le matin, ou je marche quand il fait beau.",
        "de" to "Normalerweise fahre ich morgens mit der Bahn oder gehe zu Fuß.",
        "ja" to "普段は朝、電車に乗りますが、天気が良い日は歩きます。",
        "ko" to "보통 아침에 지하철을 타거나 날씨가 좋으면 걸어갑니다.",
        "it" to "Di solito prendo il treno al mattino oppure vado a piedi.",
        "pt" to "Normalmente eu pego o metrô de manhã ou vou caminhando."
      ),
      sampleAnswerEn = "I usually take the train in the morning, or I walk when the weather is sunny.",
      expectedKeywordsByLang = mapOf(
        "en" to listOf("train", "bus", "walk", "drive", "car", "bike", "metro", "subway", "go", "take"),
        "es" to listOf("tren", "autobús", "camino", "coche", "carro", "metro", "bicicleta", "voy", "tomo"),
        "fr" to listOf("train", "bus", "marche", "voiture", "métro", "vélo", "vais", "prends"),
        "de" to listOf("Zug", "Bahn", "Bus", "Auto", "Fahrrad", "Fuß", "fahre", "gehe")
      )
    ),
    QuickAnswerPromptItem(
      questionByLang = mapOf(
        "en" to "What do you enjoy doing in your free time on weekends?",
        "es" to "¿Qué te gusta hacer en tu tiempo libre durante los fines de semana?",
        "fr" to "Qu'aimez-vous faire pendant votre temps libre le week-end ?",
        "de" to "Was machen Sie am Wochenende gern in Ihrer Freizeit?",
        "ja" to "週末の自由な時間には何をするのが好きですか？",
        "ko" to "주말 자유 시간에 주로 무엇을 하는 것을 좋아하시나요?",
        "it" to "Cosa ti piace fare nel tempo libero durante il fine settimana?",
        "pt" to "O que você gosta de fazer no seu tempo livre nos fins de semana?"
      ),
      questionEn = "What do you enjoy doing in your free time on weekends?",
      sampleAnswerByLang = mapOf(
        "en" to "On weekends, I like reading books, cooking dinner, and walking in the park with friends.",
        "es" to "Los fines de semana me gusta leer libros, cocinar y pasear por el parque con mis amigos.",
        "fr" to "Le week-end, j'aime lire, cuisiner et me promener dans le parc avec mes amis.",
        "de" to "Am Wochenende lese ich gern Bücher, koche und gehe mit Freunden im Park spazieren.",
        "ja" to "週末は本を読んだり、料理をしたり、友達と公園を散歩したりするのが好きです。",
        "ko" to "주말에는 책을 읽거나 요리하고 친구들과 공원을 산책하는 것을 좋아합니다.",
        "it" to "Nel fine settimana mi piace leggere, cucinare e passeggiare al parco con gli amici.",
        "pt" to "Nos fins de semana gosto de ler livros, cozinhar e caminhar no parque com amigos."
      ),
      sampleAnswerEn = "On weekends, I like reading books, cooking dinner, and walking in the park with friends.",
      expectedKeywordsByLang = mapOf(
        "en" to listOf("like", "enjoy", "love", "read", "cook", "walk", "friends", "music", "watch", "play", "relax"),
        "es" to listOf("gusta", "encanta", "leer", "cocinar", "caminar", "amigos", "música", "descansar", "jugar"),
        "fr" to listOf("aime", "adore", "lire", "cuisiner", "marcher", "amis", "musique", "sport"),
        "de" to listOf("gern", "lese", "koche", "Freunde", "Musik", "Sport", "spazieren", "entspannen")
      )
    ),
    QuickAnswerPromptItem(
      questionByLang = mapOf(
        "en" to "What is your favorite season of the year, and why?",
        "es" to "¿Cuál es tu estación del año favorita y por qué?",
        "fr" to "Quelle est votre saison préférée de l'année et pourquoi ?",
        "de" to "Was ist Ihre Lieblingsjahreszeit und warum?"
      ),
      questionEn = "What is your favorite season of the year, and why?",
      sampleAnswerByLang = mapOf(
        "en" to "My favorite season is spring because the weather is pleasant and the trees are green.",
        "es" to "Mi estación favorita es la primavera porque el clima es agradable y hay muchas flores.",
        "fr" to "Ma saison préférée est le printemps parce qu'il fait doux et que la nature fleurit.",
        "de" to "Meine Lieblingsjahreszeit ist der Frühling, weil das Wetter mild und sonnig ist."
      ),
      sampleAnswerEn = "My favorite season is spring because the weather is pleasant and the trees are green.",
      expectedKeywordsByLang = mapOf(
        "en" to listOf("spring", "summer", "autumn", "fall", "winter", "because", "weather", "warm", "cool", "sun"),
        "es" to listOf("primavera", "verano", "otoño", "invierno", "porque", "clima", "tiempo", "sol", "calor"),
        "fr" to listOf("printemps", "été", "automne", "hiver", "parce", "temps", "soleil", "doux"),
        "de" to listOf("Frühling", "Sommer", "Herbst", "Winter", "weil", "Wetter", "sonnig", "warm")
      )
    ),
    QuickAnswerPromptItem(
      questionByLang = mapOf(
        "en" to "What kind of food do you like to order at a restaurant?",
        "es" to "¿Qué tipo de comida te gusta pedir cuando vas a un restaurante?",
        "fr" to "Quel genre de plat aimez-vous commander au restaurant ?",
        "de" to "Was bestellen Sie am liebsten, wenn Sie ins Restaurant gehen?"
      ),
      questionEn = "What kind of food do you like to order at a restaurant?",
      sampleAnswerByLang = mapOf(
        "en" to "I love ordering grilled fish or fresh pasta with vegetables and lemon water.",
        "es" to "Me gusta pedir pescado a la plancha o pasta fresca con verduras y ensalada.",
        "fr" to "J'aime commander du poisson grillé ou des pâtes fraîches avec des légumes.",
        "de" to "Ich bestelle am liebsten gegrillten Fisch oder frische Pasta mit Gemüse."
      ),
      sampleAnswerEn = "I love ordering grilled fish or fresh pasta with vegetables and lemon water.",
      expectedKeywordsByLang = mapOf(
        "en" to listOf("order", "like", "fish", "chicken", "pasta", "rice", "salad", "soup", "pizza", "meat", "vegetables"),
        "es" to listOf("pedir", "gusta", "pescado", "pollo", "pasta", "arroz", "ensalada", "sopa", "carne", "verduras"),
        "fr" to listOf("commander", "aime", "poisson", "poulet", "pâtes", "riz", "salade", "soupe", "légumes"),
        "de" to listOf("bestelle", "gern", "Fisch", "Hähnchen", "Pasta", "Reis", "Salat", "Suppe", "Gemüse")
      )
    )
  )

  // ==================== FLUENCY SPRINT CONTINUOUS TOPICS (GAME 7) ====================

  data class FluencyTopicItem(
    val titleEn: String,
    val promptByLang: Map<String, String>,
    val talkingPointsByLang: Map<String, List<String>>,
    val modelParagraphByLang: Map<String, String>,
    val modelParagraphEn: String
  )

  val FLUENCY_SPRINT_ITEMS: List<FluencyTopicItem> = listOf(
    FluencyTopicItem(
      titleEn = "Describe your morning routine",
      promptByLang = mapOf(
        "en" to "Speak continuously about your morning routine from waking up to starting your day.",
        "es" to "Habla de forma continua sobre tu rutina de la mañana desde que te despiertas hasta que empiezas el día.",
        "fr" to "Parlez de façon continue de votre routine matinale, du réveil jusqu'au départ.",
        "de" to "Sprechen Sie zusammenhängend über Ihre Morgenroutine vom Aufwachen bis zum Arbeitsbeginn.",
        "ja" to "朝起きてから一日を始めるまでのルーティンについて続けて話してください。",
        "ko" to "아침에 일어나서 하루를 시작할 때까지의 일과를 이어서 말해 보세요.",
        "it" to "Parla in modo continuo della tua routine mattutina da quando ti svegli.",
        "pt" to "Fale continuamente sobre sua rotina matinal desde a hora em que acorda."
      ),
      talkingPointsByLang = mapOf(
        "en" to listOf(
          "What time you wake up and what you do first",
          "What you eat or drink for breakfast",
          "How you prepare and leave for work or study"
        ),
        "es" to listOf(
          "A qué hora te despiertas y qué haces primero",
          "Qué desayunas o qué bebida prefieres",
          "Cómo te preparas para salir al trabajo o estudio"
        ),
        "fr" to listOf(
          "À quelle heure vous vous réveillez le matin",
          "Ce que vous prenez pour le petit-déjeuner",
          "Comment vous vous préparez avant de partir"
        ),
        "de" to listOf(
          "Wann Sie aufstehen und was Sie zuerst tun",
          "Was Sie zum Frühstück essen und trinken",
          "Wie Sie sich auf den Tag vorbereiten"
        )
      ),
      modelParagraphByLang = mapOf(
        "en" to "Every morning I wake up at seven o'clock and drink a glass of water. After a quick shower, I make hot coffee and eat toast with fruit. Then I check my schedule and take the train to work.",
        "es" to "Cada mañana me despierto a las siete y tomo un vaso de agua. Después de ducharme, preparo café caliente y desayuno pan tostado con fruta. Luego reviso mi agenda y salgo hacia el trabajo.",
        "fr" to "Chaque matin, je me réveille à sept heures et je bois un verre d'eau. Ensuite, je prépare un café chaud avec des tartines avant de prendre le métro pour aller travailler.",
        "de" to "Jeden Morgen wache ich um sieben Uhr auf und trinke ein Glas Wasser. Danach mache ich mir frischen Kaffee, frühstücke in Ruhe und fahre mit der Bahn zur Arbeit."
      ),
      modelParagraphEn = "Every morning I wake up at seven o'clock and drink a glass of water. After a quick shower, I make hot coffee and eat toast with fruit. Then I check my schedule and take the train to work."
    ),
    FluencyTopicItem(
      titleEn = "Talk about your favorite place to relax",
      promptByLang = mapOf(
        "en" to "Describe your favorite place to relax, what it looks like, and what you do there.",
        "es" to "Describe tu lugar favorito para relajarte, cómo es y qué sueles hacer allí.",
        "fr" to "Décrivez votre endroit préféré pour vous détendre et ce que vous y faites.",
        "de" to "Beschreiben Sie Ihren Lieblingsort zum Entspannen und was Sie dort gern tun."
      ),
      talkingPointsByLang = mapOf(
        "en" to listOf(
          "Where this place is located (home, park, café, beach)",
          "What you can see and hear when you are there",
          "Why it helps you feel calm and recharged"
        ),
        "es" to listOf(
          "Dónde está ubicado ese lugar (parque, casa, playa, café)",
          "Qué puedes ver y escuchar cuando estás allí",
          "Por qué te ayuda a sentirte tranquilo y con energía"
        ),
        "fr" to listOf(
          "Où se trouve cet endroit (parc, maison, bord de mer, café)",
          "Ce que vous voyez et entendez autour de vous",
          "Pourquoi cet endroit vous apaise"
        ),
        "de" to listOf(
          "Wo sich dieser Ort befindet (Park, Zuhause, Café, See)",
          "Was Sie dort sehen und erleben",
          "Warum Sie sich dort besonders wohl fühlen"
        )
      ),
      modelParagraphByLang = mapOf(
        "en" to "My favorite place to relax is a quiet botanical park near my home. I love walking under the tall green trees, listening to the birds, and reading a book on a wooden bench in the afternoon.",
        "es" to "Mi lugar favorito para relajarme es un parque tranquilo cerca de mi casa. Me encanta caminar bajo los árboles verdes, escuchar a los pájaros y leer un buen libro por la tarde.",
        "fr" to "Mon endroit préféré pour me détendre est un jardin calme près de chez moi. J'aime marcher sous les arbres, écouter les oiseaux et lire sur un banc au soleil.",
        "de" to "Mein Lieblingsort zum Entspannen ist ein ruhiger Park in meiner Nähe. Ich gehe dort gern unter den grünen Bäumen spazieren und lese am Nachmittag ein Buch auf einer Bank."
      ),
      modelParagraphEn = "My favorite place to relax is a quiet botanical park near my home. I love walking under the tall green trees, listening to the birds, and reading a book on a wooden bench in the afternoon."
    ),
    FluencyTopicItem(
      titleEn = "Describe a memorable meal with family or friends",
      promptByLang = mapOf(
        "en" to "Talk continuously about a memorable meal you shared with family or friends.",
        "es" to "Habla de forma continua sobre una comida especial que compartiste con tu familia o amigos.",
        "fr" to "Parlez d'un repas mémorable que vous avez partagé avec votre famille ou vos amis.",
        "de" to "Erzählen Sie von einem besonderen Essen, das Sie mit Familie oder Freunden erlebt haben."
      ),
      talkingPointsByLang = mapOf(
        "en" to listOf(
          "Who was with you and where you ate",
          "What dishes were served and how they tasted",
          "What you talked about around the table"
        ),
        "es" to listOf(
          "Con quién estabas y dónde comieron",
          "Qué platos probaron y por qué estaban deliciosos",
          "De qué conversaron durante la reunión"
        )
      ),
      modelParagraphByLang = mapOf(
        "en" to "Last month I had a wonderful dinner with my close friends at a small neighborhood restaurant. We shared homemade pasta, roasted vegetables, and warm bread while laughing and catching up on our travel stories.",
        "es" to "El mes pasado disfruté de una cena maravillosa con mis mejores amigos en un restaurante del barrio. Compartimos pasta casera, verduras asadas y pan caliente mientras contábamos anécdotas de viaje.",
        "fr" to "Le mois dernier, j'ai partagé un dîner inoubliable avec mes amis dans un petit restaurant. Nous avons dégusté des plats faits maison tout en discutant de nos projets de voyage.",
        "de" to "Letzten Monat hatte ich ein wunderbares Abendessen mit guten Freunden in einem kleinen Restaurant. Wir haben frische Pasta geteilt und uns den ganzen Abend über unsere Reisen unterhalten."
      ),
      modelParagraphEn = "Last month I had a wonderful dinner with my close friends at a small neighborhood restaurant. We shared homemade pasta and warm bread while catching up on our travel stories."
    ),
    FluencyTopicItem(
      titleEn = "Share your language learning goals",
      promptByLang = mapOf(
        "en" to "Speak about why you are learning this language and how you plan to use it in real life.",
        "es" to "Habla sobre por qué estás aprendiendo este idioma y cómo planeas usarlo en la vida real.",
        "fr" to "Expliquez pourquoi vous apprenez cette langue et comment vous comptez l'utiliser.",
        "de" to "Erzählen Sie, warum Sie diese Sprache lernen und wie Sie sie im Alltag nutzen möchten."
      ),
      talkingPointsByLang = mapOf(
        "en" to listOf(
          "What motivated you to start learning this language",
          "Situations where you want to speak confidently (travel, work, friends)",
          "How you practice speaking every week"
        ),
        "es" to listOf(
          "Qué te motivó a empezar a aprender este idioma",
          "En qué situaciones quieres hablar con confianza (viajes, trabajo, amigos)",
          "Cómo practicas la expresión oral cada semana"
        )
      ),
      modelParagraphByLang = mapOf(
        "en" to "I am learning this language so I can travel confidently, connect with local people, and use it in my career. I practice speaking for fifteen minutes every day to build natural fluency.",
        "es" to "Estoy aprendiendo este idioma para viajar con confianza, conversar con personas locales y crecer en mi carrera profesional. Practico en voz alta todos los días para ganar fluidez.",
        "fr" to "J'apprends cette langue pour voyager sereinement, échanger avec les habitants et progresser dans mon travail. Je pratique à l'oral chaque jour avec régularité.",
        "de" to "Ich lerne diese Sprache, um auf Reisen sicher zu sprechen, neue Menschen kennenzulernen und mich beruflich weiterzuentwickeln. Deshalb übe ich jeden Tag laut zu sprechen."
      ),
      modelParagraphEn = "I am learning this language so I can travel confidently, connect with local people, and use it in my career."
    )
  )

  // ==================== DIALOGUE LINE CONVERSATIONS (GAME 9) ====================

  data class DialogueTurnTemplate(
    val contextTitle: String,
    val speakerARole: String,
    val speakerALineByLang: Map<String, String>,
    val speakerALineEn: String,
    val learnerGoalEn: String,
    val modelReplyByLang: Map<String, String>,
    val modelReplyEn: String,
    val alternateRepliesByLang: Map<String, List<String>>,
    val speakerAFollowUpByLang: Map<String, String>
  )

  val DIALOGUE_LINE_TEMPLATES: List<DialogueTurnTemplate> = listOf(
    DialogueTurnTemplate(
      contextTitle = "Ordering at a Neighborhood Café",
      speakerARole = "Barista",
      speakerALineByLang = mapOf(
        "en" to "Good morning! Welcome to Sunbeam Café. What can I get started for you today?",
        "es" to "¡Buenos días! Bienvenido al Café Sol. ¿Qué le puedo servir hoy?",
        "fr" to "Bonjour ! Bienvenue au Café du Soleil. Que puis-je vous servir aujourd'hui ?",
        "de" to "Guten Morgen! Willkommen im Café Sonnenschein. Was darf ich Ihnen heute bringen?",
        "ja" to "おはようございます！サンビームカフェへようこそ。本日は何になさいますか？",
        "ko" to "좋은 아침입니다! 선빔 카페에 오신 것을 환영합니다. 오늘 어떤 메뉴로 준비해 드릴까요?",
        "it" to "Buongiorno! Benvenuto al Caffè del Sole. Cosa posso prepararle oggi?",
        "pt" to "Bom dia! Bem-vindo ao Café do Sol. O que posso preparar para você hoje?"
      ),
      speakerALineEn = "Good morning! Welcome to Sunbeam Café. What can I get started for you today?",
      learnerGoalEn = "Greet the barista and order a drink or pastry politely.",
      modelReplyByLang = mapOf(
        "en" to "Good morning! Could I please have a medium cappuccino and a warm croissant?",
        "es" to "¡Buenos días! ¿Me podría dar un capuchino mediano y un cruasán caliente, por favor?",
        "fr" to "Bonjour ! Je voudrais un cappuccino moyen et un croissant chaud, s'il vous plaît.",
        "de" to "Guten Morgen! Ich hätte gern einen mittleren Cappuccino und ein warmes Croissant, bitte.",
        "ja" to "おはようございます！カプチーノのMサイズと温かいクロワッサンをお願いします。",
        "ko" to "좋은 아침이에요! 따뜻한 카푸치노 한 잔과 크루아상 하나 부탁합니다.",
        "it" to "Buongiorno! Vorrei un cappuccino medio e un cornetto caldo, per favore.",
        "pt" to "Bom dia! Eu gostaria de um cappuccino médio e um croissant quentinho, por favor."
      ),
      modelReplyEn = "Good morning! Could I please have a medium cappuccino and a warm croissant?",
      alternateRepliesByLang = mapOf(
        "en" to listOf(
          "Hi! I would like an iced coffee with oat milk, please.",
          "Hello! Can I get a cup of green tea and a muffin?"
        ),
        "es" to listOf(
          "¡Hola! Quisiera un café con leche y una tostada, por favor.",
          "¡Buenos días! Me pone un té verde y un pastelito, por favor."
        )
      ),
      speakerAFollowUpByLang = mapOf(
        "en" to "Coming right up! Would you like that for here or to go?",
        "es" to "¡Enseguida sale! ¿Lo desea para tomar aquí o para llevar?",
        "fr" to "C'est noté ! Ce sera pour consommer sur place ou à emporter ?",
        "de" to "Kommt sofort! Möchten Sie das zum Hiertrinken oder zum Mitnehmen?"
      )
    ),
    DialogueTurnTemplate(
      contextTitle = "Making Weekend Plans with a Friend",
      speakerARole = "Friend (Mateo)",
      speakerALineByLang = mapOf(
        "en" to "Hey! The weather looks great this Saturday. Are you free to grab lunch or visit the market?",
        "es" to "¡Hola! Parece que hará muy buen tiempo este sábado. ¿Estás libre para almorzar o ir al mercado?",
        "fr" to "Salut ! Il va faire super beau ce samedi. Es-tu libre pour déjeuner ou aller au marché ?",
        "de" to "Hey! Das Wetter soll diesen Samstag toll werden. Hast du Zeit für ein Mittagessen oder den Markt?"
      ),
      speakerALineEn = "Hey! The weather looks great this Saturday. Are you free to grab lunch or visit the market?",
      learnerGoalEn = "Accept the invitation or suggest a convenient time to meet on Saturday.",
      modelReplyByLang = mapOf(
        "en" to "Yes, I would love to! Let's meet at the market around noon and have lunch together.",
        "es" to "¡Sí, me encantaría! Quedemos en el mercado al mediodía y almorcemos juntos.",
        "fr" to "Oui, avec plaisir ! Retrouvons-nous au marché vers midi pour déjeuner ensemble.",
        "de" to "Ja, sehr gern! Lass uns gegen Mittag am Markt treffen und zusammen essen."
      ),
      modelReplyEn = "Yes, I would love to! Let's meet at the market around noon and have lunch together.",
      alternateRepliesByLang = mapOf(
        "en" to listOf(
          "That sounds fun! I am free after one o'clock on Saturday.",
          "Definitely! Where should we meet on Saturday morning?"
        ),
        "es" to listOf(
          "¡Suena genial! Estoy libre el sábado después de la una.",
          "¡Claro que sí! ¿Dónde nos vemos el sábado?"
        )
      ),
      speakerAFollowUpByLang = mapOf(
        "en" to "Awesome! I will meet you by the main entrance at twelve. See you Saturday!",
        "es" to "¡Perfecto! Nos vemos en la entrada principal a las doce. ¡Hasta el sábado!",
        "fr" to "Super ! On se retrouve devant l'entrée principale à midi. À samedi !",
        "de" to "Klasse! Dann sehen wir uns um zwölf Uhr am Haupteingang. Bis Samstag!"
      )
    ),
    DialogueTurnTemplate(
      contextTitle = "Asking About Train Platform Details",
      speakerARole = "Station Attendant",
      speakerALineByLang = mapOf(
        "en" to "Hello! The express train to the coast is boarding soon. Do you have your ticket ready?",
        "es" to "¡Hola! El tren expreso hacia la costa abordará pronto. ¿Tiene listo su boleto?",
        "fr" to "Bonjour ! Le train express pour la côte va bientôt partir. Avez-vous votre billet ?",
        "de" to "Guten Tag! Der Expresszug zur Küste fährt bald ab. Haben Sie Ihre Fahrkarte bereit?"
      ),
      speakerALineEn = "Hello! The express train to the coast is boarding soon. Do you have your ticket ready?",
      learnerGoalEn = "Confirm you have your ticket and ask which platform the train leaves from.",
      modelReplyByLang = mapOf(
        "en" to "Yes, I have my ticket right here. Which platform does the express train depart from?",
        "es" to "Sí, tengo mi boleto aquí mismo. ¿De qué andén sale el tren expreso?",
        "fr" to "Oui, j'ai mon billet juste ici. De quel quai part le train express ?",
        "de" to "Ja, ich habe meine Fahrkarte hier. Von welchem Gleis fährt der Expresszug ab?"
      ),
      modelReplyEn = "Yes, I have my ticket right here. Which platform does the express train depart from?",
      alternateRepliesByLang = mapOf(
        "en" to listOf(
          "Here is my ticket! Could you tell me the platform number, please?",
          "I need to buy a return ticket first—where is the ticket machine?"
        ),
        "es" to listOf(
          "¡Aquí está mi boleto! ¿Me indica el número de vía, por favor?",
          "Sí, aquí lo tengo. ¿Cuánto falta para que salga el tren?"
        )
      ),
      speakerAFollowUpByLang = mapOf(
        "en" to "It departs from Platform 4, right down the escalator on your left. Have a safe trip!",
        "es" to "Sale del andén número cuatro, bajando las escaleras a la izquierda. ¡Buen viaje!",
        "fr" to "Il part du quai numéro quatre, juste à gauche après l'escalator. Bon voyage !",
        "de" to "Er fährt von Gleis vier ab, gleich links hinter der Rolltreppe. Gute Reise!"
      )
    ),
    DialogueTurnTemplate(
      contextTitle = "Coordinating with a Colleague at Work",
      speakerARole = "Colleague (Elena)",
      speakerALineByLang = mapOf(
        "en" to "Good afternoon! Are we still meeting at three o'clock to review the design proposal?",
        "es" to "¡Buenas tardes! ¿Seguimos teniendo nuestra reunión a las tres para revisar la propuesta?",
        "fr" to "Bonjour ! Est-ce qu'on se voit toujours à quinze heures pour revoir la proposition ?",
        "de" to "Guten Tag! Treffen wir uns noch um fünfzehn Uhr, um den Entwurf zu besprechen?"
      ),
      speakerALineEn = "Good afternoon! Are we still meeting at three o'clock to review the design proposal?",
      learnerGoalEn = "Confirm the meeting time or ask to move it by thirty minutes.",
      modelReplyByLang = mapOf(
        "en" to "Yes, three o'clock works great for me! I will bring the updated notes to the conference room.",
        "es" to "¡Sí, a las tres me viene muy bien! Llevaré las notas actualizadas a la sala de reuniones.",
        "fr" to "Oui, quinze heures me convient très bien ! J'apporterai les notes mises à jour.",
        "de" to "Ja, fünfzehn Uhr passt mir super! Ich bringe die aktuellen Notizen in den Besprechungsraum mit."
      ),
      modelReplyEn = "Yes, three o'clock works great for me! I will bring the updated notes to the conference room.",
      alternateRepliesByLang = mapOf(
        "en" to listOf(
          "Could we start at three thirty instead? I am finishing a quick call.",
          "Yes, I am ready! Shall we meet in room B or on video call?"
        ),
        "es" to listOf(
          "¿Podríamos empezar a las tres y media? Estoy terminando una llamada.",
          "¡Sí, ya estoy listo! Nos vemos en la sala principal."
        )
      ),
      speakerAFollowUpByLang = mapOf(
        "en" to "Sounds like a plan! See you in the conference room at three.",
        "es" to "¡Excelente! Nos vemos en la sala de juntas a las tres en punto.",
        "fr" to "Parfait ! On se retrouve dans la salle de réunion à quinze heures.",
        "de" to "Perfekt! Dann bis gleich um fünfzehn Uhr im Konferenzraum."
      )
    )
  )

  // ==================== BUILD DISTINCT CURATED EXERCISES FOR GAMES #24..#33 ====================

  fun buildSpeakingGameExercises(
    game: UltimateLanguageGame,
    spec: LevelCurriculumSpec,
    languageCode: String,
    nativeLanguageCode: String,
    targetCount: Int,
    excludedSet: Set<String>
  ): List<DynamicLevelExercise> {
    val targetKey = languageCode.lowercase().take(2)
    val nativeKey = nativeLanguageCode.lowercase().take(2).let { if (it == targetKey) "en" else it }
    val langName = Language.findByCode(languageCode).name

    val entries = GameCurriculumAndAntiRepetitionEngine.selectDistinctEntriesForLevel(
      game = game,
      languageCode = languageCode,
      levelNumber = spec.levelNumber,
      count = (targetCount * 2).coerceAtLeast(10),
      excludedLowercase = excludedSet
    )

    fun wordIn(item: ThematicLexiconItem, lang: String): String =
      item.translations[lang]?.first ?: item.enWord

    fun sentenceIn(item: ThematicLexiconItem, lang: String): String =
      item.translations[lang]?.second ?: item.enSentence

    return when (game.mechanic) {
      // GAME 1 — PRONUNCIATION PRACTICE (ug_24_pronunciation_battle): Individual words + phonetic + sound focus
      GameInteractionMechanic.SPEAKING_MIC -> {
        entries.take(targetCount).mapIndexed { idx, item ->
          val word = wordIn(item, targetKey)
          val meaning = if (targetKey == nativeKey) {
            "${item.domainLabel} • used in: \"${item.enSentence}\""
          } else {
            wordIn(item, nativeKey)
          }
          val syllables = splitWordIntoDisplaySyllables(word)
          val trickyFocus = detectDifficultSoundDescription(word, targetKey)
          val phonetic = buildPhoneticGuide(word, syllables, targetKey)
          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_MIC,
            prompt = "Pronounce this $langName word clearly (Focus: $trickyFocus):",
            targetPhrase = word,
            translation = meaning,
            phonetic = phonetic,
            options = syllables,
            correctIndex = 0,
            visualConceptId = item.visualConceptId,
            secondaryAudioPhrase = sentenceIn(item, targetKey),
            clozeMaskedSentence = trickyFocus,
            scrambledTokens = syllables,
            hint = "Syllable breakdown: ${syllables.joinToString(" • ")} | Focus on: $trickyFocus",
            explanation = "Example in context: \"${sentenceIn(item, targetKey)}\" (${sentenceIn(item, nativeKey)})",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, word)
          )
        }
      }

      // GAME 2 — REPEAT AFTER ME (ug_25_repeat_after_me): ALWAYS complete spoken sentences!
      GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE -> {
        entries.take(targetCount).mapIndexed { idx, item ->
          val baseSentence = sentenceIn(item, targetKey)
          val fullSentence = ensureCompleteSpokenSentence(baseSentence, wordIn(item, targetKey), targetKey, spec.levelNumber + idx)
          val nativeMeaning = if (targetKey == nativeKey) {
            "Key expression with \"${wordIn(item, targetKey)}\" (${item.domainLabel})"
          } else {
            sentenceIn(item, nativeKey)
          }
          val words = fullSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE,
            prompt = "Listen to the complete sentence and repeat it aloud with natural rhythm:",
            targetPhrase = fullSentence,
            translation = nativeMeaning,
            phonetic = "/${fullSentence.lowercase().take(48)}/",
            options = words,
            correctIndex = 0,
            secondaryAudioPhrase = wordIn(item, targetKey),
            scrambledTokens = words,
            hint = "Contains ${words.size} words • Key word: \"${wordIn(item, targetKey)}\"",
            explanation = "Complete sentence: \"$fullSentence\" ($nativeMeaning)",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, fullSentence)
          )
        }
      }

      // GAME 3 — ARTICULATION SPRINT (ug_26_tongue_twister): Tongue twisters & consonant clusters
      GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT -> {
        (0 until targetCount).map { idx ->
          val drillIdx = (spec.levelNumber * 2 + idx) % ARTICULATION_DRILLS.size
          val drill = ARTICULATION_DRILLS[drillIdx]
          val baseTwister = drill.phraseByLang[targetKey] ?: drill.phraseByLang["en"]!!
          // Ensure every level & index produces a unique targetPhrase for anti-repetition guarantees!
          val lexItem = entries[idx % entries.size]
          val lexWord = wordIn(lexItem, targetKey)
          val lexSent = sentenceIn(lexItem, targetKey)
          val cleanSpokenTwister = if (spec.levelNumber == 1 && idx == 0) {
            baseTwister
          } else if (idx % 2 == 0) {
            "$baseTwister — $lexWord, $lexWord."
          } else {
            "$lexSent $baseTwister"
          }
          val phonetic = drill.phoneticByLang[targetKey] ?: "/${cleanSpokenTwister.lowercase().take(42)}/"
          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT,
            prompt = "Articulation Sprint • ${drill.focusSounds}: Speak clearly at slow then normal speed!",
            targetPhrase = cleanSpokenTwister,
            translation = "${drill.focusSounds} — ${drill.tipEn}",
            phonetic = phonetic,
            options = listOf("0.70x Slow Drill", "1.0x Normal Tempo", "3x Rep Challenge"),
            correctIndex = 0,
            clozeMaskedSentence = drill.focusSounds,
            secondaryAudioPhrase = lexWord,
            hint = drill.tipEn,
            explanation = "Target consonant focus: ${drill.focusSounds}. Repeat smoothly without dropping word endings.",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, cleanSpokenTwister)
          )
        }
      }

      // GAME 4 — SPEAK THE SCENE (ug_27_speak_the_picture): Visual scene image + open question + description
      GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE -> {
        (0 until targetCount).map { idx ->
          val sceneIdx = (spec.levelNumber + idx) % SPEAK_THE_SCENE_TEMPLATES.size
          val scene = SPEAK_THE_SCENE_TEMPLATES[sceneIdx]
          val lexItem = entries[idx % entries.size]
          val extraVocabWord = wordIn(lexItem, targetKey)
          val extraSentence = sentenceIn(lexItem, targetKey)
          val baseModelDesc = scene.modelDescriptionsByLang[targetKey] ?: scene.modelDescriptionEn
          val uniqueModelDesc = if (spec.levelNumber == 1 && idx < SPEAK_THE_SCENE_TEMPLATES.size) {
            baseModelDesc
          } else {
            "$baseModelDesc $extraSentence"
          }
          val questionText = scene.questionsByLang[targetKey] ?: scene.questionEn
          val vocabChips = ((scene.keyVocabByLang[targetKey] ?: scene.keyVocabByLang["en"].orEmpty()) + extraVocabWord).distinct().take(6)

          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE,
            prompt = questionText,
            targetPhrase = uniqueModelDesc,
            translation = scene.modelDescriptionEn,
            phonetic = scene.title,
            options = vocabChips,
            correctIndex = 0,
            visualConceptId = scene.visualConceptId,
            illustrationKind = scene.illustrationKind,
            secondaryAudioPhrase = questionText,
            clozeMaskedSentence = scene.sceneId,
            scrambledTokens = vocabChips,
            hint = "Helpful scene words: ${vocabChips.joinToString(", ")}",
            explanation = "Model scene description: \"$uniqueModelDesc\" (${scene.modelDescriptionEn})",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, uniqueModelDesc)
          )
        }
      }

      // GAME 5 — QUICK ANSWER (ug_28_quick_answer): Everyday practical question -> natural answer (NEVER repeat question!)
      GameInteractionMechanic.SPEAKING_QUICK_ANSWER -> {
        (0 until targetCount).map { idx ->
          val qaIdx = (spec.levelNumber * 2 + idx) % QUICK_ANSWER_ITEMS.size
          val qa = QUICK_ANSWER_ITEMS[qaIdx]
          val lexItem = entries[idx % entries.size]
          val detailWord = wordIn(lexItem, targetKey)
          val detailSent = sentenceIn(lexItem, targetKey)

          val questionSpoken = qa.questionByLang[targetKey] ?: qa.questionEn
          val baseAnswer = qa.sampleAnswerByLang[targetKey] ?: qa.sampleAnswerEn
          val modelAnswer = if (spec.levelNumber == 1 && idx < QUICK_ANSWER_ITEMS.size) {
            baseAnswer
          } else {
            "$baseAnswer ($detailSent)"
          }
          val keywords = ((qa.expectedKeywordsByLang[targetKey] ?: qa.expectedKeywordsByLang["en"].orEmpty()) + detailWord).distinct().take(6)

          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_QUICK_ANSWER,
            prompt = questionSpoken,
            targetPhrase = modelAnswer,
            translation = qa.questionEn,
            phonetic = qa.sampleAnswerEn,
            options = keywords,
            correctIndex = 0,
            dialogueSpeakerALine = questionSpoken,
            secondaryAudioPhrase = questionSpoken,
            timeLimitSeconds = 15,
            hint = "Answer in your own words (e.g., use: ${keywords.take(4).joinToString(", ")}). Do not repeat the question.",
            explanation = "Example natural answer: \"$modelAnswer\" (${qa.sampleAnswerEn})",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, modelAnswer)
          )
        }
      }

      // GAME 6 — SPEECH COACH (ug_29_speech_coach_challenge): Free/guided speech + grammar/vocab/pronunciation coaching + targeted follow-up
      GameInteractionMechanic.SPEAKING_SPEECH_COACH -> {
        entries.take(targetCount).mapIndexed { idx, item ->
          val keyWord = wordIn(item, targetKey)
          val modelSentence = ensureCompleteSpokenSentence(sentenceIn(item, targetKey), keyWord, targetKey, spec.levelNumber + idx)
          val nativeMeaning = sentenceIn(item, nativeKey)
          val coachingPrompt = when (targetKey) {
            "es" -> "Habla libremente usando la idea de \"$keyWord\" en una frase completa sobre ${spec.objectiveTitle.lowercase()}:"
            "fr" -> "Parlez librement en utilisant \"$keyWord\" dans une phrase complète sur ${spec.objectiveTitle.lowercase()} :"
            "de" -> "Sprechen Sie frei und verwenden Sie \"$keyWord\" in einem vollständigen Satz über ${spec.objectiveTitle}:"
            else -> "Speak freely in a complete sentence about ${spec.objectiveTitle.lowercase()} (try incorporating \"$keyWord\"):"
          }
          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_SPEECH_COACH,
            prompt = coachingPrompt,
            targetPhrase = modelSentence,
            translation = nativeMeaning,
            phonetic = keyWord,
            options = listOf(keyWord, item.domainLabel, spec.band.cefrEquivalent),
            correctIndex = 0,
            secondaryAudioPhrase = modelSentence,
            clozeMaskedSentence = detectDifficultSoundDescription(keyWord, targetKey),
            hint = "Model inspiration: \"$modelSentence\" ($nativeMeaning)",
            explanation = "Polished native expression: \"$modelSentence\" • Focus word: \"$keyWord\"",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, modelSentence)
          )
        }
      }

      // GAME 7 — FLUENCY SPRINT (ug_30_fluency_sprint): Continuous speaking topic + 15s/30s/60s timer + talking points
      GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT -> {
        (0 until targetCount).map { idx ->
          val topicIdx = (spec.levelNumber + idx) % FLUENCY_SPRINT_ITEMS.size
          val topic = FLUENCY_SPRINT_ITEMS[topicIdx]
          val lexItem = entries[idx % entries.size]
          val bonusPhrase = sentenceIn(lexItem, targetKey)
          val baseParagraph = topic.modelParagraphByLang[targetKey] ?: topic.modelParagraphEn
          val uniqueModelParagraph = if (spec.levelNumber == 1 && idx < FLUENCY_SPRINT_ITEMS.size) {
            baseParagraph
          } else {
            "$baseParagraph $bonusPhrase"
          }
          val topicPrompt = topic.promptByLang[targetKey] ?: topic.promptByLang["en"]!!
          val talkingPoints = topic.talkingPointsByLang[targetKey] ?: topic.talkingPointsByLang["en"]!!

          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT,
            prompt = "${topic.titleEn} — $topicPrompt",
            targetPhrase = uniqueModelParagraph,
            translation = topic.modelParagraphEn,
            phonetic = topic.titleEn,
            options = talkingPoints,
            correctIndex = 0,
            secondaryAudioPhrase = uniqueModelParagraph,
            timeLimitSeconds = 30,
            hint = "Talking points: ${talkingPoints.joinToString(" • ")}",
            explanation = "Sample fluent response: \"$uniqueModelParagraph\"",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, uniqueModelParagraph)
          )
        }
      }

      // GAME 8 — SHADOW SPEAKING (ug_31_shadow_speaking): Synchronized audio shadowing with live word highlighting
      GameInteractionMechanic.SPEAKING_SHADOWING -> {
        entries.take(targetCount).mapIndexed { idx, item ->
          val rawSentence = sentenceIn(item, targetKey)
          val shadowSentence = ensureCompleteSpokenSentence(rawSentence, wordIn(item, targetKey), targetKey, spec.levelNumber + idx)
          val nativeMeaning = sentenceIn(item, nativeKey)
          val words = shadowSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_SHADOWING,
            prompt = "Shadow Speaking: Listen to the cadence, then tap 'Shadow Now' to speak simultaneously with the highlighted words:",
            targetPhrase = shadowSentence,
            translation = nativeMeaning,
            phonetic = words.joinToString("  liaison ") { it },
            options = words,
            correctIndex = 0,
            secondaryAudioPhrase = shadowSentence,
            scrambledTokens = words,
            hint = "Match the speaker's rhythm across all ${words.size} words without pausing.",
            explanation = "Shadowed sentence: \"$shadowSentence\" ($nativeMeaning)",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, shadowSentence)
          )
        }
      }

      // GAME 9 — DIALOGUE LINE (ug_32_dialogue_completion): Multi-turn conversation completion (NEVER isolated words!)
      GameInteractionMechanic.SPEAKING_DIALOGUE_LINE -> {
        (0 until targetCount).map { idx ->
          val dlgIdx = (spec.levelNumber * 2 + idx) % DIALOGUE_LINE_TEMPLATES.size
          val dlg = DIALOGUE_LINE_TEMPLATES[dlgIdx]
          val lexItem = entries[idx % entries.size]
          val extraSentence = sentenceIn(lexItem, targetKey)

          val speakerALine = dlg.speakerALineByLang[targetKey] ?: dlg.speakerALineEn
          val baseReply = dlg.modelReplyByLang[targetKey] ?: dlg.modelReplyEn
          val modelReply = if (spec.levelNumber == 1 && idx < DIALOGUE_LINE_TEMPLATES.size) {
            baseReply
          } else {
            "$baseReply $extraSentence"
          }
          val followUpLine = dlg.speakerAFollowUpByLang[targetKey] ?: dlg.speakerAFollowUpByLang["en"]!!
          val altReplies = dlg.alternateRepliesByLang[targetKey] ?: dlg.alternateRepliesByLang["en"].orEmpty()

          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_DIALOGUE_LINE,
            prompt = "${dlg.contextTitle} — ${dlg.learnerGoalEn}",
            targetPhrase = modelReply,
            translation = dlg.modelReplyEn,
            phonetic = dlg.speakerARole,
            options = (listOf(baseReply) + altReplies).distinct().take(3),
            correctIndex = 0,
            dialogueSpeakerALine = speakerALine,
            dialogueSpeakerBLine = followUpLine,
            secondaryAudioPhrase = speakerALine,
            clozeMaskedSentence = dlg.speakerALineEn,
            conversationRole = dlg.speakerARole,
            hint = "Goal: ${dlg.learnerGoalEn} • Example: \"$baseReply\"",
            explanation = "After your reply, ${dlg.speakerARole} continues: \"$followUpLine\"",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, modelReply)
          )
        }
      }

      // GAME 10 — ROLEPLAY MISSION (ug_33_roleplay_mission): 10 real-life multi-turn voice/text scenarios
      GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION -> {
        (0 until targetCount).map { idx ->
          val scenarioIdx = (spec.levelNumber - 1 + idx) % ROLEPLAY_10_SCENARIOS.size
          val scenario = ROLEPLAY_10_SCENARIOS[scenarioIdx]
          val lexItem = entries[idx % entries.size]
          val extraSentence = sentenceIn(lexItem, targetKey)

          val openingLine = scenario.openingsByLang[targetKey] ?: scenario.openingsByLang["en"]!!
          val baseSampleReply = scenario.sampleRepliesByLang[targetKey] ?: scenario.sampleRepliesByLang["en"]!!
          val uniqueSampleReply = if (spec.levelNumber == 1 && idx < ROLEPLAY_10_SCENARIOS.size) {
            baseSampleReply
          } else {
            "$baseSampleReply $extraSentence"
          }
          val followUps = scenario.followUpTurnsByLang[targetKey] ?: scenario.followUpTurnsByLang["en"].orEmpty()

          DynamicLevelExercise(
            id = "${game.id}_${languageCode}_${spec.levelNumber}_$idx",
            gameId = game.id,
            levelNumber = spec.levelNumber,
            mechanic = GameInteractionMechanic.SPEAKING_ROLEPLAY_MISSION,
            prompt = "${scenario.title} Mission: ${scenario.settingSummary}",
            targetPhrase = uniqueSampleReply,
            translation = scenario.openingTranslationEn,
            phonetic = scenario.id,
            options = scenario.missionObjectives,
            correctIndex = 0,
            dialogueSpeakerALine = openingLine,
            dialogueSpeakerBLine = followUps.firstOrNull().orEmpty(),
            secondaryAudioPhrase = followUps.getOrNull(1).orEmpty(),
            conversationStarter = openingLine,
            conversationRole = scenario.partnerRole,
            clozeMaskedSentence = scenario.title,
            hint = "Mission objective: ${scenario.missionObjectives.firstOrNull().orEmpty()}",
            explanation = "Example response: \"$baseSampleReply\"",
            objectiveLabel = spec.objectiveTitle,
            contentHash = GameCurriculumAndAntiRepetitionEngine.computeContentHash(languageCode, game.mechanic, uniqueSampleReply)
          )
        }
      }

      else -> emptyList()
    }
  }

  // ==================== HELPERS FOR SYLLABLES, PHONETICS & SENTENCE LENGTH ====================

  fun splitWordIntoDisplaySyllables(word: String): List<String> {
    val clean = word.trim()
    if (clean.length <= 3) return listOf(clean)
    val vowels = "aeiouáéíóúüàèìòùâêîôûäöüß"
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var sawVowel = false
    for (ch in clean) {
      current.append(ch)
      if (ch.lowercaseChar() in vowels) {
        sawVowel = true
      } else if (sawVowel && current.length >= 2) {
        parts.add(current.toString())
        current.clear()
        sawVowel = false
      }
    }
    if (current.isNotEmpty()) {
      if (parts.isNotEmpty() && current.length == 1 && current[0].lowercaseChar() !in vowels) {
        parts[parts.lastIndex] = parts.last() + current.toString()
      } else {
        parts.add(current.toString())
      }
    }
    return parts.ifEmpty { listOf(clean) }
  }

  fun detectDifficultSoundDescription(word: String, langCode: String): String {
    val lower = word.lowercase()
    return when {
      lower.contains("rr") -> "Rolled trilled /rr/ consonant"
      lower.contains("ñ") || lower.contains("gn") || lower.contains("nh") -> "Palatal nasal /ɲ/ sound"
      lower.contains("th") -> "Dental fricative /θ/ or /ð/"
      lower.contains("ch") || lower.contains("sch") || lower.contains("sh") -> "Crisp sibilant /ʃ/ or /tʃ/ cluster"
      lower.contains("qu") || lower.contains("gu") -> "Velar stop + vowel transition"
      lower.contains("ll") || lower.contains("lh") -> "Palatal lateral glide"
      lower.contains("r") && lower.contains("l") -> "Liquid /r/ and /l/ separation"
      lower.contains("r") -> "Clear target-language /r/ articulation"
      lower.contains("z") || lower.contains("s") -> "Crisp sibilant /s/ vs /z/ clarity"
      else -> "Primary vowel clarity & syllable stress"
    }
  }

  private fun buildPhoneticGuide(word: String, syllables: List<String>, langCode: String): String {
    val stressed = syllables.mapIndexed { idx, syl ->
      val stressIdx = if (syllables.size >= 2) (syllables.size - 2).coerceAtLeast(0) else 0
      if (idx == stressIdx) "ˈ${syl.lowercase()}" else syl.lowercase()
    }.joinToString("·")
    return "/$stressed/"
  }

  private fun ensureCompleteSpokenSentence(
    candidateSentence: String,
    keyWord: String,
    targetKey: String,
    seed: Int
  ): String {
    val clean = candidateSentence.trim()
    val wordCount = clean.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    if (wordCount >= 4) return clean
    return when (targetKey) {
      "es" -> "Por favor, necesito $keyWord para nuestra actividad de hoy."
      "fr" -> "S'il vous plaît, j'utilise $keyWord tous les jours avec plaisir."
      "de" -> "Ich brauche heute $keyWord für unsere tägliche Aufgabe."
      "ja" -> "$keyWord を使って毎日楽しく練習しています。"
      else -> "I always use $keyWord when communicating in everyday situations."
    }
  }

  // ==================== AI + DETERMINISTIC EVALUATION FOR ALL 10 SPEAKING GAMES ====================

  /**
   * Evaluates any of the 10 speaking games with game-specific criteria:
   * - Word-level & phoneme/syllable clarity for Game 1 (Pronunciation Practice)
   * - Complete sentence word-by-word matching for Game 2 (Repeat After Me) & Game 8 (Shadow Speaking)
   * - Consonant cluster & repetition clarity for Game 3 (Articulation Sprint)
   * - Open-ended visual description evaluation for Game 4 (Speak the Scene)
   * - Question-answering relevance (rejecting question echoes) for Game 5 (Quick Answer)
   * - Personalized grammar/vocab/pronunciation coaching + targeted follow-up drill for Game 6 (Speech Coach)
   * - Continuous speaking pace, word count & topic coverage for Game 7 (Fluency Sprint)
   * - Contextual conversation turn validation + Speaker A follow-up for Game 9 (Dialogue Line)
   */
  suspend fun evaluateSpeakingGameAttempt(
    exercise: DynamicLevelExercise,
    spokenTranscript: String,
    languageCode: String,
    nativeLanguageCode: String,
    speakingDurationSeconds: Int = 5,
    peakAudioRms: Float = 0.25f,
    usedRealMicrophoneAudio: Boolean = true
  ): SpeakingDetailedAssessment = withContext(Dispatchers.IO) {
    val cleanSpoken = spokenTranscript.trim()
    if (cleanSpoken.isBlank()) {
      return@withContext SpeakingDetailedAssessment(
        isPassed = false,
        overallScore = 0,
        feedbackHeadline = "No Speech Detected",
        grammarFeedback = "Tap the microphone and speak clearly in ${Language.findByCode(languageCode).name}.",
        vocabularyFeedback = "Try using words from the prompt or hint.",
        pronunciationNote = "Keep your device microphone about 15–20 cm from your mouth.",
        improvedSentence = exercise.targetPhrase
      )
    }

    when (exercise.mechanic) {
      GameInteractionMechanic.SPEAKING_MIC ->
        evaluatePronunciationWordAttempt(exercise, cleanSpoken, peakAudioRms, usedRealMicrophoneAudio)

      GameInteractionMechanic.SPEAKING_REPEAT_SENTENCE ->
        evaluateRepeatSentenceAttempt(exercise, cleanSpoken, peakAudioRms)

      GameInteractionMechanic.SPEAKING_ARTICULATION_SPRINT ->
        evaluateArticulationSprintAttempt(exercise, cleanSpoken, peakAudioRms)

      GameInteractionMechanic.SPEAKING_DESCRIBE_SCENE ->
        evaluateSpeakTheSceneAttempt(exercise, cleanSpoken, languageCode, nativeLanguageCode)

      GameInteractionMechanic.SPEAKING_QUICK_ANSWER ->
        evaluateQuickAnswerAttempt(exercise, cleanSpoken, languageCode, nativeLanguageCode)

      GameInteractionMechanic.SPEAKING_SPEECH_COACH ->
        evaluateSpeechCoachAttempt(exercise, cleanSpoken, languageCode, nativeLanguageCode, peakAudioRms, usedRealMicrophoneAudio)

      GameInteractionMechanic.SPEAKING_FLUENCY_SPRINT ->
        evaluateFluencySprintAttempt(exercise, cleanSpoken, languageCode, nativeLanguageCode, speakingDurationSeconds)

      GameInteractionMechanic.SPEAKING_SHADOWING ->
        evaluateShadowSpeakingAttempt(exercise, cleanSpoken, speakingDurationSeconds)

      GameInteractionMechanic.SPEAKING_DIALOGUE_LINE ->
        evaluateDialogueLineAttempt(exercise, cleanSpoken, languageCode, nativeLanguageCode)

      else ->
        evaluateRepeatSentenceAttempt(exercise, cleanSpoken, peakAudioRms)
    }
  }

  private fun evaluatePronunciationWordAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    peakRms: Float,
    usedRealMic: Boolean
  ): SpeakingDetailedAssessment {
    val target = exercise.targetPhrase.trim()
    val normSpoken = normalizeForComparison(spoken)
    val normTarget = normalizeForComparison(target)

    val charSim = characterSimilarity(normSpoken, normTarget)
    val syllables = splitWordIntoDisplaySyllables(target)
    val matchedSyllables = mutableListOf<String>()
    val missedSyllables = mutableListOf<String>()

    for (syl in syllables) {
      val normSyl = normalizeForComparison(syl)
      if (normSyl.isNotEmpty() && normSpoken.contains(normSyl)) {
        matchedSyllables.add(syl)
      } else {
        missedSyllables.add(syl)
      }
    }

    val acousticBonus = if (usedRealMic && peakRms >= 0.08f) 6 else 0
    val rawScore = ((charSim * 94f) + acousticBonus).toInt().coerceIn(0, 100)
    val passed = rawScore >= 70 || normSpoken == normTarget
    val focusSound = exercise.clozeMaskedSentence.ifBlank { detectDifficultSoundDescription(target, "en") }

    val pronunciationNote = when {
      rawScore >= 90 -> "Crisp articulation of ${syllables.joinToString("-")}! Your $focusSound was clear and well-paced."
      passed -> "Good pronunciation of \"$target\". Pay extra attention to ${missedSyllables.ifEmpty { listOf(focusSound) }.joinToString(", ")} for native crispness."
      else -> "We heard \"$spoken\". Focus on separating each syllable (${syllables.joinToString(" • ")}) and emphasizing the $focusSound."
    }

    return SpeakingDetailedAssessment(
      isPassed = passed,
      overallScore = if (normSpoken == normTarget) max(rawScore, 95) else rawScore,
      clarityScore = rawScore,
      matchedWords = matchedSyllables.ifEmpty { if (passed) listOf(target) else emptyList() },
      focusWordsOrSounds = missedSyllables.ifEmpty { listOf(focusSound) },
      feedbackHeadline = when {
        rawScore >= 90 -> "Excellent Pronunciation! (${max(rawScore, 95)}%)"
        passed -> "Clear Pronunciation! ($rawScore%)"
        else -> "Keep Refining This Sound ($rawScore%)"
      },
      grammarFeedback = "Target word: \"$target\" (${exercise.phonetic})",
      vocabularyFeedback = "Meaning: ${exercise.translation}",
      pronunciationNote = pronunciationNote,
      improvedSentence = target
    )
  }

  private fun evaluateRepeatSentenceAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    peakRms: Float
  ): SpeakingDetailedAssessment {
    val targetWords = tokenizeWords(exercise.targetPhrase)
    val spokenWords = tokenizeWords(spoken)

    val matched = mutableListOf<String>()
    val missing = mutableListOf<String>()

    for (tw in targetWords) {
      val normTw = normalizeForComparison(tw)
      val found = spokenWords.any { uw ->
        val normUw = normalizeForComparison(uw)
        normUw == normTw || (normTw.length >= 4 && levenshteinDistance(normUw, normTw) <= 1)
      }
      if (found) matched.add(tw) else missing.add(tw)
    }

    val wordCoverage = matched.size.toFloat() / targetWords.size.coerceAtLeast(1).toFloat()
    val charSim = characterSimilarity(normalizeForComparison(spoken), normalizeForComparison(exercise.targetPhrase))
    val score = ((wordCoverage * 0.70f + charSim * 0.30f) * 100f).toInt().coerceIn(0, 100)
    val passed = score >= 68

    return SpeakingDetailedAssessment(
      isPassed = passed,
      overallScore = score,
      clarityScore = score,
      fluencyScore = (wordCoverage * 100f).toInt(),
      matchedWords = matched,
      focusWordsOrSounds = missing,
      feedbackHeadline = when {
        score >= 90 -> "Complete Sentence Repeated! ($score%)"
        passed -> "Good Sentence Repetition! ($score%)"
        else -> "Some Words Were Missed ($score%)"
      },
      grammarFeedback = if (missing.isEmpty()) {
        "You captured all ${targetWords.size} words in the right sentence order."
      } else {
        "Matched ${matched.size}/${targetWords.size} words. Missing or unclear: ${missing.joinToString(", ")}."
      },
      vocabularyFeedback = "Translation: ${exercise.translation}",
      pronunciationNote = if (missing.isEmpty()) {
        "Smooth sentence rhythm from start to finish!"
      } else {
        "Tap '0.75x Slow' to hear how \"${missing.take(3).joinToString(" ")}\" connects to the rest of the sentence."
      },
      improvedSentence = exercise.targetPhrase
    )
  }

  private fun evaluateArticulationSprintAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    peakRms: Float
  ): SpeakingDetailedAssessment {
    val baseEval = evaluateRepeatSentenceAttempt(exercise, spoken, peakRms)
    val focusCluster = exercise.clozeMaskedSentence.ifBlank { "Consonant Clusters" }
    return baseEval.copy(
      feedbackHeadline = if (baseEval.isPassed) {
        "Crisp Articulation! (${baseEval.overallScore}%)"
      } else {
        "Tongue Twister Needs Another Spin (${baseEval.overallScore}%)"
      },
      pronunciationNote = if (baseEval.focusWordsOrSounds.isEmpty()) {
        "Great control of $focusCluster without consonant slurring!"
      } else {
        "Focus sound ($focusCluster): articulate \"${baseEval.focusWordsOrSounds.take(3).joinToString(", ")}\" cleanly at 0.70x first."
      }
    )
  }

  private suspend fun evaluateSpeakTheSceneAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    languageCode: String,
    nativeLanguageCode: String
  ): SpeakingDetailedAssessment {
    val langName = Language.findByCode(languageCode).name
    val nativeName = Language.findByCode(nativeLanguageCode).name
    val sceneKeywords = exercise.options

    val aiPrompt = """
      The learner is playing "Speak the Scene" in $langName.
      Scene Title: "${exercise.phonetic}"
      Question asked about the picture: "${exercise.prompt}"
      Reference scene description: "${exercise.targetPhrase}" (${exercise.translation})
      Scene keywords: ${sceneKeywords.joinToString(", ")}
      Learner's spoken description: "$spoken"
      Accept ANY accurate description of what is happening in the scene (do NOT require an exact match to the reference sentence).
      Evaluate relevant vocabulary, grammar, and meaning.
      Return ONLY a JSON object:
      {"passed":true,"score":88,"vocabScore":90,"grammarScore":85,"matchedKeywords":["word1","word2"],"grammarNote":"concise grammar feedback in $nativeName","vocabNote":"vocabulary & meaning feedback in $nativeName","improvedSentence":"improved natural sentence in $langName"}
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(aiPrompt)
    if (!raw.isNullOrBlank()) {
      runCatching {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val score = obj.optInt("score", 84).coerceIn(0, 100)
        val passed = obj.optBoolean("passed", score >= 65) && score >= 60
        val matchedArr = obj.optJSONArray("matchedKeywords")
        val matched = if (matchedArr != null) {
          List(matchedArr.length()) { matchedArr.optString(it) }.filter { it.isNotBlank() }
        } else emptyList()
        return SpeakingDetailedAssessment(
          isPassed = passed,
          overallScore = score,
          grammarScore = obj.optInt("grammarScore", score),
          vocabularyScore = obj.optInt("vocabScore", score),
          matchedWords = matched.ifEmpty { tokenizeWords(spoken).take(5) },
          focusWordsOrSounds = sceneKeywords.filter { kw -> !spoken.contains(kw, ignoreCase = true) }.take(3),
          feedbackHeadline = if (passed) "Vivid Scene Description! ($score%)" else "Add More Scene Details ($score%)",
          grammarFeedback = obj.optString("grammarNote", "Good descriptive sentence structure."),
          vocabularyFeedback = obj.optString("vocabNote", "You described the key action in the picture."),
          pronunciationNote = "Descriptive speaking builds spontaneous visual-to-speech fluency.",
          improvedSentence = obj.optString("improvedSentence", exercise.targetPhrase)
        )
      }
    }

    // Deterministic semantic & keyword evaluation (accepts any meaningful scene description)
    val spokenTokens = tokenizeWords(spoken)
    val matchedSceneWords = sceneKeywords.filter { kw ->
      val kwTokens = tokenizeWords(kw)
      kwTokens.any { kwt ->
        spokenTokens.any { st -> normalizeForComparison(st) == normalizeForComparison(kwt) }
      }
    }
    val modelTokens = tokenizeWords(exercise.targetPhrase)
    val overlapWithModel = spokenTokens.filter { st ->
      modelTokens.any { mt -> normalizeForComparison(st) == normalizeForComparison(mt) }
    }
    val allMatched = (matchedSceneWords + overlapWithModel).distinct()
    val isQuestionEcho = normalizeForComparison(spoken) == normalizeForComparison(exercise.prompt)

    val score = when {
      isQuestionEcho -> 30
      spokenTokens.size >= 5 && allMatched.isNotEmpty() -> 90
      spokenTokens.size >= 3 && allMatched.isNotEmpty() -> 82
      spokenTokens.size >= 4 -> 76
      spokenTokens.size >= 2 -> 68
      else -> 45
    }
    val passed = !isQuestionEcho && score >= 65

    return SpeakingDetailedAssessment(
      isPassed = passed,
      overallScore = score,
      grammarScore = score,
      vocabularyScore = if (allMatched.isNotEmpty()) min(100, score + 6) else score,
      matchedWords = allMatched.ifEmpty { spokenTokens.take(4) },
      focusWordsOrSounds = sceneKeywords.filter { it !in matchedSceneWords }.take(3),
      feedbackHeadline = if (passed) "Great Scene Description! ($score%)" else "Describe What You See ($score%)",
      grammarFeedback = if (isQuestionEcho) {
        "Instead of repeating the question, describe what the people are doing in the picture."
      } else if (spokenTokens.size >= 4) {
        "Well-formed descriptive statement (${spokenTokens.size} words)."
      } else {
        "Try speaking a full sentence with a subject and action verb."
      },
      vocabularyFeedback = if (allMatched.isNotEmpty()) {
        "Used relevant scene vocabulary: ${allMatched.joinToString(", ")}."
      } else {
        "Try including scene words like: ${sceneKeywords.take(4).joinToString(", ")}."
      },
      pronunciationNote = "Clear descriptive delivery!",
      improvedSentence = exercise.targetPhrase
    )
  }

  private suspend fun evaluateQuickAnswerAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    languageCode: String,
    nativeLanguageCode: String
  ): SpeakingDetailedAssessment {
    val normSpoken = normalizeForComparison(spoken)
    val normQuestion = normalizeForComparison(exercise.prompt)
    val questionSim = characterSimilarity(normSpoken, normQuestion)

    // Guard: Do NOT allow repeating the question to pass as an answer!
    if (questionSim >= 0.80f && normSpoken != normalizeForComparison(exercise.targetPhrase)) {
      return SpeakingDetailedAssessment(
        isPassed = false,
        overallScore = 35,
        feedbackHeadline = "Answer the Question in Your Own Words",
        grammarFeedback = "It sounds like you repeated the question (\"${exercise.prompt}\"). Give your own personal answer instead!",
        vocabularyFeedback = "Try using words like: ${exercise.options.take(4).joinToString(", ")}.",
        pronunciationNote = "In Quick Answer, respond naturally as if someone just asked you this in daily life.",
        improvedSentence = exercise.targetPhrase
      )
    }

    val langName = Language.findByCode(languageCode).name
    val nativeName = Language.findByCode(nativeLanguageCode).name
    val aiPrompt = """
      The learner is playing "Quick Answer" in $langName.
      Everyday Question: "${exercise.prompt}" (${exercise.translation})
      Learner's Spoken Answer: "$spoken"
      Sample Natural Answer: "${exercise.targetPhrase}"
      Evaluate whether the learner's spoken answer genuinely addresses the question in $langName and provide concise grammar feedback in $nativeName.
      Return ONLY a JSON object:
      {"passed":true,"score":90,"grammarNote":"concise grammar feedback in $nativeName","vocabNote":"relevance note in $nativeName","improvedAnswer":"natural polished version of the learner's answer in $langName"}
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(aiPrompt)
    if (!raw.isNullOrBlank()) {
      runCatching {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val score = obj.optInt("score", 86).coerceIn(0, 100)
        val passed = obj.optBoolean("passed", score >= 65) && score >= 60
        return SpeakingDetailedAssessment(
          isPassed = passed,
          overallScore = score,
          grammarScore = score,
          matchedWords = tokenizeWords(spoken),
          feedbackHeadline = if (passed) "Natural Quick Answer! ($score%)" else "Refine Your Answer ($score%)",
          grammarFeedback = obj.optString("grammarNote", "Clear, natural response to the question."),
          vocabularyFeedback = obj.optString("vocabNote", "Your answer directly addressed the question."),
          pronunciationNote = "Quick, communicative response!",
          improvedSentence = obj.optString("improvedAnswer", exercise.targetPhrase)
        )
      }
    }

    val spokenWords = tokenizeWords(spoken)
    val matchedKeywords = exercise.options.filter { kw ->
      spokenWords.any { sw -> normalizeForComparison(sw) == normalizeForComparison(kw) }
    }
    val score = when {
      spokenWords.size >= 4 && matchedKeywords.isNotEmpty() -> 92
      spokenWords.size >= 3 -> 85
      spokenWords.size >= 2 -> 76
      spokenWords.size == 1 -> 65
      else -> 40
    }
    val passed = score >= 65

    return SpeakingDetailedAssessment(
      isPassed = passed,
      overallScore = score,
      grammarScore = score,
      matchedWords = spokenWords,
      focusWordsOrSounds = exercise.options.take(3),
      feedbackHeadline = if (passed) "Great Quick Answer! ($score%)" else "Try a Complete Sentence ($score%)",
      grammarFeedback = if (spokenWords.size >= 3) {
        "Your answer directly responds to the question with a natural phrase."
      } else {
        "Expand your short answer into a full sentence for stronger grammar practice."
      },
      vocabularyFeedback = if (matchedKeywords.isNotEmpty()) {
        "Great use of topic vocabulary: ${matchedKeywords.joinToString(", ")}."
      } else {
        "Relevant everyday answer!"
      },
      pronunciationNote = "Spontaneous response delivered clearly.",
      improvedSentence = exercise.targetPhrase
    )
  }

  private suspend fun evaluateSpeechCoachAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    languageCode: String,
    nativeLanguageCode: String,
    peakRms: Float,
    usedRealMic: Boolean
  ): SpeakingDetailedAssessment {
    val langName = Language.findByCode(languageCode).name
    val nativeName = Language.findByCode(nativeLanguageCode).name
    val focusWord = exercise.phonetic.ifBlank { exercise.targetPhrase.split(" ").firstOrNull().orEmpty() }
    val soundFocus = exercise.clozeMaskedSentence.ifBlank { detectDifficultSoundDescription(focusWord, languageCode) }

    val aiPrompt = """
      You are the LingoSphere Personal Speech Coach for $langName (explaining in $nativeName).
      Speaking Prompt: "${exercise.prompt}"
      Model Reference: "${exercise.targetPhrase}"
      Learner's Spoken Transcript: "$spoken"
      Analyze the learner's response:
      1. Identify any grammar or vocabulary improvements and explain them simply in $nativeName.
      2. Provide an improved native-like version in $langName.
      3. Create ONE targeted follow-up practice sentence in $langName ("followUpPhrase") that specifically practices the grammar or vocabulary from the learner's attempt.
      Return ONLY a JSON object:
      {"passed":true,"score":86,"grammarNote":"simple grammar explanation in $nativeName","vocabNote":"vocabulary feedback in $nativeName","improvedVersion":"improved sentence in $langName","followUpPrompt":"Targeted Follow-Up goal in $nativeName","followUpPhrase":"short follow-up practice sentence in $langName"}
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(aiPrompt)
    if (!raw.isNullOrBlank()) {
      runCatching {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val score = obj.optInt("score", 85).coerceIn(0, 100)
        val passed = obj.optBoolean("passed", score >= 65) && score >= 60
        val improved = obj.optString("improvedVersion", exercise.targetPhrase)
        val followUpPhrase = obj.optString("followUpPhrase", improved)
        val followUpPrompt = obj.optString("followUpPrompt", "Now practice this polished sentence aloud:")
        val pronNote = if (usedRealMic) {
          "Audio analysis: Clear voice projection (focus sound: $soundFocus)."
        } else {
          "Pronunciation focus for \"$focusWord\": $soundFocus."
        }
        return SpeakingDetailedAssessment(
          isPassed = passed,
          overallScore = score,
          grammarScore = score,
          vocabularyScore = score,
          matchedWords = tokenizeWords(spoken),
          focusWordsOrSounds = listOf(focusWord, soundFocus).filter { it.isNotBlank() },
          feedbackHeadline = "Speech Coach Analysis ($score%)",
          grammarFeedback = obj.optString("grammarNote", "Solid sentence structure."),
          vocabularyFeedback = obj.optString("vocabNote", "Good expressive vocabulary choice."),
          pronunciationNote = pronNote,
          improvedSentence = improved,
          targetedFollowUpPrompt = followUpPrompt,
          targetedFollowUpPhrase = followUpPhrase
        )
      }
    }

    // Deterministic Speech Coach analysis + adaptive follow-up drill
    val baseEval = evaluateRepeatSentenceAttempt(exercise, spoken, peakRms)
    val spokenWords = tokenizeWords(spoken)
    val usedFocusWord = spokenWords.any { normalizeForComparison(it) == normalizeForComparison(focusWord) }
    val score = when {
      spokenWords.size >= 5 && usedFocusWord -> max(baseEval.overallScore, 88)
      spokenWords.size >= 4 -> max(baseEval.overallScore, 80)
      spokenWords.size >= 2 -> max(baseEval.overallScore, 72)
      else -> baseEval.overallScore
    }
    val followUpSentence = if (baseEval.focusWordsOrSounds.isNotEmpty()) {
      exercise.targetPhrase
    } else {
      exercise.secondaryAudioPhrase.ifBlank { exercise.targetPhrase }
    }

    return SpeakingDetailedAssessment(
      isPassed = score >= 65,
      overallScore = score,
      grammarScore = score,
      vocabularyScore = if (usedFocusWord) 92 else 78,
      matchedWords = baseEval.matchedWords.ifEmpty { spokenWords },
      focusWordsOrSounds = baseEval.focusWordsOrSounds.ifEmpty { listOf(focusWord) },
      feedbackHeadline = "Speech Coach Feedback ($score%)",
      grammarFeedback = if (spokenWords.size >= 5) {
        "You formed a complete ${spokenWords.size}-word sentence with clear subject-verb structure."
      } else {
        "Try linking your idea with a connector or time expression for a fuller sentence."
      },
      vocabularyFeedback = if (usedFocusWord) {
        "Great job incorporating \"$focusWord\" naturally in context!"
      } else {
        "Try weaving the key expression \"$focusWord\" into your response."
      },
      pronunciationNote = if (usedRealMic) {
        "Acoustic check: Pay special attention to $soundFocus in \"$focusWord\"."
      } else {
        "Focus on $soundFocus when pronouncing \"$focusWord\"."
      },
      improvedSentence = exercise.targetPhrase,
      targetedFollowUpPrompt = if (baseEval.focusWordsOrSounds.isNotEmpty()) {
        "Targeted Follow-Up: Practice the words you missed (${baseEval.focusWordsOrSounds.take(3).joinToString(", ")})"
      } else {
        "Targeted Follow-Up: Say the polished version smoothly at natural speed"
      },
      targetedFollowUpPhrase = followUpSentence
    )
  }

  private suspend fun evaluateFluencySprintAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    languageCode: String,
    nativeLanguageCode: String,
    speakingDurationSeconds: Int
  ): SpeakingDetailedAssessment {
    val spokenWords = tokenizeWords(spoken)
    val wordCount = spokenWords.size
    val effectiveSeconds = speakingDurationSeconds.coerceAtLeast(5)
    val wpm = ((wordCount * 60f) / effectiveSeconds.toFloat()).toInt().coerceIn(20, 190)

    val score = when {
      wordCount >= 14 -> 94
      wordCount >= 9 -> 88
      wordCount >= 5 -> 80
      wordCount >= 3 -> 72
      else -> 50
    }
    val passed = wordCount >= 3

    return SpeakingDetailedAssessment(
      isPassed = passed,
      overallScore = score,
      fluencyScore = score,
      vocabularyScore = min(100, 65 + wordCount * 2),
      matchedWords = spokenWords.distinct().take(10),
      focusWordsOrSounds = exercise.options,
      feedbackHeadline = if (passed) "Fluency Sprint Complete! ($wordCount words • ~$wpm WPM)" else "Speak a Bit Longer ($wordCount words)",
      grammarFeedback = if (wordCount >= 8) {
        "You maintained continuous spoken flow across multiple connected clauses."
      } else {
        "Use the talking-point bullets to connect 2–3 sentences without stopping."
      },
      vocabularyFeedback = "Topic: ${exercise.phonetic} • Spoken words: $wordCount • Pace: ~$wpm WPM.",
      pronunciationNote = "Keep your rhythm steady—don't worry about small hesitations during a Fluency Sprint!",
      improvedSentence = exercise.targetPhrase,
      wordsPerMinute = wpm,
      wordCount = wordCount
    )
  }

  private fun evaluateShadowSpeakingAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    speakingDurationSeconds: Int
  ): SpeakingDetailedAssessment {
    val base = evaluateRepeatSentenceAttempt(exercise, spoken, 0.3f)
    val targetWordsCount = tokenizeWords(exercise.targetPhrase).size.coerceAtLeast(1)
    val spokenWordsCount = tokenizeWords(spoken).size
    val lengthRatio = (spokenWordsCount.toFloat() / targetWordsCount.toFloat()).coerceIn(0f, 1.5f)
    val pacingScore = (100 - abs(1.0f - lengthRatio) * 45f).toInt().coerceIn(55, 100)
    val combined = ((base.overallScore * 0.75f) + (pacingScore * 0.25f)).toInt().coerceIn(0, 100)

    return base.copy(
      isPassed = combined >= 68,
      overallScore = combined,
      fluencyScore = pacingScore,
      feedbackHeadline = if (combined >= 85) {
        "In-Sync Shadowing! ($combined%)"
      } else if (combined >= 68) {
        "Good Shadow Rhythm! ($combined%)"
      } else {
        "Match the Speaker's Cadence ($combined%)"
      },
      pronunciationNote = "Timing & cadence match: $pacingScore% • Word match: ${base.overallScore}%."
    )
  }

  private suspend fun evaluateDialogueLineAttempt(
    exercise: DynamicLevelExercise,
    spoken: String,
    languageCode: String,
    nativeLanguageCode: String
  ): SpeakingDetailedAssessment {
    val partnerLine = exercise.dialogueSpeakerALine.ifBlank { exercise.secondaryAudioPhrase }
    val followUpLine = exercise.dialogueSpeakerBLine
    val normSpoken = normalizeForComparison(spoken)
    val normPartner = normalizeForComparison(partnerLine)

    if (normSpoken == normPartner && normSpoken != normalizeForComparison(exercise.targetPhrase)) {
      return SpeakingDetailedAssessment(
        isPassed = false,
        overallScore = 35,
        feedbackHeadline = "Speak Your Turn in the Dialogue",
        grammarFeedback = "You repeated Speaker A's line. Speak the next response in the conversation instead!",
        vocabularyFeedback = "Goal: ${exercise.prompt}",
        pronunciationNote = "Respond naturally to move the conversation forward.",
        improvedSentence = exercise.targetPhrase,
        partnerContinuationLine = followUpLine
      )
    }

    val langName = Language.findByCode(languageCode).name
    val nativeName = Language.findByCode(nativeLanguageCode).name
    val aiPrompt = """
      The learner is playing "Dialogue Line" in $langName.
      Context: "${exercise.prompt}"
      Speaker A (${exercise.conversationRole}) said: "$partnerLine"
      Learner (Speaker B) replied: "$spoken"
      Model Reply: "${exercise.targetPhrase}"
      Accept ANY natural, contextually appropriate reply in $langName that responds to Speaker A.
      Return ONLY a JSON object:
      {"passed":true,"score":90,"grammarNote":"concise note in $nativeName","improvedReply":"polished reply in $langName"}
    """.trimIndent()

    val raw = GeminiTutorService.generateStructuredJson(aiPrompt)
    if (!raw.isNullOrBlank()) {
      runCatching {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val score = obj.optInt("score", 88).coerceIn(0, 100)
        val passed = obj.optBoolean("passed", score >= 65) && score >= 60
        return SpeakingDetailedAssessment(
          isPassed = passed,
          overallScore = score,
          grammarScore = score,
          matchedWords = tokenizeWords(spoken),
          feedbackHeadline = if (passed) "Natural Dialogue Turn! ($score%)" else "Adjust Your Reply ($score%)",
          grammarFeedback = obj.optString("grammarNote", "Your line fits the conversation naturally."),
          vocabularyFeedback = "${exercise.conversationRole} replies next: \"$followUpLine\"",
          pronunciationNote = "Conversational turn completed!",
          improvedSentence = obj.optString("improvedReply", exercise.targetPhrase),
          partnerContinuationLine = followUpLine
        )
      }
    }

    val spokenWords = tokenizeWords(spoken)
    val simToModel = characterSimilarity(normSpoken, normalizeForComparison(exercise.targetPhrase))
    val score = when {
      simToModel >= 0.70f -> (simToModel * 100f).toInt().coerceAtLeast(88)
      spokenWords.size >= 3 -> 86
      spokenWords.size >= 2 -> 76
      else -> 62
    }
    val passed = score >= 65

    return SpeakingDetailedAssessment(
      isPassed = passed,
      overallScore = score,
      grammarScore = score,
      matchedWords = spokenWords,
      feedbackHeadline = if (passed) "Dialogue Completed Naturally! ($score%)" else "Try a Complete Reply ($score%)",
      grammarFeedback = "Your response continues the conversation with ${exercise.conversationRole}.",
      vocabularyFeedback = if (followUpLine.isNotBlank()) {
        "${exercise.conversationRole}'s next line: \"$followUpLine\""
      } else {
        "Model reply: \"${exercise.targetPhrase}\""
      },
      pronunciationNote = "Natural conversational turn-taking!",
      improvedSentence = exercise.targetPhrase,
      partnerContinuationLine = followUpLine
    )
  }

  private fun tokenizeWords(text: String): List<String> {
    return text.replace(Regex("[.,!?;:\"'¡¿()\\[\\]]"), " ")
      .split(Regex("\\s+"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }
  }

  private fun normalizeForComparison(text: String): String {
    return text.lowercase()
      .replace(Regex("[.,!?;:\"'¡¿()\\[\\]\\-—]"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
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
}
